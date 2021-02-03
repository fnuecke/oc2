package li.cil.oc2.common.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
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

    public static void putBlockPos(final CompoundNBT tag, final String key, final BlockPos value) {
        final CompoundNBT valueTag = new CompoundNBT();
        valueTag.putInt("x", value.getX());
        valueTag.putInt("y", value.getY());
        valueTag.putInt("z", value.getZ());
        tag.put(key, valueTag);
    }

    public static BlockPos getBlockPos(final CompoundNBT tag, final String key) {
        final CompoundNBT valueTag = tag.getCompound(key);
        return new BlockPos(
                valueTag.getInt("x"),
                valueTag.getInt("y"),
                valueTag.getInt("z")
        );
    }

    public static void putVector3d(final CompoundNBT tag, final String key, final Vector3d value) {
        final CompoundNBT valueTag = new CompoundNBT();
        valueTag.putDouble("x", value.getX());
        valueTag.putDouble("y", value.getY());
        valueTag.putDouble("z", value.getZ());
        tag.put(key, valueTag);
    }

    public static Vector3d getVector3d(final CompoundNBT tag, final String key) {
        final CompoundNBT valueTag = tag.getCompound(key);
        return new Vector3d(
                valueTag.getDouble("x"),
                valueTag.getDouble("y"),
                valueTag.getDouble("z")
        );
    }

    public static CompoundNBT makeInventoryTag(final ItemStack... items) {
        final ItemStackHandler itemStackHandler = new ItemStackHandler(items.length);
        for (int i = 0; i < items.length; i++) {
            itemStackHandler.setStackInSlot(i, items[i]);
        }
        return itemStackHandler.serializeNBT();
    }
}
