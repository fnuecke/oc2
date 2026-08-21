/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.ResetRecipe;
import li.cil.oc2.common.item.crafting.WrenchRecipe;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static li.cil.oc2.gametest.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlobStorageTests {
    private static final String DEVICE_KEY = "oc2:hard_drive";
    private static final String BLOB_HANDLE_TAG_NAME = "blob";
    private static final long ANCIENT_MILLIS = 1000L;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public static void capacityIsClampedToConfiguredMaximum(final GameTestHelper helper) {
        final int originalMaximum = Config.maxBlobCapacity;
        try {
            Config.maxBlobCapacity = 8 * 1024 * 1024;

            final HardDriveItem item = Items.HARD_DRIVE_LARGE.get();
            final ItemStack stack = item.withCapacity(new ItemStack(item), Integer.MAX_VALUE);

            final int capacity = item.getCapacity(stack);
            if (capacity != Config.maxBlobCapacity) {
                throw new GameTestAssertException("Hand crafted capacity was not clamped: expected "
                    + Config.maxBlobCapacity + ", got " + capacity);
            }
        } finally {
            Config.maxBlobCapacity = originalMaximum;
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void capacityIsUnboundedWhenMaximumIsZero(final GameTestHelper helper) {
        final int originalMaximum = Config.maxBlobCapacity;
        try {
            Config.maxBlobCapacity = 0;

            final HardDriveItem item = Items.HARD_DRIVE_LARGE.get();
            final ItemStack stack = item.withCapacity(new ItemStack(item), 12345);

            if (item.getCapacity(stack) != 12345) {
                throw new GameTestAssertException("A maximum of zero should mean no limit, got "
                    + item.getCapacity(stack));
            }
        } finally {
            Config.maxBlobCapacity = originalMaximum;
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void openRefusesMissingBlob(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();

        try {
            BlobStorage.open(handle, false);
            throw new GameTestAssertException("Opening a blob that does not exist must fail rather than "
                + "silently handing out a blank one");
        } catch (final BlobStorage.BlobMissingException e) {
            // Expected.
        } catch (final IOException e) {
            throw new GameTestAssertException("Expected BlobMissingException, got " + e);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void openRefusesBlobAlreadyInUse(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        try {
            BlobStorage.open(handle, true);

            try {
                BlobStorage.open(handle, true);
                throw new GameTestAssertException("Opening a blob that is already in use must fail, or two "
                    + "duplicated drives would corrupt each other");
            } catch (final BlobStorage.BlobInUseException e) {
                // Expected.
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void evictionRemovesLeastRecentlyUsedBlob(final GameTestHelper helper) {
        final ConfigSnapshot config = ConfigSnapshot.take();
        final List<UUID> handles = new ArrayList<>();
        try {
            // A grace period plus deliberately backdated blobs means ours are the only candidates.
            // Blobs another test creates while this one runs are recent.
            Config.blobEvictionGraceHours = 24;
            Config.maxTrashedBlobCount = 0;

            final UUID oldest = createClosedBlob(handles, ANCIENT_MILLIS);
            final UUID newer = createClosedBlob(handles, ANCIENT_MILLIS + 60_000L);

            // Anything we add from here on has to push something out.
            Config.maxBlobCount = BlobStorage.getBlobCount();

            createClosedBlob(handles, System.currentTimeMillis());

            if (BlobStorage.exists(oldest)) {
                throw new GameTestAssertException("Least recently used blob should have been evicted");
            }
            if (!BlobStorage.exists(newer)) {
                throw new GameTestAssertException("More recently used blob should have been kept");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void evictionKeepsBlobsThatAreInUse(final GameTestHelper helper) {
        final ConfigSnapshot config = ConfigSnapshot.take();
        final List<UUID> handles = new ArrayList<>();
        try {
            Config.blobEvictionGraceHours = 24;
            Config.maxTrashedBlobCount = 0;

            // The oldest blob by a wide margin, but left open, so it must survive regardless.
            final UUID inUse = BlobStorage.allocateHandle();
            handles.add(inUse);
            final FileChannel channel = BlobStorage.open(inUse, true);
            setLastUsed(inUse, ANCIENT_MILLIS);

            final UUID evictable = createClosedBlob(handles, ANCIENT_MILLIS + 60_000L);

            Config.maxBlobCount = BlobStorage.getBlobCount();

            createClosedBlob(handles, System.currentTimeMillis());

            if (!BlobStorage.exists(inUse) || !channel.isOpen()) {
                throw new GameTestAssertException("A blob that is currently open must never be evicted, or "
                    + "we pull data out from under a running machine");
            }
            if (BlobStorage.exists(evictable)) {
                throw new GameTestAssertException("The closed blob should have been evicted instead");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void evictionKeepsBlobsWithinGracePeriod(final GameTestHelper helper) {
        final ConfigSnapshot config = ConfigSnapshot.take();
        final List<UUID> handles = new ArrayList<>();
        try {
            Config.blobEvictionGraceHours = 24;
            Config.maxTrashedBlobCount = 0;

            // One blob well outside the grace window, one inside it. Only the former may be taken, which
            // is what makes this test say anything about the grace period at all.
            final UUID stale = createClosedBlob(handles, ANCIENT_MILLIS);
            final UUID recent = createClosedBlob(handles,
                System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));

            Config.maxBlobCount = BlobStorage.getBlobCount();

            createClosedBlob(handles, System.currentTimeMillis());

            if (BlobStorage.exists(stale)) {
                throw new GameTestAssertException("A blob well past the grace period should have been evicted");
            }
            if (!BlobStorage.exists(recent)) {
                throw new GameTestAssertException("A blob used within the grace period must not be evicted");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void evictedBlobsAreRecoverableFromTrash(final GameTestHelper helper) {
        final ConfigSnapshot config = ConfigSnapshot.take();
        final List<UUID> handles = new ArrayList<>();
        final Path trashDirectory = BlobStorage.getTrashDirectory();
        if (trashDirectory == null) {
            throw new GameTestAssertException("Blob storage is not initialized");
        }

        try {
            Config.blobEvictionGraceHours = 24;
            Config.maxTrashedBlobCount = 8;

            final UUID evicted = createClosedBlob(handles, ANCIENT_MILLIS);
            Config.maxBlobCount = BlobStorage.getBlobCount();
            createClosedBlob(handles, System.currentTimeMillis());

            if (BlobStorage.exists(evicted)) {
                throw new GameTestAssertException("Blob should have been evicted");
            }
            if (!Files.exists(trashDirectory.resolve(evicted.toString()))) {
                throw new GameTestAssertException("An evicted blob should be kept in the trash directory under "
                    + "its original name, so it can be recovered by moving it back");
            }

            Files.deleteIfExists(trashDirectory.resolve(evicted.toString()));
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void trashIsTrimmedToConfiguredSize(final GameTestHelper helper) {
        final ConfigSnapshot config = ConfigSnapshot.take();
        final List<UUID> handles = new ArrayList<>();
        final Path trashDirectory = BlobStorage.getTrashDirectory();
        if (trashDirectory == null) {
            throw new GameTestAssertException("Blob storage is not initialized");
        }

        try {
            Config.blobEvictionGraceHours = 24;
            Config.maxTrashedBlobCount = 1;

            final UUID first = createClosedBlob(handles, ANCIENT_MILLIS);
            final UUID second = createClosedBlob(handles, ANCIENT_MILLIS + 60_000L);

            // Force both out, one at a time, so the trash has to trim itself down to one entry.
            Config.maxBlobCount = BlobStorage.getBlobCount();
            createClosedBlob(handles, System.currentTimeMillis());
            Config.maxBlobCount = BlobStorage.getBlobCount();
            createClosedBlob(handles, System.currentTimeMillis());

            final long trashedCount;
            try (var paths = Files.list(trashDirectory)) {
                trashedCount = paths.count();
            }

            if (trashedCount > Config.maxTrashedBlobCount) {
                throw new GameTestAssertException("Trash should have been trimmed to "
                    + Config.maxTrashedBlobCount + ", holds " + trashedCount);
            }
            if (Files.exists(trashDirectory.resolve(first.toString()))) {
                throw new GameTestAssertException("Trash should drop its oldest entry first");
            }

            Files.deleteIfExists(trashDirectory.resolve(second.toString()));
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void clearBlobDataReleasesBlobAndClearsFlag(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        try {
            BlobStorage.open(handle, true);
            BlobStorage.close(handle);

            final ItemStack stack = driveReferencing(handle);
            StorageItemUtils.setCorrupted(stack);

            if (!StorageItemUtils.isCorrupted(stack)) {
                throw new GameTestAssertException("Item should have been flagged as corrupted");
            }

            StorageItemUtils.clearBlobData(stack);

            if (StorageItemUtils.isCorrupted(stack)) {
                throw new GameTestAssertException("Resetting the item should clear the corrupted flag");
            }
            if (ItemDeviceUtils.getItemDeviceData(stack).getCompound(DEVICE_KEY).hasUUID(BLOB_HANDLE_TAG_NAME)) {
                throw new GameTestAssertException("Resetting the item should drop the blob handle");
            }
            if (BlobStorage.exists(handle)) {
                throw new GameTestAssertException("Resetting the item should delete the blob it referenced, "
                    + "otherwise players can orphan blobs at will");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void clearBlobDataKeepsBlobThatIsInUse(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        try {
            BlobStorage.open(handle, true);

            StorageItemUtils.clearBlobData(driveReferencing(handle));

            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException("A blob still in use by a duplicate of the item must not be "
                    + "deleted when one copy is reset");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stripBlobDataLeavesBlobAlone(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        try {
            BlobStorage.open(handle, true);
            BlobStorage.close(handle);

            final ItemStack stack = driveReferencing(handle);
            StorageItemUtils.stripBlobData(stack);

            if (ItemDeviceUtils.getItemDeviceData(stack).getCompound(DEVICE_KEY).hasUUID(BLOB_HANDLE_TAG_NAME)) {
                throw new GameTestAssertException("Stripping should drop the blob handle");
            }
            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException("Stripping is used to compute crafting previews, which are "
                    + "speculative, so it must never touch the blob");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void resetRecipeOnlyAppliesToCorruptedDrives(final GameTestHelper helper) {
        final ResetRecipe recipe = new ResetRecipe(CraftingBookCategory.MISC);

        final ItemStack healthy = new ItemStack(Items.HARD_DRIVE_LARGE.get());
        if (recipe.matches(gridOf(healthy, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException("Resetting must not apply to a healthy drive, or players would "
                + "wipe good drives by accident and it would fight the wrench "
                + "recipes that convert drives");
        }

        final ItemStack corrupted = new ItemStack(Items.HARD_DRIVE_LARGE.get());
        StorageItemUtils.setCorrupted(corrupted);
        if (!recipe.matches(gridOf(corrupted, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException("Resetting must apply to a drive that lost its data");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void resetRecipePreservesEverythingButTheData(final GameTestHelper helper) {
        final ResetRecipe recipe = new ResetRecipe(CraftingBookCategory.MISC);

        final HardDriveItem item = Items.HARD_DRIVE_LARGE.get();
        final ItemStack corrupted = item.withCapacity(new ItemStack(item), 2 * 1024 * 1024);
        StorageItemUtils.setCorrupted(corrupted);

        final ItemStack result = recipe.assemble(
            gridOf(corrupted, new ItemStack(Items.WRENCH.get())), helper.getLevel().registryAccess());

        if (!result.is(item)) {
            throw new GameTestAssertException("Reset should hand back the same kind of drive");
        }
        if (item.getCapacity(result) != 2 * 1024 * 1024) {
            throw new GameTestAssertException("Reset must keep the drive's capacity, got "
                + item.getCapacity(result));
        }
        if (StorageItemUtils.isCorrupted(result)) {
            throw new GameTestAssertException("Reset should hand back a drive that is no longer corrupted");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wrenchRecipeStepsAsideForCorruptedDrives(final GameTestHelper helper) {
        // Mirrors the data generated recipe that turns a large hard drive into one with custom data.
        final WrenchRecipe recipe = new WrenchRecipe(new ShapelessRecipe("", CraftingBookCategory.MISC,
            new ItemStack(Items.HARD_DRIVE_CUSTOM.get()),
            NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(Items.HARD_DRIVE_LARGE.get()),
                Ingredient.of(Items.WRENCH.get()))));

        final ItemStack healthy = new ItemStack(Items.HARD_DRIVE_LARGE.get());
        if (!recipe.matches(gridOf(healthy, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException("Converting a healthy drive should still work");
        }

        final ItemStack corrupted = new ItemStack(Items.HARD_DRIVE_LARGE.get());
        StorageItemUtils.setCorrupted(corrupted);
        if (recipe.matches(gridOf(corrupted, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException("A corrupted drive must be reset before it can be converted, "
                + "otherwise this recipe and the reset recipe both match the "
                + "same inputs and which one wins is anyone's guess");
        }

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static CraftingInput gridOf(final ItemStack... stacks) {
        return CraftingInput.of(stacks.length, 1, List.of(stacks));
    }

    private static ItemStack driveReferencing(final UUID handle) {
        final ItemStack stack = new ItemStack(Items.HARD_DRIVE_LARGE.get());

        final CompoundTag driveData = new CompoundTag();
        driveData.putUUID(BLOB_HANDLE_TAG_NAME, handle);

        final CompoundTag deviceData = new CompoundTag();
        deviceData.put(DEVICE_KEY, driveData);

        ItemDeviceUtils.setItemDeviceData(stack, deviceData);

        return stack;
    }

    private static UUID createClosedBlob(final List<UUID> handles, final long lastUsedMillis) throws IOException {
        final UUID handle = BlobStorage.allocateHandle();
        handles.add(handle);

        BlobStorage.open(handle, true);
        BlobStorage.close(handle);
        setLastUsed(handle, lastUsedMillis);

        return handle;
    }

    private static void setLastUsed(final UUID handle, final long millis) throws IOException {
        final Path directory = BlobStorage.getDataDirectory();
        if (directory == null) {
            throw new GameTestAssertException("Blob storage is not initialized");
        }

        Files.setLastModifiedTime(directory.resolve(handle.toString()), FileTime.fromMillis(millis));
    }

    private static void releaseAll(final List<UUID> handles) {
        handles.forEach(BlobStorageTests::release);
    }

    private static void release(final UUID handle) {
        // Deletion refuses to touch a blob that is still open, so make sure it is not.
        BlobStorage.close(handle);
        BlobStorage.delete(handle);
    }

    private record ConfigSnapshot(int maxBlobCount, int maxTrashedBlobCount, int blobEvictionGraceHours) {
        static ConfigSnapshot take() {
            return new ConfigSnapshot(Config.maxBlobCount, Config.maxTrashedBlobCount, Config.blobEvictionGraceHours);
        }

        void restore() {
            Config.maxBlobCount = maxBlobCount;
            Config.maxTrashedBlobCount = maxTrashedBlobCount;
            Config.blobEvictionGraceHours = blobEvictionGraceHours;
        }
    }
}
