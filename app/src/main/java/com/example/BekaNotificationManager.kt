package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class NotificationVisibility {
    PRIVATE_USER,
    ADMIN_ONLY,
    PUBLIC
}

sealed class NotificationDestination {
    abstract val key: String

    data class ProductDetail(
        val productDocId: String,
        val numericProductId: Int = 0
    ) : NotificationDestination() {
        override val key: String get() = "prod_${productDocId}_$numericProductId"
    }

    data class Discounts(
        val campaignId: String = ""
    ) : NotificationDestination() {
        override val key: String get() = "discounts_$campaignId"
    }

    data class CustomerOrderDetail(
        val orderId: String,
        val orderNumber: String = "",
        val recipientUid: String = "",
        val isProductRequest: Boolean = false
    ) : NotificationDestination() {
        override val key: String get() = "order_${orderId}_$orderNumber"
    }

    data class ProductRequestDetail(
        val requestId: String,
        val requestNumber: String = "",
        val recipientUid: String = ""
    ) : NotificationDestination() {
        override val key: String get() = "req_${requestId}_$requestNumber"
    }

    data class AdminOrderDetail(
        val orderId: String,
        val orderType: String = "service_order"
    ) : NotificationDestination() {
        override val key: String get() = "admin_order_${orderId}_$orderType"
    }

    data class AppUpdate(
        val updateAction: String = "check"
    ) : NotificationDestination() {
        override val key: String get() = "app_update_$updateAction"
    }

    data class InAppTab(
        val tabIndex: Int,
        val subSection: Int = 0
    ) : NotificationDestination() {
        override val key: String get() = "tab_${tabIndex}_$subSection"
    }

    data class NotificationDetailModal(
        val notificationId: String,
        val title: String,
        val body: String,
        val type: String,
        val createdAtMillis: Long = 0L
    ) : NotificationDestination() {
        override val key: String get() = "modal_$notificationId"
    }

    data class UnavailableOrFallback(
        val reasonKa: String,
        val notificationId: String = ""
    ) : NotificationDestination() {
        override val key: String get() = "unavailable_${notificationId}_${reasonKa.hashCode()}"
    }
}

data class PushNotificationRecord(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "announcement",
    val targetProductId: Int = 0,
    val targetTab: Int = -1,
    val senderEmail: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val audience: String = "all",
    val status: String = "submitted",
    val orderId: String = "",
    val orderNumber: String = "",
    val visibility: String = "",
    val recipientUid: String = "",
    val relatedEntityId: String = "",
    val isRead: Boolean = false,
    val targetProductDocId: String = "",
    val targetType: String = "",
    val destination: String = "",
    val imageUrl: String = ""
)

object BekaNotificationManager {
    const val CHANNEL_ID = "bekaservice_push_channel"
    const val CHANNEL_NAME = "BSTORE საჯარო შეტყობინებები"
    const val CHANNEL_DESCRIPTION = "BSTORE საჯარო სიახლეები, აქციები და ზოგადი განცხადებები"
    const val PRIVATE_CHANNEL_ID = "bstore_private_user_channel"
    const val PRIVATE_CHANNEL_NAME = "BSTORE პირადი შეკვეთები და ანგარიში"
    const val PRIVATE_CHANNEL_DESCRIPTION = "პირადი შეკვეთების, გადახდების და ანგარიშის დაცული შეტყობინებები"
    const val ADMIN_ORDERS_CHANNEL_ID = "beka_orders_channel"
    const val ADMIN_ORDERS_CHANNEL_NAME = "BSTORE ადმინის შეკვეთები"
    const val ADMIN_ORDERS_CHANNEL_DESCRIPTION = "ახალი შეკვეთების შეტყობინებები მხოლოდ ავტორიზებული ადმინისტრატორისთვის"
    const val TOPIC_ANNOUNCEMENTS = "bekaservice_announcements"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TARGET_PRODUCT_ID = "extra_target_product_id"
    const val EXTRA_TARGET_PRODUCT_DOC_ID = "extra_target_product_doc_id"
    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val EXTRA_TARGET_ORDER_ID = "extra_target_order_id"
    const val EXTRA_NOTIFICATION_TITLE = "extra_notification_title"
    const val EXTRA_NOTIFICATION_BODY = "extra_notification_body"
    const val EXTRA_NOTIFICATION_VISIBILITY = "extra_notification_visibility"
    const val EXTRA_RECIPIENT_UID = "extra_recipient_uid"
    const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"

    const val EXTRA_ADMIN_NOTIFICATION_ID = "extra_admin_notification_id"
    const val EXTRA_ADMIN_ORDER_ID = "extra_admin_order_id"
    const val EXTRA_ADMIN_ORDER_TYPE = "extra_admin_order_type"
    const val EXTRA_ADMIN_DEEP_LINK = "extra_admin_deep_link"

    private const val PREFS_NAME = "bekaservice_push_prefs"
    private const val KEY_FCM_TOKEN = "fcm_registration_token"
    private const val KEY_DEVICE_INSTALLATION_ID = "device_installation_id"
    private const val KEY_ADMIN_DEVICE_TOKENS_JSON = "admin_device_tokens_json_v1"
    private const val KEY_ADMIN_ORDER_NOTIFICATIONS_JSON = "admin_order_notifications_json_v1"
    private const val KEY_ADMIN_DELIVERED_ORDER_IDS = "admin_delivered_order_notif_ids_v1"
    private const val KEY_DELIVERED_IDS = "delivered_notification_ids"
    private const val KEY_READ_IDS = "read_notification_ids"
    private const val KEY_DISMISSED_USER_IDS = "dismissed_user_notification_ids"
    private const val KEY_LOCAL_HISTORY_JSON = "local_notification_history_json"
    private const val KEY_PUBLIC_HISTORY_JSON = "public_notification_history_v2"
    private const val KEY_PUBLIC_READ_IDS = "public_read_ids_v2"
    private const val KEY_PUBLIC_DELIVERED_IDS = "public_delivered_ids_v2"
    private const val KEY_PUBLIC_DISMISSED_IDS = "public_dismissed_ids_v2"
    private const val KEY_PRIVATE_HISTORY_PREFIX = "private_history_v2_"
    private const val KEY_PRIVATE_READ_PREFIX = "private_read_ids_v2_"
    private const val KEY_PRIVATE_DELIVERED_PREFIX = "private_delivered_ids_v2_"
    private const val KEY_PRIVATE_DISMISSED_PREFIX = "private_dismissed_ids_v2_"
    private const val KEY_LEGACY_GLOBAL_PURGED_V5 = "legacy_global_notif_purged_v5"
    private const val KEY_INITIAL_SNAPSHOT_BASELINE_TS = "initial_snapshot_baseline_ts"
    private const val KEY_PERMISSION_PROMPTED = "post_notifications_prompted"
    private const val KEY_PERMISSION_EXPLANATION_SHOWN = "post_notifications_explanation_shown"
    private const val KEY_PERMISSION_USER_DECLINED_EXPLANATION = "post_notifications_user_declined_explanation"
    private const val KEY_SYSTEM_PERMISSION_REQUESTED = "post_notifications_system_requested"
    private const val KEY_SYSTEM_PERMISSION_DENIED = "post_notifications_system_denied"
    private const val KEY_FAKE_PURGED_V2 = "fake_welcome_notifications_purged_v2"
    private const val KEY_FAKE_PURGED_V3 = "fake_welcome_notifications_purged_v3"
    private const val KEY_FAKE_PURGED_V4 = "fake_welcome_notifications_purged_v4"

    private val PUBLIC_SAFE_TYPES = setOf(
        "announcement",
        "product",
        "product_offer",
        "service_update",
        "system_update",
        "app_update",
        "promotion",
        "product_restock",
        "system_info",
        "maintenance"
    )

    private val PRIVATE_USER_TYPES = setOf(
        "order_update",
        "product_request",
        "payment_update",
        "payment_confirmed",
        "payment_instructions",
        "account_update",
        "direct_message"
    )

    // რეაქტიული ვერსიის მთვლელი UI-ს მყისიერი განახლებისთვის
    val stateVersion = mutableIntStateOf(0)
    val adminNotificationsStateVersion = mutableIntStateOf(0)

    private fun notifyStateChanged() {
        stateVersion.intValue += 1
    }

    private fun notifyAdminNotificationsChanged() {
        adminNotificationsStateVersion.intValue += 1
        stateVersion.intValue += 1
    }

    fun isSyntheticOrTestNotificationId(id: String): Boolean {
        val clean = id.trim().lowercase()
        return clean.startsWith("welcome_bekaservice") ||
            clean.startsWith("local_test_") ||
            clean.startsWith("test_") ||
            clean.startsWith("mock_") ||
            clean.startsWith("dummy_") ||
            clean.startsWith("sample_") ||
            clean.startsWith("fake_") ||
            clean.startsWith("notif_denied") ||
            clean == "welcome" ||
            clean == "test" ||
            clean == "demo"
    }

    fun isSyntheticOrTestNotification(id: String, title: String = "", body: String = ""): Boolean {
        if (isSyntheticOrTestNotificationId(id)) return true
        val cleanBody = body.trim().lowercase()
        if (cleanBody in listOf("test", "ტესტი", "testing", "ტესტ", "demo", "sample", "check", "check 123", "123", "test notification")) {
            return true
        }
        val cleanTitle = title.trim().lowercase()
        if (cleanTitle in listOf("test", "ტესტი", "testing", "ტესტ", "bekaservice ტესტი", "bstore ტესტი")) {
            return true
        }
        if (cleanTitle.contains("ტესტი", ignoreCase = true) && cleanBody.length <= 10) {
            return true
        }
        return false
    }

    fun targetTabLabel(targetTab: Int): String {
        return when (targetTab) {
            0 -> "🏠 მთავარი გვერდი"
            1 -> "📦 კატალოგი"
            2 -> "🏷️ ფასდაკლებები"
            3 -> "🤖 AI ასისტენტი & დიაგნოსტიკა"
            4 -> "👤 პროფილი"
            5 -> "🛒 კალათა"
            6 -> "🔔 შეტყობინებები"
            7 -> "📲 განახლება"
            else -> "🏠 მთავარი გვერდი"
        }
    }

    fun notificationTypeLabel(type: String): String {
        return when (type) {
            "product" -> "🛍️ პროდუქტის სიახლე"
            "product_offer" -> "🔥 პროდუქტის აქცია"
            "promotion" -> "🏷️ ფასდაკლება / აქცია"
            "order_update", "payment_update", "payment_confirmed" -> "📦 შეკვეთის სტატუსი"
            "product_request" -> "📩 პროდუქტის მოთხოვნა"
            "service_update" -> "🛠️ სერვისის სიახლე"
            "system_update", "app_update" -> "📲 აპის განახლება"
            "admin_order" -> "📦 ახალი შეკვეთა"
            else -> "📢 ზოგადი განცხადება"
        }
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
            val existingPrivate = manager.getNotificationChannel(PRIVATE_CHANNEL_ID)
            if (existingPrivate == null) {
                val privateChannel = NotificationChannel(
                    PRIVATE_CHANNEL_ID,
                    PRIVATE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = PRIVATE_CHANNEL_DESCRIPTION
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(privateChannel)
            }
            val existingAdmin = manager.getNotificationChannel(ADMIN_ORDERS_CHANNEL_ID)
            if (existingAdmin == null) {
                val adminChannel = NotificationChannel(
                    ADMIN_ORDERS_CHANNEL_ID,
                    ADMIN_ORDERS_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = ADMIN_ORDERS_CHANNEL_DESCRIPTION
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(adminChannel)
            }
        }
    }

    data class NotificationDeliveryDiagnostics(
        val runtimePermissionGranted: Boolean,
        val appNotificationsEnabled: Boolean,
        val generalChannelEnabled: Boolean,
        val adminOrdersChannelEnabled: Boolean,
        val hasCachedFcmToken: Boolean,
        val manufacturerName: String,
        val manufacturerTitleKa: String,
        val manufacturerStepsKa: List<String>
    ) {
        val isFullyOperational: Boolean
            get() = runtimePermissionGranted && appNotificationsEnabled && generalChannelEnabled
    }

    fun isNotificationChannelEnabled(context: Context, channelId: String = CHANNEL_ID): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return true
        ensureNotificationChannels(context)
        val channel = manager.getNotificationChannel(channelId) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val appContext = context.applicationContext ?: context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        val appEnabled = try {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        } catch (_: Exception) {
            true
        }
        if (!appEnabled) return false
        return isNotificationChannelEnabled(appContext, CHANNEL_ID)
    }

