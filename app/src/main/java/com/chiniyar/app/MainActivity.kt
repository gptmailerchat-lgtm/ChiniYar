package com.chiniyar.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.icu.text.BreakIterator
import android.icu.text.Transliterator
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChiniYarApp() }
    }
}

data class Phrase(val zh: String, val pinyin: String, val fa: String, val category: String)
data class City(val zh: String, val en: String, val fa: String, val tag: String, val desc: String, val tips: List<String>)
data class OcrLine(val text: String, val rect: RectF)
data class PinyinToken(val zh: String, val pinyin: String)
data class MetroLine(val name: String, val tag: String, val stations: List<String>)
data class MetroCity(val city: String, val cityZh: String, val hint: String, val lines: List<MetroLine>)

private val phrases = listOf(
    Phrase("你好", "Nǐ hǎo", "سلام", "مکالمه"),
    Phrase("多少钱？", "Duōshao qián?", "چند قیمت است؟", "خرید"),
    Phrase("太贵了", "Tài guì le", "خیلی گران است", "خرید"),
    Phrase("可以便宜一点吗？", "Kěyǐ piányi yìdiǎn ma?", "می‌شود کمی ارزان‌تر حساب کنید؟", "مذاکره"),
    Phrase("洗手间在哪里？", "Xǐshǒujiān zài nǎlǐ?", "دستشویی کجاست؟", "سفر"),
    Phrase("我要去火车站", "Wǒ yào qù huǒchēzhàn", "می‌خواهم به ایستگاه قطار بروم", "حمل‌ونقل"),
    Phrase("请给我这个", "Qǐng gěi wǒ zhège", "لطفاً این را به من بدهید", "خرید"),
    Phrase("谢谢", "Xièxie", "ممنون", "مکالمه"),
    Phrase("请问地铁站在哪里？", "Qǐngwèn dìtiě zhàn zài nǎlǐ?", "ببخشید، ایستگاه مترو کجاست؟", "حمل‌ونقل"),
    Phrase("我想去这里", "Wǒ xiǎng qù zhèlǐ", "می‌خواهم به اینجا بروم", "تاکسی"),
    Phrase("请慢一点说", "Qǐng màn yìdiǎn shuō", "لطفاً کمی آهسته‌تر صحبت کنید", "مکالمه"),
    Phrase("可以刷卡吗？", "Kěyǐ shuākǎ ma?", "می‌شود با کارت پرداخت کرد؟", "پرداخت"),
    Phrase("我要看样品", "Wǒ yào kàn yàngpǐn", "می‌خواهم نمونه کالا را ببینم", "تجارت"),
    Phrase("这个价格太高了", "Zhège jiàgé tài gāo le", "این قیمت خیلی بالاست", "مذاکره"),
    Phrase("如果买一千个，价格是多少？", "Rúguǒ mǎi yìqiān gè, jiàgé shì duōshao?", "اگر هزار عدد بخرم، قیمت چقدر می‌شود؟", "تجارت")
)

