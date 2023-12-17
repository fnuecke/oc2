/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import li.cil.oc2.common.blockentity.KeyboardBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.KeyboardInputMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public final class KeyboardScreen extends Screen {
    private static final int BORDER_SIZE = 4;
    private static final float ARM_SWING_RATE = 0.8f;
    private static final int BORDER_COLOR = 0xFFFFFFFF;

    private static final MutableComponent CLOSE_INFO = Component.translatable("gui.oc2.keyboard.close_info");

    ///////////////////////////////////////////////////////////////////

    private final KeyboardBlockEntity keyboard;

    private boolean hideHotbar = false;

    ///////////////////////////////////////////////////////////////////

    public KeyboardScreen(final KeyboardBlockEntity keyboard) {
        super(Items.KEYBOARD.get().getDescription());
        this.keyboard = keyboard;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();

        // Grabbing the mouse allows us to let the player keep turning the camera (to get a better
        // look at the projection of a projector, e.g.), while still grabbing all keyboard input.
        grabMouse();

        // Disable hotbar since we don't need it here, and it just blocks screen space.
        hideHotbar = true;
    }

    @Override
    public void tick() {
        super.tick();

        final Vec3 keyboardCenter = Vec3.atCenterOf(keyboard.getBlockPos());
        if (!keyboard.isValid() || getMinecraft().player == null || getMinecraft().player.distanceToSqr(keyboardCenter) > 8 * 8) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(final int keycode, final int scancode, final int modifiers) {
        sendInputMessage(keycode, true);
        return true;
    }

    @Override
    public boolean keyReleased(final int keycode, final int scancode, final int modifiers) {
        sendInputMessage(keycode, false);
        return true;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
            onClose();
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void render(final PoseStack stack, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(stack, mouseX, mouseY, partialTicks);

        renderBorderOverlay(stack);

        font.drawWordWrap(CLOSE_INFO, BORDER_SIZE * 3, height - BORDER_SIZE * 3 - font.lineHeight, width - BORDER_SIZE * 6, 0x88FFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();

        hideHotbar = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onOverlayRenderPre(RenderGuiOverlayEvent.Pre event) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.isSpectator()) return;
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() && hideHotbar) {
            event.setCanceled(true);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void renderBorderOverlay(final PoseStack stack) {
        blitQuad(stack, BORDER_SIZE, BORDER_SIZE, width - BORDER_SIZE, BORDER_SIZE * 2, BORDER_COLOR);
        blitQuad(stack, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE * 2, height - BORDER_SIZE, BORDER_COLOR);
        blitQuad(stack, BORDER_SIZE, height - BORDER_SIZE * 2, width - BORDER_SIZE, height - BORDER_SIZE, BORDER_COLOR);
        blitQuad(stack, width - BORDER_SIZE * 2, BORDER_SIZE, width - BORDER_SIZE, height - BORDER_SIZE, BORDER_COLOR);
    }

    private void blitQuad(final PoseStack stack, final int x0, final int y0, final int x1, final int y1, final int color) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final Tesselator tesselator = Tesselator.getInstance();
        final BufferBuilder builder = tesselator.getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.vertex(stack.last().pose(), x0, y1, getBlitOffset()).color(color).endVertex();
        builder.vertex(stack.last().pose(), x1, y1, getBlitOffset()).color(color).endVertex();
        builder.vertex(stack.last().pose(), x1, y0, getBlitOffset()).color(color).endVertex();
        builder.vertex(stack.last().pose(), x0, y0, getBlitOffset()).color(color).endVertex();
        tesselator.end();
    }

    private void grabMouse() {
        final Minecraft minecraft = getMinecraft();
        final MouseHandler mouseHandler = minecraft.mouseHandler;
        mouseHandler.mouseGrabbed = true;
        InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), InputConstants.CURSOR_DISABLED, mouseHandler.xpos(), mouseHandler.ypos());
    }

    private void sendInputMessage(final int keycode, final boolean isDown) {
        if (KeyCodeMapping.MAPPING.containsKey(keycode)) {
            swingArm();
            final int evdevCode = KeyCodeMapping.MAPPING.get(keycode);
            Network.sendToServer(new KeyboardInputMessage(keyboard, evdevCode, isDown));
        }
    }

    private void swingArm() {
        final Minecraft minecraft = getMinecraft();
        final LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }

        final RandomSource random = player.getRandom();
        if (random.nextFloat() < ARM_SWING_RATE) {
            return;
        }

        final InteractionHand handToSwing;
        if (minecraft.options.getCameraType().isFirstPerson()) {
            handToSwing = InteractionHand.MAIN_HAND;
        } else {
            handToSwing = random.nextBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        }

        player.swing(handToSwing);
    }
}
