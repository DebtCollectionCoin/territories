# UI/UX Guidelines

> **Agent Instructions:** All screens must follow Material Design 3. 
> Use Compose `MaterialTheme` throughout. Support light and dark modes from day one.

---

## Visual Identity

### Color Palette

```kotlin
// ui/theme/Color.kt

// Player colors — must be clearly distinguishable in both light and dark mode
val PlayerAColor = Color(0xFF1565C0)        // Deep Blue
val PlayerAColorAlpha = Color(0x401565C0)   // Translucent fill for territory
val PlayerBColor = Color(0xFFC62828)        // Deep Red
val PlayerBColorAlpha = Color(0x40C62828)   // Translucent fill for territory

// Board
val GridLineColor = Color(0xFF9E9E9E)       // Medium grey
val BoardBackground = Color(0xFFFAF8F3)     // Warm off-white (paper feel)
val BoardBackgroundDark = Color(0xFF1A1A1A) // Near-black for dark mode

// UI
val AccentColor = Color(0xFF4CAF50)         // Green for confirmations
val DangerColor = Color(0xFFE53935)         // Red for surrender/danger actions
```

### Typography
- Use **Roboto Mono** or **JetBrains Mono** for score numbers (fixed-width, avoids layout shifts)
- Use **Roboto** (system default) for all other text
- Minimum touch-target size: 48dp × 48dp (Material3 requirement)

### Grid & Dot Sizing

| Board Size | Cell size (dp) | Dot radius (dp) |
|------------|---------------|-----------------|
| Small (20×15) | 24dp | 5dp |
| Medium (30×20) | 16dp | 4dp |
| Large (60×40) | requires zoom | 3dp |

For large boards: enable pinch-to-zoom and pan. Start at a zoom level that shows the full board, allow zoom up to 3× to place precise dots.

---

## Screen Specifications

### Screen 1: Home

```
┌─────────────────────────────┐
│                             │
│      [App Logo / Icon]      │
│         TERRITORIES         │
│                             │
│      [ NEW GAME ]           │
│                             │
│      [ RESUME GAME ]        │  ← hidden if no saved game
│                             │
│      [ HOW TO PLAY ]        │
│                             │
│      [ SETTINGS ]           │
│                             │
└─────────────────────────────┘
```

- Logo: stylized grid with dots — conveys game immediately
- All buttons: full-width, rounded, Material3 `FilledButton` style
- No ads, no pop-ups, clean and focused

---

### Screen 2: New Game Setup

```
┌─────────────────────────────┐
│  ← Back         New Game   │
├─────────────────────────────┤
│  Board Size                 │
│  ○ Small  ● Medium  ○ Large │
│  ○ Custom: [20] × [15]      │
│                             │
│  Scoring                    │
│  ● Territory Area           │
│  ○ Captured Dots            │
│                             │
│  Opponent                   │
│  ○ Human (pass-and-play)    │
│  ● AI — Easy / Med / Hard   │
│                             │
│  First Player               │
│  ● Player 1  ○ Random       │
│                             │
│         [ START GAME ]      │
└─────────────────────────────┘
```

---

### Screen 3: Game Screen (Primary Screen)

```
┌─────────────────────────────┐
│  [≡]  A: 24 pts  B: 18 pts  │  ← HUD top bar
│        ▶ Player A's turn    │
├─────────────────────────────┤
│                             │
│                             │
│      [BOARD CANVAS]         │
│                             │
│    Grid with dots,          │
│    territory fills,         │
│    interaction highlights   │
│                             │
│                             │
├─────────────────────────────┤
│  [↩ Undo]      [✖ Surrender]│  ← HUD bottom bar
└─────────────────────────────┘
```

**Landscape orientation (phones/tablets):**
```
┌────────────────┬────────────┐
│                │  A: 24     │
│  [BOARD        │  B: 18     │
│   CANVAS]      │            │
│                │  ▶ A turn  │
│                │  Move: 45  │
│                │            │
│                │  [Undo]    │
│                │  [≡ Menu]  │
└────────────────┴────────────┘
```

---

### Screen 4: Result Screen

```
┌─────────────────────────────┐
│         GAME OVER           │
│                             │
│    🏆 Player A Wins!        │
│                             │
│   Final Score               │
│   Player A ████████ 124     │
│   Player B ████████ 87      │
│                             │
│   Stats                     │
│   Total moves: 203          │
│   Game time: 14:32          │
│                             │
│   [ PLAY AGAIN ]            │
│   [ NEW GAME ]              │
│   [ HOME ]                  │
└─────────────────────────────┘
```

---

## Interaction Design

### Dot Placement Flow
1. User touches the board → nearest intersection highlighted (preview dot, translucent)
2. User lifts finger → if intersection is legal, dot is placed with a subtle pop animation
3. If intersection is illegal → gentle shake animation, no placement
4. If move triggers a capture → territory fills in with an animated flood (expanding color fill)

### Animations
- Dot placement: scale from 0→1 in 150ms (`spring(dampingRatio = Spring.DampingRatioMediumBouncy)`)
- Territory fill: expanding ripple from the capturing dot, 400ms
- Score change: number counter animates (`animateIntAsState`)
- Turn indicator: slide in from top, 200ms
- AI thinking: pulsing dots indicator on the board during AI computation

### Touch Sensitivity
- Snap radius: tap snaps to nearest intersection within `cellSize * 0.7` pixels
- Outside snap radius: no action, no visual feedback
- This prevents accidental placement on large boards

---

## Accessibility

- [ ] All interactive elements have `contentDescription`
- [ ] Board intersections announced as "Row X, Column Y, [empty/Player A dot/Player B dot/territory]"
- [ ] Color is never the sole differentiator — use dot shape (circle vs. square) as a secondary signal for color-blind users
- [ ] High-contrast mode: toggle in Settings, increases grid line width and dot size
- [ ] Touch target minimum: 48dp (intercept tap and route to nearest legal intersection)

---

## Settings Screen

```
Board
  Default board size:    [Small / Medium / Large]
  Default scoring:       [Territory Area / Captured Dots]

Appearance
  Theme:                 [System / Light / Dark]
  Player A color:        [Color picker — Blue / Green / Purple / Custom]
  Player B color:        [Color picker — Red / Orange / Brown / Custom]
  High contrast mode:    [Toggle]
  Show move numbers:     [Toggle]
  Show last move:        [Toggle]

Gameplay
  Undo limit:            [5 / 10 / 50 / Unlimited]
  AI think time:         [Fast / Normal / Slow]
  AI difficulty:         [Easy / Medium / Hard]
  
Sound (future)
  Sound effects:         [Toggle]
  
About
  Version: 1.0.0
  How to Play
  Open Source Licenses
```

---

## Error States

| Error | Visual Treatment |
|-------|-----------------|
| Illegal move attempted | Brief red ring flash at tapped position; no placement |
| AI fails to find move | Show error toast; fall back to random move |
| Save failed | Toast: "Could not save game" — don't block gameplay |
| Database error | Log silently; don't show to user unless it affects gameplay |

---

## Empty States

| State | Message |
|-------|---------|
| No saved game (Home) | Hide "Resume" button — don't show empty state |
| Game history empty (future) | "No games played yet — start a new game!" |
