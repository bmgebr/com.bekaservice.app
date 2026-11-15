package com.example

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.provider.Settings
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.BstoreAmbientGlassBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val TOTAL_ONBOARDING_STEPS = 4

internal enum class OnboardingIntroVisualStage(
    val id: String,
    val drawableRes: Int,
    val contentDescription: String,
    val isBrandLogo: Boolean,
    val enterDurationMs: Int,
    val holdDurationMs: Long
) {
    BRAND_INTRO(
        id = "brand_intro",
        drawableRes = R.drawable.ic_bstore_brand_icon,
        contentDescription = "BSTORE ლოგო",
        isBrandLogo = true,
        enterDurationMs = 700,
        holdDurationMs = 1350L
    ),
    SMARTPHONE(
        id = "smartphone",
        drawableRes = R.drawable.onboarding_intro_smartphone,
        contentDescription = "BSTORE სმარტფონი",
        isBrandLogo = false,
        enterDurationMs = 600,
        holdDurationMs = 1550L
    ),
    AIRPODS(
        id = "airpods",
        drawableRes = R.drawable.onboarding_intro_airpods,
        contentDescription = "BSTORE უსადენო ყურსასმენები",
        isBrandLogo = false,
        enterDurationMs = 550,
        holdDurationMs = 1550L
    ),
    HEADPHONES(
        id = "headphones",
        drawableRes = R.drawable.onboarding_intro_headphones,
        contentDescription = "BSTORE ყურსასმენები",
        isBrandLogo = false,
        enterDurationMs = 550,
        holdDurationMs = 1550L
    ),
    SMARTWATCH(
        id = "smartwatch",
        drawableRes = R.drawable.onboarding_intro_smartwatch,
        contentDescription = "BSTORE სმარტ საათი",
        isBrandLogo = false,
        enterDurationMs = 550,
        holdDurationMs = 1550L
    ),
    BRAND_RETURN(
        id = "brand_return",
        drawableRes = R.drawable.ic_bstore_brand_icon,
        contentDescription = "BSTORE ლოგო",
        isBrandLogo = true,
        enterDurationMs = 700,
        holdDurationMs = 2600L
    )
}

/**
 * BSTORE First-Launch Premium Onboarding Flow (4 Steps).
 *
 * Steps:
 *  0 (1/4) -> Welcome screen ("მოგესალმები!") with animated BSTORE Brand/Product showcase
 *  1 (2/4) -> Language selection ("აირჩიეთ ენა" — ქართული, English, Русский)
 *  2 (3/4) -> Optional Authentication or Guest Access ("შესვლა ანგარიშით")
 *  3 (4/4) -> Final Step: Theme Selection ("აირჩიე აპის თემა" / "მთავარ გვერდზე გადასვლა")
 */
