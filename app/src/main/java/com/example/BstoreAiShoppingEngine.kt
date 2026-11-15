package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

// ==============================================================================
// BSTORE AI Shopping Advisor Engine (Mode A — Grounded Live Catalog Assistant)
// ==============================================================================

data class AiShoppingReply(
    val text: String,
    val recommendedProducts: List<ProductItem> = emptyList(),
    val isDiagnosticRedirect: Boolean = false,
    val isRetryableError: Boolean = false,
    val allowStockInquiry: Boolean = false,
    val stockInquiryPrefillQuery: String = ""
)

data class AiShoppingSessionContext(
    val lastQuery: String = "",
    val lastBrand: String? = null,
    val lastModel: String? = null,
    val lastCategory: String? = null,
    val lastMaxBudgetGel: Double? = null,
    val lastProductIds: List<Int> = emptyList()
)

class AiShoppingSessionHolder {
    var context: AiShoppingSessionContext = AiShoppingSessionContext()
        private set

    fun updateFromTurn(
        userQuery: String,
        detectedBrand: String?,
        detectedModel: String?,
        detectedCategory: String?,
        maxBudget: Double?,
        recommendedProducts: List<ProductItem>
    ) {
        val prev = context
        val resolvedBrand = detectedBrand
            ?: recommendedProducts.firstOrNull()?.brand?.takeIf { it.isNotBlank() }
            ?: prev.lastBrand
        val resolvedModel = detectedModel ?: prev.lastModel
        val resolvedCategory = detectedCategory
            ?: recommendedProducts.firstOrNull()?.category?.takeIf { it.isNotBlank() }
            ?: prev.lastCategory
        val resolvedBudget = maxBudget ?: prev.lastMaxBudgetGel
        val resolvedIds = if (recommendedProducts.isNotEmpty()) {
            recommendedProducts.map { it.id }
        } else {
            prev.lastProductIds
        }
        context = AiShoppingSessionContext(
            lastQuery = userQuery,
            lastBrand = resolvedBrand,
            lastModel = resolvedModel,
            lastCategory = resolvedCategory,
            lastMaxBudgetGel = resolvedBudget,
            lastProductIds = resolvedIds
        )
    }

    fun reset() {
        context = AiShoppingSessionContext()
    }
}

object BstoreAiShoppingEngine {
    private const val TAG = "BstoreAiShopping"
    private const val MODEL_FIREBASE_AI = "gemini-2.5-flash"
    private val REST_CANDIDATE_MODELS = listOf("gemini-2.5-flash", "gemini-2.0-flash")

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val shoppingResponseSchema by lazy {
        Schema.obj(
            mapOf(
                "replyText" to Schema.string("Natural, warm, Gemini-style human Georgian reply strictly grounded in the live catalog and BSTORE facts."),
                "recommendedProductIds" to Schema.array(
                    Schema.integer("Matching active product ID from the provided catalog. Leave empty for greetings, capability questions, or general conversation.")
                ),
                "isUnavailableProductSearch" to Schema.boolean("Set to true ONLY when the user explicitly searched for a specific product or spare part that does not exist in the catalog.")
            )
        )
    }

