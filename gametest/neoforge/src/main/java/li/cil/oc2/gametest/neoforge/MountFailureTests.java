/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.vm.item.FlashStorageDevice;
import li.cil.oc2.common.bus.device.vm.item.HardDriveDevice;
import li.cil.oc2.common.bus.device.vm.item.MemoryDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.ResetRecipe;
import li.cil.oc2.common.item.crafting.ToolRecipe;
import li.cil.oc2.common.serialization.BlobReference;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.oc2.common.util.StorageItemUtils.State;
import li.cil.oc2.common.vm.VMDeviceBusAdapter;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.sedna.riscv.R5Board;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
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
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class MountFailureTests {
    @GameTest(template = TEMPLATE)
    public static void missingMemoryBlobReleasesHandle(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        final VMDeviceBusAdapter adapter = adapter();
        final MemoryDevice device = new MemoryDevice(new ItemStack(Items.MEMORY_SMALL.get()), 4096);

        device.deserializeNBT(tagReferencing(handle));
        adapter.addDevices(List.of(device));

        final VMDeviceLoadResult first = adapter.mountDevices();
        if (first.wasSuccessful()) {
            throw new GameTestAssertException("Mounting memory whose blob is gone should fail");
        }
        if (!first.isPermanent()) {
            throw new GameTestAssertException("missing blob should fail permanently");
        }

        if (!adapter.mountDevices().wasSuccessful()) {
            throw new GameTestAssertException("memory should release a handle it cannot open");
        }

        adapter.disposeDevices();
        release(handle);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void memoryBlobInUseIsPermanent(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);

            final VMDeviceBusAdapter adapter = adapter();
            final MemoryDevice device = new MemoryDevice(new ItemStack(Items.MEMORY_SMALL.get()), 4096);
            device.deserializeNBT(tagReferencing(handle));
            adapter.addDevices(List.of(device));

            final VMDeviceLoadResult result = adapter.mountDevices();
            if (result.wasSuccessful() || !result.isPermanent()) {
                throw new GameTestAssertException("memory blob in use should fail permanently");
            }

            adapter.disposeDevices();
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void missingDriveBlobKeepsHandle(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        final ItemStack stack = new ItemStack(Items.HARD_DRIVE_SMALL.get());
        final VMDeviceBusAdapter adapter = adapter();
        final HardDriveDevice device = new HardDriveDevice(stack, 4096, false, Optional::empty);

        device.deserializeNBT(tagReferencing(handle));
        adapter.addDevices(List.of(device));

        final VMDeviceLoadResult result = adapter.mountDevices();
        if (result.wasSuccessful() || !result.isPermanent()) {
            throw new GameTestAssertException("missing drive blob should fail permanently");
        }
        if (!isCorrupted(stack)) {
            throw new GameTestAssertException("drive should keep its handle and be flagged");
        }

        adapter.disposeDevices();
        StorageItemUtils.clearBlobData(stack);
        release(handle);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void driveBlobInUseStaysWithItsHolder(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        final ItemStack stack = new ItemStack(Items.HARD_DRIVE_SMALL.get());
        try {
            final UUID handle = createBlob(blob);

            final VMDeviceBusAdapter adapter = adapter();
            final HardDriveDevice device = new HardDriveDevice(stack, 4096, false, Optional::empty);
            device.deserializeNBT(tagReferencing(handle));
            adapter.addDevices(List.of(device));

            final VMDeviceLoadResult result = adapter.mountDevices();
            if (result.wasSuccessful() || !result.isPermanent() || !isCorrupted(stack)) {
                throw new GameTestAssertException("A drive whose blob another device holds is a duplicate");
            }

            adapter.disposeDevices();
            if (!BlobStorage.isOpen(handle)) {
                throw new GameTestAssertException("failed mount released the blob of the device holding it");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            StorageItemUtils.clearBlobData(stack);
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fullStorageIsRetryable(final GameTestHelper helper) {
        final int maxBlobCount = Config.maxBlobCount;
        final int graceHours = Config.blobEvictionGraceHours;
        final ItemStack stack = new ItemStack(Items.HARD_DRIVE_SMALL.get());
        try {
            Config.blobEvictionGraceHours = 24 * 365;
            Config.maxBlobCount = Math.max(1, BlobStorage.getBlobCount());

            final VMDeviceBusAdapter adapter = adapter();
            adapter.addDevices(List.of(new HardDriveDevice(stack, 4096, false, Optional::empty)));

            final VMDeviceLoadResult result = adapter.mountDevices();
            if (result.wasSuccessful()) {
                throw new GameTestAssertException("Mounting should fail while blob storage is full");
            }
            if (result.isPermanent()) {
                throw new GameTestAssertException("full storage should be retryable");
            }
            if (isCorrupted(stack)) {
                throw new GameTestAssertException("full storage should not flag the drive");
            }

            adapter.disposeDevices();
        } finally {
            Config.maxBlobCount = maxBlobCount;
            Config.blobEvictionGraceHours = graceHours;
            StorageItemUtils.clearBlobData(stack);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void staleFlashNeedsAcknowledgement(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        final ItemStack stack = new ItemStack(Items.FLASH_MEMORY.get());
        try {
            markStale(handle);

            final FlashStorageDevice device = new FlashStorageDevice(stack, 4096);
            device.deserializeNBT(tagReferencing(handle));

            final VMDeviceBusAdapter refusing = adapter();
            refusing.addDevices(List.of(device));

            final VMDeviceLoadResult refused = refusing.mountDevices();
            if (refused.wasSuccessful() || !refused.isPermanent()) {
                throw new GameTestAssertException("stale flash memory should not mount unacknowledged");
            }
            if (StorageItemUtils.getState(stack) != State.INCONSISTENT) {
                throw new GameTestAssertException("The item carries the warning; got "
                    + StorageItemUtils.getState(stack));
            }

            StorageItemUtils.setState(stack, State.ACKNOWLEDGED);

            final VMDeviceBusAdapter accepting = adapter();
            final FlashStorageDevice retry = new FlashStorageDevice(stack, 4096);
            retry.deserializeNBT(tagReferencing(handle));
            accepting.addDevices(List.of(retry));

            if (!accepting.mountDevices().wasSuccessful()) {
                throw new GameTestAssertException("acknowledged flash memory should mount");
            }
            if (StorageItemUtils.getState(stack) != State.OK) {
                throw new GameTestAssertException("acknowledgement should be spent on the mount that uses it; got " + StorageItemUtils.getState(stack));
            }

            accepting.disposeDevices();
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            StorageItemUtils.clearBlobData(stack);
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void staleDriveNeedsAcknowledgement(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        final ItemStack stack = new ItemStack(Items.HARD_DRIVE_SMALL.get());
        try {
            markStale(handle);

            final HardDriveDevice device = new HardDriveDevice(stack, 4096, false, Optional::empty);
            device.deserializeNBT(tagReferencing(handle));

            final VMDeviceBusAdapter refusing = adapter();
            refusing.addDevices(List.of(device));

            final VMDeviceLoadResult refused = refusing.mountDevices();
            if (refused.wasSuccessful() || !refused.isPermanent()) {
                throw new GameTestAssertException("stale drive should not mount unacknowledged");
            }
            if (StorageItemUtils.getState(stack) != State.INCONSISTENT) {
                throw new GameTestAssertException("The item carries the warning, so it survives the player "
                    + "taking the drive out; got " + StorageItemUtils.getState(stack));
            }
            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException("The data is kept: it is what the guest actually wrote");
            }

            StorageItemUtils.setState(stack, State.ACKNOWLEDGED);

            final VMDeviceBusAdapter accepting = adapter();
            final HardDriveDevice retry = new HardDriveDevice(stack, 4096, false, Optional::empty);
            retry.deserializeNBT(tagReferencing(handle));
            accepting.addDevices(List.of(retry));

            if (!accepting.mountDevices().wasSuccessful()) {
                throw new GameTestAssertException("Once the player has accepted it, the drive mounts");
            }
            if (StorageItemUtils.getState(stack) != State.OK) {
                throw new GameTestAssertException("acknowledgement should be spent on the mount that uses it");
            }

            accepting.disposeDevices();
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            StorageItemUtils.clearBlobData(stack);
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void staleMemoryIsDiscarded(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        try {
            markStale(handle);

            final VMDeviceBusAdapter adapter = adapter();
            final MemoryDevice device = new MemoryDevice(new ItemStack(Items.MEMORY_SMALL.get()), 4096);
            device.deserializeNBT(tagReferencing(handle));
            adapter.addDevices(List.of(device));

            final VMDeviceLoadResult result = adapter.mountDevices();
            if (result.wasSuccessful() || !result.isPermanent()) {
                throw new GameTestAssertException("stale memory should stop the machine");
            }
            if (BlobStorage.exists(handle)) {
                throw new GameTestAssertException("discarded memory should not keep its blob");
            }

            if (!adapter.mountDevices().wasSuccessful()) {
                throw new GameTestAssertException("The machine has to be startable again afterwards");
            }

            adapter.disposeDevices();
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void resetAcknowledgesUnverifiedDrive(final GameTestHelper helper) {
        final ResetRecipe recipe = new ResetRecipe(CraftingBookCategory.MISC);
        final BlobReference blob = new BlobReference();
        final ItemStack drive = new ItemStack(Items.HARD_DRIVE_SMALL.get());
        try {
            final UUID handle = createBlob(blob);
            blob.close();
            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException("Precondition: the drive has data to keep");
            }

            final CompoundTag deviceData = new CompoundTag();
            deviceData.put("oc2:hard_drive", tagReferencing(handle));
            ItemDeviceUtils.setItemDeviceData(drive, deviceData);
            StorageItemUtils.setState(drive, State.INCONSISTENT);

            final CraftingInput input = CraftingInput.of(2, 1,
                List.of(drive, new ItemStack(Items.WRENCH.get())));

            if (!recipe.matches(input, helper.getLevel())) {
                throw new GameTestAssertException("Accepting an unverified drive is the same craft as "
                    + "resetting a corrupted one");
            }

            final ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
            if (StorageItemUtils.getState(result) != State.ACKNOWLEDGED) {
                throw new GameTestAssertException("The craft moves the drive to acknowledged, got "
                    + StorageItemUtils.getState(result));
            }
            if (!ItemDeviceUtils.getItemDeviceData(result).getCompound("oc2:hard_drive").hasUUID("blob")) {
                throw new GameTestAssertException("reset of an unverified drive should keep the handle");
            }

            recipe.getRemainingItems(input);
            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException("...and it must not delete the blob either");
            }
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            StorageItemUtils.clearBlobData(drive);
            release(blob);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolRecipeStepsAsideForUnverifiedDrives(final GameTestHelper helper) {
        final ToolRecipe recipe = new ToolRecipe(new ShapelessRecipe("", CraftingBookCategory.MISC,
            new ItemStack(Items.HARD_DRIVE_LARGE.get()),
            NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(Items.WRENCH.get()),
                Ingredient.of(Items.HARD_DRIVE_LARGE.get()))));

        final ItemStack drive = Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId());
        StorageItemUtils.setState(drive, State.INCONSISTENT);

        if (recipe.matches(CraftingInput.of(2, 1, List.of(drive, new ItemStack(Items.WRENCH.get()))),
            helper.getLevel())) {
            throw new GameTestAssertException("tool recipe should not match an unverified drive");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void permanentMountFailureStopsComputer(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        final ItemStack drive = new ItemStack(Items.HARD_DRIVE_SMALL.get());
        final CompoundTag driveData = new CompoundTag();
        driveData.putUUID("blob", BlobStorage.allocateHandle());
        final CompoundTag deviceData = new CompoundTag();
        deviceData.put("oc2:hard_drive", driveData);
        ItemDeviceUtils.setItemDeviceData(drive, deviceData);

        helper.startSequence()
            .thenExecuteAfter(20, () -> computer
                .install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
                .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
                .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_SMALL.get()))
                .install(DeviceTypes.HARD_DRIVE.get(), drive))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(100, () -> {
                computer.assertRunState(VMRunState.STOPPED, "a drive whose data is gone should fail permanently");
                assertBootError(computer, "gui.oc2.computer.error.storage_corrupted");
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void assertBootError(final ComputerFixture computer, final String key) {
        final Component error = computer.virtualMachine().getBootError();
        if (error == null) {
            throw new GameTestAssertException("Expected boot error [" + key + "], got none");
        }
        if (!(error.getContents() instanceof final TranslatableContents contents) || !key.equals(contents.getKey())) {
            throw new GameTestAssertException("Expected boot error [" + key + "], got [" + error.getString() + "]");
        }
    }

    private static VMDeviceBusAdapter adapter() {
        final R5Board board = new R5Board();
        return new VMDeviceBusAdapter(new GlobalVMContext(board, board.getCpu(), () -> {
        }, null), unused -> OptionalLong.empty());
    }

    private static CompoundTag tagReferencing(final UUID handle) {
        final CompoundTag tag = new CompoundTag();
        tag.putUUID("blob", handle);
        return tag;
    }

    private static void markStale(final UUID handle) throws IOException {
        final Path directory = BlobStorage.getDataDirectory();
        if (directory == null) {
            throw new GameTestAssertException("Blob storage is not initialized");
        }

        Files.createFile(directory.resolve(handle.toString()));
        Files.createFile(directory.resolve(handle + ".dirty"));
    }

    private static void release(final BlobReference blob) {
        final UUID handle = blob.getHandle();
        blob.close();
        if (handle != null) {
            release(handle);
        }
    }

    private static void release(final UUID handle) {
        BlobStorage.delete(handle);
        BlobStorage.handleSaved();

        final Path directory = BlobStorage.getDataDirectory();
        if (directory != null) {
            try {
                Files.deleteIfExists(directory.resolve(handle + ".dirty"));
            } catch (final IOException e) {
                throw new GameTestAssertException("Failed cleaning up: " + e);
            }
        }
    }

    private static boolean isCorrupted(final ItemStack stack) {
        return StorageItemUtils.getState(stack) == State.CORRUPTED;
    }

    // --------------------------------------------------------------------- //

    private MountFailureTests() {
    }
}
