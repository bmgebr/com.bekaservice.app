package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ადმინ პანელის სექცია: კონტაქტები და სოციალური ქსელები
 * (Admin Panel Section: Contacts & Social Media Management)
 */
@Composable
fun AdminContactsAndSocialScreen(
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppPreferencesManager.isDarkMode

    // მიმდინარე ლოკალური და Cloud მდგომარეობა
    var config by remember { mutableStateOf(ContactAndSocialManager.loadConfig(context)) }
    var isSavingToCloud by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }

    // დიალოგების მდგომარეობა
    var phoneToEdit by remember { mutableStateOf<ContactPhoneNumber?>(null) }
    var isAddingPhone by remember { mutableStateOf(false) }
    var phoneToDelete by remember { mutableStateOf<ContactPhoneNumber?>(null) }

    var socialToEdit by remember { mutableStateOf<SocialMediaLink?>(null) }
    var isAddingSocial by remember { mutableStateOf(false) }
    var socialToDelete by remember { mutableStateOf<SocialMediaLink?>(null) }

    // Real-time Cloud Firestore სინქრონიზაციის მოსმენა
    DisposableEffect(Unit) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            val repo = FirestoreRepository(context)
            registration = repo.listenToContactConfig { cloudCfg ->
                ContactAndSocialManager.saveLocalConfig(context, cloudCfg)
                config = cloudCfg
            }
        } catch (_: Exception) {}
        onDispose {
            registration?.remove()
        }
    }

    val surfaceBg = if (isDark) Color(0xFF14171E) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) Color(0xFF1A1E27) else Color(0xFFF8FAFC)
    val borderCol = if (isDark) Color(0xFF262C38) else Color(0xFFE2E8F0)
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFFA0A5B0) else Color(0xFF64748B)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_contacts_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ჰედერი & ინფო ბარათი
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = surfaceBg,
                border = BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF7A00).copy(alpha = 0.16f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📞", fontSize = 16.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "კონტაქტები და სოციალური ქსელები",
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Firestore: app_config/contacts • ცვლილებები მომენტალურად აისახება აპლიკაციაში",
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // შეტყობინების ზოლი (Status Banner)
                    if (statusMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isErrorStatus) Color(0xFFFF4D4F).copy(alpha = 0.15f) else Color(0xFF4ADE80).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isErrorStatus) Color(0xFFFF4D4F) else Color(0xFF4ADE80)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_contacts_status_banner")
                        ) {
                            Text(
                                text = statusMessage!!,
                                color = if (isErrorStatus) Color(0xFFFF4D4F) else Color(0xFF4ADE80),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // სექცია 1: ტელეფონის ნომრები (Phone Numbers)
        // =====================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ტელეფონის ნომრები",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF7A00).copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${config.phoneNumbers.size}",
                            color = Color(0xFFFF7A00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = { isAddingPhone = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("admin_add_phone_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("დამატება", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (config.phoneNumbers.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_empty_phones_card")
                ) {
                    Text(
                        text = "ტელეფონის ნომრები არ არის დამატებული. დააჭირეთ „დამატება“ ღილაკს.",
                        color = textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            itemsIndexed(config.phoneNumbers, key = { _, phone -> phone.id }) { index, phone ->
                val isFirst = index == 0
                val isLast = index == config.phoneNumbers.size - 1

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (phone.isEnabled) Color(0xFFFF7A00).copy(alpha = 0.40f) else borderCol
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_phone_item_${phone.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (phone.isEnabled) Color(0xFFFF7A00).copy(alpha = 0.18f) else Color.Gray.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = phone.label,
                                        color = if (phone.isEnabled) Color(0xFFFF7A00) else textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = phone.number,
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Enable / Disable ჩამრთველი
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (phone.isEnabled) "ჩართული" else "გამორთული",
                                    color = if (phone.isEnabled) Color(0xFF4ADE80) else textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = phone.isEnabled,
                                    onCheckedChange = { checked ->
                                        val updatedPhones = config.phoneNumbers.map {
                                            if (it.id == phone.id) it.copy(isEnabled = checked) else it
                                        }
                                        val newCfg = config.copy(
                                            phoneNumbers = updatedPhones,
                                            updatedAtMillis = System.currentTimeMillis()
                                        )
                                        config = newCfg
                                        ContactAndSocialManager.saveLocalConfig(context, newCfg)
                                    },
                                    modifier = Modifier.testTag("admin_toggle_phone_${phone.id}")
                                )
                            }
                        }

                        // მოქმედებების ღილაკები
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // აკრეფის ტესტი (Dialer Test)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1B2C1F),
                                border = BorderStroke(1.dp, Color(0xFF2E6339)),
                                modifier = Modifier
                                    .clickable { ContactAndSocialManager.openDialer(context, phone.number) }
                                    .testTag("admin_test_dial_${phone.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("დარეკვის ტესტი", color = Color(0xFF4ADE80), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // ზემოთ აწევა
                                IconButton(
                                    onClick = {
                                        if (!isFirst) {
                                            val list = config.phoneNumbers.toMutableList()
                                            val current = list.removeAt(index)
                                            list.add(index - 1, current)
                                            val reordered = list.mapIndexed { idx, item -> item.copy(displayOrder = idx + 1) }
                                            val newCfg = config.copy(phoneNumbers = reordered)
                                            config = newCfg
                                            ContactAndSocialManager.saveLocalConfig(context, newCfg)
                                        }
                                    },
                                    enabled = !isFirst,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (isFirst) textSecondary.copy(alpha = 0.3f) else textPrimary, modifier = Modifier.size(16.dp))
                                }

                                // ქვემოთ ჩამოწევა
                                IconButton(
                                    onClick = {
                                        if (!isLast) {
                                            val list = config.phoneNumbers.toMutableList()
                                            val current = list.removeAt(index)
                                            list.add(index + 1, current)
                                            val reordered = list.mapIndexed { idx, item -> item.copy(displayOrder = idx + 1) }
                                            val newCfg = config.copy(phoneNumbers = reordered)
                                            config = newCfg
                                            ContactAndSocialManager.saveLocalConfig(context, newCfg)
                                        }
                                    },
                                    enabled = !isLast,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (isLast) textSecondary.copy(alpha = 0.3f) else textPrimary, modifier = Modifier.size(16.dp))
                                }

                                // რედაქტირება
                                IconButton(
                                    onClick = { phoneToEdit = phone },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("admin_edit_phone_${phone.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFFF7A00), modifier = Modifier.size(15.dp))
                                }

                                // წაშლა
                                IconButton(
                                    onClick = { phoneToDelete = phone },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("admin_delete_phone_${phone.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D4F), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // სექცია 2: სოციალური ქსელები და გარე ბმულები (Social Media Links)
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "სოციალური ქსელები და ბმულები",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF7A00).copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${config.socialLinks.size}",
                            color = Color(0xFFFF7A00),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = { isAddingSocial = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("admin_add_social_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("დამატება", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (config.socialLinks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_empty_socials_card")
                ) {
                    Text(
                        text = "სოციალური ქსელების ბმულები არ არის დამატებული. დააჭირეთ „დამატება“ ღილაკს.",
                        color = textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            itemsIndexed(config.socialLinks, key = { _, link -> link.id }) { index, link ->
                val isFirst = index == 0
                val isLast = index == config.socialLinks.size - 1

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (link.isEnabled) Color(link.platform.brandColorHex).copy(alpha = 0.45f) else borderCol
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_social_item_${link.id}")
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                SocialPlatformBrandIcon(
                                    platform = link.platform,
                                    containerSize = 24.dp
                                )
                                Column {
                                    Text(
                                        text = link.title.ifBlank { link.platform.titleKa },
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = link.url,
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (link.isEnabled) "ჩართული" else "გამორთული",
                                    color = if (link.isEnabled) Color(0xFF4ADE80) else textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = link.isEnabled,
                                    onCheckedChange = { checked ->
                                        val updatedSocials = config.socialLinks.map {
                                            if (it.id == link.id) it.copy(isEnabled = checked) else it
                                        }
                                        val newCfg = config.copy(
                                            socialLinks = updatedSocials,
                                            updatedAtMillis = System.currentTimeMillis()
                                        )
                                        config = newCfg
                                        ContactAndSocialManager.saveLocalConfig(context, newCfg)
                                    },
                                    modifier = Modifier.testTag("admin_toggle_social_${link.id}")
                                )
                            }
                        }

                        // Live კლიენტის Preview ბარათი (როგორ გამოჩნდება მომხმარებელთან)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF101318) else Color(0xFFF1F5F9),
                            border = BorderStroke(0.8.dp, borderCol),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "კლიენტის ხედი:", color = textSecondary, fontSize = 10.sp)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(link.platform.brandColorHex).copy(alpha = 0.20f),
                                        border = BorderStroke(0.8.dp, Color(link.platform.brandColorHex))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            SocialPlatformBrandIcon(platform = link.platform, containerSize = 14.dp)
                                            Text(
                                                text = link.title.ifBlank { link.platform.titleKa },
                                                color = textPrimary,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (link.isEnabled) "✅ ჩანს აპში" else "❌ დამალულია",
                                    color = if (link.isEnabled) Color(0xFF4ADE80) else Color(0xFFFF4D4F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // მოქმედებების ღილაკები
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ბმულის ტესტი (Open Link Test)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF162534),
                                border = BorderStroke(1.dp, Color(0xFF264C6E)),
                                modifier = Modifier
                                    .clickable { ContactAndSocialManager.openSocialLink(context, link) }
                                    .testTag("admin_test_social_${link.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ბმულის შემოწმება", color = Color(0xFF60A5FA), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        if (!isFirst) {
                                            val list = config.socialLinks.toMutableList()
                                            val current = list.removeAt(index)
                                            list.add(index - 1, current)
                                            val reordered = list.mapIndexed { idx, item -> item.copy(displayOrder = idx + 1) }
                                            val newCfg = config.copy(socialLinks = reordered)
                                            config = newCfg
                                            ContactAndSocialManager.saveLocalConfig(context, newCfg)
                                        }
                                    },
                                    enabled = !isFirst,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (isFirst) textSecondary.copy(alpha = 0.3f) else textPrimary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (!isLast) {
                                            val list = config.socialLinks.toMutableList()
                                            val current = list.removeAt(index)
                                            list.add(index + 1, current)
                                            val reordered = list.mapIndexed { idx, item -> item.copy(displayOrder = idx + 1) }
                                            val newCfg = config.copy(socialLinks = reordered)
                                            config = newCfg
                                            ContactAndSocialManager.saveLocalConfig(context, newCfg)
                                        }
                                    },
                                    enabled = !isLast,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (isLast) textSecondary.copy(alpha = 0.3f) else textPrimary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { socialToEdit = link },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("admin_edit_social_${link.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFFF7A00), modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = { socialToDelete = link },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("admin_delete_social_${link.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF4D4F), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // სექცია 3: მისამართი & სამუშაო საათები (Address & Working Hours)
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "სერვის ცენტრის მისამართი და სამუშაო საათები",
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = config.serviceAddress,
                        onValueChange = { config = config.copy(serviceAddress = it) },
                        label = { Text("მისამართი") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_contact_address_input")
                    )

                    OutlinedTextField(
                        value = config.workingHours,
                        onValueChange = { config = config.copy(workingHours = it) },
                        label = { Text("სამუშაო საათები") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_contact_hours_input")
                    )
                }
            }
        }

        // =====================================================================
        // სექცია 4: შენახვა & Cloud სინქრონიზაცია (Save & Sync Button)
        // =====================================================================
        item {
            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isSavingToCloud = true
                        statusMessage = null

                        // 1. ლოკალური შენახვა
                        val cfgToSave = config.copy(
                            updatedAtMillis = System.currentTimeMillis()
                        )
                        ContactAndSocialManager.saveLocalConfig(context, cfgToSave)

                        // 2. Cloud Firestore სინქრონიზაცია
                        val cloudResult = withContext(Dispatchers.IO) {
                            try {
                                FirestoreRepository(context).saveContactConfigToCloud(cfgToSave)
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                        }

                        isSavingToCloud = false
                        if (cloudResult.isSuccess) {
                            isErrorStatus = false
                            statusMessage = "✅ კონტაქტები და სოციალური ქსელები წარმატებით შეინახა და დასინქრონდა ბაზასთან!"
                            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
                        } else {
                            val errMsg = cloudResult.exceptionOrNull()?.message ?: "შეცდომა"
                            isErrorStatus = true
                            statusMessage = "⚠️ ლოკალურად შეინახა, თუმცა ბაზასთან სინქრონიზაცია ვერ მოხერხდა: $errMsg"
                            Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSavingToCloud,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("admin_save_contacts_button")
            ) {
                if (isSavingToCloud) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ინახება Firestore-ში...", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("შენახვა & Cloud სინქრონიზაცია", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // =========================================================================
    // ტელეფონის ნომრის დამატების / რედაქტირების დიალოგი
    // =========================================================================
    if (isAddingPhone || phoneToEdit != null) {
        val editing = phoneToEdit
        var labelInput by remember { mutableStateOf(editing?.label ?: "მთავარი ნომერი") }
        var numberInput by remember { mutableStateOf(editing?.number ?: "+995 598 23 33 32") }
        var isEnabledInput by remember { mutableStateOf(editing?.isEnabled ?: true) }
        var validationError by remember { mutableStateOf<String?>(null) }

        val quickLabels = listOf("მთავარი ნომერი", "WhatsApp", "სერვის ცენტრი", "დახმარება", "გაყიდვები")

        AlertDialog(
            onDismissRequest = {
                isAddingPhone = false
                phoneToEdit = null
            },
            title = {
                Text(
                    text = if (editing != null) "ნომრის რედაქტირება" else "ახალი ნომრის დამატება",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // სწრაფი ლეიბლის არჩევა
                    Text(text = "სწრაფი ლეიბლები:", color = textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickLabels.forEach { ql ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (labelInput == ql) Color(0xFFFF7A00).copy(alpha = 0.25f) else cardBg,
                                border = BorderStroke(1.dp, if (labelInput == ql) Color(0xFFFF7A00) else borderCol),
                                modifier = Modifier.clickable { labelInput = ql }
                            ) {
                                Text(
                                    text = ql,
                                    color = if (labelInput == ql) Color(0xFFFF7A00) else textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (labelInput == ql) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("დასახელება (Label)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_phone_label_input")
                    )

                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = {
                            numberInput = it
                            validationError = null
                        },
                        label = { Text("ტელეფონის ნომერი (მაგ: +995 598 23 33 32)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = validationError != null,
                        supportingText = {
                            if (validationError != null) {
                                Text(validationError!!, color = Color(0xFFFF4D4F), fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_phone_number_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "სტატუსი: აქტიური", color = textPrimary, fontSize = 13.sp)
                        Switch(
                            checked = isEnabledInput,
                            onCheckedChange = { isEnabledInput = it },
                            modifier = Modifier.testTag("admin_dialog_phone_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!ContactAndSocialManager.isValidPhoneNumber(numberInput)) {
                            validationError = "გთხოვთ შეიყვანოთ სწორი ტელეფონის ნომერი (მინიმუმ 6 ციფრი)"
                            return@Button
                        }
                        if (editing != null) {
                            val updated = config.phoneNumbers.map {
                                if (it.id == editing.id) {
                                    it.copy(
                                        label = labelInput.trim().ifBlank { "მთავარი ნომერი" },
                                        number = numberInput.trim(),
                                        isEnabled = isEnabledInput
                                    )
                                } else it
                            }
                            val newCfg = config.copy(phoneNumbers = updated)
                            config = newCfg
                            ContactAndSocialManager.saveLocalConfig(context, newCfg)
                        } else {
                            val newPhone = ContactPhoneNumber(
                                id = UUID.randomUUID().toString(),
                                label = labelInput.trim().ifBlank { "მთავარი ნომერი" },
                                number = numberInput.trim(),
                                isEnabled = isEnabledInput,
                                displayOrder = config.phoneNumbers.size + 1
                            )
                            val newCfg = config.copy(phoneNumbers = config.phoneNumbers + newPhone)
                            config = newCfg
                            ContactAndSocialManager.saveLocalConfig(context, newCfg)
                        }
                        isAddingPhone = false
                        phoneToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                    modifier = Modifier.testTag("admin_phone_dialog_confirm_button")
                ) {
                    Text("შენახვა", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isAddingPhone = false
                        phoneToEdit = null
                    }
                ) {
                    Text("გაუქმება", color = textSecondary)
                }
            }
        )
    }

    // =========================================================================
    // ტელეფონის ნომრის წაშლის დადასტურების დიალოგი
    // =========================================================================
    if (phoneToDelete != null) {
        val phone = phoneToDelete!!
        AlertDialog(
            onDismissRequest = { phoneToDelete = null },
            title = { Text("ნომრის წაშლა", color = textPrimary) },
            text = {
                Text(
                    text = "ნამდვილად გსურთ ნომრის „${phone.label}: ${phone.number}“ წაშლა?",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val filtered = config.phoneNumbers.filterNot { it.id == phone.id }
                        val reordered = filtered.mapIndexed { idx, item -> item.copy(displayOrder = idx + 1) }
                        val newCfg = config.copy(phoneNumbers = reordered)
                        config = newCfg
                        ContactAndSocialManager.saveLocalConfig(context, newCfg)
                        phoneToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F)),
                    modifier = Modifier.testTag("admin_confirm_delete_phone_button")
                ) {
                    Text("წაშლა", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { phoneToDelete = null }) {
                    Text("გაუქმება", color = textSecondary)
                }
            }
        )
    }

    // =========================================================================
    // სოციალური ბმულის დამატების / რედაქტირების დიალოგი
    // =========================================================================
    if (isAddingSocial || socialToEdit != null) {
        val editing = socialToEdit
        var selectedPlatform by remember { mutableStateOf(editing?.platform ?: SocialPlatform.WHATSAPP) }
        var titleInput by remember { mutableStateOf(editing?.title ?: selectedPlatform.titleKa) }
        var urlInput by remember { mutableStateOf(editing?.url ?: selectedPlatform.defaultUrlPrefix) }
        var isEnabledInput by remember { mutableStateOf(editing?.isEnabled ?: true) }
        var validationError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                isAddingSocial = false
                socialToEdit = null
            },
            title = {
                Text(
                    text = if (editing != null) "სოციალური ბმულის რედაქტირება" else "ახალი სოციალური ბმულის დამატება",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // პლატფორმის არჩევა
                    Text(text = "პლატფორმა:", color = textSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SocialPlatform.entries.forEach { p ->
                            val isSel = selectedPlatform == p
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(p.brandColorHex).copy(alpha = 0.25f) else cardBg,
                                border = BorderStroke(1.dp, if (isSel) Color(p.brandColorHex) else borderCol),
                                modifier = Modifier
                                    .clickable {
                                        selectedPlatform = p
                                        if (titleInput.isBlank() || SocialPlatform.entries.any { it.titleKa == titleInput }) {
                                            titleInput = p.titleKa
                                        }
                                        if (urlInput.isBlank() || SocialPlatform.entries.any { it.defaultUrlPrefix == urlInput }) {
                                            urlInput = p.defaultUrlPrefix
                                        }
                                        validationError = null
                                    }
                                    .testTag("admin_platform_${p.key}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SocialPlatformBrandIcon(platform = p, containerSize = 16.dp)
                                    Text(
                                        text = p.titleKa,
                                        color = if (isSel) textPrimary else textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("დასახელება (სათაური)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_social_title_input")
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            validationError = null
                        },
                        label = { Text("ბმული (მაგ: https://wa.me/995...)") },
                        singleLine = true,
                        isError = validationError != null,
                        supportingText = {
                            if (validationError != null) {
                                Text(validationError!!, color = Color(0xFFFF4D4F), fontSize = 11.sp)
                            } else {
                                Text("მხარდაჭერილია https://, wa.me და t.me ბმულები", color = textSecondary, fontSize = 10.sp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_social_url_input")
                    )

                    // ცოცხალი Preview
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, Color(selectedPlatform.brandColorHex).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("კლიენტის Preview:", color = textSecondary, fontSize = 10.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SocialPlatformBrandIcon(platform = selectedPlatform, containerSize = 18.dp)
                                Text(
                                    text = titleInput.ifBlank { selectedPlatform.titleKa },
                                    color = textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "სტატუსი: აქტიური", color = textPrimary, fontSize = 13.sp)
                        Switch(
                            checked = isEnabledInput,
                            onCheckedChange = { isEnabledInput = it },
                            modifier = Modifier.testTag("admin_dialog_social_switch")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!ContactAndSocialManager.isValidSocialUrl(urlInput)) {
                            validationError = "გთხოვთ შეიყვანოთ სწორი და უსაფრთხო ბმული (მაგ: https://...)"
                            return@Button
                        }

                        if (editing != null) {
                            val updated = config.socialLinks.map {
                                if (it.id == editing.id) {
                                    it.copy(
                                        platform = selectedPlatform,
                                        title = titleInput.trim().ifBlank { selectedPlatform.titleKa },
                                        url = urlInput.trim(),
                                        isEnabled = isEnabledInput
                                    )
                                } else it
                            }
                            val newCfg = config.copy(socialLinks = updated)
                            config = newCfg
                            ContactAndSocialManager.saveLocalConfig(context, newCfg)
                        } else {
                            val newSocial = SocialMediaLink(
                                id = UUID.randomUUID().toString(),
                                platform = selectedPlatform,
                                title = titleInput.trim().ifBlank { selectedPlatform.titleKa },
                                url = urlInput.trim(),
                                isEnabled = isEnabledInput,
                                displayOrder = config.socialLinks.size + 1
                            )
                            val newCfg = config.copy(socialLinks = config.socialLinks + newSocial)
                            config = newCfg
                            ContactAndSocialManager.saveLocalConfig(context, newCfg)
                        }
                        isAddingSocial = false
                        socialToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6A00)),
                    modifier = Modifier.testTag("admin_social_dialog_confirm_button")
                ) {
                    Text("შენახვა", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isAddingSocial = false
                        socialToEdit = null
                    }
                ) {
                    Text("გაუქმება", color = textSecondary)
                }
            }
        )
    }

    // =========================================================================
    // სოციალური ბმულის წაშლის დადასტურების დიალოგი
    // =========================================================================
    if (socialToDelete != null) {
        val link = socialToDelete!!
        AlertDialog(
            onDismissRequest = { socialToDelete = null },
            title = { Text("ბმულის წაშლა", color = textPrimary) },
            text = {
                Text(
                    text = "ნამდვილად გსურთ „${link.title.ifBlank { link.platform.titleKa }}“ ბმულის წაშლა?",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val filtered = config.socialLinks.filterNot { it.id == link.id }
                        val reordered = filtered.mapIndexed { idx, item -> item.copy(displayOrder = idx + 1) }
                        val newCfg = config.copy(socialLinks = reordered)
                        config = newCfg
                        ContactAndSocialManager.saveLocalConfig(context, newCfg)
                        socialToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F)),
                    modifier = Modifier.testTag("admin_confirm_delete_social_button")
                ) {
                    Text("წაშლა", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { socialToDelete = null }) {
                    Text("გაუქმება", color = textSecondary)
                }
            }
        )
    }
}
