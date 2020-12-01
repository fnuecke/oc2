package li.cil.oc2.api.device.object;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.DeviceMethod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A reflection based implementation of {@link Device} using the {@link Callback}
 * annotation to discover {@link DeviceMethod} in a target object via
 * {@link Callbacks#collectMethods(Object)}.
 * <p>
 * This class was designed targeting two possible use-cases:
 * <ul>
 *     <li>Wrapping some separate object containing the annotated method.</li>
 *     <li>Subclassing this type and implementing annotated methods in the subclass.</li>
 * </ul>
 * The two sets of constructors are designed for these use cases, with the constructors
 * targeting the workflow using an external object being {@code public}, the ones targeting
 * subclassing being {@code protected}.
 */
public class ObjectDevice implements Device {
    private final ArrayList<String> typeNames;
    private final List<DeviceMethod> methods;
    private final String className;

    public ObjectDevice(final Object object, final List<String> typeNames) {
        this.typeNames = new ArrayList<>(typeNames);
        this.methods = Callbacks.collectMethods(object);
        this.className = object.getClass().getSimpleName();
    }

    public ObjectDevice(final Object object, @Nullable final String typeName) {
        this(object, typeName != null ? Collections.singletonList(typeName) : Collections.emptyList());
    }

    public ObjectDevice(final Object object) {
        this(object, Collections.emptyList());
    }

    protected ObjectDevice(final List<String> typeNames) {
        this.typeNames = new ArrayList<>(typeNames);
        this.methods = Callbacks.collectMethods(this);
        this.className = getClass().getSimpleName();
    }

    protected ObjectDevice(final String typeName) {
        this(Collections.singletonList(typeName));
    }

    protected ObjectDevice() {
        this(Collections.emptyList());
    }

    @Override
    public List<String> getTypeNames() {
        return typeNames;
    }

    @Override
    public List<DeviceMethod> getMethods() {
        return methods;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ObjectDevice that = (ObjectDevice) o;
        return typeNames.equals(that.typeNames) &&
               methods.equals(that.methods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeNames, methods);
    }

    @Override
    public String toString() {
        return className;
    }
}
