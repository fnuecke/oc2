package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.DiskDriveTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public final class DiskDriveFloppyMessage extends AbstractMessage {
    private BlockPos pos;
    private CompoundNBT data;

    ///////////////////////////////////////////////////////////////////

    public DiskDriveFloppyMessage(final DiskDriveTileEntity diskDrive) {
        this.pos = diskDrive.getBlockPos();
        this.data = diskDrive.getFloppy().serializeNBT();
    }

    public DiskDriveFloppyMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        data = buffer.readNbt();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
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
