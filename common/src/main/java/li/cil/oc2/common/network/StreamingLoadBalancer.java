/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Mostly round-robin load balancer for streaming media to clients.
 * <p>
 * We try to satisfy two limits here:
 * <ul>
 * <li>
 * Server side, limiting overall bandwidth consumed by one kind of media.
 * </li>
 * <li>
 * Client side, limiting per-client bandwidth consumed by it.
 * </li>
 * </ul>
 * <p>
 * To achieve this, there's a global budget and a per-source skip count. The global budget
 * controls overall data sent from the server. The skip counts modulate the round-robin behavior
 * of the load balancer. For example, sources further away from their closest watcher will get
 * a penalty, as will sources with a large number of players watching them.
 *
 * @param <K> the key sources are tracked by.
 * @param <E> the per-source state.
 */
public abstract class StreamingLoadBalancer<K, E extends StreamingLoadBalancer.Entry> {
    /**
     * Tracks a single source. Entries form a circular double linked list, i.e. the last entry
     * will always point back to the first entry, which makes looping over it for the round-robin
     * load-balancing more convenient.
     */
    public abstract static class Entry {
        private Entry next = this;
        private Entry previous = this;

        /**
         * The players watching this source, to know where to send the data and how strongly to penalize.
         */
        private final WeakHashMap<ServerPlayer, Long> players = new WeakHashMap<>();

        /**
         * The current penalty, in the form of rounds in the round-robin to skip.
         */
        private int skipCount;

        /**
         * The position penalties for distant watchers are measured from.
         */
        protected abstract Vec3 getPosition();

        /**
         * Whether there is something to send right now.
         */
        protected abstract boolean isReady();

        /**
         * Sends what is ready. Implementations consume budget for what they send via {@link #consumeBudget(int)}.
         */
        protected abstract void send(List<ServerPlayer> recipients);

        /**
         * Called when a player starts watching this source.
         */
        protected void onPlayerAdded() {
        }
    }

    // --------------------------------------------------------------------- //

    private static final long CACHE_EXPIRES_AFTER_MILLIS = 2000;

    // --------------------------------------------------------------------- //

    private final IntSupplier averageMaxBytesPerSecond;
    private final int maxSendsPerTick;
    private final double penaltyDistance;
    private final LongSupplier clock;

    /**
     * Maps sources to their entries, i.e. players watching them and sending state.
     * <p>
     * Player watching state is expired manually, by checking the last update time for players in
     * the entry every tick. This is required in case two players start watching a source but then
     * only one keeps watching it. In that case, we want to still remove the other player from the
     * entry, but keep the entry for the still watching player.
     */
    private final Map<K, E> entries = new HashMap<>();

    /**
     * Global byte budget for sending stuff to clients. This is filled up every tick and consumed
     * when sending packets to clients. We only send stuff when budget is non-negative.
     */
    private final AtomicInteger budget;

    /**
     * Pointer into our circular list pointing to the entry we last sent a packet for.
     * <p>
     * At the same time represents the head of our doubly-linked, circular list of entries. This
     * list is used to do the round-robin load balancing, by advancing it to the next entry until
     * we find one that can send something.
     */
    @Nullable
    private Entry lastSender;

    // --------------------------------------------------------------------- //

    /**
     * @param averageMaxBytesPerSecond the bandwidth this kind of media may use on average.
     * @param maxSendsPerTick          how many sources may send per tick.
     * @param penaltyDistance          distance to the closest watcher beyond which a source sends less often.
     * @param clock                    wall clock in milliseconds, used to expire watchers.
     */
    protected StreamingLoadBalancer(final IntSupplier averageMaxBytesPerSecond, final int maxSendsPerTick,
                                    final double penaltyDistance, final LongSupplier clock) {
        this.averageMaxBytesPerSecond = averageMaxBytesPerSecond;
        this.maxSendsPerTick = maxSendsPerTick;
        this.penaltyDistance = penaltyDistance;
        this.clock = clock;
        this.budget = new AtomicInteger(getMaxBudget());
    }

    // --------------------------------------------------------------------- //

