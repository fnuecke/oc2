/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.Device;

import java.util.List;

public interface DeviceWithAliases extends Device {
    List<String> getAliases();
}
