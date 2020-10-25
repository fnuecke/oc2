package li.cil.oc2.data;

import li.cil.oc2.api.API;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.client.model.generators.ItemModelProvider;

public final class ItemModels extends ItemModelProvider {
    public ItemModels(final DataGenerator generator, final ExistingFileHelper existingFileHelper) {
        super(generator, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
    }
}
