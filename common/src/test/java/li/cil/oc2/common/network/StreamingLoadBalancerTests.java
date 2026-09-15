/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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
    public void aSingleWatcherCostsASourceNothing() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        balancer.update("a", playerAtOrigin());

        for (int i = 0; i < 20; i++) {
            balancer.tick();
        }

        assertEquals(20, sent.size(), "a source nobody else watches must send on every tick");
    }

    @Test
    public void eachWatcherPastTheFirstCostsARound() {
        final TestBalancer balancer = new TestBalancer(PLENTY_OF_BUDGET, 1);
        balancer.update("one", playerAtOrigin());
        balancer.update("two", playerAtOrigin());
        balancer.update("two", playerAtOrigin());

        for (int i = 0; i < 30; i++) {
            balancer.tick();
        }

        final int oneCount = Collections.frequency(sent, "one");
        final int twoCount = Collections.frequency(sent, "two");
        assertEquals(30, oneCount + twoCount, "a turn was taken every tick");
        assertEquals(2 * twoCount, oneCount,
            "the second watcher costs a round, so that source gets half the turns: " + sent);
    }

    @Test
    public void everyWatcherIsChargedForWhatItReceives() {
        // Budget caps at 225, and 22 comes back per tick, so nothing lands exactly on zero.
        final TestBalancer balancer = new TestBalancer(450, 1);
        balancer.update("a", playerAtOrigin());
        balancer.update("a", playerAtOrigin());
        balancer.entry("a").payloadSize = 100; // 200 a send, with two watchers to send to

        balancer.tick(); // sends
        balancer.tick(); // the second watcher's skipped round
        balancer.tick(); // sends again, overdrawing the budget
        assertEquals(2, sent.size(), "the budget covers two sends before it goes negative");
        assertEquals(2, balancer.entry("a").recipients.get(0).size(), "both watchers were sent to");

        for (int tick = 4; tick <= 9; tick++) {
            balancer.tick();
        }
        assertEquals(2, sent.size(), "two watchers overdrew it by 131, and a skipped round follows");

        balancer.tick();
        assertEquals(3, sent.size(), "and then sending resumes");
    }

    @Test
    public void pausingForBudgetKeepsEverySourceInTheRotation() {
        final TestBalancer balancer = new TestBalancer(450, 1);
        final ServerPlayer player = playerAtOrigin();
        for (final String key : List.of("a", "b", "c")) {
            balancer.update(key, player);
            balancer.entry(key).payloadSize = 150; // overdraws repeatedly, so sending keeps pausing
        }

        for (int i = 0; i < 60; i++) {
            balancer.tick();
        }

        final int a = Collections.frequency(sent, "a");
        final int b = Collections.frequency(sent, "b");
        final int c = Collections.frequency(sent, "c");
        assertTrue(a > 0 && b > 0 && c > 0, "a budget pause must not starve a source: " + sent);
        assertTrue(Math.max(a, Math.max(b, c)) - Math.min(a, Math.min(b, c)) <= 1,
            "and the rotation must resume where it left off, not restart: " + sent);
    }

    @Test
    public void aChargeLandingAfterTheSendStillPausesIt() {
        // Projectors charge from their encoder thread, well after the send was decided.
        final TestBalancer balancer = new TestBalancer(450, 1);
        balancer.update("a", playerAtOrigin());
        balancer.entry("a").payloadSize = 0;

        balancer.tick();
        assertEquals(1, sent.size());

        balancer.consumeBudget(1000); // the encode finishes and settles up
        balancer.tick();
        assertEquals(1, sent.size(), "a charge that lands late still has to stop the next send");
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
        players.add(player);
        return player;
    }

    // --------------------------------------------------------------------- //

    private final class TestBalancer extends StreamingLoadBalancer<String, TestEntry> {
        TestBalancer(final int averageMaxBytesPerSecond, final int maxSendsPerTick) {
            super(() -> averageMaxBytesPerSecond, maxSendsPerTick, now::get);
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
        private int playersAdded;

        TestEntry(final TestBalancer balancer, final String key) {
            this.balancer = balancer;
            this.key = key;
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
