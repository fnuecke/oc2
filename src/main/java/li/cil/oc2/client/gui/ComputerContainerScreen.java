package li.cil.oc2.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.common.container.ComputerInventoryContainer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ComputerContainerScreen extends AbstractMachineInventoryScreen<ComputerInventoryContainer> {
    public ComputerContainerScreen(final ComputerInventoryContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title);
        imageWidth = Sprites.COMPUTER_CONTAINER.width;
        imageHeight = Sprites.COMPUTER_CONTAINER.height;
        inventoryLabelY = imageHeight - 94;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final PoseStack stack, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(stack, mouseX, mouseY, partialTicks);

        renderMissingDeviceInfo(stack, mouseX, mouseY);

        renderTooltip(stack, mouseX, mouseY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final PoseStack stack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        Sprites.COMPUTER_CONTAINER.draw(stack, leftPos, topPos);
        super.renderBg(stack, partialTicks, mouseX, mouseY);
    }
}
