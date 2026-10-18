package eu.amadeos.games.solitaire.ui.main;

import eu.amadeos.games.solitaire.domain.Klondike;

import java.util.Optional;

// --8<-- [start:view]
/** What {@link SolitairePresenter} needs from the window; tests replace it. */
public interface SolitaireView {
    /**
     * Shows the table after a change. The view moves every card from where it was shown to its new place,
     * so moves, undo and a card dropped where it may not go all animate the same way. The view only reads
     * the game.
     */
    void showGame(Klondike game);

    void showMoves(int moves);

    void showTime(int seconds);

    void showOptions(boolean drawThree);

    void showUndo(boolean canUndo, boolean canRedo);

    /** Starts or stops the timer that calls {@link SolitairePresenter#tick()}. */
    void setTicking(boolean ticking);

    /** Starts or stops the timer that calls {@link SolitairePresenter#finishStep()}. */
    void setFinishing(boolean finishing);

    /** The game is won: cards bounce across the table until the next click. */
    void celebrate();

    /** Asks for the player's name after a best time; empty if cancelled. */
    Optional<String> askName(String message);

    /** Shows the best times, the table of the draw mode first; {@code highlight} -1 for no new entry. */
    void showScores(boolean drawThree, int highlight);

    void showAbout();
}
// --8<-- [end:view]
