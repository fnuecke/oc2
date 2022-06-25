/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class ItemRenameHandler {
    private static final Map<String, Supplier<Item>> RENAMES = Util.make(() -> {
        final Map<String, Supplier<Item>> map = new HashMap<>();

        map.put("hard_drive_buildroot", Items.HARD_DRIVE_CUSTOM::get);
        map.put("flash_memory_buildroot", Items.FLASH_MEMORY_CUSTOM::get);

        return map;
    });

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addGenericListener(Item.class, ItemRenameHandler::handleMissingMappings);
    }

    ///////////////////////////////////////////////////////////////////

    private static void handleMissingMappings(final RegistryEvent.MissingMappings<Item> event) {
        for (final RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            final ResourceLocation key = mapping.key;
            if (key == null || !Objects.equals(key.getNamespace(), API.MOD_ID)) {
                continue;
            }

            final Supplier<Item> replacement = RENAMES.get(key.getPath());
            if (replacement != null) {
                mapping.remap(replacement.get());
            }
        }
    }
}
