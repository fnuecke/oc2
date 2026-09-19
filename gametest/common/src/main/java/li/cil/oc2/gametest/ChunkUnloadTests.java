/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.Hardware;
import li.cil.oc2.gametest.fixture.RobotFixture;
import li.cil.oc2.gametest.util.Chunks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static li.cil.oc2.gametest.fixture.MachineFixture.LOGIN_PROMPT;
import static li.cil.oc2.gametest.fixture.MachineFixture.SHELL_PROMPT;
import static li.cil.oc2.gametest.util.TestSupport.*;

public final class ChunkUnloadTests {
    private static final BlockPos COMPUTER_SCENE = new BlockPos(1024, WORK_Y, 0);
    private static final BlockPos WRITING_COMPUTER_SCENE = new BlockPos(1088, WORK_Y, 0);
    private static final BlockPos ROBOT_SCENE = new BlockPos(1152, WORK_Y, 0);

    private static final int UNLOAD_CYCLES = 3;
    private static final int WRITES_PER_CYCLE = 3;
    private static final Pattern WRITE = Pattern.compile("^W(\\d+) *$", Pattern.MULTILINE);
    private static final Pattern WRITER_DONE = Pattern.compile("WRITER-DONE \\d+");
    private static final Pattern VERIFY_RESULT = Pattern.compile("RESULT bad=(\\d+) err=(\\d+) END");

    // --------------------------------------------------------------------- //

