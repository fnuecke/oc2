/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

import javax.annotation.Nullable;

public final class TestSupport {
    public static final String MOD_ID = "oc2gametest";
    public static final String TEMPLATE = "empty";
    public static final BlockPos COMPUTER_POS = new BlockPos(4, 1, 4);
    public static final BlockPos CABLE_POS = new BlockPos(5, 1, 4);
    public static final BlockPos DEVICE_POS = new BlockPos(6, 1, 4);

    public static void assertNotNull(final GameTestHelper helper, @Nullable final Object value, final String what) {
        if (value == null) {
            throw new GameTestAssertException(what + " is null");
        }
    }

    private TestSupport() {
    }
}
