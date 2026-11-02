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
        val deviceId = BekaNotificationManager.getOrCreateDeviceId(applicationContext)
        if (AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(applicationContext)) {
            BekaNotificationManager.registerAdminDeviceToken(applicationContext, token, deviceId)
        }
        if (runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() != null) {
            serviceScope.launch {
                try {
                    val repo = FirestoreRepository(applicationContext)
                    repo.saveCurrentUserFcmToken(
                        token = token,
                        notificationsEnabled = BekaNotificationManager.hasNotificationPermission(applicationContext),
                        deviceId = deviceId
                    )
                    if (AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(applicationContext)) {
                        repo.registerAdminDeviceFcmToken(
                            token = token,
                            deviceId = deviceId
                        )
                    }
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

        BekaNotificationManager.handleIncomingFcmMessageData(
            context = applicationContext,
            data = data,
            notificationTitle = notificationPayload?.title,
            notificationBody = notificationPayload?.body,
            messageId = remoteMessage.messageId
        )
    }
}