private val cities = listOf(
    City("上海", "Shanghai", "شانگهای", "تجارت + فناوری", "مرکز مالی و تجاری مهم چین و مقصدی مهم برای نمایشگاه‌ها و شرکت‌های بین‌المللی.", listOf("تاکسی و مترو بسیار مهم است", "آدرس چینی هتل را ذخیره کنید", "برای مراکز تجاری نام چینی را همراه داشته باشید")),
    City("深圳", "Shenzhen", "شنژن", "فناوری + الکترونیک", "یکی از قطب‌های فناوری، تولید و تأمین الکترونیک چین.", listOf("برای بازارهای الکترونیک محله و ایستگاه چینی را ذخیره کنید", "برای جلسات تجاری کارت ویزیت همراه داشته باشید")),
    City("广州", "Guangzhou", "گوانگژو", "تجارت + عمده‌فروشی", "مرکز تجارت جنوب چین و میزبان نمایشگاه مشهور Canton Fair.", listOf("برای نمایشگاه از قبل مسیر و خروجی مترو را ذخیره کنید", "نام سالن نمایشگاه را چینی هم داشته باشید")),
    City("义乌", "Yiwu", "ییوو", "بازار عمده‌فروشی", "مرکز مشهور کالاهای مصرفی، بازارهای عمده‌فروشی و خرید صادراتی.", listOf("آدرس بازار را به چینی نشان دهید", "برای مذاکره درباره قیمت و MOQ عبارات آماده داشته باشید")),
    City("北京", "Beijing", "پکن", "فرهنگ + پایتخت", "پایتخت چین با جاذبه‌های تاریخی، اداری و فرهنگی مهم.", listOf("برای مکان‌های توریستی نام چینی را ذخیره کنید", "در شهر قدیمی زمان بیشتری برای بازرسی و ورود در نظر بگیرید")),
    City("杭州", "Hangzhou", "هانگژو", "فناوری + تجارت", "شهر مهم فناوری و تجارت در شرق چین با پیوندهای ریلی خوب.", listOf("برای سفر بین‌شهری، نام ایستگاه قطار را دقیق بررسی کنید", "West Lake یکی از نقاط شاخص شهر است")),
    City("宁波", "Ningbo", "نینگبو", "بندر + لجستیک", "شهر بندری مهم در شرق چین و یکی از مراکز تجارت و لجستیک.", listOf("نام بندر و منطقه صنعتی را چینی ذخیره کنید", "برای رفت‌وآمد بین بندر و هتل مسیر را آفلاین نگه دارید")),
    City("成都", "Chengdu", "چنگدو", "فرهنگ + غذا", "مرکز مهم غرب چین، مشهور به فرهنگ سیچوان و غذاهای تند.", listOf("برای سفارش غذا سطح تندی را مشخص کنید", "علائم مترو و ایستگاه‌ها را از قبل دانلود کنید"))
)

private val metroCities = listOf(
    MetroCity("Shanghai", "上海", "داده شماتیک MVP؛ ایستگاه‌های کلیدی برای مسافر", listOf(
        MetroLine("Line 2", "2", listOf("虹桥火车站", "中山公园", "人民广场", "南京东路", "陆家嘴", "浦东国际机场")),
        MetroLine("Line 10", "10", listOf("虹桥火车站", "上海动物园", "南京东路", "豫园", "新天地"))
    )),
    MetroCity("Beijing", "北京", "داده شماتیک MVP؛ محورهای پرتردد", listOf(
        MetroLine("Line 1", "1", listOf("苹果园", "天安门西", "天安门东", "王府井", "国贸")),
        MetroLine("Line 2", "2", listOf("北京站", "建国门", "东直门", "雍和宫", "西直门")),
        MetroLine("Airport Express", "AE", listOf("东直门", "三元桥", "首都机场"))
    )),
    MetroCity("Guangzhou", "广州", "داده شماتیک MVP؛ ایستگاه‌های مهم تجاری", listOf(
        MetroLine("Line 1", "1", listOf("广州东站", "体育西路", "公园前", "西塱")),
        MetroLine("Line 3", "3", listOf("机场北", "机场南", "体育西路", "汉溪长隆", "番禺广场"))
    )),
    MetroCity("Shenzhen", "深圳", "داده شماتیک MVP؛ مسیرهای کلیدی", listOf(
        MetroLine("Line 1", "1", listOf("罗湖", "老街", "大剧院", "会展中心", "世界之窗", "机场东")),
        MetroLine("Line 4", "4", listOf("福田口岸", "会展中心", "少年宫", "深圳北站", "牛湖"))
    )),
    MetroCity("Hangzhou", "杭州", "داده شماتیک MVP؛ اتصال نقاط مهم شهر", listOf(
        MetroLine("Line 1", "1", listOf("湘湖", "龙翔桥", "凤起路", "杭州东站", "萧山国际机场")),
        MetroLine("Line 19", "19", listOf("杭州西站", "创景路", "杭州东站", "萧山国际机场"))
    ))
)

