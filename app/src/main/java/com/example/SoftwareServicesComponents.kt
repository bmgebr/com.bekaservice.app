package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==============================================================================
// 1. მონაცემთა სტრუქტურები პროგრამული სერვისების დეტალური ჩამონათვალისთვის
// ==============================================================================
data class SoftwareBrandItem(
    val brandName: String,
    val iconEmoji: String,
    val supportedModels: String,
    val method: String,
    val estimatedTime: String,
    val price: String,
    val details: String,
    val requirements: String
)

data class DiagnosticAiReport(
    val title: String,
    val issueCategory: String, // "პროგრამული (Software)" / "აპარატურული (Hardware)"
    val confidence: String, // e.g. "98.4% სიზუსტე"
    val estimatedPrice: String,
    val estimatedTime: String,
    val primaryCause: String,
    val technicalSteps: List<String>,
    val recommendation: String,
    val isSoftware: Boolean
)

// ==============================================================================
// 2. პროგრამული სერვისების დეტალური ჩამონათვალი (FRP, iCloud, Unlock და სხვა)
// ==============================================================================
object SoftwareServicesData {

    fun getFrpBrandList(): List<SoftwareBrandItem> = listOf(
        SoftwareBrandItem(
            brandName = "Samsung (Galaxy S, Z, A სერიები)",
            iconEmoji = "📱",
            supportedModels = "Galaxy S24/S23/S22/S21, Z Fold/Flip, A54/A34/A15 და ყველა მოდელი",
            method = "MTP *#0*# Code / Knox Enrollment QR / EDL 9008 / TestPoint",
            estimatedTime = "10-15 წუთი",
            price = "45 ₾",
            details = "Google Account (FRP)-ის სრული გვერდის ავლით Android 9-დან Android 15-მდე (One UI 1.0 - 7.0). Knox Guard-ის დაუზიანებლად და ოფიციალური გადატვირთვით.",
            requirements = "Type-C კაბელი, მოწყობილობა ჩართულ მდგომარეობაში (Start Screen)"
        ),
        SoftwareBrandItem(
            brandName = "Xiaomi / Redmi / POCO",
            iconEmoji = "⚡",
            supportedModels = "Xiaomi 14/13/12, Redmi Note 13/12/11, POCO X6/F5 და ყველა მოდელი",
            method = "Fastboot Sideload Assistant / EDL 9008 Auth / Recovery",
            estimatedTime = "15-20 წუთი",
            price = "45 ₾",
            details = "HyperOS 1.0/2.0 და MIUI 12-14 FRP ბლოკის მოხსნა. ასევე შესაძლებელია Clean სტატუსის Mi Cloud ანგარიშის სრული დეაქტივაცია.",
            requirements = "Fastboot რეჟიმში გადაყვანა (Volume Down + Power)"
        ),
        SoftwareBrandItem(
            brandName = "Motorola (Moto G, Edge, Razr)",
            iconEmoji = "Ⓜ️",
            supportedModels = "Moto Edge 40/50, Moto G84/G54/G14, Razr 40/50 Ultra",
            method = "Fastboot Factory Mode / ADB Direct Exploit",
            estimatedTime = "10-15 წუთი",
            price = "40 ₾",
            details = "უახლესი 2024-2025 უსაფრთხოების პატჩების გვერდის ავლით, Setup Wizard-ის სრული გასუფთავება და მოწყობილობის მყისიერი ჩართვა.",
            requirements = "Fastboot / Bootloader რეჟიმი"
        ),
        SoftwareBrandItem(
            brandName = "Google Pixel (Pixel 6 - 9 Pro)",
            iconEmoji = "🌐",
            supportedModels = "Pixel 9 Pro, 8 Pro, 7a, 7 Pro, 6 Pro, Pixel Fold",
            method = "Google Tensor Security Bypass / MTP Setup Exploit",
            estimatedTime = "15-25 წუთი",
            price = "50 ₾",
            details = "Google Titan M2 უსაფრთხოების ჩიპის გვერდის ავლით უახლეს Android 14/15 ვერსიებზე. მოწყობილობა რჩება სრულად ფუნქციური განახლებების მხარდაჭერით.",
            requirements = "Wi-Fi კავშირი საწყის ეკრანზე"
        ),
        SoftwareBrandItem(
            brandName = "Huawei & Honor",
            iconEmoji = "💠",
            supportedModels = "P60/P50/Mate 50, Honor 90/Magic 6 Pro/X9b",
            method = "USB COM 1.0 / TestPoint / Kirin & Snapdragon Tool",
            estimatedTime = "20 წუთი",
            price = "45 ₾",
            details = "Huawei ID და Google FRP ბლოკის მოხსნა EMUI 12/13 და MagicOS სისტემებზე.",
            requirements = "TestPoint წერტილის დამოკლება დედაპლატაზე ან USB COM რეჟიმი"
        ),
        SoftwareBrandItem(
            brandName = "MediaTek (MTK Universal)",
            iconEmoji = "⚙️",
            supportedModels = "ნებისმიერი ბრენდი MediaTek Helio & Dimensity პროცესორით",
            method = "BROM Mode Exploit / Preloader Auth Bypass (1-Click)",
            estimatedTime = "5-10 წუთი",
            price = "35 ₾",
            details = "მყისიერი 1-Click განბლოკვა. ბლოკავს FRP partition-ს და ასუფთავებს Google-ის ტოკენებს აპარატურულ დონეზე.",
            requirements = "ტელეფონის გამორთვა და შეერთება ღილაკების კომბინაციით"
        ),
        SoftwareBrandItem(
            brandName = "Qualcomm (Universal EDL 9008)",
            iconEmoji = "🔌",
            supportedModels = "Qualcomm Snapdragon 8 Gen 1/2/3, 7/6 სერიების ყველა სმარტფონი",
            method = "Emergency Download (EDL 9008) Firehose Loader",
            estimatedTime = "10-15 წუთი",
            price = "45 ₾",
            details = "EDL რეჟიმში პირდაპირი წვდომა eMMC/UFS მეხსიერებასთან და Persist/FRP სექტორის გასუფთავება.",
            requirements = "EDL კაბელი ან TestPoint"
        )
    )

