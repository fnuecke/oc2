package li.cil.oc2.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.common.container.ComputerInventoryContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ComputerContainerScreen extends AbstractMachineInventoryScreen<ComputerInventoryContainer> {
    public ComputerContainerScreen(final ComputerInventoryContainer container, final Inventory inventory, final Component title) {
        super(container, inventory, title);
        imageWidth = Sprites.COMPUTER_CONTAINER.width;
        imageHeight = Sprites.COMPUTER_CONTAINER.height;
        inventoryLabelY = imageHeight - 94;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void renderBg(final PoseStack stack, final float partialTicks, final int mouseX, final int mouseY) {
        super.renderBg(stack, partialTicks, mouseX, mouseY);
        Sprites.COMPUTER_CONTAINER.draw(stack, leftPos, topPos);
    }
}
