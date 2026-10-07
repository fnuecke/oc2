/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.energy.EnergyHandler;

import java.io.IOException;

@RPCDeviceDescription(typeName = "energy_storage", description = """
    Provided by any energy storage connected through a [bus interface](../block/bus_interface.md), such as a [charger](../block/charger.md).""")
@IODeviceDescription(name = "ENERGY", description = """
    Amounts are four bytes, low byte first. Amounts past `0xFFFFFFFF` read as `0xFFFFFFFF`.""")
public final class EnergyHandlerDevice extends IdentityProxy<EnergyHandler> {
    private static final int GET_ENERGY_STORED_CODE = 1;
    private static final int GET_MAX_ENERGY_STORED_CODE = 2;
    private static final int CAN_EXTRACT_ENERGY_CODE = 3;
    private static final int CAN_RECEIVE_ENERGY_CODE = 4;

    private static final long MAX_IO_AMOUNT = 0xFFFFFFFFL;

    // --------------------------------------------------------------------- //

    public EnergyHandlerDevice(final EnergyHandler identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how much energy is currently stored.",
        returnValueDescription = "the stored amount of energy.")
    public int getEnergyStored() {
        return (int) identity.getEnergyStored();
    }

    @Callback(description = "Gets how much energy can be stored at most.",
        returnValueDescription = "the capacity of the storage.")
    public int getMaxEnergyStored() {
        return (int) identity.getMaxEnergyStored();
    }

    @Callback(description = "Gets whether energy can be taken out of the storage.",
        returnValueDescription = "whether the storage allows extracting energy.")
    public boolean canExtractEnergy() {
        return identity.canExtract();
    }

    @Callback(description = "Gets whether energy can be put into the storage.",
        returnValueDescription = "whether the storage allows receiving energy.")
    public boolean canReceiveEnergy() {
        return identity.canReceive();
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_ENERGY_STORED_CODE,
        description = "Gets how much energy is currently stored.",
        resultsDescription = "four bytes, the stored amount of energy.")
    public void getEnergyStored(final IOOutputStream results) throws IOException {
        results.writeU32(Math.min(identity.getEnergyStored(), MAX_IO_AMOUNT));
    }

    @IOCallback(value = GET_MAX_ENERGY_STORED_CODE,
        description = "Gets how much energy can be stored at most.",
        resultsDescription = "four bytes, the capacity of the storage.")
    public void getMaxEnergyStored(final IOOutputStream results) throws IOException {
        results.writeU32(Math.min(identity.getMaxEnergyStored(), MAX_IO_AMOUNT));
    }

    @IOCallback(value = CAN_EXTRACT_ENERGY_CODE,
        description = "Gets whether energy can be taken out of the storage.",
        resultsDescription = "one byte, `1` if the storage allows extracting energy, `0` otherwise.")
    public void canExtractEnergy(final IOOutputStream results) throws IOException {
        results.writeU8(identity.canExtract() ? 1 : 0);
    }

    @IOCallback(value = CAN_RECEIVE_ENERGY_CODE,
        description = "Gets whether energy can be put into the storage.",
        resultsDescription = "one byte, `1` if the storage allows receiving energy, `0` otherwise.")
    public void canReceiveEnergy(final IOOutputStream results) throws IOException {
        results.writeU8(identity.canReceive() ? 1 : 0);
    }
}
