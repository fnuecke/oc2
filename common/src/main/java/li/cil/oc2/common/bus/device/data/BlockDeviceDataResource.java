/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import net.minecraft.resources.Identifier;

public interface BlockDeviceDataResource extends BlockDeviceData {
    Identifier getLocation();
}
