package com.example

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.RepairProTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UiRefinementRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bekaservice_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bekaservice_notif_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bstore_payment_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        AppPreferencesManager.init(context)
        AppPreferencesManager.setOnboardingCompleted(context, true)
        AppPreferencesManager.setDarkMode(context, true)
        AppPreferencesManager.setLanguage(context, AppLanguage.KA)
        BekaNotificationManager.markPermissionPrompted(context)
        hasCompletedInitialLaunch = true
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `bottom nav displays singular ფასდაკლება label and home categories ყველა navigates to catalog`() {
        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BekaServiceApp(deepLinkIntent = null, onDeepLinkConsumed = {})
            }
        }
        composeTestRule.waitForIdle()

        // 1. Bottom navigation label is "ფასდაკლება" (not "ფასდაკლებები")
        composeTestRule.onNodeWithText("ფასდაკლება", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("ფასდაკლებები", useUnmergedTree = true).assertCountEquals(0)

        // 2. Scroll Home to popular section title and verify updated title
        composeTestRule.onNodeWithTag("home_popular_products_title", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("ყველაზე მოთხოვნადი პროდუქტები", useUnmergedTree = true).assertIsDisplayed()

        // 3. Tapping "ყველა" beside "კატეგორიები" navigates to Catalog tab
        composeTestRule.onNodeWithTag("home_categories_see_all_button", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        // Verify Catalog sort/filter UI is rendered (Tab 1)
        composeTestRule.onNodeWithText("სორტირება:", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `product photo card has single readable cart CTA and independent card vs favorite vs cart actions`() {
        val discountedProduct = ProductRepository.getAllProducts().first().copy(
            id = 701,
            title = "iPhone 16 Pro Max OLED ეკრანი",
            price = "234 ₾",
            originalPrice = "390 ₾",
            discountPercent = 40,
            inStock = true,
            isFavorite = false
        )

        var detailsClicks = 0
        var cartClicks = 0
        var favClicks = 0

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProductPhotoCard(
                    product = discountedProduct,
                    onClick = { detailsClicks++ },
                    onAddToCart = { cartClicks++ },
                    onToggleFavorite = { favClicks++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Old truncated button must be gone; single "კალათაში" CTA must be visible
        composeTestRule.onAllNodesWithText("დეტალურად", useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onNodeWithTag("add_to_cart_button_701", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("კალათაში", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("-40%", useUnmergedTree = true).assertIsDisplayed()

        // Tapping favorite does not trigger details or cart
        composeTestRule.onNodeWithTag("favorite_button_701", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, favClicks)
        assertEquals(0, detailsClicks)
        assertEquals(0, cartClicks)

        // Tapping Add to Cart triggers cart only
        composeTestRule.onNodeWithTag("add_to_cart_button_701", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, cartClicks)
        assertEquals(0, detailsClicks)

        // Tapping card surface opens details
        composeTestRule.onNodeWithTag("product_card_701", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, detailsClicks)
    }

    @Test
    fun `discounts screen shows compact სპეციალური შეთავაზებები header and filters only active discounts`() {
        val activePromo = ProductRepository.getAllProducts().first().copy(
            id = 801,
            title = "Samsung S24 Ultra OLED მოდული",
            price = "400 ₾",
            originalPrice = "500 ₾",
            discountPercent = 20,
            inStock = true
        )
        val regularItem = ProductRepository.getAllProducts().last().copy(
            id = 802,
            title = "Regular USB-C Port",
            price = "60 ₾",
            originalPrice = "",
            discountPercent = 0,
            inStock = true
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                DiscountsScreen(
                    products = listOf(activePromo, regularItem),
                    onProductClick = {},
                    onAddToCart = {},
                    onToggleFavorite = {},
                    onOpenFullCatalog = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("discounts_header_title", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("სპეციალური შეთავაზებები", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("შეარჩიე პროდუქტები შეღავათიან ფასად", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("1 აქცია", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ძებნა ფასდაკლებებში...", useUnmergedTree = true).assertIsDisplayed()

        // Active discounted item is rendered; non-discounted item is excluded
        composeTestRule.onNodeWithText("Samsung S24 Ultra OLED მოდული", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Regular USB-C Port", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `ai assistant composer input is compact single-line height when empty and accepts typing`() {
        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                AiAssistantScreen(
                    products = ProductRepository.getAllProducts().take(4),
                    selectedSection = 0
                )
            }
        }
        composeTestRule.waitForIdle()

        val inputContainer = composeTestRule.onNodeWithTag("ai_assistant_input_container", useUnmergedTree = true)
            .assertIsDisplayed()
        val density = context.resources.displayMetrics.density
        val heightDp = inputContainer.fetchSemanticsNode().boundsInRoot.height / density
        // Verify composer input is compact (< 68.dp) rather than stretched to 96.dp
        assertTrue("AI input container should start compact (actual: ${heightDp}dp)", heightDp in 42f..68f)

        composeTestRule.onNodeWithTag("ai_assistant_input", useUnmergedTree = true)
            .performTextReplacement("iPhone 15 ეკრანი")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_assistant_send_button", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `admin bank dialog displays single-line horizontal presets validates blank fields and saves valid account`() {
        AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = true)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                AdminMultiBankAccountsManagementView()
            }
        }
        composeTestRule.waitForIdle()

        // Open Add Bank dialog
        composeTestRule.onNodeWithTag("admin_add_bank_account_button", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_bank_edit_dialog", useUnmergedTree = true).assertIsDisplayed()

        // All 3 bank preset chips must render horizontally on one row (matching top Y coordinate)
        val bogNode = composeTestRule.onNodeWithTag("admin_bank_preset_bog", useUnmergedTree = true).assertIsDisplayed()
        val tbcNode = composeTestRule.onNodeWithTag("admin_bank_preset_tbc", useUnmergedTree = true).assertIsDisplayed()
        val otherNode = composeTestRule.onNodeWithTag("admin_bank_preset_other", useUnmergedTree = true).assertIsDisplayed()
        val bogCenterY = bogNode.fetchSemanticsNode().boundsInRoot.center.y
        val tbcCenterY = tbcNode.fetchSemanticsNode().boundsInRoot.center.y
        val otherCenterY = otherNode.fetchSemanticsNode().boundsInRoot.center.y
        assertEquals(bogCenterY, tbcCenterY, 4f)
        assertEquals(bogCenterY, otherCenterY, 4f)

        // Blank IBAN + recipient triggers inline validation error and keeps dialog open
        composeTestRule.onNodeWithTag("admin_save_bank_account_button", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("admin_bank_edit_dialog", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("მიუთითეთ მიმღების სახელი", substring = true, useUnmergedTree = true).assertIsDisplayed()

        // Fill valid fields and save
        otherNode.performClick()
        composeTestRule.onNodeWithTag("admin_bank_name_input", useUnmergedTree = true)
            .performScrollTo()
            .performTextReplacement("ლიბერთი ბანკი")
        composeTestRule.onNodeWithTag("admin_bank_recipient_input", useUnmergedTree = true)
            .performScrollTo()
            .performTextReplacement("შპს ბისთორ")
        composeTestRule.onNodeWithTag("admin_bank_iban_input", useUnmergedTree = true)
            .performScrollTo()
            .performTextReplacement("GE29LB0115111111111111")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_save_bank_account_button", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // Verify saved in BstorePaymentManager
        val accounts = BstorePaymentManager.getAllBankAccounts(context)
        assertTrue(accounts.any { it.bankName == "ლიბერთი ბანკი" && it.iban == "GE29LB0115111111111111" })
    }

    @Test
    fun `admin notification panel supports open markRead viewAll modal and delete confirmation without deleting order`() {
        AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = true)
        val cart = listOf(
            CartItem(
                product = ProductItem(id = 1, title = "iPhone 16 Pro OLED", price = "350 ₾", category = "ეკრანები", imageUrl = ""),
                quantity = 1
            )
        )
        val savedOrder = AuthAndOrderManager.addOrderFromCart(context, cart)

        val notifs = listOf(
            AdminOrderNotificationRecord(
                notificationId = "admin_order_${savedOrder.id}",
                orderId = savedOrder.id,
                orderType = "product_order",
                title = "ახალი შეკვეთა #${savedOrder.orderNumber}",
                body = "მომხმარებელი • 350 ₾",
                read = false,
                createdAtMillis = System.currentTimeMillis()
            )
        )

        var openedOrderId: String? = null
        var markedReadId: String? = null
        var deletedNotifId: String? = null
        var viewAllClicked = false

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                AdminOrderNotificationsHistoryPanel(
                    notifications = notifs,
                    unreadCount = 1,
                    onOpenOrderNotification = { openedOrderId = it.orderId },
                    onMarkNotificationRead = { markedReadId = it.notificationId },
                    onDeleteNotification = { notif, onComplete ->
                        deletedNotifId = notif.notificationId
                        onComplete()
                    },
                    onMarkAllRead = {},
                    onViewAllClick = { viewAllClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify "ყველას ნახვა" button
        composeTestRule.onNodeWithTag("admin_notifications_view_all_button", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        assertTrue(viewAllClicked)

        // 2. Verify individual Mark Read
        composeTestRule.onNodeWithTag("admin_order_notif_mark_read_${savedOrder.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        assertEquals("admin_order_${savedOrder.id}", markedReadId)

        // 3. Verify opening notification opens exact order
        composeTestRule.onNodeWithTag("admin_order_notif_item_${savedOrder.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        assertEquals(savedOrder.id, openedOrderId)

        // 4. Verify delete triggers confirmation dialog and only deletes notification while keeping order intact
        composeTestRule.onNodeWithTag("admin_order_notif_delete_${savedOrder.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("confirm_delete_admin_notif_button", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("admin_order_${savedOrder.id}", deletedNotifId)
        assertTrue(
            "Underlying order must remain intact when notification is deleted",
            AuthAndOrderManager.getOrders(context).any { it.id == savedOrder.id }
        )
    }
}
