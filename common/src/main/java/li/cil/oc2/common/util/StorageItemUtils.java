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
    private static final String STATE_TAG_NAME = "state";
    private static final String BLOB_HANDLE_TAG_NAME = "blob";
    public static final String IMAGE_TAG_NAME = "image";

    public enum State {
        OK,
        INCONSISTENT,
        ACKNOWLEDGED,
        CORRUPTED,
    }

    // --------------------------------------------------------------------- //

    public static State getState(final ItemStack stack) {
        final State state = NBTUtils.getEnum(ItemStackUtils.getModDataTag(stack), STATE_TAG_NAME, State.class);
        return state != null ? state : State.OK;
    }

    public static void setState(final ItemStack stack, final State state) {
        if (stack.isEmpty() || getState(stack) == state) {
            return;
        }

        ItemStackUtils.modifyModDataTag(stack, tag -> {
            if (state == State.OK) {
                tag.remove(STATE_TAG_NAME);
            } else {
                NBTUtils.putEnum(tag, STATE_TAG_NAME, state);
            }
        });
    }

    public static boolean needsRepair(final ItemStack stack) {
        final State state = getState(stack);
        return state == State.CORRUPTED || state == State.INCONSISTENT;
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

    // --------------------------------------------------------------------- //

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

            tag.remove(STATE_TAG_NAME);
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
