package li.cil.oc2.data;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tags.BlockTags;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

import static li.cil.oc2.common.tags.ItemTags.*;

public final class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(final DataGenerator generator, final BlockTagsProvider blockTagProvider, @Nullable final ExistingFileHelper existingFileHelper) {
        super(generator, blockTagProvider, API.MOD_ID, existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void registerTags() {
        copy(BlockTags.CABLES, CABLES);

        copy(BlockTags.DEVICES, DEVICES);
        getOrCreateBuilder(DEVICES).addTags(
                DEVICES_MEMORY,
                DEVICES_HARD_DRIVE,
                DEVICES_FLASH_MEMORY,
                DEVICES_CARD,
                DEVICES_ROBOT_MODULE,
                DEVICES_FLOPPY
        );
        getOrCreateBuilder(DEVICES_MEMORY).add(
                Items.MEMORY_SMALL.get(),
                Items.MEMORY_MEDIUM.get(),
                Items.MEMORY_LARGE.get()
        );
        getOrCreateBuilder(DEVICES_HARD_DRIVE).add(
                Items.HARD_DRIVE_SMALL.get(),
                Items.HARD_DRIVE_MEDIUM.get(),
                Items.HARD_DRIVE_LARGE.get(),
                Items.HARD_DRIVE_CUSTOM.get()
        );
        getOrCreateBuilder(DEVICES_FLASH_MEMORY).add(
                Items.FLASH_MEMORY.get(),
                Items.FLASH_MEMORY_CUSTOM.get()
        );
        getOrCreateBuilder(DEVICES_FLOPPY).add(
                Items.FLOPPY.get()
        );
        getOrCreateBuilder(DEVICES_CARD).add(
                Items.REDSTONE_INTERFACE_CARD.get(),
                Items.NETWORK_INTERFACE_CARD.get(),
                Items.CLOUD_INTERFACE_CARD.get()
        );
        getOrCreateBuilder(DEVICES_ROBOT_MODULE).add(
                Items.INVENTORY_OPERATIONS_MODULE.get(),
                Items.BLOCK_OPERATIONS_MODULE.get()
        );

        getOrCreateBuilder(TOOL_MATERIALS).addTags(
                TOOL_MATERIAL_WOOD,
                TOOL_MATERIAL_STONE,
                TOOL_MATERIAL_IRON,
                TOOL_MATERIAL_GOLD,
                TOOL_MATERIAL_DIAMOND,
                TOOL_MATERIAL_NETHERITE
        );
        getOrCreateBuilder(TOOL_MATERIAL_WOOD).addTags(
                ItemTags.PLANKS
        );
        getOrCreateBuilder(TOOL_MATERIAL_STONE).addTags(
                ItemTags.STONE_TOOL_MATERIALS
        );
        getOrCreateBuilder(TOOL_MATERIAL_IRON).addTags(
                Tags.Items.INGOTS_IRON
        );
        getOrCreateBuilder(TOOL_MATERIAL_GOLD).addTags(
                Tags.Items.INGOTS_GOLD
        );
        getOrCreateBuilder(TOOL_MATERIAL_DIAMOND).addTags(
                Tags.Items.GEMS_DIAMOND
        );
        getOrCreateBuilder(TOOL_MATERIAL_NETHERITE).addTags(
                Tags.Items.INGOTS_NETHERITE
        );

        getOrCreateBuilder(WRENCHES).add(Items.WRENCH.get());

        getOrCreateBuilder(DEVICE_NEEDS_REBOOT).add(
                Items.MEMORY_SMALL.get(),
                Items.MEMORY_MEDIUM.get(),
                Items.MEMORY_LARGE.get(),
                Items.HARD_DRIVE_SMALL.get(),
                Items.HARD_DRIVE_MEDIUM.get(),
                Items.HARD_DRIVE_LARGE.get(),
                Items.HARD_DRIVE_CUSTOM.get(),
                Items.FLASH_MEMORY.get(),
                Items.FLASH_MEMORY_CUSTOM.get(),
                Items.NETWORK_INTERFACE_CARD.get(),
                Items.DISK_DRIVE.get()
        );
    }
}
