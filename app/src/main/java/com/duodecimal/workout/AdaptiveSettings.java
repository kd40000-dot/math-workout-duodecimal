package com.duodecimal.workout;

import android.content.SharedPreferences;

/** User-adjustable learning rules. Stable defaults preserve existing progress. */
public final class AdaptiveSettings {
    private AdaptiveSettings() {}

    private static final String P = "adaptive_settings_v1_";
    public static final String TARGET_MS=P+"target_ms";
    public static final String ACCURACY=P+"accuracy";
    public static final String CORRECT=P+"min_correct";
    public static final String STREAK=P+"streak";
    public static final String UNLOCK_PERCENT=P+"unlock_percent";
    public static final String REQUIRE_ALL=P+"require_all";
    public static final String RECHECK=P+"recheck";
    public static final String ORDER=P+"order";
    public static final String INITIAL=P+"initial";
    public static final String BATCH=P+"batch";
    public static final String QUESTIONS=P+"questions";
    public static final String PRIORITIZE_SLOW=P+"prioritize_slow";
    public static final String REVIEW_MASTERED=P+"review_mastered";

    public static final int TABLE_ORDER=0;
    public static final int EASY_FIRST=1;
    public static final int FIXED_SHUFFLE=2;
    public static final String[] ORDERS={
        "Table order (row by row)",
        "Simpler facts first",
        "Mixed order (fixed shuffle)"
    };

    private static int clamp(int v,int lo,int hi) {
        return Math.max(lo,Math.min(hi,v));
    }

    public static int targetMs(SharedPreferences p) {
        return clamp(p.getInt(TARGET_MS,8000),2000,30000);
    }
    public static int accuracy(SharedPreferences p) {
        return clamp(p.getInt(ACCURACY,80),50,100);
    }
    public static int minCorrect(SharedPreferences p) {
        return clamp(p.getInt(CORRECT,3),2,10);
    }
    public static int streak(SharedPreferences p) {
        return clamp(p.getInt(STREAK,2),1,6);
    }
    public static int unlockPercent(SharedPreferences p) {
        return clamp(p.getInt(UNLOCK_PERCENT,80),50,100);
    }
    public static boolean requireAll(SharedPreferences p) {
        return p.getBoolean(REQUIRE_ALL,false);
    }
    public static boolean recheck(SharedPreferences p) {
        return p.getBoolean(RECHECK,false);
    }
    public static int order(SharedPreferences p) {
        return clamp(p.getInt(ORDER,TABLE_ORDER),0,ORDERS.length-1);
    }
    public static int initial(SharedPreferences p) {
        return clamp(p.getInt(INITIAL,4),2,12);
    }
    public static int batch(SharedPreferences p) {
        return clamp(p.getInt(BATCH,2),1,6);
    }
    public static int questions(SharedPreferences p) {
        return clamp(p.getInt(QUESTIONS,24),12,60);
    }
    public static boolean prioritizeSlow(SharedPreferences p) {
        return p.getBoolean(PRIORITIZE_SLOW,true);
    }
    public static boolean reviewMastered(SharedPreferences p) {
        return p.getBoolean(REVIEW_MASTERED,true);
    }
}
