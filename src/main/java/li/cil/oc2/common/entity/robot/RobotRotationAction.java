package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

public final class RobotRotationAction extends AbstractRobotAction {
    public static final float TARGET_EPSILON = 0.0001f;

    ///////////////////////////////////////////////////////////////////

    private static final float ROTATION_SPEED = 90f / Constants.TICK_SECONDS; // In degrees per second.

    private static final String DIRECTION_TAG_NAME = "direction";
    private static final String TARGET_TAG_NAME = "start";

    ///////////////////////////////////////////////////////////////////

    private RotationDirection direction;
    @Nullable private Direction target;

    ///////////////////////////////////////////////////////////////////

    public RobotRotationAction(final RotationDirection direction) {
        super(RobotActions.ROTATION);
        this.direction = direction;
    }

    RobotRotationAction(final CompoundNBT tag) {
        super(RobotActions.ROTATION);
        deserialize(tag);
    }

    ///////////////////////////////////////////////////////////////////

    public static void rotateTowards(final RobotEntity robot, final Direction targetRotation) {
        robot.rotationYaw = MathHelper.approachDegrees(robot.rotationYaw, targetRotation.getHorizontalAngle(), ROTATION_SPEED);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void initialize(final RobotEntity robot) {
        if (target == null) {
            target = robot.getHorizontalFacing();
            switch (direction) {
                case LEFT:
                    target = target.rotateYCCW();
                    break;
                case RIGHT:
                    target = target.rotateY();
                    break;
            }
        }

        robot.getDataManager().set(RobotEntity.TARGET_DIRECTION, target);
    }

    @Override
    public RobotActionResult perform(final RobotEntity robot) {
        if (target == null) {
            throw new IllegalStateException();
        }

        rotateTowards(robot, target);

        if (MathHelper.degreesDifferenceAbs(robot.rotationYaw, target.getHorizontalAngle()) < TARGET_EPSILON) {
            return RobotActionResult.SUCCESS;
        }

        return RobotActionResult.INCOMPLETE;
    }

    @Override
    public CompoundNBT serialize() {
        final CompoundNBT tag = super.serialize();

        NBTUtils.putEnum(tag, DIRECTION_TAG_NAME, direction);
        if (target != null) {
            NBTUtils.putEnum(tag, TARGET_TAG_NAME, target);
        }

        return tag;
    }

    @Override
    public void deserialize(final CompoundNBT tag) {
        super.deserialize(tag);

        direction = NBTUtils.getEnum(tag, DIRECTION_TAG_NAME, RotationDirection.class);
        if (tag.contains(TARGET_TAG_NAME, NBTTagIds.TAG_INT)) {
            target = NBTUtils.getEnum(tag, TARGET_TAG_NAME, Direction.class);
        }
    }
}
