/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.gui.components.AbstractWidget;

public class WidgetNode extends Node {
    private final AbstractWidget widget;
    private final int contentWidth, contentHeight;

    // --------------------------------------------------------------------- //

    public WidgetNode(final AbstractWidget widget) {
        this.widget = widget;
        this.contentWidth = widget.getWidth();
        this.contentHeight = widget.getHeight();
    }

    // --------------------------------------------------------------------- //
    // Node

    @Override
    protected void accept(final NodeVisitor visitor) {
        visitor.widget(this, widget);
    }

    @Override
    protected int contentWidth() {
        return contentWidth;
    }

    @Override
    protected int contentHeight() {
        return contentHeight;
    }
}
