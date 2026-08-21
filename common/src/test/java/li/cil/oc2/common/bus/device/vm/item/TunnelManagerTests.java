/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import li.cil.oc2.api.capabilities.NetworkInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TunnelManagerTests {
    @BeforeEach
    public void setupEach() {
        tunnels().clear();
    }

    @Test
    public void endpointsOnTheSameTunnelShareIt() {
        final UUID id = UUID.randomUUID();

        NetworkTunnelDevice.TunnelManager.registerEndpoint(id, new NullNetworkInterface());
        NetworkTunnelDevice.TunnelManager.registerEndpoint(id, new NullNetworkInterface());

        assertEquals(1, tunnels().size());
        assertEquals(2, tunnels().get(id).size());
    }

    @Test
    public void secondTunnelCanBeOpenedWhileTheFirstOneSitsEmpty() {
        final NetworkInterface endpoint = new NullNetworkInterface();
        NetworkTunnelDevice.TunnelManager.registerEndpoint(UUID.randomUUID(), endpoint);
        NetworkTunnelDevice.TunnelManager.unregisterEndpoint(endpoint);

        assertDoesNotThrow(() ->
            NetworkTunnelDevice.TunnelManager.registerEndpoint(UUID.randomUUID(), new NullNetworkInterface()));
    }

    @Test
    public void unregisteringTheLastEndpointDropsTheTunnel() {
        final UUID id = UUID.randomUUID();
        final NetworkInterface endpoint = new NullNetworkInterface();

        NetworkTunnelDevice.TunnelManager.registerEndpoint(id, endpoint);
        NetworkTunnelDevice.TunnelManager.unregisterEndpoint(endpoint);

        assertTrue(tunnels().isEmpty(), "an emptied tunnel should not linger until the next tick");
    }

    @Test
    public void unregisteringOneOfTwoEndpointsKeepsTheTunnel() {
        final UUID id = UUID.randomUUID();
        final NetworkInterface stays = new NullNetworkInterface();

        NetworkTunnelDevice.TunnelManager.registerEndpoint(id, stays);
        NetworkTunnelDevice.TunnelManager.registerEndpoint(id, new NullNetworkInterface());
        NetworkTunnelDevice.TunnelManager.unregisterEndpoint(stays);

        assertEquals(1, tunnels().size());
        assertEquals(1, tunnels().get(id).size());
    }

    // --------------------------------------------------------------------- //

    @SuppressWarnings("unchecked")
    private static Map<UUID, ? extends java.util.Collection<NetworkInterface>> tunnels() {
        // Only used here, so let's just grab it with reflection...
        try {
            final Field field = NetworkTunnelDevice.TunnelManager.class.getDeclaredField("TUNNELS");
            field.setAccessible(true);
            return (Map<UUID, ? extends java.util.Collection<NetworkInterface>>) field.get(null);
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("could not read the tunnel map", e);
        }
    }

    private static final class NullNetworkInterface implements NetworkInterface {
        @Nullable
        @Override
        public byte[] readEthernetFrame() {
            return null;
        }

        @Override
        public void writeEthernetFrame(final NetworkInterface source, final byte[] frame, final int timeToLive) {
        }
    }
}
