package com.example.card;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;

import java.util.Optional;

public final class DiceCardDeviceProvider implements ItemDeviceProvider {
    @Override
    public Optional<ItemDevice> getDevice(final ItemDeviceQuery query) {
        if (!query.getItemStack().is(ExampleMod.DICE_CARD)) {
            return Optional.empty();
        }
        return Optional.of(new ObjectDevice(new DiceDevice()));
    }
}
