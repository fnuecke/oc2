/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TerminalParserTests {
    @Test
    public void textIsPrinted() {
        assertEquals(List.of("print:h", "print:i"), parse("hi"));
    }

    @Test
    public void multiByteCharactersArriveWhole() {
        assertEquals(List.of("print:ä"), parse("ä"));
    }

    @Test
    public void controlsAreExecuted() {
        assertEquals(List.of("execute:13", "execute:10"), parse("\r\n"));
    }

    @Test
    public void controlSequencesCarryTheirParameters() {
        assertEquals(List.of("csi:H(12,34)"), parse("\033[12;34H"));
    }

    @Test
    public void omittedParametersReadAsZero() {
        assertEquals(List.of("csi:H(0)"), parse("\033[H"));
    }

    @Test
    public void thePrivateMarkerIsReported() {
        assertEquals(List.of("csi:h?(7)"), parse("\033[?7h"));
    }

    @Test
    public void escapeSequencesReportTheirIntermediate() {
        assertEquals(List.of("esc:(0"), parse("\033(0"));
    }

    @Test
    public void escapeSequencesWithoutAnIntermediateReportNone() {
        assertEquals(List.of("esc:M"), parse("\033M"));
    }

    @Test
    public void sequencesWeDoNotImplementAreSwallowed() {
        assertEquals(List.of("print:X"), parse("\033[2 qX"));
    }

    @Test
    public void stringsAreSwallowed() {
        assertEquals(List.of("print:X"), parse("\033]0;title\007X"));
    }

    @Test
    public void escapeAbandonsAnUnfinishedSequence() {
        assertEquals(List.of("csi:J(2)"), parse("\033[1;\033[2J"));
    }

    @Test
    public void controlsInsideASequenceAreExecutedWithoutEndingIt() {
        assertEquals(List.of("execute:13", "csi:H(1)"), parse("\033[1\rH"));
    }

    // --------------------------------------------------------------------- //

    private static List<String> parse(final String value) {
        final TerminalParser parser = new TerminalParser();
        final Recorder recorder = new Recorder();
        for (final byte b : value.getBytes(StandardCharsets.UTF_8)) {
            parser.put(b, recorder);
        }
        return recorder.events;
    }

    private static final class Recorder implements TerminalParser.Sink {
        private final List<String> events = new ArrayList<>();

        @Override
        public void print(final char ch) {
            events.add("print:" + ch);
        }

        @Override
        public void execute(final byte control) {
            events.add("execute:" + control);
        }

        @Override
        public void escapeDispatch(final char intermediate, final char finalByte) {
            events.add("esc:" + (intermediate == 0 ? "" : intermediate) + finalByte);
        }

        @Override
        public void controlSequenceDispatch(final char finalByte, final TerminalParser.Parameters parameters) {
            final StringBuilder builder = new StringBuilder("csi:").append(finalByte);
            if (parameters.isPrivate()) {
                builder.append('?');
            }
            builder.append('(');
            for (int i = 0; i < parameters.count(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(parameters.get(i));
            }
            events.add(builder.append(')').toString());
        }
    }
}
