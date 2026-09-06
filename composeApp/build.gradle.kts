import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.crashlytics)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

kotlin {
    // Android hedefi — yeni compilerOptions DSL (kotlinOptions deprecated)
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        // ─────────────────────────────────────────────────
        // COMMON: Hem Android hem iOS'ta çalışan kod
        // ─────────────────────────────────────────────────
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Ktor (Network)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Supabase KMP
            implementation(libs.supabase.postgrest)
            // Realtime dependency kütüphanesi gerekli (ChatScreenModel, AskidaViewModel, SupabaseQuizMultiplayerRepository kullanıyor)
            // Ancak SupabaseManager'da install(Realtime) KAPALIIDIR - WebSocket bağlantısı açılmaz
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.compose.auth)

            // Koin (Dependency Injection - Hilt yerine)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Coil (Image loading - KMP)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Voyager Navigation
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.tab.navigator)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            // UI Extras
            implementation(libs.compose.shimmer)
            implementation(libs.filekit.compose)
            
            // Settings
            implementation(libs.multiplatform.settings.core)
            implementation(libs.multiplatform.settings.no.arg)
        }

        // ─────────────────────────────────────────────────
        // ANDROID ONLY: Android'e özel kütüphaneler
        // ─────────────────────────────────────────────────
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.core.ktx)

            // Ktor Android engine
            implementation(libs.ktor.client.okhttp)

            // Koin Android
            implementation(libs.koin.android)

            // Firebase — BOM ile versiyon yönetimi
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth.ktx)
            implementation(libs.firebase.firestore.ktx)
            implementation(libs.firebase.storage.ktx)
            implementation(libs.firebase.messaging.ktx)
            implementation(libs.firebase.crashlytics.ktx)

            // Media3 / ExoPlayer (Android sesli kitap oynatma)
            implementation(libs.media3.exoplayer)
            implementation(libs.media3.exoplayer.hls)
            implementation(libs.media3.session)
            implementation(libs.media3.ui)
            implementation(libs.media3.datasource.okhttp)

            // AdMob Android
            implementation(libs.play.services.ads)

            // Google Play Billing
            implementation(libs.billing.ktx)

            // OneSignal Push Notifications
            implementation(libs.onesignal)

            // Lottie
            implementation(libs.lottie.compose)

            // WorkManager
            implementation(libs.work.runtime.ktx)
        }

        // ─────────────────────────────────────────────────
        // iOS ONLY: iOS'a özel kütüphaneler
        // ─────────────────────────────────────────────────
        iosMain.dependencies {
            // iOS'ta HTTPS ve NSURLSession entegrasyonu için desteklenen motor Darwin'dir.
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.semtinazizi.kitapduy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.semtinazizi.kitapduy"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        val supabaseUrl = "\"${localProperties.getProperty("SUPABASE_URL") ?: ""}\""
        val supabaseKey = "\"${localProperties.getProperty("SUPABASE_KEY") ?: ""}\""
        val cfAccountId = "\"${localProperties.getProperty("CF_ACCOUNT_ID") ?: ""}\""
        val cfPublicUrl = "\"${localProperties.getProperty("CF_PUBLIC_URL") ?: ""}\""
        val cfWorkerUrl = "\"${localProperties.getProperty("CF_WORKER_URL") ?: ""}\""
        val cfUploadToken = "\"${localProperties.getProperty("CF_UPLOAD_TOKEN") ?: ""}\""
        val cfAccessKey = "\"${localProperties.getProperty("CF_ACCESS_KEY") ?: ""}\""
        val cfSecretKey = "\"${localProperties.getProperty("CF_SECRET_KEY") ?: ""}\""

        buildConfigField("String", "SUPABASE_URL", supabaseUrl)
        buildConfigField("String", "SUPABASE_KEY", supabaseKey)
        buildConfigField("String", "CF_ACCOUNT_ID", cfAccountId)
        buildConfigField("String", "CF_PUBLIC_URL", cfPublicUrl)
        buildConfigField("String", "CF_WORKER_URL", cfWorkerUrl)
        buildConfigField("String", "CF_UPLOAD_TOKEN", cfUploadToken)
        buildConfigField("String", "CF_ACCESS_KEY", cfAccessKey)
        buildConfigField("String", "CF_SECRET_KEY", cfSecretKey)
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/INDEX.LIST")
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
