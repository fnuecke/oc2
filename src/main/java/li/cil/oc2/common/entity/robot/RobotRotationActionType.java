package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Direction;
import net.minecraft.util.math.Mth;

public final class RobotRotationActionType extends AbstractRobotActionType {
    public RobotRotationActionType(final int id) {
        super(id);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void initializeData(final RobotEntity robot) {
        robot.getEntityData().set(RobotEntity.TARGET_DIRECTION, robot.getDirection());
    }

    @Override
    public void performServer(final RobotEntity robot, final AbstractRobotAction currentAction) {
        if (!(currentAction instanceof RobotRotationAction)) {
            robot.getEntityData().set(RobotEntity.TARGET_DIRECTION, robot.getDirection());
        }
    }

    @Override
    public void performClient(final RobotEntity robot) {
        final Direction target = robot.getEntityData().get(RobotEntity.TARGET_DIRECTION);
        if (Mth.degreesDifferenceAbs(robot.yRot, target.toYRot()) > RobotRotationAction.TARGET_EPSILON) {
            RobotRotationAction.rotateTowards(robot, target);
        }
    }

    @Override
    public AbstractRobotAction deserialize(final CompoundTag tag) {
        return new RobotRotationAction(tag);
    }
}
