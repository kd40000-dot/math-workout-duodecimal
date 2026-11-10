package com.duodecimal.workout;

import java.util.Random;

/** Base-twelve workout engine. Internal integers; all visible values in base twelve. */
public final class DozenMath {
    public static final String DIGITS = "0123456789↊↋";
    public static final String[] MODES = {
        "Mixed workout", "Addition", "Subtraction", "Multiplication", "Exact division",
        "Squares", "Square roots", "Powers", "Times tables", "Missing number",
        "Three-term arithmetic", "Bigger number", "Middle number", "Rounding to dozens",
        "Per-gross percentages", "Fractions of quantities", "Near 100₁₂ products",
        "Doubling and halving", "Remainders", "Place value"
    };
    private final Random random;
    public DozenMath(Random random) { this.random = random; }
    public DozenMath() { this(new Random()); }

    public static String format(long value) {
        if (value == 0) return "0";
        if (value < 0) return "−" + format(-value);
        StringBuilder result = new StringBuilder();
        do { result.append(DIGITS.charAt((int) (value % 12))); value /= 12; }
        while (value != 0);
        return result.reverse().toString();
    }
    public static Long parse(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        String s = text.trim().replace('A', '↊').replace('a', '↊')
            .replace('B', '↋').replace('b', '↋').replace('−', '-');
        boolean neg = s.startsWith("-");
        int start = neg ? 1 : 0;
        if (start == s.length()) return null;
        long value = 0;
        for (int i = start; i < s.length(); i++) {
            int digit = DIGITS.indexOf(s.charAt(i));
            if (digit < 0 || value > (Long.MAX_VALUE - digit) / 12) return null;
            value = 12 * value + digit;
        }
        return neg ? -value : value;
    }
    public static final class Problem {
        public final String mode, prompt, details;
        public final long answer;
        Problem(String mode, String prompt, long answer, String details) {
            this.mode = mode; this.prompt = prompt; this.answer = answer; this.details = details;
        }
    }
    private long power(int n) { long x = 1; for (int i=0;i<n;i++) x*=12; return x; }
    private long number(int digits) {
        long min = digits <= 1 ? 1 : power(digits - 1);
        long max = power(digits) - 1;
        return min + random.nextInt((int)(max - min + 1));
    }
    private long small(int max) { return 1 + random.nextInt(max); }
    private Problem p(int mode, String text, long answer) {
        return new Problem(MODES[mode], text, answer,
            text + " = " + format(answer) + " (all values are base 12)");
    }
    public Problem next(int requestedMode, int difficulty) {
        int level = Math.max(1, Math.min(5, difficulty));
        int mode = requestedMode <= 0 || requestedMode >= MODES.length
            ? 1 + random.nextInt(MODES.length - 1) : requestedMode;
        int digits = Math.min(4, level);
        long a, b, c, result;
        switch (mode) {
            case 1:
                a = number(digits); b = number(digits);
                return p(mode, format(a) + " + " + format(b), a + b);
            case 2:
                a = number(digits); b = number(digits);
                if (b > a) { long tmp = a; a = b; b = tmp; }
                return p(mode, format(a) + " − " + format(b), a - b);
            case 3:
                a = number(Math.min(3, 1 + level / 2));
                b = number(Math.min(3, 1 + (level-1) / 2));
                return p(mode, format(a) + " × " + format(b), a*b);
            case 4:
                b = number(Math.min(3, 1 + level / 2));
                c = number(Math.min(3, 1 + (level-1) / 2));
                return p(mode, format(b*c) + " ÷ " + format(b), c);
            case 5:
                a = number(Math.min(3, 1 + level / 2));
                return p(mode, format(a) + "²", a*a);
            case 6:
                a = number(Math.min(3, 1 + level / 2));
                return p(mode, "√" + format(a*a), a);
            case 7:
                a = number(Math.min(2, 1 + level / 3));
                b = 2 + random.nextInt(level >= 4 ? 3 : 2);
                result = 1;
                for (int i=0; i<b; i++) result *= a;
                return p(mode, format(a) + "^" + format(b), result);
            case 8:
                a = small(11); b = small(Math.min(11, level * 2 + 1));
                return p(mode, format(a) + " × " + format(b), a*b);
            case 9:
                a = number(digits); b = number(digits);
                return p(mode, format(a) + " + □ = " + format(a+b), b);
            case 10:
                a = number(Math.min(3, level)); b = number(Math.min(3, level));
                c = small(Math.min(11,level*2+1));
                return p(mode, "(" + format(a) + " + " + format(b) + ") × " + format(c), (a+b)*c);
            case 11:
                a = number(digits); b = number(digits);
                return p(mode, "Larger: " + format(a) + " or " + format(b) + "?", Math.max(a,b));
            case 12:
                a = number(digits); b = number(digits); c = number(digits);
                long min = Math.min(a,Math.min(b,c)), max = Math.max(a,Math.max(b,c));
                return p(mode, "Middle: " + format(a) + ", " + format(b) + ", " + format(c) + "?", a+b+c-min-max);
            case 13:
                a = number(Math.max(2,digits));
                return p(mode, "Round " + format(a) + " to nearest 10₁₂", ((a+6)/12)*12);
            case 14:
                // An amount "per 100₁₂" means per gross (dividing by 144).
                a = (1 + random.nextInt(11)) * 12L;
                b = (1 + random.nextInt(Math.min(100, level*15))) * 12L;
                return p(mode, format(a) + " per 100₁₂ of " + format(b), a*b/144L);
            case 15:
                b = 2 + random.nextInt(Math.min(10,level*2+1));
                a = small(Math.min(40,level*8)) * b;
                return p(mode, "1/" + format(b) + " of " + format(a), a/b);
            case 16:
                a = 144 + (random.nextBoolean() ? 1 : -1) * small(level * 4);
                b = 144 + (random.nextBoolean() ? 1 : -1) * small(level * 4);
                return p(mode, format(a) + " × " + format(b), a*b);
            case 17:
                a = number(digits);
                if (random.nextBoolean()) return p(mode, "Double " + format(a), a*2);
                return p(mode, "Half of " + format(a*2), a);
            case 18:
                b = 2 + random.nextInt(Math.min(10, 2+level*2));
                c = random.nextInt((int)b);
                a = number(Math.min(3,level)) * b + c;
                return p(mode, format(a) + " mod " + format(b), c);
            case 19:
                a = number(Math.max(2,digits));
                result = (a / 12) % 12;
                return p(mode, "Dozens-place digit of " + format(a) + "?", result);
            default: throw new IllegalStateException("Unknown mode: " + mode);
        }
    }
}
