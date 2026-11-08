/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.ResourceLocation;

import java.util.function.DoubleSupplier;

public class ProgressBarNode extends Node {
    @FunctionalInterface
    public interface FillRenderer {
        void draw(UiRenderer renderer, int x, int y, int width, int height);
    }

    private static final int BORDER = 1;

    // --------------------------------------------------------------------- //

    public static FillRenderer sprite(final ResourceLocation sprite) {
        return (renderer, x, y, width, height) -> renderer.sprite(sprite, x, y, width, height);
    }

    // --------------------------------------------------------------------- //

    private final Axis axis;
    private final DoubleSupplier progress;
    private final FillRenderer fill;

    // --------------------------------------------------------------------- //

    public ProgressBarNode(final Axis axis, final DoubleSupplier progress, final FillRenderer fill) {
        this.axis = axis;
        this.progress = progress;
        this.fill = fill;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void drawBackground(final UiRenderer renderer, final int mouseX, final int mouseY) {
        final ScreenRectangle bounds = bounds();
        renderer.sprite(renderer.theme().inset(), bounds.left(), bounds.top(), bounds.width(), bounds.height());

        final int x = bounds.left() + BORDER, y = bounds.top() + BORDER;
        final int width = bounds.width() - 2 * BORDER, height = bounds.height() - 2 * BORDER;
        final int filled = filledLength(progress.getAsDouble(), axis == Axis.HORIZONTAL ? width : height);
        if (filled == 0) {
            return;
        }

        if (axis == Axis.HORIZONTAL) {
            renderer.pushScissor(x, y, x + filled, y + height);
        } else {
            renderer.pushScissor(x, y + height - filled, x + width, y + height);
        }
        fill.draw(renderer, x, y, width, height);
        renderer.popScissor();
    }

    // --------------------------------------------------------------------- //

    static int filledLength(final double progress, final int length) {
        if (!(progress > 0)) { // catch nans as well
            return 0;
        }
        return Math.max(1, (int) Math.round(Math.min(1, progress) * length));
    }
}
