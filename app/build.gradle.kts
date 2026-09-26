import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serialization)
}

// Gemini API key: local.properties (git-ignored) wins, environment is the fallback.
// Empty means unconfigured — the app keeps working with Mock AI.
val localProperties = Properties().also { props ->
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(props::load)
}
val geminiApiKey: String =
    (localProperties.getProperty("GEMINI_API_KEY") ?: System.getenv("GEMINI_API_KEY"))
        .orEmpty()
        .replace("\"", "")
val openRouterApiKey: String =
    (localProperties.getProperty("OPENROUTER_API_KEY") ?: System.getenv("OPENROUTER_API_KEY"))
        .orEmpty()
        .replace("\"", "")
val tavilyApiKey: String =
    (localProperties.getProperty("TAVILY_API_KEY") ?: System.getenv("TAVILY_API_KEY"))
        .orEmpty()
        .replace("\"", "")

// Release signing: keystore.properties (git-ignored, see
// keystore.properties.example) wins when it points at an existing keystore.
// Without it, release falls back to the debug key (internal testing only;
// a public APK must always be signed with the real release keystore,
// otherwise later updates with a different signature won't install).
val keystoreProperties = Properties().also { props ->
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(props::load)
}
val publicKeystoreFile = keystoreProperties.getProperty("storeFile")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.exists() }

android {
    namespace = "com.curio.notes"
    // AndroidX releases used here (Compose BOM 2026.09.00, core-ktx 1.19.1,
    // navigation 2.10.2, lifecycle 2.11.0, room 2.8.5) require compileSdk 37+.
    // targetSdk stays 36; minSdk 26 covers modern devices.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.curio.notes"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.0"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterApiKey\"")
        buildConfigField("String", "TAVILY_API_KEY", "\"$tavilyApiKey\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (publicKeystoreFile != null) {
            create("pubRelease") {
                storeFile = publicKeystoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Real keystore when configured, debug key otherwise (internal only).
            signingConfig = publicKeystoreFile?.let { signingConfigs.getByName("pubRelease") }
                ?: signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Built-in Kotlin (AGP 9+): jvmTarget defaults to compileOptions.targetCompatibility,
// so no explicit kotlin.compilerOptions block is needed.

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.okhttp)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
