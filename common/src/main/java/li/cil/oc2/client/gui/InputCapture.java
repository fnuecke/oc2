/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class InputCapture {
    private static boolean isEnabled;

    // --------------------------------------------------------------------- //

    public static boolean isEnabled() {
        return isEnabled;
    }

    public static void toggle() {
        isEnabled = !isEnabled;
    }

    public static ToggleImageButton button() {
        final ToggleImageButton button = new ToggleImageButton(0, 0, 12, 12,
            Sprites.INPUT_BUTTON_BASE,
            Sprites.INPUT_BUTTON_PRESSED,
            Sprites.INPUT_BUTTON_ACTIVE
        ) {
            @Override
            public void onPress() {
                super.onPress();
                InputCapture.toggle();
            }

            @Override
            public boolean isToggled() {
                return InputCapture.isEnabled();
            }
        };
        button.withTooltip(
            Component.translatable(Constants.TERMINAL_CAPTURE_INPUT_CAPTION),
            Component.translatable(Constants.TERMINAL_CAPTURE_INPUT_DESCRIPTION));
        return button;
    }
}
