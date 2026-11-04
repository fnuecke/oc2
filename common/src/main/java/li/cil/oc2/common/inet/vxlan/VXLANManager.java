package li.cil.oc2.common.inet.vxlan;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.inet.InternetAdapter;

public class VXLANManager {
    private static final Logger LOGGER = LogManager.getLogger(VXLANManager.class);

    // --------------------------------------------------------------------- //

    @Nullable
    private static VXLANManager instance;

    private final List<VXLANConnection> connections = new CopyOnWriteArrayList<>();

    // --------------------------------------------------------------------- //

    private VXLANManager() {
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
        // try {
            instance = new VXLANManager();
        // } catch (final IOException e) {
        //     LOGGER.error("Failed to start VXLAN manager; VXLAN is unavailable.", e);
        //     return;
        // }
        LOGGER.info("VXLAN is enabled. Computers can reach the network this server sits on, "
            + "subject to the configured allowed peers: {}", Config.vxlanAllowedPeers);
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

    public VXLANConnection connect(final InternetAdapter adapter, final String peer, final int port, final int vni) {
        final VXLANConnection connection = new VXLANConnection(adapter, peer, port, vni);
        connections.add(connection);
        return connection;
    }

    public void onServerTick() {
            // Implementation for server tick handling
        }

    private void shutdown() {
        
    }
}
