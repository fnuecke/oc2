package li.cil.oc2.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.common.container.ComputerContainer;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static java.util.Objects.requireNonNull;

public final class ComputerContainerScreen extends HandledScreen<ComputerContainer> {
    private static final Identifier BACKGROUND = new Identifier(API.MOD_ID, "textures/gui/container/computer.png");

    ///////////////////////////////////////////////////////////////////

    public ComputerContainerScreen(final ComputerContainer container, final PlayerInventory inventory, final Text title) {
        super(container, inventory, title);
        backgroundWidth = 196;
        backgroundHeight = 197;
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    ///////////////////////////////////////////////////////////////////


    @Override
    protected void drawBackground(final MatrixStack matrixStack, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        requireNonNull(client).getTextureManager().bindTexture(BACKGROUND);
        drawTexture(matrixStack, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }
}
