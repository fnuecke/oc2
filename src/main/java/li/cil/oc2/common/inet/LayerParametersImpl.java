package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.LayerParameters;
import li.cil.oc2.api.inet.InternetManager;
import net.minecraft.nbt.Tag;

public record LayerParametersImpl(Tag getSavedState, InternetManager getInternetManager) implements LayerParameters {
}
