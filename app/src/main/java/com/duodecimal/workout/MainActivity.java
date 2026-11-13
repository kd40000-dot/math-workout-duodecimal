package com.duodecimal.workout;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

/** Offline workout UI. No ad SDKs, network services or proprietary app code. */
public final class MainActivity extends Activity {
    private static final int BG=0xFF282A36, SURFACE=0xFF44475A, FG=0xFFF8F8F2;
    private static final int PURPLE=0xFFBD93F9, CYAN=0xFF8BE9FD, GREEN=0xFF50FA7B;
    private static final int RED=0xFFFF5555, MUTED=0xFFB5B8C6;
    private final DozenMath engine = new DozenMath();
    private final StringBuilder input = new StringBuilder();
    private TextView problemLabel, answerLabel, feedbackLabel, statsLabel, difficultyLabel, hintLabel;
    private Button submitButton;
    private SharedPreferences prefs;
    private DozenMath.Problem current;
    private int mode, difficulty, correct, attempted, streak, best;
    private boolean solved;
    // Short visual confirmation, then proceed without requiring NEXT.
    private static final long AUTO_ADVANCE_MS = 400L;
    private final Handler autoAdvanceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingAdvance;
    private int questionSerial;

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
            if (!isFinishing() && !isDestroyed() && solved && questionSerial == answeredQuestion) {
                newProblem();
            }
        };
        autoAdvanceHandler.postDelayed(pendingAdvance, AUTO_ADVANCE_MS);
    }

    @Override public void onBackPressed() {
        if(DrawerNavigation.close(this))return;
        super.onBackPressed();
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
        mode=prefs.getInt("mode",0); difficulty=prefs.getInt("difficulty",2);
        correct=prefs.getInt("correct",0); attempted=prefs.getInt("attempted",0);
        best=prefs.getInt("best",0);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout body = vertical();
        body.setPadding(dp(16),dp(20),dp(16),dp(28));
        scroll.addView(body);
        setContentView(scroll);
        DrawerNavigation.addBar(this,body,"Other workouts",DrawerNavigation.MORE);
        TextView title=label("More workouts",29,PURPLE,true);
        body.addView(title);
        body.addView(label("Explore mixed exercises and challenge yourself beyond the adaptive fact table.",14,MUTED,false));
        addSpace(body,20);
        body.addView(label("Quick workout",19,CYAN,true));
        addSpace(body,8);
        body.addView(label("Exercise type",14,MUTED,true));
        Spinner modes = new Spinner(this, Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,DozenMath.MODES) {
            @Override public View getView(int position,View convertView,ViewGroup parent) {
                return styledItem(super.getView(position,convertView,parent), FG);
            }
            @Override public View getDropDownView(int position,View convertView,ViewGroup parent) {
                return styledItem(super.getDropDownView(position,convertView,parent), FG);
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modes.setAdapter(adapter);
        modes.setBackground(round(SURFACE,12));
        modes.setPopupBackgroundDrawable(round(SURFACE,12));
        modes.setPadding(dp(12),dp(2),dp(8),dp(2));
        body.addView(modes,new LinearLayout.LayoutParams(-1,dp(52)));
        modes.setSelection(Math.max(0,Math.min(DozenMath.MODES.length-1,mode)));
        modes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) {}
            public void onItemSelected(AdapterView<?> parent,View view,int pos,long id) {
                if (mode != pos) { mode=pos;save();newProblem(); }
            }
        });
        addSpace(body,12);
        difficultyLabel=label("Difficulty: " + DozenMath.format(difficulty) + " / 5",14,MUTED,true);
        body.addView(difficultyLabel);
        SeekBar seek = new SeekBar(this);
        seek.setMax(4);
        seek.setProgress(difficulty-1);
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        body.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
            public void onProgressChanged(SeekBar s,int n,boolean fromUser) {
                if (fromUser) {difficulty=n+1; difficultyLabel.setText("Difficulty: "+DozenMath.format(difficulty)+" / 5");save();newProblem();}
            }
        });
        statsLabel=label("",14,CYAN,false);
        statsLabel.setGravity(Gravity.CENTER);
        body.addView(statsLabel);
        addSpace(body,13);
        LinearLayout card=vertical();
        card.setPadding(dp(14),dp(20),dp(14),dp(18));
        card.setBackground(round(SURFACE,16));
        body.addView(card,new LinearLayout.LayoutParams(-1,-2));
        TextView eyebrow=label("SOLVE IN BASE TWELVE",12,CYAN,true);
        eyebrow.setGravity(Gravity.CENTER);
        card.addView(eyebrow);
        addSpace(card,16);
        problemLabel=label("",28,FG,true);
        problemLabel.setGravity(Gravity.CENTER);
        problemLabel.setMinHeight(dp(80));
        card.addView(problemLabel);
        answerLabel=label("_",32,GREEN,true);
        answerLabel.setGravity(Gravity.CENTER);
        answerLabel.setMinHeight(dp(56));
        card.addView(answerLabel);
        feedbackLabel=label("",15,MUTED,false);
        feedbackLabel.setGravity(Gravity.CENTER);
        feedbackLabel.setMinHeight(dp(26));
        card.addView(feedbackLabel);
        hintLabel=label("",13,MUTED,false);
        hintLabel.setGravity(Gravity.CENTER);
        card.addView(hintLabel);
        addSpace(body,16);
        String[] rows={"1","2","3","4","5","6","7","8","9","↊","↋","0"};
        for (int r=0;r<3;r++) {
            LinearLayout line=horizontal();
            body.addView(line,new LinearLayout.LayoutParams(-1,dp(62)));
            for (int c=0;c<4;c++) {
                String digit=rows[r*4+c];
                Button key=button(digit,SURFACE,FG);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1f);
                lp.setMargins(dp(3),dp(3),dp(3),dp(3));
                line.addView(key,lp);
                key.setOnClickListener(v->append(digit));
            }
        }
        LinearLayout actions=horizontal();
        body.addView(actions,new LinearLayout.LayoutParams(-1,dp(60)));
        Button minus=button("−",SURFACE,FG), back=button("⌫",SURFACE,FG);
        submitButton=button("CHECK",PURPLE,BG);
        for (Button btn: new Button[] {minus,back,submitButton}) {
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,btn==submitButton?2f:1f);
            lp.setMargins(dp(3),dp(4),dp(3),dp(3));
            actions.addView(btn,lp);
        }
        minus.setOnClickListener(v->{if(!solved && input.length()==0){input.append('−');displayInput();}});
        back.setOnClickListener(v->{if(!solved && input.length()>0){input.deleteCharAt(input.length()-1);displayInput();}});
        submitButton.setOnClickListener(v->{if(solved)newProblem();else check();});
        addSpace(body,12);
        TextView foot=label("100₁₂ = one gross (144 in decimal). All exercises, operands, answers, and results use base 12. Answers can also use A for ↊ and B for ↋ when typed externally.",12,MUTED,false);
        foot.setGravity(Gravity.CENTER);
        body.addView(foot);
        updateStats();
        newProblem();
    }
    private View styledItem(View v,int color) {
        if (v instanceof TextView) {TextView t=(TextView)v;t.setText(Numerals.render(t.getText()));t.setTextColor(color);t.setTextSize(16);t.setPadding(dp(8),dp(10),dp(8),dp(10));}
        return v;
    }
    private LinearLayout vertical() {LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout horizontal(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);return v;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView label(String value,int sp,int color,boolean bold) {
        TextView v=new Numerals.NumeralTextView(this); v.setText(value);v.setTextColor(color);v.setTextSize(sp);
        if (bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private GradientDrawable round(int color,int radius) {
        GradientDrawable drawable=new GradientDrawable();drawable.setColor(color);drawable.setCornerRadius(dp(radius));return drawable;
    }
    private Button button(String value,int bg,int fg) {
        Button b=new Numerals.NumeralButton(this);b.setText(value);b.setTextSize(20);b.setAllCaps(false);
        b.setTextColor(fg);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackground(round(bg,10));return b;
    }
    private void addSpace(LinearLayout v,int height) {v.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private void append(String digit) {
        if (solved || input.length()>14) return;
        input.append(digit);displayInput();
        autoAcceptIfCorrect();
    }
    private void displayInput() {
        answerLabel.setText(input.length()==0?"_":input.toString());
        feedbackLabel.setText("");
    }
    private void autoAcceptIfCorrect() {
        if (!solved && current != null && DozenMath.isCorrect(input.toString(), current.answer)) {
            check();
        }
    }

    private void newProblem() {
        cancelAutoAdvance();
        if (problemLabel==null) return;
        questionSerial++;
        current=engine.next(mode,difficulty);
        input.setLength(0);
        solved=false;
        problemLabel.setText(current.prompt);
        hintLabel.setText(current.mode);
        feedbackLabel.setText("");
        answerLabel.setText("_");
        submitButton.setText("CHECK");
        updateStats();
    }
    private void check() {
        if (solved || current == null) return;
        Long entered=DozenMath.parse(input.toString());
        if (entered==null) {feedbackLabel.setTextColor(RED);feedbackLabel.setText("Enter a base-12 number.");return;}
        attempted++;
        solved=true;
        if (entered == current.answer) {
            correct++;streak++;best=Math.max(best,streak);
            feedbackLabel.setTextColor(GREEN);
            feedbackLabel.setText("Correct! ✓");
            advanceAfterCorrect();
        } else {
            streak=0;
            feedbackLabel.setTextColor(RED);
            feedbackLabel.setText("Answer: " + DozenMath.format(current.answer));
        }
        submitButton.setText("NEXT →");
        hintLabel.setText(current.details);
        save(); updateStats();
    }
    private void updateStats() {
        if (statsLabel != null) statsLabel.setText("Correct " + DozenMath.format(correct) + "/" + DozenMath.format(attempted)
            + "   ·   Streak " + DozenMath.format(streak) + "   ·   Best " + DozenMath.format(best));
    }
    private void save() {
        prefs.edit().putInt("mode",mode).putInt("difficulty",difficulty)
            .putInt("correct",correct).putInt("attempted",attempted).putInt("best",best).apply();
    }
}
