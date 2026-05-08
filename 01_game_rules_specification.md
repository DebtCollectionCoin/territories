# Game Rules Specification

> Source: Czech Wikipedia article on *Židi* (Wikipedie, rev. 25495607)
> This document is the authoritative, implementation-ready rule set for the **Territories** game engine.

---

## 1. Setup

### 1.1 Board
- A finite rectangular grid of **intersections** (nodes).
- Default recommended size: **60 columns × 40 rows** of squares → 61×41 intersections.
- The grid has a **border** (the outermost ring of intersections adjacent to the edge of the paper/board).
- Board size is configurable; common quick-game sizes: 20×15, 30×20.

### 1.2 Players
- Exactly **2 players**: Player A and Player B.
- Each player has a distinct color/symbol for their dots.

### 1.3 Starting State
- All intersections are **empty**.
- No territories exist.
- Player A moves first (or determined by coin-flip / random).

---

## 2. Anatomy of a Turn

On each turn, the active player must place exactly **one dot** on any **legal intersection**.

### 2.1 Legal Placement
An intersection is **legal** if and only if:
- It is **empty** (no dot of any color occupies it), AND
- It is **not inside any existing territory** (neither player's).

Dots placed in the center of squares (alternate rule variant) — see Section 8.

### 2.2 After Placement
Immediately after placing a dot, check for **capture** (Section 4).
If one or more captures occur, resolve them all before passing the turn.

---

## 3. Connectivity and Neighborhood

### 3.1 Adjacency (for loop/circuit detection)
Two dots are **adjacent** if they are:
- Directly horizontal or vertical neighbors (4-connected), OR
- Diagonal neighbors (8-connected / Moore neighborhood).

> **Critical rule:** The capture algorithm uses **8-directional** adjacency for determining whether a loop is closed (no gap). This is what distinguishes Židi from Go, and is a core implementation detail.

### 3.2 Paths and Circuits
- A **path** is a sequence of distinct dots where each consecutive pair is adjacent.
- A **circuit** is a path where the last dot is adjacent to the first.
- A circuit is **gap-free** if there exists at least one dot in the circuit from which you can traverse the entire circuit and return to start using only valid adjacent steps, visiting each dot exactly once.

---

## 4. Capture (Territory Formation)

### 4.1 Capture Condition
After a player places a dot, a **capture occurs** if there now exists a gap-free circuit composed entirely of the **active player's dots** that encloses one or more **opponent dots**.

More precisely:
- Find all connected regions of empty intersections or opponent dots that are **completely surrounded** by the active player's dots (including dots already part of the player's own territories).
- A region is "completely surrounded" if no path from any point in the region can reach the **board border** without crossing an active player's dot.

### 4.2 What Is Captured
The **territory** consists of:
- All opponent dots enclosed by the circuit.
- All empty intersections enclosed by the circuit.
- The capturing player's own dots that were enclosed (if any — this can happen with nested structures).

> **Note:** You CAN capture a territory that the opponent has already captured (re-capture). The entire previously-captured region is absorbed into the new territory.

### 4.3 Wall Rule (Critical Difference from Go)
- A player **cannot** capture opponent dots that are connected to the board border without an intercepting ring of the capturing player's dots.
- Equivalently: if an opponent's group of dots has a path to the board border that does not pass through the capturing player's dots, it cannot be captured.
- **Key tactic:** connecting your territory or dot-chain to the board border with an "impenetrable" (gap-free) link protects that group from ever being captured.

### 4.4 Territory State
- Intersections inside a captured territory are **locked** — no dot may be placed there.
- Dots inside a captured territory retain their original color (they are not removed).
- Territories are displayed filled with the capturing player's color/pattern.

### 4.5 Multi-capture
- It is possible for a single dot placement to trigger **multiple simultaneous captures**.
- All valid captures must be resolved in the same turn.

---

## 5. End of Game

The game ends when **either** of the following occurs:

1. **Surrender:** A player voluntarily concedes defeat.
2. **Board Full:** No legal intersections remain (all empty intersections are either occupied or inside existing territories).

---

## 6. Scoring

Two variants exist; the app must support both (configurable before game start).

### Variant A — Territory Area
- Count the number of intersections (dots + empty squares) inside each player's territories.
- The player with the **greater total territory area** wins.

### Variant B — Captured Dots
- Count only the **opponent's dots** captured inside each player's territories.
- The player who has captured **more opponent dots** wins.

### Ties
- If scores are equal, the game is a **draw** (can be configured to offer sudden-death or tiebreaker).

---

## 7. Tips & Strategy (Encode in Tutorial / AI Hints)

These are documented in the original source and should inform both the AI opponent and the in-game hint system:

### 7.1 Staircase Pattern
When building a defensive or offensive line of dots, use a **staircase (step) pattern** rather than a straight diagonal. A straight diagonal allows the opponent to "pass through" diagonally. A staircase pattern has no diagonal gaps, creating an impenetrable boundary.

- ❌ Straight diagonal: opponent can slip through at each diagonal step
- ✅ Staircase: each step overlaps adjacency, no gap possible

### 7.2 Border Anchoring
Connecting a chain to the board border makes it impossible for the opponent to surround it. This is a primary defensive maneuver.

### 7.3 Territory Timing
Closing (finalizing) a territory is optional until end of game. Premature closing can waste a move. Conversely, leaving a territory open risks forgetting it and the opponent may exploit the delay. Players should evaluate whether closing a territory now vs. playing elsewhere is worth more.

---

## 8. Rule Variants (To Be Supported as Settings)

| Variant | Description | Default |
|---------|-------------|---------|
| Dot placement | Intersections (nodes) vs. centers of squares | Intersections |
| Scoring | Territory area vs. captured dots | Territory area |
| Board size | Presets: Small (20×15), Medium (30×20), Large (60×40), Custom | Medium |
| First player | Player 1 always, random | Player 1 |
| Self-capture | Allow/disallow placing a dot that immediately gets captured | Disallow |

---

## 9. Formal Algorithm: Is a Dot Surrounded? (Flood Fill Method)

The recommended implementation approach for capture detection:

```
function checkCapture(board, placedDot, activePlayer):
    for each connected region R of (emptyIntersections ∪ opponentDots)
        adjacent to or containing at least one opponentDot:
        
        floodFill from any point in R:
            allowed moves: to adjacent intersections (4-connected for region expansion)
            blocked by: activePlayer's dots
        
        if floodFill does NOT reach the board border:
            → R is fully enclosed → CAPTURE
            mark all intersections in R as part of activePlayer's new territory
```

> Note: The flood-fill approach is simpler and more robust than explicitly tracing circuits, and is the recommended implementation path. The circuit-based description in the original rules is equivalent but harder to implement correctly for complex shapes.

---

## 10. Edge Cases to Handle

| Case | Expected Behavior |
|------|-------------------|
| A dot is placed that simultaneously closes two separate territories | Both territories captured in same turn |
| A territory is placed inside another territory (nested) | Inner territory is captured by outer's owner |
| A player's own dots are enclosed inside opponent's capture | They become part of opponent territory |
| A dot is placed on the border | Legal; border dots participate in circuits as normal |
| No opponent dots in a possible closure | **Cannot** close territory — must contain ≥1 opponent dot |
| Re-capture of opponent's territory | Legal — entire territory absorbed |
