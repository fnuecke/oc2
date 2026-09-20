/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.blockentity.TransposerBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.util.BusCables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import static li.cil.oc2.gametest.util.DeviceCalls.invokeIo;
import static li.cil.oc2.gametest.util.DeviceCalls.invokeRpc;
import static li.cil.oc2.gametest.util.TestSupport.*;
import static net.minecraft.world.item.Items.REDSTONE;

public final class TransposerTests {
    private static final BlockPos TRANSPOSER_POS = new BlockPos(8, WORK_Y, 2);
    private static final BlockPos SOURCE_POS = TRANSPOSER_POS.west();
    private static final BlockPos TARGET_POS = TRANSPOSER_POS.east();

    private static final int UP = 1;
    private static final int WEST = 4;
    private static final int EAST = 5;
    private static final int CHEST_SLOTS = 27;

    private static final int GET_SLOT_COUNT_CODE = 1;
    private static final int GET_SLOTS_CODE = 2;
    private static final int MOVE_ITEMS_CODE = 6;

    public static void transposerJoinsTheBus(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        BusCables.placeCableWithInterfaces(helper, player, CABLE_POS, Direction.WEST, Direction.EAST);
        place(helper, player, new ItemStack(Items.TRANSPOSER.get()), DEVICE_POS);

        helper.startSequence()
            .thenWaitUntil(() -> {
                final boolean hasRpcDevice = computer.devices().stream().anyMatch(device ->
                    device instanceof final RPCDevice rpc && rpc.getTypeNames().contains("transposer"));
                final boolean hasIoDevice = computer.devices().stream().anyMatch(device ->
                    device instanceof final IODevice io && "TRANSP".equals(io.getIOName()));
                if (!hasRpcDevice || !hasIoDevice) {
                    throw new GameTestAssertException("the transposer is not on the bus: " + computer.describe());
                }
            })
            .thenSucceed();
    }

    public static void transposerMovesItemsBetweenInventories(final GameTestHelper helper) {
        final TransposerBlockEntity transposer = placeTransposerBetweenChests(helper);
        final ChestBlockEntity source = helper.getBlockEntity(SOURCE_POS);
        final ChestBlockEntity target = helper.getBlockEntity(TARGET_POS);
        source.setItem(0, new ItemStack(REDSTONE, 42));
        target.setItem(0, new ItemStack(REDSTONE, 60));

        assertEquals(helper, "slot count of the source chest", CHEST_SLOTS, transposer.getItemSlotCount(Side.WEST));
        assertEquals(helper, "slot count of an empty side", 0, transposer.getItemSlotCount(Side.UP));
        assertEquals(helper, "stack in the source slot", 42, transposer.getItemStackInSlot(Side.WEST, 0).getCount());

        assertEquals(helper, "moving into a nearly full slot", 4, transposer.moveItems(Side.WEST, 0, Side.EAST, 0, 10));
        assertEquals(helper, "source after the partial move", 38, source.getItem(0).getCount());
        assertEquals(helper, "target after the partial move", 64, target.getItem(0).getCount());

        assertEquals(helper, "moving into an empty slot", 10, transposer.moveItems(Side.WEST, 0, Side.EAST, 1, 10));
        assertEquals(helper, "source after the full move", 28, source.getItem(0).getCount());
        assertEquals(helper, "target after the full move", 10, target.getItem(1).getCount());

        assertEquals(helper, "moving from an empty slot", 0, transposer.moveItems(Side.WEST, 1, Side.EAST, 2, 10));
        assertEquals(helper, "moving nothing", 0, transposer.moveItems(Side.WEST, 0, Side.EAST, 2, 0));
        assertEquals(helper, "moving a slot onto itself", 0, transposer.moveItems(Side.WEST, 0, Side.WEST, 0, 10));
        assertEquals(helper, "source after the no-op moves", 28, source.getItem(0).getCount());

        assertEquals(helper, "moving within one inventory", 8, transposer.moveItems(Side.WEST, 0, Side.WEST, 3, 8));
        assertEquals(helper, "source slot 3 after the in-place move", 8, source.getItem(3).getCount());

        assertThrows(helper, "a side without an inventory", () -> transposer.moveItems(Side.UP, 0, Side.EAST, 0, 1));
        assertThrows(helper, "a slot the inventory does not have", () -> transposer.moveItems(Side.WEST, CHEST_SLOTS, Side.EAST, 0, 1));
        assertThrows(helper, "reading a slot the inventory does not have", () -> transposer.getItemStackInSlot(Side.EAST, CHEST_SLOTS));

        final Object movedByRpc = invokeRpc(new ObjectDevice(transposer), "moveItems", "west", 0, "east", 4, 5);
        assertNotNull(helper, movedByRpc, "items moved through the RPC layer");
        assertEquals(helper, "items moved through the RPC layer", 5, ((Number) movedByRpc).longValue());
        assertEquals(helper, "target slot 4 after the RPC move", 5, target.getItem(4).getCount());

        helper.succeed();
    }

    public static void transposerMovesItemsViaMidLevelApi(final GameTestHelper helper) {
        final TransposerBlockEntity transposer = placeTransposerBetweenChests(helper);
        final ChestBlockEntity source = helper.getBlockEntity(SOURCE_POS);
        final ChestBlockEntity target = helper.getBlockEntity(TARGET_POS);
        source.setItem(0, new ItemStack(REDSTONE, 42));

        final ObjectDevice device = new ObjectDevice(transposer);
        assertTrue(helper, "device exposes the TRANSP mid-level API", "TRANSP".equals(device.getIOName()));

        final byte[] slotCount = invokeIo(device, GET_SLOT_COUNT_CODE, WEST);
        assertEquals(helper, "slot count reply length", 1, slotCount.length);
        assertEquals(helper, "slot count of the source chest", CHEST_SLOTS, slotCount[0] & 0xFF);

        final byte[] moved = invokeIo(device, MOVE_ITEMS_CODE, WEST, 0, EAST, 0, 10);
        assertEquals(helper, "move reply length", 1, moved.length);
        assertEquals(helper, "items moved", 10, moved[0] & 0xFF);
        assertEquals(helper, "source after the move", 32, source.getItem(0).getCount());
        assertEquals(helper, "target after the move", 10, target.getItem(0).getCount());

        final byte[] slots = invokeIo(device, GET_SLOTS_CODE, EAST, 0, 1);
        assertEquals(helper, "slot record length", 4, slots.length);
        final int id = (slots[0] & 0xFF) | ((slots[1] & 0xFF) << 8);
        assertEquals(helper, "item id of the moved stack", BuiltInRegistries.ITEM.getId(REDSTONE), id);
        assertEquals(helper, "count of the moved stack", 10, slots[2] & 0xFF);

        assertThrows(helper, "a side without an inventory", () -> invokeIo(device, MOVE_ITEMS_CODE, UP, 0, EAST, 0, 1));

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static TransposerBlockEntity placeTransposerBetweenChests(final GameTestHelper helper) {
        helper.setBlock(TRANSPOSER_POS, li.cil.oc2.common.block.Blocks.TRANSPOSER.get());
        helper.setBlock(SOURCE_POS, Blocks.CHEST);
        helper.setBlock(TARGET_POS, Blocks.CHEST);
        return helper.getBlockEntity(TRANSPOSER_POS);
    }

    private TransposerTests() {
    }
}
