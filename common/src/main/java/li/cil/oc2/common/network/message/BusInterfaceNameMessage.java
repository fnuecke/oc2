/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;

public abstract class BusInterfaceNameMessage extends AbstractMessage {
    protected BlockPos pos;
    protected Direction side;
    protected String value;

    // ------------------------------------------------------------- //

    protected BusInterfaceNameMessage(final BusCableBlockEntity busCable, final Direction side, final String value) {
        this.pos = busCable.getBlockPos();
        this.side = side;
        this.value = value;
    }

    protected BusInterfaceNameMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // ------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        side = buffer.readEnum(Direction.class);
        value = buffer.readUtf(32);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(side);
        buffer.writeUtf(value, 32);
    }

    // ------------------------------------------------------------- //

    public static final class ToClient extends BusInterfaceNameMessage {
        public ToClient(final BusCableBlockEntity busCable, final Direction side, final String value) {
            super(busCable, side, value);
        }

        public ToClient(final RegistryFriendlyByteBuf buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkManager.PacketContext context) {
            MessageUtils.withClientBlockEntityAt(pos, BusCableBlockEntity.class,
                busCable -> busCable.setInterfaceName(side, value));
        }
    }

    public static final class ToServer extends BusInterfaceNameMessage {
        public ToServer(final BusCableBlockEntity busCable, final Direction side, final String value) {
            super(busCable, side, value);
        }

        public ToServer(final RegistryFriendlyByteBuf buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkManager.PacketContext context) {
            MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, BusCableBlockEntity.class,
                (player, busCable) -> busCable.setInterfaceName(side, value));
        }
    }
}
