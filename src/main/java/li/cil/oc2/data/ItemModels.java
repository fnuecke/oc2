package li.cil.oc2.data;

import li.cil.oc2.Constants;
import li.cil.oc2.api.API;
import li.cil.oc2.common.init.Items;
import li.cil.oc2.common.item.HddItem;
import li.cil.oc2.common.item.RamItem;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.fml.RegistryObject;

public final class ItemModels extends ItemModelProvider {
    public ItemModels(final DataGenerator generator, final ExistingFileHelper existingFileHelper) {
        super(generator, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simple(Items.WRENCH_ITEM, "items/wrench");

        simple(Items.RAM_ITEM, "items/ram1")
                .override()
                .predicate(RamItem.CAPACITY_PROPERTY, 4 * Constants.MEGABYTE)
                .model(simple(Items.RAM_ITEM, "items/ram2", "2"))
                .end()
                .override()
                .predicate(RamItem.CAPACITY_PROPERTY, 8 * Constants.MEGABYTE)
                .model(simple(Items.RAM_ITEM, "items/ram3", "3"))
                .end();

        simple(Items.HDD_ITEM, "items/hdd1")
                .override()
                .predicate(HddItem.CAPACITY_PROPERTY, 4 * Constants.MEGABYTE)
                .model(simple(Items.HDD_ITEM, "items/hdd2", "2"))
                .end()
                .override()
                .predicate(HddItem.CAPACITY_PROPERTY, 8 * Constants.MEGABYTE)
                .model(simple(Items.HDD_ITEM, "items/hdd3", "3"))
                .end();
    }

    private <T extends Item> ItemModelBuilder simple(final RegistryObject<T> item, final String texturePath) {
        return simple(item, texturePath, "");
    }

    private <T extends Item> ItemModelBuilder simple(final RegistryObject<T> item, final String texturePath, final String nameSuffix) {
        return singleTexture(item.getId().getPath() + nameSuffix,
                new ResourceLocation("item/handheld"),
                "layer0",
                new ResourceLocation(API.MOD_ID, texturePath));
    }
}
