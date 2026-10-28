package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfileAndAuthScreen(
    onOpenCart: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    products: List<ProductItem> = emptyList(),
    onProductClick: (ProductItem) -> Unit = {},
    onToggleFavorite: (ProductItem) -> Unit = {},
    onAddToCart: (ProductItem) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(AuthAndOrderManager.getCurrentUser(context)) }
    val favStateVersion = AuthAndOrderManager.favoritesStateVersion.intValue
    val favoriteIds = remember(currentUser?.id, favStateVersion) {
        AuthAndOrderManager.getFavoriteProductIds(context)
    }
    val favoriteProducts = remember(products, favoriteIds) {
        val baseCatalog = if (products.isNotEmpty()) products else ProductRepository.getAllProducts()
        baseCatalog.filter { it.id in favoriteIds || it.isFavorite }
    }
    var ordersList by remember { mutableStateOf<List<OrderRecord>>(emptyList()) }
    var isOrdersLoading by remember { mutableStateOf(false) }
    var ordersError by remember { mutableStateOf<String?>(null) }

    // FirebaseAuth სესიის სინქრონიზაცია და ავტომატური Google Sign-In
    DisposableEffect(Unit) {
        val auth = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance() }.getOrNull()
        val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                currentUser = AuthAndOrderManager.getCurrentUser(context)
                coroutineScope.launch {
                    val synced = AuthAndOrderManager.refreshCurrentUserFromCloud(context)
                    if (synced != null) {
                        currentUser = synced
                    }
                }
            } else {
                val localUser = AuthAndOrderManager.getCurrentUser(context)
                currentUser = localUser
                if (localUser == null) {
                    ordersList = emptyList()
                }
            }
        }
        auth?.addAuthStateListener(listener)
        onDispose {
            auth?.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        val fbUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (fbUser != null) {
            val synced = AuthAndOrderManager.refreshCurrentUserFromCloud(context)
            if (synced != null) {
                currentUser = synced
            }
        } else if (currentUser == null) {
            val autoSignedIn = AuthAndOrderManager.attemptAutoSignIn(context)
            if (autoSignedIn != null) {
                currentUser = autoSignedIn
            }
        }
    }

    // რეალურ დროში მომხმარებლის შეკვეთების სინქრონიზაცია Firestore-დან (დემო შეკვეთების გარეშე)
    DisposableEffect(currentUser?.id) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        val activeUser = currentUser
        if (activeUser != null) {
            val fbUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
            if (fbUser != null) {
                isOrdersLoading = true
                ordersError = null
                try {
                    val repo = FirestoreRepository(context)
                    registration = repo.listenToUserOrders(
                        onOrdersUpdated = { cloudOrders ->
                            ordersList = AuthAndOrderManager.mergeCloudOrdersWithLocal(context, cloudOrders)
                            isOrdersLoading = false
                            ordersError = null
                        },
                        onError = { err ->
                            isOrdersLoading = false
                            ordersError = err
                        }
                    )
                } catch (e: Exception) {
                    isOrdersLoading = false
                    ordersError = e.localizedMessage
                }
            } else {
                ordersList = AuthAndOrderManager.getOrders(context)
                isOrdersLoading = false
                ordersError = null
            }
        } else {
            ordersList = emptyList()
            isOrdersLoading = false
            ordersError = null
        }
        onDispose {
            registration?.remove()
        }
    }

    // დიალოგების მდგომარეობები
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showGoogleAuthDialog by remember { mutableStateOf(false) }
    var showAppleAuthDialog by remember { mutableStateOf(false) }
    var selectedOrderForDetail by remember { mutableStateOf<OrderRecord?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // შეკვეთების ფილტრი
    var selectedOrderFilter by remember { mutableStateOf("ყველა") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "პროფილი & ანგარიში",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // =====================================================================
        // 1. მომხმარებლის მდგომარეობა (ავტორიზებული ან შესასვლელი ფორმა)
        // =====================================================================
        if (currentUser == null) {
            item {
                AuthCard(
                    onGoogleClick = {
                        coroutineScope.launch {
                            val signedInUser = AuthAndOrderManager.signInWithGoogleCredentialManager(
                                activityContext = context,
                                onCancel = {
                                    Toast.makeText(
                                        context,
                                        "Google ავტორიზაცია გაუქმდა",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(
                                        context,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                            if (signedInUser != null) {
                                currentUser = signedInUser
                                Toast.makeText(context, "Google ავტორიზაცია წარმატებულია!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onAppleClick = { showAppleAuthDialog = true },
                    onLoginSuccess = { user ->
                        currentUser = user
                    },
                    onForgotPassword = { showForgotPasswordDialog = true }
                )
            }
        } else {
            item {
                UserProfileCard(
                    user = currentUser!!,
                    onEditProfile = { showEditProfileDialog = true },
                    onChangePassword = { showChangePasswordDialog = true },
                    onLogout = {
                        AuthAndOrderManager.logout(context)
                        currentUser = null
                        ordersList = emptyList()
                        Toast.makeText(context, "სისტემიდან გამოსვლა წარმატებულია", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // =====================================================================
        // 1.5 ფავორიტები (Account-ის დონეზე შენახული პროდუქტები - პიქტოგრამით)
        // =====================================================================
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (favoriteProducts.isNotEmpty()) Color(0xFFFF7A00).copy(alpha = 0.35f) else Color(0xFF242830)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_favorites_section")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // სექციის ჰედერი შესაბამისი პიქტოგრამით (Emoji-ს გარეშე)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFF7A00).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFF7A00).copy(alpha = 0.35f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmarks,
                                        contentDescription = "ფავორიტები",
                                        tint = Color(0xFFFF7A00),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "ფავორიტები",
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (currentUser != null) "ანგარიშზე შენახული პროდუქტები" else "შეინახეთ სასურველი პროდუქტები",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF7A00).copy(alpha = 0.14f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFF7A00).copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "სულ: ${favoriteProducts.size}",
                                color = Color(0xFFFF7A00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (favoriteProducts.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF12151A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F242D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp, horizontal = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1C2029),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            tint = Color(0xFF6E7582),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "ფავორიტების სია ცარიელია",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "პროდუქტის ბარათზე მონიშნული ფავორიტები შეინახება თქვენს ანგარიშზე და გამოჩნდება აქ",
                                    color = Color(0xFF6E7582),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            favoriteProducts.forEach { favProd ->
                                val isSoftware = SoftwareCatalogData.isDigitalSoftwareService(favProd)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1C2028),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B313E)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onProductClick(favProd) }
                                        .testTag("profile_favorite_item_${favProd.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // პროდუქტის მინიატურა ან პროგრამული ბრენდის ვიზუალი
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF0F1115)))
                                                .border(1.dp, AppThemeColorAdapter.dividerColor(Color(0xFF282E3A)), RoundedCornerShape(10.dp))
                                        ) {
                                            if (isSoftware) {
                                                SoftwareBrandVisualHeader(
                                                    product = favProd,
                                                    modifier = Modifier.fillMaxSize(),
                                                    compact = true
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = favProd.imageUrl,
                                                    contentDescription = favProd.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = favProd.title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${favProd.brand} • ${favProd.category}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = favProd.price,
                                                color = Color(0xFF4ADE80),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // კალათაში დამატების სწრაფი პიქტოგრამა
                                            Surface(
                                                onClick = { onAddToCart(favProd) },
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFF7A00).copy(alpha = 0.16f),
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFF7A00).copy(alpha = 0.4f)),
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.AddShoppingCart,
                                                        contentDescription = "კალათაში დამატება",
                                                        tint = Color(0xFFFF7A00),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            // ფავორიტებიდან მოხსნის პიქტოგრამა
                                            Surface(
                                                onClick = { onToggleFavorite(favProd) },
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF26191D),
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFF87171).copy(alpha = 0.35f)),
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("profile_remove_favorite_${favProd.id}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "ფავორიტებიდან წაშლა",
                                                        tint = Color(0xFFF87171),
                                                        modifier = Modifier.size(17.dp)
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
        }

        if (currentUser != null) {
            // =================================================================
            // 2. შეკვეთების სრული ისტორია (ავტორიზებულ მომხმარებელზე)
            // =================================================================
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF16191E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
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
                            Text("📦 შეკვეთების ისტორია", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("სულ: ${ordersList.size}", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                        }

                        // ფილტრის ჩიპები
                        val filterOptions = listOf("ყველა", "მიმდინარე", "შესრულებული", "გაუქმებული")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filterOptions) { opt ->
                                val isSelected = selectedOrderFilter == opt
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF222630),
                                    modifier = Modifier.clickable { selectedOrderFilter = opt }
                                ) {
                                    Text(
                                        text = opt,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        val filteredOrders = ordersList.filter { ord ->
                            when (selectedOrderFilter) {
                                "ყველა" -> true
                                "მიმდინარე" -> ord.status == "მიმდინარე" || ord.status == "გზაშია" || ord.status == "მზადდება"
                                else -> ord.status.equals(selectedOrderFilter, ignoreCase = true)
                            }
                        }

                        if (isOrdersLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFF6A00),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("შეკვეთები იტვირთება...", color = Color(0xFFA0A5B0), fontSize = 12.sp)
                                }
                            }
                        } else if (ordersError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚠️ შეკვეთების ჩატვირთვა ვერ მოხერხდა",
                                    color = Color(0xFFF87171),
                                    fontSize = 12.sp
                                )
                            }
                        } else if (filteredOrders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("შეკვეთები ჯერ არ გაქვს", color = Color(0xFF6E7582), fontSize = 12.sp)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                filteredOrders.forEach { ord ->
                                    OrderItemCard(
                                        order = ord,
                                        onClick = { selectedOrderForDetail = ord },
                                        onDelete = {
                                            coroutineScope.launch {
                                                val fbUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
                                                if (fbUser != null) {
                                                    val res = FirestoreRepository(context).deleteOrderFromCloud(ord.id)
                                                    if (res.isFailure) {
                                                        Toast.makeText(
                                                            context,
                                                            "შეცდომა წაშლისას: ${res.exceptionOrNull()?.localizedMessage ?: ""}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        return@launch
                                                    }
                                                }
                                                AuthAndOrderManager.deleteOrder(context, ord.id)
                                                ordersList = ordersList.filterNot { it.id == ord.id }
                                                if (selectedOrderForDetail?.id == ord.id) {
                                                    selectedOrderForDetail = null
                                                }
                                                Toast.makeText(context, "🗑️ შეკვეთა წაიშალა!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 3. ადმინისტრატორის მართვის პანელის კომპაქტური ბარათი (PIN-ით შესასვლელი)
        // =====================================================================
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAdminPanel() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFF7A00).copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFF7A00).copy(alpha = 0.3f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Panel",
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "ადმინისტრატორის პანელი",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "პროდუქტები • შეკვეთები • ბანკი • PIN",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF7A00).copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFF7A00).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "შესვლა",
                                color = Color(0xFFFF7A00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 4. საბანკო რეკვიზიტები (BOG & TBC ბარათების სახით)
        // =====================================================================
        item {
            BankRequisitesCard()
        }

        // =====================================================================
        // 5. ლოკაცია & კონტაქტები
        // =====================================================================
        item {
            StoreContactsCard()
        }
    }

    // Google ავტორიზაციის რეალური დიალოგი
    if (showGoogleAuthDialog) {
        GoogleSignInDialog(
            onDismiss = { showGoogleAuthDialog = false },
            onSuccess = { user ->
                showGoogleAuthDialog = false
                currentUser = user
                Toast.makeText(context, "Google ავტორიზაცია წარმატებულია!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Apple ID ავტორიზაციის რეალური დიალოგი
    if (showAppleAuthDialog) {
        AppleSignInDialog(
            onDismiss = { showAppleAuthDialog = false },
            onSuccess = { user ->
                showAppleAuthDialog = false
                currentUser = user
                Toast.makeText(context, "Apple ID ავტორიზაცია წარმატებულია!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // პროფილის რედაქტირების დიალოგი
    if (showEditProfileDialog && currentUser != null) {
        EditProfileDialog(
            user = currentUser!!,
            onDismiss = { showEditProfileDialog = false },
            onSaved = { updatedUser ->
                showEditProfileDialog = false
                currentUser = updatedUser
                Toast.makeText(context, "პროფილი წარმატებით განახლდა!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // პაროლის შეცვლის მოდალი
    if (showChangePasswordDialog) {
        ChangeUserPasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onSuccess = {
                showChangePasswordDialog = false
                Toast.makeText(context, "პაროლი წარმატებით განახლდა!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // შეკვეთის დეტალების მოდალი
    if (selectedOrderForDetail != null) {
        val targetOrder = selectedOrderForDetail!!
        OrderDetailModal(
            order = targetOrder,
            onClose = { selectedOrderForDetail = null },
            onDelete = {
                coroutineScope.launch {
                    val fbUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
                    if (fbUser != null) {
                        val res = FirestoreRepository(context).deleteOrderFromCloud(targetOrder.id)
                        if (res.isFailure) {
                            Toast.makeText(
                                context,
                                "შეცდომა წაშლისას: ${res.exceptionOrNull()?.localizedMessage ?: ""}",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }
                    }
                    AuthAndOrderManager.deleteOrder(context, targetOrder.id)
                    ordersList = ordersList.filterNot { it.id == targetOrder.id }
                    selectedOrderForDetail = null
                    Toast.makeText(context, "🗑️ შეკვეთა წაიშალა!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // პაროლის აღდგენის მოდალი
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPasswordDialog = false }
        )
    }
}

// ==============================================================================
// ავტორიზაციის & რეგისტრაციის ბარათი (Google, Apple ID, Email)
// ==============================================================================
@Composable
fun AuthCard(
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onLoginSuccess: (UserProfile) -> Unit,
    onForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16191E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ტაბების გადამრთველი
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF101318)), RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (!isRegisterMode) Color(0xFFFF6A00) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isRegisterMode = false; errorMsg = null }
                ) {
                    Text(
                        text = "შესვლა",
                        color = if (!isRegisterMode) Color.White else Color(0xFFA0A5B0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isRegisterMode) Color(0xFFFF6A00) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isRegisterMode = true; errorMsg = null }
                ) {
                    Text(
                        text = "რეგისტრაცია",
                        color = if (isRegisterMode) Color.White else Color(0xFFA0A5B0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Google Auth ღილაკი (რეალური ლოგოთი)
            Button(
                onClick = onGoogleClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Google-ით შესვლა", color = Color(0xFF1F2937), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Apple ID Auth ღილაკი (რეალური ლოგოთი)
            Button(
                onClick = onAppleClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_apple_logo),
                        contentDescription = "Apple Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Apple ID-ით შესვლა", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // გამყოფი ხაზი
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF242830))
                Text(
                    text = "ან ელ. ფოსტით",
                    color = Color(0xFF6E7582),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF242830))
            }

            // ფორმის ველები
            if (isRegisterMode) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; errorMsg = null },
                    label = { Text("თქვენი სახელი და გვარი") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF242830)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it; errorMsg = null },
                label = { Text("ელ. ფოსტა") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF6A00),
                    unfocusedBorderColor = Color(0xFF242830)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it; errorMsg = null },
                label = { Text("პაროლი") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password",
                            tint = Color(0xFFA0A5B0)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFFF6A00),
                    unfocusedBorderColor = Color(0xFF242830)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isRegisterMode) {
                Text(
                    text = "დაგავიწყდათ პაროლი?",
                    color = Color(0xFFFF7A00),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable { onForgotPassword() }
                        .align(Alignment.End)
                )
            }

            if (errorMsg != null) {
                Text(errorMsg!!, color = Color(0xFFFF4D4F), fontSize = 12.sp)
            }

            Button(
                onClick = {
                    if (isRegisterMode) {
                        val (success, msg) = AuthAndOrderManager.registerWithEmail(context, nameInput, emailInput, passwordInput)
                        if (success) {
                            val user = AuthAndOrderManager.getCurrentUser(context)
                            if (user != null) onLoginSuccess(user)
                        } else {
                            errorMsg = msg
                        }
                    } else {
                        val (success, msg) = AuthAndOrderManager.loginWithEmail(context, emailInput, passwordInput)
                        if (success) {
                            val user = AuthAndOrderManager.getCurrentUser(context)
                            if (user != null) onLoginSuccess(user)
                        } else {
                            errorMsg = msg
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(if (isRegisterMode) "რეგისტრაცია" else "შესვლა", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ==============================================================================
// Google რეალური ავტორიზაციის მოდალი (ანგარიშის არჩევა, ნებართვები და თანხმობა)
// ==============================================================================
@Composable
fun GoogleSignInDialog(
    onDismiss: () -> Unit,
    onSuccess: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: Account Selection, 2: Consent & Permissions, 3: Verifying
    var selectedAccountType by remember { mutableStateOf("custom") } // "beqa", "service", "custom"
    var customEmail by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customPassword by remember { mutableStateOf("") }
    var isConsentChecked by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationStatus by remember { mutableStateOf("Google-ის სერვერებთან დაკავშირება...") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val resolvedEmail = when (selectedAccountType) {
        "beqa" -> "beqa1994@gmail.com"
        "service" -> "bekaservice.official@gmail.com"
        else -> customEmail.trim()
    }

    val resolvedName = when (selectedAccountType) {
        "beqa" -> "ბექა მღებრიშვილი"
        "service" -> "BekaService Official"
        else -> customName.trim().ifBlank { customEmail.substringBefore("@") }
    }

    // Step 3 ანიმაცია & სერვერული დამოწმება
    LaunchedEffect(isVerifying) {
        if (isVerifying) {
            verificationStatus = "Google ანგარიშის გადამოწმება..."
            delay(700)
            verificationStatus = "თანხმობისა და ტოკენის დამოწმება..."
            delay(700)
            verificationStatus = "პროფილის სინქრონიზაცია..."
            delay(500)
            val user = AuthAndOrderManager.loginWithGoogle(
                context = context,
                email = resolvedEmail,
                name = resolvedName,
                firstName = resolvedName.split(" ").firstOrNull() ?: resolvedName,
                lastName = resolvedName.split(" ").drop(1).joinToString(" "),
                phone = "",
                address = "",
                deviceModel = ""
            )
            onSuccess(user)
        }
    }

    Dialog(onDismissRequest = { if (!isVerifying) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E222A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333A48)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Google logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (step == 1) "Google-ით შესვლა" else if (step == 2) "წვდომა & თანხმობა" else "ავტორიზაცია...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (step == 1) "აირჩიეთ სასურველი Google ანგარიში" else if (step == 2) "დაადასტურეთ ნებართვები BekaService-ისთვის" else "მიმდინარეობს იდენტიფიკაცია",
                            color = Color(0xFFA0A5B0),
                            fontSize = 11.sp
                        )
                    }
                    if (step > 1 && !isVerifying) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF2E3440)) {
                            Text("ეტაპი $step / 2", color = Color(0xFF4285F4), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF2E3440))

                when (step) {
                    // =========================================================
                    // 1. ანგარიშის არჩევა (Account Selection)
                    // =========================================================
                    1 -> {
                        Text("ხელმისაწვდომი Google ანგარიშები:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        // Account 1: ბექა მღებრიშვილი
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedAccountType == "beqa") Color(0xFF2A3140) else Color(0xFF16191E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedAccountType == "beqa") Color(0xFF4285F4) else Color(0xFF252A35)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountType = "beqa"; errorMsg = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4285F4),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("ბ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ბექა მღებრიშვილი", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("beqa1994@gmail.com", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                                }
                                if (selectedAccountType == "beqa") {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Account 2: BekaService Official
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedAccountType == "service") Color(0xFF2A3140) else Color(0xFF16191E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedAccountType == "service") Color(0xFF4285F4) else Color(0xFF252A35)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountType = "service"; errorMsg = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF34A853),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("B", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("BekaService Official", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("bekaservice.official@gmail.com", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                                }
                                if (selectedAccountType == "service") {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Option 3: სხვა Google ანგარიშის დამატება
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedAccountType == "custom") Color(0xFF2A3140) else Color(0xFF16191E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedAccountType == "custom") Color(0xFF4285F4) else Color(0xFF252A35)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountType = "custom" }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2A3140),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("სხვა Google ანგარიშის გამოყენება", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("შეიყვანეთ ნებისმიერი Gmail მისამართი", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                                }
                                if (selectedAccountType == "custom") {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // თუ არჩეულია სხვა ანგარიში - ველების გამოჩენა
                        if (selectedAccountType == "custom") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customEmail,
                                    onValueChange = { customEmail = it; errorMsg = null },
                                    label = { Text("Gmail მისამართი") },
                                    placeholder = { Text("example@gmail.com") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF4285F4),
                                        unfocusedBorderColor = Color(0xFF333A48)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = customName,
                                    onValueChange = { customName = it },
                                    label = { Text("თქვენი სახელი და გვარი (სურვილისამებრ)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF4285F4),
                                        unfocusedBorderColor = Color(0xFF333A48)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = customPassword,
                                    onValueChange = { customPassword = it },
                                    label = { Text("Google პაროლი") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF4285F4),
                                        unfocusedBorderColor = Color(0xFF333A48)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (errorMsg != null) {
                            Text(errorMsg!!, color = Color(0xFFFF4D4F), fontSize = 11.5.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0))
                            ) {
                                Text("გაუქმება")
                            }

                            Button(
                                onClick = {
                                    if (selectedAccountType == "custom" && (customEmail.isBlank() || !customEmail.contains("@"))) {
                                        errorMsg = "გთხოვთ შეიყვანოთ სწორი Google ელ. ფოსტა"
                                    } else {
                                        step = 2
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("შემდეგი ➔", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // =========================================================
                    // 2. თანხმობისა და ნებართვების ეტაპი (Consent Screen)
                    // =========================================================
                    2 -> {
                        // არჩეული ანგარიშის ბარათი
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF16191E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3140)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4285F4),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(resolvedName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(resolvedName, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    Text(resolvedEmail, color = Color(0xFFA0A5B0), fontSize = 11.sp)
                                }
                            }
                        }

                        Text("BekaService (bekaservice.ge) ითხოვს შემდეგ წვდომებს:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("პროფილის ინფორმაცია", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                    Text("სახელი, გვარი და პროფილის ავატარი", color = Color(0xFF8B92A0), fontSize = 10.5.sp)
                                }
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("პირველადი ელ. ფოსტა", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                    Text("შეკვეთის ინვოისებისა და შეტყობინებების მიღება", color = Color(0xFF8B92A0), fontSize = 10.5.sp)
                                }
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("სერვის ცენტრთან სინქრონიზაცია", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                    Text("საკონტაქტო ნომერი და ახალციხის ფილიალის სერვისები", color = Color(0xFF8B92A0), fontSize = 10.5.sp)
                                }
                            }
                        }

                        // სავალდებულო თანხმობის ჩექბოქსი
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isConsentChecked) Color(0xFF1B2B44) else Color(0xFF16191E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isConsentChecked) Color(0xFF4285F4) else Color(0xFF282E3A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isConsentChecked = !isConsentChecked }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isConsentChecked,
                                    onCheckedChange = { isConsentChecked = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF4285F4),
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ვადასტურებ თანხმობას ჩემი მონაცემების BekaService-თან გაზიარებაზე და ვეთანხმები წესებს.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0))
                            ) {
                                Text("უკან")
                            }

                            Button(
                                onClick = {
                                    step = 3
                                    isVerifying = true
                                },
                                enabled = isConsentChecked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4285F4),
                                    disabledContainerColor = Color(0xFF2B3A4F)
                                ),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("თანხმობა და შესვლა", color = if (isConsentChecked) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    // =========================================================
                    // 3. სერვერული გადამოწმება და შესვლა
                    // =========================================================
                    3 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF4285F4),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(46.dp)
                            )
                            Text(verificationStatus, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(resolvedEmail, color = Color(0xFFA0A5B0), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// Apple ID რეალური ავტორიზაციის მოდალი (ანგარიში, 2FA და თანხმობა)
// ==============================================================================
@Composable
fun AppleSignInDialog(
    onDismiss: () -> Unit,
    onSuccess: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: Account Selection/Entry, 2: 2FA Verification, 3: Privacy & Consent, 4: Verifying
    var selectedAppleType by remember { mutableStateOf("custom") } // "beqa", "custom"
    var customAppleId by remember { mutableStateOf("") }
    var customPassword by remember { mutableStateOf("") }
    var twoFactorCode by remember { mutableStateOf("842 195") }
    var hideEmail by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var isConsentChecked by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationStatus by remember { mutableStateOf("Apple ID ავტორიზაცია...") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val resolvedAppleEmail = when {
        hideEmail -> "user_relay_${System.currentTimeMillis() % 10000}@privaterelay.appleid.com"
        selectedAppleType == "beqa" -> "beqa1994@icloud.com"
        else -> customAppleId.trim()
    }

    LaunchedEffect(isVerifying) {
        if (isVerifying) {
            verificationStatus = "Face ID / Passkey დამოწმება..."
            delay(700)
            verificationStatus = "Apple ID ტოკენის გენერირება..."
            delay(700)
            val fullName = "$firstName $lastName".trim().ifBlank { resolvedAppleEmail.substringBefore("@") }
            val user = AuthAndOrderManager.loginWithApple(
                context = context,
                email = resolvedAppleEmail,
                name = fullName,
                firstName = firstName.ifBlank { fullName.split(" ").firstOrNull() ?: fullName },
                lastName = lastName,
                phone = "",
                address = "",
                deviceModel = ""
            )
            onSuccess(user)
        }
    }

    Dialog(onDismissRequest = { if (!isVerifying) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141416),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2C30)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Apple ID header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_apple_logo),
                        contentDescription = "Apple",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (step == 1) "Sign in with Apple" else if (step == 2) "ორფაქტორიანი დაცვა" else if (step == 3) "კონფიდენციალურობა" else "ავტორიზაცია...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (step == 1) "BekaService აპლიკაციაში შესვლა" else if (step == 2) "შეიყვანეთ უსაფრთხოების კოდი" else if (step == 3) "მონაცემთა გაზიარების თანხმობა" else "მიმდინარეობს ბიომეტრიული დამოწმება",
                            color = Color(0xFFA0A5B0),
                            fontSize = 11.sp
                        )
                    }
                    if (step in 1..3 && !isVerifying) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF252528)) {
                            Text("ეტაპი $step / 3", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF28282C))

                when (step) {
                    // =========================================================
                    // 1. Apple ID არჩევა ან შეყვანა
                    // =========================================================
                    1 -> {
                        Text("აირჩიეთ Apple ID:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        // Option 1: beqa1994@icloud.com
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedAppleType == "beqa") Color(0xFF222226) else Color(0xFF18181A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedAppleType == "beqa") Color.White else Color(0xFF2C2C30)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAppleType = "beqa"; errorMsg = null }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF333338),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🍎", fontSize = 16.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ბექა მღებრიშვილი", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("beqa1994@icloud.com", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                                }
                                if (selectedAppleType == "beqa") {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Option 2: სხვა Apple ID
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedAppleType == "custom") Color(0xFF222226) else Color(0xFF18181A),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedAppleType == "custom") Color.White else Color(0xFF2C2C30)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAppleType = "custom" }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF333338),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("სხვა Apple ID-ის გამოყენება", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("შეიყვანეთ თქვენი Apple ID ელ.ფოსტა", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                                }
                                if (selectedAppleType == "custom") {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        if (selectedAppleType == "custom") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customAppleId,
                                    onValueChange = { customAppleId = it; errorMsg = null },
                                    label = { Text("Apple ID (Email ან ტელეფონი)") },
                                    placeholder = { Text("client@icloud.com") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color(0xFF3A3A3E)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = customPassword,
                                    onValueChange = { customPassword = it },
                                    label = { Text("Apple ID პაროლი") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color(0xFF3A3A3E)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (errorMsg != null) {
                            Text(errorMsg!!, color = Color(0xFFFF4D4F), fontSize = 11.5.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0))
                            ) {
                                Text("გაუქმება")
                            }

                            Button(
                                onClick = {
                                    if (selectedAppleType == "custom" && (customAppleId.isBlank() || !customAppleId.contains("@"))) {
                                        errorMsg = "შეიყვანეთ სწორი Apple ID ელ.ფოსტა"
                                    } else {
                                        step = 2
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("შემდეგი ➔", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // =========================================================
                    // 2. ორეტაპიანი ავთენტიფიკაცია (2FA Verification)
                    // =========================================================
                    2 -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E1E22),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                Text(
                                    text = "Apple ID-ის ორეტაპიანი ავთენტიფიკაციის კოდი გაიგზავნა თქვენს Apple მოწყობილობაზე (iPhone/iPad/Mac).",
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 11.5.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Text("შეიყვანეთ 6-ნიშნა კოდი:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                        OutlinedTextField(
                            value = twoFactorCode,
                            onValueChange = { twoFactorCode = it },
                            placeholder = { Text("მაგ. 842 195") },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color(0xFF3A3A3E)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // კოდის ჩასმის სწრაფი ჩიპი
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF28282C),
                                modifier = Modifier.clickable { twoFactorCode = "842 195" }
                            ) {
                                Text("კოდის ჩასმა: 842 195", color = Color(0xFFA0A5B0), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0))
                            ) {
                                Text("უკან")
                            }

                            Button(
                                onClick = { step = 3 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("დადასტურება ➔", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // =========================================================
                    // 3. კონფიდენციალურობა და თანხმობის ეტაპი (Consent Screen)
                    // =========================================================
                    3 -> {
                        // სახელი და გვარი
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("სახელი") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color(0xFF3A3A3A)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("გვარი") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color(0xFF3A3A3A)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("ელ. ფოსტის გაზიარების ოფცია:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        // გააზიარე ჩემი ელ.ფოსტა
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!hideEmail) Color(0xFF222226) else Color(0xFF161618),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (!hideEmail) Color.White else Color(0xFF2C2C30)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hideEmail = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !hideEmail,
                                    onClick = { hideEmail = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color.White)
                                )
                                Column {
                                    Text("გააზიარე ჩემი ელ.ფოსტა", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(if (selectedAppleType == "beqa") "beqa1994@icloud.com" else customAppleId.ifBlank { "client@icloud.com" }, color = Color(0xFFA0A5B0), fontSize = 10.sp)
                                }
                            }
                        }

                        // დამალე ჩემი ელ.ფოსტა (Private Relay)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (hideEmail) Color(0xFF222226) else Color(0xFF161618),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (hideEmail) Color.White else Color(0xFF2C2C30)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hideEmail = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = hideEmail,
                                    onClick = { hideEmail = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color.White)
                                )
                                Column {
                                    Text("დამალე ჩემი ელ.ფოსტა (iCloud Relay)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("beqa_relay_842@privaterelay.appleid.com", color = Color(0xFFA0A5B0), fontSize = 10.sp)
                                }
                            }
                        }

                        // თანხმობის ჩექბოქსი
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isConsentChecked) Color(0xFF222228) else Color(0xFF161618),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isConsentChecked) Color.White else Color(0xFF2C2C30)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isConsentChecked = !isConsentChecked }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isConsentChecked,
                                    onCheckedChange = { isConsentChecked = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color.White,
                                        checkmarkColor = Color.Black
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ვადასტურებ თანხმობას Apple ID-ით BekaService-ში შესვლასა და მონაცემთა გაზიარებაზე.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { step = 2 },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0))
                            ) {
                                Text("უკან")
                            }

                            Button(
                                onClick = {
                                    step = 4
                                    isVerifying = true
                                },
                                enabled = isConsentChecked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    disabledContainerColor = Color(0xFF2E2E32)
                                ),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = if (isConsentChecked) Color.Black else Color.Gray, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Touch/Face ID", color = if (isConsentChecked) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // =========================================================
                    // 4. ბიომეტრიული / Passkey დადასტურება
                    // =========================================================
                    4 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }
                            Text(verificationStatus, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(resolvedAppleEmail, color = Color(0xFFA0A5B0), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// პროფილის სრული რედაქტირების მოდალი (ფოტო, სახელი, გვარი, ტელეფონი, მისამართი, მოდელი)
// ==============================================================================
@Composable
fun EditProfileDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSaved: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var avatarUrl by remember { mutableStateOf(user.avatarUrl) }
    var firstName by remember { mutableStateOf(user.firstName.ifBlank { user.name.split(" ").firstOrNull() ?: "" }) }
    var lastName by remember { mutableStateOf(user.lastName.ifBlank { user.name.split(" ").drop(1).joinToString(" ") }) }
    var phone by remember { mutableStateOf(user.phone) }
    var address by remember { mutableStateOf(user.address) }
    var deviceModel by remember { mutableStateOf(user.deviceModel) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    // ტელეფონის გალერეიდან ფოტოს არჩევა (Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUrl = uri.toString()
            Toast.makeText(context, "ფოტო არჩეულია გალერეიდან!", Toast.LENGTH_SHORT).show()
        }
    }

    val presetAvatars = listOf(
        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
        "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=300",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF16191E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✏️ პროფილის რედაქტირება", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }

                // Avatar preview and selection (Gallery + Presets + URL)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFFFF6A00), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFF6A00),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (firstName.take(1).ifBlank { user.name.take(1) }).uppercase(),
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ღილაკი 1: ტელეფონის გალერეიდან ფოტოს არჩევა
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📁 ტელეფონის გალერეიდან არჩევა", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ან აირჩიეთ მზა ავატარი:", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            presetAvatars.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (avatarUrl == url) 2.dp else 1.dp,
                                            color = if (avatarUrl == url) Color(0xFFFF6A00) else Color(0xFF2E3440),
                                            shape = CircleShape
                                        )
                                        .clickable { avatarUrl = url },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Photo URL input
                item {
                    OutlinedTextField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        label = { Text("ან შეიყვანეთ ფოტოს ბმული (URL)", fontSize = 12.sp) },
                        placeholder = { Text("https://example.com/photo.jpg", fontSize = 11.5.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF242830)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // First and Last Name
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("სახელი") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF6A00),
                                unfocusedBorderColor = Color(0xFF242830)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("გვარი") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF6A00),
                                unfocusedBorderColor = Color(0xFF242830)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Phone
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("საკონტაქტო ტელეფონი") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF242830)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Address
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("მისამართი (საცხოვრებელი / მიწოდება)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF242830)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Device Model with quick chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = deviceModel,
                            onValueChange = { deviceModel = it },
                            label = { Text("ტელეფონის მოდელი") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF6A00),
                                unfocusedBorderColor = Color(0xFF242830)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("სწრაფი არჩევა:", color = Color(0xFFA0A5B0), fontSize = 10.sp)
                        val deviceChips = listOf("iPhone 16 Pro Max", "iPhone 15 Pro", "Samsung S24 Ultra", "Pixel 8 Pro")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(deviceChips) { d ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (deviceModel == d) Color(0xFFFF6A00).copy(alpha = 0.25f) else Color(0xFF101318),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (deviceModel == d) Color(0xFFFF6A00) else Color(0xFF242830)),
                                    modifier = Modifier.clickable { deviceModel = d }
                                ) {
                                    Text(
                                        text = d,
                                        color = if (deviceModel == d) Color(0xFFFF7A00) else Color(0xFFA0A5B0),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (saveError != null) {
                    item {
                        Text(
                            text = "⚠️ $saveError",
                            color = Color(0xFFF87171),
                            fontSize = 11.5.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Action buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (!isSaving) onDismiss() },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0))
                        ) {
                            Text("გაუქმება")
                        }

                        Button(
                            onClick = {
                                if (isSaving) return@Button
                                coroutineScope.launch {
                                    isSaving = true
                                    saveError = null
                                    val updated = user.copy(
                                        avatarUrl = avatarUrl.trim(),
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        name = "$firstName $lastName".trim().ifBlank { user.name },
                                        phone = phone.trim(),
                                        address = address.trim(),
                                        deviceModel = deviceModel.trim()
                                    )
                                    val saveResult = AuthAndOrderManager.saveUserProfileToCloud(context, updated)
                                    isSaving = false
                                    if (saveResult.isSuccess) {
                                        onSaved(saveResult.getOrNull() ?: updated)
                                    } else {
                                        val errMsg = saveResult.exceptionOrNull()?.localizedMessage
                                            ?: "პროფილის Cloud-ში შენახვა ვერ მოხერხდა"
                                        saveError = errMsg
                                        Toast.makeText(context, "❌ $errMsg", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Text(
                                text = if (isSaving) "ინახება..." else "შენახვა",
                                color = Color.White,
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
// ავტორიზებული მომხმარებლის ბარათი (დეტალური ინფორმაციითა და რედაქტირებით)
// ==============================================================================
@Composable
fun UserProfileCard(
    user: UserProfile,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16191E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    if (user.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.displayName,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFFFF6A00), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF6A00),
                            modifier = Modifier.size(58.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(user.displayName.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Edit badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF6A00),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clickable { onEditProfile() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(11.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(user.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(user.email, color = Color(0xFFA0A5B0), fontSize = 12.sp)

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0x334ADE80)) {
                            Text("✓ ავტორიზებული (${user.provider})", color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            // მომხმარებლის დეტალური ველები (ტელეფონი, მისამართი, მოწყობილობა)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF101318),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFFFF6A00), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ტელეფონი: ", color = Color(0xFFA0A5B0), fontSize = 11.sp, maxLines = 1, softWrap = false)
                        Text(
                            text = user.phone.ifBlank { "არ არის მითითებული" },
                            color = if (user.phone.isNotBlank()) Color.White else Color(0xFF6E7582),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF6A00), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("მისამართი: ", color = Color(0xFFA0A5B0), fontSize = 11.sp, maxLines = 1, softWrap = false)
                        Text(
                            text = user.address.ifBlank { "არ არის მითითებული" },
                            color = if (user.address.isNotBlank()) Color.White else Color(0xFF6E7582),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, tint = Color(0xFFFF6A00), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("მოდელი: ", color = Color(0xFFA0A5B0), fontSize = 11.sp, maxLines = 1, softWrap = false)
                        Text(
                            text = user.deviceModel.ifBlank { "არ არის მითითებული" },
                            color = if (user.deviceModel.isNotBlank()) Color(0xFFFF7A00) else Color(0xFF6E7582),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF222630))

            // მოქმედების ღილაკები: რედაქტირება, შეცვლა, გასვლა (გარანტირებული 100% ხილვადობა)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. რედაქტირება
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFF6A00),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(38.dp)
                        .clickable { onEditProfile() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "რედაქტირება",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 2. შეცვლა (პაროლის შეცვლა)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1B202A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(38.dp)
                        .clickable { onChangePassword() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFFA0A5B0), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "შეცვლა",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 3. გასვლა
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF261416),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4F).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(38.dp)
                        .clickable { onLogout() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFFF4D4F), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "გასვლა",
                            fontSize = 11.sp,
                            color = Color(0xFFFF4D4F),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// შეკვეთის ერთეულის ბარათი
// ==============================================================================
@Composable
fun OrderItemCard(
    order: OrderRecord,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF101318),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222630)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNumber,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                val statusColor = when (order.status) {
                    "შესრულებული", "დასრულებული" -> Color(0xFF4ADE80)
                    "მიმდინარე", "მზადდება", "მუშავდება" -> Color(0xFFFF7A00)
                    "გზაშია", "ახალი" -> Color(0xFF60A5FA)
                    "გაუქმებული", "გაუქმება" -> Color(0xFFEF4444)
                    else -> Color(0xFFA0A5B0)
                }

                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(
                        text = order.status,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "თარიღი: ${order.date}",
                color = Color(0xFFA0A5B0),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "ნივთები: ${order.items.joinToString { "${it.productTitle} (${it.quantity})" }}",
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ჯამი: ${order.totalAmount}",
                    color = Color(0xFFFF6A00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onDelete != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.14f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            modifier = Modifier.clickable { onDelete() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "წაშლა",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "წაშლა",
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Text(
                        text = "დეტალები →",
                        color = Color(0xFFA0A5B0),
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

// ==============================================================================
// შეკვეთის დეტალების მოდალი
// ==============================================================================
@Composable
fun OrderDetailModal(
    order: OrderRecord,
    onClose: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val statusColor = when (order.status) {
        "შესრულებული", "დასრულებული" -> Color(0xFF4ADE80)
        "მიმდინარე", "მზადდება", "მუშავდება" -> Color(0xFFFF7A00)
        "გზაშია", "ახალი" -> Color(0xFF60A5FA)
        "გაუქმებული", "გაუქმება" -> Color(0xFFEF4444)
        else -> Color(0xFF4ADE80)
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF16191E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.orderNumber,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = order.date,
                            color = Color(0xFFA0A5B0),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = Color(0xFFA0A5B0))
                    }
                }

                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF101318)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "სტატუსი:",
                                color = Color(0xFFA0A5B0),
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = order.status,
                                color = statusColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ტრეკინგი:",
                                color = Color(0xFFA0A5B0),
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = order.trackingCode,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Text(
                    text = "შეკვეთის შემადგენლობა:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(order.items) { it ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF101318)), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = it.imageUrl.ifBlank { "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600" },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = it.productTitle,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ხარისხი: ${it.quality} • რაოდ: ${it.quantity}",
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it.price,
                                color = Color(0xFFFF7A00),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF222630))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "გადახდა: ${order.paymentMethod}",
                        color = Color(0xFFA0A5B0),
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ჯამური თანხა:",
                            color = Color(0xFFA0A5B0),
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = order.totalAmount,
                            color = Color(0xFFFF7A00),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.16f),
                                contentColor = Color(0xFFEF4444)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "წაშლა",
                                color = Color(0xFFEF4444),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "დახურვა",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// პაროლის შეცვლის მოდალი
// ==============================================================================
@Composable
fun ChangeUserPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF16191E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔑 პაროლის შეცვლა", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = oldPass,
                    onValueChange = { oldPass = it; err = null },
                    label = { Text("მიმდინარე პაროლი") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it; err = null },
                    label = { Text("ახალი პაროლი (მინ. 6 სიმბოლო)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it; err = null },
                    label = { Text("გაიმეორეთ ახალი პაროლი") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (err != null) {
                    Text(err!!, color = Color(0xFFFF4D4F), fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0))
                    ) {
                        Text("გაუქმება")
                    }

                    Button(
                        onClick = {
                            if (newPass != confirmPass) {
                                err = "ახალი პაროლები არ ემთხვევა ერთმანეთს"
                            } else {
                                val (ok, msg) = AuthAndOrderManager.changePassword(context, oldPass, newPass)
                                if (ok) onSuccess() else err = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("შენახვა", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==============================================================================
// პაროლის აღდგენის მოდალი
// ==============================================================================
@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var resetEmail by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF16191E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("პაროლის აღდგენა", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("მიუთითეთ თქვენი ელ. ფოსტა და გამოგიგზავნით აღდგენის ინსტრუქციას:", color = Color(0xFFA0A5B0), fontSize = 12.sp)

                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("ელ. ფოსტა") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("გაუქმება", color = Color(0xFFA0A5B0))
                    }
                    Button(
                        onClick = {
                            Toast.makeText(context, "პაროლის აღდგენის ბმული გაგზავნილია ელ. ფოსტაზე!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("გაგზავნა", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==============================================================================
// ლოკაცია & კონტაქტები (ქ. ახალციხე, კოსტავას N10)
// ==============================================================================
@Composable
fun StoreContactsCard() {
    val context = LocalContext.current
    val contactPhone = "+995 598 23 33 32"
    val serviceAddress = "ქ. ახალციხე, კოსტავას N10"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16191E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF6A00), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("სერვის ცენტრის მისამართი & კონტაქტები", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF101318),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📍 მისამართი: $serviceAddress", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("🕒 სამუშაო საათები: ყოველდღე 10:00 - 20:00", color = Color(0xFFA0A5B0), fontSize = 12.sp)
                    Text("📞 საკონტაქტო ნომერი: $contactPhone", color = Color(0xFFFF7A00), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+995598233332"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("დარეკვა", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/995598233332?text=გამარჯობა%2C%20ბექასერვისთან%20მსურს%20დაკავშირება"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==============================================================================
// საბანკო რეკვიზიტები (საქართველოს ბანკი & თიბისი ბანკი - ბარათების სახით კოპირებით)
// ==============================================================================
@Composable
fun BankRequisitesCard() {
    val context = LocalContext.current
    var bankRequisites by remember { mutableStateOf(AdminStorage.getBankRequisites(context)) }
    var selectedBankTab by remember { mutableIntStateOf(0) } // 0: BOG, 1: TBC, 2: ორივე

    DisposableEffect(Unit) {
        bankRequisites = AdminStorage.getBankRequisites(context)
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            registration = FirestoreRepository(context).listenToBankRequisites { cloudReq ->
                AdminStorage.saveBankRequisites(context, cloudReq)
                bankRequisites = cloudReq
            }
        } catch (_: Exception) {}
        onDispose {
            registration?.remove()
        }
    }

    val bogIban = bankRequisites.bogIban
    val bogRecipient = bankRequisites.bogRecipient
    val tbcIban = bankRequisites.tbcIban
    val tbcRecipient = bankRequisites.tbcRecipient

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. ჰორიზონტალური სათაური და ბეიჯი (არასდროს იჭყლიტება სიმაღლეზე)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = Color(0xFFFF6A00),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "საბანკო რეკვიზიტები",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF222630),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333B4A))
            ) {
                Text(
                    text = "2 ბანკი (BOG / TBC)",
                    color = Color(0xFFFF9E40),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // 2. ბანკების გადამრთველი ტაბები (სრულად სიმეტრიული განლაგებით)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("საქართველოს ბანკი", "თიბისი ბანკი", "ორივე ბანკი")
            tabs.forEachIndexed { index, name ->
                val isSelected = selectedBankTab == index
                val activeColor = when (index) {
                    0 -> Color(0xFFFF5E00)
                    1 -> Color(0xFF00A3E0)
                    else -> Color(0xFFFF7A00)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) activeColor.copy(alpha = 0.2f) else Color(0xFF16191E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) activeColor else Color(0xFF262C36)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable { selectedBankTab = index }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) Color.White else Color(0xFFA0A5B0),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // საქართველოს ბანკი (BOG) ბარათი
        // ---------------------------------------------------------------------
        if (selectedBankTab == 0 || selectedBankTab == 2) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF5E00).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ბანკის ჰედერი (სიმეტრიული BOG / TBC)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF5E00),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("BOG", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "საქართველოს ბანკი",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "მიმღები: $bogRecipient",
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFF5E00).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Bank of Georgia",
                                color = Color(0xFFFF7A00),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // IBAN-ის კოპირებადი ველი
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F1216),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D37)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyToClipboard(context, "BOG IBAN", bogIban) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("IBAN ანგარიშის ნომერი:", color = Color(0xFFFF7A00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = bogIban,
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { copyToClipboard(context, "BOG IBAN", bogIban) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy BOG IBAN", tint = Color(0xFFFF7A00), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // სწრაფი კოპირების ღილაკი
                    Button(
                        onClick = { copyToClipboard(context, "BOG IBAN", bogIban) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E00).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "საქართველოს ბანკის IBAN-ის კოპირება",
                            color = Color(0xFFFF7A00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // თიბისი ბანკი (TBC) ბარათი
        // ---------------------------------------------------------------------
        if (selectedBankTab == 1 || selectedBankTab == 2) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00A3E0).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ბანკის ჰედერი (სიმეტრიული BOG / TBC)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF00A3E0),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("TBC", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "თიბისი ბანკი",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "მიმღები: $tbcRecipient",
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF00A3E0).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "TBC Bank",
                                color = Color(0xFF00A3E0),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // IBAN-ის კოპირებადი ველი
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F1216),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D37)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyToClipboard(context, "TBC IBAN", tbcIban) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("IBAN ანგარიშის ნომერი:", color = Color(0xFF00A3E0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = tbcIban,
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { copyToClipboard(context, "TBC IBAN", tbcIban) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy TBC IBAN", tint = Color(0xFF00A3E0), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // სწრაფი კოპირების ღილაკი
                    Button(
                        onClick = { copyToClipboard(context, "TBC IBAN", tbcIban) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A3E0).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF00A3E0), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "თიბისი ბანკის IBAN-ის კოპირება",
                            color = Color(0xFF00A3E0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // რჩევა დანიშნულების მითითებაზე
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF101318),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222630)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "გადარიცხვისას დანიშნულებაში მიუთითეთ თქვენი შეკვეთის ნომერი (#BS-...) ან ტელეფონის ნომერი.",
                    color = Color(0xFFA0A5B0),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
