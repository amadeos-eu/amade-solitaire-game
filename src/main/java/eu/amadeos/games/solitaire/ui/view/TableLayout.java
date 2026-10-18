package eu.amadeos.games.solitaire.ui.view;

import eu.amadeos.games.solitaire.domain.Card;
import eu.amadeos.games.solitaire.domain.Klondike;
import eu.amadeos.games.solitaire.domain.Pile;
import eu.amadeos.games.solitaire.domain.PileId;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where everything lies on the table, in pixels: the places of the piles and of each card in them, and
 * which card is under a point. Pure arithmetic on the card size, so it is tested without a window.
 *
 * <pre>
 *  stock  waste         f0    f1    f2    f3
 *  t0     t1    t2    t3    t4    t5    t6
 * </pre>
 */
public final class TableLayout {
    /** A card in a pile: its position from the bottom, 0 for the lowest. */
    public record Hit(PileId pile, int index) {
    }

    private final double cardWidth;
    private final double cardHeight;
    private final double gap;

    /** @param cardWidth the width of a card in pixels; the height follows at 1.4 times */
    public TableLayout(double cardWidth) {
        this.cardWidth = cardWidth;
        this.cardHeight = Math.round(cardWidth * 1.4);
        this.gap = Math.round(cardWidth / 7);
    }

    public double cardWidth() {
        return cardWidth;
    }

    public double cardHeight() {
        return cardHeight;
    }

    /** Space for the table: seven columns, and a column of six hidden cards and a full run of thirteen. */
    public Dimension size() {
        int width = (int) Math.ceil(2 * gap + 7 * cardWidth + 6 * gap);
        int height = (int) Math.ceil(row2() + 6 * downStep() + 12 * upStep() + cardHeight + gap);
        return new Dimension(width, height);
    }

    /** Vertical step between face-down cards of a column. */
    double downStep() {
        return Math.round(cardHeight * 0.1);
    }

    /** Vertical step between face-up cards of a column: enough to read the index. */
    double upStep() {
        return Math.round(cardHeight * 0.25);
    }

    private double row2() {
        return gap + cardHeight + gap;
    }

    // --8<-- [start:slot]
    /** The top left corner of a pile's place. */
    public Point2D slot(PileId id) {
        double column = switch (id.kind()) {
            case STOCK -> 0;
            case WASTE -> 1;
            case FOUNDATION -> 3 + id.index();
            case TABLEAU -> id.index();
        };
        double x = gap + column * (cardWidth + gap);
        double y = id.kind() == PileId.Kind.TABLEAU ? row2() : gap;
        return new Point2D.Double(x, y);
    }

    /** The top left corner of card {@code index} of a pile: columns fan downwards, the waste sideways. */
    public Point2D cardAt(Klondike game, PileId id, int index) {
        Point2D slot = slot(id);
        Pile pile = game.pile(id);
        double x = slot.getX();
        double y = slot.getY();
        switch (id.kind()) {
            case TABLEAU -> {
                for (int i = 0; i < index; i++) y += pile.faceUp(i) ? upStep() : downStep();
            }
            case WASTE -> {
                // Draw three: the top three cards fan out, so the player sees what comes next.
                int shown = game.drawThree() ? Math.min(3, pile.size()) : 1;
                int fanned = index - (pile.size() - shown);
                if (fanned > 0) x += fanned * cardWidth * 0.22;
            }
            default -> {
            }
        }
        return new Point2D.Double(x, y);
    }
    // --8<-- [end:slot]

    /** Every card with its place on the table. */
    public Map<Card, Point2D> places(Klondike game) {
        Map<Card, Point2D> places = new LinkedHashMap<>();
        for (PileId id : PileId.all()) {
            List<Card> cards = game.pile(id).cards();
            for (int i = 0; i < cards.size(); i++) places.put(cards.get(i), cardAt(game, id, i));
        }
        return places;
    }

    /** The rectangle of a card in a pile. */
    public Rectangle2D bounds(Klondike game, PileId id, int index) {
        Point2D p = cardAt(game, id, index);
        return new Rectangle2D.Double(p.getX(), p.getY(), cardWidth, cardHeight);
    }

    // --8<-- [start:hit]
    /** The card under a point, the topmost if cards overlap; a click on an empty place gives index -1. */
    public Optional<Hit> hit(Klondike game, Point2D point) {
        for (PileId id : PileId.all()) {
            Pile pile = game.pile(id);
            for (int i = pile.size() - 1; i >= 0; i--) {
                if (bounds(game, id, i).contains(point)) return Optional.of(new Hit(id, i));
            }
            Point2D slot = slot(id);
            if (pile.isEmpty() && new Rectangle2D.Double(slot.getX(), slot.getY(), cardWidth, cardHeight)
                    .contains(point)) {
                return Optional.of(new Hit(id, -1));
            }
        }
        return Optional.empty();
    }

    /**
     * Where dragged cards land: the foundation or column whose top card (or empty place) overlaps most with
     * the first dragged card. The pile they come from does not count. Empty if it touches none.
     */
    public Optional<PileId> dropTarget(Klondike game, Rectangle2D dragged, PileId from) {
        PileId best = null;
        double most = 0;
        for (PileId id : PileId.all()) {
            if (id.equals(from) || id.kind() != PileId.Kind.FOUNDATION && id.kind() != PileId.Kind.TABLEAU) continue;
            Pile pile = game.pile(id);
            Rectangle2D target = pile.isEmpty()
                    ? new Rectangle2D.Double(slot(id).getX(), slot(id).getY(), cardWidth, cardHeight)
                    : bounds(game, id, pile.size() - 1);
            Rectangle2D overlap = target.createIntersection(dragged);
            double area = overlap.isEmpty() ? 0 : overlap.getWidth() * overlap.getHeight();
            if (area > most) {
                most = area;
                best = id;
            }
        }
        return Optional.ofNullable(best);
    }
    // --8<-- [end:hit]
}
