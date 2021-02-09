package li.cil.oc2.common.bus.device.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import li.cil.oc2.common.vm.fs.LayeredFileSystem;
import li.cil.oc2.common.vm.fs.ResourceFileSystem;
import li.cil.sedna.fs.FileSystem;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class FileSystems {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final LayeredFileSystem LAYERED_FILE_SYSTEM = new LayeredFileSystem();

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(FileSystems::handleAddReloadListenerEvent);
    }

    public static FileSystem getLayeredFileSystem() {
        return LAYERED_FILE_SYSTEM;
    }

    public static void reset() {
        LAYERED_FILE_SYSTEM.clear();
    }

    ///////////////////////////////////////////////////////////////////

    private static void handleAddReloadListenerEvent(final AddReloadListenerEvent event) {
        event.addListener(ReloadListener.INSTANCE);
    }

    private static void reload(final IResourceManager resourceManager) {
        reset();

        LOGGER.info("Searching for datapack filesystems...");
        final Collection<ResourceLocation> fileSystemDescriptorLocations = resourceManager
                .getAllResourceLocations("file_systems", s -> s.endsWith(".fs.json"));

        for (final ResourceLocation fileSystemDescriptorLocation : fileSystemDescriptorLocations) {
            LOGGER.info("Found [{}]", fileSystemDescriptorLocation);
            try {
                final IResource fileSystemDescriptor = resourceManager.getResource(fileSystemDescriptorLocation);
                final JsonObject json = new JsonParser().parse(new InputStreamReader(fileSystemDescriptor.getInputStream())).getAsJsonObject();
                final String type = json.getAsJsonPrimitive("type").getAsString();
                switch (type) {
                    case "virtio-9p": {
                        final ResourceLocation location = new ResourceLocation(json.getAsJsonPrimitive("location").getAsString());
                        final ResourceFileSystem fileSystem = new ResourceFileSystem(resourceManager, location);
                        final long fileCount = fileSystem.statfs().fileCount;
                        if (fileCount > 0) {
                            LOGGER.info("  Adding layer with [{}] file(s).", fileCount);
                            LAYERED_FILE_SYSTEM.addLayer(fileSystem);
                        } else {
                            LOGGER.info("  Skipping empty layer.");
                        }
                        break;
                    }
                    case "virtio-blk": {
                        LOGGER.error("Not yet implemented.");
                        break;
                    }
                    default: {
                        LOGGER.error("Unsupported file system type [{}].", type);
                        break;
                    }
                }
            } catch (final Throwable e) {
                LOGGER.error(e);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class ReloadListener implements IFutureReloadListener {
        public static final ReloadListener INSTANCE = new ReloadListener();

        @Override
        public CompletableFuture<Void> reload(final IFutureReloadListener.IStage stage, final IResourceManager resourceManager, final IProfiler preparationsProfiler, final IProfiler reloadProfiler, final Executor backgroundExecutor, final Executor gameExecutor) {
            return CompletableFuture
                    .runAsync(() -> FileSystems.reload(resourceManager), backgroundExecutor)
                    .thenCompose(stage::markCompleteAwaitingOthers);
        }
    }
}
