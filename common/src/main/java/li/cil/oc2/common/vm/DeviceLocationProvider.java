/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.vm.VMDevice;

@FunctionalInterface
public interface DeviceLocationProvider {
    DeviceLocation getDeviceLocation(VMDevice device);
}
