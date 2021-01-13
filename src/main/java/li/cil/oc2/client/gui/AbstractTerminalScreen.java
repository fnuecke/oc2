package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.client.gui.widget.Sprite;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachineState;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;

public abstract class AbstractTerminalScreen extends Screen {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(API.MOD_ID, "textures/gui/screen/terminal.png");
    private static final ResourceLocation BACKGROUND_TERMINAL_FOCUSED = new ResourceLocation(API.MOD_ID, "textures/gui/screen/terminal_focused.png");
    private static final int TEXTURE_SIZE = 512;

    private static final Sprite CONTROLS_BACKGROUND = new Sprite(BACKGROUND, TEXTURE_SIZE, 19, 34, 82, 244);

    private static final Sprite CAPTURE_INPUT_BASE = new Sprite(BACKGROUND, TEXTURE_SIZE, 12, 12, 15, 241);
    private static final Sprite CAPTURE_INPUT_PRESSED = new Sprite(BACKGROUND, TEXTURE_SIZE, 12, 12, 29, 241);
    private static final Sprite CAPTURE_INPUT_ACTIVE = new Sprite(BACKGROUND, TEXTURE_SIZE, 12, 12, 1, 241);

    private static final Sprite POWER_BASE = new Sprite(BACKGROUND, TEXTURE_SIZE, 12, 12, 15, 255);
    private static final Sprite POWER_PRESSED = new Sprite(BACKGROUND, TEXTURE_SIZE, 12, 12, 29, 255);
    private static final Sprite POWER_ACTIVE = new Sprite(BACKGROUND, TEXTURE_SIZE, 12, 12, 1, 255);

    private static final int SCREEN_WIDTH = 336;
    private static final int SCREEN_HEIGHT = 208;
    private static final int TERMINAL_AREA_X = 8;
    private static final int TERMINAL_AREA_Y = 8;
    private static final int TERMINAL_AREA_WIDTH = 80 * 8 / 2;
    private static final int TERMINAL_AREA_HEIGHT = 24 * 16 / 2;
    private static final int CONTROLS_TOP = 8;

    private static boolean enableInputCapture;

    ///////////////////////////////////////////////////////////////////

    private final VirtualMachineState state;
    private final Terminal terminal;
    private final int windowWidth, windowHeight;
    private int windowLeft, windowTop;
    private boolean isMouseOverTerminal;

    ///////////////////////////////////////////////////////////////////

    public AbstractTerminalScreen(final VirtualMachineState state, final Terminal terminal, final ITextComponent title) {
        super(title);
        this.state = state;
        this.terminal = terminal;
        windowWidth = SCREEN_WIDTH;
        windowHeight = SCREEN_HEIGHT;
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground(matrixStack);

        isMouseOverTerminal = isPointInRegion(TERMINAL_AREA_X, TERMINAL_AREA_Y, TERMINAL_AREA_WIDTH, TERMINAL_AREA_HEIGHT, mouseX, mouseY);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        requireNonNull(minecraft).getTextureManager().bindTexture(BACKGROUND);
        blit(matrixStack, windowLeft, windowTop, 0, 0, windowWidth, windowHeight, TEXTURE_SIZE, TEXTURE_SIZE);

        CONTROLS_BACKGROUND.draw(matrixStack, windowLeft - CONTROLS_BACKGROUND.width, windowTop + CONTROLS_TOP);

        if (shouldCaptureInput()) {
            requireNonNull(minecraft).getTextureManager().bindTexture(BACKGROUND_TERMINAL_FOCUSED);
            blit(matrixStack, windowLeft, windowTop, 0, 0, windowWidth, windowHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (state.isRunning()) {
            final MatrixStack stack = new MatrixStack();
            stack.translate(windowLeft + TERMINAL_AREA_X, windowTop + TERMINAL_AREA_Y, this.itemRenderer.zLevel);
            stack.scale(TERMINAL_AREA_WIDTH / (float) terminal.getWidth(), TERMINAL_AREA_HEIGHT / (float) terminal.getHeight(), 1f);
            terminal.render(stack);
        } else {
            final ITextComponent bootError = state.getBootError();
            if (bootError != null) {
                final int textWidth = font.getStringPropertyWidth(bootError);
                final int textOffsetX = (TERMINAL_AREA_WIDTH - textWidth) / 2;
                final int textOffsetY = (TERMINAL_AREA_HEIGHT - font.FONT_HEIGHT) / 2;
                font.func_243246_a(matrixStack,
                        bootError,
                        windowLeft + TERMINAL_AREA_X + textOffsetX,
                        windowTop + TERMINAL_AREA_Y + textOffsetY,
                        0xEE3322);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        final ByteBuffer input = terminal.getInput();
        if (input != null) {
            sendTerminalInputToServer(input);
        }

        assert minecraft != null;
        final ClientPlayerEntity player = minecraft.player;
        assert player != null;
        if (!player.isAlive() || !canInteractWith(player)) {
            closeScreen();
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
        if (!shouldCaptureInput() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V) {
            final String value = requireNonNull(minecraft).keyboardListener.getClipboardString();
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

        requireNonNull(minecraft).keyboardListener.enableRepeatEvents(false);
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract void sendPowerStateToServer(boolean value);

    protected abstract void sendTerminalInputToServer(final ByteBuffer input);

    protected abstract boolean canInteractWith(PlayerEntity player);

    protected void init() {
        super.init();
        this.windowLeft = (this.width - this.windowWidth) / 2;
        this.windowTop = (this.height - this.windowHeight) / 2;

        requireNonNull(minecraft).keyboardListener.enableRepeatEvents(true);

        addButton(new ToggleImageButton(
                this, windowLeft - CONTROLS_BACKGROUND.width + 4, windowTop + CONTROLS_TOP + 4,
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
                sendPowerStateToServer(!state.isRunning());
            }

            @Override
            public boolean isToggled() {
                return state.isRunning();
            }
        });

        addButton(new ToggleImageButton(
                this, windowLeft - CONTROLS_BACKGROUND.width + 4, windowTop + CONTROLS_TOP + 18,
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
                enableInputCapture = !enableInputCapture;
            }

            @Override
            public boolean isToggled() {
                return enableInputCapture;
            }
        });
    }

    ///////////////////////////////////////////////////////////////////

    private boolean shouldCaptureInput() {
        return isMouseOverTerminal && enableInputCapture && state.isRunning();
    }

    private boolean isPointInRegion(final int x, final int y, final int width, final int height, double mouseX, double mouseY) {
        mouseX = mouseX - this.windowLeft;
        mouseY = mouseY - this.windowTop;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
