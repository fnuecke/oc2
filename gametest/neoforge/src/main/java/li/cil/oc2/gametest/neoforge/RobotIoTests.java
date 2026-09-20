/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.gametest.fixture.Hardware;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static li.cil.oc2.gametest.util.DeviceCalls.invokeIo;
import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotIoTests {
    private static final int SETTLE_TICKS = 60;
    private static final String MOVE_BATCH = "oc2_robot_io_move";
    private static final String CPM_BATCH = "oc2_robot_io_cpm";
    private static final int CPM_BOOT_TIMEOUT_TICKS = 20000;

    private static final int DETECT_CODE = 1;
    private static final int GET_ENERGY_STORED_CODE = 2;
    private static final int GET_ENERGY_CAPACITY_CODE = 3;
    private static final int GET_SELECTED_SLOT_CODE = 4;
    private static final int SET_SELECTED_SLOT_CODE = 5;
    private static final int GET_STACK_IN_SLOT_CODE = 6;
    private static final int GET_ITEM_NAME_CODE = 7;
    private static final int GET_ITEM_ID_CODE = 8;
    private static final int MOVE_CODE = 9;
    private static final int TURN_CODE = 10;
    private static final int GET_LAST_ACTION_ID_CODE = 11;
    private static final int GET_QUEUED_ACTION_COUNT_CODE = 12;
    private static final int GET_ACTION_RESULT_CODE = 13;

    private static final int FORWARD = 0;
    private static final int FRONT = 0;

    private static final int RESULT_UNKNOWN = 0;
    private static final int RESULT_INCOMPLETE = 1;
    private static final int RESULT_SUCCESS = 2;

    @GameTest(template = TEMPLATE)
    public static void detectReportsOccupancyAsCodes(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final IODevice device = robotDevice(robot);
                assertDetects(helper, robot, device, Blocks.AIR, 0);
                assertDetects(helper, robot, device, Blocks.WATER, 1);
                assertDetects(helper, robot, device, Blocks.STONE, 2);
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void energyReadsBackAsFourBytes(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                robot.charge();

                final IODevice device = robotDevice(robot);
                final byte[] capacity = invokeIo(device, GET_ENERGY_CAPACITY_CODE);
                final byte[] stored = invokeIo(device, GET_ENERGY_STORED_CODE);

                assertEquals(helper, "capacity is four bytes", 4, capacity.length);
                assertEquals(helper, "stored is four bytes", 4, stored.length);
                assertEquals(helper, "capacity reads back low byte first",
                    Config.robotEnergyStorage, readU32(capacity));
                assertEquals(helper, "a charged robot is full", readU32(capacity), readU32(stored));
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void selectedSlotRoundTripsAndClamps(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final IODevice device = robotDevice(robot);
                final int slots = robot.inventory().getSlots();

                assertEquals(helper, "setSelectedSlot reports what it applied",
                    3, invokeIo(device, SET_SELECTED_SLOT_CODE, 3)[0] & 0xFF);
                assertEquals(helper, "getSelectedSlot agrees",
                    3, invokeIo(device, GET_SELECTED_SLOT_CODE)[0] & 0xFF);
                assertEquals(helper, "a slot past the inventory clamps to the last one",
                    slots - 1, invokeIo(device, SET_SELECTED_SLOT_CODE, 0xFF)[0] & 0xFF);
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void stackInSlotReadsAsASlotRecord(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                robot.give(new ItemStack(net.minecraft.world.item.Items.REDSTONE, 7));

                final IODevice device = robotDevice(robot);
                final int slot = robot.entity().getSelectedSlot();
                final byte[] record = invokeIo(device, GET_STACK_IN_SLOT_CODE, slot);

                assertEquals(helper, "a slot record is four bytes", 4, record.length);
                assertEquals(helper, "the count is the third byte", 7, record[2] & 0xFF);

                final int itemId = readU16(record);
                assertEquals(helper, "the item is the registry id, low byte first",
                    BuiltInRegistries.ITEM.getId(net.minecraft.world.item.Items.REDSTONE), itemId);
                assertEquals(helper, "getItemId finds the same id by name",
                    itemId, readU16(invokeIo(device, GET_ITEM_ID_CODE, ascii("redstone"))));

                final String name = new String(
                    invokeIo(device, GET_ITEM_NAME_CODE, itemId & 0xFF, itemId >>> 8), US_ASCII);
                if (!"minecraft:redstone".equals(name)) {
                    throw new GameTestAssertException("getItemName reported [" + name + "]");
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void invalidArgumentsAreRejected(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final IODevice device = robotDevice(robot);
                assertThrows(helper, "a side that is not front, up or down",
                    () -> invokeIo(device, DETECT_CODE, 3));
                assertThrows(helper, "a direction that is not one of the four",
                    () -> invokeIo(device, MOVE_CODE, 4));
                assertThrows(helper, "a rotation that is neither left nor right",
                    () -> invokeIo(device, TURN_CODE, 2));
                assertThrows(helper, "a slot the robot does not have",
                    () -> invokeIo(device, GET_STACK_IN_SLOT_CODE, 0xFF));
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = MOVE_BATCH)
    public static void moveEnqueuesAndReportsItsResult(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockPos[] origin = new BlockPos[1];
        final int[] actionId = new int[1];

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                robot.charge();
                robot.putBlockInFront(Blocks.AIR);
                Hardware.installLinux(robot);
            })
            .thenExecuteAfter(20, robot::start)
            .thenWaitUntil(() -> {
                robot.charge();
                robot.assertRunState(VMRunState.RUNNING, "precondition");
            })
            .thenExecute(() -> {
                final IODevice device = robotDevice(robot);
                origin[0] = robot.blockPos();

                assertEquals(helper, "move should be enqueued",
                    1, invokeIo(device, MOVE_CODE, FORWARD)[0] & 0xFF);
                assertEquals(helper, "the action should be waiting",
                    1, invokeIo(device, GET_QUEUED_ACTION_COUNT_CODE)[0] & 0xFF);

                final byte[] id = invokeIo(device, GET_LAST_ACTION_ID_CODE);
                actionId[0] = (id[0] & 0xFF) | ((id[1] & 0xFF) << 8);
                assertTrue(helper, "an enqueued action has an id", actionId[0] != 0);
                assertEquals(helper, "the action has not finished yet", RESULT_INCOMPLETE,
                    invokeIo(device, GET_ACTION_RESULT_CODE, actionId[0] & 0xFF, actionId[0] >>> 8)[0] & 0xFF);
                assertEquals(helper, "an id the robot never handed out", RESULT_UNKNOWN,
                    invokeIo(device, GET_ACTION_RESULT_CODE, 0xFE, 0xFF)[0] & 0xFF);
            })
            .thenWaitUntil(() -> {
                robot.charge();
                assertEquals(helper, "the move should complete", RESULT_SUCCESS,
                    invokeIo(robotDevice(robot), GET_ACTION_RESULT_CODE,
                        actionId[0] & 0xFF, actionId[0] >>> 8)[0] & 0xFF);
            })
            .thenExecute(() -> assertTrue(helper, "a successful move leaves the robot somewhere else",
                !origin[0].equals(robot.blockPos())))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = CPM_BOOT_TIMEOUT_TICKS, batch = CPM_BATCH)
    public static void cpmRobotEnumeratesTheRobotDevice(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                robot.charge();
                robot.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_Z80.get()));
                robot.install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_Z80.getId()));
                robot.install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_SMALL.get()));
                robot.install(DeviceTypes.FLOPPY.get(), Items.FLOPPY.get().withData(BlockDeviceDataRegistry.CPM.getId()));
            })
            .thenExecuteAfter(20, robot::start)
            .thenWaitUntil(() -> {
                robot.charge();
                robot.assertScreenContains("A>", "CP/M should reach its prompt");
            })
            .thenExecute(() -> robot.type("DEVS\r"))
            .thenWaitUntil(() -> robot.assertScreenContains("ROBOT",
                "the robot should enumerate through the device API port"))
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void assertDetects(final GameTestHelper helper, final RobotFixture robot,
                                      final IODevice device, final Block block, final int expected) {
        final BlockPos target = robot.putBlockInFront(block);

        final int actual = invokeIo(device, DETECT_CODE, FRONT)[0] & 0xFF;
        if (actual != expected) {
            throw new GameTestAssertException("detect reported " + actual + " for "
                + helper.getLevel().getBlockState(target) + ", expected " + expected);
        }
    }

    private static int[] ascii(final String text) {
        final byte[] bytes = text.getBytes(US_ASCII);
        final int[] values = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            values[i] = bytes[i] & 0xFF;
        }
        return values;
    }

    private static int readU16(final byte[] bytes) {
        return (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8);
    }

    private static long readU32(final byte[] bytes) {
        long value = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            value = (value << 8) | (bytes[i] & 0xFF);
        }
        return value;
    }

    private static IODevice robotDevice(final RobotFixture robot) {
        for (final Device device : robot.devices()) {
            if (device instanceof final IODevice io && "ROBOT".equals(io.getIOName())) {
                return io;
            }
        }
        throw new GameTestAssertException("no device on the robot's bus provides the ROBOT mid-level API");
    }

    private RobotIoTests() {
    }
}
