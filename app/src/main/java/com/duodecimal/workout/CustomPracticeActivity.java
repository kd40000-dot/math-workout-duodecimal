package com.duodecimal.workout;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

/**
 * Drill an explicitly selected subset of the base-twelve operation table.
 * Rows represent the first operand; columns represent the second.
 *
 * There are 12 entries per axis page. One-digit tables therefore show 12x12
 * results (0 through ↋). Larger digit lengths page through consecutive dozen
 * values on each axis, with sliders for jumping to any page.
 */
public final class CustomPracticeActivity extends Activity {
    private static final int BG = 0xFF282A36, SURFACE = 0xFF44475A, FG = 0xFFF8F8F2;
    private static final int PURPLE = 0xFFBD93F9, CYAN = 0xFF8BE9FD, GREEN = 0xFF50FA7B;
    private static final int RED = 0xFFFF5555, MUTED = 0xFFB5B8C6, HEADER = 0xFF3A3D4D;

    private SharedPreferences prefs;
    private ScrollView scroll;
    private LinearLayout content;
    private int operation, digitA, digitB, rowPage, colPage, repsIndex;
    private LinkedHashSet<Long> selected = new LinkedHashSet<>();
    private TextView selectedLabel;
    private Button startButton;
    private List<Long> session;
    private int sessionIndex, sessionCorrect, sessionAttempted;
    private boolean answered;
    private final StringBuilder typed = new StringBuilder();
    private TextView progress, question, response, feedback;
    private Button checkButton;
    private final Random random = new Random();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("duodecimal_workout", MODE_PRIVATE);
        operation = clamp(prefs.getInt("custom_op", 0), 0, 3);
        digitA = clamp(prefs.getInt("custom_digits_a", 1), 1, 4);
        digitB = clamp(prefs.getInt("custom_digits_b", 1), 1, 4);
        repsIndex = clamp(prefs.getInt("custom_reps_index", 3), 0, PracticeGrid.REP_CHOICES.length - 1);
        loadSelected();
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        content = vertical();
        content.setPadding(dp(14), dp(18), dp(14), dp(30));
        scroll.addView(content);
        setContentView(scroll);
        showConfiguration();
    }

    private static int clamp(int n, int min, int max) {
        return Math.max(min, Math.min(n, max));
    }

    private void saveConfig() {
        prefs.edit().putInt("custom_op", operation).putInt("custom_digits_a", digitA)
            .putInt("custom_digits_b", digitB).putInt("custom_reps_index", repsIndex).apply();
    }

    private void saveSelected() {
        prefs.edit().putString(PracticeGrid.profileKey(operation, digitA, digitB),
            PracticeGrid.serialize(selected)).apply();
    }

    private void loadSelected() {
        selected = PracticeGrid.deserialize(
            prefs.getString(PracticeGrid.profileKey(operation, digitA, digitB), ""),
            operation, digitA, digitB);
    }

    private void changeProfile(int newOperation, int newA, int newB) {
        if (operation == newOperation && digitA == newA && digitB == newB) return;
        saveSelected();
        operation = newOperation;
        digitA = newA;
        digitB = newB;
        rowPage = 0;
        colPage = 0;
        loadSelected();
        saveConfig();
        showConfiguration();
    }

    private void showConfiguration() {
        session = null;
        content.removeAllViews();
        header("Custom Practice", "Choose exact facts to practice. Tap table cells to include or exclude them.");
        content.addView(label("OPERATION", 12, MUTED, true));
        Spinner ops = spinner(PracticeGrid.OPERATIONS, operation);
        content.addView(ops, new LinearLayout.LayoutParams(-1, dp(48)));
        ops.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                if (index != operation) changeProfile(index, digitA, digitB);
            }
        });
        addSpace(12);
        content.addView(label("OPERAND LENGTHS", 12, MUTED, true));
        content.addView(label("Rows = first operand · Columns = second operand", 12, MUTED, false));
        LinearLayout sizes = horizontal();
        content.addView(sizes, new LinearLayout.LayoutParams(-1, dp(50)));
        Spinner left = spinner(PracticeGrid.DIGIT_CHOICES, digitA - 1);
        Spinner right = spinner(PracticeGrid.DIGIT_CHOICES, digitB - 1);
        LinearLayout.LayoutParams half1 = new LinearLayout.LayoutParams(0, -1, 1);
        half1.setMargins(0, 0, dp(6), 0);
        sizes.addView(left, half1);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, -1, 1);
        half2.setMargins(dp(6), 0, 0, 0);
        sizes.addView(right, half2);
        left.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                if (digitA != index + 1) changeProfile(operation, index + 1, digitB);
            }
        });
        right.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                if (digitB != index + 1) changeProfile(operation, digitA, index + 1);
            }
        });
        addSpace(14);

        content.addView(label("REPETITIONS PER SELECTED FACT", 12, MUTED, true));
        String[] repLabels = new String[PracticeGrid.REP_CHOICES.length];
        for (int i = 0; i < repLabels.length; i++)
            repLabels[i] = DozenMath.format(PracticeGrid.REP_CHOICES[i]) + " reps";
        Spinner repSpinner = spinner(repLabels, repsIndex);
        content.addView(repSpinner, new LinearLayout.LayoutParams(-1, dp(48)));
        repSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                if (repsIndex != index) {
                    repsIndex = index;
                    saveConfig();
                    updateSelectionCount();
                }
            }
        });
        addSpace(13);

        content.addView(label("SELECT FACTS", 17, CYAN, true));
        content.addView(label("Each cell shows its result. Purple = selected. Ordered pairs are distinct: 3 + 5 and 5 + 3 can be selected separately.", 12, MUTED, false));
        addSpace(8);

        axisPager("Rows · first operand", digitA, true);
        axisPager("Columns · second operand", digitB, false);
        addSpace(8);
        addTable();
        addSpace(12);

        LinearLayout tools = horizontal();
        content.addView(tools, new LinearLayout.LayoutParams(-1, dp(48)));
        Button all = button("Select page", SURFACE, FG, 13);
        Button clearPage = button("Clear page", SURFACE, FG, 13);
        Button clearAll = button("Clear all", SURFACE, RED, 13);
        addWeighted(tools, all, 1);
        addWeighted(tools, clearPage, 1);
        addWeighted(tools, clearAll, 1);
        all.setOnClickListener(v -> changeVisible(true, false));
        clearPage.setOnClickListener(v -> changeVisible(false, false));
        clearAll.setOnClickListener(v -> changeVisible(false, true));
        addSpace(12);

        selectedLabel = label("", 14, CYAN, true);
        selectedLabel.setGravity(Gravity.CENTER);
        content.addView(selectedLabel);
        addSpace(8);
        startButton = button("START PRACTICE", PURPLE, BG, 17);
        content.addView(startButton, new LinearLayout.LayoutParams(-1, dp(56)));
        startButton.setOnClickListener(v -> startPractice());
        addSpace(12);
        content.addView(label("Your selected facts are saved separately for each operation and operand-length combination. All table entries and session counts use base 12.", 12, MUTED, false));
        updateSelectionCount();
        scrollToTop();
    }

    private void updateSelectionCount() {
        if (selectedLabel == null || startButton == null) return;
        int count = selected.size(), reps = PracticeGrid.REP_CHOICES[repsIndex];
        selectedLabel.setText(DozenMath.format(count) + " facts × " + DozenMath.format(reps)
            + " reps = " + DozenMath.format((long) count * reps) + " questions");
        startButton.setEnabled(count > 0);
        startButton.setAlpha(count > 0 ? 1f : 0.5f);
    }

    private void changeVisible(boolean include, boolean clearEverything) {
        if (clearEverything) {
            selected.clear();
        } else {
            int firstA = PracticeGrid.firstForPage(digitA, rowPage);
            int firstB = PracticeGrid.firstForPage(digitB, colPage);
            for (int a = firstA; a < firstA + 12; a++)
                for (int b = firstB; b < firstB + 12; b++) {
                    long key = PracticeGrid.key(a, b);
                    if (include && PracticeGrid.valid(operation, digitA, digitB, a, b))
                        selected.add(key);
                    else if (!include) selected.remove(key);
                }
        }
        saveSelected();
        showConfiguration();
    }

    private void axisPager(String title, int digits, boolean row) {
        int page = row ? rowPage : colPage;
        int count = PracticeGrid.pageCount(digits);
        int start = PracticeGrid.firstForPage(digits, page);
        int end = start + 11;
        LinearLayout line = horizontal();
        line.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(line, new LinearLayout.LayoutParams(-1, dp(44)));
        Button prev = button("‹", SURFACE, FG, 23);
        Button next = button("›", SURFACE, FG, 23);
        TextView caption = label(title + ": " + DozenMath.format(start) + "–" + DozenMath.format(end), 13, FG, true);
        caption.setGravity(Gravity.CENTER);
        line.addView(prev, new LinearLayout.LayoutParams(dp(44), dp(40)));
        line.addView(caption, new LinearLayout.LayoutParams(0, -1, 1));
        line.addView(next, new LinearLayout.LayoutParams(dp(44), dp(40)));
        prev.setEnabled(page > 0);
        next.setEnabled(page + 1 < count);
        prev.setAlpha(page > 0 ? 1 : 0.35f);
        next.setAlpha(page + 1 < count ? 1 : 0.35f);
        prev.setOnClickListener(v -> { if (row) rowPage--; else colPage--; showConfiguration(); });
        next.setOnClickListener(v -> { if (row) rowPage++; else colPage++; showConfiguration(); });

        if (count > 1) {
            LinearLayout seeker = horizontal();
            seeker.setGravity(Gravity.CENTER_VERTICAL);
            content.addView(seeker, new LinearLayout.LayoutParams(-1, dp(34)));
            SeekBar bar = new SeekBar(this);
            bar.setMax(count - 1);
            bar.setProgress(page);
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
            bar.setThumbTintList(android.content.res.ColorStateList.valueOf(PURPLE));
            seeker.addView(bar, new LinearLayout.LayoutParams(0, -1, 1));
            TextView pages = label(DozenMath.format(page + 1) + "/" + DozenMath.format(count), 12, MUTED, false);
            pages.setGravity(Gravity.CENTER);
            seeker.addView(pages, new LinearLayout.LayoutParams(dp(70), -1));
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStartTrackingTouch(SeekBar b) {}
                @Override public void onProgressChanged(SeekBar b, int position, boolean user) {
                    if (user) {
                        int begin = PracticeGrid.firstForPage(digits, position);
                        caption.setText(title + ": " + DozenMath.format(begin) + "–" + DozenMath.format(begin + 11));
                        pages.setText(DozenMath.format(position + 1) + "/" + DozenMath.format(count));
                    }
                }
                @Override public void onStopTrackingTouch(SeekBar b) {
                    if (row) rowPage = b.getProgress(); else colPage = b.getProgress();
                    showConfiguration();
                }
            });
        }
    }

    private void addTable() {
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(true);
        horizontal.setFillViewport(false);
        content.addView(horizontal, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout grid = vertical();
        horizontal.addView(grid);
        int beginA = PracticeGrid.firstForPage(digitA, rowPage);
        int beginB = PracticeGrid.firstForPage(digitB, colPage);
        LinearLayout headings = horizontal();
        grid.addView(headings);
        cell(headings, PracticeGrid.SYMBOLS[operation], HEADER, CYAN, true, null, 58);
        for (int j = 0; j < 12; j++)
            cell(headings, DozenMath.format(beginB + j), HEADER, CYAN, true, null, 58);

        for (int i = 0; i < 12; i++) {
            final int a = beginA + i;
            LinearLayout line = horizontal();
            grid.addView(line);
            cell(line, DozenMath.format(a), HEADER, CYAN, true, null, 58);
            for (int j = 0; j < 12; j++) {
                final int b = beginB + j;
                boolean valid = PracticeGrid.valid(operation, digitA, digitB, a, b);
                long key = PracticeGrid.key(a, b);
                String value = valid ? DozenMath.format(PracticeGrid.answer(operation, a, b)) : "—";
                boolean chosen = selected.contains(key);
                TextView item = cell(line, value, chosen ? PURPLE : SURFACE,
                    valid ? (chosen ? BG : FG) : MUTED, chosen, null, 58);
                if (valid) {
                    item.setContentDescription(PracticeGrid.expression(operation, a, b)
                        + " equals " + value + (chosen ? ". Selected" : ". Not selected"));
                    item.setOnClickListener(v -> {
                        if (!selected.add(key)) selected.remove(key);
                        boolean nowSelected = selected.contains(key);
                        setCellAppearance(item, nowSelected ? PURPLE : SURFACE,
                            nowSelected ? BG : FG, nowSelected);
                        item.setContentDescription(PracticeGrid.expression(operation, a, b)
                            + " equals " + value + (nowSelected ? ". Selected" : ". Not selected"));
                        saveSelected();
                        updateSelectionCount();
                    });
                } else {
                    item.setAlpha(0.40f);
                    item.setContentDescription("Not an exact division");
                }
            }
        }
    }

    private void startPractice() {
        if (selected.isEmpty()) return;
        saveSelected();
        session = PracticeGrid.makeSession(selected, PracticeGrid.REP_CHOICES[repsIndex], random);
        sessionIndex = sessionAttempted = sessionCorrect = 0;
        answered = false;
        showQuestion();
    }

    private void showQuestion() {
        if (session == null || sessionIndex >= session.size()) {
            showFinished();
            return;
        }
        answered = false;
        typed.setLength(0);
        content.removeAllViews();
        header("Practice", PracticeGrid.OPERATIONS[operation]
            + " · " + DozenMath.format(selected.size()) + " selected facts");
        progress = label("", 15, CYAN, true);
        progress.setGravity(Gravity.CENTER);
        content.addView(progress);
        addSpace(14);
        LinearLayout card = vertical();
        card.setPadding(dp(12), dp(25), dp(12), dp(20));
        card.setBackground(round(SURFACE, 15));
        content.addView(card, new LinearLayout.LayoutParams(-1, -2));
        TextView eyebrow = label("SOLVE IN BASE TWELVE", 13, CYAN, true);
        eyebrow.setGravity(Gravity.CENTER);
        card.addView(eyebrow);
        addSpaceIn(card, 18);
        DozenMath.Problem p = PracticeGrid.problem(operation, session.get(sessionIndex));
        question = label(p.prompt, 31, FG, true);
        question.setMinHeight(dp(78));
        question.setGravity(Gravity.CENTER);
        card.addView(question);
        response = label("_", 31, GREEN, true);
        response.setGravity(Gravity.CENTER);
        response.setMinHeight(dp(60));
        card.addView(response);
        feedback = label("", 15, GREEN, false);
        feedback.setGravity(Gravity.CENTER);
        feedback.setMinHeight(dp(58));
        card.addView(feedback);
        addSpace(20);
        String[] digits = {"1","2","3","4","5","6","7","8","9","↊","↋","0"};
        for (int r = 0; r < 3; r++) {
            LinearLayout row = horizontal();
            content.addView(row, new LinearLayout.LayoutParams(-1, dp(66)));
            for (int c = 0; c < 4; c++) {
                final String digit = digits[4*r+c];
                Button button = button(digit, SURFACE, FG, 22);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(button, lp);
                button.setOnClickListener(v -> append(digit));
            }
        }
        LinearLayout actions = horizontal();
        content.addView(actions, new LinearLayout.LayoutParams(-1, dp(64)));
        Button negative = button("−", SURFACE, FG, 20);
        Button delete = button("⌫", SURFACE, FG, 20);
        checkButton = button("CHECK", PURPLE, BG, 17);
        addWeighted(actions, negative, 1);
        addWeighted(actions, delete, 1);
        addWeighted(actions, checkButton, 2);
        negative.setOnClickListener(v -> {
            if (!answered && typed.length() == 0) {
                typed.append('−');
                showTyped();
            }
        });
        delete.setOnClickListener(v -> {
            if (!answered && typed.length() > 0) {
                typed.deleteCharAt(typed.length()-1);
                showTyped();
            }
        });
        checkButton.setOnClickListener(v -> {
            if (answered) {
                sessionIndex++;
                showQuestion();
            } else {
                checkAnswer();
            }
        });
        addSpace(18);
        Button edit = button("EDIT SELECTION / END SESSION", HEADER, MUTED, 14);
        content.addView(edit, new LinearLayout.LayoutParams(-1, dp(48)));
        edit.setOnClickListener(v -> showConfiguration());
        updateProgress();
        scrollToTop();
    }

    private void append(String digit) {
        if (!answered && typed.length() < 17) {
            typed.append(digit);
            showTyped();
        }
    }

    private void showTyped() {
        response.setText(typed.length() == 0 ? "_" : typed.toString());
        feedback.setText("");
    }

    private void checkAnswer() {
        Long entered = DozenMath.parse(typed.toString());
        if (entered == null) {
            feedback.setTextColor(RED);
            feedback.setText("Enter a base-12 answer.");
            return;
        }
        DozenMath.Problem current = PracticeGrid.problem(operation, session.get(sessionIndex));
        answered = true;
        sessionAttempted++;
        if (entered.longValue() == current.answer) {
            sessionCorrect++;
            feedback.setTextColor(GREEN);
            feedback.setText("Correct! ✓");
        } else {
            feedback.setTextColor(RED);
            feedback.setText("Correct answer: " + DozenMath.format(current.answer));
        }
        checkButton.setText(sessionIndex + 1 == session.size() ? "FINISH →" : "NEXT →");
        updateProgress();
    }

    private void updateProgress() {
        progress.setText("Question " + DozenMath.format(sessionIndex + 1) + "/"
            + DozenMath.format(session.size()) + "  ·  Correct "
            + DozenMath.format(sessionCorrect) + "/"
            + DozenMath.format(sessionAttempted));
    }

    private void showFinished() {
        content.removeAllViews();
        header("Session complete", "All selected facts have been practiced the requested number of times.");
        TextView score = label(DozenMath.format(sessionCorrect) + " / "
            + DozenMath.format(session.size()) + " correct", 27, GREEN, true);
        score.setGravity(Gravity.CENTER);
        content.addView(score);
        addSpace(18);
        Button again = button("REPEAT SAME FACTS", PURPLE, BG, 17);
        content.addView(again, new LinearLayout.LayoutParams(-1, dp(56)));
        again.setOnClickListener(v -> startPractice());
        addSpace(10);
        Button modify = button("EDIT SELECTION", SURFACE, FG, 16);
        content.addView(modify, new LinearLayout.LayoutParams(-1, dp(52)));
        modify.setOnClickListener(v -> showConfiguration());
        scrollToTop();
    }

    private void header(String title, String description) {
        TextView back = label("‹  WORKOUT", 14, CYAN, true);
        back.setPadding(0, dp(4), 0, dp(10));
        back.setOnClickListener(v -> finish());
        content.addView(back);
        content.addView(label(title, 28, PURPLE, true));
        content.addView(label(description, 13, MUTED, false));
        addSpace(18);
    }

    private void addWeighted(LinearLayout row, View view, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, weight);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        row.addView(view, params);
    }

    private Spinner spinner(String[] choices, int index) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, choices) {
            @Override public View getView(int i, View existing, ViewGroup parent) {
                return styleOption(super.getView(i, existing, parent), FG);
            }
            @Override public View getDropDownView(int i, View existing, ViewGroup parent) {
                return styleOption(super.getDropDownView(i, existing, parent), BG);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(index);
        spinner.setBackground(round(SURFACE, 10));
        spinner.setPadding(dp(8), 0, dp(7), 0);
        return spinner;
    }

    private View styleOption(View view, int color) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setTextColor(color);
            text.setTextSize(15);
            text.setPadding(dp(8), dp(8), dp(8), dp(8));
        }
        return view;
    }

    private TextView cell(LinearLayout row, String text, int bg, int fg, boolean bold,
                          View.OnClickListener listener, int width) {
        TextView t = label(text, text.length() > 5 ? 12 : text.length() > 3 ? 14 : 16, fg, bold);
        t.setGravity(Gravity.CENTER);
        setCellAppearance(t, bg, fg, bold);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(width), dp(51));
        lp.setMargins(dp(1), dp(1), dp(1), dp(1));
        row.addView(t, lp);
        if (listener != null) t.setOnClickListener(listener);
        return t;
    }

    private void setCellAppearance(TextView t, int bg, int fg, boolean bold) {
        t.setBackground(round(bg, 6));
        t.setTextColor(fg);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    private LinearLayout vertical() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private LinearLayout horizontal() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.HORIZONTAL);
        return v;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private Button button(String text, int background, int foreground, int sp) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(sp);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(foreground);
        button.setBackground(round(background, 10));
        return button;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        return bg;
    }

    private int dp(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + .5f);
    }

    private void addSpace(int dp) { addSpaceIn(content, dp); }

    private void addSpaceIn(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void scrollToTop() { scroll.post(() -> scroll.scrollTo(0, 0)); }

    @Override public void onBackPressed() {
        if (session != null) showConfiguration();
        else super.onBackPressed();
    }
}
