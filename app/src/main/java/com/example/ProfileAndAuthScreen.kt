package com.example

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
    targetOrderId: String? = null,
    onTargetOrderConsumed: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(top = 74.dp, bottom = 82.dp)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppPreferencesManager.isDarkMode
    val authStateVersion = AuthAndOrderManager.authStateVersion.intValue
    val favStateVersion = AuthAndOrderManager.favoritesStateVersion.intValue

    var currentUser by remember(authStateVersion) {
        mutableStateOf(AuthAndOrderManager.getCurrentUser(context))
    }
    val isAuthorizedAdminUi = remember(currentUser?.id, currentUser?.email, authStateVersion, favStateVersion) {
        currentUser != null && AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)
    }
    val favoriteIds = remember(currentUser?.id, favStateVersion) {
        AuthAndOrderManager.getFavoriteProductIds(context)
    }
    val favoriteProducts = remember(products, favoriteIds, currentUser?.id, favStateVersion) {
        val baseCatalog = if (products.isNotEmpty()) products else ProductRepository.getAllProducts()
        baseCatalog.filter { it.id in favoriteIds || it.isFavorite }
    }

    var ordersList by remember(currentUser?.id, authStateVersion) {
        mutableStateOf<List<OrderRecord>>(emptyList())
    }
    var isOrdersLoading by remember(currentUser?.id, authStateVersion) {
        mutableStateOf(false)
    }
    var ordersError by remember(currentUser?.id, authStateVersion) {
        mutableStateOf<String?>(null)
    }
    var retryOrdersNonce by remember { mutableIntStateOf(0) }

    // Dialog states
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var quickEditContactMode by remember { mutableStateOf<String?>(null) } // "phone" | "address"
    var showPersonalDataDialog by remember { mutableStateOf(false) }
    var showSecuritySettingsDialog by remember { mutableStateOf(false) }
    var showAllOrdersDialog by remember { mutableStateOf(false) }
    var showAllFavoritesDialog by remember { mutableStateOf(false) }
    var selectedOrderForDetail by remember { mutableStateOf<OrderRecord?>(null) }
    var selectedOrderForPaymentInstructions by remember { mutableStateOf<OrderRecord?>(null) }
    var activeLegalPolicyTopic by remember { mutableStateOf<LegalPolicyTopic?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    var selectedOrderFilter by remember { mutableStateOf("აქტიური") }

    // Clear all user-scoped open dialogs immediately whenever account UID switches or logs out
    LaunchedEffect(currentUser?.id, authStateVersion) {
        selectedOrderForDetail = null
        selectedOrderForPaymentInstructions = null
        showAllOrdersDialog = false
        showAllFavoritesDialog = false
        showPersonalDataDialog = false
        showEditProfileDialog = false
        showChangePasswordDialog = false
        quickEditContactMode = null
        showDeleteAccountDialog = false
    }

    // Sync FirebaseAuth session changes cleanly
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
                    ordersError = null
                }
            }
        }
        auth?.addAuthStateListener(listener)
        onDispose { auth?.removeAuthStateListener(listener) }
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

    // Real-time UID-scoped orders sync from Firestore (fallback to UID-scoped local cache when offline/JVM test)
    DisposableEffect(currentUser?.id, authStateVersion, retryOrdersNonce) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        val activeUser = currentUser
        val fbUser = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
        val isRegisteredNonAnon = AuthAndOrderManager.isRegisteredAuthenticatedUser(context) &&
            !AuthAndOrderManager.isAnonymousFirebaseSession()

        ordersList = emptyList()
        ordersError = null

        if (activeUser != null && isRegisteredNonAnon) {
            if (fbUser != null && !fbUser.isAnonymous) {
                isOrdersLoading = true
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
            isOrdersLoading = false
        }

        onDispose { registration?.remove() }
    }

    // Deep-link target order handler with strict owner/admin check
    LaunchedEffect(targetOrderId, ordersList, isOrdersLoading, currentUser?.id, authStateVersion) {
        val requestedId = targetOrderId?.trim().orEmpty()
        if (requestedId.isBlank()) return@LaunchedEffect

        if (AuthAndOrderManager.isAnonymousFirebaseSession() ||
            !AuthAndOrderManager.isRegisteredAuthenticatedUser(context) ||
            currentUser == null
        ) {
            delay(350)
            if (AuthAndOrderManager.isAnonymousFirebaseSession() ||
                !AuthAndOrderManager.isRegisteredAuthenticatedUser(context) ||
                AuthAndOrderManager.getCurrentUser(context) == null
            ) {
                Toast.makeText(context, "შეკვეთის სანახავად გაიარეთ ავტორიზაცია", Toast.LENGTH_SHORT).show()
                onTargetOrderConsumed()
            }
            return@LaunchedEffect
        }

        val activeUid = AuthAndOrderManager.activeRegisteredUidOrNull(context).orEmpty()
        val matched = ordersList.find { it.id == requestedId || it.orderNumber == requestedId }
            ?: AuthAndOrderManager.getOrders(context).find { it.id == requestedId || it.orderNumber == requestedId }

        if (matched != null) {
            val isOwnerOrAdmin = (matched.userId.isBlank() || (activeUid.isNotBlank() && matched.userId == activeUid)) ||
                AuthAndOrderManager.isCurrentUserVerifiedSoleAdmin(context)
            if (!isOwnerOrAdmin) {
                Toast.makeText(context, "წვდომა უარყოფილია: ეს შეკვეთა სხვა ანგარიშს ეკუთვნის", Toast.LENGTH_LONG).show()
                onTargetOrderConsumed()
                return@LaunchedEffect
            }
            selectedOrderFilter = if (isOrderCompletedOrCancelled(matched.status)) "დასრულებული" else "აქტიური"
            selectedOrderForDetail = matched
            onTargetOrderConsumed()
        } else if (!isOrdersLoading) {
            val fetchResult = runCatching {
                FirestoreRepository(context).fetchUserOrderById(requestedId, activeUid)
            }.getOrNull()
            val fetchedOrder = fetchResult?.getOrNull()
            if (fetchedOrder != null) {
                ordersList = listOf(fetchedOrder) + ordersList.filterNot { it.id == fetchedOrder.id }
                selectedOrderFilter = if (isOrderCompletedOrCancelled(fetchedOrder.status)) "დასრულებული" else "აქტიური"
                selectedOrderForDetail = fetchedOrder
                onTargetOrderConsumed()
            } else if (fetchResult?.isFailure == true && fetchResult.exceptionOrNull() is IllegalAccessException) {
                Toast.makeText(context, "წვდომა უარყოფილია: ეს შეკვეთა სხვა ანგარიშს ეკუთვნის", Toast.LENGTH_LONG).show()
                onTargetOrderConsumed()
            } else {
                Toast.makeText(context, "შეკვეთა ან მოთხოვნა ვერ მოიძებნა ან წაშლილია.", Toast.LENGTH_LONG).show()
                onTargetOrderConsumed()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("profile_lazy_column"),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: "პროფილი & ანგარიშები" with orange accent underline
        item(key = "profile_header") {
            ProfileScreenHeader(isDark = isDark)
        }

        // Section A + B + C: User Identity Card OR Auth/Guest Card
        if (currentUser == null) {
            item(key = "profile_auth_guest") {
                AuthCard(
                    onGoogleClick = {
                        coroutineScope.launch {
                            val signedInUser = AuthAndOrderManager.signInWithGoogleCredentialManager(
                                activityContext = context,
                                onCancel = {
                                    Toast.makeText(context, "Google ავტორიზაცია გაუქმდა", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                }
                            )
                            if (signedInUser != null) {
                                currentUser = signedInUser
                                Toast.makeText(context, "Google ავტორიზაცია წარმატებულია", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onLoginSuccess = { user -> currentUser = user },
                    onForgotPassword = { showForgotPasswordDialog = true }
                )
            }
        } else {
            item(key = "profile_user_identity") {
                UserProfileCard(
                    user = currentUser!!,
                    onEditProfile = { showEditProfileDialog = true },
                    onOpenPersonalData = { showPersonalDataDialog = true },
                    onQuickAddPhone = { quickEditContactMode = "phone" },
                    onQuickAddAddress = { quickEditContactMode = "address" },
                    onChangePassword = { showChangePasswordDialog = true },
                    onLogout = {
                        coroutineScope.launch {
                            AuthAndOrderManager.logoutSafely(context)
                            currentUser = null
                            ordersList = emptyList()
                            Toast.makeText(context, "სისტემიდან გამოსვლა წარმატებულია", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        // Section D: Orders Card (for signed-in user)
        if (currentUser != null) {
            item(key = "profile_orders_section_item") {
                ProfileOrdersSection(
                    ordersList = ordersList,
                    selectedOrderFilter = selectedOrderFilter,
                    onFilterChange = { selectedOrderFilter = it },
                    isOrdersLoading = isOrdersLoading,
                    ordersError = ordersError,
                    products = products,
                    onRetryOrders = { retryOrdersNonce++ },
                    onOpenAllOrders = { showAllOrdersDialog = true },
                    onOpenOrderDetail = { selectedOrderForDetail = it },
                    onOpenPaymentInstructions = { selectedOrderForPaymentInstructions = it },
                    onOpenCatalog = onOpenCatalog
                )
            }
        }

        // Section E: Favorites Section
        item(key = "profile_favorites_item") {
            ProfileFavoritesSection(
                favoriteProducts = favoriteProducts,
                onOpenAllFavorites = { showAllFavoritesDialog = true },
                onProductClick = onProductClick,
                onRemoveFavorite = onToggleFavorite,
                onBrowseCatalog = onOpenCatalog
            )
        }

        // Section F: Security/Settings + Personal Data + Delete Account grouped card
        item(key = "profile_settings_group") {
            ProfileSecurityAndSettingsCard(
                isAuthenticated = currentUser != null,
                onOpenSecuritySettings = { showSecuritySettingsDialog = true },
                onOpenPersonalData = { showPersonalDataDialog = true },
                onRequestDeleteAccount = { showDeleteAccountDialog = true }
            )
        }

        // Section I: Policies & Payment Terms grouped card
        item(key = "profile_policies_group") {
            ProfilePoliciesGroupedCard(
                onOpenPolicyTopic = { activeLegalPolicyTopic = it }
            )
        }

        // Section G & J: Help & Contact card backed by ContactAndSocialManager
        item(key = "profile_contacts_card") {
            StoreContactsCard()
        }

        // Section H: Bottom Admin Panel Banner (ONLY for verified sole admin)
        if (isAuthorizedAdminUi) {
            item(key = "profile_admin_banner") {
                ProfileAdminPanelBanner(onOpenAdminPanel = onOpenAdminPanel)
            }
        }
    }

    // =========================================================================
    // DIALOGS & MODALS
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

    if (quickEditContactMode != null && currentUser != null) {
        val mode = quickEditContactMode!!
        QuickContactFieldDialog(
            mode = mode,
            initialValue = if (mode == "phone") currentUser!!.phone else currentUser!!.address,
            onDismiss = { quickEditContactMode = null },
            onSaved = { updated ->
                currentUser = updated
                quickEditContactMode = null
                Toast.makeText(
                    context,
                    if (mode == "phone") "ტელეფონის ნომერი შენახულია" else "მისამართი შენახულია",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    if (showPersonalDataDialog && currentUser != null) {
        PersonalDataDialog(
            user = currentUser!!,
            onDismiss = { showPersonalDataDialog = false },
            onOpenEditProfile = { showEditProfileDialog = true }
        )
    }

    if (showSecuritySettingsDialog) {
        SecurityAndSettingsDialog(
            user = currentUser,
            onDismiss = { showSecuritySettingsDialog = false },
            onOpenChangePassword = { showChangePasswordDialog = true },
            onOpenDeleteAccount = { showDeleteAccountDialog = true }
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

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(onDismiss = { showForgotPasswordDialog = false })
    }

    if (showAllOrdersDialog && currentUser != null) {
        AllOrdersDialog(
            orders = ordersList,
            products = products,
            initialFilter = selectedOrderFilter,
            onDismiss = { showAllOrdersDialog = false },
            onSelectOrder = {
                showAllOrdersDialog = false
                selectedOrderForDetail = it
            },
            onOpenPaymentInstructions = {
                showAllOrdersDialog = false
                selectedOrderForPaymentInstructions = it
            }
        )
    }

    if (showAllFavoritesDialog) {
        AllFavoritesDialog(
            favoriteProducts = favoriteProducts,
            onDismiss = { showAllFavoritesDialog = false },
            onProductClick = onProductClick,
            onAddToCart = onAddToCart,
            onRemoveFavorite = onToggleFavorite
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
            } else {
                null
            },
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
                    selectedOrderForDetail = null
                    Toast.makeText(context, "შეკვეთა წაიშალა", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (selectedOrderForPaymentInstructions != null) {
        OrderPaymentInstructionsDialog(
            order = selectedOrderForPaymentInstructions!!,
            onDismiss = { selectedOrderForPaymentInstructions = null },
            onOrderUpdated = { updatedOrder ->
                ordersList = ordersList.map { if (it.id == updatedOrder.id) updatedOrder else it }
                selectedOrderForPaymentInstructions = updatedOrder
            }
        )
    }

    if (activeLegalPolicyTopic != null) {
        BstoreLegalPoliciesDialog(
            initialTopic = activeLegalPolicyTopic!!,
            onDismiss = { activeLegalPolicyTopic = null }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountConfirmationDialog(
            isDeleting = isDeletingAccount,
            onDismiss = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            onConfirmDelete = {
                coroutineScope.launch {
                    isDeletingAccount = true
                    val result = AuthAndOrderManager.deleteUserAccount(context)
                    isDeletingAccount = false
                    showDeleteAccountDialog = false
                    if (result.isSuccess) {
                        currentUser = null
                        ordersList = emptyList()
                        Toast.makeText(context, "ანგარიში და მონაცემები სრულად წაიშალა", Toast.LENGTH_LONG).show()
                    } else {
                        val errMsg = result.exceptionOrNull()?.localizedMessage ?: ""
                        if (errMsg.contains("recent authentication", ignoreCase = true) ||
                            errMsg.contains("requires-recent-login", ignoreCase = true)
                        ) {
                            Toast.makeText(
                                context,
                                "უსაფრთხოებისთვის ხელახლა შედით ანგარიშში და სცადეთ წაშლა",
                                Toast.LENGTH_LONG
                            ).show()
                            currentUser = null
                            ordersList = emptyList()
                        } else {
                            Toast.makeText(
                                context,
                                "ანგარიში გასუფთავდა ლოკალურად ($errMsg)",
                                Toast.LENGTH_LONG
                            ).show()
                            currentUser = null
                            ordersList = emptyList()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun BankRequisitesCard() {
    var activeTopic by remember { mutableStateOf<LegalPolicyTopic?>(null) }
    ProfilePoliciesGroupedCard(
        onOpenPolicyTopic = { activeTopic = it }
    )
    if (activeTopic != null) {
        BstoreLegalPoliciesDialog(
            initialTopic = activeTopic!!,
            onDismiss = { activeTopic = null }
        )
    }
}
