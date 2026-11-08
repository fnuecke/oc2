/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class NodeStack extends Node {
    private final List<Node> children = new ArrayList<>();
    @Nullable
    private Function<Theme, ResourceLocation> background;

    final Axis axis;
    Align alignmentAlongAxis = Align.START, alignmentAcrossAxis = Align.START;
    Insets padding = Insets.NONE;
    int spacing = 4;

    // --------------------------------------------------------------------- //

    public static NodeStack horizontal(final Node... children) {
        return new NodeStack(Axis.HORIZONTAL).add(children);
    }

    public static NodeStack vertical(final Node... children) {
        return new NodeStack(Axis.VERTICAL).add(children);
    }

    // --------------------------------------------------------------------- //

    NodeStack(final Axis axis) {
        this.axis = axis;
    }

    // --------------------------------------------------------------------- //

    public NodeStack add(final Node... children) {
        Collections.addAll(this.children, children);
        return this;
    }

    public NodeStack padding(final Insets padding) {
        this.padding = padding;
        return this;
    }

    public NodeStack padding(final int padding) {
        return padding(Insets.of(padding));
    }

    public NodeStack spacing(final int spacing) {
        this.spacing = spacing;
        return this;
    }

    public NodeStack alignment(final Align alongAxis, final Align acrossAxis) {
        this.alignmentAlongAxis = alongAxis;
        this.alignmentAcrossAxis = acrossAxis;
        return this;
    }

    public NodeStack background(final ResourceLocation sprite) {
        this.background = theme -> sprite;
        return this;
    }

    public NodeStack background(final Function<Theme, ResourceLocation> sprite) {
        this.background = sprite;
        return this;
    }

    // --------------------------------------------------------------------- //
    // Node

    @Override
    public NodeStack size(final int width, final int height) {
        super.size(width, height);
        return this;
    }

    @Override
    public NodeStack width(final Size width) {
        super.width(width);
        return this;
    }

    @Override
    public NodeStack height(final Size height) {
        super.height(height);
        return this;
    }

    @Override
    public NodeStack position(final int x, final int y) {
        super.position(x, y);
        return this;
    }

    @Override
    public NodeStack overlay() {
        super.overlay();
        return this;
    }

    @Override
    public NodeStack visibleWhen(final BooleanSupplier condition) {
        super.visibleWhen(condition);
        return this;
    }

    @Override
    public NodeStack tooltip(final Supplier<List<Component>> tooltip) {
        super.tooltip(tooltip);
        return this;
    }

    @Override
    protected int contentWidth() {
        return contentSizeAlong(Axis.HORIZONTAL);
    }

    @Override
    protected int contentHeight() {
        return contentSizeAlong(Axis.VERTICAL);
    }

    @Override
    protected void drawBackground(final UiRenderer renderer, final int mouseX, final int mouseY) {
        if (background != null) {
            renderer.sprite(background.apply(renderer.theme()), x, y, w, h);
        }
    }

    @Override
    List<Node> children() {
        return children;
    }

    // --------------------------------------------------------------------- //

    int contentSizeAlong(final Axis along) {
        int content = 0, count = 0, nonFlowPlacement = 0;
        for (final Node child : children) {
            if (!child.visible) {
                continue;
            }
            if (child.skipFlowLayout()) {
                nonFlowPlacement++;
                continue;
            }
            if (along == axis) {
                content += child.minAlong(along);
            } else {
                content = Math.max(content, child.minAlong(along));
            }
            count++;
        }
        if (count == 0 && nonFlowPlacement > 0) {
            throw new IllegalStateException("cannot compute size with only non-flow placement nodes");
        }
        if (along == axis) {
            content += spacing * Math.max(0, count - 1);
        }
        return content + padding.total(along);
    }
}
