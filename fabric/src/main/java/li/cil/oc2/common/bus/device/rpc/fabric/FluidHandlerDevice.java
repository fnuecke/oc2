/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.fabric;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class FluidHandlerDevice extends IdentityProxy<Storage<FluidVariant>> implements NamedDevice {
    private static final long DROPLETS_PER_MILLIBUCKET = FluidConstants.BUCKET / 1000;

    // ------------------------------------------------------------- //

    public FluidHandlerDevice(final Storage<FluidVariant> identity) {
        super(identity);
    }

    // ------------------------------------------------------------- //

    @Override
    public Collection<String> getDeviceTypeNames() {
        return Collections.singletonList("fluid_handler");
    }

    @Callback
    public int getFluidTanks() {
        return tanks().size();
    }

    @Callback
    public FluidInfo getFluidInTank(final int tank) {
        final StorageView<FluidVariant> view = tank(tank);
        if (view == null || view.isResourceBlank()) {
            return FluidInfo.EMPTY;
        }

        final ResourceLocation fluid = BuiltInRegistries.FLUID.getKey(view.getResource().getFluid());
        return new FluidInfo(fluid.toString(), toMillibuckets(view.getAmount()));
    }

    @Callback
    public int getFluidTankCapacity(final int tank) {
        final StorageView<FluidVariant> view = tank(tank);
        return view == null ? 0 : toMillibuckets(view.getCapacity());
    }

    // ------------------------------------------------------------- //

    public record FluidInfo(String fluid, int amount) {
        static final FluidInfo EMPTY = new FluidInfo(BuiltInRegistries.FLUID.getKey(Fluids.EMPTY).toString(), 0);
    }

    // ------------------------------------------------------------- //

    private static int toMillibuckets(final long droplets) {
        return (int) Math.min(droplets / DROPLETS_PER_MILLIBUCKET, Integer.MAX_VALUE);
    }

    @Nullable
    private StorageView<FluidVariant> tank(final int tank) {
        final List<StorageView<FluidVariant>> tanks = tanks();
        return tank >= 0 && tank < tanks.size() ? tanks.get(tank) : null;
    }

    private List<StorageView<FluidVariant>> tanks() {
        if (identity instanceof final SlottedStorage<FluidVariant> slotted) {
            return new ArrayList<>(slotted.getSlots());
        }

        final List<StorageView<FluidVariant>> tanks = new ArrayList<>();
        for (final StorageView<FluidVariant> view : identity) {
            tanks.add(view);
        }
        return tanks;
    }
}
