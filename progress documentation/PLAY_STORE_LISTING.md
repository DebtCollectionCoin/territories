# Play Store listing copy

Drop these strings into the Play Console **Main store listing** (or the
appropriate localised entry). The 0.2.0 release notes section can be
reused for `app-android/src/main/play/release-notes/en-GB/internal.txt`
if you wire up the [Triple-T gradle-play-publisher](https://github.com/Triple-T/gradle-play-publisher)
plugin later.

## App name

`Territories`

## Short description (≤ 80 chars)

```
Surround your opponent's dots before they surround yours. Pure strategy.
```

(78 chars.)

## Full description (≤ 4000 chars)

```
Territories is a fast, offline strategy duel. Place your dots one at a
time and surround your opponent's pieces to claim their stones — and
the empty squares behind your wall — as your territory.

▸ Pure strategy, no luck. The first player to surround the most
  enemy stones wins.
▸ Three AI opponents — Easy for learning the patterns, Medium for a
  fair fight, Hard for a deep, multi-ply minimax search that hunts
  for surround-and-capture combinations.
▸ Local multiplayer on a single device — pass-and-play with a friend.
▸ Three board sizes (small / medium / large) and two scoring variants
  (territory area or captured stones only).
▸ Full game history with replay — review your captured games move by
  move.
▸ Accessibility: colour-blind mode (distinct shapes for each player),
  TalkBack support, configurable haptics and sound.
▸ Works fully offline. No accounts, no ads, no tracking.

How a game flows:
  1. Players take turns placing one dot per turn on an empty square.
  2. When you completely encircle one or more enemy dots with your
     own dots and edges of the board, the enclosed area becomes your
     territory and the captured dots count toward your score.
  3. The game ends when the board fills up or both players surrender;
     whoever has the most captured stones (or biggest territory, in
     area mode) wins.

Tap to play, hold for hints, two-finger pinch to zoom on larger boards.

Built as a Kotlin multiplatform engine — also available on the web at
the project's GitHub Pages site.
```

## Categorisation

- **App category:** Game
- **Game category:** Board
- **Tags:** strategy, board game, two player, AI, offline

## Contact details

- Email: `<your-email>@<example.com>`
- Website: `https://github.com/DebtCollectionCoin/territories`
- Privacy policy URL: see `progress documentation/PRIVACY_POLICY.md`

## Graphic assets checklist

| Asset | Spec | Status |
|---|---|---|
| App icon | 512×512 PNG, 32-bit | TODO — `app-android/src/main/res/mipmap-*/ic_launcher.*` |
| Feature graphic | 1024×500 PNG/JPG (no transparency) | TODO |
| Phone screenshots | min 2, 16:9 or 9:16, ≥ 1080 px on long edge | TODO — capture: home, in-game (mid-game), in-game (capture moment), history, settings |
| 7-inch tablet screenshots | optional but recommended | TODO |
| 10-inch tablet screenshots | optional but recommended | TODO |

The desktop build runs the same Compose UI and is convenient for
capturing screenshots at controlled resolutions.

## Release notes — v0.2.0

```
What's new in 0.2.0:
• Tuned AI: opponents now play more aggressively, hunting for local
  encirclements instead of building unhelpful long walls.
• Share games via link on the web build (#g=… fragments).
• TalkBack now announces score, turn, and last move on the board.
• Internal: history-replay now has end-to-end test coverage.
```

## Data safety form quick answers

- Data collected: **None**
- Data shared with third parties: **None**
- Data encrypted in transit: **N/A** (no network)
- Users can request data deletion: **N/A** (no data leaves the device)

The app stores game history in an on-device Room database only.
Clearing app data deletes all of it.
