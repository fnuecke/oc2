package li.cil.oc2.common.network.message;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.NetworkInterfaceCardItem;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class NetworkInterfaceCardConfigurationMessage extends AbstractMessage {
    private InteractionHand hand;
    private Direction side;
    private boolean value;

    ///////////////////////////////////////////////////////////////////

    public NetworkInterfaceCardConfigurationMessage(final InteractionHand hand, final Direction side, final boolean value) {
        this.hand = hand;
        this.side = side;
        this.value = value;
    }

    public NetworkInterfaceCardConfigurationMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        hand = buffer.readEnum(InteractionHand.class);
        side = buffer.readEnum(Direction.class);
        value = buffer.readBoolean();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeEnum(hand);
        buffer.writeEnum(side);
        buffer.writeBoolean(value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        final ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        final ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.NETWORK_INTERFACE_CARD.get())) {
            return;
        }

        NetworkInterfaceCardItem.setSideConfiguration(itemStack, side, value);
    }
}
