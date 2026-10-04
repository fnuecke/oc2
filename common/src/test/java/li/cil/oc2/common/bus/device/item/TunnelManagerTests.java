/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.capabilities.NetworkInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TunnelManagerTests {
    private final List<NetworkInterface> endpoints = new ArrayList<>();

    @AfterEach
    public void teardownEach() {
        endpoints.forEach(NetworkTunnelDevice.TunnelManager::unregisterEndpoint);
    }

    @Test
    public void framesArriveAtTheOtherEndpointsOfTheSameTunnel() {
        final UUID id = UUID.randomUUID();
        final TestNetworkInterface sender = new TestNetworkInterface();
        final TestNetworkInterface receiver = new TestNetworkInterface();
        final TestNetworkInterface stranger = new TestNetworkInterface();

        register(id, sender);
        register(id, receiver);
        register(UUID.randomUUID(), stranger);

        final byte[] frame = {1, 2, 3};
        sender.outgoing.add(frame);
        tick();

        assertEquals(1, receiver.received.size());
        assertArrayEquals(frame, receiver.received.get(0));
        assertTrue(sender.received.isEmpty(), "frames must not echo back to their sender");
        assertTrue(stranger.received.isEmpty(), "frames must not leak into other tunnels");
    }

    @Test
    public void secondTunnelCanBeOpenedWhileTheFirstOneSitsEmpty() {
        final NetworkInterface endpoint = new TestNetworkInterface();
        register(UUID.randomUUID(), endpoint);
        NetworkTunnelDevice.TunnelManager.unregisterEndpoint(endpoint);

        assertDoesNotThrow(() ->
            register(UUID.randomUUID(), new TestNetworkInterface()));
    }

    @Test
    public void unregisteredEndpointsGetNothing() {
        final UUID id = UUID.randomUUID();
        final TestNetworkInterface sender = new TestNetworkInterface();
        final TestNetworkInterface stays = new TestNetworkInterface();
        final TestNetworkInterface leaves = new TestNetworkInterface();

        register(id, sender);
        register(id, stays);
        register(id, leaves);
        NetworkTunnelDevice.TunnelManager.unregisterEndpoint(leaves);

        sender.outgoing.add(new byte[]{1, 2, 3});
        tick();

        assertEquals(1, stays.received.size(), "the remaining endpoints should keep the tunnel");
        assertTrue(leaves.received.isEmpty());
    }

    // --------------------------------------------------------------------- //

    private void register(final UUID id, final NetworkInterface endpoint) {
        endpoints.add(endpoint);
        NetworkTunnelDevice.TunnelManager.registerEndpoint(id, endpoint);
    }

    private static void tick() {
        NetworkTunnelDevice.TunnelManager.pumpMessages();
    }

    // --------------------------------------------------------------------- //

    private static final class TestNetworkInterface implements NetworkInterface {
        private final Queue<byte[]> outgoing = new ArrayDeque<>();
        private final List<byte[]> received = new ArrayList<>();

        @Nullable
        @Override
        public byte[] readEthernetFrame() {
            return outgoing.poll();
        }

        @Override
        public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
            received.add(frame);
        }
    }
}
