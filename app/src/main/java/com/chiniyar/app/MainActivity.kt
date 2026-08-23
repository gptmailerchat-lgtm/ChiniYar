package com.chiniyar.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChiniYarApp() }
    }
}

data class Phrase(val zh: String, val pinyin: String, val fa: String, val category: String)
data class City(val zh: String, val en: String, val fa: String, val desc: String)

private val phrases = listOf(
    Phrase("你好", "Nǐ hǎo", "سلام", "مکالمه"),
    Phrase("谢谢", "Xièxie", "ممنون", "مکالمه"),
    Phrase("多少钱？", "Duōshao qián?", "چند قیمت است؟", "خرید"),
    Phrase("太贵了", "Tài guì le", "خیلی گران است", "خرید"),
    Phrase("可以便宜一点吗？", "Kěyǐ piányi yìdiǎn ma?", "می‌شود کمی ارزان‌تر حساب کنید؟", "مذاکره"),
    Phrase("洗手间在哪里？", "Xǐshǒujiān zài nǎlǐ?", "دستشویی کجاست؟", "سفر"),
    Phrase("我要去火车站", "Wǒ yào qù huǒchēzhàn", "می‌خواهم به ایستگاه قطار بروم", "حمل‌ونقل"),
    Phrase("请慢一点说", "Qǐng màn yìdiǎn shuō", "لطفاً کمی آهسته‌تر صحبت کنید", "مکالمه")
)

private val cities = listOf(
    City("上海", "Shanghai", "شانگهای", "مرکز مهم مالی، تجاری و فناوری چین."),
    City("深圳", "Shenzhen", "شنژن", "قطب فناوری، تولید و الکترونیک چین."),
    City("广州", "Guangzhou", "گوانگژو", "مرکز تجارت جنوب چین و مقصد مهم نمایشگاهی."),
    City("义乌", "Yiwu", "ییوو", "مرکز معروف بازارهای عمده‌فروشی و کالاهای مصرفی."),
    City("北京", "Beijing", "پکن", "پایتخت چین و مرکز مهم تاریخی و فرهنگی."),
    City("杭州", "Hangzhou", "هانگژو", "مرکز مهم فناوری و تجارت در شرق چین.")
)

@Composable
fun ChiniYarApp() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFC62828))) {
        var selectedTab by remember { mutableIntStateOf(0) }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("⌂") }, label = { Text("خانه") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("📷") }, label = { Text("مترجم") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("🀄") }, label = { Text("عبارات") })
                    NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Text("🇨🇳") }, label = { Text("چین") })
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(padding))
                1 -> TranslateScreen(Modifier.padding(padding))
                2 -> PhraseScreen(Modifier.padding(padding))
                else -> ChinaScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text("🇨🇳", fontSize = 48.sp)
        Text("چینی‌یار", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("دستیار سفر، مکالمه و ترجمه چینی", modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("امکانات نسخه MVP", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("• OCR چینی از عکس")
                Text("• ترجمه چینی به فارسی")
                Text("• عبارات کاربردی با Pinyin")
                Text("• راهنمای شهرهای مهم چین")
            }
        }
    }
}

@Composable
private fun TranslateScreen(modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognizedText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var modelReady by remember { mutableStateOf(false) }

    val recognizer = remember { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }
    val translator = remember {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.CHINESE)
                .setTargetLanguage(TranslateLanguage.PERSIAN)
                .build()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
            translator.close()
        }
    }

    fun processUri(uri: Uri) {
        scope.launch {
            busy = true
            try {
                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                imageBitmap = bitmap
                recognizedText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
                translatedText = ""
                message = if (recognizedText.isBlank()) "متن چینی پیدا نشد." else "متن شناسایی شد."
            } catch (e: Exception) {
                message = "خطا در OCR: ${e.message ?: "نامشخص"}"
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
                if (!modelReady) {
                    translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                    modelReady = true
                }
                translatedText = translator.translate(recognizedText).await()
            } catch (e: Exception) {
                message = "ترجمه انجام نشد: ${e.message ?: "نامشخص"}"
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
            Text("مترجم تصویری چینی", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("یک عکس از نوشته چینی انتخاب کنید.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { galleryLauncher.launch("image/*") }) { Text("انتخاب عکس") }
                Button(onClick = { translate() }, enabled = recognizedText.isNotBlank() && !busy) { Text("ترجمه") }
            }
        }
        imageBitmap?.let { bitmap ->
            item {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "تصویر انتخاب‌شده",
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (recognizedText.isNotBlank()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("متن چینی", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(recognizedText)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            TextButton(onClick = { copy(context, recognizedText) }) { Text("کپی") }
                            TextButton(onClick = { speakChinese(context, recognizedText) }) { Text("🔊") }
                        }
                    }
                }
            }
        }
        if (translatedText.isNotBlank()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ترجمه فارسی", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(translatedText)
                        TextButton(onClick = { copy(context, translatedText) }) { Text("کپی") }
                    }
                }
            }
        }
        if (message.isNotBlank()) item { Text(message) }
        if (busy) item { Text("در حال پردازش…") }
    }
}

@Composable
private fun PhraseScreen(modifier: Modifier) {
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("عبارات ضروری", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        items(phrases) { phrase ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(phrase.zh, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(phrase.pinyin)
                    Text(phrase.fa)
                    Text(phrase.category, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { speakChinese(context, phrase.zh) }) { Text("🔊 تلفظ") }
                }
            }
        }
    }
}

@Composable
private fun ChinaScreen(modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("شهرهای مهم چین", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        items(cities) { city ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${city.fa}  ${city.zh}", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(city.en, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(city.desc)
                }
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("ChiniYar", text))
}

private fun speakChinese(context: Context, text: String) {
    lateinit var tts: TextToSpeech
    tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val language = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            if (language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chiniyar-${System.currentTimeMillis()}")
            } else {
                tts.shutdown()
            }
        }
    }
}
