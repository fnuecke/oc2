/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.FlashMemoryItem;
import li.cil.oc2.common.item.FloppyItem;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.item.Items;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.stream.Stream;

import static li.cil.oc2.gametest.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class DatapackDataTests {
    private static final ResourceLocation FIRMWARE =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "block_devices/flash/test_firmware.bin");
    private static final ResourceLocation FLOPPY =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "block_devices/floppy/test_floppy.bin");
    private static final ResourceLocation HDD =
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "block_devices/hdd/test_hdd.bin");

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public static void firmwareCanComeFromADatapack(final GameTestHelper helper) {
        final BlockDeviceData data = BlockDeviceDataRegistry.getValue(FIRMWARE);
        if (data == null) {
            throw new GameTestAssertException("datapack firmware did not resolve through the firmware registry");
        }

        if (!FIRMWARE.equals(BlockDeviceDataRegistry.getKey(data))) {
            throw new GameTestAssertException("datapack firmware does not round trip back to its id");
        }

        if (BlockDeviceDataRegistry.firmwareValues().map(BlockDeviceDataRegistry::getKey).noneMatch(FIRMWARE::equals)) {
            throw new GameTestAssertException("datapack firmware is missing from the registry listing, "
                + "so it would not show up in the creative tab");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void flashMemoryTakesDatapackFirmware(final GameTestHelper helper) {
        final FlashMemoryItem item = Items.FLASH_MEMORY.get();
        final BlockDeviceData data = item.getData(item.withData(FIRMWARE));

        if (data == null || !FIRMWARE.equals(BlockDeviceDataRegistry.getKey(data))) {
            throw new GameTestAssertException("flash memory did not resolve its datapack firmware, got " + data);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void floppyTakesDatapackImage(final GameTestHelper helper) {
        final BlockDeviceData data = BlockDeviceDataRegistry.getValue(FLOPPY);
        if (data == null) {
            throw new GameTestAssertException("datapack image did not resolve through the block device registry");
        }

        final FloppyItem item = Items.FLOPPY.get();
        final BlockDeviceData onFloppy = item.getData(item.withData(FLOPPY));
        if (onFloppy == null || !FLOPPY.equals(BlockDeviceDataRegistry.getKey(onFloppy))) {
            throw new GameTestAssertException("floppy did not resolve its datapack image, got " + onFloppy);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mediaOnlyOfferTheirOwnKind(final GameTestHelper helper) {
        requireOffered(BlockDeviceDataRegistry.floppyValues(), FLOPPY, "floppy", true);
        requireOffered(BlockDeviceDataRegistry.floppyValues(), HDD, "floppy", false);
        requireOffered(BlockDeviceDataRegistry.hardDriveValues(), HDD, "hard drive", true);
        requireOffered(BlockDeviceDataRegistry.hardDriveValues(), FLOPPY, "hard drive", false);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void imagesGoOnTheSmallestDriveTheyFit(final GameTestHelper helper) {
        requireDrive(Items.HARD_DRIVE_SMALL.get(), HDD, true);
        requireDrive(Items.HARD_DRIVE_MEDIUM.get(), HDD, false);
        requireDrive(Items.HARD_DRIVE_LARGE.get(), HDD, false);

        final BlockDeviceData buildroot = BlockDeviceDataRegistry.BUILDROOT.get();
        requireDrive(Items.HARD_DRIVE_SMALL.get(), buildroot, false);
        requireDrive(Items.HARD_DRIVE_MEDIUM.get(), buildroot, false);
        requireDrive(Items.HARD_DRIVE_LARGE.get(), buildroot, true);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void registeredDataUsesImagePaths(final GameTestHelper helper) {
        requireKey(BlockDeviceDataRegistry.BUILDROOT.get(), "oc2:block_devices/hdd/sedna.bin");
        requireKey(BlockDeviceDataRegistry.CPM.get(), "oc2:block_devices/floppy/cpm.bin");
        requireKey(BlockDeviceDataRegistry.FIRMWARE_RISCV.get(), "oc2:block_devices/flash/riscv.bin");
        requireKey(BlockDeviceDataRegistry.FIRMWARE_Z80.get(), "oc2:block_devices/flash/z80.bin");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void datapackDataCarriesItsColor(final GameTestHelper helper) {
        requireColor(BlockDeviceDataRegistry.getValue(FIRMWARE), DyeColor.MAGENTA, "firmware");
        requireColor(BlockDeviceDataRegistry.getValue(FLOPPY), DyeColor.CYAN, "block device");

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static void requireOffered(final Stream<BlockDeviceData> values,
                                       final ResourceLocation location,
                                       final String medium,
                                       final boolean expected) {
        final boolean offered = values.map(BlockDeviceDataRegistry::getKey).anyMatch(location::equals);
        if (offered != expected) {
            throw new GameTestAssertException(location + (expected ? " is not" : " is") + " offered for "
                + medium + " in the creative tab");
        }
    }

    private static void requireDrive(final HardDriveItem drive, final ResourceLocation location, final boolean expected) {
        final BlockDeviceData data = BlockDeviceDataRegistry.getValue(location);
        if (data == null) {
            throw new GameTestAssertException(location + " did not resolve");
        }
        requireDrive(drive, data, expected);
    }

    private static void requireDrive(final HardDriveItem drive, final BlockDeviceData data, final boolean expected) {
        final long size = data.getBlockDevice().getCapacity();
        if (drive.isSmallestDriveFor(size) != expected) {
            throw new GameTestAssertException("an image of " + size + " bytes is "
                + (expected ? "not " : "") + "offered on " + drive + ", but should " + (expected ? "" : "not ") + "be");
        }
    }

    private static void requireKey(final BlockDeviceData data, final String expected) {
        final ResourceLocation key = BlockDeviceDataRegistry.getKey(data);
        if (key == null || !expected.equals(key.toString())) {
            throw new GameTestAssertException("registered data has id [" + key + "], expected [" + expected + "]");
        }
    }

    private static void requireColor(final BlockDeviceData data, final DyeColor expected, final String what) {
        if (data == null) {
            throw new GameTestAssertException("datapack " + what + " did not resolve");
        }
        if (data.getColor() != expected) {
            throw new GameTestAssertException("datapack " + what + " has color " + data.getColor()
                + ", expected " + expected + "; items preloaded with it would not be tinted");
        }
    }

    // --------------------------------------------------------------------- //

    private DatapackDataTests() {
    }
}