@Composable
fun BekaOnboardingFlow(
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val activeStep = currentStep.coerceIn(0, TOTAL_ONBOARDING_STEPS - 1)
    if (currentStep != activeStep) {
        currentStep = activeStep
    }
    var showAppleAuthForm by rememberSaveable { mutableStateOf(false) }
    var appleEmailInput by rememberSaveable { mutableStateOf("") }
    var applePasswordInput by rememberSaveable { mutableStateOf("") }
    var showApplePassword by rememberSaveable { mutableStateOf(false) }
    var hideAppleEmail by rememberSaveable { mutableStateOf(false) }
    var showEmailAuthForm by rememberSaveable { mutableStateOf(false) }
    var isEmailRegisterMode by rememberSaveable { mutableStateOf(false) }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var emailInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var authStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }

    var currentUser by remember {
        mutableStateOf(AuthAndOrderManager.getCurrentUser(context))
    }

    // Properly handle the Android system Back button across onboarding steps
    BackHandler(enabled = activeStep > 0 || showEmailAuthForm || showAppleAuthForm) {
        if (showAppleAuthForm) {
            showAppleAuthForm = false
            authStatusMessage = null
        } else if (showEmailAuthForm) {
            showEmailAuthForm = false
            authStatusMessage = null
        } else if (activeStep > 0) {
            authStatusMessage = null
            currentStep = (activeStep - 1).coerceAtLeast(0)
        }
    }

    val isDark = AppPreferencesManager.isDarkMode

    CompositionLocalProvider(LocalPreserveWhiteText provides isDark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color(0xFF07080B) else Color(0xFFF0F1F4))
                .testTag("onboarding_root_container")
        ) {
            // Layer 0: Native lightweight ambient flowing glass background (Dark or Light variant)
            OnboardingAnimatedGlassBackground(
                stepIndex = activeStep,
                isDark = isDark,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 1: Responsive, inset-safe step content with subtle horizontal + fade transition
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = activeStep,
                    transitionSpec = {
                        val direction = if (targetState >= initialState) 1 else -1
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> (direction * fullWidth * 0.08f).toInt() }
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing)
                        )).togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing),
                                targetOffsetX = { fullWidth -> (-direction * fullWidth * 0.05f).toInt() }
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
                            )
                        ).using(SizeTransform(clip = true))
                    },
                    label = "OnboardingStepTransition",
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 520.dp)
                ) { step ->
                    when (step) {
                        0 -> WelcomeOnboardingStep(
                            onNext = { currentStep = 1 }
                        )

                        1 -> LanguageSelectionOnboardingStep(
                            selectedLanguage = AppPreferencesManager.currentLanguage,
                            onSelectLanguage = { lang ->
                                AppPreferencesManager.setLanguage(context, lang)
                            },
                            onBack = { currentStep = 0 },
                            onNext = {
                                currentUser = AuthAndOrderManager.getCurrentUser(context)
                                currentStep = 2
                            }
                        )

                        2 -> AuthOrGuestOnboardingStep(
                            currentUser = currentUser,
                            isGoogleSigningIn = isGoogleSigningIn,
                            showAppleForm = showAppleAuthForm,
                            appleEmailInput = appleEmailInput,
                            applePasswordInput = applePasswordInput,
                            showApplePassword = showApplePassword,
                            hideAppleEmail = hideAppleEmail,
                            showEmailForm = showEmailAuthForm,
                            isRegisterMode = isEmailRegisterMode,
                            nameInput = nameInput,
                            emailInput = emailInput,
                            passwordInput = passwordInput,
                            showPassword = showPassword,
                            statusMessage = authStatusMessage,
                            onBack = {
                                if (showAppleAuthForm) {
                                    showAppleAuthForm = false
                                    authStatusMessage = null
                                } else if (showEmailAuthForm) {
                                    showEmailAuthForm = false
                                    authStatusMessage = null
                                } else {
                                    authStatusMessage = null
                                    currentStep = 1
                                }
                            },
                            onGoogleSignInClick = {
                                if (!isGoogleSigningIn) {
                                    authStatusMessage = null
                                    isGoogleSigningIn = true
                                    coroutineScope.launch {
                                        val signedInUser = AuthAndOrderManager.signInWithGoogleCredentialManager(
                                            activityContext = context,
                                            onCancel = {
                                                isGoogleSigningIn = false
                                                authStatusMessage = AppLocaleManager.translate(
                                                    "Google ავტორიზაცია გაუქმდა",
                                                    AppPreferencesManager.currentLanguage
                                                )
                                            },
                                            onError = { errMsg ->
                                                isGoogleSigningIn = false
                                                authStatusMessage = errMsg.ifBlank {
                                                    AppLocaleManager.translate(
                                                        "Google ავტორიზაცია ვერ მოხერხდა",
                                                        AppPreferencesManager.currentLanguage
                                                    )
                                                }
                                            }
                                        )
                                        isGoogleSigningIn = false
                                        if (signedInUser != null) {
                                            currentUser = signedInUser
                                            authStatusMessage = null
                                            Toast.makeText(
                                                context,
                                                AppLocaleManager.translate(
                                                    "Google ავტორიზაცია წარმატებულია!",
                                                    AppPreferencesManager.currentLanguage
                                                ),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            currentStep = 3
                                        }
                                    }
                                }
                            },
                            onToggleAppleForm = {
                                showAppleAuthForm = !showAppleAuthForm
                                if (showAppleAuthForm) {
                                    showEmailAuthForm = false
                                }
                                authStatusMessage = null
                            },
                            onAppleEmailChange = {
                                appleEmailInput = it
                                authStatusMessage = null
                            },
                            onApplePasswordChange = {
                                applePasswordInput = it
                                authStatusMessage = null
                            },
                            onToggleApplePasswordVisibility = {
                                showApplePassword = !showApplePassword
                            },
                            onToggleHideAppleEmail = {
                                hideAppleEmail = !hideAppleEmail
                                authStatusMessage = null
                            },
                            onSubmitAppleAuth = {
                                val resolvedEmail = if (hideAppleEmail) {
                                    val base = appleEmailInput.trim().substringBefore("@").ifBlank { "apple_user" }
                                    "${base}_relay@privaterelay.appleid.com"
                                } else {
                                    appleEmailInput.trim()
                                }
                                if (!hideAppleEmail && (resolvedEmail.isBlank() || !resolvedEmail.contains("@"))) {
                                    authStatusMessage = AppLocaleManager.translate(
                                        "გთხოვთ შეიყვანოთ სწორი Apple ID ელ. ფოსტა",
                                        AppPreferencesManager.currentLanguage
                                    )
                                } else if (applePasswordInput.length < 6) {
                                    authStatusMessage = AppLocaleManager.translate(
                                        "პაროლი უნდა შედგებოდეს მინიმუმ 6 სიმბოლოსგან",
                                        AppPreferencesManager.currentLanguage
                                    )
                                } else {
                                    val displayName = resolvedEmail.substringBefore("@")
                                        .replaceFirstChar { it.uppercase() }
                                    val signedInAppleUser = AuthAndOrderManager.loginWithApple(
                                        context = context,
                                        email = resolvedEmail,
                                        name = displayName,
                                        firstName = displayName
                                    )
                                    currentUser = signedInAppleUser
                                    showAppleAuthForm = false
                                    authStatusMessage = null
                                    Toast.makeText(
                                        context,
                                        AppLocaleManager.translate(
                                            "Apple ID ავტორიზაცია წარმატებულია!",
                                            AppPreferencesManager.currentLanguage
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    currentStep = 3
                                }
                            },
                            onToggleEmailForm = {
                                showEmailAuthForm = !showEmailAuthForm
                                if (showEmailAuthForm) {
                                    showAppleAuthForm = false
                                }
                                authStatusMessage = null
                            },
                            onToggleRegisterMode = { registerMode ->
                                isEmailRegisterMode = registerMode
                                authStatusMessage = null
                            },
                            onNameChange = {
                                nameInput = it
                                authStatusMessage = null
                            },
                            onEmailChange = {
                                emailInput = it
                                authStatusMessage = null
                            },
                            onPasswordChange = {
                                passwordInput = it
                                authStatusMessage = null
                            },
                            onTogglePasswordVisibility = {
                                showPassword = !showPassword
                            },
                            onSubmitEmailAuth = {
                                val (success, msg) = if (isEmailRegisterMode) {
                                    AuthAndOrderManager.registerWithEmail(
                                        context = context,
                                        name = nameInput,
                                        email = emailInput,
                                        pass = passwordInput
                                    )
                                } else {
                                    AuthAndOrderManager.loginWithEmail(
                                        context = context,
                                        email = emailInput,
                                        pass = passwordInput
                                    )
                                }
                                if (success) {
                                    currentUser = AuthAndOrderManager.getCurrentUser(context)
                                    showEmailAuthForm = false
                                    authStatusMessage = null
                                    currentStep = 3
                                } else {
                                    authStatusMessage = AppLocaleManager.translate(
                                        msg,
                                        AppPreferencesManager.currentLanguage
                                    )
                                }
                            },
                            onSkipAsGuest = {
                                authStatusMessage = null
                                showAppleAuthForm = false
                                showEmailAuthForm = false
                                currentStep = 3
                            },
                            onContinueAuthenticated = {
                                authStatusMessage = null
                                currentStep = 3
                            },
                            onSwitchAccount = {
                                coroutineScope.launch {
                                    AuthAndOrderManager.logoutSafely(context)
                                    currentUser = null
                                    authStatusMessage = null
                                }
                            }
                        )

                        else -> ThemeSelectionOnboardingStep(
                            isDark = isDark,
                            onSelectDarkMode = { selectedDark ->
                                AppPreferencesManager.setDarkMode(context, selectedDark)
                            },
                            onBack = { currentStep = 2 },
                            onFinish = {
                                // Validate and persist selected theme & Setup completion before entering Home screen
                                AppPreferencesManager.setDarkMode(context, AppPreferencesManager.isDarkMode)
                                AppPreferencesManager.setOnboardingCompleted(context, true)
                                onFinishOnboarding()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// Native Lightweight Animated Ambient Glass Background
// ==============================================================================
@Composable
fun OnboardingAnimatedGlassBackground(
    stepIndex: Int,
    modifier: Modifier = Modifier,
    isDark: Boolean = AppPreferencesManager.isDarkMode
) {
    BstoreAmbientGlassBackground(
        isDark = isDark,
        stepShift = stepIndex.coerceIn(0, TOTAL_ONBOARDING_STEPS - 1) * 0.04f,
        modifier = modifier
    )
}

// ==============================================================================
// Screen 1 — Welcome ("მოგესალმები!")
// ==============================================================================
@Composable
private fun WelcomeOnboardingStep(
    onNext: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 20.dp)
            .testTag("onboarding_welcome_step"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Upper-middle: Animated BSTORE Brand & Product Showcase + Wordmark
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingBrandProductShowcase(
                    isDark = isDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = if (isDark) Color(0xFFFF6B00) else Color(0xFFF36B16),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 26.8.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = 0.8.sp
                                )
                            ) {
                                append("B")
                            }
                            withStyle(
                                SpanStyle(
                                    color = if (isDark) Color.White else Color(0xFF20242B),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 26.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = 0.8.sp
                                )
                            ) {
                                append("STORE")
                            }
                        },
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            lineHeight = 26.sp,
                            letterSpacing = 0.8.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ტექნიკა, აქსესუარები და სერვისი",
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF687180),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Lower-middle: Concise Georgian Welcome Headline & Supporting Sentence
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "მოგესალმები!",
                    color = if (isDark) Color.White else Color(0xFF20242B),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    modifier = Modifier.testTag("onboarding_welcome_headline")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ყველაფერი, რაც გჭირდება — ერთ სივრცეში.",
                    color = if (isDark) Color(0xFFA0A8B8) else Color(0xFF687180),
                    fontSize = 15.5.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.testTag("onboarding_welcome_subtitle")
                )
            }
        }

        // Bottom Progress Indicator + Circular Frosted-Glass Forward Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                for (i in 0 until TOTAL_ONBOARDING_STEPS) {
                    val isCurrent = i == 0
                    val dotWidth by animateDpAsState(
                        targetValue = if (isCurrent) 8.dp else 6.dp,
                        label = "welcomeDot_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) (if (isDark) Color(0xFFFF7A00) else Color(0xFFF36B16))
                                else if (isDark) Color.White.copy(alpha = 0.22f)
                                else Color(0xFF20242B).copy(alpha = 0.18f)
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Text(
                    text = "1 / $TOTAL_ONBOARDING_STEPS",
                    color = if (isDark) Color(0xFF7E8695) else Color(0xFF687180),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.testTag("onboarding_welcome_step_counter")
                )
            }

            OnboardingCircularForwardButton(
                onClick = onNext,
                modifier = Modifier.testTag("onboarding_welcome_next_button")
            )
        }
    }
}

