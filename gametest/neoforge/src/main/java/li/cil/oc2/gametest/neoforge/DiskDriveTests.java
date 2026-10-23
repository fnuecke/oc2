/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.DiskDriveFixture;
import li.cil.oc2.gametest.util.Drops;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.*;

@ForEachTest(groups = "disk_drive")
public final class DiskDriveTests {
    @GameTest(template = TEMPLATE_ID, timeoutTicks = 200)
    @TestHolder
    public static void breakingDiskDriveDropsItsFloppy(final GameTestHelper helper) {
        final DiskDriveFixture drive = DiskDriveFixture.place(helper);

        helper.startSequence()
            .thenExecuteAfter(20, () -> drive.insert(new ItemStack(Items.FLOPPY.get())))
            .thenExecuteAfter(10, () -> breakBlockAndDrop(helper, drive.pos()))
            .thenExecuteAfter(10, () -> {
                Drops.assertDropped(helper, Items.FLOPPY.get(), "the floppy");
                Drops.assertDropped(helper, Items.DISK_DRIVE.get(), "the drive itself");
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private DiskDriveTests() {
    }
}
