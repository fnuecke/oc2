package li.cil.oc2.common.item;

import li.cil.oc2.Constants;
import li.cil.oc2.api.API;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.TextFormatUtils;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class HddItem extends Item {
    public static final ResourceLocation CAPACITY_PROPERTY = new ResourceLocation(API.MOD_ID, "hdd_capacity");
    public static final String CAPACITY_TAG_NAME = "size";
    public static final String BASE_TAG_NAME = "base";
    public static final String READONLY_TAG_NAME = "readonly";
    public static final int DEFAULT_CAPACITY = 2 * Constants.MEGABYTE;

    public static int getCapacity(final ItemStack stack) {
        final CompoundNBT modNbt = ItemStackUtils.getModDataTag(stack);
        if (modNbt == null || !modNbt.contains(CAPACITY_TAG_NAME, NBTTagIds.TAG_INT)) {
            return DEFAULT_CAPACITY;
        }

        return modNbt.getInt(CAPACITY_TAG_NAME);
    }

    public static ItemStack withCapacity(final ItemStack stack, final int capacity) {
        ItemStackUtils.getOrCreateModDataTag(stack).putInt(CAPACITY_TAG_NAME, capacity);
        return stack;
    }

    @Nullable
    public static String getBaseBlockDevice(final ItemStack stack) {
        final CompoundNBT modNbt = ItemStackUtils.getModDataTag(stack);
        if (modNbt == null || !modNbt.contains(BASE_TAG_NAME, NBTTagIds.TAG_STRING)) {
            return null;
        }

        return modNbt.getString(BASE_TAG_NAME);
    }

    public static ItemStack withBaseBlockDevice(final ItemStack stack, final String baseBlockDevice) {
        ItemStackUtils.getOrCreateModDataTag(stack).putString(BASE_TAG_NAME, baseBlockDevice);
        return stack;
    }

    public static boolean isReadonly(final ItemStack stack) {
        final CompoundNBT modNbt = ItemStackUtils.getModDataTag(stack);
        if (modNbt == null) {
            return false;
        }

        return modNbt.getBoolean(READONLY_TAG_NAME);
    }

    public HddItem(final Properties properties) {
        super(properties);
        ItemModelsProperties.registerProperty(this, CAPACITY_PROPERTY, HddItem::getHddItemProperties);
    }

    @Override
    public ITextComponent getDisplayName(final ItemStack stack) {
        final String baseBlockDevice = getBaseBlockDevice(stack);
        if (baseBlockDevice != null) {
            return new StringTextComponent("")
                    .append(super.getDisplayName(stack))
                    .append(new TranslationTextComponent(Constants.TOOLTIP_SUFFIX_FORMAT, baseBlockDevice));
        } else {
            return new StringTextComponent("")
                    .append(super.getDisplayName(stack))
                    .append(new TranslationTextComponent(Constants.TOOLTIP_SUFFIX_FORMAT, TextFormatUtils.formatSize(getCapacity(stack))));
        }
    }

    private static float getHddItemProperties(final ItemStack stack, final ClientWorld world, final LivingEntity entity) {
        return getCapacity(stack);
    }
}
