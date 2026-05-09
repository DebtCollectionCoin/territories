plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotest.assertions)
            }
        }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner)
            }
        }
        val jsMain by getting
        val jsTest by getting
    }
}

tasks.register<JavaExec>("runSimulation") {
    group = "verification"
    description = "Plays Medium AI vs Medium AI and prints move-by-move breakdown"
    dependsOn("jvmMainClasses")
    classpath = kotlin.jvm().compilations["main"].output.allOutputs +
                kotlin.jvm().compilations["main"].runtimeDependencyFiles
    mainClass.set("territories.engine.ai.SimulationKt")
}

tasks.register<JavaExec>("runBenchmark") {
    group = "verification"
    description = "Runs N AI vs AI games, summarises captures / score / draws"
    dependsOn("jvmMainClasses")
    classpath = kotlin.jvm().compilations["main"].output.allOutputs +
                kotlin.jvm().compilations["main"].runtimeDependencyFiles
    mainClass.set("territories.engine.ai.AiBenchmarkKt")
    // Args forwarded from -Pbench.args="..."
    val benchArgs = (project.findProperty("bench.args") as String?)?.split(" ") ?: emptyList()
    args = benchArgs
}
