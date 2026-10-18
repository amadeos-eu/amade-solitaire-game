package eu.amadeos.games.solitaire.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Klondike, the classic patience, without any user interface. Twenty-eight cards are dealt into seven
 * columns, the rest form the stock. Cards are built down on the columns in alternating colours and up on
 * the foundations by suit, from ace to king. The game is won when all 52 cards are on the foundations.
 *
 * <p>A game is mutable; {@link #copy()} makes an independent copy for undo.
 */
public final class Klondike {
    private final Map<PileId, Pile> piles = new LinkedHashMap<>();
    private final boolean drawThree;
    private int moves;

    private Klondike(boolean drawThree) {
        this.drawThree = drawThree;
        for (PileId id : PileId.all()) piles.put(id, new Pile());
    }

    // --8<-- [start:deal]
    /** Shuffles a deck and deals: column {@code i} gets {@code i + 1} cards, the top one face up. */
    public static Klondike deal(boolean drawThree, RandomGenerator random) {
        List<Card> deck = Card.deck();
        Collections.shuffle(deck, random);
        Klondike game = new Klondike(drawThree);
        int next = 0;
        for (int column = 0; column < PileId.COLUMNS; column++) {
            Pile pile = game.pile(PileId.tableau(column));
            for (int i = 0; i <= column; i++) pile.add(deck.get(next++), i == column);
        }
        Pile stock = game.pile(PileId.STOCK);
        while (next < deck.size()) stock.add(deck.get(next++), false);
        return game;
    }
    // --8<-- [end:deal]

    /**
     * A game laid out as text, for tests: one line per pile ({@code stock}, {@code waste}, {@code f0} to
     * {@code f3}, {@code t0} to {@code t6}), cards in short notation bottom first, face-down cards of a column
     * in brackets. Piles not named are empty.
     *
     * <pre>
     * Klondike.of(false,
     *         "stock: 2C 3C",
     *         "f0: AS",
     *         "t0: [KD 9C] 8H 7S");
     * </pre>
     */
    public static Klondike of(boolean drawThree, String... lines) {
        Klondike game = new Klondike(drawThree);
        Map<String, PileId> names = new LinkedHashMap<>();
        for (PileId id : PileId.all()) names.put(id.toString(), id);
        for (String line : lines) {
            int colon = line.indexOf(':');
            PileId id = names.get(line.substring(0, colon).strip());
            if (id == null) throw new IllegalArgumentException("No pile: " + line);
            Pile pile = game.pile(id);
            boolean hidden = false;
            for (String token : line.substring(colon + 1).trim().split("\\s+")) {
                if (token.isEmpty()) continue;
                if (token.startsWith("[")) {
                    hidden = true;
                    token = token.substring(1);
                }
                boolean closes = token.endsWith("]");
                if (closes) token = token.substring(0, token.length() - 1);
                pile.add(Card.of(token), !hidden);
                if (closes) hidden = false;
            }
            if (id == PileId.STOCK) pile.setAllFaceDown();
            else if (id.kind() != PileId.Kind.TABLEAU) pile.setAllFaceUp();
        }
        return game;
    }

    /** An independent copy: changes to one never show in the other. */
    public Klondike copy() {
        Klondike copy = new Klondike(drawThree);
        piles.forEach((id, pile) -> copy.piles.put(id, pile.copy()));
        copy.moves = moves;
        return copy;
    }

    public Pile pile(PileId id) {
        return piles.get(id);
    }

    public boolean drawThree() {
        return drawThree;
    }

    /** Moves made so far, including draws from the stock. */
    public int moves() {
        return moves;
    }

    public boolean won() {
        for (int i = 0; i < PileId.FOUNDATIONS; i++) {
            if (pile(PileId.foundation(i)).size() < Rank.values().length) return false;
        }
        return true;
    }

    // --8<-- [start:draw]
    /**
     * Turns one or three cards from the stock onto the waste; with an empty stock, the waste is turned
     * over to become the stock again.
     *
     * @return true if anything changed
     */
    public boolean draw() {
        Pile stock = pile(PileId.STOCK);
        Pile waste = pile(PileId.WASTE);
        if (stock.isEmpty()) {
            if (waste.isEmpty()) return false;
            List<Card> cards = waste.takeFrom(0);
            Collections.reverse(cards);
            for (Card c : cards) stock.add(c, false);
        } else {
            int count = drawThree ? Math.min(3, stock.size()) : 1;
            for (int i = 0; i < count; i++) waste.add(stock.takeFrom(stock.size() - 1).getFirst(), true);
        }
        moves++;
        return true;
    }
    // --8<-- [end:draw]

    /**
     * True if the player may pick up the cards from {@code index} to the top of a pile: a face-up run of a
     * column, or the top card of the waste or a foundation.
     */
    public boolean canPick(PileId from, int index) {
        Pile pile = pile(from);
        if (index < 0 || index >= pile.size()) return false;
        return switch (from.kind()) {
            case STOCK -> false;
            case WASTE, FOUNDATION -> index == pile.size() - 1;
            case TABLEAU -> pile.faceUp(index);
        };
    }

    // --8<-- [start:can-move]
    /** True if the cards from {@code index} to the top of {@code from} may be put on {@code to}. */
    public boolean canMove(PileId from, int index, PileId to) {
        if (from.equals(to) || !canPick(from, index)) return false;
        Pile source = pile(from);
        Card moving = source.cards().get(index);
        int count = source.size() - index;
        Card target = pile(to).top();
        return switch (to.kind()) {
            case STOCK, WASTE -> false;
            // Foundations: one card at a time, ace first, then the same suit one rank up.
            case FOUNDATION -> count == 1 && (target == null
                    ? moving.rank() == Rank.ACE
                    : moving.suit() == target.suit() && moving.rank().value() == target.rank().value() + 1);
            // Columns: a king on an empty column, else the other colour one rank down.
            case TABLEAU -> target == null
                    ? moving.rank() == Rank.KING
                    : moving.red() != target.red() && moving.rank().value() == target.rank().value() - 1;
        };
    }
    // --8<-- [end:can-move]

    // --8<-- [start:move]
    /**
     * Moves the cards from {@code index} to the top of {@code from} onto {@code to} if the rules allow it.
     * A face-down card uncovered in a column is turned over.
     *
     * @return true if the cards moved
     */
    public boolean move(PileId from, int index, PileId to) {
        if (!canMove(from, index, to)) return false;
        Pile target = pile(to);
        for (Card c : pile(from).takeFrom(index)) target.add(c, true);
        pile(from).turnTop();
        moves++;
        return true;
    }
    // --8<-- [end:move]

    /** The foundation that takes the top card of a pile (for a double click), if any. */
    public Optional<PileId> foundationFor(PileId from) {
        Pile pile = pile(from);
        if (pile.isEmpty() || from.kind() == PileId.Kind.FOUNDATION) return Optional.empty();
        for (int i = 0; i < PileId.FOUNDATIONS; i++) {
            if (canMove(from, pile.size() - 1, PileId.foundation(i))) return Optional.of(PileId.foundation(i));
        }
        return Optional.empty();
    }

    /**
     * True if the rest of the game plays itself: stock and waste are empty and every card lies face up, so
     * the cards can go to the foundations in order.
     */
    public boolean canFinish() {
        if (won() || !pile(PileId.STOCK).isEmpty() || !pile(PileId.WASTE).isEmpty()) return false;
        for (int i = 0; i < PileId.COLUMNS; i++) {
            if (pile(PileId.tableau(i)).faceDown() > 0) return false;
        }
        return true;
    }

    /**
     * Moves the lowest card that fits onto a foundation, one step of finishing the game.
     *
     * @return the pile the card came from, empty if none fits
     */
    public Optional<PileId> finishStep() {
        PileId best = null;
        int lowest = Integer.MAX_VALUE;
        for (PileId id : PileId.all()) {
            Card top = pile(id).top();
            if (top != null && top.rank().value() < lowest && foundationFor(id).isPresent()) {
                best = id;
                lowest = top.rank().value();
            }
        }
        if (best == null) return Optional.empty();
        move(best, pile(best).size() - 1, foundationFor(best).orElseThrow());
        return Optional.of(best);
    }

    /** All piles with their cards, for showing the table. */
    public Map<PileId, Pile> piles() {
        return Collections.unmodifiableMap(piles);
    }

    /** Every pile in the text form of {@link #of}, one per line; useful in failing tests. */
    @Override
    public String toString() {
        List<String> lines = new ArrayList<>();
        piles.forEach((id, pile) -> {
            StringBuilder line = new StringBuilder(id.toString()).append(':');
            for (int i = 0; i < pile.size(); i++) {
                line.append(' ');
                if (id.kind() == PileId.Kind.TABLEAU && i == 0 && pile.faceDown() > 0) line.append('[');
                line.append(pile.cards().get(i).code());
                if (id.kind() == PileId.Kind.TABLEAU && i == pile.faceDown() - 1) line.append(']');
            }
            lines.add(line.toString());
        });
        return String.join("\n", lines);
    }
}
