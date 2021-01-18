package li.cil.oc2.data;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tags.BlockTags;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.ItemTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import static li.cil.oc2.common.tags.ItemTags.*;

public final class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(final DataGenerator generator, final BlockTagsProvider blockTagProvider, @Nullable final ExistingFileHelper existingFileHelper) {
        super(generator, blockTagProvider, API.MOD_ID, existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void registerTags() {
        copy(BlockTags.DEVICES, DEVICES);
        copy(BlockTags.CABLES, CABLES);
        getOrCreateBuilder(DEVICES).addTags(
                DEVICES_MEMORY,
                DEVICES_HARD_DRIVE,
                DEVICES_FLASH_MEMORY,
                DEVICES_CARD,
                DEVICES_ROBOT_MODULE
        );
        getOrCreateBuilder(DEVICES_MEMORY).add(
                Items.MEMORY_ITEM.get()
        );
        getOrCreateBuilder(DEVICES_HARD_DRIVE).add(
                Items.HARD_DRIVE_ITEM.get()
        );
        getOrCreateBuilder(DEVICES_FLASH_MEMORY).add(
                Items.FLASH_MEMORY_ITEM.get()
        );
        getOrCreateBuilder(DEVICES_CARD).add(
                Items.REDSTONE_INTERFACE_CARD_ITEM.get(),
                Items.NETWORK_INTERFACE_CARD_ITEM.get()
        );
        getOrCreateBuilder(DEVICES_ROBOT_MODULE).add(
                Items.INVENTORY_OPERATIONS_MODULE.get(),
                Items.BLOCK_OPERATIONS_MODULE.get()
        );
        getOrCreateBuilder(WRENCHES).add(Items.WRENCH_ITEM.get());
    }
}
