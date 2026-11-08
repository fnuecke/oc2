/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.world.inventory.Slot;

public interface NodeVisitor {
    default void slot(final Node node, final Slot slot) {
    }

    default void widget(final Node node, final AbstractWidget widget) {
    }
}
