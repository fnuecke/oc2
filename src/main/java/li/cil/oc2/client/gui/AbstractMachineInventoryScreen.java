/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.client.gui.util.GuiUtils;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractMachineInventoryScreen<T extends AbstractMachineTerminalContainer> extends AbstractModContainerScreen<T> {
    private static final int CONTROLS_TOP = 8;
    private static final int ENERGY_TOP = CONTROLS_TOP + Sprites.SIDEBAR_2.height + 4;

    ///////////////////////////////////////////////////////////////////

    public AbstractMachineInventoryScreen(final T container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
    }

    ///////////////////////////////////////////////////////////////////

    public List<Rect2i> getExtraAreas() {
        final List<Rect2i> list = new ArrayList<>();
        list.add(new Rect2i(
            leftPos - Sprites.SIDEBAR_2.width, topPos + CONTROLS_TOP,
            Sprites.SIDEBAR_2.width, Sprites.SIDEBAR_2.height
        ));

        if (shouldRenderEnergyBar()) {
            list.add(new Rect2i(
                leftPos - Sprites.SIDEBAR_2.width, topPos + ENERGY_TOP,
                Sprites.SIDEBAR_2.width, Sprites.SIDEBAR_2.height
            ));
        }

        return list;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(new ToggleImageButton(
            leftPos - Sprites.SIDEBAR_3.width + 4, topPos + CONTROLS_TOP + 4,
            12, 12,
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
        }).withTooltip(
            Component.translatable(Constants.COMPUTER_SCREEN_POWER_CAPTION),
            Component.translatable(Constants.COMPUTER_SCREEN_POWER_DESCRIPTION)
        );

        addRenderableWidget(new ImageButton(
            leftPos - Sprites.SIDEBAR_3.width + 4, topPos + CONTROLS_TOP + 4 + 14,
            12, 12,
            Sprites.INVENTORY_BUTTON_ACTIVE,
            Sprites.INVENTORY_BUTTON_INACTIVE
        ) {
            @Override
            public void onPress() {
                menu.switchToTerminal();
            }
        }.withTooltip(Component.translatable(Constants.MACHINE_OPEN_TERMINAL_CAPTION)));
    }

    @Override
    protected void renderBg(final PoseStack stack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        Sprites.SIDEBAR_2.draw(stack, leftPos - Sprites.SIDEBAR_2.width, topPos + CONTROLS_TOP);

        if (shouldRenderEnergyBar()) {
            final int x = leftPos - Sprites.SIDEBAR_2.width;
            final int y = topPos + ENERGY_TOP;
            Sprites.SIDEBAR_2.draw(stack, x, y);
            Sprites.ENERGY_BASE.draw(stack, x + 4, y + 4);
        }
    }

    @Override
    protected void renderFg(final PoseStack stack, final float partialTicks, final int mouseX, final int mouseY) {
        super.renderFg(stack, partialTicks, mouseX, mouseY);

        GuiUtils.renderMissingDeviceInfoIcon(stack, this, DeviceTypes.FLASH_MEMORY, Sprites.WARN_ICON);
        GuiUtils.renderMissingDeviceInfoIcon(stack, this, DeviceTypes.MEMORY, Sprites.WARN_ICON);
        GuiUtils.renderMissingDeviceInfoIcon(stack, this, DeviceTypes.HARD_DRIVE, Sprites.INFO_ICON);

        if (shouldRenderEnergyBar()) {
            final int x = leftPos - Sprites.SIDEBAR_2.width + 4;
            final int y = topPos + ENERGY_TOP + 4;
            Sprites.ENERGY_BAR.drawFillY(stack, x, y, menu.getEnergy() / (float) menu.getEnergyCapacity());
        }
    }

    @Override
    protected void renderTooltip(final PoseStack stack, final int mouseX, final int mouseY) {
        super.renderTooltip(stack, mouseX, mouseY);

        GuiUtils.renderMissingDeviceInfoTooltip(stack, this, mouseX, mouseY, DeviceTypes.FLASH_MEMORY);
        GuiUtils.renderMissingDeviceInfoTooltip(stack, this, mouseX, mouseY, DeviceTypes.MEMORY);
        GuiUtils.renderMissingDeviceInfoTooltip(stack, this, mouseX, mouseY, DeviceTypes.HARD_DRIVE);

        if (shouldRenderEnergyBar()) {
            if (isMouseOver(mouseX, mouseY, -Sprites.SIDEBAR_2.width + 4, ENERGY_TOP + 4, Sprites.ENERGY_BAR.width, Sprites.ENERGY_BAR.height)) {
                final List<? extends FormattedText> tooltip = asList(
                    Component.translatable(Constants.TOOLTIP_ENERGY,
                        withFormat(menu.getEnergy() + "/" + menu.getEnergyCapacity(), ChatFormatting.GREEN)),
                    Component.translatable(Constants.TOOLTIP_ENERGY_CONSUMPTION,
                        withFormat(String.valueOf(menu.getEnergyConsumption()), ChatFormatting.GREEN))
                );
                TooltipUtils.drawTooltip(stack, tooltip, mouseX, mouseY, 200);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    private boolean shouldRenderEnergyBar() {
        return menu.getEnergyCapacity() > 0;
    }
}
