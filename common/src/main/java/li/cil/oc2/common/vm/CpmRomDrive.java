/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import dev.architectury.registry.ReloadListenerRegistry;
import li.cil.oc2.api.API;
import li.cil.sedna.cpm.Cpm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class CpmRomDrive {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String DIRECTORY = "cpm";
    private static final int END_OF_FILE = 0x1A;

    @Nullable
    private static byte[] image;

    // --------------------------------------------------------------------- //

    public static byte[] getImage() {
        final byte[] result = image;
        if (result == null) {
            throw new IllegalStateException("The CP/M ROM drive has not been loaded yet.");
        }
        return result;
    }

    public static void initialize() {
        ReloadListenerRegistry.register(PackType.SERVER_DATA, ReloadListener.INSTANCE,
            ResourceLocation.fromNamespaceAndPath(API.MOD_ID, DIRECTORY));
    }

    public static byte[] compose(final Map<String, byte[]> files) {
        final byte[] composed = readBaseImage();
        files.forEach((name, content) -> CpmImage.addFile(composed, name, toCpmText(content)));
        return composed;
    }

    static byte[] toCpmText(final byte[] content) {
        for (final byte b : content) {
            final int value = b & 0xFF;
            final boolean looksLikeBinaryToMe = value != '\t' && value != '\r' && value != '\n' && (value < 0x20 || value > 0x7E);
            if (looksLikeBinaryToMe) {
                return content;
            }
        }

        final ByteArrayOutputStream text = new ByteArrayOutputStream(content.length + content.length / 16);
        for (final byte b : content) {
            if (b == '\r') {
                continue;
            }
            if (b == '\n') {
                text.write('\r');
            }
            text.write(b);
        }
        text.write(END_OF_FILE);
        return text.toByteArray();
    }

    // --------------------------------------------------------------------- //

    private static void reload(final ResourceManager resourceManager) {
        final Map<ResourceLocation, Resource> resources = resourceManager.listResources(
            DIRECTORY, location -> !location.getPath().endsWith("/"));

        final List<ResourceLocation> locations = new ArrayList<>(resources.keySet());
        locations.sort(Comparator.comparing(ResourceLocation::toString));

        final byte[] composed = readBaseImage();

        for (final ResourceLocation location : locations) {
            final String path = location.getPath();
            final String name = path.substring(path.lastIndexOf('/') + 1);
            try (InputStream stream = resources.get(location).open()) {
                CpmImage.addFile(composed, name, toCpmText(stream.readAllBytes()));
                LOGGER.info("Added [{}] to the CP/M ROM drive as [{}].", location, name);
            } catch (final Throwable e) {
                LOGGER.error("Failed adding [{}] to the CP/M ROM drive.", location, e);
            }
        }

        image = composed;
    }

    private static byte[] readBaseImage() {
        try (InputStream stream = Cpm.getFloppyImage()) {
            return stream.readAllBytes();
        } catch (final IOException e) {
            throw new IllegalStateException("Failed reading the built-in system disk.", e);
        }
    }

    private CpmRomDrive() {
    }

    // --------------------------------------------------------------------- //

    private static final class ReloadListener implements PreparableReloadListener {
        public static final ReloadListener INSTANCE = new ReloadListener();

        @Override
        public CompletableFuture<Void> reload(final PreparableReloadListener.PreparationBarrier stage, final ResourceManager resourceManager, final ProfilerFiller preparationsProfiler, final ProfilerFiller reloadProfiler, final Executor backgroundExecutor, final Executor gameExecutor) {
            return CompletableFuture
                .runAsync(() -> CpmRomDrive.reload(resourceManager), backgroundExecutor)
                .thenCompose(stage::wait);
        }
    }
}
