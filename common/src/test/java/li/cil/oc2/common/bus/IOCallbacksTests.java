/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOCallbacks;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.api.bus.device.io.IOName;
import li.cil.oc2.api.util.Side;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class IOCallbacksTests {
    @Test
    public void collectsAnnotatedMethods() {
        final List<IOMethod> functions = IOCallbacks.collectMethods(new AllSignatures());
        assertEquals(4, functions.size());
        assertEquals(List.of(1, 2, 3, 4), functions.stream().map(IOMethod::getCode).sorted().toList());
    }

    @Test
    public void collectsNothingFromPlainObject() {
        assertTrue(IOCallbacks.collectMethods(new Object()).isEmpty());
        assertFalse(IOCallbacks.hasMethods(new Object()));
        assertTrue(IOCallbacks.hasMethods(new AllSignatures()));
    }

    @Test
    public void synchronizeDefaultsToTrue() {
        final AllSignatures target = new AllSignatures();
        assertTrue(function(target, 1).isSynchronized());
        assertFalse(function(target, 4).isSynchronized());
    }

    @Test
    public void invokesAllFourSignatures() throws Throwable {
        final AllSignatures target = new AllSignatures();

        invoke(function(target, 1), new byte[0]);
        assertTrue(target.calledWithoutStreams);

        invoke(function(target, 2), new byte[]{42});
        assertEquals(42, target.readArgument);

        assertArrayEquals(new byte[]{7}, invoke(function(target, 3), new byte[0]));

        assertArrayEquals(new byte[]{23}, invoke(function(target, 4), new byte[]{23}));
    }

    @Test
    public void duplicateFunctionCodesThrow() {
        assertThrows(IllegalArgumentException.class, () -> IOCallbacks.collectMethods(new DuplicateCodes()));
    }

    @Test
    public void functionCodeOutsideRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> IOCallbacks.collectMethods(new ReservedCode()));
        assertThrows(IllegalArgumentException.class, () -> IOCallbacks.collectMethods(new NegativeCode()));
    }

    @Test
    public void unsupportedSignatureThrows() {
        assertThrows(IllegalArgumentException.class, () -> IOCallbacks.collectMethods(new WrongParameters()));
        assertThrows(IllegalArgumentException.class, () -> IOCallbacks.collectMethods(new NonVoidReturn()));
    }

    @Test
    public void missingNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> IOCallbacks.getName(new Unnamed()));
    }

    @Test
    public void nameIsReadFromAnnotation() {
        assertEquals("TEST", IOCallbacks.getName(new AllSignatures()));
    }

    @Test
    public void overlongNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> IOCallbacks.getName(new OverlongName()));
    }

    @Test
    public void sideByIndexFollowsMinecraftNumbering() {
        for (int i = 0; i < 6; i++) {
            assertEquals(Direction.from3DDataValue(i), Side.byIndex(i).getDirection());
        }
        assertThrows(IllegalArgumentException.class, () -> Side.byIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> Side.byIndex(6));
    }

    // --------------------------------------------------------------------- //

    private static IOMethod function(final Object target, final int code) {
        return IOCallbacks.collectMethods(target).stream()
            .filter(f -> f.getCode() == code)
            .findFirst().orElseThrow();
    }

    private static byte[] invoke(final IOMethod function, final byte[] arguments) throws Throwable {
        final ByteArrayOutputStream results = new ByteArrayOutputStream();
        function.invoke(new ByteArrayInputStream(arguments), results);
        return results.toByteArray();
    }

    // --------------------------------------------------------------------- //

    @IOName("TEST")
    public static final class AllSignatures {
        public boolean calledWithoutStreams;
        public int readArgument = -1;

        @IOCallback(1)
        public void noStreams() {
            calledWithoutStreams = true;
        }

        @IOCallback(2)
        public void argumentsOnly(final InputStream arguments) throws Exception {
            readArgument = arguments.read();
        }

        @IOCallback(3)
        public void resultsOnly(final OutputStream results) throws Exception {
            results.write(7);
        }

        @IOCallback(value = 4, synchronize = false)
        public void bothStreams(final InputStream arguments, final OutputStream results) throws Exception {
            results.write(arguments.read());
        }
    }

    @IOName("DUP")
    public static final class DuplicateCodes {
        @IOCallback(1)
        public void first() {
        }

        @IOCallback(1)
        public void second() {
        }
    }

    @IOName("RSVD")
    public static final class ReservedCode {
        @IOCallback(IOCallback.RESERVED_CODE)
        public void reserved() {
        }
    }

    @IOName("NEG")
    public static final class NegativeCode {
        @IOCallback(-1)
        public void negative() {
        }
    }

    @IOName("PARAM")
    public static final class WrongParameters {
        @IOCallback(1)
        public void wrong(final String unsupported) {
        }
    }

    @IOName("RET")
    public static final class NonVoidReturn {
        @IOCallback(1)
        public int wrong() {
            return 0;
        }
    }

    public static final class Unnamed {
        @IOCallback(1)
        public void unnamed() {
        }
    }

    @IOName("TOOLONGNAME")
    public static final class OverlongName {
        @IOCallback(1)
        public void overlong() {
        }
    }
}
