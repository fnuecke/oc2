/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.event.events.client.ClientTickEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import li.cil.oc2.client.ClientPlatform;
import li.cil.oc2.common.block.FlippableOrientableBlock;
import li.cil.oc2.common.block.FlippableOrientation;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.bus.device.block.ProjectorDevice;
import li.cil.oc2.common.ext.LevelRendererExt;
import li.cil.oc2.common.ext.MinecraftExt;
import li.cil.oc2.common.util.FakePlayerUtils;
import li.cil.oc2.jcodec.common.model.Picture;
import li.cil.oc2.jcodec.scale.Yuv420jToRgb;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.lwjgl.system.MemoryStack;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

// No @Mod.EventBusSubscriber: we need to register this manually, because static init throws errors when running data generation.
public final class ProjectorDepthRenderer {
    private static final int DEPTH_CAPTURE_SIZE = 256;
    private static final int DEPTH_BUFFER_SOURCE_BYTES = 768 * 1024;
    private static final float EMISSIVE_STRENGTH = 1.0f;
    private static final int PROJECTORS_CONFIG_SIZE = (1 + ModShaders.MAX_PROJECTORS) * 64 + 16;
    private static final String[] PROJECTOR_COLOR_NAMES = {"ProjectorColor0", "ProjectorColor1", "ProjectorColor2"};
    private static final String[] PROJECTOR_DEPTH_NAMES = {"ProjectorDepth0", "ProjectorDepth1", "ProjectorDepth2"};

    private static final List<ProjectorBlockEntity> VISIBLE_PROJECTORS = new ArrayList<>();
    private static final LazyDepthOnlyRenderBuffer[] PROJECTOR_DEPTH_BUFFER_LEDGER = new LazyDepthOnlyRenderBuffer[ModShaders.MAX_PROJECTORS];
    private static final RenderTarget[] PROJECTOR_DEPTH_BUFFERS = new RenderTarget[ModShaders.MAX_PROJECTORS];
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
    private static final Matrix4f UPSIDE_DOWN_DEPTH_CAMERA_PROJECTION_MATRIX = getFrustumMatrix(
        PROJECTOR_NEAR, PROJECTOR_FAR,
        ProjectorBlockEntity.MAX_GOOD_RENDER_DISTANCE,
        -HALF_FRUSTUM_WIDTH, HALF_FRUSTUM_WIDTH,
        0, -FRUSTUM_HEIGHT);
    private static final Quaternionf VIEW_ROTATION = new Quaternionf();
    private static final Matrix4f MODEL_VIEW_MATRIX = new Matrix4f();
    private static final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    private static final Matrix4f INVERSE_MAIN_CAMERA_MATRIX = new Matrix4f();
    private static final LevelRenderState DEPTH_LEVEL_RENDER_STATE = new LevelRenderState();
    private static final ParticlesRenderState DEPTH_PARTICLES_RENDER_STATE = new ParticlesRenderState();

