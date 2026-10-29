package com.example

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ==============================================================================
// მონაცემთა მოდელები
// ==============================================================================
data class ProductItem(
    val id: Int,
    val title: String,
    val price: String,
    val category: String,
    val type: String = "product",
    val subcategory: String = "",
    val imageUrl: String,
    val shortDescription: String = "",
    val description: String = "უმაღლესი ხარისხის OLED ეკრანი თხელი ჩარჩოთი, True Tone მხარდაჭერით და ორიგინალური სენსორული მგრძნობელობით.",
    val features: List<String> = emptyList(),
    val targetAudience: String = "",
    val offerType: String = "",
    val duration: String = "",
    val accountType: String = "",
    val activationTerms: String = "",
    val officialUrl: String = "",
    val termsUrl: String = "",
    val lastVerifiedAt: String = "2026-10-25",
    val searchKeywords: String = "",
    val isFeatured: Boolean = false,
    val displayOrder: Int = 0,
    val rating: Double = 4.9,
    var isFavorite: Boolean = false,
    val inStock: Boolean = true,
    val brand: String = "Apple",
    val compatibleModels: String = "iPhone 16 Pro Max",
    val warranty: String = "6 თვე",
    val firestoreDocId: String = "",
    val brandColorHex: String = "#FF7A00",
    val brandBadgeText: String = ""
)

data class CartItem(
    val product: ProductItem,
    var quantity: Int = 1,
    var selectedQuality: String = "Original"
)

data class ChatMessage(
    val sender: String, // "user" ან "ai"
    val text: String,
    val recommendedProducts: List<ProductItem> = emptyList(),
    val id: String = java.util.UUID.randomUUID().toString()
)

internal var hasCompletedInitialLaunch = false

class MainActivity : ComponentActivity() {
    private val pendingDeepLinkIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferencesManager.init(this)
        if (savedInstanceState != null) {
            hasCompletedInitialLaunch = true
        }
        pendingDeepLinkIntent.value = intent
        enableEdgeToEdge()
        setContent {
            RepairProTheme(darkTheme = AppPreferencesManager.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppThemeColorAdapter.adaptPageBackground(Color(0xFF0A0B0E))
                ) {
                    BekaServiceApp(
                        deepLinkIntent = pendingDeepLinkIntent.value,
                        onDeepLinkConsumed = { pendingDeepLinkIntent.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkIntent.value = intent
    }
}

// ==============================================================================
// ოფიციალური BEKASERVICE Seamless Launch Splash (Uniform Fade-In + Brand Wordmark)
// ==============================================================================
@Composable
fun VideoSplashScreen(
    isAppReady: Boolean = true,
    onFinish: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    var minTransitionDone by remember { mutableStateOf(false) }

    val smoothFadeEasing = remember { androidx.compose.animation.core.CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f) }

    val fadeAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 680, easing = smoothFadeEasing),
        label = "SplashUniformFadeAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.92f,
        animationSpec = tween(durationMillis = 680, easing = smoothFadeEasing),
        label = "SplashIconExpandScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (startAnim) 0.14f else 0f,
        animationSpec = tween(durationMillis = 680, easing = smoothFadeEasing),
        label = "SplashGlowAlpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(950L)
        minTransitionDone = true
    }

    LaunchedEffect(minTransitionDone, isAppReady) {
        if (minTransitionDone && isAppReady) {
            hasCompletedInitialLaunch = true
            onFinish()
        }
    }

    // Fallback safety guard so initialization errors or slow network never block launch
    LaunchedEffect(Unit) {
        delay(1250L)
        hasCompletedInitialLaunch = true
        onFinish()
    }

    CompositionLocalProvider(LocalPreserveWhiteText provides true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0B0E))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    hasCompletedInitialLaunch = true
                    onFinish()
                }
                .testTag("splash_screen_container"),
            contentAlignment = Alignment.Center
        ) {
            // Subtle ambient warm radial light behind the logo lockup
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer { alpha = glowAlpha }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF7A00),
                                Color(0xFFFF7A00).copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = fadeAlpha
                        scaleX = scale
                        scaleY = scale
                    }
                    .testTag("splash_lockup_column")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bekaservice_splash_logo),
                    contentDescription = "BEKASERVICE",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(124.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .testTag("splash_logo_image")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.testTag("splash_brand_text_container")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.testTag("splash_brand_title_row")
                    ) {
                        Text(
                            text = "ბექა",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 25.sp,
                            lineHeight = 26.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            style = TextStyle(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        Text(
                            text = "სერვისი",
                            color = Color(0xFFFF7A00),
                            fontWeight = FontWeight.Black,
                            fontSize = 25.sp,
                            lineHeight = 26.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            style = TextStyle(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }

                    Text(
                        text = "სანდო სერვისი",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 13.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        ),
                        modifier = Modifier.testTag("splash_brand_subtitle")
                    )
                }
            }
        }
    }
}

// ==============================================================================
// საძიებო სისტემის ოპტიმიზაცია (Smart Transliteration, Stemming & Multi-Field Search)
// ==============================================================================
fun transliterateGeoToLat(input: String): String {
    val map = mapOf(
        'ა' to "a", 'ბ' to "b", 'გ' to "g", 'დ' to "d", 'ე' to "e", 'ვ' to "v", 'ზ' to "z",
        'თ' to "t", 'ი' to "i", 'კ' to "k", 'ლ' to "l", 'მ' to "m", 'ნ' to "n", 'ო' to "o",
        'პ' to "p", 'ჟ' to "zh", 'რ' to "r", 'ს' to "s", 'ტ' to "t", 'უ' to "u", 'ფ' to "p",
        'ქ' to "k", 'ღ' to "gh", 'ყ' to "q", 'შ' to "sh", 'ჩ' to "ch", 'ც' to "ts", 'ძ' to "dz",
        'წ' to "ts", 'ჭ' to "ch", 'ხ' to "kh", 'ჯ' to "j", 'ჰ' to "h"
    )
    val sb = StringBuilder()
    for (ch in input) {
        sb.append(map[ch] ?: ch)
    }
    return sb.toString()
}

fun transliterateLatToGeo(input: String): String {
    var res = input
    val multiMap = listOf(
        "sh" to "შ", "ch" to "ჩ", "zh" to "ჟ", "gh" to "ღ", "kh" to "ხ", "dz" to "ძ", "ts" to "ც"
    )
    for ((lat, geo) in multiMap) {
        res = res.replace(lat, geo)
    }
    val singleMap = mapOf(
        'a' to 'ა', 'b' to 'ბ', 'g' to 'გ', 'd' to 'დ', 'e' to 'ე', 'v' to 'ვ', 'z' to 'ზ',
        't' to 'თ', 'i' to 'ი', 'k' to 'კ', 'l' to 'ლ', 'm' to 'მ', 'n' to 'ნ', 'o' to 'ო',
        'p' to 'პ', 'r' to 'რ', 's' to 'ს', 'u' to 'უ', 'f' to 'ფ', 'q' to 'ქ', 'x' to 'ხ',
        'j' to 'ჯ', 'h' to 'ჰ', 'y' to 'ყ', 'w' to 'წ', 'c' to 'ც'
    )
    val sb = StringBuilder()
    for (ch in res) {
        sb.append(singleMap[ch] ?: ch)
    }
    return sb.toString()
}

fun normalizeGeorgianWord(word: String): String {
    var s = word.trim().lowercase()
    if (s.length >= 4) {
        if (s.endsWith("ის") || s.endsWith("ზე") || s.endsWith("ში") || s.endsWith("ით")) {
            s = s.substring(0, s.length - 2)
        } else if (s.endsWith("ი") || s.endsWith("ს")) {
            s = s.substring(0, s.length - 1)
        }
    }
    return s
}

fun optimizeSearchFilter(
    products: List<ProductItem>,
    query: String,
    categoryFilter: String = "ყველა",
    brandFilter: String = "ყველა",
    sortBy: String = "default",
    subcategoryFilter: String = "ყველა"
): List<ProductItem> {
    val cleanQuery = query.trim().lowercase()

    if (cleanQuery.isEmpty()) {
        val baseFiltered = products.filter { item ->
            val matchesCategory = when (categoryFilter) {
                "ყველა" -> true
                "ფავორიტები" -> item.isFavorite
                else -> item.category.equals(categoryFilter, ignoreCase = true)
            }
            val matchesSubcategory = subcategoryFilter == "ყველა" || item.subcategory.equals(subcategoryFilter, ignoreCase = true)
            val matchesBrand = brandFilter == "ყველა" || item.brand.equals(brandFilter, ignoreCase = true)
            matchesCategory && matchesSubcategory && matchesBrand
        }.let { list ->
            if (categoryFilter == "პროგრამული სერვისები") {
                list.sortedWith(
                    compareByDescending<ProductItem> { it.isFeatured }
                        .thenBy { if (it.displayOrder > 0) it.displayOrder else 999 }
                )
            } else list
        }
        return when (sortBy) {
            "price_asc" -> baseFiltered.sortedBy { it.price.filter { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }
            "price_desc" -> baseFiltered.sortedByDescending { it.price.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            "rating" -> baseFiltered.sortedByDescending { it.rating }
            else -> baseFiltered
        }
    }

    val phoneticSynonyms = mapOf(
        "aifon" to listOf("iphone", "apple", "აიფონ"),
        "aifoni" to listOf("iphone", "apple", "აიფონ"),
        "айфон" to listOf("iphone", "apple", "აიფონ"),
        "აიფონ" to listOf("iphone", "apple"),
        "iphone" to listOf("აიფონ", "apple"),
        "apple" to listOf("აიფონ", "iphone"),
        "samsung" to listOf("სამსუნგ", "galaxy"),
        "სამსუნგ" to listOf("samsung", "galaxy"),
        "galaxy" to listOf("სამსუნგ", "samsung"),
        "xiaomi" to listOf("შაომი", "redmi", "poco"),
        "შაომი" to listOf("xiaomi", "redmi", "poco"),
        "redmi" to listOf("რედმი", "xiaomi"),
        "poco" to listOf("პოკო", "xiaomi"),
        "pixel" to listOf("პიქსელ", "google"),
        "ekrani" to listOf("ეკრან", "oled", "display", "screen"),
        "ekran" to listOf("ეკრან", "oled", "display", "screen"),
        "ეკრან" to listOf("oled", "screen", "display", "დისპლეი", "amoled", "lcd"),
        "დისპლეი" to listOf("oled", "screen", "display", "ეკრან", "amoled"),
        "display" to listOf("ეკრან", "oled", "amoled", "დისპლეი"),
        "screen" to listOf("ეკრან", "oled", "amoled", "დისპლეი"),
        "oled" to listOf("ეკრან", "დისპლეი", "amoled", "screen"),
        "amoled" to listOf("ეკრან", "oled", "დისპლეი", "screen"),
        "elementi" to listOf("ელემენტ", "battery", "ბატარე"),
        "batarea" to listOf("ელემენტ", "battery", "ბატარე"),
        "ელემენტ" to listOf("battery", "ბატარე", "აკუმულატორ"),
        "ბატარე" to listOf("battery", "ელემენტ", "აკუმულატორ"),
        "battery" to listOf("ელემენტ", "ბატარე", "აკუმულატორ"),
        "damteni" to listOf("type-c", "port", "charger", "დამტენ", "პორტ"),
        "dasteni" to listOf("type-c", "port", "charger", "დამტენ", "პორტ"),
        "პორტ" to listOf("type-c", "port", "charger", "დამტენ", "შლიფ"),
        "დამტენ" to listOf("type-c", "port", "charger", "პორტ"),
        "kamera" to listOf("camera", "კამერ", "ობიექტივ", "ლინზ"),
        "კამერ" to listOf("camera", "ობიექტივ", "ლინზ"),
        "camera" to listOf("კამერ", "ობიექტივ"),
        "korpusi" to listOf("case", "housing", "ჩარჩო", "ხუფ", "შუშ", "კორპუს"),
        "კორპუს" to listOf("case", "housing", "ჩარჩო", "ხუფ", "შუშ"),
        "shusha" to listOf("glass", "back glass", "კორპუს", "შუშ"),
        "შუშ" to listOf("glass", "back glass", "კორპუს"),
        "glass" to listOf("შუშ", "კორპუს"),
        "dinamiki" to listOf("speaker", "დინამიკ", "აუდიო"),
        "დინამიკ" to listOf("speaker", "აუდიო"),
        "speaker" to listOf("დინამიკ", "აუდიო"),
        "vibro" to listOf("taptic", "ვიბრო", "ვიბრაცი"),
        "ვიბრო" to listOf("taptic", "ვიბრაცი"),
        "taptic" to listOf("ვიბრო", "ვიბრაცი"),
        "pro" to listOf("პრო"),
        "პრო" to listOf("pro"),
        "max" to listOf("მაქს"),
        "მაქს" to listOf("max"),
        "plus" to listOf("პლუს"),
        "პლუს" to listOf("plus"),
        "ultra" to listOf("ულტრა"),
        "ულტრა" to listOf("ultra"),
        // 24 პროგრამული სერვისის სინონიმები
        "chatgpt" to listOf("chatgpt", "chatgpt plus", "chatgpt pro", "ჩატჯიპიტი", "ჯიპიტი", "openai", "gpt-4o", "o3"),
        "ჩატჯიპიტ" to listOf("chatgpt", "chatgpt plus", "chatgpt pro", "openai", "ჯიპიტი"),
        "ჯიპიტ" to listOf("chatgpt", "chatgpt plus", "chatgpt pro", "openai", "ჩატჯიპიტი"),
        "openai" to listOf("chatgpt", "chatgpt plus", "chatgpt pro", "ოპენაი"),
        "ოპენა" to listOf("openai", "chatgpt", "chatgpt plus", "chatgpt pro"),
        "google" to listOf("გუგლ", "google ai", "google storage", "gmail", "gemini", "pixel"),
        "გუგლ" to listOf("google", "google ai", "google storage", "gmail", "gemini", "pixel"),
        "ჯემინ" to listOf("gemini", "google ai pro", "google ai ultra", "გუგლ აი"),
        "gemini" to listOf("ჯემინი", "google ai pro", "google ai ultra"),
        "grok" to listOf("გროკი", "სუპერგროკი", "supergrok", "supergrok heavy", "xai"),
        "გროკ" to listOf("grok", "supergrok", "supergrok heavy", "xai"),
        "სუპერგროკ" to listOf("supergrok", "grok", "supergrok heavy", "xai"),
        "claude" to listOf("კლოდი", "claude pro", "private account", "shared account", "anthropic", "sonnet"),
        "კლოდ" to listOf("claude", "claude pro", "anthropic", "sonnet", "კლოდი"),
        "perplexity" to listOf("პერპლექსიტი", "perplexity pro"),
        "პერპლექსიტ" to listOf("perplexity", "perplexity pro"),
        "cursor" to listOf("კურსორი", "cursor ai pro", "ide"),
        "კურსორ" to listOf("cursor", "cursor ai pro", "ide"),
        "lovable" to listOf("ლავაბლ", "lovable pro"),
        "ლავაბლ" to listOf("lovable", "lovable pro"),
        "gmail" to listOf("ჯიმეილი", "gmail phone verified account", "ელფოსტა", "მეილი"),
        "ჯიმეილ" to listOf("gmail", "gmail phone verified account", "ელფოსტა", "მეილი"),
        "storage" to listOf("მეხსიერება", "სტორეჯი", "google storage", "2tb", "5tb", "bundle"),
        "მეხსიერებ" to listOf("storage", "google storage", "2tb", "5tb", "google one"),
        "სტორეჯ" to listOf("storage", "google storage", "2tb", "5tb", "მეხსიერება"),
        "bundle" to listOf("ბანდლი", "google ai pro + storage bundle"),
        "ბანდლ" to listOf("bundle", "google ai pro + storage bundle"),
        "capcut" to listOf("კაპკატი", "capcut pro", "ვიდეო მონტაჟი"),
        "კაპკატ" to listOf("capcut", "capcut pro", "ვიდეო მონტაჟი"),
        "adobe" to listOf("ადობე", "adobe creative cloud", "photoshop", "illustrator", "premiere", "ფოტოშოპი"),
        "ადობ" to listOf("adobe", "adobe creative cloud", "photoshop", "illustrator", "premiere"),
        "ფოტოშოპ" to listOf("photoshop", "adobe", "adobe creative cloud"),
        "canva" to listOf("კანვა", "canva pro", "დიზაინი"),
        "კანვ" to listOf("canva", "canva pro", "დიზაინი"),
        "youtube" to listOf("იუთუბი", "youtube premium", "youtube music"),
        "იუთუბ" to listOf("youtube", "youtube premium", "youtube music"),
        "spotify" to listOf("სპოტიფაი", "spotify premium", "მუსიკა"),
        "სპოტიფა" to listOf("spotify", "spotify premium", "მუსიკა"),
        "windows" to listOf("ვინდოუსი", "windows 11 pro retail key", "microsoft"),
        "ვინდოუს" to listOf("windows", "windows 11", "windows 11 pro", "retail key"),
        "office" to listOf("ოფისი", "office ltsc 2024", "office 2019", "microsoft 365", "word", "excel"),
        "ოფის" to listOf("office", "office ltsc 2024", "office 2019", "microsoft 365", "word", "excel"),
        "microsoft" to listOf("მაიკროსოფტი", "microsoft 365", "windows 11", "office"),
        "მაიკროსოფტ" to listOf("microsoft", "microsoft 365", "windows 11", "office"),
        "ლიცენზი" to listOf("license", "key", "retail key", "windows 11", "office", "გასაღები"),
        "გასაღებ" to listOf("key", "retail key", "windows 11", "office", "ლიცენზია")
    )

    val rawTokens = cleanQuery.split(Regex("[\\s,.-]+")).filter { it.isNotBlank() }

    val filtered = products.filter { item ->
        val itemSearchContent = buildString {
            append(item.title).append(" ")
            append(item.category).append(" ")
            append(item.subcategory).append(" ")
            append(item.brand).append(" ")
            append(item.shortDescription).append(" ")
            append(item.compatibleModels).append(" ")
            append(item.description).append(" ")
            append(item.offerType).append(" ")
            append(item.duration).append(" ")
            append(item.accountType).append(" ")
            append(item.searchKeywords).append(" ")
            append(item.features.joinToString(" "))
        }.lowercase()

        // 1. პირდაპირი სრული დამთხვევა
        val directMatch = itemSearchContent.contains(cleanQuery) ||
                itemSearchContent.contains(transliterateLatToGeo(cleanQuery)) ||
                itemSearchContent.contains(transliterateGeoToLat(cleanQuery))

        // 2. ტოკენების მიხედვით დამთხვევა (ყველა საძიებო სიტყვა უნდა მოიძებნოს)
        val allTokensMatch = if (rawTokens.isNotEmpty()) {
            rawTokens.all { token ->
                val normToken = normalizeGeorgianWord(token)
                val variants = mutableSetOf<String>()
                variants.add(token)
                variants.add(normToken)
                variants.add(transliterateGeoToLat(token))
                variants.add(transliterateLatToGeo(token))
                variants.add(normalizeGeorgianWord(transliterateLatToGeo(token)))

                phoneticSynonyms.forEach { (key, synonyms) ->
                    if (token.contains(key) || key.contains(token) || normToken.contains(key) || key.contains(normToken)) {
                        variants.addAll(synonyms)
                    }
                }

                variants.any { v -> v.isNotBlank() && itemSearchContent.contains(v) }
            }
        } else false

        val matchesSearch = directMatch || allTokensMatch

        // როდესაც მომხმარებელი წერს საძიებო ტექსტს, ეძებს მთელ ერთიან მაღაზიაში (გარდა "ფავორიტები" ფილტრისა)
        val matchesCategory = when {
            categoryFilter == "ფავორიტები" -> item.isFavorite
            categoryFilter == "ყველა" || cleanQuery.isNotEmpty() -> true
            else -> item.category.equals(categoryFilter, ignoreCase = true)
        }
        val matchesSubcategory = subcategoryFilter == "ყველა" || cleanQuery.isNotEmpty() || item.subcategory.equals(subcategoryFilter, ignoreCase = true)
        val matchesBrand = brandFilter == "ყველა" || cleanQuery.isNotEmpty() || item.brand.equals(brandFilter, ignoreCase = true)

        matchesSearch && matchesCategory && matchesSubcategory && matchesBrand
    }

    // შედეგების რანჟირება: ზუსტი სათაურის და საკვანძო სიტყვების დამთხვევები თავში!
    val sortedResults = filtered.sortedWith(
        compareByDescending<ProductItem> { it.title.lowercase().contains(cleanQuery) }
            .thenByDescending { it.searchKeywords.lowercase().contains(cleanQuery) }
            .thenByDescending { it.compatibleModels.lowercase().contains(cleanQuery) }
    )

    return when (sortBy) {
        "price_asc" -> sortedResults.sortedBy { it.price.filter { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE }
        "price_desc" -> sortedResults.sortedByDescending { it.price.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        "rating" -> sortedResults.sortedByDescending { it.rating }
        else -> sortedResults
    }
}

// ==============================================================================
// Xiaomi HyperOS 4-Inspired Glassmorphic Floating Top App Bar
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BekaGlassTopAppBar(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    selectedTab: Int,
    unreadNotificationsCount: Int,
    cartItemCount: Int,
    onMenuClick: () -> Unit,
    onTitleClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCartClick: () -> Unit
) {
    val topBarShape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .shadow(
                elevation = if (isDark) 12.dp else 6.dp,
                shape = topBarShape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color(0xFFFF7A00).copy(alpha = 0.18f)
            )
            .clip(topBarShape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color(0xFFFF7A00).copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.10f),
                            Color(0xFFFF7A00).copy(alpha = 0.28f)
                        )
                    } else {
                        listOf(
                            Color(0x26000000),
                            Color(0xFFFF7A00).copy(alpha = 0.30f),
                            Color(0x1A000000)
                        )
                    }
                ),
                shape = topBarShape
            )
            .testTag("glassmorphic_top_app_bar_container")
    ) {
        // Translucent frosted glass backing layer with subtle blur
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        Modifier.blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xE6161B26),
                                Color(0xD90E1118)
                            )
                        } else {
                            listOf(
                                Color(0xF2FFFFFF),
                                Color(0xF5F8FAFC)
                            )
                        }
                    )
                )
        )
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            navigationIcon = {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF16191E).copy(alpha = 0.85f) else Color(0xFFFFFFFF).copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp)
                        .size(36.dp)
                        .clickable { onMenuClick() }
                        .testTag("drawer_menu_button")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Navigation Menu",
                            tint = Color(0xFFFF7A00),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .clickable { onTitleClick() }
                        .testTag("header_title")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_beka_main_emblem),
                        contentDescription = "BEKASERVICE ლოგო",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .testTag("header_logo_emblem")
                    )
                    Column(
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("header_logo_text_column"),
                        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.testTag("header_brand_title_row")
                        ) {
                            Text(
                                text = "BEKA",
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                lineHeight = 17.sp,
                                letterSpacing = 0.4.sp,
                                maxLines = 1,
                                softWrap = false,
                                style = TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                            Text(
                                text = "SERVICE",
                                color = Color(0xFFFF7A00),
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                lineHeight = 17.sp,
                                letterSpacing = 0.4.sp,
                                maxLines = 1,
                                softWrap = false,
                                style = TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                        Text(
                            text = "სანდო სერვისი",
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                            fontSize = 9.5.sp,
                            lineHeight = 10.sp,
                            letterSpacing = 0.4.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            softWrap = false,
                            style = TextStyle(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                            ),
                            modifier = Modifier.testTag("header_brand_subtitle")
                        )
                    }
                }
            },
            actions = {
                // შეტყობინებები
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onNotificationsClick() }
                        .testTag("notifications_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF16191E).copy(alpha = 0.85f) else Color(0xFFFFFFFF).copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedTab == 6 || unreadNotificationsCount > 0) Color(0xFFFF7A00).copy(alpha = 0.7f) else if (isDark) Color(0xFF282F3B).copy(alpha = 0.7f) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (selectedTab == 6 || unreadNotificationsCount > 0) Color(0xFFFF7A00) else if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    if (unreadNotificationsCount > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp)
                                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                .background(
                                    color = Color(0xFFFF5500),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isDark) Color(0xFF0F1115) else Color.White,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (unreadNotificationsCount > 9) "9+" else "$unreadNotificationsCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center,
                                style = androidx.compose.ui.text.TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // პროფილი
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF16191E).copy(alpha = 0.85f) else Color(0xFFFFFFFF).copy(alpha = 0.90f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedTab == 4) Color(0xFFFF7A00).copy(alpha = 0.7f) else if (isDark) Color(0xFF282F3B).copy(alpha = 0.7f) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onProfileClick() }
                        .testTag("profile_button")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = if (selectedTab == 4) Color(0xFFFF7A00) else if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // კალათა
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onCartClick() }
                        .testTag("cart_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF16191E).copy(alpha = 0.85f) else Color(0xFFFFFFFF).copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedTab == 5 || cartItemCount > 0) Color(0xFFFF7A00).copy(alpha = 0.7f) else if (isDark) Color(0xFF282F3B).copy(alpha = 0.7f) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = if (selectedTab == 5 || cartItemCount > 0) Color(0xFFFF7A00) else if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (cartItemCount > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 3.dp, y = (-3).dp)
                                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                .background(
                                    color = Color(0xFFFF5500),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isDark) Color(0xFF0F1115) else Color.White,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (cartItemCount > 9) "9+" else "$cartItemCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center,
                                style = androidx.compose.ui.text.TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}

