import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

/**
 * The desktop target is not a product requirement — it is the fastest feedback loop the
 * team has. `./gradlew :app:desktop:run` launches the real screen against the real feed
 * in about two seconds, with no emulator and no Xcode, which is where most UI and
 * performance work should happen.
 */
compose.desktop {
    application {
        mainClass = "com.dept.markets.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "MarketInsights"
            packageVersion = "1.0.0"
        }
    }
}
