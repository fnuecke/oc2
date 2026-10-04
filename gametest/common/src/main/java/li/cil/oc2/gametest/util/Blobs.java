/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.util;

import li.cil.oc2.common.serialization.BlobReference;
import li.cil.oc2.common.serialization.BlobStorage;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.StorageItemUtils;
import li.cil.oc2.common.util.StorageItemUtils.State;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class Blobs {
    public static final String DEVICE_KEY = "oc2:hard_drive";
    public static final String HANDLE_TAG_NAME = "blob";

    public static UUID createBlob(final BlobReference blob) throws IOException {
        blob.open();
        return requireNonNull(blob.getHandle());
    }

    public static CompoundTag tagReferencing(final UUID handle) {
        final CompoundTag tag = new CompoundTag();
        tag.putUUID(HANDLE_TAG_NAME, handle);
        return tag;
    }

    public static ItemStack driveReferencing(final ItemStack drive, final UUID handle) {
        final CompoundTag deviceData = new CompoundTag();
        deviceData.put(DEVICE_KEY, tagReferencing(handle));
        ItemDeviceUtils.setItemDeviceData(drive, deviceData);
        return drive;
    }

    public static void markStale(final UUID handle) throws IOException {
        final Path directory = BlobStorage.getDataDirectory();
        if (directory == null) {
            throw new GameTestAssertException("Blob storage is not initialized");
        }

        Files.createFile(directory.resolve(handle.toString()));
        Files.createFile(directory.resolve(handle + ".dirty"));
    }

    public static boolean isCorrupted(final ItemStack stack) {
        return StorageItemUtils.getState(stack) == State.CORRUPTED;
    }

    // --------------------------------------------------------------------- //

    private Blobs() {
    }
}
