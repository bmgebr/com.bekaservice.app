package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShareLocation
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.theme.*

/**
 * BEKASERVICE — სერვისცენტრის ფიზიკური მისამართისა და Google Maps Share ბმულის
 * სრულფასოვანი ადმინ პანელი (Admin Service Center Location Management) - Light & Dark mode optimized.
 */
@Composable
fun AdminServiceCenterLocationView(
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = AppPreferencesManager.isDarkMode
    val p = bstorePalette(isDark)

    val initialConfig = remember { ServiceCenterLocationManager.currentConfig }

    var centerName by remember { mutableStateOf(initialConfig.centerName) }
    var physicalAddress by remember { mutableStateOf(initialConfig.physicalAddress) }
    var googleMapsUrl by remember { mutableStateOf(initialConfig.googleMapsUrl) }
    var phone by remember { mutableStateOf(initialConfig.phone) }
    var workingHours by remember { mutableStateOf(initialConfig.workingHours) }
    var landmark by remember { mutableStateOf(initialConfig.landmark) }

    val servicesList = remember { mutableStateListOf<String>().apply { addAll(initialConfig.services) } }
    var newServiceInput by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Format analysis of the entered location link
    val urlTrimmed = googleMapsUrl.trim()
    val isShareLink = urlTrimmed.contains("maps.app.goo.gl", ignoreCase = true) || urlTrimmed.contains("goo.gl/maps", ignoreCase = true)
    val isGoogleMapsWeb = urlTrimmed.contains("google.com/maps", ignoreCase = true)
    val isCoordinates = Regex("""^([+-]?\d+(?:\.\d+)?)\s*,\s*([+-]?\d+(?:\.\d+)?)$""").matches(urlTrimmed)
    val isGeoUri = urlTrimmed.startsWith("geo:", ignoreCase = true)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(p.pageBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("admin_service_center_location_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card with Back button, Title & Cloud Badge
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = p.cardBg,
                border = BorderStroke(1.dp, p.brandOrangeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = p.brandOrangeSoft,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = p.brandOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "სერვისცენტრის ლოკაცია & რუკა",
                                color = p.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "BEKASERVICE ფიზიკური მისამართი და Google Maps Share ბმული",
                                color = p.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    // Cloud real-time sync badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = p.successSoft,
                        border = BorderStroke(1.dp, p.successGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = p.successGreen, modifier = Modifier.size(15.dp))
                            Text(
                                text = "Firestore: app_config/service_center_location • ცვლილებები მომენტალურად აისახება აპლიკაციაში",
                                color = p.successGreen,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // 2. Google Maps Share Link Hero Section
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = p.cardBg,
                border = BorderStroke(1.2.dp, p.brandOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ShareLocation, contentDescription = null, tint = p.brandOrangeHighlight, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Google Maps Share Location ბმული",
                            color = p.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Link type indicator pill - full width or correctly wrapped below title to prevent clipping
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isShareLink -> p.successSoft
                            isGoogleMapsWeb -> p.chipBg
                            isCoordinates || isGeoUri -> p.brandOrangeSoft
                            urlTrimmed.isBlank() -> p.errorSoft
                            else -> p.subSurfaceBg
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isShareLink -> p.successGreen
                                isGoogleMapsWeb -> p.infoBlue
                                isCoordinates || isGeoUri -> p.warningAmber
                                urlTrimmed.isBlank() -> p.errorRed
                                else -> p.borderStrong
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when {
                                    isShareLink -> "✅ Share Link (რეკომენდებული)"
                                    isGoogleMapsWeb -> "🌐 Google Maps ვებ ბმული"
                                    isCoordinates -> "📍 კოორდინატები (Lat, Lng)"
                                    isGeoUri -> "🧭 Geo URI"
                                    urlTrimmed.isBlank() -> "⚠️ ბმული ცარიელია"
                                    else -> "🔍 საძიებო ტექსტი"
                                },
                                color = when {
                                    isShareLink -> p.successGreen
                                    isGoogleMapsWeb -> p.infoBlue
                                    isCoordinates || isGeoUri -> p.warningAmber
                                    urlTrimmed.isBlank() -> p.errorRed
                                    else -> p.textSecondary
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "ინსტრუქცია: გახსენით Google Maps აპლიკაცია, მონიშნეთ სერვისცენტრის წერტილი, დააჭირეთ „Share“ (გაზიარება) -> „Copy Link“ და ჩასვით აქ. მომხმარებლის მიერ „რუკაზე ნახვა“-ზე დაკლიკებისას გაიხსნება ეს ზუსტი ლოკაცია.",
                        color = p.textSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = googleMapsUrl,
                        onValueChange = { googleMapsUrl = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_center_maps_url_input"),
                        placeholder = { Text("მაგ. https://maps.app.goo.gl/w8G6k689mZqU8Z488 ან 41.6389, 42.9861", color = p.textMuted, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Map, contentDescription = null, tint = p.brandOrange, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (googleMapsUrl.isNotBlank()) {
                                IconButton(onClick = { googleMapsUrl = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "გასუფთავება", tint = p.textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p.brandOrange,
                            unfocusedBorderColor = p.borderSubtle,
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            focusedContainerColor = p.subSurfaceBg,
                            unfocusedContainerColor = p.subSurfaceBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Quick test map button right inside the card
                    Button(
                        onClick = {
                            ServiceCenterLocationManager.openOnMap(
                                context = context,
                                rawInput = googleMapsUrl,
                                fallbackAddress = physicalAddress
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = p.subSurfaceBg),
                        border = BorderStroke(1.dp, p.brandOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("admin_service_center_test_map_button")
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = p.brandOrangeHighlight, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🗺️ რუკის შემოწმება (ტესტირება აპში)",
                            color = p.brandOrangeHighlight,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Physical Address & Store Identity Fields
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = p.cardBg,
                border = BorderStroke(1.dp, p.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ფიზიკური მისამართი და რეკვიზიტები",
                        color = p.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Center Name
                    Text("სერვისცენტრის დასახელება:", color = p.textSecondary, fontSize = 11.5.sp)
                    OutlinedTextField(
                        value = centerName,
                        onValueChange = { centerName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_center_name_input"),
                        placeholder = { Text("BEKASERVICE", color = p.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p.brandOrange,
                            unfocusedBorderColor = p.borderSubtle,
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            focusedContainerColor = p.subSurfaceBg,
                            unfocusedContainerColor = p.subSurfaceBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Physical Address
                    Text("ფიზიკური მისამართი (ქალაქი, ქუჩა, ნომერი):", color = p.textSecondary, fontSize = 11.5.sp)
                    OutlinedTextField(
                        value = physicalAddress,
                        onValueChange = { physicalAddress = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_center_address_input"),
                        placeholder = { Text("კოსტავას N10, ახალციხე", color = p.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p.brandOrange,
                            unfocusedBorderColor = p.borderSubtle,
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            focusedContainerColor = p.subSurfaceBg,
                            unfocusedContainerColor = p.subSurfaceBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Contact Phone
                    Text("სერვისცენტრის საკონტაქტო ტელეფონი:", color = p.textSecondary, fontSize = 11.5.sp)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_center_phone_input"),
                        placeholder = { Text("+995 577 00 11 22", color = p.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p.brandOrange,
                            unfocusedBorderColor = p.borderSubtle,
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            focusedContainerColor = p.subSurfaceBg,
                            unfocusedContainerColor = p.subSurfaceBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Working Hours
                    Text("სამუშაო საათები:", color = p.textSecondary, fontSize = 11.5.sp)
                    OutlinedTextField(
                        value = workingHours,
                        onValueChange = { workingHours = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_center_hours_input"),
                        placeholder = { Text("10:00 – 20:00 (ყოველდღე)", color = p.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p.brandOrange,
                            unfocusedBorderColor = p.borderSubtle,
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            focusedContainerColor = p.subSurfaceBg,
                            unfocusedContainerColor = p.subSurfaceBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Landmark
                    Text("ორიენტირი / შენიშვნა მომხმარებლისთვის:", color = p.textSecondary, fontSize = 11.5.sp)
                    OutlinedTextField(
                        value = landmark,
                        onValueChange = { landmark = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("service_center_landmark_input"),
                        placeholder = { Text("ცენტრალურ მოედანთან, შესასვლელი ქუჩიდან", color = p.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = p.brandOrange,
                            unfocusedBorderColor = p.borderSubtle,
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            focusedContainerColor = p.subSurfaceBg,
                            unfocusedContainerColor = p.subSurfaceBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        // 4. Services Offered in Service Center
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = p.cardBg,
                border = BorderStroke(1.dp, p.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "სერვისცენტრის მიმართულებები (დიალოგში ნაჩვენები სერვისები)",
                        color = p.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Add new service row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newServiceInput,
                            onValueChange = { newServiceInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("service_center_new_service_input"),
                            placeholder = { Text("ახალი სერვისის დასახელება", color = p.textMuted, fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = p.brandOrange,
                                unfocusedBorderColor = p.borderSubtle,
                                focusedTextColor = p.textPrimary,
                                unfocusedTextColor = p.textPrimary,
                                focusedContainerColor = p.subSurfaceBg,
                                unfocusedContainerColor = p.subSurfaceBg
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val s = newServiceInput.trim()
                                if (s.isNotBlank() && !servicesList.contains(s)) {
                                    servicesList.add(s)
                                    newServiceInput = ""
                                }
                            },
                            enabled = newServiceInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = p.brandOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("service_center_add_service_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        }
                    }

                    // Existing services list
                    servicesList.forEachIndexed { idx, srv ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = p.subSurfaceBg,
                            border = BorderStroke(1.dp, p.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("•", color = p.brandOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(srv, color = p.textPrimary, fontSize = 12.5.sp)
                                }
                                IconButton(
                                    onClick = { if (servicesList.size > 1) servicesList.removeAt(idx) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "წაშლა", tint = p.errorRed, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Interactive Live Preview Card (როგორ ჩანს მომხმარებლისთვის)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = p.cardBg,
                border = BorderStroke(1.dp, p.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = p.warningAmber, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Live Preview: როგორ ჩანს მომხმარებლისთვის Drawer-ში",
                            color = p.warningAmber,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Scaled preview card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = p.subSurfaceBg,
                        border = BorderStroke(1.dp, p.brandOrangeBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = centerName.ifBlank { "BEKASERVICE" },
                                    color = p.brandOrange,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = p.brandOrangeSoft
                                ) {
                                    Text(
                                        text = "სერვისცენტრი",
                                        color = p.brandOrangeHighlight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = p.brandOrange, modifier = Modifier.size(13.dp))
                                Text(
                                    text = physicalAddress.ifBlank { "კოსტავას N10, ახალციხე" },
                                    color = p.textPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = p.textSecondary, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "სამუშაო საათები: ${workingHours.ifBlank { "10:00 – 20:00 (ყოველდღე)" }}",
                                    color = p.textSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            // 2 Preview buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = p.brandOrange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("დარეკვა", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = p.cardBg,
                                    border = BorderStroke(1.dp, p.brandOrange),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = null, tint = p.brandOrange, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("რუკაზე ნახვა", color = p.brandOrange, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Status banner
        if (statusMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isError) p.errorSoft else p.successSoft,
                    border = BorderStroke(1.dp, if (isError) p.errorRed else p.successGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage!!,
                        color = if (isError) p.errorRed else p.successGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 6. Action Buttons: შენახვა & სინქრონიზაცია + ნაგულისხმევზე დაბრუნება
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val activeEmail = AuthAndOrderManager.resolveActiveUserEmail(context)
                        if (activeEmail.isNotBlank() && !AuthAndOrderManager.isSoleAdminEmail(activeEmail)) {
                            Toast.makeText(
                                context,
                                "❌ წვდომა უარყოფილია: მხოლოდ beqa1994@gmail.com არის ადმინისტრატორი!",
                                Toast.LENGTH_LONG
                            ).show()
                            statusMessage = "❌ წვდომა უარყოფილია: მხოლოდ beqa1994@gmail.com არის ადმინისტრატორი!"
                            isError = true
                            return@Button
                        }

                        val cleanUrl = googleMapsUrl.trim()
                        val cleanAddress = physicalAddress.trim().ifBlank { "კოსტავას N10, ახალციხე" }
                        val cleanName = centerName.trim().ifBlank { "BEKASERVICE" }
                        val cleanPhone = phone.trim().ifBlank { "+995 577 00 11 22" }
                        val cleanHours = workingHours.trim().ifBlank { "10:00 – 20:00 (ყოველდღე)" }
                        val cleanLandmark = landmark.trim()
                        val finalServices = if (servicesList.isNotEmpty()) servicesList.toList() else ServiceCenterLocationConfig.DEFAULT.services

                        val newConfig = ServiceCenterLocationConfig(
                            centerName = cleanName,
                            physicalAddress = cleanAddress,
                            googleMapsUrl = cleanUrl.ifBlank { "https://maps.app.goo.gl/w8G6k689mZqU8Z488" },
                            phone = cleanPhone,
                            workingHours = cleanHours,
                            landmark = cleanLandmark,
                            services = finalServices,
                            updatedAtMillis = System.currentTimeMillis(),
                            updatedBy = "beqa1994@gmail.com"
                        )

                        // 1. Save locally immediately
                        ServiceCenterLocationManager.saveLocalConfig(context, newConfig)
                        statusMessage = "✅ ლოკაცია წარმატებით შეინახა და დასინქრონდა!"
                        isError = false
                        Toast.makeText(context, "✅ სერვისცენტრის ლოკაცია წარმატებით შეინახა!", Toast.LENGTH_SHORT).show()

                        // 2. Sync to Firestore in background
                        isSaving = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val repo = FirestoreRepository(context)
                            val res = repo.saveServiceCenterLocation(newConfig)
                            isSaving = false
                            withContext(Dispatchers.Main) {
                                if (res.isSuccess) {
                                    Toast.makeText(context, "✅ Cloud სინქრონიზაცია წარმატებულია!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val err = res.exceptionOrNull()?.message ?: ""
                                    if (!err.contains("PERMISSION_DENIED", ignoreCase = true) && !err.contains("offline", ignoreCase = true)) {
                                        Toast.makeText(context, "⚠️ შენახულია ლოკალურად: $err", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = p.brandOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("service_center_save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("სინქრონიზაცია...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("შენახვა და Cloud სინქრონიზაცია", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Reset to default button
                OutlinedButton(
                    onClick = {
                        val d = ServiceCenterLocationConfig.DEFAULT
                        centerName = d.centerName
                        physicalAddress = d.physicalAddress
                        googleMapsUrl = d.googleMapsUrl
                        phone = d.phone
                        workingHours = d.workingHours
                        landmark = d.landmark
                        servicesList.clear()
                        servicesList.addAll(d.services)
                        Toast.makeText(context, "მნიშვნელობები დაბრუნდა ნაგულისხმევზე", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = p.textSecondary),
                    border = BorderStroke(1.dp, p.borderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("service_center_reset_defaults_button")
                ) {
                    Text("🔄 ნაგულისხმევ მონაცემებზე დაბრუნება", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
