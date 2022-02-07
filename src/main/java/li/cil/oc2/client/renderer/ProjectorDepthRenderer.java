/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.ProjectorDevice;
import li.cil.oc2.common.ext.MinecraftExt;
import li.cil.oc2.common.util.FakePlayerUtils;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.Yuv420jToRgb;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

// No @Mod.EventBusSubscriber: we need to register this manually, because static init throws errors when running data generation.
public final class ProjectorDepthRenderer {
    private static final int DEPTH_CAPTURE_SIZE = 256;

    private static final List<ProjectorBlockEntity> VISIBLE_PROJECTORS = new ArrayList<>();
    private static final DepthOnlyRenderTarget[] PROJECTOR_DEPTH_TARGETS = new DepthOnlyRenderTarget[ModShaders.MAX_PROJECTORS];
    private static final DynamicTexture[] PROJECTOR_COLOR_TARGETS = new DynamicTexture[ModShaders.MAX_PROJECTORS];
    private static final Matrix4f[] PROJECTOR_CAMERA_MATRICES = new Matrix4f[ModShaders.MAX_PROJECTORS];
    private static final Camera PROJECTOR_DEPTH_CAMERA = new Camera();
    private static final DepthOnlyRenderTarget MAIN_CAMERA_DEPTH = new DepthOnlyRenderTarget(MainTarget.DEFAULT_WIDTH, MainTarget.DEFAULT_HEIGHT);
    private static final float PROJECTOR_FORWARD_SHIFT = 7 / 16f; // From center of projector block.
    private static final float PROJECTOR_NEAR = 0.5f - PROJECTOR_FORWARD_SHIFT;
    private static final float PROJECTOR_FAR = ProjectorBlockEntity.MAX_RENDER_DISTANCE;
    private static final int HALF_FRUSTUM_WIDTH = (ProjectorBlockEntity.MAX_WIDTH - 1) / 2;
    private static final int FRUSTUM_HEIGHT = ProjectorBlockEntity.MAX_HEIGHT - 1;
    private static final Matrix4f DEPTH_CAMERA_PROJECTION_MATRIX = getFrustumMatrix(
        PROJECTOR_NEAR, PROJECTOR_FAR,
        ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE,
        -HALF_FRUSTUM_WIDTH, HALF_FRUSTUM_WIDTH,
        FRUSTUM_HEIGHT, 0);

