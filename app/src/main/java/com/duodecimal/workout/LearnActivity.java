package com.duodecimal.workout;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Discover the three focused teaching tools without mixing them with drills. */
public final class LearnActivity extends Activity {
    private static final int BG=0xFF282A36, SURFACE=0xFF44475A;
    private static final int FG=0xFFF8F8F2, PURPLE=0xFFBD93F9;
    private static final int CYAN=0xFF8BE9FD, MUTED=0xFFB5B8C6;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);
        root.setPadding(dp(16),dp(18),dp(16),dp(30));
        root.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(root);
        setContentView(scroll);
        AppNavigation.add(this,root,AppNavigation.LEARN);
        TextView title=label("Learn",30,PURPLE,true);
        root.addView(title);
        TextView subtitle=label("Understand the glyphs, then practice their patterns.",14,MUTED,false);
        root.addView(subtitle);
        gap(root,22);
        section(root,"Recognize","Build instant recall of all twelve symbols.");
        destination(root,"Glyph recognition","Match each custom glyph to its familiar value, or reverse the mapping.",
            GlyphRecognitionActivity.class);
        gap(root,20);
        section(root,"Understand","Use the cap–body structure to calculate visually.");
        destination(root,"Visual addition","Practice cap and body sums, with optional carry recognition.",
            VisualAdditionActivity.class);
        gap(root,10);
        destination(root,"Visual multiplication","Multiply cap and body residues, then reconstruct the last digit and carry.",
            VisualMultiplicationActivity.class);
        gap(root,24);
        TextView note=label("Your numerals encode digit mod 3 in the cap and digit mod 4 in the body. These two components uniquely determine every base-twelve digit.",13,MUTED,false);
        root.addView(note);
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color) {
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(color);drawable.setCornerRadius(dp(14));
        return drawable;
    }
    private TextView label(String value,int size,int color,boolean bold) {
        TextView v=new TextView(this);v.setText(value);v.setTextColor(color);
        v.setTextSize(size);v.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);
        return v;
    }
    private void gap(LinearLayout parent,int height) {
        TextView spacer=new TextView(this);
        parent.addView(spacer,new LinearLayout.LayoutParams(1,dp(height)));
    }
    private void section(LinearLayout parent,String name,String description) {
        parent.addView(label(name,19,FG,true));
        gap(parent,4);
        parent.addView(label(description,13,MUTED,false));
        gap(parent,12);
    }
    private void destination(LinearLayout parent,String title,String detail,Class<?> target){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18),dp(16),dp(18),dp(16));
        card.setBackground(background(SURFACE));
        card.setClickable(true);card.setFocusable(true);
        TextView name=label(title+"   ›",19,CYAN,true);
        card.addView(name);
        gap(card,5);
        card.addView(label(detail,14,FG,false));
        card.setContentDescription(title+". "+detail);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.minimumHeight=dp(80);
        parent.addView(card,lp);
        card.setOnClickListener(v->startActivity(new Intent(this,target)));
    }
}
