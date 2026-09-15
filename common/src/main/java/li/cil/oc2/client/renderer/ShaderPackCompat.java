/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import dev.architectury.platform.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class ShaderPackCompat {
    private static final Logger LOGGER = LogManager.getLogger(ShaderPackCompat.class);

    // --------------------------------------------------------------------- //

    private static boolean isInitialized;
    @Nullable
    private static Class<?> pipelineClass;
    @Nullable
    private static MethodHandle getPipelineManager;
    @Nullable
    private static MethodHandle getPipelineNullable;
    @Nullable
    private static MethodHandle bindDefault;

    // --------------------------------------------------------------------- //

    public static boolean isShaderPackRendering() {
        return pipeline() != null;
    }

    public static boolean bindShaderPackGBuffer() {
        final Object pipeline = pipeline();
        if (pipeline == null) {
            return false;
        }

        try {
            bindDefault.invoke(pipeline);
            return true;
        } catch (final Throwable e) {
            LOGGER.error("Failed binding shader pack g-buffer, projections will not be lit by the shader pack.", e);
            bindDefault = null;
            return false;
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    private static Object pipeline() {
        initialize();

        if (bindDefault == null) {
            return null;
        }

        try {
            final Object pipeline = getPipelineNullable.invoke(getPipelineManager.invoke());
            return pipelineClass.isInstance(pipeline) ? pipeline : null;
        } catch (final Throwable e) {
            LOGGER.error("Failed querying shader pack pipeline, projections will composite after the level.", e);
            bindDefault = null;
            return null;
        }
    }

    private static void initialize() {
        if (isInitialized) {
            return;
        }
        isInitialized = true;

        if (!Platform.isModLoaded("iris")) {
            return;
        }

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            final Class<?> pipelineManagerClass = Class.forName("net.irisshaders.iris.pipeline.PipelineManager");
            final Class<?> worldRenderingPipelineClass = Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPipeline");

            pipelineClass = Class.forName("net.irisshaders.iris.pipeline.IrisRenderingPipeline");
            getPipelineManager = lookup.findStatic(irisClass, "getPipelineManager", MethodType.methodType(pipelineManagerClass));
            getPipelineNullable = lookup.findVirtual(pipelineManagerClass, "getPipelineNullable", MethodType.methodType(worldRenderingPipelineClass));
            bindDefault = lookup.findVirtual(pipelineClass, "bindDefault", MethodType.methodType(void.class));
        } catch (final Throwable e) {
            LOGGER.warn("Shader mod present but its pipeline does not look as expected, projections will composite after the level.", e);
        }
    }

    private ShaderPackCompat() {
    }
}
