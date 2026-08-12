/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.item;

import li.cil.oc2.api.API;
import li.cil.oc2.client.ClientPlatform;
import li.cil.oc2.common.item.Items;
import net.minecraft.resources.ResourceLocation;

public final class CustomItemModelProperties {
    public static final ResourceLocation COLOR_PROPERTY = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "color");

    // ------------------------------------------------------------- //

    public static void initialize() {
        ClientPlatform.registerItemProperty(Items.HARD_DRIVE_SMALL.get(), CustomItemModelProperties.COLOR_PROPERTY,
            (stack, level, entity, seed) -> CustomItemColors.getColor(stack));
        ClientPlatform.registerItemProperty(Items.HARD_DRIVE_MEDIUM.get(), CustomItemModelProperties.COLOR_PROPERTY,
            (stack, level, entity, seed) -> CustomItemColors.getColor(stack));
        ClientPlatform.registerItemProperty(Items.HARD_DRIVE_LARGE.get(), CustomItemModelProperties.COLOR_PROPERTY,
            (stack, level, entity, seed) -> CustomItemColors.getColor(stack));
        ClientPlatform.registerItemProperty(Items.HARD_DRIVE_CUSTOM.get(), CustomItemModelProperties.COLOR_PROPERTY,
            (stack, level, entity, seed) -> CustomItemColors.getColor(stack));
        ClientPlatform.registerItemProperty(Items.FLOPPY.get(), CustomItemModelProperties.COLOR_PROPERTY,
            (stack, level, entity, seed) -> CustomItemColors.getColor(stack));
    }
}
