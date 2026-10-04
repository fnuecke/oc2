/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import com.google.gson.JsonObject;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.common.bus.device.rpc.RPCTypeAdapters;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static li.cil.oc2.gametest.util.DeviceCalls.i16;
import static li.cil.oc2.gametest.util.DeviceCalls.invokeIo;
import static li.cil.oc2.gametest.util.DeviceCalls.invokeRpc;
import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotLocalizationTests {
    private static final int SETTLE_TICKS = 20;
    private static final BlockPos SECOND_ROBOT_POS = ROBOT_POS.offset(4, 0, 0);

    private static final int GET_FACING_CODE = 17;
    private static final int GET_POSITION_CODE = 18;
    private static final int CALIBRATE_POSITION_CODE = 19;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public static void placedRobotStartsAtItsOrigin(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> assertPosition(helper, robot, 0, 0, 0))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void facingNamesEachCompassDirection(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                assertFacing(helper, robot, Direction.NORTH, "north", 0);
                assertFacing(helper, robot, Direction.EAST, "east", 1);
                assertFacing(helper, robot, Direction.SOUTH, "south", 2);
                assertFacing(helper, robot, Direction.WEST, "west", 3);
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void positionChangesHalfwayThroughAMove(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final Vec3 start = robot.entity().position();
                robot.entity().setPos(start.subtract(0, 0.25, 0));
                assertPosition(helper, robot, 0, 0, 0);
                robot.entity().setPos(start.subtract(0, 0.75, 0));
                assertPosition(helper, robot, 0, -1, 0);
                robot.entity().setPos(start.add(0, 0.25, 0));
                assertPosition(helper, robot, 0, 0, 0);
                robot.entity().setPos(start.add(0, 0.75, 0));
                assertPosition(helper, robot, 0, 1, 0);
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void changingDimensionResetsTheOrigin(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                moveBy(robot, 1, 1, 1);
                reload(helper, robot, saved ->
                    saved.getCompound("origin").putString("dimension", Level.NETHER.location().toString()));
            })
            .thenExecuteAfter(SETTLE_TICKS, () -> assertPosition(helper, RobotFixture.find(helper, ROBOT_POS, 3), 0, 0, 0))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void corruptOriginResetsTheOrigin(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                moveBy(robot, 1, 1, 1);
                reload(helper, robot, saved -> saved.getCompound("origin").putString("pos", "not a position"));
            })
            .thenExecuteAfter(SETTLE_TICKS, () -> assertPosition(helper, RobotFixture.find(helper, ROBOT_POS, 3), 0, 0, 0))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void positionFollowsTeleports(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                moveBy(robot, -2, 3, 1);
                assertPosition(helper, robot, -2, 3, 1);
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void positionFollowsPistons(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final BlockPos piston = robot.blockPos().west();
                helper.getLevel().setBlockAndUpdate(piston,
                    Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
                helper.getLevel().setBlockAndUpdate(piston.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            })
            .thenExecuteAfter(SETTLE_TICKS, () -> assertPosition(helper, robot, 1, 0, 0))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void calibratingZeroesThePosition(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                moveBy(robot, 2, 0, 0);
                invokeRpc(robot.devices(), "calibratePosition");
                assertPosition(helper, robot, 0, 0, 0);
                moveBy(robot, 0, 0, -1);
                assertPosition(helper, robot, 0, 0, -1);
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void originSurvivesSaving(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                moveBy(robot, 1, 1, 1);
                reload(helper, robot, saved -> {
                });
            })
            .thenExecuteAfter(SETTLE_TICKS, () -> assertPosition(helper, RobotFixture.find(helper, ROBOT_POS, 3), 1, 1, 1))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void pickingUpTheRobotClearsTheOrigin(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                moveBy(robot, 0, 1, 0);

                final ItemStack stack = new ItemStack(Items.ROBOT.get());
                robot.entity().exportToItemStack(stack);
                robot.entity().discard();
                useOn(helper, fakePlayer(helper), stack, SECOND_ROBOT_POS, Direction.UP);
            })
            .thenExecuteAfter(SETTLE_TICKS, () -> assertPosition(helper, RobotFixture.find(helper, SECOND_ROBOT_POS, 3), 0, 0, 0))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void ioReportsSignedPosition(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(SETTLE_TICKS, () -> {
                final IODevice device = robot.robotDevice();

                moveBy(robot, -3, 2, -1);
                final byte[] position = invokeIo(device, GET_POSITION_CODE);
                assertEquals(helper, "the position is six bytes", 6, position.length);
                assertEquals(helper, "x is signed, low byte first", -3, i16(position, 0));
                assertEquals(helper, "y", 2, i16(position, 2));
                assertEquals(helper, "z", -1, i16(position, 4));

                invokeIo(device, CALIBRATE_POSITION_CODE);
                assertPosition(helper, robot, 0, 0, 0);
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void moveBy(final RobotFixture robot, final int dx, final int dy, final int dz) {
        final Vec3 position = robot.entity().position();
        robot.entity().teleportTo(position.x + dx, position.y + dy, position.z + dz);
    }

    private static void reload(final GameTestHelper helper, final RobotFixture robot, final Consumer<CompoundTag> edit) {
        final CompoundTag saved = robot.entity().saveWithoutId(new CompoundTag());
        edit.accept(saved);
        robot.entity().discard();
        final Robot loaded = Entities.ROBOT.get().create(helper.getLevel());
        loaded.load(saved);
        helper.getLevel().addFreshEntity(loaded);
    }

    private static void assertFacing(final GameTestHelper helper, final RobotFixture robot,
                                     final Direction direction, final String name, final int code) {
        robot.entity().setYRot(direction.toYRot());
        final Object facing = invokeRpc(robot.devices(), "getFacing");
        if (!name.equals(facing)) {
            throw failure(helper, "getFacing() reported " + facing + " facing " + name);
        }
        assertEquals(helper, "the facing byte for " + name, code, invokeIo(robot.robotDevice(), GET_FACING_CODE)[0] & 0xFF);
    }

    private static void assertPosition(final GameTestHelper helper, final RobotFixture robot, final int x, final int y, final int z) {
        final Object result = invokeRpc(robot.devices(), "getPosition");
        final JsonObject json = RPCTypeAdapters.beginBuildGson().create().toJsonTree(result).getAsJsonObject();
        final int actualX = json.get("x").getAsInt();
        final int actualY = json.get("y").getAsInt();
        final int actualZ = json.get("z").getAsInt();
        if (actualX != x || actualY != y || actualZ != z) {
            throw failure(helper, "getPosition() reported " + json + ", expected " + x + ", " + y + ", " + z);
        }
    }

    // --------------------------------------------------------------------- //

    private RobotLocalizationTests() {
    }
}
