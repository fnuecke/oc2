package li.cil.oc2.common.bus.device.data;

import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import li.cil.oc2.common.vm.fs.LayeredFileSystem;
import li.cil.oc2.common.vm.fs.ResourceFileSystem;
import li.cil.sedna.fs.FileSystem;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.profiler.IProfiler;
import net.minecraftforge.event.AddReloadListenerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.util.Collection;

public final class FileSystems {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final LayeredFileSystem LAYERED_FILE_SYSTEM = new LayeredFileSystem();
    
    private static final IFutureReloadListener RELOAD_LISTENER = new IFutureReloadListener() {
            @Override
            public String getSimpleName() {
                return "oc2:file_system";
            };
            
            @Override
            public CompletableFuture<Void> reload(IFutureReloadListener.IStage stage, IResourceManager resourceManager, IProfiler preparaationsProfiler, IProfiler reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return CompletableFuture.runAsync(() -> {
                        LAYERED_FILE_SYSTEM.clear();

                        LOGGER.info("Searching for datapack filesystems!");
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
                                    LAYERED_FILE_SYSTEM.addLayer(fileSystem);
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
                    }, backgroundExecutor)
                    .thenCompose(stage::markCompleteAwaitingOthers);
            }
        };

    
    public static FileSystem getLayeredFileSystem() {
        return LAYERED_FILE_SYSTEM;
    }


    public static void addReloadListenerEvent(final AddReloadListenerEvent event) {
        event.addListener(RELOAD_LISTENER);
    };

    public static void reset() {
        LAYERED_FILE_SYSTEM.clear();
    }
}
