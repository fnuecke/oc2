package li.cil.oc2.client.gui;

import li.cil.oc2.common.container.ComputerTerminalContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

public final class ComputerTerminalScreen extends AbstractMachineTerminalScreen<ComputerTerminalContainer> {
    public ComputerTerminalScreen(final ComputerTerminalContainer container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
    }
}
