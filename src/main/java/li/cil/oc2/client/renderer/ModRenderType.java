package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import li.cil.oc2.api.API;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

@OnlyIn(Dist.CLIENT)
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

    private static final RenderType PROJECTOR_LIGHT = create(
        API.MOD_ID + "/projector_light",
        DefaultVertexFormat.POSITION_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
            .setOutputState(WEATHER_TARGET)
            .setTransparencyState(LIGHTNING_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false));

    ///////////////////////////////////////////////////////////////////

    public static RenderType getNetworkCable() {
        return NETWORK_CABLE;
    }

    public static RenderType getProjectorLight() {
        return PROJECTOR_LIGHT;
    }

    public static RenderType getUnlitBlock(final ResourceLocation location) {
        final TextureStateShard texture = new TextureStateShard(location, false, true);
        final RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(POSITION_TEX_SHADER)
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

    public static RenderType getOverlay(final ResourceLocation location) {
        final TextureStateShard texture = new TextureStateShard(location, false, true);
        final RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(POSITION_TEX_SHADER)
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

    public static RenderType getProjector(final DynamicTexture texture) {
        final RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(POSITION_TEX_SHADER)
            .setTextureState(new DynamicTextureStateShard(texture))
            .setOutputState(TRANSLUCENT_TARGET)
            .setTransparencyState(ADDITIVE_TRANSPARENCY)
            .setCullState(NO_CULL)
            .createCompositeState(false);
        return create(
            API.MOD_ID + "/projector",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            state);
    }

    ///////////////////////////////////////////////////////////////////

    private static final class DynamicTextureStateShard extends EmptyTextureStateShard {
        public DynamicTextureStateShard(final DynamicTexture texture) {
            super(() -> {
                RenderSystem.enableTexture();
                // TODO client side setting
//                texture.setFilter(true, false);
                RenderSystem.setShaderTexture(0, texture.getId());
            }, () -> {
            });
        }
    }

    private ModRenderType(final String name, final VertexFormat format, final VertexFormat.Mode drawMode, final int bufferSize, final boolean useDelegate, final boolean needsSorting, final Runnable setupTask, final Runnable clearTask) {
        super(name, format, drawMode, bufferSize, useDelegate, needsSorting, setupTask, clearTask);
    }
}
