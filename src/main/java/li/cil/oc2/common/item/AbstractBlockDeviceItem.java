package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistration;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public abstract class AbstractBlockDeviceItem extends AbstractStorageItem {
    private static final String DATA_TAG_NAME = "data";
    private static final String READONLY_TAG_NAME = "readonly";

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static BlockDeviceData getData(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbstractBlockDeviceItem)) {
            return null;
        }

        final String registryName = ItemStackUtils.getModDataTag(stack).getString(DATA_TAG_NAME);
        if (StringUtils.isNullOrEmpty(registryName)) {
            return null;
        }

        try {
            return BlockDeviceDataRegistration.REGISTRY.get().getValue(new ResourceLocation(registryName));
        } catch (final ResourceLocationException ignored) {
            return null;
        }
    }

    public static ItemStack withData(final ItemStack stack, final BlockDeviceData data) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbstractBlockDeviceItem)) {
            return stack;
        }

        final ResourceLocation key = BlockDeviceDataRegistration.REGISTRY.get().getKey(data);
        if (key == null) {
            return stack;
        }

        ItemStackUtils.getOrCreateModDataTag(stack).putString(DATA_TAG_NAME, key.toString());

        return stack;
    }

    public static boolean isReadonly(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AbstractBlockDeviceItem)) {
            return false;
        }

        return ItemStackUtils.getModDataTag(stack).getBoolean(READONLY_TAG_NAME);
    }

    public static ItemStack withReadonly(final ItemStack stack, final boolean readonly) {
        if (!stack.isEmpty() && stack.getItem() instanceof AbstractBlockDeviceItem) {
            ItemStackUtils.getOrCreateModDataTag(stack).putBoolean(READONLY_TAG_NAME, readonly);
        }

        return stack;
    }

    public ItemStack withData(final BlockDeviceData data) {
        return withData(new ItemStack(this), data);
    }

    public ItemStack withReadonly(final boolean readonly) {
        return withReadonly(new ItemStack(this), readonly);
    }

    ///////////////////////////////////////////////////////////////////

    protected AbstractBlockDeviceItem(final Properties properties, final int defaultCapacity) {
        super(properties.maxStackSize(1), defaultCapacity);
    }

    protected AbstractBlockDeviceItem(final int defaultCapacity) {
        this(createProperties(), defaultCapacity);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected ITextComponent getDisplayNameSuffix(final ItemStack stack) {
        final BlockDeviceData data = getData(stack);
        if (data != null) {
            return data.getDisplayName();
        } else {
            return super.getDisplayNameSuffix(stack);
        }
    }
}
