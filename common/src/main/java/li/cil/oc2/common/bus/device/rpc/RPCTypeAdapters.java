/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import com.google.gson.GsonBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import li.cil.oc2.api.bus.device.rpc.RPCTypeAdapter;
import li.cil.oc2.api.util.Registries;
import li.cil.oc2.common.serialization.gson.DirectionJsonSerializer;
import li.cil.oc2.common.serialization.gson.ItemStackJsonSerializer;
import li.cil.oc2.common.util.RegistryUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public final class RPCTypeAdapters {
    public static final Registrar<RPCTypeAdapter> REGISTRY =
        RegistryUtils.builder(Registries.RPC_TYPE_ADAPTER).build();
    private static final DeferredRegister<RPCTypeAdapter> TYPE_ADAPTERS =
        RegistryUtils.getInitializerFor(Registries.RPC_TYPE_ADAPTER);

    // --------------------------------------------------------------------- //

    public static void initialize() {
        register("item_stack", ItemStack.class, new ItemStackJsonSerializer());
        register("direction", Direction.class, new DirectionJsonSerializer());
    }

    public static GsonBuilder beginBuildGson() {
        final GsonBuilder builder = new GsonBuilder();

        for (final RPCTypeAdapter value : REGISTRY) {
            builder.registerTypeAdapter(value.type(), value.typeAdapter());
        }

        return builder;
    }

    // --------------------------------------------------------------------- //

    private static void register(final String name, final Class<?> type, final Object typeAdapter) {
        TYPE_ADAPTERS.register(name, () -> new RPCTypeAdapter(type, typeAdapter));
    }

    private RPCTypeAdapters() {
    }
}
