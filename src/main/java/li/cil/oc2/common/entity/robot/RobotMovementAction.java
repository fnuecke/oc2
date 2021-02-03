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
import java.util.Objects;

public final class RobotMovementAction extends AbstractRobotAction {
    public static final double TARGET_EPSILON = 0.0001;

    ///////////////////////////////////////////////////////////////////

    private static final float MOVEMENT_SPEED = 1f / Constants.TICK_SECONDS; // In blocks per second.

    private static final String DIRECTION_TAG_NAME = "direction";
    private static final String ORIGIN_TAG_NAME = "origin";
    private static final String START_TAG_NAME = "start";
    private static final String TARGET_TAG_NAME = "target";

    ///////////////////////////////////////////////////////////////////

    private MovementDirection direction;
    @Nullable private BlockPos origin;
    @Nullable private BlockPos start;
    @Nullable private BlockPos target;
    @Nullable private Vector3d targetPos;

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

    ///////////////////////////////////////////////////////////////////

    @Override
    public void initialize(final RobotEntity robot) {
        if (origin == null || start == null || target == null) {
            origin = robot.getPosition();
            start = origin;
            target = start;
            switch (direction) {
                case UP:
                    target = target.offset(Direction.UP);
                    break;
                case DOWN:
                    target = target.offset(Direction.DOWN);
                    break;
                case FORWARD:
                    target = target.offset(robot.getHorizontalFacing());
                    break;
                case BACKWARD:
                    target = target.offset(robot.getHorizontalFacing().getOpposite());
                    break;
            }
        }

        targetPos = getTargetPositionInBlock(target);
        robot.getDataManager().set(RobotEntity.TARGET_POSITION, target);
    }

    @Override
    public RobotActionResult perform(final RobotEntity robot) {
        if (targetPos == null) {
            throw new IllegalStateException();
        }

        validateTarget(robot);

        moveAndResolveCollisions(robot);

        if (robot.getPositionVec().squareDistanceTo(targetPos) < TARGET_EPSILON) {
            if (Objects.equals(target, origin)) {
                return RobotActionResult.FAILURE; // Collided and returned.
            } else {
                return RobotActionResult.SUCCESS; // Made it to new location.
            }
        }

        return RobotActionResult.INCOMPLETE;
    }

    @Override
    public CompoundNBT serialize() {
        final CompoundNBT tag = super.serialize();

        NBTUtils.putEnum(tag, DIRECTION_TAG_NAME, direction);
        if (origin != null) {
            NBTUtils.putBlockPos(tag, ORIGIN_TAG_NAME, origin);
        }
        if (start != null) {
            NBTUtils.putBlockPos(tag, START_TAG_NAME, start);
        }
        if (target != null) {
            NBTUtils.putBlockPos(tag, TARGET_TAG_NAME, target);
        }

        return tag;
    }

    @Override
    public void deserialize(final CompoundNBT tag) {
        super.deserialize(tag);

        direction = NBTUtils.getEnum(tag, DIRECTION_TAG_NAME, MovementDirection.class);
        if (tag.contains(ORIGIN_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            origin = NBTUtils.getBlockPos(tag, ORIGIN_TAG_NAME);
        }
        if (tag.contains(START_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            start = NBTUtils.getBlockPos(tag, START_TAG_NAME);
        }
        if (tag.contains(TARGET_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            target = NBTUtils.getBlockPos(tag, TARGET_TAG_NAME);
            targetPos = getTargetPositionInBlock(target);
        }
    }

    private void moveAndResolveCollisions(final RobotEntity robot) {
        moveTowards(robot, targetPos);

        final boolean didCollide = robot.collidedHorizontally || robot.collidedVertically;
        final long gameTime = robot.getEntityWorld().getGameTime();
        if (didCollide && !robot.getEntityWorld().isRemote()
            && robot.getLastPistonMovement() < gameTime - 1) {
            final BlockPos newStart = target;
            target = start;
            start = newStart;
            targetPos = getTargetPositionInBlock(target);
            robot.getDataManager().set(RobotEntity.TARGET_POSITION, target);
        }
    }

    private void validateTarget(final RobotEntity robot) {
        final BlockPos currentPosition = robot.getPosition();
        if (Objects.equals(currentPosition, start) || Objects.equals(currentPosition, target)) {
            return;
        }

        // Got pushed out of our original path. Adjust start and target by the least offset.
        final BlockPos fromStart = currentPosition.subtract(start);
        final BlockPos fromTarget = currentPosition.subtract(target);

        final int deltaStart = fromStart.getX() + fromStart.getY() + fromStart.getZ();
        final int deltaTarget = fromTarget.getX() + fromTarget.getY() + fromTarget.getZ();

        if (deltaStart < deltaTarget) {
            start = currentPosition;
            target = target.add(fromStart);
        } else {
            start = start.add(fromTarget);
            target = currentPosition;
        }

        targetPos = getTargetPositionInBlock(target);
        robot.getDataManager().set(RobotEntity.TARGET_POSITION, target);
    }
}
