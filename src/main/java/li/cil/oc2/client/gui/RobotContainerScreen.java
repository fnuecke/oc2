package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.common.container.RobotContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public final class RobotContainerScreen extends ContainerScreen<RobotContainer> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(API.MOD_ID, "textures/gui/container/robot.png");
    private static final ResourceLocation SELECTION = new ResourceLocation(API.MOD_ID, "textures/gui/overlay/robot_selection.png");

    private static final int SLOT_SIZE = 18;

    ///////////////////////////////////////////////////////////////////

    public static void renderSelection(final MatrixStack matrixStack, final int selectedSlot, final int x, final int y, final int columns) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        Minecraft.getInstance().getTextureManager().bindTexture(SELECTION);

        final int slotX = (selectedSlot % columns) * SLOT_SIZE;
        final int slotY = (selectedSlot / columns) * SLOT_SIZE;
        final float offset = SLOT_SIZE * (int) (15 * (System.currentTimeMillis() % 1000) / 1000f);
        blit(matrixStack, x + slotX, y + slotY, 0, offset, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, 270);
    }

    ///////////////////////////////////////////////////////////////////

    public RobotContainerScreen(final RobotContainer container, final PlayerInventory playerInventory, final ITextComponent title) {
        super(container, playerInventory, title);
        xSize = 176;
        ySize = 197;
        playerInventoryTitleY = ySize - 94;
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        renderSelection(matrixStack);
        renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void drawGuiContainerBackgroundLayer(final MatrixStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        getMinecraft().getTextureManager().bindTexture(BACKGROUND);
        blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    ///////////////////////////////////////////////////////////////////

    private void renderSelection(final MatrixStack matrixStack) {
        renderSelection(matrixStack, container.getRobot().getSelectedSlot(), guiLeft + 115, guiTop + 23, 2);
    }
}
