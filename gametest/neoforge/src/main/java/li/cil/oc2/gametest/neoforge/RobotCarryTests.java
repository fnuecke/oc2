/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.entity.robot.MovementDirection;
import li.cil.oc2.common.entity.robot.RobotActionResult;
import li.cil.oc2.common.entity.robot.RobotMovementAction;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.ROBOT_POS;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotCarryTests {
    private static final int SETTLE_TICKS = 10;
    private static final double TOLERANCE = 0.02;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public static void itemInPathDoesNotBlock(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final Vec3 itemPos = RobotMovementAction.getTargetPositionInBlock(robot.frontPos());
        helper.getLevel().addFreshEntity(new ItemEntity(helper.getLevel(), itemPos.x, itemPos.y, itemPos.z, new ItemStack(Items.DIRT), 0, 0, 0));

        move(helper, robot.entity(), MovementDirection.FORWARD, () -> result ->
            requireResult(result, RobotActionResult.SUCCESS));
    }

    @GameTest(template = TEMPLATE)
    public static void carriesAMobForward(final GameTestHelper helper) {
        final Robot robot = RobotFixture.place(helper, ROBOT_POS).entity();
        final Pig pig = spawnPigOnTop(helper, robot);

        move(helper, robot, MovementDirection.FORWARD, () -> {
            final Vec3 offset = pig.position().subtract(robot.position());
            return result -> {
                requireResult(result, RobotActionResult.SUCCESS);
                requireNear(pig.position().subtract(robot.position()), offset, "pig offset to robot");
            };
        });
    }

    @GameTest(template = TEMPLATE)
    public static void liftsAMob(final GameTestHelper helper) {
        final Robot robot = RobotFixture.place(helper, ROBOT_POS).entity();
        final Pig pig = spawnPigOnTop(helper, robot);

        move(helper, robot, MovementDirection.UPWARD, () -> result -> {
            requireResult(result, RobotActionResult.SUCCESS);
            requireNear(pig.getBoundingBox().minY, robot.getBoundingBox().maxY, "pig feet vs robot top");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void liftingAMobIntoACeilingFails(final GameTestHelper helper) {
        final Robot robot = RobotFixture.place(helper, ROBOT_POS).entity();
        final BlockPos ceiling = robot.blockPosition().above(2);
        helper.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
        final Pig pig = spawnPigOnTop(helper, robot);

        move(helper, robot, MovementDirection.UPWARD, () -> {
            final BlockPos start = robot.blockPosition();
            return result -> {
                requireResult(result, RobotActionResult.FAILURE);
                if (!robot.blockPosition().equals(start)) {
                    throw new GameTestAssertException("robot did not return to " + start + ", is at " + robot.blockPosition());
                }
                if (pig.getBoundingBox().maxY > ceiling.getY() + TOLERANCE) {
                    throw new GameTestAssertException("pig was pushed into the ceiling, maxY " + pig.getBoundingBox().maxY);
                }
            };
        });
    }

    @GameTest(template = TEMPLATE)
    public static void playerOnTopDoesNotBlockLifting(final GameTestHelper helper) {
        final Robot robot = RobotFixture.place(helper, ROBOT_POS).entity();
        placePlayerOnTop(helper, robot);

        move(helper, robot, MovementDirection.UPWARD, () -> result ->
            requireResult(result, RobotActionResult.SUCCESS));
    }

    @GameTest(template = TEMPLATE)
    public static void liftingAPlayerIntoACeilingFails(final GameTestHelper helper) {
        final Robot robot = RobotFixture.place(helper, ROBOT_POS).entity();
        helper.getLevel().setBlockAndUpdate(robot.blockPosition().above(2), Blocks.STONE.defaultBlockState());
        placePlayerOnTop(helper, robot);

        move(helper, robot, MovementDirection.UPWARD, () -> result ->
            requireResult(result, RobotActionResult.FAILURE));
    }

    // --------------------------------------------------------------------- //

    private interface ResultCheck {
        void check(RobotActionResult result);
    }

    private static void move(final GameTestHelper helper, final Robot robot, final MovementDirection direction, final Supplier<ResultCheck> start) {
        final AtomicReference<RobotMovementAction> action = new AtomicReference<>();
        final AtomicReference<ResultCheck> check = new AtomicReference<>();
        final AtomicReference<RobotActionResult> result = new AtomicReference<>(RobotActionResult.INCOMPLETE);
        helper.onEachTick(() -> {
            if (action.get() != null && result.get() == RobotActionResult.INCOMPLETE) {
                result.set(action.get().perform(robot));
            }
        });
        helper.startSequence()
            .thenIdle(SETTLE_TICKS)
            .thenExecute(() -> {
                check.set(start.get());
                final RobotMovementAction value = new RobotMovementAction(direction);
                value.initialize(robot);
                action.set(value);
            })
            .thenWaitUntil(() -> {
                if (result.get() == RobotActionResult.INCOMPLETE) {
                    throw new GameTestAssertException("movement still in progress");
                }
            })
            .thenExecute(() -> check.get().check(result.get()))
            .thenSucceed();
    }

    private static void placePlayerOnTop(final GameTestHelper helper, final Robot robot) {
        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(robot.getX(), robot.getBoundingBox().maxY, robot.getZ());
    }

    private static Pig spawnPigOnTop(final GameTestHelper helper, final Robot robot) {
        final Pig pig = EntityType.PIG.create(helper.getLevel());
        if (pig == null) {
            throw new GameTestAssertException("failed creating pig");
        }
        pig.setNoAi(true);
        pig.moveTo(robot.getX(), robot.getBoundingBox().maxY, robot.getZ(), 0, 0);
        helper.getLevel().addFreshEntity(pig);
        return pig;
    }

    private static void requireResult(final RobotActionResult actual, final RobotActionResult expected) {
        if (actual != expected) {
            throw new GameTestAssertException("expected " + expected + ", got " + actual);
        }
    }

    private static void requireNear(final Vec3 actual, final Vec3 expected, final String what) {
        if (actual.distanceTo(expected) > TOLERANCE) {
            throw new GameTestAssertException(what + ": expected " + expected + ", got " + actual);
        }
    }

    private static void requireNear(final double actual, final double expected, final String what) {
        if (Math.abs(actual - expected) > TOLERANCE) {
            throw new GameTestAssertException(what + ": expected " + expected + ", got " + actual);
        }
    }

    // --------------------------------------------------------------------- //

    private RobotCarryTests() {
    }
}
