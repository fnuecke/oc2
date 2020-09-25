package li.cil.circuity.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.client.gui.terminal.Terminal;
import li.cil.circuity.client.gui.terminal.TerminalInput;
import li.cil.circuity.common.vm.VirtualMachineRunner;
import li.cil.circuity.vm.device.UART16550A;
import li.cil.circuity.vm.device.memory.Memory;
import li.cil.circuity.vm.device.virtio.VirtIOConsoleDevice;
import li.cil.circuity.vm.device.virtio.VirtIOKeyboardDevice;
import li.cil.circuity.vm.riscv.R5Board;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Objects;

public final class RISCVTestScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final boolean USE_VIRT_IO = false;

    private final Terminal terminal = new Terminal();
    private VirtualMachineRunner runner;
    private UART16550A uart;

    private VirtIOConsoleDevice console;
    private VirtIOKeyboardDevice keyboard;

    public RISCVTestScreen() {
        super(new StringTextComponent("RISC-V"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        final R5Board board = new R5Board();
        final PhysicalMemory rom = Memory.create(128 * 1024);
        final PhysicalMemory memory = Memory.create(128 * 1014 * 1024);

        if (USE_VIRT_IO) {
            uart = new UART16550A();
            uart.getInterrupt().set(0xA, board.getInterruptController());

            board.addDevice(uart);
        } else {
            console = new VirtIOConsoleDevice(board.getMemoryMap());
            console.getInterrupt().set(0x1, board.getInterruptController());

            keyboard = new VirtIOKeyboardDevice(board.getMemoryMap());
            keyboard.getInterrupt().set(0x2, board.getInterruptController());

            board.addDevice(console);
            board.addDevice(keyboard);
        }

        board.addDevice(0x80000000, rom);
        board.addDevice(0x80000000 + 0x400000, memory);
        board.reset();

        final String firmware = "../buildroot/fw_jump.bin";
        final String kernel = "../buildroot/Image";

        loadProgramFile(memory, kernel);
        loadProgramFile(rom, firmware);

        runner = new VirtualMachineRunner(board) {
            // Thread-local buffers for lock-free read/writes in inner loop.
            private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
            private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);

            @Override
            protected void handleBeforeRun() {
                int value;
                while ((value = terminal.readInput()) != -1) {
                    inputBuffer.enqueue((byte) value);
                }
            }

            @Override
            protected void step() {
                if (USE_VIRT_IO) {
                    boolean wrote = false;
                    while (!inputBuffer.isEmpty() && console.canPutByte()) {
                        wrote = true;
                        console.putByte(inputBuffer.dequeueByte());
                    }
                    if (wrote) {
                        console.flush();
                    }
                } else {
                    while (!inputBuffer.isEmpty() && uart.canPutByte()) {
                        uart.putByte(inputBuffer.dequeueByte());
                    }
                }

                int value;
                while ((value = uart.read()) != -1) {
                    outputBuffer.enqueue((byte) value);
                }
            }

            @Override
            protected void handleAfterRun() {
                while (!outputBuffer.isEmpty()) {
                    terminal.putOutput(outputBuffer.dequeueByte());
                }
            }
        };

        if (minecraft != null) {
            minecraft.keyboardListener.enableRepeatEvents(true);
        }
    }

    @Override
    public void removed() {
        super.removed();

        if (minecraft != null) {
            minecraft.keyboardListener.enableRepeatEvents(false);
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Allow VM to run for the next second.
        runner.tick();
    }

    @Override
    public void render(final int mouseX, final int mouseY, final float partialTicks) {
        renderBackground();
        super.render(mouseX, mouseY, partialTicks);

        final MatrixStack stack = new MatrixStack();
        stack.translate(0, 0, this.itemRenderer.zLevel);
        stack.scale(0.5f, 0.5f, 0.5f);
        stack.translate((width - terminal.getWidth() * 0.5f) / 2f,
                (height - terminal.getHeight() * 0.5f) / 2f,
                0);
        terminal.render(stack);
    }

    @Override
    public boolean charTyped(final char ch, final int modifier) {
        terminal.putInput((byte) ch);
        return true;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (USE_VIRT_IO) {
            if (KeyCodeMapping.MAPPING.containsKey(keyCode)) {
                keyboard.sendKeyEvent(KeyCodeMapping.MAPPING.get(keyCode), true);
                return true;
            }
        } else {
            if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
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
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
        if (USE_VIRT_IO) {
            if (KeyCodeMapping.MAPPING.containsKey(keyCode)) {
                keyboard.sendKeyEvent(KeyCodeMapping.MAPPING.get(keyCode), false);
                return true;
            }
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private static void loadProgramFile(final PhysicalMemory memory, final String path) {
        try {
            try (final FileInputStream is = new FileInputStream(path)) {
                final BufferedInputStream bis = new BufferedInputStream(is);
                for (int value = bis.read(), address = 0; value != -1; value = bis.read()) {
                    memory.store(address++, (byte) value, Sizes.SIZE_8_LOG2);
                }
            }
        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }
}
