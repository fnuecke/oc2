/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.client.gui.layout.*;
import li.cil.oc2.common.container.ComputerInventoryContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import static li.cil.oc2.client.gui.layout.SlotNode.SLOT_SIZE;

@Environment(EnvType.CLIENT)
public final class ComputerContainerScreen extends UiContainerScreen<ComputerInventoryContainer> {
    private static final Identifier MAINBOARD = Identifier.fromNamespaceAndPath(API.MOD_ID, "computer/mainboard");
    private static final int MAINBOARD_WIDTH = 103;
    private static final int MAINBOARD_HEIGHT = 81;

    // --------------------------------------------------------------------- //

    private final Inventory inventory;

    // --------------------------------------------------------------------- //

    public ComputerContainerScreen(final ComputerInventoryContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title, Sprites.THEME);
        this.inventory = inventory;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Window createWindow() {
        return new Window(
            NodeStack.vertical(
                Ui.section(title, NodeStack.vertical(MachineScreens.mainboard(menu, MAINBOARD, MAINBOARD_WIDTH, MAINBOARD_HEIGHT, ComputerContainerScreen::placeSlot))
                    .alignment(Align.START, Align.CENTER).width(Size.GROW)),
                Ui.section(playerInventoryTitle, Ui.playerInventory(menu, inventory))
            ),
            MachineScreens.toolbar(menu, MachineScreens.openTerminalButton(menu)));
    }

    // --------------------------------------------------------------------- //

    private static void placeSlot(final DeviceType type, final int index, final Node node) {
        if (type == DeviceTypes.CPU.get()) {
            node.position(26, 32);
        } else if (type == DeviceTypes.FLASH_MEMORY.get()) {
            node.position(26, 58);
        } else if (type == DeviceTypes.MEMORY.get()) {
            node.position(26 + index * SLOT_SIZE, 4);
        } else if (type == DeviceTypes.HARD_DRIVE.get()) {
            node.position(62 + index % 2 * SLOT_SIZE, 40 + index / 2 * SLOT_SIZE);
        } else if (type == DeviceTypes.CARD.get()) {
            node.position(0, 4 + index * SLOT_SIZE);
        } else {
            throw new IllegalArgumentException("Unexpected device type " + type);
        }
    }
}
