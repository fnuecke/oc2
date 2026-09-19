/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.UUID;

public final class BlobReference {
    public static final String TAG_NAME = "blob";

    // --------------------------------------------------------------------- //

    @Nullable
    private UUID handle;
    @Nullable
    private FileChannel channel;

    // --------------------------------------------------------------------- //

    public boolean isValid() {
        return BlobStorage.isValidHandle(handle);
    }

    public boolean isStale() {
        return BlobStorage.isStaleHandle(handle);
    }

    @Nullable
    public UUID getHandle() {
        return handle;
    }

    public FileChannel open() throws IOException {
        if (channel != null) {
            throw new IllegalStateException("Blob [" + handle + "] is already open.");
        }

        final UUID current = handle;
        final boolean isNew = current == null || !isValid();
        final UUID target = isNew ? BlobStorage.allocateHandle() : current;
        channel = BlobStorage.open(target, isNew);
        handle = target;

        return channel;
    }

    public void close() {
        if (channel != null) {
            BlobStorage.close(channel);
            channel = null;
        }
    }

    public void release() {
        close();
        handle = null;
    }

    public void delete() {
        close();
        if (handle != null) {
            BlobStorage.delete(handle);
            handle = null;
        }
    }

    public void writeTo(final CompoundTag tag) {
        if (handle != null) {
            tag.putUUID(TAG_NAME, handle);
        }
    }

    public void readFrom(final CompoundTag tag) {
        if (tag.hasUUID(TAG_NAME)) {
            handle = tag.getUUID(TAG_NAME);
        }
    }
}
