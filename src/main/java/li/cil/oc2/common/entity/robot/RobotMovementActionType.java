package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

public final class RobotMovementActionType extends AbstractRobotActionType {
    public RobotMovementActionType(final int id) {
        super(id);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void registerData(final EntityDataManager dataManager) {
        dataManager.register(RobotEntity.TARGET_POSITION, BlockPos.ZERO);
    }

    @Override
    public void initializeData(final RobotEntity robot) {
        robot.getDataManager().set(RobotEntity.TARGET_POSITION, robot.getPosition());
    }

    @Override
    public void performServer(final RobotEntity robot, final AbstractRobotAction currentAction) {
        if (!(currentAction instanceof RobotMovementAction)) {
            robot.getDataManager().set(RobotEntity.TARGET_POSITION, robot.getPosition());
        }
    }

    @Override
    public void performClient(final RobotEntity robot) {
        final Vector3d target = RobotMovementAction.getTargetPositionInBlock(robot.getDataManager().get(RobotEntity.TARGET_POSITION));
        if (robot.getPositionVec().squareDistanceTo(target) > RobotMovementAction.TARGET_EPSILON) {
            RobotMovementAction.moveTowards(robot, target);
        }
    }

    @Override
    public AbstractRobotAction deserialize(final CompoundNBT tag) {
        return new RobotMovementAction(tag);
    }
}
