package com.example

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

enum class AppLanguage(val code: String, val label: String, val shortLabel: String, val flag: String) {
    KA("ka", "ქართული", "GEO", "🇬🇪"),
    EN("en", "English", "ENG", "🇬🇧"),
    RU("ru", "Русский", "RUS", "🇷🇺");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: KA
        }
    }
}

object AppPreferencesManager {
    private const val PREFS_NAME = "bekaservice_ui_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private const val KEY_APP_LANG = "app_language_code"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    var isDarkMode by mutableStateOf(true)
        private set

    var currentLanguage by mutableStateOf(AppLanguage.KA)
        private set

    var isOnboardingCompleted by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true)
        currentLanguage = AppLanguage.fromCode(prefs.getString(KEY_APP_LANG, AppLanguage.KA.code))
        isOnboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setDarkMode(context: Context, dark: Boolean) {
        isDarkMode = dark
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE, dark)
            .apply()
    }

    fun toggleDarkMode(context: Context) {
        setDarkMode(context, !isDarkMode)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        currentLanguage = language
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LANG, language.code)
            .apply()
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean = true) {
        isOnboardingCompleted = completed
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
            .apply()
    }
}

// Tracks whether the current Surface/Button is a vivid accent/colored button or dark image badge
// where white text must remain white even in Light Mode.
val LocalPreserveWhiteText = compositionLocalOf { false }

object AppThemeColorAdapter {
    fun isVividAccentColor(color: Color): Boolean {
        if (color == Color.Unspecified || color.alpha < 0.5f) return false
        val max = maxOf(color.red, color.green, color.blue)
        val min = minOf(color.red, color.green, color.blue)
        val delta = max - min
        return delta > 0.28f && max > 0.45f
    }

    fun adaptPageBackground(darkColor: Color): Color {
        if (AppPreferencesManager.isDarkMode) return darkColor
        return Color(0xFFF3F6FB)
    }

    fun adaptTopBarBackground(darkColor: Color): Color {
        if (AppPreferencesManager.isDarkMode) return darkColor
        return Color(0xFFFFFFFF)
    }

    fun inputTextColor(): Color {
        return if (AppPreferencesManager.isDarkMode) Color.White else Color(0xFF0F172A)
    }

    fun dividerColor(darkColor: Color = Color(0xFF242830)): Color {
        return if (AppPreferencesManager.isDarkMode) darkColor else Color(0xFFE2E8F0)
    }

    fun adaptSurfaceColor(color: Color): Color {
        if (AppPreferencesManager.isDarkMode) return color
        if (color == Color.Unspecified || color == Color.Transparent) return color
        // Preserve semi-transparent scrims, glass badges on images, and vivid tinted badges
        if (color.alpha < 0.85f) {
            if (isVividAccentColor(color)) return color
            // Dark scrim overlay (e.g. modal backdrop Color.Black.copy(alpha = 0.7f))
            if (color.red < 0.08f && color.green < 0.08f && color.blue < 0.08f) {
                return color
            }
            return color
        }
        if (isVividAccentColor(color)) return color

        // Search bars, chat inputs, and primary cards -> crisp white in Light Mode
        if (color == Color(0xFF16191E) || color == Color(0xFF1A1D24) ||
            color == Color(0xFF14171E) || color == Color(0xFF14171D) ||
            color == Color(0xFF141820) || color == Color(0xFF12151B) ||
            color == Color(0xFF161A22) || color == Color(0xFF181C23) ||
            color == Color(0xFF191D24) || color == Color(0xFF0F1216) ||
            color == Color(0xFF0F1217)
        ) {
            return Color(0xFFFFFFFF)
        }

        val lum = color.luminance()
        val channelDelta = maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue)
        if (lum < 0.16f && channelDelta > 0.045f) {
            // Tinted dark surfaces -> clean light tinted surfaces in Light Mode
            if (color.red > color.green * 1.35f && color.red > color.blue * 1.25f) {
                return Color(0xFFFEF2F2) // Light red/danger tint (#2B1618, #26191D, #261416)
            }
            if (color.green > color.red * 1.35f && color.green > color.blue * 1.2f) {
                return Color(0xFFECFDF5) // Light green/WhatsApp tint (#0F2618)
            }
            if (color.blue > color.red * 1.35f && color.blue > color.green * 1.15f) {
                return Color(0xFFEFF6FF) // Light blue/Telegram/Gemini tint (#0E2235, #1E2640, #1B2230)
            }
            if (color.red > color.blue * 1.35f && color.green > color.blue * 1.15f) {
                return Color(0xFFFFFBEB) // Light amber/warning tint (#1F1A14, #1A1712)
            }
        }

