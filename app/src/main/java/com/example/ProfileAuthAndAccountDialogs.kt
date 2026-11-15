package com.example

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AuthCard(
    onGoogleClick: () -> Unit,
    onLoginSuccess: (UserProfile) -> Unit,
    onForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    var isRegisterMode by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    BstoreCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_auth_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isRegisterMode) "ანგარიშის შექმნა" else "ავტორიზაცია BSTORE-ში",
                color = p.textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold
            )

            BstoreSegmentedSelector(
                options = listOf(
                    BstoreSegmentOption(false, "შესვლა", testTag = "auth_tab_login"),
                    BstoreSegmentOption(true, "რეგისტრაცია", testTag = "auth_tab_register")
                ),
                selectedValue = isRegisterMode,
                onSelect = {
                    isRegisterMode = it
                    errorMsg = null
                },
                isDark = isDark
            )

            // Google Auth Button (Real Jetpack Credential Manager)
            Button(
                onClick = onGoogleClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F2937)
                ),
                border = if (!isDark) BorderStroke(1.dp, p.borderStrong) else null,
                shape = RoundedCornerShape(BstoreRadii.Control),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("auth_google_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Google-ით გაგრძელება",
                        color = Color(0xFF1F2937),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = p.borderSubtle)
                Text(
                    text = "ან ელ. ფოსტით",
                    color = p.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = p.borderSubtle)
            }

            if (isRegisterMode) {
                BstoreOutlinedInput(
                    value = nameInput,
                    onValueChange = { nameInput = it; errorMsg = null },
                    label = "თქვენი სახელი და გვარი",
                    isDark = isDark,
                    leadingIcon = Icons.Default.Person,
                    modifier = Modifier.testTag("auth_name_input")
                )
            }

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it; errorMsg = null },
                label = { Text("ელ. ფოსტა") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(BstoreRadii.Control),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = p.textPrimary,
                    unfocusedTextColor = p.textPrimary,
                    focusedContainerColor = p.subSurfaceBg,
                    unfocusedContainerColor = p.subSurfaceBg,
                    focusedBorderColor = p.brandOrange,
                    unfocusedBorderColor = p.borderStrong
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input")
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
                            contentDescription = "პაროლის ჩვენება",
                            tint = p.textSecondary
                        )
                    }
                },
                shape = RoundedCornerShape(BstoreRadii.Control),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = p.textPrimary,
                    unfocusedTextColor = p.textPrimary,
                    focusedContainerColor = p.subSurfaceBg,
                    unfocusedContainerColor = p.subSurfaceBg,
                    focusedBorderColor = p.brandOrange,
                    unfocusedBorderColor = p.borderStrong
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input")
            )

            if (!isRegisterMode) {
                Text(
                    text = "დაგავიწყდათ პაროლი?",
                    color = p.brandOrange,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onForgotPassword() }
                        .align(Alignment.End)
                        .testTag("auth_forgot_password_button")
                )
            }

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = p.errorRed,
                    fontSize = 12.sp
                )
            }

            BstorePrimaryButton(
                text = if (isRegisterMode) "რეგისტრაცია" else "შესვლა",
                onClick = {
                    if (isRegisterMode) {
                        val (success, msg) = AuthAndOrderManager.registerWithEmail(
                            context, nameInput, emailInput, passwordInput
                        )
                        if (success) {
                            AuthAndOrderManager.getCurrentUser(context)?.let(onLoginSuccess)
                        } else {
                            errorMsg = msg
                        }
                    } else {
                        val (success, msg) = AuthAndOrderManager.loginWithEmail(
                            context, emailInput, passwordInput
                        )
                        if (success) {
                            AuthAndOrderManager.getCurrentUser(context)?.let(onLoginSuccess)
                        } else {
                            errorMsg = msg
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_submit_button")
            )
        }
    }
}

