package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.common.container.RobotContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import static java.util.Objects.requireNonNull;

public final class RobotContainerScreen extends ContainerScreen<RobotContainer> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(API.MOD_ID, "textures/gui/container/robot.png");
    private static final ResourceLocation SELECTION = new ResourceLocation(API.MOD_ID, "textures/gui/overlay/robot_selection.png");

    private static final int SLOT_SIZE = 18;

    ///////////////////////////////////////////////////////////////////

    public RobotContainerScreen(final RobotContainer container, final PlayerInventory inventory, final ITextComponent title) {
        super(container, inventory, title);
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
        requireNonNull(minecraft).getTextureManager().bindTexture(BACKGROUND);
        blit(matrixStack, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    private void renderSelection(final MatrixStack matrixStack) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        requireNonNull(minecraft).getTextureManager().bindTexture(SELECTION);

        final int selectedSlot = container.getRobot().getSelectedSlot();
        final int x = 115 + (selectedSlot % 3) * SLOT_SIZE;
        final int y = 23 + (selectedSlot / 3) * SLOT_SIZE;
        final float offset = SLOT_SIZE * (int) (15 * (System.currentTimeMillis() % 1000) / 1000f);
        blit(matrixStack, guiLeft + x, guiTop + y, 0, offset, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, 270);
    }
}
