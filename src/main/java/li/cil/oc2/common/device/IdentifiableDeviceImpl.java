package li.cil.oc2.common.device;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.DeviceMethod;
import li.cil.oc2.api.device.IdentifiableDevice;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class IdentifiableDeviceImpl implements IdentifiableDevice {
    private final Device device;
    private final UUID uuid;

    public IdentifiableDeviceImpl(final Device device, final UUID uuid) {
        this.device = device;
        this.uuid = uuid;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public List<String> getTypeNames() {
        return device.getTypeNames();
    }

    @Override
    public List<DeviceMethod> getMethods() {
        return device.getMethods();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final IdentifiableDeviceImpl that = (IdentifiableDeviceImpl) o;
        return uuid.equals(that.uuid) &&
               device.equals(that.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, device);
    }
}
