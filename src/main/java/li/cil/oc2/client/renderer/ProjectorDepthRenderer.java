package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import li.cil.oc2.api.API;
import li.cil.oc2.common.ext.MinecraftExt;
import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ProjectorDepthRenderer {
    private static final int DEPTH_CAPTURE_SIZE = 256;

    private static final TextureTarget projectorDepthTarget = new TextureTarget(DEPTH_CAPTURE_SIZE, DEPTH_CAPTURE_SIZE, true, Minecraft.ON_OSX);
    private static final ProjectorDepthCamera camera = new ProjectorDepthCamera();
    private static boolean isRenderingProjectorDepth;

    public static boolean isIsRenderingProjectorDepth() {
        return isRenderingProjectorDepth;
    }

    @SubscribeEvent
    public static void handleRenderLevelLast(final RenderLevelLastEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft instanceof MinecraftExt minecraftExt)) {
            return;
        }

        final ClientLevel level = minecraft.level;
        final LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        try {
            isRenderingProjectorDepth = true;
            RenderSystem.backupProjectionMatrix();

            final BlockPos blockPos = player.eyeBlockPosition();
            camera.configure(level, blockPos.relative(Direction.UP), player.getDirection().toYRot());
            minecraft.setCameraEntity(camera.getEntity());

            final PoseStack viewModelStack;
            viewModelStack = new PoseStack();
            viewModelStack.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()));
            viewModelStack.mulPose(Vector3f.YP.rotationDegrees(camera.getYRot() + 180));

            final Matrix3f viewRotationMatrix = viewModelStack.last().normal().copy();
            if (viewRotationMatrix.invert()) {
                RenderSystem.setInverseViewRotationMatrix(viewRotationMatrix);
            }

            projectorDepthTarget.bindWrite(true);
            minecraftExt.setMainRenderTargetOverride(projectorDepthTarget);

            final Matrix4f projectionMatrix = Matrix4f.perspective(90, 1, 0.05f, 64f);
            RenderSystem.setProjectionMatrix(projectionMatrix);

            final LevelRenderer levelRenderer = event.getLevelRenderer();
            levelRenderer.prepareCullFrustum(viewModelStack, camera.getPosition(), projectionMatrix);
            levelRenderer.renderLevel(
                viewModelStack,
                event.getPartialTick(),
                event.getStartNanos(),
                /* shouldRenderBlockOutline: */ false,
                camera,
                minecraft.gameRenderer,
                minecraft.gameRenderer.lightTexture(),
                projectionMatrix
            );
        } finally {
            RenderSystem.restoreProjectionMatrix();
            minecraftExt.setMainRenderTargetOverride(null);
            minecraft.getMainRenderTarget().bindWrite(true);
            minecraft.setCameraEntity(player);
            isRenderingProjectorDepth = false;
        }
    }

    // TODO [PERF] override any vertex formats to only use pos

    @SubscribeEvent
    public static void handleRenderGameOverlay(final RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        final PoseStack stack = event.getMatrixStack();
        stack.pushPose();

        final Tesselator tesselator = Tesselator.getInstance();
        final BufferBuilder builder = tesselator.getBuilder();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, projectorDepthTarget.getDepthTextureId());

        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        builder.vertex(50, 50, 0).uv(0, 0).color(0xFFFF0000).endVertex(); // top left
        builder.vertex(50, 50 + DEPTH_CAPTURE_SIZE / 2, 0).uv(0, 1).color(0xFF00FF00).endVertex(); // bottom left
        builder.vertex(50 + DEPTH_CAPTURE_SIZE / 2, 50 + DEPTH_CAPTURE_SIZE / 2, 0).uv(1, 1).color(0xFF0000FF).endVertex(); // bottom right
        builder.vertex(50 + DEPTH_CAPTURE_SIZE / 2, 50, 0).uv(1, 0).color(0xFFFFFFFF).endVertex(); // top right

        tesselator.end();

        stack.popPose();
    }

    @SubscribeEvent
    public static void handleFog(final EntityViewRenderEvent.RenderFogEvent event) {
        if (isRenderingProjectorDepth) {
            FogRenderer.setupNoFog();
        }
    }

    private static final class ProjectorDepthCamera extends Camera {
        public void configure(final Level level, final BlockPos blockPos, final float rotationY) {
            final ProjectorEntity entity = new ProjectorEntity(level, blockPos, rotationY);
            setup(level, entity, false, false, 0);
        }

        private static final class ProjectorEntity extends Player {
            public ProjectorEntity(final Level level, final BlockPos blockPos, final float rotationY) {
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
}
