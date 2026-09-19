/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public abstract class TerminalConfigurationMessage extends AbstractMessage {
    protected BlockPos pos;
    protected int address;
    protected int baudRate;

    // --------------------------------------------------------------------- //

    protected TerminalConfigurationMessage(final TerminalBlockEntity terminal, final int address, final int baudRate) {
        this.pos = terminal.getBlockPos();
        this.address = address;
        this.baudRate = baudRate;
    }

    protected TerminalConfigurationMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        address = buffer.readVarInt();
        baudRate = buffer.readVarInt();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(address);
        buffer.writeVarInt(baudRate);
    }

    // --------------------------------------------------------------------- //

    public static final class ToClient extends TerminalConfigurationMessage {
        public ToClient(final TerminalBlockEntity terminal, final int address, final int baudRate) {
            super(terminal, address, baudRate);
        }

        public ToClient(final RegistryFriendlyByteBuf buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkManager.PacketContext context) {
            MessageUtils.withClientBlockEntityAt(pos, TerminalBlockEntity.class,
                terminal -> terminal.setConfigurationClient(address, baudRate));
        }
    }

    public static final class ToServer extends TerminalConfigurationMessage {
        public ToServer(final TerminalBlockEntity terminal, final int address, final int baudRate) {
            super(terminal, address, baudRate);
        }

        public ToServer(final RegistryFriendlyByteBuf buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkManager.PacketContext context) {
            MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, TerminalBlockEntity.class,
                (player, terminal) -> terminal.setConfiguration(address, baudRate));
        }
    }
}
