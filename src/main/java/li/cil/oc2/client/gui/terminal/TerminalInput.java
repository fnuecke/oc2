package li.cil.oc2.client.gui.terminal;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class TerminalInput {
    private static final Int2ObjectArrayMap<Int2ObjectArrayMap<byte[]>> KEYCODE_SEQUENCES = new Int2ObjectArrayMap<>();

    static {
        addSequence(GLFW.GLFW_KEY_ENTER, '\n');
        addSequence(GLFW.GLFW_KEY_TAB, '\t');
        addSequence(GLFW.GLFW_KEY_BACKSPACE, '\b');

        addSequence(GLFW.GLFW_KEY_ESCAPE, "\33");
        addSequence(GLFW.GLFW_KEY_HOME, "\033[1~");
        addSequence(GLFW.GLFW_KEY_INSERT, "\033[2~");
        addSequence(GLFW.GLFW_KEY_DELETE, "\033[3~");
        addSequence(GLFW.GLFW_KEY_END, "\033[4~");
        addSequence(GLFW.GLFW_KEY_PAGE_UP, "\033[5~");
        addSequence(GLFW.GLFW_KEY_PAGE_DOWN, "\033[6~");

        addSequence(GLFW.GLFW_KEY_UP, "\033[A");
        addSequence(GLFW.GLFW_KEY_DOWN, "\033[B");
        addSequence(GLFW.GLFW_KEY_RIGHT, "\033[C");
        addSequence(GLFW.GLFW_KEY_LEFT, "\033[D");

        for (int i = 'A'; i <= 'Z'; i++) {
            addSequence(GLFW.GLFW_MOD_CONTROL,
                    GLFW.GLFW_KEY_A + (i - 'A'),
                    (byte) (1 + i - 'A'));
            addSequence(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT,
                    GLFW.GLFW_KEY_A + (i - 'A'),
                    (byte) 27, (byte) ('a' + i - 'A'));
            addSequence(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SHIFT,
                    GLFW.GLFW_KEY_A + (i - 'A'),
                    (byte) 27, (byte) i);
        }

        addSequence(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_LEFT_BRACKET, (byte) 27);
        addSequence(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_BACKSLASH, (byte) 28);
        addSequence(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_RIGHT_BRACKET, (byte) 29);
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    public static byte[] getSequence(final int keyCode) {
        return getSequence(keyCode, 0);
    }

    @Nullable
    public static byte[] getSequence(final int keyCode, final int modifiers) {
        final Int2ObjectArrayMap<byte[]> map = KEYCODE_SEQUENCES.get(modifiers);
        if (map == null) {
            return null;
        }
        return map.get(keyCode);
    }

    ///////////////////////////////////////////////////////////////////

    private static void addSequence(final int keyCode, final char ch) {
        addSequence(keyCode, (byte) ch);
    }

    private static void addSequence(final int keyCode, final byte... sequence) {
        addSequence(0, keyCode, sequence);
    }

    private static void addSequence(final int keyCode, final String sequence) {
        addSequence(0, keyCode, sequence);
    }

    private static void addSequence(final int modifiers, final int keyCode, final String sequence) {
        final byte[] bytes = new byte[sequence.length()];
        final char[] chars = sequence.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        addSequence(modifiers, keyCode, bytes);
    }

    private static void addSequence(final int modifiers, final int keyCode, final byte... sequence) {
        KEYCODE_SEQUENCES
                .computeIfAbsent(modifiers, i -> new Int2ObjectArrayMap<>())
                .put(keyCode, sequence);
    }
}
