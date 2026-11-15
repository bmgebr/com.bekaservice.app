package com.example

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.RepairProTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationSecurityAuditTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val app = context.applicationContext as android.app.Application
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        AppPreferencesManager.init(context)
        AppPreferencesManager.setOnboardingCompleted(context, true)
        BekaNotificationManager.markPermissionPrompted(context)
        hasCompletedInitialLaunch = true

        context.getSharedPreferences("bekaservice_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        BekaNotificationManager.clearAllNotifications(context)
        AuthAndOrderManager.logout(context)
    }

    @Test
    fun `Test A - User A creates order and switches to User B receives zero User A notifications`() {
        val userA = AuthAndOrderManager.loginWithGoogle(context, "user.a@bstore.ge", "User A", hasAdminClaim = false)
        val orderA = OrderRecord(
            id = "ord_test_a_1",
            orderNumber = "#BS-1111",
            date = "12 ნოე, 2026",
            items = listOf(OrderItemDetail("iPhone OLED", "Original", 1, "350 ₾", "")),
            totalAmount = "350 ₾",
            status = "მიმდინარე",
            trackingCode = "TRK-A1",
            userId = userA.id
        )

        val privDeliveredToA = BekaNotificationManager.handleIncomingFcmMessageData(
            context = context,
            data = mapOf(
                "notificationId" to "notif_a_order_1",
                "title" to "შეკვეთა #BS-1111 მიღებულია",
                "body" to "თქვენი შეკვეთა დადასტურდა.",
                "type" to "order_update",
                "visibility" to "PRIVATE_USER",
                "recipientUid" to userA.id,
                "orderId" to orderA.id,
                "orderNumber" to orderA.orderNumber
            )
        )
        assertTrue("User A should receive own PRIVATE_USER order push", privDeliveredToA)
        assertEquals(1, BekaNotificationManager.loadLocalNotificationHistory(context).size)

        // Switch to User B
        AuthAndOrderManager.logout(context)
        val userB = AuthAndOrderManager.loginWithGoogle(context, "user.b@bstore.ge", "User B", hasAdminClaim = false)
        assertTrue(userA.id != userB.id)

        // Trigger a new PRIVATE_USER push addressed to User A while User B is signed in
        val leakedToB = BekaNotificationManager.handleIncomingFcmMessageData(
            context = context,
            data = mapOf(
                "notificationId" to "notif_a_order_2",
                "title" to "გადახდა დადასტურდა #BS-1111",
                "body" to "User A payment confirmed",
                "type" to "payment_update",
                "visibility" to "PRIVATE_USER",
                "recipientUid" to userA.id,
                "orderId" to orderA.id,
                "orderNumber" to orderA.orderNumber
            )
        )
        assertFalse("User B must NOT receive User A's PRIVATE_USER notification", leakedToB)

        val historyB = BekaNotificationManager.loadLocalNotificationHistory(context)
        assertFalse(
            "User B local notification history must contain zero User A records",
            historyB.any { it.id.startsWith("notif_a_order") || it.orderId == orderA.id }
        )
        assertEquals(0, BekaNotificationManager.getUnreadNotificationsCount(context, emptyList()))
    }

    @Test
    fun `Test B - Unauthenticated guest after logout sees zero User A private notifications including legacy cache purge`() {
        val userA = AuthAndOrderManager.loginWithGoogle(context, "guest.leak.a@bstore.ge", "User A", hasAdminClaim = false)
        BekaNotificationManager.handleIncomingFcmMessageData(
            context = context,
            data = mapOf(
                "notificationId" to "notif_priv_guest_check",
                "title" to "პირადი შეკვეთა #BS-2020",
                "body" to "შეკვეთა გზაშია",
                "type" to "order_update",
                "visibility" to "PRIVATE_USER",
                "recipientUid" to userA.id,
                "orderId" to "ord_2020"
            )
        )
        assertEquals(1, BekaNotificationManager.loadLocalNotificationHistory(context).size)

        // Seed simulated legacy unscoped shared cache from old buggy version
        val pushPrefs = context.getSharedPreferences("bekaservice_push_prefs", Context.MODE_PRIVATE)
        val legacyArr = """[{"id":"legacy_leak_ord_99","title":"Old Order #BS-99","body":"Secret old order","type":"order_update","orderId":"ord_99"}]"""
        pushPrefs.edit()
            .putString("local_push_history_json", legacyArr)
            .putBoolean("legacy_global_notif_purged_v5", false)
            .commit()

        AuthAndOrderManager.logout(context)
        assertFalse(AuthAndOrderManager.isRegisteredAuthenticatedUser(context))
        assertTrue(AuthAndOrderManager.isGuestSession(context))

        BekaNotificationManager.purgeSyntheticNotificationsIfNeeded(context, force = true)
        val guestHistory = BekaNotificationManager.loadLocalNotificationHistory(context)
        assertTrue("Guest must see ZERO private/legacy notifications", guestHistory.isEmpty())
        assertEquals(0, BekaNotificationManager.getUnreadNotificationsCount(context, emptyList()))

        // Incoming private FCM while unauthenticated must be blocked
        val pushedWhileLoggedOut = BekaNotificationManager.handleIncomingFcmMessageData(
            context = context,
            data = mapOf(
                "notificationId" to "notif_priv_after_logout",
                "title" to "User A Secret",
                "body" to "Should not show",
                "type" to "order_update",
                "visibility" to "PRIVATE_USER",
                "recipientUid" to userA.id,
                "orderId" to "ord_2020"
            )
        )
        assertFalse("Unauthenticated guest must NEVER display User A's private push", pushedWhileLoggedOut)
    }

    @Test
    fun `Test C - Anonymous or unauthenticated session blocked from private records but receives PUBLIC`() {
        val registeredUserId = "uid_registered_customer"
        val anonPrivRecord = PushNotificationRecord(
            id = "priv_for_registered",
            title = "პირადი განახლება",
            body = "თქვენი გადახდა დადასტურდა",
            type = "payment_update",
            visibility = "PRIVATE_USER",
            recipientUid = registeredUserId,
            audience = "user:$registeredUserId",
            orderId = "ord_reg_1"
        )
        val adminRecord = PushNotificationRecord(
            id = "adm_only_1",
            title = "ახალი შეკვეთა",
            body = "საჭიროა დადასტურება",
            type = "admin_order",
            visibility = "ADMIN_ONLY",
            audience = "admin_only"
        )
        val publicRecord = PushNotificationRecord(
            id = "pub_ann_1",
            title = "BSTORE განახლება",
            body = "ახალი აქსესუარები დაემატა!",
            type = "announcement",
            visibility = "PUBLIC",
            audience = "all"
        )

        assertFalse(BekaNotificationManager.isNotificationTargetedToCurrentUser(context, anonPrivRecord))
        assertFalse(BekaNotificationManager.isNotificationTargetedToCurrentUser(context, adminRecord))
        assertTrue(BekaNotificationManager.isNotificationTargetedToCurrentUser(context, publicRecord))

        val visible = BekaNotificationManager.getVisibleUserNotifications(
            context,
            listOf(anonPrivRecord, adminRecord, publicRecord)
        )
        assertEquals(listOf("pub_ann_1"), visible.map { it.id })
    }

    @Test
    fun `Test D - Account switch UI isolation and unread badges`() {
        val userA = AuthAndOrderManager.loginWithGoogle(context, "switch.a@bstore.ge", "Switch A", hasAdminClaim = false)
        val privNotifA = PushNotificationRecord(
            id = "notif_switch_a_1",
            title = "User A შეკვეთა #BS-444",
            body = "თქვენი #BS-444 შეკვეთა მზად არის",
            type = "order_update",
            visibility = "PRIVATE_USER",
            recipientUid = userA.id,
            audience = "user:${userA.id}",
            orderId = "ord_switch_444",
            orderNumber = "#BS-444"
        )
        val publicNotif = PushNotificationRecord(
            id = "notif_public_all_1",
            title = "BSTORE აქცია",
            body = "სპეციალური შეთავაზება ყველასთვის",
            type = "product_offer",
            visibility = "PUBLIC",
            audience = "all"
        )

        BekaNotificationManager.syncCloudNotificationsToLocal(context, listOf(privNotifA, publicNotif))
        assertEquals(2, BekaNotificationManager.getUnreadNotificationsCount(context, listOf(privNotifA, publicNotif)))

        // Switch to User B
        AuthAndOrderManager.logout(context)
        val userB = AuthAndOrderManager.loginWithGoogle(context, "switch.b@bstore.ge", "Switch B", hasAdminClaim = false)
        assertTrue(userA.id != userB.id)

        // Even if stale cloud list still holds User A's record temporarily, User B UI must filter it out completely
        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                UserNotificationsScreen(
                    cloudNotifications = listOf(privNotifA, publicNotif),
                    products = ProductRepository.getAllProducts(),
                    onNavigateToTarget = { _, _, _ -> },
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("notification_history_item_notif_public_all_1").assertIsDisplayed()
        composeTestRule.onNodeWithText("User A შეკვეთა #BS-444").assertDoesNotExist()
        assertEquals(1, BekaNotificationManager.getUnreadNotificationsCount(context, listOf(privNotifA, publicNotif)))
    }

    @Test
    fun `Test H - Deep link hijack protection`() {
        val userA = AuthAndOrderManager.loginWithGoogle(context, "switch.a@bstore.ge", "Switch A", hasAdminClaim = false)
        val privNotifA = PushNotificationRecord(
            id = "notif_switch_a_1",
            title = "User A შეკვეთა #BS-444",
            body = "თქვენი #BS-444 შეკვეთა მზად არის",
            type = "order_update",
            visibility = "PRIVATE_USER",
            recipientUid = userA.id,
            audience = "user:${userA.id}",
            orderId = "ord_switch_444",
            orderNumber = "#BS-444"
        )
        AuthAndOrderManager.logout(context)
        AuthAndOrderManager.loginWithGoogle(context, "switch.b@bstore.ge", "Switch B", hasAdminClaim = false)

        // Attempt Deep Link Hijack: User B opens notification deep link pointing to User A's recipientUid & orderId
        val hijackIntent = BekaNotificationManager.buildDeepLinkIntent(
            context = context,
            notificationId = privNotifA.id,
            title = privNotifA.title,
            body = privNotifA.body,
            targetTab = 4,
            orderId = privNotifA.orderId,
            visibility = "PRIVATE_USER",
            recipientUid = userA.id
        )
        var consumed = false
        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BekaServiceApp(
                    deepLinkIntent = hijackIntent,
                    onDeepLinkConsumed = { consumed = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        assertTrue("Malicious cross-user deep link must be consumed and rejected", consumed)
        assertFalse(
            "Rejected hijack deep link must NOT persist User A notification to User B history",
            BekaNotificationManager.loadLocalNotificationHistory(context).any { it.id == privNotifA.id }
        )
    }

    @Test
    fun `Test E - Multi device token doc scoping and single-device logout preserves other devices`() {
        val uid = "user_multidevice_1"
        val docDev1 = FirestoreRepository.buildDeviceTokenDocId(uid, "dev_phone_1")
        val docDev2 = FirestoreRepository.buildDeviceTokenDocId(uid, "dev_tablet_2")
        assertTrue("Distinct devices under same UID must have distinct document IDs", docDev1 != docDev2)
        assertTrue(docDev1.startsWith("${uid}_"))
        assertTrue(docDev2.startsWith("${uid}_"))

        val user = AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Admin Multi", hasAdminClaim = true)
        assertTrue(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
        assertEquals(AuthAndOrderManager.SOLE_ADMIN_EMAIL, user.email)

        // Register token locally for Device 1 and Device 2
        BekaNotificationManager.registerAdminDeviceToken(context, "fcm_tok_phone_1_1234567890", deviceId = "dev_phone_1")
        BekaNotificationManager.registerAdminDeviceToken(context, "fcm_tok_tablet_2_1234567890", deviceId = "dev_tablet_2")
        assertEquals(2, BekaNotificationManager.getRegisteredAdminDeviceTokens(context).size)

        // Logout ONLY dev_phone_1 -> dev_tablet_2 must remain registered
        BekaNotificationManager.cleanupOnUserLogout(context, specificDeviceId = "dev_phone_1")
        val remaining = BekaNotificationManager.getRegisteredAdminDeviceTokens(context)
        assertEquals(1, remaining.size)
        assertEquals("dev_tablet_2", remaining.first().deviceId)
    }

    @Test
    fun `Test F - Admin notification isolation and immediate revocation`() {
        // 1. Ordinary customer cannot receive ADMIN_ONLY FCM
        AuthAndOrderManager.loginWithGoogle(context, "ordinary.cust@bstore.ge", "Customer", hasAdminClaim = false)
        val deniedToCustomer = BekaNotificationManager.handleIncomingFcmMessageData(
            context = context,
            data = mapOf(
                "notificationId" to "admin_order_999",
                "title" to "ახალი შეკვეთა #BS-999",
                "body" to "შემოვიდა ახალი შეკვეთა",
                "type" to "admin_order",
                "visibility" to "ADMIN_ONLY",
                "audience" to "admin_only",
                "orderId" to "ord_999"
            )
        )
        assertFalse("Ordinary user must NOT receive ADMIN_ONLY notification", deniedToCustomer)

        // 2. Verified Admin receives ADMIN_ONLY push
        AuthAndOrderManager.logout(context)
        AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Admin Beqa", hasAdminClaim = true)
        assertTrue(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))

        val deliveredToAdmin = BekaNotificationManager.handleIncomingFcmMessageData(
            context = context,
            data = mapOf(
                "notificationId" to "admin_order_999",
                "title" to "ახალი შეკვეთა #BS-999",
                "body" to "შემოვიდა ახალი შეკვეთა",
                "type" to "admin_order",
                "visibility" to "ADMIN_ONLY",
                "audience" to "admin_only",
                "orderId" to "ord_999"
            )
        )
        assertTrue("Verified admin must receive ADMIN_ONLY notification", deliveredToAdmin)

        // 3. Revoke admin access -> access immediately blocked
        AuthAndOrderManager.setAdminClaimVerified(context, false)
        assertFalse(AuthAndOrderManager.hasVerifiedAdminClaim(context))
        val deniedAfterRevocation = BekaNotificationManager.handleIncomingFcmMessageData(
            context = context,
            data = mapOf(
                "notificationId" to "admin_order_1000",
                "title" to "ახალი შეკვეთა #BS-1000",
                "body" to "შემოვიდა ახალი შეკვეთა",
                "type" to "admin_order",
                "visibility" to "ADMIN_ONLY",
                "audience" to "admin_only",
                "orderId" to "ord_1000"
            )
        )
        assertFalse("Revoked admin must NOT receive ADMIN_ONLY notification", deniedAfterRevocation)
    }

    @Test
    fun `Test G - Public notification rejects private data and redacts PII`() {
        val malformedPublic = PushNotificationRecord(
            id = "malformed_pub_1",
            title = "Order Leak",
            body = "Leaked order",
            type = "announcement",
            visibility = "PUBLIC",
            audience = "all",
            orderId = "ord_secret_1"
        )
        assertNull(
            "PUBLIC notification carrying orderId must fail fail-closed classification",
            BekaNotificationManager.classifyRecord(malformedPublic)
        )

        val droppedMalformedFcm = BekaNotificationManager.handleIncomingFcmMessageData(
            context = context,
            data = mapOf(
                "notificationId" to "malformed_fcm_1",
                "title" to "Missing Visibility",
                "body" to "Unknown category payload",
                "type" to "unknown_custom_type"
            )
        )
        assertFalse("Incoming non-admin FCM without explicit visibility must be dropped", droppedMalformedFcm)

        val stripped = BekaNotificationManager.stripSensitiveDataFromPayload(
            "IMEI: 356789012345678 | Email: secret@gmail.com | Tel: +995 599 11 22 33"
        )
        assertFalse(stripped.contains("356789012345678"))
        assertFalse(stripped.contains("secret@gmail.com"))
        assertFalse(stripped.contains("599 11 22 33"))
    }

    @Test
    fun `Test I - Token refresh and logout race condition leaves zero stale association`() = runBlocking {
        val user = AuthAndOrderManager.loginWithGoogle(context, "race.user@bstore.ge", "Race User", hasAdminClaim = false)
        BekaNotificationManager.saveLocalFcmToken(context, "old_token_1234567890")
        BekaNotificationManager.upsertLocalNotificationRecord(
            context,
            PushNotificationRecord(
                id = "race_priv_1",
                title = "შეკვეთა #BS-707",
                body = "შეკვეთა შექმნილია",
                type = "order_update",
                visibility = "PRIVATE_USER",
                recipientUid = user.id,
                audience = "user:${user.id}",
                orderId = "ord_race_707"
            )
        )

        // Simulate concurrent logout & token sync race
        AuthAndOrderManager.isLoggingOut = true
        val tokenWhileLoggingOut = BekaNotificationManager.syncFcmTokenAndSubscribeTopic(context, null)
        assertNull("Token sync while logging out must remain local-only / never bind a stale Firebase user", AuthAndOrderManager.activeRegisteredUidOrNull(context))

        // Finish logoutSafely cleanly (resets isLoggingOut back to false and clears private state)
        AuthAndOrderManager.logoutSafely(context)
        assertFalse(AuthAndOrderManager.isLoggingOut)
        assertNull(AuthAndOrderManager.activeRegisteredUidOrNull(context))
        assertTrue(BekaNotificationManager.loadLocalNotificationHistory(context).isEmpty())
        assertEquals(tokenWhileLoggingOut ?: "old_token_1234567890", BekaNotificationManager.getLocalFcmToken(context))
    }

    @Test
    fun `Test J - All notification categories isolation and lockscreen VISIBILITY_PRIVATE enforcement`() {
        val userA = AuthAndOrderManager.loginWithGoogle(context, "cat.a@bstore.ge", "Cat A", hasAdminClaim = false)
        val categoriesPrivate = listOf(
            "order_update",
            "payment_update",
            "payment_confirmed",
            "account_update",
            "private_message"
        )
        categoriesPrivate.forEachIndexed { idx, catType ->
            val rec = PushNotificationRecord(
                id = "cat_priv_$idx",
                title = "Private $catType",
                body = "Private details for User A ($catType)",
                type = catType,
                visibility = "PRIVATE_USER",
                recipientUid = userA.id,
                audience = "user:${userA.id}"
            )
            assertEquals(NotificationVisibility.PRIVATE_USER, BekaNotificationManager.classifyRecord(rec))
            assertTrue(BekaNotificationManager.isNotificationTargetedToCurrentUser(context, rec))
        }

        // Trigger system notification for PRIVATE_USER and inspect lockscreen privacy + publicVersion
        val postedPrivate = BekaNotificationManager.showNotification(
            context = context,
            notificationId = "lockscreen_priv_test_1",
            title = "გადახდა დადასტურდა #BS-5555",
            body = "თქვენი 450 ლარის შეკვეთა დადასტურდა",
            type = "payment_update",
            orderId = "ord_5555",
            orderNumber = "#BS-5555",
            audience = "user:${userA.id}",
            visibility = "PRIVATE_USER",
            recipientUid = userA.id
        )
        assertTrue(postedPrivate)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val postedNotif = Shadows.shadowOf(nm).allNotifications.find {
            it.channelId == BekaNotificationManager.PRIVATE_CHANNEL_ID
        }
        assertNotNull("PRIVATE_USER notification must use PRIVATE_CHANNEL_ID", postedNotif)
        assertEquals(Notification.VISIBILITY_PRIVATE, postedNotif!!.visibility)
        assertNotNull("PRIVATE_USER notification must carry neutral publicVersion for lockscreen", postedNotif.publicVersion)
        assertEquals(
            "თქვენ გაქვთ ახალი შეტყობინება BSTORE-ში.",
            postedNotif.publicVersion.extras.getString(Notification.EXTRA_TEXT)
        )

        // All PUBLIC categories visible as PUBLIC without private fields
        val categoriesPublic = listOf(
            "product_offer",
            "promotion",
            "product_restock",
            "announcement",
            "system_info",
            "maintenance"
        )
        AuthAndOrderManager.logout(context)
        categoriesPublic.forEachIndexed { idx, pubType ->
            val pubRec = PushNotificationRecord(
                id = "cat_pub_$idx",
                title = "Public $pubType",
                body = "Public announcement",
                type = pubType,
                visibility = "PUBLIC",
                audience = "all"
            )
            assertEquals(NotificationVisibility.PUBLIC, BekaNotificationManager.classifyRecord(pubRec))
            assertTrue("Guest can view PUBLIC $pubType", BekaNotificationManager.isNotificationTargetedToCurrentUser(context, pubRec))
        }
        // Private categories must all be blocked for guest
        categoriesPrivate.forEachIndexed { idx, catType ->
            val privRec = PushNotificationRecord(
                id = "cat_priv_guest_$idx",
                title = "Private $catType",
                body = "Private details",
                type = catType,
                visibility = "PRIVATE_USER",
                recipientUid = userA.id,
                audience = "user:${userA.id}"
            )
            assertFalse("Guest must NOT view PRIVATE_USER $catType", BekaNotificationManager.isNotificationTargetedToCurrentUser(context, privRec))
        }
    }
}
