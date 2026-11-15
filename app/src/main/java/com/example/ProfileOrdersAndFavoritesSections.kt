package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.*

fun isOrderCompletedOrCancelled(status: String): Boolean {
    return status == "შესრულებული" || status == "დასრულებული" || status == "გაუქმებული"
}

@Composable
fun ProfileOrdersSection(
    ordersList: List<OrderRecord>,
    selectedOrderFilter: String,
    onFilterChange: (String) -> Unit,
    isOrdersLoading: Boolean,
    ordersError: String?,
    products: List<ProductItem>,
    onRetryOrders: () -> Unit,
    onOpenAllOrders: () -> Unit,
    onOpenOrderDetail: (OrderRecord) -> Unit,
    onOpenPaymentInstructions: (OrderRecord) -> Unit,
    onOpenCatalog: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    val activeCount = remember(ordersList) {
        ordersList.count { !isOrderCompletedOrCancelled(it.status) }
    }
    val completedCount = remember(ordersList) {
        ordersList.size - activeCount
    }
    val sortedFilteredOrders = remember(ordersList, selectedOrderFilter) {
        ordersList
            .filter { ord ->
                val isDone = isOrderCompletedOrCancelled(ord.status)
                if (selectedOrderFilter == "დასრულებული") isDone else !isDone
            }
            .sortedByDescending { it.createdAtMillis }
    }
    val previewOrders = remember(sortedFilteredOrders) {
        sortedFilteredOrders.take(3)
    }

    BstoreCard(
        isDark = isDark,
        accentBorder = if (activeCount > 0) p.brandOrangeBorder else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_orders_section")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section Header: Orange box icon + title + "ნახვა ყველა >"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "ჩემი შეკვეთები",
                        tint = p.brandOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ჩემი შეკვეთები",
                        color = p.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (ordersList.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenAllOrders() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("profile_orders_view_all_button")
                    ) {
                        Text(
                            text = "ნახვა ყველა",
                            color = p.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = p.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Segmented Pill Tabs matching mockup: "აქტიური (n)" / "დასრულებული (m)"
            BstoreSegmentedSelector(
                options = listOf(
                    BstoreSegmentOption(
                        value = "აქტიური",
                        label = "აქტიური ($activeCount)",
                        icon = Icons.Default.ShoppingCart,
                        testTag = "profile_orders_tab_აქტიური"
                    ),
                    BstoreSegmentOption(
                        value = "დასრულებული",
                        label = "დასრულებული ($completedCount)",
                        icon = Icons.Default.CheckCircleOutline,
                        testTag = "profile_orders_tab_დასრულებული"
                    )
                ),
                selectedValue = selectedOrderFilter,
                onSelect = onFilterChange,
                isDark = isDark
            )

            // Distinct Loading / Error / Empty / Populated states
            when {
                isOrdersLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp)
                            .testTag("profile_orders_loading_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = p.brandOrange,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "შეკვეთები იტვირთება...",
                                color = p.textSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                !ordersError.isNullOrBlank() -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = p.errorSoft,
                        border = BorderStroke(0.8.dp, p.errorRed.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_orders_error_state")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "შეკვეთების ჩატვირთვა ვერ მოხერხდა",
                                color = p.errorRed,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = ordersError,
                                color = p.textSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            BstoreOutlinedButton(
                                text = "თავიდან ცდა",
                                onClick = onRetryOrders,
                                isDark = isDark,
                                accentColor = p.brandOrange,
                                size = BstoreButtonSize.COMPACT,
                                modifier = Modifier.testTag("profile_orders_retry_button")
                            )
                        }
                    }
                }

                sortedFilteredOrders.isEmpty() -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = p.subSurfaceBg,
                        border = BorderStroke(0.8.dp, p.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_orders_empty_state")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = p.brandOrangeSoft,
                                border = BorderStroke(1.dp, p.brandOrangeBorder),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = p.brandOrange,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (selectedOrderFilter == "აქტიური") {
                                    "აქტიური შეკვეთები არ გაქვთ"
                                } else {
                                    "დასრულებული შეკვეთები არ არის"
                                },
                                color = p.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "ახალი შეკვეთის გაფორმების შემდეგ სტატუსი და დეტალები აქ გამოჩნდება.",
                                color = p.textMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            BstoreSecondaryButton(
                                text = "კატალოგში გადასვლა",
                                onClick = onOpenCatalog,
                                isDark = isDark,
                                leadingIcon = Icons.Default.Storefront,
                                size = BstoreButtonSize.COMPACT,
                                modifier = Modifier.testTag("profile_orders_go_shopping_button")
                            )
                        }
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        previewOrders.forEach { ord ->
                            OrderItemCard(
                                order = ord,
                                products = products,
                                onClick = { onOpenOrderDetail(ord) },
                                onOpenPaymentInstructions = { onOpenPaymentInstructions(ord) }
                            )
                        }
                        if (sortedFilteredOrders.size > previewOrders.size) {
                            BstoreOutlinedButton(
                                text = "ყველა შეკვეთის ნახვა (${sortedFilteredOrders.size})",
                                onClick = onOpenAllOrders,
                                isDark = isDark,
                                accentColor = p.brandOrange,
                                size = BstoreButtonSize.COMPACT,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_orders_show_more_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderProgressStepper(order: OrderRecord) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    val steps = listOf("მიღებული", "მუშავდება", "გზაშია", "დასრულებული")
    val isCancelled = order.status == "გაუქმებული" || order.status == "გაუქმება"

    if (isCancelled) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = p.errorSoft,
            border = BorderStroke(0.8.dp, p.errorRed.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("order_stepper_cancelled_${order.id}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null, tint = p.errorRed, modifier = Modifier.size(14.dp))
                Text(
                    text = "შეკვეთა გაუქმებულია",
                    color = p.errorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    val activeIndex = when (order.status) {
        "ახალი" -> 0
        "მიმდინარე", "მზადდება", "მუშავდება", "დამუშავების პროცესში" -> 1
        "გზაშია" -> 2
        "შესრულებული", "დასრულებული" -> 3
        else -> 1
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_stepper_${order.id}"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { idx, _ ->
                val isCompleted = idx <= activeIndex
                val dotColor = if (isCompleted) p.brandOrange else p.borderStrong
                Box(
                    modifier = Modifier
                        .size(if (idx == activeIndex) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                if (idx < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (idx < activeIndex) p.brandOrange else p.borderSubtle)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            steps.forEachIndexed { idx, label ->
                Text(
                    text = label,
                    color = if (idx <= activeIndex) p.textPrimary else p.textMuted,
                    fontSize = 9.5.sp,
                    fontWeight = if (idx == activeIndex) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun OrderItemCard(
    order: OrderRecord,
    products: List<ProductItem> = emptyList(),
    onClick: () -> Unit,
    onOpenPaymentInstructions: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    val statusColor = when (order.status) {
        "შესრულებული", "დასრულებული" -> p.successGreen
        "მიმდინარე", "მზადდება", "მუშავდება", "დამუშავების პროცესში" -> p.brandOrange
        "გზაშია", "ახალი" -> p.infoBlue
        "გაუქმებული", "გაუქმება" -> p.errorRed
        else -> p.brandOrange
    }
    val statusBg = statusColor.copy(alpha = if (isDark) 0.16f else 0.12f)

    val paymentMethodType = PaymentMethodType.fromCode(order.paymentMethodCode, order.paymentMethod)
    val paymentStatusEnum = PaymentStatus.fromCode(order.paymentStatus, paymentMethodType)
    val showPayInstructionsBtn = paymentMethodType == PaymentMethodType.BANK_TRANSFER &&
        (paymentStatusEnum == PaymentStatus.PENDING || paymentStatusEnum == PaymentStatus.VERIFICATION_PENDING) &&
        order.status != "გაუქმებული" &&
        BstorePaymentManager.canUserAccessOrderPaymentInstructions(context, order) &&
        onOpenPaymentInstructions != null

    val firstItem = order.items.firstOrNull()
    val matchedCatalogProduct = remember(firstItem?.productTitle, products) {
        if (firstItem == null) null
        else products.find { it.title.equals(firstItem.productTitle, ignoreCase = true) }
    }
    val extraCount = (order.items.size - 1).coerceAtLeast(0)
    val displayTitle = when {
        firstItem != null && extraCount > 0 -> "${firstItem.productTitle} (+$extraCount)"
        firstItem != null -> firstItem.productTitle
        else -> "შეკვეთა ${order.orderNumber}"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = p.subSurfaceBg,
        border = BorderStroke(0.9.dp, p.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("profile_order_card_${order.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Real thumbnail or neutral hardware package visual (never fake external product stock photos)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.cardElevatedBg)
                        .border(0.8.dp, p.borderStrong, RoundedCornerShape(12.dp))
                ) {
                    val itemImg = firstItem?.imageUrl?.trim().orEmpty()
                    when {
                        itemImg.isNotBlank() -> AsyncImage(
                            model = itemImg,
                            contentDescription = displayTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        matchedCatalogProduct != null -> ProductSmartVisual(
                            product = matchedCatalogProduct,
                            modifier = Modifier.fillMaxSize(),
                            compact = true
                        )
                        else -> Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = p.brandOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayTitle,
                        color = p.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "შეკვეთა ${order.orderNumber} • ${order.date}",
                        color = p.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusBg,
                            border = BorderStroke(0.7.dp, statusColor.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = order.status,
                                color = statusColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (order.totalAmount.isNotBlank()) {
                            Text(
                                text = order.totalAmount,
                                color = p.brandOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "შეკვეთის დეტალები",
                    tint = p.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            OrderProgressStepper(order = order)

            if (paymentStatusEnum == PaymentStatus.VERIFICATION_PENDING) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = p.infoBlue.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, p.infoBlue.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_card_verification_pending_${order.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = p.infoBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "გადარიცხვა მოწმდება ადმინისტრატორის მიერ",
                            color = p.infoBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (showPayInstructionsBtn) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = p.brandOrangeSoft,
                    border = BorderStroke(0.9.dp, p.brandOrangeBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPaymentInstructions?.invoke() }
                        .testTag("order_card_payment_instructions_${order.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = p.brandOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "გადახდის ინსტრუქცია & IBAN",
                                color = p.brandOrange,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = p.brandOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileFavoritesSection(
    favoriteProducts: List<ProductItem>,
    onOpenAllFavorites: () -> Unit,
    onProductClick: (ProductItem) -> Unit,
    onRemoveFavorite: (ProductItem) -> Unit,
    onBrowseCatalog: () -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    BstoreCard(
        isDark = isDark,
        accentBorder = if (favoriteProducts.isNotEmpty()) p.brandOrangeBorder else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_favorites_section")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Orange Heart + "რჩეულები (N)" + "ნახვა ყველა >"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "რჩეულები",
                        tint = p.brandOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "რჩეულები (${favoriteProducts.size})",
                        color = p.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (favoriteProducts.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenAllFavorites() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("profile_favorites_view_all_button")
                    ) {
                        Text(
                            text = "ნახვა ყველა",
                            color = p.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = p.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (favoriteProducts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = p.subSurfaceBg,
                    border = BorderStroke(0.8.dp, p.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_favorites_empty_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = p.brandOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "რჩეული პროდუქტები ჯერ არ გაქვთ",
                            color = p.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "დააჭირეთ გულის ღილაკს კატალოგში სასურველი ნაწილების შესანახად.",
                            color = p.textMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        BstoreSecondaryButton(
                            text = "კატალოგის ნახვა",
                            onClick = onBrowseCatalog,
                            isDark = isDark,
                            leadingIcon = Icons.Default.Storefront,
                            size = BstoreButtonSize.COMPACT,
                            modifier = Modifier.testTag("profile_favorites_browse_catalog_button")
                        )
                    }
                }
            } else {
                // Horizontal thumbnail preview row matching mockup
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_favorites_preview_row"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favoriteProducts, key = { it.id }) { favProd ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = p.subSurfaceBg,
                            border = BorderStroke(0.9.dp, p.borderSubtle),
                            modifier = Modifier
                                .width(104.dp)
                                .clickable { onProductClick(favProd) }
                                .testTag("profile_favorite_item_${favProd.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(7.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(78.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(p.cardElevatedBg)
                                ) {
                                    ProductSmartVisual(
                                        product = favProd,
                                        modifier = Modifier.fillMaxSize(),
                                        compact = true
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(22.dp)
                                            .clickable { onRemoveFavorite(favProd) }
                                            .testTag("profile_remove_favorite_${favProd.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "წაშლა რჩეულებიდან",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = favProd.title,
                                    color = p.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = favProd.price,
                                    color = p.brandOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
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
fun AllOrdersDialog(
    orders: List<OrderRecord>,
    products: List<ProductItem>,
    initialFilter: String,
    onDismiss: () -> Unit,
    onSelectOrder: (OrderRecord) -> Unit,
    onOpenPaymentInstructions: (OrderRecord) -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    var filterTab by remember(initialFilter) { mutableStateOf(initialFilter) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(orders, filterTab, searchQuery) {
        orders
            .filter { ord ->
                val isDone = isOrderCompletedOrCancelled(ord.status)
                val matchesTab = if (filterTab == "დასრულებული") isDone else !isDone
                val q = searchQuery.trim()
                val matchesQuery = q.isBlank() ||
                    ord.orderNumber.contains(q, ignoreCase = true) ||
                    ord.trackingCode.contains(q, ignoreCase = true) ||
                    ord.items.any { it.productTitle.contains(q, ignoreCase = true) }
                matchesTab && matchesQuery
            }
            .sortedByDescending { it.createdAtMillis }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(BstoreRadii.Dialog),
            color = p.cardBg,
            border = BorderStroke(1.dp, p.borderStrong),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("all_orders_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(p.cardElevatedBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ყველა შეკვეთა (${orders.size})",
                        color = p.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = p.textSecondary)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BstoreSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "ძებნა ნომრით ან პროდუქტით...",
                        isDark = isDark
                    )

                    val activeCount = orders.count { !isOrderCompletedOrCancelled(it.status) }
                    val completedCount = orders.size - activeCount
                    BstoreSegmentedSelector(
                        options = listOf(
                            BstoreSegmentOption("აქტიური", "აქტიური ($activeCount)"),
                            BstoreSegmentOption("დასრულებული", "დასრულებული ($completedCount)")
                        ),
                        selectedValue = filterTab,
                        onSelect = { filterTab = it },
                        isDark = isDark
                    )

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ამ კატეგორიაში შეკვეთები ვერ მოიძებნა",
                                color = p.textMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { ord ->
                                OrderItemCard(
                                    order = ord,
                                    products = products,
                                    onClick = { onSelectOrder(ord) },
                                    onOpenPaymentInstructions = { onOpenPaymentInstructions(ord) }
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
fun AllFavoritesDialog(
    favoriteProducts: List<ProductItem>,
    onDismiss: () -> Unit,
    onProductClick: (ProductItem) -> Unit,
    onAddToCart: (ProductItem) -> Unit,
    onRemoveFavorite: (ProductItem) -> Unit
) {
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(favoriteProducts, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) favoriteProducts
        else favoriteProducts.filter {
            it.title.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(BstoreRadii.Dialog),
            color = p.cardBg,
            border = BorderStroke(1.dp, p.borderStrong),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.84f)
                .testTag("all_favorites_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(p.cardElevatedBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "რჩეული პროდუქტები (${favoriteProducts.size})",
                        color = p.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = p.textSecondary)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BstoreSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "ძებნა რჩეულებში...",
                        isDark = isDark
                    )

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "პროდუქტი ვერ მოიძებნა",
                                color = p.textMuted,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { prod ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = p.subSurfaceBg,
                                    border = BorderStroke(0.8.dp, p.borderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onDismiss()
                                            onProductClick(prod)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(p.cardElevatedBg)
                                        ) {
                                            ProductSmartVisual(
                                                product = prod,
                                                modifier = Modifier.fillMaxSize(),
                                                compact = true
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.title,
                                                color = p.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = prod.price,
                                                color = p.brandOrange,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        IconButton(
                                            onClick = { onAddToCart(prod) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddShoppingCart,
                                                contentDescription = "კალათაში დამატება",
                                                tint = p.brandOrange,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onRemoveFavorite(prod) },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "წაშლა რჩეულებიდან",
                                                tint = p.errorRed,
                                                modifier = Modifier.size(19.dp)
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
    }
}
