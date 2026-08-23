package com.chiniyar.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
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
data class MetroLine(val name: String, val color: Color, val stations: List<String>, val route: List<Pair<Float, Float>>)
data class MetroCity(val name: String, val zh: String, val mapUrl: String, val lines: List<MetroLine>)

private const val YAJING_URL = "https://yajingchinese.ir/"

private val phrases = listOf(
    Phrase("你好", "Nǐ hǎo", "سلام", "مکالمه"),
    Phrase("早上好", "Zǎoshang hǎo", "صبح بخیر", "مکالمه"),
    Phrase("晚上好", "Wǎnshang hǎo", "شب بخیر", "مکالمه"),
    Phrase("谢谢", "Xièxie", "ممنون", "مکالمه"),
    Phrase("不客气", "Bú kèqi", "خواهش می‌کنم", "مکالمه"),
    Phrase("对不起", "Duìbuqǐ", "ببخشید / متأسفم", "مکالمه"),
    Phrase("没关系", "Méi guānxi", "اشکالی ندارد", "مکالمه"),
    Phrase("请慢一点说", "Qǐng màn yìdiǎn shuō", "لطفاً کمی آهسته‌تر صحبت کنید", "مکالمه"),
    Phrase("我听不懂", "Wǒ tīng bù dǒng", "متوجه نمی‌شوم", "مکالمه"),
    Phrase("请再说一遍", "Qǐng zài shuō yí biàn", "لطفاً دوباره بگویید", "مکالمه"),
    Phrase("多少钱？", "Duōshao qián?", "چند قیمت است؟", "خرید"),
    Phrase("太贵了", "Tài guì le", "خیلی گران است", "خرید"),
    Phrase("可以便宜一点吗？", "Kěyǐ piányi yìdiǎn ma?", "می‌شود کمی ارزان‌تر حساب کنید؟", "مذاکره"),
    Phrase("最低价是多少？", "Zuìdī jià shì duōshao?", "کمترین قیمت چقدر است؟", "مذاکره"),
    Phrase("我要这个", "Wǒ yào zhège", "این را می‌خواهم", "خرید"),
    Phrase("有别的颜色吗？", "Yǒu bié de yánsè ma?", "رنگ دیگری دارید؟", "خرید"),
    Phrase("可以刷卡吗？", "Kěyǐ shuākǎ ma?", "می‌شود با کارت پرداخت کرد؟", "پرداخت"),
    Phrase("可以用支付宝吗？", "Kěyǐ yòng Zhīfùbǎo ma?", "می‌شود با Alipay پرداخت کرد؟", "پرداخت"),
    Phrase("洗手间在哪里？", "Xǐshǒujiān zài nǎlǐ?", "دستشویی کجاست؟", "سفر"),
    Phrase("火车站在哪里？", "Huǒchēzhàn zài nǎlǐ?", "ایستگاه قطار کجاست؟", "حمل‌ونقل"),
    Phrase("地铁站在哪里？", "Dìtiě zhàn zài nǎlǐ?", "ایستگاه مترو کجاست؟", "حمل‌ونقل"),
    Phrase("我要去这里", "Wǒ yào qù zhèlǐ", "می‌خواهم به اینجا بروم", "تاکسی"),
    Phrase("请带我去这个地址", "Qǐng dài wǒ qù zhège dìzhǐ", "لطفاً مرا به این آدرس ببرید", "تاکسی"),
    Phrase("我迷路了", "Wǒ mílù le", "گم شده‌ام", "سفر"),
    Phrase("今天几点开门？", "Jīntiān jǐ diǎn kāimén?", "امروز چه ساعتی باز می‌شود؟", "خدمات"),
    Phrase("明天几点开始？", "Míngtiān jǐ diǎn kāishǐ?", "فردا چه ساعتی شروع می‌شود؟", "خدمات"),
    Phrase("我需要帮助", "Wǒ xūyào bāngzhù", "به کمک نیاز دارم", "اضطراری"),
    Phrase("请帮我", "Qǐng bāng wǒ", "لطفاً به من کمک کنید", "اضطراری"),
    Phrase("我要看样品", "Wǒ yào kàn yàngpǐn", "می‌خواهم نمونه را ببینم", "تجارت"),
    Phrase("如果买一千个，价格是多少？", "Rúguǒ mǎi yìqiān gè, jiàgé shì duōshao?", "اگر هزار عدد بخرم قیمت چقدر می‌شود؟", "تجارت"),
    Phrase("最小订单量是多少？", "Zuìxiǎo dìngdān liàng shì duōshao?", "حداقل تعداد سفارش چقدر است؟", "تجارت"),
    Phrase("可以发到伊朗吗？", "Kěyǐ fā dào Yīlǎng ma?", "می‌توانید به ایران ارسال کنید؟", "تجارت"),
    Phrase("请给我名片", "Qǐng gěi wǒ míngpiàn", "لطفاً کارت ویزیت بدهید", "تجارت"),
    Phrase("我们可以加微信吗？", "Wǒmen kěyǐ jiā Wēixìn ma?", "می‌توانیم در WeChat همدیگر را اضافه کنیم؟", "تجارت")
)

