package com.duodecimal.workout;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

/**
 * Visual arithmetic lessons and five drills based on Draft A's mod-3/mod-4
 * glyph components. Does not affect normal workouts or custom fact selections.
 */
public final class VisualAdditionActivity extends Activity {
    private static final int BG = 0xFF282A36, SURFACE = 0xFF44475A;
    private static final int FG = 0xFFF8F8F2, PURPLE = 0xFFBD93F9;
    private static final int CYAN = 0xFF8BE9FD, GREEN = 0xFF50FA7B;
    private static final int RED = 0xFFFF5555, MUTED = 0xFFB5B8C6;
    private static final int HEADER = 0xFF363849;
    private static final long FEEDBACK_DELAY_MS = 480;
    private static final long TIMED_DURATION_MS = 60000;
    private static final int[] LENGTHS = {12, 24, 48}; // 10, 20, 40 in base 12
    private static final String[] CAP_RULES = {
        "A flat cap adds zero modulo 3, so it leaves the other cap unchanged.",
        "Breve + breve gives dot. Breve + dot gives flat. Dot + dot gives breve.",
        "Caps cycle through flat → breve → dot → flat when adding one."
    };
    private static final String[] BODY_RULES = {
        "The loop body adds zero modulo 4 and leaves the other body unchanged.",
        "Short-T + short-T and el + el both give the V body.",
        "Short-T + el and V + V both give the loop body."
    };

