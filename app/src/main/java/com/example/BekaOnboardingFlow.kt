package com.example

import android.provider.Settings
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

private const val TOTAL_ONBOARDING_STEPS = 4

/**
 * BEKASERVICE First-Launch Premium Dark Glassmorphism Onboarding Flow.
 *
 * Steps:
 *  0 (1/4) -> Welcome screen ("მოგესალმები!")
 *  1 (2/4) -> Language selection ("აირჩიეთ ენა" — ქართული, English, Русский)
 *  2 (3/4) -> Optional Authentication or Guest Access ("შესვლა ანგარიშით")
 *  3 (4/4) -> Introductory Benefits & Completion ("მზად ხარ!" / "დაიწყე →")
 */
@Composable
fun BekaOnboardingFlow(
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
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
    BackHandler(enabled = currentStep > 0 || showEmailAuthForm || showAppleAuthForm) {
        if (showAppleAuthForm) {
            showAppleAuthForm = false
            authStatusMessage = null
        } else if (showEmailAuthForm) {
            showEmailAuthForm = false
            authStatusMessage = null
        } else if (currentStep > 0) {
            authStatusMessage = null
            currentStep -= 1
        }
    }

    CompositionLocalProvider(LocalPreserveWhiteText provides true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07080B))
                .testTag("onboarding_root_container")
        ) {
            // Layer 0: Native lightweight ambient flowing dark-glass background
            OnboardingAnimatedGlassBackground(
                stepIndex = currentStep,
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
                    targetState = currentStep,
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
                                AuthAndOrderManager.logout(context)
                                currentUser = null
                                authStatusMessage = null
                            }
                        )

                        else -> ReadyAndBenefitsOnboardingStep(
                            onBack = { currentStep = 2 },
                            onFinish = {
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isReducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    var isResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val driftPhase: Float = if (isReducedMotion || !isResumed) {
        0.35f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "OnboardingAmbientLight")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "AmbientLightDrift"
        )
        animated
    }

    val stepShift = (stepIndex.coerceIn(0, 3) * 0.04f)
    val primaryRibbonPath = remember { Path() }
    val secondaryRibbonPath = remember { Path() }

    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val baseBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF07080B),
                    Color(0xFF0B0D12),
                    Color(0xFF06070A)
                )
            )

            onDrawBehind {
                // 1. Deep near-black & graphite base
                drawRect(brush = baseBrush)

                val phase = (driftPhase + stepShift).coerceIn(0f, 1f)

                // 2. Upper-left / upper-center warm diffuse amber light form
                val topLightCenter = Offset(
                    x = w * (0.24f + 0.18f * phase),
                    y = h * (0.18f + 0.08f * (1f - phase))
                )
                val topLightRadius = maxOf(w, h) * 0.58f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF7A00).copy(alpha = 0.14f),
                            Color(0xFFD95B00).copy(alpha = 0.06f),
                            Color(0xFF6B2B00).copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = topLightCenter,
                        radius = topLightRadius
                    ),
                    radius = topLightRadius,
                    center = topLightCenter
                )

                // 3. Lower-right soft warm amber counter-glow
                val bottomLightCenter = Offset(
                    x = w * (0.78f - 0.16f * phase),
                    y = h * (0.78f - 0.07f * phase)
                )
                val bottomLightRadius = maxOf(w, h) * 0.52f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF7A00).copy(alpha = 0.11f),
                            Color(0xFFB84A00).copy(alpha = 0.045f),
                            Color.Transparent
                        ),
                        center = bottomLightCenter,
                        radius = bottomLightRadius
                    ),
                    radius = bottomLightRadius,
                    center = bottomLightCenter
                )

                // 4. Subtle silky curved light contour (matching the dark glass wave reference)
                primaryRibbonPath.reset()
                primaryRibbonPath.moveTo(-w * 0.1f, h * (0.34f - 0.03f * phase))
                primaryRibbonPath.cubicTo(
                    w * 0.28f,
                    h * (0.24f + 0.04f * phase),
                    w * 0.65f,
                    h * (0.14f - 0.03f * phase),
                    w * 1.12f,
                    h * (0.04f + 0.02f * phase)
                )
                drawPath(
                    path = primaryRibbonPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFF8820).copy(alpha = 0.11f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )

                secondaryRibbonPath.reset()
                secondaryRibbonPath.moveTo(-w * 0.08f, h * (0.86f - 0.02f * phase))
                secondaryRibbonPath.cubicTo(
                    w * 0.35f,
                    h * (0.76f + 0.03f * phase),
                    w * 0.72f,
                    h * (0.66f - 0.02f * phase),
                    w * 1.1f,
                    h * (0.54f + 0.03f * phase)
                )
                drawPath(
                    path = secondaryRibbonPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFF7A00).copy(alpha = 0.09f),
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    )
}

