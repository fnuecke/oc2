package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public final class RobotMovementActionType extends AbstractRobotActionType {
    public RobotMovementActionType(final int id) {
        super(id);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void initializeData(final RobotEntity robot) {
        robot.getEntityData().set(RobotEntity.TARGET_POSITION, robot.blockPosition());
    }

    @Override
    public void performServer(final RobotEntity robot, final AbstractRobotAction currentAction) {
        if (!(currentAction instanceof RobotMovementAction)) {
            robot.getEntityData().set(RobotEntity.TARGET_POSITION, robot.blockPosition());
        }
    }

    @Override
    public void performClient(final RobotEntity robot) {
        final Vec3 target = RobotMovementAction.getTargetPositionInBlock(robot.getEntityData().get(RobotEntity.TARGET_POSITION));
        if (robot.position().distanceToSqr(target) > RobotMovementAction.TARGET_EPSILON) {
            RobotMovementAction.moveTowards(robot, target);
        }
    }

    @Override
    public AbstractRobotAction deserialize(final CompoundTag tag) {
        return new RobotMovementAction(tag);
    }
}