    public static void runningMachineSurvivesChunkUnload(final GameTestHelper helper) {
        final ChunkPos chunkPos = Chunks.hold(helper, COMPUTER_SCENE);
        final ComputerFixture computer = ComputerFixture.at(helper, COMPUTER_SCENE);

        final long[] instructions = new long[1];
        final ComputerBlockEntity[] before = new ComputerBlockEntity[1];
        helper.startSequence()
            .thenWaitUntil(() -> Chunks.assertTicking(helper, COMPUTER_SCENE))
            .thenExecute(() -> placeMachine(helper, COMPUTER_SCENE))
            .thenExecuteAfter(20, () -> Hardware.installLinux(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> computer.assertRunState(VMRunState.RUNNING, "precondition"))
            .thenWaitUntil(() -> assertTrue(helper, "precondition: the guest ran", computer.guestInstructions() > 0))
            .thenExecute(() -> {
                instructions[0] = computer.guestInstructions();
                before[0] = computer.blockEntity();
                Chunks.release(helper, chunkPos);
            })
            .thenWaitUntil(() -> Chunks.assertUnloaded(helper, chunkPos))
            .thenExecute(() -> Chunks.hold(helper, chunkPos))
            .thenWaitUntil(() -> Chunks.assertTicking(helper, COMPUTER_SCENE))
            .thenWaitUntil(() -> computer.assertRunState(VMRunState.RUNNING, "after the chunk came back"))
            .thenExecute(() -> {
                computer.assertNoError();
                assertTrue(helper, "chunk came back without a reload; same block entity",
                    computer.blockEntity() != before[0]);
                if (computer.guestInstructions() < instructions[0]) {
                    throw new GameTestAssertException("machine started cold instead of resuming: retired "
                        + computer.guestInstructions() + " instructions, had " + instructions[0] + " before");
                }
            })
            .thenSucceed();
    }

    public static void diskSurvivesUnloadsUnderWrites(final GameTestHelper helper) {
        final ChunkPos chunkPos = Chunks.hold(helper, WRITING_COMPUTER_SCENE);
        final ComputerFixture computer = ComputerFixture.at(helper, WRITING_COMPUTER_SCENE);

        final int[] writes = new int[1];
        final GameTestSequence sequence = helper.startSequence()
            .thenWaitUntil(() -> Chunks.assertTicking(helper, WRITING_COMPUTER_SCENE))
            .thenExecute(() -> placeMachine(helper, WRITING_COMPUTER_SCENE))
            .thenExecuteAfter(20, () -> Hardware.installLinux(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> computer.assertScreenContains(LOGIN_PROMPT, "guest should reach its login prompt"))
            .thenExecute(computer::loginAsRoot)
            .thenWaitUntil(() -> computer.assertScreenContains(SHELL_PROMPT, "root should get a shell"))
            .thenExecute(() -> computer.type(script(
                "mkdir -p /root/t",
                "rm -f /root/stop",
                "(",
                "i=0",
                "while [ ! -e /root/stop ]; do",
                "  i=$((i+1))",
                "  f=/root/t/f$((i%16))",
                "  head -c 65536 /dev/urandom > $f",
                "  md5sum $f > $f.md5",
                "  echo W$i",
                "done",
                "echo WRITER-DONE $i",
                ") &")));

        for (int cycle = 0; cycle < UNLOAD_CYCLES; cycle++) {
            sequence
                .thenWaitUntil(() -> awaitWrites(computer, writes))
                .thenExecute(() -> Chunks.release(helper, chunkPos))
                .thenWaitUntil(() -> Chunks.assertUnloaded(helper, chunkPos))
                .thenExecute(() -> Chunks.hold(helper, chunkPos))
                .thenWaitUntil(() -> Chunks.assertTicking(helper, WRITING_COMPUTER_SCENE))
                .thenWaitUntil(() -> computer.assertRunState(VMRunState.RUNNING, "after the chunk came back"));
        }

        sequence
            .thenWaitUntil(() -> awaitWrites(computer, writes))
            .thenExecute(() -> computer.type("touch /root/stop\n"))
            .thenWaitUntil(() -> find(computer, WRITER_DONE))
            .thenExecute(() -> computer.type(script(
                "sync",
                "echo 3 > /proc/sys/vm/drop_caches",
                "bad=$(cat /root/t/*.md5 | md5sum -c 2>&1 | grep -vc ': OK$')",
                "err=$(dmesg | grep -ci -e 'fs error' -e 'i/o error' -e corrupt)",
                "echo RESULT bad=$bad err=$err END")))
            .thenWaitUntil(() -> find(computer, VERIFY_RESULT))
            .thenExecute(() -> {
                computer.assertNoError();
                computer.assertNoGuestPanic();
                final Matcher result = find(computer, VERIFY_RESULT);
                if (Integer.parseInt(result.group(1)) != 0 || Integer.parseInt(result.group(2)) != 0) {
                    throw new GameTestAssertException("disk diverged from the guest across " + UNLOAD_CYCLES
                        + " unloads (" + writes[0] + " writes): " + result.group() + "\n" + computer.screen());
                }
            })
            .thenSucceed();
    }

    public static void robotSurvivesChunkUnload(final GameTestHelper helper) {
        final ChunkPos chunkPos = Chunks.hold(helper, ROBOT_SCENE);

        final RobotFixture[] robot = new RobotFixture[1];
        final Robot[] reloaded = new Robot[1];
        final long[] instructions = new long[1];
        helper.startSequence()
            .thenWaitUntil(() -> Chunks.assertTicking(helper, ROBOT_SCENE))
            .thenExecute(() -> {
                Chunks.prepare(helper, ROBOT_SCENE);
                robot[0] = RobotFixture.place(helper, ROBOT_SCENE);
                Hardware.installLinux(robot[0]);
            })
            .thenExecuteAfter(20, () -> robot[0].start())
            .thenWaitUntil(() -> {
                robot[0].charge();
                robot[0].assertRunState(VMRunState.RUNNING, "precondition");
            })
            .thenWaitUntil(() -> {
                robot[0].charge();
                assertTrue(helper, "precondition: the guest ran", robot[0].guestInstructions() > 0);
            })
            .thenExecute(() -> {
                instructions[0] = robot[0].guestInstructions();
                Chunks.release(helper, chunkPos);
            })
            .thenWaitUntil(() -> Chunks.assertUnloaded(helper, chunkPos))
            .thenExecute(() -> Chunks.hold(helper, chunkPos))
            .thenWaitUntil(() -> Chunks.assertTicking(helper, ROBOT_SCENE))
            .thenWaitUntil(() -> reloaded[0] = reloadedRobot(helper, robot[0].entity()))
            .thenWaitUntil(() -> {
                final RobotFixture restored = RobotFixture.of(helper, reloaded[0]);
                restored.charge();
                restored.assertRunState(VMRunState.RUNNING, "after the chunk came back");
            })
            .thenExecute(() -> {
                final RobotFixture restored = RobotFixture.of(helper, reloaded[0]);
                restored.assertNoError();
                if (restored.guestInstructions() < instructions[0]) {
                    throw new GameTestAssertException("robot started cold instead of resuming: retired "
                        + restored.guestInstructions() + " instructions, had " + instructions[0] + " before");
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static Robot reloadedRobot(final GameTestHelper helper, final Robot unloaded) {
        final BlockPos pos = helper.absolutePos(ROBOT_SCENE);
        final List<Robot> robots = helper.getLevel()
            .getEntitiesOfClass(Robot.class, new AABB(pos).inflate(4));
        if (robots.size() != 1) {
            throw new GameTestAssertException("expected exactly one robot to come back, found " + robots.size());
        }
        final Robot robot = robots.getFirst();
        if (robot == unloaded) {
            throw new GameTestAssertException("chunk came back without a reload; same robot instance");
        }
        return robot;
    }

    private static void placeMachine(final GameTestHelper helper, final BlockPos pos) {
        Chunks.prepare(helper, pos);
        ComputerFixture.placePowered(helper, pos);
    }

    private static void awaitWrites(final ComputerFixture computer, final int[] writes) {
        final Matcher matcher = WRITE.matcher(computer.screen());
        int latest = -1;
        while (matcher.find()) {
            latest = Integer.parseInt(matcher.group(1));
        }
        if (latest < writes[0] + WRITES_PER_CYCLE) {
            throw new GameTestAssertException("guest has written " + latest + " time(s), waiting for "
                + (writes[0] + WRITES_PER_CYCLE));
        }
        writes[0] = latest;
    }

    private static Matcher find(final ComputerFixture computer, final Pattern pattern) {
        final String screen = computer.screen();
        final Matcher matcher = pattern.matcher(screen);
        if (!matcher.find()) {
            throw new GameTestAssertException("screen does not match [" + pattern + "]\n" + screen);
        }
        return matcher;
    }

    // --------------------------------------------------------------------- //

    private ChunkUnloadTests() {
    }
}
