/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public abstract class Node {
    Placement placement = Placement.Flow.FIT;
    int minWidth, minHeight;

    BooleanSupplier shouldBeVisible = () -> true;
    boolean visible = true;

    @Nullable
    Supplier<List<Component>> tooltip;

    int x, y, w, h;

    // --------------------------------------------------------------------- //

    public Node size(final int width, final int height) {
        return width(Size.fixed(width)).height(Size.fixed(height));
    }

    public Node width(final Size width) {
        this.placement = new Placement.Flow(width, flow().height());
        return this;
    }

    public Node height(final Size height) {
        this.placement = new Placement.Flow(flow().width(), height);
        return this;
    }

    public Node position(final int x, final int y) {
        requireDefaultPlacement();
        this.placement = new Placement.Fixed(x, y);
        return this;
    }

    public Node overlay() {
        requireDefaultPlacement();
        this.placement = new Placement.Overlay();
        return this;
    }

    public Node visibleWhen(final BooleanSupplier condition) {
        this.shouldBeVisible = condition;
        return this;
    }

    public Node tooltip(final Supplier<List<Component>> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public ScreenRectangle bounds() {
        return new ScreenRectangle(x, y, w, h);
    }

    // --------------------------------------------------------------------- //

    protected void accept(final NodeVisitor visitor) {
    }

    protected int contentWidth() {
        return 0;
    }

    protected int contentHeight() {
        return 0;
    }

    protected void drawBackground(final UiRenderer renderer, final int mouseX, final int mouseY) {
    }

    protected void drawForeground(final UiRenderer renderer, final int mouseX, final int mouseY) {
    }

    @Nullable
    protected List<Component> getTooltip() {
        return tooltip != null ? tooltip.get() : null;
    }

    // --------------------------------------------------------------------- //

    boolean skipFlowLayout() {
        return !visible || !(placement instanceof Placement.Flow);
    }

    int contentSizeAlong(final Axis axis) {
        return axis == Axis.HORIZONTAL ? contentWidth() : contentHeight();
    }

    int minAlong(final Axis axis) {
        return axis == Axis.HORIZONTAL ? minWidth : minHeight;
    }

    int extentAlong(final Axis axis) {
        return axis == Axis.HORIZONTAL ? w : h;
    }

    void setExtentAlong(final Axis axis, final int value) {
        if (axis == Axis.HORIZONTAL) w = value;
        else h = value;
    }

    boolean contains(final double px, final double py) {
        return visible && px >= x && py >= y && px < x + w && py < y + h;
    }

    List<Node> children() {
        return List.of();
    }

    // --------------------------------------------------------------------- //

    private Placement.Flow flow() {
        if (placement instanceof final Placement.Flow flow) {
            return flow;
        }
        throw new IllegalStateException(getClass().getSimpleName() + " has a fixed position or is an overlay; it cannot have a size.");
    }

    private void requireDefaultPlacement() {
        if (!Placement.Flow.FIT.equals(placement)) {
            throw new IllegalStateException(getClass().getSimpleName() + " already has a size or placement.");
        }
    }
}
