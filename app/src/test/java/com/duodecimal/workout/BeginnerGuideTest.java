package com.duodecimal.workout;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/** Tutorial examples must stay mathematically correct as copy evolves. */
public final class BeginnerGuideTest {
    @Test public void baseTwelvePlaceValuesAreCorrect() {
        assertEquals("↊",DozenMath.format(10));
        assertEquals("↋",DozenMath.format(11));
        assertEquals("10",DozenMath.format(12));
        assertEquals("100",DozenMath.format(144));
        assertEquals("20",DozenMath.format(24));
    }
    @Test public void oneDigitFractionsUseSimpleTwelfths() {
        assertEquals(12,3*4); // 1/3 = 0.4 base twelve
        assertEquals(12,4*3); // 1/4 = 0.3 base twelve
        assertEquals(12,6*2); // 1/6 = 0.2 base twelve
        assertEquals(12,2*6); // 1/2 = 0.6 base twelve
    }
    @Test public void everyGlyphHasUniqueCapAndBody() {
        Set<String> seen=new HashSet<>();
        for(int d=0;d<12;d++){
            String pair=ModularDigits.cap(d)+":"+ModularDigits.body(d);
            assertTrue("Duplicate glyph for digit "+d,seen.add(pair));
        }
        assertEquals(12,seen.size());
        assertEquals(1,ModularDigits.cap(7));  // Breve
        assertEquals(3,ModularDigits.body(7)); // El
        assertEquals(2,ModularDigits.cap(5));  // Dot
        assertEquals(1,ModularDigits.body(5)); // Stem
    }
}
