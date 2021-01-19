package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.data.Firmwares;
import li.cil.oc2.common.bus.device.item.ByteBufferFlashMemoryVMDevice;
import li.cil.oc2.common.util.ItemStackUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public final class FlashMemoryItem extends AbstractStorageItem {
    public static final String FIRMWARE_TAG_NAME = "firmware";

    private static final int DEFAULT_CAPACITY = 4 * Constants.KILOBYTE;

    ///////////////////////////////////////////////////////////////////

    public static ItemStack withCapacity(final int capacity) {
        return withCapacity(new ItemStack(Items.FLASH_MEMORY.get()), capacity);
    }

    @Nullable
    public static Firmware getFirmware(final ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof FlashMemoryItem)) {
            return null;
        }

        final CompoundNBT modNbt = ItemStackUtils.getModDataTag(stack);
        if (modNbt == null || !modNbt.contains(FIRMWARE_TAG_NAME, NBTTagIds.TAG_STRING)) {
            return null;
        }

        final String registryName = modNbt.getString(FIRMWARE_TAG_NAME);

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

    public static ItemStack withFirmware(final Firmware firmware) {
        return withFirmware(new ItemStack(Items.FLASH_MEMORY.get()), firmware);
    }

    ///////////////////////////////////////////////////////////////////

    public FlashMemoryItem(final Properties properties) {
        super(properties, DEFAULT_CAPACITY);
    }

    ///////////////////////////////////////////////////////////////////

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
            return firmware.getName();
        } else {
            return super.getDisplayNameSuffix(stack);
        }
    }
}
