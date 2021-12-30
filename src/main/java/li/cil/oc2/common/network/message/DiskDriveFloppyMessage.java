package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.DiskDriveTileEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;

public final class DiskDriveFloppyMessage extends AbstractMessage {
    private BlockPos pos;
    private CompoundTag data;

    ///////////////////////////////////////////////////////////////////

    public DiskDriveFloppyMessage(final DiskDriveTileEntity diskDrive) {
        this.pos = diskDrive.getBlockPos();
        this.data = diskDrive.getFloppy().serializeNBT();
    }

    public DiskDriveFloppyMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        data = buffer.readNbt();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeNbt(data);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientTileEntityAt(pos, DiskDriveTileEntity.class,
                (diskDrive) -> diskDrive.setFloppyClient(ItemStack.of(data)));
    }
}