// ==============================================================================
// Xiaomi HyperOS 4-Inspired Glassmorphic Floating Bottom Navigation Bar
// ==============================================================================
@Composable
fun BekaGlassBottomNavBar(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val bottomBarShape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .shadow(
                elevation = if (isDark) 14.dp else 8.dp,
                shape = bottomBarShape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color(0xFFFF7A00).copy(alpha = 0.20f)
            )
            .clip(bottomBarShape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color(0xFFFF7A00).copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.10f),
                            Color(0xFFFF7A00).copy(alpha = 0.25f)
                        )
                    } else {
                        listOf(
                            Color(0x26000000),
                            Color(0xFFFF7A00).copy(alpha = 0.30f),
                            Color(0x1A000000)
                        )
                    }
                ),
                shape = bottomBarShape
            )
            .testTag("glassmorphic_bottom_nav_bar_container")
    ) {
        // Translucent frosted glass backing layer with subtle blur
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        Modifier.blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xE6161B26),
                                Color(0xEB0E1118)
                            )
                        } else {
                            listOf(
                                Color(0xF2FFFFFF),
                                Color(0xF5F8FAFC)
                            )
                        }
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(0, "მთავარი", Icons.Default.Home),
                Triple(1, "კატალოგი", Icons.Default.GridView),
                Triple(2, "დიაგნოსტიკა", Icons.Default.Build),
                Triple(3, "AI ასისტენტი", Icons.Default.SmartToy),
                Triple(4, "პროფილი", Icons.Default.Person)
            )

            tabs.forEach { (index, title, icon) ->
                val isSelected = selectedTab == index
                val iconScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "navTabScale_$index"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFFF7A00) else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    label = "navTabColor_$index"
                )
                val indicatorWidth by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "navTabIndicatorWidth_$index"
                )
                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(180),
                    label = "navTabIndicatorAlpha_$index"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("nav_tab_$index")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onSelectTab(index)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        if (index == 3) {
                            // მცირე ნარინჯისფერი AI მანათობელი წერტილი
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFF7A00),
                                modifier = Modifier
                                    .size(5.dp)
                                    .align(Alignment.TopEnd)
                            ) {}
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // გარანტირებული 1 ხაზი, შემოკლების ან გადატეხვის გარეშე
                    Text(
                        text = title,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = iconTint,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(indicatorWidth)
                            .graphicsLayer { alpha = indicatorAlpha }
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFF7A00).copy(alpha = 0.7f),
                                        Color(0xFFFF9500),
                                        Color(0xFFFF7A00).copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

// ==============================================================================
// მთავარი აპლიკაციის კომპონენტი
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BekaServiceApp(
    deepLinkIntent: Intent? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    // Show splash only on initial cold launch; never replay when returning or rotating
    var showSplash by remember { mutableStateOf(!hasCompletedInitialLaunch) }
    var isEssentialInitReady by remember { mutableStateOf(hasCompletedInitialLaunch) }

    // Tab ინდექსები: 0=მთავარი, 1=კატალოგი, 2=დიაგნოსტიკა, 3=AI ასისტენტი, 4=პროფილი, 5=კალათა, 6=შეტყობინებები
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedProductForDetail by remember { mutableStateOf<ProductItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ყველა") }

    var showAdminPinDialog by remember { mutableStateOf(false) }
    var showAdminPanelScreen by remember { mutableStateOf(false) }
    var isAdminOpen by remember { mutableStateOf(false) }

    val homeListState = rememberLazyListState()
    var lastHomeBackPressTime by remember { mutableLongStateOf(0L) }

    val cartList = remember { mutableStateListOf<CartItem>() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(Unit) {
        AppPreferencesManager.init(context)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            BekaNotificationManager.ensureNotificationChannels(context)
            BekaNotificationManager.purgeSyntheticNotificationsIfNeeded(context)
        }
    }

    // Firestore რეპოზიტორი რეალური სინქრონიზაციისთვის
    val firestoreRepo = remember {
        try {
            FirestoreRepository(context)
        } catch (e: Exception) {
            null
        }
    }

    var latestBroadcastNotifications by remember {
        mutableStateOf(BekaNotificationManager.loadLocalNotificationHistory(context))
    }
    var isNotificationsLoading by remember { mutableStateOf(true) }
    var notificationsErrorMessage by remember { mutableStateOf<String?>(null) }
    var notificationsRetryKey by remember { mutableIntStateOf(0) }
    val notifStateVersion = BekaNotificationManager.stateVersion.intValue
    val unreadNotificationsCount = remember(latestBroadcastNotifications, notifStateVersion) {
        BekaNotificationManager.getUnreadNotificationsCount(context, latestBroadcastNotifications)
    }

    // Android 13+ (API 33+) POST_NOTIFICATIONS ნებართვის მოთხოვნა
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        BekaNotificationManager.markPermissionPrompted(context)
        coroutineScope.launch {
            BekaNotificationManager.syncFcmTokenAndSubscribeTopic(context, firestoreRepo)
        }
        if (isGranted) {
            val baselineTs = BekaNotificationManager.getOrCreateListenerBaselineTimestamp(context)
            val now = System.currentTimeMillis()
            latestBroadcastNotifications.firstOrNull { n ->
                !BekaNotificationManager.hasDeliveredNotification(context, n.id) &&
                    (n.createdAtMillis >= baselineTs - 600_000L || n.createdAtMillis >= now - 600_000L)
            }?.let { recentNotif ->
                BekaNotificationManager.showNotification(
                    context = context,
                    notificationId = recentNotif.id,
                    title = recentNotif.title,
                    body = recentNotif.body,
                    targetProductId = recentNotif.targetProductId,
                    targetTab = recentNotif.targetTab
                )
            }
            latestBroadcastNotifications.drop(1).forEach { older ->
                BekaNotificationManager.markNotificationDelivered(context, older.id)
            }
        } else {
            android.util.Log.i("BekaPush", "POST_NOTIFICATIONS permission denied by user")
        }
    }

    // ბაზაში არსებული სათადარიგო ნაწილების სრული ასორტიმენტი
    val productsList = remember {
        mutableStateListOf<ProductItem>().apply {
            try {
                addAll(AdminStorage.loadProducts(context))
            } catch (e: Exception) {
                android.util.Log.e("BekaService", "Initial product load error", e)
            }
        }
    }

    val favoritesVersion = AuthAndOrderManager.favoritesStateVersion.intValue

    // სინქრონიზაცია: როდესაც იცვლება ანგარიში ან ფავორიტების სია, მყისიერად განახლდეს ყველა პროდუქტის isFavorite
    LaunchedEffect(favoritesVersion, productsList.size) {
        val activeFavIds = AuthAndOrderManager.getFavoriteProductIds(context)
        for (i in productsList.indices) {
            val currentItem = productsList[i]
            val shouldBeFav = currentItem.id in activeFavIds
            if (currentItem.isFavorite != shouldBeFav) {
                productsList[i] = currentItem.copy(isFavorite = shouldBeFav)
            }
        }
        selectedProductForDetail?.let { detailProd ->
            val shouldBeFav = detailProd.id in activeFavIds
            if (detailProd.isFavorite != shouldBeFav) {
                selectedProductForDetail = detailProd.copy(isFavorite = shouldBeFav)
            }
        }
    }

    // რეალურ დროში მომხმარებლის ანგარიშის ფავორიტების სინქრონიზაცია Firestore-დან (/users/{uid})
    DisposableEffect(firestoreRepo) {
        var favRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        val auth = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance() }.getOrNull()
        val attachFavoritesListener = {
            favRegistration?.remove()
            favRegistration = null
            if (auth?.currentUser != null && firestoreRepo != null) {
                favRegistration = firestoreRepo.listenToUserFavorites(
                    onFavoritesUpdated = { cloudFavIds ->
                        AuthAndOrderManager.setFavoriteProductIdsFromCloud(context, cloudFavIds)
                    }
                )
            }
        }
        val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
            attachFavoritesListener()
            AuthAndOrderManager.favoritesStateVersion.intValue++
        }
        auth?.addAuthStateListener(authListener)
        attachFavoritesListener()
        onDispose {
            auth?.removeAuthStateListener(authListener)
            favRegistration?.remove()
        }
    }

    val handleToggleFavorite: (ProductItem) -> Unit = { prod ->
        val isNowFav = AuthAndOrderManager.toggleFavorite(context, prod.id)
        val idx = productsList.indexOfFirst { it.id == prod.id }
        if (idx != -1) {
            productsList[idx] = productsList[idx].copy(isFavorite = isNowFav)
        }
        if (selectedProductForDetail?.id == prod.id) {
            selectedProductForDetail = selectedProductForDetail?.copy(isFavorite = isNowFav)
        }
        val toastMsg = if (isNowFav) {
            "${prod.title} დაემატა ფავორიტებში"
        } else {
            "${prod.title} წაიშალა ფავორიტებიდან"
        }
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                BekaNotificationManager.ensureNotificationChannels(context)
                val existing = AuthAndOrderManager.getCurrentUser(context)
                if (existing == null && AppPreferencesManager.isOnboardingCompleted) {
                    AuthAndOrderManager.attemptAutoSignIn(context)
                } else if (existing != null) {
                    AuthAndOrderManager.refreshCurrentUserFromCloud(context)
                }
                BekaNotificationManager.syncFcmTokenAndSubscribeTopic(context, firestoreRepo)
                firestoreRepo?.performSafeInitialMigrationIfNeeded(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("BekaService", "Auth/Migration init error", e)
        } finally {
            isEssentialInitReady = true
        }
    }

    val isOnboardingCompleted = AppPreferencesManager.isOnboardingCompleted

    LaunchedEffect(showSplash, isOnboardingCompleted) {
        if (!showSplash && isOnboardingCompleted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (!BekaNotificationManager.hasNotificationPermission(context) &&
                !BekaNotificationManager.wasPermissionPrompted(context)
            ) {
                BekaNotificationManager.markPermissionPrompted(context)
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // შეტყობინებაზე დაჭერისას კონკრეტულ პროდუქტზე ან ტაბზე გადასვლა (Foreground / Background / Closed state)
    LaunchedEffect(deepLinkIntent, productsList.size) {
        val intent = deepLinkIntent ?: return@LaunchedEffect
        val clickedNotifId = intent.getStringExtra(BekaNotificationManager.EXTRA_NOTIFICATION_ID).orEmpty()
        if (clickedNotifId.isNotBlank()) {
            BekaNotificationManager.markNotificationRead(context, clickedNotifId)
        }

        val targetProdId = intent.getIntExtra(BekaNotificationManager.EXTRA_TARGET_PRODUCT_ID, 0)
            .takeIf { it > 0 }
            ?: intent.getStringExtra("targetProductId")?.toIntOrNull()
            ?: intent.getStringExtra("productId")?.toIntOrNull()
            ?: 0

        val targetTab = intent.getIntExtra(BekaNotificationManager.EXTRA_TARGET_TAB, -1)
            .takeIf { it in 0..7 }
            ?: intent.getStringExtra("targetTab")?.toIntOrNull()?.takeIf { it in 0..7 }
            ?: -1

        if (targetProdId > 0 || targetTab in 0..7) {
            hasCompletedInitialLaunch = true
            showSplash = false
            AppPreferencesManager.setOnboardingCompleted(context, true)
            if (targetTab in 0..7) {
                selectedTab = targetTab
            }
            if (targetProdId > 0) {
                val found = productsList.find { it.id == targetProdId }
                if (found != null) {
                    selectedProductForDetail = found
                }
            }
            intent.removeExtra(BekaNotificationManager.EXTRA_NOTIFICATION_ID)
            intent.removeExtra(BekaNotificationManager.EXTRA_TARGET_PRODUCT_ID)
            intent.removeExtra(BekaNotificationManager.EXTRA_TARGET_TAB)
            intent.removeExtra("targetProductId")
            intent.removeExtra("productId")
            intent.removeExtra("targetTab")
            onDeepLinkConsumed()
        }
    }

    // რეალურ დროში Firestore notifications კოლექციის მოსმენა:
    // როცა ადმინი (beqa1994@gmail.com) აგზავნის Push შეტყობინებას, მოწყობილობა აჩვენებს სისტემურ შეტყობინებას
    DisposableEffect(firestoreRepo, notificationsRetryKey) {
        var notifRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        val baselineTs = BekaNotificationManager.getOrCreateListenerBaselineTimestamp(context)
        var initialSnapshotHandled = false
        isNotificationsLoading = true
        notificationsErrorMessage = null
        try {
            if (firestoreRepo == null) {
                isNotificationsLoading = false
            } else {
                notifRegistration = firestoreRepo.listenToBroadcastNotifications(
                    onError = { errMsg ->
                        isNotificationsLoading = false
                        notificationsErrorMessage = errMsg
                    },
                    onNotificationsUpdated = { cloudNotifs ->
                        isNotificationsLoading = false
                        notificationsErrorMessage = null
                        latestBroadcastNotifications = BekaNotificationManager.syncCloudNotificationsToLocal(context, cloudNotifs)
                        val now = System.currentTimeMillis()
                        if (!initialSnapshotHandled) {
                            initialSnapshotHandled = true
                            cloudNotifs.forEachIndexed { index, n ->
                                val isRecentBroadcast = index == 0 &&
                                    (n.createdAtMillis >= now - 120_000L || n.createdAtMillis >= baselineTs - 60_000L)
                                if (isRecentBroadcast) {
                                    BekaNotificationManager.showNotification(
                                        context = context,
                                        notificationId = n.id,
                                        title = n.title,
                                        body = n.body,
                                        targetProductId = n.targetProductId,
                                        targetTab = n.targetTab,
                                        type = n.type
                                    )
                                } else if (BekaNotificationManager.hasNotificationPermission(context)) {
                                    BekaNotificationManager.markNotificationDelivered(context, n.id)
                                }
                            }
                        } else {
                            cloudNotifs
                                .filter { n ->
                                    !BekaNotificationManager.hasDeliveredNotification(context, n.id) &&
                                        (n.createdAtMillis >= baselineTs - 120_000L || n.createdAtMillis >= now - 300_000L)
                                }
                                .sortedBy { it.createdAtMillis }
                                .forEach { newNotif ->
                                    BekaNotificationManager.showNotification(
                                        context = context,
                                        notificationId = newNotif.id,
                                        title = newNotif.title,
                                        body = newNotif.body,
                                        targetProductId = newNotif.targetProductId,
                                        targetTab = newNotif.targetTab,
                                        type = newNotif.type
                                    )
                                }
                        }
                    }
                )
            }
        } catch (e: Exception) {
            isNotificationsLoading = false
            notificationsErrorMessage = FirestoreRepository.formatUserFriendlyFirestoreError(e)
            android.util.Log.w("BekaPush", "Broadcast notifications listener error", e)
        }
        onDispose {
            notifRegistration?.remove()
        }
    }

    // რეალურ დროში Firestore products კოლექციის სინქრონიზაცია (onSnapshot)
    // Cloud Firestore არის ცენტრალური წყარო (Source of Truth):
    // ავტომატურად ასახავს დამატებას, ფასის/აღწერის შეცვლას, წაშლას, მარაგის და რიგითობის ცვლილებას.
    DisposableEffect(firestoreRepo) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            firestoreRepo?.let { repo ->
                registration = repo.listenToProductsWithChanges(
                    onSnapshotUpdated = { syncSnapshot ->
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val merged = AdminStorage.mergeCloudWithLocal(
                                context = context,
                                cloudProducts = syncSnapshot.products,
                                removedProductIds = syncSnapshot.removedProductIds,
                                presentCloudDocIds = syncSnapshot.presentDocIds
                            )
                            AdminStorage.saveProductsFromCloud(context, merged)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                if (productsList.toList() != merged) {
                                    productsList.clear()
                                    productsList.addAll(merged)
                                }
                                if (selectedProductForDetail != null) {
                                    selectedProductForDetail = merged.find { it.id == selectedProductForDetail?.id }
                                }
                            }
                        }
                    },
                    onError = { errMsg ->
                        android.util.Log.w("BekaService", "Products listener error: $errMsg")
                    }
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("BekaService", "Firestore sync init error", e)
        }
        onDispose {
            registration?.remove()
        }
    }

    val isHomeScrolledDown by remember {
        derivedStateOf {
            selectedTab == 0 && (homeListState.firstVisibleItemIndex > 0 || homeListState.firstVisibleItemScrollOffset > 0)
        }
    }

    // უკან ღილაკის მართვა ქვე-გვერდებიდან, Drawer-იდან, Splash-დან და მთავარ გვერდზე სქროლვიდან თავში დაბრუნება
    BackHandler(
        enabled = showSplash || (isOnboardingCompleted && (drawerState.isOpen || isAdminOpen || showAdminPanelScreen || selectedProductForDetail != null || selectedTab != 0 || isHomeScrolledDown))
    ) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (showSplash) {
            hasCompletedInitialLaunch = true
            showSplash = false
        } else if (isAdminOpen) {
            isAdminOpen = false
        } else if (showAdminPanelScreen) {
            showAdminPanelScreen = false
        } else if (selectedProductForDetail != null) {
            selectedProductForDetail = null
        } else if (selectedTab != 0) {
            selectedTab = 0
        } else if (isHomeScrolledDown) {
            val now = System.currentTimeMillis()
            if (now - lastHomeBackPressTime < 1500L) {
                // მეორეჯერ სწრაფად უკან გასვლისას აპლიკაცია იხურება
                (context as? Activity)?.finish()
            } else {
                lastHomeBackPressTime = now
                coroutineScope.launch {
                    if (homeListState.firstVisibleItemIndex > 6) {
                        homeListState.scrollToItem(6)
                    }
                    homeListState.animateScrollToItem(0)
                }
            }
        }
    }

    val appLaunchStage = when {
        showSplash -> 0
        !isOnboardingCompleted -> 1
        else -> 2
    }

    AnimatedContent(
        targetState = appLaunchStage,
        transitionSpec = {
            fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing))
                .togetherWith(fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)))
        },
        label = "SplashOnboardingAppTransition"
    ) { stage ->
        when (stage) {
            0 -> {
                VideoSplashScreen(
                    isAppReady = isEssentialInitReady,
                    onFinish = {
                        hasCompletedInitialLaunch = true
                        showSplash = false
                    }
                )
            }
            1 -> {
                BekaOnboardingFlow(
                    onFinishOnboarding = {
                        AppPreferencesManager.setOnboardingCompleted(context, true)
                    }
                )
            }
            else -> {
                val isDark = AppPreferencesManager.isDarkMode
                val scaffoldBgColor = AppThemeColorAdapter.adaptPageBackground(Color(0xFF121212))
                val topBarBgColor = if (isDark) Color(0xFF12141A).copy(alpha = 0.92f) else Color(0xFFFFFFFF).copy(alpha = 0.94f)

                ModalNavigationDrawer(
                drawerState = drawerState,
                // პირველ გვერდზე (selectedTab == 0) სლაიდით გახსნა/დახურვა ჩართულია, ასევე ღია მენიუს დახურვა სლაიდით ყველგან მუშაობს
                gesturesEnabled = (selectedTab == 0 && selectedProductForDetail == null && !showAdminPanelScreen && !isAdminOpen) || drawerState.isOpen,
                drawerContent = {
                    BekaSideNavigationDrawerContent(
                        selectedTab = selectedTab,
                        unreadNotificationsCount = unreadNotificationsCount,
                        cartItemsCount = cartList.sumOf { it.quantity },
                        onSelectTab = { newTab ->
                            focusManager.clearFocus()
                            selectedProductForDetail = null
                            isAdminOpen = false
                            showAdminPanelScreen = false
                            showAdminPinDialog = false
                            if (selectedTab == newTab && newTab == 0) {
                                coroutineScope.launch {
                                    if (homeListState.firstVisibleItemIndex > 6) {
                                        homeListState.scrollToItem(6)
                                    }
                                    homeListState.animateScrollToItem(0)
                                }
                            } else {
                                selectedTab = newTab
                            }
                        },
                        onOpenAdminPanel = {
                            focusManager.clearFocus()
                            showAdminPinDialog = true
                        },
                        onCloseDrawer = {
                            coroutineScope.launch { drawerState.close() }
                        }
                    )
                }
            ) {
                val statusBarTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val topBarCalculatedPadding = statusBarTopInset + 70.dp
                val bottomBarCalculatedPadding = navBarBottomInset + 74.dp
                val screenScrollPadding = remember(statusBarTopInset, navBarBottomInset) {
                    PaddingValues(
                        top = topBarCalculatedPadding + 4.dp,
                        bottom = bottomBarCalculatedPadding + 10.dp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scaffoldBgColor)
                ) {
                    val currentProductsSnapshot = productsList.toList()

                    // Layer 0: Screen content container (Edge-to-Edge scrolling beneath glass bars)
                    Box(modifier = Modifier.fillMaxSize()) {
            if (!showAdminPanelScreen) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val direction = if (targetState >= initialState) 1 else -1
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> (direction * fullWidth * 0.04f).toInt() }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 140, easing = LinearOutSlowInEasing),
                            initialAlpha = 0f
                        )).togetherWith(
                            fadeOut(
                                animationSpec = tween(durationMillis = 75, easing = FastOutLinearInEasing),
                                targetAlpha = 0f
                            )
                        ).apply {
                            targetContentZIndex = 1f
                        }.using(SizeTransform(clip = true))
                    },
                    label = "HyperOsStyleTabSlideTransition"
                ) { currentTab ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scaffoldBgColor)
                    ) {
                        when (currentTab) {
                            0 -> HomeScreen(
                                listState = homeListState,
                                contentPadding = screenScrollPadding,
                                products = currentProductsSnapshot,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                selectedCategory = selectedCategoryFilter,
                                onCategoryChange = { selectedCategoryFilter = it },
                                onProductClick = { selectedProductForDetail = it },
                                onAddToCart = { prod ->
                                    val existing = cartList.find { it.product.id == prod.id }
                                    if (existing != null) {
                                        existing.quantity += 1
                                    } else {
                                        cartList.add(CartItem(prod, 1))
                                    }
                                    Toast.makeText(context, "${prod.title} დაემატა კალათაში!", Toast.LENGTH_SHORT).show()
                                },
                                onToggleFavorite = handleToggleFavorite,
                                onOpenDiagnostics = { selectedTab = 2 },
                                onOpenAiChat = { selectedTab = 3 },
                                onOpenCatalogTab = { selectedTab = 1 },
                                onRefreshCatalog = {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val cloudResult = firestoreRepo?.fetchProductsOnce()
                                            val cloudProds = cloudResult?.getOrNull()
                                            if (!cloudProds.isNullOrEmpty()) {
                                                val merged = AdminStorage.mergeCloudWithLocal(
                                                    context = context,
                                                    cloudProducts = cloudProds
                                                )
                                                AdminStorage.saveProductsFromCloud(context, merged)
                                                withContext(Dispatchers.Main) {
                                                    productsList.clear()
                                                    productsList.addAll(merged)
                                                }
                                            } else {
                                                val localReloaded = AdminStorage.loadProducts(context)
                                                withContext(Dispatchers.Main) {
                                                    productsList.clear()
                                                    productsList.addAll(localReloaded)
                                                }
                                            }
                                        } catch (_: Exception) {
                                            val localReloaded = AdminStorage.loadProducts(context)
                                            withContext(Dispatchers.Main) {
                                                productsList.clear()
                                                productsList.addAll(localReloaded)
                                            }
                                        }
                                    }
                                }
                            )
                            1 -> CatalogScreen(
                                contentPadding = screenScrollPadding,
                                products = currentProductsSnapshot,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                selectedCategory = selectedCategoryFilter,
                                onCategoryChange = { selectedCategoryFilter = it },
                                onProductClick = { selectedProductForDetail = it },
                                onAddToCart = { prod ->
                                    val existing = cartList.find { it.product.id == prod.id }
                                    if (existing != null) existing.quantity += 1 else cartList.add(CartItem(prod, 1))
                                    Toast.makeText(context, "${prod.title} დაემატა კალათაში!", Toast.LENGTH_SHORT).show()
                                },
                                onToggleFavorite = handleToggleFavorite
                            )
                            2 -> DiagnosticsScreen(
                                contentPadding = screenScrollPadding,
                                onProductClick = { selectedProductForDetail = it },
                                products = currentProductsSnapshot
                            )
                            3 -> AiAssistantScreen(
                                contentPadding = screenScrollPadding,
                                products = currentProductsSnapshot,
                                onProductClick = { selectedProductForDetail = it },
                                onAddToCart = { prod ->
                                    val existing = cartList.find { it.product.id == prod.id }
                                    if (existing != null) existing.quantity += 1 else cartList.add(CartItem(prod, 1))
                                    Toast.makeText(context, "${prod.title} დაემატა კალათაში!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            4 -> ProfileScreen(
                                contentPadding = screenScrollPadding,
                                onOpenCart = { selectedTab = 5 },
                                onOpenAdminPanel = { showAdminPinDialog = true },
                                products = currentProductsSnapshot,
                                onProductClick = { selectedProductForDetail = it },
                                onToggleFavorite = handleToggleFavorite,
                                onAddToCart = { prod ->
                                    val existing = cartList.find { it.product.id == prod.id }
                                    if (existing != null) existing.quantity += 1 else cartList.add(CartItem(prod, 1))
                                    Toast.makeText(context, "${prod.title} დაემატა კალათაში!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            5 -> CartScreen(
                                contentPadding = screenScrollPadding,
                                cartList = cartList,
                                onBack = { selectedTab = 0 },
                                onCheckout = {
                                    val snapshotCart = cartList.toList()
                                    val itemsText = snapshotCart.joinToString("\n") { "• ${it.product.title} (${it.selectedQuality}) x${it.quantity} - ${it.product.price}" }
                                    val totalMsg = "გამარჯობა ბექა! მსურს კალათიდან შეკვეთა:\n$itemsText\nსულ: ${snapshotCart.size} პოზიცია."
                                    coroutineScope.launch {
                                        val persistRes = AuthAndOrderManager.createAndPersistOrderFromCart(context, snapshotCart)
                                        if (persistRes.isSuccess) {
                                            Toast.makeText(context, "✅ შეკვეთა შენახულია ისტორიაში!", Toast.LENGTH_SHORT).show()
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/995598233332?text=${Uri.encode(totalMsg)}"))
                                        context.startActivity(intent)
                                    }
                                }
                            )
                            6 -> UserNotificationsScreen(
                                contentPadding = screenScrollPadding,
                                cloudNotifications = latestBroadcastNotifications,
                                products = currentProductsSnapshot,
                                onNavigateToTarget = { targetTabIdx, targetProdId ->
                                    if (targetTabIdx in 0..7) {
                                        selectedTab = targetTabIdx
                                    }
                                    if (targetProdId > 0) {
                                        val found = currentProductsSnapshot.find { it.id == targetProdId }
                                        if (found != null) {
                                            selectedProductForDetail = found
                                        }
                                    }
                                },
                                onBack = { selectedTab = 0 },
                                isLoading = isNotificationsLoading,
                                errorMessage = notificationsErrorMessage,
                                onRetry = { notificationsRetryKey += 1 }
                            )
                            7 -> AppUpdateScreen(
                                contentPadding = screenScrollPadding,
                                onBack = { selectedTab = 0 }
                            )
                        }
                    }
                }
            }
        }

        // Layer 1: Glassmorphic Floating Top App Bar (Xiaomi HyperOS 4-inspired)
        BekaGlassTopAppBar(
            modifier = Modifier.align(Alignment.TopCenter),
            isDark = isDark,
            selectedTab = selectedTab,
            unreadNotificationsCount = unreadNotificationsCount,
            cartItemCount = cartList.sumOf { it.quantity },
            onMenuClick = {
                focusManager.clearFocus()
                coroutineScope.launch {
                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                }
            },
            onTitleClick = {
                focusManager.clearFocus()
                selectedProductForDetail = null
                if (selectedTab == 0) {
                    coroutineScope.launch {
                        if (homeListState.firstVisibleItemIndex > 6) {
                            homeListState.scrollToItem(6)
                        }
                        homeListState.animateScrollToItem(0)
                    }
                } else {
                    selectedTab = 0
                }
            },
            onNotificationsClick = {
                focusManager.clearFocus()
                selectedProductForDetail = null
                selectedTab = 6
                if (latestBroadcastNotifications.isNotEmpty()) {
                    BekaNotificationManager.markAllNotificationsRead(context, latestBroadcastNotifications)
                }
            },
            onProfileClick = {
                focusManager.clearFocus()
                selectedProductForDetail = null
                selectedTab = 4
            },
            onCartClick = {
                focusManager.clearFocus()
                selectedProductForDetail = null
                selectedTab = 5
            }
        )

        // Layer 2: Glassmorphic Floating Bottom Navigation Bar (Xiaomi HyperOS 4-inspired)
        BekaGlassBottomNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            isDark = isDark,
            selectedTab = selectedTab,
            onSelectTab = { index ->
                focusManager.clearFocus()
                selectedProductForDetail = null
                isAdminOpen = false
                showAdminPanelScreen = false
                showAdminPinDialog = false
                if (selectedTab == index && index == 0) {
                    coroutineScope.launch {
                        if (homeListState.firstVisibleItemIndex > 6) {
                            homeListState.scrollToItem(6)
                        }
                        homeListState.animateScrollToItem(0)
                    }
                } else {
                    selectedTab = index
                }
            }
        )

            // ==============================================================================
            // პროდუქტის გახსნის ფანჯარა (Product Detail Window)
            // ==============================================================================
            if (selectedProductForDetail != null) {
                ProductDetailModalWindow(
                    product = selectedProductForDetail!!,
                    onClose = { selectedProductForDetail = null },
                    onAddToCart = { prod, quality ->
                        val existing = cartList.find { it.product.id == prod.id && it.selectedQuality == quality }
                        if (existing != null) {
                            existing.quantity += 1
                        } else {
                            cartList.add(CartItem(prod, 1, quality))
                        }
                        Toast.makeText(context, "${prod.title} ($quality) დაემატა კალათაში!", Toast.LENGTH_SHORT).show()
                    },
                    onToggleFavorite = handleToggleFavorite
                )
            }

            // ==============================================================================
            // ⚙️ ადმინ პანელი - პროდუქტის დამატების მოდალი (Firebase Firestore Real-time)
            // ==============================================================================
            if (isAdminOpen) {
                AdminAddProductModal(
                    onDismiss = { isAdminOpen = false },
                    onAddProduct = { title, price, rating, imageUrl, category ->
                        coroutineScope.launch {
                            val repo = firestoreRepo
                            if (repo == null) {
                                Toast.makeText(context, "❌ Firestore კავშირი მიუწვდომელია", Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            val nextId = (productsList.maxOfOrNull { it.id } ?: 200) + 1
                            val deterministicDocId = "prod_$nextId"
                            val newProd = ProductItem(
                                id = nextId,
                                title = title.ifBlank { "ნაწილი" },
                                price = price.ifBlank { "0 ₾" },
                                category = category,
                                type = if (category == "პროგრამული სერვისები") "software" else "product",
                                imageUrl = imageUrl,
                                rating = rating.toDoubleOrNull() ?: 5.0,
                                inStock = true,
                                firestoreDocId = deterministicDocId
                            )
                            val writeResult = repo.saveOrUpdateProductResult(newProd)
                            if (writeResult.isSuccess) {
                                AdminStorage.unmarkProductDeleted(context, newProd.id)
                                if (productsList.none { it.id == newProd.id }) {
                                    productsList.add(0, newProd)
                                }
                                AdminStorage.saveProductsFromCloud(context, productsList.toList())
                                Toast.makeText(context, "✅ პროდუქტი წარმატებით დაემატა Firestore-ში!", Toast.LENGTH_SHORT).show()
                                isAdminOpen = false
                            } else {
                                val err = writeResult.exceptionOrNull()
                                val friendlyMsg = if (err != null) {
                                    repo.formatUserFriendlyFirestoreError(err)
                                } else {
                                    "Firestore-ში ჩაწერა ვერ მოხერხდა"
                                }
                                Toast.makeText(context, "❌ $friendlyMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onOpenFullAdmin = {
                        showAdminPinDialog = true
                    }
                )
            }

            // ==============================================================================
            // ადმინისტრატორის PIN კოდის შეყვანის მოდალი
            // ==============================================================================
            if (showAdminPinDialog) {
                AdminPinDialog(
                    onDismiss = { showAdminPinDialog = false },
                    onSuccess = {
                        showAdminPinDialog = false
                        coroutineScope.launch {
                            kotlinx.coroutines.yield()
                            showAdminPanelScreen = true
                        }
                    }
                )
            }

            // ==============================================================================
            // ადმინისტრატორის მართვის პანელის სრული ეკრანი
            // ==============================================================================
            if (showAdminPanelScreen) {
                AdminPanelScreen(
                    products = currentProductsSnapshot,
                    onProductsUpdated = { updatedList ->
                        AdminStorage.saveProductsFromCloud(context, updatedList)
                        productsList.clear()
                        productsList.addAll(updatedList)
                        if (selectedProductForDetail != null) {
                            selectedProductForDetail = updatedList.find { it.id == selectedProductForDetail?.id }
                        }
                    },
                    onClose = { showAdminPanelScreen = false }
                )
            }
                }
            }
            }
        }
    }
}