    fun getDeliveryDiagnostics(
        context: Context,
        manufacturerOverride: String? = null
    ): NotificationDeliveryDiagnostics {
        val appContext = context.applicationContext ?: context
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val appEnabled = try {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        } catch (_: Exception) {
            true
        }
        val generalChannelOk = isNotificationChannelEnabled(context, CHANNEL_ID)
        val adminChannelOk = isNotificationChannelEnabled(context, ADMIN_ORDERS_CHANNEL_ID)
        val token = getLocalFcmToken(context)
        val hasValidToken = token.length >= 10 && !token.startsWith("fcm_admin_dev_")
        val rawMfr = (manufacturerOverride ?: Build.MANUFACTURER ?: "Android").trim()
        val lowerMfr = rawMfr.lowercase()

        val (titleKa, stepsKa) = when {
            lowerMfr.contains("xiaomi") || lowerMfr.contains("poco") || lowerMfr.contains("redmi") -> {
                "Xiaomi / POCO / Redmi (MIUI & HyperOS)" to listOf(
                    "გახსენით Settings -> Apps -> Manage apps -> BSTORE -> ჩართეთ Autostart (Background autostart).",
                    "გადადით Battery saver / Battery optimization-ში და აირჩიეთ No restrictions (შეზღუდვის გარეშე).",
                    "Notifications განყოფილებაში ჩართეთ Lock screen და Floating notifications.",
                    "შენიშვნა: აპლიკაციის ჩვეულებრივ დახურვისას (Swipe) შეტყობინებები მოდის, მაგრამ Settings-იდან Force Stop-ის დაჭერა სრულად თიშავს ფონურ მიწოდებას აპის ხელახლა გახსნამდე."
                )
            }
            lowerMfr.contains("samsung") -> {
                "Samsung (One UI)" to listOf(
                    "გახსენით Settings -> Apps -> BSTORE -> Battery და აირჩიეთ Unrestricted (შეუზღუდავი).",
                    "შეამოწმეთ Settings -> Battery -> Background usage limits და დარწმუნდით, რომ BSTORE არ არის Sleeping ან Deep sleeping apps სიაში.",
                    "დარწმუნდით, რომ Data Saver რეჟიმში BSTORE-ს აქვს ფონური ინტერნეტის უფლება."
                )
            }
            lowerMfr.contains("motorola") || lowerMfr.contains("lenovo") -> {
                "Motorola / Lenovo" to listOf(
                    "გახსენით Settings -> Apps -> See all apps -> BSTORE -> App battery usage და აირჩიეთ Unrestricted.",
                    "შეამოწმეთ Adaptive battery და ჩართეთ Background data (ფონური მონაცემები)."
                )
            }
            lowerMfr.contains("huawei") || lowerMfr.contains("honor") -> {
                "Huawei / Honor" to listOf(
                    "გახსენით Settings -> Apps -> App launch -> BSTORE -> გამორთეთ Manage automatically და ჩართეთ Auto-launch, Secondary launch და Run in background."
                )
            }
            else -> {
                "Android ფონური შეტყობინებების რეკომენდაცია ($rawMfr)" to listOf(
                    "გახსენით Settings -> Apps -> BSTORE -> Battery და აირჩიეთ Unrestricted (შეზღუდვის გარეშე).",
                    "დარწმუნდით, რომ Data Saver რეჟიმში ფონური მონაცემები (Unrestricted data) ჩართულია.",
                    "არ გამოიყენოთ Force Stop (იძულებით გაჩერება) სისტემურ პარამეტრებში — ჩვეულებრივი დახურვისას FCM შეტყობინებები უწყვეტად მუშაობს."
                )
            }
        }

        return NotificationDeliveryDiagnostics(
            runtimePermissionGranted = runtimeGranted,
            appNotificationsEnabled = appEnabled,
            generalChannelEnabled = generalChannelOk,
            adminOrdersChannelEnabled = adminChannelOk,
            hasCachedFcmToken = hasValidToken,
            manufacturerName = rawMfr,
            manufacturerTitleKa = titleKa,
            manufacturerStepsKa = stepsKa
        )
    }

