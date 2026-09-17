/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.SerialInterfaceCardItem;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class SerialInterfaceCardConfigurationMessage extends AbstractMessage {
    private InteractionHand hand;
    private boolean[] sides;
    private int address;

    // --------------------------------------------------------------------- //

    public SerialInterfaceCardConfigurationMessage(final InteractionHand hand, final ItemStack stack) {
        this.hand = hand;
        this.sides = new boolean[Constants.BLOCK_FACE_COUNT];
        for (final Direction side : Direction.values()) {
            sides[side.get3DDataValue()] = SerialInterfaceCardItem.getSideConfiguration(stack, side);
        }
        this.address = SerialInterfaceCardItem.getAddress(stack);
    }

    public SerialInterfaceCardConfigurationMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        hand = buffer.readEnum(InteractionHand.class);
        sides = new boolean[Constants.BLOCK_FACE_COUNT];
        for (int i = 0; i < sides.length; i++) {
            sides[i] = buffer.readBoolean();
        }
        address = buffer.readVarInt();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(hand);
        for (final boolean side : sides) {
            buffer.writeBoolean(side);
        }
        buffer.writeVarInt(address);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        if (!(context.getPlayer() instanceof final ServerPlayer player)) {
            return;
        }

        final ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.SERIAL_INTERFACE_CARD.get())) {
            return;
        }

        for (final Direction side : Direction.values()) {
            SerialInterfaceCardItem.setSideConfiguration(itemStack, side, sides[side.get3DDataValue()]);
        }
        SerialInterfaceCardItem.setAddress(itemStack, address);
    }
}
