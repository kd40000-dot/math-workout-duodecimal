package com.duodecimal.workout;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.Random;

/** Interactive CRT multiplication: cap mod 3, body mod 4, final digit, carry. */
public final class VisualMultiplicationActivity extends Activity {
    private static final int BG=0xFF282A36, SURFACE=0xFF44475A, FG=0xFFF8F8F2;
    private static final int PURPLE=0xFFBD93F9, CYAN=0xFF8BE9FD, GREEN=0xFF50FA7B;
    private static final int RED=0xFFFF5555, MUTED=0xFFB5B8C6;
    private static final String[] MODES={"Cap multiplication (mod 3)",
        "Body multiplication (mod 4)", "Final digit from components",
        "Complete multiplication + carry"};
    private static final String[] CAPS={"Bar","Breve","Dot"};
    private static final String[] BODIES={"Loop","Stem","Vee","El"};
    private final Random random=new Random();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private LinearLayout root;
    private ScrollView scroll;
    private int mode, a, b, stage=-1, completed, perfect, total=12;
    private boolean questionPerfect=true, resolved=false;
    private Runnable pending;
    private TextView feedback;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("duodecimal_workout",MODE_PRIVATE);
        mode=Math.max(0,Math.min(3,prefs.getInt("visual_multiply_mode",3)));
        scroll=new ScrollView(this);
        scroll.setBackgroundColor(BG);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(18),dp(16),dp(30));
        scroll.addView(root);
        setContentView(scroll);
        setup();
    }
    @Override protected void onDestroy(){cancel();super.onDestroy();}
    @Override public void onBackPressed(){if(completed>0 || stage>=0){cancel();completed=0;stage=-1;setup();} else super.onBackPressed();}
    private void cancel(){if(pending!=null){handler.removeCallbacks(pending);pending=null;}}
    private int dp(int x){return (int)(getResources().getDisplayMetrics().density*x+.5f);}
    private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(10));return d;}
    private TextView label(String text,int size,int color){
        TextView v=new Numerals.NumeralTextView(this);
        v.setTextSize(size);v.setTextColor(color);v.setText(text);
        return v;
    }
    private Button button(String text,int color,int ink){
        Button b=new Numerals.NumeralButton(this);
        b.setAllCaps(false);b.setText(text);b.setTextSize(17);
        b.setTextColor(ink);b.setBackground(bg(color));
        return b;
    }
    private void add(String s,int size,int color){
        TextView v=label(s,size,color);root.addView(v);
    }
    private void gap(int h){root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private void header(String subtitle){
        TextView back=label("‹  WORKOUT",14,CYAN);
        root.addView(back);back.setOnClickListener(v->finish());gap(14);
        add("Visual Multiplication",26,PURPLE);
        add(subtitle,13,MUTED);gap(14);
    }
    private void setup(){
        cancel();stage=-1;root.removeAllViews();
        header("Multiply the cap and body independently, then find the carry.");
        add("TRAINING MODE",13,CYAN);gap(6);
        Spinner spinner=new Spinner(this,Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,MODES){
            @Override public View getView(int pos,View old,ViewGroup parent){return decorate(super.getView(pos,old,parent));}
            @Override public View getDropDownView(int pos,View old,ViewGroup parent){return decorate(super.getDropDownView(pos,old,parent));}
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);spinner.setSelection(mode);
        spinner.setBackground(bg(SURFACE));spinner.setPopupBackgroundDrawable(bg(SURFACE));
        root.addView(spinner,new LinearLayout.LayoutParams(-1,dp(52)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(AdapterView<?> parent){}
            @Override public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){
                mode=pos;prefs.edit().putInt("visual_multiply_mode",pos).apply();
            }
        });
        gap(16);
        add("MULTIPLICATION RULES",15,CYAN);gap(8);
        add("CAPS (mod 3): Bar × anything = Bar; Breve leaves the other cap unchanged; Dot × Dot = Breve.",14,FG);
        gap(10);
        add("BODIES (mod 4): Loop × anything = Loop; Stem leaves the other body unchanged; Vee × Vee = Loop; El × El = Stem.",14,FG);
        gap(10);
        add("The cap and body determine the final product digit. The carry is the whole-number quotient of the product divided by 10₁₂.",14,FG);
        gap(15);
        add("Example: 7 × 8 = 48₁₂. El × Loop = Loop; Breve × Dot = Dot; the last digit is 8 and the carry is 4.",14,CYAN);
        gap(24);
        Button start=button("START 10₁₂ QUESTIONS →",PURPLE,BG);
        root.addView(start,new LinearLayout.LayoutParams(-1,dp(58)));
        start.setOnClickListener(v->{completed=0;perfect=0;nextQuestion();});
    }
    private View decorate(View view){
        if(view instanceof TextView){
            TextView text=(TextView)view;text.setTextColor(FG);text.setTextSize(16);
            text.setPadding(dp(10),dp(8),dp(10),dp(8));
        }
        return view;
    }
    private int[] stages(){
        switch(mode){
            case 0:return new int[]{0};
            case 1:return new int[]{1};
            case 2:return new int[]{0,1,2};
            default:return new int[]{0,1,2,3};
        }
    }
    private int expected(int part){
        switch(part){
            case 0:return (a%3)*(b%3)%3;
            case 1:return (a%4)*(b%4)%4;
            case 2:return (a*b)%12;
            default:return (a*b)/12;
        }
    }
    private String partName(int part){
        return part==0?"RESULT CAP":part==1?"RESULT BODY":part==2?"FINAL DIGIT":"CARRY";
    }
    private void nextQuestion(){
        cancel();
        if(completed>=total){finished();return;}
        a=1+random.nextInt(11);b=1+random.nextInt(11);
        stage=0;questionPerfect=true;showStage();
    }
    private void showStage(){
        cancel();resolved=false;root.removeAllViews();
        header(MODES[mode]);
        add("Question "+DozenMath.format(completed+1)+"/"+DozenMath.format(total)+
            "  ·  Perfect "+DozenMath.format(perfect),14,CYAN);
        gap(12);
        TextView equation=label(DozenMath.format(a)+" × "+DozenMath.format(b),34,FG);
        equation.setGravity(Gravity.CENTER);equation.setPadding(0,dp(16),0,dp(16));
        equation.setBackground(bg(SURFACE));
        root.addView(equation,new LinearLayout.LayoutParams(-1,dp(100)));
        gap(14);
        int part=stages()[stage];
        add("STEP "+DozenMath.format(stage+1)+"/"+DozenMath.format(stages().length)+" · "+partName(part),15,PURPLE);
        gap(7);
        if(part==0 || part==1){
            boolean cap=part==0;
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            root.addView(row,new LinearLayout.LayoutParams(-1,dp(83)));
            int l=cap?a%3:a%4, r=cap?b%3:b%4;
            row.addView(new ComponentView(cap,l),new LinearLayout.LayoutParams(0,-1,1));
            TextView times=label("×",25,CYAN);times.setGravity(Gravity.CENTER);
            row.addView(times,new LinearLayout.LayoutParams(dp(32),-1));
            row.addView(new ComponentView(cap,r),new LinearLayout.LayoutParams(0,-1,1));
            TextView equal=label("= ?",25,CYAN);equal.setGravity(Gravity.CENTER);
            row.addView(equal,new LinearLayout.LayoutParams(dp(56),-1));
            add((cap?CAPS[l]:BODIES[l])+" × "+(cap?CAPS[r]:BODIES[r]),14,MUTED);
        } else if(part==2){
            add("Cap: "+CAPS[expected(0)]+" · Body: "+BODIES[expected(1)],16,CYAN);
            add("Which digit has both features?",15,FG);
        } else {
            add("The final digit is "+DozenMath.format(expected(2))+".",16,CYAN);
            add("How many dozens carry to the next column?",15,FG);
        }
        gap(15);
        int count=part==0?3:part==1?4:part==2?12:11;
        for(int r=0;r<(count+3)/4;r++){
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            root.addView(row,new LinearLayout.LayoutParams(-1,dp(64)));
            for(int c=0;c<4;c++){
                final int choice=r*4+c;
                if(choice>=count){View blank=new View(this);row.addView(blank,new LinearLayout.LayoutParams(0,-1,1));continue;}
                String name=part==0?CAPS[choice]:part==1?BODIES[choice]:DozenMath.format(choice);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
                lp.setMargins(dp(2),dp(2),dp(2),dp(2));
                if(part<=1){
                    LinearLayout tile=new LinearLayout(this);
                    tile.setOrientation(LinearLayout.VERTICAL);
                    tile.setGravity(Gravity.CENTER);
                    tile.setBackground(bg(SURFACE));
                    tile.addView(new ComponentView(part==0,choice),new LinearLayout.LayoutParams(-1,dp(43)));
                    TextView nameLabel=label(name,11,FG);
                    nameLabel.setGravity(Gravity.CENTER);
                    tile.addView(nameLabel);
                    row.addView(tile,lp);
                    tile.setContentDescription(name);
                    tile.setClickable(true);
                    tile.setOnClickListener(v->answer(part,choice,tile));
                } else {
                    Button key=button(name,SURFACE,FG);
                    row.addView(key,lp);
                    key.setOnClickListener(v->answer(part,choice,key));
                }
            }
        }
        gap(12);
        feedback=label("",15,GREEN);feedback.setGravity(Gravity.CENTER);
        root.addView(feedback,new LinearLayout.LayoutParams(-1,dp(45)));
        Button reveal=button("SHOW EXPLANATION",SURFACE,CYAN);
        root.addView(reveal,new LinearLayout.LayoutParams(-1,dp(48)));
        reveal.setOnClickListener(v->feedback.setText(explain(part)));
        gap(10);
        Button end=button("END SESSION / CHANGE MODE",SURFACE,MUTED);
        root.addView(end,new LinearLayout.LayoutParams(-1,dp(48)));
        end.setOnClickListener(v->{completed=0;setup();});
        scroll.post(()->scroll.scrollTo(0,0));
    }
    private String explain(int part){
        int product=a*b;
        if(part==0)return "("+DozenMath.format(a%3)+" × "+DozenMath.format(b%3)+") mod 3 = "+CAPS[expected(0)];
        if(part==1)return "("+DozenMath.format(a%4)+" × "+DozenMath.format(b%4)+") mod 4 = "+BODIES[expected(1)];
        if(part==2)return "The unique digit with "+CAPS[expected(0)]+" cap and "+BODIES[expected(1)]+" body is "+DozenMath.format(expected(2))+".";
        return DozenMath.format(a)+" × "+DozenMath.format(b)+" = "+DozenMath.format(product)+
            " = "+DozenMath.format(expected(3))+" × 10₁₂ + "+DozenMath.format(expected(2))+".";
    }
    private void answer(int part,int choice,View key){
        if(resolved)return;
        if(choice!=expected(part)){
            questionPerfect=false;feedback.setTextColor(RED);
            feedback.setText("Try again. "+explain(part));
            key.setBackground(bg(0xFF803D51));return;
        }
        resolved=true;feedback.setTextColor(GREEN);
        feedback.setText("Correct! "+explain(part));
        boolean last=stage+1==stages().length;
        if(last){
            if(questionPerfect)perfect++;
            completed++;
        }
        pending=()->{pending=null;if(isFinishing()||isDestroyed())return;
            if(last)nextQuestion();else{stage++;showStage();}
        };
        handler.postDelayed(pending,last?1100:700);
    }
    private final class ComponentView extends View {
        private final boolean cap;
        private final int value;
        ComponentView(boolean cap,int value){
            super(VisualMultiplicationActivity.this);
            this.cap=cap;this.value=value;
            setContentDescription((cap?"Cap ":"Body ")+(cap?CAPS[value]:BODIES[value]));
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float h=Math.min(dp(cap?44:66),getHeight()*.82f);
            Numerals.drawComponent(canvas,cap,value,getWidth()/2f,getHeight()/2f,h,FG);
        }
    }

    private void finished(){
        stage=-1;root.removeAllViews();header("Session complete");
        add("Perfect questions: "+DozenMath.format(perfect)+"/"+DozenMath.format(total),21,GREEN);
        gap(15);
        Button again=button("PRACTICE AGAIN",PURPLE,BG);
        root.addView(again,new LinearLayout.LayoutParams(-1,dp(54)));
        again.setOnClickListener(v->{completed=0;perfect=0;nextQuestion();});
        gap(10);
        Button modes=button("CHANGE MODE",SURFACE,FG);
        root.addView(modes,new LinearLayout.LayoutParams(-1,dp(54)));
        modes.setOnClickListener(v->setup());
    }
}
