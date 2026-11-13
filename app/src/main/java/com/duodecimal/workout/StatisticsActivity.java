package com.duodecimal.workout;

import android.app.Activity;
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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keybr-inspired offline analysis of arithmetic fluency.
 * Historical per-fact aggregates remain intact; time-series graphs show only
 * real events captured by PracticeHistory, never reconstructed samples.
 */
public final class StatisticsActivity extends Activity {
    private static final int BG=0xFF282A36,SURFACE=0xFF44475A,HEADER=0xFF363849;
    private static final int FG=0xFFF8F8F2,MUTED=0xFFB5B8C6,PURPLE=0xFFBD93F9;
    private static final int CYAN=0xFF8BE9FD,GREEN=0xFF50FA7B,RED=0xFFFF5555;
    private static final Pattern FACT_KEY=Pattern.compile(
        "^adaptive_v1_(\\d+)_(\\d+)_(\\d+)_(\\d+)_(\\d+)_tries$");
    private static final String[] OPERATIONS={
        "All operations","Addition","Subtraction","Multiplication","Exact division"
    };
    private static final String[] SORTS={
        "Slowest first","Lowest accuracy","Most practiced","Fastest first","Fact order"
    };
    private static final String[] LENGTHS=lengthOptions();

    private SharedPreferences prefs;
    private LinearLayout root;
    private ScrollView scroll;
    private int selectedOperation=-1, selectedLength=0,sortMode=0;
    private boolean showMoreRows=false;

