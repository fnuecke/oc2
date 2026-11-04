/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.vxlan;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.inet.InternetAdapter;
import li.cil.oc2.common.inet.util.AddressParseException;
import li.cil.oc2.common.inet.util.InternetUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VXLANManager {
    private static final Logger LOGGER = LogManager.getLogger(VXLANManager.class);

    // --------------------------------------------------------------------- //

    @Nullable
    private static VXLANManager instance;

    private final Set<InetAddress> allowedPeers;
    private final List<VXLANConnection> connections = new CopyOnWriteArrayList<>();
    private final DatagramChannel channel;

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
            VXLANManager newinstance = new VXLANManager();

            InetSocketAddress localAddress = (InetSocketAddress) newinstance.channel.getLocalAddress();       

            LOGGER.info("VXLAN is enabled and bound to {}:{}.", localAddress.getAddress().getHostAddress(), localAddress.getPort());
            LOGGER.info("Computers can reach the network provided by VXLAN, "
                + "subject to the configured allowed peers: {}", Config.vxlanAllowedPeers);

            instance = newinstance;
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
        if (port < 0 || port > 65535) {
            return Optional.empty();
        }
        if (vni < 0 || vni > 16777215) {
            return Optional.empty();
        }
        try {
            InternetUtils.toJavaInetAddress(InternetUtils.parseIpv4Address(peer));
        } catch (final AddressParseException e) {
            return Optional.empty();
        }

        final InetSocketAddress peerAddress = new InetSocketAddress(peer, port);
        final VXLANConnection connection = new VXLANConnection(adapter, peerAddress, vni);
        connections.add(connection);
        return Optional.of(connection);
    }

    public void onServerTick() {
        connections.removeIf(VXLANConnection::isStopped);
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
}