        return when {
            // Deep nested box or page background (#0A0B0E, #0D0F12, #121212, #0F1115, #101318, #121418, #12151A)
            lum < 0.0068f -> Color(0xFFF1F5F9)
            // Primary card / container / input surface (#16191E, #12151B, #141820, #181C23, #161A22, #191D24)
            lum < 0.0125f -> Color(0xFFFFFFFF)
            // Secondary chip / nested surface (#1C2028, #1E222B, #1E2430, #1E2532, #222630, #262A34, #282D37, #2E3440)
            lum < 0.16f -> Color(0xFFEEF2F8)
            else -> color
        }
    }

    fun adaptBorderStroke(border: BorderStroke?): BorderStroke? {
        if (AppPreferencesManager.isDarkMode || border == null) return border
        val brush = border.brush
        if (brush is androidx.compose.ui.graphics.SolidColor) {
            val c = brush.value
            if (!isVividAccentColor(c) && c.luminance() < 0.22f && c.alpha > 0.5f) {
                return BorderStroke(border.width, Color(0xFFD8E0EC))
            }
        }
        return border
    }

    fun adaptTextColor(color: Color, preserveWhite: Boolean): Color {
        if (AppPreferencesManager.isDarkMode) return color
        if (preserveWhite) return color
        if (color == Color.Unspecified) return Color(0xFF0F172A)

        val max = maxOf(color.red, color.green, color.blue)
        val min = minOf(color.red, color.green, color.blue)
        val delta = max - min

        // 1. Neutral text colors (white, near-white, silver, gray) -> high-contrast dark slate
        if (delta < 0.12f) {
            val lum = color.luminance()
            return when {
                // White or near-white primary text (#FFFFFF, #E2E8F0, #CBD5E1, #D0D5E0) -> Deep Slate
                lum > 0.55f -> Color(0xFF0F172A)
                // Secondary gray text (#A0A5B0, #94A3B8, #8C94A4, #888E9B, #8A92A0) -> Dark Slate
                lum > 0.20f -> Color(0xFF334155)
                // Muted / placeholder gray text (#6E7582, #7E8695, #505560) -> Medium-Dark Slate
                lum > 0.08f -> Color(0xFF475569)
                else -> color
            }
        }

        // 2. Colored/accent text: deepen light/pastel accents on white backgrounds for strong readability
        if (color.green > 0.7f && color.red < 0.45f) {
            return Color(0xFF15803D) // #4ADE80 -> Deep Green
        }
        if (color.blue > 0.8f && color.blue > color.red * 1.25f) {
            return Color(0xFF1D4ED8) // #93C5FD / #60A5FA -> Deep Blue
        }
        if (color.red > 0.9f && color.green > 0.45f && color.red > color.green * 1.2f && color.red > color.blue * 1.2f) {
            return Color(0xFFDC2626) // #FFCCC7 / #FF7875 -> Deep Red
        }
        if (color.red > 0.85f && color.green > 0.68f && color.blue < 0.35f) {
            return Color(0xFFB45309) // #FBBF24 -> Deep Amber
        }
        if (color == Color(0xFFFF9E40)) {
            return Color(0xFFEA580C) // Soft orange -> High-contrast orange
        }

        return color
    }
}

