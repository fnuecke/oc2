package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractCapabilityAnyTileEntityDeviceProvider;
import li.cil.oc2.common.bus.device.provider.util.AbstractObjectProxy;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStorageDeviceProvider extends AbstractCapabilityAnyTileEntityDeviceProvider<IEnergyStorage> {
    private static final String ENERGY_STORAGE_TYPE_NAME = "energyStorage";

    ///////////////////////////////////////////////////////////////////

    public EnergyStorageDeviceProvider() {
        super(() -> Capabilities.ENERGY_STORAGE_CAPABILITY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected LazyOptional<Device> getDeviceInterface(final BlockDeviceQuery query, final IEnergyStorage value) {
        return LazyOptional.of(() -> new ObjectDevice(new EnergyStorageDevice(value), ENERGY_STORAGE_TYPE_NAME));
    }

    ///////////////////////////////////////////////////////////////////

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
