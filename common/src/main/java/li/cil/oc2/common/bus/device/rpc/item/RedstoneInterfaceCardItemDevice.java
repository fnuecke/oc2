/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.capabilities.RedstoneEmitter;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.rpc.RedstoneInterfaceDevice;
import li.cil.oc2.common.bus.device.util.NeighborChangeListener;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityProvider;
import li.cil.oc2.common.capabilities.CapabilityType;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public final class RedstoneInterfaceCardItemDevice extends AbstractItemRPCDevice implements CapabilityProvider, NeighborChangeListener {
    private final RedstoneInterfaceDevice redstoneInterface;
    private final RedstoneEmitter[] capabilities;

    // --------------------------------------------------------------------- //

    public RedstoneInterfaceCardItemDevice(final ItemStack identity, final BlockEntity blockEntity) {
        this(identity, new RedstoneInterfaceDevice(blockEntity));
    }

    private RedstoneInterfaceCardItemDevice(final ItemStack identity, final RedstoneInterfaceDevice redstoneInterface) {
        super(identity, redstoneInterface);
        this.redstoneInterface = redstoneInterface;

        capabilities = new RedstoneEmitter[Constants.BLOCK_FACE_COUNT];
        for (int i = 0; i < Constants.BLOCK_FACE_COUNT; i++) {
            final Direction direction = Direction.from3DDataValue(i);
            capabilities[i] = () -> redstoneInterface.getOutput(direction);
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(final CapabilityType<T> capability, @Nullable final Direction side) {
        if (capability == Capabilities.REDSTONE_EMITTER && side != null) {
            return (T) capabilities[side.get3DDataValue()];
        }

        return null;
    }

    @Override
    public void handleNeighborChanged() {
        redstoneInterface.handleNeighborChanged();
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        redstoneInterface.save(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        redstoneInterface.load(tag);
    }
}
