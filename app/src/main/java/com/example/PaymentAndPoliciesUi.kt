package com.example

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.*

// ==============================================================================
// 1. BSTORE PAYMENT SYSTEM — CORE ENUMS & DATA MODELS
// ==============================================================================

enum class PaymentMethodType(
    val code: String,
    val titleKa: String,
    val subtitleKa: String
) {
    BANK_TRANSFER(
        code = "bank_transfer",
        titleKa = "საბანკო გადარიცხვა",
        subtitleKa = "გადარიცხვა BSTORE-ის არჩეულ საბანკო ანგარიშზე (BOG / TBC)"
    ),
    COD(
        code = "cod",
        titleKa = "ნაღდი ანგარიშსწორება",
        subtitleKa = "გადაიხადე ნივთის მიღებისას"
    ),
    LEGACY_UNSPECIFIED(
        code = "legacy_unspecified",
        titleKa = "სტანდარტული შეკვეთა",
        subtitleKa = "შეკვეთის დეტალების დაზუსტება"
    );

    companion object {
        fun fromCode(code: String?, legacyPaymentMethodLabel: String = ""): PaymentMethodType {
            return when (code?.trim()?.lowercase()) {
                "bank_transfer" -> BANK_TRANSFER
                "cod" -> COD
                "legacy_unspecified" -> LEGACY_UNSPECIFIED
                else -> {
                    val lower = legacyPaymentMethodLabel.lowercase()
                    when {
                        lower.contains("საბანკო გადარიცხვა") && !lower.contains("whatsapp") && !lower.contains("ნაღდი /") -> BANK_TRANSFER
                        lower.contains("ნაღდი ანგარიშსწორება") -> COD
                        else -> LEGACY_UNSPECIFIED
                    }
                }
            }
        }
    }
}

enum class PaymentStatus(
    val code: String,
    val customerLabelKa: String,
    val adminLabelKa: String,
    val badgeColorHex: Long
) {
    PENDING(
        code = "pending",
        customerLabelKa = "გადახდის მოლოდინში",
        adminLabelKa = "გადახდის მოლოდინში",
        badgeColorHex = 0xFFF59E0B
    ),
    VERIFICATION_PENDING(
        code = "verification_pending",
        customerLabelKa = "გადარიცხვა მოწმდება",
        adminLabelKa = "გადარიცხვა მოწმდება",
        badgeColorHex = 0xFF60A5FA
    ),
    AWAITING_COLLECTION(
        code = "awaiting_collection",
        customerLabelKa = "გადაიხადე ნივთის მიღებისას",
        adminLabelKa = "COD მოლოდინში",
        badgeColorHex = 0xFF38BDF8
    ),
    PAID(
        code = "paid",
        customerLabelKa = "გადახდილია",
        adminLabelKa = "გადახდილია",
        badgeColorHex = 0xFF4ADE80
    ),
    FAILED(
        code = "failed",
        customerLabelKa = "გადახდა ვერ დადასტურდა",
        adminLabelKa = "გადახდა ვერ დადასტურდა",
        badgeColorHex = 0xFFEF4444
    ),
    CANCELLED(
        code = "cancelled",
        customerLabelKa = "გადახდა გაუქმებულია",
        adminLabelKa = "გაუქმებული",
        badgeColorHex = 0xFFEF4444
    ),
    EXPIRED(
        code = "expired",
        customerLabelKa = "ვადაგასული",
        adminLabelKa = "ვადაგასული",
        badgeColorHex = 0xFF94A3B8
    ),
    REFUNDED(
        code = "refunded",
        customerLabelKa = "თანხა დაბრუნებულია",
        adminLabelKa = "თანხა დაბრუნებულია",
        badgeColorHex = 0xFFA855F7
    );

    companion object {
        fun fromCode(code: String?, methodType: PaymentMethodType = PaymentMethodType.LEGACY_UNSPECIFIED): PaymentStatus {
            return when (code?.trim()?.lowercase()) {
                "pending" -> PENDING
                "verification_pending" -> VERIFICATION_PENDING
                "awaiting_collection" -> AWAITING_COLLECTION
                "paid" -> PAID
                "failed" -> FAILED
                "cancelled" -> CANCELLED
                "expired" -> EXPIRED
                "refunded" -> REFUNDED
                else -> {
                    // Never infer old/missing paymentStatus as PAID!
                    if (methodType == PaymentMethodType.COD) AWAITING_COLLECTION else PENDING
                }
            }
        }
    }
}

enum class DeliveryMethodType(
    val code: String,
    val titleKa: String,
    val subtitleKa: String,
    val feeLabelKa: String
) {
    PICKUP(
        code = "pickup",
        titleKa = "ფილიალიდან გატანა (უფასო)",
        subtitleKa = "ახალციხე, კოსტავას N10 — შეკვეთის მზადყოფნის შეტყობინების შემდეგ",
        feeLabelKa = "უფასო (0 ₾)"
    ),
    COURIER(
        code = "courier",
        titleKa = "საკურიერო მიწოდება საქართველოს მასშტაბით",
        subtitleKa = "ახლომდებარე ქალაქები: 1–3 სამუშაო დღე • სხვა რეგიონები: 2–5 სამუშაო დღე",
        feeLabelKa = "ტარიფი ქალაქისა და მანძილის მიხედვით"
    );

    companion object {
        fun fromCode(code: String?): DeliveryMethodType {
            return if (code?.trim()?.lowercase() == "courier") COURIER else PICKUP
        }
    }
}

data class BankAccountConfig(
    val id: String,
    val bankCode: String, // "bog", "tbc", or custom bank code
    val bankName: String, // "საქართველოს ბანკი", "თიბისი ბანკი", etc.
    val recipientName: String,
    val iban: String,
    val description: String = "",
    val paymentInstructions: String = DEFAULT_PAYMENT_INSTRUCTIONS_KA,
    val isEnabled: Boolean = true,
    val displayOrder: Int = 1,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val updatedBy: String = AuthAndOrderManager.SOLE_ADMIN_EMAIL
) {
    fun brandColor(): Color = when (bankCode.lowercase()) {
        "bog" -> Color(0xFFFF5E00)
        "tbc" -> Color(0xFF00A3E0)
        else -> Color(0xFFFF6B00)
    }

    fun shortBadgeText(): String = when (bankCode.lowercase()) {
        "bog" -> "BOG"
        "tbc" -> "TBC"
        else -> bankCode.take(4).uppercase().ifBlank { "BANK" }
    }

    fun toMap(): Map<String, Any> = hashMapOf(
        "id" to id,
        "bankCode" to bankCode,
        "bankName" to bankName,
        "recipientName" to recipientName,
        "iban" to iban,
        "description" to description,
        "paymentInstructions" to paymentInstructions,
        "isEnabled" to isEnabled,
        "displayOrder" to displayOrder,
        "createdAtMillis" to createdAtMillis,
        "updatedAtMillis" to updatedAtMillis,
        "updatedBy" to updatedBy
    )

    companion object {
        const val DEFAULT_PAYMENT_INSTRUCTIONS_KA =
            "გადარიცხვისას დანიშნულებაში (კომენტარში) აუცილებლად მიუთითეთ შეკვეთის ნომერი. გადახდა დადასტურდება ჩარიცხვის გადამოწმების შემდეგ."

        fun fromJson(obj: JSONObject): BankAccountConfig {
            return BankAccountConfig(
                id = obj.optString("id", "bank_${System.currentTimeMillis()}"),
                bankCode = obj.optString("bankCode", "bog"),
                bankName = obj.optString("bankName", "საქართველოს ბანკი"),
                recipientName = obj.optString("recipientName", ""),
                iban = obj.optString("iban", "").uppercase().replace(" ", ""),
                description = obj.optString("description", ""),
                paymentInstructions = obj.optString("paymentInstructions", DEFAULT_PAYMENT_INSTRUCTIONS_KA)
                    .ifBlank { DEFAULT_PAYMENT_INSTRUCTIONS_KA },
                isEnabled = obj.optBoolean("isEnabled", true),
                displayOrder = obj.optInt("displayOrder", 1),
                createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis()),
                updatedBy = obj.optString("updatedBy", AuthAndOrderManager.SOLE_ADMIN_EMAIL)
            )
        }
    }
}

data class PaymentAuditEvent(
    val id: String,
    val orderId: String,
    val orderNumber: String,
    val eventType: String, // "ORDER_PAYMENT_INITIALIZED", "BANK_TRANSFER_CONFIRMED", "COD_COLLECTION_CONFIRMED", "PAYMENT_STATUS_CHANGED", "BANK_ACCOUNT_UPDATED"
    val previousStatus: String,
    val newStatus: String,
    val amount: Double,
    val currency: String = "GEL",
    val actorUid: String,
    val actorEmail: String,
    val actorRole: String, // "customer", "sole_admin"
    val timestampMillis: Long = System.currentTimeMillis(),
    val note: String = ""
) {
    val formattedTimestamp: String
        get() = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.forLanguageTag("ka")).format(Date(timestampMillis))

    fun eventTitleKa(): String = when (eventType) {
        "ORDER_PAYMENT_INITIALIZED" -> "შეკვეთის გადახდის ინიციალიზაცია"
        "BANK_TRANSFER_CONFIRMED" -> "საბანკო ჩარიცხვის დადასტურება"
        "COD_COLLECTION_CONFIRMED" -> "ნაღდი ანგარიშსწორების მიღების დადასტურება"
        "PAYMENT_STATUS_CHANGED" -> "გადახდის სტატუსის ცვლილება"
        "BANK_ACCOUNT_UPDATED" -> "საბანკო ანგარიშის კონფიგურაციის განახლება"
        else -> eventType
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("orderId", orderId)
        put("orderNumber", orderNumber)
        put("eventType", eventType)
        put("previousStatus", previousStatus)
        put("newStatus", newStatus)
        put("amount", amount)
        put("currency", currency)
        put("actorUid", actorUid)
        put("actorEmail", actorEmail)
        put("actorRole", actorRole)
        put("timestampMillis", timestampMillis)
        put("note", note)
    }

    companion object {
        fun fromJson(obj: JSONObject): PaymentAuditEvent = PaymentAuditEvent(
            id = obj.optString("id", "audit_${System.currentTimeMillis()}"),
            orderId = obj.optString("orderId", ""),
            orderNumber = obj.optString("orderNumber", ""),
            eventType = obj.optString("eventType", "PAYMENT_STATUS_CHANGED"),
            previousStatus = obj.optString("previousStatus", "pending"),
            newStatus = obj.optString("newStatus", "pending"),
            amount = obj.optDouble("amount", 0.0),
            currency = obj.optString("currency", "GEL"),
            actorUid = obj.optString("actorUid", ""),
            actorEmail = obj.optString("actorEmail", ""),
            actorRole = obj.optString("actorRole", "customer"),
            timestampMillis = obj.optLong("timestampMillis", 0L).takeIf { it > 0L }
                ?: obj.optLong("timestamp", System.currentTimeMillis()),
            note = obj.optString("note", "")
        )
    }
}

