package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.RepairProTheme
import com.example.ui.theme.SoftGraphiteLightBgPrimary
import com.example.ui.theme.SoftGraphiteLightBgSecondary
import com.example.ui.theme.SoftGraphiteLightBorder
import com.example.ui.theme.SoftGraphiteLightBrandOrange
import com.example.ui.theme.SoftGraphiteLightDisabled
import com.example.ui.theme.SoftGraphiteLightSecondaryOrange
import com.example.ui.theme.SoftGraphiteLightSoftOrangeBg
import com.example.ui.theme.SoftGraphiteLightSurfaceElevated
import com.example.ui.theme.SoftGraphiteLightSurfacePrimary
import com.example.ui.theme.SoftGraphiteLightSurfaceSecondary
import com.example.ui.theme.SoftGraphiteLightTextPrimary
import com.example.ui.theme.SoftGraphiteLightTextSecondary
import com.example.ui.theme.SoftGraphiteLightTextTertiary
import com.example.ui.theme.bstorePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BstoreSoftGraphiteThemeAndOnboardingTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    AppPreferencesManager.init(context)
  }

  @Test
  fun `soft graphite light theme palette matches exact color specification while dark mode remains intact`() {
    // Verify exact Soft Graphite Light Theme hex tokens
    assertEquals(Color(0xFFF0F1F4), SoftGraphiteLightBgPrimary)
    assertEquals(Color(0xFFE9EBEF), SoftGraphiteLightBgSecondary)
    assertEquals(Color(0xFFF8F9FB), SoftGraphiteLightSurfacePrimary)
    assertEquals(Color(0xFFE5E8ED), SoftGraphiteLightSurfaceSecondary)
    assertEquals(Color(0xFFFFFFFF), SoftGraphiteLightSurfaceElevated)
    assertEquals(Color(0xFF20242B), SoftGraphiteLightTextPrimary)
    assertEquals(Color(0xFF687180), SoftGraphiteLightTextSecondary)
    assertEquals(Color(0xFF9299A5), SoftGraphiteLightTextTertiary)
    assertEquals(Color(0xFFF36B16), SoftGraphiteLightBrandOrange)
    assertEquals(Color(0xFFFF8A3D), SoftGraphiteLightSecondaryOrange)
    assertEquals(Color(0xFFFFF0E5), SoftGraphiteLightSoftOrangeBg)
    assertEquals(Color(0xFFDCE0E6), SoftGraphiteLightBorder)
    assertEquals(Color(0xFFAEB4BE), SoftGraphiteLightDisabled)

    // Verify bstorePalette(isDark = false) uses Soft Graphite Light Theme
    val lightPalette = bstorePalette(isDark = false)
    assertEquals(Color(0xFFF0F1F4), lightPalette.pageBg)
    assertEquals(Color(0xFFF8F9FB), lightPalette.cardBg)
    assertEquals(Color(0xFFFFFFFF), lightPalette.cardElevatedBg)
    assertEquals(Color(0xFFE9EBEF), lightPalette.subSurfaceBg)
    assertEquals(Color(0xFFE5E8ED), lightPalette.chipBg)
    assertEquals(Color(0xFFDCE0E6), lightPalette.borderSubtle)
    assertEquals(Color(0xFF20242B), lightPalette.textPrimary)
    assertEquals(Color(0xFF687180), lightPalette.textSecondary)
    assertEquals(Color(0xFF9299A5), lightPalette.textMuted)
    assertEquals(Color(0xFFF36B16), lightPalette.brandOrange)
    assertEquals(Color(0xFFFF8A3D), lightPalette.brandOrangeHighlight)
    assertEquals(Color(0xFFFFF0E5), lightPalette.brandOrangeSoft)

    // Verify bstorePalette(isDark = true) remains 100% preserved
    val darkPalette = bstorePalette(isDark = true)
    assertEquals(Color(0xFF0D0F12), darkPalette.pageBg)
    assertEquals(Color(0xFFFF6B00), darkPalette.brandOrange)

    // Verify AppThemeColorAdapter adapts surfaces, backgrounds, borders, and text in Light vs Dark mode
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.setDarkMode(context, false)
    assertFalse(AppPreferencesManager.isDarkMode)
    assertEquals(Color(0xFFF0F1F4), AppThemeColorAdapter.adaptPageBackground(Color(0xFF0A0A0D)))
    assertEquals(Color(0xFFF8F9FB), AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF14161D)))
    assertEquals(Color(0xFF20242B), AppThemeColorAdapter.adaptTextColor(Color.White, preserveWhite = false))
    assertEquals(Color(0xFF687180), AppThemeColorAdapter.adaptTextColor(Color(0xFF9BA1B0), preserveWhite = false))
    assertEquals(Color(0xFFDCE0E6), AppThemeColorAdapter.dividerColor())

    AppPreferencesManager.setDarkMode(context, true)
    assertTrue(AppPreferencesManager.isDarkMode)
    assertEquals(Color(0xFF0A0A0D), AppThemeColorAdapter.adaptPageBackground(Color(0xFF0A0A0D)))
    assertEquals(Color(0xFF14161D), AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF14161D)))
    assertEquals(Color.White, AppThemeColorAdapter.adaptTextColor(Color.White, preserveWhite = false))
  }

  @Test
  fun `onboarding product showcase assets are true 32-bit RGBA PNGs with transparent exterior backgrounds`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val resIds = listOf(
      R.drawable.onboarding_intro_smartphone,
      R.drawable.onboarding_intro_airpods,
      R.drawable.onboarding_intro_headphones,
      R.drawable.onboarding_intro_smartwatch
    )
    for (resId in resIds) {
      val bitmap = BitmapFactory.decodeResource(context.resources, resId)
      assertNotNull("Drawable $resId must decode into a valid Bitmap", bitmap)
      assertTrue("Drawable $resId must have an alpha channel", bitmap.hasAlpha())
      val topLeftAlpha = android.graphics.Color.alpha(bitmap.getPixel(0, 0))
      val bottomRightAlpha = android.graphics.Color.alpha(bitmap.getPixel(bitmap.width - 1, bitmap.height - 1))
      assertEquals("Top-left exterior corner of $resId must be fully transparent (alpha=0)", 0, topLeftAlpha)
      assertEquals("Bottom-right exterior corner of $resId must be fully transparent (alpha=0)", 0, bottomRightAlpha)
    }
  }

  @Test
  fun `first setup screen animates BSTORE logo and 4 product stages inside brand visual area without altering layout`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.setDarkMode(context, false)

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.setContent {
      RepairProTheme(darkTheme = AppPreferencesManager.isDarkMode) {
        BekaOnboardingFlow(onFinishOnboarding = {})
      }
    }
    composeTestRule.mainClock.advanceTimeByFrame()

    // 1. First setup screen elements are intact and step counter shows 1 / 4
    composeTestRule.onNodeWithTag("onboarding_welcome_step").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_brand_logo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_welcome_headline").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_welcome_subtitle").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_welcome_step_counter").assertIsDisplayed().assertTextContains("1 / 4")
    composeTestRule.onNodeWithTag("onboarding_welcome_next_button").assertIsDisplayed()

    // 2. Stage 0: Starts with original BSTORE logo (brand_intro)
    composeTestRule.onNodeWithTag("onboarding_intro_stage_brand_intro", useUnmergedTree = true).assertIsDisplayed()

    // 3. Stage 1: Advances to Smartphone after initial 680ms + 1350ms hold
    composeTestRule.mainClock.advanceTimeBy(2200L)
    composeTestRule.onNodeWithTag("onboarding_intro_stage_smartphone", useUnmergedTree = true).assertIsDisplayed()

    // 4. Stage 2: Advances to AirPods after 600ms transition + 1550ms hold
    composeTestRule.mainClock.advanceTimeBy(2200L)
    composeTestRule.onNodeWithTag("onboarding_intro_stage_airpods", useUnmergedTree = true).assertIsDisplayed()

    // 5. Stage 3: Advances to Over-ear Headphones after 550ms transition + 1550ms hold
    composeTestRule.mainClock.advanceTimeBy(2150L)
    composeTestRule.onNodeWithTag("onboarding_intro_stage_headphones", useUnmergedTree = true).assertIsDisplayed()

    // 6. Stage 4: Advances to Smartwatch after 550ms transition + 1550ms hold
    composeTestRule.mainClock.advanceTimeBy(2150L)
    composeTestRule.onNodeWithTag("onboarding_intro_stage_smartwatch", useUnmergedTree = true).assertIsDisplayed()

    // 7. Stage 5: Smoothly returns to original BSTORE logo (brand_return)
    composeTestRule.mainClock.advanceTimeBy(2300L)
    composeTestRule.onNodeWithTag("onboarding_intro_stage_brand_return", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun `theme selection screen is final 4 of 4 setup step with strictly equal card dimensions and guest persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AppPreferencesManager.setDarkMode(context, false)
    AppPreferencesManager.setOnboardingCompleted(context, false)

    // Confirm Setup flow constant is exactly 4 steps
    assertEquals(4, TOTAL_ONBOARDING_STEPS)

    composeTestRule.setContent {
      RepairProTheme(darkTheme = AppPreferencesManager.isDarkMode) {
        BekaOnboardingFlow(
          onFinishOnboarding = {
            AppPreferencesManager.setOnboardingCompleted(context, true)
          }
        )
      }
    }
    composeTestRule.waitForIdle()

    // Step 1 (1 / 4) -> Step 2 (2 / 4, Language) -> Step 3 (3 / 4, Auth/Guest) -> Step 4 (4 / 4, Theme Selection)
    composeTestRule.onNodeWithText("1 / 4").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_welcome_next_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("2 / 4").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_language_next_button").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("3 / 4").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_skip_auth_button").performClick()
    composeTestRule.waitForIdle()

    // Guest mode must not require authentication
    assertEquals(null, AuthAndOrderManager.getCurrentUser(context))

    // Verify we are on Step 4 / 4 (Final Setup screen)
    composeTestRule.onNodeWithTag("onboarding_theme_step").assertIsDisplayed()
    composeTestRule.onNodeWithText("4 / 4").assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_theme_headline").assertIsDisplayed()
    composeTestRule.onNodeWithText("აირჩიე აპის თემა").assertIsDisplayed()
    composeTestRule.onNodeWithText("შეგიძლია ნებისმიერ დროს შეცვალო პარამეტრებიდან.").assertIsDisplayed()

    // Confirm obsolete 5th screen ("5 / 5", "მზად ხარ!", "onboarding_ready_step") does not exist
    assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasTestTag("onboarding_ready_step")).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("5 / 5")).fetchSemanticsNodes().size)

    // Verify both Light and Dark theme cards and previews are displayed with exact equal width and height
    val lightCardBounds = composeTestRule.onNodeWithTag("onboarding_theme_light_card").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    val darkCardBounds = composeTestRule.onNodeWithTag("onboarding_theme_dark_card").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    assertEquals("Light and Dark theme cards must have identical width", lightCardBounds.width, darkCardBounds.width, 0.5f)
    assertEquals("Light and Dark theme cards must have identical height", lightCardBounds.height, darkCardBounds.height, 0.5f)
    assertEquals("Light and Dark theme cards must be aligned at top", lightCardBounds.top, darkCardBounds.top, 0.5f)
    assertEquals("Light and Dark theme cards must be aligned at bottom", lightCardBounds.bottom, darkCardBounds.bottom, 0.5f)

    val lightPreviewBounds = composeTestRule.onNodeWithTag("onboarding_theme_light_preview", useUnmergedTree = true)
      .assertIsDisplayed()
      .fetchSemanticsNode().boundsInRoot
    val darkPreviewBounds = composeTestRule.onNodeWithTag("onboarding_theme_dark_preview", useUnmergedTree = true)
      .assertIsDisplayed()
      .fetchSemanticsNode().boundsInRoot
    assertEquals("Light and Dark preview windows must have identical width", lightPreviewBounds.width, darkPreviewBounds.width, 0.5f)
    assertEquals("Light and Dark preview windows must have identical height", lightPreviewBounds.height, darkPreviewBounds.height, 0.5f)

    // Verify selection indicators have identical positions relative to their card tops
    val lightIndicatorBounds = composeTestRule.onNodeWithTag("onboarding_theme_light_card_indicator", useUnmergedTree = true)
      .assertIsDisplayed()
      .fetchSemanticsNode().boundsInRoot
    val darkIndicatorBounds = composeTestRule.onNodeWithTag("onboarding_theme_dark_card_indicator", useUnmergedTree = true)
      .assertIsDisplayed()
      .fetchSemanticsNode().boundsInRoot
    assertEquals("Selection indicators must have identical top alignment", lightIndicatorBounds.top, darkIndicatorBounds.top, 0.5f)
    assertEquals("Selection indicators must have identical size", lightIndicatorBounds.height, darkIndicatorBounds.height, 0.5f)

    // Verify final CTA button says "მთავარ გვერდზე გადასვლა" and is fully visible without clipping
    val finishButtonNode = composeTestRule.onNodeWithTag("onboarding_finish_button").assertIsDisplayed()
    composeTestRule.onNodeWithText("მთავარ გვერდზე გადასვლა").assertIsDisplayed()
    val finishButtonBounds = finishButtonNode.fetchSemanticsNode().boundsInRoot
    val rootBounds = composeTestRule.onNodeWithTag("onboarding_theme_step").fetchSemanticsNode().boundsInRoot
    assertTrue("Finish button bottom (${finishButtonBounds.bottom}) must be within root (${rootBounds.bottom})", finishButtonBounds.bottom <= rootBounds.bottom)

    // Select Dark Mode card -> persists Dark Mode immediately
    composeTestRule.onNodeWithTag("onboarding_theme_dark_card").performClick()
    composeTestRule.waitForIdle()
    assertTrue(AppPreferencesManager.isDarkMode)
    assertTrue(
      context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE)
        .getBoolean("is_dark_mode", false)
    )

    // Select Light Mode card -> persists Light Mode immediately
    composeTestRule.onNodeWithTag("onboarding_theme_light_card").performClick()
    composeTestRule.waitForIdle()
    assertFalse(AppPreferencesManager.isDarkMode)
    assertFalse(
      context.getSharedPreferences("bekaservice_ui_prefs", Context.MODE_PRIVATE)
        .getBoolean("is_dark_mode", true)
    )

    // Back button returns to Step 3 / 4 (Auth/Guest), and skipping Auth returns to Step 4 / 4 (Theme Selection)
    composeTestRule.onNodeWithTag("onboarding_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_auth_step").assertIsDisplayed()
    composeTestRule.onNodeWithText("3 / 4").assertIsDisplayed()

    composeTestRule.onNodeWithTag("onboarding_skip_auth_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_theme_step").assertIsDisplayed()
    composeTestRule.onNodeWithText("4 / 4").assertIsDisplayed()

    // Finish Setup with Light Theme selected
    composeTestRule.onNodeWithTag("onboarding_finish_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(AppPreferencesManager.isOnboardingCompleted)
    assertFalse(AppPreferencesManager.isDarkMode)

    // Simulate app restart via AppPreferencesManager.init(context): Light Theme and Setup completion remain persisted
    AppPreferencesManager.init(context)
    assertTrue("Setup completion must persist across restart", AppPreferencesManager.isOnboardingCompleted)
    assertFalse("Light Theme selection must persist across restart", AppPreferencesManager.isDarkMode)

    // Simulate Dark Theme selection and app restart
    AppPreferencesManager.setDarkMode(context, true)
    AppPreferencesManager.init(context)
    assertTrue("Dark Theme selection must persist across restart", AppPreferencesManager.isDarkMode)
  }
}

