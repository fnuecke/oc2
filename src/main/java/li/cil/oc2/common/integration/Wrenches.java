package li.cil.oc2.common.integration;

import li.cil.oc2.common.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class Wrenches {
    public static boolean isWrench(@Nullable final ItemStack stack) {
        return stack != null && !stack.isEmpty() && isWrench(stack.getItem());
    }

    public static boolean isWrench(final Item item) {
        return ItemTags.WRENCHES.contains(item);
    }

    public static boolean isHoldingWrench(final Entity entity) {
        for (final ItemStack stack : entity.getHandSlots()) {
            if (isWrench(stack.getItem())) {
                return true;
            }
        }

        return false;
    }
}
