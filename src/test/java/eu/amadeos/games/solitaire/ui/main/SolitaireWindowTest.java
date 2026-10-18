package eu.amadeos.games.solitaire.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.time.GameClock;
import eu.amadeos.games.solitaire.bootstrap.SolitaireModule;
import eu.amadeos.games.solitaire.domain.Klondike;
import eu.amadeos.games.solitaire.domain.PileId;
import eu.amadeos.games.solitaire.repository.SolitaireSettings;
import eu.amadeos.games.solitaire.ui.view.TableView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** The real window with the real presenter: the mouse reaches the presenter and its answers are shown. */
class SolitaireWindowTest {
    @TempDir
    Path dir;

    private SolitaireWindow window;
    private SolitairePresenter presenter;

    @BeforeEach
    void open() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        HighScores scores = new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS);
        Messages msg = Messages.load(SolitaireModule.class, SolitaireModule.MESSAGES, Locale.ENGLISH);
        onEdt(() -> {
            window = new SolitaireWindow(msg, scores, new PlayerName(dir, "tester"));
            presenter = new SolitairePresenter(window, msg, new SolitaireSettings(dir.resolve("s.properties")),
                    scores, new SplittableRandom(3), new GameClock());
            window.bind(presenter);
            presenter.start();
        });
    }

    @AfterEach
    void close() throws Exception {
        if (window != null) onEdt(window::dispose);
    }

    private static void onEdt(Runnable code) throws Exception {
        try {
            SwingUtilities.invokeAndWait(code);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error error) throw error;
            throw (Exception) e.getCause();
        }
    }

    private void mouse(int id, Point2D at, int clicks) throws Exception {
        onEdt(() -> {
            TableView table = window.table();
            int mask = id == MouseEvent.MOUSE_RELEASED ? 0 : InputEvent.BUTTON1_DOWN_MASK;
            table.dispatchEvent(new MouseEvent(table, id, System.currentTimeMillis(), mask,
                    (int) at.getX(), (int) at.getY(), clicks, false, MouseEvent.BUTTON1));
        });
    }

    private Point2D middleOf(PileId pile, int index) throws Exception {
        Point2D[] p = new Point2D[1];
        onEdt(() -> {
            var bounds = window.table().tableLayout().bounds(presenter.game(), pile, index);
            p[0] = new Point2D.Double(bounds.getCenterX(), bounds.getY() + 8);
        });
        return p[0];
    }

    @Test
    void clickingTheStockDraws() throws Exception {
        mouse(MouseEvent.MOUSE_PRESSED, middleOf(PileId.STOCK, 0), 1);
        mouse(MouseEvent.MOUSE_RELEASED, middleOf(PileId.STOCK, 0), 1);
        onEdt(() -> {
            assertEquals(1, presenter.game().pile(PileId.WASTE).size());
            assertEquals("Moves: 1", window.movesLabel().getText());
            assertTrue(window.undoItem().isEnabled());
            assertTrue(window.table().animating(), "the card flies to the waste");
        });
    }

    @Test
    void draggingACardMovesIt() throws Exception {
        onEdt(() -> presenter.play(Klondike.of(false, "stock: 2C", "t0: 8H", "t1: 7S")));
        Point2D from = middleOf(PileId.tableau(1), 0);
        Point2D to = middleOf(PileId.tableau(0), 0);
        mouse(MouseEvent.MOUSE_PRESSED, from, 1);
        mouse(MouseEvent.MOUSE_DRAGGED, new Point2D.Double((from.getX() + to.getX()) / 2, to.getY() + 20), 1);
        mouse(MouseEvent.MOUSE_DRAGGED, new Point2D.Double(to.getX(), to.getY() + 20), 1);
        mouse(MouseEvent.MOUSE_RELEASED, new Point2D.Double(to.getX(), to.getY() + 20), 1);
        onEdt(() -> {
            assertEquals(2, presenter.game().pile(PileId.tableau(0)).size());
            assertTrue(presenter.game().pile(PileId.tableau(1)).isEmpty());
        });
    }

    @Test
    void aDoubleClickSendsTheCardHome() throws Exception {
        onEdt(() -> presenter.play(Klondike.of(false, "t3: [9C] AD")));
        Point2D ace = middleOf(PileId.tableau(3), 1);
        mouse(MouseEvent.MOUSE_PRESSED, ace, 1);
        mouse(MouseEvent.MOUSE_RELEASED, ace, 1);
        mouse(MouseEvent.MOUSE_PRESSED, ace, 2);
        mouse(MouseEvent.MOUSE_RELEASED, ace, 2);
        onEdt(() -> {
            assertEquals(1, presenter.game().pile(PileId.foundation(0)).size());
            assertEquals(0, presenter.game().pile(PileId.tableau(3)).faceDown(), "the 9 is turned over");
        });
    }

    @Test
    void finishingRunsOnATimerAndEndsWithTheCelebration() throws Exception {
        onEdt(() -> {
            for (int i = 0; i < HighScores.SIZE; i++) {
                new HighScores(dir.resolve("scores"), Ranking.LOWER_WINS).add("draw-one",
                        new eu.amadeos.gameengine.score.Score("Pro", 0, java.time.Instant.EPOCH));
            }
            presenter.play(Klondike.of(false, "f0: AC 2C 3C 4C 5C 6C 7C 8C 9C TC JC QC",
                    "f1: AD 2D 3D 4D 5D 6D 7D 8D 9D TD JD QD", "f2: AH 2H 3H 4H 5H 6H 7H 8H 9H TH JH QH",
                    "f3: AS 2S 3S 4S 5S 6S 7S 8S 9S TS JS QS", "t0: KC", "t1: KD", "t2: KH", "waste: KS"));
            presenter.move(PileId.WASTE, 0, PileId.foundation(3));
            assertTrue(window.finishing());
        });
        long deadline = System.currentTimeMillis() + 5000;
        boolean[] won = {false};
        while (!won[0] && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            onEdt(() -> won[0] = presenter.game().won());
        }
        onEdt(() -> {
            assertTrue(presenter.game().won());
            assertFalse(window.finishing());
            assertTrue(window.table().celebrating());
        });
    }
}
