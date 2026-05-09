plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // Use the JVM running Gradle — no toolchain auto-detection needed
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":session"))
    implementation(project(":shared-ui"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}

compose.desktop {
    application {
        mainClass = "territories.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "territories"
            packageVersion = "1.0.0"
            description = "Territories — a strategy game of encirclement"
            vendor = "Territories"
            copyright = "© 2026 Territories"

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
                menuGroup = "Games"
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "Games"
                shortcut = true
                dirChooser = true
                upgradeUuid = "A3F9B1C2-4D7E-4A8F-9B2D-1C3E5A7F9B0D"
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
                bundleID = "territories.desktop"
            }
        }

        buildTypes.release.proguard {
            isEnabled = false   // keep disabled until rules are tuned
        }
    }
}
