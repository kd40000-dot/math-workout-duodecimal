package com.duodecimal.workout;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Consistent top-level destination navigation for the three peer screens.
 * A navigation item changes destinations; the main practice CTA starts a drill.
 */
public final class AppNavigation {
    public static final int PRACTICE=0, LEARN=1, MORE=2;
    private static final int INK=0xFFF8F8F2, SURFACE=0xFF44475A;
    private static final int SELECTED=0xFFBD93F9, BG=0xFF282A36;
    private static final String[] LABELS={"Practice","Learn","More"};
    private static final Class<?>[] DESTINATIONS={
        CustomPracticeActivity.class, LearnActivity.class, MainActivity.class
    };
    private AppNavigation(){}
    private static int dp(Activity activity,int n) {
        return Math.round(activity.getResources().getDisplayMetrics().density*n);
    }
    private static GradientDrawable shape(Activity activity,int color) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(activity,12));return d;
    }
    public static void add(Activity activity,LinearLayout parent,int current) {
        LinearLayout bar=new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setContentDescription("Main navigation");
        LinearLayout.LayoutParams outer=new LinearLayout.LayoutParams(-1,dp(activity,56));
        outer.bottomMargin=dp(activity,18);
        parent.addView(bar,outer);
        for(int i=0;i<LABELS.length;i++){
            final int destination=i;
            TextView item=new TextView(activity);
            boolean active=i==current;
            item.setText(LABELS[i]);
            item.setTextColor(active?BG:INK);
            item.setTextSize(15);
            item.setTypeface(Typeface.DEFAULT,active?Typeface.BOLD:Typeface.NORMAL);
            item.setGravity(Gravity.CENTER);
            item.setBackground(shape(activity,active?SELECTED:SURFACE));
            item.setContentDescription(LABELS[i]+(active?", current screen":" tab"));
            item.setClickable(true);
            item.setFocusable(true);
            item.setSelected(active);
            LinearLayout.LayoutParams child=new LinearLayout.LayoutParams(0,-1,1);
            child.setMargins(dp(activity,3),0,dp(activity,3),0);
            bar.addView(item,child);
            item.setOnClickListener(v->{
                if(destination==current)return;
                Intent intent=new Intent(activity,DESTINATIONS[destination]);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            });
        }
    }
}
