package eu.amadeos.games.solitaire.domain;

/** The four suits; hearts and diamonds are red, clubs and spades black. */
public enum Suit {
    CLUBS('C', false),
    DIAMONDS('D', true),
    HEARTS('H', true),
    SPADES('S', false);

    private final char letter;
    private final boolean red;

    Suit(char letter, boolean red) {
        this.letter = letter;
        this.red = red;
    }

    public boolean red() {
        return red;
    }

    /** The letter in the short notation, e.g. {@code H} in {@code QH}. */
    public char letter() {
        return letter;
    }

    public static Suit ofLetter(char letter) {
        for (Suit s : values()) {
            if (s.letter == Character.toUpperCase(letter)) return s;
        }
        throw new IllegalArgumentException("No suit: " + letter);
    }
}