    fun getICloudBrandList(): List<SoftwareBrandItem> = listOf(
        SoftwareBrandItem(
            brandName = "Activation Lock (Hello Screen + Signal)",
            iconEmoji = "🍏",
            supportedModels = "iPhone 6s, 7, 8, X, iPad 5-9, iPad Pro/Air",
            method = "Checkm8 BootROM Exploit + Full Baseband Activation",
            estimatedTime = "20-30 წუთი",
            price = "120 ₾",
            details = "სრული მუშა მდგომარეობა: ზარები, SMS, 4G/LTE ინტერნეტი, FaceTime, iMessage, App Store და iCloud სერვისები.",
            requirements = "ტელეფონი Hello Screen-ზე, IMEI Clean"
        ),
        SoftwareBrandItem(
            brandName = "Passcode / Disabled Screen Backup",
            iconEmoji = "🔒",
            supportedModels = "iPhone 6s - iPhone 14 Pro Max",
            method = "Token Backup & Clean Restore",
            estimatedTime = "25 წუთი",
            price = "110 ₾",
            details = "თუ ტელეფონს ეკრანის კოდი დაავიწყდა ან დაბლოკილია წუთებით - ორიგინალი აქტივაციის სერტიფიკატის ამოღება და ჩაწერა.",
            requirements = "ტელეფონი არ უნდა იყოს გადატვირთული ქარხნულად"
        ),
        SoftwareBrandItem(
            brandName = "Open Menu FMI OFF (სამუდამო მოხსნა)",
            iconEmoji = "🔓",
            supportedModels = "ყველა iPhone (iPhone 6-დან iPhone 16 Pro Max-მდე)",
            method = "Apple Official Server Token Proxy Removal",
            estimatedTime = "15 წუთი",
            price = "90 ₾",
            details = "როცა ტელეფონი მენიუში შედის, მაგრამ Apple ID-ის პაროლი დავიწყებულია. მოხსნა ხდება პირდაპირ Apple-ის ოფიციალური სერვერიდან სამუდამოდ.",
            requirements = "iCloud საცავში (Storage) შესვლის შესაძლებლობა"
        ),
        SoftwareBrandItem(
            brandName = "Apple Watch & iPad iCloud",
            iconEmoji = "⌚",
            supportedModels = "Apple Watch Series 3 - Ultra 2, iPad Mini/Air/Pro",
            method = "Hardware ID Deregister / DFU Bypass",
            estimatedTime = "30-45 წუთი",
            price = "80 ₾",
            details = "ჭკვიანი საათებისა და პლანშეტების სრული გათავისუფლება ძველი მფლობელის ანგარიშისგან.",
            requirements = "დატენილი ბატარეა"
        )
    )

