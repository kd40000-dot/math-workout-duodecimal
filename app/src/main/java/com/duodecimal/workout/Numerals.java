package com.duodecimal.workout;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.widget.Button;
import android.widget.TextView;
import android.content.Context;

/**
 * Exact Draft A modular numerals for the entire standalone workout app.
 *
 * Each digit d uses:
 *    main body = d % 4
 *    upper cap = d % 3
 *
 * Body 0: rounded square, body 1: short-crossbar ⊺, body 2: V,
 * body 3: U+218B duodecimal EL (↋), in REGULAR weight.
 * Caps 0/1/2 are the flat bar, Vietnamese breve (ă), and dot.
 *
 * The el outline below is a normalized outline of the Unicode U+218B
 * glyph from the regular-weight Andika design, NOT an approximation
 * or a font file. The remaining shapes are original stroked vectors.
 *
 * No font installation is needed. The underlying text stays as 0-9↊↋
 * for TalkBack, text extraction and arithmetic, but is drawn with
 * the modular glyphs consistently on every Android version.
 */
public final class Numerals {
    private Numerals() {}

    private static int digit(char ch) { return ModularDigits.valueOf(ch); }

    public static CharSequence render(CharSequence text) {
        if (text == null) return "";
        SpannableStringBuilder result = new SpannableStringBuilder(text.toString());
        for (int i = 0; i < result.length(); i++) {
            int value = digit(result.charAt(i));
            if (value >= 0) {
                result.setSpan(new ModularDigitSpan(value), i, i + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return result;
    }

    /*
     * Numeric-only text is painted directly in the view's Canvas rather than
     * using ReplacementSpan. Android versions / modified system fonts can
     * measure or clip ReplacementSpan runs in short single-line controls,
     * leaving keypad keys and grid cells visually empty. Direct drawing
     * gives all twelve symbols a predictable, measurable painted area.
     */
    private static boolean isNumeralString(CharSequence text) {
        if (text == null || text.length() == 0) return false;
        boolean foundDigit = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (digit(ch) >= 0) { foundDigit = true; continue; }
            if (i == 0 && (ch == '−' || ch == '-')) continue;
            return false;
        }
        return foundDigit;
    }

    private static void drawNumericText(Canvas canvas, TextView view) {
        CharSequence text = view.getText();
        Paint paint = view.getPaint();
        float size = paint.getTextSize();
        float glyphWidth = size * 0.69f;
        float totalWidth = text.length() * glyphWidth;
        float availableWidth = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
        float startX = view.getPaddingLeft() + (availableWidth - totalWidth) * 0.5f;
        // The glyph paths occupy y=12..105 of a 112-unit design grid.
        // Place their visible outlines in the center of the content rectangle.
        float contentHeight = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
        float top = view.getPaddingTop() + (contentHeight - size * (93f / 112f)) * 0.5f;
        float baseY = top + size * (106f / 112f);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int value = digit(ch);
            if (value >= 0) {
                drawDigit(canvas, value, startX + i * glyphWidth, baseY, size, paint.getColor());
            } else {
                canvas.drawText(String.valueOf(ch), startX + i * glyphWidth, baseY, paint);
            }
        }
    }

    public static final class NumeralTextView extends TextView {
        public NumeralTextView(Context context) { super(context); }
        @Override public void setText(CharSequence text, BufferType type) {
            super.setText(isNumeralString(text) ? text : Numerals.render(text), type);
        }
        @Override protected void onDraw(Canvas canvas) {
            if (isNumeralString(getText())) drawNumericText(canvas, this);
            else super.onDraw(canvas);
        }
    }

    public static final class NumeralButton extends Button {
        public NumeralButton(Context context) { super(context); }
        @Override public void setText(CharSequence text, BufferType type) {
            super.setText(isNumeralString(text) ? text : Numerals.render(text), type);
        }
        @Override protected void onDraw(Canvas canvas) {
            if (isNumeralString(getText())) drawNumericText(canvas, this);
            else super.onDraw(canvas);
        }
    }

    private static final Path EL_REGULAR = makeEl();
    private static final Path[] BODY_PATHS = makeBodies();
    private static final Path[] CAP_PATHS = makeCaps();

    private static Path[] makeBodies() {
        Path[] bodies = new Path[4];
        Path loop = new Path();
        loop.moveTo(39f, 41f);
        loop.lineTo(61f, 41f);
        loop.quadTo(73f, 41f, 73f, 55f);
        loop.lineTo(73f, 91f);
        loop.quadTo(73f, 105f, 61f, 105f);
        loop.lineTo(39f, 105f);
        loop.quadTo(27f, 105f, 27f, 91f);
        loop.lineTo(27f, 55f);
        loop.quadTo(27f, 41f, 39f, 41f);
        loop.close();
        bodies[0] = loop;

        Path shortT = new Path();
        shortT.moveTo(40f, 43f);
        shortT.lineTo(60f, 43f);
        shortT.moveTo(50f, 43f);
        shortT.lineTo(50f, 104f);
        bodies[1] = shortT;

        Path vee = new Path();
        vee.moveTo(24f, 43f);
        vee.lineTo(42f, 93f);
        vee.quadTo(50f, 114f, 58f, 93f);
        vee.lineTo(76f, 43f);
        bodies[2] = vee;

        bodies[3] = EL_REGULAR;
        return bodies;
    }

    private static Path[] makeCaps() {
        Path[] caps = new Path[3];

        Path flat = new Path();
        flat.moveTo(27f, 18f);
        flat.lineTo(73f, 18f);
        caps[0] = flat;

        Path breve = new Path();
        breve.moveTo(32f, 12f);
        breve.quadTo(50f, 33f, 68f, 12f);
        caps[1] = breve;

        Path dot = new Path();
        dot.addCircle(50f, 20f, 5.4f, Path.Direction.CW);
        caps[2] = dot;

        return caps;
    }

    /**
     * Genuine Unicode duodecimal EL outline in REGULAR weight, normalized
     * onto the same 100x112 design grid as the three other bodies.
     */
    private static Path makeEl() {
        Path p = new Path();
        p.moveTo(51.02f, 105.00f);
        p.quadTo(44.69f, 105.00f, 39.36f, 103.24f);
        p.quadTo(34.03f, 101.48f, 30.79f, 98.20f);
        p.quadTo(27.55f, 94.91f, 27.55f, 90.38f);
        p.quadTo(27.55f, 86.48f, 30.13f, 83.47f);
        p.quadTo(32.70f, 80.46f, 36.48f, 78.51f);
        p.quadTo(40.26f, 76.56f, 43.88f, 75.97f);
        p.quadTo(41.17f, 75.59f, 37.91f, 74.50f);
        p.quadTo(34.64f, 73.42f, 31.71f, 71.45f);
        p.quadTo(28.78f, 69.48f, 26.89f, 66.47f);
        p.quadTo(25.00f, 63.46f, 25.00f, 59.23f);
        p.quadTo(25.00f, 55.50f, 27.24f, 52.19f);
        p.quadTo(29.49f, 48.88f, 33.37f, 46.36f);
        p.quadTo(37.24f, 43.84f, 42.27f, 42.42f);
        p.quadTo(47.30f, 41.00f, 52.81f, 41.00f);
        p.quadTo(59.59f, 41.00f, 65.28f, 42.93f);
        p.quadTo(70.97f, 44.86f, 75.00f, 47.99f);
        p.lineTo(73.72f, 57.11f);
        p.quadTo(69.54f, 53.21f, 64.11f, 50.81f);
        p.quadTo(58.67f, 48.42f, 51.79f, 48.42f);
        p.quadTo(47.91f, 48.42f, 44.01f, 49.71f);
        p.quadTo(40.10f, 51.00f, 37.53f, 53.59f);
        p.quadTo(34.95f, 56.17f, 34.95f, 60.07f);
        p.quadTo(34.95f, 63.51f, 37.73f, 66.13f);
        p.quadTo(40.51f, 68.76f, 45.31f, 70.25f);
        p.quadTo(50.10f, 71.73f, 56.12f, 71.73f);
        p.lineTo(59.69f, 71.73f);
        p.lineTo(59.69f, 78.93f);
        p.lineTo(56.12f, 78.93f);
        p.quadTo(51.07f, 78.93f, 46.76f, 80.44f);
        p.quadTo(42.45f, 81.94f, 39.85f, 84.32f);
        p.quadTo(37.24f, 86.69f, 37.24f, 89.32f);
        p.quadTo(37.24f, 92.16f, 39.29f, 94.00f);
        p.quadTo(41.33f, 95.85f, 44.39f, 96.71f);
        p.quadTo(47.45f, 97.58f, 50.51f, 97.58f);
        p.quadTo(56.43f, 97.58f, 60.87f, 96.14f);
        p.quadTo(65.31f, 94.70f, 69.64f, 91.01f);
        p.lineTo(71.43f, 98.22f);
        p.quadTo(68.37f, 101.10f, 62.88f, 103.05f);
        p.quadTo(57.40f, 105.00f, 51.02f, 105.00f);
        p.close();
        return p;
    }

    private static final class ModularDigitSpan extends ReplacementSpan {
        private final int value;
        ModularDigitSpan(int value) { this.value = value; }

        @Override public int getSize(Paint paint, CharSequence text, int start, int end,
                Paint.FontMetricsInt fm) {
            return Math.max(1, Math.round(paint.getTextSize() * 0.69f));
        }

        @Override public void draw(Canvas canvas, CharSequence text, int start, int end,
                float x, int top, int y, int bottom, Paint paint) {
            drawDigit(canvas, value, x, y, paint.getTextSize(), paint.getColor());
        }
    }

    private static void drawDigit(Canvas canvas, int value, float x, float y,
                                  float size, int color) {
            float scale = size / 112f;
            float width = size * 0.69f;
            canvas.save();
            canvas.translate(x + width * 0.5f - 50f * scale, y - 106f * scale);
            canvas.scale(scale, scale);

            Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            glyphPaint.setColor(color);
            glyphPaint.setStrokeCap(Paint.Cap.ROUND);
            glyphPaint.setStrokeJoin(Paint.Join.ROUND);
            glyphPaint.setStrokeWidth(6.6f);
            glyphPaint.setStyle(ModularDigits.body(value) == 3 ? Paint.Style.FILL : Paint.Style.STROKE);
            canvas.drawPath(BODY_PATHS[ModularDigits.body(value)], glyphPaint);

            glyphPaint.setStyle(ModularDigits.cap(value) == 2 ? Paint.Style.FILL : Paint.Style.STROKE);
            glyphPaint.setStrokeWidth(7f);
            canvas.drawPath(CAP_PATHS[ModularDigits.cap(value)], glyphPaint);

            canvas.restore();
    }
}
