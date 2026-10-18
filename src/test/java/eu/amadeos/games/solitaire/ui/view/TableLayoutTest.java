package eu.amadeos.games.solitaire.ui.view;

import eu.amadeos.games.solitaire.domain.Klondike;
import eu.amadeos.games.solitaire.domain.PileId;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableLayoutTest {
    private final TableLayout layout = new TableLayout(70);   // 70 × 98, gap 10

    @Test
    void pilesLieInTwoRows() {
        assertEquals(new Point2D.Double(10, 10), layout.slot(PileId.STOCK));
        assertEquals(new Point2D.Double(90, 10), layout.slot(PileId.WASTE));
        assertEquals(new Point2D.Double(250, 10), layout.slot(PileId.foundation(0)));
        assertEquals(new Point2D.Double(10, 118), layout.slot(PileId.tableau(0)));
        assertEquals(new Point2D.Double(490, 118), layout.slot(PileId.tableau(6)));
        assertEquals(570, layout.size().width);
    }

    @Test
    void columnsFanFaceDownCardsCloserThanFaceUpOnes() {
        Klondike game = Klondike.of(false, "t0: [KD 9C] 8H 7S");
        assertEquals(118, layout.cardAt(game, PileId.tableau(0), 0).getY());
        assertEquals(128, layout.cardAt(game, PileId.tableau(0), 1).getY(), "a face-down step");
        assertEquals(138, layout.cardAt(game, PileId.tableau(0), 2).getY(), "a face-down step");
        assertEquals(163, layout.cardAt(game, PileId.tableau(0), 3).getY(), "a face-up step");
    }

    @Test
    void drawThreeFansTheTopOfTheWaste() {
        Klondike game = Klondike.of(true, "waste: 2C 3C 4C 5C");
        assertEquals(90, layout.cardAt(game, PileId.WASTE, 0).getX());
        assertEquals(90, layout.cardAt(game, PileId.WASTE, 1).getX());
        assertTrue(layout.cardAt(game, PileId.WASTE, 3).getX() > layout.cardAt(game, PileId.WASTE, 2).getX());
    }

    @Test
    void theTopmostCardIsHit() {
        Klondike game = Klondike.of(false, "t0: [KD 9C] 8H 7S");
        assertEquals(Optional.of(new TableLayout.Hit(PileId.tableau(0), 3)), layout.hit(game, new Point2D.Double(20, 200)));
        assertEquals(Optional.of(new TableLayout.Hit(PileId.tableau(0), 2)), layout.hit(game, new Point2D.Double(20, 150)));
        assertEquals(Optional.of(new TableLayout.Hit(PileId.tableau(1), -1)), layout.hit(game, new Point2D.Double(100, 130)),
                "an empty column");
        assertEquals(Optional.empty(), layout.hit(game, new Point2D.Double(200, 50)), "between the piles");
    }

    @Test
    void cardsLandWhereTheyOverlapMost() {
        Klondike game = Klondike.of(false, "t0: 8H", "t1: 7S");
        Rectangle2D dropped = new Rectangle2D.Double(30, 140, 70, 98);   // mostly over column 0
        assertEquals(Optional.of(PileId.tableau(0)), layout.dropTarget(game, dropped, PileId.tableau(1)));
        assertEquals(Optional.empty(), layout.dropTarget(game, new Rectangle2D.Double(200, 10, 20, 20), PileId.tableau(1)));
    }
}
