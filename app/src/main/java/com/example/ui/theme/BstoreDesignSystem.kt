package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// ==============================================================================
// BSTORE UNIFIED DESIGN SYSTEM TOKENS & REUSABLE COMPONENTS
// ==============================================================================

object BstoreRadii {
    val Badge: Dp = 6.dp
    val Chip: Dp = 10.dp
    val Control: Dp = 12.dp
    val Card: Dp = 16.dp
    val Dialog: Dp = 20.dp
    val Pill: Dp = 24.dp
}

object BstoreDimens {
    val ButtonPrimaryMinHeight: Dp = 48.dp
    val ButtonSecondaryMinHeight: Dp = 44.dp
    val ButtonCompactMinHeight: Dp = 38.dp
    val ChipMinHeight: Dp = 36.dp
    val InputMinHeight: Dp = 48.dp
    val IconSm: Dp = 15.dp
    val IconMd: Dp = 18.dp
    val IconLg: Dp = 22.dp
    val IconTextGap: Dp = 6.dp
    val IconTextGapPrimary: Dp = 8.dp
    val BorderThin: Dp = 1.dp
    val BorderFocused: Dp = 1.5.dp
    val ScreenHorizontalPadding: Dp = 14.dp
    val SectionSpacing: Dp = 14.dp
    val CardPadding: Dp = 14.dp
    val GridSpacing: Dp = 10.dp
}

@Immutable
data class BstoreUiPalette(
    val isDark: Boolean,
    val pageBg: Color,
    val cardBg: Color,
    val cardElevatedBg: Color,
    val subSurfaceBg: Color,
    val chipBg: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val brandOrange: Color,
    val brandOrangeHighlight: Color,
    val brandOrangeSoft: Color,
    val brandOrangeBorder: Color,
    val successGreen: Color,
    val successSoft: Color,
    val warningAmber: Color,
    val errorRed: Color,
    val errorSoft: Color,
    val infoBlue: Color
)

fun bstorePalette(isDark: Boolean): BstoreUiPalette {
    return if (isDark) {
        BstoreUiPalette(
            isDark = true,
            pageBg = Color(0xFF0D0F12),
            cardBg = Color(0xFF15181E),
            cardElevatedBg = Color(0xFF1B1F27),
            subSurfaceBg = Color(0xFF101318),
            chipBg = Color(0xFF1B2029),
            borderSubtle = Color(0xFF262B35),
            borderStrong = Color(0xFF333B49),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFA0A8B5),
            textMuted = Color(0xFF707887),
            brandOrange = Color(0xFFFF6B00),
            brandOrangeHighlight = Color(0xFFFF7A00),
            brandOrangeSoft = Color(0xFFFF6B00).copy(alpha = 0.14f),
            brandOrangeBorder = Color(0xFFFF6B00).copy(alpha = 0.45f),
            successGreen = Color(0xFF22C55E),
            successSoft = Color(0xFF16A34A).copy(alpha = 0.16f),
            warningAmber = Color(0xFFF59E0B),
            errorRed = Color(0xFFEF4444),
            errorSoft = Color(0xFFEF4444).copy(alpha = 0.14f),
            infoBlue = Color(0xFF38BDF8)
        )
    } else {
        BstoreUiPalette(
            isDark = false,
            pageBg = SoftGraphiteLightBgPrimary,
            cardBg = SoftGraphiteLightSurfacePrimary,
            cardElevatedBg = SoftGraphiteLightSurfaceElevated,
            subSurfaceBg = SoftGraphiteLightBgSecondary,
            chipBg = SoftGraphiteLightSurfaceSecondary,
            borderSubtle = SoftGraphiteLightBorder,
            borderStrong = SoftGraphiteLightDisabled,
            textPrimary = SoftGraphiteLightTextPrimary,
            textSecondary = SoftGraphiteLightTextSecondary,
            textMuted = SoftGraphiteLightTextTertiary,
            brandOrange = SoftGraphiteLightBrandOrange,
            brandOrangeHighlight = SoftGraphiteLightSecondaryOrange,
            brandOrangeSoft = SoftGraphiteLightSoftOrangeBg,
            brandOrangeBorder = SoftGraphiteLightBrandOrange.copy(alpha = 0.42f),
            successGreen = Color(0xFF16A34A),
            successSoft = Color(0xFF16A34A).copy(alpha = 0.10f),
            warningAmber = Color(0xFFD97706),
            errorRed = Color(0xFFDC2626),
            errorSoft = Color(0xFFDC2626).copy(alpha = 0.10f),
            infoBlue = Color(0xFF0284C7)
        )
    }
}

