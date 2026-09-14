/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MuLawTests {
    @Test
    public void everyCodeSurvivesARoundTrip() {
        for (int code = 0; code < 256; code++) {
            if (code == 0x7F) {
                continue; // Negative zero; encoding zero yields the positive one.
            }
            assertEquals((byte) code, MuLaw.encode(MuLaw.decode((byte) code)), "code " + code);
        }
    }

    @Test
    public void decodingMatchesG711() {
        assertEquals(0, MuLaw.decode((byte) 0xFF), "0xFF is positive zero");
        assertEquals(0, MuLaw.decode((byte) 0x7F), "0x7F is negative zero");
        assertEquals(-32124, MuLaw.decode((byte) 0x00), "0x00 is the largest negative magnitude");
        assertEquals(32124, MuLaw.decode((byte) 0x80), "0x80 is the largest positive magnitude");
    }

    @Test
    public void errorIsProportionalToTheSignal() {
        for (int sample = Short.MIN_VALUE; sample <= Short.MAX_VALUE; sample++) {
            final int error = Math.abs(MuLaw.decode(MuLaw.encode(sample)) - sample);
            assertTrue(error <= Math.max(4, Math.abs(sample) / 15),
                "sample " + sample + " came back off by " + error);
        }
    }

    @Test
    public void samplesBeyondTheRangeClip() {
        assertEquals(MuLaw.encode(32635), MuLaw.encode(Short.MAX_VALUE));
        assertEquals(MuLaw.encode(-32635), MuLaw.encode(Short.MIN_VALUE));
    }
}
