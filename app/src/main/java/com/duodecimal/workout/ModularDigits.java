package com.duodecimal.workout;

/**
 * Draft A duodecimal glyph assignment. All printed numbers are base 12.
 * The body reports the remainder mod 4, the cap the remainder mod 3.
 */
public final class ModularDigits {
    private ModularDigits() {}
    public static int valueOf(char digit) {
        if (digit >= '0' && digit <= '9') return digit - '0';
        if (digit == '↊') return 10;
        if (digit == '↋') return 11;
        return -1;
    }
    public static int body(int value) {
        if (value < 0 || value > 11) throw new IllegalArgumentException("not a base-12 digit");
        return value % 4;
    }
    public static int cap(int value) {
        if (value < 0 || value > 11) throw new IllegalArgumentException("not a base-12 digit");
        return value % 3;
    }
}
