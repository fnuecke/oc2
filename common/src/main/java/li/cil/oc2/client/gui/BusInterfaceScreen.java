/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.layout.*;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import li.cil.sedna.api.device.bus.DeviceDescription;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class BusInterfaceScreen extends UiScreen {
    private static final int NAME_WIDTH = 198;

    // --------------------------------------------------------------------- //

    private final BusCableBlockEntity busCable;
    private final Direction side;
    private final EditBoxNode nameNode;
    private final EditBox nameField;

    // --------------------------------------------------------------------- //

    public BusInterfaceScreen(final BusCableBlockEntity busCable, final Direction side) {
        super(Items.BUS_INTERFACE.get().getDescription(), Sprites.THEME);
        this.busCable = busCable;
        this.side = side;

        nameNode = new EditBoxNode(text("{mod}.gui.bus_interface_name"));
        nameNode.width(Size.fixed(NAME_WIDTH));
        nameField = nameNode.editBox();
        nameField.setCanLoseFocus(false);
        nameField.setMaxLength(32);
        nameField.setFilter(value -> value.isEmpty() || DeviceDescription.isValidName(value));
        nameField.setValue(busCable.getInterfaceName(side));
    }

    // --------------------------------------------------------------------- //

    @Override
    public void tick() {
        super.tick();

        final Vec3 busCableCenter = Vec3.atCenterOf(busCable.getBlockPos());
        if (!busCable.isValid() ||
            minecraft.player == null ||
            minecraft.player.distanceToSqr(busCableCenter) > 8 * 8) {
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

    // --------------------------------------------------------------------- //

    @Override
    protected Window createWindow() {
        final ImageButton confirmButton = new ImageButton(0, 0,
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
        };
        confirmButton.withTooltip(Component.translatable(Constants.TOOLTIP_CONFIRM));

        final ImageButton cancelButton = new ImageButton(0, 0,
            Sprites.CANCEL_BASE.width, Sprites.CANCEL_BASE.height,
            Sprites.CANCEL_BASE,
            Sprites.CANCEL_PRESSED
        ) {
            @Override
            public void onPress() {
                super.onPress();
                onClose();
            }
        };
        cancelButton.withTooltip(Component.translatable(Constants.TOOLTIP_CANCEL));

        return new Window(NodeStack.horizontal(
            nameNode,
            new WidgetNode(confirmButton),
            new WidgetNode(cancelButton)
        ).alignment(Align.START, Align.CENTER));
    }

    @Override
    protected void init() {
        super.init();
        setInitialFocus(nameField);
    }

    // --------------------------------------------------------------------- //

    private void setInterfaceName(final String name) {
        Network.sendToServer(new BusInterfaceNameMessage.ToServer(busCable, side, name));
    }
}
