package eu.amadeos.games.solitaire.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.gameengine.time.GameClock;
import eu.amadeos.games.solitaire.bootstrap.SolitaireModule;
import eu.amadeos.games.solitaire.domain.Card;
import eu.amadeos.games.solitaire.domain.Klondike;
import eu.amadeos.games.solitaire.domain.PileId;
import eu.amadeos.games.solitaire.repository.SolitaireSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The game without a window: the presenter talks to a {@link FakeView}. */
class SolitairePresenterTest {
    private static final Instant WON_AT = Instant.parse("2026-10-18T20:00:00Z");
    /** Everything on the foundations but the four kings, which lie in the columns. */
    private static final String[] ALMOST_WON = {
            "f0: AC 2C 3C 4C 5C 6C 7C 8C 9C TC JC QC",
            "f1: AD 2D 3D 4D 5D 6D 7D 8D 9D TD JD QD",
            "f2: AH 2H 3H 4H 5H 6H 7H 8H 9H TH JH QH",
            "f3: AS 2S 3S 4S 5S 6S 7S 8S 9S TS JS QS",
            "t0: KC", "t1: KD", "t2: KH", "waste: KS"};

    @TempDir
    Path dir;

    private final long[] now = {1_000_000_000L};
    private final FakeView view = new FakeView();
    private SolitaireSettings settings;
    private HighScores scores;
    private SolitairePresenter presenter;

    @BeforeEach
    void start() {
        settings = new SolitaireSettings(dir.resolve("solitaire.properties"));
        scores = new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS);
        Messages msg = Messages.load(SolitaireModule.class, SolitaireModule.MESSAGES, Locale.ENGLISH);
        presenter = new SolitairePresenter(view, msg, settings, scores, new SplittableRandom(3),
                new GameClock(() -> now[0]), () -> WON_AT);
        presenter.start();
    }

    @Test
    void dealsAGameAtTheStart() {
        assertEquals(24, view.game.pile(PileId.STOCK).size());
        assertFalse(view.drawThree);
        assertFalse(view.canUndo);
        assertFalse(view.ticking);
        assertEquals(0, view.moves);
    }

    @Test
    void drawingIsAMoveAndStartsTheClock() {
        presenter.draw();
        assertEquals(1, view.game.pile(PileId.WASTE).size());
        assertEquals(1, view.moves);
        assertTrue(view.ticking);
        assertTrue(view.canUndo);
    }

    // --8<-- [start:test-undo]
    @Test
    void undoAndRedoRestoreTheTable() {
        presenter.play(Klondike.of(false, "t0: [KD 9C] 8H 7S", "t1: 9S"));
        presenter.move(PileId.tableau(0), 2, PileId.tableau(1));
        assertEquals(3, view.game.pile(PileId.tableau(1)).size());
        assertEquals(1, view.game.pile(PileId.tableau(0)).faceDown(), "the 9 of clubs was turned over");

        presenter.undo();
        assertEquals(1, view.game.pile(PileId.tableau(1)).size());
        assertEquals(2, view.game.pile(PileId.tableau(0)).faceDown(), "and is face down again");
        assertFalse(view.canUndo);
        assertTrue(view.canRedo);

        presenter.redo();
        assertEquals(3, view.game.pile(PileId.tableau(1)).size());
        assertEquals(1, view.moves);
    }
    // --8<-- [end:test-undo]

    @Test
    void anIllegalDropShowsTheTableAgainUnchanged() {
        presenter.play(Klondike.of(false, "stock: 2C", "t0: 8H", "t1: 7D"));
        int before = view.shows;
        presenter.move(PileId.tableau(1), 0, PileId.tableau(0));
        assertEquals(before + 1, view.shows, "shown again, so the card flies back");
        assertEquals(List.of(Card.of("7D")), view.game.pile(PileId.tableau(1)).cards());
        assertFalse(view.canUndo);
        assertFalse(view.ticking, "an illegal move does not start the clock");
    }

    @Test
    void aDoubleClickSendsACardToItsFoundation() {
        presenter.play(Klondike.of(false, "t0: [5C] AH", "t1: 2C"));
        presenter.moveToFoundation(PileId.tableau(0));
        assertEquals(List.of(Card.of("AH")), view.game.pile(PileId.foundation(0)).cards());
        presenter.moveToFoundation(PileId.tableau(1));
        assertEquals(1, view.moves, "the 2 of clubs fits nowhere");
    }

    // --8<-- [start:test-finish]
    @Test
    void anOpenGameFinishesItselfAndRecordsTheTime() {
        view.names.push(Optional.of("Anna"));
        presenter.play(Klondike.of(false, ALMOST_WON));
        presenter.move(PileId.WASTE, 0, PileId.foundation(3));     // the last move by hand
        assertTrue(view.finishing, "now the rest plays itself");
        now[0] += 41_500_000_000L;
        while (view.finishing) presenter.finishStep();              // what the view's timer does
        assertTrue(view.game.won());
        assertEquals(1, view.celebrations);
        assertFalse(view.ticking);
        assertEquals("You won with \"Draw one\" in 42 seconds.", view.askedWith);
        assertEquals(List.of(new Score("Anna", 42, WON_AT)), scores.list("draw-one"));
        assertEquals(Boolean.FALSE, view.scoresShownForDrawThree);
        assertEquals(0, view.highlight);
    }
    // --8<-- [end:test-finish]

    @Test
    void nothingMovesAfterTheWin() {
        presenter.play(Klondike.of(false, ALMOST_WON));
        presenter.move(PileId.WASTE, 0, PileId.foundation(3));
        while (view.finishing) presenter.finishStep();
        presenter.undo();
        assertTrue(view.game.won());
        assertFalse(view.canUndo);
        assertEquals(1, view.celebrations);
    }

    @Test
    void cancellingTheNameKeepsTheTableEmpty() {
        presenter.play(Klondike.of(false, ALMOST_WON));
        presenter.move(PileId.WASTE, 0, PileId.foundation(3));
        while (view.finishing) presenter.finishStep();
        assertEquals(List.of(), scores.list("draw-one"));
        assertNull(view.scoresShownForDrawThree);
    }

    @Test
    void drawThreeIsRememberedAndDealsAgain() {
        presenter.draw();
        presenter.setDrawThree(true);
        assertTrue(view.drawThree);
        assertTrue(settings.drawThree());
        assertEquals(0, view.moves);
        presenter.draw();
        assertEquals(3, view.game.pile(PileId.WASTE).size());
        presenter.showScores();
        assertEquals(Boolean.TRUE, view.scoresShownForDrawThree);
    }

    @Test
    void aNewGameForgetsTheHistory() {
        presenter.draw();
        presenter.newGame();
        assertFalse(view.canUndo);
        assertFalse(view.ticking);
        assertEquals(0, view.seconds);
    }
}
