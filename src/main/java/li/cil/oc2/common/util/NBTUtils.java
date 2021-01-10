package li.cil.oc2.common.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public final class NBTUtils {
    public static <T extends Enum<T>> void putEnum(final CompoundNBT compound, final String key, @Nullable final Enum<T> value) {
        if (value != null) {
            compound.putInt(key, value.ordinal());
        }
    }

    @Nullable
    public static <T extends Enum<T>> T getEnum(final CompoundNBT compound, final String key, final Class<T> enumType) {
        if (!compound.contains(key, NBTTagIds.TAG_INT)) {
            return null;
        }

        final int ordinal = compound.getInt(key);
        try {
            return enumType.getEnumConstants()[ordinal];
        } catch (final IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    public static CompoundNBT makeInventoryTag(final ItemStack... items) {
        final ItemStackHandler itemStackHandler = new ItemStackHandler(items.length);
        for (int i = 0; i < items.length; i++) {
            itemStackHandler.setStackInSlot(i, items[i]);
        }
        return itemStackHandler.serializeNBT();
    }
}
