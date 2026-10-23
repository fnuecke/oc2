/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.item.Items;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static li.cil.oc2.gametest.util.TestSupport.DEVICE_POS;
import static li.cil.oc2.gametest.util.TestSupport.assertValue;

public final class BusCablePickTests {
    public static void picksInterfaceWhenLookingAtInterface(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.BUS_CABLE.get());
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        BusCableBlock.addInterface(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), Direction.NORTH);

        assertPicked(helper, Direction.NORTH, Items.BUS_INTERFACE.get());
    }

    public static void picksCableWhenLookingAtCable(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.BUS_CABLE.get());
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        BusCableBlock.addInterface(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), Direction.NORTH);

        assertPicked(helper, Direction.SOUTH, Items.BUS_CABLE.get());
    }

    public static void picksFacade(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.BUS_CABLE.get());
        helper.getBlockEntity(DEVICE_POS, BusCableBlockEntity.class).setFacade(new ItemStack(net.minecraft.world.item.Items.STONE));

        assertPicked(helper, Direction.NORTH, net.minecraft.world.item.Items.STONE);
    }

    // --------------------------------------------------------------------- //

    @SuppressWarnings("removal")
    private static void assertPicked(final GameTestHelper helper, final Direction side, final Item expected) {
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final Vec3 eye = Vec3.atCenterOf(pos).relative(side, 1.5);

        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = true;
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));

        player.connection.handlePickItemFromBlock(new ServerboundPickItemFromBlockPacket(pos, false));
        final Item picked = player.getMainHandItem().getItem();

        helper.getLevel().getServer().getPlayerList().remove(player);

        assertValue(helper, "picked item", expected, picked);
        helper.succeed();
    }

    private BusCablePickTests() {
    }
}
