package li.cil.oc2.common.device;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.DeviceMethod;
import li.cil.oc2.api.device.IdentifiableDevice;
import li.cil.oc2.common.util.LazyOptionalUtils;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.*;

public final class IdentifiableDeviceImpl implements IdentifiableDevice {
    private final LazyOptional<? extends Device> device;
    private final UUID uuid;
    @Nullable private final String typeName;

    public IdentifiableDeviceImpl(final LazyOptional<? extends Device> device, final UUID uuid) {
        this(device, uuid, null);
    }

    public IdentifiableDeviceImpl(final LazyOptional<? extends Device> device, final UUID uuid, @Nullable final String typeName) {
        this.device = device;
        this.uuid = uuid;
        this.typeName = typeName;
    }

    @Override
    public UUID getUniqueIdentifier() {
        return uuid;
    }

    @Override
    public Device getIdentifiedDevice() {
        return device.<Device>cast().orElse(this);
    }

    @Override
    public List<String> getTypeNames() {
        if (typeName != null) {
            final List<String> typeNames = new ArrayList<>(device.map(Device::getTypeNames).orElse(Collections.emptyList()));
            typeNames.add(typeName);
            return typeNames;
        } else {
            return device.map(Device::getTypeNames).orElse(Collections.emptyList());
        }
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
               LazyOptionalUtils.equals(device, that.device) &&
               Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, LazyOptionalUtils.hashCode(device), typeName);
    }
}
