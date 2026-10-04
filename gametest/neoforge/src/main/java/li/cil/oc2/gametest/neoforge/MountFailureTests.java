/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.item.FlashStorageDevice;
import li.cil.oc2.common.bus.device.item.HardDriveDevice;
import li.cil.oc2.common.bus.device.item.MemoryDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.serialization.BlobReference;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.oc2.common.util.StorageItemUtils.State;
import li.cil.oc2.common.vm.VMDeviceRegistry;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.sedna.riscv.R5Board;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static li.cil.oc2.gametest.util.Blobs.*;
import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class MountFailureTests {
    @GameTest(template = TEMPLATE)
    public static void missingMemoryBlobReleasesHandle(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        final VMDeviceRegistry registry = registry();
        final MemoryDevice device = new MemoryDevice(new ItemStack(Items.MEMORY_SMALL.get()), 4096);

        device.deserializeNBT(tagReferencing(handle));
        registry.rebuild(bus(device));

        final VMDeviceLoadResult first = registry.mountDevices();
        if (first.wasSuccessful()) {
            throw new GameTestAssertException("Mounting memory whose blob is gone should fail");
        }
        if (!first.isPermanent()) {
            throw new GameTestAssertException("missing blob should fail permanently");
        }

        if (!registry.mountDevices().wasSuccessful()) {
            throw new GameTestAssertException("memory should release a handle it cannot open");
        }

        registry.unmountDevices();
        device.dispose();
        release(handle);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void memoryBlobInUseIsPermanent(final GameTestHelper helper) {
        final BlobReference blob = new BlobReference();
        try {
            final UUID handle = createBlob(blob);

            final VMDeviceRegistry registry = registry();
            final MemoryDevice device = new MemoryDevice(new ItemStack(Items.MEMORY_SMALL.get()), 4096);
            device.deserializeNBT(tagReferencing(handle));
            registry.rebuild(bus(device));

            final VMDeviceLoadResult result = registry.mountDevices();
            if (result.wasSuccessful() || !result.isPermanent()) {
                throw new GameTestAssertException("memory blob in use should fail permanently");
            }

            registry.unmountDevices();
            device.dispose();
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
        final VMDeviceRegistry registry = registry();
        final HardDriveDevice device = new HardDriveDevice(stack, 4096, false, Optional::empty);

        device.deserializeNBT(tagReferencing(handle));
        registry.rebuild(bus(device));

        final VMDeviceLoadResult result = registry.mountDevices();
        if (result.wasSuccessful() || !result.isPermanent()) {
            throw new GameTestAssertException("missing drive blob should fail permanently");
        }
        if (!isCorrupted(stack)) {
            throw new GameTestAssertException("drive should keep its handle and be flagged");
        }

        registry.unmountDevices();
        device.dispose();
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

            final VMDeviceRegistry registry = registry();
            final HardDriveDevice device = new HardDriveDevice(stack, 4096, false, Optional::empty);
            device.deserializeNBT(tagReferencing(handle));
            registry.rebuild(bus(device));

            final VMDeviceLoadResult result = registry.mountDevices();
            if (result.wasSuccessful() || !result.isPermanent() || !isCorrupted(stack)) {
                throw new GameTestAssertException("A drive whose blob another device holds is a duplicate");
            }

            registry.unmountDevices();
            device.dispose();
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

            final VMDeviceRegistry registry = registry();
            final HardDriveDevice device = new HardDriveDevice(stack, 4096, false, Optional::empty);
            registry.rebuild(bus(device));

            final VMDeviceLoadResult result = registry.mountDevices();
            if (result.wasSuccessful()) {
                throw new GameTestAssertException("Mounting should fail while blob storage is full");
            }
            if (result.isPermanent()) {
                throw new GameTestAssertException("full storage should be retryable");
            }
            if (isCorrupted(stack)) {
                throw new GameTestAssertException("full storage should not flag the drive");
            }

            registry.unmountDevices();
            device.dispose();
        } finally {
            Config.maxBlobCount = maxBlobCount;
            Config.blobEvictionGraceHours = graceHours;
            StorageItemUtils.clearBlobData(stack);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void staleStorageNeedsAcknowledgement(final GameTestHelper helper) {
        assertStaleStorageNeedsAcknowledgement(new ItemStack(Items.FLASH_MEMORY.get()),
            stack -> new FlashStorageDevice(stack, 4096));
        assertStaleStorageNeedsAcknowledgement(new ItemStack(Items.HARD_DRIVE_SMALL.get()),
            stack -> new HardDriveDevice(stack, 4096, false, Optional::empty));

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void staleMemoryIsDiscarded(final GameTestHelper helper) {
        final UUID handle = BlobStorage.allocateHandle();
        try {
            markStale(handle);

            final VMDeviceRegistry registry = registry();
            final MemoryDevice device = new MemoryDevice(new ItemStack(Items.MEMORY_SMALL.get()), 4096);
            device.deserializeNBT(tagReferencing(handle));
            registry.rebuild(bus(device));

            final VMDeviceLoadResult result = registry.mountDevices();
            if (result.wasSuccessful() || !result.isPermanent()) {
                throw new GameTestAssertException("stale memory should stop the machine");
            }
            if (BlobStorage.exists(handle)) {
                throw new GameTestAssertException("discarded memory should not keep its blob");
            }

            if (!registry.mountDevices().wasSuccessful()) {
                throw new GameTestAssertException("The machine has to be startable again afterwards");
            }

            registry.unmountDevices();
            device.dispose();
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            release(handle);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void permanentMountFailureStopsComputer(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.placePowered(helper, player);

        final ItemStack drive = driveReferencing(new ItemStack(Items.HARD_DRIVE_SMALL.get()), BlobStorage.allocateHandle());

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

    private static void assertStaleStorageNeedsAcknowledgement(final ItemStack stack,
                                                                final Function<ItemStack, Device> factory) {
        final UUID handle = BlobStorage.allocateHandle();
        try {
            markStale(handle);

            final Device device = factory.apply(stack);
            device.deserializeNBT(tagReferencing(handle));

            final VMDeviceRegistry refusing = registry();
            refusing.rebuild(bus(device));

            final VMDeviceLoadResult refused = refusing.mountDevices();
            if (refused.wasSuccessful() || !refused.isPermanent()) {
                throw new GameTestAssertException("stale " + stack + " should not mount unacknowledged");
            }
            if (StorageItemUtils.getState(stack) != State.INCONSISTENT) {
                throw new GameTestAssertException("The item carries the warning, so it survives the player "
                    + "taking it out; got " + StorageItemUtils.getState(stack));
            }
            if (!BlobStorage.exists(handle)) {
                throw new GameTestAssertException("The data is kept: it is what the guest actually wrote");
            }

            StorageItemUtils.setState(stack, State.ACKNOWLEDGED);

            final VMDeviceRegistry accepting = registry();
            final Device retry = factory.apply(stack);
            retry.deserializeNBT(tagReferencing(handle));
            accepting.rebuild(bus(retry));

            if (!accepting.mountDevices().wasSuccessful()) {
                throw new GameTestAssertException("Once the player has accepted it, " + stack + " mounts");
            }
            if (StorageItemUtils.getState(stack) != State.OK) {
                throw new GameTestAssertException("acknowledgement should be spent on the mount that uses it; got "
                    + StorageItemUtils.getState(stack));
            }

            accepting.unmountDevices();
            retry.dispose();
        } catch (final IOException e) {
            throw new GameTestAssertException("Unexpected failure: " + e);
        } finally {
            StorageItemUtils.clearBlobData(stack);
            release(handle);
        }
    }

    private static DeviceBusController bus(final Device device) {
        return new DeviceBusController() {
            @Override
            public Optional<ArchitectureType> getArchitectureType() {
                return Optional.empty();
            }

            @Override
            public void scheduleBusScan(final ScanReason reason) {
            }

            @Override
            public void scanDevices() {
            }

            @Override
            public Set<Device> getDevices() {
                return Set.of(device);
            }

            @Override
            public Set<UUID> getDeviceIdentifiers(final Device unused) {
                return Set.of();
            }
        };
    }

    private static VMDeviceRegistry registry() {
        final R5Board board = new R5Board();
        return new VMDeviceRegistry(new GlobalVMContext(board, board.getCpu(), () -> {
        }, null), unused -> OptionalLong.empty());
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

    // --------------------------------------------------------------------- //

    private MountFailureTests() {
    }
}
