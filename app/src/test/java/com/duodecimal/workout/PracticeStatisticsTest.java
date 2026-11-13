package com.duodecimal.workout;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/** Safeguard base-twelve timing and truthful event-history decoding. */
public final class PracticeStatisticsTest {
    @Test public void secondsAlwaysUseTwoBaseTwelveFractionDigits() {
        assertEquals("0.00 s",PracticeTiming.formatSeconds(0));
        assertEquals("0.60 s",PracticeTiming.formatSeconds(500));
        assertEquals("1.00 s",PracticeTiming.formatSeconds(1000));
        assertEquals("1.60 s",PracticeTiming.formatSeconds(1500));
        assertEquals("10.00 s",PracticeTiming.formatSeconds(12000));
    }
    @Test public void historyOnlyDecodesRealWellFormedEvents() {
        List<PracticeHistory.Event> samples=PracticeHistory.decode(
            "1780000000|5|7|1234|1;damaged;1780000100|8|3|900|0",2);
        assertEquals(2,samples.size());
        assertEquals(2,samples.get(0).op);
        assertEquals(5,samples.get(0).a);
        assertEquals(7,samples.get(0).b);
        assertEquals(1234,samples.get(0).durationMs);
        assertTrue(samples.get(0).correct);
        assertFalse(samples.get(1).correct);
    }
}
