/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.inventory.ItemHandler;

import java.util.Optional;

public interface VMItemStackHandlers {
    Optional<ItemHandler> getItemHandler(DeviceType deviceType);

    boolean isEmpty();

    void exportDeviceDataToItemStacks();
}
