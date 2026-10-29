package com.example

import android.content.Context
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
  fun `header logo BEKASERVICE and subtitle are symmetric and vertically aligned with right action buttons`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.init(context)
    AppPreferencesManager.setOnboardingCompleted(context, true)
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
    val brandSubtitleNode = composeTestRule.onNodeWithTag("header_brand_subtitle", useUnmergedTree = true).assertIsDisplayed()

    val menuBtnNode = composeTestRule.onNodeWithTag("drawer_menu_button", useUnmergedTree = true).assertIsDisplayed()
    val notifBtnNode = composeTestRule.onNodeWithTag("notifications_button", useUnmergedTree = true).assertIsDisplayed()
    val profileBtnNode = composeTestRule.onNodeWithTag("profile_button", useUnmergedTree = true).assertIsDisplayed()
    val cartBtnNode = composeTestRule.onNodeWithTag("cart_button", useUnmergedTree = true).assertIsDisplayed()

    val logoTitleBounds = logoTitleNode.fetchSemanticsNode().boundsInRoot
    val logoEmblemBounds = logoEmblemNode.fetchSemanticsNode().boundsInRoot
    val logoTextColBounds = logoTextColNode.fetchSemanticsNode().boundsInRoot
    val brandTitleRowBounds = brandTitleRowNode.fetchSemanticsNode().boundsInRoot
    val brandSubtitleBounds = brandSubtitleNode.fetchSemanticsNode().boundsInRoot

    val menuBounds = menuBtnNode.fetchSemanticsNode().boundsInRoot
    val notifBounds = notifBtnNode.fetchSemanticsNode().boundsInRoot
    val profileBounds = profileBtnNode.fetchSemanticsNode().boundsInRoot
    val cartBounds = cartBtnNode.fetchSemanticsNode().boundsInRoot

    // 1. BEKASERVICE და "სანდო სერვისი" ახლოს და ასო "B"-ს გასწვრივ ერთად:
    val verticalGap = brandSubtitleBounds.top - brandTitleRowBounds.bottom
    assertTrue(
      "Vertical gap between BEKASERVICE and სანდო სერვისი should be tight (<= 2px), was $verticalGap",
      verticalGap <= 2.0f
    )
    assertTrue(
      "BEKASERVICE and სანდო სერვისი must start at the exact same left edge under 'B', diff=${abs(brandTitleRowBounds.left - brandSubtitleBounds.left)}",
      abs(brandTitleRowBounds.left - brandSubtitleBounds.left) <= 1.5f
    )

    // 2. ლოგო წარწერით BEKASERVICE უნდა უტოლდებოდეს ერთ სიმაღლეზე მარჯვენა მხარეს არსებულ შეტყობინების, მომხმარებლის და კალათის ღილაკებს:
    assertEquals(notifBounds.height, logoTitleBounds.height, 1.5f)
    assertEquals(notifBounds.height, logoEmblemBounds.height, 1.5f)
    assertEquals(notifBounds.height, logoTextColBounds.height, 1.5f)
    assertEquals(notifBounds.height, profileBounds.height, 1.5f)
    assertEquals(notifBounds.height, cartBounds.height, 1.5f)

    assertEquals(notifBounds.center.y, logoTitleBounds.center.y, 1.5f)
    assertEquals(profileBounds.center.y, logoTitleBounds.center.y, 1.5f)
    assertEquals(cartBounds.center.y, logoTitleBounds.center.y, 1.5f)
    assertEquals(menuBounds.center.y, logoTitleBounds.center.y, 1.5f)
    assertEquals(notifBounds.center.y, logoTextColBounds.center.y, 1.5f)

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
    assertEquals("ბექასერვისი", appName)
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
  fun `admin storage pin verification and update works`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    // საწყისი PIN კოდია "bmgebr"
    assertTrue("Default PIN should be bmgebr", AdminStorage.verifyPin(context, "bmgebr"))
    assertFalse("Invalid PIN should be rejected", AdminStorage.verifyPin(context, "wrong123"))

    // PIN-ის შეცვლა
    val changed = AdminStorage.setAdminPin(context, "newpin99")
    assertTrue(changed)
    assertTrue("New PIN should verify", AdminStorage.verifyPin(context, "newpin99"))
    assertFalse("Old PIN should no longer verify", AdminStorage.verifyPin(context, "bmgebr"))

    // აღდგენა ტესტის შემდგომ
    AdminStorage.setAdminPin(context, "bmgebr")
    assertTrue(AdminStorage.verifyPin(context, "bmgebr"))
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

    // პაროლის შეცვლა
    val (changedOk, _) = AuthAndOrderManager.changePassword(context, "password123", "bekaSecure2026")
    assertTrue(changedOk)

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
  fun `ai assistant recommends only Canva when user asks for canva and does not dump other multimedia services`() {
    val allProducts = ProductRepository.getAllProducts()

    // 1. "canva მინდა" should return ONLY Canva Pro (id=118), NOT CapCut (116) or Adobe (117)
    val matchedForCanva = GeminiDiagnosticService.findRelevantProducts("canva მინდა", allProducts)
    assertEquals(1, matchedForCanva.size)
    assertEquals(118, matchedForCanva.first().id)
    assertEquals("Canva Pro", matchedForCanva.first().title)

    // 2. Local diagnostic text should be dedicated to Canva Pro without dumping CapCut or Adobe
    val diagnosticReply = GeminiDiagnosticService.findRelevantProducts("კანვა მინდა", allProducts)
    assertEquals(1, diagnosticReply.size)
    assertEquals(118, diagnosticReply.first().id)

    // 3. Similarly for CapCut: should return ONLY CapCut Pro (id=116)
    val matchedForCapCut = GeminiDiagnosticService.findRelevantProducts("capcut მინდა", allProducts)
    assertEquals(1, matchedForCapCut.size)
    assertEquals(116, matchedForCapCut.first().id)

    // 4. General multimedia query should return the broader multimedia services
    val matchedGeneral = GeminiDiagnosticService.findRelevantProducts("მულტიმედია რა სერვისები გაქვთ?", allProducts)
    assertTrue("General multimedia request should return multiple services", matchedGeneral.size >= 3)
    assertTrue(matchedGeneral.any { it.id == 118 })
    assertTrue(matchedGeneral.any { it.id == 116 })
    assertTrue(matchedGeneral.any { it.id == 117 })
  }

  @Test
  fun `ai assistant dynamically finds newly added services from catalog`() {
    val baseProducts = ProductRepository.getAllProducts()

    // Create a newly added service by admin
    val newCustomService = ProductItem(
      id = 999,
      title = "Midjourney v7 Mega",
      price = "45 ₾",
      category = "პროგრამული სერვისები",
      type = "software",
      subcategory = "ხელოვნური ინტელექტი",
      imageUrl = "https://example.com/midjourney.jpg",
      shortDescription = "Midjourney v7 უმაღლესი ხარისხის ფოტორეალისტური AI გენერატორი ულიმიტო GPU საათებით.",
      features = listOf("v7 photorealism", "Fast GPU hours", "Commercial license"),
      offerType = "თვიური გამოწერა",
      duration = "1 თვე",
      accountType = "Private",
      activationTerms = "მყისიერი აქტივაცია",
      searchKeywords = "midjourney, მიდჯორნი, გენერაცია",
      inStock = true,
      brand = "Midjourney"
    )

    val updatedCatalog = baseProducts + newCustomService

    // User asks for the newly added service: "midjourney მინდა"
    val foundNewService = GeminiDiagnosticService.findRelevantProducts("midjourney მინდა", updatedCatalog)
    assertEquals(1, foundNewService.size)
    assertEquals(999, foundNewService.first().id)
    assertEquals("Midjourney v7 Mega", foundNewService.first().title)

    // User asks with Georgian transliteration
    val foundNewGeorgian = GeminiDiagnosticService.findRelevantProducts("მიდჯორნი მინდა", updatedCatalog)
    assertEquals(1, foundNewGeorgian.size)
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

    // 5. Screen 4 (Ready & Benefits) displays and finishes onboarding into the existing Home screen
    composeTestRule.onNodeWithTag("onboarding_ready_step").assertIsDisplayed()
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
  fun `splash screen renders logo with ბექასერვისი brand wordmark underneath and adaptive icon has 108dp safe zone dimensions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Verify adaptive launcher foreground intrinsic size corresponds to 108dp (not 66dp stretched)
    val density = context.resources.displayMetrics.density
    val fgDrawable = context.getDrawable(R.drawable.ic_launcher_foreground)
    assertNotNull(fgDrawable)
    val expected108Px = (108f * density).toInt()
    assertEquals("Foreground drawable intrinsic width must be 108dp", expected108Px.toFloat(), fgDrawable!!.intrinsicWidth.toFloat(), 2.0f)
    assertEquals("Foreground drawable intrinsic height must be 108dp", expected108Px.toFloat(), fgDrawable.intrinsicHeight.toFloat(), 2.0f)

    // 2. Verify VideoSplashScreen displays logo and "ბექასერვისი" below it with uniform fade-in and standard duration
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

    val logoNode = composeTestRule.onNodeWithTag("splash_logo_image", useUnmergedTree = true).assertIsDisplayed()
    val brandTitleNode = composeTestRule.onNodeWithTag("splash_brand_title_row", useUnmergedTree = true).assertIsDisplayed()
    val brandSubtitleNode = composeTestRule.onNodeWithTag("splash_brand_subtitle", useUnmergedTree = true).assertIsDisplayed()

    val logoBounds = logoNode.fetchSemanticsNode().boundsInRoot
    val brandTitleBounds = brandTitleNode.fetchSemanticsNode().boundsInRoot
    val brandSubtitleBounds = brandSubtitleNode.fetchSemanticsNode().boundsInRoot

    assertTrue("Brand wordmark ბექასერვისი must be positioned below the logo", brandTitleBounds.top > logoBounds.bottom)
    assertTrue("Subtitle სანდო სერვისი must be positioned below ბექასერვისი", brandSubtitleBounds.top >= brandTitleBounds.bottom - 1f)
    assertEquals("Logo and ბექასერვისი must be horizontally centered together", logoBounds.center.x, brandTitleBounds.center.x, 2.0f)

    composeTestRule.mainClock.advanceTimeBy(1000L)
    composeTestRule.waitForIdle()
    assertTrue("Splash screen should complete after standard animation duration", finished)
  }

  @Test
  fun `admin product dialog supports AI auto-fill, manual category and brand entry, and custom photo via URL or phone memory`() {
    // 1. Verify ProductImageManager distinguishes auto-generated vs URL vs phone memory file
    val autoProduct = ProductItem(
      id = 301,
      title = "PDF Pro ყოველთვიური გამოწერა",
      price = "25 ₾",
      category = "პროგრამული სერვისები",
      type = "software",
      imageUrl = ProductImageManager.AUTO_GENERATED_IMAGE_MARKER
    )
    assertFalse("Auto marker should use auto-generated visual", ProductImageManager.hasCustomAttachedPhoto(autoProduct))

    val urlProduct = autoProduct.copy(imageUrl = "https://example.com/custom_pdf_banner.png")
    assertTrue("Real URL should be recognized as custom attached photo", ProductImageManager.hasCustomAttachedPhoto(urlProduct))

    val localMemoryProduct = autoProduct.copy(imageUrl = "file:///data/user/0/com.example/files/product_images/prod_img_123.jpg")
    assertTrue("Phone memory file:// URI should be recognized as custom attached photo", ProductImageManager.hasCustomAttachedPhoto(localMemoryProduct))
    assertTrue(ProductImageManager.isLocalOrEmbeddedImage(localMemoryProduct.imageUrl))

    // 2. Verify ProductEditDialog UI: AI auto-fill button, custom photo URL input, and manual category/brand fields
    var savedProduct: ProductItem? = null
    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        ProductEditDialog(
          product = autoProduct.copy(title = "Adobe Acrobat PDF Pro ყოველთვიური გამოწერა", shortDescription = ""),
          isNew = true,
          existingCategories = listOf("პროგრამული სერვისები", "ეკრანები"),
          existingBrands = listOf("OpenAI", "Adobe", "Apple"),
          onDismiss = {},
          onSave = { savedProduct = it }
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
    composeTestRule.onNodeWithText("გალერეა", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("photo_mode_url_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("product_edit_lazy_column")
      .performScrollToNode(hasTestTag("product_image_url_input"))
    composeTestRule.onNodeWithTag("product_image_url_input")
      .performTextReplacement("https://example.com/real_acrobat_photo.jpg")
    composeTestRule.waitForIdle()

    // Manually enter a custom category and custom brand
    composeTestRule.onNodeWithTag("product_edit_lazy_column")
      .performScrollToNode(hasTestTag("custom_category_input"))
    composeTestRule.onNodeWithTag("custom_category_input")
      .performTextReplacement("PDF და საოფისე პროგრამები")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("product_edit_lazy_column")
      .performScrollToNode(hasTestTag("custom_brand_input"))
    composeTestRule.onNodeWithTag("custom_brand_input")
      .performTextReplacement("Adobe Systems")
    composeTestRule.waitForIdle()

    // Save the product
    composeTestRule.onNodeWithTag("save_product_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    assertNotNull("Product must be saved when clicking შენახვა", savedProduct)
    assertEquals("https://example.com/real_acrobat_photo.jpg", savedProduct!!.imageUrl)
    assertEquals("PDF და საოფისე პროგრამები", savedProduct!!.category)
    assertEquals("Adobe Systems", savedProduct!!.brand)
    assertTrue("AI auto-fill must populate shortDescription", savedProduct!!.shortDescription.isNotBlank())
    assertTrue("AI auto-fill must populate full description", savedProduct!!.description.isNotBlank())
    assertTrue("AI auto-fill must populate features", savedProduct!!.features.isNotEmpty())
  }

  @Test
  fun `gallery photo button is centered, software detail განახლდა badge stays on one line, and home pull-to-refresh animates with pull progress`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleSoftwareProduct = ProductItem(
      id = 117,
      title = "Adobe Acrobat Pro ყოველთვიური გამოწერა",
      price = "25 ₾",
      category = "პროგრამული სერვისები",
      type = "software",
      subcategory = "ვიდეო, დიზაინი და კონტენტის შექმნა",
      imageUrl = ProductImageManager.AUTO_GENERATED_IMAGE_MARKER,
      offerType = "პრემიუმ გამოწერა (1 თვე)",
      duration = "1 თვე",
      lastVerifiedAt = "2026-10-25"
    )

    // 1. Verify SoftwareServiceDetailSection and BekaPullToRefreshIndicator together
    composeTestRule.setContent {
      RepairProTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column {
          SoftwareServiceDetailSection(product = sampleSoftwareProduct)
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

    val headerNode = composeTestRule.onNodeWithText("სერვისის სრული პარამეტრები").assertIsDisplayed()
    val updatedBadgeNode = composeTestRule.onNodeWithText("განახლდა: 25.10.2026").assertIsDisplayed()

    val headerBounds = headerNode.fetchSemanticsNode().boundsInRoot
    val badgeBounds = updatedBadgeNode.fetchSemanticsNode().boundsInRoot

    // Both the title and the განახლდა badge must be on a single line (equal 1-line height) and aligned on the same row
    assertEquals(
      "განახლდა badge must remain on a single line matching the 1-line header height",
      headerBounds.height,
      badgeBounds.height,
      4.0f
    )
    assertEquals(
      "განახლდა badge and header title must be vertically centered on the same row",
      headerBounds.center.y,
      badgeBounds.center.y,
      4.0f
    )
    assertTrue(
      "განახლდა badge must sit to the right of სერვისის სრული პარამეტრები",
      badgeBounds.left >= headerBounds.right - 4f
    )

    // 2. Verify BekaPullToRefreshIndicator responds to pull progress
    composeTestRule.onNodeWithTag("home_pull_refresh_indicator").assertIsDisplayed()
    composeTestRule.onNodeWithText("ჩამოწიეთ განახლებისთვის • 65%").assertIsDisplayed()
  }

  @Test
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
    composeTestRule.onNodeWithText("რა პრობლემა აქვს ტელეფონს?").assertIsDisplayed()
    composeTestRule.onNodeWithTag("diag_mode_standard").assertIsDisplayed()
    composeTestRule.onNodeWithTag("diag_mode_technician").assertIsDisplayed()
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

    // 2. Ask about a catalog product ("Canva Pro მინდა") inside the Diagnostics workspace
    composeTestRule.runOnIdle {
      holderRef!!.sendMessage(
        overrideText = "Canva Pro მინდა, რა ღირს და რა პირობებია?",
        catalogProducts = fullCatalog
      )
    }
    composeTestRule.waitUntil(timeoutMillis = 10000L) {
      !holderRef!!.isAnalyzing && holderRef!!.messages.size >= 4
    }
    composeTestRule.waitForIdle()

    val canvaReply = holderRef!!.messages.last()
    assertTrue("Catalog query for Canva Pro must recommend product 118", canvaReply.recommendedProductIds.contains(118))
    assertTrue("Catalog query must mention Canva Pro details", canvaReply.text.contains("Canva Pro", ignoreCase = true))

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
    val expectedGeorgianGreeting = "გამარჯობა! მე ვარ BEKASERVICE-ის AI დიაგნოსტიკური ასისტენტი. აღწერე ტელეფონის პრობლემა ან ატვირთე ფოტო და დაგეხმარები შესაძლო მიზეზებისა და შემოწმების ნაბიჯების განსაზღვრაში."
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
    assertTrue(enTurn.text.contains("BEKASERVICE AI Diagnostics Assistant"))
    assertFalse(enTurn.text.contains("ChatGPT"))

    AppPreferencesManager.setLanguage(context, "ru")
    val ruTurn = BekaMultimodalDiagnosticsEngine.generateContextAwareDiagnosticResponse(
      history = emptyList(),
      currentMessage = DiagnosticMessage(role = "user", text = "привет"),
      mode = DiagnosticUserMode.STANDARD,
      catalogProducts = catalog
    )
    assertTrue(ruTurn.text.contains("AI-ассистент диагностики BEKASERVICE"))
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
    assertFalse("Hardware diagnosis must not recommend digital software", turn1Recs.any { SoftwareCatalogData.isDigitalSoftwareService(it) })

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
}


