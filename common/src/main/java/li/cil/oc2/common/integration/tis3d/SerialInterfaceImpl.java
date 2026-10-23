/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.tis3d;

import li.cil.oc2.common.serial.SerialPort;
import li.cil.tis3d.api.serial.SerialInterface;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public final class SerialInterfaceImpl implements SerialInterface {
    private final SerialPort port;

    // --------------------------------------------------------------------- //

    SerialInterfaceImpl(final SerialPort port) {
        this.port = port;
    }

    // --------------------------------------------------------------------- //

    boolean matchesPort(@Nullable final SerialPort port) {
        return this.port == port;
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean canWrite() {
        return port.canWrite();
    }

    @Override
    public void write(final short value) {
        port.write(new byte[]{(byte) value});
    }

    @Override
    public boolean canRead() {
        return port.available() > 0;
    }

    @Override
    public short peek() {
        return (short) Math.max(0, port.peek());
    }

    @Override
    public void skip() {
        port.skip();
    }

    @Override
    public void reset() {
        port.clear();
    }

    @Override
    public void save(final ValueOutput output) {
        port.saveConfiguration(output);
    }

    @Override
    public void load(final ValueInput input) {
        port.loadConfiguration(input);
    }
}
