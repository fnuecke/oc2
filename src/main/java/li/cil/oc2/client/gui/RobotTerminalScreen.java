package li.cil.oc2.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.common.container.RobotTerminalContainer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

public final class RobotTerminalScreen extends AbstractMachineTerminalScreen<RobotTerminalContainer> {
    private static final int SLOTS_X = (MachineTerminalWidget.WIDTH - Sprites.HOTBAR.width) / 2;
    private static final int SLOTS_Y = MachineTerminalWidget.HEIGHT - 1;

    ///////////////////////////////////////////////////////////////////

    public RobotTerminalScreen(final RobotTerminalContainer container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
    }

    @Override
    protected void renderBg(final PoseStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        Sprites.HOTBAR.draw(matrixStack, leftPos + SLOTS_X, topPos + SLOTS_Y);

        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }

    @Override
    public void render(final PoseStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        RobotContainerScreen.renderSelection(matrixStack, menu.getRobot().getSelectedSlot(), leftPos + SLOTS_X + 4, topPos + SLOTS_Y + 4, 12);
        renderTooltip(matrixStack, mouseX, mouseY);
    }
}
