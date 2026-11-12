package com.duodecimal.workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Selected operand pairs in base twelve. Pure Java to keep drill logic testable. */
public final class PracticeGrid {
    public static final String[] OPERATIONS = {"Addition (+)", "Subtraction (−)", "Multiplication (×)", "Exact division (÷)"};
    public static final String[] SYMBOLS = {"+", "−", "×", "÷"};
    public static final String[] DIGIT_CHOICES = {"1 digit", "2 digits", "3 digits", "4 digits"};
    public static final int[] REP_CHOICES = {1, 2, 3, 5, 12, 24};
    public static final int CELL_PAGE_SIZE = 12;
    public static final int OP_ADD = 0, OP_SUB = 1, OP_MULTIPLY = 2, OP_DIVIDE = 3;

    private PracticeGrid() {}

    public static int firstForDigits(int digits) {
        if (digits < 1 || digits > 4) throw new IllegalArgumentException("digits must be 1..4");
        int min = 1;
        for (int i = 1; i < digits; i++) min *= 12;
        return digits == 1 ? 0 : min;
    }

    public static int lastForDigits(int digits) {
        if (digits < 1 || digits > 4) throw new IllegalArgumentException("digits must be 1..4");
        int end = 1;
        for (int i = 0; i < digits; i++) end *= 12;
        return end - 1;
    }

    public static int pageCount(int digits) {
        return (lastForDigits(digits) - firstForDigits(digits) + CELL_PAGE_SIZE) / CELL_PAGE_SIZE;
    }

    public static int firstForPage(int digits, int page) {
        if (page < 0 || page >= pageCount(digits)) throw new IllegalArgumentException("Invalid page");
        return firstForDigits(digits) + page * CELL_PAGE_SIZE;
    }

    public static long key(int a, int b) {
        return (((long) a) << 32) | (b & 0xffffffffL);
    }

    public static int firstOperand(long key) { return (int) (key >>> 32); }
    public static int secondOperand(long key) { return (int) key; }

    public static boolean valid(int op, int digitsA, int digitsB, int a, int b) {
        if (op < OP_ADD || op > OP_DIVIDE) return false;
        if (a < firstForDigits(digitsA) || a > lastForDigits(digitsA)) return false;
        if (b < firstForDigits(digitsB) || b > lastForDigits(digitsB)) return false;
        return op != OP_DIVIDE || (b != 0 && a % b == 0);
    }

    public static long answer(int op, int a, int b) {
        switch (op) {
            case OP_ADD: return (long) a + b;
            case OP_SUB: return (long) a - b;
            case OP_MULTIPLY: return (long) a * b;
            case OP_DIVIDE:
                if (b == 0 || a % b != 0) throw new IllegalArgumentException("Not an exact division");
                return a / b;
            default: throw new IllegalArgumentException("Unknown operation");
        }
    }

    public static String expression(int op, int a, int b) {
        return DozenMath.format(a) + " " + SYMBOLS[op] + " " + DozenMath.format(b);
    }

    public static String resultText(int op, int a, int b) {
        return valid(op, numberDigits(a), numberDigits(b), a, b)
            ? DozenMath.format(answer(op, a, b)) : "—";
    }

    private static int numberDigits(int n) {
        if (n < 12) return 1;
        if (n < 144) return 2;
        if (n < 1728) return 3;
        return 4;
    }

    public static String profileKey(int op, int digitsA, int digitsB) {
        return "custom_facts_v1_" + op + "_" + digitsA + "_" + digitsB;
    }

    public static String serialize(Set<Long> selected) {
        StringBuilder s = new StringBuilder();
        for (long key : selected) {
            if (s.length() != 0) s.append(',');
            s.append(DozenMath.format(firstOperand(key))).append(':')
             .append(DozenMath.format(secondOperand(key)));
        }
        return s.toString();
    }

    public static LinkedHashSet<Long> deserialize(String encoded, int op, int digitsA, int digitsB) {
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        if (encoded == null || encoded.isEmpty()) return set;
        for (String item : encoded.split(",")) {
            String[] pair = item.split(":", -1);
            if (pair.length != 2) continue;
            Long a = DozenMath.parse(pair[0]), b = DozenMath.parse(pair[1]);
            if (a == null || b == null || a > Integer.MAX_VALUE || b > Integer.MAX_VALUE ||
                    a < 0 || b < 0) continue;
            if (valid(op, digitsA, digitsB, a.intValue(), b.intValue()))
                set.add(key(a.intValue(), b.intValue()));
        }
        return set;
    }

    /**
     * A session contains exactly reps occurrences of each selected operand pair.
     * Shuffles each complete pass to avoid accidentally practicing one fact
     * repeatedly before seeing the others.
     */
    public static List<Long> makeSession(Set<Long> selected, int reps, Random random) {
        if (selected == null || selected.isEmpty()) throw new IllegalArgumentException("Select at least one fact");
        if (reps < 1 || reps > 144) throw new IllegalArgumentException("Invalid reps");
        if ((long) selected.size() * reps > 200000) throw new IllegalArgumentException("Too many questions");
        ArrayList<Long> base = new ArrayList<>(selected);
        ArrayList<Long> result = new ArrayList<>(base.size() * reps);
        for (int i = 0; i < reps; i++) {
            Collections.shuffle(base, random);
            result.addAll(base);
        }
        return result;
    }

    public static DozenMath.Problem problem(int op, long pair) {
        int a = firstOperand(pair), b = secondOperand(pair);
        String expression = expression(op, a, b);
        long result = answer(op, a, b);
        return new DozenMath.Problem("Custom " + OPERATIONS[op], expression, result,
            expression + " = " + DozenMath.format(result));
    }
}
