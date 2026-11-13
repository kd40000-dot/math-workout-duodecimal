package com.duodecimal.workout;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/** Bidirectional glyph memorization. Arabic references are plain TextViews. */
public final class GlyphRecognitionActivity extends Activity {
    private static final int BG=0xFF282A36, SURFACE=0xFF44475A, FG=0xFFF8F8F2;
    private static final int CYAN=0xFF8BE9FD, PURPLE=0xFFBD93F9, GREEN=0xFF50FA7B;
    private static final int RED=0xFFFF5555, MUTED=0xFFB5B8C6;
    private static final String[] CAPS={"Bar","Breve","Dot"};
    private static final String[] BODIES={"Loop","Stem","Vee","El"};
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Random random=new Random();
    private final ArrayList<Integer> deck=new ArrayList<>();
    private Runnable pending;
    private ScrollView scroll;
    private LinearLayout root;
    private TextView feedback,stats;
    private SharedPreferences prefs;
    private int mode=0,index=0,perfect=0,asked=0;
    private boolean missed=false,locked=false;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("duodecimal_workout",MODE_PRIVATE);
        mode=prefs.getInt("recognition_direction",0);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(18),dp(16),dp(30));
        scroll.addView(root);setContentView(scroll);setup();
    }
    @Override protected void onDestroy(){cancel();super.onDestroy();}
    @Override public void onBackPressed(){if(DrawerNavigation.close(this))return;if(!deck.isEmpty())setup();else super.onBackPressed();}
    private void cancel(){if(pending!=null){handler.removeCallbacks(pending);pending=null;}}
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(10));return d;}
    private TextView text(String str,int size,int color,boolean glyph){
        TextView t=glyph?new Numerals.NumeralTextView(this):new TextView(this);
        t.setText(str);t.setTextColor(color);t.setTextSize(size);t.setGravity(Gravity.CENTER);return t;
    }
    private Button button(String str,int bgColor){
        Button b=new Button(this);b.setAllCaps(false);b.setText(str);b.setTextColor(FG);
        b.setTextSize(15);b.setBackground(bg(bgColor));return b;
    }
    private void gap(int h){root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private void header(){
        DrawerNavigation.addBar(this,root,"Glyph recognition",DrawerNavigation.LEARN);
        root.addView(text("Glyph Recognition",27,PURPLE,false),
            new LinearLayout.LayoutParams(-1,dp(57)));
    }
    private void setup(){
        cancel();deck.clear();root.removeAllViews();header();
        root.addView(text("Memorize the Arabic value of each of our twelve cap–body glyphs.",
            16,FG,false),new LinearLayout.LayoutParams(-1,dp(70)));
        gap(15);
        Button a=button("GLYPH → ARABIC NUMBER",mode==0?0xFF626583:SURFACE);
        root.addView(a,new LinearLayout.LayoutParams(-1,dp(60)));
        a.setOnClickListener(v->{mode=0;save();start();});
        gap(9);
        Button b=button("ARABIC NUMBER → GLYPH",mode==1?0xFF626583:SURFACE);
        root.addView(b,new LinearLayout.LayoutParams(-1,dp(60)));
        b.setOnClickListener(v->{mode=1;save();start();});
        gap(20);
        root.addView(text("Two shuffled passes of all 12 symbols per session. Correct answers advance automatically; mistakes can be retried and do not count as first-try correct.",
            14,MUTED,false),new LinearLayout.LayoutParams(-1,dp(100)));
        int trials=prefs.getInt("glyph_trials_"+mode,0);
        int hits=prefs.getInt("glyph_hits_"+mode,0);
        root.addView(text("Past first-try accuracy: "+hits+" / "+trials,14,CYAN,false),
            new LinearLayout.LayoutParams(-1,dp(42)));
    }
    private void save(){prefs.edit().putInt("recognition_direction",mode).apply();}
    private void start(){
        cancel();deck.clear();index=perfect=asked=0;
        for(int pass=0;pass<2;pass++){
            ArrayList<Integer> values=new ArrayList<>();
            for(int d=0;d<12;d++)values.add(d);
            Collections.shuffle(values,random);deck.addAll(values);
        }
        showQuestion();
    }
    private void showQuestion(){
        cancel();if(index>=deck.size()){finishSession();return;}
        missed=locked=false;root.removeAllViews();header();
        int target=deck.get(index);
        stats=text("Question "+(index+1)+" / "+deck.size()+
            "   ·   First try "+perfect+" / "+asked,14,CYAN,false);
        root.addView(stats,new LinearLayout.LayoutParams(-1,dp(43)));
        gap(14);
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(9),dp(12),dp(9),dp(12));card.setBackground(bg(SURFACE));
        root.addView(card,new LinearLayout.LayoutParams(-1,-2));
        card.addView(text(mode==0?"IDENTIFY THIS GLYPH":"CHOOSE THE MATCHING GLYPH",13,CYAN,false),
            new LinearLayout.LayoutParams(-1,dp(42)));
        TextView large=text(mode==0?DozenMath.format(target):Integer.toString(target),
            72,FG,mode==0);
        card.addView(large,new LinearLayout.LayoutParams(-1,dp(116)));
        feedback=text("",14,GREEN,false);
        card.addView(feedback,new LinearLayout.LayoutParams(-1,dp(84)));
        gap(14);
        for(int r=0;r<3;r++){
            LinearLayout line=new LinearLayout(this);
            root.addView(line,new LinearLayout.LayoutParams(-1,dp(78)));
            for(int c=0;c<4;c++){
                final int d=r*4+c;
                TextView key=text(mode==0?Integer.toString(d):DozenMath.format(d),
                    mode==0?27:35,FG,mode==1);
                key.setBackground(bg(SURFACE));key.setClickable(true);key.setFocusable(true);
                key.setContentDescription("Digit "+d);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
                lp.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(key,lp);
                key.setOnClickListener(v->choose(target,d));
            }
        }
        gap(12);
        Button end=button("END SESSION / CHANGE DIRECTION",SURFACE);
        root.addView(end,new LinearLayout.LayoutParams(-1,dp(55)));
        end.setOnClickListener(v->setup());
        scroll.post(()->scroll.scrollTo(0,0));
    }
    private void choose(int target,int selected){
        if(locked)return;
        if(target!=selected){
            missed=true;feedback.setTextColor(RED);
            feedback.setText("Not quite. Look for "+CAPS[target%3].toLowerCase()+
                " cap and "+BODIES[target%4].toLowerCase()+" body.");
            return;
        }
        locked=true;asked++;
        if(!missed)perfect++;
        prefs.edit().putInt("glyph_trials_"+mode,prefs.getInt("glyph_trials_"+mode,0)+1)
            .putInt("glyph_hits_"+mode,prefs.getInt("glyph_hits_"+mode,0)+(missed?0:1)).apply();
        feedback.setTextColor(GREEN);
        feedback.setText("Correct: "+target+" = "+CAPS[target%3]+" cap + "+
            BODIES[target%4]+" body.");
        stats.setText("Question "+(index+1)+" / "+deck.size()+
            "   ·   First try "+perfect+" / "+asked);
        index++;
        pending=()->{pending=null;if(!isFinishing()&&!isDestroyed())showQuestion();};
        handler.postDelayed(pending,1250);
    }
    private void finishSession(){
        deck.clear();root.removeAllViews();header();
        root.addView(text("Session complete",25,GREEN,false),
            new LinearLayout.LayoutParams(-1,dp(85)));
        root.addView(text("First-try correct: "+perfect+" / "+asked,20,FG,false),
            new LinearLayout.LayoutParams(-1,dp(65)));
        Button again=button("PRACTICE AGAIN",SURFACE);
        root.addView(again,new LinearLayout.LayoutParams(-1,dp(59)));
        again.setOnClickListener(v->start());gap(10);
        Button modes=button("CHANGE DIRECTION",SURFACE);
        root.addView(modes,new LinearLayout.LayoutParams(-1,dp(59)));
        modes.setOnClickListener(v->setup());
    }
}
