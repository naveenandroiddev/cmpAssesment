@file:Suppress("unused")

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
class CmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.compose")

        val compose = extensions.getByType<ComposeCompilerGradlePluginExtension>()
        compose.stabilityConfigurationFiles.add(
            isolated.rootProject.projectDirectory.file("compose-stability.conf"),
        )

        val reportsEnabled = providers.gradleProperty("enableComposeCompilerReports")
            .map { it.toBoolean() }
            .getOrElse(false)
        if (reportsEnabled) {
            val dir = layout.buildDirectory.dir("compose-reports")
            compose.reportsDestination.set(dir)
            compose.metricsDestination.set(dir)
        }
    }
}
