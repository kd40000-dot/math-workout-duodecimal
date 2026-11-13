package com.duodecimal.workout;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Local, independent per-fact adaptive progress. No account or network needed. */
public final class AdaptiveFacts {
    private final SharedPreferences prefs;
    private final String prefix;
    private final String unlockKey;
    public AdaptiveFacts(SharedPreferences prefs, int operation, int digitsA, int digitsB,
                         int rowPage, int colPage) {
        this.prefs=prefs;
        this.prefix="adaptive_v1_"+operation+"_"+digitsA+"_"+digitsB+"_";
        this.unlockKey=prefix+"unlocked_"+rowPage+"_"+colPage;
    }
    private String id(long key) {
        return prefix+PracticeGrid.firstOperand(key)+"_"+PracticeGrid.secondOperand(key)+"_";
    }
    public int attempts(long key) {return prefs.getInt(id(key)+"tries",0);}
    public int correct(long key) {return prefs.getInt(id(key)+"correct",0);}
    public float averageMs(long key) {return prefs.getFloat(id(key)+"latency",0f);}
    public int streak(long key) {return prefs.getInt(id(key)+"streak",0);}
    public void record(long key, boolean right, long elapsedMs) {
        int count=attempts(key);
        float previous=averageMs(key);
        float ms=Math.max(50,Math.min(60000,elapsedMs));
        float average=count==0?ms:(previous*count+ms)/(count+1);
        prefs.edit().putInt(id(key)+"tries",count+1)
            .putInt(id(key)+"correct",correct(key)+(right?1:0))
            .putInt(id(key)+"streak",right?streak(key)+1:0)
            .putFloat(id(key)+"latency",average).apply();
    }
    /** Mastery requires accuracy, multiple correct recalls and reasonable speed. */
    public boolean mastered(long key) {
        int trials=attempts(key);
        return trials>=3 && correct(key)>=3 && streak(key)>=2
            && (double)correct(key)/trials>=0.80 && averageMs(key)<=8000;
    }
    public int unlocked(int total) {
        if(total<=0)return 0;
        int count=Math.max(1,Math.min(total,prefs.getInt(unlockKey,4)));
        return count;
    }
    public void saveUnlocked(int count) {
        prefs.edit().putInt(unlockKey,count).apply();
    }
    /** Deterministic progression for the visible page, sorted row-major. */
    public List<Long> candidates(int operation,int digitA,int digitB,int rowPage,int colPage) {
        ArrayList<Long> all=new ArrayList<>();
        int startA=PracticeGrid.firstForPage(digitA,rowPage);
        int startB=PracticeGrid.firstForPage(digitB,colPage);
        for(int i=0;i<12;i++)for(int j=0;j<12;j++){
            int a=startA+i,b=startB+j;
            if(PracticeGrid.valid(operation,digitA,digitB,a,b))all.add(PracticeGrid.key(a,b));
        }
        return all;
    }
    public int progress(List<Long> candidates) {
        int count=unlocked(candidates.size());
        if(count==0)return 0;
        int mastered=0;
        for(int i=0;i<count;i++)if(mastered(candidates.get(i)))mastered++;
        return mastered;
    }
    public int updateUnlocks(List<Long> candidates) {
        int count=unlocked(candidates.size());
        // Require mastery of at least 80% of active facts; introduce a small
        // batch and retain previous facts for spaced review.
        while(count<candidates.size() && count>0 && progress(candidates)*5>=count*4) {
            int next=Math.min(candidates.size(),count+2);
            if(next==count)break;
            count=next;
            saveUnlocked(count);
            // One batch per evaluation, so unseen facts cannot unlock instantly.
            break;
        }
        return count;
    }
    public List<Long> session(List<Long> candidates,int length,Random random) {
        int count=updateUnlocks(candidates);
        ArrayList<Long> result=new ArrayList<>();
        if(count<=0)return result;
        long previous=-1;
        for(int q=0;q<length;q++){
            // Priority: unseen > slow > inaccurate; occasional mastered
            // review prevents facts from silently disappearing.
            double total=0;
            double[] weights=new double[count];
            for(int i=0;i<count;i++){
                long fact=candidates.get(i);
                int tries=attempts(fact);
                double weight=tries==0?5.0:
                    0.65+3.5*(1.0-(double)correct(fact)/tries)
                    +Math.min(3,averageMs(fact)/5000.0);
                if(mastered(fact))weight*=0.25;
                if(fact==previous && count>1)weight*=0.05;
                weights[i]=weight;total+=weight;
            }
            double target=random.nextDouble()*total;
            int i=0;
            for(;i<count-1;i++){target-=weights[i];if(target<=0)break;}
            previous=candidates.get(i);
            result.add(previous);
        }
        return result;
    }
}
