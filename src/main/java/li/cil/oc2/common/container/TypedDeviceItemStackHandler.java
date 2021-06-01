package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.util.Devices;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class TypedDeviceContainerHelper extends DeviceContainerHelper {
    private final DeviceType deviceType;
    private final Function<ItemStack, ItemDeviceQuery> queryFactory;

    ///////////////////////////////////////////////////////////////////

    public TypedDeviceContainerHelper(final int size, final Function<ItemStack, ItemDeviceQuery> queryFactory, final DeviceType deviceType) {
        super(size, queryFactory);
        this.deviceType = deviceType;
        this.queryFactory = queryFactory;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return super.isItemValid(slot, stack) && Devices.getDeviceTypes(queryFactory.apply(stack)).contains(deviceType);
    }
}
