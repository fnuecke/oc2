package com.example.blockentity;

import li.cil.oc2.api.bus.device.object.Callback;

// This being a record means equals() matches devices from repeated queries for the same
// block entity; otherwise the bus would remove and re-add the device on every scan.
public record CounterDevice(CounterBlockEntity blockEntity) {
    @Callback(description = "Returns how often the block was used.")
    public int getCount() {
        return blockEntity.getCount();
    }

    @Callback(description = "Resets the count to zero.")
    public void reset() {
        blockEntity.reset();
    }
}
