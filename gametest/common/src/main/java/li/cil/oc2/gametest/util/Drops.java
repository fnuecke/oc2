/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.util;

import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class Drops {
    public static ItemStack single(final GameTestHelper helper) {
        final List<ItemStack> stacks = all(helper);
        if (stacks.size() != 1) {
            throw new GameTestAssertException(Component.literal("expected exactly one dropped stack, found " + stacks.size()), 0);
        }
        return stacks.getFirst();
    }

    public static void assertDropped(final GameTestHelper helper, final Item item, final String what) {
        final List<ItemStack> stacks = all(helper);
        if (stacks.stream().noneMatch(stack -> stack.is(item))) {
            throw new GameTestAssertException(Component.literal(what + " did not drop, got " + stacks), 0);
        }
    }

    // --------------------------------------------------------------------- //

    private static List<ItemStack> all(final GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2))
            .stream().map(ItemEntity::getItem).toList();
    }

    // --------------------------------------------------------------------- //

    private Drops() {
    }
}
