/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.node;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.client.gui.Textures;
import li.cil.oc2.client.gui.widget.Texture;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.common.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static li.cil.oc2.common.util.TranslationUtils.key;
import static li.cil.oc2.common.util.TranslationUtils.text;

public final class SideConfigurationWidget extends AbstractWidget {
    private static final String SIDE_STATE_TEXT = key("gui.{mod}.sided_device.side_state");
    private static final Component CONNECTIVITY_ENABLED_TEXT = text("gui.{mod}.sided_device.connectivity.enabled");
    private static final Component CONNECTIVITY_DISABLED_TEXT = text("gui.{mod}.sided_device.connectivity.disabled");
    private static final int MAX_BLOCK_PITCH = 30;
    private static final double DRAG_THRESHOLD = 3;
    private static final int SIZE = 100;

    // --------------------------------------------------------------------- //

    private final Predicate<Direction> getConfiguration;
    private final BiConsumer<Direction, Boolean> sendConfiguration;
    private final ComputerBlockItemRenderer computerBlockItemRenderer = new ComputerBlockItemRenderer();

    private Vector3f blockRotation = new Vector3f(-30, 45, 0);
    @Nullable
    private Direction focusedSide;
    private boolean isPressed, hasDraggedBlock;
    private double dragStartX, dragStartY;

    // --------------------------------------------------------------------- //

