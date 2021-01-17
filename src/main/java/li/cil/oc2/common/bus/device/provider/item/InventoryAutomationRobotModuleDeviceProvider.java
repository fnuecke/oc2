package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.item.InventoryAutomationRobotModuleDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;

import java.util.Optional;

public final class InventoryAutomationRobotModuleDeviceProvider extends AbstractItemDeviceProvider {
    public InventoryAutomationRobotModuleDeviceProvider() {
        super(Items.INVENTORY_AUTOMATION_MODULE);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<DeviceType> getItemDeviceType(final ItemDeviceQuery query) {
        return Optional.of(DeviceTypes.ROBOT_MODULE);
    }

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return query.getContainerEntity().flatMap(entity ->
                entity.getCapability(Capabilities.ROBOT).map(robot ->
                        new InventoryAutomationRobotModuleDevice(query.getItemStack(), entity, robot)));
    }
}
