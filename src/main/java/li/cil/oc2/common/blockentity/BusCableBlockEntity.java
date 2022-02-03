/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.bus.BlockEntityDeviceBusElement;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.BusCableFacadeMessage;
import li.cil.oc2.common.network.message.BusInterfaceNameMessage;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;

public final class BusCableBlockEntity extends ModBlockEntity {
    public enum FacadeType {
        NOT_A_BLOCK,
        INVALID_BLOCK,
        VALID_BLOCK,
    }

    private static final String BUS_ELEMENT_TAG_NAME = "busElement";
    private static final String INTERFACE_NAMES_TAG_NAME = "interfaceNames";
    private static final String FACADE_TAG_NAME = "facade";

    ///////////////////////////////////////////////////////////////////

    private final BlockEntityDeviceBusElement busElement = new BusCableBusElement();
    private final String[] interfaceNames = new String[Constants.BLOCK_FACE_COUNT];
    private ItemStack facade = ItemStack.EMPTY;

    ///////////////////////////////////////////////////////////////////

    public BusCableBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.BUS_CABLE.get(), pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    public String getInterfaceName(final Direction side) {
        final String interfaceName = interfaceNames[side.get3DDataValue()];
        return interfaceName == null ? "" : interfaceName;
    }

    public void setInterfaceName(final Direction side, final String name) {
        if (level == null) {
            return;
        }

        final String validatedName = validateName(name);
        if (Objects.equals(validatedName, interfaceNames[side.get3DDataValue()])) {
            return;
        }

        interfaceNames[side.get3DDataValue()] = validatedName;
        setChanged();

        if (!level.isClientSide()) {
            final BusInterfaceNameMessage message = new BusInterfaceNameMessage.ToClient(this, side, interfaceNames[side.get3DDataValue()]);
            Network.sendToClientsTrackingBlockEntity(message, this);
            handleNeighborChanged(getBlockPos().relative(side));
        }
    }

    public FacadeType getFacadeType(final ItemStack stack) {
        return getFacadeType(ItemStackUtils.getBlockState(stack));
    }

    public FacadeType getFacadeType(@Nullable final BlockState state) {
        if (state == null) {
            return FacadeType.NOT_A_BLOCK;
        }

        if (level == null ||
            state.getRenderShape() != RenderShape.MODEL ||
            !state.isSolidRender(level, getBlockPos()) ||
            state.getBlock() instanceof EntityBlock) {
            return FacadeType.INVALID_BLOCK;
        }

        return FacadeType.VALID_BLOCK;
    }

    public ItemStack getFacade() {
        return facade;
    }

    public void setFacade(ItemStack stack) {
        if (level == null) {
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(stack);
        if (getFacadeType(facadeState) != FacadeType.VALID_BLOCK) {
            stack = ItemStack.EMPTY;
        }

        if (ItemStack.isSame(stack, facade)) {
            return;
        }

        facade = stack.copy();
        facade.setCount(1);
        BusCableBlock.setHasFacade(level, getBlockPos(), getBlockState(), facadeState, true);

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);

        if (!level.isClientSide()) {
            final BusCableFacadeMessage message = new BusCableFacadeMessage(getBlockPos(), facade);
            Network.sendToClientsTrackingBlockEntity(message, this);
        }
    }

    public void removeFacade() {
        if (level == null) {
            return;
        }

        final BlockState facadeState = ItemStackUtils.getBlockState(facade);
        facade = ItemStack.EMPTY;
        BusCableBlock.setHasFacade(level, getBlockPos(), getBlockState(), facadeState, false);

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);

        if (!level.isClientSide()) {
            final BusCableFacadeMessage message = new BusCableFacadeMessage(getBlockPos(), facade);
            Network.sendToClientsTrackingBlockEntity(message, this);
        }
    }

    public void handleNeighborChanged(final BlockPos pos) {
        busElement.handleNeighborChanged(pos);
    }

    public void handleConnectivityChanged(@Nullable final Direction side, final boolean neighborConnectionChanged) {
        if (side == null) {
            busElement.scheduleScan();
        } else {
            // Whenever the type changes we can clear it. Technically only needed
            // for the interface->none transition, but all others are no-ops, so
            // we can just do this.
            setInterfaceName(side, "");

            invalidateCapability(Capabilities.DEVICE_BUS_ELEMENT, side);
            handleNeighborChanged(getBlockPos().relative(side));

            if (neighborConnectionChanged) {
                busElement.scheduleScan();
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        final CompoundTag tag = super.getUpdateTag();

        tag.put(INTERFACE_NAMES_TAG_NAME, serializeInterfaceNames());
        tag.put(FACADE_TAG_NAME, facade.serializeNBT());

        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag) {
        deserializeInterfaceNames(tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
        setFacade(ItemStack.of(tag.getCompound(FACADE_TAG_NAME)));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put(BUS_ELEMENT_TAG_NAME, busElement.save());
        tag.put(INTERFACE_NAMES_TAG_NAME, serializeInterfaceNames());
        tag.put(FACADE_TAG_NAME, facade.serializeNBT());
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        busElement.load(tag.getCompound(BUS_ELEMENT_TAG_NAME));
        deserializeInterfaceNames(tag.getList(INTERFACE_NAMES_TAG_NAME, NBTTagIds.TAG_STRING));
        facade = ItemStack.of(tag.getCompound(FACADE_TAG_NAME));
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
    protected void unloadServer(final boolean isRemove) {
        super.unloadServer(isRemove);

        if (isRemove) {
            // Bus element will usually be discovered via bus scan, not via capability request, so
            // automatic invalidation via capability will *not* necessarily schedule a scan on the
            // controller of our current bus. So we need to trigger that manually.
            // The controller already listens to chunk unloads, so we don't want to call this when
            // the containing chunk gets unloaded, only when we're being removed.
            busElement.scheduleScan();
        }
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
        protected void collectSyntheticDevices(final Level level, final BlockPos pos, @Nullable final Direction direction, final HashSet<BlockEntry> entries) {
            super.collectSyntheticDevices(level, pos, direction, entries);
            if (direction != null) {
                final String interfaceName = interfaceNames[direction.get3DDataValue()];
                if (!StringUtil.isNullOrEmpty(interfaceName)) {
                    entries.add(new BlockEntry(new BlockDeviceInfo(null, new TypeNameRPCDevice(interfaceName)), pos));
                }
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
