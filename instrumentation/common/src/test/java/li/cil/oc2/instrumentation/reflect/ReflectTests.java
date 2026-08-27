/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation.reflect;

import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.UnusedPrivateMethod"})
public final class ReflectTests {
    private static final ReflectContext CTX = new ReflectContext(null, null, 4);

    private static String get(final Object root, final String path) {
        return String.valueOf(Reflect.get(root, path, CTX).value());
    }

    private static String set(final Object root, final String path, final String json) {
        return String.valueOf(Reflect.set(root, path, json, CTX).value());
    }

    private static String invoke(final Object root, final String path, final List<String> args) {
        return String.valueOf(Reflect.invoke(root, path, args, CTX).value());
    }

    private record Point(int x, int y) {
    }

    private static final class Inner {
        private final int[] numbers = {10, 20, 30};
        private final List<String> names = new java.util.ArrayList<>(List.of("a", "b"));
        private final Map<String, Integer> scores = new LinkedHashMap<>(Map.of("k", 7));
        private String label = "inner";
    }

    private enum Mode { FAST, FANCY }

    private interface Described {
        default String defaulted() {
            return "from-interface";
        }
    }

    private static final class Ambiguous {
        private String pick(final long value) {
            return "long:" + value;
        }

        private String pick(final double value) {
            return "double:" + value;
        }
    }

    private static final class Outer implements Described {
        private static final int SHARED = 3;
        private Boolean flag = Boolean.TRUE;
        private Mode mode = Mode.FAST;
        private String text = "t";
        @Nullable
        private Object nothing;
        private final Inner inner = new Inner();
        private final Point point = new Point(1, 2);
        private final byte[] raw = new byte[4];
        private int counter;
        private final String constant = String.valueOf("fixed");
        private Outer self;

        private int twice(final int value) {
            return value * 2;
        }

        private String describe(final String text) {
            return "s:" + text;
        }

        private String describe(final int number) {
            return "i:" + number;
        }

        private void write(final ByteBuffer buffer) {
            buffer.get(raw, 0, Math.min(raw.length, buffer.remaining()));
        }

        private void bump() {
            counter++;
        }
    }

    // --------------------------------------------------------------------- //

    @Test
    void readsNestedPrivateFields() {
        assertEquals("\"inner\"", get(new Outer(), "inner.label"));
    }

    @Test
    void readsArrayListAndMapIndices() {
        final Outer outer = new Outer();
        assertEquals("20", get(outer, "inner.numbers[1]"));
        assertEquals("\"b\"", get(outer, "inner.names[1]"));
        assertEquals("7", get(outer, "inner.scores[k]"));
    }

    @Test
    void writesPrivateAndFinalFields() {
        final Outer outer = new Outer();
        set(outer, "counter", "5");
        assertEquals(5, outer.counter);

        Reflect.set(outer, "constant", "\"changed\"", CTX);
        assertEquals("\"changed\"", get(outer, "constant"));
    }

    @Test
    void writesIntoArraysListsAndMaps() {
        final Outer outer = new Outer();
        set(outer, "inner.numbers[0]", "99");
        assertEquals(99, outer.inner.numbers[0]);

        Reflect.set(outer, "inner.names[0]", "\"z\"", CTX);
        assertEquals("z", outer.inner.names.get(0));

        set(outer, "inner.scores[k]", "42");
        assertEquals(42, outer.inner.scores.get("k"));
    }

