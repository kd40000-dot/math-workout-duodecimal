package com.duodecimal.workout;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/** Regression tests for persisted per-answer history used by charts. */
public final class PracticeHistoryTest {
    @Test public void readsCorrectAndIncorrectAnswers() {
        List<PracticeHistory.Event> list=PracticeHistory.decode(
            "1790000000|7|8|1432|1;1790000001|2|3|8330|0",2);
        assertEquals(2,list.size());
        assertEquals(1790000000L,list.get(0).epochSeconds);
        assertEquals(7,list.get(0).a);
        assertEquals(8,list.get(0).b);
        assertEquals(1432,list.get(0).durationMs);
        assertTrue(list.get(0).correct);
        assertEquals(PracticeGrid.key(7,8),list.get(0).key());
        assertEquals(2,list.get(1).op);
        assertFalse(list.get(1).correct);
        assertEquals(8330,list.get(1).durationMs);
    }
    @Test public void malformedEntriesDoNotFabricateEvents() {
        List<PracticeHistory.Event> list=PracticeHistory.decode(
            "garbage;0|1|2|50|1;1790|1|2|5|1;1790|1|2|1200|3;"+
            "1790|1|2|1200|0;1790|bad|2|1200|1",0);
        assertEquals(1,list.size());
        assertEquals(1,list.get(0).a);
        assertFalse(list.get(0).correct);
    }
    @Test public void separatesOperationAndOperandLengthProfiles() {
        assertNotEquals(PracticeHistory.storageKey(0,1,1),
            PracticeHistory.storageKey(2,1,1));
        assertNotEquals(PracticeHistory.storageKey(2,1,1),
            PracticeHistory.storageKey(2,2,1));
    }
}
