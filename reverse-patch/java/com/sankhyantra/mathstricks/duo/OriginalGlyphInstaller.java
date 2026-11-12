package com.sankhyantra.mathstricks.duo;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

import java.util.WeakHashMap;

/**
 * Installs Draft A body=(digit mod 4), cap=(digit mod 3) glyphs across
 * the existing Math Tricks Workout activities without changing values or
 * its answer checking logic.
 *
 * The app's original Application calls install(...) from onCreate().
 * All state is per-process and views are held weakly.
 */
public final class OriginalGlyphInstaller {
    private static boolean registered;
    private static final WeakHashMap<View, Boolean> watchedRoots = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Boolean> watchedTexts = new WeakHashMap<>();
    private static final WeakHashMap<TextView, CharSequence> lastRendered = new WeakHashMap<>();
    private static final WeakHashMap<TextView, String> lastContent = new WeakHashMap<>();
    private static final WeakHashMap<TextView, Boolean> updating = new WeakHashMap<>();

    private OriginalGlyphInstaller() {}

    public static void install(Application app) {
        if (registered) return;
        registered = true;
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle state) { attach(a); }
            @Override public void onActivityStarted(Activity a) { attach(a); }
            @Override public void onActivityResumed(Activity a) { attach(a); }
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle out) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }

    private static void attach(Activity activity) {
        if (activity == null) return;
        View decor = activity.getWindow().getDecorView();
        watch(decor);
    }

    /**
     * Also callable directly for custom views and dialog roots.
     * A global layout hook discovers views added by fragments after activity startup.
     */
    public static void watch(View root) {
        if (root == null) return;
        if (!watchedRoots.containsKey(root)) {
            watchedRoots.put(root, Boolean.TRUE);
            root.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> scan(root));
        }
        scan(root);
    }

    private static void scan(View view) {
        if (view == null) return;
        if (view instanceof TextView) {
            observe((TextView) view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int i=0;i<count;i++) scan(group.getChildAt(i));
        }
    }

    private static void observe(TextView textView) {
        if (!watchedTexts.containsKey(textView)) {
            watchedTexts.put(textView, Boolean.TRUE);
            textView.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable editable) {
                    // SetText invokes this after its new text has been installed.
                    // Edits preserve underlying chars; only presentation spans change.
                    transform(textView);
                }
            });
        }
        transform(textView);
    }

    private static boolean isNumeral(char c) {
        return (c >= '0' && c <= '9') || c == '↊' || c == '↋';
    }

    private static void transform(TextView view) {
        if (updating.containsKey(view)) return;
        CharSequence value = view.getText();
        if (value == null || value.length() == 0) return;
        String text = value.toString();

        // Avoid touching nonnumeric labels and unchanged text/spans.
        if (lastRendered.get(view) == value && text.equals(lastContent.get(view))) return;
        boolean found = false;
        for (int i=0;i<text.length();i++) if (isNumeral(text.charAt(i))) {
            found = true;
            break;
        }
        if (!found) {
            lastRendered.put(view, value);
            lastContent.put(view, text);
            return;
        }

        updating.put(view, Boolean.TRUE);
        try {
            if (view instanceof EditText && value instanceof Editable) {
                // Do not reset an editor or disturb cursor/selection/composition.
                Editable editable = (Editable)value;
                ReplacementSpan[] old = editable.getSpans(0, editable.length(), ReplacementSpan.class);
                for (ReplacementSpan span: old) {
                    if (span.getClass().getName().contains("Numerals$ModularDigitSpan"))
                        editable.removeSpan(span);
                }
                CharSequence converted = Numerals.render(text);
                if (converted instanceof Spanned) {
                    Spanned formatted = (Spanned)converted;
                    ReplacementSpan[] spans = formatted.getSpans(0, formatted.length(), ReplacementSpan.class);
                    for (ReplacementSpan span: spans) {
                        editable.setSpan(span, formatted.getSpanStart(span), formatted.getSpanEnd(span),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else {
                // Keep any preexisting rich-text spans while adding glyph drawings.
                SpannableStringBuilder formatted = new SpannableStringBuilder(value);
                CharSequence replacement = Numerals.render(text);
                if (replacement instanceof Spanned) {
                    Spanned spans = (Spanned)replacement;
                    ReplacementSpan[] digitSpans = spans.getSpans(0, spans.length(), ReplacementSpan.class);
                    for (ReplacementSpan span: digitSpans) {
                        formatted.setSpan(span, spans.getSpanStart(span), spans.getSpanEnd(span),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                view.setText(formatted, TextView.BufferType.SPANNABLE);
            }
            lastRendered.put(view, view.getText());
            lastContent.put(view, text);
        } finally {
            updating.remove(view);
        }
    }
}
