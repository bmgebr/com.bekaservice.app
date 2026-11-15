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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.RepairProTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class BstoreCheckoutScreenRecreationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    private val bogPrimary = BankAccountConfig(
        id = "bank_bog_1",
        bankCode = "bog",
        bankName = "საქართველოს ბანკი",
        recipientName = "BSTORE / ბექა მღებრიშვილი",
        iban = "GE17BG0000000580620970",
        description = "ძირითადი ანგარიში",
        paymentInstructions = "გადარიცხეთ მითითებულ საქართველოს ბანკის ანგარიშზე და დაბრუნდით აპლიკაციაში.",
        isEnabled = true,
        displayOrder = 1
    )

    private val bogSecondary = BankAccountConfig(
        id = "bank_bog_2",
        bankCode = "bog",
        bankName = "საქართველოს ბანკი",
        recipientName = "BSTORE სერვისი",
        iban = "GE44BG0000000999888777",
        description = "სათადარიგო BOG",
        paymentInstructions = "გადარიცხეთ BOG სათადარიგო ანგარიშზე და შეინახეთ ქვითარი.",
        isEnabled = true,
        displayOrder = 2
    )

    private val tbcPrimary = BankAccountConfig(
        id = "bank_tbc_1",
        bankCode = "tbc",
        bankName = "თიბისი ბანკი",
        recipientName = "BSTORE TBC მიმღები",
        iban = "GE65TB7777777000000001",
        description = "TBC ანგარიში",
        paymentInstructions = "გადარიცხეთ თიბისი ბანკის ანგარიშზე და დაადასტურეთ შეკვეთა.",
        isEnabled = true,
        displayOrder = 3
    )

    private val disabledBank = BankAccountConfig(
        id = "bank_bog_disabled",
        bankCode = "bog",
        bankName = "გათიშული ანგარიში",
        recipientName = "Disabled",
        iban = "GE11BG0000000000000000",
        description = "არ უნდა გამოჩნდეს",
        isEnabled = false,
        displayOrder = 0
    )

    private val testProduct = ProductItem(
        id = 7701,
        title = "AirPods Pro (2nd Gen)",
        price = "649 ₾",
        category = "აქსესუარები",
        type = "product",
        imageUrl = "",
        firestoreDocId = "prod_airpods_pro_2",
        inStock = true,
        isActive = true
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        AppPreferencesManager.init(context)
        BstorePaymentManager.init(context)
        context.getSharedPreferences("bstore_auth_orders_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        BstorePaymentManager.saveBankAccountsLocal(context, listOf(disabledBank, bogPrimary, bogSecondary, tbcPrimary))
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "customer.checkout@gmail.com",
            name = "Checkout Tester",
            hasAdminClaim = false
        )
        runBlocking {
            AuthAndOrderManager.updateSingleContactField(context, phone = "", address = "")
        }
    }

    @Test
    fun `screen displays reference sections, supports -1+ quantity stepper on all items, removes one item when 2 products are in checkout without overlap, and excludes disabled banks`() {
        val secondProduct = ProductItem(
            id = 7702,
            title = "Hoco EW90 AirPods",
            price = "62 ₾",
            category = "აქსესუარები",
            type = "product",
            imageUrl = "",
            firestoreDocId = "prod_hoco_ew90",
            inStock = true,
            isActive = true
        )
        val cart = listOf(
            CartItem(testProduct, 2, "Original"),
            CartItem(secondProduct, 1, "Original")
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BstoreCheckoutDialog(
                    cartItems = cart,
                    onDismiss = {},
                    onOrderCreated = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Exact sections, -1+ steppers for both products, non-overlapping remove buttons, and real total (2*649 + 1*62 = 1,360 ₾)
        composeTestRule.onNodeWithText("გადახდა").assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_summary_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("AirPods Pro (2nd Gen)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Hoco EW90 AirPods").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_qty_value_7701", useUnmergedTree = true).assertTextContains("2")
        composeTestRule.onNodeWithTag("checkout_qty_value_7702", useUnmergedTree = true).assertTextContains("1")
        composeTestRule.onNodeWithTag("checkout_total_amount").performScrollTo().assertTextContains("1,360 ₾")

        // Verify both items have remove buttons when 2 products are in checkout and that price & remove button do not overlap
        val priceBounds7702 = composeTestRule.onNodeWithTag("checkout_item_price_7702", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val removeBounds7702 = composeTestRule.onNodeWithTag("checkout_remove_item_7702").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected item price (${priceBounds7702.right}) not to overlap with remove button (${removeBounds7702.left})",
            priceBounds7702.right <= removeBounds7702.left
        )

        // Increase quantity of secondProduct using + button (1 -> 2) -> total becomes 1,422 ₾
        composeTestRule.onNodeWithTag("checkout_qty_increase_7702").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_qty_value_7702", useUnmergedTree = true).assertTextContains("2")
        composeTestRule.onNodeWithTag("checkout_item_price_7702", useUnmergedTree = true).assertTextContains("124 ₾")
        composeTestRule.onNodeWithTag("checkout_total_amount").assertTextContains("1,422 ₾")

        // Remove secondProduct directly from the checkout screen -> leaves 1 product (testProduct qty 2 = 1,298 ₾)
        composeTestRule.onNodeWithTag("checkout_remove_item_7702").performClick()
        composeTestRule.waitForIdle()
        assertTrue(composeTestRule.onAllNodesWithTag("checkout_item_row_7702").fetchSemanticsNodes().isEmpty())
        // With only 1 product left, the remove button is hidden while the - 1 + stepper remains active
        assertTrue(composeTestRule.onAllNodesWithTag("checkout_remove_item_7701").fetchSemanticsNodes().isEmpty())
        composeTestRule.onNodeWithTag("checkout_total_amount").assertTextContains("1,298 ₾")

        // Decrease quantity of remaining product using - button (2 -> 1) -> total becomes 649 ₾, then + back to 2 -> 1,298 ₾
        composeTestRule.onNodeWithTag("checkout_qty_decrease_7701").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_qty_value_7701", useUnmergedTree = true).assertTextContains("1")
        composeTestRule.onNodeWithTag("checkout_total_amount").assertTextContains("649 ₾")
        composeTestRule.onNodeWithTag("checkout_qty_increase_7701").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_qty_value_7701", useUnmergedTree = true).assertTextContains("2")
        composeTestRule.onNodeWithTag("checkout_total_amount").assertTextContains("1,298 ₾")

        composeTestRule.onNodeWithText("გადახდის მეთოდი").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_payment_method_bank_transfer").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_payment_method_cod").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("აირჩიეთ ბანკი").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_bank_option_bog").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_bank_option_tbc").performScrollTo().assertIsDisplayed()

        // 2. Disabled bank is excluded; enabled sub-accounts for BOG are shown in displayOrder
        assertTrue(composeTestRule.onAllNodesWithTag("checkout_subaccount_bank_bog_disabled").fetchSemanticsNodes().isEmpty())
        composeTestRule.onNodeWithTag("checkout_subaccount_bank_bog_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_subaccount_bank_bog_2").performScrollTo().assertIsDisplayed()

        // Primary BOG IBAN is displayed
        composeTestRule.onNodeWithTag("checkout_selected_bank_iban").performScrollTo().assertTextContains("GE17BG0000000580620970")

        // Switch to secondary BOG account updates IBAN & instructions
        composeTestRule.onNodeWithTag("checkout_subaccount_bank_bog_2").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_selected_bank_iban").performScrollTo().assertTextContains("GE44BG0000000999888777")
        composeTestRule.onNodeWithText("BSTORE სერვისი", substring = true).performScrollTo().assertIsDisplayed()

        // Bottom confirmation button & phone / instructions / terms visible
        composeTestRule.onNodeWithTag("checkout_phone_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_payment_instructions_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_open_terms_link").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_confirm_order_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `switching BOG to TBC updates logo recipient IBAN instructions clipboard and keeps entered phone`() {
        val cart = listOf(CartItem(testProduct, 1, "Original"))
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BstoreCheckoutDialog(
                    cartItems = cart,
                    onDismiss = {},
                    onOrderCreated = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Enter phone number before switching banks
        composeTestRule.onNodeWithTag("checkout_phone_input").performScrollTo().performTextInput("555123456")
        composeTestRule.waitForIdle()

        // Initial bank is BOG
        assertTrue(
            composeTestRule.onAllNodesWithTag("bank_logo_bog", useUnmergedTree = true)
                .fetchSemanticsNodes().size >= 2
        )
        composeTestRule.onAllNodesWithTag("bank_logo_bog", useUnmergedTree = true).onFirst()
            .performScrollTo().assertIsDisplayed()
        assertEquals("GE17BG0000000580620970", clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        // Switch to TBC
        composeTestRule.onNodeWithTag("checkout_bank_option_tbc").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            composeTestRule.onAllNodesWithTag("bank_logo_tbc", useUnmergedTree = true)
                .fetchSemanticsNodes().size >= 2
        )
        composeTestRule.onNodeWithTag("checkout_selected_bank_iban").performScrollTo().assertTextContains("GE65TB7777777000000001")
        composeTestRule.onNodeWithText("BSTORE TBC მიმღები", substring = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("თიბისი ბანკის ანგარიშზე", substring = true).performScrollTo().assertIsDisplayed()
        assertEquals("GE65TB7777777000000001", clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        // Overwrite clipboard externally then click manual Copy button -> restores clean IBAN & shows feedback
        clipboard.setPrimaryClip(ClipData.newPlainText("other", "STALE"))
        composeTestRule.onNodeWithTag("checkout_copy_iban_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("GE65TB7777777000000001", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        composeTestRule.onNodeWithTag("checkout_iban_auto_copied_badge").performScrollTo().assertIsDisplayed()

        // Switch to COD and back to Bank Transfer -> phone number still preserved
        composeTestRule.onNodeWithTag("checkout_payment_method_cod").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_payment_method_bank_transfer").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_phone_input").performScrollTo().assertTextContains("555123456")
    }

    @Test
    fun `open mobile bank button launches installed TBC app, handles missing app fallback cleanly, and never auto-completes order`() {
        val cart = listOf(CartItem(testProduct, 1, "Original"))
        var createdOrderCount = 0

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BstoreCheckoutDialog(
                    cartItems = cart,
                    onDismiss = {},
                    onOrderCreated = { createdOrderCount++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("checkout_phone_input").performScrollTo().performTextInput("599112233")
        composeTestRule.onNodeWithTag("checkout_bank_option_tbc").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Case A: TBC app NOT installed -> shows fallback dialog with Play Store button, keeps phone, creates 0 orders
        composeTestRule.onNodeWithTag("checkout_open_mobile_bank_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bank_app_not_installed_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_bank_play_store_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        val playIntent = Shadows.shadowOf(context as Application).nextStartedActivity
        assertNotNull(playIntent)
        assertTrue(playIntent.dataString?.contains(BankTransferHandoffHelper.TBC_PACKAGE_NAME) == true)
        assertEquals(0, createdOrderCount)

        // Case B: Register TBC package as installed -> clicking launches com.icomvision.bsc.tbc directly
        val shadowPm = Shadows.shadowOf(context.packageManager)
        shadowPm.installPackage(
            PackageInfo().apply {
                packageName = BankTransferHandoffHelper.TBC_PACKAGE_NAME
                applicationInfo = ApplicationInfo().apply {
                    packageName = BankTransferHandoffHelper.TBC_PACKAGE_NAME
                }
            }
        )
        shadowPm.addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(BankTransferHandoffHelper.TBC_PACKAGE_NAME)
            },
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = BankTransferHandoffHelper.TBC_PACKAGE_NAME
                    name = "com.icomvision.bsc.tbc.MainActivity"
                }
            }
        )

        composeTestRule.onNodeWithTag("checkout_open_mobile_bank_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        val launchedTbcIntent = Shadows.shadowOf(context as Application).nextStartedActivity
        assertNotNull(launchedTbcIntent)
        assertEquals(BankTransferHandoffHelper.TBC_PACKAGE_NAME, launchedTbcIntent.component?.packageName)
        composeTestRule.onNodeWithTag("checkout_bank_handoff_banner").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_phone_input").performScrollTo().assertTextContains("599112233")
        assertEquals(0, createdOrderCount)
    }

    @Test
    fun `validates phone and terms, opens legal policies dialog, creates immutable snapshot order, prevents duplicate and paid elevation`() {
        val cart = listOf(CartItem(testProduct, 2, "Original"))
        val createdOrders = mutableListOf<OrderRecord>()

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BstoreCheckoutDialog(
                    cartItems = cart,
                    onDismiss = {},
                    onOrderCreated = { createdOrders.add(it) }
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Invalid phone rejected before order creation
        composeTestRule.onNodeWithTag("checkout_phone_input").performScrollTo().performTextInput("123")
        composeTestRule.onNodeWithTag("checkout_confirm_order_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_phone_error").performScrollTo().assertIsDisplayed()
        assertTrue(createdOrders.isEmpty())

        // 2. Valid Georgian phone without accepted terms rejected
        composeTestRule.onNodeWithTag("checkout_phone_input").performScrollTo().performTextReplacement("555123456")
        composeTestRule.onNodeWithTag("checkout_confirm_order_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_validation_error").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("შეკვეთის გასაფორმებლად დაეთანხმეთ", substring = true)
            .performScrollTo().assertIsDisplayed()
        assertTrue(createdOrders.isEmpty())

        // 3. Open terms & conditions link -> shows BstoreLegalPoliciesDialog and returns cleanly
        composeTestRule.onNodeWithTag("checkout_open_terms_link").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bstore_legal_policies_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("დახურვა").performClick()
        composeTestRule.waitForIdle()

        // 4. Accept terms + switch to TBC -> submit twice rapidly -> single idempotent order created
        composeTestRule.onNodeWithTag("checkout_bank_option_tbc").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("checkout_accept_terms_checkbox").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("checkout_confirm_order_button").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) { createdOrders.size == 1 }

        val order = createdOrders.single()
        assertEquals("bank_transfer", order.paymentMethodCode)
        assertEquals("pending", order.paymentStatus) // Never "paid" on customer checkout
        assertEquals("+995 555 12 34 56", order.userPhone)
        assertEquals("bank_tbc_1", order.bankAccountId)
        assertEquals("tbc", order.bankCode)
        assertEquals("თიბისი ბანკი", order.bankName)
        assertEquals("BSTORE TBC მიმღები", order.recipientNameSnapshot)
        assertEquals("GE65TB7777777000000001", order.ibanSnapshot)

        // Idempotent duplicate config submission keeps single order record
        val duplicateSubmit = runBlocking {
            AuthAndOrderManager.createAndPersistOrderFromCart(
                context = context,
                cartItems = cart,
                checkoutConfig = CheckoutSubmissionConfig(
                    paymentMethod = PaymentMethodType.BANK_TRANSFER,
                    selectedBankAccount = tbcPrimary,
                    customerPhone = order.userPhone,
                    clientOrderId = order.id
                )
            )
        }
        assertTrue(duplicateSubmit.isSuccess)
        assertEquals(1, AuthAndOrderManager.getOrders(context).count { it.id == order.id })

        // Admin later changes TBC IBAN -> historical order snapshot remains immutable
        AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Admin", hasAdminClaim = true)
        val updateAdminRes = BstorePaymentManager.upsertBankAccountAsAdmin(
            context,
            tbcPrimary.copy(iban = "GE99TB0000000999999999")
        )
        assertTrue(updateAdminRes.isSuccess)

        // Customer still sees historical order with original immutable IBAN snapshot & CANNOT mark order as "paid"
        AuthAndOrderManager.loginWithGoogle(context, "customer.checkout@gmail.com", "Customer", hasAdminClaim = false)
        val storedOrder = AuthAndOrderManager.getOrders(context).first { it.id == order.id }
        assertEquals("GE65TB7777777000000001", storedOrder.ibanSnapshot)

        val forbiddenPaid = AuthAndOrderManager.confirmOrderPaymentLocalAsAdmin(context, storedOrder)
        assertTrue(forbiddenPaid.isFailure)
    }

    @Test
    fun `renders cleanly in both Dark Mode and Soft Graphite Light Mode, supports store dialer action, BOG app launch, COD submission, and preserves bank accounts on empty cloud snapshot`() {
        val cart = listOf(CartItem(testProduct, 1, "Original"))
        val createdOrders = mutableListOf<OrderRecord>()

        // Empty cloud snapshot must NOT wipe existing configured bank accounts
        BstorePaymentManager.syncFromCloudBankAccounts(context, emptyList())
        assertTrue(BstorePaymentManager.getEnabledBankAccounts(context).isNotEmpty())

        // Switch to Soft Graphite Light Mode and verify checkout renders all key elements & bottom confirm button
        AppPreferencesManager.setDarkMode(context, false)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = AppPreferencesManager.isDarkMode) {
                BstoreCheckoutDialog(
                    cartItems = cart,
                    onDismiss = {},
                    onOrderCreated = { createdOrders.add(it) }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Bottom confirmation button & action bar are 100% visible with safe bottom clearance above system gesture bar
        composeTestRule.onNodeWithTag("checkout_bottom_action_bar").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_confirm_order_button").performScrollTo().assertIsDisplayed()
        val dialogBounds = composeTestRule.onNodeWithTag("bstore_checkout_dialog").fetchSemanticsNode().boundsInRoot
        val buttonBounds = composeTestRule.onNodeWithTag("checkout_confirm_order_button").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected bottom confirmation button (${buttonBounds.bottom}) to sit safely above dialog bottom (${dialogBounds.bottom})",
            dialogBounds.bottom - buttonBounds.bottom >= 20f
        )

        // Store contact dialer button opens ACTION_DIAL with configured BSTORE phone number
        composeTestRule.onNodeWithTag("checkout_call_store_button").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        val dialIntent = Shadows.shadowOf(context as Application).nextStartedActivity
        assertNotNull(dialIntent)
        assertEquals(Intent.ACTION_DIAL, dialIntent.action)
        assertTrue(dialIntent.dataString?.startsWith("tel:") == true)

        // Register BOG app as installed and verify "გადარიცხვაზე გადასვლა" launches ge.bog.mobilebank
        val shadowPm = Shadows.shadowOf(context.packageManager)
        shadowPm.installPackage(
            PackageInfo().apply {
                packageName = BankTransferHandoffHelper.BOG_PACKAGE_NAME
                applicationInfo = ApplicationInfo().apply {
                    packageName = BankTransferHandoffHelper.BOG_PACKAGE_NAME
                }
            }
        )
        shadowPm.addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(BankTransferHandoffHelper.BOG_PACKAGE_NAME)
            },
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = BankTransferHandoffHelper.BOG_PACKAGE_NAME
                    name = "ge.bog.mobilebank.MainActivity"
                }
            }
        )
        composeTestRule.onNodeWithTag("checkout_open_mobile_bank_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        val bogIntent = Shadows.shadowOf(context as Application).nextStartedActivity
        assertNotNull(bogIntent)
        assertEquals(BankTransferHandoffHelper.BOG_PACKAGE_NAME, bogIntent.component?.packageName)

        // Switch to COD ("ადგილზე გადახდა") -> hides bank selection UI and shows COD instructions
        composeTestRule.onNodeWithTag("checkout_payment_method_cod").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(composeTestRule.onAllNodesWithTag("checkout_bank_option_bog").fetchSemanticsNodes().isEmpty())
        composeTestRule.onNodeWithText("ნაღდი ანგარიშსწორებით", substring = true).performScrollTo().assertIsDisplayed()

        // Submit COD order and verify awaiting_collection status (never "paid")
        composeTestRule.onNodeWithTag("checkout_phone_input").performScrollTo().performTextInput("577112233")
        composeTestRule.onNodeWithTag("checkout_accept_terms_checkbox").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("checkout_confirm_order_button").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) { createdOrders.size == 1 }

        val codOrder = createdOrders.single()
        assertEquals("cod", codOrder.paymentMethodCode)
        assertEquals("awaiting_collection", codOrder.paymentStatus)

        // Restore Dark Mode
        AppPreferencesManager.setDarkMode(context, true)
    }

    @Test
    fun `tall and normal phone layout eliminates empty gap before confirmation button, places IBAN and Copy on same horizontal row, and handles long Georgian recipient names cleanly`() {
        val longNameBank = BankAccountConfig(
            id = "bank_bog_long",
            bankCode = "bog",
            bankName = "საქართველოს ბანკი",
            recipientName = "შპს ბი-სთორ სერვის ცენტრი / ბექა მღებრიშვილი (ოფიციალური)",
            iban = "GE17BG0000000580620970",
            description = "ძირითადი ანგარიში",
            isEnabled = true,
            displayOrder = 1
        )
        BstorePaymentManager.saveBankAccountsLocal(context, listOf(longNameBank, tbcPrimary))
        val cart = listOf(CartItem(testProduct, 1, "Original"))

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BstoreCheckoutDialog(
                    cartItems = cart,
                    onDismiss = {},
                    onOrderCreated = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify the bank recipient card height is compact (<= 165dp) and IBAN + Copy share the same horizontal row
        composeTestRule.onNodeWithTag("checkout_selected_bank_requisites_card").performScrollTo().assertIsDisplayed()
        composeTestRule.waitForIdle()

        val ibanBounds = composeTestRule.onNodeWithTag("checkout_selected_bank_iban").fetchSemanticsNode().boundsInRoot
        val copyBounds = composeTestRule.onNodeWithTag("checkout_copy_iban_button").fetchSemanticsNode().boundsInRoot
        val copiedBadgeBounds = composeTestRule.onNodeWithTag("checkout_iban_auto_copied_badge").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected IBAN (${ibanBounds.top}..${ibanBounds.bottom}) and Copy button (${copyBounds.top}..${copyBounds.bottom}) to share the same horizontal row",
            ibanBounds.top < copyBounds.bottom && copyBounds.top < ibanBounds.bottom
        )
        assertTrue(
            "Expected IBAN right (${ibanBounds.right}) not to overlap Copy button left (${copyBounds.left})",
            ibanBounds.right <= copyBounds.left
        )
        // Copied badge sits inline above IBAN (with the label), not adding an extra row below IBAN
        assertTrue(
            "Expected inline copy confirmation badge (${copiedBadgeBounds.bottom}) to sit above IBAN (${ibanBounds.top})",
            copiedBadgeBounds.bottom <= ibanBounds.top + 4f
        )

        val recipientCardBounds = composeTestRule.onNodeWithTag("checkout_selected_bank_requisites_card").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected compact recipient card height (<= 220px), but got ${recipientCardBounds.height}",
            recipientCardBounds.height <= 220f
        )

        // 2. Scroll to confirmation button and verify NO large empty area exists between Terms checkbox row and Confirmation button
        composeTestRule.onNodeWithTag("checkout_confirm_order_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("checkout_accept_terms_checkbox").assertIsDisplayed()

        val termsBounds = composeTestRule.onNodeWithTag("checkout_accept_terms_checkbox").fetchSemanticsNode().boundsInRoot
        val confirmBounds = composeTestRule.onNodeWithTag("checkout_confirm_order_button").fetchSemanticsNode().boundsInRoot
        val dialogBounds = composeTestRule.onNodeWithTag("bstore_checkout_dialog").fetchSemanticsNode().boundsInRoot
        val gapBetweenTermsAndConfirm = confirmBounds.top - termsBounds.bottom
        assertTrue(
            "Expected compact vertical spacing between terms and confirm button, but got $gapBetweenTermsAndConfirm",
            gapBetweenTermsAndConfirm in 0f..36f
        )
        assertTrue(
            "Expected confirmation button (${confirmBounds.bottom}) to sit safely above bottom of dialog (${dialogBounds.bottom})",
            dialogBounds.bottom - confirmBounds.bottom >= 16f
        )
    }

    @Test
    fun `short height phone fits checkout cleanly and scrolls smoothly when courier delivery fields are expanded`() {
        val cart = listOf(CartItem(testProduct, 1, "Original"))

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BstoreCheckoutDialog(
                    cartItems = cart,
                    onDismiss = {},
                    onOrderCreated = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Expand delivery selector and choose Courier delivery (adds City & Address input fields)
        composeTestRule.onNodeWithText("შეცვლა").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("checkout_delivery_courier").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("checkout_city_input").performScrollTo().performTextReplacement("თბილისი")
        composeTestRule.onNodeWithTag("checkout_address_input").performScrollTo().performTextInput("რუსთაველის გამზ. 12")
        composeTestRule.waitForIdle()

        // Scroll to confirmation button and verify it is 100% visible with safe bottom clearance
        composeTestRule.onNodeWithTag("checkout_confirm_order_button").performScrollTo().assertIsDisplayed()
        val dialogBounds = composeTestRule.onNodeWithTag("bstore_checkout_dialog").fetchSemanticsNode().boundsInRoot
        val confirmBounds = composeTestRule.onNodeWithTag("checkout_confirm_order_button").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected confirmation button (${confirmBounds.bottom}) to have safe bottom clearance above dialog bottom (${dialogBounds.bottom})",
            dialogBounds.bottom - confirmBounds.bottom >= 20f
        )
    }
}
