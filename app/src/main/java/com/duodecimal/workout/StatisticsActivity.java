package com.duodecimal.workout;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** A small AnkiDroid-style stats destination backed by actual saved fact data. */
public final class StatisticsActivity extends Activity {
    private static final int BG=0xFF282A36, SURFACE=0xFF44475A, HEADER=0xFF363849;
    private static final int FG=0xFFF8F8F2, MUTED=0xFFB5B8C6, PURPLE=0xFFBD93F9;
    private static final int CYAN=0xFF8BE9FD, GREEN=0xFF50FA7B;
    private SharedPreferences prefs;
    private LinearLayout root;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("duodecimal_workout",MODE_PRIVATE);
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(12),dp(16),dp(32));
        scroll.addView(root);
        setContentView(scroll);
    }
    @Override protected void onResume(){super.onResume();draw();}
    @Override public void onBackPressed(){
        if(DrawerNavigation.close(this))return;
        super.onBackPressed();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String text,int size,int color,boolean bold){
        TextView result=new Numerals.NumeralTextView(this);
        result.setText(text);result.setTextSize(size);result.setTextColor(color);
        if(bold)result.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return result;
    }
    private GradientDrawable shape(int color) {
        GradientDrawable bg=new GradientDrawable();bg.setColor(color);
        bg.setCornerRadius(dp(14));return bg;
    }
    private void gap(int n){root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(n)));}
    private void draw(){
        root.removeAllViews();
        DrawerNavigation.addBar(this,root,"Statistics",DrawerNavigation.STATS);
        root.addView(label("Your progress",28,PURPLE,true));
        gap(8);
        root.addView(label("Accuracy, fluency and mastery for the twelve-digit tables. Your results are stored only on this device.",
            14,MUTED,false));
        gap(18);
        int tries=0,correct=0,mastered=0,used=0,total=0;
        double weightedMs=0;
        for(int op=0;op<4;op++){
            AdaptiveFacts model=new AdaptiveFacts(prefs,op,1,1,0,0);
            List<Long> candidates=model.candidates(op,1,1,0,0);
            for(long key:candidates){
                total++;
                int n=model.attempts(key);
                tries+=n;correct+=model.correct(key);
                if(n>0){
                    used++;
                    weightedMs+=model.recentCorrectMs(key)*n;
                }
                if(model.mastered(key))mastered++;
            }
        }
        LinearLayout overview=card();
        overview.addView(label(DozenMath.format(mastered)+" / "+DozenMath.format(total),
            27,GREEN,true));
        overview.addView(label("One-digit facts currently mastered",13,MUTED,false));
        innerGap(overview,10);
        addProgress(overview,mastered,total);
        innerGap(overview,14);
        overview.addView(label("Accuracy  "+(tries==0?"—":DozenMath.format(correct*100L/tries)+"%")+
            "     Attempts  "+DozenMath.format(tries),15,FG,true));
        innerGap(overview,7);
        overview.addView(label("Average recent response  "+
            (tries==0?"—":DozenMath.format(Math.round(weightedMs/tries/1000))+" s"),
            14,MUTED,false));
        gap(20);
        root.addView(label("By operation",20,FG,true));
        gap(10);
        for(int op=0;op<4;op++){
            AdaptiveFacts model=new AdaptiveFacts(prefs,op,1,1,0,0);
            List<Long> candidates=model.candidates(op,1,1,0,0);
            int n=0,hits=0,master=0,attempted=0;
            for(long key:candidates){
                n+=model.attempts(key);hits+=model.correct(key);
                if(model.mastered(key))master++;
                if(model.attempts(key)>0)attempted++;
            }
            LinearLayout card=card();
            String operation=PracticeGrid.OPERATIONS[op].split(" \\(")[0];
            card.addView(label(operation,18,CYAN,true));
            innerGap(card,5);
            card.addView(label("Practiced "+DozenMath.format(attempted)+"/"+
                DozenMath.format(candidates.size())+" · Mastered "+DozenMath.format(master),
                14,FG,false));
            innerGap(card,11);
            addProgress(card,master,candidates.size());
            innerGap(card,9);
            card.addView(label("First-table accuracy  "+
                (n==0?"—":DozenMath.format(100L*hits/n)+"%"),13,MUTED,false));
            gap(10);
        }
        gap(18);
        root.addView(label("Mastery follows your current thresholds in Settings. Changing a target updates these figures without deleting your history.",13,MUTED,false));
    }
    private LinearLayout card(){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(14),dp(16),dp(14));
        card.setBackground(shape(SURFACE));
        root.addView(card,new LinearLayout.LayoutParams(-1,-2));
        return card;
    }
    private void innerGap(LinearLayout parent,int amount){
        parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(amount)));
    }
    private void addProgress(LinearLayout parent,int value,int maximum){
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1,maximum));
        bar.setProgress(value);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(HEADER));
        bar.setContentDescription(value+" mastered of "+maximum+" facts");
        parent.addView(bar,new LinearLayout.LayoutParams(-1,dp(10)));
    }
}