    private static final Cache<ProjectorBlockEntity, RenderInfo> RENDER_INFO = CacheBuilder.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(5))
        .removalListener(ProjectorDepthRenderer::handleProjectorNoLongerRendering)
        .build();

    @Nullable
    private static RenderTarget activeProjectorDepthTarget;
    @Nullable
    private static DepthPassResources depthPassResources;
    @Nullable
    private static GpuBuffer projectorsConfig;

    private static int pendingRenderCount;
    private static int refreshSlot;
    private static boolean isRenderingProjectorDepth;
    private static boolean isRenderedBeforeTransparencyChain;
    @Nullable
    private static Camera gameRendererMainCameraBak;
    @Nullable
    private static GpuBuffer globalSettingsBak;

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

    private static DepthPassResources depthPassResources() {
        if (depthPassResources == null) {
            depthPassResources = new DepthPassResources(Minecraft.getInstance());
        }
        return depthPassResources;
    }

    private static GpuBuffer projectorsConfig() {
        if (projectorsConfig == null) {
            projectorsConfig = RenderSystem.getDevice().createBuffer(() -> "oc2 projectors config",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, PROJECTORS_CONFIG_SIZE);
        }
        return projectorsConfig;
    }

    // --------------------------------------------------------------------- //

    public static void addProjector(final ProjectorBlockEntity projector) {
        // Projectors may be rendered multiple times, e.g. in shadow passes when shaders are enabled.
        if (!VISIBLE_PROJECTORS.contains(projector) && isProjectionVisible(projector)) {
            VISIBLE_PROJECTORS.add(projector);
        }
    }

    public static boolean isRenderingProjectorDepth() {
        return isRenderingProjectorDepth;
    }

    @Nullable
    public static RenderTarget getActiveProjectorDepthTarget() {
        return activeProjectorDepthTarget;
    }

    public static void onBeforeLevel(final DeltaTracker deltaTracker) {
        try {
            if (VISIBLE_PROJECTORS.isEmpty()) {
                return;
            }

            final int projectorIndex = getNextDepthTargetSlotToRender(Math.min(VISIBLE_PROJECTORS.size(), ModShaders.MAX_PROJECTORS));
            if (projectorIndex < 0) {
                return;
            }

            final ProjectorBlockEntity projector = VISIBLE_PROJECTORS.get(projectorIndex);
            final Minecraft minecraft = Minecraft.getInstance();
            final ClientLevel level = minecraft.level;
            if (level == null || projector.isRemoved() || projector.getLevel() != level) {
                return;
            }

            renderDepthBuffer(minecraft, level, deltaTracker, projectorIndex, projector);
        } finally {
            VISIBLE_PROJECTORS.clear();
        }
    }

    public static void captureMainCamera(final Matrix4f modelViewMatrix, final Matrix4f projectionMatrix) {
        MODEL_VIEW_MATRIX.set(modelViewMatrix);
        PROJECTION_MATRIX.set(projectionMatrix);
    }

    public static void onBeforeTransparencyChain() {
        isRenderedBeforeTransparencyChain = true;
        if (prepareProjectorRendering()) {
            renderProjectors(Minecraft.getInstance());
        }
    }

    public static void onAfterLevel() {
        try {
            if (!isRenderedBeforeTransparencyChain && prepareProjectorRendering()) {
                renderProjectors(Minecraft.getInstance());
            }
        } finally {
            isRenderedBeforeTransparencyChain = false;
            pendingRenderCount = 0;
            Arrays.fill(PROJECTOR_COLOR_BUFFERS, null);
            Arrays.fill(PROJECTOR_DEPTH_BUFFERS, null);
        }
    }

    public static void initialize() {
        IrisCompat.copyPipeline(RenderPipelines.LIGHTNING, ModRenderType.getProjectorLight().pipeline());

        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            RENDER_INFO.cleanUp();
            if (minecraft.level == null) {
                VISIBLE_PROJECTORS.clear();
            }
        });
    }

    // --------------------------------------------------------------------- //

    private static boolean isProjectionVisible(final ProjectorBlockEntity projector) {
        final Frustum frustum = ((LevelRendererExt) Minecraft.getInstance().levelRenderer).oc2$getCullingFrustum();
        return frustum != null && frustum.isVisible(projectionBounds(projector));
    }

    private static AABB projectionBounds(final ProjectorBlockEntity projector) {
        return projector.getExpandedRenderBoundingBox()
            .inflate(1); // Compensate e.g. view bobbing and allow init a bit early.
    }

    private static boolean prepareProjectorRendering() {
        if (VISIBLE_PROJECTORS.isEmpty()) {
            return false;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            VISIBLE_PROJECTORS.clear();
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

        pendingRenderCount = updateProjectorDepths(minecraft, projectorCount);

        return pendingRenderCount > 0;
    }

    private static int updateProjectorDepths(final Minecraft minecraft, final int projectorCount) {
        final Vec3 mainCameraPosition = minecraft.gameRenderer.getMainCamera().position();
        final PoseStack viewModelStack = new PoseStack();

        matchDepthTargetsToOwningProjectors(projectorCount);

        final int validDepthTargetCount = sortByDepthBufferValidity(projectorCount);

        for (int i = 0; i < validDepthTargetCount; i++) {
            final ProjectorBlockEntity projector = VISIBLE_PROJECTORS.get(i);
            final FlippableOrientation orientation = projector.getBlockState().getValue(FlippableOrientableBlock.ORIENTATION);
            final Vec3 projectorPos = Vec3
                .atCenterOf(projector.getBlockPos())
                .add(new Vec3(orientation.getFacing().step()).scale(PROJECTOR_FORWARD_SHIFT));

            setupViewModelMatrix(viewModelStack, orientation);

            storeProjectorMatrix(i, projectorPos, mainCameraPosition, viewModelStack, getProjectionMatrix(orientation));

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
    }

    private static int sortByDepthBufferValidity(final int projectorCount) {
        int validDepthTargetCount = 0;

        for (int i = 0; i < projectorCount; i++) {
            if (!projectorDepthBuffers()[i].isOwnedBy(VISIBLE_PROJECTORS.get(i))) {
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
                                          final DeltaTracker deltaTracker, final int projectorIndex,
                                          final ProjectorBlockEntity projector) {
        final PoseStack viewModelStack = new PoseStack();
        final FlippableOrientation orientation = projector.getBlockState().getValue(FlippableOrientableBlock.ORIENTATION);
        final Vec3 projectorPos = Vec3
            .atCenterOf(projector.getBlockPos())
            .add(new Vec3(orientation.getFacing().step()).scale(PROJECTOR_FORWARD_SHIFT));

        try {
            prepareDepthBufferRendering(minecraft);

            configureProjectorDepthCamera(level, projectorPos, orientation);
            updateGlobalSettings(minecraft, deltaTracker);

            final Matrix4f projectionMatrix = getProjectionMatrix(orientation);
            RenderSystem.setProjectionMatrix(depthPassResources().projectionMatrixBuffer.getBuffer(projectionMatrix), ProjectionType.PERSPECTIVE);
            setupViewModelMatrix(viewModelStack, orientation);

            bindProjectorDepthRenderTarget(projectorIndex, projector, minecraft);

            renderProjectorDepthBuffer(minecraft, level, deltaTracker, viewModelStack, projectionMatrix);
        } finally {
            finishDepthBufferRendering(minecraft);
        }
    }

    private static int getNextDepthTargetSlotToRender(final int projectorCount) {
        refreshSlot = (refreshSlot + 1) % ModShaders.MAX_PROJECTORS;
        return refreshSlot < projectorCount ? refreshSlot : -1;
    }

    private static void prepareDepthBufferRendering(final Minecraft minecraft) {
        isRenderingProjectorDepth = true;

        gameRendererMainCameraBak = minecraft.gameRenderer.mainCamera;
        minecraft.gameRenderer.mainCamera = PROJECTOR_DEPTH_CAMERA;
        prepareRenderDispatchers(minecraft, PROJECTOR_DEPTH_CAMERA);

        RenderSystem.backupProjectionMatrix();
        globalSettingsBak = RenderSystem.getGlobalSettingsUniform();
    }

    private static void finishDepthBufferRendering(final Minecraft minecraft) {
        isRenderingProjectorDepth = false;
        activeProjectorDepthTarget = null;

        ((MinecraftExt) minecraft).setMainRenderTargetOverride(null);

        RenderSystem.restoreProjectionMatrix();
        RenderSystem.setGlobalSettingsUniform(globalSettingsBak);

        minecraft.gameRenderer.mainCamera = gameRendererMainCameraBak;
        prepareRenderDispatchers(minecraft, gameRendererMainCameraBak);
    }

    // Same as in GameRenderer::render(), but for the projector camera.
    private static void updateGlobalSettings(final Minecraft minecraft, final DeltaTracker deltaTracker) {
        depthPassResources().globalSettingsUniform.update(
            minecraft.getWindow().getWidth(),
            minecraft.getWindow().getHeight(),
            minecraft.options.glintStrength().get(),
            minecraft.level == null ? 0L : minecraft.level.getGameTime(),
            deltaTracker,
            minecraft.options.getMenuBackgroundBlurriness(),
            PROJECTOR_DEPTH_CAMERA,
            minecraft.options.textureFiltering().get() == TextureFilteringMethod.RGSS
        );
    }

    private static void prepareRenderDispatchers(final Minecraft minecraft, final Camera camera) {
        // Same calls as in LevelRenderer::renderLevel() before it extracts entities.
        minecraft.getBlockEntityRenderDispatcher().prepare(camera);
        minecraft.getEntityRenderDispatcher().prepare(camera, minecraft.crosshairPickEntity);
    }

    // Manual level rendering a la LevelRenderer::renderLevel(), with the bits we need for the projector depth.
    private static void renderProjectorDepthBuffer(final Minecraft minecraft, final ClientLevel level, final DeltaTracker deltaTracker, final PoseStack viewModelStack, final Matrix4f projectionMatrix) {
        final LevelRenderer levelRenderer = minecraft.levelRenderer;
        final LevelRendererExt levelRendererExt = (LevelRendererExt) levelRenderer;
        final DepthPassResources resources = depthPassResources();
        final Matrix4f frustumMatrix = viewModelStack.last().pose();
        final Vec3 cameraPosition = PROJECTOR_DEPTH_CAMERA.position();
        final double cameraX = cameraPosition.x(), cameraY = cameraPosition.y(), cameraZ = cameraPosition.z();
        final float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);

        final Frustum frustum = new Frustum(frustumMatrix, projectionMatrix);
        frustum.prepare(cameraX, cameraY, cameraZ);

        final Object sodiumMatrices = SodiumCompat.setChunkRenderMatrices(levelRenderer, projectionMatrix, frustumMatrix);
        try {
            levelRendererExt.oc2$cullTerrain(PROJECTOR_DEPTH_CAMERA, frustum);
            levelRendererExt.oc2$prepareChunkRenders(frustumMatrix, cameraX, cameraY, cameraZ)
                .renderGroup(ChunkSectionLayerGroup.OPAQUE, resources.terrainSampler);
        } finally {
            SodiumCompat.restoreChunkRenderMatrices(levelRenderer, sodiumMatrices);
        }

        NetworkCableRenderer.render(PROJECTOR_DEPTH_CAMERA, frustumMatrix, frustum);

        final LevelRenderState renderState = DEPTH_LEVEL_RENDER_STATE;
        final CameraRenderState cameraRenderState = renderState.cameraRenderState;
        cameraRenderState.initialized = true;
        cameraRenderState.pos = cameraPosition;
        cameraRenderState.blockPos = PROJECTOR_DEPTH_CAMERA.blockPosition();
        cameraRenderState.entityPos = cameraPosition;
        cameraRenderState.orientation.set(PROJECTOR_DEPTH_CAMERA.rotation());

        final Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix().mul(frustumMatrix);
        try {
            final PoseStack poseStack = new PoseStack();

            extractEntities(minecraft, level, frustum, partialTicks, cameraX, cameraY, cameraZ, renderState);
            final EntityRenderDispatcher entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
            for (final EntityRenderState entityRenderState : renderState.entityRenderStates) {
                entityRenderDispatcher.submit(entityRenderState, cameraRenderState,
                    entityRenderState.x - cameraX, entityRenderState.y - cameraY, entityRenderState.z - cameraZ,
                    poseStack, resources.submitNodeStorage);
            }

            extractBlockEntities(minecraft, level, frustum, partialTicks, cameraX, cameraY, cameraZ, renderState);
            final BlockEntityRenderDispatcher blockEntityRenderDispatcher = minecraft.getBlockEntityRenderDispatcher();
            for (final BlockEntityRenderState blockEntityRenderState : renderState.blockEntityRenderStates) {
                final BlockPos pos = blockEntityRenderState.blockPos;
                poseStack.pushPose();
                poseStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                blockEntityRenderDispatcher.submit(blockEntityRenderState, poseStack, resources.submitNodeStorage, cameraRenderState);
                poseStack.popPose();
            }

            minecraft.particleEngine.extract(DEPTH_PARTICLES_RENDER_STATE, frustum, PROJECTOR_DEPTH_CAMERA, partialTicks);
            DEPTH_PARTICLES_RENDER_STATE.submit(resources.submitNodeStorage, cameraRenderState);

            resources.featureRenderDispatcher.renderAllFeatures();
            resources.bufferSource.endBatch();

            // Weather only writes depth with "Fabulous!", same as for the main camera.
            if (Minecraft.useShaderTransparency()) {
                resources.weatherEffectRenderer.extractRenderState(level, levelRendererExt.oc2$getTicks(), partialTicks, cameraPosition, renderState.weatherRenderState);
                resources.weatherEffectRenderer.render(resources.bufferSource, cameraPosition, renderState.weatherRenderState);
                resources.bufferSource.endBatch();
            }
        } finally {
            modelViewStack.popMatrix();
            resources.submitNodeStorage.clear();
            resources.submitNodeStorage.endFrame();
            resources.featureRenderDispatcher.endFrame();
            DEPTH_PARTICLES_RENDER_STATE.reset();
            renderState.reset();
        }
    }

    private static void extractEntities(final Minecraft minecraft, final ClientLevel level, final Frustum frustum,
                                        final float partialTicks,
                                        final double cameraX, final double cameraY, final double cameraZ,
                                        final LevelRenderState renderState) {
        final EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        for (final Entity entity : level.entitiesForRendering()) {
            if (dispatcher.shouldRender(entity, frustum, cameraX, cameraY, cameraZ)) {
                final EntityRenderState entityRenderState = dispatcher.extractEntity(entity, partialTicks);
                entityRenderState.outlineColor = 0;
                entityRenderState.nameTag = null;
                if (entityRenderState instanceof final AvatarRenderState avatarRenderState) {
                    avatarRenderState.scoreText = null;
                }
                // Skip shadow rendering for perf.
                entityRenderState.shadowPieces.clear();
                renderState.entityRenderStates.add(entityRenderState);
            }
        }
    }

    // Same as in LevelRenderer::renderLevel() with the relevant block entity subset (chunks in projector frustum).
    private static void extractBlockEntities(final Minecraft minecraft, final ClientLevel level, final Frustum frustum,
                                             final float partialTicks,
                                             final double cameraX, final double cameraY, final double cameraZ,
                                             final LevelRenderState renderState) {
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
                    final int sectionY = SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY());
                    if (sectionY < minSectionY || sectionY > maxSectionY) {
                        continue;
                    }

                    final BlockEntityRenderState blockEntityRenderState = ClientPlatform.tryExtractBlockEntityRenderState(dispatcher, blockEntity, partialTicks, frustum);
                    if (blockEntityRenderState != null) {
                        renderState.blockEntityRenderStates.add(blockEntityRenderState);
                    }
                }
            }
        }
    }

    private static void configureProjectorDepthCamera(final ClientLevel level, final Vec3 pos, final FlippableOrientation orientation) {
        final FlippableOrientation viewOrientation = orientation.getUpright();
        final float rotationY = viewOrientation.getRotationY() + 180;
        final float rotationX = viewOrientation.getRotationX();
        PROJECTOR_DEPTH_CAMERA.setup(level, ProjectorCameraEntity.get(level, pos, rotationY, rotationX), false, false, 0);
    }

    private static void setupViewModelMatrix(final PoseStack viewModelStack, final FlippableOrientation orientation) {
        viewModelStack.setIdentity();
        viewModelStack.mulPose(orientation.getUpright().getRotation().conjugate(VIEW_ROTATION));
    }

    private static Matrix4f getProjectionMatrix(final FlippableOrientation orientation) {
        return orientation.isUpsideDown() ? UPSIDE_DOWN_DEPTH_CAMERA_PROJECTION_MATRIX : DEPTH_CAMERA_PROJECTION_MATRIX;
    }

    private static AABB frustumBounds(final double cameraX, final double cameraY, final double cameraZ) {
        final double reach = ProjectorBlockEntity.MAX_RENDER_DISTANCE;
        return new AABB(cameraX - reach, cameraY - reach, cameraZ - reach,
            cameraX + reach, cameraY + reach, cameraZ + reach);
    }

    private static void bindProjectorDepthRenderTarget(final int projectorIndex, final ProjectorBlockEntity projector, final Minecraft minecraft) {
        final var projectorDepthTarget = projectorDepthBuffers()[projectorIndex];
        if (!projectorDepthTarget.isOwnedBy(projector)) {
            projectorDepthTarget.owner = new WeakReference<>(projector);
        }
        final RenderTarget target = projectorDepthTarget.target();
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
        activeProjectorDepthTarget = target;
        ((MinecraftExt) minecraft).setMainRenderTargetOverride(target);
    }

    private static void storeProjectorMatrix(final int projectorIndex, final Vec3 projectorPos, final Vec3 mainCameraPosition, final PoseStack viewModelStack, final Matrix4f projectionMatrix) {
        // Save model-view-projection matrix for mapping in compositing shader. We use the position relative to the
        // main camera here, so that the main camera can sit at the origin. This avoids loss of precision.
        PROJECTOR_CAMERA_MATRICES[projectorIndex].set(projectionMatrix);
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
        PROJECTOR_DEPTH_BUFFERS[projectorIndex] = projectorDepthBuffers()[projectorIndex].target();
    }

    private static void renderProjectors(final Minecraft minecraft) {
        final RenderTarget mainRenderTarget = minecraft.getMainRenderTarget();
        final CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        INVERSE_MAIN_CAMERA_MATRIX.set(PROJECTION_MATRIX).mul(MODEL_VIEW_MATRIX).invert();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final Std140Builder builder = Std140Builder.onStack(stack, PROJECTORS_CONFIG_SIZE)
                .putMat4f(INVERSE_MAIN_CAMERA_MATRIX);
            for (final Matrix4f matrix : PROJECTOR_CAMERA_MATRICES) {
                builder.putMat4f(matrix);
            }
            final ByteBuffer data = builder
                .putInt(pendingRenderCount)
                .putFloat(EMISSIVE_STRENGTH)
                .get();
            commandEncoder.writeToBuffer(projectorsConfig().slice(), data);
        }

        final GpuSampler nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        final GpuSampler linear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        try (RenderPass pass = commandEncoder.createRenderPass(() -> "oc2 projectors", mainRenderTarget.getColorTextureView(), OptionalInt.empty())) {
            pass.setPipeline(ModShaders.PROJECTORS_PIPELINE);
            pass.setUniform("ProjectorsConfig", projectorsConfig());
            pass.bindTexture("MainCameraDepth", mainRenderTarget.getDepthTextureView(), nearest);
            for (int i = 0; i < ModShaders.MAX_PROJECTORS; i++) {
                // Unused slots are skipped by the shader, but every sampler must be bound.
                final int slot = i < pendingRenderCount ? i : 0;
                pass.bindTexture(PROJECTOR_COLOR_NAMES[i], PROJECTOR_COLOR_BUFFERS[slot].getTextureView(), linear);
                pass.bindTexture(PROJECTOR_DEPTH_NAMES[i], PROJECTOR_DEPTH_BUFFERS[slot].getDepthTextureView(), nearest);
            }
            pass.draw(0, 3);
        }
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
                final DynamicTexture texture = new DynamicTexture("oc2 projector", ProjectorDevice.WIDTH, ProjectorDevice.HEIGHT, true);
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

    private static final class DepthPassResources {
        public final GlobalSettingsUniform globalSettingsUniform = new GlobalSettingsUniform();
        public final PerspectiveProjectionMatrixBuffer projectionMatrixBuffer = new PerspectiveProjectionMatrixBuffer("oc2 projector depth");
        public final GpuSampler terrainSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
        public final MultiBufferSource.BufferSource bufferSource;
        public final SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
        public final FeatureRenderDispatcher featureRenderDispatcher;
        public final WeatherEffectRenderer weatherEffectRenderer = new WeatherEffectRenderer();

        public DepthPassResources(final Minecraft minecraft) {
            // Same fixed buffers as vanilla. Layered consumers, e.g. enchantment glint, need their own buffer.
            // Direct field access of bufferSource for Iris compatibility (it changes what the getter method returns).
            final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers = new Object2ObjectLinkedOpenHashMap<>();
            for (final RenderType renderType : minecraft.renderBuffers().bufferSource.fixedBuffers.keySet()) {
                fixedBuffers.put(renderType, new ByteBufferBuilder(renderType.bufferSize()));
            }
            bufferSource = MultiBufferSource.immediateWithBuffers(fixedBuffers, new ByteBufferBuilder(DEPTH_BUFFER_SOURCE_BYTES));

            featureRenderDispatcher = new FeatureRenderDispatcher(
                submitNodeStorage,
                minecraft.getBlockRenderer(),
                bufferSource,
                minecraft.getAtlasManager(),
                new OutlineBufferSource(),
                minecraft.renderBuffers().crumblingBufferSource(),
                minecraft.font
            );
        }
    }

    private static class LazyDepthOnlyRenderBuffer {
        @Nullable
        public RenderTarget target;
        @Nullable
        public WeakReference<?> owner;

        public RenderTarget target() {
            if (target == null) {
                target = new TextureTarget("oc2 projector depth", DEPTH_CAPTURE_SIZE, DEPTH_CAPTURE_SIZE, true);
            }
            return target;
        }

        public boolean isOwnedBy(ProjectorBlockEntity projector) {
            return owner != null && owner.get() == projector;
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
            texture.upload();
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
            image.setPixelABGR(col, row, r | (g << 8) | (b << 16) | (0xFF << 24));
        }
    }

    private static final class ProjectorCameraEntity extends Player {
        @Nullable
        private static ProjectorCameraEntity instance;

        public static ProjectorCameraEntity get(final Level level, final Vec3 pos, final float rotationY, final float rotationX) {
            if (instance == null) {
                instance = new ProjectorCameraEntity(level);
            }

            instance.setLevel(level);
            instance.snapTo(pos.x(), pos.y(), pos.z(), rotationY, rotationX);

            return instance;
        }

        private ProjectorCameraEntity(final Level level) {
            super(level, FakePlayerUtils.getFakePlayerProfile());
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
        public GameType gameMode() {
            return GameType.CREATIVE;
        }
    }
}
