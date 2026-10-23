/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.architectury.platform.Platform;
import it.unimi.dsi.fastutil.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;

public final class IrisCompat {
    private static final Logger LOGGER = LogManager.getLogger(IrisCompat.class);

    // --------------------------------------------------------------------- //

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void assignPipeline(final RenderPipeline pipeline, final String program, final String shadowProgram) {
        if (!Platform.isModLoaded("iris")) {
            return;
        }

        try {
            final Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            final Class<Enum> programClass = (Class<Enum>) Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            final Object api = apiClass.getMethod("getInstance").invoke(null);
            apiClass.getMethod("assignPipeline", RenderPipeline.class, programClass).invoke(api, pipeline, Enum.valueOf(programClass, program));
        } catch (final Throwable e) {
            LOGGER.warn("Failed assigning shader pack program to {}, it will not render while a shader pack is active.", pipeline.getLocation(), e);
        }

        try {
            final Object key = Enum.valueOf((Class<Enum>) Class.forName("net.irisshaders.iris.pipeline.programs.ShaderKey"), shadowProgram);
            final Field shadowPrograms = Class.forName("net.irisshaders.iris.pipeline.IrisPipelines").getDeclaredField("coreShaderMapShadow");
            shadowPrograms.setAccessible(true);
            ((Map<RenderPipeline, Function<Object, Object>>) shadowPrograms.get(null)).put(pipeline, p -> key);
        } catch (final Throwable e) {
            LOGGER.warn("Failed assigning shader pack shadow program to {}, it will render into shadow maps with its own program.", pipeline.getLocation(), e);
        }
    }

    public static void copyPipeline(final RenderPipeline source, final RenderPipeline pipeline) {
        if (!Platform.isModLoaded("iris")) {
            return;
        }

        try {
            Class.forName("net.irisshaders.iris.pipeline.IrisPipelines")
                .getMethod("copyPipeline", RenderPipeline.class, RenderPipeline.class)
                .invoke(null, source, pipeline);
        } catch (final Throwable e) {
            LOGGER.warn("Failed assigning shader pack program to {}, it will not render while a shader pack is active.", pipeline.getLocation(), e);
        }
    }

    private IrisCompat() {
    }
}
