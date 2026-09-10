plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val ciVersionCode = System.getenv("ZTE_VERSION_CODE")?.toIntOrNull()
val ciVersionName = System.getenv("ZTE_VERSION_NAME")
val ciDebugStoreFile = System.getenv("ZTE_CI_DEBUG_STORE_FILE")
val ciDebugStorePassword = System.getenv("ZTE_CI_DEBUG_STORE_PASSWORD")
val ciDebugKeyAlias = System.getenv("ZTE_CI_DEBUG_KEY_ALIAS")
val ciDebugKeyPassword = System.getenv("ZTE_CI_DEBUG_KEY_PASSWORD")
val hasStableCiDebugSigning = listOf(
    ciDebugStoreFile,
    ciDebugStorePassword,
    ciDebugKeyAlias,
    ciDebugKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.malik.ztesmartmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.malik.ztesmartmanager"
        minSdk = 26
        targetSdk = 36
        // Local builds keep a sane base code. CI injects a monotonically increasing code so
        // Honor/Android Package Installer always sees a newer package instead of a same-code APK.
        versionCode = ciVersionCode ?: 36
        versionName = ciVersionName ?: "0.5.8"
    }

    if (hasStableCiDebugSigning) {
        signingConfigs {
            create("stableCiDebug") {
                storeFile = file(ciDebugStoreFile!!)
                storePassword = ciDebugStorePassword
                keyAlias = ciDebugKeyAlias
                keyPassword = ciDebugKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasStableCiDebugSigning) {
                signingConfig = signingConfigs.getByName("stableCiDebug")
            }
        }
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
