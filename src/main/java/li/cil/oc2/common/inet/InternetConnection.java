package li.cil.oc2.common.inet;

import net.minecraft.nbt.Tag;

import java.util.Optional;

public interface InternetConnection {
    Optional<Tag> saveAdapterState();
    void stop();
}
