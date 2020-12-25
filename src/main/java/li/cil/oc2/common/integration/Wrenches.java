package li.cil.oc2.common.integration;

import li.cil.oc2.common.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Predicate;

public final class Wrenches {
    private static final ArrayList<Predicate<Item>> WRENCHES = new ArrayList<>();

    static {
        WRENCHES.add((item) -> item == Items.WRENCH_ITEM.get());
    }

    public static void addWrenchFilter(final Predicate<Item> filter) {
        WRENCHES.add(filter);
    }

    public static boolean isWrench(@Nullable final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return isWrench(stack.getItem());
    }

    public static boolean isWrench(final Item item) {
        for (final Predicate<Item> wrench : WRENCHES) {
            if (wrench.test(item)) {
                return true;
            }
        }

        return false;
    }
}
