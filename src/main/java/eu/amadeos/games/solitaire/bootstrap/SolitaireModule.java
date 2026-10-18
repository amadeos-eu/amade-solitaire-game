package eu.amadeos.games.solitaire.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.time.GameClock;
import eu.amadeos.games.solitaire.repository.SolitaireSettings;
import eu.amadeos.games.solitaire.ui.main.SolitairePresenter;
import eu.amadeos.games.solitaire.ui.main.SolitaireView;
import eu.amadeos.games.solitaire.ui.main.SolitaireWindow;

import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * amade-solitaire-game bindings. Theme, language and the XDG directories come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class SolitaireModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.games.solitaire.messages";

    @Override
    protected void configure() {
        bind(SolitaireSettings.class).in(Scopes.SINGLETON);
        bind(SolitaireWindow.class).in(Scopes.SINGLETON);
        bind(SolitaireView.class).to(SolitaireWindow.class);
        bind(SolitairePresenter.class).in(Scopes.SINGLETON);
        bind(GameClock.class).toInstance(new GameClock());
    }

    /** Best times per draw mode in {@code ~/.local/share/amadeos/games/solitaire}. */
    @Provides
    @Singleton
    HighScores highScores(XdgDirectories xdg, AppInfo app) {
        return HighScores.of(xdg, app.configName(), Ranking.LOWER_WINS);
    }

    @Provides
    @Singleton
    PlayerName playerName(AppDirectories directories) {
        return new PlayerName(directories.config(), System.getProperty("user.name"));
    }

    @Provides
    RandomGenerator random() {
        return RandomGenerator.getDefault();
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(SolitaireModule.class, MESSAGES, locale);
    }
}
