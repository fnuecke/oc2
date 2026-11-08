/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.client.gui.layout.*;
import li.cil.oc2.client.gui.node.RobotSelectionNode;
import li.cil.oc2.client.gui.node.TankNode;
import li.cil.oc2.common.container.RobotInventoryContainer;
import li.cil.oc2.common.container.RobotSlot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

import static li.cil.oc2.client.gui.layout.SlotNode.SLOT_SIZE;

@Environment(EnvType.CLIENT)
public final class RobotContainerScreen extends UiContainerScreen<RobotInventoryContainer> {
    private static final ResourceLocation MAINBOARD = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "robot/mainboard");
    private static final int MAINBOARD_WIDTH = 103, MAINBOARD_HEIGHT = 81;
    private static final int INVENTORY_COLUMNS = 3;
    private static final int TANK_GAP = 1;

    // --------------------------------------------------------------------- //

    private final Inventory inventory;

    // --------------------------------------------------------------------- //

    public RobotContainerScreen(final RobotInventoryContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title, Sprites.THEME);
        this.inventory = inventory;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Window createWindow() {
        final List<Slot> robotSlots = menu.slots.stream().filter(slot -> slot instanceof RobotSlot).toList();
        final NodeStack robotInventory = Ui.slotGrid(robotSlots, INVENTORY_COLUMNS)
            .add(new RobotSelectionNode(menu.getRobot(), INVENTORY_COLUMNS));

        return new Window(
            NodeStack.vertical(
                Ui.section(title, NodeStack.horizontal(
                    MachineScreens.mainboard(menu, MAINBOARD, MAINBOARD_WIDTH, MAINBOARD_HEIGHT, RobotContainerScreen::placeSlot),
                    NodeStack.vertical(robotInventory, new TankNode(menu)).spacing(TANK_GAP)
                )),
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
            node.position(62, 40 + index * SLOT_SIZE);
        } else if (type == DeviceTypes.FLOPPY.get()) {
            node.position(80, 40 + index * SLOT_SIZE);
        } else if (type == DeviceTypes.ROBOT_MODULE.get()) {
            node.position(0, 4 + index * SLOT_SIZE);
        } else {
            throw new IllegalArgumentException("Unexpected device type " + type);
        }
    }
}
