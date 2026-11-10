/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.container.DeviceTypeSlot;
import li.cil.oc2.common.container.DeviceTypeSlotItemHandler;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

public class SlotNode extends Node {
    public static final int SLOT_SIZE = 18;

    // --------------------------------------------------------------------- //

    private final Slot slot;

    // --------------------------------------------------------------------- //

    public SlotNode(final Slot slot) {
        this.slot = slot;
    }

    // --------------------------------------------------------------------- //
    // Node

    @Override
    protected void accept(final NodeVisitor visitor) {
        visitor.slot(this, slot);
    }

    @Override
    protected int contentWidth() {
        return SLOT_SIZE;
    }

    @Override
    protected int contentHeight() {
        return SLOT_SIZE;
    }

    @Override
    protected void drawBackground(final UiRenderer renderer, final int mouseX, final int mouseY) {
        renderer.sprite(renderer.theme().slot(), x, y, w, h);

        // Drawn here instead of via Slot.getNoItemIcon so the slot highlight goes on top.
        final DeviceType deviceType = getDeviceType();
        if (deviceType != null && !slot.hasItem()) {
            renderer.sprite(deviceType.getBackgroundIcon(), x + 1, y + 1, 16, 16);
        }
    }

    // --------------------------------------------------------------------- //

    @Nullable
    private DeviceType getDeviceType() {
        if (slot instanceof final DeviceTypeSlot deviceTypeSlot) {
            return deviceTypeSlot.getDeviceType();
        } else if (slot instanceof final DeviceTypeSlotItemHandler deviceTypeSlot) {
            return deviceTypeSlot.getDeviceType();
        } else {
            return null;
        }
    }
}
