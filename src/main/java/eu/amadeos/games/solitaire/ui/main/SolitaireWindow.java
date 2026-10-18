package eu.amadeos.games.solitaire.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.swing.GameMenus;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.gameengine.swing.HighScoreDialog;
import eu.amadeos.games.solitaire.bootstrap.AmadeSolitaireApplication;
import eu.amadeos.games.solitaire.domain.Klondike;
import eu.amadeos.games.solitaire.domain.PileId;
import eu.amadeos.games.solitaire.ui.view.TableLayout;
import eu.amadeos.games.solitaire.ui.view.TableView;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Optional;

/**
 * The game window: the table, a status line with moves and time, and the menu. It only shows what
 * {@link SolitairePresenter} tells it and passes menu commands and the gestures of the table on.
 */
public final class SolitaireWindow extends JFrame implements SolitaireView {
    /** How often the clock is redrawn while a game runs. */
    private static final int TICK = 250;
    /** Pause between two cards when the game finishes itself. */
    private static final int FINISH_STEP = 90;

    private final Messages msg;
    private final GameMenus menus = new GameMenus(GameTexts.load());
    private final HighScores scores;
    private final PlayerName playerName;
    private final TableView table;
    private final JLabel movesLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();
    private final JMenuItem undoItem;
    private final JMenuItem redoItem;
    private final JRadioButtonMenuItem drawOneItem;
    private final JRadioButtonMenuItem drawThreeItem;
    private final Timer ticker;
    private final Timer finisher;
    private SolitairePresenter presenter;

    @Inject
    public SolitaireWindow(Messages msg, HighScores scores, PlayerName playerName) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.scores = scores;
        this.playerName = playerName;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        java.net.URL icon = getClass().getResource("/icons/amade-solitaire-game.png");
        if (icon != null) setIconImage(new ImageIcon(icon).getImage());

        table = new TableView(new TableLayout(cardWidth()), new TableView.Listener() {
            @Override
            public void draw() {
                presenter.draw();
            }

            @Override
            public void move(PileId from, int index, PileId to) {
                presenter.move(from, index, to);
            }

            @Override
            public void moveToFoundation(PileId from) {
                presenter.moveToFoundation(from);
            }
        });
        movesLabel.setName("moves");
        timeLabel.setName("time");
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        status.setBorder(AmadeUI.sunken());
        status.add(movesLabel);
        status.add(timeLabel);
        JPanel content = new JPanel(new BorderLayout());
        content.add(table, BorderLayout.CENTER);
        content.add(status, BorderLayout.SOUTH);
        setContentPane(content);

        undoItem = menus.undo(() -> presenter.undo());
        redoItem = menus.redo(() -> presenter.redo());
        drawOneItem = Mnemonics.apply(new JRadioButtonMenuItem(), msg.get("menu.game.draw-one"));
        drawOneItem.setName("game-draw-one");
        drawOneItem.addActionListener(e -> presenter.setDrawThree(false));
        drawThreeItem = Mnemonics.apply(new JRadioButtonMenuItem(), msg.get("menu.game.draw-three"));
        drawThreeItem.setName("game-draw-three");
        drawThreeItem.addActionListener(e -> presenter.setDrawThree(true));
        ButtonGroup group = new ButtonGroup();
        group.add(drawOneItem);
        group.add(drawThreeItem);
        setJMenuBar(menuBar());

        ticker = new Timer(TICK, e -> presenter.tick());
        finisher = new Timer(FINISH_STEP, e -> presenter.finishStep());
        pack();
        setMinimumSize(getSize());
    }

    /** Connects the window with the presenter; call once before {@link SolitairePresenter#start()}. */
    public void bind(SolitairePresenter presenter) {
        this.presenter = presenter;
    }

    /** About 5.5 times the text size, so the cards grow with the font on large screens. */
    static int cardWidth() {
        java.awt.Font font = UIManager.getFont("Label.font");
        int size = font == null ? 12 : font.getSize();
        return Math.max(56, Math.round(size * 5.5f));
    }

    private JMenuBar menuBar() {
        JMenu game = menus.game();
        game.add(menus.newGame(() -> presenter.newGame()));
        game.add(undoItem);
        game.add(redoItem);
        game.addSeparator();
        game.add(drawOneItem);
        game.add(drawThreeItem);
        game.addSeparator();
        game.add(menus.highScores(() -> presenter.showScores()));
        game.addSeparator();
        game.add(menus.exit(this::dispose));
        JMenu help = menus.help();
        help.add(menus.about(msg.get("window.title"), () -> presenter.showAbout()));
        JMenuBar bar = new JMenuBar();
        bar.add(game);
        bar.add(help);
        return bar;
    }

    // ---- SolitaireView ---------------------------------------------------------------------------

    @Override
    public void showGame(Klondike game) {
        table.showGame(game);
    }

    @Override
    public void showMoves(int moves) {
        movesLabel.setText(msg.format("status.moves", moves));
    }

    @Override
    public void showTime(int seconds) {
        timeLabel.setText(msg.format("status.time", seconds / 60, String.format("%02d", seconds % 60)));
    }

    @Override
    public void showOptions(boolean drawThree) {
        (drawThree ? drawThreeItem : drawOneItem).setSelected(true);
    }

    @Override
    public void showUndo(boolean canUndo, boolean canRedo) {
        undoItem.setEnabled(canUndo);
        redoItem.setEnabled(canRedo);
    }

    @Override
    public void setTicking(boolean ticking) {
        if (ticking) ticker.start();
        else ticker.stop();
    }

    @Override
    public void setFinishing(boolean finishing) {
        if (finishing) finisher.start();
        else finisher.stop();
    }

    @Override
    public void celebrate() {
        table.celebrate();
    }

    @Override
    public Optional<String> askName(String message) {
        return HighScoreDialog.askName(this, message, playerName);
    }

    @Override
    public void showScores(boolean drawThree, int highlight) {
        List<HighScoreDialog.Table> tables = List.of(
                new HighScoreDialog.Table(SolitairePresenter.tableId(false), msg.get("mode.draw-one")),
                new HighScoreDialog.Table(SolitairePresenter.tableId(true), msg.get("mode.draw-three")));
        HighScoreDialog.show(this, scores, tables,
                seconds -> msg.format("status.time", seconds / 60, String.format("%02d", seconds % 60)),
                SolitairePresenter.tableId(drawThree), highlight);
    }

    @Override
    public void showAbout() {
        // Swing's HTML only wraps lines with a fixed body width; it grows with the font like the cards.
        int width = cardWidth() * 5;
        JOptionPane.showMessageDialog(this,
                "<html><body style='width: " + width + "px'><center><h2>"
                        + msg.get("window.title") + " " + AmadeSolitaireApplication.version() + "</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p><br>"
                        + "<p>" + msg.get("dialog.about.rules") + "</p><br>"
                        + "<small>amade-solitaire-game &bull; &copy; 2026 datazuul.com &bull; EUPL-1.2</small>"
                        + "</center></html>",
                Mnemonics.strip(menus.texts().format("menu.help.about", msg.get("window.title"))),
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void dispose() {
        ticker.stop();
        finisher.stop();
        super.dispose();
    }

    // ---- for tests -------------------------------------------------------------------------------

    TableView table() {
        return table;
    }

    JLabel movesLabel() {
        return movesLabel;
    }

    JMenuItem undoItem() {
        return undoItem;
    }

    boolean finishing() {
        return finisher.isRunning();
    }
}
