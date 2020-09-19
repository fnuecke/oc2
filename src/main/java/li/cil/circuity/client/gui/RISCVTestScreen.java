package li.cil.circuity.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import li.cil.circuity.Circuity;
import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.client.gui.terminal.Terminal;
import li.cil.circuity.client.gui.terminal.TerminalInput;
import li.cil.circuity.common.vm.VirtualMachineRunner;
import li.cil.circuity.vm.device.memory.UnsafeMemory;
import li.cil.circuity.vm.riscv.R5Board;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public final class RISCVTestScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();

    private VirtualMachineRunner runner;
    private final Terminal terminal = new Terminal();

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
        final PhysicalMemory rom = new UnsafeMemory(128 * 1024);
        final PhysicalMemory memory = new UnsafeMemory(128 * 1014 * 1024);

        board.addDevice(0x80000000, rom);
        board.addDevice(0x80000000 + 0x400000, memory);
        board.reset();

        final String firmware = "../buildroot/fw_jump.bin";
        final String kernel = "../buildroot/Image";

        loadProgramFile(memory, kernel);
        loadProgramFile(rom, firmware);

        runner = new VirtualMachineRunner(board);

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

        // Pump output from VM to VT100.
        moveSerialToTerminal();
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
        runner.putByte((byte) ch);
        return true;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (TerminalInput.KEYCODE_SEQUENCES.containsKey(keyCode)) {
            final byte[] sequence = TerminalInput.KEYCODE_SEQUENCES.get(keyCode);
            for (int i = 0; i < sequence.length; i++) {
                runner.putByte(sequence[i]);
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void moveSerialToTerminal() {
        int value;
        while ((value = runner.readByte()) != -1) {
            terminal.putByte((byte) value);
        }
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
