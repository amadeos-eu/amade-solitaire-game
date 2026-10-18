package eu.amadeos.games.solitaire.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a pile lies on the table: the stock, the waste, one of four foundations or one of seven tableau
 * columns.
 */
public record PileId(Kind kind, int index) {
    public enum Kind { STOCK, WASTE, FOUNDATION, TABLEAU }

    public static final int FOUNDATIONS = 4;
    public static final int COLUMNS = 7;
    public static final PileId STOCK = new PileId(Kind.STOCK, 0);
    public static final PileId WASTE = new PileId(Kind.WASTE, 0);

    public PileId {
        int count = switch (kind) {
            case STOCK, WASTE -> 1;
            case FOUNDATION -> FOUNDATIONS;
            case TABLEAU -> COLUMNS;
        };
        if (index < 0 || index >= count) throw new IllegalArgumentException(kind + " " + index);
    }

    public static PileId foundation(int index) {
        return new PileId(Kind.FOUNDATION, index);
    }

    public static PileId tableau(int index) {
        return new PileId(Kind.TABLEAU, index);
    }

    /** All thirteen piles: stock, waste, the foundations, the columns. */
    public static List<PileId> all() {
        List<PileId> all = new ArrayList<>(List.of(STOCK, WASTE));
        for (int i = 0; i < FOUNDATIONS; i++) all.add(foundation(i));
        for (int i = 0; i < COLUMNS; i++) all.add(tableau(i));
        return all;
    }

    @Override
    public String toString() {
        return switch (kind) {
            case STOCK -> "stock";
            case WASTE -> "waste";
            case FOUNDATION -> "f" + index;
            case TABLEAU -> "t" + index;
        };
    }
}