@Composable
internal fun OnboardingBrandProductShowcase(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val stages = remember { OnboardingIntroVisualStage.entries }

    val isReducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    var isLifecycleResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isLifecycleResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isLifecycleResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var currentStageIndex by remember { mutableIntStateOf(0) }
    var initialLogoRevealDone by remember { mutableStateOf(isReducedMotion) }

    val initialScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (initialLogoRevealDone) 1f else 0.92f,
        animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        label = "OnboardingInitialLogoScale"
    )
    val initialAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (initialLogoRevealDone) 1f else 0f,
        animationSpec = tween(durationMillis = 680, easing = LinearOutSlowInEasing),
        label = "OnboardingInitialLogoAlpha"
    )

    LaunchedEffect(isReducedMotion, isLifecycleResumed) {
        if (isReducedMotion) {
            currentStageIndex = 0
            initialLogoRevealDone = true
            return@LaunchedEffect
        }
        initialLogoRevealDone = true
        while (isLifecycleResumed) {
            val activeStage = stages[currentStageIndex]
            delay(activeStage.enterDurationMs.toLong() + activeStage.holdDurationMs)
            if (!isLifecycleResumed) break
            val nextIdx = (currentStageIndex + 1) % stages.size
            // Skip repeating Stage 0 after BRAND_RETURN (index 5) so the logo doesn't double-hold
            currentStageIndex = if (currentStageIndex == stages.lastIndex) 1 else nextIdx
        }
    }

    val activeStage = stages[currentStageIndex]
    val accentOrange = if (isDark) Color(0xFFFF6B00) else Color(0xFFF36B16)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(148.dp)
            .graphicsLayer {
                scaleX = initialScale
                scaleY = initialScale
                alpha = initialAlpha
            }
            .testTag("onboarding_brand_logo")
    ) {
        // Subtle ambient radial halo behind logo/product
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(
                            accentOrange.copy(alpha = if (activeStage.isBrandLogo) 0.16f else 0.22f),
                            accentOrange.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            accentOrange.copy(alpha = if (activeStage.isBrandLogo) 0.12f else 0.18f),
                            Color(0xFFFFF0E5).copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    }
                ),
                radius = size.minDimension * 0.50f
            )
        }

        AnimatedContent(
            targetState = activeStage,
            transitionSpec = {
                val enterMs = targetState.enterDurationMs
                val exitMs = (enterMs * 0.78f).toInt().coerceAtLeast(380)
                (fadeIn(
                    animationSpec = tween(durationMillis = enterMs, easing = FastOutSlowInEasing)
                ) + slideInVertically(
                    animationSpec = tween(durationMillis = enterMs, easing = FastOutSlowInEasing),
                    initialOffsetY = { fullHeight -> fullHeight / 8 }
                ) + scaleIn(
                    animationSpec = tween(durationMillis = enterMs, easing = FastOutSlowInEasing),
                    initialScale = 0.90f
                )).togetherWith(
                    fadeOut(
                        animationSpec = tween(durationMillis = exitMs, easing = FastOutLinearInEasing)
                    ) + slideOutVertically(
                        animationSpec = tween(durationMillis = exitMs, easing = FastOutLinearInEasing),
                        targetOffsetY = { fullHeight -> -fullHeight / 10 }
                    ) + scaleOut(
                        animationSpec = tween(durationMillis = exitMs, easing = FastOutLinearInEasing),
                        targetScale = 0.94f
                    )
                ).using(SizeTransform(clip = false))
            },
            contentAlignment = Alignment.Center,
            label = "OnboardingBrandProductStageTransition",
            modifier = Modifier.fillMaxSize()
        ) { stage ->
            if (stage.isBrandLogo) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.Black)
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    accentOrange.copy(alpha = 0.78f),
                                    Color.White.copy(alpha = 0.24f),
                                    accentOrange.copy(alpha = 0.45f)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(8.dp)
                        .testTag("onboarding_intro_stage_${stage.id}")
                ) {
                    Image(
                        painter = painterResource(id = stage.drawableRes),
                        contentDescription = stage.contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                    )
                }
            } else {
                // Product stage inside a refined Soft Graphite / Dark glass showcase pedestal
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(136.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xFF161920).copy(alpha = 0.88f),
                                        Color(0xFF0E1015).copy(alpha = 0.92f)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFF8F9FB).copy(alpha = 0.96f),
                                        Color(0xFFE9EBEF).copy(alpha = 0.94f)
                                    )
                                }
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(
                                        accentOrange.copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.14f),
                                        accentOrange.copy(alpha = 0.30f)
                                    )
                                } else {
                                    listOf(
                                        accentOrange.copy(alpha = 0.48f),
                                        Color(0xFFDCE0E6),
                                        accentOrange.copy(alpha = 0.28f)
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(30.dp)
                        )
                        .padding(12.dp)
                        .testTag("onboarding_intro_stage_${stage.id}")
                ) {
                    Image(
                        painter = painterResource(id = stage.drawableRes),
                        contentDescription = stage.contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ==============================================================================
// Screen 2 — Language Selection ("აირჩიეთ ენა" — ქართული, English, Русский)
// ==============================================================================
@Composable
private fun LanguageSelectionOnboardingStep(
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag("onboarding_language_step"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        OnboardingStepHeader(
            stepNumber = 2,
            totalSteps = TOTAL_ONBOARDING_STEPS,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "აირჩიეთ ენა",
                color = if (isDark) Color.White else Color(0xFF0F172A),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("onboarding_language_headline")
            )

            Spacer(modifier = Modifier.height(6.dp))

            androidx.compose.material3.Text(
                text = when (selectedLanguage) {
                    AppLanguage.KA -> "Select language"
                    AppLanguage.EN -> "Choose your preferred language"
                    AppLanguage.RU -> "Выберите предпочитаемый язык"
                },
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppLanguage.entries.forEach { lang ->
                    val isSelected = selectedLanguage == lang
                    OnboardingLanguageRow(
                        language = lang,
                        isSelected = isSelected,
                        onClick = { onSelectLanguage(lang) }
                    )
                }
            }
        }

        OnboardingPrimaryGlassButton(
            text = "შემდეგი",
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
                .testTag("onboarding_language_next_button")
        )
    }
}

@Composable
private fun OnboardingLanguageRow(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val rowShape = RoundedCornerShape(18.dp)
    val borderBrush = if (isSelected) {
        Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFFFF7A00).copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.22f),
                    Color(0xFFFF7A00).copy(alpha = 0.45f)
                )
            } else {
                listOf(
                    Color(0xFFFF7A00),
                    Color(0xFFFFA94D).copy(alpha = 0.65f),
                    Color(0xFFFF7A00).copy(alpha = 0.85f)
                )
            }
        )
    } else {
        Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.06f)
                )
            } else {
                listOf(
                    Color.White,
                    Color(0xFFCBD5E1).copy(alpha = 0.75f)
                )
            }
        )
    }

    val backgroundBrush = if (isSelected) {
        Brush.horizontalGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF231B16).copy(alpha = 0.82f),
                    Color(0xFF191B22).copy(alpha = 0.80f),
                    Color(0xFF281C14).copy(alpha = 0.82f)
                )
            } else {
                listOf(
                    Color(0xFFFFF5EB).copy(alpha = 0.92f),
                    Color(0xFFFFFFFF).copy(alpha = 0.88f),
                    Color(0xFFFFEFE0).copy(alpha = 0.92f)
                )
            }
        )
    } else {
        Brush.horizontalGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF14171F).copy(alpha = 0.72f),
                    Color(0xFF10131A).copy(alpha = 0.72f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.82f),
                    Color(0xFFF3F7FD).copy(alpha = 0.78f)
                )
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clip(rowShape)
            .background(backgroundBrush)
            .border(width = 1.dp, brush = borderBrush, shape = rowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("onboarding_lang_${language.code}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LanguageFlagBadge(language = language)

            // Native language name stays in its own script (ქართული / English / Русский)
            androidx.compose.material3.Text(
                text = language.label,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }

        // Restrained orange selection radio indicator
        val outerRingColor by animateColorAsState(
            targetValue = if (isSelected) Color(0xFFFF7A00)
            else if (isDark) Color.White.copy(alpha = 0.28f)
            else Color(0xFF94A3B8).copy(alpha = 0.55f),
            animationSpec = tween(180),
            label = "langRadioRing_${language.code}"
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .border(width = 1.6.dp, color = outerRingColor, shape = CircleShape)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF7A00))
                )
            }
        }
    }
}

