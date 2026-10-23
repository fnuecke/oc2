/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import li.cil.oc2.common.container.ItemStackHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class NBTUtils {
    public static <T extends Enum<T>> void putEnum(final CompoundTag compound, final String key, @Nullable final Enum<T> value) {
        if (value != null) {
            compound.putInt(key, value.ordinal());
        }
    }

    @Nullable
    public static <T extends Enum<T>> T getEnum(final CompoundTag compound, final String key, final Class<T> enumType) {
        if (!compound.getInt(key).isPresent()) {
            return null;
        }

        final int ordinal = compound.getIntOr(key, 0);
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
            if (!childTag.getCompound(tagName).isPresent()) {
                return new CompoundTag();
            }
            childTag = childTag.getCompoundOrEmpty(tagName);
        }

        return childTag;
    }

    public static CompoundTag getOrCreateChildTag(final CompoundTag tag, final String... path) {
        CompoundTag childTag = tag;
        for (final String tagName : path) {
            if (!childTag.getCompound(tagName).isPresent()) {
                childTag.put(tagName, new CompoundTag());
            }
            childTag = childTag.getCompoundOrEmpty(tagName);
        }
        return childTag;
    }

    public static CompoundTag makeInventoryTag(final HolderLookup.Provider provider, final ItemStack... items) {
        return SerializationUtils.saveToTag(ProblemReporter.Collector.EMPTY_ROOT, provider, new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, items))::save);
    }
}
