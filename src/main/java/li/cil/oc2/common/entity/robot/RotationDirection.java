package li.cil.oc2.common.entity.robot;

import javax.annotation.Nullable;

public enum RotationDirection {
    LEFT,
    left(LEFT),
    l(LEFT),

    RIGHT,
    right(RIGHT),
    r(RIGHT);

    @Nullable private final RotationDirection parent;

    RotationDirection() {
        this.parent = null;
    }

    RotationDirection(final RotationDirection parent) {
        this.parent = resolve(parent);
    }

    public RotationDirection resolve() {
        return resolve(this);
    }

    @Nullable
    private static RotationDirection resolve(@Nullable final RotationDirection value) {
        if (value == null) {
            return null;
        } else if (value.parent != null) {
            return resolve(value.parent);
        } else {
            return value;
        }
    }
}
