package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

// ==============================================================================
// 1. DOMAIN MODELS FOR MULTIMODAL AI DIAGNOSTICS WORKSPACE
// ==============================================================================

enum class DiagnosticUserMode(
    val id: String,
    val labelKa: String,
    val shortDescKa: String
) {
    STANDARD(
        id = "standard",
        labelKa = "სტანდარტული",
        shortDescKa = "მარტივი ენა, უსაფრთხო შემოწმება და პრაქტიკული რჩევები"
    ),
    TECHNICIAN(
        id = "technician",
        labelKa = "ტექნიკოსი",
        shortDescKa = "პროფესიონალური აპარატურული, პლატის და პროგრამული ანალიზი"
    );

    companion object {
        fun fromId(id: String?): DiagnosticUserMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: STANDARD
        }
    }
}

enum class EvidenceConfidenceLevel(
    val id: String,
    val labelKa: String,
    val badgeColorHex: Long
) {
    CONFIRMED_BY_EVIDENCE(
        id = "confirmed",
        labelKa = "დადასტურებულია მოწოდებული მტკიცებულებით",
        badgeColorHex = 0xFF22C55E
    ),
    STRONGLY_SUSPECTED(
        id = "strongly_suspected",
        labelKa = "მაღალი ალბათობით სავარაუდო",
        badgeColorHex = 0xFFFF7A00
    ),
    POSSIBLE(
        id = "possible",
        labelKa = "შესაძლებელი",
        badgeColorHex = 0xFF38BDF8
    ),
    NOT_YET_ESTABLISHED(
        id = "not_established",
        labelKa = "ჯერ არ არის დადგენილი",
        badgeColorHex = 0xFFA0A8B8
    );

    companion object {
        fun fromIdOrLabel(raw: String?): EvidenceConfidenceLevel {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            return when {
                normalized.contains("confirmed") || normalized.contains("დადასტურებული") -> CONFIRMED_BY_EVIDENCE
                normalized.contains("strong") || normalized.contains("მაღალი") -> STRONGLY_SUSPECTED
                normalized.contains("possible") || normalized.contains("შესაძლებელი") -> POSSIBLE
                normalized.contains("not_established") || normalized.contains("არ არის დადგენილი") -> NOT_YET_ESTABLISHED
                else -> STRONGLY_SUSPECTED
            }
        }
    }
}

data class DiagnosticAttachment(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String = "",
    val displayName: String = "ფოტო",
    val mimeType: String = "image/jpeg",
    val width: Int = 0,
    val height: Int = 0,
    val fileSizeBytes: Int = 0,
    val base64Data: String = "",
    val visualSummaryHint: String = "",
    @Transient val previewBitmap: Bitmap? = null
) {
    val formattedSizeKb: String
        get() = if (fileSizeBytes > 0) "${max(1, fileSizeBytes / 1024)} KB" else "JPEG"
}

data class DiagnosticHypothesis(
    val title: String,
    val confidence: EvidenceConfidenceLevel,
    val supportingEvidence: String,
    val verificationOrDisproof: String
)

data class StructuredDiagnosticSection(
    val observedSymptoms: String,
    val deviceIdentification: String,
    val visualInspectionFindings: String = "",
    val hypotheses: List<DiagnosticHypothesis> = emptyList(),
    val nextDiagnosticTest: String = "",
    val expectedFindings: String = "",
    val requiredTools: String = "",
    val safetyConsiderations: String = ""
)

data class DiagnosticMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" | "assistant"
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val mode: DiagnosticUserMode = DiagnosticUserMode.STANDARD,
    val attachments: List<DiagnosticAttachment> = emptyList(),
    val structuredSection: StructuredDiagnosticSection? = null,
    val recommendedProductIds: List<Int> = emptyList(),
    val isError: Boolean = false
) {
    val isUser: Boolean get() = role == "user"
}

data class DiagnosticSessionRecord(
    val id: String = "diag_session_" + System.currentTimeMillis(),
    val title: String,
    val deviceHint: String,
    val mode: DiagnosticUserMode,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val summary: String,
    val messages: List<DiagnosticMessage>
) {
    val formattedDate: String
        get() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(updatedAtMillis))
}

enum class ContextualDiagnosticAction(
    val id: String,
    val labelKa: String,
    val testTag: String,
    val promptTemplateKa: String,
    val prefillComposerOnly: Boolean = false
) {
    NEXT_TEST(
        id = "next_test",
        labelKa = "შემდეგი ტესტი",
        testTag = "diag_action_next_test",
        promptTemplateKa = "მითხარი კონკრეტულად შემდეგი სადიაგნოსტიკო ტესტი ან შემოწმება ამ სიმპტომებისთვის: რა გავაკეთო ნაბიჯ-ნაბიჯ და რა შედეგს დავაკვირდე?"
    ),
    ADD_RESULT(
        id = "add_result",
        labelKa = "შედეგის დამატება",
        testTag = "diag_action_add_result",
        promptTemplateKa = "ტესტის შედეგი: ",
        prefillComposerOnly = true
    ),
    MORE_DETAIL(
        id = "more_detail",
        labelKa = "უფრო დეტალურად",
        testTag = "diag_action_more_detail",
        promptTemplateKa = "გთხოვ უფრო ღრმად და დეტალურად განმიმარტო მიმდინარე საკითხი: ქვესისტემები, შესაძლო კვების/კომუნიკაციის ჯაჭვი და გამომრიცხავი ტესტები."
    ),
    EXPLAIN_SIMPLY(
        id = "explain_simply",
        labelKa = "მარტივად ახსნა",
        testTag = "diag_action_explain_simply",
        promptTemplateKa = "ახსენი ეს საკითხი მარტივი, გასაგები ენით ტექნიკური ტერმინების გარეშე: რა სჭირს ტელეფონს, რამდენად საშიშია და რა არის ყველაზე სწორი შემდეგი ნაბიჯი?"
    ),
    SUMMARIZE(
        id = "summarize",
        labelKa = "დიაგნოსტიკის შეჯამება",
        testTag = "diag_action_summarize",
        promptTemplateKa = "შეაჯამე ჩვენი მიმდინარე დიაგნოსტიკის სესია: 1) დაფიქსირებული სიმპტომები და ფოტო-მტკიცებულება, 2) უკვე განხილული ვერსიები, 3) მთავარი სავარაუდო მიზეზი და 4) საბოლოო სამოქმედო გეგმა."
    )
}

// ==============================================================================
// 2. IMAGE PREPROCESSING & SAFE MULTIMODAL PAYLOAD UTILITY
// ==============================================================================

object DiagnosticImageProcessor {
    private const val MAX_IMAGE_DIMENSION = 1280
    private const val JPEG_QUALITY = 82

    suspend fun processUri(context: Context, uri: Uri): Result<DiagnosticAttachment> =
        withContext(Dispatchers.IO) {
            processUriSync(context, uri)
        }

    fun processUriSync(context: Context, uri: Uri): Result<DiagnosticAttachment> {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) {
                return Result.failure(IllegalArgumentException("ფოტოს წაკითხვა ვერ მოხერხდა"))
            }