    fun getNetworkUnlockList(): List<SoftwareBrandItem> = listOf(
        SoftwareBrandItem(
            brandName = "აშშ-ის ოპერატორები (USA Carriers)",
            iconEmoji = "🇺🇸",
            supportedModels = "AT&T, T-Mobile, Verizon, Sprint, Cricket, MetroPCS",
            method = "Official Carrier Server Database Factory Unlock",
            estimatedTime = "24-48 სთ / 1-Click",
            price = "65 ₾",
            details = "ოფიციალური Factory Unlock. ტელეფონი სამუდამოდ გათავისუფლდება SIM ბლოკისგან და იმუშავებს მსოფლიოს ნებისმიერ ქსელში.",
            requirements = "15-ნიშნა IMEI კოდი (*#06#)"
        ),
        SoftwareBrandItem(
            brandName = "იაპონიის ოპერატორები (Japan Carriers)",
            iconEmoji = "🇯🇵",
            supportedModels = "NTT Docomo, SoftBank, AU KDDI",
            method = "IMEI Server Whitelist Unlock",
            estimatedTime = "1-2 სამუშაო დღე",
            price = "70 ₾",
            details = "იაპონური iPhone და Android მოდელების სრული ქსელური განბლოკვა კამერის ჩამკეტის ხმის გამორთვის მხარდაჭერით.",
            requirements = "IMEI კოდი"
        ),
        SoftwareBrandItem(
            brandName = "ევროპის ოპერატორები (Europe Carriers)",
            iconEmoji = "🇪🇺",
            supportedModels = "Vodafone, O2, EE UK, Orange, Deutsche Telekom",
            method = "Direct Factory Whitelist",
            estimatedTime = "12-24 სთ",
            price = "60 ₾",
            details = "ევროპული ქვეყნებიდან ჩამოტანილი მოწყობილობების განბლოკვა ქართული სიმ-ბარათებისთვის (Magti, Silknet, Cellfie).",
            requirements = "IMEI კოდი"
        )
    )

    fun getAuthFlashingList(): List<SoftwareBrandItem> = listOf(
        SoftwareBrandItem(
            brandName = "Xiaomi Authorized Server EDL Flashing",
            iconEmoji = "⚡",
            supportedModels = "Xiaomi, Redmi, POCO (Qualcomm Snapdragon & MTK)",
            method = "Xiaomi Official Service Account Token (Firehose Auth)",
            estimatedTime = "15 წუთი",
            price = "55 ₾",
            details = "სისტემურად აგურებული (Hard-Bricked / 9008 Mode) ტელეფონების აღდგენა ოფიციალური სერვერული ავტორიზაციით.",
            requirements = "EDL რეჟიმი (TestPoint)"
        ),
        SoftwareBrandItem(
            brandName = "Oppo / Realme MSM Download Tool Auth",
            iconEmoji = "🟢",
            supportedModels = "Realme GT/Number სერიები, Oppo Find/Reno",
            method = "Oppo Official Customer Service Login Auth",
            estimatedTime = "20 წუთი",
            price = "55 ₾",
            details = "სრული ოფიციალური firmware-ის ჩაწერა და DM-Verity კორუფციის გასწორება.",
            requirements = "Qualcomm 9008 Driver"
        ),
        SoftwareBrandItem(
            brandName = "OnePlus CrashDump & MSM Unbrick",
            iconEmoji = "🔴",
            supportedModels = "OnePlus 12/11/10/9/8/Nord",
            method = "MSM Download Tool Factory Firmware",
            estimatedTime = "20 წუთი",
            price = "50 ₾",
            details = "Qualcomm CrashDump Mode-დან და გაჭედილი Fastboot-იდან ტელეფონის ქარხნულ მდგომარეობაში დაბრუნება.",
            requirements = "Vol Up + Vol Down შეერთება"
        )
    )
}

// ==============================================================================
// 3. პროგრამული სერვისების დეტალური ჩამონათვალის UI (ProductDetail-ში ჩასაშენებლად)
// ==============================================================================
private fun formatVerifiedDateBadge(rawDate: String): String {
    val trimmed = rawDate.trim()
    if (trimmed.isBlank()) {
        return java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.US).format(java.util.Date())
    }
    // Convert YYYY-MM-DD (e.g. 2026-10-25) -> DD.MM.YYYY (25.10.2026) so it is compact and readable
    val isoRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
    val match = isoRegex.matchEntire(trimmed)
    if (match != null) {
        val (year, month, day) = match.destructured
        return "$day.$month.$year"
    }
    return trimmed
}

