/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.provider;

import li.cil.oc2.common.bus.IODeviceBusAdapter;
import li.cil.sedna.device.bus.DeviceDescriptionRegistry;
import li.cil.sedna.device.bus.DevicePortRegistry;

public final class DeviceDescriptionProviders {
    public static void initialize() {
        DeviceDescriptionRegistry.putProvider(IODeviceBusAdapter.class,
            device -> ((IODeviceBusAdapter) device).getDescriptions());
        DevicePortRegistry.putWidth(IODeviceBusAdapter.class, IODeviceBusAdapter.LENGTH);
    }
}
