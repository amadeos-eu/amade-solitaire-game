package eu.amadeos.games.solitaire.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.games.solitaire.ui.main.SolitairePresenter;
import eu.amadeos.games.solitaire.ui.main.SolitaireWindow;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class AmadeSolitaireApplication {
    public static final AppInfo APP = new AppInfo("amade-solitaire-game", "Solitaire", "solitaire");

    private AmadeSolitaireApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeSolitaireApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            SolitaireWindow window = injector.getInstance(SolitaireWindow.class);
            SolitairePresenter presenter = injector.getInstance(SolitairePresenter.class);
            window.bind(presenter);
            presenter.start();
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    System.exit(0);
                }
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
        }, new SolitaireModule());
    }
}
