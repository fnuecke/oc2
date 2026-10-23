/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import dev.architectury.platform.Platform;
import net.minecraft.client.renderer.LevelRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class SodiumCompat {
    private static final Logger LOGGER = LogManager.getLogger(SodiumCompat.class);

    // --------------------------------------------------------------------- //

    private static boolean isInitialized;
    @Nullable
    private static MethodHandle getMatrices;
    @Nullable
    private static MethodHandle setMatrices;
    @Nullable
    private static MethodHandle createMatrices;

    // --------------------------------------------------------------------- //

    @Nullable
    public static Object setChunkRenderMatrices(final LevelRenderer levelRenderer, final Matrix4fc projection, final Matrix4fc modelView) {
        initialize();

        if (setMatrices == null) {
            return null;
        }

        try {
            final Object previous = getMatrices.invoke(levelRenderer);
            setMatrices.invoke(levelRenderer, createMatrices.invoke(projection, modelView));
            return previous;
        } catch (final Throwable e) {
            LOGGER.error("Failed setting Sodium chunk render matrices, projections rendering will be fucky.", e);
            setMatrices = null;
            return null;
        }
    }

    public static void restoreChunkRenderMatrices(final LevelRenderer levelRenderer, @Nullable final Object matrices) {
        if (setMatrices == null || matrices == null) {
            return;
        }

        try {
            setMatrices.invoke(levelRenderer, matrices);
        } catch (final Throwable e) {
            LOGGER.error("Failed restoring Sodium chunk render matrices.", e);
            setMatrices = null;
        }
    }

    // --------------------------------------------------------------------- //

    private static void initialize() {
        if (isInitialized) {
            return;
        }
        isInitialized = true;

        if (!Platform.isModLoaded("sodium")) {
            return;
        }

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Class<?> extensionClass = Class.forName("net.caffeinemc.mods.sodium.client.world.LevelRendererExtension");
            final Class<?> matricesClass = Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices");

            getMatrices = lookup.findVirtual(extensionClass, "sodium$getMatrices", MethodType.methodType(matricesClass));
            createMatrices = lookup.findConstructor(matricesClass, MethodType.methodType(void.class, Matrix4fc.class, Matrix4fc.class));
            setMatrices = lookup.findVirtual(extensionClass, "sodium$setMatrices", MethodType.methodType(void.class, matricesClass));
        } catch (final Throwable e) {
            LOGGER.warn("Sodium present but its level renderer does not look as expected, projector rendering will be fucky.", e);
        }
    }

    private SodiumCompat() {
    }
}
