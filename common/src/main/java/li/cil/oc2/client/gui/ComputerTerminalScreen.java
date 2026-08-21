/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.common.container.ComputerTerminalContainer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

@Environment(EnvType.CLIENT)
public final class ComputerTerminalScreen extends AbstractMachineTerminalScreen<ComputerTerminalContainer> {
    @SuppressWarnings("all")
    private EditBox focusIndicatorEditBox;

    // --------------------------------------------------------------------- //

    public ComputerTerminalScreen(final ComputerTerminalContainer container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void setFocusIndicatorEditBox(final EditBox editBox) {
        focusIndicatorEditBox = editBox;
    }
}
