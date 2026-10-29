package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val BrandOrange = OrangePrimary

// ==============================================================================
// 0. THEME-AWARE GLASSMORPHISM PALETTE (DARK, LIGHT & SYSTEM THEME SUPPORT)
// ==============================================================================

@Immutable
internal data class DiagnosticsThemePalette(
    val isDark: Boolean,
    val pageBg: Color,
    val cardBg: Color,
    val cardElevatedBg: Color,
    val subSurfaceBg: Color,
    val chipBg: Color,
    val borderSubtle: Color,
    val composerGlassTop: Color,
    val composerGlassBottom: Color,
    val composerFrostTint: Color,
    val composerButtonBg: Color,
    val composerUnfocusedBorder: Color,
    val composerFocusedBorder: Color,
    val composerTopSpecular: Color,
    val composerDockGradient: List<Color>,
    val textPrimary: Color,
    val textSecondary: Color,
    val textPlaceholder: Color,
    val accentOrange: Color
)

@Composable
internal fun rememberDiagnosticsThemePalette(): DiagnosticsThemePalette {
    val systemDark = isSystemInDarkTheme()
    val isDark = AppPreferencesManager.isDarkMode || (MaterialTheme.colorScheme.background.luminance() < 0.5f && systemDark)
    // Respect explicit AppPreferencesManager toggle directly
    val effectiveDark = AppPreferencesManager.isDarkMode
    return remember(effectiveDark) {
        if (effectiveDark) {
            DiagnosticsThemePalette(
                isDark = true,
                pageBg = Color(0xFF0D0F12),
                cardBg = Color(0xFF16191E),
                cardElevatedBg = Color(0xFF1A1E26),
                subSurfaceBg = Color(0xFF111419),
                chipBg = Color(0xFF1E2229),
                borderSubtle = Color(0xFF2A2E37),
                composerGlassTop = Color(0xFF1E2432).copy(alpha = 0.92f),
                composerGlassBottom = Color(0xFF121620).copy(alpha = 0.96f),
                composerFrostTint = Color(0xFF1A202C).copy(alpha = 0.50f),
                composerButtonBg = Color(0xFF202634),
                composerUnfocusedBorder = Color.White.copy(alpha = 0.22f),
                composerFocusedBorder = Color(0xFFFF6B00),
                composerTopSpecular = Color.White.copy(alpha = 0.28f),
                composerDockGradient = listOf(
                    Color.Transparent,
                    Color(0xFF0D0F12).copy(alpha = 0.88f),
                    Color(0xFF0D0F12).copy(alpha = 0.98f)
                ),
                textPrimary = Color(0xFFF8FAFC),
                textSecondary = Color(0xFF9CA3AF),
                textPlaceholder = Color(0xFF9CA3AF).copy(alpha = 0.85f),
                accentOrange = Color(0xFFFF6B00)
            )
        } else {
            DiagnosticsThemePalette(
                isDark = false,
                pageBg = Color(0xFFF3F6FB),
                cardBg = Color(0xFFFFFFFF),
                cardElevatedBg = Color(0xFFFFFFFF),
                subSurfaceBg = Color(0xFFF1F5F9),
                chipBg = Color(0xFFE2E8F0),
                borderSubtle = Color(0xFFCBD5E1),
                composerGlassTop = Color(0xFFFFFFFF).copy(alpha = 0.96f),
                composerGlassBottom = Color(0xFFF8FAFC).copy(alpha = 0.98f),
                composerFrostTint = Color(0xFFFFFFFF).copy(alpha = 0.72f),
                composerButtonBg = Color(0xFFF1F5F9),
                composerUnfocusedBorder = Color(0xFF94A3B8).copy(alpha = 0.65f),
                composerFocusedBorder = Color(0xFFFF6B00),
                composerTopSpecular = Color.White.copy(alpha = 0.95f),
                composerDockGradient = listOf(
                    Color.Transparent,
                    Color(0xFFF3F6FB).copy(alpha = 0.90f),
                    Color(0xFFF3F6FB).copy(alpha = 0.99f)
                ),
                textPrimary = Color(0xFF0F172A),
                textSecondary = Color(0xFF475569),
                textPlaceholder = Color(0xFF64748B),
                accentOrange = Color(0xFFFF6B00)
            )
        }
    }
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

// ==============================================================================
// 1. STATE HOLDER FOR MULTIMODAL DIAGNOSTICS WORKSPACE
// ==============================================================================

class DiagnosticsWorkspaceStateHolder(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    initialMode: DiagnosticUserMode = DiagnosticUserMode.STANDARD
) {
    var sessionId by mutableStateOf("diag_session_" + System.currentTimeMillis())
        private set

    var selectedMode by mutableStateOf(initialMode)
        private set

    var inputText by mutableStateOf("")
    val messages = mutableStateListOf<DiagnosticMessage>()
    val pendingAttachments = mutableStateListOf<DiagnosticAttachment>()
    val savedSessions = mutableStateListOf<DiagnosticSessionRecord>()

    var isAnalyzing by mutableStateOf(false)
        private set

    var isProcessingImage by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var showHistorySheet by mutableStateOf(false)
    var startersDismissed by mutableStateOf(false)
    var zoomedAttachment by mutableStateOf<DiagnosticAttachment?>(null)

    private var lastFailedText: String = ""
    private var lastFailedAttachments: List<DiagnosticAttachment> = emptyList()
    private var activeRequestJob: kotlinx.coroutines.Job? = null
    private var activeRequestId: Long = 0L

    init {
        refreshSavedSessions()
    }

    fun refreshSavedSessions() {
        val loaded = DiagnosticsSessionRepository.loadSessions(context)
        savedSessions.clear()
        savedSessions.addAll(loaded)
    }

    fun switchMode(newMode: DiagnosticUserMode) {
        if (selectedMode == newMode) return
        selectedMode = newMode
    }

    fun startNewSession() {
        activeRequestId++
        activeRequestJob?.cancel()
        activeRequestJob = null
        isAnalyzing = false
        if (messages.isNotEmpty()) {
            DiagnosticsSessionRepository.saveOrUpdateSession(
                context = context,
                sessionId = sessionId,
                mode = selectedMode,
                messages = messages.toList()
            )
            refreshSavedSessions()
        }
        sessionId = "diag_session_" + System.currentTimeMillis() + "_" + activeRequestId
        messages.clear()
        pendingAttachments.clear()
        inputText = ""
        errorMessage = null
        lastFailedText = ""
        lastFailedAttachments = emptyList()
    }

    fun openSavedSession(record: DiagnosticSessionRecord) {
        activeRequestId++
        activeRequestJob?.cancel()
        activeRequestJob = null
        isAnalyzing = false
        sessionId = record.id
        selectedMode = record.mode
        messages.clear()
        messages.addAll(record.messages)
        pendingAttachments.clear()
        errorMessage = null
        showHistorySheet = false
    }

    fun deleteSavedSession(targetSessionId: String) {
        val updated = DiagnosticsSessionRepository.deleteSession(context, targetSessionId)
        savedSessions.clear()
        savedSessions.addAll(updated)
        if (sessionId == targetSessionId) {
            activeRequestId++
            activeRequestJob?.cancel()
            activeRequestJob = null
            isAnalyzing = false
            messages.clear()
            pendingAttachments.clear()
            inputText = ""
            errorMessage = null
            lastFailedText = ""
            lastFailedAttachments = emptyList()
            sessionId = "diag_session_" + System.currentTimeMillis() + "_" + activeRequestId
        }
    }

    fun attachBitmapDirect(bitmap: Bitmap, displayName: String = "photo_${pendingAttachments.size + 1}.jpg") {
        if (pendingAttachments.size >= 4) {
            errorMessage = "მაქსიმუმ 4 ფოტოს მიმაგრებაა შესაძლებელი ერთ შეტყობინებაში"
            return
        }
        val processed = DiagnosticImageProcessor.processBitmap(bitmap, displayName)
        pendingAttachments.add(processed)
        errorMessage = null
    }

    fun attachUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        coroutineScope.launch {
            isProcessingImage = true
            errorMessage = null
            for (uri in uris) {
                if (pendingAttachments.size >= 4) break
                val res = DiagnosticImageProcessor.processUri(context, uri)
                res.onSuccess { att ->
                    pendingAttachments.add(att)
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "ფოტოს დამუშავება ვერ მოხერხდა"
                }
            }
            isProcessingImage = false
        }
    }

    fun removePendingAttachment(attachmentId: String) {
        pendingAttachments.removeAll { it.id == attachmentId }
    }

    fun dismissError() {
        errorMessage = null
    }

    fun sendMessage(
        overrideText: String? = null,
        catalogProducts: List<ProductItem>,
        onFocusInputRequest: () -> Unit = {}
    ) {
        if (isAnalyzing || isProcessingImage) return // Duplicate send prevention

        val textToSend = (overrideText ?: inputText).trim()
        val attachmentsToSend = pendingAttachments.toList()

        if (textToSend.isBlank() && attachmentsToSend.isEmpty()) {
            errorMessage = "გთხოვთ აღწეროთ სიმპტომი ან მიამაგროთ ფოტო"
            return
        }

        errorMessage = null
        lastFailedText = textToSend
        lastFailedAttachments = attachmentsToSend

        val userMsg = DiagnosticMessage(
            role = "user",
            text = textToSend.ifBlank { "ატვირთულია ფოტო ვიზუალური დიაგნოსტიკისთვის" },
            mode = selectedMode,
            attachments = attachmentsToSend
        )

        val historyBeforeTurn = messages.toList()
        messages.add(userMsg)
        inputText = ""
        pendingAttachments.clear()
        isAnalyzing = true

        val requestId = ++activeRequestId
        val targetSessionId = sessionId
        activeRequestJob?.cancel()

        activeRequestJob = coroutineScope.launch {
            try {
                val response = BekaMultimodalDiagnosticsEngine.analyzeTurn(
                    conversationHistory = historyBeforeTurn,
                    currentUserMessage = userMsg,
                    mode = selectedMode,
                    catalogProducts = catalogProducts
                )
                if (requestId != activeRequestId || targetSessionId != sessionId) {
                    return@launch
                }
                if (response.assistantMessage.isError || response.assistantMessage.text.isBlank()) {
                    throw IllegalStateException("Diagnostic response is empty or flagged as error")
                }
                if (messages.none { it.id == response.assistantMessage.id }) {
                    messages.add(response.assistantMessage)
                }
                val saved = DiagnosticsSessionRepository.saveOrUpdateSession(
                    context = context,
                    sessionId = sessionId,
                    mode = selectedMode,
                    messages = messages.toList()
                )
                refreshSavedSessions()
                if (saved != null) {
                    DiagnosticsSessionRepository.syncSessionSummaryToFirestoreIfSignedIn(context, saved)
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (e: Exception) {
                if (requestId == activeRequestId && targetSessionId == sessionId) {
                    // Remove optimistic user message and restore composer so user can retry without losing anything
                    messages.removeAll { it.id == userMsg.id }
                    inputText = lastFailedText
                    pendingAttachments.clear()
                    pendingAttachments.addAll(lastFailedAttachments)
                    errorMessage = "დიაგნოსტიკის მოთხოვნა დროებით შეფერხდა. თქვენი ტექსტი და ფოტო შენახულია — სცადეთ თავიდან."
                    onFocusInputRequest()
                }
            } finally {
                if (requestId == activeRequestId) {
                    isAnalyzing = false
                }
            }
        }
    }

    fun triggerContextualAction(
        action: ContextualDiagnosticAction,
        catalogProducts: List<ProductItem>,
        onFocusInputRequest: () -> Unit = {}
    ) {
        if (isAnalyzing) return
        if (action.prefillComposerOnly) {
            if (!inputText.startsWith(action.promptTemplateKa)) {
                inputText = action.promptTemplateKa + inputText
            }
            onFocusInputRequest()
            return
        }
        if (action == ContextualDiagnosticAction.EXPLAIN_SIMPLY) {
            selectedMode = DiagnosticUserMode.STANDARD
        } else if (action == ContextualDiagnosticAction.MORE_DETAIL) {
            selectedMode = DiagnosticUserMode.TECHNICIAN
        }
        sendMessage(
            overrideText = action.promptTemplateKa,
            catalogProducts = catalogProducts,
            onFocusInputRequest = onFocusInputRequest
        )
    }

    fun retryLastFailed(catalogProducts: List<ProductItem>) {
        if (isAnalyzing) return
        if (inputText.isBlank() && lastFailedText.isNotBlank()) {
            inputText = lastFailedText
        }
        if (pendingAttachments.isEmpty() && lastFailedAttachments.isNotEmpty()) {
            pendingAttachments.addAll(lastFailedAttachments)
        }
        sendMessage(catalogProducts = catalogProducts)
    }
}

// ==============================================================================
// 2. MAIN COMPOSABLE: DIAGNOSTICS WORKSPACE SCREEN
// ==============================================================================

@Composable
fun DiagnosticsWorkspaceScreen(
    contentPadding: PaddingValues,
    onProductClick: (ProductItem) -> Unit,
    products: List<ProductItem>,
    externalStateHolder: DiagnosticsWorkspaceStateHolder? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val inputFocusRequester = remember { FocusRequester() }
    val palette = rememberDiagnosticsThemePalette()

    val stateHolder = externalStateHolder ?: remember {
        DiagnosticsWorkspaceStateHolder(context = context, coroutineScope = coroutineScope)
    }

    val listState = rememberLazyListState()

    // Single unified bottom inset strategy:
    // - When keyboard is closed (imeBottom == 0.dp), effectiveBottomInset == bottomNavPadding so the composer sits cleanly above the floating bottom navigation bar.
    // - When keyboard opens (imeBottom > bottomNavPadding), effectiveBottomInset == imeBottom so the composer sits immediately above the keyboard with zero extra gap or double-applied padding.
    val topPadding = contentPadding.calculateTopPadding()
    val bottomNavPadding = contentPadding.calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = imeBottom > bottomNavPadding
    val effectiveBottomInset = maxOf(imeBottom, bottomNavPadding)

    // Auto-scroll to newest message when conversation updates or when keyboard opens
    LaunchedEffect(stateHolder.messages.size, stateHolder.isAnalyzing, isKeyboardOpen) {
        val totalItems = stateHolder.messages.size + (if (stateHolder.isAnalyzing) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    // Native Android Photo Picker (Gallery — up to 4 images, zero storage permissions needed)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            stateHolder.attachUris(uris)
        }
    }

    // Native Android Camera Capture (TakePicturePreview — returns Bitmap directly)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            stateHolder.attachBitmapDirect(bitmap, "camera_${System.currentTimeMillis()}.jpg")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.pageBg)
            .padding(top = topPadding, bottom = effectiveBottomInset)
            .testTag("diagnostics_workspace_root")
    ) {
        // 1. COMPACT GLASSMORPHISM HEADER
        DiagnosticsWorkspaceHeader(
            palette = palette,
            selectedMode = stateHolder.selectedMode,
            savedSessionCount = stateHolder.savedSessions.size,
            hasActiveMessages = stateHolder.messages.isNotEmpty(),
            onSelectMode = { stateHolder.switchMode(it) },
            onNewSession = { stateHolder.startNewSession() },
            onOpenHistory = {
                stateHolder.refreshSavedSessions()
                stateHolder.showHistorySheet = true
            }
        )

        // 2. MAIN WORKSPACE AREA (EMPTY STATE OR CONVERSATION FEED)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (stateHolder.messages.isEmpty() && !stateHolder.isAnalyzing) {
                DiagnosticsEmptyStateContent(
                    palette = palette,
                    selectedMode = stateHolder.selectedMode,
                    startersDismissed = stateHolder.startersDismissed,
                    onDismissStarters = { stateHolder.startersDismissed = true },
                    onPickGallery = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onTakePhoto = {
                        try {
                            cameraLauncher.launch(null)
                        } catch (_: Exception) {
                            Toast.makeText(context, "კამერის გახსნა ვერ მოხერხდა", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onStarterPromptSelected = { promptText ->
                        stateHolder.sendMessage(
                            overrideText = promptText,
                            catalogProducts = products
                        )
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("diag_conversation_list"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = stateHolder.messages,
                        key = { it.id }
                    ) { message ->
                        if (message.isUser) {
                            DiagnosticUserMessageBubble(
                                palette = palette,
                                message = message,
                                onAttachmentClick = { stateHolder.zoomedAttachment = it }
                            )
                        } else {
                            val isLatestAssistant = stateHolder.messages.lastOrNull { !it.isUser }?.id == message.id
                            DiagnosticAssistantCard(
                                palette = palette,
                                message = message,
                                isLatestAssistant = isLatestAssistant,
                                isAnalyzing = stateHolder.isAnalyzing,
                                catalogProducts = products,
                                onProductClick = onProductClick,
                                onContextualAction = { action ->
                                    stateHolder.triggerContextualAction(
                                        action = action,
                                        catalogProducts = products,
                                        onFocusInputRequest = {
                                            try {
                                                inputFocusRequester.requestFocus()
                                            } catch (_: Exception) {
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }

                    if (stateHolder.isAnalyzing) {
                        item(key = "diag_analyzing_indicator") {
                            DiagnosticAnalyzingIndicator(
                                palette = palette,
                                mode = stateHolder.selectedMode
                            )
                        }
                    }
                }
            }
        }

        // 3. MULTIMODAL GLASSMORPHISM MESSAGE COMPOSER (ANCHORED ABOVE BOTTOM NAV OR KEYBOARD)
        DiagnosticsMessageComposer(
            palette = palette,
            inputText = stateHolder.inputText,
            onInputTextChange = { stateHolder.inputText = it },
            pendingAttachments = stateHolder.pendingAttachments,
            isAnalyzing = stateHolder.isAnalyzing,
            isProcessingImage = stateHolder.isProcessingImage,
            errorMessage = stateHolder.errorMessage,
            focusRequester = inputFocusRequester,
            onPickGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onTakePhoto = {
                try {
                    cameraLauncher.launch(null)
                } catch (_: Exception) {
                    Toast.makeText(context, "კამერის გახსნა ვერ მოხერხდა", Toast.LENGTH_SHORT).show()
                }
            },
            onRemoveAttachment = { stateHolder.removePendingAttachment(it) },
            onAttachmentPreviewClick = { stateHolder.zoomedAttachment = it },
            onSend = {
                focusManager.clearFocus()
                stateHolder.sendMessage(catalogProducts = products)
            },
            onRetry = {
                focusManager.clearFocus()
                stateHolder.retryLastFailed(catalogProducts = products)
            },
            onDismissError = { stateHolder.dismissError() }
        )
    }

    // 4. FULL-SCREEN IMAGE ZOOM DIALOG
    stateHolder.zoomedAttachment?.let { att ->
        DiagnosticAttachmentZoomDialog(
            palette = palette,
            attachment = att,
            onDismiss = { stateHolder.zoomedAttachment = null }
        )
    }

    // 5. DIAGNOSTIC SESSION HISTORY SHEET
    if (stateHolder.showHistorySheet) {
        DiagnosticHistorySheetDialog(
            palette = palette,
            sessions = stateHolder.savedSessions,
            activeSessionId = stateHolder.sessionId,
            onOpenSession = { stateHolder.openSavedSession(it) },
            onDeleteSession = { stateHolder.deleteSavedSession(it) },
            onStartNewSession = {
                stateHolder.startNewSession()
                stateHolder.showHistorySheet = false
            },
            onDismiss = { stateHolder.showHistorySheet = false }
        )
    }
}

// ==============================================================================
// 3. COMPACT HEADER WITH MODE TOGGLE & SESSION CONTROLS
// ==============================================================================

@Composable
private fun DiagnosticsWorkspaceHeader(
    palette: DiagnosticsThemePalette,
    selectedMode: DiagnosticUserMode,
    savedSessionCount: Int,
    hasActiveMessages: Boolean,
    onSelectMode: (DiagnosticUserMode) -> Unit,
    onNewSession: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Surface(
        color = palette.cardBg,
        border = BorderStroke(1.dp, palette.borderSubtle),
        shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .testTag("diag_header_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        BrandOrange.copy(alpha = 0.24f),
                                        BrandOrange.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(1.dp, BrandOrange.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BuildCircle,
                            contentDescription = "AI დიაგნოსტიკა",
                            tint = BrandOrange,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI დიაგნოსტიკა",
                            color = palette.textPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ტექნიკური ანალიზი და კონსულტაცია",
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // History Button
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = palette.chipBg,
                        border = BorderStroke(1.dp, palette.borderSubtle),
                        modifier = Modifier
                            .height(29.dp)
                            .clickable { onOpenHistory() }
                            .testTag("diag_history_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "ისტორია",
                                tint = palette.textPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (savedSessionCount > 0) "ისტორია ($savedSessionCount)" else "ისტორია",
                                color = palette.textPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    // New Session Button
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = if (hasActiveMessages) BrandOrange.copy(alpha = 0.16f) else palette.chipBg,
                        border = BorderStroke(
                            1.dp,
                            if (hasActiveMessages) BrandOrange.copy(alpha = 0.6f) else palette.borderSubtle
                        ),
                        modifier = Modifier
                            .height(29.dp)
                            .clickable { onNewSession() }
                            .testTag("diag_new_session_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "ახალი სესია",
                                tint = if (hasActiveMessages) BrandOrange else palette.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "ახალი",
                                color = if (hasActiveMessages) BrandOrange else palette.textPrimary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Mode Selector: სტანდარტული vs ტექნიკოსი
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(palette.subSurfaceBg)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(11.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DiagnosticUserMode.entries.forEach { mode ->
                    val isSelected = selectedMode == mode
                    val tag = if (mode == DiagnosticUserMode.STANDARD) "diag_mode_standard" else "diag_mode_technician"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) BrandOrange else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clickable { onSelectMode(mode) }
                            .testTag(tag)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (mode == DiagnosticUserMode.STANDARD) {
                                    Icons.Default.Person
                                } else {
                                    Icons.Default.Engineering
                                },
                                contentDescription = mode.labelKa,
                                tint = if (isSelected) Color.White else palette.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = mode.labelKa,
                                color = if (isSelected) Color.White else palette.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 4. COMPACT INITIAL EMPTY STATE CARD (“რა პრობლემა აქვს ტელეფონს?”)
// ==============================================================================

@Composable
private fun DiagnosticsEmptyStateContent(
    palette: DiagnosticsThemePalette,
    selectedMode: DiagnosticUserMode,
    startersDismissed: Boolean,
    onDismissStarters: () -> Unit,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onStarterPromptSelected: (String) -> Unit
) {
    val starterPrompts = remember(selectedMode) {
        if (selectedMode == DiagnosticUserMode.TECHNICIAN) {
            listOf(
                "iPhone 14 Pro დავარდნის შემდეგ ეკრანი ბნელია, ზარი გადის და ვიბრირებს",
                "Samsung S23 Ultra დამტენზე აჩვენებს 0.00A-ს და არ ირთვება",
                "Xiaomi Redmi Note 12 ჩერდება Fastboot/Recovery ეკრანზე განახლების შემდეგ",
                "ტელეფონი დასველდა, დამტენის პორტი ცხელდება და მალე ჯდება"
            )
        } else {
            listOf(
                "ტელეფონი დამივარდა, ეკრანზე მწვანე ზოლი გაჩნდა და ნაწილი აღარ რეაგირებს",
                "დამტენს რომ ვაერთებ არ იტენება, კაბელი ირყევა პორტში",
                "ტელეფონი ჩართვისას ჩერდება ლოგოზე და აღარ ირთვება ბოლომდე",
                "წყალი მოხვდა ტელეფონს, დინამიკში ხმა ჩახლეჩილია და მალე ჯდება"
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("diag_empty_state"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.cardBg,
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            BrandOrange.copy(alpha = 0.50f),
                            palette.borderSubtle,
                            BrandOrange.copy(alpha = 0.25f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diag_intro_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // Compact Title Row + Secondary Feature Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BrandOrange.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.42f)),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BrandOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "რა პრობლემა აქვს ტელეფონს?",
                            color = palette.textPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Concise Georgian description
                    Text(
                        text = "აღწერე სიმპტომი ან ატვირთე ფოტო. დაგეხმარები სავარაუდო მიზეზების, შემოწმების ნაბიჯებისა და შეკეთების ვარიანტების განსაზღვრაში.",
                        color = palette.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.5.sp,
                        modifier = Modifier.testTag("diag_intro_description")
                    )

                    // Compact secondary feature chips ("ფოტოს ანალიზი", "ტექნიკური რჩევა")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticCompactFeatureChip(
                            modifier = Modifier.weight(1f),
                            palette = palette,
                            icon = Icons.Default.ImageSearch,
                            label = "ფოტოს ანალიზი",
                            testTag = "diag_feature_chip_photo"
                        )
                        DiagnosticCompactFeatureChip(
                            modifier = Modifier.weight(1f),
                            palette = palette,
                            icon = Icons.Default.Verified,
                            label = "ტექნიკური რჩევა",
                            testTag = "diag_feature_chip_tech"
                        )
                    }

                    // Direct Visual Diagnostics Callout Buttons (Gallery & Camera)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.subSurfaceBg,
                            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onPickGallery() }
                                .testTag("diag_empty_gallery_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "გალერეიდან ფოტოს არჩევა",
                                    tint = BrandOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ფოტოს ატვირთვა",
                                    color = palette.textPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.subSurfaceBg,
                            border = BorderStroke(1.dp, palette.borderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTakePhoto() }
                                .testTag("diag_empty_camera_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "კამერით გადაღება",
                                    tint = palette.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "კამერით გადაღება",
                                    color = palette.textPrimary,
                                    fontSize = 11.5.sp,
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

        // Secondary, dismissible starter examples
        if (!startersDismissed) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("diag_starter_prompts_section"),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "სწრაფი მაგალითები (სურვილისამებრ):",
                            color = palette.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = onDismissStarters,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("diag_dismiss_starters_button")
                        ) {
                            Text(
                                text = "დამალვა",
                                color = palette.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    starterPrompts.forEachIndexed { idx, prompt ->
                        Surface(
                            shape = RoundedCornerShape(13.dp),
                            color = palette.cardBg,
                            border = BorderStroke(1.dp, palette.borderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStarterPromptSelected(prompt) }
                                .testTag("diag_starter_chip_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = prompt,
                                    color = palette.textPrimary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.NorthEast,
                                    contentDescription = null,
                                    tint = BrandOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DiagnosticCompactFeatureChip(
    palette: DiagnosticsThemePalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Surface(
        modifier = modifier
            .height(28.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(9.dp),
        color = BrandOrange.copy(alpha = if (palette.isDark) 0.12f else 0.08f),
        border = BorderStroke(1.dp, BrandOrange.copy(alpha = if (palette.isDark) 0.35f else 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandOrange,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = BrandOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==============================================================================
// 5. CONVERSATION MESSAGE ITEMS (USER BUBBLE & STRUCTURED ASSISTANT CARD)
// ==============================================================================

@Composable
private fun DiagnosticUserMessageBubble(
    palette: DiagnosticsThemePalette,
    message: DiagnosticMessage,
    onAttachmentClick: (DiagnosticAttachment) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_user_message_${message.id}"),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 4.dp
            ),
            color = if (palette.isDark) BrandOrange.copy(alpha = 0.18f) else BrandOrange.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.55f)),
            modifier = Modifier.widthIn(max = 330.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (message.attachments.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(message.attachments, key = { it.id }) { att ->
                            DiagnosticAttachmentThumbnail(
                                palette = palette,
                                attachment = att,
                                sizeDp = 88,
                                onClick = { onAttachmentClick(att) }
                            )
                        }
                    }
                }

                Text(
                    text = message.text,
                    color = palette.textPrimary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${message.mode.labelKa} რეჟიმი",
                        color = BrandOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticAssistantCard(
    palette: DiagnosticsThemePalette,
    message: DiagnosticMessage,
    isLatestAssistant: Boolean,
    isAnalyzing: Boolean,
    catalogProducts: List<ProductItem>,
    onProductClick: (ProductItem) -> Unit,
    onContextualAction: (ContextualDiagnosticAction) -> Unit
) {
    val context = LocalContext.current
    var showTechnicalDetails by remember(message.id, message.mode) {
        mutableStateOf(message.mode == DiagnosticUserMode.TECHNICIAN)
    }

    val section = message.structuredSection

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.cardBg,
        border = BorderStroke(1.dp, palette.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_assistant_message_${message.id}")
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Assistant Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BrandOrange.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = BrandOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "BEKASERVICE AI ინჟინერი",
                            color = palette.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${message.mode.labelKa} ანალიზი",
                            color = palette.textSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                }

                if (section != null &&
                    section.deviceIdentification.isNotBlank() &&
                    !section.deviceIdentification.contains("არ არის მითითებული")
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.subSurfaceBg,
                        border = BorderStroke(1.dp, palette.borderSubtle)
                    ) {
                        Text(
                            text = section.deviceIdentification,
                            color = palette.textPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 155.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Main Conversational Summary (with clean **bold** markdown support)
            val formattedMainText = remember(message.text, palette.textPrimary) {
                formatDiagnosticMarkdownText(message.text, palette.textPrimary)
            }
            Text(
                text = formattedMainText,
                color = palette.textPrimary,
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )

            if (section != null) {
                // Visual Inspection Findings (if photo was analyzed)
                if (section.visualInspectionFindings.isNotBlank()) {
                    val infoBlue = if (palette.isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = infoBlue.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, infoBlue.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diag_visual_findings_box")
                    ) {
                        Column(
                            modifier = Modifier.padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = infoBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "ვიზუალური დათვალიერების დასკვნა",
                                    color = infoBlue,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = section.visualInspectionFindings,
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Hypotheses with Evidence Confidence Badges
                if (section.hypotheses.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("diag_hypotheses_section")
                    ) {
                        Text(
                            text = "სავარაუდო მიზეზები და მტკიცებულების დონე:",
                            color = palette.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )

                        section.hypotheses.forEach { hyp ->
                            val badgeColor = Color(hyp.confidence.badgeColorHex)
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = palette.subSurfaceBg,
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(11.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = hyp.title,
                                            color = palette.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Evidence Confidence Pill (No fake percentages)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeColor.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = hyp.confidence.labelKa,
                                            color = badgeColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    if (hyp.supportingEvidence.isNotBlank()) {
                                        Text(
                                            text = "• საფუძველი: ${hyp.supportingEvidence}",
                                            color = palette.textSecondary,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    if (hyp.verificationOrDisproof.isNotBlank()) {
                                        Text(
                                            text = "• გადამოწმება: ${hyp.verificationOrDisproof}",
                                            color = palette.textPrimary,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recommended Next Diagnostic Step
                if (section.nextDiagnosticTest.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = BrandOrange.copy(alpha = 0.11f),
                        border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diag_next_test_box")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = BrandOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "შემდეგი სადიაგნოსტიკო ნაბიჯი",
                                    color = BrandOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = section.nextDiagnosticTest,
                                color = palette.textPrimary,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )
                            if (section.expectedFindings.isNotBlank()) {
                                Text(
                                    text = "მოსალოდნელი შედეგი: ${section.expectedFindings}",
                                    color = palette.textSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Clarifying Follow-up Question (only if not already included in summaryText)
                val cleanFollowUp = section.followUpQuestion.trim()
                if (cleanFollowUp.isNotBlank() && !message.text.contains(cleanFollowUp.take(24))) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.subSurfaceBg,
                        border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diag_followup_question_box")
                    ) {
                        Column(
                            modifier = Modifier.padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "დამაზუსტებელი კითხვა დიაგნოზის დასაზუსტებლად:",
                                color = BrandOrange,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = cleanFollowUp,
                                color = palette.textPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                // Expandable Technical & Safety Details
                if (section.requiredTools.isNotBlank() || section.safetyConsiderations.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTechnicalDetails = !showTechnicalDetails }
                                .padding(vertical = 4.dp)
                                .testTag("diag_toggle_tech_details"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (showTechnicalDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (showTechnicalDetails) "უსაფრთხოების და ხელსაწყოების დამალვა" else "ხელსაწყოები და უსაფრთხოების რჩევები",
                                color = palette.textSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        AnimatedVisibility(visible = showTechnicalDetails) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.subSurfaceBg,
                                border = BorderStroke(1.dp, palette.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(11.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (section.requiredTools.isNotBlank()) {
                                        Text(
                                            text = "🔧 საჭირო ხელსაწყოები / რეჟიმი: ${section.requiredTools}",
                                            color = palette.textPrimary,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    if (section.safetyConsiderations.isNotBlank()) {
                                        Text(
                                            text = "⚠️ უსაფრთხოება: ${section.safetyConsiderations}",
                                            color = if (palette.isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Optional Relevant BEKASERVICE Catalog Items (only if genuinely matched)
            val matchedCatalogItems = remember(message.recommendedProductIds, catalogProducts) {
                catalogProducts.filter { it.id in message.recommendedProductIds }.take(3)
            }
            if (matchedCatalogItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "შესაბამისი სერვისი / პროდუქტი BEKASERVICE-ის კატალოგში:",
                        color = BrandOrange,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    matchedCatalogItems.forEach { prod ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.subSurfaceBg,
                            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductClick(prod) }
                                .testTag("diag_recommended_product_${prod.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.title,
                                        color = palette.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${prod.category} • ${prod.warranty}",
                                        color = palette.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = prod.price,
                                        color = BrandOrange,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForwardIos,
                                        contentDescription = "ნახვა",
                                        tint = BrandOrange,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Contextual Follow-up Action Chips (active on the latest assistant message)
            if (isLatestAssistant) {
                HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.6f))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "დიაგნოსტიკის გაგრძელება:",
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ContextualDiagnosticAction.entries.forEach { action ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.chipBg,
                                border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable(enabled = !isAnalyzing) { onContextualAction(action) }
                                    .testTag(action.testTag)
                            ) {
                                Text(
                                    text = action.labelKa,
                                    color = palette.textPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                )
                            }
                        }

                        // Optional direct WhatsApp consultation with Beka keeping current diagnostic summary
                        val greenAccent = if (palette.isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF22C55E).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable {
                                    val device = section?.deviceIdentification ?: "ტელეფონი"
                                    val summary = message.text.take(250)
                                    val waText = "გამარჯობა ბექა! AI დიაგნოსტიკის დასკვნა ($device): $summary"
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://wa.me/995598233332?text=${Uri.encode(waText)}")
                                    )
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                }
                                .testTag("diag_action_contact_service")
                        ) {
                            Text(
                                text = "სერვისთან დაკავშირება",
                                color = greenAccent,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticAnalyzingIndicator(
    palette: DiagnosticsThemePalette,
    mode: DiagnosticUserMode
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.cardBg,
        border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_analyzing_card")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = BrandOrange,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = "მიმდინარეობს მულტიმოდალური ანალიზი (${mode.labelKa} რეჟიმი)...",
                    color = palette.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "მოწმდება სიმპტომების კონტექსტი, ვიზუალური მტკიცებულება და სადიაგნოსტიკო ტესტები",
                    color = palette.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ==============================================================================
// 6. PREMIUM GLASSMORPHISM MESSAGE COMPOSER (IME-AWARE, LIGHT/DARK ADAPTIVE)
// ==============================================================================

@Composable
private fun DiagnosticsMessageComposer(
    palette: DiagnosticsThemePalette,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    pendingAttachments: List<DiagnosticAttachment>,
    isAnalyzing: Boolean,
    isProcessingImage: Boolean,
    errorMessage: String?,
    focusRequester: FocusRequester,
    onPickGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onAttachmentPreviewClick: (DiagnosticAttachment) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit
) {
    val canSend = !isAnalyzing && !isProcessingImage && (inputText.isNotBlank() || pendingAttachments.isNotEmpty())
    var isInputFocused by remember { mutableStateOf(false) }

    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            isInputFocused -> palette.composerFocusedBorder
            inputText.isNotBlank() || pendingAttachments.isNotEmpty() -> BrandOrange.copy(alpha = 0.58f)
            else -> palette.composerUnfocusedBorder
        },
        animationSpec = tween(durationMillis = 180),
        label = "composerBorderColor"
    )

    val composerShape = RoundedCornerShape(24.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_composer_container")
            .background(Brush.verticalGradient(palette.composerDockGradient))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Error / Retry Banner
        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diag_retry_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMessage,
                        color = if (palette.isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                        fontSize = 11.5.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("diag_retry_button")
                    ) {
                        Text(
                            text = "თავიდან ცდა",
                            color = BrandOrange,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "დახურვა",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Image Processing Indicator
        if (isProcessingImage) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                CircularProgressIndicator(
                    color = BrandOrange,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "ფოტო მუშავდება...",
                    color = palette.textSecondary,
                    fontSize = 11.5.sp
                )
            }
        }

        // Pending Image Attachments Preview Strip
        if (pendingAttachments.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .testTag("diag_attachment_preview_row")
            ) {
                items(pendingAttachments, key = { it.id }) { att ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("diag_pending_attachment_${att.id}")
                    ) {
                        DiagnosticAttachmentThumbnail(
                            palette = palette,
                            attachment = att,
                            sizeDp = 72,
                            onClick = { onAttachmentPreviewClick(att) }
                        )

                        // Size badge
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = att.formattedSizeKb,
                                color = Color.White,
                                fontSize = 8.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        // Remove button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF14171E).copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(22.dp)
                                .clickable { onRemoveAttachment(att.id) }
                                .testTag("remove_attachment_${att.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "ფოტოს წაშლა",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Premium BEKASERVICE Glassmorphism Composer Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isInputFocused) 14.dp else 9.dp,
                    shape = composerShape,
                    ambientColor = if (isInputFocused) BrandOrange.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.35f),
                    spotColor = if (isInputFocused) BrandOrange.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.45f)
                )
                .clip(composerShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.composerGlassTop,
                            palette.composerGlassBottom
                        )
                    )
                )
                .border(
                    width = if (isInputFocused) 1.35.dp else 1.dp,
                    brush = if (isInputFocused) {
                        Brush.horizontalGradient(
                            listOf(
                                BrandOrange,
                                Color(0xFFFF9E44),
                                BrandOrange
                            )
                        )
                    } else {
                        SolidColor(animatedBorderColor)
                    },
                    shape = composerShape
                )
                .drawBehind {
                    // Delicate top-edge specular glass highlight
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                if (isInputFocused) BrandOrange.copy(alpha = 0.55f) else palette.composerTopSpecular,
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.08f, 1f),
                        end = Offset(size.width * 0.92f, 1f),
                        strokeWidth = 1.5f
                    )
                }
                .testTag("diag_composer_glass_pill")
        ) {
            // Subtle frosted blur layer on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(palette.composerFrostTint)
                        .blur(16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Gallery Picker Button (48dp accessible touch target, 38dp visual glass circle)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.composerButtonBg)
                        .border(1.dp, BrandOrange.copy(alpha = 0.42f), CircleShape)
                        .clickable(enabled = !isAnalyzing) { onPickGallery() }
                        .testTag("diag_attach_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "გალერეიდან ფოტოს მიმაგრება",
                        tint = BrandOrange,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Camera Capture Button (48dp accessible touch target, 38dp visual glass circle)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.composerButtonBg)
                        .border(1.dp, palette.borderSubtle, CircleShape)
                        .clickable(enabled = !isAnalyzing) { onTakePhoto() }
                        .testTag("diag_attach_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "კამერით ფოტოს გადაღება",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Multiline Glass Input Field
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    textStyle = TextStyle(
                        color = palette.textPrimary,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    ),
                    cursorBrush = SolidColor(BrandOrange),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) onSend()
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp, max = 96.dp)
                        .padding(horizontal = 4.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isInputFocused = focusState.isFocused
                        }
                        .testTag("diag_message_input"),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = if (pendingAttachments.isNotEmpty()) {
                                        "დაამატე კომენტარი ფოტოზე..."
                                    } else {
                                        "აღწერე ტელეფონის პრობლემა..."
                                    },
                                    color = palette.textPlaceholder,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Circular Send Button (48dp accessible touch target, 38dp visual circle)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) {
                                Brush.linearGradient(
                                    listOf(
                                        BrandOrange,
                                        Color(0xFFFF8524)
                                    )
                                )
                            } else {
                                SolidColor(palette.composerButtonBg)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (canSend) BrandOrange else palette.borderSubtle,
                            shape = CircleShape
                        )
                        .clickable(enabled = canSend) { onSend() }
                        .testTag("diag_send_button")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            color = BrandOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(17.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "გაგზავნა",
                            tint = if (canSend) Color.White else palette.textSecondary.copy(alpha = 0.65f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDiagnosticMarkdownText(raw: String, primaryTextColor: Color): AnnotatedString {
    if (!raw.contains("**")) return AnnotatedString(raw)
    return buildAnnotatedString {
        var idx = 0
        while (idx < raw.length) {
            val startBold = raw.indexOf("**", idx)
            if (startBold == -1) {
                append(raw.substring(idx))
                break
            }
            if (startBold > idx) {
                append(raw.substring(idx, startBold))
            }
            val endBold = raw.indexOf("**", startBold + 2)
            if (endBold == -1) {
                append(raw.substring(startBold + 2))
                break
            }
            val boldContent = raw.substring(startBold + 2, endBold)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryTextColor)) {
                append(boldContent)
            }
            idx = endBold + 2
        }
    }
}

// ==============================================================================
// 7. ATTACHMENT THUMBNAIL, FULL-SCREEN ZOOM DIALOG & HISTORY SHEET
// ==============================================================================

@Composable
private fun DiagnosticAttachmentThumbnail(
    palette: DiagnosticsThemePalette,
    attachment: DiagnosticAttachment,
    sizeDp: Int,
    onClick: () -> Unit
) {
    val bmp = remember(attachment.id, attachment.base64Data) {
        attachment.previewBitmap ?: DiagnosticImageProcessor.decodeBitmapFromBase64(attachment.base64Data)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.subSurfaceBg,
        border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.45f)),
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = attachment.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (attachment.uriString.isNotBlank() && !attachment.uriString.startsWith("bitmap://")) {
            AsyncImage(
                model = attachment.uriString,
                contentDescription = attachment.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = attachment.displayName,
                    tint = BrandOrange,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun DiagnosticAttachmentZoomDialog(
    palette: DiagnosticsThemePalette,
    attachment: DiagnosticAttachment,
    onDismiss: () -> Unit
) {
    val bmp = remember(attachment.id, attachment.base64Data) {
        attachment.previewBitmap ?: DiagnosticImageProcessor.decodeBitmapFromBase64(attachment.base64Data)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cardBg,
            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("diag_zoom_dialog")
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.displayName,
                            color = palette.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${attachment.width}x${attachment.height}px • ${attachment.formattedSizeKb}",
                            color = palette.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "დახურვა",
                            tint = palette.textPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 380.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.subSurfaceBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = attachment.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = attachment.uriString,
                            contentDescription = attachment.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (attachment.visualSummaryHint.isNotBlank()) {
                    Text(
                        text = "ვიზუალური პარამეტრები: ${attachment.visualSummaryHint}",
                        color = palette.textSecondary,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticHistorySheetDialog(
    palette: DiagnosticsThemePalette,
    sessions: List<DiagnosticSessionRecord>,
    activeSessionId: String,
    onOpenSession: (DiagnosticSessionRecord) -> Unit,
    onDeleteSession: (String) -> Unit,
    onStartNewSession: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.cardBg,
            border = BorderStroke(1.dp, palette.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 520.dp)
                .testTag("diag_history_sheet")
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
                    Column {
                        Text(
                            text = "დიაგნოსტიკის ისტორია",
                            color = palette.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "შენახული სესიების გაგრძელება ან წაშლა",
                            color = palette.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "დახურვა",
                            tint = palette.textPrimary
                        )
                    }
                }

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ჯერ არ გაქვთ შენახული დიაგნოსტიკის სესია.",
                            color = palette.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            val isCurrent = session.id == activeSessionId
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.subSurfaceBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) BrandOrange else palette.borderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSession(session) }
                                    .testTag("diag_history_open_${session.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = session.title,
                                                color = palette.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(5.dp),
                                                color = BrandOrange.copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = session.mode.labelKa,
                                                    color = BrandOrange,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = session.summary,
                                            color = palette.textSecondary,
                                            fontSize = 11.5.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${session.deviceHint} • ${session.messages.size} შეტყობინება • ${session.formattedDate}",
                                            color = palette.textSecondary.copy(alpha = 0.78f),
                                            fontSize = 10.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSession(session.id) },
                                        modifier = Modifier.testTag("diag_history_delete_${session.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "სესიის წაშლა",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onStartNewSession,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ახალი დიაგნოსტიკის დაწყება",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
