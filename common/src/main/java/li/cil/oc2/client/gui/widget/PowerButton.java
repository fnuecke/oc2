/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.widget;

import li.cil.oc2.client.gui.Sprites;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.container.AbstractMachineContainer;
import li.cil.oc2.common.vm.PowerAction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

import static li.cil.oc2.common.util.TextFormatUtils.withFormat;

public final class PowerButton extends ToggleImageButton {
    private static final Component POWER_ON = Component.translatable(Constants.COMPUTER_SCREEN_POWER_ON);
    private static final Component SHUTDOWN = Component.translatable(Constants.COMPUTER_SCREEN_POWER_SHUTDOWN);
    private static final Component REBOOT = Component.translatable(Constants.COMPUTER_SCREEN_POWER_REBOOT);
    private static final Component POWER_OFF = Component.translatable(Constants.COMPUTER_SCREEN_POWER_POWER_OFF);

    // --------------------------------------------------------------------- //

    private final AbstractMachineContainer menu;

    // --------------------------------------------------------------------- //

    public PowerButton(final int x, final int y, final AbstractMachineContainer menu) {
        super(x, y, 12, 12,
            Sprites.POWER_BUTTON_BASE,
            Sprites.POWER_BUTTON_PRESSED,
            Sprites.POWER_BUTTON_ACTIVE);
        this.menu = menu;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void onPress() {
        super.onPress();
        menu.sendPowerActionToServer(getAction());
    }

    @Override
    public boolean isToggled() {
        return menu.getVirtualMachine().isRunning();
    }

    // --------------------------------------------------------------------- //

    @Override
    protected List<Component> getTooltipLines() {
        final PowerAction action = getAction();
        if (action == PowerAction.START) {
            return List.of(POWER_ON);
        }

        return List.of(
            highlightIf(SHUTDOWN, action == PowerAction.SHUTDOWN),
            highlightIf(REBOOT, action == PowerAction.REBOOT),
            highlightIf(POWER_OFF, action == PowerAction.POWER_OFF));
    }

    // --------------------------------------------------------------------- //

    private PowerAction getAction() {
        if (!isToggled()) {
            return PowerAction.START;
        }
        if (Screen.hasControlDown()) {
            return PowerAction.POWER_OFF;
        }
        if (Screen.hasShiftDown()) {
            return PowerAction.REBOOT;
        }
        return PowerAction.SHUTDOWN;
    }

    private static Component highlightIf(final Component text, final boolean value) {
        return withFormat(text, value ? ChatFormatting.WHITE : ChatFormatting.GRAY);
    }
}
