/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import li.cil.oc2.common.blockentity.KeyboardBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.KeyboardInputMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.Vec3;

public final class KeyboardScreen extends Screen {
    private final KeyboardBlockEntity keyboard;

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
    }

    @Override
    public void tick() {
        super.tick();

        final Vec3 keyboardCenter = Vec3.atCenterOf(keyboard.getBlockPos());
        if (keyboard.isRemoved() ||
            getMinecraft().player == null ||
            getMinecraft().player.distanceToSqr(keyboardCenter) > 8 * 8) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(final int keycode, final int scancode, final int modifiers) {
        sendInputMessage(keycode, true);
        return super.keyPressed(keycode, scancode, modifiers);
    }

    @Override
    public boolean keyReleased(final int keycode, final int scancode, final int modifiers) {
        sendInputMessage(keycode, false);
        return super.keyReleased(keycode, scancode, modifiers);
    }

    @Override
    public void mouseMoved(final double p_94758_, final double p_94759_) {
        super.mouseMoved(p_94758_, p_94759_);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////

    private void grabMouse() {
        final Minecraft minecraft = getMinecraft();
        final MouseHandler mouseHandler = minecraft.mouseHandler;
        mouseHandler.mouseGrabbed = true;
        InputConstants.grabOrReleaseMouse(minecraft.getWindow().getWindow(), InputConstants.CURSOR_DISABLED, mouseHandler.xpos(), mouseHandler.ypos());
    }

    private void sendInputMessage(final int keycode, final boolean isDown) {
        if (KeyCodeMapping.MAPPING.containsKey(keycode)) {
            final int evdevCode = KeyCodeMapping.MAPPING.get(keycode);
            Network.sendToServer(new KeyboardInputMessage(keyboard, evdevCode, isDown));
        }
    }
}
