/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class BusInterfaceScreen extends Screen {
    private static final int TEXT_LEFT = 9;
    private static final int TEXT_TOP = 11;
    private static final int CONFIRM_LEFT = 206;
    private static final int CONFIRM_TOP = 9;
    private static final int CANCEL_LEFT = 219;
    private static final int CANCEL_TOP = 9;

    private final BusCableBlockEntity busCable;
    private final Direction side;

    private EditBox nameField;

    private int left, top;

    ///////////////////////////////////////////////////////////////////

    public BusInterfaceScreen(final BusCableBlockEntity busCable, final Direction side) {
        super(Items.BUS_INTERFACE.get().getDescription());
        this.busCable = busCable;
        this.side = side;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();

        getMinecraft().keyboardHandler.setSendRepeatsToGui(true);

        left = (width - Sprites.BUS_INTERFACE_SCREEN.width) / 2;
        top = (height - Sprites.BUS_INTERFACE_SCREEN.height) / 2;

        nameField = new EditBox(font, left + TEXT_LEFT, top + TEXT_TOP, 192, 12, text("{mod}.gui.bus_interface_name"));
        nameField.setCanLoseFocus(false);
        nameField.setTextColor(0xFFFFFFFF);
        nameField.setBordered(false);
        nameField.setMaxLength(32);
        nameField.setValue(busCable.getInterfaceName(side));
        addWidget(nameField);
        setInitialFocus(nameField);

        addRenderableWidget(new ImageButton(
            left + CONFIRM_LEFT, top + CONFIRM_TOP,
            Sprites.CONFIRM_BASE.width, Sprites.CONFIRM_BASE.height,
            Sprites.CONFIRM_BASE,
            Sprites.CONFIRM_PRESSED
        ) {
            @Override
            public void onPress() {
                super.onPress();
                setInterfaceName(nameField.getValue());
                onClose();
            }
        }).withTooltip(Component.translatable(Constants.TOOLTIP_CONFIRM));

        addRenderableWidget(new ImageButton(
            left + CANCEL_LEFT, top + CANCEL_TOP,
            Sprites.CANCEL_BASE.width, Sprites.CANCEL_BASE.height,
            Sprites.CANCEL_BASE,
            Sprites.CANCEL_PRESSED
        ) {
            @Override
            public void onPress() {
                super.onPress();
                onClose();
            }
        }).withTooltip(Component.translatable(Constants.TOOLTIP_CANCEL));
    }

    @Override
    public void onClose() {
        super.onClose();

        getMinecraft().keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public void tick() {
        super.tick();
        nameField.tick();

        final Vec3 busCableCenter = Vec3.atCenterOf(busCable.getBlockPos());
        if (!busCable.isValid() ||
            getMinecraft().player == null ||
            getMinecraft().player.distanceToSqr(busCableCenter) > 8 * 8) {
            onClose();
        }
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER ||
            keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            setInterfaceName(nameField.getValue());
            onClose();
            return true;
        }

        return nameField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(final PoseStack stack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(stack);
        Sprites.BUS_INTERFACE_SCREEN.draw(stack, left, top);

        super.render(stack, mouseX, mouseY, partialTicks);

        RenderSystem.disableBlend();
        nameField.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////

    private void setInterfaceName(final String name) {
        Network.sendToServer(new BusInterfaceNameMessage.ToServer(busCable, side, name));
    }
}
