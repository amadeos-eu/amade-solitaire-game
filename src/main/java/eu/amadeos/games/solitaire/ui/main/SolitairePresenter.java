package eu.amadeos.games.solitaire.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.gameengine.time.GameClock;
import eu.amadeos.gameengine.undo.History;
import eu.amadeos.games.solitaire.domain.Klondike;
import eu.amadeos.games.solitaire.domain.PileId;
import eu.amadeos.games.solitaire.repository.SolitaireSettings;

import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Runs the game, without Swing: dealing, drawing, moving cards, undo and redo, the clock, finishing a game
 * that plays itself, and the best times. All methods run on the event dispatch thread.
 */
public final class SolitairePresenter {
    private final SolitaireView view;
    private final Messages msg;
    private final SolitaireSettings settings;
    private final HighScores scores;
    private final RandomGenerator random;
    private final GameClock clock;
    private final Supplier<Instant> now;
    private final History<Klondike> history = new History<>();
    private boolean drawThree;
    private boolean finishing;
    /** The won game was celebrated and recorded. */
    private boolean over;
    private Klondike game;

    @Inject
    public SolitairePresenter(SolitaireView view, Messages msg, SolitaireSettings settings, HighScores scores,
                              RandomGenerator random, GameClock clock) {
        this(view, msg, settings, scores, random, clock, Instant::now);
    }

    SolitairePresenter(SolitaireView view, Messages msg, SolitaireSettings settings, HighScores scores,
                       RandomGenerator random, GameClock clock, Supplier<Instant> now) {
        this.view = view;
        this.msg = msg;
        this.settings = settings;
        this.scores = scores;
        this.random = random;
        this.clock = clock;
        this.now = now;
    }

    /** Deals the first game with the saved draw mode. */
    public void start() {
        drawThree = settings.drawThree();
        view.showOptions(drawThree);
        newGame();
    }

    public void newGame() {
        deal(Klondike.deal(drawThree, random));
    }

    public void setDrawThree(boolean on) {
        drawThree = on;
        settings.setDrawThree(on);
        view.showOptions(on);
        newGame();
    }

    /** Clicked on the stock. */
    public void draw() {
        play(() -> game.draw());
    }

    /** Dropped the cards from {@code index} of {@code from} on {@code to}; an illegal drop snaps back. */
    public void move(PileId from, int index, PileId to) {
        play(() -> game.move(from, index, to));
    }

    /** Double-clicked the top card of a pile: off to its foundation if one takes it. */
    public void moveToFoundation(PileId from) {
        play(() -> game.foundationFor(from)
                .map(to -> game.move(from, game.pile(from).size() - 1, to))
                .orElse(false));
    }

    // --8<-- [start:undo]
    public void undo() {
        if (game.won()) return;
        history.undo(game.copy()).ifPresent(previous -> game = previous);
        refresh();
    }

    public void redo() {
        if (game.won()) return;
        history.redo(game.copy()).ifPresent(next -> game = next);
        refresh();
    }

    /**
     * Plays a move: remembers the game before it for undo, starts the clock with the first move. If the
     * move is not allowed the table is shown again anyway, so a dropped card flies back to where it came from.
     */
    private void play(BooleanSupplier move) {
        if (game.won() || finishing) {
            refresh();
            return;
        }
        Klondike before = game.copy();
        if (move.getAsBoolean()) {
            history.record(before);
            clock.start();
            view.setTicking(true);
        }
        refresh();
    }
    // --8<-- [end:undo]

    /** Called by the view's timer while a game runs. */
    public void tick() {
        view.showTime(clock.seconds());
    }

    /** Called by the view's timer while the game finishes itself: one card to the foundations. */
    public void finishStep() {
        if (game.finishStep().isEmpty()) finishing = false;
        refresh();
    }

    public void showScores() {
        view.showScores(drawThree, -1);
    }

    public void showAbout() {
        view.showAbout();
    }

    private void deal(Klondike dealt) {
        game = dealt;
        history.clear();
        clock.reset();
        finishing = false;
        over = false;
        view.setTicking(false);
        view.setFinishing(false);
        refresh();
    }

    // --8<-- [start:refresh]
    /** Shows the table and the counters; starts finishing an open game and celebrates a won one. */
    private void refresh() {
        view.showGame(game);
        view.showMoves(game.moves());
        boolean justWon = game.won() && !over;
        if (justWon) {
            over = true;
            clock.stop();
            history.clear();
            finishing = false;
            view.setTicking(false);
            view.setFinishing(false);
        } else if (!finishing && game.canFinish()) {
            finishing = true;
            view.setFinishing(true);
        }
        view.showTime(clock.seconds());
        view.showUndo(history.canUndo(), history.canRedo());
        if (justWon) {
            view.celebrate();
            recordWin(clock.seconds());
        }
    }
    // --8<-- [end:refresh]

    private void recordWin(int seconds) {
        String table = tableId(drawThree);
        if (!scores.qualifies(table, seconds)) return;
        String mode = msg.get(drawThree ? "mode.draw-three" : "mode.draw-one");
        view.askName(msg.format("scores.won", mode, seconds)).ifPresent(name -> {
            int place = scores.add(table, new Score(name, seconds, now.get()));
            view.showScores(drawThree, place);
        });
    }

    /** The high-score table of a draw mode: {@code draw-one} or {@code draw-three}. */
    static String tableId(boolean drawThree) {
        return drawThree ? "draw-three" : "draw-one";
    }

    // ---- for tests -------------------------------------------------------------------------------

    Klondike game() {
        return game;
    }

    /** Puts a prepared game on the table, as if it had just been dealt. */
    void play(Klondike prepared) {
        deal(prepared);
    }
}
