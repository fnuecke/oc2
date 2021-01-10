package li.cil.oc2.data;

import li.cil.oc2.api.API;
import li.cil.oc2.client.item.CustomItemModelProperties;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.item.Items;
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

        simple(Items.NETWORK_CABLE_ITEM, "items/network_cable");

        simple(Items.MEMORY_ITEM, "items/memory1")
                .override()
                .predicate(CustomItemModelProperties.CAPACITY_PROPERTY, 4 * Constants.MEGABYTE)
                .model(simple(Items.MEMORY_ITEM, "items/memory2", "2"))
                .end()
                .override()
                .predicate(CustomItemModelProperties.CAPACITY_PROPERTY, 8 * Constants.MEGABYTE)
                .model(simple(Items.MEMORY_ITEM, "items/memory3", "3"))
                .end();
        simple(Items.HARD_DRIVE_ITEM, "items/hard_drive1")
                .override()
                .predicate(CustomItemModelProperties.CAPACITY_PROPERTY, 4 * Constants.MEGABYTE)
                .model(simple(Items.HARD_DRIVE_ITEM, "items/hard_drive2", "2"))
                .end()
                .override()
                .predicate(CustomItemModelProperties.CAPACITY_PROPERTY, 8 * Constants.MEGABYTE)
                .model(simple(Items.HARD_DRIVE_ITEM, "items/hard_drive3", "3"))
                .end();
        simple(Items.FLASH_MEMORY_ITEM, "items/flash_memory");
        simple(Items.REDSTONE_INTERFACE_CARD_ITEM, "items/redstone_interface_card");
        simple(Items.NETWORK_INTERFACE_CARD_ITEM, "items/network_interface_card");
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
