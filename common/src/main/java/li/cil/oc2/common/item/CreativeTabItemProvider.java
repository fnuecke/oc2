/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import net.minecraft.world.item.CreativeModeTab;

public interface CreativeTabItemProvider {
    void addCreativeTabItems(final CreativeModeTab.ItemDisplayParameters parameters, final CreativeModeTab.Output output);
}