// ==============================================================================
// 1. პროდუქტის გახსნის ფანჯარა (Product Detail Window / Sheet)
// ==============================================================================
@Composable
fun ProductDetailModalWindow(
    product: ProductItem,
    onClose: () -> Unit,
    onAddToCart: (ProductItem, String) -> Unit,
    onToggleFavorite: (ProductItem) -> Unit = {}
) {
    val context = LocalContext.current
    val isFavorite = product.isFavorite
    val isSoftware = product.category == "პროგრამული სერვისები" || product.type == "software"
    val isDigitalSoftware = SoftwareCatalogData.isDigitalSoftwareService(product)
    var selectedQuality by remember(product.id) {
        mutableStateOf(
            when {
                isDigitalSoftware -> product.duration.ifBlank { product.offerType.ifBlank { "სტანდარტული" } }
                isSoftware -> "სტანდარტული"
                else -> "Original"
            }
        )
    }

    // ხარისხის ან სერვისის პაკეტის მიხედვით დინამიური ფასი
    val currentPrice = when {
        isDigitalSoftware -> product.price
        isSoftware -> when (selectedQuality) {
            "ექსპრეს სერვისი" -> "${(product.price.replace("₾", "").trim().toIntOrNull() ?: 45) + 15} ₾"
            else -> product.price
        }
        else -> when (selectedQuality) {
            "High Copy" -> "280 ₾"
            "AAA" -> "190 ₾"
            else -> product.price
        }
    }

    BackHandler { onClose() }

    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClose() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val isDark = AppPreferencesManager.isDarkMode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
            ) {
                // Glassmorphic translucent backing layer with blur
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .then(
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                Modifier.blur(18.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xFF151922).copy(alpha = 0.88f),
                                        Color(0xFF0F1218).copy(alpha = 0.95f)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFFFFFFF).copy(alpha = 0.92f),
                                        Color(0xFFF8FAFC).copy(alpha = 0.97f)
                                    )
                                }
                            )
                        )
                )
                // Specular top hairline highlight
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = if (isDark) {
                                        listOf(
                                            Color.White.copy(alpha = 0.05f),
                                            Color.White.copy(alpha = 0.22f),
                                            Color(0xFFFF7A00).copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.05f)
                                        )
                                    } else {
                                        listOf(
                                            Color(0x1F000000),
                                            Color(0x33000000),
                                            Color(0x1F000000)
                                        )
                                    }
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                )
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* შეაჩერე გარეთ დაჭერის დახურვა */ }
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // ზედა სახელური და სათაურის ზოლი
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppThemeColorAdapter.adaptTopBarBackground(Color(0xFF161A22)))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (AppPreferencesManager.isDarkMode) Color(0xFF3E4554) else Color(0xFFCBD5E1))
                                .align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onClose() }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFFFF7A00),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "უკან დაბრუნება",
                                    color = Color(0xFFFF7A00),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { onClose() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA0A5B0))
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
                    ) {
                        // 1. პროდუქტის ჰერო სურათი ან პროგრამული ბრენდის ვიზუალი
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF0F1115)))
                                    .border(1.dp, AppThemeColorAdapter.dividerColor(Color(0xFF282F3A)), RoundedCornerShape(16.dp))
                            ) {
                                ProductSmartVisual(
                                    product = product,
                                    modifier = Modifier.fillMaxSize(),
                                    compact = false
                                )

                                // გულის ღილაკი
                                Surface(
                                    onClick = { onToggleFavorite(product) },
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(10.dp)
                                        .size(36.dp)
                                        .testTag("detail_favorite_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFavorite) Color(0xFFFF7A00) else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // ბრენდის აბი
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .align(Alignment.BottomStart)
                                ) {
                                    Text(
                                        "ბრენდი: ${product.brand}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // 2. კატეგორია, ქვეკატეგორია და რეიტინგი
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0x33FF7A00)
                                    ) {
                                        Text(
                                            product.category,
                                            color = Color(0xFFFF7A00),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (product.subcategory.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF1A202C),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D3748))
                                        ) {
                                            Text(
                                                product.subcategory,
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF16191E),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB800), modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${product.rating}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 3. სათაური, მოკლე აღწერა, ფასი და ხელმისაწვდომობა
                        item {
                            Column {
                                Text(
                                    product.title,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (product.shortDescription.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        product.shortDescription,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val isLongModalPrice = currentPrice.length > 10
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        currentPrice,
                                        color = Color(0xFFFF7A00),
                                        fontSize = if (isLongModalPrice) 18.sp else 25.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (product.inStock) Color(0x224ADE80) else Color(0x22F87171)
                                    ) {
                                        Text(
                                            if (product.inStock) "ხელმისაწვდომია ✅" else "დროებით მიუწვდომელია ⏳",
                                            color = if (product.inStock) Color(0xFF4ADE80) else Color(0xFFF87171),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. ხარისხის ან სერვისის პაკეტის არჩევა (მხოლოდ აპარატურული ნაწილებისა და მობილური განბლოკვისთვის)
                        if (!isDigitalSoftware) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        if (isSoftware) "სერვისის ტიპი / პაკეტი:" else "ხარისხის არჩევა:",
                                        color = Color(0xFFA0A5B0),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val qualityOptions = if (isSoftware) {
                                        listOf("სტანდარტული", "ექსპრეს სერვისი", "დისტანციური USB")
                                    } else {
                                        listOf("Original", "High Copy", "AAA")
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        qualityOptions.forEach { quality ->
                                            val isSelected = selectedQuality == quality
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) Color(0xFFFF7A00) else Color(0xFF16191E),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSelected) Color(0xFFFF7A00) else Color(0xFF282F3A)
                                                ),
                                                modifier = Modifier.clickable { selectedQuality = quality }
                                            ) {
                                                Text(
                                                    quality,
                                                    color = if (isSelected) Color.White else Color(0xFFA0A5B0),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4.1 თუ პროგრამული სერვისია - სრული სპეციფიკაცია, ფუნქციები, პირობები და ოფიციალური წყაროები
                        if (isSoftware) {
                            item {
                                SoftwareServiceDetailSection(product = product)
                            }
                        }

                        // 5. აღწერა და მახასიათებლები
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF16191E),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            if (isDigitalSoftware) "სრული აღწერა" else "აღწერა",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(product.description, color = Color(0xFFA0A5B0), fontSize = 12.sp, lineHeight = 17.sp)
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            if (isDigitalSoftware) "თავსებადი პლატფორმები და მოწყობილობები" else "თავსებადი მოდელები",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(product.compatibleModels, color = Color(0xFFA0A5B0), fontSize = 12.sp)
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            if (isDigitalSoftware) "BEKASERVICE გარანტია და მხარდაჭერა" else "გარანტია",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(product.warranty, color = Color(0xFFFF7A00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 6. პირდაპირი შეკვეთა: WhatsApp & Telegram
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (product.inStock) Color(0xFF0F2618) else Color(0xFF1A1D24),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (product.inStock) Color(0xFF1B4E2E) else Color(0xFF282E3A)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable(enabled = product.inStock) {
                                            val extraInfo = if (isDigitalSoftware) {
                                                "\nპაკეტი: ${product.offerType}\nხანგრძლივობა: ${product.duration}\nანგარიშის ტიპი: ${product.accountType}"
                                            } else {
                                                " ($selectedQuality)"
                                            }
                                            val msg = "გამარჯობა BEKASERVICE! მსურს შეკვეთა:\n${product.title}$extraInfo\nფასი: $currentPrice"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/995598233332?text=${Uri.encode(msg)}"))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("💬", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (product.inStock) "WhatsApp შეკვეთა" else "მიუწვდომელია",
                                            color = if (product.inStock) Color(0xFF4ADE80) else Color(0xFF6E7582),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (product.inStock) Color(0xFF0E2235) else Color(0xFF1A1D24),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (product.inStock) Color(0xFF1A456B) else Color(0xFF282E3A)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable(enabled = product.inStock) {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/geounlock"))
                                            context.startActivity(intent)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("✈️", fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (product.inStock) "Telegram შეკვეთა" else "მიუწვდომელია",
                                            color = if (product.inStock) Color(0xFF38BDF8) else Color(0xFF6E7582),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 7. კალათაში დამატების / შეკვეთის ღილაკი
                        item {
                            Button(
                                onClick = {
                                    onAddToCart(product, selectedQuality)
                                    onClose()
                                },
                                enabled = product.inStock,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6A00),
                                    disabledContainerColor = Color(0xFF262A34)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = if (product.inStock) Color.White else Color(0xFF6E7582),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (product.inStock) "შეკვეთა / კალათაში დამატება ($currentPrice)" else "დროებით მიუწვდომელია",
                                    color = if (product.inStock) Color.White else Color(0xFF6E7582),
                                    fontSize = if (currentPrice.length > 10) 12.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ==============================================================================
// 2. მთავარი გვერდი (Home Screen) ოპტიმიზებული ძებნით
// ==============================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    products: List<ProductItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onProductClick: (ProductItem) -> Unit,
    onAddToCart: (ProductItem) -> Unit,
    onToggleFavorite: (ProductItem) -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenCatalogTab: () -> Unit,
    onRefreshCatalog: suspend () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp),
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val refreshScope = rememberCoroutineScope()
    var selectedSubcategory by remember { mutableStateOf("ყველა") }

    // შეთავაზებების ბანერები AdminStorage-დან და Cloud Firestore-დან (ადმინ პანელიდან სრულად რედაქტირებადი)
    var banners by remember { mutableStateOf(AdminStorage.getHomeBanners(context)) }

    DisposableEffect(Unit) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            val repo = FirestoreRepository(context)
            registration = repo.listenToHomeBanners { cloudBanners ->
                if (cloudBanners.isNotEmpty()) {
                    AdminStorage.saveHomeBanners(context, cloudBanners)
                    banners = AdminStorage.getHomeBanners(context)
                }
            }
        } catch (_: Exception) {}
        onDispose {
            registration?.remove()
        }
    }

    LaunchedEffect(products) {
        banners = AdminStorage.getHomeBanners(context)
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != "პროგრამული სერვისები") {
            selectedSubcategory = "ყველა"
        }
    }

    // კატეგორიები (მოიცავს როგორც სტანდარტულ, ისე ადმინის/AI-ის მიერ დამატებულ კატეგორიებს)
    val categories = remember(products) {
        val basePairs = listOf(
            Pair("ყველა", "🔥"),
            Pair("პროგრამული სერვისები", "💻"),
            Pair("ეკრანები", "📱"),
            Pair("ელემენტები", "🔋"),
            Pair("კამერები", "📸"),
            Pair("დინამიკები", "🔊"),
            Pair("დამტენის პორტები", "🔌"),
            Pair("კორპუსები", "🛠️")
        )
        val existingNames = basePairs.map { it.first }.toSet()
        val customPairs = products
            .map { it.category.trim() }
            .filter { it.isNotBlank() && it !in existingNames }
            .distinct()
            .map { Pair(it, "✨") }
        basePairs + customPairs
    }

    // ოპტიმიზებული საძიებო ფილტრი (ყოველთვის იყენებს განახლებულ products სიას)
    val filteredProducts = remember(products, searchQuery, selectedCategory, selectedSubcategory) {
        optimizeSearchFilter(products, searchQuery, selectedCategory, "ყველა", "default", selectedSubcategory)
    }
    val productRows = remember(filteredProducts) {
        filteredProducts.chunked(2)
    }
    val allSoftwareServices = remember(products) {
        products.filter {
            SoftwareCatalogData.isDigitalSoftwareService(it) || it.category == "პროგრამული სერვისები"
        }
    }
    val featuredSoftwareProducts = remember(products, allSoftwareServices) {
        val explicitlyFeatured = allSoftwareServices
            .filter { it.isFeatured }
            .sortedWith(compareBy<ProductItem> { if (it.displayOrder > 0) it.displayOrder else Int.MAX_VALUE }.thenBy { it.id })
        if (explicitlyFeatured.isNotEmpty()) {
            explicitlyFeatured.take(10)
        } else {
            val priorityIds = listOf(103, 101, 118, 107, 116, 121, 105, 119)
            priorityIds.mapNotNull { id -> products.find { it.id == id } }
        }
    }

    // =========================================================================
    // Pull-to-Refresh მექანიზმი ჩამოთრევის დონის პროპორციული ანიმაციით
    // =========================================================================
    val pullThresholdPx = remember(density) { with(density) { 84.dp.toPx() } }
    val maxPullPx = remember(density) { with(density) { 150.dp.toPx() } }
    val refreshingHoldPx = remember(density) { with(density) { 70.dp.toPx() } }

    var rawPullDistancePx by remember { mutableFloatStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isRefreshCompleted by remember { mutableStateOf(false) }

    val pullProgress = remember(rawPullDistancePx, pullThresholdPx) {
        (rawPullDistancePx / pullThresholdPx).coerceIn(0f, 1.35f)
    }

    val pullRefreshNestedScrollConnection = remember(listState, pullThresholdPx, maxPullPx, refreshingHoldPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // როცა მომხმარებელს ჩამოწეული აქვს და თითს ისევ ზემოთ აბრუნებს გაშვებამდე
                if (available.y < 0f && rawPullDistancePx > 0f && !isRefreshing) {
                    val consumedY = available.y.coerceAtLeast(-rawPullDistancePx)
                    rawPullDistancePx = (rawPullDistancePx + consumedY).coerceAtLeast(0f)
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // როცა სია თავშია და მომხმარებელი ქვემოთ ქაჩავს (ჩამოთრევა)
                if (source == NestedScrollSource.UserInput &&
                    available.y > 0f &&
                    !isRefreshing &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                ) {
                    val resistanceRatio = (rawPullDistancePx / maxPullPx).coerceIn(0f, 1f)
                    val dragMultiplier = 0.54f * (1f - resistanceRatio * 0.44f)
                    rawPullDistancePx = (rawPullDistancePx + available.y * dragMultiplier).coerceIn(0f, maxPullPx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (rawPullDistancePx > 0f && !isRefreshing) {
                    if (rawPullDistancePx >= pullThresholdPx) {
                        isRefreshing = true
                        isRefreshCompleted = false
                        refreshScope.launch {
                            animate(
                                initialValue = rawPullDistancePx,
                                targetValue = refreshingHoldPx,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { value, _ ->
                                rawPullDistancePx = value
                            }
                            banners = AdminStorage.getHomeBanners(context)
                            onRefreshCatalog()
                            delay(700L)
                            isRefreshCompleted = true
                            delay(380L)
                            isRefreshing = false
                            isRefreshCompleted = false
                            animate(
                                initialValue = rawPullDistancePx,
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) { value, _ ->
                                rawPullDistancePx = value
                            }
                        }
                        return available
                    } else {
                        val startPx = rawPullDistancePx
                        refreshScope.launch {
                            animate(
                                initialValue = startPx,
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) { value, _ ->
                                rawPullDistancePx = value
                            }
                        }
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val contentPullShiftDp = with(density) { (rawPullDistancePx * 0.68f).toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullRefreshNestedScrollConnection)
            .testTag("home_pull_refresh_container")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    translationY = rawPullDistancePx * 0.68f
                },
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = contentPadding
        ) {
            // 1. საძიებო ველი (Glassmorphism Frosted Search Bar & Smooth Transition)
            item {
                val isDark = AppPreferencesManager.isDarkMode
            val isSearchActive = searchQuery.isNotEmpty()
            val searchBorderColor by animateColorAsState(
                targetValue = if (isSearchActive) Color(0xFFFF7A00) else if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1),
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "homeSearchBorder"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                // Glassmorphic translucent backing layer with subtle frosted blur
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                Modifier.blur(14.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xFF171A22).copy(alpha = 0.80f),
                                        Color(0xFF101319).copy(alpha = 0.88f)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFFFFFFF).copy(alpha = 0.86f),
                                        Color(0xFFF1F5F9).copy(alpha = 0.92f)
                                    )
                                }
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, searchBorderColor),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchActive) Color(0xFFFF7A00) else Color(0xFFA0A5B0),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = AppThemeColorAdapter.inputTextColor(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color(0xFFFF7A00)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_search_input"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "მოძებნე: ChatGPT, Google AI, Canva, iPhone, OLED...",
                                        color = Color(0xFF888E9B),
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        )
                        AnimatedVisibility(
                            visible = isSearchActive,
                            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
                            exit = fadeOut(tween(150)) + scaleOut(tween(150))
                        ) {
                            IconButton(
                                onClick = {
                                    onSearchChange("")
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // თუ მომხმარებელი ეძებს ტექსტით, შედეგები მყისიერად ჩანს თავშივე!
        if (searchQuery.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ძებნის შედეგი: ",
                            color = Color(0xFFA0A5B0),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "\"$searchQuery\"",
                            color = Color(0xFFFF7A00),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E232B)
                    ) {
                        Text(
                            text = "ნაპოვნია ${filteredProducts.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        } else {
            // ==============================================================================
            // 2. ჰედერის სლაიდერი (3 მთავარი პროგრამული შეთავაზება + არსებული სლაიდები)
            // ==============================================================================
            item {
                val pagerState = rememberPagerState(pageCount = { banners.size })

                LaunchedEffect(banners.size) {
                    while (true) {
                        delay(4000)
                        if (banners.isNotEmpty() && !pagerState.isScrollInProgress) {
                            val nextPage = (pagerState.currentPage + 1) % banners.size
                            pagerState.animateScrollToPage(
                                page = nextPage,
                                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(166.dp),
                        pageSpacing = 12.dp,
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) { page ->
                        val banner = banners[page]
                        val title = banner.title
                        val subtitle = banner.subtitle
                        val imageUrl = banner.imageUrl
                        val targetProdId = banner.targetProductId
                        val tag = banner.tag
                        val realProduct = products.find { it.id == targetProdId }
                            ?: products.firstOrNull { it.title.contains(title, ignoreCase = true) }
                            ?: products.firstOrNull()
                        val isSoftwareSlide = realProduct != null && SoftwareCatalogData.isDigitalSoftwareService(realProduct)

                        // დაკლიკებისას იხსნება რეალური პროდუქტი ზუსტად ისე, როგორც კატალოგში!
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF16191E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSoftwareSlide) Color(0xFFFF7A00).copy(alpha = 0.55f) else Color(0xFF2E333F)
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("hero_slide_$page")
                                .clickable {
                                    realProduct?.let { onProductClick(it) }
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isSoftwareSlide && realProduct != null) {
                                    SoftwareBrandVisualHeader(
                                        product = realProduct,
                                        modifier = Modifier.fillMaxSize(),
                                        compact = false
                                    )
                                } else {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                // მუქი გრადიენტის გადაფარვა ტექსტის მკვეთრი ხილვადობისთვის
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Black.copy(alpha = if (isSoftwareSlide) 0.18f else 0.25f),
                                                    Color.Black.copy(alpha = 0.90f)
                                                )
                                            )
                                        )
                                )
                                CompositionLocalProvider(LocalPreserveWhiteText provides true) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFF7A00)
                                            ) {
                                                Text(
                                                    tag,
                                                    color = Color.White,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFF7A00),
                                                modifier = Modifier.clickable {
                                                    realProduct?.let { onProductClick(it) }
                                                }
                                            ) {
                                                Text(
                                                    "ნახე შეთავაზება →",
                                                    color = Color.White,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isSoftwareSlide && realProduct != null && banner.id.startsWith("slide_")) realProduct.title else title,
                                                color = Color.White,
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (realProduct != null) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = realProduct.price,
                                                    color = Color(0xFF4ADE80),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isSoftwareSlide && realProduct != null && realProduct.shortDescription.isNotBlank()) {
                                                realProduct.shortDescription
                                            } else {
                                                subtitle
                                            },
                                            color = Color(0xFFD0D5E0),
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            lineHeight = 14.5.sp,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ინდიკატორის წერტილები (გლუვი ანიმაციით)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(banners.size) { index ->
                            val isCurrent = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (isCurrent) 22.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) Color(0xFFFF7A00)
                                        else if (AppPreferencesManager.isDarkMode) Color(0xFF333B4A)
                                        else Color(0xFFCBD5E1)
                                    )
                            )
                        }
                    }
                }
            }

            // 3. კატეგორიები (ჰორიზონტალური სლაიდერი)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "კატეგორიები",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ყველა",
                            color = Color(0xFFFF7A00),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onCategoryChange("ყველა") }
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(categories) { (catName, icon) ->
                            val isSelected = (selectedCategory == catName) || (selectedCategory == "ყველა" && catName == "ყველა")
                            val isDark = AppPreferencesManager.isDarkMode
                            val chipBgColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    Color(0xFFFF7A00).copy(alpha = 0.22f)
                                } else if (isDark) {
                                    Color(0xFF161922).copy(alpha = 0.72f)
                                } else {
                                    Color(0xFFFFFFFF).copy(alpha = 0.85f)
                                },
                                animationSpec = tween(200, easing = FastOutSlowInEasing),
                                label = "catChipBg_$catName"
                            )
                            val chipBorderColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    Color(0xFFFF7A00)
                                } else if (isDark) {
                                    Color(0xFF2E3442).copy(alpha = 0.75f)
                                } else {
                                    Color(0xFFCBD5E1)
                                },
                                animationSpec = tween(200, easing = FastOutSlowInEasing),
                                label = "catChipBorder_$catName"
                            )
                            val chipTextColor by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFFFF7A00) else if (isDark) Color.White else Color(0xFF0F172A),
                                animationSpec = tween(200),
                                label = "catChipText_$catName"
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = chipBgColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, chipBorderColor),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onCategoryChange(if (isSelected && catName != "ყველა") "ყველა" else catName)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(icon, fontSize = 18.sp)
                                    Text(
                                        catName,
                                        color = chipTextColor,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // თუ არჩეულია "პროგრამული სერვისები", ვაჩვენებთ ქვეკატეგორიების ფილტრებს
                    if (selectedCategory == "პროგრამული სერვისები") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(SoftwareCatalogData.softwareSubcategories) { subcat ->
                                val isSubSelected = selectedSubcategory == subcat
                                val isDark = AppPreferencesManager.isDarkMode
                                val subBgColor by animateColorAsState(
                                    targetValue = if (isSubSelected) Color(0xFFFF7A00) else if (isDark) Color(0xFF181C26).copy(alpha = 0.75f) else Color(0xFFFFFFFF).copy(alpha = 0.85f),
                                    animationSpec = tween(200),
                                    label = "subcatBg_$subcat"
                                )
                                val subBorderColor by animateColorAsState(
                                    targetValue = if (isSubSelected) Color(0xFFFF7A00) else if (isDark) Color(0xFF2E3444).copy(alpha = 0.75f) else Color(0xFFCBD5E1),
                                    animationSpec = tween(200),
                                    label = "subcatBorder_$subcat"
                                )
                                val subTextColor by animateColorAsState(
                                    targetValue = if (isSubSelected) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                    animationSpec = tween(200),
                                    label = "subcatText_$subcat"
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = subBgColor,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, subBorderColor),
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedSubcategory = subcat }
                                ) {
                                    Text(
                                        subcat,
                                        color = subTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3.5 პროგრამული სერვისების და AI პაკეტების სწრაფი ვიტრინა (როცა "ყველა" კატეგორიაა არჩეული)
            if (selectedCategory == "ყველა" && featuredSoftwareProducts.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("💻", fontSize = 15.sp)
                                Text(
                                    text = "პროგრამული სერვისები & AI",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ყველა (${allSoftwareServices.size}) →",
                                color = Color(0xFFFF7A00),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.clickable {
                                    onCategoryChange("პროგრამული სერვისები")
                                }
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(featuredSoftwareProducts, key = { "feat_sw_${it.id}" }) { swProd ->
                                Box(modifier = Modifier.width(172.dp)) {
                                    ProductPhotoCard(
                                        product = swProd,
                                        onClick = { onProductClick(swProd) },
                                        onAddToCart = { onAddToCart(swProd) },
                                        onToggleFavorite = { onToggleFavorite(swProd) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. სწრაფი სერვისები და AI ასისტენტი (მოკლე ბანერები სისტემის უწყვეტი მუშაობისთვის)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // დიაგნოსტიკა
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF16191E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262A34)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenDiagnostics() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF7A00).copy(alpha = 0.2f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(17.dp))
                                }
                            }
                            Column {
                                Text("დიაგნოსტიკა", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("ონლაინ შეფასება", color = Color(0xFFA0A5B0), fontSize = 10.sp)
                            }
                        }
                    }

                    // AI ასისტენტი
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF16191E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262A34)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenAiChat() }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF7A00).copy(alpha = 0.2f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🤖", fontSize = 16.sp)
                                }
                            }
                            Column {
                                Text("AI ასისტენტი", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("კონსულტაცია 24/7", color = Color(0xFFA0A5B0), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // 5. პროდუქტების სიის სათაური
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "პროგრამული სერვისები") "პროგრამული სერვისების კატალოგი" else "პოპულარული სერვისები და ნაწილები",
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF7A00).copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onCategoryChange("ყველა") }
                    ) {
                        Text(
                            text = "სულ: ${filteredProducts.size}",
                            color = Color(0xFFFF7A00),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (filteredProducts.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF16191E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔍", fontSize = 30.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("პროდუქტი ან სერვისი ვერ მოიძებნა", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("სცადეთ სხვა საძიებო სიტყვა (მაგ: ChatGPT, Google AI, Canva, iPhone) ან გაასუფთავეთ ფილტრი", color = Color(0xFFA0A5B0), fontSize = 12.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = {
                            onSearchChange("")
                            onCategoryChange("ყველა")
                            selectedSubcategory = "ყველა"
                        }) {
                            Text("ძებნის გასუფთავება", color = Color(0xFFFF7A00))
                        }
                    }
                }
            }
        }

        // 6. 2-სვეტიანი პროდუქტების სია
        items(
            items = productRows,
            key = { row -> row.joinToString("-") { "${it.id}" } }
        ) { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItemPlacement(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { product ->
                    Box(modifier = Modifier.weight(1f)) {
                        ProductPhotoCard(
                            product = product,
                            onClick = { onProductClick(product) },
                            onAddToCart = { onAddToCart(product) },
                            onToggleFavorite = { onToggleFavorite(product) }
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 7. უპირატესობების ქვედა ზოლი
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("100% ხარისხი", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        Text("გარანტირებული სერვისი", color = Color(0xFF6E7582), fontSize = 9.sp, textAlign = TextAlign.Center)
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(AppThemeColorAdapter.dividerColor(Color(0xFF242830))))

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("სწრაფი ჩართვა", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        Text("მიტანა საქართველოში", color = Color(0xFF6E7582), fontSize = 9.sp, textAlign = TextAlign.Center)
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(AppThemeColorAdapter.dividerColor(Color(0xFF242830))))

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(3.dp))
                        Text("BEKASERVICE", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        Text("სანდო სერვისი", color = Color(0xFF6E7582), fontSize = 9.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

        // Pull-to-Refresh ანიმირებული ინდიკატორი (მოძრაობს ჩამოთრევის დონის შესაბამისად)
        if (rawPullDistancePx > 1f || isRefreshing) {
            BekaPullToRefreshIndicator(
                pullProgress = pullProgress,
                rawPullDistancePx = rawPullDistancePx,
                isRefreshing = isRefreshing,
                isRefreshCompleted = isRefreshCompleted,
                topInsetPadding = contentPadding.calculateTopPadding(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .testTag("home_pull_refresh_indicator")
            )
        }
    }
}

@Composable
fun BekaPullToRefreshIndicator(
    pullProgress: Float,
    rawPullDistancePx: Float,
    isRefreshing: Boolean,
    isRefreshCompleted: Boolean,
    topInsetPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val isDark = AppPreferencesManager.isDarkMode
    val density = LocalDensity.current

    val clampedProgress = pullProgress.coerceIn(0f, 1f)
    val isReadyToRefresh = pullProgress >= 1f

    val infiniteTransition = rememberInfiniteTransition(label = "pullRefreshInfinite")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 760, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pullRefreshSpinAngle"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pullRefreshPulse"
    )

    val readyScale by animateFloatAsState(
        targetValue = when {
            isRefreshCompleted -> 1.06f
            isRefreshing -> 1.0f
            isReadyToRefresh -> 1.04f
            else -> (0.68f + clampedProgress * 0.32f).coerceIn(0.68f, 1.0f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pullRefreshScale"
    )

    // ინდიკატორი ჩამოდის ჩამოთრევის დონის პროპორციულად
    val indicatorSlideDp = with(density) { (rawPullDistancePx * 0.42f).toDp().coerceAtMost(44.dp) }
    val indicatorAlpha = (clampedProgress * 1.45f).coerceIn(0f, 1f)

    val accentColor = when {
        isRefreshCompleted -> Color(0xFF4ADE80)
        isReadyToRefresh || isRefreshing -> Color(0xFFFF6A00)
        else -> Color(0xFFFF8533)
    }

    val containerBg = if (isDark) {
        Color(0xFF141821).copy(alpha = 0.95f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.97f)
    }

    val borderColor = when {
        isRefreshCompleted -> Color(0xFF4ADE80).copy(alpha = 0.75f)
        isReadyToRefresh || isRefreshing -> Color(0xFFFF6A00).copy(alpha = 0.80f)
        isDark -> Color(0xFF2D3546)
        else -> Color(0xFFCBD5E1)
    }

    Box(
        modifier = modifier
            .padding(top = topInsetPadding + 4.dp)
            .offset(y = indicatorSlideDp - 18.dp)
            .graphicsLayer {
                alpha = indicatorAlpha
                scaleX = readyScale
                scaleY = readyScale
            }
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = containerBg,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
            shadowElevation = if (isDark) 10.dp else 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ორბიტალური რგოლი + ცენტრალური ხატულა, რომელიც ბრუნავს ჩამოთრევის დონის შესაბამისად
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 2.6.dp.toPx()
                        val radius = (size.minDimension - strokeW) / 2f
                        val centerOffset = Offset(size.width / 2f, size.height / 2f)

                        // 1. ფონური ტრეკი
                        drawCircle(
                            color = accentColor.copy(alpha = if (isDark) 0.16f else 0.14f),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = strokeW)
                        )

                        // 2. 8 ორბიტალური წერტილი, რომლებიც ინთება ჩამოთრევის დონის ზრდასთან ერთად
                        val totalTicks = 8
                        val activeTicks = if (isRefreshing || isRefreshCompleted) {
                            totalTicks
                        } else {
                            (clampedProgress * totalTicks).toInt()
                        }
                        for (i in 0 until totalTicks) {
                            val angleRad = Math.toRadians((i * (360.0 / totalTicks)) - 90.0)
                            val dotX = centerOffset.x + (radius * 0.72f * kotlin.math.cos(angleRad)).toFloat()
                            val dotY = centerOffset.y + (radius * 0.72f * kotlin.math.sin(angleRad)).toFloat()
                            val isTickActive = i < activeTicks
                            drawCircle(
                                color = if (isTickActive) accentColor.copy(alpha = 0.85f * pulseGlow) else accentColor.copy(alpha = 0.15f),
                                radius = if (isTickActive) 1.5.dp.toPx() else 1.dp.toPx(),
                                center = Offset(dotX, dotY)
                            )
                        }

                        // 3. პროგრესის რკალი (იზრდება ჩამოთრევის დონის მიხედვით ან ბრუნავს განახლებისას)
                        val startAngle = if (isRefreshing && !isRefreshCompleted) spinAngle - 90f else -90f
                        val sweepAngle = when {
                            isRefreshCompleted -> 360f
                            isRefreshing -> 250f
                            else -> (clampedProgress * 330f).coerceIn(8f, 330f)
                        }
                        drawArc(
                            color = accentColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }

                    // ცენტრალური ხატულა (ბრუნავს ჩამოთრევის პროპორციულად: pullProgress * 240°)
                    val iconRotation = when {
                        isRefreshCompleted -> 0f
                        isRefreshing -> spinAngle
                        else -> pullProgress * 240f
                    }
                    Icon(
                        imageVector = if (isRefreshCompleted) Icons.Default.Check else Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = accentColor,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                rotationZ = iconRotation
                            }
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = when {
                            isRefreshCompleted -> "განახლდა წარმატებით!"
                            isRefreshing -> "მიმდინარეობს განახლება..."
                            isReadyToRefresh -> "გაუშვით განახლებისთვის ✨"
                            else -> "ჩამოწიეთ განახლებისთვის • ${(clampedProgress * 100).toInt()}%"
                        },
                        color = when {
                            isRefreshCompleted -> if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                            isReadyToRefresh || isRefreshing -> if (isDark) Color.White else Color(0xFF0F172A)
                            isDark -> Color(0xFFE2E8F0)
                            else -> Color(0xFF1E293B)
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )

                    // ჩამოთრევის დონის პროპორციული მიკრო-პროგრეს ზოლი
                    Box(
                        modifier = Modifier
                            .width(136.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isDark) Color(0xFF262D3B) else Color(0xFFE2E8F0))
                    ) {
                        val barFraction = if (isRefreshing || isRefreshCompleted) 1f else clampedProgress
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(barFraction)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = if (isRefreshCompleted) {
                                            listOf(Color(0xFF22C55E), Color(0xFF4ADE80))
                                        } else {
                                            listOf(Color(0xFFFF6A00), Color(0xFFFFAE52))
                                        }
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 3. სრული კატალოგის გვერდი (Catalog Screen - Tab 1)
// ==============================================================================
@Composable
fun CatalogScreen(
    products: List<ProductItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onProductClick: (ProductItem) -> Unit,
    onAddToCart: (ProductItem) -> Unit,
    onToggleFavorite: (ProductItem) -> Unit,
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    val baseCategories = listOf("ყველა", "პროგრამული სერვისები", "ეკრანები", "ელემენტები", "დამტენის პორტები", "კამერები", "კორპუსები", "დინამიკები")
    val categories = remember(products) {
        val dynamicCats = products.map { it.category.trim() }.filter { it.isNotBlank() }
        (baseCategories + dynamicCats).distinct()
    }
    var selectedBrand by remember { mutableStateOf("ყველა") }
    var selectedSubcategory by remember { mutableStateOf("ყველა") }
    var sortBy by remember { mutableStateOf("default") }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != "პროგრამული სერვისები") {
            selectedSubcategory = "ყველა"
        }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory, selectedBrand, sortBy, selectedSubcategory) {
        optimizeSearchFilter(products, searchQuery, selectedCategory, selectedBrand, sortBy, selectedSubcategory)
    }

    val focusManager = LocalFocusManager.current
    val isDark = AppPreferencesManager.isDarkMode
    val isCatalogSearchActive = searchQuery.isNotEmpty()
    val catalogSearchBorderColor by animateColorAsState(
        targetValue = if (isCatalogSearchActive) Color(0xFFFF7A00) else if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "catalogSearchBorder"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "catalog_title_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedCategory == "პროგრამული სერვისები") "პროგრამული სერვისები" else "სრული კატალოგი",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("სულ: ${filteredProducts.size}", color = Color(0xFFFF7A00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "catalog_search_box") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                Modifier.blur(14.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xFF171A22).copy(alpha = 0.80f),
                                        Color(0xFF101319).copy(alpha = 0.88f)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFFFFFFF).copy(alpha = 0.86f),
                                        Color(0xFFF1F5F9).copy(alpha = 0.92f)
                                    )
                                }
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, catalogSearchBorderColor),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isCatalogSearchActive) Color(0xFFFF7A00) else Color(0xFF888E9B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = AppThemeColorAdapter.inputTextColor(),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(Color(0xFFFF7A00)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("catalog_search_input"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "ძებნა კატალოგში (ChatGPT, Google AI, Canva, iPhone)...",
                                        color = Color(0xFF6E7582),
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        )
                        AnimatedVisibility(
                            visible = isCatalogSearchActive,
                            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
                            exit = fadeOut(tween(150)) + scaleOut(tween(150))
                        ) {
                            IconButton(
                                onClick = {
                                    onSearchChange("")
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
            item(span = { GridItemSpan(maxLineSpan) }, key = "catalog_filters_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // კატეგორიების ჰორიზონტალური ზოლი
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            val catChipBg by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFFFF7A00) else if (isDark) Color(0xFF161922).copy(alpha = 0.75f) else Color(0xFFFFFFFF).copy(alpha = 0.85f),
                                animationSpec = tween(200),
                                label = "catalogCatBg_$cat"
                            )
                            val catChipBorder by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFFFF7A00) else if (isDark) Color(0xFF282F3C).copy(alpha = 0.75f) else Color(0xFFCBD5E1),
                                animationSpec = tween(200),
                                label = "catalogCatBorder_$cat"
                            )
                            val catChipText by animateColorAsState(
                                targetValue = if (isSelected) Color.White else if (isDark) Color(0xFFA0A5B0) else Color(0xFF334155),
                                animationSpec = tween(200),
                                label = "catalogCatText_$cat"
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = catChipBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, catChipBorder),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onCategoryChange(cat) }
                            ) {
                                Text(
                                    cat,
                                    color = catChipText,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // ქვეკატეგორიები პროგრამული სერვისებისთვის ან ბრენდების ფილტრი ნაწილებისთვის
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCategory == "პროგრამული სერვისები") {
                            val dynamicSubcats = remember(products) {
                                val extra = products
                                    .filter { it.category == "პროგრამული სერვისები" || it.type == "software" }
                                    .map { it.subcategory.trim() }
                                    .filter { it.isNotBlank() }
                                (SoftwareCatalogData.softwareSubcategories + extra).distinct()
                            }
                            LazyRow(
                                modifier = Modifier.weight(1f).padding(end = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(dynamicSubcats) { subcat ->
                                    val isSel = selectedSubcategory == subcat
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) Color(0x33FF7A00) else Color(0xFF14171D),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFF7A00) else Color(0xFF20252F)),
                                        modifier = Modifier.clickable { selectedSubcategory = subcat }
                                    ) {
                                        Text(
                                            subcat,
                                            color = if (isSel) Color(0xFFFF7A00) else Color(0xFF8A92A0),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            val brands = remember(products) {
                                val baseBrands = listOf("ყველა", "Apple", "Samsung", "Xiaomi", "Google Pixel", "OnePlus", "Honor")
                                val extraBrands = products.map { it.brand.trim() }.filter { it.isNotBlank() }
                                (baseBrands + extraBrands).distinct()
                            }
                            LazyRow(
                                modifier = Modifier.weight(1f).padding(end = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(brands) { brand ->
                                    val isSel = selectedBrand == brand
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSel) Color(0x33FF7A00) else Color(0xFF14171D),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFF7A00) else Color(0xFF20252F)),
                                        modifier = Modifier.clickable { selectedBrand = brand }
                                    ) {
                                        Text(
                                            brand,
                                            color = if (isSel) Color(0xFFFF7A00) else Color(0xFF8A92A0),
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // სორტირების გადამრთველი
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF14171D),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF20252F)),
                            modifier = Modifier.clickable {
                                sortBy = when (sortBy) {
                                    "default" -> "price_asc"
                                    "price_asc" -> "price_desc"
                                    "price_desc" -> "rating"
                                    else -> "default"
                                }
                            }
                        ) {
                            Text(
                                when (sortBy) {
                                    "price_asc" -> "ფასი: ↑"
                                    "price_desc" -> "ფასი: ↓"
                                    "rating" -> "რეიტინგი ⭐"
                                    else -> "სორტირება ⇅"
                                },
                                color = Color(0xFFFF7A00),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            items(filteredProducts, key = { it.id }) { product ->
                ProductPhotoCard(
                    product = product,
                    onClick = { onProductClick(product) },
                    onAddToCart = { onAddToCart(product) },
                    onToggleFavorite = { onToggleFavorite(product) }
                )
            }
        }
    }

// ==============================================================================
// 4. AI დიაგნოსტიკის სამუშაო სივრცე (Diagnostics Screen - Tab 2)
// ==============================================================================
@Composable
fun DiagnosticsScreen(
    onProductClick: (ProductItem) -> Unit,
    products: List<ProductItem>,
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    DiagnosticsWorkspaceScreen(
        contentPadding = contentPadding,
        onProductClick = onProductClick,
        products = products
    )
}

// ==============================================================================
// 5. პროდუქტის ბარათი (Product Photo Card)
// ==============================================================================
@Composable
fun ProductPhotoCard(
    product: ProductItem,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isDigitalSoftware = SoftwareCatalogData.isDigitalSoftwareService(product)
    val cardSubtitle = remember(product.shortDescription, product.compatibleModels, product.brand, product.warranty, product.category) {
        product.shortDescription.ifBlank {
            product.compatibleModels.ifBlank {
                listOf(product.brand, product.warranty.ifBlank { product.category })
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF16191E),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDigitalSoftware) Color(0xFF28303F) else Color(0xFF242830)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF0F1115)))
            ) {
                ProductSmartVisual(
                    product = product,
                    modifier = Modifier.fillMaxSize(),
                    compact = true
                )

                // Favorite Button
                Surface(
                    onClick = onToggleFavorite,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .testTag("favorite_button_${product.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = product.isFavorite,
                            label = "fav_anim"
                        ) { isFav ->
                            Icon(
                                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Color(0xFFFF6A00) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Rating & Availability Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Text(
                            "⭐ ${product.rating}",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (product.inStock) Color(0xCC0F291A) else Color(0xCC2D1418)
                    ) {
                        Text(
                            if (product.inStock) "ხელმისაწვდომია" else "მიუწვდომელია",
                            color = if (product.inStock) Color(0xFF4ADE80) else Color(0xFFF87171),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title (uniform 2-line height across all 117 cards)
            Text(
                text = product.title,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2,
                lineHeight = 15.5.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Subtitle / short description / compatible models (uniform 2-line height)
            Text(
                text = cardSubtitle,
                color = Color(0xFF9BA3B0),
                fontSize = 10.sp,
                maxLines = 2,
                minLines = 2,
                lineHeight = 13.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Price
            Text(
                text = product.price,
                color = Color(0xFFFF7A00),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Card Actions: [ დეტალურად ] + [ შეკვეთა / 🛒 კალათაში ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E232D),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3545)),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(34.dp)
                        .clickable { onClick() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            "დეტალურად",
                            color = Color(0xFFE2E8F0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = onAddToCart,
                    enabled = product.inStock,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF7A00),
                        disabledContainerColor = Color(0xFF262A34)
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1.05f)
                        .height(34.dp)
                ) {
                    Text(
                        if (isDigitalSoftware) "შეკვეთა" else "🛒 კალათაში",
                        color = if (product.inStock) Color.White else Color(0xFF6E7582),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ==============================================================================
// ==============================================================================
// 6. Gemini AI დიაგნოსტიკური და პროგრამული კონსულტანტის სერვისი (BEKASERVICE Exclusive)
// ==============================================================================
object GeminiDiagnosticService {
    private const val MODEL = "gemini-2.5-flash"
    private val CANDIDATE_MODELS = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
    private const val BASE_SYSTEM_INSTRUCTION = """
შენ ხარ „ბექასერვისის“ (BEKASERVICE) ოფიციალური AI ტექნიკოს-ინჟინერი, დიაგნოსტიკოსი და ციფრული პროგრამული სერვისების კონსულტანტი.
ესაუბრე მომხმარებელს ცოცხლად, პროფესიონალურად, ზუსტად და ადამიანურად ქართულ ენაზე.

სავალდებულო ტექნიკური და დიაგნოსტიკური წესები:
1. პირველ რიგში უპასუხე უშუალოდ მომხმარებლის მიერ დასმულ კონკრეტულ კითხვას ან აღწერილ სიმპტომს. არ შეცვალო თემა.
2. მკაფიოდ განასხვავე ერთმანეთისგან:
   - სიმპტომი (რასაც მომხმარებელი ხედავს ან აღწერს);
   - შესაძლო მიზეზი (ჰიპოთეზა, რომელიც საჭიროებს გადამოწმებას);
   - დადასტურებული ფაქტი (რაც უკვე შემოწმებული და დადასტურებულია).
3. არ მოიგონო პრობლემა, რომელიც მომხმარებელს არ უხსენებია. არ წარმოადგინო სავარაუდო მიზეზი როგორც 100%-ით დადასტურებული დაზიანება.
4. არ შესთავაზო დაუკავშირებელი სერვისი ან კატალოგის პროდუქტი, თუ ის პირდაპირ არ უკავშირდება მომხმარებლის მიერ აღწერილ პრობლემას ან მოდელს.
5. თუ ინფორმაცია არასაკმარისია, დასვი 1–2 ზუსტი, მოკლე დამაზუსტებელი კითხვა.
6. შესთავაზე მომხმარებელს უსაფრთხო პირველადი შემოწმების ნაბიჯები მარტივიდან რთულისკენ.
7. განმარტე, რა შემთხვევაშია აუცილებელი სერვისცენტრში ფიზიკური დიაგნოსტიკა.
8. არ დააბრალო პრობლემა კონკრეტულ დეტალს (მაგალითად, ელემენტს ან დედაპლატას), თუ სიმპტომი ამას პირდაპირ არ ადასტურებს.
9. არასოდეს განაცხადო, რომ ჩაატარე ტელეფონის სკანირება ან სენსორების გაზომვა, თუ ეს რეალურად არ მომხდარა.
10. არასდროს ახსენო სხვა სახელოსნო ან მაღაზია — მიუთითე მხოლოდ BEKASERVICE-ზე (ქ. ახალციხე, კოსტავას 10, WhatsApp: +995 598 23 33 32).
"""

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    fun isDiagnosticFaultQuery(qLower: String): Boolean {
        val q = qLower.lowercase()
        return q.contains("არ ირთვება") || q.contains("არ იტენება") || q.contains("არ ფოკუსირდება") ||
                q.contains("არ მუშაობს") || q.contains("არ რეაგირებს") || q.contains("გაშავდა") ||
                q.contains("ჩაქრა") || q.contains("ციმციმებს") || q.contains("ზოლ") || q.contains("ლაქ") ||
                q.contains("ბზარ") || q.contains("გატყდა") || q.contains("დამივარდა") || q.contains("დავარდა") ||
                q.contains("წყალ") || q.contains("სითხ") || q.contains("დასველ") || q.contains("ნესტ") ||
                q.contains("ცხელდება") || q.contains("ხურდება") || q.contains("ჯდება") || q.contains("ითიშება") ||
                q.contains("გაბერ") || q.contains("ლოგო") || q.contains("bootloop") || q.contains("რესტარტ") ||
                q.contains("ფოკუს") || q.contains("ბუნდოვან") || q.contains("კანკალ") || q.contains("ხმა არ") ||
                q.contains("შხრიალ") || q.contains("პრობლემა") || q.contains("ხარვეზ") || q.contains("დაზიან") ||
                q.contains("გაფუჭდა")
    }

    fun extractDeviceFromText(text: String): String? {
        val q = text.lowercase()
        return when {
            q.contains("16 pro max") -> "iPhone 16 Pro Max"
            q.contains("16 pro") -> "iPhone 16 Pro"
            q.contains("iphone 16") -> "iPhone 16"
            q.contains("15 pro max") -> "iPhone 15 Pro Max"
            q.contains("15 pro") -> "iPhone 15 Pro"
            q.contains("iphone 15") -> "iPhone 15"
            q.contains("14 pro max") -> "iPhone 14 Pro Max"
            q.contains("14 pro") -> "iPhone 14 Pro"
            q.contains("iphone 14") -> "iPhone 14"
            q.contains("13 pro max") -> "iPhone 13 Pro Max"
            q.contains("13 pro") -> "iPhone 13 Pro"
            q.contains("iphone 13") -> "iPhone 13"
            q.contains("12 pro") -> "iPhone 12 Pro"
            q.contains("iphone 12") -> "iPhone 12"
            q.contains("11 pro") -> "iPhone 11 Pro"
            q.contains("iphone 11") -> "iPhone 11"
            q.contains("s25 ultra") -> "Samsung Galaxy S25 Ultra"
            q.contains("s24 ultra") -> "Samsung Galaxy S24 Ultra"
            q.contains("s24") -> "Samsung Galaxy S24"
            q.contains("s23 ultra") -> "Samsung Galaxy S23 Ultra"
            q.contains("s23") -> "Samsung Galaxy S23"
            q.contains("s22") -> "Samsung Galaxy S22"
            q.contains("a55") -> "Samsung Galaxy A55"
            q.contains("a54") -> "Samsung Galaxy A54"
            q.contains("a53") -> "Samsung Galaxy A53"
            q.contains("redmi note 13") -> "Redmi Note 13"
            q.contains("redmi note 12") -> "Redmi Note 12"
            q.contains("pixel 9") -> "Google Pixel 9"
            q.contains("pixel 8") -> "Google Pixel 8"
            q.contains("pixel 7") -> "Google Pixel 7"
            q.contains("pixel") -> "Google Pixel"
            q.contains("samsung") || q.contains("სამსუნგ") -> "Samsung Galaxy"
            q.contains("iphone") || q.contains("აიფონ") -> "iPhone"
            q.contains("xiaomi") || q.contains("შაომ") || q.contains("redmi") -> "Xiaomi / Redmi"
            else -> null
        }
    }

    private fun buildDynamicCatalogContext(products: List<ProductItem>): String {
        val catalog = if (products.isNotEmpty()) products else ProductRepository.getAllProducts()
        if (catalog.isEmpty()) return ""
        val sb = StringBuilder("\n\n=== BEKASERVICE LIVE სერვისებისა და პროდუქტების კატალოგი ===\n")
        catalog.forEach { p ->
            val status = if (p.inStock) "ხელმისაწვდომია" else "დროებით მიუწვდომელია"
            val isSoft = SoftwareCatalogData.isDigitalSoftwareService(p)
            if (isSoft) {
                sb.append("• [პროგრამული სერვისი ID:${p.id}] ${p.title} (${p.subcategory.ifBlank { p.category }}) | ფასი: ${p.price} | სტატუსი: $status | პაკეტი: ${p.offerType} | ვადა: ${p.duration} | ანგარიში: ${p.accountType}\n")
                if (p.shortDescription.isNotBlank()) sb.append("  აღწერა: ${p.shortDescription}\n")
                if (p.features.isNotEmpty()) sb.append("  ფუნქციები: ${p.features.joinToString("; ")}\n")
                if (p.activationTerms.isNotBlank()) sb.append("  პირობები: ${p.activationTerms}\n")
            } else {
                sb.append("• [პროდუქტი/ნაწილი ID:${p.id}] ${p.title} (${p.category}) | ბრენდი: ${p.brand} | მოდელები: ${p.compatibleModels} | ფასი: ${p.price} | გარანტია: ${p.warranty} | სტატუსი: $status\n")
                if (p.shortDescription.isNotBlank()) sb.append("  აღწერა: ${p.shortDescription}\n")
            }
        }
        return sb.toString()
    }

    fun findRelevantProducts(query: String, products: List<ProductItem>): List<ProductItem> {
        val q = query.lowercase().trim()
        if (q.length < 2) return emptyList()
        val catalog = if (products.isNotEmpty()) products else ProductRepository.getAllProducts()

        val isGreetingOrGeneral = q == "გამარჯობა" || q == "სალამი" || q.contains("როგორ ხარ") ||
                q.contains("მადლობა") || q.contains("მისამართ") || q.contains("სად ხართ")
        if (isGreetingOrGeneral) return emptyList()

        // Do NOT push unrelated catalog items when user describes a generic hardware symptom without a specific catalog model/service
        val isHardwareSymptom = isDiagnosticFaultQuery(q)

        // 1. კონკრეტული სერვისის პირდაპირი ზუსტი მოთხოვნა:
        if (q.contains("canva") || q.contains("კანვა")) {
            val canva = catalog.find { it.title.contains("Canva", ignoreCase = true) || it.id == 118 }
            if (canva != null) return listOf(canva)
        }

        if (q.contains("capcut") || q.contains("კაპკატ")) {
            val capcut = catalog.find { it.title.contains("CapCut", ignoreCase = true) || it.id == 116 }
            if (capcut != null) return listOf(capcut)
        }

        if (q.contains("adobe") || q.contains("ადობ") || q.contains("photoshop") || q.contains("ფოტოშოპ") || q.contains("premiere") || q.contains("პრემიერ")) {
            val adobe = catalog.find { it.title.contains("Adobe", ignoreCase = true) || it.id == 117 }
            if (adobe != null) return listOf(adobe)
        }

        if (q.contains("youtube") || q.contains("იუთუბ")) {
            val yt = catalog.find { it.title.contains("YouTube", ignoreCase = true) || it.id == 119 }
            if (yt != null) return listOf(yt)
        }

        if (q.contains("spotify") || q.contains("სპოტიფაი")) {
            val spot = catalog.find { it.title.contains("Spotify", ignoreCase = true) || it.id == 120 }
            if (spot != null) return listOf(spot)
        }

        if (q.contains("perplexity") || q.contains("პერპლექს")) {
            val p = catalog.find { it.title.contains("Perplexity", ignoreCase = true) || it.id == 109 }
            if (p != null) return listOf(p)
        }

        if (q.contains("cursor") || q.contains("კურსორ")) {
            val p = catalog.find { it.title.contains("Cursor", ignoreCase = true) || it.id == 110 }
            if (p != null) return listOf(p)
        }

        if (q.contains("lovable") || q.contains("ლავაბლ")) {
            val p = catalog.find { it.title.contains("Lovable", ignoreCase = true) || it.id == 111 }
            if (p != null) return listOf(p)
        }

        if (q.contains("grok") || q.contains("გროკ")) {
            val groks = catalog.filter { it.title.contains("Grok", ignoreCase = true) || it.id in listOf(105, 106) }
            if (groks.isNotEmpty()) return groks.take(2)
        }

        if (q.contains("claude") || q.contains("კლოდ")) {
            val claudes = catalog.filter { it.title.contains("Claude", ignoreCase = true) || it.id in listOf(107, 108) }
            if (claudes.isNotEmpty()) return claudes.take(2)
        }

        if (q.contains("chatgpt") || q.contains("ჩატჯიპიტი") || q.contains("gpt") || q.contains("openai")) {
            if (q.contains("plus") || q.contains("პლუს")) {
                val plus = catalog.find { it.id == 101 || (it.title.contains("ChatGPT", ignoreCase = true) && it.title.contains("Plus", ignoreCase = true)) }
                if (plus != null) return listOf(plus)
            }
            if (q.contains("pro") || q.contains("პრო")) {
                val pro = catalog.find { it.id == 102 || (it.title.contains("ChatGPT", ignoreCase = true) && it.title.contains("Pro", ignoreCase = true)) }
                if (pro != null) return listOf(pro)
            }
            val gpts = catalog.filter { it.id in listOf(101, 102) || it.title.contains("ChatGPT", ignoreCase = true) }
            if (gpts.isNotEmpty()) return gpts.take(2)
        }

        if (q.contains("google ai") || q.contains("gemini") || q.contains("ჯემინი") || q.contains("18 თვ")) {
            val geminis = catalog.filter { it.id in listOf(103, 104, 115) || it.title.contains("Google AI", ignoreCase = true) }
            if (geminis.isNotEmpty()) return geminis.take(3)
        }

        // Google Storage / Gmail (only when not describing phone internal memory bootloop/full)
        if (!isHardwareSymptom && (q.contains("gmail") || q.contains("ჯიმეილ") || q.contains("google storage") || q.contains("google one") || q.contains("2tb") || q.contains("5tb") || (q.contains("მეხსიერება") && q.contains("cloud")))) {
            val storages = catalog.filter { it.id in listOf(112, 113, 114, 115) }
            if (storages.isNotEmpty()) return storages.take(3)
        }

        if (q.contains("windows") || q.contains("ვინდოუს")) {
            val win = catalog.find { it.id == 121 || it.title.contains("Windows", ignoreCase = true) }
            if (win != null) return listOf(win)
        }

        if (q.contains("office") || q.contains("ოფის") || q.contains("microsoft 365") || q.contains("word") || q.contains("excel") || q.contains("ltsc")) {
            val offices = catalog.filter { it.id in listOf(122, 123, 124) || it.title.contains("Office", ignoreCase = true) }
            if (offices.isNotEmpty()) return offices.take(3)
        }

        if (!isHardwareSymptom && (q.contains("მულტიმედია") || q.contains("ვიდეო და დიზაინ") || (q.contains("დიზაინ") && !q.contains("canva") && !q.contains("adobe")) || (q.contains("მონტაჟ") && !q.contains("capcut")))) {
            return listOf(118, 116, 117).mapNotNull { id -> catalog.find { it.id == id } }
        }

        if (!isHardwareSymptom && (q.contains("სტუდენტ") || q.contains("სწავლ"))) {
            return listOf(103, 101, 109, 118).mapNotNull { id -> catalog.find { it.id == id } }
        }

        // If it's a hardware symptom without an explicit device model, do not attach random catalog products
        if (isHardwareSymptom) {
            val detectedModel = extractDeviceFromText(query)
            if (detectedModel == null) {
                if (q.contains("frp") || q.contains("google account")) {
                    return catalog.filter { it.title.contains("FRP", ignoreCase = true) }.take(2)
                }
                if (q.contains("icloud") || q.contains("apple id")) {
                    return catalog.filter { it.title.contains("iCloud", ignoreCase = true) }.take(2)
                }
                return emptyList()
            }
            val modelLower = detectedModel.lowercase()
            return catalog.filter { prod ->
                !SoftwareCatalogData.isDigitalSoftwareService(prod) &&
                        (prod.title.lowercase().contains(modelLower) || prod.compatibleModels.lowercase().contains(modelLower)) &&
                        ((q.contains("ეკრან") && prod.category.contains("ეკრან")) ||
                                ((q.contains("ელემენტ") || q.contains("ბატარე")) && prod.category.contains("ელემენტ")) ||
                                ((q.contains("პორტ") || q.contains("დამტენ") || q.contains("იტენება")) && prod.category.contains("პორტ")))
            }.take(2)
        }

        // 2. დინამიური ამოცნობა ადმინის მიერ ახლად დამატებული ან სხვა სერვისებისთვის კატალოგში
        val dynamicExact = catalog.filter { prod ->
            val titleLower = prod.title.lowercase()
            val brandLower = prod.brand.lowercase()
            val keywordsLower = prod.searchKeywords.lowercase()
            titleLower == q ||
                    (titleLower.length >= 4 && q.contains(titleLower)) ||
                    (brandLower.length >= 4 && q.contains(brandLower) && !isHardwareSymptom) ||
                    (keywordsLower.isNotBlank() && keywordsLower.split(",").any { kw ->
                        val cleanKw = kw.trim()
                        cleanKw.length >= 4 && q.contains(cleanKw)
                    })
        }
        if (dynamicExact.isNotEmpty()) {
            return dynamicExact.take(3)
        }

        return emptyList()
    }

    suspend fun diagnose(
        history: List<ChatMessage>,
        latestUserMessage: String,
        products: List<ProductItem> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val cleanInput = latestUserMessage.trim()
        val apiKey = BuildConfig.GEMINI_API_KEY
        val fullSystemInstruction = BASE_SYSTEM_INSTRUCTION + buildDynamicCatalogContext(products)
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            for (modelName in CANDIDATE_MODELS) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

                    val requestJson = JSONObject().apply {
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", fullSystemInstruction))
                            })
                        })

                        val contentsArray = JSONArray()

                        val historyTurns = if (history.isNotEmpty() && history.last().text.trim() == cleanInput && history.last().sender == "user") {
                            history.dropLast(1).takeLast(8)
                        } else {
                            history.takeLast(8)
                        }

                        var lastRole: String? = null
                        for (msg in historyTurns) {
                            if (msg.text.isBlank()) continue
                            val role = if (msg.sender == "user") "user" else "model"
                            if (role != lastRole) {
                                contentsArray.put(JSONObject().apply {
                                    put("role", role)
                                    put("parts", JSONArray().apply {
                                        put(JSONObject().put("text", msg.text))
                                    })
                                })
                                lastRole = role
                            }
                        }

                        contentsArray.put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", cleanInput))
                            })
                        })

                        put("contents", contentsArray)

                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.35)
                            put("topP", 0.9)
                            put("topK", 40)
                        })
                    }

                    val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url(url)
                        .post(body)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val responseBodyString = response.body?.string() ?: ""
                            val responseJson = JSONObject(responseBodyString)
                            val candidates = responseJson.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val candidate = candidates.getJSONObject(0)
                                val content = candidate.optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    val replyText = parts.getJSONObject(0).optString("text", "")
                                    if (replyText.isNotBlank()) {
                                        return@withContext replyText
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Try next candidate model
                }
            }
        }

        return@withContext generateLocalDiagnostic(cleanInput, products, history)
    }

    private fun formatSoftwareProductSummary(p: ProductItem): String {
        val statusText = if (p.inStock) "✅ ხელმისაწვდომია" else "⏳ დროებით მიუწვდომელია"
        val featuresText = if (p.features.isNotEmpty()) {
            p.features.take(4).joinToString("\n") { "  • $it" }
        } else ""
        return "🔹 **${p.title}** ($statusText)\n" +
                "• **ფასი:** ${p.price} | **ვადა:** ${p.duration}\n" +
                "• **პაკეტი:** ${p.offerType} (${p.accountType})\n" +
                "• **აღწერა:** ${p.shortDescription}\n" +
                (if (featuresText.isNotBlank()) "• **მთავარი უპირატესობები:**\n$featuresText\n" else "") +
                (if (p.activationTerms.isNotBlank()) "• **აქტივაცია:** ${p.activationTerms}\n" else "")
    }

    fun generateLocalDiagnostic(
        query: String,
        products: List<ProductItem> = emptyList(),
        history: List<ChatMessage> = emptyList()
    ): String {
        val q = query.lowercase().trim()
        val catalog = if (products.isNotEmpty()) products else ProductRepository.getAllProducts()

        val priorUserMessages = history
            .filter { it.sender == "user" && it.text.trim() != query.trim() }
            .map { it.text.lowercase() }
        val combinedHistoryText = (priorUserMessages.takeLast(3) + listOf(q)).joinToString(" | ")
        val isHardwareSymptom = isDiagnosticFaultQuery(q)

        // =========================================================================
        // 0. კონკრეტული სერვისები — პირდაპირი, დეტალური რეკომენდაცია (მხოლოდ როცა არ არის აპარატურული სიმპტომი)
        // =========================================================================
        if (q.contains("canva") || q.contains("კანვა")) {
            val canva = catalog.find { it.title.contains("Canva", ignoreCase = true) || it.id == 118 }
            if (canva != null) {
                return "🎨 **Canva Pro — ოფიციალური პრემიუმ წვდომა BEKASERVICE-ში:**\n\n" +
                        formatSoftwareProductSummary(canva) + "\n" +
                        "✨ **რა შესაძლებლობებს გაძლევთ Canva Pro?**\n" +
                        "• 100+ მილიონი პრემიუმ ფოტო, ვიდეო, აუდიო და გრაფიკული ელემენტი\n" +
                        "• AI Magic Studio (ფონის წაშლა 1 კლიკით, Magic Eraser, Magic Expand)\n" +
                        "• Brand Kit საკუთარი ლოგოებისა და შრიფტების შესანახად\n" +
                        "• 100 GB Cloud მეხსიერება და სრული მხარდაჭერა ყველა მოწყობილობაზე (iOS, Android, Web)\n\n" +
                        "შესაკვეთად დააჭირეთ ქვემოთ მოცემულ „შეკვეთა“ ღილაკს ან გახსენით დეტალები!"
            }
        }

        if (q.contains("capcut") || q.contains("კაპკატ")) {
            val capcut = catalog.find { it.title.contains("CapCut", ignoreCase = true) || it.id == 116 }
            if (capcut != null) {
                return "🎬 **CapCut Pro — პროფესიონალური ვიდეო მონტაჟი BEKASERVICE-ში:**\n\n" +
                        formatSoftwareProductSummary(capcut) + "\n" +
                        "✨ **რა შესაძლებლობებს გაძლევთ CapCut Pro?**\n" +
                        "• AI ავტო-სუბტიტრები (Auto Captions) და Vocal Isolation (ხმის გასუფთავება)\n" +
                        "• AI Background Removal და 4K 60fps ექსპორტი Watermark-ის გარეშე\n" +
                        "• ყველა Pro ეფექტის, გადასვლისა და ფილტრის სრული განბლოკვა\n\n" +
                        "შესაკვეთად დააჭირეთ ქვემოთ მოცემულ „შეკვეთა“ ღილაკს ან გახსენით დეტალები!"
            }
        }

        if (q.contains("adobe") || q.contains("ადობ") || q.contains("photoshop") || q.contains("ფოტოშოპ") || q.contains("premiere") || q.contains("პრემიერ")) {
            val adobe = catalog.find { it.title.contains("Adobe", ignoreCase = true) || it.id == 117 }
            if (adobe != null) {
                return "🖌️ **Adobe Creative Cloud (All Apps) — ოფიციალური პაკეტი BEKASERVICE-ში:**\n\n" +
                        formatSoftwareProductSummary(adobe) + "\n" +
                        "✨ **პაკეტში შედის:** Photoshop, Illustrator, Premiere Pro, After Effects, Lightroom + Adobe Firefly AI გენერაციული ინსტრუმენტები და 100 GB მეხსიერება.\n\n" +
                        "შესაკვეთად დააჭირეთ ქვემოთ მოცემულ „შეკვეთა“ ღილაკს ან გახსენით დეტალები!"
            }
        }

        if (q.contains("youtube") || q.contains("იუთუბ")) {
            val yt = catalog.find { it.title.contains("YouTube", ignoreCase = true) || it.id == 119 }
            if (yt != null) {
                return "▶️ **YouTube Premium — რეკლამების გარეშე BEKASERVICE-ში:**\n\n" +
                        formatSoftwareProductSummary(yt) + "\n" +
                        "• ფონური რეჟიმი (Background Play) და ვიდეოების ჩამოტვირთვა ოფლაინ რეჟიმში\n" +
                        "• YouTube Music Premium სრული წვდომით.\n\n" +
                        "შესაკვეთად დააჭირეთ ქვემოთ მოცემულ „შეკვეთა“ ღილაკს!"
            }
        }

        if (q.contains("spotify") || q.contains("სპოტიფაი")) {
            val spot = catalog.find { it.title.contains("Spotify", ignoreCase = true) || it.id == 120 }
            if (spot != null) {
                return "🎵 **Spotify Premium — ულიმიტო მუსიკა BEKASERVICE-ში:**\n\n" +
                        formatSoftwareProductSummary(spot) + "\n" +
                        "• ულიმიტო გადახვევა, მაღალი ხარისხის აუდიო და სიმღერების ოფლაინ ჩაწერა.\n\n" +
                        "შესაკვეთად დააჭირეთ ქვემოთ მოცემულ „შეკვეთა“ ღილაკს!"
            }
        }

        if (q.contains("chatgpt") || q.contains("ჩატჯიპიტი") || q.contains("gpt") || q.contains("openai")) {
            val plus = catalog.find { it.id == 101 }
            val pro = catalog.find { it.id == 102 }
            return "🤖 **OpenAI ChatGPT Plus და ChatGPT Pro — შედარება და პირობები BEKASERVICE-ში:**\n\n" +
                    (if (plus != null) formatSoftwareProductSummary(plus) + "\n" else "") +
                    (if (pro != null) formatSoftwareProductSummary(pro) + "\n" else "") +
                    "💡 **განსხვავება:** **ChatGPT Plus** იდეალურია ყოველდღიური სამუშაოსთვის, სწავლისთვის, კოდის წერისა და სურათების გენერაციისთვის, ხოლო **ChatGPT Pro** განკუთვნილია მაქსიმალური დატვირთვის, ულიმიტო მსჯელობის (o1/o3 Pro) და მძიმე კვლევითი ამოცანებისთვის.\n\n" +
                    "ქვემოთ მოცემული ბარათებიდან შეგიძლიათ პირდაპირ გახსნათ დეტალები ან შეუკვეთოთ!"
        }

        if (q.contains("google ai") || q.contains("gemini") || q.contains("ჯემინი") || q.contains("18 თვ")) {
            val aiPro = catalog.find { it.id == 103 }
            val aiUltra = catalog.find { it.id == 104 }
            val bundle = catalog.find { it.id == 115 }
            return "✨ **Google AI Pro (18-თვიანი შეთავაზება) და Google AI Ultra BEKASERVICE-ში:**\n\n" +
                    (if (aiPro != null) formatSoftwareProductSummary(aiPro) + "\n" else "") +
                    (if (aiUltra != null) formatSoftwareProductSummary(aiUltra) + "\n" else "") +
                    (if (bundle != null) "🎁 **ასევე ხელმისაწვდომია:** ${bundle.title} (${bundle.price}) — ${bundle.shortDescription}\n\n" else "") +
                    "📌 **მნიშვნელოვანი განმარტება:** 18-თვიანი პერიოდი წარმოადგენს BEKASERVICE-ის სპეციალურ პრომო/სტუდენტურ აქტივაციას ოფიციალურ Google One + Gemini Advanced ეკოსისტემაზე (2TB მეხსიერებით)."
        }

        if (q.contains("claude") || q.contains("კლოდ")) {
            val priv = catalog.find { it.id == 107 }
            val shared = catalog.find { it.id == 108 }
            return "🧠 **Anthropic Claude Pro — Private vs Shared ანგარიშების შედარება:**\n\n" +
                    (if (priv != null) formatSoftwareProductSummary(priv) + "\n" else "") +
                    (if (shared != null) formatSoftwareProductSummary(shared) + "\n" else "") +
                    "🔐 **რომელი აირჩიოთ?** თუ მუშაობთ კონფიდენციალურ კოდზე ან პირად დოკუმენტებზე, გირჩევთ **Private Account**-ს (სრული პრივატულობით). თუ გსურთ ბიუჯეტური წვდომა Claude 3.7 Sonnet-ზე, აირჩიეთ **Shared Account**."
        }

        if (q.contains("grok") || q.contains("გროკ")) {
            val grok = catalog.find { it.id == 105 }
            val heavy = catalog.find { it.id == 106 }
            return "🚀 **xAI Grok SuperGrok და SuperGrok Heavy BEKASERVICE-ში:**\n\n" +
                    (if (grok != null) formatSoftwareProductSummary(grok) + "\n" else "") +
                    (if (heavy != null) formatSoftwareProductSummary(heavy) + "\n" else "") +
                    "ორივე პაკეტი ხელმისაწვდომია სწრაფი აქტივაციით და სრული მხარდაჭერით."
        }

        if (q.contains("perplexity") || q.contains("პერპლექს") || q.contains("cursor") || q.contains("კურსორ") || q.contains("lovable") || q.contains("ლავაბლ")) {
            val matched = listOf(109, 110, 111).mapNotNull { id -> catalog.find { it.id == id } }
            return "💻 **AI კვლევისა და პროგრამირების პრემიუმ ინსტრუმენტები BEKASERVICE-ში:**\n\n" +
                    matched.joinToString("\n") { formatSoftwareProductSummary(it) } +
                    "\nშეგიძლიათ გახსნათ სასურველი სერვისის ბარათი ქვემოთ და შეუკვეთოთ მყისიერად."
        }

        if (!isHardwareSymptom && (q.contains("gmail") || q.contains("ჯიმეილ") || q.contains("google storage") || q.contains("google one") || q.contains("2tb") || q.contains("5tb") || (q.contains("მეხსიერება") && q.contains("cloud")))) {
            val matched = listOf(112, 113, 114, 115).mapNotNull { id -> catalog.find { it.id == id } }
            return "☁️ **Google-ის ანგარიშები და Cloud მეხსიერების პაკეტები BEKASERVICE-ში:**\n\n" +
                    matched.joinToString("\n") { formatSoftwareProductSummary(it) }
        }

        if (!isHardwareSymptom && (q.contains("მულტიმედია") || q.contains("ვიდეო და დიზაინ") || q.contains("ყველა დიზაინ") || (q.contains("მონტაჟ") && q.contains("დიზაინ")))) {
            val matched = listOf(116, 117, 118, 119, 120).mapNotNull { id -> catalog.find { it.id == id } }
            return "🎨 **ვიდეო, დიზაინი და მულტიმედია პრემიუმ სერვისები BEKASERVICE-ში:**\n\n" +
                    matched.joinToString("\n") { formatSoftwareProductSummary(it) }
        }

        if (q.contains("windows") || q.contains("ვინდოუს") || q.contains("office") || q.contains("ოფის") || q.contains("microsoft 365") || q.contains("ltsc") || q.contains("word") || q.contains("excel")) {
            val matched = listOf(121, 122, 123, 124).mapNotNull { id -> catalog.find { it.id == id } }
            return "🪟 **Windows 11 Pro და Microsoft Office ლიცენზიების შედარება BEKASERVICE-ში:**\n\n" +
                    matched.joinToString("\n") { formatSoftwareProductSummary(it) } +
                    "\n💡 **განსხვავება Office-ის ვერსიებს შორის:**\n" +
                    "• **Office LTSC 2024** და **Office 2019** არის ერთჯერადი (უვადო) ლიცენზიები კონკრეტულ კომპიუტერზე ყოველთვიური გადასახადის გარეშე.\n" +
                    "• **Microsoft 365 Personal** არის 1-წლიანი ქლაუდ გამოწერა 1TB OneDrive მეხსიერებით და 5 მოწყობილობაზე სინქრონიზაციით."
        }

        if (!isHardwareSymptom && (q.contains("სტუდენტ") || q.contains("დიზაინერ") || q.contains("მონტაჟ") || q.contains("პროგრამისტ") || q.contains("დეველოპერ"))) {
            return "🎯 **BEKASERVICE-ის რეკომენდაციები საქმიანობის მიხედვით:**\n\n" +
                    "• 🎓 **სტუდენტებისა და მკვლევრებისთვის:** Google AI Pro (18 თვე + 2TB), ChatGPT Plus, Perplexity Pro, Canva Pro.\n" +
                    "• 🎬 **ვიდეო მონტაჟისა და დიზაინისთვის:** CapCut Pro, Adobe Creative Cloud (All Apps), Canva Pro.\n" +
                    "• 💻 **პროგრამისტებისა და დეველოპერებისთვის:** Cursor AI Pro, Claude Pro (Private Account), Lovable Pro, ChatGPT Plus.\n" +
                    "• 🏢 **საოფისე და ბიზნეს სამუშაოსთვის:** Windows 11 Pro Retail Key, Office LTSC 2024, Microsoft 365 Personal, Google Storage 2TB/5TB.\n\n" +
                    "ქვემოთ მოცემულ ბარათებზე დაჭერით შეგიძლიათ ნახოთ დეტალური პირობები და შეუკვეთოთ!"
        }

        // დინამიური ამოცნობა ნებისმიერი ახლად დამატებული სერვისისთვის (მხოლოდ როცა არ არის დიაგნოსტიკური სიმპტომი!)
        val isGreetingOrGeneral = q == "გამარჯობა" || q == "სალამი" || q.contains("როგორ ხარ") ||
                q.contains("მადლობა") || q.contains("მისამართ") || q.contains("სად ხართ")
        if (!isHardwareSymptom && !isGreetingOrGeneral) {
            val dynamicMatched = catalog.firstOrNull { prod ->
                val titleLower = prod.title.lowercase()
                (titleLower.length >= 4 && q.contains(titleLower)) ||
                        (prod.searchKeywords.isNotBlank() && prod.searchKeywords.split(",").any { kw ->
                            val cleanKw = kw.trim().lowercase()
                            cleanKw.length >= 4 && q.contains(cleanKw) && cleanKw != "ეკრანი" && cleanKw != "ელემენტი" && cleanKw != "პორტი" && cleanKw != "კამერა"
                        })
            }
            if (dynamicMatched != null) {
                val isSoft = SoftwareCatalogData.isDigitalSoftwareService(dynamicMatched)
                return if (isSoft) {
                    "💎 **${dynamicMatched.title} — ოფიციალური შეთავაზება BEKASERVICE-ში:**\n\n" +
                            formatSoftwareProductSummary(dynamicMatched) + "\n" +
                            "დეტალების სანახავად ან შესაკვეთად გამოიყენეთ ქვემოთ მოცემული ბარათი!"
                } else {
                    "🔧 **${dynamicMatched.title} (${dynamicMatched.brand}) BEKASERVICE-ში:**\n\n" +
                            "• **ფასი:** ${dynamicMatched.price} | **სტატუსი:** ${if (dynamicMatched.inStock) "ხელმისაწვდომია" else "შეკვეთით"}\n" +
                            "• **თავსებადი მოდელები:** ${dynamicMatched.compatibleModels}\n" +
                            "• **გარანტია:** ${dynamicMatched.warranty}\n" +
                            (if (dynamicMatched.shortDescription.isNotBlank()) "• **აღწერა:** ${dynamicMatched.shortDescription}\n\n" else "\n") +
                            "დეტალების სანახავად და შესაკვეთად გამოიყენეთ ქვემოთ მოცემული ბარათი!"
                }
            }
        }

        // 1. მოდელის დინამიური ამოცნობა (მიმდინარე კითხვიდან ან სესიის ისტორიიდან)
        val detectedDevice = extractDeviceFromText(query)
            ?: history.asReversed().firstNotNullOfOrNull { extractDeviceFromText(it.text) }
            ?: ""

        val devicePrefix = if (detectedDevice.isNotBlank()) "📱 მოდელი: **$detectedDevice**\n\n" else ""

        // =========================================================================
        // 1. მისალმება და ზრდილობიანი შეტყობინებები
        // =========================================================================
        val isGreeting = q == "გამარჯობა" || q == "გამარჯობათ" || q == "სალამი" || q == "მოგესალმებით" ||
                q == "დილა მშვიდობისა" || q == "საღამო მშვიდობისა" || q == "hello" || q == "hi" || q == "hey" ||
                q.startsWith("გამარჯობა") || q.startsWith("სალამი") || q.startsWith("მოგესალმებით")

        if (isGreeting && !isHardwareSymptom && !q.contains("ეკრან") && !q.contains("ელემენტ") && !q.contains("პორტ") &&
            !q.contains("frp") && !q.contains("icloud") && !q.contains("ბლოკ")
        ) {
            return "გამარჯობა! 👋 მოგესალმებით **BEKASERVICE**-ში. მე ვარ თქვენი AI ტექნიკოსი და პროგრამული სერვისების კონსულტანტი.\n\n" +
                    "რით შემიძლია დაგეხმაროთ დღეს?\n" +
                    "• 📱 **ტექნიკური დიაგნოსტიკა:** აღწერეთ ტელეფონის სიმპტომი (მაგ: ეკრანი არ ირთვება, ტელეფონი არ იტენება, კამერა არ ფოკუსირდება) და განვსაზღვროთ შესაძლო მიზეზები და უსაფრთხო შემოწმების ნაბიჯები.\n" +
                    "• 💻 **ციფრული პროგრამული სერვისები (24 პაკეტი):** Canva Pro, CapCut Pro, Adobe CC, YouTube/Spotify Premium, Windows 11 & Office\n" +
                    "• 🔓 **მობილურის განბლოკვა:** Google FRP, iCloud Activation, SIM Lock, Bootloop."
        }

        // =========================================================================
        // 2. როგორ ხარ / მდგომარეობა
        // =========================================================================
        if (q.contains("როგორ ხარ") || q.contains("როგორ ბრძანდებით") || q.contains("როგორ ხართ") || q == "how are you") {
            return "მადლობა, შესანიშნავად! 😊 მზად ვარ გაგიწიოთ კონსულტაცია როგორც ტელეფონის ტექნიკურ დიაგნოსტიკაზე, ისე პროგრამულ სერვისებზე.\n\nრა გაინტერესებთ დღეს?"
        }

        // =========================================================================
        // 3. მადლობა / კეთილი სურვილები
        // =========================================================================
        if (q == "მადლობა" || q == "დიდი მადლობა" || q == "გაიხარე" || q.contains("მადლობ") || q == "thanks" || q == "thank you") {
            return "გაიხარეთ, დიდი მადლობა თქვენ! 🤝 ყოველთვის მოხარული ვარ დაგეხმაროთ.\n\n📍 გელოდებით BEKASERVICE-ში: ქ. ახალციხე, კოსტავას 10 (WhatsApp: +995 598 23 33 32). წარმატებულ დღეს გისურვებთ! ✨"
        }

        // =========================================================================
        // 4. ლოკაცია / მისამართი / სამუშაო საათები / კონტაქტი
        // NOTE: Never trigger on generic "ტელეფონ" (which matches "ტელეფონი არ იტენება"!)
        // =========================================================================
        if (!isHardwareSymptom && (q.contains("სად ხართ") || q.contains("მისამართ") || q.contains("რომელ საათ") || q.contains("სად მდებარეობს") ||
                    q.contains("ლოკაცია") || q.contains("ახალციხე") || q.contains("ტელეფონის ნომერ") || q.contains("საკონტაქტო") || q.contains("კონტაქტ") || q.contains("ნომერი") ||
                    q.contains("whatsapp") || q.contains("ვაცაპ") || q.contains("telegram") || q.contains("ტელეგრამ"))
        ) {
            return "📍 **BEKASERVICE-ის საკონტაქტო მონაცემები:**\n\n" +
                    "• **მისამართი:** ქ. ახალციხე, მერაბ კოსტავას ქუჩა N10\n" +
                    "• ⏰ **სამუშაო საათები:** ყოველდღე 10:00 - 20:00 (შაბათ-კვირის ჩათვლით)\n" +
                    "• 📞 **ტელეფონი / WhatsApp:** +995 598 23 33 32\n" +
                    "• ✈️ **Telegram:** t.me/geounlock\n" +
                    "• 💻 **ციფრული სერვისების ჩართვა:** დისტანციურად მთელი საქართველოს მასშტაბით 15-30 წუთში!\n" +
                    "• 🛡️ **გარანტია:** ყველა შესრულებულ სამუშაოსა და პროგრამულ პაკეტზე მოქმედებს BEKASERVICE-ის გარანტია."
        }

        // =========================================================================
        // 5. რა სერვისები გაქვთ / პროგრამული სერვისების სია
        // =========================================================================
        if (q.contains("რა სერვის") || q.contains("რას არემონტებთ") || q.contains("რა შეგიძლიათ") || q.contains("რა მომსახურება") || q.contains("პროგრამული სერვის")) {
            return "🛠️ **BEKASERVICE-ის სრული კატალოგი მოიცავს:**\n\n" +
                    "1. 🤖 **ხელოვნური ინტელექტის სერვისები:**\n" +
                    "   • ChatGPT Plus & ChatGPT Pro\n" +
                    "   • Google AI Pro (18-თვიანი შეთავაზება + 2TB) & Google AI Ultra\n" +
                    "   • Claude Pro (Private & Shared), Grok SuperGrok & Heavy\n" +
                    "   • Perplexity Pro, Cursor AI Pro, Lovable Pro\n\n" +
                    "2. ☁️ **Google-ის ანგარიშები და მეხსიერება:**\n" +
                    "   • Gmail Phone Verified Account, Google Storage 2TB / 5TB, Google AI Pro + Storage Bundle\n\n" +
                    "3. 🎨 **ვიდეო, დიზაინი და მულტიმედია:**\n" +
                    "   • CapCut Pro, Adobe Creative Cloud, Canva Pro, YouTube Premium, Spotify Premium\n\n" +
                    "4. 🪟 **პროგრამული ლიცენზიები:**\n" +
                    "   • Windows 11 Pro Retail Key, Office LTSC 2024, Microsoft 365 Personal, Office 2019\n\n" +
                    "5. 📱 **სმარტფონების აპარატურული და პროგრამული რემონტი:**\n" +
                    "   • ეკრანები, ელემენტები, პორტები, კამერები, FRP, iCloud, SIM Unlock, Bootloop.\n\n" +
                    "დაწერეთ ნებისმიერი პროდუქტის სახელი ან ტელეფონის სიმპტომი!"
        }

        // =========================================================================
        // 6. ფასები / რა ღირს
        // =========================================================================
        if ((q.contains("რა ღირს") || q.contains("ფასი") || q.contains("ღირებულება") || q.contains("რა ჯდება")) &&
            !isHardwareSymptom && !q.contains("ეკრან") && !q.contains("ელემენტ") && !q.contains("პორტ") && !q.contains("frp") && !q.contains("icloud")
        ) {
            return "💰 **BEKASERVICE-ის ფასების მიმოხილვა:**\n\n" +
                    "• 💻 **ციფრული პროგრამული სერვისები:** მოქმედებს მოქნილი ტარიფები („ფასი შეთანხმებით“ — დამოკიდებულია არჩეულ ვადასა და Private/Shared ტიპზე).\n" +
                    "• 🔋 **ელემენტის შეცვლა:** 65 ₾ - 110 ₾ (ორიგინალი, 6 თვე გარანტია)\n" +
                    "• 📱 **ეკრანის შეცვლა:** 140 ₾ - 390 ₾ (OLED / AMOLED)\n" +
                    "• 🔌 **დამტენის პორტი:** 45 ₾ - 75 ₾\n" +
                    "• 🔓 **Google FRP / iCloud / SIM Lock:** 40 ₾ - 120 ₾\n\n" +
                    "დააკონკრეტეთ რომელი სერვისი ან მოდელი გაინტერესებთ!"
        }

        // Use combinedHistoryText when current message is a follow-up answer in a multi-turn conversation
        val diagQuery = if (!isHardwareSymptom && priorUserMessages.isNotEmpty()) combinedHistoryText else q

        // =========================================================================
        // 7. მტკიცებულებაზე დაფუძნებული დიაგნოსტიკა სიმპტომების მიხედვით
        // =========================================================================
        return when {
            // 7.0 კომპლექსური სიმპტომი: დავარდნა + შავი ეკრანი + ვიბრაცია/ხმა/კომპიუტერი მუშაობს
            ((diagQuery.contains("ვარდნ") || diagQuery.contains("დამივარდა") || diagQuery.contains("დავარდა") || diagQuery.contains("დარტყმ")) &&
             (diagQuery.contains("შავ") || diagQuery.contains("არ ანათებს") || diagQuery.contains("ჩაქრა") || diagQuery.contains("ჩაბნელ") || diagQuery.contains("ეკრან") || diagQuery.contains("არ ირთვება")) &&
             (diagQuery.contains("ვიბრაცი") || diagQuery.contains("ვიბრირებს") || diagQuery.contains("ხმა") || diagQuery.contains("კომპიუტერ") || diagQuery.contains("ზარი"))) -> {
                "📱 **დიაგნოსტიკური შეფასება: ტელეფონი ჩართულია, სავარაუდოა დისპლეის (OLED/განათების) ან შლეიფის დაზიანება**\n\n" +
                devicePrefix +
                "• **დადასტურებული სიმპტომი:** ტელეფონი გამოსცემს ხმას/ვიბრაციას ან რეაგირებს ზარზე, რაც ნიშნავს, რომ დედაპლატა და კვების სისტემა მუშაობს, ხოლო პირადი მონაცემები უსაფრთხოა.\n" +
                "• **შესაძლო მიზეზები:**\n" +
                "  1. დარტყმის შედეგად OLED/AMOLED მატრიცის ან განათების წრედის (Backlight) დაზიანება;\n" +
                "  2. ეკრანის შლეიფის (FPC კონექტორის) ამოვარდნა ან კონტაქტის დარღვევა.\n" +
                "• **უსაფრთხო შემოწმების ნაბიჯები:**\n" +
                "  1. სცადეთ იძულებითი გადატვირთვა (Force Restart);\n" +
                "  2. დააკვირდით კაშკაშა შუქის ქვეშ, ჩანს თუ არა ეკრანზე მკრთალი გამოსახულება.\n" +
                "• **როდის მიმართოთ სერვისს:** თუ Force Restart-ის შემდეგ ეკრანი ისევ ბნელია, საჭიროა სერვისცენტრში სატესტო ეკრანით და კონექტორის მიკროსკოპული შემოწმებით ზუსტი დიაგნოზის დადასტურება."
            }

            // 7.0.1 კომპლექსური სიმპტომი: წყალი/სითხე + დამტენზე შეერთება
            ((diagQuery.contains("წყალ") || diagQuery.contains("სითხ") || diagQuery.contains("დასველ") || diagQuery.contains("ნესტ")) &&
             (diagQuery.contains("დამტენ") || diagQuery.contains("შევაერთე") || diagQuery.contains("დავტენე") || diagQuery.contains("იტენება"))) -> {
                "💧⚠️ **კრიტიკული გაფრთხილება: სითხის მოხვედრა და დამტენზე შეერთება!**\n\n" +
                devicePrefix +
                "• **რისკის შეფასება:** სითხის მოხვედრის შემდეგ მოწყობილობის დამტენზე შეერთება იწვევს სველ კონტაქტებზე ელექტროლიზს და პორტის ან კვების კონტროლერის მოკლე ჩართვის რისკს.\n" +
                "• **უსაფრთხო მოქმედება ახლავე:** დაუყოვნებლივ გამოაერთეთ დამტენიდან და გამორთეთ მოწყობილობა. არ სცადოთ მისი ფენით გაშრობა ან ხელახლა დატენვა.\n" +
                "• **სერვისცენტრის დიაგნოსტიკა:** აუცილებელია ტელეფონის გახსნა, ელემენტის გათიშვა და პლატის ულტრაბგერითი ქიმიური წმენდა კოროზიის შესაჩერებლად."
            }

            // 7.1 ეკრანი არ ირთვება / შავი ეკრანი / ზოლები / ბზარი
            (diagQuery.contains("ეკრან") && (diagQuery.contains("არ ირთვება") || diagQuery.contains("შავ") || diagQuery.contains("ჩაქრა") || diagQuery.contains("გაშავდა"))) ||
            diagQuery == "ეკრანი არ ირთვება" -> {
                "📱 **დიაგნოსტიკური ანალიზი: ეკრანი არ ირთვება (შავი ეკრანის სიმპტომი)**\n\n" +
                devicePrefix +
                "• **სიმპტომის გამიჯვნა:** როდესაც ეკრანი არ ირთვება, პირველ რიგში უნდა გაირკვეს — ტელეფონი საერთოდ გათიშულია (No Power), თუ ჩართულია და მხოლოდ გამოსახულება არ გამოაქვს (No Display / Black Screen).\n" +
                "• **შესაძლო მიზეზები (საჭიროებს გადამოწმებას):**\n" +
                "  1. **ეკრანის მოდულის, OLED მატრიცის ან განათების (Backlight) ხარვეზი** — თუ ტელეფონი რეაგირებს ზარზე, ვიბრაციაზე ან დამტენის შეერთების ხმაზე;\n" +
                "  2. **დისპლეის შლეიფის (FPC კონექტორის) კონტაქტის დარღვევა** — განსაკუთრებით დავარდნის ან დარტყმის შემდეგ;\n" +
                "  3. **ღრმა განმუხტვა ან სისტემური გაჭედვა (System Hang)** — თუ მოწყობილობა საერთოდ არ რეაგირებს;\n" +
                "  4. **კვების წრედის (PMIC) ან ელემენტის ხარვეზი** — განიხილება მხოლოდ მაშინ, თუ დატენვისა და გადატვირთვის შემდეგაც სრული უმოქმედობაა.\n" +
                "• **უსაფრთხო პირველადი შემოწმება (მარტივიდან რთულისკენ):**\n" +
                "  1. შეაერთეთ ტელეფონი заведомо მუშა კაბელსა და ადაპტერზე 15–20 წუთით;\n" +
                "  2. შეამოწმეთ, ისმის თუ არა დამტენის შეერთების ხმა, ვიბრაცია ან შემოდის თუ არა ზარი;\n" +
                "  3. სცადეთ იძულებითი გადატვირთვა (Force Restart: ხმის ჩაწევისა და ჩართვის ღილაკებზე ხანგრძლივი დაჭერით 10–15 წამი).\n" +
                "• **დამაზუსტებელი კითხვა:** დავარდნის ან სითხის მოხვედრის შემდეგ დაეწყო ეს პრობლემა, და გამოსცემს თუ არა ტელეფონი ხმას/ვიბრაციას?"
            }

            // 7.2 ეკრანის სხვა ხარვეზები (ზოლები, ბზარი, სენსორი, ციმციმი)
            diagQuery.contains("ეკრან") || diagQuery.contains("ზოლ") || diagQuery.contains("ბზარ") || diagQuery.contains("ციმციმ") || diagQuery.contains("display") || diagQuery.contains("screen") -> {
                "📱 **დიაგნოსტიკური ანალიზი: ეკრანის გამოსახულების ან სენსორის ხარვეზი**\n\n" +
                devicePrefix +
                "• **შესაძლო მიზეზები:**\n" +
                "  1. OLED/AMOLED პიქსელების შრის ან შიდა სუბსტრატის მექანიკური დაზიანება (მწვანე/თეთრი ზოლები, მელნისფერი ლაქები);\n" +
                "  2. დისპლეის შლეიფის ან კონექტორის კონტაქტის დაზიანება;\n" +
                "  3. სენსორული შრის (Digitizer) ან დროებითი პროგრამული ხარვეზი.\n" +
                "• **უსაფრთხო შემოწმება:** გადატვირთეთ მოწყობილობა, მოხსენით დაზიანებული დამცავი ბრონი და შეამოწმეთ, ჩანს თუ არა ხარვეზი ჩართვის ლოგოზეც (Boot screen).\n" +
                "• **დამაზუსტებელი კითხვა:** აღენიშნება თუ არა ეკრანს გარეგანი ბზარი და მუშაობს თუ არა შეხება (Touch) მთელ ფართობზე?"
            }

            // 7.3 კამერა არ ფოკუსირდება / ბუნდოვანია / კანკალებს
            diagQuery.contains("კამერ") || diagQuery.contains("ფოკუს") || diagQuery.contains("კანკალ") || diagQuery.contains("ბუნდოვან") -> {
                "📷 **დიაგნოსტიკური ანალიზი: კამერის ფოკუსირების / გამოსახულების ხარვეზი**\n\n" +
                devicePrefix +
                "• **სიმპტომის შეფასება:** კამერის მიერ ფოკუსის ვერდაჭერა ან ბუნდოვანი გამოსახულება უკავშირდება ოპტიკურ ან ავტოფოკუსის მექანიზმს (და არა ელემენტს ან კვების სისტემას).\n" +
                "• **შესაძლო მიზეზები (მარტივიდან რთულისკენ):**\n" +
                "  1. **ლინზის გარეგანი დაბინძურება, ნაკაწრი ან ქეისის/დამცავი შუშის ხელშეშლა** — ხელს უშლის ლაზერულ/ფაზურ ავტოფოკუსს;\n" +
                "  2. **პროგრამული/აპლიკაციის ხარვეზი** — კამერის აპის დროებითი შეცდომა;\n" +
                "  3. **ავტოფოკუსის (AF) ან ოპტიკური სტაბილიზაციის (OIS) მექანიზმის დაზიანება** — ხშირია დავარდნის ან ძლიერი ვიბრაციის შემდეგ.\n" +
                "• **უსაფრთხო პირველადი შემოწმება:**\n" +
                "  1. გაწმინდეთ კამერის შუშა რბილი მიკროფიბრით და დროებით მოხსენით ქეისი/ლინზის დამცავი;\n" +
                "  2. შეამოწმეთ ფოკუსირება სხვადასხვა რეჟიმში: **0.5x, 1x და 2x/3x** გადართვისას — თუ 0.5x სუფთაა და მხოლოდ 1x ბუნდოვანია, ხარვეზი კონკრეტულად მთავარ (1x) კამერის მოდულშია;\n" +
                "  3. გადატვირთეთ ტელეფონი და შეამოწმეთ კამერა სხვა აპლიკაციაშიც.\n" +
                "• **დამაზუსტებელი კითხვა:** დავარდნის შემდეგ დაეწყო ფოკუსის პრობლემა და ისმის თუ არა კამერის ჩართვისას ჩუმი წკაპუნის ან ზუზუნის ხმა?"
            }

            // 7.4 ტელეფონი არ იტენება / დამტენის პორტი
            diagQuery.contains("არ იტენება") || diagQuery.contains("პორტ") || diagQuery.contains("დამტენ") || diagQuery.contains("charger") || diagQuery.contains("type-c") -> {
                "🔌 **დიაგნოსტიკური ანალიზი: ტელეფონი არ იტენება / დამუხტვის ხარვეზი**\n\n" +
                devicePrefix +
                "• **სიმპტომის შეფასება:** დამუხტვის შეწყვეტა შეიძლება გამოწვეული იყოს როგორც გარე აქსესუარით (კაბელი/ადაპტერი), ისე ტელეფონის პორტით ან კვების კვანძით. წინასწარი შემოწმების გარეშე ელემენტის ან პლატის დაზიანების დაბრალება არასწორია.\n" +
                "• **შესაძლო მიზეზები (საჭიროებს გადამოწმებას):**\n" +
                "  1. **კაბელის ან დამტენი ადაპტერის გაუმართაობა** (ყველაზე ხშირი და მარტივად გამოსარიცხი მიზეზი);\n" +
                "  2. **დამტენის პორტში (Type-C / Lightning) დაგროვილი მტვერი, ნადები ან კონტაქტების ცვეთა** — კაბელი ბოლომდე არ ჯდება ან ირყევა;\n" +
                "  3. **პორტში სისველის/ნესტის მოხვედრა ან ქვედა შლეიფის დაზიანება**;\n" +
                "  4. **ელემენტის ღრმა განმუხტვა ან დამუხტვის კონტროლერის (Charging IC / PMIC) ხარვეზი** (შესაძლო მიზეზი, თუ კაბელი და პორტი გამართულია).\n" +
                "• **უსაფრთხო პირველადი შემოწმება (მარტივიდან რთულისკენ):**\n" +
                "  1. სცადეთ სხვა, заведомо მუშა კაბელი, სხვა ადაპტერი და სხვა დენის წყარო (შტეფსელი);\n" +
                "  2. ფანრით ჩახედეთ დამტენის პორტში — ხომ არ ჩანს მტვერი ან უცხო ნაწილაკი (არ გამოიყენოთ ლითონის ნემსი პორტის გასაწმენდად!);\n" +
                "  3. დააკვირდით, ეწერება თუ არა დამუხტვის ნიშანი კაბელის განძრევისას ან ხომ არ ცხელდება პორტის მიდამო.\n" +
                "• **დამაზუსტებელი კითხვა:** სხვა კაბელზე/ადაპტერზეც იგივეა და ჩნდება თუ არა ეკრანზე დატენვის ინდიკატორი?"
            }

            // 7.5 ელემენტი / ბატარეა (მხოლოდ მაშინ, როცა მომხმარებელი აღწერს მალე დაჯდომას, გათიშვას ან გაბერვას)
            diagQuery.contains("ელემენტ") || diagQuery.contains("ბატარე") || diagQuery.contains("battery") || diagQuery.contains("ჯდება") || diagQuery.contains("ითიშება") || diagQuery.contains("გაბერ") -> {
                "🔋 **დიაგნოსტიკური ანალიზი: ელემენტის სწრაფი განმუხტვა ან არასტაბილური მუშაობა**\n\n" +
                devicePrefix +
                "• **შესაძლო მიზეზები:**\n" +
                "  1. ელემენტის ქიმიური ცვეთა (Battery Health < 80%, გაზრდილი შიდა წინააღმდეგობა);\n" +
                "  2. ფონური აპლიკაციების ან სისტემური პროცესების მიერ მომატებული ენერგომოხმარება;\n" +
                "  3. დედაპლატაზე მომატებული დენის მოხმარება (თუ ტელეფონი უმოქმედო რეჟიმშიც ცხელდება).\n" +
                "• **უსაფრთხო შემოწმება:** შეამოწმეთ პარამეტრებში Battery Health და აპლიკაციების ენერგომოხმარების სტატისტიკა. თუ ეკრანი ამოწეულია (ელემენტი გაბერილია), არ დატენოთ ტელეფონი!\n" +
                "• **დამაზუსტებელი კითხვა:** რამდენ პროცენტზეა Battery Health და ითიშება თუ არა ტელეფონი მოულოდნელად დაბალ პროცენტზე?"
            }

            // 7.6 Bootloop / ლოგოზე გაჭედვა / არ ირთვება ბოლომდე
            diagQuery.contains("bootloop") || diagQuery.contains("აგური") || diagQuery.contains("ლოგო") || diagQuery.contains("არ ირთვება") || diagQuery.contains("ფლეშ") -> {
                "⚙️ **დიაგნოსტიკური ანალიზი: ჩატვირთვის შეფერხება (Bootloop / არ ირთვება)**\n\n" +
                devicePrefix +
                "• **შესაძლო მიზეზები:**\n" +
                "  1. ოპერაციული სისტემის (Firmware) შეცდომა ან მეხსიერების გადავსება;\n" +
                "  2. ჩართვის ღილაკის ან პერიფერიული შლეიფის მოკლე ჩართვა;\n" +
                "  3. ელემენტის ძაბვის ვარდნა ჩატვირთვის პიკურ დატვირთვაზე.\n" +
                "• **უსაფრთხო შემოწმება:** შეაერთეთ დამტენზე 15 წუთით და სცადეთ Force Restart. არ გააკეთოთ Factory Reset, თუ ტელეფონში მნიშვნელოვანი ფოტოები და მონაცემები გაქვთ!\n" +
                "• **დამაზუსტებელი კითხვა:** ჩერდება თუ არა ტელეფონი ჩართვის ლოგოზე, თუ საერთოდ არ რეაგირებს ღილაკზე?"
            }

            // 7.7 წყალი / სითხე
            diagQuery.contains("წყალ") || diagQuery.contains("სისველ") || diagQuery.contains("ჩავარდა") -> {
                "💧 **დიაგნოსტიკური ანალიზი: სითხის მოხვედრა და კოროზიის რისკი**\n\n" +
                devicePrefix +
                "• **უსაფრთხო მოქმედება ახლავე:** გამორთეთ მოწყობილობა და არ შეაერთოთ დამტენზე! დენის მიწოდება სველ კონტაქტებზე აჩქარებს ელექტროქიმიურ კოროზიას.\n" +
                "• **რატომ არის საჭირო სერვისი:** გარეგანი გაშრობა პლატის ეკრანების ქვეშ დარჩენილ სითხეს ვერ აშორებს; საჭიროა დაშლა და ულტრაბგერითი წმენდა."
            }

            // 7.8 დინამიკი / მიკროფონი / ხმა
            diagQuery.contains("დინამიკ") || diagQuery.contains("მიკროფონ") || diagQuery.contains("ხმა") || diagQuery.contains("აუდიო") || diagQuery.contains("speaker") -> {
                "🔊 **დიაგნოსტიკური ანალიზი: ხმის / მიკროფონის ხარვეზი**\n\n" +
                devicePrefix +
                "• **შესაძლო მიზეზები:** დინამიკის/მიკროფონის ბადის დაბინძურება, Bluetooth აუდიოზე გადართვა, ან აუდიო მოდულის/კოდეკის ხარვეზი.\n" +
                "• **უსაფრთხო შემოწმება:** ჩაწერეთ ხმოვანი შეტყობინება დიქტოფონში და შეამოწმეთ ხმა სპიკერზე (Speakerphone).\n" +
                "• **დამაზუსტებელი კითხვა:** ხმა საერთოდ არ ისმის, თუ ისმის დაბალზე/შხრიალით?"
            }

            // FRP / Google Account
            diagQuery.contains("frp") || diagQuery.contains("google account") || diagQuery.contains("ფრპ") -> {
                "🔓 **Google FRP (Factory Reset Protection) აქტივაციის ბლოკი**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** მოწყობილობის ქარხნული გადატვირთვის შემდეგ ჩართულია Google-ის უსაფრთხოების დაცვა, რომელიც ითხოვს წინა სინქრონიზებულ ანგარიშს.\n" +
                "• **პირველადი შემოწმება:** დაუკავშირეთ ტელეფონი სტაბილურ Wi-Fi ქსელს და სცადეთ Google ანგარიშის პაროლის აღდგენა ოფიციალური გვერდიდან.\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** სერვისცენტრში შესაძლებელია FRP ბლოკის პროგრამული მოხსნა მოდელისა და უსაფრთხოების პატჩის მიხედვით."
            }

            // iCloud / Apple ID
            diagQuery.contains("icloud") || diagQuery.contains("აიქლაუდ") || diagQuery.contains("apple id") || diagQuery.contains("აქტივაცია") -> {
                "🍏 **Apple iCloud / Activation Lock დიაგნოსტიკა**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** მოწყობილობაზე გააქტიურებულია Find My (FMI) უსაფრთხოების ბლოკი.\n" +
                "• **დამაზუსტებელი კითხვა:** ტელეფონი გაჩერებულია Hello ეკრანზე თუ Passcode რეჟიმში და რომელი მოდელია ზუსტად?"
            }

            // ქსელის ბლოკი (SIM Lock / Carrier Unlock)
            diagQuery.contains("ქსელ") || diagQuery.contains("sim") || diagQuery.contains("სიმ") || diagQuery.contains("unlock") || diagQuery.contains("ოპერატორ") -> {
                "📶 **ქსელის / SIM ბარათის კავშირის დიაგნოსტიკა**\n\n" +
                devicePrefix +
                "• **შესაძლო მიზეზები:** SIM ბარათის კონტაქტის ხარვეზი, უცხოურ ოპერატორზე დაბლოკვა (Carrier SIM Lock), ან ქსელის მოდემის/ანტენის ხარვეზი.\n" +
                "• **უსაფრთხო შემოწმება:** სცადეთ სხვა მოქმედი SIM ბარათი და გადაამოწმეთ პარამეტრებში Carrier Lock სტატუსი.\n" +
                "• **დამაზუსტებელი კითხვა:** ეკრანზე ეწერება 'SIM Not Supported' / 'Invalid SIM', თუ 'No Service'?"
            }

            else -> {
                val matched = findRelevantProducts(query, catalog).take(3)
                if (matched.isNotEmpty()) {
                    "🔎 **თქვენი მოთხოვნით BEKASERVICE-ის კატალოგში მოიძებნა:**\n\n" +
                            matched.joinToString("\n\n") { p ->
                                if (SoftwareCatalogData.isDigitalSoftwareService(p)) {
                                    formatSoftwareProductSummary(p)
                                } else {
                                    "🔹 **${p.title}** (${if (p.inStock) "ხელმისაწვდომია ✅" else "მიუწვდომელია"})\n" +
                                    "• ფასი: **${p.price}** | კატეგორია: ${p.category}\n" +
                                    "• თავსებადობა: ${p.compatibleModels} | გარანტია: ${p.warranty}"
                                }
                            } +
                            "\n\nქვემოთ მოცემულ ბარათებზე დაჭერით შეგიძლიათ გახსნათ დეტალური გვერდი ან შეუკვეთოთ!"
                } else {
                    val modelHint = if (detectedDevice.isNotBlank()) "აღნიშნულ მოდელზე (**$detectedDevice**) " else ""
                    "🔍 **ტექნიკური დიაგნოსტიკა — დამაზუსტებელი კითხვა**\n\n" +
                    devicePrefix +
                    "${modelHint}ზუსტი დიაგნოსტიკური ანალიზისთვის გთხოვთ მომწეროთ:\n" +
                    "1. **ტელეფონის ზუსტი მოდელი** (მაგ: iPhone 13, Samsung S23, Redmi Note 12);\n" +
                    "2. **კონკრეტული სიმპტომი** (მაგ: ეკრანი არ ირთვება, ტელეფონი არ იტენება, კამერა არ ფოკუსირდება);\n" +
                    "3. **რის შემდეგ დაეწყო პრობლემა** (დავარდნა, სითხის მოხვედრა, განახლება თუ თავისით)."
                }
            }
        }
    }
}

