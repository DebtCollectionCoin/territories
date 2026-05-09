# `shared-ui` migration plan

This document is the next-step plan for consolidating the duplicated
Compose UI between [`app-android`](../app-android/) and
[`app-desktop`](../app-desktop/) into a Compose Multiplatform
`shared-ui` module. The work is **deliberately deferred** until v1.0
because:

- The Android and Desktop modules currently use intentionally different
  palettes (Android: cream/light, Desktop: charcoal/dark).
- The Android `BoardCanvas` has six features the Desktop one lacks
  (pan/zoom, capture ripple, color-blind shapes, accessibility
  semantics, animated territory fade, second-player-shape variant).
- Verifying any cross-target Compose Multiplatform refactor needs a
  working Android SDK + emulator on the same machine. Drive-by changes
  without that ability would risk shipping a broken Android build.

## Recommended structure when resuming

```
shared-ui/
├── build.gradle.kts                # KMP + compose-multiplatform + android-library
└── src/
    ├── commonMain/kotlin/territories/sharedui/
    │   ├── Palette.kt              # Palette data class + LightPalette / DarkPalette presets
    │   ├── BoardCanvas.kt          # Stateless drawing of board, grid, stones, last-move ring
    │   └── BoardSemantics.kt       # buildBoardDescription helper (already in Android)
    ├── androidMain/kotlin/territories/sharedui/
    │   └── BoardCanvasA11y.kt      # liveRegion + colorBlindMode shape variants
    └── jvmMain/kotlin/territories/sharedui/
        └── BoardCanvasDesktop.kt   # any desktop-specific input handling
```

## Build wiring

Add to [`settings.gradle.kts`](../settings.gradle.kts):
```kotlin
include(":engine", ":session", ":app-web", ":app-android", ":app-desktop", ":shared-ui")
```

`shared-ui/build.gradle.kts` skeleton:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()
    jvm("desktop")
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(project(":engine"))
            }
        }
    }
}

android {
    namespace = "territories.sharedui"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}
```

## Migration steps (do in this order)

1. **Palette** (~30 LOC). Create `Palette` data class with all 12 colour
   fields. Move `app-android` literals into `LightPalette` and
   `app-desktop`'s `AppColors` into `DarkPalette`. Both apps import the
   palette of their choice. **Verify both builds compile.**

2. **`buildBoardDescription`**. Already pure logic in Android. Move to
   `commonMain`. Desktop can opt in for accessibility parity later.

3. **Pure board drawing**. Move grid + territory fill + stone +
   last-move ring drawing into a `commonMain` `drawBoard(state, palette)`
   `DrawScope` extension. Both apps wrap this in their own gesture +
   pan/zoom + ripple layers.

4. **`BoardCanvas`** as a single composable. Last and most invasive
   step. Accepts `palette`, `colorBlindMode`, `enablePanZoom`,
   `enableLiveRegionA11y`, optional capture-ripple state. Replaces both
   apps' files with a one-line wrapper.

5. **Theme** (Material3 colour scheme). Different colours per platform
   so this is shared *structurally* (a single `AppColorScheme` data
   class) but each app builds its own instance. Lower priority.

## Verification before merge

Each step should be followed by:

```bash
./gradlew :app-android:assembleDebug
./gradlew :app-desktop:run     # smoke check
./gradlew :app-web:jsBrowserDevelopmentExecutableDistribution
./gradlew :engine:jvmTest :session:jvmTest
```

A regression in any of those means the step needs to be split smaller.

## What this is **not**

- Not a path to sharing the **web** UI. The web app is hand-rolled HTML
  + Canvas 2D and would need a separate JS Canvas renderer or a full
  Compose Multiplatform JS target adoption (heavy and currently
  experimental for canvas). Deferred indefinitely.

- Not a Hilt/AppViewModel consolidation. The Android app's DI graph
  uses Hilt; Desktop uses plain coroutines. Don't merge them.

- Not a navigation graph consolidation. Compose Navigation has very
  different artifacts on Android vs Desktop and the screens are small
  enough that duplication is cheaper than the abstraction.
