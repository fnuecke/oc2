package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.nbt.CompoundNBT;

public abstract class AbstractRobotActionType {
    private final int id;

    ///////////////////////////////////////////////////////////////////

    protected AbstractRobotActionType(final int id) {
        this.id = id;
    }

    ///////////////////////////////////////////////////////////////////

    public int getId() {
        return id;
    }

    public void initializeData(final RobotEntity robot) {
    }

    public void performServer(final RobotEntity robot, final AbstractRobotAction currentAction) {
    }

    public void performClient(final RobotEntity robot) {
    }

    public abstract AbstractRobotAction deserialize(final CompoundNBT tag);
}
