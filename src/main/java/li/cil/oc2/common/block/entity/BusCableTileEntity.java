package li.cil.oc2.common.block.entity;

import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.init.TileEntities;
import li.cil.oc2.common.serialization.NBTSerialization;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public final class BusCableTileEntity extends AbstractTileEntity {
    private static final String BUS_ELEMENT_NBT_TAG_NAME = "busElement";

    ///////////////////////////////////////////////////////////////////

    private final TileEntityDeviceBusElement busElement;

    ///////////////////////////////////////////////////////////////////

    public BusCableTileEntity() {
        super(TileEntities.BUS_CABLE_TILE_ENTITY.get());

        busElement = new BusElement();
    }

    ///////////////////////////////////////////////////////////////////

    public void handleNeighborChanged(final BlockPos pos) {
        busElement.handleNeighborChanged(pos);
    }

    public void handleConnectionTypeChanged(final Direction side) {
        invalidateCapability(Capabilities.DEVICE_BUS_ELEMENT, side);
        handleNeighborChanged(getPos().offset(side));
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

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        if (BusCableBlock.getConnectionType(getBlockState(), direction) != BusCableBlock.ConnectionType.NONE) {
            collector.offer(Capabilities.DEVICE_BUS_ELEMENT, busElement);
        }
    }

    @Override
    protected void loadServer() {
        super.loadServer();

        busElement.initialize();
    }

    @Override
    public void remove() {
        super.remove();

        // Bus element will usually be discovered via bus scan, not via capability request, so
        // automatic invalidation via capability will *not* necessarily schedule a scan on the
        // controller of our current bus. So we need to trigger that manually.
        busElement.dispose();
    }

    ///////////////////////////////////////////////////////////////////

    private final class BusElement extends TileEntityDeviceBusElement {
        public BusElement() {
            super(BusCableTileEntity.this);
        }

        @Override
        public boolean canConnectToSide(@Nullable final Direction direction) {
            return BusCableBlock.getConnectionType(getBlockState(), direction) == BusCableBlock.ConnectionType.LINK;
        }

        @Override
        public boolean hasInterfaceOnSide(@Nullable final Direction direction) {
            return BusCableBlock.getConnectionType(getBlockState(), direction) == BusCableBlock.ConnectionType.PLUG;
        }
    }
}
