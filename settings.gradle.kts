pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "territories"
include(":engine", ":session", ":app-web", ":app-android", ":app-desktop", ":shared-ui")
