/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.layout.Align;
import li.cil.oc2.client.gui.layout.Node;
import li.cil.oc2.client.gui.layout.NodeStack;
import li.cil.oc2.client.gui.layout.Ui;
import li.cil.oc2.client.gui.node.RobotSelectionNode;
import li.cil.oc2.common.container.RobotSlot;
import li.cil.oc2.common.container.RobotTerminalContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class RobotTerminalScreen extends AbstractMachineTerminalScreen<RobotTerminalContainer> {
    @SuppressWarnings("all")
    private EditBox focusIndicatorEditBox;

    // --------------------------------------------------------------------- //

    public RobotTerminalScreen(final RobotTerminalContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Node createContent() {
        final List<Slot> robotSlots = menu.slots.stream().filter(slot -> slot instanceof RobotSlot).toList();
        final NodeStack slots = Ui.slotGrid(robotSlots, robotSlots.size())
            .add(new RobotSelectionNode(menu.getRobot(), robotSlots.size()));
        return NodeStack.vertical(super.createContent(), slots).alignment(Align.START, Align.CENTER);
    }

    @Override
    protected void setFocusIndicatorEditBox(final EditBox editBox) {
        focusIndicatorEditBox = editBox;
    }
}