enum class LegalPolicyTopic(
    val id: String,
    val titleKa: String,
    val shortSubtitleKa: String
) {
    DELIVERY_POLICY(
        id = "delivery_policy",
        titleKa = "მიწოდების პოლიტიკა",
        shortSubtitleKa = "ფილიალიდან უფასო გატანა და საკურიერო მიწოდება საქართველოს მასშტაბით"
    ),
    RETURN_AND_REFUND_POLICY(
        id = "return_refund_policy",
        titleKa = "დაბრუნება და თანხის ანაზღაურება",
        shortSubtitleKa = "პროდუქციის შემოწმების, შეცვლისა და თანხის დაბრუნების პირობები"
    ),
    PRIVACY_POLICY(
        id = "privacy_policy",
        titleKa = "კონფიდენციალურობის პოლიტიკა",
        shortSubtitleKa = "მომხმარებლის მონაცემების უსაფრთხოება და დამუშავების წესი"
    ),
    TERMS_AND_CONDITIONS(
        id = "terms_and_conditions",
        titleKa = "წესები და პირობები",
        shortSubtitleKa = "შეკვეთის გაფორმებისა და ანგარიშსწორების ოფიციალური პირობები"
    ),
    CONTACT_INFO(
        id = "contact_info",
        titleKa = "კონტაქტი",
        shortSubtitleKa = "ახალციხე, კოსტავას N10 • +995 598 23 33 32"
    )
}

data class CheckoutSubmissionConfig(
    val paymentMethod: PaymentMethodType,
    val selectedBankAccount: BankAccountConfig? = null,
    val deliveryMethod: DeliveryMethodType = DeliveryMethodType.PICKUP,
    val deliveryCity: String = "ახალციხე",
    val deliveryAddress: String = "ქ. ახალციხე, კოსტავას N10 (ფილიალიდან გატანა)",
    val customerPhone: String = "",
    val customerNote: String = "",
    val acceptedTermsVersion: String = BstorePaymentManager.CURRENT_TERMS_VERSION,
    val acceptedTermsAtMillis: Long = System.currentTimeMillis(),
    val clientOrderId: String = ""
)

// ==============================================================================
// 2. BSTORE PAYMENT MANAGER — BANK ACCOUNTS, VALIDATION & AUDIT TRAIL
// ==============================================================================

object BstorePaymentManager {
    private const val PREFS_NAME = "bstore_payment_system_prefs"
    private const val KEY_BANK_ACCOUNTS_JSON = "payment_bank_accounts_json"
    private const val KEY_AUDIT_LOGS_JSON = "payment_audit_logs_json"
    const val CURRENT_TERMS_VERSION = "BSTORE-TERMS-2026.1"

    val stateVersion = mutableIntStateOf(0)

    private val GEORGIAN_IBAN_REGEX = Regex("^GE[0-9]{2}[A-Z0-9]{2}[0-9]{16}$")
    private val GENERIC_IBAN_REGEX = Regex("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$")

    private fun getPrefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun init(context: Context) {
        runCatching { getAllBankAccounts(context) }
    }

    fun normalizeIban(raw: String): String =
        raw.trim().uppercase(Locale.ROOT).replace(Regex("\\s+"), "")

    fun isValidIban(raw: String): Boolean {
        val clean = normalizeIban(raw)
        if (clean.startsWith("GE")) {
            return GEORGIAN_IBAN_REGEX.matches(clean)
        }
        return GENERIC_IBAN_REGEX.matches(clean)
    }

