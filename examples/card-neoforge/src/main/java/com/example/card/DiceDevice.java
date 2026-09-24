package com.example.card;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IODeviceDescription;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@RPCDeviceDescription(typeNames = "dice", description = "Rolls dice.")
@IODeviceDescription(name = "DICE", description = "Rolls dice.")
public final class DiceDevice {
    private static final int ROLL_CODE = 0;

    @Callback(synchronize = false,
        description = "Rolls a die with the given number of sides.",
        returnValueDescription = "the result.")
    public int roll(@Parameter(value = "sides", description = "the number of sides.") final int sides) {
        if (sides < 1) {
            throw new IllegalArgumentException("sides must be positive");
        }
        return 1 + ThreadLocalRandom.current().nextInt(sides);
    }

    @IOCallback(value = ROLL_CODE, synchronize = false,
        description = "Rolls a die with the given number of sides.",
        argumentsDescription = "one byte, the number of sides.",
        resultsDescription = "one byte, the result.")
    public void roll(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(roll(arguments.readU8()));
    }
}
