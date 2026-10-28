package com.example

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
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                splashScreenView.remove()
            }
        }
        super.onCreate(savedInstanceState)
        AppPreferencesManager.init(this)
        BekaNotificationManager.ensureNotificationChannels(this)
        BekaNotificationManager.purgeSyntheticNotificationsIfNeeded(this)
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
// ოფიციალური BEKASERVICE Seamless Icon-to-Splash Transition
// ==============================================================================
@Composable
fun VideoSplashScreen(
    isAppReady: Boolean = true,
    onFinish: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }
    var minTransitionDone by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.62f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "SplashIconExpandScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (startAnim) 0.16f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "SplashGlowAlpha"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(320L)
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
        delay(400L)
        hasCompletedInitialLaunch = true
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
        Image(
            painter = painterResource(id = R.drawable.bekaservice_splash_logo),
            contentDescription = "BEKASERVICE",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(168.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .testTag("splash_logo_image")
        )
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

    val cartList = remember { mutableStateListOf<CartItem>() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(Unit) {
        AppPreferencesManager.init(context)
        BekaNotificationManager.purgeSyntheticNotificationsIfNeeded(context)
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
        BekaNotificationManager.purgeSyntheticNotificationsIfNeeded(context, force = true)
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
            BekaNotificationManager.ensureNotificationChannels(context)
            val existing = AuthAndOrderManager.getCurrentUser(context)
            if (existing == null) {
                AuthAndOrderManager.attemptAutoSignIn(context)
            } else {
                AuthAndOrderManager.refreshCurrentUserFromCloud(context)
            }
            BekaNotificationManager.syncFcmTokenAndSubscribeTopic(context, firestoreRepo)
            firestoreRepo?.performSafeInitialMigrationIfNeeded(context)
        } catch (e: Exception) {
            android.util.Log.e("BekaService", "Auth/Migration init error", e)
        } finally {
            isEssentialInitReady = true
        }
    }

    LaunchedEffect(showSplash) {
        if (!showSplash && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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
                        val merged = AdminStorage.mergeCloudWithLocal(
                            context = context,
                            cloudProducts = syncSnapshot.products,
                            removedProductIds = syncSnapshot.removedProductIds,
                            presentCloudDocIds = syncSnapshot.presentDocIds
                        )
                        if (productsList.toList() != merged) {
                            productsList.clear()
                            productsList.addAll(merged)
                        }
                        if (selectedProductForDetail != null) {
                            selectedProductForDetail = merged.find { it.id == selectedProductForDetail?.id }
                        }
                        AdminStorage.saveProductsFromCloud(context, merged)
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

    // უკან ღილაკის მართვა ქვე-გვერდებიდან, Drawer-იდან და Splash-დან
    BackHandler(enabled = drawerState.isOpen || showSplash || isAdminOpen || showAdminPanelScreen || selectedProductForDetail != null || selectedTab != 0) {
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
        }
    }

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = {
            fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing))
                .togetherWith(fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)))
        },
        label = "SplashAppTransition"
    ) { isSplash ->
        if (isSplash) {
            VideoSplashScreen(
                isAppReady = isEssentialInitReady,
                onFinish = {
                    hasCompletedInitialLaunch = true
                    showSplash = false
                }
            )
        } else {
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
                            selectedTab = newTab
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
                                onOpenCatalogTab = { selectedTab = 1 }
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
                selectedTab = 0
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
                selectedTab = index
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
                                if (isDigitalSoftware) {
                                    SoftwareBrandVisualHeader(
                                        product = product,
                                        modifier = Modifier.fillMaxSize(),
                                        compact = false
                                    )
                                } else {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = product.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

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
                                            val extraInfo = if (isDigitalSoftware) {
                                                "\nპაკეტი: ${product.offerType}\nხანგრძლივობა: ${product.duration}"
                                            } else {
                                                " ($selectedQuality)"
                                            }
                                            val msg = "გამარჯობა BEKASERVICE! მსურს შეკვეთა:\n${product.title}$extraInfo\nფასი: $currentPrice"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/share/url?url=&text=${Uri.encode(msg)}"))
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
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
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

    // კატეგორიები
    val categories = remember {
        listOf(
            Pair("ყველა", "🔥"),
            Pair("პროგრამული სერვისები", "💻"),
            Pair("ეკრანები", "📱"),
            Pair("ელემენტები", "🔋"),
            Pair("კამერები", "📸"),
            Pair("დინამიკები", "🔊"),
            Pair("დამტენის პორტები", "🔌"),
            Pair("კორპუსები", "🛠️")
        )
    }

    // ოპტიმიზებული საძიებო ფილტრი (ყოველთვის იყენებს განახლებულ products სიას)
    val filteredProducts = optimizeSearchFilter(products, searchQuery, selectedCategory, "ყველა", "default", selectedSubcategory)
    val productRows = filteredProducts.chunked(2)
    val allSoftwareServices = products.filter {
        SoftwareCatalogData.isDigitalSoftwareService(it) || it.category == "პროგრამული სერვისები"
    }
    val featuredSoftwareProducts = run {
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
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
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
    val categories = listOf("ყველა", "პროგრამული სერვისები", "ეკრანები", "ელემენტები", "დამტენის პორტები", "კამერები", "კორპუსები", "დინამიკები")
    var selectedBrand by remember { mutableStateOf("ყველა") }
    var selectedSubcategory by remember { mutableStateOf("ყველა") }
    var sortBy by remember { mutableStateOf("default") }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != "პროგრამული სერვისები") {
            selectedSubcategory = "ყველა"
        }
    }

    val filteredProducts = optimizeSearchFilter(products, searchQuery, selectedCategory, selectedBrand, sortBy, selectedSubcategory)

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
                            LazyRow(
                                modifier = Modifier.weight(1f).padding(end = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(SoftwareCatalogData.softwareSubcategories) { subcat ->
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
                            LazyRow(
                                modifier = Modifier.weight(1f).padding(end = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val brands = listOf("ყველა", "Apple", "Samsung", "Xiaomi", "Google Pixel", "OnePlus", "Honor")
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
// 4. სწრაფი დიაგნოსტიკა (Diagnostics Screen - Tab 2)
// ==============================================================================
@Composable
fun DiagnosticsScreen(
    onProductClick: (ProductItem) -> Unit,
    products: List<ProductItem>,
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    val context = LocalContext.current
    var selectedBrand by remember { mutableStateOf("Apple") }
    var selectedModel by remember { mutableStateOf("iPhone 16 Pro Max") }
    var selectedIssue by remember { mutableStateOf("ეკრანი გატეხილია") }

    val brands = listOf("Apple", "Samsung", "Xiaomi", "Google Pixel")
    val models = when (selectedBrand) {
        "Apple" -> listOf("iPhone 16 Pro Max", "iPhone 16 Pro", "iPhone 15 Pro", "iPhone 14 Pro", "iPhone 13")
        "Samsung" -> listOf("Galaxy S24 Ultra", "Galaxy S23 Ultra", "Galaxy S22", "Galaxy A54")
        "Xiaomi" -> listOf("Xiaomi 14", "Xiaomi 13T", "Redmi Note 13 Pro")
        else -> listOf("Pixel 8 Pro", "Pixel 7 Pro", "Pixel 6")
    }

    val issues = listOf(
        Pair("ეკრანი გატეხილია", "📱"),
        Pair("FRP ბლოკი (Google Account)", "🔓"),
        Pair("iCloud ბლოკი / Apple ID", "🍏"),
        Pair("ქსელის ბლოკი (SIM Lock)", "📶"),
        Pair("Bootloop / არ ირთვება", "⚙️"),
        Pair("Auth Flashing (EDL 9008)", "🔐"),
        Pair("ელემენტი სწრაფად ჯდება", "🔋"),
        Pair("არ იტენება / პორტი", "🔌"),
        Pair("კამერა არ ფოკუსირებს", "📷"),
        Pair("წყლის მოხვედრა", "💧")
    )

    // გამოთვლილი სავარაუდო ფასი და დრო
    val estimateData = when (selectedIssue) {
        "ეკრანი გატეხილია" -> Triple("180 ₾ - 390 ₾", "25-35 წუთი", "ორიგინალი OLED ეკრანის მოდული")
        "FRP ბლოკი (Google Account)" -> Triple("40 ₾ - 50 ₾", "15-20 წუთი", "Google FRP Bypass & Knox გასუფთავება")
        "iCloud ბლოკი / Apple ID" -> Triple("90 ₾ - 120 ₾", "20-35 წუთი", "Apple Server / Checkm8 Bypass")
        "ქსელის ბლოკი (SIM Lock)" -> Triple("60 ₾ - 75 ₾", "12-24 საათი", "ოფიციალური Factory SIM Unlock")
        "Bootloop / არ ირთვება" -> Triple("35 ₾ - 55 ₾", "20-30 წუთი", "ოფიციალური Stock ROM-ის ჩაწერა")
        "Auth Flashing (EDL 9008)" -> Triple("50 ₾ - 60 ₾", "15-20 წუთი", "Xiaomi/Oppo/Realme სერვერული ავტორიზაცია")
        "ელემენტი სწრაფად ჯდება" -> Triple("85 ₾ - 120 ₾", "15-20 წუთი", "ახალი ორიგინალი ელემენტი 100%")
        "არ იტენება / პორტი" -> Triple("60 ₾ - 85 ₾", "20-30 წუთი", "Type-C დამტენის პორტის შლეიფი")
        "კამერა არ ფოკუსირებს" -> Triple("140 ₾ - 220 ₾", "25-35 წუთი", "მთავარი კამერის OIS მოდული")
        else -> Triple("50 ₾ - 150 ₾", "40-60 წუთი", "ულტრაბგერითი წმენდა და დიაგნოსტიკა")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("სწრაფი თვითდიაგნოსტიკა", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("აირჩიე მოდელი და პრობლემა ზუსტი ფასის გასაგებად", color = Color(0xFFA0A5B0), fontSize = 12.sp)
        }

        // ნაბიჯი 1: ბრენდი
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. აირჩიე ბრენდი", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        brands.forEach { brand ->
                            val isSelected = selectedBrand == brand
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF121418),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFFF6A00) else Color(0xFF222630)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedBrand = brand
                                        selectedModel = when (brand) {
                                            "Apple" -> "iPhone 16 Pro Max"
                                            "Samsung" -> "Galaxy S24 Ultra"
                                            "Xiaomi" -> "Xiaomi 14"
                                            else -> "Pixel 8 Pro"
                                        }
                                    }
                            ) {
                                Text(
                                    brand,
                                    color = if (isSelected) Color.White else Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ნაბიჯი 2: მოდელი
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. აირჩიე მოდელი", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(models) { model ->
                            val isSelected = selectedModel == model
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0x33FF6A00) else Color(0xFF121418),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFFF6A00) else Color(0xFF222630)),
                                modifier = Modifier.clickable { selectedModel = model }
                            ) {
                                Text(
                                    model,
                                    color = if (isSelected) Color(0xFFFF6A00) else Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ნაბიჯი 3: პრობლემა
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. რა პრობლემა აქვს ტელეფონს?", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        issues.forEach { (issue, icon) ->
                            val isSelected = selectedIssue == issue
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0x22FF6A00) else Color(0xFF121418),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFFF6A00) else Color(0xFF222630)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedIssue = issue }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(icon, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        issue,
                                        color = if (isSelected) Color(0xFFFF6A00) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFF6A00), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // დიაგნოსტიკის შედეგი
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF191D24),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6A00).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("დიაგნოსტიკის შედეგი", color = Color(0xFFFF7A00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0x334ADE80)) {
                            Text("გარანტია 6 თვე", color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Text("$selectedModel - $selectedIssue", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("სავარაუდო ფასი", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                            Text(estimateData.first, color = Color(0xFFFF7A00), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("სამუშაო დრო", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                            Text(estimateData.second, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("საჭირო ნაწილი: ${estimateData.third}", color = Color(0xFFA0A5B0), fontSize = 11.5.sp)

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val msg = "გამარჯობა ბექა! დიაგნოსტიკით გამოიკვეთა:\nმოდელი: $selectedModel\nპრობლემა: $selectedIssue\nმსურს სერვისზე ჩაწერა."
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/995598233332?text=${Uri.encode(msg)}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("ჩაეწერე სერვისზე WhatsApp-ით 💬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // 5. მიმაგრებული AI ასისტენტი (მაქსიმალური სიზუსტისთვის)
        item {
            AttachedAiDiagnosticsCard(
                brand = selectedBrand,
                model = selectedModel,
                selectedIssue = selectedIssue,
                onDirectOrderClick = { report ->
                    val msg = "გამარჯობა ბექა! AI დიაგნოსტიკის შედეგი:\nმოდელი: $selectedBrand $selectedModel\nპრობლემა: ${report.title}\nფასი: ${report.estimatedPrice}\nმსურს სერვისზე ჩაწერა."
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/995598233332?text=${Uri.encode(msg)}"))
                    context.startActivity(intent)
                }
            )
        }
    }
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
                if (isDigitalSoftware) {
                    SoftwareBrandVisualHeader(
                        product = product,
                        modifier = Modifier.fillMaxSize(),
                        compact = true
                    )
                } else {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

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
    private const val MODEL = "gemini-3.5-flash"
    private val CANDIDATE_MODELS = listOf("gemini-3.5-flash", "gemini-3.8-flash", "gemini-flash-latest")
    private const val BASE_SYSTEM_INSTRUCTION = """
შენ ხარ „ბექასერვისის“ (BEKASERVICE) ოფიციალური AI ტექნიკოს-ინჟინერი, დიაგნოსტიკოსი და ციფრული პროგრამული სერვისების კონსულტანტი.
ესაუბრე მომხმარებელს ცოცხლად, პროფესიონალურად, ზუსტად და ადამიანურად ქართულ ენაზე.

მნიშვნელოვანი წესები:
1. არ გასცე ერთფეროვანი ან რობოტული შაბლონები! უპასუხე კონკრეტულ კითხვას რეალური კატალოგის მონაცემებზე დაყრდნობით.
2. შენ გაქვს წვდომა BEKASERVICE-ის სრულ LIVE კატალოგზე (პროგრამული სერვისები, განბლოკვა, ტელეფონის ნაწილები და შეკეთება).
3. პროგრამულ სერვისებზე კითხვისას:
   - დაეყრდენი ქვემოთ მოცემულ LIVE კატალოგის მონაცემებს (ფასი, ხელმისაწვდომობა, ხანგრძლივობა, ანგარიშის ტიპი Private vs Shared, აქტივაციის პირობები).
   - მკაფიოდ განასხვავე ოფიციალური სერვისის ფუნქციები და BEKASERVICE-ის სპეციალური პრომო პირობები (მაგ: Google AI Pro — 18-თვიანი პრომო შეთავაზება 2TB მეხსიერებითა და Gemini 2.5 Pro წვდომით).
   - თუ სერვისის ფასია „ფასი შეთანხმებით“, განუმარტე, რომ ზუსტი ტარიფი დამოკიდებულია არჩეულ ვადასა და აქტივაციის ტიპზე და შეუძლია პირდაპირ შეუკვეთოს აპიდან.
4. აპარატურულ დიაგნოსტიკაზე კითხვისას:
   - ჩაატარე სიღრმისეული ტექნიკური ანალიზი (PMIC, Hydra/Tristar, True Tone EEPROM, BMS 0-ციკლი, OLED მატრიცა, UFS/eMMC, EDL 9008, Checkm8).
5. უმკაცრესი წესი: არასდროს ახსენო სხვა სახელოსნო ან მაღაზია. მიუთითე მხოლოდ BEKASERVICE-ზე (ქ. ახალციხე, კოსტავას 10, WhatsApp: +995 598 23 33 32).
6. კონკრეტული სერვისის ან პროდუქტის მოთხოვნისას (მაგალითად: „canva მინდა“, „canva pro“, „capcut“, „adobe“, „chatgpt“, ან ნებისმიერი სხვა კონკრეტული სერვისი თუ პროდუქტი):
   - უპასუხე კონკრეტულად და პირდაპირ მხოლოდ მოთხოვნილ სერვისზე (მაგალითად Canva Pro-ზე)!
   - ნუ ჩამოთვლი მთელ კატეგორიას (არ ჩამოთვალო CapCut, Adobe, YouTube, Spotify ერთად, თუ მომხმარებელს ისინი არ უთხოვია ან არ უკითხავს შედარება/მთელი კატეგორია).
   - მიაწოდე ზუსტი ინფორმაცია ამ კონკრეტულ სერვისზე: ფასი, ვადა, პაკეტის ტიპი, მთავარი უპირატესობები, და განუმარტე რომ შეუძლია შეკვეთა პირდაპირ ქვემოთ მოცემული ბარათიდან.
7. ახლად დამატებული სერვისები:
   - ქვემოთ მოცემულ LIVE კატალოგში დინამიურად ემატება ადმინისტრატორის მიერ შეყვანილი ახალი სერვისები და ნაწილები. როცა მომხმარებელი იკითხავს მათზე, მოძებნე ისინი კატალოგში სათაურით, აღწერით ან საკვანძო სიტყვებით და ურჩიე ზუსტად.
"""

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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

        // 1. კონკრეტული სერვისის პირდაპირი ზუსტი მოთხოვნა:
        // Canva - მხოლოდ Canva Pro (არა სხვა მულტიმედია პროდუქტები)!
        if (q.contains("canva") || q.contains("კანვა")) {
            val canva = catalog.find { it.title.contains("Canva", ignoreCase = true) || it.id == 118 }
            if (canva != null) return listOf(canva)
        }

        // CapCut - მხოლოდ CapCut Pro!
        if (q.contains("capcut") || q.contains("კაპკატ")) {
            val capcut = catalog.find { it.title.contains("CapCut", ignoreCase = true) || it.id == 116 }
            if (capcut != null) return listOf(capcut)
        }

        // Adobe - Adobe Creative Cloud!
        if (q.contains("adobe") || q.contains("ადობ") || q.contains("photoshop") || q.contains("ფოტოშოპ") || q.contains("premiere") || q.contains("პრემიერ")) {
            val adobe = catalog.find { it.title.contains("Adobe", ignoreCase = true) || it.id == 117 }
            if (adobe != null) return listOf(adobe)
        }

        // YouTube
        if (q.contains("youtube") || q.contains("იუთუბ")) {
            val yt = catalog.find { it.title.contains("YouTube", ignoreCase = true) || it.id == 119 }
            if (yt != null) return listOf(yt)
        }

        // Spotify
        if (q.contains("spotify") || q.contains("სპოტიფაი")) {
            val spot = catalog.find { it.title.contains("Spotify", ignoreCase = true) || it.id == 120 }
            if (spot != null) return listOf(spot)
        }

        // Perplexity
        if (q.contains("perplexity") || q.contains("პერპლექს")) {
            val p = catalog.find { it.title.contains("Perplexity", ignoreCase = true) || it.id == 109 }
            if (p != null) return listOf(p)
        }

        // Cursor
        if (q.contains("cursor") || q.contains("კურსორ")) {
            val p = catalog.find { it.title.contains("Cursor", ignoreCase = true) || it.id == 110 }
            if (p != null) return listOf(p)
        }

        // Lovable
        if (q.contains("lovable") || q.contains("ლავაბლ")) {
            val p = catalog.find { it.title.contains("Lovable", ignoreCase = true) || it.id == 111 }
            if (p != null) return listOf(p)
        }

        // Grok
        if (q.contains("grok") || q.contains("გროკ")) {
            val groks = catalog.filter { it.title.contains("Grok", ignoreCase = true) || it.id in listOf(105, 106) }
            if (groks.isNotEmpty()) return groks.take(2)
        }

        // Claude
        if (q.contains("claude") || q.contains("კლოდ")) {
            val claudes = catalog.filter { it.title.contains("Claude", ignoreCase = true) || it.id in listOf(107, 108) }
            if (claudes.isNotEmpty()) return claudes.take(2)
        }

        // ChatGPT
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

        // Google AI / Gemini
        if (q.contains("google ai") || q.contains("gemini") || q.contains("ჯემინი") || q.contains("18 თვ")) {
            val geminis = catalog.filter { it.id in listOf(103, 104, 115) || it.title.contains("Google AI", ignoreCase = true) }
            if (geminis.isNotEmpty()) return geminis.take(3)
        }

        // Google Storage / Gmail
        if (q.contains("gmail") || q.contains("ჯიმეილ") || q.contains("google storage") || q.contains("google one") || q.contains("2tb") || q.contains("5tb") || q.contains("მეხსიერება")) {
            val storages = catalog.filter { it.id in listOf(112, 113, 114, 115) }
            if (storages.isNotEmpty()) return storages.take(3)
        }

        // Windows
        if (q.contains("windows") || q.contains("ვინდოუს")) {
            val win = catalog.find { it.id == 121 || it.title.contains("Windows", ignoreCase = true) }
            if (win != null) return listOf(win)
        }

        // Office
        if (q.contains("office") || q.contains("ოფის") || q.contains("microsoft 365") || q.contains("word") || q.contains("excel") || q.contains("ltsc")) {
            val offices = catalog.filter { it.id in listOf(122, 123, 124) || it.title.contains("Office", ignoreCase = true) }
            if (offices.isNotEmpty()) return offices.take(3)
        }

        // მულტიმედია / ვიდეო / დიზაინის ზოგადი შეკითხვა (მხოლოდ მაშინ თუ ზოგადადაა დასმული და არა კონკრეტულ სერვისზე)
        if (q.contains("მულტიმედია") || q.contains("ვიდეო და დიზაინ") || (q.contains("დიზაინ") && !q.contains("canva") && !q.contains("adobe")) || (q.contains("მონტაჟ") && !q.contains("capcut"))) {
            return listOf(118, 116, 117).mapNotNull { id -> catalog.find { it.id == id } }
        }

        if (q.contains("სტუდენტ") || q.contains("სწავლ")) {
            return listOf(103, 101, 109, 118).mapNotNull { id -> catalog.find { it.id == id } }
        }

        // 2. დინამიური ამოცნობა ადმინის მიერ ახლად დამატებული ან სხვა სერვისებისთვის კატალოგში
        val dynamicExact = catalog.filter { prod ->
            val titleLower = prod.title.lowercase()
            val brandLower = prod.brand.lowercase()
            val keywordsLower = prod.searchKeywords.lowercase()
            val queryWords = q.split(" ", ",", "-", "?", "!", ".").filter { 
                it.length >= 3 && it != "მინდა" && it != "გაქვთ" && it != "სერვისი" && it != "ფასი" && it != "რამდენი" 
            }
            
            titleLower == q || 
            (titleLower.length >= 3 && q.contains(titleLower)) || 
            (brandLower.length >= 3 && q.contains(brandLower)) ||
            (keywordsLower.isNotBlank() && keywordsLower.split(",").any { kw -> 
                val cleanKw = kw.trim()
                cleanKw.length >= 3 && (q.contains(cleanKw) || cleanKw.contains(q))
            }) ||
            (queryWords.isNotEmpty() && queryWords.any { word -> 
                titleLower.contains(word) || (brandLower.length >= 3 && brandLower.contains(word)) || (keywordsLower.length >= 3 && keywordsLower.contains(word))
            })
        }
        if (dynamicExact.isNotEmpty()) {
            return dynamicExact.take(3)
        }

        // საკვანძო სიტყვებითა და ტექსტით ძებნა
        val searchMatched = optimizeSearchFilter(catalog, query)
        if (searchMatched.isNotEmpty()) {
            val first = searchMatched.first()
            if (q.contains(first.title.lowercase())) {
                return listOf(first)
            }
            return searchMatched.take(3)
        }

        return emptyList()
    }

    suspend fun diagnose(
        history: List<ChatMessage>,
        latestUserMessage: String,
        products: List<ProductItem> = emptyList()
    ): String = withContext(Dispatchers.IO) {
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

                        val historyTurns = if (history.isNotEmpty() && history.last().text == latestUserMessage && history.last().sender == "user") {
                            history.dropLast(1).takeLast(6)
                        } else {
                            history.takeLast(6)
                        }

                        var lastRole: String? = null
                        for (msg in historyTurns) {
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
                                put(JSONObject().put("text", latestUserMessage))
                            })
                        })

                        put("contents", contentsArray)

                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.7)
                            put("topP", 0.95)
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

        return@withContext generateLocalDiagnostic(latestUserMessage, products)
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

    private fun generateLocalDiagnostic(query: String, products: List<ProductItem> = emptyList()): String {
        val q = query.lowercase().trim()
        val catalog = if (products.isNotEmpty()) products else ProductRepository.getAllProducts()

        // =========================================================================
        // 0. კონკრეტული სერვისები — პირდაპირი, დეტალური რეკომენდაცია (მთელი კატეგორიის ჩამოთვლის გარეშე)
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

        if (q.contains("gmail") || q.contains("ჯიმეილ") || q.contains("google storage") || q.contains("google one") || q.contains("2tb") || q.contains("5tb") || q.contains("მეხსიერება")) {
            val matched = listOf(112, 113, 114, 115).mapNotNull { id -> catalog.find { it.id == id } }
            return "☁️ **Google-ის ანგარიშები და Cloud მეხსიერების პაკეტები BEKASERVICE-ში:**\n\n" +
                    matched.joinToString("\n") { formatSoftwareProductSummary(it) }
        }

        // მულტიმედია კატეგორიის ზოგადი მიმოხილვა (მხოლოდ მაშინ თუ მომხმარებელი ითხოვს მთელ კატეგორიას ან შედარებას)
        if (q.contains("მულტიმედია") || q.contains("ვიდეო და დიზაინ") || q.contains("ყველა დიზაინ") || (q.contains("მონტაჟ") && q.contains("დიზაინ"))) {
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

        if (q.contains("სტუდენტ") || q.contains("დიზაინერ") || q.contains("მონტაჟ") || q.contains("პროგრამისტ") || q.contains("დეველოპერ")) {
            return "🎯 **BEKASERVICE-ის რეკომენდაციები საქმიანობის მიხედვით:**\n\n" +
                    "• 🎓 **სტუდენტებისა და მკვლევრებისთვის:** Google AI Pro (18 თვე + 2TB), ChatGPT Plus, Perplexity Pro, Canva Pro.\n" +
                    "• 🎬 **ვიდეო მონტაჟისა და დიზაინისთვის:** CapCut Pro, Adobe Creative Cloud (All Apps), Canva Pro.\n" +
                    "• 💻 **პროგრამისტებისა და დეველოპერებისთვის:** Cursor AI Pro, Claude Pro (Private Account), Lovable Pro, ChatGPT Plus.\n" +
                    "• 🏢 **საოფისე და ბიზნეს სამუშაოსთვის:** Windows 11 Pro Retail Key, Office LTSC 2024, Microsoft 365 Personal, Google Storage 2TB/5TB.\n\n" +
                    "ქვემოთ მოცემულ ბარათებზე დაჭერით შეგიძლიათ ნახოთ დეტალური პირობები და შეუკვეთოთ!"
        }

        // დინამიური ამოცნობა ნებისმიერი ახლად დამატებული სერვისისთვის
        val isGreetingOrGeneral = q == "გამარჯობა" || q == "სალამი" || q.contains("როგორ ხარ") || 
                q.contains("მადლობა") || q.contains("მისამართ") || q.contains("სად ხართ")
        val dynamicMatched = catalog.firstOrNull { prod ->
            val titleLower = prod.title.lowercase()
            (titleLower.length >= 3 && q.contains(titleLower)) ||
            (prod.searchKeywords.isNotBlank() && prod.searchKeywords.split(",").any { kw -> kw.trim().length >= 3 && q.contains(kw.trim().lowercase()) })
        }
        if (dynamicMatched != null && !isGreetingOrGeneral) {
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

        // 1. მოდელის დინამიური ამოცნობა
        val detectedDevice = when {
            q.contains("16 pro max") -> "iPhone 16 Pro Max"
            q.contains("16 pro") -> "iPhone 16 Pro"
            q.contains("16") -> "iPhone 16"
            q.contains("15 pro max") -> "iPhone 15 Pro Max"
            q.contains("15 pro") -> "iPhone 15 Pro"
            q.contains("15") -> "iPhone 15"
            q.contains("14 pro max") -> "iPhone 14 Pro Max"
            q.contains("14 pro") -> "iPhone 14 Pro"
            q.contains("14") -> "iPhone 14"
            q.contains("13 pro max") -> "iPhone 13 Pro Max"
            q.contains("13 pro") -> "iPhone 13 Pro"
            q.contains("13") -> "iPhone 13"
            q.contains("12 pro") -> "iPhone 12 Pro"
            q.contains("12") -> "iPhone 12"
            q.contains("11 pro") -> "iPhone 11 Pro"
            q.contains("11") -> "iPhone 11"
            q.contains("xs") || q.contains("xr") || q.contains("x") -> "iPhone X / XR / XS"
            q.contains("s24 ultra") -> "Samsung Galaxy S24 Ultra"
            q.contains("s24") -> "Samsung Galaxy S24"
            q.contains("s23 ultra") -> "Samsung Galaxy S23 Ultra"
            q.contains("s23") -> "Samsung Galaxy S23"
            q.contains("s22") -> "Samsung Galaxy S22"
            q.contains("a54") -> "Samsung Galaxy A54"
            q.contains("a53") -> "Samsung Galaxy A53"
            q.contains("xiaomi 14") -> "Xiaomi 14"
            q.contains("xiaomi 13") -> "Xiaomi 13T / 13"
            q.contains("redmi note 13") -> "Redmi Note 13 Pro"
            q.contains("redmi") -> "Xiaomi Redmi"
            q.contains("pixel 8") -> "Google Pixel 8 Pro"
            q.contains("pixel 7") -> "Google Pixel 7 Pro"
            q.contains("pixel") -> "Google Pixel"
            q.contains("samsung") || q.contains("სამსუნგ") -> "Samsung Galaxy"
            q.contains("iphone") || q.contains("აიფონ") || q.contains("apple") -> "iPhone"
            q.contains("xiaomi") || q.contains("შაომ") -> "Xiaomi"
            else -> ""
        }

        val devicePrefix = if (detectedDevice.isNotBlank()) "📱 მოდელი: **$detectedDevice**\n\n" else ""

        // =========================================================================
        // 1. მისალმება და ზრდილობიანი შეტყობინებები
        // =========================================================================
        val isGreeting = q == "გამარჯობა" || q == "გამარჯობათ" || q == "სალამი" || q == "მოგესალმებით" ||
                q == "დილა მშვიდობისა" || q == "საღამო მშვიდობისა" || q == "hello" || q == "hi" || q == "hey" ||
                q.startsWith("გამარჯობა") || q.startsWith("სალამი") || q.startsWith("მოგესალმებით")

        if (isGreeting && !q.contains("ეკრან") && !q.contains("ელემენტ") && !q.contains("პორტ") &&
            !q.contains("frp") && !q.contains("icloud") && !q.contains("ბლოკ") && !q.contains("გაფუჭდა")
        ) {
            return "გამარჯობა! 👋 მოგესალმებით **BEKASERVICE**-ში. მე ვარ თქვენი AI ტექნიკოსი და პროგრამული სერვისების კონსულტანტი.\n\n" +
                    "რით შემიძლია დაგეხმაროთ დღეს?\n" +
                    "• 💻 **ციფრული პროგრამული სერვისები (24 პაკეტი):** ChatGPT Plus/Pro, Google AI Pro (18 თვე + 2TB), Claude Pro, Grok, Cursor, Canva Pro, CapCut Pro, Adobe CC, YouTube/Spotify Premium, Windows 11 & Office\n" +
                    "• 📱 **ტექნიკური დიაგნოსტიკა და ნაწილები:** ეკრანი, ელემენტი, კამერა, დამტენის პორტი, წყლისგან დაზიანება\n" +
                    "• 🔓 **მობილურის განბლოკვა:** Google FRP, iCloud Activation, SIM Lock, Bootloop\n\n" +
                    "მომწერეთ სასურველი პროგრამული სერვისი ან ტელეფონის მოდელი!"
        }

        // =========================================================================
        // 2. როგორ ხარ / მდგომარეობა
        // =========================================================================
        if (q.contains("როგორ ხარ") || q.contains("როგორ ბრძანდებით") || q.contains("როგორ ხართ") || q == "how are you") {
            return "მადლობა, შესანიშნავად! 😊 მზად ვარ გაგიწიოთ კონსულტაცია როგორც პროგრამულ სერვისებზე (ChatGPT, Google AI Pro, Canva, Windows/Office), ისე ნებისმიერი სმარტფონის შეკეთებაზე.\n\nრა გაინტერესებთ დღეს?"
        }

        // =========================================================================
        // 3. მადლობა / კეთილი სურვილები
        // =========================================================================
        if (q == "მადლობა" || q == "დიდი მადლობა" || q == "გაიხარე" || q.contains("მადლობ") || q == "thanks" || q == "thank you") {
            return "გაიხარეთ, დიდი მადლობა თქვენ! 🤝 ყოველთვის მოხარული ვარ დაგეხმაროთ.\n\n📍 გელოდებით BEKASERVICE-ში: ქ. ახალციხე, კოსტავას 10 (WhatsApp: +995 598 23 33 32). წარმატებულ დღეს გისურვებთ! ✨"
        }

        // =========================================================================
        // 4. ლოკაცია / მისამართი / სამუშაო საათები / კონტაქტი
        // =========================================================================
        if (q.contains("სად ხართ") || q.contains("მისამართ") || q.contains("რომელ საათ") || q.contains("სად მდებარეობს") ||
            q.contains("ლოკაცია") || q.contains("ახალციხე") || q.contains("ტელეფონ") || q.contains("კონტაქტ") || q.contains("ნომერი") ||
            q.contains("whatsapp") || q.contains("ვაცაპ")
        ) {
            return "📍 **BEKASERVICE-ის საკონტაქტო მონაცემები:**\n\n" +
                    "• **მისამართი:** ქ. ახალციხე, მერაბ კოსტავას ქუჩა N10\n" +
                    "• ⏰ **სამუშაო საათები:** ყოველდღე 10:00 - 20:00 (შაბათ-კვირის ჩათვლით)\n" +
                    "• 📞 **ტელეფონი / WhatsApp:** +995 598 23 33 32\n" +
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
                    "დაწერეთ ნებისმიერი პროდუქტის სახელი და გაჩვენებთ დეტალურ პირობებს!"
        }

        // =========================================================================
        // 6. ფასები / რა ღირს
        // =========================================================================
        if ((q.contains("რა ღირს") || q.contains("ფასი") || q.contains("ღირებულება") || q.contains("რა ჯდება")) &&
            !q.contains("ეკრან") && !q.contains("ელემენტ") && !q.contains("პორტ") && !q.contains("frp") && !q.contains("icloud")
        ) {
            return "💰 **BEKASERVICE-ის ფასების მიმოხილვა:**\n\n" +
                    "• 💻 **ციფრული პროგრამული სერვისები (ChatGPT, Google AI Pro 18 თვე, Canva, CapCut, Windows, Office):** მოქმედებს მოქნილი ტარიფები („ფასი შეთანხმებით“ — დამოკიდებულია არჩეულ ვადასა და Private/Shared ტიპზე). შეგიძლიათ პირდაპირ გახსნათ სასურველი ბარათი და მოგვწეროთ WhatsApp/Telegram-ში.\n" +
                    "• 🔋 **ელემენტის შეცვლა:** 65 ₾ - 110 ₾ (ორიგინალი, 6 თვე გარანტია)\n" +
                    "• 📱 **ეკრანის შეცვლა:** 140 ₾ - 390 ₾ (OLED / AMOLED)\n" +
                    "• 🔌 **დამტენის პორტი:** 45 ₾ - 75 ₾\n" +
                    "• 🔓 **Google FRP / iCloud / SIM Lock:** 40 ₾ - 120 ₾\n\n" +
                    "დააკონკრეტეთ რომელი სერვისი ან მოდელი გაინტერესებთ!"
        }

        // =========================================================================
        // 7. დიაგნოსტიკა სიმპტომების მიხედვით
        // =========================================================================
        return when {
            // FRP / Google Account
            q.contains("frp") || q.contains("google account") || q.contains("ფრპ") -> {
                "🔓 **Google FRP (Factory Reset Protection) აქტივაციის ბლოკი**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** მოწყობილობის გადატვირთვის შემდეგ ჩართულია Google-ის ქარხნული დაცვა, რომელიც ითხოვს ძველ ანგარიშს.\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** განბლოკვა ხდება ლიცენზირებული სერვერული ხელსაწყოებით (Knox / Testpoint / Brom). მოწყობილობა ინარჩუნებს ყველა ფუნქციას, SIM სიგნალს და განახლებებს.\n" +
                "• **შესრულების დრო:** 15-20 წუთი ადგილზე (ან დისტანციურად USB-ით)\n" +
                "• **ღირებულება:** 40 ₾ - 50 ₾ (ოფიციალური მუდმივი გარანტიით)\n\n" +
                "📍 გელოდებით ბექასერვისში: ქ. ახალციხე, კოსტავას 10 (WhatsApp: +995 598 23 33 32)."
            }

            // iCloud / Apple ID
            q.contains("icloud") || q.contains("აიქლაუდ") || q.contains("apple id") || q.contains("აქტივაცია") -> {
                "🍏 **Apple iCloud / Activation Lock (Hello Screen ან Passcode)**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** iPhone მიბმულია Apple ID-ზე Find My (FMI) ჩართული სტატუსით.\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** Checkm8 / Ramdisk და სერვერული Token Bypass მეთოდით სრულად ნარჩუნდება სიმ-ბარათის სიგნალი, ზარები, SMS, LTE, iCloud სერვისები და App Store.\n" +
                "• **სამუშაო დრო:** 20-35 წუთი\n" +
                "• **ღირებულება:** 90 ₾ - 120 ₾\n\n" +
                "ბექასერვისში გვაქვს ოფიციალური პროგრამული ლიცენზიები iPhone-ის ყველა ვერსიისთვის. შეგიძლიათ მობრძანდეთ ან მოგვწეროთ WhatsApp-ში!"
            }

            // ქსელის ბლოკი (SIM Lock / Carrier Unlock)
            q.contains("ქსელ") || q.contains("sim") || q.contains("სიმ") || q.contains("unlock") || q.contains("ოპერატორ") -> {
                "📶 **Network Carrier SIM Lock (უცხოურ ოპერატორზე ბლოკი)**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** ტელეფონი ჩამოტანილია საზღვარგარეთიდან და მიბმულია კონკრეტულ ქსელზე (AT&T, T-Mobile, Verizon, Vodafone, Softbank და სხვ.).\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** ოფიციალური Factory Unlock IMEI ბაზით ან R-SIM / eSIM Patch კონფიგურაციით. ტელეფონი იმუშავებს Silknet, Magti და Cellfie-ზე შეუფერხებლად.\n" +
                "• **დრო & ფასი:** 60 ₾ - 75 ₾ (სრული გარანტიით).\n\n" +
                "დეტალებისთვის მოგვაწოდეთ IMEI კოდი ბექასერვისის WhatsApp-ზე: +995 598 23 33 32."
            }

            // Auth Flashing / EDL 9008
            q.contains("auth") || q.contains("edl") || q.contains("9008") || q.contains("msm") -> {
                "🔐 **Emergency Download Mode (EDL 9008 / BROM Hard-Brick)**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** სისტემური ბირთვის დაზიანების გამო პროცესორი გადავიდა დაბალი დონის Qualcomm HS-USB 9008 რეჟიმში და ითხოვს ოფიციალურ სერვერულ Firehose ავტორიზაციას.\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** გვაქვს ოფიციალური მწარმოებლის სერვერული წვდომა Xiaomi, OnePlus, Realme, Oppo მოდელებზე. ტელეფონი აღდგება სრულად 20 წუთში.\n" +
                "• **ღირებულება:** 55 ₾.\n\n" +
                "ბექასერვისი ახალციხეში ერთადერთი სერვისია ოფიციალური Auth სერვერული ხელსაწყოებით!"
            }

            // Bootloop / არ ირთვება / ლოგოზე გაჭედილი
            q.contains("bootloop") || q.contains("აგური") || q.contains("ლოგო") || q.contains("არ ირთვება") || q.contains("ფლეშ") -> {
                "⚙️ **Bootloop / სისტემური ფაილების კოლაფსი**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** მეხსიერების UFS/eMMC დანაყოფის შეცდომა, შეწყვეტილი OTA განახლება ან Bootloader-ის კონფლიქტი.\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** Stock Firmware-ის ოფიციალური გადაწერა, ქეშის გასუფთავება და შესაძლებლობის შემთხვევაში პირადი მონაცემების შენარჩუნება.\n" +
                "• **შესრულების დრო:** 20-30 წუთი | **ღირებულება:** 35 ₾ - 55 ₾\n\n" +
                "ადგილზე გაგირემონტებთ უმოკლეს დროში ბექასერვისში: კოსტავას 10."
            }

            // ეკრანი (ზოლები, ბზარი, სენსორი)
            q.contains("ეკრან") || q.contains("ზოლ") || q.contains("შავ") || q.contains("ბზარ") || q.contains("display") || q.contains("screen") -> {
                "📱 **დისპლეის მატრიცის / სენსორის დაზიანება**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** დარტყმის ან წნევის შედეგად დაზიანდა ორგანული OLED პიქსელების შრე (მწვანე/თეთრი ზოლები, შავი ლაქები) ან სენსორის დიგიტაიზერი.\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** ახალი ორიგინალი OLED / Super AMOLED დისპლეის მონტაჟი, True Tone მონაცემების გადაპროგრამება და ჰერმეტული წებოს აღდგენა.\n" +
                "• **სამუშაო დრო:** 25-35 წუთი ადგილზევე\n" +
                "• **ღირებულება:** 180 ₾ - 390 ₾ (მოდელის მიხედვით, 6-თვიანი გარანტიით)\n\n" +
                "ყველა პოპულარული მოდელის ეკრანი ადგილზე გვაქვს მარაგში ბექასერვისში!"
            }

            // ელემენტი / ბატარეა
            q.contains("ელემენტ") || q.contains("ბატარე") || q.contains("battery") || q.contains("ჯდება") || q.contains("ითიშება") || q.contains("გაბერ") -> {
                "🔋 **ლითიუმ-იონური ელემენტის დეგრადაცია / ცვეთა**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** ელემენტის ქიმიური ცვეთა (ციკლები > 500, Health < 80%) ან შიდა წინააღმდეგობის მატება.\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** 100% ახალი ორიგინალი ელემენტის ინსტალაცია 0-ციკლით. iPhone მოდელებზე გადაგვაქვს ორიგინალი BMS ჩიპი 100% Health-ის საჩვენებლად.\n" +
                "• **დრო & ფასი:** 15-20 წუთი | 65 ₾ - 110 ₾ (6 თვე ოფიციალური გარანტია)\n\n" +
                "მობრძანდით ბექასერვისში და 20 წუთში ჩაგისვამთ ახალ ელემენტს."
            }

            // დამტენის პორტი / არ იტენება
            q.contains("პორტ") || q.contains("დამტენ") || q.contains("charger") || q.contains("type-c") || q.contains("არ იტენება") -> {
                "🔌 **Type-C / Lightning პორტის ან დამუხტვის კონტროლერის ხარვეზი**\n\n" +
                devicePrefix +
                "• **სიმპტომის მიზეზი:** პინების მექანიკური ცვეთა, დაჟანგვა ან Hydra/Tristar/PMIC დამტენის კონტროლერის ხარვეზი.\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** მიკროსკოპული ულტრაბგერითი წმენდა ან ქვედა ორიგინალი შლეიფის შეცვლა Fast Charging / PD მხარდაჭერით.\n" +
                "• **სამუშაო დრო:** 20-30 წუთი | ღირებულება: 55 ₾ - 85 ₾.\n\n" +
                "ბექასერვისში შევამოწმებთ ამპერმეტრით და მოვაგვარებთ უმოკლეს დროში!"
            }

            // წყალი / სითხე
            q.contains("წყალ") || q.contains("სისველ") || q.contains("ჩავარდა") -> {
                "💧 **სითხის მოხვედრა და ელექტროქიმიური კოროზია**\n\n" +
                devicePrefix +
                "• **სასწრაფო რეკომენდაცია:** გამორთეთ მოწყობილობა, არ შეაერთოთ დამტენზე! დენი აჩქარებს კონტაქტების დაჟანგვას.\n" +
                "• **BEKASERVICE-ის გადაწყვეტა:** ტელეფონის დაუყოვნებელი გახსნა, დედაპლატის ულტრაბგერითი ქიმიური წმენდა და მოკლე ჩართვების ლოკალიზება თერმო-კამერით.\n" +
                "• **ფასი:** 50 ₾ - 90 ₾ | დრო: 40-60 წუთი.\n\n" +
                "სასწრაფოდ მოიტანეთ ბექასერვისში: ქ. ახალციხე, კოსტავას 10."
            }

            // კამერა / ფოკუსი
            q.contains("კამერ") || q.contains("ფოკუს") || q.contains("კანკალ") || q.contains("ბუნდოვან") -> {
                "📷 **კამერის ოპტიკური სტაბილიზატორის (OIS) ან ლინზის დაზიანება**\n\n" +
                devicePrefix +
                "• **BEKASERVICE-ის გადაწყვეტა:** ორიგინალი კამერის მოდულის მონტაჟი და ლაზერული ფოკუსირების კალიბრაცია.\n" +
                "• **დრო & ფასი:** 25 წუთი | 140 ₾ - 220 ₾ (6-თვიანი გარანტიით).\n\n" +
                "მოგვწერეთ WhatsApp-ში და დაგიჯავშნით ორიგინალ ნაწილს ბექასერვისში: +995 598 23 33 32."
            }

            // დინამიკი / მიკროფონი / ხმა
            q.contains("დინამიკ") || q.contains("მიკროფონ") || q.contains("ხმა") || q.contains("აუდიო") || q.contains("speaker") -> {
                "🔊 **დინამიკის / აუდიო კოდეკის დაზიანება**\n\n" +
                devicePrefix +
                "• **BEKASERVICE-ის გადაწყვეტა:** ულტრაბგერითი წმენდა ან ორიგინალი სმენის/ზარის დინამიკის შეცვლა.\n" +
                "• **დრო & ფასი:** 20 წუთი | 35 ₾ - 55 ₾."
            }

            // Face ID / Touch ID
            q.contains("face id") || q.contains("ფეის") || q.contains("touch id") || q.contains("ანაბეჭდ") -> {
                "👤 **Face ID / TrueDepth სენსორის ხარვეზი**\n\n" +
                devicePrefix +
                "• **BEKASERVICE-ის გადაწყვეტა:** TrueDepth სენსორის მიკრო-შედუღება და გადაპროგრამება სპეციალური პროგრამატორით.\n" +
                "• **დრო & ფასი:** 45 წუთი | 80 ₾ - 120 ₾ (სრული გარანტიით)."
            }

            // უკანა შუშა / კორპუსი
            q.contains("კორპუს") || q.contains("შუშ") || q.contains("ხუფ") || q.contains("ჩარჩო") || q.contains("glass") -> {
                "✨ **უკანა შუშის / კორპუსის ლაზერული შეცვლა**\n\n" +
                devicePrefix +
                "• **BEKASERVICE-ის გადაწყვეტა:** უკანა შუშის მოხსნა ხდება სპეციალური ლაზერული დანადგარით ტელეფონის დაუშლელად.\n" +
                "• **დრო & ფასი:** 40-50 წუთი | 70 ₾ - 130 ₾ (ორიგინალი ფერებით)."
            }

            else -> {
                val matched = optimizeSearchFilter(catalog, query).take(3)
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
                    "🔍 **ტექნიკური და პროგრამული კონსულტაცია BEKASERVICE-ისგან**\n\n" +
                    devicePrefix +
                    "${modelHint}შეგიძლიათ მკითხოთ როგორც 24 ციფრულ პროგრამულ სერვისზე (ChatGPT Plus/Pro, Google AI Pro 18 თვე, Claude, Grok, Canva, CapCut, Adobe CC, Windows 11, Office), ისე ნებისმიერი ტელეფონის ნაწილსა და შეკეთებაზე.\n\n" +
                    "• 📍 მისამართი: ქ. ახალციხე, კოსტავას ქ. N10\n" +
                    "• ⏰ სამუშაო საათები: ყოველდღე 10:00 - 20:00\n" +
                    "• 📞 WhatsApp კონსულტაცია & შეკვეთა: +995 598 23 33 32"
                }
            }
        }
    }
}

// ==============================================================================
// 7. AI ასისტენტის ინტერფეისი (Gemini Multi-Turn Diagnostics & Software Chat)
// ==============================================================================
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
    var textInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val chatList = remember {
        mutableStateListOf(
            ChatMessage(
                "ai",
                "გამარჯობა! მე ვარ BEKASERVICE-ის ოფიციალური AI ასისტენტი & ტექნიკური კონსულტანტი.\n\n" +
                "შემიძლია დაგეხმაროთ:\n" +
                "• 💻 **24 პროგრამული სერვისის შერჩევაში:** Google AI Pro (18 თვე + 2TB), ChatGPT Plus/Pro, Claude Pro, Grok, Cursor, Canva Pro, CapCut Pro, Adobe CC, Windows 11 & Office\n" +
                "• 📱 **სმარტფონების დიაგნოსტიკასა და ნაწილებში:** ეკრანები, ელემენტები, პორტები, FRP, iCloud, SIM Lock.",
                recommendedProducts = listOf(103, 101, 118).mapNotNull { id -> products.find { it.id == id } }
            )
        )
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val quickQuestions = listOf(
        "✨ Google AI Pro — 18 თვე",
        "🤖 ChatGPT Plus vs Pro შედარება",
        "🎨 Canva Pro & CapCut Pro",
        "🧠 Claude Pro Private vs Shared",
        "🪟 Windows 11 & Office ლიცენზიები",
        "🔓 Google FRP ბლოკის მოხსნა",
        "🍏 iCloud Activation / Apple ID",
        "📱 ეკრანზე მწვანე ზოლი / ბზარი",
        "🔋 ელემენტი 80%-ზე ნაკლებია"
    )

    fun sendMessage(prompt: String) {
        if (prompt.isBlank() || isLoading) return
        val userText = prompt.trim()
        chatList.add(ChatMessage("user", userText))
        textInput = ""
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(chatList.size - 1)
            val reply = GeminiDiagnosticService.diagnose(chatList, userText, products)
            val matchedProducts = GeminiDiagnosticService.findRelevantProducts(userText, products)
            chatList.add(ChatMessage("ai", reply, matchedProducts))
            isLoading = false
            listState.animateScrollToItem(chatList.size - 1)
        }
    }

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
                                        "Gemini",
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
                        chatList.clear()
                        chatList.add(
                            ChatMessage(
                                "ai",
                                "ჩათი გასუფთავდა. მკითხეთ ნებისმიერ პროგრამულ სერვისზე (ChatGPT, Google AI Pro 18 თვე, Canva, Windows/Office) ან ტელეფონის დიაგნოსტიკაზე!",
                                recommendedProducts = listOf(103, 101, 118).mapNotNull { id -> products.find { it.id == id } }
                            )
                        )
                    },
                    modifier = Modifier.size(32.dp)
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
                modifier = Modifier.weight(1f)
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
                                    val isSoft = SoftwareCatalogData.isDigitalSoftwareService(prod)
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
                                                if (isSoft) {
                                                    SoftwareBrandVisualHeader(
                                                        product = prod,
                                                        modifier = Modifier.fillMaxSize(),
                                                        compact = true
                                                    )
                                                } else {
                                                    AsyncImage(
                                                        model = prod.imageUrl,
                                                        contentDescription = prod.title,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
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
                            modifier = Modifier.fillMaxWidth(0.9f)
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
                                    "⚡ AI ამოწმებს BEKASERVICE-ის კატალოგს...",
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
                .padding(top = 6.dp),
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
                            onValueChange = { textInput = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                color = AppThemeColorAdapter.inputTextColor(),
                                lineHeight = 18.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFFFF7A00)),
                            modifier = Modifier.fillMaxWidth()
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
                                    if (isSoft) {
                                        SoftwareBrandVisualHeader(
                                            product = item.product,
                                            modifier = Modifier.fillMaxSize(),
                                            compact = true
                                        )
                                    } else {
                                        AsyncImage(
                                            model = item.product.imageUrl,
                                            contentDescription = item.product.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
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