    /**
     * Updates timestamp of a player currently watching a source.
     */
    public final void update(final K key, final ServerPlayer player) {
        final Entry entry = entries.computeIfAbsent(key, this::addEntry);
        if (entry.players.put(player, clock.getAsLong()) == null) {
            entry.onPlayerAdded();
        }
    }

    /**
     * Stops tracking a source right away, rather than once its watchers expire.
     */
    public final void remove(final K key) {
        final E entry = entries.remove(key);
        if (entry != null) {
            removeEntry(entry);
        }
    }

    /**
     * Expires stale watchers and sends whatever is ready, as far as the budget allows.
     * <p>
     * Call once per server tick.
     */
    public final void tick() {
        removeExpiredPlayers();

        if (budget.updateAndGet(this::replenishBudget) <= 0) {
            return;
        }

        for (int i = 0; i < maxSendsPerTick && budget.get() > 0; i++) {
            if (!sendNextReady()) {
                break;
            }
        }
    }

    /**
     * Forgets every source, e.g. when the server stops.
     */
    public final void clear() {
        entries.clear();
        lastSender = null;
    }

    /**
     * Charges data sent against the budget. Safe to call from any thread.
     */
    public final void consumeBudget(final int bytes) {
        budget.addAndGet(-bytes);
    }

    // --------------------------------------------------------------------- //

    protected abstract E createEntry(K key);

    @Nullable
    protected final E getEntry(final K key) {
        return entries.get(key);
    }

    // --------------------------------------------------------------------- //

    private int getMaxBudget() {
        // We allow over-budgeting to some degree, to allow short bursts of larger payloads.
        // Otherwise, this would be divided by twenty, since we attempt to send every tick.
        return averageMaxBytesPerSecond.getAsInt() / 2;
    }

    private int replenishBudget(final int value) {
        return Math.min(getMaxBudget(), value + Math.max(1, averageMaxBytesPerSecond.getAsInt() / 20));
    }

    private void removeExpiredPlayers() {
        final long now = clock.getAsLong();
        final var iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            final Entry entry = iterator.next();
            entry.players.entrySet().removeIf(player -> now - player.getValue() > CACHE_EXPIRES_AFTER_MILLIS);
            if (entry.players.isEmpty()) {
                iterator.remove();
                removeEntry(entry);
            }
        }
    }

    private E addEntry(final K key) {
        final E entry = createEntry(key);
        if (lastSender == null) {
            // No sender yet, start the circle.
            lastSender = entry;
        } else {
            // Just add after last sender.
            final Entry added = entry;
            added.next = lastSender.next;
            lastSender.next.previous = added;
            lastSender.next = added;
            added.previous = lastSender;
        }
        return entry;
    }

    private void removeEntry(final Entry entry) {
        if (lastSender == entry) {
            if (entry.next == entry) {
                // Last element in list, clear list.
                lastSender = null;
            } else {
                // Shift current entry to next.
                lastSender = entry.next;
            }
        }

        entry.previous.next = entry.next;
        entry.next.previous = entry.previous;
        entry.next = entry;
        entry.previous = entry;
    }

    private boolean sendNextReady() {
        if (lastSender == null) {
            return false;
        }

        final Entry start = lastSender;
        Entry current = start;
        do {
            current = current.next;
            lastSender = current;
            if (sendIfReady(current)) {
                return true;
            }
        } while (current != start);

        return false;
    }

    private boolean sendIfReady(final Entry entry) {
        if (entry.skipCount > 0) {
            entry.skipCount--;
            return false;
        }

        final boolean isReady = !entry.players.isEmpty() && entry.isReady();
        if (isReady) {
            entry.send(List.copyOf(entry.players.keySet()));
            updateSkipCount(entry);
        }

        return isReady;
    }

    private void updateSkipCount(final Entry entry) {
        entry.skipCount = 0;

        double closestPlayerDistanceSqr = Double.MAX_VALUE;
        final Vec3 position = entry.getPosition();
        for (final ServerPlayer player : entry.players.keySet()) {
            entry.skipCount++;
            closestPlayerDistanceSqr = Math.min(closestPlayerDistanceSqr, player.distanceToSqr(position));
        }

        if (Math.sqrt(closestPlayerDistanceSqr) > penaltyDistance) {
            entry.skipCount++;
        }
    }
}
