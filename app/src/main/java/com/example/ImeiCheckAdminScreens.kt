package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// ============================================================================
// PHASE 4, 5, 10, 11 — ADMIN PANEL: IMEI CHECK CONTROL CENTER
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminImeiCheckControlCenter(
    initialTargetRequestId: String? = null,
    onTargetRequestConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stateVersion = ImeiCheckManager.stateVersion.intValue

    var selectedSubTab by remember { mutableIntStateOf(0) }
    LaunchedEffect(initialTargetRequestId) {
        if (!initialTargetRequestId.isNullOrBlank()) {
            selectedSubTab = 0
        }
    }
    val subTabs = listOf(
        "განაცხადები",
        "პარამეტრები & ქარდი",
        "ბრენდები",
        "შედეგის ველები",
        "შემოწმების წყაროები",
        "სტატისტიკა"
    )

    val allRequests = remember(stateVersion) { ImeiCheckManager.loadAllRequestsLocal(context) }
    val allReports = remember(stateVersion) { ImeiCheckManager.loadAllReportsLocal(context) }
    val allBrands = remember(stateVersion) { ImeiCheckManager.loadBrands(context) }
    val allFields = remember(stateVersion) { ImeiCheckManager.loadResultFields(context) }
    val allSources = remember(stateVersion) { ImeiCheckManager.loadSources(context) }
    val currentConfig = remember(stateVersion) { ImeiCheckManager.loadConfig(context) }

    // Real-time Firestore listener for admin requests
    DisposableEffect(Unit) {
        val reg = ImeiCheckManager.listenToAllRequestsForAdmin(context) { _ -> }
        onDispose { reg?.remove() }
    }

    val pendingCount = remember(allRequests) {
        allRequests.count {
            !it.userDeletedHistory && (
                it.status == ImeiRequestStatus.PENDING.id ||
                    it.status == ImeiRequestStatus.IN_PROGRESS.id ||
                    it.status == ImeiRequestStatus.WAITING_FOR_INFO.id
                )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C10))
            .testTag("admin_imei_control_center")
    ) {
        // Sub-Tab Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF10141C))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            subTabs.forEachIndexed { index, label ->
                val isSelected = selectedSubTab == index
                Surface(
                    onClick = { selectedSubTab = index },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFFFF6B00) else Color(0xFF181E29),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFFFF6B00) else Color(0xFF2B3548)
                    ),
                    modifier = Modifier
                        .heightIn(min = 42.dp)
                        .testTag("admin_imei_subtab_$index")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        if (index == 0 && pendingCount > 0) {
                            Surface(
                                color = if (isSelected) Color.Black else Color(0xFFFF6B00),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = pendingCount.toString(),
                                    color = if (isSelected) Color(0xFFFF6B00) else Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1E293B))

        when (selectedSubTab) {
            0 -> AdminImeiRequestsTab(
                requests = allRequests,
                reports = allReports,
                brands = allBrands,
                fields = allFields,
                sources = allSources,
                initialTargetRequestId = initialTargetRequestId,
                onTargetRequestConsumed = onTargetRequestConsumed
            )
            1 -> AdminImeiGeneralSettingsAndCardTab(initialConfig = currentConfig)
            2 -> AdminImeiBrandsManagementTab(
                brands = allBrands,
                requests = allRequests
            )
            3 -> AdminImeiResultFieldsManagementTab(
                fields = allFields,
                brands = allBrands,
                sources = allSources
            )
            4 -> AdminImeiSourcesManagementTab(
                sources = allSources,
                brands = allBrands
            )
            5 -> AdminImeiAnalyticsDashboardTab(
                requests = allRequests,
                reports = allReports,
                fields = allFields,
                config = currentConfig
            )
        }
    }
}

