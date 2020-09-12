package li.cil.circuity.api.vm.devicetree;

import li.cil.circuity.api.vm.device.Device;
import li.cil.circuity.vm.devicetree.FlattenedDeviceTree;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public interface DeviceTree {
    FlattenedDeviceTree flatten();

    int createPHandle(final Device device);

    int getPHandle(final Device device);

    DeviceTree find(final String path);

    DeviceTree addProp(final String name, final Object... values);

    DeviceTree getChild(final String name, @Nullable final String address);

    default DeviceTree getChild(final String name, final int address) {
        return getChild(name, String.format("%x", address));
    }

    default DeviceTree getChild(final String name) {
        return getChild(name, null);
    }

    default DeviceTree putChild(final String name, @Nullable final String address, final Consumer<DeviceTree> builder) {
        builder.accept(getChild(name, address));
        return this;
    }

    default DeviceTree putChild(final String name, final int address, final Consumer<DeviceTree> builder) {
        return putChild(name, String.format("%x", address), builder);
    }

    default DeviceTree putChild(final String name, final Consumer<DeviceTree> builder) {
        return putChild(name, null, builder);
    }
}
