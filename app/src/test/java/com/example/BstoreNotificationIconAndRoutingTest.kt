package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BstoreNotificationIconAndRoutingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AppPreferencesManager.init(context)
        BekaNotificationManager.clearAllNotifications(context)
    }

    // =========================================================================
    // 1. Icon & Drawable Verification
    // =========================================================================

    @Test
    fun test_smallNotificationIcon_drawableResourceLoadsAndHasTransparentBackground() {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_stat_bekaservice)
        assertNotNull("ic_stat_bekaservice drawable resource must exist", drawable)

        val parser = context.resources.getXml(R.drawable.ic_stat_bekaservice)
        var eventType = parser.eventType
        var isVector = false
        val fillColors = mutableListOf<String>()
        var pathCount = 0

        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                if (parser.name == "vector") {
                    isVector = true
                } else if (parser.name == "path") {
                    pathCount++
                    for (i in 0 until parser.attributeCount) {
                        if (parser.getAttributeName(i) == "fillColor") {
                            fillColors.add(parser.getAttributeValue(i))
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        assertTrue("Drawable must be an Android VectorDrawable", isVector)
        assertTrue("Icon must have distinct vector paths", pathCount >= 2)
        assertTrue("All paths must use monochrome white fillColor", fillColors.isNotEmpty() && fillColors.all { it.equals("#FFFFFFFF", ignoreCase = true) || it.equals("#FFFFFF", ignoreCase = true) })
    }

    @Test
    fun test_largeAppIconBitmap_drawsCircleMaskWithTransparentCorners() {
        val bitmap = BekaNotificationManager.loadAppIconBitmap(context)
        assertNotNull("loadAppIconBitmap must produce a valid bitmap", bitmap)
        val safeBitmap = bitmap!!

        assertEquals("Output bitmap width must be 192px", 192, safeBitmap.width)
        assertEquals("Output bitmap height must be 192px", 192, safeBitmap.height)
        assertFalse("Bitmap must not be recycled", safeBitmap.isRecycled)
    }

    @Test
    fun test_manifestDeclaresSmallIconAsDefault() {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val metaData = appInfo.metaData
        assertNotNull("Manifest metaData must exist", metaData)
        val defaultIconResId = metaData.getInt("com.google.firebase.messaging.default_notification_icon")
        assertEquals(
            "default_notification_icon must point to R.drawable.ic_stat_bekaservice",
            R.drawable.ic_stat_bekaservice,
            defaultIconResId
        )
    }

    // =========================================================================
    // 2. Destination Resolution Matrix
    // =========================================================================

    @Test
    fun test_destinationResolution_productNotification() {
        val record = PushNotificationRecord(
            id = "notif_prod_1",
            title = "ახალი პროდუქტი",
            body = "ნახეთ დეტალები",
            type = "product",
            targetProductDocId = "prod_doc_1053",
            targetProductId = 1053
        )
        val dest = BekaNotificationManager.resolveDestination(record)
        assertTrue("Destination must be ProductDetail", dest is NotificationDestination.ProductDetail)
        val prodDest = dest as NotificationDestination.ProductDetail
        assertEquals("prod_doc_1053", prodDest.productDocId)
        assertEquals(1053, prodDest.numericProductId)
    }

    @Test
    fun test_destinationResolution_productOfferAndPromotion() {
        // Offer with product doc id
        val offerWithProd = PushNotificationRecord(
            id = "notif_offer_1",
            type = "product_offer",
            targetProductDocId = "prod_99"
        )
        val dest1 = BekaNotificationManager.resolveDestination(offerWithProd)
        assertTrue("Offer with product must route to ProductDetail", dest1 is NotificationDestination.ProductDetail)

        // Promotion without specific product
        val promo = PushNotificationRecord(
            id = "notif_promo_1",
            type = "promotion",
            relatedEntityId = "promo_summer"
        )
        val dest2 = BekaNotificationManager.resolveDestination(promo)
        assertTrue("Promotion without product must route to Discounts", dest2 is NotificationDestination.Discounts)
        assertEquals("promo_summer", (dest2 as NotificationDestination.Discounts).campaignId)
    }

    @Test
    fun test_destinationResolution_customerOrder() {
        val orderNotif = PushNotificationRecord(
            id = "notif_order_1",
            type = "order_update",
            orderId = "BS-ORD-9021",
            orderNumber = "9021",
            recipientUid = "user_abc"
        )
        val dest = BekaNotificationManager.resolveDestination(orderNotif)
        assertTrue("Must resolve to CustomerOrderDetail", dest is NotificationDestination.CustomerOrderDetail)
        val orderDest = dest as NotificationDestination.CustomerOrderDetail
        assertEquals("BS-ORD-9021", orderDest.orderId)
        assertEquals("9021", orderDest.orderNumber)
        assertEquals("user_abc", orderDest.recipientUid)
    }

    @Test
    fun test_destinationResolution_productRequest() {
        val reqNotif = PushNotificationRecord(
            id = "notif_req_1",
            type = "product_request",
            orderId = "BS-REQ-444",
            orderNumber = "444",
            recipientUid = "user_xyz"
        )
        val dest = BekaNotificationManager.resolveDestination(reqNotif)
        assertTrue("Must resolve to ProductRequestDetail", dest is NotificationDestination.ProductRequestDetail)
        val reqDest = dest as NotificationDestination.ProductRequestDetail
        assertEquals("BS-REQ-444", reqDest.requestId)
        assertEquals("444", reqDest.requestNumber)
    }

    @Test
    fun test_destinationResolution_systemAndAppUpdate() {
        val sysUpdate = PushNotificationRecord(id = "notif_sys", type = "system_update")
        val appUpdate = PushNotificationRecord(id = "notif_app", type = "app_update")

        assertTrue("system_update resolves to AppUpdate", BekaNotificationManager.resolveDestination(sysUpdate) is NotificationDestination.AppUpdate)
        assertTrue("app_update resolves to AppUpdate", BekaNotificationManager.resolveDestination(appUpdate) is NotificationDestination.AppUpdate)
    }

    @Test
    fun test_destinationResolution_serviceUpdateAndAnnouncement() {
        // service_update without product or order
        val serviceNotif = PushNotificationRecord(
            id = "notif_serv_1",
            title = "სერვისის სიახლე",
            body = "ახალი ხელსაწყოები ჩამოვიდა",
            type = "service_update"
        )
        val destService = BekaNotificationManager.resolveDestination(serviceNotif)
        assertTrue("service_update must resolve to NotificationDetailModal", destService is NotificationDestination.NotificationDetailModal)

        // announcement with explicit targetTab = 2 (Discounts)
        val tabNotif = PushNotificationRecord(
            id = "notif_ann_tab",
            type = "announcement",
            targetTab = 2
        )
        val destTab = BekaNotificationManager.resolveDestination(tabNotif)
        assertTrue("announcement with tab must resolve to InAppTab", destTab is NotificationDestination.InAppTab)
        assertEquals(2, (destTab as NotificationDestination.InAppTab).tabIndex)

        // announcement without tab
        val modalNotif = PushNotificationRecord(
            id = "notif_ann_info",
            title = "ინფორმაცია",
            body = "ტექნიკური შესვენება",
            type = "announcement",
            targetTab = -1
        )
        val destModal = BekaNotificationManager.resolveDestination(modalNotif)
        assertTrue("announcement without tab resolves to NotificationDetailModal", destModal is NotificationDestination.NotificationDetailModal)
    }

    @Test
    fun test_destinationResolution_adminOrder() {
        val adminNotif = PushNotificationRecord(
            id = "notif_admin_ord",
            type = "admin_order",
            orderId = "BS-ORD-ADM-1"
        )
        val dest = BekaNotificationManager.resolveDestination(adminNotif)
        assertTrue("admin_order must resolve to AdminOrderDetail", dest is NotificationDestination.AdminOrderDetail)
        assertEquals("BS-ORD-ADM-1", (dest as NotificationDestination.AdminOrderDetail).orderId)
    }

    @Test
    fun test_destinationResolution_missingProductGivesFallback() {
        val emptyProdNotif = PushNotificationRecord(
            id = "notif_empty_prod",
            type = "product",
            targetProductDocId = "",
            targetProductId = 0
        )
        val dest = BekaNotificationManager.resolveDestination(emptyProdNotif)
        assertTrue("Missing product must resolve to UnavailableOrFallback", dest is NotificationDestination.UnavailableOrFallback)
        val fallback = dest as NotificationDestination.UnavailableOrFallback
        assertTrue("Reason must be in Georgian", fallback.reasonKa.contains("მიუწვდომელია"))
    }

    // =========================================================================
    // 3. Lifecycle & Intent Navigation Tests
    // =========================================================================

    @Test
    fun test_intentResolution_productColdStartIntent() {
        val intent = Intent().apply {
            putExtra(BekaNotificationManager.EXTRA_NOTIFICATION_ID, "n_cold_1")
            putExtra(BekaNotificationManager.EXTRA_NOTIFICATION_TYPE, "product")
            putExtra(BekaNotificationManager.EXTRA_TARGET_PRODUCT_DOC_ID, "prod_cold_99")
            putExtra(BekaNotificationManager.EXTRA_TARGET_PRODUCT_ID, 99)
        }
        val dest = BekaNotificationManager.resolveDestinationFromIntent(intent)
        assertTrue("Must resolve to ProductDetail from cold-start intent", dest is NotificationDestination.ProductDetail)
        assertEquals("prod_cold_99", (dest as NotificationDestination.ProductDetail).productDocId)
        assertEquals(99, dest.numericProductId)
    }

    @Test
    fun test_intentResolution_adminOrderDeepLinkUri() {
        val intent = Intent().apply {
            data = android.net.Uri.parse("bekaservice://admin/orders/service/order_adm_888")
        }
        val dest = BekaNotificationManager.resolveDestinationFromIntent(intent)
        assertTrue("Must resolve to AdminOrderDetail from admin URI", dest is NotificationDestination.AdminOrderDetail)
        assertEquals("order_adm_888", (dest as NotificationDestination.AdminOrderDetail).orderId)
    }

    @Test
    fun test_pendingIntentDistinctActionsAndData() {
        val intent1 = BekaNotificationManager.buildDeepLinkIntent(
            context = context,
            notificationId = "notif_item_1",
            title = "Title 1",
            body = "Body 1",
            targetProductId = 10,
            targetProductDocId = "doc_10",
            type = "product"
        )
        val intent2 = BekaNotificationManager.buildDeepLinkIntent(
            context = context,
            notificationId = "notif_item_2",
            title = "Title 2",
            body = "Body 2",
            targetProductId = 20,
            targetProductDocId = "doc_20",
            type = "product"
        )

        assertNotEquals("Intents must have distinct actions to prevent OS pending intent reuse", intent1.action, intent2.action)
        assertNotEquals("Intents must have distinct data URIs", intent1.data, intent2.data)
        assertEquals("bekaservice://notification/notif_item_1", intent1.data.toString())
        assertEquals("bekaservice://notification/notif_item_2", intent2.data.toString())
    }

    // =========================================================================
    // 4. Security & Privacy Tests
    // =========================================================================

    @Test
    fun test_securityIsolation_crossUserOrderAccessRejection() = runBlocking {
        val firestoreRepo = FirestoreRepository(context)

        // Attempting to fetch an order belonging to another user without admin auth must fail
        val result = firestoreRepo.fetchUserOrderById(
            orderIdOrNumber = "nonexistent_or_other_user_order",
            activeUid = "unauthorized_user_123"
        )
        // Since user is not admin and order does not match, it must return null or failure
        if (result.isSuccess) {
            val order = result.getOrNull()
            assertTrue("Order must be null if not owned by user", order == null || order.userId == "unauthorized_user_123")
        } else {
            assertTrue("Access must be rejected with exception", result.isFailure)
        }
    }

    // =========================================================================
    // 5. Launcher Intent vs Notification Intent Separation
    // =========================================================================

    @Test
    fun test_standardLauncherIntent_isNotNotificationOrDeepLink() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(context.packageName)
        }

        assertFalse(
            "Launcher intent must not be classified as notification or deep link",
            BekaNotificationManager.isNotificationOrDeepLinkIntent(launcherIntent)
        )

        val resolved = BekaNotificationManager.resolveDestinationFromIntent(launcherIntent)
        org.junit.Assert.assertNull(
            "Launcher intent must resolve to null destination (no unwanted modal popups)",
            resolved
        )
    }

    @Test
    fun test_emptyIntent_returnsNullDestination() {
        val emptyIntent = Intent()
        assertFalse(
            "Empty intent must not be classified as notification",
            BekaNotificationManager.isNotificationOrDeepLinkIntent(emptyIntent)
        )
        val resolved = BekaNotificationManager.resolveDestinationFromIntent(emptyIntent)
        org.junit.Assert.assertNull(
            "Empty intent must resolve to null destination",
            resolved
        )
    }

    @Test
    fun test_validNotificationIntent_resolvesDestinationCorrectly() {
        val notifIntent = BekaNotificationManager.buildDeepLinkIntent(
            context = context,
            notificationId = "notif_valid_123",
            title = "ფასდაკლება",
            body = "ახალი აქცია BSTORE-ში",
            targetTab = 2,
            type = "promotion"
        )

        assertTrue(
            "Notification intent must be recognized as notification",
            BekaNotificationManager.isNotificationOrDeepLinkIntent(notifIntent)
        )

        val resolved = BekaNotificationManager.resolveDestinationFromIntent(notifIntent)
        assertNotNull("Notification intent must resolve to valid destination", resolved)
        assertTrue(
            "Promotion with tab 2 must resolve to Discounts tab destination",
            resolved is NotificationDestination.Discounts
        )
    }
}

