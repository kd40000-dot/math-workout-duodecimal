package com.duodecimal.workout;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared native widgets for the beginner tutorial and reference guide. */
final class GuideUi {
    static final int BG=0xFF282A36, PANEL=0xFF44475A, DARK=0xFF343644;
    static final int FG=0xFFF8F8F2, MUTED=0xFFB5B8C6, PURPLE=0xFFBD93F9;
    static final int CYAN=0xFF8BE9FD, GREEN=0xFF50FA7B, RED=0xFFFF5555;
    static final String[] CAPS={"Bar","Breve","Dot"};
    static final String[] BODIES={"Loop","Stem","Vee","El"};

    private GuideUi() {}
    static int dp(Activity activity,int value){
        return Math.round(value*activity.getResources().getDisplayMetrics().density);
    }
    static GradientDrawable shape(Activity a,int color,int radius){
        GradientDrawable result=new GradientDrawable();
        result.setColor(color);
        result.setCornerRadius(dp(a,radius));
        return result;
    }
    static LinearLayout column(Activity a){
        LinearLayout l=new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    static LinearLayout row(Activity a){
        LinearLayout l=new LinearLayout(a);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    static TextView text(Activity a,String value,int sp,int color,boolean bold){
        // Ordinary TextViews ensure explanations and decimal reference values
        // never get converted into glyphs.
        TextView view=new TextView(a);
        view.setText(value);view.setTextSize(sp);view.setTextColor(color);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }
    static TextView glyph(Activity a,int value,int sp,int color){
        Numerals.NumeralTextView view=new Numerals.NumeralTextView(a);
        view.setText(DozenMath.format(value));
        view.setTextColor(color);view.setTextSize(sp);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription("Custom glyph for "+value);
        return view;
    }
    static Button button(Activity a,String value,boolean primary){
        Button b=new Button(a);
        b.setAllCaps(false);
        b.setText(value);
        b.setTextSize(16);
        b.setTextColor(primary?BG:FG);
        b.setBackground(shape(a,primary?PURPLE:PANEL,11));
        b.setMinHeight(dp(a,52));
        return b;
    }
    static void gap(Activity a,LinearLayout parent,int dp){
        parent.addView(new View(a),
            new LinearLayout.LayoutParams(1,GuideUi.dp(a,dp)));
    }
    static LinearLayout card(Activity a,LinearLayout parent){
        LinearLayout c=column(a);
        c.setBackground(shape(a,PANEL,15));
        c.setPadding(dp(a,15),dp(a,15),dp(a,15),dp(a,15));
        parent.addView(c,new LinearLayout.LayoutParams(-1,-2));
        return c;
    }
    static void copy(Activity a,LinearLayout parent,String value){
        TextView t=text(a,value,15,FG,false);
        t.setLineSpacing(dp(a,3),1f);
        parent.addView(t);
    }
    static void caption(Activity a,LinearLayout parent,String value){
        TextView t=text(a,value,13,MUTED,false);
        t.setLineSpacing(dp(a,2),1f);
        parent.addView(t);
    }
    static void title(Activity a,LinearLayout parent,String value){
        parent.addView(text(a,value,24,PURPLE,true));
        gap(a,parent,12);
    }
    /** Short 12-piece bar for making fraction sizes visible. */
    static void fraction(Activity a,LinearLayout parent,int numerator,String title,String detail){
        parent.addView(text(a,title,17,FG,true));
        gap(a,parent,5);
        LinearLayout pieces=row(a);
        parent.addView(pieces,new LinearLayout.LayoutParams(-1,dp(a,32)));
        for(int i=0;i<12;i++){
            View tile=new View(a);
            tile.setBackground(shape(a,i<numerator?CYAN:DARK,4));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
            p.setMargins(dp(a,2),dp(a,2),dp(a,2),dp(a,2));
            pieces.addView(tile,p);
        }
        gap(a,parent,4);
        caption(a,parent,detail);
    }
    static void gallery(Activity a,LinearLayout parent,boolean showNames){
        for(int r=0;r<4;r++){
            LinearLayout line=row(a);
            parent.addView(line,new LinearLayout.LayoutParams(-1,-2));
            for(int c=0;c<3;c++){
                int digit=r*3+c;
                LinearLayout item=column(a);
                item.setGravity(Gravity.CENTER_HORIZONTAL);
                item.setBackground(shape(a,PANEL,11));
                item.setPadding(dp(a,3),dp(a,8),dp(a,3),dp(a,8));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
                p.setMargins(dp(a,3),dp(a,3),dp(a,3),dp(a,3));
                line.addView(item,p);
                TextView glyph=glyph(a,digit,44,FG);
                item.addView(glyph,new LinearLayout.LayoutParams(-1,dp(a,64)));
                TextView number=text(a,Integer.toString(digit),18,CYAN,true);
                number.setGravity(Gravity.CENTER);
                item.addView(number,new LinearLayout.LayoutParams(-1,dp(a,26)));
                if(showNames){
                    TextView name=text(a,CAPS[digit%3]+" / "+BODIES[digit%4],
                        11,MUTED,false);
                    name.setGravity(Gravity.CENTER);
                    item.addView(name,new LinearLayout.LayoutParams(-1,dp(a,32)));
                }
            }
        }
    }
}
