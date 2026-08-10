/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import dev.architectury.networking.NetworkManager;

public final class BusCableFacadeMessage extends AbstractMessage {
    private BlockPos pos;
    private ItemStack stack;

    ///////////////////////////////////////////////////////////////////

    public BusCableFacadeMessage(final BlockPos pos, final ItemStack stack) {
        this.pos = pos;
        this.stack = stack;
    }

    public BusCableFacadeMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        stack = buffer.readItem();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeItem(stack);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, BusCableBlockEntity.class,
            busCable -> busCable.setFacade(stack));
    }
}
