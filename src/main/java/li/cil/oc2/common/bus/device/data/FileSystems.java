package li.cil.oc2.common.bus.device.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import li.cil.oc2.api.API;
import li.cil.oc2.common.vm.fs.LayeredFileSystem;
import li.cil.sedna.fs.FileSystem;
import li.cil.sedna.fs.ZipStreamFileSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FileSystems {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final LayeredFileSystem LAYERED_FILE_SYSTEM = new LayeredFileSystem();

    ///////////////////////////////////////////////////////////////////

    public static FileSystem getLayeredFileSystem() {
        return LAYERED_FILE_SYSTEM;
    }

    public static void reset() {
        LAYERED_FILE_SYSTEM.clear();
    }

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void handleAddReloadListenerEvent(final AddReloadListenerEvent event) {
        event.addListener(ReloadListener.INSTANCE);
    }

    @SubscribeEvent
    public static void handleServerStopped(final ServerStoppedEvent event) {
        reset();
    }

    ///////////////////////////////////////////////////////////////////

    private static void reload(final ResourceManager resourceManager) {
        reset();

        LOGGER.info("Searching for datapack filesystems...");
        final Collection<ResourceLocation> fileSystemDescriptorLocations = resourceManager
            .listResources("file_systems", s -> s.endsWith(".json"));

        final ArrayList<ZipStreamFileSystem> fileSystems = new ArrayList<>();
        final Object2IntArrayMap<ZipStreamFileSystem> fileSystemOrder = new Object2IntArrayMap<>();

        for (final ResourceLocation fileSystemDescriptorLocation : fileSystemDescriptorLocations) {
            LOGGER.info("Found [{}]", fileSystemDescriptorLocation);
            try {
                final Resource fileSystemDescriptor = resourceManager.getResource(fileSystemDescriptorLocation);
                final JsonObject json = JsonParser.parseReader(new InputStreamReader(fileSystemDescriptor.getInputStream())).getAsJsonObject();
                final String type = json.getAsJsonPrimitive("type").getAsString();
                switch (type) {
                    case "layer" -> {
                        final ResourceLocation location = new ResourceLocation(json.getAsJsonPrimitive("location").getAsString());

                        final ZipStreamFileSystem fileSystem;
                        try (final InputStream stream = resourceManager.getResource(location).getInputStream()) {
                            fileSystem = new ZipStreamFileSystem(stream);
                        }

                        final long fileCount = fileSystem.statfs().fileCount;
                        if (fileCount > 0) {
                            LOGGER.info("  Adding layer with [{}] file(s).", fileCount);
                            fileSystems.add(fileSystem);
                        } else {
                            LOGGER.info("  Skipping empty layer.");
                        }

                        if (json.has("order")) {
                            final JsonPrimitive order = json.getAsJsonPrimitive("order");
                            fileSystemOrder.put(fileSystem, order.getAsInt());
                        } else {
                            fileSystemOrder.put(fileSystem, 0);
                        }
                    }
                    case "block" -> LOGGER.error("Not yet implemented.");
                    default -> LOGGER.error("Unsupported file system type [{}].", type);
                }
            } catch (final Throwable e) {
                LOGGER.error(e);
            }
        }

        fileSystems.sort(Comparator.comparingInt(fileSystemOrder::getInt));
        fileSystems.forEach(LAYERED_FILE_SYSTEM::addLayer);
    }

    ///////////////////////////////////////////////////////////////////

    private static final class ReloadListener implements PreparableReloadListener {
        public static final ReloadListener INSTANCE = new ReloadListener();

        @Override
        public CompletableFuture<Void> reload(final PreparableReloadListener.PreparationBarrier stage, final ResourceManager resourceManager, final ProfilerFiller preparationsProfiler, final ProfilerFiller reloadProfiler, final Executor backgroundExecutor, final Executor gameExecutor) {
            return CompletableFuture
                .runAsync(() -> FileSystems.reload(resourceManager), backgroundExecutor)
                .thenCompose(stage::wait);
        }
    }
}
