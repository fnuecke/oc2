package li.cil.oc2.common.tileentity;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ChargerBlockEntity extends AbstractBlockEntity implements TickableBlockEntity {
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.chargerEnergyStorage);

    ///////////////////////////////////////////////////////////////////

    ChargerBlockEntity() {
        super(TileEntities.CHARGER_TILE_ENTITY.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void tick() {
        chargeBlock();
        chargeEntities();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag = super.save(tag);

        tag.put(Constants.ENERGY_TAG_NAME, energy.serializeNBT());

        return tag;
    }

    @Override
    public void load(final BlockState state, final CompoundTag tag) {
        super.load(state, tag);

        energy.deserializeNBT(tag.getCompound(Constants.ENERGY_TAG_NAME));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void collectCapabilities(final CapabilityCollector collector, @Nullable final Direction direction) {
        collector.offer(Capabilities.ENERGY_STORAGE, energy);
    }

    ///////////////////////////////////////////////////////////////////

    private void chargeBlock() {
        if (energy.getEnergyStored() == 0) {
            return;
        }

        final BlockEntity tileEntity = getLevel().getBlockEntity(getBlockPos().above());
        if (tileEntity != null) {
            chargeCapabilityProvider(tileEntity);
        }
    }

    private void chargeEntities() {
        if (energy.getEnergyStored() == 0) {
            return;
        }

        final List<Entity> entities = getLevel().getEntities((Entity) null, new AABB(getBlockPos().above()), null);
        for (final Entity entity : entities) {
            chargeCapabilityProvider(entity);
        }
    }

    private void chargeCapabilityProvider(final ICapabilityProvider capabilityProvider) {
        capabilityProvider.getCapability(Capabilities.ENERGY_STORAGE, Direction.DOWN).ifPresent(this::charge);
        capabilityProvider.getCapability(Capabilities.ITEM_HANDLER, Direction.DOWN).ifPresent(this::chargeItems);
    }

    private void chargeItems(final IItemHandler itemHandler) {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            final ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stack.getCapability(Capabilities.ENERGY_STORAGE).ifPresent(this::charge);
            }
        }
    }

    private void charge(final IEnergyStorage energyStorage) {
        final int amount = Math.min(energy.getEnergyStored(), Config.chargerEnergyPerTick);
        energy.extractEnergy(energyStorage.receiveEnergy(amount, false), false);
    }
}
