/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import li.cil.oc2.api.capabilities.NetworkInterface;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

public final class CompoundNetworkInterfaceTests {
    @Test
    public void sideWithoutAnyCardHasNoInterface() {
        assertNull(CompoundNetworkInterface.of(List.of(), Direction.UP));
        assertNull(CompoundNetworkInterface.of(List.of(new Card(Direction.NORTH)), Direction.UP));
    }

    @Test
    public void loneCardIsItsOwnInterface() {
        final Card card = new Card(Direction.UP);

        assertSame(card.networkInterface, CompoundNetworkInterface.of(List.of(card), Direction.UP));
    }

    @Test
    public void everyCardOnTheSideGetsAnIncomingFrame() {
        final Card first = new Card(Direction.UP);
        final Card second = new Card(Direction.UP);
        final Card elsewhere = new Card(Direction.NORTH);

        final NetworkInterface networkInterface = CompoundNetworkInterface.of(List.of(first, second, elsewhere), Direction.UP);
        assertNotNull(networkInterface);
        final byte[] frame = {1};
        networkInterface.writeEthernetFrame(networkInterface, frame, 8);

        assertEquals(List.of(new Delivery(networkInterface, frame, 8)), first.received, "the frame, its source and its remaining hops are passed on as they came");
        assertEquals(1, second.received.size());
        assertEquals(0, elsewhere.received.size(), "a card configured for another side is not on this wire");
    }

    @Test
    public void cardHearsWhatTheOthersOnItsSideSend() {
        final Card first = new Card(Direction.UP);
        final Card second = new Card(Direction.UP);
        final Card elsewhere = new Card(Direction.NORTH);
        final byte[] frame = {1};
        first.queue(frame);

        final NetworkInterface networkInterface = CompoundNetworkInterface.of(List.of(first, second, elsewhere), Direction.UP);
        assertNotNull(networkInterface);

        assertArrayEquals(frame, networkInterface.readEthernetFrame(), "it still goes out to the segment");
        assertEquals(List.of(new Delivery(first.networkInterface, frame, 1)), second.received, "one side is one wire");
        assertEquals(0, first.received.size(), "but a card does not hear itself");
        assertEquals(0, elsewhere.received.size());
    }

    @Test
    public void outgoingFramesAreTakenFromTheCardsInTurn() {
        final Card first = new Card(Direction.UP);
        final Card second = new Card(Direction.UP);
        first.queue(new byte[]{1});
        first.queue(new byte[]{2});
        second.queue(new byte[]{3});

        final NetworkInterface networkInterface = CompoundNetworkInterface.of(List.of(first, second), Direction.UP);
        assertNotNull(networkInterface);

        assertArrayEquals(new byte[]{1}, networkInterface.readEthernetFrame());
        assertArrayEquals(new byte[]{3}, networkInterface.readEthernetFrame(), "a busy card does not get to send twice in a row");
        assertArrayEquals(new byte[]{2}, networkInterface.readEthernetFrame(), "what the other one had still follows");
        assertNull(networkInterface.readEthernetFrame(), "and then the side is quiet");
    }

    // --------------------------------------------------------------------- //

    private record Delivery(NetworkInterface source, byte[] frame, int timeToLive) {
    }

    private static final class Card implements CapabilityProvider {
        private final Direction side;
        private final NetworkInterface networkInterface = new CardInterface();
        private final Queue<byte[]> pending = new ArrayDeque<>();
        private final List<Delivery> received = new ArrayList<>();

        private Card(final Direction side) {
            this.side = side;
        }

        private void queue(final byte[] frame) {
            pending.add(frame);
        }

        @Nullable
        @SuppressWarnings("unchecked")
        @Override
        public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
            if (capability == Capabilities.NETWORK_INTERFACE && side == this.side) {
                return (T) networkInterface;
            }

            return null;
        }

        private final class CardInterface implements NetworkInterface {
            @Nullable
            @Override
            public byte[] readEthernetFrame() {
                return pending.poll();
            }

            @Override
            public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
                received.add(new Delivery(source, frame, timeToLive));
            }
        }
    }
}
