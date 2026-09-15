/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc2.api.API;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public abstract class ModRenderType extends RenderType {
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "textures/misc/white.png");

    private static final RenderType INDICATOR = create(
        API.MOD_ID + "/indicator",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        false,
        CompositeState.builder()
            .setShaderState(POSITION_COLOR_SHADER)
            .setTextureState(NO_TEXTURE)
            .setTransparencyState(NO_TRANSPARENCY)
            .setLayeringState(POLYGON_OFFSET_LAYERING)
            .createCompositeState(false));

    private static final RenderType PROJECTOR_LIGHT = create(
        API.MOD_ID + "/projector_light",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
            .setTransparencyState(LIGHTNING_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false));

    private static final Function<ResourceLocation, RenderType> UNLIT_BLOCK = Util.memoize(location -> create(
        API.MOD_ID + "/unlit_block/" + location,
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        CompositeState.builder()
            .setShaderState(POSITION_TEX_SHADER)
            .setTextureState(new TextureStateShard(location, false, true))
            .setTransparencyState(ADDITIVE_TRANSPARENCY)
            .setCullState(NO_CULL)
            .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> OVERLAY = Util.memoize(location -> create(
        API.MOD_ID + "/overlay/" + location,
        DefaultVertexFormat.POSITION_TEX,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        CompositeState.builder()
            .setShaderState(POSITION_TEX_SHADER)
            .setTextureState(new TextureStateShard(location, false, true))
            .setTransparencyState(ADDITIVE_TRANSPARENCY)
            .createCompositeState(false)));

    private static final TransparencyStateShard PREMULTIPLIED_TRANSPARENCY = new TransparencyStateShard(
        API.MOD_ID + ":premultiplied_transparency",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        });

    private static final RenderType TERMINAL_SCREEN = create(
        API.MOD_ID + "/terminal_screen",
        DefaultVertexFormat.POSITION_TEX_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        false,
        CompositeState.builder()
            .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
            .setTextureState(NO_TEXTURE)
            .setTransparencyState(PREMULTIPLIED_TRANSPARENCY)
            .createCompositeState(false));

    // --------------------------------------------------------------------- //

    public static RenderType getNetworkCable() {
        return entityCutoutNoCull(WHITE_TEXTURE);
    }

    public static RenderType getIndicator() {
        return INDICATOR;
    }

    public static RenderType getProjectorLight() {
        return PROJECTOR_LIGHT;
    }

    public static RenderType getUnlitBlock(final ResourceLocation location) {
        return UNLIT_BLOCK.apply(location);
    }

    public static RenderType getOverlay(final ResourceLocation location) {
        return OVERLAY.apply(location);
    }

    public static RenderType getTerminalScreen() {
        return TERMINAL_SCREEN;
    }

    // --------------------------------------------------------------------- //

    private ModRenderType(final String name, final VertexFormat format, final VertexFormat.Mode drawMode, final int bufferSize, final boolean useDelegate, final boolean needsSorting, final Runnable setupTask, final Runnable clearTask) {
        super(name, format, drawMode, bufferSize, useDelegate, needsSorting, setupTask, clearTask);
    }
}
