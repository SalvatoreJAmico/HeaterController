import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.salvatore.heatercontroller"
    compileSdk {
        version = release(36)
    }

    // Read API keys once so we can inject per buildType
    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) load(f.inputStream())
    }
    val baseApiKey = localProps.getProperty("API_KEY")
        ?: System.getenv("API_KEY")
        ?: ""

    // Load signing configuration from signing.properties (at project root)
    val signingPropsFile = rootProject.file("signing.properties")
    val signingProps = Properties().apply {
        if (signingPropsFile.exists()) load(signingPropsFile.inputStream())
    }

    signingConfigs {
        create("release") {
            val sf = signingProps.getProperty("storeFile") ?: ""
            if (sf.isNotBlank()) {
                // Resolve relative to the root project to allow paths like "app/keystore/..."
                storeFile = rootProject.file(sf)
            }
            storePassword = signingProps.getProperty("storePassword")
            keyAlias = signingProps.getProperty("keyAlias")
            keyPassword = signingProps.getProperty("keyPassword")
        }
    }

    defaultConfig {
        applicationId = "com.salvatore.heatercontroller"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig field is provided per buildType below; no default injection here
    }

    buildTypes {
        debug {
            val apiKeyDebug = localProps.getProperty("API_KEY_DEBUG")
                ?: System.getenv("API_KEY_DEBUG")
                ?: baseApiKey
            buildConfigField("String", "API_KEY", "\"$apiKeyDebug\"")

            val insideDevice = localProps.getProperty("GOVEE_INSIDE_DEVICE") ?: ""
            val insideModel = localProps.getProperty("GOVEE_INSIDE_MODEL") ?: ""
            val tanksDevice = localProps.getProperty("GOVEE_TANKS_DEVICE") ?: ""
            val tanksModel = localProps.getProperty("GOVEE_TANKS_MODEL") ?: ""
            val waterDevice = localProps.getProperty("GOVEE_WATER_DEVICE") ?: ""
            val waterModel = localProps.getProperty("GOVEE_WATER_MODEL") ?: ""
            buildConfigField("String", "GOVEE_INSIDE_DEVICE", "\"$insideDevice\"")
            buildConfigField("String", "GOVEE_INSIDE_MODEL", "\"$insideModel\"")
            buildConfigField("String", "GOVEE_TANKS_DEVICE", "\"$tanksDevice\"")
            buildConfigField("String", "GOVEE_TANKS_MODEL", "\"$tanksModel\"")
            buildConfigField("String", "GOVEE_WATER_DEVICE", "\"$waterDevice\"")
            buildConfigField("String", "GOVEE_WATER_MODEL", "\"$waterModel\"")

            // Outputs (Heaters and Lamp)
            val heaterADevice = localProps.getProperty("GOVEE_HEATER_A_DEVICE") ?: ""
            val heaterAModel = localProps.getProperty("GOVEE_HEATER_A_MODEL") ?: ""
            val heaterBDevice = localProps.getProperty("GOVEE_HEATER_B_DEVICE") ?: ""
            val heaterBModel = localProps.getProperty("GOVEE_HEATER_B_MODEL") ?: ""
            val lampDevice = localProps.getProperty("GOVEE_LAMP_DEVICE") ?: ""
            val lampModel = localProps.getProperty("GOVEE_LAMP_MODEL") ?: ""
            buildConfigField("String", "GOVEE_HEATER_A_DEVICE", "\"$heaterADevice\"")
            buildConfigField("String", "GOVEE_HEATER_A_MODEL", "\"$heaterAModel\"")
            buildConfigField("String", "GOVEE_HEATER_B_DEVICE", "\"$heaterBDevice\"")
            buildConfigField("String", "GOVEE_HEATER_B_MODEL", "\"$heaterBModel\"")
            buildConfigField("String", "GOVEE_LAMP_DEVICE", "\"$lampDevice\"")
            buildConfigField("String", "GOVEE_LAMP_MODEL", "\"$lampModel\"")

            // Weather config
            val weatherApiKey = localProps.getProperty("WEATHER_API_KEY") ?: ""
            val weatherLocation = localProps.getProperty("WEATHER_LOCATION") ?: ""
            buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
            buildConfigField("String", "WEATHER_LOCATION", "\"$weatherLocation\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            val apiKeyRelease = localProps.getProperty("API_KEY_RELEASE")
                ?: System.getenv("API_KEY_RELEASE")
                ?: baseApiKey
            buildConfigField("String", "API_KEY", "\"$apiKeyRelease\"")

            val insideDevice = localProps.getProperty("GOVEE_INSIDE_DEVICE") ?: ""
            val insideModel = localProps.getProperty("GOVEE_INSIDE_MODEL") ?: ""
            val tanksDevice = localProps.getProperty("GOVEE_TANKS_DEVICE") ?: ""
            val tanksModel = localProps.getProperty("GOVEE_TANKS_MODEL") ?: ""
            val waterDevice = localProps.getProperty("GOVEE_WATER_DEVICE") ?: ""
            val waterModel = localProps.getProperty("GOVEE_WATER_MODEL") ?: ""
            buildConfigField("String", "GOVEE_INSIDE_DEVICE", "\"$insideDevice\"")
            buildConfigField("String", "GOVEE_INSIDE_MODEL", "\"$insideModel\"")
            buildConfigField("String", "GOVEE_TANKS_DEVICE", "\"$tanksDevice\"")
            buildConfigField("String", "GOVEE_TANKS_MODEL", "\"$tanksModel\"")
            buildConfigField("String", "GOVEE_WATER_DEVICE", "\"$waterDevice\"")
            buildConfigField("String", "GOVEE_WATER_MODEL", "\"$waterModel\"")

            // Outputs (Heaters and Lamp)
            val heaterADevice = localProps.getProperty("GOVEE_HEATER_A_DEVICE") ?: ""
            val heaterAModel = localProps.getProperty("GOVEE_HEATER_A_MODEL") ?: ""
            val heaterBDevice = localProps.getProperty("GOVEE_HEATER_B_DEVICE") ?: ""
            val heaterBModel = localProps.getProperty("GOVEE_HEATER_B_MODEL") ?: ""
            val lampDevice = localProps.getProperty("GOVEE_LAMP_DEVICE") ?: ""
            val lampModel = localProps.getProperty("GOVEE_LAMP_MODEL") ?: ""
            buildConfigField("String", "GOVEE_HEATER_A_DEVICE", "\"$heaterADevice\"")
            buildConfigField("String", "GOVEE_HEATER_A_MODEL", "\"$heaterAModel\"")
            buildConfigField("String", "GOVEE_HEATER_B_DEVICE", "\"$heaterBDevice\"")
            buildConfigField("String", "GOVEE_HEATER_B_MODEL", "\"$heaterBModel\"")
            buildConfigField("String", "GOVEE_LAMP_DEVICE", "\"$lampDevice\"")
            buildConfigField("String", "GOVEE_LAMP_MODEL", "\"$lampModel\"")

            // Weather config
            val weatherApiKey = localProps.getProperty("WEATHER_API_KEY") ?: ""
            val weatherLocation = localProps.getProperty("WEATHER_LOCATION") ?: ""
            buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
            buildConfigField("String", "WEATHER_LOCATION", "\"$weatherLocation\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}