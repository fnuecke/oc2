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
import net.minecraft.core.UUIDUtil;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static li.cil.oc2.gametest.util.Blobs.*;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "blob_storage")
public final class BlobStorageTests {
    private static final long ANCIENT_MILLIS = 1000L;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void capacityIsClampedToConfiguredMaximum(final GameTestHelper helper) {
        final int originalMaximum = Config.maxBlobCapacity;
        try {
            Config.maxBlobCapacity = 8 * 1024 * 1024;

            final HardDriveItem item = Items.HARD_DRIVE_LARGE.get();
            final ItemStack stack = item.withCapacity(new ItemStack(item), Integer.MAX_VALUE);

            final int capacity = item.getCapacity(stack);
            if (capacity != Config.maxBlobCapacity) {
                throw new GameTestAssertException(Component.literal("Hand crafted capacity was not clamped: expected "
                    + Config.maxBlobCapacity + ", got " + capacity), 0);
            }
        } finally {
            Config.maxBlobCapacity = originalMaximum;
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void capacityIsUnboundedWhenMaximumIsZero(final GameTestHelper helper) {
        final int originalMaximum = Config.maxBlobCapacity;
        try {
            Config.maxBlobCapacity = 0;

            final HardDriveItem item = Items.HARD_DRIVE_LARGE.get();
            final ItemStack stack = item.withCapacity(new ItemStack(item), 12345);

            if (item.getCapacity(stack) != 12345) {
                throw new GameTestAssertException(Component.literal("A maximum of zero should mean no limit, got "
                    + item.getCapacity(stack)), 0);
            }
        } finally {
            Config.maxBlobCapacity = originalMaximum;
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void saveClearsClosedMarkers(final GameTestHelper helper) {
        final BlobReference closedBlob = new BlobReference();
        final BlobReference stillOpenBlob = new BlobReference();
        try {
            final UUID closed = createBlob(closedBlob);
            closedBlob.close();
            final UUID stillOpen = createBlob(stillOpenBlob);

            BlobStorage.handleSaved();

            if (BlobStorage.isMarkedHandle(closed)) {
                throw new GameTestAssertException(Component.literal("marker of a blob closed before the save should be gone"), 0);
            }
            if (!BlobStorage.isMarkedHandle(stillOpen)) {
                throw new GameTestAssertException(Component.literal("blob still open at the save should keep its marker"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(closedBlob);
            release(stillOpenBlob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void ownMarkersAreNeverStale(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            if (BlobStorage.isStaleHandle(handle)) {
                throw new GameTestAssertException(Component.literal("A blob we are holding open is not evidence of a crash"), 0);
            }

            blob.close();
            if (!BlobStorage.isMarkedHandle(handle)) {
                throw new GameTestAssertException(Component.literal("Precondition: closing keeps the marker"), 0);
            }
            if (BlobStorage.isStaleHandle(handle)) {
                throw new GameTestAssertException(Component.literal("A marker this session wrote is not evidence of a crash"), 0);
            }

            BlobStorage.handleSaved();
            if (BlobStorage.isMarkedHandle(handle) || BlobStorage.isStaleHandle(handle)) {
                throw new GameTestAssertException(Component.literal("The save clears the marker and the handle with it"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignMarkerIsStale(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        final Path directory = BlobStorage.getDataDirectory();
        if (directory == null) {
            throw new GameTestAssertException(Component.literal("Blob storage is not initialized"), 0);
        }

        try {
            markStale(handle);

            if (!BlobStorage.isStaleHandle(handle)) {
                throw new GameTestAssertException(Component.literal("marker from another session should read as stale"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            try {
                Files.deleteIfExists(directory.resolve(handle + ".dirty"));
            } catch (final IOException e) {
                throw new GameTestAssertException(Component.literal("Failed cleaning up: " + e), 0);
            }
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void reopeningBeforeSaveKeepsTheMarker(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            blob.close();
            blob.open();

            BlobStorage.handleSaved();

            if (!BlobStorage.isMarkedHandle(handle)) {
                throw new GameTestAssertException(Component.literal("save should keep the marker of a reopened blob"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void deletingABlobTakesItsMarker(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            blob.close();
            BlobStorage.delete(handle);

            if (BlobStorage.isMarkedHandle(handle)) {
                throw new GameTestAssertException(Component.literal("A marker outliving its blob would flag whatever handle "
                    + "happened to land on it next"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void evictionMovesBlobAndMarkerToTrash(final GameTestHelper helper) {
        final ConfigSnapshot config = ConfigSnapshot.take();
        final List<UUID> handles = new ArrayList<>();
        final Path trashDirectory = BlobStorage.getTrashDirectory();
        if (trashDirectory == null) {
            throw new GameTestAssertException(Component.literal("Blob storage is not initialized"), 0);
        }

        try {
            Config.blobEvictionGraceHours = 24;
            Config.maxTrashedBlobCount = 8;

            final UUID evicted = createClosedBlob(handles, ANCIENT_MILLIS);

            Config.maxBlobCount = BlobStorage.getBlobCount();
            createClosedBlob(handles, System.currentTimeMillis());

            if (BlobStorage.exists(evicted) || BlobStorage.isMarkedHandle(evicted)) {
                throw new GameTestAssertException(Component.literal("Eviction must take the blob and its marker out of the blob "
                    + "directory"), 0);
            }
            if (!Files.exists(trashDirectory.resolve(evicted.toString()))
                || !Files.exists(trashDirectory.resolve(evicted + ".dirty"))) {
                throw new GameTestAssertException(Component.literal("blob and marker belong in the trash under their original names"), 0);
            }

            Files.deleteIfExists(trashDirectory.resolve(evicted.toString()));
            Files.deleteIfExists(trashDirectory.resolve(evicted + ".dirty"));
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void markersDoNotCountAsBlobs(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final int before = BlobStorage.getBlobCount();
            createBlob(blob);

            if (BlobStorage.getBlobCount() != before + 1) {
                throw new GameTestAssertException(Component.literal("marker should not count towards the blob count"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void openRefusesMissingBlob(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();

        try {
            BlobStorage.open(handle, false);
            throw new GameTestAssertException(Component.literal("opening a missing blob should fail"), 0);
        } catch (final BlobStorage.BlobMissingException ignored) {
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Expected BlobMissingException, got " + e), 0);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void openRefusesBlobAlreadyInUse(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);

            try {
                BlobStorage.open(handle, true);
                throw new GameTestAssertException(Component.literal("opening a blob that is already in use should fail"), 0);
            } catch (final BlobStorage.BlobInUseException ignored) {
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void referenceRefusesToOpenTwice(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            createBlob(blob);

            try {
                blob.open();
                throw new GameTestAssertException(Component.literal("opening a reference that is already open should fail"), 0);
            } catch (final IllegalStateException ignored) {
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
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
                throw new GameTestAssertException(Component.literal("Least recently used blob should have been evicted"), 0);
            }
            if (!BlobStorage.exists(newer)) {
                throw new GameTestAssertException(Component.literal("More recently used blob should have been kept"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
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
                throw new GameTestAssertException(Component.literal("an open blob should never be evicted"), 0);
            }
            if (BlobStorage.exists(evictable)) {
                throw new GameTestAssertException(Component.literal("The closed blob should have been evicted instead"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            config.restore();
            release(inUseBlob);
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
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
                throw new GameTestAssertException(Component.literal("A blob well past the grace period should have been evicted"), 0);
            }
            if (!BlobStorage.exists(recent)) {
                throw new GameTestAssertException(Component.literal("A blob used within the grace period must not be evicted"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void trashIsTrimmedToConfiguredSize(final GameTestHelper helper) {
        final ConfigSnapshot config = ConfigSnapshot.take();
        final List<UUID> handles = new ArrayList<>();
        final Path trashDirectory = BlobStorage.getTrashDirectory();
        if (trashDirectory == null) {
            throw new GameTestAssertException(Component.literal("Blob storage is not initialized"), 0);
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
                throw new GameTestAssertException(Component.literal("Trash should have been trimmed to "
                    + Config.maxTrashedBlobCount + ", holds " + trashedCount), 0);
            }
            if (Files.exists(trashDirectory.resolve(first.toString()))) {
                throw new GameTestAssertException(Component.literal("Trash should drop its oldest entry first"), 0);
            }

            Files.deleteIfExists(trashDirectory.resolve(second.toString()));
            Files.deleteIfExists(trashDirectory.resolve(second + ".dirty"));
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            config.restore();
            releaseAll(handles);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void clearBlobDataReleasesBlobAndClearsFlag(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            blob.close();

            final ItemStack stack = driveReferencing(new ItemStack(Items.HARD_DRIVE_LARGE.get()), handle);
            StorageItemUtils.setState(stack, State.CORRUPTED);

            if (!isCorrupted(stack)) {
                throw new GameTestAssertException(Component.literal("Item should have been flagged as corrupted"), 0);
            }

            StorageItemUtils.clearBlobData(stack);

            if (isCorrupted(stack)) {
                throw new GameTestAssertException(Component.literal("Resetting the item should clear the corrupted flag"), 0);
            }
            if (ItemDeviceUtils.getItemDeviceData(stack).getCompoundOrEmpty(DEVICE_KEY).read(HANDLE_TAG_NAME, UUIDUtil.CODEC).isPresent()) {
                throw new GameTestAssertException(Component.literal("Resetting the item should drop the blob handle"), 0);
            }
            if (BlobStorage.exists(handle)) {
                throw new GameTestAssertException(Component.literal("resetting the item should delete its blob"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void clearBlobDataKeepsBlobThatIsInUse(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);

            StorageItemUtils.clearBlobData(driveReferencing(new ItemStack(Items.HARD_DRIVE_LARGE.get()), handle));

            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException(Component.literal("a blob another item holds open should survive a reset"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void stripBlobDataLeavesBlobAlone(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);
            blob.close();

            final ItemStack stack = driveReferencing(new ItemStack(Items.HARD_DRIVE_LARGE.get()), handle);
            StorageItemUtils.stripBlobData(stack);

            if (ItemDeviceUtils.getItemDeviceData(stack).getCompoundOrEmpty(DEVICE_KEY).read(HANDLE_TAG_NAME, UUIDUtil.CODEC).isPresent()) {
                throw new GameTestAssertException(Component.literal("Stripping should drop the blob handle"), 0);
            }
            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException(Component.literal("stripping should leave the blob alone"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void resetRecipeOnlyAppliesToCorruptedOrUnverifiedDrives(final GameTestHelper helper) {
        final ResetRecipe recipe = new ResetRecipe(CraftingBookCategory.MISC);

        final ItemStack healthy = new ItemStack(Items.HARD_DRIVE_LARGE.get());
        if (recipe.matches(gridOf(healthy, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException(Component.literal("reset should not match a healthy drive"), 0);
        }

        final ItemStack corrupted = new ItemStack(Items.HARD_DRIVE_LARGE.get());
        StorageItemUtils.setState(corrupted, State.CORRUPTED);
        if (!recipe.matches(gridOf(corrupted, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException(Component.literal("Resetting must apply to a drive that lost its data"), 0);
        }

        final BlobReference blob = new BlobReference();
        final ItemStack unverified = new ItemStack(Items.HARD_DRIVE_SMALL.get());
        try {
            final UUID handle = createBlob(blob);
            blob.close();
            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException(Component.literal("Precondition: the drive has data to keep"), 0);
            }

            driveReferencing(unverified, handle);
            StorageItemUtils.setState(unverified, State.INCONSISTENT);

            final CraftingInput input = gridOf(unverified, new ItemStack(Items.WRENCH.get()));

            if (!recipe.matches(input, helper.getLevel())) {
                throw new GameTestAssertException(Component.literal("Accepting an unverified drive is the same craft as "
                    + "resetting a corrupted one"), 0);
            }

            final ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
            if (StorageItemUtils.getState(result) != State.ACKNOWLEDGED) {
                throw new GameTestAssertException(Component.literal("The craft moves the drive to acknowledged, got "
                    + StorageItemUtils.getState(result)), 0);
            }
            if (!ItemDeviceUtils.getItemDeviceData(result).getCompoundOrEmpty(DEVICE_KEY).read(HANDLE_TAG_NAME, UUIDUtil.CODEC).isPresent()) {
                throw new GameTestAssertException(Component.literal("reset of an unverified drive should keep the handle"), 0);
            }

            recipe.getRemainingItems(input);
            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException(Component.literal("...and it must not delete the blob either"), 0);
            }
        } catch (final IOException e) {
            throw new GameTestAssertException(Component.literal("Unexpected failure: " + e), 0);
        } finally {
            StorageItemUtils.clearBlobData(unverified);
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void resetKeepsCapacity(final GameTestHelper helper) {
        final ResetRecipe recipe = new ResetRecipe(CraftingBookCategory.MISC);

        final HardDriveItem item = Items.HARD_DRIVE_LARGE.get();
        final ItemStack corrupted = item.withCapacity(new ItemStack(item), 2 * 1024 * 1024);
        StorageItemUtils.setState(corrupted, State.CORRUPTED);

        final ItemStack result = recipe.assemble(
            gridOf(corrupted, new ItemStack(Items.WRENCH.get())), helper.getLevel().registryAccess());

        if (!result.is(item)) {
            throw new GameTestAssertException(Component.literal("Reset should hand back the same kind of drive"), 0);
        }
        if (item.getCapacity(result) != 2 * 1024 * 1024) {
            throw new GameTestAssertException(Component.literal("Reset must keep the drive's capacity, got "
                + item.getCapacity(result)), 0);
        }
        if (isCorrupted(result)) {
            throw new GameTestAssertException(Component.literal("Reset should hand back a drive that is no longer corrupted"), 0);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void toolRecipeStepsAsideForCorruptedOrUnverifiedDrives(final GameTestHelper helper) {
        final ToolRecipe recipe = new ToolRecipe(new ShapelessRecipe("", CraftingBookCategory.MISC,
            new ItemStack(Items.HARD_DRIVE_LARGE.get()),
            List.of(
                Ingredient.of(Items.WRENCH.get()),
                Ingredient.of(Items.HARD_DRIVE_LARGE.get()))));

        final ItemStack healthy = Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.SEDNA_LINUX.getId());
        if (!recipe.matches(gridOf(healthy, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException(Component.literal("Converting a healthy drive should still work"), 0);
        }

        final ItemStack corrupted = Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.SEDNA_LINUX.getId());
        StorageItemUtils.setState(corrupted, State.CORRUPTED);
        if (recipe.matches(gridOf(corrupted, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException(Component.literal("tool recipe should not match a corrupted drive"), 0);
        }

        final ItemStack unverified = Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.SEDNA_LINUX.getId());
        StorageItemUtils.setState(unverified, State.INCONSISTENT);
        if (recipe.matches(gridOf(unverified, new ItemStack(Items.WRENCH.get())), helper.getLevel())) {
            throw new GameTestAssertException(Component.literal("tool recipe should not match an unverified drive"), 0);
        }

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static CraftingInput gridOf(final ItemStack... stacks) {
        return CraftingInput.of(stacks.length, 1, List.of(stacks));
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
            throw new GameTestAssertException(Component.literal("Blob storage is not initialized"), 0);
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
