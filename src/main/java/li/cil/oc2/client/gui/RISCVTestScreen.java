package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.oc2.client.gui.terminal.Terminal;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.common.vm.VirtualMachineRunner;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.device.PhysicalMemory;
import li.cil.sedna.buildroot.Buildroot;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.memory.Memory;
import li.cil.sedna.device.serial.UART16550A;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import li.cil.sedna.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.device.virtio.VirtIOKeyboardDevice;
import li.cil.sedna.riscv.R5Board;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class RISCVTestScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final boolean USE_VIRTIO_CONSOLE = true;
    private static final boolean USE_VIRTIO_KEYBOARD = false;

    private final Terminal terminal = new Terminal();
    private VirtualMachineRunner runner;
    private UART16550A uart;
    private VirtIOBlockDevice hdd;

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

        try {
            createVirtualMachine();
        } catch (final Throwable e) {
            LOGGER.error(e);
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.sendChatMessage("Failed creating VM: " + e.toString());
                minecraft.displayGuiScreen(null);
            }
        }

        if (minecraft != null) {
            minecraft.keyboardListener.enableRepeatEvents(true);
        }
    }

    @Override
    public void removed() {
        super.removed();

        if (hdd != null) {
            try {
                hdd.close();
            } catch (final IOException e) {
                LOGGER.error(e);
            }
        }

        if (minecraft != null) {
            minecraft.keyboardListener.enableRepeatEvents(false);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (runner != null) {
            // Allow VM to run for the next second.
            runner.tick();
        }
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
        if (USE_VIRTIO_KEYBOARD) {
            return false;
        }
        terminal.putInput((byte) ch);
        return true;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (USE_VIRTIO_KEYBOARD) {
            if (KeyCodeMapping.MAPPING.containsKey(keyCode)) {
                keyboard.sendKeyEvent(KeyCodeMapping.MAPPING.get(keyCode), true);
                return true;
            }
        } else {
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_V) {
                final String value = Objects.requireNonNull(minecraft).keyboardListener.getClipboardString();
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
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
        if (USE_VIRTIO_KEYBOARD) {
            if (KeyCodeMapping.MAPPING.containsKey(keyCode)) {
                keyboard.sendKeyEvent(KeyCodeMapping.MAPPING.get(keyCode), false);
                return true;
            }
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void createVirtualMachine() throws Throwable {
        final R5Board board = new R5Board();
        final PhysicalMemory rom = Memory.create(128 * 1024);
        final PhysicalMemory memory = Memory.create(32 * 1014 * 1024);
        hdd = new VirtIOBlockDevice(board.getMemoryMap(),
                ByteBufferBlockDevice.createFromStream(Buildroot.getRootFilesystem(), true));
        uart = new UART16550A();
        console = new VirtIOConsoleDevice(board.getMemoryMap());
        keyboard = new VirtIOKeyboardDevice(board.getMemoryMap());

        final StringBuilder bootargs = new StringBuilder();
        bootargs.append("root=/dev/vda ro");

        if (USE_VIRTIO_CONSOLE) {
            board.addDevice(console);
            bootargs.append(" console=hvc0");
        } else {
            board.addDevice(uart);
            bootargs.append(" console=ttyS0");
        }

        console.getInterrupt().set(0x1, board.getInterruptController());
        keyboard.getInterrupt().set(0x2, board.getInterruptController());
        hdd.getInterrupt().set(0x3, board.getInterruptController());
        uart.getInterrupt().set(0xA, board.getInterruptController());

        board.addDevice(keyboard);
        board.addDevice(hdd);
        board.addDevice(0x80000000, rom);
        board.addDevice(0x80000000 + 0x400000, memory);

        board.setBootargs(bootargs.toString());

        board.reset();

        loadProgramFile(memory, Buildroot.getLinuxImage());
        loadProgramFile(rom, Buildroot.getFirmware());
        board.installDeviceTree(0x80000000 + 0x02200000);

        runner = new ConsoleRunner(board);
    }

    private static void loadProgramFile(final PhysicalMemory memory, final InputStream stream) {
        try {
            final BufferedInputStream bis = new BufferedInputStream(stream);
            for (int address = 0, value = bis.read(); value != -1; value = bis.read(), address++) {
                memory.store(address, (byte) value, Sizes.SIZE_8_LOG2);
            }
        } catch (final Throwable e) {
            LOGGER.error(e);
        }
    }

    private final class ConsoleRunner extends VirtualMachineRunner {
        // Thread-local buffers for lock-free read/writes in inner loop.
        private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
        private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(32);

        public ConsoleRunner(final R5Board board) {
            super(board);
        }

        @Override
        protected void handleBeforeRun() {
            int value;
            while ((value = terminal.readInput()) != -1) {
                inputBuffer.enqueue((byte) value);
            }
        }

        @Override
        protected void step() {
            if (USE_VIRTIO_CONSOLE) {
                boolean wrote = false;
                while (!inputBuffer.isEmpty() && console.canPutByte()) {
                    wrote = true;
                    console.putByte(inputBuffer.dequeueByte());
                }
                if (wrote) {
                    console.flush();
                }

                int value;
                while ((value = console.read()) != -1) {
                    outputBuffer.enqueue((byte) value);
                }
            } else {
                while (!inputBuffer.isEmpty() && uart.canPutByte()) {
                    uart.putByte(inputBuffer.dequeueByte());
                }

                int value;
                while ((value = uart.read()) != -1) {
                    outputBuffer.enqueue((byte) value);
                }
            }
        }

        @Override
        protected void handleAfterRun() {
            while (!outputBuffer.isEmpty()) {
                terminal.putOutput(outputBuffer.dequeueByte());
            }
        }
    }
}