// ============================================================================
// SUB-TAB 0: PHASE 5 & 10 — ADMIN REQUEST MANAGEMENT & MANUAL VERIFICATION
// ============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminImeiRequestsTab(
    requests: List<ImeiCheckRequest>,
    reports: Map<String, ImeiVerificationReport>,
    brands: List<ImeiBrandConfig>,
    fields: List<ImeiResultFieldDefinition>,
    sources: List<ImeiVerificationSource>,
    initialTargetRequestId: String? = null,
    onTargetRequestConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var brandFilter by remember { mutableStateOf("ALL") }
    var editingRequest by remember { mutableStateOf<ImeiCheckRequest?>(null) }

    LaunchedEffect(initialTargetRequestId, requests) {
        val targetId = initialTargetRequestId?.trim().orEmpty()
        if (targetId.isNotBlank()) {
            BekaNotificationManager.markAdminOrderNotificationAsRead(context, targetId, "imei_check")
            val found = requests.find { it.id == targetId }
            if (found != null) {
                editingRequest = found
                onTargetRequestConsumed()
            }
        }
    }

    val filteredRequests = remember(requests, searchQuery, statusFilter, brandFilter) {
        val q = searchQuery.trim().lowercase()
        requests.filter { req ->
            val matchesStatus = statusFilter == "ALL" || req.status == statusFilter
            val matchesBrand = brandFilter == "ALL" || req.brandId.equals(brandFilter, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                req.imei.lowercase().contains(q) ||
                req.imeiMasked.lowercase().contains(q) ||
                req.brandName.lowercase().contains(q) ||
                req.verifiedModel.lowercase().contains(q) ||
                req.userName.lowercase().contains(q) ||
                req.userEmail.lowercase().contains(q) ||
                req.id.lowercase().contains(q)
            matchesStatus && matchesBrand && matchesQuery
        }.sortedByDescending { it.createdAtMillis }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_imei_request_search_input"),
            placeholder = {
                Text(
                    text = "ძებნა IMEI-ით, მწარმოებლით, მოდელით ან მომხმარებლით...",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFFF6B00))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "გასუფთავება", tint = Color(0xFF94A3B8))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF6B00),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF121721),
                unfocusedContainerColor = Color(0xFF121721)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Status Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val statusChips = listOf(
                "ALL" to "ყველა (${requests.size})",
                ImeiRequestStatus.PENDING.id to "მოლოდინში",
                ImeiRequestStatus.IN_PROGRESS.id to "მოწმდება",
                ImeiRequestStatus.WAITING_FOR_INFO.id to "დაზუსტება",
                ImeiRequestStatus.COMPLETED.id to "დასრულებული",
                ImeiRequestStatus.REJECTED.id to "უარყოფილი"
            )
            statusChips.forEach { (id, label) ->
                val selected = statusFilter == id
                FilterChip(
                    selected = selected,
                    onClick = { statusFilter = id },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF6B00).copy(alpha = 0.22f),
                        selectedLabelColor = Color(0xFFFF6B00),
                        containerColor = Color(0xFF121721),
                        labelColor = Color(0xFFCBD5E1)
                    )
                )
            }
        }

        if (filteredRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "IMEI შემოწმების განაცხადები ვერ მოიძებნა",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRequests, key = { it.id }) { req ->
                    val st = req.statusEnum
                    val paySt = req.paymentStatusEnum
                    val stColor = when (st) {
                        ImeiRequestStatus.COMPLETED -> Color(0xFF22C55E)
                        ImeiRequestStatus.IN_PROGRESS -> Color(0xFF38BDF8)
                        ImeiRequestStatus.WAITING_FOR_INFO -> Color(0xFFF59E0B)
                        ImeiRequestStatus.REJECTED -> Color(0xFFEF4444)
                        ImeiRequestStatus.PENDING -> Color(0xFFA855F7)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_imei_request_card_${req.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                        border = BorderStroke(1.dp, stColor.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (req.verifiedModel.isNotBlank()) {
                                            "${req.brandName} • ${req.verifiedModel}"
                                        } else {
                                            req.brandName
                                        },
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (req.userDeletedHistory || req.imei == "DELETED") {
                                            "IMEI: ${req.imeiMasked} (მომხმარებელმა წაშალა ისტორიიდან)"
                                        } else {
                                            "IMEI: ${req.imei}"
                                        },
                                        color = if (req.userDeletedHistory) Color(0xFFF87171) else Color(0xFFFFB074),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    color = stColor.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, stColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = st.shortLabelKa,
                                        color = stColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF0B0E14),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Text(
                                        text = "👤 ${req.userName} (${req.userEmail.ifBlank { req.userId.take(8) }})",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    color = Color(0xFF0B0E14),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Text(
                                        text = "💳 ${req.formattedPrice} • ${paySt.labelKa}",
                                        color = if (paySt == ImeiPaymentStatus.PAID) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1
                                    )
                                }

                                Surface(
                                    color = Color(0xFF0B0E14),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Text(
                                        text = "🕒 ${ImeiCheckManager.formatTimestampKa(req.createdAtMillis)}",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!req.userDeletedHistory && req.imei != "DELETED") {
                                    OutlinedButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("IMEI", req.imei))
                                            Toast.makeText(context, "IMEI დაკოპირებულია: ${req.imei}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.heightIn(min = 46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "IMEI კოპირება",
                                            tint = Color(0xFFFF6B00),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "IMEI კოპირება",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        BekaNotificationManager.markAdminOrderNotificationAsRead(context, req.id, "imei_check")
                                        editingRequest = req
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 46.dp)
                                        .testTag("admin_open_imei_verification_modal_${req.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF6B00),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FactCheck,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "შემოწმება / რეპორტის შევსება",
                                        fontWeight = FontWeight.ExtraBold,
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
        }
    }

    editingRequest?.let { req ->
        AdminImeiVerificationEditorDialog(
            request = req,
            existingReport = reports[req.id],
            allFields = fields,
            allSources = sources,
            onDismiss = { editingRequest = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminImeiVerificationEditorDialog(
    request: ImeiCheckRequest,
    existingReport: ImeiVerificationReport?,
    allFields: List<ImeiResultFieldDefinition>,
    allSources: List<ImeiVerificationSource>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var selectedStatus by remember(request) { mutableStateOf(request.statusEnum) }
    var selectedPaymentStatus by remember(request) { mutableStateOf(request.paymentStatusEnum) }
    var verifiedModel by remember(request, existingReport) {
        mutableStateOf(existingReport?.verifiedModel?.ifBlank { request.verifiedModel } ?: request.verifiedModel)
    }
    var internalNotes by remember(request) { mutableStateOf(request.internalNotes) }

    // ONE large multiline verification result text
    // Restores draft text if saved, else published resultText, else customerNotes
    var resultText by remember(request, existingReport) {
        val initial = request.draftResultText.ifBlank {
            existingReport?.draftResultText?.ifBlank {
                request.resultText.ifBlank {
                    existingReport?.resultText?.ifBlank {
                        request.customerNotes.ifBlank {
                            existingReport?.customerNotes ?: ""
                        }
                    }
                }
            }
        } ?: ""
        mutableStateOf(initial)
    }

    val selectedSources = remember(existingReport, allSources, request.brandId) {
        val initial = existingReport?.sourcesUsed ?: allSources
            .filter { it.active && (it.supportedBrands.contains("ALL") || it.supportedBrands.contains(request.brandId)) }
            .map { it.name }
        mutableStateListOf<String>().apply { addAll(initial) }
    }

    var isSaving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 64.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("admin_imei_verification_editor_dialog"),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0D1118),
                border = BorderStroke(1.5.dp, Color(0xFFFF6B00).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Modal Header (Sticky Top)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF141923))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "შეკვეთა #${request.id.takeLast(8)} • ${request.brandName}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "მომხმარებელი: ${request.userName.ifBlank { "სტუმარი" }} • ${request.formattedDate}",
                                color = Color(0xFFFF6B00),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { if (!isSaving) onDismiss() }) {
                            Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = Color.White)
                        }
                    }

                    // Vertically Scrollable Content Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Order Info Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                            border = BorderStroke(1.dp, Color(0xFF263042)),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "სრული IMEI კოდი (TAC: ${request.tac})",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = if (request.userDeletedHistory) "${request.imeiMasked} [წაშლილია მომხმარებლის მიერ]" else request.imei,
                                            color = if (request.userDeletedHistory) Color(0xFFF87171) else Color(0xFFFFB074),
                                            fontSize = 16.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    if (!request.userDeletedHistory && request.imei != "DELETED") {
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(request.imei))
                                                Toast.makeText(context, "IMEI დაკოპირდა", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFF6B00),
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("admin_imei_copy_imei_button")
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text("კოპირება", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF0B0E14),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("შეკვეთის ფასი", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            Text(request.formattedPrice, color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFF0B0E14),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("შექმნის დრო", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            Text(request.formattedDate, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }

                                // Quick External Source Links for Admin
                                val activeSources = allSources.filter { it.active }
                                if (activeSources.isNotEmpty()) {
                                    Text(
                                        text = "სწრაფი გადასვლა შემოწმების წყაროებზე:",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        activeSources.forEach { src ->
                                            val isUsed = selectedSources.contains(src.name)
                                            FilterChip(
                                                selected = isUsed,
                                                onClick = {
                                                    if (isUsed) selectedSources.remove(src.name)
                                                    else selectedSources.add(src.name)
                                                },
                                                label = {
                                                    Text(src.name, fontSize = 11.sp, maxLines = 1)
                                                },
                                                trailingIcon = if (src.websiteUrl.isNotBlank()) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.OpenInNew,
                                                            contentDescription = "საიტის გახსნა",
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clickable {
                                                                    runCatching {
                                                                        context.startActivity(
                                                                            Intent(Intent.ACTION_VIEW, Uri.parse(src.websiteUrl))
                                                                        )
                                                                    }
                                                                }
                                                        )
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Request Status & Payment Status Controls
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                            border = BorderStroke(1.dp, Color(0xFF263042)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "მოთხოვნის სტატუსი:",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ImeiRequestStatus.entries.forEach { st ->
                                        val selected = selectedStatus == st
                                        FilterChip(
                                            selected = selected,
                                            onClick = { selectedStatus = st },
                                            label = {
                                                Text(st.labelKa, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFFF6B00).copy(alpha = 0.25f),
                                                selectedLabelColor = Color(0xFFFF6B00)
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "გადახდის სტატუსი (${request.formattedPrice}):",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ImeiPaymentStatus.entries.forEach { pst ->
                                        val selected = selectedPaymentStatus == pst
                                        FilterChip(
                                            selected = selected,
                                            onClick = { selectedPaymentStatus = pst },
                                            label = {
                                                Text(pst.labelKa, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF22C55E).copy(alpha = 0.25f),
                                                selectedLabelColor = Color(0xFF4ADE80)
                                            )
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = verifiedModel,
                                    onValueChange = { verifiedModel = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_imei_verified_model_input"),
                                    label = {
                                        Text(
                                            when {
                                                request.brandName.contains("Apple", ignoreCase = true) -> "დადასტურებული მოდელი (მაგ: iPhone 15 Pro Max 256GB)"
                                                request.brandName.contains("Samsung", ignoreCase = true) -> "დადასტურებული მოდელი (მაგ: Galaxy S24 Ultra)"
                                                request.brandName.contains("Xiaomi", ignoreCase = true) -> "დადასტურებული მოდელი (მაგ: Poco F7 Pro / Xiaomi 14)"
                                                else -> "დადასტურებული მოდელი (მაგ: ${request.brandName} მოდელი)"
                                            }
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF6B00),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                        }

                        // 3. THE SINGLE LARGE MULTILINE TEXT AREA (Replaces multiple individual technical fields)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                            border = BorderStroke(1.5.dp, Color(0xFFFF6B00).copy(alpha = 0.5f)),
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6B00),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "შემოწმების შედეგი",
                                            color = Color.White,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFF0B0E14),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Text(
                                            text = if (resultText.isBlank()) "0 სიმბოლო" else "${resultText.length} სიმბოლო",
                                            color = if (resultText.isNotBlank()) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "ჩასვით გადამოწმებით მიღებული სრული ტექნიკური და იურიდიული მონაცემები (FRP, iCloud, Blacklist, SimLock, გარანტია და ა.შ.). შეინახება ზუსტი ფორმატირებით.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                )

                                // Helper Toolbar: Paste, Copy, Clear
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                resultText = if (resultText.isBlank()) clip else "$resultText\n$clip"
                                                Toast.makeText(context, "ტექსტი ჩაისვა ბუფერიდან", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "ბუფერი ცარიელია", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("admin_imei_paste_clipboard_button")
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ჩასმა", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            if (resultText.isNotBlank()) {
                                                clipboardManager.setText(AnnotatedString(resultText))
                                                Toast.makeText(context, "ტექსტი დაკოპირდა", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = resultText.isNotBlank(),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("admin_imei_copy_result_button")
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("კოპირება", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (resultText.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { resultText = "" },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                                            border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.5f)),
                                            modifier = Modifier.testTag("admin_imei_clear_result_button")
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("გასუფთავება", fontSize = 11.sp)
                                        }
                                    }
                                }

                                // Large Multiline Text Field
                                OutlinedTextField(
                                    value = resultText,
                                    onValueChange = { resultText = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_imei_result_text_input"),
                                    placeholder = {
                                        Text(
                                            text = "ჩასვით აქ IMEI-ის გადამოწმებით მიღებული სრული ინფორმაცია...",
                                            color = Color(0xFF64748B),
                                            fontSize = 12.5.sp
                                        )
                                    },
                                    minLines = 4,
                                    maxLines = 16,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFFF6B00),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color(0xFFFF6B00)
                                    )
                                )
                            }
                        }

                        // 4. Admin Internal Notes (Private to Admin)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                            border = BorderStroke(1.dp, Color(0xFF263042)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = internalNotes,
                                    onValueChange = { internalNotes = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_imei_internal_notes_input"),
                                    label = { Text("🔒 შიდა ადმინისტრაციული შენიშვნა (კლიენტს არ უჩანს)") },
                                    minLines = 2,
                                    maxLines = 5,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color(0xFFFDE68A),
                                        unfocusedTextColor = Color(0xFFFDE68A)
                                    )
                                )
                            }
                        }
                    }

                    // 3. Fixed Bottom Action Buttons Container (Fits perfectly on one screen, never clipped)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_imei_bottom_action_bar"),
                        color = Color(0xFF141923),
                        tonalElevation = 8.dp,
                        border = BorderStroke(1.dp, Color(0xFF263042))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Secondary Action: Save Draft Without Publishing (Customer Cannot See It)
                            OutlinedButton(
                                onClick = {
                                    if (isSaving) return@OutlinedButton
                                    isSaving = true
                                    scope.launch {
                                        ImeiCheckManager.saveAdminVerificationDraft(
                                            context = context,
                                            request = request,
                                            draftText = resultText,
                                            newStatus = selectedStatus,
                                            newPaymentStatus = selectedPaymentStatus,
                                            verifiedModel = verifiedModel,
                                            internalNotes = internalNotes
                                        )
                                        isSaving = false
                                        Toast.makeText(context, "დრაფტი შენახულია გამოქვეყნების გარეშე", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 46.dp)
                                    .testTag("admin_imei_save_draft_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.2.dp, Color(0xFF38BDF8))
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "დრაფტი",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Primary Action: Save Result and Send to Customer (Publishes Report)
                            Button(
                                onClick = {
                                    if (isSaving) return@Button
                                    isSaving = true
                                    scope.launch {
                                        val finalStatus = if (selectedStatus == ImeiRequestStatus.PENDING || selectedStatus == ImeiRequestStatus.IN_PROGRESS) {
                                            ImeiRequestStatus.COMPLETED
                                        } else {
                                            selectedStatus
                                        }
                                        ImeiCheckManager.saveAdminVerificationProgress(
                                            context = context,
                                            request = request,
                                            newStatus = finalStatus,
                                            newPaymentStatus = selectedPaymentStatus,
                                            verifiedModel = verifiedModel,
                                            internalNotes = internalNotes,
                                            customerNotes = resultText,
                                            fieldValues = emptyList(),
                                            sourcesUsed = selectedSources.toList(),
                                            notifyCustomer = true,
                                            resultText = resultText,
                                            draftResultText = "",
                                            isPublished = true
                                        )
                                        isSaving = false
                                        Toast.makeText(
                                            context,
                                            "შედეგი გამოქვეყნდა და მომხმარებელს გაეგზავნა!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onDismiss()
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1.35f)
                                    .heightIn(min = 46.dp)
                                    .testTag("admin_imei_complete_and_notify_button"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22C55E),
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "გაგზავნა",
                                    fontWeight = FontWeight.ExtraBold,
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
    }
}

// ============================================================================
// SUB-TAB 1: PHASE 2.3 & 4.1 — GENERAL SETTINGS & ENTRY CARD CUSTOMIZATION
// ============================================================================

@Composable
private fun AdminImeiGeneralSettingsAndCardTab(
    initialConfig: ImeiServiceConfig
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serviceEnabled by remember(initialConfig) { mutableStateOf(initialConfig.serviceEnabled) }
    var acceptingRequests by remember(initialConfig) { mutableStateOf(initialConfig.acceptingRequests) }
    var entryCardVisible by remember(initialConfig) { mutableStateOf(initialConfig.entryCardVisible) }
    var priceText by remember(initialConfig) {
        mutableStateOf(
            if (initialConfig.priceGel % 1.0 == 0.0) initialConfig.priceGel.toInt().toString()
            else initialConfig.priceGel.toString()
        )
    }
    var minMinutesText by remember(initialConfig) { mutableStateOf(initialConfig.estimatedMinMinutes.toString()) }
    var maxMinutesText by remember(initialConfig) { mutableStateOf(initialConfig.estimatedMaxMinutes.toString()) }
    var title by remember(initialConfig) { mutableStateOf(initialConfig.title) }
    var subtitle by remember(initialConfig) { mutableStateOf(initialConfig.subtitle) }
    var shortDescription by remember(initialConfig) { mutableStateOf(initialConfig.shortDescription) }
    var detailedDescription by remember(initialConfig) { mutableStateOf(initialConfig.detailedDescription) }
    var promoBadgeText by remember(initialConfig) { mutableStateOf(initialConfig.promoBadgeText) }
    var cardImageUrl by remember(initialConfig) { mutableStateOf(initialConfig.cardImageUrl) }
    var userInstructions by remember(initialConfig) { mutableStateOf(initialConfig.userInstructions) }
    var privacyNotice by remember(initialConfig) { mutableStateOf(initialConfig.privacyNotice) }
    var maintenanceMessage by remember(initialConfig) { mutableStateOf(initialConfig.maintenanceMessage) }
    var paymentInstructions by remember(initialConfig) { mutableStateOf(initialConfig.paymentInstructions) }

    var pendingCropSourcePath by remember { mutableStateOf<String?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    // Gallery Picker reusing ProductImageManager + ProductImageCropDialog
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = ProductImageManager.copyPickerUriToInternalStorage(context, uri)
                if (savedPath != null) {
                    pendingCropSourcePath = savedPath
                } else {
                    Toast.makeText(context, "ფოტოს იმპორტი ვერ მოხერხდა", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val livePreviewConfig = remember(
        serviceEnabled,
        acceptingRequests,
        entryCardVisible,
        priceText,
        minMinutesText,
        maxMinutesText,
        title,
        subtitle,
        shortDescription,
        detailedDescription,
        promoBadgeText,
        cardImageUrl,
        userInstructions,
        privacyNotice,
        maintenanceMessage,
        paymentInstructions
    ) {
        ImeiServiceConfig(
            serviceEnabled = serviceEnabled,
            acceptingRequests = acceptingRequests,
            entryCardVisible = entryCardVisible,
            priceGel = priceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 3.0,
            currency = "GEL",
            estimatedMinMinutes = minMinutesText.toIntOrNull()?.coerceAtLeast(1) ?: 5,
            estimatedMaxMinutes = maxMinutesText.toIntOrNull()?.coerceAtLeast(1) ?: 30,
            title = title.ifBlank { "IMEI CHECK • სრული შემოწმება" },
            subtitle = subtitle,
            shortDescription = shortDescription,
            detailedDescription = detailedDescription,
            promoBadgeText = promoBadgeText,
            cardImageUrl = cardImageUrl.ifBlank { ImeiServiceConfig.DEFAULT_CARD_IMAGE_URL },
            userInstructions = userInstructions,
            privacyNotice = privacyNotice,
            maintenanceMessage = maintenanceMessage,
            paymentInstructions = paymentInstructions,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Preview of Diagnostics Entry Card (Phase 2.3)
        Text(
            text = "დიაგნოსტიკის IMEI ქარდის ცოცხალი გადახედვა (Live Preview)",
            color = Color(0xFFFF6B00),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )

        ImeiCheckDiagnosticsEntryCard(
            config = livePreviewConfig,
            historyCount = 3,
            onOpenImeiCheck = {},
            onOpenHistory = {},
            isPreviewMode = true
        )

        // 2. Entry Card Image Upload, URL, Crop & Restore Default
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
            border = BorderStroke(1.dp, Color(0xFF263042)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ქარდის ვიზუალური ბანერის მართვა (ფოტო / კროპი / URL)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                OutlinedTextField(
                    value = cardImageUrl,
                    onValueChange = { cardImageUrl = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_imei_card_image_url_input"),
                    label = { Text("ფოტოს URL ან ლოკალური ბილიკი") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B00),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ფოტოს ატვირთვა", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1)
                    }

                    if (cardImageUrl.startsWith("/")) {
                        OutlinedButton(
                            onClick = { pendingCropSourcePath = cardImageUrl },
                            modifier = Modifier.heightIn(min = 46.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Crop, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("კროპი", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            cardImageUrl = ImeiServiceConfig.DEFAULT_CARD_IMAGE_URL
                            Toast.makeText(context, "სტანდარტული ფოტო აღდგენილია", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.heightIn(min = 46.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF64748B))
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Default", color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // 3. Toggles & Pricing / Time Configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
            border = BorderStroke(1.dp, Color(0xFF263042)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "სერვისის სტატუსი, ტარიფი და დრო",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                AdminSwitchRow(
                    title = "IMEI სერვისი ჩართულია",
                    subtitle = "გამორთვისას მომხმარებელს გამოუჩნდება ტექნიკური სამუშაოების შეტყობინება",
                    checked = serviceEnabled,
                    onCheckedChange = { serviceEnabled = it }
                )

                AdminSwitchRow(
                    title = "ახალი განაცხადების მიღება",
                    checked = acceptingRequests,
                    subtitle = "დროებით შეაჩერეთ ახალი მოთხოვნების მიღება ისტორიის გათიშვის გარეშე",
                    onCheckedChange = { acceptingRequests = it }
                )

                AdminSwitchRow(
                    title = "დიაგნოსტიკის გვერდზე IMEI ქარდის ჩვენება",
                    subtitle = "აჩვენებს ან მალავს IMEI CHECK ბანერს დიაგნოსტიკის სექციაში",
                    checked = entryCardVisible,
                    onCheckedChange = { entryCardVisible = it }
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_imei_price_input"),
                    label = { Text("სერვისის ფასი (GEL)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minMinutesText,
                        onValueChange = { minMinutesText = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.weight(1f),
                        label = { Text("მინ. წუთი") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = maxMinutesText,
                        onValueChange = { maxMinutesText = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.weight(1f),
                        label = { Text("მაქს. წუთი") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }
        }

        // 4. Georgian Titles, Descriptions & Disclaimers
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
            border = BorderStroke(1.dp, Color(0xFF263042)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ქართული ტექსტები, ინსტრუქცია და კონფიდენციალურობა",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("მთავარი სათაური") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ქვესათაური") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = promoBadgeText,
                    onValueChange = { promoBadgeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("პრომო ბეიჯის ტექსტი") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = shortDescription,
                    onValueChange = { shortDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("მოკლე აღწერა (დიაგნოსტიკის ქარდზე)") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = detailedDescription,
                    onValueChange = { detailedDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("სრული აღწერა (IMEI შემოწმების გვერდზე)") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = userInstructions,
                    onValueChange = { userInstructions = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("მომხმარებლის ინსტრუქცია") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = privacyNotice,
                    onValueChange = { privacyNotice = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("კონფიდენციალურობის შეტყობინება") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = maintenanceMessage,
                    onValueChange = { maintenanceMessage = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ტექნიკური შეჩერების შეტყობინება") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }

        Button(
            onClick = {
                if (isPublishing) return@Button
                isPublishing = true
                scope.launch {
                    val res = ImeiCheckManager.publishConfigToCloud(context, livePreviewConfig)
                    isPublishing = false
                    res.onSuccess {
                        Toast.makeText(
                            context,
                            "IMEI პარამეტრები და ქარდის დიზაინი წარმატებით გამოქვეყნდა!",
                            Toast.LENGTH_LONG
                        ).show()
                    }.onFailure {
                        Toast.makeText(
                            context,
                            "პარამეტრები შენახულია ლოკალურად",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            enabled = !isPublishing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("admin_publish_imei_settings_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B00),
                contentColor = Color.Black
            )
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPublishing) "მიმდინარეობს გამოქვეყნება..." else "ცვლილებების შენახვა და გამოქვეყნება",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Reuse existing ImageCropperDialog for cropping/repositioning the Entry Card image
    pendingCropSourcePath?.let { path ->
        ImageCropperDialog(
            imageUriOrUrl = path,
            onDismiss = { pendingCropSourcePath = null },
            onCropFinished = { croppedPath ->
                cardImageUrl = croppedPath
                pendingCropSourcePath = null
                Toast.makeText(context, "ბანერის ფოტო მოიჭრა და განახლდა", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun AdminSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B0E14))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFFFF6B00)
            )
        )
    }
}

// ============================================================================
// SUB-TAB 2: PHASE 4.2 — BRAND MANAGEMENT
// ============================================================================

@Composable
private fun AdminImeiBrandsManagementTab(
    brands: List<ImeiBrandConfig>,
    requests: List<ImeiCheckRequest>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newBrandName by remember { mutableStateOf("") }
    var newBrandLocalized by remember { mutableStateOf("") }
    var newBrandEmoji by remember { mutableStateOf("📱") }
    var newBrandLogoUrl by remember { mutableStateOf("") }

    val sortedBrands = remember(brands) { brands.sortedBy { it.displayOrder } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Add New Brand Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
            border = BorderStroke(1.dp, Color(0xFF263042)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ახალი მწარმოებლის (ბრენდის) დამატება",
                    color = Color(0xFFFF6B00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newBrandName,
                        onValueChange = { newBrandName = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_add_imei_brand_name_input"),
                        label = { Text("სახელი (მაგ: Sony)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = newBrandEmoji,
                        onValueChange = { newBrandEmoji = it },
                        modifier = Modifier.width(84.dp),
                        label = { Text("Icon") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newBrandLogoUrl,
                        onValueChange = { newBrandLogoUrl = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("ლოგოს URL (არასავალდებულო)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF6B00),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Button(
                        onClick = {
                            val cleanName = newBrandName.trim()
                            if (cleanName.isBlank()) return@Button
                            val id = cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "_").ifBlank {
                                "brand_${System.currentTimeMillis()}"
                            }
                            val updated = sortedBrands.toMutableList()
                            updated.add(
                                ImeiBrandConfig(
                                    id = id,
                                    name = cleanName,
                                    localizedName = newBrandLocalized.trim().ifBlank { cleanName },
                                    logoUrl = newBrandLogoUrl.trim(),
                                    iconEmoji = newBrandEmoji.trim().ifBlank { "📱" },
                                    displayOrder = updated.size + 1,
                                    enabled = true,
                                    isPopular = false,
                                    updatedAtMillis = System.currentTimeMillis()
                                )
                            )
                            newBrandName = ""
                            newBrandLocalized = ""
                            newBrandLogoUrl = ""
                            scope.launch {
                                ImeiCheckManager.publishBrandsToCloud(context, updated)
                                Toast.makeText(context, "ბრენდი დაემატა: $cleanName", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("admin_add_imei_brand_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B00),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("დამატება", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedBrands, key = { it.id }) { brand ->
                val usedCount = requests.count { it.brandId == brand.id }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                    border = BorderStroke(
                        1.dp,
                        if (brand.enabled) Color(0xFF263042) else Color(0xFFEF4444).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                RealBrandLogoBadge(brand = brand, size = 36.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${brand.displayOrder}. ${brand.localizedName}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ID: ${brand.id} • შემოწმებები: $usedCount",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Switch(
                                checked = brand.enabled,
                                onCheckedChange = { enabled ->
                                    val list = sortedBrands.map {
                                        if (it.id == brand.id) it.copy(enabled = enabled) else it
                                    }
                                    scope.launch { ImeiCheckManager.publishBrandsToCloud(context, list) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFFFF6B00)
                                )
                            )
                        }

                        HorizontalDivider(color = Color(0xFF1E293B))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val idx = sortedBrands.indexOfFirst { it.id == brand.id }
                                        if (idx > 0) {
                                            val list = sortedBrands.toMutableList()
                                            val prev = list[idx - 1]
                                            list[idx - 1] = brand.copy(displayOrder = idx)
                                            list[idx] = prev.copy(displayOrder = idx + 1)
                                            scope.launch { ImeiCheckManager.publishBrandsToCloud(context, list) }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "ზემოთ", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val idx = sortedBrands.indexOfFirst { it.id == brand.id }
                                        if (idx >= 0 && idx < sortedBrands.lastIndex) {
                                            val list = sortedBrands.toMutableList()
                                            val next = list[idx + 1]
                                            list[idx + 1] = brand.copy(displayOrder = idx + 2)
                                            list[idx] = next.copy(displayOrder = idx + 1)
                                            scope.launch { ImeiCheckManager.publishBrandsToCloud(context, list) }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "ქვემოთ", tint = Color(0xFFCBD5E1), modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val list = sortedBrands.map {
                                            if (it.id == brand.id) it.copy(isPopular = !it.isPopular) else it
                                        }
                                        scope.launch { ImeiCheckManager.publishBrandsToCloud(context, list) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "პოპულარული",
                                        tint = if (brand.isPopular) Color(0xFFFF6B00) else Color(0xFF475569),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (usedCount == 0 && sortedBrands.size > 1) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            ImeiCheckManager.deleteBrandFromCloud(context, brand.id)
                                            Toast.makeText(context, "ბრენდი წაიშალა", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "წაშლა", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SUB-TAB 3: PHASE 4.3 — DYNAMIC RESULT FIELDS MANAGEMENT
// ============================================================================

@Composable
private fun AdminImeiResultFieldsManagementTab(
    fields: List<ImeiResultFieldDefinition>,
    brands: List<ImeiBrandConfig>,
    sources: List<ImeiVerificationSource>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newTitleKa by remember { mutableStateOf("") }
    var newDescKa by remember { mutableStateOf("") }
    var newInternalInstr by remember { mutableStateOf("") }
    var newIsImportant by remember { mutableStateOf(false) }

    val sortedFields = remember(fields) { fields.sortedBy { it.displayOrder } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
            border = BorderStroke(1.dp, Color(0xFF263042)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ახალი დინამიკური შედეგის ველის დამატება",
                    color = Color(0xFFFF6B00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                OutlinedTextField(
                    value = newTitleKa,
                    onValueChange = { newTitleKa = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_add_imei_field_title_input"),
                    label = { Text("ველის დასახელება (ქართულად)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = newDescKa,
                    onValueChange = { newDescKa = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("განმარტება მომხმარებლისთვის") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = newIsImportant,
                        onCheckedChange = { newIsImportant = it }
                    )
                    Text(
                        text = "მნიშვნელოვანი / კრიტიკული ველი",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = {
                        val cleanTitle = newTitleKa.trim()
                        if (cleanTitle.isBlank()) return@Button
                        val id = "field_${System.currentTimeMillis()}"
                        val updated = sortedFields.toMutableList()
                        updated.add(
                            ImeiResultFieldDefinition(
                                id = id,
                                titleKa = cleanTitle,
                                descriptionKa = newDescKa.trim(),
                                supportedBrands = listOf("ALL"),
                                displayOrder = updated.size + 1,
                                enabled = true,
                                isImportant = newIsImportant,
                                inputType = "status_and_text",
                                internalInstructions = newInternalInstr.trim()
                            )
                        )
                        newTitleKa = ""
                        newDescKa = ""
                        newInternalInstr = ""
                        newIsImportant = false
                        scope.launch {
                            ImeiCheckManager.publishResultFieldsToCloud(context, updated)
                            Toast.makeText(context, "ველი დაემატა: $cleanTitle", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .testTag("admin_add_imei_field_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B00),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ველის დამატება", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedFields, key = { it.id }) { field ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                    border = BorderStroke(
                        1.dp,
                        if (field.isImportant) Color(0xFFFF6B00).copy(alpha = 0.5f) else Color(0xFF263042)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "${field.displayOrder}. ${field.titleKa}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (field.descriptionKa.isNotBlank()) {
                                Text(
                                    text = field.descriptionKa,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "ბრენდები: ${field.applicableBrands.joinToString(", ")}",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val updated = sortedFields.map {
                                        if (it.id == field.id) it.copy(isImportant = !it.isImportant) else it
                                    }
                                    scope.launch { ImeiCheckManager.publishResultFieldsToCloud(context, updated) }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "მნიშვნელოვანი",
                                    tint = if (field.isImportant) Color(0xFFFF6B00) else Color(0xFF475569)
                                )
                            }
                            Switch(
                                checked = field.enabled,
                                onCheckedChange = { en ->
                                    val updated = sortedFields.map {
                                        if (it.id == field.id) it.copy(enabled = en) else it
                                    }
                                    scope.launch { ImeiCheckManager.publishResultFieldsToCloud(context, updated) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFFFF6B00)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SUB-TAB 4: PHASE 4.4 — VERIFICATION SOURCES MANAGEMENT ("შემოწმების წყაროები")
// ============================================================================

@Composable
private fun AdminImeiSourcesManagementTab(
    sources: List<ImeiVerificationSource>,
    brands: List<ImeiBrandConfig>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newSourceName by remember { mutableStateOf("") }
    var newSourceDesc by remember { mutableStateOf("") }
    var newSourceUrl by remember { mutableStateOf("") }
    var newSourceInstr by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
            border = BorderStroke(1.dp, Color(0xFF263042)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ახალი შემოწმების წყაროს დამატება (მხოლოდ ადმინისთვის)",
                    color = Color(0xFFFF6B00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                OutlinedTextField(
                    value = newSourceName,
                    onValueChange = { newSourceName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_add_imei_source_name_input"),
                    label = { Text("წყაროს სახელი (მაგ: Sickw / iUnlocker / GSMA)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = newSourceUrl,
                    onValueChange = { newSourceUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ვებ-გვერდის მისამართი (URL)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = newSourceDesc,
                    onValueChange = { newSourceDesc = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("აღწერა და დანიშნულება") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B00),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Button(
                    onClick = {
                        val cleanName = newSourceName.trim()
                        if (cleanName.isBlank()) return@Button
                        val id = "src_${System.currentTimeMillis()}"
                        val updated = sources.toMutableList()
                        updated.add(
                            ImeiVerificationSource(
                                id = id,
                                name = cleanName,
                                description = newSourceDesc.trim(),
                                websiteUrl = newSourceUrl.trim(),
                                supportedBrands = listOf("ALL"),
                                supportedFields = listOf("ALL"),
                                internalInstructions = newSourceInstr.trim(),
                                active = true
                            )
                        )
                        newSourceName = ""
                        newSourceUrl = ""
                        newSourceDesc = ""
                        newSourceInstr = ""
                        scope.launch {
                            ImeiCheckManager.publishSourcesToCloud(context, updated)
                            Toast.makeText(context, "წყარო დაემატა: $cleanName", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .testTag("admin_add_imei_source_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B00),
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("შემოწმების წყაროს შენახვა", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sources, key = { it.id }) { src ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
                    border = BorderStroke(1.dp, Color(0xFF263042)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = src.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (src.description.isNotBlank()) {
                                Text(
                                    text = src.description,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                            if (src.websiteUrl.isNotBlank()) {
                                Text(
                                    text = src.websiteUrl,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (src.websiteUrl.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(src.websiteUrl)))
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = "გახსნა", tint = Color(0xFF38BDF8))
                                }
                            }
                            Switch(
                                checked = src.active,
                                onCheckedChange = { act ->
                                    val updated = sources.map {
                                        if (it.id == src.id) it.copy(active = act) else it
                                    }
                                    scope.launch { ImeiCheckManager.publishSourcesToCloud(context, updated) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color(0xFFFF6B00)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SUB-TAB 5: PHASE 11 — ADMIN ANALYTICS & STATISTICS DASHBOARD
// ============================================================================

@Composable
private fun AdminImeiAnalyticsDashboardTab(
    requests: List<ImeiCheckRequest>,
    reports: Map<String, ImeiVerificationReport>,
    fields: List<ImeiResultFieldDefinition>,
    config: ImeiServiceConfig
) {
    val analytics = remember(requests, reports, fields) {
        ImeiCheckManager.computeAnalytics(requests, reports, fields)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("admin_imei_analytics_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 11.1 Operational Statistics
        Text(
            text = "1. ოპერაციული სტატისტიკა",
            color = Color(0xFFFF6B00),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsMetricCard(
                title = "სულ მოთხოვნა",
                value = analytics.totalRequests.toString(),
                subtitle = "დღეს: ${analytics.todayRequests} • 7დღე: ${analytics.last7DaysRequests}",
                accentColor = Color(0xFFFF6B00),
                modifier = Modifier.weight(1f)
            )
            AnalyticsMetricCard(
                title = "დასრულებული",
                value = analytics.completedRequests.toString(),
                subtitle = "30 დღეში: ${analytics.last30DaysRequests}",
                accentColor = Color(0xFF22C55E),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsMetricCard(
                title = "მოლოდინში / პროცესში",
                value = "${analytics.pendingRequests} / ${analytics.inProgressRequests}",
                subtitle = "დაზუსტება: ${analytics.waitingForInfoRequests} • უარყოფილი: ${analytics.rejectedRequests}",
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
            AnalyticsMetricCard(
                title = "საშუალო დრო",
                value = String.format(java.util.Locale.US, "%.1f წთ", analytics.averageProcessingMinutes),
                subtitle = "გეგმიური: ${config.formattedEstimatedTime}",
                accentColor = Color(0xFFA855F7),
                modifier = Modifier.weight(1f)
            )
        }

        // 11.2 Business & Revenue Statistics
        Text(
            text = "2. ფინანსური და ბიზნეს ანალიტიკა",
            color = Color(0xFF22C55E),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsMetricCard(
                title = "დადასტურებული შემოსავალი",
                value = String.format(java.util.Locale.US, "%.2f ₾", analytics.confirmedPaidRevenueGel),
                subtitle = "გადახდილი შემოწმებები: ${analytics.paidChecksCount}",
                accentColor = Color(0xFF22C55E),
                modifier = Modifier.weight(1f)
            )
            AnalyticsMetricCard(
                title = "მოლოდინში / გადაუხდელი",
                value = String.format(java.util.Locale.US, "%.2f ₾", analytics.outstandingPaymentTotalGel),
                subtitle = "დაბრუნებული (Refund): ${String.format(java.util.Locale.US, "%.2f ₾", analytics.refundedTotalGel)}",
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnalyticsMetricCard(
                title = "მიმდინარე ტარიფი",
                value = config.formattedPrice,
                subtitle = "საშ. შემოსავალი/შემოწმებაზე: ${String.format(java.util.Locale.US, "%.2f ₾", analytics.averageRevenuePerCompletedCheckGel)}",
                accentColor = Color(0xFFFF6B00),
                modifier = Modifier.weight(1f)
            )
            AnalyticsMetricCard(
                title = "კონვერსიის მაჩვენებელი",
                value = String.format(java.util.Locale.US, "%.1f%%", analytics.conversionRatePercent),
                subtitle = "დასრულებული / სულ მოთხოვნა",
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
        }

        // 11.3 Brand Statistics
        Text(
            text = "3. ბრენდების სტატისტიკა (ლიდერი: ${analytics.mostCheckedManufacturer})",
            color = Color(0xFF38BDF8),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
            border = BorderStroke(1.dp, Color(0xFF263042)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (analytics.brandRequestCounts.isEmpty()) {
                    Text(
                        text = "ბრენდების სტატისტიკა ჯერ ცარიელია",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                } else {
                    analytics.brandRequestCounts.entries
                        .sortedByDescending { it.value }
                        .forEach { (brandName, count) ->
                            val completionPct = analytics.brandCompletionRates[brandName] ?: 0.0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = brandName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$count მოთხოვნა • დასრულება: ${String.format(java.util.Locale.US, "%.0f%%", completionPct)}",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp
                                )
                            }
                        }
                }
            }
        }

        // 11.4 Verification Quality Statistics
        Text(
            text = "4. ვერიფიკაციის ხარისხის და წყაროების ანალიტიკა",
            color = Color(0xFFA855F7),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
            border = BorderStroke(1.dp, Color(0xFF263042)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (analytics.sourceUsageCounts.isNotEmpty()) {
                    Text(
                        text = "წყაროების გამოყენების სიხშირე:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    analytics.sourceUsageCounts.entries.sortedByDescending { it.value }.forEach { (src, cnt) ->
                        Text(
                            text = "• $src: $cnt რეპორტში",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp
                        )
                    }
                }

                if (analytics.unavailableOrUnverifiedFieldCounts.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF1E293B))
                    Text(
                        text = "ველები მიუწვდომელი / დაუდასტურებელი შედეგით:",
                        color = Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    analytics.unavailableOrUnverifiedFieldCounts.entries
                        .sortedByDescending { it.value }
                        .take(8)
                        .forEach { (fieldTitle, cnt) ->
                            Text(
                                text = "• $fieldTitle: $cnt შემთხვევა",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }
                } else {
                    Text(
                        text = "ყველა შემოწმებული ველი სრულად დადასტურებულია.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AnalyticsMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121721)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = Color(0xFFCBD5E1),
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
