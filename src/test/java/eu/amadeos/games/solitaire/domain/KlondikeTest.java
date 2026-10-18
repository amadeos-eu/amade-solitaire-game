package eu.amadeos.games.solitaire.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KlondikeTest {
    private static List<Card> cards(String codes) {
        return codes.isBlank() ? List.of() : java.util.Arrays.stream(codes.split(" ")).map(Card::of).toList();
    }

    // --8<-- [start:test-deal]
    @Test
    void theDealUsesEveryCardOnce() {
        Klondike game = Klondike.deal(false, new SplittableRandom(1));
        Set<Card> seen = new HashSet<>();
        for (Pile pile : game.piles().values()) seen.addAll(pile.cards());
        assertEquals(52, seen.size());
        for (int column = 0; column < PileId.COLUMNS; column++) {
            Pile pile = game.pile(PileId.tableau(column));
            assertEquals(column + 1, pile.size());
            assertEquals(column, pile.faceDown(), "only the top card lies face up");
        }
        assertEquals(24, game.pile(PileId.STOCK).size());
    }
    // --8<-- [end:test-deal]

    @Test
    void theSameSeedDealsTheSameGame() {
        assertEquals(Klondike.deal(false, new SplittableRandom(5)).toString(),
                Klondike.deal(false, new SplittableRandom(5)).toString());
        assertNotEquals(Klondike.deal(false, new SplittableRandom(5)).toString(),
                Klondike.deal(false, new SplittableRandom(6)).toString());
    }

    @Test
    void theTextFormReadsBack() {
        Klondike game = Klondike.of(false, "stock: 2C 3C", "waste: 4D", "f0: AS", "t0: [KD 9C] 8H 7S");
        assertEquals(cards("KD 9C 8H 7S"), game.pile(PileId.tableau(0)).cards());
        assertEquals(2, game.pile(PileId.tableau(0)).faceDown());
        assertEquals(2, game.pile(PileId.STOCK).faceDown());
        assertEquals(game.toString(), Klondike.of(false, game.toString().split("\n")).toString());
    }

    // --8<-- [start:test-columns]
    @Test
    void columnsBuildDownInAlternatingColours() {
        Klondike game = Klondike.of(false, "t0: 8H", "t1: 7S", "t2: 7D", "t3: 6S", "waste: KC");
        assertTrue(game.canMove(PileId.tableau(1), 0, PileId.tableau(0)), "black 7 on red 8");
        assertFalse(game.canMove(PileId.tableau(2), 0, PileId.tableau(0)), "red 7 on red 8");
        assertFalse(game.canMove(PileId.tableau(3), 0, PileId.tableau(0)), "6 on 8");
        assertTrue(game.canMove(PileId.WASTE, 0, PileId.tableau(4)), "a king on an empty column");
        assertFalse(game.canMove(PileId.tableau(1), 0, PileId.tableau(4)), "only kings on empty columns");
    }
    // --8<-- [end:test-columns]

    @Test
    void foundationsBuildUpBySuitFromTheAce() {
        Klondike game = Klondike.of(false, "t0: AH", "t1: 2H", "t2: 2S", "f1: AS", "t3: [5C] 3S 2D");
        assertTrue(game.canMove(PileId.tableau(0), 0, PileId.foundation(0)));
        assertFalse(game.canMove(PileId.tableau(1), 0, PileId.foundation(0)), "a 2 on an empty foundation");
        assertTrue(game.canMove(PileId.tableau(2), 0, PileId.foundation(1)));
        assertFalse(game.canMove(PileId.tableau(1), 0, PileId.foundation(1)), "2H on AS");
        assertFalse(game.canMove(PileId.tableau(3), 1, PileId.foundation(2)), "one card at a time");
    }

    @Test
    void aRunMovesTogetherAndUncoversACard() {
        Klondike game = Klondike.of(false, "t0: [KD 9C] 8H 7S", "t1: 9S");
        assertTrue(game.move(PileId.tableau(0), 2, PileId.tableau(1)));
        assertEquals(cards("9S 8H 7S"), game.pile(PileId.tableau(1)).cards());
        assertEquals(cards("KD 9C"), game.pile(PileId.tableau(0)).cards());
        assertEquals(1, game.pile(PileId.tableau(0)).faceDown(), "the 9 of clubs was turned over");
        assertEquals(1, game.moves());
    }

    @Test
    void faceDownCardsCannotBePicked() {
        Klondike game = Klondike.of(false, "t0: [KD 9C] 8H", "t1: TH", "waste: 2C 3C", "f0: AD");
        assertFalse(game.canPick(PileId.tableau(0), 1));
        assertFalse(game.move(PileId.tableau(0), 1, PileId.tableau(1)));
        assertFalse(game.canPick(PileId.WASTE, 0), "only the top of the waste");
        assertTrue(game.canPick(PileId.foundation(0), 0), "foundation cards may come back");
        assertFalse(game.canPick(PileId.STOCK, 0));
    }

    // --8<-- [start:test-draw]
    @Test
    void drawingThreeTurnsThemOverOneByOne() {
        Klondike game = Klondike.of(true, "stock: 2C 3C 4C 5C");
        game.draw();
        assertEquals(cards("5C 4C 3C"), game.pile(PileId.WASTE).cards(), "the third card ends on top");
        game.draw();
        assertEquals(cards("5C 4C 3C 2C"), game.pile(PileId.WASTE).cards());
        assertTrue(game.pile(PileId.STOCK).isEmpty());
        game.draw();
        assertEquals(cards("2C 3C 4C 5C"), game.pile(PileId.STOCK).cards(), "the waste turned over again");
        assertTrue(game.pile(PileId.WASTE).isEmpty());
        assertEquals(3, game.moves());
        assertFalse(Klondike.of(true).draw(), "nothing to draw");
    }
    // --8<-- [end:test-draw]

    @Test
    void aCopyIsIndependent() {
        Klondike game = Klondike.of(false, "t0: [KD 9C] 8H 7S", "t1: 9S");
        Klondike copy = game.copy();
        game.move(PileId.tableau(0), 2, PileId.tableau(1));
        assertEquals(4, copy.pile(PileId.tableau(0)).size());
        assertEquals(2, copy.pile(PileId.tableau(0)).faceDown());
        assertEquals(0, copy.moves());
    }

    @Test
    void aDoubleClickFindsTheFoundation() {
        Klondike game = Klondike.of(false, "f2: AH 2H", "t0: 3H", "t1: 3S");
        assertEquals(Optional.of(PileId.foundation(2)), game.foundationFor(PileId.tableau(0)));
        assertEquals(Optional.of(PileId.foundation(0)), Klondike.of(false, "t0: AC").foundationFor(PileId.tableau(0)),
                "the first free foundation takes an ace");
        assertEquals(Optional.empty(), game.foundationFor(PileId.tableau(1)));
    }

    @Test
    void anOpenGameFinishesItself() {
        Klondike game = Klondike.of(false,
                "f0: AC 2C 3C 4C 5C 6C 7C 8C 9C TC JC QC",
                "f1: AD 2D 3D 4D 5D 6D 7D 8D 9D TD JD QD",
                "f2: AH 2H 3H 4H 5H 6H 7H 8H 9H TH JH",
                "f3: AS 2S 3S 4S 5S 6S 7S 8S 9S TS JS QS",
                "t0: KC QH", "t1: KD", "t2: KH", "t3: KS");
        assertTrue(game.canFinish());
        assertEquals(Optional.of(PileId.tableau(0)), game.finishStep(), "the queen first, the lowest card");
        int steps = 1;
        while (game.finishStep().isPresent()) steps++;
        assertEquals(5, steps);
        assertTrue(game.won());
        assertFalse(game.canFinish());
    }

    @Test
    void aHiddenCardStopsFinishing() {
        assertFalse(Klondike.of(false, "t0: [AC] KD").canFinish());
        assertFalse(Klondike.of(false, "stock: AC").canFinish());
    }
}
