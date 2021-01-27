package li.cil.oc2.client.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.item.AbstractStorageItem;
import li.cil.oc2.common.item.AbstractBlockDeviceItem;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public final class CustomItemModelProperties {
    public static final ResourceLocation CAPACITY_PROPERTY = new ResourceLocation(API.MOD_ID, "capacity");
    public static final ResourceLocation COLOR_PROPERTY = new ResourceLocation(API.MOD_ID, "color");

    private static final String COLOR_TAG_NAME = "color";

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        ItemModelsProperties.registerProperty(Items.MEMORY.get(), CustomItemModelProperties.CAPACITY_PROPERTY,
                (stack, world, entity) -> AbstractStorageItem.getCapacity(stack));
        ItemModelsProperties.registerProperty(Items.HARD_DRIVE.get(), CustomItemModelProperties.CAPACITY_PROPERTY,
                (stack, world, entity) -> AbstractBlockDeviceItem.getData(stack) != null ? Integer.MAX_VALUE : AbstractStorageItem.getCapacity(stack));

        ItemModelsProperties.registerProperty(Items.FLOPPY.get(), CustomItemModelProperties.COLOR_PROPERTY,
                (stack, world, entity) -> getColor(stack));
    }

    public static float getColor(final ItemStack stack) {
        final CompoundNBT modTag = ItemStackUtils.getModDataTag(stack);
        if (modTag == null) {
            return 0;
        }

        return modTag.getInt("color");
    }

    public static ItemStack withColor(final ItemStack stack, final TextFormatting color) {
        return withColor(stack, color.getColorIndex());
    }

    public static ItemStack withColor(final ItemStack stack, final int color) {
        ItemStackUtils.getOrCreateModDataTag(stack).putInt(COLOR_TAG_NAME, color);
        return stack;
    }
}
