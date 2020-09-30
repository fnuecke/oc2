package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.terminal.Terminal;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.common.network.ComputerTerminalInputMessage;
import li.cil.oc2.common.network.Network;
import li.cil.oc2.common.tile.ComputerTileEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class TerminalScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(API.MOD_ID, "textures/gui/screen/terminal.png");
    private static final ResourceLocation BACKGROUND_TERMINAL_FOCUSED = new ResourceLocation(API.MOD_ID, "textures/gui/screen/terminal_focused.png");
    private static final int TEXTURE_SIZE = 512;
    private static final int SCREEN_WIDTH = 8 + 80 * 9 / 2 + 8;
    private static final int SCREEN_HEIGHT = 8 + 24 * 16 / 2 + 8;
    private static final int TERMINAL_AREA_X = 8;
    private static final int TERMINAL_AREA_Y = 8;
    private static final int TERMINAL_AREA_WIDTH = 80 * 9 / 2;
    private static final int TERMINAL_AREA_HEIGHT = 24 * 16 / 2;

    private final ComputerTileEntity tileEntity;
    private final Terminal terminal;
    private final int windowWidth, windowHeight;
    private int windowLeft, windowTop;
    private boolean isMouseOverTerminal;

    public TerminalScreen(final ComputerTileEntity tileEntity, final ITextComponent title) {
        super(title);
        this.tileEntity = tileEntity;
        terminal = tileEntity.getTerminal();
        windowWidth = SCREEN_WIDTH;
        windowHeight = SCREEN_HEIGHT;
    }

    @Override
    public void render(final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground();

        isMouseOverTerminal = isPointInRegion(TERMINAL_AREA_X, TERMINAL_AREA_Y, TERMINAL_AREA_WIDTH, TERMINAL_AREA_HEIGHT, mouseX, mouseY);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        Objects.requireNonNull(minecraft).getTextureManager().bindTexture(BACKGROUND);
        blit(windowLeft, windowTop, 0, 0, windowWidth, windowHeight, TEXTURE_SIZE, TEXTURE_SIZE);

        if (isMouseOverTerminal) {
            Objects.requireNonNull(minecraft).getTextureManager().bindTexture(BACKGROUND_TERMINAL_FOCUSED);
            blit(windowLeft, windowTop, 0, 0, windowWidth, windowHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        super.render(mouseX, mouseY, partialTicks);

        final MatrixStack stack = new MatrixStack();
        stack.translate(windowLeft + TERMINAL_AREA_X, windowTop + TERMINAL_AREA_Y, this.itemRenderer.zLevel);
        stack.scale(TERMINAL_AREA_WIDTH / (float) terminal.getWidth(), TERMINAL_AREA_HEIGHT / (float) terminal.getHeight(), 1f);
        terminal.render(stack);
    }

    @Override
    public void tick() {
        super.tick();

        final ByteBuffer input = terminal.getInput();
        if (input != null) {
            Network.INSTANCE.sendToServer(new ComputerTerminalInputMessage(tileEntity, input));
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
        if (!isMouseOverTerminal && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V) {
            final String value = Objects.requireNonNull(minecraft).keyboardListener.getClipboardString();
            for (final char ch : value.toCharArray()) {
                terminal.putInput((byte) ch);
            }
            return true;
        }

        if (TerminalInput.KEYCODE_SEQUENCES.containsKey(keyCode)) {
            final byte[] sequence = TerminalInput.KEYCODE_SEQUENCES.get(keyCode);
            for (int i = 0; i < sequence.length; i++) {
                terminal.putInput(sequence[i]);
            }
            return true;
        }

        return false;
    }

    protected void init() {
        super.init();
        this.windowLeft = (this.width - this.windowWidth) / 2;
        this.windowTop = (this.height - this.windowHeight) / 2;
    }

    private boolean isPointInRegion(int x, int y, int width, int height, double mouseX, double mouseY) {
        mouseX = mouseX - this.windowLeft;
        mouseY = mouseY - this.windowTop;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
