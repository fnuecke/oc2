/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.client.gui.layout.*;
import li.cil.oc2.client.gui.node.EnergyNode;
import li.cil.oc2.client.gui.node.MissingDeviceNode;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.client.gui.widget.PowerButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMachineContainer;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import li.cil.oc2.common.container.DeviceTypeSlotItemHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public final class MachineScreens {
    @FunctionalInterface
    public interface SlotPlacement {
        void place(DeviceType type, int index, Node node);
    }

    // --------------------------------------------------------------------- //

    public static NodeStack[] toolbar(final AbstractMachineContainer menu, final AbstractWidget... controls) {
        final Node[] items = Stream.concat(Stream.of(new PowerButton(0, 0, menu)), Stream.of(controls))
            .map(WidgetNode::new)
            .toArray(Node[]::new);
        return new NodeStack[]{
            Ui.toolbar(items),
            Ui.toolbar(new EnergyNode(menu)).visibleWhen(() -> menu.getEnergyCapacity() > 0),
        };
    }

    public static AbstractWidget openTerminalButton(final AbstractMachineTerminalContainer menu) {
        return new ImageButton(0, 0, 12, 12, Sprites.INVENTORY_BUTTON_ACTIVE, Sprites.INVENTORY_BUTTON_INACTIVE) {
            @Override
            public void onPress(final InputWithModifiers input) {
                menu.switchToTerminal();
            }
        }.withTooltip(Component.translatable(Constants.MACHINE_OPEN_TERMINAL_CAPTION));
    }

    public static AbstractWidget openInventoryButton(final AbstractMachineContainer menu) {
        return new ImageButton(0, 0, 12, 12, Sprites.INVENTORY_BUTTON_INACTIVE, Sprites.INVENTORY_BUTTON_ACTIVE) {
            @Override
            public void onPress(final InputWithModifiers input) {
                menu.switchToInventory();
            }
        }.withTooltip(Component.translatable(Constants.MACHINE_OPEN_INVENTORY_CAPTION));
    }

    public static NodeStack mainboard(final AbstractMachineContainer menu, final Identifier sprite,
                                final int width, final int height, final SlotPlacement placement) {
        final NodeStack mainboard = NodeStack.vertical().size(width, height).background(sprite);
        final DeviceHints hints = DeviceHints.get();
        for (final Slot slot : menu.slots) {
            if (!(slot instanceof final DeviceTypeSlotItemHandler deviceSlot)) {
                continue;
            }

            final DeviceType type = deviceSlot.getDeviceType();
            final int index = slot.getContainerSlot();
            final SlotNode node = new SlotNode(slot);
            placement.place(type, index, node);
            mainboard.add(node);

            if (index == 0) {
                final List<DeviceHints.Hint> typeHints = hints.get(type);
                if (!typeHints.isEmpty()) {
                    final Node hint = new MissingDeviceNode(menu, type, typeHints);
                    placement.place(type, index, hint);
                    mainboard.add(hint);
                }
            }
        }
        return mainboard;
    }
}
