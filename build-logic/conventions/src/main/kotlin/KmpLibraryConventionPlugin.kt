@file:Suppress("UnstableApiUsage", "unused")

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()

        val androidTarget = (kotlin as ExtensionAware).extensions
            .getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
        androidTarget.apply {
            namespace = "com.dept.markets." + path.removePrefix(":").replace(':', '.').replace('-', '_')
            compileSdk = catalog.findVersion("androidCompileSdk").get().toString().toInt()
            minSdk = catalog.findVersion("androidMinSdk").get().toString().toInt()
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }

        with(kotlin) {
            jvm("desktop") {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
            iosArm64()
            iosSimulatorArm64()

            sourceSets.all {
                languageSettings.apply {
                    optIn("kotlin.experimental.ExperimentalObjCName")
                    optIn("kotlin.experimental.ExperimentalObjCRefinement")
                    optIn("kotlinx.cinterop.ExperimentalForeignApi")
                    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    optIn("kotlin.time.ExperimentalTime")
                }
            }

            compilerOptions {
                extraWarnings.set(false)
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
}
