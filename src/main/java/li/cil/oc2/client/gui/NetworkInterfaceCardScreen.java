/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import li.cil.oc2.client.gui.widget.Texture;
import li.cil.oc2.client.renderer.ModRenderType;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.NetworkInterfaceCardItem;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.NetworkInterfaceCardConfigurationMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import static li.cil.oc2.common.util.TranslationUtils.key;
import static li.cil.oc2.common.util.TranslationUtils.text;

public final class NetworkInterfaceCardScreen extends Screen {
    private static final String SIDE_STATE_TEXT = key("gui.{mod}.network_interface_card.side_state");
    private static final Component CONNECTIVITY_ENABLED_TEXT = text("gui.{mod}.network_interface_card.connectivity.enabled");
    private static final Component CONNECTIVITY_DISABLED_TEXT = text("gui.{mod}.network_interface_card.connectivity.disabled");
    private static final Component INFO_TEXT = text("gui.{mod}.network_interface_card.info");

    public static final int UI_WIDTH = Sprites.NETWORK_INTERFACE_CARD_SCREEN.width;
    public static final int UI_HEIGHT = Sprites.NETWORK_INTERFACE_CARD_SCREEN.height;
    public static final int BLOCK_LEFT = UI_WIDTH / 2;
    public static final int BLOCK_TOP = 53;
    public static final int INFO_TEXT_LEFT = 8;
    public static final int INFO_TEXT_TOP = 104;
    public static final int INFO_TEXT_WIDTH = UI_WIDTH - 16;
    public static final int MAX_BLOCK_PITCH = 30;

    ///////////////////////////////////////////////////////////////////

    private final Player player;
    private final InteractionHand hand;

    private final ComputerBlockItemRenderer computerBlockItemRenderer = new ComputerBlockItemRenderer();

    private Vector3f blockRotation = new Vector3f(-30, 45, 0);
    private int left, top;
    @Nullable private Direction focusedSide;
    private boolean isDraggingBlock, hasDraggedBlock;
    private double dragStartX, dragStartY;

    ///////////////////////////////////////////////////////////////////

    public NetworkInterfaceCardScreen(final Player player, final InteractionHand hand) {
        super(Items.NETWORK_INTERFACE_CARD.get().getDescription());
        this.player = player;
        this.hand = hand;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();

        left = (width - UI_WIDTH) / 2;
        top = (height - UI_HEIGHT) / 2;
    }

