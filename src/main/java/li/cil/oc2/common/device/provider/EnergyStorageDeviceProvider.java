package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.object.Callback;
import li.cil.oc2.api.device.object.ObjectDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyStorageDeviceProvider extends AbstractCapabilityBlockDeviceProvider<IEnergyStorage> {
    public EnergyStorageDeviceProvider() {
        super(() -> Capabilities.ENERGY_STORAGE_CAPABILITY);
    }

    @Override
    protected Device getDevice(final IEnergyStorage value) {
        return new EnergyStorageDevice(value);
    }

    public static final class EnergyStorageDevice extends ObjectDevice {
        private final IEnergyStorage energyStorage;

        public EnergyStorageDevice(final IEnergyStorage energyStorage) {
            super("energyStorage");
            this.energyStorage = energyStorage;
        }

        @Callback
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }

        @Callback
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }

        @Callback
        public boolean canExtract() {
            return energyStorage.canExtract();
        }

        @Callback
        public boolean canReceive() {
            return energyStorage.canReceive();
        }
    }
}
