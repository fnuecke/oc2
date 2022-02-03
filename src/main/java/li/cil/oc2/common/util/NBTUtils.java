/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public final class NBTUtils {
    public static <T extends Enum<T>> void putEnum(final CompoundTag compound, final String key, @Nullable final Enum<T> value) {
        if (value != null) {
            compound.putInt(key, value.ordinal());
        }
    }

    @Nullable
    public static <T extends Enum<T>> T getEnum(final CompoundTag compound, final String key, final Class<T> enumType) {
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

    public static CompoundTag getChildTag(@Nullable final CompoundTag tag, final String... path) {
        if (tag == null) {
            return new CompoundTag();
        }

        CompoundTag childTag = tag;
        for (final String tagName : path) {
            if (!childTag.contains(tagName, NBTTagIds.TAG_COMPOUND)) {
                return new CompoundTag();
            }
            childTag = childTag.getCompound(tagName);
        }

        return childTag;
    }

    public static CompoundTag getOrCreateChildTag(final CompoundTag tag, final String... path) {
        CompoundTag childTag = tag;
        for (final String tagName : path) {
            if (!childTag.contains(tagName, NBTTagIds.TAG_COMPOUND)) {
                childTag.put(tagName, new CompoundTag());
            }
            childTag = childTag.getCompound(tagName);
        }
        return childTag;
    }

    public static CompoundTag makeInventoryTag(final ItemStack... items) {
        return new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, items)).serializeNBT();
    }
}