// ==============================================================================
// 7. AI ასისტენტის ინტერფეისი (Gemini Multi-Turn Diagnostics & Software Chat)
// ==============================================================================
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AiAssistantScreen(
    products: List<ProductItem> = emptyList(),
    onProductClick: (ProductItem) -> Unit = {},
    onAddToCart: (ProductItem) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    val context = LocalContext.current
    val isDark = AppPreferencesManager.isDarkMode
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    var textInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var activeChatJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var activeRequestId by remember { mutableStateOf(0L) }

    val chatList = remember {
        mutableStateListOf(
            ChatMessage(
                "ai",
                "გამარჯობა! მე ვარ BEKASERVICE-ის ოფიციალური AI ასისტენტი & ტექნიკური კონსულტანტი.\n\n" +
                "შემიძლია დაგეხმაროთ:\n" +
                "• 📱 **სმარტფონების დიაგნოსტიკასა და ნაწილებში:** ეკრანები, ელემენტები, პორტები, კამერა, FRP, iCloud, SIM Lock\n" +
                "• 💻 **24 პროგრამული სერვისის შერჩევაში:** Canva Pro, CapCut Pro, Adobe CC, Windows 11 & Office.",
                recommendedProducts = listOf(103, 101, 118).mapNotNull { id -> products.find { it.id == id } }
            )
        )
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // IME (Keyboard) & Bottom Navigation unified inset calculation:
    // Prevents the keyboard from ever covering the composer while avoiding double bottom padding.
    val topPadding = contentPadding.calculateTopPadding()
    val bottomNavPadding = contentPadding.calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = imeBottom > bottomNavPadding
    val effectiveBottomInset = maxOf(imeBottom, bottomNavPadding)

    // Auto-scroll to latest message when messages change, loading state updates, or keyboard opens
    LaunchedEffect(chatList.size, isLoading, isKeyboardOpen) {
        if (chatList.isNotEmpty()) {
            val lastIdx = (chatList.size - 1 + (if (isLoading) 1 else 0)).coerceAtLeast(0)
            try {
                listState.animateScrollToItem(lastIdx)
            } catch (_: Exception) {
            }
        }
    }

    val quickQuestions = listOf(
        "📱 ეკრანი არ ირთვება",
        "🔌 ტელეფონი არ იტენება",
        "📷 კამერა არ ფოკუსირდება",
        "✨ Google AI Pro — 18 თვე",
        "🎨 Canva Pro & CapCut Pro",
        "🪟 Windows 11 & Office ლიცენზიები",
        "🔓 Google FRP ბლოკის მოხსნა",
        "🔋 ელემენტი 80%-ზე ნაკლებია"
    )

    fun sendMessage(prompt: String) {
        val userText = prompt.trim()
        if (userText.isBlank() || isLoading) return
        val historySnapshot = chatList.toList()
        chatList.add(ChatMessage("user", userText))
        textInput = ""
        isLoading = true

        val requestId = ++activeRequestId
        activeChatJob?.cancel()
        activeChatJob = coroutineScope.launch {
            try {
                val reply = GeminiDiagnosticService.diagnose(historySnapshot, userText, products)
                if (requestId != activeRequestId) return@launch
                val matchedProducts = GeminiDiagnosticService.findRelevantProducts(userText, products)
                chatList.add(ChatMessage("ai", reply, matchedProducts))
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (requestId == activeRequestId) {
                    chatList.add(
                        ChatMessage(
                            "ai",
                            "⚠️ მოთხოვნის დამუშავებისას დაფიქსირდა დროებითი შეფერხება. გთხოვთ სცადოთ თავიდან.",
                            emptyList()
                        )
                    )
                }
            } finally {
                if (requestId == activeRequestId) {
                    isLoading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = topPadding,
                bottom = effectiveBottomInset,
                start = 14.dp,
                end = 14.dp
            )
            .testTag("ai_assistant_root"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: AI ასისტენტის სტატუსი და გასუფთავება
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0x33FF6A00),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🤖", fontSize = 18.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "BEKASERVICE AI ასისტენტი",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E2640),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.6f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("✨", fontSize = 9.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "AI",
                                        color = Color(0xFF93C5FD),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        Text(
                            "კატალოგი & დიაგნოსტიკა • 24/7",
                            color = Color(0xFF4ADE80),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = {
                        activeRequestId++
                        activeChatJob?.cancel()
                        activeChatJob = null
                        isLoading = false
                        chatList.clear()
                        chatList.add(
                            ChatMessage(
                                "ai",
                                "ჩათი გასუფთავდა. მკითხეთ ტელეფონის დიაგნოსტიკაზე ან ნებისმიერ პროგრამულ სერვისზე!",
                                recommendedProducts = listOf(103, 101, 118).mapNotNull { id -> products.find { it.id == id } }
                            )
                        )
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("ai_assistant_clear_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Clear Chat", tint = Color(0xFF6E7582), modifier = Modifier.size(18.dp))
                }
            }

            // სწრაფი შეკითხვების ჩიპები (Glassmorphic Translucent Chips)
            val isDark = AppPreferencesManager.isDarkMode
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quickQuestions) { q ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF161922).copy(alpha = 0.75f) else Color(0xFFFFFFFF).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF282F3C).copy(alpha = 0.70f) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.clickable(
                            enabled = !isLoading,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { sendMessage(q) }
                    ) {
                        Text(
                            q,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // შეტყობინებების სია (Multi-turn Scrollable Thread)
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_assistant_chat_list")
            ) {
                items(chatList, key = { it.id }) { msg ->
                    val isAi = msg.sender == "ai"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isAi) Color(0xFF16191E) else Color(0xFFFF6A00),
                            border = if (isAi) {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6A00).copy(alpha = 0.35f))
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth(if (isAi) 0.95f else 0.85f)
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    msg.text,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // ინტერაქტიული პროდუქტების ბარათები პირდაპირ ჩათში ("დეტალურად" + "შეკვეთა")
                        if (isAi && msg.recommendedProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(0.95f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                msg.recommendedProducts.forEach { prod ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF141820),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28303E)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onProductClick(prod) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF0F1115)))
                                            ) {
                                                ProductSmartVisual(
                                                    product = prod,
                                                    modifier = Modifier.fillMaxSize(),
                                                    compact = true
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    prod.title,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (prod.shortDescription.isNotBlank()) {
                                                    Text(
                                                        prod.shortDescription,
                                                        color = Color(0xFF94A3B8),
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    "${prod.price} • ${if (prod.inStock) "ხელმისაწვდომია" else "მიუწვდომელია"}",
                                                    color = Color(0xFFFF7A00),
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF1E2532),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333D4F)),
                                                    modifier = Modifier.clickable { onProductClick(prod) }
                                                ) {
                                                    Text(
                                                        "დეტალურად",
                                                        color = Color.White,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (prod.inStock) Color(0xFFFF7A00) else Color(0xFF262A34),
                                                    modifier = Modifier.clickable(enabled = prod.inStock) { onAddToCart(prod) }
                                                ) {
                                                    Text(
                                                        "შეკვეთა",
                                                        color = if (prod.inStock) Color.White else Color(0xFF6E7582),
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // AI პასუხის ქვემოთ სწრაფი მოქმედებების ღილაკები
                        if (isAi && (msg.text.contains("ბექასერვის") || msg.text.contains("BEKASERVICE"))) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F2618),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B4E2E)),
                                    modifier = Modifier.clickable {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://wa.me/995598233332?text=${Uri.encode("გამარჯობა BEKASERVICE! მსურს კონსულტაცია / შეკვეთა.")}")
                                        )
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("💬", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WhatsApp მიწერა", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1F1A14),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38291A)),
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+995598233332"))
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("დარეკვა", color = Color(0xFFFF7A00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF16191E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6A00).copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .testTag("ai_assistant_loading_indicator")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFF7A00),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "⚡ AI აანალიზებს მოთხოვნას...",
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // შეტყობინების შეყვანის ველი: Floating Glassmorphism Pill Bar & Smooth Animated Send
        val isInputActive = textInput.isNotBlank()
        val inputBorderColor by animateColorAsState(
            targetValue = if (isInputActive) Color(0xFFFF7A00) else if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFCBD5E1),
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            label = "chatInputBorder"
        )
        val sendScale by animateFloatAsState(
            targetValue = if (isInputActive && !isLoading) 1.05f else 0.95f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "chatSendScale"
        )
        val sendColor by animateColorAsState(
            targetValue = if (isInputActive && !isLoading) Color(0xFFFF7A00) else if (isDark) Color(0xFF1E242E).copy(alpha = 0.75f) else Color(0xFFE2E8F0),
            animationSpec = tween(180),
            label = "chatSendColor"
        )
        val sendIconTint by animateColorAsState(
            targetValue = if (isInputActive && !isLoading) Color.White else if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
            animationSpec = tween(180),
            label = "chatSendIconTint"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .bringIntoViewRequester(bringIntoViewRequester)
                .testTag("ai_assistant_composer_row"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp, max = 96.dp)
            ) {
                // Glassmorphic translucent backing layer with blur
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(22.dp))
                        .then(
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                Modifier.blur(14.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xFF161922).copy(alpha = 0.80f),
                                        Color(0xFF0F1218).copy(alpha = 0.90f)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFFFFFFF).copy(alpha = 0.88f),
                                        Color(0xFFF1F5F9).copy(alpha = 0.94f)
                                    )
                                }
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, inputBorderColor),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textInput.isEmpty()) {
                            Text(
                                text = "იკითხეთ სერვისზე ან პრობლემაზე...",
                                color = Color(0xFF7E8695),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = textInput,
                            onValueChange = {
                                textInput = it
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                color = AppThemeColorAdapter.inputTextColor(),
                                lineHeight = 18.sp
                            ),
                            maxLines = 4,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (!isLoading && textInput.isNotBlank()) {
                                        sendMessage(textInput)
                                    }
                                }
                            ),
                            cursorBrush = SolidColor(Color(0xFFFF7A00)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        coroutineScope.launch {
                                            bringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                }
                                .testTag("ai_assistant_input")
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = sendColor,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isInputActive && !isLoading) Color(0xFFFF9500).copy(alpha = 0.6f) else Color.Transparent
                ),
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                    .clickable(
                        enabled = !isLoading && isInputActive,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        sendMessage(textInput)
                    }
                    .testTag("ai_assistant_send_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = sendIconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

// ==============================================================================
// 7. კალათის გვერდი (Cart Screen - Tab 5)
// ==============================================================================
@Composable
fun CartScreen(
    cartList: MutableList<CartItem>,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
                start = 14.dp,
                end = 14.dp
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBack() }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFFF6A00), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("თქვენი კალათა", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (cartList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF6E7582), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("კალათა ცარიელია", color = Color(0xFFA0A5B0), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("დაამატეთ სასურველი ნაწილები ან პროგრამული სერვისები კატალოგიდან", color = Color(0xFF6E7582), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(cartList) { item ->
                        val isSoft = SoftwareCatalogData.isDigitalSoftwareService(item.product)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF16191E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF0F1115)))
                                ) {
                                    ProductSmartVisual(
                                        product = item.product,
                                        modifier = Modifier.fillMaxSize(),
                                        compact = true
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (isSoft) "პაკეტი: ${item.selectedQuality}" else "ხარისხი: ${item.selectedQuality}",
                                        color = Color(0xFFA0A5B0),
                                        fontSize = 11.sp
                                    )
                                    Text(item.product.price, color = Color(0xFFFF6A00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (item.quantity > 1) item.quantity -= 1 else cartList.remove(item)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("-", color = Color.White, fontSize = 18.sp)
                                    }
                                    Text("${item.quantity}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { item.quantity += 1 },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("+", color = Color.White, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (cartList.isNotEmpty()) {
            Button(
                onClick = onCheckout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("WhatsApp-ით შეკვეთის გაფორმება 💬", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==============================================================================
// 8. პროფილი & კონტაქტები (Profile Screen - Tab 4)
// ==============================================================================
@Composable
fun ProfileScreen(
    onOpenCart: () -> Unit,
    onOpenAdminPanel: () -> Unit = {},
    products: List<ProductItem> = emptyList(),
    onProductClick: (ProductItem) -> Unit = {},
    onToggleFavorite: (ProductItem) -> Unit = {},
    onAddToCart: (ProductItem) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    ProfileAndAuthScreen(
        onOpenCart = onOpenCart,
        onOpenAdminPanel = onOpenAdminPanel,
        products = products,
        onProductClick = onProductClick,
        onToggleFavorite = onToggleFavorite,
        onAddToCart = onAddToCart,
        contentPadding = contentPadding
    )
}


fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "$label დაკოპირდა!", Toast.LENGTH_SHORT).show()
}
