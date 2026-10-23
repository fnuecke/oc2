/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.Robot;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class AbstractRobotAction {
    private static final String ID_TAG_NAME = "id";

    // --------------------------------------------------------------------- //

    private final AbstractRobotActionType type;
    private int id;

    // --------------------------------------------------------------------- //

    public AbstractRobotAction(final AbstractRobotActionType type) {
        this.type = type;
    }

    public AbstractRobotAction(final AbstractRobotActionType type, final ValueInput input) {
        this(type);
        deserialize(input);
    }

    // --------------------------------------------------------------------- //

    public AbstractRobotActionType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public void setId(final int value) {
        id = value;
    }

    public void initialize(final Robot robot) {
    }

    public abstract RobotActionResult perform(Robot robot);

    public void serialize(final ValueOutput output) {
        output.putInt(ID_TAG_NAME, id);
    }

    public void deserialize(final ValueInput input) {
        id = input.getIntOr(ID_TAG_NAME, 0);
    }
}
