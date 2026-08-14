/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.common.serialization.NBTSerialization;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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

    // ------------------------------------------------------------- //

    private static String render(final String value, final int expectedLength) {
        final Terminal terminal = new Terminal();
        for (final byte b : value.getBytes(StandardCharsets.UTF_8)) {
            terminal.putOutput(b);
        }

        return read(terminal, expectedLength);
    }

    private static String read(final Terminal terminal, final int length) {
        // Only used here, so let's just grab it with reflection...
        try {
            final Field field = Terminal.class.getDeclaredField("buffer");
            field.setAccessible(true);
            final byte[] cells = new byte[length];
            System.arraycopy(field.get(terminal), 0, cells, 0, length);
            return new String(cells, StandardCharsets.ISO_8859_1);
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("could not read the terminal's cell buffer", e);
        }
    }
}
