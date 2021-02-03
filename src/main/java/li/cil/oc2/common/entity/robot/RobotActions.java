package li.cil.oc2.common.entity.robot;

import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.nbt.CompoundNBT;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.IntFunction;

public final class RobotActions {
    private static final String ACTION_TYPE_TAG_NAME = "action_type";

    private static final ArrayList<AbstractRobotActionType> ACTIONS = new ArrayList<>();

    ///////////////////////////////////////////////////////////////////

    public static final AbstractRobotActionType MOVEMENT = register(RobotMovementActionType::new);
    public static final AbstractRobotActionType ROTATION = register(RobotRotationActionType::new);

    ///////////////////////////////////////////////////////////////////

    public static void initializeData(final RobotEntity robot) {
        for (final AbstractRobotActionType type : ACTIONS) {
            type.initializeData(robot);
        }
    }

    public static void performServer(final RobotEntity robot, final AbstractRobotAction currentAction) {
        for (final AbstractRobotActionType type : ACTIONS) {
            type.performServer(robot, currentAction);
        }
    }

    public static void performClient(final RobotEntity robot) {
        for (final AbstractRobotActionType type : ACTIONS) {
            type.performClient(robot);
        }
    }

    public static CompoundNBT serialize(final AbstractRobotAction action) {
        final CompoundNBT actionTag = action.serialize();
        actionTag.putInt(ACTION_TYPE_TAG_NAME, action.getType().getId());
        return actionTag;
    }

    @Nullable
    public static AbstractRobotAction deserialize(final CompoundNBT tag) {
        final int type = tag.getInt(ACTION_TYPE_TAG_NAME);
        if (type < 1 || type > ACTIONS.size()) {
            return null;
        }

        final AbstractRobotActionType actionType = ACTIONS.get(type - 1);
        return actionType.deserialize(tag);
    }

    ///////////////////////////////////////////////////////////////////

    private static AbstractRobotActionType register(final IntFunction<? extends AbstractRobotActionType> factory) {
        final AbstractRobotActionType type = factory.apply(ACTIONS.size() + 1);
        ACTIONS.add(type);
        return type;
    }
}