@Composable
fun EditProfileDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSaved: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    var avatarUrl by remember { mutableStateOf(user.avatarUrl) }
    var firstName by remember {
        mutableStateOf(user.firstName.ifBlank { user.name.split(" ").firstOrNull().orEmpty() })
    }
    var lastName by remember {
        mutableStateOf(user.lastName.ifBlank { user.name.split(" ").drop(1).joinToString(" ") })
    }
    var phone by remember { mutableStateOf(user.phone) }
    var address by remember { mutableStateOf(user.address) }
    var deviceModel by remember { mutableStateOf(user.deviceModel) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUrl = uri.toString()
            Toast.makeText(context, "ფოტო არჩეულია გალერეიდან", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(BstoreRadii.Dialog),
            color = p.cardBg,
            border = BorderStroke(1.dp, p.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("edit_profile_dialog")
        ) {
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "პროფილის რედაქტირება",
                            color = p.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { if (!isSaving) onDismiss() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = p.textSecondary)
                        }
                    }
                }

                // Avatar selection via gallery or clear (zero fake people stock photos)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(p.subSurfaceBg)
                                .border(2.dp, p.brandOrange, CircleShape)
                        ) {
                            if (avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "პროფილის ფოტო",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initLetter = (firstName.firstOrNull() ?: user.name.firstOrNull())?.uppercaseChar()?.toString()
                                if (!initLetter.isNullOrBlank()) {
                                    Text(
                                        text = initLetter,
                                        color = p.textPrimary,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = p.brandOrange,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BstorePrimaryButton(
                                text = "გალერეიდან არჩევა",
                                leadingIcon = Icons.Default.PhotoLibrary,
                                size = BstoreButtonSize.COMPACT,
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (avatarUrl.isNotBlank()) {
                                BstoreOutlinedButton(
                                    text = "წაშლა",
                                    leadingIcon = Icons.Default.DeleteOutline,
                                    isDark = isDark,
                                    accentColor = p.errorRed,
                                    size = BstoreButtonSize.COMPACT,
                                    onClick = { avatarUrl = "" }
                                )
                            }
                        }
                    }
                }

                item {
                    BstoreOutlinedInput(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        label = "ფოტოს ბმული (სურვილისამებრ)",
                        placeholder = "https://...",
                        isDark = isDark
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BstoreOutlinedInput(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = "სახელი",
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_first_name_input")
                        )
                        BstoreOutlinedInput(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = "გვარი",
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_last_name_input")
                        )
                    }
                }

                item {
                    BstoreOutlinedInput(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "საკონტაქტო ტელეფონი",
                        isDark = isDark,
                        modifier = Modifier.testTag("edit_profile_phone_input")
                    )
                }

                item {
                    BstoreOutlinedInput(
                        value = address,
                        onValueChange = { address = it },
                        label = "მისამართი (მიწოდება)",
                        isDark = isDark,
                        modifier = Modifier.testTag("edit_profile_address_input")
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BstoreOutlinedInput(
                            value = deviceModel,
                            onValueChange = { deviceModel = it },
                            label = "ტელეფონის მოდელი",
                            isDark = isDark,
                            modifier = Modifier.testTag("edit_profile_device_input")
                        )
                        val quickModels = listOf("iPhone 16 Pro Max", "iPhone 15 Pro", "Samsung S24 Ultra", "Pixel 8 Pro")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(quickModels) { modelName ->
                                BstoreFilterChip(
                                    text = modelName,
                                    selected = deviceModel == modelName,
                                    isDark = isDark,
                                    onClick = { deviceModel = modelName }
                                )
                            }
                        }
                    }
                }

                if (saveError != null) {
                    item {
                        Text(
                            text = saveError!!,
                            color = p.errorRed,
                            fontSize = 11.5.sp
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BstoreOutlinedButton(
                            text = "გაუქმება",
                            onClick = { if (!isSaving) onDismiss() },
                            enabled = !isSaving,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                        BstorePrimaryButton(
                            text = "შენახვა",
                            isLoading = isSaving,
                            onClick = {
                                if (isSaving) return@BstorePrimaryButton
                                coroutineScope.launch {
                                    isSaving = true
                                    saveError = null
                                    val cleanFirst = firstName.trim()
                                    val cleanLast = lastName.trim()
                                    val combinedName = "$cleanFirst $cleanLast".trim()
                                    val updated = user.copy(
                                        avatarUrl = avatarUrl.trim(),
                                        firstName = cleanFirst,
                                        lastName = cleanLast,
                                        name = combinedName,
                                        phone = phone.trim(),
                                        address = address.trim(),
                                        deviceModel = deviceModel.trim()
                                    )
                                    val result = AuthAndOrderManager.saveUserProfileToCloud(context, updated)
                                    isSaving = false
                                    if (result.isSuccess) {
                                        onSaved(result.getOrNull() ?: updated)
                                    } else {
                                        saveError = result.exceptionOrNull()?.localizedMessage
                                            ?: "პროფილის შენახვა ვერ მოხერხდა"
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("edit_profile_save_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickContactFieldDialog(
    mode: String, // "phone" | "address"
    initialValue: String,
    onDismiss: () -> Unit,
    onSaved: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    val isPhone = mode == "phone"
    var inputValue by remember { mutableStateOf(initialValue) }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = p.cardBg,
            border = BorderStroke(1.dp, p.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("quick_contact_field_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isPhone) "ტელეფონის ნომერი" else "მიწოდების მისამართი",
                    color = p.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = inputValue,
                    onValueChange = {
                        inputValue = it
                        errorText = null
                    },
                    label = { Text(if (isPhone) "საკონტაქტო ტელეფონი" else "ქალაქი, ქუჩა, ნომერი") },
                    placeholder = { Text(if (isPhone) "+995 5XX XX XX XX" else "მაგ: ქ. ახალციხე, კოსტავას 10") },
                    singleLine = true,
                    keyboardOptions = if (isPhone) KeyboardOptions(keyboardType = KeyboardType.Phone) else KeyboardOptions.Default,
                    shape = RoundedCornerShape(BstoreRadii.Control),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = p.textPrimary,
                        unfocusedTextColor = p.textPrimary,
                        focusedContainerColor = p.subSurfaceBg,
                        unfocusedContainerColor = p.subSurfaceBg,
                        focusedBorderColor = p.brandOrange,
                        unfocusedBorderColor = p.borderStrong
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_contact_field_input")
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = p.errorRed,
                        fontSize = 11.5.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BstoreOutlinedButton(
                        text = "გაუქმება",
                        onClick = { if (!isSaving) onDismiss() },
                        enabled = !isSaving,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )

                    BstorePrimaryButton(
                        text = "შენახვა",
                        isLoading = isSaving,
                        onClick = {
                            if (isSaving) return@BstorePrimaryButton
                            val cleanVal = inputValue.trim()
                            if (isPhone && cleanVal.isNotBlank() && !ContactAndSocialManager.isValidPhoneNumber(cleanVal)) {
                                errorText = "მიუთითეთ ვალიდური ტელეფონის ნომერი"
                                return@BstorePrimaryButton
                            }
                            coroutineScope.launch {
                                isSaving = true
                                errorText = null
                                val res = if (isPhone) {
                                    AuthAndOrderManager.updateSingleContactField(context, phone = cleanVal)
                                } else {
                                    AuthAndOrderManager.updateSingleContactField(context, address = cleanVal)
                                }
                                isSaving = false
                                if (res.isSuccess) {
                                    onSaved(res.getOrNull() ?: AuthAndOrderManager.getCurrentUser(context)!!)
                                } else {
                                    errorText = res.exceptionOrNull()?.localizedMessage ?: "შენახვა ვერ მოხერხდა"
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_contact_field_save_button")
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalDataDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onOpenEditProfile: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(BstoreRadii.Dialog),
            color = p.cardBg,
            border = BorderStroke(1.dp, p.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("personal_data_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "პირადი მონაცემები",
                        color = p.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = p.textSecondary)
                    }
                }

                PersonalDataItemRow("სახელი და გვარი", user.displayName.ifBlank { "არ არის მითითებული" }, isDark)
                PersonalDataItemRow("ელ. ფოსტა", user.email.ifBlank { "არ არის მითითებული" }, isDark)
                PersonalDataItemRow("ტელეფონი", user.phone.ifBlank { "არ არის მითითებული" }, isDark)
                PersonalDataItemRow("მისამართი", user.address.ifBlank { "არ არის მითითებული" }, isDark)
                PersonalDataItemRow("მოწყობილობა", user.deviceModel.ifBlank { "არ არის მითითებული" }, isDark)
                PersonalDataItemRow(
                    "ავტორიზაციის ტიპი",
                    when {
                        user.provider.equals("Google", ignoreCase = true) -> "Google"
                        user.provider.equals("Apple", ignoreCase = true) -> "Apple"
                        else -> "Email / პაროლი"
                    },
                    isDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BstoreOutlinedButton(
                        text = "დახურვა",
                        onClick = onDismiss,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    BstorePrimaryButton(
                        text = "რედაქტირება",
                        leadingIcon = Icons.Default.Edit,
                        onClick = {
                            onDismiss()
                            onOpenEditProfile()
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalDataItemRow(label: String, value: String, isDark: Boolean) {
    val p = bstorePalette(isDark)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = p.subSurfaceBg,
        border = BorderStroke(0.6.dp, p.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(text = label, color = p.textMuted, fontSize = 10.5.sp)
            Text(text = value, color = p.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SecurityAndSettingsDialog(
    user: UserProfile?,
    onDismiss: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onOpenDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val isDark = AppPreferencesManager.isDarkMode
    val currentLang = AppPreferencesManager.currentLanguage
    val p = bstorePalette(isDark)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(BstoreRadii.Dialog),
            color = p.cardBg,
            border = BorderStroke(1.dp, p.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("security_settings_dialog")
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
                    Text(
                        text = "უსაფრთხოება და პარამეტრები",
                        color = p.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = p.textSecondary)
                    }
                }

                // Theme Switch (Real AppPreferencesManager)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = p.subSurfaceBg,
                    border = BorderStroke(0.8.dp, p.borderSubtle),
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
                                text = "მუქი თემა (Dark Mode)",
                                color = p.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDark) "ჩართულია მუქი გრაფიტის ფონი" else "ჩართულია ნათელი რეჟიმი",
                                color = p.textMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isDark,
                            onCheckedChange = { AppPreferencesManager.setDarkMode(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = p.brandOrange
                            ),
                            modifier = Modifier.testTag("settings_dark_mode_switch")
                        )
                    }
                }

                // Language Selector (Real AppPreferencesManager)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "აპლიკაციის ენა",
                        color = p.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { lang ->
                            BstoreFilterChip(
                                text = "${lang.flag} ${lang.shortLabel}",
                                selected = currentLang == lang,
                                isDark = isDark,
                                onClick = { AppPreferencesManager.setLanguage(context, lang) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_lang_${lang.code}")
                            )
                        }
                    }
                }

                if (user != null) {
                    HorizontalDivider(color = p.borderSubtle)
                    BstoreSecondaryButton(
                        text = "პაროლის შეცვლა / უსაფრთხოება",
                        leadingIcon = Icons.Default.Lock,
                        isDark = isDark,
                        onClick = {
                            onDismiss()
                            onOpenChangePassword()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_change_password_row")
                    )

                    BstoreDestructiveButton(
                        text = "ანგარიშის წაშლა",
                        leadingIcon = Icons.Default.DeleteForever,
                        isDark = isDark,
                        onClick = {
                            onDismiss()
                            onOpenDeleteAccount()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                BstorePrimaryButton(
                    text = "დახურვა",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ChangeUserPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    val user = remember { AuthAndOrderManager.getCurrentUser(context) }
    val supportsPasswordChange = remember(user?.id, user?.provider) {
        AuthAndOrderManager.supportsPasswordChangeForCurrentUser(context)
    }

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(BstoreRadii.Dialog),
            color = p.cardBg,
            border = BorderStroke(1.dp, p.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("change_password_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "პაროლის შეცვლა",
                    color = p.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                if (!supportsPasswordChange) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = p.brandOrangeSoft,
                        border = BorderStroke(0.9.dp, p.brandOrangeBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_password_provider_notice")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "თქვენი ანგარიში დაკავშირებულია ${user?.provider ?: "Google"} ავტორიზაციასთან.",
                                color = p.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "პაროლისა და უსაფრთხოების შეცვლა ხდება უშუალოდ თქვენი ${user?.provider ?: "Google"} ანგარიშის პარამეტრებიდან.",
                                color = p.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    BstorePrimaryButton(
                        text = "გასაგებია",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it; err = null },
                        label = { Text("მიმდინარე პაროლი") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            focusedBorderColor = p.brandOrange,
                            unfocusedBorderColor = p.borderStrong
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_password_old_input")
                    )

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; err = null },
                        label = { Text("ახალი პაროლი (მინ. 6 სიმბოლო)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            focusedBorderColor = p.brandOrange,
                            unfocusedBorderColor = p.borderStrong
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_password_new_input")
                    )

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it; err = null },
                        label = { Text("გაიმეორეთ ახალი პაროლი") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            focusedBorderColor = p.brandOrange,
                            unfocusedBorderColor = p.borderStrong
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_password_confirm_input")
                    )

                    if (err != null) {
                        Text(err!!, color = p.errorRed, fontSize = 11.5.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BstoreOutlinedButton(
                            text = "გაუქმება",
                            onClick = { if (!isSubmitting) onDismiss() },
                            enabled = !isSubmitting,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )

                        BstorePrimaryButton(
                            text = "შენახვა",
                            isLoading = isSubmitting,
                            onClick = {
                                if (isSubmitting) return@BstorePrimaryButton
                                if (newPass != confirmPass) {
                                    err = "ახალი პაროლები არ ემთხვევა ერთმანეთს"
                                } else {
                                    coroutineScope.launch {
                                        isSubmitting = true
                                        val (ok, msg) = AuthAndOrderManager.changePasswordWithFirebaseIfActive(
                                            context, oldPass, newPass
                                        )
                                        isSubmitting = false
                                        if (ok) onSuccess() else err = msg
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_password_save_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    var resetEmail by remember { mutableStateOf("") }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isSending) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(BstoreRadii.Dialog),
            color = p.cardBg,
            border = BorderStroke(1.dp, p.borderStrong),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "პაროლის აღდგენა",
                    color = p.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "მიუთითეთ თქვენი ელ. ფოსტა აღდგენის ბმულის მისაღებად:",
                    color = p.textSecondary,
                    fontSize = 12.sp
                )

                BstoreOutlinedInput(
                    value = resetEmail,
                    onValueChange = { resetEmail = it; feedbackMsg = null },
                    label = "ელ. ფოსტა",
                    isDark = isDark
                )

                if (feedbackMsg != null) {
                    Text(
                        text = feedbackMsg!!,
                        color = if (isError) p.errorRed else p.successGreen,
                        fontSize = 11.5.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BstoreOutlinedButton(
                        text = "გაუქმება",
                        onClick = { if (!isSending) onDismiss() },
                        enabled = !isSending,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    BstorePrimaryButton(
                        text = "გაგზავნა",
                        isLoading = isSending,
                        onClick = {
                            if (isSending) return@BstorePrimaryButton
                            coroutineScope.launch {
                                isSending = true
                                val (ok, msg) = AuthAndOrderManager.sendPasswordResetEmail(resetEmail)
                                isSending = false
                                isError = !ok
                                feedbackMsg = msg
                                if (ok) {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailModal(
    order: OrderRecord,
    onClose: () -> Unit,
    onOpenPaymentInstructions: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    val statusColor = when (order.status) {
        "შესრულებული", "დასრულებული" -> p.successGreen
        "მიმდინარე", "მზადდება", "მუშავდება", "დამუშავების პროცესში" -> p.brandOrange
        "გზაშია", "ახალი" -> p.infoBlue
        "გაუქმებული", "გაუქმება" -> p.errorRed
        else -> p.brandOrange
    }
    val paymentMethodType = PaymentMethodType.fromCode(order.paymentMethodCode, order.paymentMethod)
    val paymentStatusEnum = PaymentStatus.fromCode(order.paymentStatus, paymentMethodType)
    val paymentBadgeColor = Color(paymentStatusEnum.badgeColorHex)
    val deliveryType = DeliveryMethodType.fromCode(order.deliveryMethodCode)
    val canOpenPaymentInstructions = paymentMethodType == PaymentMethodType.BANK_TRANSFER &&
        paymentStatusEnum != PaymentStatus.PAID &&
        paymentStatusEnum != PaymentStatus.CANCELLED &&
        order.status != "გაუქმებული" &&
        onOpenPaymentInstructions != null

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(BstoreRadii.Dialog),
            color = p.cardBg,
            border = BorderStroke(1.dp, p.borderStrong),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("order_detail_modal_${order.id}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.orderNumber.ifBlank { order.id },
                            color = p.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = order.date,
                            color = p.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = p.textSecondary)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OrderProgressStepper(order = order)

                    Surface(shape = RoundedCornerShape(10.dp), color = p.subSurfaceBg) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetailKeyValRow("შეკვეთის სტატუსი:", order.status, statusColor, p.textSecondary)
                            DetailKeyValRow("გადახდის სტატუსი:", paymentStatusEnum.customerLabelKa, paymentBadgeColor, p.textSecondary)
                            DetailKeyValRow("გადახდის მეთოდი:", order.paymentMethod, p.textPrimary, p.textSecondary)
                            if (order.bankName.isNotBlank()) {
                                DetailKeyValRow("ბანკი:", order.bankName, p.textPrimary, p.textSecondary)
                            }
                            DetailKeyValRow(
                                "დანიშნულება / კოდი:",
                                order.paymentReference.ifBlank { order.trackingCode },
                                p.infoBlue,
                                p.textSecondary
                            )
                        }
                    }

                    Surface(shape = RoundedCornerShape(10.dp), color = p.subSurfaceBg) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("მიწოდება & კონტაქტი", color = p.brandOrange, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Text("მეთოდი: ${deliveryType.titleKa}", color = p.textPrimary, fontSize = 11.sp)
                            if (order.deliveryFeeText.isNotBlank()) {
                                Text("მიწოდების საფასური: ${order.deliveryFeeText}", color = p.textSecondary, fontSize = 11.sp)
                            }
                            if (order.deliveryCity.isNotBlank() || order.deliveryAddress.isNotBlank()) {
                                Text(
                                    text = "მისამართი: ${listOf(order.deliveryCity, order.deliveryAddress).filter { it.isNotBlank() }.joinToString(", ")}",
                                    color = p.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            if (order.userPhone.isNotBlank()) {
                                Text("ტელეფონი: ${order.userPhone}", color = p.textSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Text(
                        text = "შეკვეთის შემადგენლობა:",
                        color = p.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(p.subSurfaceBg)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(p.cardElevatedBg)
                                ) {
                                    if (item.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = item.productTitle,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = p.brandOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productTitle,
                                        color = p.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ხარისხი: ${item.quality} • რაოდ: ${item.quantity}",
                                        color = p.textSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = item.price,
                                    color = p.brandOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = p.borderSubtle)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ჯამური თანხა:", color = p.textSecondary, fontSize = 12.5.sp)
                        Text(order.totalAmount, color = p.brandOrange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (canOpenPaymentInstructions) {
                    BstoreOutlinedButton(
                        text = "გადახდის ინსტრუქცია & IBAN",
                        leadingIcon = Icons.Default.AccountBalance,
                        isDark = isDark,
                        accentColor = p.brandOrange,
                        onClick = { onOpenPaymentInstructions?.invoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("order_detail_open_payment_instructions")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onDelete != null) {
                        BstoreDestructiveButton(
                            text = "წაშლა",
                            leadingIcon = Icons.Default.Delete,
                            isDark = isDark,
                            onClick = onDelete,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    BstorePrimaryButton(
                        text = "დახურვა",
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailKeyValRow(label: String, value: String, valColor: Color, labelColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = labelColor, fontSize = 11.sp)
        Text(value, color = valColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DeleteAccountConfirmationDialog(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = p.cardBg,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "გაფრთხილება",
                tint = p.errorRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "ანგარიშისა და მონაცემების წაშლა",
                color = p.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Text(
                text = "ეს მოქმედება შეუქცევადია. წაიშლება თქვენი პროფილის მონაცემები, რჩეულები, ტოკენები და Firebase-ის ანგარიში.",
                color = p.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            BstoreDestructiveButton(
                text = if (isDeleting) "იშლება..." else "საბოლოოდ წაშლა",
                filled = true,
                enabled = !isDeleting,
                isDark = isDark,
                onClick = onConfirmDelete,
                modifier = Modifier.testTag("delete_account_confirm_button")
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                modifier = Modifier.testTag("delete_account_cancel_button")
            ) {
                Text("გაუქმება", color = p.textSecondary)
            }
        },
        modifier = Modifier.testTag("delete_account_dialog")
    )
}
