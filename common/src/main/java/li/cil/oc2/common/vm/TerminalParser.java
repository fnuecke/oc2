/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Splits a terminal's byte stream into characters and escape sequences.
 * <p>
 * Shaped after the VT500 parser at <a href="https://vt100.net/emu/dec_ansi_parser">vt100.net</a>,
 * collapsed to the states we need: the four CSI states become one plus an ignore flag, and every
 * string sequence is consumed and discarded. Everything it recognizes goes to a {@link Sink}; the
 * parser itself holds no terminal state.
 * <p>
 * Persisted, so a sequence split across a save resumes rather than being displayed as text.
 */
@Serialized
public final class TerminalParser {
    private static final byte ESC = 0x1B, BEL = 0x07, CAN = 0x18, SUB = 0x1A;
    private static final char DEL = 0x7F;

    private static final char INTERMEDIATE_FIRST = 0x20, INTERMEDIATE_LAST = 0x2F;
    private static final char UNHANDLED_PARAMETER_FIRST = 0x3A, UNHANDLED_PARAMETER_LAST = 0x3F;

    private static final int MAX_PARAMETERS = 8;

    public enum State { // Must be public for serialization.
        NORMAL, // Reading characters normally.
        ESCAPE, // Last character was ESC, figure out what kind next.
        ESCAPE_INTERMEDIATE, // Escape sequence with an intermediate, the next byte ends it.
        CONTROL_SEQUENCE, // Know what sequence we have, now parsing it.
        STRING, // Inside an OSC/DCS/PM/APC string, discarding until it terminates.
    }

    public interface Sink {
        void print(char ch);
        void execute(byte control);
        void escapeDispatch(char intermediate, char finalByte);
        void controlSequenceDispatch(char finalByte, Parameters parameters);
    }

    public interface Parameters {
        int count();
        int get(int index);
        boolean isPrivate();
    }

    // --------------------------------------------------------------------- //

    private State state = State.NORMAL;
    private char intermediate;
    private final int[] arguments = new int[MAX_PARAMETERS];
    private int argumentCount;
    private boolean isIgnored, isPrivate;

    // Persisted so a multi-byte character split across a save resumes after load.
    private final byte[] utf8Pending = new byte[4];
    private int utf8PendingCount;

    private final transient CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
    private final transient ByteBuffer utf8Input = ByteBuffer.allocate(8);
    private final transient CharBuffer utf8Output = CharBuffer.allocate(8);
    private final transient ParameterView parameters = new ParameterView();

    // --------------------------------------------------------------------- //

    public void reset() {
        state = State.NORMAL;
        intermediate = 0;
        argumentCount = 0;
        isIgnored = isPrivate = false;
        utf8PendingCount = 0;
    }

    public void put(final byte value, final Sink sink) {
        if (value == ESC) {
            state = State.ESCAPE;
            return;
        }

        final char ch = (char) (value & 0xFF);
        switch (state) {
            case NORMAL -> {
                if (ch < ' ' || ch == DEL) {
                    sink.execute(value);
                } else {
                    decode(value, sink);
                }
            }
            case ESCAPE -> {
                if (ch == '[') { // Control Sequence Indicator
                    beginControlSequence();
                } else if (ch == ']' || ch == 'P' || ch == '^' || ch == '_') { // OSC, DCS, PM, APC
                    state = State.STRING;
                } else if (ch >= INTERMEDIATE_FIRST && ch <= INTERMEDIATE_LAST) {
                    intermediate = ch;
                    state = State.ESCAPE_INTERMEDIATE;
                } else {
                    state = State.NORMAL;
                    sink.escapeDispatch((char) 0, ch);
                }
            }
            case ESCAPE_INTERMEDIATE -> {
                state = State.NORMAL;
                sink.escapeDispatch(intermediate, ch);
            }
            case CONTROL_SEQUENCE -> {
                if (value == CAN || value == SUB) {
                    state = State.NORMAL; // Both stop the sequence without displaying anything.
                } else if (ch < ' ' || ch == DEL) {
                    sink.execute(value); // Handle controls right away.
                } else if (ch >= '0' && ch <= '9') {
                    accumulate(ch - '0');
                } else if (ch == ';') {
                    nextArgument();
                } else if (ch == '?') {
                    isPrivate = true;
                } else if (ch >= UNHANDLED_PARAMETER_FIRST && ch <= UNHANDLED_PARAMETER_LAST
                    || ch >= INTERMEDIATE_FIRST && ch <= INTERMEDIATE_LAST) {
                    isIgnored = true; // We implement no sequence using these.
                } else {
                    nextArgument();
                    state = State.NORMAL;
                    if (!isIgnored) {
                        sink.controlSequenceDispatch(ch, parameters);
                    }
                }
            }
            case STRING -> {
                if (value == BEL) { // The other terminator, ST, arrives as ESC.
                    state = State.NORMAL;
                }
            }
        }
    }

    // --------------------------------------------------------------------- //

    private void beginControlSequence() {
        Arrays.fill(arguments, 0);
        argumentCount = 0;
        isIgnored = isPrivate = false;
        state = State.CONTROL_SEQUENCE;
    }

    private void accumulate(final int digit) {
        if (argumentCount < arguments.length) {
            if (arguments[argumentCount] < (Integer.MAX_VALUE - digit) / 10) {
                arguments[argumentCount] = arguments[argumentCount] * 10 + digit;
            } else {
                arguments[argumentCount] = Integer.MAX_VALUE;
            }
        }
    }

    private void nextArgument() {
        if (argumentCount < arguments.length) {
            argumentCount++;
        }
    }

    private void decode(final byte value, final Sink sink) {
        utf8Input.clear();
        utf8Input.put(utf8Pending, 0, utf8PendingCount);
        utf8Input.put(value);
        utf8Input.flip();

        utf8Output.clear();
        utf8Decoder.decode(utf8Input, utf8Output, false);

        utf8PendingCount = utf8Input.remaining();
        utf8Input.get(utf8Pending, 0, utf8PendingCount);

        utf8Output.flip();
        while (utf8Output.hasRemaining()) {
            sink.print(utf8Output.get());
        }
    }

    // --------------------------------------------------------------------- //

    private final class ParameterView implements Parameters {
        @Override
        public int count() {
            return argumentCount;
        }

        @Override
        public int get(final int index) {
            return index < arguments.length ? arguments[index] : 0;
        }

        @Override
        public boolean isPrivate() {
            return isPrivate;
        }
    }
}
