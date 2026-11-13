package com.duodecimal.workout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** Compact, self-contained offline charts; no web view or chart dependency. */
public final class StatisticsChartView extends View {
    public static final int TIME=0, ACCURACY=1;
    private static final int GRID=0xFF616579, CYAN=0xFF8BE9FD, GREEN=0xFF50FA7B;
    private static final int TARGET=0xFFBD93F9;
    private final ArrayList<Float> values=new ArrayList<>();
    private final int kind;
    private final float threshold;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);

    public StatisticsChartView(Context context,int kind,List<Float> points,float threshold) {
        super(context);
        this.kind=kind;
        this.threshold=threshold;
        values.addAll(points);
        setContentDescription((kind==TIME?"Response-time":"Accuracy")+
            " trend chart with "+points.size()+" points, in chronological order.");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    private void stroke(Canvas canvas,int color,float width,float x1,float y1,float x2,float y2) {
        paint.reset();paint.setAntiAlias(true);paint.setColor(color);
        paint.setStrokeWidth(width);paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(x1,y1,x2,y2,paint);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(values.isEmpty())return;
        float density=getResources().getDisplayMetrics().density;
        float left=12*density,right=getWidth()-12*density;
        float top=14*density,bottom=getHeight()-18*density;
        if(right<=left||bottom<=top)return;
        float upper=kind==ACCURACY?1f:Math.max(3000f,threshold*1.2f);
        if(kind==TIME)for(float v:values)upper=Math.max(upper,v*1.15f);
        for(int i=0;i<=4;i++){
            float y=top+(bottom-top)*i/4f;
            stroke(canvas,GRID,0.7f*density,left,y,right,y);
        }
        float yGoal=bottom-Math.max(0,Math.min(1f,threshold/upper))*(bottom-top);
        paint.reset();paint.setAntiAlias(true);paint.setColor(TARGET);
        paint.setStrokeWidth(1.5f*density);paint.setStyle(Paint.Style.STROKE);
        paint.setPathEffect(new DashPathEffect(new float[]{6*density,4*density},0));
        canvas.drawLine(left,yGoal,right,yGoal,paint);
        paint.setPathEffect(null);
        Path line=new Path();
        int n=values.size();
        int color=kind==TIME?CYAN:GREEN;
        for(int i=0;i<n;i++){
            float x=n==1?(left+right)/2:left+(right-left)*i/(n-1f);
            float v=Math.max(0,Math.min(upper,values.get(i)));
            float y=bottom-v/upper*(bottom-top);
            if(i==0)line.moveTo(x,y);else line.lineTo(x,y);
        }
        paint.setColor(color);paint.setStrokeWidth(2.5f*density);
        paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(line,paint);
        int last=n-1;
        float x=n==1?(left+right)/2:right;
        float y=bottom-Math.max(0,Math.min(upper,values.get(last)))/upper*(bottom-top);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(x,y,3.5f*density,paint);
    }
}
