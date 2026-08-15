/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import li.cil.oc2.common.serialization.BlobStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class StorageItemUtils {
    private static final String CORRUPTED_TAG_NAME = "corrupted";
    private static final String BLOB_HANDLE_TAG_NAME = "blob";
    private static final String DISK_DATA_TAG_NAME = "data";

    // ------------------------------------------------------------- //

    public static boolean isCorrupted(final ItemStack stack) {
        return ItemStackUtils.getModDataTag(stack).getBoolean(CORRUPTED_TAG_NAME);
    }

    public static void setCorrupted(final ItemStack stack) {
        if (stack.isEmpty() || isCorrupted(stack)) {
            return;
        }

        ItemStackUtils.modifyModDataTag(stack, tag -> tag.putBoolean(CORRUPTED_TAG_NAME, true));
    }

    public static void clearBlobData(final ItemStack stack) {
        final List<UUID> handles = new ArrayList<>();
        removeBlobData(stack, handles);

        for (final UUID handle : handles) {
            releaseBlob(handle);
        }
    }

    public static void stripBlobData(final ItemStack stack) {
        removeBlobData(stack, null);
    }

    // ------------------------------------------------------------- //

    private static void removeBlobData(final ItemStack stack, @Nullable final List<UUID> handles) {
        if (stack.isEmpty() || ItemStackUtils.getModDataTag(stack).isEmpty()) {
            return;
        }

        ItemStackUtils.modifyModDataTag(stack, tag -> {
            // Hard drives and other item devices: item_device -> <device type> -> blob
            if (tag.contains(ItemDeviceUtils.ITEM_DEVICE_DATA_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
                final CompoundTag deviceData = tag.getCompound(ItemDeviceUtils.ITEM_DEVICE_DATA_TAG_NAME);
                for (final String key : new ArrayList<>(deviceData.getAllKeys())) {
                    if (deviceData.contains(key, NBTTagIds.TAG_COMPOUND)) {
                        takeHandle(deviceData.getCompound(key), handles);
                    }
                }
            }

            // Floppy disks: data -> blob
            if (tag.contains(DISK_DATA_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
                takeHandle(tag.getCompound(DISK_DATA_TAG_NAME), handles);
            }

            tag.remove(CORRUPTED_TAG_NAME);
        });
    }

    private static void takeHandle(final CompoundTag tag, @Nullable final List<UUID> handles) {
        if (!tag.hasUUID(BLOB_HANDLE_TAG_NAME)) {
            return;
        }

        if (handles != null) {
            handles.add(tag.getUUID(BLOB_HANDLE_TAG_NAME));
        }

        tag.remove(BLOB_HANDLE_TAG_NAME);
    }

    private static void releaseBlob(final UUID handle) {
        // Note that this leaves the blob alone if a duplicate of this item has it mounted right now;
        // BlobStorage.delete takes care of that.
        BlobStorage.delete(handle);
    }
}
