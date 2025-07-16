import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.detekt)
    alias(libs.plugins.android.junit5)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.sms_app"
    compileSdk = 34

    val buildDate = SimpleDateFormat("dd-MM-yyyy").format(Date())
    defaultConfig {
        applicationId = "com.example.sms_app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // Thêm thông tin build để debug
        buildConfigField("int", "VERSION_CODE", "$versionCode")
        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "VERSION_NAME", "\"$versionName\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/release-key.jks")
            storePassword = "android"
            keyAlias = "release"
            keyPassword = "android"
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Bật minify cho release build
            isShrinkResources = true // Bật shrink resources để giảm kích thước APK
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // BuildConfig fields
            buildConfigField("boolean", "ENABLE_INTEGRITY_CHECK", "true")
            buildConfigField("boolean", "ENABLE_TAMPER_DETECTION", "true")
            buildConfigField("String", "APP_SIGNATURE", "\"release\"")
        }
        
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release") // Sử dụng cùng signing với release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // BuildConfig fields
            buildConfigField("boolean", "ENABLE_INTEGRITY_CHECK", "false")
            buildConfigField("boolean", "ENABLE_TAMPER_DETECTION", "false")
            buildConfigField("String", "APP_SIGNATURE", "\"debug\"")
        }
    }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        // Kotlin 2.0+ uses the built-in Compose compiler
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
        }
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Material Icons Extended
    implementation(libs.androidx.material.icons.extended)
    
    // Fragment
    implementation(libs.androidx.fragment.ktx)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    
    // SMS and telephony
    implementation(libs.play.services.auth)
    implementation(libs.play.services.auth.api.phone)
    
    // SafetyNet
    implementation(libs.play.services.safetynet)
    
    // Excel processing - Apache POI (supports both .xls and .xlsx formats)
    implementation(libs.poi)
    implementation(libs.poi.ooxml)
    implementation(libs.poi.scratchpad)
    
    // JSON processing
    implementation(libs.gson)

    // HTTP client for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Logging
    implementation(libs.timber)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)

    // view-model
    implementation(libs.viewmodel.compose)
    implementation(libs.androidx.runtime.livedata)

    // hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation)

    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