    private val firebaseShoppingModel by lazy {
        com.google.firebase.Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_FIREBASE_AI,
            generationConfig = generationConfig {
                temperature = 0.55f
                topP = 0.9f
                responseMimeType = "application/json"
                responseSchema = shoppingResponseSchema
            },
            systemInstruction = content {
                text(SHOPPING_SYSTEM_INSTRUCTION)
            }
        )
    }

    private val SHOPPING_SYSTEM_INSTRUCTION = """
        შენ ხარ „BSTORE“-ის ოფიციალური AI ასისტენტი (Mode A).
        შენი სტილი: ისაუბრე ბუნებრივად, ადამიანურად, მეგობრულად და ცოცხალი ქართულით — ზუსტად თანამედროვე Gemini AI ასისტენტის მანერით, მშრალი შაბლონური ფრაზების გარეშე.

        კრიტიკული წესები:
        1. ზოგადი საუბარი და კითხვები („გამარჯობა“, „რისი გაკეთება შეგიძლია?“, „ვინ ხარ?“, მადლობა, რჩევა):
           - უპასუხე თბილად, თავისუფლად და ადამიანურად, recommendedProductIds დატოვე ცარიელი `[]` და isUnavailableProductSearch დააყენე `false`-ზე.
           - არასოდეს უთხრა ასეთ კითხვებზე „პროდუქტი ვერ მოიძებნა“.
        2. კატალოგის სიზუსტე (Grounding):
           - თუ მომხმარებელი ეძებს კონკრეტულ პროდუქტს, ნაწილს, ფასს ან ფასდაკლებას, დაეყრდენი მხოლოდ მოწოდებულ LIVE კატალოგს (isActive=true).
           - არასოდეს გამოიგონო პროდუქტი, ფასი, ფასდაკლება, მარაგი ან გარანტია.
           - თუ მოთხოვნილი კონკრეტული პროდუქტი კატალოგში არ არის, მხოლოდ მაშინ დააყენე isUnavailableProductSearch = true.
        3. რეჟიმების მკაცრი გამიჯვნა (Mode A vs Mode B):
           - არ ჩაატარო ტექნიკური დიაგნოსტიკა ან სიმპტომების ანალიზი. დაზიანების სიმპტომებზე მოკლედ ურჩიე ზედა ღილაკით გადავიდეს „დიაგნოსტიკა“ რეჟიმში.
        4. BSTORE-ის დადასტურებული ფაქტები:
           - მისამართი: ${AppBrandConfig.SERVICE_CENTER_ADDRESS_KA}
           - სამუშაო საათები: ${AppBrandConfig.WORKING_HOURS_KA}
           - კონტაქტი / WhatsApp: ${AppBrandConfig.SUPPORT_PHONE}
           - გადახდა: საბანკო გადარიცხვა (საქართველოს ბანკი / თიბისი ბანკი) და ნაღდი ანგარიშსწორება მიღებისას (COD).
        5. უპასუხე მოკლედ, გასაგებად და ბუნებრივად ქართულად.
    """.trimIndent()

    // ==============================================================================
    // კატალოგის წყარო — მხოლოდ ცოცხალი Firestore / AdminStorage / მიწოდებული სია
    // ==============================================================================

    fun activeCatalog(products: List<ProductItem>): List<ProductItem> {
        return products.filter { it.isActive && !ProductRepository.isProhibitedOrRetiredService(it) }
    }

    suspend fun loadLatestLiveProducts(
        context: Context,
        currentProducts: List<ProductItem>
    ): List<ProductItem> {
        if (currentProducts.isNotEmpty()) return currentProducts
        return withContext(Dispatchers.IO) {
            val fromCloud = runCatching {
                FirestoreRepository(context).fetchProductsOnce().getOrNull()
            }.getOrNull()
            if (!fromCloud.isNullOrEmpty()) {
                val merged = AdminStorage.mergeCloudWithLocal(context, fromCloud)
                AdminStorage.saveProductsFromCloud(context, merged)
                merged
            } else {
                AdminStorage.loadProducts(context)
            }
        }
    }

    // ==============================================================================
    // ქართული ტექსტის ნორმალიზაცია, სინონიმები, ბიუჯეტი და Typo Tolerance
    // ==============================================================================

    private val GEO_SUFFIXES = listOf(
        "ებიდან", "ებისთვის", "ებზე", "ებში", "ებით", "ების", "ები",
        "ისთვის", "იდან", "ამდე", "მდე", "ით", "ის", "ზე", "ში", "თან", "ს", "ი"
    )

    private val CATEGORY_SYNONYMS: Map<String, List<String>> = mapOf(
        "ეკრანები" to listOf("ეკრან", "დისპლე", "ekran", "display", "screen", "oled", "amoled", "lcd", "incell", "სენსორ"),
        "ელემენტები" to listOf("ელემენტ", "ბატარე", "აკუმულატორ", "element", "batare", "battery", "mah", "health"),
        "დამტენის პორტები" to listOf("პორტ", "დამტენ", "კონექტორ", "შლეიფ", "port", "charging", "charger", "type-c", "usb", "lightning", "damten"),
        "კამერები" to listOf("კამერ", "ლინზ", "ობიექტივ", "kamera", "camera", "ois", "lens"),
        "კორპუსები" to listOf("კორპუს", "ხუფ", "ჩარჩო", "უკანა შუშ", "შუშა", "korpus", "housing", "frame", "back glass", "case"),
        "დინამიკები" to listOf("დინამიკ", "სპიკერ", "აუდიო", "dinamik", "speaker", "audio", "buzzer")
    )

    private val TOKEN_SYNONYMS: Map<String, List<String>> = mapOf(
        "iphone" to listOf("აიფონ", "apple", "ios", "აიფონი"),
        "აიფონ" to listOf("iphone", "apple"),
        "apple" to listOf("აიფონ", "iphone"),
        "samsung" to listOf("სამსუნგ", "galaxy", "გალაქსი"),
        "სამსუნგ" to listOf("samsung", "galaxy"),
        "galaxy" to listOf("სამსუნგ", "samsung"),
        "xiaomi" to listOf("შაომ", "redmi", "poco", "რედმი", "ქსაომ", "სიაომ"),
        "შაომ" to listOf("xiaomi", "redmi", "poco"),
        "redmi" to listOf("რედმი", "xiaomi", "შაომ"),
        "pixel" to listOf("პიქსელ", "google", "გუგლ"),
        "nothing" to listOf("ნათინგ"),
        "ნათინგ" to listOf("nothing"),
        "honor" to listOf("ჰონორ"),
        "oneplus" to listOf("ვანპლას"),
        "pro" to listOf("პრო"),
        "პრო" to listOf("pro"),
        "max" to listOf("მაქს"),
        "მაქს" to listOf("max"),
        "ultra" to listOf("ულტრა"),
        "ულტრა" to listOf("ultra"),
        "plus" to listOf("პლუს"),
        "პლუს" to listOf("plus"),
        "ორიგინალ" to listOf("original", "oem"),
        "original" to listOf("ორიგინალ", "oem")
    )

    private val STOP_WORDS = setOf(
        "მინდა", "მაქვს", "გაქვთ", "არის", "რა", "ღირს", "ფასი", "რომელი", "შეიძლება",
        "მითხარი", "მიჩვენე", "მაჩვენე", "მირჩიე", "მომწერე", "ვიყიდი", "შეძენა", "შეკვეთა",
        "ლარამდე", "ლარად", "ლარი", "ლარში", "ლარიდან", "₾", "gel", "lari",
        "ყველაზე", "იაფი", "ძვირი", "კარგი", "უკეთესი", "ხარისხით", "ფასდაკლება", "ფასდაკლებით",
        "აქცია", "და", "ან", "თუ", "არა", "მე", "თქვენ", "ჩემი", "ჩემთვის", "ტელეფონის", "ნაწილი",
        "მარაგში", "ხელმისაწვდომი", "შემოთავაზება", "ბიუჯეტი", "მაქსიმუმ", "მინიმუმ", "დაბალი", "უფრო"
    )

    internal fun stemWord(raw: String): String {
        var s = raw.trim().lowercase()
            .replace("₾", "")
            .replace(Regex("[^\\p{L}\\p{N}-]"), "")
        if (s.length >= 5) {
            for (suffix in GEO_SUFFIXES) {
                if (s.endsWith(suffix) && s.length - suffix.length >= 3) {
                    s = s.removeSuffix(suffix)
                    break
                }
            }
        }
        return s
    }

    internal fun damerauLevenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val lenA = a.length
        val lenB = b.length
        val dp = Array(lenA + 1) { IntArray(lenB + 1) }
        for (i in 0..lenA) dp[i][0] = i
        for (j in 0..lenB) dp[0][j] = j
        for (i in 1..lenA) {
            for (j in 1..lenB) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var minVal = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    minVal = min(minVal, dp[i - 2][j - 2] + cost)
                }
                dp[i][j] = minVal
            }
        }
        return dp[lenA][lenB]
    }

    data class BudgetRange(val minGel: Double? = null, val maxGel: Double? = null)

    fun extractBudgetRange(query: String): BudgetRange {
        val q = query.lowercase()
        // Range like "200-350 ₾" or "200-დან 350-მდე"
        val rangeRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:ლარ)?(?:-?დან|დან|-|–|to)\s*(\d+(?:[.,]\d+)?)\s*(?:₾|ლარ|gel)?""")
        rangeRegex.find(q)?.let { match ->
            val a = match.groupValues[1].replace(',', '.').toDoubleOrNull()
            val b = match.groupValues[2].replace(',', '.').toDoubleOrNull()
            if (a != null && b != null && a <= b) {
                return BudgetRange(minGel = a, maxGel = b)
            }
        }
        // Max budget like "350 ლარამდე", "350₾-მდე", "350-მდე", "მაქსიმუმ 350", "ბიუჯეტი 350"
        val maxPatterns = listOf(
            Regex("""(\d+(?:[.,]\d+)?)\s*(?:₾|ლარ|ლ|gel)?\s*-?\s*(?:მდე|ამდე|ნაკლებ)"""),
            Regex("""(?:მაქსიმუმ|ბიუჯეტ\p{L}*|არაუმეტეს|ქვემოთ|up to|under|max)\s*:?\s*(\d+(?:[.,]\d+)?)""")
        )
        for (regex in maxPatterns) {
            regex.find(q)?.let { m ->
                val v = m.groupValues[1].replace(',', '.').toDoubleOrNull()
                if (v != null && v > 0.0) return BudgetRange(maxGel = v)
            }
        }
        // Min budget like "200 ლარიდან"
        Regex("""(\d+(?:[.,]\d+)?)\s*(?:₾|ლარ|ლ)?\s*-?\s*(?:იდან|დან|ზემოთ|მეტი)""").find(q)?.let { m ->
            val v = m.groupValues[1].replace(',', '.').toDoubleOrNull()
            if (v != null && v > 0.0) return BudgetRange(minGel = v)
        }
        return BudgetRange()
    }

    fun productNumericPrice(item: ProductItem): Double? {
        return item.parseNumericPrice(item.effectiveDisplayPrice)
            ?: item.parseNumericPrice(item.discountedPrice)
            ?: item.parseNumericPrice(item.price)
    }

    fun detectRequestedCategory(query: String): String? {
        val q = query.lowercase()
        val geoQ = transliterateLatToGeo(q)
        for ((category, keywords) in CATEGORY_SYNONYMS) {
            if (keywords.any { kw -> q.contains(kw) || geoQ.contains(kw) }) {
                return category
            }
            // Also check typo distance against individual query words
            val words = q.split(Regex("[\\s,.;!?/()]+")).filter { it.length >= 4 }
            for (w in words) {
                val stemmedW = stemWord(w)
                if (keywords.any { kw -> kw.length >= 4 && damerauLevenshtein(stemmedW, kw) <= 1 }) {
                    return category
                }
            }
        }
        return null
    }

    fun detectBrandFromCatalogOrText(query: String, catalog: List<ProductItem>): String? {
        val q = query.lowercase()
        val latQ = transliterateGeoToLat(q)
        // Check known brands dynamically present in live catalog first
        val liveBrands = catalog.map { it.brand.trim() }.filter { it.isNotBlank() }.distinct()
        for (brand in liveBrands) {
            val bLower = brand.lowercase()
            if (q.contains(bLower) || latQ.contains(bLower)) return brand
            val synonyms = TOKEN_SYNONYMS[bLower] ?: emptyList()
            if (synonyms.any { q.contains(it) || latQ.contains(it) }) return brand
        }
        return when {
            q.contains("iphone") || q.contains("აიფონ") || q.contains("apple") || latQ.contains("aifon") -> "Apple"
            q.contains("samsung") || q.contains("სამსუნგ") || q.contains("galaxy") || latQ.contains("samsng") -> "Samsung"
            q.contains("xiaomi") || q.contains("შაომ") || q.contains("redmi") || q.contains("poco") -> "Xiaomi"
            q.contains("pixel") || q.contains("გუგლ") || q.contains("პიქსელ") -> "Google Pixel"
            q.contains("oneplus") || q.contains("ვანპლას") -> "OnePlus"
            q.contains("honor") || q.contains("ჰონორ") -> "Honor"
            else -> null
        }
    }

    fun detectModelPhrase(query: String): String? {
        GeminiDiagnosticService.extractDeviceFromText(query)?.let { return it }
        // Match alphanumeric model tokens dynamically (e.g., "2a", "x6", "a56", "17 pro", "note 14")
        val modelMatch = Regex("""(?:iphone|galaxy|redmi|pixel|nothing|poco|honor|oneplus|huawei|s|a)\s*\d+\s*(?:pro\s*max|pro|ultra|plus|max|fe|a|lite)?""", RegexOption.IGNORE_CASE)
            .find(transliterateGeoToLat(query))
        return modelMatch?.value?.trim()?.takeIf { it.length >= 3 }
    }

    private fun extractMeaningfulTokens(query: String): List<String> {
        val rawTokens = query.lowercase()
            .split(Regex("[\\s,.;:!?/()«»\"']+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return rawTokens.mapNotNull { tok ->
            // Ignore budget suffix tokens like "350₾-მდე" or pure price words
            if (tok.endsWith("ლარამდე") || tok.endsWith("₾-მდე") || tok.endsWith("-მდე") && tok.any { it.isDigit() }) {
                return@mapNotNull null
            }
            val cleaned = tok.replace("₾", "")
            val stemmed = stemWord(cleaned)
            if (cleaned in STOP_WORDS || stemmed in STOP_WORDS) return@mapNotNull null
            if (cleaned.length < 2 && !cleaned.any { it.isDigit() }) return@mapNotNull null
            stemmed.ifBlank { cleaned }
        }
    }

    private fun tokenVariants(token: String): Set<String> {
        val lat = transliterateGeoToLat(token)
        val geo = transliterateLatToGeo(token)
        val result = linkedSetOf(token, stemWord(token), lat, geo, stemWord(geo))
        TOKEN_SYNONYMS.forEach { (key, syns) ->
            if (token == key || lat == key || geo == key ||
                (token.length >= 4 && (token.contains(key) || key.contains(token))) ||
                (lat.length >= 4 && damerauLevenshtein(lat, key) <= 1) ||
                (token.length >= 4 && damerauLevenshtein(token, key) <= 1)
            ) {
                result.add(key)
                result.addAll(syns)
            }
        }
        CATEGORY_SYNONYMS.values.flatten().forEach { kw ->
            if (token.length >= 4 && (token.contains(kw) || kw.contains(token) || damerauLevenshtein(token, kw) <= 1)) {
                result.add(kw)
            }
        }
        return result.filterTo(linkedSetOf()) { it.length >= 2 }
    }

    private fun buildSearchHaystack(item: ProductItem): String {
        return buildString(256) {
            append(item.title).append(' ')
            append(item.category).append(' ')
            append(item.subcategory).append(' ')
            append(item.brand).append(' ')
            append(item.exactModel).append(' ')
            append(item.compatibleModels).append(' ')
            append(item.shortDescription).append(' ')
            append(item.description).append(' ')
            append(item.searchKeywords).append(' ')
            append(item.features.joinToString(" ")).append(' ')
            append(item.specifications.entries.joinToString(" ") { "${it.key} ${it.value}" })
        }.lowercase()
    }

    private fun haystackContainsWithTypoTolerance(haystack: String, haystackWords: List<String>, variants: Set<String>): Boolean {
        for (v in variants) {
            if (haystack.contains(v)) return true
            if (v.length >= 4) {
                val maxDist = if (v.length >= 6) 2 else 1
                if (haystackWords.any { w ->
                        abs(w.length - v.length) <= maxDist && damerauLevenshtein(w, v) <= maxDist
                    }
                ) {
                    return true
                }
            }
        }
        return false
    }

    // ==============================================================================
    // ცოცხალი კატალოგის ძებნა + რანჟირება + ბიუჯეტი + ფასდაკლებები
    // ==============================================================================

    fun searchProducts(
        query: String,
        products: List<ProductItem>,
        session: AiShoppingSessionContext? = null,
        includeOutOfStock: Boolean = true,
        maxResults: Int = 4
    ): List<ProductItem> {
        val catalog = activeCatalog(products)
        if (catalog.isEmpty()) return emptyList()

        val q = query.trim().lowercase()
        val budget = extractBudgetRange(query)
        val requestedCategory = detectRequestedCategory(query)
            ?: if (isFollowUpRefinement(q)) session?.lastCategory else null
        val requestedBrand = detectBrandFromCatalogOrText(query, catalog)
            ?: if (isFollowUpRefinement(q)) session?.lastBrand else null
        val requestedModel = detectModelPhrase(query)
            ?: if (isFollowUpRefinement(q)) session?.lastModel else null

        val wantsDiscount = q.contains("ფასდაკლ") || q.contains("აქცი") || q.contains("სეილ") || q.contains("discount")
        val wantsCheapest = q.contains("ყველაზე იაფი") || q.contains("უფრო იაფი") || q.contains("იაფიან") || q.contains("ბიუჯეტ") || q.contains("დაბალი ფას")
        val tokens = extractMeaningfulTokens(query)

        // Base candidate pool filtered by budget, discount, category
        val pool = catalog.filter { item ->
            val price = productNumericPrice(item)
            val minOk = budget.minGel == null || (price != null && price >= budget.minGel)
            val maxOk = budget.maxGel == null || (price != null && price <= budget.maxGel)
            val stockOk = includeOutOfStock || item.inStock
            minOk && maxOk && stockOk
        }

        // Special query: only discounts
        if (wantsDiscount && tokens.isEmpty() && requestedCategory == null && requestedBrand == null) {
            return pool.filter { it.hasActiveDiscount() }
                .sortedWith(
                    compareByDescending<ProductItem> { it.inStock }
                        .thenByDescending { it.calculatedDiscountPercent }
                        .thenBy { productNumericPrice(it) ?: Double.MAX_VALUE }
                )
                .take(maxResults)
        }

        // Special query: cheapest overall or by category/brand/budget
        if (wantsCheapest && tokens.none { it.any { c -> c.isDigit() } }) {
            val refined = pool.filter { item ->
                val catOk = requestedCategory == null || item.category.equals(requestedCategory, ignoreCase = true)
                val brandOk = requestedBrand == null || item.brand.equals(requestedBrand, ignoreCase = true) ||
                    item.title.contains(requestedBrand, ignoreCase = true)
                val discOk = !wantsDiscount || item.hasActiveDiscount()
                catOk && brandOk && discOk
            }
            if (refined.isNotEmpty()) {
                return refined.sortedWith(
                    compareByDescending<ProductItem> { it.inStock }
                        .thenBy { productNumericPrice(it) ?: Double.MAX_VALUE }
                ).take(maxResults)
            }
        }

        // Score every active product
        val scored = pool.mapNotNull { item ->
            val titleL = item.title.lowercase()
            val compatL = item.compatibleModels.lowercase()
            val brandL = item.brand.lowercase()
            val keywordsL = item.searchKeywords.lowercase()
            val haystack = buildSearchHaystack(item)
            val haystackWords = haystack.split(Regex("[\\s,.;:!?/()«»\"'-]+")).filter { it.length >= 3 }

            var score = 0
            var matchedTokens = 0

            // Check exact title / model / keyword match
            if (q.length >= 4 && titleL == q) score += 120
            if (q.length >= 4 && titleL.contains(q)) score += 70
            if (requestedModel != null) {
                val modelCore = requestedModel.lowercase()
                    .removePrefix("apple ").removePrefix("samsung ").removePrefix("galaxy ").removePrefix("xiaomi ").trim()
                if (titleL.contains(modelCore) || compatL.contains(modelCore) || item.exactModel.lowercase().contains(modelCore)) {
                    score += 65
                }
            }
            if (requestedCategory != null && item.category.equals(requestedCategory, ignoreCase = true)) {
                score += 45
            }
            if (requestedBrand != null && (brandL == requestedBrand.lowercase() || titleL.contains(requestedBrand.lowercase()))) {
                score += 35
            }
            if (wantsDiscount && item.hasActiveDiscount()) {
                score += 40
            }

            // Token-level matches with synonym + typo tolerance
            for (tok in tokens) {
                val vars = tokenVariants(tok)
                if (haystackContainsWithTypoTolerance(haystack, haystackWords, vars)) {
                    matchedTokens++
                    score += when {
                        vars.any { titleL.contains(it) || keywordsL.contains(it) } -> 30
                        vars.any { compatL.contains(it) } -> 24
                        else -> 16
                    }
                }
            }

            // Also match dynamic model numbers explicitly if user typed digits like "16" or "24"
            val numericTokens = tokens.filter { it.any { c -> c.isDigit() } }
            val numericMismatchPenalty = numericTokens.count { numTok ->
                !haystack.contains(numTok)
            } * 40
            score -= numericMismatchPenalty

            val hasBudgetOnly = (budget.maxGel != null || budget.minGel != null) && tokens.isEmpty() && requestedCategory == null && requestedBrand == null
            val minimumSatisfied = when {
                hasBudgetOnly -> true
                tokens.isEmpty() && (requestedCategory != null || requestedBrand != null || wantsDiscount) -> score >= 30
                tokens.size == 1 -> matchedTokens == 1 || score >= 45
                else -> matchedTokens >= ((tokens.size + 1) / 2).coerceAtLeast(1) && score >= 35
            }

            if (!minimumSatisfied || score <= 0) return@mapNotNull null

            if (item.inStock) score += 12
            if (item.hasActiveDiscount()) score += 8
            if (item.isFeatured) score += 4

            Pair(item, score)
        }

        val sorted = if (wantsCheapest) {
            scored.sortedWith(
                compareByDescending<Pair<ProductItem, Int>> { it.first.inStock }
                    .thenBy { productNumericPrice(it.first) ?: Double.MAX_VALUE }
                    .thenByDescending { it.second }
            )
        } else {
            scored.sortedWith(
                compareByDescending<Pair<ProductItem, Int>> { it.second }
                    .thenByDescending { it.first.inStock }
                    .thenBy { productNumericPrice(it.first) ?: Double.MAX_VALUE }
            )
        }

        return sorted.map { it.first }.distinctBy { it.id }.take(maxResults)
    }

    // Detect if user specifically asked about a deactivated (`!isActive`) item
    fun findMatchingDeactivatedProducts(query: String, allProducts: List<ProductItem>): List<ProductItem> {
        val deactivated = allProducts.filter { !it.isActive && !ProductRepository.isProhibitedOrRetiredService(it) }
        if (deactivated.isEmpty()) return emptyList()
        val tokens = extractMeaningfulTokens(query)
        if (tokens.isEmpty()) return emptyList()
        return deactivated.filter { item ->
            val haystack = buildSearchHaystack(item)
            val words = haystack.split(Regex("[\\s,.;:!?/()«»\"'-]+")).filter { it.length >= 3 }
            tokens.all { tok -> haystackContainsWithTypoTolerance(haystack, words, tokenVariants(tok)) }
        }
    }

    // ==============================================================================
    // Mode A vs Mode B Intent Classifier & Conversational Intelligence
    // ==============================================================================

    fun isFollowUpRefinement(qLower: String): Boolean {
        val q = qLower.lowercase()
        return q.contains("უფრო იაფი") || q.contains("იაფიანი") || q.contains("სხვა ვარიანტ") ||
            q.contains("ალტერნატივ") || q.contains("ხარისხით რომელი") || q.contains("რომელი ჯობია") ||
            q.contains("შეადარ") || q.contains("ესეც") || q.contains("ამას ") || q.contains("მისი ") ||
            q.contains("გამომიგზავნ") || q.contains("გარანტია რამდენი") || q.contains("მარაგშია") ||
            q == "უფრო იაფი?" || q == "და უფრო იაფი?" || q == "ხარისხით რომელია უკეთესი?"
    }

    fun isDiagnosticOnlyFaultIntent(query: String): Boolean {
        val q = query.lowercase().trim()
        // Explicit request to buy/check price/parts is Shopping, even if mentioning screen/battery
        val explicitShoppingCue = q.contains("ყიდვა") || q.contains("შეძენ") || q.contains("შეკვეთა") ||
            q.contains("რა ღირს") || q.contains("ფასი") || q.contains("ფასდაკლ") || q.contains("აქცი") ||
            q.contains("ლარამდე") || q.contains("კატალოგ") || q.contains("მიჩვენე") ||
            (q.contains("მინდა") && !q.contains("დიაგნოსტიკა") && !GeminiDiagnosticService.isDiagnosticFaultQuery(q))

        if (explicitShoppingCue && !GeminiDiagnosticService.isDiagnosticFaultQuery(q)) {
            return false
        }
        return GeminiDiagnosticService.isDiagnosticFaultQuery(q) ||
            q.contains("დიაგნოსტიკა") || q.contains("სიმპტომ") || q.contains("ტექნიკოს") ||
            q.contains("პლატა") || q.contains("მიკროსკოპ") || q.contains("შემოწმება ჩაუტარ")
    }

    private fun isGreeting(q: String): Boolean {
        return q == "გამარჯობა" || q == "გამარჯობათ" || q == "სალამი" || q == "მოგესალმებით" ||
            q == "პრივეტ" || q == "ჰაი" || q == "hi" || q == "hello" || q == "hey" ||
            ((q.startsWith("გამარჯობა") || q.startsWith("სალამი") || q.startsWith("მოგესალმებით")) && q.length <= 28)
    }

    private fun isCapabilityOrHelpQuery(q: String): Boolean {
        val clean = q.lowercase().trim()
        return clean.contains("რისი გაკეთება შეგიძლია") ||
            clean.contains("რა შეგიძლია") ||
            clean.contains("რით შეგიძლია") ||
            clean.contains("რით დამეხმარ") ||
            clean.contains("როგორ შეგიძლია დამეხმარ") ||
            clean.contains("რაში დამეხმარ") ||
            clean.contains("ვინ ხარ") ||
            clean.contains("რა ხარ") ||
            clean.contains("როგორ მუშაობ") ||
            clean.contains("რა ფუნქციები გაქვს") ||
            clean.contains("what can you do") ||
            clean.contains("who are you") ||
            clean == "დახმარება" ||
            clean == "help"
    }

    private fun isConversationalSmallTalk(q: String): Boolean {
        val clean = q.lowercase().trim()
        return clean == "რას შვები" || clean == "რას აკეთებ" || clean == "ოკ" || clean == "კარგი" ||
            clean == "გასაგებია" || clean == "საინტერესოა" || clean == "მომეწონა" ||
            clean == "სუპერ" || clean == "მაგარია" || clean == "ბოდიში"
    }

    private fun isHowAreYou(q: String): Boolean {
        return q.contains("როგორ ხარ") || q.contains("როგორ ხართ") || q.contains("რავა ხარ") ||
            q.contains("როგორ ბრძანდებით") || q == "how are you"
    }

    private fun isThanks(q: String): Boolean {
        return q.contains("მადლობ") || q == "გაიხარე" || q == "გაიხარეთ" || q == "thanks" || q == "thank you"
    }

    private fun isHumorOrTightBudget(q: String): Boolean {
        return q.contains("ფული არ მაქვს") || q.contains("ბევრი ფული არ") || q.contains("დაბალი ბიუჯეტი") ||
            q.contains("იაფად რა") || q.contains("ხელფასამდე") || q.contains("😂") || q.contains("😅")
    }

    private fun isAmbiguousBestQuery(q: String): Boolean {
        return q.contains("ყველაზე მაგარი") || q.contains("საუკეთესო რა გაქვთ") ||
            q.contains("შენ რომელს იყიდდი") || (q.contains("რას მირჩევ") && extractMeaningfulTokens(q).isEmpty() && detectRequestedCategory(q) == null)
    }

    private fun isPaymentDeliveryStoreQuery(q: String): Boolean {
        return q.contains("გადახდ") || q.contains("ანგარიშსწორ") || q.contains("ბანკ") || q.contains("iban") ||
            q.contains("მიწოდ") || q.contains("კურიერ") || q.contains("ადგილზე") || q.contains("ჩაბარ") ||
            q.contains("მისამართ") || q.contains("სად ხართ") || q.contains("სამუშაო საათ") || q.contains("რომელ საათ") ||
            q.contains("კონტაქტ") || q.contains("ნომერ") || q.contains("whatsapp") || q.contains("ვაცაპ")
    }

    fun isGenuineProductSearchIntent(query: String, activeCatalog: List<ProductItem>): Boolean {
        val clean = query.trim()
        val qLower = clean.lowercase()
        if (clean.isBlank()) return false
        if (isGreeting(qLower) || isCapabilityOrHelpQuery(qLower) || isHowAreYou(qLower) ||
            isThanks(qLower) || isConversationalSmallTalk(qLower) || isPaymentDeliveryStoreQuery(qLower)
        ) {
            return false
        }
        if (detectRequestedCategory(clean) != null ||
            detectBrandFromCatalogOrText(clean, activeCatalog) != null ||
            detectModelPhrase(clean) != null
        ) {
            return true
        }
        val productSignalWords = listOf(
            "ეკრან", "ელემენტ", "ბატარე", "კამერ", "დინამიკ", "პორტ", "დამტენ", "კაბელ",
            "ყურსასმენ", "აუდიო", "როუტერ", "მოდემ", "აქსესუარ", "ქეის", "შუშა", "მატრიც",
            "ნაწილ", "მოდელ", "ფასი", "რა ღირს", "მარაგში", "გაქვთ", "ყიდვა", "შეკვეთ",
            "ლარ", "ფასდაკლ", "აქცი", "ბიუჯეტ", "phone", "screen", "battery", "pro", "max", "ultra"
        )
        if (productSignalWords.any { qLower.contains(it) }) return true

        val meaningfulTokens = extractMeaningfulTokens(clean)
        return meaningfulTokens.any { tok ->
            tok.any { it.isDigit() } || tok.any { it in 'a'..'z' }
        }
    }

    // ==============================================================================
    // მთავარი ასინქრონული შოპინგ პასუხის გენერატორი (Gemini AI + Live Catalog Grounding)
    // ==============================================================================

    suspend fun generateShoppingReply(
        context: Context,
        history: List<ChatMessage>,
        latestUserMessage: String,
        products: List<ProductItem>,
        sessionHolder: AiShoppingSessionHolder
    ): AiShoppingReply {
        val cleanInput = latestUserMessage.trim()
        val qLower = cleanInput.lowercase()

        // 1. Mode A / Mode B მკაცრი გამიჯვნა — დიაგნოსტიკის მოთხოვნისას არ გაცემს დიაგნოზს Mode A-ში
        if (isDiagnosticOnlyFaultIntent(cleanInput)) {
            return AiShoppingReply(
                text = "მე ვარ BSTORE-ის AI შოპინგ ასისტენტი და გეხმარებით პროდუქტების, სათადარიგო ნაწილების, ფასებისა და ფასდაკლებების შერჩევაში! 📱\n\n" +
                    "ტექნიკური დიაგნოსტიკისა და სიმპტომების შესაფასებლად, გთხოვთ, ზედა მენიუდან გადართოთ რეჟიმზე „დიაგნოსტიკა“ (🔬).",
                recommendedProducts = emptyList(),
                isDiagnosticRedirect = true
            )
        }

        val liveProducts = loadLatestLiveProducts(context, products)
        val activeCatalog = activeCatalog(liveProducts)

        // 2. შეამოწმე, ხომ არ ითხოვს ადმინის მიერ გამორთულ (isActive=false) პროდუქტს
        val deactivatedExact = findMatchingDeactivatedProducts(cleanInput, liveProducts)
        val localResult = buildGroundedLocalShoppingReply(
            userText = cleanInput,
            activeCatalog = activeCatalog,
            deactivatedMatches = deactivatedExact,
            sessionHolder = sessionHolder
        )

        // 3. თუ მარტივი მისალმება / კითხვა შესაძლებლობებზე / მადლობა / მაღაზიის ფაქტებია ან დეაქტივირებული პროდუქტი — მყისიერი და ადამიანური პასუხი
        val shouldBypassLlm = isGreeting(qLower) || isCapabilityOrHelpQuery(qLower) || isHowAreYou(qLower) ||
            isThanks(qLower) || isConversationalSmallTalk(qLower) || isPaymentDeliveryStoreQuery(qLower) ||
            deactivatedExact.isNotEmpty()

        if (shouldBypassLlm) {
            return localResult
        }

        // 4. Hybrid AI (Firebase AI Logic ან BuildConfig.GEMINI_API_KEY) Grounded Catalog JSON-ით
        val candidatePool = if (localResult.recommendedProducts.isNotEmpty()) {
            localResult.recommendedProducts
        } else {
            activeCatalog.sortedByDescending { it.hasActiveDiscount() }.take(12)
        }

        val llmReply = tryGenerateWithGemini(
            context = context,
            history = history,
            latestUserMessage = cleanInput,
            activeCatalog = activeCatalog,
            candidateSlice = candidatePool,
            sessionContext = sessionHolder.context
        )

        if (llmReply != null) {
            val activeById = activeCatalog.associateBy { it.id }
            val validatedProds = llmReply.recommendedIds
                .mapNotNull { activeById[it] }
                .distinctBy { it.id }
                .ifEmpty { localResult.recommendedProducts }
                .take(4)

            val categoryHint = detectRequestedCategory(cleanInput)
            val brandHint = detectBrandFromCatalogOrText(cleanInput, activeCatalog)
            val modelHint = detectModelPhrase(cleanInput)
            val budgetHint = extractBudgetRange(cleanInput).maxGel
            sessionHolder.updateFromTurn(cleanInput, brandHint, modelHint, categoryHint, budgetHint, validatedProds)

            val genuineSearch = isGenuineProductSearchIntent(cleanInput, activeCatalog)
            val shouldAllowInquiry = validatedProds.isEmpty() &&
                genuineSearch &&
                !isAmbiguousBestQuery(qLower) &&
                !isHumorOrTightBudget(qLower) &&
                llmReply.isUnavailableProductSearch != false

            return AiShoppingReply(
                text = llmReply.replyText,
                recommendedProducts = validatedProds,
                allowStockInquiry = shouldAllowInquiry,
                stockInquiryPrefillQuery = if (shouldAllowInquiry) cleanInput else ""
            )
        }

        // 5. სრულად ფუნქციური Grounded Local Advisor (არასდროს იგონებს ფასს/მარაგს/გარანტიას)
        return localResult
    }

    private data class LlmStructuredReply(
        val replyText: String,
        val recommendedIds: List<Int>,
        val isUnavailableProductSearch: Boolean? = null
    )

    private suspend fun tryGenerateWithGemini(
        context: Context,
        history: List<ChatMessage>,
        latestUserMessage: String,
        activeCatalog: List<ProductItem>,
        candidateSlice: List<ProductItem>,
        sessionContext: AiShoppingSessionContext
    ): LlmStructuredReply? {
        if (activeCatalog.isEmpty()) return null
        val fp = android.os.Build.FINGERPRINT.orEmpty()
        val hw = android.os.Build.HARDWARE.orEmpty()
        if (fp.contains("robolectric", ignoreCase = true) || hw.contains("robolectric", ignoreCase = true)) {
            return null
        }
        return withContext(Dispatchers.IO) {

        val catalogPrompt = buildString {
            append("=== BSTORE LIVE ACTIVE CATALOG (${activeCatalog.size} active items) ===\n")
            candidateSlice.take(15).forEach { p ->
                val priceStr = if (p.hasActiveDiscount()) {
                    "${p.effectiveDisplayPrice} (ძველი ფასი ${p.formattedOriginalPrice}, ფასდაკლება -${p.calculatedDiscountPercent}%)"
                } else {
                    p.effectiveDisplayPrice
                }
                append("ID:${p.id} | ${p.title} | კატეგორია: ${p.category} | ბრენდი: ${p.brand} | თავსებადობა: ${p.compatibleModels} | ფასი: $priceStr | გარანტია: ${p.warranty} | მარაგი: ${if (p.inStock) "მარაგშია" else "ამოწურულია"}\n")
            }
            if (sessionContext.lastProductIds.isNotEmpty() || sessionContext.lastBrand != null || sessionContext.lastCategory != null) {
                append("\n=== წინა დიალოგის კონტექსტი (Mode A) ===\n")
                append("ბრენდი: ${sessionContext.lastBrand ?: "-"}, მოდელი: ${sessionContext.lastModel ?: "-"}, კატეგორია: ${sessionContext.lastCategory ?: "-"}, ნაჩვენები ID-ები: ${sessionContext.lastProductIds}\n")
            }
        }

        // A. Try Firebase AI Logic when Firebase is genuinely initialized
        val isFirebaseInitialized = runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
        if (isFirebaseInitialized) {
            val firebaseAttempt = runCatching {
                withTimeout(12_000L) {
                    val promptContent = content {
                        text("$catalogPrompt\n\nმომხმარებლის კითხვა: $latestUserMessage")
                    }
                    val resp = firebaseShoppingModel.generateContent(promptContent)
                    parseStructuredJson(resp.text.orEmpty())
                }
            }.onFailure { err ->
                Log.d(TAG, "Firebase AI Logic fallback to local/REST: ${err.message}")
            }.getOrNull()
            if (firebaseAttempt != null) return@withContext firebaseAttempt
        }

        // B. Optional fallback via BuildConfig.GEMINI_API_KEY if configured cleanly
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            for (modelName in REST_CANDIDATE_MODELS) {
                val restAttempt = runCatching {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val requestJson = JSONObject().apply {
                        put("systemInstruction", JSONObject().apply {
                            put("parts", JSONArray().put(JSONObject().put("text", SHOPPING_SYSTEM_INSTRUCTION + "\n\n" + catalogPrompt)))
                        })
                        val contentsArray = JSONArray()
                        history.takeLast(6).forEach { msg ->
                            if (msg.text.isNotBlank() && msg.text.trim() != latestUserMessage.trim()) {
                                contentsArray.put(JSONObject().apply {
                                    put("role", if (msg.sender == "user") "user" else "model")
                                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                                })
                            }
                        }
                        contentsArray.put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().put(JSONObject().put("text", latestUserMessage)))
                        })
                        put("contents", contentsArray)
                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.55)
                            put("responseMimeType", "application/json")
                        })
                    }
                    val req = Request.Builder()
                        .url(url)
                        .post(requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    httpClient.newCall(req).execute().use { res ->
                        if (!res.isSuccessful) return@use null
                        val bodyStr = res.body?.string().orEmpty()
                        val json = JSONObject(bodyStr)
                        val text = json.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text", "")
                            .orEmpty()
                        parseStructuredJson(text)
                    }
                }.getOrNull()
                if (restAttempt != null) return@withContext restAttempt
            }
        }

        null
        }
    }

    private fun parseStructuredJson(rawText: String): LlmStructuredReply? {
        val cleaned = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        if (!cleaned.startsWith("{")) return null
        return runCatching {
            val obj = JSONObject(cleaned)
            val reply = obj.optString("replyText", "").trim()
            if (reply.isBlank()) return@runCatching null
            val idsArr = obj.optJSONArray("recommendedProductIds")
            val ids = mutableListOf<Int>()
            if (idsArr != null) {
                for (i in 0 until idsArr.length()) {
                    val v = idsArr.optInt(i, -1)
                    if (v > 0) ids.add(v)
                }
            }
            val unavailable = if (obj.has("isUnavailableProductSearch")) {
                obj.optBoolean("isUnavailableProductSearch", false)
            } else {
                null
            }
            LlmStructuredReply(replyText = reply, recommendedIds = ids, isUnavailableProductSearch = unavailable)
        }.getOrNull()
    }

    // ==============================================================================
    // Grounded Local Advisor (Deterministic + Rich Conversational Georgian Logic)
    // ==============================================================================

    fun buildGroundedLocalShoppingReply(
        userText: String,
        activeCatalog: List<ProductItem>,
        deactivatedMatches: List<ProductItem> = emptyList(),
        sessionHolder: AiShoppingSessionHolder = AiShoppingSessionHolder()
    ): AiShoppingReply {
        val q = userText.trim().lowercase()

        // 0. Mode A Isolation check
        if (isDiagnosticOnlyFaultIntent(userText)) {
            return AiShoppingReply(
                text = "მე ვარ BSTORE-ის AI შოპინგ ასისტენტი და გეხმარებით პროდუქტების, სათადარიგო ნაწილების, ფასებისა და ფასდაკლებების შერჩევაში! 📱\n\n" +
                    "ტექნიკური დიაგნოსტიკისა და სიმპტომების დეტალური შეფასებისთვის, გთხოვთ, ზედა გადამრთველიდან აირჩიოთ „დიაგნოსტიკა“ (🔬).",
                recommendedProducts = emptyList(),
                isDiagnosticRedirect = true
            )
        }

        // 1. Deactivated item check — never recommend deactivated item, offer active alternatives
        if (deactivatedMatches.isNotEmpty()) {
            val deactivatedItem = deactivatedMatches.first()
            val alternatives = activeCatalog.filter {
                it.inStock && (it.category.equals(deactivatedItem.category, true) || it.brand.equals(deactivatedItem.brand, true))
            }.take(3)
            val altHint = if (alternatives.isNotEmpty()) {
                "\n\nსამაგიეროდ, ამჟამად ხელმისაწვდომი გვაქვს მსგავსი აქტიური ვარიანტები:"
            } else {
                "\n\nშეგიძლიათ მომწეროთ სხვა მოდელი ან ბიუჯეტი და მოგიძებნით საუკეთესო ვარიანტს."
            }
            return AiShoppingReply(
                text = "„${deactivatedItem.title}“ ამ ეტაპზე გაყიდვაში/კატალოგში არ არის აქტიური.$altHint",
                recommendedProducts = alternatives,
                allowStockInquiry = true,
                stockInquiryPrefillQuery = deactivatedItem.title
            )
        }

        // 2. Capability & conversational intent BEFORE plain greeting
        if (isCapabilityOrHelpQuery(q)) {
            return AiShoppingReply(
                text = "გამარჯობა! 😊 მე ვარ BSTORE-ის ჭკვიანი AI ასისტენტი და სიამოვნებით გაგიწევ კონსულტაციას.\n\n" +
                    "შემიძლია დაგეხმარო:\n" +
                    "• სასურველი ტელეფონის, ეკრანის, ელემენტის, ყურსასმენის თუ აქსესუარის პოვნაში მოდელითა და ბიუჯეტით;\n" +
                    "• ფასების, ფასდაკლებების, გარანტიისა და ხარისხის კლასების (Original / AAA) შედარებაში;\n" +
                    "• მაღაზიის მისამართის, მიწოდებისა და გადახდის პირობების დაზუსტებაში.\n\n" +
                    "თუ რომელიმე კონკრეტული მოდელი კატალოგში ვერ იპოვე, იქვე შეძლებ საწყობში გადამოწმების მოთხოვნის გაგზავნას, ხოლო ტექნიკურ ხარვეზებზე ზედა „დიაგნოსტიკა“ რეჟიმში დაგეხმარები. მითხარი, ახლა რას ეძებ? ✨",
                recommendedProducts = emptyList(),
                allowStockInquiry = false
            )
        }
        if (isGreeting(q)) {
            return AiShoppingReply(
                text = "გამარჯობა! 👋 მიხარია, რომ BSTORE-ში შემოიარე.\n\n" +
                    "მითხარი, რომელი მოდელისთვის ეძებ ნაწილს ან აქსესუარს, რა ბიუჯეტში გსურს ჩატევა ან უბრალოდ რა გაინტერესებს — მაშინვე შეგირჩევ საუკეთესო ვარიანტს! 😊",
                recommendedProducts = emptyList(),
                allowStockInquiry = false
            )
        }
        if (isHowAreYou(q)) {
            return AiShoppingReply(
                text = "მშვენივრად ვარ, მადლობა მოკითხვისთვის! 😊 თავად როგორ ხარ? თუ BSTORE-ის კატალოგში ეძებ ტელეფონს, ეკრანს, ელემენტს ან ყურსასმენს — მომწერე მოდელი და სიამოვნებით მოგიძებნი.",
                recommendedProducts = emptyList(),
                allowStockInquiry = false
            )
        }
        if (isThanks(q)) {
            return AiShoppingReply(
                text = "არაფრის, დიდი სიამოვნებით! 🤝 თუ კიდევ დაგჭირდება ფასის შედარება, მოდელის დაზუსტება ან შეკვეთაში დახმარება — ნებისმიერ დროს მომწერე.",
                recommendedProducts = emptyList(),
                allowStockInquiry = false
            )
        }
        if (isConversationalSmallTalk(q)) {
            return AiShoppingReply(
                text = "აქ ვარ და მზად ვარ დაგეხმარო! 😊 მომწერე სასურველი მოწყობილობა, ბრენდი ან ბიუჯეტი და ერთად შევარჩიოთ BSTORE-ის კატალოგში.",
                recommendedProducts = emptyList(),
                allowStockInquiry = false
            )
        }

        // 3. Store Info, Payments, Delivery
        if (isPaymentDeliveryStoreQuery(q) && detectRequestedCategory(userText) == null && detectModelPhrase(userText) == null) {
            return AiShoppingReply(
                text = "📍 **BSTORE-ის მაღაზია, გადახდა და მიწოდება:**\n\n" +
                    "• **მისამართი:** ${AppBrandConfig.SERVICE_CENTER_ADDRESS_KA}\n" +
                    "• **სამუშაო საათები:** ${AppBrandConfig.WORKING_HOURS_KA}\n" +
                    "• **კონტაქტი / WhatsApp:** ${AppBrandConfig.SUPPORT_PHONE}\n" +
                    "• **გადახდის მეთოდები:** საბანკო გადარიცხვა (საქართველოს ბანკი / თიბისი ბანკი) და ნაღდი ანგარიშსწორება მიღებისას (COD)\n" +
                    "• **მიწოდება:** კურიერით მიწოდება ან მაღაზიიდან გატანა.\n\n" +
                    "რომელი პროდუქტის შეკვეთას გეგმავთ?",
                recommendedProducts = emptyList()
            )
        }

        // 4. Casual / Humor / Tight Budget
        if (isHumorOrTightBudget(q)) {
            val budget = extractBudgetRange(userText)
            val affordable = activeCatalog
                .filter { it.inStock && (budget.maxGel == null || (productNumericPrice(it) ?: Double.MAX_VALUE) <= budget.maxGel) }
                .sortedWith(
                    compareByDescending<ProductItem> { it.hasActiveDiscount() }
                        .thenBy { productNumericPrice(it) ?: Double.MAX_VALUE }
                )
                .take(3)
            return if (affordable.isNotEmpty()) {
                val rangeText = affordable.joinToString("\n") { "• **${it.title}** — **${it.effectiveDisplayPrice}**" }
                AiShoppingReply(
                    text = "აბსოლუტურად მესმის, ბიუჯეტს გაფრთხილება უნდა 😄 BSTORE-ში გვაქვს ხელმისაწვდომი და ფასდაკლებული ვარიანტებიც:\n\n$rangeText\n\nთუ მომწერთ თქვენი ტელეფონის მოდელს ან ბიუჯეტს, ზუსტად იმის მიხედვით შეგირჩევთ!",
                    recommendedProducts = affordable
                )
            } else {
                AiShoppingReply(
                    text = "ბიუჯეტის დაზოგვა ჭკვიანურია 😄 მომწერეთ ტელეფონის მოდელი და რა თანხაში გინდათ ჩატევა — შევეცდები საუკეთესო ვარიანტი გიპოვოთ!",
                    recommendedProducts = emptyList()
                )
            }
        }

        // 5. Ambiguous "Best" / "What do you recommend" without product specifics
        if (isAmbiguousBestQuery(q)) {
            val discountedTop = activeCatalog.filter { it.inStock && it.hasActiveDiscount() }
                .sortedByDescending { it.calculatedDiscountPercent }
                .take(3)
            val showcase = discountedTop.ifEmpty { activeCatalog.filter { it.inStock && it.isFeatured }.take(3) }
            return AiShoppingReply(
                text = "საუკეთესო არჩევანი დამოკიდებულია თქვენი ტელეფონის მოდელსა და ბიუჯეტზე 🙂\n\n" +
                    "მომწერეთ: რომელი ტელეფონისთვის გჭირდებათ ნაწილი (მაგ. iPhone 16 Pro, Samsung S24) და რა კატეგორია გაინტერესებთ (ეკრანი, ელემენტი, კამერა)?\n" +
                    if (showcase.isNotEmpty()) "\nამჟამად კატალოგში პოპულარული/აქციური ვარიანტებია:" else "",
                recommendedProducts = showcase
            )
        }

        val session = sessionHolder.context

        // 6. Multi-turn follow-up: "და ხარისხით რომელია უკეთესი?" or comparison
        val isComparisonOrQuality = q.contains("ხარისხით რომელი") || q.contains("რომელი ჯობია") ||
            q.contains("შეადარ") || q.contains("განსხვავება") || q.contains("სხვაობა")
        if (isComparisonOrQuality) {
            val prevIds = session.lastProductIds
            val contextProducts = if (prevIds.isNotEmpty()) {
                activeCatalog.filter { it.id in prevIds }
            } else {
                searchProducts(userText, activeCatalog, session, maxResults = 3)
            }
            if (contextProducts.isNotEmpty()) {
                val primary = contextProducts.first()
                val comparisonText = buildString {
                    append("ხარისხისა და მახასიათებლების მიხედვით BSTORE-ის კატალოგში ასეთი სურათია:\n\n")
                    contextProducts.forEach { prod ->
                        append("• **${prod.title}** — **${prod.effectiveDisplayPrice}** | მდგომარეობა: ${prod.condition} | გარანტია: ${prod.warranty}\n")
                        if (prod.features.isNotEmpty()) {
                            append("  მახასიათებლები: ${prod.features.take(3).joinToString(", ")}\n")
                        }
                    }
                    if (primary.type != "software" && !primary.category.equals("პროგრამული სერვისები", true)) {
                        append("\n💡 **Original** კლასი გთავაზობთ ქარხნულ ფერებს, სიმკვეთრესა და სენსორულ სიზუსტეს, ხოლო **High Copy / AAA** ვარიანტები ბიუჯეტური ალტერნატივაა. დეტალურ ბარათზე შეგიძლიათ ხარისხის გადამრთველით ნახოთ ფასები.")
                    }
                }
                sessionHolder.updateFromTurn(userText, session.lastBrand, session.lastModel, session.lastCategory, null, contextProducts)
                return AiShoppingReply(text = comparisonText.trim(), recommendedProducts = contextProducts)
            }
        }

        // 7. Multi-turn follow-up: "უფრო იაფი?" -> cheaper active items in same category/brand
        val isCheaperFollowUp = (q.contains("უფრო იაფი") || q.contains("იაფიანი") || q.contains("იაფად")) &&
            extractMeaningfulTokens(userText).isEmpty() && session.lastProductIds.isNotEmpty()
        if (isCheaperFollowUp) {
            val prevItems = activeCatalog.filter { it.id in session.lastProductIds }
            val maxPrevPrice = prevItems.mapNotNull { productNumericPrice(it) }.minOrNull() ?: Double.MAX_VALUE
            val cheaperSameCatBrand = activeCatalog.filter { item ->
                val p = productNumericPrice(item) ?: return@filter false
                val sameCat = session.lastCategory == null || item.category.equals(session.lastCategory, true)
                val sameBrand = session.lastBrand == null || item.brand.equals(session.lastBrand, true)
                item.inStock && sameCat && sameBrand && p < maxPrevPrice && item.id !in session.lastProductIds
            }.sortedBy { productNumericPrice(it) ?: Double.MAX_VALUE }.take(3)

            val fallbackSameCat = if (cheaperSameCatBrand.isEmpty()) {
                activeCatalog.filter { item ->
                    val p = productNumericPrice(item) ?: return@filter false
                    val sameCat = session.lastCategory == null || item.category.equals(session.lastCategory, true)
                    item.inStock && sameCat && p < maxPrevPrice
                }.sortedBy { productNumericPrice(it) ?: Double.MAX_VALUE }.take(3)
            } else {
                cheaperSameCatBrand
            }

            if (fallbackSameCat.isNotEmpty()) {
                val contextLabel = listOfNotNull(session.lastBrand, session.lastCategory).joinToString(" • ").ifBlank { "კატალოგში" }
                sessionHolder.updateFromTurn(userText, session.lastBrand, session.lastModel, session.lastCategory, null, fallbackSameCat)
                return AiShoppingReply(
                    text = "დიახ! $contextLabel უფრო ბიუჯეტური და ხელმისაწვდომი ვარიანტებია:\n\n" +
                        fallbackSameCat.joinToString("\n") { "• **${it.title}** — **${it.effectiveDisplayPrice}** (გარანტია: ${it.warranty})" },
                    recommendedProducts = fallbackSameCat
                )
            } else if (prevItems.isNotEmpty()) {
                val firstPrev = prevItems.first()
                return AiShoppingReply(
                    text = "ამ მოდელზე უფრო იაფი სხვა პროდუქტი კატალოგში ამჟამად არ არის, თუმცა „${firstPrev.title}“-ის დეტალურ ბარათზე შეგიძლიათ ნახოთ ხელმისაწვდომი ხარისხის კლასები (Original / High Copy / AAA).",
                    recommendedProducts = prevItems.take(2)
                )
            }
        }

        // 8. Catalog search (exact, synonym, typo, budget, discounts)
        val matched = searchProducts(userText, activeCatalog, session, maxResults = 3)
        val categoryHint = detectRequestedCategory(userText)
        val brandHint = detectBrandFromCatalogOrText(userText, activeCatalog)
        val modelHint = detectModelPhrase(userText)
        val budget = extractBudgetRange(userText)

        if (matched.isNotEmpty()) {
            // Check if all matches are out of stock -> proactively suggest similar in-stock alternatives
            val allOutOfStock = matched.all { !it.inStock }
            if (allOutOfStock) {
                val firstOut = matched.first()
                val alternatives = activeCatalog.filter {
                    it.inStock && it.id != firstOut.id &&
                        (it.category.equals(firstOut.category, true) || it.brand.equals(firstOut.brand, true))
                }.sortedBy { productNumericPrice(it) ?: Double.MAX_VALUE }.take(2)

                val combined = (matched.take(1) + alternatives).distinctBy { it.id }
                sessionHolder.updateFromTurn(userText, brandHint ?: firstOut.brand, modelHint, categoryHint ?: firstOut.category, budget.maxGel, combined)
                return AiShoppingReply(
                    text = "„${firstOut.title}“ ამ ეტაპზე მარაგში ამოწურულია ⏳" +
                        if (alternatives.isNotEmpty()) {
                            "\n\nსამაგიეროდ მარაგში გვაქვს მსგავსი ხელმისაწვდომი ვარიანტები. ასევე შეგიძლიათ ქვემოთ ღილაკით დააზუსტოთ აღნიშნული პროდუქტის მარაგის განახლება."
                        } else {
                            "\n\nქვემოთ ღილაკით შეგიძლიათ გააგზავნოთ მოთხოვნა და გადაგიმოწმებთ მარაგში ხელმისაწვდომობას."
                        },
                    recommendedProducts = combined,
                    allowStockInquiry = true,
                    stockInquiryPrefillQuery = firstOut.title
                )
            }

            sessionHolder.updateFromTurn(userText, brandHint, modelHint, categoryHint, budget.maxGel, matched)

            val wantsDiscount = q.contains("ფასდაკლ") || q.contains("აქცი")
            val header = when {
                wantsDiscount && matched.any { it.hasActiveDiscount() } ->
                    "🔥 **აქტიური ფასდაკლებები BSTORE-ში:**"
                budget.maxGel != null ->
                    "💰 **${budget.maxGel.toInt()} ₾-მდე ბიუჯეტში მოიძებნა:**"
                else ->
                    "🛍️ **BSTORE-ის კატალოგში მოიძებნა:**"
            }

            val body = matched.joinToString("\n\n") { p ->
                val priceDesc = if (p.hasActiveDiscount()) {
                    "**${p.effectiveDisplayPrice}** (ძველი ფასი ~~${p.formattedOriginalPrice}~~, -${p.calculatedDiscountPercent}%)"
                } else {
                    "**${p.effectiveDisplayPrice}**"
                }
                val stockBadge = if (p.inStock) "მარაგშია ✅" else "ამოწურულია ⏳"
                "🔹 **${p.title}** ($stockBadge)\n" +
                    "• ფასი: $priceDesc | კატეგორია: ${p.category}\n" +
                    "• თავსებადობა: ${p.compatibleModels.ifBlank { p.brand }} | გარანტია: ${p.warranty}"
            }

            return AiShoppingReply(
                text = "$header\n\n$body\n\nქვემოთ ბარათიდან შეგიძლიათ ნახოთ დეტალები ან დაამატოთ კალათაში.",
                recommendedProducts = matched
            )
        }

        // 9. Zero match — distinguish genuine product searches from open human conversation
        if (!isGenuineProductSearchIntent(userText, activeCatalog)) {
            return AiShoppingReply(
                text = "მზად ვარ დაგეხმარო! 😊 მომწერე რომელი მოდელისთვის ეძებ პროდუქტს (მაგ. iPhone 16 Pro ეკრანი, Samsung ბატარეა, ყურსასმენები ან როუტერი) ან დამისვი კითხვა ფასებისა და პირობების შესახებ.",
                recommendedProducts = emptyList(),
                allowStockInquiry = false,
                stockInquiryPrefillQuery = ""
            )
        }

        val queryTokens = extractMeaningfulTokens(userText)
        val allCategoryRoots = CATEGORY_SYNONYMS.values.flatten()
        val hasUnknownDeviceTokens = brandHint == null && queryTokens.any { tok ->
            allCategoryRoots.none { kw -> tok.contains(kw) || kw.contains(tok) }
        }
        val budgetPart = budget.maxGel?.let { " ${it.toInt()} ₾-მდე ბიუჯეტში" } ?: ""
        val fallbackAlts = if (!hasUnknownDeviceTokens && (budget.maxGel != null || categoryHint != null || brandHint != null)) {
            activeCatalog.filter { item ->
                val price = productNumericPrice(item)
                val maxBudgetOk = budget.maxGel == null || (price != null && price <= budget.maxGel)
                item.inStock &&
                    maxBudgetOk &&
                    (categoryHint == null || item.category.equals(categoryHint, true)) &&
                    (brandHint == null || item.brand.equals(brandHint, true))
            }.sortedBy { productNumericPrice(it) ?: Double.MAX_VALUE }.take(2)
        } else {
            emptyList()
        }

        val noMatchText = if (fallbackAlts.isNotEmpty()) {
            "თქვენი ზუსტი მოთხოვნით$budgetPart კატალოგში პირდაპირი დამთხვევა ვერ მოიძებნა, მაგრამ შესაძლოა გამოგადგეთ ეს ხელმისაწვდომი ვარიანტები.\n\n" +
                "თუ გსურთ, ქვემოთ ღილაკით გამოგზავნეთ დაზუსტების მოთხოვნა — გადავამოწმებთ არის თუ არა მარაგში 👇"
        } else {
            "ამ მოთხოვნით$budgetPart BSTORE-ის აქტიურ კატალოგში შესაბამისი პროდუქტი ვერ მოიძებნა.\n\n" +
                "შეგიძლიათ დამიზუსტოთ მოდელი/ბიუჯეტი, ან ქვემოთ მოცემული ღილაკით იკითხოთ მარაგშია თუ არა — ჩვენი გუნდი მიიღებს მოთხოვნას და დაგიკავშირდებათ! 📦"
        }

        return AiShoppingReply(
            text = noMatchText,
            recommendedProducts = fallbackAlts,
            allowStockInquiry = true,
            stockInquiryPrefillQuery = userText.trim()
        )
    }
}
