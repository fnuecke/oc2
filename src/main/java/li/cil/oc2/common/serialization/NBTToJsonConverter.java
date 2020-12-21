package li.cil.oc2.common.serialization;

import com.google.gson.*;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

public final class NBTToJsonConverter {
    public static JsonElement convert(@Nullable final Tag nbt) {
        if (nbt == null) {
            return JsonNull.INSTANCE;
        }

        switch (nbt.getType()) {
            case NBTTagIds.TAG_BYTE: {
                return new JsonPrimitive(((ByteTag) nbt).getByte());
            }
            case NBTTagIds.TAG_SHORT: {
                return new JsonPrimitive(((ShortTag) nbt).getShort());
            }
            case NBTTagIds.TAG_INT: {
                return new JsonPrimitive(((IntTag) nbt).getInt());
            }
            case NBTTagIds.TAG_LONG: {
                return new JsonPrimitive(((LongTag) nbt).getLong());
            }
            case NBTTagIds.TAG_FLOAT: {
                return new JsonPrimitive(((FloatTag) nbt).getFloat());
            }
            case NBTTagIds.TAG_DOUBLE: {
                return new JsonPrimitive(((DoubleTag) nbt).getDouble());
            }
            case NBTTagIds.TAG_BYTE_ARRAY: {
                final JsonArray json = new JsonArray();
                final byte[] array = ((ByteArrayTag) nbt).getByteArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            case NBTTagIds.TAG_STRING: {
                return new JsonPrimitive(nbt.asString());
            }
            case NBTTagIds.TAG_LIST: {
                final JsonArray json = new JsonArray();
                final ListTag list = (ListTag) nbt;
                for (final Tag item : list) {
                    json.add(convert(item));
                }
                return json;
            }
            case NBTTagIds.TAG_COMPOUND: {
                final JsonObject json = new JsonObject();
                final CompoundTag compound = (CompoundTag) nbt;
                for (final String key : compound.getKeys()) {
                    json.add(key, convert(compound.get(key)));
                }
                return json;
            }
            case NBTTagIds.TAG_INT_ARRAY: {
                final JsonArray json = new JsonArray();
                final int[] array = ((IntArrayTag) nbt).getIntArray();
                for (int i = 0; i < array.length; i++) {
                    json.add(array[i]);
                }
                return json;
            }
            case NBTTagIds.TAG_LONG_ARRAY: {
                final JsonArray json = new JsonArray();
                final long[] array = ((LongArrayTag) nbt).getLongArray();
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
