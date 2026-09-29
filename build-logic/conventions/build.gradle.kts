import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "buildlogic.plugins"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.gradle.android.tools)
    implementation(libs.gradle.kotlin)
    implementation(libs.gradle.compose.multiplatform)
    implementation(libs.gradle.compose.compiler)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "buildlogic.plugins.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("cmpLibrary") {
            id = "buildlogic.plugins.kmp.compose"
            implementationClass = "CmpLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "buildlogic.plugins.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
