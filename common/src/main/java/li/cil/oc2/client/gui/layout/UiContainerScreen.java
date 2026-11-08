/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class UiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private static final float FOREGROUND_Z = 320;

    // --------------------------------------------------------------------- //

    private final Theme theme;
    @Nullable
    private Window window;

    // --------------------------------------------------------------------- //

    protected UiContainerScreen(final T menu, final Inventory inventory, final Component title, final Theme theme) {
        super(menu, inventory, title);
        this.theme = theme;
    }

    // --------------------------------------------------------------------- //

    public List<Rect2i> getExtraAreas() {
        final List<Rect2i> areas = new ArrayList<>();
        if (window != null) {
            for (final ScreenRectangle area : window.extraAreas()) {
                areas.add(new Rect2i(area.left(), area.top(), area.width(), area.height()));
            }
        }
        return areas;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final Window window = window();
        if (window.isStale()) {
            layout(window);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);

        if (menu.getCarried().isEmpty() && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            window.drawTooltip(new UiRenderer(graphics, theme), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    // --------------------------------------------------------------------- //

    protected abstract Window createWindow();

    @Override
    protected void init() {
        super.init();
        if (window == null) {
            window = createWindow();
            for (final Slot slot : menu.slots) {
                if (!window.slots().contains(slot)) {
                    throw new IllegalStateException("Slot " + slot.index + " is not placed by the window.");
                }
            }
        }
        layout(window);
        window.widgets().forEach(this::addRenderableWidget);
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        window().drawBackground(new UiRenderer(graphics, theme), mouseX, mouseY);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(-leftPos, -topPos, FOREGROUND_Z);
        window().drawForeground(new UiRenderer(graphics, theme), mouseX, mouseY);
        graphics.pose().popPose();
    }

    @Override
    protected boolean hasClickedOutside(final double mouseX, final double mouseY, final int left, final int top, final int button) {
        return super.hasClickedOutside(mouseX, mouseY, left, top, button) && !window().isInExtraArea(mouseX, mouseY);
    }

    // --------------------------------------------------------------------- //

    private void layout(final Window window) {
        window.layout(width, height);
        final ScreenRectangle bounds = window.bounds();
        imageWidth = bounds.width();
        imageHeight = bounds.height();
        leftPos = bounds.left();
        topPos = bounds.top();
    }

    private Window window() {
        if (window == null) {
            throw new IllegalStateException("Screen was not initialized.");
        }
        return window;
    }
}
