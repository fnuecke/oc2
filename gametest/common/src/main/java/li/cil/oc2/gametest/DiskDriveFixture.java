/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static li.cil.oc2.gametest.TestSupport.DEVICE_POS;
import static li.cil.oc2.gametest.TestSupport.fakePlayer;

public final class DiskDriveFixture {
    private final GameTestHelper helper;
    private final BlockPos pos;

    // ------------------------------------------------------------- //

    public static DiskDriveFixture place(final GameTestHelper helper) {
        return place(helper, fakePlayer(helper), DEVICE_POS);
    }

    public static DiskDriveFixture place(final GameTestHelper helper, final Player player, final BlockPos pos) {
        TestSupport.place(helper, player, new ItemStack(Items.DISK_DRIVE.get()), pos);
        return new DiskDriveFixture(helper, pos);
    }

    public static DiskDriveFixture at(final GameTestHelper helper, final BlockPos pos) {
        return new DiskDriveFixture(helper, pos);
    }

    // ------------------------------------------------------------- //

    public BlockPos pos() {
        return pos;
    }

    public DiskDriveBlockEntity blockEntity() {
        return helper.getBlockEntity(pos);
    }

    public DiskDriveFixture insert(final ItemStack stack) {
        if (!blockEntity().insert(stack, null).isEmpty()) {
            throw new GameTestAssertException("could not insert " + stack + " into the disk drive");
        }
        return this;
    }

    public ItemStack floppy() {
        return blockEntity().getFloppy();
    }

    // ------------------------------------------------------------- //

    private DiskDriveFixture(final GameTestHelper helper, final BlockPos pos) {
        this.helper = helper;
        this.pos = pos;
    }
}
