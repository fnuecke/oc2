package li.cil.oc2.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntegerSpaceTest {
    @Test
    void integerSpaceTest() {
        final IntegerSpace space = new IntegerSpace();
        try {
            assertEquals(0, space.count());
            assertFalse(space.contains(30));

            assertTrue(space.put(30)); // [30]
            assertFalse(space.contains(29));
            assertTrue(space.contains(30));
            assertFalse(space.contains(31));
            assertEquals(1, space.rangeCount());
            assertEquals(1, space.count());

            assertTrue(space.put(31)); // [30-31]
            assertEquals(1, space.rangeCount());
            assertEquals(2, space.count());
            assertFalse(space.contains(29));
            assertTrue(space.contains(31));
            assertFalse(space.contains(32));

            assertTrue(space.put(32)); // [30-32]
            assertEquals(1, space.rangeCount());
            assertEquals(3, space.count());
            assertFalse(space.contains(29));
            assertTrue(space.contains(30));
            assertTrue(space.contains(31));
            assertTrue(space.contains(32));
            assertFalse(space.contains(33));

            assertTrue(space.put(29)); // [29-32]
            assertEquals(1, space.rangeCount());
            assertEquals(4, space.count());
            assertFalse(space.contains(28));
            assertTrue(space.contains(29));
            assertTrue(space.contains(30));
            assertTrue(space.contains(31));
            assertTrue(space.contains(32));
            assertFalse(space.contains(33));

            assertFalse(space.put(29));
            assertFalse(space.put(30));
            assertFalse(space.put(31));
            assertFalse(space.put(32));

            assertTrue(space.put(34)); // [29-32] [34]
            assertFalse(space.contains(33));
            assertEquals(2, space.rangeCount());
            assertEquals(5, space.count());
            assertFalse(space.contains(33));
            assertTrue(space.contains(34));

            assertTrue(space.put(33)); // [29-34]
            assertEquals(1, space.rangeCount());
            assertEquals(6, space.count());
            assertTrue(space.contains(33));

            assertTrue(space.put(38)); // [29-34] [38]
            assertEquals(2, space.rangeCount());
            assertEquals(7, space.count());
            assertTrue(space.contains(38));

            assertTrue(space.put(37)); // [29-34] [37-38]
            assertEquals(2, space.rangeCount());
            assertEquals(8, space.count());
            assertTrue(space.contains(37));
            assertFalse(space.contains(36));
            assertFalse(space.contains(35));

            assertTrue(space.put(35)); // [29-35] [37-38]
            assertEquals(2, space.rangeCount());
            assertEquals(9, space.count());
            assertTrue(space.contains(37));
            assertFalse(space.contains(36));
            assertTrue(space.contains(35));

            assertTrue(space.put(27)); // [27] [29-35] [37-38]
            assertEquals(3, space.rangeCount());
            assertEquals(10, space.count());
            assertTrue(space.contains(27));
            assertFalse(space.contains(28));
            assertTrue(space.contains(29));

            assertTrue(space.put(31, 37)); // [27] [29-38]
            assertEquals(2, space.rangeCount());
            assertEquals(11, space.count());
            assertTrue(space.contains(27));
            assertFalse(space.contains(28));
            for (int i = 29; i <= 38; ++i) {
                assertTrue(space.contains(i), Integer.toString(i));
            }

            assertTrue(space.put(33, 39)); // [27] [29-39]
            assertEquals(2, space.rangeCount());
            assertEquals(12, space.count());
            assertEquals("[27, 29-39]", space.toString());

            assertTrue(space.put(23, 26));
            assertEquals("[23-27, 29-39]", space.toString());
        } catch (final AssertionError e) {
            System.out.println("Space state: " + space);
            throw e;
        }
    }
}
