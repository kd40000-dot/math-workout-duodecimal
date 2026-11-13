package com.duodecimal.workout;

/**
 * Consistent base-twelve time display.
 * Two fractional dozenal digits = 1/144 second resolution.
 * Stored and compared response times remain precise integer milliseconds.
 */
public final class PracticeTiming {
    private PracticeTiming() {}

    public static String formatSeconds(long milliseconds) {
        long safe=Math.max(0,milliseconds);
        long ticks=Math.round(safe*144.0/1000.0);
        long whole=ticks/144;
        int digits=(int)(ticks%144);
        return DozenMath.format(whole)+"."+
            DozenMath.DIGITS.charAt(digits/12)+
            DozenMath.DIGITS.charAt(digits%12)+" s";
    }
}