private val cities = listOf(
    City("上海", "Shanghai", "شانگهای", "مرکز مهم مالی، تجاری و فناوری چین."),
    City("深圳", "Shenzhen", "شنژن", "قطب فناوری، تولید و الکترونیک چین."),
    City("广州", "Guangzhou", "گوانگژو", "مرکز تجارت جنوب چین و مقصد مهم نمایشگاهی."),
    City("义乌", "Yiwu", "ییوو", "مرکز معروف بازارهای عمده‌فروشی و کالاهای مصرفی."),
    City("北京", "Beijing", "پکن", "پایتخت چین و مرکز مهم تاریخی و فرهنگی."),
    City("杭州", "Hangzhou", "هانگژو", "مرکز مهم فناوری و تجارت در شرق چین.")
)

private val metroCities = listOf(
    MetroCity("Shanghai", "上海", "https://sg.trip.com/guide/transport/shanghai-metro.html", listOf(
        MetroLine("2", Color(0xFF7CB342), listOf("虹桥火车站", "中山公园", "人民广场", "南京东路", "陆家嘴", "浦东国际机场"), listOf(.08f to .70f, .24f to .55f, .40f to .45f, .56f to .35f, .72f to .25f, .90f to .12f)),
        MetroLine("10", Color(0xFF7E57C2), listOf("虹桥火车站", "上海动物园", "南京东路", "豫园", "新天地"), listOf(.10f to .78f, .20f to .65f, .52f to .40f, .60f to .58f, .50f to .78f)),
        MetroLine("11", Color(0xFF8D6E63), listOf("上海赛车场", "徐家汇", "交通大学", "迪士尼"), listOf(.12f to .20f, .34f to .32f, .48f to .50f, .84f to .82f))
    )),
    MetroCity("Beijing", "北京", "https://zh.beijingmap360.com/", listOf(
        MetroLine("1", Color(0xFFD32F2F), listOf("苹果园", "天安门西", "天安门东", "王府井", "国贸"), listOf(.08f to .45f, .34f to .45f, .50f to .45f, .66f to .45f, .84f to .45f)),
        MetroLine("2", Color(0xFF1976D2), listOf("西直门", "复兴门", "北京站", "建国门", "东直门"), listOf(.18f to .25f, .30f to .25f, .58f to .25f, .72f to .25f, .82f to .16f)),
        MetroLine("10", Color(0xFF00897B), listOf("巴沟", "海淀黄庄", "国贸", "三元桥", "知春路"), listOf(.16f to .76f, .34f to .64f, .58f to .54f, .76f to .66f, .60f to .82f))
    )),
    MetroCity("Guangzhou", "广州", "https://www.trip.com/guide/transport/guangzhou-metro-map.html", listOf(
        MetroLine("1", Color(0xFFE5B700), listOf("西塱", "公园前", "体育西路", "广州东站"), listOf(.10f to .60f, .40f to .46f, .65f to .48f, .86f to .30f)),
        MetroLine("3", Color(0xFFFF8F00), listOf("机场南", "体育西路", "珠江新城", "汉溪长隆", "番禺广场"), listOf(.86f to .10f, .65f to .48f, .60f to .62f, .48f to .76f, .26f to .88f)),
        MetroLine("8", Color(0xFF00838F), listOf("文化公园", "客村", "琶洲", "万胜围"), listOf(.14f to .42f, .38f to .55f, .62f to .54f, .84f to .56f))
    )),
    MetroCity("Shenzhen", "深圳", "https://sz.bendibao.com/jt/2021823/871845.htm", listOf(
        MetroLine("1", Color(0xFF43A047), listOf("罗湖", "老街", "大剧院", "会展中心", "世界之窗", "机场东"), listOf(.14f to .78f, .24f to .67f, .36f to .58f, .48f to .56f, .64f to .50f, .92f to .30f)),
        MetroLine("4", Color(0xFF8E24AA), listOf("福田口岸", "会展中心", "少年宫", "深圳北站", "牛湖"), listOf(.16f to .88f, .46f to .56f, .50f to .42f, .58f to .28f, .86f to .16f)),
        MetroLine("11", Color(0xFF6D4C41), listOf("福田", "车公庙", "机场", "碧头"), listOf(.30f to .30f, .44f to .44f, .70f to .28f, .90f to .14f))
    )),
    MetroCity("Hangzhou", "杭州", "https://www.chinatravel.com/hangzhou/metro-transport", listOf(
        MetroLine("1", Color(0xFFE53935), listOf("湘湖", "龙翔桥", "凤起路", "杭州东站", "萧山国际机场"), listOf(.10f to .74f, .32f to .60f, .48f to .44f, .66f to .30f, .90f to .16f)),
        MetroLine("5", Color(0xFF8E24AA), listOf("金星", "城站", "江城路", "姑娘桥"), listOf(.12f to .30f, .42f to .42f, .56f to .60f, .78f to .78f)),
        MetroLine("19", Color(0xFF00897B), listOf("杭州西站", "创景路", "杭州东站", "萧山国际机场"), listOf(.10f to .16f, .34f to .25f, .66f to .30f, .90f to .16f))
    ))
)

