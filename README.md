# amade-solitaire-game

**amade-solitaire-game** is the card game of the [amadeOS](https://github.com/amadeos-eu/amadeos) desktop: Klondike, the classic patience. Built on [amade-game-engine](https://github.com/amadeos-eu/amade-game-engine); runs on other operating systems as well.

![License](https://img.shields.io/badge/license-EUPL--1.2-blue)
![Java](https://img.shields.io/badge/Java-25-orange)

## Playing

- Build the four **foundations** (top right) up from ace to king, each in one suit.
- In the seven **columns**, cards go down in alternating colours: a black 7 on a red 8. Only a king may fill an empty column. Runs of face-up cards move together, and a face-down card that is uncovered turns over by itself.
- **Click the stock** (top left) to turn over one card, or three with *Game → Draw Three*. When the stock is empty, a click turns the waste over again.
- **Drag** cards with the mouse. **Double-click** a card to send it to its foundation.
- **Undo** (Ctrl+Z) and **redo** (Ctrl+Y) as far back as the game goes.
- Once every card lies face up and the stock is empty, the game **finishes itself**, and the cards bounce across the table. Click to stop them.
- **Best times**, ten for each draw mode (*Game → High Scores...*).

Every change on the table is animated: cards fly to their new place after a move, an undo, a new deal, and back to where they came from when dropped where they may not go. Cards are drawn in code (indices, pips, court cards, suit symbols), so they are sharp at any size. The backs take the accent colour of the desktop theme, and the cards grow with the font size.

Texts in English, Deutsch, Français, Español and Italiano.

## Files

| What | Where |
|---|---|
| Settings (draw one or three) | `~/.config/amadeos/solitaire/solitaire.properties` |
| Name for high scores | `~/.config/amadeos/solitaire/player.properties` |
| Best times | `~/.local/share/amadeos/games/solitaire/draw-one.scores`, `draw-three.scores` |

## Building

```bash
./gradlew test          # the window tests need a display (xvfb-run on servers)
./gradlew run
./gradlew deb           # build/distributions/amade-solitaire-game_<version>_all.deb
./gradlew installToAmadeOs   # copies the package to ../amadeos/config/packages.chroot/
```

With `../amade-game-engine` (and `../amade-commons`) checked out next to this repository, Gradle builds those checkouts instead of the JitPack versions.

## Structure

- `domain`: the rules without user interface. `Klondike` (deal, draw, moves, finishing, copies for undo; a text form for tests), `Pile`, `PileId`, `Card`, `Rank`, `Suit`.
- `repository`: `SolitaireSettings`.
- `ui.main`: Model-View-Presenter. `SolitairePresenter` runs the game without Swing (moves, undo with the engine's `History`, clock, finishing, best times), `SolitaireView` is what it needs from the window, `SolitaireWindow` implements it. Tests drive the presenter through a `FakeView`, without a display.
- `ui.view`: `TableView` (shows the game, animates every card to its new place with the engine's `Tween` and `Animator`, drag and drop, the bouncing cards), `TableLayout` (where piles and cards lie, which card is under the mouse; tested without a window), `CardPainter` (faces, backs, suit symbols as shapes).

Comments of the form `// --8<-- [start:name]` mark the excerpts the [Game Developer's Handbook](https://datazuul.github.io/amade-game-engine/) quotes.

## Licence

[EUPL-1.2](LICENSE)
