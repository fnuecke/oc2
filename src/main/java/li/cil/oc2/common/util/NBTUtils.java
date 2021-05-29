package li.cil.oc2.common.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
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

    public static CompoundNBT getChildTag(@Nullable final CompoundNBT tag, final String... path) {
        if (tag == null) {
            return new CompoundNBT();
        }

        CompoundNBT childTag = tag;
        for (final String tagName : path) {
            if (!childTag.contains(tagName, NBTTagIds.TAG_COMPOUND)) {
                return new CompoundNBT();
            }
            childTag = childTag.getCompound(tagName);
        }

        return childTag;
    }

    public static CompoundNBT getOrCreateChildTag(final CompoundNBT tag, final String... path) {
        CompoundNBT childTag = tag;
        for (final String tagName : path) {
            if (!childTag.contains(tagName, NBTTagIds.TAG_COMPOUND)) {
                childTag.put(tagName, new CompoundNBT());
            }
            childTag = childTag.getCompound(tagName);
        }
        return childTag;
    }

    public static CompoundNBT makeInventoryTag(final ItemStack... items) {
        return new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, items)).serializeNBT();
    }
}
