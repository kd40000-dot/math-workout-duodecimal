package com.duodecimal.workout;

import org.junit.Test;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

/** All modular identities exhaustively tested across the entire 12x12 table. */
public class VisualAdditionTest {
    @Test public void everyPairReturnsCorrectComponentAndCarry() {
        for (int a=0;a<12;a++) for (int b=0;b<12;b++) {
            VisualAddition.Fact f = new VisualAddition.Fact(a,b);
            assertEquals(a+b,f.sum);
            assertEquals((a%3+b%3)%3,f.cap);
            assertEquals((a%4+b%4)%4,f.body);
            assertEquals((a+b)/12,f.carry);
            assertEquals((a+b)%12,f.digit);
            assertEquals(f.digit,VisualAddition.digitFor(f.cap,f.body));
            assertEquals(f.cap,f.expected(VisualAddition.PART_CAP));
            assertEquals(f.body,f.expected(VisualAddition.PART_BODY));
            assertEquals(f.carry,f.expected(VisualAddition.PART_CARRY));
            assertEquals(Long.valueOf(a+b),DozenMath.parse(DozenMath.format(f.sum)));
            assertEquals(DozenMath.format(a)+" + "+DozenMath.format(b),f.expression());
        }
    }

    @Test public void everyCapCombinationAppearsExactlyOncePerDeck() {
        List<VisualAddition.Fact> deck=VisualAddition.deck(VisualAddition.CAP,new Random(42));
        assertEquals(9,deck.size());
        Set<Integer> used=new HashSet<>();
        for(VisualAddition.Fact f:deck) assertTrue(used.add((f.a%3)*3+f.b%3));
        assertEquals(9,used.size());
    }

    @Test public void everyBodyCombinationAppearsExactlyOncePerDeck() {
        List<VisualAddition.Fact> deck=VisualAddition.deck(VisualAddition.BODY,new Random(42));
        assertEquals(16,deck.size());
        Set<Integer> used=new HashSet<>();
        for(VisualAddition.Fact f:deck) assertTrue(used.add((f.a%4)*4+f.b%4));
        assertEquals(16,used.size());
    }

    @Test public void noCarryDeckContainsExactlySeventyEightOrderedPairs() {
        List<VisualAddition.Fact> deck=VisualAddition.deck(VisualAddition.COMBINE,new Random(42));
        assertEquals(78,deck.size());
        Set<String> used=new HashSet<>();
        for(VisualAddition.Fact f:deck) {
            assertTrue(f.sum<12);
            assertEquals(0,f.carry);
            assertTrue(used.add(f.a+":"+f.b));
        }
    }

    @Test public void carryAndFullDecksCoverTheEntireAdditionTable() {
        for(int mode : new int[]{VisualAddition.CARRY,VisualAddition.FULL}){
            List<VisualAddition.Fact> deck=VisualAddition.deck(mode,new Random(1));
            assertEquals(144,deck.size());
            Set<String> used=new HashSet<>();
            int yes=0,no=0;
            for(VisualAddition.Fact f:deck) {
                if(f.carry==1) yes++;else no++;
                assertTrue(used.add(f.a+":"+f.b));
            }
            assertEquals(66,yes);
            assertEquals(78,no);
        }
    }

    @Test public void eachModeHasCorrectStageSequence() {
        assertArrayEquals(new int[]{VisualAddition.PART_CAP},VisualAddition.stages(VisualAddition.CAP));
        assertArrayEquals(new int[]{VisualAddition.PART_BODY},VisualAddition.stages(VisualAddition.BODY));
        assertArrayEquals(new int[]{VisualAddition.PART_CAP,VisualAddition.PART_BODY},
            VisualAddition.stages(VisualAddition.COMBINE));
        assertArrayEquals(new int[]{VisualAddition.PART_CARRY},VisualAddition.stages(VisualAddition.CARRY));
        assertArrayEquals(new int[]{VisualAddition.PART_CAP,VisualAddition.PART_BODY,VisualAddition.PART_CARRY},
            VisualAddition.stages(VisualAddition.FULL));
        for (int mode=0;mode<VisualAddition.MODE_COUNT;mode++){
            assertEquals(24,VisualAddition.session(mode,24,new Random(4)).size());
            assertEquals(48,VisualAddition.session(mode,48,new Random(4)).size());
        }
    }

    @Test public void specialDigitRelationsAndComplements() {
        VisualAddition.Fact f=new VisualAddition.Fact(5,7);
        assertEquals(0,f.digit);
        assertEquals(1,f.carry);
        assertEquals(0,f.cap);
        assertEquals(0,f.body);
        assertEquals("10",DozenMath.format(f.sum));
        for(int a=1;a<=11;a++){
            VisualAddition.Fact pair=new VisualAddition.Fact(a,12-a);
            assertEquals(0,pair.cap);
            assertEquals(0,pair.body);
            assertEquals(1,pair.carry);
            assertEquals(0,pair.digit);
        }
        VisualAddition.Fact dots=new VisualAddition.Fact(5,5);
        assertEquals(1,dots.cap); // dot + dot => breve, NOT dot
        assertEquals(2,dots.body);
        assertEquals("↊",DozenMath.format(dots.sum));
    }
}
