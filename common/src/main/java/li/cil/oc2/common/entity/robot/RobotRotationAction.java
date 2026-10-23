/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.util.SerializationUtils;
import li.cil.oc2.common.util.TickUtils;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.time.Duration;

public final class RobotRotationAction extends AbstractRobotAction {
    public static final float TARGET_EPSILON = 0.0001f;

    // --------------------------------------------------------------------- //

    private static final float ROTATION_SPEED = 90f / TickUtils.toTicks(Duration.ofSeconds(1)); // degrees per tick

    private static final String DIRECTION_TAG_NAME = "direction";
    private static final String TARGET_TAG_NAME = "start";

    // --------------------------------------------------------------------- //

    @Nullable
    private RotationDirection direction;
    @Nullable
    private Direction target;

    // --------------------------------------------------------------------- //

    public RobotRotationAction(final RotationDirection direction) {
        super(RobotActions.ROTATION);
        this.direction = direction.resolve();
    }

    RobotRotationAction(final ValueInput input) {
        super(RobotActions.ROTATION, input);
    }

    // --------------------------------------------------------------------- //

    public static boolean isComplete(final Robot robot) {
        return Mth.degreesDifferenceAbs(robot.getYRot(), robot.getEntityData().get(Robot.TARGET_DIRECTION).toYRot()) <= TARGET_EPSILON;
    }

    public static void rotateTowards(final Robot robot, final Direction targetRotation) {
        robot.setYRot(Mth.approachDegrees(robot.getYRot(), targetRotation.toYRot(), ROTATION_SPEED));
    }

    // --------------------------------------------------------------------- //

    @Override
    public void initialize(final Robot robot) {
        if (target == null) {
            target = robot.getDirection();
            if (direction != null) {
                switch (direction) {
                    case LEFT -> target = target.getCounterClockWise();
                    case RIGHT -> target = target.getClockWise();
                }
            }
        }

        robot.getEntityData().set(Robot.TARGET_DIRECTION, target);
    }

    @Override
    public RobotActionResult perform(final Robot robot) {
        if (target == null) {
            throw new IllegalStateException();
        }

        rotateTowards(robot, target);

        if (Mth.degreesDifferenceAbs(robot.getYRot(), target.toYRot()) < TARGET_EPSILON) {
            return RobotActionResult.SUCCESS;
        }

        return RobotActionResult.INCOMPLETE;
    }

    @Override
    public void serialize(final ValueOutput output) {
        super.serialize(output);

        SerializationUtils.putEnum(output, DIRECTION_TAG_NAME, direction);
        SerializationUtils.putEnum(output, TARGET_TAG_NAME, target);
    }

    @Override
    public void deserialize(final ValueInput input) {
        super.deserialize(input);

        direction = SerializationUtils.getEnum(input, DIRECTION_TAG_NAME, RotationDirection.class);
        if (direction == null) direction = RotationDirection.LEFT;
        direction = direction.resolve();
        if (input.getInt(TARGET_TAG_NAME).isPresent()) {
            target = SerializationUtils.getEnum(input, TARGET_TAG_NAME, Direction.class);
        }
    }
}
