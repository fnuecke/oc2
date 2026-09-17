/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.provider;

import li.cil.oc2.common.bus.IODeviceBusAdapter;
import li.cil.oc2.common.serial.BufferedSerialDevice;
import li.cil.sedna.api.device.bus.DeviceClass;
import li.cil.sedna.api.device.bus.DeviceDescription;
import li.cil.sedna.api.device.bus.DeviceDescriptionProvider;
import li.cil.sedna.device.bus.DeviceDescriptionRegistry;
import li.cil.sedna.device.bus.DevicePortRegistry;

public final class DeviceDescriptionProviders {
    public static void initialize() {
        DeviceDescriptionRegistry.putProvider(IODeviceBusAdapter.class,
            device -> ((IODeviceBusAdapter) device).getDescriptions());
        DevicePortRegistry.putWidth(IODeviceBusAdapter.class, IODeviceBusAdapter.LENGTH);

        DeviceDescriptionRegistry.putProvider(BufferedSerialDevice.class,
            DeviceDescriptionProvider.of(new DeviceDescription(DeviceClass.CHARACTER, "UART")));
        DevicePortRegistry.putWidth(BufferedSerialDevice.class, BufferedSerialDevice.PORT_COUNT);
    }
}
