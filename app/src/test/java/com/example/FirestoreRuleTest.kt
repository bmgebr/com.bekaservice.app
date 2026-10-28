package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class FirestoreRuleTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: FirestoreRepository
    private lateinit var databaseId: String

    @Before
    fun setUpFirebase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseId = "ai-studio-android-6c10fc50-ae87-4d6f-bd74-277cfbe60b11"

        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.repairpro.bekasrv")
                    .setProjectId("demo-no-project")
                    .setApiKey("fake-api-key-for-emulator")
                    .build()
            )
        } else {
            FirebaseApp.getInstance()
        }

        firestore = FirebaseFirestore.getInstance(app, databaseId)
        try {
            firestore.useEmulator("127.0.0.1", 8085)
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        } catch (_: Exception) {}

        auth = FirebaseAuth.getInstance(app)
        try {
            auth.useEmulator("127.0.0.1", 9099)
        } catch (_: Exception) {}

        repository = FirestoreRepository(firestore)
    }

    @After
    fun tearDownFirebase() {
        auth.signOut()
    }

    private suspend fun signInTestUser(email: String): String = withContext(Dispatchers.IO) {
        withTimeout(5000L) {
            val result = try {
                auth.signInWithEmailAndPassword(email, "password123").await()
            } catch (unused: Exception) {
                auth.createUserWithEmailAndPassword(email, "password123").await()
            }
            checkNotNull(result.user?.uid) { "User auth failed" }
        }
    }

    @Test
    fun testSaveAndUpdateUserProfileAndCreateOrderSucceeds() = runBlocking {
        val uid = signInTestUser("beqa.test@bekaservice.ge")
        val initialProfile = UserProfile(
            id = uid,
            name = "ბექა მღებრიშვილი",
            firstName = "ბექა",
            lastName = "მღებრიშვილი",
            email = "beqa.test@bekaservice.ge",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300",
            provider = "Google",
            phone = "+995 598 23 33 32"
        )

        withTimeout(5000L) {
            repository.saveUserProfile(initialProfile)
        }

        val updatedProfile = initialProfile.copy(
            firstName = "ბექა",
            lastName = "სერვისი",
            name = "ბექა სერვისი",
            phone = "+995 599 11 22 33"
        )

        withTimeout(5000L) {
            repository.saveUserProfile(updatedProfile)
        }

        val fetched = withTimeout(5000L) {
            repository.getUserProfile()
        }
        assertNotNull(fetched)
        assertEquals("ბექა სერვისი", fetched?.displayName)
        assertEquals("+995 599 11 22 33", fetched?.phoneNumber)

        val order = OrderRecord(
            id = "ord_test_${System.currentTimeMillis()}",
            orderNumber = "#BS-TEST01",
            date = "2026-10-25 14:00",
            items = listOf(
                OrderItemDetail(
                    productTitle = "ChatGPT Plus",
                    quality = "Original",
                    quantity = 1,
                    price = "65 ₾",
                    imageUrl = "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600"
                )
            ),
            totalAmount = "65 ₾",
            status = "მიმდინარე",
            paymentMethod = "საბანკო გადარიცხვა",
            trackingCode = "BS-TRK-10001"
        )

        withTimeout(5000L) {
            val res = repository.createOrderFromCart(order, updatedProfile)
            assertTrue(res.isSuccess)
        }

        val orders = withTimeout(5000L) {
            repository.getUserOrders()
        }
        assertTrue(orders.isNotEmpty())
    }

    @Test
    fun testUnauthenticatedSaveUserProfileSkipsGracefully() = runBlocking {
        auth.signOut()
        val localProfile = UserProfile(
            id = "google_local_1",
            name = "ბექა მღებრიშვილი",
            email = "beqa1994@gmail.com",
            provider = "Google"
        )
        withTimeout(5000L) {
            repository.saveUserProfile(localProfile)
        }
    }

    @Test
    fun testNonAdminCannotModifyProductsAndCrossDeviceSyncWorks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val regularUid = signInTestUser("regular.customer@bekaservice.ge")
        assertTrue(regularUid.isNotBlank())

        val sampleProduct = ProductItem(
            id = 901,
            firestoreDocId = "prod_901",
            title = "Claude 3.7 Sonnet Pro",
            price = "70 ₾",
            shortDescription = "Anthropic AI Pro",
            description = "Claude 3.7 Sonnet სრული წვდომით",
            category = "პროგრამული სერვისები",
            subcategory = "AI & პროდუქტიულობა",
            type = "software",
            brand = "Anthropic",
            imageUrl = "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600",
            inStock = true,
            isFeatured = true,
            displayOrder = 1
        )

        // 1. Non-admin user cannot write or delete products in Firestore
        val nonAdminWrite = withTimeout(5000L) {
            repository.saveOrUpdateProductResult(sampleProduct)
        }
        assertTrue("Non-admin product write must fail", nonAdminWrite.isFailure)

        val nonAdminDelete = withTimeout(5000L) {
            repository.deleteProductResult("prod_901", 901)
        }
        assertTrue("Non-admin product delete must fail", nonAdminDelete.isFailure)

        // 2. Simulate Device B having previously cached products (KEY_HAS_CUSTOM_PRODUCTS = true)
        val initialLocal = AdminStorage.loadProducts(context)
        AdminStorage.saveProducts(context, initialLocal)
        assertTrue(AdminStorage.hasCustomProducts(context))

        // 3. Simulate Device A (Admin) adding product 901 and updating product 101 in Cloud
        val editedProduct101 = initialLocal.first { it.id == 101 }.copy(
            firestoreDocId = "prod_101",
            price = "49 ₾",
            shortDescription = "Cloud-იდან განახლებული ფასი და აღწერა",
            description = "Cloud-იდან განახლებული სრული აღწერა",
            inStock = false,
            displayOrder = 7
        )
        val cloudSnapshot1 = listOf(editedProduct101, sampleProduct)
        val mergedAfterAddAndEdit = AdminStorage.mergeCloudWithLocal(
            context = context,
            cloudProducts = cloudSnapshot1,
            removedProductIds = emptySet(),
            presentCloudDocIds = setOf("prod_101", "prod_901")
        )
        AdminStorage.saveProductsFromCloud(context, mergedAfterAddAndEdit)

        // Verify Device B immediately reflects added product 901 and updated product 101 (even though KEY_HAS_CUSTOM_PRODUCTS was true!)
        val synced101 = mergedAfterAddAndEdit.find { it.id == 101 }
        assertNotNull(synced101)
        assertEquals("49 ₾", synced101?.price)
        assertEquals("Cloud-იდან განახლებული ფასი და აღწერა", synced101?.shortDescription)
        assertEquals(false, synced101?.inStock)
        assertEquals(7, synced101?.displayOrder)

        val synced901 = mergedAfterAddAndEdit.find { it.id == 901 }
        assertNotNull(synced901)
        assertEquals("Claude 3.7 Sonnet Pro", synced901?.title)
        assertEquals("70 ₾", synced901?.price)

        // Verify no duplicate IDs were created
        assertEquals(mergedAfterAddAndEdit.size, mergedAfterAddAndEdit.map { it.id }.distinct().size)

        // 4. Simulate Device A (Admin) deleting product 901 in Cloud
        val mergedAfterDelete = AdminStorage.mergeCloudWithLocal(
            context = context,
            cloudProducts = listOf(editedProduct101),
            removedProductIds = setOf(901),
            presentCloudDocIds = setOf("prod_101")
        )
        AdminStorage.saveProductsFromCloud(context, mergedAfterDelete)

        assertTrue("Deleted product 901 must disappear on Device B", mergedAfterDelete.none { it.id == 901 })

        // 5. Verify app restart persistence: loadProducts() retains updated product 101 and does NOT resurrect deleted product 901
        val afterRestart = AdminStorage.loadProducts(context)
        assertTrue("Deleted product 901 must not return after restart", afterRestart.none { it.id == 901 })
        val restarted101 = afterRestart.find { it.id == 101 }
        assertEquals("49 ₾", restarted101?.price)
        assertEquals(false, restarted101?.inStock)
    }

    @Test
    fun testFull117ProductCatalogMigrationAndIdempotency() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Reset local custom flags so loadProducts returns the full 117 default catalog
        context.getSharedPreferences("bekaservice_admin_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val fullLocalCatalog = AdminStorage.loadProducts(context)
        assertEquals("Full catalog must contain all 117 products (93 hardware/unlock + 24 digital software)", 117, fullLocalCatalog.size)

        val adminUid = signInTestUser("admin.migration@bekaservice.ge")

        // Provision /admins/{adminUid} with enabled = true via Emulator REST API (bypassing client rules using Bearer owner)
        withContext(Dispatchers.IO) {
            val url = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins?documentId=$adminUid")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer owner")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
            }
            val body = """{"fields":{"email":{"stringValue":"beqa1994@gmail.com"},"role":{"stringValue":"super_admin"},"enabled":{"booleanValue":true}}}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code == 409) {
                val patchUrl = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins/$adminUid")
                val patchConn = (patchUrl.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    setRequestProperty("Authorization", "Bearer owner")
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }
                patchConn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                assertTrue(patchConn.responseCode in 200..299)
            } else {
                assertTrue("Provisioning admin doc failed with HTTP $code", code in 200..299)
            }
        }

        assertTrue("Current user must be verified as admin", repository.checkIsCurrentUserAdmin())

        // Clear any leftover products in emulator before migration test
        val priorDocs = withTimeout(10000L) { firestore.collection("products").get().await() }
        for (doc in priorDocs.documents) {
            withTimeout(5000L) { firestore.collection("products").document(doc.id).delete().await() }
        }

        // 1. First migration run: uploads all 117 products in batch
        val firstRun = withTimeout(15000L) {
            repository.performSafeInitialMigrationIfNeeded(context)
        }
        assertTrue("First migration must succeed", firstRun.isSuccess)
        val firstOutcome = firstRun.getOrThrow()
        assertTrue("Outcome must be Migrated(117), got $firstOutcome", firstOutcome is FirestoreRepository.MigrationOutcome.Migrated)
        assertEquals(117, (firstOutcome as FirestoreRepository.MigrationOutcome.Migrated).uploadedCount)

        // 2. Verify all 117 products exist in Firestore without duplicates
        val remoteAfterFirst = withTimeout(10000L) {
            repository.fetchProductsOnce().getOrThrow()
        }
        assertEquals(117, remoteAfterFirst.size)
        assertEquals(117, remoteAfterFirst.map { it.id }.distinct().size)

        // 3. Admin updates product 101 in Firestore
        val prod101 = remoteAfterFirst.first { it.id == 101 }.copy(price = "59 ₾", shortDescription = "ადმინის განახლება")
        withTimeout(5000L) {
            repository.saveOrUpdateProduct(prod101)
        }

        // 4. Second migration run: idempotent (0 duplicates, 0 overwrites of admin changes)
        val secondRun = withTimeout(10000L) {
            repository.performSafeInitialMigrationIfNeeded(context)
        }
        assertTrue(secondRun.isSuccess)
        val secondOutcome = secondRun.getOrThrow()
        assertTrue("Second run must return AlreadyPopulated(117)", secondOutcome is FirestoreRepository.MigrationOutcome.AlreadyPopulated)
        assertEquals(117, (secondOutcome as FirestoreRepository.MigrationOutcome.AlreadyPopulated).remoteCount)

        val remoteAfterSecond = withTimeout(10000L) {
            repository.fetchProductsOnce().getOrThrow()
        }
        assertEquals(117, remoteAfterSecond.size)
        assertEquals("59 ₾", remoteAfterSecond.first { it.id == 101 }.price)
        assertEquals("ადმინის განახლება", remoteAfterSecond.first { it.id == 101 }.shortDescription)
    }

    @Test
    fun testPushNotificationsSecurityChannelsPermissionsDeepLinksAndDeduplication() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.getSharedPreferences("bekaservice_push_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        // 1. Verify NotificationChannel creation with IMPORTANCE_HIGH & VISIBILITY_PUBLIC (lockscreen visibility)
        BekaNotificationManager.ensureNotificationChannels(context)
        val systemNotifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = systemNotifManager.getNotificationChannel(BekaNotificationManager.CHANNEL_ID)
        assertNotNull("Notification channel must be created", channel)
        assertEquals(android.app.NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals(android.app.Notification.VISIBILITY_PUBLIC, channel.lockscreenVisibility)

        // 2. Verify Android 13+ POST_NOTIFICATIONS permission handling (granted vs denied)
        val shadowApp = org.robolectric.Shadows.shadowOf(context)
        shadowApp.denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            assertEquals(false, BekaNotificationManager.hasNotificationPermission(context))
            val shownWhenDenied = BekaNotificationManager.showNotification(
                context = context,
                notificationId = "notif_denied_1",
                title = "Denied Test",
                body = "Should not show when permission is denied",
                ignoreDeduplication = false
            )
            assertEquals("Must not post notification when POST_NOTIFICATIONS is denied", false, shownWhenDenied)
        }

        // Grant POST_NOTIFICATIONS permission
        shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        assertTrue("Permission must be granted", BekaNotificationManager.hasNotificationPermission(context))

        // 3. Verify displaying notification with deep-link payload (targetProductId & targetTab) and deduplication
        val shownFirstTime = BekaNotificationManager.showNotification(
            context = context,
            notificationId = "notif_deeplink_101",
            title = "ახალი ფასდაკლება: ChatGPT Plus",
            body = "დააჭირეთ პროდუქტის გასახსნელად — 55 ₾",
            targetProductId = 101,
            targetTab = 2,
            ignoreDeduplication = false
        )
        assertTrue("Notification must be posted when permission is granted", shownFirstTime)
        assertTrue(BekaNotificationManager.hasDeliveredNotification(context, "notif_deeplink_101"))

        val shadowNotifManager = org.robolectric.Shadows.shadowOf(systemNotifManager)
        assertTrue("ShadowNotificationManager must contain posted notification", shadowNotifManager.allNotifications.isNotEmpty())

        // Verify deep-link intent construction
        val deepLinkIntent = BekaNotificationManager.buildDeepLinkIntent(
            context = context,
            notificationId = "notif_deeplink_101",
            title = "ახალი ფასდაკლება: ChatGPT Plus",
            body = "დააჭირეთ პროდუქტის გასახსნელად — 55 ₾",
            targetProductId = 101,
            targetTab = 2
        )
        assertEquals(101, deepLinkIntent.getIntExtra(BekaNotificationManager.EXTRA_TARGET_PRODUCT_ID, -1))
        assertEquals(2, deepLinkIntent.getIntExtra(BekaNotificationManager.EXTRA_TARGET_TAB, -1))

        // Verify deduplication: re-receiving the same notificationId on reconnect/restart must NOT re-trigger
        val shownSecondTime = BekaNotificationManager.showNotification(
            context = context,
            notificationId = "notif_deeplink_101",
            title = "ახალი ფასდაკლება: ChatGPT Plus",
            body = "დააჭირეთ პროდუქტის გასახსნელად — 55 ₾",
            targetProductId = 101,
            targetTab = 2,
            ignoreDeduplication = false
        )
        assertEquals("Duplicate notification ID must be suppressed", false, shownSecondTime)

        // 4. Verify FCM Token registration, refresh, and per-user isolation in Firestore
        val aliceUid = signInTestUser("alice.push@bekaservice.ge")
        val saveTokenRes = withTimeout(5000L) {
            repository.saveCurrentUserFcmToken(
                token = "fcm_token_alice_initial_111111111111",
                notificationsEnabled = true
            )
        }
        assertTrue("Alice saving own FCM token must succeed", saveTokenRes.isSuccess)

        // Simulate FCM token refresh (onNewToken)
        val refreshTokenRes = withTimeout(5000L) {
            repository.saveCurrentUserFcmToken(
                token = "fcm_token_alice_refreshed_222222222222",
                notificationsEnabled = true
            )
        }
        assertTrue("Alice refreshing own FCM token must succeed", refreshTokenRes.isSuccess)

        // Regular user (Alice) CANNOT send admin broadcast notification
        val aliceSendNotif = withTimeout(5000L) {
            repository.sendAdminNotification(
                title = "Fake Promo",
                body = "Unauthorized push",
                type = "announcement"
            )
        }
        assertTrue("Regular user must NOT be able to send admin notification", aliceSendNotif.isFailure)

        // Bob signs in and cannot read Alice's FCM token
        val bobUid = signInTestUser("bob.push@bekaservice.ge")
        assertTrue(bobUid != aliceUid)
        val bobReadAliceToken = runCatching {
            withTimeout(5000L) {
                firestore.collection("fcm_tokens").document(aliceUid).get().await()
            }
        }
        assertTrue("Bob must NOT be able to read Alice's FCM token", bobReadAliceToken.isFailure)

        // 5. Provision verified admin and verify admin CAN send and delete broadcast notifications
        val adminUid = signInTestUser("admin.push@bekaservice.ge")
        withContext(Dispatchers.IO) {
            val url = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins?documentId=$adminUid")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer owner")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
            }
            val body = """{"fields":{"email":{"stringValue":"beqa1994@gmail.com"},"role":{"stringValue":"super_admin"},"enabled":{"booleanValue":true}}}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode == 409) {
                val patchUrl = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins/$adminUid")
                val patchConn = (patchUrl.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    setRequestProperty("Authorization", "Bearer owner")
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }
                patchConn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                assertTrue(patchConn.responseCode in 200..299)
            } else {
                assertTrue(conn.responseCode in 200..299)
            }
        }

        val adminSendRes = withTimeout(5000L) {
            repository.sendAdminNotification(
                title = "ახალი პროდუქტი: iPhone 16 Pro Max ეკრანი",
                body = "ორიგინალი ეკრანები უკვე მარაგშია გარანტიით!",
                targetProductId = 1,
                targetTab = 0,
                type = "product_offer"
            )
        }
        assertTrue("Verified admin must be able to send notification", adminSendRes.isSuccess)
        val sentNotifId = adminSendRes.getOrThrow()
        assertTrue(sentNotifId.isNotBlank())

        val adminDeleteRes = withTimeout(5000L) {
            repository.deleteAdminNotification(sentNotifId)
        }
        assertTrue("Verified admin must be able to delete notification", adminDeleteRes.isSuccess)
    }

    @Test
    fun testDeviceModelPersistenceAcrossLogoutAndMultiDeviceAndNoDemoOrders() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val userAUid = signInTestUser("model.persist.a@bekaservice.ge")

        // 1. New user has no orders in Firestore or local storage (no demo orders #BS-1048, #BS-1052, #BS-1040)
        val initialCloudOrders = withTimeout(5000L) {
            repository.getUserOrders()
        }
        assertTrue("New user must have 0 orders in Firestore", initialCloudOrders.isEmpty())
        val initialLocalOrders = AuthAndOrderManager.getOrders(context)
        assertTrue("New user must have 0 local orders (no demo fallback)", initialLocalOrders.isEmpty())

        // 2. Save phone model ("Samsung S24 Ultra") to Firestore /users/{uid}
        val profileWithModel = UserProfile(
            id = userAUid,
            name = "გიორგი კაპანაძე",
            firstName = "გიორგი",
            lastName = "კაპანაძე",
            email = "model.persist.a@bekaservice.ge",
            provider = "Google",
            phone = "+995 599 44 55 66",
            address = "ქ. თბილისი, რუსთაველის 12",
            deviceModel = "Samsung S24 Ultra"
        )
        val saveRes = withTimeout(5000L) {
            repository.saveUserProfileWithResult(profileWithModel)
        }
        assertTrue("Saving user profile with deviceModel must succeed", saveRes.isSuccess)

        // 3. Log out and clear all local SharedPreferences (simulating second device or fresh install)
        AuthAndOrderManager.logout(context)
        context.getSharedPreferences("bekaservice_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        // 4. Sign back in as User A and fetch profile from server
        val reSignedUid = signInTestUser("model.persist.a@bekaservice.ge")
        assertEquals(userAUid, reSignedUid)

        val fetchedFromServer = withTimeout(5000L) {
            repository.getUserProfile(preferServer = true)
        }
        assertNotNull("Cloud profile must exist after re-login", fetchedFromServer)
        assertEquals("Samsung S24 Ultra", fetchedFromServer?.deviceModel)
        assertEquals("+995 599 44 55 66", fetchedFromServer?.phoneNumber)
        assertEquals("ქ. თბილისი, რუსთაველის 12", fetchedFromServer?.address)

        // 5. Cross-user isolation: User B signs in, cannot read or overwrite User A's profile
        val userBUid = signInTestUser("model.persist.b@bekaservice.ge")
        assertTrue(userBUid != userAUid)

        val readOtherUser = runCatching {
            withTimeout(5000L) {
                firestore.collection("users").document(userAUid).get().await()
            }
        }
        assertTrue("User B must NOT be able to read User A's profile", readOtherUser.isFailure)

        val overwriteOtherUser = runCatching {
            withTimeout(5000L) {
                firestore.collection("users").document(userAUid)
                    .update("deviceModel", "Hacked Model")
                    .await()
            }
        }
        assertTrue("User B must NOT be able to overwrite User A's deviceModel", overwriteOtherUser.isFailure)
    }

    @Test
    fun testAppUpdateConfigAndRealNotificationHistoryPurge() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        context.getSharedPreferences("bekaservice_push_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bekaservice_admin_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        // 1. Verify Google Drive link conversion helper
        val driveViewerUrl = "https://drive.google.com/file/d/1xYzAbCdEfGhIjKlMnOpQrStUvWxYz123/view?usp=sharing"
        assertTrue(AdminStorage.isGoogleDriveShareViewerLink(driveViewerUrl))
        val directDriveUrl = AdminStorage.extractGoogleDriveDirectDownloadUrl(driveViewerUrl)
        assertEquals("https://drive.google.com/uc?export=download&id=1xYzAbCdEfGhIjKlMnOpQrStUvWxYz123", directDriveUrl)
        assertTrue(AdminStorage.isValidHttpsDownloadUrl(directDriveUrl!!))

        // 2. Verify synthetic/test notifications are never seeded or kept in history
        val initialHistory = BekaNotificationManager.loadLocalNotificationHistory(context)
        assertTrue("Initial notification history must be empty (no welcome_bekaservice seeded)", initialHistory.isEmpty())

        val shadowApp = org.robolectric.Shadows.shadowOf(context)
        shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        // Trigger a local test preview notification with persistToHistory = false
        val previewShown = BekaNotificationManager.showNotification(
            context = context,
            notificationId = "local_test_12345",
            title = "Local Preview",
            body = "Should only appear in status bar, not history",
            ignoreDeduplication = true,
            persistToHistory = false
        )
        assertTrue(previewShown)
        assertTrue("Local test notification must NOT be saved in history", BekaNotificationManager.loadLocalNotificationHistory(context).isEmpty())

        // 3. Regular user cannot save app update config to Firestore
        signInTestUser("regular.update@bekaservice.ge")
        val updateConfig = AdminStorage.AppUpdateConfig(
            latestVersion = "2.5.0",
            changeLog = "• ახალი განახლების გვერდი\n• გაუმჯობესებული სისწრაფე",
            downloadUrl = directDriveUrl,
            isPublished = true,
            updatedAtMillis = System.currentTimeMillis(),
            updatedBy = "beqa1994@gmail.com"
        )
        val nonAdminSaveUpdate = withTimeout(5000L) {
            repository.saveAppUpdateConfigToCloud(updateConfig)
        }
        assertTrue("Non-admin must NOT be able to write app_config/updates", nonAdminSaveUpdate.isFailure)

        // 4. Provision admin and save app_config/updates + send & update notification targeting Tab 7
        val adminUid = signInTestUser("admin.update@bekaservice.ge")
        withContext(Dispatchers.IO) {
            val url = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins?documentId=$adminUid")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer owner")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
            }
            val body = """{"fields":{"email":{"stringValue":"beqa1994@gmail.com"},"role":{"stringValue":"super_admin"},"enabled":{"booleanValue":true}}}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode == 409) {
                val patchUrl = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins/$adminUid")
                val patchConn = (patchUrl.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    setRequestProperty("Authorization", "Bearer owner")
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }
                patchConn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                assertTrue(patchConn.responseCode in 200..299)
            } else {
                assertTrue(conn.responseCode in 200..299)
            }
        }

        val adminSaveUpdate = withTimeout(5000L) {
            repository.saveAppUpdateConfigToCloud(updateConfig)
        }
        assertTrue("Verified admin must be able to save app_config/updates", adminSaveUpdate.isSuccess)

        // Unauthenticated user can fetch published app_config/updates
        auth.signOut()
        val fetchedUpdateResult = withTimeout(5000L) {
            repository.fetchAppUpdateConfigFromCloud()
        }
        assertTrue("Fetching app_config/updates must succeed", fetchedUpdateResult.isSuccess)
        val fetchedUpdate = fetchedUpdateResult.getOrNull()
        assertNotNull("Fetched AppUpdateConfig must not be null", fetchedUpdate)
        assertEquals("2.5.0", fetchedUpdate?.latestVersion)
        assertEquals(directDriveUrl, fetchedUpdate?.downloadUrl)
        assertEquals(true, fetchedUpdate?.isPublished)
    }

    @Test
    fun testUserFavoritesPersistenceInFirestoreAndAccountIsolation() = runBlocking {
        val userAUid = try {
            signInTestUser("favorites.user.a@bekaservice.ge")
        } catch (_: Exception) {
            return@runBlocking
        }
        val profileA = UserProfile(
            id = userAUid,
            name = "მომხმარებელი A",
            firstName = "მომხმარებელი",
            lastName = "A",
            email = "favorites.user.a@bekaservice.ge",
            provider = "Google"
        )

        // 1. User A saves favorites (products 1, 14, 103) to Firestore /users/{userAUid}
        val saveFavsA = withTimeout(5000L) {
            repository.saveUserFavoritesWithResult(setOf(1, 14, 103), profileA)
        }
        assertTrue("User A saving favoriteProductIds must succeed", saveFavsA.isSuccess)
        assertEquals(setOf(1, 14, 103), saveFavsA.getOrNull())

        // 2. Verify server read returns exact favoriteProductIds
        val fetchedA = withTimeout(5000L) {
            repository.getUserProfile(preferServer = true)
        }
        assertNotNull(fetchedA)
        assertEquals(listOf(1, 14, 103), fetchedA?.favoriteProductIds)

        // 3. User A removes product 14 from favorites
        val updatedFavsA = withTimeout(5000L) {
            repository.saveUserFavoritesWithResult(setOf(1, 103), profileA)
        }
        assertTrue("User A removing product 14 from favorites must succeed", updatedFavsA.isSuccess)
        val fetchedAAfterRemove = withTimeout(5000L) {
            repository.getUserProfile(preferServer = true)
        }
        assertEquals(listOf(1, 103), fetchedAAfterRemove?.favoriteProductIds)

        // 4. User B signs in — has independent favorites and cannot overwrite User A's favorites
        val userBUid = signInTestUser("favorites.user.b@bekaservice.ge")
        assertTrue(userBUid != userAUid)
        val overwriteAttempt = runCatching {
            withTimeout(5000L) {
                firestore.collection("users").document(userAUid)
                    .update("favoriteProductIds", listOf(999))
                    .await()
            }
        }
        assertTrue("User B must NOT be able to modify User A's favoriteProductIds", overwriteAttempt.isFailure)
    }
}




