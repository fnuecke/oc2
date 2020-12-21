package li.cil.oc2.common.block.entity;

import alexiil.mc.lib.attributes.AttributeList;
import alexiil.mc.lib.attributes.AttributeProviderBlockEntity;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.bus.TileEntityDeviceBusElement;
import li.cil.oc2.common.init.TileEntities;
import li.cil.oc2.common.serialization.NBTSerialization;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BusCableTileEntity extends AbstractTileEntity implements AttributeProviderBlockEntity {
    private static final String BUS_ELEMENT_NBT_TAG_NAME = "busElement";

    ///////////////////////////////////////////////////////////////////

    private final TileEntityDeviceBusElement busElement;

    ///////////////////////////////////////////////////////////////////

    public BusCableTileEntity() {
        super(TileEntities.BUS_CABLE_TILE_ENTITY);

        busElement = new BusElement();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void addAllAttributes(final AttributeList<?> attributeList) {
        attributeList.offer(busElement);
    }

    ///////////////////////////////////////////////////////////////////

    public void handleNeighborChanged(final BlockPos pos) {
        busElement.handleNeighborChanged(pos);
    }

    @Override
    public CompoundTag toTag(CompoundTag compound) {
        compound = super.toTag(compound);
        compound.put(BUS_ELEMENT_NBT_TAG_NAME, NBTSerialization.serialize(busElement));
        return compound;
    }

    @Override
    public void fromTag(final BlockState state, final CompoundTag compound) {
        super.fromTag(state, compound);
        NBTSerialization.deserialize(compound.getCompound(BUS_ELEMENT_NBT_TAG_NAME), busElement);
    }

    ///////////////////////////////////////////////////////////////////

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

    ///////////////////////////////////////////////////////////////////

    private final class BusElement extends TileEntityDeviceBusElement {
        public BusElement() {
            super(BusCableTileEntity.this);
        }

        @Override
        protected boolean canConnectToSide(final Direction direction) {
            final EnumProperty<BusCableBlock.ConnectionType> property = BusCableBlock.FACING_TO_CONNECTION_MAP.get(direction);
            final BusCableBlock.ConnectionType connectionType = getCachedState().get(property);
            return connectionType == BusCableBlock.ConnectionType.PLUG;
        }
    }
}
