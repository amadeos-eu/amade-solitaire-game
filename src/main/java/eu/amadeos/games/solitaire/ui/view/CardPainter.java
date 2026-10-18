package eu.amadeos.games.solitaire.ui.view;

import eu.amadeos.commons.theme.DesktopTheme;
import eu.amadeos.games.solitaire.domain.Card;
import eu.amadeos.games.solitaire.domain.Rank;
import eu.amadeos.games.solitaire.domain.Suit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Draws cards from shapes: no images, no fonts with suit symbols, so cards look the same everywhere and are
 * sharp at any size. The back takes the accent colour of the desktop theme.
 */
public final class CardPainter {
    static final Color RED = new Color(0xC8, 0x10, 0x2E);
    static final Color FACE = Color.WHITE;
    static final Color EDGE = new Color(0x55, 0x55, 0x55);

    /**
     * Where the pips of 2 to 10 sit, as fractions of the card's inner area (x, y); index 0 is the ace. Pips
     * in the lower half are drawn upside down, as on printed cards.
     */
    // --8<-- [start:pips]
    private static final double[][][] PIPS = {
            {{0.5, 0.5}},
            {{0.5, 0.1}, {0.5, 0.9}},
            {{0.5, 0.1}, {0.5, 0.5}, {0.5, 0.9}},
            {{0.2, 0.1}, {0.8, 0.1}, {0.2, 0.9}, {0.8, 0.9}},
            {{0.2, 0.1}, {0.8, 0.1}, {0.5, 0.5}, {0.2, 0.9}, {0.8, 0.9}},
            {{0.2, 0.1}, {0.8, 0.1}, {0.2, 0.5}, {0.8, 0.5}, {0.2, 0.9}, {0.8, 0.9}},
            {{0.2, 0.1}, {0.8, 0.1}, {0.5, 0.3}, {0.2, 0.5}, {0.8, 0.5}, {0.2, 0.9}, {0.8, 0.9}},
            {{0.2, 0.1}, {0.8, 0.1}, {0.5, 0.3}, {0.2, 0.5}, {0.8, 0.5}, {0.5, 0.7}, {0.2, 0.9}, {0.8, 0.9}},
            {{0.2, 0.1}, {0.8, 0.1}, {0.2, 0.37}, {0.8, 0.37}, {0.5, 0.5}, {0.2, 0.63}, {0.8, 0.63},
                    {0.2, 0.9}, {0.8, 0.9}},
            {{0.2, 0.1}, {0.8, 0.1}, {0.5, 0.24}, {0.2, 0.37}, {0.8, 0.37}, {0.2, 0.63}, {0.8, 0.63},
                    {0.5, 0.76}, {0.2, 0.9}, {0.8, 0.9}},
    };
    // --8<-- [end:pips]

    private CardPainter() {
    }

