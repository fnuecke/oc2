package li.cil.oc2.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.network.message.TerminalBlockInputMessage;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;

public final class TerminalScreen extends Screen {
    private static final Identifier BACKGROUND = new Identifier(API.MOD_ID, "textures/gui/screen/terminal.png");
    private static final Identifier BACKGROUND_TERMINAL_FOCUSED = new Identifier(API.MOD_ID, "textures/gui/screen/terminal_focused.png");
    private static final int TEXTURE_SIZE = 512;
    private static final int SCREEN_WIDTH = 8 + 80 * 8 / 2 + 8;
    private static final int SCREEN_HEIGHT = 8 + 24 * 16 / 2 + 8;
    private static final int TERMINAL_AREA_X = 8;
    private static final int TERMINAL_AREA_Y = 8;
    private static final int TERMINAL_AREA_WIDTH = 80 * 8 / 2;
    private static final int TERMINAL_AREA_HEIGHT = 24 * 16 / 2;

    ///////////////////////////////////////////////////////////////////

    private final ComputerTileEntity tileEntity;
    private final Terminal terminal;
    private final int windowWidth, windowHeight;
    private int windowLeft, windowTop;
    private boolean isMouseOverTerminal;

    ///////////////////////////////////////////////////////////////////

    public TerminalScreen(final ComputerTileEntity tileEntity, final Text title) {
        super(title);
        this.tileEntity = tileEntity;
        terminal = tileEntity.getTerminal();
        windowWidth = SCREEN_WIDTH;
        windowHeight = SCREEN_HEIGHT;
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);

        isMouseOverTerminal = isPointInRegion(TERMINAL_AREA_X, TERMINAL_AREA_Y, TERMINAL_AREA_WIDTH, TERMINAL_AREA_HEIGHT, mouseX, mouseY);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        requireNonNull(client).getTextureManager().bindTexture(BACKGROUND);
        drawTexture(matrixStack, windowLeft, windowTop, 0, 0, windowWidth, windowHeight, TEXTURE_SIZE, TEXTURE_SIZE);

        if (isMouseOverTerminal && tileEntity.isRunning()) {
            requireNonNull(client).getTextureManager().bindTexture(BACKGROUND_TERMINAL_FOCUSED);
            drawTexture(matrixStack, windowLeft, windowTop, 0, 0, windowWidth, windowHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (tileEntity.isRunning()) {
            final MatrixStack stack = new MatrixStack();
            stack.translate(windowLeft + TERMINAL_AREA_X, windowTop + TERMINAL_AREA_Y, this.itemRenderer.zOffset);
            stack.scale(TERMINAL_AREA_WIDTH / (float) terminal.getWidth(), TERMINAL_AREA_HEIGHT / (float) terminal.getHeight(), 1f);
            terminal.render(stack);
        }
    }

    @Override
    public void tick() {
        super.tick();

        final ByteBuffer input = terminal.getInput();
        if (input != null) {
            Network.sendToServer(new TerminalBlockInputMessage(tileEntity, input));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean charTyped(final char ch, final int modifier) {
        terminal.putInput((byte) ch);
        return true;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if ((!isMouseOverTerminal || !tileEntity.isRunning()) && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V) {
            final String value = requireNonNull(client).keyboard.getClipboard();
            for (final char ch : value.toCharArray()) {
                terminal.putInput((byte) ch);
            }
            return true;
        }

        final byte[] sequence = TerminalInput.getSequence(keyCode, modifiers);
        if (sequence != null) {
            for (int i = 0; i < sequence.length; i++) {
                terminal.putInput(sequence[i]);
            }
            return true;
        }

        return false;
    }

    @Override
    public void onClose() {
        super.onClose();

        requireNonNull(client).keyboard.setRepeatEvents(false);
    }

    ///////////////////////////////////////////////////////////////////

    protected void init() {
        super.init();
        this.windowLeft = (this.width - this.windowWidth) / 2;
        this.windowTop = (this.height - this.windowHeight) / 2;

        requireNonNull(client).keyboard.setRepeatEvents(true);
    }

    private boolean isPointInRegion(final int x, final int y, final int width, final int height, double mouseX, double mouseY) {
        mouseX = mouseX - this.windowLeft;
        mouseY = mouseY - this.windowTop;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
