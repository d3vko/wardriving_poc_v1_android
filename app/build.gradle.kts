plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

val dotEnv = loadDotEnv(rootProject.file(".env"))

android {
    namespace = "com.d3vk0.wardriving.rf.village.mx"
    compileSdk = 34
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.d3vk0.wardriving.rf.village.mx"
        minSdk = 26
        targetSdk = 34
        versionCode = configInt("APP_VERSION_CODE", 1)
        versionName = configValue("APP_VERSION_NAME", "0.0.2")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", configString("API_BASE_URL", "https://api.example.invalid/"))
        buildConfigField("String", "API_LOGIN_PATH", configString("API_LOGIN_PATH", "auth/login"))
        buildConfigField("String", "API_REGISTER_PATH", configString("API_REGISTER_PATH", "auth/register"))
        buildConfigField("String", "API_PASSWORD_RECOVERY_PATH", configString("API_PASSWORD_RECOVERY_PATH", "auth/password-recovery"))
        buildConfigField("String", "API_UPLOAD_PATH", configString("API_UPLOAD_PATH", "files-uploaded/"))
        buildConfigField("String", "API_UPLOAD_TYPE_WIFI_BLE", configString("API_UPLOAD_TYPE_WIFI_BLE", "wifi_ble"))
        buildConfigField("String", "API_UPLOAD_TYPE_LTE", configString("API_UPLOAD_TYPE_LTE", "lte"))
        buildConfigField("String", "APP_ACCENT_COLOR", configString("APP_ACCENT_COLOR", "#00A676"))
        buildConfigField("String", "MAPLIBRE_STYLE_URL", configString("MAPLIBRE_STYLE_URL", "https://demotiles.maplibre.org/style.json"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    @Suppress("DEPRECATION")
    applicationVariants.configureEach {
        val apkBasename = configValue("APP_APK_BASENAME", "wardriving")
        val outputName = when (buildType.name) {
            "release" -> "$apkBasename-release.apk"
            "debug" -> "$apkBasename.apk"
            else -> "$apkBasename-${buildType.name}.apk"
        }
        outputs.configureEach {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = outputName
        }
    }
}

fun configValue(name: String, defaultValue: String): String {
    return providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: dotEnv[name]
        ?: defaultValue
}

fun configInt(name: String, defaultValue: Int): Int =
    configValue(name, defaultValue.toString()).toIntOrNull() ?: defaultValue

fun configString(name: String, defaultValue: String): String {
    val value = configValue(name, defaultValue)
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

fun loadDotEnv(file: File): Map<String, String> {
    if (!file.isFile) return emptyMap()
    return file.readLines()
        .mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
            val separator = trimmed.indexOf('=')
            if (separator <= 0) return@mapNotNull null
            val key = trimmed.substring(0, separator).trim()
            val rawValue = trimmed.substring(separator + 1).trim()
            key to rawValue.trimMatchingQuotes()
        }
        .toMap()
}

fun String.trimMatchingQuotes(): String {
    return when {
        length >= 2 && first() == '"' && last() == '"' -> substring(1, length - 1)
        length >= 2 && first() == '\'' && last() == '\'' -> substring(1, length - 1)
        else -> this
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.location)
    implementation(libs.maplibre.android.sdk)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
