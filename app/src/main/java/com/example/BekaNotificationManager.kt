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

data class PushNotificationRecord(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "announcement",
    val targetProductId: Int = 0,
    val targetTab: Int = 0,
    val senderEmail: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val audience: String = "all",
    val status: String = "submitted"
)

object BekaNotificationManager {
    const val CHANNEL_ID = "bekaservice_push_channel"
    const val CHANNEL_NAME = "BEKASERVICE შეტყობინებები"
    const val CHANNEL_DESCRIPTION = "BEKASERVICE ოფიციალური სიახლეები, შეთავაზებები და სერვისის შეტყობინებები"
    const val ADMIN_ORDERS_CHANNEL_ID = "beka_orders_channel"
    const val ADMIN_ORDERS_CHANNEL_NAME = "BEKASERVICE ადმინის შეკვეთები"
    const val ADMIN_ORDERS_CHANNEL_DESCRIPTION = "ახალი IMEI და სერვისის შეკვეთების შეტყობინებები მხოლოდ ავტორიზებული ადმინისტრატორისთვის"
    const val TOPIC_ANNOUNCEMENTS = "bekaservice_announcements"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TARGET_PRODUCT_ID = "extra_target_product_id"
    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val EXTRA_NOTIFICATION_TITLE = "extra_notification_title"
    const val EXTRA_NOTIFICATION_BODY = "extra_notification_body"

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
    private const val KEY_INITIAL_SNAPSHOT_BASELINE_TS = "initial_snapshot_baseline_ts"
    private const val KEY_PERMISSION_PROMPTED = "post_notifications_prompted"
    private const val KEY_FAKE_PURGED_V2 = "fake_welcome_notifications_purged_v2"
    private const val KEY_FAKE_PURGED_V3 = "fake_welcome_notifications_purged_v3"
    private const val KEY_FAKE_PURGED_V4 = "fake_welcome_notifications_purged_v4"

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
        if (cleanTitle in listOf("test", "ტესტი", "testing", "ტესტ", "bekaservice ტესტი")) {
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
            2 -> "🛠️ დიაგნოსტიკა"
            3 -> "🤖 AI ასისტენტი"
            4 -> "👤 პროფილი"
            5 -> "🛒 კალათა"
            6 -> "🔔 შეტყობინებები"
            7 -> "📲 განახლება"
            else -> "🏠 მთავარი გვერდი"
        }
    }

    fun notificationTypeLabel(type: String): String {
        return when (type) {
            "product_offer" -> "🔥 პროდუქტის აქცია"
            "service_update" -> "🛠️ სერვისის სიახლე"
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        val appEnabled = try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (_: Exception) {
            true
        }
        if (!appEnabled) return false
        return isNotificationChannelEnabled(context, CHANNEL_ID)
    }

    fun getDeliveryDiagnostics(
        context: Context,
        manufacturerOverride: String? = null
    ): NotificationDeliveryDiagnostics {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val appEnabled = try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
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
                    "გახსენით Settings -> Apps -> Manage apps -> BEKASERVICE -> ჩართეთ Autostart (Background autostart).",
                    "გადადით Battery saver / Battery optimization-ში და აირჩიეთ No restrictions (შეზღუდვის გარეშე).",
                    "Notifications განყოფილებაში ჩართეთ Lock screen და Floating notifications.",
                    "შენიშვნა: აპლიკაციის ჩვეულებრივ დახურვისას (Swipe) შეტყობინებები მოდის, მაგრამ Settings-იდან Force Stop-ის დაჭერა სრულად თიშავს ფონურ მიწოდებას აპის ხელახლა გახსნამდე."
                )
            }
            lowerMfr.contains("samsung") -> {
                "Samsung (One UI)" to listOf(
                    "გახსენით Settings -> Apps -> BEKASERVICE -> Battery და აირჩიეთ Unrestricted (შეუზღუდავი).",
                    "შეამოწმეთ Settings -> Battery -> Background usage limits და დარწმუნდით, რომ BEKASERVICE არ არის Sleeping ან Deep sleeping apps სიაში.",
                    "დარწმუნდით, რომ Data Saver რეჟიმში BEKASERVICE-ს აქვს ფონური ინტერნეტის უფლება."
                )
            }
            lowerMfr.contains("motorola") || lowerMfr.contains("lenovo") -> {
                "Motorola / Lenovo" to listOf(
                    "გახსენით Settings -> Apps -> See all apps -> BEKASERVICE -> App battery usage და აირჩიეთ Unrestricted.",
                    "შეამოწმეთ Adaptive battery და ჩართეთ Background data (ფონური მონაცემები)."
                )
            }
            lowerMfr.contains("huawei") || lowerMfr.contains("honor") -> {
                "Huawei / Honor" to listOf(
                    "გახსენით Settings -> Apps -> App launch -> BEKASERVICE -> გამორთეთ Manage automatically და ჩართეთ Auto-launch, Secondary launch და Run in background."
                )
            }
            else -> {
                "Android ფონური შეტყობინებების რეკომენდაცია ($rawMfr)" to listOf(
                    "გახსენით Settings -> Apps -> BEKASERVICE -> Battery და აირჩიეთ Unrestricted (შეზღუდვის გარეშე).",
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
        prefs.edit().putBoolean(KEY_PERMISSION_PROMPTED, true).apply()
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

    fun hasDeliveredNotification(context: Context, notificationId: String): Boolean {
        if (notificationId.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DELIVERED_IDS, "") ?: ""
        return raw.split(",").any { it == notificationId }
    }

    fun markNotificationDelivered(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getString(KEY_DELIVERED_IDS, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableList()
        if (!current.contains(notificationId)) {
            current.add(notificationId)
        }
        val trimmed = current.takeLast(150).joinToString(",")
        prefs.edit().putString(KEY_DELIVERED_IDS, trimmed).apply()
    }

    fun clearDeliveredFlag(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getString(KEY_DELIVERED_IDS, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() && it != notificationId }
        prefs.edit().putString(KEY_DELIVERED_IDS, current.joinToString(",")).apply()
    }

    // ==========================================================================
    // წაკითხული / წაუკითხავი სტატუსის და ლოკალური ისტორიის მართვა
    // ==========================================================================
    fun isNotificationRead(context: Context, notificationId: String): Boolean {
        if (notificationId.isBlank()) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_READ_IDS, "") ?: ""
        return raw.split(",").any { it == notificationId }
    }

    fun markNotificationRead(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        markNotificationDelivered(context, notificationId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getString(KEY_READ_IDS, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
        if (current.add(notificationId)) {
            prefs.edit().putString(KEY_READ_IDS, current.take(300).joinToString(",")).apply()
            notifyStateChanged()
        }
    }

    fun markNotificationUnread(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_READ_IDS, "") ?: ""
        val current = raw
            .split(",")
            .filter { it.isNotBlank() && it != notificationId }
        val updated = current.joinToString(",")
        if (updated != raw) {
            prefs.edit().putString(KEY_READ_IDS, updated).apply()
            notifyStateChanged()
        }
    }

    fun markAllNotificationsRead(context: Context, notifications: List<PushNotificationRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getString(KEY_READ_IDS, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
        notifications.forEach { if (it.id.isNotBlank()) current.add(it.id) }
        prefs.edit().putString(KEY_READ_IDS, current.take(400).joinToString(",")).apply()
        notifyStateChanged()
    }

    fun isDismissedByUser(context: Context, notificationId: String): Boolean {
        if (notificationId.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DISMISSED_USER_IDS, "") ?: ""
        return raw.split(",").any { it == notificationId }
    }

    fun dismissNotificationForUser(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getString(KEY_DISMISSED_USER_IDS, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableSet()
        current.add(notificationId)
        prefs.edit().putString(KEY_DISMISSED_USER_IDS, current.take(300).joinToString(",")).apply()
        markNotificationRead(context, notificationId)
        notifyStateChanged()
    }

    fun unDismissNotificationForUser(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_DISMISSED_USER_IDS, "") ?: ""
        val current = raw
            .split(",")
            .filter { it.isNotBlank() && it != notificationId }
        val updated = current.joinToString(",")
        if (updated != raw) {
            prefs.edit().putString(KEY_DISMISSED_USER_IDS, updated).apply()
            notifyStateChanged()
        }
    }

    /**
     * ასუფთავებს ძველ სატესტო/მისასალმებელ ჩანაწერებს, რათა ისტორიაში მხოლოდ რეალურად
     * გაგზავნილი ან მიღებული შეტყობინებები გამოჩნდეს.
     */
    fun purgeSyntheticNotificationsIfNeeded(context: Context, force: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!force && prefs.getBoolean(KEY_FAKE_PURGED_V4, false)) return
        val cleaned = loadLocalNotificationHistory(context).filterNot {
            isSyntheticOrTestNotification(it.id, it.title, it.body)
        }
        saveLocalNotificationHistoryList(context, cleaned, notify = false)
        prefs.edit().putBoolean(KEY_FAKE_PURGED_V4, true).apply()
    }

    fun clearAllNotifications(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_LOCAL_HISTORY_JSON)
            .remove(KEY_READ_IDS)
            .remove(KEY_DISMISSED_USER_IDS)
            .apply()
        notifyStateChanged()
    }

    fun loadLocalNotificationHistory(context: Context): List<PushNotificationRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LOCAL_HISTORY_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<PushNotificationRecord>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val title = obj.optString("title", "").trim()
                val body = obj.optString("body", "").trim()
                if (id.isBlank() || title.isBlank() || isSyntheticOrTestNotification(id, title, body)) continue
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
                        audience = obj.optString("audience", "all"),
                        status = obj.optString("status", "submitted")
                    )
                )
            }
            list.sortedByDescending { it.createdAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLocalNotificationHistoryList(
        context: Context,
        list: List<PushNotificationRecord>,
        notify: Boolean = true
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        list
            .filterNot { it.id.isBlank() || isSyntheticOrTestNotification(it.id, it.title, it.body) }
            .distinctBy { it.id }
            .sortedByDescending { it.createdAtMillis }
            .take(80)
            .forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("body", item.body)
                    put("type", item.type)
                    put("targetProductId", item.targetProductId)
                    put("targetTab", item.targetTab)
                    put("senderEmail", item.senderEmail)
                    put("createdAtMillis", item.createdAtMillis)
                    put("updatedAtMillis", item.updatedAtMillis)
                    put("audience", item.audience)
                    put("status", item.status)
                }
                arr.put(obj)
            }
        val serialized = arr.toString()
        val previous = prefs.getString(KEY_LOCAL_HISTORY_JSON, null)
        if (serialized != previous) {
            prefs.edit().putString(KEY_LOCAL_HISTORY_JSON, serialized).apply()
            if (notify) {
                notifyStateChanged()
            }
        }
    }

    fun upsertLocalNotificationRecord(context: Context, record: PushNotificationRecord) {
        if (record.id.isBlank() || isSyntheticOrTestNotification(record.id, record.title, record.body)) return
        unDismissNotificationForUser(context, record.id)
        val current = loadLocalNotificationHistory(context).toMutableList()
        val existingIdx = current.indexOfFirst { it.id == record.id }
        if (existingIdx >= 0) {
            current[existingIdx] = record
        } else {
            current.add(0, record)
        }
        saveLocalNotificationHistoryList(context, current, notify = true)
    }

    fun removeLocalNotificationRecord(context: Context, notificationId: String) {
        if (notificationId.isBlank()) return
        val current = loadLocalNotificationHistory(context).filterNot { it.id == notificationId }
        saveLocalNotificationHistoryList(context, current, notify = true)
    }

    fun mergeCloudAndLocalNotifications(
        context: Context,
        cloudList: List<PushNotificationRecord>
    ): List<PushNotificationRecord> {
        val byId = LinkedHashMap<String, PushNotificationRecord>()
        // 1. Cloud-იდან წამოსული რეალური შეტყობინებები წარმოადგენს მთავარ წყაროს
        cloudList.forEach { n ->
            if (n.id.isNotBlank() && !isSyntheticOrTestNotification(n.id, n.title, n.body)) {
                byId[n.id] = n
            }
        }
        // 2. ლოკალური ისტორიიდან ვამატებთ მხოლოდ FCM ფონურ შეტყობინებებს (fcm_*)
        val localList = loadLocalNotificationHistory(context)
        localList.forEach { local ->
            if (local.id.isNotBlank() && !isSyntheticOrTestNotification(local.id, local.title, local.body)) {
                if ((local.id.startsWith("fcm_") || local.id.startsWith("imei_notif_")) && !byId.containsKey(local.id)) {
                    byId[local.id] = local
                }
            }
        }
        return byId.values.sortedByDescending { it.createdAtMillis }
    }

    /**
     * სინქრონიზაციას უკეთებს Firestore-ის რეალურ შეტყობინებებს და FCM-ით ფონურ რეჟიმში
     * მიღებულ ჩანაწერებს. თუ Cloud-იდან წაშლილია ან ცარიელია, ლოკალური ghost ჩანაწერები იწმინდება.
     */
    fun syncCloudNotificationsToLocal(context: Context, cloudList: List<PushNotificationRecord>): List<PushNotificationRecord> {
        purgeSyntheticNotificationsIfNeeded(context, force = true)
        val cloudIds = cloudList.map { it.id }.toSet()
        // ფონურად FCM-ით მოსული ჩანაწერები (fcm_*) და IMEI სტატუსის შეტყობინებები (imei_notif_*) შევინარჩუნოთ
        val retainedFcmOnly = loadLocalNotificationHistory(context).filter { local ->
            (local.id.startsWith("fcm_") || local.id.startsWith("imei_notif_")) && !cloudIds.contains(local.id) && !isSyntheticOrTestNotification(local.id, local.title, local.body)
        }
        val byId = LinkedHashMap<String, PushNotificationRecord>()
        cloudList.forEach { n ->
            if (n.id.isNotBlank() && !isSyntheticOrTestNotification(n.id, n.title, n.body)) {
                byId[n.id] = n
            }
        }
        retainedFcmOnly.forEach { fcmRecord ->
            if (!byId.containsKey(fcmRecord.id)) {
                byId[fcmRecord.id] = fcmRecord
            }
        }
        val reconciled = byId.values.sortedByDescending { it.createdAtMillis }
        saveLocalNotificationHistoryList(context, reconciled, notify = true)

        // მკვდარი ID-ების გასუფთავება read_ids სიიდან
        val validIds = reconciled.map { it.id }.toSet()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentReadIds = (prefs.getString(KEY_READ_IDS, "") ?: "")
            .split(",")
            .filter { it.isNotBlank() && validIds.contains(it) }
        prefs.edit().putString(KEY_READ_IDS, currentReadIds.joinToString(",")).apply()

        notifyStateChanged()
        return reconciled
    }

    fun getVisibleUserNotifications(
        context: Context,
        cloudList: List<PushNotificationRecord>
    ): List<PushNotificationRecord> {
        val merged = mergeCloudAndLocalNotifications(context, cloudList)
        return merged.filterNot { isDismissedByUser(context, it.id) }
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
        targetTab: Int = 0
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_NOTIFICATION_TITLE, title)
            putExtra(EXTRA_NOTIFICATION_BODY, body)
            putExtra(EXTRA_TARGET_PRODUCT_ID, targetProductId)
            putExtra(EXTRA_TARGET_TAB, targetTab)
        }
    }

    @Volatile
    private var cachedLargeAppIcon: android.graphics.Bitmap? = null

    private fun loadAppIconBitmap(context: Context): android.graphics.Bitmap? {
        cachedLargeAppIcon?.let { if (!it.isRecycled) return it }
        return try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = 2
            }
            val raw = android.graphics.BitmapFactory.decodeResource(
                context.applicationContext.resources,
                R.drawable.ic_beka_main_emblem,
                options
            ) ?: return null
            val size = 192
            val scaled = android.graphics.Bitmap.createScaledBitmap(raw, size, size, true)
            val output = android.graphics.Bitmap.createBitmap(
                size,
                size,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val rect = android.graphics.Rect(0, 0, size, size)
            val rectF = android.graphics.RectF(rect)
            val cornerRadius = size * 0.22f
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(scaled, rect, rect, paint)
            cachedLargeAppIcon = output
            output
        } catch (e: Exception) {
            Log.w("BekaPush", "Could not decode large app icon bitmap: ${e.message}")
            null
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
        persistToHistory: Boolean = true
    ): Boolean {
        val cleanTitle = title.trim().ifBlank { "BEKASERVICE" }
        val cleanBody = body.trim()
        if (cleanBody.isBlank()) return false

        ensureNotificationChannels(context)

        val effectiveId = notificationId.trim().ifBlank { "notif_${cleanTitle.hashCode()}_${cleanBody.hashCode()}" }

        // მხოლოდ რეალური შეტყობინებები შევინახოთ ისტორიაში (არა ლოკალური სატესტო preview)
        if (persistToHistory && !isSyntheticOrTestNotificationId(effectiveId)) {
            upsertLocalNotificationRecord(
                context,
                PushNotificationRecord(
                    id = effectiveId,
                    title = cleanTitle,
                    body = cleanBody,
                    type = type,
                    targetProductId = targetProductId,
                    targetTab = targetTab,
                    senderEmail = "beqa1994@gmail.com",
                    createdAtMillis = System.currentTimeMillis()
                )
            )
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

        val tapIntent = buildDeepLinkIntent(
            context = context,
            notificationId = effectiveId,
            title = cleanTitle,
            body = cleanBody,
            targetProductId = targetProductId,
            targetTab = targetTab
        )

        val requestCode = effectiveId.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeAppIcon = loadAppIconBitmap(context)
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (largeAppIcon != null) {
            notificationBuilder.setLargeIcon(largeAppIcon)
        }

        val notification = notificationBuilder.build()

        return try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
            markNotificationDelivered(context, effectiveId)
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
        if (!token.isNullOrBlank()) {
            saveLocalFcmToken(context, token)
            if (runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() != null) {
                firestoreRepo?.saveCurrentUserFcmToken(
                    token = token,
                    notificationsEnabled = hasNotificationPermission(context),
                    deviceId = deviceId
                )
            }
            if (AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
                registerAdminDeviceToken(context, token, deviceId)
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
            .filterNot { it.deviceId == safeDeviceId || it.token == cleanToken }
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

    /**
     * Called when a regular (non-admin) customer logs in on this device.
     * Immediately removes this device's token from the admin token registry, clears any displayed
     * admin notifications, and registers the customer's FCM token in `/fcm_tokens`.
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
        if (runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() != null) {
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
     * Unregisters the device's FCM token from `/admin_fcm_tokens` and `/fcm_tokens` BEFORE
     * `FirebaseAuth.getInstance().signOut()` clears the session so a logged-out device never
     * receives administrator or previous-user push notifications.
     */
    fun cleanupOnUserLogout(context: Context, specificDeviceId: String? = null) {
        val targetDeviceId = specificDeviceId?.takeIf { it.isNotBlank() } ?: getOrCreateDeviceId(context)
        val localToken = getLocalFcmToken(context)
        val currentUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            ?: AuthAndOrderManager.getCurrentUser(context)?.id.orEmpty()
        val currentTokens = loadAllAdminDeviceTokensInternal(context)
        val removedTokens = currentTokens.filter {
            it.deviceId == targetDeviceId || (specificDeviceId == null && localToken.isNotBlank() && it.token == localToken)
        }
        val remaining = currentTokens.filterNot { item -> removedTokens.any { it.tokenId == item.tokenId } }
        saveAllAdminDeviceTokensInternal(context, remaining)

        if (runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() != null) {
            val repo = runCatching { FirestoreRepository(context) }.getOrNull()
            removedTokens.forEach { rec ->
                repo?.enqueueUnregisterDeviceFcmTokensBeforeSignOut(rec.token, rec.deviceId, currentUid)
            }
            if (localToken.isNotBlank() || currentUid.isNotBlank()) {
                repo?.enqueueUnregisterDeviceFcmTokensBeforeSignOut(localToken, targetDeviceId, currentUid)
            }
        }
        runCatching { NotificationManagerCompat.from(context).cancelAll() }
        notifyAdminNotificationsChanged()
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
            if (record.orderType == "service_order") {
                "bekaservice://admin/orders/service/${record.orderId}"
            } else {
                "bekaservice://admin/orders/imei/${record.orderId}"
            }
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
        val deepLinkUri = if (orderType == "service_order") {
            "bekaservice://admin/orders/service/$orderId"
        } else {
            "bekaservice://admin/orders/imei/$orderId"
        }
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
            if (record.orderType == "imei_check") "ახალი IMEI შეკვეთა" else "ახალი შეკვეთა"
        }
        val safeBody = stripSensitiveDataFromPayload(record.body).ifBlank {
            if (record.orderType == "imei_check") {
                "შემოვიდა ახალი IMEI შემოწმების მოთხოვნა."
            } else {
                "შემოვიდა ახალი სერვისის შეკვეთა."
            }
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
        orderType: String
    ): AdminOrderNotificationRecord {
        val cleanOrderId = orderId.trim()
        val normalizedType = if (orderType == "service_order") "service_order" else "imei_check"
        val notificationId = "admin_order_$cleanOrderId"
        val safeTitle = if (normalizedType == "imei_check") {
            "ახალი IMEI შეკვეთა"
        } else {
            "ახალი შეკვეთა"
        }
        val safeBody = if (normalizedType == "imei_check") {
            "შემოვიდა ახალი IMEI შემოწმების მოთხოვნა."
        } else {
            "შემოვიდა ახალი სერვისის შეკვეთა."
        }
        val deepLink = if (normalizedType == "imei_check") {
            "bekaservice://admin/orders/imei/$cleanOrderId"
        } else {
            "bekaservice://admin/orders/service/$cleanOrderId"
        }

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
     * Enforces strict admin-only gating for `admin_order` messages and strips any sensitive data.
     */
    fun handleIncomingFcmMessageData(
        context: Context,
        data: Map<String, String>,
        notificationTitle: String? = null,
        notificationBody: String? = null,
        messageId: String? = null
    ): Boolean {
        val type = data["type"]?.trim().orEmpty()
        val isAdminOrderPayload = type == "admin_order" ||
            data.containsKey("orderId") ||
            data["deepLink"]?.startsWith("bekaservice://admin/orders") == true ||
            data["targetAdminEmail"]?.equals(AuthAndOrderManager.SOLE_ADMIN_EMAIL, ignoreCase = true) == true

        if (isAdminOrderPayload) {
            // Strictly reject admin order push on logged-out or non-admin devices!
            if (!AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)) {
                onNonAdminAuthenticated(context)
                return false
            }
            val orderId = data["orderId"]?.trim().orEmpty()
            if (orderId.isBlank()) return false
            val orderType = data["orderType"]?.trim()?.takeIf { it == "service_order" || it == "imei_check" }
                ?: if (orderId.startsWith("imei_")) "imei_check" else "service_order"
            val notificationId = data["notificationId"]?.trim()?.takeIf { it.isNotBlank() }
                ?: "admin_order_$orderId"
            val rawTitle = data["title"] ?: notificationTitle ?: if (orderType == "imei_check") "ახალი IMEI შეკვეთა" else "ახალი შეკვეთა"
            val rawBody = data["body"] ?: data["message"] ?: notificationBody
                ?: if (orderType == "imei_check") "შემოვიდა ახალი IMEI შემოწმების მოთხოვნა." else "შემოვიდა ახალი სერვისის შეკვეთა."
            val deepLink = data["deepLink"]?.trim()?.takeIf { it.startsWith("bekaservice://admin/orders/") }
                ?: if (orderType == "service_order") "bekaservice://admin/orders/service/$orderId" else "bekaservice://admin/orders/imei/$orderId"

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
            return showAdminOrderSystemNotification(context, record)
        }

        // Standard public/customer announcement or service status update
        val notificationId = data["notificationId"]
            ?: data["id"]
            ?: messageId
            ?: "fcm_${System.currentTimeMillis()}"
        val title = stripSensitiveDataFromPayload(data["title"] ?: notificationTitle ?: "BEKASERVICE")
        val body = stripSensitiveDataFromPayload(data["body"] ?: data["message"] ?: notificationBody ?: "")
        val targetProductId = (data["targetProductId"] ?: data["productId"])?.toIntOrNull() ?: 0
        val targetTab = data["targetTab"]?.toIntOrNull() ?: 0
        val resolvedType = type.ifBlank { "announcement" }

        if (body.isBlank()) return false
        return showNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            body = body,
            targetProductId = targetProductId,
            targetTab = targetTab,
            type = resolvedType,
            persistToHistory = true
        )
    }
}