enum class BstoreButtonSize(val minHeight: Dp, val horizontalPad: Dp, val verticalPad: Dp, val fontSizeSp: Float) {
    PRIMARY(BstoreDimens.ButtonPrimaryMinHeight, 16.dp, 10.dp, 13.5f),
    SECONDARY(BstoreDimens.ButtonSecondaryMinHeight, 14.dp, 9.dp, 12.5f),
    COMPACT(BstoreDimens.ButtonCompactMinHeight, 12.dp, 7.dp, 11.5f)
}

@Composable
fun BstorePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    size: BstoreButtonSize = BstoreButtonSize.PRIMARY,
    containerColor: Color = Color(0xFFFF6B00),
    contentColor: Color = Color.White,
    maxLines: Int = 2,
    shape: Shape = RoundedCornerShape(BstoreRadii.Control)
) {
    val effectiveEnabled = enabled && !isLoading
    Button(
        onClick = onClick,
        enabled = effectiveEnabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFF262B35),
            disabledContentColor = Color(0xFF8B93A3)
        ),
        contentPadding = PaddingValues(horizontal = size.horizontalPad, vertical = size.verticalPad),
        modifier = modifier.heightIn(min = size.minHeight)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(BstoreDimens.IconMd)
            )
            Spacer(modifier = Modifier.width(BstoreDimens.IconTextGap))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (effectiveEnabled) contentColor else Color(0xFF8B93A3),
                modifier = Modifier.size(if (size == BstoreButtonSize.COMPACT) BstoreDimens.IconSm else BstoreDimens.IconMd)
            )
            Spacer(modifier = Modifier.width(BstoreDimens.IconTextGapPrimary))
        }
        Text(
            text = text,
            color = if (effectiveEnabled) contentColor else Color(0xFF8B93A3),
            fontSize = size.fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (size.fontSizeSp + 3.5f).sp,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BstoreSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDark: Boolean = true,
    leadingIcon: ImageVector? = null,
    emojiPrefix: String? = null,
    size: BstoreButtonSize = BstoreButtonSize.SECONDARY,
    containerColor: Color? = null,
    borderColor: Color? = null,
    contentColor: Color? = null,
    maxLines: Int = 2
) {
    val p = bstorePalette(isDark)
    val bg = containerColor ?: p.cardElevatedBg
    val border = borderColor ?: p.borderStrong
    val fg = contentColor ?: p.textPrimary

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(BstoreRadii.Control),
        border = BorderStroke(BstoreDimens.BorderThin, if (enabled) border else p.borderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (enabled) bg else p.subSurfaceBg,
            contentColor = if (enabled) fg else p.textMuted,
            disabledContainerColor = p.subSurfaceBg,
            disabledContentColor = p.textMuted
        ),
        contentPadding = PaddingValues(horizontal = size.horizontalPad, vertical = size.verticalPad),
        modifier = modifier.heightIn(min = size.minHeight)
    ) {
        if (!emojiPrefix.isNullOrBlank()) {
            Text(text = emojiPrefix, fontSize = (size.fontSizeSp + 1f).sp)
            Spacer(modifier = Modifier.width(BstoreDimens.IconTextGap))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (enabled) fg else p.textMuted,
                modifier = Modifier.size(if (size == BstoreButtonSize.COMPACT) BstoreDimens.IconSm else BstoreDimens.IconMd)
            )
            Spacer(modifier = Modifier.width(BstoreDimens.IconTextGap))
        }
        Text(
            text = text,
            color = if (enabled) fg else p.textMuted,
            fontSize = size.fontSizeSp.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = (size.fontSizeSp + 3.5f).sp,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BstoreOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDark: Boolean = true,
    leadingIcon: ImageVector? = null,
    size: BstoreButtonSize = BstoreButtonSize.SECONDARY,
    accentColor: Color? = null,
    maxLines: Int = 2
) {
    val p = bstorePalette(isDark)
    val tint = accentColor ?: p.textSecondary
    val border = accentColor?.copy(alpha = 0.55f) ?: p.borderStrong
    BstoreSecondaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isDark = isDark,
        leadingIcon = leadingIcon,
        size = size,
        containerColor = Color.Transparent,
        borderColor = border,
        contentColor = tint,
        maxLines = maxLines
    )
}

