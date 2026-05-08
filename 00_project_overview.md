# Territories — Project Overview

## What Is This Game?

**Territories** is a digital adaptation of **Židi** (also known as: *židy, obkličovačka, obklíčovaná, území, hradby, kasárny, tečkovaná, puntíkovaná*) — a classic Czech pen-and-paper strategy game for 2 players. The game is likely a descendant of the Chinese game of Go, simplified for play on grid paper without any physical components.

The game is a pure-strategy, zero-randomness game of encirclement and territorial control.

---

## Platform Rollout Order

| Priority | Platform     | Status     |
|----------|--------------|------------|
| 1        | Android      | Primary target |
| 2        | Windows      | Desktop port |
| 3        | Web (browser)| Broad reach |
| 4        | Linux        | Desktop port |
| 5        | Apple (iOS + macOS) | Final port |

---

## Core Concept Summary

- Players alternate placing dots on intersections of a square grid
- A player captures territory by forming a closed loop of their own dots around one or more opponent dots
- Captured dots and the enclosed area become the capturing player's territory
- Captured territory cannot be played into, but can itself be re-captured
- Dots cannot be placed on the wall/border (or: territory connected to the wall cannot be captured)
- The game ends by surrender or when no legal moves remain
- Winner is determined either by total territory area **or** total captured opponent dots (two scoring variants)

---

## Project Name

**Territories** — chosen as the English equivalent of the Czech *území* (one of the game's alternative names). Clean, descriptive, globally understandable.

---

## Planning Document Index

| File | Description |
|------|-------------|
| `00_project_overview.md` | This file — master summary |
| `01_game_rules_specification.md` | Formal, implementation-ready rules |
| `02_core_game_engine.md` | Game logic engine design and tasks |
| `03_data_models.md` | Data structures and state representation |
| `04_android_implementation.md` | Android-first build plan (Kotlin + Jetpack Compose) |
| `05_cross_platform_strategy.md` | Cross-platform approach and porting plan |
| `06_ai_opponent.md` | AI / bot opponent design |
| `07_ui_ux_guidelines.md` | UI/UX principles and screen-by-screen specs |

---

## Technology Decisions (Top Level)

- **Language:** Kotlin (Android primary), with a Kotlin Multiplatform (KMP) path for future ports
- **Android UI:** Jetpack Compose
- **Game engine:** Pure Kotlin module — no framework dependency, fully testable
- **State management:** Immutable game state + event-driven updates
- **AI:** Minimax with alpha-beta pruning (Phase 1), expandable to MCTS
- **Persistence:** Room (local game save), no server required for offline play
- **Multiplayer (future):** Pass-and-play first, then local Wi-Fi, then online

---

## Non-Goals (v1.0)

- Online multiplayer
- Accounts / leaderboards
- Variants for 3+ players
- Animated tutorials (text rules only in v1)
- Monetization / ads
