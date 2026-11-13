package com.duodecimal.workout;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * First-launch, skippable seven-step walkthrough. It does not edit workout
 * progress or ask for any account information. Available for replay via drawer.
 */
public final class TutorialActivity extends Activity {
    public static final String PREF_COMPLETE="beginner_tutorial_v1_complete";
    private static final int LAST_PAGE=6;
    private static final String[] PAGE_NAMES={
        "Welcome","Counting","Why twelve?","Our glyphs",
        "All twelve symbols","Try one","You're ready!"
    };
    private SharedPreferences prefs;
    private ScrollView scroll;
    private LinearLayout body,outer;
    private Button back,next,skip;
    private int page;
    private boolean quizCorrect;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(GuideUi.BG);
        getWindow().setNavigationBarColor(GuideUi.BG);
        prefs=getSharedPreferences("duodecimal_workout",MODE_PRIVATE);
        if(state!=null){
            page=Math.max(0,Math.min(LAST_PAGE,state.getInt("page",0)));
            quizCorrect=state.getBoolean("quiz_correct",false);
        }
        outer=GuideUi.column(this);
        outer.setBackgroundColor(GuideUi.BG);
        scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        body=GuideUi.column(this);
        body.setPadding(GuideUi.dp(this,18),GuideUi.dp(this,17),
            GuideUi.dp(this,18),GuideUi.dp(this,22));
        scroll.addView(body);
        outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout footer=GuideUi.column(this);
        footer.setBackgroundColor(GuideUi.DARK);
        footer.setPadding(GuideUi.dp(this,14),GuideUi.dp(this,10),
            GuideUi.dp(this,14),GuideUi.dp(this,15));
        LinearLayout row=GuideUi.row(this);
        footer.addView(row,new LinearLayout.LayoutParams(-1,GuideUi.dp(this,55)));
        back=GuideUi.button(this,"Back",false);
        LinearLayout.LayoutParams pBack=new LinearLayout.LayoutParams(0,-1,0.95f);
        pBack.rightMargin=GuideUi.dp(this,8);
        row.addView(back,pBack);
        next=GuideUi.button(this,"Next",true);
        row.addView(next,new LinearLayout.LayoutParams(0,-1,1.35f));
        GuideUi.gap(this,footer,7);
        skip=GuideUi.button(this,"Skip tutorial",false);
        footer.addView(skip,new LinearLayout.LayoutParams(-1,GuideUi.dp(this,45)));
        outer.addView(footer,new LinearLayout.LayoutParams(-1,-2));