// ==============================================================================
// სრული მრავალენოვანი თარგმანის სისტემა (ქართული Default / English / Русский)
// ==============================================================================
object AppLocaleManager {
    private val exactTranslationsEn = mapOf(
        // Header & Navigation
        "სანდო სერვისი" to "Trusted Service",
        "მთავარი" to "Home",
        "მთავარი გვერდი" to "Home Page",
        "კატალოგი" to "Catalog",
        "კატალოგი & სერვისები" to "Catalog & Services",
        "დიაგნოსტიკა" to "Diagnostics",
        "ონლაინ დიაგნოსტიკა" to "Online Diagnostics",
        "AI ასისტენტი" to "AI Assistant",
        "პროფილი" to "Profile",
        "პროფილი & შეკვეთები" to "Profile & Orders",
        "კალათა" to "Cart",
        "შეტყობინებები" to "Notifications",
        "შეტყობინებების ცენტრი" to "Notifications Center",
        "მართვის პანელი" to "Admin Panel",
        "⚙️ მართვის პანელი" to "⚙️ Admin Panel",
        "უკან დაბრუნება" to "Go Back",
        "დახურვა" to "Close",
        "გაუქმება" to "Cancel",
        "შენახვა" to "Save",
        "წაშლა" to "Delete",
        "დამატება" to "Add",
        "+ დამატება" to "+ Add",
        "კოპირება" to "Copy",
        "არჩეულია" to "Selected",
        "ყველა" to "All",
        "წაუკითხავი" to "Unread",
        "წაკითხული" to "Read",
        "წაკითხულად მონიშვნა" to "Mark as Read",
        "წაუკითხავად დაბრუნება" to "Mark Unread",
        "ყველას წაკითხულად მონიშვნა" to "Mark All as Read",
        "გადასვლა" to "Open",
        "გახსნა / გადასვლა" to "Open / Navigate",
        "განმეორებით გაგზავნა" to "Resend",
        "🔄 განმეორებით გაგზავნა" to "🔄 Resend",
        "შესწორება" to "Edit",
        "✏️ შესწორება" to "✏️ Edit",
        "ტესტი აქვე" to "Test Here",
        "ყველასთან გაგზავნა" to "Send to All",
        "💾 განახლება & გაგზავნა" to "💾 Update & Send",
        "იგზავნება..." to "Sending...",

        // Categories & Filters
        "ეკრანები" to "Screens",
        "ელემენტები" to "Batteries",
        "დამტენი პორტები" to "Charging Ports",
        "კამერები" to "Cameras",
        "კორპუსები" to "Housings",
        "დინამიკები" to "Speakers",
        "პროგრამული სერვისები" to "Software Services",
        "💻 პროგრამული სერვისები" to "💻 Software Services",
        "ხელმისაწვდომია" to "In Stock",
        "ხელმისაწვდომია ✅" to "Available ✅",
        "მარაგშია" to "In Stock",
        "დროებით მიუწვდომელია" to "Out of Stock",
        "დროებით მიუწვდომელია ⏳" to "Unavailable ⏳",
        "მიუწვდომელია" to "Unavailable",
        "ფასი შეთანხმებით" to "Price on Request",
        "ფასი / აღწერა" to "Price / Info",

        // Home & Catalog
        "ძებნა: ChatGPT, Google AI, Canva, iPhone..." to "Search: ChatGPT, Google AI, Canva, iPhone...",
        "ძებნა: მოდელი, ნაწილი, AI სერვისი..." to "Search: model, part, AI service...",
        "პოპულარული ნაწილები და სერვისები" to "Popular Parts & Services",
        "ყველას ნახვა" to "See All",
        "კატეგორიები" to "Categories",
        "სწრაფი სერვისები" to "Quick Services",
        "WhatsApp შეკვეთა" to "WhatsApp Order",
        "Telegram შეკვეთა" to "Telegram Order",
        "აღწერა" to "Description",
        "სრული აღწერა" to "Full Description",
        "თავსებადი მოდელები" to "Compatible Models",
        "თავსებადი პლატფორმები და მოწყობილობები" to "Compatible Platforms & Devices",
        "გარანტია" to "Warranty",
        "BEKASERVICE გარანტია და მხარდაჭერა" to "BEKASERVICE Warranty & Support",
        "ხარისხის არჩევა:" to "Select Quality:",
        "სერვისის ტიპი / პაკეტი:" to "Service Type / Package:",
        "სტანდარტული" to "Standard",
        "ექსპრეს სერვისი" to "Express Service",
        "დისტანციური USB" to "Remote USB",

        // Profile & Auth & Banks
        "საქართველოს ბანკი" to "Bank of Georgia",
        "თიბისი ბანკი" to "TBC Bank",
        "ორივე ბანკი" to "Both Banks",
        "💳 საბანკო რეკვიზიტები" to "💳 Bank Details",
        "გადარიცხვისთვის დააკოპირეთ სასურველი ბანკის ანგარიში" to "Copy the preferred bank IBAN for transfer",
        "ანგარიშის ნომერი (IBAN):" to "Account Number (IBAN):",
        "📋 IBAN კოპირება" to "📋 Copy IBAN",
        "📋 საქართველოს ბანკის კოპირება" to "📋 Copy BOG IBAN",
        "📋 თიბისი ბანკის კოპირება" to "📋 Copy TBC IBAN",
        "📋 ორივე ანგარიშის ერთად კოპირება" to "📋 Copy Both Accounts",
        "არ არის მითითებული" to "Not specified",
        "შესვლა" to "Sign In",
        "რეგისტრაცია" to "Register",
        "გასვლა" to "Sign Out",
        "ჩემი შეკვეთები" to "My Orders",
        "ახალი" to "New",
        "მიმდინარე" to "In Progress",
        "მუშავდება" to "Processing",
        "დასრულებული" to "Completed",
        "შესრულებული" to "Completed",
        "გაუქმებული" to "Cancelled",

        // Admin Panel
        "ADMIN მართვის პანელი" to "ADMIN Control Panel",
        "BEKASERVICE • სრული კონტროლი" to "BEKASERVICE • Full Control",
        "პროდუქტები & სერვისები" to "Products & Services",
        "შეკვეთები" to "Orders",
        "მომხმარებლები" to "Users",
        "ბანერები (Home)" to "Banners (Home)",
        "ბანკები" to "Banks",
        "Push შეტყობინება" to "Push Notifications",
        "🔔 Push შეტყობინებების ცენტრი" to "🔔 Push Notifications Center",
        "ნებართვა ჩართულია ✅" to "Permission Enabled ✅",
        "ნებართვა გამორთულია ⚠️" to "Permission Disabled ⚠️",
        "ახალი შეტყობინების გაგზავნა" to "Send New Notification",
        "✏️ შეტყობინების რედაქტირება" to "✏️ Edit Notification",
        "📢 ზოგადი განცხადება" to "📢 Announcement",
        "🔥 პროდუქტის აქცია" to "🔥 Product Offer",
        "🛠️ სერვისის სიახლე" to "🛠️ Service Update",
        "სათაური (Title)" to "Title",
        "შეტყობინების ტექსტი" to "Notification Message",
        "გადასასვლელი გვერდი დაკლიკებისას:" to "Target Page on Click:",
        "კონკრეტული პროდუქტი / სერვისი (სურვილისამებრ):" to "Specific Product / Service (Optional):",
        "არცერთი (მხოლოდ გვერდზე გადასვლა)" to "None (Navigate to Page Only)",

        // Drawer & Theme & Language
        "ნავიგაციის მენიუ" to "Navigation Menu",
        "თემა (Dark / Light)" to "Theme (Dark / Light)",
        "მუქი რეჟიმი (Dark)" to "Dark Mode",
        "ნათელი რეჟიმი (Light)" to "Light Mode",
        "აპლიკაციის ენა" to "App Language",
        "სწრაფი კონტაქტი" to "Quick Contact",

        // Onboarding Flow
        "შენი ციფრული სერვისები" to "Your Digital Services",
        "მოგესალმები!" to "Welcome!",
        "ყველაფერი, რაც გჭირდება — ერთ სივრცეში." to "Everything you need — in one place.",
        "აირჩიეთ ენა" to "Select Language",
        "აირჩიეთ აპლიკაციის სასურველი ენა" to "Choose your preferred app language",
        "შემდეგი" to "Next",
        "შესვლა ანგარიშით" to "Sign in with Account",
        "აირჩიე შენთვის მოსახერხებელი გზა ან გააგრძელე ანგარიშის გარეშე." to "Choose a convenient way for you or continue without an account.",
        "ანგარიშით შესვლა საშუალებას გაძლევთ შეინახოთ ფავორიტები და აკონტროლოთ შეკვეთების ისტორია." to "Signing in lets you save favorites and track your order history.",
        "Google-ით შესვლა" to "Sign in with Google",
        "Apple-ით შესვლა" to "Sign in with Apple",
        "Apple ID-ით შესვლა" to "Sign in with Apple ID",
        "Apple ID ავტორიზაცია" to "Sign in with Apple ID",
        "Apple ID (ელ. ფოსტა)" to "Apple ID (Email)",
        "დამალე ჩემი ელ. ფოსტა (Private Relay)" to "Hide My Email (Private Relay)",
        "Apple ID ავტორიზაცია წარმატებულია!" to "Apple ID sign-in successful!",
        "გთხოვთ შეიყვანოთ სწორი Apple ID ელ. ფოსტა" to "Please enter a valid Apple ID email",
        "ელ. ფოსტით" to "With Email",
        "ელ. ფოსტით შესვლა" to "Sign in with Email",
        "ავტორიზაციის გამოტოვება" to "Skip authorization",
        "გაგრძელება" to "Continue",
        "გააგრძელეთ ანგარიშის გარეშე" to "Continue without an account",
        "შეგიძლიათ გამოიყენოთ აპლიკაცია ანგარიშის გარეშე — დაათვალიეროთ და შეუკვეთოთ სერვისები." to "You can use the app without an account — browse and order services freely.",
        "მზად ხარ!" to "You're Ready!",
        "გაეცანი ციფრულ სერვისებს BEKASERVICE-ში." to "Explore digital services in BEKASERVICE.",
        "მარტივი ნავიგაცია" to "Easy Navigation",
        "სწრაფი ძიება და კატეგორიები ერთ სივრცეში" to "Fast search and categories in one place",
        "სანდოობა" to "Reliability",
        "გარანტირებული ხარისხი და ოფიციალური სერვისი" to "Guaranteed quality and official service",
        "ციფრული სერვისები" to "Digital Services",
        "AI პაკეტები, პროგრამული უზრუნველყოფა და ტექნიკა" to "AI packages, software, and hardware parts",
        "დაიწყე →" to "Get Started →",
        "დაიწყე" to "Get Started",
        "აპლიკაციაში გადასვლა" to "Enter App",
        "მთავარ გვერდზე" to "Go to Home",
        "გამოტოვება" to "Skip",
        "დაბრუნება" to "Go Back",
        "თქვენ უკვე შესული ხართ სისტემაში" to "You are already signed in",
        "ავტორიზებული ანგარიში" to "Signed-in Account",
        "სხვა ანგარიშით შესვლა" to "Switch Account",
        "თქვენი სახელი და გვარი" to "Your Full Name",
        "ელ. ფოსტა" to "Email",
        "პაროლი" to "Password",
        "Google ავტორიზაცია გაუქმდა" to "Google sign-in was cancelled",
        "Google ავტორიზაცია წარმატებულია!" to "Google sign-in successful!",
        "Google ავტორიზაცია ვერ მოხერხდა" to "Google sign-in failed",
        "გთხოვთ შეიყვანოთ სწორი ელ. ფოსტა" to "Please enter a valid email address",
        "პაროლი უნდა შედგებოდეს მინიმუმ 6 სიმბოლოსგან" to "Password must be at least 6 characters",
        "პაროლი უნდა იყოს მინიმუმ 6 სიმბოლო" to "Password must be at least 6 characters",
        "გთხოვთ მიუთითოთ თქვენი სახელი" to "Please enter your name",
        "ტექნიკა" to "Hardware",
        "პროგრამული" to "Software",
        "ქლაუდი" to "Cloud",
        "AI სერვისები" to "AI Services",
        "დიზაინი" to "Design",
        "და სხვა..." to "And more..."
    )

