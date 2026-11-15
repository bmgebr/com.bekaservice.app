package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ListenerRegistration
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * BEKASERVICE — დინამიკური კონტაქტებისა და სოციალური ქსელების მართვის სისტემა.
 *
 * უზრუნველყოფს ტელეფონის ნომრებისა და სოციალური ბმულების სრულ CRUD მართვას
 * ადმინ პანელიდან (Firebase Firestore: app_config/contacts), ლოკალურ ქეშირებას
 * და მომხმარებლის აპლიკაციაში დინამიკურ ასახვას კოდის ხელახალი აწყობის გარეშე.
 */

// ==============================================================================
// 1. მონაცემთა მოდელები (Data Models)
// ==============================================================================

/**
 * ტელეფონის ნომრის მოდელი
 */
data class ContactPhoneNumber(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "მთავარი ნომერი",
    val number: String = "+995 598 23 33 32",
    val isEnabled: Boolean = true,
    val displayOrder: Int = 1
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("label", label)
            put("number", number)
            put("isEnabled", isEnabled)
            put("displayOrder", displayOrder)
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "label" to label,
            "number" to number,
            "isEnabled" to isEnabled,
            "displayOrder" to displayOrder
        )
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ContactPhoneNumber {
            return ContactPhoneNumber(
                id = json.optString("id", UUID.randomUUID().toString()),
                label = json.optString("label", "მთავარი ნომერი"),
                number = json.optString("number", "+995 598 23 33 32"),
                isEnabled = json.optBoolean("isEnabled", true),
                displayOrder = json.optInt("displayOrder", 1)
            )
        }

        fun fromMap(map: Map<*, *>): ContactPhoneNumber {
            return ContactPhoneNumber(
                id = map["id"]?.toString() ?: UUID.randomUUID().toString(),
                label = map["label"]?.toString() ?: "მთავარი ნომერი",
                number = map["number"]?.toString() ?: "+995 598 23 33 32",
                isEnabled = (map["isEnabled"] as? Boolean) ?: true,
                displayOrder = (map["displayOrder"] as? Number)?.toInt() ?: 1
            )
        }
    }
}

/**
 * სოციალური პლატფორმის ტიპები
 */
enum class SocialPlatform(
    val key: String,
    val titleKa: String,
    val defaultUrlPrefix: String,
    val iconEmoji: String,
    val brandColorHex: Long
) {
    WHATSAPP("WHATSAPP", "WhatsApp", "https://wa.me/", "💬", 0xFF25D366),
    TELEGRAM("TELEGRAM", "Telegram", "https://t.me/", "✈️", 0xFF2AABEE),
    FACEBOOK("FACEBOOK", "Facebook", "https://www.facebook.com/", "📘", 0xFF1877F2),
    INSTAGRAM("INSTAGRAM", "Instagram", "https://www.instagram.com/", "📷", 0xFFE4405F),
    TIKTOK("TIKTOK", "TikTok", "https://www.tiktok.com/@", "🎵", 0xFFEE1D52),
    YOUTUBE("YOUTUBE", "YouTube", "https://www.youtube.com/", "▶️", 0xFFFF0000),
    WEBSITE("WEBSITE", "ვებსაიტი", "https://", "🌐", 0xFFFF7A00),
    CUSTOM("CUSTOM", "სხვა ბმული", "https://", "🔗", 0xFF94A3B8);

    companion object {
        fun fromKey(key: String): SocialPlatform {
            return entries.find { it.key.equals(key.trim(), ignoreCase = true) } ?: CUSTOM
        }
    }
}

/**
 * სოციალური ქსელისა და გარე ბმულის მოდელი
 */
