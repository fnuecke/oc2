/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static li.cil.oc2.gametest.TestSupport.place;
import static li.cil.oc2.gametest.TestSupport.useOn;

public final class BusCables {
    public static void placeCable(final GameTestHelper helper, final Player player, final BlockPos pos) {
        place(helper, player, new ItemStack(Items.BUS_CABLE.get()), pos);
    }

    public static void placeInterface(final GameTestHelper helper, final Player player, final BlockPos cable, final Direction face) {
        useOn(helper, player, new ItemStack(Items.BUS_INTERFACE.get()), cable, face);
    }

    public static void placeCableWithInterfaces(final GameTestHelper helper, final Player player, final BlockPos pos, final Direction... faces) {
        placeCable(helper, player, pos);
        for (final Direction face : faces) {
            placeInterface(helper, player, pos, face);
        }
    }

    public static void placeCableRun(final GameTestHelper helper, final Player player, final BlockPos from, final BlockPos to) {
        for (int x = from.getX(); x <= to.getX(); x++) {
            placeCable(helper, player, new BlockPos(x, from.getY(), from.getZ()));
        }
    }

    // ------------------------------------------------------------- //

    private BusCables() {
    }
}