    @Override
    public void tick() {
        super.tick();

        final ItemStack heldItem = player.getItemInHand(hand);
        if (!heldItem.is(Items.NETWORK_INTERFACE_CARD.get())) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final boolean result = super.mouseClicked(mouseX, mouseY, button);

        if (!result && isMouseInBlockArea(mouseX, mouseY) && button == 0) {
            isDraggingBlock = true;
            hasDraggedBlock = false;
            dragStartX = mouseX;
            dragStartY = mouseY;
        }

        return result;
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (isDraggingBlock && button == 0) {
            isDraggingBlock = false;
            if (!hasDraggedBlock && focusedSide != null) {
                final NetworkInterfaceCardConfigurationMessage message = new NetworkInterfaceCardConfigurationMessage(hand, focusedSide, !getConfiguration(focusedSide));
                Network.sendToServer(message);
                Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int activeButton, final double deltaX, final double deltaY) {
        if (isDraggingBlock && activeButton == 0) {
            if (!hasDraggedBlock) {
                final double dx = mouseX - dragStartX;
                final double dy = mouseY - dragStartY;
                final double delta = Math.sqrt(dx * dx + dy * dy);
                hasDraggedBlock = delta > 3;
            }
            if (hasDraggedBlock) {
                blockRotation = new Vector3f(
                    Mth.clamp(blockRotation.x() - (float) deltaY, -MAX_BLOCK_PITCH, MAX_BLOCK_PITCH),
                    Mth.wrapDegrees(blockRotation.y() + (float) deltaX),
                    blockRotation.z()
                );
            }
        }

        return true;
    }

    @Override
    public void render(final PoseStack stack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(stack);
        Sprites.NETWORK_INTERFACE_CARD_SCREEN.draw(stack, left, top);

        super.render(stack, mouseX, mouseY, partialTicks);

        final int blockX = left + BLOCK_LEFT;
        final int blockY = top + BLOCK_TOP;
        focusedSide = computerBlockItemRenderer.getFocusedSide(blockX - mouseX, blockY - mouseY, blockRotation);
        computerBlockItemRenderer.render(blockX, blockY, blockRotation);

        if (focusedSide != null) {
            final Component enabledComponent = getConfiguration(focusedSide) ? CONNECTIVITY_ENABLED_TEXT : CONNECTIVITY_DISABLED_TEXT;
            final MutableComponent tooltip = Component.translatable(SIDE_STATE_TEXT, enabledComponent);
            renderTooltip(stack, tooltip, mouseX, mouseY);
        }

        font.drawWordWrap(INFO_TEXT, left + INFO_TEXT_LEFT, top + INFO_TEXT_TOP, INFO_TEXT_WIDTH, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////

    private boolean isMouseInBlockArea(final double mouseX, final double mouseY) {
        return mouseX >= left + 37 && mouseX <= left + (37 + 102) &&
            mouseY >= top + 10 && mouseY <= top + (10 + 102);
    }

    private boolean getConfiguration(@Nullable final Direction side) {
        return side != null && NetworkInterfaceCardItem.getSideConfiguration(player.getItemInHand(hand), side);
    }

    ///////////////////////////////////////////////////////////////////

    private final class ComputerBlockItemRenderer {
        public static final int BLOCK_RENDER_SIZE = 48;

        private final ItemStack computerItemStack = new ItemStack(Items.COMPUTER.get());
        private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        private final BakedModel model = itemRenderer.getModel(computerItemStack, null, null, 0);

        @Nullable
        private Direction getFocusedSide(final float mouseX, final float mouseY, final Vector3f rotation) {
            // Rotate ray inversely around block to represent visual block rotation.
            final Quaternion quaternion = Quaternion.fromXYZDegrees(rotation);
            quaternion.conj();

            // Move ray in screen space to mouse position.
            final float relMouseX = -mouseX / (float) BLOCK_RENDER_SIZE;
            final float relMouseY = -mouseY / (float) BLOCK_RENDER_SIZE;

            final Vector3f source = new Vector3f();
            source.add(relMouseX, relMouseY, 1);
            source.transform(quaternion);

            final Vector3f target = new Vector3f();
            target.add(relMouseX, relMouseY, -1);
            target.transform(quaternion);

            // Intersect rotated ray with bounding box representing block.
            final AABB aabb = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
            return aabb.clip(new Vec3(source), new Vec3(target))
                .map(hit -> Direction.getNearest(hit.x, -hit.y(), hit.z()))
                .filter(side -> side != Direction.SOUTH)
                .orElse(null);
        }

        public void render(final int x, final int y, final Vector3f rotation) {
            RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            final Vector3f renderRotation = rotation.copy();
            renderRotation.add(0, 180, 0);

            final PoseStack stack = RenderSystem.getModelViewStack();
            stack.pushPose();
            stack.translate(x, y, 0);
            stack.mulPose(Quaternion.fromXYZDegrees(renderRotation));
            stack.scale(BLOCK_RENDER_SIZE, -BLOCK_RENDER_SIZE, BLOCK_RENDER_SIZE);
            RenderSystem.applyModelViewMatrix();

            final MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            renderBlock(bufferSource);
            renderOverlays(stack, bufferSource);
            bufferSource.endBatch();

            stack.popPose();
            RenderSystem.applyModelViewMatrix();
        }

        private void renderBlock(final MultiBufferSource.BufferSource bufferSource) {
            itemRenderer.render(computerItemStack, ItemTransforms.TransformType.NONE, false, new PoseStack(), bufferSource, 0xF000F0, OverlayTexture.NO_OVERLAY, model);
        }

        private void renderOverlays(final PoseStack poseStack, final MultiBufferSource.BufferSource bufferSource) {
            for (final Direction side : Direction.values()) {
                // South face of computers is the front face (screen) and there's no connectivity allowed there.
                if (side == Direction.SOUTH) {
                    continue;
                }

                poseStack.pushPose();
                poseStack.setIdentity();

                poseStack.translate(-side.getStepX() * 0.51, side.getStepY() * 0.51, -side.getStepZ() * 0.51);

                final Vector3f sideRotation = switch (side) {
                    case DOWN -> new Vector3f(-90, 0, 0);
                    case UP -> new Vector3f(90, 0, 0);
                    case NORTH -> new Vector3f(0, 180, 0);
                    case WEST -> new Vector3f(0, -90, 0);
                    case EAST -> new Vector3f(0, 90, 0);
                    default -> throw new IllegalStateException("Unexpected value: " + side);
                };
                poseStack.mulPose(Quaternion.fromXYZDegrees(sideRotation));

                poseStack.translate(-0.5, -0.5, 0);

                if (getConfiguration(side)) {
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

            buffer.vertex(poseStack.last().pose(), 0, 0, 0).uv(0, 0).endVertex();
            buffer.vertex(poseStack.last().pose(), 0, 1, 0).uv(0, 1).endVertex();
            buffer.vertex(poseStack.last().pose(), 1, 1, 0).uv(1, 1).endVertex();
            buffer.vertex(poseStack.last().pose(), 1, 0, 0).uv(1, 0).endVertex();
        }
    }
}
