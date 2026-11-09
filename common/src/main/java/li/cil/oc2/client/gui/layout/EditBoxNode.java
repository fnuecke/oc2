/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class EditBoxNode extends NodeStack {
    public static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final Insets PADDING = new Insets(3, 5, 3, 5);
    private static final int FIELD_HEIGHT = 9;

    // --------------------------------------------------------------------- //

    private final EditBox editBox;

    // --------------------------------------------------------------------- //

    public EditBoxNode(final Component message) {
        super(Axis.VERTICAL);
        editBox = new ResizableEditBox(message);
        editBox.setBordered(false);
        editBox.setTextColor(TEXT_COLOR);
        add(new WidgetNode(editBox).width(Size.GROW));
        padding(PADDING);
        background(Theme::insetBlack);
    }

    // --------------------------------------------------------------------- //

    public EditBox editBox() {
        return editBox;
    }

    // --------------------------------------------------------------------- //

    private static final class ResizableEditBox extends EditBox {
        ResizableEditBox(final Component message) {
            super(Minecraft.getInstance().font, 0, 0, 0, FIELD_HEIGHT, message);
        }

        @Override
        public void setWidth(final int width) {
            super.setWidth(width);
            setCursorPosition(getCursorPosition());
        }

        @Override
        public void setSize(final int width, final int height) {
            super.setSize(width, height);
            setCursorPosition(getCursorPosition());
        }
    }
}
