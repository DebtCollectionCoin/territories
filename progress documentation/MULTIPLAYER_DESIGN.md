# Multiplayer & Ranking Design — 1v1v1v1 Free-for-all

Status: **draft, awaiting approval before coding**.

This document specifies the design for adding 4-player free-for-all with
a TrueSkill-based ranking ladder to Territories. It supersedes the
"online multiplayer is a non-goal" line in [00_project_overview.md](../00_project_overview.md)
for the v1.x roadmap.

Three topologies are in scope:

1. **Local hot-seat** — 2–4 humans on the same device, passing it
   around. Ships first because it needs only engine + UI changes, no
   server.
2. **Online real-time** — 2–4 humans matched via a lobby on a Ktor
   server, low-latency turn play (one move per few seconds).
3. **Online async / correspondence** — same backend, but turns can be
   hours or days apart; players get a notification when it's their turn.

Ranking only applies to the two online modes. Hot-seat is unranked.

---

## 1. Game-rule changes for N players

The current rules in [01_game_rules_specification.md](../01_game_rules_specification.md)
assume exactly two players. For N ∈ {2, 3, 4}:

### 1.1 Turn order
- Random fixed order at game start: `[P1, P2, P3, P4]` (FFA seat
  permutation drawn once and saved).
- Turns rotate clockwise through the seat list. A player who has
  resigned or been eliminated is **skipped** in the rotation.
- A player is **eliminated** if all remaining empty cells are illegal
  for them under standard placement rules. The game continues among
  survivors. If only one survivor remains they are awarded the lowest
  per-cell score for all remaining empty cells of any colour they
  surround.

### 1.2 Capture rules
The flood-fill capture rule generalises naturally:
- A capture happens when a region of empty cells is fully surrounded
  on its 4-connected interior by stones of **a single non-mover
  colour**, and the move closing the surround is by that colour's
  *opponent in any direction* (i.e. any other player).
- Multi-colour borders **never capture** — this preserves the existing
  invariant that capture requires single-colour encirclement.
- Self-capture remains illegal (`config.allowSelfCapture` stays).
- A move that simultaneously closes two regions captures both.

### 1.3 Scoring
Two variants kept; both extended trivially per-player:
- **TERRITORY_AREA**: each player's score is the count of empty cells
  whose surrounding stones are all theirs (existing `Territory`
  detection runs once per player).
- **CAPTURED_DOTS**: each player's score is the count of opponent
  stones they have captured. Total captures stays a flat integer per
  player.

Final ranking within a single game is by score descending; ties broken
by (a) fewer eliminations against you, (b) earlier elimination of
others, (c) coin flip.

### 1.4 Board sizes for FFA
4-player games on a 21×16 board feel cramped. Shipping presets:

| Players | SMALL    | MEDIUM   | LARGE    |
|---------|----------|----------|----------|
| 2       | 21×16    | 31×21    | 61×41    |
| 3       | 25×19    | 35×25    | 61×41    |
| 4       | 29×21    | 41×29    | 61×41    |

`GameConfig` gains a `playerCount: Int` field; presets adjust as above.

### 1.5 End-game conditions
Game ends when **either**:
- Only one player has any legal move, **or**
- All non-eliminated players pass consecutively, **or**
- The move counter reaches a per-board-size cap (failsafe; same as
  today).

---

## 2. Engine refactor

### 2.1 `Player` enum → `Player` value class

```kotlin
@JvmInline
@Serializable
value class Player(val id: Byte) {
    companion object {
        val NONE = Player(0)
        val A    = Player(1)
        val B    = Player(2)
        val C    = Player(3)
        val D    = Player(4)
        val ALL  = listOf(A, B, C, D)
    }
}
```

`opponent()` is dropped (not meaningful for N>2). Code that needed it
for AI heuristics is replaced with `othersOf(currentPlayer)` returning
the active non-mover seats.

### 2.2 Cell encoding
`Cell` already stores `dot: Player` and `territory: Player`. Both
become `Byte` automatically via the value class. Capture detector and
score calculator are unchanged structurally — all loops over `Player.A`
and `Player.B` become loops over `gameState.activePlayers`.

### 2.3 GameState additions
```kotlin
data class GameState(
    ...
    val players: List<Player>,           // seat order, length 2-4
    val eliminated: Set<Player>,         // accumulated eliminations
    val passes: Int,                     // consecutive passes
    val score: Map<Player, Int>          // was Score(playerA, playerB)
)
```

Backwards compatibility: existing 2-player saves (`#g=...` URL share,
desktop save file, Android Room) are migrated by a one-shot reader that
maps `Player.A/B` to seat 1/2 and sets `players = [A, B]`.

