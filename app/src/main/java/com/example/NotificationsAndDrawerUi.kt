package com.example

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.*

@Composable
fun BekaSideNavigationDrawerContent(
    selectedTab: Int,
    unreadNotificationsCount: Int,
    cartItemsCount: Int,
    onSelectTab: (Int) -> Unit,
    onOpenDiagnostics: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    onOpenAdminPanel: () -> Unit,
    onCloseDrawer: () -> Unit,
    products: List<ProductItem> = emptyList(),
    onProductClick: (ProductItem) -> Unit = {},
    onAddToCart: (ProductItem) -> Unit = {},
    onToggleFavorite: (ProductItem) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppPreferencesManager.isDarkMode
    val currentLang = AppPreferencesManager.currentLanguage
    val authVersion = AuthAndOrderManager.authStateVersion.intValue
    val favVersion = AuthAndOrderManager.favoritesStateVersion.intValue

    var currentUser by remember(selectedTab, isDark, currentLang, authVersion, favVersion) {
        mutableStateOf(AuthAndOrderManager.getCurrentUser(context))
    }
    val isAuthorizedAdminUi = remember(currentUser?.id, currentUser?.email, authVersion, favVersion) {
        AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)
    }

    val userOrders = remember(currentUser?.id, authVersion) {
        if (currentUser != null && AuthAndOrderManager.isRegisteredAuthenticatedUser(context)) {
            AuthAndOrderManager.getOrders(context)
        } else {
            emptyList()
        }
    }
    val favoriteIds = remember(currentUser?.id, favVersion) {
        AuthAndOrderManager.getFavoriteProductIds(context)
    }
    val favoriteProducts = remember(products, favoriteIds, currentUser?.id, favVersion) {
        val baseCatalog = if (products.isNotEmpty()) products else AdminStorage.loadProducts(context).ifEmpty { ProductRepository.getAllProducts() }
        baseCatalog.filter { it.id in favoriteIds || it.isFavorite }
    }

    val installedVersionName = remember(context) {
        runCatching {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pkgInfo.versionName?.takeIf { it.isNotBlank() } ?: "1.0"
        }.getOrDefault("1.0")
    }

    // Dialog states triggered directly from the drawer
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAllOrdersDialog by remember { mutableStateOf(false) }
    var selectedOrderForDetail by remember { mutableStateOf<OrderRecord?>(null) }
    var selectedOrderForPaymentInstructions by remember { mutableStateOf<OrderRecord?>(null) }
    var showAllFavoritesDialog by remember { mutableStateOf(false) }
    var showHelpContactDialog by remember { mutableStateOf(false) }
    var showServiceCenterDialog by remember { mutableStateOf(false) }
    var activeLegalPolicyTopic by remember { mutableStateOf<LegalPolicyTopic?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    // Reference visual palette (deep dark graphite drawer in Dark Mode; Soft Graphite Light in Light Mode)
    val sheetBgColor = if (isDark) Color(0xFF0E1015) else Color(0xFFF0F1F4)
    val cardBgColor = if (isDark) Color(0xFF161922) else Color(0xFFF8F9FB)
    val borderCol = if (isDark) Color(0xFF252A34) else Color(0xFFDCE0E6)
    val dividerCol = if (isDark) Color(0xFF222733) else Color(0xFFDCE0E6)
    val primaryTextCol = if (isDark) Color.White else Color(0xFF20242B)
    val secondaryTextCol = if (isDark) Color(0xFF9BA3B2) else Color(0xFF687180)
    val iconMutedCol = if (isDark) Color(0xFFCBD5E1) else Color(0xFF687180)
    val chevronCol = if (isDark) Color(0xFF94A3B8) else Color(0xFF9299A5)
    val brandOrange = if (isDark) Color(0xFFFF6B00) else Color(0xFFF36B16)

    ModalDrawerSheet(
        drawerContainerColor = sheetBgColor,
        drawerTonalElevation = 0.dp,
        windowInsets = WindowInsets.systemBars,
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .widthIn(max = 340.dp)
            .fillMaxHeight()
            .testTag("bstore_side_navigation_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .testTag("drawer_scroll_column"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // =========================================================================
            // 1. HEADER: BS Logo + BSTORE Wordmark + Circular X Close Button
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_header_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black,
                        border = BorderStroke(1.2.dp, brandOrange),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("drawer_brand_icon")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_bstore_brand_icon),
                            contentDescription = "BSTORE Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        )
                    }

                    androidx.compose.material3.Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            append(
                                androidx.compose.ui.text.AnnotatedString(
                                    "B",
                                    androidx.compose.ui.text.SpanStyle(
                                        color = brandOrange,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                        letterSpacing = 0.6.sp
                                    )
                                )
                            )
                            append(
                                androidx.compose.ui.text.AnnotatedString(
                                    "STORE",
                                    androidx.compose.ui.text.SpanStyle(
                                        color = primaryTextCol,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 21.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                        letterSpacing = 0.6.sp
                                    )
                                )
                            )
                        },
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.testTag("drawer_brand_wordmark")
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF1A1E28) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onCloseDrawer() }
                        .testTag("drawer_close_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "მენიუს დახურვა",
                            tint = primaryTextCol,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // =========================================================================
            // 2. USER PROFILE CARD (Real user data, Google verified badge, Edit button)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_profile_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectTab(4)
                                onCloseDrawer()
                            }
                            .testTag("drawer_profile_identity_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Circular Avatar with 2dp Orange Ring
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E222D) else Color(0xFFF1F5F9),
                            border = BorderStroke(2.dp, brandOrange),
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("drawer_user_avatar")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val avatarUrl = currentUser?.avatarUrl.orEmpty().trim()
                                if (avatarUrl.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "მომხმარებლის ფოტო",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "მომხმარებლის ავატარი",
                                        tint = if (isDark) Color.White else Color(0xFF334155),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val displayNameText = currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
                                ?: if (currentUser != null) "მომხმარებელი" else "სტუმარი"
                            val emailText = currentUser?.email?.trim()?.takeIf { it.isNotBlank() }
                                ?: if (currentUser != null) "ელ. ფოსტა არ არის მითითებული" else "გაიარეთ ავტორიზაცია"

                            androidx.compose.material3.Text(
                                text = displayNameText,
                                color = primaryTextCol,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("drawer_user_name")
                            )

                            androidx.compose.material3.Text(
                                text = emailText,
                                color = secondaryTextCol,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("drawer_user_email")
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            // Provider Status Pill — strictly reflects real session provider
                            val isGoogleAuth = currentUser != null && currentUser!!.provider.equals("Google", ignoreCase = true)
                            val isAuthenticatedUser = currentUser != null
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    isGoogleAuth -> if (isDark) Color(0xFF0F3923) else Color(0xFFDCFCE7)
                                    isAuthenticatedUser -> if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE)
                                    else -> if (isDark) Color(0xFF1F2430) else Color(0xFFF1F5F9)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isGoogleAuth -> if (isDark) Color(0xFF22C55E).copy(alpha = 0.65f) else Color(0xFF16A34A)
                                        isAuthenticatedUser -> Color(0xFF38BDF8).copy(alpha = 0.45f)
                                        else -> borderCol
                                    }
                                ),
                                modifier = Modifier.testTag("drawer_auth_provider_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isGoogleAuth) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        androidx.compose.material3.Text(
                                            text = "ავტორიზებული (Google)",
                                            color = if (isDark) Color(0xFFE8F5E9) else Color(0xFF14532D),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    } else if (isAuthenticatedUser) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        androidx.compose.material3.Text(
                                            text = "ავტორიზებული (${currentUser!!.provider.ifBlank { "Email" }})",
                                            color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PersonOutline,
                                            contentDescription = null,
                                            tint = secondaryTextCol,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        androidx.compose.material3.Text(
                                            text = "სტუმრის რეჟიმი",
                                            color = secondaryTextCol,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "პროფილი",
                            tint = chevronCol,
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("drawer_profile_chevron")
                        )
                    }

                    // Full-width outlined orange Edit Profile button inside the card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF1B1614) else Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, brandOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (currentUser != null) {
                                    showEditProfileDialog = true
                                } else {
                                    onSelectTab(4)
                                    onCloseDrawer()
                                }
                            }
                            .testTag("drawer_edit_profile_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentUser != null) Icons.Default.Edit else Icons.Default.Login,
                                contentDescription = null,
                                tint = brandOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            androidx.compose.material3.Text(
                                text = if (currentUser != null) "რედაქტირება" else "შესვლა / ავტორიზაცია",
                                color = brandOrange,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. SHOPPING SECTION (🛍️ შოპინგი)
            // =========================================================================
            DrawerSectionHeader(
                icon = Icons.Default.ShoppingBag,
                title = "შოპინგი",
                brandOrange = brandOrange
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_shopping_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DrawerGroupedRow(
                        icon = Icons.Default.ShoppingBag,
                        title = "ჩემი შეკვეთები",
                        badgeCount = userOrders.size,
                        primaryTextCol = primaryTextCol,
                        iconTint = iconMutedCol,
                        chevronCol = chevronCol,
                        brandOrange = brandOrange,
                        testTag = "drawer_my_orders_row",
                        onClick = {
                            if (currentUser != null) {
                                showAllOrdersDialog = true
                            } else {
                                onSelectTab(4)
                                onCloseDrawer()
                            }
                        }
                    )
                    HorizontalDivider(color = dividerCol, thickness = 0.8.dp)
                    DrawerGroupedRow(
                        icon = Icons.Default.Favorite,
                        title = "რჩეულები",
                        badgeCount = favoriteProducts.size,
                        primaryTextCol = primaryTextCol,
                        iconTint = iconMutedCol,
                        chevronCol = chevronCol,
                        brandOrange = brandOrange,
                        testTag = "drawer_favorites_row",
                        onClick = {
                            showAllFavoritesDialog = true
                        }
                    )
                    HorizontalDivider(color = dividerCol, thickness = 0.8.dp)
                    DrawerGroupedRow(
                        icon = Icons.Default.ShoppingCart,
                        title = "კალათა",
                        badgeCount = cartItemsCount,
                        primaryTextCol = primaryTextCol,
                        iconTint = iconMutedCol,
                        chevronCol = chevronCol,
                        brandOrange = brandOrange,
                        testTag = "drawer_cart_row",
                        onClick = {
                            onSelectTab(5)
                            onCloseDrawer()
                        }
                    )
                }
            }

            // =========================================================================
            // 4. HELP SECTION (🎧 დახმარება)
            // =========================================================================
            DrawerSectionHeader(
                icon = Icons.Default.HeadsetMic,
                title = "დახმარება",
                brandOrange = brandOrange
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_help_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DrawerGroupedRow(
                        icon = Icons.Default.Chat,
                        title = "დახმარება და კონტაქტი",
                        badgeCount = 0,
                        primaryTextCol = primaryTextCol,
                        iconTint = iconMutedCol,
                        chevronCol = chevronCol,
                        brandOrange = brandOrange,
                        testTag = "drawer_help_contact_row",
                        onClick = { showHelpContactDialog = true }
                    )
                    HorizontalDivider(color = dividerCol, thickness = 0.8.dp)
                    DrawerGroupedRow(
                        icon = Icons.Default.LocationOn,
                        title = "სერვისცენტრი",
                        badgeCount = 0,
                        primaryTextCol = primaryTextCol,
                        iconTint = iconMutedCol,
                        chevronCol = chevronCol,
                        brandOrange = brandOrange,
                        testTag = "drawer_service_center_row",
                        onClick = { showServiceCenterDialog = true }
                    )
                }
            }

            // =========================================================================
            // 5. INFORMATION SECTION (ℹ️ ინფორმაცია)
            // =========================================================================
            DrawerSectionHeader(
                icon = Icons.Default.Info,
                title = "ინფორმაცია",
                brandOrange = brandOrange
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_information_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DrawerGroupedRow(
                        icon = Icons.Default.Description,
                        title = "წესები და პირობები",
                        badgeCount = 0,
                        primaryTextCol = primaryTextCol,
                        iconTint = iconMutedCol,
                        chevronCol = chevronCol,
                        brandOrange = brandOrange,
                        testTag = "drawer_terms_row",
                        onClick = { activeLegalPolicyTopic = LegalPolicyTopic.TERMS_AND_CONDITIONS }
                    )
                    HorizontalDivider(color = dividerCol, thickness = 0.8.dp)
                    DrawerGroupedRow(
                        icon = Icons.Default.VerifiedUser,
                        title = "კონფიდენციალურობა",
                        badgeCount = 0,
                        primaryTextCol = primaryTextCol,
                        iconTint = iconMutedCol,
                        chevronCol = chevronCol,
                        brandOrange = brandOrange,
                        testTag = "drawer_privacy_row",
                        onClick = { activeLegalPolicyTopic = LegalPolicyTopic.PRIVACY_POLICY }
                    )
                    HorizontalDivider(color = dividerCol, thickness = 0.8.dp)
                    DrawerGroupedRow(
                        icon = Icons.Default.CreditCard,
                        title = "გადახდა და დაბრუნება",
                        badgeCount = 0,
                        primaryTextCol = primaryTextCol,
                        iconTint = iconMutedCol,
                        chevronCol = chevronCol,
                        brandOrange = brandOrange,
                        testTag = "drawer_payment_return_row",
                        onClick = { activeLegalPolicyTopic = LegalPolicyTopic.RETURN_AND_REFUND_POLICY }
                    )
                }
            }

            // =========================================================================
            // 6. SETTINGS ROW (⚙️ პარამეტრები)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBgColor,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showSettingsDialog = true }
                    .testTag("drawer_settings_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "პარამეტრები",
                            tint = iconMutedCol,
                            modifier = Modifier.size(20.dp)
                        )
                        androidx.compose.material3.Text(
                            text = "პარამეტრები",
                            color = primaryTextCol,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = chevronCol,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // =========================================================================
            // 7. ADMINISTRATOR ROW (ONLY for verified sole admin: beqa1994@gmail.com)
            // =========================================================================
            if (isAuthorizedAdminUi) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF1C1613) else Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF5C3418) else brandOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onCloseDrawer()
                            onOpenAdminPanel()
                        }
                        .testTag("admin_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "ადმინისტრატორი",
                                tint = brandOrange,
                                modifier = Modifier.size(21.dp)
                            )
                            androidx.compose.material3.Text(
                                text = "ადმინისტრატორი",
                                color = if (isDark) Color(0xFFFFB366) else Color(0xFFC2410C),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "გადასვლა",
                            tint = if (isDark) Color(0xFFFFB366) else brandOrange,
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("drawer_admin_arrow_icon")
                        )
                    }
                }
            }

            // =========================================================================
            // 8. APPLICATION INFORMATION ROW (BSTORE / აპლიკაციის ვერსია + real vX.Y)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onSelectTab(7)
                        onCloseDrawer()
                    }
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .testTag("drawer_item_7"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF1A1E28) else Color(0xFFE2E8F0),
                        border = BorderStroke(1.dp, borderCol),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "აპლიკაციის ვერსია",
                                tint = secondaryTextCol,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Column {
                        androidx.compose.material3.Text(
                            text = "BSTORE",
                            color = primaryTextCol,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        androidx.compose.material3.Text(
                            text = "აპლიკაციის ვერსია",
                            color = secondaryTextCol,
                            fontSize = 11.sp
                        )
                    }
                }

                androidx.compose.material3.Text(
                    text = "v$installedVersionName",
                    color = secondaryTextCol,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("drawer_app_version_text")
                )
            }

            // =========================================================================
            // 9. LOGOUT BUTTON (გასვლა — restrained red outline & icon)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF1B1316) else Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFF87171)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        coroutineScope.launch {
                            if (currentUser != null) {
                                AuthAndOrderManager.logoutSafely(context)
                                currentUser = null
                                Toast.makeText(context, "სისტემიდან გამოსვლა წარმატებულია", Toast.LENGTH_SHORT).show()
                                onCloseDrawer()
                            } else {
                                onSelectTab(4)
                                onCloseDrawer()
                            }
                        }
                    }
                    .testTag("drawer_logout_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "გასვლა",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text(
                        text = "გასვლა",
                        color = Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Safe bottom padding ensuring logout button is never overlapped by gesture or nav bar
            Spacer(modifier = Modifier.navigationBarsPadding().height(20.dp))
        }
    }

    // =========================================================================
    // REAL DIALOGS LAUNCHED FROM THE SIDE NAVIGATION DRAWER
    // =========================================================================
    if (showEditProfileDialog && currentUser != null) {
        EditProfileDialog(
            user = currentUser!!,
            onDismiss = { showEditProfileDialog = false },
            onSaved = { updated ->
                currentUser = updated
                showEditProfileDialog = false
                Toast.makeText(context, "პროფილი წარმატებით განახლდა", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAllOrdersDialog && currentUser != null) {
        AllOrdersDialog(
            orders = userOrders,
            products = products,
            initialFilter = "აქტიური",
            onDismiss = { showAllOrdersDialog = false },
            onSelectOrder = { ord ->
                showAllOrdersDialog = false
                selectedOrderForDetail = ord
            },
            onOpenPaymentInstructions = { ord ->
                showAllOrdersDialog = false
                selectedOrderForPaymentInstructions = ord
            }
        )
    }

    if (selectedOrderForDetail != null) {
        val ord = selectedOrderForDetail!!
        val canShowInstr = PaymentMethodType.fromCode(ord.paymentMethodCode, ord.paymentMethod) == PaymentMethodType.BANK_TRANSFER &&
            BstorePaymentManager.canUserAccessOrderPaymentInstructions(context, ord)
        OrderDetailModal(
            order = ord,
            onClose = { selectedOrderForDetail = null },
            onOpenPaymentInstructions = if (canShowInstr) {
                {
                    selectedOrderForDetail = null
                    selectedOrderForPaymentInstructions = ord
                }
            } else null,
            onDelete = {
                AuthAndOrderManager.deleteOrder(context, ord.id)
                selectedOrderForDetail = null
            }
        )
    }

    if (selectedOrderForPaymentInstructions != null) {
        OrderPaymentInstructionsDialog(
            order = selectedOrderForPaymentInstructions!!,
            onDismiss = { selectedOrderForPaymentInstructions = null },
            onOrderUpdated = { updated ->
                selectedOrderForPaymentInstructions = updated
            }
        )
    }

    if (showAllFavoritesDialog) {
        AllFavoritesDialog(
            favoriteProducts = favoriteProducts,
            onDismiss = { showAllFavoritesDialog = false },
            onProductClick = { item ->
                showAllFavoritesDialog = false
                onCloseDrawer()
                onProductClick(item)
            },
            onAddToCart = onAddToCart,
            onRemoveFavorite = onToggleFavorite
        )
    }

    if (showHelpContactDialog) {
        DrawerHelpAndContactDialog(
            onDismiss = { showHelpContactDialog = false },
            onOpenAiAssistant = {
                showHelpContactDialog = false
                onCloseDrawer()
                onOpenAiAssistant()
            }
        )
    }

    if (showServiceCenterDialog) {
        DrawerServiceCenterDialog(
            onDismiss = { showServiceCenterDialog = false },
            onOpenDiagnostics = {
                showServiceCenterDialog = false
                onCloseDrawer()
                onOpenDiagnostics()
            }
        )
    }

    if (activeLegalPolicyTopic != null) {
        BstoreLegalPoliciesDialog(
            initialTopic = activeLegalPolicyTopic!!,
            onDismiss = { activeLegalPolicyTopic = null }
        )
    }

    if (showSettingsDialog) {
        SecurityAndSettingsDialog(
            user = currentUser,
            onDismiss = { showSettingsDialog = false },
            onOpenChangePassword = {
                showSettingsDialog = false
                showChangePasswordDialog = true
            },
            onOpenDeleteAccount = {
                showSettingsDialog = false
                showDeleteAccountDialog = true
            }
        )
    }

    if (showChangePasswordDialog && currentUser != null) {
        ChangeUserPasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onSuccess = {
                showChangePasswordDialog = false
                Toast.makeText(context, "პაროლი წარმატებით შეიცვალა", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountConfirmationDialog(
            isDeleting = isDeletingAccount,
            onDismiss = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            onConfirmDelete = {
                coroutineScope.launch {
                    isDeletingAccount = true
                    AuthAndOrderManager.deleteUserAccount(context)
                    isDeletingAccount = false
                    showDeleteAccountDialog = false
                    currentUser = null
                    Toast.makeText(context, "ანგარიში და მონაცემები წაიშალა", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
private fun DrawerSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    brandOrange: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = brandOrange,
            modifier = Modifier.size(18.dp)
        )
        androidx.compose.material3.Text(
            text = title,
            color = brandOrange,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DrawerGroupedRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    badgeCount: Int,
    primaryTextCol: Color,
    iconTint: Color,
    chevronCol: Color,
    brandOrange: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            androidx.compose.material3.Text(
                text = title,
                color = primaryTextCol,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (badgeCount > 0) {
                val badgeText = "$badgeCount"
                val isSingleDigit = badgeText.length == 1
                Surface(
                    shape = CircleShape,
                    color = brandOrange,
                    modifier = if (isSingleDigit) Modifier.size(20.dp) else Modifier.height(20.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().padding(horizontal = if (isSingleDigit) 0.dp else 6.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = chevronCol,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun DrawerHelpAndContactDialog(
    onDismiss: () -> Unit,
    onOpenAiAssistant: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = AppPreferencesManager.isDarkMode
    val contactConfig = ContactAndSocialManager.currentConfig
    val activePhones = contactConfig.phoneNumbers.filter { it.isEnabled }.sortedBy { it.displayOrder }
    val activeSocials = contactConfig.socialLinks.filter { it.isEnabled }.sortedBy { it.displayOrder }
    var showDirectContacts by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF161922) else Color.White,
            border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp)
                .testTag("drawer_help_contact_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showDirectContacts) {
                            IconButton(
                                onClick = { showDirectContacts = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "უკან",
                                    tint = Color(0xFFFF6B00),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        androidx.compose.material3.Text(
                            text = if (showDirectContacts) "📞 კონტაქტი" else "🎧 დახმარება და კონტაქტი",
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = Color.Gray)
                    }
                }

                if (!showDirectContacts) {
                    androidx.compose.material3.Text(
                        text = "აირჩიეთ დახმარების სასურველი ფორმა:",
                        color = if (isDark) Color(0xFF9BA3B2) else Color(0xFF64748B),
                        fontSize = 12.5.sp
                    )

                    // Option 1: AI Assistant
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1E2430) else Color(0xFFFFF7ED),
                        border = BorderStroke(1.2.dp, Color(0xFFFF6B00)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenAiAssistant()
                            }
                            .testTag("help_option_ai_assistant")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFF6B00).copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    androidx.compose.material3.Text("🤖", fontSize = 22.sp)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                androidx.compose.material3.Text(
                                    text = "AI ასისტენტი",
                                    color = Color(0xFFFF7A00),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                androidx.compose.material3.Text(
                                    text = "დაგეხმარებით პროდუქტის შერჩევაში და კითხვებზე პასუხის მიღებაში.",
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Option 2: Direct Contact
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF1E222D) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2B3140) else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDirectContacts = true }
                            .testTag("help_option_contacts")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF262C3A) else Color(0xFFE2E8F0),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                androidx.compose.material3.Text(
                                    text = "კონტაქტი",
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                androidx.compose.material3.Text(
                                    text = "დაგვიკავშირდით დამატებითი ინფორმაციისა და დახმარებისთვის.",
                                    color = if (isDark) Color(0xFF9BA3B2) else Color(0xFF64748B),
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    androidx.compose.material3.Text(
                        text = "დაგვიკავშირდით ტელეფონით ან სოციალური არხებით:",
                        color = if (isDark) Color(0xFF9BA3B2) else Color(0xFF64748B),
                        fontSize = 12.sp
                    )

                    activePhones.forEach { phone ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1E222D) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF2B3140) else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { ContactAndSocialManager.openDialer(context, phone.number) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    androidx.compose.material3.Text(
                                        text = phone.label.ifBlank { "ცხელი ხაზი" },
                                        color = if (isDark) Color(0xFF9BA3B2) else Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                    androidx.compose.material3.Text(
                                        text = phone.number,
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFF6B00)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "დარეკვა",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (activeSocials.isNotEmpty()) {
                        androidx.compose.material3.Text(
                            text = "სოციალური ქსელები:",
                            color = if (isDark) Color(0xFF9BA3B2) else Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeSocials.forEach { social ->
                                val brandColor = Color(social.platform.brandColorHex)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = brandColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, brandColor.copy(alpha = 0.55f)),
                                    modifier = Modifier.clickable {
                                        ContactAndSocialManager.openSocialLink(context, social)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        SocialPlatformBrandIcon(
                                            platform = social.platform,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        androidx.compose.material3.Text(
                                            text = social.title.ifBlank { social.platform.titleKa },
                                            color = if (isDark) Color.White else Color(0xFF0F172A),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerServiceCenterDialog(
    onDismiss: () -> Unit,
    onOpenDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = AppPreferencesManager.isDarkMode
    val locationConfig = ServiceCenterLocationManager.currentConfig
    val contactConfig = ContactAndSocialManager.currentConfig
    val primaryPhone = locationConfig.phone.ifBlank {
        contactConfig.phoneNumbers.firstOrNull { it.isEnabled }?.number ?: AppBrandConfig.SUPPORT_PHONE
    }

    val serviceCategories = if (locationConfig.services.isNotEmpty()) {
        locationConfig.services
    } else {
        listOf(
            "ტელეფონის აქსესუარები",
            "პროგრამული მომსახურება",
            "ტელეფონის პროგრამული აღდგენა და განახლება",
            "პროგრამირება და განბლოკვა",
            "ტელეფონების შეკეთება",
            "სხვა ტექნიკური მომსახურება"
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF161922) else Color.White,
            border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(12.dp)
                .testTag("drawer_service_center_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            androidx.compose.material3.Text(
                                text = locationConfig.centerName.ifBlank { "BEKASERVICE" },
                                color = Color(0xFFFF6B00),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFF6B00).copy(alpha = 0.16f)
                            ) {
                                androidx.compose.material3.Text(
                                    text = "სერვისცენტრი",
                                    color = Color(0xFFFF7A00),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(14.dp))
                            androidx.compose.material3.Text(
                                text = locationConfig.physicalAddress.ifBlank { "კოსტავას N10, ახალციხე" },
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = Color.Gray)
                    }
                }

                // სერვისების კომპაქტური ჩამონათვალი
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E222D) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF2B3140) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "სერვისები და მომსახურება:",
                            color = Color(0xFFFF9E40),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        serviceCategories.forEach { srv ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                androidx.compose.material3.Text("•", color = Color(0xFFFF7A00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                androidx.compose.material3.Text(
                                    text = srv,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }

                // სამუშაო საათები
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFA0A5B0), modifier = Modifier.size(15.dp))
                    androidx.compose.material3.Text(
                        text = "სამუშაო საათები: ${locationConfig.workingHours.ifBlank { "10:00 – 20:00 (ყოველდღე)" }}",
                        color = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                        fontSize = 11.5.sp
                    )
                }

                // 2 Action Buttons: დარეკვა & რუკაზე ნახვა (ზუსტი Google Maps Share ბმულით!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFF6B00),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clickable { ServiceCenterLocationManager.openDialer(context, primaryPhone) }
                            .testTag("service_center_call_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            androidx.compose.material3.Text(
                                text = "დარეკვა",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF1E222D) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFFF6B00)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clickable {
                                ServiceCenterLocationManager.openOnMap(
                                    context = context,
                                    rawInput = locationConfig.googleMapsUrl,
                                    fallbackAddress = locationConfig.physicalAddress
                                )
                            }
                            .testTag("service_center_map_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            androidx.compose.material3.Text(
                                text = "რუკაზე ნახვა",
                                color = Color(0xFFFF6B00),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// ზოგადი შეტყობინებების გვერდი მომხმარებლისთვის (Notifications Center - Tab 6)
// ==============================================================================
@Composable
fun UserNotificationsScreen(
    cloudNotifications: List<PushNotificationRecord>,
    products: List<ProductItem>,
    onNavigateToTarget: (targetTab: Int, targetProductId: Int, targetOrderId: String) -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp),
    onNavigateToProductDoc: ((targetTab: Int, targetProductId: Int, targetProductDocId: String, targetOrderId: String) -> Unit)? = null,
    onNavigateDestination: ((NotificationDestination) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestoreRepo = remember { runCatching { FirestoreRepository(context) }.getOrNull() }
    val version = BekaNotificationManager.stateVersion.intValue
    val authVersion = AuthAndOrderManager.authStateVersion.intValue
    var selectedFilter by remember { mutableStateOf("all") }

    LaunchedEffect(Unit) {
        BekaNotificationManager.purgeSyntheticNotificationsIfNeeded(context, force = true)
    }

    val allVisibleNotifications = remember(cloudNotifications, version, authVersion) {
        BekaNotificationManager.getVisibleUserNotifications(context, cloudNotifications)
    }

    val unreadCount = remember(allVisibleNotifications, version, authVersion) {
        allVisibleNotifications.count { !BekaNotificationManager.isNotificationRead(context, it.id) }
    }

    val filteredNotifications = remember(allVisibleNotifications, selectedFilter, version, authVersion) {
        when (selectedFilter) {
            "unread" -> allVisibleNotifications.filter { !BekaNotificationManager.isNotificationRead(context, it.id) }
            "product_offer" -> allVisibleNotifications.filter { it.type == "product" || it.type == "product_offer" }
            "orders" -> allVisibleNotifications.filter {
                it.type == "order_update" || it.type == "service_update" ||
                    it.orderId.isNotBlank() || it.orderNumber.isNotBlank()
            }
            "announcement" -> allVisibleNotifications.filter { it.type == "announcement" }
            else -> allVisibleNotifications
        }
    }

    val filterTabs = listOf(
        "all" to "ყველა (${allVisibleNotifications.size})",
        "unread" to "წაუკითხავი ($unreadCount)",
        "product_offer" to "🔥 პროდუქტები",
        "orders" to "📦 შეკვეთები",
        "announcement" to "📢 განცხადებები"
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
                                    val privateIds = allVisibleNotifications
                                        .filter { BekaNotificationManager.classifyRecord(it) == NotificationVisibility.PRIVATE_USER }
                                        .map { it.id }
                                    if (privateIds.isNotEmpty()) {
                                        coroutineScope.launch {
                                            privateIds.forEach { pid ->
                                                firestoreRepo?.markUserNotificationReadInCloud(pid)
                                            }
                                        }
                                    }
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

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("notif_filter_tabs_row")
                    ) {
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

        // სისტემური არხის სტატუსი და მწარმოებლის (Xiaomi/POCO, Samsung, Motorola) ფონური მიწოდების გზამკვლევი
        item(key = "notif_oem_delivery_guide") {
            val diagnostics = remember(version) { BekaNotificationManager.getDeliveryDiagnostics(context) }
            var showOemGuide by remember { mutableStateOf(!diagnostics.isFullyOperational) }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (diagnostics.isFullyOperational) Color(0xFF141820) else Color(0xFF281912),
                border = BorderStroke(
                    1.dp,
                    if (diagnostics.isFullyOperational) Color(0xFF262C3A) else Color(0xFFFF7A00).copy(alpha = 0.65f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("oem_notification_delivery_guide_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOemGuide = !showOemGuide }
                            .testTag("toggle_oem_delivery_guide_button"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (diagnostics.isFullyOperational) {
                                    "🔔 ფონური შეტყობინებები აქტიურია • გზამკვლევი (${diagnostics.manufacturerName})"
                                } else {
                                    "⚠️ შეტყობინებების არხი ან ნებართვა გამორთულია"
                                },
                                color = if (diagnostics.isFullyOperational) Color(0xFFE2E8F0) else Color(0xFFFFB067),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (showOemGuide) "დააჭირეთ დასაკეცად ▲" else "როგორ მივიღოთ შეტყობინებები დახურულ აპზე ▼",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.5.sp,
                                maxLines = 1
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!diagnostics.runtimePermissionGranted || !diagnostics.appNotificationsEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFF6A00),
                                    modifier = Modifier
                                        .clickable {
                                            BekaNotificationManager.openSystemNotificationSettings(context)
                                        }
                                        .testTag("enable_notifications_from_settings_button")
                                ) {
                                    Text(
                                        text = "შეტყობინებების ჩართვა",
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF7A00).copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clickable {
                                        BekaNotificationManager.openSystemNotificationSettings(
                                            context,
                                            BekaNotificationManager.CHANNEL_ID
                                        )
                                    }
                                    .testTag("open_notification_channel_settings_button")
                            ) {
                                Text(
                                    text = "არხის პარამეტრები",
                                    color = Color(0xFFFF9E40),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (showOemGuide) {
                        HorizontalDivider(color = Color(0xFF262C3A))
                        Text(
                            text = diagnostics.manufacturerTitleKa,
                            color = Color(0xFFFF9E40),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        diagnostics.manufacturerStepsKa.forEachIndexed { idx, step ->
                            Text(
                                text = "${idx + 1}. $step",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
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
                val targetProd = remember(notif.targetProductId, notif.targetProductDocId, products) {
                    products.find {
                        (notif.targetProductDocId.isNotBlank() &&
                            (it.firestoreDocId == notif.targetProductDocId || "prod_${it.id}" == notif.targetProductDocId)) ||
                            (notif.targetProductId > 0 && it.id == notif.targetProductId)
                    }
                }
                val destination = remember(notif) { BekaNotificationManager.resolveDestination(notif) }
                val resolvedOrderNumber = remember(notif.orderNumber, notif.title, notif.body) {
                    notif.orderNumber.ifBlank {
                        Regex("#BS-[A-Z0-9\\-]+").find("${notif.title} ${notif.body}")?.value.orEmpty()
                    }
                }
                val resolvedOrderId = remember(notif.orderId, resolvedOrderNumber) {
                    notif.orderId.ifBlank { resolvedOrderNumber }
                }
                val isOrderNotif = destination is NotificationDestination.CustomerOrderDetail ||
                    destination is NotificationDestination.ProductRequestDetail
                val effectiveTargetTab = when (destination) {
                    is NotificationDestination.CustomerOrderDetail, is NotificationDestination.ProductRequestDetail -> 4
                    is NotificationDestination.ProductDetail -> 1
                    is NotificationDestination.Discounts -> 2
                    is NotificationDestination.AppUpdate -> 7
                    is NotificationDestination.InAppTab -> destination.tabIndex
                    else -> if (isOrderNotif) 4 else notif.targetTab
                }
                val dateStr = remember(notif.createdAtMillis) {
                    SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
                        .format(Date(notif.createdAtMillis))
                }

                val performNavigation = {
                    BekaNotificationManager.markNotificationRead(context, notif.id)
                    if (BekaNotificationManager.classifyRecord(notif) == NotificationVisibility.PRIVATE_USER) {
                        coroutineScope.launch {
                            firestoreRepo?.markUserNotificationReadInCloud(notif.id)
                        }
                    }
                    if (onNavigateDestination != null) {
                        onNavigateDestination(destination)
                    } else if (onNavigateToProductDoc != null) {
                        onNavigateToProductDoc(
                            effectiveTargetTab,
                            notif.targetProductId,
                            notif.targetProductDocId,
                            resolvedOrderId
                        )
                    } else {
                        onNavigateToTarget(effectiveTargetTab, notif.targetProductId, resolvedOrderId)
                    }
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
                        .clickable { performNavigation() }
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

                        // სამიზნე გვერდის / შეკვეთის / პროდუქტის ბეიჯი
                        val badgeLabel = when (destination) {
                            is NotificationDestination.ProductDetail -> {
                                if (targetProd != null) "🔗 ${targetProd.title} (${targetProd.price})"
                                else if (notif.targetProductDocId.isNotBlank()) "🔗 პროდუქტი ${notif.targetProductDocId}"
                                else "🛍️ პროდუქტის დეტალები"
                            }
                            is NotificationDestination.Discounts -> "🏷️ ფასდაკლებები"
                            is NotificationDestination.CustomerOrderDetail -> {
                                if (destination.orderNumber.isNotBlank()) "📦 შეკვეთა ${destination.orderNumber}"
                                else if (resolvedOrderNumber.isNotBlank()) "📦 შეკვეთა $resolvedOrderNumber"
                                else "📦 ჩემი შეკვეთები"
                            }
                            is NotificationDestination.ProductRequestDetail -> {
                                if (destination.requestNumber.isNotBlank()) "📩 მოთხოვნა ${destination.requestNumber}"
                                else "📩 პროდუქტის მოთხოვნა"
                            }
                            is NotificationDestination.AppUpdate -> "📲 აპის განახლება"
                            is NotificationDestination.InAppTab -> "📍 ${BekaNotificationManager.targetTabLabel(destination.tabIndex)}"
                            is NotificationDestination.NotificationDetailModal -> "ℹ️ ინფორმაციული შეტყობინება"
                            is NotificationDestination.AdminOrderDetail -> "📦 შეკვეთა (ადმინი)"
                            is NotificationDestination.UnavailableOrFallback -> "⚠️ ${destination.reasonKa}"
                        }
                        val badgeColor = when (destination) {
                            is NotificationDestination.CustomerOrderDetail, is NotificationDestination.ProductRequestDetail -> Color(0xFF60A5FA)
                            is NotificationDestination.ProductDetail, is NotificationDestination.Discounts -> Color(0xFFFF7A00)
                            is NotificationDestination.AppUpdate -> Color(0xFFA855F7)
                            is NotificationDestination.UnavailableOrFallback -> Color(0xFFF87171)
                            else -> Color(0xFF4ADE80)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E222B)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = badgeColor,
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
                                    modifier = Modifier.clickable { performNavigation() }
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

/**
 * შეტყობინებების ნებართვის წინასწარი განმარტებითი დიალოგი (ქართულ ენაზე).
 * ჩნდება ონბორდინგის დასრულების შემდეგ, სანამ Android-ის სისტემური POST_NOTIFICATIONS დიალოგი გამოჩნდება.
 */
@Composable
fun BekaNotificationPermissionExplanationDialog(
    onEnableClick: () -> Unit,
    onNotNowClick: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onNotNowClick)

    CompositionLocalProvider(LocalPreserveWhiteText provides true) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Backdrop scrim as a sibling so its clickable semantics never merge/swallow dialog child testTags
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF06080C).copy(alpha = 0.76f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF14171E),
                border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.48f)),
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .widthIn(max = 380.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("notification_permission_explanation_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Bell Icon Badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF6A00).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.42f)),
                        modifier = Modifier
                            .size(58.dp)
                            .testTag("notification_permission_bell_icon")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "შეტყობინებები",
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Title
                    Text(
                        text = "არ გამოგრჩეს მნიშვნელოვანი სიახლეები",
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        softWrap = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notification_permission_dialog_title")
                    )

                    // Description
                    Text(
                        text = "ჩართე შეტყობინებები, რომ დროულად მიიღო ინფორმაცია შენი შეკვეთების სტატუსის, სიახლეებისა და მნიშვნელოვანი განახლებების შესახებ.",
                        color = Color(0xFFB4BCC8),
                        fontSize = 13.5.sp,
                        lineHeight = 19.5.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        softWrap = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notification_permission_dialog_description")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Primary Action: "შეტყობინებების ჩართვა"
                    Button(
                        onClick = onEnableClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6A00),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("notification_permission_enable_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "შეტყობინებების ჩართვა",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Secondary Action: "ახლა არა"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1C2029),
                        border = BorderStroke(1.dp, Color(0xFF2B313E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onNotNowClick)
                            .testTag("notification_permission_not_now_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "ახლა არა",
                                color = Color(0xFFA0A8B8),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
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
                                text = "BSTORE ოფიციალური ვერსიის კონტროლი",
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

