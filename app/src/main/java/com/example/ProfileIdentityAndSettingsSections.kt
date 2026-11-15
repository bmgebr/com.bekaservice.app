package com.example

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ProfileScreenHeader(isDark: Boolean) {
    val p = bstorePalette(isDark)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp)
            .testTag("profile_header_section"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "პროფილი & ანგარიშები",
            color = p.textPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(p.brandOrange)
        )
    }
}

@Composable
fun UserProfileCard(
    user: UserProfile,
    onEditProfile: () -> Unit,
    onOpenPersonalData: () -> Unit,
    onQuickAddPhone: () -> Unit,
    onQuickAddAddress: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    val resolvedName = user.displayName.trim()
    val initials = remember(user.firstName, user.lastName, resolvedName) {
        val f = user.firstName.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        val l = user.lastName.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        val combined = "$f$l"
        when {
            combined.isNotBlank() -> combined
            resolvedName.isNotBlank() -> resolvedName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.take(1).uppercase() }
            else -> ""
        }
    }
    val isGoogle = user.provider.equals("Google", ignoreCase = true)
    val isApple = user.provider.equals("Apple", ignoreCase = true)

    BstoreCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Identity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar with 2.dp Brand Orange ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(p.subSurfaceBg)
                        .border(2.dp, p.brandOrange, CircleShape)
                        .clickable { onEditProfile() }
                        .testTag("profile_avatar_box")
                ) {
                    if (user.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = "პროფილის ფოტო",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else if (initials.isNotBlank()) {
                        Text(
                            text = initials,
                            color = p.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "მომხმარებლის ავატარი",
                            tint = p.brandOrange,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Name, Email, Provider Pill
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = resolvedName.ifBlank { "მომხმარებელი" },
                            color = p.textPrimary,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onOpenPersonalData,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("profile_personal_data_chevron")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "პირადი მონაცემები",
                                tint = p.textSecondary
                            )
                        }
                    }

                    Text(
                        text = user.email.ifBlank { "ელ. ფოსტა არ არის მითითებული" },
                        color = p.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Provider pill - strictly reflects real auth provider
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when {
                                isGoogle -> p.successSoft
                                else -> p.chipBg
                            },
                            border = BorderStroke(
                                0.8.dp,
                                when {
                                    isGoogle -> p.successGreen.copy(alpha = 0.45f)
                                    else -> p.borderStrong
                                }
                            ),
                            modifier = Modifier.testTag("profile_provider_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (isGoogle) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "G",
                                                color = Color(0xFF16A34A),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Google ანგარიში",
                                        color = p.successGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isApple) Icons.Default.PhoneIphone else Icons.Default.Email,
                                        contentDescription = null,
                                        tint = p.textSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (isApple) "Apple ანგარიში" else "ელ. ფოსტით ავტორიზაცია",
                                        color = p.textSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Compact Edit pill matching mockup
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = p.brandOrangeSoft,
                            border = BorderStroke(1.dp, p.brandOrangeBorder),
                            modifier = Modifier
                                .clickable { onEditProfile() }
                                .testTag("profile_edit_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "რედაქტირება",
                                    tint = p.brandOrange,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "რედაქტირება",
                                    color = p.brandOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = p.borderSubtle, thickness = 0.8.dp)

            // Integrated Phone & Address quick info rows (real data only, preserves required testTags)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileContactFieldRow(
                    icon = Icons.Default.Phone,
                    label = "ტელეფონი",
                    value = user.phone,
                    emptyHint = "არ არის მითითებული",
                    actionTag = if (user.phone.isBlank()) "profile_add_phone_button" else "profile_edit_phone_button",
                    actionText = if (user.phone.isBlank()) "+ დამატება" else "შეცვლა",
                    isDark = isDark,
                    onClick = onQuickAddPhone
                )

                ProfileContactFieldRow(
                    icon = Icons.Default.LocationOn,
                    label = "მისამართი",
                    value = user.address,
                    emptyHint = "არ არის მითითებული",
                    actionTag = if (user.address.isBlank()) "profile_add_address_button" else "profile_edit_address_button",
                    actionText = if (user.address.isBlank()) "+ დამატება" else "შეცვლა",
                    isDark = isDark,
                    onClick = onQuickAddAddress
                )

                if (user.deviceModel.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(p.subSurfaceBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = p.brandOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "მოწყობილობა: ${user.deviceModel}",
                            color = p.textSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Compact Security & Logout quick strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = p.subSurfaceBg,
                    border = BorderStroke(0.8.dp, p.borderStrong),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clickable { onChangePassword() }
                        .testTag("profile_change_password_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = p.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "პაროლის შეცვლა",
                            color = p.textPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = p.errorSoft,
                    border = BorderStroke(0.8.dp, p.errorRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clickable { onLogout() }
                        .testTag("profile_logout_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "გასვლა",
                            tint = p.errorRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "გასვლა",
                            color = p.errorRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContactFieldRow(
    icon: ImageVector,
    label: String,
    value: String,
    emptyHint: String,
    actionTag: String,
    actionText: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val p = bstorePalette(isDark)
    val hasValue = value.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(p.subSurfaceBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = p.brandOrange,
                modifier = Modifier.size(15.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = p.textMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = if (hasValue) value else emptyHint,
                    color = if (hasValue) p.textPrimary else p.textMuted,
                    fontSize = 12.sp,
                    fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (hasValue) p.chipBg else p.brandOrangeSoft,
            border = BorderStroke(0.8.dp, if (hasValue) p.borderStrong else p.brandOrangeBorder),
            modifier = Modifier
                .clickable { onClick() }
                .testTag(actionTag)
        ) {
            Text(
                text = actionText,
                color = if (hasValue) p.textSecondary else p.brandOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun ProfileSecurityAndSettingsCard(
    isAuthenticated: Boolean,
    onOpenSecuritySettings: () -> Unit,
    onOpenPersonalData: () -> Unit,
    onRequestDeleteAccount: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    BstoreCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_settings_group_card")
    ) {
        Column {
            ProfileGroupedActionRow(
                icon = Icons.Default.Security,
                title = "უსაფრთხოება და პარამეტრები",
                isDark = isDark,
                testTag = "profile_row_security_settings",
                onClick = onOpenSecuritySettings
            )
            if (isAuthenticated) {
                HorizontalDivider(
                    color = p.borderSubtle,
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                ProfileGroupedActionRow(
                    icon = Icons.Default.Person,
                    title = "პირადი მონაცემები",
                    isDark = isDark,
                    testTag = "profile_row_personal_data",
                    onClick = onOpenPersonalData
                )
                HorizontalDivider(
                    color = p.borderSubtle,
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                ProfileGroupedActionRow(
                    icon = Icons.Default.DeleteOutline,
                    title = "ანგარიშისა და მონაცემების წაშლა",
                    iconTint = p.errorRed.copy(alpha = 0.85f),
                    isDark = isDark,
                    testTag = "profile_row_delete_account",
                    onClick = onRequestDeleteAccount
                )
            }
        }
    }
}

@Composable
fun ProfilePoliciesGroupedCard(
    onOpenPolicyTopic: (LegalPolicyTopic) -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    BstoreCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_payment_and_policies_card")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = p.brandOrange,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ინფორმაცია და პირობები",
                        color = p.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "მიწოდება, დაბრუნება, საბანკო გადარიცხვა და წესები",
                        color = p.textMuted,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            LegalPolicyTopic.entries.forEach { topic ->
                HorizontalDivider(
                    color = p.borderSubtle,
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                val topicIcon = when (topic) {
                    LegalPolicyTopic.DELIVERY_POLICY -> Icons.Default.LocalShipping
                    LegalPolicyTopic.RETURN_AND_REFUND_POLICY -> Icons.AutoMirrored.Filled.AssignmentReturn
                    LegalPolicyTopic.PRIVACY_POLICY -> Icons.Default.PrivacyTip
                    LegalPolicyTopic.TERMS_AND_CONDITIONS -> Icons.Default.Gavel
                    LegalPolicyTopic.CONTACT_INFO -> Icons.Default.ContactPhone
                }
                ProfileGroupedActionRow(
                    icon = topicIcon,
                    title = topic.titleKa,
                    subtitle = topic.shortSubtitleKa,
                    isDark = isDark,
                    testTag = "policy_topic_item_${topic.id}",
                    onClick = { onOpenPolicyTopic(topic) }
                )
            }
        }
    }
}

@Composable
private fun ProfileGroupedActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color? = null,
    isDark: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val p = bstorePalette(isDark)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint ?: p.textSecondary,
            modifier = Modifier.size(19.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = p.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = p.textMuted,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = p.textMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreContactsCard() {
    val context = LocalContext.current
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    val configVersion = ContactAndSocialManager.configStateVersion.intValue
    val contactConfig = remember(configVersion) { ContactAndSocialManager.currentConfig }

    val activePhones = remember(contactConfig) {
        contactConfig.phoneNumbers
            .filter { it.isEnabled && it.number.isNotBlank() }
            .sortedBy { it.displayOrder }
    }
    val activeSocials = remember(contactConfig) {
        contactConfig.socialLinks
            .filter { it.isEnabled && it.url.isNotBlank() && ContactAndSocialManager.isValidSocialUrl(it.url) }
            .sortedBy { it.displayOrder }
    }

    val serviceAddress = contactConfig.serviceAddress.ifBlank { AppBrandConfig.SERVICE_CENTER_ADDRESS_KA }
    val workingHours = contactConfig.workingHours.ifBlank { AppBrandConfig.WORKING_HOURS_KA }
    val primaryPhone = activePhones.firstOrNull()?.number.orEmpty()
    var isPhonesExpanded by remember { mutableStateOf(false) }

    BstoreCard(
        isDark = isDark,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("store_contacts_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row matching mockup ("🎧 დახმარება და კონტაქტი   >")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = activePhones.isNotEmpty()) { isPhonesExpanded = !isPhonesExpanded }
                    .testTag("store_contacts_expand_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HeadsetMic,
                        contentDescription = "დახმარება და კონტაქტი",
                        tint = p.brandOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "დახმარება და კონტაქტი",
                        color = p.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (activePhones.isNotEmpty()) {
                    Icon(
                        imageVector = if (isPhonesExpanded) Icons.Default.KeyboardArrowUp else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isPhonesExpanded) "დაკეცვა" else "გახსნა",
                        tint = p.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Address Row (clicks open Google Maps / geo URI)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        runCatching {
                            val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(serviceAddress)}")
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, mapUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.onFailure {
                            Toast.makeText(context, serviceAddress, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 2.dp)
                    .testTag("store_contact_address_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "მისამართი",
                    tint = p.brandOrange,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = serviceAddress,
                    color = p.textPrimary,
                    fontSize = 12.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Primary Phone Row (clickable)
            if (primaryPhone.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isPhonesExpanded = !isPhonesExpanded }
                        .padding(vertical = 2.dp)
                        .testTag("store_contact_phone_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "ტელეფონი",
                            tint = p.brandOrange,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = primaryPhone,
                            color = p.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = if (isPhonesExpanded) "დაკეცვა" else "${activePhones.size} ნომერი",
                        color = p.brandOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Working Hours Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "სამუშაო საათები",
                    tint = p.brandOrange,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "სამუშაო საათები: $workingHours",
                    color = p.textSecondary,
                    fontSize = 12.sp
                )
            }

            // Expandable Phone Numbers Card (keeps exact existing testTags)
            if (activePhones.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = p.subSurfaceBg,
                    border = BorderStroke(
                        0.8.dp,
                        if (isPhonesExpanded) p.brandOrangeBorder else p.borderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_phones_collapsible_card")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPhonesExpanded = !isPhonesExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "საკონტაქტო ნომრები (${activePhones.size})",
                                color = p.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (isPhonesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isPhonesExpanded) "Collapse" else "Expand",
                                tint = p.brandOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isPhonesExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                activePhones.forEach { phone ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = p.cardBg,
                                        border = BorderStroke(0.8.dp, p.brandOrangeBorder.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_phone_${phone.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = phone.label.ifBlank { "საკონტაქტო ნომერი" },
                                                    color = p.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = phone.number,
                                                    color = p.textSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = { ContactAndSocialManager.openDialer(context, phone.number) },
                                                colors = ButtonDefaults.buttonColors(containerColor = p.brandOrange),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier
                                                    .height(34.dp)
                                                    .testTag("profile_call_button_${phone.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Call,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "დარეკვა",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
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

            // Social Icons Bar matching mockup
            if (activeSocials.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeSocials.forEach { social ->
                        val brandColor = Color(social.platform.brandColorHex)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = brandColor.copy(alpha = if (isDark) 0.16f else 0.10f),
                            border = BorderStroke(1.dp, brandColor.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .testTag("profile_social_${social.id}")
                                .clickable { ContactAndSocialManager.openSocialLink(context, social) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SocialPlatformBrandIcon(
                                    platform = social.platform,
                                    containerSize = 18.dp
                                )
                                Text(
                                    text = social.title.ifBlank { social.platform.titleKa },
                                    color = p.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = brandColor,
                                    modifier = Modifier.size(11.dp)
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
fun ProfileAdminPanelBanner(
    onOpenAdminPanel: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val goldAccent = Color(0xFFF59E0B)
    val bg = if (isDark) Color(0xFF281C0E) else Color(0xFFFFF7ED)
    val textColor = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.48f)),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable { onOpenAdminPanel() }
            .testTag("profile_admin_panel_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "ადმინისტრატორის პანელი",
                    tint = goldAccent,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "ადმინისტრატორის პანელი",
                    color = textColor,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = goldAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