    private val exactTranslationsRu = mapOf(
        // Header & Navigation
        "სანდო სერვისი" to "Надёжный сервис",
        "მთავარი" to "Главная",
        "მთავარი გვერდი" to "Главная страница",
        "კატალოგი" to "Каталог",
        "კატალოგი & სერვისები" to "Каталог и сервисы",
        "დიაგნოსტიკა" to "Диагностика",
        "ონლაინ დიაგნოსტიკა" to "Онлайн диагностика",
        "AI ასისტენტი" to "AI Ассистент",
        "პროფილი" to "Профиль",
        "პროფილი & შეკვეთები" to "Профиль и заказы",
        "კალათა" to "Корзина",
        "შეტყობინებები" to "Уведомления",
        "შეტყობინებების ცენტრი" to "Центр уведомлений",
        "მართვის პანელი" to "Панель управления",
        "⚙️ მართვის პანელი" to "⚙️ Панель админа",
        "უკან დაბრუნება" to "Назад",
        "დახურვა" to "Закрыть",
        "გაუქმება" to "Отмена",
        "შენახვა" to "Сохранить",
        "წაშლა" to "Удалить",
        "დამატება" to "Добавить",
        "+ დამატება" to "+ Добавить",
        "კოპირება" to "Копировать",
        "არჩეულია" to "Выбрано",
        "ყველა" to "Все",
        "წაუკითხავი" to "Непрочитанные",
        "წაკითხული" to "Прочитанные",
        "წაკითხულად მონიშვნა" to "Отметить прочитанным",
        "წაუკითხავად დაბრუნება" to "Отметить непрочитанным",
        "ყველას წაკითხულად მონიშვნა" to "Прочитать все",
        "გადასვლა" to "Открыть",
        "გახსნა / გადასვლა" to "Открыть / Перейти",
        "განმეორებით გაგზავნა" to "Отправить снова",
        "🔄 განმეორებით გაგზავნა" to "🔄 Повторить",
        "შესწორება" to "Изменить",
        "✏️ შესწორება" to "✏️ Изменить",
        "ტესტი აქვე" to "Тест здесь",
        "ყველასთან გაგზავნა" to "Отправить всем",
        "💾 განახლება & გაგზავნა" to "💾 Обновить и отправить",
        "იგზავნება..." to "Отправка...",

        // Categories & Filters
        "ეკრანები" to "Экраны",
        "ელემენტები" to "Батареи",
        "დამტენი პორტები" to "Порты зарядки",
        "კამერები" to "Камеры",
        "კორპუსები" to "Корпуса",
        "დინამიკები" to "Динамики",
        "პროგრამული სერვისები" to "Программные сервисы",
        "💻 პროგრამული სერვისები" to "💻 Программные сервисы",
        "ხელმისაწვდომია" to "В наличии",
        "ხელმისაწვდომია ✅" to "В наличии ✅",
        "მარაგშია" to "В наличии",
        "დროებით მიუწვდომელია" to "Нет в наличии",
        "დროებით მიუწვდომელია ⏳" to "Недоступно ⏳",
        "მიუწვდომელია" to "Недоступно",
        "ფასი შეთანხმებით" to "Цена договорная",
        "ფასი / აღწერა" to "Цена / Инфо",

        // Home & Catalog
        "ძებნა: ChatGPT, Google AI, Canva, iPhone..." to "Поиск: ChatGPT, Google AI, Canva, iPhone...",
        "ძებნა: მოდელი, ნაწილი, AI სერვისი..." to "Поиск: модель, деталь, AI сервис...",
        "პოპულარული ნაწილები და სერვისები" to "Популярные детали и сервисы",
        "ყველას ნახვა" to "Смотреть все",
        "კატეგორიები" to "Категории",
        "სწრაფი სერვისები" to "Быстрые сервисы",
        "WhatsApp შეკვეთა" to "Заказ WhatsApp",
        "Telegram შეკვეთა" to "Заказ Telegram",
        "აღწერა" to "Описание",
        "სრული აღწერა" to "Полное описание",
        "თავსებადი მოდელები" to "Совместимые модели",
        "თავსებადი პლატფორმები და მოწყობილობები" to "Совместимые платформы и устройства",
        "გარანტია" to "Гарантия",
        "BEKASERVICE გარანტია და მხარდაჭერა" to "Гарантия и поддержка BEKASERVICE",
        "ხარისხის არჩევა:" to "Выбор качества:",
        "სერვისის ტიპი / პაკეტი:" to "Тип сервиса / пакет:",
        "სტანდარტული" to "Стандартный",
        "ექსპრეს სერვისი" to "Экспресс сервис",
        "დისტანციური USB" to "Удалённый USB",

        // Profile & Auth & Banks
        "საქართველოს ბანკი" to "Банк Грузии",
        "თიბისი ბანკი" to "TBC Банк",
        "ორივე ბანკი" to "Оба банка",
        "💳 საბანკო რეკვიზიტები" to "💳 Банковские реквизиты",
        "გადარიცხვისთვის დააკოპირეთ სასურველი ბანკის ანგარიში" to "Скопируйте счёт нужного банка для перевода",
        "ანგარიშის ნომერი (IBAN):" to "Номер счёта (IBAN):",
        "📋 IBAN კოპირება" to "📋 Копировать IBAN",
        "📋 საქართველოს ბანკის კოპირება" to "📋 Копировать BOG IBAN",
        "📋 თიბისი ბანკის კოპირება" to "📋 Копировать TBC IBAN",
        "📋 ორივე ანგარიშის ერთად კოპირება" to "📋 Копировать оба счёта",
        "არ არის მითითებული" to "Не указано",
        "შესვლა" to "Войти",
        "რეგისტრაცია" to "Регистрация",
        "გასვლა" to "Выйти",
        "ჩემი შეკვეთები" to "Мои заказы",
        "ახალი" to "Новый",
        "მიმდინარე" to "В процессе",
        "მუშავდება" to "В обработке",
        "დასრულებული" to "Завершён",
        "შესრულებული" to "Выполнен",
        "გაუქმებული" to "Отменён",

        // Admin Panel
        "ADMIN მართვის პანელი" to "ADMIN Панель управления",
        "BEKASERVICE • სრული კონტროლი" to "BEKASERVICE • Полный контроль",
        "პროდუქტები & სერვისები" to "Товары и сервисы",
        "შეკვეთები" to "Заказы",
        "მომხმარებლები" to "Пользователи",
        "ბანერები (Home)" to "Баннеры (Home)",
        "ბანკები" to "Банки",
        "Push შეტყობინება" to "Push уведомления",
        "🔔 Push შეტყობინებების ცენტრი" to "🔔 Центр Push уведомлений",
        "ნებართვა ჩართულია ✅" to "Разрешение включено ✅",
        "ნებართვა გამორთულია ⚠️" to "Разрешение выключено ⚠️",
        "ახალი შეტყობინების გაგზავნა" to "Отправить новое уведомление",
        "✏️ შეტყობინების რედაქტირება" to "✏️ Редактирование уведомления",
        "📢 ზოგადი განცხადება" to "📢 Объявление",
        "🔥 პროდუქტის აქცია" to "🔥 Акция на товар",
        "🛠️ სერვისის სიახლე" to "🛠️ Новость сервиса",
        "სათაური (Title)" to "Заголовок (Title)",
        "შეტყობინების ტექსტი" to "Текст уведомления",
        "გადასასვლელი გვერდი დაკლიკებისას:" to "Страница перехода при нажатии:",
        "კონკრეტული პროდუქტი / სერვისი (სურვილისამებრ):" to "Конкретный товар / сервис (необязательно):",
        "არცერთი (მხოლოდ გვერდზე გადასვლა)" to "Нет (только переход на страницу)",

        // Drawer & Theme & Language
        "ნავიგაციის მენიუ" to "Меню навигации",
        "თემა (Dark / Light)" to "Тема (Тёмная / Светлая)",
        "მუქი რეჟიმი (Dark)" to "Тёмная тема",
        "ნათელი რეჟიმი (Light)" to "Светлая тема",
        "აპლიკაციის ენა" to "Язык приложения",
        "სწრაფი კონტაქტი" to "Быстрый контакт",

        // Onboarding Flow
        "შენი ციფრული სერვისები" to "Ваши цифровые сервисы",
        "მოგესალმები!" to "Добро пожаловать!",
        "ყველაფერი, რაც გჭირდება — ერთ სივრცეში." to "Всё, что вам нужно — в одном месте.",
        "აირჩიეთ ენა" to "Выберите язык",
        "აირჩიეთ აპლიკაციის სასურველი ენა" to "Выберите предпочитаемый язык приложения",
        "შემდეგი" to "Далее",
        "შესვლა ანგარიშით" to "Вход в аккаунт",
        "აირჩიე შენთვის მოსახერხებელი გზა ან გააგრძელე ანგარიშის გარეშე." to "Выберите удобный способ или продолжите без аккаунта.",
        "ანგარიშით შესვლა საშუალებას გაძლევთ შეინახოთ ფავორიტები და აკონტროლოთ შეკვეთების ისტორია." to "Вход в аккаунт позволяет сохранять избранное и отслеживать историю заказов.",
        "Google-ით შესვლა" to "Войти через Google",
        "Apple-ით შესვლა" to "Войти через Apple",
        "Apple ID-ით შესვლა" to "Войти через Apple ID",
        "Apple ID ავტორიზაცია" to "Вход через Apple ID",
        "Apple ID (ელ. ფოსტა)" to "Apple ID (Эл. почта)",
        "დამალე ჩემი ელ. ფოსტა (Private Relay)" to "Скрыть мой email (Private Relay)",
        "Apple ID ავტორიზაცია წარმატებულია!" to "Вход через Apple ID выполнен успешно!",
        "გთხოვთ შეიყვანოთ სწორი Apple ID ელ. ფოსტა" to "Пожалуйста, введите корректный email Apple ID",
        "ელ. ფოსტით" to "По эл. почте",
        "ელ. ფოსტით შესვლა" to "Вход по эл. почте",
        "ავტორიზაციის გამოტოვება" to "Пропустить авторизацию",
        "გაგრძელება" to "Продолжить",
        "გააგრძელეთ ანგარიშის გარეშე" to "Продолжить без аккаунта",
        "შეგიძლიათ გამოიყენოთ აპლიკაცია ანგარიშის გარეშე — დაათვალიეროთ და შეუკვეთოთ სერვისები." to "Вы можете пользоваться приложением без аккаунта — просматривать и заказывать сервисы.",
        "მზად ხარ!" to "Всё готово!",
        "გაეცანი ციფრულ სერვისებს BEKASERVICE-ში." to "Откройте цифровые сервисы в BEKASERVICE.",
        "მარტივი ნავიგაცია" to "Простая навигация",
        "სწრაფი ძიება და კატეგორიები ერთ სივრცეში" to "Быстрый поиск и категории в одном месте",
        "სანდოობა" to "Надёжность",
        "გარანტირებული ხარისხი და ოფიციალური სერვისი" to "Гарантированное качество и официальный сервис",
        "ციფრული სერვისები" to "Цифровые сервисы",
        "AI პაკეტები, პროგრამული უზრუნველყოფა და ტექნიკა" to "AI пакеты, программы и комплектующие",
        "დაიწყე →" to "Начать →",
        "დაიწყე" to "Начать",
        "აპლიკაციაში გადასვლა" to "Перейти в приложение",
        "მთავარ გვერდზე" to "На главную",
        "გამოტოვება" to "Пропустить",
        "დაბრუნება" to "Назад",
        "თქვენ უკვე შესული ხართ სისტემაში" to "Вы уже вошли в систему",
        "ავტორიზებული ანგარიში" to "Авторизованный аккаунт",
        "სხვა ანგარიშით შესვლა" to "Войти в другой аккаунт",
        "თქვენი სახელი და გვარი" to "Ваше имя и фамилия",
        "ელ. ფოსტა" to "Эл. почта",
        "პაროლი" to "Пароль",
        "Google ავტორიზაცია გაუქმდა" to "Вход через Google отменён",
        "Google ავტორიზაცია წარმატებულია!" to "Вход через Google выполнен успешно!",
        "Google ავტორიზაცია ვერ მოხერხდა" to "Не удалось войти через Google",
        "გთხოვთ შეიყვანოთ სწორი ელ. ფოსტა" to "Пожалуйста, введите корректный email",
        "პაროლი უნდა შედგებოდეს მინიმუმ 6 სიმბოლოსგან" to "Пароль должен содержать минимум 6 символов",
        "პაროლი უნდა იყოს მინიმუმ 6 სიმბოლო" to "Пароль должен содержать минимум 6 символов",
        "გთხოვთ მიუთითოთ თქვენი სახელი" to "Пожалуйста, укажите ваше имя",
        "ტექნიკა" to "Техника",
        "პროგრამული" to "Программы",
        "ქლაუდი" to "Облако",
        "AI სერვისები" to "AI Сервисы",
        "დიზაინი" to "Дизайн",
        "და სხვა..." to "И другое..."
    )

