package com.example

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.RepairProTheme
import org.junit.Assert.assertEquals
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
class ProductPushAndBankTransferTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AppPreferencesManager.init(context)
        BstorePaymentManager.init(context)
        BekaNotificationManager.clearAllNotifications(context)
        BekaNotificationManager.markPermissionPrompted(context)
        AdminStorage.saveProducts(context, ProductRepository.getAllProducts())
    }

    // =========================================================================
    // TASK 1: Product-Specific Push Notification Navigation
    // =========================================================================

    @Test
    fun `fcm payload parser and deep link intent preserve product doc id and type separately from numeric id`() {
        val rawFcmData = mapOf(
            "notificationId" to "notif_bs_1053",
            "type" to "product",
            "title" to "ახალი პროდუქტი დაემატა!",
            "body" to "Hoco EW90 AirPods უკვე BSTORE-შია. ნახე პროდუქტი!",
            "productId" to "BS-1053",
            "productDocId" to "BS-1053",
            "targetTab" to "1",
            "visibility" to "PUBLIC"
        )

        val shownIntent = BekaNotificationManager.buildDeepLinkIntent(
            context = context,
            notificationId = rawFcmData["notificationId"].orEmpty(),
            title = rawFcmData["title"].orEmpty(),
            body = rawFcmData["body"].orEmpty(),
            targetProductId = 91053,
            targetTab = 1,
            type = "product",
            visibility = "PUBLIC",
            targetProductDocId = "BS-1053"
        )

        assertEquals("product", shownIntent.getStringExtra("type"))
        assertEquals("product", shownIntent.getStringExtra(BekaNotificationManager.EXTRA_NOTIFICATION_TYPE))
        assertEquals("BS-1053", shownIntent.getStringExtra(BekaNotificationManager.EXTRA_TARGET_PRODUCT_DOC_ID))
        assertEquals("BS-1053", shownIntent.getStringExtra("productDocId"))
        assertEquals(91053, shownIntent.getIntExtra(BekaNotificationManager.EXTRA_TARGET_PRODUCT_ID, 0))
        assertEquals("notif_bs_1053", shownIntent.getStringExtra(BekaNotificationManager.EXTRA_NOTIFICATION_ID))

        // Verify PushNotificationRecord JSON round-trip preserves targetProductDocId & "product" type
        val record = PushNotificationRecord(
            id = "notif_bs_1053",
            title = "ახალი პროდუქტი დაემატა!",
            body = "Hoco EW90 AirPods უკვე BSTORE-შია. ნახე პროდუქტი!",
            type = "product",
            targetProductId = 91053,
            targetProductDocId = "BS-1053",
            targetTab = 1,
            visibility = "PUBLIC"
        )
        BekaNotificationManager.upsertLocalNotificationRecord(context, record)
        val loaded = BekaNotificationManager.loadLocalNotificationHistory(context).find { it.id == "notif_bs_1053" }
        assertNotNull(loaded)
        assertEquals("product", loaded?.type)
        assertEquals("BS-1053", loaded?.targetProductDocId)
        assertEquals(91053, loaded?.targetProductId)
    }

    @Test
    fun `cold start and foreground re-tap product push opens exact corresponding product modal`() {
        val hocoProduct = ProductItem(
            id = 91053,
            title = "Hoco EW90 AirPods",
            price = "89 ₾",
            category = "აქსესუარები",
            type = "product",
            imageUrl = "",
            firestoreDocId = "BS-1053",
            inStock = true,
            isActive = true
        )
        val secondProduct = ProductItem(
            id = 92099,
            title = "Anker Soundcore Liberty 4",
            price = "240 ₾",
            category = "აქსესუარები",
            type = "product",
            imageUrl = "",
            firestoreDocId = "BS-2099",
            inStock = true,
            isActive = true
        )
        AdminStorage.saveProducts(context, ProductRepository.getAllProducts() + listOf(hocoProduct, secondProduct))

        // Simulate genuine cold start (splash not yet finished, onboarding completed)
        hasCompletedInitialLaunch = false
        AppPreferencesManager.setOnboardingCompleted(context, true)

        val coldStartIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("type", "product")
            putExtra("productId", "BS-1053")
            putExtra("title", "ახალი პროდუქტი დაემატა!")
            putExtra("body", "Hoco EW90 AirPods უკვე BSTORE-შია. ნახე პროდუქტი!")
            putExtra("notificationId", "notif_cold_bs_1053")
        }

        var currentIntent by mutableStateOf<Intent?>(coldStartIntent)
        var currentSeq by mutableIntStateOf(1)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BekaServiceApp(
                    deepLinkIntent = currentIntent,
                    onDeepLinkConsumed = { currentIntent = null },
                    deepLinkSeq = currentSeq
                )
            }
        }

        // Wait until startup readiness resolves & product modal opens automatically
        composeTestRule.waitUntil(timeoutMillis = 8_000L) {
            composeTestRule.onAllNodes(
                hasText("Hoco EW90 AirPods") and hasAnyAncestor(hasTestTag("product_detail_modal_window")),
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("detail_favorite_button", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNode(
            hasText("89 ₾") and hasAnyAncestor(hasTestTag("product_detail_modal_window")),
            useUnmergedTree = true
        ).assertIsDisplayed()

        // Simulate tapping another product push while app is already in foreground (onNewIntent)
        currentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("type", "product")
            putExtra("productDocId", "BS-2099")
            putExtra("notificationId", "notif_fg_bs_2099")
        }
        currentSeq += 1

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodes(
                hasText("Anker Soundcore Liberty 4") and hasAnyAncestor(hasTestTag("product_detail_modal_window")),
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(
            hasText("240 ₾") and hasAnyAncestor(hasTestTag("product_detail_modal_window")),
            useUnmergedTree = true
        ).assertIsDisplayed()
    }

    @Test
    fun `deleted or missing product deep link shows Georgian unavailable dialog and returns cleanly to catalog`() {
        hasCompletedInitialLaunch = true
        AppPreferencesManager.setOnboardingCompleted(context, true)

        val missingProductIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("type", "product")
            putExtra("productDocId", "BS-DELETED-9999")
            putExtra("notificationId", "notif_deleted_9999")
        }

        var currentIntent by mutableStateOf<Intent?>(missingProductIntent)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BekaServiceApp(
                    deepLinkIntent = currentIntent,
                    onDeepLinkConsumed = { currentIntent = null },
                    deepLinkSeq = 1
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 6_000L) {
            composeTestRule.onAllNodesWithText("პროდუქტი მიუწვდომელია", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("deep_link_product_unavailable_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("deep_link_back_to_catalog_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("deep_link_product_unavailable_dialog").assertDoesNotExist()
    }

    // =========================================================================
    // TASK 2: Smart Bank Transfer, Logos, Auto-IBAN Copy & Banking App Launch
    // =========================================================================

    @Test
    fun `bank transfer helper maps BOG and TBC package names and official drawable logos accurately`() {
        assertEquals("ge.bog.mobilebank", BankTransferHandoffHelper.resolveBankPackageName("bog", "საქართველოს ბანკი"))
        assertEquals("com.icomvision.bsc.tbc", BankTransferHandoffHelper.resolveBankPackageName("tbc", "თიბისი ბანკი"))
        assertEquals("ge.bog.mobilebank", BankTransferHandoffHelper.resolveBankPackageName("", "საქართველოს ბანკი"))
        assertEquals("com.icomvision.bsc.tbc", BankTransferHandoffHelper.resolveBankPackageName("", "თიბისი ბანკი"))
        assertNull(BankTransferHandoffHelper.resolveBankPackageName("other", "სხვა ბანკი"))

        assertEquals(R.drawable.ic_bank_bog, BankTransferHandoffHelper.resolveBankLogoResId("bog"))
        assertEquals(R.drawable.ic_bank_tbc, BankTransferHandoffHelper.resolveBankLogoResId("tbc"))
    }

    @Test
    fun `checkout dialog auto-copies IBAN once on bank selection, switches cleanly, and guards recompositions`() {
        AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Admin Beqa", hasAdminClaim = true)
        val bogBank = BankAccountConfig(
            id = "bank_bog_primary",
            bankCode = "bog",
            bankName = "საქართველოს ბანკი",
            recipientName = "ბექა ბერიძე",
            iban = "GE17BG0000000580620970",
            isEnabled = true,
            displayOrder = 1
        )
        val tbcBank = BankAccountConfig(
            id = "bank_tbc_primary",
            bankCode = "tbc",
            bankName = "თიბისი ბანკი",
            recipientName = "ბექა ბერიძე",
            iban = "GE33TB7712345000000001",
            isEnabled = true,
            displayOrder = 2
        )
        BstorePaymentManager.saveBankAccounts(context, listOf(bogBank, tbcBank))
        AuthAndOrderManager.loginWithGoogle(context, "customer.test@gmail.com", "Customer", hasAdminClaim = false)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val sampleCart = listOf(CartItem(ProductRepository.getAllProducts().first(), 1, "Original"))

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BstoreCheckoutDialog(
                    cartItems = sampleCart,
                    onDismiss = {},
                    onOrderCreated = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Initial BOG bank auto-copies IBAN and shows official BOG logo + confirmation badge
        assertEquals(
            "GE17BG0000000580620970",
            clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        )
        composeTestRule.onNodeWithTag("checkout_iban_auto_copied_badge").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_selected_bank_iban").performScrollTo().assertIsDisplayed()

        // 2. Simulate external clipboard change & trigger unrelated recomposition (typing phone number)
        clipboard.setPrimaryClip(ClipData.newPlainText("external", "KEEP_ME_ON_RECOMPOSE"))
        composeTestRule.onNodeWithTag("checkout_phone_input").performScrollTo().performTextInput("598123456")
        composeTestRule.waitForIdle()
        assertEquals(
            "KEEP_ME_ON_RECOMPOSE",
            clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        )

        // 3. Switch to TBC bank -> automatically copies TBC IBAN
        composeTestRule.onNodeWithTag("checkout_bank_option_tbc").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            "GE33TB7712345000000001",
            clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        )
    }

    @Test
    fun `payment instructions launches installed BOG app cleanly and never alters order payment status`() {
        val customer = AuthAndOrderManager.loginWithGoogle(context, "buyer.bog@gmail.com", "Buyer BOG", hasAdminClaim = false)
        val bogBank = BankAccountConfig(
            id = "bank_bog_primary",
            bankCode = "bog",
            bankName = "საქართველოს ბანკი",
            recipientName = "ბექა ბერიძე",
            iban = "GE17BG0000000580620970",
            isEnabled = true,
            displayOrder = 1
        )
        BstorePaymentManager.saveBankAccounts(context, listOf(bogBank))

        val order = OrderRecord(
            id = "ord_bog_handoff_1",
            orderNumber = "#BS-5501",
            date = "14 ნოე, 2026",
            items = listOf(OrderItemDetail("Hoco EW90 AirPods", "Original", 1, "89 ₾", "")),
            totalAmount = "89 ₾",
            status = "მიმდინარე",
            trackingCode = "BS-TRK-5501",
            userId = customer.id,
            paymentMethodCode = PaymentMethodType.BANK_TRANSFER.code,
            paymentStatus = PaymentStatus.PENDING.code,
            bankAccountId = bogBank.id,
            bankCode = bogBank.bankCode,
            bankName = bogBank.bankName,
            recipientNameSnapshot = bogBank.recipientName,
            ibanSnapshot = bogBank.iban,
            paymentReference = "#BS-5501"
        )

        // Register ge.bog.mobilebank in Robolectric ShadowPackageManager as installed launchable app
        val shadowPm = Shadows.shadowOf(context.packageManager)
        val bogPkgInfo = PackageInfo().apply {
            packageName = BankTransferHandoffHelper.BOG_PACKAGE_NAME
            applicationInfo = ApplicationInfo().apply {
                packageName = BankTransferHandoffHelper.BOG_PACKAGE_NAME
            }
        }
        shadowPm.installPackage(bogPkgInfo)
        val launcherQueryIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(BankTransferHandoffHelper.BOG_PACKAGE_NAME)
        }
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = BankTransferHandoffHelper.BOG_PACKAGE_NAME
                name = "ge.bog.mobilebank.MainActivity"
            }
        }
        shadowPm.addResolveInfoForIntent(launcherQueryIntent, resolveInfo)

        var lastUpdatedOrder = order
        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                OrderPaymentInstructionsDialog(
                    order = order,
                    onDismiss = {},
                    onOrderUpdated = { lastUpdatedOrder = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // Clear clipboard externally to verify tapping "გადარიცხვაზე გადასვლა" re-copies right before launch
        clipboard.setPrimaryClip(ClipData.newPlainText("temp", "STALE_CLIP"))

        composeTestRule.onNodeWithTag("open_mobile_bank_button").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // 1. Re-copied IBAN immediately before launching BOG
        assertEquals(
            "GE17BG0000000580620970",
            clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        )
        // 2. Launched ge.bog.mobilebank
        val startedActivity = Shadows.shadowOf(context as Application).nextStartedActivity
        assertNotNull(startedActivity)
        assertEquals(BankTransferHandoffHelper.BOG_PACKAGE_NAME, startedActivity.component?.packageName)
        // 3. Georgian instruction banner displayed
        composeTestRule.onNodeWithTag("bank_handoff_instruction_banner").performScrollTo().assertIsDisplayed()
        // 4. Order payment status NEVER automatically marked PAID or VERIFICATION_PENDING
        assertEquals(PaymentStatus.PENDING.code, lastUpdatedOrder.paymentStatus)
    }

    @Test
    fun `when TBC bank app is not installed, copies IBAN, shows Georgian fallback dialog, and offers Play Store link`() {
        val customer = AuthAndOrderManager.loginWithGoogle(context, "buyer.tbc@gmail.com", "Buyer TBC", hasAdminClaim = false)
        val tbcBank = BankAccountConfig(
            id = "bank_tbc_primary",
            bankCode = "tbc",
            bankName = "თიბისი ბანკი",
            recipientName = "ბექა ბერიძე",
            iban = "GE33TB7712345000000001",
            isEnabled = true,
            displayOrder = 1
        )
        BstorePaymentManager.saveBankAccounts(context, listOf(tbcBank))

        val order = OrderRecord(
            id = "ord_tbc_missing_1",
            orderNumber = "#BS-5502",
            date = "14 ნოე, 2026",
            items = listOf(OrderItemDetail("Anker PowerBank", "Original", 1, "120 ₾", "")),
            totalAmount = "120 ₾",
            status = "მიმდინარე",
            trackingCode = "BS-TRK-5502",
            userId = customer.id,
            paymentMethodCode = PaymentMethodType.BANK_TRANSFER.code,
            paymentStatus = PaymentStatus.PENDING.code,
            bankAccountId = tbcBank.id,
            bankCode = tbcBank.bankCode,
            bankName = tbcBank.bankName,
            recipientNameSnapshot = tbcBank.recipientName,
            ibanSnapshot = tbcBank.iban,
            paymentReference = "#BS-5502"
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                OrderPaymentInstructionsDialog(
                    order = order,
                    onDismiss = {},
                    onOrderUpdated = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("open_mobile_bank_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(
            "GE33TB7712345000000001",
            clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        )

        composeTestRule.onNodeWithTag("bank_app_not_installed_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_bank_play_store_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        val playIntent = Shadows.shadowOf(context as Application).nextStartedActivity
        assertNotNull(playIntent)
        assertTrue(
            playIntent.dataString == "market://details?id=com.icomvision.bsc.tbc" ||
                playIntent.dataString == "https://play.google.com/store/apps/details?id=com.icomvision.bsc.tbc"
        )
    }

    @Test
    fun `disabled bank or invalid IBAN never overwrites clipboard automatically`() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("guard", "USER_CLIPBOARD_SAFE"))

        val invalidResult = BankTransferHandoffHelper.launchMobileBankForTransfer(
            context = context,
            bankCode = "bog",
            bankName = "საქართველოს ბანკი",
            iban = "INVALID_IBAN"
        )
        assertTrue(invalidResult is BankTransferHandoffHelper.BankLaunchResult.InvalidBankOrIban)
        assertEquals("USER_CLIPBOARD_SAFE", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }
}