    fun getAllBankAccounts(context: Context): List<BankAccountConfig> {
        val prefs = getPrefs(context)
        val raw = prefs.getString(KEY_BANK_ACCOUNTS_JSON, null)
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                val list = mutableListOf<BankAccountConfig>()
                for (i in 0 until arr.length()) {
                    list.add(BankAccountConfig.fromJson(arr.getJSONObject(i)))
                }
                return list.sortedWith(compareBy({ it.displayOrder }, { it.bankName }))
            } catch (_: Exception) {}
        }

        // Only hydrate from legacy AdminStorage if Admin explicitly saved real bank requisites
        val legacy = AdminStorage.getBankRequisites(context)
        val legacyAccounts = mutableListOf<BankAccountConfig>()
        if (isValidIban(legacy.bogIban) && legacy.bogRecipient.isNotBlank()) {
            legacyAccounts.add(
                BankAccountConfig(
                    id = "bank_bog_primary",
                    bankCode = "bog",
                    bankName = "საქართველოს ბანკი",
                    recipientName = legacy.bogRecipient.trim(),
                    iban = normalizeIban(legacy.bogIban),
                    description = "საქართველოს ბანკის (BOG) ლარის ანგარიში",
                    paymentInstructions = BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA,
                    isEnabled = true,
                    displayOrder = 1
                )
            )
        }
        if (isValidIban(legacy.tbcIban) && legacy.tbcRecipient.isNotBlank()) {
            legacyAccounts.add(
                BankAccountConfig(
                    id = "bank_tbc_primary",
                    bankCode = "tbc",
                    bankName = "თიბისი ბანკი",
                    recipientName = legacy.tbcRecipient.trim(),
                    iban = normalizeIban(legacy.tbcIban),
                    description = "თიბისი ბანკის (TBC) ლარის ანგარიში",
                    paymentInstructions = BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA,
                    isEnabled = true,
                    displayOrder = 2
                )
            )
        }
        return legacyAccounts
    }

    fun getEnabledBankAccounts(context: Context): List<BankAccountConfig> {
        return getAllBankAccounts(context)
            .filter { it.isEnabled && isValidIban(it.iban) && it.recipientName.isNotBlank() }
            .sortedWith(compareBy({ it.displayOrder }, { it.bankName }))
    }

    fun saveBankAccountsLocal(context: Context, accounts: List<BankAccountConfig>) {
        val sorted = accounts.sortedWith(compareBy({ it.displayOrder }, { it.bankName }))
        val arr = JSONArray()
        sorted.forEach { acc ->
            val obj = JSONObject()
            acc.toMap().forEach { (k, v) -> obj.put(k, v) }
            arr.put(obj)
        }
        getPrefs(context).edit().putString(KEY_BANK_ACCOUNTS_JSON, arr.toString()).commit()

        val bogAcc = sorted.firstOrNull { it.bankCode.equals("bog", ignoreCase = true) }
        val tbcAcc = sorted.firstOrNull { it.bankCode.equals("tbc", ignoreCase = true) }
        AdminStorage.saveBankRequisites(
            context,
            AdminStorage.BankRequisites(
                bogIban = bogAcc?.iban.orEmpty(),
                bogRecipient = bogAcc?.recipientName.orEmpty(),
                tbcIban = tbcAcc?.iban.orEmpty(),
                tbcRecipient = tbcAcc?.recipientName.orEmpty()
            )
        )
        stateVersion.intValue++
    }

    fun saveBankAccounts(context: Context, accounts: List<BankAccountConfig>) {
        saveBankAccountsLocal(context, accounts)
    }

    fun syncFromCloudBankAccounts(context: Context, cloudBanks: List<BankAccountConfig>) {
        if (cloudBanks.isNotEmpty()) {
            saveBankAccountsLocal(context, cloudBanks)
        }
    }

    fun syncFromLegacyRequisites(context: Context, requisites: AdminStorage.BankRequisites) {
        if (!isValidIban(requisites.bogIban) && !isValidIban(requisites.tbcIban)) return
        AdminStorage.saveBankRequisites(context, requisites)
        val raw = getPrefs(context).getString(KEY_BANK_ACCOUNTS_JSON, null)
        if (raw.isNullOrBlank() || raw == "[]") {
            val hydrated = mutableListOf<BankAccountConfig>()
            if (isValidIban(requisites.bogIban) && requisites.bogRecipient.isNotBlank()) {
                hydrated.add(
                    BankAccountConfig(
                        id = "bank_bog_primary",
                        bankCode = "bog",
                        bankName = "საქართველოს ბანკი",
                        recipientName = requisites.bogRecipient.trim(),
                        iban = normalizeIban(requisites.bogIban),
                        description = "საქართველოს ბანკის (BOG) ლარის ანგარიში",
                        paymentInstructions = BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA,
                        isEnabled = true,
                        displayOrder = 1
                    )
                )
            }
            if (isValidIban(requisites.tbcIban) && requisites.tbcRecipient.isNotBlank()) {
                hydrated.add(
                    BankAccountConfig(
                        id = "bank_tbc_primary",
                        bankCode = "tbc",
                        bankName = "თიბისი ბანკი",
                        recipientName = requisites.tbcRecipient.trim(),
                        iban = normalizeIban(requisites.tbcIban),
                        description = "თიბისი ბანკის (TBC) ლარის ანგარიში",
                        paymentInstructions = BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA,
                        isEnabled = true,
                        displayOrder = 2
                    )
                )
            }
            if (hydrated.isNotEmpty()) {
                saveBankAccountsLocal(context, hydrated)
            }
        } else {
            stateVersion.intValue++
        }
    }

    fun upsertBankAccountAsAdmin(
        context: Context,
        account: BankAccountConfig
    ): Result<BankAccountConfig> {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return Result.failure(
                SecurityException("PERMISSION_DENIED: საბანკო ანგარიშების მართვა შეუძლია მხოლოდ ავტორიზებულ ადმინისტრატორს (beqa1994@gmail.com)")
            )
        }
        val cleanBankName = account.bankName.trim()
        val cleanRecipient = account.recipientName.trim()
        val cleanIban = normalizeIban(account.iban)
        if (cleanBankName.isBlank()) {
            return Result.failure(IllegalArgumentException("გთხოვთ მიუთითოთ ბანკის დასახელება"))
        }
        if (cleanRecipient.isBlank()) {
            return Result.failure(IllegalArgumentException("გთხოვთ მიუთითოთ მიმღების სახელი"))
        }
        if (!isValidIban(cleanIban)) {
            return Result.failure(
                IllegalArgumentException("არასწორი IBAN ფორმატი. ქართული IBAN უნდა იწყებოდეს GE-ით და შეიცავდეს 22 სიმბოლოს (მაგ: GE00BG0000000000000000)")
            )
        }

        val normalized = account.copy(
            bankName = cleanBankName,
            recipientName = cleanRecipient,
            iban = cleanIban,
            paymentInstructions = account.paymentInstructions.trim().ifBlank { BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA },
            updatedAtMillis = System.currentTimeMillis(),
            updatedBy = AuthAndOrderManager.SOLE_ADMIN_EMAIL
        )

        val current = getAllBankAccounts(context).toMutableList()
        val idx = current.indexOfFirst { it.id == normalized.id }
        if (idx >= 0) {
            current[idx] = normalized
        } else {
            current.add(normalized)
        }
        saveBankAccountsLocal(context, current)

        recordAuditEventLocal(
            context = context,
            event = PaymentAuditEvent(
                id = "audit_${System.currentTimeMillis()}",
                orderId = normalized.id,
                orderNumber = normalized.bankName,
                eventType = "BANK_ACCOUNT_UPDATED",
                previousStatus = if (idx >= 0) "configured" else "new",
                newStatus = if (normalized.isEnabled) "enabled" else "disabled",
                amount = 0.0,
                actorUid = AuthAndOrderManager.getCurrentUser(context)?.id ?: "sole_admin",
                actorEmail = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                actorRole = "sole_admin",
                note = "${normalized.bankName} (${normalized.iban}) — ${if (normalized.isEnabled) "აქტიური" else "გათიშული"}"
            )
        )
        return Result.success(normalized)
    }

    fun deleteBankAccountAsAdmin(context: Context, accountId: String): Result<Unit> {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return Result.failure(
                SecurityException("PERMISSION_DENIED: საბანკო ანგარიშის წაშლა შეუძლია მხოლოდ ადმინისტრატორს")
            )
        }
        val current = getAllBankAccounts(context).filterNot { it.id == accountId }
        saveBankAccountsLocal(context, current)
        return Result.success(Unit)
    }

    fun getAuditEvents(context: Context, filterOrderId: String? = null): List<PaymentAuditEvent> {
        val raw = getPrefs(context).getString(KEY_AUDIT_LOGS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PaymentAuditEvent>()
            for (i in 0 until arr.length()) {
                val ev = PaymentAuditEvent.fromJson(arr.getJSONObject(i))
                if (filterOrderId.isNullOrBlank() || ev.orderId == filterOrderId) {
                    list.add(ev)
                }
            }
            list.sortedByDescending { it.timestampMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getPaymentAuditLogs(context: Context, filterOrderId: String? = null): List<PaymentAuditEvent> =
        getAuditEvents(context, filterOrderId)

    fun recordAuditEventLocal(context: Context, event: PaymentAuditEvent) {
        val existing = getAuditEvents(context).toMutableList()
        existing.removeAll { it.id == event.id }
        existing.add(0, event)
        val trimmed = existing.take(300)
        val arr = JSONArray()
        trimmed.forEach { arr.put(it.toJson()) }
        getPrefs(context).edit().putString(KEY_AUDIT_LOGS_JSON, arr.toString()).commit()
        stateVersion.intValue++
    }

    fun mergeCloudAuditEvents(context: Context, cloudEvents: List<PaymentAuditEvent>): List<PaymentAuditEvent> {
        val local = getAuditEvents(context)
        val byId = LinkedHashMap<String, PaymentAuditEvent>()
        (cloudEvents + local)
            .sortedByDescending { it.timestampMillis }
            .forEach { ev ->
                if (!byId.containsKey(ev.id)) {
                    byId[ev.id] = ev
                }
            }
        val merged = byId.values.take(300)
        val arr = JSONArray()
        merged.forEach { arr.put(it.toJson()) }
        getPrefs(context).edit().putString(KEY_AUDIT_LOGS_JSON, arr.toString()).commit()
        return merged
    }

    /**
     * Enforces order ownership check before returning bank transfer instructions.
     * Customers can only view instructions for their own valid bank_transfer order;
     * the sole administrator can inspect any order.
     */
    fun canUserAccessOrderPaymentInstructions(
        context: Context,
        order: OrderRecord
    ): Boolean {
        if (order.paymentMethodCode != PaymentMethodType.BANK_TRANSFER.code) {
            return false
        }
        if (AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return true
        }
        val currentUser = AuthAndOrderManager.getCurrentUser(context)
        val currentUid = runCatching {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        }.getOrNull() ?: currentUser?.id.orEmpty()

        // If the order has a userId recorded, it must match the signed-in user
        if (order.userId.isNotBlank() && currentUid.isNotBlank()) {
            return order.userId == currentUid
        }
        // Or the order must be in the current user's local orders list
        return AuthAndOrderManager.getOrders(context).any { it.id == order.id }
    }
}

// ==============================================================================
// 2B. SMART BANK TRANSFER HANDOFF HELPER & OFFICIAL BANK LOGOS (BOG / TBC)
// ==============================================================================

object BankTransferHandoffHelper {
    const val BOG_PACKAGE_NAME = "ge.bog.mobilebank"
    const val TBC_PACKAGE_NAME = "com.icomvision.bsc.tbc"

    enum class BankType { BOG, TBC, UNKNOWN }

    sealed class BankLaunchResult {
        data class Launched(
            val packageName: String,
            val copiedIban: String,
            val bankType: BankType
        ) : BankLaunchResult()

        data class AppNotInstalled(
            val packageName: String,
            val copiedIban: String,
            val bankType: BankType
        ) : BankLaunchResult()

        data class InvalidBankOrIban(val reasonKa: String) : BankLaunchResult()

        data class LaunchFailed(
            val packageName: String,
            val copiedIban: String,
            val reasonKa: String
        ) : BankLaunchResult()
    }

    fun resolveBankType(bankCode: String, bankName: String = ""): BankType {
        val code = bankCode.trim().lowercase(Locale.ROOT)
        val name = bankName.trim().lowercase(Locale.ROOT)
        return when {
            code == "bog" || name.contains("საქართველო") || name.contains("bank of georgia") || name.contains("bog") -> BankType.BOG
            code == "tbc" || name.contains("თიბისი") || name.contains("tbc") -> BankType.TBC
            else -> BankType.UNKNOWN
        }
    }

    fun resolveBankPackageName(bankCode: String, bankName: String = ""): String? {
        return when (resolveBankType(bankCode, bankName)) {
            BankType.BOG -> BOG_PACKAGE_NAME
            BankType.TBC -> TBC_PACKAGE_NAME
            BankType.UNKNOWN -> null
        }
    }

    fun resolveBankLogoResId(bankCode: String, bankName: String = ""): Int? {
        return when (resolveBankType(bankCode, bankName)) {
            BankType.BOG -> R.drawable.ic_bank_bog
            BankType.TBC -> R.drawable.ic_bank_tbc
            BankType.UNKNOWN -> null
        }
    }

    fun copyIbanToClipboardSilently(
        context: Context,
        iban: String,
        label: String = "IBAN"
    ): String? {
        val cleanIban = BstorePaymentManager.normalizeIban(iban)
        if (!BstorePaymentManager.isValidIban(cleanIban)) return null
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return null
            clipboard.setPrimaryClip(ClipData.newPlainText(label, cleanIban))
            cleanIban
        } catch (_: Exception) {
            null
        }
    }

    fun launchMobileBankForTransfer(
        context: Context,
        bankCode: String,
        bankName: String,
        iban: String
    ): BankLaunchResult {
        val bankType = resolveBankType(bankCode, bankName)
        val packageName = resolveBankPackageName(bankCode, bankName)
            ?: return BankLaunchResult.InvalidBankOrIban(
                "არჩეული ბანკის მობაილბანკის აპლიკაცია მხარდაჭერილი არ არის. გამოიყენეთ დაკოპირებული IBAN."
            )

        val copiedIban = copyIbanToClipboardSilently(context, iban, "IBAN ანგარიშის ნომერი")
            ?: return BankLaunchResult.InvalidBankOrIban(
                "ანგარიშის ნომერი (IBAN) არასწორია ან ცარიელია."
            )

        val launchIntent = runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName)
        }.getOrNull() ?: return BankLaunchResult.AppNotInstalled(
            packageName = packageName,
            copiedIban = copiedIban,
            bankType = bankType
        )

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            BankLaunchResult.Launched(
                packageName = packageName,
                copiedIban = copiedIban,
                bankType = bankType
            )
        } catch (e: Exception) {
            BankLaunchResult.LaunchFailed(
                packageName = packageName,
                copiedIban = copiedIban,
                reasonKa = e.localizedMessage ?: "მობაილბანკის გახსნა ვერ მოხერხდა."
            )
        }
    }

    fun openBankInPlayStore(context: Context, packageName: String): Boolean {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isBlank()) return false
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$cleanPkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$cleanPkg")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(marketIntent)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(webIntent)
                true
            } catch (_: Exception) {
                false
            }
        } catch (_: Exception) {
            try {
                context.startActivity(webIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}

@Composable
fun BankBrandLogo(
    bankCode: String,
    bankName: String = "",
    size: Dp = 36.dp,
    circular: Boolean = false,
    modifier: Modifier = Modifier
) {
    val logoResId = BankTransferHandoffHelper.resolveBankLogoResId(bankCode, bankName)
    val bankType = BankTransferHandoffHelper.resolveBankType(bankCode, bankName)
    val shape = if (circular) CircleShape else RoundedCornerShape(if (size >= 30.dp) 9.dp else 6.dp)
    if (logoResId != null) {
        val desc = when (bankType) {
            BankTransferHandoffHelper.BankType.BOG -> "საქართველოს ბანკის ლოგო"
            BankTransferHandoffHelper.BankType.TBC -> "თიბისი ბანკის ლოგო"
            BankTransferHandoffHelper.BankType.UNKNOWN -> "ბანკის ლოგო"
        }
        Image(
            painter = painterResource(id = logoResId),
            contentDescription = desc,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(size)
                .clip(shape)
                .testTag("bank_logo_${bankType.name.lowercase()}")
        )
    } else {
        Surface(
            shape = shape,
            color = Color(0xFF232A38),
            modifier = modifier.size(size)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = bankName.ifBlank { "ბანკის ლოგო" },
                    tint = Color(0xFFFF7A00),
                    modifier = Modifier.size(size * 0.58f)
                )
            }
        }
    }
}

// ==============================================================================
// 3. CUSTOMER CHECKOUT SCREEN (Moved to BstorePaymentCheckoutScreen.kt)
// ==============================================================================


// ==============================================================================
// 4. DEDICATED PAYMENT INSTRUCTIONS SCREEN (ONLY FOR AUTHENTICATED OWNER / ADMIN)
// ==============================================================================

