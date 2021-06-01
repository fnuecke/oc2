package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.nbt.CompoundTag;

public abstract class AbstractRobotAction {
    private static final String ID_TAG_NAME = "id";

    ///////////////////////////////////////////////////////////////////

    private final AbstractRobotActionType type;
    private int id;

    ///////////////////////////////////////////////////////////////////

    public AbstractRobotAction(final AbstractRobotActionType type) {
        this.type = type;
    }

    public AbstractRobotAction(final AbstractRobotActionType type, final CompoundTag tag) {
        this(type);
        deserialize(tag);
    }

    ///////////////////////////////////////////////////////////////////

    public AbstractRobotActionType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public void setId(final int value) {
        id = value;
    }

    public void initialize(final RobotEntity robot) {
    }

    public abstract RobotActionResult perform(RobotEntity robot);

    public CompoundTag serialize() {
        final CompoundTag tag = new CompoundTag();

        tag.putInt(ID_TAG_NAME, id);

        return tag;
    }

    public void deserialize(final CompoundTag tag) {
        id = tag.getInt(ID_TAG_NAME);
    }
}
