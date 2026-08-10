/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import dev.architectury.networking.NetworkManager;

public final class DiskDriveFloppyMessage extends AbstractMessage {
    private BlockPos pos;
    private ItemStack floppy;

    ///////////////////////////////////////////////////////////////////

    public DiskDriveFloppyMessage(final DiskDriveBlockEntity diskDrive) {
        this.pos = diskDrive.getBlockPos();
        this.floppy = diskDrive.getFloppy().copy();
    }

    public DiskDriveFloppyMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        floppy = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, floppy);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, DiskDriveBlockEntity.class,
            diskDrive -> diskDrive.setFloppyClient(floppy));
    }
}
