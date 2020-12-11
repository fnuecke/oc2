package li.cil.oc2.common.bus.device.provider.util;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;

import java.util.function.Supplier;

public abstract class AbstractCapabilityAnyTileEntityDeviceProvider<TCapability> extends AbstractCapabilityTileEntityDeviceProvider<TCapability, TileEntity> {
    public AbstractCapabilityAnyTileEntityDeviceProvider(final Supplier<Capability<TCapability>> capabilitySupplier) {
        super(TileEntity.class, capabilitySupplier);
    }
}
