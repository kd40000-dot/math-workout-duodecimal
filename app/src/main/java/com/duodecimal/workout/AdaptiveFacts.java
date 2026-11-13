package com.duodecimal.workout;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Adaptive practice with persistent per-fact history and configurable mastery. */
public final class AdaptiveFacts {
    private final SharedPreferences prefs;
    private final String prefix;
    private final int operation, digitsA, digitsB;
    private final String unlockKey;

    public AdaptiveFacts(SharedPreferences prefs, int operation, int digitsA, int digitsB,
                         int rowPage, int colPage) {
        this.prefs=prefs;
        this.operation=operation;this.digitsA=digitsA;this.digitsB=digitsB;
        // Retain existing v1 keys: changing the UI or thresholds does not erase learning history.
        this.prefix="adaptive_v1_"+operation+"_"+digitsA+"_"+digitsB+"_";
        this.unlockKey=prefix+"unlocked_"+rowPage+"_"+colPage;
    }

    private String id(long key) {
        return prefix+PracticeGrid.firstOperand(key)+"_"+
            PracticeGrid.secondOperand(key)+"_";
    }
    public int attempts(long key) {return prefs.getInt(id(key)+"tries",0);}
    public int correct(long key) {return prefs.getInt(id(key)+"correct",0);}
    public float averageMs(long key) {return prefs.getFloat(id(key)+"latency",0f);}
    public float recentCorrectMs(long key) {
        return prefs.getFloat(id(key)+"recent_correct_ms",averageMs(key));
    }
    public int streak(long key) {return prefs.getInt(id(key)+"streak",0);}
    private long lastSeen(long key) {return prefs.getLong(id(key)+"last_seen",0L);}

    public void record(long key, boolean right, long elapsedMs) {
        int count=attempts(key);
        float previous=averageMs(key);
        float ms=Math.max(50,Math.min(60000,elapsedMs));
        float mean=count==0?ms:(previous*count+ms)/(count+1);
        long event=prefs.getLong(prefix+"answer_sequence",0L)+1;
        SharedPreferences.Editor editor=prefs.edit()
            .putInt(id(key)+"tries",count+1)
            .putInt(id(key)+"correct",correct(key)+(right?1:0))
            .putInt(id(key)+"streak",right?streak(key)+1:0)
            .putFloat(id(key)+"latency",mean)
            .putLong(id(key)+"last_seen",event)
            .putLong(prefix+"answer_sequence",event);
        // Measure fluency using successful recalls; exponentially weighted
        // recent time lets a learner recover from older, slower answers.
        if(right) {
            float old=prefs.getFloat(id(key)+"recent_correct_ms",0);
            editor.putFloat(id(key)+"recent_correct_ms",old<=0?ms:old*.7f+ms*.3f);
        }
        editor.apply();
        PracticeHistory.record(prefs,operation,digitsA,digitsB,
            PracticeGrid.firstOperand(key),PracticeGrid.secondOperand(key),
            right,elapsedMs);
        if(mastered(key))prefs.edit().putBoolean(id(key)+"ever_mastered",true).apply();
    }

    public boolean mastered(long key) {
        int trials=attempts(key);
        if(trials<AdaptiveSettings.minCorrect(prefs) ||
                correct(key)<AdaptiveSettings.minCorrect(prefs) ||
                streak(key)<AdaptiveSettings.streak(prefs))return false;
        return (long)correct(key)*100>=
                (long)trials*AdaptiveSettings.accuracy(prefs)
            && recentCorrectMs(key)<=AdaptiveSettings.targetMs(prefs);
    }
    public boolean everMastered(long key) {
        return prefs.getBoolean(id(key)+"ever_mastered",false) || mastered(key);
    }
    public boolean unlockEligible(long key) {
        return AdaptiveSettings.recheck(prefs)?mastered(key):everMastered(key);
    }

    public int unlocked(int total) {
        if(total<=0)return 0;
        return Math.max(1,Math.min(total,
            prefs.getInt(unlockKey,AdaptiveSettings.initial(prefs))));
    }
    public void saveUnlocked(int count){prefs.edit().putInt(unlockKey,count).apply();}

