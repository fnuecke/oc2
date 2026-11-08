/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.node;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.client.gui.layout.Node;
import li.cil.oc2.client.gui.layout.SlotNode;
import li.cil.oc2.client.gui.layout.UiRenderer;
import li.cil.oc2.common.container.DeviceTypeSlotItemHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class MissingDeviceNode extends Node {
    public static final ResourceLocation INFO = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "ui/slot_info");
    public static final ResourceLocation WARN = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "ui/slot_warn");
    private static final int ICON_SIZE = 28;
    private static final int ICON_OFFSET = (SlotNode.SLOT_SIZE - ICON_SIZE) / 2;

    // --------------------------------------------------------------------- //

    private final AbstractContainerMenu menu;
    private final DeviceType type;
    private final ResourceLocation icon;
    private final Component tooltip;

    // --------------------------------------------------------------------- //

    public MissingDeviceNode(final AbstractContainerMenu menu, final DeviceType type, final ResourceLocation icon, final Component tooltip) {
        this.menu = menu;
        this.type = type;
        this.icon = icon;
        this.tooltip = tooltip;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected int contentWidth() {
        return SlotNode.SLOT_SIZE;
    }

    @Override
    protected int contentHeight() {
        return SlotNode.SLOT_SIZE;
    }

    @Override
    protected void drawForeground(final UiRenderer renderer, final int mouseX, final int mouseY) {
        if (isMissing()) {
            final ScreenRectangle bounds = bounds();
            renderer.sprite(icon, bounds.left() + ICON_OFFSET, bounds.top() + ICON_OFFSET, ICON_SIZE, ICON_SIZE);
        }
    }

    @Nullable
    @Override
    protected List<Component> getTooltip() {
        return isMissing() ? List.of(tooltip) : null;
    }

    // --------------------------------------------------------------------- //

    private boolean isMissing() {
        for (final Slot slot : menu.slots) {
            if (slot instanceof final DeviceTypeSlotItemHandler typedSlot && typedSlot.getDeviceType() == type && slot.hasItem()) {
                return false;
            }
        }
        return true;
    }
}
