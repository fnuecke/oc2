package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;

import javax.annotation.Nullable;

public final class MemoryItem extends AbstractStorageItem {
    @Nullable private String descriptionId;

    ///////////////////////////////////////////////////////////////////

    public MemoryItem(final int defaultCapacity) {
        super(defaultCapacity);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected String getOrCreateDescriptionId() {
        if (descriptionId == null) {
            descriptionId = Util.makeDescriptionId("item", new ResourceLocation(API.MOD_ID, "memory"));
        }
        return descriptionId;
    }
}
