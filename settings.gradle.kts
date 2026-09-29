@file:Suppress("UnstableApiUsage")

pluginManagement {
    // Convention plugins live in a composite build so they are compiled once and
    // shared by every module. Modules never repeat target/compiler configuration.
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // Modules may not declare their own repositories: one resolution surface,
    // auditable for a regulated (capital markets) supply chain.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "MarketInsights"

// ---- Core: platform-agnostic, no feature knowledge -------------------------
include(":core:model")
include(":core:domain")
include(":core:designsystem")

// ---- Data: implements core:domain contracts --------------------------------
include(":data:market")

// ---- Feature: one vertical slice, UI + presentation -------------------------
include(":feature:marketinsights")

// ---- Shared umbrella consumed by iOS as an XCFramework ----------------------
include(":shared")

// ---- Apps: thin platform hosts ---------------------------------------------
include(":app:android")
include(":app:desktop")
