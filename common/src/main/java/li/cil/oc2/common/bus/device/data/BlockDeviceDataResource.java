/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.data;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import net.minecraft.resources.ResourceLocation;

public interface BlockDeviceDataResource extends BlockDeviceData {
    ResourceLocation getLocation();
}
