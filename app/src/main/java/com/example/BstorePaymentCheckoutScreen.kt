package com.example

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// ==============================================================================
// BSTORE CHECKOUT & PAYMENT SCREEN (EXACT REFERENCE RECREATION + REAL LOGIC)
// ==============================================================================

private object CheckoutPalette {
    val isDark: Boolean
        get() = AppPreferencesManager.isDarkMode
    val ScreenBg: Color
        get() = if (isDark) Color(0xFF0E0F12) else Color(0xFFEEF0F5)
    val CardBg: Color
        get() = if (isDark) Color(0xFF1A1B20) else Color(0xFFFFFFFF)
    val SelectedCardBg: Color
        get() = if (isDark) Color(0xFF211712) else Color(0xFFFFF3E8)
    val DisabledCardBg: Color
        get() = if (isDark) Color(0xFF141519) else Color(0xFFE5E8F0)
    val CardBorder: Color
        get() = if (isDark) Color(0xFF2B2E37) else Color(0xFFCBD2DF)
    val Divider: Color
        get() = if (isDark) Color(0xFF282A32) else Color(0xFFD8DEE9)
    val CopyBg: Color
        get() = if (isDark) Color(0xFF23252C) else Color(0xFFF3F5F9)
    val CopyBorder: Color
        get() = if (isDark) Color(0xFF393C46) else Color(0xFFC5CDD9)
    val AccentOrange: Color
        get() = if (isDark) Color(0xFFFF6A00) else Color(0xFFEA580C)
    val CtaDarkText: Color
        get() = if (isDark) Color(0xFF111214) else Color.White
    val PrimaryText: Color
        get() = if (isDark) Color.White else Color(0xFF111827)
    val InfoBlue: Color
        get() = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val SuccessGreen: Color
        get() = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
    val CopiedBadgeBg: Color
        get() = if (isDark) Color(0xFF102A1A) else Color(0xFFECFDF5)
    val MutedText: Color
        get() = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val SecondaryText: Color
        get() = if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151)
    val ErrorRed: Color
        get() = if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)
    val ErrorBannerBg: Color
        get() = if (isDark) Color(0xFF2A1215) else Color(0xFFFEF2F2)
    val ErrorBannerText: Color
        get() = if (isDark) Color(0xFFFECACA) else Color(0xFFB91C1C)
}

internal object BstoreCheckoutLogic {
    fun parseNumericGel(priceText: String): Int {
        return priceText.filter { it.isDigit() }.toIntOrNull() ?: 0
    }

    fun formatGelAmount(amount: Int): String {
        if (amount <= 0) return "ფასი შეთანხმებით"
        val formatted = NumberFormat.getIntegerInstance(Locale.US).format(amount)
        return "$formatted ₾"
    }

    fun formatItemPriceDisplay(rawPrice: String): String {
        val numeric = parseNumericGel(rawPrice)
        return if (numeric > 0) formatGelAmount(numeric) else rawPrice.ifBlank { "0 ₾" }
    }

    fun formatItemLinePriceDisplay(rawPrice: String, quantity: Int): String {
        val numeric = parseNumericGel(rawPrice)
        val safeQty = quantity.coerceAtLeast(1)
        return if (numeric > 0) formatGelAmount(numeric * safeQty) else rawPrice.ifBlank { "0 ₾" }
    }

    data class PhoneValidationResult(
        val isValid: Boolean,
        val formattedPhone: String,
        val errorMessageKa: String? = null
    )

    fun validateAndNormalizeGeorgianPhone(rawInput: String): PhoneValidationResult {
        val compact = rawInput.trim().replace(Regex("[\\s\\-()]"), "")
        if (compact.isBlank()) {
            return PhoneValidationResult(
                isValid = false,
                formattedPhone = "",
                errorMessageKa = "გთხოვთ მიუთითოთ საკონტაქტო ტელეფონის ნომერი"
            )
        }
        val localDigits = when {
            compact.startsWith("+995") -> compact.removePrefix("+995")
            compact.startsWith("995") && compact.length == 12 -> compact.removePrefix("995")
            compact.startsWith("0") && compact.length == 10 -> compact.removePrefix("0")
            else -> compact
        }
        if (localDigits.length != 9 || !localDigits.all { it.isDigit() }) {
            return PhoneValidationResult(
                isValid = false,
                formattedPhone = rawInput.trim(),
                errorMessageKa = "შეიყვანეთ სწორი ქართული ნომერი (მაგ: +995 555 12 34 56)"
            )
        }
        val validPrefix = localDigits.startsWith("5") ||
            localDigits.startsWith("3") ||
            localDigits.startsWith("4") ||
            localDigits.startsWith("7") ||
            localDigits.startsWith("2")
        if (!validPrefix) {
            return PhoneValidationResult(
                isValid = false,
                formattedPhone = rawInput.trim(),
                errorMessageKa = "მიუთითეთ მოქმედი ქართული ტელეფონის ნომერი"
            )
        }
        val formatted = "+995 ${localDigits.substring(0, 3)} ${localDigits.substring(3, 5)} ${localDigits.substring(5, 7)} ${localDigits.substring(7, 9)}"
        return PhoneValidationResult(isValid = true, formattedPhone = formatted)
    }

    fun twoLineBankLabel(bankCode: String, bankName: String): String {
        return when (BankTransferHandoffHelper.resolveBankType(bankCode, bankName)) {
            BankTransferHandoffHelper.BankType.BOG -> "საქართველოს\nბანკი"
            BankTransferHandoffHelper.BankType.TBC -> "თიბისი\nბანკი"
            BankTransferHandoffHelper.BankType.UNKNOWN -> bankName.ifBlank { "სხვა ბანკი" }
        }
    }

    fun bankGroupKey(account: BankAccountConfig): String {
        return when (BankTransferHandoffHelper.resolveBankType(account.bankCode, account.bankName)) {
            BankTransferHandoffHelper.BankType.BOG -> "bog"
            BankTransferHandoffHelper.BankType.TBC -> "tbc"
            BankTransferHandoffHelper.BankType.UNKNOWN -> account.bankCode.lowercase(Locale.ROOT).ifBlank { account.id }
        }
    }

    fun resolveReferenceInstructionText(bank: BankAccountConfig): String {
        val custom = bank.paymentInstructions.trim()
        if (custom.isNotBlank() && custom != BankAccountConfig.DEFAULT_PAYMENT_INSTRUCTIONS_KA) {
            return custom
        }
        return "გადარიცხეთ მითითებულ ანგარიშზე და დაბრუნდით აპლიკაციაში."
    }
}