@Composable
fun SoftwareServiceDetailSection(product: ProductItem) {
    val context = LocalContext.current
    val isDark = AppPreferencesManager.isDarkMode
    val formattedVerifiedDate = remember(product.lastVerifiedAt) {
        formatVerifiedDateBadge(product.lastVerifiedAt)
    }

    if (SoftwareCatalogData.isDigitalSoftwareService(product)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF14171D),
            border = BorderStroke(1.dp, Color(0xFF282F3B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: ოფიციალური სპეციფიკაცია და გადამოწმების თარიღი (ერთ ხაზზე, გადატანის გარეშე)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFFFF7A00),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "სერვისის სრული პარამეტრები",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF182420) else Color(0xFFDCFCE7),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF234233) else Color(0xFF86EFAC)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "განახლდა: $formattedVerifiedDate",
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                                fontSize = 9.5.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // გეგმა, ხანგრძლივობა და ანგარიშის ტიპი
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF101318),
                    border = BorderStroke(1.dp, Color(0xFF222834)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (product.subcategory.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("კატეგორია:", color = Color(0xFFA0A5B0), fontSize = 11.5.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = product.subcategory,
                                    color = Color(0xFFFF7A00),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (product.offerType.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("გეგმის ტიპი:", color = Color(0xFFA0A5B0), fontSize = 11.5.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = product.offerType,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (product.duration.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("ხანგრძლივობა:", color = Color(0xFFA0A5B0), fontSize = 11.5.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = product.duration,
                                    color = Color(0xFF4ADE80),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (product.accountType.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("ანგარიშის / ლიცენზიის ტიპი:", color = Color(0xFFA0A5B0), fontSize = 11.5.sp)
                                Text(product.accountType, color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ძირითადი შესაძლებლობები და უპირატესობები
                if (product.features.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "✨ ძირითადი შესაძლებლობები და უპირატესობები:",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        product.features.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("•", color = Color(0xFFFF7A00), fontSize = 14.sp, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = feat,
                                    color = Color(0xFFD5DAE2),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // ვის გამოადგება
                if (product.targetAudience.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF181D26),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "🎯 ვის გამოადგება:",
                                color = Color(0xFFFF7A00),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = product.targetAudience,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // აქტივაციის და უსაფრთხოების პირობები
                if (product.activationTerms.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1A1712),
                        border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "🛡️ აქტივაციისა და შეძენის პირობები (BEKASERVICE):",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = product.activationTerms,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // ოფიციალური ბმულები
                if (product.officialUrl.isNotBlank() || product.termsUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (product.officialUrl.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1B2230),
                                border = BorderStroke(1.dp, Color(0xFF2E3A4E)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(product.officialUrl)))
                                        } catch (_: Exception) {}
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "ოფიციალური გვერდი",
                                        color = Color(0xFF93C5FD),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (product.termsUrl.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1B2230),
                                border = BorderStroke(1.dp, Color(0xFF2E3A4E)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(product.termsUrl)))
                                        } catch (_: Exception) {}
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Policy, contentDescription = null, tint = Color(0xFFA0A5B0), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "ოფიციალური წესები",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    val titleLower = product.title.lowercase()

    val brandItems: List<SoftwareBrandItem> = remember(product) {
        when {
            titleLower.contains("frp") -> SoftwareServicesData.getFrpBrandList()
            titleLower.contains("icloud") || titleLower.contains("apple id") -> SoftwareServicesData.getICloudBrandList()
            titleLower.contains("ქსელ") || titleLower.contains("sim") || titleLower.contains("unlock") -> SoftwareServicesData.getNetworkUnlockList()
            titleLower.contains("auth") -> SoftwareServicesData.getAuthFlashingList()
            else -> SoftwareServicesData.getFrpBrandList()
        }
    }

    var expandedBrandIndex by remember { mutableStateOf<Int?>(0) } // პირველი ბრენდი გახსნილია საწყისად

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF14171D),
        border = BorderStroke(1.dp, Color(0xFF282F3B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛠️", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "მხარდაჭერილი ბრენდები & მეთოდები",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF222936)
                ) {
                    Text(
                        "${brandItems.size} ბრენდი",
                        color = Color(0xFFFF7A00),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                "აირჩიეთ თქვენი მოდელი დეტალური ტექნიკური აღწერის, მეთოდისა და დროის სანახავად:",
                color = Color(0xFFA0A5B0),
                fontSize = 11.5.sp
            )

            // ბრენდების აკორდეონის სია
            brandItems.forEachIndexed { index, item ->
                val isExpanded = expandedBrandIndex == index
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isExpanded) Color(0xFF1C222C) else Color(0xFF101318),
                    border = BorderStroke(
                        1.dp,
                        if (isExpanded) Color(0xFFFF7A00) else Color(0xFF222731)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedBrandIndex = if (isExpanded) null else index
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(item.iconEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        item.brandName,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "დრო: ${item.estimatedTime} | ფასი: ${item.price}",
                                        color = Color(0xFFFF7A00),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isExpanded) Color(0xFFFF7A00) else Color(0xFF6E7582),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // დეტალური გაშლილი ინფორმაცია
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(color = Color(0xFF2A313E))

                                // მოდელები
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("მოდელები:", color = Color(0xFFA0A5B0), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    Text(item.supportedModels, color = Color.White, fontSize = 11.5.sp)
                                }

                                // მეთოდი
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("ტექნიკური მეთოდი:", color = Color(0xFFA0A5B0), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    Text(item.method, color = Color(0xFF38BDF8), fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                }

                                // დეტალური აღწერა
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("პროცესის აღწერა:", color = Color(0xFFA0A5B0), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    Text(item.details, color = Color(0xFFCBD5E1), fontSize = 11.sp, lineHeight = 15.sp)
                                }

                                // მოთხოვნები
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("საჭირო მოთხოვნა:", color = Color(0xFFA0A5B0), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    Text(item.requirements, color = Color(0xFFFBBF24), fontSize = 11.sp)
                                }

                                // სწრაფი დაკავშირების ღილაკი ამ ბრენდისთვის
                                Button(
                                    onClick = {
                                        val msg = "გამარჯობა ბექა! მსურს სერვისი:\nსერვისი: ${product.title}\nბრენდი/ვარიანტი: ${item.brandName}\nმეთოდი: ${item.method}\nფასი: ${item.price}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/995598233332?text=${Uri.encode(msg)}"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Text("💬 შეკვეთა WhatsApp-ით (${item.price})", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // გარანტიისა და ადგილმდებარეობის ბლოკი
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1B202A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("სრული სამუდამო გარანტია პროგრამულ სერვისზე", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Text("სერვის ცენტრი: ქ. ახალციხე, კოსტავას N10 | დისტანციური USB მხარდაჭერა", color = Color(0xFFA0A5B0), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 4. ჭკვიანი AI დიაგნოსტიკის ლოგიკა (აპარატურული + პროგრამული)
// ==============================================================================
object DiagnosticsAiEngine {

    fun analyzeIssue(brand: String, model: String, issue: String, additionalNote: String): DiagnosticAiReport {
        val combined = "$issue $additionalNote".lowercase()

        return when {
            // FRP
            combined.contains("frp") || combined.contains("google account") || combined.contains("გუგლ") || combined.contains("ფრპ") || combined.contains("ანგარიში") ->
                DiagnosticAiReport(
                    title = "Google FRP (Factory Reset Protection) ბლოკი",
                    issueCategory = "პროგრამული (Software)",
                    confidence = "მაღალი ალბათობით სავარაუდო",
                    estimatedPrice = "45 ₾",
                    estimatedTime = "10-20 წუთი",
                    primaryCause = "ქარხნული გადატვირთვის შემდეგ გააქტიურდა Google-ის უსაფრთხოების ბლოკი, რადგან წინა ანგარიშის პაროლი არ იქნა შეყვანილი.",
                    technicalSteps = listOf(
                        "მოწყობილობის დაკავშირება MTP / Fastboot / EDL რეჟიმში",
                        "Security Patch ვერსიის ამოკითხვა და დამოწმება",
                        "FRP/Persist დანაყოფის სუფთა შემოვლა და Google ტოკენის წაშლა",
                        "ქარხნული გადატვირთვა და საწყისი Setup Wizard-ის გავლა"
                    ),
                    recommendation = "სერვისი სრულდება ადგილზე ბექასერვისში 15 წუთში ქ. ახალციხეში (ან დისტანციურად USB-ით). მუდმივი გარანტიით!",
                    isSoftware = true
                )

            // iCloud
            combined.contains("icloud") || combined.contains("აიქლაუდ") || combined.contains("apple id") || combined.contains("აქტივაცია") ->
                DiagnosticAiReport(
                    title = "iCloud Activation Lock / Apple ID ბლოკი",
                    issueCategory = "პროგრამული (Software)",
                    confidence = "მაღალი ალბათობით სავარაუდო",
                    estimatedPrice = "90 ₾ - 120 ₾",
                    estimatedTime = "20-35 წუთი",
                    primaryCause = "Apple მოწყობილობა დაკავშირებულია წინა მფლობელის iCloud ანგარიშთან და Find My iPhone გააქტიურებულია.",
                    technicalSteps = listOf(
                        "IMEI და სერიული ნომრის Clean/Lost სტატუსის გადამოწმება",
                        "Baseband და Activation Certificate სერვერული გენერაცია",
                        "Checkm8 / Ramdisk ინექცია და სიგნალის სრული აქტივაცია",
                        "iCloud, App Store და FaceTime სერვისების ამუშავება"
                    ),
                    recommendation = "მიმართეთ მხოლოდ ბექასერვისს! გაიცემა სრული გარანტია ქსელისა და ზარების მუშაობაზე.",
                    isSoftware = true
                )

            // ქსელის ბლოკი
            combined.contains("ქსელ") || combined.contains("sim") || combined.contains("სიმ") || combined.contains("ოპერატორ") || combined.contains("unlock") ->
                DiagnosticAiReport(
                    title = "ოპერატორის ქსელური ბლოკი (Carrier / SIM Lock)",
                    issueCategory = "პროგრამული (Software)",
                    confidence = "მაღალი ალბათობით სავარაუდო",
                    estimatedPrice = "60 ₾ - 75 ₾",
                    estimatedTime = "12-24 სთ (სერვერული)",
                    primaryCause = "საზღვარგარეთ შეძენილი ტელეფონი მიბმულია კონკრეტულ ოპერატორზე (AT&T, T-Mobile, Docomo, Vodafone) და არ იღებს ქართულ სიმ-ბარათს.",
                    technicalSteps = listOf(
                        "ოპერატორისა და კონტრაქტის სტატუსის იდენტიფიცირება",
                        "Apple / Brand მონაცემთა ბაზაში Whitelist ჩაწერა",
                        "Factory Unlock-ის დამოწმება და ქსელის სრული გახსნა"
                    ),
                    recommendation = "ტელეფონი სამუდამოდ იმუშავებს Magti, Silknet და Cellfie-ზე. განახლებების დროს არ იბლოკება!",
                    isSoftware = true
                )

            // Bootloop / სისტემა
            combined.contains("bootloop") || combined.contains("აგური") || combined.contains("ლოგო") || combined.contains("არ ირთვება") || combined.contains("პროგრამა") || combined.contains("ფლეშ") ->
                DiagnosticAiReport(
                    title = "სისტემური კოლაფსი (Bootloop / Firmware Corruption)",
                    issueCategory = "პროგრამული (Software)",
                    confidence = "მაღალი ალბათობით სავარაუდო",
                    estimatedPrice = "35 ₾ - 55 ₾",
                    estimatedTime = "25-35 წუთი",
                    primaryCause = "დაზიანდა Android/iOS სისტემური დანაყოფი (Boot/System Partition), წარუმატებელი განახლების ან მეხსიერების გადავსების გამო.",
                    technicalSteps = listOf(
                        "სისტემის დიაგნოსტიკა Recovery/Fastboot რეჟიმში",
                        "ოფიციალური მწარმოებლის Stock ROM-ის ჩაწერა",
                        "მონაცემთა მაქსიმალური შენარჩუნება (Userdata partition შემოწმება)",
                        "სისტემის სტაბილურობისა და ბატარეის ტესტი"
                    ),
                    recommendation = "ნუ ეცდებით საეჭვო ფლეშერებით ჩაწერას, რათა არ დაზიანდეს მეხსიერების ჩიპი. ბექასერვისში აღდგება უსაფრთხოდ!",
                    isSoftware = true
                )

            // Auth Flashing
            combined.contains("auth") || combined.contains("edl") || combined.contains("9008") || combined.contains("msm") ->
                DiagnosticAiReport(
                    title = "Authorized EDL 9008 სერვერული ფლეშინგი",
                    issueCategory = "პროგრამული (Software)",
                    confidence = "მაღალი ალბათობით სავარაუდო",
                    estimatedPrice = "55 ₾",
                    estimatedTime = "15-20 წუთი",
                    primaryCause = "მოწყობილობა ჩავარდნილია ღრმა აგურის რეჟიმში (Emergency Download Mode) და მოითხოვს მწარმოებლის ოფიციალურ სერვერულ ტოკენს.",
                    technicalSteps = listOf(
                        "Qualcomm HS-USB QDLoader 9008 პორტის სტაბილიზაცია",
                        "ოფიციალური სერვერული Firehose გასაღების ავტორიზაცია",
                        "სრული Rawprogram და Patch XML სექტორების ჩაწერა"
                    ),
                    recommendation = "ბექასერვისს გააჩნია ოფიციალური სერვერული ანგარიშები Xiaomi, Oppo, Realme, OnePlus მოდელებისთვის.",
                    isSoftware = true
                )

            // ეკრანი (Hardware)
            combined.contains("ეკრან") || combined.contains("ზოლ") || combined.contains("ლაქ") || combined.contains("ბზარ") || combined.contains("screen") ->
                DiagnosticAiReport(
                    title = "OLED დისპლეის მატრიცის ფიზიკური დაზიანება",
                    issueCategory = "აპარატურული (Hardware)",
                    confidence = "მაღალი ალბათობით სავარაუდო",
                    estimatedPrice = "180 ₾ - 390 ₾",
                    estimatedTime = "25-35 წუთი",
                    primaryCause = "დარტყმის შედეგად დაზიანდა AMOLED/OLED მატრიცის ორგანული პიქსელების ფენა ან სენსორული შლეიფი.",
                    technicalSteps = listOf(
                        "ტელეფონის გახსნა თერმო-სადგურზე ჰერმეტულობის შენარჩუნებით",
                        "True Tone და სენსორის EEPROM მონაცემების გადაპროგრამება",
                        "ახალი ორიგინალი Super Retina / AMOLED ეკრანის მონტაჟი",
                        "წყალგაუმტარი წებოს (Waterproof seal) აღდგენა"
                    ),
                    recommendation = "ბექასერვისში გვაქვს უმაღლესი ხარისხის ორიგინალი OLED ეკრანები ადგილზე მარაგში, 6-თვიანი გარანტიით!",
                    isSoftware = false
                )

            // ელემენტი (Hardware)
            combined.contains("ელემენტ") || combined.contains("ბატარე") || combined.contains("battery") || combined.contains("ჯდება") || combined.contains("გაბერ") ->
                DiagnosticAiReport(
                    title = "ლითიუმ-იონური ელემენტის ქიმიური დეგრადაცია",
                    issueCategory = "აპარატურული (Hardware)",
                    confidence = "მაღალი ალბათობით სავარაუდო",
                    estimatedPrice = "75 ₾ - 110 ₾",
                    estimatedTime = "15-20 წუთი",
                    primaryCause = "ელემენტის ციკლების რაოდენობამ გადააჭარბა 500-ს (Health < 80%) ან გაჩნდა შიდა მიკრო-მოკლე ჩართვა.",
                    technicalSteps = listOf(
                        "ძველი ელემენტის უსაფრთხო დემონტაჟი",
                        "ორიგინალი BMS კონტროლერის გადატანა (iPhone-ზე 100% Health შენარჩუნებით)",
                        "ახალი 0-ციკლიანი ორიგინალი ელემენტის ინსტალაცია"
                    ),
                    recommendation = "დროული შეცვლა აუცილებელია დედაპლატისა და ეკრანის დაზიანების თავიდან ასაცილებლად.",
                    isSoftware = false
                )

            // დამტენის პორტი (Hardware)
            combined.contains("პორტ") || combined.contains("დამტენ") || combined.contains("charger") || combined.contains("არ იტენება") ->
                DiagnosticAiReport(
                    title = "Type-C / Lightning დამტენის პორტის კონტაქტების ცვეთა",
                    issueCategory = "აპარატურული (Hardware)",
                    confidence = "მაღალი ალბათობით სავარაუდო",
                    estimatedPrice = "55 ₾ - 85 ₾",
                    estimatedTime = "20-30 წუთი",
                    primaryCause = "მექანიკური ცვეთა, მტვრის დაპრესვა ან ტენიანობით გამოწვეული პინების დაჟანგვა.",
                    technicalSteps = listOf(
                        "პორტის მიკროსკოპული ინსპექცია და ულტრაბგერითი წმენდა",
                        "საჭიროების შემთხვევაში ორიგინალი ქვედა პლატის / შლეიფის შეცვლა",
                        "სწრაფი დამუხტვის (Fast Charge / PD) ტესტირება"
                    ),
                    recommendation = "ადგილზე შეიცვლება 25 წუთში ბექასერვისში.",
                    isSoftware = false
                )

            // ზოგადი
            else ->
                DiagnosticAiReport(
                    title = "$brand $model - კომპლექსური დიაგნოსტიკა",
                    issueCategory = "კომპლექსური (Hardware & Software)",
                    confidence = "საჭიროებს გადამოწმებას",
                    estimatedPrice = "30 ₾ - 120 ₾",
                    estimatedTime = "20-40 წუთი",
                    primaryCause = "სიმპტომები მოითხოვს პროფესიონალურ შემოწმებას ტესტერითა და დიაგნოსტიკური პროგრამით.",
                    technicalSteps = listOf(
                        "დედაპლატის დენის მოხმარების შემოწმება ლაბორატორიულ ბლოკზე",
                        "სისტემური ლოგების (Panic Full / Bugreport) ანალიზი",
                        "ოპტიმალური შეკეთების გეგმის შერჩევა"
                    ),
                    recommendation = "მობრძანდით ბექასერვისში: ქ. ახალციხე, კოსტავას N10. კონსულტაცია უფასოა!",
                    isSoftware = false
                )
        }
    }
}

// ==============================================================================
// 5. DiagnosticsScreen-ში ჩასამაგრებელი AI ასისტენტის ინტერაქტიული ბარათი
// ==============================================================================
@Composable
fun AttachedAiDiagnosticsCard(
    brand: String,
    model: String,
    selectedIssue: String,
    onDirectOrderClick: (DiagnosticAiReport) -> Unit
) {
    val context = LocalContext.current
    var additionalNotes by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var aiReport by remember(brand, model, selectedIssue) {
        mutableStateOf(DiagnosticsAiEngine.analyzeIssue(brand, model, selectedIssue, ""))
    }

    LaunchedEffect(brand, model, selectedIssue) {
        aiReport = DiagnosticsAiEngine.analyzeIssue(brand, model, selectedIssue, additionalNotes)
    }

    val coroutineScope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF14171E),
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFF38BDF8)))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF6A00).copy(alpha = 0.2f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🤖", fontSize = 15.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "AI დიაგნოსტიკა",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "ბექასერვისი AI",
                            color = Color(0xFFA0A5B0),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF22C55E).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color(0xFF22C55E))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = aiReport.confidence,
                            color = Color(0xFF4ADE80),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Input for additional symptoms / error code
            OutlinedTextField(
                value = additionalNotes,
                onValueChange = {
                    additionalNotes = it
                    aiReport = DiagnosticsAiEngine.analyzeIssue(brand, model, selectedIssue, it)
                },
                placeholder = {
                    Text(
                        "დამატებითი სიმპტომი (მაგ: FRP ბლოკი, ეკრანზე მწვანე ზოლი, Bootloop...)",
                        color = Color(0xFF6E7582),
                        fontSize = 11.5.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF7A00),
                    unfocusedBorderColor = Color(0xFF262C38),
                    focusedContainerColor = Color(0xFF0F1216),
                    unfocusedContainerColor = Color(0xFF0F1216)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // სწრაფი ჩიპები
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val chips = listOf("FRP მოხსნა", "iCloud ბლოკი", "ეკრანზე მწვანე ზოლი", "Bootloop ლოგო", "ქსელის SIM ბლოკი", "Auth 9008")
                items(chips) { chip ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E2430),
                        border = BorderStroke(1.dp, Color(0xFF2E384A)),
                        modifier = Modifier.clickable {
                            additionalNotes = chip
                            aiReport = DiagnosticsAiEngine.analyzeIssue(brand, model, selectedIssue, chip)
                        }
                    ) {
                        Text(
                            chip,
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF262D3B))

            // დიაგნოზის შედეგი
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        aiReport.title,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (aiReport.isSoftware) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFFFF7A00).copy(alpha = 0.2f)
                    ) {
                        Text(
                            aiReport.issueCategory,
                            color = if (aiReport.isSoftware) Color(0xFF38BDF8) else Color(0xFFFF7A00),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    aiReport.primaryCause,
                    color = Color(0xFFA0A5B0),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }

            // ფასი & დრო
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F1217),
                border = BorderStroke(1.dp, Color(0xFF222834)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("სავარაუდო ღირებულება", color = Color(0xFFA0A5B0), fontSize = 10.5.sp)
                        Text(aiReport.estimatedPrice, color = Color(0xFFFF7A00), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("სამუშაო დრო", color = Color(0xFFA0A5B0), fontSize = 10.5.sp)
                        Text(aiReport.estimatedTime, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ტექნიკური ეტაპები
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("შესრულების ტექნიკური ეტაპები:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                aiReport.technicalSteps.forEachIndexed { i, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. ", color = Color(0xFFFF7A00), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        Text(step, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    }
                }
            }

            // WhatsApp შეკვეთა AI დიაგნოზით
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            FirestoreRepository(context).saveDiagnosticReport(
                                deviceModel = "$brand $model",
                                issueCategory = aiReport.issueCategory,
                                symptoms = additionalNotes.ifBlank { selectedIssue },
                                aiAnalysis = "${aiReport.title} - ${aiReport.primaryCause}",
                                estimatedCost = aiReport.estimatedPrice,
                                confidence = 98.4
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("AiDiag", "Error saving report to Firestore", e)
                        }
                    }
                    val msg = "გამარჯობა ბექა! AI დიაგნოსტიკის შედეგი:\nმოდელი: $brand $model\nპრობლემა: ${aiReport.title}\nტიპი: ${aiReport.issueCategory}\nსავარაუდო ფასი: ${aiReport.estimatedPrice}\nდრო: ${aiReport.estimatedTime}\nმსურს სერვისზე ჩაწერა."
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/995598233332?text=${Uri.encode(msg)}"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ჩაეწერე სერვისზე AI დიაგნოზით 💬", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