### 2.4 Migration of existing tests
The existing 90+ engine tests assume 2 players; they continue to pass
because `Player.A/B` and 2-player default behaviour are preserved. New
tests cover 3-player and 4-player capture and elimination scenarios.

---

## 3. AI for FFA

Hard AI today is minimax; minimax does not generalise to N>2. Options:

- **Paranoid minimax**: AI assumes all other players collude against it.
  Pessimistic but cheap. Ship as the FFA Hard AI.
- **Max^N**: each player maximises their own score; tuple-valued
  evaluation. Stronger but 4× slower.

Decision: **paranoid minimax** for v1.x FFA, max^N revisited if Hard AI
plays too defensively in playtesting. Easy/Medium AIs need only the
heuristic (`oppDepth Σ` becomes `Σ over all others`) and work
unchanged.

---

## 4. Networking

### 4.1 Server tech
- **Ktor 2.x** server with WebSocket endpoints.
- **Postgres 16** for users, games, ratings, completed-game archive.
- **Redis** (optional, phase 2) for live-game state cache + pub/sub
  fanout.
- The server reuses the **`:engine`** module directly — same JVM
  artefact as the client. The server is therefore authoritative on
  rules.

### 4.2 Wire protocol
WebSocket frames are JSON, schema lives in a new `:protocol` KMP
module shared by client and server.

```
C→S  Hello { authToken, clientVersion }
S→C  Welcome { userId, displayName, ratingMu, ratingSigma }
C→S  CreateLobby { config: GameConfig, ranked: Boolean }
C→S  JoinLobby { lobbyId }
S→C  LobbyState { lobbyId, players, ready, config }
S→C  GameStarted { gameId, seatOrder, initialState }
C→S  SubmitMove { gameId, move, expectedMoveCount }
S→C  MoveApplied { gameId, move, newState, nextPlayer, deadlineMs }
S→C  PlayerEliminated { gameId, player, finalScore }
S→C  GameEnded { gameId, finalScores, ratingDelta[] }
C→S  Resign { gameId }
C→S  Heartbeat
```

`expectedMoveCount` lets the server reject stale moves on lag. Server
is fully authoritative; client renders optimistically and reverts on
rejection.

### 4.3 Real-time vs async
Same protocol. Difference is the **per-move deadline**:

- Real-time: 30 s per move. Missed deadline → auto-resign.
- Async: 24 h per move. Notification (web push / email) on turn start.
  Missed deadline → auto-resign after 48 h.

Both modes share the lobby and matchmaking pipeline.

### 4.4 Reconnect / resync
On reconnect, client sends `Resume { gameId, lastKnownMoveCount }`.
Server replies with the moves the client missed. If the client's hash
of `(initialState, moves[])` doesn't match the server's, server pushes
a full state snapshot.

### 4.5 Hot-seat
No server. The existing `:session` module's `LocalGameSession` is
extended to rotate through `state.players` instead of toggling A↔B.
Same `submitMove(...)` API; the UI just hands the device to whichever
player's seat is current.

---

## 5. TrueSkill ranking

### 5.1 Why TrueSkill
Elo is well-defined only pairwise. TrueSkill is the standard for FFA
games (Halo, Forza). It models each player's skill as `N(μ, σ²)` and
updates after each game using the observed finishing order.

### 5.2 Defaults
- New player: `μ = 25, σ = 25/3 ≈ 8.333`.
- Displayed conservative rating: `μ - 3σ`. Caps at 0 below.
- After each ranked game (any number of survivors), apply the
  TrueSkill update for the observed finishing order. Draws between
  seats with equal final score are supported by the standard
  TrueSkill draw formulation.

### 5.3 Implementation
- Pure-Kotlin port of the TrueSkill update equations into a new module
  `:ranking` (commonMain). About 300 LOC; reference impl from the
  Microsoft Research paper. We don't need the team-vs-team general
  case, only the FFA case which is much simpler.
- Server runs the update inside the same Postgres transaction that
  archives the finished game.
- Client receives `ratingDelta` in the `GameEnded` frame and animates
  the new rating on the result screen.

### 5.4 Anti-abuse
- Ranked games require both players in the match to be authenticated.
- Concurrency limit: a player can be in **at most 2** ranked games at
  once (to prevent rating-farm sock-puppeting via parallel matches).
- Resignation in a ranked game counts as last-place finish.
- Repeated resignations (>30% over rolling 20 games) flag the account
  for shadow-only matchmaking.

---

## 6. Database schema (initial)

