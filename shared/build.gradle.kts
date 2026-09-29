import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("buildlogic.plugins.kmp.library")
    id("buildlogic.plugins.kmp.compose")
}

kotlin {
    // One framework, assembled from every shared module. iOS integrates against this
    // single binary (see iosApp/Package.swift) instead of N frameworks, which keeps
    // Swift imports and the Xcode build graph trivial.
    val xcf = XCFramework("MarketKit")
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "MarketKit"
            // Static: no dynamic-linking cost at launch and no dSYM juggling. The
            // trade-off (framework rebuilt into every consumer) is irrelevant with one app.
            isStatic = true
            // `export` is what makes the domain types visible to Swift with their real
            // names instead of as opaque `KotlinBase` instances.
            export(projects.core.model)
            export(projects.core.domain)
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.domain)
            api(projects.feature.marketinsights)
            implementation(projects.core.designsystem)
            implementation(projects.data.market)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
