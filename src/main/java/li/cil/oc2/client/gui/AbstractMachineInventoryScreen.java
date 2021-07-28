package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.client.gui.util.GuiUtils;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Arrays;
import java.util.List;

import static li.cil.oc2.common.util.TooltipUtils.withColor;

public abstract class AbstractMachineInventoryScreen<T extends AbstractMachineTerminalContainer> extends AbstractContainerScreen<T> {
    private static final int CONTROLS_TOP = 8;
    private static final int ENERGY_TOP = CONTROLS_TOP + Sprites.SIDEBAR_2.height + 4;

    ///////////////////////////////////////////////////////////////////

    public AbstractMachineInventoryScreen(final T container, final PlayerInventory playerInventory, final ITextComponent title) {
        super(container, playerInventory, title);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();

        addButton(new ToggleImageButton(
                this, leftPos - Sprites.SIDEBAR_3.width + 4, topPos + CONTROLS_TOP + 4,
                12, 12,
                new TranslationTextComponent(Constants.COMPUTER_SCREEN_POWER_CAPTION),
                new TranslationTextComponent(Constants.COMPUTER_SCREEN_POWER_DESCRIPTION),
                Sprites.POWER_BUTTON_BASE,
                Sprites.POWER_BUTTON_PRESSED,
                Sprites.POWER_BUTTON_ACTIVE
        ) {
            @Override
            public void onPress() {
                super.onPress();
                menu.sendPowerStateToServer(!menu.getVirtualMachine().isRunning());
            }

            @Override
            public boolean isToggled() {
                return menu.getVirtualMachine().isRunning();
            }
        });

        addButton(new ImageButton(
                this, leftPos - Sprites.SIDEBAR_3.width + 4, topPos + CONTROLS_TOP + 4 + 14,
                12, 12,
                new TranslationTextComponent(Constants.MACHINE_OPEN_TERMINAL_CAPTION),
                null,
                Sprites.INVENTORY_BUTTON_ACTIVE,
                Sprites.INVENTORY_BUTTON_INACTIVE
        ) {
            @Override
            public void onPress() {
                menu.switchToTerminal();
            }
        });
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        final int energyCapacity = menu.getEnergyCapacity();
        if (energyCapacity > 0) {
            final int energyStored = menu.getEnergy();
            final int energyConsumption = menu.getEnergyConsumption();

            Sprites.ENERGY_BAR.drawFillY(matrixStack, leftPos - Sprites.SIDEBAR_2.width + 4, topPos + ENERGY_TOP + 4, energyStored / (float) energyCapacity);

            if (isMouseOver(mouseX, mouseY, -Sprites.SIDEBAR_2.width + 4, ENERGY_TOP + 4, Sprites.ENERGY_BAR.width, Sprites.ENERGY_BAR.height)) {
                final List<? extends ITextProperties> tooltip = Arrays.asList(
                        new TranslationTextComponent(Constants.TOOLTIP_ENERGY, withColor(energyStored + "/" + energyCapacity, TextFormatting.GREEN)),
                        new TranslationTextComponent(Constants.TOOLTIP_ENERGY_CONSUMPTION, withColor(String.valueOf(energyConsumption), TextFormatting.GREEN))
                );
                net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(matrixStack, tooltip, mouseX, mouseY, width, height, 200, font);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final MatrixStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        Sprites.SIDEBAR_2.draw(matrixStack, leftPos - Sprites.SIDEBAR_2.width, topPos + CONTROLS_TOP);

        if (menu.getEnergyCapacity() > 0) {
            final int x = leftPos - Sprites.SIDEBAR_2.width;
            final int y = topPos + ENERGY_TOP;
            Sprites.SIDEBAR_2.draw(matrixStack, x, y);
            Sprites.ENERGY_BASE.draw(matrixStack, x + 4, y + 4);
        }
    }

    protected void renderMissingDeviceInfo(final MatrixStack matrixStack, final int mouseX, final int mouseY) {
        GuiUtils.renderMissingDeviceInfoIcon(matrixStack, this, DeviceTypes.FLASH_MEMORY, Sprites.WARN_ICON);
        GuiUtils.renderMissingDeviceInfoIcon(matrixStack, this, DeviceTypes.MEMORY, Sprites.WARN_ICON);
        GuiUtils.renderMissingDeviceInfoIcon(matrixStack, this, DeviceTypes.HARD_DRIVE, Sprites.INFO_ICON);

        GuiUtils.renderMissingDeviceInfoTooltip(matrixStack, this, mouseX, mouseY, DeviceTypes.FLASH_MEMORY);
        GuiUtils.renderMissingDeviceInfoTooltip(matrixStack, this, mouseX, mouseY, DeviceTypes.MEMORY);
        GuiUtils.renderMissingDeviceInfoTooltip(matrixStack, this, mouseX, mouseY, DeviceTypes.HARD_DRIVE);
    }
}
