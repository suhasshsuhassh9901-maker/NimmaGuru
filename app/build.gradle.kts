plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nimmaguru"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nimmaguru"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // 🔴 REPLACE these with your actual Supabase values
        buildConfigField("String", "SUPABASE_URL", "\"https://puqpivfpuhyisqhbeclp.supabase.co\"")
        buildConfigField("String", "SUPABASE_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB1cXBpdmZwdWh5aXNxaGJlY2xwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzgxNjYzNTYsImV4cCI6MjA5Mzc0MjM1Nn0.uEQbv4AiyeX2eFTplTOBUSr-jzg46UBOEprn8aSthIo\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Supabase BOM
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // Ktor engine for Supabase
    implementation("io.ktor:ktor-client-android:3.0.0")

    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.cardview:cardview:1.0.0")
}