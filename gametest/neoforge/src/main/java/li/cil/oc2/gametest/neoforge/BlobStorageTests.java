/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.ResetRecipe;
import li.cil.oc2.common.item.crafting.ToolRecipe;
import li.cil.oc2.common.serialization.BlobReference;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.oc2.common.util.StorageItemUtils.State;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;
import static li.cil.oc2.gametest.util.TestSupport.createBlob;

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
    public static void saveClearsClosedMarkers(final GameTestHelper helper) {
        final BlobReference closedBlob = new BlobReference();
        final BlobReference stillOpenBlob = new BlobReference();
        try {
            final UUID closed = createBlob(closedBlob);
            closedBlob.close();
            final UUID stillOpen = createBlob(stillOpenBlob);

            BlobStorage.handleSaved();

            if (BlobStorage.isMarkedHandle(closed)) {
                throw new GameTestAssertException("marker of a blob closed before the save should be gone");
            }
            if (!BlobStorage.isMarkedHandle(stillOpen)) {
                throw new GameTestAssertException("blob still open at the save should keep its marker");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(closedBlob);
            release(stillOpenBlob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ownMarkersAreNeverStale(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            if (BlobStorage.isStaleHandle(handle)) {
                throw new GameTestAssertException("A blob we are holding open is not evidence of a crash");
            }

            blob.close();
            if (!BlobStorage.isMarkedHandle(handle)) {
                throw new GameTestAssertException("Precondition: closing keeps the marker");
            }
            if (BlobStorage.isStaleHandle(handle)) {
                throw new GameTestAssertException("A marker this session wrote is not evidence of a crash");
            }

            BlobStorage.handleSaved();
            if (BlobStorage.isMarkedHandle(handle) || BlobStorage.isStaleHandle(handle)) {
                throw new GameTestAssertException("The save clears the marker and the handle with it");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void foreignMarkerIsStale(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        final Path directory = BlobStorage.getDataDirectory();
        if (directory == null) {
            throw new GameTestAssertException("Blob storage is not initialized");
        }

        try {
            Files.createFile(directory.resolve(handle.toString()));
            Files.createFile(directory.resolve(handle + ".dirty"));

            if (!BlobStorage.isStaleHandle(handle)) {
                throw new GameTestAssertException("marker from another session should read as stale");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            try {
                Files.deleteIfExists(directory.resolve(handle + ".dirty"));
            } catch (final IOException e) {
                throw new GameTestAssertException("Failed cleaning up: " + e);
            }
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void reopeningBeforeSaveKeepsTheMarker(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            blob.close();
            blob.open();

            BlobStorage.handleSaved();

            if (!BlobStorage.isMarkedHandle(handle)) {
                throw new GameTestAssertException("save should keep the marker of a reopened blob");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deletingABlobTakesItsMarker(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            blob.close();
            BlobStorage.delete(handle);

            if (BlobStorage.isMarkedHandle(handle)) {
                throw new GameTestAssertException("A marker outliving its blob would flag whatever handle "
                    + "happened to land on it next");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void evictionMovesBlobAndMarkerToTrash(final GameTestHelper helper) {
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

            if (BlobStorage.exists(evicted) || BlobStorage.isMarkedHandle(evicted)) {
                throw new GameTestAssertException("Eviction must take the blob and its marker out of the blob "
                    + "directory");
            }
            if (!Files.exists(trashDirectory.resolve(evicted.toString()))
                || !Files.exists(trashDirectory.resolve(evicted + ".dirty"))) {
                throw new GameTestAssertException("blob and marker belong in the trash under their original names");
            }

            Files.deleteIfExists(trashDirectory.resolve(evicted.toString()));
            Files.deleteIfExists(trashDirectory.resolve(evicted + ".dirty"));
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void markersDoNotCountAsBlobs(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final int before = BlobStorage.getBlobCount();
            createBlob(blob);

            if (BlobStorage.getBlobCount() != before + 1) {
                throw new GameTestAssertException("marker should not count towards the blob count");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void openRefusesMissingBlob(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();

        try {
            BlobStorage.open(handle, false);
            throw new GameTestAssertException("opening a missing blob should fail");
        } catch (final BlobStorage.BlobMissingException ignored) {
        } catch (final IOException e) {
            throw new GameTestAssertException("Expected BlobMissingException, got " + e);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void openRefusesBlobAlreadyInUse(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);

            try {
                BlobStorage.open(handle, true);
                throw new GameTestAssertException("opening a blob that is already in use should fail");
            } catch (final BlobStorage.BlobInUseException ignored) {
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void referenceRefusesToOpenTwice(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            createBlob(blob);

            try {
                blob.open();
                throw new GameTestAssertException("opening a reference that is already open should fail");
            } catch (final IllegalStateException ignored) {
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void evictionRemovesLeastRecentlyUsedBlob(final GameTestHelper helper) {
        final ConfigSnapshot config = ConfigSnapshot.take();
        final List<UUID> handles = new ArrayList<>();
        try {
            Config.blobEvictionGraceHours = 24;
            Config.maxTrashedBlobCount = 0;

            final UUID oldest = createClosedBlob(handles, ANCIENT_MILLIS);
            final UUID newer = createClosedBlob(handles, ANCIENT_MILLIS + 60_000L);

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
        final BlobReference inUseBlob = new BlobReference();
        try {
            Config.blobEvictionGraceHours = 24;
            Config.maxTrashedBlobCount = 0;

            final UUID inUse = createBlob(inUseBlob);
            setLastUsed(inUse, ANCIENT_MILLIS);

            final UUID evictable = createClosedBlob(handles, ANCIENT_MILLIS + 60_000L);

            Config.maxBlobCount = BlobStorage.getBlobCount();

            createClosedBlob(handles, System.currentTimeMillis());

            if (!BlobStorage.exists(inUse) || !BlobStorage.isOpen(inUse)) {
                throw new GameTestAssertException("an open blob should never be evicted");
            }
            if (BlobStorage.exists(evictable)) {
                throw new GameTestAssertException("The closed blob should have been evicted instead");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            config.restore();
            release(inUseBlob);
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

            Config.maxBlobCount = BlobStorage.getBlobCount();
            createClosedBlob(handles, System.currentTimeMillis());
            Config.maxBlobCount = BlobStorage.getBlobCount();
            createClosedBlob(handles, System.currentTimeMillis());

            final long trashedCount;
            try (var paths = Files.list(trashDirectory)) {
                trashedCount = paths.filter(path -> !path.getFileName().toString().endsWith(".dirty")).count();
            }

            if (trashedCount > Config.maxTrashedBlobCount) {
                throw new GameTestAssertException("Trash should have been trimmed to "
                    + Config.maxTrashedBlobCount + ", holds " + trashedCount);
            }
            if (Files.exists(trashDirectory.resolve(first.toString()))) {
                throw new GameTestAssertException("Trash should drop its oldest entry first");
            }

            Files.deleteIfExists(trashDirectory.resolve(second.toString()));
            Files.deleteIfExists(trashDirectory.resolve(second + ".dirty"));
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
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            blob.close();

            final ItemStack stack = driveReferencing(handle);
            StorageItemUtils.setState(stack, State.CORRUPTED);

            if (!isCorrupted(stack)) {
                throw new GameTestAssertException("Item should have been flagged as corrupted");
            }

            StorageItemUtils.clearBlobData(stack);

            if (isCorrupted(stack)) {
                throw new GameTestAssertException("Resetting the item should clear the corrupted flag");
            }
            if (ItemDeviceUtils.getItemDeviceData(stack).getCompound(DEVICE_KEY).hasUUID(BLOB_HANDLE_TAG_NAME)) {
                throw new GameTestAssertException("Resetting the item should drop the blob handle");
            }
            if (BlobStorage.exists(handle)) {
                throw new GameTestAssertException("resetting the item should delete its blob");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void clearBlobDataKeepsBlobThatIsInUse(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);

            StorageItemUtils.clearBlobData(driveReferencing(handle));

            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException("a blob another item holds open should survive a reset");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void stripBlobDataLeavesBlobAlone(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            blob.close();

            final ItemStack stack = driveReferencing(handle);
            StorageItemUtils.stripBlobData(stack);

            if (ItemDeviceUtils.getItemDeviceData(stack).getCompound(DEVICE_KEY).hasUUID(BLOB_HANDLE_TAG_NAME)) {
                throw new GameTestAssertException("Stripping should drop the blob handle");
            }
            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException("stripping should leave the blob alone");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void resetRecipeOnlyAppliesToCorruptedDrives(final GameTestHelper helper) {
        final ResetRecipe recipe = new ResetRecipe(CraftingBookCategory.MISC);

        final ItemStack healthy = new ItemStack(Items.HARD_DRIVE_LARGE.get());
        if (recipe.matches(gridOf(healthy, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException("reset should not match a healthy drive");
        }

        final ItemStack corrupted = new ItemStack(Items.HARD_DRIVE_LARGE.get());
        StorageItemUtils.setState(corrupted, State.CORRUPTED);
        if (!recipe.matches(gridOf(corrupted, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException("Resetting must apply to a drive that lost its data");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void resetKeepsCapacity(final GameTestHelper helper) {
        final ResetRecipe recipe = new ResetRecipe(CraftingBookCategory.MISC);

        final HardDriveItem item = Items.HARD_DRIVE_LARGE.get();
        final ItemStack corrupted = item.withCapacity(new ItemStack(item), 2 * 1024 * 1024);
        StorageItemUtils.setState(corrupted, State.CORRUPTED);

        final ItemStack result = recipe.assemble(
            gridOf(corrupted, new ItemStack(Items.WRENCH.get())), helper.getLevel().registryAccess());

        if (!result.is(item)) {
            throw new GameTestAssertException("Reset should hand back the same kind of drive");
        }
        if (item.getCapacity(result) != 2 * 1024 * 1024) {
            throw new GameTestAssertException("Reset must keep the drive's capacity, got "
                + item.getCapacity(result));
        }
        if (isCorrupted(result)) {
            throw new GameTestAssertException("Reset should hand back a drive that is no longer corrupted");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolRecipeStepsAsideForCorruptedDrives(final GameTestHelper helper) {
        final ToolRecipe recipe = new ToolRecipe(new ShapelessRecipe("", CraftingBookCategory.MISC,
            new ItemStack(Items.HARD_DRIVE_LARGE.get()),
            NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(Items.WRENCH.get()),
                Ingredient.of(Items.HARD_DRIVE_LARGE.get()))));

        final ItemStack healthy = Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId());
        if (!recipe.matches(gridOf(healthy, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException("Converting a healthy drive should still work");
        }

        final ItemStack corrupted = Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId());
        StorageItemUtils.setState(corrupted, State.CORRUPTED);
        if (recipe.matches(gridOf(corrupted, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException("tool recipe should not match a corrupted drive");
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
        final BlobReference blob = new BlobReference();
        final UUID handle = createBlob(blob);
        blob.close();
        handles.add(handle);

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
        BlobStorage.delete(handle);
        BlobStorage.handleSaved();
    }

    private static void release(final BlobReference blob) {
        blob.delete();
        BlobStorage.handleSaved();
    }

    private static boolean isCorrupted(final ItemStack stack) {
        return StorageItemUtils.getState(stack) == State.CORRUPTED;
    }

    // --------------------------------------------------------------------- //

    private BlobStorageTests() {
    }

    // --------------------------------------------------------------------- //

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
