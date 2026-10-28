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
    const val TOPIC_ANNOUNCEMENTS = "bekaservice_announcements"

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TARGET_PRODUCT_ID = "extra_target_product_id"
    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val EXTRA_NOTIFICATION_TITLE = "extra_notification_title"
    const val EXTRA_NOTIFICATION_BODY = "extra_notification_body"

    private const val PREFS_NAME = "bekaservice_push_prefs"
    private const val KEY_FCM_TOKEN = "fcm_registration_token"
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

    private fun notifyStateChanged() {
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
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return try {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (_: Exception) {
            true
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
        if (token.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token.trim()).apply()
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
                if (local.id.startsWith("fcm_") && !byId.containsKey(local.id)) {
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
        // ფონურად FCM-ით მოსული ჩანაწერები (fcm_*) შევინარჩუნოთ, ხოლო Cloud-ში წაშლილი notif_* ჩანაწერები ამოვშალოთ
        val retainedFcmOnly = loadLocalNotificationHistory(context).filter { local ->
            local.id.startsWith("fcm_") && !cloudIds.contains(local.id) && !isSyntheticOrTestNotification(local.id, local.title, local.body)
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
            getLocalFcmToken(context).takeIf { it.isNotBlank() }
        }

        if (!token.isNullOrBlank()) {
            saveLocalFcmToken(context, token)
            if (FirebaseAuth.getInstance().currentUser != null) {
                firestoreRepo?.saveCurrentUserFcmToken(
                    token = token,
                    notificationsEnabled = hasNotificationPermission(context)
                )
            }
        }
        return token
    }
}

