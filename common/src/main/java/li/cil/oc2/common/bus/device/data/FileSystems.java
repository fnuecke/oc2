/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import li.cil.oc2.api.API;
import li.cil.oc2.common.vm.fs.LayeredFileSystem;
import li.cil.sedna.fs.FileSystem;
import li.cil.sedna.fs.ZipStreamFileSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class FileSystems {
    private static final Logger LOGGER = LogManager.getLogger(FileSystems.class);

    private static final String DIRECTORY = "file_systems";
    private static final String ARCHIVE_EXTENSION = ".zip";
    private static final String DESCRIPTOR_EXTENSION = ".json";

    private static final LayeredFileSystem LAYERED_FILE_SYSTEM = new LayeredFileSystem();

    // --------------------------------------------------------------------- //

    public static FileSystem getLayeredFileSystem() {
        return LAYERED_FILE_SYSTEM;
    }

    public static void initialize() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, ReloadListener.INSTANCE,
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, DIRECTORY));
        LifecycleEvent.SERVER_STOPPED.register(server -> LAYERED_FILE_SYSTEM.clear());
    }

    // --------------------------------------------------------------------- //

    private static void reload(final ResourceManager resourceManager) {
        LAYERED_FILE_SYSTEM.clear();

        LOGGER.info("Searching for datapack file systems...");
        final Map<ResourceLocation, Resource> descriptors = resourceManager
            .listResources(DIRECTORY, location -> location.getPath().endsWith(DESCRIPTOR_EXTENSION));

        final ArrayList<ZipStreamFileSystem> fileSystems = new ArrayList<>();
        final Object2IntArrayMap<ZipStreamFileSystem> fileSystemOrder = new Object2IntArrayMap<>();

        for (final Map.Entry<ResourceLocation, Resource> entry : descriptors.entrySet()) {
            LOGGER.info("Found [{}]", entry.getKey());
            try {
                final JsonObject json;
                try (Reader reader = entry.getValue().openAsReader()) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }

                final ZipStreamFileSystem fileSystem;
                try (InputStream stream = resourceManager.getResourceOrThrow(getArchiveLocation(entry.getKey())).open()) {
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
            } catch (final Throwable e) {
                LOGGER.error(e);
            }
        }

        fileSystems.sort(Comparator.comparingInt(fileSystemOrder::getInt));
        fileSystems.forEach(LAYERED_FILE_SYSTEM::addLayer);
    }

    private static ResourceLocation getArchiveLocation(final ResourceLocation descriptorLocation) {
        final String path = descriptorLocation.getPath();
        return descriptorLocation.withPath(path.substring(0, path.length() - DESCRIPTOR_EXTENSION.length()) + ARCHIVE_EXTENSION);
    }

    // --------------------------------------------------------------------- //

    private static final class ReloadListener implements PreparableReloadListener {
        public static final ReloadListener INSTANCE = new ReloadListener();

        @Override
        public CompletableFuture<Void> reload(final PreparableReloadListener.PreparationBarrier stage, final ResourceManager resourceManager, final ProfilerFiller preparationsProfiler, final ProfilerFiller reloadProfiler, final Executor backgroundExecutor, final Executor gameExecutor) {
            return CompletableFuture
                .runAsync(() -> FileSystems.reload(resourceManager), backgroundExecutor)
                .thenCompose(stage::wait);
        }
    }

    private FileSystems() {
    }
}
