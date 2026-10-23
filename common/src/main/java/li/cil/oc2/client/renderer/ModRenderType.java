/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc2.api.API;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public final class ModRenderType {
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath(API.MOD_ID, "textures/misc/white.png");

    private static final RenderPipeline INDICATOR_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/indicator"))
        .withVertexShader("core/position_color")
        .withFragmentShader("core/position_color")
        .withoutBlend()
        .withDepthBias(-1, -10)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .build();

    private static final RenderPipeline GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/glow"))
        .withVertexShader("core/position_color")
        .withFragmentShader("core/position_color")
        .withBlend(BlendFunction.ADDITIVE)
        .withDepthWrite(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .build();

    private static final RenderPipeline PROJECTOR_LIGHT_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/projector_light"))
        .withVertexShader("core/rendertype_lightning")
        .withFragmentShader("core/rendertype_lightning")
        .withBlend(BlendFunction.LIGHTNING)
        .withDepthWrite(false)
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
        .build();

    private static final RenderPipeline UNLIT_BLOCK_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/unlit_block"))
        .withVertexShader("core/position_tex")
        .withFragmentShader("core/position_tex")
        .withSampler("Sampler0")
        .withBlend(BlendFunction.ADDITIVE)
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
        .build();

    private static final RenderPipeline OVERLAY_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/overlay"))
        .withVertexShader("core/position_tex")
        .withFragmentShader("core/position_tex")
        .withSampler("Sampler0")
        .withBlend(BlendFunction.ADDITIVE)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
        .build();

    private static final RenderPipeline TERMINAL_SCREEN_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(API.MOD_ID, "pipeline/terminal_screen"))
        .withVertexShader("core/position_tex_color")
        .withFragmentShader("core/position_tex_color")
        .withSampler("Sampler0")
        .withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
        .build();

    private static final RenderType INDICATOR = RenderType.create(API.MOD_ID + "/indicator",
        RenderSetup.builder(INDICATOR_PIPELINE).createRenderSetup());

    private static final RenderType GLOW = RenderType.create(API.MOD_ID + "/glow",
        RenderSetup.builder(GLOW_PIPELINE).createRenderSetup());

    private static final RenderType PROJECTOR_LIGHT = RenderType.create(API.MOD_ID + "/projector_light",
        RenderSetup.builder(PROJECTOR_LIGHT_PIPELINE).sortOnUpload().createRenderSetup());

    private static final Function<Identifier, RenderType> UNLIT_BLOCK = Util.memoize(location -> RenderType.create(
        API.MOD_ID + "/unlit_block/" + location,
        RenderSetup.builder(UNLIT_BLOCK_PIPELINE).withTexture("Sampler0", location).sortOnUpload().createRenderSetup()));

    private static final Function<Identifier, RenderType> OVERLAY = Util.memoize(location -> RenderType.create(
        API.MOD_ID + "/overlay/" + location,
        RenderSetup.builder(OVERLAY_PIPELINE).withTexture("Sampler0", location).sortOnUpload().createRenderSetup()));

    private static final Function<Identifier, RenderType> TERMINAL_SCREEN = Util.memoize(location -> RenderType.create(
        API.MOD_ID + "/terminal_screen/" + location,
        RenderSetup.builder(TERMINAL_SCREEN_PIPELINE).withTexture("Sampler0", location).createRenderSetup()));

    // --------------------------------------------------------------------- //

    public static void initialize() {
        IrisCompat.assignPipeline(INDICATOR_PIPELINE, "BASIC", "SHADOW_BASIC_COLOR");
        IrisCompat.assignPipeline(GLOW_PIPELINE, "BASIC", "SHADOW_BASIC_COLOR");
        IrisCompat.assignPipeline(UNLIT_BLOCK_PIPELINE, "TEXTURED", "SHADOW_TEX");
        IrisCompat.assignPipeline(OVERLAY_PIPELINE, "TEXTURED", "SHADOW_TEX");
        IrisCompat.assignPipeline(TERMINAL_SCREEN_PIPELINE, "TEXTURED", "SHADOW_TEX_COLOR");
    }

    public static RenderType getNetworkCable() {
        return RenderTypes.entityCutoutNoCull(WHITE_TEXTURE);
    }

    public static RenderType getIndicator() {
        return INDICATOR;
    }

    public static RenderType getGlow() {
        return GLOW;
    }

    public static RenderType getProjectorLight() {
        return PROJECTOR_LIGHT;
    }

    public static RenderType getUnlitBlock(final Identifier location) {
        return UNLIT_BLOCK.apply(location);
    }

    public static RenderType getOverlay(final Identifier location) {
        return OVERLAY.apply(location);
    }

    public static RenderType getTerminalScreen(final Identifier location) {
        return TERMINAL_SCREEN.apply(location);
    }

    // --------------------------------------------------------------------- //

    private ModRenderType() {
    }
}
