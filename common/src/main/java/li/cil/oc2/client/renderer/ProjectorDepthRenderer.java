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
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.architectury.event.events.client.ClientTickEvent;
import li.cil.oc2.client.ClientPlatform;
import li.cil.oc2.common.block.ProjectorBlock;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.ProjectorDevice;
import li.cil.oc2.common.ext.MinecraftExt;
import li.cil.oc2.common.util.FakePlayerUtils;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.Yuv420jToRgb;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

// No @Mod.EventBusSubscriber: we need to register this manually, because static init throws errors when running data generation.
public final class ProjectorDepthRenderer {
    private static final int DEPTH_CAPTURE_SIZE = 256;
    private static final int DEPTH_BUFFER_SOURCE_BYTES = 768 * 1024;
    private static final float BASE_EMISSIVE_STRENGTH = 1.0f;
    private static final float SHADER_EMISSIVE_STRENGTH = 3.0f;

    private static final List<ProjectorBlockEntity> VISIBLE_PROJECTORS = new ArrayList<>();
    private static final LazyDepthOnlyRenderBuffer[] PROJECTOR_DEPTH_BUFFER_LEDGER = new LazyDepthOnlyRenderBuffer[ModShaders.MAX_PROJECTORS];
    private static final DepthOnlyRenderTarget[] PROJECTOR_DEPTH_BUFFERS = new DepthOnlyRenderTarget[ModShaders.MAX_PROJECTORS];
    private static final DynamicTexture[] PROJECTOR_COLOR_BUFFERS = new DynamicTexture[ModShaders.MAX_PROJECTORS];
    private static final Matrix4f[] PROJECTOR_CAMERA_MATRICES = new Matrix4f[ModShaders.MAX_PROJECTORS];
    private static final Camera PROJECTOR_DEPTH_CAMERA = new Camera();
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
    private static final Matrix4f MODEL_VIEW_MATRIX = new Matrix4f();
    private static final Matrix4f PROJECTION_MATRIX = new Matrix4f();

