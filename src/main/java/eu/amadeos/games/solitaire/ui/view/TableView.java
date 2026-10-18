package eu.amadeos.games.solitaire.ui.view;

import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.theme.DesktopTheme;
import eu.amadeos.gameengine.anim.Easing;
import eu.amadeos.gameengine.anim.Tween;
import eu.amadeos.gameengine.swing.Animator;
import eu.amadeos.games.solitaire.domain.Card;
import eu.amadeos.games.solitaire.domain.Klondike;
import eu.amadeos.games.solitaire.domain.Pile;
import eu.amadeos.games.solitaire.domain.PileId;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * The card table. It shows a {@link Klondike} game and animates every change: when a new state arrives,
 * each card flies from where it is shown to where the new state puts it. Moves, undo, a new deal and a card
 * dropped where it may not go all look right without the presenter knowing about animation.
 *
 * <p>Gestures: a click on the stock draws, cards are dragged with the left button, a double click sends the
 * top card of a pile to its foundation.
 */
public final class TableView extends JComponent {
    /** What the window does with the gestures. */
    public interface Listener {
        void draw();

        void move(PileId from, int index, PileId to);

        void moveToFoundation(PileId from);
    }

    /** The table cloth. */
    static final Color FELT = new Color(0x2E, 0x6B, 0x3F);
    /** How long a card flies to its new place. */
    static final long FLIGHT = Tween.millis(220);
    /** Pixels the mouse must move before a press becomes a drag. */
    private static final double DRAG_THRESHOLD = 3;

    /** A card on its way from one place to another. */
    private record Flight(Point2D from, Point2D to, Tween tween) {
        Point2D at(long now) {
            double t = tween.at(now);
            return new Point2D.Double(Tween.lerp(from.getX(), to.getX(), t), Tween.lerp(from.getY(), to.getY(), t));
        }
    }

    /** Cards held under the mouse. */
    private static final class Drag {
        final PileId from;
        final int index;
        final List<Card> cards;
        final double grabX;
        final double grabY;
        final Point2D pressed;
        Point2D mouse;
        boolean moved;

        Drag(PileId from, int index, List<Card> cards, Point2D card, Point2D mouse) {
            this.from = from;
            this.index = index;
            this.cards = cards;
            this.grabX = mouse.getX() - card.getX();
            this.grabY = mouse.getY() - card.getY();
            this.pressed = mouse;
            this.mouse = mouse;
        }
    }

    private final TableLayout layout;
    private final Listener listener;
    private final Animator animator = new Animator(this::frame);
    /** Where each card is drawn when it does not fly: its place in the last state shown. */
    private final Map<Card, Point2D> shown = new HashMap<>();
    private final Map<Card, Flight> flights = new HashMap<>();
    private Klondike game;
    private Drag drag;
    private Celebration celebration;

