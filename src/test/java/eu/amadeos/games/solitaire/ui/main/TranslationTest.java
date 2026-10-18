package eu.amadeos.games.solitaire.ui.main;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.i18n.TranslationCheck;
import eu.amadeos.gameengine.swing.GameTexts;
import eu.amadeos.games.solitaire.bootstrap.SolitaireModule;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationTest {
    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(SolitaireModule.class, SolitaireModule.MESSAGES));
    }

    @Test
    void textsAreFormattedInTheLanguage() {
        Messages german = Messages.load(SolitaireModule.class, SolitaireModule.MESSAGES, Locale.GERMANY);
        assertEquals("Zeit: 2:05", german.format("status.time", 2, "05"));
        assertEquals("Sie haben mit „Drei Karten“ in 95 Sekunden gewonnen.",
                german.format("scores.won", german.get("mode.draw-three"), 95));
        assertEquals("Sie haben mit „Eine Karte“ in einer Sekunde gewonnen.",
                german.format("scores.won", german.get("mode.draw-one"), 1));
        Messages english = Messages.load(SolitaireModule.class, SolitaireModule.MESSAGES, Locale.ENGLISH);
        assertEquals("You won with \"Draw one\" in 1 second.", english.format("scores.won", "Draw one", 1));
    }

    /** The Game menu mixes the engine's entries with the game's own; no two may share a letter. */
    @Test
    void mnemonicsAreUniqueInTheGameMenu() {
        Map<String, String> clashes = new HashMap<>();
        for (Language language : Language.values()) {
            Messages engine = GameTexts.load(language.locale());
            Messages game = Messages.load(SolitaireModule.class, SolitaireModule.MESSAGES, language.locale());
            List<String> texts = List.of(engine.get("menu.game.new"), engine.get("menu.game.undo"),
                    engine.get("menu.game.redo"), game.get("menu.game.draw-one"), game.get("menu.game.draw-three"),
                    engine.get("menu.game.scores"), engine.get("menu.game.exit"));
            Map<Character, String> seen = new HashMap<>();
            for (String text : texts) {
                Mnemonics.Text t = Mnemonics.parse(text);
                if (t.index() < 0) {
                    clashes.put(language.code() + " " + text, "no mnemonic");
                    continue;
                }
                String other = seen.put(Character.toLowerCase(t.text().charAt(t.index())), text);
                if (other != null) clashes.put(language.code() + " " + text, other);
            }
        }
        assertEquals(Map.of(), clashes);
    }
}
