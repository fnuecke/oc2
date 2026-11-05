/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.vxlan;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.inet.InternetAdapter;
import javax.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static li.cil.oc2.common.inet.InternetConnection.MAX_FRAME_SIZE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    public void encodesFrameCorrectly() throws IOException {
        final StubAdapter adapter = new StubAdapter();
        final VXLANManager manager = startManager("127.0.0.1");

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(1000);
            final VXLANConnection connection = manager.connect(adapter, "127.0.0.1", peer.getLocalPort(), 100).orElseThrow();
            adapter.toInternet.add(new byte[]{1, 2, 3});

            manager.onServerTick();

            final byte[] received = new byte[11];
            final DatagramPacket packet = new DatagramPacket(received, received.length);
            peer.receive(packet);

            assertArrayEquals(new byte[]{8, 0, 0, 0, 0, 0, 100, 0, 1, 2, 3}, packet.getData());
            connection.stop();
        }
    }

    @Test
    public void deniedPeerDoesNotReceiveFrame() throws IOException {
        final StubAdapter adapter = new StubAdapter();
        final VXLANManager manager = startManager();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(100);
            manager.connect(adapter, "127.0.0.1", peer.getLocalPort(), 100).orElseThrow();
            adapter.toInternet.add(new byte[]{1, 2, 3});

            manager.onServerTick();

            assertThrows(SocketTimeoutException.class, () -> peer.receive(new DatagramPacket(new byte[11], 11)));
            assertTrue(adapter.toInternet.isEmpty());
        }
    }

    @Test
    public void oversizedFrameIsDropped() throws IOException {
        final StubAdapter adapter = new StubAdapter();
        final VXLANManager manager = startManager("127.0.0.1");

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(100);
            manager.connect(adapter, "127.0.0.1", peer.getLocalPort(), 100).orElseThrow();
            adapter.toInternet.add(new byte[MAX_FRAME_SIZE + 1]);

            manager.onServerTick();

            assertThrows(SocketTimeoutException.class, () -> peer.receive(new DatagramPacket(new byte[11], 11)));
        }
    }

    @Test
    public void stoppedConnectionDoesNotReceiveFrame() throws IOException {
        final StubAdapter adapter = new StubAdapter();
        final VXLANManager manager = startManager("127.0.0.1");

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(100);
            final VXLANConnection connection = manager.connect(adapter, "127.0.0.1", peer.getLocalPort(), 100).orElseThrow();
            connection.stop();
            adapter.toInternet.add(new byte[]{1, 2, 3});

            manager.onServerTick();

            assertThrows(SocketTimeoutException.class, () -> peer.receive(new DatagramPacket(new byte[11], 11)));
        }
    }

    @Test
    public void receivesDatagramFromLoopback() throws IOException {
        final byte[] frame = testFrame();
        final StubAdapter adapter = new StubAdapter();
        final VXLANManager manager = startManager("127.0.0.1");
        manager.connect(adapter, "127.0.0.1", 4789, 100).orElseThrow();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.send(new DatagramPacket(vxlanDatagram(100, true, frame), 8 + frame.length,
                InetAddress.getLoopbackAddress(), Config.vxlanBindPort));

            tickUntilDelivered(manager, adapter);

            assertEquals(1, adapter.fromInternet.size());
            assertArrayEquals(frame, adapter.fromInternet.get(0));
        }
    }

    @Test
    public void dropsDatagramWithWrongVni() throws IOException {
        assertBadDatagramDropped(vxlanDatagram(101, true, testFrame()));
    }

    @Test
    public void dropsDatagramWithUnsetIFlag() throws IOException {
        assertBadDatagramDropped(vxlanDatagram(100, false, testFrame()));
    }

    @Test
    public void dropsDatagramShorterThanEthernetFrame() throws IOException {
        assertBadDatagramDropped(new byte[8 + 13]);
    }

    @Test
    public void dropsDatagramLargerThanMaximumFrame() throws IOException {
        assertBadDatagramDropped(new byte[8 + MAX_FRAME_SIZE + 1]);
    }

    @Test
    public void deniedSourceDoesNotDeliverDatagram() throws IOException {
        final StubAdapter adapter = new StubAdapter();
        final VXLANManager manager = startManager();
        manager.connect(adapter, "127.0.0.1", 4789, 100).orElseThrow();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.send(new DatagramPacket(vxlanDatagram(100, true, testFrame()), 8 + 14,
                InetAddress.getLoopbackAddress(), Config.vxlanBindPort));

            tickForDuration(manager, 200);

            assertTrue(adapter.fromInternet.isEmpty());
        }
    }

    @Test
    public void ignoresSourcePort() throws IOException {
        final byte[] frame = testFrame();
        final StubAdapter adapter = new StubAdapter();
        final VXLANManager manager = startManager("127.0.0.1");
        manager.connect(adapter, "127.0.0.1", 4789, 100).orElseThrow();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.send(new DatagramPacket(vxlanDatagram(100, true, frame), 8 + frame.length,
                InetAddress.getLoopbackAddress(), Config.vxlanBindPort));

            tickUntilDelivered(manager, adapter);

            assertEquals(1, adapter.fromInternet.size());
            assertArrayEquals(frame, adapter.fromInternet.get(0));
        }
    }

    @Test
    public void echoesFrameRoundTrip() throws IOException {
        final byte[] frame = testFrame();
        final StubAdapter adapter = new StubAdapter();
        final VXLANManager manager = startManager("127.0.0.1");
        manager.connect(adapter, "127.0.0.1", 4789, 100).orElseThrow();
        adapter.toInternet.add(frame);

        try (DatagramSocket peer = new DatagramSocket(4789, InetAddress.getLoopbackAddress())) {
            peer.setSoTimeout(1000);
            manager.onServerTick();

            final byte[] encoded = new byte[8 + MAX_FRAME_SIZE];
            final DatagramPacket packet = new DatagramPacket(encoded, encoded.length);
            peer.receive(packet);
            final byte[] received = Arrays.copyOf(packet.getData(), packet.getLength());

            peer.send(new DatagramPacket(received, received.length,
                InetAddress.getLoopbackAddress(), Config.vxlanBindPort));
            tickUntilDelivered(manager, adapter);

            assertEquals(1, adapter.fromInternet.size());
            assertArrayEquals(frame, adapter.fromInternet.get(0));
        }
    }

    private void assertBadDatagramDropped(final byte[] badDatagram) throws IOException {
        final byte[] frame = testFrame();
        final StubAdapter adapter = new StubAdapter();
        final VXLANManager manager = startManager("127.0.0.1");
        manager.connect(adapter, "127.0.0.1", 4789, 100).orElseThrow();

        try (DatagramSocket peer = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            peer.send(new DatagramPacket(badDatagram, badDatagram.length,
                InetAddress.getLoopbackAddress(), Config.vxlanBindPort));
            peer.send(new DatagramPacket(vxlanDatagram(100, true, frame), 8 + frame.length,
                InetAddress.getLoopbackAddress(), Config.vxlanBindPort));

            tickUntilDelivered(manager, adapter);

            assertEquals(1, adapter.fromInternet.size());
            assertArrayEquals(frame, adapter.fromInternet.get(0));
        }
    }

    private static void tickUntilDelivered(final VXLANManager manager, final StubAdapter adapter) {
        final long deadline = System.nanoTime() + 1_000_000_000L;
        while (adapter.fromInternet.isEmpty() && System.nanoTime() < deadline) {
            manager.onServerTick();
            sleepBriefly();
        }
    }

    private static void tickForDuration(final VXLANManager manager, final long durationMillis) {
        final long deadline = System.nanoTime() + durationMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            manager.onServerTick();
            sleepBriefly();
        }
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(1);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for UDP loopback traffic.", e);
        }
    }

    private static byte[] vxlanDatagram(final int vni, final boolean vniValid, final byte[] frame) {
        final byte[] datagram = new byte[8 + frame.length];
        datagram[0] = (byte) (vniValid ? 8 : 0);
        datagram[4] = (byte) (vni >>> 16);
        datagram[5] = (byte) (vni >>> 8);
        datagram[6] = (byte) vni;
        System.arraycopy(frame, 0, datagram, 8, frame.length);
        return datagram;
    }

    private static byte[] testFrame() {
        final byte[] frame = new byte[14];
        for (int i = 0; i < frame.length; i++) {
            frame[i] = (byte) i;
        }
        return frame;
    }

    private VXLANManager startManager(final String... allowedPeers) {
        configureEnabledManager(allowedPeers);
        VXLANManager.start();
        return VXLANManager.getInstance().orElseThrow();
    }

    private void configureEnabledManager(final String... allowedPeers) {
        Config.vxlanEnabled = true;
        Config.vxlanBindAddress = "127.0.0.1";
        Config.vxlanBindPort = findFreePort();
        Config.vxlanAllowedPeers = Arrays.asList(allowedPeers);
    }

    private static final class StubAdapter implements InternetAdapter {
        final Deque<byte[]> toInternet = new ArrayDeque<>();
        final List<byte[]> fromInternet = new ArrayList<>();

        @Nullable
        @Override
        public byte[] readInternetFrame() {
            return toInternet.pollFirst();
        }

        @Override
        public void writeInternetFrame(final byte[] frame) {
            fromInternet.add(frame);
        }
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
