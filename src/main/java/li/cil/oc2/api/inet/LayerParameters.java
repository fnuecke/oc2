package li.cil.oc2.api.inet;

import net.minecraft.nbt.Tag;

public interface LayerParameters {
    Tag getSavedState();
    InternetManager getInternetManager();
}
