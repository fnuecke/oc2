package li.cil.oc2.api.inet;

import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

public interface InternetDeviceLifecycle {
    default Optional<Tag> onSave() {
        return Optional.empty();
    }
    default void onStop() {}
}
