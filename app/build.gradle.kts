import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
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
val privacyNoticeVersion = environmentConfig.getProperty("privacyNoticeVersion", "2026-08-31")
val productionApiBaseUrl = loadEnvironment("production").getProperty("apiBaseUrl")
val releaseApiBaseUrl = when (qingkuiEnvironment) {
    "pilot", "production" -> qingkuiApiBaseUrl
    else -> productionApiBaseUrl
}
val qingkuiVersionCodeProperty = providers.gradleProperty("QINGKUI_VERSION_CODE").orNull
val qingkuiVersionNameProperty = providers.gradleProperty("QINGKUI_VERSION_NAME").orNull
val qingkuiVersionCode = qingkuiVersionCodeProperty?.toIntOrNull() ?: 4
val qingkuiVersionName = qingkuiVersionNameProperty ?: "0.1.3"
val signingStorePath = providers.environmentVariable("QINGKUI_SIGNING_STORE_FILE").orNull
val signingStorePassword = providers.environmentVariable("QINGKUI_SIGNING_STORE_PASSWORD").orNull
val signingKeyAlias = providers.environmentVariable("QINGKUI_SIGNING_KEY_ALIAS").orNull
val signingKeyPassword = providers.environmentVariable("QINGKUI_SIGNING_KEY_PASSWORD").orNull
val releaseSigningConfigured = listOf(
    signingStorePath,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword,
).all { !it.isNullOrBlank() }
require(releaseApiBaseUrl.startsWith("https://")) {
    "Release API base URL must use HTTPS: $releaseApiBaseUrl"
}
require(qingkuiVersionCode > 0) { "QINGKUI_VERSION_CODE must be a positive integer" }

android {
    namespace = "cn.qingkui.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "cn.qingkui.app"
        minSdk = 27
        targetSdk = 35
        versionCode = qingkuiVersionCode
        versionName = qingkuiVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "API_BASE_URL", "\"$qingkuiApiBaseUrl\"")
        buildConfigField("String", "PRIVACY_NOTICE_VERSION", "\"$privacyNoticeVersion\"")
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(signingStorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
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

val verifyReleaseSigning by tasks.registering {
    group = "verification"
    description = "Reject production release builds without explicit version and signing inputs."
    doLast {
        require(qingkuiEnvironment in setOf("pilot", "production")) {
            "Release builds require -PQINGKUI_ENV=pilot or production"
        }
        require(!qingkuiVersionCodeProperty.isNullOrBlank()) {
            "Release builds require -PQINGKUI_VERSION_CODE=<increasing integer>"
        }
        require(!qingkuiVersionNameProperty.isNullOrBlank()) {
            "Release builds require -PQINGKUI_VERSION_NAME=<version>"
        }
        require(releaseSigningConfigured) {
            "Release signing requires QINGKUI_SIGNING_STORE_FILE, QINGKUI_SIGNING_STORE_PASSWORD, " +
                "QINGKUI_SIGNING_KEY_ALIAS and QINGKUI_SIGNING_KEY_PASSWORD"
        }
        require(rootProject.file(signingStorePath!!).isFile) {
            "Signing keystore does not exist: $signingStorePath"
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyReleaseSigning)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
    implementation("androidx.webkit:webkit:1.13.0")
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
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
