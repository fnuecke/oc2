package li.cil.oc2.common.capabilities;

import alexiil.mc.lib.attributes.Attribute;
import alexiil.mc.lib.attributes.Attributes;
import alexiil.mc.lib.attributes.fluid.FixedFluidInvView;
import alexiil.mc.lib.attributes.fluid.FluidAttributes;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.ItemAttributes;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.bus.AbstractDeviceBusElement;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

public final class Capabilities {
    public static Attribute<DeviceBusElement> DEVICE_BUS_ELEMENT_CAPABILITY = Attributes.createCombinable(DeviceBusElement.class, new AbstractDeviceBusElement() {
    }, list -> new AbstractDeviceBusElement() {
        @Override
        public Optional<Collection<DeviceBusElement>> getNeighbors() {
            final HashSet<DeviceBusElement> neighbors = new HashSet<>();
            for (final DeviceBusElement element : list) {
                final Optional<Collection<DeviceBusElement>> elementNeighbors = element.getNeighbors();
                if (elementNeighbors.isPresent()) {
                    neighbors.addAll(elementNeighbors.get());
                } else {
                    return Optional.empty();
                }
            }
            return Optional.of(neighbors);
        }
    });

    public static Attribute<DeviceBusController> DEVICE_BUS_CONTROLLER_CAPABILITY = Attributes.create(DeviceBusController.class);

//    public static Attribute<IEnergyStorage> ENERGY_STORAGE_CAPABILITY = null;

    public static Attribute<FixedFluidInvView> FLUID_HANDLER_CAPABILITY = FluidAttributes.FIXED_INV_VIEW;

    public static Attribute<FixedItemInvView> ITEM_HANDLER_CAPABILITY = ItemAttributes.FIXED_INV_VIEW;
}
