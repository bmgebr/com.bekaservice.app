package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfileRedesignVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bekaservice_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bekaservice_notif_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bekaservice_contacts_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bstore_payment_system_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bekaservice_admin_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        AppPreferencesManager.init(context)
        AppPreferencesManager.setOnboardingCompleted(context, true)
        AppPreferencesManager.setDarkMode(context, true)
        AppPreferencesManager.setLanguage(context, AppLanguage.KA)
        ContactAndSocialManager.init(context)
        BstorePaymentManager.init(context)
        BekaNotificationManager.markPermissionPrompted(context)
        AuthAndOrderManager.logout(context)
    }

    private fun seedBank(): BankAccountConfig {
        val bank = BankAccountConfig(
            id = "bank_bog_test",
            bankCode = "bog",
            bankName = "საქართველოს ბანკი",
            recipientName = "BSTORE LLC",
            iban = "GE17BG0000000580620970",
            isEnabled = true,
            displayOrder = 1
        )
        BstorePaymentManager.saveBankAccountsLocal(context, listOf(bank))
        return bank
    }

    // 1. User with complete profile
    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `scenario 01 - complete user profile displays real identity phone address and device without fake copy`() {
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "nino.beridze@gmail.com",
            name = "ნინო ბერიძე",
            avatarUrl = "https://example.com/nino.png",
            phone = "+995 599 12 34 56",
            address = "ქ. თბილისი, ჭავჭავაძის 15",
            deviceModel = "iPhone 15 Pro",
            hasAdminClaim = false
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(onOpenCart = {}, onOpenAdminPanel = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("პროფილი & ანგარიშები").assertIsDisplayed()
        composeTestRule.onNodeWithText("ნინო ბერიძე").assertIsDisplayed()
        composeTestRule.onNodeWithText("nino.beridze@gmail.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Google ანგარიში", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("+995 599 12 34 56", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ქ. თბილისი, ჭავჭავაძის 15", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("მოწყობილობა: iPhone 15 Pro", useUnmergedTree = true).assertIsDisplayed()

        // Ensure zero mockup placeholder strings exist
        composeTestRule.onAllNodesWithText("გიორგი მელაძე").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("AirPods Pro (2nd Gen)").assertCountEquals(0)
    }

    // 2. User with missing optional fields (no derived email prefix name!)
    @Test
    fun `scenario 02 - missing optional fields show neutral fallback and never derive name from email`() {
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "secretprefix99@gmail.com",
            name = "",
            avatarUrl = "",
            phone = "",
            address = "",
            deviceModel = "",
            hasAdminClaim = false
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(onOpenCart = {}, onOpenAdminPanel = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("მომხმარებელი").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("secretprefix99", substring = true, ignoreCase = true).assertCountEquals(1) // only the full email line
        composeTestRule.onNodeWithText("secretprefix99@gmail.com").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_add_phone_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_add_address_button").assertIsDisplayed()
    }

    // 3. Google vs non-Google authenticated user
    @Test
    fun `scenario 03 - email user does not receive fake Google badge`() {
        val regRes = AuthAndOrderManager.registerWithEmail(
            context = context,
            name = "დავით ლომიძე",
            email = "dato@example.com",
            pass = "password123"
        )
        assertTrue(regRes.first)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(onOpenCart = {}, onOpenAdminPanel = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("დავით ლომიძე").assertIsDisplayed()
        composeTestRule.onNodeWithText("ელ. ფოსტით ავტორიზაცია", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Google ანგარიში", useUnmergedTree = true).assertCountEquals(0)
    }

    // 4. User with NO orders + empty CTA
    @Test
    fun `scenario 04 - user with no orders shows real zero counts empty state and catalog CTA`() {
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "noorders@gmail.com",
            name = "ანა კაპანაძე",
            hasAdminClaim = false
        )
        var openedCatalog = false

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(
                    onOpenCart = {},
                    onOpenAdminPanel = {},
                    onOpenCatalog = { openedCatalog = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_orders_empty_state"))
        composeTestRule.onNodeWithText("აქტიური (0)", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("დასრულებული (0)", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_orders_empty_state").assertIsDisplayed()

        composeTestRule.onNodeWithTag("profile_orders_go_shopping_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(openedCatalog)
    }

    // 5, 6, 7. Active orders, Completed orders, Both + Tab switching + View All dialog
    @Test
    fun `scenarios 05 06 07 - active and completed orders display accurately support tab switch and View All modal`() {
        val bank = seedBank()
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "orders.combo@gmail.com",
            name = "ლევან ბერიძე",
            hasAdminClaim = false
        )
        val prod = ProductRepository.getAllProducts().first()
        val prod2 = ProductRepository.getAllProducts().drop(1).first()

        val activeOrd = AuthAndOrderManager.addOrderFromCart(
            context = context,
            cartItems = listOf(CartItem(prod, 1, "Original")),
            checkoutConfig = CheckoutSubmissionConfig(
                paymentMethod = PaymentMethodType.BANK_TRANSFER,
                selectedBankAccount = bank
            )
        )
        val doneOrd = AuthAndOrderManager.addOrderFromCart(
            context = context,
            cartItems = listOf(CartItem(prod2, 1, "OEM")),
            checkoutConfig = CheckoutSubmissionConfig(
                paymentMethod = PaymentMethodType.COD,
                deliveryMethod = DeliveryMethodType.COURIER,
                deliveryCity = "თბილისი",
                deliveryAddress = "პეკინის 10"
            )
        )
        AuthAndOrderManager.updateOrderStatus(context, doneOrd.id, "შესრულებული", "completed")

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(
                    onOpenCart = {},
                    onOpenAdminPanel = {},
                    products = ProductRepository.getAllProducts()
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_order_card_${activeOrd.id}"))
        composeTestRule.onNodeWithText("აქტიური (1)", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("დასრულებული (1)", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_order_card_${activeOrd.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("order_stepper_${activeOrd.id}", useUnmergedTree = true).assertIsDisplayed()

        // Switch to completed tab
        composeTestRule.onNodeWithTag("profile_orders_tab_დასრულებული").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_order_card_${doneOrd.id}").assertIsDisplayed()

        // Open View All Orders Dialog
        composeTestRule.onNodeWithTag("profile_orders_view_all_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_orders_dialog").assertIsDisplayed()
    }

    // 8 & 9. Favorites empty + populated + remove + View All
    @Test
    fun `scenarios 08 09 - favorites empty state then populated preview remove and View All dialog`() {
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "favs@gmail.com",
            name = "მარიამ გიორგაძე",
            hasAdminClaim = false
        )
        val allProds = ProductRepository.getAllProducts()
        val p1 = allProds[0]
        val p2 = allProds[1]
        var clickedCatalog = false
        var clickedProductId = 0

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(
                    onOpenCart = {},
                    onOpenAdminPanel = {},
                    products = allProds,
                    onProductClick = { clickedProductId = it.id },
                    onToggleFavorite = { AuthAndOrderManager.toggleFavorite(context, it.id) },
                    onOpenCatalog = { clickedCatalog = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_favorites_empty_state"))
        composeTestRule.onNodeWithText("რჩეულები (0)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_favorites_browse_catalog_button").performClick()
        assertTrue(clickedCatalog)

        // Add 2 real favorites
        AuthAndOrderManager.toggleFavorite(context, p1.id)
        AuthAndOrderManager.toggleFavorite(context, p2.id)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("რჩეულები (2)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_favorite_item_${p1.id}").assertIsDisplayed().performClick()
        assertEquals(p1.id, clickedProductId)

        composeTestRule.onNodeWithTag("profile_favorites_view_all_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_favorites_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescAndDismiss("დახურვა")

        composeTestRule.onNodeWithTag("profile_remove_favorite_${p1.id}").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("რჩეულები (1)").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescAndDismiss(desc: String) {
        onAllNodes(androidx.compose.ui.test.hasContentDescription(desc))[0].performClick()
        waitForIdle()
    }

    // 10 & 11. Normal user vs Authorized Admin user
    @Test
    fun `scenarios 10 11 - normal user hides admin banner while verified beqa1994 admin renders and opens it`() {
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "regular.user@gmail.com",
            name = "ჩვეულებრივი მომხმარებელი",
            hasAdminClaim = false
        )

        var adminOpened = false
        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(
                    onOpenCart = {},
                    onOpenAdminPanel = { adminOpened = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("profile_admin_panel_button").assertCountEquals(0)

        // Switch to verified sole admin
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            name = "ბექა ადმინი",
            hasAdminClaim = true
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_admin_panel_button"))
        composeTestRule.onNodeWithTag("profile_admin_panel_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertTrue(adminOpened)
    }

    // 12. Profile editing flow
    @Test
    fun `scenario 12 - profile edit modal updates name phone address and deviceModel`() {
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "editor@gmail.com",
            name = "ძველი სახელი",
            hasAdminClaim = false
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(onOpenCart = {}, onOpenAdminPanel = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_edit_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edit_profile_dialog").assertIsDisplayed()

        composeTestRule.onNodeWithTag("edit_profile_first_name_input").performTextReplacement("გიორგი")
        composeTestRule.onNodeWithTag("edit_profile_last_name_input").performTextReplacement("მაისურაძე")
        composeTestRule.onNodeWithTag("edit_profile_phone_input").performTextReplacement("+995 591 22 33 44")
        composeTestRule.onNodeWithTag("edit_profile_address_input").performTextReplacement("ბათუმი, რუსთაველის 8")
        composeTestRule.onNodeWithTag("edit_profile_device_input").performTextReplacement("Galaxy S24 Ultra")

        composeTestRule.onNodeWithTag("edit_profile_save_button").performClick()
        composeTestRule.waitForIdle()

        val saved = AuthAndOrderManager.getCurrentUser(context)
        assertNotNull(saved)
        assertEquals("გიორგი მაისურაძე", saved?.displayName)
        assertEquals("+995 591 22 33 44", saved?.phone)
        assertEquals("ბათუმი, რუსთაველის 8", saved?.address)
        assertEquals("Galaxy S24 Ultra", saved?.deviceModel)
    }

    // 13. Password change flow (Email vs Google)
    @Test
    fun `scenario 13 - email user can change password while Google user gets provider-managed notice`() {
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "googleuser@gmail.com",
            name = "Google მომხმარებელი",
            hasAdminClaim = false
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(onOpenCart = {}, onOpenAdminPanel = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_change_password_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("change_password_provider_notice").assertIsDisplayed()
        composeTestRule.onNodeWithText("გასაგებია").performClick()
        composeTestRule.waitForIdle()

        // Switch to Email account
        AuthAndOrderManager.registerWithEmail(context, "Email User", "emailuser@example.com", "oldPass123")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_change_password_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("change_password_old_input").performTextReplacement("oldPass123")
        composeTestRule.onNodeWithTag("change_password_new_input").performTextReplacement("newPass456")
        composeTestRule.onNodeWithTag("change_password_confirm_input").performTextReplacement("newPass456")
        composeTestRule.onNodeWithTag("change_password_save_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag("change_password_dialog").assertCountEquals(0)
        // Verify old password fails & new succeeds
        assertFalse(AuthAndOrderManager.changePassword(context, "oldPass123", "other999").first)
        assertTrue(AuthAndOrderManager.changePassword(context, "newPass456", "final999").first)
    }

    // 14 & 15. Sign-out & Account deletion confirmation flow
    @Test
    fun `scenarios 14 15 - account deletion dialog cancel and confirm plus logout return cleanly to guest state`() {
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "delete.me@gmail.com",
            name = "წასაშლელი ანგარიში",
            hasAdminClaim = false
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(onOpenCart = {}, onOpenAdminPanel = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_row_delete_account"))
        composeTestRule.onNodeWithTag("profile_row_delete_account").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("delete_account_dialog").assertIsDisplayed()

        // Cancel preserves user session
        composeTestRule.onNodeWithTag("delete_account_cancel_button").performClick()
        composeTestRule.waitForIdle()
        assertNotNull(AuthAndOrderManager.getCurrentUser(context))

        // Confirm deletes/clears session -> renders Guest Auth Card
        composeTestRule.onNodeWithTag("profile_row_delete_account").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("delete_account_confirm_button").performClick()
        composeTestRule.waitForIdle()
        assertNull(AuthAndOrderManager.getCurrentUser(context))
        composeTestRule.onNodeWithTag("profile_auth_card").assertIsDisplayed()

        // Now login again & test Logout button
        AuthAndOrderManager.loginWithGoogle(context, "logout@gmail.com", "Logout Test", hasAdminClaim = false)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_logout_button").performClick()
        composeTestRule.waitForIdle()
        assertNull(AuthAndOrderManager.getCurrentUser(context))
        composeTestRule.onNodeWithTag("profile_auth_card").assertIsDisplayed()
    }

    // 16. Policy navigation flow
    @Test
    fun `scenario 16 - grouped policies card opens legal policies modal for specific topic`() {
        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(onOpenCart = {}, onOpenAdminPanel = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("customer_payment_and_policies_card"))
        composeTestRule.onNodeWithTag("policy_topic_item_return_refund_policy").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("bstore_legal_policies_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("1. პროდუქციის შემოწმება და დაბრუნების პირობები").assertIsDisplayed()
    }

    // 17 & 18. Contact actions, service address & working hours dynamically reflect ContactAndSocialManager
    @Test
    fun `scenarios 17 18 - help and contact dynamically reflects configured phones address hours and dialer`() {
        val customConfig = ContactAndSocialConfig(
            phoneNumbers = listOf(
                ContactPhoneNumber(id = "ph_dyn", label = "ოპერატორი", number = "+995 555 11 22 33", isEnabled = true, displayOrder = 1)
            ),
            socialLinks = listOf(
                SocialMediaLink(id = "soc_wa", platform = SocialPlatform.WHATSAPP, title = "WhatsApp", url = "https://wa.me/995555112233", isEnabled = true, displayOrder = 1),
                SocialMediaLink(id = "soc_bad", platform = SocialPlatform.CUSTOM, title = "Broken", url = "javascript:alert(1)", isEnabled = true, displayOrder = 2)
            ),
            serviceAddress = "ქ. ახალციხე, კოსტავას N10 (სერვის ცენტრი)",
            workingHours = "10:00 – 19:00"
        )
        ContactAndSocialManager.saveLocalConfig(context, customConfig)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(onOpenCart = {}, onOpenAdminPanel = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("store_contacts_card"))
        composeTestRule.onNodeWithText("ქ. ახალციხე, კოსტავას N10 (სერვის ცენტრი)").assertIsDisplayed()
        composeTestRule.onNodeWithText("სამუშაო საათები: 10:00 – 19:00").assertIsDisplayed()

        // Invalid javascript: link is hidden while valid WhatsApp link renders
        composeTestRule.onNodeWithTag("profile_social_soc_wa").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("profile_social_soc_bad").assertCountEquals(0)

        // Expand phone list and tap call button -> fires ACTION_DIAL
        composeTestRule.onNodeWithTag("store_contact_phone_row").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_call_button_ph_dyn").assertIsDisplayed().performClick()
        val nextIntent = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).nextStartedActivity
        assertEquals(Intent.ACTION_DIAL, nextIntent.action)
        assertTrue(nextIntent.dataString?.contains("995555112233") == true)
    }

    // 19. Scrolling & bottom nav clearance on compact device
    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h780dp")
    fun `scenario 19 - compact viewport scrolls to bottom banner cleanly with bottom inset clearance`() {
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            name = "Admin Beqa",
            hasAdminClaim = true
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(
                    onOpenCart = {},
                    onOpenAdminPanel = {},
                    contentPadding = PaddingValues(top = 74.dp, bottom = 92.dp)
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_admin_panel_button"))
        composeTestRule.onNodeWithTag("profile_admin_panel_button").assertIsDisplayed()

        // Verify dark <-> light toggle works inside Security & Settings dialog
        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_row_security_settings"))
        composeTestRule.onNodeWithTag("profile_row_security_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_dark_mode_switch").performClick()
        composeTestRule.waitForIdle()
        assertFalse(AppPreferencesManager.isDarkMode)
    }

    // 20. Distinct Loading / Empty / Error order states
    @Test
    fun `scenario 20 - orders section renders distinct loading error retry and empty states`() {
        val loadingState = androidx.compose.runtime.mutableStateOf(true)
        val errorState = androidx.compose.runtime.mutableStateOf<String?>(null)
        var retried = false

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileOrdersSection(
                    ordersList = emptyList(),
                    selectedOrderFilter = "აქტიური",
                    onFilterChange = {},
                    isOrdersLoading = loadingState.value,
                    ordersError = errorState.value,
                    products = emptyList(),
                    onRetryOrders = { retried = true },
                    onOpenAllOrders = {},
                    onOpenOrderDetail = {},
                    onOpenPaymentInstructions = {},
                    onOpenCatalog = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_orders_loading_state").assertIsDisplayed()

        loadingState.value = false
        errorState.value = "ქსელის შეცდომა"
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_orders_error_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_orders_retry_button").performClick()
        assertTrue(retried)

        errorState.value = null
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_orders_empty_state").assertIsDisplayed()
    }

    // 21. Strict account-switching isolation
    @Test
    fun `scenario 21 - switching accounts completely isolates profile fields orders favorites and admin access`() {
        val prod1 = ProductRepository.getAllProducts()[0]
        val prod2 = ProductRepository.getAllProducts()[1]

        // Account A (Verified Admin with 1 order and fav prod1)
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            name = "ბექა ადმინი",
            phone = "+995 598 00 00 01",
            hasAdminClaim = true
        )
        AuthAndOrderManager.toggleFavorite(context, prod1.id)
        val orderA = AuthAndOrderManager.addOrderFromCart(
            context = context,
            cartItems = listOf(CartItem(prod1, 1, "Original")),
            checkoutConfig = CheckoutSubmissionConfig(PaymentMethodType.COD)
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                ProfileAndAuthScreen(
                    onOpenCart = {},
                    onOpenAdminPanel = {},
                    products = ProductRepository.getAllProducts()
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ბექა ადმინი").assertIsDisplayed()

        // Account B (Normal customer with different order and different favorite)
        AuthAndOrderManager.logout(context)
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = "customer.b@gmail.com",
            name = "მომხმარებელი ბ",
            phone = "+995 577 00 00 02",
            hasAdminClaim = false
        )
        AuthAndOrderManager.toggleFavorite(context, prod2.id)
        val orderB = AuthAndOrderManager.addOrderFromCart(
            context = context,
            cartItems = listOf(CartItem(prod2, 1, "OEM")),
            checkoutConfig = CheckoutSubmissionConfig(PaymentMethodType.COD)
        )
        composeTestRule.waitForIdle()

        // Verify Account B sees ONLY Account B's profile, favorites, orders, and NO admin button
        composeTestRule.onNodeWithText("მომხმარებელი ბ").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("ბექა ადმინი").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("+995 598 00 00 01").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("profile_admin_panel_button").assertCountEquals(0)

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_order_card_${orderB.id}"))
        composeTestRule.onNodeWithTag("profile_order_card_${orderB.id}").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("profile_order_card_${orderA.id}").assertCountEquals(0)

        composeTestRule.onNodeWithTag("profile_lazy_column")
            .performScrollToNode(hasTestTag("profile_favorite_item_${prod2.id}"))
        composeTestRule.onNodeWithTag("profile_favorite_item_${prod2.id}").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("profile_favorite_item_${prod1.id}").assertCountEquals(0)
    }
}
