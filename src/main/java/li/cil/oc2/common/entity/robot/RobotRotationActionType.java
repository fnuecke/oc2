package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.Robot;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class RobotRotationActionType extends AbstractRobotActionType {
    public RobotRotationActionType(final int id) {
        super(id);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void initializeData(final Robot robot) {
        robot.getEntityData().set(Robot.TARGET_DIRECTION, robot.getDirection());
    }

    @Override
    public void performServer(final Robot robot, final AbstractRobotAction currentAction) {
        if (!(currentAction instanceof RobotRotationAction)) {
            robot.getEntityData().set(Robot.TARGET_DIRECTION, robot.getDirection());
        }
    }

    @Override
    public void performClient(final Robot robot) {
        final Direction target = robot.getEntityData().get(Robot.TARGET_DIRECTION);
        if (Mth.degreesDifferenceAbs(robot.getYRot(), target.toYRot()) > RobotRotationAction.TARGET_EPSILON) {
            RobotRotationAction.rotateTowards(robot, target);
        }
    }

    @Override
    public AbstractRobotAction deserialize(final CompoundTag tag) {
        return new RobotRotationAction(tag);
    }
}
