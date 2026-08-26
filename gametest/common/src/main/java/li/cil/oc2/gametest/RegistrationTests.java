/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.DeviceTypes;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.TestSupport.assertNotNull;

public final class RegistrationTests {
    public static void deviceTypesAreRegistered(final GameTestHelper helper) {
        assertNotNull(helper, DeviceTypes.MEMORY.get(), "DeviceTypes.MEMORY.get()");
        assertNotNull(helper, DeviceTypes.HARD_DRIVE.get(), "DeviceTypes.HARD_DRIVE.get()");
        assertNotNull(helper, DeviceTypes.FLASH_MEMORY.get(), "DeviceTypes.FLASH_MEMORY.get()");
        assertNotNull(helper, DeviceTypes.CARD.get(), "DeviceTypes.CARD.get()");
        assertNotNull(helper, DeviceTypes.ROBOT_MODULE.get(), "DeviceTypes.ROBOT_MODULE.get()");
        assertNotNull(helper, DeviceTypes.FLOPPY.get(), "DeviceTypes.FLOPPY.get()");
        assertNotNull(helper, DeviceTypes.NETWORK_TUNNEL.get(), "DeviceTypes.NETWORK_TUNNEL.get()");
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private RegistrationTests() {
    }
}
