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

public final class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(final DataGenerator generator, final ExistingFileHelper existingFileHelper) {
        super(generator, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simple(Items.WRENCH, "item/wrench");

        simple(Items.NETWORK_CABLE, "item/network_cable");

        simple(Items.MEMORY, "item/memory1")
                .override()
                .predicate(CustomItemModelProperties.CAPACITY_PROPERTY, 4 * Constants.MEGABYTE)
                .model(simple(Items.MEMORY, "item/memory2", "2"))
                .end()
                .override()
                .predicate(CustomItemModelProperties.CAPACITY_PROPERTY, 8 * Constants.MEGABYTE)
                .model(simple(Items.MEMORY, "item/memory3", "3"))
                .end();
        simple(Items.HARD_DRIVE, "item/hard_drive1")
                .override()
                .predicate(CustomItemModelProperties.CAPACITY_PROPERTY, 4 * Constants.MEGABYTE)
                .model(simple(Items.HARD_DRIVE, "item/hard_drive2", "2"))
                .end()
                .override()
                .predicate(CustomItemModelProperties.CAPACITY_PROPERTY, 8 * Constants.MEGABYTE)
                .model(simple(Items.HARD_DRIVE, "item/hard_drive3", "3"))
                .end();
        simple(Items.FLASH_MEMORY, "item/flash_memory");
        simple(Items.REDSTONE_INTERFACE_CARD, "item/redstone_interface_card");
        simple(Items.NETWORK_INTERFACE_CARD, "item/network_interface_card");
        simple(Items.FLOPPY, "item/floppy_base")
                .texture("layer1", "item/floppy_tint");

        simple(Items.INVENTORY_OPERATIONS_MODULE, "item/inventory_operations_module");
        simple(Items.BLOCK_OPERATIONS_MODULE, "item/block_operations_module");

        withExistingParent(Constants.ROBOT_ENTITY_NAME, "template_shulker_box");
    }

    private <T extends Item> ItemModelBuilder simple(final RegistryObject<T> item, final String texturePath) {
        return simple(item, texturePath, "");
    }

    private <T extends Item> ItemModelBuilder simple(final RegistryObject<T> item, final String texturePath, final String nameSuffix) {
        return singleTexture(item.getId().getPath() + nameSuffix,
                new ResourceLocation("item/generated"),
                "layer0",
                new ResourceLocation(API.MOD_ID, texturePath));
    }

    private <T extends Item> ItemModelBuilder withExistingParent(final RegistryObject<T> item) {
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated"));
    }
}
