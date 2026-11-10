/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

sealed interface Placement {
    boolean growsAlong(Axis axis);

    int minExtent(Node node, Axis axis);

    void resolve(Node node, Axis axis, int parentExtent);

    void updatePosition(Node node, Node parent);

    // --------------------------------------------------------------------- //

    record Flow(Size width, Size height) implements Placement {
        static final Flow FIT = new Flow(Size.FIT, Size.FIT);

        @Override
        public boolean growsAlong(final Axis axis) {
            return along(axis) instanceof Size.Grow;
        }

        @Override
        public int minExtent(final Node node, final Axis axis) {
            return along(axis) instanceof Size.Fixed(final int value) ? value : node.contentSizeAlong(axis);
        }

        @Override
        public void resolve(final Node node, final Axis axis, final int parentExtent) {
            // Resolved via parent layout step.
        }

        @Override
        public void updatePosition(final Node node, final Node parent) {
            // Resolved via parent layout step.
        }

        private Size along(final Axis axis) {
            return axis == Axis.HORIZONTAL ? width : height;
        }
    }

    record Fixed(int x, int y) implements Placement {
        @Override
        public boolean growsAlong(final Axis axis) {
            return false;
        }

        @Override
        public int minExtent(final Node node, final Axis axis) {
            return node.contentSizeAlong(axis);
        }

        @Override
        public void resolve(final Node node, final Axis axis, final int parentExtent) {
            Layout.resolveSizesAlong(node, axis, node.minAlong(axis));
        }

        @Override
        public void updatePosition(final Node node, final Node parent) {
            Layout.updatePositions(node, parent.x + x, parent.y + y);
        }
    }

    record Overlay() implements Placement {
        @Override
        public boolean growsAlong(final Axis axis) {
            return false;
        }

        @Override
        public int minExtent(final Node node, final Axis axis) {
            return 0;
        }

        @Override
        public void resolve(final Node node, final Axis axis, final int parentExtent) {
            Layout.resolveSizesAlong(node, axis, parentExtent);
        }

        @Override
        public void updatePosition(final Node node, final Node parent) {
            Layout.updatePositions(node, parent.x, parent.y);
        }
    }
}
