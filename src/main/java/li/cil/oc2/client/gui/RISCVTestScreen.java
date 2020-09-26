package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.oc2.client.gui.terminal.Terminal;
import li.cil.oc2.client.gui.terminal.TerminalInput;
import li.cil.oc2.common.vm.VirtualMachineRunner;
import li.cil.sedna.api.vm.device.memory.PhysicalMemory;
import li.cil.sedna.api.vm.device.memory.Sizes;
import li.cil.sedna.vm.device.ByteBufferBlockDevice;
import li.cil.sedna.vm.device.UART16550A;
import li.cil.sedna.vm.device.memory.Memory;
import li.cil.sedna.vm.device.virtio.VirtIOBlockDevice;
import li.cil.sedna.vm.device.virtio.VirtIOConsoleDevice;
import li.cil.sedna.vm.device.virtio.VirtIOKeyboardDevice;
import li.cil.sedna.vm.riscv.R5Board;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        if (USE_VIRTIO_KEYBOARD) {
            if (KeyCodeMapping.MAPPING.containsKey(keyCode)) {
                keyboard.sendKeyEvent(KeyCodeMapping.MAPPING.get(keyCode), false);
                return true;
            }
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void createVirtualMachine() throws Throwable {
        final String firmware = "../buildroot/output/images/fw_jump.bin";
        final String kernel = "../buildroot/output/images/Image";
        final File rootfsFile = new File("../buildroot/output/images/rootfs.ext2");

        final R5Board board = new R5Board();
        final PhysicalMemory rom = Memory.create(128 * 1024);
        final PhysicalMemory memory = Memory.create(32 * 1014 * 1024);
        hdd = new VirtIOBlockDevice(board.getMemoryMap(), ByteBufferBlockDevice.createFromFile(rootfsFile, true));
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

        loadProgramFile(memory, kernel);
        loadProgramFile(rom, firmware);

        runner = new ConsoleRunner(board);
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
