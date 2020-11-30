package li.cil.oc2.common.device;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.DeviceMethod;
import li.cil.oc2.api.device.IdentifiableDevice;
import li.cil.oc2.common.util.LazyOptionalUtils;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class IdentifiableDeviceImpl implements IdentifiableDevice {
    private final LazyOptional<? extends Device> device;
    private final UUID uuid;

    public IdentifiableDeviceImpl(final Device device, final UUID uuid) {
        this(LazyOptional.of(() -> device), uuid);
    }

    public IdentifiableDeviceImpl(final LazyOptional<? extends Device> device, final UUID uuid) {
        this.device = device;
        this.uuid = uuid;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public List<String> getTypeNames() {
        return device.map(Device::getTypeNames).orElse(Collections.emptyList());
    }

    @Override
    public List<DeviceMethod> getMethods() {
        return device.map(Device::getMethods).orElse(Collections.emptyList());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final IdentifiableDeviceImpl that = (IdentifiableDeviceImpl) o;
        return uuid.equals(that.uuid) &&
               LazyOptionalUtils.equals(device, that.device);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, LazyOptionalUtils.hashCode(device));
    }
}