    private static final Cache<ProjectorBlockEntity, RenderInfo> RENDER_INFO = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(5))
        .removalListener(ProjectorDepthRenderer::handleProjectorNoLongerRendering)
        .build();

    private static boolean isRenderingProjectorDepth;
    private static HitResult hitResultBak;
    private static boolean entityShadowsBak;

    private static void handleProjectorNoLongerRendering(final RemovalNotification<ProjectorBlockEntity, RenderInfo> notification) {
        final ProjectorBlockEntity projector = notification.getKey();
        if (projector != null) {
            projector.setFrameConsumer(null);
        }
        final RenderInfo renderInfo = notification.getValue();
        if (renderInfo != null) {
            renderInfo.close();
        }
    }

    static {
        for (int i = 0; i < ModShaders.MAX_PROJECTORS; i++) {
            PROJECTOR_DEPTH_TARGETS[i] = new DepthOnlyRenderTarget(DEPTH_CAPTURE_SIZE, DEPTH_CAPTURE_SIZE);
            PROJECTOR_CAMERA_MATRICES[i] = new Matrix4f();
        }
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * Adds a projector that is being rendered this frame. This is called every frame a projector is rendering,
     * the list of rendering projectors is cleared at the end of every frame.
     */
    public static void addProjector(final ProjectorBlockEntity projector) {
        VISIBLE_PROJECTORS.add(projector);
    }

    /**
     * Whether we're currently rendering projector depth maps.
     * <p>
     * This is used in a couple of events and mixins, used to suppress regular rendering of things not needed in the
     * depth buffer.
     */
    public static boolean isIsRenderingProjectorDepth() {
        return isRenderingProjectorDepth;
    }

    /**
     * Called from a mixin in the {@link LevelRenderer#renderLevel(PoseStack, float, long, boolean, Camera, GameRenderer, LightTexture, Matrix4f)}
     * method to grab the current depth buffer. This is necessary, because the depth buffer may be messed up by other
     * render passes when using the "Fabulous!" graphics mode.
     * <p>
     * Called before {@link #handleRenderLevelLast(RenderLevelLastEvent)} every frame.
     */
    public static void captureMainCameraDepth() {
        final RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
        if (mainRenderTarget.width != MAIN_CAMERA_DEPTH.width || mainRenderTarget.height != MAIN_CAMERA_DEPTH.height) {
            MAIN_CAMERA_DEPTH.resize(mainRenderTarget.width, mainRenderTarget.height, Minecraft.ON_OSX);
        }
        if (mainRenderTarget.isStencilEnabled()) {
            MAIN_CAMERA_DEPTH.enableStencil();
        }
        MAIN_CAMERA_DEPTH.copyDepthFrom(mainRenderTarget);
        mainRenderTarget.bindWrite(false);
    }

    /**
     * Renders the projected images of {@link ProjectorBlockEntity} instances that were registered via
     * {@link #addProjector(ProjectorBlockEntity)} this frame.
     */
    @SubscribeEvent
    public static void handleRenderLevelLast(final RenderLevelLastEvent event) {
        if (VISIBLE_PROJECTORS.isEmpty()) {
            return;
        }
        try {
            final Minecraft minecraft = Minecraft.getInstance();
            final ClientLevel level = minecraft.level;
            final LocalPlayer player = minecraft.player;
            if (level == null || player == null) {
                return;
            }

            VISIBLE_PROJECTORS.sort((projector1, projector2) -> {
                final double distance1 = player.distanceToSqr(Vec3.atCenterOf(projector1.getBlockPos()));
                final double distance2 = player.distanceToSqr(Vec3.atCenterOf(projector2.getBlockPos()));
                return Double.compare(distance1, distance2);
            });

            final int projectorCount = Math.min(VISIBLE_PROJECTORS.size(), ModShaders.MAX_PROJECTORS);
            renderProjectorDepths(minecraft, level, player, event.getPartialTick(), event.getStartNanos(), projectorCount);
            renderProjectorColors(minecraft, event.getPoseStack().last().pose(), event.getProjectionMatrix(), projectorCount);
        } finally {
            VISIBLE_PROJECTORS.clear();
        }
    }

    /**
     * Suppresses fog rendering while rendering depth buffer for projectors.
     */
    @SubscribeEvent
    public static void handleFog(final EntityViewRenderEvent.RenderFogEvent event) {
        if (isRenderingProjectorDepth) {
            FogRenderer.setupNoFog();
        }
    }

    /**
     * Suppresses nameplate rendering while rendering depth buffer for projectors.
     */
    @SubscribeEvent
    public static void handleNameplate(final RenderNameplateEvent event) {
        if (isRenderingProjectorDepth) {
            event.setResult(Event.Result.DENY);
        }
    }

    /**
     * Updates cached rendering info, such as textures holding image data for projectors, to allow expiration.
     */
    @SubscribeEvent
    public static void handleClientTick(final TickEvent.ClientTickEvent event) {
        RENDER_INFO.cleanUp();
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * Stage one of projector rendering, render scene depths from the perspective of all projectors that should
     * bre rendered. The output is the list of depth buffers, MVP matrices that were used to render them, and the
     * associated color texture for the projector.
     */
    private static void renderProjectorDepths(final Minecraft minecraft, final ClientLevel level, final Player player,
                                              final float partialTicks, final long startNanos,
                                              final int projectorCount) {
        prepareDepthBufferRendering(minecraft);
        try {
            final PoseStack viewModelStack = new PoseStack();
            for (int projectorIndex = 0; projectorIndex < projectorCount; projectorIndex++) {
                final ProjectorBlockEntity projector = VISIBLE_PROJECTORS.get(projectorIndex);
                final Direction facing = projector.getBlockState().getValue(ProjectorBlock.FACING);
                final Vec3 projectorPos = Vec3
                    .atCenterOf(projector.getBlockPos())
                    .add(new Vec3(facing.step()).scale(PROJECTOR_FORWARD_SHIFT));

                configureProjectorDepthCamera(level, projectorPos, facing.toYRot());

                RenderSystem.setProjectionMatrix(DEPTH_CAMERA_PROJECTION_MATRIX);
                setupViewModelMatrix(viewModelStack);

                storeProjectorMatrix(projectorIndex, minecraft, projectorPos, viewModelStack);

                bindProjectorDepthRenderTarget(projectorIndex, minecraft);

                renderProjectorDepthBuffer(minecraft, partialTicks, startNanos, viewModelStack);

                storeProjectorColorBuffer(projectorIndex, projector);

                projector.onRendering();
            }
        } finally {
            finishDepthBufferRendering(minecraft, player);
        }
    }

    private static void prepareDepthBufferRendering(final Minecraft minecraft) {
        isRenderingProjectorDepth = true;

        // Suppresses hit outlines being rendered.
        hitResultBak = minecraft.hitResult;
        minecraft.hitResult = null;

        // Skip shadow rendering for perf.
        entityShadowsBak = minecraft.options.entityShadows;
        minecraft.options.entityShadows = false;

        minecraft.setCameraEntity(PROJECTOR_DEPTH_CAMERA.getEntity());

        RenderSystem.backupProjectionMatrix();
    }

    private static void finishDepthBufferRendering(final Minecraft minecraft, final Player player) {
        minecraft.hitResult = hitResultBak;
        minecraft.options.entityShadows = entityShadowsBak;

        RenderSystem.restoreProjectionMatrix();

        ((MinecraftExt) minecraft).setMainRenderTargetOverride(null);
        minecraft.getMainRenderTarget().bindWrite(true);

        minecraft.setCameraEntity(player);

        isRenderingProjectorDepth = false;
    }

    private static void configureProjectorDepthCamera(final ClientLevel level, final Vec3 pos, final float rotationY) {
        PROJECTOR_DEPTH_CAMERA.setup(level, ProjectorCameraEntity.get(level, pos, rotationY), false, false, 0);
    }

    private static void setupViewModelMatrix(final PoseStack viewModelStack) {
        viewModelStack.setIdentity();
        viewModelStack.mulPose(Vector3f.YP.rotationDegrees(PROJECTOR_DEPTH_CAMERA.getYRot() + 180));

        final Matrix3f viewRotationMatrix = viewModelStack.last().normal().copy();
        if (viewRotationMatrix.invert()) {
            RenderSystem.setInverseViewRotationMatrix(viewRotationMatrix);
        }
    }

    private static void storeProjectorMatrix(final int projectorIndex, final Minecraft minecraft, final Vec3 projectorPos, final PoseStack viewModelStack) {
        final Camera mainCamera = minecraft.gameRenderer.getMainCamera();
        final Vec3 mainCameraPosition = mainCamera.getPosition();

        // Save model-view-projection matrix for mapping in compositing shader. We use the position relative to the
        // main camera here, so that the main camera can sit at the origin. This avoids loss of precision.
        PROJECTOR_CAMERA_MATRICES[projectorIndex].load(DEPTH_CAMERA_PROJECTION_MATRIX);
        viewModelStack.pushPose();
        viewModelStack.translate(
            mainCameraPosition.x() - projectorPos.x(),
            mainCameraPosition.y() - projectorPos.y(),
            mainCameraPosition.z() - projectorPos.z()
        );
        PROJECTOR_CAMERA_MATRICES[projectorIndex].multiply(viewModelStack.last().pose());
        viewModelStack.popPose();
    }

    private static void bindProjectorDepthRenderTarget(final int projectorIndex, final Minecraft minecraft) {
        final DepthOnlyRenderTarget projectorDepthTarget = PROJECTOR_DEPTH_TARGETS[projectorIndex];
        projectorDepthTarget.bindWrite(true);
        ((MinecraftExt) minecraft).setMainRenderTargetOverride(projectorDepthTarget);
    }

    private static void renderProjectorDepthBuffer(final Minecraft minecraft, final float partialTicks, final long startNanos, final PoseStack viewModelStack) {
        final LevelRenderer levelRenderer = minecraft.levelRenderer;
        levelRenderer.prepareCullFrustum(
            viewModelStack,
            PROJECTOR_DEPTH_CAMERA.getPosition(),
            DEPTH_CAMERA_PROJECTION_MATRIX
        );
        levelRenderer.renderLevel(
            viewModelStack,
            partialTicks,
            startNanos,
            /* shouldRenderBlockOutline: */ false,
            PROJECTOR_DEPTH_CAMERA,
            minecraft.gameRenderer,
            minecraft.gameRenderer.lightTexture(),
            DEPTH_CAMERA_PROJECTION_MATRIX
        );
    }

    private static void storeProjectorColorBuffer(final int projectorIndex, final ProjectorBlockEntity projector) {
        PROJECTOR_COLOR_TARGETS[projectorIndex] = getRenderInfo(projector).texture();
    }

    /**
     * Stage two or projector rendering, this composes the projections of the projectors being rendered into the
     * main render target, using the camera matrices and depth information to determine where to render. This is
     * essentially a post-processing pass, i.e. it renders a screen-filling rectangle blending the projector light
     * into the existing main render target output.
     */
    private static void renderProjectorColors(final Minecraft minecraft, final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final int projectorCount) {
        prepareColorBufferRendering();

        try {
            prepareOrthographicRendering(minecraft);

            RenderSystem.setShader(ModShaders::getProjectorsShader);
            ModShaders.configureProjectorsShader(
                MAIN_CAMERA_DEPTH,
                constructInverseMainCameraMatrix(modelViewMatrix, projectionMatrix),
                PROJECTOR_COLOR_TARGETS,
                PROJECTOR_DEPTH_TARGETS,
                PROJECTOR_CAMERA_MATRICES,
                projectorCount
            );

            renderIntoScreenRect();
        } finally {
            finishColorBufferRendering();
        }
    }

    private static void prepareColorBufferRendering() {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.getModelViewStack().pushPose();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

        RenderSystem.colorMask(true, true, true, false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    private static void finishColorBufferRendering() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableBlend();

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static void prepareOrthographicRendering(final Minecraft minecraft) {
        final Matrix4f screenProjectionMatrix = Matrix4f.orthographic(
            minecraft.getWindow().getWidth(),
            -minecraft.getWindow().getHeight(),
            1000, 3000
        );
        RenderSystem.setProjectionMatrix(screenProjectionMatrix);

        final PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.setIdentity();
        modelViewStack.translate(0, 0, -2000);
        RenderSystem.applyModelViewMatrix();
    }

    private static Matrix4f constructInverseMainCameraMatrix(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix) {
        final Matrix4f inverseModelViewMatrix = projectionMatrix.copy();
        inverseModelViewMatrix.multiply(modelViewMatrix);
        inverseModelViewMatrix.invert();
        return inverseModelViewMatrix;
    }

    private static void renderIntoScreenRect() {
        final Tesselator tesselator = Tesselator.getInstance();
        final BufferBuilder builder = tesselator.getBuilder();

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.vertex(0, 0, 0).uv(0, 1).endVertex();
        builder.vertex(0, MAIN_CAMERA_DEPTH.height, 0).uv(0, 0).endVertex();
        builder.vertex(MAIN_CAMERA_DEPTH.width, MAIN_CAMERA_DEPTH.height, 0).uv(1, 0).endVertex();
        builder.vertex(MAIN_CAMERA_DEPTH.width, 0, 0).uv(1, 1).endVertex();
        tesselator.end();
    }

    private static Matrix4f getFrustumMatrix(final float near, final float far, final float dist,
                                             final float left, final float right,
                                             final float top, final float bottom) {
        return new Matrix4f(new float[]{
            2 * dist / (right - left), 0, (right + left) / (right - left), 0,
            0, 2 * dist / (top - bottom), (top + bottom) / (top - bottom), 0,
            0, 0, -(far + near) / (far - near), -(2 * far * near) / (far - near),
            0, 0, -1, 0,
        });
    }

    private static RenderInfo getRenderInfo(final ProjectorBlockEntity projector) {
        try {
            return RENDER_INFO.get(projector, () -> {
                final DynamicTexture texture = new DynamicTexture(ProjectorDevice.WIDTH, ProjectorDevice.HEIGHT, false);
                final RenderInfo renderInfo = new RenderInfo(texture);
                projector.setFrameConsumer(renderInfo);
                return renderInfo;
            });
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////

    /**
     * Tracks a render texture holding color info for rendering projectors.
     * <p>
     * Automatically updated by projectors when new data arrives (from a worker thread).
     */
    private record RenderInfo(DynamicTexture texture) implements ProjectorBlockEntity.FrameConsumer {
        private static final ThreadLocal<byte[]> RGB = ThreadLocal.withInitial(() -> new byte[3]);

        public synchronized void close() {
            texture.close();
        }

        @Override
        public synchronized void processFrame(final Picture picture) {
            final NativeImage image = texture.getPixels();
            if (image == null) {
                return;
            }

            final byte[] y = picture.getPlaneData(0);
            final byte[] u = picture.getPlaneData(1);
            final byte[] v = picture.getPlaneData(2);

            // Convert in quads, based on the half resolution of UV. As such, skip every other row, since
            // we're setting the current and the next.
            int lumaIndex = 0, chromaIndex = 0;
            for (int halfRow = 0; halfRow < ProjectorDevice.HEIGHT / 2; halfRow++, lumaIndex += ProjectorDevice.WIDTH * 2) {
                final int row = halfRow * 2;
                for (int halfCol = 0; halfCol < ProjectorDevice.WIDTH / 2; halfCol++, chromaIndex++) {
                    final int col = halfCol * 2;
                    final int yIndex = lumaIndex + col;
                    final byte cb = u[chromaIndex];
                    final byte cr = v[chromaIndex];
                    setFromYUV420(image, col, row, y[yIndex], cb, cr);
                    setFromYUV420(image, col + 1, row, y[yIndex + 1], cb, cr);
                    setFromYUV420(image, col, row + 1, y[yIndex + ProjectorDevice.WIDTH], cb, cr);
                    setFromYUV420(image, col + 1, row + 1, y[yIndex + ProjectorDevice.WIDTH + 1], cb, cr);
                }
            }

            texture.upload();
        }

        private static void setFromYUV420(final NativeImage image, final int col, final int row, final byte y, final byte cb, final byte cr) {
            final byte[] bytes = RGB.get();
            Yuv420jToRgb.YUVJtoRGB(y, cb, cr, bytes, 0);
            final int r = bytes[0] + 128;
            final int g = bytes[1] + 128;
            final int b = bytes[2] + 128;
            image.setPixelRGBA(col, row, r | (g << 8) | (b << 16) | (0xFF << 24));
        }
    }

    /**
     * Optimized texture target that doesn't have a color texture, so we can completely skip that when rendering
     * projector depth buffers.
     */
    private static final class DepthOnlyRenderTarget extends TextureTarget {
        public DepthOnlyRenderTarget(final int width, final int height) {
            super(width, height, true, Minecraft.ON_OSX);
        }

        @Override
        public void createBuffers(final int width, final int height, final boolean isOnOSX) {
            super.createBuffers(width, height, isOnOSX);
            if (colorTextureId > -1) {
                if (frameBufferId > -1) {
                    glBindFramebuffer(GL_FRAMEBUFFER, frameBufferId);
                    glDrawBuffer(GL_NONE);
                    glBindFramebuffer(GL_FRAMEBUFFER, 0);
                }
                TextureUtil.releaseTextureId(this.colorTextureId);
                this.colorTextureId = -1;
            }
        }
    }

    /**
     * Fake entity used as rendering context when rendering projector depth buffers.
     */
    private static final class ProjectorCameraEntity extends Player {
        private static ProjectorCameraEntity instance;

        /**
         * Singleton getter for the fake render entity, to avoid unnecessary allocations.
         */
        public static ProjectorCameraEntity get(final Level level, final Vec3 pos, final float rotationY) {
            if (instance == null) {
                instance = new ProjectorCameraEntity(level, BlockPos.ZERO, rotationY);
            }

            instance.level = level;
            instance.moveTo(pos.x(), pos.y(), pos.z(), rotationY, 0);

            return instance;
        }

        private ProjectorCameraEntity(final Level level, final BlockPos blockPos, final float rotationY) {
            super(level, blockPos, rotationY, FakePlayerUtils.getFakePlayerProfile());
        }

        @Override
        public float getViewYRot(final float partialTicks) {
            return yRotO;
        }

        @Override
        public float getViewXRot(final float partialTicks) {
            return xRotO;
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return true;
        }
    }
}