    private SharedPreferences prefs;
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pending, tick;
    private ScrollView scroll;
    private LinearLayout root;
    private TextView prompt, stepTitle, feedback, explanation, progress, clockLabel;
    private int mode, lengthIndex, questionIndex, stageIndex;
    private boolean hints, showValues, timed, showTables, questionPerfect, firstAttempt, stageResolved;
    private int correctQuestions, attemptedQuestions;
    private long endTimeMs;
    private List<VisualAddition.Fact> questions;
    private VisualAddition.Fact current;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("duodecimal_workout", MODE_PRIVATE);
        mode = range(prefs.getInt("visual_mode", VisualAddition.CAP), 0, VisualAddition.MODE_COUNT-1);
        lengthIndex = range(prefs.getInt("visual_length", 0), 0, LENGTHS.length-1);
        hints = prefs.getBoolean("visual_hints", true);
        showValues = prefs.getBoolean("visual_values", false);
        timed = prefs.getBoolean("visual_timed", false);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        root = vertical();
        root.setPadding(dp(16), dp(18), dp(16), dp(28));
        scroll.addView(root);
        setContentView(scroll);
        showSetup();
    }

    private static int range(int n, int a, int b) { return Math.max(a, Math.min(b,n)); }
    private void saveSetup() {
        prefs.edit().putInt("visual_mode", mode).putInt("visual_length", lengthIndex)
            .putBoolean("visual_hints", hints).putBoolean("visual_values", showValues)
            .putBoolean("visual_timed", timed).apply();
    }
    private void cancelCallbacks() {
        if (pending != null) { handler.removeCallbacks(pending); pending = null; }
        if (tick != null) { handler.removeCallbacks(tick); tick = null; }
    }

    @Override protected void onDestroy() {
        cancelCallbacks();
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (questions != null) showSetup();
        else super.onBackPressed();
    }

    private void showSetup() {
        cancelCallbacks();
        questions = null;
        root.removeAllViews();
        pageHeader("Visual Addition", "Read the cap and body of each glyph separately.");
        title("TRAINING MODE", 12, MUTED);
        Spinner modeSpinner = spinner(VisualAddition.MODE_NAMES, mode);
        root.addView(modeSpinner, new LinearLayout.LayoutParams(-1, dp(50)));
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View v, int i, long id) {
                if (mode != i) {
                    mode = i; saveSetup(); showSetup();
                }
            }
        });
        gap(12);
        title("QUESTIONS PER SESSION", 12, MUTED);
        String[] lengthOptions = new String[LENGTHS.length];
        for (int i=0; i<LENGTHS.length; i++)
            lengthOptions[i] = DozenMath.format(LENGTHS[i]) + " questions";
        Spinner lengthSpinner = spinner(lengthOptions,lengthIndex);
        root.addView(lengthSpinner, new LinearLayout.LayoutParams(-1, dp(50)));
        lengthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View v, int i, long id) {
                if (lengthIndex != i) { lengthIndex = i; saveSetup(); }
            }
        });
        gap(10);
        checkbox("Show pattern hints", hints, checked -> { hints = checked; saveSetup(); });
        checkbox("Show traditional base-12 digit labels (learning aid)", showValues,
            checked -> { showValues = checked; saveSetup(); });
        checkbox("One-minute timed challenge", timed, checked -> { timed = checked; saveSetup(); });
        gap(12);
        LinearLayout lesson = vertical();
        lesson.setPadding(dp(14), dp(15), dp(14), dp(15));
        lesson.setBackground(bg(SURFACE, 14));
        root.addView(lesson);
        TextView headline = label("HOW THIS DRILL WORKS", 13, CYAN, true);
        lesson.addView(headline);
        gapInto(lesson, 8);
        String description;
        switch (mode) {
            case VisualAddition.CAP:
                description = "Add only the caps, modulo 3. Choose flat, breve or dot. Each of the 9 cap combinations appears once per pass.";
                break;
            case VisualAddition.BODY:
                description = "Add only the bodies, modulo 4. Choose loop, short-T, V or el. Each of the 16 body combinations appears once per pass.";
                break;
            case VisualAddition.COMBINE:
                description = "No-carry additions: first select the resulting cap, then the body. Together they uniquely determine the final digit.";
                break;
            case VisualAddition.CARRY:
                description = "Decide whether two single-digit addends reach 10 in base twelve. The final digit is separate from the carry.";
                break;
            default:
                description = "Full visual addition: select the cap, then the body, and finally whether to carry. Complete all three steps.";
        }
        lesson.addView(label(description, 14, FG, false));
        gapInto(lesson, 6);
        lesson.addView(label("Body = digit mod 4; cap = digit mod 3. Both components add independently.", 12, MUTED, false));
        gap(12);

        title("PATTERN REFERENCE", 12, CYAN);
        Button show = button(showTables ? "HIDE REFERENCE TABLES" : "SHOW REFERENCE TABLES",
            HEADER, FG, 13);
        root.addView(show, new LinearLayout.LayoutParams(-1, dp(44)));
        show.setOnClickListener(v -> { showTables = !showTables; showSetup(); });
        if (showTables) {
            gap(10);
            if (mode != VisualAddition.BODY && mode != VisualAddition.CARRY) addReference(true);
            if (mode != VisualAddition.CAP && mode != VisualAddition.CARRY) addReference(false);
            if (mode == VisualAddition.CARRY)
                root.addView(label("A carry occurs exactly when a + b is at least 10₁₂. The digit body and cap alone do not reveal it.", 13, FG, false));
        }

        gap(16);
        int tries = prefs.getInt("visual_mode_tried_"+mode,0);
        int correct = prefs.getInt("visual_mode_correct_"+mode,0);
        TextView history = label("Previous first-try questions: " + DozenMath.format(correct) +
            "/" + DozenMath.format(tries), 14, CYAN, false);
        history.setGravity(Gravity.CENTER);
        root.addView(history);
        gap(10);
        Button start = button("START VISUAL ADDITION →", PURPLE, BG, 17);
        root.addView(start, new LinearLayout.LayoutParams(-1, dp(58)));
        start.setOnClickListener(v -> startSession());
        gap(13);
        root.addView(label("Answers are checked on each tap. Correct choices advance automatically. Wrong choices can be retried, but count as a missed first attempt.", 12, MUTED, false));
        toTop();
    }

    private interface SwitchHandler { void changed(boolean checked); }

    private void checkbox(String name, boolean checked, SwitchHandler action) {
        CheckBox cb = new CheckBox(this);
        cb.setText(name);
        cb.setTextColor(FG);
        cb.setTextSize(14);
        cb.setChecked(checked);
        cb.setButtonTintList(ColorStateList.valueOf(PURPLE));
        cb.setOnCheckedChangeListener((button, state) -> action.changed(state));
        root.addView(cb, new LinearLayout.LayoutParams(-1, dp(43)));
    }

    private void addReference(boolean cap) {
        TextView title = label(cap ? "CAP: mod 3" : "BODY: mod 4", 14, CYAN, true);
        root.addView(title);
        int n = cap ? 3 : 4;
        LinearLayout table = vertical();
        table.setBackground(bg(HEADER, 9));
        root.addView(table);
        for (int row=0;row<=n;row++) {
            LinearLayout line = horizontal();
            table.addView(line);
            for (int col=0;col<=n;col++) {
                if(row==0 && col==0) {
                    TextView plus = label("+", 19, CYAN, true);
                    plus.setGravity(Gravity.CENTER);
                    line.addView(plus, new LinearLayout.LayoutParams(0, dp(52),1));
                } else {
                    int value = row==0 ? col-1 : col==0 ? row-1 : (row+col-2)%n;
                    ComponentView icon = new ComponentView(cap, value,
                        row==0||col==0 ? CYAN : FG);
                    line.addView(icon, new LinearLayout.LayoutParams(0, dp(52),1));
                }
            }
        }
        gap(12);
    }

    private void startSession() {
        cancelCallbacks();
        questions = VisualAddition.session(mode, LENGTHS[lengthIndex], random);
        questionIndex = 0;
        correctQuestions = attemptedQuestions = 0;
        if (timed) {
            endTimeMs = SystemClock.elapsedRealtime() + TIMED_DURATION_MS;
            scheduleTick();
        }
        showQuestion();
    }

    private void scheduleTick() {
        if (questions == null || !timed) return;
        tick = () -> {
            tick = null;
            if (questions == null) return;
            if (SystemClock.elapsedRealtime() >= endTimeMs) {
                showFinished();
            } else {
                updateProgress();
                scheduleTick();
            }
        };
        handler.postDelayed(tick, 200);
    }

    private void showQuestion() {
        if (questions == null) return;
        cancelPendingOnly();
        if (questionIndex >= questions.size() ||
            (timed && SystemClock.elapsedRealtime() >= endTimeMs)) {
            showFinished();
            return;
        }
        current = questions.get(questionIndex);
        stageIndex = 0;
        questionPerfect = true;
        firstAttempt = true;
        stageResolved = false;
        showStep();
    }

    private void showStep() {
        if (questions == null) return;
        root.removeAllViews();
        pageHeader("Visual Addition", VisualAddition.MODE_NAMES[mode]);
        progress = label("", 14, CYAN, true);
        progress.setGravity(Gravity.CENTER);
        root.addView(progress);
        if (timed) {
            clockLabel = label("", 15, PURPLE, true);
            clockLabel.setGravity(Gravity.CENTER);
            root.addView(clockLabel);
        } else clockLabel = null;
        gap(14);
        LinearLayout card = vertical();
        card.setPadding(dp(10), dp(20), dp(10), dp(18));
        card.setBackground(bg(SURFACE, 16));
        root.addView(card, new LinearLayout.LayoutParams(-1, -2));
        TextView preface = label("ADD IN BASE TWELVE", 12, CYAN, true);
        preface.setGravity(Gravity.CENTER);
        card.addView(preface);
        gapInto(card, 12);
        prompt = label(current.expression(), 33, FG, true);
        prompt.setMinHeight(dp(65));
        prompt.setGravity(Gravity.CENTER);
        card.addView(prompt);
        if (showValues) {
            // Only shown when requested: standard-notation reference for learners.
            TextView conventional = new TextView(this);
            conventional.setText("Traditional notation: " + current.expression());
            conventional.setTextColor(MUTED);
            conventional.setTextSize(12);
            conventional.setGravity(Gravity.CENTER);
            card.addView(conventional);
        }
        gapInto(card, 8);
        int[] stages = VisualAddition.stages(mode);
        int stage = stages[stageIndex];
        stepTitle = label("STEP " + DozenMath.format(stageIndex+1) + "/" +
            DozenMath.format(stages.length) + " · " + stageName(stage), 14, PURPLE, true);
        stepTitle.setGravity(Gravity.CENTER);
        card.addView(stepTitle);
        gapInto(card, 10);
        if (stage == VisualAddition.PART_CAP || stage == VisualAddition.PART_BODY) {
            LinearLayout parts = horizontal();
            parts.setGravity(Gravity.CENTER);
            card.addView(parts, new LinearLayout.LayoutParams(-1, dp(72)));
            boolean cap = stage==VisualAddition.PART_CAP;
            int left = cap ? current.a % 3 : current.a % 4;
            int right = cap ? current.b % 3 : current.b % 4;
            parts.addView(new ComponentView(cap,left,FG),new LinearLayout.LayoutParams(0,-1,1));
            TextView plus = label("+", 23, CYAN, true);
            plus.setGravity(Gravity.CENTER);
            parts.addView(plus,new LinearLayout.LayoutParams(dp(34),-1));
            parts.addView(new ComponentView(cap,right,FG),new LinearLayout.LayoutParams(0,-1,1));
            TextView equals = label("=", 23, CYAN, true);
            equals.setGravity(Gravity.CENTER);
            parts.addView(equals,new LinearLayout.LayoutParams(dp(34),-1));
            TextView mark = label("?", 30, MUTED, true);
            mark.setGravity(Gravity.CENTER);
            parts.addView(mark,new LinearLayout.LayoutParams(0,-1,1));
        } else {
            TextView promptCarry = label("Does the sum reach 10₁₂ or more?", 17, FG, true);
            promptCarry.setGravity(Gravity.CENTER);
            card.addView(promptCarry);
        }
        gapInto(card, 8);
        feedback = label("", 14, GREEN, true);
        feedback.setMinHeight(dp(28));
        feedback.setGravity(Gravity.CENTER);
        card.addView(feedback);
        gap(17);
        title("CHOOSE THE " + stageName(stage).toUpperCase(), 12, MUTED);
        if (stage == VisualAddition.PART_CARRY) {
            LinearLayout choices = horizontal();
            root.addView(choices, new LinearLayout.LayoutParams(-1, dp(70)));
            for (int i=0;i<=1;i++) {
                final int choice = i;
                Button button = button(i==0 ? "NO CARRY" : "CARRY 1", HEADER, FG, 17);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1,1);
                lp.setMargins(dp(3),dp(3),dp(3),dp(3));
                choices.addView(button,lp);
                button.setOnClickListener(v -> answer(choice,button));
            }
        } else {
            boolean cap = stage == VisualAddition.PART_CAP;
            LinearLayout choices = horizontal();
            root.addView(choices,new LinearLayout.LayoutParams(-1,dp(119)));
            for (int i=0; i<(cap?3:4); i++) {
                final int choice = i;
                LinearLayout choiceCard = vertical();
                choiceCard.setPadding(dp(3),dp(7),dp(3),dp(4));
                choiceCard.setBackground(bg(HEADER,12));
                choiceCard.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,-1,1);
                lp.setMargins(dp(3),dp(3),dp(3),dp(3));
                choices.addView(choiceCard, lp);
                choiceCard.addView(new ComponentView(cap,choice,FG),
                    new LinearLayout.LayoutParams(-1,dp(64)));
                TextView label = label(cap ? VisualAddition.CAP_NAMES[i]
                    : VisualAddition.BODY_NAMES[i], 12, FG, false);
                label.setGravity(Gravity.CENTER);
                choiceCard.addView(label);
                choiceCard.setContentDescription((cap ? "Cap: " : "Body: ")
                    +(cap ? VisualAddition.CAP_NAMES[i] : VisualAddition.BODY_NAMES[i]));
                choiceCard.setOnClickListener(v -> answer(choice,choiceCard));
            }
        }
        gap(12);
        explanation = label("",13,MUTED,false);
        if (hints) explanation.setText(hintFor(stage));
        root.addView(explanation);
        gap(10);
        Button lesson = button("REVEAL RULE / EXPLANATION", SURFACE, CYAN, 13);
        root.addView(lesson,new LinearLayout.LayoutParams(-1,dp(45)));
        lesson.setOnClickListener(v -> explanation.setText(current.explanation(stage)));
        gap(14);
        Button back = button("END SESSION / CHANGE MODE", SURFACE, MUTED, 13);
        root.addView(back,new LinearLayout.LayoutParams(-1,dp(45)));
        back.setOnClickListener(v -> showSetup());
        updateProgress();
        toTop();
    }

    private String stageName(int stage) {
        return stage==VisualAddition.PART_CAP ? "Result cap"
            : stage==VisualAddition.PART_BODY ? "Result body" : "Carry";
    }

    private String hintFor(int stage) {
        if (stage==VisualAddition.PART_CAP) return CAP_RULES[(questionIndex+stageIndex)%CAP_RULES.length];
        if (stage==VisualAddition.PART_BODY) return BODY_RULES[(questionIndex+stageIndex)%BODY_RULES.length];
        return "A carry happens when the sum is 10₁₂ or greater. The cap/body tell you only the final digit.";
    }

    private void answer(int choice, View chosen) {
        if (questions == null || stageResolved) return;
        int stage = VisualAddition.stages(mode)[stageIndex];
        int expected = current.expected(stage);
        boolean accurate = choice == expected;
        if (firstAttempt) {
            firstAttempt = false;
            prefs.edit().putInt("visual_part_tried_"+stage,
                prefs.getInt("visual_part_tried_"+stage,0)+1)
                .putInt("visual_part_correct_"+stage,
                    prefs.getInt("visual_part_correct_"+stage,0)+(accurate?1:0)).apply();
        }
        if (!accurate) {
            questionPerfect = false;
            feedback.setTextColor(RED);
            feedback.setText("Not quite. Try another choice.");
            chosen.setBackground(bg(0xFF7D3D52,12));
            return;
        }
        stageResolved = true;
        feedback.setTextColor(GREEN);
        boolean finishedQuestion = stageIndex+1 == VisualAddition.stages(mode).length;
        feedback.setText(finishedQuestion ? "✓ " + current.equation()
            : (questionPerfect ? "Correct ✓" : "Correct ✓ · practice retry"));
        if (finishedQuestion) {
            attemptedQuestions++;
            if(questionPerfect) correctQuestions++;
            prefs.edit().putInt("visual_mode_tried_"+mode,
                    prefs.getInt("visual_mode_tried_"+mode,0)+1)
                .putInt("visual_mode_correct_"+mode,
                    prefs.getInt("visual_mode_correct_"+mode,0)+(questionPerfect?1:0)).apply();
        }
        updateProgress();
        pending = () -> {
            pending = null;
            if (questions == null || isFinishing() || isDestroyed()) return;
            if (stageIndex+1 < VisualAddition.stages(mode).length) {
                stageIndex++;
                firstAttempt = true;
                stageResolved = false;
                showStep();
            } else {
                questionIndex++;
                showQuestion();
            }
        };
        handler.postDelayed(pending,finishedQuestion ? 850 : FEEDBACK_DELAY_MS);
    }

    private void cancelPendingOnly() {
        if (pending!=null) { handler.removeCallbacks(pending); pending=null; }
    }

    private void updateProgress() {
        if (progress != null && questions != null) {
            progress.setText("Question " + DozenMath.format(questionIndex+1) + "/" +
                DozenMath.format(questions.size()) + " · Perfect " +
                DozenMath.format(correctQuestions) + "/" + DozenMath.format(attemptedQuestions));
        }
        if (clockLabel!=null && timed) {
            long remain = Math.max(0,endTimeMs-SystemClock.elapsedRealtime());
            clockLabel.setText("Time remaining: " + DozenMath.format((remain+999)/1000) + " s");
        }
    }

    private void showFinished() {
        cancelCallbacks();
        if (questions==null) return;
        questions = null;
        root.removeAllViews();
        pageHeader("Training complete", VisualAddition.MODE_NAMES[mode]);
        TextView summary=label(DozenMath.format(correctQuestions) + "/" +
            DozenMath.format(attemptedQuestions) + " perfect first tries", 24, GREEN, true);
        summary.setGravity(Gravity.CENTER);
        root.addView(summary);
        gap(12);
        root.addView(label("A question counts as perfect only if every step is correct on the first tap. Wrong answers can always be retried to learn the pattern.", 14, FG, false));
        gap(18);
        title("CUMULATIVE COMPONENT ACCURACY", 13, CYAN);
        for (int stage=0;stage<3;stage++) {
            int good=prefs.getInt("visual_part_correct_"+stage,0);
            int tried=prefs.getInt("visual_part_tried_"+stage,0);
            root.addView(label(stageName(stage)+": "+DozenMath.format(good)+"/"+
                DozenMath.format(tried),14,FG,false));
            gap(8);
        }
        gap(20);
        Button again = button("REPEAT DRILL", PURPLE, BG, 17);
        root.addView(again, new LinearLayout.LayoutParams(-1,dp(56)));
        again.setOnClickListener(v -> startSession());
        gap(10);
        Button edit = button("CHOOSE ANOTHER MODE", SURFACE, FG, 15);
        root.addView(edit, new LinearLayout.LayoutParams(-1,dp(52)));
        edit.setOnClickListener(v -> showSetup());
        toTop();
    }

    private Spinner spinner(String[] values,int choice) {
        Spinner s=new Spinner(this, Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,
            android.R.layout.simple_spinner_item,values) {
            @Override public View getView(int position, View old, ViewGroup parent) {
                return styleSpinner(super.getView(position,old,parent),FG);
            }
            @Override public View getDropDownView(int position, View old, ViewGroup parent) {
                return styleSpinner(super.getDropDownView(position,old,parent),FG);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setSelection(choice);
        s.setPadding(dp(10),0,dp(8),0);
        s.setBackground(bg(SURFACE,10));
        // Explicit popup background and bright text: Android's dropdown theme
        // otherwise combines almost-black options with a dark popup.
        s.setPopupBackgroundDrawable(bg(SURFACE,10));
        return s;
    }
    private View styleSpinner(View v,int fg) {
        if(v instanceof TextView){
            TextView t=(TextView)v;
            t.setText(Numerals.render(t.getText()));
            t.setTextColor(fg);
            t.setTextSize(16);
            t.setPadding(dp(8),dp(8),dp(8),dp(8));
        }
        return v;
    }

    private void pageHeader(String heading, String subtitle) {
        TextView back=label("‹  WORKOUT",14,CYAN,true);
        back.setPadding(0,0,0,dp(12));
        back.setOnClickListener(v -> finish());
        root.addView(back);
        root.addView(label(heading,28,PURPLE,true));
        root.addView(label(subtitle,13,MUTED,false));
        gap(16);
    }

    private LinearLayout vertical() {
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    private LinearLayout horizontal() {
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l;
    }
    private int dp(int x) { return (int)(x*getResources().getDisplayMetrics().density+.5f); }
    private void title(String text, int size,int color) {
        root.addView(label(text,size,color,true));
        gap(7);
    }
    private TextView label(String text,int sp,int color,boolean bold) {
        TextView v=new Numerals.NumeralTextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);
        return v;
    }
    private Button button(String text,int bg,int color,int sp) {
        Button b=new Numerals.NumeralButton(this);
        b.setText(text);
        b.setTextSize(sp);
        b.setAllCaps(false);
        b.setTextColor(color);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(bg(bg,10));
        return b;
    }
    private GradientDrawable bg(int color,int radius) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private void gap(int amount) { gapInto(root, amount); }
    private void gapInto(LinearLayout container,int amount) {
        container.addView(new View(this),new LinearLayout.LayoutParams(1,dp(amount)));
    }
    private void toTop() { scroll.post(() -> scroll.scrollTo(0,0)); }

    private final class ComponentView extends View {
        private final boolean cap;
        private final int part;
        private final int color;
        ComponentView(boolean cap,int part,int color) {
            super(VisualAdditionActivity.this);
            this.cap=cap;this.part=part;this.color=color;
            setContentDescription((cap? "Cap ":"Body ") +
                (cap?VisualAddition.CAP_NAMES[part]:VisualAddition.BODY_NAMES[part]));
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float height=Math.min(dp(cap?46:70),getHeight()*.80f);
            Numerals.drawComponent(canvas,cap,part,getWidth()/2f,getHeight()/2f,height,color);
        }
    }
}
