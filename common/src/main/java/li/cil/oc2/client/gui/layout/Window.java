/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class Window {
    private static final int PANEL_PADDING = 7;
    private static final int TOOLBAR_TOP = 8;

    private final NodeStack panel;
    private final NodeStack toolbar;
    private final List<Slot> slots = new ArrayList<>();
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final List<ScreenRectangle> extraAreas = new ArrayList<>();

    // --------------------------------------------------------------------- //

    public Window(final Node content, final NodeStack... toolbarSections) {
        this.panel = NodeStack.vertical(content).padding(PANEL_PADDING).background(Theme::panel);
        this.toolbar = NodeStack.vertical(toolbarSections).alignment(Align.START, Align.END);

        final NodeVisitor collectSlotsAndWidgets = new NodeVisitor() {
            @Override
            public void slot(final Node node1, final Slot slot) {
                slots.add(slot);
            }

            @Override
            public void widget(final Node node1, final AbstractWidget widget) {
                widgets.add(widget);
            }
        };
        forEach(node -> node.accept(collectSlotsAndWidgets));
    }

    // --------------------------------------------------------------------- //

    public ScreenRectangle bounds() {
        return panel.bounds();
    }

    public List<Slot> slots() {
        return slots;
    }

    public List<AbstractWidget> widgets() {
        return widgets;
    }

    public boolean isStale() {
        return Layout.isStale(panel) || Layout.isStale(toolbar);
    }

    public void layout(final int screenWidth, final int screenHeight) {
        Layout.updateSizes(panel);
        Layout.updateSizes(toolbar);

        final int x = toolbar.w + panel.w <= screenWidth
            ? Math.max((screenWidth - panel.w) / 2, toolbar.w)
            : toolbar.w + Math.floorDiv(screenWidth - toolbar.w - panel.w, 2);
        final int y = (screenHeight - panel.h) / 2;
        Layout.updatePositions(panel, x, y);
        Layout.updatePositions(toolbar, x - toolbar.w, y + TOOLBAR_TOP);

        final NodeVisitor synchronizeState = new NodeVisitor() {
            @Override
            public void slot(final Node node, final Slot slot) {
                if (!node.visible) {
                    throw new IllegalStateException("Slots cannot be hidden - e.g. server shift-click logic would still hit them.");
                }
                slot.x = node.x + 1 - x;
                slot.y = node.y + 1 - y;
            }

            @Override
            public void widget(final Node node, final AbstractWidget widget) {
                widget.setRectangle(node.w, node.h, node.x, node.y);
                widget.visible = node.visible;
            }
        };
        forEach(node -> node.accept(synchronizeState));

        extraAreas.clear();
        collectExtraAreas(panel, panel.bounds());
        collectExtraAreas(toolbar, panel.bounds());
    }

    public List<ScreenRectangle> extraAreas() {
        return extraAreas;
    }

    public boolean isInExtraArea(final double x, final double y) {
        for (final ScreenRectangle area : extraAreas) {
            if (area.containsPoint((int) x, (int) y)) {
                return true;
            }
        }
        return false;
    }

    public void drawBackground(final UiRenderer renderer, final int mouseX, final int mouseY) {
        forEachVisible(node -> node.drawBackground(renderer, mouseX, mouseY));
    }

    public void drawForeground(final UiRenderer renderer, final int mouseX, final int mouseY) {
        forEachVisible(node -> node.drawForeground(renderer, mouseX, mouseY));
    }

    public void drawTooltip(final UiRenderer renderer, final int mouseX, final int mouseY) {
        final AtomicReference<List<Component>> result = new AtomicReference<>();
        forEachVisible(node -> {
            if (node.contains(mouseX, mouseY)) {
                final List<Component> tooltip = node.getTooltip();
                if (tooltip != null && !tooltip.isEmpty()) {
                    result.set(tooltip);
                }
            }
        });
        if (result.get() != null) {
            renderer.tooltip(result.get(), mouseX, mouseY);
        }
    }

    // --------------------------------------------------------------------- //

    private void forEach(final Consumer<Node> consumer) {
        visit(panel, consumer, false);
        visit(toolbar, consumer, false);
    }

    private void forEachVisible(final Consumer<Node> consumer) {
        visit(panel, consumer, true);
        visit(toolbar, consumer, true);
    }

    private void collectExtraAreas(final Node node, final ScreenRectangle panelBounds) {
        if (!node.visible || node.w == 0 || node.h == 0) {
            return;
        }
        if (!node.bounds().equals(panelBounds.intersection(node.bounds()))) {
            extraAreas.add(node.bounds());
        } else {
            for (final Node child : node.children()) {
                collectExtraAreas(child, panelBounds);
            }
        }
    }

    private static void visit(final Node node, final Consumer<Node> consumer, final boolean skipInvisible) {
        if (skipInvisible && !node.visible) {
            return;
        }
        consumer.accept(node);
        for (final Node child : node.children()) {
            visit(child, consumer, skipInvisible);
        }
    }
}
