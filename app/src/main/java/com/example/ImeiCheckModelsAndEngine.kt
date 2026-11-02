package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

// ==============================================================================
// 1. ENUMS & STATUS DEFINITIONS
// ==============================================================================

enum class ImeiRequestStatus(
    val id: String,
    val labelKa: String,
    val shortLabelKa: String,
    val badgeColorHex: Long,
    val descriptionKa: String
) {
    PENDING("pending", "მოლოდინში (Pending)", "მოლოდინში", 0xFFF59E0B, "მოთხოვნა მიღებულია და ელოდება სპეციალისტის მიერ შემოწმების დაწყებას."),
    IN_PROGRESS("in_progress", "მოწმდება (In Progress)", "მოწმდება", 0xFF38BDF8, "მოწყობილობის IMEI კოდი ამჟამად მოწმდება ოფიციალურ ბაზებში."),
    WAITING_FOR_INFO("waiting_for_information", "საჭიროა დაზუსტება", "დაზუსტება", 0xFFA855F7, "შემოწმების დასასრულებლად საჭიროა დამატებითი ინფორმაცია ან დაზუსტება."),
    COMPLETED("completed", "დასრულებული", "დასრულებული", 0xFF4ADE80, "ვერიფიკაცია დასრულებულია და სრული რეპორტი ხელმისაწვდომია."),
    REJECTED("rejected", "უარყოფილი", "უარყოფილი", 0xFFFF4D4F, "მოთხოვნა უარყოფილია (იხილეთ სპეციალისტის განმარტება).");

    companion object {
        fun fromId(raw: String?): ImeiRequestStatus {
            val clean = raw?.trim()?.lowercase().orEmpty()
            return entries.find { it.id == clean } ?: PENDING
        }
    }
}

enum class ImeiPaymentStatus(
    val id: String,
    val labelKa: String,
    val badgeColorHex: Long
) {
    UNPAID("unpaid", "გადასახდელი", 0xFF94A3B8),
    PENDING("pending", "გადახდა მოწმდება", 0xFFF59E0B),
    PAID("paid", "გადახდილია", 0xFF4ADE80),
    REFUNDED("refunded", "დაბრუნებული (Refund)", 0xFFF87171);

    companion object {
        fun fromId(raw: String?): ImeiPaymentStatus {
            val clean = raw?.trim()?.lowercase().orEmpty()
            return entries.find { it.id == clean } ?: PENDING
        }
    }
}

enum class ImeiResultStatus(
    val id: String,
    val labelKa: String,
    val badgeColorHex: Long,
    val iconSymbol: String,
    val shortBadgeKa: String
) {
    CONFIRMED_POSITIVE("confirmed_positive", "დადასტურებული (სუფთა / დადებითი)", 0xFF4ADE80, "✅", "✅ სუფთა / დადებითი"),
    CONFIRMED_NEGATIVE("confirmed_negative", "დაფიქსირდა შეზღუდვა / უარყოფითი", 0xFFFF4D4F, "⚠️", "⚠️ შეზღუდვა / უარყოფითი"),
    NOT_CHECKED("not_checked", "არ შემოწმებულა", 0xFF94A3B8, "➖", "➖ არ შემოწმებულა"),
    UNAVAILABLE("unavailable", "ინფორმაცია მიუწვდომელია", 0xFFF59E0B, "ℹ️", "ℹ️ მიუწვდომელია"),
    UNABLE_TO_VERIFY("unable_to_verify", "ვერიფიკაცია ვერ მოხერხდა", 0xFFFB923C, "❓", "❓ ვერ დადასტურდა");

    companion object {
        fun fromId(raw: String?): ImeiResultStatus {
            val clean = raw?.trim()?.lowercase().orEmpty()
            return entries.find { it.id == clean } ?: NOT_CHECKED
        }
    }
}

// ==============================================================================
// 2. DATA MODELS FOR IMEI CHECK PLATFORM
// ==============================================================================

data class ImeiServiceConfig(
    val serviceEnabled: Boolean = true,
    val acceptingRequests: Boolean = true,
    val entryCardVisible: Boolean = true,
    val priceGel: Double = 3.0,
    val currency: String = "GEL",
    val estimatedMinMinutes: Int = 5,
    val estimatedMaxMinutes: Int = 30,
    val title: String = "IMEI CHECK • სრული შემოწმება",
    val subtitle: String = "შეამოწმე სანამ იყიდი • შეამოწმე სრული ისტორია",
    val shortDescription: String = "შეამოწმე სანამ იყიდი, შეამოწმე სრული ისტორია — iCloud, SimLock, Blacklist და გარანტია.",
    val detailedDescription: String = "BEKASERVICE-ის სერტიფიცირებული სპეციალისტები ამოწმებენ მოწყობილობას ოფიციალურ საერთაშორისო და მწარმოებლის ბაზებში. მიიღებთ სრულ, სტრუქტურირებულ ანგარიშს პირდაპირ აპლიკაციაში.",
    val promoBadgeText: String = "IMEI CHECK • 3 ₾",
    val cardImageUrl: String = DEFAULT_CARD_IMAGE_URL,
    val userInstructions: String = "1. აკრიფეთ *#06# ტელეფონის ზარების აპლიკაციაში ან იპოვეთ 15-ნიშნა IMEI პარამეტრებში.\n2. შეიყვანეთ IMEI კოდი ქვემოთ მოცემულ ველში.\n3. აირჩიეთ მწარმოებელი ბრენდი და გამოაგზავნეთ მოთხოვნა.",
    val privacyNotice: String = "🔒 კონფიდენციალურობა და მონაცემთა დაცვა: თქვენი IMEI კოდი გამოიყენება მხოლოდ მოწყობილობის ტექნიკური სტატუსის შესამოწმებლად. ისტორიაში IMEI ნაგულისხმევად დაფარულია (******1234). ისტორიის წაშლისას სრული IMEI და პირადი რეპორტი იშლება, ხოლო კანონმდებლობით გათვალისწინებული ფინანსური აღრიცხვისთვის რჩება მხოლოდ ანონიმიზებული ტრანზაქციის ჩანაწერი IMEI-ს გარეშე.",
    val maintenanceMessage: String = "IMEI შემოწმების სერვისი დროებით ტექნიკურ განახლებაზეა. გთხოვთ სცადოთ მოგვიანებით.",
    val paymentInstructions: String = "გადახდა სრულდება საბანკო გადარიცხვით (BOG / TBC) ან WhatsApp დადასტურებით.",
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val formattedPrice: String
        get() {
            val clean = if (priceGel % 1.0 == 0.0) priceGel.toInt().toString() else String.format(Locale.US, "%.2f", priceGel)
            val sym = if (currency.equals("GEL", ignoreCase = true) || currency == "₾") "₾" else currency
            return "$clean $sym"
        }

    val formattedEstimatedTime: String
        get() = "${estimatedMinMinutes}–${estimatedMaxMinutes} წთ"

    companion object {
        const val DEFAULT_CARD_IMAGE_URL = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800"
    }
}