        setContentView(outer);
        back.setOnClickListener(v->previous());
        next.setOnClickListener(v->advance());
        skip.setOnClickListener(v->complete());
        render();
    }
    @Override protected void onSaveInstanceState(Bundle out){
        out.putInt("page",page);
        out.putBoolean("quiz_correct",quizCorrect);
        super.onSaveInstanceState(out);
    }
    @Override public void onBackPressed(){
        if(page>0)previous();
        else complete();
    }
    private void previous(){
        if(page>0){page--;render();}
    }
    private void advance(){
        if(page==5 && !quizCorrect)return;
        if(page>=LAST_PAGE){complete();return;}
        page++;render();
    }
    private void complete(){
        prefs.edit().putBoolean(PREF_COMPLETE,true).apply();
        finish();
    }
    private void top(String title,String detail){
        TextView over=GuideUi.text(this,
            "BEGINNER TUTORIAL   •   "+(page+1)+" / "+(LAST_PAGE+1),
            12,GuideUi.CYAN,true);
        body.addView(over);
        GuideUi.gap(this,body,9);
        GuideUi.title(this,body,title);
        GuideUi.caption(this,body,detail);
        GuideUi.gap(this,body,21);
    }
    private void render(){
        body.removeAllViews();
        next.setText(page==LAST_PAGE?"Start practicing":"Next");
        next.setEnabled(page!=5 || quizCorrect);
        next.setAlpha(next.isEnabled()?1f:.45f);
        back.setEnabled(page>0);
        back.setAlpha(page>0?1f:.42f);
        skip.setVisibility(page==LAST_PAGE?View.GONE:View.VISIBLE);
        switch(page){
            case 0:welcome();break;
            case 1:counting();break;
            case 2:fractions();break;
            case 3:glyphs();break;
            case 4:gallery();break;
            case 5:quiz();break;
            default:ready();break;
        }
        scroll.post(()->scroll.scrollTo(0,0));
    }
    private void welcome(){
        top("Welcome to base twelve",
            "A new way to write numbers, built around groups of twelve.");
        LinearLayout card=GuideUi.card(this,body);
        GuideUi.copy(this,card,
            "You probably count in groups of ten. That is called base ten.");
        GuideUi.gap(this,card,13);
        GuideUi.copy(this,card,
            "In this app, we count in groups of twelve instead. That's called base twelve—or duodecimal.");
        GuideUi.gap(this,card,13);
        GuideUi.copy(this,card,
            "The quantities don't change. Only the symbols and the way we group them change.");
        GuideUi.gap(this,body,20);
        TextView note=GuideUi.text(this,"One dozen = twelve things",22,GuideUi.CYAN,true);
        note.setGravity(Gravity.CENTER);
        body.addView(note);
        GuideUi.gap(this,body,12);
        GuideUi.caption(this,body,
            "You'll learn why twelve is handy, what our new symbols mean, and how to try your first question. No special math knowledge needed.");
    }
    private void counting(){
        top("Counting past nine",
            "We need twelve different single-digit symbols, not just ten.");
        LinearLayout card=GuideUi.card(this,body);
        TextView count=GuideUi.text(this,"8    9    ↊    ↋    10",25,GuideUi.CYAN,true);
        count.setGravity(Gravity.CENTER);
        card.addView(count);
        GuideUi.gap(this,card,15);
        GuideUi.copy(this,card,"↊ is ten and ↋ is eleven.");
        GuideUi.gap(this,card,12);
        GuideUi.copy(this,card,
            "After eleven, we write 10. But in base twelve, 10 means one group of twelve—just like 10 in base ten means one group of ten.");
        GuideUi.gap(this,card,13);
        GuideUi.caption(this,card,
            "So 10 in base twelve and 12 in everyday counting mean the same quantity.");
        GuideUi.gap(this,body,17);
        GuideUi.copy(this,body,
            "These are the standard written base-twelve symbols. In a moment you'll see our custom glyphs for the same twelve digits.");
    }
    private void fractions(){
        top("Why choose twelve?",
            "Twelve is especially friendly to splitting things into equal shares.");
        LinearLayout card=GuideUi.card(this,body);
        GuideUi.copy(this,card,
            "Imagine twelve equal pieces of chocolate. You can share them evenly in lots of useful ways.");
        GuideUi.gap(this,card,14);
        GuideUi.fraction(this,card,6,"Half","Six pieces each.");
        GuideUi.gap(this,card,14);
        GuideUi.fraction(this,card,4,"Third","Four pieces each.");
        GuideUi.gap(this,card,14);
        GuideUi.fraction(this,card,3,"Quarter","Three pieces each.");
        GuideUi.gap(this,card,12);
        GuideUi.copy(this,card,
            "That is why a third is written 0.4 in base twelve, instead of 0.333… in our usual base ten.");
        GuideUi.gap(this,body,13);
        GuideUi.caption(this,body,
            "You aren't changing what a third means. You're choosing a system that writes many common fractions more neatly.");
    }
    private void glyphs(){
        top("Meet our glyphs",
            "A glyph is just a shape that stands for a number.");
        LinearLayout card=GuideUi.card(this,body);
        LinearLayout preview=GuideUi.row(this);
        card.addView(preview,new LinearLayout.LayoutParams(-1,GuideUi.dp(this,130)));
        TextView glyph=GuideUi.glyph(this,7,91,GuideUi.FG);
        preview.addView(glyph,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout description=GuideUi.column(this);
        description.setGravity(Gravity.CENTER_VERTICAL);
        preview.addView(description,new LinearLayout.LayoutParams(0,-1,1));
        description.addView(GuideUi.text(this,"7 = seven",20,GuideUi.CYAN,true));
        GuideUi.gap(this,description,7);
        GuideUi.copy(this,description,"Top: Breve");
        GuideUi.copy(this,description,"Bottom: El");
        GuideUi.gap(this,card,12);
        GuideUi.copy(this,card,
            "Every glyph has a mark on top and a shape below it.");
        GuideUi.gap(this,card,11);
        GuideUi.copy(this,card,
            "The top mark follows a repeating three-step pattern: Bar, Breve, Dot.");
        GuideUi.gap(this,card,11);
        GuideUi.copy(this,card,
            "The lower shape follows a repeating four-step pattern: Loop, Stem, Vee, El.");
        GuideUi.gap(this,card,11);
        GuideUi.copy(this,card,
            "Because the two patterns repeat at different speeds, each digit gets its own combination.");
        GuideUi.gap(this,body,14);
        GuideUi.caption(this,body,
            "The shapes are designed to help you spot number patterns, not just memorize twelve unrelated pictures.");
    }
    private void gallery(){
        top("The twelve glyphs",
            "Here is the complete set. The ordinary decimal value appears below each symbol.");
        GuideUi.gallery(this,body,false);
        GuideUi.gap(this,body,15);
        GuideUi.copy(this,body,
            "You don't need to remember them all immediately. Recognition practice introduces them as you learn.");
    }
    private void quiz(){
        top("Try recognizing one",
            "Choose the everyday number represented by this custom glyph.");
        LinearLayout card=GuideUi.card(this,body);
        card.addView(GuideUi.glyph(this,5,99,GuideUi.FG),
            new LinearLayout.LayoutParams(-1,GuideUi.dp(this,133)));
        GuideUi.gap(this,card,7);
        TextView feedback=GuideUi.text(this,
            quizCorrect?"Correct! This glyph represents five.":"Look closely at the top mark and the lower shape.",
            14,quizCorrect?GuideUi.GREEN:GuideUi.MUTED,false);
        feedback.setGravity(Gravity.CENTER);
        card.addView(feedback,new LinearLayout.LayoutParams(-1,GuideUi.dp(this,64)));
        GuideUi.gap(this,body,17);
        LinearLayout choices=GuideUi.row(this);
        body.addView(choices,new LinearLayout.LayoutParams(-1,GuideUi.dp(this,64)));
        for(int answer=4;answer<=7;answer++){
            final int picked=answer;
            Button option=GuideUi.button(this,Integer.toString(answer),false);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1);
            params.setMargins(GuideUi.dp(this,3),0,GuideUi.dp(this,3),0);
            choices.addView(option,params);
            option.setContentDescription("Answer "+answer);
            option.setOnClickListener(v->{
                if(quizCorrect)return;
                if(picked==5){
                    quizCorrect=true;
                    feedback.setTextColor(GuideUi.GREEN);
                    feedback.setText("Exactly! Five has a Dot on top and a Stem below.");
                    next.setEnabled(true);next.setAlpha(1f);
                } else {
                    feedback.setTextColor(GuideUi.RED);
                    feedback.setText("Not quite. Look for the Dot on top and Stem below, then try again.");
                }
            });
        }
        GuideUi.gap(this,body,18);
        GuideUi.caption(this,body,
            "This is only a warm-up. Your real glyph-recognition exercises are in the Learning tools section.");
    }
    private void ready(){
        top("You're ready!",
            "Knowing everything at once isn't necessary. The app will help you learn gradually.");
        LinearLayout card=GuideUi.card(this,body);
        GuideUi.copy(this,card,
            "Practice starts with a small set of arithmetic facts. As you get them right and answer more quickly, new facts become available.");
        GuideUi.gap(this,card,13);
        GuideUi.copy(this,card,
            "Use the ☰ menu to find Glyph recognition, Statistics, Learning settings, and a permanent explanation of base twelve and the glyphs.");
        GuideUi.gap(this,card,13);
        GuideUi.copy(this,card,
            "Your learning history is saved on this device, and you can replay this tutorial at any time.");
        GuideUi.gap(this,body,18);
        Button more=GuideUi.button(this,"Read the full explanation",false);
        body.addView(more,new LinearLayout.LayoutParams(-1,GuideUi.dp(this,55)));
        more.setOnClickListener(v->{
            prefs.edit().putBoolean(PREF_COMPLETE,true).apply();
            startActivity(new Intent(this,AboutDuodecimalActivity.class));
            finish();
        });
        GuideUi.gap(this,body,12);
        GuideUi.caption(this,body,"Tap Start practicing to enter the main workout.");
    }
}
