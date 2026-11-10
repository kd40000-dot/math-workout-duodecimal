package com.sankhyantra.mathstricks.duo;

import android.content.res.Resources;
import android.view.View;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import l5.x;

/**
 * Patch helper for the user's original Math Tricks Workout v2.8.5 APK.
 * Existing workout generators and results remain numeric internally.
 * Only the presentation and input boundary is converted to base twelve.
 */
public final class Dozenal implements View.OnClickListener {
    private static final String DIGITS = "0123456789↊↋";
    private static final Pattern NUMBER = Pattern.compile("[0-9]+(?:[.,][0-9]+)?");
    private final x fragment;
    private final String digit;

    private Dozenal(x fragment, String digit) {
        this.fragment = fragment;
        this.digit = digit;
    }

    @Override public void onClick(View view) {
        fragment.b3(digit);
    }

    public static void attach(x fragment, View root) {
        if (root == null) return;
        Resources r = root.getResources();
        String pkg = root.getContext().getPackageName();
        attachDigit(fragment, root, r.getIdentifier("duo_ten", "id", pkg), "↊");
        attachDigit(fragment, root, r.getIdentifier("duo_eleven", "id", pkg), "↋");
    }

    private static void attachDigit(x fragment, View root, int id, String digit) {
        if (id == 0) return;
        View key = root.findViewById(id);
        if (key != null) key.setOnClickListener(new Dozenal(fragment, digit));
    }

    public static String integer(int value) {
        return Integer.toString(value, 12).replace('a','↊').replace('b','↋');
    }

    public static String format(double value) {
        if (!Double.isFinite(value)) return Double.toString(value);
        if (value == 0.0) return "0";
        boolean neg = value < 0;
        double abs = Math.abs(value);
        long whole = (long) Math.floor(abs);
        int frac = (int) Math.round((abs - whole) * 144.0);
        if (frac >= 144) { whole++; frac = 0; }
        String start = Long.toString(whole, 12).replace('a','↊').replace('b','↋');
        if (frac == 0) return (neg ? "-" : "") + start;
        int first = frac / 12, second = frac % 12;
        String tail = second == 0 ? String.valueOf(DIGITS.charAt(first))
            : "" + DIGITS.charAt(first) + DIGITS.charAt(second);
        return (neg ? "-" : "") + start + "." + tail;
    }

    public static String expression(String input) {
        if (input == null) return "";
        Matcher m = NUMBER.matcher(input);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String token = m.group();
            String replacement;
            try {
                replacement = format(Double.parseDouble(token.replace(',', '.')));
            } catch (NumberFormatException e) {
                replacement = token;
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    public static int parseInt(String value) {
        String normalized = value.trim().replace('↊','a').replace('↋','b');
        return Integer.parseInt(normalized, 12);
    }

    public static boolean matches(double expected, String response) {
        if (response == null || response.trim().isEmpty()) return false;
        String text = response.trim().replace(',', '.').toLowerCase(Locale.ROOT)
            .replace('↊','a').replace('↋','b');
        boolean negative = text.startsWith("-");
        if (negative) text = text.substring(1);
        String[] parts = text.split("\\.", -1);
        if (parts.length > 2 || parts[0].isEmpty() ||
            (parts.length == 2 && parts[1].isEmpty())) return false;
        double actual = 0;
        for (int i = 0; i < parts[0].length(); i++) {
            int d = Character.digit(parts[0].charAt(i), 12);
            if (d < 0) return false;
            actual = actual * 12 + d;
        }
        if (parts.length == 2) {
            double factor = 1.0 / 12.0;
            for (int i=0; i<parts[1].length(); i++) {
                int d = Character.digit(parts[1].charAt(i), 12);
                if (d < 0) return false;
                actual += d * factor;
                factor /= 12.0;
            }
        }
        if (negative) actual = -actual;
        // The original exercise engine rounds decimal answers. Base-12
        // presentation uses at most two fractional digits: allow half a unit.
        return Double.isFinite(actual) && Math.abs(actual - expected) <= (1.0/288.0 + 1e-8);
    }
}
