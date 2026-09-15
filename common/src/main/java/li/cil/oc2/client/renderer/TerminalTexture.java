/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import li.cil.oc2.common.vm.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL30;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public final class TerminalTexture implements Terminal.Listener, AutoCloseable {
    private static final float CURSOR_DEPTH_OFFSET = 0.025f;

    // --------------------------------------------------------------------- //

    private final Terminal terminal;
    private final TerminalRenderer renderer;
    private final Matrix4f projectionMatrix = new Matrix4f();

    @Nullable
    private TextureTarget target;
    private volatile boolean isDirty = true;

    // --------------------------------------------------------------------- //

    public TerminalTexture(final Terminal terminal) {
        this.terminal = terminal;
        this.renderer = new TerminalRenderer(terminal);
        terminal.addListener(this);
    }

    // --------------------------------------------------------------------- //

    public void draw(final PoseStack stack) {
        if (target == null) {
            return;
        }

        RenderSystem.setShaderTexture(0, target.getColorTextureId());

        final RenderType renderType = ModRenderType.getTerminalScreen();
        BufferBuilder builder = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
        final Matrix4f matrix = stack.last().pose();
        final float x = terminal.getWidth(), y = terminal.getHeight();

        builder.addVertex(matrix, 0, 0, 0).setUv(0, 1).setColor(-1);
        builder.addVertex(matrix, 0, y, 0).setUv(0, 0).setColor(-1);
        builder.addVertex(matrix, x, y, 0).setUv(1, 0).setColor(-1);
        builder.addVertex(matrix, x, 0, 0).setUv(1, 1).setColor(-1);

        MeshData mesh = builder.build();
        if (mesh != null) {
            renderType.draw(mesh);
        }

        if (!TerminalRenderer.shouldRenderCursor(terminal)) {
            return;
        }

        RenderSystem.setShaderTexture(0, TerminalRenderer.getFontTexture());
        builder = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
        renderer.buildCursor(new Matrix4f(matrix).translate(0, 0, CURSOR_DEPTH_OFFSET), builder);

        mesh = builder.build();
        if (mesh != null) {
            renderType.draw(mesh);
        }
    }

    public void refresh() {
        if (!isDirty) {
            return;
        }
        isDirty = false;

        if (target == null) {
            target = new TextureTarget(terminal.getWidth(), terminal.getHeight(), false, Minecraft.ON_OSX);
            // Mipmapped so the copy survives being minified in world; premultiplied colour is exactly what
            // filtering wants, so the levels stay correct. setFilterMode unbinds, hence the rebind.
            target.setFilterMode(GlConst.GL_LINEAR);
            RenderSystem.bindTexture(target.getColorTextureId());
            GlStateManager._texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_LINEAR_MIPMAP_LINEAR);
        }

        target.setClearColor(0, 0, 0, 0);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);

        projectionMatrix.setOrtho(0, terminal.getWidth(), terminal.getHeight(), 0, -1, 1);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);
        final Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableDepthTest();
        renderer.renderBody(new PoseStack(), new Matrix4f(), projectionMatrix);
        RenderSystem.enableDepthTest();

        modelViewStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();

        final RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
        mainTarget.bindWrite(true);

        RenderSystem.bindTexture(target.getColorTextureId());
        GL30.glGenerateMipmap(GlConst.GL_TEXTURE_2D);
    }

    @Override
    public void handleTerminalChanged() {
        isDirty = true;
    }

    @Override
    public void close() {
        terminal.removeListener(this);
        renderer.close();
        releaseTarget();
    }

    // --------------------------------------------------------------------- //

    private void releaseTarget() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
