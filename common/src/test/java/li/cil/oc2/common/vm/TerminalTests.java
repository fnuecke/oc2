/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.common.serialization.NBTSerialization;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TerminalTests {
    @BeforeAll
    public static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void asciiIsUnchanged() {
        assertEquals("hello", render("hello", 5));
    }

    @Test
    public void multiByteCharactersOccupyOneCell() {
        assertEquals("äöüÄÖÜ", render("äöüÄÖÜ", 6));
    }

    @Test
    public void codePointsBeyondTheFontBecomeOneReplacement() {
        assertEquals("a?b", render("a€b", 3));
    }

    @Test
    public void aSequenceMayStraddleTwoWrites() {
        final Terminal terminal = new Terminal();
        final byte[] bytes = "ä".getBytes(StandardCharsets.UTF_8);

        terminal.putOutput(bytes[0]);
        terminal.putOutput(bytes[1]);

        assertEquals("ä", read(terminal, 1));
    }

    @Test
    public void aSequenceSurvivesSaveAndLoad() {
        final byte[] bytes = "ä".getBytes(StandardCharsets.UTF_8);

        final Terminal saved = new Terminal();
        saved.putOutput(bytes[0]);

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        loaded.putOutput(bytes[1]);

        assertEquals("ä", read(loaded, 1));
    }

    @Test
    public void malformedInputDoesNotDesynchronise() {
        final Terminal terminal = new Terminal();
        terminal.putOutput((byte) 0xE4);
        terminal.putOutput((byte) 'x');

        assertEquals("?x", read(terminal, 2));
    }

    @Test
    public void inputIsCappedRatherThanGrowingWithoutBound() {
        final Terminal terminal = new Terminal();
        final int cap = maxInputSize();

        for (int i = 0; i < cap * 2; i++) {
            terminal.putInput((byte) 'x');
        }

        assertEquals(cap, drainInput(terminal), "input past the cap should be dropped, not queued");
    }

    @Test
    public void bulkWriteStopsAtTheCap() {
        final Terminal terminal = new Terminal();
        final int cap = maxInputSize();

        terminal.putInput(ByteBuffer.wrap(new byte[cap * 2]));

        assertEquals(cap, drainInput(terminal));
    }

    @Test
    public void inputCanBeQueuedAgainAfterDraining() {
        final Terminal terminal = new Terminal();
        final int cap = maxInputSize();

        terminal.putInput(ByteBuffer.wrap(new byte[cap * 2]));
        assertEquals(cap, drainInput(terminal));

        terminal.putInput((byte) 'x');
        assertEquals(1, drainInput(terminal));
    }

    @Test
    public void cursorUpAtTheTopRowStaysAtTheTopRow() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[A");

        assertEquals(0, terminal.getCursorY());
    }

    @Test
    public void cursorUpFarPastTheTopRowStaysAtTheTopRow() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[3;1H");

        write(terminal, "\033[5A");

        assertEquals(0, terminal.getCursorY());
    }

    @Test
    public void cursorDownAtTheBottomRowStaysAtTheBottomRow() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[24;1H");

        write(terminal, "\033[B");

        assertEquals(Terminal.HEIGHT - 1, terminal.getCursorY());
    }

    @Test
    public void cursorUpStopsAtTheTopOfTheScrollRegion() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;20r\033[10;1H");

        write(terminal, "\033[20A");

        assertEquals(4, terminal.getCursorY());
    }

    @Test
    public void cursorDownStopsAtTheBottomOfTheScrollRegion() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;20r\033[10;1H");

        write(terminal, "\033[20B");

        assertEquals(19, terminal.getCursorY());
    }

    @Test
    public void cursorMovesWhileAboveTheScrollRegion() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;20r");

        write(terminal, "\033[C\033[D\033[A\033[B");

        assertEquals(0, terminal.getCursorX());
    }

    @Test
    public void eraseToStartOfLineAtTheRightMarginClearsTheWholeLine() {
        final Terminal terminal = new Terminal();
        write(terminal, fill(Terminal.WIDTH));

        write(terminal, "\033[1K");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0));
    }

    @Test
    public void eraseToStartOfLineAtTheRightMarginLeavesTheNextLineAlone() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[2;1HZ\033[1;1H");
        write(terminal, fill(Terminal.WIDTH));

        write(terminal, "\033[1K");

        assertEquals('Z', readLine(terminal, 1).charAt(0), "erase must not run past the end of the line");
    }

    @Test
    public void eraseToStartOfLineAtTheRightMarginOfTheLastLineStaysInBounds() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[24;1H");
        write(terminal, fill(Terminal.WIDTH));

        write(terminal, "\033[1K");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, Terminal.HEIGHT - 1));
    }

    @Test
    public void eraseToStartOfScreenAtTheRightMarginOfTheLastLineStaysInBounds() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[24;1H");
        write(terminal, fill(Terminal.WIDTH));

        write(terminal, "\033[1J");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, Terminal.HEIGHT - 1));
    }

    @Test
    public void eraseToEndOfLineAtTheRightMarginClearsTheLastColumn() {
        final Terminal terminal = new Terminal();
        write(terminal, fill(Terminal.WIDTH));

        write(terminal, "\033[K");

        assertEquals(' ', readLine(terminal, 0).charAt(Terminal.WIDTH - 1));
    }

    // --------------------------------------------------------------------- //

    private static int drainInput(final Terminal terminal) {
        int count = 0;
        while (terminal.readInput() != -1) {
            count++;
        }
        return count;
    }

    private static int maxInputSize() {
        // Only used here, so let's just grab it with reflection...
        try {
            final Field field = Terminal.class.getDeclaredField("MAX_INPUT_SIZE");
            field.setAccessible(true);
            return field.getInt(null);
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("could not read the terminal's input cap", e);
        }
    }

    // --------------------------------------------------------------------- //

    private static void write(final Terminal terminal, final String value) {
        for (final byte b : value.getBytes(StandardCharsets.UTF_8)) {
            terminal.putOutput(b);
        }
    }

    private static String render(final String value, final int expectedLength) {
        final Terminal terminal = new Terminal();
        write(terminal, value);

        return read(terminal, expectedLength);
    }

    private static String fill(final int length) {
        return "#".repeat(length);
    }

    private static String readLine(final Terminal terminal, final int y) {
        return read(terminal, y * Terminal.WIDTH, Terminal.WIDTH);
    }

    private static String read(final Terminal terminal, final int length) {
        return read(terminal, 0, length);
    }

    private static String read(final Terminal terminal, final int offset, final int length) {
        // Only used here, so let's just grab it with reflection...
        try {
            final Field field = Terminal.class.getDeclaredField("buffer");
            field.setAccessible(true);
            final byte[] cells = new byte[length];
            System.arraycopy(field.get(terminal), offset, cells, 0, length);
            return new String(cells, StandardCharsets.ISO_8859_1);
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("could not read the terminal's cell buffer", e);
        }
    }
}
