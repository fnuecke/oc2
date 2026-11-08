/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.node;

import li.cil.oc2.api.API;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.client.gui.DeviceHints;
import li.cil.oc2.client.gui.layout.Node;
import li.cil.oc2.client.gui.layout.SlotNode;
import li.cil.oc2.client.gui.layout.UiRenderer;
import li.cil.oc2.common.container.DeviceTypeSlotItemHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;
import java.util.List;

import static li.cil.oc2.common.util.TranslationUtils.key;

@Environment(EnvType.CLIENT)
public final class MissingDeviceNode extends Node {
    private static final ResourceLocation INFO = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "ui/slot_info");
    private static final ResourceLocation WARN = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "ui/slot_warn");
    private static final int ICON_SIZE = 28;
    private static final int ICON_OFFSET = (SlotNode.SLOT_SIZE - ICON_SIZE) / 2;

    // --------------------------------------------------------------------- //

    private final AbstractContainerMenu menu;
    private final DeviceType type;
    private final List<DeviceHints.Hint> hints;

    // --------------------------------------------------------------------- //

    public MissingDeviceNode(final AbstractContainerMenu menu, final DeviceType type, final List<DeviceHints.Hint> hints) {
        this.menu = menu;
        this.type = type;
        this.hints = hints;
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
        final DeviceHints.Hint hint = activeHint();
        if (hint != null) {
            final ScreenRectangle bounds = bounds();
            renderer.sprite(hint.isRequired() ? WARN : INFO, bounds.left() + ICON_OFFSET, bounds.top() + ICON_OFFSET, ICON_SIZE, ICON_SIZE);
        }
    }

    @Nullable
    @Override
    protected List<Component> getTooltip() {
        final DeviceHints.Hint hint = activeHint();
        if (hint == null) {
            return null;
        }

        return List.of(Component.translatable(key(hint.isRequired() ? "tooltip.{mod}.device_required" : "tooltip.{mod}.device_suggested"), type.getName()));
    }

    // --------------------------------------------------------------------- //

    @Nullable
    private DeviceHints.Hint activeHint() {
        if (isInstalled()) {
            return null;
        }

        for (final DeviceHints.Hint hint : hints) {
            if (hint.item().isEmpty() || isInstalled(hint.item().get())) {
                return hint;
            }
        }
        return null;
    }

    private boolean isInstalled() {
        for (final Slot slot : menu.slots) {
            if (slot instanceof final DeviceTypeSlotItemHandler typedSlot && typedSlot.getDeviceType() == type && slot.hasItem()) {
                return true;
            }
        }
        return false;
    }

    private boolean isInstalled(final ItemPredicate predicate) {
        for (final Slot slot : menu.slots) {
            if (slot instanceof DeviceTypeSlotItemHandler && slot.hasItem() && predicate.test(slot.getItem())) {
                return true;
            }
        }
        return false;
    }
}
