/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static li.cil.oc2.gametest.util.DeviceCalls.*;
import static li.cil.oc2.gametest.util.TestSupport.*;

public final class VanillaDeviceTests {
    private static final BlockPos POS = new BlockPos(20, WORK_Y, 2);

    // --------------------------------------------------------------------- //

    public static void signJoinsTheBus(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placeWithCable(helper, fakePlayer(helper));
        helper.setBlock(DEVICE_POS, Blocks.OAK_SIGN);

        helper.startSequence()
            .thenWaitUntil(() -> {
                final boolean hasRpcDevice = computer.devices().stream().anyMatch(device ->
                    device instanceof final RPCDevice rpc && rpc.getTypeNames().contains("sign"));
                final boolean hasIoDevice = computer.devices().stream().anyMatch(device ->
                    device instanceof final IODevice io && "SIGN".equals(io.getIOName()));
                if (!hasRpcDevice || !hasIoDevice) {
                    throw new GameTestAssertException("the sign is not on the bus: " + computer.describe());
                }
            })
            .thenSucceed();
    }

    public static void noteBlockKeepsItsDeviceWhenWritten(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placeWithCable(helper, fakePlayer(helper));
        helper.setBlock(DEVICE_POS, Blocks.NOTE_BLOCK);
        final Device[] device = new Device[1];

        helper.startSequence()
            .thenWaitUntil(() -> device[0] = computer.devices().stream()
                .filter(d -> d instanceof final RPCDevice rpc && rpc.getTypeNames().contains("note_block"))
                .findFirst()
                .orElseThrow(() -> new GameTestAssertException("the note block is not on the bus: " + computer.describe())))
            .thenExecute(() -> invokeRpc((RPCDevice) device[0], "setNote", 5))
            .thenIdle(10)
            .thenExecute(() -> assertTrue(helper, "the note block device was replaced after a write: " + computer.describe(),
                computer.devices().stream().anyMatch(d -> d == device[0])))
            .thenSucceed();
    }

    public static void signReadsAndWritesText(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.OAK_SIGN);
        final List<Device> devices = devicesAt(helper, POS);
        final IODevice io = ioDevice(devices, "SIGN");

        invokeRpc(devices, "setText", "front", new String[]{"Hello", "World"});
        assertArrayEquals(helper, "front text", new String[]{"Hello", "World", "", ""},
            (String[]) invokeRpc(devices, "getText", "front"));
        assertArrayEquals(helper, "back text", new String[]{"", "", "", ""},
            (String[]) invokeRpc(devices, "getText", "back"));
        assertValue(helper, "color", "black", invokeRpc(devices, "getColor", "front"));
        assertValue(helper, "glowing", false, invokeRpc(devices, "isGlowing", "front"));

        invokeRpc(devices, "setText", "back", new String[]{"@@@@@@@@@@@@"});
        assertFails(helper, "a line over 12 characters", () -> invokeRpc(devices, "setText", "front", new String[]{"0123456789abc"}));
        assertFails(helper, "a non-ASCII character", () -> invokeRpc(devices, "setText", "front", new String[]{"ä"}));
        assertFails(helper, "a formatting character", () -> invokeRpc(devices, "setText", "front", new String[]{"§cred"}));
        assertFails(helper, "a control character", () -> invokeRpc(devices, "setText", "front", new String[]{"a\tb"}));
        assertFails(helper, "five lines", () -> invokeRpc(devices, "setText", "front", new String[]{"", "", "", "", ""}));
        assertFails(helper, "an unknown side", () -> invokeRpc(devices, "setText", "top", new String[]{""}));

        invokeIo(io, 2, concat(new int[]{1, 3}, "via io"));
        assertValue(helper, "line written through io", "via io", ((String[]) invokeRpc(devices, "getText", "back"))[3]);
        assertValue(helper, "line read through io", "Hello", new String(invokeIo(io, 1, 0, 0), StandardCharsets.US_ASCII));
        assertEquals(helper, "color through io", 15, invokeIo(io, 3, 0)[0]);
        assertIoFails(helper, "a line over 12 characters through io", () -> invokeIo(io, 2, concat(new int[]{0, 0}, "0123456789abc")));
        assertIoFails(helper, "a fifth line through io", () -> invokeIo(io, 2, concat(new int[]{0, 4}, "x")));

