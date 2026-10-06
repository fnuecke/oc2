/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.Robot;
import net.minecraft.nbt.CompoundTag;

public final class RobotRotationActionType extends AbstractRobotActionType {
    public RobotRotationActionType(final int id) {
        super(id);
    }

    // --------------------------------------------------------------------- //

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
        if (!RobotRotationAction.isComplete(robot)) {
            RobotRotationAction.rotateTowards(robot, robot.getEntityData().get(Robot.TARGET_DIRECTION));
        }
    }

    @Override
    public AbstractRobotAction deserialize(final CompoundTag tag) {
        return new RobotRotationAction(tag);
    }
}
