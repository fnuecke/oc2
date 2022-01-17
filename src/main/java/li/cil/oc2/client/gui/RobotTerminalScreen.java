package li.cil.oc2.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.common.container.RobotTerminalContainer;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RobotTerminalScreen extends AbstractMachineTerminalScreen<RobotTerminalContainer> {
    private static final int SLOTS_X = (MachineTerminalWidget.WIDTH - Sprites.HOTBAR.width) / 2;
    private static final int SLOTS_Y = MachineTerminalWidget.HEIGHT - 1;

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("all") private EditBox focusIndicatorEditBox;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalScreen(final RobotTerminalContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final PoseStack stack, final float partialTicks, final int mouseX, final int mouseY) {
        Sprites.HOTBAR.draw(stack, leftPos + SLOTS_X, topPos + SLOTS_Y);
        RobotContainerScreen.renderSelection(stack, menu.getRobot().getSelectedSlot(), leftPos + SLOTS_X + 4, topPos + SLOTS_Y + 4, 12);

        super.renderBg(stack, partialTicks, mouseX, mouseY);
    }

    @Override
    protected void setFocusIndicatorEditBox(final EditBox editBox) {
        focusIndicatorEditBox = editBox;
    }
}
