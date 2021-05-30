package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.Constants;
import li.cil.oc2.common.entity.Entities;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NBTUtil;
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

    RobotMovementAction(final CompoundTag tag) {
        super(RobotActions.MOVEMENT);
        deserialize(tag);
    }

    ///////////////////////////////////////////////////////////////////

    public static Vector3d getTargetPositionInBlock(final BlockPos position) {
        return Vector3d.atBottomCenterOf(position).add(0, 0.5f * (1 - Entities.ROBOT.get().getHeight()), 0);
    }

    public static void moveTowards(final RobotEntity robot, final Vector3d targetPosition) {
        Vector3d delta = targetPosition.subtract(robot.position());
        if (delta.lengthSqr() > MOVEMENT_SPEED * MOVEMENT_SPEED) {
            delta = delta.normalize().scale(MOVEMENT_SPEED);
        }

        robot.move(MoverType.SELF, delta);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void initialize(final RobotEntity robot) {
        if (origin == null || start == null || target == null) {
            origin = robot.blockPosition();
            start = origin;
            target = start;
            switch (direction) {
                case UP:
                    target = target.relative(Direction.UP);
                    break;
                case DOWN:
                    target = target.relative(Direction.DOWN);
                    break;
                case FORWARD:
                    target = target.relative(robot.getDirection());
                    break;
                case BACKWARD:
                    target = target.relative(robot.getDirection().getOpposite());
                    break;
            }
        }

        targetPos = getTargetPositionInBlock(target);
        robot.getEntityData().set(RobotEntity.TARGET_POSITION, target);
    }

    @Override
    public RobotActionResult perform(final RobotEntity robot) {
        if (targetPos == null) {
            throw new IllegalStateException();
        }

        validateTarget(robot);

        moveAndResolveCollisions(robot);

        if (robot.position().distanceToSqr(targetPos) < TARGET_EPSILON) {
            if (Objects.equals(target, origin)) {
                return RobotActionResult.FAILURE; // Collided and returned.
            } else {
                return RobotActionResult.SUCCESS; // Made it to new location.
            }
        }

        return RobotActionResult.INCOMPLETE;
    }

    @Override
    public CompoundTag serialize() {
        final CompoundTag tag = super.serialize();

        NBTUtils.putEnum(tag, DIRECTION_TAG_NAME, direction);
        if (origin != null) {
            tag.put(ORIGIN_TAG_NAME, NBTUtil.writeBlockPos(origin));
        }
        if (start != null) {
            tag.put(START_TAG_NAME, NBTUtil.writeBlockPos(start));
        }
        if (target != null) {
            tag.put(TARGET_TAG_NAME, NBTUtil.writeBlockPos(target));
        }

        return tag;
    }

    @Override
    public void deserialize(final CompoundTag tag) {
        super.deserialize(tag);

        direction = NBTUtils.getEnum(tag, DIRECTION_TAG_NAME, MovementDirection.class);
        if (tag.contains(ORIGIN_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            origin = NBTUtil.readBlockPos(tag.getCompound(ORIGIN_TAG_NAME));
        }
        if (tag.contains(START_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            start = NBTUtil.readBlockPos(tag.getCompound(START_TAG_NAME));
        }
        if (tag.contains(TARGET_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            target = NBTUtil.readBlockPos(tag.getCompound(TARGET_TAG_NAME));
            targetPos = getTargetPositionInBlock(target);
        }
    }

    private void moveAndResolveCollisions(final RobotEntity robot) {
        moveTowards(robot, targetPos);

        final boolean didCollide = robot.horizontalCollision || robot.verticalCollision;
        final long gameTime = robot.level.getGameTime();
        if (didCollide && !robot.level.isClientSide
            && robot.getLastPistonMovement() < gameTime - 1) {
            final BlockPos newStart = target;
            target = start;
            start = newStart;
            targetPos = getTargetPositionInBlock(target);
            robot.getEntityData().set(RobotEntity.TARGET_POSITION, target);
        }
    }

    private void validateTarget(final RobotEntity robot) {
        final BlockPos currentPosition = robot.blockPosition();
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
            target = target.offset(fromStart);
        } else {
            start = start.offset(fromTarget);
            target = currentPosition;
        }

        targetPos = getTargetPositionInBlock(target);
        robot.getEntityData().set(RobotEntity.TARGET_POSITION, target);
    }
}