    public SideConfigurationWidget(final Predicate<Direction> getConfiguration, final BiConsumer<Direction, Boolean> sendConfiguration) {
        super(0, 0, SIZE, SIZE, CommonComponents.EMPTY);
        this.getConfiguration = getConfiguration;
        this.sendConfiguration = sendConfiguration;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void onClick(final double mouseX, final double mouseY) {
        isPressed = true;
        hasDraggedBlock = false;
        dragStartX = mouseX;
        dragStartY = mouseY;
    }

    @Override
    public void onRelease(final double mouseX, final double mouseY) {
        if (isPressed && !hasDraggedBlock && focusedSide != null) {
            sendConfiguration.accept(focusedSide, !getConfiguration.test(focusedSide));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
        }
        isPressed = false;
    }

    @Override
    public void playDownSound(final SoundManager soundManager) {
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void onDrag(final double mouseX, final double mouseY, final double deltaX, final double deltaY) {
        if (!hasDraggedBlock) {
            final double dx = mouseX - dragStartX;
            final double dy = mouseY - dragStartY;
            hasDraggedBlock = Math.sqrt(dx * dx + dy * dy) > DRAG_THRESHOLD;
        }
        if (hasDraggedBlock) {
            blockRotation = new Vector3f(
                Mth.clamp(blockRotation.x() - (float) deltaY, -MAX_BLOCK_PITCH, MAX_BLOCK_PITCH),
                Mth.wrapDegrees(blockRotation.y() + (float) deltaX),
                blockRotation.z()
            );
        }
    }

    @Override
    protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final int blockX = getX() + width / 2;
        final int blockY = getY() + height / 2;
        focusedSide = isHovered() ? computerBlockItemRenderer.getFocusedSide(blockX - mouseX, blockY - mouseY, blockRotation) : null;
        computerBlockItemRenderer.render(blockX, blockY, blockRotation);

        final Screen screen = Minecraft.getInstance().screen;
        if (focusedSide != null && screen != null) {
            final Component enabledComponent = getConfiguration.test(focusedSide) ? CONNECTIVITY_ENABLED_TEXT : CONNECTIVITY_DISABLED_TEXT;
            screen.setTooltipForNextRenderPass(Component.translatable(SIDE_STATE_TEXT, enabledComponent));
        }
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
    }

    // --------------------------------------------------------------------- //

    private final class ComputerBlockItemRenderer {
        public static final int BLOCK_RENDER_SIZE = 48;

        private final ItemStack computerItemStack = new ItemStack(Items.COMPUTER.get());
        private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        private final BakedModel model = itemRenderer.getModel(computerItemStack, null, null, 0);

        @Nullable
        private Direction getFocusedSide(final float mouseX, final float mouseY, final Vector3f rotation) {
            // Rotate ray inversely around block to represent visual block rotation.
            final Quaternionf quaternion = fromXYZDegrees(toRenderRotation(rotation));
            quaternion.conjugate();

            // Move ray in screen space to mouse position.
            final float relMouseX = -mouseX / BLOCK_RENDER_SIZE;
            final float relMouseY = -mouseY / BLOCK_RENDER_SIZE;

            final Vector3f source = new Vector3f();
            source.add(relMouseX, relMouseY, 1);
            source.rotate(quaternion);

            final Vector3f target = new Vector3f();
            target.add(relMouseX, relMouseY, -1);
            target.rotate(quaternion);

            // Intersect rotated ray with bounding box representing block.
            final AABB aabb = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
            return aabb.clip(new Vec3(source), new Vec3(target))
                .map(hit -> Direction.getNearest(-hit.x, -hit.y(), -hit.z()))
                .filter(side -> side != Direction.SOUTH)
                .orElse(null);
        }

        public void render(final int x, final int y, final Vector3f rotation) {
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            final Vector3f renderRotation = toRenderRotation(rotation);

            final PoseStack poseStack = new PoseStack();
            poseStack.translate(x, y, BLOCK_RENDER_SIZE);
            poseStack.mulPose(fromXYZDegrees(renderRotation));
            poseStack.scale(BLOCK_RENDER_SIZE, -BLOCK_RENDER_SIZE, BLOCK_RENDER_SIZE);

            final MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            renderBlock(poseStack, bufferSource);
            renderOverlays(poseStack, bufferSource);
            bufferSource.endBatch();
        }

        private void renderBlock(final PoseStack poseStack, final MultiBufferSource.BufferSource bufferSource) {
            itemRenderer.render(computerItemStack, ItemDisplayContext.NONE, false, poseStack, bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY, model);
        }

        private void renderOverlays(final PoseStack poseStack, final MultiBufferSource.BufferSource bufferSource) {
            for (final Direction side : Direction.values()) {
                // South face of computers is the front face (screen) and there's no connectivity allowed there.
                if (side == Direction.SOUTH) {
                    continue;
                }

                poseStack.pushPose();

                poseStack.translate(-side.getStepX() * 0.51, side.getStepY() * 0.51, -side.getStepZ() * 0.51);

                final Vector3f sideRotation = switch (side) {
                    case DOWN -> new Vector3f(-90, 0, 0);
                    case UP -> new Vector3f(90, 0, 0);
                    case NORTH -> new Vector3f(0, 180, 0);
                    case WEST -> new Vector3f(0, -90, 0);
                    case EAST -> new Vector3f(0, 90, 0);
                    default -> throw new IllegalStateException("Unexpected value: " + side);
                };
                poseStack.mulPose(fromXYZDegrees(sideRotation));

                poseStack.translate(-0.5, -0.5, 0);

                if (getConfiguration.test(side)) {
                    renderOverlay(poseStack, bufferSource, Textures.BLOCK_FACE_ENABLED_TEXTURE);
                } else {
                    renderOverlay(poseStack, bufferSource, Textures.BLOCK_FACE_DISABLED_TEXTURE);
                }

                if (side == focusedSide) {
                    renderOverlay(poseStack, bufferSource, Textures.BLOCK_FACE_FOCUSED_TEXTURE);
                }

                poseStack.popPose();
            }
        }

        private void renderOverlay(final PoseStack poseStack, final MultiBufferSource.BufferSource bufferSource, final Texture texture) {
            final VertexConsumer buffer = bufferSource.getBuffer(ModRenderType.getOverlay(texture.location));

            buffer.addVertex(poseStack.last().pose(), 0, 0, 0).setUv(0, 0);
            buffer.addVertex(poseStack.last().pose(), 0, 1, 0).setUv(0, 1);
            buffer.addVertex(poseStack.last().pose(), 1, 1, 0).setUv(1, 1);
            buffer.addVertex(poseStack.last().pose(), 1, 0, 0).setUv(1, 0);
        }

        private static Vector3f toRenderRotation(final Vector3f rotation) {
            return new Vector3f(rotation).add(0, 180, 0);
        }

        private static Quaternionf fromXYZDegrees(final Vector3f degrees) {
            return new Quaternionf().rotationXYZ(
                (float) Math.toRadians(degrees.x()),
                (float) Math.toRadians(degrees.y()),
                (float) Math.toRadians(degrees.z()));
        }
    }
}