    private val phraseReplacementsEn = listOf(
        "შეკვეთა / კალათაში დამატება" to "Order / Add to Cart",
        "კალათაში დამატება" to "Add to Cart",
        "გაგზავნილი შეტყობინებების ისტორია" to "Sent Notifications History",
        "მიმღები:" to "Recipient:",
        "ბრენდი:" to "Brand:",
        "თარიღი:" to "Date:",
        "სტატუსი:" to "Status:",
        "გადახდა:" to "Payment:",
        "თრექინგ კოდი:" to "Tracking Code:",
        "ჯამური თანხა:" to "Total Amount:",
        "სულ:" to "Total:",
        "პოზიცია" to "items",
        "პროდუქტის ID:" to "Product ID:",
        "გვერდი:" to "Page:",
        "Cloud სინქრონიზაცია" to "Cloud Sync",
        "რეგისტრირებული მომხმარებლები" to "Registered Users",
        "მთავარი გვერდის ბანერები" to "Home Screen Banners",
        "ხელოვნური ინტელექტი" to "Artificial Intelligence",
        "პროგრამული სერვისები" to "Software Services",
        "ეკრანები" to "Screens",
        "ელემენტები" to "Batteries",
        "დამტენი პორტები" to "Charging Ports",
        "კამერები" to "Cameras",
        "კორპუსები" to "Housings",
        "დინამიკები" to "Speakers",
        "ფასი შეთანხმებით" to "Price on Request",
        "ხელმისაწვდომია" to "Available",
        "დროებით მიუწვდომელია" to "Out of Stock",
        "თვე" to "months",
        "წელი" to "year",
        "დღე" to "days",
        "საათი" to "hours",
        "წუთი" to "min"
    )