@Composable
private fun LanguageFlagBadge(language: AppLanguage) {
    val isDark = AppPreferencesManager.isDarkMode
    Canvas(
        modifier = Modifier
            .size(width = 30.dp, height = 21.dp)
            .clip(RoundedCornerShape(5.dp))
            .border(0.6.dp, if (isDark) Color.White.copy(alpha = 0.24f) else Color(0xFF94A3B8).copy(alpha = 0.45f), RoundedCornerShape(5.dp))
    ) {
        val w = size.width
        val h = size.height
        when (language) {
            AppLanguage.KA -> {
                // Georgian Flag: White field + Red central cross + 4 small quadrant crosses
                drawRect(color = Color.White)
                val red = Color(0xFFE8112D)
                val barW = w * 0.16f
                val barH = h * 0.22f
                drawRect(
                    color = red,
                    topLeft = Offset((w - barW) / 2f, 0f),
                    size = Size(barW, h)
                )
                drawRect(
                    color = red,
                    topLeft = Offset(0f, (h - barH) / 2f),
                    size = Size(w, barH)
                )
                val qCenters = listOf(
                    Offset(w * 0.22f, h * 0.24f),
                    Offset(w * 0.78f, h * 0.24f),
                    Offset(w * 0.22f, h * 0.76f),
                    Offset(w * 0.78f, h * 0.76f)
                )
                val miniV = Size(w * 0.07f, h * 0.20f)
                val miniH = Size(w * 0.14f, h * 0.10f)
                qCenters.forEach { c ->
                    drawRect(
                        color = red,
                        topLeft = Offset(c.x - miniV.width / 2f, c.y - miniV.height / 2f),
                        size = miniV
                    )
                    drawRect(
                        color = red,
                        topLeft = Offset(c.x - miniH.width / 2f, c.y - miniH.height / 2f),
                        size = miniH
                    )
                }
            }

            AppLanguage.EN -> {
                // UK Flag: Navy base + diagonals + St George Cross
                val navy = Color(0xFF012169)
                val red = Color(0xFFC8102E)
                drawRect(color = navy)
                drawLine(
                    color = Color.White,
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                    strokeWidth = h * 0.22f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(w, 0f),
                    end = Offset(0f, h),
                    strokeWidth = h * 0.22f
                )
                drawLine(
                    color = red,
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                    strokeWidth = h * 0.10f
                )
                drawLine(
                    color = red,
                    start = Offset(w, 0f),
                    end = Offset(0f, h),
                    strokeWidth = h * 0.10f
                )
                val whiteV = w * 0.24f
                val whiteH = h * 0.34f
                drawRect(
                    color = Color.White,
                    topLeft = Offset((w - whiteV) / 2f, 0f),
                    size = Size(whiteV, h)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(0f, (h - whiteH) / 2f),
                    size = Size(w, whiteH)
                )
                val redV = w * 0.14f
                val redH = h * 0.20f
                drawRect(
                    color = red,
                    topLeft = Offset((w - redV) / 2f, 0f),
                    size = Size(redV, h)
                )
                drawRect(
                    color = red,
                    topLeft = Offset(0f, (h - redH) / 2f),
                    size = Size(w, redH)
                )
            }

            AppLanguage.RU -> {
                // Russian Tricolor Flag: White / Blue / Red
                val stripeH = h / 3f
                drawRect(
                    color = Color.White,
                    topLeft = Offset(0f, 0f),
                    size = Size(w, stripeH)
                )
                drawRect(
                    color = Color(0xFF0039A6),
                    topLeft = Offset(0f, stripeH),
                    size = Size(w, stripeH)
                )
                drawRect(
                    color = Color(0xFFD52B1E),
                    topLeft = Offset(0f, stripeH * 2f),
                    size = Size(w, stripeH)
                )
            }
        }
    }
}

