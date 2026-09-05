/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.block.FlashDriveBlock;
import li.cil.oc2.common.bus.device.vm.block.FlashDriveDevice;
import li.cil.oc2.common.network.message.AbstractMessage;
import li.cil.oc2.common.network.message.FlashDriveFlashMemoryMessage;
import li.cil.oc2.common.tags.ItemTags;
import li.cil.oc2.common.util.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class FlashDriveBlockEntity extends AbstractRemovableMediaBlockEntity<FlashDriveDevice> {
    public FlashDriveBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.FLASH_DRIVE.get(), pos, state,
            ItemTags.DEVICES_FLASH_MEMORY,
            SoundEvents.FLASH_INSERT.get(),
            SoundEvents.FLASH_EJECT.get());
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Direction getEjectDirection() {
        return getBlockState().getValue(FlashDriveBlock.FACING);
    }

    @Override
    protected AbstractMessage createMediaMessage() {
        return new FlashDriveFlashMemoryMessage(this);
    }
}
