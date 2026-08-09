/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import net.minecraft.world.item.ItemStack;

public final class ItemGroup {
    public static final net.minecraft.world.item.CreativeModeTab COMMON = new net.minecraft.world.item.CreativeModeTab(API.MOD_ID + ".common") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.COMPUTER.get());
        }
    };
}
