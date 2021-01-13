package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

public final class RobotRotationAction extends AbstractRobotAction {
    public static final DataParameter<Direction> TARGET_DIRECTION = EntityDataManager.createKey(RobotEntity.class, DataSerializers.DIRECTION);
    public static final float TARGET_EPSILON = 0.0001f;

    ///////////////////////////////////////////////////////////////////

    private static final float ROTATION_SPEED = 45f / Constants.TICK_SECONDS; // In degrees per second.

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

        robot.getDataManager().set(TARGET_DIRECTION, target);
    }

    @Override
    public boolean perform(final RobotEntity robot) {
        if (target == null) {
            return true;
        }

        rotateTowards(robot, target);

        return MathHelper.degreesDifferenceAbs(robot.rotationYaw, target.getHorizontalAngle()) < TARGET_EPSILON;
    }

    ///////////////////////////////////////////////////////////////////

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
    protected void deserialize(final CompoundNBT tag) {
        super.deserialize(tag);

        direction = NBTUtils.getEnum(tag, DIRECTION_TAG_NAME, RotationDirection.class);
        if (tag.contains(TARGET_TAG_NAME, NBTTagIds.TAG_INT)) {
            target = NBTUtils.getEnum(tag, TARGET_TAG_NAME, Direction.class);
        }
    }
}
