/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.provider;

import li.cil.oc2.common.serial.BufferedSerialDevice;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.sedna.devicetree.DeviceTreeRegistry;
import li.cil.sedna.devicetree.provider.UART16550AProvider;

public final class DeviceTreeProviders {
    public static void initialize() {
        DeviceTreeRegistry.putProvider(SimpleFramebufferDevice.class, new SimpleFramebufferDeviceProvider());
        DeviceTreeRegistry.putProvider(BufferedSerialDevice.class, new UART16550AProvider());
    }
}
