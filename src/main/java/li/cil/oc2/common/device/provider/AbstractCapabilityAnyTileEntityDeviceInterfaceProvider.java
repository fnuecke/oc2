package li.cil.oc2.common.device.provider;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;

import java.util.function.Supplier;

public abstract class AbstractCapabilityAnyTileEntityDeviceInterfaceProvider<TCapability> extends AbstractCapabilityTileEntityDeviceInterfaceProvider<TCapability, TileEntity> {
    public AbstractCapabilityAnyTileEntityDeviceInterfaceProvider(final Supplier<Capability<TCapability>> capabilitySupplier) {
        super(TileEntity.class, capabilitySupplier);
    }
}
