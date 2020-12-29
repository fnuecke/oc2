package li.cil.oc2.common.item;

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

public abstract class AbstractStorageItem extends Item {
    public static final ResourceLocation CAPACITY_PROPERTY = new ResourceLocation(API.MOD_ID, "capacity");
    public static final String CAPACITY_TAG_NAME = "size";

    ///////////////////////////////////////////////////////////////////

    private final int defaultCapacity;

    ///////////////////////////////////////////////////////////////////

    public static int getCapacity(final ItemStack stack) {
        final Item item = stack.getItem();
        if (stack.isEmpty() || !(item instanceof AbstractStorageItem)) {
            return 0;
        }

        final CompoundNBT modNbt = ItemStackUtils.getModDataTag(stack);
        if (modNbt == null || !modNbt.contains(CAPACITY_TAG_NAME, NBTTagIds.TAG_INT)) {
            final AbstractStorageItem storageItem = (AbstractStorageItem) item;
            return storageItem.defaultCapacity;
        }

        return modNbt.getInt(CAPACITY_TAG_NAME);
    }

    public static ItemStack withCapacity(final ItemStack stack, final int capacity) {
        ItemStackUtils.getOrCreateModDataTag(stack).putInt(CAPACITY_TAG_NAME, capacity);
        return stack;
    }

    ///////////////////////////////////////////////////////////////////

    public AbstractStorageItem(final Properties properties, final int defaultCapacity) {
        super(properties);
        this.defaultCapacity = defaultCapacity;
        ItemModelsProperties.registerProperty(this, CAPACITY_PROPERTY, AbstractStorageItem::getCapacityProperty);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public ITextComponent getDisplayName(final ItemStack stack) {
        return new StringTextComponent("")
                .append(super.getDisplayName(stack))
                .appendString(" (")
                .append(getDisplayNameSuffix(stack))
                .appendString(")");
    }

    ///////////////////////////////////////////////////////////////////

    protected ITextComponent getDisplayNameSuffix(final ItemStack stack) {
        return new StringTextComponent(TextFormatUtils.formatSize(getCapacity(stack)));
    }

    ///////////////////////////////////////////////////////////////////

    private static float getCapacityProperty(final ItemStack stack, final ClientWorld world, final LivingEntity entity) {
        return getCapacity(stack);
    }
}