@Composable
fun BstoreDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDark: Boolean = true,
    filled: Boolean = false,
    leadingIcon: ImageVector? = null,
    size: BstoreButtonSize = BstoreButtonSize.SECONDARY,
    maxLines: Int = 1
) {
    val p = bstorePalette(isDark)
    if (filled) {
        BstorePrimaryButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            leadingIcon = leadingIcon,
            size = size,
            containerColor = p.errorRed,
            contentColor = Color.White,
            maxLines = maxLines
        )
    } else {
        BstoreSecondaryButton(
            text = text,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            isDark = isDark,
            leadingIcon = leadingIcon,
            size = size,
            containerColor = p.errorSoft,
            borderColor = p.errorRed.copy(alpha = 0.45f),
            contentColor = p.errorRed,
            maxLines = maxLines
        )
    }
}

@Composable
fun BstoreTextActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color(0xFFFF6B00),
    leadingIcon: ImageVector? = null
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(BstoreRadii.Chip),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = modifier.heightIn(min = 36.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(BstoreDimens.IconSm)
            )
            Spacer(modifier = Modifier.width(BstoreDimens.IconTextGap))
        }
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BstoreIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDark: Boolean = true,
    highlighted: Boolean = false,
    tint: Color? = null,
    containerColor: Color? = null,
    borderColor: Color? = null,
    size: Dp = 40.dp,
    shape: Shape = RoundedCornerShape(BstoreRadii.Chip)
) {
    val p = bstorePalette(isDark)
    val resolvedBg = containerColor ?: if (highlighted) p.brandOrangeSoft else p.chipBg
    val resolvedBorder = borderColor ?: if (highlighted) p.brandOrangeBorder else p.borderSubtle
    val resolvedTint = tint ?: if (highlighted) p.brandOrange else p.textSecondary

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
    ) {
        Surface(
            shape = shape,
            color = resolvedBg,
            border = BorderStroke(BstoreDimens.BorderThin, resolvedBorder),
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enabled) { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (enabled) resolvedTint else p.textMuted,
                    modifier = Modifier.size(BstoreDimens.IconMd)
                )
            }
        }
    }
}

@Composable
fun BstoreFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDark: Boolean = true,
    leadingEmoji: String? = null,
    leadingIcon: ImageVector? = null
) {
    val p = bstorePalette(isDark)
    val bgColor by animateColorAsState(
        targetValue = if (selected) p.brandOrange else p.cardBg,
        animationSpec = tween(160, easing = FastOutSlowInEasing),
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) p.brandOrange else p.borderStrong,
        animationSpec = tween(160),
        label = "chipBorder"
    )

    Surface(
        shape = RoundedCornerShape(BstoreRadii.Chip),
        color = bgColor,
        border = BorderStroke(BstoreDimens.BorderThin, borderColor),
        modifier = modifier
            .heightIn(min = BstoreDimens.ChipMinHeight)
            .clickable(enabled = enabled) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (!leadingEmoji.isNullOrBlank()) {
                Text(text = leadingEmoji, fontSize = 12.sp)
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (selected) Color.White else p.brandOrange,
                    modifier = Modifier.size(BstoreDimens.IconSm)
                )
            }
            Text(
                text = text,
                color = if (selected) Color.White else p.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class BstoreSegmentOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val testTag: String = ""
)