@Composable
fun ChiniYarApp() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFC62828), secondary = Color(0xFFD97706))) {
        var selectedTab by remember { mutableIntStateOf(0) }
        Scaffold(bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Text("⌂") }, label = { Text("خانه") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Text("📷") }, label = { Text("مترجم") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("🀄") }, label = { Text("عبارات") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Text("🚇") }, label = { Text("مترو") })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4 }, icon = { Text("🇨🇳") }, label = { Text("چین") })
            }
        }) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(Modifier.padding(padding))
                1 -> TranslateScreen(Modifier.padding(padding))
                2 -> PhraseScreen(Modifier.padding(padding))
                3 -> MetroScreen(Modifier.padding(padding))
                else -> ChinaScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun ScenicBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFFF8F0), Color(0xFFF5F9FF))))
            drawCircle(Color(0x22C62828), size.minDimension * .42f, Offset(size.width * .82f, size.height * .12f))
            drawCircle(Color(0x227B1FA2), size.minDimension * .32f, Offset(size.width * .12f, size.height * .72f))
            val skyline = Path().apply {
                moveTo(0f, size.height * .88f)
                lineTo(size.width * .12f, size.height * .74f)
                lineTo(size.width * .20f, size.height * .82f)
                lineTo(size.width * .32f, size.height * .66f)
                lineTo(size.width * .42f, size.height * .80f)
                lineTo(size.width * .54f, size.height * .60f)
                lineTo(size.width * .64f, size.height * .76f)
                lineTo(size.width * .78f, size.height * .58f)
                lineTo(size.width, size.height * .72f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(skyline, Color(0x11000000))
        }
        content()
    }
}

@Composable
private fun HomeScreen(modifier: Modifier) {
    val context = LocalContext.current
    ScenicBackground {
        LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🇨🇳", fontSize = 56.sp)
                    Text("چینی‌یار", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("دستیار سفر، مکالمه و ترجمه چینی")
                }
            }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
                Text("برای سفر به چین آماده‌ای؟", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("OCR چینی، ترجمه، عبارات کاربردی، تلفظ و نقشه‌های آفلاین مترو در یک اپ.")
            }}}
            item {
                Card(Modifier.fillMaxWidth().clickable { openUrl(context, YAJING_URL) }, shape = RoundedCornerShape(20.dp)) {
                    Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFE65100)))).padding(18.dp)) {
                        Column {
                            Text("🎓 آموزش زبان چینی با Yajing Chinese", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("دوره‌ها و منابع آموزش زبان چینی", color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Text("yajingchinese.ir", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
    val translator = remember { Translation.getClient(TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.CHINESE).setTargetLanguage(TranslateLanguage.PERSIAN).build()) }

    DisposableEffect(Unit) { onDispose { recognizer.close(); translator.close() } }

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

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(::processUri) }

    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("مترجم تصویری چینی", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("یک عکس از نوشته چینی انتخاب کنید.") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { galleryLauncher.launch("image/*") }) { Text("انتخاب عکس") }
            Button(onClick = { translate() }, enabled = recognizedText.isNotBlank() && !busy) { Text("ترجمه") }
        }}
        imageBitmap?.let { bitmap -> item { Image(bitmap.asImageBitmap(), "تصویر انتخاب‌شده", Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop) } }
        if (recognizedText.isNotBlank()) item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("متن چینی", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(recognizedText)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { copy(context, recognizedText) }) { Text("کپی") }
                TextButton(onClick = { speakChinese(context, recognizedText) }) { Text("🔊") }
            }
        }}}
        if (translatedText.isNotBlank()) item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            Text("ترجمه فارسی", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Text(translatedText)
            TextButton(onClick = { copy(context, translatedText) }) { Text("کپی") }
        }}}
        if (message.isNotBlank()) item { Text(message) }
        if (busy) item { Text("در حال پردازش…") }
    }
}

