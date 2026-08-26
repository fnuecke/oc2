/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import com.google.gson.GsonBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import li.cil.oc2.api.bus.device.rpc.RPCTypeAdapter;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.serialization.gson.DirectionJsonSerializer;
import li.cil.oc2.common.serialization.gson.ItemStackJsonSerializer;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class RPCTypeAdapters {
    private static Iterable<RPCTypeAdapter> adapters = List.of();

    // --------------------------------------------------------------------- //

    public static void initialize() {
        adapters = RegistryUtils.builder(Registries.RPC_TYPE_ADAPTER).build();

        final DeferredRegister<RPCTypeAdapter> typeAdapters = RegistryUtils.getInitializerFor(Registries.RPC_TYPE_ADAPTER);
        register(typeAdapters, "item_stack", ItemStack.class, new ItemStackJsonSerializer());
        register(typeAdapters, "direction", Direction.class, new DirectionJsonSerializer());
    }

    public static GsonBuilder beginBuildGson() {
        final GsonBuilder builder = new GsonBuilder();

        for (final RPCTypeAdapter value : adapters) {
            builder.registerTypeAdapter(value.type(), value.typeAdapter());
        }

        return builder;
    }

    // --------------------------------------------------------------------- //

    private static void register(final DeferredRegister<RPCTypeAdapter> registry, final String name,
                                 final Class<?> type, final Object typeAdapter) {
        registry.register(name, () -> new RPCTypeAdapter(type, typeAdapter));
    }

    private RPCTypeAdapters() {
    }
}
