package li.cil.oc2.common.item;

import li.cil.oc2.Constants;
import li.cil.oc2.api.bus.device.data.BaseBlockDevice;
import li.cil.oc2.common.bus.device.data.BaseBlockDevices;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public final class HardDriveItem extends AbstractStorageItem {
    public static final String BASE_TAG_NAME = "base";
    public static final String READONLY_TAG_NAME = "readonly";

    private static final int DEFAULT_CAPACITY = 2 * Constants.MEGABYTE;

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static BaseBlockDevice getBaseBlockDevice(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof HardDriveItem)) {
            return null;
        }

        final CompoundNBT modNbt = ItemStackUtils.getModDataTag(stack);
        if (modNbt == null || !modNbt.contains(BASE_TAG_NAME, NBTTagIds.TAG_STRING)) {
            return null;
        }

        final String registryName = modNbt.getString(BASE_TAG_NAME);

        try {
            return BaseBlockDevices.REGISTRY.get().getValue(new ResourceLocation(registryName));
        } catch (final ResourceLocationException ignored) {
            return null;
        }
    }

    public static ItemStack withBase(final ItemStack stack, final BaseBlockDevice baseBlockDevice) {
        if (stack.isEmpty() || !(stack.getItem() instanceof HardDriveItem)) {
            return stack;
        }

        final ResourceLocation key = BaseBlockDevices.REGISTRY.get().getKey(baseBlockDevice);
        if (key == null) {
            return stack;
        }

        ItemStackUtils.getOrCreateModDataTag(stack).putString(BASE_TAG_NAME, key.toString());

        return stack;
    }

    public static boolean isReadonly(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof HardDriveItem)) {
            return false;
        }

        final CompoundNBT modNbt = ItemStackUtils.getModDataTag(stack);
        if (modNbt == null) {
            return false;
        }

        return modNbt.getBoolean(READONLY_TAG_NAME);
    }

    public static ItemStack withReadonly(final ItemStack stack, final boolean readonly) {
        if (!stack.isEmpty() && stack.getItem() instanceof HardDriveItem) {
            ItemStackUtils.getOrCreateModDataTag(stack).putBoolean(READONLY_TAG_NAME, readonly);
        }

        return stack;
    }

    ///////////////////////////////////////////////////////////////////

    public HardDriveItem(final Properties properties) {
        super(properties, DEFAULT_CAPACITY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected ITextComponent getDisplayNameSuffix(final ItemStack stack) {
        final BaseBlockDevice baseBlockDevice = getBaseBlockDevice(stack);
        if (baseBlockDevice != null) {
            return baseBlockDevice.getName();
        } else {
            return super.getDisplayNameSuffix(stack);
        }
    }
}
