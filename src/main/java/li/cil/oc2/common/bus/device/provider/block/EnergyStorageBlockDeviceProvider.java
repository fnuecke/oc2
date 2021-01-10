package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractTileEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public final class EnergyStorageBlockDeviceProvider extends AbstractTileEntityCapabilityDeviceProvider<IEnergyStorage, TileEntity> {
    public EnergyStorageBlockDeviceProvider() {
        super(() -> Capabilities.ENERGY_STORAGE);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final IEnergyStorage value) {
        return LazyOptional.of(() -> new ObjectDevice(new EnergyStorageDevice(value), "energy_storage"));
    }

    ///////////////////////////////////////////////////////////////////

    public static final class EnergyStorageDevice extends IdentityProxy<IEnergyStorage> {
        public EnergyStorageDevice(final IEnergyStorage identity) {
            super(identity);
        }

        @Callback
        public int getEnergyStored() {
            return identity.getEnergyStored();
        }

        @Callback
        public int getMaxEnergyStored() {
            return identity.getMaxEnergyStored();
        }

        @Callback
        public boolean canExtract() {
            return identity.canExtract();
        }

        @Callback
        public boolean canReceive() {
            return identity.canReceive();
        }
    }
}