@Composable
fun BstoreCheckoutDialog(
    cartItems: List<CartItem>,
    onDismiss: () -> Unit,
    onOpenLegalPolicy: (LegalPolicyTopic) -> Unit = {},
    onUpdateCartItemQuantity: ((CartItem, Int) -> Unit)? = null,
    onRemoveCartItem: ((CartItem) -> Unit)? = null,
    onOrderCreated: (OrderRecord) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser = remember { AuthAndOrderManager.getCurrentUser(context) }
    val paymentStateVersion = BstorePaymentManager.stateVersion.intValue
    val enabledBanks = remember(paymentStateVersion) {
        BstorePaymentManager.getEnabledBankAccounts(context)
    }

    val editableCartItems = remember {
        mutableStateListOf<CartItem>().apply {
            addAll(cartItems.map { it.copy() })
        }
    }
    LaunchedEffect(cartItems) {
        val externalSig = cartItems.map { Triple(it.product.id, it.selectedQuality, it.quantity) }
        val localSig = editableCartItems.map { Triple(it.product.id, it.selectedQuality, it.quantity) }
        if (externalSig != localSig) {
            editableCartItems.clear()
            editableCartItems.addAll(cartItems.map { it.copy() })
        }
    }

    val initialPhone = remember(currentUser?.phone) {
        val saved = currentUser?.phone.orEmpty().trim()
        if (saved.isBlank()) "" else BstoreCheckoutLogic.validateAndNormalizeGeorgianPhone(saved).let {
            if (it.isValid) it.formattedPhone else saved
        }
    }

    var selectedDeliveryMethodCode by rememberSaveable { mutableStateOf(DeliveryMethodType.PICKUP.code) }
    var isDeliverySelectorExpanded by rememberSaveable { mutableStateOf(false) }
    var deliveryCityInput by rememberSaveable { mutableStateOf("ახალციხე") }
    var deliveryAddressInput by rememberSaveable { mutableStateOf(currentUser?.address.orEmpty()) }
    var customerPhoneInput by rememberSaveable { mutableStateOf(initialPhone) }

    var selectedPaymentMethodCode by rememberSaveable {
        mutableStateOf(
            if (enabledBanks.isNotEmpty()) PaymentMethodType.BANK_TRANSFER.code else PaymentMethodType.COD.code
        )
    }
    var userManuallyChosePaymentMethod by rememberSaveable { mutableStateOf(false) }
    var selectedBankAccountId by rememberSaveable {
        mutableStateOf(enabledBanks.firstOrNull()?.id.orEmpty())
    }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var submissionOrderId by rememberSaveable { mutableStateOf("ord_${System.currentTimeMillis()}") }

    var validationError by remember { mutableStateOf<String?>(null) }
    var phoneFieldError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var isLoadingBanks by remember { mutableStateOf(false) }
    var bankLoadError by remember { mutableStateOf<String?>(null) }
    var bankReloadTrigger by remember { mutableIntStateOf(0) }

    var lastAutoCopiedKey by rememberSaveable { mutableStateOf("") }
    var checkoutIbanCopiedHint by rememberSaveable { mutableStateOf(false) }
    var manualCopyPulse by remember { mutableStateOf(false) }
    var bankHandoffInstructionBanner by rememberSaveable { mutableStateOf<String?>(null) }
    var bankHandoffErrorMessage by remember { mutableStateOf<String?>(null) }
    var missingBankAppDialogPackage by remember { mutableStateOf<String?>(null) }
    var activeLegalPolicyTopic by remember { mutableStateOf<LegalPolicyTopic?>(null) }

    val selectedDeliveryMethod = DeliveryMethodType.fromCode(selectedDeliveryMethodCode)
    val selectedPaymentMethod = PaymentMethodType.fromCode(selectedPaymentMethodCode)

    // Real-time Firestore bank account sync & fallback to app_config/bank_requisites
    DisposableEffect(Unit) {
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        val repo = runCatching { FirestoreRepository(context) }.getOrNull()
        val registration = if (fbUser != null && !fbUser.isAnonymous && repo != null) {
            runCatching {
                repo.listenToPaymentBankAccounts { cloudAccounts ->
                    if (cloudAccounts.isNotEmpty()) {
                        BstorePaymentManager.syncFromCloudBankAccounts(context, cloudAccounts)
                    }
                    bankLoadError = null
                    isLoadingBanks = false
                }
            }.getOrNull()
        } else {
            null
        }
        val legacyRegistration = runCatching {
            repo?.listenToBankRequisites { requisites ->
                BstorePaymentManager.syncFromLegacyRequisites(context, requisites)
                bankLoadError = null
                isLoadingBanks = false
            }
        }.getOrNull()
        onDispose {
            registration?.remove()
            legacyRegistration?.remove()
        }
    }

    LaunchedEffect(bankReloadTrigger) {
        val repo = runCatching { FirestoreRepository(context) }.getOrNull() ?: return@LaunchedEffect
        val fbUser = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (fbUser != null && !fbUser.isAnonymous) {
            isLoadingBanks = true
            bankLoadError = null
            val cloudResult = runCatching {
                repo.fetchPaymentBankAccountsFromCloud()
            }.getOrElse { Result.failure(it) }
            if (cloudResult.isSuccess) {
                val accounts = cloudResult.getOrNull().orEmpty()
                if (accounts.isNotEmpty()) {
                    BstorePaymentManager.syncFromCloudBankAccounts(context, accounts)
                } else {
                    val legacyReq = runCatching { repo.fetchBankRequisitesFromCloud().getOrNull() }.getOrNull()
                    if (legacyReq != null) {
                        BstorePaymentManager.syncFromLegacyRequisites(context, legacyReq)
                    }
                }
                isLoadingBanks = false
                bankLoadError = null
            } else {
                val legacyReq = runCatching { repo.fetchBankRequisitesFromCloud().getOrNull() }.getOrNull()
                if (legacyReq != null) {
                    BstorePaymentManager.syncFromLegacyRequisites(context, legacyReq)
                }
                isLoadingBanks = false
                if (BstorePaymentManager.getEnabledBankAccounts(context).isEmpty()) {
                    bankLoadError = "საბანკო რეკვიზიტების ჩატვირთვა ვერ მოხერხდა. შეამოწმეთ ინტერნეტი და სცადეთ თავიდან."
                }
            }
        } else {
            val legacyReq = runCatching { repo.fetchBankRequisitesFromCloud().getOrNull() }.getOrNull()
            if (legacyReq != null) {
                BstorePaymentManager.syncFromLegacyRequisites(context, legacyReq)
            }
        }
    }

    LaunchedEffect(enabledBanks) {
        if (enabledBanks.isEmpty()) {
            selectedPaymentMethodCode = PaymentMethodType.COD.code
            selectedBankAccountId = ""
        } else {
            if (!userManuallyChosePaymentMethod && selectedPaymentMethodCode == PaymentMethodType.COD.code) {
                selectedPaymentMethodCode = PaymentMethodType.BANK_TRANSFER.code
            }
            if (selectedBankAccountId.isBlank() || enabledBanks.none { it.id == selectedBankAccountId }) {
                selectedBankAccountId = enabledBanks.first().id
            }
        }
    }

    val selectedBank = remember(enabledBanks, selectedBankAccountId) {
        enabledBanks.find { it.id == selectedBankAccountId } ?: enabledBanks.firstOrNull()
    }

    val bankGroups = remember(enabledBanks) {
        enabledBanks.groupBy { BstoreCheckoutLogic.bankGroupKey(it) }
    }

    val selectedGroupAccounts = remember(bankGroups, selectedBank) {
        if (selectedBank == null) emptyList()
        else bankGroups[BstoreCheckoutLogic.bankGroupKey(selectedBank)].orEmpty()
    }

    val totalNumericGel = editableCartItems.sumOf { item ->
        val unitGel = BstoreCheckoutLogic.parseNumericGel(
            item.product.effectiveDisplayPrice.ifBlank { item.product.price }
        )
        unitGel * item.quantity
    }
    val totalFormatted = BstoreCheckoutLogic.formatGelAmount(totalNumericGel)

    // Auto-copy active valid IBAN when selected bank changes (guarded against unrelated recompositions)
    LaunchedEffect(
        selectedPaymentMethod,
        selectedBank?.id,
        selectedBank?.iban,
        selectedBank?.isEnabled
    ) {
        val activeBank = selectedBank
        if (selectedPaymentMethod == PaymentMethodType.BANK_TRANSFER &&
            activeBank != null &&
            activeBank.isEnabled &&
            BstorePaymentManager.isValidIban(activeBank.iban)
        ) {
            val cleanIban = BstorePaymentManager.normalizeIban(activeBank.iban)
            val key = "${activeBank.id}:$cleanIban"
            if (lastAutoCopiedKey != key) {
                val copied = BankTransferHandoffHelper.copyIbanToClipboardSilently(
                    context = context,
                    iban = cleanIban,
                    label = "${activeBank.bankName} IBAN"
                )
                if (copied != null) {
                    lastAutoCopiedKey = key
                    checkoutIbanCopiedHint = true
                    bankHandoffInstructionBanner = null
                    bankHandoffErrorMessage = null
                }
            }
        }
    }

    BackHandler(enabled = !isSubmitting) {
        onDismiss()
    }

    // Compute root window bottom inset (handles Dialog window where WindowInsets.navigationBars may report 0)
    val rootNavBottomDp = with(density) {
        val rootInsets = ViewCompat.getRootWindowInsets(view)
        val navPx = rootInsets?.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.navigationBars() or
                WindowInsetsCompat.Type.mandatorySystemGestures()
        )?.bottom ?: 0
        val composeNavPx = WindowInsets.navigationBars.getBottom(this)
        maxOf(navPx, composeNavPx).toDp()
    }
    // Guaranteed clearance above Android gesture pill / 3-button navigation bar
    val bottomSafeClearanceDp = maxOf(rootNavBottomDp + 12.dp, 20.dp)

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            color = CheckoutPalette.ScreenBg,
            modifier = Modifier
                .fillMaxSize()
                .testTag("bstore_checkout_dialog")
        ) {
            ProvideTextStyle(
                value = LocalTextStyle.current.merge(
                    TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                            .padding(bottom = bottomSafeClearanceDp)
                            .imePadding()
                    ) {
                        // ==========================================================
                        // TOP BAR (<- გადახდა) — Compact header
                        // ==========================================================
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (!isSubmitting) onDismiss() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("checkout_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "უკან დაბრუნება",
                                    tint = CheckoutPalette.PrimaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "გადახდა",
                                color = CheckoutPalette.PrimaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // ==========================================================
                        // SCROLLABLE BODY — Unified flow (Summary -> Terms -> CTA)
                        // No weight(1f) stretching gap before the confirmation button
                        // ==========================================================
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    start = 14.dp,
                                    end = 14.dp,
                                    top = 2.dp,
                                    bottom = 8.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                        // ------------------------------------------------------
                        // 1. ORDER SUMMARY CARD
                        // ------------------------------------------------------
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CheckoutPalette.CardBg,
                            border = BorderStroke(1.dp, CheckoutPalette.CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_summary_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val canRemoveItems = editableCartItems.size > 1
                                editableCartItems.forEachIndexed { index, item ->
                                    if (index > 0) {
                                        HorizontalDivider(color = CheckoutPalette.Divider.copy(alpha = 0.6f))
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkout_item_row_${item.product.id}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(Color(0xFFF3F4F6))
                                                .border(1.dp, CheckoutPalette.CardBorder, RoundedCornerShape(9.dp))
                                        ) {
                                            ProductSmartVisual(
                                                product = item.product,
                                                compact = true,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = item.product.title,
                                                color = CheckoutPalette.PrimaryText,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            // Quantity stepper (- 1 +) on every product
                                            Surface(
                                                shape = RoundedCornerShape(7.dp),
                                                color = CheckoutPalette.CopyBg,
                                                border = BorderStroke(1.dp, CheckoutPalette.CopyBorder),
                                                modifier = Modifier.testTag("checkout_qty_stepper_${item.product.id}")
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.height(22.dp)
                                                ) {
                                                    val canDecrease = item.quantity > 1 && !isSubmitting
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(width = 24.dp, height = 22.dp)
                                                            .clickable(enabled = canDecrease) {
                                                                val newQty = (item.quantity - 1).coerceAtLeast(1)
                                                                val idx = editableCartItems.indexOfFirst {
                                                                    it.product.id == item.product.id &&
                                                                        it.selectedQuality == item.selectedQuality
                                                                }
                                                                if (idx >= 0) {
                                                                    val updated = editableCartItems[idx].copy(quantity = newQty)
                                                                    editableCartItems[idx] = updated
                                                                    onUpdateCartItemQuantity?.invoke(updated, newQty)
                                                                    validationError = null
                                                                }
                                                            }
                                                            .testTag("checkout_qty_decrease_${item.product.id}")
                                                    ) {
                                                        Text(
                                                            text = "-",
                                                            color = if (canDecrease) CheckoutPalette.PrimaryText else CheckoutPalette.MutedText.copy(alpha = 0.45f),
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .widthIn(min = 22.dp)
                                                            .padding(horizontal = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "${item.quantity}",
                                                            color = CheckoutPalette.PrimaryText,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.testTag("checkout_qty_value_${item.product.id}")
                                                        )
                                                    }

                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier
                                                            .size(width = 24.dp, height = 22.dp)
                                                            .clickable(enabled = !isSubmitting) {
                                                                val newQty = (item.quantity + 1).coerceAtMost(99)
                                                                val idx = editableCartItems.indexOfFirst {
                                                                    it.product.id == item.product.id &&
                                                                        it.selectedQuality == item.selectedQuality
                                                                }
                                                                if (idx >= 0) {
                                                                    val updated = editableCartItems[idx].copy(quantity = newQty)
                                                                    editableCartItems[idx] = updated
                                                                    onUpdateCartItemQuantity?.invoke(updated, newQty)
                                                                    validationError = null
                                                                }
                                                            }
                                                            .testTag("checkout_qty_increase_${item.product.id}")
                                                    ) {
                                                        Text(
                                                            text = "+",
                                                            color = CheckoutPalette.AccentOrange,
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = BstoreCheckoutLogic.formatItemLinePriceDisplay(
                                                    item.product.effectiveDisplayPrice.ifBlank { item.product.price },
                                                    item.quantity
                                                ),
                                                color = CheckoutPalette.PrimaryText,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                modifier = Modifier.testTag("checkout_item_price_${item.product.id}")
                                            )

                                            if (canRemoveItems) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = CheckoutPalette.ErrorBannerBg,
                                                    border = BorderStroke(1.dp, CheckoutPalette.ErrorRed.copy(alpha = 0.45f)),
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clickable(enabled = !isSubmitting && editableCartItems.size > 1) {
                                                            val idx = editableCartItems.indexOfFirst {
                                                                it.product.id == item.product.id &&
                                                                    it.selectedQuality == item.selectedQuality
                                                            }
                                                            if (idx >= 0 && editableCartItems.size > 1) {
                                                                val removed = editableCartItems.removeAt(idx)
                                                                onRemoveCartItem?.invoke(removed)
                                                                validationError = null
                                                            }
                                                        }
                                                        .testTag("checkout_remove_item_${item.product.id}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.DeleteOutline,
                                                            contentDescription = "პროდუქტის წაშლა",
                                                            tint = CheckoutPalette.ErrorRed,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = CheckoutPalette.Divider)

                                // Delivery line matching reference ("მიწოდება: ...") + expandable toggle
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isDeliverySelectorExpanded = !isDeliverySelectorExpanded }
                                            .padding(vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.LocalShipping,
                                                contentDescription = null,
                                                tint = CheckoutPalette.SecondaryText,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (selectedDeliveryMethod == DeliveryMethodType.PICKUP) {
                                                    "მიწოდება: 1-2 დღე (ფილიალიდან)"
                                                } else {
                                                    "მიწოდება: 1-3 დღე (${deliveryCityInput.ifBlank { "საქართველო" }})"
                                                },
                                                color = CheckoutPalette.SecondaryText,
                                                fontSize = 12.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = if (isDeliverySelectorExpanded) "დამალვა" else "შეცვლა",
                                            color = CheckoutPalette.AccentOrange,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = isDeliverySelectorExpanded || selectedDeliveryMethod == DeliveryMethodType.COURIER
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                DeliveryMethodType.entries.forEach { method ->
                                                    val isSel = selectedDeliveryMethod == method
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (isSel) CheckoutPalette.SelectedCardBg else CheckoutPalette.DisabledCardBg,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isSel) CheckoutPalette.AccentOrange else CheckoutPalette.CardBorder
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 38.dp)
                                                            .clickable {
                                                                selectedDeliveryMethodCode = method.code
                                                                validationError = null
                                                            }
                                                            .testTag("checkout_delivery_${method.code}")
                                                    ) {
                                                        Box(
                                                            contentAlignment = Alignment.Center,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                        ) {
                                                            Text(
                                                                text = if (method == DeliveryMethodType.PICKUP) "ფილიალიდან (0 ₾)" else "კურიერით მიწოდება",
                                                                color = if (isSel) CheckoutPalette.PrimaryText else CheckoutPalette.MutedText,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (selectedDeliveryMethod == DeliveryMethodType.COURIER) {
                                                OutlinedTextField(
                                                    value = deliveryCityInput,
                                                    onValueChange = {
                                                        deliveryCityInput = it
                                                        validationError = null
                                                    },
                                                    label = { Text("ქალაქი / რეგიონი *") },
                                                    placeholder = { Text("მაგ: თბილისი, ბათუმი, ახალციხე...") },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = CheckoutPalette.PrimaryText,
                                                        unfocusedTextColor = CheckoutPalette.PrimaryText,
                                                        focusedBorderColor = CheckoutPalette.AccentOrange,
                                                        unfocusedBorderColor = CheckoutPalette.CardBorder,
                                                        focusedLabelColor = CheckoutPalette.AccentOrange,
                                                        unfocusedLabelColor = CheckoutPalette.MutedText
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("checkout_city_input")
                                                )
                                                OutlinedTextField(
                                                    value = deliveryAddressInput,
                                                    onValueChange = {
                                                        deliveryAddressInput = it
                                                        validationError = null
                                                    },
                                                    label = { Text("ზუსტი მისამართი *") },
                                                    placeholder = { Text("ქუჩა, ნომერი, სადარბაზო...") },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = CheckoutPalette.PrimaryText,
                                                        unfocusedTextColor = CheckoutPalette.PrimaryText,
                                                        focusedBorderColor = CheckoutPalette.AccentOrange,
                                                        unfocusedBorderColor = CheckoutPalette.CardBorder,
                                                        focusedLabelColor = CheckoutPalette.AccentOrange,
                                                        unfocusedLabelColor = CheckoutPalette.MutedText
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("checkout_address_input")
                                                )
                                            } else {
                                                Text(
                                                    text = "ფილიალის მისამართი: ქ. ახალციხე, კოსტავას N10",
                                                    color = CheckoutPalette.MutedText,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = CheckoutPalette.Divider)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "საბოლოო თანხა:",
                                        color = CheckoutPalette.PrimaryText,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = totalFormatted,
                                        color = CheckoutPalette.AccentOrange,
                                        fontSize = 19.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("checkout_total_amount")
                                    )
                                }
                            }
                        }

                        // ------------------------------------------------------
                        // 2. PAYMENT METHOD SECTION ("გადახდის მეთოდი")
                        // ------------------------------------------------------
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CreditCard,
                                    contentDescription = null,
                                    tint = CheckoutPalette.AccentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "გადახდის მეთოდი",
                                    color = CheckoutPalette.PrimaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val bankTransferAvailable = enabledBanks.isNotEmpty()
                                ReferenceSelectableOptionCard(
                                    selected = selectedPaymentMethod == PaymentMethodType.BANK_TRANSFER,
                                    enabled = bankTransferAvailable,
                                    onClick = {
                                        userManuallyChosePaymentMethod = true
                                        selectedPaymentMethodCode = PaymentMethodType.BANK_TRANSFER.code
                                        validationError = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_payment_method_bank_transfer")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalance,
                                            contentDescription = null,
                                            tint = if (bankTransferAvailable) CheckoutPalette.PrimaryText else CheckoutPalette.MutedText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "საბანკო\nგადარიცხვა",
                                            color = if (bankTransferAvailable) CheckoutPalette.PrimaryText else CheckoutPalette.MutedText,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 15.sp,
                                            maxLines = 2
                                        )
                                    }
                                }

                                ReferenceSelectableOptionCard(
                                    selected = selectedPaymentMethod == PaymentMethodType.COD,
                                    enabled = true,
                                    onClick = {
                                        userManuallyChosePaymentMethod = true
                                        selectedPaymentMethodCode = PaymentMethodType.COD.code
                                        validationError = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_payment_method_cod")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = CheckoutPalette.PrimaryText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "ადგილზე\nგადახდა",
                                            color = CheckoutPalette.PrimaryText,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 15.sp,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            if (enabledBanks.isEmpty() && !isLoadingBanks) {
                                Text(
                                    text = bankLoadError
                                        ?: "საბანკო გადარიცხვა დროებით მიუწვდომელია — გთხოვთ აირჩიოთ ნაღდი ანგარიშსწორება",
                                    color = if (bankLoadError != null) CheckoutPalette.ErrorRed else CheckoutPalette.MutedText,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // ------------------------------------------------------
                        // 3. BANK SELECTION + BANK ACCOUNT INFO CARD
                        // ------------------------------------------------------
                        AnimatedVisibility(visible = selectedPaymentMethod == PaymentMethodType.BANK_TRANSFER) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (isLoadingBanks && enabledBanks.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CheckoutPalette.CardBg,
                                        border = BorderStroke(1.dp, CheckoutPalette.CardBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkout_banks_loading")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = CheckoutPalette.AccentOrange,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "იტვირთება საბანკო ანგარიშები...",
                                                color = CheckoutPalette.SecondaryText,
                                                fontSize = 12.5.sp
                                            )
                                        }
                                    }
                                } else if (bankLoadError != null && enabledBanks.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CheckoutPalette.ErrorBannerBg,
                                        border = BorderStroke(1.dp, CheckoutPalette.ErrorRed.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("checkout_banks_error")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = bankLoadError!!,
                                                color = CheckoutPalette.ErrorBannerText,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                            OutlinedButton(
                                                onClick = { bankReloadTrigger += 1 },
                                                border = BorderStroke(1.dp, CheckoutPalette.AccentOrange),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("checkout_bank_retry_button")
                                            ) {
                                                Text(
                                                    text = "ხელახლა ცდა",
                                                    color = CheckoutPalette.AccentOrange,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else if (enabledBanks.isNotEmpty()) {
                                    // Header + Bank cards grid grouped tightly
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccountBalance,
                                                contentDescription = null,
                                                tint = CheckoutPalette.AccentOrange,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "აირჩიეთ ბანკი",
                                                color = CheckoutPalette.PrimaryText,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        // Bank cards grid (2 columns side-by-side matching reference)
                                        val bankEntries = bankGroups.entries.toList()
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            bankEntries.chunked(2).forEach { pair ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    pair.forEach { (groupKey, accountsInGroup) ->
                                                        val primaryAcc = accountsInGroup.first()
                                                        val isGroupSelected = selectedBank != null &&
                                                            BstoreCheckoutLogic.bankGroupKey(selectedBank) == groupKey
                                                        val displayLabel = BstoreCheckoutLogic.twoLineBankLabel(
                                                            primaryAcc.bankCode,
                                                            primaryAcc.bankName
                                                        )
                                                        ReferenceSelectableOptionCard(
                                                            selected = isGroupSelected,
                                                            enabled = true,
                                                            onClick = {
                                                                selectedBankAccountId = primaryAcc.id
                                                                validationError = null
                                                            },
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .testTag("checkout_bank_option_$groupKey")
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                BankBrandLogo(
                                                                    bankCode = primaryAcc.bankCode,
                                                                    bankName = primaryAcc.bankName,
                                                                    size = 32.dp,
                                                                    circular = true
                                                                )
                                                                Text(
                                                                    text = displayLabel,
                                                                    color = CheckoutPalette.PrimaryText,
                                                                    fontSize = 12.5.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    lineHeight = 15.sp,
                                                                    maxLines = 2,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (pair.size == 1) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Multiple enabled accounts for the same bank (admin display order)
                                    if (selectedGroupAccounts.size > 1 && selectedBank != null) {
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            selectedGroupAccounts.forEachIndexed { idx, accOption ->
                                                val isAccSel = selectedBank.id == accOption.id
                                                Surface(
                                                    shape = RoundedCornerShape(9.dp),
                                                    color = if (isAccSel) CheckoutPalette.SelectedCardBg else CheckoutPalette.CardBg,
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isAccSel) CheckoutPalette.AccentOrange else CheckoutPalette.CardBorder
                                                    ),
                                                    modifier = Modifier
                                                        .heightIn(min = 32.dp)
                                                        .clickable {
                                                            selectedBankAccountId = accOption.id
                                                            validationError = null
                                                        }
                                                        .testTag("checkout_subaccount_${accOption.id}")
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    ) {
                                                        Text(
                                                            text = accOption.description.ifBlank { "ანგარიში ${idx + 1}" },
                                                            color = if (isAccSel) CheckoutPalette.PrimaryText else CheckoutPalette.MutedText,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = if (isAccSel) FontWeight.Bold else FontWeight.Medium,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Selected Bank Account Info Card + Copy + "გადარიცხვაზე გადასვლა"
                                    if (selectedBank != null) {
                                        val cleanSelectedIban = BstorePaymentManager.normalizeIban(selectedBank.iban)
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = CheckoutPalette.CardBg,
                                            border = BorderStroke(1.dp, CheckoutPalette.CardBorder),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("checkout_selected_bank_requisites_card")
                                        ) {
                                            BoxWithConstraints(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                val isCompactWidth = maxWidth < 285.dp
                                                val canInlineRecipientOnTitleRow = maxWidth >= 420.dp
                                                val tightTextStyle = TextStyle(
                                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                                )

                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    // 1. Bank identity row: Authentic bank logo + Bank name + Recipient name
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                                                    ) {
                                                        BankBrandLogo(
                                                            bankCode = selectedBank.bankCode,
                                                            bankName = selectedBank.bankName,
                                                            size = 32.dp,
                                                            circular = true
                                                        )

                                                        Column(
                                                            modifier = Modifier.weight(1f),
                                                            verticalArrangement = Arrangement.spacedBy(1.dp)
                                                        ) {
                                                            if (canInlineRecipientOnTitleRow && selectedBank.recipientName.isNotBlank()) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    Text(
                                                                        text = selectedBank.bankName,
                                                                        color = CheckoutPalette.PrimaryText,
                                                                        fontSize = 13.5.sp,
                                                                        lineHeight = 16.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        style = tightTextStyle,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Text(
                                                                        text = "• მიმღები: ${selectedBank.recipientName}",
                                                                        color = CheckoutPalette.SecondaryText,
                                                                        fontSize = 11.5.sp,
                                                                        lineHeight = 14.sp,
                                                                        style = tightTextStyle,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        modifier = Modifier.weight(1f)
                                                                    )
                                                                }
                                                            } else {
                                                                Text(
                                                                    text = selectedBank.bankName,
                                                                    color = CheckoutPalette.PrimaryText,
                                                                    fontSize = 13.5.sp,
                                                                    lineHeight = 16.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    style = tightTextStyle,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                if (selectedBank.recipientName.isNotBlank()) {
                                                                    Text(
                                                                        text = "მიმღები: ${selectedBank.recipientName}",
                                                                        color = CheckoutPalette.SecondaryText,
                                                                        fontSize = 11.5.sp,
                                                                        lineHeight = 14.sp,
                                                                        style = tightTextStyle,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }

                                                            if (selectedBank.description.isNotBlank() && selectedGroupAccounts.size > 1) {
                                                                Text(
                                                                    text = selectedBank.description,
                                                                    color = CheckoutPalette.MutedText,
                                                                    fontSize = 10.5.sp,
                                                                    lineHeight = 13.sp,
                                                                    style = tightTextStyle,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // 2. IBAN row + Copy button on the same horizontal row + compact copy confirmation
                                                    val copyButtonComposable: @Composable () -> Unit = {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = CheckoutPalette.CopyBg,
                                                            border = BorderStroke(
                                                                1.dp,
                                                                if (manualCopyPulse) CheckoutPalette.SuccessGreen else CheckoutPalette.CopyBorder
                                                            ),
                                                            modifier = Modifier
                                                                .height(27.dp)
                                                                .clickable(enabled = cleanSelectedIban.isNotBlank()) {
                                                                    val copied = BankTransferHandoffHelper.copyIbanToClipboardSilently(
                                                                        context = context,
                                                                        iban = cleanSelectedIban,
                                                                        label = "${selectedBank.bankName} IBAN"
                                                                    )
                                                                    if (copied != null) {
                                                                        checkoutIbanCopiedHint = true
                                                                        manualCopyPulse = true
                                                                        Toast.makeText(
                                                                            context,
                                                                            "ანგარიშის ნომერი დაკოპირდა",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                }
                                                                .testTag("checkout_copy_iban_button")
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (manualCopyPulse) Icons.Default.Check else Icons.Outlined.ContentCopy,
                                                                    contentDescription = "IBAN-ის კოპირება",
                                                                    tint = if (manualCopyPulse) CheckoutPalette.SuccessGreen else CheckoutPalette.SecondaryText,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Text(
                                                                    text = if (manualCopyPulse) "დაკოპირდა" else "კოპირება",
                                                                    color = if (manualCopyPulse) CheckoutPalette.SuccessGreen else CheckoutPalette.PrimaryText,
                                                                    fontSize = 11.sp,
                                                                    lineHeight = 13.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    style = tightTextStyle,
                                                                    maxLines = 1,
                                                                    softWrap = false
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "ანგარიშის ნომერი (IBAN):",
                                                                color = CheckoutPalette.MutedText,
                                                                fontSize = 10.5.sp,
                                                                lineHeight = 13.sp,
                                                                style = tightTextStyle
                                                            )
                                                            if (checkoutIbanCopiedHint) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(5.dp),
                                                                    color = CheckoutPalette.CopiedBadgeBg,
                                                                    border = BorderStroke(1.dp, CheckoutPalette.SuccessGreen.copy(alpha = 0.45f)),
                                                                    modifier = Modifier.testTag("checkout_iban_auto_copied_badge")
                                                                ) {
                                                                    Text(
                                                                        text = "✓ დაკოპირდა",
                                                                        color = CheckoutPalette.SuccessGreen,
                                                                        fontSize = 9.5.sp,
                                                                        lineHeight = 11.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        style = tightTextStyle,
                                                                        maxLines = 1,
                                                                        softWrap = false,
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        if (isCompactWidth) {
                                                            Text(
                                                                text = cleanSelectedIban,
                                                                color = CheckoutPalette.PrimaryText,
                                                                fontSize = 13.sp,
                                                                lineHeight = 16.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace,
                                                                style = tightTextStyle,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .testTag("checkout_selected_bank_iban")
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            copyButtonComposable()
                                                        } else {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Text(
                                                                    text = cleanSelectedIban,
                                                                    color = CheckoutPalette.PrimaryText,
                                                                    fontSize = 13.sp,
                                                                    lineHeight = 16.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    style = tightTextStyle,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .testTag("checkout_selected_bank_iban")
                                                                )
                                                                copyButtonComposable()
                                                            }
                                                        }
                                                    }

                                                    // 3. Direct Bank App Opening CTA ("გადარიცხვაზე გადასვლა")
                                                    val canLaunchBank = cleanSelectedIban.isNotBlank() && !isSubmitting
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (canLaunchBank) CheckoutPalette.AccentOrange else CheckoutPalette.AccentOrange.copy(alpha = 0.45f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(36.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable(enabled = canLaunchBank) {
                                                                bankHandoffErrorMessage = null
                                                                when (
                                                                    val launchResult = BankTransferHandoffHelper.launchMobileBankForTransfer(
                                                                        context = context,
                                                                        bankCode = selectedBank.bankCode,
                                                                        bankName = selectedBank.bankName,
                                                                        iban = cleanSelectedIban
                                                                    )
                                                                ) {
                                                                    is BankTransferHandoffHelper.BankLaunchResult.Launched -> {
                                                                        checkoutIbanCopiedHint = true
                                                                        manualCopyPulse = true
                                                                        bankHandoffInstructionBanner =
                                                                            "IBAN დაკოპირებულია. მობაილბანკში გადარიცხვისას ჩასვით მიმღების ანგარიშის ველში."
                                                                    }
                                                                    is BankTransferHandoffHelper.BankLaunchResult.AppNotInstalled -> {
                                                                        checkoutIbanCopiedHint = true
                                                                        manualCopyPulse = true
                                                                        bankHandoffInstructionBanner =
                                                                            "IBAN დაკოპირებულია. მობაილბანკში გადარიცხვისას ჩასვით მიმღების ანგარიშის ველში."
                                                                        missingBankAppDialogPackage = launchResult.packageName
                                                                    }
                                                                    is BankTransferHandoffHelper.BankLaunchResult.InvalidBankOrIban -> {
                                                                        bankHandoffErrorMessage = launchResult.reasonKa
                                                                    }
                                                                    is BankTransferHandoffHelper.BankLaunchResult.LaunchFailed -> {
                                                                        checkoutIbanCopiedHint = true
                                                                        bankHandoffErrorMessage = launchResult.reasonKa
                                                                    }
                                                                }
                                                            }
                                                            .testTag("checkout_open_mobile_bank_button")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .padding(horizontal = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.AccountBalance,
                                                                contentDescription = null,
                                                                tint = CheckoutPalette.CtaDarkText,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "გადარიცხვაზე გადასვლა",
                                                                color = CheckoutPalette.CtaDarkText,
                                                                fontSize = 12.5.sp,
                                                                lineHeight = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                style = tightTextStyle,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                if (bankHandoffInstructionBanner != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = CheckoutPalette.CopiedBadgeBg,
                                                        border = BorderStroke(1.dp, CheckoutPalette.SuccessGreen.copy(alpha = 0.45f)),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("checkout_bank_handoff_banner")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.CheckCircle,
                                                                contentDescription = null,
                                                                tint = CheckoutPalette.SuccessGreen,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                text = bankHandoffInstructionBanner!!,
                                                                color = CheckoutPalette.PrimaryText,
                                                                fontSize = 11.sp,
                                                                lineHeight = 14.5.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                if (bankHandoffErrorMessage != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = CheckoutPalette.ErrorBannerBg,
                                                        border = BorderStroke(1.dp, CheckoutPalette.ErrorRed.copy(alpha = 0.6f)),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("checkout_bank_handoff_error")
                                                    ) {
                                                        Text(
                                                            text = bankHandoffErrorMessage!!,
                                                            color = CheckoutPalette.ErrorBannerText,
                                                            fontSize = 11.sp,
                                                            lineHeight = 14.5.sp,
                                                            modifier = Modifier.padding(8.dp)
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

                        // ------------------------------------------------------
                        // 4. CONTACT NUMBER INPUT CARD
                        // ------------------------------------------------------
                        val storePrimaryPhone = remember(ContactAndSocialManager.configStateVersion.intValue) {
                            ContactAndSocialManager.getPrimaryPhoneNumber(context)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CheckoutPalette.CardBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (phoneFieldError != null) CheckoutPalette.ErrorRed else CheckoutPalette.CardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Phone,
                                        contentDescription = "საკონტაქტო ნომერი",
                                        tint = CheckoutPalette.SecondaryText,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Text(
                                            text = "საკონტაქტო ნომერი",
                                            color = CheckoutPalette.MutedText,
                                            fontSize = 10.5.sp
                                        )
                                        BasicTextField(
                                            value = customerPhoneInput,
                                            onValueChange = { next ->
                                                customerPhoneInput = next.filter { ch ->
                                                    ch.isDigit() || ch == '+' || ch == ' ' || ch == '-'
                                                }.take(20)
                                                phoneFieldError = null
                                                validationError = null
                                            },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = CheckoutPalette.PrimaryText,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            cursorBrush = SolidColor(CheckoutPalette.AccentOrange),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Phone,
                                                imeAction = ImeAction.Done
                                            ),
                                            decorationBox = { innerTextField ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 18.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    if (customerPhoneInput.isEmpty()) {
                                                        Text(
                                                            text = "+995 5XX XX XX XX",
                                                            color = if (CheckoutPalette.isDark) Color(0xFF6B7280) else Color(0xFF9CA3AF),
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("checkout_phone_input")
                                        )
                                    }
                                    if (storePrimaryPhone.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = CheckoutPalette.CopyBg,
                                            border = BorderStroke(1.dp, CheckoutPalette.CopyBorder),
                                            modifier = Modifier
                                                .clickable {
                                                    runCatching {
                                                        val dialIntent = Intent(
                                                            Intent.ACTION_DIAL,
                                                            ContactAndSocialManager.normalizeDialUri(storePrimaryPhone)
                                                        ).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        context.startActivity(dialIntent)
                                                    }
                                                }
                                                .testTag("checkout_call_store_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Phone,
                                                    contentDescription = "BSTORE-თან დაკავშირება",
                                                    tint = CheckoutPalette.AccentOrange,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "დახმარება",
                                                    color = CheckoutPalette.SecondaryText,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (phoneFieldError != null) {
                                Text(
                                    text = phoneFieldError!!,
                                    color = CheckoutPalette.ErrorRed,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .testTag("checkout_phone_error")
                                )
                            }
                        }

                        // ------------------------------------------------------
                        // 5. PAYMENT INSTRUCTIONS CARD
                        // ------------------------------------------------------
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CheckoutPalette.CardBg,
                            border = BorderStroke(1.dp, CheckoutPalette.CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_payment_instructions_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = CheckoutPalette.InfoBlue,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(18.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = "გადახდის ინსტრუქცია:",
                                        color = CheckoutPalette.PrimaryText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (selectedPaymentMethod == PaymentMethodType.BANK_TRANSFER && selectedBank != null) {
                                            BstoreCheckoutLogic.resolveReferenceInstructionText(selectedBank)
                                        } else {
                                            "შეკვეთის ღირებულებას ($totalFormatted) გადაიხდით ნაღდი ანგარიშსწორებით ნივთის ჩაბარებისას ან ფილიალიდან გატანისას."
                                        },
                                        color = CheckoutPalette.SecondaryText,
                                        fontSize = 11.sp,
                                        lineHeight = 14.5.sp
                                    )
                                }
                            }
                        }

                        // ------------------------------------------------------
                        // 6. TERMS & CONDITIONS CHECKBOX ROW
                        // ------------------------------------------------------
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 30.dp)
                                .padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable {
                                        acceptedTerms = !acceptedTerms
                                        validationError = null
                                    }
                                    .testTag("checkout_accept_terms_checkbox")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(19.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (acceptedTerms) CheckoutPalette.AccentOrange else Color.Transparent
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (acceptedTerms) CheckoutPalette.AccentOrange else CheckoutPalette.MutedText,
                                            shape = RoundedCornerShape(5.dp)
                                        )
                                ) {
                                    if (acceptedTerms) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "პირობებზე თანხმობა",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            val termsAnnotated = buildAnnotatedString {
                                withStyle(SpanStyle(color = CheckoutPalette.SecondaryText)) {
                                    append("ვეთანხმები BSTORE-ის ")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = CheckoutPalette.AccentOrange,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append("წესებსა და პირობებს")
                                }
                            }

                            androidx.compose.material3.Text(
                                text = termsAnnotated,
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        activeLegalPolicyTopic = LegalPolicyTopic.TERMS_AND_CONDITIONS
                                        onOpenLegalPolicy(LegalPolicyTopic.TERMS_AND_CONDITIONS)
                                    }
                                    .testTag("checkout_open_terms_link")
                            )
                        }

                        if (validationError != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CheckoutPalette.ErrorBannerBg,
                                border = BorderStroke(1.dp, CheckoutPalette.ErrorRed),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_validation_error")
                            ) {
                                Text(
                                    text = validationError!!,
                                    color = CheckoutPalette.ErrorBannerText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // ==========================================================
                        // 7. ORDER CONFIRMATION BUTTON ("შეკვეთის დადასტურება")
                        // Naturally associated with the Terms & Conditions section
                        // with zero artificial empty gap above it.
                        // ==========================================================
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("checkout_bottom_action_bar")
                        ) {
                            Button(
                                onClick = {
                                    if (isSubmitting) return@Button
                                    if (editableCartItems.isEmpty()) {
                                        validationError = "კალათა ცარიელია"
                                        return@Button
                                    }
                                    val phoneRes = BstoreCheckoutLogic.validateAndNormalizeGeorgianPhone(customerPhoneInput)
                                    if (!phoneRes.isValid) {
                                        phoneFieldError = phoneRes.errorMessageKa
                                        validationError = phoneRes.errorMessageKa
                                        return@Button
                                    }
                                    if (selectedDeliveryMethod == DeliveryMethodType.COURIER &&
                                        (deliveryCityInput.trim().isBlank() || deliveryAddressInput.trim().isBlank())
                                    ) {
                                        validationError = "საკურიერო მიწოდებისთვის მიუთითეთ ქალაქი და ზუსტი მისამართი"
                                        return@Button
                                    }
                                    if (selectedPaymentMethod == PaymentMethodType.BANK_TRANSFER &&
                                        (selectedBank == null || !selectedBank.isEnabled || !BstorePaymentManager.isValidIban(selectedBank.iban))
                                    ) {
                                        validationError = "საბანკო გადარიცხვისთვის აირჩიეთ აქტიური ბანკი ან ნაღდი ანგარიშსწორება"
                                        return@Button
                                    }
                                    if (!acceptedTerms) {
                                        validationError = "შეკვეთის გასაფორმებლად დაეთანხმეთ BSTORE-ის წესებსა და პირობებს"
                                        return@Button
                                    }

                                    customerPhoneInput = phoneRes.formattedPhone
                                    isSubmitting = true
                                    validationError = null
                                    phoneFieldError = null

                                    val resolvedAddress = if (selectedDeliveryMethod == DeliveryMethodType.PICKUP) {
                                        "ქ. ახალციხე, კოსტავას N10 (ფილიალიდან გატანა)"
                                    } else {
                                        "${deliveryCityInput.trim()}, ${deliveryAddressInput.trim()}"
                                    }

                                    val config = CheckoutSubmissionConfig(
                                        paymentMethod = selectedPaymentMethod,
                                        selectedBankAccount = if (selectedPaymentMethod == PaymentMethodType.BANK_TRANSFER) selectedBank else null,
                                        deliveryMethod = selectedDeliveryMethod,
                                        deliveryCity = if (selectedDeliveryMethod == DeliveryMethodType.PICKUP) "ახალციხე" else deliveryCityInput.trim(),
                                        deliveryAddress = resolvedAddress,
                                        customerPhone = phoneRes.formattedPhone,
                                        customerNote = "",
                                        acceptedTermsVersion = BstorePaymentManager.CURRENT_TERMS_VERSION,
                                        acceptedTermsAtMillis = System.currentTimeMillis(),
                                        clientOrderId = submissionOrderId
                                    )

                                    val submittedItems = editableCartItems.toList()
                                    coroutineScope.launch {
                                        val result = AuthAndOrderManager.createAndPersistOrderFromCart(
                                            context = context,
                                            cartItems = submittedItems,
                                            checkoutConfig = config
                                        )
                                        isSubmitting = false
                                        if (result.isSuccess) {
                                            onOrderCreated(result.getOrThrow())
                                        } else {
                                            validationError = result.exceptionOrNull()?.localizedMessage
                                                ?: "შეკვეთის შექმნა ვერ მოხერხდა"
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CheckoutPalette.AccentOrange,
                                    contentColor = CheckoutPalette.CtaDarkText
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("checkout_confirm_order_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        color = CheckoutPalette.CtaDarkText,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "იგზავნება...",
                                        color = CheckoutPalette.CtaDarkText,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "შეკვეთის დადასტურება",
                                        color = CheckoutPalette.CtaDarkText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
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
        }
    }

    // Google Play / Missing Bank App Fallback Dialog
    if (missingBankAppDialogPackage != null && selectedBank != null) {
        val targetPackage = missingBankAppDialogPackage!!
        val bankColor = selectedBank.brandColor()
        Dialog(onDismissRequest = { missingBankAppDialogPackage = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CheckoutPalette.CardBg,
                border = BorderStroke(1.dp, bankColor.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bank_app_not_installed_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BankBrandLogo(
                        bankCode = selectedBank.bankCode,
                        bankName = selectedBank.bankName,
                        size = 48.dp,
                        circular = true
                    )
                    Text(
                        text = "მობაილბანკი არ არის დაყენებული",
                        color = CheckoutPalette.PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${selectedBank.bankName.ifBlank { "ბანკის" }} მობაილბანკის აპლიკაცია ამ მოწყობილობაზე ვერ მოიძებნა. IBAN უკვე დაკოპირებულია — შეგიძლიათ გახსნათ Google Play ან გამოიყენოთ დაკოპირებული ანგარიშის ნომერი.",
                        color = CheckoutPalette.SecondaryText,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            val opened = BankTransferHandoffHelper.openBankInPlayStore(context, targetPackage)
                            missingBankAppDialogPackage = null
                            if (!opened) {
                                Toast.makeText(
                                    context,
                                    "Google Play Store-ის გახსნა ვერ მოხერხდა",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CheckoutPalette.AccentOrange),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_bank_play_store_button")
                    ) {
                        Text(
                            text = "Google Play-ში გახსნა",
                            color = CheckoutPalette.CtaDarkText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { missingBankAppDialogPackage = null },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CheckoutPalette.CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dismiss_bank_app_not_installed_button")
                    ) {
                        Text(
                            text = "დახურვა",
                            color = CheckoutPalette.MutedText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (activeLegalPolicyTopic != null) {
        BstoreLegalPoliciesDialog(
            initialTopic = activeLegalPolicyTopic!!,
            onDismiss = { activeLegalPolicyTopic = null }
        )
    }
}

@Composable
private fun ReferenceSelectableOptionCard(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when {
                !enabled -> CheckoutPalette.DisabledCardBg
                selected -> CheckoutPalette.SelectedCardBg
                else -> CheckoutPalette.CardBg
            },
            border = BorderStroke(
                width = if (selected && enabled) 1.5.dp else 1.dp,
                color = when {
                    !enabled -> CheckoutPalette.CardBorder.copy(alpha = 0.55f)
                    selected -> CheckoutPalette.AccentOrange
                    else -> CheckoutPalette.CardBorder
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                content()
            }
        }

        if (selected && enabled) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 5.dp, end = 5.dp)
                    .zIndex(1f)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(CheckoutPalette.AccentOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "არჩეული",
                    tint = Color.White,
                    modifier = Modifier.size(10.5.dp)
                )
            }
        }
    }
}
