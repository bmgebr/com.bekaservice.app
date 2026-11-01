package com.example

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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

// ==============================================================================
// 1. PIN კოდის შეყვანის დიალოგი
// ==============================================================================
@Composable
fun AdminPinDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val isAlreadyActive = remember { AdminStorage.isAdminSessionActive(context) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPin by remember { mutableStateOf(false) }

    LaunchedEffect(isAlreadyActive) {
        if (isAlreadyActive) {
            onSuccess()
        }
    }

    if (isAlreadyActive) {
        return
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF16191E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp)
                .testTag("admin_pin_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFF6A00).copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = Color(0xFFFF7A00),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = "ადმინ პანელში შესვლა",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "შეიყვანეთ ადმინისტრატორის PIN კოდი (PIN: bmgebr ან 1234)",
                    color = Color(0xFFA0A5B0),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        errorMessage = null
                    },
                    label = { Text("PIN კოდი") },
                    singleLine = true,
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle PIN",
                                tint = Color(0xFFA0A5B0)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440),
                        focusedLabelColor = Color(0xFFFF6A00),
                        unfocusedLabelColor = Color(0xFFA0A5B0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF4D4F),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("admin_pin_error_text")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_pin_cancel_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0))
                    ) {
                        Text("გაუქმება")
                    }

                    Button(
                        onClick = {
                            val trimmedPin = pinInput.trim()
                            if (AdminStorage.verifyPin(context, trimmedPin)) {
                                AdminStorage.setAdminSessionActive(context, true)
                                Toast.makeText(context, "ავტორიზაცია წარმატებულია!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                errorMessage = "არასწორი PIN კოდი! სცადეთ ხელახლა."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_pin_submit_button")
                    ) {
                        Text("შესვლა", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 2. PIN-ის შეცვლის დიალოგი
// ==============================================================================
@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF16191E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
            modifier = Modifier.fillMaxWidth(0.92f).padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔐 PIN კოდის შეცვლა",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = it; errorMsg = null },
                    label = { Text("მიმდინარე PIN") },
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
                    value = newPin,
                    onValueChange = { newPin = it; errorMsg = null },
                    label = { Text("ახალი PIN") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it; errorMsg = null },
                    label = { Text("გაიმეორეთ ახალი PIN") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = Color(0xFFFF4D4F), fontSize = 12.sp)
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
                            if (!AdminStorage.verifyPin(context, currentPin)) {
                                errorMsg = "მიმდინარე PIN არასწორია"
                            } else if (newPin.trim().length < 4) {
                                errorMsg = "ახალი PIN უნდა იყოს მინიმუმ 4 სიმბოლო"
                            } else if (newPin != confirmPin) {
                                errorMsg = "ახალი PIN-ები არ ემთხვევა ერთმანეთს"
                            } else {
                                AdminStorage.setAdminPin(context, newPin.trim())
                                onSuccess(newPin.trim())
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
// 3. ადმინ პანელის სრული ეკრანი
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    products: List<ProductItem>,
    onProductsUpdated: (List<ProductItem>) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var adminTab by remember { mutableIntStateOf(0) } // 0=პროდუქტები, 1=მასიური ფასები/მარჟა, 2=სტატისტიკა
    var showChangePinDialog by remember { mutableStateOf(false) }

    // რედაქტირება / დამატება
    var editingProduct by remember { mutableStateOf<ProductItem?>(null) }
    var isAddingNewProduct by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductItem?>(null) }

    // არსებული სუბ-მოდალების დახურვა Back ღილაკზე დაჭერისას, ან AdminPanelScreen-დან უსაფრთხო გამოსვლა
    BackHandler {
        when {
            editingProduct != null -> editingProduct = null
            isAddingNewProduct -> isAddingNewProduct = false
            productToDelete != null -> productToDelete = null
            showChangePinDialog -> showChangePinDialog = false
            else -> onClose()
        }
    }

    // Google Admin vs PIN-only სესიის სტატუსი (მხოლოდ beqa1994@gmail.com არის სრული უფლებების მქონე ადმინი)
    var firebaseUserEmail by remember {
        mutableStateOf(
            AuthAndOrderManager.resolveActiveUserEmail(context).ifBlank {
                if (AdminStorage.isAdminSessionActive(context)) AuthAndOrderManager.SOLE_ADMIN_EMAIL else ""
            }
        )
    }
    var isCloudAdminVerified by remember {
        mutableStateOf(AuthAndOrderManager.isSoleAdminEmail(firebaseUserEmail))
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                AuthAndOrderManager.ensureAdminFirebaseSession(context)
            }
            val resolvedEmail = AuthAndOrderManager.resolveActiveUserEmail(context).ifBlank {
                if (AdminStorage.isAdminSessionActive(context)) AuthAndOrderManager.SOLE_ADMIN_EMAIL else ""
            }
            val verifiedAdmin = if (resolvedEmail.isNotBlank() && !AuthAndOrderManager.isSoleAdminEmail(resolvedEmail)) {
                false
            } else {
                val repoCheck = kotlinx.coroutines.withTimeoutOrNull(3500L) {
                    runCatching {
                        FirestoreRepository(context).checkIsCurrentUserAdmin()
                    }.getOrDefault(false)
                } ?: false
                repoCheck || AuthAndOrderManager.isSoleAdminEmail(resolvedEmail)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                firebaseUserEmail = resolvedEmail
                isCloudAdminVerified = verifiedAdmin
            }
        }
    }

    // საძიებო & ფილტრი პროდუქტების ტაბში
    var productSearch by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("ყველა") }
    var selectedSubcategoryFilter by remember { mutableStateOf("ყველა") }
    var selectedBrandFilter by remember { mutableStateOf("ყველა") }

    val baseCategories = listOf(
        "ყველა",
        "პროგრამული სერვისები",
        "მობილური ტელეფონები",
        "როუტერები და ქსელი",
        "ეკრანები",
        "ელემენტები",
        "დამტენის პორტები",
        "დამტენები და კაბელები",
        "ყურსასმენები და აუდიო",
        "აქსესუარები",
        "კამერები",
        "კორპუსები",
        "დინამიკები"
    )
    val allCategories = remember(products) {
        val dynamicCats = products.map { it.category.trim() }.filter { it.isNotBlank() }
        (baseCategories + dynamicCats).distinct()
    }
    val softwareSubcategories = remember(products) {
        val dynamicSubcats = products
            .filter { it.category == "პროგრამული სერვისები" || it.type == "software" }
            .map { it.subcategory.trim() }
            .filter { it.isNotBlank() }
        (SoftwareCatalogData.softwareSubcategories + dynamicSubcats).distinct()
    }
    val baseBrands = listOf(
        "ყველა",
        "Apple",
        "Samsung",
        "Huawei",
        "Xiaomi",
        "Google Pixel",
        "OnePlus",
        "Honor",
        "OpenAI",
        "Google",
        "Anthropic",
        "xAI",
        "Perplexity",
        "Cursor",
        "Lovable",
        "Canva",
        "CapCut",
        "Adobe",
        "YouTube",
        "Spotify",
        "Microsoft"
    )
    val allBrands = remember(products) {
        val dynamicBrands = products.map { it.brand.trim() }.filter { it.isNotBlank() }
        (baseBrands + dynamicBrands).distinct()
    }

    val isDark = AppPreferencesManager.isDarkMode

    Scaffold(
        modifier = Modifier.testTag("admin_panel_screen"),
        containerColor = AppThemeColorAdapter.adaptPageBackground(Color(0xFF0D0F12)),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF6A00),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "ADMIN მართვის პანელი",
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("BEKASERVICE • სრული კონტროლი", color = Color(0xFFFF7A00), fontSize = 10.sp)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            AdminStorage.clearAdminSession(context)
                            Toast.makeText(context, "🔒 ადმინ სესია დაიხურა (დაბლოკილია)", Toast.LENGTH_SHORT).show()
                            onClose()
                        },
                        modifier = Modifier.testTag("admin_lock_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock Session", tint = Color(0xFFFF4D4F))
                    }
                    IconButton(
                        onClick = { showChangePinDialog = true },
                        modifier = Modifier.testTag("admin_change_pin_button")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = "Change PIN", tint = Color(0xFFFF6A00))
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("admin_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color.White else Color(0xFF1E293B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF16191E) else Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ტაბების ზოლი
            Surface(
                color = if (isDark) Color(0xFF12151B) else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF222630) else Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "პროდუქტები & სერვისები",
                        "კატეგორიები",
                        "შეკვეთები",
                        "მომხმარებლები",
                        "მარჟა & ფასები",
                        "Home Screen",
                        "სტატისტიკა",
                        "ბანკის რეკვიზიტები",
                        "Push შეტყობინება",
                        "აპლიკაციის განახლების მართვა"
                    )
                    tabs.forEachIndexed { index, title ->
                        val isSelected = adminTab == index
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFFF6A00) else Color.Transparent,
                            modifier = Modifier
                                .clickable { adminTab = index }
                                .padding(horizontal = 2.dp)
                        ) {
                            CompositionLocalProvider(LocalPreserveWhiteText provides isSelected) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ტაბის შიგთავსი Smooth Slide გადასვლით
            AnimatedContent(
                targetState = adminTab,
                transitionSpec = {
                    val direction = if (targetState >= initialState) 1 else -1
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> direction * fullWidth }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        initialAlpha = 0.88f
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> -direction * fullWidth }
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            targetAlpha = 0.88f
                        )
                    ).using(SizeTransform(clip = true))
                },
                label = "AdminTabSlideTransition"
            ) { currentAdminTab ->
            when (currentAdminTab) {
                0 -> {
                    // =========================================================================
                    // ტაბი 1: პროდუქტებისა და პროგრამული სერვისების მართვა (ფასი, აღწერა, მარაგი)
                    // სრული ეკრანის სქროლი: ძებნა, ფილტრები და სტატუსის ზოლი მოთავსებულია LazyColumn-ში,
                    // რათა ჩამოსქროლვისას პროდუქტების სიამ დაიკავოს მთელი სამუშაო სივრცე.
                    // =========================================================================
                    val filteredList = remember(
                        products,
                        selectedCategoryFilter,
                        selectedSubcategoryFilter,
                        selectedBrandFilter,
                        productSearch
                    ) {
                        products.filter { item ->
                            val matchCat = selectedCategoryFilter == "ყველა" || item.category.equals(selectedCategoryFilter, ignoreCase = true)
                            val matchSubcat = selectedSubcategoryFilter == "ყველა" || item.subcategory.equals(selectedSubcategoryFilter, ignoreCase = true)
                            val matchBrand = selectedBrandFilter == "ყველა" || item.brand.equals(selectedBrandFilter, ignoreCase = true)
                            val matchSearch = productSearch.isBlank() ||
                                    item.title.contains(productSearch, ignoreCase = true) ||
                                    item.shortDescription.contains(productSearch, ignoreCase = true) ||
                                    item.description.contains(productSearch, ignoreCase = true) ||
                                    item.category.contains(productSearch, ignoreCase = true) ||
                                    item.subcategory.contains(productSearch, ignoreCase = true) ||
                                    item.brand.contains(productSearch, ignoreCase = true) ||
                                    item.compatibleModels.contains(productSearch, ignoreCase = true) ||
                                    item.searchKeywords.contains(productSearch, ignoreCase = true)
                            matchCat && matchSubcat && matchBrand && matchSearch
                        }
                    }

                    var cloudSyncStatusMessage by remember { mutableStateOf<String?>(null) }
                    var isCloudOperationInProgress by remember { mutableStateOf(false) }
                    var showFiltersExpanded by remember { mutableStateOf(true) }

                    LaunchedEffect(firebaseUserEmail, isCloudAdminVerified) {
                        val hasFirebaseAuthUser = try {
                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
                        } catch (_: Exception) {
                            false
                        }
                        if (!hasFirebaseAuthUser || !isCloudAdminVerified) {
                            // PIN-only ან არა-ადმინის რეჟიმში არ ველოდებით Firestore მიგრაციას, რათა აპი არ გაიჭედოს
                            return@LaunchedEffect
                        }
                        val migrationRes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            kotlinx.coroutines.withTimeoutOrNull(6000L) {
                                val repo = FirestoreRepository(context)
                                repo.performSafeInitialMigrationIfNeeded(context)
                            }
                        }
                        migrationRes?.onSuccess { outcome ->
                            when (outcome) {
                                is FirestoreRepository.MigrationOutcome.Migrated -> {
                                    if (outcome.uploadedCount > 0) {
                                        cloudSyncStatusMessage = "☁️ საწყისი მიგრაცია შესრულდა: ${outcome.uploadedCount} პროდუქტი აიტვირთა Cloud-ში"
                                    }
                                }
                                is FirestoreRepository.MigrationOutcome.AlreadyPopulated -> {
                                    if (outcome.conflicts.isNotEmpty()) {
                                        cloudSyncStatusMessage = "⚠️ აღმოჩენილია ${outcome.conflicts.size} განსხვავება ლოკალურ და Cloud მონაცემებს შორის. დააჭირეთ „Cloud სინქრონიზაცია“-ს ასატვირთად."
                                    }
                                }
                                is FirestoreRepository.MigrationOutcome.SkippedNotAdmin -> Unit
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                    ) {
                        // 0. ავტორიზაციის რეჟიმის სტატუსის ბანერი (PIN-only vs Google Admin)
                        item(key = "admin_access_mode_banner_item") {
                            val bannerText = when {
                                isCloudAdminVerified -> "☁️ Google Admin ავტორიზებულია ($firebaseUserEmail) • Cloud სინქრონიზაცია აქტიურია"
                                firebaseUserEmail.isNotBlank() -> "⚠️ შესული ხართ არა-ადმინისტრატორის ანგარიშით ($firebaseUserEmail). Cloud-ში ჩაწერა შეზღუდულია."
                                else -> "🔑 PIN-კოდით წვდომის რეჟიმი (Google ავტორიზაციის გარეშე) • ცვლილებები ინახება ლოკალურად"
                            }
                            val bannerColor = when {
                                isCloudAdminVerified -> Color(0xFF4ADE80)
                                firebaseUserEmail.isNotBlank() -> Color(0xFFFF4D4F)
                                else -> Color(0xFFFF9E40)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = bannerColor.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, bannerColor.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_access_mode_banner")
                            ) {
                                Text(
                                    text = bannerText,
                                    color = bannerColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // 1. ძებნა + ფილტრების ჩაკეცვა/გაშლა + ახლის დამატება
                        item(key = "admin_products_header_controls") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDark) Color(0xFF16191E) else Color.White,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (productSearch.isNotEmpty()) Color(0xFFFF6A00) else if (isDark) Color(0xFF222630) else Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = if (productSearch.isNotEmpty()) Color(0xFFFF6A00) else if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            BasicTextField(
                                                value = productSearch,
                                                onValueChange = { productSearch = it },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    color = AppThemeColorAdapter.inputTextColor(),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                cursorBrush = SolidColor(Color(0xFFFF6A00)),
                                                modifier = Modifier.weight(1f),
                                                decorationBox = { innerTextField ->
                                                    if (productSearch.isEmpty()) {
                                                        Text(
                                                            text = "ძებნა: ChatGPT, Google AI, iPhone...",
                                                            fontSize = 11.5.sp,
                                                            color = if (isDark) Color(0xFF6E7582) else Color(0xFF94A3B8),
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                            if (productSearch.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { productSearch = "" },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Clear,
                                                        contentDescription = "Clear",
                                                        tint = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (showFiltersExpanded) Color(0xFFFF6A00).copy(alpha = if (isDark) 0.2f else 0.12f) else if (isDark) Color(0xFF16191E) else Color.White,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (showFiltersExpanded) Color(0xFFFF6A00) else if (isDark) Color(0xFF222630) else Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clickable { showFiltersExpanded = !showFiltersExpanded }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.FilterList,
                                                contentDescription = "ფილტრები",
                                                tint = if (showFiltersExpanded) Color(0xFFFF6A00) else if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = { isAddingNewProduct = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("admin_add_product_button"),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "დამატება",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                if (showFiltersExpanded) {
                                    // კატეგორიის ფილტრი (პროგრამული სერვისები + ნაწილები)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(allCategories, key = { it }) { cat ->
                                            val isSelected = selectedCategoryFilter == cat
                                            val isSoftwareCat = cat == "პროგრამული სერვისები"
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = when {
                                                    isSelected -> Color(0xFFFF6A00)
                                                    isSoftwareCat -> if (isDark) Color(0xFF1E222D) else Color(0xFFFFF7ED)
                                                    else -> if (isDark) Color(0xFF16191E) else Color.White
                                                },
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    when {
                                                        isSelected -> Color(0xFFFF7A00)
                                                        isSoftwareCat -> Color(0xFFFF7A00).copy(alpha = 0.55f)
                                                        else -> if (isDark) Color(0xFF242830) else Color(0xFFCBD5E1)
                                                    }
                                                ),
                                                modifier = Modifier.clickable {
                                                    selectedCategoryFilter = cat
                                                    if (cat != "პროგრამული სერვისები") {
                                                        selectedSubcategoryFilter = "ყველა"
                                                    }
                                                }
                                            ) {
                                                CompositionLocalProvider(LocalPreserveWhiteText provides isSelected) {
                                                    Text(
                                                        text = if (isSoftwareCat) "💻 $cat" else cat,
                                                        color = if (isSelected) Color.White else if (isSoftwareCat) Color(0xFFFF7A00) else if (isDark) Color(0xFFA0A5B0) else Color(0xFF475569),
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isSelected || isSoftwareCat) FontWeight.Bold else FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // ქვეკატეგორიის ფილტრი (როდესაც არჩეულია პროგრამული სერვისები)
                                    if (selectedCategoryFilter == "პროგრამული სერვისები") {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(softwareSubcategories, key = { it }) { subcat ->
                                                val isSelected = selectedSubcategoryFilter == subcat
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = if (isSelected) Color(0xFFFF7A00).copy(alpha = if (isDark) 0.22f else 0.15f) else if (isDark) Color(0xFF12151B) else Color.White,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isSelected) Color(0xFFFF7A00) else if (isDark) Color(0xFF262B36) else Color(0xFFE2E8F0)
                                                    ),
                                                    modifier = Modifier.clickable { selectedSubcategoryFilter = subcat }
                                                ) {
                                                    Text(
                                                        text = subcat,
                                                        color = if (isSelected) Color(0xFFFF7A00) else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                                        fontSize = 10.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // ბრენდის ფილტრი
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(allBrands, key = { it }) { brand ->
                                            val isSelected = selectedBrandFilter == brand
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) Color(0xFFFF6A00).copy(alpha = if (isDark) 0.2f else 0.12f) else if (isDark) Color(0xFF16191E) else Color.White,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFFF6A00) else if (isDark) Color(0xFF242830) else Color(0xFFCBD5E1)),
                                                modifier = Modifier.clickable { selectedBrandFilter = brand }
                                            ) {
                                                Text(
                                                    text = brand,
                                                    color = if (isSelected) Color(0xFFFF6A00) else if (isDark) Color(0xFFA0A5B0) else Color(0xFF475569),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (cloudSyncStatusMessage != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF60A5FA).copy(alpha = 0.45f) else Color(0xFF93C5FD)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = cloudSyncStatusMessage ?: "",
                                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                                fontSize = 10.5.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { cloudSyncStatusMessage = null },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "სულ: ${filteredList.size} / ${products.size} პოზიცია",
                                        color = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDark) Color(0xFF1F2A38) else Color(0xFFEFF6FF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF60A5FA).copy(alpha = 0.5f) else Color(0xFF93C5FD)),
                                        modifier = Modifier.clickable(enabled = !isCloudOperationInProgress) {
                                            isCloudOperationInProgress = true
                                            coroutineScope.launch {
                                                val res = FirestoreRepository(context).seedCatalogToFirestore(products)
                                                isCloudOperationInProgress = false
                                                val count = res.getOrDefault(0)
                                                if (res.isSuccess && count > 0) {
                                                    AdminStorage.saveProductsFromCloud(context, products)
                                                    AdminStorage.markLocalEditsSynced(context)
                                                    AdminStorage.setInitialCloudMigrationCompleted(context, true)
                                                    cloudSyncStatusMessage = "☁️ სინქრონიზებულია Cloud-თან ($count პოზიცია)"
                                                    Toast.makeText(context, "☁️ კატალოგი წარმატებით აიტვირთა Cloud-ში ($count პოზიცია)!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val errMsg = FirestoreRepository.formatUserFriendlyFirestoreError(res.exceptionOrNull())
                                                    Toast.makeText(context, "❌ Cloud სინქრონიზაცია ვერ მოხერხდა: $errMsg", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isCloudOperationInProgress) "იტვირთება..." else "Cloud სინქრონიზაცია",
                                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(filteredList, key = { it.id }) { item ->
                            AdminProductRowItem(
                                item = item,
                                onEdit = { editingProduct = item },
                                onDelete = { productToDelete = item },
                                onToggleStock = {
                                    val toggledItem = item.copy(
                                        inStock = !item.inStock,
                                        firestoreDocId = item.firestoreDocId.ifBlank { "prod_${item.id}" }
                                    )
                                    coroutineScope.launch {
                                        val activeEmail = AuthAndOrderManager.resolveActiveUserEmail(context)
                                        if (activeEmail.isNotBlank() && !AuthAndOrderManager.isSoleAdminEmail(activeEmail)) {
                                            Toast.makeText(
                                                context,
                                                "❌ წვდომა უარყოფილია: მხოლოდ beqa1994@gmail.com არის ადმინისტრატორი!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }
                                        val updated = products.map { if (it.id == item.id) toggledItem else it }
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            AdminStorage.saveProducts(context, updated, markUnsyncedLocalEdit = true)
                                        }
                                        onProductsUpdated(updated)

                                        val repo = FirestoreRepository(context)
                                        val writeRes = repo.saveOrUpdateProductResult(toggledItem)
                                        if (writeRes.isSuccess) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                AdminStorage.saveProductsFromCloud(context, updated)
                                                AdminStorage.markLocalEditsSynced(context)
                                            }
                                            Toast.makeText(
                                                context,
                                                if (toggledItem.inStock) "✅ სტატუსი განახლდა Cloud-ში: მარაგშია" else "✅ სტატუსი განახლდა Cloud-ში: არ არის მარაგში",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                if (toggledItem.inStock) "✅ სტატუსი განახლდა: მარაგშია" else "✅ სტატუსი განახლდა: არ არის მარაგში",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                1 -> {
                    // =========================================================================
                    // ტაბი 2: კატეგორიების მართვა (Dynamic Categories Management)
                    // =========================================================================
                    AdminCategoriesManagementView(
                        products = products,
                        onProductsUpdated = onProductsUpdated
                    )
                }

                2 -> {
                    // =========================================================================
                    // ტაბი 3: შეკვეთების მართვა (Orders Management - Cloud Firestore)
                    // =========================================================================
                    AdminOrdersManagementView()
                }

                3 -> {
                    // =========================================================================
                    // ტაბი 4: მომხმარებლების მართვა (Users Management - Cloud Firestore)
                    // =========================================================================
                    AdminUsersManagementView()
                }

                4 -> {
                    // =========================================================================
                    // ტაბი 5: მასიური ფასებისა და მარჟის ცვლილება (Bulk Price / Margin)
                    // =========================================================================
                    BulkPriceAndMarginScreen(
                        products = products,
                        allBrands = allBrands,
                        allCategories = allCategories,
                        onApply = { targetBrand, targetCategory, mode, value ->
                            coroutineScope.launch {
                                val activeEmail = AuthAndOrderManager.resolveActiveUserEmail(context)
                                if (activeEmail.isNotBlank() && !AuthAndOrderManager.isSoleAdminEmail(activeEmail)) {
                                    Toast.makeText(
                                        context,
                                        "❌ წვდომა უარყოფილია: მხოლოდ beqa1994@gmail.com არის ადმინისტრატორი!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@launch
                                }
                                val candidateList = AdminStorage.applyBulkPriceChange(
                                    context = context,
                                    currentList = products,
                                    targetBrand = targetBrand,
                                    targetCategory = targetCategory,
                                    mode = mode,
                                    value = value
                                )
                                onProductsUpdated(candidateList)
                                val res = runCatching {
                                    FirestoreRepository(context).seedCatalogToFirestore(candidateList)
                                }.getOrElse { Result.failure(it) }
                                if (res.isSuccess) {
                                    AdminStorage.saveProductsFromCloud(context, candidateList)
                                    AdminStorage.markLocalEditsSynced(context)
                                    Toast.makeText(context, "✅ ფასები წარმატებით განახლდა და დასინქრონდა ბაზასთან!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "✅ ფასები წარმატებით განახლდა!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onResetDefaults = {
                            Toast.makeText(context, "⚠️ კატალოგის სრული განულება დაბლოკილია მონაცემთა დაცვის მიზნით. გამოიყენეთ ინდივიდუალური რედაქტირება.", Toast.LENGTH_LONG).show()
                        }
                    )
                }

                5 -> {
                    // =========================================================================
                    // ტაბი 6: Home Screen items მართვა & რედაქტირება (ბანერები, შეთავაზებები)
                    // =========================================================================
                    AdminHomeScreenView(products = products)
                }

                6 -> {
                    // =========================================================================
                    // ტაბი 7: სტატისტიკა & მიმოხილვა
                    // =========================================================================
                    AdminStatisticsView(products = products)
                }

                7 -> {
                    // =========================================================================
                    // ტაბი 8: საბანკო რეკვიზიტების მართვა & რედაქტირება
                    // =========================================================================
                    AdminBankRequisitesView()
                }

                8 -> {
                    // =========================================================================
                    // ტაბი 9: Push შეტყობინებების გაგზავნა და ტესტირება (მხოლოდ beqa1994@gmail.com)
                    // =========================================================================
                    AdminPushNotificationsView(products = products)
                }

                9 -> {
                    // =========================================================================
                    // ტაბი 10: აპლიკაციის განახლების მართვა (app_config/updates)
                    // =========================================================================
                    AdminAppUpdateManagementView()
                }
            }
            }
        }
    }

    // პროდუქტის / პროგრამული სერვისის რედაქტირების დიალოგი
    if (editingProduct != null) {
        ProductEditDialog(
            product = editingProduct!!,
            isNew = false,
            existingCategories = allCategories.filter { it != "ყველა" },
            existingBrands = allBrands.filter { it != "ყველა" },
            onDismiss = { editingProduct = null },
            onSave = { updatedItem ->
                coroutineScope.launch {
                    val activeEmail = AuthAndOrderManager.resolveActiveUserEmail(context)
                    if (activeEmail.isNotBlank() && !AuthAndOrderManager.isSoleAdminEmail(activeEmail)) {
                        Toast.makeText(
                            context,
                            "❌ წვდომა უარყოფილია: მხოლოდ beqa1994@gmail.com არის ადმინისტრატორი!",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    val savedItem = updatedItem.copy(
                        firestoreDocId = updatedItem.firestoreDocId.ifBlank { "prod_${updatedItem.id}" }
                    )
                    val updated = products.map { if (it.id == savedItem.id) savedItem else it }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        AdminStorage.saveProducts(context, updated, markUnsyncedLocalEdit = true)
                    }
                    onProductsUpdated(updated)
                    editingProduct = null

                    val writeRes = runCatching {
                        FirestoreRepository(context).saveOrUpdateProductResult(savedItem)
                    }.getOrElse { Result.failure(it) }
                    if (writeRes.isSuccess) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            AdminStorage.saveProductsFromCloud(context, updated)
                            AdminStorage.markLocalEditsSynced(context)
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "✅ ცვლილებები წარმატებით შეინახა და დასინქრონდა ბაზასთან!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "✅ ცვლილებები წარმატებით შეინახა!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    // ახალი პროდუქტის ან პროგრამული სერვისის დამატების დიალოგი
    if (isAddingNewProduct) {
        val nextId = (products.maxOfOrNull { it.id } ?: 200) + 1
        val defaultCat = if (selectedCategoryFilter != "ყველა") selectedCategoryFilter else "მობილური ტელეფონები"
        val isSw = defaultCat == "პროგრამული სერვისები"
        val defaultType = ProductFormSchema.resolveProductCategoryType(
            productTypeRaw = if (isSw) "software" else "mobile_phone",
            categoryRaw = defaultCat
        )
        val newItem = ProductItem(
            id = nextId,
            firestoreDocId = "prod_$nextId",
            title = "",
            price = "",
            category = defaultCat,
            type = if (defaultType.isDigitalService) "software" else "product",
            productType = defaultType.id,
            subcategory = if (defaultType.isDigitalService && selectedSubcategoryFilter != "ყველა") selectedSubcategoryFilter else "",
            imageUrl = ProductImageManager.AUTO_GENERATED_IMAGE_MARKER,
            shortDescription = "",
            description = "",
            brand = if (selectedBrandFilter != "ყველა") selectedBrandFilter else "",
            compatibleModels = "",
            warranty = "",
            condition = "",
            inStock = true
        )
        ProductEditDialog(
            product = newItem,
            isNew = true,
            existingCategories = allCategories.filter { it != "ყველა" },
            existingBrands = allBrands.filter { it != "ყველა" },
            onDismiss = { isAddingNewProduct = false },
            onSave = { addedItem ->
                coroutineScope.launch {
                    val activeEmail = AuthAndOrderManager.resolveActiveUserEmail(context)
                    if (activeEmail.isNotBlank() && !AuthAndOrderManager.isSoleAdminEmail(activeEmail)) {
                        Toast.makeText(
                            context,
                            "❌ წვდომა უარყოფილია: მხოლოდ beqa1994@gmail.com არის ადმინისტრატორი!",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    val itemWithDocId = addedItem.copy(
                        firestoreDocId = addedItem.firestoreDocId.ifBlank { "prod_${addedItem.id}" }
                    )
                    val updated = (listOf(itemWithDocId) + products).distinctBy { it.id }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        AdminStorage.saveProducts(context, updated, markUnsyncedLocalEdit = true)
                    }
                    onProductsUpdated(updated)
                    isAddingNewProduct = false

                    val writeRes = runCatching {
                        FirestoreRepository(context).saveOrUpdateProductResult(itemWithDocId)
                    }.getOrElse { Result.failure(it) }
                    if (writeRes.isSuccess) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            AdminStorage.saveProductsFromCloud(context, updated)
                            AdminStorage.markLocalEditsSynced(context)
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "✅ ახალი პოზიცია წარმატებით დაემატა და დასინქრონდა ბაზასთან!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "✅ ახალი პოზიცია წარმატებით დაემატა!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    // წაშლის დადასტურების დიალოგი
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("პროდუქტის წაშლა", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("ნამდვილად გსურთ წაშალოთ „${productToDelete!!.title}“?", color = Color(0xFFA0A5B0)) },
            containerColor = Color(0xFF16191E),
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = productToDelete ?: return@Button
                        val docIdToDelete = toDel.firestoreDocId.ifBlank { "prod_${toDel.id}" }
                        coroutineScope.launch {
                            val activeEmail = AuthAndOrderManager.resolveActiveUserEmail(context)
                            if (activeEmail.isNotBlank() && !AuthAndOrderManager.isSoleAdminEmail(activeEmail)) {
                                Toast.makeText(
                                    context,
                                    "❌ წვდომა უარყოფილია: მხოლოდ beqa1994@gmail.com არის ადმინისტრატორი!",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }
                            val updated = products.filter { it.id != toDel.id }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                AdminStorage.markProductDeleted(context, toDel.id)
                                AdminStorage.saveProducts(context, updated, markUnsyncedLocalEdit = true)
                            }
                            onProductsUpdated(updated)
                            productToDelete = null

                            val delRes = runCatching {
                                FirestoreRepository(context).deleteProductResult(docIdToDelete, toDel.id)
                            }.getOrElse { Result.failure(it) }
                            if (delRes.isSuccess) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    AdminStorage.saveProductsFromCloud(context, updated)
                                    AdminStorage.markLocalEditsSynced(context)
                                }
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(context, "✅ პროდუქტი წაიშალა ბაზიდან!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(context, "✅ პროდუქტი წაიშალა!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F))
                ) {
                    Text("წაშლა", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("გაუქმება", color = Color(0xFFA0A5B0))
                }
            }
        )
    }

    // PIN-ის შეცვლის დიალოგი
    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onSuccess = {
                showChangePinDialog = false
                Toast.makeText(context, "ახალი PIN წარმატებით დაყენდა!", Toast.LENGTH_LONG).show()
            }
        )
    }
}

// ==============================================================================
// 4. პროდუქტისა და პროგრამული სერვისის მწკრივი ადმინ სიაში
// ==============================================================================
@Composable
fun AdminProductRowItem(
    item: ProductItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStock: () -> Unit
) {
    val isSoftware = SoftwareCatalogData.isDigitalSoftwareService(item) || item.category == "პროგრამული სერვისები"
    val previewDesc = item.shortDescription.ifBlank { item.description }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF16191E),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSoftware) Color(0xFFFF7A00).copy(alpha = 0.35f) else Color(0xFF222630)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ვიზუალი / სურათი (მხარს უჭერს როგორც მიბმულ ფოტოს ლინკით/მეხსიერებიდან, ისე ავტო-გენერირებულ ბრენდ-ვიზუალს)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF222630)))
                ) {
                    ProductSmartVisual(
                        product = item,
                        modifier = Modifier.fillMaxSize(),
                        compact = true
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.brand,
                            color = Color(0xFFFF7A00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = " • ",
                            color = Color(0xFF6E7582),
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = item.subcategory.ifBlank { item.category },
                            color = Color(0xFFA0A5B0),
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    if (previewDesc.isNotBlank()) {
                        Text(
                            text = previewDesc,
                            color = Color(0xFF8C94A4),
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val isLongPrice = item.price.length > 10
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.price,
                            color = Color(0xFF4ADE80),
                            fontSize = if (isLongPrice) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (item.inStock) Color(0x334ADE80) else Color(0x33FF4D4F),
                            modifier = Modifier.clickable { onToggleStock() }
                        ) {
                            Text(
                                text = if (item.inStock) {
                                    if (isLongPrice) "მარაგშია" else "ხელმისაწვდომია"
                                } else {
                                    "მიუწვდომელია"
                                },
                                color = if (item.inStock) Color(0xFF4ADE80) else Color(0xFFFF4D4F),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // მოქმედების ღილაკები: რედაქტირება (ფასი/აღწერა) და წაშლა
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF6A00).copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6A00)),
                        modifier = Modifier.clickable { onEdit() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFFF7A00), modifier = Modifier.size(13.dp))
                            Text(
                                "ფასი / აღწერა",
                                color = Color(0xFFFF9E40),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D4F), modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 5. პროდუქტისა და პროგრამული სერვისის რედაქტირების მოდალი (ფასი, აღწერა, პირობები)
// ==============================================================================
@Composable
fun ProductEditDialog(
    product: ProductItem,
    isNew: Boolean,
    existingCategories: List<String> = emptyList(),
    existingBrands: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (ProductItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(product.title) }
    var price by remember { mutableStateOf(product.price) }
    var brand by remember { mutableStateOf(product.brand) }
    var category by remember { mutableStateOf(product.category) }
    var subcategory by remember { mutableStateOf(product.subcategory) }
    var shortDescription by remember { mutableStateOf(product.shortDescription) }
    var description by remember { mutableStateOf(product.description) }
    var featuresText by remember { mutableStateOf(product.features.joinToString("\n")) }
    var targetAudience by remember { mutableStateOf(product.targetAudience) }
    var offerType by remember { mutableStateOf(product.offerType) }
    var duration by remember { mutableStateOf(product.duration) }
    var accountType by remember { mutableStateOf(product.accountType) }
    var activationTerms by remember { mutableStateOf(product.activationTerms) }
    var compatibleModels by remember { mutableStateOf(product.compatibleModels) }
    var warranty by remember { mutableStateOf(product.warranty) }
    var searchKeywords by remember { mutableStateOf(product.searchKeywords) }

    val initialHasCustomPhoto = remember(product) { ProductImageManager.hasCustomAttachedPhoto(product) }
    var imageUrl by remember {
        mutableStateOf(
            if (initialHasCustomPhoto) product.imageUrl else ProductImageManager.AUTO_GENERATED_IMAGE_MARKER
        )
    }
    var urlInputText by remember {
        mutableStateOf(
            if (initialHasCustomPhoto && !ProductImageManager.isLocalOrEmbeddedImage(product.imageUrl)) {
                product.imageUrl
            } else {
                ""
            }
        )
    }
    var showUrlInputBox by remember {
        mutableStateOf(initialHasCustomPhoto && !ProductImageManager.isLocalOrEmbeddedImage(product.imageUrl))
    }

    var inStock by remember { mutableStateOf(product.inStock) }
    var isFeatured by remember { mutableStateOf(product.isFeatured) }
    var displayOrderInput by remember { mutableStateOf(product.displayOrder.toString()) }

    var exactModel by remember { mutableStateOf(product.exactModel) }
    var selectedCategoryType by remember(product) {
        mutableStateOf(
            ProductFormSchema.resolveProductCategoryType(
                productTypeRaw = product.productType,
                categoryRaw = product.category,
                legacyTypeRaw = product.type,
                titleRaw = product.title
            )
        )
    }
    val currentFormConfig = remember(selectedCategoryType) {
        ProductFormSchema.getConfig(selectedCategoryType)
    }
    var productType by remember(selectedCategoryType) { mutableStateOf(selectedCategoryType.id) }
    var condition by remember { mutableStateOf(product.condition) }
    var productImages by remember {
        mutableStateOf(
            product.allImages.filter { it.isNotBlank() && it != ProductImageManager.AUTO_GENERATED_IMAGE_MARKER }.toMutableList()
        )
    }
    var primaryImageUrl by remember { mutableStateOf(product.displayPrimaryImage) }
    var specificationsMap by remember { mutableStateOf(product.specifications.toMutableMap()) }
    var newSpecKey by remember { mutableStateOf("") }
    var newSpecVal by remember { mutableStateOf("") }
    var imageToCrop by remember { mutableStateOf<String?>(null) }
    var aiReviewFields by remember { mutableStateOf<AdminProductAiGenerator.GeneratedProductFields?>(null) }

    var isAiGenerating by remember { mutableStateOf(false) }
    var aiStatusMessage by remember { mutableStateOf<String?>(null) }
    var isAiError by remember { mutableStateOf(false) }

    // State for warning administrator before clearing incompatible type-specific fields on type switch
    var pendingCategoryTypeSwitch by remember { mutableStateOf<ProductCategoryType?>(null) }
    var pendingIncompatibleFieldNames by remember { mutableStateOf<List<String>>(emptyList()) }

    fun applyCategoryTypeSwitch(targetType: ProductCategoryType, clearIncompatible: Boolean) {
        val prevConfig = ProductFormSchema.getConfig(selectedCategoryType)
        val targetConfig = ProductFormSchema.getConfig(targetType)
        if (clearIncompatible) {
            if (!targetConfig.showSoftwareFields) {
                offerType = ""
                duration = ""
                accountType = ""
                activationTerms = ""
                subcategory = ""
            }
            if (!targetConfig.showCompatibleModels) {
                compatibleModels = ""
            }
            val targetSpecKeys = targetConfig.specFields.map { it.key }.toSet()
            val prevExclusiveSpecKeys = prevConfig.specFields.map { it.key }.toSet() - targetSpecKeys
            if (prevExclusiveSpecKeys.isNotEmpty()) {
                val updatedSpecs = specificationsMap.toMutableMap()
                prevExclusiveSpecKeys.forEach { updatedSpecs.remove(it) }
                specificationsMap = updatedSpecs
            }
        }
        if (category.isBlank() || category == prevConfig.categoryNameGeo) {
            category = targetConfig.categoryNameGeo
        }
        if (targetType == ProductCategoryType.DIGITAL_SERVICE) {
            condition = "ციფრული"
        } else if (condition == "ციფრული") {
            condition = ""
        }
        selectedCategoryType = targetType
        productType = targetType.id
    }

    fun requestCategoryTypeSwitch(targetType: ProductCategoryType) {
        if (targetType == selectedCategoryType) return
        val incompatible = ProductFormSchema.detectIncompatiblePopulatedFields(
            fromType = selectedCategoryType,
            toType = targetType,
            offerType = offerType,
            duration = duration,
            accountType = accountType,
            activationTerms = activationTerms,
            subcategory = subcategory,
            specifications = specificationsMap
        )
        if (incompatible.isNotEmpty()) {
            pendingCategoryTypeSwitch = targetType
            pendingIncompatibleFieldNames = incompatible
        } else {
            applyCategoryTypeSwitch(targetType, clearIncompatible = false)
        }
    }

    // ტელეფონის მეხსიერებიდან ფოტოს არჩევის ლანჩერი (Zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = ProductImageManager.copyPickerUriToInternalStorage(context, uri)
                if (!savedPath.isNullOrBlank()) {
                    imageUrl = savedPath
                    if (!productImages.contains(savedPath)) {
                        productImages = (productImages + savedPath).toMutableList()
                    }
                    if (primaryImageUrl.isBlank() || primaryImageUrl == ProductImageManager.AUTO_GENERATED_IMAGE_MARKER) {
                        primaryImageUrl = savedPath
                    }
                    showUrlInputBox = false
                    Toast.makeText(context, "✅ ფოტო ტელეფონის მეხსიერებიდან წარმატებით მიება!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "⚠️ ფოტოს წაკითხვა ვერ მოხერხდა, სცადეთ სხვა სურათი", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // მრავალფოტოიანი არჩევის ლანჩერი (Multi-Image Picker)
    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val added = mutableListOf<String>()
                for (uri in uris) {
                    val saved = ProductImageManager.copyPickerUriToInternalStorage(context, uri)
                    if (!saved.isNullOrBlank() && !productImages.contains(saved)) {
                        added.add(saved)
                    }
                }
                if (added.isNotEmpty()) {
                    productImages = (productImages + added).toMutableList()
                    if (primaryImageUrl.isBlank() || primaryImageUrl == ProductImageManager.AUTO_GENERATED_IMAGE_MARKER) {
                        primaryImageUrl = productImages.first()
                        imageUrl = productImages.first()
                    }
                    Toast.makeText(context, "✅ ${added.size} ფოტო წარმატებით დაემატა!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Schema-driven determination: ONLY true when DIGITAL_SERVICE is selected
    val isSoftwareService = currentFormConfig.showSoftwareFields

    val baseBrands = listOf(
        "Apple", "Samsung", "Xiaomi", "Google Pixel", "OnePlus", "Honor", "Huawei",
        "TP-Link", "ZTE", "Anker", "Baseus", "Ugreen", "Sony", "JBL",
        "OpenAI", "Google", "Anthropic", "xAI", "Perplexity", "Cursor", "Lovable",
        "Canva", "CapCut", "Adobe", "YouTube", "Spotify", "Microsoft"
    )
    val brands = remember(existingBrands, brand) {
        (baseBrands + existingBrands + listOfNotNull(brand.trim().takeIf { it.isNotBlank() })).distinct()
    }

    val baseCategories = listOf(
        "მობილური ტელეფონები", "ეკრანები", "აქსესუარები", "ყურსასმენები და აუდიო",
        "დამტენები და კაბელები", "ქსელური მოწყობილობები", "ელექტრონიკა",
        "პროგრამული სერვისები", "ელემენტები", "დამტენის პორტები", "კამერები", "კორპუსები", "დინამიკები"
    )
    val categories = remember(existingCategories, category) {
        (baseCategories + existingCategories + listOfNotNull(category.trim().takeIf { it.isNotBlank() })).distinct()
    }

    val softwareSubcategories = remember(subcategory) {
        (SoftwareCatalogData.softwareSubcategories.filter { it != "ყველა" } +
            listOfNotNull(subcategory.trim().takeIf { it.isNotBlank() })).distinct()
    }

    val quickPrices = listOf(
        "ფასი შეთანხმებით", "15 ₾", "20 ₾", "25 ₾", "30 ₾", "35 ₾",
        "45 ₾", "50 ₾", "65 ₾", "80 ₾", "95 ₾", "120 ₾", "150 ₾"
    )
    val quickDurations = listOf("1 თვე", "3 თვე", "6 თვე", "12 თვე", "18 თვე", "უვადო (Lifetime)")
    val quickAccountTypes = listOf(
        "პირადი ანგარიში (Private)",
        "საზიარო ანგარიში (Shared)",
        "პირადი Google ანგარიში",
        "Retail ლიცენზიის გასაღები"
    )

    // ცოცხალი გადახედვის ობიექტი ქარდის ვიზუალისთვის
    val previewProduct = remember(
        title, price, brand, category, subcategory, shortDescription, description, duration, imageUrl, selectedCategoryType
    ) {
        product.copy(
            title = title.ifBlank { "ახალი პროდუქტი" },
            price = price,
            brand = brand,
            category = category.ifBlank { currentFormConfig.categoryNameGeo },
            type = if (isSoftwareService) "software" else "product",
            productType = selectedCategoryType.id,
            subcategory = subcategory,
            shortDescription = shortDescription,
            description = description,
            duration = duration,
            imageUrl = imageUrl
        )
    }

    val isDark = AppPreferencesManager.isDarkMode

    if (pendingCategoryTypeSwitch != null) {
        val targetType = pendingCategoryTypeSwitch!!
        AlertDialog(
            onDismissRequest = {
                pendingCategoryTypeSwitch = null
                pendingIncompatibleFieldNames = emptyList()
            },
            containerColor = if (isDark) Color(0xFF1A1F29) else Color.White,
            titleContentColor = if (isDark) Color.White else Color(0xFF0F172A),
            textContentColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
            title = {
                Text(
                    "⚠️ პროდუქტის ტიპის შეცვლა",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "არჩეულ ტიპზე (${targetType.labelGeo}) გადასვლისას გასუფთავდება შეუთავსებელი ველები:",
                        fontSize = 12.sp
                    )
                    Text(
                        pendingIncompatibleFieldNames.joinToString(", "),
                        color = Color(0xFFFF9E40),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "საერთო ველები (დასახელება, ბრენდი, ფასი, აღწერა, ფოტოები, გარანტია, მარაგი) სრულად შენარჩუნდება.",
                        color = Color(0xFF4ADE80),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        applyCategoryTypeSwitch(targetType, clearIncompatible = true)
                        pendingCategoryTypeSwitch = null
                        pendingIncompatibleFieldNames = emptyList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                    modifier = Modifier.testTag("confirm_type_switch_button")
                ) {
                    Text("გადართვა და გასუფთავება", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        pendingCategoryTypeSwitch = null
                        pendingIncompatibleFieldNames = emptyList()
                    },
                    modifier = Modifier.testTag("cancel_type_switch_button")
                ) {
                    Text("გაუქმება", fontSize = 11.5.sp)
                }
            },
            modifier = Modifier.testTag("type_switch_warning_dialog")
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF16191E) else Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = if (isDark) 0.55f else 0.45f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isNew -> "➕ ახალი პოზიციის დამატება"
                                isSoftwareService -> "✏️ ციფრული სერვისის რედაქტირება"
                                else -> "✏️ ${selectedCategoryType.labelGeo} — რედაქტირება"
                            },
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ფორმის ველები ავტომატურად ერგება არჩეული პროდუქტის ტიპს",
                            color = Color(0xFFFF9E40),
                            fontSize = 10.5.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_edit_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
                ) {
                    // 1. დასახელება + AI დეტალური ანალიზის ღილაკი (ერთი ხატულით)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("დასახელება (Title)") },
                                placeholder = { Text("მაგ: Samsung Galaxy S24 Ultra 12/256GB, Huawei B535-232, ChatGPT Plus...", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                    unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFFFF6A00),
                                    unfocusedBorderColor = if (isDark) Color(0xFF2E3440) else Color(0xFFCBD5E1),
                                    focusedContainerColor = if (isDark) Color.Transparent else Color(0xFFF8FAFC),
                                    unfocusedContainerColor = if (isDark) Color.Transparent else Color(0xFFF8FAFC)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("product_title_input")
                            )

                            // AI-ით დეტალური ანალიზი და შევსება (ზუსტად 1 ხატულით და მკაფიო ტექსტით)
                            Button(
                                onClick = {
                                    if (isAiGenerating) return@Button
                                    if (title.trim().isBlank()) {
                                        isAiError = true
                                        aiStatusMessage = "⚠️ გთხოვთ ჯერ შეიყვანოთ პროდუქტის ან მოდელის დასახელება"
                                        Toast.makeText(context, "⚠️ ჯერ მიუთითეთ პროდუქტის დასახელება!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isAiGenerating = true
                                    isAiError = false
                                    aiStatusMessage = "AI იკვლევს ზუსტ მოდელს და ამოწმებს ოფიციალურ ტექნიკურ მონაცემებს..."
                                    coroutineScope.launch {
                                        val generated = AdminProductAiGenerator.generateForTitle(
                                            rawTitle = title,
                                            existingCategories = categories,
                                            existingBrands = brands,
                                            selectedProductType = if (title.trim().isNotBlank()) "" else selectedCategoryType.id,
                                            selectedCategory = category
                                        )
                                        isAiGenerating = false
                                        if (generated.errorMessage != null) {
                                            isAiError = true
                                            aiReviewFields = null
                                            aiStatusMessage = "⚠️ ${generated.errorMessage}"
                                        } else {
                                            isAiError = false
                                            aiReviewFields = generated
                                            aiStatusMessage = "✅ AI კვლევა დასრულდა (${generated.categoryType.labelGeo}). გადახედეთ შედეგებს ქვემოთ."
                                        }
                                    }
                                },
                                enabled = !isAiGenerating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFF7ED),
                                    contentColor = if (isDark) Color.White else Color(0xFFC2410C),
                                    disabledContainerColor = if (isDark) Color(0xFF161C28) else Color(0xFFF1F5F9)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("ai_autofill_product_button")
                            ) {
                                if (isAiGenerating) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFF7A00),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI იკვლევს მოდელს...",
                                        color = if (isDark) Color(0xFFFF9E40) else Color(0xFFC2410C),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI დეტალური ანალიზი",
                                        tint = Color(0xFFFF7A00),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AI-ით დეტალური ანალიზი და შევსება",
                                        color = if (isDark) Color.White else Color(0xFFC2410C),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (!aiStatusMessage.isNullOrBlank()) {
                                val statusTag = when {
                                    isAiGenerating -> "ai_status_loading"
                                    isAiError -> "ai_status_error"
                                    else -> "ai_status_completed"
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isAiError -> if (isDark) Color(0xFF2D1215) else Color(0xFFFEE2E2)
                                        else -> if (isDark) Color(0xFF0F2618) else Color(0xFFDCFCE7)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when {
                                            isAiError -> Color(0xFFFF4D4F)
                                            else -> if (isDark) Color(0xFF1B4E2E) else Color(0xFF86EFAC)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(statusTag)
                                ) {
                                    Text(
                                        text = aiStatusMessage!!,
                                        color = when {
                                            isAiError -> Color(0xFFFF4D4F)
                                            else -> if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // AI Review Card (გადახედვა & დადასტურება — ხელით შეყვანილი ველების დაცვით)
                            if (aiReviewFields != null) {
                                val gen = aiReviewFields!!
                                AdminAiProductReviewCard(
                                    generated = gen,
                                    onAcceptAll = {
                                        val currentManualFeatures = featuresText
                                            .lines()
                                            .map { it.trim().removePrefix("•").removePrefix("-").trim() }
                                            .filter { it.isNotBlank() }

                                        val merged = AdminProductAiGenerator.mergeGeneratedWithManualFields(
                                            generated = gen,
                                            manual = ExistingProductFormState(
                                                price = price,
                                                brand = brand,
                                                category = category,
                                                subcategory = subcategory,
                                                shortDescription = shortDescription,
                                                description = description,
                                                featuresText = featuresText,
                                                targetAudience = targetAudience,
                                                offerType = offerType,
                                                duration = duration,
                                                accountType = accountType,
                                                activationTerms = activationTerms,
                                                compatibleModels = compatibleModels,
                                                warranty = warranty,
                                                searchKeywords = searchKeywords,
                                                exactModel = exactModel,
                                                condition = condition,
                                                specifications = specificationsMap
                                            )
                                        )

                                        selectedCategoryType = gen.categoryType
                                        productType = gen.categoryType.id
                                        if (merged.price.isNotBlank()) price = merged.price
                                        shortDescription = merged.shortDescription
                                        description = merged.description
                                        category = merged.category
                                        brand = merged.brand
                                        if (gen.categoryType == ProductCategoryType.DIGITAL_SERVICE) {
                                            subcategory = merged.subcategory
                                            offerType = merged.offerType
                                            duration = merged.duration
                                            accountType = merged.accountType
                                            activationTerms = merged.activationTerms
                                        } else {
                                            subcategory = ""
                                            offerType = ""
                                            duration = ""
                                            accountType = ""
                                            activationTerms = ""
                                        }
                                        if (merged.features.isNotEmpty()) {
                                            featuresText = merged.features.joinToString("\n")
                                        }
                                        targetAudience = merged.targetAudience
                                        compatibleModels = merged.compatibleModels
                                        if (merged.warranty.isNotBlank()) warranty = merged.warranty
                                        searchKeywords = merged.searchKeywords
                                        if (merged.exactModel.isNotBlank()) exactModel = merged.exactModel
                                        if (merged.condition.isNotBlank()) condition = merged.condition
                                        specificationsMap = merged.specifications.toMutableMap()
                                        aiReviewFields = null
                                        aiStatusMessage = "✅ დადასტურებული მონაცემები შევსებულია (ხელით შეყვანილი ველები შენარჩუნდა)."
                                    },
                                    onDismiss = { aiReviewFields = null }
                                )
                            }
                        }
                    }

                    // 1.2. პროდუქტის ტიპის დინამიკური არჩევა (8 კატეგორია) + ზუსტი მოდელი + მდგომარეობა
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF12151C) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF28303E) else Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "🏷️ პროდუქტის ტიპი და მოდელის იდენტიფიკაცია:",
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "აირჩიეთ პროდუქტის ტიპი (განსაზღვრავს ფორმის სპეციფიკურ ველებს):",
                                        color = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.testTag("product_type_selector_row")
                                    ) {
                                        items(ProductCategoryType.entries.toList()) { typeOption ->
                                            val isSel = selectedCategoryType == typeOption
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) Color(0xFFFF6A00) else (if (isDark) Color(0xFF1E2430) else Color(0xFFE2E8F0)),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSel) Color(0xFFFF6A00) else Color.Transparent
                                                ),
                                                modifier = Modifier
                                                    .clickable { requestCategoryTypeSwitch(typeOption) }
                                                    .testTag("product_type_chip_${typeOption.id}")
                                            ) {
                                                Text(
                                                    text = typeOption.labelGeo,
                                                    color = if (isSel) Color.White else (if (isDark) Color(0xFFA0A5B0) else Color(0xFF334155)),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (currentFormConfig.showExactModelField) {
                                    OutlinedTextField(
                                        value = exactModel,
                                        onValueChange = { exactModel = it },
                                        label = { Text("ზუსტი მოდელის კოდი (Exact Model)") },
                                        placeholder = { Text("მაგ: SM-S928B/DS, Huawei B535-232, A2848...", fontSize = 11.5.sp) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                            focusedBorderColor = Color(0xFFFF6A00),
                                            unfocusedBorderColor = if (isDark) Color(0xFF2E3440) else Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("exact_model_input")
                                    )
                                }

                                if (currentFormConfig.showConditionField) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            "ფიზიკური მდგომარეობა (მხოლოდ ადმინისტრატორის მიერ დასაზუსტებელი):",
                                            color = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                        val condOptions = listOf("ახალი", "მეორადი (იდეალური)", "მეორადი (კარგი)", "Refurbished")
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(condOptions) { cond ->
                                                val isSel = condition == cond
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) Color(0xFFFF6A00) else (if (isDark) Color(0xFF1E2430) else Color(0xFFE2E8F0)),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFF6A00) else Color.Transparent),
                                                    modifier = Modifier
                                                        .clickable { condition = if (isSel) "" else cond }
                                                        .testTag("condition_chip_$cond")
                                                ) {
                                                    Text(
                                                        text = cond,
                                                        color = if (isSel) Color.White else (if (isDark) Color(0xFFA0A5B0) else Color(0xFF334155)),
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 1.25. კატეგორიის სპეციფიკური სტრუქტურირებული ველები (Dynamic Category-Specific Schema Fields)
                    if (currentFormConfig.specFields.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) Color(0xFF12151C) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dynamic_category_fields_section")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "📋 ${selectedCategoryType.labelGeo} — სპეციფიკური პარამეტრები:",
                                        color = Color(0xFFFF9E40),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    currentFormConfig.specFields.forEach { specDef ->
                                        val currentVal = specificationsMap[specDef.key] ?: ""
                                        OutlinedTextField(
                                            value = currentVal,
                                            onValueChange = { updatedText ->
                                                val updated = specificationsMap.toMutableMap()
                                                if (updatedText.isBlank()) {
                                                    updated.remove(specDef.key)
                                                } else {
                                                    updated[specDef.key] = updatedText
                                                }
                                                specificationsMap = updated
                                            },
                                            label = {
                                                Text(
                                                    text = if (specDef.isSellerProvided && !specDef.labelGeo.contains("ხელით")) {
                                                        "${specDef.labelGeo} (ხელით)"
                                                    } else {
                                                        specDef.labelGeo
                                                    },
                                                    fontSize = 11.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            placeholder = { Text(specDef.placeholderGeo, fontSize = 11.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                                unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                                focusedBorderColor = Color(0xFFFF6A00),
                                                unfocusedBorderColor = if (isDark) Color(0xFF2E3440) else Color(0xFFCBD5E1)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag(specDef.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 1.3. ტექნიკური მახასიათებლები (Dynamic Technical Specifications)
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF12151C) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF28303E) else Color(0xFFCBD5E1)),
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
                                    Text(
                                        "⚙️ ტექნიკური მახასიათებლები (Specs):",
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFF6A00).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "${specificationsMap.size} პარამეტრი",
                                            color = Color(0xFFFF6A00),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (specificationsMap.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        specificationsMap.entries.forEach { (k, v) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isDark) Color(0xFF1A1F2B) else Color(0xFFF1F5F9))
                                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(k, color = Color(0xFFFF9E40), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(v, color = if (isDark) Color.White else Color(0xFF1E293B), fontSize = 11.5.sp)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = specificationsMap.toMutableMap()
                                                        updated.remove(k)
                                                        specificationsMap = updated
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remove spec", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                // ახალი პარამეტრის დამატება
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newSpecKey,
                                        onValueChange = { newSpecKey = it },
                                        placeholder = {
                                            Text(
                                                "პარამეტრი",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                            focusedBorderColor = Color(0xFFFF6A00),
                                            unfocusedBorderColor = if (isDark) Color(0xFF2E3440) else Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    )
                                    OutlinedTextField(
                                        value = newSpecVal,
                                        onValueChange = { newSpecVal = it },
                                        placeholder = {
                                            Text(
                                                "მნიშვნელობა",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                            unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                                            focusedBorderColor = Color(0xFFFF6A00),
                                            unfocusedBorderColor = if (isDark) Color(0xFF2E3440) else Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (newSpecKey.isNotBlank() && newSpecVal.isNotBlank()) {
                                                val updated = specificationsMap.toMutableMap()
                                                updated[newSpecKey.trim()] = newSpecVal.trim()
                                                specificationsMap = updated
                                                newSpecKey = ""
                                                newSpecVal = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(width = 48.dp, height = 52.dp)
                                    ) {
                                        Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 1.5 პროდუქტის ფოტოს / ქარდის ვიზუალის მართვა (ყველა პროდუქტისთვის: ავტო-გენერაცია + ტელეფონის მეხსიერება + რეალური ლინკი)
                    item {
                        val isDark = AppPreferencesManager.isDarkMode
                        val isUsingCustomPhoto = ProductImageManager.hasCustomAttachedPhoto(previewProduct)
                        val isLocalGalleryPhoto = isUsingCustomPhoto && ProductImageManager.isLocalOrEmbeddedImage(imageUrl)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF12151C) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0xFF28303E) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🖼️ ქარდის ფოტო / ვიზუალი:",
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when {
                                            isUsingCustomPhoto && isDark -> Color(0x264ADE80)
                                            isUsingCustomPhoto && !isDark -> Color(0xFFDCFCE7)
                                            isDark -> Color(0x26FF7A00)
                                            else -> Color(0xFFFFEDD5)
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            when {
                                                isUsingCustomPhoto && isDark -> Color(0x404ADE80)
                                                isUsingCustomPhoto && !isDark -> Color(0xFF86EFAC)
                                                isDark -> Color(0x40FF7A00)
                                                else -> Color(0xFFFDBA74)
                                            }
                                        )
                                    ) {
                                        Text(
                                            text = when {
                                                isLocalGalleryPhoto -> "📱 გალერეიდან"
                                                isUsingCustomPhoto -> "🔗 მიბმული ლინკით"
                                                else -> "🎨 ავტო-გენერაცია"
                                            },
                                            color = when {
                                                isUsingCustomPhoto && isDark -> Color(0xFF4ADE80)
                                                isUsingCustomPhoto && !isDark -> Color(0xFF15803D)
                                                isDark -> Color(0xFFFF9E40)
                                                else -> Color(0xFFEA580C)
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // ცოცხალი გადახედვა (Live Card Visual Preview)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(116.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0D1017))
                                        .border(1.dp, Color(0xFF262D3B), RoundedCornerShape(12.dp))
                                ) {
                                    ProductSmartVisual(
                                        product = previewProduct,
                                        modifier = Modifier.fillMaxSize(),
                                        compact = false
                                    )
                                }

                                // 3 რეჟიმის ღილაკი: ავტო-გენერაცია | მეხსიერებიდან (გალერეა) | ლინკით
                                val isAutoSelected = !isUsingCustomPhoto && !showUrlInputBox
                                val isGallerySelected = isLocalGalleryPhoto
                                val isUrlSelected = showUrlInputBox || (isUsingCustomPhoto && !isLocalGalleryPhoto)

                                val unselectedBtnBg = if (isDark) Color(0xFF1E2430) else Color(0xFFFFFFFF)
                                val unselectedBtnBorder = if (isDark) Color(0xFF333B4D) else Color(0xFFCBD5E1)
                                val unselectedBtnText = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                val unselectedBtnIcon = if (isDark) Color(0xFFFF9E40) else Color(0xFFEA580C)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1) ავტო-გენერაცია
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isAutoSelected) Color(0xFFFF6A00) else unselectedBtnBg,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isAutoSelected) Color(0xFFFF6A00) else unselectedBtnBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clickable {
                                                imageUrl = ProductImageManager.AUTO_GENERATED_IMAGE_MARKER
                                                showUrlInputBox = false
                                            }
                                            .testTag("photo_mode_auto_button")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Auto Photo",
                                                tint = if (isAutoSelected) Color.White else unselectedBtnIcon,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ავტო-ფოტო",
                                                color = if (isAutoSelected) Color.White else unselectedBtnText,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // 2) ტელეფონის მეხსიერებიდან არჩევა (მრავალფოტოიანი არჩევა)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isGallerySelected) Color(0xFFFF6A00) else unselectedBtnBg,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isGallerySelected) Color(0xFFFF6A00) else unselectedBtnBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clickable {
                                                multiPhotoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                 )
                                            }
                                            .testTag("photo_mode_gallery_button")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = "Pick Photo From Phone Memory",
                                                tint = if (isGallerySelected) Color.White else unselectedBtnIcon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "მრავალფოტო",
                                                color = if (isGallerySelected) Color.White else unselectedBtnText,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    // 3) რეალური ლინკით მიბმა
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isUrlSelected) Color(0xFFFF6A00) else unselectedBtnBg,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isUrlSelected) Color(0xFFFF6A00) else unselectedBtnBorder
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clickable {
                                                showUrlInputBox = true
                                                if (urlInputText.isNotBlank()) {
                                                    imageUrl = urlInputText.trim()
                                                }
                                            }
                                            .testTag("photo_mode_url_button")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = "Photo URL Link",
                                                tint = if (isUrlSelected) Color.White else unselectedBtnIcon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ლინკით",
                                                color = if (isUrlSelected) Color.White else unselectedBtnText,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // ლინკის ჩასაწერი ველი და დამატების ღილაკი
                                if (showUrlInputBox || (isUsingCustomPhoto && !isLocalGalleryPhoto)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = urlInputText,
                                            onValueChange = { newUrl ->
                                                urlInputText = newUrl
                                                imageUrl = newUrl.trim().ifBlank { ProductImageManager.AUTO_GENERATED_IMAGE_MARKER }
                                            },
                                            label = { Text("ფოტოს რეალური ლინკი (https://...)") },
                                            placeholder = { Text("ჩასვით სურათის პირდაპირი URL ბმული...", fontSize = 11.5.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                focusedBorderColor = Color(0xFFFF6A00),
                                                unfocusedBorderColor = Color(0xFF2E3440)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("product_image_url_input")
                                        )
                                        Button(
                                            onClick = {
                                                val trimmed = urlInputText.trim()
                                                if (trimmed.isNotBlank() && !productImages.contains(trimmed)) {
                                                    productImages = (productImages + trimmed).toMutableList()
                                                    if (primaryImageUrl.isBlank() || primaryImageUrl == ProductImageManager.AUTO_GENERATED_IMAGE_MARKER) {
                                                        primaryImageUrl = trimmed
                                                        imageUrl = trimmed
                                                    }
                                                    urlInputText = ""
                                                    Toast.makeText(context, "✅ ფოტოს ლინკი დაემატა!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // 4) მრავალფოტოიანი გალერეის სია (Multi-Image Gallery List)
                                if (productImages.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "ატვირთული ფოტოები (${productImages.size}):",
                                                color = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "⭐ მონიშნეთ მთავარი ფოტო",
                                                color = Color(0xFFFFB800),
                                                fontSize = 10.sp
                                            )
                                        }

                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(productImages, key = { it }) { imgPath ->
                                                val isPrimary = primaryImageUrl == imgPath || (primaryImageUrl.isBlank() && productImages.firstOrNull() == imgPath)
                                                val imgIndex = productImages.indexOf(imgPath)

                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isDark) Color(0xFF1E2430) else Color(0xFFF1F5F9),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.5.dp,
                                                        if (isPrimary) Color(0xFFFF6A00) else Color(0xFF333B4D)
                                                    ),
                                                    modifier = Modifier.width(105.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(4.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(97.dp, 75.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.Black)
                                                        ) {
                                                            SafeProductAsyncImage(
                                                                imageUrl = imgPath,
                                                                contentDescription = "Product image",
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )

                                                            if (isPrimary) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                                                                    color = Color(0xFFFF6A00),
                                                                    modifier = Modifier.align(Alignment.TopStart)
                                                                ) {
                                                                    Text("⭐ მთავარი", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                                }
                                                            }
                                                        }

                                                        // მოქმედებების ღილაკები: Star (Primary), Crop, Delete, Left/Right
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceAround,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Primary toggle
                                                            IconButton(
                                                                onClick = {
                                                                    primaryImageUrl = imgPath
                                                                    imageUrl = imgPath
                                                                },
                                                                modifier = Modifier.size(22.dp)
                                                            ) {
                                                                Icon(
                                                                    if (isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                                                                    contentDescription = "Set Primary",
                                                                    tint = if (isPrimary) Color(0xFFFFB800) else Color.Gray,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                            }

                                                            // Crop button
                                                            IconButton(
                                                                onClick = { imageToCrop = imgPath },
                                                                modifier = Modifier.size(22.dp)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Crop,
                                                                    contentDescription = "Crop",
                                                                    tint = Color(0xFFFF7A00),
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }

                                                            // Move Left
                                                            if (imgIndex > 0) {
                                                                IconButton(
                                                                    onClick = {
                                                                        val list = productImages.toMutableList()
                                                                        val tmp = list[imgIndex]
                                                                        list[imgIndex] = list[imgIndex - 1]
                                                                        list[imgIndex - 1] = tmp
                                                                        productImages = list
                                                                    },
                                                                    modifier = Modifier.size(20.dp)
                                                                ) {
                                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", tint = Color.LightGray, modifier = Modifier.size(13.dp))
                                                                }
                                                            }

                                                            // Delete image
                                                            IconButton(
                                                                onClick = {
                                                                    val list = productImages.toMutableList()
                                                                    list.remove(imgPath)
                                                                    productImages = list
                                                                    if (primaryImageUrl == imgPath) {
                                                                        primaryImageUrl = productImages.firstOrNull() ?: ""
                                                                        imageUrl = primaryImageUrl.ifBlank { ProductImageManager.AUTO_GENERATED_IMAGE_MARKER }
                                                                    }
                                                                },
                                                                modifier = Modifier.size(22.dp)
                                                             ) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D4F), modifier = Modifier.size(14.dp))
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

                    // 2. ფასი + სწრაფი არჩევის ღილაკები (მხოლოდ ადმინისტრატორის მიერ შესავსები)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("ფასი (მაგ: 65 ₾ ან ფასი შეთანხმებით)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF4ADE80),
                                    unfocusedTextColor = Color(0xFF4ADE80),
                                    focusedBorderColor = Color(0xFFFF6A00),
                                    unfocusedBorderColor = Color(0xFF2E3440)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("product_price_input")
                            )

                            Text("სწრაფი ფასის არჩევა:", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(quickPrices) { qp ->
                                    val isSel = price.trim() == qp
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSel) Color(0xFFFF6A00) else Color(0xFF222630),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSel) Color(0xFFFF7A00) else Color(0xFF2E3440)
                                        ),
                                        modifier = Modifier.clickable { price = qp }
                                    ) {
                                        Text(
                                            text = qp,
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

                    // 3. მოკლე აღწერა (ჩანს კატალოგისა და მთავარი გვერდის ქარდზე)
                    item {
                        OutlinedTextField(
                            value = shortDescription,
                            onValueChange = { shortDescription = it },
                            label = { Text("მოკლე აღწერა (ჩანს ბარათზე)") },
                            placeholder = { Text("მოკლე ქართული აღწერა კატალოგის ბარათისთვის...", fontSize = 12.sp) },
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF6A00),
                                unfocusedBorderColor = Color(0xFF2E3440)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_short_description_input")
                        )
                    }

                    // 4. სრული / დეტალური აღწერა (ჩანს პროდუქტის დეტალურ გვერდზე)
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("სრული / დეტალური აღწერა (ჩანს დეტალურ ფანჯარაში)") },
                            minLines = 3,
                            maxLines = 7,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF6A00),
                                unfocusedBorderColor = Color(0xFF2E3440)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_description_input")
                        )
                    }

                    // 5. კატეგორიის არჩევა + ხელით მითითება
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("კატეგორია (აირჩიეთ ან ჩაწერეთ ხელით):", color = Color(0xFFA0A5B0), fontSize = 12.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(categories) { c ->
                                    val isSelected = category.trim().equals(c, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF222630),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFFFF6A00) else Color(0xFF2E3440)
                                        ),
                                        modifier = Modifier.clickable { category = c }
                                    ) {
                                        Text(
                                            text = c,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = {
                                    Text(
                                        "კატეგორია (ხელით / ახალი)",
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                placeholder = {
                                    Text(
                                        "მაგ: მობილური ტელეფონები, ეკრანები...",
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFF6A00),
                                    unfocusedBorderColor = Color(0xFF2E3440)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_category_input")
                            )
                        }
                    }

                    // 6. ბრენდის არჩევა + ხელით მითითება
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ბრენდი (აირჩიეთ ან ჩაწერეთ ხელით):", color = Color(0xFFA0A5B0), fontSize = 12.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(brands) { b ->
                                    val isSelected = brand.trim().equals(b, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF222630),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFFFF6A00) else Color(0xFF2E3440)
                                        ),
                                        modifier = Modifier.clickable { brand = b }
                                    ) {
                                        Text(
                                            text = b,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = brand,
                                onValueChange = { brand = it },
                                label = {
                                    Text(
                                        "ბრენდი / მწარმოებელი",
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                placeholder = {
                                    Text(
                                        "მაგ: Samsung, Apple, Huawei, Xiaomi...",
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFF6A00),
                                    unfocusedBorderColor = Color(0xFF2E3440)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_brand_input")
                            )
                        }
                    }

                    // 7. ციფრული / პროგრამული სერვისის სპეციფიკური ველები (მხოლოდ DIGITAL_SERVICE ტიპისთვის!)
                    if (isSoftwareService) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.testTag("software_specific_fields_section")
                            ) {
                                Text("პროგრამული ქვეკატეგორია:", color = Color(0xFFFF9E40), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(softwareSubcategories) { sc ->
                                        val isSel = subcategory == sc
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSel) Color(0xFFFF6A00) else Color(0xFF222630),
                                            modifier = Modifier.clickable { subcategory = sc }
                                        ) {
                                            Text(
                                                text = sc,
                                                color = Color.White,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = subcategory,
                                    onValueChange = { subcategory = it },
                                    label = { Text("ქვეკატეგორია (ხელით მითითება)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF6A00),
                                        unfocusedBorderColor = Color(0xFF2E3440)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("custom_subcategory_input")
                                )
                            }
                        }

                        // პაკეტის ტიპი & მოქმედების ვადა
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = offerType,
                                    onValueChange = { offerType = it },
                                    label = { Text("პაკეტის / შეთავაზების ტიპი (Offer Type)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF6A00),
                                        unfocusedBorderColor = Color(0xFF2E3440)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("offer_type_input")
                                )

                                OutlinedTextField(
                                    value = duration,
                                    onValueChange = { duration = it },
                                    label = { Text("მოქმედების ვადა (მაგ: 1 თვე, 18 თვე, უვადო)") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF6A00),
                                        unfocusedBorderColor = Color(0xFF2E3440)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("duration_input")
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(quickDurations) { d ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (duration == d) Color(0xFFFF6A00) else Color(0xFF222630),
                                            modifier = Modifier.clickable { duration = d }
                                        ) {
                                            Text(
                                                text = d,
                                                color = Color.White,
                                                fontSize = 10.5.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ანგარიშის ტიპი
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = accountType,
                                    onValueChange = { accountType = it },
                                    label = { Text("ანგარიშის / ლიცენზიის ტიპი") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF6A00),
                                        unfocusedBorderColor = Color(0xFF2E3440)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("account_type_input")
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(quickAccountTypes) { acc ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (accountType == acc) Color(0xFFFF6A00) else Color(0xFF222630),
                                            modifier = Modifier.clickable { accountType = acc }
                                        ) {
                                            Text(
                                                text = acc,
                                                color = Color.White,
                                                fontSize = 10.5.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // სამიზნე აუდიტორია, აქტივაციის და უსაფრთხოების პირობები
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = targetAudience,
                                    onValueChange = { targetAudience = it },
                                    label = { Text("ვისთვის არის განკუთვნილი") },
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF6A00),
                                        unfocusedBorderColor = Color(0xFF2E3440)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("target_audience_input")
                                )

                                OutlinedTextField(
                                    value = activationTerms,
                                    onValueChange = { activationTerms = it },
                                    label = { Text("აქტივაციისა და უსაფრთხოების პირობები") },
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF6A00),
                                        unfocusedBorderColor = Color(0xFF2E3440)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("activation_terms_input")
                                )
                            }
                        }
                    }

                    // 7.5 ძირითადი უპირატესობები / მახასიათებლები (Features)
                    item {
                        OutlinedTextField(
                            value = featuresText,
                            onValueChange = { featuresText = it },
                            label = {
                                Text(
                                    if (isSoftwareService) "ძირითადი შესაძლებლობები (ახალ ხაზზე)" else "მთავარი მახასიათებლები (ახალ ხაზზე)",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            minLines = 2,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF6A00),
                                unfocusedBorderColor = Color(0xFF2E3440)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_features_input")
                        )
                    }

                    // თავსებადი მოდელები / პლატფორმები (მხოლოდ იმ ტიპებისთვის, სადაც რელევანტურია) და გარანტია
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (currentFormConfig.showCompatibleModels) {
                                OutlinedTextField(
                                    value = compatibleModels,
                                    onValueChange = { compatibleModels = it },
                                    label = {
                                        Text(
                                            currentFormConfig.compatibleModelsLabelGeo,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFFF6A00),
                                        unfocusedBorderColor = Color(0xFF2E3440)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("compatible_models_input")
                                )
                            }

                            OutlinedTextField(
                                value = warranty,
                                onValueChange = { warranty = it },
                                label = {
                                    Text(
                                        "გარანტია (ხელით მისათითებელი)",
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFF6A00),
                                    unfocusedBorderColor = Color(0xFF2E3440)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("warranty_input")
                            )
                        }
                    }

                    // მარაგშია / ხელმისაწვდომია & Featured / რიგითობა
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ხელმისაწვდომია / მარაგშია (In Stock)", color = Color.White, fontSize = 13.sp)
                                Switch(
                                    checked = inStock,
                                    onCheckedChange = { inStock = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFF6A00)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("მთავარ ეკრანზე გამოტანა (Featured)", color = Color(0xFFFF9E40), fontSize = 13.sp)
                                Switch(
                                    checked = isFeatured,
                                    onCheckedChange = { isFeatured = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFF6A00)
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = displayOrderInput,
                                onValueChange = { displayOrderInput = it.filter { ch -> ch.isDigit() } },
                                label = { Text("რიგითობა / პრიორიტეტი (Sort Order)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFFF6A00),
                                    unfocusedBorderColor = Color(0xFF2E3440)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0))
                    ) {
                        Text("გაუქმება", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val trimmedPrice = price.trim()
                            val normalizedPrice = when {
                                trimmedPrice.isBlank() -> "ფასი შეთანხმებით"
                                trimmedPrice.all { it.isDigit() || it == '.' || it == ',' } -> "$trimmedPrice ₾"
                                else -> trimmedPrice
                            }
                            val parsedFeatures = featuresText
                                .lines()
                                .map { it.trim().removePrefix("•").removePrefix("-").trim() }
                                .filter { it.isNotBlank() }

                            val normalizedCategory = category.trim().ifBlank {
                                currentFormConfig.categoryNameGeo
                            }
                            val normalizedBrand = brand.trim()
                            val savedImageUrl = imageUrl.trim().ifBlank {
                                ProductImageManager.AUTO_GENERATED_IMAGE_MARKER
                            }

                            val finalImages = if (productImages.isNotEmpty()) productImages else if (savedImageUrl.isNotBlank() && savedImageUrl != ProductImageManager.AUTO_GENERATED_IMAGE_MARKER) listOf(savedImageUrl) else emptyList()
                            val finalPrimary = primaryImageUrl.ifBlank { finalImages.firstOrNull() ?: savedImageUrl }

                            val finalItem = product.copy(
                                title = title.trim().ifBlank { selectedCategoryType.labelGeo },
                                price = normalizedPrice,
                                brand = normalizedBrand,
                                category = normalizedCategory,
                                type = if (isSoftwareService) "software" else "product",
                                subcategory = if (isSoftwareService) subcategory.trim() else "",
                                shortDescription = shortDescription.trim(),
                                description = description.trim().ifBlank { shortDescription.trim() },
                                features = if (parsedFeatures.isNotEmpty()) parsedFeatures else product.features,
                                targetAudience = targetAudience.trim(),
                                offerType = if (isSoftwareService) offerType.trim() else "",
                                duration = if (isSoftwareService) duration.trim() else "",
                                accountType = if (isSoftwareService) accountType.trim() else "",
                                activationTerms = if (isSoftwareService) activationTerms.trim() else "",
                                compatibleModels = if (currentFormConfig.showCompatibleModels) compatibleModels.trim() else "",
                                warranty = warranty.trim(),
                                searchKeywords = searchKeywords.trim(),
                                imageUrl = finalPrimary.ifBlank { savedImageUrl },
                                images = finalImages,
                                primaryImageUrl = finalPrimary,
                                specifications = specificationsMap,
                                productType = selectedCategoryType.id,
                                condition = condition,
                                exactModel = exactModel.trim(),
                                inStock = inStock,
                                isFeatured = isFeatured,
                                displayOrder = displayOrderInput.toIntOrNull() ?: product.displayOrder
                            )
                            onSave(finalItem)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_product_button")
                    ) {
                        Text("შენახვა", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ფოტოს ჩამოჭრის დიალოგი (Image Cropper Dialog)
    if (imageToCrop != null) {
        ImageCropperDialog(
            imageUriOrUrl = imageToCrop!!,
            onDismiss = { imageToCrop = null },
            onCropFinished = { croppedUri ->
                val currentTarget = imageToCrop
                val idx = productImages.indexOf(currentTarget)
                if (idx >= 0) {
                    val updated = productImages.toMutableList()
                    updated[idx] = croppedUri
                    productImages = updated
                } else {
                    productImages = (productImages + croppedUri).toMutableList()
                }
                if (primaryImageUrl == currentTarget || primaryImageUrl.isBlank()) {
                    primaryImageUrl = croppedUri
                    imageUrl = croppedUri
                }
                imageToCrop = null
                Toast.makeText(context, "✅ ფოტო წარმატებით ჩამოიჭრა და შეინახა!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ==============================================================================
// 6. მარჟისა და მასიური ფასების ცვლილების ეკრანი
// ==============================================================================
@Composable
fun BulkPriceAndMarginScreen(
    products: List<ProductItem>,
    allBrands: List<String>,
    allCategories: List<String>,
    onApply: (targetBrand: String, targetCategory: String, mode: String, value: Double) -> Unit,
    onResetDefaults: () -> Unit
) {
    var selectedBrand by remember { mutableStateOf("ყველა") }
    var selectedCategory by remember { mutableStateOf("ყველა") }
    var mode by remember { mutableStateOf("PERCENT") } // "PERCENT" ან "FIXED"
    var adjustmentValue by remember { mutableDoubleStateOf(10.0) }
    var customInput by remember { mutableStateOf("10") }
    var showResetConfirm by remember { mutableStateOf(false) }

    val affectedCount = remember(products, selectedBrand, selectedCategory) {
        products.count { item ->
            val matchBrand = selectedBrand == "ყველა" || item.brand.equals(selectedBrand, ignoreCase = true)
            val matchCat = selectedCategory == "ყველა" || item.category.equals(selectedCategory, ignoreCase = true)
            matchBrand && matchCat
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎯 სამიზნე პროდუქტების არჩევა", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    // ბრენდის არჩევა
                    Text("ბრენდი:", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allBrands) { brand ->
                            val isSel = selectedBrand == brand
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFFFF6A00) else Color(0xFF222630),
                                modifier = Modifier.clickable { selectedBrand = brand }
                            ) {
                                Text(
                                    text = brand,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // კატეგორიის არჩევა
                    Text("კატეგორია:", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allCategories) { cat ->
                            val isSel = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFFFF6A00) else Color(0xFF222630),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // მარჟის / ფასის რეჟიმის არჩევა
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📈 ცვლილების ტიპი & მარჟა", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { mode = "PERCENT"; customInput = "10"; adjustmentValue = 10.0 },
                            colors = ButtonDefaults.buttonColors(containerColor = if (mode == "PERCENT") Color(0xFFFF6A00) else Color(0xFF222630)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("პროცენტული მარჟა (%)", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { mode = "FIXED"; customInput = "20"; adjustmentValue = 20.0 },
                            colors = ButtonDefaults.buttonColors(containerColor = if (mode == "FIXED") Color(0xFFFF6A00) else Color(0xFF222630)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ფიქსირებული თანხა (₾)", fontSize = 11.sp)
                        }
                    }

                    // სწრაფი ღილაკები
                    Text("სწრაფი მნიშვნელობები:", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                    val presets = if (mode == "PERCENT") {
                        listOf("+5%", "+10%", "+15%", "+20%", "+25%", "-5%", "-10%")
                    } else {
                        listOf("+10 ₾", "+20 ₾", "+30 ₾", "+50 ₾", "-10 ₾", "-20 ₾")
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presets) { p ->
                            val num = p.replace("%", "").replace("₾", "").trim().toDoubleOrNull() ?: 0.0
                            val isSel = adjustmentValue == num
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFFFF6A00) else Color(0xFF222630),
                                modifier = Modifier.clickable {
                                    adjustmentValue = num
                                    customInput = num.toInt().toString()
                                }
                            ) {
                                Text(
                                    text = p,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // საკუთარი მნიშვნელობის შეყვანა
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = {
                            customInput = it
                            adjustmentValue = it.toDoubleOrNull() ?: 0.0
                        },
                        label = { Text(if (mode == "PERCENT") "მარჟის % (მაგ: 15 ან -10)" else "თანხა ₾ (მაგ: 25 ან -15)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF2E3440)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ცვლილების წინასწარი გადახედვა
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF101318),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🔍 ცვლილების შეჯამება:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("• შეეხება: $affectedCount პროდუქტს ($selectedBrand / $selectedCategory)", color = Color(0xFFA0A5B0), fontSize = 12.sp)

                    val exampleOld = 300
                    val exampleNew = if (mode == "PERCENT") {
                        val adj = exampleOld + (exampleOld * (adjustmentValue / 100.0))
                        maxOf(10, ((adj / 5.0).toInt()) * 5)
                    } else {
                        maxOf(10, (exampleOld + adjustmentValue).toInt())
                    }
                    Text(
                        text = "• მაგალითი: 300 ₾ ➔ $exampleNew ₾ (${if (adjustmentValue >= 0) "+$adjustmentValue" else "$adjustmentValue"}${if (mode == "PERCENT") "%" else " ₾"})",
                        color = Color(0xFF4ADE80),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // მოქმედების ღილაკები
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onApply(selectedBrand, selectedCategory, mode, adjustmentValue)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PriceCheck, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ფასების მასიური განახლება ($affectedCount ნივთი)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF4D4F))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFFF4D4F), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("საწყის ფასებზე დაბრუნება (Reset)", color = Color(0xFFFF4D4F), fontSize = 12.sp)
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("საწყის ფასებზე დაბრუნება", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("გსურთ კატალოგის ყველა ფასის დაბრუნება საწყის ქარხნულ პარამეტრებზე?", color = Color(0xFFA0A5B0)) },
            containerColor = Color(0xFF16191E),
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetDefaults()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F))
                ) {
                    Text("დაბრუნება")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("გაუქმება", color = Color(0xFFA0A5B0))
                }
            }
        )
    }
}

// ==============================================================================
// 7. ანალიტიკა და სტატისტიკა
// ==============================================================================
@Composable
fun AdminStatisticsView(products: List<ProductItem>) {
    val totalCount = products.size
    val inStockCount = remember(products) { products.count { it.inStock } }
    val outOfStockCount = totalCount - inStockCount

    val prices = remember(products) { products.mapNotNull { it.price.filter { ch -> ch.isDigit() }.toIntOrNull() } }
    val avgPrice = remember(prices) { if (prices.isNotEmpty()) prices.average().toInt() else 0 }
    val minPrice = remember(prices) { prices.minOrNull() ?: 0 }
    val maxPrice = remember(prices) { prices.maxOrNull() ?: 0 }

    val brandCounts = remember(products) { products.groupBy { it.brand }.mapValues { it.value.size } }
    val catCounts = remember(products) { products.groupBy { it.category }.mapValues { it.value.size } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("📊 კატალოგის სტატისტიკა & მიმოხილვა", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(title = "სულ ნივთი", value = "$totalCount", color = Color(0xFFFF6A00), modifier = Modifier.weight(1f))
                StatCard(title = "მარაგშია", value = "$inStockCount", color = Color(0xFF4ADE80), modifier = Modifier.weight(1f))
                StatCard(title = "ამოიწურა", value = "$outOfStockCount", color = Color(0xFFFF4D4F), modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard(title = "საშუალო ფასი", value = "$avgPrice ₾", color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                StatCard(title = "მინ. ფასი", value = "$minPrice ₾", color = Color(0xFFA78BFA), modifier = Modifier.weight(1f))
                StatCard(title = "მაქს. ფასი", value = "$maxPrice ₾", color = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏷️ ბრენდების განაწილება", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    brandCounts.forEach { (brand, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(brand, color = Color(0xFFA0A5B0), fontSize = 12.sp)
                            Text("$count ერთეული", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📦 კატეგორიების განაწილება", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    catCounts.forEach { (cat, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat, color = Color(0xFFA0A5B0), fontSize = 12.sp)
                            Text("$count ერთეული", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF16191E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242830)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Color(0xFFA0A5B0), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==============================================================================
// 4. საბანკო რეკვიზიტების მართვის კომპონენტი (ადმინ პანელი)
// ==============================================================================
@Composable
fun AdminBankRequisitesView() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentRequisites by remember { mutableStateOf(AdminStorage.getBankRequisites(context)) }

    var bogIbanInput by remember { mutableStateOf(currentRequisites.bogIban) }
    var bogRecipientInput by remember { mutableStateOf(currentRequisites.bogRecipient) }
    var tbcIbanInput by remember { mutableStateOf(currentRequisites.tbcIban) }
    var tbcRecipientInput by remember { mutableStateOf(currentRequisites.tbcRecipient) }

    DisposableEffect(Unit) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            registration = FirestoreRepository(context).listenToBankRequisites { cloudReq ->
                AdminStorage.saveBankRequisites(context, cloudReq)
                currentRequisites = cloudReq
                bogIbanInput = cloudReq.bogIban
                bogRecipientInput = cloudReq.bogRecipient
                tbcIbanInput = cloudReq.tbcIban
                tbcRecipientInput = cloudReq.tbcRecipient
            }
        } catch (_: Exception) {}
        onDispose {
            registration?.remove()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF7A00).copy(alpha = 0.15f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "საბანკო რეკვიზიტების რედაქტირება",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "აქ შეყვანილი ანგარიშის ნომრები მყისიერად გამოჩნდება მომხმარებლის პროფილში და შეკვეთისას გადარიცხვისთვის",
                            color = Color(0xFFA0A5B0),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 1. საქართველოს ბანკი (BOG)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF5E00).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF5E00),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("BOG", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "საქართველოს ბანკი (Bank of Georgia)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("IBAN ანგარიშის ნომერი", color = Color(0xFFFF7A00), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = bogIbanInput,
                            onValueChange = { bogIbanInput = it.uppercase().replace(" ", "") },
                            placeholder = { Text("მაგ: GE17BG0000000580620970", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF7A00),
                                unfocusedBorderColor = Color(0xFF2E3440),
                                focusedContainerColor = Color(0xFF0F1216),
                                unfocusedContainerColor = Color(0xFF0F1216)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("მიმღების სახელი / კომპანია", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                        OutlinedTextField(
                            value = bogRecipientInput,
                            onValueChange = { bogRecipientInput = it },
                            placeholder = { Text("ბექასერვისი / ბექა მღებრიშვილი", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF7A00),
                                unfocusedBorderColor = Color(0xFF2E3440),
                                focusedContainerColor = Color(0xFF0F1216),
                                unfocusedContainerColor = Color(0xFF0F1216)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 2. თიბისი ბანკი (TBC)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00A3E0).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00A3E0),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("TBC", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "თიბისი ბანკი (TBC Bank)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("IBAN ანგარიშის ნომერი", color = Color(0xFF00A3E0), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = tbcIbanInput,
                            onValueChange = { tbcIbanInput = it.uppercase().replace(" ", "") },
                            placeholder = { Text("მაგ: GE73TB7114445061100057", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00A3E0),
                                unfocusedBorderColor = Color(0xFF2E3440),
                                focusedContainerColor = Color(0xFF0F1216),
                                unfocusedContainerColor = Color(0xFF0F1216)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("მიმღების სახელი / კომპანია", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                        OutlinedTextField(
                            value = tbcRecipientInput,
                            onValueChange = { tbcRecipientInput = it },
                            placeholder = { Text("ბექასერვისი / ბექა მღებრიშვილი", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00A3E0),
                                unfocusedBorderColor = Color(0xFF2E3440),
                                focusedContainerColor = Color(0xFF0F1216),
                                unfocusedContainerColor = Color(0xFF0F1216)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // მოქმედების ღილაკები
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val newReq = AdminStorage.BankRequisites(
                            bogIban = bogIbanInput.ifBlank { AdminStorage.DEFAULT_BOG_IBAN },
                            bogRecipient = bogRecipientInput.ifBlank { AdminStorage.DEFAULT_BOG_RECIPIENT },
                            tbcIban = tbcIbanInput.ifBlank { AdminStorage.DEFAULT_TBC_IBAN },
                            tbcRecipient = tbcRecipientInput.ifBlank { AdminStorage.DEFAULT_TBC_RECIPIENT }
                        )
                        AdminStorage.saveBankRequisites(context, newReq)
                        currentRequisites = newReq
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            try {
                                FirestoreRepository(context).saveBankRequisitesToCloud(newReq)
                            } catch (_: Exception) {}
                        }
                        Toast.makeText(context, "✅ საბანკო რეკვიზიტები წარმატებით შეინახა!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("რეკვიზიტების შენახვა", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val defaultReq = AdminStorage.resetBankRequisites(context)
                        bogIbanInput = defaultReq.bogIban
                        bogRecipientInput = defaultReq.bogRecipient
                        tbcIbanInput = defaultReq.tbcIban
                        tbcRecipientInput = defaultReq.tbcRecipient
                        currentRequisites = defaultReq
                        Toast.makeText(context, "🔄 დაბრუნდა საწყის რეკვიზიტებზე", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFA0A5B0), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("საწყის რეკვიზიტებზე დაბრუნება", fontSize = 12.sp)
                }
            }
        }

        // პირდაპირი ხედის ბარათი (Live Preview)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF101318),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222630)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👀 როგორ გამოჩნდება მომხმარებელთან:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("BOG IBAN: ${bogIbanInput.ifBlank { AdminStorage.DEFAULT_BOG_IBAN }}", color = Color(0xFFFF7A00), fontSize = 11.5.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("TBC IBAN: ${tbcIbanInput.ifBlank { AdminStorage.DEFAULT_TBC_IBAN }}", color = Color(0xFF00A3E0), fontSize = 11.5.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("მიმღები: ${bogRecipientInput.ifBlank { AdminStorage.DEFAULT_BOG_RECIPIENT }}", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                }
            }
        }
    }
}

// ==============================================================================
// 5. Home Screen Items მართვის პანელი (ბანერები, შეთავაზებები, დაკავშირება)
// ==============================================================================
@Composable
fun AdminHomeScreenView(products: List<ProductItem>) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var bannersList by remember { mutableStateOf(AdminStorage.getHomeBanners(context)) }
    var editingBanner by remember { mutableStateOf<AdminStorage.HomeBannerItem?>(null) }
    var isNewBanner by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ჰედერი & ახალი ბანერის დამატება
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D37)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFF6A00).copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Home Screen Items",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "მთავარი გვერდის ბანერები (${bannersList.size})",
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                editingBanner = AdminStorage.HomeBannerItem(
                                    id = System.currentTimeMillis().toString(),
                                    imageUrl = "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800",
                                    title = "",
                                    subtitle = "",
                                    targetProductId = products.firstOrNull()?.id ?: 1,
                                    tag = "🔥 სპეც შეთავაზება"
                                )
                                isNewBanner = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "დამატება",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Text(
                        text = "აქედან შეგიძლიათ შეცვალოთ, დაამატოთ ან წაშალოთ მთავარ გვერდზე მბრუნავი ბანერები. თითოეულ ბანერს უკავშირდება რეალური პროდუქტი კატალოგიდან, რომლის დეტალური გვერდიც გაიხსნება მომხმარებლის დაკლიკებისას.",
                        color = Color(0xFFA0A5B0),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Live Preview: როგორ ჩანს Home Screen-ზე
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📱 ბანერების რეალური ხედი (Live Preview):", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (bannersList.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { bannersList.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        pageSpacing = 10.dp
                    ) { page ->
                        val item = bannersList[page]
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF16191E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6A00).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.65f))
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFF7A00)
                                    ) {
                                        Text(
                                            item.tag,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(item.subtitle, color = Color(0xFFD0D5E0), fontSize = 10.5.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ბანერების სია
        item {
            Text("📋 მიმდინარე ბანერების სია:", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }

        items(bannersList) { banner ->
            val linkedProduct = products.find { it.id == banner.targetProductId }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D37)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // სურათის პრევიუ
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(60.dp),
                        color = Color(0xFF0F1216)
                    ) {
                        AsyncImage(
                            model = banner.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFF6A00).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = banner.tag,
                                    color = Color(0xFFFF7A00),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = banner.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = banner.subtitle,
                            color = Color(0xFFA0A5B0),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🔗 პროდუქტი: ${linkedProduct?.title ?: "ID #${banner.targetProductId}"}",
                            color = Color(0xFF60A5FA),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // რიგითობის შეცვლა (Up/Down), რედაქტირება & წაშლა
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val currentIndex = bannersList.indexOfFirst { it.id == banner.id }
                        if (currentIndex > 0) {
                            IconButton(
                                onClick = {
                                    val mutable = bannersList.toMutableList()
                                    val temp = mutable[currentIndex - 1]
                                    mutable[currentIndex - 1] = mutable[currentIndex]
                                    mutable[currentIndex] = temp
                                    bannersList = mutable
                                    AdminStorage.saveHomeBanners(context, mutable)
                                    coroutineScope.launch {
                                        FirestoreRepository(context).saveHomeBannersToCloud(mutable)
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color(0xFFA0A5B0), modifier = Modifier.size(18.dp))
                            }
                        }
                        if (currentIndex >= 0 && currentIndex < bannersList.size - 1) {
                            IconButton(
                                onClick = {
                                    val mutable = bannersList.toMutableList()
                                    val temp = mutable[currentIndex + 1]
                                    mutable[currentIndex + 1] = mutable[currentIndex]
                                    mutable[currentIndex] = temp
                                    bannersList = mutable
                                    AdminStorage.saveHomeBanners(context, mutable)
                                    coroutineScope.launch {
                                        FirestoreRepository(context).saveHomeBannersToCloud(mutable)
                                    }
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color(0xFFA0A5B0), modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(
                            onClick = {
                                editingBanner = banner
                                isNewBanner = false
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFFF7A00), modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                val updated = bannersList.filter { it.id != banner.id }
                                bannersList = updated
                                AdminStorage.saveHomeBanners(context, updated)
                                coroutineScope.launch {
                                    FirestoreRepository(context).saveHomeBannersToCloud(updated)
                                }
                                Toast.makeText(context, "ბანერი წაიშალა!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D4F), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // მოქმედების ღილაკები: შენახვა & საწყისზე დაბრუნება
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val reset = AdminStorage.resetHomeBanners(context)
                        bannersList = reset
                        coroutineScope.launch {
                            FirestoreRepository(context).saveHomeBannersToCloud(reset)
                        }
                        Toast.makeText(context, "Home Screen ბანერები დაბრუნდა საწყის პარამეტრებზე!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA0A5B0)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFA0A5B0), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("საწყისი", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        AdminStorage.saveHomeBanners(context, bannersList)
                        coroutineScope.launch {
                            val res = FirestoreRepository(context).saveHomeBannersToCloud(bannersList)
                            if (res.isSuccess) {
                                Toast.makeText(context, "☁️ ბანერები წარმატებით შეინახა და აიტვირთა Cloud-ში!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "✅ Home Screen ბანერები შეინახა ლოკალურად!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f).height(44.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("შენახვა", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ბანერის რედაქტირების / დამატების მოდალური დიალოგი
    if (editingBanner != null) {
        BannerEditDialog(
            banner = editingBanner!!,
            isNew = isNewBanner,
            products = products,
            onDismiss = { editingBanner = null },
            onSave = { savedBanner ->
                val updated = if (isNewBanner) {
                    bannersList + savedBanner
                } else {
                    bannersList.map { if (it.id == savedBanner.id) savedBanner else it }
                }
                bannersList = updated
                AdminStorage.saveHomeBanners(context, updated)
                coroutineScope.launch {
                    FirestoreRepository(context).saveHomeBannersToCloud(updated)
                }
                editingBanner = null
                Toast.makeText(context, "ბანერი შენახულია!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ==============================================================================
// ბანერის რედაქტირების დიალოგი (სათაური, ფასი, თეგი, სურათი, პროდუქტის ბმული)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerEditDialog(
    banner: AdminStorage.HomeBannerItem,
    isNew: Boolean,
    products: List<ProductItem>,
    onDismiss: () -> Unit,
    onSave: (AdminStorage.HomeBannerItem) -> Unit
) {
    var title by remember { mutableStateOf(banner.title) }
    var subtitle by remember { mutableStateOf(banner.subtitle) }
    var tag by remember { mutableStateOf(banner.tag) }
    var imageUrl by remember { mutableStateOf(banner.imageUrl) }
    var selectedProductId by remember { mutableIntStateOf(banner.targetProductId) }
    var showProductPicker by remember { mutableStateOf(false) }

    val presetImages = listOf(
        "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800",
        "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=800",
        "https://images.unsplash.com/photo-1581092160607-ee22621dd758?q=80&w=800&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=800"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF16191E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6A00).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isNew) "➕ ახალი ბანერის დამატება" else "✏️ ბანერის რედაქტირება",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // სათაური
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ბანერის სათაური") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ქვესათაური / ფასი
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("ქვესათაური (მახასიათებლები / ფასი)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // თეგი / ბეიჯი
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("თეგი (მაგ: 🔥 სპეც შეთავაზება)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // დაკავშირებული რეალური პროდუქტი
                val currentLinkedProduct = products.find { it.id == selectedProductId }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("დაკავშირებული რეალური პროდუქტი:", color = Color(0xFFA0A5B0), fontSize = 11.sp)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F1216),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6A00).copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProductPicker = !showProductPicker }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentLinkedProduct?.title ?: "აირჩიეთ პროდუქტი",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ფასი: ${currentLinkedProduct?.price ?: "-"} | კატეგორია: ${currentLinkedProduct?.category ?: "-"}",
                                    color = Color(0xFFFF9E40),
                                    fontSize = 10.5.sp
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFFFF7A00))
                        }
                    }

                    // პროდუქტების სწრაფი არჩევანი
                    if (showProductPicker) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF14171E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3440)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            LazyColumn(modifier = Modifier.padding(4.dp)) {
                                items(products) { prod ->
                                    val isPicked = prod.id == selectedProductId
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isPicked) Color(0xFFFF6A00).copy(alpha = 0.2f) else Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedProductId = prod.id
                                                if (title.isBlank()) title = prod.title
                                                if (subtitle.isBlank()) subtitle = "${prod.category} • ${prod.price}"
                                                showProductPicker = false
                                            }
                                            .padding(vertical = 4.dp, horizontal = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(prod.title, color = Color.White, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(prod.price, color = Color(0xFFFF7A00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // სურათის URL
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("სურათის URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = Color(0xFF2E3440)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // სწრაფი სურათის პრესეტები
                Text("სწრაფი ფოტო პრესეტები:", color = Color(0xFFA0A5B0), fontSize = 10.5.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presetImages) { url ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { imageUrl = url },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (imageUrl == url) Color(0xFFFF6A00) else Color(0xFF2E3440)
                            )
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ღილაკები
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("გაუქმება", color = Color(0xFFA0A5B0))
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                title = "სპეც შეთავაზება"
                            }
                            onSave(
                                banner.copy(
                                    title = title.trim(),
                                    subtitle = subtitle.trim(),
                                    tag = tag.trim().ifBlank { "შეთავაზება" },
                                    imageUrl = imageUrl.trim(),
                                    targetProductId = selectedProductId
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        shape = RoundedCornerShape(10.dp),
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
// 6.5. კატეგორიების სრული დინამიური მართვის ეკრანი (Categories Management)
// ==============================================================================
@Composable
fun AdminCategoriesManagementView(
    products: List<ProductItem>,
    onProductsUpdated: (List<ProductItem>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppPreferencesManager.isDarkMode

    var categories by remember { mutableStateOf(AdminStorage.getCategories(context)) }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var isAddingNewCategory by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryItem?>(null) }
    var reassignTargetCategoryName by remember { mutableStateOf("") }
    var previewCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var categorySearch by remember { mutableStateOf("") }

    // Live listening to Firestore categories if available
    DisposableEffect(Unit) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            val repo = FirestoreRepository(context)
            registration = repo.listenToCategories(
                onCategoriesUpdated = { cloudList ->
                    if (cloudList.isNotEmpty()) {
                        AdminStorage.saveCategories(context, cloudList)
                        categories = cloudList.sortedBy { it.displayOrder }
                    }
                }
            )
        } catch (_: Exception) {}
        onDispose { registration?.remove() }
    }

    val filteredCategories = remember(categories, categorySearch) {
        if (categorySearch.isBlank()) categories
        else categories.filter {
            it.name.contains(categorySearch, ignoreCase = true) ||
            it.localizedName.contains(categorySearch, ignoreCase = true) ||
            it.categoryType.contains(categorySearch, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ჰედერი & ახალი კატეგორიის დამატების ღილაკი
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📁 კატეგორიების მართვა",
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "სულ: ${categories.size} კატეგორია • ${categories.count { it.isPopular }} პოპულარული",
                    color = Color(0xFFFF7A00),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = { isAddingNewCategory = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("დამატება", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ძებნის ველი
        OutlinedTextField(
            value = categorySearch,
            onValueChange = { categorySearch = it },
            placeholder = { Text("ძებნა კატეგორიებში...", fontSize = 12.sp, color = Color(0xFF888E9B)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (categorySearch.isNotBlank()) {
                    IconButton(onClick = { categorySearch = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                focusedBorderColor = Color(0xFFFF6A00),
                unfocusedBorderColor = if (isDark) Color(0xFF282F3C) else Color(0xFFCBD5E1),
                focusedContainerColor = if (isDark) Color(0xFF161922) else Color(0xFFF8FAFC),
                unfocusedContainerColor = if (isDark) Color(0xFF161922) else Color(0xFFF8FAFC)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(filteredCategories, key = { it.id }) { cat ->
                val productCount = remember(products, cat.name) {
                    products.count { it.category.equals(cat.name, ignoreCase = true) }
                }
                val currentIndex = categories.indexOfFirst { it.id == cat.id }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF161A22) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (cat.isPopular) Color(0xFFFF7A00).copy(alpha = 0.6f)
                        else if (isDark) Color(0xFF252B37) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
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
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0xFF222834) else Color(0xFFFFF7ED),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.3f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(cat.icon.ifBlank { "📦" }, fontSize = 20.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = cat.localizedName.ifBlank { cat.name },
                                            color = if (isDark) Color.White else Color(0xFF0F172A),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (cat.isPopular) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFF6A00).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "⭐ პოპულარული",
                                                    color = Color(0xFFFF6A00),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ID: ${cat.name}",
                                            color = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                            fontSize = 11.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isDark) Color(0xFF1E2430) else Color(0xFFF1F5F9)
                                        ) {
                                            Text(
                                                text = "$productCount პროდუქტი",
                                                color = Color(0xFFFF7A00),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // აქტიური ჩამრთველი
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = cat.isActive,
                                    onCheckedChange = { active ->
                                        val updated = cat.copy(isActive = active)
                                        AdminStorage.updateCategory(context, updated)
                                        categories = AdminStorage.getCategories(context)
                                        coroutineScope.launch {
                                            FirestoreRepository(context).saveCategory(updated)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFF6A00)
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }

                        if (cat.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cat.description,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = if (isDark) Color(0xFF252B37) else Color(0xFFF1F5F9), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // მოქმედებების ზოლი: Reorder Up/Down, Popular Toggle, Preview, Edit, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reorder buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        if (currentIndex > 0) {
                                            val list = categories.toMutableList()
                                            val prev = list[currentIndex - 1]
                                            val cur = list[currentIndex]
                                            list[currentIndex - 1] = cur.copy(displayOrder = prev.displayOrder)
                                            list[currentIndex] = prev.copy(displayOrder = cur.displayOrder)
                                            AdminStorage.saveCategories(context, list)
                                            categories = AdminStorage.getCategories(context)
                                            coroutineScope.launch {
                                                FirestoreRepository(context).saveCategory(list[currentIndex - 1])
                                                FirestoreRepository(context).saveCategory(list[currentIndex])
                                            }
                                        }
                                    },
                                    enabled = currentIndex > 0,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Move Up",
                                        tint = if (currentIndex > 0) (if (isDark) Color.White else Color(0xFF1E293B)) else Color.Gray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (currentIndex < categories.size - 1) {
                                            val list = categories.toMutableList()
                                            val next = list[currentIndex + 1]
                                            val cur = list[currentIndex]
                                            list[currentIndex + 1] = cur.copy(displayOrder = next.displayOrder)
                                            list[currentIndex] = next.copy(displayOrder = cur.displayOrder)
                                            AdminStorage.saveCategories(context, list)
                                            categories = AdminStorage.getCategories(context)
                                            coroutineScope.launch {
                                                FirestoreRepository(context).saveCategory(list[currentIndex + 1])
                                                FirestoreRepository(context).saveCategory(list[currentIndex])
                                            }
                                        }
                                    },
                                    enabled = currentIndex < categories.size - 1,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        tint = if (currentIndex < categories.size - 1) (if (isDark) Color.White else Color(0xFF1E293B)) else Color.Gray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Popular Star Button
                                IconButton(
                                    onClick = {
                                        val updated = cat.copy(isPopular = !cat.isPopular)
                                        AdminStorage.updateCategory(context, updated)
                                        categories = AdminStorage.getCategories(context)
                                        coroutineScope.launch {
                                            FirestoreRepository(context).saveCategory(updated)
                                        }
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        if (cat.isPopular) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Toggle Popular",
                                        tint = if (cat.isPopular) Color(0xFFFFB800) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Preview live catalog chip
                                IconButton(
                                    onClick = { previewCategory = cat },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = "Live Preview",
                                        tint = Color(0xFFFF7A00),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Edit & Delete
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { editingCategory = cat },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFFF7A00), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        categoryToDelete = cat
                                        reassignTargetCategoryName = categories.firstOrNull { it.id != cat.id }?.name ?: "სხვა"
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D4F), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // კატეგორიის დამატების ან რედაქტირების დიალოგი
    if (isAddingNewCategory || editingCategory != null) {
        val target = editingCategory
        val isNew = isAddingNewCategory
        CategoryEditModal(
            category = target ?: CategoryItem(
                id = "cat_${System.currentTimeMillis()}",
                name = "",
                localizedName = "",
                icon = "📦",
                categoryType = "general",
                displayOrder = categories.size + 1,
                isActive = true,
                isPopular = false
            ),
            isNew = isNew,
            onDismiss = {
                isAddingNewCategory = false
                editingCategory = null
            },
            onSave = { saved ->
                if (isNew) {
                    AdminStorage.addCategory(context, saved)
                } else {
                    AdminStorage.updateCategory(context, saved)
                }
                categories = AdminStorage.getCategories(context)
                coroutineScope.launch {
                    FirestoreRepository(context).saveCategory(saved)
                }
                Toast.makeText(context, "✅ კატეგორია წარმატებით შეინახა!", Toast.LENGTH_SHORT).show()
                isAddingNewCategory = false
                editingCategory = null
            }
        )
    }

    // უსაფრთხო წაშლის დიალოგი (Safe Deletion with Reassignment)
    if (categoryToDelete != null) {
        val toDelete = categoryToDelete!!
        val affectedCount = remember(products, toDelete.name) {
            products.count { it.category.equals(toDelete.name, ignoreCase = true) }
        }
        val destinationOptions = remember(categories, toDelete.id) {
            categories.filter { it.id != toDelete.id }.map { it.name }
        }

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text(
                    text = "კატეგორიის წაშლა",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ნამდვილად გსურთ წაშალოთ კატეგორია „${toDelete.localizedName.ifBlank { toDelete.name }}“?",
                        color = Color(0xFFA0A5B0),
                        fontSize = 13.sp
                    )

                    if (affectedCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF331616),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "⚠️ ყურადღება: ამ კატეგორიაში არის $affectedCount პროდუქტი!",
                                    color = Color(0xFFFF4D4F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "წაშლამდე მიუთითეთ, რომელ კატეგორიაში გადავიდეს ეს პროდუქტები:",
                                    color = Color.White,
                                    fontSize = 11.5.sp
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(destinationOptions) { dest ->
                                        val isSelected = reassignTargetCategoryName == dest
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF222834),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFFFF6A00) else Color(0xFF3B4455)),
                                            modifier = Modifier.clickable { reassignTargetCategoryName = dest }
                                        ) {
                                            Text(
                                                text = dest,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color(0xFF16191E),
            confirmButton = {
                Button(
                    onClick = {
                        val destination = if (affectedCount > 0) reassignTargetCategoryName.ifBlank { "სხვა" } else null
                        val success = AdminStorage.deleteCategory(context, toDelete.id, destination)
                        if (success) {
                            categories = AdminStorage.getCategories(context)
                            val updatedProducts = AdminStorage.loadProducts(context)
                            onProductsUpdated(updatedProducts)
                            coroutineScope.launch {
                                FirestoreRepository(context).deleteCategory(toDelete.id)
                            }
                            Toast.makeText(
                                context,
                                if (affectedCount > 0) "✅ კატეგორია წაიშალა და $affectedCount პროდუქტი გადავიდა: $destination"
                                else "✅ კატეგორია წაიშალა!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F))
                ) {
                    Text("წაშლა", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("გაუქმება", color = Color(0xFFA0A5B0))
                }
            }
        )
    }

    // კატალოგის ცოცხალი გადახედვის მოდალი
    if (previewCategory != null) {
        val cat = previewCategory!!
        Dialog(
            onDismissRequest = { previewCategory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF161922),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00)),
                modifier = Modifier.fillMaxWidth(0.92f).padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👁️ კატალოგის ცოცხალი გადახედვა", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { previewCategory = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Text("როგორ გამოიყურება ეს კატეგორია მომხმარებლისთვის:", color = Color(0xFFA0A5B0), fontSize = 12.sp)

                    // 1) Catalog Chip Preview
                    Text("1. კატალოგის ფილტრის ჩიპი (Filter Chip):", color = Color(0xFFFF9E40), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Unselected Chip
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E2430),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333B4D))
                        ) {
                            Text(
                                text = "${cat.icon} ${cat.localizedName.ifBlank { cat.name }}",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }

                        // Selected Chip
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFF6A00)
                        ) {
                            Text(
                                text = "${cat.icon} ${cat.localizedName.ifBlank { cat.name }}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    // 2) Popular Grid Card Preview
                    Text("2. პოპულარული კატეგორიის ქარდი (Home/Catalog Grid):", color = Color(0xFFFF9E40), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1F2430),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.5f)),
                        modifier = Modifier.width(160.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(cat.icon.ifBlank { "📦" }, fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = cat.localizedName.ifBlank { cat.name },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${products.count { it.category.equals(cat.name, true) }} პროდუქტი",
                                color = Color(0xFFFF7A00),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Button(
                        onClick = { previewCategory = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("დახურვა", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * კატეგორიის შექმნისა და რედაქტირების მოდალური ფანჯარა
 */
@Composable
fun CategoryEditModal(
    category: CategoryItem,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (CategoryItem) -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode

    var name by remember { mutableStateOf(category.name) }
    var localizedName by remember { mutableStateOf(category.localizedName) }
    var icon by remember { mutableStateOf(category.icon.ifBlank { "📦" }) }
    var description by remember { mutableStateOf(category.description) }
    var categoryType by remember { mutableStateOf(category.categoryType.ifBlank { "general" }) }
    var isActive by remember { mutableStateOf(category.isActive) }
    var isPopular by remember { mutableStateOf(category.isPopular) }

    val presetIcons = listOf("📦", "📶", "📱", "💻", "🎧", "⚡", "⚙️", "🔋", "📸", "🔊", "🛡️", "🌐")
    val types = listOf(
        "general" to "ზოგადი",
        "networking" to "ქსელური მოწყობილობები",
        "phones" to "მობილური ტელეფონები",
        "accessories" to "აქსესუარები",
        "parts" to "სათადარიგო ნაწილები",
        "software" to "პროგრამული სერვისები"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF161922) else Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(8.dp)
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
                    Text(
                        text = if (isNew) "➕ ახალი კატეგორია" else "✏️ კატეგორიის რედაქტირება",
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                // სისტემური სახელი
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("სისტემური სახელი (Category Key)") },
                    placeholder = { Text("მაგ: ქსელური მოწყობილობები, ეკრანები...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = if (isDark) Color(0xFF282F3C) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ქართული სახელი
                OutlinedTextField(
                    value = localizedName,
                    onValueChange = { localizedName = it },
                    label = { Text("ქართული სახელწოდება (Localized Title)") },
                    placeholder = { Text("მაგ: ქსელური მოწყობილობები (როუტერები)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = if (isDark) Color(0xFF282F3C) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ხატულა (Icon / Emoji)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ხატულა (Emoji ან სიმბოლო):", color = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B), fontSize = 12.sp)
                        Text(icon, fontSize = 24.sp)
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presetIcons) { ic ->
                            val isSel = icon == ic
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFFFF6A00) else (if (isDark) Color(0xFF222834) else Color(0xFFF1F5F9)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFF6A00) else Color.Transparent),
                                modifier = Modifier.clickable { icon = ic }
                            ) {
                                Text(ic, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }

                // კატეგორიის ტიპი
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("კატეგორიის ტიპი:", color = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B), fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(types) { (key, label) ->
                            val isSel = categoryType == key
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFFFF6A00) else (if (isDark) Color(0xFF222834) else Color(0xFFF1F5F9)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFFFF6A00) else Color.Transparent),
                                modifier = Modifier.clickable { categoryType = key }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) Color.White else (if (isDark) Color(0xFFA0A5B0) else Color(0xFF1E293B)),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // აღწერა
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("მოკლე აღწერა (არასავალდებულო)") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        unfocusedTextColor = if (isDark) Color.White else Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFFF6A00),
                        unfocusedBorderColor = if (isDark) Color(0xFF282F3C) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ჩამრთველები
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("აქტიურია კატალოგში", color = if (isDark) Color.White else Color(0xFF0F172A), fontSize = 13.sp)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF6A00))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("პოპულარული კატეგორია (Home/Grid)", color = Color(0xFFFF9E40), fontSize = 13.sp)
                    Switch(
                        checked = isPopular,
                        onCheckedChange = { isPopular = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF6A00))
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("გაუქმება")
                    }

                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            if (cleanName.isBlank()) return@Button
                            val saved = category.copy(
                                name = cleanName,
                                localizedName = localizedName.trim().ifBlank { cleanName },
                                icon = icon.trim().ifBlank { "📦" },
                                description = description.trim(),
                                categoryType = categoryType,
                                isActive = isActive,
                                isPopular = isPopular,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(saved)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                        shape = RoundedCornerShape(10.dp),
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
// 7. შეკვეთების მართვის ეკრანი (Orders Management - Cloud Firestore & Local)
// ==============================================================================
@Composable
fun AdminOrdersManagementView() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var ordersList by remember { mutableStateOf<List<OrderRecord>>(emptyList()) }
    var selectedStatusFilter by remember { mutableStateOf("ყველა") }
    var selectedOrderForDetail by remember { mutableStateOf<OrderRecord?>(null) }

    // რეალურ დროში ყველა შეკვეთის მოსმენა Firestore-დან
    DisposableEffect(Unit) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            val repo = FirestoreRepository(context)
            registration = repo.listenToAllOrdersForAdmin(
                onOrdersUpdated = { cloudFirestoreOrders ->
                    ordersList = cloudFirestoreOrders.map { it.toOrderRecord() }
                }
            )
        } catch (_: Exception) {}
        onDispose {
            registration?.remove()
        }
    }

    val statusFilters = remember { listOf("ყველა", "მიმდინარე", "დამუშავების პროცესში", "შესრულებული", "გაუქმებული") }
    val filteredOrders = remember(ordersList, selectedStatusFilter) {
        ordersList.filter { ord ->
            when (selectedStatusFilter) {
                "ყველა" -> true
                "მიმდინარე" -> ord.status == "მიმდინარე" || ord.status == "ახალი"
                else -> ord.status == selectedStatusFilter
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "orders_header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val isDark = AppPreferencesManager.isDarkMode
                // ჰედერი
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF16191E) else Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF282D37) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "📦 შეკვეთების მართვა",
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "სულ შეკვეთები: ${ordersList.size} | ნაჩვენებია: ${filteredOrders.size}",
                                color = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B),
                                fontSize = 11.5.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                Toast.makeText(context, "შეკვეთების სია სინქრონიზებულია Firestore-თან", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFFF7A00))
                        }
                    }
                }

                // სტატუსის ფილტრები
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusFilters) { status ->
                        val isSelected = selectedStatusFilter == status
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFFF6A00) else if (isDark) Color(0xFF1C2026) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFFF7A00) else if (isDark) Color(0xFF2E3440) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable { selectedStatusFilter = status }
                        ) {
                            CompositionLocalProvider(LocalPreserveWhiteText provides isSelected) {
                                Text(
                                    text = status,
                                    color = if (isSelected) Color.White else if (isDark) Color(0xFFA0A5B0) else Color(0xFF475569),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            item(key = "orders_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("შეკვეთები ვერ მოიძებნა", color = Color(0xFF6E7582), fontSize = 13.sp)
                }
            }
        } else {
            items(filteredOrders, key = { it.id }) { order ->
                    val summaryText = order.items.joinToString(", ") { "${it.productTitle} x${it.quantity}" }
                        .ifBlank { "სერვისის შეკვეთა" }
                    val totalCount = order.items.sumOf { it.quantity }.coerceAtLeast(1)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF16191E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D37)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOrderForDetail = order }
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
                                Text(
                                    text = order.orderNumber.ifBlank { order.id },
                                    color = Color(0xFFFF7A00),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = order.date,
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Text(
                                text = summaryText,
                                color = Color.White,
                                fontSize = 12.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ჯამი: ${order.totalAmount} ($totalCount ერთ.)",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (order.status) {
                                            "შესრულებული" -> Color(0x334ADE80)
                                            "გაუქმებული" -> Color(0x33FF4D4F)
                                            else -> Color(0x33FF7A00)
                                        }
                                    ) {
                                        Text(
                                            text = order.status,
                                            color = when (order.status) {
                                                "შესრულებული" -> Color(0xFF4ADE80)
                                                "გაუქმებული" -> Color(0xFFFF4D4F)
                                                else -> Color(0xFFFF9E40)
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0x22FF4D4F),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x55FF4D4F)),
                                        modifier = Modifier.clickable {
                                            coroutineScope.launch {
                                                val delRes = FirestoreRepository(context).deleteOrderFromCloud(order.id)
                                                AuthAndOrderManager.deleteOrder(context, order.id)
                                                if (delRes.isSuccess) {
                                                    ordersList = ordersList.filterNot { it.id == order.id }
                                                    Toast.makeText(context, "🗑️ შეკვეთა წაიშალა!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val errMsg = delRes.exceptionOrNull()?.localizedMessage ?: "შეკვეთის წაშლა ვერ მოხერხდა"
                                                    Toast.makeText(context, "❌ $errMsg", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "შეკვეთის წაშლა",
                                                tint = Color(0xFFFF4D4F),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "წაშლა",
                                                color = Color(0xFFFF4D4F),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            // სწრაფი სტატუსის შეცვლის ღილაკები ადმინისთვის
                            val statusActions = listOf(
                                Triple("pending", "მიმდინარე", "ახალი"),
                                Triple("in_progress", "დამუშავების პროცესში", "მუშავდება"),
                                Triple("completed", "შესრულებული", "დასრულებული"),
                                Triple("cancelled", "გაუქმებული", "გაუქმება")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                statusActions.forEach { (cloudStatus, uiStatusValue, label) ->
                                    val isCurrent = order.status == uiStatusValue
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) Color(0xFFFF6A00) else Color(0xFF222630),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                coroutineScope.launch {
                                                    val cloudRes = FirestoreRepository(context).updateOrderStatusAsAdmin(order.id, cloudStatus)
                                                    if (cloudRes.isSuccess) {
                                                        AuthAndOrderManager.updateOrderStatus(context, order.id, uiStatusValue)
                                                        ordersList = ordersList.map {
                                                            if (it.id == order.id) it.copy(status = uiStatusValue) else it
                                                        }
                                                        Toast.makeText(context, "☁️ სტატუსი განახლდა: $uiStatusValue", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val errMsg = cloudRes.exceptionOrNull()?.localizedMessage ?: "სტატუსის განახლება ვერ მოხერხდა"
                                                        Toast.makeText(context, "❌ $errMsg", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp, horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    if (selectedOrderForDetail != null) {
        val ord = selectedOrderForDetail!!
        AlertDialog(
            onDismissRequest = { selectedOrderForDetail = null },
            containerColor = Color(0xFF16191E),
            title = {
                Text(
                    text = "შეკვეთა ${ord.orderNumber.ifBlank { ord.id }}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("თარიღი: ${ord.date}", color = Color(0xFFA0A5B0), fontSize = 12.sp)
                    Text("სტატუსი: ${ord.status}", color = Color(0xFFFF7A00), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Text("გადახდა: ${ord.paymentMethod}", color = Color.White, fontSize = 12.sp)
                    Text("თრექინგ კოდი: ${ord.trackingCode}", color = Color(0xFF60A5FA), fontSize = 12.sp)
                    HorizontalDivider(color = Color(0xFF282D37), modifier = Modifier.padding(vertical = 4.dp))
                    Text("პროდუქტები (Snapshot):", color = Color(0xFFFF9E40), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    ord.items.forEach { itemSnap ->
                        Text(
                            text = "• ${itemSnap.productTitle} (${itemSnap.quality}) x${itemSnap.quantity} — ${itemSnap.price}",
                            color = Color.White,
                            fontSize = 11.5.sp
                        )
                    }
                    HorizontalDivider(color = Color(0xFF282D37), modifier = Modifier.padding(vertical = 4.dp))
                    Text("ჯამური თანხა: ${ord.totalAmount}", color = Color(0xFF4ADE80), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val targetId = ord.id
                        selectedOrderForDetail = null
                        coroutineScope.launch {
                            val delRes = FirestoreRepository(context).deleteOrderFromCloud(targetId)
                            AuthAndOrderManager.deleteOrder(context, targetId)
                            if (delRes.isSuccess) {
                                ordersList = ordersList.filterNot { it.id == targetId }
                                Toast.makeText(context, "🗑️ შეკვეთა წაიშალა!", Toast.LENGTH_SHORT).show()
                            } else {
                                val errMsg = delRes.exceptionOrNull()?.localizedMessage ?: "შეკვეთის წაშლა ვერ მოხერხდა"
                                Toast.makeText(context, "❌ $errMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("წაშლა", color = Color(0xFFFF4D4F), fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedOrderForDetail = null }) {
                    Text("დახურვა", color = Color(0xFFFF7A00), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ==============================================================================
// 8. მომხმარებლების მართვის ეკრანი (Users Management - Cloud Firestore)
// ==============================================================================
@Composable
fun AdminUsersManagementView() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var usersList by remember { mutableStateOf<List<FirestoreUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun buildFallbackUsers(): List<FirestoreUser> {
        val localUser = AuthAndOrderManager.getCurrentUser(context) ?: return emptyList()
        return listOf(
            FirestoreUser(
                id = localUser.id,
                userId = localUser.id,
                displayName = localUser.displayName,
                email = localUser.email,
                phoneNumber = localUser.phone,
                photoUrl = localUser.avatarUrl,
                address = localUser.address,
                deviceModel = localUser.deviceModel
            )
        )
    }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val res = FirestoreRepository(context).getAllUsersForAdmin()
            val cloudUsers = res.getOrDefault(emptyList())
            usersList = if (cloudUsers.isNotEmpty()) cloudUsers else buildFallbackUsers()
        } catch (_: Exception) {
            usersList = buildFallbackUsers()
        } finally {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "users_header") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D37)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("👥 რეგისტრირებული მომხმარებლები", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("Cloud Firestore users კოლექცია (${usersList.size})", color = Color(0xFFA0A5B0), fontSize = 11.5.sp)
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val cloudUsers = FirestoreRepository(context).getAllUsersForAdmin().getOrDefault(emptyList())
                                if (cloudUsers.isNotEmpty()) usersList = cloudUsers
                                isLoading = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFFF7A00))
                    }
                }
            }
        }

        if (isLoading) {
            item(key = "users_loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF6A00))
                }
            }
        } else if (usersList.isEmpty()) {
            item(key = "users_empty") {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("მომხმარებლები ვერ მოიძებნა", color = Color(0xFF6E7582), fontSize = 13.sp)
                }
            }
        } else {
            items(usersList, key = { it.userId.ifBlank { it.id } }) { u ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF16191E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D37)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = u.displayName.ifBlank { "მომხმარებელი" },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFF6A00).copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "Google / Cloud",
                                    color = Color(0xFFFF7A00),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("📧 ${u.email}", color = Color(0xFFA0A5B0), fontSize = 12.sp)
                        if (u.phoneNumber.isNotBlank()) {
                            Text("📞 ${u.phoneNumber}", color = Color(0xFFA0A5B0), fontSize = 12.sp)
                        }
                        if (u.address.isNotBlank()) {
                            Text("📍 ${u.address}", color = Color(0xFFA0A5B0), fontSize = 11.5.sp)
                        }
                        if (u.deviceModel.isNotBlank()) {
                            Text("📱 მოწყობილობა: ${u.deviceModel}", color = Color(0xFF6E7582), fontSize = 10.5.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 11. Push შეტყობინებების მართვა და ტესტირება (მხოლოდ ვერიფიცირებული ადმინისთვის)
// ==============================================================================
@Composable
fun AdminPushNotificationsView(
    products: List<ProductItem>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember { FirestoreRepository(context) }

    var editingNotificationId by remember { mutableStateOf<String?>(null) }
    var titleInput by remember { mutableStateOf("BEKASERVICE • ახალი შეთავაზება") }
    var bodyInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("announcement") }
    var selectedProductId by remember { mutableIntStateOf(0) }
    var selectedTargetTab by remember { mutableIntStateOf(0) }
    var productSearchFilter by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var recentNotifications by remember {
        mutableStateOf(BekaNotificationManager.loadLocalNotificationHistory(context))
    }
    var fcmToken by remember { mutableStateOf(BekaNotificationManager.getLocalFcmToken(context)) }
    var firebaseUserEmail by remember {
        mutableStateOf(
            AuthAndOrderManager.resolveActiveUserEmail(context).ifBlank {
                if (AdminStorage.isAdminSessionActive(context)) AuthAndOrderManager.SOLE_ADMIN_EMAIL else null
            }
        )
    }
    val notificationsGranted = BekaNotificationManager.hasNotificationPermission(context)

    DisposableEffect(Unit) {
        val auth = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance() }.getOrNull()
        val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { fbAuth ->
            firebaseUserEmail = fbAuth.currentUser?.email ?: AuthAndOrderManager.resolveActiveUserEmail(context).ifBlank {
                if (AdminStorage.isAdminSessionActive(context)) AuthAndOrderManager.SOLE_ADMIN_EMAIL else null
            }
        }
        auth?.addAuthStateListener(authListener)

        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            registration = repo.listenToBroadcastNotifications { list ->
                recentNotifications = BekaNotificationManager.syncCloudNotificationsToLocal(context, list)
            }
        } catch (_: Exception) {}
        onDispose {
            auth?.removeAuthStateListener(authListener)
            registration?.remove()
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { AuthAndOrderManager.ensureAdminFirebaseSession(context) }
        }
        firebaseUserEmail = AuthAndOrderManager.resolveActiveUserEmail(context).ifBlank {
            if (AdminStorage.isAdminSessionActive(context)) AuthAndOrderManager.SOLE_ADMIN_EMAIL else null
        }
        val synced = BekaNotificationManager.syncFcmTokenAndSubscribeTopic(context, repo)
        if (!synced.isNullOrBlank()) {
            fcmToken = synced
        }
    }

    val notificationTypes = listOf(
        Pair("announcement", "📢 ზოგადი განცხადება"),
        Pair("product_offer", "🔥 პროდუქტის აქცია"),
        Pair("service_update", "🛠️ სერვისის სიახლე")
    )

    val targetPages = listOf(
        Pair(0, "🏠 მთავარი გვერდი"),
        Pair(1, "📦 კატალოგი"),
        Pair(2, "🛠️ დიაგნოსტიკა"),
        Pair(3, "🤖 AI ასისტენტი"),
        Pair(4, "👤 პროფილი"),
        Pair(5, "🛒 კალათა"),
        Pair(6, "🔔 შეტყობინებები"),
        Pair(7, "📲 განახლება")
    )

    val filteredProductsForPicker = remember(products, productSearchFilter) {
        if (productSearchFilter.isBlank()) {
            products.take(30)
        } else {
            val q = productSearchFilter.trim().lowercase()
            products.filter {
                it.title.lowercase().contains(q) ||
                    it.brand.lowercase().contains(q) ||
                    it.category.lowercase().contains(q)
            }.take(30)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .imePadding(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "push_status_card") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
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
                        Text(
                            text = "🔔 Push შეტყობინებების ცენტრი",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (notificationsGranted) Color(0x224ADE80) else Color(0x22F87171)
                        ) {
                            Text(
                                text = if (notificationsGranted) "ნებართვა ჩართულია ✅" else "ნებართვა გამორთულია ⚠️",
                                color = if (notificationsGranted) Color(0xFF4ADE80) else Color(0xFFF87171),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = if (firebaseUserEmail.equals("beqa1994@gmail.com", ignoreCase = true)) {
                            "Cloud ადმინი ვერიფიცირებულია: $firebaseUserEmail ✅"
                        } else {
                            "Cloud გაგზავნა: საჭიროა Google ავტორიზაცია (beqa1994@gmail.com)"
                        },
                        color = if (firebaseUserEmail.equals("beqa1994@gmail.com", ignoreCase = true)) Color(0xFF4ADE80) else Color(0xFFA0A5B0),
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!firebaseUserEmail.equals("beqa1994@gmail.com", ignoreCase = true)) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val signedIn = AuthAndOrderManager.signInWithGoogleCredentialManager(
                                        activityContext = context,
                                        onError = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                    if (signedIn != null) {
                                        firebaseUserEmail = signedIn.email
                                        Toast.makeText(context, "✅ Google ავტორიზაცია შესრულდა: ${signedIn.email}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Google-ით შესვლა (beqa1994@gmail.com)",
                                color = Color(0xFFFF7A00),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (fcmToken.isNotBlank()) "FCM Token: ${fcmToken.take(18)}...${fcmToken.takeLast(8)}" else "FCM Token: მოწმდება...",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (fcmToken.isNotBlank()) {
                            Text(
                                text = "კოპირება",
                                color = Color(0xFFFF7A00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    copyToClipboard(context, "FCM Token", fcmToken)
                                }
                            )
                        }
                    }
                }
            }
        }

        item(key = "push_compose_card") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (editingNotificationId != null) 1.5.dp else 1.dp,
                    color = if (editingNotificationId != null) Color(0xFFFF7A00) else Color(0xFF282D37)
                ),
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
                        Text(
                            text = if (editingNotificationId != null) "✏️ შეტყობინების რედაქტირება" else "ახალი შეტყობინების გაგზავნა",
                            color = if (editingNotificationId != null) Color(0xFFFF7A00) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (editingNotificationId != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF262A34),
                                modifier = Modifier.clickable {
                                    editingNotificationId = null
                                    titleInput = "BEKASERVICE • ახალი შეთავაზება"
                                    bodyInput = ""
                                    selectedProductId = 0
                                    selectedTargetTab = 0
                                    selectedType = "announcement"
                                }
                            ) {
                                Text(
                                    text = "გაუქმება",
                                    color = Color(0xFFFF4D4F),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(notificationTypes) { (typeKey, label) ->
                            val isSelected = selectedType == typeKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF1E222B),
                                modifier = Modifier.clickable { selectedType = typeKey }
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("სათაური (Title)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF2E3440)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bodyInput,
                        onValueChange = { bodyInput = it },
                        label = { Text("შეტყობინების ტექსტი") },
                        placeholder = { Text("მაგ: ChatGPT Plus და Google AI Pro 18 თვე სპეციალურ ფასად!") },
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF2E3440)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 1. გადასასვლელი გვერდის არჩევა
                    Text(
                        text = "გადასასვლელი გვერდი დაკლიკებისას:",
                        color = Color(0xFFA0A5B0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(targetPages, key = { it.first }) { (tabIdx, tabLabel) ->
                            val isSelected = selectedTargetTab == tabIdx
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF1E222B),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFFF7A00) else Color(0xFF282D37)
                                ),
                                modifier = Modifier.clickable {
                                    selectedTargetTab = tabIdx
                                }
                            ) {
                                Text(
                                    text = tabLabel,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // 2. კონკრეტული პროდუქტის ან სერვისის მიბმა (სურვილისამებრ)
                    Text(
                        text = "კონკრეტული პროდუქტი / სერვისი (სურვილისამებრ):",
                        color = Color(0xFFA0A5B0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = productSearchFilter,
                        onValueChange = { productSearchFilter = it },
                        placeholder = { Text("პროდუქტის სწრაფი ძებნა...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (productSearchFilter.isNotEmpty()) {
                                IconButton(onClick = { productSearchFilter = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFA0A5B0), modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF2E3440)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item(key = "prod_none") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedProductId == 0) Color(0xFFFF6A00) else Color(0xFF1E222B),
                                modifier = Modifier.clickable { selectedProductId = 0 }
                            ) {
                                Text(
                                    text = "არცერთი (მხოლოდ გვერდზე გადასვლა)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(filteredProductsForPicker, key = { it.id }) { prod ->
                            val isSelected = selectedProductId == prod.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFF6A00) else Color(0xFF1E222B),
                                modifier = Modifier.clickable {
                                    selectedProductId = prod.id
                                    if (selectedTargetTab == 0) {
                                        selectedTargetTab = 1
                                    }
                                    if (bodyInput.isBlank()) {
                                        bodyInput = "${prod.title} — ${prod.price}"
                                    }
                                }
                            ) {
                                Text(
                                    text = "${prod.title} (${prod.price})",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val shown = BekaNotificationManager.showNotification(
                                    context = context,
                                    notificationId = "local_test_${System.currentTimeMillis()}",
                                    title = titleInput.ifBlank { "BEKASERVICE ტესტი" },
                                    body = bodyInput.ifBlank { "სატესტო Push შეტყობინება წარმატებით მუშაობს!" },
                                    targetProductId = selectedProductId,
                                    targetTab = selectedTargetTab,
                                    type = selectedType,
                                    ignoreDeduplication = true,
                                    persistToHistory = false
                                )
                                if (shown) {
                                    Toast.makeText(context, "🔔 ლოკალური სატესტო შეტყობინება გამოჩნდა სტატუს-ბარში (ისტორიაში არ ინახება)!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "⚠️ შეამოწმეთ შეტყობინებების ნებართვა მოწყობილობაზე", Toast.LENGTH_LONG).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "ტესტი აქვე",
                                color = Color(0xFFFF7A00),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                if (bodyInput.isBlank()) {
                                    Toast.makeText(context, "შეიყვანეთ შეტყობინების ტექსტი", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                coroutineScope.launch {
                                    isSending = true
                                    val isEdit = editingNotificationId != null
                                    val notifId = editingNotificationId ?: "notif_${System.currentTimeMillis()}"
                                    val finalTitle = titleInput.ifBlank { "BEKASERVICE • ახალი შეთავაზება" }
                                    val finalBody = bodyInput.trim()

                                    val res = if (isEdit) {
                                        repo.updateAdminNotification(
                                            notificationId = notifId,
                                            title = finalTitle,
                                            body = finalBody,
                                            targetProductId = selectedProductId,
                                            targetTab = selectedTargetTab,
                                            type = selectedType,
                                            rebroadcastNow = true
                                        )
                                    } else {
                                        repo.sendAdminNotification(
                                            title = finalTitle,
                                            body = finalBody,
                                            targetProductId = selectedProductId,
                                            targetTab = selectedTargetTab,
                                            type = selectedType,
                                            explicitNotificationId = notifId
                                        )
                                    }
                                    isSending = false

                                    if (res.isSuccess) {
                                        BekaNotificationManager.clearDeliveredFlag(context, notifId)
                                        BekaNotificationManager.showNotification(
                                            context = context,
                                            notificationId = notifId,
                                            title = finalTitle,
                                            body = finalBody,
                                            targetProductId = selectedProductId,
                                            targetTab = selectedTargetTab,
                                            type = selectedType,
                                            ignoreDeduplication = true,
                                            persistToHistory = true
                                        )
                                        val updatedRecord = PushNotificationRecord(
                                            id = notifId,
                                            title = finalTitle,
                                            body = finalBody,
                                            type = selectedType,
                                            targetProductId = selectedProductId,
                                            targetTab = selectedTargetTab,
                                            senderEmail = firebaseUserEmail ?: "beqa1994@gmail.com",
                                            createdAtMillis = System.currentTimeMillis(),
                                            audience = "all",
                                            status = "submitted"
                                        )
                                        BekaNotificationManager.upsertLocalNotificationRecord(context, updatedRecord)
                                        recentNotifications = BekaNotificationManager.loadLocalNotificationHistory(context)
                                        editingNotificationId = null
                                        bodyInput = ""

                                        Toast.makeText(
                                            context,
                                            if (isEdit) "✅ შეტყობინება განახლდა და გამოქვეყნდა Cloud-ში!" else "✅ შეტყობინება გაიგზავნა Cloud-ში და დაემატა ისტორიაში!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val errMsg = FirestoreRepository.formatUserFriendlyFirestoreError(res.exceptionOrNull())
                                        Toast.makeText(
                                            context,
                                            "❌ გაგზავნა ვერ მოხერხდა: $errMsg",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isSending,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.35f)
                        ) {
                            Text(
                                text = when {
                                    isSending -> "იგზავნება..."
                                    editingNotificationId != null -> "💾 განახლება & გაგზავნა"
                                    else -> "ყველასთან გაგზავნა"
                                },
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (recentNotifications.isNotEmpty()) {
            item(key = "push_history_header") {
                Text(
                    text = "გაგზავნილი შეტყობინებების ისტორია (${recentNotifications.size})",
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(recentNotifications, key = { it.id }) { notif ->
                val linkedProduct = remember(notif.targetProductId, products) {
                    if (notif.targetProductId > 0) products.find { it.id == notif.targetProductId } else null
                }
                val formattedDate = remember(notif.createdAtMillis) {
                    java.text.SimpleDateFormat("dd.MM.yyyy • HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(notif.createdAtMillis))
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF16191E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (editingNotificationId == notif.id) Color(0xFFFF7A00) else Color(0xFF282D37)
                    ),
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
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = formattedDate,
                                color = Color(0xFF8C94A4),
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Text(
                            text = notif.title,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = notif.body,
                            color = Color(0xFFA0A5B0),
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E222B)
                            ) {
                                Text(
                                    text = "📍 ${BekaNotificationManager.targetTabLabel(notif.targetTab)}",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }

                            if (notif.targetProductId > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E222B),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = if (linkedProduct != null) {
                                            "🔗 ${linkedProduct.title}"
                                        } else {
                                            "🔗 პროდუქტის ID: #${notif.targetProductId}"
                                        },
                                        color = Color(0xFFFF9E40),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFF242830))

                        // მოქმედების ღილაკები: განმეორებით გაგზავნა, შესწორება, წაშლა
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // 1. განმეორებით გაგზავნა
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1B3B2B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4ADE80).copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        coroutineScope.launch {
                                            val resentId = "notif_${System.currentTimeMillis()}"
                                            val res = repo.sendAdminNotification(
                                                title = notif.title,
                                                body = notif.body,
                                                targetProductId = notif.targetProductId,
                                                targetTab = notif.targetTab,
                                                type = notif.type,
                                                explicitNotificationId = resentId
                                            )
                                            if (res.isSuccess) {
                                                BekaNotificationManager.showNotification(
                                                    context = context,
                                                    notificationId = resentId,
                                                    title = notif.title,
                                                    body = notif.body,
                                                    targetProductId = notif.targetProductId,
                                                    targetTab = notif.targetTab,
                                                    type = notif.type,
                                                    ignoreDeduplication = true,
                                                    persistToHistory = true
                                                )
                                                recentNotifications = BekaNotificationManager.loadLocalNotificationHistory(context)
                                                Toast.makeText(
                                                    context,
                                                    "🔄 შეტყობინება განმეორებით გაიგზავნა!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                val errMsg = FirestoreRepository.formatUserFriendlyFirestoreError(res.exceptionOrNull())
                                                Toast.makeText(context, "❌ გაგზავნა ვერ მოხერხდა: $errMsg", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = "🔄 განმეორებით გაგზავნა",
                                        color = Color(0xFF4ADE80),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }

                                // 2. შესწორება
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFF6A00).copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00)),
                                    modifier = Modifier.clickable {
                                        editingNotificationId = notif.id
                                        titleInput = notif.title
                                        bodyInput = notif.body
                                        selectedType = notif.type
                                        selectedProductId = notif.targetProductId
                                        selectedTargetTab = notif.targetTab
                                        Toast.makeText(
                                            context,
                                            "✏️ შეტყობინება გადმოტანილია სარედაქტირებლად ზედა ფორმაში",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    Text(
                                        text = "✏️ შესწორება",
                                        color = Color(0xFFFF9E40),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            // 3. წაშლა
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val delRes = repo.deleteAdminNotification(notif.id)
                                        if (delRes.isSuccess) {
                                            BekaNotificationManager.removeLocalNotificationRecord(context, notif.id)
                                            recentNotifications = recentNotifications.filterNot { it.id == notif.id }
                                            Toast.makeText(context, "🗑️ შეტყობინება წაიშალა", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val errMsg = FirestoreRepository.formatUserFriendlyFirestoreError(delRes.exceptionOrNull())
                                            Toast.makeText(context, "❌ წაშლა ვერ მოხერხდა: $errMsg", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
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
// ტაბი 9: აპლიკაციის განახლების მართვა (Remote App Update Management: app_config/updates)
// ==============================================================================
@Composable
fun AdminAppUpdateManagementView() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember { FirestoreRepository(context) }

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

    val initialLocal = remember { AdminStorage.getAppUpdateConfig(context) }
    var latestVersionInput by remember { mutableStateOf(initialLocal.latestVersion) }
    var changeLogInput by remember { mutableStateOf(initialLocal.changeLog) }
    var downloadUrlInput by remember { mutableStateOf(initialLocal.downloadUrl) }
    var isPublishedInput by remember { mutableStateOf(initialLocal.isPublished) }
    var lastUpdatedAtMillis by remember { mutableLongStateOf(initialLocal.updatedAtMillis) }
    var hasInitializedFromCloud by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var statusBannerMessage by remember { mutableStateOf<String?>(null) }
    var isStatusError by remember { mutableStateOf(false) }

    var firebaseUserEmail by remember {
        mutableStateOf(
            AuthAndOrderManager.resolveActiveUserEmail(context).ifBlank {
                if (AdminStorage.isAdminSessionActive(context)) AuthAndOrderManager.SOLE_ADMIN_EMAIL else null
            }
        )
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { AuthAndOrderManager.ensureAdminFirebaseSession(context) }
        }
        firebaseUserEmail = AuthAndOrderManager.resolveActiveUserEmail(context).ifBlank {
            if (AdminStorage.isAdminSessionActive(context)) AuthAndOrderManager.SOLE_ADMIN_EMAIL else null
        }
    }

    DisposableEffect(repo) {
        val auth = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance() }.getOrNull()
        val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { fa ->
            firebaseUserEmail = fa.currentUser?.email ?: AuthAndOrderManager.resolveActiveUserEmail(context).ifBlank {
                if (AdminStorage.isAdminSessionActive(context)) AuthAndOrderManager.SOLE_ADMIN_EMAIL else null
            }
        }
        auth?.addAuthStateListener(authListener)

        var reg: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            reg = repo.listenToAppUpdateConfig(
                onUpdate = { cloudConfig ->
                    if (cloudConfig != null) {
                        AdminStorage.saveAppUpdateConfig(context, cloudConfig)
                        if (!hasInitializedFromCloud) {
                            latestVersionInput = cloudConfig.latestVersion
                            changeLogInput = cloudConfig.changeLog
                            downloadUrlInput = cloudConfig.downloadUrl
                            isPublishedInput = cloudConfig.isPublished
                            hasInitializedFromCloud = true
                        }
                        lastUpdatedAtMillis = cloudConfig.updatedAtMillis
                    } else {
                        hasInitializedFromCloud = true
                    }
                }
            )
        } catch (_: Exception) {}

        onDispose {
            auth?.removeAuthStateListener(authListener)
            reg?.remove()
        }
    }

    val isDriveViewerLink = remember(downloadUrlInput) {
        AdminStorage.isGoogleDriveShareViewerLink(downloadUrlInput)
    }
    val convertedDirectDriveUrl = remember(downloadUrlInput) {
        AdminStorage.extractGoogleDriveDirectDownloadUrl(downloadUrlInput)
    }
    val isUrlValidHttps = remember(downloadUrlInput) {
        downloadUrlInput.isBlank() || AdminStorage.isValidHttpsDownloadUrl(downloadUrlInput)
    }

    fun persistUpdateConfig(publishOverride: Boolean? = null) {
        val targetPublished = publishOverride ?: isPublishedInput
        val cleanVersion = latestVersionInput.trim()
        val cleanLog = changeLogInput.trim()
        val cleanUrl = downloadUrlInput.trim()

        if (targetPublished) {
            if (cleanVersion.isBlank()) {
                isStatusError = true
                statusBannerMessage = "გამოსაქვეყნებლად აუცილებელია მიუთითოთ ახალი ვერსია (მაგ: 1.2.0)"
                Toast.makeText(context, statusBannerMessage, Toast.LENGTH_LONG).show()
                return
            }
            if (cleanLog.isBlank()) {
                isStatusError = true
                statusBannerMessage = "გამოსაქვეყნებლად აუცილებელია შეავსოთ განახლების აღწერა (Change Log)"
                Toast.makeText(context, statusBannerMessage, Toast.LENGTH_LONG).show()
                return
            }
            if (!AdminStorage.isValidHttpsDownloadUrl(cleanUrl)) {
                isStatusError = true
                statusBannerMessage = "გამოსაქვეყნებლად მიუთითეთ სწორი HTTPS APK ბმული (https://...)"
                Toast.makeText(context, statusBannerMessage, Toast.LENGTH_LONG).show()
                return
            }
        } else if (cleanUrl.isNotBlank() && !AdminStorage.isValidHttpsDownloadUrl(cleanUrl)) {
            isStatusError = true
            statusBannerMessage = "APK ბმული უნდა იწყებოდეს https:// პროტოკოლით"
            Toast.makeText(context, statusBannerMessage, Toast.LENGTH_LONG).show()
            return
        }

        coroutineScope.launch {
            isSaving = true
            statusBannerMessage = null
            val configToSave = AdminStorage.AppUpdateConfig(
                latestVersion = cleanVersion,
                changeLog = cleanLog,
                downloadUrl = cleanUrl,
                isPublished = targetPublished,
                updatedAtMillis = System.currentTimeMillis(),
                updatedBy = firebaseUserEmail ?: "beqa1994@gmail.com"
            )
            val res = repo.saveAppUpdateConfigToCloud(configToSave)
            isSaving = false
            if (res.isSuccess) {
                isPublishedInput = targetPublished
                lastUpdatedAtMillis = configToSave.updatedAtMillis
                AdminStorage.saveAppUpdateConfig(context, configToSave)
                isStatusError = false
                statusBannerMessage = if (targetPublished) {
                    "✅ განახლება (v$cleanVersion) წარმატებით შეინახა და გამოქვეყნდა ყველა მომხმარებლისთვის!"
                } else {
                    "✅ განახლების პარამეტრები შეინახა Cloud-ში (სტატუსი: არ არის გამოქვეყნებული)."
                }
                Toast.makeText(context, statusBannerMessage, Toast.LENGTH_SHORT).show()
            } else {
                isStatusError = true
                val errMsg = FirestoreRepository.formatUserFriendlyFirestoreError(res.exceptionOrNull())
                statusBannerMessage = "❌ შენახვა ვერ მოხერხდა: $errMsg"
                Toast.makeText(context, statusBannerMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .imePadding()
            .testTag("admin_app_update_management_view"),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. სათაურისა და ადმინის სტატუსის ბარათი
        item(key = "admin_update_header_card") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "აპლიკაციის განახლების მართვა",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPublishedInput) Color(0x224ADE80) else Color(0x22F87171)
                        ) {
                            Text(
                                text = if (isPublishedInput) "გამოქვეყნებულია ✅" else "დამალულია / Draft",
                                color = if (isPublishedInput) Color(0xFF4ADE80) else Color(0xFFF87171),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "აქედან იმართება მომხმარებლის „განახლება“ გვერდზე ნაჩვენები ახალი ვერსია, Change Log და APK გადმოწერის ბმული (Firestore: app_config/updates).",
                        color = Color(0xFFA0A5B0),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E222B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "ამ მოწყობილობაზე დაყენებული APK:",
                                    color = Color(0xFF8C94A4),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "v${installedVersionInfo.first} (Build #${installedVersionInfo.second})",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E222B),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Cloud-ში გამოქვეყნებული ვერსია:",
                                    color = Color(0xFF8C94A4),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (latestVersionInput.isNotBlank()) "v${latestVersionInput.trim()}" else "არ არის მითითებული",
                                    color = Color(0xFFFF7A00),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!firebaseUserEmail.equals("beqa1994@gmail.com", ignoreCase = true)) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    val signedIn = AuthAndOrderManager.signInWithGoogleCredentialManager(
                                        activityContext = context,
                                        onError = { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                    if (signedIn != null) {
                                        firebaseUserEmail = signedIn.email
                                        Toast.makeText(context, "✅ ადმინის Google ავტორიზაცია შესრულდა: ${signedIn.email}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Google-ით შესვლა Cloud-ში შესანახად (beqa1994@gmail.com)",
                                color = Color(0xFFFF7A00),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // სტატუსის ბანერი
        if (!statusBannerMessage.isNullOrBlank()) {
            item(key = "admin_update_status_banner") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isStatusError) Color(0xFF2B1618) else Color(0xFF12281E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isStatusError) Color(0xFFFF4D4F) else Color(0xFF4ADE80)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusBannerMessage ?: "",
                        color = if (isStatusError) Color(0xFFFFCCC7) else Color(0xFF86EFAC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 2. რედაქტირების ფორმა
        item(key = "admin_update_form_card") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282D37)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ველი 1: ახალი ვერსია (Latest Version)
                    OutlinedTextField(
                        value = latestVersionInput,
                        onValueChange = { latestVersionInput = it },
                        label = { Text("ახალი ვერსია (მაგ: 1.2.0)") },
                        placeholder = { Text("მაგ: 1.2.0") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF2E3440)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_update_version_input")
                    )

                    // ველი 2: განახლების აღწერა (Change Log - მრავალსტრიქონიანი)
                    OutlinedTextField(
                        value = changeLogInput,
                        onValueChange = { changeLogInput = it },
                        label = { Text("განახლების აღწერა (Change Log)") },
                        placeholder = {
                            Text("• დაემატა აპლიკაციის განახლების გვერდი\n• გაუმჯობესდა შეტყობინებების ისტორია\n• გასწორდა ვიზუალური დეტალები")
                        },
                        minLines = 4,
                        maxLines = 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF2E3440)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_update_changelog_input")
                    )

                    // ველი 3: APK გადმოწერის ბმული (HTTPS URL)
                    OutlinedTextField(
                        value = downloadUrlInput,
                        onValueChange = { downloadUrlInput = it },
                        label = { Text("APK გადმოწერის ბმული (HTTPS URL)") },
                        placeholder = { Text("https://drive.google.com/uc?export=download&id=...") },
                        singleLine = true,
                        isError = !isUrlValidHttps,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6A00),
                            unfocusedBorderColor = Color(0xFF2E3440),
                            errorBorderColor = Color(0xFFFF4D4F)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_update_url_input")
                    )

                    if (!isUrlValidHttps) {
                        Text(
                            text = "⚠️ ბმული აუცილებლად უნდა იწყებოდეს https:// პროტოკოლით.",
                            color = Color(0xFFFF4D4F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Google Drive-ის გაზიარების ბმულის გაფრთხილება და 1-კლიკიანი გარდამქმნელი
                    if (isDriveViewerLink) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2A2012),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9E40).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ℹ️ ყურადღება: ჩასმულია Google Drive-ის გაზიარების (Viewer) ბმული",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "სტანდარტული /file/d/.../view ბმული ხსნის Google Drive-ის ვებ-გვერდს და არ იწყებს APK-ს პირდაპირ გადმოწერას. შეგიძლიათ შეინახოთ როგორც არის, ან გადაიყვანოთ პირდაპირი ჩამოტვირთვის ბმულზე:",
                                    color = Color(0xFFE5E7EB),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                                if (convertedDirectDriveUrl != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFF6A00),
                                        modifier = Modifier.clickable {
                                            downloadUrlInput = convertedDirectDriveUrl
                                            Toast.makeText(
                                                context,
                                                "✅ ბმული გარდაიქმნა პირდაპირი გადმოწერის ფორმატში!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    ) {
                                        Text(
                                            text = "⚡ პირდაპირ გადმოსაწერ ბმულად გარდაქმნა",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ველი 4: გამოქვეყნების სტატუსი (Publish Status)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E222B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isPublishedInput) Color(0xFF4ADE80).copy(alpha = 0.5f) else Color(0xFF2E3440)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "გამოქვეყნების სტატუსი",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isPublishedInput) {
                                        "ჩართულია: მომხმარებლები ხედავენ ახალ ვერსიას და გადმოწერის ღილაკს"
                                    } else {
                                        "გამორთულია: მომხმარებლებთან ჩანს რომ განახლება ჯერ არ არის გამოქვეყნებული"
                                    },
                                    color = Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Switch(
                                checked = isPublishedInput,
                                onCheckedChange = { isPublishedInput = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF4ADE80),
                                    uncheckedThumbColor = Color(0xFFA0A5B0),
                                    uncheckedTrackColor = Color(0xFF282D37)
                                ),
                                modifier = Modifier.testTag("admin_update_publish_switch")
                            )
                        }
                    }

                    // ბმულის შემოწმების ღილაკი (თუ ბმული შეყვანილია)
                    if (AdminStorage.isValidHttpsDownloadUrl(downloadUrlInput)) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrlInput.trim())).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "ბმულის გახსნა ვერ მოხერხდა", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "შეყვანილი APK ბმულის შემოწმება ბრაუზერში",
                                color = Color(0xFF60A5FA),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // მოქმედების ღილაკები: შენახვა & გამოქვეყნება
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { persistUpdateConfig(publishOverride = null) },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00)),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("admin_update_save_button")
                        ) {
                            Text(
                                text = if (isSaving) "ინახება..." else "💾 შენახვა",
                                color = Color(0xFFFF7A00),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = { persistUpdateConfig(publishOverride = true) },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("admin_update_publish_button")
                        ) {
                            Text(
                                text = if (isSaving) "მიმდინარეობს..." else "🚀 გამოქვეყნება",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (isPublishedInput) {
                        OutlinedButton(
                            onClick = { persistUpdateConfig(publishOverride = false) },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF4D4F).copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "გამოქვეყნების გაუქმება (მომხმარებლებთან დამალვა)",
                                color = Color(0xFFFF7875),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
