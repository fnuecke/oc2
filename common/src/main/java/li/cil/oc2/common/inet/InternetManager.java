/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.inet.l2.LinkLocalLayer;
import li.cil.oc2.common.inet.l3.AddressFilter;
import li.cil.oc2.common.inet.l3.NetworkLayer;
import li.cil.oc2.common.inet.l4.*;
import li.cil.oc2.common.inet.socket.ReachabilityProbe;
import li.cil.oc2.common.inet.socket.SocketManager;
import li.cil.oc2.common.inet.socket.SocketSessionLayer;
import li.cil.oc2.common.util.TickUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InternetManager {
    private static final int SESSIONS_PER_GATEWAY = 16;
    private static final double NEW_SESSIONS_PER_SECOND = 4;
    private static final int HOST_REFRESH_SECONDS = 300;
    private static final int SESSION_TIMEOUT_MS = 60 * 1000;
    private static final int ECHO_TIMEOUT_MS = 1000;
    private static final int ECHO_THREADS = 4;

    private static final Logger LOGGER = LogManager.getLogger();

    // --------------------------------------------------------------------- //

    @Nullable
    private static InternetManager instance;

    private final AddressFilter addressFilter;
    private final PortFilter portFilter;

    private final SessionLimits limits;
    private final SocketManager socketManager;
    private final ExecutorService internetThread;
    private final ExecutorService echoExecutor;
    private final ReachabilityProbe reachabilityProbe;

    @Nullable
    private final ScheduledExecutorService maintenanceExecutor;
    private final List<InternetConnection> connections = new CopyOnWriteArrayList<>();
    private final Queue<Runnable> commands = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean tickInFlight = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final int bytesPerTick;
    private final int sharedBytesPerTick;
    private int serverRoundRobin;
    private int workerRoundRobin;

    // --------------------------------------------------------------------- //

    private InternetManager() throws IOException {
        addressFilter = AddressFilter.withBuiltInDenials(
            Config.internetAllowedHosts, Config.internetDeniedHosts, Config.internetDenyLocalSubnets);
        portFilter = new PortFilter(Config.internetDeniedPorts);
        limits = new SessionLimits(SESSIONS_PER_GATEWAY, Config.internetSessionsTotal);
        socketManager = new SocketManager();

        internetThread = Executors.newSingleThreadExecutor(runnable -> daemon(runnable, "OC2 Internet"));
        echoExecutor = new ThreadPoolExecutor(0, ECHO_THREADS,
            30, TimeUnit.SECONDS, new SynchronousQueue<>(),
            runnable -> daemon(runnable, "OC2 Internet Probe"));
        reachabilityProbe = new ReachabilityProbe(echoExecutor, ECHO_TIMEOUT_MS);

        final int ticksPerSecond = TickUtils.toTicks(Duration.ofSeconds(1));
        bytesPerTick = Math.max(LinkLocalLayer.FRAME_SIZE, Config.internetBytesPerSecond / ticksPerSecond);
        sharedBytesPerTick = Math.max(LinkLocalLayer.FRAME_SIZE,
            Config.internetBytesPerSecondTotal / ticksPerSecond);

        if (addressFilter.needsPeriodicRefresh()) {
            maintenanceExecutor = Executors.newSingleThreadScheduledExecutor(
                runnable -> daemon(runnable, "OC2 Internet Filter"));
            maintenanceExecutor.scheduleWithFixedDelay(this::refreshFilter,
                HOST_REFRESH_SECONDS, HOST_REFRESH_SECONDS, TimeUnit.SECONDS);
        } else {
            maintenanceExecutor = null;
        }
    }

    // --------------------------------------------------------------------- //

    public static void initialize() {
        LifecycleEvent.SERVER_BEFORE_START.register(server -> start());
        LifecycleEvent.SERVER_STOPPING.register(server -> stop());
        TickEvent.SERVER_PRE.register(server -> getInstance().ifPresent(InternetManager::onServerTick));
    }

    public static void start() {
        if (instance != null) {
            LOGGER.warn("Internet manager already running.");
            return;
        }
        if (!Config.internetEnabled) {
            LOGGER.info("Internet access is disabled.");
            return;
        }
        try {
            instance = new InternetManager();
        } catch (final IOException e) {
            LOGGER.error("Failed to start internet manager; internet access is unavailable.", e);
            return;
        }
        LOGGER.info("Internet access is enabled. Computers can reach the network this server sits on, "
            + "subject to the configured address filter: {}", instance.addressFilter);
    }

    public static void stop() {
        final InternetManager manager = instance;
        instance = null;
        if (manager != null) {
            manager.shutdown();
        }
    }

    public static Optional<InternetManager> getInstance() {
        return Optional.ofNullable(instance);
    }

    public InternetConnection connect(final InternetAdapter adapter, final String originDescription) {
        final InternetConnection connection =
            new InternetConnection(adapter, buildStack(originDescription));
        connections.add(connection);
        return connection;
    }

    public void onServerTick() {
        final Budget shared = new Budget(sharedBytesPerTick);
        for (final InternetConnection connection : inRotation(serverRoundRobin++)) {
            if (connection.isStopped()) {
                if (connection.markShutdownQueued()) {
                    commands.add(connection::shutdown);
                }
            } else {
                connection.exchangeFrames(bytesPerTick, shared);
            }
        }
        connections.removeIf(InternetConnection::isStopped);

        if (!tickInFlight.compareAndSet(false, true)) {
            // The internet thread has not finished the previous pass. Don't
            // grow the executor queue.
            return;
        }
        try {
            internetThread.execute(() -> {
                try {
                    runWorkerTick();
                } finally {
                    tickInFlight.set(false);
                }
            });
        } catch (final RuntimeException e) {
            tickInFlight.set(false);
            LOGGER.error("Failed to schedule internet thread work.", e);
        }
    }

    // --------------------------------------------------------------------- //

    static final class Budget {
        private int remaining;

        Budget(final int remaining) {
            this.remaining = remaining;
        }

        boolean hasRemaining() {
            return remaining > 0;
        }

        void charge(final int bytes) {
            remaining -= bytes;
        }
    }

    private static Thread daemon(final Runnable runnable, final String name) {
        final Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }

    private LinkLocalLayer buildStack(final String originDescription) {
        final SessionLayer sessionLayer = new SocketSessionLayer(originDescription, socketManager, reachabilityProbe);
        final TransportLayer transportLayer = new TransportLayer(sessionLayer, portFilter, limits,
            new TokenBucket(SESSIONS_PER_GATEWAY, NEW_SESSIONS_PER_SECOND),
            connections::size,
            StreamSession.TcpConfig.DEFAULT,
            TimeUnit.MILLISECONDS.toNanos(SESSION_TIMEOUT_MS));
        final NetworkLayer networkLayer = new NetworkLayer(transportLayer, addressFilter);
        return new LinkLocalLayer(networkLayer);
    }

    private void runWorkerTick() {
        try {
            socketManager.poll();
            runCommands();

            final Budget shared = new Budget(sharedBytesPerTick);
            for (final InternetConnection connection : inRotation(workerRoundRobin++)) {
                connection.process(bytesPerTick, shared);
            }
        } catch (final Throwable t) {
            LOGGER.error("Uncaught error on the internet thread.", t);
        }
    }

    private List<InternetConnection> inRotation(final int rotation) {
        final List<InternetConnection> snapshot = List.copyOf(connections);
        if (snapshot.size() < 2) {
            return snapshot;
        }
        final int offset = Math.floorMod(rotation, snapshot.size());
        final List<InternetConnection> rotated = new ArrayList<>(snapshot.size());
        rotated.addAll(snapshot.subList(offset, snapshot.size()));
        rotated.addAll(snapshot.subList(0, offset));
        return rotated;
    }

    private void runCommands() {
        Runnable command;
        while ((command = commands.poll()) != null) {
            try {
                command.run();
            } catch (final Exception e) {
                LOGGER.error("Uncaught exception running internet thread command.", e);
            }
        }
    }

    private void refreshFilter() {
        try {
            addressFilter.refresh();
        } catch (final Exception e) {
            LOGGER.error("Failed to refresh internet address filter.", e);
        }
    }

    private void shutdown() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }

        if (maintenanceExecutor != null) {
            maintenanceExecutor.shutdownNow();
        }

        final List<InternetConnection> open = List.copyOf(connections);
        connections.clear();

        internetThread.execute(() -> {
            runCommands();
            for (final InternetConnection connection : open) {
                connection.shutdown();
            }
            socketManager.close();
        });
        internetThread.shutdown();

        try {
            if (!internetThread.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Internet thread did not stop in time; forcing it down.");
                internetThread.shutdownNow();
            }
        } catch (final InterruptedException e) {
            internetThread.shutdownNow();
            Thread.currentThread().interrupt();
        }

        echoExecutor.shutdownNow();
        LOGGER.info("Internet access stopped.");
    }
}
