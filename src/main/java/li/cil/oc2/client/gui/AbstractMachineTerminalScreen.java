package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.GuiUtils;

import java.util.List;

import static java.util.Arrays.asList;
import static li.cil.oc2.common.util.TooltipUtils.withColor;

public abstract class AbstractMachineTerminalScreen<T extends AbstractMachineTerminalContainer> extends AbstractContainerScreen<T> {
    private static final int CONTROLS_TOP = 8;
    private static final int ENERGY_TOP = CONTROLS_TOP + Sprites.SIDEBAR_3.height + 4;

    private static boolean isInputCaptureEnabled;

    private final MachineTerminalWidget terminalWidget;

    ///////////////////////////////////////////////////////////////////

    protected AbstractMachineTerminalScreen(final T container, final PlayerInventory playerInventory, final ITextComponent title) {
        super(container, playerInventory, title);
        this.terminalWidget = new MachineTerminalWidget(this);
        imageWidth = Sprites.TERMINAL_SCREEN.width;
        imageHeight = Sprites.TERMINAL_SCREEN.height;
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean isInputCaptureEnabled() {
        return isInputCaptureEnabled;
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        terminalWidget.render(matrixStack, mouseX, mouseY, menu.getVirtualMachine().getBootError());

        final int energyCapacity = menu.getEnergyCapacity();
        if (energyCapacity > 0) {
            final int energyStored = menu.getEnergy();
            final int energyConsumption = menu.getEnergyConsumption();

            Sprites.ENERGY_BAR.drawFillY(matrixStack, leftPos - Sprites.SIDEBAR_2.width + 4, topPos + ENERGY_TOP + 4, energyStored / (float) energyCapacity);

            if (isMouseOver(mouseX, mouseY, -Sprites.SIDEBAR_2.width + 4, ENERGY_TOP + 4, Sprites.ENERGY_BAR.width, Sprites.ENERGY_BAR.height)) {
                final List<? extends ITextProperties> tooltip = asList(
                        new TranslationTextComponent(Constants.TOOLTIP_ENERGY, withColor(energyStored + "/" + energyCapacity, TextFormatting.GREEN)),
                        new TranslationTextComponent(Constants.TOOLTIP_ENERGY_CONSUMPTION, withColor(String.valueOf(energyConsumption), TextFormatting.GREEN))
                );
                GuiUtils.drawHoveringText(matrixStack, tooltip, mouseX, mouseY, width, height, 200, font);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

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
        final InputMappings.Input input = InputMappings.getKey(keyCode, scanCode);
        if (this.minecraft.options.keyInventory.isActiveAndMatches(input)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void init() {
        super.init();
        terminalWidget.init();

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

        addButton(new ToggleImageButton(
                this, leftPos - Sprites.SIDEBAR_3.width + 4, topPos + CONTROLS_TOP + 4 + 14,
                12, 12,
                new TranslationTextComponent(Constants.TERMINAL_CAPTURE_INPUT_CAPTION),
                new TranslationTextComponent(Constants.TERMINAL_CAPTURE_INPUT_DESCRIPTION),
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
        });

        addButton(new ImageButton(
                this, leftPos - Sprites.SIDEBAR_3.width + 4, topPos + CONTROLS_TOP + 4 + 14 + 14,
                12, 12,
                new TranslationTextComponent(Constants.MACHINE_OPEN_INVENTORY_CAPTION),
                null,
                Sprites.INVENTORY_BUTTON_INACTIVE,
                Sprites.INVENTORY_BUTTON_ACTIVE
        ) {
            @Override
            public void onPress() {
                menu.switchToInventory();
            }
        });
    }

    @Override
    public void onClose() {
        super.onClose();
        terminalWidget.onClose();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final MatrixStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);

        Sprites.SIDEBAR_3.draw(matrixStack, leftPos - Sprites.SIDEBAR_2.width, topPos + CONTROLS_TOP);

        if (menu.getEnergyCapacity() > 0) {
            final int x = leftPos - Sprites.SIDEBAR_2.width;
            final int y = topPos + ENERGY_TOP;
            Sprites.SIDEBAR_2.draw(matrixStack, x, y);
            Sprites.ENERGY_BASE.draw(matrixStack, x + 4, y + 4);
        }

        terminalWidget.renderBackground(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(final MatrixStack matrixStack, final int mouseX, final int mouseY) {
        // This is required to prevent the labels from being rendered
    }
}
