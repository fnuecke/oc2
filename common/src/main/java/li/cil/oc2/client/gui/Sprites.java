/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.layout.Theme;
import li.cil.oc2.client.gui.widget.NineSliceSprite;
import li.cil.oc2.client.gui.widget.Sprite;

import static li.cil.oc2.client.gui.Textures.*;

public final class Sprites {
    private static final int BORDER = 8;

    public static final Theme THEME = Theme.of(API.MOD_ID, 0xFF404040, 0xFFAAAAAA);

    public static final NineSliceSprite GENERIC_BUTTON_BASE = new NineSliceSprite(GENERIC_BUTTON_TEXTURE, 80, 20, 0, 0, BORDER);
    public static final NineSliceSprite GENERIC_BUTTON_PRESSED = new NineSliceSprite(GENERIC_BUTTON_TEXTURE, 80, 20, 0, 20, BORDER);

    public static final Sprite INPUT_BUTTON_ACTIVE = new Sprite(INPUT_BUTTON_TEXTURE, 12, 12, 1, 1);
    public static final Sprite INPUT_BUTTON_BASE = new Sprite(INPUT_BUTTON_TEXTURE, 12, 12, 15, 1);
    public static final Sprite INPUT_BUTTON_PRESSED = new Sprite(INPUT_BUTTON_TEXTURE, 12, 12, 29, 1);

    public static final Sprite POWER_BUTTON_ACTIVE = new Sprite(POWER_BUTTON_TEXTURE, 12, 12, 1, 1);
    public static final Sprite POWER_BUTTON_BASE = new Sprite(POWER_BUTTON_TEXTURE, 12, 12, 15, 1);
    public static final Sprite POWER_BUTTON_PRESSED = new Sprite(POWER_BUTTON_TEXTURE, 12, 12, 29, 1);

    public static final Sprite INVENTORY_BUTTON_INACTIVE = new Sprite(INVENTORY_BUTTON_TEXTURE, 12, 12, 1, 1);
    public static final Sprite INVENTORY_BUTTON_ACTIVE = new Sprite(INVENTORY_BUTTON_TEXTURE, 12, 12, 15, 1);

    public static final Sprite CONFIG_BUTTON_INACTIVE = new Sprite(CONFIG_BUTTON_TEXTURE, 12, 12, 1, 1);
    public static final Sprite CONFIG_BUTTON_ACTIVE = new Sprite(CONFIG_BUTTON_TEXTURE, 12, 12, 15, 1);

    public static final Sprite NETWORK_TUNNEL_LINK_BUTTON_INACTIVE = new Sprite(NETWORK_TUNNEL_LINK_BUTTON_TEXTURE, 80, 20, 0, 0);
    public static final Sprite NETWORK_TUNNEL_LINK_BUTTON_ACTIVE = new Sprite(NETWORK_TUNNEL_LINK_BUTTON_TEXTURE, 80, 20, 0, 20);

    public static final Sprite CONFIRM_PRESSED = new Sprite(CONFIRM_BUTTON_TEXTURE, 12, 12, 14, 1);
    public static final Sprite CONFIRM_BASE = new Sprite(CONFIRM_BUTTON_TEXTURE, 12, 12, 1, 1);
    public static final Sprite CANCEL_PRESSED = new Sprite(CANCEL_BUTTON_TEXTURE, 12, 12, 14, 1);
    public static final Sprite CANCEL_BASE = new Sprite(CANCEL_BUTTON_TEXTURE, 12, 12, 1, 1);
}
