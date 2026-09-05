/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.block.DiskDriveBlock;
import li.cil.oc2.common.bus.device.vm.block.DiskDriveDevice;
import li.cil.oc2.common.network.message.AbstractMessage;
import li.cil.oc2.common.network.message.DiskDriveFloppyMessage;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.LocationSupplierUtils;
import li.cil.oc2.common.util.SoundEvents;
import li.cil.oc2.common.util.ThrottledSoundEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.time.Duration;

public final class DiskDriveBlockEntity extends AbstractRemovableMediaBlockEntity<DiskDriveDevice> {
    private final ThrottledSoundEmitter accessSoundEmitter;

    // --------------------------------------------------------------------- //

    public DiskDriveBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.DISK_DRIVE.get(), pos, state,
            ItemTags.DEVICES_FLOPPY,
            SoundEvents.FLOPPY_INSERT.get(),
            SoundEvents.FLOPPY_EJECT.get());

        this.accessSoundEmitter = new ThrottledSoundEmitter(LocationSupplierUtils.of(this),
            SoundEvents.FLOPPY_ACCESS.get()).withMinInterval(Duration.ofSeconds(1));
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Direction getEjectDirection() {
        return getBlockState().getValue(DiskDriveBlock.FACING);
    }

    @Override
    protected AbstractMessage createMediaMessage() {
        return new DiskDriveFloppyMessage(this);
    }

    @Override
    public void handleDataAccess() {
        accessSoundEmitter.play();
    }
}
