package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.client.gui.widget.Sprite;
import li.cil.oc2.client.gui.widget.ToggleImageButton;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.client.gui.GuiUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static li.cil.oc2.common.util.TooltipUtils.withColor;

public abstract class AbstractTerminalWidget extends GuiComponent {
    public static final ResourceLocation BACKGROUND_LOCATION = new ResourceLocation(API.MOD_ID, "textures/gui/screen/terminal.png");
    public static final ResourceLocation TERMINAL_FOCUSED_LOCATION = new ResourceLocation(API.MOD_ID, "textures/gui/screen/terminal_focused.png");
    public static final int TEXTURE_SIZE = 512;

    private static final Sprite BACKGROUND = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 336, 208, 0, 0);

    private static final Sprite CONTROLS_BACKGROUND = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 19, 34, 50, 250);

    private static final Sprite POWER_BASE = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 15, 255);
    private static final Sprite POWER_PRESSED = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 29, 255);
    private static final Sprite POWER_ACTIVE = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 1, 255);

    private static final Sprite CAPTURE_INPUT_BASE = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 15, 241);
    private static final Sprite CAPTURE_INPUT_PRESSED = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 29, 241);
    private static final Sprite CAPTURE_INPUT_ACTIVE = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 12, 1, 241);

    private static final Sprite ENERGY_BACKGROUND = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 19, 34, 80, 250);

    private static final Sprite ENERGY_BASE = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 26, 110, 250);
    private static final Sprite ENERGY_BAR = new Sprite(BACKGROUND_LOCATION, TEXTURE_SIZE, 12, 26, 125, 250);

    private static final Sprite TERMINAL_FOCUSED = new Sprite(TERMINAL_FOCUSED_LOCATION, TEXTURE_SIZE, 336, 208, 0, 0);

    public static final int TERMINAL_WIDTH = Terminal.WIDTH * Terminal.CHAR_WIDTH / 2;
    public static final int TERMINAL_HEIGHT = Terminal.HEIGHT * Terminal.CHAR_HEIGHT / 2;

    public static final int MARGIN_SIZE = 8;
    public static final int TERMINAL_X = MARGIN_SIZE;
    public static final int TERMINAL_Y = MARGIN_SIZE;

    public static final int WIDTH = TERMINAL_WIDTH + MARGIN_SIZE * 2;
    public static final int HEIGHT = TERMINAL_HEIGHT + MARGIN_SIZE * 2;

    private static final int CONTROLS_TOP = 8;
    private static final int ENERGY_TOP = CONTROLS_TOP + CONTROLS_BACKGROUND.height + 4;

    private static boolean isInputCaptureEnabled;

    ///////////////////////////////////////////////////////////////////

    private final Screen parent;
    private final Terminal terminal;
    private int windowLeft, windowTop;
    private boolean isMouseOverTerminal;

    private int currentEnergy, maxEnergy, energyConsumption;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTerminalWidget(final Screen parent, final Terminal terminal) {
        this.parent = parent;
        this.terminal = terminal;
    }

    public void setEnergyInfo(final int current, final int capacity, final int consumption) {
        this.currentEnergy = current;
        this.maxEnergy = capacity;
        this.energyConsumption = consumption;
    }

    public void renderBackground(final PoseStack matrixStack, final int mouseX, final int mouseY) {
        isMouseOverTerminal = isMouseOverTerminal(mouseX, mouseY);

        RenderSystem.color4f(1f, 1f, 1f, 1f);
        getClient().getTextureManager().bind(BACKGROUND_LOCATION);

        CONTROLS_BACKGROUND.draw(matrixStack, windowLeft - CONTROLS_BACKGROUND.width, windowTop + CONTROLS_TOP);

        if (maxEnergy > 0) {
            final int x = windowLeft - ENERGY_BACKGROUND.width;
            final int y = windowTop + ENERGY_TOP;
            ENERGY_BACKGROUND.draw(matrixStack, x, y);
            ENERGY_BASE.draw(matrixStack, x + 4, y + 4);
        }

        BACKGROUND.draw(matrixStack, windowLeft, windowTop);

        if (shouldCaptureInput()) {
            TERMINAL_FOCUSED.draw(matrixStack, windowLeft, windowTop);
        }
    }

    public void render(final PoseStack matrixStack, final int mouseX, final int mouseY, @Nullable final Component error) {
        if (isRunning()) {
            final PoseStack stack = new PoseStack();
            stack.translate(windowLeft + TERMINAL_X, windowTop + TERMINAL_Y, getClient().getItemRenderer().blitOffset);
            stack.scale(TERMINAL_WIDTH / (float) terminal.getWidth(), TERMINAL_HEIGHT / (float) terminal.getHeight(), 1f);
            terminal.render(stack);
        } else {
            final Font font = getClient().font;
            if (error != null) {
                final int textWidth = font.width(error);
                final int textOffsetX = (TERMINAL_WIDTH - textWidth) / 2;
                final int textOffsetY = (TERMINAL_HEIGHT - font.lineHeight) / 2;
                font.draw(matrixStack,
                        error,
                        windowLeft + TERMINAL_X + textOffsetX,
                        windowTop + TERMINAL_Y + textOffsetY,
                        0xEE3322);
            }
        }

        if (maxEnergy > 0) {
            ENERGY_BAR.drawFillY(matrixStack, windowLeft - ENERGY_BACKGROUND.width + 4, windowTop + ENERGY_TOP + 4, currentEnergy / (float) maxEnergy);

            if (isMouseOver(mouseX, mouseY, -ENERGY_BACKGROUND.width + 4, ENERGY_TOP + 4, ENERGY_BAR.width, ENERGY_BAR.height)) {
                final List<? extends FormattedText> tooltip = Arrays.asList(
                        new TranslatableComponent(Constants.TOOLTIP_ENERGY, withColor(currentEnergy + "/" + maxEnergy, TextFormatting.GREEN)),
                        new TranslatableComponent(Constants.TOOLTIP_ENERGY_CONSUMPTION, withColor(String.valueOf(energyConsumption), TextFormatting.GREEN))
                );
                GuiUtils.drawHoveringText(matrixStack, tooltip, mouseX, mouseY, parent.width, parent.height, 200, getClient().font);
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
            final String value = getClient().keyboardHandler.getClipboard();
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

        getClient().keyboardHandler.setSendRepeatsToGui(true);

        addButton(new ToggleImageButton(
                parent, windowLeft - CONTROLS_BACKGROUND.width + 4, windowTop + CONTROLS_TOP + 4,
                12, 12,
                new TranslatableComponent(Constants.COMPUTER_SCREEN_POWER_CAPTION),
                new TranslatableComponent(Constants.COMPUTER_SCREEN_POWER_DESCRIPTION),
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
        getClient().keyboardHandler.setSendRepeatsToGui(false);
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

    private boolean isMouseOverTerminal(final int mouseX, final int mouseY) {
        return isMouseOver(mouseX, mouseY,
                AbstractTerminalWidget.TERMINAL_X, AbstractTerminalWidget.TERMINAL_Y,
                AbstractTerminalWidget.TERMINAL_WIDTH, AbstractTerminalWidget.TERMINAL_HEIGHT);
    }

    private boolean isMouseOver(final int mouseX, final int mouseY, final int x, final int y, final int width, final int height) {
        final int localMouseX = mouseX - windowLeft;
        final int localMouseY = mouseY - windowTop;
        return localMouseX >= x &&
               localMouseX < x + width &&
               localMouseY >= y &&
               localMouseY < y + height;
    }
}
