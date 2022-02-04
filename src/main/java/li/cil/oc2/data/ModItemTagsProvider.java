/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.tags.BlockTags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

import static li.cil.oc2.common.tags.ItemTags.*;

public final class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(final DataGenerator generator, final BlockTagsProvider blockTagProvider, @Nullable final ExistingFileHelper existingFileHelper) {
        super(generator, blockTagProvider, API.MOD_ID, existingFileHelper);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addTags() {
        copy(BlockTags.CABLES, CABLES);

        copy(BlockTags.DEVICES, DEVICES);
        tag(DEVICES).addTags(
            DEVICES_MEMORY,
            DEVICES_HARD_DRIVE,
            DEVICES_FLASH_MEMORY,
            DEVICES_CARD,
            DEVICES_ROBOT_MODULE,
            DEVICES_FLOPPY
        );
        tag(DEVICES_MEMORY).add(
            Items.MEMORY_SMALL.get(),
            Items.MEMORY_MEDIUM.get(),
            Items.MEMORY_LARGE.get()
        );
        tag(DEVICES_HARD_DRIVE).add(
            Items.HARD_DRIVE_SMALL.get(),
            Items.HARD_DRIVE_MEDIUM.get(),
            Items.HARD_DRIVE_LARGE.get(),
            Items.HARD_DRIVE_CUSTOM.get()
        );
        tag(DEVICES_FLASH_MEMORY).add(
            Items.FLASH_MEMORY.get(),
            Items.FLASH_MEMORY_CUSTOM.get()
        );
        tag(DEVICES_FLOPPY).add(
            Items.FLOPPY.get()
        );
        tag(DEVICES_CARD).add(
            Items.REDSTONE_INTERFACE_CARD.get(),
            Items.NETWORK_INTERFACE_CARD.get(),
            Items.FILE_IMPORT_EXPORT_CARD.get(),
            Items.SOUND_CARD.get(),
            Items.NETWORK_TUNNEL_CARD.get()
        );
        tag(DEVICES_ROBOT_MODULE).add(
            Items.INVENTORY_OPERATIONS_MODULE.get(),
            Items.BLOCK_OPERATIONS_MODULE.get(),
            Items.NETWORK_TUNNEL_MODULE.get()
        );
        tag(DEVICES_NETWORK_TUNNEL).add(
            Items.NETWORK_TUNNEL_CARD.get(),
            Items.NETWORK_TUNNEL_MODULE.get()
        );

        tag(WRENCHES).add(Items.WRENCH.get());

        tag(DEVICE_NEEDS_REBOOT).add(
            Items.DISK_DRIVE.get(),
            Items.FLASH_MEMORY.get(),
            Items.FLASH_MEMORY_CUSTOM.get(),
            Items.HARD_DRIVE_SMALL.get(),
            Items.HARD_DRIVE_MEDIUM.get(),
            Items.HARD_DRIVE_LARGE.get(),
            Items.HARD_DRIVE_CUSTOM.get(),
            Items.KEYBOARD.get(),
            Items.MEMORY_SMALL.get(),
            Items.MEMORY_MEDIUM.get(),
            Items.MEMORY_LARGE.get(),
            Items.NETWORK_INTERFACE_CARD.get(),
            Items.NETWORK_TUNNEL_CARD.get(),
            Items.NETWORK_TUNNEL_MODULE.get(),
            Items.PROJECTOR.get()
        );
    }
}
