package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.client.gui.util.GuiUtils;
import li.cil.oc2.common.container.ComputerInventoryContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import static java.util.Objects.requireNonNull;

public final class ComputerInventoryScreen extends ContainerScreen<ComputerInventoryContainer> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(API.MOD_ID, "textures/gui/container/computer.png");

    ///////////////////////////////////////////////////////////////////

    public ComputerInventoryScreen(final ComputerInventoryContainer container, final PlayerInventory inventory, final ITextComponent title) {
        super(container, inventory, title);
        imageWidth = 176;
        imageHeight = 197;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        GuiUtils.renderMissingDeviceInfoIcon(matrixStack, this, DeviceTypes.FLASH_MEMORY, GuiUtils.WARN_ICON);
        GuiUtils.renderMissingDeviceInfoIcon(matrixStack, this, DeviceTypes.MEMORY, GuiUtils.WARN_ICON);
        GuiUtils.renderMissingDeviceInfoIcon(matrixStack, this, DeviceTypes.HARD_DRIVE, GuiUtils.INFO_ICON);

        GuiUtils.renderMissingDeviceInfoTooltip(matrixStack, this, mouseX, mouseY, DeviceTypes.FLASH_MEMORY);
        GuiUtils.renderMissingDeviceInfoTooltip(matrixStack, this, mouseX, mouseY, DeviceTypes.MEMORY);
        GuiUtils.renderMissingDeviceInfoTooltip(matrixStack, this, mouseX, mouseY, DeviceTypes.HARD_DRIVE);

        renderTooltip(matrixStack, mouseX, mouseY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final MatrixStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        requireNonNull(minecraft).getTextureManager().bind(BACKGROUND);
        blit(matrixStack, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
