package eu.amadeos.games.solitaire.domain;

/** Ace to king; {@link #value()} is 1 to 13. */
public enum Rank {
    ACE("A"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"), EIGHT("8"), NINE("9"),
    TEN("10"), JACK("J"), QUEEN("Q"), KING("K");

    private final String label;

    Rank(String label) {
        this.label = label;
    }

    /** 1 for the ace, 13 for the king. */
    public int value() {
        return ordinal() + 1;
    }

    /** As printed in the corner of the card: A, 2 … 10, J, Q, K. */
    public String label() {
        return label;
    }

    /** The letter in the short notation: as the label, but T for ten. */
    public char letter() {
        return this == TEN ? 'T' : label.charAt(0);
    }

    public static Rank ofLetter(char letter) {
        for (Rank r : values()) {
            if (r.letter() == Character.toUpperCase(letter)) return r;
        }
        throw new IllegalArgumentException("No rank: " + letter);
    }
}
