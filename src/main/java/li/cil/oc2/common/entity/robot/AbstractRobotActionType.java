package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.EntityDataManager;

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

    public void registerData(final EntityDataManager dataManager) {
    }

    public void initializeData(final RobotEntity robot) {
    }

    public void performClient(final RobotEntity robot) {
    }

    public abstract AbstractRobotAction deserialize(final CompoundNBT tag);
}
