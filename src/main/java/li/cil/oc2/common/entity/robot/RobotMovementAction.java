package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;

public final class RobotMovementAction extends AbstractRobotAction {
    public static final double TARGET_EPSILON = 0.0001;

    ///////////////////////////////////////////////////////////////////

    private static final float MOVEMENT_SPEED = 1f / Constants.TICK_SECONDS; // In blocks per second.

    private static final String DIRECTION_TAG_NAME = "direction";
    private static final String START_TAG_NAME = "start";
    private static final String TARGET_TAG_NAME = "start";

    ///////////////////////////////////////////////////////////////////

    private MovementDirection direction;
    @Nullable private BlockPos start;
    @Nullable private Vector3d target;

    ///////////////////////////////////////////////////////////////////

    public RobotMovementAction(final MovementDirection direction) {
        super(RobotActions.MOVEMENT);
        this.direction = direction;
    }

    RobotMovementAction(final CompoundNBT tag) {
        super(RobotActions.MOVEMENT);
        deserialize(tag);
    }

    ///////////////////////////////////////////////////////////////////

    public static Vector3d getTargetPositionInBlock(final BlockPos position) {
        return Vector3d.copyCenteredHorizontally(position).add(0, 0.5f * (1 - Entities.ROBOT.get().getHeight()), 0);
    }

    public static void moveTowards(final RobotEntity robot, final Vector3d targetPosition) {
        Vector3d delta = targetPosition.subtract(robot.getPositionVec());
        if (delta.lengthSquared() > MOVEMENT_SPEED * MOVEMENT_SPEED) {
            delta = delta.normalize().scale(MOVEMENT_SPEED);
        }

        robot.move(MoverType.SELF, delta);
    }

    @Override
    public void initialize(final RobotEntity robot) {
        if (target == null) {
            start = robot.getPosition();
            BlockPos targetPosition = start;
            switch (direction) {
                case UP:
                    targetPosition = targetPosition.offset(Direction.UP);
                    break;
                case DOWN:
                    targetPosition = targetPosition.offset(Direction.DOWN);
                    break;
                case FORWARD:
                    targetPosition = targetPosition.offset(robot.getHorizontalFacing());
                    break;
                case BACKWARD:
                    targetPosition = targetPosition.offset(robot.getHorizontalFacing().getOpposite());
                    break;
            }

            target = getTargetPositionInBlock(targetPosition);
        }

        robot.getDataManager().set(RobotEntity.TARGET_POSITION, new BlockPos(target));
    }

    @Override
    public boolean perform(final RobotEntity robot) {
        if (target == null) {
            return true;
        }

        moveTowards(robot, target);

        final boolean didCollide = robot.collidedHorizontally || robot.collidedVertically;
        if (didCollide && !robot.getEntityWorld().isRemote()) {
            if (start != null) {
                target = getTargetPositionInBlock(start);
                robot.getDataManager().set(RobotEntity.TARGET_POSITION, start);

                start = null;
            } else {
                // todo if it's a block, try to break it. or just drop ourselves?
            }
        }

        return robot.getPositionVec().squareDistanceTo(target) < TARGET_EPSILON;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public CompoundNBT serialize() {
        final CompoundNBT tag = super.serialize();

        NBTUtils.putEnum(tag, DIRECTION_TAG_NAME, direction);
        if (start != null) {
            NBTUtils.putBlockPos(tag, START_TAG_NAME, start);
        }
        if (target != null) {
            NBTUtils.putVector3d(tag, TARGET_TAG_NAME, target);
        }

        return tag;
    }

    @Override
    protected void deserialize(final CompoundNBT tag) {
        super.deserialize(tag);

        direction = NBTUtils.getEnum(tag, DIRECTION_TAG_NAME, MovementDirection.class);
        if (tag.contains(START_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            start = NBTUtils.getBlockPos(tag, START_TAG_NAME);
        }
        if (tag.contains(TARGET_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            target = NBTUtils.getVector3d(tag, TARGET_TAG_NAME);
        }
    }
}
