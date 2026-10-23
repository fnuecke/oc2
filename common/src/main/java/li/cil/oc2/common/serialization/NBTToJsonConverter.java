/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization;

import com.google.gson.*;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.*;

import javax.annotation.Nullable;

public final class NBTToJsonConverter {
    public static JsonElement convert(@Nullable final Tag tag) {
        if (tag == null) {
            return JsonNull.INSTANCE;
        }

        switch (tag.getId()) {
            case NBTTagIds.TAG_BYTE -> {
                return new JsonPrimitive(((ByteTag) tag).value());
            }
            case NBTTagIds.TAG_SHORT -> {
                return new JsonPrimitive(((ShortTag) tag).value());
            }
            case NBTTagIds.TAG_INT -> {
                return new JsonPrimitive(((IntTag) tag).value());
            }
            case NBTTagIds.TAG_LONG -> {
                return new JsonPrimitive(((LongTag) tag).value());
            }
            case NBTTagIds.TAG_FLOAT -> {
                return new JsonPrimitive(((FloatTag) tag).value());
            }
            case NBTTagIds.TAG_DOUBLE -> {
                return new JsonPrimitive(((DoubleTag) tag).value());
            }
            case NBTTagIds.TAG_BYTE_ARRAY -> {
                final JsonArray json = new JsonArray();
                final byte[] array = ((ByteArrayTag) tag).getAsByteArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            case NBTTagIds.TAG_STRING -> {
                return new JsonPrimitive(((StringTag) tag).value());
            }
            case NBTTagIds.TAG_LIST -> {
                final JsonArray json = new JsonArray();
                final ListTag listTag = (ListTag) tag;
                for (final Tag item : listTag) {
                    json.add(convert(item));
                }
                return json;
            }
            case NBTTagIds.TAG_COMPOUND -> {
                final JsonObject json = new JsonObject();
                final CompoundTag compoundTag = (CompoundTag) tag;
                for (final String key : compoundTag.keySet()) {
                    json.add(key, convert(compoundTag.get(key)));
                }
                return json;
            }
            case NBTTagIds.TAG_INT_ARRAY -> {
                final JsonArray json = new JsonArray();
                final int[] array = ((IntArrayTag) tag).getAsIntArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            case NBTTagIds.TAG_LONG_ARRAY -> {
                final JsonArray json = new JsonArray();
                final long[] array = ((LongArrayTag) tag).getAsLongArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            default -> {
                return JsonNull.INSTANCE;
            }
        }
    }
}
