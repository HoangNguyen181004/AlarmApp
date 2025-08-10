plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") // plugin kotlin chính
    id("org.jetbrains.kotlin.kapt") // ✅ kapt plugin đúng tên
}

android {
    namespace = "com.example.alarm"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.alarm"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}


dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    val room_version = "2.6.1" // hoặc phiên bản mới nhất từ trang developer.android.com

    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // Nếu dùng Kotlin KAPT
    kapt("androidx.room:room-compiler:$room_version")

    // Room với Kotlin coroutines (nếu cần)
    implementation("androidx.room:room-ktx:$room_version")

    // Room với Paging 3 (nếu cần)
    implementation("androidx.room:room-paging:$room_version")

    implementation("com.google.code.gson:gson:2.11.0")

}