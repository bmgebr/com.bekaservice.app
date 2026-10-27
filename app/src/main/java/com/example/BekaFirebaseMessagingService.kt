package com.example

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BekaFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("BekaPush", "FCM registration token refreshed")
        BekaNotificationManager.saveLocalFcmToken(applicationContext, token)
        if (FirebaseAuth.getInstance().currentUser != null) {
            serviceScope.launch {
                try {
                    val repo = FirestoreRepository(applicationContext)
                    repo.saveCurrentUserFcmToken(
                        token = token,
                        notificationsEnabled = BekaNotificationManager.hasNotificationPermission(applicationContext)
                    )
                } catch (e: Exception) {
                    Log.w("BekaPush", "Failed to sync refreshed FCM token to Firestore: ${e.message}")
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val notificationPayload = remoteMessage.notification

        val notificationId = data["notificationId"]
            ?: data["id"]
            ?: remoteMessage.messageId
            ?: "fcm_${System.currentTimeMillis()}"

        val title = data["title"]
            ?: notificationPayload?.title
            ?: "BEKASERVICE"

        val body = data["body"]
            ?: data["message"]
            ?: notificationPayload?.body
            ?: ""

        val targetProductId = (data["targetProductId"] ?: data["productId"])?.toIntOrNull() ?: 0
        val targetTab = data["targetTab"]?.toIntOrNull() ?: 0
        val type = data["type"]?.takeIf { it.isNotBlank() } ?: "announcement"

        if (body.isNotBlank()) {
            BekaNotificationManager.showNotification(
                context = applicationContext,
                notificationId = notificationId,
                title = title,
                body = body,
                targetProductId = targetProductId,
                targetTab = targetTab,
                type = type,
                persistToHistory = true
            )
        }
    }
}