@Composable
private fun PhraseScreen(modifier: Modifier) {
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("عبارات کاربردی", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("بیش از ۳۰ عبارت برای سفر، خرید و تجارت") }
        items(phrases) { phrase ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(phrase.zh, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(phrase.pinyin, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp)); Text(phrase.fa)
                Text(phrase.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { speakChinese(context, phrase.zh) }) { Text("🔊 تلفظ") }
            }}
        }
    }
}

@Composable
private fun MetroScreen(modifier: Modifier) {
    var selectedCity by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val city = metroCities[selectedCity]
    ScenicBackground {
        LazyColumn(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("نقشه متروی آفلاین ۵ شهر مهم چین", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("نام تمام ایستگاه‌های نمایش‌داده‌شده مستقیم روی نقشه نوشته شده است.") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                metroCities.forEachIndexed { index, item -> TextButton(onClick = { selectedCity = index }) { Text(if (selectedCity == index) "● ${item.name}" else item.name) } }
            }}
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp)) {
                Text("${city.name}  ${city.zh}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                MetroMap(city)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { openUrl(context, city.mapUrl) }) { Text("نقشه کامل آنلاین") }
                Text("راهنمای داخل اپ کاملاً آفلاین است؛ لینک بالا فقط برای نقشه کامل آنلاین است.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }}}
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text("ایستگاه‌های این نسخه", fontWeight = FontWeight.Bold)
                city.lines.forEach { line -> Text("خط ${line.name}: ${line.stations.joinToString(" ← ")}", fontSize = 13.sp) }
            }}}
        }
    }
}

@Composable
private fun MetroMap(city: MetroCity) {
    Canvas(Modifier.fillMaxWidth().height(430.dp).clip(RoundedCornerShape(18.dp)).background(Color.White)) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFF222222).toArgb()
            textSize = 11.dp.toPx()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        city.lines.forEachIndexed { lineIndex, line ->
            val points = line.route.map { Offset(it.first * size.width, it.second * size.height) }
            for (i in 0 until points.size - 1) {
                drawLine(line.color, points[i], points[i + 1], 10f, StrokeCap.Round)
                drawLine(Color.White, points[i], points[i + 1], 3f, StrokeCap.Round)
            }
            points.forEachIndexed { stationIndex, point ->
                drawCircle(Color.White, 8f, point)
                drawCircle(line.color, 5f, point)
                drawCircle(line.color.copy(alpha = .15f), 16f, point, style = Stroke(width = 2f))

                val stationName = line.stations.getOrNull(stationIndex).orEmpty()
                if (stationName.isNotEmpty()) {
                    val labelOffsetY = if ((stationIndex + lineIndex) % 2 == 0) -12f else 20f
                    val labelOffsetX = if (point.x < size.width * .55f) 9f else -9f
                    labelPaint.textAlign = if (point.x < size.width * .55f) Paint.Align.LEFT else Paint.Align.RIGHT
                    drawContext.canvas.nativeCanvas.drawText(
                        stationName,
                        point.x + labelOffsetX,
                        point.y + labelOffsetY,
                        labelPaint
                    )
                }
            }
        }
        drawRect(Color(0xFFE0E0E0), style = Stroke(width = 2f))
    }
}

@Composable
private fun ChinaScreen(modifier: Modifier) {
    ScenicBackground {
        LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("شهرهای مهم چین", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            items(cities) { city ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                    Text("${city.fa}  ${city.zh}", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(city.en, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Text(city.desc)
                }}
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
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
