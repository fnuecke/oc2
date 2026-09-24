/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.serial.BufferedSerialDevice;
import li.cil.oc2.common.serial.SerialEndpoint;
import li.cil.oc2.common.serial.SerialFrame;
import li.cil.oc2.common.serial.SerialPort;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public final class SerialPeripheral implements IPeripheral {
    public static final String TYPE = "oc2_serial_interface";
    public static final String DATA_EVENT = "oc2_serial_data";

    // --------------------------------------------------------------------- //

    private final NetworkConnectorBlockEntity connector;
    private final SerialPort port;
    private final AttachedComputerSet computers = new AttachedComputerSet();

    // --------------------------------------------------------------------- //

    SerialPeripheral(final NetworkConnectorBlockEntity connector, final SerialPort port) {
        this.connector = connector;
        this.port = port;
    }

    // --------------------------------------------------------------------- //

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Object getTarget() {
        return connector;
    }

    @Override
    public boolean equals(@Nullable final IPeripheral other) {
        return other instanceof final SerialPeripheral peripheral && peripheral.port == port;
    }

    @Override
    public void attach(final IComputerAccess computer) {
        computers.add(computer);
        port.setListener(this::queueDataEvent);
    }

    @Override
    public void detach(final IComputerAccess computer) {
        computers.remove(computer);
        if (!computers.hasComputers()) {
            port.setListener(null);
        }
    }

    // --------------------------------------------------------------------- //

    @LuaFunction
    public int write(final IArguments arguments) throws LuaException {
        final ByteBuffer buffer = arguments.getBytes(0);
        final byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return port.write(data);
    }

    @LuaFunction
    public MethodResult read(final IArguments arguments) throws LuaException {
        final short[] values = port.read(arguments.optInt(0).orElse(Integer.MAX_VALUE));
        if (values.length == 0) {
            return MethodResult.of();
        }

        final byte[] data = new byte[values.length];
        int noiseCount = 0;
        for (int i = 0; i < values.length; i++) {
            data[i] = (byte) values[i];
            if ((values[i] & BufferedSerialDevice.FRAME_ERROR_FLAG) != 0) {
                noiseCount++;
            }
        }
        return MethodResult.of(data, noiseCount);
    }

    @LuaFunction
    public int available() {
        return port.available();
    }

    @LuaFunction
    public void clear() {
        port.clear();
    }

    @LuaFunction
    public int getBaudRate() {
        return port.getBaudRate();
    }

    @LuaFunction(mainThread = true)
    public void setBaudRate(final int baudRate) throws LuaException {
        if (baudRate < SerialEndpoint.MIN_BAUD_RATE || baudRate > SerialEndpoint.MAX_BAUD_RATE) {
            throw new LuaException("Baud rate out of range [" + SerialEndpoint.MIN_BAUD_RATE + ", " + SerialEndpoint.MAX_BAUD_RATE + "]");
        }
        port.setConfiguration(port.getAddress(), baudRate);
    }

    @LuaFunction
    public int getAddress() {
        return port.getAddress();
    }

    @LuaFunction(mainThread = true)
    public void setAddress(final int address) throws LuaException {
        if (address < 0 || address > SerialFrame.MAX_ADDRESS) {
            throw new LuaException("Address out of range [0, " + SerialFrame.MAX_ADDRESS + "]");
        }
        port.setConfiguration(address, port.getBaudRate());
    }

    @LuaFunction
    public int getOverrunCount() {
        return port.getOverrunCount();
    }

    @LuaFunction
    public int getRxErrorCount() {
        return port.getRxErrorCount();
    }

    @LuaFunction
    public int getTxErrorCount() {
        return port.getTxErrorCount();
    }

    @LuaFunction
    public int getNoiseCount() {
        return port.getNoiseCount();
    }

    // --------------------------------------------------------------------- //

    private void queueDataEvent() {
        computers.forEach(computer -> computer.queueEvent(DATA_EVENT, computer.getAttachmentName()));
    }
}