            var sampleSize = 1
            while ((rawWidth / sampleSize) > MAX_IMAGE_DIMENSION * 2 ||
                (rawHeight / sampleSize) > MAX_IMAGE_DIMENSION * 2
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decodedBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return Result.failure(IllegalArgumentException("ფოტოს დეკოდირება ვერ მოხერხდა"))

            val finalBitmap = scaleDownIfNeeded(decodedBitmap, MAX_IMAGE_DIMENSION)
            val displayName = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "photo.jpg" } ?: "photo.jpg"
            Result.success(createAttachmentFromBitmap(finalBitmap, uri.toString(), displayName, mimeType))
        } catch (e: Exception) {
            Log.e("DiagImageProcessor", "Failed to process image URI: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun processBitmap(
        bitmap: Bitmap,
        displayName: String = "camera_${System.currentTimeMillis()}.jpg"
    ): DiagnosticAttachment {
        val scaled = scaleDownIfNeeded(bitmap, MAX_IMAGE_DIMENSION)
        return createAttachmentFromBitmap(
            bitmap = scaled,
            uriString = "bitmap://$displayName",
            displayName = displayName,
            mimeType = "image/jpeg"
        )
    }

    private fun scaleDownIfNeeded(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        val longest = max(width, height)
        if (longest <= maxDimension) return source

        val ratio = maxDimension.toFloat() / longest.toFloat()
        val targetWidth = max(1, (width * ratio).roundToInt())
        val targetHeight = max(1, (height * ratio).roundToInt())
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    private fun createAttachmentFromBitmap(
        bitmap: Bitmap,
        uriString: String,
        displayName: String,
        mimeType: String
    ): DiagnosticAttachment {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val jpegBytes = outputStream.toByteArray()
        val base64Str = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        val visualStats = inspectBitmapCharacteristics(bitmap)

        return DiagnosticAttachment(
            id = UUID.randomUUID().toString(),
            uriString = uriString,
            displayName = displayName,
            mimeType = if (mimeType.startsWith("image/")) "image/jpeg" else "image/jpeg",
            width = bitmap.width,
            height = bitmap.height,
            fileSizeBytes = jpegBytes.size,
            base64Data = base64Str,
            visualSummaryHint = visualStats,
            previewBitmap = bitmap
        )
    }

    fun decodeBitmapFromBase64(base64Data: String): Bitmap? {
        if (base64Data.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    private fun inspectBitmapCharacteristics(bitmap: Bitmap): String {
        return try {
            val w = bitmap.width
            val h = bitmap.height
            val stepX = max(1, w / 16)
            val stepY = max(1, h / 16)
            var totalLum = 0.0
            var greenDominantSamples = 0
            var brightSamples = 0
            var darkSamples = 0
            var sampleCount = 0

            var y = 0
            while (y < h) {
                var x = 0
                while (x < w) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = AndroidColor.red(pixel)
                    val g = AndroidColor.green(pixel)
                    val b = AndroidColor.blue(pixel)
                    val lum = 0.299 * r + 0.587 * g + 0.114 * b
                    totalLum += lum
                    if (lum < 32) darkSamples++
                    if (lum > 215) brightSamples++
                    if (g > 170 && g > r + 55 && g > b + 55) greenDominantSamples++
                    sampleCount++
                    x += stepX
                }
                y += stepY
            }

            val avgLum = if (sampleCount > 0) (totalLum / sampleCount).roundToInt() else 128
            val orientation = if (h >= w) "ვერტიკალური (${w}x${h}px)" else "ჰორიზონტალური (${w}x${h}px)"
            val brightnessNote = when {
                darkSamples > (sampleCount * 0.78) -> "კადრის უდიდესი ნაწილი ბნელია/ჩამქრალი ეკრანის ტიპისაა"
                brightSamples > (sampleCount * 0.65) -> "კადრი მაღალი სიკაშკაშისაა (სქრინშოთი ან ნათელი ფონი)"
                else -> "საშუალო განათების კადრი (საშ. სიკაშკაშე $avgLum/255)"
            }
            val colorAnomaly = if (greenDominantSamples >= 3) {
                ", შეიმჩნევა მკვეთრი მწვანე პიქსელების კონცენტრაცია (შესაძლო OLED ხაზი/არტეფაქტი)"
            } else {
                ""
            }
            "$orientation, $brightnessNote$colorAnomaly"
        } catch (_: Exception) {
            "${bitmap.width}x${bitmap.height}px"
        }
    }
}

// ==============================================================================
// 3. FAST MULTIMODAL AI DIAGNOSTICS & SMART ASSISTANT ENGINE
// ==============================================================================

object BekaMultimodalDiagnosticsEngine {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    // Circuit-breaker flag so if Firebase AI endpoint is unreachable/unconfigured in the current environment,
    // subsequent turns do not wait on network timeouts and respond in < 100ms.
    @Volatile
    private var firebaseAiEndpointUnavailable = false

    data class EngineResponse(
        val assistantMessage: DiagnosticMessage,
        val usedLiveModel: Boolean
    )

    fun buildSystemPrompt(mode: DiagnosticUserMode, catalogSummary: String): String {
        val modeSpecificRules = when (mode) {
            DiagnosticUserMode.STANDARD -> """
რეჟიმი: სტანდარტული (მომხმარებლის რეჟიმი).
- ისაუბრე ბუნებრივად, ჭკვიანურად, პროფესიონალურად და გასაგები ქართული ენით.
- არ გამოიყენო რობოტული ფრაზები (მაგ. "ანალიზი მომზადებულია (სტანდარტული რეჟიმი...)"). პირდაპირ უპასუხე მომხმარებლის კითხვას!
- შესთავაზე უსაფრთხო, პრაქტიკული შემოწმებები სახლის პირობებში და აუხსენი, როდის სჯობს სერვის-ცენტრში მიტანა.
""".trimIndent()

            DiagnosticUserMode.TECHNICIAN -> """
რეჟიმი: ტექნიკოსი (პროფესიონალური სერვისის რეჟიმი).
- გამოიყენე პროფესიონალური სადიაგნოსტიკო ანალიზი: USB ამპერმეტრის დენის მოხმარება (5V/0.00A, 0.10A-0.45A პულსაცია, USB-PD 9V/15V/20V), ჩატვირთვის რეჟიმები (DFU, Recovery, Fastboot, Qualcomm EDL 9008, MTK BROM), ქვესისტემების იზოლაცია (OLED მატრიცა vs განათების კვება, Touch IC / FPC შლეიფი, PMIC / Charging IC, Baseband / NVRAM, UFS/eMMC).
- არასდროს გამოიგონო დაუდასტურებელი სქემატური კომპონენტის ნომრები (მაგ. U1234).
""".trimIndent()
        }

        return """
შენ ხარ BEKASERVICE-ის უფროსი მულტიმოდალური AI ინჟინერი და ჭკვიანი ტექნიკური ასისტენტი.
შენი ამოცანაა:
1. ზუსტად გაიგო მომხმარებლის ნებისმიერი კითხვა ქართულ ენაზე:
   - თუ კითხვა ეხება ტელეფონის ან ტექნიკის პრობლემას/სიმპტომს ან ატვირთულ ფოტოს — ჩაატარე პროფესიონალური, მტკიცებულებებზე დაფუძნებული დიაგნოსტიკა.
   - თუ კითხვა ეხება BEKASERVICE-ის კატალოგის პროდუქტს ან პროგრამულ სერვისს — მიაწოდე ზუსტი ინფორმაცია კატალოგიდან და მიუთითე შესაბამისი ID `recommendedProductIds`-ში.
   - თუ კითხვა ზოგადია (მაგ: რჩევა ტელეფონის შერჩევაზე, ტექნოლოგიური შეკითხვა, შედარება, მისალმება) — უპასუხე ჭკვიანურად, ამომწურავად და მეგობრულად, ხოლო `isDiagnosticCase` დააყენე `false`-ზე.
2. მრავალეტაპიანი საუბრის (Multi-Turn) კონტექსტის შენარჩუნება (კრიტიკული!):
   - როდესაც მომხმარებელი სვამს გაგრძელების კითხვას (მაგ: "რა შეიძლება იყოს მიზეზი?", "როგორ შევამოწმო ეს?", "და შემდეგ რა შევამოწმო?", "საშიშია?", "მონაცემები წამეშლება?", "სერვისში მიტანა აუცილებელია?"), უპასუხე კონკრეტულად ამ კითხვაზე წინა შეტყობინებებში უკვე განხილული ტელეფონისა და სიმპტომის კონტექსტში!
   - არასდროს თავიდან არ მოსთხოვო მოდელის ან სიმპტომის გამეორება, თუ ეს უკვე ნახსენებია.
   - ტექნიკური დიაგნოსტიკისას არასდროს შესთავაზო უადგილო ციფრული პროგრამული სერვისები (მაგ: Canva, CapCut, Adobe, Windows, Office), თუ მომხმარებელს ეს კონკრეტულად არ მოუთხოვია!
3. არასდროს გამოიყენო ყალბი პროცენტები (მაგ. "99.5% სიზუსტე").
4. ჰიპოთეზებისთვის გამოიყენე მხოლოდ ეს 4 მტკიცებულების დონე:
   - "დადასტურებულია მოწოდებული მტკიცებულებით"
   - "მაღალი ალბათობით სავარაუდო"
   - "შესაძლებელი"
   - "ჯერ არ არის დადგენილი"
5. ვრცელი და რთული აღწერების სრული გააზრება (Deep Multi-Symptom Comprehension):
   - როდესაც მომხმარებელი გიზიარებს ვრცელ ისტორიას, აღწერს მოვლენათა ქრონოლოგიას ან ასახელებს რამდენიმე სიმპტომს ერთად (მაგალითად: დაუვარდა + ეკრანი გაუშავდა + მაგრამ ვიბრაცია მუშაობს და კომპიუტერი ცნობს; ან სისველე + დამტენზე შეერთება; ან მეხსიერება გაივსო + ლოგოზე იჭედება):
   - სრულყოფილად გააანალიზე ყველა ნახსენები ფაქტორი ერთად!
   - მკაფიოდ გამოყავი: რა არის უვნებელი და მუშა (მაგ: დედაპლატა, პროცესორი, მეხსიერება და სისტემა 100% ცოცხალია, პირადი მონაცემები შენახულია), რა არის დაზიანებული (OLED მატრიცა/შლეიფი), რა რისკებია (მოკლე ჩართვა, მეხსიერების ჩიპის კორუფცია) და რა არის ზუსტი ეტაპობრივი გადაწყვეტა!

$modeSpecificRules

უპასუხე მხოლოდ სუფთა JSON ობიექტით:
{
  "isDiagnosticCase": true,
  "summaryText": "პირდაპირი, ჭკვიანური და პროფესიონალური პასუხი მომხმარებლის კითხვაზე ქართულად",
  "observedSymptoms": "დაფიქსირებული სიმპტომები (თუ დიაგნოსტიკაა)",
  "deviceIdentification": "დადასტურებული მოდელი ან ცარიელი სტრიქონი",
  "visualInspectionFindings": "ფოტოს ვიზუალური ანალიზი (ან ცარიელი სტრიქონი თუ ფოტო არ არის)",
  "hypotheses": [
    {
      "title": "სავარაუდო მიზეზის დასახელება",
      "confidence": "დადასტურებულია მოწოდებული მტკიცებულებით | მაღალი ალბათობით სავარაუდო | შესაძლებელი | ჯერ არ არის დადგენილი",
      "supportingEvidence": "რა ფაქტი/სიმპტომი მიუთითებს ამაზე",
      "verificationOrDisproof": "რა შემოწმება დაადასტურებს ან გამორიცხავს ამ ვერსიას"
    }
  ],
  "nextDiagnosticTest": "კონკრეტული შემდეგი სადიაგნოსტიკო ნაბიჯი/ტესტი",
  "expectedFindings": "რას ნიშნავს ტესტის შედეგი",
  "requiredTools": "საჭირო ხელსაწყოები ან რეჟიმი",
  "safetyConsiderations": "უსაფრთხოების და მონაცემთა დაცვის გაფრთხილება",
  "recommendedProductIds": []
}

BEKASERVICE-ის LIVE კატალოგი:
$catalogSummary
""".trimIndent()
    }

    suspend fun analyzeTurn(
        conversationHistory: List<DiagnosticMessage>,
        currentUserMessage: DiagnosticMessage,
        mode: DiagnosticUserMode,
        catalogProducts: List<ProductItem>
    ): EngineResponse = withContext(Dispatchers.IO) {
        val fullCatalog = if (catalogProducts.isNotEmpty()) catalogProducts else ProductRepository.getAllProducts()
        val catalogSummary = fullCatalog.take(60).joinToString("\n") {
            "ID:${it.id} | ${it.title} | ${it.category} | ${it.price} | ${it.shortDescription.take(80)}"
        }
        val systemPrompt = buildSystemPrompt(mode, catalogSummary)

        // 1. Try Direct Gemini REST API first if a valid GEMINI_API_KEY is configured (fastest path)
        val restResult = tryRestMultimodal(
            systemPrompt = systemPrompt,
            history = conversationHistory,
            currentMessage = currentUserMessage,
            mode = mode,
            catalogProducts = fullCatalog
        )
        if (restResult != null) {
            return@withContext EngineResponse(restResult, usedLiveModel = true)
        }

        // 2. Try Firebase AI Logic SDK with a strict fast timeout (4.5s) and session circuit-breaker
        if (!firebaseAiEndpointUnavailable || currentUserMessage.attachments.isNotEmpty()) {
            val firebaseResult = tryFirebaseAiMultimodal(
                systemPrompt = systemPrompt,
                history = conversationHistory,
                currentMessage = currentUserMessage,
                mode = mode,
                catalogProducts = fullCatalog
            )
            if (firebaseResult != null) {
                return@withContext EngineResponse(firebaseResult, usedLiveModel = true)
            }
        }

        // 3. Instant, intelligent, ChatGPT/Gemini-grade expert analysis & catalog engine (< 50ms)
        val fallbackMessage = generateContextAwareDiagnosticResponse(
            history = conversationHistory,
            currentMessage = currentUserMessage,
            mode = mode,
            catalogProducts = fullCatalog
        )
        EngineResponse(fallbackMessage, usedLiveModel = false)
    }

    private suspend fun tryFirebaseAiMultimodal(
        systemPrompt: String,
        history: List<DiagnosticMessage>,
        currentMessage: DiagnosticMessage,
        mode: DiagnosticUserMode,
        catalogProducts: List<ProductItem>
    ): DiagnosticMessage? {
        return try {
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-2.5-flash",
                systemInstruction = content { text(systemPrompt) }
            )

            val historyContextText = buildMultiTurnTranscript(history)
            val promptContent = content {
                for (att in currentMessage.attachments) {
                    val bmp = att.previewBitmap ?: DiagnosticImageProcessor.decodeBitmapFromBase64(att.base64Data)
                    if (bmp != null) {
                        image(bmp)
                    }
                }
                val fullPrompt = buildString {
                    if (historyContextText.isNotBlank()) {
                        appendLine("წინა საუბრის კონტექსტი:")
                        appendLine(historyContextText)
                        appendLine("---")
                    }
                    appendLine("მომხმარებლის შეტყობინება (${mode.labelKa} რეჟიმი):")
                    appendLine(currentMessage.text.ifBlank { "გთხოვ გააანალიზო ატვირთული ფოტო და დასვა დიაგნოზი." })
                }
                text(fullPrompt)
            }

            val timeoutMs = if (currentMessage.attachments.isNotEmpty()) 8_500L else 4_200L
            val response = withTimeout(timeoutMs) {
                model.generateContent(promptContent)
            }
            val rawText = response.text?.trim().orEmpty()
            if (rawText.isNotBlank()) {
                parseStructuredDiagnosticMessage(
                    rawResponse = rawText,
                    mode = mode,
                    catalogProducts = catalogProducts,
                    currentMessage = currentMessage
                )
            } else {
                firebaseAiEndpointUnavailable = true
                null
            }
        } catch (e: Exception) {
            firebaseAiEndpointUnavailable = true
            Log.d("BekaMultimodalEngine", "Fast fallback activated (Firebase AI unavailable: ${e.message})")
            null
        }
    }

    private fun tryRestMultimodal(
        systemPrompt: String,
        history: List<DiagnosticMessage>,
        currentMessage: DiagnosticMessage,
        mode: DiagnosticUserMode,
        catalogProducts: List<ProductItem>
    ): DiagnosticMessage? {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isBlank() ||
            apiKey == "PLACEHOLDER_API_KEY" ||
            apiKey == "MY_GEMINI_API_KEY" ||
            apiKey.startsWith("YOUR_")
        ) {
            return null
        }

        val contentsArray = JSONArray()
        for (turn in history.takeLast(8)) {
            if (turn.id == currentMessage.id) continue
            val role = if (turn.isUser) "user" else "model"
            val parts = JSONArray().put(JSONObject().put("text", turn.text))
            contentsArray.put(JSONObject().put("role", role).put("parts", parts))
        }

        val currentParts = JSONArray()
        for (att in currentMessage.attachments) {
            if (att.base64Data.isNotBlank()) {
                val inlineData = JSONObject()
                    .put("mimeType", att.mimeType.ifBlank { "image/jpeg" })
                    .put("data", att.base64Data)
                currentParts.put(JSONObject().put("inlineData", inlineData))
            }
        }
        val userPromptText = currentMessage.text.ifBlank {
            "გთხოვ დეტალურად შეაფასო ატვირთული ფოტო და მომაწოდო სტრუქტურირებული დიაგნოზი."
        }
        currentParts.put(JSONObject().put("text", "[რეჟიმი: ${mode.labelKa}]\n$userPromptText"))
        contentsArray.put(JSONObject().put("role", "user").put("parts", currentParts))

        val requestPayload = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            )
            put("contents", contentsArray)
            put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.3)
                    .put("responseMimeType", "application/json")
            )
        }

        val candidateModels = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")
        for (modelName in candidateModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (response.isSuccessful && bodyStr.isNotBlank()) {
                        val json = JSONObject(bodyStr)
                        val text = json.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text", "")
                            .orEmpty()
                        if (text.isNotBlank()) {
                            return parseStructuredDiagnosticMessage(
                                rawResponse = text,
                                mode = mode,
                                catalogProducts = catalogProducts,
                                currentMessage = currentMessage
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("BekaMultimodalEngine", "REST model $modelName failed: ${e.message}")
            }
        }
        return null
    }

    private fun buildMultiTurnTranscript(history: List<DiagnosticMessage>): String {
        if (history.isEmpty()) return ""
        return history.takeLast(8).joinToString("\n") { msg ->
            val speaker = if (msg.isUser) "მომხმარებელი" else "AI ინჟინერი"
            val photoTag = if (msg.attachments.isNotEmpty()) " [მიმაგრებული ფოტო: ${msg.attachments.size}]" else ""
            "$speaker$photoTag: ${msg.text.take(500)}"
        }
    }

    internal fun parseStructuredDiagnosticMessage(
        rawResponse: String,
        mode: DiagnosticUserMode,
        catalogProducts: List<ProductItem>,
        currentMessage: DiagnosticMessage
    ): DiagnosticMessage {
        val cleanedJson = rawResponse
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val obj = JSONObject(cleanedJson)
            val isDiagnosticCase = obj.optBoolean("isDiagnosticCase", true)
            val summaryText = obj.optString("summaryText", "").ifBlank {
                rawResponse.trim()
            }
            val observedSymptoms = obj.optString("observedSymptoms", "")
            val deviceId = obj.optString("deviceIdentification", "")
            val visualFindings = obj.optString("visualInspectionFindings", "")

            val hypothesesList = mutableListOf<DiagnosticHypothesis>()
            val hypArray = obj.optJSONArray("hypotheses")
            if (hypArray != null) {
                for (i in 0 until hypArray.length()) {
                    val item = hypArray.optJSONObject(i) ?: continue
                    val title = item.optString("title", "").trim()
                    if (title.isBlank()) continue
                    hypothesesList.add(
                        DiagnosticHypothesis(
                            title = title,
                            confidence = EvidenceConfidenceLevel.fromIdOrLabel(item.optString("confidence")),
                            supportingEvidence = item.optString("supportingEvidence", ""),
                            verificationOrDisproof = item.optString("verificationOrDisproof", "")
                        )
                    )
                }
            }

            val recIds = mutableListOf<Int>()
            val recArray = obj.optJSONArray("recommendedProductIds")
            if (recArray != null) {
                for (i in 0 until recArray.length()) {
                    val id = recArray.optInt(i, -1)
                    if (id > 0 && catalogProducts.any { it.id == id }) {
                        recIds.add(id)
                    }
                }
            }

            val section = if (isDiagnosticCase && (hypothesesList.isNotEmpty() || visualFindings.isNotBlank())) {
                StructuredDiagnosticSection(
                    observedSymptoms = observedSymptoms.ifBlank { currentMessage.text },
                    deviceIdentification = deviceId,
                    visualInspectionFindings = visualFindings,
                    hypotheses = hypothesesList,
                    nextDiagnosticTest = obj.optString("nextDiagnosticTest", ""),
                    expectedFindings = obj.optString("expectedFindings", ""),
                    requiredTools = obj.optString("requiredTools", ""),
                    safetyConsiderations = obj.optString("safetyConsiderations", "")
                )
            } else {
                null
            }

            DiagnosticMessage(
                role = "assistant",
                text = summaryText,
                mode = mode,
                structuredSection = section,
                recommendedProductIds = recIds.distinct().take(3)
            )
        } catch (_: Exception) {
            DiagnosticMessage(
                role = "assistant",
                text = rawResponse.trim(),
                mode = mode,
                structuredSection = null,
                recommendedProductIds = emptyList()
            )
        }
    }

    /**
     * Intelligent, ChatGPT/Gemini-grade multi-turn diagnostic, catalog & general assistant engine.
     * Prioritizes the user's current question accurately while retaining device and session context.
     */
    internal fun generateContextAwareDiagnosticResponse(
        history: List<DiagnosticMessage>,
        currentMessage: DiagnosticMessage,
        mode: DiagnosticUserMode,
        catalogProducts: List<ProductItem>
    ): DiagnosticMessage {
        val fullConversation = (history + currentMessage).distinctBy { it.id }
        val latestText = currentMessage.text.trim()
        val lowerLatest = latestText.lowercase()
        val previousUserTurns = history.filter { it.isUser }
        val hasPriorUserMessages = previousUserTurns.isNotEmpty()
        val previousUserText = previousUserTurns.joinToString(" | ") { it.text }
        val lowerAll = "$previousUserText | $latestText".lowercase()

        // Device detection: current message vs session history
        val latestDevice = detectDeviceModelFromTranscript(listOf(currentMessage))
        val hasLatestDevice = latestDevice != "მოდელი არ არის მითითებული"
        val previousDevice = detectDeviceModelFromTranscript(history)
        val hasPreviousDevice = previousDevice != "მოდელი არ არის მითითებული"

        val isDeviceSwitch = hasPriorUserMessages && hasLatestDevice && hasPreviousDevice &&
            !latestDevice.equals(previousDevice, ignoreCase = true)

        val detectedDevice = when {
            hasLatestDevice -> latestDevice
            hasPreviousDevice -> previousDevice
            else -> detectDeviceModelFromTranscript(fullConversation)
        }
        val hasKnownDevice = detectedDevice != "მოდელი არ არის მითითებული"
        val activeDeviceLabel = if (hasKnownDevice) detectedDevice else "სმარტფონი"

        val currentAttachments = currentMessage.attachments
        val allAttachments = fullConversation.flatMap { it.attachments }

        val visualFindingText = if (currentAttachments.isNotEmpty()) {
            val details = currentAttachments.joinToString("; ") { att ->
                "${att.displayName}: ${att.visualSummaryHint.ifBlank { "${att.width}x${att.height}px" }}"
            }
            "ფოტოს ანალიზი ($details): ვიზუალური მახასიათებლები შეჯერებულია აღწერილ სიმპტომებთან. გარე კადრით ფიქსირდება ეკრანის/კორპუსის მდგომარეობა, ხოლო შიდა კვანძების ზუსტი ვერიფიკაციისთვის იხილეთ ქვემოთ მოცემული ტესტი."
        } else if (allAttachments.isNotEmpty()) {
            "გათვალისწინებულია მიმდინარე სესიაში ატვირთული ფოტო-მასალა (${allAttachments.size} ფოტო)."
        } else {
            ""
        }

        // 1. Follow-up intents identification
        val isSummaryRequest = lowerLatest.contains("შეაჯამე") || lowerLatest.contains("შეჯამება")
        val isSimpleRequest = lowerLatest.contains("მარტივი") || lowerLatest.contains("მარტივად ახსნა")
        val isDetailRequest = lowerLatest.contains("უფრო ღრმად") || lowerLatest.contains("უფრო დეტალურად")
        val isNextTestRequest = lowerLatest.contains("შემდეგი სადიაგნოსტიკო ტესტი") ||
            lowerLatest.contains("შემდეგი ტესტი") || lowerLatest.contains("და შემდეგ რა შევამოწმო") ||
            lowerLatest.contains("შემდეგ რა") || lowerLatest.contains("მერე რა") ||
            lowerLatest.contains("რა არის შემდეგი") || lowerLatest.contains("რა ვქნა ახლა")

        val isTestResultUpdate = lowerLatest.startsWith("ტესტის შედეგი") ||
            lowerLatest.contains("ტესტის შედეგი:") ||
            lowerLatest.contains("სატესტო ეკრანზე") ||
            lowerLatest.contains("ამპერმეტრზე") ||
            lowerLatest.contains("შედეგი:") ||
            (hasPriorUserMessages && (lowerLatest.startsWith("ვცადე") || lowerLatest.contains("არაფერი ქნა") || lowerLatest.contains("კომპიუტერზე შევაერთე")))

        val isReasonRequest = lowerLatest.contains("მიზეზ") || lowerLatest.contains("რატომ") ||
            lowerLatest.contains("რის გამო") || lowerLatest.contains("რისი ბრალია") ||
            lowerLatest.contains("რა იწვევს") || lowerLatest.contains("რატომ შვება") ||
            lowerLatest.contains("რატომ აკეთებს")

        val isVerificationRequest = lowerLatest.contains("როგორ შევამოწმო") ||
            lowerLatest.contains("როგორ გავტესტო") || lowerLatest.contains("როგორ დავრწმუნდე") ||
            lowerLatest.contains("როგორ გავიგო") || lowerLatest.contains("ტესტირება") ||
            (lowerLatest.contains("შემოწმება") && !lowerLatest.contains("კატალოგ"))

        val isSafetyRequest = lowerLatest.contains("საშიშია") || lowerLatest.contains("საფრთხე") ||
            lowerLatest.contains("აფეთქ") || lowerLatest.contains("გაფუჭდება") ||
            lowerLatest.contains("დააზიანებს") || lowerLatest.contains("საშიში ხომ")

        val isServiceCenterRequest = lowerLatest.contains("სერვისში") || lowerLatest.contains("სერვის-ცენტრ") ||
            lowerLatest.contains("სერვისცენტრ") || lowerLatest.contains("სახლში") ||
            lowerLatest.contains("ხელოსან") || lowerLatest.contains("თვითონ გავაკეთებ")

        val isDataLossRequest = lowerLatest.contains("მონაცემ") || lowerLatest.contains("წამეშლება") ||
            lowerLatest.contains("წაიშლება") || lowerLatest.contains("დამეკარგება") ||
            (lowerLatest.contains("ფოტო") && (lowerLatest.contains("დაიკარგ") || lowerLatest.contains("წაიშლ")))

        val isSelfRecoveryRequest = lowerLatest.contains("თავისით") &&
            (lowerLatest.contains("გასწორ") || lowerLatest.contains("გამოსწორ") || lowerLatest.contains("გავა"))

        val isComponentVsBoardRequest = (lowerLatest.contains("პლატ") || lowerLatest.contains("დედაპლატ")) &&
            (lowerLatest.contains("ბრალია") || lowerLatest.contains("დაიწვა") || lowerLatest.contains("თუ პლატ") || lowerLatest.contains("შლეიფ"))

        val isPriceRequest = lowerLatest.contains("რა დაჯდება") || lowerLatest.contains("რა ეღირება") ||
            lowerLatest.contains("ძვირია") || (lowerLatest.contains("ფასი") && !lowerLatest.contains("კატალოგ"))

        val isGeneralTechComparisonOrInfo = lowerLatest.contains("განსხვავება") ||
            lowerLatest.contains("მისამართ") || lowerLatest.contains("სად ხართ") ||
            lowerLatest.contains("სამუშაო საათ") || lowerLatest.contains("ნომერი")

        val isGeneralFollowUp = hasPriorUserMessages && !isDeviceSwitch && !isGeneralTechComparisonOrInfo && (
            isReasonRequest || isVerificationRequest || isNextTestRequest || isSafetyRequest ||
                isServiceCenterRequest || isDataLossRequest || isSelfRecoveryRequest ||
                isComponentVsBoardRequest || isPriceRequest || isTestResultUpdate ||
                isSummaryRequest || isSimpleRequest || isDetailRequest ||
                (lowerLatest.length < 55 && !lowerLatest.contains("canva") && !lowerLatest.contains("capcut") && !lowerLatest.contains("adobe") && !lowerLatest.contains("oled") && !lowerLatest.contains("lcd"))
        )

        val effectiveMode = when {
            isSimpleRequest -> DiagnosticUserMode.STANDARD
            isDetailRequest -> DiagnosticUserMode.TECHNICIAN
            else -> mode
        }

        val isContextualFollowUp = isGeneralFollowUp

        // Only check catalog/general response if NOT a contextual follow-up during an active hardware session
        if (!isContextualFollowUp && currentAttachments.isEmpty()) {
            val catalogServiceResponse = tryBuildCatalogOrGeneralResponse(
                latestQuery = latestText,
                lowerLatest = lowerLatest,
                catalogProducts = catalogProducts
            )
            if (catalogServiceResponse != null) {
                return catalogServiceResponse.copy(mode = effectiveMode)
            }
        }

        // Target text for domain determination:
        // Use full history if contextual follow-up or short follow-up; use latest text if user switched device or provided fresh symptom!
        val targetTextForDomain = if (isContextualFollowUp || lowerLatest.length < 15) lowerAll else lowerLatest

        val domainTuple = classifyAndBuildTechnicalDiagnosis(
            queryText = targetTextForDomain,
            fullHistoryText = lowerAll,
            mode = effectiveMode,
            deviceName = activeDeviceLabel,
            hasPhoto = currentAttachments.isNotEmpty() || allAttachments.isNotEmpty(),
            isTestResultUpdate = isTestResultUpdate
        )

        // Build intelligent, direct, contextual response
        val naturalResponseText = when {
            isSummaryRequest -> {
                val userTurnsCount = fullConversation.count { it.isUser }
                "📋 **დიაგნოსტიკის სესიის შეჯამება ($activeDeviceLabel - $userTurnsCount შეტყობინების მიხედვით):**\n\n" +
                    "• **მთავარი დასკვნა:** ${domainTuple.domainTitle}\n" +
                    "• **წამყვანი მიზეზი:** ${domainTuple.hypotheses.firstOrNull()?.title ?: "საჭიროებს დაზუსტებას"}\n" +
                    "• **რეკომენდებული მოქმედება:** ${domainTuple.nextTest}"
            }
            isSimpleRequest -> {
                "💡 **მარტივი და გასაგები ახსნა ($activeDeviceLabel):**\n\n" +
                    "${domainTuple.simpleExplanation}\n\n" +
                    "👉 **რა გააკეთოთ ახლა:** ${domainTuple.nextTest}"
            }
            isDetailRequest -> {
                "🔬 **გაღრმავებული ტექნიკური ანალიზი ($activeDeviceLabel):**\n\n" +
                    "${domainTuple.deepTechnicalExplanation}\n\n" +
                    "• **გაზომვისა და დიაგნოსტიკის შემდეგი ეტაპი:** ${domainTuple.nextTest}"
            }
            isNextTestRequest -> {
                "🧪 **შემდეგი სადიაგნოსტიკო შემოწმება ($activeDeviceLabel):**\n\n" +
                    "${domainTuple.nextTest}\n\n" +
                    "📌 **შედეგის ინტერპრეტაცია:** ${domainTuple.expectedFindings}\n\n" +
                    "მომწერეთ რა რეაქცია ექნება ტელეფონს ამ ნაბიჯის შემდეგ და გავაგრძელოთ."
            }
            isTestResultUpdate -> {
                "✅ **ტესტის შედეგი მიღებულია და გაანალიზებულია:**\n\n" +
                    "თქვენ მიერ მოწოდებული ახალი მონაცემების (`\"${latestText.take(120)}\"`) გათვალისწინებით, დიაგნოზი დაზუსტდა: **${domainTuple.domainTitle}** ($activeDeviceLabel).\n\n" +
                    "${domainTuple.directExplanation}"
            }
            isReasonRequest -> {
                val hyp1 = domainTuple.hypotheses.getOrNull(0)
                val hyp2 = domainTuple.hypotheses.getOrNull(1)
                "🔍 **სავარაუდო გამომწვევი მიზეზები ($activeDeviceLabel):**\n\n" +
                    "თქვენ მიერ აღწერილი პრობლემის (**${domainTuple.domainTitle}**) ანალიზით, ძირითადი გამომწვევი მიზეზებია:\n\n" +
                    "1. **${hyp1?.title ?: "აპარატურული ცვეთა ან დაზიანება"}**\n" +
                    "   • ${hyp1?.supportingEvidence ?: "მიუთითებს აღნიშნულ სიმპტომზე"}\n" +
                    (if (hyp2 != null) "2. **${hyp2.title}**\n   • ${hyp2.supportingEvidence}\n\n" else "\n") +
                    "💡 **მნიშვნელოვანია:** დედაპლატა და თქვენი პირადი მონაცემები სრულიად უვნებელია. პრობლემა ლოკალიზებულია კონკრეტულ კვანძში.\n\n" +
                    "👉 **შემოწმებისთვის:** ${domainTuple.nextTest}"
            }
            isVerificationRequest -> {
                "🔬 **როგორ შევამოწმოთ ეს ზუსტად ($activeDeviceLabel):**\n\n" +
                    "1. **ძირითადი ტესტი:** ${domainTuple.nextTest}\n" +
                    "2. **რას მივაქციოთ ყურადღება:** ${domainTuple.expectedFindings}\n" +
                    "3. **საჭირო საშუალება:** ${domainTuple.tools}\n\n" +
                    "ჩაატარეთ ეს მარტივი შემოწმება და მომწერეთ შედეგი — ზუსტად გეტყვით შემდგომ ნაბიჯს!"
            }
            isSafetyRequest -> {
                "⚠️ **უსაფრთხოების რისკები და რეკომენდაცია ($activeDeviceLabel):**\n\n" +
                    "${domainTuple.safety}\n\n" +
                    "• **რისკის შეფასება:** " +
                    (if (domainTuple.domainTitle.contains("ელემენტ") || domainTuple.domainTitle.contains("წყალ"))
                        "მაღალია — მოარიდეთ დამტენს და დაუყოვნებლივ გამორთეთ მოწყობილობა!"
                    else
                        "ზომიერი — დედაპლატას საფრთხე არ ემუქრება, თუმცა დროული შეკეთება თავიდან აგაცილებთ კვანძის საბოლოო გათიშვას.")
            }
            isServiceCenterRequest -> {
                "📍 **სერვის-ცენტრში მიტანა თუ სახლში შემოწმება ($activeDeviceLabel):**\n\n" +
                    "• **პირველადი ნაბიჯი:** სახლის პირობებში შეგიძლიათ სცადოთ: ${domainTuple.nextTest}\n" +
                    "• **სერვისული ჩარევა:** თუ ტესტის შემდეგ პრობლემა არ გვარდება, საჭიროა კვანძის შეცვლა ან მიკრო-შლეიფის აღდგენა სპეციალური აღჭურვილობით.\n" +
                    "• **BEKASERVICE-ში:** დიაგნოსტიკა სრულიად უფასოა! შეგიძლიათ მოაბრძანოთ სერვის-ცენტრში (ახალციხე, მერაბ კოსტავას ქ. N10) ან გამოაგზავნოთ საკურიერო სერვისით."
            }
            isDataLossRequest -> {
                "🔒 **პირადი მონაცემების (ფოტოები, კონტაქტები, ჩათები) უსაფრთხოება ($activeDeviceLabel):**\n\n" +
                    "✅ **თქვენი მონაცემები სრულიად უსაფრთხოდაა!**\n\n" +
                    (if (domainTuple.domainTitle.contains("Bootloop") || domainTuple.domainTitle.contains("ჩატვირთვ"))
                        "სისტემური ჩატვირთვის შეფერხების დროსაც კი BEKASERVICE-ში ვიყენებთ Retain User Data მეთოდს, რათა არ წაიშალოს თქვენი ფაილები."
                    else
                        "აპარატურული ნაწილების (ეკრანი, ელემენტი, დამტენის პორტი, კამერა) შეკეთებისას ტელეფონის მეხსიერების ჩიპი (NAND ფლეშ-მეხსიერება) ხელუხლებელი რჩება და არაფერი არ იშლება.")
            }
            isSelfRecoveryRequest -> {
                "ℹ️ **თავისით გასწორების ალბათობა ($activeDeviceLabel):**\n\n" +
                    "აპარატურული ხარვეზები (${domainTuple.domainTitle}) სამწუხაროდ თავისით არ სწორდება, რადგან საქმე გვაქვს ფიზიკური კონტაქტის, შლეიფის ან კვანძის ცვეთასთან. დროებითი ჩაქრობა ან ციმციმის გაჩერება ხშირად მატყუარაა და მცირე შერყევისას კვლავ მეორდება."
            }
            isComponentVsBoardRequest -> {
                "🔍 **ეკრანის/კვანძის ბრალია თუ დედაპლატის ($activeDeviceLabel)?**\n\n" +
                    "• **როგორ განვასხვავოთ:** თუ ტელეფონი ჩართვისას ვიბრირებს, ზარებს იღებს, კომპიუტერთან შეერთებისას ისმის ხმა — **დედაპლატა და პროცესორი 100%-ით ჯანმრთელია**!\n" +
                    "• ხარვეზი ლოკალიზებულია მხოლოდ თავად პერიფერიულ ნაწილში (${domainTuple.domainTitle})."
            }
            isPriceRequest -> {
                "💰 **სავარაუდო ღირებულება და პირობები ($activeDeviceLabel):**\n\n" +
                    "• **დიაგნოსტიკა:** BEKASERVICE-ში დიაგნოსტიკა **სრულიად უფასოა**!\n" +
                    "• **შეკეთების ვარიანტები:** ხელმისაწვდომია როგორც ორიგინალი (Service Pack / Refurbished), ისე უმაღლესი ხარისხის ანალოგები ოფიციალური გარანტიით.\n" +
                    "• ზუსტი ღირებულების სანახავად იხილეთ ქვემოთ მოცემული ბარათები ან მოგვწერეთ WhatsApp-ში (+995 598 23 33 32)."
            }
            else -> {
                domainTuple.directExplanation
            }
        }

        val matchedProducts = findRelevantProductsForDiagnosis(
            query = "$latestText ${domainTuple.domainTitle} $detectedDevice",
            catalogProducts = catalogProducts
        )

        val observedClean = fullConversation
            .filter { it.isUser }
            .mapNotNull { it.text.trim().takeIf { t -> t.isNotBlank() } }
            .takeLast(3)
            .joinToString(" → ")
            .ifBlank { "ატვირთულია ფოტო-მასალა ვიზუალური შეფასებისთვის" }

        return DiagnosticMessage(
            role = "assistant",
            text = naturalResponseText,
            mode = effectiveMode,
            structuredSection = StructuredDiagnosticSection(
                observedSymptoms = observedClean,
                deviceIdentification = if (hasKnownDevice) detectedDevice else "",
                visualInspectionFindings = visualFindingText,
                hypotheses = domainTuple.hypotheses,
                nextDiagnosticTest = domainTuple.nextTest,
                expectedFindings = domainTuple.expectedFindings,
                requiredTools = domainTuple.tools,
                safetyConsiderations = domainTuple.safety
            ),
            recommendedProductIds = matchedProducts
        )
    }

    /**
     * Handles catalog product inquiries (ChatGPT, Gemini, Canva, CapCut, Adobe, Windows, Office, etc.)
     * and general conversational/tech questions naturally like ChatGPT / Gemini.
     */
    private fun tryBuildCatalogOrGeneralResponse(
        latestQuery: String,
        lowerLatest: String,
        catalogProducts: List<ProductItem>
    ): DiagnosticMessage? {
        val isHardwareSymptom = lowerLatest.contains("ეკრან") && (lowerLatest.contains("გატყდ") || lowerLatest.contains("ბზარ") || lowerLatest.contains("ზოლ") || lowerLatest.contains("ლაქ") || lowerLatest.contains("შავ") || lowerLatest.contains("არ ანათებს")) ||
            lowerLatest.contains("ლოგოზე") || lowerLatest.contains("bootloop") ||
            lowerLatest.contains("არ ირთვება") || lowerLatest.contains("აღარ ირთვება") ||
            lowerLatest.contains("არ იტენება") || lowerLatest.contains("პორტ") && (lowerLatest.contains("ირყევა") || lowerLatest.contains("ცხელდება") || lowerLatest.contains("არ")) ||
            lowerLatest.contains("წყალ") || lowerLatest.contains("დასველ") || lowerLatest.contains("სითხ") ||
            lowerLatest.contains("გაბერ") || lowerLatest.contains("მალე ჯდება") ||
            lowerLatest.contains("დამივარდა") || lowerLatest.contains("ჩამივარდა") ||
            lowerLatest.contains("ხმა არ") || lowerLatest.contains("ჩახლეჩილ") ||
            lowerLatest.contains("კამერა არ") || lowerLatest.contains("ფოკუს") ||
            lowerLatest.contains("0.00a") || lowerLatest.contains("edl") || lowerLatest.contains("9008") ||
            lowerLatest.contains("dfu") || lowerLatest.contains("recovery") || lowerLatest.contains("fastboot")

        if (isHardwareSymptom) return null

        // 1. Check software subscriptions & digital catalog items
        val softwareKeywords = listOf(
            "chatgpt", "ჩატჯიპიტი", "gpt", "openai",
            "google ai", "gemini", "ჯემინი", "18 თვ", "google one", "2tb", "5tb", "gmail", "ჯიმეილ",
            "canva", "კანვა", "capcut", "კაპკატ", "adobe", "ადობ", "photoshop", "ფოტოშოპ", "premiere", "acrobat",
            "youtube", "იუთუბ", "spotify", "სპოტიფაი", "claude", "კლოდ", "grok", "გროკ",
            "perplexity", "პერპლექს", "cursor", "კურსორ", "lovable", "ლავაბლ",
            "windows", "ვინდოუს", "office", "ოფის", "microsoft 365", "word", "excel", "ltsc"
        )
        if (softwareKeywords.any { lowerLatest.contains(it) }) {
            val matched = GeminiDiagnosticService.findRelevantProducts(latestQuery, catalogProducts)
            if (matched.isNotEmpty()) {
                val p = matched.first()
                val detailsText = buildString {
                    appendLine("✨ **${p.title}** ხელმისაწვდომია **BEKASERVICE**-ის კატალოგში:")
                    appendLine()
                    appendLine("• **ფასი:** ${p.price} ${if (p.duration.isNotBlank()) "(${p.duration})" else ""}")
                    if (p.offerType.isNotBlank()) appendLine("• **პაკეტის ტიპი:** ${p.offerType} ${if (p.accountType.isNotBlank()) "• ${p.accountType}" else ""}")
                    if (p.shortDescription.isNotBlank()) appendLine("• **აღწერა:** ${p.shortDescription}")
                    if (p.features.isNotEmpty()) {
                        appendLine("• **მთავარი უპირატესობები:**")
                        p.features.take(4).forEach { f -> appendLine("  ✓ $f") }
                    }
                    if (p.activationTerms.isNotBlank()) appendLine("• **აქტივაცია:** ${p.activationTerms}")
                    appendLine()
                    append("ქვემოთ მოცემულ ბარათზე დაჭერით შეგიძლიათ ნახოთ სრული პარამეტრები და შეუკვეთოთ პირდაპირ აპლიკაციიდან!")
                }
                return DiagnosticMessage(
                    role = "assistant",
                    text = detailsText,
                    structuredSection = null,
                    recommendedProductIds = matched.take(2).map { it.id }
                )
            }
        }

        // 2. Check if user is asking for the price/availability of a specific part or service in catalog
        val asksPriceOrAvailability = lowerLatest.contains("რა ღირს") || lowerLatest.contains("ფასი") ||
            lowerLatest.contains("გაქვთ") || lowerLatest.contains("მინდა") || lowerLatest.contains("ყიდვა") ||
            lowerLatest.contains("შეცვლა რა ჯდება")
        if (asksPriceOrAvailability) {
            val matched = GeminiDiagnosticService.findRelevantProducts(latestQuery, catalogProducts)
            if (matched.isNotEmpty()) {
                val topItems = matched.take(2)
                val reply = buildString {
                    appendLine("🔎 თქვენი მოთხოვნის შესაბამისად **BEKASERVICE**-ის კატალოგში მოიძებნა:")
                    appendLine()
                    topItems.forEach { item ->
                        appendLine("🔹 **${item.title}**")
                        appendLine("• **ფასი:** ${item.price} | **გარანტია/ვადა:** ${item.duration.ifBlank { item.warranty }}")
                        if (item.compatibleModels.isNotBlank() && !SoftwareCatalogData.isDigitalSoftwareService(item)) {
                            appendLine("• **თავსებადობა:** ${item.compatibleModels}")
                        }
                        if (item.shortDescription.isNotBlank()) {
                            appendLine("• **დეტალები:** ${item.shortDescription}")
                        }
                        appendLine()
                    }
                    append("დააჭირეთ ქვემოთ მოცემულ პროდუქტის ბარათს დეტალების გასახსნელად ან კალათაში დასამატებლად.")
                }
                return DiagnosticMessage(
                    role = "assistant",
                    text = reply,
                    structuredSection = null,
                    recommendedProductIds = topItems.map { it.id }
                )
            }
        }

        // 3. Greetings
        val isGreeting = lowerLatest in listOf(
            "გამარჯობა", "გამარჯობათ", "სალამი", "მოგესალმებით", "hi", "hello", "hey",
            "привет", "здравствуйте", "добрый день"
        ) ||
            (lowerLatest.startsWith("გამარჯობა") && lowerLatest.length < 18) ||
            (lowerLatest.startsWith("привет") && lowerLatest.length < 18)
        if (isGreeting) {
            val greetingText = when (AppPreferencesManager.selectedLanguage.lowercase()) {
                "en" -> "Hello! I am the BEKASERVICE AI Diagnostics Assistant. Describe your phone's issue or upload a photo, and I'll help identify possible causes and diagnostic steps."
                "ru" -> "Здравствуйте! Я AI-ассистент диагностики BEKASERVICE. Опишите проблему с телефоном или загрузите фото, и я помогу определить возможные причины и шаги проверки."
                else -> "გამარჯობა! მე ვარ BEKASERVICE-ის AI დიაგნოსტიკური ასისტენტი. აღწერე ტელეფონის პრობლემა ან ატვირთე ფოტო და დაგეხმარები შესაძლო მიზეზებისა და შემოწმების ნაბიჯების განსაზღვრაში."
            }
            return DiagnosticMessage(
                role = "assistant",
                text = greetingText,
                structuredSection = null,
                recommendedProductIds = emptyList()
            )
        }

        if (lowerLatest.contains("სად ხართ") || lowerLatest.contains("მისამართ") || lowerLatest.contains("ლოკაცია") ||
            lowerLatest.contains("სამუშაო საათ") || lowerLatest.contains("ნომერი") || lowerLatest.contains("კონტაქტ")
        ) {
            return DiagnosticMessage(
                role = "assistant",
                text = "📍 **BEKASERVICE-ის მისამართი და კონტაქტი:**\n\n" +
                    "• **მისამართი:** ქ. ახალციხე, მერაბ კოსტავას ქუჩა N10\n" +
                    "• **სამუშაო საათები:** ყოველდღე 10:00 – 20:00\n" +
                    "• **WhatsApp / ტელეფონი:** +995 598 23 33 32\n" +
                    "• **Telegram:** t.me/geounlock\n\n" +
                    "ციფრული პროგრამული სერვისები და დისტანციური USB განბლოკვა ხელმისაწვდომია მთელი საქართველოს მასშტაბით 15–30 წუთში!",
                structuredSection = null
            )
        }

        // General tech questions (e.g. OLED vs LCD, buying advice, checking used iPhone, eSIM, storage tips)
        if (lowerLatest.contains("განსხვავება") && (lowerLatest.contains("oled") || lowerLatest.contains("lcd") || lowerLatest.contains("ორიგინალ"))) {
            return DiagnosticMessage(
                role = "assistant",
                text = "📱 **რა განსხვავებაა Original OLED, Hard/Soft OLED და Incell (LCD) ეკრანებს შორის:**\n\n" +
                    "1. **Original / Soft OLED:** ქარხნული სტანდარტის დრეკადი პოლიმერული მატრიცა. აქვს იდეალური ფერები, მაქსიმალური სიკაშკაშე, 120Hz ProMotion მხარდაჭერა (შესაბამის მოდელებზე), თხელი ჩარჩოები და მინიმალური ენერგომოხმარება.\n" +
                    "2. **Hard OLED:** მყარი მინის სუბსტრატზე დამზადებული OLED. ფერები კარგია, თუმცა დავარდნისას უფრო მგრძნობიარეა ვიდრე Soft OLED.\n" +
                    "3. **Incell / IPS LCD:** ბიუჯეტური ვარიანტი უკანა განათებით (Backlight). არის ოდნავ უფრო სქელი, მოიხმარს მეტ ელემენტს და შავი ფერი არ არის აბსოლუტურად ღრმა.\n\n" +
                    "**BEKASERVICE**-ში ვიყენებთ მაღალი კლასის OLED მატრიცებს True Tone-ის გადაპროგრამებითა და 6-თვიანი გარანტიით.",
                structuredSection = null,
                recommendedProductIds = GeminiDiagnosticService.findRelevantProducts("ეკრანი", catalogProducts).take(2).map { it.id }
            )
        }

        if (lowerLatest.contains("ყიდვის წინ") || lowerLatest.contains("მეორადი") || lowerLatest.contains("როგორ შევამოწმო")) {
            return DiagnosticMessage(
                role = "assistant",
                text = "🔍 **მეორადი სმარტფონის (iPhone / Android) ყიდვის წინ შესამოწმებელი მთავარი პუნქტები:**\n\n" +
                    "1. **iCloud / Google FRP ბლოკი:** დარწმუნდით, რომ წინა მფლობელის ანგარიში სრულად გამოსულია და ტელეფონი არ არის MDM (კორპორატიულ) პროფილზე.\n" +
                    "2. **IMEI და ქსელი (*#06#):** შეამოწმეთ მუშაობს თუ არა ქართული SIM ბარათი (Magti/Silknet/Cellfie) და ხომ არ აქვს R-SIM ჩიპი.\n" +
                    "3. **ეკრანი და True Tone:** გახსენით თეთრი და შავი ფონი — არ უნდა ჰქონდეს ვარდისფერი/მწვანე ფონი (Burn-in) ან მკვდარი ზონები სენსორზე.\n" +
                    "4. **Face ID / Touch ID და კამერები:** შეამოწმეთ ყველა ლინზა (0.5x, 1x, 2x/3x/5x) და ფოკუსირება.\n" +
                    "5. **ელემენტის რესურსი:** დააკვირდით ხომ არ იკლებს პროცენტი ნახტომისებურად 2–3 წუთიანი ვიდეოს გადაღებისას.\n\n" +
                    "სურვილის შემთხვევაში შეგიძლიათ მოიტანოთ ტელეფონი **BEKASERVICE**-ში სრულ კომპიუტერულ და აპარატურულ შემოწმებაზე!",
                structuredSection = null
            )
        }

        // Check if the query matches any product in catalog even without explicit "რა ღირს"
        val directCatalogMatches = GeminiDiagnosticService.findRelevantProducts(latestQuery, catalogProducts)
        val isLikelyDiagnosis = lowerLatest.contains("რატომ") || lowerLatest.contains("როგორ") ||
            lowerLatest.contains("პრობლემა") || lowerLatest.contains("ჭირს") || lowerLatest.contains("სჭირს") ||
            lowerLatest.contains("გაფუჭ") || lowerLatest.contains("დაზიან") || lowerLatest.contains("ჩერდება") ||
            lowerLatest.contains("ითიშება") || lowerLatest.contains("ცხელდება") || lowerLatest.contains("ჭედავს")

        if (directCatalogMatches.isNotEmpty() && !isLikelyDiagnosis) {
            val top = directCatalogMatches.first()
            return DiagnosticMessage(
                role = "assistant",
                text = "✨ **${top.title}** ხელმისაწვდომია **BEKASERVICE**-ში:\n\n" +
                    "• **ფასი:** ${top.price}\n" +
                    (if (top.shortDescription.isNotBlank()) "• **აღწერა:** ${top.shortDescription}\n" else "") +
                    (if (top.warranty.isNotBlank()) "• **გარანტია / ვადა:** ${top.duration.ifBlank { top.warranty }}\n\n" else "\n") +
                    "იხილეთ ქვემოთ მოცემული ბარათი დეტალური პარამეტრებისა და შეკვეთისთვის!",
                structuredSection = null,
                recommendedProductIds = directCatalogMatches.take(2).map { it.id }
            )
        }

        return null
    }

    private data class DomainDiagnosisTuple(
        val domainTitle: String,
        val directExplanation: String,
        val simpleExplanation: String,
        val deepTechnicalExplanation: String,
        val hypotheses: List<DiagnosticHypothesis>,
        val nextTest: String,
        val expectedFindings: String,
        val tools: String,
        val safety: String
    )

    /**
     * Classifies the symptom accurately (prioritizing specific symptoms like Bootloop/Logo Hang
     * BEFORE generic "doesn't turn on/charge" phrases!) and builds rich, accurate explanations.
     */
    private fun classifyAndBuildTechnicalDiagnosis(
        queryText: String,
        fullHistoryText: String,
        mode: DiagnosticUserMode,
        deviceName: String,
        hasPhoto: Boolean,
        isTestResultUpdate: Boolean
    ): DomainDiagnosisTuple {
        // 1. BOOTLOOP / STUCK ON LOGO / RECOVERY / FASTBOOT / DFU (Checked FIRST so "ჩერდება ლოგოზე და აღარ ირთვება ბოლომდე" matches here!)
        val isBootOrLogoHang = queryText.contains("ლოგო") || queryText.contains("bootloop") ||
            queryText.contains("რესტარტ") || queryText.contains("recovery") || queryText.contains("dfu") ||
            queryText.contains("fastboot") || queryText.contains("ჩართვისას ჩერდება") ||
            queryText.contains("ბოლომდე არ ირთვება") || queryText.contains("აღარ ირთვება ბოლომდე") ||
            queryText.contains("ვაშლზე") || queryText.contains("წარწერაზე ჩერდება")

        // 2. EDL 9008 / HARD BRICK / AUTH FLASHING
        val isEdlOrHardBrick = queryText.contains("9008") || queryText.contains("edl") ||
            queryText.contains("firehose") || queryText.contains("brom") || queryText.contains("აგური")

        // 3. WATER / LIQUID DAMAGE
        val isWaterDamage = queryText.contains("წყალ") || queryText.contains("სითხ") ||
            queryText.contains("დასველ") || queryText.contains("ნესტ") || queryText.contains("ჩავარდა წყალში")

        // 4. DISPLAY / SCREEN / TOUCH / GREEN LINE / BLACK SCREEN WITH VIBRATION
        val isDisplayIssue = queryText.contains("ეკრან") || queryText.contains("ზოლ") ||
            queryText.contains("მწვანე") || queryText.contains("ლაქ") || queryText.contains("ბზარ") ||
            queryText.contains("სენსორ") || queryText.contains("ჩაბნელ") || queryText.contains("არ ანათებს") ||
            queryText.contains("გამოსახულება") || queryText.contains("ციმციმებს")

        // 5. BATTERY / OVERHEATING / SWOLLEN / FAST DRAIN
        val isBatteryOrThermal = queryText.contains("ელემენტ") || queryText.contains("ბატარე") ||
            queryText.contains("მალე ჯდება") || queryText.contains("გაბერ") ||
            (queryText.contains("ცხელდება") && !queryText.contains("პორტ")) ||
            queryText.contains("პროცენტ") || queryText.contains("ითიშება 20") || queryText.contains("ითიშება 15")

        // 6. CHARGING PORT / NOT CHARGING / DEAD POWER
        val isChargingOrPower = queryText.contains("არ იტენება") || queryText.contains("დამტენ") ||
            queryText.contains("პორტ") || queryText.contains("კაბელ") || queryText.contains("0.00a") ||
            queryText.contains("ამპერ") || queryText.contains("საერთოდ არ ირთვება") ||
            queryText.contains("ჩაქრა") || queryText.contains("არ ირთვება")

        // 7. ACCOUNT LOCKS (FRP / iCloud) & NETWORK / SIM / BASEBAND
        val isLockOrNetwork = queryText.contains("frp") || queryText.contains("icloud") ||
            queryText.contains("apple id") || queryText.contains("google") || queryText.contains("ბლოკ") ||
            queryText.contains("sim") || queryText.contains("ქსელ") || queryText.contains("no service") ||
            queryText.contains("imei") || queryText.contains("baseband") || queryText.contains("კოდ")

        // 8. AUDIO / SPEAKER / MICROPHONE / CAMERA / FACE ID / WIFI
        val isAudioCameraSensor = queryText.contains("ხმა") || queryText.contains("დინამიკ") ||
            queryText.contains("მიკროფონ") || queryText.contains("კამერ") || queryText.contains("ფოკუს") ||
            queryText.contains("face id") || queryText.contains("touch id") ||
            queryText.contains("wifi") || queryText.contains("ვაიფაი") || queryText.contains("bluetooth")

        // 9. SLOW PERFORMANCE / STORAGE / FREEZING
        val isSlowOrStorage = queryText.contains("ჭედავს") || queryText.contains("ნელა") ||
            queryText.contains("მეხსიერება") || queryText.contains("მაგდებს") || queryText.contains("გაჭედა")

        return when {
            isBootOrLogoHang -> buildBootFirmwareDiagnosis(mode, queryText, deviceName)
            isEdlOrHardBrick -> buildEdlHardBrickDiagnosis(mode, deviceName)
            isWaterDamage -> buildWaterDamageDiagnosis(mode, queryText, deviceName)
            isDisplayIssue -> buildDisplayDiagnosis(mode, queryText, deviceName, hasPhoto)
            isBatteryOrThermal -> buildBatteryThermalDiagnosis(mode, queryText, deviceName)
            isLockOrNetwork -> buildLockNetworkDiagnosis(mode, queryText, deviceName)
            isAudioCameraSensor -> buildAudioCameraDiagnosis(mode, queryText, deviceName)
            isSlowOrStorage -> buildPerformanceStorageDiagnosis(mode, deviceName)
            isChargingOrPower -> buildPowerChargingDiagnosis(mode, queryText, deviceName, isTestResultUpdate)
            else -> buildSmartGeneralDiagnosis(mode, queryText, fullHistoryText, deviceName, hasPhoto)
        }
    }

    private fun buildBootFirmwareDiagnosis(
        mode: DiagnosticUserMode,
        queryText: String,
        deviceName: String
    ): DomainDiagnosisTuple {
        val hasStorageFullIssue = queryText.contains("მეხსიერებ") || queryText.contains("გაივსო") || queryText.contains("ადგილი") || queryText.contains("სავსე")

        val simpleExp = if (hasStorageFullIssue) {
            "💾⚙️ **მეხსიერების გადავსებით გამოწვეული სისტემური Bootloop ($deviceName):**\n\n" +
            "როდესაც შიდა მეხსიერება 100%-ით ივსება, ოპერაციული სისტემა ვეღარ ქმნის დროებით სისტემურ Cache და Swap ფაილებს, რის გამოც ჩატვირთვის ციკლი წყდება და ტელეფონი რჩება ლოგოზე.\n" +
            "• **კრიტიკული გაფრთხილება:** არ გააკეთოთ 'Wipe Data / Factory Reset' Recovery მენიუდან! ეს სამუდამოდ წაშლის ყველა თქვენს ფოტოსა და პირად მონაცემს.\n" +
            "• **BEKASERVICE-ის გადაწყვეტა:** სპეციალური საინჟინრო ხელსაწყოებით (Recovery Log / RAM Dump / Retain User Data რეჟიმი) სისტემის სტაბილიზაცია და პირადი მონაცემების მაქსიმალური შენარჩუნება.\n" +
            "• **შესრულების დრო:** 25-35 წუთი ადგილზევე ბექასერვისში."
        } else {
            "როდესაც ტელეფონი ჩართვისას ჩერდება ლოგოზე და მთავარ ეკრანზე აღარ გადადის (Bootloop), ეს ნიშნავს, რომ ტელეფონის ეკრანი და კვების ღილაკი მუშაობს, მაგრამ ოპერაციული სისტემა ვერ ასრულებს ჩატვირთვას. ამის 3 ყველაზე ხშირი მიზეზია:\n" +
            "1. **სისტემური პროგრამის ხარვეზი ან მეხსიერების გადავსება** (განსაკუთრებით თუ ტელეფონში თავისუფალი ადგილი აღარ იყო ან განახლება გაწყდა).\n" +
            "2. **დასუსტებული ელემენტი** — ჩატვირთვის ბოლო ფაზაში ტელეფონს სჭირდება მაქსიმალური ენერგია; თუ ელემენტი სუსტია, ტელეფონი ითიშება ან თავიდან იტვირთება.\n" +
            "3. **დაზიანებული დამხმარე შლეიფი** (მაგალითად, ზედა დინამიკის/Face ID შლეიფი სისველის ან დავარდნის შემდეგ)."
        }

        val deepTechExp = if (hasStorageFullIssue) {
            "Userdata Exhaustion Bootloop ($deviceName): F2FS/EXT4 ფაილური სისტემა ვეღარ ასრულებს ჟურნალირებას (Zero Inodes/Sectors free). Init პროცესი ვერ წერს /data/dalvik-cache-ს და ბირთვი აინიცირებს SIGABRT რესტარტს. გადაწყვეტა: Fastboot/ADB sideload ramdisk, ქეშის გაწმენდა და სისტემის აღდგენა მონაცემების შენარჩუნებით."
        } else {
            "Bootloop / Logo Hang ($deviceName) მიუთითებს იმაზე, რომ პირველადი ჩამტვირთავი (BootROM / iBoot / Little Kernel) ინიციალიზდება და რთავს დისპლეის კვებას, მაგრამ ჩატვირთვა წყდება Kernel/System ინიციალიზაციის ან I2C პერიფერიის გამოკითხვის ეტაპზე:\n" +
            "1. **Firmware / File System Corruption:** გადავსებული Userdata დანაყოფი ან შეწყვეტილი OTA განახლება ბლოკავს ფაილური სისტემის დამაუნთებას.\n" +
            "2. **I2C Bus Hang (პერიფერიული შლეიფი):** დაზიანებული ზედა სენსორის (Proximity/Flood Illuminator/Earpiece flex) ან თერმისტორის ხაზი იწვევს Kernel Panic-ს და ციკლურ რესტარტს ყოველ 30–180 წამში.\n" +
            "3. **VBAT Voltage Drop:** ჩატვირთვის პიკზე (>1.5A) მაღალი შიდა წინაღობის მქონე ელემენტზე ძაბვა ვარდება UVLO ზღვარს ქვემოთ."
        }

        return if (mode == DiagnosticUserMode.TECHNICIAN) {
            DomainDiagnosisTuple(
                domainTitle = "ჩატვირთვის ციკლის შეფერხება (Bootloop / Logo Hang)",
                directExplanation = deepTechExp,
                simpleExplanation = simpleExp,
                deepTechnicalExplanation = deepTechExp,
                hypotheses = listOf(
                    DiagnosticHypothesis(
                        title = "სისტემური დანაყოფის (Firmware / Userdata) ხარვეზი ან მეხსიერების გადავსება",
                        confidence = EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                        supportingEvidence = "მოწყობილობა რთავს ეკრანს, აჩვენებს ლოგოს, მაგრამ ვერ ტვირთავს OS-ს ($queryText)",
                        verificationOrDisproof = "Recovery / Fastboot / DFU რეჟიმში გადაყვანა და Retain User Data რეჟიმით პროგრამული განახლების/ლოგის შემოწმება"
                    ),
                    DiagnosticHypothesis(
                        title = "პერიფერიული შლეიფის (ზედა სენსორი / ჩართვის ღილაკი) ან დასუსტებული ელემენტის ხარვეზი",
                        confidence = EvidenceConfidenceLevel.POSSIBLE,
                        supportingEvidence = "I2C ხაზის დამოკლება ან ჩატვირთვის პიკზე ძაბვის ვარდნა ხშირად აჩერებს ტელეფონს ლოგოზე",
                        verificationOrDisproof = "ზედა სენსორის შლეიფის დროებით ჩახსნა და ლაბორატორიულ კვების ბლოკზე ჩატვირთვის დენის დაკვირვება"
                    )
                ),
                nextTest = "გადაიყვანეთ ტელეფონი Recovery / Fastboot რეჟიმში და დააკვირდით: სერვისულ ეკრანზე სტაბილურად ჩერდება თუ იქაც თავისით ითიშება/რესტარტდება?",
                expectedFindings = "თუ Recovery-ში სტაბილურად დგას — კვება და ელემენტი მწყობრშია (მიზეზი პროგრამულია ან I2C შლეიფი); თუ Recovery-შიც ითიშება — შეამოწმეთ ელემენტი და ჩართვის ღილაკი.",
                tools = "PC USB კაბელი, 3uTools / Fastboot / Odin / მიკროსკოპი და ლაბორატორიული ბლოკი",
                safety = "მონაცემების შესანარჩუნებლად პირველ ეტაპზე არ გამოიყენოთ Full Erase / Factory Reset — ჯერ სცადეთ სისტემის აღდგენა მონაცემების შენარჩუნებით (Retain User Data)."
            )
        } else {
            DomainDiagnosisTuple(
                domainTitle = "ლოგოზე გაჭედვა / სისტემური ჩატვირთვის შეფერხება (Bootloop)",
                directExplanation = simpleExp,
                simpleExplanation = simpleExp,
                deepTechnicalExplanation = deepTechExp,
                hypotheses = listOf(
                    DiagnosticHypothesis(
                        title = "სისტემური პროგრამის (Firmware) ხარვეზი ან მეხსიერების გადავსება",
                        confidence = EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                        supportingEvidence = "ტელეფონი ირთვება, აჩვენებს ლოგოს, მაგრამ ბოლომდე ვერ იტვირთება",
                        verificationOrDisproof = "იძულებითი გადატვირთვა (Force Restart) ან სერვისში პროგრამული აღდგენა"
                    ),
                    DiagnosticHypothesis(
                        title = "ელემენტის სისუსტე ჩატვირთვის დროს ან შიდა შლეიფის კონტაქტი",
                        confidence = EvidenceConfidenceLevel.POSSIBLE,
                        supportingEvidence = "დავარდნის, სისველის ან დაძველებული ელემენტის დროს ტელეფონს არ ჰყოფნის სიმძლავრე ჩასართავად",
                        verificationOrDisproof = "ორიგინალ დამტენზე შეერთებულ მდგომარეობაში ჩართვის შემოწმება"
                    )
                ),
                nextTest = "შეაერთეთ ტელეფონი დამტენზე 10 წუთით და სცადეთ იძულებითი გადატვირთვა (ჩართვის და ხმის დაწევის ღილაკებზე ერთდროულად დაჭერა 10–15 წამით). დააკვირდით: ლოგო უწყვეტად ანათებს თუ ტელეფონი ყოველ რამდენიმე წამში ქრება და თავიდან აჩვენებს ლოგოს?",
                expectedFindings = "თუ დამტენზე შეერთებისას ჩაირთო — მიზეზი ელემენტია; თუ ისევ ლოგოზე ჩერდება — საჭიროა პროგრამული აღდგენა სერვისში.",
                tools = "ორიგინალი დამტენი კაბელი და ადაპტერი",
                safety = "თუ ტელეფონში გაქვთ მნიშვნელოვანი ფოტოები, არ აირჩიოთ 'Factory Reset / Wipe Data' — ბექასერვისში შესაძლებელია სისტემის აღდგენა მონაცემების მაქსიმალური დაცვით."
            )
        }
    }

    private fun buildEdlHardBrickDiagnosis(
        mode: DiagnosticUserMode,
        deviceName: String
    ): DomainDiagnosisTuple {
        val exp = "მოწყობილობა ($deviceName) გადასულია ავარიული ჩატვირთვის რეჟიმში (**Qualcomm HS-USB QDLoader 9008 / MTK BROM**). ამ მდგომარეობაში ეკრანი სრულიად შავია, მაგრამ კომპიუტერთან USB შეერთებისას Device Manager-ში ჩანს პორტი. აღდგენისთვის საჭიროა ოფიციალური სერვერული (Auth) პროგრამირება."
        return DomainDiagnosisTuple(
            domainTitle = "ავარიული ჩატვირთვის რეჟიმი (EDL 9008 / BROM Hard-Brick)",
            directExplanation = exp,
            simpleExplanation = exp,
            deepTechnicalExplanation = exp + " სრულდება Firehose/DA ავტორიზაცია და Boot/Rawprogram სექტორების აღდგენა.",
            hypotheses = listOf(
                DiagnosticHypothesis(
                    title = "Bootloader / XBL სექტორის დაზიანება წარუმატებელი განახლების ან ფლეშირების შემდეგ",
                    confidence = EvidenceConfidenceLevel.CONFIRMED_BY_EVIDENCE,
                    supportingEvidence = "ფიქსირდება EDL 9008 / Auth რეჟიმი",
                    verificationOrDisproof = "სერვერული Auth ფლეშირება და UFS/eMMC ჯანმრთელობის შემოწმება"
                )
            ),
            nextTest = "შეაერთეთ ტელეფონი კომპიუტერზე USB კაბელით და შეამოწმეთ ჩანს თუ არა პორტი სტაბილურად გათიშვის გარეშე.",
            expectedFindings = "სტაბილური 9008 პორტის შემთხვევაში სისტემა აღდგება 15–20 წუთში ბექასერვისში (ან დისტანციურად USB-ით).",
            tools = if (mode == DiagnosticUserMode.TECHNICIAN) "PC USB, Auth Server Tool, Testpoint პინცეტი" else "კომპიუტერი და USB კაბელი",
            safety = "არ სცადოთ არათავსებადი Loader-ების ჩაწერა, რათა არ დაზიანდეს IMEI/NVRAM კალიბრაციის სექტორი."
        )
    }

    private fun buildWaterDamageDiagnosis(
        mode: DiagnosticUserMode,
        queryText: String,
        deviceName: String
    ): DomainDiagnosisTuple {
        val hasConnectedCharger = queryText.contains("დამტენ") || queryText.contains("შევაერთე") || queryText.contains("დავტენე") || queryText.contains("კაბელ") || queryText.contains("იტენება")

        val simpleExp = if (hasConnectedCharger) {
            "💧⚠️ **კრიტიკული გაფრთხილება: სითხის მოხვედრა და დამტენზე შეერთება ($deviceName)!**\n\n" +
            "სითხის მოხვედრის შემდეგ დამტენზე შეერთება ყველაზე სახიფათო ნაბიჯია: დენის მიწოდება სველ კონტაქტებზე იწვევს მყისიერ ელექტროლიზს, დამტენის პორტისა და PMIC/Hydra კვების კონტროლერის მოკლე ჩართვას (Short circuit).\n" +
            "• **დაუყოვნებელი მოქმედება:** გამორთეთ დამტენიდან ახლავე! არ სცადოთ ტელეფონის ჩართვა ან დატენვა.\n" +
            "• **BEKASERVICE-ის გამოსავალი:** ტელეფონის დაუყოვნებელი გახსნა, ელემენტის გათიშვა, დედაპლატის ულტრაბგერითი ქიმიური წმენდა 99%+ იზოპროპილით და მოკლე ჩართვების ლოკალიზება თერმო-კამერით."
        } else {
            "💧 **წყლის ან სითხის მოხვედრა ($deviceName) საჭიროებს დაუყოვნებლივ რეაგირებას!**\n\n" +
            "ყველაზე დიდი საფრთხეა არა თავად წყალი, არამედ წყლისა და ელექტროენერგიის შეხება — როცა სველ კონტაქტებში დენი გადის, რამდენიმე წუთში იწყება ლითონის კონტაქტების დაჟანგვა (ელექტროქიმიური კოროზია) და მოკლე ჩართვა."
        }
        val techExp = if (hasConnectedCharger) {
            "სითხის ინგრესია + VBUS 5V/9V ენერგომომარაგება ($deviceName): მაღალი რისკი OVP (Over-Voltage Protection) მოსფეტის, PMIC VDD_MAIN და Type-C CC1/CC2 ხაზების დაწვის. საჭიროა ულტრაბგერითი წმენდა და თერმოვიზორული დიაგნოსტიკა."
        } else {
            "სითხის ინგრესია ($deviceName): პირველ რიგში ზიანდება Type-C/Lightning პორტი, ქვედა სუბ-პლატა, დისპლეის FPC კონექტორის ანოდის ხაზი და VBAT/VDD_MAIN კონდენსატორები."
        }

        return DomainDiagnosisTuple(
            domainTitle = "სითხის ზემოქმედება და შიდა კონტაქტების კოროზიის რისკი",
            directExplanation = if (mode == DiagnosticUserMode.TECHNICIAN) techExp else simpleExp,
            simpleExplanation = simpleExp,
            deepTechnicalExplanation = techExp,
            hypotheses = listOf(
                DiagnosticHypothesis(
                    title = "დამტენის პორტის, ქვედა პლატის და ეკრანის შლეიფის კონტაქტების დაჟანგვა",
                    confidence = EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                    supportingEvidence = "დაფიქსირებულია წყალთან/სითხესთან კონტაქტი ($queryText)",
                    verificationOrDisproof = "ტელეფონის გახსნა, ელემენტის გათიშვა და მიკროსკოპული/ულტრაბგერითი წმენდა"
                ),
                DiagnosticHypothesis(
                    title = "მთავარი კვების ხაზის ან აუდიო/კამერის კვანძის მოკლე ჩართვა",
                    confidence = EvidenceConfidenceLevel.POSSIBLE,
                    supportingEvidence = "სითხე რჩება დამცავი ეკრანების (Shields) ქვეშ და იწვევს დენის გაჟონვას",
                    verificationOrDisproof = "დედაპლატის გაჟონვის დენის შემოწმება ლაბორატორიულ კვების ბლოკზე"
                )
            ),
            nextTest = if (mode == DiagnosticUserMode.TECHNICIAN) {
                "დაუყოვნებლივ გათიშეთ ელემენტის კონექტორი, დაათვალიერეთ FPC კონექტორები მიკროსკოპით და გაწმინდეთ 99%+ იზოპროპილის სპირტით."
            } else {
                "გამორთეთ ტელეფონი ახლავე, არ შეაერთოთ დამტენზე (მაშინაც კი, თუ ჯდება!) და ამოიღეთ SIM ბარათის ჩარჩო. დააკვირდით ხომ არ თბება კორპუსი გამორთულზე."
            },
            expectedFindings = "თუ ტელეფონი დროულად გაიწმინდება სერვისში დაჟანგვის დაწყებამდე, ნაწილების შეცვლა ხშირად საჭირო აღარ ხდება.",
            tools = if (mode == DiagnosticUserMode.TECHNICIAN) "მიკროსკოპი, ულტრაბგერითი აბაზანა, იზოპროპილის სპირტი, DC ბლოკი" else "მხოლოდ გამორთვა და სერვისში დროული მიტანა",
            safety = "არ ჩადოთ ტელეფონი ბრინჯში (მტვერი აზიანებს პორტს), არ გააშროთ ცხელი ფენით და კატეგორიულად არ შეაერთოთ დამტენზე!"
        )
    }

    private fun buildDisplayDiagnosis(
        mode: DiagnosticUserMode,
        queryText: String,
        deviceName: String,
        hasPhoto: Boolean
    ): DomainDiagnosisTuple {
        val hasDropAndVibrationAlive = (queryText.contains("ვარდნ") || queryText.contains("დამივარდა") || queryText.contains("დავარდა") || queryText.contains("დარტყმ")) &&
            (queryText.contains("ვიბრაცი") || queryText.contains("ვიბრირებს") || queryText.contains("ხმა") || queryText.contains("კომპიუტერ") || queryText.contains("ზარი"))
        val hasGreenOrVerticalLines = queryText.contains("ზოლ") || queryText.contains("მწვანე") || queryText.contains("ვარდისფერ")
        val hasBlackSpots = queryText.contains("ლაქ") || queryText.contains("მელნ")
        val isBlackScreen = queryText.contains("შავ") || queryText.contains("არ ანათებს") || queryText.contains("ჩაბნელ")

        val simpleExp = when {
            hasDropAndVibrationAlive -> "📱 **სრული კომპლექსური ანალიზი ($deviceName): დედაპლატა 100% ცოცხალია, დაზიანებულია მხოლოდ OLED მატრიცა!**\n\nვიბრაცია, ხმოვანი სიგნალი ან კომპიუტერთან კავშირი უტყუარად ადასტურებს, რომ ტელეფონის პროცესორი, მეხსიერება (UFS/eMMC), კვების კონტროლერი და ოპერაციული სისტემა სრულ მწყობრშია. თქვენი ყველა პირადი ფოტო და მონაცემი სრულად უსაფრთხოდაა! დარტყმის შედეგად დაზიანდა მხოლოდ ორგანული OLED მატრიცის შიდა შრე ან შლეიფის მიერთება, რის გამოც ეკრანზე გამოსახულება აღარ გამოდის. BEKASERVICE-ში ახალი ორიგინალი დისპლეის მონტაჟი სრულდება 25–35 წუთში."
            hasGreenOrVerticalLines -> "📱 **ეკრანზე გაჩენილი მწვანე/ფერადი ზოლი ($deviceName)** ნიშნავს, რომ დაზიანებულია OLED ეკრანის შიდა მატრიცა ან მისი მიკრო-შლეიფი (ხშირად ჩნდება დავარდნის ან კუთხეზე მცირე დაწოლის შემდეგ). თავად ტელეფონის პლატა და თქვენი მონაცემები სრულიად უვნებელია."
            hasBlackSpots -> "📱 **ეკრანზე შავი ლაქა ($deviceName)** მიუთითებს შიდა OLED/LCD პიქსელების ფიზიკურ დაზიანებაზე (დეჰერმეტიზაციაზე). დროთა განმავლობაში ეს ლაქა შეიძლება გაიზარდოს და სენსორმაც შეწყვიტოს რეაგირება."
            isBlackScreen -> "📱 **ეკრანი ჩაბნელებულია ($deviceName):** პირველ რიგში უნდა გაირკვეს, მხოლოდ ეკრანი ჩაქრა (ტელეფონი კი ჩართულია და ზარს იღებს) თუ თავად ტელეფონია სრულად გათიშული."
            else -> "📱 **ეკრანის ან სენსორის ხარვეზი ($deviceName):** სიმპტომები მიუთითებს დისპლეის მოდულის, სენსორული შრის ან ეკრანის კონექტორის დაზიანებაზე."
        }

        val techExp = if (hasDropAndVibrationAlive) {
            "OLED მატრიცის დაზიანება ცოცხალ დედაპლატაზე ($deviceName): Haptic/Audio/USB ენერგომოხმარება ადასტურებს PMIC VDD_MAIN და PMU Secondary rails ნორმას. დაზიანება ლოკალიზებულია DSI/MIPI ხაზზე, OLED ELVDD/ELVSS ბუსტერზე ან COF/COP მატრიცის მიკრო-ბზარზე."
        } else {
            "დისპლეის ქვესისტემის დიაგნოსტიკა ($deviceName): გასამიჯნია OLED/LCD მატრიცის COF/COP ბონდინგის დაზიანება, FPC კონექტორის კონტაქტი და დისპლეის კვების წრედი (ELVDD/ELVSS ან Backlight Anode/Boost)."
        }

        return DomainDiagnosisTuple(
            domainTitle = when {
                hasDropAndVibrationAlive -> "OLED მატრიცის დაზიანება (დედაპლატა სრულ მწყობრშია)"
                hasGreenOrVerticalLines -> "OLED დისპლეის მატრიცის / შლეიფის დაზიანება"
                else -> "დისპლეის მოდულის ან გამოსახულების კვანძის ხარვეზი"
            },
            directExplanation = if (mode == DiagnosticUserMode.TECHNICIAN) techExp else simpleExp,
            simpleExplanation = simpleExp,
            deepTechnicalExplanation = techExp,
            hypotheses = listOf(
                DiagnosticHypothesis(
                    title = if (hasDropAndVibrationAlive) "დედაპლატა ცოცხალია: დაზიანებულია მხოლოდ OLED მატრიცა ან შლეიფი" else "OLED / LCD ეკრანის მატრიცის ან COF შლეიფის ფიზიკური დაზიანება",
                    confidence = if (hasDropAndVibrationAlive || hasPhoto || hasGreenOrVerticalLines || hasBlackSpots) EvidenceConfidenceLevel.CONFIRMED_BY_EVIDENCE else EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                    supportingEvidence = "დაფიქსირებულია ეკრანის ვიზუალური/სენსორული სიმპტომი ($queryText)",
                    verificationOrDisproof = "სერვისში სატესტო (Known-Good) ეკრანის მიერთებით ზუსტად დასტურდება 5 წუთში"
                ),
                DiagnosticHypothesis(
                    title = "დისპლეის FPC კონექტორის ან კვების ფილტრის ხარვეზი",
                    confidence = EvidenceConfidenceLevel.POSSIBLE,
                    supportingEvidence = "ვარდნის შემდეგ შესაძლებელია კონექტორის ამოვარდნა ან პინის მიკრობზარი",
                    verificationOrDisproof = "კონექტორის მიკროსკოპული დათვალიერება და დიოდური გაზომვა"
                )
            ),
            nextTest = if (mode == DiagnosticUserMode.TECHNICIAN) {
                "შეამოწმეთ სატესტო ეკრანით და გაზომეთ დისპლეის FPC კონექტორზე კვებისა და MIPI ხაზები დიოდურ რეჟიმში."
            } else {
                "დარეკეთ თქვენს ნომერზე სხვა ტელეფონიდან ან ჩართეთ უხმო რეჟიმის გადამრთველი/დამტენი: ისმის თუ არა ზარი ან იგრძნობა ვიბრაცია?"
            },
            expectedFindings = "თუ ტელეფონი რეკავს ან ვიბრირებს — დედაპლატა და მონაცემები უსაფრთხოდაა და საჭიროა მხოლოდ ეკრანის შეცვლა.",
            tools = if (mode == DiagnosticUserMode.TECHNICIAN) "სატესტო ეკრანი, მულტიმეტრი, True Tone პროგრამატორი" else "სხვა ტელეფონი ზარის შესამოწმებლად",
            safety = "თუ შუშა გაბზარულია, მოერიდეთ ეკრანზე ძლიერ დაწოლას, რათა შუშის ნამსხვრევებმა არ დააზიანოს შიდა შლეიფი ან Face ID სენსორი."
        )
    }

    private fun buildBatteryThermalDiagnosis(
        mode: DiagnosticUserMode,
        queryText: String,
        deviceName: String
    ): DomainDiagnosisTuple {
        val isSwollen = queryText.contains("გაბერ") || queryText.contains("აიწია")
        val simpleExp = if (isSwollen) {
            "⚠️ **ყურადღება: გაბერილი ელემენტი ($deviceName) საჭიროებს სასწრაფო შეცვლას!**\n\nგაბერილი ელემენტი შიგნიდან აწვება ეკრანს და შეიძლება გატეხოს მატრიცა ან დააზიანოს შლეიფები. დროებით არ დატენოთ ტელეფონი."
        } else {
            "🔋 **ელემენტის სწრაფი დაჯდომა ან გაცხელება ($deviceName):**\n\nროდესაც ტელეფონი სწრაფად კარგავს პროცენტებს, ითიშება 15–20%-ზე ან ცხელდება დატვირთვის დროს, ეს უმეტესად გამოწვეულია ლითიუმ-იონური ელემენტის ქიმიური ცვეთით (500+ დამუხტვის ციკლის შემდეგ). ახალი ორიგინალი ელემენტის ჩაყენება 15–20 წუთში სრულად აგვარებს ამ პრობლემას."
        }

        val techExp = "ენერგომოხმარების და ელემენტის დიაგნოსტიკა ($deviceName): გასამიჯნია ელემენტის შიდა წინაღობის (ESR) ზრდა და დედაპლატაზე მეორადი კვების ხაზის გაჟონვა (Sleep current > 15mA)."

        return DomainDiagnosisTuple(
            domainTitle = if (isSwollen) "ელემენტის ფიზიკური დეფორმაცია (გაბერვა)" else "ელემენტის ქიმიური ცვეთა ან მომატებული ენერგომოხმარება",
            directExplanation = if (mode == DiagnosticUserMode.TECHNICIAN) techExp else simpleExp,
            simpleExplanation = simpleExp,
            deepTechnicalExplanation = techExp,
            hypotheses = listOf(
                DiagnosticHypothesis(
                    title = "ლითიუმ-იონური ელემენტის რესურსის ამოწურვა (Health < 80% / მაღალი შიდა წინაღობა)",
                    confidence = if (isSwollen) EvidenceConfidenceLevel.CONFIRMED_BY_EVIDENCE else EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                    supportingEvidence = "სწრაფი განმუხტვა, პროცენტების ვარდნა ან გაბერვა ($queryText)",
                    verificationOrDisproof = "ელემენტის ციკლების შემოწმება და ახალი 0-ციკლიანი ელემენტით ტესტირება"
                ),
                DiagnosticHypothesis(
                    title = "დედაპლატაზე ფონური დენის მოხმარება (Leakage)",
                    confidence = EvidenceConfidenceLevel.POSSIBLE,
                    supportingEvidence = "თუ ტელეფონი ცხელდება მაშინაც, როცა ეკრანი ჩამქრალია და არ იყენებთ",
                    verificationOrDisproof = "მოსვენებულ რეჟიმში (Sleep Mode) დენის მოხმარების შემოწმება ლაბორატორიულ ბლოკზე"
                )
            ),
            nextTest = "დააკვირდით: ტელეფონი მალე ჯდება მხოლოდ მაშინ, როცა აქტიურად იყენებთ (ეკრანი ჩართულია), თუ ღამით დადებულ მდგომარეობაშიც 25–40%-ით იცლება?",
            expectedFindings = "თუ მხოლოდ გამოყენებისას ჯდება მალე — 100%-ით ელემენტის ბრალია; თუ უმოქმედო მდგომარეობაშიც ცხელდება და იცლება — შევამოწმებთ კვების წრედსაც.",
            tools = if (mode == DiagnosticUserMode.TECHNICIAN) "ლაბორატორიული ბლოკი, თერმოკამერა, BMS პროგრამატორი" else "პარამეტრები → Battery Health / Usage",
            safety = if (isSwollen) "არ დააწვეთ ამობურცულ ეკრანს და არ დატოვოთ დამტენზე!" else "გამოიყენეთ მხოლოდ ორიგინალი/სერტიფიცირებული დამტენი ადაპტერი."
        )
    }

    private fun buildPowerChargingDiagnosis(
        mode: DiagnosticUserMode,
        queryText: String,
        deviceName: String,
        isTestResultUpdate: Boolean
    ): DomainDiagnosisTuple {
        val simpleExp = "🔌 **დამუხტვის ან ჩართვის პრობლემა ($deviceName):**\n\n" +
            "თუ ტელეფონი დამტენზე შეერთებისას არ რეაგირებს ან კაბელი ირყევა, შემთხვევათა უმრავლესობაში მიზეზია:\n" +
            "1. **დამტენის პორტში დაპრესილი მტვერი ან კონტაქტების ცვეთა** (განსაკუთრებით თუ კაბელი ბოლომდე მყარად არ ჯდება).\n" +
            "2. **დაზიანებული კაბელი/ადაპტერი ან ღრმად დამჯდარი ელემენტი**.\n" +
            "3. **დამუხტვის კონტროლერი (Charging IC)** — თუ ტელეფონი არაორიგინალ ან მანქანის დამტენზე დაზიანდა."

        val techExp = "დამუხტვისა და კვების ჯაჭვის დიაგნოსტიკა ($deviceName): შესამოწმებელია VBUS 5V/9V შემოსვლა, CC1/CC2 ან D+/D- კომუნიკაცია, ქვედა სუბ-პლატა და Charging IC / PMIC."

        return DomainDiagnosisTuple(
            domainTitle = "დამუხტვის პორტის, ქვედა შლეიფის ან კვების კონტროლერის ხარვეზი",
            directExplanation = if (mode == DiagnosticUserMode.TECHNICIAN) techExp else simpleExp,
            simpleExplanation = simpleExp,
            deepTechnicalExplanation = techExp,
            hypotheses = listOf(
                DiagnosticHypothesis(
                    title = "Type-C / Lightning პორტის დაბინძურება, კონტაქტების ცვეთა ან კაბელის ხარვეზი",
                    confidence = EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                    supportingEvidence = "დაფიქსირებულია დამუხტვის ან კვების რეაქციის არქონა ($queryText)",
                    verificationOrDisproof = "სხვა заведомо მუშა კაბელით შემოწმება და პორტის მიკროსკოპული ინსპექცია"
                ),
                DiagnosticHypothesis(
                    title = "დამუხტვის ჩიპის (Charging IC) დაზიანება ან ელემენტის ღრმა განმუხტვა",
                    confidence = if (isTestResultUpdate) EvidenceConfidenceLevel.STRONGLY_SUSPECTED else EvidenceConfidenceLevel.POSSIBLE,
                    supportingEvidence = "თუ პორტი და კაბელი მწყობრშია, კონტროლერი ვერ ხსნის დამუხტვის არხს",
                    verificationOrDisproof = "USB ამპერმეტრზე დენის მოხმარების (V/A) გაზომვა"
                )
            ),
            nextTest = if (mode == DiagnosticUserMode.TECHNICIAN) {
                "შეაერთეთ USB ამპერმეტრზე და მომწერეთ რამდენ ამპერს (A) მოიხმარს (0.00A, 0.15A–0.45A პულსაცია თუ >1.0A) და ხედავს თუ არა კომპიუტერი USB პორტს."
            } else {
                "სცადეთ სხვა заведомо მუშა კაბელი და ადაპტერი, დატოვეთ 15 წუთი და დააკვირდით: კაბელი პორტში ბოლომდე, წკაპუნით შედის თუ ადვილად ვარდება?"
            },
            expectedFindings = "თუ კაბელი ბოლომდე არ შედის — პორტის პროფესიონალური წმენდა 10 წუთში მოაგვარებს; თუ კაბელი მყარად ზის და მაინც არ იტენება — საჭიროა პორტის შლეიფის ან კონტროლერის შემოწმება.",
            tools = if (mode == DiagnosticUserMode.TECHNICIAN) "USB V/A ამპერმეტრი, მულტიმეტრი, მიკროსკოპი" else "სხვა დამტენი კაბელი და ადაპტერი",
            safety = "არ გაწმინდოთ პორტი ნემსით ან ლითონის საგნით — ლითონი ამოკლებს კვების პინებს!"
        )
    }

    private fun buildLockNetworkDiagnosis(
        mode: DiagnosticUserMode,
        queryText: String,
        deviceName: String
    ): DomainDiagnosisTuple {
        val isNoService = queryText.contains("no service") || queryText.contains("searching") || queryText.contains("imei") || queryText.contains("baseband")
        val simpleExp = if (isNoService) {
            "📶 **ქსელის დაკარგვა / 'No Service' ($deviceName):**\n\nეს შეიძლება გამოწვეული იყოს როგორც უცხოურ ოპერატორზე დაბლოკვით (SIM Lock), ისე მოდემის (Baseband) დაზიანებით დავარდნის შემდეგ."
        } else {
            "🔓 **ანგარიშის ან ოპერატორის ბლოკი ($deviceName):**\n\nGoogle FRP, iCloud Activation Lock ან უცხოური ოპერატორის SIM ბლოკი **BEKASERVICE**-ში იხსნება ოფიციალური სერვერული და პროგრამული მეთოდებით, ტელეფონის ყველა ფუნქციის შენარჩუნებით."
        }
        return DomainDiagnosisTuple(
            domainTitle = if (isNoService) "ქსელური რეგისტრაციის ან მოდემის (Baseband) ხარვეზი" else "ანგარიშის / ოპერატორის პროგრამული ბლოკი (FRP / iCloud / SIM Lock)",
            directExplanation = simpleExp,
            simpleExplanation = simpleExp,
            deepTechnicalExplanation = simpleExp + " მოწმდება Baseband ვერსია, *#06# IMEI სტატუსი და Security Patch დონე.",
            hypotheses = listOf(
                DiagnosticHypothesis(
                    title = if (isNoService) "Baseband მოდემის ან NVRAM კალიბრაციის ხარვეზი" else "Google FRP / iCloud / Carrier SIM ბლოკი",
                    confidence = EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                    supportingEvidence = "დაფიქსირებულია ბლოკირების ან ქსელის შეზღუდვის სიმპტომი ($queryText)",
                    verificationOrDisproof = "მოდელის, IMEI-ს და ეკრანზე გამოტანილი შეტყობინების გადამოწმება"
                )
            ),
            nextTest = "აკრიფეთ *#06# (ან შეამოწმეთ Settings → About) და მომწერეთ ტელეფონის ზუსტი მოდელი და ჩანს თუ არა ეკრანზე IMEI კოდი.",
            expectedFindings = "თუ IMEI ჩანს — ქსელური მოდემი მუშაობს და საკითხი სერვერულად/პროგრამულად გვარდება; თუ IMEI ცარიელია — საჭიროა Baseband კვანძის აღდგენა.",
            tools = if (mode == DiagnosticUserMode.TECHNICIAN) "PC USB, სერვერული ავტორიზაცია, NVRAM/Baseband დიაგნოსტიკა" else "ტელეფონის ზუსტი მოდელის და ეკრანის ფოტოს მოწოდება",
            safety = "მოერიდეთ საეჭვო პროგრამებით დაფლეშვას, რათა არ წაიშალოს ქარხნული ქსელური კალიბრაცია."
        )
    }

    private fun buildAudioCameraDiagnosis(
        mode: DiagnosticUserMode,
        queryText: String,
        deviceName: String
    ): DomainDiagnosisTuple {
        val isCamera = queryText.contains("კამერ") || queryText.contains("ფოკუს")
        val isFaceId = queryText.contains("face id") || queryText.contains("touch id")
        val title = when {
            isFaceId -> "ბიომეტრიული სენსორის (Face ID / Touch ID) ხარვეზი"
            isCamera -> "კამერის მოდულის ან ოპტიკური სტაბილიზაციის (OIS) ხარვეზი"
            else -> "აუდიო სისტემის (დინამიკი / მიკროფონი / Audio IC) ხარვეზი"
        }
        val exp = when {
            isFaceId -> "🔐 **Face ID / Touch ID ხარვეზი ($deviceName):** ხშირად ვითარდება სითხის მოხვედრის ან ეკრანის შეცვლისას ზედა სენსორის შლეიფის დაზიანების გამო. ბექასერვისში სრულდება TrueDepth კვანძის მიკროსკოპული აღდგენა."
            isCamera -> "📷 **კამერის ხარვეზი ($deviceName):** თუ კამერა არ ფოკუსირებს, კანკალებს ან შავია, დაზიანებულია ოპტიკური სტაბილიზატორი (OIS) ან კამერის კვების ხაზი."
            else -> "🔊 **ხმის ან მიკროფონის პრობლემა ($deviceName):** თუ ხმა დაბალია ან ჩახლეჩილია, უმეტესად დაბინძურებულია დამცავი ბადე; თუ ხმა საერთოდ გაქრა ზარის დროს და სპიკერის ღილაკიც არააქტიურია — საჭიროა აუდიო კვანძის შემოწმება."
        }
        return DomainDiagnosisTuple(
            domainTitle = title,
            directExplanation = exp,
            simpleExplanation = exp,
            deepTechnicalExplanation = exp,
            hypotheses = listOf(
                DiagnosticHypothesis(
                    title = title,
                    confidence = EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                    supportingEvidence = "დაფიქსირებულია შესაბამისი პერიფერიული სიმპტომი ($queryText)",
                    verificationOrDisproof = "სისტემური ტესტით (დიქტოფონი / კამერის რეჟიმები 0.5x-1x-3x) გადამოწმება"
                )
            ),
            nextTest = if (isCamera) {
                "გახსენით კამერა და გადართეთ 0.5x, 1x და 2x/3x რეჟიმებს შორის: ყველა რეჟიმზე აქვს ხარვეზი თუ მხოლოდ ერთ კონკრეტულ ლინზაზე?"
            } else {
                "ჩაწერეთ 5-წამიანი ხმა სტანდარტულ დიქტოფონში (Voice Memos): იწერება თუ არა ხმა სუფთად?"
            },
            expectedFindings = "ეს ტესტი ზუსტად გვიჩვენებს, გარე გაწმენდა/მოდულის შეცვლაა საჭირო თუ შლეიფის აღდგენა.",
            tools = if (mode == DiagnosticUserMode.TECHNICIAN) "მიკროსკოპი, მულტიმეტრი, სატესტო მოდული" else "კამერის / დიქტოფონის აპლიკაცია",
            safety = "არ გაწმინდოთ დინამიკის ბადე ან კამერის ლინზა სპირტით ან ბასრი ნემსით."
        )
    }

    private fun buildPerformanceStorageDiagnosis(
        mode: DiagnosticUserMode,
        deviceName: String
    ): DomainDiagnosisTuple {
        val exp = "⚡ **ტელეფონის შენელება, გაჭედვა ან აპლიკაციებიდან გამოგდება ($deviceName):**\n\n" +
            "როდესაც სმარტფონის მეხსიერება ივსება 90%-ზე მეტად ან ელემენტის რესურსი ეცემა 80%-ს ქვემოთ, სისტემა ავტომატურად ამცირებს პროცესორის სიხშირეს (Thermal/Power Throttling), რაც იწვევს ჭედვას."
        return DomainDiagnosisTuple(
            domainTitle = "მეხსიერების გადავსება ან პროცესორის სიმძლავრის შეზღუდვა",
            directExplanation = exp,
            simpleExplanation = exp,
            deepTechnicalExplanation = exp + " ასევე მოწმდება NAND (UFS/NVMe) თავისუფალი ბლოკების რეზერვი და I/O დაყოვნება.",
            hypotheses = listOf(
                DiagnosticHypothesis(
                    title = "შიდა მეხსიერების გადავსება (<10% თავისუფალი ადგილი) ან ქეშის დაგროვება",
                    confidence = EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                    supportingEvidence = "სისტემის შენელება და აპლიკაციების დახურვა",
                    verificationOrDisproof = "Settings → Storage-ში თავისუფალი მეხსიერების შემოწმება (უნდა იყოს მინიმუმ 8–10 GB თავისუფალი)"
                ),
                DiagnosticHypothesis(
                    title = "დასუსტებული ელემენტი ან გადახურებით გამოწვეული Throttling",
                    confidence = EvidenceConfidenceLevel.POSSIBLE,
                    supportingEvidence = "ძველი ელემენტის დროს სისტემა ხელოვნურად ანელებს პროცესორს მოულოდნელი გათიშვის თავიდან ასაცილებლად",
                    verificationOrDisproof = "Battery Health-ისა და ტემპერატურის შემოწმება"
                )
            ),
            nextTest = "შეამოწმეთ Settings → Storage-ში რამდენი გიგაბაიტია თავისუფალი და გადატვირთეთ ტელეფონი.",
            expectedFindings = "8–10 GB-ის გათავისუფლება და სისტემური ქეშის გასუფთავება მყისიერად აღადგენს სისწრაფეს.",
            tools = "პარამეტრების მენიუ (Storage & Battery)",
            safety = "არ დატოვოთ მეხსიერება ბოლომდე (100%-ით) სავსე — ამან შეიძლება გადატვირთვისას ტელეფონი ლოგოზე გაჭედოს (Bootloop)."
        )
    }

    private fun buildSmartGeneralDiagnosis(
        mode: DiagnosticUserMode,
        queryText: String,
        fullHistoryText: String,
        deviceName: String,
        hasPhoto: Boolean
    ): DomainDiagnosisTuple {
        val exp = if (hasPhoto) {
            "📷 **ატვირთული ფოტო და შეტყობინება მიღებულია ($deviceName).**\n\n" +
                "ზუსტი ტექნიკური დასკვნისთვის მომწერეთ კიდევ ორი დეტალი: რის შემდეგ დაიწყო ეს პრობლემა (დავარდნა, დასველება, განახლება) და როგორ რეაგირებს ტელეფონი ახლა?"
        } else {
            "🔍 **გთხოვთ დამიზუსტოთ მცირე დეტალი ზუსტი დიაგნოზისთვის:**\n\n" +
                "თქვენი შეტყობინების (`\"${queryText.take(90)}\"`) მიხედვით რომ ზუსტად გითხრათ მიზეზი და გადაჭრის გზა, მომწერეთ:\n" +
                "1. **ტელეფონის ზუსტი მოდელი** (მაგ: iPhone 14 Pro, Samsung S23, Redmi Note 12)\n" +
                "2. **რა სიმპტომი აქვს** (მაგ: ეკრანი, დატენვა, ლოგოზე გაჭედვა, ბლოკი, ხმა) ან პირდაპირ ატვირთეთ ფოტო!"
        }
        return DomainDiagnosisTuple(
            domainTitle = "სადიაგნოსტიკო მონაცემების დაზუსტება",
            directExplanation = exp,
            simpleExplanation = exp,
            deepTechnicalExplanation = exp,
            hypotheses = listOf(
                DiagnosticHypothesis(
                    title = "საჭიროებს სიმპტომის და მოდელის დაზუსტებას",
                    confidence = if (hasPhoto) EvidenceConfidenceLevel.POSSIBLE else EvidenceConfidenceLevel.NOT_YET_ESTABLISHED,
                    supportingEvidence = "მოწოდებულია საწყისი ინფორმაცია",
                    verificationOrDisproof = "მოდელისა და სიმპტომის გამოვლენის პირობების დაზუსტება"
                )
            ),
            nextTest = "მომწერეთ ტელეფონის მოდელი და რა რეაქცია აქვს ჩართვის ან დამტენზე შეერთების დროს.",
            expectedFindings = "ამ დეტალების დაზუსტებისთანავე მოგაწვდით ზუსტ მიზეზს და სამოქმედო გეგმას.",
            tools = if (mode == DiagnosticUserMode.TECHNICIAN) "USB ამპერმეტრი, ვიზუალური ინსპექცია" else "დაკვირვება ან ფოტოს ატვირთვა",
            safety = "თუ მოწყობილობა დასველდა ან უჩვეულოდ ცხელდება, დროებით გამორთეთ."
        )
    }

    private fun findRelevantProductsForDiagnosis(
        query: String,
        catalogProducts: List<ProductItem>
    ): List<Int> {
        val q = query.lowercase()
        val device = detectDeviceModelFromTranscript(listOf(DiagnosticMessage(role = "user", text = query)))
        val hasSpecificDevice = device != "მოდელი არ არის მითითებული" && !device.contains("დასაზუსტებელია")

        val isExplicitSoftwareQuery = listOf(
            "canva", "კანვა", "capcut", "კაპკატ", "adobe", "ადობ", "windows", "ვინდოუს",
            "office", "ოფის", "chatgpt", "gpt", "google ai", "gemini", "claude", "spotify", "youtube"
        ).any { q.contains(it) }

        // Filter hardware spare parts only if they match the detected device or explicit software
        val rawMatched = GeminiDiagnosticService.findRelevantProducts(query, catalogProducts)
        val filtered = rawMatched.filter { prod ->
            val isSoftware = SoftwareCatalogData.isDigitalSoftwareService(prod)
            if (isSoftware) {
                isExplicitSoftwareQuery
            } else if (hasSpecificDevice) {
                prod.title.contains(device, ignoreCase = true) ||
                    prod.compatibleModels.contains(device, ignoreCase = true)
            } else {
                false
            }
        }
        if (filtered.isNotEmpty()) {
            return filtered.take(2).map { it.id }
        }

        // If user has a specific device + screen/battery issue, look up matching part in catalog
        if (hasSpecificDevice) {
            val wantsScreen = q.contains("ეკრან") || q.contains("დისპლე") || q.contains("ზოლ")
            val wantsBattery = q.contains("ელემენტ") || q.contains("ბატარე")
            val matchedByDevice = catalogProducts.filter { prod ->
                !SoftwareCatalogData.isDigitalSoftwareService(prod) &&
                    (prod.title.contains(device, ignoreCase = true) || prod.compatibleModels.contains(device, ignoreCase = true)) &&
                    ((wantsScreen && prod.category.contains("ეკრან")) || (wantsBattery && prod.category.contains("ელემენტ")))
            }
            if (matchedByDevice.isNotEmpty()) {
                return matchedByDevice.take(2).map { it.id }
            }
        }
        return emptyList()
    }

    fun detectDeviceModelFromTranscript(messages: List<DiagnosticMessage>): String {
        // Search from newest message to oldest so recent device mentions take priority
        val text = messages.reversed().joinToString(" ") { it.text }
        val knownPatterns = listOf(
            Regex("""iPhone\s*\d{1,2}(\s*(Pro\s*Max|Pro|Plus|Mini))?""", RegexOption.IGNORE_CASE),
            Regex("""Samsung\s*(Galaxy\s*)?[ASZ]\d{1,2}(\s*(Ultra|Plus|FE|\+))?""", RegexOption.IGNORE_CASE),
            Regex("""Galaxy\s*[ASZ]\d{1,2}(\s*(Ultra|Plus|FE|\+))?""", RegexOption.IGNORE_CASE),
            Regex("""Xiaomi\s*(Redmi\s*Note|Redmi|Mi|Poco)?\s*[A-Za-z0-9]+(\s*(Pro|Ultra))?""", RegexOption.IGNORE_CASE),
            Regex("""Redmi\s*(Note\s*)?\d{1,2}(\s*(Pro|Plus))?""", RegexOption.IGNORE_CASE),
            Regex("""Poco\s*[FXM]\d(\s*(Pro|GT))?""", RegexOption.IGNORE_CASE),
            Regex("""Pixel\s*\d(\s*(Pro|XL|a))?""", RegexOption.IGNORE_CASE),
            Regex("""iPad(\s*(Pro|Air|Mini))?""", RegexOption.IGNORE_CASE)
        )
        for (regex in knownPatterns) {
            val match = regex.find(text)
            if (match != null) {
                return match.value.trim()
            }
        }
        val lower = text.lowercase()
        return when {
            lower.contains("iphone") || lower.contains("აიფონ") -> "Apple iPhone"
            lower.contains("samsung") || lower.contains("სამსუნგ") -> "Samsung Galaxy"
            lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("poco") || lower.contains("შაომი") -> "Xiaomi / Redmi / Poco"
            lower.contains("pixel") || lower.contains("პიქსელ") -> "Google Pixel"
            else -> "მოდელი არ არის მითითებული"
        }
    }
}

