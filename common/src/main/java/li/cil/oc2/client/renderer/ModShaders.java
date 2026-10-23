/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ModShaders {
    public static final int MAX_PROJECTORS = 3;

    public static final RenderPipeline TERMINAL_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/terminal"))
        .withVertexShader(Identifier.fromNamespaceAndPath(API.MOD_ID, "core/terminal"))
        .withFragmentShader(Identifier.fromNamespaceAndPath(API.MOD_ID, "core/terminal"))
        .withSampler("Sampler0")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .build();

    public static final RenderPipeline PROJECTORS_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/projectors"))
        .withVertexShader("core/screenquad")
        .withFragmentShader(Identifier.fromNamespaceAndPath(API.MOD_ID, "core/projectors"))
        .withUniform("ProjectorsConfig", UniformType.UNIFORM_BUFFER)
        .withSampler("MainCameraDepth")
        .withSampler("ProjectorColor0")
        .withSampler("ProjectorDepth0")
        .withSampler("ProjectorColor1")
        .withSampler("ProjectorDepth1")
        .withSampler("ProjectorColor2")
        .withSampler("ProjectorDepth2")
        .withBlend(BlendFunction.ADDITIVE)
        .withColorWrite(true, false)
        .build();

    public static final RenderPipeline TERMINAL_DOWNSAMPLE_PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/terminal_downsample"))
        .withVertexShader("core/screenquad")
        .withFragmentShader("core/blit_screen")
        .withSampler("InSampler")
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
        .build();

    public static final RenderPipeline GUI_TERMINAL_CURSOR_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/gui_terminal_cursor"))
        .withVertexShader(Identifier.fromNamespaceAndPath(API.MOD_ID, "core/terminal"))
        .withFragmentShader(Identifier.fromNamespaceAndPath(API.MOD_ID, "core/terminal"))
        .withSampler("Sampler0")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .build();

    public static final RenderPipeline GUI_TERMINAL_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/gui_terminal"))
        .withVertexShader("core/position_tex_color")
        .withFragmentShader(Identifier.fromNamespaceAndPath(API.MOD_ID, "core/gui_terminal"))
        .withSampler("Sampler0")
        .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .build();

    // --------------------------------------------------------------------- //

    private ModShaders() {
    }
}
