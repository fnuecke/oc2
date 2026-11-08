/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.node;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc2.api.API;
import li.cil.oc2.client.gui.InputCapture;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.client.renderer.TerminalRenderer;
import li.cil.oc2.common.vm.device.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class TerminalWidget extends AbstractWidget {
    private static final int WIDTH = Terminal.WIDTH * Terminal.CHAR_WIDTH / 2;
    private static final int HEIGHT = Terminal.HEIGHT * Terminal.CHAR_HEIGHT / 2;

    private static final ResourceLocation FOCUSED = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "ui/inset_focused");

    private static final int WHEEL_UP_BUTTON = 64;
    private static final int WHEEL_DOWN_BUTTON = 65;
    private static final int WHEEL_SCROLL_LINES = 3;

    // --------------------------------------------------------------------- //

    private final Terminal terminal;
    private final BooleanSupplier isRunning;
    private final Supplier<Component> error;
    private final Consumer<ByteBuffer> inputSender;
    @Nullable private TerminalRenderer terminalRenderer;

    // --------------------------------------------------------------------- //

    public TerminalWidget(final Terminal terminal, final BooleanSupplier isRunning,
                          final Supplier<Component> error, final Consumer<ByteBuffer> inputSender) {
        super(0, 0, WIDTH, HEIGHT, CommonComponents.EMPTY);
        this.terminal = terminal;
        this.isRunning = isRunning;
        this.error = error;
        this.inputSender = inputSender;
    }

    // --------------------------------------------------------------------- //


    public void tick() {
        final ByteBuffer input = terminal.getInput();
        if (input != null) {
            inputSender.accept(input);
        }
    }

    public void close() {
        if (terminalRenderer != null) {
            terminalRenderer.close();
            terminalRenderer = null;
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return putMouseEvent(mouseX, mouseY, toReportedButton(button), true);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        return putMouseEvent(mouseX, mouseY, toReportedButton(button), false);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double deltaX, final double deltaY) {
        if (deltaY == 0) {
            return false;
        }

        if (putMouseEvent(mouseX, mouseY, deltaY > 0 ? WHEEL_UP_BUTTON : WHEEL_DOWN_BUTTON, true)) {
            return true;
        }

        return shouldCaptureInput() && terminal.putScroll(deltaY > 0, WHEEL_SCROLL_LINES);
    }

    @Override
    public boolean charTyped(final char ch, final int modifier) {
        final boolean isControlChord = (modifier & GLFW.GLFW_MOD_CONTROL) != 0
            && (modifier & GLFW.GLFW_MOD_ALT) == 0;
        if (!isControlChord) {
            putInput(String.valueOf(ch));
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (!shouldCaptureInput() && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V) {
            terminal.putPaste(Minecraft.getInstance().keyboardHandler.getClipboard());
        } else {
            final byte[] sequence = TerminalInput.getSequence(keyCode, modifiers, terminal.isCursorKeyApplicationMode(), terminal.isNewLineMode());
            if (sequence != null) {
                for (final byte b : sequence) {
                    terminal.putInput(b);
                }
            }
        }

        return true;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        if (shouldCaptureInput()) {
            final int padding = 1;
            graphics.blitSprite(FOCUSED, getX() - padding, getY() - padding, width + 2 * padding, height + 2 * padding);
        }

        if (isRunning.getAsBoolean()) {
            final PoseStack terminalStack = new PoseStack();
            terminalStack.translate(getX(), getY(), 0);
            terminalStack.scale(WIDTH / (float) terminal.getWidth(), HEIGHT / (float) terminal.getHeight(), 1f);

            if (terminalRenderer == null) {
                terminalRenderer = new TerminalRenderer(terminal);
            }

            final Minecraft minecraft = Minecraft.getInstance();
            final Matrix4f projectionMatrix = new Matrix4f().setOrtho(0,
                minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), 0, -10, 10f);
            terminalRenderer.render(terminalStack, new Matrix4f(), projectionMatrix);
        } else {
            final Component message = error.get();
            if (message != null) {
                final Font font = Minecraft.getInstance().font;
                graphics.drawString(font, message,
                    getX() + (width - font.width(message)) / 2,
                    getY() + (height - font.lineHeight) / 2,
                    0xEE3322);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(final NarrationElementOutput output) {
    }

    // --------------------------------------------------------------------- //

    private boolean shouldCaptureInput() {
        return isHovered() && InputCapture.isEnabled() && isRunning.getAsBoolean();
    }

    private void putInput(final String value) {
        for (final byte b : value.getBytes(StandardCharsets.UTF_8)) {
            terminal.putInput(b);
        }
    }

    private static int toReportedButton(final int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> 2;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> 1;
            default -> button;
        };
    }

    private boolean putMouseEvent(final double mouseX, final double mouseY, final int button, final boolean isPressed) {
        if (!shouldCaptureInput() || !terminal.isMouseReportingEnabled()) {
            return false;
        }

        final int column = ((int) mouseX - getX()) * 2 / Terminal.CHAR_WIDTH;
        final int row = ((int) mouseY - getY()) * 2 / Terminal.CHAR_HEIGHT;
        terminal.putMouseEvent(button,
            Math.clamp(column, 0, Terminal.WIDTH - 1),
            Math.clamp(row, 0, Terminal.HEIGHT - 1),
            isPressed);

        return true;
    }
}
