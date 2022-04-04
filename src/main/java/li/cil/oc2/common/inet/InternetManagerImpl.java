package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.InternetManager;
import li.cil.oc2.api.inet.InternetProvider;
import li.cil.oc2.api.inet.LinkLocalLayer;
import li.cil.oc2.common.Config;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InternetManagerImpl implements InternetManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static InternetManagerImpl INSTANCE = null;

    //////////////////////////////////////////////////////////////

    private final InternetProvider internetProvider;
    private final List<InternetConnectionImpl> connections = new LinkedList<>();
    private final List<TaskImpl> tasks = new LinkedList<>();

    private final ExecutorService executor;
    private final Ipv4Space ipSpace;

    private InternetManagerImpl() {
        final ServiceLoader<InternetProvider> serviceLoader = ServiceLoader.load(InternetProvider.class);
        final Iterator<InternetProvider> iterator = serviceLoader.iterator();
        if (iterator.hasNext()) {
            internetProvider = iterator.next();
        } else {
            internetProvider = DefaultInternetProvider.INSTANCE;
        }
        executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Internet"));
        ipSpace = InetUtils.computeIpSpace(Config.deniedHosts, Config.allowedHosts);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static void initialize() {
        if (!Config.internetCardEnabled) {
            LOGGER.info("Internet card is disabled; Internet manager will not start");
        } else {
            INSTANCE = new InternetManagerImpl();
            LOGGER.warn("Internet card is enabled; Players may access to the internal network");
        }
    }

    public static Optional<InternetManagerImpl> getInstance() {
        return Optional.ofNullable(INSTANCE);
    }

    @Override
    public Task runOnInternetThreadTick(final Runnable action) {
        final TaskImpl task = new TaskImpl(action);
        tasks.add(task);
        return task;
    }

    public InternetConnection connect(final InternetAdapter internetAdapter, final Tag savedState) {
        final LayerParameters layerParameters = new LayerParametersImpl(savedState, this);
        final InternetConnectionImpl internetConnection =
            new InternetConnectionImpl(internetAdapter, internetProvider.provideInternet(layerParameters));
        connections.add(internetConnection);
        LOGGER.debug("A new internet access provided");
        return internetConnection;
    }

    private void processInternetAdapter(final InternetConnectionImpl connection) {
        final InternetAdapter adapter = connection.adapter;
        final byte[] received = connection.incoming.get();
        if (received != null) {
            adapter.sendEthernetFrame(received);
        }
        final byte[] sending = adapter.receiveEthernetFrame();
        if (sending != null) {
            connection.outcoming.put(sending);
        }
    }

    public boolean isAllowedToConnect(final int ipAddress) {
        return ipSpace.isAllowed(ipAddress);
    }

    private void runTasks() {
        tasks.removeIf(task -> {
            if (task.isClosed()) {
                return true;
            } else {
                final Runnable action = task.getAction();
                try {
                    action.run();
                    return false;
                } catch (final Exception exception) {
                    LOGGER.error("Uncaught exception while running internet thread task; this task removed from schedule", exception);
                    return true;
                }
            }
        });
    }

    private void runOnInternetThread(
        final List<InternetConnectionImpl> connectionsToStop,
        final List<InternetConnectionImpl> connectionsToProcess
    ) {
        runTasks();
        connectionsToStop.forEach(InternetConnectionImpl::stop);
        connectionsToProcess.forEach(InternetConnectionImpl::process);
    }

    //////////////////////////////////////////////////////////////

    @SubscribeEvent
    public void onTick(final TickEvent.ServerTickEvent event) {
        final List<InternetConnectionImpl> connectionsToStop = connections.stream()
            .filter(connection -> connection.isStopped)
            .collect(Collectors.toList());
        final List<InternetConnectionImpl> connectionsToProcess = connections.stream()
            .filter(connection -> !connection.isStopped)
            .collect(Collectors.toList());
        connections.removeIf(connection -> {
            if (connection.isStopped) {
                return true;
            } else {
                processInternetAdapter(connection);
                return false;
            }
        });
        executor.execute(() -> runOnInternetThread(connectionsToStop, connectionsToProcess));
    }

    @SubscribeEvent
    public void onStopping(final ServerStoppingEvent event) {
        connections.clear();
    }

    //////////////////////////////////////////////////////////////

    private static final class TaskImpl implements Task {

        private final Runnable action;
        private boolean closed = false;

        public TaskImpl(final Runnable action) {
            this.action = action;
        }

        public Runnable getAction() {
            return action;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }


    private final class InternetConnectionImpl implements InternetConnection {

        public final PendingFrame incoming = new PendingFrame();
        public final PendingFrame outcoming = new PendingFrame();

        private final ByteBuffer receiveBuffer = ByteBuffer.allocate(LinkLocalLayer.FRAME_SIZE);
        private final LinkLocalLayer ethernet;
        private final InternetAdapter adapter;
        private boolean isStopped = false;

        //////////////////////////////////////////////////////////////

        public InternetConnectionImpl(final InternetAdapter adapter, final LinkLocalLayer ethernet) {
            this.adapter = adapter;
            this.ethernet = ethernet;
        }

        @Override
        public Optional<Tag> saveAdapterState() {
            try {
                // TODO: blocking code; should reconsider it
                return executor.submit(ethernet::onSave).get();
            } catch (final InterruptedException | ExecutionException exception) {
                LOGGER.error("Error on saving internet adapter state", exception);
                return Optional.empty();
            }
        }

        public void process() {
            try {
                final byte[] outFrame = outcoming.get();
                if (outFrame != null) {
                    ethernet.sendEthernetFrame(ByteBuffer.wrap(outFrame));
                }
                receiveBuffer.clear();
                if (ethernet.receiveEthernetFrame(receiveBuffer)) {
                    final byte[] inFrame = new byte[receiveBuffer.remaining()];
                    receiveBuffer.get(inFrame);
                    incoming.put(inFrame);
                }
            } catch (Exception e) {
                LOGGER.error("Uncaught exception", e);
            }
        }

        @Override
        public void stop() {
            isStopped = true;
        }

        //////////////////////////////////////////////////////////////

        private static final class PendingFrame {

            private final AtomicReference<byte[]> pendingFrame = new AtomicReference<>();

            //////////////////////////////////////////////////////////////

            @Nullable
            public byte[] get() {
                return pendingFrame.getAndSet(null);
            }

            public void put(final byte[] frame) {
                pendingFrame.set(frame);
            }
        }
    }
}
