plugins { id("com.android.application") }
android {
    namespace = "cl.thomasogaz.buscadormarcas"
    compileSdk = 35
    defaultConfig {
        applicationId = "cl.thomasogaz.buscadormarcas"
        minSdk = 24
        targetSdk = 35
        versionCode = 33
        versionName = "1.29"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.22"))
    implementation("androidx.core:core:1.13.1")
    implementation("com.google.android.gms:play-services-base:18.5.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
}
