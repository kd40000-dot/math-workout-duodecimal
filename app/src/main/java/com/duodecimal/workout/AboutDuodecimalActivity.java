package com.duodecimal.workout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Plain-language reference available any time from the side menu.
 * Use normal TextViews for explanatory decimal numbers; only actual custom
 * digit examples are painted by Numerals.NumeralTextView.
 */
public final class AboutDuodecimalActivity extends Activity {
    private LinearLayout root;

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().setStatusBarColor(GuideUi.BG);
        getWindow().setNavigationBarColor(GuideUi.BG);
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(GuideUi.BG);
        scroll.setFillViewport(true);
        root=GuideUi.column(this);
        root.setPadding(GuideUi.dp(this,16),GuideUi.dp(this,12),
            GuideUi.dp(this,16),GuideUi.dp(this,32));
        scroll.addView(root);
        setContentView(scroll);
        render();
    }
    @Override public void onBackPressed(){
        if(DrawerNavigation.close(this))return;
        super.onBackPressed();
    }
    private void gap(int n){GuideUi.gap(this,root,n);}
    private void heading(String title,String description) {
        gap(23);
        root.addView(GuideUi.text(this,title,20,GuideUi.CYAN,true));
        GuideUi.gap(this,root,5);
        GuideUi.caption(this,root,description);
        gap(12);
    }
    private void render(){
        root.removeAllViews();
        DrawerNavigation.addBar(this,root,"About base twelve",DrawerNavigation.ABOUT);
        GuideUi.title(this,root,"Why base twelve?");
        GuideUi.copy(this,root,
            "Most of us learned to count in groups of ten. Base twelve (also called duodecimal or dozenal) is simply counting in groups of twelve instead. The amount of something stays the same; only how we write the number changes.");
        gap(12);
        LinearLayout example=GuideUi.card(this,root);
        example.addView(GuideUi.text(this,"A dozen eggs is still twelve eggs.",17,GuideUi.FG,true));
        GuideUi.gap(this,example,8);
        GuideUi.copy(this,example,"In everyday decimal notation: 12");
        GuideUi.copy(this,example,"In base-twelve notation: 10");
        GuideUi.gap(this,example,6);
        GuideUi.caption(this,example,
            "Just as “10” in ordinary counting means one group of ten, “10” in base twelve means one group of twelve.");

        heading("Counting beyond nine",
            "The familiar digits 0 through 9 stay the same in ordinary written base-twelve notation.");
        LinearLayout counting=GuideUi.card(this,root);
        TextView line=GuideUi.text(this,"8   9   ↊   ↋   10",25,GuideUi.CYAN,true);
        line.setGravity(Gravity.CENTER);
        counting.addView(line);
        GuideUi.gap(this,counting,9);
        GuideUi.copy(this,counting,
            "↊ means ten, and ↋ means eleven. After eleven, we start a new group of twelve and write 10.");
        GuideUi.gap(this,counting,7);
        GuideUi.caption(this,counting,"So 10 in base twelve = 12 in everyday decimal. 100 in base twelve = 144 in everyday decimal.");

        heading("Why use twelve instead of ten?",
            "Twelve divides neatly into halves, thirds, quarters, and sixths.");
        LinearLayout pieces=GuideUi.card(this,root);
        GuideUi.copy(this,pieces,
            "Imagine twelve equal pieces of a chocolate bar. Many common shares come out in whole pieces.");
        GuideUi.gap(this,pieces,14);
        GuideUi.fraction(this,pieces,6,"One half","6 of 12 pieces.");
        GuideUi.gap(this,pieces,14);
        GuideUi.fraction(this,pieces,4,"One third","4 of 12 pieces.");
        GuideUi.gap(this,pieces,14);
        GuideUi.fraction(this,pieces,3,"One quarter","3 of 12 pieces.");
        GuideUi.gap(this,pieces,12);
        GuideUi.copy(this,pieces,
            "This makes many everyday fractions shorter to write. For example, a third is 0.4 in base twelve, while it is 0.333… in base ten.");
        GuideUi.gap(this,pieces,7);
        GuideUi.caption(this,pieces,
            "Other examples: one quarter = 0.3 in base twelve; one sixth = 0.2. The numbers are not different amounts—only different ways to write them.");

        heading("Why create new digit shapes?",
            "Our app uses its own glyphs (number symbols) rather than the usual digits.");
        GuideUi.copy(this,root,
            "Instead of asking you to memorize twelve unrelated shapes, we built each symbol from two repeating patterns. Once you notice the patterns, you can recognize the number and even spot useful relationships.");
        gap(12);
        LinearLayout breakdown=GuideUi.card(this,root);
        breakdown.addView(GuideUi.text(this,"Two parts, one digit",18,GuideUi.CYAN,true));
        GuideUi.gap(this,breakdown,9);
        LinearLayout row=GuideUi.row(this);
        breakdown.addView(row,new LinearLayout.LayoutParams(-1,GuideUi.dp(this,122)));
        TextView symbol=GuideUi.glyph(this,7,78,GuideUi.FG);
        row.addView(symbol,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout legend=GuideUi.column(this);
        legend.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(legend,new LinearLayout.LayoutParams(0,-1,2));
        legend.addView(GuideUi.text(this,"7 (seven)",20,GuideUi.FG,true));
        GuideUi.gap(this,legend,6);
        GuideUi.copy(this,legend,"Top: Breve");
        GuideUi.copy(this,legend,"Bottom: El");
        GuideUi.gap(this,breakdown,10);
        GuideUi.copy(this,breakdown,
            "The top mark comes in three styles: Bar, Breve, and Dot. Starting at zero, the styles repeat every three numbers.");
        GuideUi.gap(this,breakdown,8);
        GuideUi.copy(this,breakdown,
            "The lower shape comes in four styles: Loop, Stem, Vee, and El. Starting at zero, these repeat every four numbers.");
        GuideUi.gap(this,breakdown,8);
        GuideUi.copy(this,breakdown,
            "The two patterns line up in a different combination for every digit from zero to eleven. Together they identify the digit.");

        heading("The complete glyph guide",
            "Each tile shows the custom glyph, its familiar value in decimal, and its two parts.");
        GuideUi.gallery(this,root,true);

        heading("Does this make calculation easier?",
            "The goal is to make useful arithmetic patterns visible, not to change mathematics itself.");
        LinearLayout motivation=GuideUi.card(this,root);
        GuideUi.copy(this,motivation,
            "When adding or multiplying, you can sometimes work with the top marks and bottom shapes separately to figure out the last digit of an answer. With practice, those small patterns may be easier to recognize than memorizing every calculation from scratch.");
        GuideUi.gap(this,motivation,10);
        GuideUi.copy(this,motivation,
            "For example, four groups of three make twelve. In base twelve, that answer is written 10: one full dozen and zero left over.");
        GuideUi.gap(this,motivation,10);
        GuideUi.copy(this,motivation,
            "Base twelve is especially convenient for fractions involving 2, 3, 4, and 6. But it is not magically better for every calculation, and our glyphs are an experimental way to practice seeing patterns.");

        heading("Getting started",
            "You don't need to understand the patterns before using the app.");
        LinearLayout start=GuideUi.card(this,root);
        GuideUi.copy(this,start,
            "Start with Glyph recognition under Learning tools. Then use the adaptive Practice mode: it introduces a few facts at a time and focuses more often on those you find difficult.");
        GuideUi.gap(this,start,12);
        Button tutorial=GuideUi.button(this,"Replay beginner tutorial",false);
        start.addView(tutorial,new LinearLayout.LayoutParams(-1,GuideUi.dp(this,54)));
        tutorial.setOnClickListener(v->startActivity(new Intent(this,TutorialActivity.class)));
        GuideUi.gap(this,start,10);
        Button practice=GuideUi.button(this,"Go to practice",true);
        start.addView(practice,new LinearLayout.LayoutParams(-1,GuideUi.dp(this,54)));
        practice.setOnClickListener(v->startActivity(
            new Intent(this,CustomPracticeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP)));
    }
}