    private val phraseReplacementsRu = listOf(
        "შეკვეთა / კალათაში დამატება" to "Заказать / В корзину",
        "კალათაში დამატება" to "В корзину",
        "გაგზავნილი შეტყობინებების ისტორია" to "История отправленных уведомлений",
        "მიმღები:" to "Получатель:",
        "ბრენდი:" to "Бренд:",
        "თარიღი:" to "Дата:",
        "სტატუსი:" to "Статус:",
        "გადახდა:" to "Оплата:",
        "თრექინგ კოდი:" to "Трек-код:",
        "ჯამური თანხა:" to "Итоговая сумма:",
        "სულ:" to "Всего:",
        "პოზიცია" to "поз.",
        "პროდუქტის ID:" to "ID товара:",
        "გვერდი:" to "Страница:",
        "Cloud სინქრონიზაცია" to "Cloud синхронизация",
        "რეგისტრირებული მომხმარებლები" to "Зарегистрированные пользователи",
        "მთავარი გვერდის ბანერები" to "Баннеры главной страницы",
        "ხელოვნური ინტელექტი" to "Искусственный интеллект",
        "პროგრამული სერვისები" to "Программные сервисы",
        "ეკრანები" to "Экраны",
        "ელემენტები" to "Батареи",
        "დამტენი პორტები" to "Порты зарядки",
        "კამერები" to "Камеры",
        "კორპუსები" to "Корпуса",
        "დინამიკები" to "Динамики",
        "ფასი შეთანხმებით" to "Цена договорная",
        "ხელმისაწვდომია" to "В наличии",
        "დროებით მიუწვდომელია" to "Нет в наличии",
        "თვე" to "мес.",
        "წელი" to "г.",
        "დღე" to "дн.",
        "საათი" to "ч.",
        "წუთი" to "мин."
    )