data class SocialMediaLink(
    val id: String = UUID.randomUUID().toString(),
    val platform: SocialPlatform = SocialPlatform.CUSTOM,
    val title: String = "",
    val url: String = "",
    val isEnabled: Boolean = true,
    val displayOrder: Int = 1
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("platform", platform.key)
            put("title", title)
            put("url", url)
            put("isEnabled", isEnabled)
            put("displayOrder", displayOrder)
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "platform" to platform.key,
            "title" to title,
            "url" to url,
            "isEnabled" to isEnabled,
            "displayOrder" to displayOrder
        )
    }

    companion object {
        fun fromJsonObject(json: JSONObject): SocialMediaLink {
            val platformKey = json.optString("platform", "CUSTOM")
            return SocialMediaLink(
                id = json.optString("id", UUID.randomUUID().toString()),
                platform = SocialPlatform.fromKey(platformKey),
                title = json.optString("title", ""),
                url = json.optString("url", ""),
                isEnabled = json.optBoolean("isEnabled", true),
                displayOrder = json.optInt("displayOrder", 1)
            )
        }

        fun fromMap(map: Map<*, *>): SocialMediaLink {
            val platformKey = map["platform"]?.toString() ?: "CUSTOM"
            return SocialMediaLink(
                id = map["id"]?.toString() ?: UUID.randomUUID().toString(),
                platform = SocialPlatform.fromKey(platformKey),
                title = map["title"]?.toString() ?: "",
                url = map["url"]?.toString() ?: "",
                isEnabled = (map["isEnabled"] as? Boolean) ?: true,
                displayOrder = (map["displayOrder"] as? Number)?.toInt() ?: 1
            )
        }
    }
}

/**
 * კონტაქტებისა და სოციალური ქსელების სრული კონფიგურაციის მოდელი
 */
data class ContactAndSocialConfig(
    val phoneNumbers: List<ContactPhoneNumber> = emptyList(),
    val socialLinks: List<SocialMediaLink> = emptyList(),
    val serviceAddress: String = AppBrandConfig.SERVICE_CENTER_ADDRESS_KA,
    val workingHours: String = AppBrandConfig.WORKING_HOURS_KA,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val updatedBy: String = "beqa1994@gmail.com"
)

// ==============================================================================
// 2. მენეჯერი & ლოკალური ქეში (ContactAndSocialManager)
// ==============================================================================

object ContactAndSocialManager {
    private const val PREFS_NAME = "bekaservice_contacts_prefs"
    private const val KEY_CONTACTS_JSON = "key_contact_config_json_v1"
    private const val KEY_HAS_CUSTOM_CONFIG = "key_has_custom_contacts_v1"

    // საწყისი ნაგულისხმევი მონაცემები (Default Clean Hardware Business Contacts)
    val DEFAULT_PHONE_NUMBERS = listOf(
        ContactPhoneNumber(
            id = "phone_main",
            label = "მთავარი ნომერი",
            number = "+995 598 23 33 32",
            isEnabled = true,
            displayOrder = 1
        ),
        ContactPhoneNumber(
            id = "phone_whatsapp",
            label = "WhatsApp კონსულტაცია",
            number = "+995 598 23 33 32",
            isEnabled = true,
            displayOrder = 2
        ),
        ContactPhoneNumber(
            id = "phone_service",
            label = "სერვის ცენტრი",
            number = "+995 598 23 33 32",
            isEnabled = true,
            displayOrder = 3
        )
    )

    val DEFAULT_SOCIAL_LINKS = listOf(
        SocialMediaLink(
            id = "social_wa",
            platform = SocialPlatform.WHATSAPP,
            title = "WhatsApp",
            url = "https://wa.me/995598233332",
            isEnabled = true,
            displayOrder = 1
        ),
        SocialMediaLink(
            id = "social_tg",
            platform = SocialPlatform.TELEGRAM,
            title = "Telegram არხი",
            url = "https://t.me/bekaservice",
            isEnabled = true,
            displayOrder = 2
        ),
        SocialMediaLink(
            id = "social_fb",
            platform = SocialPlatform.FACEBOOK,
            title = "Facebook გვერდი",
            url = "https://www.facebook.com",
            isEnabled = true,
            displayOrder = 3
        ),
        SocialMediaLink(
            id = "social_ig",
            platform = SocialPlatform.INSTAGRAM,
            title = "Instagram",
            url = "https://www.instagram.com",
            isEnabled = true,
            displayOrder = 4
        ),
        SocialMediaLink(
            id = "social_web",
            platform = SocialPlatform.WEBSITE,
            title = "ოფიციალური საიტი",
            url = "https://bekaservice.ge",
            isEnabled = true,
            displayOrder = 5
        )
    )

