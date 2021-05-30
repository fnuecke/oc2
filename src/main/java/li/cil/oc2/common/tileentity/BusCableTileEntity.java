package li.cil.oc2.common.tileentity;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.bus.BlockEntityDeviceBusElement;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;

public final class BusCableBlockEntity extends AbstractBlockEntity {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String INTERFACE_NAMES_TAG_NAME = "interfaceNames";

    ///////////////////////////////////////////////////////////////////

    private final BlockEntityDeviceBusElement busElement = new BusCableBusElement();
    private final String[] interfaceNames = new String[Constants.BLOCK_FACE_COUNT];

    ///////////////////////////////////////////////////////////////////

    public BusCableBlockEntity() {
        super(TileEntities.BUS_CABLE_TILE_ENTITY.get());
    }

    ///////////////////////////////////////////////////////////////////

    public String getInterfaceName(final Direction side) {
        final String interfaceName = interfaceNames[side.get3DDataValue()];
        return interfaceName == null ? "" : interfaceName;
    }

    public void setInterfaceName(final Direction side, final String name) {
        final String validatedName = validateName(name);
        if (Objects.equals(validatedName, interfaceNames[side.get3DDataValue()])) {
            return;
        }

        interfaceNames[side.get3DDataValue()] = validatedName;
        if (!level.isClientSide) {
            final BusInterfaceNameMessage message = new BusInterfaceNameMessage.ToClient(this, side, interfaceNames[side.get3DDataValue()]);
            Network.sendToClientsTrackingChunk(message, level.getChunkAt(getBlockPos()));
            handleNeighborChanged(getBlockPos().relative(side));
        }
    }

    public void handleNeighborChanged(final BlockPos pos) {
        busElement.handleNeighborChanged(pos);
    }

    public void handleConnectivityChanged(@Nullable final Direction side) {
        if (side == null) {
            busElement.scheduleScan();
        } else {
            // Whenever they type changes we can clear it. Technically only needed
            // for the interface->none transition, but all others are no-ops, so
            // we can just do this.
            setInterfaceName(side, "");

            invalidateCapability(Capabilities.DEVICE_BUS_ELEMENT, side);
            handleNeighborChanged(getBlockPos().relative(side));
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        // Bus element will usually be discovered via bus scan, not via capability request, so
        // automatic invalidation via capability will *not* necessarily schedule a scan on the
        // controller of our current bus. So we need to trigger that manually.
        busElement.scheduleScan();
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = super.getUpdateTag();

        tag.put(INTERFACE_NAMES_TAG_NAME, serializeInterfaceNames());

        return tag;
    }

    @Override
    public void handleUpdateTag(final BlockState state, final CompoundTag tag) {
        deserializeInterfaceNames(tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag = super.save(tag);
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.serializeNBT());
        tag.put(INTERFACE_NAMES_TAG_NAME, serializeInterfaceNames());

        return tag;
    }

    @Override
    public void load(final BlockState state, final CompoundTag tag) {
        super.load(state, tag);
        busElement.deserializeNBT(tag.getList(BUS_ELEMENT_TAG_NAME, NBTTagIds.TAG_COMPOUND));
        deserializeInterfaceNames(tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
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

    ///////////////////////////////////////////////////////////////////

    private ListTag serializeInterfaceNames() {
        final ListTag tag = new ListTag();
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            tag.add(StringTag.valueOf(getInterfaceName(Direction.from3DDataValue(i))));
        }
        return tag;
    }

    private void deserializeInterfaceNames(final ListTag tag) {
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            final String name = tag.getString(i).trim();
            interfaceNames[i] = name.substring(0, Math.min(32, name.length()));
        }
    }

    private static String validateName(final String name) {
        final String trimmed = name.trim();
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }

    ///////////////////////////////////////////////////////////////////

    private final class BusCableBusElement extends BlockEntityDeviceBusElement {
        public BusCableBusElement() {
            super(BusCableBlockEntity.this);
        }

        @Override
        public boolean canScanContinueTowards(@Nullable final Direction direction) {
            final BusCableBlock.ConnectionType connectionType = BusCableBlock.getConnectionType(getBlockState(), direction);
            return connectionType == BusCableBlock.ConnectionType.CABLE ||
                   connectionType == BusCableBlock.ConnectionType.INTERFACE;
        }

        @Override
        public boolean canDetectDevicesTowards(@Nullable final Direction direction) {
            final BusCableBlock.ConnectionType connectionType = BusCableBlock.getConnectionType(getBlockState(), direction);
            return connectionType == BusCableBlock.ConnectionType.INTERFACE;
        }

        @Override
        protected void collectSyntheticDevices(final World world, final BlockPos pos, final Direction direction, final HashSet<BlockDeviceInfo> devices) {
            super.collectSyntheticDevices(world, pos, direction, devices);
            final String interfaceName = interfaceNames[direction.get3DDataValue()];
            if (!StringUtils.isNullOrEmpty(interfaceName)) {
                devices.add(new BlockDeviceInfo(null, new TypeNameRPCDevice(interfaceName)));
            }
        }

        @Override
        public double getEnergyConsumption() {
            return super.getEnergyConsumption()
                   + Config.busCableEnergyPerTick
                   + BusCableBlock.getInterfaceCount(getBlockState()) * Config.busInterfaceEnergyPerTick;
        }
    }
}
