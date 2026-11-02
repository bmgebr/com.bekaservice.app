package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import kotlinx.coroutines.launch

// ============================================================================
// PHASE 2 — DIAGNOSTICS ENTRY CARD (FULL-WIDTH PROMINENT CARD)
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImeiCheckDiagnosticsEntryCard(
    config: ImeiServiceConfig,
    historyCount: Int,
    onOpenImeiCheck: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = false
) {
    if (!isPreviewMode && !config.entryCardVisible) return

    val context = LocalContext.current
    val resolvedImageModel: Any = remember(config.cardImageUrl) {
        val raw = config.cardImageUrl.trim()
        when {
            raw.isBlank() -> ImeiServiceConfig.DEFAULT_CARD_IMAGE_URL
            raw.startsWith("/") -> File(raw)
            else -> raw
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("imei_diagnostics_entry_card")
            .clickable(enabled = !isPreviewMode) { onOpenImeiCheck() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12161E)),
        border = BorderStroke(1.5.dp, Color(0xFFFF6B00).copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Hero Photo Banner — clean & unobstructed so the main card photo is clearly visible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .background(Color(0xFF0B0E14))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(resolvedImageModel)
                        .crossfade(true)
                        .build(),
                    contentDescription = config.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle bottom-only gradient so the photo remains bright and clear
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x26000000),
                                    Color.Transparent,
                                    Color(0xCC12161E)
                                )
                            )
                        )
                )

                // Floating Top Corner Badges (Price & Turnaround Time)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFFFF6B00),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "IMEI CHECK • ${config.formattedPrice}",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF0D1118).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = config.formattedEstimatedTime,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2. Clean, Uncluttered Content & Full-Visibility Action Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF6B00).copy(alpha = 0.16f))
                                .border(1.dp, Color(0xFFFF6B00), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "IMEI ვერიფიკაცია",
                                tint = Color(0xFFFF6B00),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = config.title.ifBlank { "IMEI CHECK • სრული შემოწმება" },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "შეამოწმე სანამ იყიდი • შეამოწმე სრული ისტორია",
                                color = Color(0xFFFFB074),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.sp,
                                maxLines = 2
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenHistory,
                        enabled = !isPreviewMode,
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("open_imei_history_from_card_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF181E29),
                            contentColor = Color(0xFFE2E8F0)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "IMEI ისტორია",
                            tint = Color(0xFFFF6B00),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (historyCount > 0) "ისტორია ($historyCount)" else "ისტორია",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                if (!config.serviceEnabled || !config.acceptingRequests) {
                    Surface(
                        color = Color(0xFF3B1219),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = config.maintenanceMessage,
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Full-width Primary CTA Button — "IMEI შემოწმების დაწყება" is 100% visible without truncation
                Button(
                    onClick = onOpenImeiCheck,
                    enabled = !isPreviewMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_imei_check_screen_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B00),
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IMEI შემოწმების დაწყება",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ImeiInfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color
) {
    Surface(
        color = accentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = Color(0xFFF1F5F9),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// PHASE 3 & PHASE 8 — FULL-SCREEN IMEI CHECK PAGE + SMART REPEAT CHECK
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImeiCheckFullScreen(
    currentUser: UserProfile?,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenReport: (String) -> Unit,
    onRequireAuth: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateVersion = ImeiCheckManager.stateVersion.intValue

    val config = remember(stateVersion) { ImeiCheckManager.loadConfig(context) }
    val brands = remember(stateVersion) {
        ImeiCheckManager.loadBrands(context).filter { it.enabled }.sortedBy { it.displayOrder }
    }
    val resolvedUid = remember(currentUser, stateVersion) {
        ImeiCheckManager.resolveCurrentUserId(context, currentUser)
    }
    val userHistory = remember(resolvedUid, stateVersion) {
        if (resolvedUid.isNotBlank()) ImeiCheckManager.loadUserHistoryLocal(context, resolvedUid) else emptyList()
    }

    var imeiInput by remember { mutableStateOf("") }
    var selectedBrandId by remember(brands) {
        mutableStateOf(brands.firstOrNull()?.id ?: "apple")
    }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorBanner by remember { mutableStateOf<String?>(null) }
    var bypassRepeatCheckWarning by remember { mutableStateOf(false) }
    var submittedEntry by remember { mutableStateOf<ImeiHistoryEntry?>(null) }

    val validation = remember(imeiInput) { ImeiValidator.validateImei(imeiInput) }
    val selectedBrand = remember(brands, selectedBrandId) {
        brands.find { it.id == selectedBrandId } ?: brands.firstOrNull()
    }

    // Phase 8: Smart Repeat Check (strictly scoped to the SAME authenticated user)
    val previousSameUserEntry = remember(imeiInput, validation.isValid, resolvedUid, stateVersion) {
        if (validation.isValid && resolvedUid.isNotBlank()) {
            ImeiCheckManager.findPreviousCheckForCurrentUser(context, resolvedUid, validation.cleanedDigits)
        } else {
            null
        }
    }

    LaunchedEffect(imeiInput) {
        bypassRepeatCheckWarning = false
        errorBanner = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C10))
            .testTag("imei_check_full_screen")
    ) {
        // Top Navigation Bar
        Surface(
            color = Color(0xFF11151C),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A202C))
                            .testTag("imei_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "უკან დაბრუნება",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(
                            text = config.title.ifBlank { "IMEI CHECK • სრული შემოწმება" },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "შეამოწმე სანამ იყიდი • სრული ისტორია",
                            color = Color(0xFFFF6B00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenHistory,
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .testTag("imei_screen_history_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B00)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "ჩემი შემოწმებები",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "ისტორია (${userHistory.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Service Overview & Price / Time Banner — "სერვისის პირობები" is completely visible without truncation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                border = BorderStroke(1.dp, Color(0xFF263042))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFFFF6B00),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "სერვისის პირობები",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 18.sp
                            )
                        }

                        Surface(
                            color = Color(0xFF22C55E).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "ტარიფი: ${config.formattedPrice}",
                                color = Color(0xFF4ADE80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = config.detailedDescription,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0B0E14),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "სავარაუდო დრო",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = config.formattedEstimatedTime,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0B0E14),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "როგორ გავიგო IMEI?",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "აკრიფეთ *#06#",
                                    color = Color(0xFFFF6B00),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. Submission Confirmation Card if just submitted
            AnimatedVisibility(visible = submittedEntry != null) {
                submittedEntry?.let { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("imei_submission_success_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F291E)),
                        border = BorderStroke(1.5.dp, Color(0xFF22C55E))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "მოთხოვნა წარმატებით გაიგზავნა!",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = "ბრენდი: ${entry.brandName} • IMEI: ${entry.imeiMasked} • ღირებულება: ${entry.formattedPrice}",
                                color = Color(0xFFD1FAE5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "სტატუსის განახლებისა და სრული რეპორტის მომზადებისთანავე მიიღებთ შეტყობინებას.",
                                color = Color(0xFFA7F3D0),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onOpenHistory,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF22C55E),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(
                                        text = "ჩემი შემოწმებების ნახვა",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                                OutlinedButton(
                                    onClick = { submittedEntry = null },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF4ADE80))
                                ) {
                                    Text(
                                        text = "დახურვა",
                                        color = Color(0xFF4ADE80),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. 15-Digit IMEI Input & Luhn Checksum Section (Highly Prominent & Clear)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131826)),
                border = BorderStroke(
                    width = 2.dp,
                    color = when {
                        imeiInput.isEmpty() -> Color(0xFFFF6B00)
                        validation.isValid -> Color(0xFF22C55E)
                        else -> Color(0xFFF59E0B)
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row with glowing icon badge, title and digit counter
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
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF6B00),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Dialpad,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "IMEI კოდის შეყვანა",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Surface(
                            color = if (validation.isFifteenDigits && validation.isLuhnValid) {
                                Color(0xFF166534)
                            } else {
                                Color(0xFF1E293B)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${validation.cleanedDigits.length} / 15 ციფრი",
                                color = if (validation.isFifteenDigits && validation.isLuhnValid) {
                                    Color(0xFF4ADE80)
                                } else {
                                    Color(0xFFFFB074)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "შეიყვანეთ ტელეფონის 15-ნიშნა კოდი გადამოწმებისთვის",
                        color = Color(0xFFFFB074),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Dial code helper chip: single clean row with separated dial badge
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFF6B00).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9E44),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "კოდის გასაგებად აკრიფეთ:",
                                    color = Color(0xFFFFD7A8),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Surface(
                                color = Color(0xFFFF6B00),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "*#06#",
                                    color = Color.Black,
                                    fontSize = 12.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // High-contrast Monospace IMEI Input Field
                    OutlinedTextField(
                        value = imeiInput,
                        onValueChange = { raw ->
                            val digitsOnly = ImeiValidator.normalizeDigits(raw)
                            imeiInput = digitsOnly
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("imei_input_field"),
                        placeholder = {
                            Text(
                                text = "მაგ: 353912101234566",
                                color = Color(0xFF64748B),
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                tint = Color(0xFFFF6B00)
                            )
                        },
                        trailingIcon = if (imeiInput.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { imeiInput = "" },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("imei_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "გასუფთავება",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFFFF8800).copy(alpha = 0.7f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF090D15),
                            unfocusedContainerColor = Color(0xFF090D15),
                            cursorColor = Color(0xFFFF6B00)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Separate Dedicated Paste Button (Never overlaps or interferes with Clear)
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val pasted = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            val digits = ImeiValidator.normalizeDigits(pasted)
                            if (digits.isNotEmpty()) {
                                imeiInput = digits
                                Toast.makeText(context, "IMEI ჩასმულია ბუფერიდან: $digits", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "ბუფერში ციფრები ვერ მოიძებნა", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .testTag("imei_paste_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFF6B00).copy(alpha = 0.12f),
                            contentColor = Color(0xFFFF8800)
                        ),
                        border = BorderStroke(1.2.dp, Color(0xFFFF6B00).copy(alpha = 0.75f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "ბუფერიდან ჩასმა",
                            tint = Color(0xFFFF8800),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📋 ბუფერიდან ჩასმა (Paste)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = Color(0xFFFFD7A8)
                        )
                    }

                    // Luhn validation status row
                    if (imeiInput.isNotEmpty()) {
                        val isOk = validation.isValid
                        Surface(
                            color = if (isOk) Color(0xFF0F291E) else Color(0xFF2D1B14),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isOk) Color(0xFF22C55E).copy(alpha = 0.5f) else Color(0xFFF97316).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isOk) Color(0xFF4ADE80) else Color(0xFFFB923C),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isOk) {
                                            "✓ Luhn კონტროლის ჯამი სწორია (TAC: ${validation.tac})"
                                        } else {
                                            validation.errorMessageKa ?: "არასწორი IMEI ფორმატი"
                                        },
                                        color = if (isOk) Color(0xFF86EFAC) else Color(0xFFFED7AA),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Mandatory Luhn Disclaimer (Phase 3.2)
                    Text(
                        text = "ℹ️ შენიშვნა: Luhn ალგორითმით მოწმდება მხოლოდ 15-ნიშნა IMEI კოდის მათემატიკური სტრუქტურა. მოწყობილობის რეალური მოდელი, iCloud/FMI, SimLock და Blacklist სტატუსი დასტურდება უშუალოდ BEKASERVICE-ის სპეციალისტის მიერ ოფიციალურ ბაზებში გადამოწმების შემდეგ.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // 4. Phase 8: Smart Repeat Check Warning Banner (if same user already checked this IMEI)
            AnimatedVisibility(visible = previousSameUserEntry != null && !bypassRepeatCheckWarning) {
                previousSameUserEntry?.let { prev ->
                    val prevStatus = prev.statusEnum
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("imei_smart_repeat_check_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B12)),
                        border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ეს IMEI თქვენ უკვე შემოწმებული გაქვთ",
                                    color = Color(0xFFFDE68A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Text(
                                text = "ბრენდი: ${prev.brandName}${if (prev.verifiedModel.isNotBlank()) " • ${prev.verifiedModel}" else ""}\n" +
                                    "თარიღი: ${ImeiCheckManager.formatTimestampKa(prev.createdAtMillis)} • სტატუსი: ${prevStatus.labelKa}",
                                color = Color(0xFFFEF3C7),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onOpenReport(prev.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("open_previous_imei_report_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "წინა რეპორტის გახსნა",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = { bypassRepeatCheckWarning = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("proceed_repeat_imei_check_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFBBF24))
                                ) {
                                    Text(
                                        text = "ახალი შემოწმება",
                                        color = Color(0xFFFDE68A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Phase 3.3: Dynamic Manufacturer Selection Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                border = BorderStroke(1.dp, Color(0xFF263042))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. აირჩიეთ მწარმოებელი",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        selectedBrand?.let {
                            Surface(
                                color = Color(0xFFFF6B00).copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF6B00).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "✓ ${it.localizedName.replace(" (Other)", "")}",
                                    color = Color(0xFFFF6B00),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Responsive 3-column grid using chunked rows for clean vertical scrolling
                    val rows = remember(brands) { brands.chunked(3) }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowBrands ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowBrands.forEach { brand ->
                                    val isSelected = brand.id == selectedBrandId
                                    ImeiBrandGridCard(
                                        brand = brand,
                                        isSelected = isSelected,
                                        onClick = { selectedBrandId = brand.id },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rowBrands.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 6. Instructions & Privacy Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                border = BorderStroke(1.dp, Color(0xFF263042))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ინსტრუქცია და კონფიდენციალურობა",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = config.userInstructions,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Text(
                        text = "🔒 ${config.privacyNotice}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Error Banner if any
            errorBanner?.let { err ->
                Surface(
                    color = Color(0xFF3B1219),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFF87171)
                        )
                        Text(
                            text = err,
                            color = Color(0xFFFECACA),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 7. Submit Button
            val canSubmit = config.serviceEnabled &&
                config.acceptingRequests &&
                validation.isValid &&
                selectedBrand != null &&
                !isSubmitting

            Button(
                onClick = {
                    if (currentUser == null && ImeiCheckManager.resolveCurrentUserId(context, null).isBlank()) {
                        onRequireAuth()
                        return@Button
                    }
                    val brand = selectedBrand ?: return@Button
                    isSubmitting = true
                    errorBanner = null
                    scope.launch {
                        val res = ImeiCheckManager.submitImeiCheckRequest(
                            context = context,
                            rawImei = imeiInput,
                            selectedBrand = brand,
                            currentUser = currentUser,
                            allowRepeatSubmission = bypassRepeatCheckWarning || previousSameUserEntry != null
                        )
                        isSubmitting = false
                        res.onSuccess { entry ->
                            submittedEntry = entry
                            imeiInput = ""
                            Toast.makeText(
                                context,
                                "IMEI შემოწმების მოთხოვნა მიღებულია!",
                                Toast.LENGTH_LONG
                            ).show()
                        }.onFailure { e ->
                            errorBanner = e.message ?: "მოთხოვნის გაგზავნა ვერ მოხერხდა"
                        }
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_imei_request_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B00),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color(0xFF64748B)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "იგზავნება...",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "შემოწმებაზე გაგზავნა • ${config.formattedPrice}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))
        }
    }
}

@Composable
private fun ImeiBrandGridCard(
    brand: ImeiBrandConfig,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanDisplayName = remember(brand.localizedName, brand.name) {
        val raw = brand.localizedName.ifBlank { brand.name }
        if (raw.contains("სხვა")) "სხვა ბრენდი" else raw
    }
    Surface(
        modifier = modifier
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("imei_brand_item_${brand.id}"),
        color = if (isSelected) Color(0xFFFF6B00).copy(alpha = 0.16f) else Color(0xFF0B0E14),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) Color(0xFFFF6B00) else Color(0xFF1E293B)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RealBrandLogoBadge(
                brand = brand,
                size = 36.dp
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = cleanDisplayName,
                color = if (isSelected) Color(0xFFFF6B00) else Color(0xFFE2E8F0),
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * რეალური, ვექტორული მწარმოებლის ლოგოები ყველა ბრენდისთვის (Apple, Samsung, Xiaomi, Google Pixel,
 * Huawei, Honor, OnePlus, Motorola, Oppo, Vivo, Realme, Sony, Nokia, Asus, Nothing და სხვა).
 * თუ ადმინისტრატორს მითითებული აქვს საკუთარი ლოგოს URL/ფაილი, იტვირთება სურათი, ხოლო სტანდარტულად
 * იხატება ბრენდის ორიგინალი ვექტორული ლოგო (გამოგონილი ემოჯების გარეშე).
 */
@Composable
fun RealBrandLogoBadge(
    brand: ImeiBrandConfig,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val normalizedKey = remember(brand.id, brand.name) {
        "${brand.id.lowercase()} ${brand.name.lowercase()}"
    }

    if (brand.logoUrl.isNotBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF161C26))
                .border(1.dp, Color(0xFF2B3548), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(if (brand.logoUrl.startsWith("/")) File(brand.logoUrl) else brand.logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = brand.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            )
        }
        return
    }

    when {
        normalizedKey.contains("apple") || normalizedKey.contains("iphone") -> {
            // Authentic Apple vector silhouette (bitten apple + top leaf) on dark brushed badge
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF262B36), Color(0xFF141820))
                        )
                    )
                    .border(1.dp, Color(0xFF3E4756), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.62f)) {
                    val w = this.size.width
                    val h = this.size.height
                    // Apple leaf
                    val leafPath = Path().apply {
                        moveTo(w * 0.51f, h * 0.23f)
                        cubicTo(w * 0.51f, h * 0.11f, w * 0.63f, h * 0.02f, w * 0.72f, h * 0.02f)
                        cubicTo(w * 0.73f, h * 0.13f, w * 0.62f, h * 0.23f, w * 0.51f, h * 0.23f)
                        close()
                    }
                    drawPath(leafPath, color = Color.White)

                    // Apple body with right-side bite
                    val bodyPath = Path().apply {
                        moveTo(w * 0.50f, h * 0.28f)
                        cubicTo(w * 0.40f, h * 0.28f, w * 0.33f, h * 0.22f, w * 0.24f, h * 0.23f)
                        cubicTo(w * 0.11f, h * 0.24f, w * 0.02f, h * 0.38f, w * 0.03f, h * 0.56f)
                        cubicTo(w * 0.04f, h * 0.76f, w * 0.18f, h * 0.98f, w * 0.31f, h * 0.98f)
                        cubicTo(w * 0.38f, h * 0.98f, w * 0.42f, h * 0.93f, w * 0.50f, h * 0.93f)
                        cubicTo(w * 0.58f, h * 0.93f, w * 0.61f, h * 0.98f, w * 0.69f, h * 0.98f)
                        cubicTo(w * 0.80f, h * 0.98f, w * 0.90f, h * 0.82f, w * 0.93f, h * 0.72f)
                        cubicTo(w * 0.80f, h * 0.66f, w * 0.77f, h * 0.51f, w * 0.88f, h * 0.41f)
                        cubicTo(w * 0.82f, h * 0.30f, w * 0.72f, h * 0.24f, w * 0.63f, h * 0.24f)
                        cubicTo(w * 0.56f, h * 0.24f, w * 0.53f, h * 0.28f, w * 0.50f, h * 0.28f)
                        close()
                    }
                    drawPath(bodyPath, color = Color.White)
                }
            }
        }

        normalizedKey.contains("samsung") -> {
            // Authentic Samsung tilted blue oval badge + geometric SAMSUNG wordmark
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0C1838))
                    .border(1.dp, Color(0xFF1E3A8A), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                    rotate(degrees = -10f) {
                        drawOval(
                            color = Color(0xFF1428A0),
                            topLeft = Offset(this.size.width * 0.03f, this.size.height * 0.22f),
                            size = Size(this.size.width * 0.94f, this.size.height * 0.56f)
                        )
                    }
                }
                Text(
                    text = "SAMSUNG",
                    color = Color.White,
                    fontSize = 6.2.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp,
                    maxLines = 1
                )
            }
        }

        normalizedKey.contains("xiaomi") || normalizedKey == "mi" -> {
            // Authentic Xiaomi #FF6900 squircle with vector "mi" paths
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFFFF6900)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.58f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val strokeW = w * 0.13f
                    // Left 'm' arch with rounded top-right corner
                    val mPath = Path().apply {
                        moveTo(w * 0.08f, h * 0.78f)
                        lineTo(w * 0.08f, h * 0.24f)
                        lineTo(w * 0.48f, h * 0.24f)
                        cubicTo(w * 0.60f, h * 0.24f, w * 0.64f, h * 0.30f, w * 0.64f, h * 0.42f)
                        lineTo(w * 0.64f, h * 0.78f)
                    }
                    drawPath(
                        path = mPath,
                        color = Color.White,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Middle vertical leg of 'm'
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.36f, h * 0.44f),
                        end = Offset(w * 0.36f, h * 0.78f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    // Right 'i' vertical bar
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.88f, h * 0.24f),
                        end = Offset(w * 0.88f, h * 0.78f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        normalizedKey.contains("google") || normalizedKey.contains("pixel") -> {
            // Authentic 4-Color Google 'G' vector emblem
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFFFFF))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.62f)) {
                    val strokeW = this.size.width * 0.22f
                    val pad = strokeW / 2f
                    val arcSize = Size(this.size.width - strokeW, this.size.height - strokeW)
                    val topLeft = Offset(pad, pad)

                    // Red top arc
                    drawArc(
                        color = Color(0xFFEA4335),
                        startAngle = 200f,
                        sweepAngle = 115f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    // Yellow left arc
                    drawArc(
                        color = Color(0xFFFBBC05),
                        startAngle = 140f,
                        sweepAngle = 65f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    // Green bottom arc
                    drawArc(
                        color = Color(0xFF34A853),
                        startAngle = 45f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    // Blue right-bottom arc + horizontal crossbar
                    drawArc(
                        color = Color(0xFF4285F4),
                        startAngle = -15f,
                        sweepAngle = 65f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    drawLine(
                        color = Color(0xFF4285F4),
                        start = Offset(this.size.width * 0.50f, this.size.height * 0.50f),
                        end = Offset(this.size.width * 0.94f, this.size.height * 0.50f),
                        strokeWidth = strokeW * 0.92f,
                        cap = StrokeCap.Butt
                    )
                }
            }
        }

        normalizedKey.contains("huawei") -> {
            // Authentic Huawei red 8-petal radiating fan logo
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A0F14))
                    .border(1.dp, Color(0xFFCF0A2C).copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.68f)) {
                    val cx = this.size.width * 0.5f
                    val cy = this.size.height * 0.78f
                    val angles = listOf(-68f, -48f, -28f, -9f, 9f, 28f, 48f, 68f)
                    angles.forEach { angle ->
                        rotate(degrees = angle, pivot = Offset(cx, cy)) {
                            drawRoundRect(
                                color = Color(0xFFCF0A2C),
                                topLeft = Offset(cx - this.size.width * 0.055f, this.size.height * 0.14f),
                                size = Size(this.size.width * 0.11f, this.size.height * 0.52f),
                                cornerRadius = CornerRadius(this.size.width * 0.06f, this.size.width * 0.06f)
                            )
                        }
                    }
                }
            }
        }

        normalizedKey.contains("honor") -> {
            // Authentic HONOR cyan-magenta gradient wordmark badge
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF2E93))),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HONOR",
                    color = Color.White,
                    fontSize = 7.2.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    maxLines = 1
                )
            }
        }

        normalizedKey.contains("oneplus") -> {
            // Authentic OnePlus red frame with '+' in top-right corner and '1' inside
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF5010C)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.64f)) {
                    val w = this.size.width
                    val h = this.size.height
                    val strokeW = w * 0.11f
                    // Open square frame (left, bottom, right-bottom, top-left)
                    val framePath = Path().apply {
                        moveTo(w * 0.56f, h * 0.16f)
                        lineTo(w * 0.14f, h * 0.16f)
                        lineTo(w * 0.14f, h * 0.86f)
                        lineTo(w * 0.84f, h * 0.86f)
                        lineTo(w * 0.84f, h * 0.44f)
                    }
                    drawPath(framePath, color = Color.White, style = Stroke(width = strokeW, cap = StrokeCap.Square))
                    // '+' in top-right
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.66f, h * 0.20f),
                        end = Offset(w * 0.94f, h * 0.20f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Square
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.80f, h * 0.06f),
                        end = Offset(w * 0.80f, h * 0.34f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Square
                    )
                    // '1' in center
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.49f, h * 0.34f),
                        end = Offset(w * 0.49f, h * 0.68f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Square
                    )
                }
            }
        }

        normalizedKey.contains("motorola") -> {
            // Authentic Motorola circle with iconic Batwing 'M' vector path
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0E1B33))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(size * 0.68f)) {
                    val w = this.size.width
                    val h = this.size.height
                    drawCircle(color = Color(0xFF0284C7), radius = w * 0.48f)
                    val mPath = Path().apply {
                        moveTo(w * 0.20f, h * 0.74f)
                        lineTo(w * 0.37f, h * 0.22f)
                        lineTo(w * 0.50f, h * 0.56f)
                        lineTo(w * 0.63f, h * 0.22f)
                        lineTo(w * 0.80f, h * 0.74f)
                        lineTo(w * 0.69f, h * 0.74f)
                        cubicTo(w * 0.65f, h * 0.54f, w * 0.61f, h * 0.48f, w * 0.50f, h * 0.68f)
                        cubicTo(w * 0.39f, h * 0.48f, w * 0.35f, h * 0.54f, w * 0.31f, h * 0.74f)
                        close()
                    }
                    drawPath(mPath, color = Color.White)
                }
            }
        }

        normalizedKey.contains("oppo") -> {
            // Authentic OPPO #008A45 emerald green badge + lowercase geometric "oppo" wordmark
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF046A38))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "oppo",
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1
                )
            }
        }

        normalizedKey.contains("vivo") -> {
            // Authentic Vivo #415FFF royal blue badge + geometric "vivo" wordmark
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF415FFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "vivo",
                    color = Color.White,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    maxLines = 1
                )
            }
        }

        normalizedKey.contains("realme") -> {
            // Authentic Realme #FFC915 yellow badge + bold black "realme" wordmark
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFC915)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "realme",
                    color = Color.Black,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.2.sp,
                    maxLines = 1
                )
            }
        }

        normalizedKey.contains("sony") -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF111827))
                    .border(1.dp, Color(0xFF4B5563), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SONY",
                    color = Color.White,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    maxLines = 1
                )
            }
        }

        normalizedKey.contains("nokia") -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF005AFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NOKIA",
                    color = Color.White,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.4.sp,
                    maxLines = 1
                )
            }
        }

        else -> {
            // Clean hardware device vector badge for "Other" or custom added brands
            val brandMonogram = remember(brand.name) {
                val clean = brand.name.trim()
                if (clean.equals("Other", ignoreCase = true) || clean.contains("სხვა")) {
                    "ALL"
                } else {
                    clean.take(4).uppercase()
                }
            }
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF182030))
                    .border(1.dp, Color(0xFFFF6B00).copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (brandMonogram == "ALL") {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = brand.name,
                        tint = Color(0xFFFF6B00),
                        modifier = Modifier.size(size * 0.56f)
                    )
                } else {
                    Text(
                        text = brandMonogram,
                        color = Color(0xFFFF6B00),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ============================================================================
// PHASE 7 — CUSTOMER IMEI HISTORY & SECURE DELETION SCREEN
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImeiCustomerHistoryScreen(
    currentUser: UserProfile?,
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
    onStartNewCheck: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateVersion = ImeiCheckManager.stateVersion.intValue

    val resolvedUid = remember(currentUser, stateVersion) {
        ImeiCheckManager.resolveCurrentUserId(context, currentUser)
    }

    var historyList by remember(resolvedUid, stateVersion) {
        mutableStateOf(
            if (resolvedUid.isNotBlank()) ImeiCheckManager.loadUserHistoryLocal(context, resolvedUid)
            else emptyList()
        )
    }

    DisposableEffect(resolvedUid) {
        val reg = if (resolvedUid.isNotBlank()) {
            ImeiCheckManager.listenToUserImeiHistory(context, resolvedUid) { cloudList ->
                historyList = cloudList
            }
        } else null
        onDispose { reg?.remove() }
    }

    var entryPendingDelete by remember { mutableStateOf<ImeiHistoryEntry?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var isProcessingDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C10))
            .testTag("imei_customer_history_screen")
    ) {
        // Header
        Surface(
            color = Color(0xFF11151C),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A202C))
                            .testTag("imei_history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "უკან დაბრუნება",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                        Text(
                            text = "ჩემი IMEI შემოწმებები",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "სულ: ${historyList.size} ჩანაწერი • პირადი არქივი",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (historyList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showClearAllDialog = true },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("imei_clear_all_history_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "მთელი ისტორიის გასუფთავება",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "გასუფთავება",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                    border = BorderStroke(1.dp, Color(0xFF263042)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFFFF6B00),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "თქვენი IMEI შემოწმების ისტორია ცარიელია",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "აქ შეინახება თქვენ მიერ გაგზავნილი ყველა IMEI მოთხოვნა, სტატუსი და სრული ვერიფიკაციის რეპორტი.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                        Button(
                            onClick = onStartNewCheck,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B00),
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ახალი IMEI შემოწმება",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList, key = { it.id }) { entry ->
                    ImeiHistoryItemCard(
                        entry = entry,
                        onOpenReport = { onOpenReport(entry.id) },
                        onDeleteClick = { entryPendingDelete = entry }
                    )
                }
            }
        }
    }

    // Individual History Item Delete Confirmation Dialog (Phase 7.2)
    entryPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!isProcessingDelete) entryPendingDelete = null },
            containerColor = Color(0xFF141923),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = {
                Text(
                    text = "ჩანაწერის წაშლა ისტორიიდან",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "ნამდვილად გსურთ წაშალოთ ${target.brandName} (${target.imeiMasked}) თქვენი პირადი IMEI ისტორიიდან?\n\n" +
                        "წაშლის შემდეგ სრული IMEI და პირადი რეპორტი სამუდამოდ წაიშლება.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingDelete = true
                        scope.launch {
                            val res = ImeiCheckManager.deleteUserHistoryEntry(
                                context = context,
                                currentUserId = resolvedUid,
                                entryId = target.id
                            )
                            isProcessingDelete = false
                            entryPendingDelete = null
                            res.onSuccess {
                                historyList = ImeiCheckManager.loadUserHistoryLocal(context, resolvedUid)
                                Toast.makeText(context, "ჩანაწერი წაიშალა ისტორიიდან", Toast.LENGTH_SHORT).show()
                            }.onFailure { e ->
                                Toast.makeText(context, e.message ?: "წაშლა ვერ მოხერხდა", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isProcessingDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_imei_history_item_button")
                ) {
                    Text("წაშლა", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { entryPendingDelete = null },
                    enabled = !isProcessingDelete
                ) {
                    Text("გაუქმება", color = Color(0xFFCBD5E1))
                }
            }
        )
    }

    // Clear Entire History Confirmation Dialog (Phase 7.3)
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessingDelete) showClearAllDialog = false },
            containerColor = Color(0xFF141923),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1),
            title = {
                Text(
                    text = "მთელი ისტორიის გასუფთავება",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "ნამდვილად გსურთ მთლიანად გაასუფთავოთ თქვენი IMEI შემოწმების ისტორია (${historyList.size} ჩანაწერი)?\n\n" +
                        "ყველა პირადი რეპორტი და სრული IMEI მონაცემი სამუდამოდ წაიშლება.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingDelete = true
                        scope.launch {
                            val res = ImeiCheckManager.clearAllUserHistory(
                                context = context,
                                currentUserId = resolvedUid
                            )
                            isProcessingDelete = false
                            showClearAllDialog = false
                            res.onSuccess {
                                historyList = emptyList()
                                Toast.makeText(context, "IMEI ისტორია სრულად გასუფთავდა", Toast.LENGTH_SHORT).show()
                            }.onFailure { e ->
                                Toast.makeText(context, e.message ?: "გასუფთავება ვერ მოხერხდა", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isProcessingDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_clear_all_imei_history_button")
                ) {
                    Text("მთელი ისტორიის წაშლა", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearAllDialog = false },
                    enabled = !isProcessingDelete
                ) {
                    Text("გაუქმება", color = Color(0xFFCBD5E1))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImeiHistoryItemCard(
    entry: ImeiHistoryEntry,
    onOpenReport: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val st = entry.statusEnum
    val paySt = entry.paymentStatusEnum
    val statusColor = when (st) {
        ImeiRequestStatus.COMPLETED -> Color(0xFF22C55E)
        ImeiRequestStatus.IN_PROGRESS -> Color(0xFF38BDF8)
        ImeiRequestStatus.WAITING_FOR_INFO -> Color(0xFFF59E0B)
        ImeiRequestStatus.REJECTED -> Color(0xFFEF4444)
        ImeiRequestStatus.PENDING -> Color(0xFFA855F7)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("imei_history_item_${entry.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (entry.verifiedModel.isNotBlank()) {
                            "${entry.brandName} • ${entry.verifiedModel}"
                        } else {
                            entry.brandName
                        },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "IMEI: ${entry.imeiMasked}",
                        color = Color(0xFFFFB074),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = st.shortLabelKa,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ImeiInfoPill(
                    icon = Icons.Default.Payments,
                    label = "ტარიფი: ${entry.formattedPrice} (${paySt.labelKa})",
                    accentColor = Color(0xFF22C55E)
                )
                ImeiInfoPill(
                    icon = Icons.Default.CalendarToday,
                    label = ImeiCheckManager.formatTimestampKa(entry.createdAtMillis),
                    accentColor = Color(0xFF94A3B8)
                )
                if (entry.completedAtMillis > 0L) {
                    ImeiInfoPill(
                        icon = Icons.Default.DoneAll,
                        label = "დასრულდა: ${ImeiCheckManager.formatTimestampKa(entry.completedAtMillis)}",
                        accentColor = Color(0xFF38BDF8)
                    )
                }
            }

            if (entry.customerNotes.isNotBlank()) {
                Surface(
                    color = Color(0xFF0B0E14),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💬 სპეციალისტის კომენტარი: ${entry.customerNotes}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenReport,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("open_imei_report_button_${entry.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B00),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (st == ImeiRequestStatus.COMPLETED) "სრული რეპორტის გახსნა" else "სტატუსის / დეტალების ნახვა",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2B1216))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .testTag("delete_imei_history_item_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "ისტორიიდან წაშლა",
                        tint = Color(0xFFF87171)
                    )
                }
            }
        }
    }
}

// ============================================================================
// PHASE 6 — CUSTOMER RESULT REPORT SCREEN & PDF EXPORT
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImeiCustomerReportScreen(
    requestId: String,
    currentUser: UserProfile?,
    isAdminCaller: Boolean = false,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateVersion = ImeiCheckManager.stateVersion.intValue

    val resolvedUid = remember(currentUser, stateVersion) {
        ImeiCheckManager.resolveCurrentUserId(context, currentUser)
    }

    var report by remember { mutableStateOf<ImeiVerificationReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isExportingPdf by remember { mutableStateOf(false) }

    LaunchedEffect(requestId, resolvedUid, stateVersion) {
        isLoading = true
        val res = ImeiCheckManager.fetchReportForRequest(
            context = context,
            requestId = requestId,
            currentUserId = resolvedUid,
            isAdminCaller = isAdminCaller
        )
        isLoading = false
        res.onSuccess { rep ->
            report = rep
            errorMessage = null
        }.onFailure { e ->
            errorMessage = e.message ?: "რეპორტის ჩატვირთვა ვერ მოხერხდა"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C10))
            .testTag("imei_customer_report_screen")
    ) {
        // Header
        Surface(
            color = Color(0xFF11151C),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A202C))
                            .testTag("imei_report_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "უკან დაბრუნება",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "IMEI ვერიფიკაციის რეპორტი",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ID: $requestId",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                report?.let { rep ->
                    Button(
                        onClick = {
                            if (isExportingPdf) return@Button
                            isExportingPdf = true
                            scope.launch {
                                val pdfRes = ImeiCheckManager.generateReportPdfFile(context, rep)
                                isExportingPdf = false
                                pdfRes.onSuccess { pdfFile ->
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            pdfFile
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(
                                                Intent.EXTRA_SUBJECT,
                                                "BEKASERVICE IMEI Report - ${rep.brandName} (${rep.imeiMasked})"
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "PDF რეპორტის გაზიარება / შენახვა")
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            "PDF ფაილი შენახულია: ${pdfFile.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(context, "PDF გენერაცია ვერ მოხერხდა", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isExportingPdf,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("export_imei_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B00),
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF ჩამოტვირთვა",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PDF ამონაწერი",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF6B00))
            }
        } else if (errorMessage != null || report == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "რეპორტი არ მოიძებნა",
                    color = Color(0xFFF87171),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val rep = report!!
            val reqStatus = rep.statusEnum
            val statusColor = when (reqStatus) {
                ImeiRequestStatus.COMPLETED -> Color(0xFF22C55E)
                ImeiRequestStatus.IN_PROGRESS -> Color(0xFF38BDF8)
                ImeiRequestStatus.WAITING_FOR_INFO -> Color(0xFFF59E0B)
                ImeiRequestStatus.REJECTED -> Color(0xFFEF4444)
                ImeiRequestStatus.PENDING -> Color(0xFFA855F7)
            }

            val importantFields = remember(rep.fields) { rep.fields.filter { it.isImportant } }
            val otherFields = remember(rep.fields) { rep.fields.filterNot { it.isImportant } }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                    border = BorderStroke(1.5.dp, statusColor.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    text = rep.brandName,
                                    color = Color(0xFFFF6B00),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = rep.verifiedModel.ifBlank { "${rep.brandName} (მოდელი ზუსტდება)" },
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                color = statusColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = reqStatus.labelKa,
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    maxLines = 1
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF1E293B))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ImeiInfoPill(
                                icon = Icons.Default.Dialpad,
                                label = "IMEI: ${rep.imeiMasked}",
                                accentColor = Color(0xFFFF6B00)
                            )
                            ImeiInfoPill(
                                icon = Icons.Default.Payments,
                                label = "ფასი: ${rep.formattedPrice}",
                                accentColor = Color(0xFF22C55E)
                            )
                            ImeiInfoPill(
                                icon = Icons.Default.EventAvailable,
                                label = ImeiCheckManager.formatTimestampKa(
                                    if (rep.verifiedAtMillis > 0L) rep.verifiedAtMillis else rep.updatedAtMillis
                                ),
                                accentColor = Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                // Primary Verification Result Text Area (from Admin)
                val primaryResult = rep.resultText.ifBlank { rep.customerNotes }
                if (primaryResult.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_imei_report_result_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141A24)),
                        border = BorderStroke(1.5.dp, Color(0xFFFF6B00).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B00),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "შემოწმების შედეგი",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Surface(
                                    color = Color(0xFF22C55E).copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "ოფიციალური ამონაწერი",
                                        color = Color(0xFF4ADE80),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFF263042))

                            SelectionContainer {
                                Text(
                                    text = primaryResult,
                                    color = Color(0xFFF1F5F9),
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("customer_imei_result_text_content")
                                )
                            }
                        }
                    }
                }

                // If resultText and customerNotes are both present and different, show additional notes
                if (rep.resultText.isNotBlank() && rep.customerNotes.isNotBlank() && rep.customerNotes.trim() != rep.resultText.trim()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF172030)),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "BEKASERVICE-ის სპეციალისტის დასკვნა",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = rep.customerNotes,
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // If request has no resultText and no fields filled yet
                if (primaryResult.isBlank() && rep.fields.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                        border = BorderStroke(1.dp, Color(0xFF263042))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFFF6B00),
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = reqStatus.descriptionKa,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "ვერიფიკაციის დასრულებისთანავე ამ გვერდზე გამოჩნდება სრული ოფიციალური შემოწმების შედეგი.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                        }
                    }
                } else {
                    // Important / Highlighted Fields Section
                    if (importantFields.isNotEmpty()) {
                        Text(
                            text = "კრიტიკული პარამეტრები და უსაფრთხოება",
                            color = Color(0xFFFF6B00),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        importantFields.forEach { fv ->
                            ImeiReportFieldRowCard(field = fv)
                        }
                    }

                    // Remaining Dynamic Fields Section
                    if (otherFields.isNotEmpty()) {
                        Text(
                            text = "დამატებითი ტექნიკური მახასიათებლები",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        otherFields.forEach { fv ->
                            ImeiReportFieldRowCard(field = fv)
                        }
                    }
                }

                // Verification Sources Used
                if (rep.sourcesUsed.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                        border = BorderStroke(1.dp, Color(0xFF263042))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "გამოყენებული ვერიფიკაციის წყაროები:",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rep.sourcesUsed.forEach { src ->
                                    Surface(
                                        color = Color(0xFF0B0E14),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Text(
                                            text = "✓ $src",
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Mandatory Phase 6 Disclaimer: Missing info is NEVER interpreted as clean
                Surface(
                    color = Color(0xFF181D26),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ ოფიციალური განმარტება: თუ რომელიმე ველს მითითებული აქვს სტატუსი „არ შემოწმებულა“, „ინფორმაცია მიუწვდომელია“ ან „ვერ დადასტურდა“, ეს არ ნიშნავს უპირობოდ სუფთა სტატუსს. დადასტურებულად სუფთა ან უსაფრთხო სტატუსად ითვლება მხოლოდ „დადასტურებულია (სუფთა / დადებითი)“ ნიშნული.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ImeiReportFieldRowCard(field: ImeiResultFieldValue) {
    val st = field.statusEnum
    val badgeColor = when (st) {
        ImeiResultStatus.CONFIRMED_POSITIVE -> Color(0xFF22C55E)
        ImeiResultStatus.CONFIRMED_NEGATIVE -> Color(0xFFEF4444)
        ImeiResultStatus.NOT_CHECKED -> Color(0xFF64748B)
        ImeiResultStatus.UNAVAILABLE -> Color(0xFFF59E0B)
        ImeiResultStatus.UNABLE_TO_VERIFY -> Color(0xFFA855F7)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
        border = BorderStroke(
            width = if (field.isImportant) 1.2.dp else 1.dp,
            color = if (field.isImportant) badgeColor.copy(alpha = 0.5f) else Color(0xFF263042)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    if (field.isImportant) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFF6B00),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = field.titleKa,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        lineHeight = 16.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = st.shortBadgeKa,
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            Text(
                text = field.value.ifBlank { st.labelKa },
                color = Color(0xFFF1F5F9),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (field.descriptionKa.isNotBlank()) {
                Text(
                    text = field.descriptionKa,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (field.sourceName.isNotBlank()) {
                Text(
                    text = "წყარო: ${field.sourceName}",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
