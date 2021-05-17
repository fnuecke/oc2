package li.cil.oc2.data;

import li.cil.oc2.api.API;
import li.cil.oc2.common.entity.Entities;
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

        simple(Items.MEMORY_SMALL, "item/memory_small");
        simple(Items.MEMORY_MEDIUM, "item/memory_medium");
        simple(Items.MEMORY_LARGE, "item/memory_large");
        simple(Items.HARD_DRIVE_SMALL, "item/hard_drive_base")
                .texture("layer1", "item/hard_drive_tint");
        simple(Items.HARD_DRIVE_MEDIUM, "item/hard_drive_base")
                .texture("layer1", "item/hard_drive_tint");
        simple(Items.HARD_DRIVE_LARGE, "item/hard_drive_base")
                .texture("layer1", "item/hard_drive_tint");
        simple(Items.HARD_DRIVE_CUSTOM, "item/hard_drive_base")
                .texture("layer1", "item/hard_drive_tint");
        simple(Items.FLASH_MEMORY, "item/flash_memory");
        simple(Items.FLASH_MEMORY_CUSTOM, "item/flash_memory");
        simple(Items.FLOPPY, "item/floppy_base")
                .texture("layer1", "item/floppy_tint");

        simple(Items.REDSTONE_INTERFACE_CARD, "item/redstone_interface_card");
        simple(Items.NETWORK_INTERFACE_CARD, "item/network_interface_card");
        simple(Items.FILE_IMPORT_EXPORT_CARD, "item/file_import_export_card");

        simple(Items.INVENTORY_OPERATIONS_MODULE, "item/inventory_operations_module");
        simple(Items.BLOCK_OPERATIONS_MODULE, "item/block_operations_module");

        simple(Items.TRANSISTOR, "item/transistor");
        simple(Items.CIRCUIT_BOARD, "item/circuit_board");

        withExistingParent(Entities.ROBOT.getId().getPath(), "template_shulker_box");
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
