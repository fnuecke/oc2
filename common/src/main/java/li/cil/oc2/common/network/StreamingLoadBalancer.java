/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public abstract class StreamingLoadBalancer<K, E extends StreamingLoadBalancer.Entry> {
    public abstract static class Entry {
        private Entry next = this;
        private Entry previous = this;

        private final WeakHashMap<ServerPlayer, Long> players = new WeakHashMap<>();

        private int skipCount;

        protected abstract boolean isReady();

        protected abstract void send(List<ServerPlayer> recipients);

        protected void onPlayerAdded() {
        }
    }

    // --------------------------------------------------------------------- //

    private static final long CACHE_EXPIRES_AFTER_MILLIS = 2000;

    // --------------------------------------------------------------------- //

    private final IntSupplier averageMaxBytesPerSecond;
    private final int maxSendsPerTick;
    private final LongSupplier clock;

    private final Map<K, E> entries = new ConcurrentHashMap<>();
    private final AtomicInteger budget;

    @Nullable
    private Entry lastSender;

    // --------------------------------------------------------------------- //

    protected StreamingLoadBalancer(final IntSupplier averageMaxBytesPerSecond, final int maxSendsPerTick, final LongSupplier clock) {
        this.averageMaxBytesPerSecond = averageMaxBytesPerSecond;
        this.maxSendsPerTick = maxSendsPerTick;
        this.clock = clock;
        this.budget = new AtomicInteger(getMaxBudget());
    }

    // --------------------------------------------------------------------- //

    public final void update(final K key, final ServerPlayer player) {
        final Entry entry = entries.computeIfAbsent(key, this::addEntry);
        if (entry.players.put(player, clock.getAsLong()) == null) {
            entry.onPlayerAdded();
        }
    }

    public final void remove(final K key) {
        final E entry = entries.remove(key);
        if (entry != null) {
            removeEntry(entry);
        }
    }

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

    public final void clear() {
        entries.clear();
        lastSender = null;
    }

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
            entry.skipCount = Math.max(0, entry.players.size() - 1);
        }

        return isReady;
    }
}
