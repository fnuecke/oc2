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
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TerminalTests {
    private static final int COLOR_BLACK = 0;
    private static final int COLOR_RED = 1;
    private static final int COLOR_BLUE = 4;

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
        assertEquals(1, terminal.getCursorY(), "only the vertical moves should have moved the row");
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

    @Test
    public void reverseIndexKeepsTheColumn() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;40H");

        write(terminal, "\033M");

        assertEquals(39, terminal.getCursorX());
        assertEquals(3, terminal.getCursorY());
    }

    @Test
    public void reverseIndexAtTheTopOfTheScrollRegionScrolls() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[1;1HTOP");

        write(terminal, "\033[1;1H\033M");

        assertEquals("TOP", read(terminal, Terminal.WIDTH, 3), "the top line should have moved down one");
    }

    @Test
    public void horizontalMovementDoesNotChangeTheRow() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;20r");

        write(terminal, "\033[C");

        assertEquals(0, terminal.getCursorY(), "moving right must not drag the cursor into the scroll region");
    }

    @Test
    public void linesWrapByDefault() {
        final Terminal terminal = new Terminal();

        write(terminal, fill(Terminal.WIDTH + 5));

        assertEquals(fill(5) + " ".repeat(Terminal.WIDTH - 5), readLine(terminal, 1));
    }

    @Test
    public void wrappingCanBeTurnedOff() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?7l");

        write(terminal, fill(Terminal.WIDTH + 5));

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 1));
    }

    @Test
    public void tabStopsAreEightColumnsApart() {
        final Terminal terminal = new Terminal();

        write(terminal, "A\tB");

        assertEquals("A" + " ".repeat(7) + "B", read(terminal, 9));
    }

    @Test
    public void tabStopsAreEightColumnsApartAfterReset() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033cA\tB");

        assertEquals("A" + " ".repeat(7) + "B", read(terminal, 9));
    }

    @Test
    public void paletteColorsDoNotLeakIntoStyles() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[38;5;196mX");

        assertEquals(COLOR_RED, Terminal.getForegroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void paletteBackgroundColorsAreApplied() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[48;5;21mX");

        assertEquals(COLOR_BLUE, Terminal.getBackgroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void trueColorIsMappedOntoThePalette() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[38;2;255;0;0mX");

        assertEquals(COLOR_RED, Terminal.getForegroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void anExtendedColorTruncatedByTheArgumentLimitIsIgnored() {
        final Terminal terminal = new Terminal();

        // More parameters than we keep, so the trailing color index is never seen.
        write(terminal, "\033[1;1;1;1;1;1;38;5;196mX");

        final int cell = terminal.getCell(0);
        assertEquals(Terminal.COLOR_WHITE, Terminal.getForegroundColorIndex(cell), "a partial color run must not paint");
        assertTrue(Terminal.isBold(cell), "arguments before the color run still apply");
    }

    @Test
    public void extendedColorsStillWorkAfterSaveAndLoad() {
        final Terminal saved = new Terminal();

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        write(loaded, "\033[38;2;255;0;0mX");

        assertEquals(COLOR_RED, Terminal.getForegroundColorIndex(loaded.getCell(0)));
    }

    @Test
    public void resettingOriginModeDoesNotClearTheScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "hello");

        write(terminal, "\033[?6l");

        assertEquals("hello", read(terminal, 5));
    }

    @Test
    public void switchingColumnModeClearsTheScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[10;10Hhello");

        write(terminal, "\033[?3h");

        assertEquals(" ".repeat(5), read(terminal, 9 * Terminal.WIDTH + 9, 5));
        assertEquals(0, terminal.getCursorX());
        assertEquals(0, terminal.getCursorY());
    }

    @Test
    public void switchingBackToEightyColumnsAlsoClearsTheScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "hello");

        write(terminal, "\033[?3l");

        assertEquals(" ".repeat(5), read(terminal, 5));
    }

    @Test
    public void switchingColumnModeRestoresTheFullScrollRegion() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;10r\033[?3h");

        write(terminal, "TOP\033[24;1H\n");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0), "the whole screen should scroll again");
    }

    @Test
    public void underlineReachesTheCell() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[4mu\033[24mv");

        assertTrue(Terminal.isUnderline(terminal.getCell(0)));
        assertFalse(Terminal.isUnderline(terminal.getCell(1)));
    }

    @Test
    public void newLineModeFollowsTheMode() {
        final Terminal terminal = new Terminal();
        assertFalse(terminal.isNewLineMode());

        write(terminal, "\033[20h");
        assertTrue(terminal.isNewLineMode());

        write(terminal, "\033[20l");
        assertFalse(terminal.isNewLineMode());
    }

    @Test
    public void resetRestoresTheFullScrollRegion() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;10r\033c");

        write(terminal, "TOP\033[24;1H\n");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0), "the whole screen should scroll again");
    }

    @Test
    public void resetRestoresTheCursorAndModes() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[10;10H\033[?7l");

        write(terminal, "\033c");

        assertEquals(0, terminal.getCursorX());
        assertEquals(0, terminal.getCursorY());

        write(terminal, fill(Terminal.WIDTH + 5));
        assertEquals(fill(5) + " ".repeat(Terminal.WIDTH - 5), readLine(terminal, 1), "wrapping should be back on");
    }

    @Test
    public void erasingKeepsTheCurrentBackground() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[44m\033[2K");

        assertEquals(COLOR_BLUE, Terminal.getBackgroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void scrollingExposesLinesWithTheCurrentBackground() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[44m\033[24;1H\n");

        final int cell = terminal.getCell((Terminal.HEIGHT - 1) * Terminal.WIDTH);
        assertEquals(COLOR_BLUE, Terminal.getBackgroundColorIndex(cell));
    }

    @Test
    public void savedCursorRestoresAttributes() {
        final Terminal terminal = new Terminal();

        write(terminal, "\0337\033[31m\0338X");

        assertEquals(Terminal.COLOR_WHITE, Terminal.getForegroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void indexBelowTheScrollRegionMovesDownInsteadOfScrolling() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[1;11r\033[21;1H");

        write(terminal, "\n");

        assertEquals(21, terminal.getCursorY());
    }

    @Test
    public void scrollRegionWithAnOmittedFirstLineCoversTheScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;10r\033[;24r");

        write(terminal, "TOP\033[24;1H\n");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0));
    }

    @Test
    public void cursorPositionReportStaysOnScreenAtTheRightMargin() {
        final Terminal terminal = new Terminal();
        write(terminal, fill(Terminal.WIDTH));

        write(terminal, "\033[6n");

        assertEquals("\033[1;80R", readResponse(terminal));
    }

    @Test
    public void windowTitlesAreNotPrintedToTheScreen() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033]0;some title\007X");

        assertEquals("X", read(terminal, 1));
    }

    @Test
    public void windowTitlesDoNotRingTheBell() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033]0;some title\007");

        assertFalse(terminal.consumePendingBell());
    }

    @Test
    public void stringTerminatorEndsAString() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033]0;some title\033\\X");

        assertEquals("X", read(terminal, 1));
    }

    @Test
    public void deviceControlStringsAreNotPrintedToTheScreen() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033P1;2|junk\033\\X");

        assertEquals("X", read(terminal, 1));
    }

    @Test
    public void anUnterminatedStringIsRecoveredByReset() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033]0;never terminated");

        write(terminal, "\033cX");

        assertEquals("X", read(terminal, 1));
    }

    @Test
    public void cursorStyleIsNotPrintedToTheScreen() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[2 qX");

        assertEquals("X", read(terminal, 1));
    }

    @Test
    public void softResetIsNotPrintedToTheScreen() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[!pX");

        assertEquals("X", read(terminal, 1));
    }

    @Test
    public void privateControlSequencesAreNotPrintedToTheScreen() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[>4;2mX");

        assertEquals("X", read(terminal, 1));
    }

    @Test
    public void privateControlSequencesDoNotRunTheirPublicCounterpart() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[>31mX");

        assertEquals(Terminal.COLOR_WHITE, Terminal.getForegroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void decPrivateModesStillApply() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[?7l");

        write(terminal, fill(Terminal.WIDTH + 5));
        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 1), "ESC[?7l must still turn wrapping off");
    }

    @Test
    public void defaultForegroundColorIsRestored() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[31mA\033[39mB");

        assertEquals(Terminal.COLOR_WHITE, Terminal.getForegroundColorIndex(terminal.getCell(1)));
    }

    @Test
    public void defaultBackgroundColorIsRestored() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[44mA\033[49mB");

        assertEquals(COLOR_BLACK, Terminal.getBackgroundColorIndex(terminal.getCell(1)));
    }

    @Test
    public void brightForegroundColorsAreApplied() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[91mA");

        assertEquals(COLOR_RED, Terminal.getForegroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void brightBackgroundColorsAreApplied() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[104mA");

        assertEquals(COLOR_BLUE, Terminal.getBackgroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void lineDrawingIsOffByDefault() {
        final Terminal terminal = new Terminal();

        write(terminal, "lqk");

        assertEquals("lqk", read(terminal, 3));
    }

    @Test
    public void shiftOutSelectsTheLineDrawingSet() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033)0\016lqk");

        assertEquals('l' - '_', cellCharacter(terminal, 0));
        assertEquals('q' - '_', cellCharacter(terminal, 1));
        assertEquals('k' - '_', cellCharacter(terminal, 2));
    }

    @Test
    public void shiftInReturnsToText() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033)0\016l\017l");

        assertEquals('l' - '_', cellCharacter(terminal, 0));
        assertEquals('l', cellCharacter(terminal, 1));
    }

    @Test
    public void theLineDrawingSetCanBeMappedIntoG0() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033(0lqk");

        assertEquals('l' - '_', cellCharacter(terminal, 0));
    }

    @Test
    public void charactersOutsideTheLineDrawingRangeAreUnchanged() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033(0AZ");

        assertEquals('A', cellCharacter(terminal, 0));
        assertEquals('Z', cellCharacter(terminal, 1));
    }

    @Test
    public void resetReturnsToTheTextCharacterSet() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033)0\016");

        write(terminal, "\033clqk");

        assertEquals("lqk", read(terminal, 3));
    }

    @Test
    public void cursorKeyApplicationModeFollowsTheMode() {
        final Terminal terminal = new Terminal();
        assertFalse(terminal.isCursorKeyApplicationMode());

        write(terminal, "\033[?1h");
        assertTrue(terminal.isCursorKeyApplicationMode());

        write(terminal, "\033[?1l");
        assertFalse(terminal.isCursorKeyApplicationMode());
    }

    @Test
    public void escapeAbandonsAnUnfinishedControlSequence() {
        final Terminal terminal = new Terminal();
        write(terminal, "ABC\033[1;");

        write(terminal, "\033[2JX");

        assertEquals("   X" + " ".repeat(Terminal.WIDTH - 4), readLine(terminal, 0));
    }

    @Test
    public void escapeAbandonsAnUnfinishedCharacterSetSelection() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033(");

        write(terminal, "\033[2JX");

        assertEquals("X" + " ".repeat(Terminal.WIDTH - 1), readLine(terminal, 0));
    }

    @Test
    public void escapeAbandonsAnUnfinishedHashSequence() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033#");

        write(terminal, "\033[2JX");

        assertEquals("X" + " ".repeat(Terminal.WIDTH - 1), readLine(terminal, 0));
    }

    @Test
    public void aSaturatedCursorForwardStopsAtTheLastColumn() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[1;2H"); // Not column 0, or the addition would not overflow.

        write(terminal, "\033[2147483647C");

        assertEquals(Terminal.WIDTH - 1, terminal.getCursorX());
    }

    @Test
    public void aSaturatedCursorDownStopsAtTheLastRow() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[2;1H"); // Not row 0, or the addition would not overflow.

        write(terminal, "\033[2147483647B");

        assertEquals(Terminal.HEIGHT - 1, terminal.getCursorY());
    }

    @Test
    public void aSaturatedCursorPositionStopsAtTheLastRow() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[2147483647;2147483647H");

        assertEquals(Terminal.WIDTH - 1, terminal.getCursorX());
        assertEquals(Terminal.HEIGHT - 1, terminal.getCursorY());
    }

    @Test
    public void aSaturatedCursorPositionStopsAtTheBottomMarginInOriginMode() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;20r\033[?6h");

        write(terminal, "\033[2147483647;1H");

        assertEquals(19, terminal.getCursorY());
    }

    @Test
    public void subParameterSequencesAreNotPrintedToTheScreen() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[38:2::255:0:0mX");

        assertEquals("X", read(terminal, 1));
        assertEquals(Terminal.COLOR_WHITE, Terminal.getForegroundColorIndex(terminal.getCell(0)),
            "we do not implement the colon form, so it must not paint a guessed color");
    }

    @Test
    public void controlCharactersInsideASequenceStillExecute() {
        final Terminal terminal = new Terminal();
        write(terminal, "ABC");

        write(terminal, "\033[1\rZ");

        assertEquals("ABC", read(terminal, 3), "the sequence must not leak its final byte");
        assertEquals(0, terminal.getCursorX(), "the carriage return must still have moved the cursor");
    }

    @Test
    public void deleteInsideASequenceIsIgnored() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;1H");

        write(terminal, "\033[1\177A");

        assertEquals(3, terminal.getCursorY(), "the sequence should still be a cursor up");
    }

    @Test
    public void cancelAbandonsASequence() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[1\030A");

        assertEquals(0, terminal.getCursorY());
        assertEquals("A" + " ".repeat(Terminal.WIDTH - 1), readLine(terminal, 0));
    }

    @Test
    public void cursorPositionReportsUseLatinDigits() {
        final Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("ar-EG-u-nu-arab"));

            final Terminal terminal = new Terminal();
            write(terminal, "\033[12;34H\033[6n");

            assertEquals("\033[12;34R", readResponse(terminal));
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }
    }

    @Test
    public void originModeHomesToTheTopMargin() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;20r\033[?6h");

        write(terminal, "\033[H");

        assertEquals(4, terminal.getCursorY());
    }

    @Test
    public void ansiModesDoNotReachDecPrivateModes() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;20r");

        write(terminal, "\033[6h\033[H"); // ANSI mode 6, not DECOM.

        assertEquals(0, terminal.getCursorY(), "setting an ANSI mode must not turn on origin mode");
    }

    @Test
    public void resettingAnAnsiModeLeavesTheDecPrivateModeAlone() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[7l"); // ANSI mode 7, not DECAWM.

        write(terminal, fill(Terminal.WIDTH + 5));
        assertEquals(fill(5) + " ".repeat(Terminal.WIDTH - 5), readLine(terminal, 1),
            "wrapping is a DEC private mode and must be untouched");
    }

    @Test
    public void highModeNumbersDoNotAliasOntoLowOnes() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[?2004h"); // Bracketed paste; a plain shift by 2004 would land on bit 20.

        write(terminal, "abc\n");
        assertEquals(3, terminal.getCursorX(), "a line feed must not have turned into a new line");
    }

    @Test
    public void ansiNewLineModeStillApplies() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[20h");

        write(terminal, "abc\n");
        assertEquals(0, terminal.getCursorX());
    }

    @Test
    public void theCursorColumnNeverLeavesTheScreen() {
        final Terminal terminal = new Terminal();

        write(terminal, fill(Terminal.WIDTH));

        assertEquals(Terminal.WIDTH - 1, terminal.getCursorX());
    }

    @Test
    public void aTabPastTheLastStopMovesToTheRightMargin() {
        final Terminal terminal = new Terminal();
        write(terminal, fill(72)); // The last tab stop.

        write(terminal, "\tX");

        assertEquals('X', readLine(terminal, 0).charAt(Terminal.WIDTH - 1));
        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 1), "a tab must not wrap the line");
    }

    @Test
    public void aPendingWrapSurvivesSaveAndLoad() {
        final Terminal saved = new Terminal();
        write(saved, fill(Terminal.WIDTH));

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        write(loaded, "X");
        assertEquals('X', readLine(loaded, 1).charAt(0), "the deferred wrap should still happen");
    }

    @Test
    public void aControlSequenceSurvivesSaveAndLoad() {
        final Terminal saved = new Terminal();
        write(saved, "\033[2");

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        write(loaded, ";3HX");

        assertEquals('X', readLine(loaded, 1).charAt(2), "the split sequence should still be a CUP");
    }

    @Test
    public void theSavedCursorRestoresTheCharacterSet() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033(0\0337\033(B\0338l");

        assertEquals('l' - '_', cellCharacter(terminal, 0));
    }

    @Test
    public void theSavedCursorRestoresOriginMode() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;20r\033[?6h");

        write(terminal, "\0337\033[?6l\0338\033[H");

        assertEquals(4, terminal.getCursorY());
    }

    @Test
    public void theSavedCursorRestoresAPendingWrap() {
        final Terminal terminal = new Terminal();
        write(terminal, fill(Terminal.WIDTH));

        write(terminal, "\0337\033[1;1H\0338X");

        assertEquals('X', readLine(terminal, 1).charAt(0), "the deferred wrap should have been restored");
    }

    @Test
    public void theSavedCursorSurvivesSaveAndLoad() {
        final Terminal saved = new Terminal();
        write(saved, "\033[6;7H\0337\033[1;1H");

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        write(loaded, "\0338");
        assertEquals(6, loaded.getCursorX());
        assertEquals(5, loaded.getCursorY());
    }

    @Test
    public void aScrollRegionPastTheLastRowIsClamped() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;10r");

        write(terminal, "\033[1;30rTOP\033[24;1H\n");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0), "the whole screen should scroll");
    }

    @Test
    public void aDegenerateScrollRegionCoversTheScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[5;10r");

        write(terminal, "\033[10;5rTOP\033[24;1H\n");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0), "the whole screen should scroll");
    }

    @Test
    public void insertedCharactersShiftTheRestOfTheLineRight() {
        final Terminal terminal = new Terminal();

        write(terminal, "abcdef\033[1;3H\033[2@");

        assertEquals("ab  cdef", read(terminal, 0, 8));
    }

    @Test
    public void deletedCharactersPullTheRestOfTheLineLeft() {
        final Terminal terminal = new Terminal();

        write(terminal, "abcdef\033[1;3H\033[2P");

        assertEquals("abef  ", read(terminal, 0, 6));
    }

    @Test
    public void erasedCharactersDoNotShiftTheLine() {
        final Terminal terminal = new Terminal();

        write(terminal, "abcdef\033[1;3H\033[2X");

        assertEquals("ab  ef", read(terminal, 0, 6));
    }

    @Test
    public void deletingMoreCharactersThanTheLineHoldsClearsIt() {
        final Terminal terminal = new Terminal();

        write(terminal, "abc\033[1;1H\033[99P");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0));
    }

    @Test
    public void editingAtTheRightEdgeStaysInBounds() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[1;80HX\033[99@\033[99P\033[99X");

        assertEquals(79, terminal.getCursorX());
        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0));
    }

    @Test
    public void insertedLinesShiftTheOnesBelowDown() {
        final Terminal terminal = new Terminal();

        write(terminal, "one\r\ntwo\033[1;1H\033[L");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0));
        assertEquals("one", read(terminal, Terminal.WIDTH, 3));
        assertEquals("two", read(terminal, 2 * Terminal.WIDTH, 3), "the lines must move, not be overwritten");
    }

    @Test
    public void deletedLinesPullTheOnesBelowUp() {
        final Terminal terminal = new Terminal();

        write(terminal, "one\r\ntwo\033[1;1H\033[M");

        assertEquals("two", read(terminal, 0, 3));
        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 1), "the vacated line must be cleared");
    }

    @Test
    public void insertingALineMovesTheCursorToTheLineHome() {
        final Terminal terminal = new Terminal();

        write(terminal, "abc\033[L");

        assertEquals(0, terminal.getCursorX());
    }

    @Test
    public void deletingMoreLinesThanTheRegionHoldsClearsIt() {
        final Terminal terminal = new Terminal();

        write(terminal, "one\r\ntwo\r\nthree\033[1;2r\033[99M");

        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0));
        assertEquals("three", read(terminal, 2 * Terminal.WIDTH, 5),
            "lines outside the scroll region must be untouched");
    }

    @Test
    public void insertedLinesStopAtTheScrollRegionBottom() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[4;1Hfour\033[1;3r\033[1;1H\033[L");

        assertEquals("four", read(terminal, 3 * Terminal.WIDTH, 4),
            "lines below the region must not be pushed down");
    }

    @Test
    public void insertingLinesOutsideTheScrollRegionDoesNothing() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[1;3r");
        write(terminal, "\033[10;3Habc");

        write(terminal, "\033[10;3H\033[L");

        assertEquals("abc", read(terminal, 9 * Terminal.WIDTH + 2, 3));
        assertEquals(2, terminal.getCursorX(), "a no-op must not move the cursor to the line home");
    }

    @Test
    public void scrollUpMovesTheRegionWithoutMovingTheCursor() {
        final Terminal terminal = new Terminal();

        write(terminal, "one\r\ntwo\033[S");

        assertEquals("two", read(terminal, 0, 3));
        assertEquals(3, terminal.getCursorX());
        assertEquals(1, terminal.getCursorY());
    }

    @Test
    public void scrollDownMovesTheRegion() {
        final Terminal terminal = new Terminal();

        write(terminal, "one\033[T");

        assertEquals("one", read(terminal, Terminal.WIDTH, 3));
    }

    @Test
    public void scrollUpStaysInsideTheScrollRegion() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[4;1Hfour\033[1;3r\033[S");

        assertEquals("four", read(terminal, 3 * Terminal.WIDTH, 4));
    }

    @Test
    public void cursorCharacterAbsoluteSelectsTheColumn() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[10G");

        assertEquals(9, terminal.getCursorX());
    }

    @Test
    public void linePositionAbsoluteSelectsTheRow() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[10d");

        assertEquals(9, terminal.getCursorY());
    }

    @Test
    public void absolutePositioningDefaultsToTheFirstColumnAndRow() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[10;10H");

        write(terminal, "\033[G");
        assertEquals(9, terminal.getCursorY());
        assertEquals(0, terminal.getCursorX());

        write(terminal, "\033[d");
        assertEquals(0, terminal.getCursorY());
    }

    @Test
    public void omittedEditingParametersDefaultToOne() {
        final Terminal terminal = new Terminal();

        write(terminal, "abcdef\033[1;3H\033[@");

        assertEquals("ab cde", read(terminal, 0, 6));
    }

    @Test
    public void zeroEditingParametersDefaultToOne() {
        final Terminal terminal = new Terminal();

        write(terminal, "abcdef\033[1;3H\033[0P");

        assertEquals("abdef", read(terminal, 0, 5));
    }

    @Test
    public void theCursorIsVisibleByDefault() {
        final Terminal terminal = new Terminal();

        assertTrue(terminal.isCursorVisible());
    }

    @Test
    public void theCursorCanBeHiddenAndShown() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[?25l");
        assertFalse(terminal.isCursorVisible());

        write(terminal, "\033[?25h");
        assertTrue(terminal.isCursorVisible());
    }

    @Test
    public void aResetMakesTheCursorVisibleAgain() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?25l");

        write(terminal, "\033c");

        assertTrue(terminal.isCursorVisible());
    }

    @Test
    public void cursorVisibilitySurvivesSaveAndLoad() {
        final Terminal saved = new Terminal();
        write(saved, "\033[?25l");

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        assertFalse(loaded.isCursorVisible());
    }

    @Test
    public void bracketedPasteIsOffByDefault() {
        final Terminal terminal = new Terminal();

        assertFalse(terminal.isBracketedPasteMode());
    }

    @Test
    public void bracketedPasteCanBeToggled() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[?2004h");
        assertTrue(terminal.isBracketedPasteMode());

        write(terminal, "\033[?2004l");
        assertFalse(terminal.isBracketedPasteMode());
    }

    @Test
    public void bracketedPasteSurvivesSaveAndLoad() {
        final Terminal saved = new Terminal();
        write(saved, "\033[?2004h");

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        assertTrue(loaded.isBracketedPasteMode());
    }

    @Test
    public void aResetTurnsBracketedPasteOff() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?2004h");

        write(terminal, "\033c");

        assertFalse(terminal.isBracketedPasteMode());
    }

    @Test
    public void unimplementedHighModesLeaveLowOnesAlone() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[?39l"); // A plain shift by 39 would land on DECAWM and turn wrapping off.

        write(terminal, fill(Terminal.WIDTH + 5));

        assertEquals(fill(5) + " ".repeat(Terminal.WIDTH - 5), readLine(terminal, 1));
    }

    @Test
    public void aPasteIsBracketedWhenTheGuestAsksForIt() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?2004h");

        terminal.putPaste("hi");

        assertEquals("\033[200~hi\033[201~", readResponse(terminal));
    }

    @Test
    public void aPasteIsPlainWhenTheGuestDidNotAskForBracketing() {
        final Terminal terminal = new Terminal();

        terminal.putPaste("hi");

        assertEquals("hi", readResponse(terminal));
    }

    @Test
    public void aPasteCannotCloseItsOwnBracket() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?2004h");

        terminal.putPaste("a\033[201~evil");

        assertEquals("\033[200~a[201~evil\033[201~", readResponse(terminal));
    }

    @Test
    public void anOversizedPasteStillGetsItsTerminator() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?2004h");

        terminal.putPaste("x".repeat(maxInputSize() * 2));

        final String input = readResponse(terminal);
        assertTrue(input.startsWith("\033[200~"));
        assertTrue(input.endsWith("\033[201~"),
            "a truncated paste must not strand the guest in paste mode");
    }

    @Test
    public void brightForegroundColorsAreFlagged() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[91mA\033[31mB");

        assertTrue(Terminal.isForegroundBright(terminal.getCell(0)));
        assertFalse(Terminal.isForegroundBright(terminal.getCell(1)));
    }

    @Test
    public void brightBackgroundColorsAreFlagged() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[104mA\033[44mB");

        assertTrue(Terminal.isBackgroundBright(terminal.getCell(0)));
        assertFalse(Terminal.isBackgroundBright(terminal.getCell(1)));
    }

    @Test
    public void brightnessFollowsInversion() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[90;7mA");

        assertTrue(Terminal.isBackgroundBright(terminal.getCell(0)),
            "the bright foreground becomes the background when inverted");
        assertFalse(Terminal.isForegroundBright(terminal.getCell(0)));
    }

    @Test
    public void brightBlackIsNotPlainBlack() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[90mA");

        assertEquals(COLOR_BLACK, Terminal.getForegroundColorIndex(terminal.getCell(0)));
        assertTrue(Terminal.isForegroundBright(terminal.getCell(0)),
            "dimmed UI text must stay distinguishable from the background");
    }

    @Test
    public void defaultColorsAreNotBright() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[91;104mA\033[39;49mB");

        assertFalse(Terminal.isForegroundBright(terminal.getCell(1)));
        assertFalse(Terminal.isBackgroundBright(terminal.getCell(1)));
    }

    @Test
    public void aBrightBackgroundIsKeptWhenErasing() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[104m\033[2J");

        assertTrue(Terminal.isBackgroundBright(terminal.getCell(0)));
        assertEquals(COLOR_BLUE, Terminal.getBackgroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void brightPaletteColorsAreFlagged() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[38;5;9mA\033[38;5;1mB");

        assertTrue(Terminal.isForegroundBright(terminal.getCell(0)));
        assertFalse(Terminal.isForegroundBright(terminal.getCell(1)));
    }

    @Test
    public void aBrightForegroundSurvivesSaveAndLoad() {
        final Terminal saved = new Terminal();
        write(saved, "\033[91mA");

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        assertTrue(Terminal.isForegroundBright(loaded.getCell(0)), "the bright bit is the color byte's sign bit");
        assertEquals(COLOR_RED, Terminal.getForegroundColorIndex(loaded.getCell(0)));
    }

    @Test
    public void theAltBufferIsInactiveByDefault() {
        final Terminal terminal = new Terminal();

        assertFalse(terminal.isAltBufferActive());
    }

    @Test
    public void enteringTheAltBufferClearsTheScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "shell");

        write(terminal, "\033[?1049h");

        assertTrue(terminal.isAltBufferActive());
        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0));
    }

    @Test
    public void leavingTheAltBufferRestoresTheScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "shell");

        write(terminal, "\033[?1049h");
        write(terminal, "editor");
        write(terminal, "\033[?1049l");

        assertFalse(terminal.isAltBufferActive());
        assertEquals("shell", read(terminal, 0, 5));
    }

    @Test
    public void theLegacyAltBufferModeAlsoRestoresTheScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "shell");

        write(terminal, "\033[?47h");
        assertTrue(terminal.isAltBufferActive());
        assertEquals(" ".repeat(Terminal.WIDTH), readLine(terminal, 0));

        write(terminal, "editor");
        write(terminal, "\033[?47l");

        assertEquals("shell", read(terminal, 0, 5));
    }

    @Test
    public void theAltBufferRestoresColorsAndStyles() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[91;44mX\033[0m");

        write(terminal, "\033[?1049h\033[?1049l");

        assertTrue(Terminal.isForegroundBright(terminal.getCell(0)));
        assertEquals(COLOR_BLUE, Terminal.getBackgroundColorIndex(terminal.getCell(0)));
    }

    @Test
    public void theAltBufferWithCursorRestoresTheCursor() {
        final Terminal terminal = new Terminal();
        write(terminal, "abc");

        write(terminal, "\033[?1049h\033[10;10H\033[?1049l");

        assertEquals(3, terminal.getCursorX());
        assertEquals(0, terminal.getCursorY());
    }

    @Test
    public void theLegacyAltBufferModeLeavesTheCursorAlone() {
        final Terminal terminal = new Terminal();
        write(terminal, "abc");

        write(terminal, "\033[?47h\033[10;10H\033[?47l");

        assertEquals(9, terminal.getCursorX());
        assertEquals(9, terminal.getCursorY());
    }

    @Test
    public void overlappingAltBufferModesKeepOneSavedScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "shell");

        write(terminal, "\033[?1049h");
        write(terminal, "\033[?1047h");
        write(terminal, "\033[?1047l");
        assertTrue(terminal.isAltBufferActive(), "1049 is still set, so the alt screen must stay up");

        write(terminal, "\033[?1049l");

        assertEquals("shell", read(terminal, 0, 5));
    }

    @Test
    public void aResetLeavesTheAltBuffer() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?1049h");

        write(terminal, "\033c");

        assertFalse(terminal.isAltBufferActive());
    }

    @Test
    public void theAltBufferSurvivesSaveAndLoad() {
        final Terminal saved = new Terminal();
        write(saved, "shell\033[?1049heditor");

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        assertTrue(loaded.isAltBufferActive());
        write(loaded, "\033[?1049l");
        assertEquals("shell", read(loaded, 0, 5));
    }

    @Test
    public void saveCursorModeRestoresTheCursorWithoutSwitchingScreens() {
        final Terminal terminal = new Terminal();
        write(terminal, "abc");

        write(terminal, "\033[?1048h\033[10;10H\033[?1048l");

        assertFalse(terminal.isAltBufferActive());
        assertEquals(3, terminal.getCursorX());
    }

    @Test
    public void theClearingAltBufferModeAlsoRestoresTheScreen() {
        final Terminal terminal = new Terminal();
        write(terminal, "shell");

        write(terminal, "\033[?1047h");
        assertTrue(terminal.isAltBufferActive());
        write(terminal, "editor");
        write(terminal, "\033[?1047l");

        assertEquals("shell", read(terminal, 0, 5));
    }

    @Test
    public void theCursorIsRestoredByWhicheverModeLeavesTheAltBuffer() {
        final Terminal terminal = new Terminal();
        write(terminal, "abc");

        write(terminal, "\033[?1049h\033[?1047h\033[10;10H");
        write(terminal, "\033[?1049l\033[?1047l");

        assertEquals(3, terminal.getCursorX(), "1049 saved the cursor, so leaving must restore it");
        assertEquals(0, terminal.getCursorY());
    }

    @Test
    public void aCursorIsOnlyRestoredIfTheAltBufferSavedOne() {
        final Terminal terminal = new Terminal();
        write(terminal, "abc\0337\033[20;40Hxyz");

        write(terminal, "\033[?1047h\033[?1049h\033[5;5H");
        write(terminal, "\033[?1047l\033[?1049l");

        assertEquals(4, terminal.getCursorX(), "1047 saved no cursor, so the guest's own save must be left alone");
        assertEquals(4, terminal.getCursorY());
    }

    @Test
    public void savingTheCursorInsideTheAltBufferDoesNotDisturbTheRestore() {
        final Terminal terminal = new Terminal();
        write(terminal, "prompt$ ");

        write(terminal, "\033[?1049h\033[12;40H\0337");
        write(terminal, "\033[?1049l");

        assertEquals(8, terminal.getCursorX(), "the guest's own save must not clobber the alt buffer's");
        assertEquals(0, terminal.getCursorY());
    }

    @Test
    public void theAltBufferContentsSurviveSaveAndLoad() {
        final Terminal saved = new Terminal();
        write(saved, "shell\033[?1049h\033[1;1Heditor");

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        assertEquals("editor", read(loaded, 0, 6), "the alt screen itself must survive too");
        write(loaded, "\033[?1049l");
        assertEquals("shell", read(loaded, 0, 5));
    }

    @Test
    public void mouseReportingIsOffByDefault() {
        final Terminal terminal = new Terminal();

        assertFalse(terminal.isMouseReportingEnabled());
    }

    @Test
    public void mouseReportingCanBeToggled() {
        final Terminal terminal = new Terminal();

        write(terminal, "\033[?1000h");
        assertTrue(terminal.isMouseReportingEnabled());

        write(terminal, "\033[?1000l");
        assertFalse(terminal.isMouseReportingEnabled());
    }

    @Test
    public void mouseEventsUseTheLegacyEncodingByDefault() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?1000h");

        terminal.putMouseEvent(0, 0, 0, true);

        assertEquals("\033[M !!", readResponse(terminal), "the legacy encoding offsets button and position by 32");
    }

    @Test
    public void theLegacyEncodingReportsOneCodeForEveryRelease() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?1000h");

        terminal.putMouseEvent(2, 0, 0, false);

        assertEquals("\033[M#!!", readResponse(terminal), "the legacy encoding cannot say which button came up");
    }

    @Test
    public void theSgrEncodingKeepsPressAndReleaseApart() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?1000h\033[?1006h");

        terminal.putMouseEvent(2, 9, 4, true);
        assertEquals("\033[<2;10;5M", readResponse(terminal));

        terminal.putMouseEvent(2, 9, 4, false);
        assertEquals("\033[<2;10;5m", readResponse(terminal));
    }

    @Test
    public void wheelEventsAreReportedAsHighButtonNumbers() {
        final Terminal terminal = new Terminal();
        write(terminal, "\033[?1000h\033[?1006h");

        terminal.putMouseEvent(64, 0, 0, true);

        assertEquals("\033[<64;1;1M", readResponse(terminal));
    }

    @Test
    public void mouseModesSurviveSaveAndLoad() {
        final Terminal saved = new Terminal();
        write(saved, "\033[?1000h\033[?1006h");

        final Terminal loaded = new Terminal();
        NBTSerialization.deserialize(NBTSerialization.serialize(saved), loaded);

        assertTrue(loaded.isMouseReportingEnabled());
        loaded.putMouseEvent(0, 0, 0, true);
        assertEquals("\033[<0;1;1M", readResponse(loaded), "the SGR mode must survive too");
    }

    // --------------------------------------------------------------------- //

    private static int cellCharacter(final Terminal terminal, final int index) {
        return Terminal.getCharacter(terminal.getCell(index));
    }

    private static int drainInput(final Terminal terminal) {
        int count = 0;
        while (terminal.readInput() != -1) {
            count++;
        }
        return count;
    }

    private static String readResponse(final Terminal terminal) {
        final StringBuilder response = new StringBuilder();
        int value;
        while ((value = terminal.readInput()) != -1) {
            response.append((char) value);
        }
        return response.toString();
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
