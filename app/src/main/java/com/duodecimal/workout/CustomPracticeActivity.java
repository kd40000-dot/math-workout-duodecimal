package com.duodecimal.workout;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.content.Intent;
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
    private static final long AUTO_ADVANCE_MS = 400L;
    private final Handler autoAdvanceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingAdvance;
    private int questionSerial;
    private boolean adaptive = true;
    private boolean showTable = false;
    private AdaptiveFacts learning;
    private long questionStartMs;
    private long slowestDurationMs;
    private long slowestPair;
    private boolean slowestWasCorrect;
    private boolean hasSlowest;
    private boolean firstAttemptOnQuestion;

    private void cancelAutoAdvance() {
        if (pendingAdvance != null) {
            autoAdvanceHandler.removeCallbacks(pendingAdvance);
            pendingAdvance = null;
        }
    }

    private void advanceAfterCorrect() {
        cancelAutoAdvance();
        final int answeredQuestion = questionSerial;
        pendingAdvance = () -> {
            pendingAdvance = null;
            if (!isFinishing() && !isDestroyed() && session != null
                    && answered && questionSerial == answeredQuestion) {
                sessionIndex++;
                showQuestion();
            }
        };
        autoAdvanceHandler.postDelayed(pendingAdvance, AUTO_ADVANCE_MS);
    }

    @Override protected void onDestroy() {
        cancelAutoAdvance();
        super.onDestroy();
    }


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
        adaptive = prefs.getBoolean("custom_adaptive_enabled", true);
        showTable = getIntent().hasExtra("open_fact_table") ?
            getIntent().getBooleanExtra("open_fact_table",false) :
            prefs.getBoolean("custom_show_table", false);
        learning = new AdaptiveFacts(prefs,operation,digitA,digitB,rowPage,colPage);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        content = vertical();
        content.setPadding(dp(14), dp(18), dp(14), dp(30));
        scroll.addView(content);
        setContentView(scroll);
        showConfiguration();
        // Launch exactly once for this installation, without changing any
        // adaptive-progress preferences. Also works for upgrades from older
        // app versions where the tutorial has not yet been shown.
        if(state==null && !prefs.getBoolean(TutorialActivity.PREF_COMPLETE,false)){
            startActivity(new Intent(this,TutorialActivity.class));
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if(intent.hasExtra("open_fact_table")){
            showTable=intent.getBooleanExtra("open_fact_table",false);
            prefs.edit().putBoolean("custom_show_table",showTable).apply();
        }
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
        learning = new AdaptiveFacts(prefs,operation,digitA,digitB,rowPage,colPage);
        saveConfig();
        showConfiguration();
    }

    private void showConfiguration() {
        learning = new AdaptiveFacts(prefs,operation,digitA,digitB,rowPage,colPage);
        cancelAutoAdvance();
        questionSerial++;
        session = null;
        content.removeAllViews();
        header("Practice", "Master one fact at a time. Your practice adapts to your speed and accuracy.");
        Button adaptiveButton = button(adaptive ? "Learning mode: Adaptive  ·  Change to manual" :
            "Learning mode: Manual  ·  Change to adaptive", SURFACE, FG, 14);
        content.addView(adaptiveButton, new LinearLayout.LayoutParams(-1,dp(49)));
        adaptiveButton.setOnClickListener(v -> {
            adaptive=!adaptive;
            prefs.edit().putBoolean("custom_adaptive_enabled",adaptive).apply();
            showConfiguration();
        });
        addSpace(12);
        if(adaptive) {
            List<Long> pool=learning.candidates(operation,digitA,digitB,rowPage,colPage);
            int unlocked=learning.updateUnlocks(pool);
            int mastered=learning.progress(pool);
            content.addView(label("Unlocked "+DozenMath.format(unlocked)+"/"+
                DozenMath.format(pool.size())+"  ·  Mastered "+DozenMath.format(mastered),
                17,CYAN,true));
            int required=AdaptiveSettings.requireAll(prefs)?100:AdaptiveSettings.unlockPercent(prefs);
            content.addView(label("Unlock "+DozenMath.format(AdaptiveSettings.batch(prefs))+
                " new facts at "+DozenMath.format(required)+
                "% mastery · Target "+DozenMath.format(AdaptiveSettings.targetMs(prefs)/1000)+
                " seconds.",13,MUTED,false));
            addSpace(10);
            Button go=button("Start practice",PURPLE,BG,18);
            content.addView(go,new LinearLayout.LayoutParams(-1,dp(55)));
            go.setOnClickListener(v->startPractice());
            addSpace(12);
        }
        content.addView(label("Operation", 14, MUTED, true));
        Spinner ops = spinner(PracticeGrid.OPERATIONS, operation);
        content.addView(ops, new LinearLayout.LayoutParams(-1, dp(48)));
        ops.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                if (index != operation) changeProfile(index, digitA, digitB);
            }
        });
        addSpace(12);
        content.addView(label("Number of digits", 14, MUTED, true));
        content.addView(label("First number · Second number", 12, MUTED, false));
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

        if (!adaptive) content.addView(label("Repetitions per fact", 14, MUTED, true));
        String[] repLabels = new String[PracticeGrid.REP_CHOICES.length];
        for (int i = 0; i < repLabels.length; i++)
            repLabels[i] = DozenMath.format(PracticeGrid.REP_CHOICES[i]) + " reps";
        Spinner repSpinner = spinner(repLabels, repsIndex);
        if(!adaptive) content.addView(repSpinner, new LinearLayout.LayoutParams(-1, dp(48)));
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

        content.addView(label(adaptive ? "Your fact table" : "Select facts", 19, CYAN, true));
        content.addView(label(adaptive ?
            "Purple: learning · Teal: mastered · Dim: locked. Tap a fact to see your statistics." :
            "Tap cells to select facts. Purple indicates selected facts.", 13, MUTED, false));
        addSpace(8);
        if(adaptive) {
            Button toggle=button(showTable ? "Hide table" : "View progress table",HEADER,CYAN,14);
            content.addView(toggle,new LinearLayout.LayoutParams(-1,dp(52)));
            toggle.setOnClickListener(v->{
                showTable=!showTable;
                prefs.edit().putBoolean("custom_show_table",showTable).apply();
                showConfiguration();
            });
            addSpace(10);
        }
        if(!adaptive || showTable) {
            axisPager("Rows · first number", digitA, true);
            axisPager("Columns · second number", digitB, false);
            addSpace(8);
            addTable();
            addSpace(12);
        }

        LinearLayout tools = horizontal();
        if (!adaptive) content.addView(tools, new LinearLayout.LayoutParams(-1, dp(48)));
        Button all = button("Select page", SURFACE, FG, 13);
        Button clearPage = button("Clear page", SURFACE, FG, 13);
        Button clearAll = button("Clear selection", SURFACE, RED, 13);
        addWeighted(tools, all, 1);
        addWeighted(tools, clearPage, 1);
        addWeighted(tools, clearAll, 1);
        all.setOnClickListener(v -> changeVisible(true, false));
        clearPage.setOnClickListener(v -> changeVisible(false, false));
        clearAll.setOnClickListener(v -> changeVisible(false, true));
        addSpace(12);

        selectedLabel = label("", 14, CYAN, true);
        selectedLabel.setGravity(Gravity.CENTER);
        if(!adaptive) content.addView(selectedLabel);
        addSpace(8);
        startButton = button("START PRACTICE", PURPLE, BG, 17);
        if(!adaptive) content.addView(startButton, new LinearLayout.LayoutParams(-1, dp(56)));
        startButton.setOnClickListener(v -> startPractice());
        addSpace(12);
        if(!adaptive) content.addView(label("Your selected facts are saved separately for each operation and operand-length combination. All table entries and session counts use base 12.", 12, MUTED, false));
        updateSelectionCount();
        scrollToTop();
    }

    private void updateSelectionCount() {
        if(adaptive || selectedLabel == null || startButton == null) return;
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

        List<Long> adaptivePool=adaptive ?
            learning.candidates(operation,digitA,digitB,rowPage,colPage) : null;
        int unlockedCount=adaptive ? learning.updateUnlocks(adaptivePool) : 0;
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
                int rank=adaptive && valid ? adaptivePool.indexOf(key) : -1;
                boolean open=adaptive && rank>=0 && rank<unlockedCount;
                boolean mastered=adaptive && open && learning.mastered(key);
                int fill=adaptive ? (mastered ? 0xFF245665 : open ? PURPLE : HEADER) :
                    (chosen ? PURPLE : SURFACE);
                int ink=adaptive ? (open && !mastered ? BG : FG) :
                    (chosen ? BG : FG);
                TextView item = cell(line, value, fill, valid ? ink : MUTED, chosen, null, 58);
                if(adaptive && !open)item.setAlpha(0.40f);
                if (valid && adaptive) {
                    int tries=learning.attempts(key);
                    int hits=learning.correct(key);
                    item.setContentDescription(PracticeGrid.expression(operation,a,b)+
                        (open ? ", unlocked" : ", locked")+", "+hits+" correct of "+tries+
                        " attempts, average "+(int)learning.averageMs(key)+" milliseconds");
                    // Inspect a fact's learning history without bypassing unlocks.
                    item.setOnClickListener(v -> {
                        int n=learning.attempts(key), right=learning.correct(key);
                        String status=!open ? "Locked" : learning.mastered(key) ? "Mastered" : "Learning";
                        new android.app.AlertDialog.Builder(this)
                            .setTitle(PracticeGrid.expression(operation,a,b))
                            .setMessage("Status: "+status+"\nCorrect: "+right+"/"+n+
                                "\nAccuracy: "+(n==0?0:(100*right/n))+"%"+
                                "\nRecent correct-answer time: "+(int)learning.recentCorrectMs(key)+" ms"+
                                "\nCorrect streak: "+learning.streak(key)+
                                "\nMastery target: "+AdaptiveSettings.minCorrect(prefs)+" correct, "+
                                AdaptiveSettings.accuracy(prefs)+"% accuracy, streak "+
                                AdaptiveSettings.streak(prefs)+", "+
                                (AdaptiveSettings.targetMs(prefs)/1000)+" seconds.")
                            .setPositiveButton("OK",null).show();
                    });
                } else if (valid) {
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
        if(adaptive){
            List<Long> pool=learning.candidates(operation,digitA,digitB,rowPage,colPage);
            session=learning.session(pool,AdaptiveSettings.questions(prefs),random);
            if(session.isEmpty())return;
        } else {
            if (selected.isEmpty()) return;
            saveSelected();
            session = PracticeGrid.makeSession(selected, PracticeGrid.REP_CHOICES[repsIndex], random);
        }
        sessionIndex = sessionAttempted = sessionCorrect = 0;
        slowestDurationMs = 0;
        slowestPair = 0;
        hasSlowest = false;
        slowestWasCorrect = false;
        answered = false;
        showQuestion();
    }

    private void showQuestion() {
        cancelAutoAdvance();
        questionSerial++;
        if (session == null || sessionIndex >= session.size()) {
            showFinished();
            return;
        }
        if (adaptive && sessionIndex > 0) {
            // Re-sample after each answer so the newest accuracy, latency and
            // mastery values immediately influence subsequent practice.
            List<Long> pool=learning.candidates(operation,digitA,digitB,rowPage,colPage);
            List<Long> proposals=learning.session(pool,2,random);
            if(!proposals.isEmpty()) {
                long previous=session.get(sessionIndex-1);
                long chosen=proposals.get(0);
                if(chosen==previous && proposals.size()>1)chosen=proposals.get(1);
                session.set(sessionIndex,chosen);
            }
        }
        answered = false;
        firstAttemptOnQuestion=true;
        questionStartMs=SystemClock.elapsedRealtime();
        typed.setLength(0);
        content.removeAllViews();
        header("Practice", PracticeGrid.OPERATIONS[operation]
            + (adaptive ? " · adaptive learning" :
            " · " + DozenMath.format(selected.size()) + " selected facts"));
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
                // Button's framework text transformation can discard ReplacementSpan
                // glyphs on some Android versions. Use the same numeral-aware
                // TextView as the question and answer, with a clickable key surface.
                TextView key = label(digit, 27, FG, true);
                key.setGravity(Gravity.CENTER);
                key.setBackground(round(SURFACE, 10));
                key.setClickable(true);
                key.setFocusable(true);
                key.setContentDescription("Digit " + digit);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
                lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                row.addView(key, lp);
                key.setOnClickListener(v -> append(digit));
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
            autoAcceptIfCorrect();
        }
    }

    private void showTyped() {
        response.setText(typed.length() == 0 ? "_" : typed.toString());
        // Force the independently drawn numeral glyphs to repaint after every key.
        response.invalidate();
        feedback.setText("");
    }

    private void autoAcceptIfCorrect() {
        if (session != null && !answered && DozenMath.isCorrect(typed.toString(),
                PracticeGrid.problem(operation, session.get(sessionIndex)).answer)) {
            checkAnswer();
        }
    }

    private void checkAnswer() {
        if (answered || session == null || sessionIndex >= session.size()) return;
        Long entered = DozenMath.parse(typed.toString());
        if (entered == null) {
            feedback.setTextColor(RED);
            feedback.setText("Enter a base-12 answer.");
            return;
        }
        DozenMath.Problem current = PracticeGrid.problem(operation, session.get(sessionIndex));
        answered = true;
        long durationMs = Math.max(0,SystemClock.elapsedRealtime()-questionStartMs);
        boolean correctAnswer = entered.longValue()==current.answer;
        if (!hasSlowest || durationMs > slowestDurationMs) {
            hasSlowest = true;
            slowestDurationMs = durationMs;
            slowestPair = session.get(sessionIndex);
            slowestWasCorrect = correctAnswer;
        }
        learning.record(session.get(sessionIndex),correctAnswer,durationMs);
        sessionAttempted++;
        if (entered.longValue() == current.answer) {
            sessionCorrect++;
            feedback.setTextColor(GREEN);
            feedback.setText("Correct! ✓");
            advanceAfterCorrect();
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
        header("Session complete", adaptive ?
            "Adaptive learning updated your per-fact speed and accuracy. More facts unlock as mastery improves." :
            "All selected facts have been practiced the requested number of times.");
        TextView score = label(DozenMath.format(sessionCorrect) + " / "
            + DozenMath.format(session.size()) + " correct", 27, GREEN, true);
        score.setGravity(Gravity.CENTER);
        content.addView(score);
        addSpace(16);
        if(hasSlowest) {
            LinearLayout slowCard = vertical();
            slowCard.setPadding(dp(16),dp(15),dp(16),dp(15));
            slowCard.setBackground(round(SURFACE,14));
            content.addView(slowCard,new LinearLayout.LayoutParams(-1,-2));
            slowCard.addView(label("SLOWEST FACT THIS ROUND",13,CYAN,true));
            addSpaceIn(slowCard,8);
            String expression = PracticeGrid.expression(operation,
                PracticeGrid.firstOperand(slowestPair),
                PracticeGrid.secondOperand(slowestPair));
            slowCard.addView(label(expression+" = "+
                DozenMath.format(PracticeGrid.answer(operation,
                    PracticeGrid.firstOperand(slowestPair),
                    PracticeGrid.secondOperand(slowestPair))),25,FG,true));
            addSpaceIn(slowCard,5);
            slowCard.addView(label(PracticeTiming.formatSeconds(slowestDurationMs)+
                " response time  ·  "+(slowestWasCorrect?"Correct":"Incorrect"),
                17,slowestWasCorrect?GREEN:RED,true));
            addSpaceIn(slowCard,5);
            slowCard.addView(label("Measured from question display until the answer was submitted.",12,MUTED,false));
        }
        addSpace(18);
        Button again = button(adaptive ? "Continue learning" : "Repeat practice", PURPLE, BG, 17);
        content.addView(again, new LinearLayout.LayoutParams(-1, dp(56)));
        again.setOnClickListener(v -> startPractice());
        addSpace(10);
        Button modify = button(adaptive ? "View progress" : "Edit selection", SURFACE, FG, 16);
        content.addView(modify, new LinearLayout.LayoutParams(-1, dp(52)));
        modify.setOnClickListener(v -> showConfiguration());
        scrollToTop();
    }

    private void header(String title, String description) {
        DrawerNavigation.addBar(this,content,
            session!=null?"Practice":showTable?"Browse facts":"Practice",
            session!=null?DrawerNavigation.PRACTICE:
                showTable?DrawerNavigation.BROWSE:DrawerNavigation.PRACTICE);
        if (session != null) {
            TextView back = label("‹  Back to practice", 15, CYAN, true);
            back.setMinHeight(dp(48));
            back.setGravity(Gravity.CENTER_VERTICAL);
            back.setOnClickListener(v -> showConfiguration());
            content.addView(back);
        }
        content.addView(label(title, 28, PURPLE, true));
        content.addView(label(description, 14, MUTED, false));
        addSpace(18);
    }

    private void addWeighted(LinearLayout row, View view, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, weight);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        row.addView(view, params);
    }

    private Spinner spinner(String[] choices, int index) {
        Spinner spinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, choices) {
            @Override public View getView(int i, View existing, ViewGroup parent) {
                return styleOption(super.getView(i, existing, parent), FG);
            }
            @Override public View getDropDownView(int i, View existing, ViewGroup parent) {
                return styleOption(super.getDropDownView(i, existing, parent), FG);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(index);
        spinner.setBackground(round(SURFACE, 10));
        spinner.setPopupBackgroundDrawable(round(SURFACE, 10));
        spinner.setPadding(dp(8), 0, dp(7), 0);
        return spinner;
    }

    private View styleOption(View view, int color) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            text.setText(Numerals.render(text.getText()));
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
        TextView view = new Numerals.NumeralTextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private Button button(String text, int background, int foreground, int sp) {
        Button button = new Numerals.NumeralButton(this);
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
        if(DrawerNavigation.close(this))return;
        if (session != null) showConfiguration();
        else super.onBackPressed();
    }
}