    public List<Long> candidates(int operation,int digitA,int digitB,int rowPage,int colPage) {
        ArrayList<Long> all=new ArrayList<>();
        int a0=PracticeGrid.firstForPage(digitA,rowPage);
        int b0=PracticeGrid.firstForPage(digitB,colPage);
        for(int a=a0;a<a0+12;a++)for(int b=b0;b<b0+12;b++)
            if(PracticeGrid.valid(operation,digitA,digitB,a,b))
                all.add(PracticeGrid.key(a,b));
        int order=AdaptiveSettings.order(prefs);
        if(order==AdaptiveSettings.EASY_FIRST){
            Collections.sort(all,(one,two)->{
                long lhs=easeScore(operation,one),rhs=easeScore(operation,two);
                return lhs==rhs?Long.compare(one,two):Long.compare(lhs,rhs);
            });
        }else if(order==AdaptiveSettings.FIXED_SHUFFLE){
            // Stable across app restarts, so unlocking doesn't move around.
            Collections.shuffle(all,new Random(
                (((long)prefix.hashCode())<<32)^(rowPage*131L+colPage*719L)));
        }
        return all;
    }

    private static long easeScore(int op,long key){
        long a=PracticeGrid.firstOperand(key),b=PracticeGrid.secondOperand(key);
        if(op==PracticeGrid.OP_MULTIPLY && (a==0||b==0))return 0;
        if(op==PracticeGrid.OP_MULTIPLY && (a==1||b==1))return 1;
        if((op==PracticeGrid.OP_ADD || op==PracticeGrid.OP_SUB) && b==0)return 0;
        if(op==PracticeGrid.OP_DIVIDE && a==b)return 0;
        return 2+a+b;
    }

    public int progress(List<Long> candidates){
        int count=unlocked(candidates.size()),mastered=0;
        for(int i=0;i<count;i++)if(mastered(candidates.get(i)))mastered++;
        return mastered;
    }

    public int eligibleProgress(List<Long> candidates){
        int count=unlocked(candidates.size()),eligible=0;
        for(int i=0;i<count;i++)if(unlockEligible(candidates.get(i)))eligible++;
        return eligible;
    }

    public int updateUnlocks(List<Long> candidates){
        int count=unlocked(candidates.size());
        if(count>=candidates.size()||count==0)return count;
        int eligible=eligibleProgress(candidates);
        int threshold=AdaptiveSettings.requireAll(prefs)?100:
            AdaptiveSettings.unlockPercent(prefs);
        if((long)eligible*100 >= (long)count*threshold){
            count=Math.min(candidates.size(),count+AdaptiveSettings.batch(prefs));
            saveUnlocked(count);
        }
        // Only one batch per evaluation. Newly introduced facts must be learned
        // before the next unlock, and the permanent unlocked count never shrinks.
        return count;
    }

    /** Weighted drill sampling: weak, slow and overdue facts appear more often. */
    public List<Long> session(List<Long> candidates,int length,Random random){
        int count=updateUnlocks(candidates);
        ArrayList<Long> result=new ArrayList<>();
        if(count<=0||length<=0)return result;
        long previous=-1;
        long sequence=prefs.getLong(prefix+"answer_sequence",0);
        for(int q=0;q<length;q++){
            double total=0;
            double[] weights=new double[count];
            boolean canSkipMastered=false;
            for(int i=0;i<count;i++)if(!mastered(candidates.get(i)))canSkipMastered=true;
            for(int i=0;i<count;i++){
                long key=candidates.get(i);
                int tries=attempts(key);
                double accuracy=tries==0?0:(double)correct(key)/tries;
                double speedPenalty=AdaptiveSettings.prioritizeSlow(prefs)?
                    Math.min(3.0,recentCorrectMs(key)/
                        (float)AdaptiveSettings.targetMs(prefs)*1.5):0.0;
                double weight=tries==0?6.0:
                    0.75+4.0*(1.0-accuracy)+speedPenalty;
                if(mastered(key)){
                    weight=(!AdaptiveSettings.reviewMastered(prefs)&&canSkipMastered)
                        ? 0.0 : weight*0.18;
                }
                long age=sequence-lastSeen(key);
                if(tries>0 && age>8)weight*=1.3;
                if(key==previous && count>1)weight*=0.05;
                weights[i]=weight;total+=weight;
            }
            double target=random.nextDouble()*Math.max(total,0.0001);
            int chosen=count-1;
            for(int i=0;i<count;i++){
                target-=weights[i];
                if(target<=0){chosen=i;break;}
            }
            previous=candidates.get(chosen);
            result.add(previous);
        }
        return result;
    }
}
