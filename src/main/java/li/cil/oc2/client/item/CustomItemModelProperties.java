package li.cil.oc2.client.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.AbstractStorageItem;
import li.cil.oc2.common.item.HardDriveItem;
import li.cil.oc2.common.item.Items;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;

public final class CustomItemModelProperties {
    public static final ResourceLocation CAPACITY_PROPERTY = new ResourceLocation(API.MOD_ID, "capacity");

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        ItemModelsProperties.registerProperty(Items.MEMORY_ITEM.get(), CustomItemModelProperties.CAPACITY_PROPERTY,
                (stack, world, entity) -> AbstractStorageItem.getCapacity(stack));
        ItemModelsProperties.registerProperty(Items.HARD_DRIVE_ITEM.get(), CustomItemModelProperties.CAPACITY_PROPERTY,
                (stack, world, entity) -> HardDriveItem.getBaseBlockDevice(stack) != null ? Integer.MAX_VALUE : AbstractStorageItem.getCapacity(stack));
    }
}
