/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class OpenComputerInventoryMessage extends AbstractMessage {
    private BlockPos pos;

    ///////////////////////////////////////////////////////////////////

    public OpenComputerInventoryMessage(final ComputerBlockEntity computer) {
        this.pos = computer.getBlockPos();
    }

    public OpenComputerInventoryMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }
    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, ComputerBlockEntity.class,
            (player, computer) -> computer.openInventoryScreen(player));
    }
}
