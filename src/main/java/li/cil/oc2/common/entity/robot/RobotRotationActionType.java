package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;

public final class RobotRotationActionType extends AbstractRobotActionType {
    public RobotRotationActionType(final int id) {
        super(id);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void registerData(final EntityDataManager dataManager) {
        dataManager.register(RobotRotationAction.TARGET_DIRECTION, Direction.NORTH);
    }

    @Override
    public void initializeData(final RobotEntity robot) {
        robot.getDataManager().set(RobotRotationAction.TARGET_DIRECTION, robot.getHorizontalFacing());
    }

    @Override
    public void performClient(final RobotEntity robot) {
        final Direction target = robot.getDataManager().get(RobotRotationAction.TARGET_DIRECTION);
        if (MathHelper.degreesDifferenceAbs(robot.rotationYaw, target.getHorizontalAngle()) > RobotRotationAction.TARGET_EPSILON) {
            RobotRotationAction.rotateTowards(robot, target);
        }
    }

    @Override
    public AbstractRobotAction deserialize(final CompoundNBT tag) {
        return new RobotRotationAction(tag);
    }
}
