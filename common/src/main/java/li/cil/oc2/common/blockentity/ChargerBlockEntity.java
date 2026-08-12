/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.EnergyStorage;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import li.cil.oc2.common.util.ChunkUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Collections.singletonList;

public final class ChargerBlockEntity extends ModBlockEntity implements NamedDevice, TickableBlockEntity {
    private static final Predicate<Entity> ENTITY_PREDICATE =
        EntitySelector.NO_SPECTATORS
            .and(EntitySelector.ENTITY_STILL_ALIVE);

    // ------------------------------------------------------------- //

    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.chargerEnergyStorage);
    private boolean isCharging;
    private final AABB renderBoundingBox;

    // ------------------------------------------------------------- //

    ChargerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.CHARGER.get(), pos, state);
        renderBoundingBox = new AABB(pos.above());
    }

    // ------------------------------------------------------------- //

    @Override
    public void clientTick() {
        tick();
    }

    @Override
    public void serverTick() {
        tick();
    }

    private void tick() {
        if (level == null) {
            return;
        }

        isCharging = false;
        chargeBlock();
        chargeEntities();

        if (isCharging) {
            ChunkUtils.setLazyUnsaved(level, getBlockPos());
        }
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put(Constants.ENERGY_TAG_NAME, energy.serializeNBT());
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        energy.deserializeNBT(tag.getCompound(Constants.ENERGY_TAG_NAME));
    }

    @Callback
    public boolean isCharging() {
        return isCharging;
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return singletonList("charger");
    }

    // ------------------------------------------------------------- //

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.ENERGY_STORAGE, energy);
    }

    // ------------------------------------------------------------- //

    private void chargeBlock() {
        assert level != null;

        if (energy.getEnergyStored() == 0) {
            return;
        }

        final BlockEntity blockEntity = level.getBlockEntity(getBlockPos().above());
        if (blockEntity != null) {
            chargeBlockEntity(blockEntity);
        }
    }

    private void chargeEntities() {
        assert level != null;

        if (energy.getEnergyStored() == 0) {
            return;
        }

        final List<Entity> entities = level.getEntities((Entity) null, new AABB(getBlockPos().above()), ENTITY_PREDICATE);
        for (final Entity entity : entities) {
            chargeEntity(entity);
        }
    }

    private void chargeBlockEntity(final BlockEntity blockEntity) {
        final EnergyStorage energyStorage = Capabilities.get(blockEntity, Capabilities.ENERGY_STORAGE, Direction.DOWN);
        if (energyStorage != null) {
            charge(energyStorage);
        }

        final ItemHandler itemHandler = Capabilities.get(blockEntity, Capabilities.ITEM_HANDLER, Direction.DOWN);
        if (itemHandler != null) {
            chargeItems(itemHandler);
        }
    }

    private void chargeEntity(final Entity entity) {
        final EnergyStorage energyStorage = Capabilities.get(entity, Capabilities.ENERGY_STORAGE, Direction.DOWN);
        if (energyStorage != null) {
            charge(energyStorage);
        }

        final ItemHandler itemHandler = Capabilities.get(entity, Capabilities.ITEM_HANDLER, Direction.DOWN);
        if (itemHandler != null) {
            chargeItems(itemHandler);
        }
    }

    private void chargeItems(final ItemHandler itemHandler) {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            final ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                final EnergyStorage stackEnergy = Capabilities.get(stack, Capabilities.ENERGY_STORAGE);
                if (stackEnergy != null) {
                    charge(stackEnergy);
                }
            }
        }
    }

    private void charge(final EnergyStorage energyStorage) {
        assert level != null;

        final long amount = Math.min(energy.getEnergyStored(), Config.chargerEnergyPerTick);
        final boolean simulate = level.isClientSide;
        if (energy.extractEnergy(energyStorage.receiveEnergy(amount, simulate), simulate) > 0) {
            isCharging = true;
        }
    }

    @Nullable
    @Override
    public AABB getExpandedRenderBoundingBox() {
        return renderBoundingBox;
    }
}