@Composable
fun OrderPaymentInstructionsDialog(
    order: OrderRecord,
    onDismiss: () -> Unit,
    onOrderUpdated: (OrderRecord) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val paymentStateVersion = BstorePaymentManager.stateVersion.intValue

    var currentOrder by remember(
        order.id,
        order.paymentStatus,
        order.bankAccountId,
        order.ibanSnapshot,
        order.userConfirmedPaymentAtMillis
    ) {
        mutableStateOf(order)
    }

    val hasAccess = remember(currentOrder.id, currentOrder.userId, currentOrder.paymentMethodCode) {
        BstorePaymentManager.canUserAccessOrderPaymentInstructions(context, currentOrder)
    }

    val enabledBanks = remember(paymentStateVersion) {
        BstorePaymentManager.getEnabledBankAccounts(context)
    }

    // Refresh real enabled banks from Firebase when opened
    LaunchedEffect(currentOrder.id) {
        runCatching {
            val cloudBanks = FirestoreRepository(context).fetchPaymentBankAccountsFromCloud().getOrNull()
            if (!cloudBanks.isNullOrEmpty()) {
                BstorePaymentManager.syncFromCloudBankAccounts(context, cloudBanks)
            } else {
                val legacyReq = FirestoreRepository(context).fetchBankRequisitesFromCloud().getOrNull()
                if (legacyReq != null) {
                    BstorePaymentManager.syncFromLegacyRequisites(context, legacyReq)
                }
            }
        }
    }

    val paymentStatus = PaymentStatus.fromCode(
        currentOrder.paymentStatus,
        PaymentMethodType.fromCode(currentOrder.paymentMethodCode, currentOrder.paymentMethod)
    )
    val isBankPending = paymentStatus == PaymentStatus.PENDING && currentOrder.status != "გაუქმებული"
    val bankColor = when (currentOrder.bankCode.lowercase()) {
        "bog" -> Color(0xFFFF5E00)
        "tbc" -> Color(0xFF00A3E0)
        else -> Color(0xFFFF6B00)
    }
    val paymentRef = currentOrder.paymentReference.ifBlank { currentOrder.orderNumber }
    val instructionsText = currentOrder.paymentInstructionsSnapshot.ifBlank {
        BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA
    }

    var amountCopied by remember { mutableStateOf(false) }
    var recipientCopied by remember { mutableStateOf(false) }
    var ibanCopied by remember(currentOrder.ibanSnapshot) { mutableStateOf(false) }
    var refCopied by remember { mutableStateOf(false) }
    var isSwitchingBank by remember { mutableStateOf(false) }
    var isSubmittingTransfer by remember { mutableStateOf(false) }
    var lastAutoCopiedKey by remember { mutableStateOf("") }
    var autoCopiedStatusMessage by remember { mutableStateOf<String?>(null) }
    var bankHandoffInstructionBanner by remember { mutableStateOf<String?>(null) }
    var bankHandoffErrorMessage by remember { mutableStateOf<String?>(null) }
    var missingBankAppDialogPackage by remember { mutableStateOf<String?>(null) }
    var transferSubmittedMessage by remember {
        mutableStateOf(
            if (paymentStatus == PaymentStatus.VERIFICATION_PENDING) {
                "თქვენი გადარიცხვის შესახებ ინფორმაცია მიღებულია. თანხის ჩარიცხვას ადმინისტრატორი გადაამოწმებს."
            } else null
        )
    }

    LaunchedEffect(
        hasAccess,
        isBankPending,
        currentOrder.id,
        currentOrder.bankAccountId,
        currentOrder.bankCode,
        currentOrder.ibanSnapshot,
        enabledBanks
    ) {
        if (!hasAccess || !isBankPending) return@LaunchedEffect
        val allConfiguredBanks = BstorePaymentManager.getAllBankAccounts(context)
        val matchingBank = allConfiguredBanks.find {
            (currentOrder.bankAccountId.isNotBlank() && it.id == currentOrder.bankAccountId) ||
                it.bankCode.equals(currentOrder.bankCode, ignoreCase = true)
        }
        val isBankExplicitlyDisabled = matchingBank != null && !matchingBank.isEnabled
        if (isBankExplicitlyDisabled) return@LaunchedEffect

        val normalizedIban = BstorePaymentManager.normalizeIban(currentOrder.ibanSnapshot)
        if (!BstorePaymentManager.isValidIban(normalizedIban)) return@LaunchedEffect

        val selectionKey = "${currentOrder.id}:${currentOrder.bankCode.lowercase(Locale.ROOT)}:$normalizedIban"
        if (lastAutoCopiedKey != selectionKey) {
            val copied = BankTransferHandoffHelper.copyIbanToClipboardSilently(
                context = context,
                iban = normalizedIban,
                label = "IBAN ანგარიშის ნომერი"
            )
            if (copied != null) {
                lastAutoCopiedKey = selectionKey
                ibanCopied = true
                autoCopiedStatusMessage = "ანგარიშის ნომერი დაკოპირდა"
                bankHandoffErrorMessage = null
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSubmittingTransfer) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF12151C),
            border = BorderStroke(1.5.dp, bankColor.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("dedicated_payment_instructions_dialog")
        ) {
            if (!hasAccess) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "წვდომა შეზღუდულია",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "საბანკო გადარიცხვის რეკვიზიტები ხელმისაწვდომია მხოლოდ შეკვეთის მფლობელისთვის აქტიური საბანკო გადარიცხვის შეკვეთაზე.",
                        color = Color(0xFFA0A5B0),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("close_payment_instructions_button")
                    ) {
                        Text("დახურვა", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // ==============================================================
                    // 1. STICKY HEADER
                    // ==============================================================
                    Surface(
                        color = Color(0xFF161A22),
                        border = BorderStroke(1.dp, Color(0xFF222834)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                BankBrandLogo(
                                    bankCode = currentOrder.bankCode,
                                    bankName = currentOrder.bankName,
                                    size = 38.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "საბანკო გადარიცხვის ინსტრუქცია",
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "შეკვეთა ${currentOrder.orderNumber}",
                                        color = Color(0xFFFF7A00),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(
                                onClick = { if (!isSubmittingTransfer) onDismiss() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = Color(0xFFA0A5B0))
                            }
                        }
                    }

                    // ==============================================================
                    // 2. SCROLLABLE CONTENT
                    // ==============================================================
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Inline Bank Switcher (real enabled banks from Firebase)
                        if (enabledBanks.isNotEmpty() && isBankPending) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (enabledBanks.size > 1) {
                                        "აირჩიეთ მიმღები ბანკი (ანგარიშის შესაცვლელად):"
                                    } else {
                                        "აქტიური მიმღები ბანკი:"
                                    },
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    enabledBanks.forEach { bankOpt ->
                                        val isBankSelected = currentOrder.bankAccountId == bankOpt.id ||
                                            (currentOrder.bankAccountId.isBlank() && currentOrder.bankCode.equals(bankOpt.bankCode, true))
                                        val optColor = bankOpt.brandColor()
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isBankSelected) optColor.copy(alpha = 0.2f) else Color(0xFF171C26),
                                            border = BorderStroke(
                                                1.5.dp,
                                                if (isBankSelected) optColor else Color(0xFF28303E)
                                            ),
                                            modifier = Modifier
                                                .clickable(enabled = !isSwitchingBank && !isSubmittingTransfer) {
                                                    if (!isBankSelected) {
                                                        val optimistic = AuthAndOrderManager.switchOrderBankLocal(
                                                            context = context,
                                                            orderId = currentOrder.id,
                                                            bank = bankOpt
                                                        ) ?: return@clickable
                                                        bankHandoffInstructionBanner = null
                                                        bankHandoffErrorMessage = null
                                                        currentOrder = optimistic
                                                        onOrderUpdated(optimistic)
                                                        val fbUser = runCatching {
                                                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                                        }.getOrNull()
                                                        if (fbUser != null) {
                                                            isSwitchingBank = true
                                                            coroutineScope.launch {
                                                                val res = FirestoreRepository(context).switchOrderBankByCustomer(optimistic, bankOpt)
                                                                isSwitchingBank = false
                                                                if (res.isSuccess) {
                                                                    val cloudOrd = res.getOrThrow()
                                                                    currentOrder = cloudOrd
                                                                    onOrderUpdated(cloudOrd)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                .testTag("switch_bank_tab_${bankOpt.bankCode}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                BankBrandLogo(
                                                    bankCode = bankOpt.bankCode,
                                                    bankName = bankOpt.bankName,
                                                    size = 22.dp
                                                )
                                                Text(
                                                    text = bankOpt.bankName,
                                                    color = Color.White,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isBankSelected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Status & Amount Banner (with Copy Button)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF171C26),
                            border = BorderStroke(1.dp, Color(paymentStatus.badgeColorHex).copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("გადასახდელი თანხა (GEL):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = currentOrder.totalAmount,
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.testTag("payment_instructions_amount")
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (amountCopied) Color(0xFF16A34A) else Color(0xFF232A38),
                                                modifier = Modifier
                                                    .clickable {
                                                        val numericText = currentOrder.totalAmount
                                                            .filter { it.isDigit() || it == '.' }
                                                            .ifBlank { currentOrder.totalAmount }
                                                        copyToClipboard(context, "გადასახდელი თანხა", numericText)
                                                        amountCopied = true
                                                    }
                                                    .testTag("copy_amount_button")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (amountCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                        contentDescription = "თანხის კოპირება",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = if (amountCopied) "დაკოპირდა!" else "კოპირება",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(paymentStatus.badgeColorHex).copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, Color(paymentStatus.badgeColorHex).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = paymentStatus.customerLabelKa,
                                            color = Color(paymentStatus.badgeColorHex),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier
                                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                                .testTag("payment_instructions_status_badge")
                                        )
                                    }
                                }

                                if (transferSubmittedMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(9.dp),
                                        color = Color(0xFF1E3A8A).copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, Color(0xFF60A5FA).copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("transfer_verification_pending_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.HourglassTop,
                                                contentDescription = null,
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = transferSubmittedMessage!!,
                                                color = Color(0xFFDBEAFE),
                                                fontSize = 11.5.sp,
                                                lineHeight = 15.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Selected Bank & Recipient (with Recipient Copy Button)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF161922),
                            border = BorderStroke(1.dp, Color(0xFF262D3B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("მიმღები ბანკი:", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                                    Text(
                                        text = currentOrder.bankName.ifBlank { "საბანკო გადარიცხვა" },
                                        color = bankColor,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("payment_instructions_bank_name")
                                    )
                                }
                                HorizontalDivider(color = Color(0xFF232936))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("მიმღების სახელი:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        Text(
                                            text = currentOrder.recipientNameSnapshot.ifBlank { "მითითებული არ არის" },
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("payment_instructions_recipient")
                                        )
                                    }
                                    if (currentOrder.recipientNameSnapshot.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (recipientCopied) Color(0xFF16A34A) else Color(0xFF232A38),
                                            modifier = Modifier
                                                .clickable {
                                                    copyToClipboard(context, "მიმღების სახელი", currentOrder.recipientNameSnapshot)
                                                    recipientCopied = true
                                                }
                                                .testTag("copy_recipient_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (recipientCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                    contentDescription = "მიმღების კოპირება",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (recipientCopied) "დაკოპირდა!" else "კოპირება",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // IBAN Field with Copy Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0E1117),
                            border = BorderStroke(1.dp, bankColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ანგარიშის ნომერი (IBAN):",
                                        color = bankColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (autoCopiedStatusMessage != null || ibanCopied) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF16A34A).copy(alpha = 0.22f),
                                            border = BorderStroke(1.dp, Color(0xFF4ADE80).copy(alpha = 0.45f)),
                                            modifier = Modifier.testTag("iban_auto_copied_badge")
                                        ) {
                                            Text(
                                                text = "✓ ${autoCopiedStatusMessage ?: "ანგარიშის ნომერი დაკოპირდა"}",
                                                color = Color(0xFF4ADE80),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = currentOrder.ibanSnapshot.ifBlank { "საბანკო რეკვიზიტი მიუწვდომელია" },
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.6.sp,
                                    modifier = Modifier.testTag("payment_instructions_iban_text")
                                )
                                Button(
                                    onClick = {
                                        if (currentOrder.ibanSnapshot.isNotBlank()) {
                                            copyToClipboard(context, "IBAN ანგარიშის ნომერი", currentOrder.ibanSnapshot)
                                            ibanCopied = true
                                            autoCopiedStatusMessage = "ანგარიშის ნომერი დაკოპირდა"
                                        }
                                    },
                                    enabled = currentOrder.ibanSnapshot.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (ibanCopied) Color(0xFF16A34A) else bankColor.copy(alpha = 0.22f)
                                    ),
                                    shape = RoundedCornerShape(9.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("copy_iban_button")
                                ) {
                                    Icon(
                                        imageVector = if (ibanCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (ibanCopied) "დაკოპირდა!" else "IBAN ანგარიშის კოპირება",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Payment Reference / Order Number with Copy Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0E1117),
                            border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "გადარიცხვის დანიშნულება (აუცილებლად მიუთითეთ):",
                                    color = Color(0xFFFF9E40),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = paymentRef,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("payment_instructions_reference_text")
                                )
                                Button(
                                    onClick = {
                                        copyToClipboard(context, "გადახდის დანიშნულება", paymentRef)
                                        refCopied = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (refCopied) Color(0xFF16A34A) else Color(0xFFFF6B00).copy(alpha = 0.22f)
                                    ),
                                    shape = RoundedCornerShape(9.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .testTag("copy_payment_reference_button")
                                ) {
                                    Icon(
                                        imageVector = if (refCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (refCopied) "დაკოპირდა!" else "დანიშნულების კოპირება ($paymentRef)",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Direct Mobile Banking Handoff CTA (Placed directly below payment info)
                        if (PaymentMethodType.fromCode(currentOrder.paymentMethodCode, currentOrder.paymentMethod) == PaymentMethodType.BANK_TRANSFER &&
                            isBankPending
                        ) {
                            val canAttemptMobileBankLaunch = currentOrder.ibanSnapshot.isNotBlank() &&
                                BankTransferHandoffHelper.resolveBankPackageName(currentOrder.bankCode, currentOrder.bankName) != null

                            if (canAttemptMobileBankLaunch) {
                                Button(
                                    onClick = {
                                        bankHandoffErrorMessage = null
                                        when (
                                            val launchResult = BankTransferHandoffHelper.launchMobileBankForTransfer(
                                                context = context,
                                                bankCode = currentOrder.bankCode,
                                                bankName = currentOrder.bankName,
                                                iban = currentOrder.ibanSnapshot
                                            )
                                        ) {
                                            is BankTransferHandoffHelper.BankLaunchResult.Launched -> {
                                                ibanCopied = true
                                                autoCopiedStatusMessage = "ანგარიშის ნომერი დაკოპირდა"
                                                bankHandoffInstructionBanner =
                                                    "ანგარიშის ნომერი დაკოპირებულია. მობაილბანკში გადარიცხვისას ჩასვით მიმღების ანგარიშის ველში."
                                            }
                                            is BankTransferHandoffHelper.BankLaunchResult.AppNotInstalled -> {
                                                ibanCopied = true
                                                autoCopiedStatusMessage = "ანგარიშის ნომერი დაკოპირდა"
                                                bankHandoffInstructionBanner =
                                                    "ანგარიშის ნომერი დაკოპირებულია. მობაილბანკში გადარიცხვისას ჩასვით მიმღების ანგარიშის ველში."
                                                missingBankAppDialogPackage = launchResult.packageName
                                            }
                                            is BankTransferHandoffHelper.BankLaunchResult.InvalidBankOrIban -> {
                                                bankHandoffErrorMessage = launchResult.reasonKa
                                            }
                                            is BankTransferHandoffHelper.BankLaunchResult.LaunchFailed -> {
                                                ibanCopied = true
                                                bankHandoffErrorMessage = launchResult.reasonKa
                                            }
                                        }
                                    },
                                    enabled = !isSwitchingBank && !isSubmittingTransfer && currentOrder.ibanSnapshot.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = bankColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("open_mobile_bank_button")
                                ) {
                                    BankBrandLogo(
                                        bankCode = currentOrder.bankCode,
                                        bankName = currentOrder.bankName,
                                        size = 24.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "გადარიცხვაზე გადასვლა",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (bankHandoffInstructionBanner != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0E291A),
                                    border = BorderStroke(1.dp, Color(0xFF4ADE80).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bank_handoff_instruction_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4ADE80),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = bankHandoffInstructionBanner!!,
                                            color = Color(0xFFDCFCE7),
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            if (bankHandoffErrorMessage != null) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF2B1618),
                                    border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bank_handoff_error_banner")
                                ) {
                                    Text(
                                        text = "⚠️ ${bankHandoffErrorMessage!!}",
                                        color = Color(0xFFFECACA),
                                        fontSize = 11.5.sp,
                                        lineHeight = 15.5.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        // Step-by-step Instructions
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF161922),
                            border = BorderStroke(1.dp, Color(0xFF262D3B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFFF7A00),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "მოკლე ინსტრუქცია:",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = instructionsText,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                                Text(
                                    text = "• სხვა ბანკიდან გადმორიცხვაც ჩვეულებრივ სრულდება მითითებულ IBAN ანგარიშზე. თანხის გადარიცხვის შემდეგ დააჭირეთ ღილაკს „✓ თანხა გადავრიცხე“.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // ==============================================================
                    // 3. STICKY FOOTER (Safe Area / Gesture Navigation Compatible)
                    // ==============================================================
                    Surface(
                        color = Color(0xFF161A22),
                        border = BorderStroke(1.dp, Color(0xFF222834)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (PaymentMethodType.fromCode(currentOrder.paymentMethodCode, currentOrder.paymentMethod) == PaymentMethodType.BANK_TRANSFER &&
                                isBankPending
                            ) {
                                Button(
                                    onClick = {
                                        if (isSubmittingTransfer) return@Button
                                        isSubmittingTransfer = true
                                        val selectedBankObj = enabledBanks.find {
                                            it.id == currentOrder.bankAccountId || it.bankCode.equals(currentOrder.bankCode, true)
                                        }
                                        val fbUser = runCatching {
                                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                        }.getOrNull()
                                        if (fbUser != null) {
                                            coroutineScope.launch {
                                                val res = FirestoreRepository(context).confirmTransferSentByCustomer(
                                                    order = currentOrder,
                                                    selectedBank = selectedBankObj
                                                )
                                                isSubmittingTransfer = false
                                                if (res.isSuccess) {
                                                    val updatedOrd = res.getOrThrow()
                                                    currentOrder = updatedOrd
                                                    onOrderUpdated(updatedOrd)
                                                    transferSubmittedMessage =
                                                        "თქვენი გადარიცხვის შესახებ ინფორმაცია მიღებულია. თანხის ჩარიცხვას ადმინისტრატორი გადაამოწმებს."
                                                    Toast.makeText(
                                                        context,
                                                        transferSubmittedMessage,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        res.exceptionOrNull()?.localizedMessage ?: "შეცდომა გაგზავნისას",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            val localRes = AuthAndOrderManager.markOrderTransferSentByUserLocal(
                                                context = context,
                                                orderId = currentOrder.id,
                                                bankOverride = selectedBankObj
                                            )
                                            isSubmittingTransfer = false
                                            if (localRes.isSuccess) {
                                                val updatedOrd = localRes.getOrThrow()
                                                currentOrder = updatedOrd
                                                onOrderUpdated(updatedOrd)
                                                transferSubmittedMessage =
                                                    "თქვენი გადარიცხვის შესახებ ინფორმაცია მიღებულია. თანხის ჩარიცხვას ადმინისტრატორი გადაამოწმებს."
                                                Toast.makeText(
                                                    context,
                                                    transferSubmittedMessage,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    localRes.exceptionOrNull()?.localizedMessage ?: "შეცდომა გაგზავნისას",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    },
                                    enabled = !isSubmittingTransfer && currentOrder.ibanSnapshot.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("confirm_transfer_sent_button")
                                ) {
                                    if (isSubmittingTransfer) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("იგზავნება...", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text(
                                            text = "✓ თანხა გადავრიცხე",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { if (!isSubmittingTransfer) onDismiss() },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF2E3440)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("close_payment_instructions_button")
                            ) {
                                Text("დახურვა", color = Color(0xFFA0A5B0), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (missingBankAppDialogPackage != null) {
        val targetPackage = missingBankAppDialogPackage!!
        Dialog(onDismissRequest = { missingBankAppDialogPackage = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141821),
                border = BorderStroke(1.dp, bankColor.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bank_app_not_installed_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BankBrandLogo(
                        bankCode = currentOrder.bankCode,
                        bankName = currentOrder.bankName,
                        size = 48.dp
                    )
                    Text(
                        text = "მობაილბანკი არ არის დაყენებული",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${currentOrder.bankName.ifBlank { "ბანკის" }} მობაილბანკის აპლიკაცია ამ მოწყობილობაზე ვერ მოიძებნა. ანგარიშის ნომერი (IBAN) უკვე დაკოპირებულია ბუფერში — შეგიძლიათ ჩამოტვირთოთ აპლიკაცია Google Play-დან ან გამოიყენოთ დაკოპირებული IBAN.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            val opened = BankTransferHandoffHelper.openBankInPlayStore(context, targetPackage)
                            missingBankAppDialogPackage = null
                            if (!opened) {
                                Toast.makeText(
                                    context,
                                    "Google Play Store-ის გახსნა ვერ მოხერხდა",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = bankColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_bank_play_store_button")
                    ) {
                        Text(
                            text = "Google Play-ში გახსნა",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { missingBankAppDialogPackage = null },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3440)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dismiss_bank_app_not_installed_button")
                    ) {
                        Text(
                            text = "დახურვა",
                            color = Color(0xFFA0A5B0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 5. POLICIES & CUSTOMER INFORMATION SECTION (5 GEORGIAN PAGES)
// ==============================================================================

@Composable
fun CustomerPaymentAndPoliciesCard(
    onOpenPolicyTopic: (LegalPolicyTopic) -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val surfaceBg = if (isDark) Color(0xFF16191E) else Color.White
    val nestedBg = if (isDark) Color(0xFF101318) else Color(0xFFF8FAFC)
    val borderCol = if (isDark) Color(0xFF242830) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceBg,
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_payment_and_policies_card")
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "გადახდის პირობები და სამართლებრივი ინფორმაცია",
                        color = textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = nestedBg,
                border = BorderStroke(0.8.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "💳 ხელმისაწვდომი ანგარიშსწორების მეთოდები:",
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• საბანკო გადარიცხვა (საქართველოს ბანკი / თიბისი ბანკი) — საბანკო რეკვიზიტები და დანიშნულების კოდი გენერირდება შეკვეთის გაფორმებისას.",
                        color = textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = "• ნაღდი ანგარიშსწორება — გადაიხადეთ ნივთის მიღებისას (საკურიერო ჩაბარებისას ან ფილიალიდან გატანისას).",
                        color = textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LegalPolicyTopic.entries.forEach { topic ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = nestedBg,
                        border = BorderStroke(0.8.dp, borderCol),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPolicyTopic(topic) }
                            .testTag("policy_topic_item_${topic.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topic.titleKa,
                                    color = textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = topic.shortSubtitleKa,
                                    color = textSecondary,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFFF6B00),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BstoreLegalPoliciesDialog(
    initialTopic: LegalPolicyTopic = LegalPolicyTopic.DELIVERY_POLICY,
    onDismiss: () -> Unit
) {
    var selectedTopic by remember(initialTopic) { mutableStateOf(initialTopic) }
    val contactConfig = ContactAndSocialManager.currentConfig
    val primaryPhone = contactConfig.phoneNumbers.firstOrNull { it.isEnabled }?.number
        ?: AppBrandConfig.SUPPORT_PHONE
    val serviceAddress = contactConfig.serviceAddress.ifBlank { "ქ. ახალციხე, კოსტავას N10" }
    val workingHours = contactConfig.workingHours.ifBlank { AppBrandConfig.WORKING_HOURS_KA }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF12151C),
            border = BorderStroke(1.dp, Color(0xFF28303E)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("bstore_legal_policies_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161A22))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Policy,
                            contentDescription = null,
                            tint = Color(0xFFFF6B00),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BSTORE • წესები და პოლიტიკა",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = Color(0xFFA0A5B0))
                    }
                }

                // Topic Selector Tabs
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141820))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(LegalPolicyTopic.entries) { topic ->
                        val isSelected = selectedTopic == topic
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFFF6B00) else Color(0xFF1C212C),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFFF6B00) else Color(0xFF2B3242)
                            ),
                            modifier = Modifier
                                .clickable { selectedTopic = topic }
                                .testTag("legal_policy_tab_${topic.id}")
                        ) {
                            Text(
                                text = topic.titleKa,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Content Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = selectedTopic.titleKa,
                        color = Color(0xFFFF7A00),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    when (selectedTopic) {
                        LegalPolicyTopic.DELIVERY_POLICY -> {
                            PolicySectionCard(
                                title = "1. უფასო გატანა BSTORE-ის ფილიალიდან",
                                bullets = listOf(
                                    "მისამართი: ქ. ახალციხე, კოსტავას N10.",
                                    "ფილიალიდან გატანა სრულიად უფასოა (0 ₾).",
                                    "შეკვეთის გატანა შესაძლებელია მას შემდეგ, რაც მიიღებთ შეტყობინებას შეკვეთის მზადყოფნის შესახებ."
                                )
                            )
                            PolicySectionCard(
                                title = "2. საკურიერო მიწოდება საქართველოს მასშტაბით",
                                bullets = listOf(
                                    "ახლომდებარე ქალაქებსა და მუნიციპალიტეტებში მიწოდების ვადა: 1–3 სამუშაო დღე.",
                                    "საქართველოს სხვა რეგიონებში მიწოდების ვადა: 2–5 სამუშაო დღე.",
                                    "საკურიერო მომსახურების საფასური დამოკიდებულია მიმღებ ქალაქსა და მანძილზე.",
                                    "გაგზავნამდე თითოეული ნაწილი და მოწყობილობა გადის ვიზუალურ და ტექნიკურ შემოწმებას."
                                )
                            )
                        }

                        LegalPolicyTopic.RETURN_AND_REFUND_POLICY -> {
                            PolicySectionCard(
                                title = "1. პროდუქციის შემოწმება და დაბრუნების პირობები",
                                bullets = listOf(
                                    "მომხმარებელს უფლება აქვს მოითხოვოს ქარხნული წუნის მქონე სათადარიგო ნაწილის ან აქსესუარის შეცვლა ან თანხის დაბრუნება.",
                                    "სათადარიგო ნაწილებზე (ეკრანი, შლეიფი, კამერა, ელემენტი) აუცილებელია პირველადი შემოწმება მონტაჟამდე — ქარხნული დამცავი ფირისა და პლომბის მოხსნის გარეშე.",
                                    "მექანიკურად დაზიანებული, დაწებებული ან პლომბმოხსნილი დეტალი უკან არ ბრუნდება."
                                )
                            )
                            PolicySectionCard(
                                title = "2. თანხის ანაზღაურების წესი",
                                bullets = listOf(
                                    "დადასტურებული დაბრუნების შემთხვევაში თანხის ანაზღაურება ხდება იმავე მეთოდით (საბანკო გადმორიცხვით ან ნაღდი ანგარიშსწორებით) 1–3 სამუშაო დღის ვადაში.",
                                    "ყველა დაბრუნებული გადახდა აისახება შეკვეთის ისტორიაში სტატუსით „დაბრუნებული“."
                                )
                            )
                        }

                        LegalPolicyTopic.PRIVACY_POLICY -> {
                            PolicySectionCard(
                                title = "1. რა მონაცემები მუშავდება",
                                bullets = listOf(
                                    "შეკვეთის შესასრულებლად მუშავდება მხოლოდ აუცილებელი მონაცემები: სახელი, ელ. ფოსტა, საკონტაქტო ტელეფონი, მიწოდების მისამართი და შეკვეთის ისტორია.",
                                    "BSTORE არ ინახავს მომხმარებლის საბანკო ბარათის ნომრებს ან CVC კოდებს."
                                )
                            )
                            PolicySectionCard(
                                title = "2. მონაცემთა დაცვა და ანგარიშის წაშლა",
                                bullets = listOf(
                                    "მონაცემები დაცულია Firebase Authentication-ისა და Firestore Security Rules-ის მეშვეობით.",
                                    "მომხმარებელს ნებისმიერ დროს შეუძლია საკუთარი ანგარიშისა და პერსონალური მონაცემების სრული წაშლა პროფილის გვერდიდან."
                                )
                            )
                        }

                        LegalPolicyTopic.TERMS_AND_CONDITIONS -> {
                            PolicySectionCard(
                                title = "1. შეკვეთის გაფორმება და ანგარიშსწორება (${BstorePaymentManager.CURRENT_TERMS_VERSION})",
                                bullets = listOf(
                                    "შეკვეთის გაფორმება შესაძლებელია ორი მეთოდით: საბანკო გადარიცხვა (საქართველოს ბანკი / თიბისი ბანკი) ან ნაღდი ანგარიშსწორება ნივთის მიღებისას.",
                                    "საბანკო გადარიცხვის არჩევისას მომხმარებელმა დანიშნულებაში აუცილებლად უნდა მიუთითოს შეკვეთის ნომერი (#BS-...).",
                                    "საბანკო გადარიცხვით გაფორმებული შეკვეთა გადახდილად ითვლება მხოლოდ BSTORE-ის მიერ საბანკო ანგარიშზე თანხის ჩარიცხვის გადამოწმებისა და დადასტურების შემდეგ.",
                                    "ნაღდი ანგარიშსწორების (COD) არჩევისას მომხმარებელი იღებს ვალდებულებას გადაიხადოს შეკვეთის სრული ღირებულება ნივთის ჩაბარებისას."
                                )
                            )
                        }

                        LegalPolicyTopic.CONTACT_INFO -> {
                            PolicySectionCard(
                                title = "BSTORE — ოფიციალური საკონტაქტო ინფორმაცია",
                                bullets = listOf(
                                    "ფილიალის მისამართი: $serviceAddress",
                                    "ტელეფონი / WhatsApp: $primaryPhone",
                                    "სამუშაო საათები: $workingHours",
                                    "ოფიციალური ელ. ფოსტა: ${AuthAndOrderManager.SOLE_ADMIN_EMAIL}"
                                )
                            )
                        }
                    }
                }

                // Footer
                Surface(
                    color = Color(0xFF161A22),
                    border = BorderStroke(1.dp, Color(0xFF222834)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(14.dp)) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("დახურვა", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySectionCard(
    title: String,
    bullets: List<String>
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF171B24),
        border = BorderStroke(1.dp, Color(0xFF262D3B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            bullets.forEach { line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("•", color = Color(0xFFFF6B00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = line,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ==============================================================================
// 6. ADMIN PANEL — PAYMENT DASHBOARD, BANK TRANSFER & COD VERIFICATION, AUDIT LOGS
// ==============================================================================

@Composable
fun AdminPaymentDashboardSection(
    orders: List<OrderRecord>,
    selectedPaymentFilter: String,
    onPaymentFilterChange: (String) -> Unit,
    searchOrderQuery: String,
    onSearchOrderQueryChange: (String) -> Unit,
    onOpenAuditHistory: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode

    val metrics = remember(orders) {
        var pendingBankCount = 0
        var verificationPendingCount = 0
        var awaitingCodCount = 0
        var confirmedPaidCount = 0
        var confirmedPaidGelTotal = 0.0

        orders.forEach { ord ->
            val mType = PaymentMethodType.fromCode(ord.paymentMethodCode, ord.paymentMethod)
            val pStatus = PaymentStatus.fromCode(ord.paymentStatus, mType)
            val isOrderCancelled = ord.status == "გაუქმებული" || pStatus == PaymentStatus.CANCELLED || pStatus == PaymentStatus.FAILED || pStatus == PaymentStatus.REFUNDED

            if (!isOrderCancelled) {
                if (mType == PaymentMethodType.BANK_TRANSFER &&
                    (pStatus == PaymentStatus.PENDING || pStatus == PaymentStatus.VERIFICATION_PENDING)
                ) {
                    pendingBankCount++
                    if (pStatus == PaymentStatus.VERIFICATION_PENDING) {
                        verificationPendingCount++
                    }
                }
                if (mType == PaymentMethodType.COD && pStatus == PaymentStatus.AWAITING_COLLECTION) {
                    awaitingCodCount++
                }
                if (pStatus == PaymentStatus.PAID) {
                    confirmedPaidCount++
                    val num = ord.totalAmount.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                    confirmedPaidGelTotal += num
                }
            }
        }
        listOf(
            pendingBankCount.toDouble(),
            verificationPendingCount.toDouble(),
            awaitingCodCount.toDouble(),
            confirmedPaidCount.toDouble(),
            confirmedPaidGelTotal
        )
    }
    val pendingBankCount = metrics[0].toInt()
    val verificationPendingCount = metrics[1].toInt()
    val awaitingCodCount = metrics[2].toInt()
    val confirmedPaidCount = metrics[3].toInt()
    val confirmedPaidGelTotal = metrics[4]

    val paymentFilters = remember {
        listOf(
            "ყველა",
            "გადახდის მოლოდინში",
            "გადარიცხვა მოწმდება",
            "გადახდილია",
            "COD",
            "გაუქმებული",
            "საბანკო გადარიცხვა",
            "საქართველოს ბანკი",
            "თიბისი ბანკი",
            "დაბრუნებული"
        )
    }

    val p = bstorePalette(isDark)

    BstoreCard(
        isDark = isDark,
        accentBorder = p.borderSubtle,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_payment_dashboard_section")
    ) {
        Column(
            modifier = Modifier.padding(BstoreDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Payments,
                        contentDescription = null,
                        tint = p.brandOrange,
                        modifier = Modifier.size(BstoreDimens.IconMd)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "გადახდების კონტროლი & ფინანსური პანელი",
                        color = p.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        lineHeight = 17.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                BstoreSecondaryButton(
                    text = "აუდიტი",
                    onClick = onOpenAuditHistory,
                    isDark = isDark,
                    leadingIcon = Icons.Default.History,
                    size = BstoreButtonSize.COMPACT,
                    containerColor = p.brandOrangeSoft,
                    borderColor = p.brandOrangeBorder,
                    contentColor = p.brandOrange,
                    maxLines = 1,
                    modifier = Modifier.testTag("admin_open_payment_audit_button")
                )
            }

            // 4 Metric Cards in Clean Responsive 2x2 Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_financial_metrics_2x2_grid"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMetricMiniCard(
                        modifier = Modifier.weight(1f),
                        isDark = isDark,
                        title = "საბანკო გადარიცხვის მოლოდინში",
                        value = "$pendingBankCount",
                        subtitle = if (verificationPendingCount > 0) "მოწმდება: $verificationPendingCount" else "ჩარიცხვის მოლოდინში",
                        valueColor = p.warningAmber,
                        tag = "metric_pending_bank"
                    )
                    AdminMetricMiniCard(
                        modifier = Modifier.weight(1f),
                        isDark = isDark,
                        title = "COD მოლოდინში",
                        value = "$awaitingCodCount",
                        subtitle = "მიღებისას გადახდა",
                        valueColor = p.infoBlue,
                        tag = "metric_awaiting_cod"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminMetricMiniCard(
                        modifier = Modifier.weight(1f),
                        isDark = isDark,
                        title = "დადასტურებული გადახდები",
                        value = "$confirmedPaidCount",
                        subtitle = "გადახდილი შეკვეთა",
                        valueColor = p.successGreen,
                        tag = "metric_confirmed_paid_count"
                    )
                    AdminMetricMiniCard(
                        modifier = Modifier.weight(1f),
                        isDark = isDark,
                        title = "ჩარიცხული (GEL)",
                        value = "${confirmedPaidGelTotal.toInt()} ₾",
                        subtitle = "დადასტურებული თანხა",
                        valueColor = p.successGreen,
                        tag = "metric_confirmed_paid_gel"
                    )
                }
            }

            // Order Number / Reference Search Input
            BstoreSearchField(
                query = searchOrderQuery,
                onQueryChange = onSearchOrderQueryChange,
                placeholder = "ძებნა: #BS-..., ტელეფონი ან ბანკი",
                inputTestTag = "admin_orders_search_input",
                isDark = isDark
            )

            // Payment Filters Row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(paymentFilters) { filterName ->
                    val isSelected = selectedPaymentFilter == filterName
                    BstoreFilterChip(
                        text = filterName,
                        selected = isSelected,
                        onClick = { onPaymentFilterChange(filterName) },
                        isDark = isDark,
                        modifier = Modifier.testTag("admin_payment_filter_$filterName")
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
private fun AdminMetricMiniCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    title: String,
    value: String,
    subtitle: String = "",
    valueColor: Color,
    tag: String
) {
    val p = bstorePalette(isDark)
    Surface(
        shape = RoundedCornerShape(BstoreRadii.Control),
        color = p.subSurfaceBg,
        border = BorderStroke(BstoreDimens.BorderThin, p.borderSubtle),
        modifier = modifier.testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = p.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = p.textMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun matchesAdminPaymentFilter(order: OrderRecord, filter: String, searchQuery: String = ""): Boolean {
    val mType = PaymentMethodType.fromCode(order.paymentMethodCode, order.paymentMethod)
    val pStatus = PaymentStatus.fromCode(order.paymentStatus, mType)

    if (searchQuery.isNotBlank()) {
        val q = searchQuery.trim().lowercase()
        val matchesSearch = order.orderNumber.lowercase().contains(q) ||
            order.id.lowercase().contains(q) ||
            order.paymentReference.lowercase().contains(q) ||
            order.userPhone.lowercase().contains(q) ||
            order.userName.lowercase().contains(q) ||
            order.bankName.lowercase().contains(q) ||
            order.items.any { it.productTitle.lowercase().contains(q) }
        if (!matchesSearch) return false
    }

    return when (filter) {
        "ყველა" -> true
        "საბანკო გადარიცხვა" -> mType == PaymentMethodType.BANK_TRANSFER
        "საქართველოს ბანკი" -> mType == PaymentMethodType.BANK_TRANSFER &&
            (order.bankCode.equals("bog", true) || order.bankName.contains("საქართველოს", true))
        "თიბისი ბანკი" -> mType == PaymentMethodType.BANK_TRANSFER &&
            (order.bankCode.equals("tbc", true) || order.bankName.contains("თიბისი", true))
        "ნაღდი ანგარიშსწორება", "COD" -> mType == PaymentMethodType.COD
        "გადახდის მოლოდინში", "დადასტურების მოლოდინში" ->
            pStatus == PaymentStatus.PENDING || pStatus == PaymentStatus.AWAITING_COLLECTION || pStatus == PaymentStatus.VERIFICATION_PENDING
        "გადარიცხვა მოწმდება" -> pStatus == PaymentStatus.VERIFICATION_PENDING
        "გადახდილია", "გადახდილი" -> pStatus == PaymentStatus.PAID
        "გაუქმებული" -> pStatus == PaymentStatus.CANCELLED || order.status == "გაუქმებული"
        "დაბრუნებული" -> pStatus == PaymentStatus.REFUNDED
        else -> true
    }
}

// ==============================================================================
// 7. ADMIN BANK TRANSFER & COD CONFIRMATION DIALOGS
// ==============================================================================

@Composable
fun AdminConfirmPaymentDialog(
    order: OrderRecord,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (internalNote: String) -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    val methodType = PaymentMethodType.fromCode(order.paymentMethodCode, order.paymentMethod)
    val isBankTransfer = methodType == PaymentMethodType.BANK_TRANSFER
    var internalNote by remember { mutableStateOf(order.paymentInternalNote) }

    BstoreDialogShell(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        isDark = isDark,
        borderColor = p.successGreen.copy(alpha = 0.6f),
        maxWidth = 460.dp,
        modifier = Modifier.testTag("admin_confirm_payment_dialog")
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = p.successSoft,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = p.successGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBankTransfer) "საბანკო ჩარიცხვის დადასტურება" else "ნაღდი ანგარიშსწორების დადასტურება",
                        color = p.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 19.sp
                    )
                    Text(
                        text = "შეკვეთა ${order.orderNumber} • ${order.totalAmount}",
                        color = p.brandOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(BstoreRadii.Control),
                color = p.subSurfaceBg,
                border = BorderStroke(BstoreDimens.BorderThin, p.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isBankTransfer) {
                            "დარწმუნებული ხართ, რომ თანხა ნამდვილად ჩაირიცხა თქვენს საბანკო ანგარიშზე?"
                        } else {
                            "დარწმუნებული ხართ, რომ ნაღდი ანგარიშსწორების თანხა (${order.totalAmount}) ნამდვილად მიღებულია?"
                        },
                        color = p.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("admin_confirm_payment_prompt_text")
                    )
                    if (isBankTransfer) {
                        Text(
                            text = "არჩეული ბანკი: ${order.bankName.ifBlank { "საბანკო გადარიცხვა" }}",
                            color = p.textSecondary,
                            fontSize = 11.5.sp
                        )
                        if (order.ibanSnapshot.isNotBlank()) {
                            Text(
                                text = "IBAN: ${order.ibanSnapshot}",
                                color = p.brandOrange,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "დანიშნულება: ${order.paymentReference.ifBlank { order.orderNumber }}",
                            color = p.infoBlue,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "მოსალოდნელი თანხა: ${order.totalAmount}",
                        color = p.successGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            BstoreOutlinedInput(
                value = internalNote,
                onValueChange = { internalNote = it },
                label = "შიდა შენიშვნა (არასავალდებულო)",
                placeholder = "მაგ: ჩარიცხვა გადამოწმებულია BOG ბანკინგში",
                singleLine = true,
                enabled = !isSubmitting,
                isDark = isDark,
                accentColor = p.successGreen,
                modifier = Modifier.testTag("admin_confirm_payment_note_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BstoreOutlinedButton(
                    text = "გაუქმება",
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    isDark = isDark,
                    size = BstoreButtonSize.SECONDARY,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                BstorePrimaryButton(
                    text = if (isSubmitting) "მუშავდება..." else "დადასტურება ✓",
                    onClick = { if (!isSubmitting) onConfirm(internalNote.trim()) },
                    enabled = !isSubmitting,
                    isLoading = isSubmitting,
                    size = BstoreButtonSize.SECONDARY,
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1.35f)
                        .testTag("admin_confirm_payment_submit_button")
                )
            }
        }
    }
}

@Composable
fun AdminPaymentAuditHistoryDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stateVer = BstorePaymentManager.stateVersion.intValue
    var auditList by remember(stateVer) {
        mutableStateOf(BstorePaymentManager.getAuditEvents(context))
    }

    LaunchedEffect(Unit) {
        runCatching {
            val cloudEvents = FirestoreRepository(context).fetchPaymentAuditLogsForAdmin().getOrNull()
            if (!cloudEvents.isNullOrEmpty()) {
                auditList = BstorePaymentManager.mergeCloudAuditEvents(context, cloudEvents)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF12151C),
            border = BorderStroke(1.dp, Color(0xFF28303E)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("admin_payment_audit_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161A22))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFFF6B00))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "გადახდების უცვლელი აუდიტის ჟურნალი",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "სულ ჩანაწერი: ${auditList.size}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = Color(0xFFA0A5B0))
                    }
                }

                if (auditList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("აუდიტის ჩანაწერები ჯერ არ არის", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(auditList, key = { it.id }) { ev ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF171B24),
                                border = BorderStroke(1.dp, Color(0xFF262D3B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = ev.eventTitleKa(),
                                            color = Color(0xFFFF7A00),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ev.formattedTimestamp,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.5.sp
                                        )
                                    }
                                    Text(
                                        text = "შეკვეთა/ობიექტი: ${ev.orderNumber.ifBlank { ev.orderId }} • სტატუსი: ${ev.previousStatus} → ${ev.newStatus}",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (ev.amount > 0) {
                                        Text(
                                            text = "თანხა: ${ev.amount.toInt()} ${ev.currency}",
                                            color = Color(0xFF4ADE80),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "შემსრულებელი: ${ev.actorEmail.ifBlank { ev.actorUid }} (${ev.actorRole})",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.5.sp
                                    )
                                    if (ev.note.isNotBlank()) {
                                        Text(
                                            text = "შენიშვნა: ${ev.note}",
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 11.sp
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
}

// ==============================================================================
// 8. ADMIN SCALABLE MULTI-BANK ACCOUNT MANAGEMENT SCREEN
// ==============================================================================

@Composable
fun AdminMultiBankAccountsManagementView() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val stateVer = BstorePaymentManager.stateVersion.intValue
    var bankAccounts by remember(stateVer) {
        mutableStateOf(BstorePaymentManager.getAllBankAccounts(context))
    }
    var editingAccount by remember { mutableStateOf<BankAccountConfig?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    val pendingBankOrdersCount = remember(stateVer) {
        AuthAndOrderManager.getOrders(context).count { ord ->
            val mType = PaymentMethodType.fromCode(ord.paymentMethodCode, ord.paymentMethod)
            val pStatus = PaymentStatus.fromCode(ord.paymentStatus, mType)
            mType == PaymentMethodType.BANK_TRANSFER && pStatus == PaymentStatus.PENDING
        }
    }

    DisposableEffect(Unit) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            registration = FirestoreRepository(context).listenToPaymentBankAccounts { cloudList ->
                if (cloudList.isNotEmpty()) {
                    BstorePaymentManager.syncFromCloudBankAccounts(context, cloudList)
                    bankAccounts = BstorePaymentManager.getAllBankAccounts(context)
                }
            }
        } catch (_: Exception) {}
        onDispose {
            registration?.remove()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .imePadding()
            .testTag("admin_bank_accounts_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ჰედერი: საბანკო ანგარიშების მართვა (ჰორიზონტალური, სუფთა განლაგება)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF7A00).copy(alpha = 0.16f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Color(0xFFFF7A00),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "საბანკო ანგარიშების მართვა",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Multi-Bank • აქტიური: ${bankAccounts.count { it.isEnabled }} / ${bankAccounts.size} • უსაფრთხო Snapshot",
                                color = Color(0xFFA0A5B0),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isAddingNew = true
                            editingAccount = BankAccountConfig(
                                id = "bank_custom_${System.currentTimeMillis()}",
                                bankCode = "bog",
                                bankName = "საქართველოს ბანკი",
                                recipientName = "",
                                iban = "",
                                description = "ლარის ანგარიში (GEL)",
                                isEnabled = true,
                                displayOrder = bankAccounts.size + 1
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("admin_add_bank_account_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ახალი ბანკის დამატება", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF101319),
                        border = BorderStroke(1.dp, Color(0xFF252B37)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔒 უსაფრთხოების პოლიტიკა: საბანკო ანგარიშის ნომრები (IBAN) არ ქვეყნდება საჯაროდ. მომხმარებელი ხედავს მხოლოდ საკუთარ შეკვეთაზე არჩეული ბანკის რეკვიზიტებს შეკვეთის შექმნის შემდეგ. ანგარიშის გათიშვა ან შეცვლა არ აზიანებს უკვე შექმნილ შეკვეთებს (Snapshot დაცვა).",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    if (pendingBankOrdersCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF261D0E),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ℹ️ ამჟამად დადასტურების მოლოდინშია $pendingBankOrdersCount საბანკო გადარიცხვის შეკვეთა. თითოეული მათგანი ინახავს შეკვეთის მომენტში მოქმედ IBAN Snapshot-ს.",
                                color = Color(0xFFFDE68A),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        items(bankAccounts, key = { it.id }) { acc ->
            val brandColor = acc.brandColor()
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = BorderStroke(
                    1.5.dp,
                    if (acc.isEnabled) brandColor.copy(alpha = 0.55f) else Color(0xFF282D37)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_bank_card_${acc.id}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            BankBrandLogo(
                                bankCode = acc.bankCode,
                                bankName = acc.bankName,
                                size = 36.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${acc.displayOrder}. ${acc.bankName}",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "მიმღები: ${acc.recipientName}",
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = acc.isEnabled,
                            onCheckedChange = { enabledNow ->
                                val updated = acc.copy(isEnabled = enabledNow)
                                val res = BstorePaymentManager.upsertBankAccountAsAdmin(context, updated)
                                if (res.isSuccess) {
                                    bankAccounts = BstorePaymentManager.getAllBankAccounts(context)
                                    coroutineScope.launch(Dispatchers.IO) {
                                        runCatching {
                                            FirestoreRepository(context).savePaymentBankAccountToCloud(updated)
                                        }
                                    }
                                    Toast.makeText(
                                        context,
                                        if (enabledNow) "✅ ${acc.bankName} ჩაირთო" else "⏸️ ${acc.bankName} გაითიშა ახალი შეკვეთებისთვის",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        res.exceptionOrNull()?.localizedMessage ?: "შეცდომა",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = brandColor
                            ),
                            modifier = Modifier.testTag("admin_bank_toggle_${acc.id}")
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F1216),
                        border = BorderStroke(1.dp, Color(0xFF252B37)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "IBAN: ${acc.iban}",
                                color = brandColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (acc.description.isNotBlank()) {
                                Text(
                                    text = acc.description,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "ინსტრუქცია: ${acc.paymentInstructions}",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val reordered = acc.copy(displayOrder = (acc.displayOrder - 1).coerceAtLeast(1))
                                    BstorePaymentManager.upsertBankAccountAsAdmin(context, reordered)
                                    bankAccounts = BstorePaymentManager.getAllBankAccounts(context)
                                    coroutineScope.launch(Dispatchers.IO) {
                                        runCatching { FirestoreRepository(context).savePaymentBankAccountToCloud(reordered) }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("↑ ზემოთ", color = Color(0xFFA0A5B0), fontSize = 10.5.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val reordered = acc.copy(displayOrder = acc.displayOrder + 1)
                                    BstorePaymentManager.upsertBankAccountAsAdmin(context, reordered)
                                    bankAccounts = BstorePaymentManager.getAllBankAccounts(context)
                                    coroutineScope.launch(Dispatchers.IO) {
                                        runCatching { FirestoreRepository(context).savePaymentBankAccountToCloud(reordered) }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("↓ ქვემოთ", color = Color(0xFFA0A5B0), fontSize = 10.5.sp)
                            }
                        }

                        Button(
                            onClick = {
                                isAddingNew = false
                                editingAccount = acc
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222834)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("admin_edit_bank_button_${acc.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("რედაქტირება", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (editingAccount != null) {
        val initial = editingAccount!!
        var bankCode by remember(initial.id) { mutableStateOf(initial.bankCode) }
        var bankName by remember(initial.id) { mutableStateOf(initial.bankName) }
        var recipientName by remember(initial.id) { mutableStateOf(initial.recipientName) }
        var iban by remember(initial.id) { mutableStateOf(initial.iban) }
        var description by remember(initial.id) { mutableStateOf(initial.description) }
        var instructions by remember(initial.id) { mutableStateOf(initial.paymentInstructions) }
        var displayOrderText by remember(initial.id) { mutableStateOf(initial.displayOrder.toString()) }
        var isEnabled by remember(initial.id) { mutableStateOf(initial.isEnabled) }
        var bankNameError by remember(initial.id) { mutableStateOf<String?>(null) }
        var recipientError by remember(initial.id) { mutableStateOf<String?>(null) }
        var ibanError by remember(initial.id) { mutableStateOf<String?>(null) }
        var formError by remember(initial.id) { mutableStateOf<String?>(null) }
        var isSaving by remember(initial.id) { mutableStateOf(false) }

        val bankPresets = remember {
            listOf(
                "bog" to "საქართველოს ბანკი",
                "tbc" to "თიბისი ბანკი",
                "other" to "სხვა ბანკი"
            )
        }

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFFFF6B00),
            unfocusedBorderColor = Color(0xFF2E3440),
            errorBorderColor = Color(0xFFF87171),
            focusedLabelColor = Color(0xFFFF6B00),
            unfocusedLabelColor = Color(0xFF94A3B8),
            errorLabelColor = Color(0xFFF87171),
            cursorColor = Color(0xFFFF6B00)
        )

        Dialog(
            onDismissRequest = { if (!isSaving) editingAccount = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .imePadding()
                    .navigationBarsPadding()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF161922),
                    border = BorderStroke(1.dp, Color(0xFF2E3440)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .heightIn(max = 680.dp)
                        .testTag("admin_bank_edit_dialog")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isAddingNew) "ახალი საბანკო ანგარიშის დამატება" else "საბანკო ანგარიშის რედაქტირება",
                                color = Color.White,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { if (!isSaving) editingAccount = null },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "დახურვა",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF242A36))

                        // Scrollable Body
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Bank selector — Horizontal scrollable single-line chips
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "აირჩიეთ ბანკი *",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_bank_selector_row"),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(bankPresets, key = { it.first }) { (codePreset, namePreset) ->
                                        val selected = bankCode.equals(codePreset, ignoreCase = true) ||
                                            (codePreset == "other" && !bankCode.equals("bog", true) && !bankCode.equals("tbc", true))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (selected) Color(0xFFFF6B00) else Color(0xFF101319),
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) Color(0xFFFF6B00) else Color(0xFF2E3440)
                                            ),
                                            modifier = Modifier
                                                .heightIn(min = 40.dp)
                                                .testTag("admin_bank_preset_$codePreset")
                                                .clickable {
                                                    bankCode = codePreset
                                                    if (codePreset != "other") {
                                                        bankName = namePreset
                                                        bankNameError = null
                                                    } else if (bankName == "საქართველოს ბანკი" || bankName == "თიბისი ბანკი") {
                                                        bankName = ""
                                                    }
                                                    formError = null
                                                }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                            ) {
                                                Text(
                                                    text = namePreset,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = bankName,
                                onValueChange = {
                                    bankName = it
                                    bankNameError = null
                                    formError = null
                                },
                                label = { Text("ბანკის დასახელება *") },
                                singleLine = true,
                                isError = bankNameError != null,
                                supportingText = bankNameError?.let { err -> { Text(err, color = Color(0xFFF87171), fontSize = 11.sp) } },
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_bank_name_input")
                            )

                            OutlinedTextField(
                                value = recipientName,
                                onValueChange = {
                                    recipientName = it
                                    recipientError = null
                                    formError = null
                                },
                                label = { Text("მიმღების სახელი / კომპანია *") },
                                singleLine = true,
                                isError = recipientError != null,
                                supportingText = recipientError?.let { err -> { Text(err, color = Color(0xFFF87171), fontSize = 11.sp) } },
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_bank_recipient_input")
                            )

                            OutlinedTextField(
                                value = iban,
                                onValueChange = {
                                    iban = BstorePaymentManager.normalizeIban(it)
                                    ibanError = null
                                    formError = null
                                },
                                label = { Text("IBAN ანგარიშის ნომერი *") },
                                placeholder = { Text("GE00BG0000000000000000", color = Color(0xFF64748B)) },
                                singleLine = true,
                                isError = ibanError != null,
                                supportingText = {
                                    Text(
                                        text = ibanError ?: "ქართული 22-სიმბოლოიანი IBAN (მაგ: GE00BG...)",
                                        color = if (ibanError != null) Color(0xFFF87171) else Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                },
                                colors = fieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_bank_iban_input")
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("მოკლე აღწერა") },
                                placeholder = { Text("ლარის ანგარიში (GEL)", color = Color(0xFF64748B)) },
                                singleLine = true,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = instructions,
                                onValueChange = { instructions = it },
                                label = { Text("გადარიცხვის ინსტრუქცია მომხმარებლისთვის") },
                                minLines = 3,
                                maxLines = 5,
                                colors = fieldColors,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = displayOrderText,
                                    onValueChange = { displayOrderText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("რიგითობა") },
                                    singleLine = true,
                                    colors = fieldColors,
                                    modifier = Modifier.weight(1f)
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF101319),
                                    border = BorderStroke(1.dp, Color(0xFF2E3440)),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isEnabled) "აქტიური" else "გამორთული",
                                            color = if (isEnabled) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Switch(
                                            checked = isEnabled,
                                            onCheckedChange = { isEnabled = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFFFF6B00)
                                            )
                                        )
                                    }
                                }
                            }

                            if (formError != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x22EF4444),
                                    border = BorderStroke(1.dp, Color(0x55EF4444)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⚠️ ${formError!!}",
                                        color = Color(0xFFF87171),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF242A36))

                        // Sticky Footer Buttons (Always reachable above keyboard)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { if (!isSaving) editingAccount = null },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("admin_cancel_bank_account_button")
                            ) {
                                Text(
                                    text = "გაუქმება",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Button(
                                onClick = {
                                    val cleanBank = bankName.trim()
                                    val cleanRec = recipientName.trim()
                                    val cleanIban = BstorePaymentManager.normalizeIban(iban)
                                    var hasErr = false
                                    if (cleanBank.isBlank()) {
                                        bankNameError = "მიუთითეთ ბანკის დასახელება"
                                        hasErr = true
                                    }
                                    if (cleanRec.isBlank()) {
                                        recipientError = "მიუთითეთ მიმღების სახელი"
                                        hasErr = true
                                    }
                                    if (!BstorePaymentManager.isValidIban(cleanIban)) {
                                        ibanError = "არასწორი IBAN ფორმატი (22 სიმბოლო)"
                                        hasErr = true
                                    }
                                    if (hasErr) return@Button

                                    val candidate = initial.copy(
                                        bankCode = bankCode.trim().ifBlank { "custom" },
                                        bankName = cleanBank,
                                        recipientName = cleanRec,
                                        iban = cleanIban,
                                        description = description.trim(),
                                        paymentInstructions = instructions.trim(),
                                        displayOrder = displayOrderText.toIntOrNull() ?: initial.displayOrder,
                                        isEnabled = isEnabled
                                    )
                                    isSaving = true
                                    val saveRes = BstorePaymentManager.upsertBankAccountAsAdmin(context, candidate)
                                    if (saveRes.isSuccess) {
                                        val saved = saveRes.getOrThrow()
                                        bankAccounts = BstorePaymentManager.getAllBankAccounts(context)
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val fbUser = runCatching {
                                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                            }.getOrNull()
                                            val cloudRes = if (fbUser != null) {
                                                FirestoreRepository(context).savePaymentBankAccountToCloud(saved)
                                            } else {
                                                Result.success(Unit)
                                            }
                                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                isSaving = false
                                                if (cloudRes.isSuccess) {
                                                    editingAccount = null
                                                    Toast.makeText(context, "✅ საბანკო ანგარიში შეინახა!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    formError = cloudRes.exceptionOrNull()?.localizedMessage
                                                        ?: "Cloud შენახვა ვერ მოხერხდა"
                                                }
                                            }
                                        }
                                    } else {
                                        isSaving = false
                                        formError = saveRes.exceptionOrNull()?.localizedMessage
                                    }
                                },
                                enabled = !isSaving,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("admin_save_bank_account_button")
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = "შენახვა",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
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
