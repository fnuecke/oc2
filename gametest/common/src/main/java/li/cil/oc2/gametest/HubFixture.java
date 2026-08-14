/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.blockentity.NetworkHubBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;

import static li.cil.oc2.gametest.TestSupport.fakePlayer;

public final class HubFixture {
    private final GameTestHelper helper;
    private final BlockPos pos;

    // ------------------------------------------------------------- //

    public static HubFixture place(final GameTestHelper helper, final BlockPos pos) {
        return place(helper, fakePlayer(helper), pos);
    }

    public static HubFixture place(final GameTestHelper helper, final Player player, final BlockPos pos) {
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_HUB.get()), pos);
        return new HubFixture(helper, pos);
    }

    // ------------------------------------------------------------- //

    public BlockPos pos() {
        return pos;
    }

    public NetworkHubBlockEntity blockEntity() {
        return helper.getBlockEntity(pos);
    }

    /**
     * Private state, but the only observation point that can witness a delivery without a guest to ask.
     */
    public int framesReceived() {
        try {
            final Field field = NetworkHubBlockEntity.class.getDeclaredField("frameCount");
            field.setAccessible(true);
            return (Integer) field.get(blockEntity());
        } catch (final ReflectiveOperationException e) {
            throw new GameTestAssertException("could not read the hub's frame counter: " + e);
        }
    }

    // ------------------------------------------------------------- //

    private HubFixture(final GameTestHelper helper, final BlockPos pos) {
        this.helper = helper;
        this.pos = pos;
    }
}
