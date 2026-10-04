/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.gametest.device.GuestTestDevices;
import li.cil.oc2.gametest.fixture.GuestTests;
import li.cil.oc2.gametest.fixture.Hardware;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotActionEventTests {
    private static final BlockPos ROBOT_POS = new BlockPos(20, WORK_Y, 2);

    private static final String BATCH = "oc2_robot_events";
    private static final String SUITE = "robot_events";

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void guestReceivesActionEvents(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final GuestTests tests = robot.guestTests();

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                robot.charge();
                Hardware.installLinuxWithExtraMemory(robot);
                robot.install(DeviceTypes.ROBOT_MODULE.get(), new ItemStack(GuestTestDevices.GUEST_TEST_PORT.get()));
            })
            .thenExecuteAfter(20, robot::start)
            .thenWaitUntil(() -> {
                robot.keepAlive();
                tests.requireReady();
            })
            .thenExecute(() -> tests.run(SUITE))
            .thenWaitUntil(() -> {
                robot.keepAlive();
                tests.requireSuccess();
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private RobotActionEventTests() {
    }
}
