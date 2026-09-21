/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serialization.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import li.cil.oc2.common.bus.device.rpc.RPCItemStackTagFilters;
import li.cil.oc2.common.serialization.NBTToJsonConverter;
import li.cil.oc2.common.util.ServerUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Type;

public final class ItemStackJsonSerializer implements JsonSerializer<ItemStack> {
    @Override
    public JsonElement serialize(final ItemStack src, final Type typeOfSrc, final JsonSerializationContext context) {
        if (src.isEmpty()) {
            return JsonNull.INSTANCE;
        }

        // The codec allows 1-99, some mod containers may have larger stacks, so work around that.
        final CompoundTag tag = (CompoundTag) src.copyWithCount(1).save(ServerUtils.getRegistryAccess());
        final JsonElement json = NBTToJsonConverter.convert(RPCItemStackTagFilters.getFilteredTag(src, tag));
        json.getAsJsonObject().addProperty("count", src.getCount());
        return json;
    }
}
