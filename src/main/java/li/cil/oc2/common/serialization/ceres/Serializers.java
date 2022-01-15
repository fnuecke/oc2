package li.cil.oc2.common.serialization.ceres;

import com.google.gson.JsonArray;
import li.cil.ceres.Ceres;
import li.cil.oc2.common.vm.context.global.MemoryRangeList;
import li.cil.sedna.api.memory.MemoryRange;
import net.minecraft.network.chat.Component;

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
        Ceres.putSerializer(Component.class, new TextComponentSerializer());
        Ceres.putSerializer(MemoryRange.class, new MemoryRangeSerializer());
        Ceres.putSerializer(MemoryRangeList.class, new MemoryRangeListSerializer());
    }
}
