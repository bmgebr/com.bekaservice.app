package com.example

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BekaSideNavigationDrawerContent(
    selectedTab: Int,
    unreadNotificationsCount: Int,
    cartItemsCount: Int,
    onSelectTab: (Int) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current
    val isDark = AppPreferencesManager.isDarkMode
    val currentLang = AppPreferencesManager.currentLanguage
    val currentUser = remember(selectedTab, isDark, currentLang) {
        AuthAndOrderManager.getCurrentUser(context)
    }

    val sheetBgColor = if (isDark) Color(0xFF101318) else Color(0xFFFFFFFF)
    val cardBgColor = if (isDark) Color(0xFF16191E) else Color(0xFFF4F7FB)
    val borderCol = if (isDark) Color(0xFF242830) else Color(0xFFDCE3EE)

    ModalDrawerSheet(
        drawerContainerColor = sheetBgColor,
        drawerTonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth(0.84f)
            .widthIn(max = 330.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. ლოგო და მომხმარებლის სტატუსის ჰედერი
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.45f)),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_beka_main_emblem),
                                contentDescription = "BEKASERVICE",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = "BEKA",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        lineHeight = 17.sp,
                                        letterSpacing = 0.4.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = androidx.compose.ui.text.TextStyle(
                                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                    Text(
                                        text = "SERVICE",
                                        color = Color(0xFFFF7A00),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        lineHeight = 17.sp,
                                        letterSpacing = 0.4.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = androidx.compose.ui.text.TextStyle(
                                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                                Text(
                                    text = "სანდო სერვისი",
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp,
                                    letterSpacing = 0.4.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                    maxLines = 1,
                                    softWrap = false,
                                    style = androidx.compose.ui.text.TextStyle(
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint = Color(0xFFA0A5B0),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = borderCol)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectTab(4)
                                onCloseDrawer()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFF7A00).copy(alpha = 0.18f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFFFF7A00),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser?.displayName?.ifBlank { "მომხმარებელი" }
                                        ?: AppLocaleManager.tr("სტუმრის რეჟიმი", "Guest Mode", "Гостевой режим"),
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentUser?.email?.ifBlank {
                                        AppLocaleManager.tr("პროფილის გახსნა", "Open Profile", "Открыть профиль")
                                    } ?: AppLocaleManager.tr("ავტორიზაცია / პროფილი", "Sign In / Profile", "Вход / Профиль"),
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFFF7A00),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. ნავიგაციის პუნქტები
            Text(
                text = "ნავიგაციის მენიუ",
                color = Color(0xFFA0A5B0),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            val drawerItems = listOf(
                DrawerMenuItemData(0, "მთავარი გვერდი", Icons.Default.Home, 0),
                DrawerMenuItemData(1, "კატალოგი & სერვისები", Icons.Default.GridView, 0),
                DrawerMenuItemData(2, "ონლაინ დიაგნოსტიკა", Icons.Default.Build, 0),
                DrawerMenuItemData(3, "AI ასისტენტი", Icons.Default.SmartToy, 0),
                DrawerMenuItemData(6, "შეტყობინებები", Icons.Default.Notifications, unreadNotificationsCount),
                DrawerMenuItemData(5, "კალათა", Icons.Default.ShoppingCart, cartItemsCount),
                DrawerMenuItemData(4, "პროფილი & შეკვეთები", Icons.Default.Person, 0)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                drawerItems.forEach { item ->
                    val isSelected = selectedTab == item.tabIndex
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFFF6A00).copy(alpha = 0.16f) else cardBgColor,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFFF7A00) else borderCol
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectTab(item.tabIndex)
                                onCloseDrawer()
                            }
                            .testTag("drawer_item_${item.tabIndex}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) Color(0xFFFF7A00) else Color(0xFFA0A5B0),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = item.title,
                                    color = if (isSelected) Color(0xFFFF7A00) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (item.badgeCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFF6A00)
                                ) {
                                    Text(
                                        text = "${item.badgeCount}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Light / Dark Mode გადამრთველი
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "თემა (Dark / Light)",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = !isDark,
                            onCheckedChange = { lightSelected ->
                                AppPreferencesManager.setDarkMode(context, !lightSelected)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF7A00),
                                uncheckedThumbColor = Color(0xFFFF7A00),
                                uncheckedTrackColor = Color(0xFF262B35)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFFFF6A00) else Color(0xFF1E222B),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable { AppPreferencesManager.setDarkMode(context, true) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "🌙 Dark Mode",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!isDark) Color(0xFFFF6A00) else Color(0xFF1E222B),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable { AppPreferencesManager.setDarkMode(context, false) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "☀️ Light Mode",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // 4. ენის არჩევა (ქართული Default / English / Русский)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color(0xFFFF7A00),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "აპლიკაციის ენა",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            val isSelected = currentLang == lang
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF1E222B),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF7A00) else borderCol
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable {
                                        AppPreferencesManager.setLanguage(context, lang)
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "${lang.flag} ${lang.label}",
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. მართვის პანელში შესვლის ღილაკი (გადატანილია გვერდითა ნავიგაციის მენიუში)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFF6A00).copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color(0xFFFF7A00)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCloseDrawer()
                        onOpenAdminPanel()
                    }
                    .testTag("admin_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Panel",
                            tint = Color(0xFFFF7A00),
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚙️ მართვის პანელი",
                                color = Color(0xFFFF7A00),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "BEKASERVICE • სრული კონტროლი",
                                color = Color(0xFFA0A5B0),
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFF7A00),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 6. სწრაფი კონტაქტი
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F2618),
                    border = BorderStroke(1.dp, Color(0xFF1B4E2E)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/995598233332"))
                            context.startActivity(intent)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "💬 WhatsApp",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+995598233332"))
                            context.startActivity(intent)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "📞 598 23 33 32",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 7. აპლიკაციის განახლება — გვერდითა პანელის ყველაზე ქვედა ნავიგაციის პუნქტი
            val isUpdateSelected = selectedTab == 7
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isUpdateSelected) Color(0xFFFF6A00).copy(alpha = 0.16f) else cardBgColor,
                border = BorderStroke(
                    1.dp,
                    if (isUpdateSelected) Color(0xFFFF7A00) else borderCol
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectTab(7)
                        onCloseDrawer()
                    }
                    .testTag("drawer_item_7")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "განახლება",
                            tint = if (isUpdateSelected) Color(0xFFFF7A00) else Color(0xFFA0A5B0),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "განახლება",
                            color = if (isUpdateSelected) Color(0xFFFF7A00) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isUpdateSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (isUpdateSelected) Color(0xFFFF7A00) else Color(0xFF8C94A4),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class DrawerMenuItemData(
    val tabIndex: Int,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeCount: Int
)

