/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataClientView;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.FlashMemoryItem;
import li.cil.oc2.common.item.FloppyItem;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.item.Items;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "datapack_data")
public final class DatapackDataTests {
    private static final Identifier FIRMWARE =
        Identifier.fromNamespaceAndPath(MOD_ID, "block_devices/flash/test_firmware.bin");
    private static final Identifier FLOPPY =
        Identifier.fromNamespaceAndPath(MOD_ID, "block_devices/floppy/test_floppy.bin");
    private static final Identifier HDD =
        Identifier.fromNamespaceAndPath(MOD_ID, "block_devices/hdd/test_hdd.bin");

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void firmwareCanComeFromADatapack(final GameTestHelper helper) {
        final BlockDeviceData data = BlockDeviceDataRegistry.getValue(FIRMWARE);
        if (data == null) {
            throw new GameTestAssertException(Component.literal("datapack firmware did not resolve through the firmware registry"), 0);
        }

        if (!FIRMWARE.equals(BlockDeviceDataRegistry.getKey(data))) {
            throw new GameTestAssertException(Component.literal("datapack firmware does not round trip back to its id"), 0);
        }

        if (BlockDeviceDataRegistry.firmwareValues().map(BlockDeviceDataRegistry::getKey).noneMatch(FIRMWARE::equals)) {
            throw new GameTestAssertException(Component.literal("datapack firmware is missing from the registry listing"), 0);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void flashMemoryTakesDatapackFirmware(final GameTestHelper helper) {
        final FlashMemoryItem item = Items.FLASH_MEMORY.get();
        final BlockDeviceData data = item.getData(item.withData(FIRMWARE));

        if (data == null || !FIRMWARE.equals(BlockDeviceDataRegistry.getKey(data))) {
            throw new GameTestAssertException(Component.literal("flash memory did not resolve its datapack firmware, got " + data), 0);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void floppyTakesDatapackImage(final GameTestHelper helper) {
        final BlockDeviceData data = BlockDeviceDataRegistry.getValue(FLOPPY);
        if (data == null) {
            throw new GameTestAssertException(Component.literal("datapack image did not resolve through the block device registry"), 0);
        }

        final FloppyItem item = Items.FLOPPY.get();
        final BlockDeviceData onFloppy = item.getData(item.withData(FLOPPY));
        if (onFloppy == null || !FLOPPY.equals(BlockDeviceDataRegistry.getKey(onFloppy))) {
            throw new GameTestAssertException(Component.literal("floppy did not resolve its datapack image, got " + onFloppy), 0);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void mediaOnlyOfferTheirOwnKind(final GameTestHelper helper) {
        requireOffered(BlockDeviceDataRegistry.floppyValues(), FLOPPY, "floppy", true);
        requireOffered(BlockDeviceDataRegistry.floppyValues(), HDD, "floppy", false);
        requireOffered(BlockDeviceDataRegistry.hardDriveValues(), HDD, "hard drive", true);
        requireOffered(BlockDeviceDataRegistry.hardDriveValues(), FLOPPY, "hard drive", false);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void imagesGoOnTheSmallestDriveTheyFit(final GameTestHelper helper) {
        requireDrive(Items.HARD_DRIVE_SMALL.get(), HDD, true);
        requireDrive(Items.HARD_DRIVE_MEDIUM.get(), HDD, false);
        requireDrive(Items.HARD_DRIVE_LARGE.get(), HDD, false);

        final BlockDeviceData sednaLinux = BlockDeviceDataRegistry.SEDNA_LINUX.get();
        requireDrive(Items.HARD_DRIVE_SMALL.get(), sednaLinux, false);
        requireDrive(Items.HARD_DRIVE_MEDIUM.get(), sednaLinux, false);
        requireDrive(Items.HARD_DRIVE_LARGE.get(), sednaLinux, true);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void datapackDataCarriesItsColor(final GameTestHelper helper) {
        requireColor(BlockDeviceDataRegistry.getValue(FIRMWARE), DyeColor.MAGENTA, "firmware");
        requireColor(BlockDeviceDataRegistry.getValue(FLOPPY), DyeColor.CYAN, "block device");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void datapackDataHasPriorityOverSyncedData(final GameTestHelper helper) {
        BlockDeviceDataRegistry.setClientViews(List.of(syncedFloppy(1)));
        try {
            requireHasContents(BlockDeviceDataRegistry.getValue(FLOPPY), "lookup");
            requireHasContents(BlockDeviceDataRegistry.floppyValues()
                .filter(data -> FLOPPY.equals(BlockDeviceDataRegistry.getKey(data)))
                .findFirst().orElse(null), "creative tab listing");
        } finally {
            BlockDeviceDataRegistry.setClientViews(List.of());
        }

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static BlockDeviceDataClientView syncedFloppy(final long capacity) {
        return new BlockDeviceDataClientView(FLOPPY, capacity, Component.literal("Synced"), DyeColor.WHITE);
    }

    private static void requireHasContents(@Nullable final BlockDeviceData data, final String what) {
        if (data == null) {
            throw new GameTestAssertException(Component.literal(FLOPPY + " did not resolve for the " + what), 0);
        }
        if (data instanceof BlockDeviceDataClientView) {
            throw new GameTestAssertException(Component.literal("data synced to the client replaced the server's own "
                + "datapack data in the " + what + ", leaving it without contents"), 0);
        }
    }

    private static void requireOffered(final Stream<BlockDeviceData> values,
                                       final Identifier location,
                                       final String medium,
                                       final boolean expected) {
        final boolean offered = values.map(BlockDeviceDataRegistry::getKey).anyMatch(location::equals);
        if (offered != expected) {
            throw new GameTestAssertException(Component.literal(location + (expected ? " is not" : " is") + " offered for "
                + medium + " in the creative tab"), 0);
        }
    }

    private static void requireDrive(final HardDriveItem drive, final Identifier location, final boolean expected) {
        final BlockDeviceData data = BlockDeviceDataRegistry.getValue(location);
        if (data == null) {
            throw new GameTestAssertException(Component.literal(location + " did not resolve"), 0);
        }
        requireDrive(drive, data, expected);
    }

    private static void requireDrive(final HardDriveItem drive, final BlockDeviceData data, final boolean expected) {
        final long size = data.getCapacity();
        if (drive.isSmallestDriveFor(size) != expected) {
            throw new GameTestAssertException(Component.literal("an image of " + size + " bytes is "
                + (expected ? "not " : "") + "offered on " + drive + ", but should " + (expected ? "" : "not ") + "be"), 0);
        }
    }

    private static void requireColor(@Nullable final BlockDeviceData data, final DyeColor expected, final String what) {
        if (data == null) {
            throw new GameTestAssertException(Component.literal("datapack " + what + " did not resolve"), 0);
        }
        if (data.getColor() != expected) {
            throw new GameTestAssertException(Component.literal("datapack " + what + " has color " + data.getColor()
                + ", expected " + expected + "; items preloaded with it would not be tinted"), 0);
        }
    }

    // --------------------------------------------------------------------- //

    private DatapackDataTests() {
    }
}
