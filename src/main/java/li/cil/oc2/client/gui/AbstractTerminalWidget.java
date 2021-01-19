package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.client.gui.widget.Sprite;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public abstract class AbstractTerminalWidget extends AbstractGui {
    public static final ResourceLocation BACKGROUND_LOCATION = new ResourceLocation(API.MOD_ID, "textures/gui/screen/terminal.png");
    public static final ResourceLocation TERMINAL_FOCUSED_LOCATION = new ResourceLocation(API.MOD_ID, "textures/gui/screen/terminal_focused.png");
    public static final int TEXTURE_SIZE = 512;

    private static final Sprite BACKGROUND = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 336, 208, 0, 0);
    private static final Sprite CONTROLS_BACKGROUND = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 19, 34, 80, 250);
    private static final Sprite TERMINAL_FOCUSED = new Sprite(TERMINAL_FOCUSED_LOCATION, TEXTURE_SIZE, 336, 208, 0, 0);

    private static final Sprite CAPTURE_INPUT_BASE = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 15, 241);
    private static final Sprite CAPTURE_INPUT_PRESSED = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 29, 241);
    private static final Sprite CAPTURE_INPUT_ACTIVE = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 1, 241);

    private static final Sprite POWER_BASE = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 15, 255);
    private static final Sprite POWER_PRESSED = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 29, 255);
    private static final Sprite POWER_ACTIVE = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 1, 255);

    public static final int TERMINAL_WIDTH = Terminal.WIDTH * Terminal.CHAR_WIDTH / 2;
    public static final int TERMINAL_HEIGHT = Terminal.HEIGHT * Terminal.CHAR_HEIGHT / 2;

    public static final int MARGIN_SIZE = 8;
    public static final int TERMINAL_X = MARGIN_SIZE;
    public static final int TERMINAL_Y = MARGIN_SIZE;

    public static final int WIDTH = TERMINAL_WIDTH + MARGIN_SIZE * 2;
    public static final int HEIGHT = TERMINAL_HEIGHT + MARGIN_SIZE * 2;

    private static final int CONTROLS_TOP = 8;

    private static boolean isInputCaptureEnabled;

    ///////////////////////////////////////////////////////////////////

    private final Screen parent;
    private final Terminal terminal;
    private int windowLeft, windowTop;
    private boolean isMouseOverTerminal;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTerminalWidget(final Screen parent, final Terminal terminal) {
        this.parent = parent;
        this.terminal = terminal;
    }

    public void renderBackground(final MatrixStack matrixStack, final int mouseX, final int mouseY) {
        isMouseOverTerminal = isOverTerminal(mouseX, mouseY);

        RenderSystem.color4f(1f, 1f, 1f, 1f);
        getClient().getTextureManager().bindTexture(BACKGROUND_LOCATION);

        CONTROLS_BACKGROUND.draw(matrixStack, windowLeft - CONTROLS_BACKGROUND.width, windowTop + CONTROLS_TOP);
        BACKGROUND.draw(matrixStack, windowLeft, windowTop);

        if (shouldCaptureInput()) {
            TERMINAL_FOCUSED.draw(matrixStack, windowLeft, windowTop);
        }
    }

    public void render(final MatrixStack matrixStack, @Nullable final ITextComponent error) {
        if (isRunning()) {
            final MatrixStack stack = new MatrixStack();
            stack.translate(windowLeft + TERMINAL_X, windowTop + TERMINAL_Y, getClient().getItemRenderer().zLevel);
            stack.scale(TERMINAL_WIDTH / (float) terminal.getWidth(), TERMINAL_HEIGHT / (float) terminal.getHeight(), 1f);
            terminal.render(stack);
        } else {
            final FontRenderer font = getClient().fontRenderer;
            if (error != null) {
                final int textWidth = font.getStringPropertyWidth(error);
                final int textOffsetX = (TERMINAL_WIDTH - textWidth) / 2;
                final int textOffsetY = (TERMINAL_HEIGHT - font.FONT_HEIGHT) / 2;
                font.func_243246_a(matrixStack,
                        error,
                        windowLeft + TERMINAL_X + textOffsetX,
                        windowTop + TERMINAL_Y + textOffsetY,
                        0xEE3322);
            }
        }
    }

    public void tick() {
        final ByteBuffer input = terminal.getInput();
        if (input != null) {
            sendTerminalInputToServer(input);
        }
    }

    public boolean charTyped(final char ch, final int modifier) {
        terminal.putInput((byte) ch);
        return true;
    }

    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (!shouldCaptureInput() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V) {
            final String value = getClient().keyboardListener.getClipboardString();
            for (final char ch : value.toCharArray()) {
                terminal.putInput((byte) ch);
            }
        } else {
            final byte[] sequence = TerminalInput.getSequence(keyCode, modifiers);
            if (sequence != null) {
                for (int i = 0; i < sequence.length; i++) {
                    terminal.putInput(sequence[i]);
                }
            }
        }

        return true;
    }

    public void init() {
        this.windowLeft = (parent.width - WIDTH) / 2;
        this.windowTop = (parent.height - HEIGHT) / 2;

        getClient().keyboardListener.enableRepeatEvents(true);

        addButton(new ToggleImageButton(
                parent, windowLeft - CONTROLS_BACKGROUND.width + 4, windowTop + CONTROLS_TOP + 4,
                12, 12,
                new TranslationTextComponent(Constants.COMPUTER_SCREEN_POWER_CAPTION),
                new TranslationTextComponent(Constants.COMPUTER_SCREEN_POWER_DESCRIPTION),
                POWER_BASE,
                POWER_PRESSED,
                POWER_ACTIVE
        ) {
            @Override
            public void onPress() {
                super.onPress();
                sendPowerStateToServer(!isRunning());
            }

            @Override
            public boolean isToggled() {
                return isRunning();
            }
        });

        addButton(new ToggleImageButton(
                parent, windowLeft - CONTROLS_BACKGROUND.width + 4, windowTop + CONTROLS_TOP + 18,
                12, 12,
                new TranslationTextComponent(Constants.COMPUTER_SCREEN_CAPTURE_INPUT_CAPTION),
                new TranslationTextComponent(Constants.COMPUTER_SCREEN_CAPTURE_INPUT_DESCRIPTION),
                CAPTURE_INPUT_BASE,
                CAPTURE_INPUT_PRESSED,
                CAPTURE_INPUT_ACTIVE
        ) {
            @Override
            public void onPress() {
                super.onPress();
                isInputCaptureEnabled = !isInputCaptureEnabled;
            }

            @Override
            public boolean isToggled() {
                return isInputCaptureEnabled;
            }
        });
    }

    public void onClose() {
        getClient().keyboardListener.enableRepeatEvents(false);
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract boolean isRunning();

    protected void addButton(final Widget widget) {
    }

    protected abstract void sendPowerStateToServer(boolean value);

    protected abstract void sendTerminalInputToServer(final ByteBuffer input);

    ///////////////////////////////////////////////////////////////////

    private Minecraft getClient() {
        return parent.getMinecraft();
    }

    private boolean shouldCaptureInput() {
        return isMouseOverTerminal && isInputCaptureEnabled && isRunning();
    }

    private boolean isOverTerminal(final int mouseX, final int mouseY) {
        final int localMouseX = mouseX - windowLeft;
        final int localMouseY = mouseY - windowTop;
        return localMouseX >= AbstractTerminalWidget.TERMINAL_X &&
               localMouseX < AbstractTerminalWidget.TERMINAL_X + AbstractTerminalWidget.TERMINAL_WIDTH &&
               localMouseY >= AbstractTerminalWidget.TERMINAL_Y &&
               localMouseY < AbstractTerminalWidget.TERMINAL_Y + AbstractTerminalWidget.TERMINAL_HEIGHT;
    }
}
