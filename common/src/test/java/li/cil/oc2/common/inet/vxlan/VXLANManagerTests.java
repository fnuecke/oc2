/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.vxlan;

import li.cil.oc2.common.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VXLANManagerTests {
    private final boolean originalEnabled = Config.vxlanEnabled;
    private final String originalBindAddress = Config.vxlanBindAddress;
    private final int originalBindPort = Config.vxlanBindPort;
    private final List<String> originalAllowedPeers = Config.vxlanAllowedPeers;

    @AfterEach
    public void tearDown() {
        VXLANManager.stop();
        Config.vxlanEnabled = originalEnabled;
        Config.vxlanBindAddress = originalBindAddress;
        Config.vxlanBindPort = originalBindPort;
        Config.vxlanAllowedPeers = originalAllowedPeers;
    }

    @Test
    public void disabledManagerDoesNotBind() {
        Config.vxlanEnabled = false;

        VXLANManager.start();

        assertFalse(VXLANManager.getInstance().isPresent());
    }

    @Test
    public void managerBindsWhenEnabled() throws IOException {
        configureEnabledManager();

        VXLANManager.start();

        assertTrue(VXLANManager.getInstance().isPresent());
    }

    @Test
    public void managerDoesNotStartWhenPortIsInUse() throws IOException {
        final int port;
        try (DatagramChannel occupyingChannel = openLoopbackChannel()) {
            port = ((InetSocketAddress) occupyingChannel.getLocalAddress()).getPort();
            configureEnabledManager();
            Config.vxlanBindPort = port;

            VXLANManager.start();

            assertFalse(VXLANManager.getInstance().isPresent());
        }
    }

    @Test
    public void managerDoesNotStartWithBadBindAddress() {
        configureEnabledManager();
        Config.vxlanBindAddress = "not-an-address";

        VXLANManager.start();

        assertFalse(VXLANManager.getInstance().isPresent());
    }

    @Test
    public void stopFreesTheBoundPort() throws IOException {
        configureEnabledManager();
        final int port = Config.vxlanBindPort;

        VXLANManager.start();
        VXLANManager.stop();

        try (DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)) {
            channel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "garbage"})
    public void connectRejectsBlankAndGarbagePeers(final String peer) {
        final VXLANManager manager = startManager();

        assertFalse(manager.connect(null, peer, 4789, 0).isPresent());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 65536})
    public void connectRejectsInvalidPorts(final int port) {
        final VXLANManager manager = startManager();

        assertFalse(manager.connect(null, "127.0.0.1", port, 0).isPresent());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 16777216})
    public void connectRejectsInvalidVnis(final int vni) {
        final VXLANManager manager = startManager();

        assertFalse(manager.connect(null, "127.0.0.1", 4789, vni).isPresent());
    }

    @Test
    public void connectAcceptsValidInput() {
        final VXLANManager manager = startManager();

        final Optional<VXLANConnection> connection = manager.connect(null, "127.0.0.1", 4789, 16777215);

        assertTrue(connection.isPresent());
    }

    private VXLANManager startManager() {
        configureEnabledManager();
        VXLANManager.start();
        return VXLANManager.getInstance().orElseThrow();
    }

    private void configureEnabledManager() {
        Config.vxlanEnabled = true;
        Config.vxlanBindAddress = "127.0.0.1";
        Config.vxlanBindPort = findFreePort();
        Config.vxlanAllowedPeers = new ArrayList<>();
    }

    private static int findFreePort() {
        try (DatagramChannel channel = openLoopbackChannel()) {
            return ((InetSocketAddress) channel.getLocalAddress()).getPort();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to find a free UDP port.", e);
        }
    }

    private static DatagramChannel openLoopbackChannel() throws IOException {
        final DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
        channel.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        return channel;
    }
}
