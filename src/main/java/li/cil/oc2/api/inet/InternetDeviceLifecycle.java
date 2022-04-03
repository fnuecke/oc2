package li.cil.oc2.api.inet;

import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;

public interface InternetDeviceLifecycle {
    default Tag onSave() {
        return EndTag.INSTANCE;
    }
    default void onStop() {}
}
