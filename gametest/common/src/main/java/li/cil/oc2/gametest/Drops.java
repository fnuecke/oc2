/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class Drops {
    public static List<ItemStack> all(final GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2))
            .stream().map(ItemEntity::getItem).toList();
    }

    public static ItemStack single(final GameTestHelper helper) {
        final List<ItemStack> stacks = all(helper);
        if (stacks.size() != 1) {
            throw new GameTestAssertException("expected exactly one dropped stack, found " + stacks.size());
        }
        return stacks.getFirst();
    }

    public static void assertDropped(final GameTestHelper helper, final Item item, final String what) {
        final List<ItemStack> stacks = all(helper);
        if (stacks.stream().noneMatch(stack -> stack.is(item))) {
            throw new GameTestAssertException(what + " did not drop, got " + stacks);
        }
    }

    // --------------------------------------------------------------------- //

    private Drops() {
    }
}
