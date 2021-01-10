package li.cil.oc2.common.tileentity;

import li.cil.oc2.client.model.BusCableBakedModel;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nullable;

public final class BusCableTileEntity extends AbstractTileEntity {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";

    ///////////////////////////////////////////////////////////////////

    private final TileEntityDeviceBusElement busElement = new BusCableBusElement();

    ///////////////////////////////////////////////////////////////////

    public BusCableTileEntity() {
        super(TileEntities.BUS_CABLE_TILE_ENTITY.get());
    }

    ///////////////////////////////////////////////////////////////////

    public void handleNeighborChanged(final BlockPos pos) {
        busElement.handleNeighborChanged(pos);

        // TODO Remove if https://github.com/MinecraftForge/MinecraftForge/pull/7595 gets merged.
        requestModelDataUpdate();
    }

    public void handleConnectionTypeChanged(final Direction side) {
        invalidateCapability(Capabilities.DEVICE_BUS_ELEMENT, side);
        handleNeighborChanged(getPos().offset(side));
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag = super.write(tag);
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.serializeNBT());
        return tag;
    }

    @Override
    public void read(final BlockState state, final CompoundNBT tag) {
        super.read(state, tag);
        if (tag.contains(BUS_ELEMENT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            busElement.deserializeNBT(tag.getList(BUS_ELEMENT_TAG_NAME, NBTTagIds.TAG_COMPOUND));
        }
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
        busElement.scheduleScan();
    }

    // TODO Remove if https://github.com/MinecraftForge/MinecraftForge/pull/7595 gets merged.
    @Override
    public IModelData getModelData() {
        final World world = getWorld();
        if (world != null) {
            return BusCableBakedModel.getBusCableSupportSideData(world, getPos(), getBlockState(), super.getModelData());
        }
        return super.getModelData();
    }

    ///////////////////////////////////////////////////////////////////

    private final class BusCableBusElement extends TileEntityDeviceBusElement {
        public BusCableBusElement() {
            super(BusCableTileEntity.this);
        }

        @Override
        public boolean canScanContinueTowards(@Nullable final Direction direction) {
            final BusCableBlock.ConnectionType connectionType = BusCableBlock.getConnectionType(getBlockState(), direction);
            return connectionType == BusCableBlock.ConnectionType.LINK ||
                   connectionType == BusCableBlock.ConnectionType.PLUG;
        }

        @Override
        public boolean canDetectDevicesTowards(@Nullable final Direction direction) {
            final BusCableBlock.ConnectionType connectionType = BusCableBlock.getConnectionType(getBlockState(), direction);
            return connectionType == BusCableBlock.ConnectionType.PLUG;
        }
    }
}
