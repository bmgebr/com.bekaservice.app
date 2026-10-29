package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * პროდუქტის სურათების (რეალური URL ბმული, ტელეფონის მეხსიერებიდან/გალერეიდან არჩეული ფოტო,
 * ან აპის მიერ ავტო-გენერირებული ბრენდული ქარდის ვიზუალი) მართვის სისტემა.
 */
object ProductImageManager {
    const val AUTO_GENERATED_IMAGE_MARKER = "auto"

    private val legacyDefaultPlaceholders = setOf(
        "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600",
        "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600"
    )

    private val defaultSoftwareUrlsById: Map<Int, String> by lazy {
        SoftwareCatalogData.getDigitalSoftwareProducts().associate { it.id to it.imageUrl.trim() }
    }

    /**
     * აბრუნებს true-ს, თუ პროდუქტს ადმინის მიერ მიბმული აქვს რეალური ფოტო
     * (ბმულით https://... ან ტელეფონის მეხსიერებიდან file:// / content:// / data:image/),
     * რომელიც უნდა გამოჩნდეს ავტო-გენერირებული ქარდის ნაცვლად.
     */
    fun hasCustomAttachedPhoto(product: ProductItem): Boolean {
        val raw = product.imageUrl.trim()
        if (raw.isBlank() || raw.equals(AUTO_GENERATED_IMAGE_MARKER, ignoreCase = true)) {
            return false
        }

        // ტელეფონის მეხსიერებიდან / გალერეიდან არჩეული ფოტო
        if (isLocalOrEmbeddedImage(raw)) {
            return true
        }

        // საწყისი 24 პროგრამული სერვისისთვის (ID 101..124):
        // თუ imageUrl შეცვლილია საწყისი placeholder-ისგან, ვაჩვენებთ ადმინის მიბმულ ფოტოს.
        if (product.id in 101..124) {
            val defaultUrl = defaultSoftwareUrlsById[product.id]
            return raw != defaultUrl && (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true))
        }

        // ახლად დამატებული პროგრამული სერვისისთვის: თუ ძველი დეფოლტ placeholder-ია და ადმინს არ შეუცვლია
        val isSoftware = product.category == "პროგრამული სერვისები" || product.type == "software"
        if (isSoftware && product.id > 124 && raw in legacyDefaultPlaceholders) {
            return false
        }