@Composable
fun ChiniYarApp() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFC62828), secondary = Color(0xFFD97706))) {
        var selected by remember { mutableIntStateOf(0) }
        Scaffold(bottomBar = {
            NavigationBar {
                NavigationBarItem(selected == 0, { selected = 0 }, icon = { Text("⌂") }, label = { Text("خانه") })
                NavigationBarItem(selected == 1, { selected = 1 }, icon = { Text("📷") }, label = { Text("مترجم") })
                NavigationBarItem(selected == 2, { selected = 2 }, icon = { Text("🀄") }, label = { Text("عبارات") })
                NavigationBarItem(selected == 3, { selected = 3 }, icon = { Text("🚇") }, label = { Text("مترو") })
                NavigationBarItem(selected == 4, { selected = 4 }, icon = { Text("🇨🇳") }, label = { Text("چین") })
            }
        }) { padding ->
            when (selected) {
                0 -> HomeScreen(Modifier.padding(padding), { selected = 1 }, { selected = 2 }, { selected = 3 }, { selected = 4 })
                1 -> TranslateScreen(Modifier.padding(padding))
                2 -> PhraseScreen(Modifier.padding(padding))
                3 -> MetroScreen(Modifier.padding(padding))
                else -> ChinaScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(modifier: Modifier, onTranslate: () -> Unit, onPhrases: () -> Unit, onMetro: () -> Unit, onCities: () -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🇨🇳", fontSize = 44.sp)
        Text("چینی‌یار", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("همراه فارسی‌زبان شما در چین", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onTranslate, modifier = Modifier.fillMaxWidth().height(64.dp), shape = RoundedCornerShape(18.dp)) { Text("📷  ترجمه عکس با دوربین یا گالری", fontSize = 17.sp) }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCard("🗣️", "عبارات سفر و تجارت", onPhrases, Modifier.weight(1f))
            HomeCard("🚇", "مترو آفلاین", onMetro, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        HomeCard("🏙️", "شهرهای مهم چین", onCities, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("حالت آفلاین", fontWeight = FontWeight.Bold)
            Text("OCR چینی همراه برنامه است. مدل ترجمه چینی↔فارسی را یک‌بار دانلود کنید؛ سپس ترجمه روی خود دستگاه انجام می‌شود.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }}
        Spacer(Modifier.weight(1f))
        Text("نسخه MVP 0.4 • OCR انتخابی + Pinyin واژه‌ای + مترو آفلاین", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
    }
}

@Composable
private fun HomeCard(icon: String, title: String, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier.clickable(onClick = onClick)) { Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 28.sp); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }}
}

@Composable
private fun TranslateScreen(modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var liveMode by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf(emptyList<OcrLine>()) }
    var selectedText by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var translated by remember { mutableStateOf("") }
    var pinyinTokens by remember { mutableStateOf(emptyList<PinyinToken>()) }
    var translationBusy by remember { mutableStateOf(false) }
    var modelReady by remember { mutableStateOf(false) }

    val recognizer = remember { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }
    val translator = remember {
        Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.CHINESE).setTargetLanguage(TranslateLanguage.PERSIAN).build())
    }

    fun updateSelection(line: OcrLine) {
        selectedText = line.text
        pinyinTokens = line.text.toPinyinTokens()
        translated = ""
    }

    fun processBitmap(bitmap: Bitmap) {
        busy = true; imageBitmap = bitmap; message = ""
        scope.launch {
            try {
                val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                lines = result.textBlocks.flatMap { block -> block.lines.map { line ->
                    OcrLine(line.text.trim(), RectF(line.boundingBox?.left?.toFloat() ?: 0f, line.boundingBox?.top?.toFloat() ?: 0f, line.boundingBox?.right?.toFloat() ?: 0f, line.boundingBox?.bottom?.toFloat() ?: 0f))
                }}.filter { it.text.isNotBlank() }
                lines.firstOrNull()?.let(::updateSelection)
                message = if (lines.isEmpty()) "متن چینی پیدا نشد؛ عکس واضح‌تر یا نزدیک‌تر بگیرید." else "برای انتخاب یک ناحیه، روی کادر یا متن بزنید."
            } catch (e: Exception) { message = "خطا در OCR: ${e.message ?: "نامشخص"}" }
            finally { busy = false }
        }
    }

    fun translateSelected() {
        val value = selectedText.trim(); if (value.isBlank()) return
        translationBusy = true
        scope.launch {
            try {
                if (!modelReady) { translator.downloadModel(DownloadConditions.Builder().build()).await(); modelReady = true }
                translated = translator.translate(value).await()
            } catch (e: Exception) { translated = "مدل ترجمه آفلاین در دسترس نیست: ${e.message ?: "خطای ناشناخته"}" }
            finally { translationBusy = false }
        }
    }

    fun downloadModel() {
        translationBusy = true
        scope.launch {
            try {
                translator.downloadModel(DownloadConditions.Builder().build()).await(); modelReady = true; translated = "✅ بسته ترجمه چینی↔فارسی آماده شد."
            } catch (e: Exception) { translated = "دانلود بسته ترجمه انجام نشد: ${e.message ?: "خطای شبکه"}" }
            finally { translationBusy = false }
        }
    }

    DisposableEffect(Unit) { onDispose { recognizer.close(); translator.close() } }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) try {
            @Suppress("DEPRECATION")
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            processBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false))
        } catch (e: Exception) { message = "خواندن عکس ممکن نشد: ${e.message ?: "نامشخص"}" }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (!granted) message = "برای استفاده از دوربین، اجازه دسترسی به دوربین لازم است." }
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("مترجم تصویری", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("عکس یا دوربین → OCR → انتخاب ناحیه → Pinyin واژه‌ای → ترجمه فارسی", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(liveMode, { liveMode = true }, label = { Text("📷 دوربین زنده") })
            FilterChip(!liveMode, { liveMode = false }, label = { Text("🖼️ عکس گالری") })
        }
        Spacer(Modifier.height(10.dp))

        if (liveMode) {
            if (cameraGranted) LiveCamera(onText = { detected -> if (detected.isNotEmpty()) { lines = detected; detected.firstOrNull()?.let(::updateSelection) } }, onSelect = ::updateSelection)
            else Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("دسترسی دوربین لازم است", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("فعال کردن دوربین") }
            }}
        } else {
            imageBitmap?.let { bitmap -> GalleryImageOverlay(bitmap, lines, selectedText, onSelect = ::updateSelection) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { gallery.launch("image/*") }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("انتخاب / تعویض عکس") }
        }
        Spacer(Modifier.height(10.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (message.isNotBlank()) Text(message, Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary)

        if (lines.isNotEmpty()) {
            Text("متن‌های شناسایی‌شده", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                lines.take(20).forEach { line -> Card(Modifier.fillMaxWidth().clickable { updateSelection(line) }, colors = CardDefaults.cardColors(containerColor = if (line.text == selectedText) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(line.text, Modifier.padding(11.dp), fontSize = 20.sp)
                }}
            }
        }

        if (selectedText.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("انتخاب‌شده", fontWeight = FontWeight.Bold)
                Text(selectedText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Pinyin واژه‌ای", fontWeight = FontWeight.Bold)
                FlowRowPinyin(pinyinTokens)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { copy(context, selectedText); message = "متن کپی شد." }, modifier = Modifier.weight(1f)) { Text("📋 کپی") }
                    OutlinedButton(onClick = { share(context, selectedText + "\n" + pinyinTokens.joinToString(" ") { it.pinyin }) }, modifier = Modifier.weight(1f)) { Text("↗ اشتراک") }
                    OutlinedButton(onClick = { speak(context, selectedText) }, modifier = Modifier.weight(1f)) { Text("🔊 تلفظ") }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick = ::translateSelected, enabled = !translationBusy, modifier = Modifier.fillMaxWidth()) { Text(if (translationBusy) "در حال پردازش..." else "🇮🇷 ترجمه فارسی") }
                OutlinedButton(onClick = ::downloadModel, enabled = !translationBusy, modifier = Modifier.fillMaxWidth()) { Text(if (modelReady) "✅ بسته آفلاین آماده است" else "⬇️ دانلود بسته ترجمه آفلاین") }
                if (translated.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text("نتیجه", fontWeight = FontWeight.Bold); Text(translated, fontSize = 19.sp) }
                Text("نکته: ترجمه روی دستگاه برای استفاده عمومی مناسب است؛ برای قرارداد، امور حقوقی یا پزشکی، نتیجه را حتماً دوباره بررسی کنید.", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
            }}
        }
    }
}

