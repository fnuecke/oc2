package li.cil.oc2.common.inet;

import net.minecraft.nbt.Tag;

public interface InternetConnection {
    Tag saveAdapterState();
    void stop();
}
