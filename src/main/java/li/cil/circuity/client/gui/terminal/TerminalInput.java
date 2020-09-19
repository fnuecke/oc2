package li.cil.circuity.client.gui.terminal;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.lwjgl.glfw.GLFW;

public final class TerminalInput {
    public static final Int2ObjectArrayMap<byte[]> KEYCODE_SEQUENCES = new Int2ObjectArrayMap<>();

    static {
        addSequence(GLFW.GLFW_KEY_ENTER, '\n');
        addSequence(GLFW.GLFW_KEY_TAB, '\t');
        addSequence(GLFW.GLFW_KEY_BACKSPACE, '\b');

        addSequence(GLFW.GLFW_KEY_HOME, "[1~");
        addSequence(GLFW.GLFW_KEY_INSERT, "[2~");
        addSequence(GLFW.GLFW_KEY_DELETE, "[3~");
        addSequence(GLFW.GLFW_KEY_END, "[4~");
        addSequence(GLFW.GLFW_KEY_PAGE_UP, "[5~");
        addSequence(GLFW.GLFW_KEY_PAGE_DOWN, "[6~");

        addSequence(GLFW.GLFW_KEY_UP, "[A");
        addSequence(GLFW.GLFW_KEY_DOWN, "[B");
        addSequence(GLFW.GLFW_KEY_RIGHT, "[C");
        addSequence(GLFW.GLFW_KEY_LEFT, "[D");
    }

    private static void addSequence(final int keyCode, final char ch) {
        addSequence(keyCode, (byte) ch);
    }

    private static void addSequence(final int keyCode, final String sequence) {
        final byte[] bytes = new byte[1 + sequence.length()];
        bytes[0] = 27;
        final char[] chars = sequence.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            bytes[i + 1] = (byte) chars[i];
        }
        addSequence(keyCode, bytes);
    }

    private static void addSequence(final int keyCode, final byte... sequence) {
        KEYCODE_SEQUENCES.put(keyCode, sequence);
    }
}
