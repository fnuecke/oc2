/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.Hardware;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.*;

@ForEachTest(groups = "openos2")
public final class OpenOs2Tests {
    private static final int OPEN_OS2_BOOT_TIMEOUT_TICKS = 200;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE_ID, timeoutTicks = OPEN_OS2_BOOT_TIMEOUT_TICKS)
    @TestHolder
    public static void openOs2BootsToShellOnSmallestHardware(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> Hardware.installOpenOs2(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> {
                final String text = computer.screen();
                if (text.contains("sedna-fw:") || text.contains("kernel trap")) {
                    throw new GameTestAssertException(Component.literal("OpenOS II did not boot; screen:\n" + text), 0);
                }
                if (!text.contains("on OpenOS II") || !text.contains("/ # ")) {
                    throw new GameTestAssertException(Component.literal("no shell prompt yet; screen:\n" + text), 0);
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private OpenOs2Tests() {
    }
}