@Composable
private fun FlowRowPinyin(tokens: List<PinyinToken>) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        tokens.forEach { token -> AssistChip(onClick = {}, label = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(token.zh, fontSize = 16.sp); Text(token.pinyin, fontSize = 11.sp) } }) }
    }
}

@Composable
private fun GalleryImageOverlay(bitmap: Bitmap, lines: List<OcrLine>, selected: String, onSelect: (OcrLine) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black)) {
        val imageW = bitmap.width.toFloat(); val imageH = bitmap.height.toFloat()
        val density = LocalDensity.current
        val containerW = with(density) { maxWidth.toPx() }
        val containerH = with(density) { maxHeight.toPx() }
        val scale = min(containerW / imageW, containerH / imageH)
        val shownW = imageW * scale; val shownH = imageH * scale
        val offsetX = (containerW - shownW) / 2f; val offsetY = (containerH - shownH) / 2f
        androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        Canvas(Modifier.fillMaxSize().pointerInput(lines, bitmap) { detectTapGestures { pos ->
            val x = (pos.x - offsetX) / scale; val y = (pos.y - offsetY) / scale
            lines.firstOrNull { it.rect.contains(x, y) }?.let(onSelect)
        } }) {
            lines.take(20).forEach { line ->
                val left = offsetX + line.rect.left * scale; val top = offsetY + line.rect.top * scale
                val right = offsetX + line.rect.right * scale; val bottom = offsetY + line.rect.bottom * scale
                drawRect(if (line.text == selected) Color.Yellow else Color.Cyan, Offset(left, top), Size((right-left).coerceAtLeast(4f), (bottom-top).coerceAtLeast(4f)), style = Stroke(width = 3f))
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun LiveCamera(onText: (List<OcrLine>) -> Unit, onSelect: (OcrLine) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }
    val busy = remember { AtomicBoolean(false) }
    var latestLines by remember { mutableStateOf(emptyList<OcrLine>()) }

    Box(Modifier.fillMaxWidth().height(340.dp).background(Color.Black, RoundedCornerShape(18.dp))) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy -> analyzeFrame(proxy, recognizer, busy) { detected -> latestLines = detected.take(12); onText(detected) } }
                provider.unbindAll(); provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        })
        Canvas(Modifier.fillMaxSize().pointerInput(latestLines) { detectTapGestures { pos ->
            val sx = size.width / 1280f; val sy = size.height / 720f
            latestLines.firstOrNull { l -> val r = RectF(l.rect.left*sx, l.rect.top*sy, l.rect.right*sx, l.rect.bottom*sy); r.contains(pos.x, pos.y) }?.let(onSelect)
        } }) {
            val sx = size.width / 1280f; val sy = size.height / 720f
            latestLines.forEach { line -> drawRect(Color.Yellow.copy(alpha = 0.85f), Offset(line.rect.left*sx, line.rect.top*sy), Size((line.rect.width()*sx).coerceAtLeast(4f), (line.rect.height()*sy).coerceAtLeast(4f)), style = Stroke(width=3f)) }
        }
        Text("روی کادر زرد بزنید", Modifier.align(Alignment.BottomCenter).padding(8.dp), color = Color.White, fontSize = 12.sp)
    }
    DisposableEffect(Unit) { onDispose { executor.shutdown(); recognizer.close() } }
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeFrame(imageProxy: ImageProxy, recognizer: TextRecognizer, busy: AtomicBoolean, onText: (List<OcrLine>) -> Unit) {
    val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
    if (!busy.compareAndSet(false, true)) { imageProxy.close(); return }
    recognizer.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
        .addOnSuccessListener { result ->
            val lines = result.textBlocks.flatMap { block -> block.lines.map { line -> OcrLine(line.text.trim(), RectF(line.boundingBox?.left?.toFloat() ?: 0f, line.boundingBox?.top?.toFloat() ?: 0f, line.boundingBox?.right?.toFloat() ?: 0f, line.boundingBox?.bottom?.toFloat() ?: 0f)) } }.filter { it.text.isNotBlank() }
            if (lines.isNotEmpty()) onText(lines)
        }.addOnCompleteListener { busy.set(false); imageProxy.close() }
}

