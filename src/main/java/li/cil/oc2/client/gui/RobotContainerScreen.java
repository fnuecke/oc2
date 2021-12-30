package li.cil.oc2.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.common.container.RobotInventoryContainer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;

public final class RobotContainerScreen extends AbstractMachineInventoryScreen<RobotInventoryContainer> {
    private static final int SLOT_SIZE = 18;

    ///////////////////////////////////////////////////////////////////

    public static void renderSelection(final PoseStack matrixStack, final int selectedSlot, final int x, final int y, final int columns) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        final int slotX = (selectedSlot % columns) * SLOT_SIZE;
        final int slotY = (selectedSlot / columns) * SLOT_SIZE;
        final int offset = SLOT_SIZE * (int) (15 * (System.currentTimeMillis() % 1000) / 1000);
        Sprites.SLOT_SELECTION.draw(matrixStack, x + slotX, y + slotY, 0, offset);
    }

    ///////////////////////////////////////////////////////////////////

    public RobotContainerScreen(final RobotInventoryContainer container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
        imageWidth = Sprites.ROBOT_CONTAINER.width;
        imageHeight = Sprites.ROBOT_CONTAINER.height;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(final PoseStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        renderSelection(matrixStack);

        renderMissingDeviceInfo(matrixStack, mouseX, mouseY);

        renderTooltip(matrixStack, mouseX, mouseY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final PoseStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        Sprites.ROBOT_CONTAINER.draw(matrixStack, leftPos, topPos);
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }

    ///////////////////////////////////////////////////////////////////

    private void renderSelection(final PoseStack matrixStack) {
        renderSelection(matrixStack, menu.getRobot().getSelectedSlot(), leftPos + 115, topPos + 23, 2);
    }
}