        final SignBlockEntity sign = helper.getBlockEntity(POS);
        sign.setWaxed(true);
        assertValue(helper, "read-only", true, invokeRpc(devices, "isReadonly"));
        assertEquals(helper, "read-only through io", 1, invokeIo(io, 5)[0]);
        assertFails(helper, "writing a read-only sign", "sign is read-only", () -> invokeRpc(devices, "setText", "front", new String[]{"x"}));
        assertValue(helper, "text of the read-only sign", "Hello", ((String[]) invokeRpc(devices, "getText", "front"))[0]);

        helper.setBlock(POS, Blocks.OAK_HANGING_SIGN);
        final List<Device> hanging = devicesAt(helper, POS);
        invokeRpc(hanging, "setText", "front", new String[]{"~~~~~~~~"});
        assertFails(helper, "a line over 8 characters on a hanging sign", () -> invokeRpc(hanging, "setText", "front", new String[]{"123456789"}));

        helper.succeed();
    }

    public static void furnaceReportsProgress(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.FURNACE);
        final AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(POS);
        final List<Device> devices = devicesAt(helper, POS);
        final IODevice io = ioDevice(devices, "FRNACE");

        assertValue(helper, "burning while empty", false, invokeRpc(devices, "isBurning"));
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));

        helper.startSequence()
            .thenExecuteAfter(10, () -> {
                assertValue(helper, "burning", true, invokeRpc(devices, "isBurning"));
                assertEquals(helper, "burn duration of coal", 1600, (int) invokeRpc(devices, "getBurnDuration"));
                assertTrue(helper, "burn time counts down", (int) invokeRpc(devices, "getBurnTime") < 1600);
                assertTrue(helper, "cook time advances", (int) invokeRpc(devices, "getCookTime") > 0);
                assertEquals(helper, "total cook time", 200, (int) invokeRpc(devices, "getCookTimeTotal"));
                assertEquals(helper, "total cook time through io", 200, u16(invokeIo(io, 4)));
                assertEquals(helper, "burning through io", 1, invokeIo(io, 5)[0]);
            })
            .thenSucceed();
    }

    public static void brewingStandReportsFuel(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.BREWING_STAND);
        final BrewingStandBlockEntity stand = helper.getBlockEntity(POS);
        final List<Device> devices = devicesAt(helper, POS);
        stand.setItem(4, new ItemStack(Items.BLAZE_POWDER));

        helper.startSequence()
            .thenExecuteAfter(2, () -> {
                assertEquals(helper, "fuel", 20, (int) invokeRpc(devices, "getFuel"));
                assertEquals(helper, "brew time while idle", 0, (int) invokeRpc(devices, "getBrewTime"));
                assertEquals(helper, "fuel through io", 20, invokeIo(ioDevice(devices, "BREW"), 2)[0]);
            })
            .thenSucceed();
    }

    public static void beaconReportsEffects(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.BEACON);
        final List<Device> devices = devicesAt(helper, POS);
        final IODevice io = ioDevice(devices, "BEACON");

        assertEquals(helper, "levels without a pyramid", 0, (int) invokeRpc(devices, "getLevels"));
        assertValue(helper, "primary effect when unset", null, invokeRpc(devices, "getPrimaryEffect"));
        assertEquals(helper, "primary effect through io when unset", 0, invokeIo(io, 2).length);

        final BeaconBlockEntity beacon = helper.getBlockEntity(POS);
        final CompoundTag tag = new CompoundTag();
        tag.putString("primary_effect", "minecraft:speed");
        beacon.loadWithComponents(tag, helper.getLevel().registryAccess());
        assertValue(helper, "primary effect", "minecraft:speed", invokeRpc(devices, "getPrimaryEffect"));
        assertValue(helper, "primary effect through io", "minecraft:speed", new String(invokeIo(io, 2), StandardCharsets.US_ASCII));
        assertValue(helper, "secondary effect when unset", null, invokeRpc(devices, "getSecondaryEffect"));

        helper.succeed();
    }

    public static void spawnerReportsEntityType(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.SPAWNER);
        final SpawnerBlockEntity spawner = helper.getBlockEntity(POS);
        spawner.setEntityId(EntityType.ZOMBIE, helper.getLevel().getRandom());
        final List<Device> devices = devicesAt(helper, POS);

        assertValue(helper, "entity type", "minecraft:zombie", invokeRpc(devices, "getEntityType"));
        assertValue(helper, "entity type through io", "minecraft:zombie",
            new String(invokeIo(ioDevice(devices, "SPAWNR"), 1), StandardCharsets.US_ASCII));

        helper.succeed();
    }

    public static void comparatorSwitchesMode(final GameTestHelper helper) {
        helper.setBlock(POS.north(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(POS, Blocks.COMPARATOR);
        final List<Device> devices = devicesAt(helper, POS);
        final IODevice io = ioDevice(devices, "CMPRTR");

        assertValue(helper, "initial mode", "compare", invokeRpc(devices, "getMode"));
        invokeRpc(devices, "setMode", "subtract");
        assertTrue(helper, "block state after switching",
            helper.getBlockState(POS).getValue(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT);
        assertEquals(helper, "mode through io", 1, invokeIo(io, 2)[0]);
        invokeIo(io, 3, 0);
        assertValue(helper, "mode after switching through io", "compare", invokeRpc(devices, "getMode"));
        assertFails(helper, "an unknown mode", () -> invokeRpc(devices, "setMode", "add"));
        assertIoFails(helper, "an unknown mode through io", () -> invokeIo(io, 3, 2));

        helper.startSequence()
            .thenExecuteAfter(4, () ->
                assertEquals(helper, "output signal", 15, (int) invokeRpc(devices, "getOutputSignal")))
            .thenSucceed();
    }

    public static void jukeboxReportsSong(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.JUKEBOX);
        final List<Device> devices = devicesAt(helper, POS);
        final IODevice io = ioDevice(devices, "JUKEBX");

        assertValue(helper, "playing while empty", false, invokeRpc(devices, "isPlaying"));
        assertValue(helper, "song while empty", null, invokeRpc(devices, "getSong"));

        final JukeboxBlockEntity jukebox = helper.getBlockEntity(POS);
        jukebox.setTheItem(new ItemStack(Items.MUSIC_DISC_CAT));

        helper.startSequence()
            .thenExecuteAfter(5, () -> {
                assertValue(helper, "playing", true, invokeRpc(devices, "isPlaying"));
                assertValue(helper, "song", "minecraft:cat", invokeRpc(devices, "getSong"));
                assertEquals(helper, "song length", 185 * 20, (int) invokeRpc(devices, "getSongLength"));
                assertTrue(helper, "elapsed", (int) invokeRpc(devices, "getElapsed") > 0);
                assertValue(helper, "song through io", "minecraft:cat", new String(invokeIo(io, 2), StandardCharsets.US_ASCII));
                assertEquals(helper, "song length through io", 185 * 20, u16(invokeIo(io, 3)));
            })
            .thenSucceed();
    }

    public static void lecternReportsPages(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.LECTERN);
        final List<Device> devices = devicesAt(helper, POS);
        final IODevice io = ioDevice(devices, "LECTRN");

        assertEquals(helper, "page count without a book", 0, (int) invokeRpc(devices, "getPageCount"));

        final ItemStack book = new ItemStack(Items.WRITABLE_BOOK);
        book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(
            List.of(Filterable.passThrough("a"), Filterable.passThrough("b"), Filterable.passThrough("c"))));
        final LecternBlockEntity lectern = helper.getBlockEntity(POS);
        lectern.setBook(book);

        assertEquals(helper, "page count", 3, (int) invokeRpc(devices, "getPageCount"));
        assertEquals(helper, "page count through io", 3, invokeIo(io, 2)[0]);
        assertEquals(helper, "page", 0, (int) invokeRpc(devices, "getPage"));
        assertEquals(helper, "page through io", 0, invokeIo(io, 1)[0]);

        helper.succeed();
    }

    public static void crafterTogglesSlotsAndPredictsResult(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.CRAFTER);
        final CrafterBlockEntity crafter = helper.getBlockEntity(POS);
        final List<Device> devices = devicesAt(helper, POS);
        final IODevice io = ioDevice(devices, "CRAFTR");

        invokeRpc(devices, "setSlotEnabled", 0, false);
        assertValue(helper, "disabled slot", false, invokeRpc(devices, "isSlotEnabled", 0));
        assertEquals(helper, "disabled slot through io", 0, invokeIo(io, 1, 0)[0]);
        invokeIo(io, 2, 0, 1);
        assertValue(helper, "slot enabled through io", true, invokeRpc(devices, "isSlotEnabled", 0));

        crafter.setItem(4, new ItemStack(Items.OAK_LOG));
        assertFails(helper, "disabling a filled slot", () -> invokeRpc(devices, "setSlotEnabled", 4, false));
        invokeRpc(devices, "setSlotEnabled", 4, true);
        assertFails(helper, "a slot past the grid", () -> invokeRpc(devices, "isSlotEnabled", 9));

        final ItemStack result = (ItemStack) Objects.requireNonNull(invokeRpc(devices, "getResult"));
        assertTrue(helper, "result is planks, was " + result, result.is(Items.OAK_PLANKS) && result.getCount() == 4);
        final byte[] ioResult = invokeIo(io, 3);
        assertEquals(helper, "result item through io", BuiltInRegistries.ITEM.getId(Items.OAK_PLANKS), u16(ioResult));
        assertEquals(helper, "result count through io", 4, ioResult[2]);

        helper.succeed();
    }

    public static void beehiveReportsHoney(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 5));
        final List<Device> devices = devicesAt(helper, POS);

        assertEquals(helper, "honey level", 5, (int) invokeRpc(devices, "getHoneyLevel"));
        assertEquals(helper, "bee count", 0, (int) invokeRpc(devices, "getBeeCount"));
        assertValue(helper, "sedated", false, invokeRpc(devices, "isSedated"));
        assertEquals(helper, "honey level through io", 5, invokeIo(ioDevice(devices, "BHIVE"), 1)[0]);

        helper.succeed();
    }

    public static void composterReportsLevel(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, 3));
        final List<Device> devices = devicesAt(helper, POS);

        assertEquals(helper, "level", 3, (int) invokeRpc(devices, "getLevel"));
        assertEquals(helper, "level through io", 3, invokeIo(ioDevice(devices, "CMPSTR"), 1)[0]);

        helper.succeed();
    }

    public static void noteBlockTunes(final GameTestHelper helper) {
        helper.setBlock(POS, Blocks.NOTE_BLOCK);
        final List<Device> devices = devicesAt(helper, POS);

        invokeRpc(devices, "setNote", 7);
        assertEquals(helper, "note", 7, helper.getBlockState(POS).getValue(NoteBlock.NOTE));
        assertEquals(helper, "note read back", 7, (int) invokeRpc(devices, "getNote"));

        final IODevice io = ioDevice(devices, "NOTE");
        invokeIo(io, 2, 12);
        assertEquals(helper, "note through io", 12, invokeIo(io, 1)[0]);
        invokeIo(io, 4, concat(new int[0], "bell"));
        assertValue(helper, "instrument through io", "bell", new String(invokeIo(io, 3), StandardCharsets.US_ASCII));
        assertIoFails(helper, "a note past 24 through io", () -> invokeIo(io, 2, 25));

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static void assertFails(final GameTestHelper helper, final String what, final Runnable action) {
        assertFails(helper, what, "", action);
    }

    private static void assertFails(final GameTestHelper helper, final String what, final String reason, final Runnable action) {
        try {
            action.run();
        } catch (final GameTestAssertException e) {
            if (e.getMessage().contains(" threw: ") && e.getMessage().contains(reason)) {
                return;
            }
            throw e;
        }
        throw failure(helper, what + " should have been rejected");
    }

    private static void assertIoFails(final GameTestHelper helper, final String what, final Runnable action) {
        try {
            action.run();
        } catch (final IllegalArgumentException e) {
            return;
        } catch (final GameTestAssertException e) {
            if (e.getMessage().contains(" failed: java.lang.IllegalStateException")) {
                return;
            }
            throw e;
        }
        throw failure(helper, what + " should have been rejected");
    }

    // --------------------------------------------------------------------- //

    private VanillaDeviceTests() {
    }
}
