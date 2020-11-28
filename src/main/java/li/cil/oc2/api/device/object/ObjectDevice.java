package li.cil.oc2.api.device.object;

import li.cil.oc2.api.device.AbstractDevice;
import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.DeviceMethod;

import java.util.Collections;
import java.util.List;

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
public class ObjectDevice extends AbstractDevice {
    private final List<DeviceMethod> methods;

    public ObjectDevice(final Object object, final List<String> typeNames) {
        super(typeNames);
        this.methods = Callbacks.collectMethods(object);
    }

    public ObjectDevice(final Object object, final String typeName) {
        this(object, Collections.singletonList(typeName));
    }

    public ObjectDevice(final Object object) {
        this(object, Collections.emptyList());
    }

    protected ObjectDevice(final List<String> typeNames) {
        super(typeNames);
        this.methods = Callbacks.collectMethods(this);
    }

    protected ObjectDevice(final String typeName) {
        this(Collections.singletonList(typeName));
    }

    protected ObjectDevice() {
        this(Collections.emptyList());
    }

    @Override
    public List<DeviceMethod> getMethods() {
        return methods;
    }
}