// ==============================================================================
// Screen 3 — Authentication or Guest Access ("შესვლა ანგარიშით")
// ==============================================================================
@Composable
private fun AuthOrGuestOnboardingStep(
    currentUser: UserProfile?,
    isGoogleSigningIn: Boolean,
    showAppleForm: Boolean,
    appleEmailInput: String,
    applePasswordInput: String,
    showApplePassword: Boolean,
    hideAppleEmail: Boolean,
    showEmailForm: Boolean,
    isRegisterMode: Boolean,
    nameInput: String,
    emailInput: String,
    passwordInput: String,
    showPassword: Boolean,
    statusMessage: String?,
    onBack: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onToggleAppleForm: () -> Unit,
    onAppleEmailChange: (String) -> Unit,
    onApplePasswordChange: (String) -> Unit,
    onToggleApplePasswordVisibility: () -> Unit,
    onToggleHideAppleEmail: () -> Unit,
    onSubmitAppleAuth: () -> Unit,
    onToggleEmailForm: () -> Unit,
    onToggleRegisterMode: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmitEmailAuth: () -> Unit,
    onSkipAsGuest: () -> Unit,
    onContinueAuthenticated: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag("onboarding_auth_step"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        OnboardingStepHeader(
            stepNumber = 3,
            totalSteps = TOTAL_ONBOARDING_STEPS,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Frosted Glass User Avatar Icon Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color(0xFF1C2029).copy(alpha = 0.80f),
                                    Color(0xFF10131A).copy(alpha = 0.88f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.90f),
                                    Color(0xFFEAF1FC).copy(alpha = 0.82f)
                                )
                            }
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.08f),
                                    Color(0xFFFF7A00).copy(alpha = 0.24f)
                                )
                            } else {
                                listOf(
                                    Color.White,
                                    Color(0xFFCBD5E1).copy(alpha = 0.7f),
                                    Color(0xFFFF7A00).copy(alpha = 0.35f)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Account",
                    tint = if (isDark) Color(0xFFD0D7E2) else Color(0xFF334155),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "შესვლა ანგარიშით",
                color = if (isDark) Color.White else Color(0xFF0F172A),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_auth_headline")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "აირჩიე შენთვის მოსახერხებელი გზა ან გააგრძელე ანგარიშის გარეშე.",
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(22.dp))

            if (currentUser != null) {
                // Respect already authenticated user session without forcing re-authentication
                val cardShape = RoundedCornerShape(18.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(if (isDark) Color(0xFF141820).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.86f))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xFFFF7A00).copy(alpha = 0.50f),
                                        Color.White.copy(alpha = 0.14f)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFFF7A00).copy(alpha = 0.65f),
                                        Color(0xFFCBD5E1)
                                    )
                                }
                            ),
                            shape = cardShape
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "თქვენ უკვე შესული ხართ სისტემაში",
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                                fontSize = 12.sp
                            )
                            androidx.compose.material3.Text(
                                text = currentUser.displayName,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentUser.email.isNotBlank()) {
                                androidx.compose.material3.Text(
                                    text = currentUser.email,
                                    color = if (isDark) Color(0xFFA0A8B8) else Color(0xFF475569),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                OnboardingPrimaryGlassButton(
                    text = "გაგრძელება",
                    onClick = onContinueAuthenticated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_continue_authenticated_button")
                )
            } else {
                // 1. Glass-style "Google-ით შესვლა" Button
                OnboardingAuthOptionButton(
                    text = "Google-ით შესვლა",
                    isPrimaryHighlight = true,
                    isLoading = isGoogleSigningIn,
                    onClick = onGoogleSignInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_google_sign_in_button"),
                    leadingBadge = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Glass-style "Apple-ით შესვლა" Button
                OnboardingAuthOptionButton(
                    text = "Apple-ით შესვლა",
                    isPrimaryHighlight = showAppleForm,
                    isLoading = false,
                    onClick = onToggleAppleForm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_apple_sign_in_button"),
                    leadingBadge = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_apple_logo),
                            contentDescription = "Apple",
                            tint = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )

                // Expandable inline Glass Apple Sign-In Panel
                AnimatedVisibility(
                    visible = showAppleForm,
                    enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = tween(160)) + fadeOut(animationSpec = tween(140))
                ) {
                    OnboardingAppleAuthFormPanel(
                        appleEmailInput = appleEmailInput,
                        applePasswordInput = applePasswordInput,
                        showApplePassword = showApplePassword,
                        hideAppleEmail = hideAppleEmail,
                        onAppleEmailChange = onAppleEmailChange,
                        onApplePasswordChange = onApplePasswordChange,
                        onToggleApplePasswordVisibility = onToggleApplePasswordVisibility,
                        onToggleHideAppleEmail = onToggleHideAppleEmail,
                        onSubmit = onSubmitAppleAuth,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Glass-style "ელ. ფოსტით" Alternative Authentication Button
                OnboardingAuthOptionButton(
                    text = "ელ. ფოსტით",
                    isPrimaryHighlight = showEmailForm,
                    isLoading = false,
                    onClick = onToggleEmailForm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_email_auth_button"),
                    leadingBadge = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = if (isDark) Color(0xFFD0D7E2) else Color(0xFF334155),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                )

                // Expandable inline Glass Email Sign-In / Registration Panel
                AnimatedVisibility(
                    visible = showEmailForm,
                    enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = tween(160)) + fadeOut(animationSpec = tween(140))
                ) {
                    OnboardingEmailAuthFormPanel(
                        isRegisterMode = isRegisterMode,
                        nameInput = nameInput,
                        emailInput = emailInput,
                        passwordInput = passwordInput,
                        showPassword = showPassword,
                        onToggleRegisterMode = onToggleRegisterMode,
                        onNameChange = onNameChange,
                        onEmailChange = onEmailChange,
                        onPasswordChange = onPasswordChange,
                        onTogglePasswordVisibility = onTogglePasswordVisibility,
                        onSubmit = onSubmitEmailAuth,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Non-intrusive localized status/error message if auth is cancelled or fails
                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF261719).copy(alpha = 0.78f) else Color(0xFFFEE2E2))
                            .border(1.dp, if (isDark) Color(0xFFF87171).copy(alpha = 0.35f) else Color(0xFFF87171), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("onboarding_auth_status_message")
                    ) {
                        androidx.compose.material3.Text(
                            text = statusMessage,
                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                            fontSize = 12.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Bottom Guest / Switch Account action pinned cleanly at bottom
        if (currentUser != null) {
            OnboardingUnderlinedTextAction(
                text = "სხვა ანგარიშით შესვლა",
                onClick = onSwitchAccount,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .testTag("onboarding_switch_account_button")
            )
        } else {
            OnboardingUnderlinedTextAction(
                text = "ავტორიზაციის გამოტოვება",
                onClick = onSkipAsGuest,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .testTag("onboarding_skip_auth_button")
            )
        }
    }
}

@Composable
private fun OnboardingAppleAuthFormPanel(
    appleEmailInput: String,
    applePasswordInput: String,
    showApplePassword: Boolean,
    hideAppleEmail: Boolean,
    onAppleEmailChange: (String) -> Unit,
    onApplePasswordChange: (String) -> Unit,
    onToggleApplePasswordVisibility: () -> Unit,
    onToggleHideAppleEmail: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = AppPreferencesManager.isDarkMode
    val panelShape = RoundedCornerShape(18.dp)
    val panelBg = if (isDark) Color(0xFF12151D).copy(alpha = 0.86f) else Color.White.copy(alpha = 0.88f)
    val panelBorderColors = if (isDark) {
        listOf(Color.White.copy(alpha = 0.16f), Color(0xFFFF7A00).copy(alpha = 0.28f))
    } else {
        listOf(Color(0xFFCBD5E1), Color(0xFFFF7A00).copy(alpha = 0.40f))
    }
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val inputBg = if (isDark) Color(0xFF0E1117) else Color(0xFFF8FAFC)
    val inputBorderUnfocused = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFCBD5E1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(panelBg)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(colors = panelBorderColors),
                shape = panelShape
            )
            .padding(16.dp)
            .testTag("onboarding_apple_form_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_apple_logo),
                contentDescription = "Apple",
                tint = textPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Apple ID ავტორიზაცია",
                color = textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        androidx.compose.material3.OutlinedTextField(
            value = appleEmailInput,
            onValueChange = onAppleEmailChange,
            label = { Text("Apple ID (ელ. ფოსტა)", fontSize = 12.sp, color = textSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                unfocusedBorderColor = inputBorderUnfocused,
                cursorColor = Color(0xFFFF7A00)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_apple_email_input")
        )

        androidx.compose.material3.OutlinedTextField(
            value = applePasswordInput,
            onValueChange = onApplePasswordChange,
            label = { Text("პაროლი", fontSize = 12.sp, color = textSecondary) },
            singleLine = true,
            visualTransformation = if (showApplePassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleApplePasswordVisibility) {
                    Icon(
                        imageVector = if (showApplePassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = textSecondary
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                unfocusedBorderColor = inputBorderUnfocused,
                cursorColor = Color(0xFFFF7A00)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_apple_password_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) Color(0xFF0B0D12).copy(alpha = 0.85f) else Color(0xFFF1F5F9))
                .border(
                    width = 0.8.dp,
                    color = if (hideAppleEmail) Color(0xFFFF7A00).copy(alpha = 0.55f) else inputBorderUnfocused,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(onClick = onToggleHideAppleEmail)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("onboarding_apple_hide_email_toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "დამალე ჩემი ელ. ფოსტა (Private Relay)",
                color = if (isDark) Color(0xFFD0D7E2) else Color(0xFF334155),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .border(
                        width = 1.4.dp,
                        color = if (hideAppleEmail) Color(0xFFFF7A00) else if (isDark) Color.White.copy(alpha = 0.28f) else Color(0xFF94A3B8),
                        shape = CircleShape
                    )
            ) {
                if (hideAppleEmail) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF7A00))
                    )
                }
            }
        }

        OnboardingPrimaryGlassButton(
            text = "შესვლა",
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("onboarding_apple_submit_button")
        )
    }
}

@Composable
private fun OnboardingAuthOptionButton(
    text: String,
    isPrimaryHighlight: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingBadge: @Composable () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val borderBrush = if (isPrimaryHighlight || isPressed) {
        Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.24f),
                    Color(0xFFFF7A00).copy(alpha = 0.42f),
                    Color.White.copy(alpha = 0.14f)
                )
            } else {
                listOf(
                    Color(0xFFFF7A00).copy(alpha = 0.72f),
                    Color.White,
                    Color(0xFFFF7A00).copy(alpha = 0.45f)
                )
            }
        )
    } else {
        Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.07f)
                )
            } else {
                listOf(
                    Color.White,
                    Color(0xFFCBD5E1).copy(alpha = 0.8f)
                )
            }
        )
    }

    val containerBrush = if (isPrimaryHighlight) {
        Brush.horizontalGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF171B24).copy(alpha = 0.82f),
                    Color(0xFF1E1B1A).copy(alpha = 0.82f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.90f),
                    Color(0xFFFFF4EA).copy(alpha = 0.90f)
                )
            }
        )
    } else {
        Brush.horizontalGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF13161E).copy(alpha = 0.75f),
                    Color(0xFF101319).copy(alpha = 0.75f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.84f),
                    Color(0xFFF2F6FC).copy(alpha = 0.80f)
                )
            }
        )
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(containerBrush)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    color = if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFFF7A00).copy(alpha = 0.2f)
                ),
                enabled = !isLoading,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E232E).copy(alpha = 0.85f) else Color(0xFFF1F5F9))
                .border(0.8.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1), CircleShape)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFFFF7A00),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                leadingBadge()
            }
        }

        Text(
            text = text,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(end = 20.dp)
        )
    }
}

