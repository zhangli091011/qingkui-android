import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val qingkuiEnvironment = providers.gradleProperty("QINGKUI_ENV")
    .orElse("production")
    .get()
val supportedEnvironments = setOf("development", "test", "pilot", "production")
require(qingkuiEnvironment in supportedEnvironments) {
    "QINGKUI_ENV must be one of ${supportedEnvironments.joinToString()}"
}

fun loadEnvironment(name: String): Properties {
    val file = rootProject.file("config/environments/$name.properties")
    require(file.isFile) { "Missing Android environment config: $file" }
    return Properties().apply { file.inputStream().use(::load) }
}

val environmentConfig = loadEnvironment(qingkuiEnvironment)
val qingkuiApiBaseUrl = providers.gradleProperty("QINGKUI_API_BASE_URL")
    .orElse(environmentConfig.getProperty("apiBaseUrl"))
    .get()
val productionApiBaseUrl = loadEnvironment("production").getProperty("apiBaseUrl")
val releaseApiBaseUrl = when (qingkuiEnvironment) {
    "pilot", "production" -> qingkuiApiBaseUrl
    else -> productionApiBaseUrl
}
require(releaseApiBaseUrl.startsWith("https://")) {
    "Release API base URL must use HTTPS: $releaseApiBaseUrl"
}

android {
    namespace = "cn.qingkui.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "cn.qingkui.app"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "API_BASE_URL", "\"$qingkuiApiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
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
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
