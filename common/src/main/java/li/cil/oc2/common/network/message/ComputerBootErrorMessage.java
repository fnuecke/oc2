/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import javax.annotation.Nullable;
import java.util.Optional;

public final class ComputerBootErrorMessage extends AbstractMessage {
    private BlockPos pos;
    @Nullable
    private Component value;

    // --------------------------------------------------------------------- //

    public ComputerBootErrorMessage(final ComputerBlockEntity computer, @Nullable final Component value) {
        this.pos = computer.getBlockPos();
        this.value = value;
    }

    public ComputerBootErrorMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        value = ComponentSerialization.OPTIONAL_STREAM_CODEC.decode(buffer).orElse(null);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        ComponentSerialization.OPTIONAL_STREAM_CODEC.encode(buffer, Optional.ofNullable(value));
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, ComputerBlockEntity.class,
            computer -> computer.getVirtualMachineClientState().setBootErrorClient(value));
    }
}
