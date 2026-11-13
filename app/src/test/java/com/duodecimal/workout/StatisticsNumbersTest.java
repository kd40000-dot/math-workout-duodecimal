package com.duodecimal.workout;

import org.junit.Test;
import static org.junit.Assert.*;

/** Statistics are always decimal; arithmetic facts use DozenMath separately. */
public final class StatisticsNumbersTest {
    @Test public void countUsesBaseTenArabicDigits() {
        assertEquals("10", StatisticsNumbers.count(10));
        assertEquals("11", StatisticsNumbers.count(11));
        assertEquals("12", StatisticsNumbers.count(12));
        assertEquals("144", StatisticsNumbers.count(144));
        assertEquals("0", StatisticsNumbers.count(0));
    }

    @Test public void percentagesAreConventionalDecimal() {
        assertEquals("50.0%", StatisticsNumbers.percent(1,2));
        assertEquals("83.3%", StatisticsNumbers.percent(5,6));
        assertEquals("100.0%", StatisticsNumbers.percent(12,12));
        assertEquals("—", StatisticsNumbers.percent(0,0));
        assertEquals("83%", StatisticsNumbers.percentWhole(5,6));
        assertEquals("100%", StatisticsNumbers.percentWhole(12,12));
        assertEquals("—", StatisticsNumbers.percentWhole(0,0));
    }

    @Test public void secondsAreBaseTenWithDecimalPoint() {
        assertEquals("0.00 s", StatisticsNumbers.seconds(0));
        assertEquals("0.50 s", StatisticsNumbers.seconds(500));
        assertEquals("1.25 s", StatisticsNumbers.seconds(1250));
        assertEquals("12.00 s", StatisticsNumbers.seconds(12000));
        assertEquals("—", StatisticsNumbers.percent(1,0));
    }
}
