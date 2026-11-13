package com.duodecimal.workout;

import java.util.Locale;

/**
 * Display formatting for statistics only. Facts remain in base twelve
 * with custom numeral glyphs; statistical measurements use base-ten
 * Arabic numerals independently of the device locale.
 */
public final class StatisticsNumbers {
    private StatisticsNumbers() {}

    public static String count(long value) {
        return Long.toString(value);
    }

    public static String percent(long numerator, long denominator) {
        return denominator <= 0 ? "—" :
            String.format(Locale.US, "%.1f%%", numerator * 100.0 / denominator);
    }

    public static String seconds(long milliseconds) {
        return String.format(Locale.US, "%.2f s",
            Math.max(0, milliseconds) / 1000.0);
    }

    public static String percentWhole(long numerator, long denominator) {
        return denominator <= 0 ? "—" :
            Math.round(numerator * 100.0 / denominator) + "%";
    }
}
