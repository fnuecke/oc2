package li.cil.oc2.common.bus.device.rpc;

import com.google.gson.GsonBuilder;
import li.cil.oc2.api.imc.RPCMethodParameterTypeAdapter;
import li.cil.oc2.common.serialization.serializers.DirectionJsonSerializer;
import li.cil.oc2.common.serialization.serializers.ItemStackJsonSerializer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;

import java.util.ArrayList;

public final class RPCMethodParameterTypeAdapters {
    private static final ArrayList<RPCMethodParameterTypeAdapter> TYPE_ADAPTERS = new ArrayList<>();

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        addTypeAdapter(ItemStack.class, new ItemStackJsonSerializer());
        addTypeAdapter(Direction.class, new DirectionJsonSerializer());
    }

    public static void addTypeAdapter(final Class<?> type, final Object typeAdapter) {
        addTypeAdapter(new RPCMethodParameterTypeAdapter(type, typeAdapter));
    }

    public static void addTypeAdapter(final RPCMethodParameterTypeAdapter value) {
        TYPE_ADAPTERS.add(value);
    }

    public static GsonBuilder beginBuildGson() {
        final GsonBuilder builder = new GsonBuilder();

        for (final RPCMethodParameterTypeAdapter value : TYPE_ADAPTERS) {
            builder.registerTypeAdapter(value.type(), value.typeAdapter());
        }

        return builder;
    }
}
