package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.ForgeRegistryEntry;

import java.util.Optional;

public abstract class AbstractItemDeviceProvider extends ForgeRegistryEntry<ItemDeviceProvider> implements ItemDeviceProvider {
    private final RegistryObject<? extends Item> item;

    ///////////////////////////////////////////////////////////////////

    protected AbstractItemDeviceProvider(final RegistryObject<? extends Item> item) {
        this.item = item;
    }

    protected AbstractItemDeviceProvider() {
        this.item = null;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public final Optional<ItemDevice> getDevice(final ItemDeviceQuery query) {
        return matches(query) ? getItemDevice(query) : Optional.empty();
    }

    @Override
    public final Optional<DeviceType> getDeviceType(final ItemDeviceQuery query) {
        return matches(query) ? getItemDeviceType(query) : Optional.empty();
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query);

    protected Optional<DeviceType> getItemDeviceType(final ItemDeviceQuery query) {
        return Optional.of(DeviceTypes.CARD);
    }

    ///////////////////////////////////////////////////////////////////

    private boolean matches(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        return !stack.isEmpty() && (item == null || stack.getItem() == item.get());
    }
}
