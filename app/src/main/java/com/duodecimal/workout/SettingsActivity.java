package com.duodecimal.workout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import android.widget.Switch;
import android.widget.TextView;

/**
 * Keybr-inspired learning preferences. Every setting is used by AdaptiveFacts;
 * existing per-fact history is never deleted when a threshold changes.
 */
public final class SettingsActivity extends Activity {
    private static final int BG=0xFF282A36, SURFACE=0xFF44475A, HEADER=0xFF363849;
    private static final int FG=0xFFF8F8F2, MUTED=0xFFB5B8C6, PURPLE=0xFFBD93F9;
    private static final int CYAN=0xFF8BE9FD;
    private SharedPreferences prefs;
    private LinearLayout root;
    private ScrollView scroll;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("duodecimal_workout",MODE_PRIVATE);
        scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        root=vertical();
        root.setPadding(dp(16),dp(12),dp(16),dp(32));
        scroll.addView(root);
        setContentView(scroll);
        draw();
    }
    @Override public void onBackPressed() {
        if(DrawerNavigation.close(this))return;
        super.onBackPressed();
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private LinearLayout vertical() {
        LinearLayout view=new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }
    private GradientDrawable rounded(int color) {
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(14));
        return bg;
    }
    private TextView label(String text,int size,int color,boolean bold) {
        TextView view=new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }
    private void gap(LinearLayout parent,int size) {
        parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(size)));
    }
    private LinearLayout section(String title,String summary){
        gap(root,22);
        root.addView(label(title,20,FG,true));
        gap(root,4);
        root.addView(label(summary,13,MUTED,false));
        gap(root,10);
        LinearLayout section=vertical();
        section.setPadding(dp(14),dp(12),dp(14),dp(12));
        section.setBackground(rounded(SURFACE));
        root.addView(section,new LinearLayout.LayoutParams(-1,-2));
        return section;
    }
    private void slider(LinearLayout group,String title,String description,
                        String key,int value,int min,int max,int step,String suffix){
        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView name=label(title,15,FG,true);
        top.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        TextView valueText=label(value+suffix,15,CYAN,true);
        valueText.setGravity(Gravity.END);
        top.addView(valueText);
        group.addView(top);
        SeekBar seek=new SeekBar(this);
        seek.setMax((max-min)/step);
        seek.setProgress((value-min)/step);
        seek.setProgressTintList(ColorStateList.valueOf(PURPLE));
        seek.setThumbTintList(ColorStateList.valueOf(PURPLE));
        seek.setContentDescription(title+", "+value+suffix);
        LinearLayout.LayoutParams seekParams=new LinearLayout.LayoutParams(-1,dp(48));
        group.addView(seek,seekParams);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
            public void onProgressChanged(SeekBar s,int progress,boolean user){
                if(!user)return;
                int current=min+progress*step;
                valueText.setText(current+suffix);
                s.setContentDescription(title+", "+current+suffix);
                prefs.edit().putInt(key,key.equals(AdaptiveSettings.TARGET_MS)?
                    current*1000:current).apply();
            }
        });
        group.addView(label(description,12,MUTED,false));
        gap(group,16);
    }
    private void toggle(LinearLayout group,String title,String description,
                        String key,boolean enabled){
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout copy=vertical();
        copy.addView(label(title,15,FG,true));
        gap(copy,3);
        copy.addView(label(description,12,MUTED,false));
        row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        Switch sw=new Switch(this);
        sw.setText("");
        sw.setChecked(enabled);
        sw.setContentDescription(title);
        sw.setThumbTintList(ColorStateList.valueOf(PURPLE));
        sw.setTrackTintList(ColorStateList.valueOf(0xFF777B90));
        LinearLayout.LayoutParams control=new LinearLayout.LayoutParams(dp(60),dp(54));
        control.leftMargin=dp(12);
        row.addView(sw,control);
        row.setMinimumHeight(dp(76));
        row.setClickable(true);
        row.setOnClickListener(v->sw.setChecked(!sw.isChecked()));
        sw.setOnCheckedChangeListener((button,checked)->{
            prefs.edit().putBoolean(key,checked).apply();
        });
        group.addView(row,new LinearLayout.LayoutParams(-1,-2));
        gap(group,12);
    }
    private Spinner spinner(String[] options,int selected){
        Spinner sp=new Spinner(this,Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(
                this,android.R.layout.simple_spinner_item,options){
            @Override public View getView(int position,View convert,ViewGroup parent){
                return style(super.getView(position,convert,parent));
            }
            @Override public View getDropDownView(int position,View convert,ViewGroup parent){
                return style(super.getDropDownView(position,convert,parent));
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        sp.setBackground(rounded(HEADER));
        sp.setPopupBackgroundDrawable(rounded(HEADER));
        sp.setSelection(selected);
        return sp;
    }
    private View style(View item) {
        if(item instanceof TextView){
            TextView text=(TextView)item;
            text.setTextColor(FG);
            text.setTextSize(15);
            text.setPadding(dp(12),dp(12),dp(8),dp(12));
        }
        return item;
    }
    private void draw(){
        root.removeAllViews();
        DrawerNavigation.addBar(this,root,"Settings",DrawerNavigation.SETTINGS);
        root.addView(label("Learning settings",28,PURPLE,true));
        gap(root,6);
        root.addView(label("Control when facts become fluent and when new ones unlock. Changes apply immediately and keep your history.",
            14,MUTED,false));

        LinearLayout fluency=section("Fluency target",
            "The equivalent of Keybr's target typing speed is how quickly you answer a fact correctly.");
        slider(fluency,"Target answer time",
            "A fact meets the speed goal when its recent correct-answer time is at or below this value.",
            AdaptiveSettings.TARGET_MS,AdaptiveSettings.targetMs(prefs)/1000,2,30,1," s");
        toggle(fluency,"Prioritize slow facts",
            "Give more practice to facts that take longer than your target.",
            AdaptiveSettings.PRIORITIZE_SLOW,AdaptiveSettings.prioritizeSlow(prefs));

        LinearLayout mastery=section("Mastery criteria",
            "A fact must satisfy every selected requirement to be considered mastered.");
        slider(mastery,"Minimum accuracy",
            "Fraction of all recorded responses to this fact that must be correct.",
            AdaptiveSettings.ACCURACY,AdaptiveSettings.accuracy(prefs),50,100,5,"%");
        slider(mastery,"Correct answers required",
            "Minimum total correct responses before a fact can be mastered.",
            AdaptiveSettings.CORRECT,AdaptiveSettings.minCorrect(prefs),2,10,1,"");
        slider(mastery,"Consecutive correct",
            "Required current streak of correct answers.",
            AdaptiveSettings.STREAK,AdaptiveSettings.streak(prefs),1,6,1,"");

        LinearLayout unlock=section("Progressive unlocking",
            "Like Keybr, new facts are introduced after earlier facts meet your goals.");
        toggle(unlock,"Require all unlocked facts",
            "Off: use the percentage below. On: every unlocked fact must meet mastery.",
            AdaptiveSettings.REQUIRE_ALL,AdaptiveSettings.requireAll(prefs));
        toggle(unlock,"Recheck previous mastery",
            "Off: once mastered is enough. On: older facts must still meet today's target before unlocking more.",
            AdaptiveSettings.RECHECK,AdaptiveSettings.recheck(prefs));
        slider(unlock,"Unlock threshold",
            "Percentage of unlocked facts that must have met mastery (unless require all is on).",
            AdaptiveSettings.UNLOCK_PERCENT,AdaptiveSettings.unlockPercent(prefs),50,100,5,"%");
        slider(unlock,"Starting facts",
            "Number available when beginning an untouched table page.",
            AdaptiveSettings.INITIAL,AdaptiveSettings.initial(prefs),2,12,1,"");
        slider(unlock,"Facts per unlock",
            "How many new facts appear after meeting the unlock requirement.",
            AdaptiveSettings.BATCH,AdaptiveSettings.batch(prefs),1,6,1,"");

        LinearLayout ordering=section("Fact order",
            "Choose which facts are introduced first. This does not erase existing answer statistics.");
        ordering.addView(label("Introduction order",15,FG,true));
        gap(ordering,8);
        Spinner choices=spinner(AdaptiveSettings.ORDERS,AdaptiveSettings.order(prefs));
        ordering.addView(choices,new LinearLayout.LayoutParams(-1,dp(56)));
        choices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(AdapterView<?> parent){}
            @Override public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){
                prefs.edit().putInt(AdaptiveSettings.ORDER,pos).apply();
            }
        });
        gap(ordering,10);
        ordering.addView(label("The unlocked count stays saved when you change order. Already recorded facts retain their scores, but the order of introduced facts may change.",12,MUTED,false));

        LinearLayout sessions=section("Practice sessions",
            "Control session length and whether already fluent facts still appear.");
        slider(sessions,"Questions per session",
            "Adaptive sessions use this many questions; manual sessions still use your selected repetitions.",
            AdaptiveSettings.QUESTIONS,AdaptiveSettings.questions(prefs),12,60,12,"");
        toggle(sessions,"Review mastered facts",
            "Occasionally show older mastered facts to maintain fluency.",
            AdaptiveSettings.REVIEW_MASTERED,AdaptiveSettings.reviewMastered(prefs));

        gap(root,24);
        Button defaults=new Button(this);
        defaults.setAllCaps(false);
        defaults.setText("Restore learning defaults");
        defaults.setTextSize(15);
        defaults.setTextColor(FG);
        defaults.setBackground(rounded(HEADER));
        root.addView(defaults,new LinearLayout.LayoutParams(-1,dp(54)));
        defaults.setOnClickListener(v->
            new AlertDialog.Builder(this)
                .setTitle("Restore default settings?")
                .setMessage("This resets only learning preferences. All accuracy, response-time, and unlocked-fact progress is kept.")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Restore",(dialog,which)->{restore();draw();})
                .show());
        gap(root,22);
    }
    private void restore(){
        SharedPreferences.Editor edit=prefs.edit();
        String[] keys={AdaptiveSettings.TARGET_MS,AdaptiveSettings.ACCURACY,
            AdaptiveSettings.CORRECT,AdaptiveSettings.STREAK,
            AdaptiveSettings.UNLOCK_PERCENT,AdaptiveSettings.REQUIRE_ALL,
            AdaptiveSettings.RECHECK,AdaptiveSettings.ORDER,
            AdaptiveSettings.INITIAL,AdaptiveSettings.BATCH,
            AdaptiveSettings.QUESTIONS,AdaptiveSettings.PRIORITIZE_SLOW,
            AdaptiveSettings.REVIEW_MASTERED};
        for(String key:keys)edit.remove(key);
        edit.apply();
    }
}
