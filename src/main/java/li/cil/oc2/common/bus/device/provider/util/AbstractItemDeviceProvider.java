package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.util.Optional;

public abstract class AbstractItemDeviceProvider extends ForgeRegistryEntry<ItemDeviceProvider> implements ItemDeviceProvider {
    private final RegistryObject<Item> item;

    ///////////////////////////////////////////////////////////////////

    protected AbstractItemDeviceProvider(final RegistryObject<Item> item) {
        this.item = item;
    }

    protected AbstractItemDeviceProvider() {
        this.item = null;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public final Optional<ItemDevice> getDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        if (item != null && stack.getItem() != item.get()) {
            return Optional.empty();
        }

        return getItemDevice(query);
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query);
}
