package com.duodecimal.workout;

import org.junit.Test;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import static org.junit.Assert.*;

public class PracticeGridTest {
    @Test public void baseTwelveRangesAndPages() {
        assertEquals(0, PracticeGrid.firstForDigits(1));
        assertEquals(11, PracticeGrid.lastForDigits(1));
        assertEquals(12, PracticeGrid.firstForDigits(2));
        assertEquals(143, PracticeGrid.lastForDigits(2));
        assertEquals(144, PracticeGrid.firstForDigits(3));
        assertEquals(1727, PracticeGrid.lastForDigits(3));
        assertEquals(1728, PracticeGrid.firstForDigits(4));
        assertEquals(20735, PracticeGrid.lastForDigits(4));
        assertEquals(1, PracticeGrid.pageCount(1));
        assertEquals(11, PracticeGrid.pageCount(2));
        assertEquals(132, PracticeGrid.pageCount(3));
        assertEquals(1584, PracticeGrid.pageCount(4));
        assertEquals(132, PracticeGrid.firstForPage(2, 10));
        assertEquals("↋0", DozenMath.format(132));
    }

    @Test public void tableAnswersAndExactDivision() {
        assertEquals(12, PracticeGrid.answer(PracticeGrid.OP_ADD, 11, 1));
        assertEquals("↋ + 1", PracticeGrid.expression(PracticeGrid.OP_ADD, 11, 1));
        assertEquals("10", DozenMath.format(PracticeGrid.answer(PracticeGrid.OP_ADD, 11, 1)));
        assertEquals(-11, PracticeGrid.answer(PracticeGrid.OP_SUB, 0, 11));
        assertEquals(121, PracticeGrid.answer(PracticeGrid.OP_MULTIPLY, 11, 11));
        assertEquals(11, PracticeGrid.answer(PracticeGrid.OP_DIVIDE, 121, 11));
        assertFalse(PracticeGrid.valid(PracticeGrid.OP_DIVIDE, 1, 1, 0, 0));
        assertFalse(PracticeGrid.valid(PracticeGrid.OP_DIVIDE, 1, 1, 11, 3));
        assertTrue(PracticeGrid.valid(PracticeGrid.OP_DIVIDE, 1, 1, 10, 5));
        assertTrue(PracticeGrid.valid(PracticeGrid.OP_DIVIDE, 1, 1, 0, 7));
    }

    @Test public void distinctOrderedPairsAndRoundTrip() {
        LinkedHashSet<Long> choices = new LinkedHashSet<>();
        choices.add(PracticeGrid.key(3, 5));
        choices.add(PracticeGrid.key(5, 3));
        choices.add(PracticeGrid.key(11, 0));
        assertEquals(3, choices.size());
        assertEquals("3:5,5:3,↋:0", PracticeGrid.serialize(choices));
        assertEquals(choices, PracticeGrid.deserialize(PracticeGrid.serialize(choices),
            PracticeGrid.OP_ADD, 1, 1));
        assertEquals(0, PracticeGrid.deserialize("3:5,b:0", PracticeGrid.OP_ADD, 2, 2).size());
        assertEquals(0, PracticeGrid.deserialize("0:0,8:3", PracticeGrid.OP_DIVIDE, 1, 1).size());
        assertNotEquals(PracticeGrid.profileKey(PracticeGrid.OP_ADD,1,1),
            PracticeGrid.profileKey(PracticeGrid.OP_MULTIPLY,1,1));
        assertNotEquals(PracticeGrid.profileKey(PracticeGrid.OP_ADD,1,1),
            PracticeGrid.profileKey(PracticeGrid.OP_ADD,2,1));
    }

    @Test public void everySelectedPairOccursExactlyRequestedReps() {
        LinkedHashSet<Long> choices = new LinkedHashSet<>();
        choices.add(PracticeGrid.key(3, 5));
        choices.add(PracticeGrid.key(5, 3));
        choices.add(PracticeGrid.key(11, 11));
        int reps = 12; // 10 in base twelve
        List<Long> deck = PracticeGrid.makeSession(choices, reps, new Random(1));
        assertEquals(36, deck.size());
        Map<Long,Integer> counts = new HashMap<>();
        for (long key : deck) {
            counts.put(key, counts.containsKey(key) ? counts.get(key) + 1 : 1);
            DozenMath.Problem p = PracticeGrid.problem(PracticeGrid.OP_ADD, key);
            assertEquals(PracticeGrid.answer(PracticeGrid.OP_ADD,
                PracticeGrid.firstOperand(key), PracticeGrid.secondOperand(key)), p.answer);
            assertEquals(Long.valueOf(p.answer), DozenMath.parse(DozenMath.format(p.answer)));
        }
        for (long key : choices) assertEquals(Integer.valueOf(reps), counts.get(key));
    }

    @Test public void fourDigitMultiplicationDoesNotOverflow() {
        long x = PracticeGrid.answer(PracticeGrid.OP_MULTIPLY, 20735, 20735);
        assertEquals(429940225L, x);
        assertEquals(Long.valueOf(x), DozenMath.parse(DozenMath.format(x)));
    }
}
