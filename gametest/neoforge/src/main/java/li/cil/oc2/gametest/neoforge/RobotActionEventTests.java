/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.GuestTests;
import li.cil.oc2.gametest.RobotFixture;
import li.cil.oc2.gametest.device.GuestTestDevices;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotActionEventTests {
    private static final BlockPos ROBOT_POS = new BlockPos(20, WORK_Y, 2);
    private static final int BOOT_TIMEOUT_TICKS = 150000;

    private static final String BATCH = "oc2_robot_events";
    private static final String SUITE = "robot_events";

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void aCompletedActionIsAnnouncedToTheGuest(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final GuestTests tests = robot.guestTests();

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                robot.charge();
                robot.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()))
                    .install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_LARGE.get()))
                    .install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_LARGE.get()))
                    .install(DeviceTypes.HARD_DRIVE, new ItemStack(Items.HARD_DRIVE_CUSTOM.get()))
                    .install(DeviceTypes.ROBOT_MODULE, new ItemStack(GuestTestDevices.GUEST_TEST_PORT.get()));
            })
            .thenExecuteAfter(20, robot::start)
            .thenWaitUntil(() -> {
                keepAlive(robot);
                tests.requireReady();
            })
            .thenExecute(() -> tests.run(SUITE))
            .thenWaitUntil(() -> {
                keepAlive(robot);
                tests.requireSuccess();
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void keepAlive(final RobotFixture robot) {
        robot.charge();
        robot.assertNoGuestPanic();
    }

    // --------------------------------------------------------------------- //

    private RobotActionEventTests() {
    }
}