@Composable
fun <T> BstoreSegmentedSelector(
    options: List<BstoreSegmentOption<T>>,
    selectedValue: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    val p = bstorePalette(isDark)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF131720) else Color(0xFFEEF2F7),
        border = BorderStroke(BstoreDimens.BorderThin, p.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { opt ->
                val isSelected = selectedValue == opt.value
                val itemBg by animateColorAsState(
                    targetValue = if (isSelected) p.brandOrange else Color.Transparent,
                    animationSpec = tween(180),
                    label = "segBg_${opt.label}"
                )
                Surface(
                    shape = RoundedCornerShape(BstoreRadii.Chip),
                    color = itemBg,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = BstoreDimens.ButtonCompactMinHeight)
                        .then(if (opt.testTag.isNotBlank()) Modifier.testTag(opt.testTag) else Modifier)
                        .clickable { onSelect(opt.value) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (opt.icon != null) {
                            Icon(
                                imageVector = opt.icon,
                                contentDescription = opt.label,
                                tint = if (isSelected) Color.White else p.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = opt.label,
                            color = if (isSelected) Color.White else p.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BstoreCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    accentBorder: Color? = null,
    containerColor: Color? = null,
    shape: Shape = RoundedCornerShape(BstoreRadii.Card),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = bstorePalette(isDark)
    Surface(
        shape = shape,
        color = containerColor ?: p.cardBg,
        border = BorderStroke(BstoreDimens.BorderThin, accentBorder ?: p.borderSubtle),
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    ) {
        Column(content = content)
    }
}

@Composable
fun BstoreSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    inputTestTag: String = "",
    isDark: Boolean = true,
    onSearchAction: () -> Unit = {}
) {
    val p = bstorePalette(isDark)
    val isActive = query.isNotEmpty()
    val borderColor by animateColorAsState(
        targetValue = if (isActive) p.brandOrange else p.borderStrong,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "bstoreSearchBorder"
    )
    Surface(
        shape = RoundedCornerShape(BstoreRadii.Control),
        color = p.cardBg,
        border = BorderStroke(BstoreDimens.BorderThin, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BstoreDimens.InputMinHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "ძებნა",
                tint = if (isActive) p.brandOrange else p.textSecondary,
                modifier = Modifier.size(BstoreDimens.IconMd)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = p.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(p.brandOrange),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
                modifier = Modifier
                    .weight(1f)
                    .then(if (inputTestTag.isNotBlank()) Modifier.testTag(inputTestTag) else Modifier),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = p.textMuted,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            )
            if (isActive) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "გასუფთავება",
                        tint = p.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BstoreOutlinedInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isDark: Boolean = true,
    accentColor: Color = Color(0xFFFF6B00),
    leadingIcon: ImageVector? = null
) {
    val p = bstorePalette(isDark)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        placeholder = if (placeholder.isNotBlank()) {
            {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = p.textMuted
                )
            }
        } else null,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(BstoreDimens.IconMd)
                )
            }
        } else null,
        singleLine = singleLine,
        enabled = enabled,
        shape = RoundedCornerShape(BstoreRadii.Control),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = p.textPrimary,
            unfocusedTextColor = p.textPrimary,
            focusedContainerColor = p.subSurfaceBg,
            unfocusedContainerColor = p.subSurfaceBg,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = p.borderStrong,
            focusedLabelColor = accentColor,
            unfocusedLabelColor = p.textSecondary
        ),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BstoreDimens.InputMinHeight)
    )
}

@Composable
fun BstoreDialogShell(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    borderColor: Color? = null,
    maxWidth: Dp = 480.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = bstorePalette(isDark)
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(BstoreRadii.Dialog),
            color = p.cardBg,
            border = BorderStroke(BstoreDimens.BorderFocused, borderColor ?: p.borderStrong),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = maxWidth)
        ) {
            Column(content = content)
        }
    }
}