private fun String.toPinyinTokens(): List<PinyinToken> {
    if (isBlank()) return emptyList()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            val iterator = BreakIterator.getWordInstance(Locale.SIMPLIFIED_CHINESE)
            iterator.setText(this)
            val result = mutableListOf<PinyinToken>()
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                val token = substring(start, end).trim()
                if (token.any { it.isLetterOrDigit() || it.code in 0x4E00..0x9FFF }) result += PinyinToken(token, token.toPinyin())
                start = end; end = iterator.next()
            }
            if (result.isNotEmpty()) return result
        } catch (_: Exception) {}
    }
    return mapToCharacters(this).map { PinyinToken(it, it.toPinyin()) }
}

private fun mapToCharacters(text: String): List<String> = text.filter { !it.isWhitespace() }.map { it.toString() }

private fun String.toPinyin(): String {
    if (isBlank()) return ""
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try { return Transliterator.getInstance("Han-Latin").transliterate(this).replace("  ", " ").trim() } catch (_: Exception) {}
    }
    val map = mapOf('你' to "nǐ", '好' to "hǎo", '谢' to "xiè", '再' to "zài", '见' to "jiàn", '请' to "qǐng", '问' to "wèn", '多' to "duō", '少' to "shao", '钱' to "qián", '太' to "tài", '贵' to "guì", '了' to "le", '可' to "kě", '以' to "yǐ", '便' to "pián", '宜' to "yí", '一' to "yī", '点' to "diǎn", '吗' to "ma", '我' to "wǒ", '要' to "yào", '去' to "qù", '火' to "huǒ", '车' to "chē", '站' to "zhàn", '地' to "dì", '铁' to "tiě", '上' to "shàng", '海' to "hǎi", '北' to "běi", '京' to "jīng", '深' to "shēn", '圳' to "zhèn", '广' to "guǎng", '州' to "zhōu")
    return mapToCharacters(this).joinToString(" ") { map[it.first()] ?: it }
}

