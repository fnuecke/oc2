package li.cil.oc2.client.gui.terminal;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

public final class TerminalInput {
    private static final Int2ObjectArrayMap<Int2ObjectArrayMap<byte[]>> KEYCODE_SEQUENCES = new Int2ObjectArrayMap<>();

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

        addSequence(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_C, (char) 3);
        addSequence(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_D, (char) 4);
        addSequence(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_Z, (char) 0x1A);
    }

    public static byte[] getSequence(final int keyCode) {
        return getSequence(0, keyCode);
    }

    @Nullable
    public static byte[] getSequence(final int modifiers, final int keyCode) {
        final Int2ObjectArrayMap<byte[]> map = KEYCODE_SEQUENCES.get(modifiers);
        if (map == null) {
            return null;
        }
        return map.get(keyCode);
    }

    private static void addSequence(final int keyCode, final char ch) {
        addSequence(0, keyCode, ch);
    }

    private static void addSequence(final int modifiers, final int keyCode, final char ch) {
        addSequence(modifiers, keyCode, (byte) ch);
    }

    private static void addSequence(final int keyCode, final String sequence) {
        addSequence(0, keyCode, sequence);
    }

    private static void addSequence(final int modifiers, final int keyCode, final String sequence) {
        final byte[] bytes = new byte[1 + sequence.length()];
        bytes[0] = 27;
        final char[] chars = sequence.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            bytes[i + 1] = (byte) chars[i];
        }
        addSequence(modifiers, keyCode, bytes);
    }

    private static void addSequence(final int modifiers, final int keyCode, final byte... sequence) {
        KEYCODE_SEQUENCES
                .computeIfAbsent(modifiers, i -> new Int2ObjectArrayMap<>())
                .put(keyCode, sequence);
    }
}
