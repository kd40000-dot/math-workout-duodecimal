package com.duodecimal.workout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The visual arithmetic is the Chinese remainder theorem:
 * cap=(a+b) mod 3, body=(a+b) mod 4, carry=floor((a+b)/12).
 *
 * Calculations use integers. Only rendering uses the Draft A glyphs.
 */
public final class VisualAddition {
    private VisualAddition() {}

    public static final int CAP = 0;
    public static final int BODY = 1;
    public static final int COMBINE = 2;
    public static final int CARRY = 3;
    public static final int FULL = 4;
    public static final int MODE_COUNT = 5;

    public static final int PART_CAP = 0;
    public static final int PART_BODY = 1;
    public static final int PART_CARRY = 2;
    public static final String[] MODE_NAMES = {
        "Caps · mod 3", "Bodies · mod 4", "Combine · no carry",
        "Carry recognition", "Full visual addition"
    };
    public static final String[] CAP_NAMES = {"Bar", "Breve", "Dot"};
    public static final String[] BODY_NAMES = {"Loop", "Stem", "Vee", "El"};

    public static int[] stages(int mode) {
        switch (mode) {
            case CAP: return new int[]{PART_CAP};
            case BODY: return new int[]{PART_BODY};
            case COMBINE: return new int[]{PART_CAP, PART_BODY};
            case CARRY: return new int[]{PART_CARRY};
            case FULL: return new int[]{PART_CAP, PART_BODY, PART_CARRY};
            default: throw new IllegalArgumentException("Invalid drill");
        }
    }

    public static final class Fact {
        public final int a;
        public final int b;
        public final int sum;
        public final int digit;
        public final int cap;
        public final int body;
        public final int carry;

        public Fact(int a, int b) {
            if (a < 0 || a >= 12 || b < 0 || b >= 12)
                throw new IllegalArgumentException("Only single-digit base-12 addends");
            this.a = a;
            this.b = b;
            this.sum = a + b;
            this.digit = sum % 12;
            this.cap = (a % 3 + b % 3) % 3;
            this.body = (a % 4 + b % 4) % 4;
            this.carry = sum / 12;
        }

        public int expected(int stage) {
            switch (stage) {
                case PART_CAP: return cap;
                case PART_BODY: return body;
                case PART_CARRY: return carry;
                default: throw new IllegalArgumentException("Invalid stage");
            }
        }

        public String expression() {
            return DozenMath.format(a) + " + " + DozenMath.format(b);
        }

        public String equation() {
            return expression() + " = " + DozenMath.format(sum);
        }

        public String explanation(int stage) {
            if (stage == PART_CAP) {
                return CAP_NAMES[a % 3] + " + " + CAP_NAMES[b % 3]
                    + " = " + CAP_NAMES[cap] + " (mod 3).";
            }
            if (stage == PART_BODY) {
                return BODY_NAMES[a % 4] + " + " + BODY_NAMES[b % 4]
                    + " = " + BODY_NAMES[body] + " (mod 4).";
            }
            if (stage == PART_CARRY) {
                return sum >= 12 ? "The sum reaches 10₁₂: carry 1."
                    : "The sum stays below 10₁₂: no carry.";
            }
            throw new IllegalArgumentException("Invalid stage");
        }
    }

    /**
     * Balanced learning decks: the cap drill presents each of 3×3 cap
     * combinations once per pass; the body drill each of 4×4 once.
     * All other drills cover every eligible ordered single-digit pair.
     */
    public static List<Fact> deck(int mode, Random random) {
        if (random == null) throw new IllegalArgumentException("Random required");
        ArrayList<Fact> facts = new ArrayList<>();
        if (mode == CAP) {
            for (int first = 0; first < 3; first++)
                for (int second = 0; second < 3; second++)
                    facts.add(new Fact(first + 3 * random.nextInt(4),
                                       second + 3 * random.nextInt(4)));
        } else if (mode == BODY) {
            for (int first = 0; first < 4; first++)
                for (int second = 0; second < 4; second++)
                    facts.add(new Fact(first + 4 * random.nextInt(3),
                                       second + 4 * random.nextInt(3)));
        } else if (mode == COMBINE || mode == CARRY || mode == FULL) {
            for (int a = 0; a < 12; a++)
                for (int b = 0; b < 12; b++) {
                    boolean producesCarry = a + b >= 12;
                    if (mode == COMBINE && producesCarry) continue;
                    // Carry drill intentionally includes both yes/no answers.
                    facts.add(new Fact(a, b));
                }
        } else {
            throw new IllegalArgumentException("Invalid drill");
        }
        Collections.shuffle(facts, random);
        return facts;
    }

    /** Fixed-size session, recycling complete shuffled decks as required. */
    public static List<Fact> session(int mode, int questions, Random random) {
        if (questions < 1 || questions > 500)
            throw new IllegalArgumentException("Invalid session length");
        ArrayList<Fact> result = new ArrayList<>(questions);
        while (result.size() < questions) {
            List<Fact> next = deck(mode, random);
            for (Fact fact : next) {
                if (result.size() == questions) break;
                result.add(fact);
            }
        }
        return result;
    }

    /** Independent of the actual visual geometry: inverse of both residues. */
    public static int digitFor(int cap, int body) {
        if (cap < 0 || cap > 2 || body < 0 || body > 3)
            throw new IllegalArgumentException("Invalid component");
        for (int digit = 0; digit < 12; digit++)
            if (digit % 3 == cap && digit % 4 == body) return digit;
        throw new AssertionError("CRT mapping is complete");
    }
}
