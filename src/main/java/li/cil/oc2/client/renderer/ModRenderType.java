package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ModRenderType extends RenderType {
    public static RenderType getUnlitBlock(final ResourceLocation location) {
        final TextureStateShard texture = new TextureStateShard(location, false, true);
        final RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_TEX_SHADER)
            .setTextureState(texture)
            .setOutputState(TRANSLUCENT_TARGET)
            .setTransparencyState(ADDITIVE_TRANSPARENCY)
            .setCullState(NO_CULL)
            .createCompositeState(false);
        return create(
            API.MOD_ID + "/unlit_block",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            state);
    }

    public static RenderType getNetworkCable() {
        final CompositeState state = CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_LIGHTMAP_SHADER)
            .setTextureState(NO_TEXTURE)
            .setTransparencyState(NO_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .createCompositeState(false);
        return create(API.MOD_ID + "/network_cable",
            DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            state);
    }

    public static RenderType getOverlay(final ResourceLocation location) {
        final TextureStateShard texture = new TextureStateShard(location, false, true);
        final RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_TEX_SHADER)
            .setTextureState(texture)
            .setOutputState(TRANSLUCENT_TARGET)
            .setTransparencyState(ADDITIVE_TRANSPARENCY)
            .createCompositeState(false);
        return create(
            API.MOD_ID + "/overlay",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            state);
    }

    ///////////////////////////////////////////////////////////////////

    private ModRenderType(final String name, final VertexFormat format, final VertexFormat.Mode drawMode, final int bufferSize, final boolean useDelegate, final boolean needsSorting, final Runnable setupTask, final Runnable clearTask) {
        super(name, format, drawMode, bufferSize, useDelegate, needsSorting, setupTask, clearTask);
    }
}
