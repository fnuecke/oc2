/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
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
public abstract class AbstractMachineTerminalScreen<T extends AbstractMachineTerminalContainer> extends AbstractModContainerScreen<T> {
    private static final int CONTROLS_TOP = 8;
    private static final int ENERGY_TOP = CONTROLS_TOP + Sprites.SIDEBAR_3.height + 4;

    private static boolean isInputCaptureEnabled;

    private final MachineTerminalWidget terminalWidget;

    ///////////////////////////////////////////////////////////////////

    protected AbstractMachineTerminalScreen(final T container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
        this.terminalWidget = new MachineTerminalWidget(this);
        imageWidth = Sprites.TERMINAL_SCREEN.width;
        imageHeight = Sprites.TERMINAL_SCREEN.height;
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean isInputCaptureEnabled() {
        return isInputCaptureEnabled;
    }

    public List<Rect2i> getExtraAreas() {
        final List<Rect2i> list = new ArrayList<>();
        list.add(new Rect2i(
            leftPos - Sprites.SIDEBAR_3.width, topPos + CONTROLS_TOP,
            Sprites.SIDEBAR_3.width, Sprites.SIDEBAR_3.height
        ));

        if (shouldRenderEnergyBar()) {
            list.add(new Rect2i(
                leftPos - Sprites.SIDEBAR_2.width, topPos + ENERGY_TOP,
                Sprites.SIDEBAR_2.width, Sprites.SIDEBAR_2.height
            ));
        }

        return list;
    }

    @Override
    public void containerTick() {
        super.containerTick();

        terminalWidget.tick();
    }

    @Override
    public boolean charTyped(final char ch, final int modifiers) {
        return terminalWidget.charTyped(ch, modifiers) ||
            super.charTyped(ch, modifiers);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (terminalWidget.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        // Don't close with inventory binding since we usually want to use that as terminal input
        // even without input capture enabled.
        final InputConstants.Key input = InputConstants.getKey(keyCode, scanCode);
        if (getMinecraft().options.keyInventory.isActiveAndMatches(input)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void init() {
        super.init();
        terminalWidget.init();

        final EditBox focusIndicatorEditBox = new EditBox(font, 0, 0, 0, 0, Component.empty());
        focusIndicatorEditBox.setFocus(true);
        setFocusIndicatorEditBox(focusIndicatorEditBox);

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

        addRenderableWidget(new ToggleImageButton(
            leftPos - Sprites.SIDEBAR_3.width + 4, topPos + CONTROLS_TOP + 4 + 14,
            12, 12,
            Sprites.INPUT_BUTTON_BASE,
            Sprites.INPUT_BUTTON_PRESSED,
            Sprites.INPUT_BUTTON_ACTIVE
        ) {
            @Override
            public void onPress() {
                super.onPress();
                isInputCaptureEnabled = !isInputCaptureEnabled;
            }

            @Override
            public boolean isToggled() {
                return isInputCaptureEnabled;
            }
        }).withTooltip(
            Component.translatable(Constants.TERMINAL_CAPTURE_INPUT_CAPTION),
            Component.translatable(Constants.TERMINAL_CAPTURE_INPUT_DESCRIPTION)
        );

        addRenderableWidget(new ImageButton(
            leftPos - Sprites.SIDEBAR_3.width + 4, topPos + CONTROLS_TOP + 4 + 14 + 14,
            12, 12,
            Sprites.INVENTORY_BUTTON_INACTIVE,
            Sprites.INVENTORY_BUTTON_ACTIVE
        ) {
            @Override
            public void onPress() {
                menu.switchToInventory();
            }
        }).withTooltip(Component.translatable(Constants.MACHINE_OPEN_INVENTORY_CAPTION));
    }

    @Override
    public void onClose() {
        super.onClose();
        terminalWidget.onClose();
    }

    ///////////////////////////////////////////////////////////////////

    // We use this text box to indicate to Forge that we want all input, and event handlers should not be allowed
    // to steal input from us (e.g. via custom key bindings). Since Forge is lazy and just uses getDeclaredFields
    // to get private fields, which completely skips fields in base classes, we require subclasses to hold the field...
    protected abstract void setFocusIndicatorEditBox(final EditBox editBox);

    @Override
    protected void renderFg(final PoseStack stack, final float partialTicks, final int mouseX, final int mouseY) {
        super.renderFg(stack, partialTicks, mouseX, mouseY);

        if (shouldRenderEnergyBar()) {
            final int x = leftPos - Sprites.SIDEBAR_2.width + 4;
            final int y = topPos + ENERGY_TOP + 4;
            Sprites.ENERGY_BAR.drawFillY(stack, x, y, menu.getEnergy() / (float) menu.getEnergyCapacity());
        }

        terminalWidget.render(stack, mouseX, mouseY, menu.getVirtualMachine().getError());
    }

    @Override
    protected void renderBg(final PoseStack stack, final float partialTicks, final int mouseX, final int mouseY) {
        Sprites.SIDEBAR_3.draw(stack, leftPos - Sprites.SIDEBAR_3.width, topPos + CONTROLS_TOP);

        if (shouldRenderEnergyBar()) {
            final int x = leftPos - Sprites.SIDEBAR_2.width;
            final int y = topPos + ENERGY_TOP;
            Sprites.SIDEBAR_2.draw(stack, x, y);
            Sprites.ENERGY_BASE.draw(stack, x + 4, y + 4);
        }

        terminalWidget.renderBackground(stack, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(final PoseStack stack, final int mouseX, final int mouseY) {
        super.renderTooltip(stack, mouseX, mouseY);

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

    @Override
    protected void renderLabels(final PoseStack stack, final int mouseX, final int mouseY) {
        // This is required to prevent the labels from being rendered
    }

    ///////////////////////////////////////////////////////////////////

    private boolean shouldRenderEnergyBar() {
        return menu.getEnergyCapacity() > 0;
    }
}