@Composable
private fun OnboardingEmailAuthFormPanel(
    isRegisterMode: Boolean,
    nameInput: String,
    emailInput: String,
    passwordInput: String,
    showPassword: Boolean,
    onToggleRegisterMode: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = AppPreferencesManager.isDarkMode
    val panelShape = RoundedCornerShape(18.dp)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val inputBg = if (isDark) Color(0xFF0E1117) else Color(0xFFF8FAFC)
    val inputBorderUnfocused = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0xFFCBD5E1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(if (isDark) Color(0xFF12151D).copy(alpha = 0.86f) else Color.White.copy(alpha = 0.88f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color(0xFFFF7A00).copy(alpha = 0.28f)
                        )
                    } else {
                        listOf(
                            Color(0xFFCBD5E1),
                            Color(0xFFFF7A00).copy(alpha = 0.40f)
                        )
                    }
                ),
                shape = panelShape
            )
            .padding(16.dp)
            .testTag("onboarding_email_form_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sign In / Register Mode Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF0B0D12).copy(alpha = 0.85f) else Color(0xFFE9EEF5))
                .padding(3.dp)
        ) {
            val loginTabSelected = !isRegisterMode
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (loginTabSelected) {
                            if (isDark) Color(0xFF232834) else Color.White
                        } else Color.Transparent
                    )
                    .border(
                        width = if (loginTabSelected) 0.8.dp else 0.dp,
                        color = if (loginTabSelected) Color(0xFFFF7A00).copy(alpha = 0.65f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggleRegisterMode(false) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "შესვლა",
                    color = if (loginTabSelected) textPrimary else textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isRegisterMode) {
                            if (isDark) Color(0xFF232834) else Color.White
                        } else Color.Transparent
                    )
                    .border(
                        width = if (isRegisterMode) 0.8.dp else 0.dp,
                        color = if (isRegisterMode) Color(0xFFFF7A00).copy(alpha = 0.65f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggleRegisterMode(true) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "რეგისტრაცია",
                    color = if (isRegisterMode) textPrimary else textSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isRegisterMode) {
            androidx.compose.material3.OutlinedTextField(
                value = nameInput,
                onValueChange = onNameChange,
                label = { Text("თქვენი სახელი და გვარი", fontSize = 12.sp, color = textSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                    unfocusedBorderColor = inputBorderUnfocused,
                    cursorColor = Color(0xFFFF7A00)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_email_name_input")
            )
        }

        androidx.compose.material3.OutlinedTextField(
            value = emailInput,
            onValueChange = onEmailChange,
            label = { Text("ელ. ფოსტა", fontSize = 12.sp, color = textSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                unfocusedBorderColor = inputBorderUnfocused,
                cursorColor = Color(0xFFFF7A00)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_email_input")
        )

        androidx.compose.material3.OutlinedTextField(
            value = passwordInput,
            onValueChange = onPasswordChange,
            label = { Text("პაროლი", fontSize = 12.sp, color = textSecondary) },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = textSecondary
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                unfocusedBorderColor = inputBorderUnfocused,
                cursorColor = Color(0xFFFF7A00)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_password_input")
        )

        OnboardingPrimaryGlassButton(
            text = if (isRegisterMode) "რეგისტრაცია" else "შესვლა",
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("onboarding_email_submit_button")
        )
    }
}

// ==============================================================================
// Screen 4 (Final, 4/4) — Theme Selection ("აირჩიე აპის თემა")
// ==============================================================================
@Composable
private fun ThemeSelectionOnboardingStep(
    isDark: Boolean,
    onSelectDarkMode: (Boolean) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val accentOrange = if (isDark) Color(0xFFFF6B00) else Color(0xFFF36B16)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag("onboarding_theme_step"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        OnboardingStepHeader(
            stepNumber = 4,
            totalSteps = TOTAL_ONBOARDING_STEPS,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "აირჩიე აპის თემა",
                color = if (isDark) Color.White else Color(0xFF20242B),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_theme_headline")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "შეგიძლია ნებისმიერ დროს შეცვალო პარამეტრებიდან.",
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF687180),
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_theme_subtitle")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OnboardingThemePreviewCard(
                    title = "ნათელი თემა",
                    subtitle = "რბილი გრაფიტის ნათელი ფონი",
                    icon = Icons.Default.LightMode,
                    isPreviewDark = false,
                    isSelected = !isDark,
                    parentIsDark = isDark,
                    accentOrange = accentOrange,
                    cardTestTag = "onboarding_theme_light_card",
                    iconTestTag = "onboarding_theme_light_icon",
                    previewTestTag = "onboarding_theme_light_preview",
                    onClick = { onSelectDarkMode(false) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                OnboardingThemePreviewCard(
                    title = "მუქი თემა",
                    subtitle = "კლასიკური მუქი ფონი",
                    icon = Icons.Default.DarkMode,
                    isPreviewDark = true,
                    isSelected = isDark,
                    parentIsDark = isDark,
                    accentOrange = accentOrange,
                    cardTestTag = "onboarding_theme_dark_card",
                    iconTestTag = "onboarding_theme_dark_icon",
                    previewTestTag = "onboarding_theme_dark_preview",
                    onClick = { onSelectDarkMode(true) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingPrimaryGlassButton(
                text = "მთავარ გვერდზე გადასვლა",
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_finish_button")
            )
        }
    }
}

@Composable
internal fun OnboardingThemePreviewCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isPreviewDark: Boolean,
    isSelected: Boolean,
    parentIsDark: Boolean,
    accentOrange: Color,
    cardTestTag: String,
    iconTestTag: String,
    previewTestTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(20.dp)
    val outerContainerBg = when {
        parentIsDark && isSelected -> Color(0xFF1F1916)
        parentIsDark -> Color(0xFF13161D)
        isSelected -> Color(0xFFFFF0E5)
        else -> Color(0xFFF8F9FB)
    }
    val outerBorderColor = when {
        isSelected -> accentOrange
        parentIsDark -> Color(0xFF282F3A)
        else -> Color(0xFFDCE0E6)
    }

    // Miniature preview tokens accurately representing BSTORE Soft Graphite Light & Dark themes
    val miniPageBg = if (isPreviewDark) Color(0xFF0D0F12) else Color(0xFFF0F1F4)
    val miniHeaderBg = if (isPreviewDark) Color(0xFF16191E) else Color(0xFFF8F9FB)
    val miniSearchBg = if (isPreviewDark) Color(0xFF1F242C) else Color(0xFFE5E8ED)
    val miniCardBg = if (isPreviewDark) Color(0xFF16191E) else Color(0xFFF8F9FB)
    val miniElevatedBg = if (isPreviewDark) Color(0xFF1F242C) else Color(0xFFFFFFFF)
    val miniBorder = if (isPreviewDark) Color(0xFF282F3A) else Color(0xFFDCE0E6)
    val miniTextPrimary = if (isPreviewDark) Color(0xFFF8FAFC) else Color(0xFF20242B)
    val miniTextSecondary = if (isPreviewDark) Color(0xFFA0A5B0) else Color(0xFF687180)
    val miniOrange = if (isPreviewDark) Color(0xFFFF7A00) else Color(0xFFF36B16)

    val cardTextStyle = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(outerContainerBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = outerBorderColor,
                shape = cardShape
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(cardTestTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top header row (fixed 36.dp height): Sun / Moon Icon + Selection Check
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (isSelected) accentOrange.copy(alpha = 0.16f)
                        else if (parentIsDark) Color(0xFF1D222C)
                        else Color(0xFFE5E8ED)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) accentOrange.copy(alpha = 0.55f) else miniBorder,
                        shape = RoundedCornerShape(11.dp)
                    )
                    .testTag(iconTestTag)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) accentOrange else if (parentIsDark) Color(0xFFE2E8F0) else Color(0xFF20242B),
                    modifier = Modifier.size(19.dp)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentOrange else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) accentOrange else if (parentIsDark) Color(0xFF3B4354) else Color(0xFFAEB4BE),
                        shape = CircleShape
                    )
                    .testTag("${cardTestTag}_indicator")
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Miniature BSTORE UI Preview Window (identical 108.dp height & structure)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(miniPageBg)
                .border(1.dp, miniBorder, RoundedCornerShape(14.dp))
                .padding(8.dp)
                .testTag(previewTestTag),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mini Top Bar (BS badge + title bar + status dot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(19.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(miniHeaderBg)
                    .border(0.6.dp, miniBorder, RoundedCornerShape(7.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(miniOrange)
                    )
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(miniTextPrimary)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(miniTextSecondary.copy(alpha = 0.55f))
                )
            }

            // Mini Search Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(miniSearchBg)
                    .border(0.6.dp, miniBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = miniTextSecondary,
                    modifier = Modifier.size(10.dp)
                )
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(miniTextSecondary.copy(alpha = 0.55f))
                )
            }

            // Mini Product Card with Surface Hierarchy + Orange CTA Pill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(49.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(miniCardBg)
                    .border(0.8.dp, miniBorder, RoundedCornerShape(9.dp))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(miniElevatedBg)
                            .border(0.5.dp, miniBorder, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(miniOrange.copy(alpha = 0.85f))
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(miniTextPrimary)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(miniTextSecondary)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(miniOrange)
                    )
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(miniOrange)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Controlled Title & Description Block (fixed 54.dp height so 1-line and 2-line descriptions never alter card height)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    color = if (parentIsDark) Color.White else Color(0xFF20242B),
                    fontSize = 14.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    style = cardTextStyle,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = subtitle,
                    color = if (parentIsDark) Color(0xFF94A3B8) else Color(0xFF687180),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    style = cardTextStyle,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==============================================================================
// Shared Glassmorphism Onboarding UI Primitives
// ==============================================================================
@Composable
private fun OnboardingStepHeader(
    stepNumber: Int,
    totalSteps: Int,
    onBack: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF151821).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.84f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.White.copy(alpha = 0.20f),
                                Color.White.copy(alpha = 0.07f)
                            )
                        } else {
                            listOf(
                                Color.White,
                                Color(0xFFCBD5E1)
                            )
                        }
                    ),
                    shape = CircleShape
                )
                .clickable(onClick = onBack)
                .testTag("onboarding_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "უკან დაბრუნება",
                tint = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.size(22.dp)
            )
        }

        androidx.compose.material3.Text(
            text = "$stepNumber / $totalSteps",
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OnboardingCircularForwardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = AppPreferencesManager.isDarkMode
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF281C15).copy(alpha = if (isPressed) 0.92f else 0.80f),
                            Color(0xFF14161D).copy(alpha = 0.86f)
                        )
                    } else {
                        listOf(
                            Color(0xFFFF8820).copy(alpha = if (isPressed) 0.95f else 0.90f),
                            Color(0xFFFF6B00)
                        )
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFFFF7A00).copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.18f),
                            Color(0xFFFF7A00).copy(alpha = 0.40f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.72f),
                            Color(0xFFFF6B00)
                        )
                    }
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = if (isDark) Color(0xFFFF7A00) else Color.White),
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "შემდეგი",
            tint = if (isDark) Color(0xFFFF7A00) else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun OnboardingPrimaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = AppPreferencesManager.isDarkMode
    val shape = RoundedCornerShape(28.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minHeight = 54.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = if (isDark) {
                        listOf(
                            Color(0xFF1A1E27).copy(alpha = if (isPressed) 0.90f else 0.78f),
                            Color(0xFF231D1B).copy(alpha = if (isPressed) 0.90f else 0.80f),
                            Color(0xFF332014).copy(alpha = if (isPressed) 0.92f else 0.82f)
                        )
                    } else {
                        listOf(
                            Color(0xFFFF6B00).copy(alpha = if (isPressed) 0.96f else 0.90f),
                            Color(0xFFFF7A00).copy(alpha = if (isPressed) 0.98f else 0.92f),
                            Color(0xFFEA580C).copy(alpha = if (isPressed) 0.98f else 0.94f)
                        )
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            Color.White.copy(alpha = 0.24f),
                            Color(0xFFFF7A00).copy(alpha = 0.35f),
                            Color(0xFFFF7A00).copy(alpha = 0.58f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.70f),
                            Color(0xFFFF7A00).copy(alpha = 0.60f),
                            Color(0xFFEA580C).copy(alpha = 0.85f)
                        )
                    }
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = Color.White.copy(alpha = 0.28f)),
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 13.dp)
    ) {
        CompositionLocalProvider(LocalPreserveWhiteText provides true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.width(20.dp))

                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingUnderlinedTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = AppPreferencesManager.isDarkMode
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (isDark) Color(0xFFA0A8B8) else Color(0xFF475569),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center
        )
    }
}
