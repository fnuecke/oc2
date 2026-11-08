/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.layout.*;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.container.NetworkTunnelContainer;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.NetworkTunnelLinkMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class NetworkTunnelScreen extends UiContainerScreen<NetworkTunnelContainer> {
    private static final ResourceLocation LINK = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "network_tunnel/link");
    private static final int LINK_WIDTH = 20, LINK_HEIGHT = 46;
    private static final int SLOT_X = 1, HAND_SLOT_Y = 1, LINK_SLOT_Y = 27;

    private static final Component LINK_BUTTON_CAPTION = text("gui.{mod}.network_tunnel.link");

    // --------------------------------------------------------------------- //

    private final Inventory inventory;
    private final ImageButton linkButton;

    // --------------------------------------------------------------------- //

    public NetworkTunnelScreen(final NetworkTunnelContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title, Sprites.THEME);
        this.inventory = inventory;
        this.linkButton = new ImageButton(0, 0,
            Sprites.NETWORK_TUNNEL_LINK_BUTTON_INACTIVE.width, Sprites.NETWORK_TUNNEL_LINK_BUTTON_INACTIVE.height,
            Sprites.NETWORK_TUNNEL_LINK_BUTTON_INACTIVE,
            Sprites.NETWORK_TUNNEL_LINK_BUTTON_ACTIVE) {
            @Override
            public void onPress() {
                super.onPress();
                createTunnel();
            }
        }.withMessage(LINK_BUTTON_CAPTION);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        linkButton.active = getMenu().hasLinkSlotItem();

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Window createWindow() {
        final NodeStack link = NodeStack.vertical().background(LINK).size(LINK_WIDTH, LINK_HEIGHT);
        link.add(
            new SlotNode(menu.getHandItemSlot()).position(SLOT_X, HAND_SLOT_Y),
            new SlotNode(menu.getLinkItemSlot()).position(SLOT_X, LINK_SLOT_Y));

        return new Window(NodeStack.vertical(
            Ui.section(title, NodeStack.vertical(link, new WidgetNode(linkButton))
                .alignment(Align.START, Align.CENTER).width(Size.GROW)),
            Ui.section(playerInventoryTitle, Ui.playerInventory(menu, inventory))
        ));
    }

    // --------------------------------------------------------------------- //

    private void createTunnel() {
        final NetworkTunnelLinkMessage message = new NetworkTunnelLinkMessage(getMenu().containerId);
        Network.sendToServer(message);
    }
}
