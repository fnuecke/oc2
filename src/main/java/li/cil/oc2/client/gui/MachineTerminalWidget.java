package li.cil.oc2.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.common.container.AbstractMachineTerminalContainer;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

@OnlyIn(Dist.CLIENT)
public final class MachineTerminalWidget extends GuiComponent {
    private static final int TERMINAL_WIDTH = Terminal.WIDTH * Terminal.CHAR_WIDTH / 2;
    private static final int TERMINAL_HEIGHT = Terminal.HEIGHT * Terminal.CHAR_HEIGHT / 2;

    private static final int MARGIN_SIZE = 8;
    private static final int TERMINAL_X = MARGIN_SIZE;
    private static final int TERMINAL_Y = MARGIN_SIZE;

    public static final int WIDTH = Sprites.TERMINAL_SCREEN.width;
    public static final int HEIGHT = Sprites.TERMINAL_SCREEN.height;

    ///////////////////////////////////////////////////////////////////

    private final AbstractMachineTerminalScreen<?> parent;
    private final AbstractMachineTerminalContainer container;
    private final Terminal terminal;
    private int leftPos, topPos;
    private boolean isMouseOverTerminal;
    private Terminal.RendererView rendererView;

    ///////////////////////////////////////////////////////////////////

    public MachineTerminalWidget(final AbstractMachineTerminalScreen<?> parent) {
        this.parent = parent;
        this.container = this.parent.getMenu();
        this.terminal = this.container.getTerminal();
    }

    public void renderBackground(final PoseStack stack, final int mouseX, final int mouseY) {
        isMouseOverTerminal = isMouseOverTerminal(mouseX, mouseY);

        Sprites.TERMINAL_SCREEN.draw(stack, leftPos, topPos);

        if (shouldCaptureInput()) {
            Sprites.TERMINAL_FOCUSED.draw(stack, leftPos, topPos);
        }
    }

    public void render(final PoseStack stack, final int mouseX, final int mouseY, @Nullable final Component error) {
        if (container.getVirtualMachine().isRunning()) {
            final PoseStack terminalStack = new PoseStack();
            terminalStack.translate(leftPos + TERMINAL_X, topPos + TERMINAL_Y, getClient().getItemRenderer().blitOffset);
            terminalStack.scale(TERMINAL_WIDTH / (float) terminal.getWidth(), TERMINAL_HEIGHT / (float) terminal.getHeight(), 1f);

            if (rendererView == null) {
                rendererView = terminal.getRenderer();
            }

            final Matrix4f projectionMatrix = Matrix4f.orthographic(0, parent.width, 0, parent.height, -10, 10f);
            rendererView.render(terminalStack, projectionMatrix);
        } else {
            final Font font = getClient().font;
            if (error != null) {
                final int textWidth = font.width(error);
                final int textOffsetX = (TERMINAL_WIDTH - textWidth) / 2;
                final int textOffsetY = (TERMINAL_HEIGHT - font.lineHeight) / 2;
                font.drawShadow(stack,
                    error,
                    leftPos + TERMINAL_X + textOffsetX,
                    topPos + TERMINAL_Y + textOffsetY,
                    0xEE3322);
            }
        }
    }

    public void tick() {
        final ByteBuffer input = terminal.getInput();
        if (input != null) {
            container.sendTerminalInputToServer(input);
        }
    }

    public boolean charTyped(final char ch, final int modifier) {
        if (modifier == 0 || modifier == GLFW.GLFW_MOD_SHIFT) {
            terminal.putInput((byte) ch);
        }
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
                for (final byte b : sequence) {
                    terminal.putInput(b);
                }
            }
        }

        return true;
    }

    public void init() {
        this.leftPos = (parent.width - WIDTH) / 2;
        this.topPos = (parent.height - HEIGHT) / 2;

        getClient().keyboardHandler.setSendRepeatsToGui(true);
    }

    public void onClose() {
        getClient().keyboardHandler.setSendRepeatsToGui(false);
        if (rendererView != null) {
            terminal.releaseRenderer(rendererView);
            rendererView = null;
        }
    }

    ///////////////////////////////////////////////////////////////////

    private Minecraft getClient() {
        return parent.getMinecraft();
    }

    private boolean shouldCaptureInput() {
        return isMouseOverTerminal && AbstractMachineTerminalScreen.isInputCaptureEnabled() &&
            container.getVirtualMachine().isRunning();
    }

    private boolean isMouseOverTerminal(final int mouseX, final int mouseY) {
        return parent.isMouseOver(mouseX, mouseY,
            MachineTerminalWidget.TERMINAL_X, MachineTerminalWidget.TERMINAL_Y,
            MachineTerminalWidget.TERMINAL_WIDTH, MachineTerminalWidget.TERMINAL_HEIGHT);
    }
}
