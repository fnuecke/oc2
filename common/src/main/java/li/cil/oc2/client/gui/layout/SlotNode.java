/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.world.inventory.Slot;

public class SlotNode extends Node {
    public static final int SLOT_SIZE = 18;

    // --------------------------------------------------------------------- //

    private final Slot slot;

    // --------------------------------------------------------------------- //

    public SlotNode(final Slot slot) {
        this.slot = slot;
    }

    // --------------------------------------------------------------------- //
    // Node

    @Override
    protected void accept(final NodeVisitor visitor) {
        visitor.slot(this, slot);
    }

    @Override
    protected int contentWidth() {
        return SLOT_SIZE;
    }

    @Override
    protected int contentHeight() {
        return SLOT_SIZE;
    }

    @Override
    protected void drawBackground(final UiRenderer renderer, final int mouseX, final int mouseY) {
        renderer.sprite(renderer.theme().slot(), x, y, w, h);
    }
}
