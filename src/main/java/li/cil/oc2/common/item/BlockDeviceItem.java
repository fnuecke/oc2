package li.cil.oc2.common.item;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistration;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public class BlockDeviceItem extends AbstractStorageItem {
    private static final String DATA_TAG_NAME = "data";
    private static final String READONLY_TAG_NAME = "readonly";

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static BlockDeviceData getData(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockDeviceItem)) {
            return null;
        }

        final CompoundNBT modNbt = ItemStackUtils.getModDataTag(stack);
        if (modNbt == null || !modNbt.contains(DATA_TAG_NAME, NBTTagIds.TAG_STRING)) {
            return null;
        }

        final String registryName = modNbt.getString(DATA_TAG_NAME);

        try {
            return BlockDeviceDataRegistration.REGISTRY.get().getValue(new ResourceLocation(registryName));
        } catch (final ResourceLocationException ignored) {
            return null;
        }
    }

    public static ItemStack withData(final ItemStack stack, final BlockDeviceData data) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockDeviceItem)) {
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
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockDeviceItem)) {
            return false;
        }

        final CompoundNBT modNbt = ItemStackUtils.getModDataTag(stack);
        if (modNbt == null) {
            return false;
        }

        return modNbt.getBoolean(READONLY_TAG_NAME);
    }

    public static ItemStack withReadonly(final ItemStack stack, final boolean readonly) {
        if (!stack.isEmpty() && stack.getItem() instanceof BlockDeviceItem) {
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

    public BlockDeviceItem(final Properties properties, final int defaultCapacity) {
        super(properties.maxStackSize(1), defaultCapacity);
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
