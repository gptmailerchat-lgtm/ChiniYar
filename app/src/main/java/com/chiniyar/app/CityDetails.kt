package com.chiniyar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChiniYarCityDetail(
    val cityZh: String,
    val cityEn: String,
    val cityFa: String,
    val intro: String,
    val products: String,
    val attractions: String,
    val travelTip: String
)

private val cityDetails = listOf(
    ChiniYarCityDetail(
        "上海", "Shanghai", "شانگهای",
        "یکی از مهم‌ترین کلان‌شهرهای چین و یک مرکز بزرگ مالی، تجاری، دریایی و فناوری است. شانگهای برای سفر کاری، نمایشگاه‌ها، خرید و دیدن ترکیب معماری تاریخی و مدرن مقصدی بسیار مهم است.",
        "لوازم الکترونیکی و فناوری، ماشین‌آلات و قطعات صنعتی، منسوجات، پوشاک، کالاهای مصرفی، تجهیزات پزشکی و محصولات نمایشگاهی.",
        "The Bund (外滩)، باغ یو (豫园)، خیابان نانجینگ، برج شانگهای، منطقه Lujiazui، موزه شانگهای و شهر آبی Zhujiajiao.",
        "برای خرید و تجارت، منطقه‌های Pudong، Huangpu و اطراف بازارهای عمده‌فروشی و نمایشگاه‌های شهر مهم‌تر هستند."
    ),
    ChiniYarCityDetail(
        "深圳", "Shenzhen", "شنژن",
        "شنژن در جنوب چین و نزدیک هنگ‌کنگ قرار دارد و یکی از مهم‌ترین مراکز فناوری، تولید و زنجیره تأمین جهان است. این شهر برای خرید قطعات الکترونیکی و ارتباط با تولیدکنندگان بسیار شناخته‌شده است.",
        "قطعات الکترونیکی، برد و ماژول، لوازم جانبی موبایل، تجهیزات هوشمند، محصولات IoT، تجهیزات شبکه، پهپاد و تجهیزات تولید صنعتی.",
        "پنجره جهان (世界之窗)، Splendid China، پارک Lianhuashan، منطقه OCT، ساحل Dameisha و مرکز شهر Futian.",
        "برای خرید الکترونیک، Huaqiangbei یکی از معروف‌ترین مناطق شهر است و برای کسب‌وکار فناوری ارزش ویژه‌ای دارد."
    ),
    ChiniYarCityDetail(
        "广州", "Guangzhou", "گوانگژو",
        "گوانگژو مرکز مهم تجاری جنوب چین و یکی از اصلی‌ترین شهرهای نمایشگاهی کشور است. شهر برای تجارت بین‌المللی، پوشاک، کالاهای مصرفی و ارتباط با کارخانه‌های جنوب چین اهمیت زیادی دارد.",
        "پوشاک و پارچه، کیف و کفش، لوازم خانگی، کالاهای مصرفی، قطعات خودرو، تجهیزات روشنایی، مبلمان و محصولات نمایشگاهی.",
        "Canton Tower، جزیره Shamian، Chen Clan Ancestral Hall، خیابان Beijing Road، مروارید رودخانه و Baiyun Mountain.",
        "در فصل نمایشگاه Canton Fair، شهر بسیار شلوغ می‌شود؛ رزرو هتل و حمل‌ونقل را از قبل انجام دهید."
    ),
    ChiniYarCityDetail(
        "义乌", "Yiwu", "ییوو",
        "ییوو یکی از مشهورترین مراکز عمده‌فروشی کالاهای کوچک در جهان است و برای خریداران بین‌المللی به‌خصوص در کالاهای متنوع مصرفی اهمیت زیادی دارد.",
        "اسباب‌بازی، لوازم تزئینی، اکسسوری، لوازم خانه، لوازم تحریر، هدایا، بدلیجات، کالاهای فصلی و انواع محصولات سبک مصرفی.",
        "Yiwu International Trade City، بازارهای Futian، Luobinwang Park، Yiwu Museum و Yiwu Wetland.",
        "برای خرید عمده، منطقه International Trade City بسیار مهم است؛ قبل از سفر دسته‌بندی و تأمین‌کنندگان موردنظر را مشخص کنید."
    ),
    ChiniYarCityDetail(
        "北京", "Beijing", "پکن",
        "پکن پایتخت چین و یکی از مهم‌ترین مراکز سیاسی، فرهنگی، تاریخی و آموزشی کشور است. برای گردشگری تاریخی و شناخت فرهنگ چین یکی از مهم‌ترین مقصدهاست.",
        "تجهیزات فناوری، محصولات فرهنگی، صنایع‌دستی، مواد غذایی، پوشاک، محصولات مصرفی و کالاهای مرتبط با سازمان‌ها و شرکت‌های بزرگ.",
        "شهر ممنوعه، میدان Tiananmen، دیوار بزرگ چین، Temple of Heaven، Summer Palace، hutongهای قدیمی و کاخ تابستانی.",
        "برای بازدید از جاذبه‌های اصلی، بلیت و زمان ورود برخی اماکن را از قبل بررسی و رزرو کنید."
    ),
    ChiniYarCityDetail(
        "杭州", "Hangzhou", "هانگژو",
        "هانگژو شهری مهم در شرق چین است که به دریاچه غربی، صنعت فناوری و تجارت دیجیتال شهرت دارد و دفتر یا فعالیت شرکت‌های بزرگ اینترنتی در آن دیده می‌شود.",
        "فناوری و تجارت الکترونیک، چای Longjing، ابریشم، محصولات فرهنگی، صنایع‌دستی، پوشاک و کالاهای مرتبط با گردشگری.",
        "West Lake، معبد Lingyin، Longjing Tea Village، خیابان Hefang و Xixi Wetland.",
        "بازدید از West Lake در ساعات مختلف روز تجربه متفاوتی دارد و برای عکاسی صبح و عصر بسیار مناسب است."
    )
)

private fun detailForCity(city: City): ChiniYarCityDetail? = cityDetails.firstOrNull {
    it.cityZh == city.zh || it.cityEn == city.en || it.cityFa == city.fa
}

@Composable
fun EnhancedCityDetailsCard(city: City) {
    val detail = detailForCity(city) ?: return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFC62828), Color(0xFFE65100))
                        )
                    )
                    .padding(18.dp)
            ) {
                Text(detail.cityFa, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("${detail.cityEn} · ${detail.cityZh}", color = Color.White.copy(alpha = .9f))
                Spacer(Modifier.height(6.dp))
                Text(detail.intro, color = Color.White, lineHeight = 22.sp)
            }
        }

        CityInfoSection("🛍 محصولات و تجارت", detail.products)
        CityInfoSection("📍 جاهای دیدنی", detail.attractions)
        CityInfoSection("💡 نکته کاربردی", detail.travelTip)
    }
}

@Composable
private fun CityInfoSection(title: String, text: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(7.dp))
            Text(text, lineHeight = 21.sp)
        }
    }
}

@Composable
fun MetroCityHero(city: MetroCity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF263238), Color(0xFF546E7A))
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text("🚇 متروی ${city.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(city.zh, fontSize = 17.sp, color = Color.White.copy(alpha = .95f))
        Spacer(Modifier.height(5.dp))
        Text("نقشه راهنما · ایستگاه‌های کلیدی · اطلاعات سریع سفر", color = Color.White.copy(alpha = .85f))
    }
}
