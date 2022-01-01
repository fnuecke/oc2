package li.cil.oc2.client.gui;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractModContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    public AbstractModContainerScreen(final T container, final Inventory playerInventory, final Component title) {
        super(container, playerInventory, title);
    }

    ///////////////////////////////////////////////////////////////////

    public boolean isMouseOver(final int mouseX, final int mouseY, final int x, final int y, final int width, final int height) {
        final int localMouseX = mouseX - leftPos;
        final int localMouseY = mouseY - topPos;
        return localMouseX >= x &&
            localMouseX < x + width &&
            localMouseY >= y &&
            localMouseY < y + height;
    }
}
