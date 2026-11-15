package com.example

import androidx.compose.ui.graphics.Color

/**
 * BSTORE — ცენტრალიზებული ბრენდინგისა და იდენტობის კონფიგურაცია (Centralized Branding Architecture).
 *
 * უზრუნველყოფს აპლიკაციის სრულ დამოუკიდებლობას კოდში გაბნეული ბრენდული სახელწოდებებისგან,
 * ბიზნეს ლოგიკის, Firebase არქიტექტურისა და ეკრანების გადაწერის გარეშე.
 */
object AppBrandConfig {
    // 1. ძირითადი სახელწოდება
    const val BRAND_NAME = "BSTORE"
    const val BRAND_NAME_KA = "BSTORE"
    const val APP_NAME_EN = "BSTORE"
    const val APP_NAME_KA = "BSTORE"
    const val STORE_LISTING_NAME = "BSTORE"
    const val TAGLINE_KA = "მობილური ტექნიკა, ეკრანები, ელემენტები და აქსესუარები"
    const val TAGLINE_EN = "Mobile Parts, Batteries & Device Accessories"

    // 2. საკონტაქტო და მხარდაჭერის რეკვიზიტები
    const val SUPPORT_PHONE = "+995 598 23 33 32"
    const val SUPPORT_PHONE_DIAL = "tel:+995598233332"
    const val SUPPORT_WHATSAPP = "https://wa.me/995598233332"
    const val SUPPORT_EMAIL = "beqa1994@gmail.com"
    const val SERVICE_CENTER_ADDRESS_KA = "საქართველო, ქ. ახალციხე, კოსტავას ქ. N10"
    const val PHYSICAL_ADDRESS_KA = "ახალციხე, კოსტავას ქ. N10"
    const val WORKING_HOURS_KA = "ორშაბათი - შაბათი: 10:00 - 19:00"

    // 3. სოციალური ქსელები და ბმულები
    const val FACEBOOK_URL = "https://www.facebook.com"
    const val INSTAGRAM_URL = "https://www.instagram.com"
    const val TELEGRAM_CHANNEL = "https://t.me/bekaservice"
    const val TELEGRAM_URL = "https://t.me/bekaservice"

    // 4. ბრენდის ძირითადი ფერები (BSTORE Dark/Orange Visual Identity)
    val PrimaryAccent = Color(0xFFFF6B00)
    val SecondaryAccent = Color(0xFFFF8524)
    val BackgroundDark = Color(0xFF0A0A0A)
    val SurfaceDark = Color(0xFF14161A)
    val SurfaceElevatedDark = Color(0xFF1E2229)

    // 5. Google Play Privacy & Legal პოლიტიკის URL-ები
    const val PRIVACY_POLICY_URL = "https://bekaservice.ge/privacy"
    const val TERMS_OF_SERVICE_URL = "https://bekaservice.ge/terms"
    const val ACCOUNT_DELETION_URL = "https://bekaservice.ge/delete-account"
}