    private static final class FactStat {
        int op,da,db,a,b,tries,hits,streak;
        float meanMs,recentMs;
        boolean mastered;
        long key;
        String expression(){
            return PracticeGrid.expression(op,a,b);
        }
        double accuracy(){return tries==0?0:(double)hits/tries;}
        float fluency(){return recentMs>0?recentMs:meanMs;}
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("duodecimal_workout",MODE_PRIVATE);
        scroll=new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(12),dp(16),dp(32));
        scroll.addView(root);
        setContentView(scroll);
    }
    @Override protected void onResume(){super.onResume();draw();}
    @Override public void onBackPressed() {
        if(DrawerNavigation.close(this))return;
        super.onBackPressed();
    }
    private static String[] lengthOptions(){
        String[] options=new String[17];
        options[0]="All recorded operand lengths";
        int i=1;
        for(int a=1;a<=4;a++)for(int b=1;b<=4;b++)
            options[i++]=a+"-digit × "+b+"-digit";
        return options;
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String text,int size,int color,boolean bold){
        TextView v=new Numerals.NumeralTextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private GradientDrawable bg(int color){
        GradientDrawable b=new GradientDrawable();
        b.setColor(color);
        b.setCornerRadius(dp(12));
        return b;
    }
    private void gap(int size){
        root.addView(new View(this),new LinearLayout.LayoutParams(1,dp(size)));
    }
    private void innerGap(LinearLayout parent,int size){
        parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(size)));
    }
    private LinearLayout card(){
        LinearLayout pane=new LinearLayout(this);
        pane.setOrientation(LinearLayout.VERTICAL);
        pane.setPadding(dp(14),dp(14),dp(14),dp(14));
        pane.setBackground(bg(SURFACE));
        root.addView(pane,new LinearLayout.LayoutParams(-1,-2));
        return pane;
    }
    private Spinner spinner(String[] names,int current){
        Spinner widget=new Spinner(this,Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(
            this,android.R.layout.simple_spinner_item,names){
            @Override public View getView(int pos,View reuse,ViewGroup parent) {
                return style(super.getView(pos,reuse,parent));
            }
            @Override public View getDropDownView(int pos,View reuse,ViewGroup parent) {
                return style(super.getDropDownView(pos,reuse,parent));
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        widget.setAdapter(adapter);
        widget.setSelection(current);
        widget.setBackground(bg(HEADER));
        widget.setPopupBackgroundDrawable(bg(HEADER));
        return widget;
    }
    private View style(View view){
        if(view instanceof TextView){
            TextView label=(TextView)view;
            label.setTextColor(FG);
            label.setTextSize(15);
            label.setPadding(dp(10),dp(10),dp(10),dp(10));
        }
        return view;
    }
    private void heading(String title,String subtitle){
        gap(20);
        root.addView(label(title,20,FG,true));
        if(subtitle!=null){
            innerGap(root,4);
            root.addView(label(subtitle,13,MUTED,false));
        }
        gap(11);
    }
    private static String perGross(long numerator,long denominator) {
        if(denominator<=0)return "—";
        long share=Math.round(numerator*144.0/denominator);
        return DozenMath.format(share)+"/100₁₂";
    }
    private static String amount(long n){return DozenMath.format(n);}
    private void addProgress(LinearLayout parent,int count,int total){
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1,total));
        bar.setProgress(count);
        bar.setProgressTintList(ColorStateList.valueOf(GREEN));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(HEADER));
        bar.setContentDescription(count+" mastered out of "+total+" practiced facts");
        parent.addView(bar,new LinearLayout.LayoutParams(-1,dp(9)));
    }

    private boolean inScope(int op,int da,int db) {
        if(selectedOperation>=0&&selectedOperation!=op)return false;
        if(selectedLength==0)return true;
        return da==(selectedLength-1)/4+1 && db==(selectedLength-1)%4+1;
    }
    private List<FactStat> loadFacts(){
        ArrayList<FactStat> rows=new ArrayList<>();
        Map<String,?> all=prefs.getAll();
        for(String name:all.keySet()){
            Matcher match=FACT_KEY.matcher(name);
            if(!match.matches())continue;
            try{
                int op=Integer.parseInt(match.group(1));
                int da=Integer.parseInt(match.group(2));
                int db=Integer.parseInt(match.group(3));
                int a=Integer.parseInt(match.group(4));
                int b=Integer.parseInt(match.group(5));
                if(op<0||op>3||da<1||da>4||db<1||db>4 ||
                    !inScope(op,da,db) || !PracticeGrid.valid(op,da,db,a,b))continue;
                AdaptiveFacts model=new AdaptiveFacts(prefs,op,da,db,0,0);
                long key=PracticeGrid.key(a,b);
                FactStat fact=new FactStat();
                fact.op=op;fact.da=da;fact.db=db;fact.a=a;fact.b=b;
                fact.key=key;fact.tries=model.attempts(key);
                if(fact.tries<=0)continue;
                fact.hits=model.correct(key);
                fact.streak=model.streak(key);
                fact.meanMs=model.averageMs(key);
                fact.recentMs=model.recentCorrectMs(key);
                fact.mastered=model.mastered(key);
                rows.add(fact);
            }catch(IllegalArgumentException ignore){/* Skip damaged preference entries. */}
        }
        return rows;
    }
    private List<PracticeHistory.Event> loadHistory(){
        ArrayList<PracticeHistory.Event> events=new ArrayList<>();
        for(int da=1;da<=4;da++)for(int db=1;db<=4;db++){
            if(selectedLength!=0 &&
                (da!=(selectedLength-1)/4+1||db!=(selectedLength-1)%4+1))continue;
            events.addAll(PracticeHistory.load(prefs,selectedOperation,da,db));
        }
        Collections.sort(events,(a,b)->Long.compare(a.epochSeconds,b.epochSeconds));
        return events;
    }
    private void draw(){
        root.removeAllViews();
        DrawerNavigation.addBar(this,root,"Statistics",DrawerNavigation.STATS);
        root.addView(label("Your learning progress",27,PURPLE,true));
        innerGap(root,6);
        root.addView(label("Response time, accuracy and mastery—tracked per arithmetic fact.",
            14,MUTED,false));
        gap(18);

        LinearLayout options=card();
        options.addView(label("Operation",14,FG,true));
        innerGap(options,7);
        Spinner operation=spinner(OPERATIONS,selectedOperation+1);
        options.addView(operation,new LinearLayout.LayoutParams(-1,dp(50)));
        innerGap(options,11);
        options.addView(label("Operand lengths",14,FG,true));
        innerGap(options,7);
        Spinner length=spinner(LENGTHS,selectedLength);
        options.addView(length,new LinearLayout.LayoutParams(-1,dp(50)));
        operation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){
                if(selectedOperation!=pos-1){selectedOperation=pos-1;draw();}
            }
        });
        length.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){
                if(selectedLength!=pos){selectedLength=pos;draw();}
            }
        });

        List<FactStat> facts=loadFacts();
        List<PracticeHistory.Event> history=loadHistory();
        int mastered=0,correct=0,attempts=0;
        double accumulatedMs=0;
        for(FactStat f:facts){
            attempts+=f.tries;
            correct+=f.hits;
            if(f.mastered)mastered++;
            accumulatedMs+=f.meanMs*f.tries;
        }
        heading("Overview","Cumulative results from all recorded practice, including older versions.");
        LinearLayout summary=card();
        summary.addView(label(amount(mastered)+" / "+amount(facts.size()),27,GREEN,true));
        innerGap(summary,4);
        summary.addView(label("Mastered / practiced facts",14,FG,false));
        innerGap(summary,11);
        addProgress(summary,mastered,facts.size());
        innerGap(summary,14);
        summary.addView(label("Answers  "+amount(attempts)+"   ·   Accuracy  "+
            perGross(correct,attempts),16,FG,true));
        innerGap(summary,9);
        summary.addView(label("Mean answer time  "+(attempts==0?"—":
            PracticeTiming.formatSeconds(Math.round(accumulatedMs/attempts))),
            15,CYAN,false));
        innerGap(summary,7);
        summary.addView(label("Target answer time  "+
            PracticeTiming.formatSeconds(AdaptiveSettings.targetMs(prefs)),14,MUTED,false));

        heading("Learning curves","From oldest to newest · averages of recent recorded answers.");
        if(history.isEmpty()){
            LinearLayout empty=card();
            empty.addView(label("Your earlier totals are available above. Timelines will appear once new answers have been recorded.",
                14,MUTED,false));
        } else {
            ArrayList<PracticeHistory.Event> recent=new ArrayList<>();
            int start=Math.max(0,history.size()-144);
            recent.addAll(history.subList(start,history.size()));
            int block=Math.max(1,(int)Math.ceil(recent.size()/18.0));
            ArrayList<Float> durations=new ArrayList<>();
            ArrayList<Float> accuracies=new ArrayList<>();
            for(int i=0;i<recent.size();i+=block){
                int end=Math.min(recent.size(),i+block);
                int hits=0;
                long time=0;
                for(int j=i;j<end;j++){
                    PracticeHistory.Event event=recent.get(j);
                    if(event.correct)hits++;
                    time+=event.durationMs;
                }
                durations.add((float)time/(end-i));
                accuracies.add((float)hits/(end-i));
            }
            lineGraph("Answer time","Lower is faster · dashed line = target",
                durations,StatisticsChartView.TIME,AdaptiveSettings.targetMs(prefs),
                PracticeTiming.formatSeconds(Math.round(durations.get(durations.size()-1))));
            gap(12);
            lineGraph("Accuracy","Higher is better · dashed line = mastery threshold",
                accuracies,StatisticsChartView.ACCURACY,
                AdaptiveSettings.accuracy(prefs)/100f,
                perGross(Math.round(accuracies.get(accuracies.size()-1)*144),144));
            innerGap(root,9);
            root.addView(label("The last "+amount(recent.size())+
                " recorded answers are grouped into blocks of "+amount(block)+
                ". Accuracy uses all submitted answers, including mistakes.",
                12,MUTED,false));
        }

        heading("Slowest facts","Recent successful response-time average for each practiced fact.");
        ArrayList<FactStat> byTime=new ArrayList<>(facts);
        Collections.sort(byTime,(a,b)->Float.compare(b.fluency(),a.fluency()));
        if(byTime.isEmpty())root.addView(label("Complete some practice to see your slowest facts.",14,MUTED,false));
        else {
            LinearLayout bars=card();
            int limit=Math.min(10,byTime.size());
            float max=Math.max(1f,byTime.get(0).fluency());
            for(int i=0;i<limit;i++){
                FactStat fact=byTime.get(i);
                LinearLayout line=new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setGravity(Gravity.CENTER_VERTICAL);
                TextView name=label(fact.expression(),13,FG,false);
                line.addView(name,new LinearLayout.LayoutParams(0,-2,1));
                TextView duration=label(PracticeTiming.formatSeconds(Math.round(fact.fluency())),
                    13,CYAN,true);
                duration.setGravity(Gravity.END);
                line.addView(duration);
                bars.addView(line);
                innerGap(bars,5);
                ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
                bar.setMax(1000);
                bar.setProgress(Math.max(1,Math.round(fact.fluency()/max*1000)));
                bar.setProgressTintList(ColorStateList.valueOf(i==0?RED:PURPLE));
                bar.setProgressBackgroundTintList(ColorStateList.valueOf(HEADER));
                bar.setContentDescription(fact.expression()+" response time "+
                    PracticeTiming.formatSeconds(Math.round(fact.fluency())));
                bars.addView(bar,new LinearLayout.LayoutParams(-1,dp(10)));
                if(i<limit-1)innerGap(bars,13);
            }
        }

        heading("Fact performance table","Compare individual facts; sort by fluency, mistakes or practice count.");
        if(facts.isEmpty())root.addView(label("No practiced facts in this filter yet.",14,MUTED,false));
        else {
            Spinner sorting=spinner(SORTS,sortMode);
            root.addView(sorting,new LinearLayout.LayoutParams(-1,dp(51)));
            sorting.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onNothingSelected(AdapterView<?> parent){}
                public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){
                    if(pos!=sortMode){sortMode=pos;draw();}
                }
            });
            gap(10);
            sortFacts(facts);
            LinearLayout host=table(facts,showMoreRows?60:20);
            root.addView(host,new LinearLayout.LayoutParams(-1,-2));
            if(facts.size()>20){
                TextView toggle=label(showMoreRows?"Show fewer facts":"Show more facts",15,CYAN,true);
                toggle.setGravity(Gravity.CENTER);
                toggle.setBackground(bg(HEADER));
                toggle.setOnClickListener(v->{showMoreRows=!showMoreRows;draw();});
                LinearLayout.LayoutParams button=new LinearLayout.LayoutParams(-1,dp(50));
                button.topMargin=dp(10);
                root.addView(toggle,button);
            }
            gap(7);
            root.addView(label("Times use base-twelve seconds. Accuracy is shown per gross (out of 100₁₂).",
                12,MUTED,false));
        }

        if(selectedLength==0 || selectedLength==1){
            heading("One-digit mastery map","Each cell is an ordered pair. Green = mastered, purple = learning, dim = not practiced.");
            if(selectedOperation<0){
                root.addView(label("Choose a single operation above to view its 12 × 12 fact map.",
                    14,MUTED,false));
            }else{
                heatmap(selectedOperation);
            }
        }

        heading("Practice activity","How many recorded answers you completed each day (last 14 days).");
        activity(history);
        gap(14);
        root.addView(label("Only facts and answers actually recorded by the app are shown. Earlier installations saved per-fact totals, not dated answer histories; their totals are retained without fabricating graph history.",
            12,MUTED,false));
    }

    private void lineGraph(String title,String description,List<Float> series,
                           int kind,float goal,String latest){
        LinearLayout panel=card();
        panel.addView(label(title,17,FG,true));
        innerGap(panel,3);
        panel.addView(label(description,12,MUTED,false));
        innerGap(panel,7);
        panel.addView(label("Most recent  "+latest,14,CYAN,true));
        StatisticsChartView chart=new StatisticsChartView(this,kind,series,goal);
        panel.addView(chart,new LinearLayout.LayoutParams(-1,dp(158)));
        TextView direction=label("Earlier                                               Recent",
            12,MUTED,false);
        direction.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.addView(direction);
    }
    private void sortFacts(List<FactStat> facts){
        switch(sortMode){
            case 1:Collections.sort(facts,(a,b)->Double.compare(a.accuracy(),b.accuracy()));break;
            case 2:Collections.sort(facts,(a,b)->Integer.compare(b.tries,a.tries));break;
            case 3:Collections.sort(facts,(a,b)->Float.compare(a.fluency(),b.fluency()));break;
            case 4:Collections.sort(facts,(a,b)->{
                int k=Integer.compare(a.op,b.op);if(k!=0)return k;
                k=Integer.compare(a.a,b.a);return k!=0?k:Integer.compare(a.b,b.b);
            });break;
            default:Collections.sort(facts,(a,b)->Float.compare(b.fluency(),a.fluency()));
        }
    }
    private TextView cell(String content,int size,int color,int width,boolean header){
        TextView item=label(content,size,color,header);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(5),dp(8),dp(5),dp(8));
        item.setBackground(bg(header?HEADER:SURFACE));
        item.setMinHeight(dp(44));
        return item;
    }
    private LinearLayout table(List<FactStat> facts,int limit){
        LinearLayout wrap=new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        HorizontalScrollView horizontal=new HorizontalScrollView(this);
        horizontal.setFillViewport(false);
        wrap.addView(horizontal);
        LinearLayout contents=new LinearLayout(this);
        contents.setOrientation(LinearLayout.VERTICAL);
        horizontal.addView(contents);
        String[] names={"Fact","Correct / tries","Accuracy","Recent time","Status"};
        int[] widths={122,104,96,106,96};
        LinearLayout heading=new LinearLayout(this);
        contents.addView(heading);
        for(int j=0;j<names.length;j++)
            heading.addView(cell(names[j],12,CYAN,widths[j],true),
                cellParams(widths[j]));
        for(int i=0;i<Math.min(limit,facts.size());i++){
            FactStat f=facts.get(i);
            LinearLayout line=new LinearLayout(this);
            contents.addView(line);
            String[] values={
                f.expression(),
                amount(f.hits)+" / "+amount(f.tries),
                perGross(f.hits,f.tries),
                PracticeTiming.formatSeconds(Math.round(f.fluency())),
                f.mastered?"Mastered":"Learning"
            };
            int[] colors={FG,FG,CYAN,FG,f.mastered?GREEN:PURPLE};
            for(int j=0;j<values.length;j++)
                line.addView(cell(values[j],j==0?14:12,colors[j],widths[j],false),
                    cellParams(widths[j]));
            if(i%2==1)line.setBackgroundColor(HEADER);
        }
        return wrap;
    }
    private LinearLayout.LayoutParams cellParams(int width){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(width),-2);
        p.setMargins(dp(1),dp(1),dp(1),dp(1));
        return p;
    }
    private void heatmap(int op){
        AdaptiveFacts model=new AdaptiveFacts(prefs,op,1,1,0,0);
        HorizontalScrollView horizontal=new HorizontalScrollView(this);
        LinearLayout matrix=new LinearLayout(this);
        matrix.setOrientation(LinearLayout.VERTICAL);
        horizontal.addView(matrix);
        int size=43;
        for(int a=-1;a<12;a++){
            LinearLayout row=new LinearLayout(this);
            matrix.addView(row);
            for(int b=-1;b<12;b++){
                boolean header=a<0||b<0;
                String text=header?(a<0&&b<0?PracticeGrid.SYMBOLS[op]:
                    DozenMath.format(a<0?b:a)):"";
                int ink=header?CYAN:FG;
                int fill=HEADER;
                long key=PracticeGrid.key(Math.max(0,a),Math.max(0,b));
                boolean valid=a>=0&&b>=0&&PracticeGrid.valid(op,1,1,a,b);
                if(valid){
                    int trials=model.attempts(key);
                    fill=trials==0?HEADER:model.mastered(key)?0xFF245A49:
                        model.correct(key)*2<trials?0xFF633E4B:0xFF584676;
                    text=trials==0?"·":DozenMath.format(
                        Math.round(144.0*model.correct(key)/trials));
                }else if(!header)text="—";
                TextView tile=cell(text,header?15:12,ink,size,header);
                tile.setBackground(bg(fill));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(size),dp(size));
                lp.setMargins(dp(1),dp(1),dp(1),dp(1));
                row.addView(tile,lp);
                if(valid){
                    final int x=a,y=b;
                    tile.setContentDescription(PracticeGrid.expression(op,x,y)+
                        ": "+model.correct(key)+" correct out of "+model.attempts(key)+
                        ", "+(model.mastered(key)?"mastered":"not mastered"));
                    tile.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
                        .setTitle(PracticeGrid.expression(op,x,y))
                        .setMessage("Correct: "+amount(model.correct(key))+"/"+
                            amount(model.attempts(key))+
                            "\nAccuracy: "+perGross(model.correct(key),model.attempts(key))+
                            "\nMean time: "+(model.attempts(key)==0?"—":
                              PracticeTiming.formatSeconds(Math.round(model.averageMs(key))))+
                            "\nRecent correct time: "+(model.attempts(key)==0?"—":
                              PracticeTiming.formatSeconds(Math.round(model.recentCorrectMs(key)))))
                        .setPositiveButton("OK",null).show());
                }
            }
        }
        root.addView(horizontal,new LinearLayout.LayoutParams(-1,-2));
        root.addView(label("Cells display correct answers per gross; swipe horizontally to browse.",
            12,MUTED,false));
    }
    private void activity(List<PracticeHistory.Event> events){
        int[] totals=new int[14];
        LocalDate today=LocalDate.now();
        ZoneId zone=ZoneId.systemDefault();
        for(PracticeHistory.Event event:events){
            LocalDate date=Instant.ofEpochSecond(event.epochSeconds).atZone(zone).toLocalDate();
            long distance=ChronoUnit.DAYS.between(date,today);
            if(distance>=0&&distance<14)totals[13-(int)distance]++;
        }
        int high=1;for(int n:totals)high=Math.max(high,n);
        LinearLayout area=card();
        LinearLayout bars=new LinearLayout(this);
        bars.setGravity(Gravity.BOTTOM);
        area.addView(bars,new LinearLayout.LayoutParams(-1,dp(87)));
        for(int day=0;day<14;day++){
            LinearLayout column=new LinearLayout(this);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
            p.setMargins(dp(2),0,dp(2),0);
            bars.addView(column,p);
            View bar=new View(this);
            int height=Math.max(dp(3),Math.round(totals[day]*dp(72)/(float)high));
            bar.setBackground(bg(totals[day]==0?HEADER:GREEN));
            bar.setContentDescription(amount(totals[day])+" answers "+(13-day)+" days ago");
            column.addView(bar,new LinearLayout.LayoutParams(-1,height));
        }
        innerGap(area,8);
        area.addView(label("Older                                       Today",12,MUTED,false));
        innerGap(area,5);
        area.addView(label(amount(events.size())+" recorded answers in selected history",
            13,CYAN,false));
    }
}
