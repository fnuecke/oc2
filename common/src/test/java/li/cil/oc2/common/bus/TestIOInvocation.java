/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.io.IOInvocation;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.api.bus.device.object.IOCallbacks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class TestIOInvocation implements IOInvocation {
    public final ByteArrayOutputStream results = new ByteArrayOutputStream();
    public int energy;
    private final InputStream arguments;

    public TestIOInvocation(final byte[] arguments) {
        this(arguments, Integer.MAX_VALUE);
    }

    public TestIOInvocation(final byte[] arguments, final int energy) {
        this.arguments = new ByteArrayInputStream(arguments);
        this.energy = energy;
    }

    @Override
    public InputStream getInput() {
        return arguments;
    }

    @Override
    public OutputStream getOutput() {
        return results;
    }

    @Override
    public boolean consumeEnergy(final int amount) {
        if (amount > energy) {
            return false;
        }
        energy -= amount;
        return true;
    }

    // --------------------------------------------------------------------- //

    public static IOMethod method(final Object target, final int code) {
        return IOCallbacks.collectMethods(target).stream()
            .filter(m -> m.getCode() == code)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no function with code " + code));
    }

    public static byte[] invoke(final IOMethod method, final byte[] arguments) throws Throwable {
        final TestIOInvocation invocation = new TestIOInvocation(arguments);
        method.invoke(invocation);
        return invocation.results.toByteArray();
    }

    public static byte[] bytes(final int... values) {
        final byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }
}