    val DEFAULT_CONFIG = ContactAndSocialConfig(
        phoneNumbers = DEFAULT_PHONE_NUMBERS,
        socialLinks = DEFAULT_SOCIAL_LINKS,
        serviceAddress = AppBrandConfig.SERVICE_CENTER_ADDRESS_KA,
        workingHours = AppBrandConfig.WORKING_HOURS_KA,
        updatedAtMillis = System.currentTimeMillis()
    )

    // რეაქტიული მდგომარეობა Jetpack Compose UI-სთვის
    var currentConfig by mutableStateOf(DEFAULT_CONFIG)
        private set

    val configStateVersion = mutableIntStateOf(0)

    /**
     * საწყისი ინიციალიზაცია (კითხულობს შენახულ მონაცემებს)
     */
    fun init(context: Context) {
        val loaded = loadConfig(context)
        currentConfig = loaded
        configStateVersion.intValue++
    }

    /**
     * ლოკალურად შენახული კონფიგურაციის წაკითხვა
     */
    fun loadConfig(context: Context): ContactAndSocialConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasCustom = prefs.getBoolean(KEY_HAS_CUSTOM_CONFIG, false)
        val jsonStr = prefs.getString(KEY_CONTACTS_JSON, null)

        if (!hasCustom || jsonStr.isNullOrBlank()) {
            return DEFAULT_CONFIG
        }

