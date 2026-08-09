/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.object;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A reflection based implementation of {@link RPCDevice} using the {@link Callback}
 * annotation to discover {@link RPCMethod}s in a target object via
 * {@link Callbacks#collectMethods(Object)}.
 */
public final class ObjectDevice implements RPCDevice, ItemDevice {
    private final Object object;
    private final ArrayList<String> typeNames;
    private final List<RPCMethodGroup> methods;
    private final String className;

    ///////////////////////////////////////////////////////////////////

    /**
     * Creates a new object device with methods in the specified object and the
     * specified list of type names.
     *
     * @param object    the object containing methods provided by this device.
     * @param typeNames the type names of the device.
     */
    public ObjectDevice(final Object object, final List<String> typeNames) {
        this.object = object;
        this.typeNames = new ArrayList<>(typeNames);
        this.methods = Callbacks.collectMethods(object);
        this.className = object.getClass().getSimpleName();

        if (object instanceof final NamedDevice namedDevice) {
            this.typeNames.addAll(namedDevice.getDeviceTypeNames());
        }
        if (this.typeNames.isEmpty()) {
            this.typeNames.add(toNiceTypeName(object.getClass()));
        }
    }

    /**
     * Creates a new object device with methods in the specified object and the
     * specified list of type names.
     *
     * @param object    the object containing methods provided by this device.
     * @param typeNames the type names of the device.
     */
    public ObjectDevice(final Object object, final String... typeNames) {
        this(object, asList(typeNames));
    }

    /**
     * Creates a new object device with methods in the specified object and the specified
     * type name. For convenience, the type name may be {@code null}, in which case using
     * this constructor is equivalent to using {@link #ObjectDevice(Object)}.
     *
     * @param object   the object containing methods provided by this device.
     * @param typeName the type name of the device.
     */
    public ObjectDevice(final Object object, @Nullable final String typeName) {
        this(object, typeName != null ? singletonList(typeName) : emptyList());
    }

    /**
     * Creates a new object device with methods in the specified object and no explicit type name.
     *
     * @param object the object containing the methods provided by this device.
     */
    public ObjectDevice(final Object object) {
        this(object, emptyList());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public List<String> getTypeNames() {
        return typeNames;
    }

    @Override
    public List<RPCMethodGroup> getMethodGroups() {
        return methods;
    }

    @Override
    public void mount() {
        if (object instanceof LifecycleAwareDevice device) {
            device.onDeviceMounted();
        }
    }

    @Override
    public void unmount() {
        if (object instanceof LifecycleAwareDevice device) {
            device.onDeviceUnmounted();
        }
    }

    @Override
    public void dispose() {
        if (object instanceof LifecycleAwareDevice device) {
            device.onDeviceDisposed();
        }
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ObjectDevice that = (ObjectDevice) o;
        return object.equals(that.object);
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public String toString() {
        return className;
    }

    ///////////////////////////////////////////////////////////////////

    private static String toNiceTypeName(final Class<?> deviceClass) {
        final String name = deviceClass.getSimpleName()
            .replaceFirst("VMDevice$", "")
            .replaceFirst("RPCDevice$", "")
            .replaceFirst("Device$", "");
        return name
            .replaceAll("([a-z])([A-Z])", "$1_$2")
            .toLowerCase(Locale.ROOT);
    }
}