/**
 * Shared atmospheric frosted glass wave/glow background matching BSTORE's signature visual style
 * in both Dark and Light themes.
 */
@Composable
fun BstoreAmbientGlassBackground(
    isDark: Boolean,
    stepShift: Float = 0f,
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
        val infiniteTransition = rememberInfiniteTransition(label = "BstoreAmbientLight")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BstoreAmbientLightDrift"
        )
        animated
    }

    val primaryRibbonPath = remember { Path() }
    val secondaryRibbonPath = remember { Path() }

    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val baseBrush = Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        Color(0xFF07080B),
                        Color(0xFF0B0D12),
                        Color(0xFF06070A)
                    )
                } else {
                    listOf(
                        SoftGraphiteLightBgPrimary,
                        SoftGraphiteLightBgSecondary,
                        SoftGraphiteLightBgPrimary
                    )
                }
            )

            onDrawBehind {
                drawRect(brush = baseBrush)

                val phase = (driftPhase + stepShift).coerceIn(0f, 1f)

                val topLightCenter = Offset(
                    x = w * (0.24f + 0.18f * phase),
                    y = h * (0.18f + 0.08f * (1f - phase))
                )
                val topLightRadius = maxOf(w, h) * 0.58f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFFFF7A00).copy(alpha = 0.14f),
                                Color(0xFFD95B00).copy(alpha = 0.06f),
                                Color(0xFF6B2B00).copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFFFF8A24).copy(alpha = 0.12f),
                                Color(0xFFFFAE52).copy(alpha = 0.065f),
                                Color(0xFFFFDEC2).copy(alpha = 0.025f),
                                Color.Transparent
                            )
                        },
                        center = topLightCenter,
                        radius = topLightRadius
                    ),
                    radius = topLightRadius,
                    center = topLightCenter
                )

                val bottomLightCenter = Offset(
                    x = w * (0.78f - 0.16f * phase),
                    y = h * (0.78f - 0.07f * phase)
                )
                val bottomLightRadius = maxOf(w, h) * 0.52f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFFFF7A00).copy(alpha = 0.11f),
                                Color(0xFFB84A00).copy(alpha = 0.045f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFFFFA94D).copy(alpha = 0.09f),
                                Color(0xFF38BDF8).copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        },
                        center = bottomLightCenter,
                        radius = bottomLightRadius
                    ),
                    radius = bottomLightRadius,
                    center = bottomLightCenter
                )

                if (!isDark) {
                    val skyAccentCenter = Offset(
                        x = w * (0.82f - 0.12f * phase),
                        y = h * (0.14f + 0.06f * phase)
                    )
                    val skyAccentRadius = maxOf(w, h) * 0.44f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.07f),
                                Color(0xFFE0EEFF).copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = skyAccentCenter,
                            radius = skyAccentRadius
                        ),
                        radius = skyAccentRadius,
                        center = skyAccentCenter
                    )
                }

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
                        colors = if (isDark) {
                            listOf(
                                Color.Transparent,
                                Color(0xFFFF8820).copy(alpha = 0.11f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.Transparent,
                                Color(0xFFFF7A00).copy(alpha = 0.16f),
                                Color.White.copy(alpha = 0.72f),
                                Color.Transparent
                            )
                        }
                    ),
                    style = Stroke(width = if (isDark) 2.2.dp.toPx() else 2.4.dp.toPx(), cap = StrokeCap.Round)
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
                        colors = if (isDark) {
                            listOf(
                                Color.Transparent,
                                Color(0xFFFF7A00).copy(alpha = 0.09f),
                                Color.White.copy(alpha = 0.03f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.Transparent,
                                Color(0xFFFF8A24).copy(alpha = 0.13f),
                                Color(0xFF38BDF8).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        }
                    ),
                    style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    )
}