```sql
-- Postgres 16
create table users (
    id           uuid primary key,
    display_name text unique not null,
    auth_token_hash text not null,
    created_at   timestamptz not null default now()
);

create table ratings (
    user_id      uuid primary key references users(id),
    mu           double precision not null,
    sigma        double precision not null,
    games        integer not null default 0,
    updated_at   timestamptz not null default now()
);

create table games (
    id           uuid primary key,
    config_json  jsonb not null,
    ranked       boolean not null,
    started_at   timestamptz not null,
    ended_at     timestamptz,
    final_state_json jsonb,
    moves_json   jsonb not null  -- array of moves; replayable from config + moves
);

create table game_seats (
    game_id      uuid not null references games(id) on delete cascade,
    seat_index   smallint not null,
    user_id      uuid references users(id),  -- null for AI
    final_rank   smallint,                   -- 1=winner; null=in-progress
    final_score  integer,
    rating_mu_before    double precision,
    rating_sigma_before double precision,
    rating_mu_after     double precision,
    rating_sigma_after  double precision,
    primary key (game_id, seat_index)
);

create index on games (ended_at desc) where ranked;
create index on game_seats (user_id, game_id);
```

A finished game is fully replayable from `(config_json, moves_json)` —
the engine guarantees determinism.

---

## 7. Phased roadmap

Each phase ends with a release tag. Phases are sized so each is
meaningful on its own.

### Phase A — N-player engine (offline only) — *v0.3.0*
- Refactor `Player` to value class with seats A-D.
- Generalise `GameState`, `CaptureDetector`, `ScoreCalculator`,
  `GameOverDetector` to N players.
- Update Easy/Medium AI heuristic to sum over all opponents.
- Save-file migration from old 2-player saves.
- Engine tests pass; new 3- and 4-player capture / elimination tests
  added.
- **No UI changes yet.** All apps continue running 2-player.

### Phase B — Local hot-seat 4-player UI — *v0.4.0*
- Setup screen: pick 2/3/4 players, each one HUMAN/AI/empty.
- Board canvas: render up to 4 stone colours (palette extended in
  `:shared-ui`).
- HUD shows current seat + score for all players.
- Result screen ranks all players and shows the seat permutation.

### Phase C — Ranking module — *v0.5.0*
- New `:ranking` module with TrueSkill FFA update + tests.
- Hooked into local AI games as a personal ladder (your μ vs the AI's
  fixed μ). No server yet; rating stored locally.

### Phase D — Server skeleton — *v0.6.0*
- Ktor server with `/auth`, `/lobby`, `/game/{id}` WebSockets.
- Postgres schema applied via Flyway migrations.
- One end-to-end happy-path: two clients log in, create a 2-player
  lobby, play a real-time ranked game, ratings update.
- Server reuses `:engine` and `:ranking`.

### Phase E — Online real-time 4-player + matchmaking — *v0.7.0*
- Lobby browser, quick-match button (matches by rating proximity).
- Reconnect-and-resync working under flaky networks.
- 30-s-per-move deadlines enforced.

### Phase F — Async correspondence — *v0.8.0*
- 24-h-per-move deadlines.
- Web push notification on turn start (PWA) and email fallback.
- "My active games" screen on each client surface.

### Phase G — Polish + leaderboards — *v1.0.0*
- Public leaderboard (top 100 by conservative rating, min 30 games).
- Profile pages (rating history graph, game archive).
- Cross-platform leaderboard widgets on home screens.

---

## 8. Out of scope (still)

- Teams (2v2, 3v1). Always free-for-all.
- Player accounts on social-platform OAuth (Google/Apple). Phase 1
  uses email + magic-link auth via Ktor; OAuth deferred.
- Replays viewer with seek-bar UI. We have replay data via
  `moves_json`; a viewer is a v1.1+ thing.
- Voice/text chat. Risk-to-effort ratio is poor and we'd inherit a
  moderation problem.
- Spectator mode. Easy to add later via the existing WebSocket;
  deferred.

---

## 9. Open questions

1. Auth method for v1.0: **email + magic-link** (no passwords,
   minimal PII), or also OAuth?
2. Rate-limiting per IP and per account: what's the budget for the
   first deployment? Default proposal: 60 ws frames/min/account, 600
   /min/IP.
3. Hosting target: bare VPS (Hetzner / Fly.io) vs managed
   container (Railway / Render). Current proposal: Fly.io, since the
   game is latency-sensitive in real-time mode and Fly.io regions
   reduce RTT.
4. Server-side AI fill for empty FFA seats — yes/no? Default proposal:
   yes, "fill-with-AI" toggle in lobby owner settings, AI seats are
   unranked.

Answers to these block implementation of phases D and onward but not
phases A-C.
