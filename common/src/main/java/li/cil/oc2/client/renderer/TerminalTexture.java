/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import com.mojang.blaze3d.vertex.*;
import li.cil.oc2.common.vm.device.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public final class TerminalTexture implements Terminal.Listener, AutoCloseable {
    private static final float CURSOR_DEPTH_OFFSET = 0.025f;
    private static final RenderType CURSOR_RENDER_TYPE = ModRenderType.getTerminalScreen(TerminalRenderer.getFontTexture());
    private static final Vector4f NO_COLOR_MODULATION = new Vector4f(1, 1, 1, 1);
    private static final Vector3f NO_MODEL_OFFSET = new Vector3f();
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static final CachedOrthoProjectionMatrixBuffer PROJECTION = new CachedOrthoProjectionMatrixBuffer("oc2 terminal", -1, 1, true);

    // --------------------------------------------------------------------- //

    private final Terminal terminal;
    private final TerminalRenderer renderer;
    private final Identifier location;
    private final RenderType renderType;
    private final SubmitNodeCollector.CustomGeometryRenderer screenGeometry = this::buildScreen;
    private final SubmitNodeCollector.CustomGeometryRenderer cursorGeometry = this::buildCursor;

    @Nullable
    private TargetTexture texture;
    private volatile boolean isDirty = true;

    // --------------------------------------------------------------------- //

    public TerminalTexture(final Terminal terminal, final Identifier location) {
        this.terminal = terminal;
        this.renderer = new TerminalRenderer(terminal);
        this.location = location;
        this.renderType = ModRenderType.getTerminalScreen(location);
        terminal.addListener(this);
    }

    // --------------------------------------------------------------------- //

    public Identifier getLocation() {
        return location;
    }

    public void draw(final PoseStack stack, final SubmitNodeCollector collector) {
        if (texture == null) {
            return;
        }

        collector.submitCustomGeometry(stack, renderType, screenGeometry);

        if (!TerminalRenderer.shouldRenderCursor(terminal)) {
            return;
        }

        stack.pushPose();
        stack.translate(0, 0, CURSOR_DEPTH_OFFSET);
        collector.submitCustomGeometry(stack, CURSOR_RENDER_TYPE, cursorGeometry);
        stack.popPose();
    }

    public void drawGui(final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        refresh();
        if (texture == null) {
            return;
        }

        final int textureWidth = terminal.getWidth(), textureHeight = terminal.getHeight();
        graphics.blit(ModShaders.GUI_TERMINAL_PIPELINE, location, x, y, 0, textureHeight, width, height,
            textureWidth, -textureHeight, textureWidth, textureHeight);

        if (!TerminalRenderer.shouldRenderCursor(terminal)) {
            return;
        }

        final Matrix3x2f pose = new Matrix3x2f(graphics.pose())
            .translate(x, y)
            .scale(width / (float) textureWidth, height / (float) textureHeight);
        final AbstractTexture font = Minecraft.getInstance().getTextureManager().getTexture(TerminalRenderer.getFontTexture());
        graphics.guiRenderState.submitGuiElement(new GuiCursorRenderState(renderer, pose,
            TextureSetup.singleTexture(font.getTextureView(), font.getSampler()),
            new ScreenRectangle(x, y, width, height).transformMaxBounds(graphics.pose())));
    }

    public void refresh() {
        if (!isDirty) {
            return;
        }
        isDirty = false;

        if (texture == null) {
            texture = new TargetTexture(location.toString(), terminal.getWidth(), terminal.getHeight());
            Minecraft.getInstance().getTextureManager().register(location, texture);
        }

        final BufferBuilder builder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        renderer.buildBody(builder);

        final MipmapChain chain = MipmapChain.get(terminal.getWidth(), terminal.getHeight());
        final CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try (MeshData mesh = builder.build()) {
            if (mesh == null) {
                encoder.clearColorTexture(chain.levels[0], 0);
            } else {
                renderBody(encoder, mesh, chain.views[0]);
            }
        }

        chain.downsample(encoder);
        chain.copyTo(encoder, texture.levelViews);
    }

    @Override
    public void handleTerminalChanged() {
        isDirty = true;
    }

    @Override
    public void close() {
        terminal.removeListener(this);
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(location);
            texture = null;
        }
    }

    // --------------------------------------------------------------------- //

    private void renderBody(final CommandEncoder encoder, final MeshData mesh, final GpuTextureView target) {
        final GpuBufferSlice projectionBuffer = PROJECTION.getBuffer(terminal.getWidth(), terminal.getHeight());
        final GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
            .writeTransform(IDENTITY, NO_COLOR_MODULATION, NO_MODEL_OFFSET, IDENTITY);
        final GpuBuffer vertices = ModShaders.TERMINAL_PIPELINE.getVertexFormat().uploadImmediateVertexBuffer(mesh.vertexBuffer());
        final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(mesh.drawState().mode());
        final GpuBuffer indexBuffer = indices.getBuffer(mesh.drawState().indexCount());
        final AbstractTexture font = Minecraft.getInstance().getTextureManager().getTexture(TerminalRenderer.getFontTexture());

        try (RenderPass pass = encoder.createRenderPass(() -> "oc2 terminal", target, OptionalInt.of(0))) {
            pass.setPipeline(ModShaders.TERMINAL_PIPELINE);
            pass.setUniform("Projection", projectionBuffer);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture("Sampler0", font.getTextureView(), font.getSampler());
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1);
        }
    }

    private void buildScreen(final PoseStack.Pose pose, final VertexConsumer consumer) {
        final float x = terminal.getWidth(), y = terminal.getHeight();
        consumer.addVertex(pose, 0, 0, 0).setUv(0, 1).setColor(-1);
        consumer.addVertex(pose, 0, y, 0).setUv(0, 0).setColor(-1);
        consumer.addVertex(pose, x, y, 0).setUv(1, 0).setColor(-1);
        consumer.addVertex(pose, x, 0, 0).setUv(1, 1).setColor(-1);
    }

    private void buildCursor(final PoseStack.Pose pose, final VertexConsumer consumer) {
        renderer.buildCursor(pose.pose(), consumer);
    }

    private static int mipLevelCount(final int width, final int height) {
        return 32 - Integer.numberOfLeadingZeros(Math.min(width, height));
    }

    // --------------------------------------------------------------------- //

    private record GuiCursorRenderState(TerminalRenderer renderer, Matrix3x2f pose, TextureSetup textureSetup,
                                        ScreenRectangle bounds) implements GuiElementRenderState {
        @Override
        public void buildVertices(final VertexConsumer consumer) {
            renderer.buildCursor(new Matrix4f().mul(pose), consumer);
        }

        @Override
        public RenderPipeline pipeline() {
            return ModShaders.GUI_TERMINAL_CURSOR_PIPELINE;
        }

        @Nullable
        @Override
        public ScreenRectangle scissorArea() {
            return null;
        }
    }

    private static final class TargetTexture extends AbstractTexture {
        private final GpuTextureView[] levelViews;

        private TargetTexture(final String label, final int width, final int height) {
            final GpuDevice device = RenderSystem.getDevice();
            final int levels = mipLevelCount(width, height);
            texture = device.createTexture(label, GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8, width, height, 1, levels);
            textureView = device.createTextureView(texture);
            levelViews = new GpuTextureView[levels];
            for (int level = 0; level < levels; level++) {
                levelViews[level] = device.createTextureView(texture, level, 1);
            }
            sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
        }

        @Override
        public void close() {
            for (final GpuTextureView view : levelViews) {
                view.close();
            }
            super.close();
        }
    }

    private static final class MipmapChain {
        @Nullable
        private static MipmapChain instance;

        private final GpuTexture[] levels;
        private final GpuTextureView[] views;
        private final GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        private MipmapChain(final int width, final int height) {
            final GpuDevice device = RenderSystem.getDevice();
            final int count = mipLevelCount(width, height);
            levels = new GpuTexture[count];
            views = new GpuTextureView[count];
            for (int level = 0; level < count; level++) {
                levels[level] = device.createTexture("oc2 terminal mipmap " + level,
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                    TextureFormat.RGBA8, width >> level, height >> level, 1, 1);
                views[level] = device.createTextureView(levels[level]);
            }
        }

        private static MipmapChain get(final int width, final int height) {
            if (instance == null || instance.levels[0].getWidth(0) != width || instance.levels[0].getHeight(0) != height) {
                if (instance != null) {
                    instance.close();
                }
                instance = new MipmapChain(width, height);
            }
            return instance;
        }

        private void downsample(final CommandEncoder encoder) {
            for (int level = 1; level < views.length; level++) {
                blit(encoder, views[level - 1], views[level]);
            }
        }

        private void copyTo(final CommandEncoder encoder, final GpuTextureView[] targets) {
            for (int level = 0; level < views.length; level++) {
                blit(encoder, views[level], targets[level]);
            }
        }

        private void close() {
            for (int level = 0; level < levels.length; level++) {
                views[level].close();
                levels[level].close();
            }
        }

        private void blit(final CommandEncoder encoder, final GpuTextureView source, final GpuTextureView target) {
            try (RenderPass pass = encoder.createRenderPass(() -> "oc2 terminal mipmap", target, OptionalInt.empty())) {
                pass.setPipeline(ModShaders.TERMINAL_DOWNSAMPLE_PIPELINE);
                pass.bindTexture("InSampler", source, sampler);
                pass.draw(0, 3);
            }
        }
    }
}
