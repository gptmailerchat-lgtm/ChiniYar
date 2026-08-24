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
        versionCode = 7
        versionName = "0.7.0"
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

// Patch the existing TranslateScreen before compilation so ML Kit clients are
// created lazily, instead of during Compose screen construction. This avoids
// release/startup crashes from native ML Kit initialization on affected devices.
tasks.register("patchTranslateScreenForRuntimeSafety") {
    doLast {
        val source = file("src/main/java/com/chiniyar/app/MainActivity.kt")
        val original = source.readText()
        val startMarker = "@Composable\nprivate fun TranslateScreen"
        val endMarker = "\n@Composable\nprivate fun PhraseScreen"
        val start = original.indexOf(startMarker)
        val end = if (start >= 0) original.indexOf(endMarker, start) else -1
        require(start >= 0 && end > start) { "TranslateScreen block not found" }

        val replacement = """@Composable
private fun TranslateScreen(modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognizedText by remember { mutableStateOf(\"\") }
    var translatedText by remember { mutableStateOf(\"\") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf(\"\") }
    var modelReady by remember { mutableStateOf(false) }

    val recognizerState = remember { mutableStateOf<com.google.mlkit.vision.text.TextRecognizer?>(null) }
    val translatorState = remember { mutableStateOf<com.google.mlkit.nl.translate.Translator?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recognizerState.value?.close()
            translatorState.value?.close()
        }
    }

    fun processUri(uri: Uri) {
        scope.launch {
            busy = true
            try {
                val bitmap = context.contentResolver.openInputStream(uri).use { stream ->
                    requireNotNull(stream) { \"امکان باز کردن تصویر وجود ندارد.\" }
                    requireNotNull(android.graphics.BitmapFactory.decodeStream(stream)) { \"فرمت تصویر پشتیبانی نمی‌شود.\" }
                }
                imageBitmap = bitmap

                val recognizer = recognizerState.value
                    ?: TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()).also {
                        recognizerState.value = it
                    }

                recognizedText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
                translatedText = \"\"
                message = if (recognizedText.isBlank()) \"متن چینی پیدا نشد.\" else \"متن شناسایی شد.\"
            } catch (e: Exception) {
                message = \"خطا در OCR: ${'$'}{e.message ?: \"نامشخص\"}\"
            } finally {
                busy = false
            }
        }
    }

    fun translate() {
        if (recognizedText.isBlank()) return
        scope.launch {
            busy = true
            try {
                val translator = translatorState.value
                    ?: Translation.getClient(
                        TranslatorOptions.Builder()
                            .setSourceLanguage(TranslateLanguage.CHINESE)
                            .setTargetLanguage(TranslateLanguage.PERSIAN)
                            .build()
                    ).also { translatorState.value = it }

                if (!modelReady) {
                    translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                    modelReady = true
                }
                translatedText = translator.translate(recognizedText).await()
            } catch (e: Exception) {
                message = \"ترجمه انجام نشد: ${'$'}{e.message ?: \"نامشخص\"}\"
            } finally {
                busy = false
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::processUri)
    }

    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(\"مترجم تصویری چینی\", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(\"یک عکس از نوشته چینی انتخاب کنید.\")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { galleryLauncher.launch(\"image/*\") }, enabled = !busy) { Text(\"انتخاب عکس\") }
                Button(onClick = { translate() }, enabled = recognizedText.isNotBlank() && !busy) { Text(\"ترجمه\") }
            }
        }
        imageBitmap?.let { bitmap ->
            item {
                Image(
                    bitmap.asImageBitmap(),
                    \"تصویر انتخاب‌شده\",
                    Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (recognizedText.isNotBlank()) item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(\"متن چینی\", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(recognizedText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { copy(context, recognizedText) }) { Text(\"کپی\") }
                    TextButton(onClick = { speakChinese(context, recognizedText) }) { Text(\"🔊\") }
                }
            }}
        }
        if (translatedText.isNotBlank()) item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(\"ترجمه فارسی\", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(translatedText)
                TextButton(onClick = { copy(context, translatedText) }) { Text(\"کپی\") }
            }}
        }
        if (message.isNotBlank()) item { Text(message) }
        if (busy) item { Text(\"در حال پردازش…\") }
    }
}
"""

        source.writeText(original.substring(0, start) + replacement + original.substring(end))
    }
}

tasks.named("preBuild").configure { dependsOn("patchTranslateScreenForRuntimeSafety") }

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
