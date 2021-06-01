package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.client.gui.util.GuiUtils;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.RobotContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Component;
import net.minecraft.util.text.TranslatableComponent;

public final class RobotContainerScreen extends ContainerScreen<RobotContainer> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(API.MOD_ID, "textures/gui/container/robot.png");
    private static final ResourceLocation SELECTION = new ResourceLocation(API.MOD_ID, "textures/gui/overlay/robot_selection.png");

    private static final int SLOT_SIZE = 18;

    ///////////////////////////////////////////////////////////////////

    public static void renderSelection(final PoseStack matrixStack, final int selectedSlot, final int x, final int y, final int columns) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        Minecraft.getInstance().getTextureManager().bind(SELECTION);

        final int slotX = (selectedSlot % columns) * SLOT_SIZE;
        final int slotY = (selectedSlot / columns) * SLOT_SIZE;
        final float offset = SLOT_SIZE * (int) (15 * (System.currentTimeMillis() % 1000) / 1000f);
        blit(matrixStack, x + slotX, y + slotY, 0, offset, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, 270);
    }

    ///////////////////////////////////////////////////////////////////

    public RobotContainerScreen(final RobotContainer container, final PlayerInventory playerInventory, final Component title) {
        super(container, playerInventory, title);
        imageWidth = 176;
        imageHeight = 197;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void render(final PoseStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        renderSelection(matrixStack);

        GuiUtils.renderMissingDeviceInfoIcon(matrixStack, this, DeviceTypes.FLASH_MEMORY, GuiUtils.WARN_ICON);
        GuiUtils.renderMissingDeviceInfoIcon(matrixStack, this, DeviceTypes.MEMORY, GuiUtils.WARN_ICON);
        GuiUtils.renderMissingDeviceInfoIcon(matrixStack, this, DeviceTypes.HARD_DRIVE, GuiUtils.INFO_ICON);

        GuiUtils.renderMissingDeviceInfoTooltip(matrixStack, this, mouseX, mouseY, DeviceTypes.FLASH_MEMORY, new TranslatableComponent(Constants.TOOLTIP_FLASH_MEMORY_MISSING));
        GuiUtils.renderMissingDeviceInfoTooltip(matrixStack, this, mouseX, mouseY, DeviceTypes.MEMORY, new TranslatableComponent(Constants.TOOLTIP_MEMORY_MISSING));
        GuiUtils.renderMissingDeviceInfoTooltip(matrixStack, this, mouseX, mouseY, DeviceTypes.HARD_DRIVE, new TranslatableComponent(Constants.TOOLTIP_HARD_DRIVE_MISSING));

        renderTooltip(matrixStack, mouseX, mouseY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final PoseStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        getMinecraft().getTextureManager().bind(BACKGROUND);
        blit(matrixStack, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    ///////////////////////////////////////////////////////////////////

    private void renderSelection(final PoseStack matrixStack) {
        renderSelection(matrixStack, menu.getRobot().getSelectedSlot(), leftPos + 115, topPos + 23, 2);
    }
}
