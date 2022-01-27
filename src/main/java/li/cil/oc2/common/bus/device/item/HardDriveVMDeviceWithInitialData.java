package li.cil.oc2.common.bus.device.item;

import com.google.common.eventbus.Subscribe;
import com.google.common.io.ByteStreams;
import li.cil.oc2.api.bus.device.vm.event.VMResumedRunningEvent;
import li.cil.oc2.common.util.Location;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public final class HardDriveVMDeviceWithInitialData extends HardDriveVMDevice {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ExecutorService WORKERS = Executors.newCachedThreadPool(r -> {
        final Thread thread = new Thread(r, "Hard Drive Initializer");
        thread.setDaemon(false);
        return thread;
    });

    private final BlockDevice base;
    private Future<Void> copyJob;

    ///////////////////////////////////////////////////////////////////

    public HardDriveVMDeviceWithInitialData(final ItemStack identity, final BlockDevice base, final boolean readonly, final Supplier<Optional<Location>> location) {
        super(identity, (int) base.getCapacity(), readonly, location);
        this.base = base;
    }

    @Subscribe
    public void handleResumedRunningEvent(final VMResumedRunningEvent event) {
        joinCopyJob();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected ByteBufferBlockDevice createBlockDevice() throws IOException {
        final boolean isInitializing = blobHandle == null;
        final ByteBufferBlockDevice device = super.createBlockDevice();
        if (isInitializing) {
            copyJob = CompletableFuture.runAsync(() -> {
                try {
                    try (final InputStream input = base.getInputStream(0);
                         final OutputStream output = device.getOutputStream(0)) {
                        ByteStreams.copy(input, output);
                    }
                } catch (final IOException e) {
                    LOGGER.error(e);
                }
            }, WORKERS);
        }
        return device;
    }

    @Override
    protected void closeBlockDevice() {
        // Join the copy job before releasing the device to avoid writes from thread to closed device.
        // Since we use memory mapped memory, closing the device leads to it holding a dead pointer,
        // meaning further access to it will hard-crash the JVM.
        joinCopyJob();

        super.closeBlockDevice();
    }

    ///////////////////////////////////////////////////////////////////

    private void joinCopyJob() {
        if (copyJob != null) {
            try {
                copyJob.get();
            } catch (final Throwable e) {
                LOGGER.error(e);
            } finally {
                copyJob = null;
            }
        }
    }
}
