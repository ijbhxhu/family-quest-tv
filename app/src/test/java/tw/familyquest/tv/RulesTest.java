package tw.familyquest.tv;

import org.junit.Test;
import static org.junit.Assert.*;

public class RulesTest {
    @Test public void schoolSpellingsAcceptCaseWhitespaceAndApostrophes() {
        assertTrue(Rules.correct("MRT", "mrt"));
        assertTrue(Rules.correct("ice cream", "  Ice   cream  "));
        assertTrue(Rules.correct("o’clock", "o'clock"));
        assertTrue(Rules.correct("A.M.", "am"));
        assertTrue(Rules.correct("T-shirt", "Ｔ-shirt"));
        assertFalse(Rules.correct("ice cream", "icecream"));
        assertFalse(Rules.correct("Tuesday", "Thuesday"));
        assertFalse(Rules.correct("good", ""));
    }
    @Test public void timeCannotBeExtendedByWallClockRollback() {
        assertEquals(30_000, Rules.remaining(100_000, 200_000, 4, 70_000, 1_000, 4));
        assertEquals(10_000, Rules.remaining(100_000, 200_000, 4, 70_000, 190_000, 4));
        assertEquals(0, Rules.remaining(100_000, 200_000, 4, 110_000, 180_000, 4));
    }
    @Test public void rebootOrClockForwardExpiresCurrentSession() {
        assertEquals(0, Rules.remaining(100_000, 200_000, 4, 100, 190_000, 5));
        assertEquals(0, Rules.remaining(100_000, 200_000, 4, 70_000, 220_000, 4));
    }
    @Test public void rewardsCapAtTwoHours() {
        assertEquals(600_000, Rules.addReward(0, 10));
        assertEquals(Rules.MAX_BANK, Rules.addReward(Rules.MAX_BANK - 60_000, 10));
    }
    @Test(expected=IllegalArgumentException.class) public void negativeRewardRejected() { Rules.addReward(0, -10); }
    @Test(expected=IllegalArgumentException.class) public void zeroQuestionRulesRejected() { Rules.validate(0, 10, "beginner"); }
    @Test public void dailyOpeningBoundaryAndMidnight() {
        assertFalse(Rules.isOpen("16:00",15*60+59)); assertTrue(Rules.isOpen("16:00",16*60));
        assertTrue(Rules.isOpen("16:00",23*60+59)); assertFalse(Rules.isOpen("16:00",0));
        assertTrue(Rules.isOpen("00:00",0)); assertFalse(Rules.isOpen("16:00",1440));
    }
    @Test(expected=IllegalArgumentException.class) public void impossibleOpeningTimeRejected() { Rules.openingMinutes("25:60"); }
}