// ==============================================================================
// ზოგადი შეტყობინებების გვერდი მომხმარებლისთვის (Notifications Center - Tab 6)
// ==============================================================================
@Composable
fun UserNotificationsScreen(
    cloudNotifications: List<PushNotificationRecord>,
    products: List<ProductItem>,
    onNavigateToTarget: (targetTab: Int, targetProductId: Int) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    val context = LocalContext.current
    val version = BekaNotificationManager.stateVersion.intValue
    var selectedFilter by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) {
        BekaNotificationManager.purgeSyntheticNotificationsIfNeeded(context, force = true)
    }

    val allVisibleNotifications = remember(cloudNotifications, version) {
        BekaNotificationManager.getVisibleUserNotifications(context, cloudNotifications)
    }

    val unreadCount = remember(allVisibleNotifications, version) {
        allVisibleNotifications.count { !BekaNotificationManager.isNotificationRead(context, it.id) }
    }

    val filteredNotifications = remember(allVisibleNotifications, selectedFilter, version) {
        when (selectedFilter) {
            "unread" -> allVisibleNotifications.filter { !BekaNotificationManager.isNotificationRead(context, it.id) }
            "product_offer" -> allVisibleNotifications.filter { it.type == "product_offer" }
            "service_update" -> allVisibleNotifications.filter { it.type == "service_update" }
            "announcement" -> allVisibleNotifications.filter { it.type == "announcement" }
            else -> allVisibleNotifications
        }
    }

    val filterTabs = listOf(
        "all" to "ყველა (${allVisibleNotifications.size})",
        "unread" to "წაუკითხავი ($unreadCount)",
        "product_offer" to "🔥 პროდუქტის აქცია",
        "service_update" to "🛠️ სერვისის სიახლე",
        "announcement" to "📢 ზოგადი განცხადება"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("user_notifications_screen"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. ჰედერი და ყველას წაკითხულად მონიშვნის ღილაკი
        item(key = "notif_screen_header") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16191E),
                border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.4f)),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFF7A00).copy(alpha = 0.16f),
                                modifier = Modifier.size(38.dp).clickable { onBack() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFFFF7A00),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "შეტყობინებების ცენტრი",
                                    color = Color.White,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        allVisibleNotifications.isEmpty() -> "შეტყობინებების რეალური ისტორია"
                                        unreadCount > 0 -> "თქვენ გაქვთ $unreadCount წაუკითხავი შეტყობინება"
                                        else -> "ყველა შეტყობინება წაკითხულია ✅"
                                    },
                                    color = when {
                                        allVisibleNotifications.isEmpty() -> Color(0xFFA0A5B0)
                                        unreadCount > 0 -> Color(0xFFFF9E40)
                                        else -> Color(0xFF4ADE80)
                                    },
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (unreadCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFF6A00),
                                modifier = Modifier.clickable {
                                    BekaNotificationManager.markAllNotificationsRead(context, allVisibleNotifications)
                                    Toast.makeText(
                                        context,
                                        "ყველა შეტყობინება მოინიშნა წაკითხულად",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Text(
                                    text = "ყველას წაკითხულად მონიშვნა",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filterTabs, key = { it.first }) { (key, label) ->
                            val isSelected = selectedFilter == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF1E222B),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF7A00) else Color(0xFF282D37)
                                ),
                                modifier = Modifier.clickable { selectedFilter = key }
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // შეცდომის ბანერი (თუ ქსელის/Firestore-ის შეცდომა მოხდა)
        if (!errorMessage.isNullOrBlank()) {
            item(key = "notif_error_banner") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2B1618),
                    border = BorderStroke(1.dp, Color(0xFFFF4D4F).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ისტორიის ჩატვირთვის შეფერხება",
                                color = Color(0xFFFF7875),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFFCCC7),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF4D4F),
                            modifier = Modifier.clickable { onRetry() }
                        ) {
                            Text(
                                text = "განახლება",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // ჩატვირთვის ინდიკატორი
        if (isLoading && allVisibleNotifications.isEmpty()) {
            item(key = "notif_loading") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16191E),
                    border = BorderStroke(1.dp, Color(0xFF242830)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF7A00),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "შეტყობინებების ისტორია იტვირთება...",
                            color = Color(0xFFA0A5B0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else if (filteredNotifications.isEmpty()) {
            // 2. ცარიელი მდგომარეობა (ყალბი/სატესტო შეტყობინებების გარეშე)
            item(key = "notif_empty") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16191E),
                    border = BorderStroke(1.dp, Color(0xFF242830)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notifications_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 44.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = Color(0xFFFF7A00),
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "შეტყობინებები ჯერ არ არის",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (allVisibleNotifications.isEmpty()) {
                                "როდესაც ადმინისტრატორი გამოაგზავნის ახალ შეტყობინებას ან აქციას, ის ავტომატურად გამოჩნდება აქ."
                            } else {
                                "არჩეულ კატეგორიაში შეტყობინებები არ მოიძებნა."
                            },
                            color = Color(0xFF8C94A4),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        } else {
            // 3. რეალური შეტყობინებების სია (დალაგებული უახლესიდან უძველესისკენ)
            items(filteredNotifications, key = { it.id }) { notif ->
                val isRead = BekaNotificationManager.isNotificationRead(context, notif.id)
                val targetProd = remember(notif.targetProductId, products) {
                    if (notif.targetProductId > 0) products.find { it.id == notif.targetProductId } else null
                }
                val dateStr = remember(notif.createdAtMillis) {
                    SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
                        .format(Date(notif.createdAtMillis))
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF16191E),
                    border = BorderStroke(
                        width = if (isRead) 1.dp else 1.5.dp,
                        color = if (isRead) Color(0xFF242830) else Color(0xFFFF7A00)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            BekaNotificationManager.markNotificationRead(context, notif.id)
                            onNavigateToTarget(notif.targetTab, notif.targetProductId)
                        }
                        .testTag("notification_history_item_${notif.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (!isRead) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF7A00))
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFF7A00).copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = BekaNotificationManager.notificationTypeLabel(notif.type),
                                        color = Color(0xFFFF7A00),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = dateStr,
                                color = Color(0xFF8C94A4),
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Text(
                            text = notif.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isRead) FontWeight.SemiBold else FontWeight.ExtraBold
                        )

                        Text(
                            text = notif.body,
                            color = Color(0xFFA0A5B0),
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )

                        // სამიზნე გვერდის / პროდუქტის ბეიჯი
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E222B)
                            ) {
                                Text(
                                    text = if (targetProd != null) {
                                        "🔗 ${targetProd.title} (${targetProd.price})"
                                    } else {
                                        "📍 ${BekaNotificationManager.targetTabLabel(notif.targetTab)}"
                                    },
                                    color = Color(0xFF4ADE80),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF242830))

                        // მოქმედების ღილაკები: წაკითხულად მონიშვნა, გადასვლა, წაშლა
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isRead) Color(0xFF1E222B) else Color(0x224ADE80),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isRead) Color(0xFF282D37) else Color(0xFF4ADE80).copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.clickable {
                                        if (isRead) {
                                            BekaNotificationManager.markNotificationUnread(context, notif.id)
                                        } else {
                                            BekaNotificationManager.markNotificationRead(context, notif.id)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (isRead) "წაუკითხავად დაბრუნება" else "წაკითხულად მონიშვნა",
                                        color = if (isRead) Color(0xFFA0A5B0) else Color(0xFF4ADE80),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFF6A00),
                                    modifier = Modifier.clickable {
                                        BekaNotificationManager.markNotificationRead(context, notif.id)
                                        onNavigateToTarget(notif.targetTab, notif.targetProductId)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "გადასვლა",
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    BekaNotificationManager.dismissNotificationForUser(context, notif.id)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFFF4D4F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// აპლიკაციის განახლების გვერდი მომხმარებლისთვის (App Update Screen - Tab 7)
// ==============================================================================
@Composable
fun AppUpdateScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    val context = LocalContext.current
    val firestoreRepo = remember {
        try {
            FirestoreRepository(context)
        } catch (_: Exception) {
            null
        }
    }

    // დინამიურად ვკითხულობთ ტელეფონში დაყენებული აპლიკაციის ვერსიას Android PackageManager-იდან
    val installedVersionInfo = remember(context) {
        try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pkgInfo.versionName ?: "1.0"
            val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toLong()
            }
            vName to vCode
        } catch (_: Exception) {
            "1.0" to 1L
        }
    }
    val installedVersionName = installedVersionInfo.first
    val installedVersionCode = installedVersionInfo.second

    var updateConfig by remember { mutableStateOf<AdminStorage.AppUpdateConfig?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(firestoreRepo, refreshTrigger) {
        isLoading = true
        errorMessage = null
        val repo = firestoreRepo
        if (repo == null) {
            val cached = AdminStorage.getAppUpdateConfig(context)
            if (cached.latestVersion.isNotBlank() || cached.changeLog.isNotBlank()) {
                updateConfig = cached
            }
            isLoading = false
            onDispose { }
        } else {
            val reg = repo.listenToAppUpdateConfig(
                onUpdate = { cloudConfig ->
                    if (cloudConfig != null) {
                        AdminStorage.saveAppUpdateConfig(context, cloudConfig)
                        updateConfig = cloudConfig
                    } else {
                        updateConfig = null
                    }
                    isLoading = false
                    errorMessage = null
                },
                onError = { err ->
                    val cached = AdminStorage.getAppUpdateConfig(context)
                    if (cached.latestVersion.isNotBlank() || cached.changeLog.isNotBlank()) {
                        updateConfig = cached
                    }
                    errorMessage = err
                    isLoading = false
                }
            )
            onDispose {
                reg.remove()
            }
        }
    }

    val publishedConfig = updateConfig?.takeIf { it.isPublished && it.latestVersion.isNotBlank() }
    val hasValidDownloadUrl = publishedConfig != null &&
        AdminStorage.isValidHttpsDownloadUrl(publishedConfig.downloadUrl)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("app_update_screen"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. სათაური: "აპლიკაციის განახლება"
        item(key = "update_screen_header") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16191E),
                border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFF7A00).copy(alpha = 0.16f),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { onBack() }
                                .testTag("update_screen_back_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "უკან",
                                    tint = Color(0xFFFF7A00),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "აპლიკაციის განახლება",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "BEKASERVICE ოფიციალური ვერსიის კონტროლი",
                                color = Color(0xFFA0A5B0),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { refreshTrigger += 1 },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "განახლების შემოწმება",
                            tint = Color(0xFFFF7A00),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. ვერსიების შედარების ბარათი (მიმდინარე დაყენებული ვერსია vs უახლესი ვერსია)
        item(key = "update_versions_card") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16191E),
                border = BorderStroke(1.dp, Color(0xFF262B35)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // მიმდინარე დაყენებული ვერსია
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E222B),
                            border = BorderStroke(1.dp, Color(0xFF2D3340)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "მიმდინარე ვერსია",
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "v$installedVersionName",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.testTag("installed_version_text")
                                )
                                Text(
                                    text = "Build #$installedVersionCode",
                                    color = Color(0xFF8C94A4),
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        // უახლესი გამოქვეყნებული ვერსია
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (publishedConfig != null) Color(0x22FF7A00) else Color(0xFF1E222B),
                            border = BorderStroke(
                                1.dp,
                                if (publishedConfig != null) Color(0xFFFF7A00) else Color(0xFF2D3340)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "უახლესი ვერსია",
                                    color = if (publishedConfig != null) Color(0xFFFF9E40) else Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (publishedConfig != null) {
                                        val rawVer = publishedConfig.latestVersion.trim()
                                        if (rawVer.startsWith("v", ignoreCase = true)) rawVer else "v$rawVer"
                                    } else {
                                        "—"
                                    },
                                    color = if (publishedConfig != null) Color(0xFFFF7A00) else Color(0xFF8C94A4),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.testTag("latest_version_text")
                                )
                                Text(
                                    text = if (publishedConfig != null) "ხელმისაწვდომია" else "არ არის გამოქვეყნებული",
                                    color = if (publishedConfig != null) Color(0xFF4ADE80) else Color(0xFF8C94A4),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (publishedConfig != null && publishedConfig.updatedAtMillis > 0L) {
                        val updatedDateStr = remember(publishedConfig.updatedAtMillis) {
                            SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
                                .format(Date(publishedConfig.updatedAtMillis))
                        }
                        Text(
                            text = "გამოქვეყნების დრო: $updatedDateStr",
                            color = Color(0xFF8C94A4),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // ქსელის შეცდომის ჩვენება (თუ მოხდა)
        if (!errorMessage.isNullOrBlank()) {
            item(key = "update_error_banner") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF2B1618),
                    border = BorderStroke(1.dp, Color(0xFFFF4D4F).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "სერვერთან დაკავშირების შეცდომა",
                                color = Color(0xFFFF7875),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFFCCC7),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF4D4F),
                            modifier = Modifier.clickable { refreshTrigger += 1 }
                        ) {
                            Text(
                                text = "თავიდან ცდა",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item(key = "update_loading") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16191E),
                    border = BorderStroke(1.dp, Color(0xFF242830)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF7A00),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "განახლების ინფორმაცია მოწმდება...",
                            color = Color(0xFFA0A5B0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else if (publishedConfig == null) {
            // როდესაც განახლება არ არის გამოქვეყნებული — ვაჩვენებთ მკაფიო ქართულ შეტყობინებას
            // და არ ვაჩვენებთ აქტიურ გადმოწერის ღილაკს
            item(key = "update_not_published") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16191E),
                    border = BorderStroke(1.dp, Color(0xFF242830)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_unpublished_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "ამჟამად გამოქვეყნებული განახლება არ არის",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "თქვენ სარგებლობთ აპლიკაციის მიმდინარე ვერსიით (v$installedVersionName). როდესაც ადმინისტრატორი გამოაქვეყნებს ახალ ვერსიას, აქ გამოჩნდება განახლების აღწერა და გადმოწერის ღილაკი.",
                            color = Color(0xFF8C94A4),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else {
            // 3. გამოქვეყნებული Change Log (განთავსებულია გადმოწერის ღილაკამდე!)
            item(key = "update_changelog_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16191E),
                    border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("update_changelog_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "რა შეიცვალა ახალ ვერსიაში (Change Log)",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        HorizontalDivider(color = Color(0xFF262B35))

                        // მრავალსტრიქონიანი ტექსტისა და ბულეტ-პოინტების სრული შენარჩუნება
                        Text(
                            text = publishedConfig.changeLog.ifBlank { "განახლების დეტალური აღწერა მითითებული არ არის." },
                            color = Color(0xFFE5E7EB),
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_changelog_text")
                        )
                    }
                }
            }

            // 4. თვალსაჩინო გადმოწერის ღილაკი: "ახალი ვერსიის გადმოწერა"
            item(key = "update_download_action") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16191E),
                    border = BorderStroke(1.dp, Color(0xFF262B35)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (hasValidDownloadUrl) {
                            Button(
                                onClick = {
                                    val targetUrl = publishedConfig.downloadUrl.trim()
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "ბმულის გახსნა ვერ მოხერხდა: შეამოწმეთ ბრაუზერი ან ბმულის სისწორე",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6A00),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("download_new_version_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ახალი ვერსიის გადმოწერა",
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = "ღილაკზე დაჭერისას გაიხსნება ოფიციალური APK ფაილის გადმოწერის ბმული.",
                                color = Color(0xFF8C94A4),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF2B1618),
                                border = BorderStroke(1.dp, Color(0xFFFF4D4F).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "გადმოწერის ბმული დროებით მიუწვდომელია ან არასწორი ფორმატისაა.",
                                    color = Color(0xFFFFCCC7),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

