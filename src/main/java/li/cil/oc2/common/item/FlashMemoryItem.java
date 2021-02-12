package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.data.Firmwares;
import li.cil.oc2.common.bus.device.item.ByteBufferFlashMemoryVMDevice;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public final class FlashMemoryItem extends AbstractStorageItem {
    public static final String FIRMWARE_TAG_NAME = "firmware";

    private static final int DEFAULT_CAPACITY = 4 * Constants.KILOBYTE;

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static Firmware getFirmware(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof FlashMemoryItem)) {
            return null;
        }

        final String registryName = ItemStackUtils.getModDataTag(stack).getString(FIRMWARE_TAG_NAME);
        if (StringUtils.isNullOrEmpty(registryName)) {
            return null;
        }

        try {
            return Firmwares.REGISTRY.get().getValue(new ResourceLocation(registryName));
        } catch (final ResourceLocationException ignored) {
            return null;
        }
    }

    public static ItemStack withFirmware(final ItemStack stack, final Firmware firmware) {
        if (stack.isEmpty() || !(stack.getItem() instanceof FlashMemoryItem)) {
            return stack;
        }

        final ResourceLocation key = Firmwares.REGISTRY.get().getKey(firmware);
        if (key == null) {
            return stack;
        }

        ItemStackUtils.getOrCreateModDataTag(stack).putString(FIRMWARE_TAG_NAME, key.toString());

        return stack;
    }

    public ItemStack withCapacity(final int capacity) {
        return withCapacity(new ItemStack(this), capacity);
    }

    public ItemStack withFirmware(final Firmware firmware) {
        return withFirmware(new ItemStack(this), firmware);
    }

    ///////////////////////////////////////////////////////////////////

    public FlashMemoryItem() {
        super(createProperties().maxStackSize(1), DEFAULT_CAPACITY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fillItemGroup(final ItemGroup group, final NonNullList<ItemStack> items) {
        if (isInGroup(group)) {
            items.add(withCapacity(4 * Constants.KILOBYTE));
            items.add(withFirmware(Firmwares.BUILDROOT.get()));
        }
    }

    @Nullable
    @Override
    public CompoundNBT getShareTag(final ItemStack stack) {
        final CompoundNBT tag = super.getShareTag(stack);
        if (tag != null && tag.contains(API.MOD_ID, NBTTagIds.TAG_COMPOUND)) {
            tag.getCompound(API.MOD_ID).remove(ByteBufferFlashMemoryVMDevice.DATA_TAG_NAME);
        }
        return tag;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected ITextComponent getDisplayNameSuffix(final ItemStack stack) {
        final Firmware firmware = getFirmware(stack);
        if (firmware != null) {
            return firmware.getDisplayName();
        } else {
            return super.getDisplayNameSuffix(stack);
        }
    }
}
