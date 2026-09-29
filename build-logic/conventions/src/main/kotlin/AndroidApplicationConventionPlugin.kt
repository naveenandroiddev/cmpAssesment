@file:Suppress("unused")

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Android host app: no business logic lives here, only wiring and manifest. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.compose")

        val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
        extensions.getByType<ApplicationExtension>().apply {
            compileSdk = catalog.findVersion("androidCompileSdk").get().toString().toInt()
            defaultConfig {
                minSdk = catalog.findVersion("androidMinSdk").get().toString().toInt()
                targetSdk = catalog.findVersion("androidTargetSdk").get().toString().toInt()
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
