/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class StreamingLoadBalancerTests {
    private static final int PLENTY_OF_BUDGET = 1024 * 1024;
    private static final long CACHE_EXPIRY_MILLIS = 2000;

    private final AtomicLong now = new AtomicLong();
    private final List<String> sent = new ArrayList<>();
    private final List<ServerPlayer> players = new ArrayList<>(); // weak refs keep-alive

    @BeforeAll
    public static void setUpAll() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    public void setUp() {
        now.set(0);
        sent.clear();
        players.clear();
    }

    // --------------------------------------------------------------------- //

    @Test
    public void aWatchedSourceSendsToPlayers() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        final ServerPlayer player = playerAtOrigin();
        balancer.update("a", player);

        balancer.tick();

        assertEquals(List.of("a"), sent);
        assertEquals(List.of(player), balancer.entry("a").recipients.get(0));
    }

    @Test
    public void aSourceWithNothingToSendSendsNothing() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        balancer.update("a", playerAtOrigin());
        balancer.entry("a").ready = false;

        balancer.tick();

        assertTrue(sent.isEmpty());
    }

    @Test
    public void aSourceIsForgottenOnceItsCachesExpire() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        balancer.update("a", playerAtOrigin());

        now.set(CACHE_EXPIRY_MILLIS + 1);
        balancer.tick();

        assertTrue(sent.isEmpty(), "nobody is watching anymore, so nothing may be sent");
        assertNull(balancer.getEntry("a"), "and the source must not cost anything");
    }

    @Test
    public void renewingKeepsASourceAlive() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        final ServerPlayer player = playerAtOrigin();
        balancer.update("a", player);

        now.set(CACHE_EXPIRY_MILLIS - 500);
        balancer.update("a", player);
        now.set(CACHE_EXPIRY_MILLIS + 1);
        balancer.tick();

        assertEquals(List.of("a"), sent);
    }

    @Test
    public void onlyNewPlayersAreAnnounced() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        final ServerPlayer first = playerAtOrigin();
        final ServerPlayer second = playerAtOrigin();

        balancer.update("a", first);
        balancer.update("a", first);
        assertEquals(1, balancer.entry("a").playersAdded, "renewing is not starting to watch");

        balancer.update("a", second);
        assertEquals(2, balancer.entry("a").playersAdded);
    }

    @Test
    public void oneSourceSendsPerTickWhenLimitedToOne() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        final ServerPlayer player = playerAtOrigin();
        balancer.update("a", player);
        balancer.update("b", player);
        balancer.update("c", player);

        balancer.tick();

        assertEquals(1, sent.size());
    }

    @Test
    public void everySourceGetsItsTurn() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        final ServerPlayer player = playerAtOrigin();
        balancer.update("a", player);
        balancer.update("b", player);
        balancer.update("c", player);

        for (int i = 0; i < 3; i++) {
            balancer.tick();
        }

        final List<String> order = new ArrayList<>(sent);
        Collections.sort(order);
        assertEquals(List.of("a", "b", "c"), order, "round-robin: each source once in three ticks");
    }

    @Test
    public void severalSourcesSendInOneTickWhenAllowed() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 3);
        final ServerPlayer player = playerAtOrigin();
        balancer.update("a", player);
        balancer.update("b", player);
        balancer.update("c", player);

        balancer.tick();

        final List<String> order = new ArrayList<>(sent);
        Collections.sort(order);
        assertEquals(List.of("a", "b", "c"), order);
    }

    @Test
    public void aNewSourceIsServedNext() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        final ServerPlayer player = playerAtOrigin();
        balancer.update("a", player);
        balancer.update("b", player);
        balancer.update("c", player);

        for (int i = 0; i < 3; i++) {
            balancer.tick();
        }

        assertEquals(List.of("c", "b", "a"), sent, "new sources are linked in right after the current one");
    }

    @Test
    public void sourcesWithMorePlayersSendLessOften() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        balancer.update("one", playerAtOrigin());
        balancer.update("two", playerAtOrigin());
        balancer.update("two", playerAtOrigin());

        for (int i = 0; i < 30; i++) {
            balancer.tick();
        }

        final int oneCount = Collections.frequency(sent, "one");
        final int twoCount = Collections.frequency(sent, "two");
        assertTrue(oneCount > twoCount, "one player sent " + oneCount + " times, two players " + twoCount);
    }

    @Test
    public void spendingTheBudgetPausesSending() {
        // Budget caps at 200, and 20 comes back per tick.
        final TestBalancer balancer = new TestBalancer(400, 1);
        balancer.update("a", playerAtOrigin());
        balancer.entry("a").payloadSize = 1000;

        balancer.tick();
        assertEquals(1, sent.size(), "a full budget pays for the first send");

        for (int tick = 2; tick <= 41; tick++) {
            balancer.tick();
        }
        assertEquals(1, sent.size(), "a budget overdrawn by 800 takes 40 ticks to recover");

        for (int tick = 42; tick <= 50; tick++) {
            balancer.tick();
        }
        assertEquals(2, sent.size(), "and then sending resumes");
    }

    @Test
    public void sourcesFarFromTheirPlayersSendLessOften() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        final ServerPlayer player = playerAtOrigin();
        balancer.update("near", player);
        balancer.update("far", player);
        balancer.entry("far").position = new Vec3(100, 0, 0);

        for (int i = 0; i < 30; i++) {
            balancer.tick();
        }

        final int nearCount = Collections.frequency(sent, "near");
        final int farCount = Collections.frequency(sent, "far");
        assertTrue(nearCount > farCount, "near sent " + nearCount + " times, far " + farCount);
    }

    @Test
    public void removingASourceStopsItImmediately() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        balancer.update("a", playerAtOrigin());

        balancer.remove("a");
        balancer.tick();

        assertTrue(sent.isEmpty());
        assertNull(balancer.getEntry("a"));
    }

    @Test
    public void removingOneSourceKeepsTheOthersInRotation() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        final ServerPlayer player = playerAtOrigin();
        balancer.update("a", player);
        balancer.update("b", player);
        balancer.update("c", player);

        balancer.remove("b");
        for (int i = 0; i < 4; i++) {
            balancer.tick();
        }

        assertFalse(sent.contains("b"));
        assertTrue(sent.contains("a") && sent.contains("c"), "sent " + sent);
    }

    @Test
    public void clearingForgetsEverySource() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        final ServerPlayer player = playerAtOrigin();
        balancer.update("a", player);

        balancer.clear();
        balancer.tick();
        assertTrue(sent.isEmpty(), "a stopped server's sources must not keep sending");

        balancer.update("b", player);
        balancer.tick();
        assertEquals(List.of("b"), sent, "and a restarted one must start from scratch");
    }

    // --------------------------------------------------------------------- //

    private ServerPlayer playerAtOrigin() {
        final ServerPlayer player = mock(ServerPlayer.class);
        when(player.distanceToSqr(any(Vec3.class))).thenAnswer(invocation -> invocation.<Vec3>getArgument(0).lengthSqr());
        players.add(player);
        return player;
    }

    // --------------------------------------------------------------------- //

    private final class TestBalancer extends StreamingLoadBalancer<String, TestEntry> {
        TestBalancer(final int averageMaxBytesPerSecond, final int maxSendsPerTick) {
            super(() -> averageMaxBytesPerSecond, maxSendsPerTick, 16, now::get);
        }

        TestEntry entry(final String key) {
            final TestEntry entry = getEntry(key);
            assertNotNull(entry, "no entry for " + key);
            return entry;
        }

        @Override
        protected TestEntry createEntry(final String key) {
            return new TestEntry(this, key);
        }
    }

    private final class TestEntry extends StreamingLoadBalancer.Entry {
        private final TestBalancer balancer;
        private final String key;
        private final List<List<ServerPlayer>> recipients = new ArrayList<>();
        private boolean ready = true;
        private int payloadSize = 1;
        private Vec3 position = Vec3.ZERO;
        private int playersAdded;

        TestEntry(final TestBalancer balancer, final String key) {
            this.balancer = balancer;
            this.key = key;
        }

        @Override
        protected Vec3 getPosition() {
            return position;
        }

        @Override
        protected boolean isReady() {
            return ready;
        }

        @Override
        protected void send(final List<ServerPlayer> recipients) {
            sent.add(key);
            this.recipients.add(recipients);
            balancer.consumeBudget(payloadSize * recipients.size());
        }

        @Override
        protected void onPlayerAdded() {
            playersAdded++;
        }
    }
}