    fun openSystemNotificationSettings(context: Context, channelId: String? = null) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!channelId.isNullOrBlank()) {
                    Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, channelId)
                    }
                } else {
                    Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                }
            } else {
                Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", context.packageName, null)
                )
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("BekaPush", "Could not open system notification settings: ${e.message}")
        }
    }

    fun wasPermissionPrompted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PERMISSION_PROMPTED, false)
    }

    fun markPermissionPrompted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PERMISSION_PROMPTED, true).commit()
    }

    fun wasPermissionExplanationShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PERMISSION_EXPLANATION_SHOWN, false)
    }

    fun didUserDeclinePermissionExplanation(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PERMISSION_USER_DECLINED_EXPLANATION, false)
    }

    fun wasSystemPermissionRequested(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SYSTEM_PERMISSION_REQUESTED, false)
    }

    fun wasSystemPermissionDenied(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SYSTEM_PERMISSION_DENIED, false)
    }

    /**
     * Determines whether the in-app Georgian notification explanation dialog should be displayed
     * after onboarding completes.
     * - Returns false on Android versions below 13 (API < 33), where runtime POST_NOTIFICATIONS is not required.
     * - Returns false if notification permission is already granted.
     * - Returns false if the explanation or system permission prompt was already shown or declined ("ახლა არა").
     */
    fun shouldShowPermissionExplanationDialog(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Boolean {
        if (sdkInt < Build.VERSION_CODES.TIRAMISU) {
            return false
        }
        if (hasNotificationPermission(context)) {
            return false
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyPrompted = prefs.getBoolean(KEY_PERMISSION_PROMPTED, false) ||
            prefs.getBoolean(KEY_PERMISSION_EXPLANATION_SHOWN, false) ||
            prefs.getBoolean(KEY_PERMISSION_USER_DECLINED_EXPLANATION, false) ||
            prefs.getBoolean(KEY_SYSTEM_PERMISSION_REQUESTED, false)
        return !alreadyPrompted
    }

    /**
     * Called when the user selects "შეტყობინებების ჩართვა" on the in-app explanation dialog.
     */
    fun onPermissionExplanationAccepted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PERMISSION_EXPLANATION_SHOWN, true)
            .putBoolean(KEY_PERMISSION_USER_DECLINED_EXPLANATION, false)
            .putBoolean(KEY_PERMISSION_PROMPTED, true)
            .putBoolean(KEY_SYSTEM_PERMISSION_REQUESTED, true)
            .commit()
        notifyStateChanged()
    }

    /**
     * Called when the user selects "ახლა არა" on the in-app explanation dialog.
     * Respects the user's choice so the dialog is not repeatedly shown on future launches.
     */
    fun onPermissionExplanationDeclined(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PERMISSION_EXPLANATION_SHOWN, true)
            .putBoolean(KEY_PERMISSION_USER_DECLINED_EXPLANATION, true)
            .putBoolean(KEY_PERMISSION_PROMPTED, true)
            .commit()
        notifyStateChanged()
    }

    /**
     * Records the result of the official Android POST_NOTIFICATIONS runtime permission request.
     */
    fun onSystemPermissionResult(context: Context, isGranted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_PERMISSION_EXPLANATION_SHOWN, true)
            .putBoolean(KEY_PERMISSION_PROMPTED, true)
            .putBoolean(KEY_SYSTEM_PERMISSION_REQUESTED, true)
            .putBoolean(KEY_SYSTEM_PERMISSION_DENIED, !isGranted)
            .commit()
        notifyStateChanged()
    }

    fun resetPermissionOnboardingState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PERMISSION_PROMPTED)
            .remove(KEY_PERMISSION_EXPLANATION_SHOWN)
            .remove(KEY_PERMISSION_USER_DECLINED_EXPLANATION)
            .remove(KEY_SYSTEM_PERMISSION_REQUESTED)
            .remove(KEY_SYSTEM_PERMISSION_DENIED)
            .commit()
        notifyStateChanged()
    }

    fun saveLocalFcmToken(context: Context, token: String) {
        val clean = token.trim()
        if (clean.isBlank() || clean.startsWith("fcm_admin_dev_")) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, clean).apply()
    }

    fun getLocalFcmToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FCM_TOKEN, "") ?: ""
    }

    fun resolveTargetUid(record: PushNotificationRecord): String {
        val explicit = record.recipientUid.trim()
        if (explicit.isNotBlank()) return explicit
        val aud = record.audience.trim()
        return if (aud.startsWith("user:")) aud.removePrefix("user:").trim() else ""
    }

    fun classifyRecord(record: PushNotificationRecord): NotificationVisibility? {
        val vis = record.visibility.trim().uppercase()
        val type = record.type.trim()
        val aud = record.audience.trim()
        val targetUid = resolveTargetUid(record)
        val hasPrivateFields = targetUid.isNotBlank() ||
            aud.startsWith("user:") ||
            record.orderId.isNotBlank() ||
            record.orderNumber.isNotBlank() ||
            type in PRIVATE_USER_TYPES

        if (vis == "ADMIN_ONLY" || type == "admin_order" || aud == "admin_only") {
            return NotificationVisibility.ADMIN_ONLY
        }
        if (vis == "PRIVATE_USER" || hasPrivateFields) {
            return NotificationVisibility.PRIVATE_USER
        }
        if (vis == "PUBLIC") {
            return if (!hasPrivateFields && type in PUBLIC_SAFE_TYPES &&
                (aud.isBlank() || aud.equals("all", ignoreCase = true) || aud.equals("all_users", ignoreCase = true))
            ) {
                NotificationVisibility.PUBLIC
            } else {
                null
            }
        }
        if (vis.isBlank() && type in PUBLIC_SAFE_TYPES &&
            (aud.isBlank() || aud.equals("all", ignoreCase = true) || aud.equals("all_users", ignoreCase = true))
        ) {
            return NotificationVisibility.PUBLIC
        }
        return null
    }

    private fun safeUidKeySegment(uid: String): String {
        return uid.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    }

    private fun readIdSetFromKey(prefs: android.content.SharedPreferences, key: String): MutableSet<String> {
        return (prefs.getString(key, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
    }

    private fun activeUidDeliveredKey(context: Context): String {
        val uid = AuthAndOrderManager.activeRegisteredUidOrNull(context)
        return if (!uid.isNullOrBlank()) "$KEY_PRIVATE_DELIVERED_PREFIX${safeUidKeySegment(uid)}" else KEY_PUBLIC_DELIVERED_IDS
    }

    private fun activeUidReadKey(context: Context): String {
        val uid = AuthAndOrderManager.activeRegisteredUidOrNull(context)
        return if (!uid.isNullOrBlank()) "$KEY_PRIVATE_READ_PREFIX${safeUidKeySegment(uid)}" else KEY_PUBLIC_READ_IDS
    }

    private fun activeUidDismissedKey(context: Context): String {
        val uid = AuthAndOrderManager.activeRegisteredUidOrNull(context)
        return if (!uid.isNullOrBlank()) "$KEY_PRIVATE_DISMISSED_PREFIX${safeUidKeySegment(uid)}" else KEY_PUBLIC_DISMISSED_IDS
    }

    fun hasDeliveredNotification(context: Context, notificationId: String): Boolean {
        if (notificationId.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (notificationId in readIdSetFromKey(prefs, KEY_PUBLIC_DELIVERED_IDS)) return true
        val uid = AuthAndOrderManager.activeRegisteredUidOrNull(context) ?: return false
        return notificationId in readIdSetFromKey(prefs, "$KEY_PRIVATE_DELIVERED_PREFIX${safeUidKeySegment(uid)}")
    }

    fun markNotificationDelivered(
        context: Context,
        notificationId: String,
        visibility: NotificationVisibility? = null
    ) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val targetKey = if (visibility == NotificationVisibility.PUBLIC) {
            KEY_PUBLIC_DELIVERED_IDS
        } else {
            activeUidDeliveredKey(context)
        }
        val current = readIdSetFromKey(prefs, targetKey).toMutableList()
        if (!current.contains(notificationId)) {
            current.add(notificationId)
        }
        prefs.edit().putString(targetKey, current.takeLast(150).joinToString(",")).commit()
    }

    fun clearDeliveredFlag(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keys = buildList {
            add(KEY_PUBLIC_DELIVERED_IDS)
            AuthAndOrderManager.activeRegisteredUidOrNull(context)?.let { uid ->
                add("$KEY_PRIVATE_DELIVERED_PREFIX${safeUidKeySegment(uid)}")
            }
        }
        val editor = prefs.edit()
        keys.forEach { key ->
            val current = readIdSetFromKey(prefs, key).filter { it != notificationId }
            editor.putString(key, current.joinToString(","))
        }
        editor.commit()
    }

    // ==========================================================================
    // წაკითხული / წაუკითხავი სტატუსის და UID-იზოლირებული ლოკალური ისტორიის მართვა
    // ==========================================================================
    fun isNotificationRead(context: Context, notificationId: String): Boolean {
        if (notificationId.isBlank()) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (notificationId in readIdSetFromKey(prefs, KEY_PUBLIC_READ_IDS)) return true
        val uid = AuthAndOrderManager.activeRegisteredUidOrNull(context) ?: return false
        return notificationId in readIdSetFromKey(prefs, "$KEY_PRIVATE_READ_PREFIX${safeUidKeySegment(uid)}")
    }

    fun markNotificationRead(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        markNotificationDelivered(context, notificationId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val matching = loadLocalNotificationHistory(context).find { it.id == notificationId }
        val vis = matching?.let { classifyRecord(it) }
        val key = if (vis == NotificationVisibility.PRIVATE_USER) {
            activeUidReadKey(context)
        } else {
            KEY_PUBLIC_READ_IDS
        }
        val current = readIdSetFromKey(prefs, key)
        if (current.add(notificationId)) {
            prefs.edit().putString(key, current.take(300).joinToString(",")).commit()
            notifyStateChanged()
        }
    }

    fun markNotificationUnread(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var changed = false
        val keys = buildList {
            add(KEY_PUBLIC_READ_IDS)
            AuthAndOrderManager.activeRegisteredUidOrNull(context)?.let { uid ->
                add("$KEY_PRIVATE_READ_PREFIX${safeUidKeySegment(uid)}")
            }
        }
        val editor = prefs.edit()
        keys.forEach { key ->
            val current = readIdSetFromKey(prefs, key)
            if (current.remove(notificationId)) {
                editor.putString(key, current.joinToString(","))
                changed = true
            }
        }
        if (changed) {
            editor.commit()
            notifyStateChanged()
        }
    }

    fun markAllNotificationsRead(context: Context, notifications: List<PushNotificationRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val publicRead = readIdSetFromKey(prefs, KEY_PUBLIC_READ_IDS)
        val activeUid = AuthAndOrderManager.activeRegisteredUidOrNull(context)
        val privateKey = activeUid?.let { "$KEY_PRIVATE_READ_PREFIX${safeUidKeySegment(it)}" }
        val privateRead = privateKey?.let { readIdSetFromKey(prefs, it) } ?: mutableSetOf()
        notifications.forEach { n ->
            if (n.id.isBlank()) return@forEach
            if (classifyRecord(n) == NotificationVisibility.PRIVATE_USER && privateKey != null) {
                privateRead.add(n.id)
            } else if (classifyRecord(n) == NotificationVisibility.PUBLIC) {
                publicRead.add(n.id)
            }
        }
        val editor = prefs.edit()
            .putString(KEY_PUBLIC_READ_IDS, publicRead.take(400).joinToString(","))
        if (privateKey != null) {
            editor.putString(privateKey, privateRead.take(400).joinToString(","))
        }
        editor.commit()
        notifyStateChanged()
    }

    fun isDismissedByUser(context: Context, notificationId: String): Boolean {
        if (notificationId.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (notificationId in readIdSetFromKey(prefs, KEY_PUBLIC_DISMISSED_IDS)) return true
        val uid = AuthAndOrderManager.activeRegisteredUidOrNull(context) ?: return false
        return notificationId in readIdSetFromKey(prefs, "$KEY_PRIVATE_DISMISSED_PREFIX${safeUidKeySegment(uid)}")
    }

    fun dismissNotificationForUser(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val matching = loadLocalNotificationHistory(context).find { it.id == notificationId }
        val key = if (matching != null && classifyRecord(matching) == NotificationVisibility.PRIVATE_USER) {
            activeUidDismissedKey(context)
        } else {
            KEY_PUBLIC_DISMISSED_IDS
        }
        val current = readIdSetFromKey(prefs, key)
        current.add(notificationId)
        prefs.edit().putString(key, current.take(300).joinToString(",")).commit()
        markNotificationRead(context, notificationId)
        notifyStateChanged()
    }

    fun unDismissNotificationForUser(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var changed = false
        val keys = buildList {
            add(KEY_PUBLIC_DISMISSED_IDS)
            AuthAndOrderManager.activeRegisteredUidOrNull(context)?.let { uid ->
                add("$KEY_PRIVATE_DISMISSED_PREFIX${safeUidKeySegment(uid)}")
            }
        }
        val editor = prefs.edit()
        keys.forEach { key ->
            val current = readIdSetFromKey(prefs, key)
            if (current.remove(notificationId)) {
                editor.putString(key, current.joinToString(","))
                changed = true
            }
        }
        if (changed) {
            editor.commit()
            notifyStateChanged()
        }
    }

    private fun parseNotificationJsonArray(raw: String?): List<PushNotificationRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PushNotificationRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val title = obj.optString("title", "").trim()
                val body = obj.optString("body", "").trim()
                if (id.isBlank() || title.isBlank() || isSyntheticOrTestNotification(id, title, body)) continue
                val audience = obj.optString("audience", "all")
                val recipientUid = obj.optString("recipientUid", "").trim()
                    .ifBlank { if (audience.startsWith("user:")) audience.removePrefix("user:").trim() else "" }
                list.add(
                    PushNotificationRecord(
                        id = id,
                        title = title,
                        body = body,
                        type = obj.optString("type", "announcement"),
                        targetProductId = obj.optInt("targetProductId", 0),
                        targetTab = obj.optInt("targetTab", 0),
                        senderEmail = obj.optString("senderEmail", ""),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = obj.optLong("updatedAtMillis", 0L),
                        audience = audience,
                        status = obj.optString("status", "submitted"),
                        orderId = obj.optString("orderId", ""),
                        orderNumber = obj.optString("orderNumber", ""),
                        visibility = obj.optString("visibility", ""),
                        recipientUid = recipientUid,
                        relatedEntityId = obj.optString("relatedEntityId", ""),
                        isRead = obj.optBoolean("isRead", false),
                        targetProductDocId = obj.optString("targetProductDocId", "").trim()
                    )
                )
            }
            list.sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeNotificationList(list: List<PushNotificationRecord>): String {
        val arr = JSONArray()
        list
            .filterNot { it.id.isBlank() || isSyntheticOrTestNotification(it.id, it.title, it.body) }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAtMillis }
            .take(80)
            .forEach { item ->
                val visStr = classifyRecord(item)?.name ?: item.visibility
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("body", item.body)
                    put("type", item.type)
                    put("targetProductId", item.targetProductId)
                    put("targetProductDocId", item.targetProductDocId)
                    put("targetTab", item.targetTab)
                    put("senderEmail", item.senderEmail)
                    put("createdAtMillis", item.createdAtMillis)
                    put("updatedAtMillis", item.updatedAtMillis)
                    put("audience", item.audience)
                    put("status", item.status)
                    put("orderId", item.orderId)
                    put("orderNumber", item.orderNumber)
                    put("visibility", visStr)
                    put("recipientUid", resolveTargetUid(item))
                    put("relatedEntityId", item.relatedEntityId)
                    put("isRead", item.isRead)
                }
                arr.put(obj)
            }
        return arr.toString()
    }

    /**
     * Migrates only explicitly PUBLIC records out of legacy shared cache and purges any
     * legacy unscoped private/ambiguous entries and synthetic test notifications.
     */
    private fun purgeLegacyGlobalNotificationCacheIfNeeded(prefs: android.content.SharedPreferences) {
        if (prefs.getBoolean(KEY_LEGACY_GLOBAL_PURGED_V5, false)) return
        val legacyRaw = prefs.getString(KEY_LOCAL_HISTORY_JSON, null)
        val publicOnlyFromLegacy = if (!legacyRaw.isNullOrBlank()) {
            parseNotificationJsonArray(legacyRaw).filter { classifyRecord(it) == NotificationVisibility.PUBLIC }
        } else {
            emptyList()
        }
        val existingPublic = parseNotificationJsonArray(prefs.getString(KEY_PUBLIC_HISTORY_JSON, null))
        val mergedPublic = (existingPublic + publicOnlyFromLegacy)
            .filter { classifyRecord(it) == NotificationVisibility.PUBLIC }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAtMillis }

        prefs.edit()
            .putString(KEY_PUBLIC_HISTORY_JSON, serializeNotificationList(mergedPublic))
            .remove(KEY_LOCAL_HISTORY_JSON)
            .remove(KEY_READ_IDS)
            .remove(KEY_DELIVERED_IDS)
            .remove(KEY_DISMISSED_USER_IDS)
            .putBoolean(KEY_LEGACY_GLOBAL_PURGED_V5, true)
            .putBoolean(KEY_FAKE_PURGED_V4, true)
            .commit()
    }

    /**
     * ასუფთავებს ძველ სატესტო/მისასალმებელ და გაჟონილ გლობალურ ჩანაწერებს.
     */
    fun purgeSyntheticNotificationsIfNeeded(context: Context, force: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        purgeLegacyGlobalNotificationCacheIfNeeded(prefs)
        if (!force && prefs.getBoolean(KEY_FAKE_PURGED_V4, false)) return
        val publicList = parseNotificationJsonArray(prefs.getString(KEY_PUBLIC_HISTORY_JSON, null))
            .filter { classifyRecord(it) == NotificationVisibility.PUBLIC }
        prefs.edit()
            .putString(KEY_PUBLIC_HISTORY_JSON, serializeNotificationList(publicList))
            .putBoolean(KEY_FAKE_PURGED_V4, true)
            .commit()
        AuthAndOrderManager.activeRegisteredUidOrNull(context)?.let { uid ->
            val key = "$KEY_PRIVATE_HISTORY_PREFIX${safeUidKeySegment(uid)}"
            val privList = parseNotificationJsonArray(prefs.getString(key, null))
                .filter { classifyRecord(it) == NotificationVisibility.PRIVATE_USER && resolveTargetUid(it) == uid }
            prefs.edit().putString(key, serializeNotificationList(privList)).commit()
        }
    }

    fun clearAllNotifications(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .remove(KEY_LOCAL_HISTORY_JSON)
            .remove(KEY_PUBLIC_HISTORY_JSON)
            .remove(KEY_READ_IDS)
            .remove(KEY_DELIVERED_IDS)
            .remove(KEY_DISMISSED_USER_IDS)
            .remove(KEY_PUBLIC_READ_IDS)
            .remove(KEY_PUBLIC_DELIVERED_IDS)
            .remove(KEY_PUBLIC_DISMISSED_IDS)
            .remove(KEY_ADMIN_ORDER_NOTIFICATIONS_JSON)
            .remove(KEY_ADMIN_DELIVERED_ORDER_IDS)
        prefs.all.keys
            .filter {
                it.startsWith(KEY_PRIVATE_HISTORY_PREFIX) ||
                    it.startsWith(KEY_PRIVATE_READ_PREFIX) ||
                    it.startsWith(KEY_PRIVATE_DELIVERED_PREFIX) ||
                    it.startsWith(KEY_PRIVATE_DISMISSED_PREFIX)
            }
            .forEach { editor.remove(it) }
        editor.commit()
        runCatching { NotificationManagerCompat.from(context).cancelAll() }
        notifyStateChanged()
    }

    /**
     * Clears only the specified registered account's local private notification caches/badges,
     * preserving explicitly PUBLIC announcements.
     */
    fun clearPrivateNotificationsForUid(context: Context, uid: String?) {
        val cleanUid = uid?.trim().orEmpty()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .remove(KEY_LOCAL_HISTORY_JSON)
            .remove(KEY_READ_IDS)
            .remove(KEY_DELIVERED_IDS)
            .remove(KEY_DISMISSED_USER_IDS)
        if (cleanUid.isNotBlank()) {
            val seg = safeUidKeySegment(cleanUid)
            editor.remove("$KEY_PRIVATE_HISTORY_PREFIX$seg")
                .remove("$KEY_PRIVATE_READ_PREFIX$seg")
                .remove("$KEY_PRIVATE_DELIVERED_PREFIX$seg")
                .remove("$KEY_PRIVATE_DISMISSED_PREFIX$seg")
        }
        editor.commit()
        notifyStateChanged()
    }

    private fun loadPublicNotificationHistory(context: Context): List<PushNotificationRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        purgeLegacyGlobalNotificationCacheIfNeeded(prefs)
        return parseNotificationJsonArray(prefs.getString(KEY_PUBLIC_HISTORY_JSON, null))
            .filter { classifyRecord(it) == NotificationVisibility.PUBLIC }
    }

    private fun loadPrivateNotificationHistoryForUid(context: Context, uid: String): List<PushNotificationRecord> {
        if (uid.isBlank() || AuthAndOrderManager.isAnonymousFirebaseSession()) return emptyList()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        purgeLegacyGlobalNotificationCacheIfNeeded(prefs)
        val key = "$KEY_PRIVATE_HISTORY_PREFIX${safeUidKeySegment(uid)}"
        return parseNotificationJsonArray(prefs.getString(key, null))
            .filter { classifyRecord(it) == NotificationVisibility.PRIVATE_USER && resolveTargetUid(it) == uid }
    }

    fun loadLocalNotificationHistory(context: Context): List<PushNotificationRecord> {
        val publicList = loadPublicNotificationHistory(context)
        val activeUid = AuthAndOrderManager.activeRegisteredUidOrNull(context)
        val privateList = if (!activeUid.isNullOrBlank()) {
            loadPrivateNotificationHistoryForUid(context, activeUid)
        } else {
            emptyList()
        }
        return (privateList + publicList)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAtMillis }
    }

    private fun saveScopedHistory(
        context: Context,
        storageKey: String,
        list: List<PushNotificationRecord>,
        notify: Boolean = true
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serialized = serializeNotificationList(list)
        val previous = prefs.getString(storageKey, null)
        if (serialized != previous) {
            prefs.edit().putString(storageKey, serialized).commit()
            if (notify) {
                notifyStateChanged()
            }
        }
    }

    fun upsertLocalNotificationRecord(context: Context, record: PushNotificationRecord) {
        if (record.id.isBlank() || isSyntheticOrTestNotification(record.id, record.title, record.body)) return
        if (!isNotificationTargetedToCurrentUser(context, record)) return
        val vis = classifyRecord(record) ?: return
        unDismissNotificationForUser(context, record.id)

        when (vis) {
            NotificationVisibility.PUBLIC -> {
                val current = loadPublicNotificationHistory(context).toMutableList()
                val idx = current.indexOfFirst { it.id == record.id }
                val normalized = record.copy(visibility = "PUBLIC", audience = "all", recipientUid = "")
                if (idx >= 0) current[idx] = normalized else current.add(0, normalized)
                saveScopedHistory(context, KEY_PUBLIC_HISTORY_JSON, current, notify = true)
            }
            NotificationVisibility.PRIVATE_USER -> {
                val activeUid = AuthAndOrderManager.activeRegisteredUidOrNull(context) ?: return
                val targetUid = resolveTargetUid(record)
                if (targetUid != activeUid) return
                val key = "$KEY_PRIVATE_HISTORY_PREFIX${safeUidKeySegment(activeUid)}"
                val current = loadPrivateNotificationHistoryForUid(context, activeUid).toMutableList()
                val idx = current.indexOfFirst { it.id == record.id }
                val normalized = record.copy(
                    visibility = "PRIVATE_USER",
                    recipientUid = activeUid,
                    audience = "user:$activeUid"
                )
                if (idx >= 0) current[idx] = normalized else current.add(0, normalized)
                saveScopedHistory(context, key, current, notify = true)
            }
            NotificationVisibility.ADMIN_ONLY -> Unit
        }
    }

    fun removeLocalNotificationRecord(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val pubFiltered = loadPublicNotificationHistory(context).filterNot { it.id == notificationId }
        saveScopedHistory(context, KEY_PUBLIC_HISTORY_JSON, pubFiltered, notify = true)
        AuthAndOrderManager.activeRegisteredUidOrNull(context)?.let { uid ->
            val key = "$KEY_PRIVATE_HISTORY_PREFIX${safeUidKeySegment(uid)}"
            val privFiltered = loadPrivateNotificationHistoryForUid(context, uid).filterNot { it.id == notificationId }
            saveScopedHistory(context, key, privFiltered, notify = true)
        }
    }

    fun mergeCloudAndLocalNotifications(
        context: Context,
        cloudList: List<PushNotificationRecord>
    ): List<PushNotificationRecord> {
        val byId = LinkedHashMap<String, PushNotificationRecord>()
        cloudList.forEach { n ->
            if (n.id.isNotBlank() &&
                !isSyntheticOrTestNotification(n.id, n.title, n.body) &&
                isNotificationTargetedToCurrentUser(context, n)
            ) {
                byId[n.id] = n
            }
        }
        val localList = loadLocalNotificationHistory(context)
        localList.forEach { local ->
            if (local.id.isNotBlank() &&
                !isSyntheticOrTestNotification(local.id, local.title, local.body) &&
                isNotificationTargetedToCurrentUser(context, local)
            ) {
                if ((local.id.startsWith("fcm_") || local.id.startsWith("imei_notif_")) && !byId.containsKey(local.id)) {
                    byId[local.id] = local
                }
            }
        }
        return byId.values.sortedByDescending { it.createdAtMillis }
    }

    /**
     * სინქრონიზაციას უკეთებს Firestore-ის რეალურ შეტყობინებებს და მიმდინარე ანგარიშის FCM
     * ჩანაწერებს — მკაცრი PUBLIC / PRIVATE_USER იზოლაციით.
     */
    fun syncCloudNotificationsToLocal(context: Context, cloudList: List<PushNotificationRecord>): List<PushNotificationRecord> {
        purgeSyntheticNotificationsIfNeeded(context, force = true)
        val authorizedCloud = cloudList.filter { n ->
            n.id.isNotBlank() &&
                !isSyntheticOrTestNotification(n.id, n.title, n.body) &&
                isNotificationTargetedToCurrentUser(context, n)
        }
        val cloudIds = authorizedCloud.map { it.id }.toSet()

        val cloudPublic = authorizedCloud.filter { classifyRecord(it) == NotificationVisibility.PUBLIC }
        val retainedPublicFcm = loadPublicNotificationHistory(context).filter { local ->
            (local.id.startsWith("fcm_") || local.id.startsWith("imei_notif_")) && !cloudIds.contains(local.id)
        }
        val reconciledPublic = (cloudPublic + retainedPublicFcm)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAtMillis }
        saveScopedHistory(context, KEY_PUBLIC_HISTORY_JSON, reconciledPublic, notify = false)

        val activeUid = AuthAndOrderManager.activeRegisteredUidOrNull(context)
        val reconciledPrivate = if (!activeUid.isNullOrBlank()) {
            val cloudPrivate = authorizedCloud.filter {
                classifyRecord(it) == NotificationVisibility.PRIVATE_USER && resolveTargetUid(it) == activeUid
            }
            val retainedPrivateFcm = loadPrivateNotificationHistoryForUid(context, activeUid).filter { local ->
                (local.id.startsWith("fcm_") || local.id.startsWith("imei_notif_")) && !cloudIds.contains(local.id)
            }
            val combinedPriv = (cloudPrivate + retainedPrivateFcm)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAtMillis }
            saveScopedHistory(context, "$KEY_PRIVATE_HISTORY_PREFIX${safeUidKeySegment(activeUid)}", combinedPriv, notify = false)
            combinedPriv
        } else {
            emptyList()
        }

        val reconciledAll = (reconciledPrivate + reconciledPublic)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAtMillis }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val validPublicIds = reconciledPublic.map { it.id }.toSet()
        val updatedPublicReads = readIdSetFromKey(prefs, KEY_PUBLIC_READ_IDS).filter { it in validPublicIds }
        val editor = prefs.edit().putString(KEY_PUBLIC_READ_IDS, updatedPublicReads.joinToString(","))
        if (!activeUid.isNullOrBlank()) {
            val privReadKey = "$KEY_PRIVATE_READ_PREFIX${safeUidKeySegment(activeUid)}"
            val validPrivIds = reconciledPrivate.map { it.id }.toSet()
            val updatedPrivReads = readIdSetFromKey(prefs, privReadKey).filter { it in validPrivIds }
            editor.putString(privReadKey, updatedPrivReads.joinToString(","))
        }
        editor.commit()

        notifyStateChanged()
        return reconciledAll
    }

    fun isNotificationTargetedToCurrentUser(context: Context, record: PushNotificationRecord): Boolean {
        if (AuthAndOrderManager.isLoggingOut) return false
        val vis = classifyRecord(record) ?: return false
        return when (vis) {
            NotificationVisibility.ADMIN_ONLY -> AuthAndOrderManager.hasVerifiedAdminClaim(context)
            NotificationVisibility.PRIVATE_USER -> {
                if (!AuthAndOrderManager.isRegisteredAuthenticatedUser(context)) return false
                val targetUid = resolveTargetUid(record)
                val activeUid = AuthAndOrderManager.activeRegisteredUidOrNull(context).orEmpty()
                activeUid.isNotBlank() && targetUid.isNotBlank() && targetUid == activeUid
            }
            NotificationVisibility.PUBLIC -> true
        }
    }

    fun getVisibleUserNotifications(
        context: Context,
        cloudList: List<PushNotificationRecord>
    ): List<PushNotificationRecord> {
        val merged = mergeCloudAndLocalNotifications(context, cloudList)
        return merged.filter {
            !isDismissedByUser(context, it.id) && isNotificationTargetedToCurrentUser(context, it)
        }
    }

    fun getUnreadNotificationsCount(
        context: Context,
        cloudList: List<PushNotificationRecord>
    ): Int {
        val visible = getVisibleUserNotifications(context, cloudList)
        return visible.count { !isNotificationRead(context, it.id) }
    }

    fun getOrCreateListenerBaselineTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getLong(KEY_INITIAL_SNAPSHOT_BASELINE_TS, 0L)
        if (existing > 0L) return existing
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_INITIAL_SNAPSHOT_BASELINE_TS, now).apply()
        return now
    }

    fun buildDeepLinkIntent(
        context: Context,
        notificationId: String,
        title: String,
        body: String,
        targetProductId: Int = 0,
        targetTab: Int = 0,
        orderId: String = "",
        visibility: String = "",
        recipientUid: String = "",
        targetProductDocId: String = "",
        type: String = ""
    ): Intent {
        val safeNotifId = notificationId.ifBlank { "notif_${title.hashCode()}_${body.hashCode()}" }
        return Intent(context, MainActivity::class.java).apply {
            action = "com.example.bstore.NOTIFICATION_${safeNotifId.hashCode()}"
            data = android.net.Uri.parse("bekaservice://notification/$safeNotifId")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, safeNotifId)
            putExtra(EXTRA_NOTIFICATION_TITLE, title)
            putExtra(EXTRA_NOTIFICATION_BODY, body)
            putExtra(EXTRA_TARGET_PRODUCT_ID, targetProductId)
            putExtra(EXTRA_TARGET_TAB, targetTab)
            if (targetProductDocId.isNotBlank()) {
                putExtra(EXTRA_TARGET_PRODUCT_DOC_ID, targetProductDocId.trim())
                putExtra("productDocId", targetProductDocId.trim())
            }
            if (type.isNotBlank()) {
                putExtra(EXTRA_NOTIFICATION_TYPE, type.trim())
                putExtra("type", type.trim())
            }
            if (visibility.isNotBlank()) {
                putExtra(EXTRA_NOTIFICATION_VISIBILITY, visibility)
            }
            if (recipientUid.isNotBlank()) {
                putExtra(EXTRA_RECIPIENT_UID, recipientUid)
            }
            if (orderId.isNotBlank()) {
                putExtra(EXTRA_TARGET_ORDER_ID, orderId)
            }
        }
    }

    @Volatile
    private var cachedLargeAppIcon: android.graphics.Bitmap? = null

    fun loadAppIconBitmap(context: Context): android.graphics.Bitmap? {
        cachedLargeAppIcon?.let { if (!it.isRecycled) return it }
        return try {
            val raw = android.graphics.BitmapFactory.decodeResource(
                context.applicationContext.resources,
                R.drawable.ic_bstore_launcher_fg
            ) ?: return null
            val size = 192
            val output = android.graphics.Bitmap.createBitmap(
                size,
                size,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            
            // Draw a dark circle with 100% transparent corners outside the circle:
            // Android notification shade masks (circular or squircle) will never crop the artwork or corners.
            val radius = size / 2f
            paint.color = android.graphics.Color.parseColor("#0E1116")
            canvas.drawCircle(radius, radius, radius, paint)

            // Draw the calibrated foreground (which already has generous 30% horizontal and 40% vertical margins)
            val destRect = android.graphics.Rect(0, 0, size, size)
            canvas.drawBitmap(raw, null, destRect, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))

            cachedLargeAppIcon = output
            output
        } catch (e: Exception) {
            Log.w("BekaPush", "Could not decode large app icon bitmap: ${e.message}")
            null
        }
    }

    fun resolveDestination(record: PushNotificationRecord): NotificationDestination {
        val rawType = record.type.trim().lowercase()
        val cleanDocId = record.targetProductDocId.trim()
        val numProdId = record.targetProductId
        val cleanOrderId = record.orderId.trim()
        val cleanOrderNum = record.orderNumber.trim()
        val cleanRelated = record.relatedEntityId.trim()
        val isProductRequest = rawType == "product_request" ||
            cleanOrderId.startsWith("req_") ||
            cleanOrderNum.startsWith("#REQ-") ||
            cleanDocId.startsWith("req_") ||
            cleanRelated.startsWith("req_") ||
            cleanRelated.startsWith("#REQ-")

        // 1. ADMIN_ONLY Order Notifications
        if (record.visibility.equals("ADMIN_ONLY", ignoreCase = true) || rawType == "admin_order") {
            val targetAdminOrderId = cleanOrderId.ifBlank { cleanRelated }
            return if (targetAdminOrderId.isNotBlank()) {
                NotificationDestination.AdminOrderDetail(targetAdminOrderId)
            } else {
                NotificationDestination.UnavailableOrFallback("ადმინისტრატორის შეკვეთის მონაცემები არასრულია.", record.id)
            }
        }

        // 2. Product-specific notification
        if (rawType == "product") {
            return if (cleanDocId.isNotBlank() || numProdId > 0) {
                NotificationDestination.ProductDetail(cleanDocId, numProdId)
            } else {
                NotificationDestination.UnavailableOrFallback("ეს პროდუქტი ამჟამად მიუწვდომელია ან წაშლილია.", record.id)
            }
        }

        // 3. Discount / Product offer / Promotion
        if (rawType == "product_offer" || rawType == "promotion") {
            return if (cleanDocId.isNotBlank() || numProdId > 0) {
                NotificationDestination.ProductDetail(cleanDocId, numProdId)
            } else {
                NotificationDestination.Discounts(cleanRelated)
            }
        }

        // 4. Product Request
        if (isProductRequest) {
            val reqId = cleanOrderId.ifBlank { cleanOrderNum }.ifBlank { cleanRelated }.ifBlank { cleanDocId }
            return if (reqId.isNotBlank()) {
                NotificationDestination.ProductRequestDetail(
                    requestId = reqId,
                    requestNumber = cleanOrderNum,
                    recipientUid = record.recipientUid
                )
            } else {
                NotificationDestination.UnavailableOrFallback("პროდუქტის მოთხოვნის მონაცემები არასრულია ან წაშლილია.", record.id)
            }
        }

        // 5. Customer Order Update
        if (rawType == "order_update" || rawType == "payment_update" || rawType == "payment_confirmed" || rawType == "payment_instructions") {
            val ordId = cleanOrderId.ifBlank { cleanOrderNum }.ifBlank { cleanRelated }
            return if (ordId.isNotBlank()) {
                NotificationDestination.CustomerOrderDetail(
                    orderId = ordId,
                    orderNumber = cleanOrderNum,
                    recipientUid = record.recipientUid,
                    isProductRequest = false
                )
            } else {
                NotificationDestination.UnavailableOrFallback("შეკვეთის მონაცემები არასრულია ან წაშლილია.", record.id)
            }
        }

        // 6. App Update
        if (rawType == "system_update" || rawType == "app_update") {
            return NotificationDestination.AppUpdate()
        }

        // 7. General Announcement / Service Update
        if (rawType == "announcement" || rawType == "service_update" || rawType == "system_info" || rawType == "maintenance") {
            // If product was explicitly attached:
            if (cleanDocId.isNotBlank() || numProdId > 0) {
                return NotificationDestination.ProductDetail(cleanDocId, numProdId)
            }
            // If order was explicitly attached:
            if (cleanOrderId.isNotBlank() || cleanOrderNum.isNotBlank()) {
                return NotificationDestination.CustomerOrderDetail(
                    orderId = cleanOrderId.ifBlank { cleanOrderNum },
                    orderNumber = cleanOrderNum,
                    recipientUid = record.recipientUid
                )
            }
            // If a specific valid in-app tab was chosen by admin:
            if (record.targetTab in 0..7) {
                return NotificationDestination.InAppTab(record.targetTab)
            }
            // Purely informational announcement: show dedicated notification detail view/dialog
            return NotificationDestination.NotificationDetailModal(
                notificationId = record.id,
                title = record.title,
                body = record.body,
                type = record.type,
                createdAtMillis = record.createdAtMillis
            )
        }

        // 8. Unknown type
        return NotificationDestination.UnavailableOrFallback("შეტყობინების ტიპი უცნობია ან მიუწვდომელია.", record.id)
    }

    fun isNotificationOrDeepLinkIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val uri = intent.data?.toString().orEmpty()
        if (uri.startsWith("bekaservice://") || uri.startsWith("http://") || uri.startsWith("https://")) {
            return true
        }
        val notifId = (intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: intent.getStringExtra("notificationId") ?: intent.getStringExtra("id")).orEmpty().trim()
        if (notifId.isNotBlank()) return true

        val orderId = (intent.getStringExtra(EXTRA_TARGET_ORDER_ID) ?: intent.getStringExtra(EXTRA_ADMIN_ORDER_ID) ?: intent.getStringExtra("orderId") ?: intent.getStringExtra("orderNumber")).orEmpty().trim()
        if (orderId.isNotBlank()) return true

        val prodId = intent.getIntExtra(EXTRA_TARGET_PRODUCT_ID, 0).takeIf { it > 0 }
            ?: intent.getStringExtra("targetProductId")?.trim()?.toIntOrNull()?.takeIf { it > 0 }
            ?: intent.getStringExtra("productId")?.trim()?.toIntOrNull()?.takeIf { it > 0 }
            ?: 0
        if (prodId > 0) return true

        val prodDocId = (intent.getStringExtra(EXTRA_TARGET_PRODUCT_DOC_ID)
            ?: intent.getStringExtra("targetProductDocId")
            ?: intent.getStringExtra("productDocId")).orEmpty().trim()
        if (prodDocId.isNotBlank()) return true

        val fcmType = (intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: intent.getStringExtra("type")).orEmpty().trim()
        if (fcmType.isNotBlank() && fcmType != "null") return true

        val targetTabStr = intent.getStringExtra("targetTab")?.trim()
        val targetTab = intent.getIntExtra(EXTRA_TARGET_TAB, -1)
        if (targetTab in 0..7 || (targetTabStr != null && targetTabStr.toIntOrNull() in 0..7)) {
            return true
        }

        return false
    }

    fun resolveDestinationFromIntent(intent: Intent): NotificationDestination? {
        if (!isNotificationOrDeepLinkIntent(intent)) {
            return null
        }
        val deepLinkUri = intent.data?.toString().orEmpty()
        val extraAdminOrderId = intent.getStringExtra(EXTRA_ADMIN_ORDER_ID)?.trim().orEmpty()
        val extraAdminDeepLink = intent.getStringExtra(EXTRA_ADMIN_DEEP_LINK)?.trim().orEmpty()
        val effectiveDeepLink = extraAdminDeepLink.ifBlank { deepLinkUri }
        val extraVisibility = (intent.getStringExtra(EXTRA_NOTIFICATION_VISIBILITY) ?: intent.getStringExtra("visibility")).orEmpty().trim()
        val fcmDataTypeRaw = (intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: intent.getStringExtra("type")).orEmpty().trim()

        val isAdminOrder = extraAdminOrderId.isNotBlank() ||
            effectiveDeepLink.startsWith("bekaservice://admin/orders") ||
            extraVisibility.equals("ADMIN_ONLY", ignoreCase = true) ||
            fcmDataTypeRaw == "admin_order"

        if (isAdminOrder) {
            val orderId = extraAdminOrderId.ifBlank {
                if (effectiveDeepLink.startsWith("bekaservice://admin/orders/")) {
                    effectiveDeepLink.substringAfterLast("/").substringBefore("?").trim()
                } else {
                    intent.getStringExtra("orderId").orEmpty().trim()
                }
            }
            val orderType = (intent.getStringExtra(EXTRA_ADMIN_ORDER_TYPE) ?: intent.getStringExtra("orderType")).orEmpty().trim()
                .ifBlank { "service_order" }
            return if (orderId.isNotBlank()) {
                NotificationDestination.AdminOrderDetail(orderId, orderType)
            } else {
                NotificationDestination.UnavailableOrFallback("ადმინისტრატორის შეკვეთის მონაცემები არასრულია.")
            }
        }

        val notifId = (intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: intent.getStringExtra("notificationId") ?: intent.getStringExtra("id")).orEmpty().trim()
        val title = (intent.getStringExtra(EXTRA_NOTIFICATION_TITLE) ?: intent.getStringExtra("title")).orEmpty().trim()
        val body = (intent.getStringExtra(EXTRA_NOTIFICATION_BODY) ?: intent.getStringExtra("body")).orEmpty().trim()
        val orderId = (intent.getStringExtra(EXTRA_TARGET_ORDER_ID) ?: intent.getStringExtra("orderId")).orEmpty().trim()
        val orderNum = intent.getStringExtra("orderNumber").orEmpty().trim()
        val recipientUid = (intent.getStringExtra(EXTRA_RECIPIENT_UID) ?: intent.getStringExtra("recipientUid")).orEmpty().trim()
        val targetTabStr = intent.getStringExtra("targetTab").orEmpty().trim()
        val targetTab = intent.getIntExtra(EXTRA_TARGET_TAB, -1).takeIf { it in 0..7 }
            ?: targetTabStr.toIntOrNull()?.takeIf { it in 0..7 }
            ?: -1

        val rawProdIdStr = intent.getStringExtra("productId").orEmpty().trim()
        val targetProdId = intent.getIntExtra(EXTRA_TARGET_PRODUCT_ID, 0).takeIf { it > 0 }
            ?: intent.getStringExtra("targetProductId")?.trim()?.toIntOrNull()?.takeIf { it > 0 }
            ?: rawProdIdStr.toIntOrNull()?.takeIf { it > 0 }
            ?: 0
        val targetProdDocId = (intent.getStringExtra(EXTRA_TARGET_PRODUCT_DOC_ID)
            ?: intent.getStringExtra("targetProductDocId")
            ?: intent.getStringExtra("productDocId")
            ?: rawProdIdStr.takeIf { it.isNotBlank() && it.toIntOrNull() == null })
            .orEmpty().trim()

        val record = PushNotificationRecord(
            id = notifId,
            title = title,
            body = body,
            type = fcmDataTypeRaw.ifBlank { "announcement" },
            targetProductId = targetProdId,
            targetProductDocId = targetProdDocId,
            targetTab = targetTab,
            orderId = orderId,
            orderNumber = orderNum,
            recipientUid = recipientUid,
            visibility = extraVisibility,
            createdAtMillis = System.currentTimeMillis()
        )
        return resolveDestination(record)
    }

    fun destinationSummaryLabel(destination: NotificationDestination): String {
        return when (destination) {
            is NotificationDestination.ProductDetail -> "🛍️ პროდუქტის დეტალები"
            is NotificationDestination.Discounts -> "🏷️ ფასდაკლებები"
            is NotificationDestination.CustomerOrderDetail -> {
                if (destination.orderNumber.isNotBlank()) "📦 შეკვეთა ${destination.orderNumber}"
                else "📦 შეკვეთის დეტალები"
            }
            is NotificationDestination.ProductRequestDetail -> {
                if (destination.requestNumber.isNotBlank()) "📩 მოთხოვნა ${destination.requestNumber}"
                else "📩 პროდუქტის მოთხოვნა"
            }
            is NotificationDestination.AdminOrderDetail -> "📦 შეკვეთა (ადმინი)"
            is NotificationDestination.AppUpdate -> "📲 აპის განახლება"
            is NotificationDestination.InAppTab -> targetTabLabel(destination.tabIndex)
            is NotificationDestination.NotificationDetailModal -> "ℹ️ შეტყობინების დეტალები"
            is NotificationDestination.UnavailableOrFallback -> "⚠️ მიუწვდომელი კონტენტი"
        }
    }

    @SuppressLint("MissingPermission")
    fun showNotification(
        context: Context,
        notificationId: String,
        title: String,
        body: String,
        targetProductId: Int = 0,
        targetTab: Int = 0,
        type: String = "announcement",
        ignoreDeduplication: Boolean = false,
        persistToHistory: Boolean = true,
        orderId: String = "",
        orderNumber: String = "",
        audience: String = "all",
        visibility: String = "",
        recipientUid: String = "",
        targetProductDocId: String = ""
    ): Boolean {
        val cleanTitle = title.trim().ifBlank { "BSTORE" }
        val cleanBody = body.trim()
        if (cleanBody.isBlank()) return false

        ensureNotificationChannels(context)

        val cleanDocId = targetProductDocId.trim()
        val effectiveId = notificationId.trim().ifBlank { "notif_${cleanTitle.hashCode()}_${cleanBody.hashCode()}" }
        val candidateRecord = PushNotificationRecord(
            id = effectiveId,
            title = cleanTitle,
            body = cleanBody,
            type = type,
            targetProductId = targetProductId,
            targetTab = targetTab,
            senderEmail = "beqa1994@gmail.com",
            createdAtMillis = System.currentTimeMillis(),
            audience = audience,
            orderId = orderId,
            orderNumber = orderNumber,
            visibility = visibility,
            recipientUid = recipientUid,
            targetProductDocId = cleanDocId
        )

        val isPreviewTest = !persistToHistory && isSyntheticOrTestNotificationId(effectiveId)
        val resolvedVis = if (isPreviewTest) {
            NotificationVisibility.PUBLIC
        } else {
            val classified = classifyRecord(candidateRecord) ?: return false
            if (!isNotificationTargetedToCurrentUser(context, candidateRecord)) {
                Log.w("BekaPush", "Blocked unauthorized notification $effectiveId ($classified)")
                return false
            }
            classified
        }

        // მხოლოდ რეალური და ავტორიზებული შეტყობინებები შევინახოთ შესაბამის იზოლირებულ ისტორიაში
        if (persistToHistory && !isSyntheticOrTestNotificationId(effectiveId)) {
            upsertLocalNotificationRecord(context, candidateRecord)
            if (ignoreDeduplication) {
                markNotificationUnread(context, effectiveId)
            }
        }

        if (!ignoreDeduplication && hasDeliveredNotification(context, effectiveId)) {
            return false
        }

        if (!hasNotificationPermission(context)) {
            Log.w("BekaPush", "Notification suppressed: POST_NOTIFICATIONS not granted or notifications disabled")
            return false
        }

        val resolvedUid = resolveTargetUid(candidateRecord)
        val tapIntent = buildDeepLinkIntent(
            context = context,
            notificationId = effectiveId,
            title = cleanTitle,
            body = cleanBody,
            targetProductId = targetProductId,
            targetTab = targetTab,
            orderId = orderId,
            visibility = resolvedVis.name,
            recipientUid = resolvedUid,
            targetProductDocId = cleanDocId,
            type = type
        )

        val requestCode = effectiveId.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeAppIcon = loadAppIconBitmap(context)
        val targetChannelId = if (resolvedVis == NotificationVisibility.PRIVATE_USER) {
            PRIVATE_CHANNEL_ID
        } else {
            CHANNEL_ID
        }
        val lockscreenVisibility = if (resolvedVis == NotificationVisibility.PRIVATE_USER) {
            NotificationCompat.VISIBILITY_PRIVATE
        } else {
            NotificationCompat.VISIBILITY_PUBLIC
        }

        val notificationBuilder = NotificationCompat.Builder(context, targetChannelId)
            .setSmallIcon(R.drawable.ic_stat_bekaservice)
            .setColor(0xFFFF7A00.toInt())
            .setContentTitle(cleanTitle)
            .setContentText(cleanBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(cleanBody)
                    .setBigContentTitle(cleanTitle)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(lockscreenVisibility)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (resolvedVis == NotificationVisibility.PRIVATE_USER) {
            val neutralPublicVersion = NotificationCompat.Builder(context, PRIVATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_bekaservice)
                .setColor(0xFFFF7A00.toInt())
                .setContentTitle("BSTORE")
                .setContentText("თქვენ გაქვთ ახალი შეტყობინება BSTORE-ში.")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            notificationBuilder.setPublicVersion(neutralPublicVersion)
        }

        if (largeAppIcon != null) {
            notificationBuilder.setLargeIcon(largeAppIcon)
        }

        val notification = notificationBuilder.build()

        return try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
            markNotificationDelivered(context, effectiveId, resolvedVis)
            true
        } catch (e: Exception) {
            Log.e("BekaPush", "Failed to post system notification", e)
            false
        }
    }

    suspend fun syncFcmTokenAndSubscribeTopic(
        context: Context,
        firestoreRepo: FirestoreRepository?
    ): String? {
        ensureNotificationChannels(context)
        purgeSyntheticNotificationsIfNeeded(context)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            loadAppIconBitmap(context)
        }
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ANNOUNCEMENTS).await()
        } catch (e: Exception) {
            Log.w("BekaPush", "Topic subscription skipped/failed: ${e.message}")
        }

        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w("BekaPush", "FCM token fetch skipped/failed: ${e.message}")
            getLocalFcmToken(context).takeIf { it.isNotBlank() && !it.startsWith("fcm_admin_dev_") }
        }

        val deviceId = getOrCreateDeviceId(context)
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        val isEligibleRegisteredUser = !AuthAndOrderManager.isLoggingOut &&
            AuthAndOrderManager.isRegisteredAuthenticatedUser(context) &&
            fbUser != null &&
            !fbUser.isAnonymous

        if (!token.isNullOrBlank()) {
            saveLocalFcmToken(context, token)
            if (isEligibleRegisteredUser) {
                firestoreRepo?.saveCurrentUserFcmToken(
                    token = token,
                    notificationsEnabled = hasNotificationPermission(context),
                    deviceId = deviceId
                )
                if (AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
                    registerAdminDeviceToken(context, token, deviceId)
                }
            } else {
                // Guest / anonymous / logged-out session: unbind any stale prior account token association
                runCatching {
                    firestoreRepo?.unbindUnauthenticatedDeviceTokenOnBackend(token, deviceId)
                }
            }
        }
        return token
    }

    // ==========================================================================
    // SECURE ADMIN-ONLY ORDER NOTIFICATIONS & MULTI-DEVICE FCM TOKEN MANAGEMENT
    // ==========================================================================

    private val FULL_IMEI_REGEX = Regex("\\b\\d{15}\\b")
    private val EMAIL_PII_REGEX = Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
    private val PHONE_PII_REGEX = Regex("\\+?\\d[\\d\\s\\-]{7,14}\\d")

    /**
     * Ensures no full 15-digit IMEI, customer email, or phone number can ever leak into
     * a push notification title or body.
     */
    fun stripSensitiveDataFromPayload(raw: String): String {
        if (raw.isBlank()) return raw
        return raw
            .replace(FULL_IMEI_REGEX, "[REDACTED]")
            .replace(EMAIL_PII_REGEX, "[REDACTED]")
            .replace(PHONE_PII_REGEX, "[REDACTED]")
            .trim()
    }

    fun containsSensitiveOrderData(text: String): Boolean {
        if (text.isBlank()) return false
        return FULL_IMEI_REGEX.containsMatchIn(text) ||
            EMAIL_PII_REGEX.containsMatchIn(text) ||
            PHONE_PII_REGEX.containsMatchIn(text)
    }

    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_INSTALLATION_ID, null)?.takeIf { it.isNotBlank() }
        if (existing != null) return existing
        val generated = "dev_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
        prefs.edit().putString(KEY_DEVICE_INSTALLATION_ID, generated).commit()
        return generated
    }

    private fun loadAllAdminDeviceTokensInternal(context: Context): List<AdminFcmTokenRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ADMIN_DEVICE_TOKENS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<AdminFcmTokenRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val token = o.optString("token", "").trim()
                val email = o.optString("email", "").trim()
                if (token.isBlank() || !AuthAndOrderManager.isSoleAdminEmail(email)) continue
                list.add(
                    AdminFcmTokenRecord(
                        tokenId = o.optString("tokenId", ""),
                        token = token,
                        userId = o.optString("userId", ""),
                        email = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                        deviceId = o.optString("deviceId", ""),
                        deviceModel = o.optString("deviceModel", "Android"),
                        platform = o.optString("platform", "android"),
                        active = o.optBoolean("active", true),
                        updatedAtMillis = o.optLong("updatedAtMillis", 0L)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveAllAdminDeviceTokensInternal(context: Context, tokens: List<AdminFcmTokenRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        tokens
            .filter { it.token.isNotBlank() && AuthAndOrderManager.isSoleAdminEmail(it.email) && it.active }
            .distinctBy { it.tokenId.ifBlank { "${it.deviceId}_${it.token}" } }
            .forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("tokenId", item.tokenId)
                        put("token", item.token)
                        put("userId", item.userId)
                        put("email", AuthAndOrderManager.SOLE_ADMIN_EMAIL)
                        put("deviceId", item.deviceId)
                        put("deviceModel", item.deviceModel)
                        put("platform", item.platform)
                        put("active", item.active)
                        put("updatedAtMillis", item.updatedAtMillis)
                    }
                )
            }
        prefs.edit().putString(KEY_ADMIN_DEVICE_TOKENS_JSON, arr.toString()).commit()
    }

    /**
     * Registers an FCM device token exclusively for the verified sole administrator ("beqa1994@gmail.com").
     * Rejects registration if no user or a non-admin user is signed in, or if the caller attempts
     * to register a token under a mismatched UID.
     */
    fun registerAdminDeviceToken(
        context: Context,
        token: String,
        deviceId: String = getOrCreateDeviceId(context),
        deviceModel: String = Build.MODEL ?: "Android",
        claimedUserId: String? = null
    ): Boolean {
        val cleanToken = token.trim()
        if (cleanToken.length < 10) return false
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return false
        }
        val currentUser = AuthAndOrderManager.getCurrentUser(context) ?: return false
        if (!AuthAndOrderManager.isSoleAdminEmail(currentUser.email)) {
            return false
        }
        if (claimedUserId != null && claimedUserId != currentUser.id) {
            // Prevent registering a token on behalf of another user's UID
            return false
        }

        val tokenHash = kotlin.math.abs(cleanToken.hashCode()).toString(16)
        val safeDeviceId = deviceId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { getOrCreateDeviceId(context) }.take(64)
        val tokenId = "adm_tok_${safeDeviceId}_$tokenHash"

        val currentTokens = loadAllAdminDeviceTokensInternal(context)
            .filterNot {
                it.deviceId == safeDeviceId ||
                    it.token == cleanToken ||
                    (!cleanToken.startsWith("fcm_admin_dev_") && it.token.startsWith("fcm_admin_dev_"))
            }
            .toMutableList()

        currentTokens.add(
            AdminFcmTokenRecord(
                tokenId = tokenId,
                token = cleanToken,
                userId = currentUser.id,
                email = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                deviceId = safeDeviceId,
                deviceModel = deviceModel,
                platform = "android",
                active = true,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        saveAllAdminDeviceTokensInternal(context, currentTokens)
        if (!cleanToken.startsWith("fcm_admin_dev_")) {
            saveLocalFcmToken(context, cleanToken)
        }

        if (!cleanToken.startsWith("fcm_admin_dev_") && runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching {
                    FirestoreRepository(context).registerAdminDeviceFcmToken(
                        token = cleanToken,
                        deviceId = safeDeviceId,
                        deviceModel = deviceModel
                    )
                }
            }
        }
        return true
    }

    /**
     * Returns active registered admin device tokens ONLY when the caller is the verified sole administrator.
     */
    fun getRegisteredAdminDeviceTokens(context: Context): List<AdminFcmTokenRecord> {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return emptyList()
        }
        return loadAllAdminDeviceTokensInternal(context).filter { it.active }
    }

    /**
     * Removes an invalid or expired FCM token from the admin token registry and Firestore.
     */
    fun removeExpiredOrInvalidAdminToken(context: Context, invalidToken: String) {
        val clean = invalidToken.trim()
        if (clean.isBlank()) return
        val remaining = loadAllAdminDeviceTokensInternal(context).filterNot { it.token == clean }
        saveAllAdminDeviceTokensInternal(context, remaining)
        if (getLocalFcmToken(context) == clean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_FCM_TOKEN)
                .commit()
        }
        if (runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching {
                    FirestoreRepository(context).removeInvalidAdminFcmToken(clean, getOrCreateDeviceId(context))
                }
            }
        }
    }

    /**
     * Called when the sole authorized administrator ("beqa1994@gmail.com") logs in.
     * Automatically registers the current device's real FCM token for admin push notifications.
     */
    fun onAuthorizedAdminAuthenticated(context: Context) {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) return
        val deviceId = getOrCreateDeviceId(context)
        val cachedRealToken = getLocalFcmToken(context).takeIf { it.isNotBlank() && !it.startsWith("fcm_admin_dev_") }
        if (cachedRealToken != null) {
            registerAdminDeviceToken(context, cachedRealToken, deviceId)
        } else {
            // Keep local in-memory state populated immediately while async real FCM token fetch completes
            registerAdminDeviceToken(context, "fcm_admin_$deviceId", deviceId)
        }
        if (runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() != null) {
            val appCtx = context.applicationContext
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching {
                    val realToken = FirebaseMessaging.getInstance().token.await()
                    if (!realToken.isNullOrBlank()) {
                        registerAdminDeviceToken(appCtx, realToken, deviceId)
                        FirestoreRepository(appCtx).saveCurrentUserFcmToken(
                            token = realToken,
                            notificationsEnabled = hasNotificationPermission(appCtx),
                            deviceId = deviceId
                        )
                    }
                }
            }
        }
        notifyAdminNotificationsChanged()
    }

    fun onUserSessionChanged(
        context: Context,
        previousUid: String?,
        newUid: String?
    ) {
        if (previousUid == newUid) return
        val deviceId = getOrCreateDeviceId(context)
        val localToken = getLocalFcmToken(context)
        if (!previousUid.isNullOrBlank()) {
            clearPrivateNotificationsForUid(context, previousUid)
        }
        runCatching { NotificationManagerCompat.from(context).cancelAll() }
        // Re-claim FCM token under the new authenticated UID so old owner's token association is deactivated
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (!newUid.isNullOrBlank() && fbUser != null && !fbUser.isAnonymous && localToken.isNotBlank()) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching {
                    val repo = FirestoreRepository(context.applicationContext)
                    repo.saveCurrentUserFcmToken(
                        token = localToken,
                        notificationsEnabled = hasNotificationPermission(context.applicationContext),
                        deviceId = deviceId
                    )
                }
            }
        }
        notifyStateChanged()
        notifyAdminNotificationsChanged()
    }

    /**
     * Called when a regular (non-admin) customer logs in on this device.
     * Immediately removes this device's token from the admin token registry, clears any displayed
     * admin notifications, and registers the customer's FCM token in `/fcm_tokens` (skipping anonymous).
     */
    fun onNonAdminAuthenticated(context: Context) {
        val deviceId = getOrCreateDeviceId(context)
        val localToken = getLocalFcmToken(context)
        val currentTokens = loadAllAdminDeviceTokensInternal(context)
        val removedAdminTokens = currentTokens.filter {
            it.deviceId == deviceId || (localToken.isNotBlank() && it.token == localToken)
        }
        val remaining = currentTokens.filterNot { item -> removedAdminTokens.any { it.tokenId == item.tokenId } }
        saveAllAdminDeviceTokensInternal(context, remaining)
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (fbUser != null && !fbUser.isAnonymous) {
            val appCtx = context.applicationContext
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val repo = runCatching { FirestoreRepository(appCtx) }.getOrNull()
                val tokenToSync = localToken.ifBlank {
                    runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull().orEmpty()
                }
                if (tokenToSync.isNotBlank()) {
                    saveLocalFcmToken(appCtx, tokenToSync)
                    repo?.saveCurrentUserFcmToken(
                        token = tokenToSync,
                        notificationsEnabled = hasNotificationPermission(appCtx),
                        deviceId = deviceId
                    )
                }
            }
        }
        runCatching { NotificationManagerCompat.from(context).cancelAll() }
        notifyAdminNotificationsChanged()
    }

    /**
     * Called on user logout (or when logging out a specific admin device).
     * Unregisters ONLY the current device's FCM token from `/admin_fcm_tokens` and `/fcm_tokens`
     * (preserving same-UID tokens on other devices) and clears all account-specific local caches/badges.
     */
    fun cleanupOnUserLogout(
        context: Context,
        specificDeviceId: String? = null,
        explicitUid: String = "",
        preFetchedIdToken: String? = null
    ) {
        val targetDeviceId = specificDeviceId?.takeIf { it.isNotBlank() } ?: getOrCreateDeviceId(context)
        val localToken = getLocalFcmToken(context)
        val currentUid = explicitUid.trim().ifBlank {
            runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
                ?: AuthAndOrderManager.activeRegisteredUidOrNull(context).orEmpty()
        }
        val currentTokens = loadAllAdminDeviceTokensInternal(context)
        val removedTokens = currentTokens.filter {
            it.deviceId == targetDeviceId || (specificDeviceId == null && localToken.isNotBlank() && it.token == localToken)
        }
        val remaining = currentTokens.filterNot { item -> removedTokens.any { it.tokenId == item.tokenId } }
        saveAllAdminDeviceTokensInternal(context, remaining)

        clearPrivateNotificationsForUid(context, currentUid)

        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (fbUser != null || !preFetchedIdToken.isNullOrBlank()) {
            val appCtx = context.applicationContext
            val repo = runCatching { FirestoreRepository(appCtx) }.getOrNull()
            removedTokens.forEach { rec ->
                repo?.enqueueUnregisterDeviceFcmTokensBeforeSignOut(rec.token, rec.deviceId, currentUid)
            }
            if (localToken.isNotBlank() || currentUid.isNotBlank()) {
                repo?.enqueueUnregisterDeviceFcmTokensBeforeSignOut(localToken, targetDeviceId, currentUid)
            }
            if (localToken.isNotBlank()) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    runCatching {
                        repo?.releaseFcmTokenOnBackend(localToken, targetDeviceId, preFetchedIdToken)
                    }
                    runCatching {
                        repo?.unbindUnauthenticatedDeviceTokenOnBackend(localToken, targetDeviceId)
                    }
                }
            }
        }
        runCatching { NotificationManagerCompat.from(context).cancelAll() }
        notifyAdminNotificationsChanged()
    }

    suspend fun cleanupOnUserLogoutSuspend(
        context: Context,
        specificDeviceId: String? = null,
        explicitUid: String = "",
        preFetchedIdToken: String? = null
    ) {
        val targetDeviceId = specificDeviceId?.takeIf { it.isNotBlank() } ?: getOrCreateDeviceId(context)
        val localToken = getLocalFcmToken(context)
        val currentUid = explicitUid.trim().ifBlank {
            runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
                ?: AuthAndOrderManager.activeRegisteredUidOrNull(context).orEmpty()
        }
        clearPrivateNotificationsForUid(context, currentUid)
        val appCtx = context.applicationContext
        val repo = runCatching { FirestoreRepository(appCtx) }.getOrNull()
        if (repo != null) {
            kotlinx.coroutines.withTimeoutOrNull(2500L) {
                runCatching {
                    repo.releaseFcmTokenOnBackend(localToken, targetDeviceId, preFetchedIdToken)
                }
                runCatching {
                    repo.unregisterDeviceFcmTokens(localToken, targetDeviceId, currentUid)
                }
            }
        }
    }

    private fun loadAdminOrderNotificationsInternal(context: Context): List<AdminOrderNotificationRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ADMIN_ORDER_NOTIFICATIONS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<AdminOrderNotificationRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val notifId = o.optString("notificationId", "").trim()
                val orderId = o.optString("orderId", "").trim()
                if (notifId.isBlank() || orderId.isBlank()) continue
                val orderType = o.optString("orderType", "imei_check")
                val safeTitle = stripSensitiveDataFromPayload(o.optString("title", "ახალი შეკვეთა"))
                val safeBody = stripSensitiveDataFromPayload(o.optString("body", "შემოვიდა ახალი შეკვეთა."))
                val deepLink = o.optString("deepLink", "").ifBlank {
                    if (orderType == "service_order") {
                        "bekaservice://admin/orders/service/$orderId"
                    } else {
                        "bekaservice://admin/orders/imei/$orderId"
                    }
                }
                list.add(
                    AdminOrderNotificationRecord(
                        notificationId = notifId,
                        orderId = orderId,
                        orderType = orderType,
                        title = safeTitle,
                        body = safeBody,
                        deepLink = deepLink,
                        targetAdminEmail = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                        read = o.optBoolean("read", false),
                        createdAtMillis = o.optLong("createdAtMillis", System.currentTimeMillis()),
                        readAtMillis = o.optLong("readAtMillis", 0L),
                        readByUid = o.optString("readByUid", "")
                    )
                )
            }
            list.sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveAdminOrderNotificationsInternal(
        context: Context,
        list: List<AdminOrderNotificationRecord>,
        notify: Boolean = true
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list
            .filter { it.notificationId.isNotBlank() && it.orderId.isNotBlank() }
            .distinctBy { it.notificationId }
            .sortedByDescending { it.createdAtMillis }
            .take(200)
            .forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("notificationId", item.notificationId)
                        put("orderId", item.orderId)
                        put("orderType", item.orderType)
                        put("title", stripSensitiveDataFromPayload(item.title))
                        put("body", stripSensitiveDataFromPayload(item.body))
                        put("deepLink", item.deepLink)
                        put("targetAdminEmail", AuthAndOrderManager.SOLE_ADMIN_EMAIL)
                        put("read", item.read)
                        put("createdAtMillis", item.createdAtMillis)
                        put("readAtMillis", item.readAtMillis)
                        put("readByUid", item.readByUid)
                    }
                )
            }
        prefs.edit().putString(KEY_ADMIN_ORDER_NOTIFICATIONS_JSON, arr.toString()).commit()
        if (notify) {
            notifyAdminNotificationsChanged()
        }
    }

    /**
     * Returns the administrator order notification history ONLY if the current user is
     * the verified sole administrator ("beqa1994@gmail.com"). Regular customers and logged-out
     * users always receive an empty list.
     */
    fun loadAdminOrderNotificationsForAdmin(context: Context): List<AdminOrderNotificationRecord> {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return emptyList()
        }
        return loadAdminOrderNotificationsInternal(context)
    }

    /**
     * Returns the count of unread orders for the verified sole administrator ("beqa1994@gmail.com").
     * Returns 0 for any non-admin or logged-out user.
     */
    fun getUnreadAdminOrdersCount(context: Context): Int {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return 0
        }
        return loadAdminOrderNotificationsInternal(context).count { !it.read }
    }

    fun isOrderUnreadForAdmin(context: Context, orderId: String): Boolean {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return false
        }
        val clean = orderId.trim().removePrefix("admin_order_")
        return loadAdminOrderNotificationsInternal(context).any { it.orderId == clean && !it.read }
    }

    /**
     * Marks a specific order notification as read ONLY when invoked by the verified sole administrator
     * ("beqa1994@gmail.com"). Rejects unauthorized attempts.
     */
    fun markAdminOrderNotificationAsRead(
        context: Context,
        orderIdOrNotificationId: String,
        orderType: String = ""
    ): Boolean {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return false
        }
        val clean = orderIdOrNotificationId.trim()
        if (clean.isBlank()) return false
        val targetNotifId = if (clean.startsWith("admin_order_")) clean else "admin_order_$clean"
        val targetOrderId = clean.removePrefix("admin_order_")
        val resolvedType = orderType.ifBlank {
            if (targetOrderId.startsWith("imei_")) "imei_check" else "service_order"
        }
        markAdminOrderPushDelivered(context, targetNotifId)
        markAdminOrderPushDelivered(context, "notif_order_${resolvedType}_$targetOrderId")
        markNotificationDelivered(context, targetNotifId)
        markNotificationDelivered(context, "notif_order_${resolvedType}_$targetOrderId")
        val adminUid = AuthAndOrderManager.getCurrentUser(context)?.id.orEmpty()
        val now = System.currentTimeMillis()

        val current = loadAdminOrderNotificationsInternal(context)
        var changed = false
        val updated = current.map { item ->
            if ((item.notificationId == targetNotifId || item.orderId == targetOrderId) && !item.read) {
                changed = true
                item.copy(
                    read = true,
                    readAtMillis = now,
                    readByUid = adminUid
                )
            } else {
                item
            }
        }
        if (changed) {
            saveAdminOrderNotificationsInternal(context, updated, notify = true)
        }
        if (runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching {
                    FirestoreRepository(context).markAdminOrderNotificationRead(targetNotifId, orderType)
                }
            }
        }
        return true
    }

    /**
     * Removes an admin order notification from local cache when the corresponding order/IMEI request
     * is permanently deleted by the sole administrator ("beqa1994@gmail.com").
     */
    fun removeAdminOrderNotificationForDeletedOrder(
        context: Context,
        orderIdOrNotificationId: String
    ): Boolean {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return false
        }
        val clean = orderIdOrNotificationId.trim()
        if (clean.isBlank()) return false
        val targetNotifId = if (clean.startsWith("admin_order_")) clean else "admin_order_$clean"
        val targetOrderId = clean.removePrefix("admin_order_")
        val current = loadAdminOrderNotificationsInternal(context)
        val updated = current.filterNot { it.notificationId == targetNotifId || it.orderId == targetOrderId }
        if (updated.size != current.size) {
            saveAdminOrderNotificationsInternal(context, updated, notify = true)
        }
        return true
    }

    /**
     * Deletes an individual admin order notification from local cache (after Cloud Firestore deletion
     * or in offline/test mode) without altering any customer orders.
     */
    fun deleteAdminOrderNotificationLocalOnly(
        context: Context,
        notificationId: String,
        orderId: String = ""
    ): Boolean {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return false
        }
        val cleanNotifId = notificationId.trim()
        val cleanOrderId = orderId.trim()
        val current = loadAdminOrderNotificationsInternal(context)
        val updated = current.filterNot { item ->
            (cleanNotifId.isNotBlank() && item.notificationId == cleanNotifId) ||
                (cleanOrderId.isNotBlank() && item.orderId == cleanOrderId)
        }
        if (updated.size != current.size) {
            saveAdminOrderNotificationsInternal(context, updated, notify = true)
        }
        return true
    }

    /**
     * Marks all admin order notifications as read ONLY when invoked by the verified sole administrator.
     */
    fun markAllAdminOrderNotificationsAsRead(context: Context): Boolean {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return false
        }
        val adminUid = AuthAndOrderManager.getCurrentUser(context)?.id.orEmpty()
        val now = System.currentTimeMillis()
        val current = loadAdminOrderNotificationsInternal(context)
        val updated = current.map { item ->
            if (!item.read) item.copy(read = true, readAtMillis = now, readByUid = adminUid) else item
        }
        saveAdminOrderNotificationsInternal(context, updated, notify = true)
        if (runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching {
                    FirestoreRepository(context).markAllAdminOrderNotificationsRead()
                }
            }
        }
        return true
    }

    /**
     * Builds the secure deep-link Intent that opens `MainActivity` -> `AdminPanelScreen`
     * directly to the corresponding IMEI request or service order.
     */
    fun buildAdminOrderDeepLinkIntent(
        context: Context,
        record: AdminOrderNotificationRecord
    ): Intent {
        val deepLinkUri = record.deepLink.ifBlank {
            "bekaservice://admin/orders/service/${record.orderId}"
        }
        return Intent(context, MainActivity::class.java).apply {
            action = "OPEN_ADMIN_ORDER_DEEP_LINK"
            data = android.net.Uri.parse(deepLinkUri)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ADMIN_NOTIFICATION_ID, record.notificationId)
            putExtra(EXTRA_ADMIN_ORDER_ID, record.orderId)
            putExtra(EXTRA_ADMIN_ORDER_TYPE, record.orderType)
            putExtra(EXTRA_ADMIN_DEEP_LINK, deepLinkUri)
        }
    }

    fun buildAdminOrderDeepLinkIntent(
        context: Context,
        notificationId: String,
        orderId: String,
        orderType: String
    ): Intent {
        val deepLinkUri = "bekaservice://admin/orders/service/$orderId"
        return buildAdminOrderDeepLinkIntent(
            context = context,
            record = AdminOrderNotificationRecord(
                notificationId = notificationId,
                orderId = orderId,
                orderType = orderType,
                deepLink = deepLinkUri
            )
        )
    }

    private fun hasDeliveredAdminOrderPush(context: Context, notificationId: String): Boolean {
        if (notificationId.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ADMIN_DELIVERED_ORDER_IDS, "") ?: ""
        return raw.split(",").any { it == notificationId }
    }

    private fun markAdminOrderPushDelivered(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getString(KEY_ADMIN_DELIVERED_ORDER_IDS, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableList()
        if (!current.contains(notificationId)) {
            current.add(notificationId)
        }
        prefs.edit().putString(KEY_ADMIN_DELIVERED_ORDER_IDS, current.takeLast(200).joinToString(",")).commit()
    }

    /**
     * Posts a system notification for an incoming order EXCLUSIVELY when the current device is
     * authenticated as the sole administrator ("beqa1994@gmail.com") and has an active registered
     * admin FCM token. Never displays full IMEI or customer private details.
     */
    @SuppressLint("MissingPermission")
    fun showAdminOrderSystemNotification(
        context: Context,
        notificationId: String,
        orderId: String,
        orderType: String,
        title: String,
        body: String,
        ignoreDeduplication: Boolean = false
    ): Boolean {
        val deepLink = if (orderType == "service_order") {
            "bekaservice://admin/orders/service/$orderId"
        } else {
            "bekaservice://admin/orders/imei/$orderId"
        }
        return showAdminOrderSystemNotification(
            context = context,
            record = AdminOrderNotificationRecord(
                notificationId = notificationId.ifBlank { "admin_order_$orderId" },
                orderId = orderId,
                orderType = orderType,
                title = stripSensitiveDataFromPayload(title),
                body = stripSensitiveDataFromPayload(body),
                deepLink = deepLink
            ),
            ignoreDeduplication = ignoreDeduplication
        )
    }

    @SuppressLint("MissingPermission")
    fun showAdminOrderSystemNotification(
        context: Context,
        record: AdminOrderNotificationRecord,
        ignoreDeduplication: Boolean = false
    ): Boolean {
        // Strict server/auth gate: NEVER show admin order notifications to customers or logged-out devices!
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return false
        }
        val activeAdminTokens = loadAllAdminDeviceTokensInternal(context).filter { it.active }
        if (activeAdminTokens.isEmpty()) {
            // Ensure current admin device token is registered if authenticated as sole admin
            onAuthorizedAdminAuthenticated(context)
        }

        if (!ignoreDeduplication && hasDeliveredAdminOrderPush(context, record.notificationId)) {
            return false
        }

        ensureNotificationChannels(context)
        if (!hasNotificationPermission(context)) {
            return false
        }

        val safeTitle = stripSensitiveDataFromPayload(record.title).ifBlank {
            "ახალი შეკვეთა"
        }
        val safeBody = stripSensitiveDataFromPayload(record.body).ifBlank {
            "შემოვიდა ახალი სერვისის შეკვეთა."
        }

        val sanitizedRecord = record.copy(title = safeTitle, body = safeBody)
        val tapIntent = buildAdminOrderDeepLinkIntent(context, sanitizedRecord)
        val requestCode = sanitizedRecord.notificationId.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeAppIcon = loadAppIconBitmap(context)
        val builder = NotificationCompat.Builder(context, ADMIN_ORDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bekaservice)
            .setColor(0xFFFF7A00.toInt())
            .setContentTitle(safeTitle)
            .setContentText(safeBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(safeBody)
                    .setBigContentTitle(safeTitle)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (largeAppIcon != null) {
            builder.setLargeIcon(largeAppIcon)
        }

        return try {
            NotificationManagerCompat.from(context).notify(requestCode, builder.build())
            markAdminOrderPushDelivered(context, sanitizedRecord.notificationId)
            true
        } catch (e: Exception) {
            Log.e("BekaPush", "Failed to post admin order notification", e)
            false
        }
    }

    /**
     * Records a new order in the administrator notification history (so missed push notifications
     * can always be reviewed by the admin and unread counts stay consistent across devices),
     * and dispatches an FCM push notification EXCLUSIVELY to registered admin device tokens.
     * Never sends order notifications to customers or non-admin users.
     */
    fun recordOrderCreatedForAdminOnly(
        context: Context,
        orderId: String,
        orderType: String,
        orderNumber: String = "",
        summaryText: String = "",
        amountText: String = ""
    ): AdminOrderNotificationRecord {
        val cleanOrderId = orderId.trim()
        val normalizedType = if (orderType == "imei_check") "imei_check" else "service_order"
        val notificationId = "admin_order_$cleanOrderId"
        val cleanOrdNum = orderNumber.trim().ifBlank { "#BS-${cleanOrderId.takeLast(4).uppercase()}" }
        val cleanSummary = stripSensitiveDataFromPayload(summaryText).take(90)
        val cleanAmount = stripSensitiveDataFromPayload(amountText).take(30)
        val safeTitle = stripSensitiveDataFromPayload(
            listOfNotNull(
                "ახალი შეკვეთა $cleanOrdNum",
                cleanSummary.takeIf { it.isNotBlank() },
                cleanAmount.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
        ).take(160)
        val safeBody = stripSensitiveDataFromPayload(
            if (cleanSummary.isNotBlank() && cleanAmount.isNotBlank()) {
                "შემოვიდა ახალი შეკვეთა $cleanOrdNum: $cleanSummary ($cleanAmount)."
            } else {
                "შემოვიდა ახალი შეკვეთა $cleanOrdNum."
            }
        ).take(300)
        val deepLink = "bekaservice://admin/orders/service/$cleanOrderId"

        val record = AdminOrderNotificationRecord(
            notificationId = notificationId,
            orderId = cleanOrderId,
            orderType = normalizedType,
            title = safeTitle,
            body = safeBody,
            deepLink = deepLink,
            targetAdminEmail = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            read = false,
            createdAtMillis = System.currentTimeMillis()
        )

        val current = loadAdminOrderNotificationsInternal(context).toMutableList()
        val existingIdx = current.indexOfFirst { it.notificationId == notificationId || it.orderId == cleanOrderId }
        if (existingIdx >= 0) {
            current[existingIdx] = record.copy(read = current[existingIdx].read)
        } else {
            current.add(0, record)
        }
        saveAdminOrderNotificationsInternal(context, current, notify = true)

        // Only display system notification if the current device is authenticated as the sole administrator
        if (AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            showAdminOrderSystemNotification(context, record)
        }
        return record
    }

    /**
     * Records a customer's product stock inquiry / potential order in the administrator
     * notification history and displays a system alert exclusively on verified admin devices.
     */
    fun recordProductInquiryForAdminOnly(
        context: Context,
        orderId: String,
        orderNumber: String,
        requestedProduct: String,
        customerNote: String = ""
    ): AdminOrderNotificationRecord {
        val cleanOrderId = orderId.trim()
        val notificationId = "admin_order_$cleanOrderId"
        val cleanOrdNum = orderNumber.trim().ifBlank { "#REQ-${cleanOrderId.takeLast(4).uppercase()}" }
        val cleanProduct = stripSensitiveDataFromPayload(requestedProduct.trim()).take(80)
        val cleanNote = stripSensitiveDataFromPayload(customerNote.trim()).take(90)

        val safeTitle = stripSensitiveDataFromPayload(
            listOfNotNull(
                "📦 პროდუქტის მოთხოვნა $cleanOrdNum",
                cleanProduct.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
        ).take(160)

        val safeBody = stripSensitiveDataFromPayload(
            buildString {
                append("პოტენციური შეკვეთა $cleanOrdNum: მომხმარებელი კითხულობს მარაგში ხელმისაწვდომობას პროდუქტზე „$cleanProduct“.")
                if (cleanNote.isNotBlank()) {
                    append(" ($cleanNote)")
                }
            }
        ).take(300)

        val deepLink = "bekaservice://admin/orders/service/$cleanOrderId"

        val record = AdminOrderNotificationRecord(
            notificationId = notificationId,
            orderId = cleanOrderId,
            orderType = "service_order",
            title = safeTitle,
            body = safeBody,
            deepLink = deepLink,
            targetAdminEmail = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            read = false,
            createdAtMillis = System.currentTimeMillis()
        )

        val current = loadAdminOrderNotificationsInternal(context).toMutableList()
        val existingIdx = current.indexOfFirst { it.notificationId == notificationId || it.orderId == cleanOrderId }
        if (existingIdx >= 0) {
            current[existingIdx] = record.copy(read = current[existingIdx].read)
        } else {
            current.add(0, record)
        }
        saveAdminOrderNotificationsInternal(context, current, notify = true)

        if (AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            showAdminOrderSystemNotification(context, record, ignoreDeduplication = true)
        }
        return record
    }

    /**
     * Updates the admin notification when the customer presses "✓ თანხა გადავრიცხე".
     */
    fun recordTransferSentForAdminOnly(
        context: Context,
        order: OrderRecord
    ): AdminOrderNotificationRecord {
        val cleanOrderId = order.id.trim()
        val cleanOrdNum = order.orderNumber.ifBlank { "#BS-${cleanOrderId.takeLast(4).uppercase()}" }
        val notificationId = "admin_order_$cleanOrderId"
        val safeTitle = stripSensitiveDataFromPayload("გადარიცხვა დადასტურდა $cleanOrdNum")
        val safeBody = stripSensitiveDataFromPayload(
            "მომხმარებელმა დაადასტურა $cleanOrdNum შეკვეთის თანხის გადმორიცხვა. საჭიროა საბანკო ანგარიშზე შემოწმება."
        )
        val deepLink = "bekaservice://admin/orders/service/$cleanOrderId"

        val record = AdminOrderNotificationRecord(
            notificationId = notificationId,
            orderId = cleanOrderId,
            orderType = "service_order",
            title = safeTitle,
            body = safeBody,
            deepLink = deepLink,
            targetAdminEmail = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            read = false,
            createdAtMillis = System.currentTimeMillis()
        )
        val current = loadAdminOrderNotificationsInternal(context).toMutableList()
        val existingIdx = current.indexOfFirst { it.notificationId == notificationId || it.orderId == cleanOrderId }
        if (existingIdx >= 0) {
            current[existingIdx] = record
        } else {
            current.add(0, record)
        }
        saveAdminOrderNotificationsInternal(context, current, notify = true)
        if (AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            showAdminOrderSystemNotification(context, record, ignoreDeduplication = true)
        }
        return record
    }

    /**
     * Synchronizes cloud `/admin_notifications` documents with the local admin notification history
     * so unread counts and read statuses stay consistent across multiple admin devices.
     */
    fun syncCloudAdminOrderNotificationsToLocal(
        context: Context,
        cloudList: List<AdminOrderNotificationRecord>
    ): List<AdminOrderNotificationRecord> {
        if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
            return emptyList()
        }
        val localById = loadAdminOrderNotificationsInternal(context).associateBy { it.notificationId }
        val mergedMap = LinkedHashMap<String, AdminOrderNotificationRecord>()

        cloudList.forEach { cloud ->
            val local = localById[cloud.notificationId]
            val isRead = cloud.read || (local?.read == true)
            val merged = cloud.copy(
                title = stripSensitiveDataFromPayload(cloud.title),
                body = stripSensitiveDataFromPayload(cloud.body),
                read = isRead,
                readAtMillis = if (cloud.readAtMillis > 0L) cloud.readAtMillis else (local?.readAtMillis ?: 0L),
                readByUid = cloud.readByUid.ifBlank { local?.readByUid.orEmpty() }
            )
            mergedMap[merged.notificationId] = merged
            if (!merged.read && !hasDeliveredAdminOrderPush(context, merged.notificationId)) {
                showAdminOrderSystemNotification(context, merged)
            }
        }

        localById.values.forEach { local ->
            if (!mergedMap.containsKey(local.notificationId)) {
                mergedMap[local.notificationId] = local
            }
        }

        val sorted = mergedMap.values.sortedByDescending { it.createdAtMillis }
        saveAdminOrderNotificationsInternal(context, sorted, notify = true)
        return sorted
    }

    /**
     * Handles incoming FCM data payloads from `BekaFirebaseMessagingService`.
     * Enforces explicit `visibility` validation, fail-closed account matching, stale-token unbinding,
     * strict admin-only gating, and PII stripping.
     */
    fun handleIncomingFcmMessageData(
        context: Context,
        data: Map<String, String>,
        notificationTitle: String? = null,
        notificationBody: String? = null,
        messageId: String? = null
    ): Boolean {
        val type = data["type"]?.trim().orEmpty()
        val explicitVisibility = data["visibility"]?.trim()?.uppercase().orEmpty()
        val isAdminOrderPayload = explicitVisibility == "ADMIN_ONLY" ||
            type == "admin_order" ||
            data["deepLink"]?.startsWith("bekaservice://admin/orders") == true ||
            data["targetAdminEmail"]?.equals(AuthAndOrderManager.SOLE_ADMIN_EMAIL, ignoreCase = true) == true ||
            data["audience"]?.trim() == "admin_only"

        if (isAdminOrderPayload) {
            // Strictly reject admin order push on logged-out, anonymous, non-admin, or revoked-admin devices!
            if (!AuthAndOrderManager.hasVerifiedAdminClaim(context)) {
                onNonAdminAuthenticated(context)
                val staleToken = getLocalFcmToken(context)
                if (staleToken.length >= 80 && !AuthAndOrderManager.isRegisteredAuthenticatedUser(context)) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        runCatching {
                            FirestoreRepository(context.applicationContext)
                                .unbindUnauthenticatedDeviceTokenOnBackend(staleToken, getOrCreateDeviceId(context))
                        }
                    }
                }
                return false
            }
            val orderId = data["orderId"]?.trim().orEmpty()
            if (orderId.isBlank()) return false
            val orderType = data["orderType"]?.trim()?.takeIf { it == "service_order" } ?: "service_order"
            val notificationId = data["notificationId"]?.trim()?.takeIf { it.isNotBlank() }
                ?: "admin_order_$orderId"
            val rawTitle = data["title"] ?: notificationTitle ?: "ახალი შეკვეთა"
            val rawBody = data["body"] ?: data["message"] ?: notificationBody ?: "შემოვიდა ახალი სერვისის შეკვეთა."
            val deepLink = data["deepLink"]?.trim()?.takeIf { it.startsWith("bekaservice://admin/orders/") }
                ?: "bekaservice://admin/orders/service/$orderId"

            val safeTitle = stripSensitiveDataFromPayload(rawTitle)
            val safeBody = stripSensitiveDataFromPayload(rawBody)

            val record = AdminOrderNotificationRecord(
                notificationId = notificationId,
                orderId = orderId,
                orderType = orderType,
                title = safeTitle,
                body = safeBody,
                deepLink = deepLink,
                targetAdminEmail = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                read = false,
                createdAtMillis = System.currentTimeMillis()
            )

            val current = loadAdminOrderNotificationsInternal(context).toMutableList()
            val idx = current.indexOfFirst { it.notificationId == notificationId || it.orderId == orderId }
            if (idx >= 0) {
                current[idx] = record.copy(read = current[idx].read)
            } else {
                current.add(0, record)
            }
            saveAdminOrderNotificationsInternal(context, current, notify = true)
            return showAdminOrderSystemNotification(context, record, ignoreDeduplication = false)
        }

        // Fail-closed: Incoming non-admin FCM payloads MUST include explicit valid visibility (PUBLIC or PRIVATE_USER)
        if (explicitVisibility != "PUBLIC" && explicitVisibility != "PRIVATE_USER") {
            Log.w("BekaPush", "Dropped FCM payload without explicit valid visibility: '$explicitVisibility'")
            return false
        }

        val notificationId = data["notificationId"]
            ?: data["id"]
            ?: messageId
            ?: "fcm_${System.currentTimeMillis()}"
        val title = stripSensitiveDataFromPayload(data["title"] ?: notificationTitle ?: "BSTORE")
        val body = stripSensitiveDataFromPayload(data["body"] ?: data["message"] ?: notificationBody ?: "")
        val rawTargetDocId = (data["targetProductDocId"] ?: data["productDocId"] ?: "").trim()
        val rawProductIdStr = (data["productId"] ?: "").trim()
        val rawTargetProdIdStr = (data["targetProductId"] ?: "").trim()
        val targetProductId = rawTargetProdIdStr.toIntOrNull()?.takeIf { it > 0 }
            ?: rawProductIdStr.toIntOrNull()?.takeIf { it > 0 }
            ?: 0
        val targetProductDocId = rawTargetDocId.ifBlank {
            if (rawProductIdStr.isNotBlank() && rawProductIdStr.toIntOrNull() == null) rawProductIdStr else ""
        }
        val targetTab = data["targetTab"]?.toIntOrNull()
            ?: if (type == "product" || targetProductDocId.isNotBlank() || targetProductId > 0) 1 else 0
        val resolvedType = type.ifBlank { "announcement" }
        val orderId = data["orderId"]?.trim().orEmpty()
        val orderNumber = data["orderNumber"]?.trim().orEmpty()
        val audience = data["audience"]?.trim().orEmpty()
        val recipientUid = data["recipientUid"]?.trim().orEmpty()
            .ifBlank { if (audience.startsWith("user:")) audience.removePrefix("user:").trim() else "" }

        if (body.isBlank()) return false

        if (explicitVisibility == "PUBLIC") {
            if (recipientUid.isNotBlank() ||
                orderId.isNotBlank() ||
                orderNumber.isNotBlank() ||
                audience.startsWith("user:") ||
                resolvedType !in PUBLIC_SAFE_TYPES
            ) {
                Log.w("BekaPush", "Rejected malformed PUBLIC FCM payload $notificationId containing private fields")
                return false
            }
        }

        if (explicitVisibility == "PRIVATE_USER") {
            val activeUid = AuthAndOrderManager.activeRegisteredUidOrNull(context)
            if (AuthAndOrderManager.isLoggingOut ||
                !AuthAndOrderManager.isRegisteredAuthenticatedUser(context) ||
                activeUid.isNullOrBlank() ||
                recipientUid.isBlank() ||
                recipientUid != activeUid
            ) {
                Log.w("BekaPush", "Rejected PRIVATE_USER FCM payload $notificationId for mismatched/unauthenticated session")
                val staleToken = getLocalFcmToken(context)
                val deviceId = getOrCreateDeviceId(context)
                if (!AuthAndOrderManager.isRegisteredAuthenticatedUser(context) && staleToken.length >= 80) {
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        runCatching {
                            FirestoreRepository(context.applicationContext)
                                .unbindUnauthenticatedDeviceTokenOnBackend(staleToken, deviceId)
                        }
                    }
                }
                return false
            }
        }

        return showNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            body = body,
            targetProductId = targetProductId,
            targetTab = targetTab,
            type = resolvedType,
            persistToHistory = true,
            orderId = orderId,
            orderNumber = orderNumber,
            audience = if (explicitVisibility == "PRIVATE_USER") "user:$recipientUid" else "all",
            visibility = explicitVisibility,
            recipientUid = recipientUid,
            targetProductDocId = targetProductDocId
        )
    }
}

