package li.cil.oc2.client.render;

import li.cil.oc2.api.API;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

public abstract class OpenComputersRenderType extends RenderLayer {
    public static RenderLayer getUnlitBlock(final Identifier location) {
        final Texture texture = new Texture(location, false, true);
        final RenderLayer.MultiPhaseParameters state = RenderLayer.MultiPhaseParameters.builder()
                .texture(texture)
                .writeMaskState(COLOR_MASK)
                .transparency(ADDITIVE_TRANSPARENCY)
                .build(false);
        return RenderLayer.of(
                API.MOD_ID + ":unlit_block",
                VertexFormats.POSITION_TEXTURE,
                GL11.GL_QUADS,
                256,
                state);
    }

    ///////////////////////////////////////////////////////////////////

    private OpenComputersRenderType(final String name, final VertexFormat format, final int drawMode, final int bufferSize, final boolean useDelegate, final boolean needsSorting, final Runnable setupTask, final Runnable clearTask) {
        super(name, format, drawMode, bufferSize, useDelegate, needsSorting, setupTask, clearTask);
    }
}
