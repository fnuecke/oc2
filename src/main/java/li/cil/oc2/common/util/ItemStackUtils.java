package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public final class ItemStackUtils {
    @Nullable
    public static CompoundTag getModDataTag(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        return stack.getSubTag(API.MOD_ID);
    }

    public static CompoundTag getOrCreateModDataTag(final ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException();
        }

        return stack.getOrCreateSubTag(API.MOD_ID);
    }
}
