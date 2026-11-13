package com.duodecimal.workout;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Bounded local answer history for honest time-series charts.
 * Older app versions store aggregate accuracy/latency but no event timeline.
 * Never synthesize historical events from those aggregates.
 */
public final class PracticeHistory {
    private static final String PREFIX="statistics_history_v1_";
    private static final int LIMIT=1200;
    private PracticeHistory(){}

    public static final class Event {
        public final long epochSeconds;
        public final int op, a, b, durationMs;
        public final boolean correct;
        public Event(long epochSeconds,int op,int a,int b,int durationMs,boolean correct) {
            this.epochSeconds=epochSeconds;
            this.op=op;this.a=a;this.b=b;
            this.durationMs=durationMs;this.correct=correct;
        }
        public long key() {return PracticeGrid.key(a,b);}
    }

    public static String storageKey(int op,int digitsA,int digitsB) {
        return PREFIX+op+"_"+digitsA+"_"+digitsB;
    }
    public static void record(SharedPreferences prefs,int op,int digitsA,int digitsB,
                              int a,int b,boolean correct,long durationMs) {
        int ms=(int)Math.max(50L,Math.min(60000L,durationMs));
        long seconds=System.currentTimeMillis()/1000L;
        String key=storageKey(op,digitsA,digitsB);
        String entry=seconds+"|"+a+"|"+b+"|"+ms+"|"+(correct?1:0);
        String previous=prefs.getString(key,"");
        // Drop only the oldest events; cumulative per-fact mastery is unaffected.
        int count=previous.isEmpty()?0:1;
        for(int i=0;i<previous.length();i++)if(previous.charAt(i)==';')count++;
        if(count>=LIMIT) {
            int remove=count-LIMIT+1;
            int start=0;
            while(remove-->0) {
                int next=previous.indexOf(';',start);
                if(next<0){start=previous.length();break;}
                start=next+1;
            }
            previous=previous.substring(start);
        }
        String stored=previous.isEmpty()?entry:previous+";"+entry;
        prefs.edit().putString(key,stored).apply();
    }
    public static List<Event> decode(String serialized,int operation) {
        ArrayList<Event> events=new ArrayList<>();
        if(serialized==null||serialized.isEmpty())return events;
        for(String record:serialized.split(";")) {
            String[] fields=record.split("\\|",-1);
            if(fields.length!=5)continue;
            try {
                long time=Long.parseLong(fields[0]);
                int a=Integer.parseInt(fields[1]),b=Integer.parseInt(fields[2]);
                int ms=Integer.parseInt(fields[3]);
                int flag=Integer.parseInt(fields[4]);
                if(time<=0||a<0||b<0||ms<50||ms>60000||(flag!=0&&flag!=1))continue;
                events.add(new Event(time,operation,a,b,ms,flag==1));
            }catch(NumberFormatException ignore){/* Ignore damaged records. */}
        }
        return events;
    }
    /** op = -1 aggregates all four operations, ordered chronologically. */
    public static List<Event> load(SharedPreferences prefs,int op,int digitsA,int digitsB) {
        ArrayList<Event> events=new ArrayList<>();
        for(int operation=0;operation<4;operation++){
            if(op>=0&&op!=operation)continue;
            events.addAll(decode(prefs.getString(
                storageKey(operation,digitsA,digitsB),""),operation));
        }
        Collections.sort(events,(a,b)->Long.compare(a.epochSeconds,b.epochSeconds));
        return events;
    }
}
