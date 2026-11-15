package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.RepairProTheme
import com.example.ui.theme.bstorePalette
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BstoreUiConsistencyAndAiAssistantTest {

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
        context.getSharedPreferences("beka_admin_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        AppPreferencesManager.init(context)
        AppPreferencesManager.setOnboardingCompleted(context, true)
        AppPreferencesManager.setDarkMode(context, true)
        AppPreferencesManager.setLanguage(context, AppLanguage.KA)
        BekaNotificationManager.markPermissionPrompted(context)
        hasCompletedInitialLaunch = true
    }

    // ==========================================================================
    // PART 1 VISUAL CONSISTENCY & NARROW SCREEN RESPONSIVE CHECKS (360dp width)
    // ==========================================================================

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h780dp")
    fun `narrow screen cart CTAs are unclipped and stacked cleanly without overlap`() {
        val sampleProduct = ProductRepository.getAllProducts().first().copy(
            id = 501,
            title = "iPhone 16 Pro Max OLED ეკრანი",
            price = "390 ₾",
            originalPrice = "490 ₾",
            discountPercent = 20,
            inStock = true
        )
        var checkoutClicks = 0

        composeTestRule.setContent {
            val cartItems = rememberMutableCartList(CartItem(sampleProduct, quantity = 1, selectedQuality = "Original"))
            RepairProTheme(darkTheme = true) {
                CartScreen(
                    cartList = cartItems,
                    onBack = {},
                    onCheckout = { checkoutClicks++ }
                )
            }
        }
        composeTestRule.waitForIdle()

        val density = context.resources.displayMetrics.density
        val maxWidthPx = 360f * density

        val checkoutNode = composeTestRule.onNodeWithTag("cart_checkout_button", useUnmergedTree = true)
            .assertIsDisplayed()
        val waNode = composeTestRule.onNodeWithTag("cart_whatsapp_consult_button", useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("შეკვეთის გაფორმება", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("WhatsApp კონსულტაცია", useUnmergedTree = true).assertIsDisplayed()

        val checkoutBounds = checkoutNode.fetchSemanticsNode().boundsInRoot
        val waBounds = waNode.fetchSemanticsNode().boundsInRoot
        assertTrue("Checkout button must stay within 360dp width", checkoutBounds.right <= maxWidthPx + 2f)
        assertTrue("WhatsApp button must stay within 360dp width", waBounds.right <= maxWidthPx + 2f)
        assertTrue("Buttons must be at least 42dp tall", checkoutBounds.height / density >= 42f)
        assertTrue("Buttons must not overlap vertically", checkoutBounds.bottom <= waBounds.top + 1f)

        checkoutNode.performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, checkoutClicks)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h780dp")
    fun `admin confirm payment dialog renders cleanly on narrow 360dp screen without button overlap`() {
        val sampleOrder = OrderRecord(
            id = "ord_confirm_360",
            orderNumber = "#BS-36001",
            date = "13.11.2026",
            items = emptyList(),
            totalAmount = "234 ₾",
            status = "მიმდინარე",
            paymentMethod = PaymentMethodType.BANK_TRANSFER.titleKa,
            trackingCode = "BSTORE-36001",
            userId = "u1",
            userName = "გიორგი",
            paymentMethodCode = PaymentMethodType.BANK_TRANSFER.code,
            bankName = "საქართველოს ბანკი",
            ibanSnapshot = "GE29BG0000000111111111",
            paymentReference = "#BS-36001"
        )

        var dismissed = false
        var confirmedNote: String? = null

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                AdminConfirmPaymentDialog(
                    order = sampleOrder,
                    isSubmitting = false,
                    onDismiss = { dismissed = true },
                    onConfirm = { note -> confirmedNote = note }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_confirm_payment_dialog", useUnmergedTree = true).assertIsDisplayed()
        val cancelBtn = composeTestRule.onNodeWithText("გაუქმება", useUnmergedTree = true).assertIsDisplayed()
        val submitBtn = composeTestRule.onNodeWithTag("admin_confirm_payment_submit_button", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("დადასტურება ✓", useUnmergedTree = true).assertIsDisplayed()

        val cancelBounds = cancelBtn.fetchSemanticsNode().boundsInRoot
        val submitBounds = submitBtn.fetchSemanticsNode().boundsInRoot
        assertTrue("Cancel & Confirm buttons must not overlap horizontally", cancelBounds.right <= submitBounds.left + 2f)

        composeTestRule.onNodeWithTag("admin_confirm_payment_note_input", useUnmergedTree = true)
            .performTextReplacement("გადამოწმდა BOG")
        submitBtn.performClick()
        composeTestRule.waitForIdle()

        assertEquals("გადამოწმდა BOG", confirmedNote)
        assertFalse(dismissed)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h780dp")
    fun `admin payment dashboard 2x2 metric grid and 2x2 order status grid stay aligned on 360dp screen`() {
        AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = true)
        val cart = listOf(
            CartItem(
                product = ProductItem(id = 11, title = "Samsung S24 OLED", price = "380 ₾", category = "ეკრანები", imageUrl = ""),
                quantity = 1
            )
        )
        val savedOrder = AuthAndOrderManager.addOrderFromCart(context, cart)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                AdminOrdersManagementView()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_payment_dashboard_section", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_financial_metrics_2x2_grid", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_open_payment_audit_button", useUnmergedTree = true).assertIsDisplayed()

        val gridTag = "admin_order_status_grid_${savedOrder.id}"
        composeTestRule.onNodeWithTag("admin_orders_lazy_column", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("admin_order_card_${savedOrder.id}"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(gridTag, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()

        val btnPending = composeTestRule.onNodeWithTag("admin_order_status_btn_${savedOrder.id}_pending", useUnmergedTree = true)
            .assertIsDisplayed()
        val btnInProgress = composeTestRule.onNodeWithTag("admin_order_status_btn_${savedOrder.id}_in_progress", useUnmergedTree = true)
            .assertIsDisplayed()
        val btnCompleted = composeTestRule.onNodeWithTag("admin_order_status_btn_${savedOrder.id}_completed", useUnmergedTree = true)
            .assertIsDisplayed()
        val btnCancelled = composeTestRule.onNodeWithTag("admin_order_status_btn_${savedOrder.id}_cancelled", useUnmergedTree = true)
            .assertIsDisplayed()

        val b1 = btnPending.fetchSemanticsNode().boundsInRoot
        val b2 = btnInProgress.fetchSemanticsNode().boundsInRoot
        val b3 = btnCompleted.fetchSemanticsNode().boundsInRoot
        val b4 = btnCancelled.fetchSemanticsNode().boundsInRoot

        // Row 1 aligned, Row 2 aligned, and zero overlap
        assertEquals(b1.center.y, b2.center.y, 4f)
        assertEquals(b3.center.y, b4.center.y, 4f)
        assertTrue("Status grid row 1 must be above row 2", b1.bottom <= b3.top + 2f)
        assertTrue("Row 1 buttons must not overlap", b1.right <= b2.left + 2f)
        assertTrue("Row 2 buttons must not overlap", b3.right <= b4.left + 2f)
    }

    @Test
    fun `design system light and dark palettes preserve high contrast and orange brand accent`() {
        val dark = bstorePalette(isDark = true)
        val light = bstorePalette(isDark = false)

        assertNotEquals(dark.pageBg, light.pageBg)
        assertNotEquals(dark.cardBg, light.cardBg)
        assertNotEquals(dark.textPrimary, light.textPrimary)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF6B00), dark.brandOrange)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF36B16), light.brandOrange)

        AppPreferencesManager.setDarkMode(context, false)
        composeTestRule.setContent {
            RepairProTheme(darkTheme = false) {
                AiAssistantScreen(
                    products = ProductRepository.getAllProducts().take(4),
                    selectedSection = 0
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ai_assistant_root", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_assistant_input_container", useUnmergedTree = true).assertIsDisplayed()
    }

    // ==========================================================================
    // PART 2 & PART 4: LIVE CATALOG SEARCH, DYNAMIC ADMIN MUTATIONS & ZERO HALLUCINATION
    // ==========================================================================

    @Test
    fun `ai shopping engine searches exact synonym typo category budget discounts and compares`() = runBlocking {
        val liveCatalog = ProductRepository.getAllProducts()
        val session = AiShoppingSessionHolder()

        // 1. Exact product search
        val exact = BstoreAiShoppingEngine.generateShoppingReply(
            context = context,
            history = emptyList(),
            latestUserMessage = "iPhone 16 Pro Max OLED ეკრანი გაქვთ?",
            products = liveCatalog,
            sessionHolder = session
        )
        assertTrue("Exact search should recommend iPhone 16 Pro Max screen", exact.recommendedProducts.any {
            it.title.contains("iPhone 16 Pro Max", ignoreCase = true)
        })
        assertFalse(exact.isDiagnosticRedirect)

        // 2. Georgian & phonetic synonym search
        val synonym = BstoreAiShoppingEngine.searchProducts("აიფონ ეკრანი", liveCatalog)
        assertTrue("Georgian 'აიფონ ეკრანი' should find Apple displays", synonym.any {
            it.brand == "Apple" && it.category == "ეკრანები"
        })

        // 3. Spelling mistake / typo tolerance
        val typo = BstoreAiShoppingEngine.searchProducts("იფონ 15 ეკარნი", liveCatalog)
        assertTrue("Typo 'იფონ 15 ეკარნი' should still find iPhone 15 screen", typo.isNotEmpty())

        // 4. Category search
        val catSearch = BstoreAiShoppingEngine.searchProducts("ელემენტები", liveCatalog)
        assertTrue(catSearch.isNotEmpty() && catSearch.all { it.category == "ელემენტები" })

        // 5. Budget filter ("350 ლარამდე ეკრანი")
        val budgetReply = BstoreAiShoppingEngine.generateShoppingReply(
            context = context,
            history = emptyList(),
            latestUserMessage = "350 ლარამდე რა ტელეფონის ეკრანები გაქვთ?",
            products = liveCatalog,
            sessionHolder = AiShoppingSessionHolder()
        )
        assertTrue("Budget search should find affordable displays", budgetReply.recommendedProducts.isNotEmpty())
        assertTrue("Every returned product must be <= 350 GEL", budgetReply.recommendedProducts.all {
            (BstoreAiShoppingEngine.productNumericPrice(it) ?: Double.MAX_VALUE) <= 350.0
        })

        // 6. Discounted products query
        val catalogWithDiscount = liveCatalog.mapIndexed { idx, item ->
            if (idx == 0) item.copy(price = "240 ₾", originalPrice = "300 ₾", discountPercent = 20)
            else item
        }
        val discountReply = BstoreAiShoppingEngine.generateShoppingReply(
            context = context,
            history = emptyList(),
            latestUserMessage = "რომელი პროდუქტებია ფასდაკლებაში?",
            products = catalogWithDiscount,
            sessionHolder = AiShoppingSessionHolder()
        )
        assertTrue("Discount query must return discounted items", discountReply.recommendedProducts.any {
            it.hasActiveDiscount()
        })

        // 7. Comparison query
        val compareReply = BstoreAiShoppingEngine.generateShoppingReply(
            context = context,
            history = emptyList(),
            latestUserMessage = "შემიდარე ეს ორი პროდუქტი",
            products = catalogWithDiscount,
            sessionHolder = session
        )
        assertTrue("Comparison reply must mention warranty/quality", compareReply.text.contains("გარანტია") || compareReply.recommendedProducts.isNotEmpty())
    }

    @Test
    fun `dynamic admin catalog mutations add price edit and deactivation reflect immediately without APK update`() = runBlocking {
        val baseCatalog = ProductRepository.getAllProducts()

        // 1. Add a completely future/new product not in static repository
        val brandNewProduct = ProductItem(
            id = 950,
            title = "Xiaomi 15 Ultra LTPO AMOLED ეკრანი",
            price = "420 ₾",
            category = "ეკრანები",
            imageUrl = "",
            brand = "Xiaomi",
            compatibleModels = "Xiaomi 15 Ultra",
            warranty = "6 თვე",
            inStock = true,
            isActive = true
        )
        val afterAdd = baseCatalog + brandNewProduct
        val discoverReply = BstoreAiShoppingEngine.generateShoppingReply(
            context = context,
            history = emptyList(),
            latestUserMessage = "Xiaomi 15 Ultra ეკრანი გაქვთ?",
            products = afterAdd,
            sessionHolder = AiShoppingSessionHolder()
        )
        assertTrue(
            "Newly added admin product must be immediately discoverable",
            discoverReply.recommendedProducts.any { it.id == 950 }
        )
        assertTrue("Reply must quote exact current price 420 ₾", discoverReply.text.contains("420"))

        // 2. Edit product price (420 ₾ -> 295 ₾)
        val afterPriceEdit = afterAdd.map {
            if (it.id == 950) it.copy(price = "295 ₾") else it
        }
        val updatedPriceReply = BstoreAiShoppingEngine.generateShoppingReply(
            context = context,
            history = emptyList(),
            latestUserMessage = "300 ლარამდე Xiaomi 15 Ultra ეკრანი",
            products = afterPriceEdit,
            sessionHolder = AiShoppingSessionHolder()
        )
        assertTrue("Updated price should now fit <= 300 GEL filter", updatedPriceReply.recommendedProducts.any { it.id == 950 })
        assertTrue("Reply must reflect updated 295 ₾ price", updatedPriceReply.text.contains("295"))
        assertFalse("Old price 420 must no longer appear", updatedPriceReply.text.contains("420"))

        // 3. Deactivate product (isActive = false)
        val afterDeactivate = afterPriceEdit.map {
            if (it.id == 950) it.copy(isActive = false) else it
        }
        assertFalse(
            "Deactivated product must be excluded from active catalog",
            BstoreAiShoppingEngine.activeCatalog(afterDeactivate).any { it.id == 950 }
        )
        assertTrue(
            "optimizeSearchFilter must exclude deactivated products",
            optimizeSearchFilter(afterDeactivate, "Xiaomi 15 Ultra", "ყველა").none { it.id == 950 }
        )
        val deactivatedReply = BstoreAiShoppingEngine.generateShoppingReply(
            context = context,
            history = emptyList(),
            latestUserMessage = "Xiaomi 15 Ultra LTPO AMOLED ეკრანი",
            products = afterDeactivate,
            sessionHolder = AiShoppingSessionHolder()
        )
        assertTrue(
            "Deactivated product must NEVER be recommended as purchasable",
            deactivatedReply.recommendedProducts.none { it.id == 950 }
        )
        assertTrue(
            "Reply must clearly inform customer that product is currently inactive",
            deactivatedReply.text.contains("აქტიური", ignoreCase = true) ||
                deactivatedReply.text.contains("ვერ მოიძებნა", ignoreCase = true)
        )
    }

    @Test
    fun `conversational quality handles greetings humor store info zero hallucination and multi-turn follow-ups`() = runBlocking {
        val catalog = ProductRepository.getAllProducts()
        val session = AiShoppingSessionHolder()

        // 1. Greeting
        val greet = BstoreAiShoppingEngine.generateShoppingReply(context, emptyList(), "გამარჯობა", catalog, session)
        assertTrue(greet.text.contains("გამარჯობა") && greet.text.contains("BSTORE"))
        assertFalse(greet.isDiagnosticRedirect)

        // 2. Casual humor / tight budget
        val humor = BstoreAiShoppingEngine.generateShoppingReply(
            context, emptyList(), "დღეს რას მირჩევ, ფული არ მაქვს ბევრი 😂", catalog, session
        )
        assertTrue("Humorous tight-budget query must yield friendly budget-oriented response", humor.text.isNotBlank())
        assertTrue("Should show budget-friendly options", humor.recommendedProducts.isNotEmpty())

        // 3. Incomplete/ambiguous query
        val ambiguous = BstoreAiShoppingEngine.generateShoppingReply(
            context, emptyList(), "ყველაზე მაგარი რა გაქვთ?", catalog, session
        )
        assertTrue("Ambiguous query should politely ask model/category clarification", ambiguous.text.contains("მოდელ"))

        // 4. BSTORE payments & store details
        val storeInfo = BstoreAiShoppingEngine.generateShoppingReply(
            context, emptyList(), "სად ხართ და როგორ გადავიხადო?", catalog, session
        )
        assertTrue(storeInfo.text.contains("ახალციხე") && storeInfo.text.contains("კოსტავას"))
        assertTrue(storeInfo.text.contains("საქართველოს ბანკი") && storeInfo.text.contains("თიბისი"))

        // 5. Unavailable fictional product — NEVER invent products or prices
        val nonExistent = BstoreAiShoppingEngine.generateShoppingReply(
            context, emptyList(), "SpaceX Mars Titanium Satellite Phone ეკრანი", catalog, session
        )
        assertTrue("Fictional product must never fabricate recommendations", nonExistent.recommendedProducts.isEmpty())
        assertTrue(nonExistent.text.contains("ვერ მოიძებნა"))

        // 6. Multi-turn follow-up context
        val multiSession = AiShoppingSessionHolder()
        val turn1 = BstoreAiShoppingEngine.generateShoppingReply(
            context, emptyList(), "iPhone 16 Pro Max-ის ეკრანი გაქვთ?", catalog, multiSession
        )
        assertTrue(turn1.recommendedProducts.isNotEmpty())
        assertEquals("Apple", multiSession.context.lastBrand)
        assertEquals("ეკრანები", multiSession.context.lastCategory)

        val turn2 = BstoreAiShoppingEngine.generateShoppingReply(
            context, emptyList(), "უფრო იაფი?", catalog, multiSession
        )
        assertTrue("Cheaper follow-up should stay within display/Apple category", turn2.recommendedProducts.isNotEmpty())
        val firstPrice1 = BstoreAiShoppingEngine.productNumericPrice(turn1.recommendedProducts.first()) ?: Double.MAX_VALUE
        val firstPrice2 = BstoreAiShoppingEngine.productNumericPrice(turn2.recommendedProducts.first()) ?: Double.MAX_VALUE
        assertTrue("Follow-up alternative should be cheaper or equal", firstPrice2 <= firstPrice1)

        val turn3 = BstoreAiShoppingEngine.generateShoppingReply(
            context, emptyList(), "და ხარისხით რომელია უკეთესი?", catalog, multiSession
        )
        assertTrue("Quality comparison must reference Original / High Copy / warranty", turn3.text.contains("Original"))
    }

    // ==========================================================================
    // MODE A VS MODE B SEPARATION & AI ASSISTANT INTERACTIVE UI TESTS
    // ==========================================================================

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `mode A blocks diagnostics redirects cleanly switches modes and triggers recommendation callbacks`() {
        val sampleCatalog = listOf(
            ProductItem(
                id = 901,
                title = "iPhone 16 Pro Max OLED ეკრანი",
                price = "234 ₾",
                originalPrice = "390 ₾",
                discountPercent = 40,
                category = "ეკრანები",
                brand = "Apple",
                compatibleModels = "iPhone 16 Pro Max",
                warranty = "6 თვე",
                inStock = true,
                isFeatured = true,
                imageUrl = ""
            ),
            ProductItem(
                id = 902,
                title = "Samsung S24 Ultra ელემენტი 5000mAh",
                price = "95 ₾",
                category = "ელემენტები",
                brand = "Samsung",
                compatibleModels = "Samsung S24 Ultra",
                warranty = "3 თვე",
                inStock = true,
                imageUrl = ""
            )
        )

        var selectedSection by mutableIntStateOf(0)
        var clickedProduct by mutableStateOf<ProductItem?>(null)
        var addedToCartProduct by mutableStateOf<ProductItem?>(null)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                AiAssistantScreen(
                    products = sampleCatalog,
                    onProductClick = { clickedProduct = it },
                    onAddToCart = { addedToCartProduct = it },
                    selectedSection = selectedSection,
                    onSectionChange = { selectedSection = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify clean Mode A greeting (NO catalog boilerplate, NO pre-search cards) & quick shopping chips
        composeTestRule.onNodeWithTag("ai_shopping_quick_chips", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_quick_chip_0", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("ვეყრდნობი", substring = true, useUnmergedTree = true)
            .assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("ai_rec_card_901", useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("ai_rec_card_902", useUnmergedTree = true).assertCountEquals(0)

        // 2. Ask diagnostic fault question inside Mode A -> must NOT activate diagnostics or run report
        composeTestRule.onNodeWithTag("ai_assistant_input", useUnmergedTree = true)
            .performTextReplacement("ტელეფონი არ იტენება და რა სჭირს?")
        composeTestRule.onNodeWithTag("ai_assistant_send_button", useUnmergedTree = true).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("შოპინგ ასისტენტი", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().size >= 2
        }
        assertEquals("Diagnostic fault prompt in Mode A must NOT auto-switch to Mode B", 0, selectedSection)
        composeTestRule.onAllNodesWithText("დიაგნოსტიკური ანალიზი", substring = true, useUnmergedTree = true)
            .assertCountEquals(0)

        // 3. Explicit top selector switch -> Mode B activates DiagnosticsWorkspaceScreen
        composeTestRule.onNodeWithTag("switch_to_diagnostics_tab", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, selectedSection)
        composeTestRule.onNodeWithTag("diagnostics_workspace_root", useUnmergedTree = true).assertIsDisplayed()

        // 4. Return to Mode A -> search for product so recommendation card appears only after user searches
        composeTestRule.onNodeWithTag("switch_to_ai_chat_tab", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, selectedSection)
        composeTestRule.onNodeWithTag("ai_shopping_quick_chips", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("ai_assistant_input", useUnmergedTree = true)
            .performTextReplacement("iPhone 16 Pro Max OLED ეკრანი")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_assistant_send_button", useUnmergedTree = true).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithTag("ai_rec_details_901", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 5. Click recommendation card Details & Order CTAs -> trigger real callbacks
        composeTestRule.onNodeWithTag("ai_rec_details_901", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(901, clickedProduct?.id)

        composeTestRule.onNodeWithTag("ai_rec_order_901", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(901, addedToCartProduct?.id)

        // 6. Click Clear Chat -> resets chat history cleanly to Mode A greeting with 0 cards
        composeTestRule.onNodeWithTag("ai_assistant_clear_button", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("ტელეფონი არ იტენება და რა სჭირს?", useUnmergedTree = true)
            .assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("ai_rec_card_901", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `missing catalog search shows stock inquiry button submits request and delivers potential order to admin`() {
        AuthAndOrderManager.loginWithGoogle(context, "customer@gmail.com", "გიორგი")
        val sampleCatalog = listOf(
            ProductItem(
                id = 901,
                title = "iPhone 16 Pro Max OLED ეკრანი",
                price = "234 ₾",
                originalPrice = "390 ₾",
                discountPercent = 40,
                category = "ეკრანები",
                brand = "Apple",
                compatibleModels = "iPhone 16 Pro Max",
                warranty = "6 თვე",
                inStock = true,
                isFeatured = true,
                imageUrl = ""
            )
        )

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                AiAssistantScreen(
                    products = sampleCatalog,
                    selectedSection = 0
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Before user searches: no inquiry button and no product cards
        composeTestRule.onAllNodesWithTag("ai_stock_inquiry_button", useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("ai_rec_card_901", useUnmergedTree = true).assertCountEquals(0)

        // 2. Search unavailable product -> AI reply exposes dedicated stock inquiry CTA
        composeTestRule.onNodeWithTag("ai_assistant_input", useUnmergedTree = true)
            .performTextReplacement("Nothing Phone 3 მატრიცა")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_assistant_send_button", useUnmergedTree = true).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("ვერ მოიძებნა", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("ai_stock_inquiry_button", useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Open stock availability inquiry dialog
        composeTestRule.onNodeWithTag("ai_assistant_chat_list", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("ai_stock_inquiry_button"))
        composeTestRule.onNodeWithTag("ai_stock_inquiry_button", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stock_availability_inquiry_dialog", useUnmergedTree = true).assertIsDisplayed()

        // 4. Submitting without phone triggers validation error and keeps dialog open
        composeTestRule.onNodeWithTag("stock_inquiry_submit_button", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stock_availability_inquiry_dialog", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("მიუთითეთ სწორი ტელეფონის ნომერი", useUnmergedTree = true).assertIsDisplayed()

        // 5. Fill phone & note and submit -> creates #REQ- order & confirmation message in chat
        composeTestRule.onNodeWithTag("stock_inquiry_name_input", useUnmergedTree = true)
            .performScrollTo()
            .performTextReplacement("გიორგი მ.")
        composeTestRule.onNodeWithTag("stock_inquiry_phone_input", useUnmergedTree = true)
            .performScrollTo()
            .performTextReplacement("555123456")
        composeTestRule.onNodeWithTag("stock_inquiry_note_input", useUnmergedTree = true)
            .performScrollTo()
            .performTextReplacement("გთხოვთ Original ვერსია შეამოწმოთ")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("stock_inquiry_submit_button", useUnmergedTree = true)
            .performScrollTo()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            AuthAndOrderManager.getOrders(context).any { it.isProductStockInquiry() }
        }
        composeTestRule.waitForIdle()

        val savedInquiry = AuthAndOrderManager.getOrders(context).first { it.isProductStockInquiry() }
        assertTrue(savedInquiry.orderNumber.startsWith("#REQ-"))
        assertEquals("Nothing Phone 3 მატრიცა", savedInquiry.items.first().productTitle)
        assertEquals("555123456", savedInquiry.userPhone)
        assertEquals("გთხოვთ Original ვერსია შეამოწმოთ", savedInquiry.paymentInternalNote)
        assertEquals("ფასი შეთანხმებით", savedInquiry.totalAmount)
        composeTestRule.onAllNodesWithTag("stock_availability_inquiry_dialog", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `admin orders dashboard surfaces stock inquiries filter badge and admin notification`() {
        // Authenticate sole admin session so local order store + admin notification panel are active
        AuthAndOrderManager.loginWithGoogle(context, AuthAndOrderManager.SOLE_ADMIN_EMAIL, "Beka Admin", hasAdminClaim = true)

        val inquiryResult = runBlocking {
            AuthAndOrderManager.submitProductStockInquiry(
                context = context,
                requestedProduct = "Sony Xperia 1 VI ეკრანი",
                customerName = "ნიკა",
                customerPhone = "599112233",
                customerNote = "ფასი მაინტერესებს"
            )
        }
        val inquiryOrder = inquiryResult.getOrThrow()

        val adminNotifs = BekaNotificationManager.loadAdminOrderNotificationsForAdmin(context)
        assertTrue(adminNotifs.any { it.orderId == inquiryOrder.id && it.title.contains("პროდუქტის მოთხოვნა") && !it.read })

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                AdminOrdersManagementView()
            }
        }
        composeTestRule.waitForIdle()

        // Admin sees "მოთხოვნები" filter tab, inquiry badge, note, and NO misleading payment confirm button
        composeTestRule.onNodeWithTag("admin_orders_lazy_column", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("admin_orders_status_filters_row"))
        composeTestRule.onNodeWithText("მოთხოვნები", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_orders_lazy_column", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("admin_order_card_${inquiryOrder.id}"))
        composeTestRule.onNodeWithTag("admin_order_inquiry_badge_${inquiryOrder.id}", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Sony Xperia 1 VI ეკრანი x1", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ფასი შეთანხმებით", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("admin_confirm_payment_btn_${inquiryOrder.id}", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `added headphones product auto registers normalized category and appears in catalog strip with accurate active count`() {
        val initialHeadphones = ProductItem(
            id = 910,
            title = "Hoco EW90 AirPods",
            price = "65 ₾",
            category = "ყურსასმენები",
            brand = "Hoco",
            inStock = true,
            isActive = true,
            imageUrl = ""
        )
        val screenProd = ProductItem(
            id = 911,
            title = "iPhone 16 Pro OLED ეკრანი",
            price = "350 ₾",
            category = "ეკრანები",
            brand = "Apple",
            inStock = true,
            isActive = true,
            imageUrl = ""
        )
        val inactiveRetired = ProductItem(
            id = 912,
            title = "Inactive Sample Display",
            price = "100 ₾",
            category = "ეკრანები",
            brand = "Apple",
            inStock = false,
            isActive = false,
            imageUrl = ""
        )
        assertEquals("ყურსასმენები", AdminStorage.normalizeCategoryName("ყურსასმენები"))
        assertEquals("ყურსასმენები", AdminStorage.normalizeCategoryName("ყურსასმენები და აუდიო"))
        assertEquals("როუტერები და ქსელი", AdminStorage.normalizeCategoryName("როუტერები & ქსელი"))

        val ensured = AdminStorage.ensureCategoryExistsForProduct(context, initialHeadphones.category)
        assertEquals("ყურსასმენები", ensured?.name)

        var selectedCat by mutableStateOf("ყველა")
        composeTestRule.setContent {
            RepairProTheme(darkTheme = false) {
                CatalogScreen(
                    products = listOf(initialHeadphones, screenProd, inactiveRetired),
                    searchQuery = "",
                    onSearchChange = {},
                    selectedCategory = selectedCat,
                    onCategoryChange = { selectedCat = it },
                    onProductClick = {},
                    onAddToCart = {},
                    onToggleFavorite = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Header shows ONLY active count ("სულ: 2"), single strip exists, headphones category renders & filters cleanly
        composeTestRule.onNodeWithTag("catalog_total_count", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("სულ: 2", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("catalog_category_strip", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("catalog_category_strip", useUnmergedTree = true)
            .performScrollToNode(hasTestTag("catalog_category_chip_ყურსასმენები"))
        composeTestRule.onNodeWithTag("catalog_category_chip_ყურსასმენები", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("ყურსასმენები", selectedCat)
        composeTestRule.onNodeWithText("Hoco EW90 AirPods", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `ai assistant answers conversational capability prompt warmly without stock inquiry fallback`() {
        val catalog = ProductRepository.getAllProducts()
        val session = AiShoppingSessionHolder()

        val reply = runBlocking {
            BstoreAiShoppingEngine.generateShoppingReply(
                context = context,
                history = emptyList(),
                latestUserMessage = "გამარჯობა, რისი გაკეთება შეგიძლია?",
                products = catalog,
                sessionHolder = session
            )
        }
        assertFalse("Capability greeting must never claim product not found", reply.text.contains("ვერ მოიძებნა"))
        assertFalse("Capability greeting must never show stock inquiry button", reply.allowStockInquiry)
        assertTrue("Capability reply must explain helpful shopping abilities", reply.text.contains("კატალოგ"))

        // Render in AiAssistantScreen UI and verify sending "გამარჯობა, რისი გაკეთება შეგიძლია?" shows no inquiry button
        composeTestRule.setContent {
            RepairProTheme(darkTheme = false) {
                AiAssistantScreen(
                    products = catalog,
                    selectedSection = 0
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ai_assistant_input", useUnmergedTree = true)
            .performTextReplacement("გამარჯობა, რისი გაკეთება შეგიძლია?")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_assistant_send_button", useUnmergedTree = true).performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithText("ჭკვიანი AI ასისტენტი", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("ვერ მოიძებნა", substring = true, useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("ai_stock_inquiry_button", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `fresh app preferences default to light mode and light glass bars and onboarding render cleanly`() {
        context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        AppPreferencesManager.init(context)
        assertFalse("Fresh setup must default to Light Mode", AppPreferencesManager.isDarkMode)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = AppPreferencesManager.isDarkMode) {
                androidx.compose.foundation.layout.Column {
                    BekaGlassTopAppBar(
                        isDark = false,
                        selectedTab = 0,
                        unreadNotificationsCount = 2,
                        cartItemCount = 1,
                        onMenuClick = {},
                        onTitleClick = {},
                        onNotificationsClick = {},
                        onProfileClick = {},
                        onCartClick = {}
                    )
                    BekaGlassBottomNavBar(
                        isDark = false,
                        selectedTab = 0,
                        onSelectTab = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("glassmorphic_top_app_bar_container", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("glassmorphic_bottom_nav_bar_container", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `redesigned side navigation drawer renders all sections real user data dialogs and non-admin hiding`() {
        AuthAndOrderManager.logout(context)
        val regRes = AuthAndOrderManager.registerWithEmail(
            context = context,
            name = "ლევან კაპანაძე",
            email = "levan.kapanadze@example.com",
            pass = "Password123!"
        )
        assertTrue(regRes.first)

        val sampleProducts = ProductRepository.getAllProducts().take(6)
        var selectedTabState by mutableStateOf(0)
        var drawerClosedCount by mutableStateOf(0)
        var adminOpenedCount by mutableStateOf(0)

        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                BekaSideNavigationDrawerContent(
                    selectedTab = selectedTabState,
                    unreadNotificationsCount = 2,
                    cartItemsCount = 3,
                    onSelectTab = { selectedTabState = it },
                    onOpenDiagnostics = {},
                    onOpenAdminPanel = { adminOpenedCount++ },
                    onCloseDrawer = { drawerClosedCount++ },
                    products = sampleProducts
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Header: BS icon, BSTORE wordmark, close button
        composeTestRule.onNodeWithTag("drawer_brand_icon", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_brand_wordmark", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_close_button", useUnmergedTree = true).assertIsDisplayed().performClick()
        assertEquals(1, drawerClosedCount)

        // 2. Profile card shows real user name, email, avatar, and non-Google provider badge (since Email login)
        composeTestRule.onNodeWithTag("drawer_profile_card", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_user_avatar", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_user_name", useUnmergedTree = true).assertTextContains("ლევან კაპანაძე")
        composeTestRule.onNodeWithTag("drawer_user_email", useUnmergedTree = true).assertTextContains("levan.kapanadze@example.com")
        composeTestRule.onNodeWithTag("drawer_auth_provider_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("ავტორიზებული (Google)", useUnmergedTree = true).assertCountEquals(0)

        // 3. Edit button opens EditProfileDialog
        composeTestRule.onNodeWithTag("drawer_edit_profile_button", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edit_profile_dialog", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("დახურვა", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // 4. Shopping section: ჩემი შეკვეთები, რჩეულები, კალათა
        composeTestRule.onNodeWithTag("drawer_my_orders_row", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_orders_dialog", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("დახურვა", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("drawer_favorites_row", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_favorites_dialog", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("დახურვა", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("drawer_cart_row", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(5, selectedTabState)

        // 5. Help section: დახმარება და კონტაქტი, სერვისცენტრი
        composeTestRule.onNodeWithTag("drawer_help_contact_row", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("drawer_help_contact_dialog", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("დახურვა", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("drawer_service_center_row", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("drawer_service_center_dialog", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("დახურვა", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // 6. Information section: წესები და პირობები, კონფიდენციალურობა, გადახდა და დაბრუნება
        composeTestRule.onNodeWithTag("drawer_terms_row", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bstore_legal_policies_dialog", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("დახურვა", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("drawer_privacy_row", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_payment_return_row", useUnmergedTree = true).performScrollTo().assertIsDisplayed()

        // 7. Settings row opens SecurityAndSettingsDialog
        composeTestRule.onNodeWithTag("drawer_settings_row", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("security_settings_dialog", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("დახურვა", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        // 8. Non-admin user MUST NOT see admin_button
        composeTestRule.onAllNodesWithTag("admin_button", useUnmergedTree = true).assertCountEquals(0)

        // 9. App version row & Logout button
        composeTestRule.onNodeWithTag("drawer_item_7", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_app_version_text", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_logout_button", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        assertNull("User must be signed out after clicking drawer logout", AuthAndOrderManager.getCurrentUser(context))

        // 10. Verified Google sole admin sees Google badge and admin_button ("მხოლოდ ადმინი")
        AuthAndOrderManager.loginWithGoogle(
            context = context,
            email = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            name = "ბექა მღებრიშვილი",
            hasAdminClaim = true
        )
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("drawer_profile_card", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_user_name", useUnmergedTree = true).assertTextContains("ბექა მღებრიშვილი")
        composeTestRule.onNodeWithTag("drawer_user_email", useUnmergedTree = true).assertTextContains("beqa1994@gmail.com")
        composeTestRule.onNodeWithText("ავტორიზებული (Google)", useUnmergedTree = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("admin_button", useUnmergedTree = true).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, adminOpenedCount)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `canonical category system and automatic product type detection assign categories accurately`() {
        AdminStorage.saveCategories(context, AdminStorage.getDefaultCategories())

        // 1. Admin and customer catalog category lists match and include "ყურსასმენები"
        val adminCats = AdminStorage.getCategories(context).filter { it.isActive && it.isVisible }
        val customerCats = AdminStorage.getCustomerVisibleCategories(context)
        assertEquals(adminCats.map { it.id }, customerCats.map { it.id })
        assertEquals(adminCats.map { it.name }, customerCats.map { it.name })
        assertTrue(customerCats.any { it.id == "cat_headphones" && it.name == "ყურსასმენები" })

        // 2. Unambiguous automatic product type & category detection
        val headphoneDetect = AdminStorage.detectProductCategoryAssignment(title = "AirPods Pro 2nd Gen Type-C")
        assertEquals("cat_headphones", headphoneDetect.categoryId)
        assertEquals("ყურსასმენები", headphoneDetect.categoryName)
        assertFalse(headphoneDetect.requiresAdminConfirmation)

        val keyboardDetect = AdminStorage.detectProductCategoryAssignment(title = "Logitech MX Keys Mini უსადენო კლავიატურა")
        assertEquals("cat_keyboards", keyboardDetect.categoryId)
        assertEquals("კლავიატურები", keyboardDetect.categoryName)

        val mouseDetect = AdminStorage.detectProductCategoryAssignment(title = "Razer DeathAdder V3 გეიმინგ მაუსი")
        assertEquals("cat_mice", mouseDetect.categoryId)
        assertEquals("მაუსები", mouseDetect.categoryName)

        val chargerDetect = AdminStorage.detectProductCategoryAssignment(title = "Apple 20W USB-C სწრაფი დამტენი და კაბელი")
        assertEquals("cat_chargers", chargerDetect.categoryId)
        assertEquals("დამტენები და კაბელები", chargerDetect.categoryName)

        val screenDetect = AdminStorage.detectProductCategoryAssignment(title = "iPhone 15 Pro Max OLED ეკრანი")
        assertEquals("cat_screens", screenDetect.categoryId)
        assertEquals("ეკრანები", screenDetect.categoryName)

        val routerDetect = AdminStorage.detectProductCategoryAssignment(title = "TP-Link Archer AX55 Wi-Fi 6 როუტერი")
        assertEquals("cat_routers", routerDetect.categoryId)
        assertEquals("როუტერები და ქსელი", routerDetect.categoryName)

        // 3. Explicit productType update updates category appropriately
        val explicitTypeDetect = AdminStorage.detectProductCategoryAssignment(
            title = "Sony WH-1000XM5",
            explicitProductType = "headphones"
        )
        assertEquals("cat_headphones", explicitTypeDetect.categoryId)
        assertEquals("ყურსასმენები", explicitTypeDetect.categoryName)

        // 4. Manual category override is preserved when explicitly selected
        val manualOverrideDetect = AdminStorage.detectProductCategoryAssignment(
            title = "AirPods Pro Case",
            manualCategoryOverride = "აქსესუარები"
        )
        assertEquals(AdminStorage.AssignmentConfidence.MANUAL_OVERRIDE, manualOverrideDetect.confidence)
        assertEquals("აქსესუარები", manualOverrideDetect.categoryName)

        // 5. Ambiguous product title requires admin confirmation rather than silent wrong assignment
        val ambiguousDetect = AdminStorage.detectProductCategoryAssignment(title = "Universal Pro Max")
        assertTrue("Ambiguous generic product title must require admin confirmation", ambiguousDetect.requiresAdminConfirmation)
        assertEquals(AdminStorage.AssignmentConfidence.AMBIGUOUS_SUGGESTION, ambiguousDetect.confidence)

        // 6. Save validation rejects blank or unconfirmed ambiguous category and accepts valid category
        assertFalse(AdminStorage.validateProductCategoryAssignment(context, "", false).first)
        assertFalse(AdminStorage.validateProductCategoryAssignment(context, "აქსესუარები", true).first)
        assertTrue(AdminStorage.validateProductCategoryAssignment(context, "ყურსასმენები", false).first)

        // 7. Inactive/hidden category does not appear in customer catalog
        val hiddenHeadphones = customerCats.first { it.id == "cat_headphones" }.copy(isVisible = false)
        AdminStorage.updateCategory(context, hiddenHeadphones)
        val visibleAfterHide = AdminStorage.getCustomerVisibleCategories(context)
        assertFalse("Hidden category must not appear in customer catalog", visibleAfterHide.any { it.id == "cat_headphones" })

        // Restore visibility
        AdminStorage.updateCategory(context, hiddenHeadphones.copy(isVisible = true))
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `admin manages service center location with Google Maps share link and drawer dialog reflects custom coordinates and map action`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AuthAndOrderManager.loginWithGoogle(context, email = "beqa1994@gmail.com", name = "ბექა (Admin)")

        // 1. ServiceCenterLocationManager resolves multiple formats of Google Maps links
        val shareLinkUri = ServiceCenterLocationManager.resolveGoogleMapsUri(
            rawInput = "https://maps.app.goo.gl/w8G6k689mZqU8Z488",
            fallbackAddress = "ახალციხე, კოსტავას N10"
        )
        assertEquals("https://maps.app.goo.gl/w8G6k689mZqU8Z488", shareLinkUri.toString())

        val coordUri = ServiceCenterLocationManager.resolveGoogleMapsUri(
            rawInput = "41.6389, 42.9861",
            fallbackAddress = "ახალციხე, კოსტავას N10"
        )
        assertTrue(coordUri.toString().startsWith("geo:41.6389,42.9861"))

        var showDrawerDialog by mutableStateOf(false)
        composeTestRule.setContent {
            RepairProTheme(darkTheme = true) {
                if (!showDrawerDialog) {
                    AdminServiceCenterLocationView(onClose = {})
                } else {
                    DrawerServiceCenterDialog(onDismiss = {})
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("admin_service_center_location_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_center_maps_url_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_service_center_test_map_button").assertIsDisplayed()
        composeTestRule.waitForIdle()

        // 3. Update to custom configuration and switch to DrawerServiceCenterDialog
        val customConfig = ServiceCenterLocationConfig(
            centerName = "BEKASERVICE",
            physicalAddress = "ახალციხე, კოსტავას ქ. N10",
            googleMapsUrl = "https://maps.app.goo.gl/testLocation999",
            phone = "+995 577 00 11 22",
            workingHours = "10:00 – 20:00 (ყოველდღე)"
        )
        ServiceCenterLocationManager.saveLocalConfig(context, customConfig)

        showDrawerDialog = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("drawer_service_center_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("BEKASERVICE", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("ახალციხე, კოსტავას ქ. N10", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_center_call_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("service_center_map_button").assertIsDisplayed()
        assertEquals("https://maps.app.goo.gl/testLocation999", ServiceCenterLocationManager.currentConfig.googleMapsUrl)
    }

    @androidx.compose.runtime.Composable
    private fun rememberMutableCartList(vararg initial: CartItem) =
        androidx.compose.runtime.remember { mutableStateListOf(*initial) }
}

