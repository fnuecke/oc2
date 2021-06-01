package li.cil.oc2.client.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.Items;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;

public final class CustomItemModelProperties {
    public static final ResourceLocation COLOR_PROPERTY = new ResourceLocation(API.MOD_ID, "color");

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        ItemModelsProperties.register(Items.HARD_DRIVE_SMALL.get(), CustomItemModelProperties.COLOR_PROPERTY,
                (stack, world, entity) -> CustomItemColors.getColor(stack));
        ItemModelsProperties.register(Items.HARD_DRIVE_MEDIUM.get(), CustomItemModelProperties.COLOR_PROPERTY,
                (stack, world, entity) -> CustomItemColors.getColor(stack));
        ItemModelsProperties.register(Items.HARD_DRIVE_LARGE.get(), CustomItemModelProperties.COLOR_PROPERTY,
                (stack, world, entity) -> CustomItemColors.getColor(stack));
        ItemModelsProperties.register(Items.HARD_DRIVE_CUSTOM.get(), CustomItemModelProperties.COLOR_PROPERTY,
                (stack, world, entity) -> CustomItemColors.getColor(stack));
        ItemModelsProperties.register(Items.FLOPPY.get(), CustomItemModelProperties.COLOR_PROPERTY,
                (stack, world, entity) -> CustomItemColors.getColor(stack));
    }
}