    private static final Cache<ProjectorBlockEntity, RenderInfo> RENDER_INFO = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(5))
        .removalListener(ProjectorDepthRenderer::handleProjectorNoLongerRendering)
        .build();

    private static DepthOnlyRenderTarget mainCameraDepth;
    private static RenderTarget activeProjectorDepthTarget;
    private static MultiBufferSource.BufferSource depthBufferSource;

    private static ProjectorRenderPath currentPath;
    private static int pendingRenderCount;
    private static int refreshSlot;
    private static boolean isRenderingProjectorDepth;
    private static HitResult hitResultBak;
    private static boolean entityShadowsBak;
    private static Entity minecraftCameraEntityBak;
    private static Camera gameRendererMainCameraBak;
    private static float shaderFogStartBak;
    private static float shaderFogEndBak;
    private static final float[] SHADER_FOG_COLOR_BAK = new float[4];
    private static FogShape shaderFogShapeBak;

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
            PROJECTOR_CAMERA_MATRICES[i] = new Matrix4f();
        }
    }

    private static LazyDepthOnlyRenderBuffer[] projectorDepthBuffers() {
        if (PROJECTOR_DEPTH_BUFFER_LEDGER[0] == null) {
            for (int i = 0; i < ModShaders.MAX_PROJECTORS; i++) {
                PROJECTOR_DEPTH_BUFFER_LEDGER[i] = new LazyDepthOnlyRenderBuffer();
            }
        }
        return PROJECTOR_DEPTH_BUFFER_LEDGER;
    }

    private static DepthOnlyRenderTarget mainCameraDepth() {
        if (mainCameraDepth == null) {
            mainCameraDepth = new DepthOnlyRenderTarget(MainTarget.DEFAULT_WIDTH, MainTarget.DEFAULT_HEIGHT);
        }
        return mainCameraDepth;
    }

    // --------------------------------------------------------------------- //

    public static void addProjector(final ProjectorBlockEntity projector) {
        VISIBLE_PROJECTORS.add(projector);
    }

    public static boolean isRenderingProjectorDepth() {
        return isRenderingProjectorDepth;
    }

    public static void bindProjectorDepthTarget() {
        if (activeProjectorDepthTarget != null) {
            glBindFramebuffer(GL_FRAMEBUFFER, activeProjectorDepthTarget.frameBufferId);
        }
    }

    public static void onBeforeTransparencyChain(final MultiBufferSource.BufferSource bufferSource) {
        path().beforeTransparencyChain(bufferSource);
    }

    public static void onBeforeTranslucentTerrain(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final DeltaTracker deltaTracker) {
        path().beforeTranslucentTerrain(modelViewMatrix, projectionMatrix, deltaTracker);
    }

    public static void onAfterParticles(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final DeltaTracker deltaTracker) {
        path().afterParticles(modelViewMatrix, projectionMatrix, deltaTracker);
    }

    public static void onAfterLevel() {
        try {
            path().afterLevel();
        } finally {
            currentPath = null;
            pendingRenderCount = 0;
            Arrays.fill(PROJECTOR_COLOR_BUFFERS, null);
        }
    }

    public static void initialize() {
        ClientTickEvent.CLIENT_POST.register(minecraft -> RENDER_INFO.cleanUp());
    }

    // --------------------------------------------------------------------- //

    private static ProjectorRenderPath path() {
        if (currentPath == null) {
            currentPath = ShaderPackCompat.isShaderPackRendering()
                ? ProjectorRenderPath.SHADER_PACK
                : Minecraft.useShaderTransparency()
                ? ProjectorRenderPath.FABULOUS
                : ProjectorRenderPath.DEFAULT;
        }
        return currentPath;
    }

    private static boolean hasVisibleProjectors() {
        return !VISIBLE_PROJECTORS.isEmpty();
    }

    private static boolean hasPendingProjectors() {
        return pendingRenderCount > 0;
    }

    private static boolean renderProjectorDepths(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final DeltaTracker deltaTracker) {
        if (VISIBLE_PROJECTORS.isEmpty()) {
            return false;
        }

        try {
            final Minecraft minecraft = Minecraft.getInstance();
            final ClientLevel level = minecraft.level;
            final LocalPlayer player = minecraft.player;
            if (level == null || player == null) {
                return false;
            }

            VISIBLE_PROJECTORS.sort((projector1, projector2) -> {
                final double distance1 = player.distanceToSqr(Vec3.atCenterOf(projector1.getBlockPos()));
                final double distance2 = player.distanceToSqr(Vec3.atCenterOf(projector2.getBlockPos()));
                return Double.compare(distance1, distance2);
            });

            final int projectorCount = Math.min(VISIBLE_PROJECTORS.size(), ModShaders.MAX_PROJECTORS);
            for (int i = 0; i < projectorCount; i++) {
                VISIBLE_PROJECTORS.get(i).onRendering();
            }

            pendingRenderCount = updateProjectorDepths(minecraft, level, deltaTracker, projectorCount);
            MODEL_VIEW_MATRIX.set(modelViewMatrix);
            PROJECTION_MATRIX.set(projectionMatrix);

            return hasPendingProjectors();
        } finally {
            VISIBLE_PROJECTORS.clear();
        }
    }

    private static void flushDepthWritingSheets(final MultiBufferSource.BufferSource bufferSource) {
        bufferSource.endBatch(Sheets.translucentCullBlockSheet());
        bufferSource.endBatch(Sheets.bannerSheet());
        bufferSource.endBatch(Sheets.shieldSheet());
    }

    private static void compositeIntoMainTarget() {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.getMainRenderTarget().bindWrite(true);
        renderProjector(minecraft, pendingRenderCount, BASE_EMISSIVE_STRENGTH);
    }

    private static void compositeIntoShaderTarget() {
        final Minecraft minecraft = Minecraft.getInstance();
        final int previousFrameBuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        mainCameraDepth().bindWrite(false);
        if (!ShaderPackCompat.bindShaderPackGBuffer()) {
            glBindFramebuffer(GL_FRAMEBUFFER, previousFrameBuffer);
            minecraft.getMainRenderTarget().bindWrite(false);
            return;
        }

        try {
            renderProjector(minecraft, pendingRenderCount, SHADER_EMISSIVE_STRENGTH);
        } finally {
            glBindFramebuffer(GL_FRAMEBUFFER, previousFrameBuffer);
            minecraft.getMainRenderTarget().bindWrite(false);
        }
    }

    private static void captureMainCameraDepth() {
        final RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
        if (mainRenderTarget.width != mainCameraDepth().width || mainRenderTarget.height != mainCameraDepth().height) {
            mainCameraDepth().resize(mainRenderTarget.width, mainRenderTarget.height, Minecraft.ON_OSX);
        }
        if (ClientPlatform.isStencilEnabled(mainRenderTarget)) {
            ClientPlatform.enableStencil(mainCameraDepth());
        } else if (ClientPlatform.isStencilEnabled(mainCameraDepth())) {
            mainCameraDepth().destroyBuffers();
            mainCameraDepth = new DepthOnlyRenderTarget(mainRenderTarget.width, mainRenderTarget.height);
        }
        mainCameraDepth().copyDepthFrom(mainRenderTarget);
        mainRenderTarget.bindWrite(false);
    }

    private static int updateProjectorDepths(final Minecraft minecraft, final ClientLevel level,
                                             final DeltaTracker deltaTracker, final int projectorCount) {
        final Vec3 mainCameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        final PoseStack viewModelStack = new PoseStack();

        matchDepthTargetsToOwningProjectors(projectorCount);

        renderDepthBuffer(minecraft, level, deltaTracker, projectorCount, viewModelStack);

        final int validDepthTargetCount = sortByDepthBufferValidity(projectorCount);

        for (int i = 0; i < validDepthTargetCount; i++) {
            final ProjectorBlockEntity projector = VISIBLE_PROJECTORS.get(i);
            final Direction facing = projector.getBlockState().getValue(ProjectorBlock.FACING);
            final Vec3 projectorPos = Vec3
                .atCenterOf(projector.getBlockPos())
                .add(new Vec3(facing.step()).scale(PROJECTOR_FORWARD_SHIFT));

            configureProjectorDepthCamera(level, projectorPos, facing.toYRot());

            setupViewModelMatrix(viewModelStack);

            storeProjectorMatrix(i, projectorPos, mainCameraPosition, viewModelStack);

            storeProjectorBuffers(i, projector);
        }

        return validDepthTargetCount;
    }

    private static void matchDepthTargetsToOwningProjectors(final int projectorCount) {
        for (int i = 0; i < projectorCount; i++) {
            final ProjectorBlockEntity projector = VISIBLE_PROJECTORS.get(i);
            for (int j = i; j < ModShaders.MAX_PROJECTORS; j++) {
                if (projectorDepthBuffers()[j].isOwnedBy(projector)) {
                    swapDepthTargets(i, j);
                    break;
                }
            }
        }

        for (int i = 0; i < projectorCount; i++) {
            projectorDepthBuffers()[i].setOwner(VISIBLE_PROJECTORS.get(i));
        }
    }

    private static int sortByDepthBufferValidity(final int projectorCount) {
        int validDepthTargetCount = 0;

        for (int i = 0; i < projectorCount; i++) {
            if (!projectorDepthBuffers()[i].isValid) {
                continue;
            }

            Collections.swap(VISIBLE_PROJECTORS, i, validDepthTargetCount);
            swapDepthTargets(i, validDepthTargetCount);

            validDepthTargetCount++;
        }

        return validDepthTargetCount;
    }

    private static void swapDepthTargets(final int a, final int b) {
        if (a == b) {
            return;
        }

        final var target = PROJECTOR_DEPTH_BUFFER_LEDGER[a];
        PROJECTOR_DEPTH_BUFFER_LEDGER[a] = PROJECTOR_DEPTH_BUFFER_LEDGER[b];
        PROJECTOR_DEPTH_BUFFER_LEDGER[b] = target;
    }

    private static void renderDepthBuffer(final Minecraft minecraft, final ClientLevel level,
                                          final DeltaTracker deltaTracker, final int projectorCount,
                                          final PoseStack viewModelStack) {
        int projectorIndex = getNextDepthTargetSlotToRender(projectorCount);
        if (projectorIndex < 0) {
            return;
        }

        final ProjectorBlockEntity projector = VISIBLE_PROJECTORS.get(projectorIndex);
        final Direction facing = projector.getBlockState().getValue(ProjectorBlock.FACING);
        final Vec3 projectorPos = Vec3
            .atCenterOf(projector.getBlockPos())
            .add(new Vec3(facing.step()).scale(PROJECTOR_FORWARD_SHIFT));

        try {
            prepareDepthBufferRendering(minecraft, level, deltaTracker.getGameTimeDeltaPartialTick(false));

            configureProjectorDepthCamera(level, projectorPos, facing.toYRot());

            RenderSystem.setProjectionMatrix(DEPTH_CAMERA_PROJECTION_MATRIX, VertexSorting.DISTANCE_TO_ORIGIN);
            setupViewModelMatrix(viewModelStack);

            bindProjectorDepthRenderTarget(projectorIndex, minecraft);

            renderProjectorDepthBuffer(minecraft, level, deltaTracker, viewModelStack);
        } finally {
            finishDepthBufferRendering(minecraft);
        }
    }

    private static int getNextDepthTargetSlotToRender(final int projectorCount) {
        refreshSlot = (refreshSlot + 1) % ModShaders.MAX_PROJECTORS;
        return refreshSlot < projectorCount ? refreshSlot : -1;
    }

    private static void prepareDepthBufferRendering(final Minecraft minecraft, final ClientLevel level, final float partialTicks) {
        isRenderingProjectorDepth = true;

        // Suppresses hit outlines being rendered.
        hitResultBak = minecraft.hitResult;
        minecraft.hitResult = null;

        // Skip shadow rendering for perf.
        entityShadowsBak = minecraft.options.entityShadows().get();
        minecraft.options.entityShadows().set(false);

        minecraftCameraEntityBak = minecraft.getCameraEntity();
        minecraft.setCameraEntity(ProjectorCameraEntity.get(level, Vec3.ZERO, partialTicks));
        gameRendererMainCameraBak = minecraft.gameRenderer.mainCamera;
        minecraft.gameRenderer.mainCamera = PROJECTOR_DEPTH_CAMERA;
        prepareRenderDispatchers(minecraft, PROJECTOR_DEPTH_CAMERA);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.getModelViewStack().pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();

        backupFog();
    }

    private static void finishDepthBufferRendering(final Minecraft minecraft) {
        isRenderingProjectorDepth = false;
        activeProjectorDepthTarget = null;

        minecraft.hitResult = hitResultBak;
        minecraft.options.entityShadows().set(entityShadowsBak);

        ((MinecraftExt) minecraft).setMainRenderTargetOverride(null);
        minecraft.getMainRenderTarget().bindWrite(true);

        minecraft.setCameraEntity(minecraftCameraEntityBak);
        minecraft.gameRenderer.mainCamera = gameRendererMainCameraBak;
        prepareRenderDispatchers(minecraft, gameRendererMainCameraBak);

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        restoreFog();
    }

    @SuppressWarnings("DataFlowIssue")
    private static void prepareRenderDispatchers(final Minecraft minecraft, final Camera camera) {
        // Same calls as in LevelRenderer::renderLevel() before it renders entities.
        minecraft.getBlockEntityRenderDispatcher().prepare(minecraft.level, camera, minecraft.hitResult);
        minecraft.getEntityRenderDispatcher().prepare(minecraft.level, camera, minecraft.crosshairPickEntity);
    }

    private static void backupFog() {
        shaderFogStartBak = RenderSystem.getShaderFogStart();
        shaderFogEndBak = RenderSystem.getShaderFogEnd();
        System.arraycopy(RenderSystem.getShaderFogColor(), 0, SHADER_FOG_COLOR_BAK, 0, SHADER_FOG_COLOR_BAK.length);
        shaderFogShapeBak = RenderSystem.getShaderFogShape();
    }

    private static void restoreFog() {
        RenderSystem.setShaderFogStart(shaderFogStartBak);
        RenderSystem.setShaderFogEnd(shaderFogEndBak);
        RenderSystem.setShaderFogColor(SHADER_FOG_COLOR_BAK[0], SHADER_FOG_COLOR_BAK[1], SHADER_FOG_COLOR_BAK[2], SHADER_FOG_COLOR_BAK[3]);
        RenderSystem.setShaderFogShape(shaderFogShapeBak);
    }

    // Manual level rendering a la LevelRenderer::renderLevel(), with the bits we need for the projector depth.
    private static void renderProjectorDepthBuffer(final Minecraft minecraft, final ClientLevel level, final DeltaTracker deltaTracker, final PoseStack viewModelStack) {
        final LevelRenderer levelRenderer = minecraft.levelRenderer;
        final Matrix4f frustumMatrix = viewModelStack.last().pose();
        final Vec3 cameraPosition = PROJECTOR_DEPTH_CAMERA.getPosition();
        final double cameraX = cameraPosition.x(), cameraY = cameraPosition.y(), cameraZ = cameraPosition.z();
        final float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);

        levelRenderer.prepareCullFrustum(cameraPosition, frustumMatrix, DEPTH_CAMERA_PROJECTION_MATRIX);
        final Frustum frustum = new Frustum(frustumMatrix, DEPTH_CAMERA_PROJECTION_MATRIX);
        frustum.prepare(cameraX, cameraY, cameraZ);

        levelRenderer.setupRender(PROJECTOR_DEPTH_CAMERA, frustum, false, false);

        levelRenderer.renderSectionLayer(RenderType.solid(), cameraX, cameraY, cameraZ, frustumMatrix, DEPTH_CAMERA_PROJECTION_MATRIX);
        levelRenderer.renderSectionLayer(RenderType.cutoutMipped(), cameraX, cameraY, cameraZ, frustumMatrix, DEPTH_CAMERA_PROJECTION_MATRIX);
        levelRenderer.renderSectionLayer(RenderType.cutout(), cameraX, cameraY, cameraZ, frustumMatrix, DEPTH_CAMERA_PROJECTION_MATRIX);

        bindActiveProjectorDepthTarget(minecraft);

        RenderSystem.getModelViewStack().pushMatrix().mul(frustumMatrix);
        RenderSystem.applyModelViewMatrix();
        try {
            final MultiBufferSource.BufferSource bufferSource = depthBufferSource();
            final PoseStack poseStack = new PoseStack();

            for (final Entity entity : level.entitiesForRendering()) {
                if (minecraft.getEntityRenderDispatcher().shouldRender(entity, frustum, cameraX, cameraY, cameraZ)) {
                    levelRenderer.renderEntity(entity, cameraX, cameraY, cameraZ, partialTicks, poseStack, bufferSource);
                }
            }
            bufferSource.endBatch();

            renderBlockEntities(minecraft, level, frustum, poseStack, bufferSource, partialTicks, cameraX, cameraY, cameraZ);

            minecraft.particleEngine.render(minecraft.gameRenderer.lightTexture(), PROJECTOR_DEPTH_CAMERA, partialTicks);
            levelRenderer.renderSnowAndRain(minecraft.gameRenderer.lightTexture(), partialTicks, cameraX, cameraY, cameraZ);
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    // Same as in LevelRenderer::renderLevel() with the relevant entity subset (chunks in projector frustum).
    private static void renderBlockEntities(final Minecraft minecraft, final ClientLevel level, final Frustum frustum,
                                            final PoseStack poseStack, final MultiBufferSource.BufferSource bufferSource,
                                            final float partialTicks,
                                            final double cameraX, final double cameraY, final double cameraZ) {
        final AABB bounds = frustumBounds(cameraX, cameraY, cameraZ);
        final int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(bounds.minX));
        final int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxX));
        final int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.minZ));
        final int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxZ));
        final int minSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.minY));
        final int maxSectionY = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxY));
        final BlockEntityRenderDispatcher dispatcher = minecraft.getBlockEntityRenderDispatcher();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (final BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    final BlockPos pos = blockEntity.getBlockPos();
                    final int sectionY = SectionPos.blockToSectionCoord(pos.getY());
                    if (sectionY < minSectionY || sectionY > maxSectionY) {
                        continue;
                    }
                    if (!ClientPlatform.isBlockEntityVisible(dispatcher, blockEntity, frustum)) {
                        continue;
                    }

                    poseStack.pushPose();
                    poseStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                    dispatcher.render(blockEntity, partialTicks, poseStack, bufferSource);
                    poseStack.popPose();
                }
            }
        }

        bufferSource.endBatch();
    }

    private static void configureProjectorDepthCamera(final ClientLevel level, final Vec3 pos, final float rotationY) {
        PROJECTOR_DEPTH_CAMERA.setup(level, ProjectorCameraEntity.get(level, pos, rotationY), false, false, 0);
    }

    private static void setupViewModelMatrix(final PoseStack viewModelStack) {
        viewModelStack.setIdentity();
        viewModelStack.mulPose(Axis.YP.rotationDegrees(PROJECTOR_DEPTH_CAMERA.getYRot() + 180));
    }

    private static AABB frustumBounds(final double cameraX, final double cameraY, final double cameraZ) {
        final double reach = ProjectorBlockEntity.MAX_RENDER_DISTANCE;
        return new AABB(cameraX - reach, cameraY - reach, cameraZ - reach,
            cameraX + reach, cameraY + reach, cameraZ + reach);
    }

    private static MultiBufferSource.BufferSource depthBufferSource() {
        if (depthBufferSource == null) {
            depthBufferSource = MultiBufferSource.immediate(new ByteBufferBuilder(DEPTH_BUFFER_SOURCE_BYTES));
        }
        return depthBufferSource;
    }

    private static void bindProjectorDepthRenderTarget(final int projectorIndex, final Minecraft minecraft) {
        final var projectorDepthTarget = projectorDepthBuffers()[projectorIndex];
        RenderSystem.depthMask(true);
        RenderSystem.disableScissor();
        projectorDepthTarget.target.clear(Minecraft.ON_OSX);
        activeProjectorDepthTarget = projectorDepthTarget.target;
        bindActiveProjectorDepthTarget(minecraft);
        projectorDepthTarget.isValid = true; // I mean, it *will* be...
    }

    private static void bindActiveProjectorDepthTarget(final Minecraft minecraft) {
        ((MinecraftExt) minecraft).setMainRenderTargetOverride(null);
        activeProjectorDepthTarget.bindWrite(true);
        ((MinecraftExt) minecraft).setMainRenderTargetOverride(activeProjectorDepthTarget);
    }

    private static void storeProjectorMatrix(final int projectorIndex, final Vec3 projectorPos, final Vec3 mainCameraPosition, final PoseStack viewModelStack) {
        // Save model-view-projection matrix for mapping in compositing shader. We use the position relative to the
        // main camera here, so that the main camera can sit at the origin. This avoids loss of precision.
        PROJECTOR_CAMERA_MATRICES[projectorIndex].set(DEPTH_CAMERA_PROJECTION_MATRIX);
        viewModelStack.pushPose();
        viewModelStack.translate(
            mainCameraPosition.x() - projectorPos.x(),
            mainCameraPosition.y() - projectorPos.y(),
            mainCameraPosition.z() - projectorPos.z()
        );
        PROJECTOR_CAMERA_MATRICES[projectorIndex].mul(viewModelStack.last().pose());
        viewModelStack.popPose();
    }

    private static void storeProjectorBuffers(final int projectorIndex, final ProjectorBlockEntity projector) {
        PROJECTOR_COLOR_BUFFERS[projectorIndex] = getColorBuffer(projector);
        PROJECTOR_DEPTH_BUFFERS[projectorIndex] = projectorDepthBuffers()[projectorIndex].target;
    }

    private static void renderProjector(final Minecraft minecraft, final int renderCount, final float emissiveStrength) {
        prepareColorBufferRendering();
        try {
            prepareOrthographicRendering(minecraft);

            RenderSystem.setShader(ModShaders::getProjectorsShader);
            ModShaders.configureProjectorsShader(
                mainCameraDepth(),
                constructInverseMainCameraMatrix(),
                PROJECTOR_COLOR_BUFFERS,
                PROJECTOR_DEPTH_BUFFERS,
                PROJECTOR_CAMERA_MATRICES,
                renderCount,
                emissiveStrength
            );

            renderIntoScreenRect();
        } finally {
            finishColorBufferRendering();
        }
    }

    private static void prepareColorBufferRendering() {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.getModelViewStack().pushMatrix();

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
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void prepareOrthographicRendering(final Minecraft minecraft) {
        final Matrix4f screenProjectionMatrix = new Matrix4f().setOrtho(
            0, minecraft.getWindow().getWidth(),
            minecraft.getWindow().getHeight(), 0,
            1000, 3000
        );
        RenderSystem.setProjectionMatrix(screenProjectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);

        final Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.identity();
        modelViewStack.translate(0, 0, -2000);
        RenderSystem.applyModelViewMatrix();
    }

    private static Matrix4f constructInverseMainCameraMatrix() {
        final Matrix4f inverseModelViewMatrix = new Matrix4f(PROJECTION_MATRIX);
        inverseModelViewMatrix.mul(MODEL_VIEW_MATRIX);
        inverseModelViewMatrix.invert();
        return inverseModelViewMatrix;
    }

    private static void renderIntoScreenRect() {
        final BufferBuilder builder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        builder.addVertex(0, 0, 0).setUv(0, 1);
        builder.addVertex(0, mainCameraDepth().height, 0).setUv(0, 0);
        builder.addVertex(mainCameraDepth().width, mainCameraDepth().height, 0).setUv(1, 0);
        builder.addVertex(mainCameraDepth().width, 0, 0).setUv(1, 1);
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static Matrix4f getFrustumMatrix(final float near, final float far, final float dist,
                                             final float left, final float right,
                                             final float top, final float bottom) {
        return new Matrix4f().set(new float[]{
            2 * dist / (right - left), 0, 0, 0,
            0, 2 * dist / (top - bottom), 0, 0,
            (right + left) / (right - left), (top + bottom) / (top - bottom), -(far + near) / (far - near), -1,
            0, 0, -(2 * far * near) / (far - near), 0,
        });
    }

    private static DynamicTexture getColorBuffer(final ProjectorBlockEntity projector) {
        try {
            final RenderInfo renderInfo = RENDER_INFO.get(projector, () -> {
                final DynamicTexture texture = new DynamicTexture(ProjectorDevice.WIDTH, ProjectorDevice.HEIGHT, false);
                final RenderInfo info = new RenderInfo(texture);
                info.upload();
                projector.setFrameConsumer(info);
                return info;
            });

            renderInfo.uploadIfDirty();

            return renderInfo.texture();
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------------------------- //

    private enum ProjectorRenderPath {
        DEFAULT {
            @Override
            void afterParticles(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final DeltaTracker deltaTracker) {
                renderProjectorDepths(modelViewMatrix, projectionMatrix, deltaTracker);
            }

            @Override
            void afterLevel() {
                if (hasPendingProjectors()) {
                    captureMainCameraDepth();
                    compositeIntoMainTarget();
                }
            }
        },
        FABULOUS {
            @Override
            void beforeTransparencyChain(final MultiBufferSource.BufferSource bufferSource) {
                if (hasVisibleProjectors()) {
                    flushDepthWritingSheets(bufferSource);
                    captureMainCameraDepth();
                }
            }

            @Override
            void afterParticles(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final DeltaTracker deltaTracker) {
                renderProjectorDepths(modelViewMatrix, projectionMatrix, deltaTracker);
            }

            @Override
            void afterLevel() {
                if (hasPendingProjectors()) {
                    compositeIntoMainTarget();
                }
            }
        },
        SHADER_PACK {
            @Override
            void beforeTranslucentTerrain(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final DeltaTracker deltaTracker) {
                if (renderProjectorDepths(modelViewMatrix, projectionMatrix, deltaTracker)) {
                    captureMainCameraDepth();
                    compositeIntoShaderTarget();
                }
            }
        };

        void beforeTransparencyChain(final MultiBufferSource.BufferSource bufferSource) {
        }

        void beforeTranslucentTerrain(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final DeltaTracker deltaTracker) {
        }

        void afterParticles(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix, final DeltaTracker deltaTracker) {
        }

        void afterLevel() {
        }
    }

    private static class LazyDepthOnlyRenderBuffer {
        public DepthOnlyRenderTarget target = new DepthOnlyRenderTarget(DEPTH_CAPTURE_SIZE, DEPTH_CAPTURE_SIZE);
        public WeakReference<?> owner;
        public boolean isValid;

        public boolean isOwnedBy(ProjectorBlockEntity projector) {
            return isValid && owner.get() == projector;
        }

        public void setOwner(ProjectorBlockEntity projector) {
            if (owner == null || owner.get() != projector) {
                owner = new WeakReference<>(projector);
                isValid = false;
            }
        }
    }

    private static final class RenderInfo implements ProjectorBlockEntity.FrameConsumer {
        private static final ThreadLocal<byte[]> RGB = ThreadLocal.withInitial(() -> new byte[3]);

        private final DynamicTexture texture;
        private boolean hasNewFrame;

        RenderInfo(final DynamicTexture texture) {
            this.texture = texture;
        }

        public DynamicTexture texture() {
            return texture;
        }

        public synchronized void close() {
            texture.close();
        }

        public synchronized void uploadIfDirty() {
            if (hasNewFrame) {
                hasNewFrame = false;
                upload();
            }
        }

        public void upload() {
            final NativeImage pixels = texture.getPixels();
            if (pixels == null) {
                return;
            }

            texture.bind();
            pixels.upload(0, 0, 0, 0, 0, pixels.getWidth(), pixels.getHeight(),
                /* blur: */ true, /* clamp: */ true, /* mipmap: */ false, /* autoClose: */ false);
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

            hasNewFrame = true;
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
                    glReadBuffer(GL_NONE);
                    glBindFramebuffer(GL_FRAMEBUFFER, 0);
                }
                TextureUtil.releaseTextureId(this.colorTextureId);
                this.colorTextureId = -1;
            }
        }
    }

    private static final class ProjectorCameraEntity extends Player {
        private static ProjectorCameraEntity instance;

        public static ProjectorCameraEntity get(final Level level, final Vec3 pos, final float rotationY) {
            if (instance == null) {
                instance = new ProjectorCameraEntity(level, BlockPos.ZERO, rotationY);
            }

            instance.setLevel(level);
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
