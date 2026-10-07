/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.ItemDevice;

import java.util.List;

public record TypeNameDevice(String name) implements DeviceWithAliases, ItemDevice {
    @Override
    public List<String> getAliases() {
        return List.of(name);
    }
}
