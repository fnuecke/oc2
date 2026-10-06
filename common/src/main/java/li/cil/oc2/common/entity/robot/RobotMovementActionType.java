/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.Robot;
import net.minecraft.nbt.CompoundTag;

public final class RobotMovementActionType extends AbstractRobotActionType {
    public RobotMovementActionType(final int id) {
        super(id);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void initializeData(final Robot robot) {
        robot.getEntityData().set(Robot.TARGET_POSITION, robot.blockPosition());
    }

    @Override
    public void performServer(final Robot robot, final AbstractRobotAction currentAction) {
        if (!(currentAction instanceof RobotMovementAction)) {
            robot.getEntityData().set(Robot.TARGET_POSITION, robot.blockPosition());
        }
    }

    @Override
    public void performClient(final Robot robot) {
        if (!RobotMovementAction.isComplete(robot)) {
            RobotMovementAction.moveTowards(robot, RobotMovementAction.getTargetPositionInBlock(robot.getEntityData().get(Robot.TARGET_POSITION)));
        }
    }

    @Override
    public AbstractRobotAction deserialize(final CompoundTag tag) {
        return new RobotMovementAction(tag);
    }
}