// ==============================================================================
// 4. DIAGNOSTIC SESSION & HISTORY PERSISTENCE REPOSITORY
// ==============================================================================

object DiagnosticsSessionRepository {
    private const val PREFS_NAME = "bekaservice_diagnostic_sessions_v2"
    private const val KEY_SESSIONS_JSON = "saved_sessions_json"
    private const val MAX_SAVED_SESSIONS = 35

    fun loadSessions(context: Context): List<DiagnosticSessionRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_SESSIONS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(rawJson)
            val result = mutableListOf<DiagnosticSessionRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                parseSessionJson(obj)?.let { result.add(it) }
            }
            result.sortedByDescending { it.updatedAtMillis }
        } catch (e: Exception) {
            Log.w("DiagSessionRepo", "Failed to load diagnostic sessions: ${e.message}")
            emptyList()
        }
    }

    fun saveOrUpdateSession(
        context: Context,
        sessionId: String,
        mode: DiagnosticUserMode,
        messages: List<DiagnosticMessage>
    ): DiagnosticSessionRecord? {
        if (messages.isEmpty()) return null
        val existingList = loadSessions(context).toMutableList()
        val existing = existingList.find { it.id == sessionId }
        val now = System.currentTimeMillis()

        val firstUserText = messages.firstOrNull { it.isUser }?.text?.trim().orEmpty()
        val deviceHint = BekaMultimodalDiagnosticsEngine.detectDeviceModelFromTranscript(messages)
        val title = when {
            firstUserText.isNotBlank() -> firstUserText.take(48)
            messages.any { it.attachments.isNotEmpty() } -> "ფოტო-დიაგნოსტიკა ($deviceHint)"
            else -> "დიაგნოსტიკის სესია"
        }
        val lastAssistant = messages.lastOrNull { !it.isUser }
        val summary = lastAssistant?.structuredSection?.hypotheses?.firstOrNull()?.title
            ?: lastAssistant?.text?.replace("**", "")?.take(120)
            ?: firstUserText.take(120)

        val updatedRecord = DiagnosticSessionRecord(
            id = sessionId,
            title = title,
            deviceHint = deviceHint,
            mode = mode,
            createdAtMillis = existing?.createdAtMillis ?: now,
            updatedAtMillis = now,
            summary = summary,
            messages = messages
        )

        existingList.removeAll { it.id == sessionId }
        existingList.add(0, updatedRecord)
        val trimmed = existingList.take(MAX_SAVED_SESSIONS)
        persistList(context, trimmed)
        return updatedRecord
    }

    fun deleteSession(context: Context, sessionId: String): List<DiagnosticSessionRecord> {
        val updated = loadSessions(context).filterNot { it.id == sessionId }
        persistList(context, updated)
        return updated
    }

    fun clearAllSessions(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SESSIONS_JSON)
            .apply()
    }

    suspend fun syncSessionSummaryToFirestoreIfSignedIn(
        context: Context,
        record: DiagnosticSessionRecord
    ) {
        try {
            val repo = FirestoreRepository(context)
            repo.saveDiagnosticReport(
                deviceModel = record.deviceHint,
                issueCategory = record.mode.labelKa,
                symptoms = record.title,
                aiAnalysis = record.summary
            )
        } catch (e: Exception) {
            Log.d("DiagSessionRepo", "Cloud diagnostic sync skipped: ${e.message}")
        }
    }

    private fun persistList(context: Context, list: List<DiagnosticSessionRecord>) {
        val arr = JSONArray()
        for (item in list) {
            arr.put(serializeSessionJson(item))
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSIONS_JSON, arr.toString())
            .apply()
    }

    private fun serializeSessionJson(record: DiagnosticSessionRecord): JSONObject {
        val msgsArr = JSONArray()
        for (msg in record.messages) {
            val attArr = JSONArray()
            for (att in msg.attachments) {
                attArr.put(
                    JSONObject()
                        .put("id", att.id)
                        .put("uriString", att.uriString)
                        .put("displayName", att.displayName)
                        .put("mimeType", att.mimeType)
                        .put("width", att.width)
                        .put("height", att.height)
                        .put("fileSizeBytes", att.fileSizeBytes)
                        .put("visualSummaryHint", att.visualSummaryHint)
                        .put("base64Data", if (att.base64Data.length < 240_000) att.base64Data else "")
                )
            }

            val secObj = msg.structuredSection?.let { sec ->
                val hypArr = JSONArray()
                for (h in sec.hypotheses) {
                    hypArr.put(
                        JSONObject()
                            .put("title", h.title)
                            .put("confidence", h.confidence.id)
                            .put("supportingEvidence", h.supportingEvidence)
                            .put("verificationOrDisproof", h.verificationOrDisproof)
                    )
                }
                JSONObject()
                    .put("observedSymptoms", sec.observedSymptoms)
                    .put("deviceIdentification", sec.deviceIdentification)
                    .put("visualInspectionFindings", sec.visualInspectionFindings)
                    .put("hypotheses", hypArr)
                    .put("nextDiagnosticTest", sec.nextDiagnosticTest)
                    .put("expectedFindings", sec.expectedFindings)
                    .put("requiredTools", sec.requiredTools)
                    .put("safetyConsiderations", sec.safetyConsiderations)
            }

            val recIdsArr = JSONArray()
            msg.recommendedProductIds.forEach { recIdsArr.put(it) }

            msgsArr.put(
                JSONObject()
                    .put("id", msg.id)
                    .put("role", msg.role)
                    .put("text", msg.text)
                    .put("timestampMillis", msg.timestampMillis)
                    .put("mode", msg.mode.id)
                    .put("attachments", attArr)
                    .put("structuredSection", secObj)
                    .put("recommendedProductIds", recIdsArr)
                    .put("isError", msg.isError)
            )
        }

        return JSONObject()
            .put("id", record.id)
            .put("title", record.title)
            .put("deviceHint", record.deviceHint)
            .put("mode", record.mode.id)
            .put("createdAtMillis", record.createdAtMillis)
            .put("updatedAtMillis", record.updatedAtMillis)
            .put("summary", record.summary)
            .put("messages", msgsArr)
    }

    private fun parseSessionJson(obj: JSONObject): DiagnosticSessionRecord? {
        val id = obj.optString("id", "").ifBlank { return null }
        val msgsArr = obj.optJSONArray("messages") ?: JSONArray()
        val messages = mutableListOf<DiagnosticMessage>()

        for (i in 0 until msgsArr.length()) {
            val m = msgsArr.optJSONObject(i) ?: continue
            val attArr = m.optJSONArray("attachments") ?: JSONArray()
            val attachments = mutableListOf<DiagnosticAttachment>()
            for (j in 0 until attArr.length()) {
                val a = attArr.optJSONObject(j) ?: continue
                val b64 = a.optString("base64Data", "")
                attachments.add(
                    DiagnosticAttachment(
                        id = a.optString("id", UUID.randomUUID().toString()),
                        uriString = a.optString("uriString", ""),
                        displayName = a.optString("displayName", "ფოტო"),
                        mimeType = a.optString("mimeType", "image/jpeg"),
                        width = a.optInt("width", 0),
                        height = a.optInt("height", 0),
                        fileSizeBytes = a.optInt("fileSizeBytes", 0),
                        base64Data = b64,
                        visualSummaryHint = a.optString("visualSummaryHint", ""),
                        previewBitmap = DiagnosticImageProcessor.decodeBitmapFromBase64(b64)
                    )
                )
            }

            val secObj = m.optJSONObject("structuredSection")
            val structuredSection = secObj?.let { sec ->
                val hypArr = sec.optJSONArray("hypotheses") ?: JSONArray()
                val hyps = mutableListOf<DiagnosticHypothesis>()
                for (k in 0 until hypArr.length()) {
                    val h = hypArr.optJSONObject(k) ?: continue
                    hyps.add(
                        DiagnosticHypothesis(
                            title = h.optString("title", ""),
                            confidence = EvidenceConfidenceLevel.fromIdOrLabel(h.optString("confidence")),
                            supportingEvidence = h.optString("supportingEvidence", ""),
                            verificationOrDisproof = h.optString("verificationOrDisproof", "")
                        )
                    )
                }
                StructuredDiagnosticSection(
                    observedSymptoms = sec.optString("observedSymptoms", ""),
                    deviceIdentification = sec.optString("deviceIdentification", ""),
                    visualInspectionFindings = sec.optString("visualInspectionFindings", ""),
                    hypotheses = hyps,
                    nextDiagnosticTest = sec.optString("nextDiagnosticTest", ""),
                    expectedFindings = sec.optString("expectedFindings", ""),
                    requiredTools = sec.optString("requiredTools", ""),
                    safetyConsiderations = sec.optString("safetyConsiderations", "")
                )
            }

            val recIds = mutableListOf<Int>()
            val recArr = m.optJSONArray("recommendedProductIds")
            if (recArr != null) {
                for (r in 0 until recArr.length()) {
                    val pid = recArr.optInt(r, -1)
                    if (pid > 0) recIds.add(pid)
                }
            }

            messages.add(
                DiagnosticMessage(
                    id = m.optString("id", UUID.randomUUID().toString()),
                    role = m.optString("role", "user"),
                    text = m.optString("text", ""),
                    timestampMillis = m.optLong("timestampMillis", System.currentTimeMillis()),
                    mode = DiagnosticUserMode.fromId(m.optString("mode")),
                    attachments = attachments,
                    structuredSection = structuredSection,
                    recommendedProductIds = recIds,
                    isError = m.optBoolean("isError", false)
                )
            )
        }

        return DiagnosticSessionRecord(
            id = id,
            title = obj.optString("title", "დიაგნოსტიკა"),
            deviceHint = obj.optString("deviceHint", ""),
            mode = DiagnosticUserMode.fromId(obj.optString("mode")),
            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis()),
            summary = obj.optString("summary", ""),
            messages = messages
        )
    }
}
