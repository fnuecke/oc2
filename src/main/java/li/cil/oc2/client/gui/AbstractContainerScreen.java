package li.cil.oc2.client.gui;

import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;

public abstract class AbstractContainerScreen<T extends Container> extends ContainerScreen<T> {
    public AbstractContainerScreen(final T container, final PlayerInventory playerInventory, final ITextComponent title) {
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
