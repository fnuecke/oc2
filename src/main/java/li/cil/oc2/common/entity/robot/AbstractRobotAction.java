package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.nbt.CompoundNBT;

public abstract class AbstractRobotAction {
    private final AbstractRobotActionType type;

    ///////////////////////////////////////////////////////////////////

    public AbstractRobotAction(final AbstractRobotActionType type) {
        this.type = type;
    }

    public AbstractRobotAction(final AbstractRobotActionType type, final CompoundNBT tag) {
        this(type);
        deserialize(tag);
    }

    ///////////////////////////////////////////////////////////////////

    public AbstractRobotActionType getType() {
        return type;
    }

    public void initialize(final RobotEntity robot) {
    }

    public abstract boolean perform(RobotEntity robot);

    public CompoundNBT serialize() {
        return new CompoundNBT();
    }

    ///////////////////////////////////////////////////////////////////

    protected void deserialize(final CompoundNBT tag) {
    }
}
