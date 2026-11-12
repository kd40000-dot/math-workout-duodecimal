package com.duodecimal.workout;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ModularDigitsTest {
    @Test public void allTwelveDigitsHaveUniqueModuloPair() {
        boolean[][] found = new boolean[3][4];
        for (int value=0; value<12; value++) {
            int cap = ModularDigits.cap(value), body = ModularDigits.body(value);
            assertEquals(value % 3, cap);
            assertEquals(value % 4, body);
            assertFalse("Duplicate cap/body pair for "+value, found[cap][body]);
            found[cap][body] = true;
            String formatted = DozenMath.format(value);
            assertEquals(value, ModularDigits.valueOf(formatted.charAt(0)));
        }
        for (int cap=0;cap<3;cap++) for(int body=0;body<4;body++) assertTrue(found[cap][body]);
    }

    @Test public void exactlyTheIntendedDigitsShareEachBody() {
        assertEquals(3, ModularDigits.body(3)); // U+218B regular shape
        assertEquals(3, ModularDigits.body(7));
        assertEquals(3, ModularDigits.body(11));
        assertEquals(1, ModularDigits.body(1)); // short-bar inverted T
        assertEquals(1, ModularDigits.body(5));
        assertEquals(1, ModularDigits.body(9));
        assertEquals(2, ModularDigits.cap(2)); // dot
        assertEquals(1, ModularDigits.cap(10)); // breve
        assertEquals(0, ModularDigits.cap(9)); // flat
    }

    @Test public void parserRemainsLegacyCompatible() {
        assertEquals(Long.valueOf(143), DozenMath.parse("↋↋"));
        assertEquals("↊↋", DozenMath.format(131));
        assertEquals(-1, ModularDigits.valueOf('x'));
        assertTrue(DozenMath.isCorrect("10", 12));
    }
}
