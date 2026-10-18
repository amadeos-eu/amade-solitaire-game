package eu.amadeos.games.solitaire.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A playing card. Whether it lies face up is not a property of the card but of where it lies (see
 * {@link Pile}), so a card is an immutable value and safe to share between copies of a game.
 */
public record Card(Rank rank, Suit suit) {
    public Card {
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(suit, "suit");
    }

    public boolean red() {
        return suit.red();
    }

    /** The short notation: rank letter and suit letter, e.g. {@code AS}, {@code TD}, {@code QH}. */
    public String code() {
        return "" + rank.letter() + suit.letter();
    }

    /** Reads the short notation, e.g. {@code Card.of("QH")}. */
    public static Card of(String code) {
        if (code.length() != 2) throw new IllegalArgumentException("Not a card: " + code);
        return new Card(Rank.ofLetter(code.charAt(0)), Suit.ofLetter(code.charAt(1)));
    }

    /** All 52 cards, ordered by suit and rank. */
    public static List<Card> deck() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit s : Suit.values()) {
            for (Rank r : Rank.values()) deck.add(new Card(r, s));
        }
        return deck;
    }

    @Override
    public String toString() {
        return code();
    }
}
