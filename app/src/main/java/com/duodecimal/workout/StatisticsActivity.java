package com.duodecimal.workout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Keybr-style statistics: actual trend lines, speed-by-digit bars, mastery
 * heatmap, sortable per-fact table and practice calendar.
 * Existing aggregate performance appears immediately. Time-series charts
 * use only real events captured by PracticeHistory since v1.9.
 */
public final class StatisticsActivity extends Activity {
    private static final int BG=0xFF282A36, SURFACE=0xFF44475A, HEADER=0xFF363849;
    private static final int FG=0xFFF8F8F2, MUTED=0xFFB5B8C6;
    private static final int PURPLE=0xFFBD93F9, CYAN=0xFF8BE9FD, GREEN=0xFF50FA7B;
    private static final int RED=0xFFFF5555, AMBER=0xFFFFB86C;
    private static final String[] OPERATIONS={
        "All operations","Addition","Subtraction","Multiplication","Exact division"
    };
    private static final String[] SORTING={
        "Slowest first","Least accurate","Most mistakes","Most practiced","Table order"
    };
    private static final int PAGE_SIZE=16;
    private SharedPreferences prefs;
    private ScrollView scroll;
    private LinearLayout root;
    private int selectedOp=3, sortMode=0, tablePage=0;

    private static final class FactStat {
        final int op,a,b,attempts,correct;
        final float milliseconds;
        final boolean mastered,unlocked;
        FactStat(int op,int a,int b,AdaptiveFacts engine,boolean unlocked) {
            this.op=op;this.a=a;this.b=b;this.unlocked=unlocked;
            long key=PracticeGrid.key(a,b);
            attempts=engine.attempts(key);
            correct=engine.correct(key);
            milliseconds=engine.recentCorrectMs(key);
            mastered=engine.mastered(key);
        }
        int misses(){return attempts-correct;}
        int accuracy(){return attempts==0?0:(int)Math.round(100.0*correct/attempts);}
        String equation(){return PracticeGrid.expression(op,a,b);}
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("duodecimal_workout",MODE_PRIVATE);
        selectedOp=Math.max(0,Math.min(4,prefs.getInt("stats_operation",3)));
        sortMode=Math.max(0,Math.min(SORTING.length-1,prefs.getInt("stats_sort",0)));
        scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(12),dp(16),dp(34));
        scroll.addView(root);
        setContentView(scroll);
    }
    @Override protected void onResume(){super.onResume();draw();}
    @Override public void onBackPressed(){
        if(DrawerNavigation.close(this))return;
        super.onBackPressed();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout vertical(){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);return row;}
    private LinearLayout horizontal(){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);return row;}
    private GradientDrawable background(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private TextView text(String value,int size,int ink,boolean glyph){
        TextView view=glyph?new Numerals.NumeralTextView(this):new TextView(this);
        view.setText(value);view.setTextSize(size);view.setTextColor(ink);
        return view;
    }
    private void gap(LinearLayout p,int n){p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(n)));}
    private LinearLayout card(){
        LinearLayout card=vertical();
        card.setPadding(dp(14),dp(14),dp(14),dp(16));
        card.setBackground(background(SURFACE,14));
        root.addView(card,new LinearLayout.LayoutParams(-1,-2));
        return card;
    }
    private TextView section(LinearLayout parent,String title,String detail){
        TextView heading=text(title,19,FG,false);parent.addView(heading);
        gap(parent,4);
        TextView description=text(detail,13,MUTED,false);parent.addView(description);
        gap(parent,12);
        return heading;
    }
    private void heading(String name,String detail){
        gap(root,22);
        root.addView(text(name,20,FG,false));
        gap(root,5);
        root.addView(text(detail,13,MUTED,false));
        gap(root,12);
    }
    private void progress(LinearLayout container,int val,int max,int color){
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1,max));bar.setProgress(val);
        bar.setProgressTintList(ColorStateList.valueOf(color));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(HEADER));
        bar.setContentDescription(val+" out of "+max);
        container.addView(bar,new LinearLayout.LayoutParams(-1,dp(9)));
    }
    private Button button(String label){
        Button b=new Button(this);
        b.setAllCaps(false);b.setText(label);b.setTextSize(14);
        b.setTextColor(FG);b.setBackground(background(HEADER,10));
        return b;
    }
    private View styleOption(View v){
        if(v instanceof TextView){
            TextView t=(TextView)v;
            t.setTextColor(FG);
            t.setTextSize(15);
            t.setPadding(dp(12),dp(12),dp(12),dp(12));
        }
        return v;
    }
    private Spinner chooser(String[] items,int selected){
        Spinner sp=new Spinner(this,Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(
            this,android.R.layout.simple_spinner_item,items){
            @Override public View getView(int p,View old,ViewGroup parent){
                return styleOption(super.getView(p,old,parent));
            }
            @Override public View getDropDownView(int p,View old,ViewGroup parent){
                return styleOption(super.getDropDownView(p,old,parent));
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        sp.setSelection(selected);
        sp.setBackground(background(HEADER,9));
        sp.setPopupBackgroundDrawable(background(HEADER,9));
        return sp;
    }
    private String dozen(long value){return DozenMath.format(value);}
    /** One base-12 fractional digit, including for seconds below one. */
    private String seconds(float milliseconds){
        if(milliseconds<=0)return "—";
        long twelfths=Math.round(milliseconds*12.0/1000.0);
        return dozen(twelfths/12)+"."+dozen(twelfths%12)+" s";
    }

    private List<FactStat> facts(int op){
        ArrayList<FactStat> result=new ArrayList<>();
        for(int operation=0;operation<4;operation++){
            if(op!=-1 && operation!=op)continue;
            AdaptiveFacts m=new AdaptiveFacts(prefs,operation,1,1,0,0);
            List<Long> candidates=m.candidates(operation,1,1,0,0);
            int unlocked=m.unlocked(candidates.size());
            for(int i=0;i<candidates.size();i++){
                long key=candidates.get(i);
                result.add(new FactStat(operation,PracticeGrid.firstOperand(key),
                    PracticeGrid.secondOperand(key),m,i<unlocked));
            }
        }
        return result;
    }
    private void draw(){
        root.removeAllViews();
        DrawerNavigation.addBar(this,root,"Statistics",DrawerNavigation.STATS);
        root.addView(text("Learning statistics",27,PURPLE,false));
        gap(root,7);
        root.addView(text("Measure your accuracy, response time and mastery. All data stays on your device.",14,MUTED,false));
        gap(root,18);
        root.addView(text("Operation",14,CYAN,false));
        gap(root,6);
        Spinner select=chooser(OPERATIONS,selectedOp);
        root.addView(select,new LinearLayout.LayoutParams(-1,dp(53)));
        select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(AdapterView<?> parent){}
            @Override public void onItemSelected(AdapterView<?> parent,View v,int position,long id){
                if(position==selectedOp)return;
                selectedOp=position;tablePage=0;
                prefs.edit().putInt("stats_operation",position).apply();
                draw();
            }
        });
        gap(root,10);
        root.addView(text("Scope: one-digit × one-digit facts (0–↋). Counts are base twelve; percentages are conventional decimal.",12,MUTED,false));
        int op=selectedOp-1;
        List<FactStat> rows=facts(op);
        List<PracticeHistory.Event> events=PracticeHistory.load(prefs,op,1,1);
        overview(rows);
        trends(events);
        responseHistogram(events);
        digitBars(rows);
        heatmap(op);
        factTable(rows);
        calendar(events);
        gap(root,20);
        root.addView(text("The learning curves use recorded answers from v1.9.0 onward. Older attempts remain included in all-time totals, per-fact tables and mastery statistics.",13,MUTED,false));
    }
    private void overview(List<FactStat> rows){
        heading("Overview","All-time one-digit facts for the selected operation.");
        int total=rows.size(),visited=0,fluent=0,attempts=0,correct=0,unlocked=0;
        double weighted=0;int successful=0;
        for(FactStat f:rows){
            attempts+=f.attempts;correct+=f.correct;
            if(f.attempts>0)visited++;
            if(f.mastered)fluent++;
            if(f.unlocked)unlocked++;
            if(f.correct>0 && f.milliseconds>0){
                weighted+=f.milliseconds*f.correct;successful+=f.correct;
            }
        }
        LinearLayout box=card();
        box.addView(text(dozen(fluent)+" / "+dozen(total)+" mastered",24,GREEN,true));
        gap(box,10);
        progress(box,fluent,total,GREEN);
        gap(box,12);
        box.addView(text("Studied "+dozen(visited)+" facts   ·   Unlocked "+
            dozen(unlocked)+"   ·   Attempts "+dozen(attempts),15,FG,true));
        gap(box,7);
        box.addView(text("Accuracy  "+(attempts==0?"—":
            String.format(Locale.US,"%.1f%%",100.0*correct/attempts)),14,CYAN,false));
        gap(box,7);
        box.addView(text("Mean recent correct-answer time  "+
            (successful==0?"—":seconds((float)(weighted/successful))),14,FG,true));
        if(attempts==0){
            gap(box,14);
            box.addView(text("No recorded facts yet. Complete a practice session to populate this dashboard.",13,MUTED,false));
        }
    }

    private static float[] accuracySeries(List<PracticeHistory.Event> events){
        float[] values=new float[events.size()];
        int hits=0;
        for(int i=0;i<events.size();i++){
            if(events.get(i).correct)hits++;
            if(i>=12 && events.get(i-12).correct)hits--;
            values[i]=100f*hits/Math.min(i+1,12);
        }
        return values;
    }
    private static float[] speedSeries(List<PracticeHistory.Event> events){
        ArrayList<Float> answerTimes=new ArrayList<>();
        for(PracticeHistory.Event event:events)
            if(event.correct)answerTimes.add(event.durationMs/1000f);
        float[] curve=new float[answerTimes.size()];
        float total=0;
        for(int i=0;i<curve.length;i++){
            total+=answerTimes.get(i);
            if(i>=8)total-=answerTimes.get(i-8);
            curve[i]=total/Math.min(i+1,8);
        }
        return curve;
    }
    private void trends(List<PracticeHistory.Event> events){
        heading("Learning curves","Rolling averages show whether practice is improving fluency and accuracy.");
        LinearLayout time=card();
        section(time,"Response time","Rolling average of the last 8 correct answers; lower is better.");
        float[] speeds=speedSeries(events);
        if(speeds.length==0){
            time.addView(text("No timed correct answers yet. A response-time curve will appear after practice.",13,MUTED,false));
        }else{
            float latest=speeds[speeds.length-1];
            time.addView(text("Latest  "+seconds(latest*1000f)+"  ·  Goal  "+
                seconds(AdaptiveSettings.targetMs(prefs)),15,CYAN,true));
            gap(time,8);
            time.addView(new TrendView(speeds,false,
                AdaptiveSettings.targetMs(prefs)/1000f),new LinearLayout.LayoutParams(-1,dp(208)));
            gap(time,5);
            time.addView(text("Earlier  →  Most recent correct answers",12,MUTED,false));
        }
        gap(root,12);
        LinearLayout accuracy=card();
        section(accuracy,"Accuracy","Rolling accuracy over the last 12 answers; higher is better.");
        float[] score=accuracySeries(events);
        if(score.length==0){
            accuracy.addView(text("Answer history starts with this update. Earlier aggregate accuracy remains available above.",13,MUTED,false));
        }else{
            accuracy.addView(text("Latest  "+
                String.format(Locale.US,"%.1f%%",score[score.length-1]),15,CYAN,true));
            gap(accuracy,8);
            accuracy.addView(new TrendView(score,true,AdaptiveSettings.accuracy(prefs)),
                new LinearLayout.LayoutParams(-1,dp(208)));
            gap(accuracy,5);
            accuracy.addView(text("Earlier  →  Latest answer   ·   Dashed line = target",12,MUTED,false));
        }
    }

    private final class TrendView extends View {
        private final float[] series;
        private final boolean percentage;
        private final float goal;
        TrendView(float[] values,boolean percentage,float goal){
            super(StatisticsActivity.this);series=values;
            this.percentage=percentage;this.goal=goal;
            setContentDescription((percentage?"Accuracy":"Response time")+
                " trend across "+values.length+" recorded responses. Current "+
                (values.length==0?"none":values[values.length-1])+
                ", target "+goal+".");
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            if(series.length==0)return;
            float left=dp(8),top=dp(12),right=getWidth()-dp(8),
                bottom=getHeight()-dp(12);
            float height=bottom-top,width=right-left;
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(1));
            float max=percentage?100f:Math.max(goal*1.4f,2f);
            if(!percentage){
                for(float n:series)if(Float.isFinite(n))max=Math.max(max,n*1.05f);
            }
            p.setColor(0xFF555768);
            for(int i=0;i<=4;i++){
                float y=top+i*height/4f;
                canvas.drawLine(left,y,right,y,p);
            }
            p.setColor(CYAN);
            p.setStrokeWidth(dp(2));
            p.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(7),dp(5)},0));
            float targetY=bottom-Math.min(max,goal)/max*height;
            canvas.drawLine(left,targetY,right,targetY,p);
            p.setPathEffect(null);
            p.setColor(percentage?CYAN:PURPLE);
            p.setStrokeWidth(dp(2.4f));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            Path path=new Path();
            int shown=Math.min(200,series.length),start=series.length-shown;
            for(int i=0;i<shown;i++){
                float x=shown==1?left+width/2:left+i*width/(shown-1);
                float value=Math.max(0,Math.min(max,series[start+i]));
                float y=bottom-value/max*height;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            canvas.drawPath(path,p);
            float latest=series[series.length-1];
            float y=bottom-Math.max(0,Math.min(max,latest))/max*height;
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(shown==1?left+width/2:right,y,dp(4),p);
        }
    }

    private void responseHistogram(List<PracticeHistory.Event> events){
        heading("Response-time distribution","How often you answer correctly within each time range.");
        int[] bins=new int[6];int max=0,successful=0;
        for(PracticeHistory.Event e:events)if(e.correct){
            successful++;
            int s=e.durationMs;
            int slot=s<2000?0:s<4000?1:s<6000?2:s<8000?3:s<12000?4:5;
            bins[slot]++;
            max=Math.max(max,bins[slot]);
        }
        LinearLayout box=card();
        if(successful==0) {
            box.addView(text("No response-time samples yet.",13,MUTED,false));
            return;
        }
        String[] labels={"0–2 s","2–4 s","4–6 s","6–8 s","8–12 s","12+ s"};
        for(int i=0;i<6;i++){
            LinearLayout row=horizontal();row.setGravity(Gravity.CENTER_VERTICAL);
            TextView name=text(labels[i],13,FG,true);
            row.addView(name,new LinearLayout.LayoutParams(dp(78),-2));
            ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
            bar.setMax(Math.max(1,max));bar.setProgress(bins[i]);
            bar.setProgressTintList(ColorStateList.valueOf(PURPLE));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(HEADER));
            row.addView(bar,new LinearLayout.LayoutParams(0,dp(11),1));
            TextView value=text(dozen(bins[i]),13,CYAN,true);
            value.setGravity(Gravity.END);
            row.addView(value,new LinearLayout.LayoutParams(dp(52),-2));
            box.addView(row,new LinearLayout.LayoutParams(-1,dp(39)));
        }
    }

    private void digitBars(List<FactStat> rows){
        heading("Speed by first digit","Recent correct-answer time for each first operand. Shorter bars are faster.");
        float[] ms=new float[12];
        int[] count=new int[12];
        for(FactStat f:rows)if(f.correct>0&&f.milliseconds>0){
            ms[f.a]+=f.milliseconds*f.correct;
            count[f.a]+=f.correct;
        }
        int available=0;float slowest=Math.max(AdaptiveSettings.targetMs(prefs)*1.25f,2000f);
        for(int i=0;i<12;i++){
            if(count[i]>0){ms[i]/=count[i];available++;slowest=Math.max(slowest,ms[i]);}
        }
        LinearLayout box=card();
        if(available==0){
            box.addView(text("Speed bars appear after your first correct answers.",13,MUTED,false));
            return;
        }
        for(int i=0;i<12;i++){
            LinearLayout row=horizontal();row.setGravity(Gravity.CENTER_VERTICAL);
            TextView d=text(dozen(i),22,FG,true);d.setGravity(Gravity.CENTER);
            row.addView(d,new LinearLayout.LayoutParams(dp(42),dp(43)));
            ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
            progress.setMax(1000);
            progress.setProgress(count[i]==0?0:Math.max(5,(int)(1000*ms[i]/slowest)));
            progress.setProgressTintList(ColorStateList.valueOf(count[i]==0?HEADER:
                ms[i]<=AdaptiveSettings.targetMs(prefs)?GREEN:AMBER));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(HEADER));
            row.addView(progress,new LinearLayout.LayoutParams(0,dp(12),1));
            TextView value=text(count[i]==0?"—":seconds(ms[i]),13,CYAN,true);
            value.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
            row.addView(value,new LinearLayout.LayoutParams(dp(86),-1));
            box.addView(row,new LinearLayout.LayoutParams(-1,dp(44)));
        }
        gap(box,7);
        box.addView(text("Green = meets current target · Amber = slower · Empty = no successful answers.",12,MUTED,false));
    }

    private static int heatColor(FactStat f){
        if(!f.unlocked)return HEADER;
        if(f.attempts==0)return 0xFF505469;
        if(f.mastered)return 0xFF327A5B;
        if(f.accuracy()<70)return 0xFF85435A;
        if(f.milliseconds>0&&f.milliseconds>8000)return 0xFF986B41;
        return 0xFF615C9A;
    }
    private void heatmap(int op){
        heading("Fact mastery heatmap","One cell per ordered pair. Tap a cell for its performance details.");
        if(op<0){
            LinearLayout box=card();
            box.addView(text("Choose a single operation above to view its 12 × 12 mastery map.",14,MUTED,false));
            return;
        }
        LinearLayout legend=horizontal();
        legend.addView(text("Unseen  ·  Learning  ·  Slow  ·  Mastered",12,MUTED,false));
        root.addView(legend);
        gap(root,8);
        HorizontalScrollView scroller=new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(true);
        root.addView(scroller,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout table=vertical();scroller.addView(table);
        LinearLayout headings=horizontal();table.addView(headings);
        heatCell(headings,PracticeGrid.SYMBOLS[op],HEADER,CYAN,null);
        for(int b=0;b<12;b++)heatCell(headings,dozen(b),HEADER,CYAN,null);
        AdaptiveFacts model=new AdaptiveFacts(prefs,op,1,1,0,0);
        List<Long> order=model.candidates(op,1,1,0,0);
        int unlock=model.unlocked(order.size());
        for(int a=0;a<12;a++){
            LinearLayout row=horizontal();table.addView(row);
            heatCell(row,dozen(a),HEADER,CYAN,null);
            for(int b=0;b<12;b++){
                if(!PracticeGrid.valid(op,1,1,a,b)){
                    heatCell(row,"—",HEADER,MUTED,null);
                    continue;
                }
                long key=PracticeGrid.key(a,b);
                FactStat f=new FactStat(op,a,b,model,
                    order.indexOf(key)<unlock);
                int ink=f.mastered?FG:FG;
                heatCell(row,dozen(PracticeGrid.answer(op,a,b)),
                    heatColor(f),ink,()->detail(f));
            }
        }
        gap(root,8);
        root.addView(text("Scroll horizontally to inspect all twelve columns. Dark cells can also mean locked or unpracticed; tap to see which.",12,MUTED,false));
    }
    private void heatCell(LinearLayout row,String value,int bg,int ink,Runnable onTap){
        TextView cell=text(value,15,ink,true);
        cell.setGravity(Gravity.CENTER);
        cell.setBackground(background(bg,5));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(47),dp(47));
        lp.setMargins(dp(1),dp(1),dp(1),dp(1));
        row.addView(cell,lp);
        if(onTap!=null){
            cell.setClickable(true);cell.setFocusable(true);
            cell.setContentDescription("Fact result "+value+". Tap for details.");
            cell.setOnClickListener(v->onTap.run());
        }
    }

    private void detail(FactStat f){
        String status=f.mastered?"Mastered":!f.unlocked?"Locked":
            f.attempts==0?"Not practiced":"Learning";
        new AlertDialog.Builder(this)
            .setTitle(f.equation())
            .setMessage("Status: "+status+
                "\nAttempts: "+dozen(f.attempts)+
                "\nCorrect: "+dozen(f.correct)+
                "\nAccuracy: "+(f.attempts==0?"—":f.accuracy()+"%")+
                "\nRecent successful time: "+seconds(f.milliseconds)+
                "\nTarget: "+seconds(AdaptiveSettings.targetMs(prefs)))
            .setPositiveButton("Close",null).show();
    }
    private void factTable(List<FactStat> facts){
        heading("Fact performance table","Rank individual facts to find the ones worth practicing next.");
        Spinner order=chooser(SORTING,sortMode);
        root.addView(order,new LinearLayout.LayoutParams(-1,dp(52)));
        order.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(AdapterView<?> parent){}
            @Override public void onItemSelected(AdapterView<?> parent,View view,int selected,long id){
                if(sortMode==selected)return;
                sortMode=selected;tablePage=0;
                prefs.edit().putInt("stats_sort",sortMode).apply();draw();
            }
        });
        gap(root,10);
        ArrayList<FactStat> ranked=new ArrayList<>();
        for(FactStat f:facts)if(sortMode==4||f.attempts>0)ranked.add(f);
        switch(sortMode){
            case 0:
                ranked.sort((a,b)->{
                    if(a.correct==0 && b.correct>0)return 1;
                    if(b.correct==0 && a.correct>0)return -1;
                    int c=Float.compare(b.milliseconds,a.milliseconds);
                    return c!=0?c:Integer.compare(b.attempts,a.attempts);
                });break;
            case 1:
                ranked.sort((a,b)->{
                    int c=Integer.compare(a.accuracy(),b.accuracy());
                    return c!=0?c:Integer.compare(b.attempts,a.attempts);
                });break;
            case 2:ranked.sort((a,b)->Integer.compare(b.misses(),a.misses()));break;
            case 3:ranked.sort((a,b)->Integer.compare(b.attempts,a.attempts));break;
            default:ranked.sort((a,b)->{
                int c=Integer.compare(a.op,b.op);
                if(c==0)c=Integer.compare(a.a,b.a);
                return c==0?Integer.compare(a.b,b.b):c;
            });break;
        }
        if(ranked.isEmpty()){
            LinearLayout box=card();
            box.addView(text("No practiced facts yet. Choose 'Table order' to see all available facts.",14,MUTED,false));
            return;
        }
        int pages=(ranked.size()+PAGE_SIZE-1)/PAGE_SIZE;
        tablePage=Math.max(0,Math.min(pages-1,tablePage));
        LinearLayout pagination=horizontal();pagination.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=button("‹");prev.setEnabled(tablePage>0);prev.setAlpha(tablePage>0?1f:.4f);
        pagination.addView(prev,new LinearLayout.LayoutParams(dp(52),dp(48)));
        TextView page=text("Page "+dozen(tablePage+1)+" / "+dozen(pages),14,CYAN,true);
        page.setGravity(Gravity.CENTER);
        pagination.addView(page,new LinearLayout.LayoutParams(0,-2,1));
        Button next=button("›");next.setEnabled(tablePage+1<pages);
        next.setAlpha(tablePage+1<pages?1f:.4f);
        pagination.addView(next,new LinearLayout.LayoutParams(dp(52),dp(48)));
        root.addView(pagination,new LinearLayout.LayoutParams(-1,dp(53)));
        prev.setOnClickListener(v->{tablePage--;draw();});
        next.setOnClickListener(v->{tablePage++;draw();});
        HorizontalScrollView scroller=new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(true);
        root.addView(scroller,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout table=vertical();scroller.addView(table);
        LinearLayout head=horizontal();table.addView(head);
        tableCell(head,"Fact",140,HEADER,true,false);
        tableCell(head,"Accuracy",83,HEADER,true,false);
        tableCell(head,"Time",82,HEADER,true,false);
        tableCell(head,"Tries",60,HEADER,true,false);
        tableCell(head,"State",93,HEADER,true,false);
        int from=tablePage*PAGE_SIZE,to=Math.min(ranked.size(),from+PAGE_SIZE);
        for(int i=from;i<to;i++){
            FactStat f=ranked.get(i);
            LinearLayout row=horizontal();table.addView(row);
            int fill=i%2==0?SURFACE:HEADER;
            tableCell(row,f.equation(),140,fill,false,true).setOnClickListener(v->detail(f));
            tableCell(row,f.attempts==0?"—":f.accuracy()+"%",83,fill,false,false);
            tableCell(row,f.correct==0?"—":seconds(f.milliseconds),82,fill,false,true);
            tableCell(row,dozen(f.attempts),60,fill,false,true);
            tableCell(row,f.mastered?"Mastered":f.unlocked?"Learning":"Locked",
                93,fill,false,false);
        }
        gap(root,7);
        root.addView(text("Tap a fact for details. Swipe the table sideways for all columns.",12,MUTED,false));
    }
    private TextView tableCell(LinearLayout row,String value,int width,int fill,
                               boolean header,boolean glyph){
        TextView cell=text(value,header?13:12,header?CYAN:FG,glyph);
        cell.setGravity(Gravity.CENTER_VERTICAL);
        cell.setPadding(dp(7),0,dp(6),0);
        cell.setBackgroundColor(fill);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(width),dp(46));
        lp.setMargins(dp(1),dp(1),dp(1),dp(1));row.addView(cell,lp);
        return cell;
    }

    private void calendar(List<PracticeHistory.Event> events){
        heading("Practice calendar","Number of answers recorded each day over the last 28 days.");
        Map<LocalDate,Integer> days=new HashMap<>();
        ZoneId zone=ZoneId.systemDefault();
        for(PracticeHistory.Event event:events){
            LocalDate date=Instant.ofEpochSecond(event.epochSeconds).atZone(zone).toLocalDate();
            days.put(date,days.getOrDefault(date,0)+1);
        }
        LocalDate today=LocalDate.now();
        int[] totals=new int[28];
        int max=0,total=0,active=0;
        for(int i=0;i<28;i++){
            LocalDate date=today.minusDays(27-i);
            totals[i]=days.getOrDefault(date,0);
            max=Math.max(max,totals[i]);total+=totals[i];
            if(totals[i]>0)active++;
        }
        LinearLayout card=card();
        card.addView(text("Active days  "+dozen(active)+"/"+dozen(28)+
            "   ·   Answers  "+dozen(total),14,CYAN,true));
        gap(card,12);
        LinearLayout weeks=horizontal();
        card.addView(weeks);
        for(int w=0;w<4;w++){
            LinearLayout week=vertical();
            weeks.addView(week,new LinearLayout.LayoutParams(0,-2,1));
            for(int d=0;d<7;d++){
                int index=w*7+d;
                int count=totals[index];
                LocalDate date=today.minusDays(27-index);
                TextView square=text(String.valueOf(date.getDayOfMonth()),11,
                    count>0?FG:MUTED,false);
                square.setGravity(Gravity.CENTER);
                int color=count==0?HEADER:
                    count*3<max?0xFF30524A:count*3<max*2?0xFF327A60:0xFF40A879;
                square.setBackground(background(color,6));
                square.setContentDescription(date+": "+count+" answers");
                LinearLayout.LayoutParams cell=new LinearLayout.LayoutParams(-1,dp(37));
                cell.setMargins(dp(3),dp(3),dp(3),dp(3));
                week.addView(square,cell);
            }
        }
        gap(card,8);
        card.addView(text("Darker = fewer answers. Only recorded events appear; historical dates are never estimated.",12,MUTED,false));
    }
}
