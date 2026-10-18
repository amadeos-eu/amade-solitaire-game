package eu.amadeos.games.solitaire.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The cards of one pile, bottom first. The lowest {@link #faceDown()} cards lie face down, the rest face
 * up. In the stock all cards lie face down, in waste and foundations all face up, in a tableau column the
 * face-down cards are covered by a run of face-up ones.
 */
public final class Pile {
    private final List<Card> cards;
    private int faceDown;

    Pile() {
        this(new ArrayList<>(), 0);
    }

    private Pile(List<Card> cards, int faceDown) {
        this.cards = cards;
        this.faceDown = faceDown;
    }

    Pile copy() {
        return new Pile(new ArrayList<>(cards), faceDown);
    }

    /** The cards, bottom first; read-only. */
    public List<Card> cards() {
        return Collections.unmodifiableList(cards);
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /** The top card, or {@code null} for an empty pile. */
    public Card top() {
        return cards.isEmpty() ? null : cards.getLast();
    }

    /** How many cards at the bottom lie face down. */
    public int faceDown() {
        return faceDown;
    }

    public boolean faceUp(int index) {
        return index >= faceDown;
    }

    void add(Card card, boolean up) {
        if (!up && faceDown < cards.size()) throw new IllegalStateException("Face-down card on face-up ones");
        cards.add(card);
        if (!up) faceDown++;
    }

    /** Takes the cards from {@code index} to the top. */
    List<Card> takeFrom(int index) {
        List<Card> taken = new ArrayList<>(cards.subList(index, cards.size()));
        cards.subList(index, cards.size()).clear();
        faceDown = Math.min(faceDown, cards.size());
        return taken;
    }

    /** Turns the top card face up if it lies face down; true if it did. */
    boolean turnTop() {
        if (!cards.isEmpty() && faceDown == cards.size()) {
            faceDown--;
            return true;
        }
        return false;
    }

    void setAllFaceDown() {
        faceDown = cards.size();
    }

    void setAllFaceUp() {
        faceDown = 0;
    }
}
