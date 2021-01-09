package li.cil.oc2.common.serialization.serializers;

import com.google.gson.JsonArray;
import li.cil.ceres.Ceres;

public final class Serializers {
    private static boolean isInitialized = false;

    static {
        initialize();
    }

    public static void initialize() {
        if (isInitialized) {
            return;
        }

        isInitialized = true;

        Ceres.putSerializer(JsonArray.class, new JsonArraySerializer());
    }
}
