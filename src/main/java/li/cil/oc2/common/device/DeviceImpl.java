package li.cil.oc2.common.device;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.DeviceInterface;
import li.cil.oc2.api.device.DeviceMethod;
import li.cil.oc2.common.util.LazyOptionalUtils;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.*;

public final class DeviceImpl implements Device {
    private final LazyOptional<DeviceInterface> deviceInterface;
    private final UUID uuid;
    @Nullable private final String typeName;

    public DeviceImpl(final LazyOptional<? extends DeviceInterface> deviceInterface, final UUID uuid) {
        this(deviceInterface, uuid, null);
    }

    public DeviceImpl(final LazyOptional<? extends DeviceInterface> deviceInterface, final UUID uuid, @Nullable final String typeName) {
        this.deviceInterface = deviceInterface.cast();
        this.uuid = uuid;
        this.typeName = typeName;
    }

    @Override
    public UUID getUniqueIdentifier() {
        return uuid;
    }

    @Override
    public DeviceInterface getIdentifiedDevice() {
        return deviceInterface.orElse(this);
    }

    @Override
    public List<String> getTypeNames() {
        if (typeName != null) {
            final List<String> typeNames = new ArrayList<>(deviceInterface.map(DeviceInterface::getTypeNames).orElse(Collections.emptyList()));
            typeNames.add(typeName);
            return typeNames;
        } else {
            return deviceInterface.map(DeviceInterface::getTypeNames).orElse(Collections.emptyList());
        }
    }

    @Override
    public List<DeviceMethod> getMethods() {
        return deviceInterface.map(DeviceInterface::getMethods).orElse(Collections.emptyList());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DeviceImpl that = (DeviceImpl) o;
        return uuid.equals(that.uuid) &&
               LazyOptionalUtils.equals(deviceInterface, that.deviceInterface) &&
               Objects.equals(typeName, that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, LazyOptionalUtils.hashCode(deviceInterface), typeName);
    }
}
