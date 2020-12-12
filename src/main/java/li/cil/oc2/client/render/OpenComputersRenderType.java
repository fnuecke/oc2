package li.cil.oc2.client.render;

import li.cil.oc2.api.API;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public abstract class OpenComputersRenderType extends RenderType {
    public static RenderType getUnlitBlock(final ResourceLocation location) {
        final TextureState texture = new TextureState(location, false, true);
        final RenderType.State state = RenderType.State.getBuilder()
                .texture(texture)
                .writeMask(COLOR_WRITE)
                .transparency(ADDITIVE_TRANSPARENCY)
                .build(false);
        return RenderType.makeType(
                API.MOD_ID + ":unlit_block",
                DefaultVertexFormats.POSITION_TEX,
                GL11.GL_QUADS,
                256,
                state);
    }

    ///////////////////////////////////////////////////////////////////

    private OpenComputersRenderType(final String name, final VertexFormat format, final int drawMode, final int bufferSize, final boolean useDelegate, final boolean needsSorting, final Runnable setupTask, final Runnable clearTask) {
        super(name, format, drawMode, bufferSize, useDelegate, needsSorting, setupTask, clearTask);
    }
}
