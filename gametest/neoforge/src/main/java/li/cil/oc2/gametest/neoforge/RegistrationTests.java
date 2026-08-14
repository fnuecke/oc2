/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RegistrationTests {
    @GameTest(template = TEMPLATE)
    public static void deviceTypesAreRegistered(final GameTestHelper helper) {
        assertNotNull(helper, DeviceTypes.MEMORY, "DeviceTypes.MEMORY");
        assertNotNull(helper, DeviceTypes.HARD_DRIVE, "DeviceTypes.HARD_DRIVE");
        assertNotNull(helper, DeviceTypes.FLASH_MEMORY, "DeviceTypes.FLASH_MEMORY");
        assertNotNull(helper, DeviceTypes.CARD, "DeviceTypes.CARD");
        assertNotNull(helper, DeviceTypes.ROBOT_MODULE, "DeviceTypes.ROBOT_MODULE");
        assertNotNull(helper, DeviceTypes.FLOPPY, "DeviceTypes.FLOPPY");
        assertNotNull(helper, DeviceTypes.NETWORK_TUNNEL, "DeviceTypes.NETWORK_TUNNEL");
        helper.succeed();
    }

    // ------------------------------------------------------------- //

    private RegistrationTests() {
    }
}
