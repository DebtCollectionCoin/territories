plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":engine"))
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                // Ktor deps added here when LanGameSession / OnlineGameSession are implemented:
                // implementation(libs.ktor.client.core)
                // implementation(libs.ktor.client.websockets)
                // implementation(libs.ktor.client.content.negotiation)
                // implementation(libs.ktor.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting
        // val jvmMain by getting { dependencies { implementation(libs.ktor.client.cio) } }
        val jsMain by getting
        // val jsMain by getting { dependencies { implementation(libs.ktor.client.js) } }
    }
}
