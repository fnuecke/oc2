package li.cil.oc2.common.serialization;

import com.google.gson.*;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.*;

import javax.annotation.Nullable;

public final class NBTToJsonConverter {
    public static JsonElement convert(@Nullable final INBT tag) {
        if (tag == null) {
            return JsonNull.INSTANCE;
        }

        switch (tag.getId()) {
            case NBTTagIds.TAG_BYTE: {
                return new JsonPrimitive(((ByteNBT) tag).getByte());
            }
            case NBTTagIds.TAG_SHORT: {
                return new JsonPrimitive(((ShortNBT) tag).getShort());
            }
            case NBTTagIds.TAG_INT: {
                return new JsonPrimitive(((IntNBT) tag).getInt());
            }
            case NBTTagIds.TAG_LONG: {
                return new JsonPrimitive(((LongNBT) tag).getLong());
            }
            case NBTTagIds.TAG_FLOAT: {
                return new JsonPrimitive(((FloatNBT) tag).getFloat());
            }
            case NBTTagIds.TAG_DOUBLE: {
                return new JsonPrimitive(((DoubleNBT) tag).getDouble());
            }
            case NBTTagIds.TAG_BYTE_ARRAY: {
                final JsonArray json = new JsonArray();
                final byte[] array = ((ByteArrayNBT) tag).getByteArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            case NBTTagIds.TAG_STRING: {
                return new JsonPrimitive(tag.getString());
            }
            case NBTTagIds.TAG_LIST: {
                final JsonArray json = new JsonArray();
                final ListNBT listTag = (ListNBT) tag;
                for (final INBT item : listTag) {
                    json.add(convert(item));
                }
                return json;
            }
            case NBTTagIds.TAG_COMPOUND: {
                final JsonObject json = new JsonObject();
                final CompoundNBT compoundTag = (CompoundNBT) tag;
                for (final String key : compoundTag.keySet()) {
                    json.add(key, convert(compoundTag.get(key)));
                }
                return json;
            }
            case NBTTagIds.TAG_INT_ARRAY: {
                final JsonArray json = new JsonArray();
                final int[] array = ((IntArrayNBT) tag).getIntArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            case NBTTagIds.TAG_LONG_ARRAY: {
                final JsonArray json = new JsonArray();
                final long[] array = ((LongArrayNBT) tag).getAsLongArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            default: {
                return JsonNull.INSTANCE;
            }
        }
    }
}
