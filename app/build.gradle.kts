plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val ciVersionCode = System.getenv("ZTE_VERSION_CODE")?.toIntOrNull()
val ciVersionName = System.getenv("ZTE_VERSION_NAME")
val stableStoreFile = System.getenv("ZTE_SIGNING_STORE_FILE")
val stableStorePassword = System.getenv("ZTE_SIGNING_STORE_PASSWORD")
val stableKeyAlias = System.getenv("ZTE_SIGNING_KEY_ALIAS")
val stableKeyPassword = System.getenv("ZTE_SIGNING_KEY_PASSWORD")
val hasStableSigning = listOf(
    stableStoreFile,
    stableStorePassword,
    stableKeyAlias,
    stableKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.malik.ztesmartmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.malik.ztesmartmanager"
        minSdk = 26
        targetSdk = 36
        // Every CI artifact gets a higher code so Android/HONOR can treat it as an upgrade.
        // UI sizing is runtime-adaptive; Honor 200 is handled by the tall-display dashboard policy.
        versionCode = ciVersionCode ?: 36
        versionName = ciVersionName ?: "0.5.8"
    }

    if (hasStableSigning) {
        signingConfigs {
            create("stableUpdate") {
                storeFile = file(stableStoreFile!!)
                storePassword = stableStorePassword
                keyAlias = stableKeyAlias
                keyPassword = stableKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (hasStableSigning) signingConfig = signingConfigs.getByName("stableUpdate")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = if (hasStableSigning) signingConfigs.getByName("stableUpdate") else null
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
