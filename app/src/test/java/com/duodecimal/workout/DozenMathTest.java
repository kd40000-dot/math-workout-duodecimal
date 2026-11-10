package com.duodecimal.workout;

import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.*;

public class DozenMathTest {
    @Test public void representation() {
        assertEquals("↊", DozenMath.format(10));
        assertEquals("↋", DozenMath.format(11));
        assertEquals("10", DozenMath.format(12));
        assertEquals("100", DozenMath.format(144));
        assertEquals("1000", DozenMath.format(1728));
        assertEquals("−↋", DozenMath.format(-11));
        assertEquals(Long.valueOf(143), DozenMath.parse("BB"));
        assertEquals(Long.valueOf(143), DozenMath.parse("↋↋"));
        assertNull(DozenMath.parse("12.5"));
        assertNull(DozenMath.parse("↌"));
    }
    @Test public void everyModeIsBaseTwelveAndCheckable() {
        DozenMath math = new DozenMath(new Random(425));
        for (int mode = 0; mode < DozenMath.MODES.length; mode++) {
            for (int difficulty = 1; difficulty <= 5; difficulty++) {
                for (int i=0; i<100; i++) {
                    DozenMath.Problem p = math.next(mode, difficulty);
                    assertTrue(p.prompt + " answer=" + p.answer, p.answer >= 0);
                    assertEquals(Long.valueOf(p.answer),DozenMath.parse(DozenMath.format(p.answer)));
                    assertFalse(p.prompt.contains("NaN"));
                }
            }
        }
    }
}
