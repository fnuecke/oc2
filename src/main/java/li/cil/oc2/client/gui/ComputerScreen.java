package li.cil.oc2.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.common.container.ComputerContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.Objects;

public final class ComputerScreen extends ContainerScreen<ComputerContainer> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(API.MOD_ID, "textures/gui/container/computer.png");

    public ComputerScreen(final ComputerContainer container, final PlayerInventory inventory, final ITextComponent title) {
        super(container, inventory, title);
        xSize = 196;
        ySize = 197;
    }

    @Override
    public void render(final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        RenderSystem.disableBlend();

        // TODO Render terminal text.

        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        Objects.requireNonNull(minecraft).getTextureManager().bindTexture(BACKGROUND);
        final int x = (width - xSize) / 2;
        final int y = (height - ySize) / 2;
        blit(x, y, 0, 0, xSize, ySize);
    }
}
