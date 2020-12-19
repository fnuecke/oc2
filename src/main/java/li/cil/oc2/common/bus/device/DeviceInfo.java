package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.DeviceProvider;

import java.util.Objects;

public final class DeviceInfo {
    public final Device device;
    public final DeviceProvider provider;

    public DeviceInfo(final Device device, final DeviceProvider provider) {
        this.device = device;
        this.provider = provider;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DeviceInfo that = (DeviceInfo) o;
        return device.equals(that.device) && provider.equals(that.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, provider);
    }
}