    @Test
    void refusesToWriteRecordComponents() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> set(new Outer(), "point.x", "9"));
        assertTrue(e.getMessage().contains("cannot write"), e.getMessage());
    }

    @Test
    void invokesPrivateMethods() {
        assertEquals("42", invoke(new Outer(), "twice", List.of("21")));
    }

    @Test
    void invokeReturnsOkForVoid() {
        final Outer outer = new Outer();
        assertEquals("null", invoke(outer, "bump", List.of()));
        assertEquals(1, outer.counter);
    }

    @Test
    void selectsOverloadByJsonShape() {
        final Outer outer = new Outer();
        assertEquals("\"s:hi\"", invoke(outer, "describe", List.of("\"hi\"")));
        assertEquals("\"i:3\"", invoke(outer, "describe", List.of("3")));
    }

    @Test
    void selectsOverloadByExplicitSignature() {
        assertEquals("\"s:7\"", invoke(new Outer(), "describe(String)", List.of("\"7\"")));
    }

    @Test
    void coercesStringToByteBuffer() {
        final Outer outer = new Outer();
        invoke(outer, "write", List.of("\"abcd\""));
        assertEquals("abcd", new String(outer.raw, StandardCharsets.UTF_8));
    }

    @Test
    void nestedMinecraftTypesGoThroughTheSameConversions() {
        final Object value = Values.fromJson("[\"a\",\"b\"]", List.class,
            new java.util.ArrayList<String>() {
            }.getClass().getGenericSuperclass(), CTX);
        assertInstanceOf(List.class, value, String.valueOf(value));
    }

    @Test
    void rejectsUnknownEnumConstantInsteadOfReturningNull() {
        final Outer outer = new Outer();
        assertThrows(IllegalArgumentException.class, () -> Reflect.set(outer, "mode", "\"NOPE\"", CTX));
        assertEquals(Mode.FAST, outer.mode);
    }

    @Test
    void acceptsEnumInAnyCase() {
        final Outer outer = new Outer();
        set(outer, "mode", "\"fancy\"");
        assertEquals(Mode.FANCY, outer.mode);
    }

    @Test
    void coercesStringToByteArray() {
        final Object value = Values.fromJson("\"hi\"", byte[].class, null, CTX);
        assertEquals("hi", new String((byte[]) value, StandardCharsets.UTF_8));
    }

    @Test
    void truncationIsVisibleNotSilent() {
        final ReflectContext shallow = new ReflectContext(null, null, 1);
        final String json = String.valueOf(Reflect.get(new Outer(), "inner", shallow).value());
        assertTrue(json.contains("<truncated"), json);
    }

    @Test
    void cyclesAreMarkedNotFatal() {
        final Outer outer = new Outer();
        outer.self = outer;
        final String json = get(outer, "self");
        assertTrue(json.contains("<cycle"), json);
    }

    @Test
    void listShowsFieldsAndMethods() {
        final Results.Listing listing = Reflect.list(new Outer(), null, CTX);
        assertTrue(listing.fields().stream()
            .anyMatch(f -> f.name().equals("counter") && f.type().equals("int")), listing.toString());
        assertTrue(listing.methods().stream()
            .anyMatch(m -> m.name().equals("twice") && m.parameters().equals(List.of("int"))
                && m.returns().equals("int")), listing.toString());
    }

    @Test
    void listWalksAPath() {
        final Results.Listing listing = Reflect.list(new Outer(), "inner", CTX);
        assertTrue(listing.fields().stream().anyMatch(f -> f.name().equals("label")), listing.toString());
    }

    @Test
    void rejectsNonBooleanForBoolean() {
        final Outer outer = new Outer();
        assertThrows(IllegalArgumentException.class, () -> set(outer, "flag", "1"));
        assertThrows(IllegalArgumentException.class, () -> Reflect.set(outer, "flag", "\"yes\"", CTX));
        assertEquals(Boolean.TRUE, outer.flag);
    }

    @Test
    void rejectsNumbersThatDoNotFit() {
        final Outer outer = new Outer();
        assertThrows(IllegalArgumentException.class, () -> set(outer, "counter", "4294967296"));
        assertThrows(IllegalArgumentException.class, () -> set(outer, "counter", "3.9"));
        assertEquals(0, outer.counter);
    }

    @Test
    void rejectsNonStringForString() {
        final Outer outer = new Outer();
        assertThrows(IllegalArgumentException.class, () -> set(outer, "text", "12"));
        assertEquals("t", outer.text);
    }

    @Test
    void rejectsTrailingGarbageInAPath() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> get(new Outer(), "inner.names[0]junk"));
        assertTrue(e.getMessage().contains("trailing"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> get(new Outer(), "inner."));
    }

    @Test
    void refusesToGuessBetweenEqualOverloads() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> invoke(new Ambiguous(), "pick", List.of("3")));
        assertTrue(e.getMessage().contains("ambiguous"), e.getMessage());
        assertEquals("\"long:3\"", invoke(new Ambiguous(), "pick(long)", List.of("3")));
    }

    @Test
    void reachesObjectAndInterfaceMethods() {
        assertTrue(invoke(new Outer(), "toString", List.of()).contains("Outer"));
        assertEquals("\"from-interface\"", invoke(new Outer(), "defaulted", List.of()));
    }

    @Test
    void indexingNullFailsClearly() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> get(new Outer(), "nothing[0]"));
        assertTrue(e.getMessage().contains("cannot index null"), e.getMessage());
    }

    @Test
    void listShowsStatics() {
        final Results.Listing listing = Reflect.list(new Outer(), null, CTX);
        assertTrue(listing.fields().stream().anyMatch(f -> f.name().equals("SHARED") && f.isStatic()),
            listing.toString());
    }

    @Test
    void responsesAreOneJsonObjectWithAResult() {
        final String json = Results.render(Results.Outcome.success(
            new Results.Inserted("oc2:memory_large", 8, 4, 4, "deviceItems")));
        assertEquals("{\"result\":\"success\",\"item\":\"oc2:memory_large\",\"requested\":8,"
            + "\"inserted\":4,\"remainder\":4,\"handler\":\"deviceItems\"}", json);
    }

    @Test
    void errorsCarryTheSameEnvelope() {
        final String json = Results.render(Results.Outcome.error(new IllegalArgumentException("nope")));
        assertEquals("{\"result\":\"error\",\"error\":\"nope\",\"type\":\"IllegalArgumentException\"}", json);
    }

    @Test
    void tokenizerKeepsJsonTogetherAndRejectsBadQuoting() {
        assertEquals(List.of("a.b", "{\"x\": 1, \"y\": [2, 3]}"),
            ReflectOps.tokenize("a.b {\"x\": 1, \"y\": [2, 3]}"));
        assertEquals(List.of("a", "\"one two\""), ReflectOps.tokenize("a \"one two\""));
        assertThrows(IllegalArgumentException.class, () -> ReflectOps.tokenize("set a.b \"oops"));
        assertThrows(IllegalArgumentException.class, () -> ReflectOps.tokenize("set a.b {\"x\":1"));
        assertThrows(IllegalArgumentException.class, () -> ReflectOps.tokenize("set a.b }"));
    }

    @Test
    void unknownFieldPointsAtList() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> get(new Outer(), "nope"));
        assertTrue(e.getMessage().contains("'list'"), e.getMessage());
    }
}
