/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
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

    // Prompts are unreliable to match, so the guest tells us when it is listening. The quotes
    // keep the echoed command line from matching the marker the shell prints.
    private static final String READY_PROBE = "echo OC2R\"\"EADY";
    private static final String READY_MARKER = "OC2READY";

    private static final String EVENT_PROBE =
            "lua -e \"local b=require('devices')"
                    + " local r=assert(b:find('robot'))"
                    + " assert(r:move('upward'))"
                    + " local e=b:waitEvent(10000, 'robotActionCompleted')"
                    + " local d=e and e.data"
                    + " local ok=d and d.actionId==1 and d.result=='SUCCESS'"
                    + " print('OC2R'..'ES', ok and 'PASS' or 'FAIL',"
                    + " tostring(e and e.type), tostring(d and d.actionId), tostring(d and d.result))\"";
    private static final String EVENT_MARKER = "OC2RES";

    private static final String LIBRARY_PROBE =
            "lua -e \"local ok=require('robot').move('downward', 5000)"
                    + " print('OC2M'..'OV', ok and 'PASS' or 'FAIL')\"";
    private static final String LIBRARY_MARKER = "OC2MOV";

    // ------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void aCompletedActionIsAnnouncedToTheGuest(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    robot.charge();
                    robot.install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()))
                            .install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_LARGE.get()))
                            .install(DeviceTypes.HARD_DRIVE, new ItemStack(Items.HARD_DRIVE_CUSTOM.get()));
                })
                .thenExecuteAfter(20, robot::start)
                .thenWaitUntil(() -> requireBooted(robot))
                .thenWaitUntil(() -> requireOutput(robot, "login:"))
                .thenExecute(() -> robot.type("root"))
                .thenWaitUntil(() -> requireShellPrompt(robot))
                .thenExecute(() -> robot.type(READY_PROBE))
                .thenWaitUntil(() -> requireOutput(robot, READY_MARKER))

                .thenExecute(() -> robot.type(EVENT_PROBE))
                .thenWaitUntil(() -> requireOutput(robot, EVENT_MARKER))
                .thenExecute(() -> requireVerdict(robot, EVENT_MARKER,
                        "the guest never saw a robotActionCompleted event for its move"))

                .thenExecute(() -> robot.type(LIBRARY_PROBE))
                .thenWaitUntil(() -> requireOutput(robot, LIBRARY_MARKER))
                .thenExecute(() -> requireVerdict(robot, LIBRARY_MARKER,
                        "robot.move() either failed or ignored the event and polled instead"))
                .thenSucceed();
    }

    // ------------------------------------------------------------- //

    private static void requireBooted(final RobotFixture robot) {
        robot.charge();
        robot.assertNoGuestPanic();

        final String text = robot.screen();
        if (!text.contains("Mounted root") || !text.contains("Run /sbin/init")) {
            throw new GameTestAssertException("guest has not handed off to userspace; screen:\n" + text);
        }
    }

    private static void requireShellPrompt(final RobotFixture robot) {
        robot.charge();
        robot.assertNoGuestPanic();

        for (final String line : robot.screen().split("\n")) {
            if (line.trim().equals("#")) {
                return;
            }
        }

        throw new GameTestAssertException("no shell prompt after logging in; screen:\n" + robot.screen());
    }

    private static void requireOutput(final RobotFixture robot, final String marker) {
        robot.charge();
        robot.assertNoGuestPanic();

        if (!robot.screen().contains(marker)) {
            throw new GameTestAssertException("guest never printed " + marker + "; screen:\n" + robot.screen());
        }
    }

    private static void requireVerdict(final RobotFixture robot, final String marker, final String what) {
        final String text = robot.screen();
        for (final String line : text.split("\n")) {
            if (line.startsWith(marker) && line.contains("PASS")) {
                return;
            }
        }

        throw new GameTestAssertException(what + "; screen:\n" + text);
    }

    // ------------------------------------------------------------- //

    private RobotActionEventTests() {
    }
}
