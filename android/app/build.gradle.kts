import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}
val usdaApiKey = (localProperties.getProperty("USDA_API_KEY") ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val nutritionixAppId = (localProperties.getProperty("NUTRITIONIX_APP_ID") ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val nutritionixApiKey = (localProperties.getProperty("NUTRITIONIX_API_KEY") ?: "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
val openFoodFactsProviderEnabled = localProperties
    .getProperty("ONLINE_PROVIDER_OPEN_FOOD_FACTS_ENABLED")
    ?.trim()
    ?.equals("true", ignoreCase = true)
    ?: true
val usdaProviderEnabled = localProperties
    .getProperty("ONLINE_PROVIDER_USDA_ENABLED")
    ?.trim()
    ?.equals("true", ignoreCase = true)
    ?: true
val nutritionixProviderEnabled = localProperties
    .getProperty("ONLINE_PROVIDER_NUTRITIONIX_ENABLED")
    ?.trim()
    ?.equals("true", ignoreCase = true)
    ?: true

android {
    namespace = "com.openfuel.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.openfuel.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "PRO_PRODUCT_ID", "\"openfuel_pro\"")
        buildConfigField("String", "USDA_API_KEY", "\"$usdaApiKey\"")
        buildConfigField("String", "NUTRITIONIX_APP_ID", "\"$nutritionixAppId\"")
        buildConfigField("String", "NUTRITIONIX_API_KEY", "\"$nutritionixApiKey\"")
        buildConfigField("boolean", "ONLINE_PROVIDER_OPEN_FOOD_FACTS_ENABLED", openFoodFactsProviderEnabled.toString())
        buildConfigField("boolean", "ONLINE_PROVIDER_USDA_ENABLED", usdaProviderEnabled.toString())
        buildConfigField("boolean", "ONLINE_PROVIDER_NUTRITIONIX_ENABLED", nutritionixProviderEnabled.toString())

        testInstrumentationRunner = "com.openfuel.app.OpenFuelAndroidTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation(project(":shared-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.google.mlkit.barcode.scanning)
    implementation(libs.google.play.billing.ktx)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