@Composable
private fun PhraseScreen(modifier: Modifier) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("عبارات کاربردی", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("سفر، خرید، پرداخت و تجارت در چین", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("جستجو در فارسی یا چینی") })
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(phrases.filter { query.isBlank() || it.zh.contains(query) || it.fa.contains(query) || it.category.contains(query) }) { p ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(p.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary); IconButton(onClick = { speak(context, p.zh) }) { Text("🔊") } }
                    Text(p.zh, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(p.pinyin, fontSize = 15.sp); Text(p.fa, fontSize = 17.sp)
                }}
            }
        }
    }
}

@Composable
private fun MetroScreen(modifier: Modifier) {
    var selectedCity by remember { mutableStateOf(metroCities.first()) }
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("🚇 حمل‌ونقل آفلاین", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("نقشه شماتیک خطوط و ایستگاه‌های کلیدی بدون اینترنت", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { metroCities.forEach { city -> FilterChip(city.city == selectedCity.city, { selectedCity = city }, label = { Text(city.city) }) } }
        Spacer(Modifier.height(10.dp))
        OfflineMetroMap(selectedCity)
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("📌 نکته", fontWeight = FontWeight.Bold); Text("این داده‌ها در MVP به‌صورت داخلی ذخیره شده‌اند و ماهیت شماتیک/راهنما دارند؛ برای مسیرهای لحظه‌ای، تغییرات سرویس و خروجی‌های جدید، منبع آنلاین لازم است.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }}
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(selectedCity.lines) { line -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("Line ${line.tag} — ${line.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(line.stations.joinToString("  →  "), fontSize = 15.sp) } } }
        }
    }
}

@Composable
private fun OfflineMetroMap(city: MetroCity) {
    Card(Modifier.fillMaxWidth().height(190.dp)) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val lineGap = size.height / (city.lines.size + 1)
            city.lines.forEachIndexed { index, line ->
                val y = lineGap * (index + 1)
                val x1 = 24f; val x2 = size.width - 24f
                drawLine(Color(0xFFC62828), Offset(x1, y), Offset(x2, y), strokeWidth = 8f)
                val count = line.stations.size.coerceAtLeast(2)
                for (i in 0 until count) {
                    val x = x1 + (x2 - x1) * i / (count - 1)
                    drawCircle(Color.White, 7f, Offset(x, y)); drawCircle(Color(0xFFC62828), 7f, Offset(x, y), style = Stroke(width = 3f))
                }
            }
        }
    }
}

@Composable
private fun ChinaScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("چین را بشناس", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("راهنمای آفلاین شهرهای مهم و نکته‌های کاربردی", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cities) { city -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("${city.fa}  ${city.zh}", fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(city.en, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(5.dp)); AssistChip(onClick = {}, label = { Text(city.tag) }); Spacer(Modifier.height(6.dp)); Text(city.desc); Spacer(Modifier.height(8.dp)); city.tips.forEach { Text("• $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }} }
        }
    }
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Chinese", text))
}

private fun share(context: Context, text: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "اشتراک‌گذاری"))
}

private fun speak(context: Context, text: String) {
    val tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = ttsLanguage(tts)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chiniyar")
            else tts.shutdown()
        }
    }
}

private fun ttsLanguage(tts: TextToSpeech): Int = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
