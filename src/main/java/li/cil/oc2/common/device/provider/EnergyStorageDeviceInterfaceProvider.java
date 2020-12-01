package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.DeviceInterface;
import li.cil.oc2.api.device.object.Callback;
import li.cil.oc2.api.device.object.ObjectDeviceInterface;
import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStorageDeviceInterfaceProvider extends AbstractCapabilityAnyTileEntityDeviceInterfaceProvider<IEnergyStorage> {
    private static final String ENERGY_STORAGE_TYPE_NAME = "energyStorage";

    public EnergyStorageDeviceInterfaceProvider() {
        super(() -> Capabilities.ENERGY_STORAGE_CAPABILITY);
    }

    @Override
    protected LazyOptional<DeviceInterface> getDeviceInterface(final BlockDeviceQuery query, final IEnergyStorage value) {
        return LazyOptional.of(() -> new ObjectDeviceInterface(new EnergyStorageDevice(value), ENERGY_STORAGE_TYPE_NAME));
    }

    public static final class EnergyStorageDevice extends AbstractObjectProxy<IEnergyStorage> {
        public EnergyStorageDevice(final IEnergyStorage energyStorage) {
            super(energyStorage);
        }

        @Callback
        public int getEnergyStored() {
            return value.getEnergyStored();
        }

        @Callback
        public int getMaxEnergyStored() {
            return value.getMaxEnergyStored();
        }

        @Callback
        public boolean canExtract() {
            return value.canExtract();
        }

        @Callback
        public boolean canReceive() {
            return value.canReceive();
        }
    }
}
