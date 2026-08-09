/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.widget.Sprite;

import static li.cil.oc2.client.gui.Textures.*;

public final class Sprites {
    public static final Sprite COMPUTER_CONTAINER = new Sprite(COMPUTER_CONTAINER_TEXTURE);
    public static final Sprite ROBOT_CONTAINER = new Sprite(ROBOT_CONTAINER_TEXTURE);
    public static final Sprite TERMINAL_SCREEN = new Sprite(TERMINAL_SCREEN_TEXTURE);
    public static final Sprite BUS_INTERFACE_SCREEN = new Sprite(BUS_INTERFACE_SCREEN_TEXTURE);
    public static final Sprite NETWORK_INTERFACE_CARD_SCREEN = new Sprite(NETWORK_INTERFACE_CARD_SCREEN_TEXTURE);
    public static final Sprite NETWORK_TUNNEL_SCREEN = new Sprite(NETWORK_TUNNEL_SCREEN_TEXTURE);

    public static final Sprite TERMINAL_FOCUSED = new Sprite(TERMINAL_FOCUSED_TEXTURE);
    public static final Sprite SLOT_SELECTION = new Sprite(SLOT_SELECTION_TEXTURE, 18, 18, 0, 0);
    public static final Sprite INFO_ICON = new Sprite(INFO_ICON_TEXTURE);
    public static final Sprite WARN_ICON = new Sprite(WARN_ICON_TEXTURE);

    public static final Sprite HOTBAR = new Sprite(HOTBAR_TEXTURE);
    public static final Sprite SIDEBAR_2 = new Sprite(SIDEBAR_2_TEXTURE);
    public static final Sprite SIDEBAR_3 = new Sprite(SIDEBAR_3_TEXTURE);

    public static final Sprite ENERGY_BASE = new Sprite(ENERGY_TEXTURE, 12, 26, 0, 0);
    public static final Sprite ENERGY_BAR = new Sprite(ENERGY_TEXTURE, 12, 26, 12, 0);

    public static final Sprite INPUT_BUTTON_ACTIVE = new Sprite(INPUT_BUTTON_TEXTURE, 12, 12, 1, 1);
    public static final Sprite INPUT_BUTTON_BASE = new Sprite(INPUT_BUTTON_TEXTURE, 12, 12, 15, 1);
    public static final Sprite INPUT_BUTTON_PRESSED = new Sprite(INPUT_BUTTON_TEXTURE, 12, 12, 29, 1);

    public static final Sprite POWER_BUTTON_ACTIVE = new Sprite(POWER_BUTTON_TEXTURE, 12, 12, 1, 1);
    public static final Sprite POWER_BUTTON_BASE = new Sprite(POWER_BUTTON_TEXTURE, 12, 12, 15, 1);
    public static final Sprite POWER_BUTTON_PRESSED = new Sprite(POWER_BUTTON_TEXTURE, 12, 12, 29, 1);

    public static final Sprite INVENTORY_BUTTON_INACTIVE = new Sprite(INVENTORY_BUTTON_TEXTURE, 12, 12, 1, 1);
    public static final Sprite INVENTORY_BUTTON_ACTIVE = new Sprite(INVENTORY_BUTTON_TEXTURE, 12, 12, 15, 1);

    public static final Sprite NETWORK_TUNNEL_LINK_BUTTON_INACTIVE = new Sprite(NETWORK_TUNNEL_LINK_BUTTON_TEXTURE, 80, 20, 0, 0);
    public static final Sprite NETWORK_TUNNEL_LINK_BUTTON_ACTIVE = new Sprite(NETWORK_TUNNEL_LINK_BUTTON_TEXTURE, 80, 20, 0, 20);

    public static final Sprite CONFIRM_PRESSED = new Sprite(CONFIRM_BUTTON_TEXTURE, 12, 12, 14, 1);
    public static final Sprite CONFIRM_BASE = new Sprite(CONFIRM_BUTTON_TEXTURE, 12, 12, 1, 1);
    public static final Sprite CANCEL_PRESSED = new Sprite(CANCEL_BUTTON_TEXTURE, 12, 12, 14, 1);
    public static final Sprite CANCEL_BASE = new Sprite(CANCEL_BUTTON_TEXTURE, 12, 12, 1, 1);
}
