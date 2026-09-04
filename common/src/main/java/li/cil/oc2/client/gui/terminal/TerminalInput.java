/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.terminal;

import it.unimi.dsi.fastutil.ints.Int2CharArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

public final class TerminalInput {
    private static final int MODIFIER_MASK = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT;
    private static final Int2ObjectArrayMap<Int2ObjectArrayMap<byte[]>> KEYCODE_SEQUENCES = new Int2ObjectArrayMap<>();
    private static final Int2CharArrayMap CSI_FINAL_BYTES = new Int2CharArrayMap();
    private static final Int2IntArrayMap CSI_CODES = new Int2IntArrayMap();
    private static final byte[] RETURN = {'\r'};
    private static final byte[] RETURN_NEW_LINE = {'\r', '\n'};

    static {
        CSI_FINAL_BYTES.defaultReturnValue('\0');
        CSI_CODES.defaultReturnValue(0);

        addSequence(GLFW.GLFW_KEY_TAB, '\t');
        addSequence(GLFW.GLFW_KEY_BACKSPACE, '\b');
        addSequence(GLFW.GLFW_MOD_ALT, GLFW.GLFW_KEY_BACKSPACE, (byte) '\033', (byte) '\b');

        addSequence(GLFW.GLFW_KEY_ESCAPE, "\33");

        addCsiCode(GLFW.GLFW_KEY_HOME, 1);
        addCsiCode(GLFW.GLFW_KEY_INSERT, 2);
        addCsiCode(GLFW.GLFW_KEY_DELETE, 3);
        addCsiCode(GLFW.GLFW_KEY_END, 4);
        addCsiCode(GLFW.GLFW_KEY_PAGE_UP, 5);
        addCsiCode(GLFW.GLFW_KEY_PAGE_DOWN, 6);

        addCsiCode(GLFW.GLFW_KEY_F1, 11);
        addCsiCode(GLFW.GLFW_KEY_F2, 12);
        addCsiCode(GLFW.GLFW_KEY_F3, 13);
        addCsiCode(GLFW.GLFW_KEY_F4, 14);
        addCsiCode(GLFW.GLFW_KEY_F5, 15);
        addCsiCode(GLFW.GLFW_KEY_F6, 17);
        addCsiCode(GLFW.GLFW_KEY_F7, 18);
        addCsiCode(GLFW.GLFW_KEY_F8, 19);
        addCsiCode(GLFW.GLFW_KEY_F9, 20);
        addCsiCode(GLFW.GLFW_KEY_F10, 21);
        addCsiCode(GLFW.GLFW_KEY_F11, 23);
        addCsiCode(GLFW.GLFW_KEY_F12, 24);

        addCsiFinalByte(GLFW.GLFW_KEY_UP, 'A');
        addCsiFinalByte(GLFW.GLFW_KEY_DOWN, 'B');
        addCsiFinalByte(GLFW.GLFW_KEY_RIGHT, 'C');
        addCsiFinalByte(GLFW.GLFW_KEY_LEFT, 'D');

        for (int i = 'A'; i <= 'Z'; i++) {
            addSequence(
                GLFW.GLFW_MOD_CONTROL,
                GLFW.GLFW_KEY_A + i - 'A',
                (byte) (1 + i - 'A')
            );
            addSequence(
                GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT,
                GLFW.GLFW_KEY_A + i - 'A',
                (byte) (1 + i - 'A')
            );

            addSequence(
                GLFW.GLFW_MOD_ALT,
                GLFW.GLFW_KEY_A + i - 'A',
                (byte) '\033', (byte) ('a' + i - 'A')
            );
            addSequence(
                GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SHIFT,
                GLFW.GLFW_KEY_A + i - 'A',
                (byte) '\033', (byte) i
            );
        }

        addSequence(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_LEFT_BRACKET, (byte) '\033');
        addSequence(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT, GLFW.GLFW_KEY_LEFT_BRACKET, (byte) '\033');
        addSequence(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_BACKSLASH, (byte) '\034');
        addSequence(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT, GLFW.GLFW_KEY_BACKSLASH, (byte) '\034');
        addSequence(GLFW.GLFW_MOD_CONTROL, GLFW.GLFW_KEY_RIGHT_BRACKET, (byte) '\035');
        addSequence(GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT, GLFW.GLFW_KEY_RIGHT_BRACKET, (byte) '\035');
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public static byte[] getSequence(final int keyCode, final int modifiers, final boolean isCursorKeyApplicationMode, final boolean isNewLineMode) {
        final int relevantModifiers = modifiers & MODIFIER_MASK;

        if (keyCode == GLFW.GLFW_KEY_ENTER && relevantModifiers == 0) {
            return isNewLineMode ? RETURN_NEW_LINE : RETURN;
        }

        final Int2ObjectArrayMap<byte[]> map = KEYCODE_SEQUENCES.get(relevantModifiers);
        if (map != null) {
            final byte[] sequence = map.get(keyCode);
            if (sequence != null) {
                return sequence;
            }
        }

        return getCsiSequence(keyCode, relevantModifiers, isCursorKeyApplicationMode);
    }

    // --------------------------------------------------------------------- //

    @Nullable
    private static byte[] getCsiSequence(final int keyCode, final int modifiers, final boolean isCursorKeyApplicationMode) {
        final int parameter = 1
            + ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 1 : 0)
            + ((modifiers & GLFW.GLFW_MOD_ALT) != 0 ? 2 : 0)
            + ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 ? 4 : 0);

        final char finalByte = CSI_FINAL_BYTES.get(keyCode);
        if (finalByte != '\0') {
            if (parameter == 1) {
                return toBytes((isCursorKeyApplicationMode ? "\033O" : "\033[") + finalByte);
            }
            return toBytes("\033[1;" + parameter + finalByte);
        }

        final int code = CSI_CODES.get(keyCode);
        if (code != 0) {
            return toBytes(parameter == 1
                ? "\033[" + code + "~"
                : "\033[" + code + ";" + parameter + "~");
        }

        return null;
    }

    private static void addCsiFinalByte(final int keyCode, final char finalByte) {
        CSI_FINAL_BYTES.put(keyCode, finalByte);
    }

    private static void addCsiCode(final int keyCode, final int code) {
        CSI_CODES.put(keyCode, code);
    }

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
        addSequence(modifiers, keyCode, toBytes(sequence));
    }

    private static void addSequence(final int modifiers, final int keyCode, final byte... sequence) {
        KEYCODE_SEQUENCES
            .computeIfAbsent(modifiers, i -> new Int2ObjectArrayMap<>())
            .put(keyCode, sequence);
    }

    private static byte[] toBytes(final String sequence) {
        final byte[] bytes = new byte[sequence.length()];
        for (int i = 0; i < sequence.length(); i++) {
            bytes[i] = (byte) sequence.charAt(i);
        }
        return bytes;
    }
}
