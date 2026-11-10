/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

public final class Layout {
    public static boolean isStale(final Node root) {
        if (root.shouldBeVisible.getAsBoolean() != root.visible) {
            return true;
        }
        if (!root.visible) {
            return false;
        }
        for (final Node child : root.children()) {
            if (isStale(child)) {
                return true;
            }
        }
        return false;
    }

    public static void updateSizes(final Node root) {
        updateVisibility(root, true);
        updateMinimumSizes(root);
        resolveSizesAlong(root, Axis.HORIZONTAL, root.minAlong(Axis.HORIZONTAL));
        resolveSizesAlong(root, Axis.VERTICAL, root.minAlong(Axis.VERTICAL));
    }

    public static void updatePositions(final Node root, final int x, final int y) {
        root.x = x;
        root.y = y;

        if (root instanceof final NodeStack stack) {
            updateChildPositions(stack);
        }
    }

    // --------------------------------------------------------------------- //

    static void resolveSizesAlong(final Node node, final Axis axis, final int extent) {
        node.setExtentAlong(axis, extent);

        if (node instanceof final NodeStack stack) {
            resolveChildSizesAlong(stack, axis, extent);
        }
    }

    // --------------------------------------------------------------------- //

    private static void updateVisibility(final Node node, final boolean parentVisible) {
        node.visible = parentVisible && node.shouldBeVisible.getAsBoolean();
        for (final Node child : node.children()) {
            updateVisibility(child, node.visible);
        }
    }

    private static void updateMinimumSizes(final Node node) {
        node.children().forEach(Layout::updateMinimumSizes);
        node.minWidth = node.placement.minExtent(node, Axis.HORIZONTAL);
        node.minHeight = node.placement.minExtent(node, Axis.VERTICAL);
    }

    private static void updateChildPositions(NodeStack stack) {
        final Axis axis = stack.axis, across = axis.other();
        final int innerAlong = stack.extentAlong(axis) - stack.padding.total(axis);
        final int innerAcross = stack.extentAlong(across) - stack.padding.total(across);

        int totalAlong = 0, count = 0;
        for (final Node child : stack.children()) {
            if (!child.visible) {
                continue;
            }

            child.placement.updatePosition(child, stack);
            if (child.placement instanceof Placement.Flow) {
                totalAlong += child.extentAlong(axis);
                count++;
            }
        }
        totalAlong += stack.spacing * Math.max(0, count - 1);

        int position = stack.padding.start(axis) + stack.alignmentAlongAxis.offset(innerAlong - totalAlong);
        for (final Node child : stack.children()) {
            if (child.skipFlowLayout()) {
                continue;
            }

            final int crossPosition = stack.padding.start(across) + stack.alignmentAcrossAxis.offset(innerAcross - child.extentAlong(across));
            if (axis == Axis.HORIZONTAL) {
                updatePositions(child, stack.x + position, stack.y + crossPosition);
            } else {
                updatePositions(child, stack.x + crossPosition, stack.y + position);
            }
            position += child.extentAlong(axis) + stack.spacing;
        }
    }

    private static void resolveChildSizesAlong(final NodeStack stack, final Axis axis, final int extent) {
        for (final Node child : stack.children()) {
            if (child.visible) {
                child.placement.resolve(child, axis, extent);
            }
        }

        final int inner = extent - stack.padding.total(axis);
        if (axis != stack.axis) {
            for (final Node child : stack.children()) {
                if (child.skipFlowLayout()) {
                    continue;
                }
                final int childExtent = child.placement.growsAlong(axis) ? inner : child.minAlong(axis);
                if (child.minAlong(axis) > inner) {
                    throw overflow(stack, axis, child.minAlong(axis), inner);
                }
                resolveSizesAlong(child, axis, childExtent);
            }
            return;
        }

        int free = inner, growing = 0, count = 0;
        for (final Node child : stack.children()) {
            if (child.skipFlowLayout()) {
                continue;
            }

            free -= child.minAlong(axis);
            count++;
            if (child.placement.growsAlong(axis)) {
                growing++;
            }
        }

        free -= stack.spacing * Math.max(0, count - 1);
        if (free < 0) {
            throw overflow(stack, axis, inner - free, inner);
        }

        int remainder = growing > 0 ? free % growing : 0;
        for (final Node child : stack.children()) {
            if (child.skipFlowLayout()) {
                continue;
            }

            int childExtent = child.minAlong(axis);
            if (child.placement.growsAlong(axis)) {
                childExtent += free / growing;
                if (remainder > 0) {
                    childExtent++;
                    remainder--;
                }
            }
            resolveSizesAlong(child, axis, childExtent);
        }
    }

    private static IllegalStateException overflow(final Node node, final Axis axis, final int required, final int available) {
        return new IllegalStateException(String.format("%s content needs %d %s pixels, but only %d are available.",
            node.getClass().getSimpleName(), required, axis == Axis.HORIZONTAL ? "horizontal" : "vertical", available));
    }
}
