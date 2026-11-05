/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.vxlan;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.inet.InternetAdapter;
import li.cil.oc2.common.inet.InternetConnection;
import li.cil.oc2.common.inet.util.AddressParseException;
import li.cil.oc2.common.inet.util.InternetUtils;
import li.cil.oc2.common.util.ThrottledLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VXLANManager {
    private static final int HEADER_SIZE = 8;
    private static final int FLAGS_VNI_VALID = 0x08;
    private static final int ETHERNET_HEADER_SIZE = 14;

    private static final Logger LOGGER = LogManager.getLogger(VXLANManager.class);
    private static final ThrottledLogger THROTTLED_LOGGER = new ThrottledLogger(LOGGER, Duration.ofMinutes(1));

    // --------------------------------------------------------------------- //

    @Nullable
    private static VXLANManager instance;

    private final Set<InetAddress> allowedPeers;
    private final List<VXLANConnection> connections = new CopyOnWriteArrayList<>();
    private final DatagramChannel channel;
    private final ByteBuffer sendBuffer = ByteBuffer.allocate(HEADER_SIZE + InternetConnection.MAX_FRAME_SIZE);
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(HEADER_SIZE + InternetConnection.MAX_FRAME_SIZE + 1); // +1 to detect oversize frames

    // --------------------------------------------------------------------- //

    private VXLANManager() throws IOException, AddressParseException {
        final InetAddress bindAddress = InternetUtils.toJavaInetAddress(InternetUtils.parseIpv4Address(Config.vxlanBindAddress));

        final Set<InetAddress> parsedAllowedPeers = new HashSet<>();
        for (final String peer : Config.vxlanAllowedPeers) {
            try {
                parsedAllowedPeers.add(InternetUtils.toJavaInetAddress(InternetUtils.parseIpv4Address(peer)));
            } catch (final AddressParseException e) {
                LOGGER.warn("Ignoring invalid VXLAN allowed peer '{}'.", peer, e);
            }
        }
        allowedPeers = parsedAllowedPeers;

        channel = DatagramChannel.open(StandardProtocolFamily.INET);
        try {
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(bindAddress, Config.vxlanBindPort));
        } catch (final IOException | RuntimeException e) {
            closeQuietly(channel);
            throw e;
        }
    }

    // --------------------------------------------------------------------- //

    public static void initialize() {
        LifecycleEvent.SERVER_BEFORE_START.register(server -> start());
        LifecycleEvent.SERVER_STOPPING.register(server -> stop());
        TickEvent.SERVER_PRE.register(server -> getInstance().ifPresent(VXLANManager::onServerTick));
    }

    public static void start() {
        if (instance != null) {
            LOGGER.warn("VXLAN manager already running.");
            return;
        }
        if (!Config.vxlanEnabled) {
            LOGGER.info("VXLAN is disabled.");
            return;
        }
        try {
            instance = new VXLANManager();

            LOGGER.info("VXLAN is enabled and bound to {}:{}.", Config.vxlanBindAddress, Config.vxlanBindPort);
            if (Config.vxlanAllowedPeers.isEmpty()) {
                LOGGER.warn("No allowed peers configured for VXLAN; no VXLAN traffic will be exchanged.");
            } else {
                LOGGER.info("Computers can reach the network provided by VXLAN, "
                    + "subject to the configured allowed peers: {}", Config.vxlanAllowedPeers);
            }
        } catch (final IOException | AddressParseException e) {
            LOGGER.error("Failed to start VXLAN manager; VXLAN is unavailable.", e);
        }
    }

    public static void stop() {
        final VXLANManager manager = instance;
        instance = null;
        if (manager != null) {
            manager.shutdown();
        }
    }

    public static Optional<VXLANManager> getInstance() {
        return Optional.ofNullable(instance);
    }

    public Optional<VXLANConnection> connect(final InternetAdapter adapter, final String peer, final int port, final int vni) {
        if (port < 1 || port > 65535) {
            return Optional.empty();
        }
        if (vni < 0 || vni > 16777215) {
            return Optional.empty();
        }

        final InetAddress address;
        try {
            address = InternetUtils.toJavaInetAddress(InternetUtils.parseIpv4Address(peer));
        } catch (final AddressParseException e) {
            return Optional.empty();
        }
        final InetSocketAddress peerAddress = new InetSocketAddress(address, port);

        final VXLANConnection connection = new VXLANConnection(adapter, peerAddress, vni);
        connections.add(connection);
        return Optional.of(connection);
    }

    public void onServerTick() {
        connections.removeIf(VXLANConnection::isStopped);
        for (final VXLANConnection connection : connections) {
            sendFrames(connection);
        }
        receiveFrames();
    }

    private void shutdown() {
        closeQuietly(channel);

        for (final VXLANConnection connection : connections) {
            connection.stop();
        }
        connections.clear();
    }

    private static void closeQuietly(final DatagramChannel channel) {
        try {
            channel.close();
        } catch (final IOException e) {
            LOGGER.warn("Failed to close VXLAN channel.", e);
        }
    }

    // --------------------------------------------------------------------- //

    private void sendFrames(final VXLANConnection connection) {
        final InetSocketAddress peer = connection.getPeer();
        byte[] frame;
        for (int i = 0; i < InternetConnection.FRAME_QUEUE_SIZE
            && (frame = connection.getAdapter().readInternetFrame()) != null; i++) {
            if (!allowedPeers.contains(peer.getAddress())) {
                THROTTLED_LOGGER.info("VXLAN peer {} is not an allowed peer; dropping frames.", peer.getAddress().getHostAddress());
                continue;
            }
            if (frame.length > InternetConnection.MAX_FRAME_SIZE) {
                continue;
            }

            sendBuffer.clear();
            sendBuffer.putInt(FLAGS_VNI_VALID << 24); // flags byte + 3 reserved bytes
            sendBuffer.putInt(connection.getVni() << 8); // 24-bit VNI + 1 reserved byte
            sendBuffer.put(frame);
            sendBuffer.flip();
            try {
                channel.send(sendBuffer, peer);
            } catch (final IOException e) {
                THROTTLED_LOGGER.error("Failed to send VXLAN datagram.", e);
            }
        }
    }

    private void receiveFrames() {
        // Fixed cap so a flood can't hold up the tick; the rest waits in the OS buffer.
        final int maxDatagrams = InternetConnection.FRAME_QUEUE_SIZE * Math.max(1, connections.size());
        for (int i = 0; i < maxDatagrams; i++) {
            receiveBuffer.clear();
            final InetSocketAddress source;
            try {
                source = (InetSocketAddress) channel.receive(receiveBuffer);
            } catch (final IOException e) {
                THROTTLED_LOGGER.error("Failed to receive VXLAN datagram.", e);
                return;
            }
            if (source == null) {
                return; // nothing pending
            }
            handleDatagram(source);
        }
    }

    private void handleDatagram(final InetSocketAddress source) {
        // 1. Allowlist first, before parsing anything. Drop silently: the volume is attacker-controlled.
        if (!allowedPeers.contains(source.getAddress())) {
            return;
        }

        receiveBuffer.flip();
        final int length = receiveBuffer.remaining();
        if (length < HEADER_SIZE + ETHERNET_HEADER_SIZE || length > HEADER_SIZE + InternetConnection.MAX_FRAME_SIZE) {
            return;
        }
        if ((receiveBuffer.get(0) & FLAGS_VNI_VALID) == 0) { // reserved bits are ignored per RFC 7348
            return;
        }
        final int vni = receiveBuffer.getInt(4) >>> 8;

        // 2. Forward to all connections with matching VNI and peer address.
        for (final VXLANConnection connection : connections) {
            if (connection.getVni() == vni && connection.getPeer().getAddress().equals(source.getAddress())) {
                final byte[] frame = new byte[length - HEADER_SIZE];
                receiveBuffer.get(HEADER_SIZE, frame);
                connection.getAdapter().writeInternetFrame(frame);
            }
        }
    }
}
