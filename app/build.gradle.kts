plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.malik.ztesmartmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.malik.ztesmartmanager"
        minSdk = 26
        targetSdk = 36
        versionCode = 25
        versionName = "0.3.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")

    debugImplementation(libs.androidx.compose.ui.tooling)
}
