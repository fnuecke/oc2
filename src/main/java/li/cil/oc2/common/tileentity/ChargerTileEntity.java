package li.cil.oc2.common.tileentity;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.FixedEnergyStorage;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;

public final class ChargerTileEntity extends AbstractTileEntity implements ITickableTileEntity, NamedDevice {
    private final FixedEnergyStorage energy = new FixedEnergyStorage(Config.chargerEnergyStorage);
    private boolean isCharging;

    ///////////////////////////////////////////////////////////////////

    ChargerTileEntity() {
        super(TileEntities.CHARGER_TILE_ENTITY.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void tick() {
        isCharging = false;
        chargeBlock();
        chargeEntities();
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        tag = super.save(tag);

        tag.put(Constants.ENERGY_TAG_NAME, energy.serializeNBT());

        return tag;
    }

    @Override
    public void load(final BlockState state, final CompoundNBT tag) {
        super.load(state, tag);

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

        final TileEntity tileEntity = level.getBlockEntity(getBlockPos().above());
        if (tileEntity != null) {
            chargeCapabilityProvider(tileEntity);
        }
    }

    private void chargeEntities() {
        if (energy.getEnergyStored() == 0) {
            return;
        }

        final List<Entity> entities = level.getEntities((Entity) null, new AxisAlignedBB(getBlockPos().above()), null);
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
        if (energy.extractEnergy(energyStorage.receiveEnergy(amount, false), false) > 0) {
            isCharging = true;
        }
    }
}
