package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.oc2.common.container.RobotTerminalContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public final class RobotTerminalScreen extends AbstractMachineTerminalScreen<RobotTerminalContainer> {
    private static final int SLOTS_X = (AbstractMachineTerminalWidget.WIDTH - Sprites.HOTBAR.width) / 2;
    private static final int SLOTS_Y = AbstractMachineTerminalWidget.HEIGHT - 1;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalScreen(final RobotTerminalContainer container, final PlayerInventory playerInventory, final ITextComponent title) {
        super(container, playerInventory, title);
    }

    @Override
    protected void renderBg(final MatrixStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        Sprites.HOTBAR.draw(matrixStack, leftPos + SLOTS_X, topPos + SLOTS_Y);
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        RobotContainerScreen.renderSelection(matrixStack, menu.getRobot().getSelectedSlot(), leftPos + SLOTS_X + 4, topPos + SLOTS_Y + 4, 12);
        renderTooltip(matrixStack, mouseX, mouseY);
    }
}
