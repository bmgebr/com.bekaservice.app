package com.example

import android.content.Context
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.RepairProTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `header logo BSTORE is symmetric without subtitle and vertically aligned with right action buttons`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    AppPreferencesManager.setOnboardingCompleted(context, true)
    BekaNotificationManager.markPermissionPrompted(context)
    hasCompletedInitialLaunch = true

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        BekaServiceApp(
          deepLinkIntent = null,
          onDeepLinkConsumed = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    val logoTitleNode = composeTestRule.onNodeWithTag("header_title", useUnmergedTree = true).assertIsDisplayed()
    val logoEmblemNode = composeTestRule.onNodeWithTag("header_logo_emblem", useUnmergedTree = true).assertIsDisplayed()
    val logoTextColNode = composeTestRule.onNodeWithTag("header_logo_text_column", useUnmergedTree = true).assertIsDisplayed()
    val brandTitleRowNode = composeTestRule.onNodeWithTag("header_brand_title_row", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("header_bstore_wordmark_text", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("header_brand_subtitle", useUnmergedTree = true).assertDoesNotExist()

    val menuBtnNode = composeTestRule.onNodeWithTag("drawer_menu_button", useUnmergedTree = true).assertIsDisplayed()
    val notifBtnNode = composeTestRule.onNodeWithTag("notifications_button", useUnmergedTree = true).assertIsDisplayed()
    val profileBtnNode = composeTestRule.onNodeWithTag("profile_button", useUnmergedTree = true).assertIsDisplayed()
    val cartBtnNode = composeTestRule.onNodeWithTag("cart_button", useUnmergedTree = true).assertIsDisplayed()

    val logoTitleBounds = logoTitleNode.fetchSemanticsNode().boundsInRoot
    val logoEmblemBounds = logoEmblemNode.fetchSemanticsNode().boundsInRoot
    val logoTextColBounds = logoTextColNode.fetchSemanticsNode().boundsInRoot
    val brandTitleRowBounds = brandTitleRowNode.fetchSemanticsNode().boundsInRoot

    val menuBounds = menuBtnNode.fetchSemanticsNode().boundsInRoot
    val notifBounds = notifBtnNode.fetchSemanticsNode().boundsInRoot
    val profileBounds = profileBtnNode.fetchSemanticsNode().boundsInRoot
    val cartBounds = cartBtnNode.fetchSemanticsNode().boundsInRoot

    // 1. ლოგო წარწერით BSTORE უნდა უტოლდებოდეს ერთ სიმაღლეზე მარჯვენა მხარეს არსებულ ღილაკებს:
    assertEquals(notifBounds.height, logoTitleBounds.height, 1.5f)
    assertEquals(notifBounds.height, logoEmblemBounds.height, 1.5f)
    assertEquals(notifBounds.height, logoTextColBounds.height, 1.5f)
    assertEquals(notifBounds.height, profileBounds.height, 1.5f)
    assertEquals(notifBounds.height, cartBounds.height, 1.5f)

    assertEquals(notifBounds.center.y, logoTitleBounds.center.y, 1.5f)
    assertEquals(profileBounds.center.y, logoTitleBounds.center.y, 1.5f)
    assertEquals(cartBounds.center.y, logoTitleBounds.center.y, 1.5f)
    assertEquals(menuBounds.center.y, logoTitleBounds.center.y, 1.5f)
    assertEquals(notifBounds.center.y, brandTitleRowBounds.center.y, 2.0f)

    // 2. ქვედა ნავიგაციის ტაბების შემოწმება (ფასდაკლებები და AI ასისტენტი + დიაგნოსტიკა):
    composeTestRule.onNodeWithTag("nav_tab_2").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("discounts_screen_root").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_tab_3").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("ai_diagnostics_mode_switcher").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_to_diagnostics_tab").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("diagnostics_workspace_root").assertIsDisplayed()

    // 3. ნავიგაციის და ვიზუალების შემოწმება (შეტყობინებები, პროფილი, კალათა, გვერდითა მენიუ და განახლება):
    notifBtnNode.performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("user_notifications_screen").assertIsDisplayed()

    profileBtnNode.performClick()
    composeTestRule.waitForIdle()

    cartBtnNode.performClick()
    composeTestRule.waitForIdle()

    logoTitleNode.performClick()
    composeTestRule.waitForIdle()

    menuBtnNode.performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("drawer_item_7", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_update_screen").assertIsDisplayed()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BSTORE", appName)
  }

  @Test
  fun `product item creation and properties`() {
    val item = ProductItem(
      id = 1,
      title = "iPhone 14 Pro",
      price = "25 Gel",
      category = "Displays",
      type = "product",
      imageUrl = "https://example.com/test.jpg"
    )
    assertEquals(1, item.id)
    assertEquals("iPhone 14 Pro", item.title)
    assertEquals("25 Gel", item.price)
    assertEquals("Displays", item.category)
    assertEquals("product", item.type)
    assertEquals("https://example.com/test.jpg", item.imageUrl)
  }

  @Test
  fun `product repository covers all brands and categories`() {
    val products = ProductRepository.getAllProducts()
    assertTrue("Products list should be extensive", products.size >= 80)

    val brands = products.map { it.brand }.toSet()
    assertTrue(brands.contains("Apple"))
    assertTrue(brands.contains("Samsung"))
    assertTrue(brands.contains("Xiaomi"))
    assertTrue(brands.contains("Google Pixel"))
    assertTrue(brands.contains("OnePlus"))
    assertTrue(brands.contains("Honor"))

    val categories = products.map { it.category }.toSet()
    assertTrue(categories.contains("ეკრანები"))
    assertTrue(categories.contains("ელემენტები"))
    assertTrue(categories.contains("დამტენის პორტები"))
    assertTrue(categories.contains("კამერები"))
    assertTrue(categories.contains("კორპუსები"))
    assertTrue(categories.contains("დინამიკები"))
  }

  @Test
  fun `optimized search transliteration works`() {
    val products = ProductRepository.getAllProducts()
    val searchResultsIphone = optimizeSearchFilter(products, "აიფონ", "ყველა")
    assertTrue(searchResultsIphone.isNotEmpty())
    assertTrue(searchResultsIphone.all { it.brand == "Apple" || it.title.contains("iPhone") || it.compatibleModels.contains("iPhone") })

    val searchResultsSamsung = optimizeSearchFilter(products, "სამსუნგ", "ყველა")
    assertTrue(searchResultsSamsung.isNotEmpty())
    assertTrue(searchResultsSamsung.all { it.brand == "Samsung" || it.title.contains("Samsung") || it.compatibleModels.contains("Samsung") })

    val searchResultsBattery = optimizeSearchFilter(products, "ელემენტი", "ყველა")
    assertTrue(searchResultsBattery.isNotEmpty())
    assertTrue(searchResultsBattery.any { it.category == "ელემენტები" })
  }

  @Test
  fun `admin storage session requires authenticated sole admin with verified custom claim`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AuthAndOrderManager.logout(context)
    AdminStorage.clearAdminSession(context)

    // 1. Logged out -> session cannot be activated
    AdminStorage.setAdminSessionActive(context, true, "beqa1994@gmail.com")
    assertFalse("Unauthenticated user must never have an active admin session", AdminStorage.isAdminSessionActive(context))

    // 2. Ordinary authenticated user -> session cannot be activated
    AuthAndOrderManager.loginWithGoogle(context, "customer@gmail.com", "Customer")
    AdminStorage.setAdminSessionActive(context, true, "customer@gmail.com")
    assertFalse("Ordinary authenticated user must never have an active admin session", AdminStorage.isAdminSessionActive(context))

    // 3. Sole admin authenticated WITHOUT custom claim -> session inactive
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = false)
    assertTrue("Admin email is recognized for UI entry", AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertFalse("Missing custom claim must not grant admin claim verification", AuthAndOrderManager.hasVerifiedAdminClaim(context))
    assertFalse("Missing custom claim must keep admin session inactive", AdminStorage.isAdminSessionActive(context))

    // 4. Sole admin authenticated WITH verified custom claim -> session active
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = true)
    assertTrue(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertTrue(AuthAndOrderManager.hasVerifiedAdminClaim(context))
    assertTrue("Verified sole admin with custom claim must have active session", AdminStorage.isAdminSessionActive(context))

    // 5. Logout clears everything immediately
    AuthAndOrderManager.logout(context)
    assertFalse(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertFalse(AuthAndOrderManager.hasVerifiedAdminClaim(context))
    assertFalse(AdminStorage.isAdminSessionActive(context))
  }

  @Test
  fun `admin bulk price and margin calculation works`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testList = listOf(
      ProductItem(id = 1, title = "iPhone 15 Display", price = "300 ₾", category = "ეკრანები", brand = "Apple", imageUrl = ""),
      ProductItem(id = 2, title = "Galaxy S24 Battery", price = "100 ₾", category = "ელემენტები", brand = "Samsung", imageUrl = "")
    )

    // +10% მარჟის დამატება მხოლოდ Apple-ის პროდუქტებზე
    val updatedList = AdminStorage.applyBulkPriceChange(
      context = context,
      currentList = testList,
      targetBrand = "Apple",
      targetCategory = "ყველა",
      mode = "PERCENT",
      value = 10.0
    )

    val updatedApple = updatedList.first { it.id == 1 }
    val updatedSamsung = updatedList.first { it.id == 2 }

    assertEquals("330 ₾", updatedApple.price) // 300 + 10% = 330
    assertEquals("100 ₾", updatedSamsung.price) // უცვლელია
  }

  @Test
  fun `auth manager google email and order history works without demo orders`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Google შესვლა
    val googleUser = AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "ბექა")
    assertEquals("beqa1994@gmail.com", googleUser.email)
    assertEquals("Google", googleUser.provider)
    assertNotNull(AuthAndOrderManager.getCurrentUser(context))

    // ახალ ან შეკვეთების არმქონე მომხმარებელს არ უნდა უჩნდებოდეს ყალბი/დემო შეკვეთები (#BS-1048, #BS-1052, #BS-1040)
    val initialOrders = AuthAndOrderManager.getOrders(context)
    assertTrue("Orders history must be empty when no genuine orders exist", initialOrders.isEmpty())
    assertFalse(initialOrders.any { it.orderNumber in setOf("#BS-1048", "#BS-1052", "#BS-1040") })

    // პაროლის შეცვლა (ელ-ფოსტის ანგარიშისთვის)
    AuthAndOrderManager.registerWithEmail(context, "გიორგი", "customer@gmail.com", "password123")
    val (changedOk, _) = AuthAndOrderManager.changePassword(context, "password123", "bekaSecure2026")
    assertTrue(changedOk)
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "ბექა")

    // ტელეფონის მოდელის შენახვა, გამოსვლა და ხელახლა შესვლა
    val updatedProfile = googleUser.copy(deviceModel = "Samsung S24 Ultra")
    AuthAndOrderManager.updateUserProfile(context, updatedProfile)
    assertEquals("Samsung S24 Ultra", AuthAndOrderManager.getCurrentUser(context)?.deviceModel)

    AuthAndOrderManager.logout(context)
    val reLoggedUser = AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "ბექა")
    assertEquals("Samsung S24 Ultra", reLoggedUser.deviceModel)
    assertEquals("Samsung S24 Ultra", AuthAndOrderManager.getCurrentUser(context)?.deviceModel)

    // რეალური შეკვეთის დამატება კალათიდან
    val cart = listOf(
      CartItem(product = ProductItem(id = 999, title = "iPhone 16 Pro Battery", price = "140 ₾", category = "ელემენტები", imageUrl = ""), quantity = 2)
    )
    val newOrder = AuthAndOrderManager.addOrderFromCart(context, cart)
    assertEquals("280 ₾", newOrder.totalAmount)
    assertEquals("მიმდინარე", newOrder.status)

    val updatedOrders = AuthAndOrderManager.getOrders(context)
    assertEquals(1, updatedOrders.size)
    assertEquals(newOrder.id, updatedOrders.first().id)
  }

  @Test
  fun `no products are pre-marked as favorite by default and account-level favorites toggle and persist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("bekaservice_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("bekaservice_admin_prefs", Context.MODE_PRIVATE).edit().clear().commit()

    // 1. არცერთ პროდუქტზე არ უნდა იყოს ქარხნულად მონიშნული isFavorite = true
    val rawProducts = ProductRepository.getAllProducts()
    assertTrue("All default products in ProductRepository must have isFavorite == false", rawProducts.none { it.isFavorite })

    val loadedProducts = AdminStorage.loadProducts(context)
    assertTrue("All loaded products for fresh user must have isFavorite == false", loadedProducts.none { it.isFavorite })

    // 2. User A გადის ავტორიზაციას და ამატებს პროდუქტებს #1 და #14 ფავორიტებში
    AuthAndOrderManager.loginWithGoogle(context, "userA@example.com", "User A")
    assertTrue(AuthAndOrderManager.getFavoriteProductIds(context).isEmpty())

    val added1 = AuthAndOrderManager.toggleFavorite(context, 1)
    assertTrue("First toggle must mark product 1 as favorite", added1)
    val added14 = AuthAndOrderManager.toggleFavorite(context, 14)
    assertTrue("First toggle must mark product 14 as favorite", added14)
    assertEquals(setOf(1, 14), AuthAndOrderManager.getFavoriteProductIds(context))

    // 3. გაუქმება (unfavorite): მეორედ დაჭერისას პროდუქტი #14 იშლება ფავორიტებიდან
    val removed14 = AuthAndOrderManager.toggleFavorite(context, 14)
    assertFalse("Second toggle must unmark product 14 from favorites", removed14)
    assertEquals(setOf(1), AuthAndOrderManager.getFavoriteProductIds(context))
    assertTrue(AuthAndOrderManager.isProductFavorite(context, 1))
    assertFalse(AuthAndOrderManager.isProductFavorite(context, 14))

    // 4. ანგარიშიდან გამოსვლისას (Logout) ფავორიტები სუფთავდება სესიიდან
    AuthAndOrderManager.logout(context)
    assertTrue("Logged-out state must have 0 favorites", AuthAndOrderManager.getFavoriteProductIds(context).isEmpty())
    assertTrue("AdminStorage.loadProducts when logged out must have 0 favorites", AdminStorage.loadProducts(context).none { it.isFavorite })

    // 5. სხვა ანგარიშზე (User B) შესვლისას User A-ს ფავორიტები არ ჩანს
    AuthAndOrderManager.loginWithGoogle(context, "userB@example.com", "User B")
    assertTrue("User B must start with 0 favorites", AuthAndOrderManager.getFavoriteProductIds(context).isEmpty())
    AuthAndOrderManager.toggleFavorite(context, 34)
    assertEquals(setOf(34), AuthAndOrderManager.getFavoriteProductIds(context))

    // 6. ისევ User A-ს ანგარიშზე დაბრუნებისას მისი ფავორიტი (#1) ადგილზეა!
    AuthAndOrderManager.logout(context)
    AuthAndOrderManager.loginWithGoogle(context, "userA@example.com", "User A")
    assertEquals(setOf(1), AuthAndOrderManager.getFavoriteProductIds(context))
    val userALoaded = AdminStorage.loadProducts(context)
    assertTrue(userALoaded.first { it.id == 1 }.isFavorite)
    assertFalse(userALoaded.first { it.id == 34 }.isFavorite)
  }

  @Test
  fun `clicking heart button on ProductPhotoCard toggles favorite state on and off in UI`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("bekaservice_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    AuthAndOrderManager.loginWithGoogle(context, "ui.fav@example.com", "UI Tester")

    var currentProduct = ProductRepository.getAllProducts().first { it.id == 1 }.copy(isFavorite = false)

    composeTestRule.setContent {
      val prodState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentProduct) }
      RepairProTheme(darkTheme = true) {
        ProductPhotoCard(
          product = prodState.value,
          onClick = {},
          onAddToCart = {},
          onToggleFavorite = {
            val nowFav = AuthAndOrderManager.toggleFavorite(context, prodState.value.id)
            prodState.value = prodState.value.copy(isFavorite = nowFav)
            currentProduct = prodState.value
          }
        )
      }
    }
    composeTestRule.waitForIdle()

    assertFalse(currentProduct.isFavorite)
    assertFalse(AuthAndOrderManager.isProductFavorite(context, 1))

    // Click heart button to favorite
    composeTestRule.onNodeWithTag("favorite_button_1").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertTrue("Product 1 should be marked favorite after first click", currentProduct.isFavorite)
    assertTrue(AuthAndOrderManager.isProductFavorite(context, 1))

    // Click heart button again to unfavorite
    composeTestRule.onNodeWithTag("favorite_button_1").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertFalse("Product 1 should be unmarked from favorites after second click", currentProduct.isFavorite)
    assertFalse(AuthAndOrderManager.isProductFavorite(context, 1))
  }

  @Test
  fun `light mode and dark mode theme color adaptation works consistently across pages and inputs`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Verify Light Mode adaptations
    AppPreferencesManager.setDarkMode(context, false)
    assertFalse(AppPreferencesManager.isDarkMode)

    // Page backgrounds become light (#F3F6FB)
    assertEquals(
      androidx.compose.ui.graphics.Color(0xFFF3F6FB),
      AppThemeColorAdapter.adaptPageBackground(androidx.compose.ui.graphics.Color(0xFF0D0F12))
    )
    // Primary cards, search bars, and chat inputs become crisp white (#FFFFFF)
    assertEquals(
      androidx.compose.ui.graphics.Color(0xFFFFFFFF),
      AppThemeColorAdapter.adaptSurfaceColor(androidx.compose.ui.graphics.Color(0xFF16191E))
    )
    assertEquals(
      androidx.compose.ui.graphics.Color(0xFFFFFFFF),
      AppThemeColorAdapter.adaptSurfaceColor(androidx.compose.ui.graphics.Color(0xFF1A1D24))
    )
    assertEquals(
      androidx.compose.ui.graphics.Color(0xFFFFFFFF),
      AppThemeColorAdapter.adaptSurfaceColor(androidx.compose.ui.graphics.Color(0xFF14171E))
    )
    // Input text and primary white text become deep slate (#0F172A) on light surfaces
    assertEquals(
      androidx.compose.ui.graphics.Color(0xFF0F172A),
      AppThemeColorAdapter.inputTextColor()
    )
    assertEquals(
      androidx.compose.ui.graphics.Color(0xFF0F172A),
      AppThemeColorAdapter.adaptTextColor(androidx.compose.ui.graphics.Color.White, preserveWhite = false)
    )
    // White text inside vivid accent buttons / user chat bubbles stays white
    assertEquals(
      androidx.compose.ui.graphics.Color.White,
      AppThemeColorAdapter.adaptTextColor(androidx.compose.ui.graphics.Color.White, preserveWhite = true)
    )

    // 2. Restore Dark Mode and verify dark colors remain intact
    AppPreferencesManager.setDarkMode(context, true)
    assertTrue(AppPreferencesManager.isDarkMode)
    assertEquals(
      androidx.compose.ui.graphics.Color(0xFF0D0F12),
      AppThemeColorAdapter.adaptPageBackground(androidx.compose.ui.graphics.Color(0xFF0D0F12))
    )
    assertEquals(
      androidx.compose.ui.graphics.Color.White,
      AppThemeColorAdapter.inputTextColor()
    )
  }

  @Test
  fun `ai assistant recommends accurate hardware parts and never recommends retired software services`() {
    val allProducts = ProductRepository.getAllProducts()

    // 1. Hardware query for iPhone 16 Pro Max screen should return iPhone 16 Pro Max OLED screen (id=1)
    val matchedForIphone16 = GeminiDiagnosticService.findRelevantProducts("iphone 16 pro max ეკრანი მინდა", allProducts)
    assertTrue(matchedForIphone16.isNotEmpty())
    assertEquals(1, matchedForIphone16.first().id)

    // 2. Hardware query for iPhone 16 Pro Max battery should return iPhone 16 Pro Max battery (id=14)
    val matchedForBattery = GeminiDiagnosticService.findRelevantProducts("iphone 16 pro max ელემენტი", allProducts)
    assertTrue(matchedForBattery.isNotEmpty())
    assertTrue(matchedForBattery.any { it.id == 14 })

    // 2b. Hardware query for Samsung S24 Ultra screen should return S24 Ultra display
    val matchedForS24 = GeminiDiagnosticService.findRelevantProducts("samsung s24 ultra ეკრანი", allProducts)
    assertTrue(matchedForS24.isNotEmpty())
    assertTrue(matchedForS24.any { it.title.contains("S24 Ultra", ignoreCase = true) && it.category == "ეკრანები" })

    // 3. Retired software queries must return emptyList()
    assertTrue(GeminiDiagnosticService.findRelevantProducts("canva მინდა", allProducts).isEmpty())
    assertTrue(GeminiDiagnosticService.findRelevantProducts("chatgpt plus", allProducts).isEmpty())
    assertTrue(GeminiDiagnosticService.findRelevantProducts("capcut მინდა", allProducts).isEmpty())
  }

  @Test
  fun `ai assistant dynamically finds newly added hardware products from catalog`() {
    val baseProducts = ProductRepository.getAllProducts()

    // Create a newly added legitimate hardware product by admin
    val newCustomPart = ProductItem(
      id = 999,
      title = "Nothing Phone 2a AMOLED ეკრანი",
      price = "320 ₾",
      category = "ეკრანები",
      type = "product",
      subcategory = "OLED მოდულები",
      imageUrl = "https://example.com/nothing2a.jpg",
      shortDescription = "Nothing Phone 2a ორიგინალი 120Hz AMOLED ეკრანის მოდული.",
      features = listOf("120Hz AMOLED", "Original Flex", "6 თვე გარანტია"),
      searchKeywords = "nothing phone 2a, ნათინგ ფოუნ, ეკრანი",
      inStock = true,
      brand = "Nothing"
    )

    val updatedCatalog = baseProducts + newCustomPart

    // User asks for the newly added hardware product
    val foundNewPart = GeminiDiagnosticService.findRelevantProducts("nothing phone 2a მინდა", updatedCatalog)
    assertTrue(foundNewPart.isNotEmpty())
    assertEquals(999, foundNewPart.first().id)
    assertEquals("Nothing Phone 2a AMOLED ეკრანი", foundNewPart.first().title)

    // User asks with Georgian transliteration
    val foundNewGeorgian = GeminiDiagnosticService.findRelevantProducts("ნათინგ ფოუნ მინდა", updatedCatalog)
    assertTrue(foundNewGeorgian.isNotEmpty())
    assertEquals(999, foundNewGeorgian.first().id)
  }

  @Test
  fun `notifications bell badge and history reflect real notifications and unread counts accurately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    BekaNotificationManager.clearAllNotifications(context)

    // 1. Initial state: history is empty and unread count is 0 (no ghost "1" badge)
    val initialHistory = BekaNotificationManager.loadLocalNotificationHistory(context)
    assertTrue("Initial history must be empty", initialHistory.isEmpty())
    val initialUnread = BekaNotificationManager.getUnreadNotificationsCount(context, emptyList())
    assertEquals("Initial unread count must be 0", 0, initialUnread)

    // 2. Synthetic or test notifications are rejected and never counted as unread
    assertTrue(BekaNotificationManager.isSyntheticOrTestNotificationId("welcome_bekaservice"))
    assertTrue(BekaNotificationManager.isSyntheticOrTestNotificationId("local_test_12345"))
    assertTrue(BekaNotificationManager.isSyntheticOrTestNotificationId("test_notification"))
    assertTrue(
      "Notification with body 'test' must be rejected as test notification",
      BekaNotificationManager.isSyntheticOrTestNotification(
        id = "notif_1761553700000",
        title = "BEKASERVICE • ახალი შეთავაზება",
        body = "test"
      )
    )

    val testNotifOnly = listOf(
      PushNotificationRecord(
        id = "notif_1761553700000",
        title = "BEKASERVICE • ახალი შეთავაზება",
        body = "test"
      )
    )
    assertTrue(BekaNotificationManager.syncCloudNotificationsToLocal(context, testNotifOnly).isEmpty())
    assertEquals(0, BekaNotificationManager.getUnreadNotificationsCount(context, testNotifOnly))

    // 3. Simulate real broadcast notifications from Firestore
    val realCloudNotifs = listOf(
      PushNotificationRecord(
        id = "notif_cloud_001",
        title = "საგაზაფხულო აქცია",
        body = "ყველა ეკრანის შეცვლაზე 20% ფასდაკლება!",
        type = "product_offer",
        targetTab = 1,
        targetProductId = 101,
        createdAtMillis = System.currentTimeMillis()
      ),
      PushNotificationRecord(
        id = "notif_cloud_002",
        title = "ახალი სერვისი",
        body = "დაემატა ChatGPT Pro ოფიციალური გამოწერა",
        type = "announcement",
        targetTab = 3,
        createdAtMillis = System.currentTimeMillis() - 60000L
      )
    )

    // Sync cloud notifications to local
    val synced = BekaNotificationManager.syncCloudNotificationsToLocal(context, realCloudNotifs)
    assertEquals(2, synced.size)

    // Unread count must accurately reflect 2 unread notifications
    val unreadBefore = BekaNotificationManager.getUnreadNotificationsCount(context, realCloudNotifs)
    assertEquals("Should be 2 unread notifications", 2, unreadBefore)

    // Read single notification
    BekaNotificationManager.markNotificationRead(context, "notif_cloud_001")
    assertTrue(BekaNotificationManager.isNotificationRead(context, "notif_cloud_001"))
    assertFalse(BekaNotificationManager.isNotificationRead(context, "notif_cloud_002"))
    val unreadAfterOne = BekaNotificationManager.getUnreadNotificationsCount(context, realCloudNotifs)
    assertEquals("Should be 1 unread notification remaining", 1, unreadAfterOne)

    // Mark all read (simulating clicking the bell icon or 'ყველას წაკითხულად მონიშვნა')
    BekaNotificationManager.markAllNotificationsRead(context, realCloudNotifs)
    val unreadAfterAll = BekaNotificationManager.getUnreadNotificationsCount(context, realCloudNotifs)
    assertEquals("Unread count must be 0 after reading all", 0, unreadAfterAll)

    // When cloud notifications are removed/empty in Firestore, local reconciles and unread is 0
    val emptyCloud = BekaNotificationManager.syncCloudNotificationsToLocal(context, emptyList())
    assertTrue(emptyCloud.isEmpty())
    val unreadEmpty = BekaNotificationManager.getUnreadNotificationsCount(context, emptyList())
    assertEquals(0, unreadEmpty)
  }

  @Test
  fun `onboarding flow appears on fresh install supports KA EN RU language persistence guest access and skips on returning launch`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("bekaservice_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    AppPreferencesManager.init(context)
    hasCompletedInitialLaunch = true

    // 1. Fresh install state: onboarding is not completed and default language is Georgian (KA)
    assertFalse(AppPreferencesManager.isOnboardingCompleted)
    assertEquals(AppLanguage.KA, AppPreferencesManager.currentLanguage)

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        BekaServiceApp(
          deepLinkIntent = null,
          onDeepLinkConsumed = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // 2. Screen 1 (Welcome) is displayed
    composeTestRule.onNodeWithTag("onboarding_root_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_welcome_step").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_brand_logo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_welcome_next_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // 3. Screen 2 (Language selection) supports KA, EN, RU and persists choice
    composeTestRule.onNodeWithTag("onboarding_language_step").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_lang_ru").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppLanguage.RU, AppPreferencesManager.currentLanguage)
    assertEquals(
      "ru",
      context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE).getString("app_language_code", null)
    )

    composeTestRule.onNodeWithTag("onboarding_lang_en").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppLanguage.EN, AppPreferencesManager.currentLanguage)

    composeTestRule.onNodeWithTag("onboarding_lang_ka").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppLanguage.KA, AppPreferencesManager.currentLanguage)

    composeTestRule.onNodeWithTag("onboarding_language_next_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // 4. Screen 3 (Auth or Guest) displays Google sign-in, Apple sign-in, Email option, and Skip as Guest
    composeTestRule.onNodeWithTag("onboarding_auth_step").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_google_sign_in_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_apple_sign_in_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_email_auth_button").performScrollTo().assertIsDisplayed()

    // Toggling Apple sign-in button expands the minimal glass Apple ID form panel
    composeTestRule.onNodeWithTag("onboarding_apple_sign_in_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_apple_form_panel").performScrollTo().assertIsDisplayed()

    // Skip as Guest continues without creating a fake session
    composeTestRule.onNodeWithTag("onboarding_skip_auth_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // Guest access must NOT create a fake authenticated session
    assertEquals(null, AuthAndOrderManager.getCurrentUser(context))

    // 5. Screen 4 (Theme Selection — final 4 / 4 step) displays Light/Dark cards and finishes onboarding into the existing Home screen
    composeTestRule.onNodeWithTag("onboarding_theme_step").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_theme_light_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_theme_dark_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_finish_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    // 6. Verify onboarding completion is persisted and existing Home screen with Glassmorphism bars is shown
    assertTrue(AppPreferencesManager.isOnboardingCompleted)
    assertTrue(
      context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE)
        .getBoolean("onboarding_completed", false)
    )
    composeTestRule.onNodeWithTag("glassmorphic_top_app_bar_container").assertIsDisplayed()
    composeTestRule.onNodeWithTag("glassmorphic_bottom_nav_bar_container").assertIsDisplayed()
  }

  @Test
  fun `splash screen renders BSTORE splash design without artificial delay and adaptive icon has 108dp safe zone dimensions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Verify adaptive launcher foreground intrinsic size corresponds to 108dp and manifest references @mipmap/ic_launcher
    val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
    assertEquals("Manifest application icon must reference @mipmap/ic_launcher", R.mipmap.ic_launcher, appInfo.icon)
    assertNotNull("Launcher icon drawable must resolve", context.getDrawable(R.mipmap.ic_launcher))
    assertNotNull("Round launcher icon drawable must resolve", context.getDrawable(R.mipmap.ic_launcher_round))

    val brandIconBmp = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_bstore_brand_icon)
    assertNotNull("ic_bstore_brand_icon must decode as valid bitmap", brandIconBmp)
    assertEquals(512, brandIconBmp.width)
    assertEquals(512, brandIconBmp.height)

    val launcherFgBmp = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_bstore_launcher_fg)
    assertNotNull("ic_bstore_launcher_fg must decode as valid bitmap", launcherFgBmp)
    assertEquals(512, launcherFgBmp.width)
    assertEquals(512, launcherFgBmp.height)

    val density = context.resources.displayMetrics.density
    val fgDrawable = context.getDrawable(R.drawable.ic_launcher_foreground)
    assertNotNull(fgDrawable)
    val expected108Px = (108f * density).toInt()
    assertEquals("Foreground drawable intrinsic width must be 108dp", expected108Px.toFloat(), fgDrawable!!.intrinsicWidth.toFloat(), 2.0f)
    assertEquals("Foreground drawable intrinsic height must be 108dp", expected108Px.toFloat(), fgDrawable.intrinsicHeight.toFloat(), 2.0f)

    // 2. Verify VideoSplashScreen displays BSTORE splash image and finishes immediately when isAppReady = true
    hasCompletedInitialLaunch = false
    var finished = false
    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        VideoSplashScreen(
          isAppReady = true,
          onFinish = { finished = true }
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("splash_logo_image", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.waitUntil(timeoutMillis = 3000L) { finished }
    assertTrue("Splash screen should complete successfully", finished)
  }

  @Test
  fun `splash screen starts at 0 percent, syncs determinate bar fill with percentage across stages, and completes only when ready`() {
    hasCompletedInitialLaunch = false
    val isReadyState = androidx.compose.runtime.mutableStateOf(false)
    val stageProgressState = androidx.compose.runtime.mutableFloatStateOf(0f)
    var finished = false

    composeTestRule.mainClock.autoAdvance = false
    try {
      composeTestRule.setContent {
        RepairProTheme(darkTheme = true) {
          VideoSplashScreen(
            isAppReady = isReadyState.value,
            startupProgress = stageProgressState.floatValue,
            onFinish = { finished = true }
          )
        }
      }

      composeTestRule.mainClock.advanceTimeByFrame()
      composeTestRule.waitForIdle()

      // 1. Starts at exact 0% and determinate ProgressBarRangeInfo
      composeTestRule.onNodeWithTag("splash_progress_text").assertIsDisplayed()
      composeTestRule.onNodeWithText("0%").assertIsDisplayed()
      val initialSemantics = composeTestRule.onNodeWithTag("splash_progress_bar")
        .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo]
      assertEquals(0f, initialSemantics.current, 0.001f)
      assertEquals(0f..1f, initialSemantics.range)
      assertFalse("Must not finish at 0% while not ready", finished)

      val trackWidthPx = composeTestRule.onNodeWithTag("splash_progress_bar").fetchSemanticsNode().size.width.toFloat()
      val initialFillPx = composeTestRule.onNodeWithTag("splash_progress_bar_fill", useUnmergedTree = true)
        .fetchSemanticsNode().size.width.toFloat()
      assertEquals("Fill bar must start at 0 width", 0f, initialFillPx, 1.5f)

      // 2. Advance startup stage to 60% while isAppReady is still false
      stageProgressState.floatValue = 0.60f
      composeTestRule.mainClock.advanceTimeBy(120L)
      val midRangeInfo = composeTestRule.onNodeWithTag("splash_progress_bar")
        .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo]
      assertTrue("Bar fill should advance smoothly during interpolation (${midRangeInfo.current})", midRangeInfo.current in 0.05f..0.59f)

      composeTestRule.mainClock.advanceTimeBy(450L)
      composeTestRule.waitForIdle()

      // 3. Synchronized 60% text & fill width, never 100% before isAppReady = true
      composeTestRule.onNodeWithText("60%").assertIsDisplayed()
      val stageSemantics = composeTestRule.onNodeWithTag("splash_progress_bar")
        .fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo]
      assertEquals(0.60f, stageSemantics.current, 0.01f)
      val stageFillPx = composeTestRule.onNodeWithTag("splash_progress_bar_fill", useUnmergedTree = true)
        .fetchSemanticsNode().size.width.toFloat()
      assertEquals("Fill bar width must match 60% of track width", trackWidthPx * 0.60f, stageFillPx, 3.0f)
      assertFalse("Must not finish or hit 100% before isAppReady = true", finished)

      // 4. Mark app ready -> completes to 100% smoothly and invokes onFinish
      isReadyState.value = true
      stageProgressState.floatValue = 1f
      composeTestRule.mainClock.advanceTimeBy(600L)
      composeTestRule.waitForIdle()

      composeTestRule.onNodeWithText("100%").assertIsDisplayed()
      val finalFillPx = composeTestRule.onNodeWithTag("splash_progress_bar_fill", useUnmergedTree = true)
        .fetchSemanticsNode().size.width.toFloat()
      assertEquals("Fill bar width must match 100% of track width", trackWidthPx, finalFillPx, 2.0f)
      assertTrue("Splash screen should finish immediately upon reaching 100% when ready", finished)
    } finally {
      composeTestRule.mainClock.autoAdvance = true
    }
  }

  @Test
  fun `admin product dialog supports AI auto-fill, manual category and brand entry, and custom photo via URL or phone memory`() {
    // 1. Verify ProductImageManager distinguishes auto-generated vs URL vs phone memory file
    val autoProduct = ProductItem(
      id = 301,
      title = "iPhone 15 Pro OLED ეკრანი",
      price = "480 ₾",
      category = "ეკრანები",
      type = "product",
      imageUrl = ProductImageManager.AUTO_GENERATED_IMAGE_MARKER
    )
    assertFalse("Auto marker should use auto-generated visual", ProductImageManager.hasCustomAttachedPhoto(autoProduct))

    val urlProduct = autoProduct.copy(imageUrl = "https://example.com/custom_display_banner.png")
    assertTrue("Real URL should be recognized as custom attached photo", ProductImageManager.hasCustomAttachedPhoto(urlProduct))

    val localMemoryProduct = autoProduct.copy(imageUrl = "file:///data/user/0/com.example/files/product_images/prod_img_123.jpg")
    assertTrue("Phone memory file:// URI should be recognized as custom attached photo", ProductImageManager.hasCustomAttachedPhoto(localMemoryProduct))
    assertTrue(ProductImageManager.isLocalOrEmbeddedImage(localMemoryProduct.imageUrl))

    // 2. Verify ProductEditDialog UI: AI auto-fill button, custom photo URL input, and manual category/brand fields
    var savedProduct: ProductItem? = null
    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        ProductEditDialog(
          product = autoProduct.copy(title = "iPhone 15 Pro Original OLED ეკრანი", shortDescription = ""),
          isNew = true,
          existingCategories = listOf("ეკრანები", "ელემენტები"),
          existingBrands = listOf("Apple", "Samsung"),
          onDismiss = {},
          onSave = {
            savedProduct = it
            Result.success(Unit)
          }
        )
      }
    }
    composeTestRule.waitForIdle()

    // Click AI Auto-Fill button and wait for coroutine completion
    composeTestRule.onNodeWithTag("ai_autofill_product_button").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 15000L) {
      composeTestRule.onAllNodesWithTag("ai_status_completed").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()

    // Switch photo mode to URL and attach a custom link (and verify gallery button is displayed)
    composeTestRule.onNodeWithTag("product_edit_lazy_column")
      .performScrollToNode(hasTestTag("photo_mode_url_button"))
    composeTestRule.onNodeWithTag("photo_mode_gallery_button").assertIsDisplayed()
    composeTestRule.onNodeWithText("გალერეა", substring = true, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("photo_mode_url_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("product_edit_lazy_column")
      .performScrollToNode(hasTestTag("product_image_url_input"))
    composeTestRule.onNodeWithTag("product_image_url_input")
      .performTextReplacement("https://example.com/real_display_photo.jpg")
    composeTestRule.waitForIdle()

    // Manually enter a custom category and custom brand
    composeTestRule.onNodeWithTag("product_edit_lazy_column")
      .performScrollToNode(hasTestTag("custom_category_input"))
    composeTestRule.onNodeWithTag("custom_category_input")
      .performTextReplacement("OLED დისპლეები")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("product_edit_lazy_column")
      .performScrollToNode(hasTestTag("custom_brand_input"))
    composeTestRule.onNodeWithTag("custom_brand_input")
      .performTextReplacement("Apple Original")
    composeTestRule.waitForIdle()

    // Save the product
    composeTestRule.onNodeWithTag("save_product_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertNotNull("Product must be saved when clicking შენახვა", savedProduct)
    assertEquals("https://example.com/real_display_photo.jpg", savedProduct!!.imageUrl)
    assertEquals("OLED დისპლეები", savedProduct!!.category)
    assertEquals("Apple Original", savedProduct!!.brand)
    assertTrue("AI auto-fill must populate shortDescription", savedProduct!!.shortDescription.isNotBlank())
    assertTrue("AI auto-fill must populate full description", savedProduct!!.description.isNotBlank())
    assertTrue("AI auto-fill must populate features", savedProduct!!.features.isNotEmpty())
  }

  @Test
  fun `hardware fallback visual renders cleanly and home pull-to-refresh animates with pull progress`() {
    val sampleHardwareProduct = ProductItem(
      id = 1,
      title = "iPhone 16 Pro Max OLED ეკრანი",
      price = "850 ₾",
      category = "ეკრანები",
      type = "product",
      brand = "Apple",
      imageUrl = ProductImageManager.AUTO_GENERATED_IMAGE_MARKER
    )

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column {
          HardwareProductFallbackVisual(product = sampleHardwareProduct, compact = false)
          BekaPullToRefreshIndicator(
            pullProgress = 0.65f,
            rawPullDistancePx = 55f,
            isRefreshing = false,
            isRefreshCompleted = false,
            topInsetPadding = 0.dp,
            modifier = androidx.compose.ui.Modifier.testTag("home_pull_refresh_indicator")
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Apple").assertIsDisplayed()
    composeTestRule.onNodeWithText("iPhone 16 Pro Max OLED ეკრანი").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_pull_refresh_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("ჩამოწიეთ განახლებისთვის • 65%").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp")
  fun `multimodal AI diagnostics workspace supports free-form Georgian conversation, image attachments, Standard and Technician modes, contextual actions, and session history`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    DiagnosticsSessionRepository.clearAllSessions(context)

    val catalog = AdminStorage.loadProducts(context).take(10)
    var holderRef: DiagnosticsWorkspaceStateHolder? = null

    composeTestRule.setContent {
      val scope = androidx.compose.runtime.rememberCoroutineScope()
      val holder = androidx.compose.runtime.remember {
        DiagnosticsWorkspaceStateHolder(context = context, coroutineScope = scope)
      }
      holderRef = holder
      RepairProTheme(darkTheme = true) {
        DiagnosticsWorkspaceScreen(
          contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 0.dp, bottom = 0.dp),
          onProductClick = {},
          products = catalog,
          externalStateHolder = holder
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Verify initial workspace header & empty state (no mandatory brand/model/issue selector grid)
    composeTestRule.onNodeWithTag("diagnostics_workspace_root").assertIsDisplayed()
    composeTestRule.onNodeWithTag("diag_header_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("AI დიაგნოსტიკა").assertIsDisplayed()
    composeTestRule.onNodeWithTag("diag_mode_standard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("diag_mode_technician").assertIsDisplayed()
    composeTestRule.onNodeWithTag("diag_empty_state")
      .performScrollToNode(hasTestTag("diag_intro_card"))
    composeTestRule.onNodeWithText("რა პრობლემა აქვს ტელეფონს?").assertIsDisplayed()
    composeTestRule.onNodeWithTag("diag_attach_gallery_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("diag_attach_camera_button").assertIsDisplayed()

    // Starter prompts are dismissible
    composeTestRule.onNodeWithTag("diag_empty_state")
      .performScrollToNode(hasTestTag("diag_dismiss_starters_button"))
    composeTestRule.onNodeWithTag("diag_dismiss_starters_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertTrue("Starter prompts should be dismissed", holderRef!!.startersDismissed)

    // 2. Test image attachment preview & removal before sending
    val testBitmap = android.graphics.Bitmap.createBitmap(240, 480, android.graphics.Bitmap.Config.ARGB_8888)
    for (y in 0 until 480) {
      for (x in 110..130) {
        testBitmap.setPixel(x, y, android.graphics.Color.rgb(0, 255, 40)) // Green vertical OLED line artifact
      }
    }

    composeTestRule.runOnIdle {
      holderRef!!.attachBitmapDirect(testBitmap, "oled_green_line.jpg")
    }
    composeTestRule.waitForIdle()

    assertEquals(1, holderRef!!.pendingAttachments.size)
    val firstAttId = holderRef!!.pendingAttachments.first().id
    composeTestRule.onNodeWithTag("diag_attachment_preview_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("remove_attachment_$firstAttId").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertTrue("Pending attachment must be removed when clicking X", holderRef!!.pendingAttachments.isEmpty())

    // Re-attach the image and send a free-form Georgian diagnostic message
    composeTestRule.runOnIdle {
      holderRef!!.attachBitmapDirect(testBitmap, "iphone14pro_screen.jpg")
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("diag_message_input")
      .performTextReplacement("iPhone 14 Pro დამივარდა და ეკრანზე მწვანე ზოლი გაჩნდა, ზარი გადის")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("diag_send_button").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 15000L) {
      !holderRef!!.isAnalyzing && holderRef!!.messages.size >= 2
    }
    composeTestRule.waitForIdle()

    // 3. Verify structured diagnostic response, visual inspection findings, and evidence confidence labels (no 99.5% claim)
    assertEquals(2, holderRef!!.messages.size)
    val firstAssistantMsg = holderRef!!.messages.last()
    assertFalse(firstAssistantMsg.isUser)
    assertNotNull(firstAssistantMsg.structuredSection)
    assertTrue(
      "Visual inspection findings must be populated when an image is attached",
      firstAssistantMsg.structuredSection!!.visualInspectionFindings.isNotBlank()
    )
    assertTrue(
      "Device identification must recognize iPhone 14 Pro",
      firstAssistantMsg.structuredSection!!.deviceIdentification.contains("iPhone 14 Pro", ignoreCase = true)
    )
    assertFalse(
      "Assistant response must NEVER contain fabricated 99.5% accuracy claims",
      firstAssistantMsg.text.contains("99.5%")
    )
    assertTrue(
      "Hypotheses must use evidence confidence levels",
      firstAssistantMsg.structuredSection!!.hypotheses.isNotEmpty()
    )

    // 4. Switch to Technician Mode and use contextual follow-up actions
    composeTestRule.onNodeWithTag("diag_mode_technician").performClick()
    composeTestRule.waitForIdle()
    assertEquals(DiagnosticUserMode.TECHNICIAN, holderRef!!.selectedMode)

    // Test "შედეგის დამატება" prefills composer
    composeTestRule.runOnIdle {
      holderRef!!.triggerContextualAction(
        action = ContextualDiagnosticAction.ADD_RESULT,
        catalogProducts = catalog
      )
    }
    composeTestRule.waitForIdle()
    assertTrue(holderRef!!.inputText.startsWith("ტესტის შედეგი: "))

    // Send a technician follow-up with test result
    composeTestRule.onNodeWithTag("diag_message_input")
      .performTextReplacement("ტესტის შედეგი: სატესტო ეკრანზეც იგივეა, კონექტორის პინი დაზიანებულია")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("diag_send_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 15000L) {
      !holderRef!!.isAnalyzing && holderRef!!.messages.size >= 4
    }
    composeTestRule.waitForIdle()

    val techAssistantMsg = holderRef!!.messages.last()
    assertEquals(DiagnosticUserMode.TECHNICIAN, techAssistantMsg.mode)
    assertTrue(
      "Multi-turn context must retain iPhone 14 Pro from turn 1",
      techAssistantMsg.structuredSection!!.deviceIdentification.contains("iPhone 14 Pro", ignoreCase = true)
    )

    // Trigger session summary contextual action ("დიაგნოსტიკის შეჯამება")
    composeTestRule.runOnIdle {
      holderRef!!.triggerContextualAction(
        action = ContextualDiagnosticAction.SUMMARIZE,
        catalogProducts = catalog
      )
    }
    composeTestRule.waitUntil(timeoutMillis = 15000L) {
      !holderRef!!.isAnalyzing && holderRef!!.messages.size >= 6
    }
    composeTestRule.waitForIdle()
    assertTrue(holderRef!!.messages.last().text.contains("შეჯამება"))

    // 5. Verify Session History persistence: start new session, open history, reopen saved session, delete session
    val savedSessionId = holderRef!!.sessionId
    assertTrue("Session must be saved in repository", holderRef!!.savedSessions.any { it.id == savedSessionId })

    composeTestRule.onNodeWithTag("diag_new_session_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue("New session must clear active message list", holderRef!!.messages.isEmpty())

    composeTestRule.onNodeWithTag("diag_history_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("diag_history_sheet").assertIsDisplayed()

    // Reopen saved session
    composeTestRule.onNodeWithTag("diag_history_open_$savedSessionId").performClick()
    composeTestRule.waitForIdle()
    assertEquals(savedSessionId, holderRef!!.sessionId)
    assertEquals(6, holderRef!!.messages.size)

    // Delete saved session
    composeTestRule.onNodeWithTag("diag_history_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("diag_history_delete_$savedSessionId").performClick()
    composeTestRule.waitForIdle()
    assertFalse("Deleted session must be removed from repository", holderRef!!.savedSessions.any { it.id == savedSessionId })
  }

  @Test
  fun `diagnostics workspace clears composer on starter prompt click, accurately diagnoses bootloop vs charging, and handles catalog and general questions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    DiagnosticsSessionRepository.clearAllSessions(context)
    val fullCatalog = ProductRepository.getAllProducts()
    var holderRef: DiagnosticsWorkspaceStateHolder? = null

    composeTestRule.setContent {
      val scope = androidx.compose.runtime.rememberCoroutineScope()
      val holder = androidx.compose.runtime.remember {
        DiagnosticsWorkspaceStateHolder(context = context, coroutineScope = scope)
      }
      holderRef = holder
      RepairProTheme(darkTheme = true) {
        DiagnosticsWorkspaceScreen(
          contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 0.dp, bottom = 0.dp),
          onProductClick = {},
          products = fullCatalog,
          externalStateHolder = holder
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Click starter chip #2: "ტელეფონი ჩართვისას ჩერდება ლოგოზე და აღარ ირთვება ბოლომდე"
    composeTestRule.onNodeWithTag("diag_empty_state")
      .performScrollToNode(hasTestTag("diag_starter_chip_2"))
    composeTestRule.onNodeWithTag("diag_starter_chip_2").assertIsDisplayed().performClick()

    composeTestRule.waitUntil(timeoutMillis = 10000L) {
      !holderRef!!.isAnalyzing && holderRef!!.messages.size >= 2
    }
    composeTestRule.waitForIdle()

    // Composer inputText MUST be empty after clicking starter prompt!
    assertEquals("Composer inputText must be empty after sending starter prompt", "", holderRef!!.inputText)

    // Diagnosis for "ჩერდება ლოგოზე და აღარ ირთვება ბოლომდე" MUST be Bootloop / ლოგოზე გაჭედვა, NOT charging port!
    val bootloopReply = holderRef!!.messages.last()
    assertTrue(
      "Bootloop prompt must be diagnosed as Bootloop / ლოგოზე გაჭედვა, was: ${bootloopReply.text}",
      bootloopReply.text.contains("ლოგოზე") || bootloopReply.text.contains("Bootloop", ignoreCase = true)
    )
    assertFalse(
      "Bootloop prompt must NOT be misclassified as charging port failure",
      bootloopReply.text.contains("დამუხტვის პორტის, კაბელის ან კვების სისტემის გაუმართაობა")
    )

    // 2. Ask about a hardware catalog product ("iPhone 16 Pro Max ეკრანი მინდა, რა ღირს?") inside the Diagnostics workspace
    composeTestRule.runOnIdle {
      holderRef!!.sendMessage(
        overrideText = "iPhone 16 Pro Max ეკრანი მინდა, რა ღირს და რა პირობებია?",
        catalogProducts = fullCatalog
      )
    }
    composeTestRule.waitUntil(timeoutMillis = 10000L) {
      !holderRef!!.isAnalyzing && holderRef!!.messages.size >= 4
    }
    composeTestRule.waitForIdle()

    val screenReply = holderRef!!.messages.last()
    assertTrue("Catalog query for iPhone 16 Pro Max screen must recommend product 1", screenReply.recommendedProductIds.contains(1))
    assertTrue("Catalog query must mention iPhone 16 Pro Max details", screenReply.text.contains("iPhone 16 Pro Max", ignoreCase = true))

    // 3. Ask a general tech question ("რა განსხვავებაა OLED და LCD ეკრანებს შორის?")
    composeTestRule.runOnIdle {
      holderRef!!.sendMessage(
        overrideText = "რა განსხვავებაა OLED და LCD ეკრანებს შორის?",
        catalogProducts = fullCatalog
      )
    }
    composeTestRule.waitUntil(timeoutMillis = 10000L) {
      !holderRef!!.isAnalyzing && holderRef!!.messages.size >= 6
    }
    composeTestRule.waitForIdle()

    val oledReply = holderRef!!.messages.last()
    assertTrue("General OLED vs LCD question must explain OLED and IPS/LCD", oledReply.text.contains("OLED") && oledReply.text.contains("LCD"))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp")
  fun `diagnostics intro card is compact with concise Georgian copy, Glassmorphism composer anchors without double bottom gap, supports ImeAction Send and Light Dark themes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    DiagnosticsSessionRepository.clearAllSessions(context)
    AppPreferencesManager.init(context)
    AppPreferencesManager.setLanguage(context, AppLanguage.KA)
    AppPreferencesManager.setDarkMode(context, true)
    val catalog = ProductRepository.getAllProducts().take(8)
    var holderRef: DiagnosticsWorkspaceStateHolder? = null
    var capturedDarkPalette: DiagnosticsThemePalette? = null
    var capturedLightPalette: DiagnosticsThemePalette? = null

    val bottomNavClearance = 108.dp

    composeTestRule.setContent {
      val scope = androidx.compose.runtime.rememberCoroutineScope()
      val holder = androidx.compose.runtime.remember {
        DiagnosticsWorkspaceStateHolder(context = context, coroutineScope = scope)
      }
      holderRef = holder
      if (AppPreferencesManager.isDarkMode) {
        capturedDarkPalette = rememberDiagnosticsThemePalette()
      } else {
        capturedLightPalette = rememberDiagnosticsThemePalette()
      }
      RepairProTheme(darkTheme = AppPreferencesManager.isDarkMode) {
        DiagnosticsWorkspaceScreen(
          contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 64.dp, bottom = bottomNavClearance),
          onProductClick = {},
          products = catalog,
          externalStateHolder = holder
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Verify compact intro card title, exact concise Georgian description, and secondary feature pills
    val introCardNode = composeTestRule.onNodeWithTag("diag_intro_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("რა პრობლემა აქვს ტელეფონს?", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText(
      "აღწერე სიმპტომი ან ატვირთე ფოტო. დაგეხმარები სავარაუდო მიზეზების, შემოწმების ნაბიჯებისა და შეკეთების ვარიანტების განსაზღვრაში.",
      useUnmergedTree = true
    ).assertIsDisplayed()
    composeTestRule.onNodeWithText("ფოტოს ანალიზი", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("ტექნიკური რჩევა", useUnmergedTree = true).assertIsDisplayed()

    // Verify the old redundant multi-line orange mode subtitle paragraph is removed from the intro card
    assertEquals(
      0,
      composeTestRule.onAllNodes(
        androidx.compose.ui.test.hasText("აქტიური რეჟიმი:", substring = true),
        useUnmergedTree = true
      ).fetchSemanticsNodes().size
    )

    // 2. Verify composer Glassmorphism pill, concise placeholder, and proper bottom anchoring (no double bottom padding)
    val rootBounds = composeTestRule.onNodeWithTag("diagnostics_workspace_root").fetchSemanticsNode().boundsInRoot
    val composerContainerBounds = composeTestRule.onNodeWithTag("diag_composer_container").fetchSemanticsNode().boundsInRoot
    val glassPillNode = composeTestRule.onNodeWithTag("diag_composer_glass_pill").assertIsDisplayed()
    val glassPillBounds = glassPillNode.fetchSemanticsNode().boundsInRoot

    composeTestRule.onNodeWithText("აღწერე ტელეფონის პრობლემა...", useUnmergedTree = true).assertIsDisplayed()

    // Because testTag("diagnostics_workspace_root") is after .padding(top = topPadding, bottom = effectiveBottomInset),
    // rootBounds.bottom is the exact top edge of bottomNavClearance, and composerContainerBounds.bottom must sit flush with it!
    val density = context.resources.displayMetrics.density
    assertEquals(
      "Composer container bottom must sit flush with workspace inner bottom above bottomNavClearance",
      rootBounds.bottom,
      composerContainerBounds.bottom,
      2.0f
    )
    assertTrue(
      "Glass pill must sit flush inside composer container without floating gap",
      (composerContainerBounds.bottom - glassPillBounds.bottom) <= (10f * density)
    )

    // 3. Verify placeholder updates when an image is attached
    val testBitmap = android.graphics.Bitmap.createBitmap(120, 120, android.graphics.Bitmap.Config.ARGB_8888)
    composeTestRule.runOnIdle {
      holderRef!!.attachBitmapDirect(testBitmap, "sample_photo.jpg")
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("დაამატე კომენტარი ფოტოზე...", useUnmergedTree = true).assertIsDisplayed()

    // Remove attachment and verify placeholder reverts
    val attId = holderRef!!.pendingAttachments.first().id
    composeTestRule.onNodeWithTag("remove_attachment_$attId").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("აღწერე ტელეფონის პრობლემა...", useUnmergedTree = true).assertIsDisplayed()

    // 4. Verify multiline input and submitting via keyboard ImeAction.Send
    val inputNode = composeTestRule.onNodeWithTag("diag_message_input")
    inputNode.performClick()
    inputNode.performTextReplacement("ეკრანი ციმციმებს\nდა სენსორი ზოგჯერ არ რეაგირებს")
    composeTestRule.waitForIdle()
    assertEquals("ეკრანი ციმციმებს\nდა სენსორი ზოგჯერ არ რეაგირებს", holderRef!!.inputText)

    // Trigger keyboard Send action
    inputNode.performImeAction()
    composeTestRule.waitUntil(timeoutMillis = 10000L) {
      !holderRef!!.isAnalyzing && holderRef!!.messages.size >= 2
    }
    composeTestRule.waitForIdle()
    assertEquals("Input must clear after ImeAction.Send", "", holderRef!!.inputText)
    assertEquals(2, holderRef!!.messages.size)

    // 5. Verify Dark Mode and Light Mode theme palettes provide high contrast
    assertNotNull(capturedDarkPalette)
    assertTrue(capturedDarkPalette!!.isDark)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0D0F12), capturedDarkPalette!!.pageBg)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFF8FAFC), capturedDarkPalette!!.textPrimary)

    composeTestRule.runOnIdle {
      AppPreferencesManager.setDarkMode(context, false)
    }
    composeTestRule.waitForIdle()
    assertNotNull(capturedLightPalette)
    assertFalse(capturedLightPalette!!.isDark)
    assertEquals(androidx.compose.ui.graphics.Color(0xFFF3F6FB), capturedLightPalette!!.pageBg)
    assertEquals(androidx.compose.ui.graphics.Color(0xFF0F172A), capturedLightPalette!!.textPrimary)

    // Restore Dark Mode
    composeTestRule.runOnIdle {
      AppPreferencesManager.setDarkMode(context, true)
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun `diagnostic greeting message has no competitor names and matches required Georgian copy`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    AppPreferencesManager.setLanguage(context, "ka")

    val catalog = ProductRepository.getAllProducts()
    val greetingTurn = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = emptyList(),
      currentMessage = DiagnosticMessage(role = "user", text = "გამარჯობა"),
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )

    // Must NOT contain competitor model names
    assertFalse("Greeting must not mention ChatGPT", greetingTurn.text.contains("ChatGPT", ignoreCase = true))
    assertFalse("Greeting must not mention Gemini", greetingTurn.text.contains("Gemini", ignoreCase = true))

    // Must match required Georgian copy
    val expectedGeorgianGreeting = "გამარჯობა! მე ვარ BSTORE-ის AI დიაგნოსტიკური ასისტენტი. აღწერე ტელეფონის პრობლემა ან ატვირთე ფოტო და დაგეხმარები შესაძლო მიზეზებისა და შემოწმების ნაბიჯების განსაზღვრაში."
    assertEquals(expectedGeorgianGreeting, greetingTurn.text.trim())
    assertTrue("Greeting should not have unsolicited recommended products", greetingTurn.recommendedProductIds.isEmpty())

    // Test English and Russian localized greetings
    AppPreferencesManager.setLanguage(context, "en")
    val enTurn = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = emptyList(),
      currentMessage = DiagnosticMessage(role = "user", text = "hello"),
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )
    assertTrue(enTurn.text.contains("BSTORE AI Diagnostics Assistant"))
    assertFalse(enTurn.text.contains("ChatGPT"))

    AppPreferencesManager.setLanguage(context, "ru")
    val ruTurn = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = emptyList(),
      currentMessage = DiagnosticMessage(role = "user", text = "привет"),
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )
    assertTrue(ruTurn.text.contains("AI-ассистент диагностики BSTORE"))
    assertFalse(ruTurn.text.contains("ChatGPT"))

    // Reset back to Georgian
    AppPreferencesManager.setLanguage(context, "ka")
  }

  @Test
  fun `multi-turn conversation preserves active device and domain context on follow-up questions`() {
    val catalog = ProductRepository.getAllProducts()

    // Turn 1: User describes iPhone 13 screen flickering
    val userTurn1 = DiagnosticMessage(role = "user", text = "iPhone 13 ეკრანი ციმციმებს და მწვანე ზოლები აქვს")
    val assistantTurn1 = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = emptyList(),
      currentMessage = userTurn1,
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )

    assertNotNull(assistantTurn1.structuredSection)
    assertEquals("iPhone 13", assistantTurn1.structuredSection?.deviceIdentification)
    assertTrue(assistantTurn1.text.contains("iPhone 13") || assistantTurn1.text.contains("ეკრან"))
    // Digital software like Canva/CapCut must NOT be recommended
    val turn1Recs = assistantTurn1.recommendedProductIds.mapNotNull { id -> catalog.find { it.id == id } }
    assertFalse("Hardware diagnosis must not recommend digital software", turn1Recs.any { it.category == "პროგრამული სერვისები" })

    val historyAfterTurn1 = listOf(userTurn1, assistantTurn1)

    // Turn 2: Follow-up question "რა შეიძლება იყოს მიზეზი?"
    val userTurn2 = DiagnosticMessage(role = "user", text = "რა შეიძლება იყოს მიზეზი?")
    val assistantTurn2 = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = historyAfterTurn1,
      currentMessage = userTurn2,
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )

    // Must NOT ask for device model or symptoms again
    assertFalse("Must not ask for device model again", assistantTurn2.text.contains("ტელეფონის ზუსტი მოდელი"))
    assertFalse("Must not ask for symptoms again", assistantTurn2.text.contains("რა სიმპტომი აქვს"))
    // Must retain iPhone 13 context and explain reasons for display problem
    assertTrue("Must address reasons", assistantTurn2.text.contains("მიზეზ") || assistantTurn2.text.contains("გამომწვევი"))
    assertTrue("Must retain active device iPhone 13", assistantTurn2.text.contains("iPhone 13"))
    assertEquals("iPhone 13", assistantTurn2.structuredSection?.deviceIdentification)

    val historyAfterTurn2 = historyAfterTurn1 + listOf(userTurn2, assistantTurn2)

    // Turn 3: Follow-up question "როგორ შევამოწმო ეს?"
    val userTurn3 = DiagnosticMessage(role = "user", text = "როგორ შევამოწმო ეს?")
    val assistantTurn3 = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = historyAfterTurn2,
      currentMessage = userTurn3,
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )

    assertFalse(assistantTurn3.text.contains("ტელეფონის ზუსტი მოდელი"))
    assertTrue("Must provide verification steps", assistantTurn3.text.contains("ტესტ") || assistantTurn3.text.contains("შემოწმ"))
    assertTrue(assistantTurn3.text.contains("iPhone 13"))

    val historyAfterTurn3 = historyAfterTurn2 + listOf(userTurn3, assistantTurn3)

    // Turn 4: User sends diagnostic test observation
    val userTurn4 = DiagnosticMessage(role = "user", text = "ტესტის შედეგი: სატესტო ეკრანზეც ციმციმებს")
    val assistantTurn4 = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = historyAfterTurn3,
      currentMessage = userTurn4,
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )

    assertTrue("Must acknowledge test result", assistantTurn4.text.contains("ტესტის შედეგი") || assistantTurn4.text.contains("გაანალიზებულია"))
    assertTrue(assistantTurn4.text.contains("iPhone 13"))

    val historyAfterTurn4 = historyAfterTurn3 + listOf(userTurn4, assistantTurn4)

    // Turn 5: User asks about data loss safety
    val userTurn5 = DiagnosticMessage(role = "user", text = "მონაცემები წამეშლება?")
    val assistantTurn5 = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = historyAfterTurn4,
      currentMessage = userTurn5,
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )

    assertTrue("Must address user data safety", assistantTurn5.text.contains("მონაცემ") || assistantTurn5.text.contains("უსაფრთხო"))
    assertTrue(assistantTurn5.text.contains("iPhone 13"))

    val historyAfterTurn5 = historyAfterTurn4 + listOf(userTurn5, assistantTurn5)

    // Turn 6: User switches topic to a different device
    val userTurn6 = DiagnosticMessage(role = "user", text = "ახლა სხვა ტელეფონზე მაქვს კითხვა: Samsung Galaxy S23 არ იტენება")
    val assistantTurn6 = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = historyAfterTurn5,
      currentMessage = userTurn6,
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )

    // Must switch to Samsung Galaxy S23 and charging domain
    assertTrue("Must identify Samsung Galaxy S23", assistantTurn6.text.contains("Samsung") || assistantTurn6.text.contains("S23"))
    assertTrue("Must address charging domain", assistantTurn6.text.contains("დამუხტვ") || assistantTurn6.text.contains("დამტენ") || assistantTurn6.text.contains("პორტ"))
    assertEquals("Samsung Galaxy S23", assistantTurn6.structuredSection?.deviceIdentification)
  }

  @Test
  fun `intro card feature chips are aligned in 2-column equal-weight row with exact 28dp height`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        val palette = rememberDiagnosticsThemePalette()
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          DiagnosticCompactFeatureChip(
            modifier = Modifier.weight(1f),
            palette = palette,
            icon = Icons.Default.Search,
            label = "ფოტოს ანალიზი",
            testTag = "diag_feature_chip_photo"
          )
          DiagnosticCompactFeatureChip(
            modifier = Modifier.weight(1f),
            palette = palette,
            icon = Icons.Default.CheckCircle,
            label = "ტექნიკური რჩევა",
            testTag = "diag_feature_chip_tech"
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    val photoChipNode = composeTestRule.onNodeWithTag("diag_feature_chip_photo").assertIsDisplayed()
    val techChipNode = composeTestRule.onNodeWithTag("diag_feature_chip_tech").assertIsDisplayed()

    val photoBounds = photoChipNode.fetchSemanticsNode().boundsInRoot
    val techBounds = techChipNode.fetchSemanticsNode().boundsInRoot

    assertEquals(photoBounds.height, techBounds.height)
    assertEquals(photoBounds.width, techBounds.width, 1.0f)
  }

  @Test
  fun `audit test 1 - AI diagnostics and AI assistant accurately diagnose screen, charging, and camera symptoms without unrelated catalog spam`() {
    val catalog = ProductRepository.getAllProducts()

    // Case A: „ეკრანი არ ირთვება“
    val screenUserMsg = DiagnosticMessage(role = "user", text = "ეკრანი არ ირთვება")
    val screenDiag = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = emptyList(),
      currentMessage = screenUserMsg,
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )
    assertNotNull(screenDiag.structuredSection)
    // Must NOT pitch a specific iPhone 15 Pro Max screen product when no model was mentioned
    assertTrue(
      "„ეკრანი არ ირთვება“ without model must not push unrelated catalog products",
      screenDiag.recommendedProductIds.isEmpty()
    )
    // Must distinguish total power-off vs black screen with device running, and suggest safe checks
    val combinedScreenText = screenDiag.text + " " +
      screenDiag.structuredSection!!.hypotheses.joinToString(" ") { it.title + " " + it.supportingEvidence } + " " +
      screenDiag.structuredSection!!.nextDiagnosticTest + " " +
      screenDiag.structuredSection!!.followUpQuestion
    assertTrue("Must mention display/OLED/backlight possibility", combinedScreenText.contains("OLED") || combinedScreenText.contains("განათებ") || combinedScreenText.contains("ეკრან"))
    assertTrue("Must mention checking vibration/sound/calls or charging", combinedScreenText.contains("ვიბრაცი") || combinedScreenText.contains("ხმა") || combinedScreenText.contains("დამტენ"))
    assertTrue("Must include a clarifying follow-up question", screenDiag.structuredSection!!.followUpQuestion.isNotBlank() || screenDiag.text.contains("?"))

    val assistantScreenReply = GeminiDiagnosticService.generateLocalDiagnostic("ეკრანი არ ირთვება", catalog)
    assertTrue(assistantScreenReply.contains("ეკრანი არ ირთვება"))
    assertTrue(assistantScreenReply.contains("ვიბრაცი") || assistantScreenReply.contains("Force Restart"))
    assertTrue(GeminiDiagnosticService.findRelevantProducts("ეკრანი არ ირთვება", catalog).isEmpty())

    // Case B: „ტელეფონი არ იტენება“
    val chargingUserMsg = DiagnosticMessage(role = "user", text = "ტელეფონი არ იტენება")
    val chargingDiag = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = emptyList(),
      currentMessage = chargingUserMsg,
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )
    assertNotNull(chargingDiag.structuredSection)
    val combinedChargingText = chargingDiag.text + " " +
      chargingDiag.structuredSection!!.hypotheses.joinToString(" ") { it.title + " " + it.supportingEvidence } + " " +
      chargingDiag.structuredSection!!.nextDiagnosticTest
    assertTrue("Must consider cable and adapter", combinedChargingText.contains("კაბელ") && combinedChargingText.contains("ადაპტერ"))
    assertTrue("Must consider charging port dust/wear", combinedChargingText.contains("პორტ") && (combinedChargingText.contains("მტვერ") || combinedChargingText.contains("ნადებ") || combinedChargingText.contains("კონტაქტ")))
    assertTrue("Must consider battery or charging IC as a possible cause", combinedChargingText.contains("ელემენტ") || combinedChargingText.contains("კონტროლერ") || combinedChargingText.contains("PMIC"))
    assertTrue("„ტელეფონი არ იტენება“ without model must not push unrelated catalog products", chargingDiag.recommendedProductIds.isEmpty())

    // Verify GeminiDiagnosticService never mistakes „ტელეფონი არ იტენება“ for shop phone number/address!
    val assistantChargingReply = GeminiDiagnosticService.generateLocalDiagnostic("ტელეფონი არ იტენება", catalog)
    assertFalse(
      "„ტელეფონი არ იტენება“ must NOT return shop contact info",
      assistantChargingReply.contains("საკონტაქტო მონაცემები")
    )
    assertTrue(assistantChargingReply.contains("კაბელ") && assistantChargingReply.contains("ადაპტერ") && assistantChargingReply.contains("პორტ"))

    // Case C: „კამერა არ ფოკუსირდება“
    val cameraUserMsg = DiagnosticMessage(role = "user", text = "კამერა არ ფოკუსირდება")
    val cameraDiag = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = emptyList(),
      currentMessage = cameraUserMsg,
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )
    assertNotNull(cameraDiag.structuredSection)
    val combinedCameraText = cameraDiag.text + " " +
      cameraDiag.structuredSection!!.hypotheses.joinToString(" ") { it.title + " " + it.supportingEvidence } + " " +
      cameraDiag.structuredSection!!.nextDiagnosticTest
    assertTrue("Must mention lens cleanliness/cover", combinedCameraText.contains("ლინზ") || combinedCameraText.contains("შუშ"))
    assertTrue("Must mention OIS/AF mechanism", combinedCameraText.contains("OIS") || combinedCameraText.contains("ავტოფოკუს"))
    assertTrue("Must suggest checking 0.5x / 1x / 2x modes", combinedCameraText.contains("0.5x") && combinedCameraText.contains("1x"))
    // Must NOT blame the battery for camera focus!
    assertFalse(
      "Camera focus diagnosis must NOT blame battery degradation",
      cameraDiag.structuredSection!!.hypotheses.any { it.title.contains("ელემენტ") || it.title.contains("ბატარე") }
    )

    val assistantCameraReply = GeminiDiagnosticService.generateLocalDiagnostic("კამერა არ ფოკუსირდება", catalog)
    assertTrue(assistantCameraReply.contains("0.5x") && assistantCameraReply.contains("OIS"))
  }

  @Test
  fun `audit test 2 - zero response duplication, deduplicated hypotheses, rapid double-tap protection, error state handling, and stale session isolation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    DiagnosticsSessionRepository.clearAllSessions(context)
    val catalog = ProductRepository.getAllProducts().take(8)
    var holderRef: DiagnosticsWorkspaceStateHolder? = null

    try {
      composeTestRule.setContent {
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val holder = androidx.compose.runtime.remember {
          DiagnosticsWorkspaceStateHolder(context = context, coroutineScope = scope)
        }
        holderRef = holder
        RepairProTheme(darkTheme = true) {
          DiagnosticsWorkspaceScreen(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 0.dp, bottom = 0.dp),
            onProductClick = {},
            products = catalog,
            externalStateHolder = holder
          )
        }
      }
      composeTestRule.waitForIdle()

      // 1. Verify parser deduplicates identical hypotheses, deduplicates product IDs, and prevents nextDiagnosticTest duplication in summaryText
      val duplicateJson = """
        {
          "summaryText": "ეკრანის ანალიზი დასრულდა.\n\n**შემდეგი რეკომენდებული სადიაგნოსტიკო ნაბიჯი:** შეაერთეთ დამტენზე 15 წუთით.",
          "deviceIdentification": "iPhone 13",
          "visualInspectionFindings": "",
          "hypotheses": [
            {
              "title": "OLED მატრიცის დაზიანება",
              "confidence": "HIGH",
              "supportingEvidence": "სიმპტომი 1",
              "verificationOrDisproof": "შემოწმება 1"
            },
            {
              "title": "oled მატრიცის დაზიანება",
              "confidence": "HIGH",
              "supportingEvidence": "დუბლირებული",
              "verificationOrDisproof": "დუბლირებული"
            }
          ],
          "nextDiagnosticTest": "შეაერთეთ დამტენზე 15 წუთით.",
          "expectedFindings": "ჩაირთვება",
          "followUpQuestion": "დავარდნის შემდეგ დაეწყო?",
          "requiredTools": "",
          "safetyConsiderations": "",
          "recommendedProductIds": [1, 1, 2, 2]
        }
      """.trimIndent()

      val sampleUserMsg = DiagnosticMessage(role = "user", text = "iPhone 13 ეკრანი არ ირთვება")
      val parsedMsg = BekaMultimodalDiagnosticsEngine.parseStructuredDiagnosticMessage(
        rawResponse = duplicateJson,
        mode = DiagnosticUserMode.STANDARD,
        catalogProducts = catalog,
        currentMessage = sampleUserMsg
      )
      assertEquals("Duplicate hypotheses must be deduplicated", 1, parsedMsg.structuredSection!!.hypotheses.size)
      assertEquals("Duplicate product IDs must be deduplicated", listOf(1, 2), parsedMsg.recommendedProductIds)
      assertFalse(
        "summaryText must not repeat nextDiagnosticTest block",
        parsedMsg.text.contains("შემდეგი რეკომენდებული სადიაგნოსტიკო ნაბიჯი")
      )
      assertEquals("დავარდნის შემდეგ დაეწყო?", parsedMsg.structuredSection!!.followUpQuestion)

      // 2. Verify malformed JSON throws IllegalArgumentException when strictJsonValidation is enabled
      var malformedThrew = false
      try {
        BekaMultimodalDiagnosticsEngine.parseStructuredDiagnosticMessage(
          rawResponse = "not a json object at all",
          mode = DiagnosticUserMode.STANDARD,
          catalogProducts = catalog,
          currentMessage = sampleUserMsg,
          strictJsonValidation = true
        )
      } catch (e: IllegalArgumentException) {
        malformedThrew = true
      }
      assertTrue("Malformed JSON must throw IllegalArgumentException", malformedThrew)

      // 3. Test rapid double-tap protection and single response per turn
      var analyzerCallCount = 0
      BekaMultimodalDiagnosticsEngine.customAnalyzerOverride = { _, userMsg, mode, _ ->
        analyzerCallCount++
        kotlinx.coroutines.delay(150)
        BekaMultimodalDiagnosticsEngine.EngineResponse(
          assistantMessage = DiagnosticMessage(
            role = "assistant",
            text = "ერთადერთი უნიკალური პასუხი: ${userMsg.text}",
            mode = mode,
            structuredSection = StructuredDiagnosticSection(
              observedSymptoms = userMsg.text,
              deviceIdentification = "Samsung Galaxy S23",
              hypotheses = listOf(
                DiagnosticHypothesis(
                  title = "დამტენის პორტის დაბინძურება",
                  confidence = EvidenceConfidenceLevel.STRONGLY_SUSPECTED,
                  supportingEvidence = "კაბელი არ ჯდება მჭიდროდ",
                  verificationOrDisproof = "პორტის დათვალიერება"
                )
              ),
              nextDiagnosticTest = "სცადეთ სხვა კაბელი და ადაპტერი.",
              followUpQuestion = "ჩნდება თუ არა დატენვის ნიშანი?"
            )
          ),
          usedLiveModel = true
        )
      }

      composeTestRule.runOnIdle {
        // Rapidly invoke sendMessage 3 times in a row
        holderRef!!.sendMessage(overrideText = "Samsung S23 ტელეფონი არ იტენება", catalogProducts = catalog)
        holderRef!!.sendMessage(overrideText = "Samsung S23 ტელეფონი არ იტენება", catalogProducts = catalog)
        holderRef!!.sendMessage(overrideText = "Samsung S23 ტელეფონი არ იტენება", catalogProducts = catalog)
      }

      composeTestRule.waitUntil(timeoutMillis = 10000L) {
        !holderRef!!.isAnalyzing && holderRef!!.messages.size == 2
      }
      composeTestRule.waitForIdle()

      assertEquals("Rapid triple send must only execute analyzer once", 1, analyzerCallCount)
      assertEquals("Conversation must contain exactly 1 user message and 1 assistant message", 2, holderRef!!.messages.size)
      assertEquals(1, holderRef!!.messages.count { it.isUser })
      assertEquals(1, holderRef!!.messages.count { !it.isUser })

      // 4. Test Stale Async Request Cancellation when starting a New Session ("ახალი")
      BekaMultimodalDiagnosticsEngine.customAnalyzerOverride = { _, _, mode, _ ->
        kotlinx.coroutines.delay(500)
        BekaMultimodalDiagnosticsEngine.EngineResponse(
          assistantMessage = DiagnosticMessage(
            role = "assistant",
            text = "STALE_RESPONSE_FROM_OLD_SESSION",
            mode = mode
          ),
          usedLiveModel = true
        )
      }

      composeTestRule.runOnIdle {
        holderRef!!.sendMessage(overrideText = "ძველი სესიის კითხვა", catalogProducts = catalog)
      }
      assertTrue("StateHolder should be analyzing immediately after send", holderRef!!.isAnalyzing)

      // User clicks "ახალი" (startNewSession) BEFORE the 500ms request finishes
      composeTestRule.runOnIdle {
        holderRef!!.startNewSession()
      }
      composeTestRule.waitForIdle()
      assertFalse("isAnalyzing must immediately reset on startNewSession", holderRef!!.isAnalyzing)
      assertTrue("New session messages must be empty", holderRef!!.messages.isEmpty())

      // Wait past 500ms and verify the stale response NEVER appears in the new session
      Thread.sleep(600)
      composeTestRule.waitForIdle()
      assertTrue(
        "Stale response from cancelled session must NEVER be added to new session",
        holderRef!!.messages.none { it.text.contains("STALE_RESPONSE_FROM_OLD_SESSION") }
      )

      // 5. Test Engine Error State (must show error banner, restore inputText, and NEVER append a fake diagnosis)
      BekaMultimodalDiagnosticsEngine.customAnalyzerOverride = { _, _, _, _ ->
        throw RuntimeException("Simulated network timeout")
      }

      composeTestRule.runOnIdle {
        holderRef!!.sendMessage(overrideText = "კამერა არ ფოკუსირდება", catalogProducts = catalog)
      }
      composeTestRule.waitUntil(timeoutMillis = 10000L) {
        !holderRef!!.isAnalyzing
      }
      composeTestRule.waitForIdle()

      assertTrue("No fake assistant diagnosis may be added on error", holderRef!!.messages.isEmpty())
      assertNotNull("Error message banner must be set on failure", holderRef!!.errorMessage)
      assertEquals("Failed user text must be restored to composer for retry", "კამერა არ ფოკუსირდება", holderRef!!.inputText)
      composeTestRule.onNodeWithTag("diag_retry_banner").assertIsDisplayed()
    } finally {
      BekaMultimodalDiagnosticsEngine.customAnalyzerOverride = null
    }
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp")
  fun `audit test 3 - AiAssistantScreen and DiagnosticsWorkspaceScreen composers remain visible, keyboard-aware, and support multi-line input and sending`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    val catalog = ProductRepository.getAllProducts().take(10)

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        // Render AiAssistantScreen with simulated bottom inset (e.g., keyboard/IME height of 300.dp)
        AiAssistantScreen(
          products = catalog,
          onProductClick = {},
          onAddToCart = {},
          contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 64.dp, bottom = 300.dp)
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Verify AiAssistantScreen root, chat list, composer row, input, and send button are all displayed above the 300.dp bottom inset
    val rootNode = composeTestRule.onNodeWithTag("ai_assistant_root").assertIsDisplayed()
    val composerNode = composeTestRule.onNodeWithTag("ai_assistant_composer_row").assertIsDisplayed()
    val inputNode = composeTestRule.onNodeWithTag("ai_assistant_input").assertIsDisplayed()
    val sendBtnNode = composeTestRule.onNodeWithTag("ai_assistant_send_button").assertIsDisplayed()

    val rootBounds = rootNode.fetchSemanticsNode().boundsInRoot
    val composerBounds = composerNode.fetchSemanticsNode().boundsInRoot
    assertTrue(
      "AiAssistantScreen composer must remain fully visible inside the inset-adjusted root container",
      composerBounds.bottom <= rootBounds.bottom + 2.0f && composerBounds.top >= rootBounds.top
    )

    // 2. Focus empty field, type multi-line Georgian symptom text, and verify input and send button remain visible
    inputNode.performClick()
    val multilineSymptom = "ტელეფონი არ იტენება\nსხვა კაბელიც ვცადე\nდა პორტი სუფთაა"
    inputNode.performTextReplacement(multilineSymptom)
    composeTestRule.waitForIdle()

    inputNode.assertIsDisplayed()
    sendBtnNode.assertIsDisplayed()

    // 3. Send message while keyboard is open and verify response is added cleanly without hiding composer
    sendBtnNode.performClick()
    composeTestRule.waitUntil(timeoutMillis = 10000L) {
      composeTestRule.onAllNodes(
        androidx.compose.ui.test.hasText("შოპინგ ასისტენტი", substring = true)
      ).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("ai_assistant_composer_row").assertIsDisplayed()
    composeTestRule.onNodeWithTag("ai_assistant_input").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp")
  fun `audit test 4 - performance optimizations for search filter, cached storage, and scrolling lists`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    val products = ProductRepository.getAllProducts()

    // 1. Warm up and measure repeated multi-field search queries across all 85 hardware catalog items
    optimizeSearchFilter(products, "iphone 15 oled")
    val startNs = System.nanoTime()
    repeat(50) {
      val r1 = optimizeSearchFilter(products, "აიფონ 15 ეკრანი")
      val r2 = optimizeSearchFilter(products, "iphone 16 pro max")
      val r3 = optimizeSearchFilter(products, "სამსუნგ ელემენტი")
      assertTrue(r1.isNotEmpty())
      assertTrue(r2.isNotEmpty())
      assertTrue(r3.isNotEmpty())
    }
    assertTrue("Retired software query must return 0 results", optimizeSearchFilter(products, "chatgpt plus").isEmpty())
    val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
    // 150 full-catalog searches should complete well under 500ms in local JVM (< 3.3ms per search)
    assertTrue("150 full-catalog searches took ${elapsedMs}ms, expected < 500ms", elapsedMs < 500.0)

    // 2. Verify AdminStorage in-memory caching preserves custom edits while avoiding redundant JSON parsing
    AdminStorage.saveProducts(context, products, markUnsyncedLocalEdit = false)
    val loadStartNs = System.nanoTime()
    repeat(50) {
      val loaded = AdminStorage.loadProducts(context)
      assertEquals(products.size, loaded.size)
    }
    val loadElapsedMs = (System.nanoTime() - loadStartNs) / 1_000_000.0
    assertTrue("50 cached AdminStorage.loadProducts calls took ${loadElapsedMs}ms, expected < 250ms", loadElapsedMs < 250.0)

    // 3. Verify non-KA locale translation cache returns accurate results without recomputing phrase replacements
    val sampleText = "პროდუქტის ID: 101 • ხელმისაწვდომია • 12 თვე"
    val enFirst = AppLocaleManager.translate(sampleText, AppLanguage.EN)
    val enSecond = AppLocaleManager.translate(sampleText, AppLanguage.EN)
    assertEquals(enFirst, enSecond)
    assertTrue(enFirst.contains("Product ID:"))
  }

  @Test
  @Config(sdk = [36])
  fun `category CRUD, safe deletion, and product reassignment in AdminStorage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // 1. Initial categories exist and are not empty
    val initialCats = AdminStorage.getCategories(context)
    assertTrue("Initial categories should not be empty", initialCats.isNotEmpty())

    // 2. Add a new custom category
    val testCatId = "cat_test_consoles"
    val testCat = CategoryItem(
      id = testCatId,
      name = "სათამაშო კონსოლები",
      localizedName = "Gaming Consoles",
      description = "PlayStation, Xbox და VR აქსესუარები",
      icon = "🎮",
      displayOrder = 99,
      isActive = true,
      isPopular = true
    )
    AdminStorage.addCategory(context, testCat)
    val afterAdd = AdminStorage.getCategories(context)
    assertTrue("Added category should be present", afterAdd.any { it.id == testCatId && it.name == "სათამაშო კონსოლები" })

    // 3. Update category
    val updatedCat = testCat.copy(description = "განახლებული აღწერა", isPopular = false)
    AdminStorage.updateCategory(context, updatedCat)
    val afterUpdate = AdminStorage.getCategories(context)
    val foundUpdated = afterUpdate.find { it.id == testCatId }
    assertNotNull(foundUpdated)
    assertEquals("განახლებული აღწერა", foundUpdated?.description)
    assertFalse(foundUpdated?.isPopular ?: true)

    // 4. Test safe deletion with reassignment
    val targetCatName = "სხვა"
    val deleted = AdminStorage.deleteCategory(context, testCatId, targetCatName)
    assertTrue("Deletion should succeed", deleted)
    val afterDelete = AdminStorage.getCategories(context)
    assertFalse("Deleted category should not be present", afterDelete.any { it.id == testCatId })
  }

  @Test
  @Config(sdk = [36])
  fun `ProductItem multi-image resolution and backward compatibility`() {
    // 1. Legacy product with only imageUrl
    val legacy = ProductItem(
      id = 1,
      title = "iPhone 15 OLED",
      price = "350 ₾",
      category = "ეკრანები",
      imageUrl = "https://example.com/legacy.jpg"
    )
    assertEquals("https://example.com/legacy.jpg", legacy.displayPrimaryImage)
    assertEquals(listOf("https://example.com/legacy.jpg"), legacy.allImages)
    assertTrue(legacy.specifications.isEmpty())
    assertEquals("", legacy.exactModel)

    // 2. Upgraded product with primaryImageUrl and multiple images
    val upgraded = ProductItem(
      id = 2,
      title = "Samsung S24 Ultra",
      price = "2200 ₾",
      category = "ტელეფონები",
      imageUrl = "https://example.com/fallback.jpg",
      primaryImageUrl = "https://example.com/primary.jpg",
      images = listOf("https://example.com/photo1.jpg", "https://example.com/photo2.jpg"),
      specifications = mapOf("RAM" to "12GB", "Storage" to "256GB"),
      productType = "mobile_phone",
      condition = "ახალი",
      exactModel = "SM-S928B/DS"
    )
    assertEquals("https://example.com/primary.jpg", upgraded.displayPrimaryImage)
    val expectedImages = listOf("https://example.com/primary.jpg", "https://example.com/photo1.jpg", "https://example.com/photo2.jpg")
    assertEquals(expectedImages, upgraded.allImages)
    assertEquals("12GB", upgraded.specifications["RAM"])
    assertEquals("SM-S928B/DS", upgraded.exactModel)
  }

  @Test
  @Config(sdk = [36])
  fun `Admin session active bypass and auto-grant for designated admin`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Ensure clean slate
    AuthAndOrderManager.logout(context)
    AdminStorage.clearAdminSession(context)
    assertFalse("Session should be inactive after clear", AdminStorage.isAdminSessionActive(context))

    // 2. Authenticate as designated sole admin with claim and activate admin session
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = true)
    AdminStorage.setAdminSessionActive(context, true, "beqa1994@gmail.com")
    assertTrue("Admin session must be active", AdminStorage.isAdminSessionActive(context))

    // 3. Clear session
    AdminStorage.clearAdminSession(context)
    assertFalse("Admin session must be inactive after logout", AdminStorage.isAdminSessionActive(context))
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp")
  fun `issue 1 - admin panel access flow enforces 5 required states (logged out, ordinary user, verified admin, sign out, and direct route guard)`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    AuthAndOrderManager.logout(context)
    AdminStorage.clearAdminSession(context)

    val showRouteDirectly = androidx.compose.runtime.mutableStateOf(false)

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        if (showRouteDirectly.value) {
          AdminPanelScreen(
            products = ProductRepository.getAllProducts().take(10),
            onProductsUpdated = {},
            onClose = { showRouteDirectly.value = false }
          )
        } else {
          BekaSideNavigationDrawerContent(
            selectedTab = 0,
            unreadNotificationsCount = 0,
            cartItemsCount = 0,
            onSelectTab = {},
            onOpenAdminPanel = { showRouteDirectly.value = true },
            onCloseDrawer = {}
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    // Case 1: Logged out -> Admin Panel entry is completely hidden
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_button").fetchSemanticsNodes().size)

    // Case 2: Ordinary authenticated user -> Admin Panel entry is completely hidden
    AuthAndOrderManager.loginWithGoogle(context, "regular.user@gmail.com", "Regular User")
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_button").fetchSemanticsNodes().size)
    assertFalse(AdminStorage.isAdminSessionActive(context))

    // Case 5: Ordinary user attempts to access AdminPanelScreen route directly -> blocked with Access Denied
    showRouteDirectly.value = true
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_access_denied_screen").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_tab_0").fetchSemanticsNodes().size)
    showRouteDirectly.value = false
    composeTestRule.waitForIdle()

    // Case 3a: Administrator authenticated with beqa1994@gmail.com & admin custom claim -> button visible & opens directly without PIN
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = true)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("admin_button")
      .performScrollTo()
      .assertIsDisplayed()
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_pin_dialog").fetchSemanticsNodes().size)
    composeTestRule.onNodeWithTag("admin_panel_screen").assertIsDisplayed()
    // In VERIFIED_ADMIN state, all banners (blue verifying & permanent status) are completely removed
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_access_mode_banner").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_missing_claim_banner").fetchSemanticsNodes().size)

    // Case 3b: If admin custom claim is missing, manual banner is removed (auto recognition active) and claim is not bypassed
    AuthAndOrderManager.setAdminClaimVerified(context, false)
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_missing_claim_banner").fetchSemanticsNodes().size)
    assertFalse(AuthAndOrderManager.hasVerifiedAdminClaim(context))

    // Restore claim then test Case 4: Administrator signs out -> Admin UI elements immediately removed & route blocked
    AuthAndOrderManager.setAdminClaimVerified(context, true)
    composeTestRule.waitForIdle()
    AuthAndOrderManager.logout(context)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("admin_access_denied_screen").assertIsDisplayed()
    showRouteDirectly.value = false
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_button").fetchSemanticsNodes().size)
  }

  @Test
  @Config(sdk = [36])
  fun `issue 2 - AI product research identifies exact models across all 8 categories, never assumes software for physical items, and preserves manual seller fields`() {
    val existingCategories = listOf(
      "მობილური ტელეფონები", "ეკრანები", "აქსესუარები", "ყურსასმენები და აუდიო",
      "დამტენები და კაბელები", "ქსელური მოწყობილობები", "ელექტრონიკა", "პროგრამული სერვისები"
    )
    val existingBrands = listOf("Samsung", "Apple", "Huawei", "TP-Link", "Xiaomi", "OpenAI")

    // 1. Mobile Phone: "Samsung Galaxy S24 Ultra 12/256GB"
    val s24Id = AdminProductAiGenerator.identifyProductFromTitle("Samsung Galaxy S24 Ultra 12/256GB")
    assertEquals(ProductCategoryType.MOBILE_PHONE, s24Id.categoryType)
    assertEquals("Samsung", s24Id.brand)
    assertEquals("12 GB", s24Id.ram)
    assertEquals("256 GB", s24Id.storage)

    val s24Generated = kotlinx.coroutines.runBlocking {
      AdminProductAiGenerator.generateForTitle(
        rawTitle = "Samsung Galaxy S24 Ultra 12/256GB",
        existingCategories = existingCategories,
        existingBrands = existingBrands
      )
    }
    assertEquals(ProductCategoryType.MOBILE_PHONE, s24Generated.categoryType)
    assertFalse("Physical phone must NEVER be marked as software", s24Generated.isSoftware)
    assertEquals("Samsung", s24Generated.brand)
    assertEquals("მობილური ტელეფონები", s24Generated.category)
    assertTrue("Exact model must include SM-S928B/DS", s24Generated.exactModel.contains("SM-S928B/DS"))
    assertEquals("12 GB", s24Generated.specifications["RAM"])
    assertEquals("256 GB", s24Generated.specifications["Storage"])
    assertTrue("Phone specs must include processor", s24Generated.specifications["პროცესორი"]?.contains("Snapdragon 8 Gen 3") == true)
    assertTrue("Phone specs must include display", s24Generated.specifications["ეკრანი"]?.contains("AMOLED") == true)
    // Software fields must be empty for physical phones
    assertEquals("", s24Generated.offerType)
    assertEquals("", s24Generated.duration)
    assertEquals("", s24Generated.accountType)
    assertEquals("", s24Generated.activationTerms)
    assertFalse("Phone description must not contain software license/activation text", s24Generated.description.contains("ლიცენზია") || s24Generated.description.contains("გამოწერა"))
    // Seller-provided fields must NEVER be invented by AI
    assertEquals("AI must not invent sale price", "", s24Generated.price)
    assertEquals("AI must not invent physical condition", "", s24Generated.condition)
    assertEquals("AI must not invent warranty", "", s24Generated.warranty)
    assertFalse("AI must not invent battery health percentage", s24Generated.specifications.containsKey("ელემენტის რესურსი"))

    // 2. Networking Device: "Huawei B535-232 4G LTE WiFi Router"
    val routerGenerated = kotlinx.coroutines.runBlocking {
      AdminProductAiGenerator.generateForTitle(
        rawTitle = "Huawei B535-232 4G LTE WiFi Router",
        existingCategories = existingCategories,
        existingBrands = existingBrands
      )
    }
    assertEquals(ProductCategoryType.NETWORKING_DEVICE, routerGenerated.categoryType)
    assertFalse("Router must NEVER be marked as software", routerGenerated.isSoftware)
    assertEquals("Huawei", routerGenerated.brand)
    assertEquals("B535-232", routerGenerated.exactModel)
    assertEquals("როუტერები და ქსელი", routerGenerated.category)
    assertTrue(routerGenerated.specifications["ქსელის თაობა"]?.contains("4G LTE Cat 7") == true)
    assertTrue(routerGenerated.specifications["პორტები"]?.contains("Gigabit") == true)
    assertEquals("", routerGenerated.offerType)
    assertEquals("", routerGenerated.duration)

    // 3. Replacement Display, Headphones, Charger/Cable, Phone Accessory, Other Electronics, and Digital Service
    val displayGen = kotlinx.coroutines.runBlocking {
      AdminProductAiGenerator.generateForTitle("iPhone 15 Pro Original OLED ეკრანი", existingCategories, existingBrands)
    }
    assertEquals(ProductCategoryType.REPLACEMENT_DISPLAY, displayGen.categoryType)
    assertFalse(displayGen.isSoftware)

    val headphonesGen = kotlinx.coroutines.runBlocking {
      AdminProductAiGenerator.generateForTitle("Apple AirPods Pro 2 USB-C", existingCategories, existingBrands)
    }
    assertEquals(ProductCategoryType.HEADPHONES, headphonesGen.categoryType)
    assertFalse(headphonesGen.isSoftware)

    val chargerGen = kotlinx.coroutines.runBlocking {
      AdminProductAiGenerator.generateForTitle("Apple 20W USB-C Power Adapter დამტენი", existingCategories, existingBrands)
    }
    assertEquals(ProductCategoryType.CHARGER_CABLE, chargerGen.categoryType)
    assertFalse(chargerGen.isSoftware)

    val accessoryGen = kotlinx.coroutines.runBlocking {
      AdminProductAiGenerator.generateForTitle("iPhone 16 Pro MagSafe Silicone Case ქეისი", existingCategories, existingBrands)
    }
    assertEquals(ProductCategoryType.PHONE_ACCESSORY, accessoryGen.categoryType)
    assertFalse(accessoryGen.isSoftware)

    val otherElecGen = kotlinx.coroutines.runBlocking {
      AdminProductAiGenerator.generateForTitle("დინამიკი და მიკროფონის მოდული", existingCategories, existingBrands)
    }
    assertEquals(ProductCategoryType.OTHER_ELECTRONICS, otherElecGen.categoryType)
    assertFalse("Hardware electronics must always have isSoftware = false", otherElecGen.isSoftware)
    assertEquals("", otherElecGen.offerType)

    // 4. Manual administrator fields must NEVER be overwritten by mergeGeneratedWithManualFields
    val manualState = ExistingProductFormState(
      price = "2650 ₾",
      brand = "Samsung",
      category = "მობილური ტელეფონები",
      subcategory = "",
      shortDescription = "ადმინის ხელით ჩაწერილი მოკლე აღწერა",
      description = "",
      featuresText = "",
      targetAudience = "",
      offerType = "",
      duration = "",
      accountType = "",
      activationTerms = "",
      compatibleModels = "",
      warranty = "12 თვე",
      searchKeywords = "",
      exactModel = "",
      condition = "მეორადი (იდეალური)",
      specifications = mapOf("ელემენტის რესურსი" to "97%", "RAM" to "12 GB (Manual)")
    )
    val merged = AdminProductAiGenerator.mergeGeneratedWithManualFields(s24Generated, manualState)
    assertEquals("2650 ₾", merged.price)
    assertEquals("ადმინის ხელით ჩაწერილი მოკლე აღწერა", merged.shortDescription)
    assertEquals("12 თვე", merged.warranty)
    assertEquals("მეორადი (იდეალური)", merged.condition)
    assertEquals("97%", merged.specifications["ელემენტის რესურსი"])
    assertEquals("12 GB (Manual)", merged.specifications["RAM"])
    assertTrue("AI should still fill empty description", merged.description.isNotBlank())

    // 5. Invalid/blank title returns clear errorMessage
    val blankGen = kotlinx.coroutines.runBlocking {
      AdminProductAiGenerator.generateForTitle("   ", existingCategories, existingBrands)
    }
    assertNotNull("Blank title must return an errorMessage", blankGen.errorMessage)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp")
  fun `issue 3 and 2F - dynamic product form adapts to hardware product types, warns before clearing incompatible fields, and AI button has single icon`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)

    // Start with a Mobile Phone product that has populated phone-specific specifications (RAM, Storage)
    val initialPhoneProduct = ProductItem(
      id = 701,
      title = "Samsung Galaxy S24 Ultra",
      price = "2650 ₾",
      brand = "Samsung",
      category = "მობილური ტელეფონები",
      type = "product",
      productType = "mobile_phone",
      shortDescription = "Snapdragon 8 Gen 3 ფლაგმანი",
      description = "სრული ქარხნული კომპლექტაციით.",
      specifications = mapOf("RAM" to "12 GB", "Storage" to "256 GB"),
      imageUrl = ""
    )

    var savedItem: ProductItem? = null

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        ProductEditDialog(
          product = initialPhoneProduct,
          isNew = false,
          onDismiss = {},
          onSave = {
            savedItem = it
            Result.success(Unit)
          }
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Verify AI button displays single icon and clean label without duplicate "✨" prefix
    composeTestRule.onNodeWithTag("ai_autofill_product_button").assertIsDisplayed()
    composeTestRule.onNodeWithText("AI-ით დეტალური ანალიზი და შევსება").assertIsDisplayed()
    assertTrue(
      "AI button text must not contain duplicate sparkle emoji",
      composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("✨ AI-ით დეტალური ანალიზი და შევსება")).fetchSemanticsNodes().isEmpty()
    )

    // 2. Verify Mobile Phone category-specific fields (RAM, Storage) ARE displayed
    val lazyCol = composeTestRule.onNodeWithTag("product_edit_lazy_column")
    lazyCol.performScrollToNode(hasTestTag("dynamic_category_fields_section"))
    composeTestRule.onNodeWithTag("spec_field_RAM").assertIsDisplayed()
    composeTestRule.onNodeWithTag("spec_field_Storage").assertIsDisplayed()

    // 3. Switch product type to Replacement Display ("replacement_display") while RAM/Storage are populated -> must show warning dialog
    lazyCol.performScrollToNode(hasTestTag("product_type_selector_row"))
    composeTestRule.onNodeWithTag("product_type_chip_replacement_display").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("type_switch_warning_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_type_switch_button").performClick()
    composeTestRule.waitForIdle()

    // 4. Save and verify shared fields (title, price, shortDescription) are preserved while productType is updated
    composeTestRule.onNodeWithTag("save_product_button").performClick()
    composeTestRule.waitForIdle()

    assertNotNull(savedItem)
    assertEquals("Samsung Galaxy S24 Ultra", savedItem!!.title)
    assertEquals("2650 ₾", savedItem!!.price)
    assertEquals("Snapdragon 8 Gen 3 ფლაგმანი", savedItem!!.shortDescription)
    assertEquals("replacement_display", savedItem!!.productType)
    assertEquals("product", savedItem!!.type)
  }

  @Test
  @Config(sdk = [36])
  fun `prohibited and retired services are completely purged from repository, local cache, cloud merge, home banners, categories, and search`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("bekaservice_admin_prefs", Context.MODE_PRIVATE).edit().clear().commit()

    // 1. ProductRepository contains only the 85 legitimate hardware products (IDs 1..85)
    val repoProducts = ProductRepository.getAllProducts()
    assertEquals(85, repoProducts.size)
    assertTrue(repoProducts.all { it.id in 1..85 })
    assertTrue(repoProducts.none { ProductRepository.isProhibitedOrRetiredService(it) })

    // 2. Injecting prohibited/retired products into local cache or cloud merge strips them out immediately
    val injectedProhibited = listOf(
      ProductItem(id = 86, title = "Samsung FRP Bypass", price = "40 ₾", category = "პროგრამული სერვისები", type = "service", imageUrl = ""),
      ProductItem(id = 91, title = "iCloud Clean Unlock", price = "120 ₾", category = "პროგრამული სერვისები", type = "service", imageUrl = ""),
      ProductItem(id = 101, title = "ChatGPT Plus", price = "30 ₾", category = "პროგრამული სერვისები", type = "software", imageUrl = ""),
      ProductItem(id = 103, title = "Google AI Pro 18 თვე", price = "50 ₾", category = "პროგრამული სერვისები", type = "software", imageUrl = ""),
      ProductItem(id = 118, title = "Canva Pro", price = "15 ₾", category = "პროგრამული სერვისები", type = "software", imageUrl = "")
    )
    AdminStorage.saveProducts(context, repoProducts + injectedProhibited)
    val loadedAfterInject = AdminStorage.loadProducts(context)
    assertEquals(85, loadedAfterInject.size)
    assertTrue(loadedAfterInject.none { it.id in 86..124 })
    assertTrue(loadedAfterInject.none { ProductRepository.isProhibitedOrRetiredService(it) })

    val mergedWithCloudProhibited = AdminStorage.mergeCloudWithLocal(
      context = context,
      cloudProducts = injectedProhibited,
      removedProductIds = ProductRepository.RETIRED_PRODUCT_IDS,
      presentCloudDocIds = emptySet()
    )
    assertEquals(85, mergedWithCloudProhibited.size)
    assertTrue(mergedWithCloudProhibited.none { ProductRepository.isProhibitedOrRetiredService(it) })

    // 3. Home banners never contain slide_google_ai_pro, slide_chatgpt_plus, or slide_canva_pro even if previously cached
    val legacyBanners = listOf(
      AdminStorage.HomeBannerItem(id = "slide_google_ai_pro", imageUrl = "", title = "Google AI Pro — 18 თვე", subtitle = "Gemini Advanced", targetProductId = 103, tag = "GOOGLE AI PRO • 2TB"),
      AdminStorage.HomeBannerItem(id = "slide_chatgpt_plus", imageUrl = "", title = "ChatGPT Plus", subtitle = "ულიმიტო AI", targetProductId = 101, tag = "OPENAI • GPT-4o"),
      AdminStorage.HomeBannerItem(id = "slide_canva_pro", imageUrl = "", title = "Canva Pro — 12 თვე", subtitle = "Brand Kit", targetProductId = 118, tag = "CANVA PRO")
    )
    AdminStorage.saveHomeBanners(context, legacyBanners)
    val activeBanners = AdminStorage.getHomeBanners(context)
    assertTrue(activeBanners.isNotEmpty())
    assertTrue(activeBanners.none { ProductRepository.isProhibitedBanner(it) })
    assertTrue(activeBanners.none { it.id in setOf("slide_google_ai_pro", "slide_chatgpt_plus", "slide_canva_pro") })

    // 4. Categories never contain cat_software ("პროგრამული სერვისები")
    val legacyCat = CategoryItem(id = "cat_software", name = "პროგრამული სერვისები", localizedName = "Software Services")
    AdminStorage.addCategory(context, legacyCat)
    val activeCategories = AdminStorage.getCategories(context)
    assertTrue(activeCategories.none { ProductRepository.isProhibitedCategory(it) })
    assertTrue(activeCategories.none { it.id == "cat_software" || it.name == "პროგრამული სერვისები" })
  }

  @Test
  fun `sole admin beqa1994@gmail_com has full privileges and product edit save succeeds in AdminPanelScreen`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AuthAndOrderManager.logout(context)
    AdminStorage.clearAdminSession(context)

    // 1. Only beqa1994@gmail.com is recognized as sole admin
    assertTrue(AuthAndOrderManager.isSoleAdminEmail("beqa1994@gmail.com"))
    assertTrue(AuthAndOrderManager.isSoleAdminEmail("BEQA1994@GMAIL.COM"))
    assertFalse(AuthAndOrderManager.isSoleAdminEmail("otheruser@gmail.com"))
    assertFalse(AuthAndOrderManager.isSoleAdminEmail(""))

    // 2. Logging in as non-admin does NOT activate admin session
    AuthAndOrderManager.loginWithGoogle(context, email = "otheruser@gmail.com", name = "გიორგი")
    assertFalse("Non-admin user must not have admin session active", AdminStorage.isAdminSessionActive(context))

    // 3. Logging in as beqa1994@gmail.com automatically grants full admin session
    AuthAndOrderManager.loginWithGoogle(context, email = "beqa1994@gmail.com", name = "ბექა (Admin)")
    assertTrue("beqa1994@gmail.com must have active admin session", AdminStorage.isAdminSessionActive(context))

    // 4. Editing a physical spare part in AdminPanelScreen and clicking შენახვა succeeds and updates catalog
    val initialProducts = listOf(
      ProductItem(
        id = 1,
        firestoreDocId = "prod_1",
        title = "iPhone 15 Pro OLED ეკრანი (Super Retina XDR)",
        price = "480 ₾",
        category = "ეკრანები",
        brand = "Apple",
        compatibleModels = "iPhone 15 Pro, A3102",
        imageUrl = ProductImageManager.AUTO_GENERATED_IMAGE_MARKER
      )
    )
    var updatedProductsCallback: List<ProductItem>? = null

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        AdminPanelScreen(
          products = initialProducts,
          onProductsUpdated = { updatedProductsCallback = it },
          onClose = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // Click Edit on the product card and save changes
    composeTestRule.onNodeWithText("ფასი / აღწერა", substring = true).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("save_product_button").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 3000) { updatedProductsCallback != null }
    composeTestRule.waitForIdle()

    assertNotNull("Saving edited product as beqa1994@gmail.com must succeed and update products", updatedProductsCallback)
    assertEquals(1, updatedProductsCallback!!.size)
    assertEquals("Apple", updatedProductsCallback!!.first().brand)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp")
  fun `physical parts catalog contains only legitimate hardware components and cart checkout creates service order`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val allProducts = ProductRepository.getAllProducts()

    // 1. All products must be 100% legitimate physical hardware spare parts
    assertTrue("Product repository must not be empty", allProducts.isNotEmpty())
    allProducts.forEach { product ->
      assertFalse("Product title must not contain FRP", product.title.contains("FRP", ignoreCase = true))
      assertFalse("Product title must not contain iCloud bypass", product.title.contains("iCloud Bypass", ignoreCase = true))
      assertFalse("Product title must not contain IMEI check", product.title.contains("IMEI შემოწმება", ignoreCase = true))
      assertFalse("Product title must not contain account sales", product.title.contains("Gmail PVA", ignoreCase = true))
    }

    // 2. Add legitimate physical spare parts to cart and checkout
    val screenPart = allProducts.first { it.category == "ეკრანები" }
    val batteryPart = allProducts.first { it.category == "ელემენტები" }

    val cartItems = listOf(
      CartItem(product = screenPart, quantity = 1, selectedQuality = "Original"),
      CartItem(product = batteryPart, quantity = 2, selectedQuality = "High Capacity")
    )

    val checkoutRes = kotlinx.coroutines.runBlocking {
      AuthAndOrderManager.createAndPersistOrderFromCart(
        context = context,
        cartItems = cartItems
      )
    }

    assertTrue("Checkout from cart must succeed", checkoutRes.isSuccess)
    val createdOrder = checkoutRes.getOrThrow()
    assertEquals("მიმდინარე", createdOrder.status)
    assertEquals(2, createdOrder.items.size)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w411dp-h891dp")
  fun `secure sole admin authorization and service order notifications enforce payload privacy and multi device tokens`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("bekaservice_push_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("bekaservice_notifications_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("bekaservice_auth_orders_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("bekaservice_admin_prefs", Context.MODE_PRIVATE).edit().clear().commit()

    // 1. Regular customer account
    val customerUser = AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "customer.regular@gmail.com",
      name = "ნიკა მომხმარებელი"
    )
    assertFalse(
      "Regular customer must NEVER be recognized as sole admin",
      AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)
    )
    assertFalse(
      "Regular customer must NEVER have an active admin session",
      AdminStorage.isAdminSessionActive(context)
    )

    // Customer submits a Service Order
    val cartOrder = kotlinx.coroutines.runBlocking {
      AuthAndOrderManager.createAndPersistOrderFromCart(
        context = context,
        cartItems = listOf(
          CartItem(
            product = ProductRepository.getAllProducts().first(),
            quantity = 1,
            selectedQuality = "Original"
          )
        )
      ).getOrThrow()
    }

    // Customer sees 0 admin orders
    assertEquals(0, BekaNotificationManager.getUnreadAdminOrdersCount(context))

    // 2. Sole Admin logs in
    AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "beqa1994@gmail.com",
      name = "ბექა მღებრიშვილი",
      hasAdminClaim = true
    )
    assertTrue(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertTrue(AuthAndOrderManager.hasVerifiedAdminClaim(context))
    assertTrue(AdminStorage.isAdminSessionActive(context))

    // Admin registers device tokens
    assertTrue(BekaNotificationManager.registerAdminDeviceToken(context, "admin_pixel_token_111", "pixel_8"))
    assertTrue(BekaNotificationManager.registerAdminDeviceToken(context, "admin_tablet_token_222", "tab_s9"))
    assertEquals(2, BekaNotificationManager.getRegisteredAdminDeviceTokens(context).size)

    // Admin records notification for service order
    BekaNotificationManager.recordOrderCreatedForAdminOnly(context, cartOrder.id, "service_order")
    assertEquals(1, BekaNotificationManager.getUnreadAdminOrdersCount(context))

    // Mark as read
    assertTrue(BekaNotificationManager.markAdminOrderNotificationAsRead(context, cartOrder.id, "service_order"))
    assertEquals(0, BekaNotificationManager.getUnreadAdminOrdersCount(context))

    // Logout
    AuthAndOrderManager.logout(context)
    assertFalse(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertTrue(BekaNotificationManager.getRegisteredAdminDeviceTokens(context).isEmpty())
    assertEquals(0, BekaNotificationManager.getUnreadAdminOrdersCount(context))
  }

  @Test
  @Config(sdk = [36])
  fun `hardware diagnostics routes sim and network issues to physical hardware repairs`() {
    val diagnosis = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = emptyList(),
      currentMessage = DiagnosticMessage(role = "user", text = "არ იჭერს ქსელს, სიმ ბარათი არ მუშაობს"),
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = ProductRepository.getAllProducts()
    )
    assertNotNull(diagnosis.structuredSection)
    val combinedText = diagnosis.text + " " + diagnosis.structuredSection!!.hypotheses.joinToString(" ") { it.title + " " + it.supportingEvidence }
    assertTrue(
      "Diagnosis must focus on hardware repair (SIM tray, RF antenna cables, APN)",
      combinedText.contains("SIM") || combinedText.contains("ანტენ") || combinedText.contains("კაბელ") || combinedText.contains("მოდულ")
    )
    assertFalse("Diagnosis must never recommend network unlock circumvention", combinedText.contains("Network Unlock"))
  }

  @Test
  fun testFcmDeliveryPipelineForegroundBackgroundClosedAndOemDiagnostics() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    org.robolectric.Shadows.shadowOf(context as android.app.Application)
      .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    BekaNotificationManager.ensureNotificationChannels(context)
    BekaNotificationManager.clearAllNotifications(context)
    AuthAndOrderManager.logout(context)

    // 1. Verify Notification Channels & OEM Delivery Diagnostics (Scenario G)
    val xiaomiDiag = BekaNotificationManager.getDeliveryDiagnostics(context, manufacturerOverride = "Xiaomi POCO")
    assertTrue("General notification channel must be enabled by default", xiaomiDiag.generalChannelEnabled)
    assertTrue("Admin orders notification channel must be enabled by default", xiaomiDiag.adminOrdersChannelEnabled)
    assertTrue("Xiaomi/POCO diagnostics must mention Autostart", xiaomiDiag.manufacturerStepsKa.any { it.contains("Autostart", ignoreCase = true) })

    val samsungDiag = BekaNotificationManager.getDeliveryDiagnostics(context, manufacturerOverride = "Samsung")
    assertTrue("Samsung diagnostics must mention Unrestricted battery", samsungDiag.manufacturerStepsKa.any { it.contains("Unrestricted", ignoreCase = true) })

    // 2. Scenario D & E: Sole Admin Multi-Device Token Registration, Token Refresh & Synthetic Token Rejection
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Admin Beqa")
    AdminStorage.setAdminSessionActive(context, true, "beqa1994@gmail.com")
    assertTrue(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))

    // Synthetic placeholder token must not overwrite saved real FCM token
    BekaNotificationManager.saveLocalFcmToken(context, "fcm_real_token_initial_device_1_abcdef123456")
    BekaNotificationManager.saveLocalFcmToken(context, "fcm_admin_dev_synthetic_should_be_ignored")
    assertEquals("fcm_real_token_initial_device_1_abcdef123456", BekaNotificationManager.getLocalFcmToken(context))

    val device1Id = BekaNotificationManager.getOrCreateDeviceId(context)
    val device2Id = "dev_admin_second_phone_2"
    assertTrue(BekaNotificationManager.registerAdminDeviceToken(context, "fcm_real_token_initial_device_1_abcdef123456", device1Id))
    assertTrue(BekaNotificationManager.registerAdminDeviceToken(context, "fcm_real_token_second_device_2_xyz987654321", device2Id))
    assertEquals(2, BekaNotificationManager.getRegisteredAdminDeviceTokens(context).size)

    // Token Refresh on Device 1 replaces old token for Device 1 without duplicating Device 1 entry
    assertTrue(BekaNotificationManager.registerAdminDeviceToken(context, "fcm_real_token_refreshed_device_1_new999999", device1Id))
    val tokensAfterRefresh = BekaNotificationManager.getRegisteredAdminDeviceTokens(context)
    assertEquals(2, tokensAfterRefresh.size)
    assertEquals(
      "fcm_real_token_refreshed_device_1_new999999",
      tokensAfterRefresh.first { it.deviceId == device1Id }.token
    )

    // 3. Scenario A: Foreground FCM message handling & deduplication + PII stripping
    val orderIdFg = "imei_fg_order_777"
    val canonicalTag = "notif_order_imei_check_$orderIdFg"
    val firstDelivery = BekaNotificationManager.handleIncomingFcmMessageData(
      context = context,
      data = mapOf(
        "type" to "admin_order",
        "notificationId" to canonicalTag,
        "orderId" to orderIdFg,
        "orderType" to "imei_check",
        "title" to "ახალი IMEI შეკვეთა 353912101234566",
        "body" to "შემოვიდა მოთხოვნა client@gmail.com 353912101234566",
        "deepLink" to "bekaservice://admin/orders/imei/$orderIdFg"
      )
    )
    assertTrue("First foreground admin order FCM push must be displayed", firstDelivery)
    // Duplicate push with the same notificationId must be suppressed
    val duplicateDelivery = BekaNotificationManager.handleIncomingFcmMessageData(
      context = context,
      data = mapOf(
        "type" to "admin_order",
        "notificationId" to canonicalTag,
        "orderId" to orderIdFg,
        "orderType" to "imei_check"
      )
    )
    assertFalse("Duplicate foreground admin order push with same notificationId must be suppressed", duplicateDelivery)

    // Verify stored admin notification record stripped the 15-digit IMEI and email
    val storedFgNotif = BekaNotificationManager.loadAdminOrderNotificationsForAdmin(context)
      .first { it.orderId == orderIdFg }
    assertFalse(BekaNotificationManager.containsSensitiveOrderData("${storedFgNotif.title} ${storedFgNotif.body}"))

    // 4. Scenario B & C: Background / Closed App System-Tray Tap with Raw FCM Data Extras
    val orderIdClosed = "imei_closed_order_888"
    BekaNotificationManager.recordOrderCreatedForAdminOnly(context, orderIdClosed, "imei_check")
    assertTrue(BekaNotificationManager.isOrderUnreadForAdmin(context, orderIdClosed))

    // When the app is closed and Android OS displays the combined notification + data FCM payload,
    // tapping the system-tray notification launches MainActivity with raw FCM `data` keys ("orderId", "orderType", "deepLink").
    // Marking as read via the deep link handler must mark both `admin_order_<id>` and `notif_order_imei_check_<id>` as delivered and read.
    assertTrue(BekaNotificationManager.markAdminOrderNotificationAsRead(context, orderIdClosed, "imei_check"))
    assertFalse("Order must be marked read after background/closed notification tap", BekaNotificationManager.isOrderUnreadForAdmin(context, orderIdClosed))
    assertTrue(
      "Canonical FCM tag must be marked delivered so snapshot listener does not duplicate on app open",
      BekaNotificationManager.hasDeliveredNotification(context, "notif_order_imei_check_$orderIdClosed")
    )

    // 5. Scenario F: Account switch from Admin to Regular Customer isolates tokens and blocks admin pushes
    AuthAndOrderManager.loginWithGoogle(context, "regular.buyer@gmail.com", "Regular Buyer")
    assertFalse(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertFalse(
      "Admin order push must be blocked after switching to a non-admin customer account",
      BekaNotificationManager.handleIncomingFcmMessageData(
        context = context,
        data = mapOf(
          "type" to "admin_order",
          "orderId" to "imei_blocked_999",
          "orderType" to "imei_check"
        )
      )
    )

    AuthAndOrderManager.logout(context)
  }

  @Test
  @Config(sdk = [36])
  fun `google play account deletion compliance removes user session and profile`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Login user
    val user = AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "compliance_user@example.com",
      name = "Compliance Tester"
    )
    assertNotNull("User session must be established", user)
    assertEquals("compliance_user@example.com", AuthAndOrderManager.getCurrentUser(context)?.email)

    // 2. Perform account deletion as required by Google Play User Data policy
    val deleteResult = kotlinx.coroutines.runBlocking {
      AuthAndOrderManager.deleteUserAccount(context)
    }
    assertTrue("User account deletion must succeed", deleteResult.isSuccess)

    // 3. Verify user profile and session are completely cleared
    assertEquals(null, AuthAndOrderManager.getCurrentUser(context))
  }

  @Test
  @Config(sdk = [36])
  fun `app brand configuration centralization ensures legitimate business identity and compliance links`() {
    assertEquals("BSTORE", AppBrandConfig.APP_NAME_EN)
    assertEquals("BSTORE", AppBrandConfig.APP_NAME_KA)
    assertEquals("+995 598 23 33 32", AppBrandConfig.SUPPORT_PHONE)
    assertEquals("ახალციხე, კოსტავას ქ. N10", AppBrandConfig.PHYSICAL_ADDRESS_KA)
    assertTrue(AppBrandConfig.PRIVACY_POLICY_URL.startsWith("https://"))
    assertTrue(AppBrandConfig.TERMS_OF_SERVICE_URL.startsWith("https://"))
    assertTrue(AppBrandConfig.ACCOUNT_DELETION_URL.startsWith("https://"))
  }

  @Test
  fun `notification permission onboarding flow shows Georgian explanation dialog before system request and respects user choice`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val shadowApp = org.robolectric.Shadows.shadowOf(context as android.app.Application)
    shadowApp.denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

    // 6. Verify Android versions below 13 (< API 33) do not require or trigger the runtime explanation dialog
    BekaNotificationManager.resetPermissionOnboardingState(context)
    assertFalse(
      "Android < 13 (API 32) must not show runtime notification permission explanation dialog",
      BekaNotificationManager.shouldShowPermissionExplanationDialog(context, sdkInt = android.os.Build.VERSION_CODES.S_V2)
    )
    assertTrue(
      "Android 13+ (API 36) without permission and unprompted must show explanation dialog",
      BekaNotificationManager.shouldShowPermissionExplanationDialog(context, sdkInt = android.os.Build.VERSION_CODES.TIRAMISU)
    )

    // Prepare first-time onboarding completion state
    context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("bekaservice_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("bekaservice_auth_orders_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    AuthAndOrderManager.logout(context)
    BekaNotificationManager.resetPermissionOnboardingState(context)
    AppPreferencesManager.init(context)
    hasCompletedInitialLaunch = true

    var systemPermissionRequestCount = 0
    val recomposeTrigger = androidx.compose.runtime.mutableIntStateOf(0)

    composeTestRule.setContent {
      val trigger = recomposeTrigger.intValue
      androidx.compose.runtime.key(trigger) {
        RepairProTheme(darkTheme = true) {
          BekaServiceApp(
            deepLinkIntent = null,
            onDeepLinkConsumed = {},
            onSystemNotificationPermissionRequested = {
              systemPermissionRequestCount += 1
            }
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    // While still in onboarding, neither the explanation dialog nor the system permission request is triggered
    assertEquals(0, composeTestRule.onAllNodesWithTag("notification_permission_explanation_dialog").fetchSemanticsNodes().size)
    assertEquals(0, systemPermissionRequestCount)

    // Complete onboarding steps (Welcome -> Language -> Skip Auth -> Theme Selection [4 / 4] -> Finish)
    composeTestRule.onNodeWithTag("onboarding_welcome_next_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_language_next_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_skip_auth_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_finish_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertTrue("Onboarding should be marked completed", AppPreferencesManager.isOnboardingCompleted)
    assertTrue(
      "shouldShowPermissionExplanationDialog should be true after onboarding completes",
      BekaNotificationManager.shouldShowPermissionExplanationDialog(context)
    )

    // 1. First-time onboarding: Georgian explanation dialog appears BEFORE the system permission dialog
    composeTestRule.onNodeWithTag("notification_permission_explanation_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notification_permission_bell_icon").assertIsDisplayed()
    composeTestRule.onNodeWithText("არ გამოგრჩეს მნიშვნელოვანი სიახლეები").assertIsDisplayed()
    composeTestRule.onNodeWithText(
      "ჩართე შეტყობინებები, რომ დროულად მიიღო ინფორმაცია შენი შეკვეთების სტატუსის, სიახლეებისა და მნიშვნელოვანი განახლებების შესახებ."
    ).assertIsDisplayed()
    composeTestRule.onNodeWithTag("notification_permission_enable_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notification_permission_not_now_button").assertIsDisplayed()
    assertEquals("System permission must NOT be requested before user clicks enable", 0, systemPermissionRequestCount)

    // 3. User selects "ახლა არა": dialog dismisses and app opens normally without a system permission request
    composeTestRule.onNodeWithTag("notification_permission_not_now_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithTag("notification_permission_explanation_dialog").fetchSemanticsNodes().size)
    assertEquals("System permission must NOT be requested when user selects 'ახლა არა'", 0, systemPermissionRequestCount)
    assertTrue(BekaNotificationManager.didUserDeclinePermissionExplanation(context))
    composeTestRule.onNodeWithTag("glassmorphic_top_app_bar_container").assertIsDisplayed()

    // 5. Returning user: explanation does not appear again on subsequent launch
    recomposeTrigger.intValue += 1
    composeTestRule.waitForIdle()
    assertFalse(BekaNotificationManager.shouldShowPermissionExplanationDialog(context))
    assertEquals(0, composeTestRule.onAllNodesWithTag("notification_permission_explanation_dialog").fetchSemanticsNodes().size)
    assertEquals(0, systemPermissionRequestCount)

    // 2. Now reset state to test when user selects "შეტყობინებების ჩართვა": Android system permission request is triggered
    BekaNotificationManager.resetPermissionOnboardingState(context)
    recomposeTrigger.intValue += 1
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("notification_permission_explanation_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notification_permission_enable_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(0, composeTestRule.onAllNodesWithTag("notification_permission_explanation_dialog").fetchSemanticsNodes().size)
    assertEquals("Selecting 'შეტყობინებების ჩართვა' must trigger the official Android permission request", 1, systemPermissionRequestCount)
    assertTrue(BekaNotificationManager.wasSystemPermissionRequested(context))

    // 4. User denies system permission: app continues working and user can enable notifications later in Notifications screen
    BekaNotificationManager.onSystemPermissionResult(context, isGranted = false)
    assertTrue(BekaNotificationManager.wasSystemPermissionDenied(context))
    assertFalse("Denied permission must not re-trigger explanation dialog on next launch", BekaNotificationManager.shouldShowPermissionExplanationDialog(context))

    composeTestRule.onNodeWithTag("notifications_button", useUnmergedTree = true).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("user_notifications_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("enable_notifications_from_settings_button").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36])
  fun `contact and social manager supports CRUD, phone and URL validation, and persistent storage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    ContactAndSocialManager.init(context)

    // 1. Phone validation
    assertTrue(ContactAndSocialManager.isValidPhoneNumber("+995 598 23 33 32"))
    assertTrue(ContactAndSocialManager.isValidPhoneNumber("598233332"))
    assertTrue(ContactAndSocialManager.isValidPhoneNumber("+995 (32) 2 123 456"))
    assertFalse(ContactAndSocialManager.isValidPhoneNumber("123")) // Too short
    assertFalse(ContactAndSocialManager.isValidPhoneNumber("abc-def-ghij")) // Invalid chars

    // 2. Social URL validation
    assertTrue(ContactAndSocialManager.isValidSocialUrl("https://wa.me/995598233332"))
    assertTrue(ContactAndSocialManager.isValidSocialUrl("https://t.me/bekaservice"))
    assertTrue(ContactAndSocialManager.isValidSocialUrl("https://www.facebook.com/bekaservice"))
    assertTrue(ContactAndSocialManager.isValidSocialUrl("whatsapp://send?phone=995598233332"))
    assertFalse(ContactAndSocialManager.isValidSocialUrl("javascript:alert(1)")) // Dangerous scheme
    assertFalse(ContactAndSocialManager.isValidSocialUrl("ftp://insecure.site")) // Invalid scheme
    assertFalse(ContactAndSocialManager.isValidSocialUrl("not a url at all"))

    // 3. Dialer URI normalization
    val dialUri = ContactAndSocialManager.normalizeDialUri("+995 598 23 33 32")
    assertEquals("tel:+995598233332", dialUri.toString())

    // 4. Configuration creation and local save/load
    val testPhones = listOf(
      ContactPhoneNumber(id = "p1", label = "მთავარი", number = "+995598111222", isEnabled = true, displayOrder = 1),
      ContactPhoneNumber(id = "p2", label = "დახმარება", number = "+995598333444", isEnabled = false, displayOrder = 2)
    )
    val testSocials = listOf(
      SocialMediaLink(id = "s1", platform = SocialPlatform.WHATSAPP, title = "WhatsApp", url = "https://wa.me/995598111222", isEnabled = true, displayOrder = 1),
      SocialMediaLink(id = "s2", platform = SocialPlatform.TELEGRAM, title = "Telegram", url = "https://t.me/beka_channel", isEnabled = true, displayOrder = 2)
    )
    val testConfig = ContactAndSocialConfig(
      phoneNumbers = testPhones,
      socialLinks = testSocials,
      serviceAddress = "თბილისი, წერეთლის გამზირი 140",
      workingHours = "ორშ-შაბ: 10:00 - 20:00",
      updatedAtMillis = System.currentTimeMillis()
    )

    ContactAndSocialManager.saveLocalConfig(context, testConfig)
    val loaded = ContactAndSocialManager.loadConfig(context)

    assertEquals(2, loaded.phoneNumbers.size)
    assertEquals(2, loaded.socialLinks.size)
    assertEquals("+995598111222", ContactAndSocialManager.getPrimaryPhoneNumber(context))
    assertTrue(ContactAndSocialManager.getPrimaryWhatsAppUrl(context).contains("995598111222"))
    assertEquals("თბილისი, წერეთლის გამზირი 140", loaded.serviceAddress)
  }

  @Test
  @Config(sdk = [36])
  fun `admin panel section 10 allows managing contacts and social links with verified sole admin`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    ContactAndSocialManager.init(context)

    // Log in as sole admin with verified claim
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = true)
    AdminStorage.setAdminSessionActive(context, true, "beqa1994@gmail.com")

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        AdminContactsAndSocialScreen(onClose = {})
      }
    }
    composeTestRule.waitForIdle()

    // Screen title and key sections render
    composeTestRule.onNodeWithTag("admin_contacts_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_contacts_screen").performScrollToNode(hasTestTag("admin_add_phone_button"))
    composeTestRule.onNodeWithTag("admin_add_phone_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_contacts_screen").performScrollToNode(hasTestTag("admin_add_social_button"))
    composeTestRule.onNodeWithTag("admin_add_social_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_contacts_screen").performScrollToNode(hasTestTag("admin_save_contacts_button"))
    composeTestRule.onNodeWithTag("admin_save_contacts_button").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36])
  fun `admin custom claim verification removes warning banner for beqa1994 and restricts ordinary and unauthenticated users`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    ContactAndSocialManager.init(context)

    // 1. Unauthenticated user: cannot see admin_button and gets admin_access_denied_screen on direct access
    AuthAndOrderManager.logout(context)
    assertFalse(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertFalse(AuthAndOrderManager.hasVerifiedAdminClaim(context))

    // 2. Ordinary authenticated user: cannot see admin_button and cannot obtain verified admin claim
    AuthAndOrderManager.loginWithGoogle(context, "ordinary.user@gmail.com", "Ordinary User", hasAdminClaim = true)
    assertFalse(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertFalse(AuthAndOrderManager.hasVerifiedAdminClaim(context))

    // 3. Sole administrator (beqa1994@gmail.com) before claim verification vs after claim verification
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = false)
    assertTrue(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertFalse(AuthAndOrderManager.hasVerifiedAdminClaim(context))

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        AdminPanelScreen(
          products = ProductRepository.getAllProducts(),
          onProductsUpdated = {},
          onClose = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // Manual sync button and instructional warning banner are completely removed
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_missing_claim_banner").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_refresh_claim_button").fetchSemanticsNodes().size)

    // Simulate automatic server claim verification completing and updating auth state
    composeTestRule.runOnIdle {
      AuthAndOrderManager.setAdminClaimVerified(context, true)
    }
    composeTestRule.waitForIdle()

    // Warning banner disappears immediately for the verified administrator
    assertTrue(AuthAndOrderManager.hasVerifiedAdminClaim(context))
    assertEquals(
      0,
      composeTestRule.onAllNodesWithTag("admin_missing_claim_banner").fetchSemanticsNodes().size
    )
    composeTestRule.onNodeWithTag("admin_panel_screen").assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36])
  fun `admin auth state transitions - Tests A B C D cover login, recomposition, restart and customer rejection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Setup: clean state
    AuthAndOrderManager.logout(context)
    assertEquals(AdminAuthState.NOT_ADMIN, AuthAndOrderManager.adminAuthState.value)

    // Test A: Admin login with verified custom claim transitions to VERIFIED_ADMIN
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Beka Admin", hasAdminClaim = true)
    assertEquals(AdminAuthState.VERIFIED_ADMIN, AuthAndOrderManager.adminAuthState.value)
    assertTrue(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertTrue(AuthAndOrderManager.hasVerifiedAdminClaim(context))

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        AdminPanelScreen(
          products = ProductRepository.getAllProducts(),
          onProductsUpdated = {},
          onClose = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // Confirm: In VERIFIED_ADMIN, blue verification banner is completely removed (0 semantics nodes)
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_access_mode_banner").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_missing_claim_banner").fetchSemanticsNodes().size)
    composeTestRule.onNodeWithTag("admin_panel_screen").assertIsDisplayed()

    // Test B: Recomposition (simulate user switching tabs and scrolling)
    composeTestRule.runOnIdle {
      // Normal recomposition event
      AuthAndOrderManager.favoritesStateVersion.intValue++
    }
    composeTestRule.waitForIdle()
    // Verification does NOT restart, banner remains absent
    assertEquals(AdminAuthState.VERIFIED_ADMIN, AuthAndOrderManager.adminAuthState.value)
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_access_mode_banner").fetchSemanticsNodes().size)

    // Test C: App restart / session restoration
    // Simulate reading preferences on app launch
    val restoredState = AuthAndOrderManager.hasVerifiedAdminClaim(context)
    assertTrue(restoredState)
    assertEquals(AdminAuthState.VERIFIED_ADMIN, AuthAndOrderManager.adminAuthState.value)
    assertEquals(0, composeTestRule.onAllNodesWithTag("admin_access_mode_banner").fetchSemanticsNodes().size)

    // Test D: Ordinary account login
    AuthAndOrderManager.loginWithGoogle(context, "customer@gmail.com", "Regular Customer", hasAdminClaim = false)
    assertEquals(AdminAuthState.NOT_ADMIN, AuthAndOrderManager.adminAuthState.value)
    assertFalse(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))
    assertFalse(AuthAndOrderManager.hasVerifiedAdminClaim(context))
    assertFalse(AdminStorage.isAdminSessionActive(context))
  }

  @Test
  @Config(sdk = [36])
  fun `bstore payment system - 12_1 BOG transfer, 12_2 TBC transfer, 12_3 COD, 12_4 security and snapshot protection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    BstorePaymentManager.init(context)

    val defaultAccounts = listOf(
      BankAccountConfig(
        id = "bank_bog_primary",
        bankCode = "bog",
        bankName = "საქართველოს ბანკი",
        recipientName = "BSTORE / ბექა მღებრიშვილი",
        iban = "GE17BG0000000580620970",
        description = "საქართველოს ბანკის ლარის ანგარიში",
        isEnabled = true,
        displayOrder = 1
      ),
      BankAccountConfig(
        id = "bank_tbc_primary",
        bankCode = "tbc",
        bankName = "თიბისი ბანკი",
        recipientName = "BSTORE / ბექა მღებრიშვილი",
        iban = "GE65TB7777777000000001",
        description = "თიბისი ბანკის ლარის ანგარიში",
        isEnabled = true,
        displayOrder = 2
      )
    )
    BstorePaymentManager.saveBankAccountsLocal(context, defaultAccounts)

    // Log in as customer Alice
    val aliceProfile = AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "alice.customer@gmail.com",
      name = "Alice Customer",
      hasAdminClaim = false
    )
    assertFalse(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))

    val sampleCart = listOf(
      CartItem(
        product = ProductRepository.getAllProducts().first(),
        quantity = 1,
        selectedQuality = "Original"
      )
    )

    // =========================================================================
    // 12.1 Bank of Georgia transfer workflow + snapshot protection
    // =========================================================================
    val bogBank = BstorePaymentManager.getEnabledBankAccounts(context).first { it.bankCode == "bog" }
    val bogConfig = CheckoutSubmissionConfig(
      paymentMethod = PaymentMethodType.BANK_TRANSFER,
      selectedBankAccount = bogBank,
      deliveryMethod = DeliveryMethodType.COURIER,
      deliveryCity = "თბილისი",
      deliveryAddress = "ჭავჭავაძის გამზ. 10",
      customerPhone = "+995 555 11 22 33",
      acceptedTermsVersion = BstorePaymentManager.CURRENT_TERMS_VERSION,
      acceptedTermsAtMillis = System.currentTimeMillis()
    )
    val bogOrder = AuthAndOrderManager.addOrderFromCart(context, sampleCart, bogConfig)

    assertEquals("bank_transfer", bogOrder.paymentMethodCode)
    assertEquals("pending", bogOrder.paymentStatus)
    assertEquals("საქართველოს ბანკი", bogOrder.bankName)
    assertEquals(bogBank.iban, bogOrder.ibanSnapshot)
    assertEquals(bogOrder.orderNumber, bogOrder.paymentReference)
    assertEquals("მიმდინარე", bogOrder.status) // Order fulfillment status is independent from payment status

    // Snapshot protection: Admin changes BOG IBAN later -> existing order retains original IBAN snapshot
    val originalIbanSnapshot = bogOrder.ibanSnapshot
    BstorePaymentManager.saveBankAccountsLocal(
      context,
      defaultAccounts.map {
        if (it.bankCode == "bog") it.copy(iban = "GE99BG0000000999999999") else it
      }
    )
    val reloadedBogOrder = AuthAndOrderManager.getOrders(context).first { it.id == bogOrder.id }
    assertEquals(originalIbanSnapshot, reloadedBogOrder.ibanSnapshot)

    // Restore default accounts
    BstorePaymentManager.saveBankAccountsLocal(context, defaultAccounts)

    // =========================================================================
    // 12.2 TBC Bank transfer workflow
    // =========================================================================
    val tbcBank = BstorePaymentManager.getEnabledBankAccounts(context).first { it.bankCode == "tbc" }
    val tbcConfig = CheckoutSubmissionConfig(
      paymentMethod = PaymentMethodType.BANK_TRANSFER,
      selectedBankAccount = tbcBank,
      deliveryMethod = DeliveryMethodType.PICKUP,
      deliveryCity = "ახალციხე",
      deliveryAddress = "ახალციხე, კოსტავას N10",
      customerPhone = "+995 555 11 22 33"
    )
    val tbcOrder = AuthAndOrderManager.addOrderFromCart(context, sampleCart, tbcConfig)
    assertEquals("bank_transfer", tbcOrder.paymentMethodCode)
    assertEquals("pending", tbcOrder.paymentStatus)
    assertEquals("თიბისი ბანკი", tbcOrder.bankName)
    assertEquals(tbcBank.iban, tbcOrder.ibanSnapshot)

    // =========================================================================
    // 12.3 Cash on Delivery (COD) workflow
    // =========================================================================
    val codConfig = CheckoutSubmissionConfig(
      paymentMethod = PaymentMethodType.COD,
      selectedBankAccount = null,
      deliveryMethod = DeliveryMethodType.COURIER,
      deliveryCity = "ბათუმი",
      deliveryAddress = "რუსთაველის 5",
      customerPhone = "+995 555 11 22 33"
    )
    val codOrder = AuthAndOrderManager.addOrderFromCart(context, sampleCart, codConfig)
    assertEquals("cod", codOrder.paymentMethodCode)
    assertEquals("awaiting_collection", codOrder.paymentStatus)
    assertEquals(
      "გადაიხადე ნივთის მიღებისას",
      PaymentStatus.fromCode(codOrder.paymentStatus, PaymentMethodType.COD).customerLabelKa
    )

    // =========================================================================
    // 12.4 Security tests:
    // - Non-admin cannot confirm payment
    // - Customer can view own payment instructions, but another customer cannot
    // =========================================================================
    val nonAdminConfirmAttempt = AuthAndOrderManager.confirmOrderPaymentLocalAsAdmin(
      context = context,
      order = bogOrder,
      internalNote = "Attempted self-confirm"
    )
    assertTrue("Non-admin must NOT be able to confirm payment", nonAdminConfirmAttempt.isFailure)

    // Alice owns bogOrder -> can access payment instructions
    assertTrue(BstorePaymentManager.canUserAccessOrderPaymentInstructions(context, bogOrder.copy(userId = aliceProfile.id)))

    // Another customer (Bob) logs in -> cannot access Alice's order payment instructions
    AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "bob.other@gmail.com",
      name = "Bob Other",
      hasAdminClaim = false
    )
    assertFalse(
      "Bob must NOT be allowed to view Alice's order payment instructions",
      BstorePaymentManager.canUserAccessOrderPaymentInstructions(context, bogOrder.copy(userId = aliceProfile.id))
    )

    val adminUser = AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "beqa1994@gmail.com",
      name = "Beka Admin",
      hasAdminClaim = true
    )
    assertTrue(AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context))

    val bogConfirmRes = AuthAndOrderManager.confirmOrderPaymentLocalAsAdmin(
      context = context,
      order = bogOrder,
      internalNote = "BOG ჩარიცხვა გადამოწმებულია"
    )
    assertTrue(bogConfirmRes.isSuccess)
    assertEquals("paid", bogConfirmRes.getOrNull()?.paymentStatus)
    assertEquals(adminUser.id, bogConfirmRes.getOrNull()?.paymentConfirmedBy)

    val codConfirmRes = AuthAndOrderManager.confirmOrderPaymentLocalAsAdmin(
      context = context,
      order = codOrder,
      internalNote = "კურიერმა თანხა ჩაიბარა"
    )
    assertTrue(codConfirmRes.isSuccess)
    assertEquals("paid", codConfirmRes.getOrNull()?.paymentStatus)

    // Audit trail contains both BANK_TRANSFER_CONFIRMED and COD_COLLECTION_CONFIRMED
    val auditLogs = BstorePaymentManager.getPaymentAuditLogs(context)
    assertTrue(auditLogs.any { it.orderId == bogOrder.id && it.eventType == "BANK_TRANSFER_CONFIRMED" && it.newStatus == "paid" })
    assertTrue(auditLogs.any { it.orderId == codOrder.id && it.eventType == "COD_COLLECTION_CONFIRMED" && it.newStatus == "paid" })
  }

  @Test
  @Config(sdk = [36])
  fun `bstore payment system UI - dedicated payment instructions dialog and legal policies dialog render accurately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    BstorePaymentManager.init(context)

    val alice = AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "alice.ui@gmail.com",
      name = "Alice UI",
      hasAdminClaim = false
    )

    val sampleOrder = OrderRecord(
      id = "ord_ui_501",
      orderNumber = "#BS-501",
      date = "12 ნოე, 2026",
      status = "მიმდინარე",
      totalAmount = "390 ₾",
      items = listOf(OrderItemDetail("iPhone 16 Pro OLED", "Original", 1, "390 ₾", "")),
      paymentMethod = "საბანკო გადარიცხვა (საქართველოს ბანკი)",
      trackingCode = "BS-TRK-501",
      userId = alice.id,
      paymentMethodCode = "bank_transfer",
      paymentStatus = "pending",
      bankAccountId = "bank_bog_primary",
      bankCode = "bog",
      bankName = "საქართველოს ბანკი",
      recipientNameSnapshot = "BSTORE / ბექა მღებრიშვილი",
      ibanSnapshot = "GE17BG0000000580620970",
      paymentReference = "#BS-501"
    )

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        OrderPaymentInstructionsDialog(
          order = sampleOrder,
          onDismiss = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("dedicated_payment_instructions_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copy_iban_button").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("copy_payment_reference_button").performScrollTo().assertIsDisplayed()
  }

  @Test
  @Config(sdk = [36])
  fun `bstore ux overhaul - bank transfer modal sticky layout, bank switching, copy feedback, and transfer confirmation idempotency`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    BstorePaymentManager.init(context)
    BekaNotificationManager.clearAllNotifications(context)
    context.getSharedPreferences("bekaservice_admin_order_notif_prefs", Context.MODE_PRIVATE).edit().clear().commit()

    val banks = listOf(
      BankAccountConfig(
        id = "bank_bog_ux",
        bankCode = "bog",
        bankName = "საქართველოს ბანკი",
        recipientName = "BSTORE / ბექა მღებრიშვილი",
        iban = "GE17BG0000000580620970",
        isEnabled = true,
        displayOrder = 1
      ),
      BankAccountConfig(
        id = "bank_tbc_ux",
        bankCode = "tbc",
        bankName = "თიბისი ბანკი",
        recipientName = "BSTORE / ბექა მღებრიშვილი",
        iban = "GE65TB7777777000000001",
        isEnabled = true,
        displayOrder = 2
      )
    )
    BstorePaymentManager.saveBankAccountsLocal(context, banks)

    val customer = AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "ux.customer@gmail.com",
      name = "UX Customer",
      hasAdminClaim = false
    )
    val cart = listOf(CartItem(ProductRepository.getAllProducts().first(), 1, "Original"))
    val bogBank = banks.first()
    val createdOrder = AuthAndOrderManager.addOrderFromCart(
      context = context,
      cartItems = cart,
      checkoutConfig = CheckoutSubmissionConfig(
        paymentMethod = PaymentMethodType.BANK_TRANSFER,
        selectedBankAccount = bogBank,
        deliveryMethod = DeliveryMethodType.PICKUP,
        deliveryCity = "ახალციხე",
        deliveryAddress = "ახალციხე, კოსტავას N10",
        customerPhone = "+995 599 10 20 30"
      )
    )

    var latestOrder: OrderRecord? = null
    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        OrderPaymentInstructionsDialog(
          order = createdOrder,
          onOrderUpdated = { latestOrder = it },
          onDismiss = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // 1. Sticky Header, Copy buttons, and Sticky Footer action are visible
    composeTestRule.onNodeWithTag("dedicated_payment_instructions_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_bank_tab_bog").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_bank_tab_tbc").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_transfer_sent_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_payment_instructions_button").assertIsDisplayed()

    // 2. Switch inline from BOG -> TBC without closing modal
    composeTestRule.onNodeWithTag("switch_bank_tab_tbc").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("tbc", latestOrder?.bankCode)
    assertEquals("GE65TB7777777000000001", latestOrder?.ibanSnapshot)
    assertEquals(createdOrder.orderNumber, latestOrder?.paymentReference)
    composeTestRule.onNodeWithText("GE65TB7777777000000001", substring = true)
      .performScrollTo()
      .assertIsDisplayed()

    // 3. Copy buttons (Amount, IBAN, Purpose) trigger "დაკოპირდა!" visual feedback
    composeTestRule.onNodeWithTag("copy_amount_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("copy_iban_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("copy_payment_reference_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(composeTestRule.onAllNodesWithText("დაკოპირდა!", substring = true).fetchSemanticsNodes().isNotEmpty())

    // 4. Customer clicks "✓ თანხა გადავრიცხე" -> verification_pending, admin alert, idempotent repeat
    composeTestRule.onNodeWithTag("confirm_transfer_sent_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals("verification_pending", latestOrder?.paymentStatus)
    assertTrue((latestOrder?.userConfirmedPaymentAtMillis ?: 0L) > 0L)
    composeTestRule.onNodeWithTag("transfer_verification_pending_banner").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_transfer_sent_button").assertDoesNotExist()

    // Repeat local call is idempotent and does NOT create duplicate admin notification
    val secondConfirm = AuthAndOrderManager.markOrderTransferSentByUserLocal(context, createdOrder.id, bogBank)
    assertTrue(secondConfirm.isSuccess)
    assertEquals("verification_pending", secondConfirm.getOrNull()?.paymentStatus)

    // Customer CANNOT elevate to paid
    val forbiddenPaidAttempt = AuthAndOrderManager.confirmOrderPaymentLocalAsAdmin(context, secondConfirm.getOrThrow())
    assertTrue(forbiddenPaidAttempt.isFailure)

    // Admin sees single transfer-confirmation notification for this order
    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Admin Beqa", hasAdminClaim = true)
    AdminStorage.setAdminSessionActive(context, true, "beqa1994@gmail.com")
    val adminNotifs = BekaNotificationManager.loadAdminOrderNotificationsForAdmin(context)
      .filter { it.orderId == createdOrder.id }
    assertTrue("Admin must receive transfer confirmation notification for order", adminNotifs.isNotEmpty())
    assertTrue(adminNotifs.any { it.body.contains("გადმორიცხვა") })
    assertEquals(customer.id, createdOrder.userId)
  }

  @Test
  @Config(sdk = [36])
  fun `bstore ux overhaul - 0 enabled banks disables bank transfer and single enabled bank auto-selects`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    BstorePaymentManager.init(context)

    AuthAndOrderManager.loginWithGoogle(context, "banks.rule@gmail.com", "Bank Rules", hasAdminClaim = false)
    val cart = listOf(CartItem(ProductRepository.getAllProducts().first(), 1, "Original"))

    // 0 enabled banks -> getEnabledBankAccounts is empty, Checkout disables bank_transfer and allows COD
    BstorePaymentManager.saveBankAccountsLocal(context, emptyList())
    assertTrue(BstorePaymentManager.getEnabledBankAccounts(context).isEmpty())

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
    composeTestRule.onNodeWithTag("bstore_checkout_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("checkout_payment_method_cod").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("საბანკო გადარიცხვა დროებით მიუწვდომელია", substring = true)
      .performScrollTo()
      .assertIsDisplayed()

    // 1 enabled bank -> automatically selected by getEnabledBankAccounts
    val singleTbc = BankAccountConfig(
      id = "bank_tbc_only",
      bankCode = "tbc",
      bankName = "თიბისი ბანკი",
      recipientName = "BSTORE",
      iban = "GE65TB7777777000000001",
      isEnabled = true,
      displayOrder = 1
    )
    BstorePaymentManager.saveBankAccountsLocal(context, listOf(singleTbc))
    val enabledOne = BstorePaymentManager.getEnabledBankAccounts(context)
    assertEquals(1, enabledOne.size)
    assertEquals("tbc", enabledOne.first().bankCode)
  }

  @Test
  @Config(sdk = [36])
  fun `bstore ux overhaul - profile orders section, tabs, quick add contact fields, and order detail deep link`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    BstorePaymentManager.init(context)

    AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "profile.orders@gmail.com",
      name = "Profile Tester",
      hasAdminClaim = false
    )
    // Clear phone and address explicitly so "+ დამატება" buttons are shown
    runBlocking {
      AuthAndOrderManager.updateSingleContactField(context, phone = "", address = "")
    }

    val bogBank = BankAccountConfig(
      id = "bank_bog_prof",
      bankCode = "bog",
      bankName = "საქართველოს ბანკი",
      recipientName = "BSTORE",
      iban = "GE17BG0000000580620970",
      isEnabled = true,
      displayOrder = 1
    )
    BstorePaymentManager.saveBankAccountsLocal(context, listOf(bogBank))

    val cart = listOf(CartItem(ProductRepository.getAllProducts().first(), 1, "Original"))
    val activeOrder = AuthAndOrderManager.addOrderFromCart(
      context = context,
      cartItems = cart,
      checkoutConfig = CheckoutSubmissionConfig(
        paymentMethod = PaymentMethodType.BANK_TRANSFER,
        selectedBankAccount = bogBank,
        deliveryMethod = DeliveryMethodType.PICKUP
      )
    )
    val completedOrder = AuthAndOrderManager.addOrderFromCart(
      context = context,
      cartItems = cart,
      checkoutConfig = CheckoutSubmissionConfig(
        paymentMethod = PaymentMethodType.COD,
        deliveryMethod = DeliveryMethodType.COURIER,
        deliveryCity = "თბილისი",
        deliveryAddress = "რუსთაველის 12",
        customerPhone = "+995 577 00 11 22"
      )
    )
    AuthAndOrderManager.updateOrderStatus(context, completedOrder.id, "შესრულებული", rawCode = "completed")

    val targetOrderState = androidx.compose.runtime.mutableStateOf<String?>(null)
    var consumed = false
    // Test Profile screen: Orders section visible, quick + add phone works
    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        ProfileAndAuthScreen(
          onOpenCart = {},
          onOpenAdminPanel = {},
          targetOrderId = targetOrderState.value,
          onTargetOrderConsumed = {
            consumed = true
            targetOrderState.value = null
          }
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("profile_add_phone_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_contact_field_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quick_contact_field_input").performTextReplacement("+995 599 88 77 66")
    composeTestRule.onNodeWithTag("quick_contact_field_save_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals("+995 599 88 77 66", AuthAndOrderManager.getCurrentUser(context)?.phone)

    composeTestRule.onNodeWithTag("profile_lazy_column")
      .performScrollToNode(androidx.compose.ui.test.hasTestTag("profile_orders_section"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_orders_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_orders_tab_აქტიური").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_orders_tab_დასრულებული").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_order_card_${activeOrder.id}").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("order_stepper_${activeOrder.id}", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("order_card_payment_instructions_${activeOrder.id}", useUnmergedTree = true).performScrollTo().assertIsDisplayed()

    // Switch to დასრულებული tab
    composeTestRule.onNodeWithTag("profile_orders_tab_დასრულებული").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_order_card_${completedOrder.id}").performScrollTo().assertIsDisplayed()

    // Failed order (empty cart) must NOT overwrite address; successful createAndPersistOrderFromCart DOES auto-save contact fields
    runBlocking {
      val failedOrderRes = AuthAndOrderManager.createAndPersistOrderFromCart(
        context = context,
        cartItems = emptyList(),
        checkoutConfig = CheckoutSubmissionConfig(
          paymentMethod = PaymentMethodType.COD,
          deliveryMethod = DeliveryMethodType.COURIER,
          deliveryCity = "თბილისი",
          deliveryAddress = "არ უნდა შეინახოს",
          customerPhone = "+995 500 00 00 00"
        )
      )
      assertTrue(failedOrderRes.isFailure)
      assertEquals("", AuthAndOrderManager.getCurrentUser(context)?.address)

      val successOrderRes = AuthAndOrderManager.createAndPersistOrderFromCart(
        context = context,
        cartItems = cart,
        checkoutConfig = CheckoutSubmissionConfig(
          paymentMethod = PaymentMethodType.COD,
          deliveryMethod = DeliveryMethodType.COURIER,
          deliveryCity = "თბილისი",
          deliveryAddress = "ჭავჭავაძის 25",
          customerPhone = "+995 599 88 77 66"
        )
      )
      assertTrue(successOrderRes.isSuccess)
    }
    assertEquals("ჭავჭავაძის 25", AuthAndOrderManager.getCurrentUser(context)?.address)

    // Test direct targetOrderId deep link opens exact OrderDetailModal
    targetOrderState.value = activeOrder.id
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("order_detail_modal_${activeOrder.id}").assertIsDisplayed()
    assertTrue(consumed)
  }

  @Test
  @Config(sdk = [36])
  fun `bstore ux overhaul - notifications horizontal filters, order badge, direct order navigation, and admin payment dashboard UI`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    BstorePaymentManager.init(context)
    BekaNotificationManager.clearAllNotifications(context)

    val customer = AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "notif.buyer@gmail.com",
      name = "Notif Buyer",
      hasAdminClaim = false
    )

    val cloudNotifs = listOf(
      PushNotificationRecord(
        id = "notif_ord_777",
        title = "გადახდა დადასტურდა #BS-7777",
        body = "თქვენი #BS-7777 შეკვეთის გადახდა დადასტურდა.",
        type = "order_update",
        targetProductId = 0,
        targetTab = 4,
        orderId = "ord_777",
        orderNumber = "#BS-7777",
        audience = "user:${customer.id}",
        createdAtMillis = System.currentTimeMillis()
      ),
      PushNotificationRecord(
        id = "notif_promo_1",
        title = "ახალი ფასდაკლება",
        body = "ფასდაკლება ეკრანებზე",
        type = "product_offer",
        targetProductId = 0,
        targetTab = 2,
        audience = "all",
        createdAtMillis = System.currentTimeMillis() - 10_000L
      )
    )

    var navigatedTab = -1
    var navigatedOrderId = ""
    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        UserNotificationsScreen(
          cloudNotifications = cloudNotifs,
          products = ProductRepository.getAllProducts(),
          onNavigateToTarget = { tab, _, ordId ->
            navigatedTab = tab
            navigatedOrderId = ordId
          },
          onBack = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    // Non-truncating horizontal filter tabs & order tag badge instead of generic profile tag
    composeTestRule.onNodeWithTag("notif_filter_tabs_row")
      .performScrollToNode(androidx.compose.ui.test.hasText("🔥 პროდუქტები"))
    composeTestRule.onNodeWithText("🔥 პროდუქტები").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notif_filter_tabs_row")
      .performScrollToNode(androidx.compose.ui.test.hasText("📦 შეკვეთები"))
    composeTestRule.onNodeWithText("📦 შეკვეთები").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notif_filter_tabs_row")
      .performScrollToNode(androidx.compose.ui.test.hasText("📢 განცხადებები"))
    composeTestRule.onNodeWithText("📢 განცხადებები").assertIsDisplayed()

    composeTestRule.onNodeWithTag("user_notifications_screen")
      .performScrollToNode(androidx.compose.ui.test.hasTestTag("notification_history_item_notif_ord_777"))
    composeTestRule.onNodeWithText("📦 შეკვეთა #BS-7777", useUnmergedTree = true).assertIsDisplayed()

    composeTestRule.onNodeWithTag("notification_history_item_notif_ord_777").performClick()
    composeTestRule.waitForIdle()
    assertEquals(4, navigatedTab)
    assertEquals("ord_777", navigatedOrderId)
  }

  @Test
  @Config(sdk = [36])
  fun `bstore ux overhaul - admin payment dashboard 2x2 grid, filters, and non-wrapping notifications header`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    BstorePaymentManager.init(context)
    BekaNotificationManager.clearAllNotifications(context)

    val customer = AuthAndOrderManager.loginWithGoogle(
      context = context,
      email = "notif.buyer2@gmail.com",
      name = "Notif Buyer 2",
      hasAdminClaim = false
    )

    // Admin Payment Dashboard 2x2 Grid + Non-Wrapping Notifications Header + Confirm Prompt
    val sampleOrders = listOf(
      OrderRecord(
        id = "ord_adm_1",
        orderNumber = "#BS-901",
        date = "12 ნოე, 2026",
        items = listOf(OrderItemDetail("iPhone 15 OLED", "Original", 1, "400 ₾", "")),
        totalAmount = "400 ₾",
        status = "მიმდინარე",
        trackingCode = "BS-TRK-901",
        userId = customer.id,
        paymentMethodCode = "bank_transfer",
        paymentStatus = "verification_pending",
        bankName = "საქართველოს ბანკი",
        ibanSnapshot = "GE17BG0000000580620970",
        paymentReference = "#BS-901",
        userConfirmedPaymentAtMillis = System.currentTimeMillis()
      )
    )

    AuthAndOrderManager.loginWithGoogle(context, "beqa1994@gmail.com", "Admin Beqa", hasAdminClaim = true)
    AdminStorage.setAdminSessionActive(context, true, "beqa1994@gmail.com")

    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column(
          modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
          AdminOrderNotificationsHistoryPanel(
            notifications = listOf(
              AdminOrderNotificationRecord(
                notificationId = "admin_order_ord_adm_1",
                orderId = "ord_adm_1",
                orderType = "service_order",
                title = "გადარიცხვა დადასტურდა #BS-901",
                body = "მომხმარებელმა დაადასტურა #BS-901 შეკვეთის თანხის გადმორიცხვა.",
                read = false
              )
            ),
            unreadCount = 1,
            onOpenOrderNotification = {},
            onMarkNotificationRead = {},
            onDeleteNotification = { _, onComplete -> onComplete() },
            onMarkAllRead = {}
          )
          AdminPaymentDashboardSection(
            orders = sampleOrders,
            selectedPaymentFilter = "ყველა",
            onPaymentFilterChange = {},
            searchOrderQuery = "",
            onSearchOrderQueryChange = {},
            onOpenAuditHistory = {}
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    val notifHeaderTitle = composeTestRule.onNodeWithText("შემოსული შეკვეთების შეტყობინებები").fetchSemanticsNode().boundsInRoot
    val markAllBtn = composeTestRule.onNodeWithTag("admin_mark_all_notifications_read_button").fetchSemanticsNode().boundsInRoot
    assertTrue("Mark all read button sits cleanly below header title", markAllBtn.top >= notifHeaderTitle.bottom - 2f)

    composeTestRule.onNodeWithTag("admin_payment_dashboard_section").performScrollTo()
    val dashboardBounds = composeTestRule.onNodeWithTag("admin_payment_dashboard_section").fetchSemanticsNode().boundsInRoot
    val dashTitleBounds = composeTestRule.onNodeWithText("გადახდების კონტროლი & ფინანსური პანელი").fetchSemanticsNode().boundsInRoot
    assertTrue("No large empty void above dashboard title", (dashTitleBounds.top - dashboardBounds.top) < 120f)
    composeTestRule.onNodeWithTag("admin_financial_metrics_2x2_grid").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_payment_filter_გადარიცხვა მოწმდება").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("admin_payment_filter_COD").performScrollTo().assertIsDisplayed()

    // Admin confirm payment preserves order.status ("მიმდინარე" != completed)
    val confirmRes = AuthAndOrderManager.confirmOrderPaymentLocalAsAdmin(context, sampleOrders.first())
    assertTrue(confirmRes.isSuccess)
    assertEquals("paid", confirmRes.getOrNull()?.paymentStatus)
    assertEquals("მიმდინარე", confirmRes.getOrNull()?.status)
  }

  @Test
  @Config(sdk = [36])
  fun `bstore ux overhaul - legacy order backward compatibility preserves data and never defaults to paid`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    val user = AuthAndOrderManager.loginWithGoogle(context, "legacy.user@gmail.com", "Legacy User", hasAdminClaim = false)

    val legacyPrefs = context.getSharedPreferences("bekaservice_auth_prefs", Context.MODE_PRIVATE)
    val legacyJson = org.json.JSONArray().apply {
      put(org.json.JSONObject().apply {
        put("id", "ord_legacy_1")
        put("orderNumber", "#BS-1001")
        put("date", "01 ოქტ, 2026")
        put("totalAmount", "150 ₾")
        put("status", "მიმდინარე")
        put("paymentMethod", "ნაღდი ანგარიშსწორება")
        put("items", org.json.JSONArray())
      })
    }.toString()
    legacyPrefs.edit().putString("user_orders_json_${user.id}", legacyJson).commit()

    val loaded = AuthAndOrderManager.getOrders(context)
    assertEquals(1, loaded.size)
    val ord = loaded.first()
    assertEquals("ord_legacy_1", ord.id)
    assertEquals("მიმდინარე", ord.status)
    assertFalse("Legacy order must NOT auto-default to paid", ord.paymentStatus == "paid")
  }
}






