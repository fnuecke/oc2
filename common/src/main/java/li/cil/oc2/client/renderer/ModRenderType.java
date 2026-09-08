/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc2.api.API;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

@Environment(EnvType.CLIENT)
public abstract class ModRenderType extends RenderType {
    private static final RenderType NETWORK_CABLE = create(
        API.MOD_ID + "/network_cable",
        DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
        VertexFormat.Mode.QUADS,
        256,
        false,
        false,
        CompositeState.builder()
            .setShaderState(POSITION_COLOR_LIGHTMAP_SHADER)
            .setTextureState(NO_TEXTURE)
            .setTransparencyState(NO_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .createCompositeState(false));

    private static final RenderType CONNECTOR_INDICATOR = create(
        API.MOD_ID + "/connector_indicator",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.TRIANGLE_STRIP,
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

    // --------------------------------------------------------------------- //

    public static RenderType getNetworkCable() {
        return NETWORK_CABLE;
    }

    public static RenderType getConnectorIndicator() {
        return CONNECTOR_INDICATOR;
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

    // --------------------------------------------------------------------- //

    private ModRenderType(final String name, final VertexFormat format, final VertexFormat.Mode drawMode, final int bufferSize, final boolean useDelegate, final boolean needsSorting, final Runnable setupTask, final Runnable clearTask) {
        super(name, format, drawMode, bufferSize, useDelegate, needsSorting, setupTask, clearTask);
    }
}
