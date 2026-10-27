package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AdminAddProductModal(
    onDismiss: () -> Unit,
    onAddProduct: (title: String, price: String, rating: String, imageUrl: String, category: String) -> Unit,
    onOpenFullAdmin: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("5.0") }
    var imageUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ეკრანები") }
    var isSaving by remember { mutableStateOf(false) }

    val presetImages = listOf(
        "⚡ Software" to "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600",
        "🔓 Unlock" to "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600",
        "⚙️ System" to "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=600",
        "📱 OLED" to "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600",
        "🔋 Battery" to "https://images.unsplash.com/photo-1591337676887-a217a6970a8a?w=600",
        "📸 Camera" to "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600",
        "🔊 Speaker" to "https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=600"
    )

    val softwarePresets = listOf(
        Triple("iCloud / Activation მოხსნა", "350 ₾", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600"),
        Triple("Google FRP ანგარიშის Bypass", "80 ₾", "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600"),
        Triple("Firmware Flash / iOS & Android", "50 ₾", "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=600"),
        Triple("Baseband & IMEI ქსელის აღდგენა", "120 ₾", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600"),
        Triple("სისტემის სრული დიაგნოსტიკა", "30 ₾", "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=600"),
        Triple("SIM Carrier Unlock", "100 ₾", "https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=600")
    )

    val categoriesList = listOf("პროგრამული სერვისები", "ეკრანები", "ელემენტები", "კამერები", "დინამიკები", "დამტენის პორტები", "კორპუსები", "აქსესუარები")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .imePadding()
                .clickable { onDismiss() }
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF16191E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(enabled = false) {} // prevent dismissing on dialog content click
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Title and Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚙️ ადმინ პანელი - პროდუქტის დამატება",
                            color = Color.White,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Text(
                        text = "Firebase Firestore ბაზაში რეალურ დროში დამატება",
                        color = Color(0xFFA0A5B0),
                        fontSize = 12.sp
                    )

                    // დასახელება
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("დასახელება (მაგ: iPhone 16 Pro)", color = Color(0xFF888888), fontSize = 13.sp) },
                        label = { Text("დასახელება", color = Color(0xFFFF7A00)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7A00),
                            unfocusedBorderColor = Color(0xFF2A2E38),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF111317),
                            unfocusedContainerColor = Color(0xFF111317)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ფასი
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        placeholder = { Text("ფასი (მაგ: 390 ₾)", color = Color(0xFF888888), fontSize = 13.sp) },
                        label = { Text("ფასი", color = Color(0xFFFF7A00)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7A00),
                            unfocusedBorderColor = Color(0xFF2A2E38),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF111317),
                            unfocusedContainerColor = Color(0xFF111317)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // რეიტინგი
                    OutlinedTextField(
                        value = rating,
                        onValueChange = { rating = it },
                        placeholder = { Text("რეიტინგი (მაგ: 5.0)", color = Color(0xFF888888), fontSize = 13.sp) },
                        label = { Text("რეიტინგი", color = Color(0xFFFF7A00)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF7A00),
                            unfocusedBorderColor = Color(0xFF2A2E38),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF111317),
                            unfocusedContainerColor = Color(0xFF111317)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // კატეგორია
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("კატეგორია:", color = Color(0xFFA0A5B0), fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categoriesList) { cat ->
                                val isSelected = category == cat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFFF7A00).copy(alpha = 0.2f) else Color(0xFF111317),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFFF7A00) else Color(0xFF2A2E38)
                                    ),
                                    modifier = Modifier.clickable { category = cat }
                                ) {
                                    Text(
                                        cat,
                                        color = if (isSelected) Color(0xFFFF7A00) else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // პროგრამული სერვისების სწრაფი შაბლონები
                    if (category == "პროგრამული სერვისები") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⚡ სწრაფი პროგრამული შაბლონები (1-დაწკაპუნებით):", color = Color(0xFFFF7A00), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(softwarePresets) { preset ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E222B),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF7A00).copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable {
                                            title = preset.first
                                            price = preset.second
                                            imageUrl = preset.third
                                        }
                                    ) {
                                        Text(
                                            preset.first,
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // სურათის URL
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            placeholder = { Text("სურათის URL ლინკი", color = Color(0xFF888888), fontSize = 13.sp) },
                            label = { Text("სურათის URL", color = Color(0xFFFF7A00)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF7A00),
                                unfocusedBorderColor = Color(0xFF2A2E38),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF111317),
                                unfocusedContainerColor = Color(0xFF111317)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // სწრაფი სურათის ჩასმა
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("სწრაფი ფოტო:", color = Color(0xFF7E8492), fontSize = 11.sp)
                            presetImages.forEach { (name, url) ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1C1F26),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E38)),
                                    modifier = Modifier.clickable { imageUrl = url }
                                ) {
                                    Text(
                                        name,
                                        color = Color(0xFFFF7A00),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ღილაკი: დამატება ბაზაში
                    Button(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                onAddProduct(
                                    title.trim(),
                                    if (price.contains("₾")) price.trim() else "${price.trim()} ₾",
                                    rating.trim(),
                                    if (imageUrl.isBlank()) "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600" else imageUrl.trim(),
                                    category
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isSaving) "ინახება Firestore-ში..." else "დამატება ბაზაში",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ღილაკი: დახურვა
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333842)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("დახურვა", color = Color(0xFFA0A5B0), fontSize = 13.sp)
                    }

                    // სრული მართვის პანელი (PIN)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1C1F26),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenFullAdmin()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFFFF7A00), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("სრული ადმინ პანელი (ფასები, შეკვეთები, PIN)", color = Color(0xFFFF7A00), fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}
