package com.duodecimal.workout;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** AnkiDroid-inspired left navigation drawer, with a single app-bar menu. */
public final class DrawerNavigation {
    public static final int PRACTICE=0, BROWSE=1, LEARN=2, STATS=3, MORE=4, SETTINGS=5;
    private static final String TAG="duodecimal-navigation-drawer";
    private static final int BG=0xFF282A36, PANEL=0xFF343644, SURFACE=0xFF44475A;
    private static final int FG=0xFFF8F8F2, MUTED=0xFFB5B8C6, PURPLE=0xFFBD93F9, CYAN=0xFF8BE9FD;
    private DrawerNavigation(){}

    private static int dp(Activity activity,int n) {
        return Math.round(n*activity.getResources().getDisplayMetrics().density);
    }
    private static TextView text(Activity a,String title,int size,int color,boolean bold){
        TextView view=new TextView(a);
        view.setText(title);
        view.setTextColor(color);
        view.setTextSize(size);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }
    private static GradientDrawable background(Activity activity,int color,int radius){
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(color);shape.setCornerRadius(dp(activity,radius));
        return shape;
    }

    /** Insert the same app bar at the top of every destination and exercise. */
    public static void addBar(Activity a,LinearLayout root,String name,int selected){
        LinearLayout bar=new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu=text(a,"☰",27,FG,false);
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription("Open navigation menu");
        menu.setClickable(true);menu.setFocusable(true);
        menu.setBackground(background(a,SURFACE,12));
        bar.addView(menu,new LinearLayout.LayoutParams(dp(a,52),dp(a,52)));
        menu.setOnClickListener(v->open(a,selected));
        TextView title=text(a,name,19,FG,true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(a,15),0,0,0);
        bar.addView(title,new LinearLayout.LayoutParams(0,dp(a,52),1));
        if(selected!=SETTINGS){
            TextView settings=text(a,"⚙",25,CYAN,false);
            settings.setGravity(Gravity.CENTER);
            settings.setContentDescription("Open learning settings");
            settings.setClickable(true);settings.setFocusable(true);
            bar.addView(settings,new LinearLayout.LayoutParams(dp(a,48),dp(a,52)));
            settings.setOnClickListener(v->navigate(a,SETTINGS));
        }
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(a,56));
        params.bottomMargin=dp(a,16);
        root.addView(bar,params);
    }

    public static boolean close(Activity a) {
        FrameLayout frame=a.findViewById(android.R.id.content);
        if(frame==null)return false;
        View overlay=frame.findViewWithTag(TAG);
        if(overlay==null)return false;
        frame.removeView(overlay);
        return true;
    }

    private static void open(Activity activity,int selected) {
        if(close(activity))return;
        FrameLayout container=activity.findViewById(android.R.id.content);
        if(container==null)return;
        FrameLayout overlay=new FrameLayout(activity);
        overlay.setTag(TAG);
        overlay.setFocusableInTouchMode(true);
        View scrim=new View(activity);
        scrim.setBackgroundColor(0xB5000000);
        scrim.setOnClickListener(v->close(activity));
        overlay.addView(scrim,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout panel=new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(activity,16),dp(activity,14),dp(activity,16),dp(activity,24));
        panel.setBackgroundColor(PANEL);
        int width=Math.min(dp(activity,322),
            activity.getResources().getDisplayMetrics().widthPixels-dp(activity,48));
        FrameLayout.LayoutParams sheet=new FrameLayout.LayoutParams(
            Math.max(dp(activity,230),width),-1,Gravity.START);
        overlay.addView(panel,sheet);

        TextView header=text(activity,"Duodecimal Workout",23,PURPLE,true);
        header.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(header,new LinearLayout.LayoutParams(-1,dp(activity,53)));
        TextView subtitle=text(activity,"Learn • Practice • Progress",13,MUTED,false);
        panel.addView(subtitle,new LinearLayout.LayoutParams(-1,dp(activity,29)));

        View line=new View(activity);
        line.setBackgroundColor(SURFACE);
        LinearLayout.LayoutParams divider=new LinearLayout.LayoutParams(-1,dp(activity,1));
        divider.topMargin=dp(activity,12);
        divider.bottomMargin=dp(activity,8);
        panel.addView(line,divider);

        ScrollView scroll=new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout items=new LinearLayout(activity);
        items.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(items);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        section(activity,items,"STUDY");
        item(activity,items,"Practice","Adaptive arithmetic",PRACTICE,selected);
        item(activity,items,"Browse facts","Unlocks and progress table",BROWSE,selected);
        item(activity,items,"Statistics","Accuracy and fluency",STATS,selected);
        section(activity,items,"LEARN");
        item(activity,items,"Learning tools","Glyphs and visual arithmetic",LEARN,selected);
        section(activity,items,"OTHER");
        item(activity,items,"Other workouts","Mixed arithmetic exercises",MORE,selected);
        item(activity,items,"Settings","Goals and unlocking rules",SETTINGS,selected);
        View separator=new View(activity);
        separator.setBackgroundColor(SURFACE);
        panel.addView(separator,new LinearLayout.LayoutParams(-1,dp(activity,1)));
        TextView footer=text(activity,"Offline · No account needed",12,MUTED,false);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(footer,new LinearLayout.LayoutParams(-1,dp(activity,44)));

        overlay.setOnKeyListener((view,key,event)->{
            if(key==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_UP){
                close(activity);return true;
            }
            return false;
        });
        container.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        overlay.requestFocus();
    }

    private static void section(Activity activity,LinearLayout parent,String title){
        TextView label=text(activity,title,12,CYAN,true);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setPadding(dp(activity,10),dp(activity,12),0,0);
        parent.addView(label,new LinearLayout.LayoutParams(-1,dp(activity,48)));
    }
    private static void item(Activity activity,LinearLayout parent,String name,String info,
                             int destination,int selected){
        boolean active=destination==selected;
        LinearLayout row=new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(activity,14),dp(activity,10),dp(activity,10),dp(activity,10));
        row.setBackground(background(activity,active?SURFACE:PANEL,10));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(name+", "+info+(active?", current page":""));
        TextView title=text(activity,name,16,active?PURPLE:FG,active);
        row.addView(title);
        TextView summary=text(activity,info,12,MUTED,false);
        row.addView(summary);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(activity,66));
        lp.bottomMargin=dp(activity,3);
        parent.addView(row,lp);
        row.setOnClickListener(v->{
            close(activity);
            navigate(activity,destination);
        });
    }
    private static void navigate(Activity a,int destination) {
        Class<?> target;
        switch(destination) {
            case PRACTICE:
            case BROWSE:target=CustomPracticeActivity.class;break;
            case LEARN:target=LearnActivity.class;break;
            case STATS:target=StatisticsActivity.class;break;
            case MORE:target=MainActivity.class;break;
            case SETTINGS:target=SettingsActivity.class;break;
            default:return;
        }
        Intent intent=new Intent(a,target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(destination==PRACTICE||destination==BROWSE)
            intent.putExtra("open_fact_table",destination==BROWSE);
        a.startActivity(intent);
    }
}
