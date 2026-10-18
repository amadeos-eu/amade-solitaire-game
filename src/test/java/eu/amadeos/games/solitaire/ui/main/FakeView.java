package eu.amadeos.games.solitaire.ui.main;

import eu.amadeos.games.solitaire.domain.Klondike;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Records what the presenter shows and answers its questions from a queue. */
final class FakeView implements SolitaireView {
    Klondike game;
    int shows;
    int moves;
    int seconds;
    boolean drawThree;
    boolean canUndo;
    boolean canRedo;
    boolean ticking;
    boolean finishing;
    int celebrations;
    final Deque<Optional<String>> names = new ArrayDeque<>();
    String askedWith;
    Boolean scoresShownForDrawThree;
    int highlight = -2;
    boolean about;

    @Override
    public void showGame(Klondike newGame) {
        game = newGame;
        shows++;
    }

    @Override
    public void showMoves(int newMoves) {
        moves = newMoves;
    }

    @Override
    public void showTime(int newSeconds) {
        seconds = newSeconds;
    }

    @Override
    public void showOptions(boolean newDrawThree) {
        drawThree = newDrawThree;
    }

    @Override
    public void showUndo(boolean undo, boolean redo) {
        canUndo = undo;
        canRedo = redo;
    }

    @Override
    public void setTicking(boolean on) {
        ticking = on;
    }

    @Override
    public void setFinishing(boolean on) {
        finishing = on;
    }

    @Override
    public void celebrate() {
        celebrations++;
    }

    @Override
    public Optional<String> askName(String message) {
        askedWith = message;
        return names.isEmpty() ? Optional.empty() : names.pop();
    }

    @Override
    public void showScores(boolean forDrawThree, int place) {
        scoresShownForDrawThree = forDrawThree;
        highlight = place;
    }

    @Override
    public void showAbout() {
        about = true;
    }
}
