/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class FlashDriveFlashMemoryMessage extends AbstractMessage {
    private BlockPos pos;
    private ItemStack flashMemory;

    // --------------------------------------------------------------------- //

    public FlashDriveFlashMemoryMessage(final FlashDriveBlockEntity flashDrive) {
        this.pos = flashDrive.getBlockPos();
        this.flashMemory = flashDrive.getFlashMemory().copy();
    }

    public FlashDriveFlashMemoryMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        flashMemory = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, flashMemory);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, FlashDriveBlockEntity.class,
            flashDrive -> flashDrive.setFlashMemoryClient(flashMemory));
    }
}
