package li.cil.oc2.common.entity.robot;

import javax.annotation.Nullable;

public enum MovementDirection {
    UPWARD,
    upward(UPWARD),
    up(UPWARD),
    u(UPWARD),

    DOWNWARD,
    downward(DOWNWARD),
    down(DOWNWARD),
    d(DOWNWARD),

    FORWARD,
    forward(FORWARD),
    ahead(FORWARD),
    f(FORWARD),

    BACKWARD,
    backward(BACKWARD),
    back(BACKWARD),
    b(BACKWARD),
    ;

    @Nullable private final MovementDirection parent;

    MovementDirection() {
        this.parent = null;
    }

    MovementDirection(final MovementDirection parent) {
        this.parent = resolve(parent);
    }

    @Nullable
    public MovementDirection resolve() {
        return resolve(this);
    }

    @Nullable
    private static MovementDirection resolve(@Nullable final MovementDirection value) {
        if (value == null) {
            return null;
        } else if (value.parent != null) {
            return resolve(value.parent);
        } else {
            return value;
        }
    }
}
