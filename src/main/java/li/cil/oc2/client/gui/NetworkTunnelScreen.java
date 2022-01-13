package li.cil.oc2.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.client.gui.widget.ImageButton;
import li.cil.oc2.common.container.NetworkTunnelContainer;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.NetworkTunnelLinkMessage;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class NetworkTunnelScreen extends AbstractModContainerScreen<NetworkTunnelContainer> {
    private static final int LINK_BUTTON_LEFT = 48;
    private static final int LINK_BUTTON_TOP = 78;

    private static final Component LINK_BUTTON_CAPTION = text("gui.{mod}.network_tunnel.link");

    private ImageButton linkButton;

    ///////////////////////////////////////////////////////////////////

    public NetworkTunnelScreen(final NetworkTunnelContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title);

        imageWidth = Sprites.NETWORK_TUNNEL_SCREEN.width;
        imageHeight = Sprites.NETWORK_TUNNEL_SCREEN.height;
        inventoryLabelY = imageHeight - 94;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void render(final PoseStack stack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(stack);

        linkButton.active = getMenu().hasLinkSlotItem();

        super.render(stack, mouseX, mouseY, partialTicks);
        renderTooltip(stack, mouseX, mouseY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();

        linkButton = addRenderableWidget(new ImageButton(
            leftPos + LINK_BUTTON_LEFT, topPos + LINK_BUTTON_TOP,
            Sprites.NETWORK_TUNNEL_LINK_BUTTON_INACTIVE.width, Sprites.NETWORK_TUNNEL_LINK_BUTTON_INACTIVE.height,
            Sprites.NETWORK_TUNNEL_LINK_BUTTON_INACTIVE,
            Sprites.NETWORK_TUNNEL_LINK_BUTTON_ACTIVE) {
            @Override
            public void onPress() {
                super.onPress();
                createTunnel();
            }
        }).withMessage(LINK_BUTTON_CAPTION);
    }

    @Override
    protected void renderBg(final PoseStack stack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        Sprites.NETWORK_TUNNEL_SCREEN.draw(stack, leftPos, topPos);
    }

    ///////////////////////////////////////////////////////////////////

    private void createTunnel() {
        final NetworkTunnelLinkMessage message = new NetworkTunnelLinkMessage(getMenu().containerId);
        Network.INSTANCE.sendToServer(message);
    }
}