    public TableView(TableLayout layout, Listener listener) {
        this.layout = layout;
        this.listener = listener;
        setName("table");
        setOpaque(true);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) pressed(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dragged(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) released();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public TableLayout tableLayout() {
        return layout;
    }

    // --8<-- [start:show]
    /** Shows a new state of the game; every card that changed place flies there. */
    public void showGame(Klondike newGame) {
        if (celebration != null && !newGame.won()) celebration = null;
        long now = System.nanoTime();
        for (Map.Entry<Card, Point2D> place : layout.places(newGame).entrySet()) {
            Card card = place.getKey();
            Point2D target = place.getValue();
            Point2D current = position(card, now);
            if (current == null || current.distance(target) < 0.5) {
                flights.remove(card);
            } else {
                flights.put(card, new Flight(current, target, new Tween(now, FLIGHT, Easing.EASE_OUT)));
            }
            shown.put(card, target);
        }
        game = newGame;
        if (!flights.isEmpty()) animator.start();
        repaint();
    }

    /** Where a card is drawn at a time: on its way, or at its place. */
    private Point2D position(Card card, long now) {
        Flight flight = flights.get(card);
        return flight != null ? flight.at(now) : shown.get(card);
    }

    /** One animation frame: lands the cards that arrived; true while something still moves. */
    private boolean frame(long now) {
        flights.values().removeIf(f -> f.tween().done(now));
        if (celebration != null) celebration.step(now);
        repaint();
        return !flights.isEmpty() || celebration != null && !celebration.finished();
    }
    // --8<-- [end:show]

    /** Cards jump off the foundations and bounce across the table, leaving trails, until a click. */
    public void celebrate() {
        BufferedImage trail = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = trail.createGraphics();
        paintTable(g, Long.MAX_VALUE);
        g.dispose();
        celebration = new Celebration(trail);
        animator.start();
    }

    public boolean celebrating() {
        return celebration != null;
    }

    public boolean animating() {
        return animator.running();
    }

    // ---- mouse -----------------------------------------------------------------------------------

    // --8<-- [start:mouse]
    private void pressed(MouseEvent e) {
        if (game == null) return;
        if (celebration != null) {
            celebration = null;
            repaint();
            return;
        }
        Point2D point = e.getPoint();
        Optional<TableLayout.Hit> hit = layout.hit(game, point);
        if (hit.isEmpty()) return;
        PileId pile = hit.get().pile();
        int index = hit.get().index();
        if (pile.equals(PileId.STOCK)) {
            listener.draw();
        } else if (e.getClickCount() == 2 && index >= 0 && index == game.pile(pile).size() - 1) {
            drag = null;
            listener.moveToFoundation(pile);
        } else if (game.canPick(pile, index)) {
            List<Card> cards = new ArrayList<>(game.pile(pile).cards().subList(index, game.pile(pile).size()));
            for (Card c : cards) flights.remove(c);
            drag = new Drag(pile, index, cards, shown.get(cards.getFirst()), point);
        }
    }

    private void dragged(MouseEvent e) {
        if (drag == null) return;
        drag.mouse = e.getPoint();
        if (drag.mouse.distance(drag.pressed) > DRAG_THRESHOLD) drag.moved = true;
        repaint();
    }

    private void released() {
        if (drag == null) return;
        Drag done = drag;
        drag = null;
        if (!done.moved) {
            repaint();
            return;
        }
        Rectangle2D first = new Rectangle2D.Double(done.mouse.getX() - done.grabX, done.mouse.getY() - done.grabY,
                layout.cardWidth(), layout.cardHeight());
        PileId target = layout.dropTarget(game, first, done.from).orElse(done.from);
        // The cards are where they were dropped; from there they fly to wherever the presenter's answer puts them.
        for (int i = 0; i < done.cards.size(); i++) shown.put(done.cards.get(i), draggedAt(done, i));
        listener.move(done.from, done.index, target);
    }
    // --8<-- [end:mouse]

    private Point2D draggedAt(Drag d, int i) {
        return new Point2D.Double(d.mouse.getX() - d.grabX, d.mouse.getY() - d.grabY + i * layout.upStep());
    }

    // ---- painting --------------------------------------------------------------------------------

    @Override
    public Dimension getPreferredSize() {
        return layout.size();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (celebration != null) {
            g2.drawImage(celebration.trail, 0, 0, null);
        } else {
            paintTable(g2, System.nanoTime());
        }
        g2.dispose();
    }

    // --8<-- [start:paint]
    /** Felt, empty places, resting cards, then flying cards, then the cards under the mouse. */
    private void paintTable(Graphics2D g, long now) {
        g.setColor(FELT);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (game == null) return;
        DesktopTheme theme = AmadeUI.theme();
        double w = layout.cardWidth();
        double h = layout.cardHeight();
        List<Runnable> flying = new ArrayList<>();
        for (PileId id : PileId.all()) {
            Point2D slot = layout.slot(id);
            boolean ring = id.equals(PileId.STOCK) && !game.pile(PileId.WASTE).isEmpty();
            CardPainter.paintSlot(g, slot.getX(), slot.getY(), w, h, ring);
            Pile pile = game.pile(id);
            for (int i = 0; i < pile.size(); i++) {
                Card card = pile.cards().get(i);
                if (drag != null && drag.cards.contains(card)) continue;
                boolean up = pile.faceUp(i);
                Flight flight = now == Long.MAX_VALUE ? null : flights.get(card);
                if (flight != null) {
                    Point2D p = flight.at(now);
                    flying.add(() -> paintCard(g, theme, card, up, p));
                } else {
                    paintCard(g, theme, card, up, shown.get(card));
                }
            }
        }
        flying.forEach(Runnable::run);
        if (drag != null) {
            for (int i = 0; i < drag.cards.size(); i++) paintCard(g, theme, drag.cards.get(i), true, draggedAt(drag, i));
        }
    }
    // --8<-- [end:paint]

    private void paintCard(Graphics2D g, DesktopTheme theme, Card card, boolean up, Point2D at) {
        if (up) CardPainter.paintFace(g, card, at.getX(), at.getY(), layout.cardWidth(), layout.cardHeight());
        else CardPainter.paintBack(g, theme, at.getX(), at.getY(), layout.cardWidth(), layout.cardHeight());
    }

    // ---- the celebration -------------------------------------------------------------------------

    // --8<-- [start:celebration]
    /**
     * The famous ending: one card after another jumps off the foundations, kings first, falls, bounces on
     * the bottom edge and leaves a trail, because the picture is never cleared between frames.
     */
    private final class Celebration {
        final BufferedImage trail;
        final List<Card> queue = new ArrayList<>();
        final Map<Card, Point2D> starts = new HashMap<>();
        final Random random = new Random();
        Card card;
        double x, y, vx, vy;
        long last = -1;

        Celebration(BufferedImage trail) {
            this.trail = trail;
            for (int value = 13; value >= 1; value--) {
                for (int f = 0; f < PileId.FOUNDATIONS; f++) {
                    PileId id = PileId.foundation(f);
                    if (game.pile(id).size() >= value) {
                        Card c = game.pile(id).cards().get(value - 1);
                        queue.add(c);
                        starts.put(c, layout.cardAt(game, id, value - 1));
                    }
                }
            }
        }

        boolean finished() {
            return card == null && queue.isEmpty();
        }

        void step(long now) {
            // Physics in steps of 1/60 s, however often frames come.
            int steps = last < 0 ? 1 : (int) Math.clamp((now - last) / 16_000_000, 1, 4);
            last = now;
            double w = layout.cardWidth();
            double h = layout.cardHeight();
            Graphics2D g = trail.createGraphics();
            for (int s = 0; s < steps; s++) {
                if (card == null) {
                    if (queue.isEmpty()) break;
                    card = queue.removeFirst();
                    Point2D start = starts.get(card);
                    x = start.getX();
                    y = start.getY();
                    vx = (random.nextBoolean() ? 1 : -1) * w * (0.03 + random.nextDouble() * 0.07);
                    vy = -h * random.nextDouble() * 0.08;
                }
                vy += h * 0.012;
                x += vx;
                y += vy;
                if (y + h > trail.getHeight()) {
                    y = trail.getHeight() - h;
                    vy = -vy * 0.75;
                }
                CardPainter.paintFace(g, card, x, y, w, h);
                if (x + w < 0 || x > trail.getWidth()) card = null;
            }
            g.dispose();
        }
    }
    // --8<-- [end:celebration]
}
