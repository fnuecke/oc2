package li.cil.oc2.common.tile;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.serialization.NBTSerialization;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;

public class BusCableTileEntity extends AbstractTileEntity {
    private static final String BUS_ELEMENT_NBT_TAG_NAME = "busElement";

    private final TileEntityDeviceBusElement busElement;

    public BusCableTileEntity() {
        super(OpenComputers.BUS_CABLE_TILE_ENTITY.get());

        busElement = new TileEntityDeviceBusElement(this);
        setCapabilityIfAbsent(Capabilities.DEVICE_BUS_ELEMENT_CAPABILITY, busElement);
    }

    public void handleNeighborChanged(final BlockPos pos) {
        busElement.handleNeighborChanged(pos);
    }

    @Override
    protected void initializeServer() {
        super.initializeServer();
        busElement.initialize();
    }

    @Override
    protected void disposeServer() {
        super.disposeServer();
        busElement.dispose();
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound = super.write(compound);
        compound.put(BUS_ELEMENT_NBT_TAG_NAME, NBTSerialization.serialize(busElement));
        return compound;
    }

    @Override
    public void read(final BlockState state, final CompoundNBT compound) {
        super.read(state, compound);
        NBTSerialization.deserialize(compound.getCompound(BUS_ELEMENT_NBT_TAG_NAME), busElement);
    }
}
