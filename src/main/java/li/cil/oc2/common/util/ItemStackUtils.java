package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import javax.annotation.Nullable;

public final class ItemStackUtils {
    @Nullable
    public static CompoundNBT getModDataTag(final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        return stack.getChildTag(API.MOD_ID);
    }

    public static CompoundNBT getOrCreateModDataTag(final ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException();
        }

        return stack.getOrCreateChildTag(API.MOD_ID);
    }
}
