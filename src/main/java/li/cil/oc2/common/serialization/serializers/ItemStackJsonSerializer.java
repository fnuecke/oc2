package li.cil.oc2.common.serialization.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import li.cil.oc2.common.bus.device.rpc.RPCItemStackTagFilters;
import li.cil.oc2.common.serialization.NBTToJsonConverter;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Type;

public final class ItemStackJsonSerializer implements JsonSerializer<ItemStack> {
    @Override
    public JsonElement serialize(final ItemStack src, final Type typeOfSrc, final JsonSerializationContext context) {
        if (src == null || src.isEmpty()) {
            return JsonNull.INSTANCE;
        }

        return NBTToJsonConverter.convert(RPCItemStackTagFilters.getFilteredTag(src, src.serializeNBT()));
    }
}