        return raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)
    }

    fun isLocalOrEmbeddedImage(imageUrl: String): Boolean {
        val clean = imageUrl.trim()
        return clean.startsWith("file://", ignoreCase = true) ||
            clean.startsWith("content://", ignoreCase = true) ||
            clean.startsWith("/data/", ignoreCase = true) ||
            clean.startsWith("/storage/", ignoreCase = true) ||
            clean.startsWith("data:image/", ignoreCase = true)
    }

    /**
     * აკოპირებს ტელეფონის მეხსიერებიდან (Photo Picker) არჩეულ სურათს აპლიკაციის მუდმივ მეხსიერებაში,
     * რათა აპის გადატვირთვის შემდეგაც მუდმივად ხელმისაწვდომი და სწრაფი იყოს.
     */
    suspend fun copyPickerUriToInternalStorage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "product_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val destFile = File(dir, "prod_img_${System.currentTimeMillis()}.jpg")

            val inputBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return@withContext null

            val decoded = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)
            if (decoded != null) {
                val maxDim = 960
                val width = decoded.width
                val height = decoded.height
                val scaledBitmap = if (width > maxDim || height > maxDim) {
                    val ratio = width.toFloat() / height.toFloat()
                    val targetW = if (ratio >= 1f) maxDim else (maxDim * ratio).toInt().coerceAtLeast(1)
                    val targetH = if (ratio >= 1f) (maxDim / ratio).toInt().coerceAtLeast(1) else maxDim
                    Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                } else {
                    decoded
                }
                FileOutputStream(destFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    out.flush()
                }
            } else {
                FileOutputStream(destFile).use { out ->
                    out.write(inputBytes)
                    out.flush()
                }
            }

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            null
        }
    }

    fun decodeLocalBitmapIfPossible(context: Context, imageUrl: String): Bitmap? {
        val clean = imageUrl.trim()
        return try {
            when {
                clean.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(clean).path ?: clean.removePrefix("file://")
                    BitmapFactory.decodeFile(path)
                }
                clean.startsWith("/data/") || clean.startsWith("/storage/") -> {
                    BitmapFactory.decodeFile(clean)
                }
                clean.startsWith("content://", ignoreCase = true) -> {
                    context.contentResolver.openInputStream(Uri.parse(clean))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                clean.startsWith("data:image/", ignoreCase = true) -> {
                    val base64Part = clean.substringAfter("base64,", "")
                    if (base64Part.isNotBlank()) {
                        val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * უნივერსალური პროდუქტის ვიზუალის კომპონენტი:
 * - თუ პროდუქტს მიბმული აქვს რეალური ფოტო (URL ბმულით ან ტელეფონის მეხსიერებიდან), აჩვენებს ფოტოს;
 * - ხოლო თუ არჩეულია ავტო-გენერირებული რეჟიმი (ან ბმული ცარიელია), აჩვენებს ბრენდულ გენერირებულ ქარდს.
 */
@Composable
fun ProductSmartVisual(
    product: ProductItem,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    forceAutoGenerated: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasCustomPhoto = !forceAutoGenerated && ProductImageManager.hasCustomAttachedPhoto(product)
    val rawUrl = product.imageUrl.trim()

    Box(
        modifier = modifier.background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF0F1115)))
    ) {
        // ფონური/სარეზერვო ავტო-გენერირებული ვიზუალი (ყოველთვის უზრუნველყოფს სავსე ვიზუალს ჩატვირთვის დროსაც)
        SoftwareBrandVisualHeader(
            product = product,
            modifier = Modifier.fillMaxSize(),
            compact = compact,
            ignoreCustomImage = true
        )

        if (hasCustomPhoto) {
            val isLocal = remember(rawUrl) { ProductImageManager.isLocalOrEmbeddedImage(rawUrl) }
            if (isLocal) {
                val localBitmapState = produceState<Bitmap?>(initialValue = null, key1 = rawUrl) {
                    value = withContext(Dispatchers.IO) {
                        ProductImageManager.decodeLocalBitmapIfPossible(context, rawUrl)
                    }
                }
                val bmp = localBitmapState.value
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    AsyncImage(
                        model = rawUrl,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                AsyncImage(
                    model = rawUrl,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * AI-ით პროდუქტის ყველა ველის ავტომატური მოძიება და შევსება დასახელების მიხედვით.
 */
object AdminProductAiGenerator {
    private val candidateModels = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    data class GeneratedProductFields(
        val title: String,
        val price: String,
        val shortDescription: String,
        val description: String,
        val category: String,
        val brand: String,
        val isSoftware: Boolean,
        val subcategory: String,
        val offerType: String,
        val duration: String,
        val accountType: String,
        val features: List<String>,
        val targetAudience: String,
        val activationTerms: String,
        val compatibleModels: String,
        val warranty: String,
        val searchKeywords: String
    )

    suspend fun generateForTitle(
        rawTitle: String,
        existingCategories: List<String>,
        existingBrands: List<String>
    ): GeneratedProductFields = withContext(Dispatchers.IO) {
        val cleanTitle = rawTitle.trim()
        if (cleanTitle.isBlank()) {
            return@withContext buildSmartFallbackFields("ახალი პროდუქტი")
        }

        // 1. პირველ რიგში ვცდილობთ რეალურ Gemini AI გენერაციას
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val aiResult = tryCallGeminiForProduct(cleanTitle, existingCategories, existingBrands, apiKey)
            if (aiResult != null) {
                return@withContext aiResult
            }
        }

        // 2. თუ ინტერნეტი არ არის ან API მიუწვდომელია, ვიყენებთ ინტელექტუალურ ლოკალურ ანალიზატორს
        buildSmartFallbackFields(cleanTitle)
    }

    private fun tryCallGeminiForProduct(
        title: String,
        existingCategories: List<String>,
        existingBrands: List<String>,
        apiKey: String
    ): GeneratedProductFields? {
        val prompt = """
შენ ხარ BEKASERVICE-ის პროდუქტების კატალოგის ექსპერტი და ტექნიკური მენეჯერი.
მომხმარებელმა (ადმინისტრატორმა) შეიყვანა ახალი პროდუქტის ან სერვისის დასახელება: "$title"

მოძებნე რეალური ინფორმაცია ამ პროდუქტის/სერვისის შესახებ და დააბრუნე მხოლოდ სუფთა JSON ობიექტი ქართულ ენაზე შემდეგი ველებით:
{
  "price": "რეალისტური ფასი ლარში, მაგ: 25 ₾ ან 65 ₾ ან 220 ₾",
  "category": "შესაბამისი კატეგორია ქართულად. თუ ერგება არსებულს (${existingCategories.joinToString(", ")}), აირჩიე ის; თუ სხვა ტიპის პროდუქტია (მაგ: აქსესუარები, დამტენები, ყურსასმენები, ლეპტოპის ნაწილები, სტრიმინგი), მიუთითე ახალი შესაბამისი კატეგორია ქართულად",
  "brand": "ზუსტი ბრენდი (მაგ: Adobe, OpenAI, Apple, Samsung, Xiaomi, Netflix, Canva, Microsoft, Sony, Baseus ან სხვა შესაბამისი ბრენდი)",
  "isSoftware": true თუ პროგრამული სერვისია/გამოწერაა/ლიცენზიაა/განბლოკვაა, false თუ ფიზიკური ნაწილი ან მოწყობილობაა,
  "subcategory": "ქვეკატეგორია ქართულად (მაგ: ხელოვნური ინტელექტის სერვისები, ვიდეო, დიზაინი და კონტენტის შექმნა, პროგრამული უზრუნველყოფის ლიცენზიები, მობილურის პროგრამირება და განბლოკვა, Google-ის ანგარიშები და მეხსიერება, ან შესაბამისი ქვეკატეგორია)",
  "shortDescription": "მოკლე, информаციული ქართული აღწერა კატალოგის ბარათისთვის (1-2 წინადადება, 90-170 სიმბოლო)",
  "description": "სრული, დეტალური პროფესიონალური ქართული აღწერა პროდუქტის დეტალური გვერდისთვის (3-5 წინადადება)",
  "offerType": "შეთავაზების ტიპი (მაგ: თვიური გამოწერა, 12-თვიანი ლიცენზია, ორიგინალი სათადარიგო ნაწილი)",
  "duration": "მოქმედების ვადა (მაგ: 1 თვე, 12 თვე, უვადო (Lifetime), ან 6 თვე გარანტია)",
  "accountType": "ანგარიშის ან ლიცენზიის ტიპი (მაგ: პირადი ანგარიში (Private), Retail ლიცენზიის გასაღები)",
  "features": [
    "პირველი მთავარი ფუნქცია ან მახასიათებელი ქართულად",
    "მეორე მთავარი ფუნქცია ან მახასიათებელი ქართულად",
    "მესამე მთავარი ფუნქცია ან მახასიათებელი ქართულად",
    "მეოთხე მთავარი ფუნქცია ან მახასიათებელი ქართულად"
  ],
  "targetAudience": "ვისთვის არის განკუთვნილი ეს პროდუქტი/სერვისი ქართულად",
  "activationTerms": "BEKASERVICE-ის აქტივაციის ან მონტაჟის პირობები და გარანტია ქართულად",
  "compatibleModels": "თავსებადი მოწყობილობები ან პლატფორმები (მაგ: Windows, macOS, iOS, Android ან კონკრეტული ტელეფონის მოდელი)",
  "warranty": "გარანტიის ვადა (მაგ: აქტიური გამოწერის სრული პერიოდი ან 6 თვე)",
  "searchKeywords": "საძიებო საკვანძო სიტყვები ქართულად და ინგლისურად"
}
""".trimIndent()

        for (modelName in candidateModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.35)
                        put("responseMimeType", "application/json")
                    })
                }

                val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respStr = response.body?.string().orEmpty()
                        val root = JSONObject(respStr)
                        val text = root.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text", "")
                            .orEmpty()
                            .trim()
                            .removePrefix("```json")
                            .removePrefix("```")
                            .removeSuffix("```")
                            .trim()

                        if (text.startsWith("{")) {
                            val json = JSONObject(text)
                            val fallback = buildSmartFallbackFields(title)
                            val featuresArr = json.optJSONArray("features")
                            val parsedFeatures = mutableListOf<String>()
                            if (featuresArr != null) {
                                for (i in 0 until featuresArr.length()) {
                                    val f = featuresArr.optString(i, "").trim()
                                    if (f.isNotBlank()) parsedFeatures.add(f)
                                }
                            }
                            return GeneratedProductFields(
                                title = title,
                                price = json.optString("price", fallback.price).ifBlank { fallback.price },
                                shortDescription = json.optString("shortDescription", fallback.shortDescription).ifBlank { fallback.shortDescription },
                                description = json.optString("description", fallback.description).ifBlank { fallback.description },
                                category = json.optString("category", fallback.category).ifBlank { fallback.category },
                                brand = json.optString("brand", fallback.brand).ifBlank { fallback.brand },
                                isSoftware = json.optBoolean("isSoftware", fallback.isSoftware),
                                subcategory = json.optString("subcategory", fallback.subcategory).ifBlank { fallback.subcategory },
                                offerType = json.optString("offerType", fallback.offerType).ifBlank { fallback.offerType },
                                duration = json.optString("duration", fallback.duration).ifBlank { fallback.duration },
                                accountType = json.optString("accountType", fallback.accountType).ifBlank { fallback.accountType },
                                features = if (parsedFeatures.isNotEmpty()) parsedFeatures else fallback.features,
                                targetAudience = json.optString("targetAudience", fallback.targetAudience).ifBlank { fallback.targetAudience },
                                activationTerms = json.optString("activationTerms", fallback.activationTerms).ifBlank { fallback.activationTerms },
                                compatibleModels = json.optString("compatibleModels", fallback.compatibleModels).ifBlank { fallback.compatibleModels },
                                warranty = json.optString("warranty", fallback.warranty).ifBlank { fallback.warranty },
                                searchKeywords = json.optString("searchKeywords", fallback.searchKeywords).ifBlank { fallback.searchKeywords }
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Continue to next model or smart local fallback
            }
        }
        return null
    }

    /**
     * ინტელექტუალური ლოკალური გენერატორი, რომელიც აანალიზებს პროდუქტის დასახელებას
     * და ავსებს ყველა ველს ზუსტი, ქართულენოვანი ტექნიკური მონაცემებით.
     */
    fun buildSmartFallbackFields(rawTitle: String): GeneratedProductFields {
        val title = rawTitle.trim()
        val lower = title.lowercase()

        // ვადის ამოცნობა დასახელებიდან
        val detectedDuration = when {
            lower.contains("18 თვ") || lower.contains("18 month") -> "18 თვე"
            lower.contains("12 თვ") || lower.contains("1 წლ") || lower.contains("წლიურ") || lower.contains("year") || lower.contains("annual") -> "12 თვე"
            lower.contains("6 თვ") || lower.contains("6 month") -> "6 თვე"
            lower.contains("3 თვ") || lower.contains("3 month") -> "3 თვე"
            lower.contains("უვადო") || lower.contains("lifetime") || lower.contains("სამუდამო") -> "უვადო (Lifetime)"
            lower.contains("ყოველთვიურ") || lower.contains("1 თვ") || lower.contains("month") -> "1 თვე"
            else -> "1 თვე"
        }

        // ბრენდის ავტომატური ამოცნობა (მათ შორის Samsung S20-S25, Apple, Xiaomi, Adobe, AI და სხვ.)
        val detectedBrand = when {
            lower.contains("chatgpt") || lower.contains("openai") || lower.contains("gpt") || lower.contains("sora") -> "OpenAI"
            lower.contains("claude") || lower.contains("anthropic") -> "Anthropic"
            lower.contains("gemini") || lower.contains("google ai") || lower.contains("google one") || lower.contains("gmail") || lower.contains("notebooklm") -> "Google"
            lower.contains("pixel") || lower.contains("პიქსელ") -> "Google Pixel"
            lower.contains("grok") || lower.contains("xai") -> "xAI"
            lower.contains("perplexity") -> "Perplexity"
            lower.contains("cursor") -> "Cursor"
            lower.contains("lovable") -> "Lovable"
            lower.contains("midjourney") -> "Midjourney"
            lower.contains("runway") -> "Runway"
            lower.contains("elevenlabs") -> "ElevenLabs"
            lower.contains("suno") -> "Suno AI"
            lower.contains("deepseek") -> "DeepSeek"
            lower.contains("canva") || lower.contains("კანვა") -> "Canva"
            lower.contains("capcut") || lower.contains("კაპკატ") -> "CapCut"
            lower.contains("adobe") || lower.contains("photoshop") || lower.contains("premiere") || lower.contains("acrobat") || lower.contains("illustrator") -> "Adobe"
            lower.contains("figma") -> "Figma"
            lower.contains("youtube") || lower.contains("იუთუბ") -> "YouTube"
            lower.contains("spotify") || lower.contains("სპოტიფაი") -> "Spotify"
            lower.contains("netflix") || lower.contains("ნეტფლიქს") -> "Netflix"
            lower.contains("disney") -> "Disney+"
            lower.contains("telegram") || lower.contains("ტელეგრამ") -> "Telegram"
            lower.contains("discord") -> "Discord"
            lower.contains("playstation") || lower.contains("ps plus") || lower.contains("ps5") -> "PlayStation"
            lower.contains("xbox") -> "Xbox"
            lower.contains("steam") -> "Steam"
            lower.contains("windows") || lower.contains("office") || lower.contains("microsoft") || lower.contains("365") || lower.contains("copilot") -> "Microsoft"
            lower.contains("jetbrains") || lower.contains("intellij") || lower.contains("webstorm") || lower.contains("pycharm") -> "JetBrains"
            lower.contains("autocad") || lower.contains("autodesk") || lower.contains("3ds max") -> "Autodesk"
            lower.contains("kaspersky") -> "Kaspersky"
            lower.contains("nordvpn") || lower.contains("vpn") -> "NordVPN"
            lower.contains("iphone") || lower.contains("აიფონ") || lower.contains("ipad") || lower.contains("macbook") || lower.contains("airpods") || lower.contains("apple") || lower.contains("icloud") || lower.contains("magsafe") -> "Apple"
            lower.contains("samsung") || lower.contains("სამსუნგ") || lower.contains("galaxy") ||
                lower.contains("s25") || lower.contains("s24") || lower.contains("s23") || lower.contains("s22") || lower.contains("s21") || lower.contains("s20") ||
                lower.contains("z fold") || lower.contains("z flip") || lower.contains("a55") || lower.contains("a54") || lower.contains("a35") || lower.contains("a15") -> "Samsung"
            lower.contains("xiaomi") || lower.contains("შაომი") || lower.contains("redmi") || lower.contains("poco") -> "Xiaomi"
            lower.contains("oneplus") -> "OnePlus"
            lower.contains("honor") || lower.contains("ჰონორ") -> "Honor"
            lower.contains("huawei") || lower.contains("ჰუავეი") -> "Huawei"
            lower.contains("motorola") || lower.contains("moto") -> "Motorola"
            lower.contains("baseus") -> "Baseus"
            lower.contains("anker") -> "Anker"
            lower.contains("ugreen") -> "Ugreen"
            lower.contains("sony") -> "Sony"
            lower.contains("jbl") -> "JBL"
            else -> {
                val firstWord = title.split(" ", "-", "/").firstOrNull { it.length >= 2 } ?: "BEKASERVICE"
                firstWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        // განბლოკვის / პროგრამული სერვისის შემოწმება (მათ შორის Carrier Unlock, FRP, iCloud, CPID, EDL)
        val isUnlockService = lower.contains("unlock") || lower.contains("განბლოკვ") || lower.contains("frp") ||
            lower.contains("icloud") || lower.contains("t-mobile") || lower.contains("verizon") || lower.contains("at&t") ||
            lower.contains("carrier") || lower.contains("cpid") || lower.contains("mdm") || lower.contains("knox") ||
            lower.contains("sim lock") || lower.contains("edl") || lower.contains("flashing")

        // 1. მობილურის პროგრამული განბლოკვა / Carrier Unlock
        if (isUnlockService) {
            val detectedModel = when {
                lower.contains("s25 ultra") -> "Samsung Galaxy S25 Ultra"
                lower.contains("s25") -> "Samsung Galaxy S25"
                lower.contains("s24 ultra") -> "Samsung Galaxy S24 Ultra"
                lower.contains("s24") -> "Samsung Galaxy S24"
                lower.contains("s23 ultra") -> "Samsung Galaxy S23 Ultra"
                lower.contains("s23") -> "Samsung Galaxy S23"
                lower.contains("16 pro max") -> "iPhone 16 Pro Max"
                lower.contains("16 pro") -> "iPhone 16 Pro"
                lower.contains("16") -> "iPhone 16"
                lower.contains("15 pro max") -> "iPhone 15 Pro Max"
                lower.contains("15 pro") -> "iPhone 15 Pro"
                lower.contains("15") -> "iPhone 15"
                else -> title.replace("unlock", "", ignoreCase = true)
                    .replace("t-mobile", "", ignoreCase = true)
                    .replace("verizon", "", ignoreCase = true)
                    .replace("at&t", "", ignoreCase = true)
                    .replace("cpid", "", ignoreCase = true)
                    .trim().ifBlank { "$detectedBrand სმარტფონი" }
            }

            val unlockPrice = when {
                lower.contains("s25") -> "389 ₾"
                lower.contains("s24") -> "280 ₾"
                lower.contains("s23") -> "210 ₾"
                lower.contains("16") -> "320 ₾"
                lower.contains("15") -> "240 ₾"
                lower.contains("icloud") -> "95 ₾"
                lower.contains("frp") -> "45 ₾"
                else -> "65 ₾"
            }

            return GeneratedProductFields(
                title = title,
                price = unlockPrice,
                shortDescription = "$title — ოფიციალური ქარხნული განბლოკვა (Factory/Carrier Unlock), SIM შეზღუდვების მოხსნა და სისტემის სრული აღდგენა მუდმივი გარანტიით.",
                description = "$title წარმოადგენს BEKASERVICE-ის ოფიციალურ სერვერულ და USB პროგრამულ მომსახურებას. განბლოკვის შემდეგ ტელეფონი მუშაობს Silknet, Magti და Cellfie ქსელებზე შეუზღუდავად, ინარჩუნებს სისტემურ განახლებებს და არ იბლოკება ქარხნული გადატვირთვის დროს.",
                category = "პროგრამული სერვისები",
                brand = detectedBrand,
                isSoftware = true,
                subcategory = "მობილურის პროგრამირება და განბლოკვა",
                offerType = "დისტანციური USB / Official Unlock",
                duration = "სამუდამო (Permanent Unlock)",
                accountType = "სერვერული CPID / USB Remote Flash",
                features = listOf(
                    "მოწყობილობის მუდმივი (Permanent) ოფიციალური განბლოკვა",
                    "ყველა ქართული და საერთაშორისო SIM ბარათის მხარდაჭერა (Silknet, Magti, Cellfie)",
                    "ქარხნული OTA განახლებებისა და სისტემური ფუნქციების სრული შენარჩუნება",
                    "დისტანციური მომსახურება BEKASERVICE-ის ინჟინრის მიერ"
                ),
                targetAudience = "$detectedModel-ის მფლობელები, რომელთა მოწყობილობაც დაბლოკილია ქსელზე ან ოპერატორზე.",
                activationTerms = "განბლოკვა ხორციელდება დისტანციურად კომპიუტერთან (USB კაბელით) ან ოფიციალური სერვერული მოთხოვნით IMEI-ს მეშვეობით 15-45 წუთში.",
                compatibleModels = detectedModel,
                warranty = "უვადო გარანტია (ქარხნულ გადატვირთვაზე/განახლებაზე არ იბლოკება)",
                searchKeywords = "${title.lowercase()}, ${detectedBrand.lowercase()}, unlock, sim lock, cpid, t-mobile, frp, imei"
            )
        }

        // არის თუ არა აპარატურული ნაწილი ან აქსესუარი
        val isDisplay = lower.contains("ეკრან") || lower.contains("დისპლე") || lower.contains("oled") || lower.contains("amoled") || lower.contains("screen") || lower.contains("display") || lower.contains("lcd")
        val isBattery = lower.contains("ელემენტ") || lower.contains("ბატარე") || lower.contains("battery") || lower.contains("mah")
        val isPort = lower.contains("პორტ") || lower.contains("შლეიფ") || lower.contains("შლიფ") || lower.contains("charging port") || (lower.contains("type-c") && !lower.contains("კაბელ") && !lower.contains("ადაპტერ"))
        val isCamera = lower.contains("კამერ") || lower.contains("camera") || lower.contains("ლინზ") || lower.contains("ობიექტივ")
        val isHousing = lower.contains("კორპუს") || lower.contains("ხუფ") || lower.contains("უკანა მინ") || lower.contains("housing") || lower.contains("back glass")
        val isSpeaker = lower.contains("დინამიკ") || lower.contains("ვიბრო") || lower.contains("taptic") || lower.contains("speaker")
        val isChargerOrCable = lower.contains("დამტენ") || lower.contains("კაბელ") || lower.contains("ადაპტერ") || lower.contains("charger") || lower.contains("magsafe")
        val isCaseOrGlass = lower.contains("ქეის") || lower.contains("ბრონ") || lower.contains("დამცავ") || lower.contains("case")
        val isHeadphones = lower.contains("ყურსასმენ") || lower.contains("airpods") || lower.contains("buds")

        val isHardware = isDisplay || isBattery || isPort || isCamera || isHousing || isSpeaker || isChargerOrCable || isCaseOrGlass || isHeadphones

        if (isHardware) {
            val category = when {
                isDisplay -> "ეკრანები"
                isBattery -> "ელემენტები"
                isPort -> "დამტენის პორტები"
                isCamera -> "კამერები"
                isHousing -> "კორპუსები"
                isSpeaker -> "დინამიკები"
                isChargerOrCable -> "დამტენები და კაბელები"
                isHeadphones -> "ყურსასმენები და აუდიო"
                isCaseOrGlass -> "აქსესუარები"
                else -> "ეკრანები"
            }

            val estimatedPrice = when {
                isDisplay && (lower.contains("16 pro") || lower.contains("s25 ultra") || lower.contains("s24 ultra")) -> "350 ₾"
                isDisplay && (lower.contains("15 pro") || lower.contains("14 pro") || lower.contains("s23 ultra")) -> "270 ₾"
                isDisplay -> "160 ₾"
                isBattery && (lower.contains("15") || lower.contains("16") || lower.contains("s25") || lower.contains("s24")) -> "95 ₾"
                isBattery -> "75 ₾"
                isCamera -> "180 ₾"
                isHousing -> "190 ₾"
                isPort -> "60 ₾"
                isSpeaker -> "50 ₾"
                isHeadphones -> "120 ₾"
                isChargerOrCable -> "45 ₾"
                else -> "80 ₾"
            }

            val shortDesc = when {
                isDisplay -> "ორიგინალი ხარისხის OLED/AMOLED დისპლეი მყისიერი სენსორული რეაგირებით, ფერების ქარხნული კალიბრაციითა და გარანტიით."
                isBattery -> "ახალი ორიგინალი მაღალი ტევადობის ელემენტი 0 ციკლით, 100% რესურსითა და გადახურებისგან დაცვის სისტემით."
                isCamera -> "ორიგინალი კამერის მოდული ოპტიკური სტაბილიზაციით (OIS), ავტოფოკუსითა და მაღალი გარჩევადობით."
                isPort -> "ორიგინალი დამტენის პორტის შლეიფი სწრაფი დამუხტვის, მიკროფონისა და მონაცემთა გადაცემის სრული მხარდაჭერით."
                isHousing -> "ქარხნული სტანდარტის კორპუსი / უკანა მინის პანელი იდეალური მორგებითა და ლაზერული მონტაჟით."
                isChargerOrCable -> "სერტიფიცირებული სწრაფი დამუხტვის აქსესუარი გადახურებისა და ძაბვის ცვალებადობისგან დაცვით."
                else -> "$title — უმაღლესი ხარისხის შემოწმებული პროდუქტი BEKASERVICE-ის ოფიციალური გარანტიით."
            }

            val fullDesc = "$title წარმოადგენს უმაღლესი ხარისხის, ქარხნულად ტესტირებულ კომპონენტს. BEKASERVICE უზრუნველყოფს პროფესიონალურ დიაგნოსტიკას, უსაფრთხო მონტაჟს სპეციალიზებული აპარატურით და სრულ საგარანტიო მხარდაჭერას."

            return GeneratedProductFields(
                title = title,
                price = estimatedPrice,
                shortDescription = shortDesc,
                description = fullDesc,
                category = category,
                brand = detectedBrand,
                isSoftware = false,
                subcategory = category,
                offerType = "ორიგინალი პროდუქტი + სერვისი",
                duration = "6 თვე გარანტია",
                accountType = "",
                features = listOf(
                    "100% ტესტირებული და შემოწმებული ქარხნული ხარისხი",
                    "სწრაფი და უსაფრთხო მონტაჟი ადგილზე 20-30 წუთში",
                    "BEKASERVICE-ის 6-თვიანი ოფიციალური გარანტია"
                ),
                targetAudience = "$detectedBrand მოწყობილობის მფლობელები",
                activationTerms = "მონტაჟი სრულდება BEKASERVICE-ის სერვის-ცენტრში ან საკურიერო მიწოდებით მთელი საქართველოს მასშტაბით.",
                compatibleModels = title.replace("ეკრანი", "").replace("ბატარეა", "").replace("ელემენტი", "").trim().ifBlank { detectedBrand },
                warranty = "6 თვე",
                searchKeywords = "${title.lowercase()}, ${detectedBrand.lowercase()}, ${category.lowercase()}"
            )
        }

        // პროგრამული სერვისის / ციფრული გამოწერის ანალიზი
        val isAcrobatOrPdf = lower.contains("acrobat") || lower.contains("pdf")
        val isAiService = lower.contains("ai") || lower.contains("gpt") || lower.contains("claude") || lower.contains("gemini") ||
            lower.contains("grok") || lower.contains("perplexity") || lower.contains("cursor") || lower.contains("lovable") ||
            lower.contains("midjourney") || lower.contains("runway") || lower.contains("suno") || lower.contains("elevenlabs") || lower.contains("deepseek")
        val isDesignOrMedia = lower.contains("canva") || lower.contains("capcut") || lower.contains("adobe") || lower.contains("photoshop") ||
            lower.contains("premiere") || lower.contains("figma") || lower.contains("youtube") || lower.contains("spotify") ||
            lower.contains("netflix") || lower.contains("disney")
        val isOfficeOrWin = lower.contains("office") || lower.contains("windows") || lower.contains("365") || lower.contains("word") || lower.contains("excel")

        val subcategory = when {
            isAiService -> SoftwareCatalogData.SUBCAT_AI
            isOfficeOrWin -> SoftwareCatalogData.SUBCAT_LICENSES
            isDesignOrMedia || isAcrobatOrPdf -> SoftwareCatalogData.SUBCAT_CREATIVE
            lower.contains("gmail") || lower.contains("google storage") || lower.contains("google one") -> SoftwareCatalogData.SUBCAT_GOOGLE
            else -> SoftwareCatalogData.SUBCAT_LICENSES
        }

        val price = when {
            isAcrobatOrPdf -> "25 ₾"
            lower.contains("claude") -> "39 ₾"
            lower.contains("chatgpt plus") -> "65 ₾"
            lower.contains("chatgpt pro") -> "550 ₾"
            lower.contains("canva") -> "25 ₾"
            lower.contains("capcut") -> "35 ₾"
            lower.contains("youtube") || lower.contains("spotify") || lower.contains("netflix") || lower.contains("telegram") -> "25 ₾"
            isOfficeOrWin -> "45 ₾"
            lower.contains("adobe") -> "65 ₾"
            detectedDuration.contains("12 თვე") -> "120 ₾"
            else -> "35 ₾"
        }

        val shortDesc = when {
            isAcrobatOrPdf -> "$title — პროფესიონალური PDF რედაქტირების, კონვერტაციის (Word/Excel), OCR ტექსტის ამოცნობისა და ელექტრონული ხელმოწერის სრული პრემიუმ პაკეტი."
            lower.contains("claude") -> "$title — უახლესი Claude 3.7 Sonnet მოდელი გაზრდილი ლიმიტებით, Extended Thinking გაღრმავებული აზროვნებითა და კოდის უმაღლესი სიზუსტით."
            isAiService -> "$title — უახლესი თაობის AI მოდელებზე სრული პრემიუმ წვდომა გაზრდილი ლიმიტებით, სწრაფი გენერაციითა და პროფესიონალური ინსტრუმენტებით."
            isOfficeOrWin -> "$title — ოფიციალური საოფისე პაკეტისა და სისტემის ლიცენზია სრული განახლებებით, დისტანციური გააქტიურებითა და გარანტიით."
            isDesignOrMedia -> "$title — ოფიციალური პრემიუმ გამოწერა შეუზღუდავი რესურსებით, რეკლამების გარეშე და უმაღლესი ხარისხის ექსპორტით ყველა მოწყობილობაზე."
            else -> "$title — ოფიციალური პროგრამული სერვისი და პრემიუმ ლიცენზია სრული ფუნქციონალით, სწრაფი აქტივაციითა და გარანტიით."
        }

        val fullDesc = when {
            isAcrobatOrPdf -> "$title გაძლევთ სრულ წვდომას PDF დოკუმენტების რედაქტირების, შექმნის, გაერთიანების, დაცვისა და Word/Excel/PowerPoint ფორმატებში უზუსტესი კონვერტაციის ინსტრუმენტებზე. მოიცავს OCR სკანირებული ტექსტის ამოცნობას, ციფრულ ხელმოწერასა და ღრუბლოვან სინქრონიზაციას კომპიუტერსა და მობილურზე."
            lower.contains("claude") -> "$title წარმოადგენს Anthropic-ის ოფიციალურ პრემიუმ გამოწერას. იძლევა სრულ წვდომას Claude 3.7 Sonnet-ის წამყვან ინტელექტუალურ შესაძლებლობებზე, რთული დოკუმენტებისა და კოდის ბაზების ანალიზზე. BEKASERVICE უზრუნველყოფს სწრაფ აქტივაციასა და სრულ საგარანტიო მხარდაჭერას."
            isOfficeOrWin -> "$title წარმოადგენს Microsoft-ის ოფიციალურ ლიცენზიას, რომელიც მოიცავს ყველა აუცილებელ საოფისე ინსტრუმენტს (Word, Excel, PowerPoint, Outlook) ან Windows 11 Pro-ს ოფიციალურ Retail აქტივაციას. BEKASERVICE-ის ინჟინერი უზრუნველყოფს დისტანციურ ინსტალაციასა და მუდმივ გარანტიას."
            else -> "$title წარმოადგენს ოფიციალურ პრემიუმ პროგრამულ სერვისს, რომელიც უზრუნველყოფს სრულ წვდომას პლატფორმის ყველა პროფესიონალურ ფუნქციაზე. BEKASERVICE უზრუნველყოფს სწრაფ დისტანციურ აქტივაციას, უსაფრთხო კონფიგურაციასა და სრულ ტექნიკურ მხარდაჭერას გამოწერის მთელი პერიოდის განმავლობაში."
        }

        val features = when {
            isAcrobatOrPdf -> listOf(
                "PDF ფაილების სრული ტექსტური და გრაფიკული რედაქტირება",
                "მყისიერი კონვერტაცია PDF ↔ Word, Excel, PowerPoint და სურათებში",
                "OCR ოპტიკური ამოცნობა სკანირებული დოკუმენტებისთვის",
                "დოკუმენტების გაერთიანება, დაყოფა, პაროლით დაცვა და ხელმოწერა"
            )
            lower.contains("claude") -> listOf(
                "სრული წვდომა Claude 3.7 Sonnet-სა და Extended Thinking რეჟიმზე",
                "დიდი მოცულობის დოკუმენტებისა და რთული კოდის სიღრმისეული ანალიზი",
                "Artifacts ინტერაქტიული სამუშაო სივრცე და სწრაფი გენერაცია",
                "მუშაობს ვებ-ბრაუზერში, iOS, Android, Mac და Windows-ზე"
            )
            isOfficeOrWin -> listOf(
                "Word, Excel, PowerPoint, Outlook და OneNote-ის სრული პაკეტი",
                "ოფიციალური ონლაინ აქტივაცია Microsoft-ის სერვერებიდან",
                "ქარხნული უსაფრთხოების განახლებების სრული მხარდაჭერა",
                "BEKASERVICE-ის დისტანციური დახმარება და ოფიციალური გარანტია"
            )
            isAiService -> listOf(
                "სრული წვდომა ფლაგმანურ AI მოდელებსა და სწრაფ რეჟიმზე",
                "ფაილების, დოკუმენტების, კოდისა და სურათების ღრმა ანალიზი",
                "გაზრდილი ყოველდღიური ლიმიტები და პრიორიტეტული სისწრაფე",
                "მუშაობს ვებ-ბრაუზერში, iOS, Android, Windows და macOS-ზე"
            )
            else -> listOf(
                "ყველა პრემიუმ ფუნქციისა და ინსტრუმენტის სრული გააქტიურება",
                "მუშაობს შეუფერხებლად კომპიუტერსა და მობილურ მოწყობილობებზე",
                "სწრაფი დისტანციური ჩართვა 10-15 წუთში",
                "BEKASERVICE-ის სრული გარანტია და ტექნიკური მხარდაჭერა"
            )
        }

        val accountType = when {
            lower.contains("shared") -> "Shared ანგარიში"
            isOfficeOrWin -> "Retail ლიცენზიის გასაღები (Key)"
            else -> "პირადი ანგარიში (Private)"
        }

        return GeneratedProductFields(
            title = title,
            price = price,
            shortDescription = shortDesc,
            description = fullDesc,
            category = "პროგრამული სერვისები",
            brand = detectedBrand,
            isSoftware = true,
            subcategory = subcategory,
            offerType = if (detectedDuration.contains("უვადო")) "უვადო ლიცენზია (Lifetime)" else "პრემიუმ გამოწერა ($detectedDuration)",
            duration = detectedDuration,
            accountType = accountType,
            features = features,
            targetAudience = "სტუდენტები, ოფისის თანამშრომლები, დიზაინერები და პროფესიონალები, რომლებსაც სჭირდებათ $title-ის სრული პრემიუმ შესაძლებლობები.",
            activationTerms = "აქტივაცია სრულდება დისტანციურად 10-15 წუთში BEKASERVICE-ის სრული გარანტიითა და მხარდაჭერით.",
            compatibleModels = if (isOfficeOrWin) "Windows 10/11, macOS" else "Windows, macOS, iOS, Android, Web",
            warranty = "აქტიური გამოწერის სრული პერიოდი",
            searchKeywords = "${title.lowercase()}, ${detectedBrand.lowercase()}, ${subcategory.lowercase()}"
        )
    }
}
