/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

@Environment(EnvType.CLIENT)
public abstract class AbstractModContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public AbstractModContainerScreen(final T container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
    }

    // ------------------------------------------------------------- //

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

        renderFg(graphics, partialTicks, mouseX, mouseY);

        renderTooltip(graphics, mouseX, mouseY);
    }

    // ------------------------------------------------------------- //


    protected void renderFg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
    }
}
