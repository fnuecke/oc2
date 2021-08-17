package li.cil.oc2.client.gui;

import li.cil.oc2.common.container.ComputerTerminalContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class ComputerTerminalScreen extends AbstractMachineTerminalScreen<ComputerTerminalContainer> {
    public ComputerTerminalScreen(final ComputerTerminalContainer container, final PlayerInventory playerInventory, final ITextComponent title) {
        super(container, playerInventory, title);
    }
}
