package li.cil.oc2.common.serialization;

import com.google.gson.*;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.*;

import javax.annotation.Nullable;

public final class NBTToJsonConverter {
    public static JsonElement convert(@Nullable final INBT nbt) {
        if (nbt == null) {
            return JsonNull.INSTANCE;
        }

        switch (nbt.getId()) {
            case NBTTagIds.TAG_BYTE: {
                return new JsonPrimitive(((ByteNBT) nbt).getByte());
            }
            case NBTTagIds.TAG_SHORT: {
                return new JsonPrimitive(((ShortNBT) nbt).getShort());
            }
            case NBTTagIds.TAG_INT: {
                return new JsonPrimitive(((IntNBT) nbt).getInt());
            }
            case NBTTagIds.TAG_LONG: {
                return new JsonPrimitive(((LongNBT) nbt).getLong());
            }
            case NBTTagIds.TAG_FLOAT: {
                return new JsonPrimitive(((FloatNBT) nbt).getFloat());
            }
            case NBTTagIds.TAG_DOUBLE: {
                return new JsonPrimitive(((DoubleNBT) nbt).getDouble());
            }
            case NBTTagIds.TAG_BYTE_ARRAY: {
                final JsonArray json = new JsonArray();
                final byte[] array = ((ByteArrayNBT) nbt).getByteArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            case NBTTagIds.TAG_STRING: {
                return new JsonPrimitive(nbt.getString());
            }
            case NBTTagIds.TAG_LIST: {
                final JsonArray json = new JsonArray();
                final ListNBT list = (ListNBT) nbt;
                for (final INBT item : list) {
                    json.add(convert(item));
                }
                return json;
            }
            case NBTTagIds.TAG_COMPOUND: {
                final JsonObject json = new JsonObject();
                final CompoundNBT compound = (CompoundNBT) nbt;
                for (final String key : compound.keySet()) {
                    json.add(key, convert(compound.get(key)));
                }
                return json;
            }
            case NBTTagIds.TAG_INT_ARRAY: {
                final JsonArray json = new JsonArray();
                final int[] array = ((IntArrayNBT) nbt).getIntArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            case NBTTagIds.TAG_LONG_ARRAY: {
                final JsonArray json = new JsonArray();
                final long[] array = ((LongArrayNBT) nbt).getAsLongArray();
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
