package eu.amadeos.games.solitaire.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolitaireSettingsTest {
    @TempDir
    Path dir;

    @Test
    void drawOneAtFirstThenAsChosen() {
        Path file = dir.resolve("solitaire.properties");
        assertFalse(new SolitaireSettings(file).drawThree());
        new SolitaireSettings(file).setDrawThree(true);
        assertTrue(new SolitaireSettings(file).drawThree());
    }
}
