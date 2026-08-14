/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.DiskDriveFixture;
import li.cil.oc2.gametest.Drops;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class DiskDriveTests {
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
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

    // ------------------------------------------------------------- //

    private DiskDriveTests() {
    }
}
