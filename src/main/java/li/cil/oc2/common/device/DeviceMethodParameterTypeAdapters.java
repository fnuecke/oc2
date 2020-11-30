package li.cil.oc2.common.device;

import com.google.gson.GsonBuilder;
import li.cil.oc2.api.imc.DeviceMethodParameterTypeAdapter;

import java.util.ArrayList;

public final class DeviceMethodParameterTypeAdapters {
    private static final ArrayList<DeviceMethodParameterTypeAdapter> TYPE_ADAPTERS = new ArrayList<>();

    public static void addTypeAdapter(final Class<?> type, final Object typeAdapter) {
        addTypeAdapter(new DeviceMethodParameterTypeAdapter(type, typeAdapter));
    }

    public static void addTypeAdapter(final DeviceMethodParameterTypeAdapter value) {
        TYPE_ADAPTERS.add(value);
    }

    public static GsonBuilder beginBuildGson() {
        final GsonBuilder builder = new GsonBuilder()
                .serializeNulls();

        for (final DeviceMethodParameterTypeAdapter value : TYPE_ADAPTERS) {
            builder.registerTypeAdapter(value.type, value.typeAdapter);
        }

        return builder;
    }
}
