/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;
import java.util.function.Predicate;

public abstract class AbstractItemDeviceProvider implements ItemDeviceProvider {
    private final Predicate<Item> predicate;

    ///////////////////////////////////////////////////////////////////

    private AbstractItemDeviceProvider(final Predicate<Item> predicate) {
        this.predicate = predicate;
    }

    protected AbstractItemDeviceProvider(final RegistryObject<? extends Item> item) {
        this(i -> i == item.get());
    }

    protected AbstractItemDeviceProvider(final Class<? extends Item> type) {
        this(type::isInstance);
    }

    protected AbstractItemDeviceProvider() {
        this.predicate = i -> true;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public final Optional<ItemDevice> getDevice(final ItemDeviceQuery query) {
        return matches(query) ? getItemDevice(query) : Optional.empty();
    }

    @Override
    public final int getEnergyConsumption(final ItemDeviceQuery query) {
        return matches(query) ? getItemDeviceEnergyConsumption(query) : 0;
    }

    ///////////////////////////////////////////////////////////////////

    protected boolean matches(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        return !stack.isEmpty() && predicate.test(stack.getItem());
    }

    protected abstract Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query);

    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return 0;
    }
}