        return try {
            val root = JSONObject(jsonStr)
            val phoneArray = root.optJSONArray("phoneNumbers") ?: JSONArray()
            val phoneList = mutableListOf<ContactPhoneNumber>()
            for (i in 0 until phoneArray.length()) {
                phoneList.add(ContactPhoneNumber.fromJsonObject(phoneArray.getJSONObject(i)))
            }

            val socialArray = root.optJSONArray("socialLinks") ?: JSONArray()
            val socialList = mutableListOf<SocialMediaLink>()
            for (i in 0 until socialArray.length()) {
                socialList.add(SocialMediaLink.fromJsonObject(socialArray.getJSONObject(i)))
            }

            ContactAndSocialConfig(
                phoneNumbers = phoneList.sortedBy { it.displayOrder },
                socialLinks = socialList.sortedBy { it.displayOrder },
                serviceAddress = root.optString("serviceAddress", AppBrandConfig.SERVICE_CENTER_ADDRESS_KA),
                workingHours = root.optString("workingHours", AppBrandConfig.WORKING_HOURS_KA),
                updatedAtMillis = root.optLong("updatedAtMillis", System.currentTimeMillis()),
                updatedBy = root.optString("updatedBy", "beqa1994@gmail.com")
            )
        } catch (e: Exception) {
            Log.e("ContactManager", "Error parsing cached contact config", e)
            DEFAULT_CONFIG
        }
    }

    /**
     * კონფიგურაციის ლოკალურად შენახვა SharedPreferences-ში
     */
    fun saveLocalConfig(context: Context, config: ContactAndSocialConfig) {
        try {
            val root = JSONObject().apply {
                val phoneArray = JSONArray()
                config.phoneNumbers.forEach { phoneArray.put(it.toJsonObject()) }
                put("phoneNumbers", phoneArray)

                val socialArray = JSONArray()
                config.socialLinks.forEach { socialArray.put(it.toJsonObject()) }
                put("socialLinks", socialArray)

                put("serviceAddress", config.serviceAddress)
                put("workingHours", config.workingHours)
                put("updatedAtMillis", config.updatedAtMillis)
                put("updatedBy", config.updatedBy)
            }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CONTACTS_JSON, root.toString())
                .putBoolean(KEY_HAS_CUSTOM_CONFIG, true)
                .commit()

            currentConfig = config
            configStateVersion.intValue++
        } catch (e: Exception) {
            Log.e("ContactManager", "Error saving local contact config", e)
        }
    }

    /**
     * პირველი აქტიური ტელეფონის ნომრის დაბრუნება (მომხმარებლის მთავარი ზარებისთვის)
     */
    fun getPrimaryPhoneNumber(context: Context? = null): String {
        val active = currentConfig.phoneNumbers
            .filter { it.isEnabled }
            .sortedBy { it.displayOrder }
        return active.firstOrNull()?.number ?: AppBrandConfig.SUPPORT_PHONE
    }

    /**
     * Cloud Firestore-იდან მიღებული კონფიგურაციის ასახვა
     */
    fun updateFromCloud(context: Context, config: ContactAndSocialConfig) {
        saveLocalConfig(context, config)
    }

    /**
     * სოციალური ქსელის ბმულის გახსნა (სტრინგი URL-ით)
     */
    fun openSocialLink(context: Context, url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return

        try {
            val uri = Uri.parse(cleanUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("ContactManager", "Could not open social link: ${e.message}")
        }
    }

    /**
     * პირველი აქტიური WhatsApp ბმულის დაბრუნება
     */
    fun getPrimaryWhatsAppUrl(context: Context, customText: String? = null): String {
        val activeWa = currentConfig.socialLinks
            .filter { it.isEnabled && it.platform == SocialPlatform.WHATSAPP }
            .sortedBy { it.displayOrder }
            .firstOrNull()

        val baseWaUrl = if (!activeWa?.url.isNullOrBlank()) {
            activeWa!!.url.trim()
        } else {
            val phone = getPrimaryPhoneNumber(context).filter { it.isDigit() }
            if (phone.isNotBlank()) "https://wa.me/$phone" else AppBrandConfig.SUPPORT_WHATSAPP
        }

        if (customText.isNullOrBlank()) return baseWaUrl
        val separator = if (baseWaUrl.contains("?")) "&" else "?"
        return "$baseWaUrl${separator}text=${Uri.encode(customText)}"
    }

    /**
     * ტელეფონის ნომრის ვალიდაცია
     */
    fun isValidPhoneNumber(number: String): Boolean {
        val clean = number.trim()
        val digits = clean.filter { it.isDigit() }
        if (digits.length < 6 || digits.length > 18) return false
        // უნდა შეიცავდეს მხოლოდ ციფრებს, პლიუსს, სივრცეებს, ტირეებსა და ფრჩხილებს
        return clean.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }
    }

    /**
     * ტელეფონის ნომრის ნორმალიზება dialer URI-სთვის (მაგ: tel:+995598233332)
     */
    fun normalizeDialUri(number: String): Uri {
        val clean = number.trim()
        val hasLeadingPlus = clean.startsWith("+")
        val digits = clean.filter { it.isDigit() }
        val finalDial = if (hasLeadingPlus) "+$digits" else digits
        return Uri.parse("tel:$finalDial")
    }

    /**
     * სოციალური და გარე ბმულის ვალიდაცია (უსაფრთხოების მოთხოვნა)
     */
    fun isValidSocialUrl(url: String): Boolean {
        val clean = url.trim()
        if (clean.isBlank() || clean.contains(" ")) return false
        // უარვყოფთ სახიფათო სქემებს
        val lower = clean.lowercase()
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("file:")) {
            return false
        }
        // მხარდაჭერილია HTTPS, WhatsApp deep link და Telegram deep link
        if (lower.startsWith("https://") ||
            lower.startsWith("http://wa.me/") ||
            lower.startsWith("whatsapp://") ||
            lower.startsWith("tg://")
        ) {
            return try {
                val parsed = Uri.parse(clean)
                !parsed.scheme.isNullOrBlank()
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    /**
     * ტელეფონის აკრეფის ეკრანის გახსნა (Android Dialer Intent)
     */
    fun openDialer(context: Context, number: String) {
        try {
            val uri = normalizeDialUri(number)
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("ContactManager", "Could not launch dialer: ${e.message}")
        }
    }

    /**
     * სოციალური ქსელის ბმულის გახსნა (აპლიკაცია ან ბრაუზერი)
     */
    fun openSocialLink(context: Context, link: SocialMediaLink) {
        val cleanUrl = link.url.trim()
        if (cleanUrl.isBlank()) return

        try {
            val uri = Uri.parse(cleanUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("ContactManager", "Could not open social link: ${e.message}")
        }
    }
}

/**
 * რეალური სოციალური ქსელების ბრენდული ვექტორული ხატულა (Facebook, Instagram, Telegram, WhatsApp, TikTok, YouTube, Website).
 * ანაცვლებს ემოჯი-სუროგატებს ერთიანი პროპორციისა და სტილის ვექტორული გრაფიკით.
 */
@Composable
fun SocialPlatformBrandIcon(
    platform: SocialPlatform,
    modifier: Modifier = Modifier,
    containerSize: Dp = 24.dp,
    glyphColor: Color = Color.White,
    useSolidBadge: Boolean = true
) {
    val brandColor = Color(platform.brandColorHex)
    val badgeShape = RoundedCornerShape((containerSize.value * 0.28f).dp)
    val bgBrush = when (platform) {
        SocialPlatform.INSTAGRAM -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFF58529),
                Color(0xFFDD2A7B),
                Color(0xFF8134AF),
                Color(0xFF515BD4)
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(brandColor, brandColor)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(containerSize)
            .clip(badgeShape)
            .then(
                if (useSolidBadge) {
                    Modifier.background(bgBrush)
                } else {
                    Modifier.background(brandColor.copy(alpha = 0.16f))
                }
            )
            .testTag("social_brand_icon_${platform.key.lowercase()}")
    ) {
        val activeGlyphColor = if (useSolidBadge) glyphColor else brandColor
        Canvas(modifier = Modifier.size(containerSize * 0.62f)) {
            val w = size.width
            val h = size.height
            val strokeW = (w * 0.115f).coerceAtLeast(1.6f)

            when (platform) {
                SocialPlatform.FACEBOOK -> {
                    // Facebook 'f' silhouette
                    val path = Path().apply {
                        moveTo(w * 0.66f, h * 0.16f)
                        lineTo(w * 0.54f, h * 0.16f)
                        cubicTo(w * 0.41f, h * 0.16f, w * 0.36f, h * 0.24f, w * 0.36f, h * 0.37f)
                        lineTo(w * 0.36f, h * 0.48f)
                        lineTo(w * 0.24f, h * 0.48f)
                        lineTo(w * 0.24f, h * 0.64f)
                        lineTo(w * 0.36f, h * 0.64f)
                        lineTo(w * 0.36f, h * 0.94f)
                        lineTo(w * 0.54f, h * 0.94f)
                        lineTo(w * 0.54f, h * 0.64f)
                        lineTo(w * 0.68f, h * 0.64f)
                        lineTo(w * 0.71f, h * 0.48f)
                        lineTo(w * 0.54f, h * 0.48f)
                        lineTo(w * 0.54f, h * 0.38f)
                        cubicTo(w * 0.54f, h * 0.33f, w * 0.56f, h * 0.31f, w * 0.62f, h * 0.31f)
                        lineTo(w * 0.66f, h * 0.31f)
                        close()
                    }
                    drawPath(path = path, color = activeGlyphColor)
                }

                SocialPlatform.INSTAGRAM -> {
                    // Instagram rounded camera frame + lens circle + top-right dot
                    drawRoundRect(
                        color = activeGlyphColor,
                        topLeft = Offset(w * 0.08f, h * 0.08f),
                        size = Size(w * 0.84f, h * 0.84f),
                        cornerRadius = CornerRadius(w * 0.24f, h * 0.24f),
                        style = Stroke(width = strokeW)
                    )
                    drawCircle(
                        color = activeGlyphColor,
                        radius = w * 0.21f,
                        center = Offset(w * 0.5f, h * 0.5f),
                        style = Stroke(width = strokeW)
                    )
                    drawCircle(
                        color = activeGlyphColor,
                        radius = w * 0.065f,
                        center = Offset(w * 0.72f, h * 0.28f)
                    )
                }

                SocialPlatform.TELEGRAM -> {
                    // Telegram paper plane vector
                    val planePath = Path().apply {
                        moveTo(w * 0.90f, h * 0.16f)
                        lineTo(w * 0.10f, h * 0.48f)
                        lineTo(w * 0.34f, h * 0.58f)
                        lineTo(w * 0.74f, h * 0.30f)
                        lineTo(w * 0.44f, h * 0.64f)
                        lineTo(w * 0.43f, h * 0.84f)
                        lineTo(w * 0.56f, h * 0.70f)
                        lineTo(w * 0.74f, h * 0.84f)
                        close()
                    }
                    drawPath(path = planePath, color = activeGlyphColor)
                }

                SocialPlatform.WHATSAPP -> {
                    // WhatsApp speech bubble circle + receiver curve
                    drawCircle(
                        color = activeGlyphColor,
                        radius = w * 0.41f,
                        center = Offset(w * 0.52f, h * 0.48f),
                        style = Stroke(width = strokeW)
                    )
                    val tailPath = Path().apply {
                        moveTo(w * 0.22f, h * 0.72f)
                        lineTo(w * 0.12f, h * 0.90f)
                        lineTo(w * 0.34f, h * 0.82f)
                        close()
                    }
                    drawPath(path = tailPath, color = activeGlyphColor)
                    val handsetPath = Path().apply {
                        moveTo(w * 0.38f, h * 0.34f)
                        cubicTo(w * 0.34f, h * 0.44f, w * 0.42f, h * 0.58f, w * 0.56f, h * 0.64f)
                        lineTo(w * 0.66f, h * 0.58f)
                    }
                    drawPath(
                        path = handsetPath,
                        color = activeGlyphColor,
                        style = Stroke(width = strokeW * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                SocialPlatform.TIKTOK -> {
                    // TikTok musical note
                    val notePath = Path().apply {
                        moveTo(w * 0.60f, h * 0.14f)
                        lineTo(w * 0.60f, h * 0.66f)
                        cubicTo(w * 0.60f, h * 0.80f, w * 0.46f, h * 0.88f, w * 0.34f, h * 0.84f)
                        cubicTo(w * 0.22f, h * 0.80f, w * 0.20f, h * 0.64f, w * 0.32f, h * 0.56f)
                        cubicTo(w * 0.38f, h * 0.52f, w * 0.45f, h * 0.53f, w * 0.49f, h * 0.55f)
                        moveTo(w * 0.60f, h * 0.16f)
                        cubicTo(w * 0.64f, h * 0.28f, w * 0.74f, h * 0.34f, w * 0.84f, h * 0.36f)
                    }
                    drawPath(
                        path = notePath,
                        color = activeGlyphColor,
                        style = Stroke(width = strokeW * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                SocialPlatform.YOUTUBE -> {
                    // YouTube play triangle inside rounded frame
                    drawRoundRect(
                        color = activeGlyphColor,
                        topLeft = Offset(w * 0.06f, h * 0.20f),
                        size = Size(w * 0.88f, h * 0.60f),
                        cornerRadius = CornerRadius(w * 0.18f, h * 0.18f),
                        style = Stroke(width = strokeW)
                    )
                    val playTriangle = Path().apply {
                        moveTo(w * 0.40f, h * 0.34f)
                        lineTo(w * 0.68f, h * 0.50f)
                        lineTo(w * 0.40f, h * 0.66f)
                        close()
                    }
                    drawPath(path = playTriangle, color = activeGlyphColor)
                }

                SocialPlatform.WEBSITE, SocialPlatform.CUSTOM -> {
                    // Globe meridian lines
                    val r = w * 0.42f
                    val center = Offset(w * 0.5f, h * 0.5f)
                    drawCircle(
                        color = activeGlyphColor,
                        radius = r,
                        center = center,
                        style = Stroke(width = strokeW)
                    )
                    drawOval(
                        color = activeGlyphColor,
                        topLeft = Offset(w * 0.29f, h * 0.08f),
                        size = Size(w * 0.42f, h * 0.84f),
                        style = Stroke(width = strokeW * 0.85f)
                    )
                    drawLine(
                        color = activeGlyphColor,
                        start = Offset(w * 0.10f, h * 0.50f),
                        end = Offset(w * 0.90f, h * 0.50f),
                        strokeWidth = strokeW * 0.85f
                    )
                }
            }
        }
    }
}

