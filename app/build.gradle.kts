import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties: Properties? = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use(::load) } }

/** local.properties (gitignored) first, then the environment — that is how CI passes secrets in. */
fun secret(name: String): String? =
    localProperties?.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }

// Developer default for the DeepSeek key: `DEEPSEEK_API_KEY=sk-...` in local.properties,
// or the environment variable of the same name for a one-off build.
// The user can always override it from 我的 → AI 洞察.
val deepSeekApiKey: String = secret("DEEPSEEK_API_KEY").orEmpty()

// Tag-driven versioning: the release workflow passes -PversionName=0.2.0 -PversionCode=<run>.
// A plain local build keeps the developer defaults below.
val appVersionName: String = (findProperty("versionName") as String?) ?: "0.1.0"
val appVersionCode: Int = (findProperty("versionCode") as String?)?.toInt() ?: 1

/**
 * Signing for the APK published to GitHub Releases. Every build must be signed with the
 * *same* key or the phone refuses to install the update over the one it has — so the
 * keystore lives outside the repo (`~/keys/moodiary-release.jks` locally, a base64
 * secret decoded to a file in CI) and is pointed at by MOODIARY_KEYSTORE.
 * Without it the release build is simply unsigned, as before.
 */
val keystoreFile: File? = secret("MOODIARY_KEYSTORE")?.let(::File)?.takeIf { it.exists() }

android {
    namespace = "com.moodiary.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.moodiary.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepSeekApiKey\"")
        // 版本更新 asks this repo's GitHub Releases what the newest build is.
        buildConfigField("String", "UPDATE_REPO", "\"ulichirock-cmyk/moo-diary-android\"")
    }

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = keystoreFile
                storePassword = secret("MOODIARY_KEYSTORE_PASSWORD")
                keyAlias = secret("MOODIARY_KEY_ALIAS") ?: "moodiary"
                keyPassword = secret("MOODIARY_KEY_PASSWORD") ?: secret("MOODIARY_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)
}
