plugins {
    id("buildlogic.plugins.android.application")
}

android {
    namespace = "com.dept.markets.android"
    defaultConfig {
        applicationId = "com.dept.markets.android"
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(compose.runtime)
    implementation(compose.ui)
    debugImplementation(compose.uiTooling)
}
