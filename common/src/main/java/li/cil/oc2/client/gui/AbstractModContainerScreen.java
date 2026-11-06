/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.container.DeviceTypeSlot;
import li.cil.oc2.common.container.DeviceTypeSlotItemHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

@Environment(EnvType.CLIENT)
public abstract class AbstractModContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public AbstractModContainerScreen(final T container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
    }

    // --------------------------------------------------------------------- //

    public boolean isMouseOver(final int mouseX, final int mouseY, final int x, final int y, final int width, final int height) {
        final int localMouseX = mouseX - leftPos;
        final int localMouseY = mouseY - topPos;
        return localMouseX >= x &&
            localMouseX < x + width &&
            localMouseY >= y &&
            localMouseY < y + height;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.renderBackground(graphics, mouseX, mouseY, partialTicks);

        // Drawn here instead of via Slot.getNoItemIcon so the slot highlight goes on top.
        for (final Slot slot : menu.slots) {
            final DeviceType deviceType;
            if (slot instanceof final DeviceTypeSlot deviceTypeSlot) {
                deviceType = deviceTypeSlot.getDeviceType();
            } else if (slot instanceof final DeviceTypeSlotItemHandler deviceTypeSlot) {
                deviceType = deviceTypeSlot.getDeviceType();
            } else {
                continue;
            }

            if (slot.isActive() && !slot.hasItem()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, deviceType.getBackgroundIcon(), leftPos + slot.x, topPos + slot.y, 16, 16);
            }
        }
    }

    @Override
    public void renderContents(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.renderContents(graphics, mouseX, mouseY, partialTicks);

        renderFg(graphics, partialTicks, mouseX, mouseY);
    }

    // --------------------------------------------------------------------- //


    protected void renderFg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
    }
}
