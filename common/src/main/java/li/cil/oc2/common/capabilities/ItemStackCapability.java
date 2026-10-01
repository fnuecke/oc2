/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public record ItemStackCapability<T>(T value, Supplier<ItemStack> container) {
}
