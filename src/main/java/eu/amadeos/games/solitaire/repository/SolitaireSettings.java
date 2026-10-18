package eu.amadeos.games.solitaire.repository;

import com.google.inject.Inject;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.PropertiesFile;

import java.nio.file.Path;
import java.util.Properties;

/** Draw one or three cards, in {@code ~/.config/amadeos/solitaire/solitaire.properties}. */
public final class SolitaireSettings {
    private static final String DRAW_THREE = "draw-three";

    private final Path file;

    @Inject
    public SolitaireSettings(AppDirectories directories) {
        this(directories.configFile("solitaire.properties"));
    }

    public SolitaireSettings(Path file) {
        this.file = file;
    }

    public boolean drawThree() {
        return Boolean.parseBoolean(PropertiesFile.load(file).getProperty(DRAW_THREE, "false"));
    }

    public void setDrawThree(boolean on) {
        Properties properties = PropertiesFile.load(file);
        properties.setProperty(DRAW_THREE, Boolean.toString(on));
        PropertiesFile.store(file, properties, "amade-solitaire-game settings");
    }
}