    fun translate(raw: String, lang: AppLanguage = AppPreferencesManager.currentLanguage): String {
        if (lang == AppLanguage.KA || raw.isBlank()) return raw
        val trimmed = raw.trim()
        when (lang) {
            AppLanguage.EN -> {
                exactTranslationsEn[trimmed]?.let { return it }
                var result = raw
                for ((ka, en) in phraseReplacementsEn) {
                    if (result.contains(ka)) {
                        result = result.replace(ka, en)
                    }
                }
                return result
            }
            AppLanguage.RU -> {
                exactTranslationsRu[trimmed]?.let { return it }
                var result = raw
                for ((ka, ru) in phraseReplacementsRu) {
                    if (result.contains(ka)) {
                        result = result.replace(ka, ru)
                    }
                }
                return result
            }
            else -> return raw
        }
    }

    fun tr(ka: String, en: String, ru: String): String {
        return when (AppPreferencesManager.currentLanguage) {
            AppLanguage.KA -> ka
            AppLanguage.EN -> en
            AppLanguage.RU -> ru
        }
    }
}

// ==============================================================================
// Package-level Material3 Wrappers for Automatic Theme (Dark/Light) & Locale (KA/EN/RU)
// ==============================================================================
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = Color(0xFF16191E),
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val adaptedColor = AppThemeColorAdapter.adaptSurfaceColor(color)
    val adaptedBorder = AppThemeColorAdapter.adaptBorderStroke(border)
    val preserveWhite = AppThemeColorAdapter.isVividAccentColor(color) ||
        (color.alpha in 0.45f..0.85f && color.luminance() < 0.08f)

    CompositionLocalProvider(LocalPreserveWhiteText provides preserveWhite) {
        androidx.compose.material3.Surface(
            modifier = modifier,
            shape = shape,
            color = adaptedColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = adaptedBorder,
            content = content
        )
    }
}

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = Color(0xFF16191E),
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val adaptedColor = AppThemeColorAdapter.adaptSurfaceColor(color)
    val adaptedBorder = AppThemeColorAdapter.adaptBorderStroke(border)
    val preserveWhite = AppThemeColorAdapter.isVividAccentColor(color) ||
        (color.alpha in 0.45f..0.85f && color.luminance() < 0.08f)

    CompositionLocalProvider(LocalPreserveWhiteText provides preserveWhite) {
        androidx.compose.material3.Surface(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            color = adaptedColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = adaptedBorder,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val translatedText = AppLocaleManager.translate(text, AppPreferencesManager.currentLanguage)
    val localContent = LocalContentColor.current
    val isInsideOrangeOrAccentButton = localContent == Color.White &&
        color == Color.White &&
        LocalPreserveWhiteText.current
    val adaptedColor = AppThemeColorAdapter.adaptTextColor(
        color = color,
        preserveWhite = LocalPreserveWhiteText.current || isInsideOrangeOrAccentButton
    )

    androidx.compose.material3.Text(
        text = translatedText,
        modifier = modifier,
        color = adaptedColor,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    val finalColors = if (AppPreferencesManager.isDarkMode) {
        colors
    } else {
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF0F172A),
            unfocusedTextColor = Color(0xFF0F172A),
            focusedContainerColor = Color(0xFFFFFFFF),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedBorderColor = Color(0xFFFF6A00),
            unfocusedBorderColor = Color(0xFFCBD5E1),
            focusedLabelColor = Color(0xFFFF6A00),
            unfocusedLabelColor = Color(0xFF475569),
            cursorColor = Color(0xFFFF6A00)
        )
    }

    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
        shape = shape,
        colors = finalColors
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalPreserveWhiteText provides true) {
        androidx.compose.material3.Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            contentPadding = contentPadding,
            interactionSource = interactionSource,
            content = content
        )
    }
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color
) {
    val adaptedColor = if (AppPreferencesManager.isDarkMode) {
        color
    } else if (!AppThemeColorAdapter.isVividAccentColor(color) && color.luminance() < 0.25f) {
        Color(0xFFE2E8F0)
    } else {
        color
    }
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = adaptedColor
    )
}