    /** Rounded corners: about a twelfth of the width. */
    static RoundRectangle2D outline(double x, double y, double w, double h) {
        return new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, w / 6, w / 6);
    }

    // --8<-- [start:face]
    /** The face of a card: indices in two corners, pips or a court picture in the middle. */
    public static void paintFace(Graphics2D g, Card card, double x, double y, double w, double h) {
        Graphics2D c = (Graphics2D) g.create();
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        c.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        RoundRectangle2D outline = outline(x, y, w, h);
        c.setColor(FACE);
        c.fill(outline);
        c.setColor(EDGE);
        c.draw(outline);

        Color ink = card.red() ? RED : Color.BLACK;
        c.setColor(ink);
        paintIndex(c, card, x, y, w, h);
        // The other index: the same drawing turned by 180° around the card's centre.
        Graphics2D turned = (Graphics2D) c.create();
        turned.rotate(Math.PI, x + w / 2, y + h / 2);
        paintIndex(turned, card, x, y, w, h);
        turned.dispose();

        // The middle, inside the indices.
        double ix = x + w * 0.22;
        double iy = y + h * 0.12;
        double iw = w * 0.56;
        double ih = h * 0.76;
        if (card.rank().value() <= 10) {
            double pip = w * (card.rank() == Rank.ACE ? 0.42 : 0.2);
            for (double[] p : PIPS[card.rank().value() - 1]) {
                double cx = ix + p[0] * iw;
                double cy = iy + p[1] * ih;
                fillSuit(c, card.suit(), cx, cy, pip, p[1] > 0.5);
            }
        } else {
            paintCourt(c, card, ix, iy, iw, ih, ink);
        }
        c.dispose();
    }
    // --8<-- [end:face]

    /** Rank and a small suit in the top left corner. */
    private static void paintIndex(Graphics2D g, Card card, double x, double y, double w, double h) {
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, (int) Math.round(h * 0.15));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String label = card.rank().label();
        double cx = x + w * 0.12;
        float tx = (float) (cx - fm.stringWidth(label) / 2.0);
        float ty = (float) (y + h * 0.04 + fm.getAscent() * 0.86);
        if (label.length() > 1) {
            // "10" is wider: squeeze it a little so it stays in the corner.
            Graphics2D narrow = (Graphics2D) g.create();
            narrow.translate(cx, 0);
            narrow.scale(0.8, 1);
            narrow.drawString(label, (float) (-fm.stringWidth(label) / 2.0), ty);
            narrow.dispose();
        } else {
            g.drawString(label, tx, ty);
        }
        fillSuit(g, card.suit(), cx, ty + h * 0.08, w * 0.13, false);
    }

    /** Jack, queen and king: a frame in the suit's colour with the letter and two suits. */
    private static void paintCourt(Graphics2D g, Card card, double x, double y, double w, double h, Color ink) {
        Rectangle2D frame = new Rectangle2D.Double(x, y, w, h);
        g.setColor(card.red() ? new Color(0xFB, 0xE9, 0xEC) : new Color(0xEC, 0xEE, 0xF4));
        g.fill(frame);
        g.setColor(ink);
        g.setStroke(new BasicStroke((float) Math.max(1, w / 40)));
        g.draw(frame);
        Font font = new Font(Font.SERIF, Font.BOLD, (int) Math.round(h * 0.42));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String letter = card.rank().label();
        g.drawString(letter, (float) (x + (w - fm.stringWidth(letter)) / 2),
                (float) (y + h / 2 + fm.getAscent() * 0.35));
        fillSuit(g, card.suit(), x + w * 0.22, y + h * 0.13, w * 0.24, false);
        fillSuit(g, card.suit(), x + w * 0.78, y + h * 0.87, w * 0.24, true);
    }

    // --8<-- [start:back]
    /** The back: a lattice in the theme's accent colour inside a white border. */
    public static void paintBack(Graphics2D g, DesktopTheme theme, double x, double y, double w, double h) {
        Graphics2D c = (Graphics2D) g.create();
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D outline = outline(x, y, w, h);
        c.setColor(FACE);
        c.fill(outline);
        double inset = w * 0.08;
        RoundRectangle2D inner = new RoundRectangle2D.Double(x + inset, y + inset, w - 2 * inset, h - 2 * inset,
                w / 10, w / 10);
        Color accent = theme.activeTitleBg();
        c.setColor(accent);
        c.fill(inner);
        c.clip(inner);
        c.setColor(accent.brighter());
        c.setStroke(new BasicStroke((float) Math.max(1, w / 50)));
        double step = w / 7;
        for (double d = -h; d < w + h; d += step) {
            c.draw(new Line2D.Double(x + d, y, x + d + h, y + h));
            c.draw(new Line2D.Double(x + d + h, y, x + d, y + h));
        }
        c.setClip(null);
        c.setColor(EDGE);
        c.draw(outline);
        c.dispose();
    }
    // --8<-- [end:back]

    /** An empty place: a rounded outline on the table, with a ring on an empty stock that can be turned. */
    public static void paintSlot(Graphics2D g, double x, double y, double w, double h, boolean ring) {
        Graphics2D c = (Graphics2D) g.create();
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        c.setColor(new Color(255, 255, 255, 40));
        c.fill(outline(x, y, w, h));
        c.setColor(new Color(255, 255, 255, 110));
        c.setStroke(new BasicStroke((float) Math.max(1.5, w / 40)));
        c.draw(outline(x, y, w, h));
        if (ring) {
            double r = w * 0.25;
            c.draw(new Ellipse2D.Double(x + w / 2 - r, y + h / 2 - r, 2 * r, 2 * r));
        }
        c.dispose();
    }

    // --8<-- [start:suits]
    /**
     * A suit symbol centred on (cx, cy), {@code size} wide, upside down if {@code turned}. The shapes are
     * defined in a unit square and scaled, the way vector graphics work.
     */
    static void fillSuit(Graphics2D g, Suit suit, double cx, double cy, double size, boolean turned) {
        AffineTransform at = new AffineTransform();
        at.translate(cx, cy);
        if (turned) at.rotate(Math.PI);
        at.scale(size, size);
        at.translate(-0.5, -0.5);
        g.fill(at.createTransformedShape(shape(suit)));
    }

    static Shape shape(Suit suit) {
        Path2D p = new Path2D.Double();
        switch (suit) {
            case DIAMONDS -> {
                p.moveTo(0.5, 0);
                p.lineTo(0.9, 0.5);
                p.lineTo(0.5, 1);
                p.lineTo(0.1, 0.5);
                p.closePath();
            }
            case HEARTS -> {
                p.moveTo(0.5, 1);
                p.curveTo(0.15, 0.7, 0, 0.5, 0, 0.3);
                p.curveTo(0, 0.05, 0.4, -0.05, 0.5, 0.25);
                p.curveTo(0.6, -0.05, 1, 0.05, 1, 0.3);
                p.curveTo(1, 0.5, 0.85, 0.7, 0.5, 1);
                p.closePath();
            }
            case SPADES -> {
                p.moveTo(0.5, 0);
                p.curveTo(0.85, 0.3, 1, 0.45, 1, 0.62);
                p.curveTo(1, 0.85, 0.65, 0.9, 0.53, 0.72);
                p.lineTo(0.62, 1);
                p.lineTo(0.38, 1);
                p.lineTo(0.47, 0.72);
                p.curveTo(0.35, 0.9, 0, 0.85, 0, 0.62);
                p.curveTo(0, 0.45, 0.15, 0.3, 0.5, 0);
                p.closePath();
            }
            case CLUBS -> {
                p.append(new Ellipse2D.Double(0.29, 0, 0.42, 0.42), false);
                p.append(new Ellipse2D.Double(0, 0.36, 0.42, 0.42), false);
                p.append(new Ellipse2D.Double(0.58, 0.36, 0.42, 0.42), false);
                p.moveTo(0.44, 0.5);
                p.lineTo(0.56, 0.5);
                p.lineTo(0.64, 1);
                p.lineTo(0.36, 1);
                p.closePath();
            }
        }
        return p;
    }
    // --8<-- [end:suits]
}