data class ImeiBrandConfig(
    val id: String = "",
    val name: String = "",
    val localizedName: String = "",
    val logoUrl: String = "",
    val iconEmoji: String = "📱",
    val displayOrder: Int = 0,
    val enabled: Boolean = true,
    val isPopular: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class ImeiResultFieldDefinition(
    val id: String = "",
    val titleKa: String = "",
    val descriptionKa: String = "",
    val supportedBrands: List<String> = listOf("ALL"), // "ALL" or specific brand names/IDs
    val displayOrder: Int = 0,
    val enabled: Boolean = true,
    val isImportant: Boolean = false,
    val inputType: String = "text_and_status", // "text_and_status", "status_only", "date"
    val availableStatuses: List<String> = ImeiResultStatus.entries.map { it.id },
    val internalInstructions: String = "",
    val sourceIds: List<String> = emptyList(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val applicableBrands: List<String>
        get() = supportedBrands

    fun appliesToBrand(brandNameOrId: String): Boolean {
        if (supportedBrands.isEmpty() || supportedBrands.any { it.equals("ALL", ignoreCase = true) || it == "ყველა" }) {
            return true
        }
        return supportedBrands.any { it.equals(brandNameOrId.trim(), ignoreCase = true) }
    }
}

data class ImeiVerificationSource(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val websiteUrl: String = "",
    val supportedBrands: List<String> = listOf("ALL"),
    val supportedFields: List<String> = emptyList(),
    val internalInstructions: String = "",
    val active: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class ImeiResultFieldValue(
    val fieldId: String = "",
    val titleKa: String = "",
    val descriptionKa: String = "",
    val value: String = "",
    val resultStatus: String = ImeiResultStatus.NOT_CHECKED.id,
    val isImportant: Boolean = false,
    val sourceName: String = ""
) {
    val statusEnum: ImeiResultStatus
        get() = ImeiResultStatus.fromId(resultStatus)
}

data class ImeiCheckRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val imei: String = "",
    val imeiMasked: String = "",
    val imeiHash: String = "",
    val tac: String = "",
    val brandId: String = "",
    val brandName: String = "",
    val verifiedModel: String = "",
    val status: String = ImeiRequestStatus.PENDING.id,
    val paymentStatus: String = ImeiPaymentStatus.PENDING.id,
    val quotedPrice: Double = 3.0,
    val currency: String = "GEL",
    val estimatedMinMinutes: Int = 5,
    val estimatedMaxMinutes: Int = 30,
    val internalNotes: String = "",
    val customerNotes: String = "",
    val draftResultText: String = "",
    val resultText: String = "",
    val isPublished: Boolean = false,
    val userDeletedHistory: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long = 0L,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val statusEnum: ImeiRequestStatus
        get() = ImeiRequestStatus.fromId(status)

    val paymentStatusEnum: ImeiPaymentStatus
        get() = ImeiPaymentStatus.fromId(paymentStatus)

    val formattedPrice: String
        get() {
            val clean = if (quotedPrice % 1.0 == 0.0) quotedPrice.toInt().toString() else String.format(Locale.US, "%.2f", quotedPrice)
            val sym = if (currency.equals("GEL", ignoreCase = true) || currency == "₾") "₾" else currency
            return "$clean $sym"
        }

    val formattedDate: String
        get() = ImeiCheckManager.formatTimestampKa(createdAtMillis)

    val estimatedTimeText: String
        get() = "${estimatedMinMinutes}–${estimatedMaxMinutes} წთ"
}

data class ImeiVerificationReport(
    val requestId: String = "",
    val userId: String = "",
    val imeiMasked: String = "",
    val imeiHash: String = "",
    val brandId: String = "",
    val brandName: String = "",
    val verifiedModel: String = "",
    val status: String = ImeiRequestStatus.COMPLETED.id,
    val paymentStatus: String = ImeiPaymentStatus.PENDING.id,
    val quotedPrice: Double = 3.0,
    val currency: String = "GEL",
    val fields: List<ImeiResultFieldValue> = emptyList(),
    val sourcesUsed: List<String> = emptyList(),
    val customerNotes: String = "",
    val draftResultText: String = "",
    val resultText: String = "",
    val isPublished: Boolean = true,
    val verifiedAtMillis: Long = System.currentTimeMillis(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val statusEnum: ImeiRequestStatus
        get() = ImeiRequestStatus.fromId(status)

    val paymentStatusEnum: ImeiPaymentStatus
        get() = ImeiPaymentStatus.fromId(paymentStatus)

    val formattedPrice: String
        get() {
            val clean = if (quotedPrice % 1.0 == 0.0) quotedPrice.toInt().toString() else String.format(Locale.US, "%.2f", quotedPrice)
            val sym = if (currency.equals("GEL", ignoreCase = true) || currency == "₾") "₾" else currency
            return "$clean $sym"
        }
}

data class ImeiHistoryEntry(
    val id: String = "",
    val userId: String = "",
    val imeiMasked: String = "",
    val imeiHash: String = "",
    val brandId: String = "",
    val brandName: String = "",
    val verifiedModel: String = "",
    val status: String = ImeiRequestStatus.PENDING.id,
    val paymentStatus: String = ImeiPaymentStatus.PENDING.id,
    val quotedPrice: Double = 3.0,
    val currency: String = "GEL",
    val estimatedTimeText: String = "5–30 წთ",
    val customerNotes: String = "",
    val draftResultText: String = "",
    val resultText: String = "",
    val isPublished: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long = 0L,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val statusEnum: ImeiRequestStatus
        get() = ImeiRequestStatus.fromId(status)

    val paymentStatusEnum: ImeiPaymentStatus
        get() = ImeiPaymentStatus.fromId(paymentStatus)

    val formattedPrice: String
        get() {
            val clean = if (quotedPrice % 1.0 == 0.0) quotedPrice.toInt().toString() else String.format(Locale.US, "%.2f", quotedPrice)
            val sym = if (currency.equals("GEL", ignoreCase = true) || currency == "₾") "₾" else currency
            return "$clean $sym"
        }
}

data class ImeiAnalyticsComputed(
    val totalRequests: Int = 0,
    val requestsToday: Int = 0,
    val requestsThisWeek: Int = 0,
    val requestsThisMonth: Int = 0,
    val completedRequests: Int = 0,
    val pendingRequests: Int = 0,
    val inProgressRequests: Int = 0,
    val waitingForInfoRequests: Int = 0,
    val rejectedRequests: Int = 0,
    val overdueRequests: Int = 0,
    val averageProcessingMinutes: Int = 0,
    val medianProcessingMinutes: Int = 0,
    val confirmedPaidRevenueGel: Double = 0.0,
    val revenueTodayGel: Double = 0.0,
    val revenueThisWeekGel: Double = 0.0,
    val revenueThisMonthGel: Double = 0.0,
    val paidChecksCount: Int = 0,
    val averageRevenuePerPaidGel: Double = 0.0,
    val refundedTotalGel: Double = 0.0,
    val outstandingPaymentRequestsCount: Int = 0,
    val outstandingPaymentTotalGel: Double = 0.0,
    val brandRequestCounts: Map<String, Int> = emptyMap(),
    val brandCompletionRates: Map<String, Int> = emptyMap(),
    val sourceUsageCounts: Map<String, Int> = emptyMap(),
    val fieldAvailabilityRates: Map<String, Int> = emptyMap(),
    val fieldUnavailableCounts: Map<String, Int> = emptyMap()
) {
    val todayRequests: Int
        get() = requestsToday

    val last7DaysRequests: Int
        get() = requestsThisWeek

    val last30DaysRequests: Int
        get() = requestsThisMonth

    val averageRevenuePerCompletedCheckGel: Double
        get() = averageRevenuePerPaidGel

    val conversionRatePercent: Double
        get() = if (totalRequests > 0) (paidChecksCount * 100.0) / totalRequests else 0.0

    val mostCheckedManufacturer: String
        get() = brandRequestCounts.entries.maxByOrNull { it.value }?.key ?: ""

    val unavailableOrUnverifiedFieldCounts: Map<String, Int>
        get() = fieldUnavailableCounts
}

// ==============================================================================
// 3. IMEI VALIDATION & CRYPTOGRAPHIC FINGERPRINT ENGINE
// ==============================================================================

data class ImeiValidationResult(
    val normalizedDigits: String,
    val isCompleteLength: Boolean,
    val isLuhnValid: Boolean,
    val isValid: Boolean,
    val tac: String,
    val messageKa: String?
) {
    val cleanedDigits: String
        get() = normalizedDigits

    val isFifteenDigits: Boolean
        get() = isCompleteLength

    val errorMessageKa: String?
        get() = if (isValid) null else messageKa
}

object ImeiValidator {
    fun normalizeDigits(raw: String): String {
        return raw.filter { it.isDigit() }.take(15)
    }

    fun validateImei(raw: String): ImeiValidationResult = validateInput(raw)

    fun generateValidImei(first14: String): String = generateValidImeiFrom14Digits(first14)

    /**
     * სტანდარტული Luhn (Mod 10) ალგორითმი 15-ნიშნა IMEI კოდის საკონტროლო ციფრის შესამოწმებლად.
     */
    fun validateLuhn(imei: String): Boolean {
        val clean = imei.trim()
        if (clean.length != 15 || !clean.all { it.isDigit() }) return false
        if (clean.all { it == '0' }) return false

        var sum = 0
        for (i in 0 until 15) {
            var digit = clean[i].digitToInt()
            // 0-indexed: positions 1, 3, 5, 7, 9, 11, 13 are doubled (every second digit from left)
            if (i % 2 == 1) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }
            sum += digit
        }
        return sum % 10 == 0
    }

    fun generateValidImeiFrom14Digits(first14: String): String {
        val clean = first14.filter { it.isDigit() }.take(14).padEnd(14, '0')
        var sum = 0
        for (i in 0 until 14) {
            var digit = clean[i].digitToInt()
            if (i % 2 == 1) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return clean + checkDigit.toString()
    }

    fun validateInput(raw: String): ImeiValidationResult {
        val digits = normalizeDigits(raw)
        if (digits.isEmpty()) {
            return ImeiValidationResult(
                normalizedDigits = "",
                isCompleteLength = false,
                isLuhnValid = false,
                isValid = false,
                tac = "",
                messageKa = null
            )
        }
        if (digits.length < 15) {
            return ImeiValidationResult(
                normalizedDigits = digits,
                isCompleteLength = false,
                isLuhnValid = false,
                isValid = false,
                tac = digits.take(8),
                messageKa = "შეყვანილია ${digits.length}/15 ციფრი. IMEI უნდა შედგებოდეს ზუსტად 15 ციფრისგან."
            )
        }
        val luhnOk = validateLuhn(digits)
        return if (!luhnOk) {
            ImeiValidationResult(
                normalizedDigits = digits,
                isCompleteLength = true,
                isLuhnValid = false,
                isValid = false,
                tac = digits.take(8),
                messageKa = "❌ IMEI კოდის საკონტროლო ციფრი (Luhn Checksum) არასწორია. გთხოვთ გადაამოწმოთ აკრეფილი 15 ციფრი (*#06#)."
            )
        } else {
            ImeiValidationResult(
                normalizedDigits = digits,
                isCompleteLength = true,
                isLuhnValid = true,
                isValid = true,
                tac = digits.take(8),
                messageKa = "✅ IMEI ფორმატი და Luhn საკონტროლო ჯამი სწორია (TAC: ${digits.take(8)}). გაითვალისწინეთ: მხოლოდ ფორმატის სისწორე არ ნიშნავს, რომ ტელეფონი არ არის შავ სიაში ან დაბლოკილი."
            )
        }
    }

    fun maskImei(imei: String): String {
        val clean = imei.filter { it.isDigit() }
        if (clean.length < 4) return "******"
        return "******${clean.takeLast(4)}"
    }

    fun computeUserScopedImeiHash(userId: String, imei: String): String {
        val cleanImei = imei.filter { it.isDigit() }
        val payload = "bekaservice_imei_v1:${userId.trim().lowercase()}:$cleanImei"
        return try {
            val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }.take(40)
        } catch (_: Exception) {
            "hash_${userId.hashCode()}_${cleanImei.hashCode()}"
        }
    }
}

// ==============================================================================
// 4. IMEI CHECK CENTRAL REPOSITORY & STATE MANAGER
// ==============================================================================

object ImeiCheckManager {
    private const val PREFS_NAME = "bekaservice_imei_platform_prefs_v1"
    private const val KEY_CONFIG_JSON = "imei_service_config_json"
    private const val KEY_BRANDS_JSON = "imei_brands_json"
    private const val KEY_FIELDS_JSON = "imei_result_fields_json"
    private const val KEY_SOURCES_JSON = "imei_sources_json"
    private const val KEY_REQUESTS_JSON = "imei_requests_cache_json"
    private const val KEY_REPORTS_JSON = "imei_reports_cache_json"
    private const val KEY_USER_HISTORY_PREFIX = "imei_user_history_"

    val stateVersion = mutableIntStateOf(0)

    fun notifyStateChanged() {
        stateVersion.intValue += 1
    }

    fun resolveCurrentUserId(context: Context, currentUser: UserProfile? = null): String {
        if (currentUser != null && currentUser.id.isNotBlank()) return currentUser.id
        val fbUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull().orEmpty()
        if (fbUid.isNotBlank()) return fbUid
        val localUser = AuthAndOrderManager.getCurrentUser(context)
        return localUser?.id.orEmpty()
    }

    fun formatTimestampKa(millis: Long): String {
        if (millis <= 0L) return "—"
        return try {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("ka")).format(Date(millis))
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(millis))
        }
    }

    // --------------------------------------------------------------------------
    // DEFAULT INITIAL CONFIGURATIONS (Phase 3 & Phase 4)
    // --------------------------------------------------------------------------

    fun getDefaultBrands(): List<ImeiBrandConfig> = listOf(
        ImeiBrandConfig(id = "apple", name = "Apple", localizedName = "Apple", iconEmoji = "Apple", displayOrder = 1, enabled = true, isPopular = true),
        ImeiBrandConfig(id = "samsung", name = "Samsung", localizedName = "Samsung", iconEmoji = "Samsung", displayOrder = 2, enabled = true, isPopular = true),
        ImeiBrandConfig(id = "xiaomi", name = "Xiaomi", localizedName = "Xiaomi", iconEmoji = "MI", displayOrder = 3, enabled = true, isPopular = true),
        ImeiBrandConfig(id = "google_pixel", name = "Google Pixel", localizedName = "Google Pixel", iconEmoji = "G", displayOrder = 4, enabled = true, isPopular = true),
        ImeiBrandConfig(id = "huawei", name = "Huawei", localizedName = "Huawei", iconEmoji = "Huawei", displayOrder = 5, enabled = true, isPopular = false),
        ImeiBrandConfig(id = "honor", name = "Honor", localizedName = "Honor", iconEmoji = "HONOR", displayOrder = 6, enabled = true, isPopular = false),
        ImeiBrandConfig(id = "oneplus", name = "OnePlus", localizedName = "OnePlus", iconEmoji = "1+", displayOrder = 7, enabled = true, isPopular = false),
        ImeiBrandConfig(id = "motorola", name = "Motorola", localizedName = "Motorola", iconEmoji = "M", displayOrder = 8, enabled = true, isPopular = false),
        ImeiBrandConfig(id = "oppo", name = "Oppo", localizedName = "Oppo", iconEmoji = "OPPO", displayOrder = 9, enabled = true, isPopular = false),
        ImeiBrandConfig(id = "vivo", name = "Vivo", localizedName = "Vivo", iconEmoji = "vivo", displayOrder = 10, enabled = true, isPopular = false),
        ImeiBrandConfig(id = "realme", name = "Realme", localizedName = "Realme", iconEmoji = "R", displayOrder = 11, enabled = true, isPopular = false),
        ImeiBrandConfig(id = "other", name = "Other", localizedName = "სხვა ბრენდი", iconEmoji = "IMEI", displayOrder = 12, enabled = true, isPopular = false)
    )

    fun getDefaultResultFields(): List<ImeiResultFieldDefinition> = listOf(
        ImeiResultFieldDefinition(
            id = "manufacturer",
            titleKa = "მწარმოებელი (Manufacturer)",
            descriptionKa = "მოწყობილობის ოფიციალური მწარმოებელი ბრენდი",
            supportedBrands = listOf("ALL"),
            displayOrder = 1,
            enabled = true,
            isImportant = false,
            internalInstructions = "მიუთითეთ ოფიციალური მწარმოებელი"
        ),
        ImeiResultFieldDefinition(
            id = "exact_model",
            titleKa = "ზუსტი მოდელი (Exact Device Model)",
            descriptionKa = "მწარმოებლის ბაზაში რეგისტრირებული ზუსტი მოდელი",
            supportedBrands = listOf("ALL"),
            displayOrder = 2,
            enabled = true,
            isImportant = true,
            internalInstructions = "მაგ: iPhone 15 Pro Max 256GB Natural Titanium"
        ),
        ImeiResultFieldDefinition(
            id = "model_number",
            titleKa = "მოდელის კოდი (Model Number)",
            descriptionKa = "ქარხნული მოდელის იდენტიფიკატორი (მაგ: A2849 / SM-S928B)",
            supportedBrands = listOf("ALL"),
            displayOrder = 3,
            enabled = true,
            isImportant = false,
            internalInstructions = "მიუთითეთ ქარხნული მოდელის ნომერი"
        ),
        ImeiResultFieldDefinition(
            id = "tac_code",
            titleKa = "TAC კოდი",
            descriptionKa = "Type Allocation Code (პირველი 8 ციფრი)",
            supportedBrands = listOf("ALL"),
            displayOrder = 4,
            enabled = true,
            isImportant = false,
            internalInstructions = "IMEI-ს პირველი 8 ციფრი"
        ),
        ImeiResultFieldDefinition(
            id = "imei_validity",
            titleKa = "IMEI ვალიდურობა",
            descriptionKa = "GSMA რეესტრში IMEI კოდის სტრუქტურული და რეგისტრაციული სტატუსი",
            supportedBrands = listOf("ALL"),
            displayOrder = 5,
            enabled = true,
            isImportant = true,
            internalInstructions = "დაადასტურეთ GSMA/TAC შესაბამისობა"
        ),
        ImeiResultFieldDefinition(
            id = "blacklist_status",
            titleKa = "შავი სიის სტატუსი (Blacklist Status)",
            descriptionKa = "საერთაშორისო GSMA Blacklist რეესტრში სტატუსი (Clean / Blacklisted)",
            supportedBrands = listOf("ALL"),
            displayOrder = 6,
            enabled = true,
            isImportant = true,
            internalInstructions = "Clean -> დადასტურებული დადებითი; Blacklisted -> დაფიქსირდა შეზღუდვა"
        ),
        ImeiResultFieldDefinition(
            id = "lost_stolen_status",
            titleKa = "დაკარგული / მოპარული სტატუსი (Lost / Stolen)",
            descriptionKa = "არის თუ არა მოწყობილობა გამოცხადებული დაკარგულად ან მოპარულად",
            supportedBrands = listOf("ALL"),
            displayOrder = 7,
            enabled = true,
            isImportant = true,
            internalInstructions = "შეამოწმეთ Lost/Stolen რეესტრი"
        ),
        ImeiResultFieldDefinition(
            id = "sim_lock_status",
            titleKa = "SIM-Lock / ოპერატორის ბლოკი",
            descriptionKa = "არის თუ არა ტელეფონი მიბმული კონკრეტულ ქსელზე (Unlocked / Locked)",
            supportedBrands = listOf("ALL"),
            displayOrder = 8,
            enabled = true,
            isImportant = true,
            internalInstructions = "Unlocked -> დადასტურებული დადებითი; Carrier Locked -> დაფიქსირდა შეზღუდვა"
        ),
        ImeiResultFieldDefinition(
            id = "carrier_info",
            titleKa = "ოპერატორის ინფორმაცია (Carrier Info)",
            descriptionKa = "საწყისი ან მიმდინარე მობილური ოპერატორი",
            supportedBrands = listOf("ALL"),
            displayOrder = 9,
            enabled = true,
            isImportant = false,
            internalInstructions = "მაგ: Unlocked / AT&T / T-Mobile / यूरोپული რეგიონი"
        ),
        ImeiResultFieldDefinition(
            id = "network_restriction",
            titleKa = "ქსელური შეზღუდვა (Network Restriction)",
            descriptionKa = "ოპერატორის ფინანსური ან ქსელური შეზღუდვის სტატუსი",
            supportedBrands = listOf("ALL"),
            displayOrder = 10,
            enabled = true,
            isImportant = false,
            internalInstructions = "შეამოწმეთ Unpaid bills / Financial block"
        ),
        ImeiResultFieldDefinition(
            id = "icloud_fmi_status",
            titleKa = "iCloud / Find My iPhone (Activation Lock)",
            descriptionKa = "Apple Find My (FMI ON / OFF) და iCloud ბლოკირების სტატუსი",
            supportedBrands = listOf("Apple"),
            displayOrder = 11,
            enabled = true,
            isImportant = true,
            internalInstructions = "FMI OFF -> დადებითი; FMI ON -> უარყოფითი"
        ),
        ImeiResultFieldDefinition(
            id = "knox_guard_status",
            titleKa = "Knox Guard / KG სტატუსი",
            descriptionKa = "Samsung Knox Guard / MDM კორპორატიული ან განვადების ბლოკი",
            supportedBrands = listOf("Samsung"),
            displayOrder = 12,
            enabled = true,
            isImportant = true,
            internalInstructions = "KG Active / Locked ან Clean"
        ),
        ImeiResultFieldDefinition(
            id = "frp_status",
            titleKa = "Google FRP / Mi Account სტატუსი",
            descriptionKa = "Factory Reset Protection ან Mi Cloud ანგარიშის ბლოკირება",
            supportedBrands = listOf("Samsung", "Xiaomi", "Google Pixel", "Huawei", "Honor", "OnePlus", "Motorola", "Oppo", "Vivo", "Realme", "Other"),
            displayOrder = 13,
            enabled = true,
            isImportant = true,
            internalInstructions = "შეამოწმეთ FRP / Mi Cloud სტატუსი"
        ),
        ImeiResultFieldDefinition(
            id = "warranty_status",
            titleKa = "გარანტიის სტატუსი (Warranty Status)",
            descriptionKa = "მწარმოებლის ოფიციალური გარანტიის მოქმედების სტატუსი",
            supportedBrands = listOf("ALL"),
            displayOrder = 14,
            enabled = true,
            isImportant = false,
            internalInstructions = "Active / Expired"
        ),
        ImeiResultFieldDefinition(
            id = "warranty_expiration",
            titleKa = "გარანტიის ვადის გასვლის თარიღი",
            descriptionKa = "ოფიციალური გარანტიის დასრულების სავარაუდო თარიღი",
            supportedBrands = listOf("ALL"),
            displayOrder = 15,
            enabled = true,
            isImportant = false,
            internalInstructions = "მიუთითეთ თარიღი თუ ხელმისაწვდომია"
        ),
        ImeiResultFieldDefinition(
            id = "production_country",
            titleKa = "წარმოების ქვეყანა / რეგიონი",
            descriptionKa = "ქარხნული წარმოების ან რეალიზაციის რეგიონი",
            supportedBrands = listOf("ALL"),
            displayOrder = 16,
            enabled = true,
            isImportant = false,
            internalInstructions = "მაგ: Europe / USA / China / Vietnam"
        ),
        ImeiResultFieldDefinition(
            id = "production_date",
            titleKa = "წარმოების / აქტივაციის თარიღი",
            descriptionKa = "მოწყობილობის წარმოების ან პირველი აქტივაციის თარიღი",
            supportedBrands = listOf("ALL"),
            displayOrder = 17,
            enabled = true,
            isImportant = false,
            internalInstructions = "მიუთითეთ პირველი აქტივაციის თარიღი"
        ),
        ImeiResultFieldDefinition(
            id = "additional_device_info",
            titleKa = "დამატებითი ინფორმაცია მოწყობილობაზე",
            descriptionKa = "სხვა ტექნიკური დეტალები (ფერი, მეხსიერება, MDM, Refurbished სტატუსი)",
            supportedBrands = listOf("ALL"),
            displayOrder = 18,
            enabled = true,
            isImportant = false,
            internalInstructions = "დამატებითი დეტალები"
        )
    )

    fun getDefaultSources(): List<ImeiVerificationSource> = listOf(
        ImeiVerificationSource(
            id = "apple_official_coverage",
            name = "Apple Official Coverage & GSX Check",
            description = "Apple-ის ოფიციალური გარანტიის, აქტივაციის, FMI და SIM-Lock სტატუსის ბაზა",
            websiteUrl = "https://checkcoverage.apple.com",
            supportedBrands = listOf("Apple"),
            supportedFields = listOf("exact_model", "model_number", "icloud_fmi_status", "sim_lock_status", "warranty_status", "warranty_expiration"),
            internalInstructions = "შეამოწმეთ FMI სტატუსი, Carrier Lock და AppleCare ვადა.",
            active = true
        ),
        ImeiVerificationSource(
            id = "samsung_knox_registry",
            name = "Samsung Official & Knox Guard Registry",
            description = "Samsung-ის მოდელის, რეგიონის, გარანტიისა და Knox Guard სტატუსის ბაზა",
            websiteUrl = "https://www.samsung.com",
            supportedBrands = listOf("Samsung"),
            supportedFields = listOf("exact_model", "model_number", "knox_guard_status", "sim_lock_status", "warranty_status", "production_country"),
            internalInstructions = "გადაამოწმეთ SM-კოდი, CSC რეგიონი და Knox Guard სტატუსი.",
            active = true
        ),
        ImeiVerificationSource(
            id = "xiaomi_mi_verification",
            name = "Xiaomi Official Product Authentication",
            description = "Xiaomi / Redmi / POCO ოფიციალური აქტივაციის, რეგიონის და Mi Cloud ბაზა",
            websiteUrl = "https://www.mi.com/global/verify/#/en/tab/imei",
            supportedBrands = listOf("Xiaomi"),
            supportedFields = listOf("exact_model", "frp_status", "production_country", "production_date", "warranty_status"),
            internalInstructions = "შეამოწმეთ Global vs CN ვერსია და Mi Lock სტატუსი.",
            active = true
        ),
        ImeiVerificationSource(
            id = "gsma_blacklist_tac",
            name = "GSMA TAC & International Blacklist Registry",
            description = "საერთაშორისო GSMA TAC იდენტიფიკაცია და Blacklist / Lost-Stolen რეესტრი",
            websiteUrl = "https://www.gsma.com",
            supportedBrands = listOf("ALL"),
            supportedFields = listOf("manufacturer", "tac_code", "imei_validity", "blacklist_status", "lost_stolen_status", "network_restriction"),
            internalInstructions = "შეამოწმეთ TAC კოდი და საერთაშორისო შავი სია.",
            active = true
        )
    )

    // --------------------------------------------------------------------------
    // LOCAL STORAGE CACHE SERIALIZATION & DESERIALIZATION
    // --------------------------------------------------------------------------

    fun loadConfig(context: Context): ImeiServiceConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CONFIG_JSON, null) ?: return ImeiServiceConfig()
        return try {
            val obj = JSONObject(raw)
            val rawSubtitle = obj.optString("subtitle", ImeiServiceConfig().subtitle)
            val normalizedSubtitle = if (
                rawSubtitle.isBlank() ||
                rawSubtitle.contains("ოფიციალური სტატუსის, ბლოკირების")
            ) {
                "შეამოწმე სანამ იყიდი • შეამოწმე სრული ისტორია"
            } else {
                rawSubtitle
            }
            val rawShortDesc = obj.optString("shortDescription", ImeiServiceConfig().shortDescription)
            val normalizedShortDesc = if (
                rawShortDesc.isBlank() ||
                rawShortDesc.startsWith("შეამოწმეთ ტელეფონის სისუფთავე, ოპერატორის ბლოკი")
            ) {
                ImeiServiceConfig().shortDescription
            } else {
                rawShortDesc
            }
            ImeiServiceConfig(
                serviceEnabled = obj.optBoolean("serviceEnabled", true),
                acceptingRequests = obj.optBoolean("acceptingRequests", true),
                entryCardVisible = obj.optBoolean("entryCardVisible", true),
                priceGel = obj.optDouble("priceGel", 3.0).takeIf { !it.isNaN() && it >= 0.0 } ?: 3.0,
                currency = obj.optString("currency", "GEL").ifBlank { "GEL" },
                estimatedMinMinutes = obj.optInt("estimatedMinMinutes", 5).coerceAtLeast(1),
                estimatedMaxMinutes = obj.optInt("estimatedMaxMinutes", 30).coerceAtLeast(1),
                title = obj.optString("title", "IMEI CHECK • სრული შემოწმება").ifBlank { "IMEI CHECK • სრული შემოწმება" },
                subtitle = normalizedSubtitle,
                shortDescription = normalizedShortDesc,
                detailedDescription = obj.optString("detailedDescription", ImeiServiceConfig().detailedDescription),
                promoBadgeText = obj.optString("promoBadgeText", "IMEI CHECK • 3 ₾"),
                cardImageUrl = obj.optString("cardImageUrl", ImeiServiceConfig.DEFAULT_CARD_IMAGE_URL).ifBlank { ImeiServiceConfig.DEFAULT_CARD_IMAGE_URL },
                userInstructions = obj.optString("userInstructions", ImeiServiceConfig().userInstructions),
                privacyNotice = obj.optString("privacyNotice", ImeiServiceConfig().privacyNotice),
                maintenanceMessage = obj.optString("maintenanceMessage", ImeiServiceConfig().maintenanceMessage),
                paymentInstructions = obj.optString("paymentInstructions", ImeiServiceConfig().paymentInstructions),
                updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            ImeiServiceConfig()
        }
    }

    fun saveConfigLocal(context: Context, config: ImeiServiceConfig) {
        val obj = JSONObject().apply {
            put("serviceEnabled", config.serviceEnabled)
            put("acceptingRequests", config.acceptingRequests)
            put("entryCardVisible", config.entryCardVisible)
            put("priceGel", config.priceGel)
            put("currency", config.currency)
            put("estimatedMinMinutes", config.estimatedMinMinutes)
            put("estimatedMaxMinutes", config.estimatedMaxMinutes)
            put("title", config.title)
            put("subtitle", config.subtitle)
            put("shortDescription", config.shortDescription)
            put("detailedDescription", config.detailedDescription)
            put("promoBadgeText", config.promoBadgeText)
            put("cardImageUrl", config.cardImageUrl)
            put("userInstructions", config.userInstructions)
            put("privacyNotice", config.privacyNotice)
            put("maintenanceMessage", config.maintenanceMessage)
            put("paymentInstructions", config.paymentInstructions)
            put("updatedAtMillis", config.updatedAtMillis)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONFIG_JSON, obj.toString())
            .apply()
        notifyStateChanged()
    }

    fun loadBrands(context: Context): List<ImeiBrandConfig> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_BRANDS_JSON, null) ?: return getDefaultBrands()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ImeiBrandConfig>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    ImeiBrandConfig(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        localizedName = o.optString("localizedName", o.optString("name")),
                        logoUrl = o.optString("logoUrl", ""),
                        iconEmoji = o.optString("iconEmoji", "📱"),
                        displayOrder = o.optInt("displayOrder", i + 1),
                        enabled = o.optBoolean("enabled", true),
                        isPopular = o.optBoolean("isPopular", false),
                        updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                    )
                )
            }
            if (list.isEmpty()) getDefaultBrands() else list.sortedBy { it.displayOrder }
        } catch (_: Exception) {
            getDefaultBrands()
        }
    }

    fun saveBrandsLocal(context: Context, brands: List<ImeiBrandConfig>) {
        val arr = JSONArray()
        brands.sortedBy { it.displayOrder }.forEach { b ->
            arr.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("localizedName", b.localizedName)
                    put("logoUrl", b.logoUrl)
                    put("iconEmoji", b.iconEmoji)
                    put("displayOrder", b.displayOrder)
                    put("enabled", b.enabled)
                    put("isPopular", b.isPopular)
                    put("updatedAtMillis", b.updatedAtMillis)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BRANDS_JSON, arr.toString())
            .apply()
        notifyStateChanged()
    }

    fun loadResultFields(context: Context): List<ImeiResultFieldDefinition> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_FIELDS_JSON, null) ?: return getDefaultResultFields()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ImeiResultFieldDefinition>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val brandsArr = o.optJSONArray("supportedBrands")
                val brandsList = mutableListOf<String>()
                if (brandsArr != null) {
                    for (j in 0 until brandsArr.length()) {
                        brandsList.add(brandsArr.optString(j))
                    }
                }
                val statusesArr = o.optJSONArray("availableStatuses")
                val statusesList = mutableListOf<String>()
                if (statusesArr != null) {
                    for (j in 0 until statusesArr.length()) {
                        statusesList.add(statusesArr.optString(j))
                    }
                }
                val sourcesArr = o.optJSONArray("sourceIds")
                val sourcesList = mutableListOf<String>()
                if (sourcesArr != null) {
                    for (j in 0 until sourcesArr.length()) {
                        sourcesList.add(sourcesArr.optString(j))
                    }
                }
                list.add(
                    ImeiResultFieldDefinition(
                        id = o.optString("id"),
                        titleKa = o.optString("titleKa"),
                        descriptionKa = o.optString("descriptionKa"),
                        supportedBrands = if (brandsList.isEmpty()) listOf("ALL") else brandsList,
                        displayOrder = o.optInt("displayOrder", i + 1),
                        enabled = o.optBoolean("enabled", true),
                        isImportant = o.optBoolean("isImportant", false),
                        inputType = o.optString("inputType", "text_and_status"),
                        availableStatuses = if (statusesList.isEmpty()) ImeiResultStatus.entries.map { it.id } else statusesList,
                        internalInstructions = o.optString("internalInstructions", ""),
                        sourceIds = sourcesList,
                        updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                    )
                )
            }
            if (list.isEmpty()) getDefaultResultFields() else list.sortedBy { it.displayOrder }
        } catch (_: Exception) {
            getDefaultResultFields()
        }
    }

    fun saveResultFieldsLocal(context: Context, fields: List<ImeiResultFieldDefinition>) {
        val arr = JSONArray()
        fields.sortedBy { it.displayOrder }.forEach { f ->
            arr.put(
                JSONObject().apply {
                    put("id", f.id)
                    put("titleKa", f.titleKa)
                    put("descriptionKa", f.descriptionKa)
                    put("supportedBrands", JSONArray(f.supportedBrands))
                    put("displayOrder", f.displayOrder)
                    put("enabled", f.enabled)
                    put("isImportant", f.isImportant)
                    put("inputType", f.inputType)
                    put("availableStatuses", JSONArray(f.availableStatuses))
                    put("internalInstructions", f.internalInstructions)
                    put("sourceIds", JSONArray(f.sourceIds))
                    put("updatedAtMillis", f.updatedAtMillis)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FIELDS_JSON, arr.toString())
            .apply()
        notifyStateChanged()
    }

    fun loadSources(context: Context): List<ImeiVerificationSource> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SOURCES_JSON, null) ?: return getDefaultSources()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ImeiVerificationSource>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val brandsArr = o.optJSONArray("supportedBrands")
                val brandsList = mutableListOf<String>()
                if (brandsArr != null) {
                    for (j in 0 until brandsArr.length()) {
                        brandsList.add(brandsArr.optString(j))
                    }
                }
                val fieldsArr = o.optJSONArray("supportedFields")
                val fieldsList = mutableListOf<String>()
                if (fieldsArr != null) {
                    for (j in 0 until fieldsArr.length()) {
                        fieldsList.add(fieldsArr.optString(j))
                    }
                }
                list.add(
                    ImeiVerificationSource(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        description = o.optString("description"),
                        websiteUrl = o.optString("websiteUrl"),
                        supportedBrands = if (brandsList.isEmpty()) listOf("ALL") else brandsList,
                        supportedFields = fieldsList,
                        internalInstructions = o.optString("internalInstructions"),
                        active = o.optBoolean("active", true),
                        updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                    )
                )
            }
            if (list.isEmpty()) getDefaultSources() else list
        } catch (_: Exception) {
            getDefaultSources()
        }
    }

    fun saveSourcesLocal(context: Context, sources: List<ImeiVerificationSource>) {
        val arr = JSONArray()
        sources.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("description", s.description)
                    put("websiteUrl", s.websiteUrl)
                    put("supportedBrands", JSONArray(s.supportedBrands))
                    put("supportedFields", JSONArray(s.supportedFields))
                    put("internalInstructions", s.internalInstructions)
                    put("active", s.active)
                    put("updatedAtMillis", s.updatedAtMillis)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SOURCES_JSON, arr.toString())
            .apply()
        notifyStateChanged()
    }

    private fun loadAllRequestsRawInternal(context: Context): List<ImeiCheckRequest> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REQUESTS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ImeiCheckRequest>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(parseRequestJson(o))
            }
            list.sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadAllRequestsLocal(context: Context): List<ImeiCheckRequest> {
        val activeEmail = AuthAndOrderManager.resolveActiveUserEmail(context)
        if (activeEmail.isNotBlank() && !AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return emptyList()
        }
        return loadAllRequestsRawInternal(context)
    }

    fun loadAllRequestsForAuthorizedAdmin(context: Context): List<ImeiCheckRequest> {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return emptyList()
        }
        return loadAllRequestsRawInternal(context)
    }

    fun saveAllRequestsLocal(context: Context, requests: List<ImeiCheckRequest>) {
        val arr = JSONArray()
        requests.sortedByDescending { it.createdAtMillis }.forEach { req ->
            arr.put(serializeRequestJson(req))
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REQUESTS_JSON, arr.toString())
            .apply()
        notifyStateChanged()
    }

    private fun serializeRequestJson(req: ImeiCheckRequest): JSONObject = JSONObject().apply {
        put("id", req.id)
        put("userId", req.userId)
        put("userName", req.userName)
        put("userEmail", req.userEmail)
        put("userPhone", req.userPhone)
        put("imei", req.imei)
        put("imeiMasked", req.imeiMasked)
        put("imeiHash", req.imeiHash)
        put("tac", req.tac)
        put("brandId", req.brandId)
        put("brandName", req.brandName)
        put("verifiedModel", req.verifiedModel)
        put("status", req.status)
        put("paymentStatus", req.paymentStatus)
        put("quotedPrice", req.quotedPrice)
        put("currency", req.currency)
        put("estimatedMinMinutes", req.estimatedMinMinutes)
        put("estimatedMaxMinutes", req.estimatedMaxMinutes)
        put("internalNotes", req.internalNotes)
        put("customerNotes", req.customerNotes)
        put("draftResultText", req.draftResultText)
        put("resultText", req.resultText)
        put("isPublished", req.isPublished)
        put("userDeletedHistory", req.userDeletedHistory)
        put("createdAtMillis", req.createdAtMillis)
        put("completedAtMillis", req.completedAtMillis)
        put("updatedAtMillis", req.updatedAtMillis)
    }

    private fun parseRequestJson(o: JSONObject): ImeiCheckRequest = ImeiCheckRequest(
        id = o.optString("id"),
        userId = o.optString("userId"),
        userName = o.optString("userName"),
        userEmail = o.optString("userEmail"),
        userPhone = o.optString("userPhone"),
        imei = o.optString("imei"),
        imeiMasked = o.optString("imeiMasked"),
        imeiHash = o.optString("imeiHash"),
        tac = o.optString("tac"),
        brandId = o.optString("brandId"),
        brandName = o.optString("brandName"),
        verifiedModel = o.optString("verifiedModel"),
        status = o.optString("status", ImeiRequestStatus.PENDING.id),
        paymentStatus = o.optString("paymentStatus", ImeiPaymentStatus.PENDING.id),
        quotedPrice = o.optDouble("quotedPrice", 3.0),
        currency = o.optString("currency", "GEL"),
        estimatedMinMinutes = o.optInt("estimatedMinMinutes", 5),
        estimatedMaxMinutes = o.optInt("estimatedMaxMinutes", 30),
        internalNotes = o.optString("internalNotes"),
        customerNotes = o.optString("customerNotes"),
        draftResultText = o.optString("draftResultText"),
        resultText = o.optString("resultText"),
        isPublished = o.optBoolean("isPublished", false),
        userDeletedHistory = o.optBoolean("userDeletedHistory", false),
        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
        completedAtMillis = o.optLong("completedAtMillis", 0L),
        updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
    )

    fun loadAllReportsLocal(context: Context): Map<String, ImeiVerificationReport> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_REPORTS_JSON, null) ?: return emptyMap()
        return try {
            val arr = JSONArray(raw)
            val map = mutableMapOf<String, ImeiVerificationReport>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val rep = parseReportJson(o)
                if (rep.requestId.isNotBlank()) {
                    map[rep.requestId] = rep
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveReportLocal(context: Context, report: ImeiVerificationReport) {
        val current = loadAllReportsLocal(context).toMutableMap()
        current[report.requestId] = report
        saveReportsMapLocal(context, current)
    }

    fun removeReportLocal(context: Context, requestId: String) {
        val current = loadAllReportsLocal(context).toMutableMap()
        current.remove(requestId)
        saveReportsMapLocal(context, current)
    }

    private fun saveReportsMapLocal(context: Context, map: Map<String, ImeiVerificationReport>) {
        val arr = JSONArray()
        map.values.forEach { rep ->
            arr.put(serializeReportJson(rep))
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REPORTS_JSON, arr.toString())
            .apply()
        notifyStateChanged()
    }

    private fun serializeReportJson(rep: ImeiVerificationReport): JSONObject = JSONObject().apply {
        put("requestId", rep.requestId)
        put("userId", rep.userId)
        put("imeiMasked", rep.imeiMasked)
        put("imeiHash", rep.imeiHash)
        put("brandId", rep.brandId)
        put("brandName", rep.brandName)
        put("verifiedModel", rep.verifiedModel)
        put("status", rep.status)
        put("paymentStatus", rep.paymentStatus)
        put("quotedPrice", rep.quotedPrice)
        put("currency", rep.currency)
        put("customerNotes", rep.customerNotes)
        put("draftResultText", rep.draftResultText)
        put("resultText", rep.resultText)
        put("isPublished", rep.isPublished)
        put("verifiedAtMillis", rep.verifiedAtMillis)
        put("createdAtMillis", rep.createdAtMillis)
        put("updatedAtMillis", rep.updatedAtMillis)
        put("sourcesUsed", JSONArray(rep.sourcesUsed))
        val fieldsArr = JSONArray()
        rep.fields.forEach { f ->
            fieldsArr.put(
                JSONObject().apply {
                    put("fieldId", f.fieldId)
                    put("titleKa", f.titleKa)
                    put("descriptionKa", f.descriptionKa)
                    put("value", f.value)
                    put("resultStatus", f.resultStatus)
                    put("isImportant", f.isImportant)
                    put("sourceName", f.sourceName)
                }
            )
        }
        put("fields", fieldsArr)
    }

    private fun parseReportJson(o: JSONObject): ImeiVerificationReport {
        val fieldsList = mutableListOf<ImeiResultFieldValue>()
        val fArr = o.optJSONArray("fields")
        if (fArr != null) {
            for (i in 0 until fArr.length()) {
                val fo = fArr.optJSONObject(i) ?: continue
                fieldsList.add(
                    ImeiResultFieldValue(
                        fieldId = fo.optString("fieldId"),
                        titleKa = fo.optString("titleKa"),
                        descriptionKa = fo.optString("descriptionKa"),
                        value = fo.optString("value"),
                        resultStatus = fo.optString("resultStatus", ImeiResultStatus.NOT_CHECKED.id),
                        isImportant = fo.optBoolean("isImportant", false),
                        sourceName = fo.optString("sourceName")
                    )
                )
            }
        }
        val sourcesList = mutableListOf<String>()
        val sArr = o.optJSONArray("sourcesUsed")
        if (sArr != null) {
            for (i in 0 until sArr.length()) {
                sourcesList.add(sArr.optString(i))
            }
        }
        return ImeiVerificationReport(
            requestId = o.optString("requestId"),
            userId = o.optString("userId"),
            imeiMasked = o.optString("imeiMasked"),
            imeiHash = o.optString("imeiHash"),
            brandId = o.optString("brandId"),
            brandName = o.optString("brandName"),
            verifiedModel = o.optString("verifiedModel"),
            status = o.optString("status", ImeiRequestStatus.COMPLETED.id),
            paymentStatus = o.optString("paymentStatus", ImeiPaymentStatus.PENDING.id),
            quotedPrice = o.optDouble("quotedPrice", 3.0),
            currency = o.optString("currency", "GEL"),
            fields = fieldsList,
            sourcesUsed = sourcesList,
            customerNotes = o.optString("customerNotes"),
            draftResultText = o.optString("draftResultText"),
            resultText = o.optString("resultText").ifBlank { o.optString("customerNotes") },
            isPublished = o.optBoolean("isPublished", false),
            verifiedAtMillis = o.optLong("verifiedAtMillis", System.currentTimeMillis()),
            createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    fun loadUserHistoryLocal(context: Context, userId: String): List<ImeiHistoryEntry> {
        if (userId.isBlank()) return emptyList()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_USER_HISTORY_PREFIX + userId, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<ImeiHistoryEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val entry = ImeiHistoryEntry(
                    id = o.optString("id"),
                    userId = o.optString("userId", userId),
                    imeiMasked = o.optString("imeiMasked"),
                    imeiHash = o.optString("imeiHash"),
                    brandId = o.optString("brandId"),
                    brandName = o.optString("brandName"),
                    verifiedModel = o.optString("verifiedModel"),
                    status = o.optString("status", ImeiRequestStatus.PENDING.id),
                    paymentStatus = o.optString("paymentStatus", ImeiPaymentStatus.PENDING.id),
                    quotedPrice = o.optDouble("quotedPrice", 3.0),
                    currency = o.optString("currency", "GEL"),
                    estimatedTimeText = o.optString("estimatedTimeText", "5–30 წთ"),
                    customerNotes = o.optString("customerNotes"),
                    draftResultText = o.optString("draftResultText"),
                    resultText = o.optString("resultText").ifBlank { o.optString("customerNotes") },
                    isPublished = o.optBoolean("isPublished", false),
                    createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                    completedAtMillis = o.optLong("completedAtMillis", 0L),
                    updatedAtMillis = o.optLong("updatedAtMillis", System.currentTimeMillis())
                )
                if (entry.userId == userId) {
                    list.add(entry)
                }
            }
            list.sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveUserHistoryLocal(context: Context, userId: String, entries: List<ImeiHistoryEntry>) {
        if (userId.isBlank()) return
        val arr = JSONArray()
        entries.filter { it.userId == userId }.sortedByDescending { it.createdAtMillis }.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("userId", e.userId)
                    put("imeiMasked", e.imeiMasked)
                    put("imeiHash", e.imeiHash)
                    put("brandId", e.brandId)
                    put("brandName", e.brandName)
                    put("verifiedModel", e.verifiedModel)
                    put("status", e.status)
                    put("paymentStatus", e.paymentStatus)
                    put("quotedPrice", e.quotedPrice)
                    put("currency", e.currency)
                    put("estimatedTimeText", e.estimatedTimeText)
                    put("customerNotes", e.customerNotes)
                    put("draftResultText", e.draftResultText)
                    put("resultText", e.resultText)
                    put("isPublished", e.isPublished)
                    put("createdAtMillis", e.createdAtMillis)
                    put("completedAtMillis", e.completedAtMillis)
                    put("updatedAtMillis", e.updatedAtMillis)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_HISTORY_PREFIX + userId, arr.toString())
            .apply()
        notifyStateChanged()
    }

    // --------------------------------------------------------------------------
    // SMART REPEAT CHECK (Phase 8: Same-User Only Duplicate Recognition)
    // --------------------------------------------------------------------------

    /**
     * ამოწმებს მხოლოდ მიმდინარე ავტორიზებული მომხმარებლის ისტორიას ნორმალიზებული ჰეშით.
     * არასოდეს ამჟღავნებს სხვა მომხმარებლის მიერ შემოწმებულ IMEI-ს ან რეპორტს.
     */
    fun findPreviousCheckForCurrentUser(
        context: Context,
        currentUserId: String,
        rawImei: String,
        currentHistory: List<ImeiHistoryEntry> = emptyList()
    ): ImeiHistoryEntry? {
        if (currentUserId.isBlank()) return null
        val digits = ImeiValidator.normalizeDigits(rawImei)
        if (digits.length != 15) return null
        val targetHash = ImeiValidator.computeUserScopedImeiHash(currentUserId, digits)
        val pool = if (currentHistory.isNotEmpty()) currentHistory else loadUserHistoryLocal(context, currentUserId)
        return pool
            .filter { it.userId == currentUserId && it.imeiHash == targetHash }
            .maxByOrNull { it.createdAtMillis }
    }

    // --------------------------------------------------------------------------
    // ANALYTICS COMPUTATION & SERVER SUMMARY AGGREGATION (Phase 11)
    // --------------------------------------------------------------------------

    fun computeAnalytics(
        requests: List<ImeiCheckRequest>,
        reports: Map<String, ImeiVerificationReport>,
        fields: List<ImeiResultFieldDefinition>
    ): ImeiAnalyticsComputed {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        val startOfWeek = now - 7L * 24L * 3600_000L
        val startOfMonth = now - 30L * 24L * 3600_000L

        val total = requests.size
        val todayCount = requests.count { it.createdAtMillis >= startOfToday }
        val weekCount = requests.count { it.createdAtMillis >= startOfWeek }
        val monthCount = requests.count { it.createdAtMillis >= startOfMonth }

        val completed = requests.count { it.status == ImeiRequestStatus.COMPLETED.id }
        val pending = requests.count { it.status == ImeiRequestStatus.PENDING.id }
        val inProgress = requests.count { it.status == ImeiRequestStatus.IN_PROGRESS.id }
        val waitingInfo = requests.count { it.status == ImeiRequestStatus.WAITING_FOR_INFO.id }
        val rejected = requests.count { it.status == ImeiRequestStatus.REJECTED.id }

        val overdue = requests.count { req ->
            (req.status == ImeiRequestStatus.PENDING.id || req.status == ImeiRequestStatus.IN_PROGRESS.id) &&
                !req.userDeletedHistory &&
                (now - req.createdAtMillis) > (req.estimatedMaxMinutes.coerceAtLeast(5) * 60_000L)
        }

        val processingDurationsMinutes = requests
            .filter { it.status == ImeiRequestStatus.COMPLETED.id && it.completedAtMillis > it.createdAtMillis }
            .map { ((it.completedAtMillis - it.createdAtMillis) / 60_000L).toInt().coerceAtLeast(1) }
            .sorted()

        val avgMinutes = if (processingDurationsMinutes.isNotEmpty()) {
            processingDurationsMinutes.average().toInt()
        } else 0

        val medianMinutes = if (processingDurationsMinutes.isNotEmpty()) {
            val mid = processingDurationsMinutes.size / 2
            if (processingDurationsMinutes.size % 2 == 1) {
                processingDurationsMinutes[mid]
            } else {
                (processingDurationsMinutes[mid - 1] + processingDurationsMinutes[mid]) / 2
            }
        } else 0

        // Business statistics: ONLY count confirmed PAID requests as revenue!
        val paidRequests = requests.filter { it.paymentStatus == ImeiPaymentStatus.PAID.id }
        val confirmedRevenue = paidRequests.sumOf { it.quotedPrice }
        val revenueToday = paidRequests.filter { it.createdAtMillis >= startOfToday }.sumOf { it.quotedPrice }
        val revenueWeek = paidRequests.filter { it.createdAtMillis >= startOfWeek }.sumOf { it.quotedPrice }
        val revenueMonth = paidRequests.filter { it.createdAtMillis >= startOfMonth }.sumOf { it.quotedPrice }
        val paidCount = paidRequests.size
        val avgRevPerPaid = if (paidCount > 0) confirmedRevenue / paidCount else 0.0

        val refundedTotal = requests
            .filter { it.paymentStatus == ImeiPaymentStatus.REFUNDED.id }
            .sumOf { it.quotedPrice }

        val outstandingRequests = requests.filter {
            (it.paymentStatus == ImeiPaymentStatus.UNPAID.id || it.paymentStatus == ImeiPaymentStatus.PENDING.id) &&
                it.status != ImeiRequestStatus.REJECTED.id &&
                !it.userDeletedHistory
        }
        val outstandingCount = outstandingRequests.size
        val outstandingTotal = outstandingRequests.sumOf { it.quotedPrice }

        // Brand statistics
        val brandGroups = requests.groupBy { it.brandName.ifBlank { "სხვა" } }
        val brandCounts = brandGroups.mapValues { it.value.size }
            .entries.sortedByDescending { it.value }.associate { it.key to it.value }
        val brandCompletionRates = brandGroups.mapValues { (_, list) ->
            if (list.isEmpty()) 0 else ((list.count { it.status == ImeiRequestStatus.COMPLETED.id } * 100.0) / list.size).toInt()
        }

        // Verification quality statistics
        val sourceCounts = mutableMapOf<String, Int>()
        val fieldAvailableCounts = mutableMapOf<String, Int>()
        val fieldUnavailableCounts = mutableMapOf<String, Int>()
        val fieldTotalCounts = mutableMapOf<String, Int>()

        val fieldTitleById = fields.associate { it.id to it.titleKa }

        reports.values.forEach { rep ->
            rep.sourcesUsed.forEach { src ->
                if (src.isNotBlank()) {
                    sourceCounts[src] = (sourceCounts[src] ?: 0) + 1
                }
            }
            rep.fields.forEach { fv ->
                val label = fv.titleKa.ifBlank { fieldTitleById[fv.fieldId] ?: fv.fieldId }
                if (label.isNotBlank()) {
                    fieldTotalCounts[label] = (fieldTotalCounts[label] ?: 0) + 1
                    when (fv.resultStatus) {
                        ImeiResultStatus.CONFIRMED_POSITIVE.id,
                        ImeiResultStatus.CONFIRMED_NEGATIVE.id -> {
                            fieldAvailableCounts[label] = (fieldAvailableCounts[label] ?: 0) + 1
                        }
                        ImeiResultStatus.UNAVAILABLE.id,
                        ImeiResultStatus.UNABLE_TO_VERIFY.id -> {
                            fieldUnavailableCounts[label] = (fieldUnavailableCounts[label] ?: 0) + 1
                        }
                    }
                }
            }
        }

        val fieldAvailabilityRates = fieldTotalCounts.mapValues { (label, tot) ->
            if (tot <= 0) 0 else (((fieldAvailableCounts[label] ?: 0) * 100.0) / tot).toInt()
        }

        return ImeiAnalyticsComputed(
            totalRequests = total,
            requestsToday = todayCount,
            requestsThisWeek = weekCount,
            requestsThisMonth = monthCount,
            completedRequests = completed,
            pendingRequests = pending,
            inProgressRequests = inProgress,
            waitingForInfoRequests = waitingInfo,
            rejectedRequests = rejected,
            overdueRequests = overdue,
            averageProcessingMinutes = avgMinutes,
            medianProcessingMinutes = medianMinutes,
            confirmedPaidRevenueGel = confirmedRevenue,
            revenueTodayGel = revenueToday,
            revenueThisWeekGel = revenueWeek,
            revenueThisMonthGel = revenueMonth,
            paidChecksCount = paidCount,
            averageRevenuePerPaidGel = avgRevPerPaid,
            refundedTotalGel = refundedTotal,
            outstandingPaymentRequestsCount = outstandingCount,
            outstandingPaymentTotalGel = outstandingTotal,
            brandRequestCounts = brandCounts,
            brandCompletionRates = brandCompletionRates,
            sourceUsageCounts = sourceCounts.entries.sortedByDescending { it.value }.associate { it.key to it.value },
            fieldAvailabilityRates = fieldAvailabilityRates,
            fieldUnavailableCounts = fieldUnavailableCounts.entries.sortedByDescending { it.value }.associate { it.key to it.value }
        )
    }

    // --------------------------------------------------------------------------
    // FIRESTORE OPERATIONS & REAL-TIME LISTENERS
    // --------------------------------------------------------------------------

    private fun getFirestoreOrNull(context: Context): FirebaseFirestore? {
        return try {
            FirestoreRepository(context).db
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveConfigToCloud(context: Context, config: ImeiServiceConfig): Result<Unit> = withContext(Dispatchers.IO) {
        val updated = config.copy(updatedAtMillis = System.currentTimeMillis())
        saveConfigLocal(context, updated)
        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            val payload = mapOf(
                "configType" to "imei_check",
                "serviceEnabled" to updated.serviceEnabled,
                "acceptingRequests" to updated.acceptingRequests,
                "entryCardVisible" to updated.entryCardVisible,
                "priceGel" to updated.priceGel,
                "currency" to updated.currency,
                "estimatedMinMinutes" to updated.estimatedMinMinutes,
                "estimatedMaxMinutes" to updated.estimatedMaxMinutes,
                "title" to updated.title,
                "subtitle" to updated.subtitle,
                "shortDescription" to updated.shortDescription,
                "detailedDescription" to updated.detailedDescription,
                "promoBadgeText" to updated.promoBadgeText,
                "cardImageUrl" to updated.cardImageUrl,
                "userInstructions" to updated.userInstructions,
                "privacyNotice" to updated.privacyNotice,
                "maintenanceMessage" to updated.maintenanceMessage,
                "paymentInstructions" to updated.paymentInstructions,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("app_config").document("imei_check").set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_config/imei_check")
            Result.failure(e)
        }
    }

    fun listenToConfig(
        context: Context,
        onUpdated: (ImeiServiceConfig) -> Unit
    ): ListenerRegistration? {
        val db = getFirestoreOrNull(context) ?: return null
        return try {
            db.collection("app_config").document("imei_check")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                    val cfg = ImeiServiceConfig(
                        serviceEnabled = snap.getBoolean("serviceEnabled") ?: true,
                        acceptingRequests = snap.getBoolean("acceptingRequests") ?: true,
                        entryCardVisible = snap.getBoolean("entryCardVisible") ?: true,
                        priceGel = snap.getDouble("priceGel") ?: 3.0,
                        currency = snap.getString("currency") ?: "GEL",
                        estimatedMinMinutes = (snap.getLong("estimatedMinMinutes") ?: 5L).toInt(),
                        estimatedMaxMinutes = (snap.getLong("estimatedMaxMinutes") ?: 30L).toInt(),
                        title = snap.getString("title") ?: "IMEI CHECK • სრული შემოწმება",
                        subtitle = snap.getString("subtitle") ?: ImeiServiceConfig().subtitle,
                        shortDescription = snap.getString("shortDescription") ?: ImeiServiceConfig().shortDescription,
                        detailedDescription = snap.getString("detailedDescription") ?: ImeiServiceConfig().detailedDescription,
                        promoBadgeText = snap.getString("promoBadgeText") ?: "სანდო ვერიფიკაცია • 3 ₾",
                        cardImageUrl = snap.getString("cardImageUrl") ?: ImeiServiceConfig.DEFAULT_CARD_IMAGE_URL,
                        userInstructions = snap.getString("userInstructions") ?: ImeiServiceConfig().userInstructions,
                        privacyNotice = snap.getString("privacyNotice") ?: ImeiServiceConfig().privacyNotice,
                        maintenanceMessage = snap.getString("maintenanceMessage") ?: ImeiServiceConfig().maintenanceMessage,
                        paymentInstructions = snap.getString("paymentInstructions") ?: ImeiServiceConfig().paymentInstructions,
                        updatedAtMillis = snap.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                    )
                    saveConfigLocal(context, cfg)
                    onUpdated(cfg)
                }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveBrandToCloud(context: Context, brand: ImeiBrandConfig): Result<Unit> = withContext(Dispatchers.IO) {
        val current = loadBrands(context).toMutableList()
        val idx = current.indexOfFirst { it.id == brand.id }
        if (idx >= 0) current[idx] = brand else current.add(brand)
        saveBrandsLocal(context, current)

        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            val payload = mapOf(
                "id" to brand.id,
                "name" to brand.name,
                "localizedName" to brand.localizedName,
                "logoUrl" to brand.logoUrl,
                "iconEmoji" to brand.iconEmoji,
                "displayOrder" to brand.displayOrder,
                "enabled" to brand.enabled,
                "isPopular" to brand.isPopular,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("imei_brands").document(brand.id).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "imei_brands/${brand.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteBrandFromCloud(context: Context, brandId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = loadBrands(context).filterNot { it.id == brandId }
        saveBrandsLocal(context, current)
        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            db.collection("imei_brands").document(brandId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "imei_brands/$brandId")
            Result.failure(e)
        }
    }

    fun listenToBrands(
        context: Context,
        onUpdated: (List<ImeiBrandConfig>) -> Unit
    ): ListenerRegistration? {
        val db = getFirestoreOrNull(context) ?: return null
        return try {
            db.collection("imei_brands")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null || snap.isEmpty) return@addSnapshotListener
                    val cloudBrands = snap.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val name = doc.getString("name") ?: return@mapNotNull null
                        ImeiBrandConfig(
                            id = id,
                            name = name,
                            localizedName = doc.getString("localizedName") ?: name,
                            logoUrl = doc.getString("logoUrl") ?: "",
                            iconEmoji = doc.getString("iconEmoji") ?: "📱",
                            displayOrder = (doc.getLong("displayOrder") ?: 99L).toInt(),
                            enabled = doc.getBoolean("enabled") ?: true,
                            isPopular = doc.getBoolean("isPopular") ?: false,
                            updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                        )
                    }.sortedBy { it.displayOrder }
                    if (cloudBrands.isNotEmpty()) {
                        saveBrandsLocal(context, cloudBrands)
                        onUpdated(cloudBrands)
                    }
                }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveResultFieldToCloud(context: Context, field: ImeiResultFieldDefinition): Result<Unit> = withContext(Dispatchers.IO) {
        val current = loadResultFields(context).toMutableList()
        val idx = current.indexOfFirst { it.id == field.id }
        if (idx >= 0) current[idx] = field else current.add(field)
        saveResultFieldsLocal(context, current)

        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            val payload = mapOf(
                "id" to field.id,
                "titleKa" to field.titleKa,
                "descriptionKa" to field.descriptionKa,
                "supportedBrands" to field.supportedBrands,
                "displayOrder" to field.displayOrder,
                "enabled" to field.enabled,
                "isImportant" to field.isImportant,
                "inputType" to field.inputType,
                "availableStatuses" to field.availableStatuses,
                "internalInstructions" to field.internalInstructions,
                "sourceIds" to field.sourceIds,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("imei_result_fields").document(field.id).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "imei_result_fields/${field.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteResultFieldFromCloud(context: Context, fieldId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = loadResultFields(context).filterNot { it.id == fieldId }
        saveResultFieldsLocal(context, current)
        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            db.collection("imei_result_fields").document(fieldId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "imei_result_fields/$fieldId")
            Result.failure(e)
        }
    }

    fun listenToResultFields(
        context: Context,
        onUpdated: (List<ImeiResultFieldDefinition>) -> Unit
    ): ListenerRegistration? {
        val db = getFirestoreOrNull(context) ?: return null
        return try {
            db.collection("imei_result_fields")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null || snap.isEmpty) return@addSnapshotListener
                    val cloudFields = snap.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val titleKa = doc.getString("titleKa") ?: return@mapNotNull null
                        val brands = (doc.get("supportedBrands") as? List<*>)?.mapNotNull { it as? String } ?: listOf("ALL")
                        val statuses = (doc.get("availableStatuses") as? List<*>)?.mapNotNull { it as? String }
                            ?: ImeiResultStatus.entries.map { it.id }
                        val sources = (doc.get("sourceIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        ImeiResultFieldDefinition(
                            id = id,
                            titleKa = titleKa,
                            descriptionKa = doc.getString("descriptionKa") ?: "",
                            supportedBrands = brands.ifEmpty { listOf("ALL") },
                            displayOrder = (doc.getLong("displayOrder") ?: 99L).toInt(),
                            enabled = doc.getBoolean("enabled") ?: true,
                            isImportant = doc.getBoolean("isImportant") ?: false,
                            inputType = doc.getString("inputType") ?: "text_and_status",
                            availableStatuses = statuses,
                            internalInstructions = doc.getString("internalInstructions") ?: "",
                            sourceIds = sources,
                            updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                        )
                    }.sortedBy { it.displayOrder }
                    if (cloudFields.isNotEmpty()) {
                        saveResultFieldsLocal(context, cloudFields)
                        onUpdated(cloudFields)
                    }
                }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveSourceToCloud(context: Context, source: ImeiVerificationSource): Result<Unit> = withContext(Dispatchers.IO) {
        val current = loadSources(context).toMutableList()
        val idx = current.indexOfFirst { it.id == source.id }
        if (idx >= 0) current[idx] = source else current.add(source)
        saveSourcesLocal(context, current)

        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            val payload = mapOf(
                "id" to source.id,
                "name" to source.name,
                "description" to source.description,
                "websiteUrl" to source.websiteUrl,
                "supportedBrands" to source.supportedBrands,
                "supportedFields" to source.supportedFields,
                "internalInstructions" to source.internalInstructions,
                "active" to source.active,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("imei_sources").document(source.id).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "imei_sources/${source.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteSourceFromCloud(context: Context, sourceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val current = loadSources(context).filterNot { it.id == sourceId }
        saveSourcesLocal(context, current)
        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            db.collection("imei_sources").document(sourceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "imei_sources/$sourceId")
            Result.failure(e)
        }
    }

    suspend fun publishConfigToCloud(context: Context, config: ImeiServiceConfig): Result<Unit> =
        saveConfigToCloud(context, config)

    suspend fun publishBrandsToCloud(context: Context, brands: List<ImeiBrandConfig>): Result<Unit> = withContext(Dispatchers.IO) {
        saveBrandsLocal(context, brands)
        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            brands.forEach { brand ->
                val payload = mapOf(
                    "id" to brand.id,
                    "name" to brand.name,
                    "localizedName" to brand.localizedName,
                    "logoUrl" to brand.logoUrl,
                    "iconEmoji" to brand.iconEmoji,
                    "displayOrder" to brand.displayOrder,
                    "enabled" to brand.enabled,
                    "isPopular" to brand.isPopular,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("imei_brands").document(brand.id).set(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "imei_brands")
            Result.failure(e)
        }
    }

    suspend fun publishResultFieldsToCloud(context: Context, fields: List<ImeiResultFieldDefinition>): Result<Unit> = withContext(Dispatchers.IO) {
        saveResultFieldsLocal(context, fields)
        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            fields.forEach { field ->
                val payload = mapOf(
                    "id" to field.id,
                    "titleKa" to field.titleKa,
                    "descriptionKa" to field.descriptionKa,
                    "supportedBrands" to field.supportedBrands,
                    "displayOrder" to field.displayOrder,
                    "enabled" to field.enabled,
                    "isImportant" to field.isImportant,
                    "inputType" to field.inputType,
                    "availableStatuses" to field.availableStatuses,
                    "internalInstructions" to field.internalInstructions,
                    "sourceIds" to field.sourceIds,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("imei_result_fields").document(field.id).set(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "imei_result_fields")
            Result.failure(e)
        }
    }

    suspend fun publishSourcesToCloud(context: Context, sources: List<ImeiVerificationSource>): Result<Unit> = withContext(Dispatchers.IO) {
        saveSourcesLocal(context, sources)
        val db = getFirestoreOrNull(context) ?: return@withContext Result.success(Unit)
        try {
            AuthAndOrderManager.ensureAdminFirebaseSession(context)
            sources.forEach { source ->
                val payload = mapOf(
                    "id" to source.id,
                    "name" to source.name,
                    "description" to source.description,
                    "websiteUrl" to source.websiteUrl,
                    "supportedBrands" to source.supportedBrands,
                    "supportedFields" to source.supportedFields,
                    "internalInstructions" to source.internalInstructions,
                    "active" to source.active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("imei_sources").document(source.id).set(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "imei_sources")
            Result.failure(e)
        }
    }

    suspend fun syncPublicCatalogAndConfigFromCloud(context: Context) = withContext(Dispatchers.IO) {
        val db = getFirestoreOrNull(context) ?: return@withContext
        try {
            val cfgDoc = db.collection("app_config").document("imei_check").get().await()
            if (cfgDoc.exists()) {
                val cfg = ImeiServiceConfig(
                    serviceEnabled = cfgDoc.getBoolean("serviceEnabled") ?: true,
                    acceptingRequests = cfgDoc.getBoolean("acceptingRequests") ?: true,
                    entryCardVisible = cfgDoc.getBoolean("entryCardVisible") ?: true,
                    priceGel = cfgDoc.getDouble("priceGel") ?: 3.0,
                    currency = cfgDoc.getString("currency") ?: "GEL",
                    estimatedMinMinutes = (cfgDoc.getLong("estimatedMinMinutes") ?: 5L).toInt(),
                    estimatedMaxMinutes = (cfgDoc.getLong("estimatedMaxMinutes") ?: 30L).toInt(),
                    title = cfgDoc.getString("title") ?: "IMEI CHECK • სრული შემოწმება",
                    subtitle = cfgDoc.getString("subtitle") ?: ImeiServiceConfig().subtitle,
                    shortDescription = cfgDoc.getString("shortDescription") ?: ImeiServiceConfig().shortDescription,
                    detailedDescription = cfgDoc.getString("detailedDescription") ?: ImeiServiceConfig().detailedDescription,
                    promoBadgeText = cfgDoc.getString("promoBadgeText") ?: "სანდო ვერიფიკაცია • 3 ₾",
                    cardImageUrl = cfgDoc.getString("cardImageUrl") ?: ImeiServiceConfig.DEFAULT_CARD_IMAGE_URL,
                    userInstructions = cfgDoc.getString("userInstructions") ?: ImeiServiceConfig().userInstructions,
                    privacyNotice = cfgDoc.getString("privacyNotice") ?: ImeiServiceConfig().privacyNotice,
                    maintenanceMessage = cfgDoc.getString("maintenanceMessage") ?: ImeiServiceConfig().maintenanceMessage,
                    paymentInstructions = cfgDoc.getString("paymentInstructions") ?: ImeiServiceConfig().paymentInstructions,
                    updatedAtMillis = cfgDoc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
                saveConfigLocal(context, cfg)
            }
        } catch (_: Exception) {
        }
        try {
            val brandsSnap = db.collection("imei_brands").get().await()
            if (!brandsSnap.isEmpty) {
                val cloudBrands = brandsSnap.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val name = doc.getString("name") ?: return@mapNotNull null
                    ImeiBrandConfig(
                        id = id,
                        name = name,
                        localizedName = doc.getString("localizedName") ?: name,
                        logoUrl = doc.getString("logoUrl") ?: "",
                        iconEmoji = doc.getString("iconEmoji") ?: "📱",
                        displayOrder = (doc.getLong("displayOrder") ?: 99L).toInt(),
                        enabled = doc.getBoolean("enabled") ?: true,
                        isPopular = doc.getBoolean("isPopular") ?: false,
                        updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }.sortedBy { it.displayOrder }
                if (cloudBrands.isNotEmpty()) {
                    saveBrandsLocal(context, cloudBrands)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun listenToAllRequestsForAdmin(
        context: Context,
        onUpdated: (List<ImeiCheckRequest>) -> Unit
    ): ListenerRegistration? {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            onUpdated(emptyList())
            return null
        }
        val db = getFirestoreOrNull(context) ?: return null
        return try {
            db.collection("imei_requests")
                .addSnapshotListener { snap, err ->
                    if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
                        onUpdated(emptyList())
                        return@addSnapshotListener
                    }
                    if (err != null || snap == null) return@addSnapshotListener
                    val cloudReqs = snap.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val userId = doc.getString("userId") ?: return@mapNotNull null
                        ImeiCheckRequest(
                            id = id,
                            userId = userId,
                            userName = doc.getString("userName") ?: "",
                            userEmail = doc.getString("userEmail") ?: "",
                            userPhone = doc.getString("userPhone") ?: "",
                            imei = doc.getString("imei") ?: "",
                            imeiMasked = doc.getString("imeiMasked") ?: "******",
                            imeiHash = doc.getString("imeiHash") ?: "",
                            tac = doc.getString("tac") ?: "",
                            brandId = doc.getString("brandId") ?: "",
                            brandName = doc.getString("brandName") ?: "",
                            verifiedModel = doc.getString("verifiedModel") ?: "",
                            status = doc.getString("status") ?: ImeiRequestStatus.PENDING.id,
                            paymentStatus = doc.getString("paymentStatus") ?: ImeiPaymentStatus.PENDING.id,
                            quotedPrice = doc.getDouble("quotedPrice") ?: 3.0,
                            currency = doc.getString("currency") ?: "GEL",
                            estimatedMinMinutes = (doc.getLong("estimatedMinMinutes") ?: 5L).toInt(),
                            estimatedMaxMinutes = (doc.getLong("estimatedMaxMinutes") ?: 30L).toInt(),
                            internalNotes = doc.getString("internalNotes") ?: "",
                            customerNotes = doc.getString("customerNotes") ?: "",
                            userDeletedHistory = doc.getBoolean("userDeletedHistory") ?: false,
                            createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                            completedAtMillis = doc.getTimestamp("completedAt")?.toDate()?.time ?: 0L,
                            updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }.sortedByDescending { it.createdAtMillis }
                    saveAllRequestsLocal(context, cloudReqs)
                    onUpdated(cloudReqs)
                }
        } catch (_: Exception) {
            null
        }
    }

    // --------------------------------------------------------------------------
    // SUBMIT NEW IMEI CHECK REQUEST (Phase 3, 7, 8, 9, 10, 12)
    // --------------------------------------------------------------------------

    suspend fun submitImeiCheckRequest(
        context: Context,
        rawImei: String,
        selectedBrand: ImeiBrandConfig,
        currentUser: UserProfile? = null,
        config: ImeiServiceConfig = loadConfig(context),
        allowRepeatSubmission: Boolean = true
    ): Result<ImeiHistoryEntry> = withContext(Dispatchers.IO) {
        val validation = ImeiValidator.validateInput(rawImei)
        if (!validation.isValid) {
            return@withContext Result.failure(IllegalArgumentException(validation.messageKa ?: "არასწორი IMEI კოდი"))
        }

        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        val localUser = currentUser ?: AuthAndOrderManager.getCurrentUser(context)
        val resolvedUid = (currentUser?.id?.takeIf { it.isNotBlank() }
            ?: fbUser?.uid?.takeIf { it.isNotBlank() }
            ?: localUser?.id?.takeIf { it.isNotBlank() })
            ?: return@withContext Result.failure(IllegalStateException("IMEI შემოწმების გასაგზავნად გთხოვთ გაიაროთ ავტორიზაცია პროფილის გვერდზე."))

        val userName = localUser?.name?.ifBlank { fbUser?.displayName.orEmpty() } ?: fbUser?.displayName.orEmpty().ifBlank { "მომხმარებელი" }
        val userEmail = localUser?.email?.ifBlank { fbUser?.email.orEmpty() } ?: fbUser?.email.orEmpty()
        val userPhone = localUser?.phone ?: ""

        val now = System.currentTimeMillis()
        val reqId = "imei_${now}_${UUID.randomUUID().toString().take(6).lowercase()}"
        val cleanImei = validation.normalizedDigits
        val masked = ImeiValidator.maskImei(cleanImei)
        val imeiHash = ImeiValidator.computeUserScopedImeiHash(resolvedUid, cleanImei)

        val requestObj = ImeiCheckRequest(
            id = reqId,
            userId = resolvedUid,
            userName = userName,
            userEmail = userEmail,
            userPhone = userPhone,
            imei = cleanImei,
            imeiMasked = masked,
            imeiHash = imeiHash,
            tac = validation.tac,
            brandId = selectedBrand.id,
            brandName = selectedBrand.name,
            verifiedModel = "",
            status = ImeiRequestStatus.PENDING.id,
            paymentStatus = ImeiPaymentStatus.PENDING.id,
            quotedPrice = config.priceGel,
            currency = config.currency,
            estimatedMinMinutes = config.estimatedMinMinutes,
            estimatedMaxMinutes = config.estimatedMaxMinutes,
            internalNotes = "",
            customerNotes = "",
            userDeletedHistory = false,
            createdAtMillis = now,
            completedAtMillis = 0L,
            updatedAtMillis = now
        )

        val historyEntry = ImeiHistoryEntry(
            id = reqId,
            userId = resolvedUid,
            imeiMasked = masked,
            imeiHash = imeiHash,
            brandId = selectedBrand.id,
            brandName = selectedBrand.name,
            verifiedModel = "",
            status = ImeiRequestStatus.PENDING.id,
            paymentStatus = ImeiPaymentStatus.PENDING.id,
            quotedPrice = config.priceGel,
            currency = config.currency,
            estimatedTimeText = config.formattedEstimatedTime,
            customerNotes = "",
            createdAtMillis = now,
            completedAtMillis = 0L,
            updatedAtMillis = now
        )

        // 1. Update local caches immediately
        val allReqs = loadAllRequestsRawInternal(context).toMutableList()
        allReqs.add(0, requestObj)
        saveAllRequestsLocal(context, allReqs)

        val userHist = loadUserHistoryLocal(context, resolvedUid).toMutableList()
        userHist.add(0, historyEntry)
        saveUserHistoryLocal(context, resolvedUid, userHist)

        // 2. Sync to Firestore if Firebase Auth is active for this user
        val db = getFirestoreOrNull(context)
        if (db != null && fbUser != null && fbUser.uid == resolvedUid) {
            try {
                withTimeoutOrNull(4500L) {
                val reqPayload = mapOf(
                    "id" to reqId,
                    "userId" to resolvedUid,
                    "userName" to userName,
                    "userEmail" to userEmail,
                    "userPhone" to userPhone,
                    "imei" to cleanImei,
                    "imeiMasked" to masked,
                    "imeiHash" to imeiHash,
                    "tac" to validation.tac,
                    "brandId" to selectedBrand.id,
                    "brandName" to selectedBrand.name,
                    "verifiedModel" to "",
                    "status" to ImeiRequestStatus.PENDING.id,
                    "paymentStatus" to ImeiPaymentStatus.PENDING.id,
                    "quotedPrice" to config.priceGel,
                    "currency" to config.currency,
                    "estimatedMinMinutes" to config.estimatedMinMinutes,
                    "estimatedMaxMinutes" to config.estimatedMaxMinutes,
                    "internalNotes" to "",
                    "customerNotes" to "",
                    "userDeletedHistory" to false,
                    "adminRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("imei_requests").document(reqId).set(reqPayload).await()

                val histPayload = mapOf(
                    "id" to reqId,
                    "userId" to resolvedUid,
                    "imeiMasked" to masked,
                    "imeiHash" to imeiHash,
                    "brandId" to selectedBrand.id,
                    "brandName" to selectedBrand.name,
                    "verifiedModel" to "",
                    "status" to ImeiRequestStatus.PENDING.id,
                    "paymentStatus" to ImeiPaymentStatus.PENDING.id,
                    "quotedPrice" to config.priceGel,
                    "currency" to config.currency,
                    "estimatedTimeText" to config.formattedEstimatedTime,
                    "customerNotes" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(resolvedUid).collection("imei_history").document(reqId).set(histPayload).await()

                val accPayload = mapOf(
                    "id" to reqId,
                    "userId" to resolvedUid,
                    "brandName" to selectedBrand.name,
                    "quotedPrice" to config.priceGel,
                    "currency" to config.currency,
                    "paymentStatus" to ImeiPaymentStatus.PENDING.id,
                    "requestStatus" to ImeiRequestStatus.PENDING.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                    db.collection("imei_accounting").document(reqId).set(accPayload).await()

                    runCatching {
                        FirestoreRepository(context).createAdminOrderNotification(
                            orderId = reqId,
                            orderType = "imei_check"
                        )
                    }
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "imei_requests/$reqId")
            }
        }

        // 3. Record admin-only order notification (NEVER sends push notification to customers or non-admin users,
        // and NEVER includes full IMEI, customer private information, or sensitive report data!)
        BekaNotificationManager.recordOrderCreatedForAdminOnly(
            context = context,
            orderId = reqId,
            orderType = "imei_check"
        )

        Result.success(historyEntry)
    }

    // --------------------------------------------------------------------------
    // ADMIN REQUEST PROCESSING & REPORT SAVING (Phase 5, 6, 9, 10, 11)
    // --------------------------------------------------------------------------

    suspend fun saveAdminVerificationProgress(
        context: Context,
        request: ImeiCheckRequest,
        newStatus: ImeiRequestStatus,
        newPaymentStatus: ImeiPaymentStatus,
        verifiedModel: String,
        internalNotes: String = "",
        customerNotes: String = "",
        fieldValues: List<ImeiResultFieldValue> = emptyList(),
        sourcesUsed: List<String> = emptyList(),
        notifyCustomer: Boolean = true,
        resultText: String = "",
        draftResultText: String = "",
        isPublished: Boolean = true
    ): Result<ImeiVerificationReport> = withContext(Dispatchers.IO) {
        val activeEmail = AuthAndOrderManager.resolveActiveUserEmail(context)
        if (activeEmail.isNotBlank() && !AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return@withContext Result.failure(
                SecurityException("PERMISSION_DENIED: მხოლოდ ავტორიზებულ ადმინისტრატორს (beqa1994@gmail.com) აქვს IMEI შეკვეთის მართვის უფლება.")
            )
        }
        val now = System.currentTimeMillis()
        val completedAt = if (newStatus == ImeiRequestStatus.COMPLETED) {
            if (request.completedAtMillis > 0L) request.completedAtMillis else now
        } else request.completedAtMillis

        val finalResultText = if (resultText.isNotBlank()) resultText.trim() else customerNotes.trim()
        val finalCustomerNotes = if (customerNotes.isNotBlank()) customerNotes.trim() else finalResultText
        val finalDraftText = if (draftResultText.isNotBlank()) draftResultText.trim() else finalResultText

        val updatedRequest = request.copy(
            status = newStatus.id,
            paymentStatus = newPaymentStatus.id,
            verifiedModel = verifiedModel.trim(),
            internalNotes = internalNotes.trim(),
            customerNotes = finalCustomerNotes,
            resultText = finalResultText,
            draftResultText = finalDraftText,
            isPublished = isPublished,
            completedAtMillis = completedAt,
            updatedAtMillis = now
        )

        val report = ImeiVerificationReport(
            requestId = request.id,
            userId = request.userId,
            imeiMasked = request.imeiMasked,
            imeiHash = request.imeiHash,
            brandId = request.brandId,
            brandName = request.brandName,
            verifiedModel = verifiedModel.trim(),
            status = newStatus.id,
            paymentStatus = newPaymentStatus.id,
            quotedPrice = request.quotedPrice,
            currency = request.currency,
            fields = fieldValues,
            sourcesUsed = sourcesUsed,
            customerNotes = finalCustomerNotes,
            resultText = finalResultText,
            draftResultText = finalDraftText,
            isPublished = isPublished,
            verifiedAtMillis = if (completedAt > 0L) completedAt else now,
            createdAtMillis = request.createdAtMillis,
            updatedAtMillis = now
        )

        // 1. Update local caches
        val allReqs = loadAllRequestsLocal(context).toMutableList()
        val reqIdx = allReqs.indexOfFirst { it.id == request.id }
        if (reqIdx >= 0) allReqs[reqIdx] = updatedRequest else allReqs.add(0, updatedRequest)
        saveAllRequestsLocal(context, allReqs)

        if (!request.userDeletedHistory && isPublished) {
            saveReportLocal(context, report)
            val userHist = loadUserHistoryLocal(context, request.userId).toMutableList()
            val histIdx = userHist.indexOfFirst { it.id == request.id }
            val updatedHistEntry = ImeiHistoryEntry(
                id = request.id,
                userId = request.userId,
                imeiMasked = request.imeiMasked,
                imeiHash = request.imeiHash,
                brandId = request.brandId,
                brandName = request.brandName,
                verifiedModel = verifiedModel.trim(),
                status = newStatus.id,
                paymentStatus = newPaymentStatus.id,
                quotedPrice = request.quotedPrice,
                currency = request.currency,
                estimatedTimeText = request.estimatedTimeText,
                customerNotes = finalCustomerNotes,
                resultText = finalResultText,
                draftResultText = finalDraftText,
                isPublished = isPublished,
                createdAtMillis = request.createdAtMillis,
                completedAtMillis = completedAt,
                updatedAtMillis = now
            )
            if (histIdx >= 0) userHist[histIdx] = updatedHistEntry else userHist.add(0, updatedHistEntry)
            saveUserHistoryLocal(context, request.userId, userHist)
        }

        // 2. Sync to Firestore
        val db = getFirestoreOrNull(context)
        if (db != null) {
            try {
                withTimeoutOrNull(4500L) {
                    AuthAndOrderManager.ensureAdminFirebaseSession(context)
                    val activeFbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
                    if (activeFbUser != null) {
                        val reqUpdateMap = mutableMapOf<String, Any?>(
                            "status" to newStatus.id,
                            "paymentStatus" to newPaymentStatus.id,
                            "verifiedModel" to verifiedModel.trim(),
                            "internalNotes" to internalNotes.trim(),
                            "customerNotes" to finalCustomerNotes,
                            "resultText" to finalResultText,
                            "draftResultText" to finalDraftText,
                            "isPublished" to isPublished,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        if (newStatus == ImeiRequestStatus.COMPLETED) {
                            reqUpdateMap["completedAt"] = FieldValue.serverTimestamp()
                        }
                        db.collection("imei_requests").document(request.id).set(reqUpdateMap, SetOptions.merge()).await()

                        if (!request.userDeletedHistory && isPublished) {
                            val fieldsMaps = fieldValues.map { fv ->
                                mapOf(
                                    "fieldId" to fv.fieldId,
                                    "titleKa" to fv.titleKa,
                                    "descriptionKa" to fv.descriptionKa,
                                    "value" to fv.value,
                                    "resultStatus" to fv.resultStatus,
                                    "isImportant" to fv.isImportant,
                                    "sourceName" to fv.sourceName
                                )
                            }
                            val repPayload = mapOf(
                                "requestId" to request.id,
                                "userId" to request.userId,
                                "imeiMasked" to request.imeiMasked,
                                "imeiHash" to request.imeiHash,
                                "brandId" to request.brandId,
                                "brandName" to request.brandName,
                                "verifiedModel" to verifiedModel.trim(),
                                "status" to newStatus.id,
                                "paymentStatus" to newPaymentStatus.id,
                                "quotedPrice" to request.quotedPrice,
                                "currency" to request.currency,
                                "fields" to fieldsMaps,
                                "sourcesUsed" to sourcesUsed,
                                "customerNotes" to finalCustomerNotes,
                                "resultText" to finalResultText,
                                "draftResultText" to finalDraftText,
                                "isPublished" to isPublished,
                                "verifiedAt" to FieldValue.serverTimestamp(),
                                "createdAt" to Timestamp(Date(request.createdAtMillis)),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                            db.collection("imei_reports").document(request.id).set(repPayload).await()

                            val histUpdateMap = mutableMapOf<String, Any?>(
                                "verifiedModel" to verifiedModel.trim(),
                                "status" to newStatus.id,
                                "paymentStatus" to newPaymentStatus.id,
                                "customerNotes" to finalCustomerNotes,
                                "resultText" to finalResultText,
                                "isPublished" to isPublished,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                            if (newStatus == ImeiRequestStatus.COMPLETED) {
                                histUpdateMap["completedAt"] = FieldValue.serverTimestamp()
                            }
                            db.collection("users").document(request.userId)
                                .collection("imei_history").document(request.id)
                                .set(histUpdateMap, SetOptions.merge()).await()
                        }

                        // Update accounting record
                        db.collection("imei_accounting").document(request.id).set(
                            mapOf(
                                "id" to request.id,
                                "userId" to request.userId,
                                "brandName" to request.brandName,
                                "quotedPrice" to request.quotedPrice,
                                "currency" to request.currency,
                                "paymentStatus" to newPaymentStatus.id,
                                "requestStatus" to newStatus.id,
                                "createdAt" to Timestamp(Date(request.createdAtMillis)),
                                "updatedAt" to FieldValue.serverTimestamp()
                            ),
                            SetOptions.merge()
                        ).await()

                        // Update aggregated analytics summary document (/imei_analytics/global_summary)
                        val computed = computeAnalytics(allReqs, loadAllReportsLocal(context), loadResultFields(context))
                        db.collection("imei_analytics").document("global_summary").set(
                            mapOf(
                                "periodId" to "global_summary",
                                "totalRequests" to computed.totalRequests,
                                "completedRequests" to computed.completedRequests,
                                "pendingRequests" to computed.pendingRequests,
                                "inProgressRequests" to computed.inProgressRequests,
                                "waitingInfoRequests" to computed.waitingForInfoRequests,
                                "rejectedRequests" to computed.rejectedRequests,
                                "paidRequestsCount" to computed.paidChecksCount,
                                "confirmedRevenueGel" to computed.confirmedPaidRevenueGel,
                                "refundedTotalGel" to computed.refundedTotalGel,
                                "outstandingPaymentGel" to computed.outstandingPaymentTotalGel,
                                "brandCounts" to computed.brandRequestCounts,
                                "sourceUsageCounts" to computed.sourceUsageCounts,
                                "totalProcessingMinutes" to computed.averageProcessingMinutes,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "imei_requests/${request.id}")
            }
        }

        // 3. Notify customer if requested and published (NEVER includes full IMEI!)
        // Prevents duplicate notifications for the same status and result
        if (notifyCustomer && isPublished && !request.userDeletedHistory) {
            val dedupeKey = "imei_notif_sent_${request.id}_${newStatus.id}_${finalResultText.hashCode()}"
            val prefs = context.getSharedPreferences("bekaservice_imei_platform_prefs_v1", Context.MODE_PRIVATE)
            val alreadySent = prefs.getBoolean(dedupeKey, false)
            if (!alreadySent) {
                prefs.edit().putBoolean(dedupeKey, true).apply()

                val (notifTitle, notifBody) = when (newStatus) {
                    ImeiRequestStatus.IN_PROGRESS -> {
                        "⏳ IMEI შემოწმება დაიწყო" to "${request.brandName} (${request.imeiMasked}) მოწმდება სპეციალისტის მიერ."
                    }
                    ImeiRequestStatus.WAITING_FOR_INFO -> {
                        "ℹ️ IMEI შემოწმება: საჭიროა დაზუსტება" to "${request.brandName} (${request.imeiMasked}): ${finalCustomerNotes.ifBlank { "გთხოვთ შეამოწმოთ დეტალები" }}"
                    }
                    ImeiRequestStatus.COMPLETED -> {
                        val modelText = verifiedModel.ifBlank { request.brandName }
                        "✅ IMEI შემოწმება დასრულდა!" to "$modelText (${request.imeiMasked}) — სრული ანგარიში მზადაა სანახავად."
                    }
                    ImeiRequestStatus.REJECTED -> {
                        "❌ IMEI შემოწმების მოთხოვნა უარყოფილია" to "${request.brandName} (${request.imeiMasked}): ${finalCustomerNotes.ifBlank { "იხილეთ დეტალები ისტორიაში" }}"
                    }
                    ImeiRequestStatus.PENDING -> {
                        "🔍 IMEI შემოწმების სტატუსი განახლდა" to "${request.brandName} (${request.imeiMasked}) მოლოდინის რეჟიმშია."
                    }
                }
                val notifId = "imei_notif_status_${request.id}_${newStatus.id}"
                BekaNotificationManager.upsertLocalNotificationRecord(
                    context = context,
                    record = PushNotificationRecord(
                        id = notifId,
                        title = notifTitle,
                        body = notifBody,
                        type = "service_update",
                        targetProductId = 0,
                        targetTab = 2,
                        senderEmail = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                        audience = request.userId,
                        status = "sent"
                    )
                )
                BekaNotificationManager.showNotification(
                    context = context,
                    notificationId = notifId,
                    title = notifTitle,
                    body = notifBody,
                    targetProductId = 0,
                    targetTab = 2,
                    type = "service_update"
                )
            }
        }

        notifyStateChanged()
        Result.success(report)
    }

    /**
     * Saves an administrator verification result as an unpublished DRAFT.
     * - Saves result text to /imei_requests/{id} in Firestore and local cache.
     * - Does NOT expose the draft to customer history or /imei_reports.
     * - Does NOT trigger customer notifications or mark order as completed.
     * - Allows administrator to reopen the order and resume editing.
     */
    suspend fun saveAdminVerificationDraft(
        context: Context,
        request: ImeiCheckRequest,
        draftText: String,
        newStatus: ImeiRequestStatus = request.statusEnum,
        newPaymentStatus: ImeiPaymentStatus = request.paymentStatusEnum,
        verifiedModel: String = request.verifiedModel,
        internalNotes: String = request.internalNotes
    ): Result<ImeiCheckRequest> = withContext(Dispatchers.IO) {
        val activeEmail = AuthAndOrderManager.resolveActiveUserEmail(context)
        if (activeEmail.isNotBlank() && !AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return@withContext Result.failure(
                SecurityException("PERMISSION_DENIED: მხოლოდ ავტორიზებულ ადმინისტრატორს (beqa1994@gmail.com) აქვს IMEI შეკვეთის მართვის უფლება.")
            )
        }
        val now = System.currentTimeMillis()
        val updatedRequest = request.copy(
            status = newStatus.id,
            paymentStatus = newPaymentStatus.id,
            verifiedModel = verifiedModel.trim(),
            internalNotes = internalNotes.trim(),
            draftResultText = draftText.trim(),
            isPublished = false,
            updatedAtMillis = now
        )

        // 1. Update local cache
        val allReqs = loadAllRequestsLocal(context).toMutableList()
        val reqIdx = allReqs.indexOfFirst { it.id == request.id }
        if (reqIdx >= 0) allReqs[reqIdx] = updatedRequest else allReqs.add(0, updatedRequest)
        saveAllRequestsLocal(context, allReqs)

        // 2. Sync draft to Firestore /imei_requests/{id} only
        val db = getFirestoreOrNull(context)
        if (db != null) {
            try {
                withTimeoutOrNull(4500L) {
                    AuthAndOrderManager.ensureAdminFirebaseSession(context)
                    val activeFbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
                    if (activeFbUser != null) {
                        val reqUpdateMap = mutableMapOf<String, Any?>(
                            "status" to newStatus.id,
                            "paymentStatus" to newPaymentStatus.id,
                            "verifiedModel" to verifiedModel.trim(),
                            "internalNotes" to internalNotes.trim(),
                            "draftResultText" to draftText.trim(),
                            "isPublished" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        db.collection("imei_requests").document(request.id).set(reqUpdateMap, SetOptions.merge()).await()
                    }
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "imei_requests/${request.id}")
            }
        }

        notifyStateChanged()
        Result.success(updatedRequest)
    }

    // --------------------------------------------------------------------------
    // CUSTOMER HISTORY SYNC & SECURE OWNERSHIP-VERIFIED DELETION (Phase 7)
    // --------------------------------------------------------------------------

    fun listenToUserImeiHistory(
        context: Context,
        userId: String,
        onUpdated: (List<ImeiHistoryEntry>) -> Unit
    ): ListenerRegistration? {
        if (userId.isBlank()) return null
        val db = getFirestoreOrNull(context) ?: return null
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() ?: return null
        if (fbUser.uid != userId) return null

        return try {
            db.collection("users").document(userId).collection("imei_history")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    val previousById = loadUserHistoryLocal(context, userId).associateBy { it.id }
                    val cloudList = snap.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val docUserId = doc.getString("userId") ?: userId
                        if (docUserId != userId) return@mapNotNull null
                        ImeiHistoryEntry(
                            id = id,
                            userId = docUserId,
                            imeiMasked = doc.getString("imeiMasked") ?: "******",
                            imeiHash = doc.getString("imeiHash") ?: "",
                            brandId = doc.getString("brandId") ?: "",
                            brandName = doc.getString("brandName") ?: "",
                            verifiedModel = doc.getString("verifiedModel") ?: "",
                            status = doc.getString("status") ?: ImeiRequestStatus.PENDING.id,
                            paymentStatus = doc.getString("paymentStatus") ?: ImeiPaymentStatus.PENDING.id,
                            quotedPrice = doc.getDouble("quotedPrice") ?: 3.0,
                            currency = doc.getString("currency") ?: "GEL",
                            estimatedTimeText = doc.getString("estimatedTimeText") ?: "5–30 წთ",
                            customerNotes = doc.getString("customerNotes") ?: "",
                            createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                            completedAtMillis = doc.getTimestamp("completedAt")?.toDate()?.time ?: 0L,
                            updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }.sortedByDescending { it.createdAtMillis }

                    // Detect status transitions for customer notification delivery
                    cloudList.forEach { entry ->
                        val prev = previousById[entry.id]
                        if (prev != null && prev.status != entry.status) {
                            val newSt = ImeiRequestStatus.fromId(entry.status)
                            val (title, body) = when (newSt) {
                                ImeiRequestStatus.IN_PROGRESS ->
                                    "⏳ IMEI შემოწმება დაიწყო" to "${entry.brandName} (${entry.imeiMasked}) მოწმდება სპეციალისტის მიერ."
                                ImeiRequestStatus.WAITING_FOR_INFO ->
                                    "ℹ️ IMEI შემოწმება: საჭიროა დაზუსტება" to "${entry.brandName} (${entry.imeiMasked}): ${entry.customerNotes.ifBlank { "გთხოვთ შეამოწმოთ დეტალები" }}"
                                ImeiRequestStatus.COMPLETED ->
                                    "✅ IMEI შემოწმება დასრულდა!" to "${entry.verifiedModel.ifBlank { entry.brandName }} (${entry.imeiMasked}) — სრული ანგარიში მზადაა სანახავად."
                                ImeiRequestStatus.REJECTED ->
                                    "❌ IMEI შემოწმების მოთხოვნა უარყოფილია" to "${entry.brandName} (${entry.imeiMasked}): ${entry.customerNotes.ifBlank { "იხილეთ დეტალები ისტორიაში" }}"
                                ImeiRequestStatus.PENDING ->
                                    "🔍 IMEI შემოწმების სტატუსი განახლდა" to "${entry.brandName} (${entry.imeiMasked}) მოლოდინის რეჟიმშია."
                            }
                            val notifId = "imei_notif_status_${entry.id}_${newSt.id}"
                            BekaNotificationManager.upsertLocalNotificationRecord(
                                context = context,
                                record = PushNotificationRecord(
                                    id = notifId,
                                    title = title,
                                    body = body,
                                    type = "service_update",
                                    targetProductId = 0,
                                    targetTab = 2,
                                    senderEmail = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                                    createdAtMillis = System.currentTimeMillis(),
                                    updatedAtMillis = System.currentTimeMillis(),
                                    audience = userId,
                                    status = "sent"
                                )
                            )
                            BekaNotificationManager.showNotification(
                                context = context,
                                notificationId = notifId,
                                title = title,
                                body = body,
                                targetProductId = 0,
                                targetTab = 2,
                                type = "service_update"
                            )
                        }
                    }

                    saveUserHistoryLocal(context, userId, cloudList)
                    onUpdated(cloudList)
                }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchReportForRequest(
        context: Context,
        requestId: String,
        currentUserId: String,
        isAdminCaller: Boolean = false
    ): Result<ImeiVerificationReport> = withContext(Dispatchers.IO) {
        if (requestId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("რეპორტის ID ცარიელია"))
        }

        // Check local cache first with ownership check
        val localReports = loadAllReportsLocal(context)
        val cached = localReports[requestId]
        if (cached != null) {
            if (!isAdminCaller && cached.userId != currentUserId) {
                return@withContext Result.failure(SecurityException("წვდომა უარყოფილია: თქვენ არ გაქვთ სხვა მომხმარებლის რეპორტის ნახვის უფლება."))
            }
            if (isAdminCaller || cached.isPublished) {
                return@withContext Result.success(cached)
            }
        }

        val db = getFirestoreOrNull(context)
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (db != null && fbUser != null) {
            try {
                val doc = withTimeoutOrNull(4500L) {
                    db.collection("imei_reports").document(requestId).get().await()
                }
                if (doc != null && doc.exists()) {
                    val docOwner = doc.getString("userId").orEmpty()
                    if (!isAdminCaller && docOwner != currentUserId) {
                        return@withContext Result.failure(SecurityException("წვდომა უარყოფილია: სხვა მომხმარებლის რეპორტი."))
                    }
                    val isPublished = doc.getBoolean("isPublished") ?: true
                    if (!isAdminCaller && !isPublished) {
                        // Unpublished draft is not returned to customers
                    } else {
                        val rawFields = (doc.get("fields") as? List<*>)?.mapNotNull { item ->
                            val map = item as? Map<*, *> ?: return@mapNotNull null
                            ImeiResultFieldValue(
                                fieldId = (map["fieldId"] as? String).orEmpty(),
                                titleKa = (map["titleKa"] as? String).orEmpty(),
                                descriptionKa = (map["descriptionKa"] as? String).orEmpty(),
                                value = (map["value"] as? String).orEmpty(),
                                resultStatus = (map["resultStatus"] as? String) ?: ImeiResultStatus.NOT_CHECKED.id,
                                isImportant = (map["isImportant"] as? Boolean) ?: false,
                                sourceName = (map["sourceName"] as? String).orEmpty()
                            )
                        }.orEmpty()
                        val sources = (doc.get("sourcesUsed") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                        val loaded = ImeiVerificationReport(
                            requestId = doc.getString("requestId") ?: requestId,
                            userId = docOwner,
                            imeiMasked = doc.getString("imeiMasked") ?: "******",
                            imeiHash = doc.getString("imeiHash") ?: "",
                            brandId = doc.getString("brandId") ?: "",
                            brandName = doc.getString("brandName") ?: "",
                            verifiedModel = doc.getString("verifiedModel") ?: "",
                            status = doc.getString("status") ?: ImeiRequestStatus.COMPLETED.id,
                            paymentStatus = doc.getString("paymentStatus") ?: ImeiPaymentStatus.PENDING.id,
                            quotedPrice = doc.getDouble("quotedPrice") ?: 3.0,
                            currency = doc.getString("currency") ?: "GEL",
                            fields = rawFields,
                            sourcesUsed = sources,
                            customerNotes = doc.getString("customerNotes") ?: "",
                            resultText = doc.getString("resultText") ?: (doc.getString("customerNotes") ?: ""),
                            draftResultText = if (isAdminCaller) (doc.getString("draftResultText") ?: "") else "",
                            isPublished = isPublished,
                            verifiedAtMillis = doc.getTimestamp("verifiedAt")?.toDate()?.time ?: System.currentTimeMillis(),
                            createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                            updatedAtMillis = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                        )
                        saveReportLocal(context, loaded)
                        return@withContext Result.success(loaded)
                    }
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, "imei_reports/$requestId")
                return@withContext Result.failure(e)
            }
        }

        // If report is not yet published by admin, construct a live status preview from the user's history/request
        val histItem = loadUserHistoryLocal(context, currentUserId).find { it.id == requestId }
        if (histItem != null && (isAdminCaller || histItem.userId == currentUserId)) {
            val provisional = ImeiVerificationReport(
                requestId = histItem.id,
                userId = histItem.userId,
                imeiMasked = histItem.imeiMasked,
                imeiHash = histItem.imeiHash,
                brandId = histItem.brandId,
                brandName = histItem.brandName,
                verifiedModel = histItem.verifiedModel,
                status = histItem.status,
                paymentStatus = histItem.paymentStatus,
                quotedPrice = histItem.quotedPrice,
                currency = histItem.currency,
                fields = emptyList(),
                sourcesUsed = emptyList(),
                customerNotes = if (histItem.isPublished || isAdminCaller) histItem.customerNotes else "",
                resultText = if (histItem.isPublished || isAdminCaller) (if (histItem.resultText.isNotBlank()) histItem.resultText else histItem.customerNotes) else "",
                isPublished = histItem.isPublished,
                verifiedAtMillis = histItem.completedAtMillis,
                createdAtMillis = histItem.createdAtMillis,
                updatedAtMillis = histItem.updatedAtMillis
            )
            return@withContext Result.success(provisional)
        }

        Result.failure(IllegalStateException("რეპორტი არ მოიძებნა ან წაშლილია."))
    }

    /**
     * Phase 7.2: კონკრეტული ისტორიის ჩანაწერის უსაფრთხო წაშლა მფლობელის ვერიფიკაციით.
     * - შლის მომხმარებლის ისტორიის ჩანაწერს (/users/{uid}/imei_history/{id})
     * - შლის მომხმარებლის პირად რეპორტს (/imei_reports/{id}) და ლოკალურ PDF ფაილს
     * - ანონიმიზებს სრულ IMEI-ს (/imei_requests/{id}-ში imei = "DELETED", userDeletedHistory = true),
     *   რათა ადმინმა არ განაგრძოს წაშლილი მოთხოვნის დამუშავება და სრული IMEI აღარ ინახებოდეს.
     */
    suspend fun deleteUserHistoryEntry(
        context: Context,
        currentUserId: String,
        entryId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (currentUserId.isBlank() || entryId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("არასწორი მომხმარებლის ან ჩანაწერის იდენტიფიკატორი"))
        }

        val currentHist = loadUserHistoryLocal(context, currentUserId)
        val target = currentHist.find { it.id == entryId }
        if (target != null && target.userId != currentUserId) {
            return@withContext Result.failure(SecurityException("წვდომა უარყოფილია: სხვა მომხმარებლის ისტორიის წაშლა აკრძალულია!"))
        }

        // 1. Remove from local history & report cache & delete any exported PDF file
        val updatedHist = currentHist.filterNot { it.id == entryId }
        saveUserHistoryLocal(context, currentUserId, updatedHist)
        removeReportLocal(context, entryId)
        deleteLocalPdfReportFile(context, entryId)

        // Anonymize local request record so admin sees userDeletedHistory = true and imei = "DELETED"
        val allReqs = loadAllRequestsRawInternal(context).map { req ->
            if (req.id == entryId && req.userId == currentUserId) {
                req.copy(
                    imei = "DELETED",
                    userDeletedHistory = true,
                    updatedAtMillis = System.currentTimeMillis()
                )
            } else req
        }
        saveAllRequestsLocal(context, allReqs)

        // 2. Execute secure Firestore deletions & anonymization
        val db = getFirestoreOrNull(context)
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (db != null && fbUser != null && fbUser.uid == currentUserId) {
            try {
                db.collection("users").document(currentUserId)
                    .collection("imei_history").document(entryId)
                    .delete().await()

                runCatching {
                    db.collection("imei_reports").document(entryId).delete().await()
                }

                runCatching {
                    db.collection("imei_requests").document(entryId).update(
                        mapOf(
                            "imei" to "DELETED",
                            "userDeletedHistory" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                val auditId = "audit_del_${entryId}_${System.currentTimeMillis()}"
                runCatching {
                    db.collection("imei_audit_logs").document(auditId).set(
                        mapOf(
                            "id" to auditId,
                            "actorUid" to currentUserId,
                            "action" to "history_deleted",
                            "targetRequestId" to entryId,
                            "details" to "User deleted individual IMEI history entry and anonymized full IMEI",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "users/$currentUserId/imei_history/$entryId")
                return@withContext Result.failure(e)
            }
        }

        Result.success(Unit)
    }

    /**
     * Phase 7.3: მომხმარებლის მთელი IMEI ისტორიის სრული გასუფთავება ("მთელი ისტორიის გასუფთავება").
     */
    suspend fun clearAllUserHistory(
        context: Context,
        currentUserId: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (currentUserId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("მომხმარებელი არ არის ავტორიზებული"))
        }

        val currentHist = loadUserHistoryLocal(context, currentUserId)
        val entryIds = currentHist.filter { it.userId == currentUserId }.map { it.id }

        // Clear local history & reports for this user only
        saveUserHistoryLocal(context, currentUserId, emptyList())
        entryIds.forEach { id ->
            removeReportLocal(context, id)
            deleteLocalPdfReportFile(context, id)
        }

        val allReqs = loadAllRequestsRawInternal(context).map { req ->
            if (req.userId == currentUserId) {
                req.copy(
                    imei = "DELETED",
                    userDeletedHistory = true,
                    updatedAtMillis = System.currentTimeMillis()
                )
            } else req
        }
        saveAllRequestsLocal(context, allReqs)

        val db = getFirestoreOrNull(context)
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (db != null && fbUser != null && fbUser.uid == currentUserId) {
            try {
                val snap = db.collection("users").document(currentUserId)
                    .collection("imei_history").get().await()
                val allIdsToClean = (entryIds + snap.documents.map { it.id }).distinct()
                for (docId in allIdsToClean) {
                    runCatching {
                        db.collection("users").document(currentUserId)
                            .collection("imei_history").document(docId)
                            .delete().await()
                    }
                    runCatching {
                        db.collection("imei_reports").document(docId).delete().await()
                    }
                    runCatching {
                        db.collection("imei_requests").document(docId).update(
                            mapOf(
                                "imei" to "DELETED",
                                "userDeletedHistory" to true,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }

                val auditId = "audit_clear_${currentUserId.take(8)}_${System.currentTimeMillis()}"
                runCatching {
                    db.collection("imei_audit_logs").document(auditId).set(
                        mapOf(
                            "id" to auditId,
                            "actorUid" to currentUserId,
                            "action" to "history_cleared",
                            "targetRequestId" to "ALL",
                            "details" to "User cleared all IMEI history (${allIdsToClean.size} entries)",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, "users/$currentUserId/imei_history")
                return@withContext Result.failure(e)
            }
        }

        Result.success(entryIds.size)
    }

    private fun deleteLocalPdfReportFile(context: Context, requestId: String) {
        try {
            val dir = File(context.filesDir, "imei_reports_pdf")
            val file = File(dir, "BEKASERVICE_IMEI_${requestId}.pdf")
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    // --------------------------------------------------------------------------
    // PDF REPORT GENERATION & EXPORT (Phase 6)
    // --------------------------------------------------------------------------

    suspend fun generateReportPdfFile(
        context: Context,
        report: ImeiVerificationReport
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "imei_reports_pdf")
            if (!dir.exists()) dir.mkdirs()
            val outFile = File(dir, "BEKASERVICE_IMEI_${report.requestId}.pdf")

            runCatching {
                val pdfDocument = PdfDocument()
                try {
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    val bgPaint = Paint().apply { color = android.graphics.Color.parseColor("#0D0F12") }
                    canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

                    val orangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#FF6B00")
                        textSize = 18f
                        isFakeBoldText = true
                    }
                    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = 14f
                        isFakeBoldText = true
                    }
                    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.parseColor("#CBD5E1")
                        textSize = 11f
                    }

                    var y = 45f
                    canvas.drawText("BEKASERVICE • IMEI ვერიფიკაციის ოფიციალური ამონაწერი", 32f, y, orangePaint)
                    y += 24f
                    canvas.drawText("მოთხოვნის ID: ${report.requestId}", 32f, y, bodyPaint)
                    y += 18f
                    canvas.drawText("IMEI (დაცული): ${report.imeiMasked}", 32f, y, titlePaint)
                    y += 18f
                    canvas.drawText("მწარმოებელი: ${report.brandName} | მოდელი: ${report.verifiedModel.ifBlank { "დაუზუსტებელი" }}", 32f, y, titlePaint)
                    y += 18f
                    canvas.drawText("თარიღი: ${formatTimestampKa(report.verifiedAtMillis)} | ღირებულება: ${report.formattedPrice}", 32f, y, bodyPaint)
                    y += 26f

                    canvas.drawText("შემოწმების შედეგები:", 32f, y, orangePaint)
                    y += 20f

                    val resLines = (report.resultText.ifBlank { report.customerNotes }).lines()
                    if (resLines.isNotEmpty() && resLines.any { it.isNotBlank() }) {
                        resLines.forEach { rLine ->
                            if (y > 760f) return@forEach
                            canvas.drawText(rLine.take(90), 36f, y, bodyPaint)
                            y += 16f
                        }
                    } else {
                        report.fields.forEach { fv ->
                            if (y > 760f) return@forEach
                            val st = ImeiResultStatus.fromId(fv.resultStatus)
                            val line = "• ${fv.titleKa}: ${fv.value.ifBlank { st.labelKa }} [${st.labelKa}]"
                            canvas.drawText(line.take(90), 36f, y, bodyPaint)
                            y += 18f
                        }
                    }

                    if (report.sourcesUsed.isNotEmpty() && y < 780f) {
                        y += 10f
                        canvas.drawText("გამოყენებული წყაროები: ${report.sourcesUsed.joinToString(", ").take(80)}", 32f, y, bodyPaint)
                    }
                    if (report.customerNotes.isNotBlank() && report.customerNotes != report.resultText && y < 800f) {
                        y += 18f
                        canvas.drawText("შენიშვნა: ${report.customerNotes.take(95)}", 32f, y, bodyPaint)
                    }

                    pdfDocument.finishPage(page)
                    FileOutputStream(outFile).use { out ->
                        pdfDocument.writeTo(out)
                    }
                } finally {
                    runCatching { pdfDocument.close() }
                }
            }

            if (!outFile.exists() || outFile.length() == 0L) {
                val fallbackContent = buildString {
                    appendLine("%PDF-1.4")
                    appendLine("% BEKASERVICE OFFICIAL IMEI VERIFICATION REPORT")
                    appendLine("Request ID: ${report.requestId}")
                    appendLine("Masked IMEI: ${report.imeiMasked}")
                    appendLine("Brand: ${report.brandName} | Model: ${report.verifiedModel}")
                    appendLine("Status: ${report.status} | Price: ${report.formattedPrice}")
                    report.fields.forEach { f ->
                        appendLine("${f.titleKa}: ${f.value} [${f.resultStatus}]")
                    }
                    appendLine("%%EOF")
                }
                outFile.writeText(fallbackContent, Charsets.UTF_8)
            }
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