// ==============================================================================
// Screen 1 — Welcome ("მოგესალმები!")
// ==============================================================================
@Composable
private fun WelcomeOnboardingStep(
    onNext: () -> Unit
) {
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
            // Upper-middle: BEKASERVICE Brand Emblem & Wordmark
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1A1817).copy(alpha = 0.86f),
                                    Color(0xFF0E1015).copy(alpha = 0.92f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF7A00).copy(alpha = 0.55f),
                                    Color.White.copy(alpha = 0.18f),
                                    Color(0xFFFF7A00).copy(alpha = 0.30f)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(14.dp)
                        .testTag("onboarding_brand_logo")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_beka_main_emblem),
                        contentDescription = "BEKASERVICE",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "BEKA",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                    Text(
                        text = "SERVICE",
                        color = Color(0xFFFF7A00),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "შენი ციფრული სერვისები",
                    color = Color(0xFF94A3B8),
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
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                    modifier = Modifier.testTag("onboarding_welcome_headline")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "ყველაფერი, რაც გჭირდება — ერთ სივრცეში.",
                    color = Color(0xFFA0A8B8),
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
                                if (isCurrent) Color(0xFFFF7A00)
                                else Color.White.copy(alpha = 0.22f)
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.material3.Text(
                    text = "1 / $TOTAL_ONBOARDING_STEPS",
                    color = Color(0xFF7E8695),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            OnboardingCircularForwardButton(
                onClick = onNext,
                modifier = Modifier.testTag("onboarding_welcome_next_button")
            )
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
                color = Color.White,
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
                color = Color(0xFF94A3B8),
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
    val rowShape = RoundedCornerShape(18.dp)
    val borderBrush = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF7A00).copy(alpha = 0.65f),
                Color.White.copy(alpha = 0.22f),
                Color(0xFFFF7A00).copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

    val backgroundBrush = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF231B16).copy(alpha = 0.82f),
                Color(0xFF191B22).copy(alpha = 0.80f),
                Color(0xFF281C14).copy(alpha = 0.82f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF14171F).copy(alpha = 0.72f),
                Color(0xFF10131A).copy(alpha = 0.72f)
            )
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
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }

        // Restrained orange selection radio indicator
        val outerRingColor by animateColorAsState(
            targetValue = if (isSelected) Color(0xFFFF7A00) else Color.White.copy(alpha = 0.28f),
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
    Canvas(
        modifier = Modifier
            .size(width = 30.dp, height = 21.dp)
            .clip(RoundedCornerShape(5.dp))
            .border(0.6.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(5.dp))
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
                            colors = listOf(
                                Color(0xFF1C2029).copy(alpha = 0.80f),
                                Color(0xFF10131A).copy(alpha = 0.88f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.08f),
                                Color(0xFFFF7A00).copy(alpha = 0.24f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Account",
                    tint = Color(0xFFD0D7E2),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "შესვლა ანგარიშით",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_auth_headline")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "აირჩიე შენთვის მოსახერხებელი გზა ან გააგრძელე ანგარიშის გარეშე.",
                color = Color(0xFF94A3B8),
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
                        .background(Color(0xFF141820).copy(alpha = 0.78f))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF7A00).copy(alpha = 0.50f),
                                    Color.White.copy(alpha = 0.14f)
                                )
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
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "თქვენ უკვე შესული ხართ სისტემაში",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            androidx.compose.material3.Text(
                                text = currentUser.displayName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentUser.email.isNotBlank()) {
                                androidx.compose.material3.Text(
                                    text = currentUser.email,
                                    color = Color(0xFFA0A8B8),
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
                            tint = Color.White,
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
                            tint = Color(0xFFD0D7E2),
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
                            .background(Color(0xFF261719).copy(alpha = 0.78f))
                            .border(1.dp, Color(0xFFF87171).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("onboarding_auth_status_message")
                    ) {
                        androidx.compose.material3.Text(
                            text = statusMessage,
                            color = Color(0xFFFCA5A5),
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
    val panelShape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(Color(0xFF12151D).copy(alpha = 0.86f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color(0xFFFF7A00).copy(alpha = 0.28f)
                    )
                ),
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
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Apple ID ავტორიზაცია",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        androidx.compose.material3.OutlinedTextField(
            value = appleEmailInput,
            onValueChange = onAppleEmailChange,
            label = { Text("Apple ID (ელ. ფოსტა)", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF0E1117),
                unfocusedContainerColor = Color(0xFF0E1117),
                focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                cursorColor = Color(0xFFFF7A00)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_apple_email_input")
        )

        androidx.compose.material3.OutlinedTextField(
            value = applePasswordInput,
            onValueChange = onApplePasswordChange,
            label = { Text("პაროლი", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
            singleLine = true,
            visualTransformation = if (showApplePassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleApplePasswordVisibility) {
                    Icon(
                        imageVector = if (showApplePassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = Color(0xFF94A3B8)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF0E1117),
                unfocusedContainerColor = Color(0xFF0E1117),
                focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
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
                .background(Color(0xFF0B0D12).copy(alpha = 0.85f))
                .border(
                    width = 0.8.dp,
                    color = if (hideAppleEmail) Color(0xFFFF7A00).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.12f),
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
                color = Color(0xFFD0D7E2),
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
                        color = if (hideAppleEmail) Color(0xFFFF7A00) else Color.White.copy(alpha = 0.28f),
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
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val borderBrush = if (isPrimaryHighlight || isPressed) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.24f),
                Color(0xFFFF7A00).copy(alpha = 0.42f),
                Color.White.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.07f)
            )
        )
    }

    val containerBrush = if (isPrimaryHighlight) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF171B24).copy(alpha = 0.82f),
                Color(0xFF1E1B1A).copy(alpha = 0.82f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF13161E).copy(alpha = 0.75f),
                Color(0xFF101319).copy(alpha = 0.75f)
            )
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
                indication = androidx.compose.material3.ripple(color = Color.White.copy(alpha = 0.2f)),
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
                .background(Color(0xFF1E232E).copy(alpha = 0.85f))
                .border(0.8.dp, Color.White.copy(alpha = 0.12f), CircleShape)
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
            color = Color.White,
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
    val panelShape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(Color(0xFF12151D).copy(alpha = 0.86f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color(0xFFFF7A00).copy(alpha = 0.28f)
                    )
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
                .background(Color(0xFF0B0D12).copy(alpha = 0.85f))
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
                        if (loginTabSelected) Color(0xFF232834) else Color.Transparent
                    )
                    .border(
                        width = if (loginTabSelected) 0.8.dp else 0.dp,
                        color = if (loginTabSelected) Color(0xFFFF7A00).copy(alpha = 0.55f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggleRegisterMode(false) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "შესვლა",
                    color = if (loginTabSelected) Color.White else Color(0xFF8E96A4),
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
                        if (isRegisterMode) Color(0xFF232834) else Color.Transparent
                    )
                    .border(
                        width = if (isRegisterMode) 0.8.dp else 0.dp,
                        color = if (isRegisterMode) Color(0xFFFF7A00).copy(alpha = 0.55f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggleRegisterMode(true) }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "რეგისტრაცია",
                    color = if (isRegisterMode) Color.White else Color(0xFF8E96A4),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isRegisterMode) {
            androidx.compose.material3.OutlinedTextField(
                value = nameInput,
                onValueChange = onNameChange,
                label = { Text("თქვენი სახელი და გვარი", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF0E1117),
                    unfocusedContainerColor = Color(0xFF0E1117),
                    focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
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
            label = { Text("ელ. ფოსტა", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF0E1117),
                unfocusedContainerColor = Color(0xFF0E1117),
                focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                cursorColor = Color(0xFFFF7A00)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_email_input")
        )

        androidx.compose.material3.OutlinedTextField(
            value = passwordInput,
            onValueChange = onPasswordChange,
            label = { Text("პაროლი", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle password",
                        tint = Color(0xFF94A3B8)
                    )
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF0E1117),
                unfocusedContainerColor = Color(0xFF0E1117),
                focusedBorderColor = Color(0xFFFF7A00).copy(alpha = 0.7f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
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
// Screen 4 — Introductory Benefits & Ready ("მზად ხარ!" / "დაიწყე →")
// ==============================================================================
@Composable
private fun ReadyAndBenefitsOnboardingStep(
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag("onboarding_ready_step"),
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
            // BEKASERVICE Emblem & Brand Header
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A1817).copy(alpha = 0.85f),
                                Color(0xFF0E1015).copy(alpha = 0.90f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF7A00).copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.18f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_beka_main_emblem),
                    contentDescription = "BEKASERVICE",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "BEKA",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = "SERVICE",
                    color = Color(0xFFFF7A00),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "მზად ხარ!",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_ready_headline")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "გაეცანი ციფრულ სერვისებს BEKASERVICE-ში.",
                color = Color(0xFF94A3B8),
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_ready_subtitle")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Concise Introductory Benefits inside subtle glass containers
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingBenefitGlassRow(
                    icon = Icons.Default.Explore,
                    title = "მარტივი ნავიგაცია",
                    description = "სწრაფი ძიება და კატეგორიები ერთ სივრცეში"
                )
                OnboardingBenefitGlassRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "სანდოობა",
                    description = "გარანტირებული ხარისხი და ოფიციალური სერვისი"
                )
                OnboardingBenefitGlassRow(
                    icon = Icons.Default.Memory,
                    title = "ციფრული სერვისები",
                    description = "AI პაკეტები, პროგრამული უზრუნველყოფა და ტექნიკა"
                )
            }
        }

        // Bottom Actions pinned cleanly so they remain visible on all screen heights
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingPrimaryGlassButton(
                text = "დაიწყე",
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_finish_button")
            )

            Spacer(modifier = Modifier.height(6.dp))

            OnboardingUnderlinedTextAction(
                text = "მთავარ გვერდზე",
                onClick = onFinish,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .testTag("onboarding_home_link_button")
            )
        }
    }
}

@Composable
private fun OnboardingBenefitGlassRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF14171F).copy(alpha = 0.72f),
                        Color(0xFF0F1218).copy(alpha = 0.74f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color(0xFFFF7A00).copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1B1F29).copy(alpha = 0.85f))
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color(0xFFFF7A00).copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFFE2E8F0),
                modifier = Modifier.size(19.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
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
                .background(Color(0xFF151821).copy(alpha = 0.72f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color.White.copy(alpha = 0.07f)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(onClick = onBack)
                .testTag("onboarding_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "უკან დაბრუნება",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        androidx.compose.material3.Text(
            text = "$stepNumber / $totalSteps",
            color = Color(0xFF94A3B8),
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF281C15).copy(alpha = if (isPressed) 0.92f else 0.80f),
                        Color(0xFF14161D).copy(alpha = 0.86f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF7A00).copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.18f),
                        Color(0xFFFF7A00).copy(alpha = 0.40f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = Color(0xFFFF7A00)),
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "შემდეგი",
            tint = Color(0xFFFF7A00),
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
                    colors = listOf(
                        Color(0xFF1A1E27).copy(alpha = if (isPressed) 0.90f else 0.78f),
                        Color(0xFF231D1B).copy(alpha = if (isPressed) 0.90f else 0.80f),
                        Color(0xFF332014).copy(alpha = if (isPressed) 0.92f else 0.82f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color(0xFFFF7A00).copy(alpha = 0.35f),
                        Color(0xFFFF7A00).copy(alpha = 0.58f)
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = Color(0xFFFF7A00).copy(alpha = 0.3f)),
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 13.dp)
    ) {
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

@Composable
private fun OnboardingUnderlinedTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            color = Color(0xFFA0A8B8),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.Center
        )
    }
}
