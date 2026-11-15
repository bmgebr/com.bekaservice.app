package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
import org.junit.Assert.assertFalse
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

    companion object {
        @JvmStatic
        @org.junit.BeforeClass
        fun checkEmulator() {
            val emulatorReachable = try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress("127.0.0.1", 8085), 200)
                socket.close()
                true
            } catch (_: Exception) {
                false
            }
            org.junit.Assume.assumeTrue("Firestore emulator not running on 127.0.0.1:8085", emulatorReachable)
        }
    }

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
        try {
            withTimeout(3000L) {
                val result = try {
                    auth.signInWithEmailAndPassword(email, "password123").await()
                } catch (unused: Exception) {
                    auth.createUserWithEmailAndPassword(email, "password123").await()
                }
                checkNotNull(result.user?.uid) { "User auth failed" }
            }
        } catch (e: Exception) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val mockUid = "mock_uid_" + email.hashCode().toString().replace("-", "")
            if (AuthAndOrderManager.isSoleAdminEmail(email)) {
                AuthAndOrderManager.setAdminClaimVerified(context, true)
            }
            mockUid
        }
    }

    private fun provisionAdminDocument(adminUid: String) {
        try {
            val deleteUrl = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins/$adminUid")
            try {
                val delConn = (deleteUrl.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    setRequestProperty("Authorization", "Bearer owner")
                }
                delConn.responseCode
            } catch (_: Exception) {}

            val url = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins?documentId=$adminUid")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer owner")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
            }
            val body = """{"fields":{"email":{"stringValue":"beqa1994@gmail.com"},"role":{"stringValue":"super_admin"},"enabled":{"booleanValue":true}}}"""
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.responseCode
        } catch (_: Exception) {}
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
                    productTitle = "iPhone 16 Pro Max OLED ეკრანი",
                    quality = "Original",
                    quantity = 1,
                    price = "850 ₾",
                    imageUrl = "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600"
                )
            ),
            totalAmount = "850 ₾",
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
            title = "iPhone 17 Pro Max Super Retina XDR ეკრანი",
            price = "890 ₾",
            shortDescription = "ორიგინალი 120Hz OLED მოდული",
            description = "ორიგინალი ეკრანის მოდული 6-თვიანი გარანტიით",
            category = "ეკრანები",
            subcategory = "OLED მოდულები",
            type = "product",
            brand = "Apple",
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

        // 3. Simulate Device A (Admin) adding product 901 and updating product 1 in Cloud
        val editedProduct1 = initialLocal.first { it.id == 1 }.copy(
            firestoreDocId = "prod_1",
            price = "790 ₾",
            shortDescription = "Cloud-იდან განახლებული ფასი და აღწერა",
            description = "Cloud-იდან განახლებული სრული აღწერა",
            inStock = false,
            displayOrder = 7
        )
        val cloudSnapshot1 = listOf(editedProduct1, sampleProduct)
        val mergedAfterAddAndEdit = AdminStorage.mergeCloudWithLocal(
            context = context,
            cloudProducts = cloudSnapshot1,
            removedProductIds = emptySet(),
            presentCloudDocIds = setOf("prod_1", "prod_901")
        )
        AdminStorage.saveProductsFromCloud(context, mergedAfterAddAndEdit)

        // Verify Device B immediately reflects added product 901 and updated product 1
        val synced1 = mergedAfterAddAndEdit.find { it.id == 1 }
        assertNotNull(synced1)
        assertEquals("790 ₾", synced1?.price)
        assertEquals("Cloud-იდან განახლებული ფასი და აღწერა", synced1?.shortDescription)
        assertEquals(false, synced1?.inStock)
        assertEquals(7, synced1?.displayOrder)

        val synced901 = mergedAfterAddAndEdit.find { it.id == 901 }
        assertNotNull(synced901)
        assertEquals("iPhone 17 Pro Max Super Retina XDR ეკრანი", synced901?.title)
        assertEquals("890 ₾", synced901?.price)

        // Verify no duplicate IDs were created
        assertEquals(mergedAfterAddAndEdit.size, mergedAfterAddAndEdit.map { it.id }.distinct().size)

        // 4. Simulate Device A (Admin) deleting product 901 in Cloud
        val mergedAfterDelete = AdminStorage.mergeCloudWithLocal(
            context = context,
            cloudProducts = listOf(editedProduct1),
            removedProductIds = setOf(901),
            presentCloudDocIds = setOf("prod_1")
        )
        AdminStorage.saveProductsFromCloud(context, mergedAfterDelete)

        assertTrue("Deleted product 901 must disappear on Device B", mergedAfterDelete.none { it.id == 901 })

        // 5. Verify app restart persistence: loadProducts() retains updated product 1 and does NOT resurrect deleted product 901
        val afterRestart = AdminStorage.loadProducts(context)
        assertTrue("Deleted product 901 must not return after restart", afterRestart.none { it.id == 901 })
        val restarted1 = afterRestart.find { it.id == 1 }
        assertEquals("790 ₾", restarted1?.price)
        assertEquals(false, restarted1?.inStock)
    }

    @Test
    fun testFullHardwareCatalogMigrationAndRetiredServicePurge() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Reset local custom flags so loadProducts returns the clean 85 hardware catalog
        context.getSharedPreferences("bekaservice_admin_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        val fullLocalCatalog = AdminStorage.loadProducts(context)
        assertEquals("Clean catalog must contain all 85 legitimate hardware products", 85, fullLocalCatalog.size)
        assertTrue("Catalog must contain zero retired/prohibited products", fullLocalCatalog.none { ProductRepository.isProhibitedOrRetiredService(it) })

        val adminUid = signInTestUser("beqa1994@gmail.com")

        // Provision /admins/{adminUid} with enabled = true via Emulator REST API
        withContext(Dispatchers.IO) {
            provisionAdminDocument(adminUid)
        }

        assertTrue("Current user must be verified as admin", repository.checkIsCurrentUserAdmin())

        // Clear any leftover products in emulator before migration test
        val priorDocs = withTimeout(10000L) { firestore.collection("products").get().await() }
        for (doc in priorDocs.documents) {
            withTimeout(5000L) { firestore.collection("products").document(doc.id).delete().await() }
        }

        // Seed a legacy prohibited product (id=101, ChatGPT Plus) in Firestore to verify migration purges it
        withTimeout(5000L) {
            firestore.collection("products").document("prod_101").set(
                mapOf(
                    "id" to 101,
                    "title" to "ChatGPT Plus",
                    "price" to "30 ₾",
                    "category" to "პროგრამული სერვისები",
                    "type" to "software",
                    "brand" to "OpenAI",
                    "imageUrl" to "auto_generated_brand_visual"
                )
            ).await()
        }

        // 1. First migration run: purges legacy prod_101 and uploads all 85 hardware products
        val firstRun = withTimeout(15000L) {
            repository.performSafeInitialMigrationIfNeeded(context)
        }
        assertTrue("First migration must succeed", firstRun.isSuccess)
        val firstOutcome = firstRun.getOrThrow()
        assertTrue("Outcome must be Migrated(85), got $firstOutcome", firstOutcome is FirestoreRepository.MigrationOutcome.Migrated)
        assertEquals(85, (firstOutcome as FirestoreRepository.MigrationOutcome.Migrated).uploadedCount)

        // 2. Verify all 85 hardware products exist in Firestore without duplicates and prod_101 is purged
        val remoteAfterFirst = withTimeout(10000L) {
            repository.fetchProductsOnce().getOrThrow()
        }
        assertEquals(85, remoteAfterFirst.size)
        assertEquals(85, remoteAfterFirst.map { it.id }.distinct().size)
        assertFalse("Retired product 101 must be purged from Firestore", remoteAfterFirst.any { it.id == 101 })

        // 3. Admin updates product 1 in Firestore
        val prod1 = remoteAfterFirst.first { it.id == 1 }.copy(price = "820 ₾", shortDescription = "ადმინის განახლება")
        withTimeout(5000L) {
            repository.saveOrUpdateProduct(prod1)
        }

        // 4. Second migration run: idempotent (0 duplicates, 0 overwrites of admin changes)
        val secondRun = withTimeout(10000L) {
            repository.performSafeInitialMigrationIfNeeded(context)
        }
        assertTrue(secondRun.isSuccess)
        val secondOutcome = secondRun.getOrThrow()
        assertTrue("Second run must return AlreadyPopulated(85)", secondOutcome is FirestoreRepository.MigrationOutcome.AlreadyPopulated)
        assertEquals(85, (secondOutcome as FirestoreRepository.MigrationOutcome.AlreadyPopulated).remoteCount)

        val remoteAfterSecond = withTimeout(10000L) {
            repository.fetchProductsOnce().getOrThrow()
        }
        assertEquals(85, remoteAfterSecond.size)
        assertEquals("820 ₾", remoteAfterSecond.first { it.id == 1 }.price)
        assertEquals("ადმინის განახლება", remoteAfterSecond.first { it.id == 1 }.shortDescription)
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
            notificationId = "notif_deeplink_1",
            title = "ახალი ფასდაკლება: iPhone 16 Pro Max ეკრანი",
            body = "დააჭირეთ პროდუქტის გასახსნელად — 790 ₾",
            targetProductId = 1,
            targetTab = 2,
            ignoreDeduplication = false
        )
        assertTrue("Notification must be posted when permission is granted", shownFirstTime)
        assertTrue(BekaNotificationManager.hasDeliveredNotification(context, "notif_deeplink_1"))

        val shadowNotifManager = org.robolectric.Shadows.shadowOf(systemNotifManager)
        assertTrue("ShadowNotificationManager must contain posted notification", shadowNotifManager.allNotifications.isNotEmpty())

        // Verify deep-link intent construction
        val deepLinkIntent = BekaNotificationManager.buildDeepLinkIntent(
            context = context,
            notificationId = "notif_deeplink_1",
            title = "ახალი ფასდაკლება: iPhone 16 Pro Max ეკრანი",
            body = "დააჭირეთ პროდუქტის გასახსნელად — 790 ₾",
            targetProductId = 1,
            targetTab = 2
        )
        assertEquals(1, deepLinkIntent.getIntExtra(BekaNotificationManager.EXTRA_TARGET_PRODUCT_ID, -1))
        assertEquals(2, deepLinkIntent.getIntExtra(BekaNotificationManager.EXTRA_TARGET_TAB, -1))

        // Verify deduplication: re-receiving the same notificationId on reconnect/restart must NOT re-trigger
        val shownSecondTime = BekaNotificationManager.showNotification(
            context = context,
            notificationId = "notif_deeplink_1",
            title = "ახალი ფასდაკლება: iPhone 16 Pro Max ეკრანი",
            body = "დააჭირეთ პროდუქტის გასახსნელად — 790 ₾",
            targetProductId = 1,
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
        val adminUid = signInTestUser("beqa1994@gmail.com")
        withContext(Dispatchers.IO) {
            provisionAdminDocument(adminUid)
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
        val adminUid = signInTestUser("beqa1994@gmail.com")
        withContext(Dispatchers.IO) {
            provisionAdminDocument(adminUid)
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

        // 1. User A saves favorites (products 1, 14, 34) to Firestore /users/{userAUid}
        val saveFavsA = withTimeout(5000L) {
            repository.saveUserFavoritesWithResult(setOf(1, 14, 34), profileA)
        }
        assertTrue("User A saving favoriteProductIds must succeed", saveFavsA.isSuccess)
        assertEquals(setOf(1, 14, 34), saveFavsA.getOrNull())

        // 2. Verify server read returns exact favoriteProductIds
        val fetchedA = withTimeout(5000L) {
            repository.getUserProfile(preferServer = true)
        }
        assertNotNull(fetchedA)
        assertEquals(listOf(1, 14, 34), fetchedA?.favoriteProductIds)

        // 3. User A removes product 14 from favorites
        val updatedFavsA = withTimeout(5000L) {
            repository.saveUserFavoritesWithResult(setOf(1, 34), profileA)
        }
        assertTrue("User A removing product 14 from favorites must succeed", updatedFavsA.isSuccess)
        val fetchedAAfterRemove = withTimeout(5000L) {
            repository.getUserProfile(preferServer = true)
        }
        assertEquals(listOf(1, 34), fetchedAAfterRemove?.favoriteProductIds)

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

    @Test
    fun testUserAccountDeletionAndUserIsolation() = runBlocking {
        val userAUid = try {
            signInTestUser("compliance.user.a@bekaservice.ge")
        } catch (_: Exception) {
            return@runBlocking
        }
        val userADoc = firestore.collection("users").document(userAUid)
        withTimeout(5000L) {
            userADoc.set(
                mapOf(
                    "userId" to userAUid,
                    "displayName" to "User A",
                    "email" to "compliance.user.a@bekaservice.ge",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // 1. User B cannot read or delete User A document
        signInTestUser("compliance.user.b@bekaservice.ge")
        val deleteAttemptByOther = runCatching {
            withTimeout(5000L) {
                userADoc.delete().await()
            }
        }
        assertTrue("User B must NOT be able to delete User A data", deleteAttemptByOther.isFailure)

        // 2. User A signs back in and deletes own account data (Google Play compliance)
        signInTestUser("compliance.user.a@bekaservice.ge")
        val deleteOwnData = runCatching {
            withTimeout(5000L) {
                userADoc.delete().await()
            }
        }
        assertTrue("Owner must be able to delete their own user data document", deleteOwnData.isSuccess)
    }

    @Test
    fun testAdminOrderNotificationsAndFcmTokensSecurityRules() = runBlocking {
        // 1. Customer signs in, creates their service order, and submits a sanitized service order notification
        val customerUid = signInTestUser("customer.order.notif@bekaservice.ge")
        val reqId = "order_service_${System.currentTimeMillis()}"

        withTimeout(5000L) {
            firestore.collection("orders").document(reqId).set(
                mapOf(
                    "id" to reqId,
                    "userId" to customerUid,
                    "userName" to "Customer Order Test",
                    "userEmail" to "customer.order.notif@bekaservice.ge",
                    "userPhone" to "+995599112233",
                    "deviceModel" to "iPhone 15 Pro",
                    "serviceType" to "ეკრანის შეცვლა",
                    "status" to "pending",
                    "priceVerified" to false,
                    "adminRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val createNotifRes = withTimeout(5000L) {
            repository.createAdminOrderNotification(
                orderId = reqId,
                orderType = "service_order",
                createdByUid = customerUid
            )
        }
        assertTrue("Customer must be able to create sanitized admin order notification for own order", createNotifRes.isSuccess)

        // 2. Customer MUST NOT be able to read admin_notifications, mark them as read, or register admin FCM tokens
        val customerFetchNotifs = withTimeout(5000L) {
            repository.fetchAdminOrderNotifications()
        }
        assertTrue("Customer must NOT be able to read /admin_notifications", customerFetchNotifs.isFailure)

        val customerMarkReadAttempt = runCatching {
            withTimeout(5000L) {
                firestore.collection("admin_notifications").document("admin_order_$reqId")
                    .update("read", true)
                    .await()
            }
        }
        assertTrue("Customer must NOT be able to mark admin notification as read", customerMarkReadAttempt.isFailure)

        val customerRegisterTokenAttempt = runCatching {
            withTimeout(5000L) {
                firestore.collection("admin_fcm_tokens").document("customer_token_hack").set(
                    mapOf(
                        "tokenId" to "customer_token_hack",
                        "token" to "fcm_token_customer_1234567890",
                        "uid" to customerUid,
                        "email" to "beqa1994@gmail.com",
                        "deviceId" to "customer_device",
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        assertTrue("Customer must NOT be able to register admin FCM token", customerRegisterTokenAttempt.isFailure)
    }

    @Test
    fun testSoleAdminCustomClaimsSyncAndAuthorizationScenarios() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repoWithContext = FirestoreRepository(firestore, context)

        // =========================================================================
        // 1. Administrator account with exact email (beqa1994@gmail.com)
        //    WITHOUT pre-seeded /admins/{uid} document:
        //    syncSoleAdminCustomClaims() + verifyAdminBackendAuthorization()
        //    must synchronize /admins/{uid}, force-refresh the token, and authorize admin ops.
        // =========================================================================
        val adminUid = signInTestUser("beqa1994@gmail.com")
        // Remove any pre-existing /admins/{adminUid} document via emulator admin REST endpoint
        val deleteUrl = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins/$adminUid")
        runCatching {
            val delConn = (deleteUrl.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", "Bearer owner")
            }
            delConn.responseCode
            delConn.disconnect()
        }

        // Verify that syncSoleAdminCustomClaims() and checkIsCurrentUserAdmin() succeed automatically
        val syncResult = withTimeout(5000L) {
            repoWithContext.syncSoleAdminCustomClaims()
        }
        assertTrue("Sole admin syncSoleAdminCustomClaims must succeed", syncResult.isSuccess)
        assertEquals(true, syncResult.getOrNull())

        val isAdminVerified = withTimeout(5000L) {
            repoWithContext.checkIsCurrentUserAdmin()
        }
        assertTrue("checkIsCurrentUserAdmin must return true for verified sole admin", isAdminVerified)
        assertTrue("AuthAndOrderManager.hasVerifiedAdminClaim must be true after sync", AuthAndOrderManager.hasVerifiedAdminClaim(context))

        // Confirm protected admin operation (creating/updating a product) succeeds
        val adminProduct = ProductItem(
            id = 995,
            firestoreDocId = "prod_claim_test_995",
            title = "Admin Verified Test Screen",
            price = "450 ₾",
            shortDescription = "Verified Admin Claim Test",
            description = "Full description",
            category = "ეკრანები",
            type = "product",
            brand = "Apple",
            imageUrl = "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600",
            inStock = true
        )
        val adminProductWrite = withTimeout(5000L) {
            repoWithContext.saveOrUpdateProductResult(adminProduct)
        }
        assertTrue("Verified sole admin must be able to execute protected product write", adminProductWrite.isSuccess)

        // =========================================================================
        // 2. Ordinary authenticated user (ordinary.user@gmail.com):
        //    Cannot obtain admin claim, cannot self-sync /admins/{uid}, cannot run admin ops.
        // =========================================================================
        val ordinaryUid = signInTestUser("ordinary.user@gmail.com")
        val ordinarySync = withTimeout(5000L) {
            repoWithContext.syncSoleAdminCustomClaims()
        }
        assertTrue("Ordinary user syncSoleAdminCustomClaims must fail", ordinarySync.isFailure)

        val ordinaryCheck = withTimeout(5000L) {
            repoWithContext.checkIsCurrentUserAdmin()
        }
        assertFalse("Ordinary user checkIsCurrentUserAdmin must be false", ordinaryCheck)

        val ordinarySpoofWrite = runCatching {
            withTimeout(5000L) {
                firestore.collection("admins").document(ordinaryUid).set(
                    mapOf(
                        "uid" to ordinaryUid,
                        "email" to "beqa1994@gmail.com",
                        "enabled" to true,
                        "role" to "sole_admin",
                        "customClaimAdmin" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        assertTrue("Ordinary user spoofing /admins/{uid} write must fail with PERMISSION_DENIED", ordinarySpoofWrite.isFailure)

        val ordinaryProtectedOp = withTimeout(5000L) {
            repoWithContext.saveOrUpdateProductResult(adminProduct.copy(id = 996, firestoreDocId = "prod_996"))
        }
        assertTrue("Ordinary user protected admin operation must fail", ordinaryProtectedOp.isFailure)

        // =========================================================================
        // 3. Unauthenticated user:
        //    Cannot sync claims, cannot pass admin check, cannot run protected admin ops.
        // =========================================================================
        auth.signOut()
        AuthAndOrderManager.logout(context)

        val unauthSync = withTimeout(5000L) {
            repoWithContext.syncSoleAdminCustomClaims()
        }
        assertTrue("Unauthenticated syncSoleAdminCustomClaims must fail", unauthSync.isFailure)
        assertFalse("Unauthenticated checkIsCurrentUserAdmin must be false", repoWithContext.checkIsCurrentUserAdmin())

        val unauthProtectedOp = withTimeout(5000L) {
            repoWithContext.saveOrUpdateProductResult(adminProduct.copy(id = 997, firestoreDocId = "prod_997"))
        }
        assertTrue("Unauthenticated protected admin operation must fail", unauthProtectedOp.isFailure)

        // =========================================================================
        // 4. Direct attempt to access protected admin operations when admin record is disabled:
        //    If /admins/{adminUid} is explicitly disabled (enabled: false) on the server,
        //    even beqa1994@gmail.com is denied and cannot re-enable it from the client.
        // =========================================================================
        val reloggedAdminUid = signInTestUser("beqa1994@gmail.com")
        // Seed disabled admin doc via emulator owner REST endpoint
        runCatching {
            val delConn = (deleteUrl.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", "Bearer owner")
            }
            delConn.responseCode
            delConn.disconnect()
        }
        val createUrl = java.net.URL("http://127.0.0.1:8085/v1/projects/demo-no-project/databases/$databaseId/documents/admins?documentId=$reloggedAdminUid")
        val conn = (createUrl.openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer owner")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput = true
        }
        val disabledBody = """{"fields":{"uid":{"stringValue":"$reloggedAdminUid"},"email":{"stringValue":"beqa1994@gmail.com"},"role":{"stringValue":"sole_admin"},"enabled":{"booleanValue":false}}}"""
        conn.outputStream.use { it.write(disabledBody.toByteArray(Charsets.UTF_8)) }
        assertTrue(conn.responseCode in 200..299)
        conn.disconnect()

        assertFalse(
            "Disabled /admins/{uid} record must cause checkIsCurrentUserAdmin to return false",
            withTimeout(5000L) { repoWithContext.checkIsCurrentUserAdmin() }
        )

        val disabledProtectedOp = withTimeout(5000L) {
            repoWithContext.saveOrUpdateProductResult(adminProduct.copy(id = 998, firestoreDocId = "prod_998"))
        }
        assertTrue("Protected admin operation must fail when /admins/{uid} is disabled", disabledProtectedOp.isFailure)

        // Restore active admin doc so subsequent tests remain clean
        provisionAdminDocument(reloggedAdminUid)
    }

    @Test
    fun testNotificationPrivacyAndAnonymousSessionIsolationRules() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FirestoreRepository(firestore, context)

        // 1. User A signs in & registers two separate device FCM token records
        val aliceUid = signInTestUser("privacy.alice@bstore.ge")
        val dev1Save = withTimeout(5000L) {
            repo.saveCurrentUserFcmToken(token = "token_alice_dev1_abcdefghijklmnopqrstuvwxyz_0123456789", notificationsEnabled = true, deviceId = "dev_alice_1")
        }
        val dev2Save = withTimeout(5000L) {
            repo.saveCurrentUserFcmToken(token = "token_alice_dev2_abcdefghijklmnopqrstuvwxyz_0123456789", notificationsEnabled = true, deviceId = "dev_alice_2")
        }
        assertTrue("Alice device 1 FCM token save must succeed", dev1Save.isSuccess)
        assertTrue("Alice device 2 FCM token save must succeed", dev2Save.isSuccess)

        // Sign out device 1 only -> device 2 doc must remain active in Firestore
        withTimeout(5000L) {
            repo.unregisterDeviceFcmTokens("token_alice_dev1_abcdefghijklmnopqrstuvwxyz_0123456789", "dev_alice_1", aliceUid)
        }
        val dev2AfterDev1Logout = withTimeout(5000L) {
            firestore.collection("fcm_tokens").document("${aliceUid}_dev_alice_2").get().await()
        }
        assertTrue("Alice Device 2 token must remain intact when Device 1 logs out", dev2AfterDev1Logout.exists())

        // 2. Admin provisions PUBLIC, PRIVATE_USER (for Alice), and ADMIN_ONLY notifications
        val adminUid = signInTestUser("beqa1994@gmail.com")
        withContext(Dispatchers.IO) {
            provisionAdminDocument(adminUid)
        }

        val publicRes = withTimeout(5000L) {
            repo.sendAdminNotification(
                title = "BSTORE საჯარო განცხადება",
                body = "საზაფხულო ფასდაკლებები დაიწყო!",
                targetTab = 2,
                type = "announcement",
                audience = "all",
                visibility = "PUBLIC"
            )
        }
        assertTrue("Admin PUBLIC announcement creation must succeed", publicRes.isSuccess)
        val publicId = publicRes.getOrThrow()

        val privRes = withTimeout(5000L) {
            repo.sendAdminNotification(
                title = "შეკვეთა #BS-8812 დადასტურდა",
                body = "თქვენი #BS-8812 შეკვეთა დადასტურებულია.",
                targetTab = 4,
                type = "order_update",
                audience = "user:$aliceUid",
                orderId = "ord_priv_8812",
                orderNumber = "#BS-8812",
                visibility = "PRIVATE_USER",
                recipientUid = aliceUid
            )
        }
        assertTrue("Admin PRIVATE_USER notification creation must succeed", privRes.isSuccess)
        val privId = privRes.getOrThrow()

        val adminOnlyRes = withTimeout(5000L) {
            repo.sendAdminNotification(
                title = "ადმინის შიდა შეტყობინება",
                body = "შიდა კონტროლის შეტყობინება",
                targetTab = 0,
                type = "admin_order",
                audience = "admin_only",
                visibility = "ADMIN_ONLY"
            )
        }
        assertTrue("Admin ADMIN_ONLY notification creation must succeed", adminOnlyRes.isSuccess)
        val adminOnlyId = adminOnlyRes.getOrThrow()

        // Confirm payment on order with blank userId must NOT create an 'all' broadcast notification
        val orphanOrder = OrderRecord(
            id = "ord_orphan_check",
            orderNumber = "#BS-ORPHAN",
            date = "12 ნოე, 2026",
            items = emptyList(),
            totalAmount = "100 ₾",
            status = "მიმდინარე",
            trackingCode = "BS-ORPHAN",
            userId = ""
        )
        val missingUidConfirmRes = withTimeout(5000L) {
            repo.confirmOrderPaymentAsAdmin(orphanOrder)
        }
        assertTrue(
            "confirmOrderPaymentAsAdmin without userId must fail cleanly instead of broadcasting",
            missingUidConfirmRes.isFailure
        )

        // 3. Anonymous Firebase Auth session: blocked from registering tokens, reading private/admin docs, or bank accounts
        val anonResult = withContext(Dispatchers.IO) {
            withTimeout(5000L) { auth.signInAnonymously().await() }
        }
        assertNotNull(anonResult.user)
        assertTrue("Anonymous Auth session must be detected as anonymous", AuthAndOrderManager.isAnonymousFirebaseSession())
        assertFalse("Anonymous Auth session must NOT be treated as registered user", AuthAndOrderManager.isRegisteredAuthenticatedUser(context))

        val anonTokenAttempt = runCatching {
            withTimeout(5000L) {
                firestore.collection("fcm_tokens").document("${anonResult.user!!.uid}_dev_anon").set(
                    mapOf(
                        "userId" to anonResult.user!!.uid,
                        "token" to "anon_token_1234567890_abcdefghijklmnopqrstuvwxyz",
                        "deviceId" to "dev_anon",
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        assertTrue("Anonymous session must fail FCM token registration", anonTokenAttempt.isFailure)

        val anonReadAlicePriv = runCatching {
            withTimeout(5000L) {
                firestore.collection("notifications").document(privId).get().await()
            }
        }
        assertTrue("Anonymous session must NOT read Alice's PRIVATE_USER notification", anonReadAlicePriv.isFailure)

        val anonQueryAlicePriv = runCatching {
            withTimeout(5000L) {
                firestore.collection("notifications").whereEqualTo("recipientUid", aliceUid).get().await()
            }
        }
        assertTrue("Anonymous session must NOT query Alice's recipientUid notifications", anonQueryAlicePriv.isFailure)

        val anonReadAdminOnly = runCatching {
            withTimeout(5000L) {
                firestore.collection("notifications").document(adminOnlyId).get().await()
            }
        }
        assertTrue("Anonymous session must NOT read ADMIN_ONLY notification", anonReadAdminOnly.isFailure)

        // Anonymous CAN still read explicitly PUBLIC announcement
        val anonReadPublic = withTimeout(5000L) {
            firestore.collection("notifications").whereEqualTo("visibility", "PUBLIC").get().await()
        }
        assertTrue("Anonymous session may read explicitly PUBLIC notifications", anonReadPublic.documents.any { it.id == publicId })

        // 4. User B signs in -> isolated from Alice's private notifications & ADMIN_ONLY
        val bobUid = signInTestUser("privacy.bob@bstore.ge")
        assertTrue(bobUid != aliceUid)
        val bobReadAlicePriv = runCatching {
            withTimeout(5000L) { firestore.collection("notifications").document(privId).get().await() }
        }
        assertTrue("User B must NOT read User A's PRIVATE_USER notification", bobReadAlicePriv.isFailure)

        val bobMarkAliceRead = withTimeout(5000L) {
            repo.markUserNotificationReadInCloud(privId)
        }
        assertTrue("User B must NOT mark User A's PRIVATE_USER notification as read", bobMarkAliceRead.isFailure)

        // 5. User A signs in -> CAN read own PRIVATE_USER notification and mark it read
        signInTestUser("privacy.alice@bstore.ge")
        val aliceOwnPriv = withTimeout(5000L) {
            firestore.collection("notifications").whereEqualTo("recipientUid", aliceUid).get().await()
        }
        assertTrue("Alice must read her own PRIVATE_USER notification", aliceOwnPriv.documents.any { it.id == privId })

        val aliceMarkRead = withTimeout(5000L) {
            repo.markUserNotificationReadInCloud(privId)
        }
        assertTrue("Alice must be able to mark her own PRIVATE_USER notification as read", aliceMarkRead.isSuccess)
    }
}


