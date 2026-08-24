plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.chiniyar.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chiniyar.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 8
        versionName = "0.8.0"
    }

    buildTypes {
        release {
            // Keep the release APK installable and runtime-safe for ML Kit OCR.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures { compose = true }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Apply small, idempotent UI patches before compilation so the richer city/metro
// components can live in their own file without rewriting the main screen by hand.
val patchUi = tasks.register("patchUi") {
    doLast {
        val source = file("src/main/java/com/chiniyar/app/MainActivity.kt")
        var text = source.readText()

        val oldMetro = "item { Text(\"${'$'}{city.name}  ${'$'}{city.zh}\", fontSize = 22.sp, fontWeight = FontWeight.Bold)"
        val newMetro = "item { MetroCityHero(city)"
        if (oldMetro in text && "MetroCityHero(city)" !in text) text = text.replace(oldMetro, newMetro)

        val oldCity = "Text(city.desc)"
        val newCity = "Text(city.desc); EnhancedCityDetailsCard(city)"
        if (oldCity in text && "EnhancedCityDetailsCard(city)" !in text) text = text.replace(oldCity, newCity)

        source.writeText(text)
    }
}

tasks.named("preBuild").configure { dependsOn(patchUi) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Chinese OCR model is downloaded through Google Play Services when needed.
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-chinese:16.0.1")
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
