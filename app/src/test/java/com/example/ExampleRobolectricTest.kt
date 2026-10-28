package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
}


