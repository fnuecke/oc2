/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.client.gui.layout.*;
import li.cil.oc2.client.gui.node.SideConfigurationWidget;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static li.cil.oc2.common.util.TranslationUtils.text;

public abstract class AbstractSideConfigurationScreen extends UiScreen {
    private static final Component HELP_TEXT = text("gui.{mod}.sided_device.help");
    private static final Component HELP_LABEL = Component.literal("?");
    private static final int HELP_MARGIN = 2;

    // --------------------------------------------------------------------- //

    protected final Player player;
    protected final InteractionHand hand;

    private final Item item;

    // --------------------------------------------------------------------- //

    protected AbstractSideConfigurationScreen(final Player player, final InteractionHand hand, final Item item) {
        super(item.getDescription(), Sprites.THEME);
        this.player = player;
        this.hand = hand;
        this.item = item;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void tick() {
        super.tick();

        final ItemStack heldItem = player.getItemInHand(hand);
        if (!heldItem.is(item)) {
            onClose();
        }
    }

    // --------------------------------------------------------------------- //

    protected abstract boolean getConfiguration(final Direction side);

    protected abstract void sendConfiguration(final Direction side, final boolean value);

    protected Node[] createControls() {
        return new Node[0];
    }

    @Override
    protected Window createWindow() {
        final NodeStack help = NodeStack.horizontal(new LabelNode(HELP_LABEL).tooltip(() -> List.of(HELP_TEXT)))
            .padding(HELP_MARGIN).alignment(Align.END, Align.START).overlay();
        final NodeStack blockView = Ui.insetBlack(new WidgetNode(new SideConfigurationWidget(this::getConfiguration, this::sendConfiguration)))
            .add(help);
        final NodeStack content = NodeStack.vertical(blockView).alignment(Align.START, Align.CENTER);
        content.add(createControls());
        return new Window(content);
    }
}
