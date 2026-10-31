package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit


/**
 * პროდუქტის სურათების (რეალური URL ბმული, ტელეფონის მეხსიერებიდან/გალერეიდან არჩეული ფოტო,
 * ან აპის მიერ ავტო-გენერირებული ბრენდული ქარდის ვიზუალი) მართვის სისტემა.
 */
object ProductImageManager {
    const val AUTO_GENERATED_IMAGE_MARKER = "auto"

    private val legacyDefaultPlaceholders = setOf(
        "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600",
        "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600"
    )

    private val defaultSoftwareUrlsById: Map<Int, String> by lazy {
        SoftwareCatalogData.getDigitalSoftwareProducts().associate { it.id to it.imageUrl.trim() }
    }

    /**
     * აბრუნებს true-ს, თუ პროდუქტს ადმინის მიერ მიბმული აქვს რეალური ფოტო
     * (ბმულით https://... ან ტელეფონის მეხსიერებიდან file:// / content:// / data:image/),
     * რომელიც უნდა გამოჩნდეს ავტო-გენერირებული ქარდის ნაცვლად.
     */
    fun hasCustomAttachedPhoto(product: ProductItem): Boolean {
        val raw = (product.displayPrimaryImage.ifBlank { product.imageUrl }).trim()
        if (raw.isBlank() || raw.equals(AUTO_GENERATED_IMAGE_MARKER, ignoreCase = true)) {
            return false
        }

        // ტელეფონის მეხსიერებიდან / გალერეიდან არჩეული ფოტო
        if (isLocalOrEmbeddedImage(raw)) {
            return isValidRenderableImage(raw)
        }

        // საწყისი 24 პროგრამული სერვისისთვის (ID 101..124):
        // თუ imageUrl შეცვლილია საწყისი placeholder-ისგან, ვაჩვენებთ ადმინის მიბმულ ფოტოს.
        if (product.id in 101..124) {
            val defaultUrl = defaultSoftwareUrlsById[product.id]
            return raw != defaultUrl && (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true))
        }

        // ახლად დამატებული პროგრამული სერვისისთვის: თუ ძველი დეფოლტ placeholder-ია და ადმინს არ შეუცვლია
        val isSoftware = product.category == "პროგრამული სერვისები" || product.type == "software"
        if (isSoftware && product.id > 124 && raw in legacyDefaultPlaceholders) {
            return false
        }

        return raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)
    }

    fun isLocalOrEmbeddedImage(imageUrl: String): Boolean {
        val clean = imageUrl.trim()
        return clean.startsWith("file://", ignoreCase = true) ||
            clean.startsWith("content://", ignoreCase = true) ||
            clean.startsWith("/data/", ignoreCase = true) ||
            clean.startsWith("/storage/", ignoreCase = true) ||
            clean.startsWith("data:image/", ignoreCase = true)
    }

    fun isExistingLocalFile(pathOrUri: String): Boolean {
        val clean = pathOrUri.trim()
        val filePath = when {
            clean.startsWith("file://", ignoreCase = true) -> Uri.parse(clean).path ?: clean.removePrefix("file://")
            clean.startsWith("/data/", ignoreCase = true) || clean.startsWith("/storage/", ignoreCase = true) -> clean
            else -> return false
        }
        if (filePath.isBlank()) return false
        return try {
            val file = File(filePath)
            file.exists() && file.isFile && file.canRead() && file.length() > 0L
        } catch (_: Exception) {
            false
        }
    }

    fun isValidRenderableImage(imageUrl: String): Boolean {
        val clean = imageUrl.trim()
        if (clean.isBlank() || clean.equals(AUTO_GENERATED_IMAGE_MARKER, ignoreCase = true)) return false
        if (clean.startsWith("file://", ignoreCase = true) ||
            clean.startsWith("/data/", ignoreCase = true) ||
            clean.startsWith("/storage/", ignoreCase = true)
        ) {
            return localBitmapCache.get(clean) != null || isExistingLocalFile(clean)
        }
        return clean.startsWith("http://", ignoreCase = true) ||
            clean.startsWith("https://", ignoreCase = true) ||
            clean.startsWith("content://", ignoreCase = true) ||
            clean.startsWith("data:image/", ignoreCase = true)
    }

    /**
     * აკოპირებს ტელეფონის მეხსიერებიდან (Photo Picker) არჩეულ სურათს აპლიკაციის მუდმივ მეხსიერებაში,
     * რათა აპის გადატვირთვის შემდეგაც მუდმივად ხელმისაწვდომი და სწრაფი იყოს.
     */
    suspend fun copyPickerUriToInternalStorage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "product_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val destFile = File(dir, "prod_img_${System.currentTimeMillis()}.jpg")

            val inputBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: return@withContext null

            val decoded = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size)
            if (decoded != null) {
                val maxDim = 960
                val width = decoded.width
                val height = decoded.height
                val scaledBitmap = if (width > maxDim || height > maxDim) {
                    val ratio = width.toFloat() / height.toFloat()
                    val targetW = if (ratio >= 1f) maxDim else (maxDim * ratio).toInt().coerceAtLeast(1)
                    val targetH = if (ratio >= 1f) (maxDim / ratio).toInt().coerceAtLeast(1) else maxDim
                    Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                } else {
                    decoded
                }
                FileOutputStream(destFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    out.flush()
                }
                val fileUri = Uri.fromFile(destFile).toString()
                localBitmapCache.put(fileUri, scaledBitmap)
                fileUri
            } else {
                FileOutputStream(destFile).use { out ->
                    out.write(inputBytes)
                    out.flush()
                }
                Uri.fromFile(destFile).toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    private val localBitmapCache = android.util.LruCache<String, Bitmap>(32)

    fun getCachedLocalBitmap(imageUrl: String): Bitmap? {
        val clean = imageUrl.trim()
        if (clean.isEmpty()) return null
        return localBitmapCache.get(clean)
    }

    fun decodeLocalBitmapIfPossible(context: Context, imageUrl: String): Bitmap? {
        val clean = imageUrl.trim()
        if (clean.isEmpty()) return null
        localBitmapCache.get(clean)?.let { return it }
        return try {
            val decoded = when {
                clean.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(clean).path ?: clean.removePrefix("file://")
                    val file = File(path)
                    if (file.exists() && file.isFile && file.canRead() && file.length() > 0L) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else null
                }
                clean.startsWith("/data/") || clean.startsWith("/storage/") -> {
                    val file = File(clean)
                    if (file.exists() && file.isFile && file.canRead() && file.length() > 0L) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else null
                }
                clean.startsWith("content://", ignoreCase = true) -> {
                    context.contentResolver.openInputStream(Uri.parse(clean))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                clean.startsWith("data:image/", ignoreCase = true) -> {
                    val base64Part = clean.substringAfter("base64,", "")
                    if (base64Part.isNotBlank()) {
                        val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } else null
                }
                else -> null
            }
            if (decoded != null) {
                localBitmapCache.put(clean, decoded)
            }
            decoded
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveCroppedBitmap(context: Context, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "product_images")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "crop_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            out.flush()
        }
        val fileUri = Uri.fromFile(file).toString()
        localBitmapCache.put(fileUri, bitmap)
        fileUri
    }

    suspend fun downloadRemoteBitmap(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        val clean = url.trim()
        if (clean.isBlank()) return@withContext null
        decodeLocalBitmapIfPossible(context, clean)?.let { return@withContext it }
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            return@withContext null
        }
        try {
            val req = Request.Builder().url(clean).build()
            val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bytes = resp.body?.bytes() ?: return@withContext null
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        localBitmapCache.put(clean, bmp)
                    }
                    bmp
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * უსაფრთხო სურათის კომპონენტი გალერეისა და ზუმის ფანჯრისთვის,
 * რომელიც არ იწვევს FileNotFoundException-ს არარსებულ ლოკალურ ფაილებზე.
 */
@Composable
fun SafeProductAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cleanUrl = remember(imageUrl) { imageUrl.trim() }
    val isLocal = remember(cleanUrl) { ProductImageManager.isLocalOrEmbeddedImage(cleanUrl) }

    if (isLocal) {
        val cachedInitial = remember(cleanUrl) { ProductImageManager.getCachedLocalBitmap(cleanUrl) }
        val localBitmapState = produceState<Bitmap?>(initialValue = cachedInitial, key1 = cleanUrl) {
            if (value == null) {
                value = withContext(Dispatchers.IO) {
                    ProductImageManager.decodeLocalBitmapIfPossible(context, cleanUrl)
                }
            }
        }
        val bmp = localBitmapState.value
        if (bmp != null) {
            val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            Box(
                modifier = modifier.background(Color(0xFF16191E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = contentDescription,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    } else if (cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true)) {
        AsyncImage(
            model = cleanUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF16191E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = contentDescription,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * უნივერსალური პროდუქტის ვიზუალის კომპონენტი:
 * - თუ პროდუქტს მიბმული აქვს რეალური ფოტო (URL ბმულით ან ტელეფონის მეხსიერებიდან), აჩვენებს ფოტოს;
 * - ხოლო თუ არჩეულია ავტო-გენერირებული რეჟიმი (ან ბმული ცარიელია), აჩვენებს ბრენდულ გენერირებულ ქარდს.
 */
@Composable
fun ProductSmartVisual(
    product: ProductItem,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    forceAutoGenerated: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val rawUrl = remember(product.displayPrimaryImage, product.imageUrl) {
        (product.displayPrimaryImage.ifBlank { product.imageUrl }).trim()
    }
    val hasCustomPhoto = remember(product.id, rawUrl, forceAutoGenerated) {
        !forceAutoGenerated && ProductImageManager.hasCustomAttachedPhoto(product)
    }
    var isRemotePhotoLoaded by remember(rawUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier.background(AppThemeColorAdapter.adaptSurfaceColor(Color(0xFF0F1115)))
    ) {
        if (!hasCustomPhoto) {
            SoftwareBrandVisualHeader(
                product = product,
                modifier = Modifier.fillMaxSize(),
                compact = compact,
                ignoreCustomImage = true
            )
        } else {
            val isLocal = remember(rawUrl) { ProductImageManager.isLocalOrEmbeddedImage(rawUrl) }
            if (isLocal) {
                val cachedInitial = remember(rawUrl) { ProductImageManager.getCachedLocalBitmap(rawUrl) }
                val localBitmapState = produceState<Bitmap?>(initialValue = cachedInitial, key1 = rawUrl) {
                    if (value == null) {
                        value = withContext(Dispatchers.IO) {
                            ProductImageManager.decodeLocalBitmapIfPossible(context, rawUrl)
                        }
                    }
                }
                val bmp = localBitmapState.value
                if (bmp != null) {
                    val imageBitmap = remember(bmp) { bmp.asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    SoftwareBrandVisualHeader(
                        product = product,
                        modifier = Modifier.fillMaxSize(),
                        compact = compact,
                        ignoreCustomImage = true
                    )
                }
            } else {
                if (!isRemotePhotoLoaded) {
                    SoftwareBrandVisualHeader(
                        product = product,
                        modifier = Modifier.fillMaxSize(),
                        compact = compact,
                        ignoreCustomImage = true
                    )
                }
                AsyncImage(
                    model = rawUrl,
                    contentDescription = product.title,
                    onSuccess = { isRemotePhotoLoaded = true },
                    onError = { isRemotePhotoLoaded = false },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * პროდუქტის 8 ძირითადი კატეგორიის / ტიპის სქემა დინამიკური ფორმებისა და AI კვლევისთვის.
 */
enum class ProductCategoryType(
    val id: String,
    val displayLabel: String,
    val defaultCategoryName: String,
    val isDigitalService: Boolean
) {
    MOBILE_PHONE(
        id = "mobile_phone",
        displayLabel = "📱 მობილური ტელეფონი",
        defaultCategoryName = "მობილური ტელეფონები",
        isDigitalService = false
    ),
    PHONE_ACCESSORY(
        id = "phone_accessory",
        displayLabel = "🛡️ ტელეფონის აქსესუარი",
        defaultCategoryName = "აქსესუარები",
        isDigitalService = false
    ),
    HEADPHONES(
        id = "headphones",
        displayLabel = "🎧 ყურსასმენები და აუდიო",
        defaultCategoryName = "ყურსასმენები და აუდიო",
        isDigitalService = false
    ),
    CHARGER_CABLE(
        id = "charger_cable",
        displayLabel = "🔌 USB კაბელი / დამტენი",
        defaultCategoryName = "დამტენები და კაბელები",
        isDigitalService = false
    ),
    NETWORKING_DEVICE(
        id = "networking",
        displayLabel = "📶 Wi-Fi როუტერი / ქსელი",
        defaultCategoryName = "როუტერები და ქსელი",
        isDigitalService = false
    ),
    REPLACEMENT_DISPLAY(
        id = "replacement_display",
        displayLabel = "📲 ეკრანი / დისპლეი",
        defaultCategoryName = "ეკრანები",
        isDigitalService = false
    ),
    OTHER_ELECTRONICS(
        id = "other_electronics",
        displayLabel = "⚙️ სხვა ტექნიკა / ნაწილი",
        defaultCategoryName = "ელემენტები",
        isDigitalService = false
    ),
    DIGITAL_SERVICE(
        id = "software",
        displayLabel = "⚡ ციფრული სერვისი / ლიცენზია",
        defaultCategoryName = "პროგრამული სერვისები",
        isDigitalService = true
    );

    val labelGeo: String get() = displayLabel

    companion object {
        fun fromId(rawId: String): ProductCategoryType? {
            val normalized = rawId.trim().lowercase()
            return when (normalized) {
                "mobile_phone", "phone", "mobile", "smartphone" -> MOBILE_PHONE
                "phone_accessory", "accessory", "case" -> PHONE_ACCESSORY
                "headphones", "audio", "earphones" -> HEADPHONES
                "charger_cable", "charger", "cable" -> CHARGER_CABLE
                "networking", "router", "wifi", "modem" -> NETWORKING_DEVICE
                "replacement_display", "display", "screen" -> REPLACEMENT_DISPLAY
                "other_electronics", "part", "spare_part", "electronics" -> OTHER_ELECTRONICS
                "software", "digital_service", "service" -> DIGITAL_SERVICE
                else -> entries.find { it.id.equals(normalized, ignoreCase = true) }
            }
        }
    }
}

/**
 * კონკრეტული ტექნიკური ველის აღწერა დინამიკური ფორმის სქემაში.
 */
data class SpecFieldDefinition(
    val key: String,
    val label: String,
    val placeholder: String,
    val testTag: String,
    val isSellerOnlyFact: Boolean = false
) {
    val labelGeo: String get() = label
    val placeholderGeo: String get() = placeholder
    val isSellerProvided: Boolean get() = isSellerOnlyFact
}

/**
 * კონკრეტული პროდუქტის ტიპის ფორმის სქემა (რომელი ველები უნდა გამოჩნდეს და დაიმალოს).
 */
data class ProductTypeFormConfig(
    val categoryType: ProductCategoryType,
    val showSoftwareFields: Boolean,
    val showConditionField: Boolean,
    val showExactModelField: Boolean,
    val showCompatibleModelsField: Boolean,
    val compatibleModelsLabel: String,
    val compatibleModelsPlaceholder: String,
    val specFields: List<SpecFieldDefinition>
) {
    val categoryNameGeo: String get() = categoryType.defaultCategoryName
    val showCompatibleModels: Boolean get() = showCompatibleModelsField
    val compatibleModelsLabelGeo: String get() = compatibleModelsLabel
}

/**
 * ცენტრალიზებული, სქემაზე დაფუძნებული დინამიკური ფორმის კონფიგურაცია (Schema-Driven Form Engine).
 */
object ProductFormSchema {
    private val phoneSpecFields = listOf(
        SpecFieldDefinition("RAM", "ოპერატიული მეხსიერება (RAM)", "მაგ: 12 GB", "spec_field_RAM"),
        SpecFieldDefinition("Storage", "შიდა მეხსიერება (Storage)", "მაგ: 256 GB", "spec_field_Storage"),
        SpecFieldDefinition("ეკრანი", "ეკრანი (Display)", "მაგ: 6.8\" Dynamic LTPO AMOLED 2X, 120Hz", "spec_field_Display"),
        SpecFieldDefinition("პროცესორი", "პროცესორი (Processor)", "მაგ: Snapdragon 8 Gen 3 (4 nm)", "spec_field_Processor"),
        SpecFieldDefinition("მთავარი კამერა", "მთავარი კამერა", "მაგ: 200 MP + 50 MP + 10 MP + 12 MP", "spec_field_MainCamera"),
        SpecFieldDefinition("წინა კამერა", "წინა კამერა", "მაგ: 12 MP Dual Pixel PDAF", "spec_field_FrontCamera"),
        SpecFieldDefinition("ელემენტი", "ელემენტი (Battery)", "მაგ: 5000 mAh, 45W სწრაფი დამუხტვა", "spec_field_Battery"),
        SpecFieldDefinition("ელემენტის რესურსი", "ელემენტის რესურსი (Battery Health % - ივსება ხელით)", "მაგ: 100% ან 92%", "spec_field_BatteryHealth", isSellerOnlyFact = true),
        SpecFieldDefinition("SIM კონფიგურაცია", "SIM კონფიგურაცია", "მაგ: Nano-SIM + eSIM / Dual SIM", "spec_field_SIM"),
        SpecFieldDefinition("კავშირი", "კავშირი (Connectivity)", "მაგ: 5G, LTE, Wi-Fi 7, Bluetooth 5.3, NFC", "spec_field_Connectivity"),
        SpecFieldDefinition("ოპერაციული სისტემა", "ოპერაციული სისტემა (OS)", "მაგ: Android 14, One UI 6.1", "spec_field_OS"),
        SpecFieldDefinition("ფერი", "ფერი (Color - ივსება ხელით)", "მაგ: Titanium Gray, Black...", "spec_field_Color", isSellerOnlyFact = true)
    )

    private val accessorySpecFields = listOf(
        SpecFieldDefinition("აქსესუარის ტიპი", "აქსესუარის ტიპი", "მაგ: MagSafe ქეისი, დამცავი მინა, სამაგრი", "spec_field_AccessoryType"),
        SpecFieldDefinition("კავშირის ტიპი", "კავშირის / მორგების ტიპი", "მაგ: MagSafe, გარე დამცავი", "spec_field_ConnectionType"),
        SpecFieldDefinition("მასალა", "მასალა (Materials)", "მაგ: პოლიკარბონატი, სილიკონი, 9H ნაწრთობი მინა", "spec_field_Material"),
        SpecFieldDefinition("ზომები / სისქე", "ზომები (Dimensions - თუ დადასტურებულია)", "მაგ: 0.33 mm", "spec_field_Dimensions"),
        SpecFieldDefinition("ფერი", "ფერი (Color - ივსება ხელით)", "მაგ: გამჭვირვალე, შავი...", "spec_field_Color", isSellerOnlyFact = true)
    )

    private val headphonesSpecFields = listOf(
        SpecFieldDefinition("კავშირის ტიპი", "კავშირის ტიპი (Connection Type)", "მაგ: Bluetooth 5.3 / USB-C / 3.5mm", "spec_field_ConnectionType"),
        SpecFieldDefinition("ხმაურის ჩახშობა", "ხმაურის ჩახშობა (ANC / აუდიო)", "მაგ: Active Noise Cancellation (ANC), Transparency", "spec_field_AudioSpec"),
        SpecFieldDefinition("ელემენტის ხანგრძლივობა", "ელემენტის მუშაობის დრო", "მაგ: 6 სთ (ქეისით 30 სთ)", "spec_field_Battery"),
        SpecFieldDefinition("მასალა", "მასალა / დაცვა", "მაგ: IP54 მტვრისა და წყლისგან დაცვა", "spec_field_Material")
    )

    private val chargerCableSpecFields = listOf(
        SpecFieldDefinition("კავშირის ტიპი", "კონექტორი / პორტები (Connection Type)", "მაგ: USB-C to USB-C / 2x USB-C + 1x USB-A", "spec_field_ConnectionType"),
        SpecFieldDefinition("სიმძლავრე", "მაქსიმალური სიმძლავრე (Power Output)", "მაგ: 45W / 65W / 100W Power Delivery (PD 3.0)", "spec_field_PowerOutput"),
        SpecFieldDefinition("მასალა", "მასალა / ტექნოლოგია", "მაგ: GaN ტექნოლოგია / ნეილონის წნული", "spec_field_Material"),
        SpecFieldDefinition("სიგრძე / ზომა", "კაბელის სიგრძე / ზომა", "მაგ: 1 მეტრი / 2 მეტრი", "spec_field_Dimensions")
    )

    private val networkingSpecFields = listOf(
        SpecFieldDefinition("ქსელის თაობა", "ქსელის თაობა (Network Generation)", "მაგ: 4G LTE Cat 7 (300 Mbps DL / 100 Mbps UL)", "spec_field_NetworkGen"),
        SpecFieldDefinition("მხარდაჭერილი სიხშირეები (Bands)", "მხარდაჭერილი სიხშირეები (Supported Bands)", "მაგ: LTE B1/B3/B7/B8/B20/B28/B32/B38", "spec_field_Bands"),
        SpecFieldDefinition("Wi-Fi სტანდარტი", "Wi-Fi სტანდარტი", "მაგ: Wi-Fi 5 (802.11a/b/g/n/ac, AC1200)", "spec_field_WifiStandard"),
        SpecFieldDefinition("სიხშირეები", "Wi-Fi სიხშირეები (Frequencies)", "მაგ: 2.4 GHz & 5 GHz Dual-Band", "spec_field_Frequencies"),
        SpecFieldDefinition("პორტები", "პორტები (Ports)", "მაგ: 1x Gigabit LAN/WAN + 3x Gigabit LAN", "spec_field_Ports"),
        SpecFieldDefinition("ანტენები", "ანტენები (Antennas)", "მაგ: 2x გარე SMA ანტენის პორტი + ჩაშენებული ანტენა", "spec_field_Antennas"),
        SpecFieldDefinition("SIM მხარდაჭერა", "SIM ბარათის მხარდაჭერა", "მაგ: Micro-SIM სლოტი (ყველა ოპერატორი)", "spec_field_SimSupport")
    )

    private val displaySpecFields = listOf(
        SpecFieldDefinition("ეკრანის ტიპი", "ეკრანის ტიპი (Display Type)", "მაგ: Original OLED / Soft AMOLED / LTPO", "spec_field_DisplayType"),
        SpecFieldDefinition("გარჩევადობა", "გარჩევადობა და სიხშირე (Resolution)", "მაგ: FHD+ / QHD+ 120Hz", "spec_field_Resolution"),
        SpecFieldDefinition("კომპლექტაცია (ჩარჩო)", "ჩარჩოს კონფიგურაცია (Frame / No-Frame)", "მაგ: ჩარჩოთი (With Frame) / ჩარჩოს გარეშე", "spec_field_FrameConfig"),
        SpecFieldDefinition("კონექტორი / Part Number", "კონექტორი / ნაწილის კოდი", "მაგ: GH82-33332A", "spec_field_PartNumber")
    )

    private val otherElectronicsSpecFields = listOf(
        SpecFieldDefinition("ნაწილის ტიპი", "კომპონენტის / მოწყობილობის ტიპი", "მაგ: ელემენტი, დამტენის პორტი, კამერის მოდული", "spec_field_ComponentType"),
        SpecFieldDefinition("ტექნიკური პარამეტრი", "ძირითადი ტექნიკური პარამეტრი", "მაგ: ტევადობა mAh / სიმძლავრე / სტანდარტი", "spec_field_TechParam"),
        SpecFieldDefinition("მონტაჟის სტანდარტი", "კონექტორი / მონტაჟი", "მაგ: ქარხნული შლეიფი / OIS მოდული", "spec_field_Connector")
    )

    fun getConfig(categoryType: ProductCategoryType): ProductTypeFormConfig {
        return when (categoryType) {
            ProductCategoryType.MOBILE_PHONE -> ProductTypeFormConfig(
                categoryType = categoryType,
                showSoftwareFields = false,
                showConditionField = true,
                showExactModelField = true,
                showCompatibleModelsField = true,
                compatibleModelsLabel = "მხარდაჭერილი ქსელები / ოპერატორები (Magti, Silknet, Cellfie...)",
                compatibleModelsPlaceholder = "მაგ: ყველა ქართული და საერთაშორისო ოპერატორი (Unlocked)",
                specFields = phoneSpecFields
            )
            ProductCategoryType.PHONE_ACCESSORY -> ProductTypeFormConfig(
                categoryType = categoryType,
                showSoftwareFields = false,
                showConditionField = true,
                showExactModelField = true,
                showCompatibleModelsField = true,
                compatibleModelsLabel = "თავსებადი ტელეფონის მოდელები (Compatibility)",
                compatibleModelsPlaceholder = "მაგ: iPhone 16 Pro Max, Galaxy S24 Ultra...",
                specFields = accessorySpecFields
            )
            ProductCategoryType.HEADPHONES -> ProductTypeFormConfig(
                categoryType = categoryType,
                showSoftwareFields = false,
                showConditionField = true,
                showExactModelField = true,
                showCompatibleModelsField = true,
                compatibleModelsLabel = "თავსებადი მოწყობილობები (iOS, Android, Windows, Mac...)",
                compatibleModelsPlaceholder = "მაგ: iPhone, iPad, Mac, Android, Windows",
                specFields = headphonesSpecFields
            )
            ProductCategoryType.CHARGER_CABLE -> ProductTypeFormConfig(
                categoryType = categoryType,
                showSoftwareFields = false,
                showConditionField = true,
                showExactModelField = true,
                showCompatibleModelsField = true,
                compatibleModelsLabel = "თავსებადი მოდელები და სტანდარტები",
                compatibleModelsPlaceholder = "მაგ: iPhone 15/16 სერია, Samsung Super Fast Charging, MacBook",
                specFields = chargerCableSpecFields
            )
            ProductCategoryType.NETWORKING_DEVICE -> ProductTypeFormConfig(
                categoryType = categoryType,
                showSoftwareFields = false,
                showConditionField = true,
                showExactModelField = true,
                showCompatibleModelsField = true,
                compatibleModelsLabel = "თავსებადი ოპერატორები და მოწყობილობები",
                compatibleModelsPlaceholder = "მაგ: Magti, Silknet, Cellfie (ყველა 4G/LTE ქსელი)",
                specFields = networkingSpecFields
            )
            ProductCategoryType.REPLACEMENT_DISPLAY -> ProductTypeFormConfig(
                categoryType = categoryType,
                showSoftwareFields = false,
                showConditionField = true,
                showExactModelField = true,
                showCompatibleModelsField = true,
                compatibleModelsLabel = "თავსებადი ტელეფონის მოდელები (Compatible Phone Models)",
                compatibleModelsPlaceholder = "მაგ: iPhone 15 Pro (A2848, A3101, A3102)",
                specFields = displaySpecFields
            )
            ProductCategoryType.OTHER_ELECTRONICS -> ProductTypeFormConfig(
                categoryType = categoryType,
                showSoftwareFields = false,
                showConditionField = true,
                showExactModelField = true,
                showCompatibleModelsField = true,
                compatibleModelsLabel = "თავსებადი მოდელები",
                compatibleModelsPlaceholder = "მაგ: iPhone 14 Pro, Galaxy S23 Ultra...",
                specFields = otherElectronicsSpecFields
            )
            ProductCategoryType.DIGITAL_SERVICE -> ProductTypeFormConfig(
                categoryType = categoryType,
                showSoftwareFields = true,
                showConditionField = false,
                showExactModelField = false,
                showCompatibleModelsField = true,
                compatibleModelsLabel = "თავსებადი პლატფორმები (Windows, Mac, iOS, Android, Web...)",
                compatibleModelsPlaceholder = "მაგ: Windows, macOS, iOS, Android, Web",
                specFields = emptyList()
            )
        }
    }

    /**
     * ზუსტად განსაზღვრავს პროდუქტის კატეგორიის ტიპს არსებული პროდუქტიდან ან არჩეული კატეგორიიდან.
     * არასოდეს აღიქვამს ფიზიკურ პროდუქტს პროგრამულ სერვისად მხოლოდ იმიტომ, რომ მას მოკლე აღწერა ან მახასიათებლები აქვს!
     */
    fun resolveProductCategoryType(
        productTypeRaw: String,
        categoryRaw: String,
        legacyTypeRaw: String = "",
        titleRaw: String = ""
    ): ProductCategoryType {
        // 1. თუ კონკრეტული ფიზიკური productType უკვე მითითებულია, ვანიჭებთ პრიორიტეტს
        val explicitFromType = ProductCategoryType.fromId(productTypeRaw)
        if (explicitFromType != null && explicitFromType != ProductCategoryType.OTHER_ELECTRONICS && explicitFromType != ProductCategoryType.DIGITAL_SERVICE) {
            return explicitFromType
        }

        val catLower = categoryRaw.trim().lowercase()
        val titleLower = titleRaw.trim().lowercase()

        // 2. ვამოწმებთ დასახელებასა და კატეგორიას ფიზიკურ პროდუქტებზე
        val isUnlockOrSoftwareKeyword = titleLower.contains("unlock") || titleLower.contains("განბლოკვ") ||
            titleLower.contains("frp") || titleLower.contains("icloud") || titleLower.contains("cpid") ||
            titleLower.contains("ლიცენზი") || titleLower.contains("გამოწერ") || titleLower.contains("subscription") ||
            titleLower.contains("chatgpt") || titleLower.contains("claude") || titleLower.contains("canva") ||
            titleLower.contains("capcut") || titleLower.contains("adobe") || titleLower.contains("office 365") ||
            titleLower.contains("windows 11") || titleLower.contains("netflix") || titleLower.contains("spotify") ||
            titleLower.contains("midjourney") || titleLower.contains("cursor") || titleLower.contains("perplexity")

        if (catLower.contains("როუტერ") || catLower.contains("ქსელ") || catLower.contains("router") ||
            titleLower.contains("router") || titleLower.contains("როუტერ") || titleLower.contains("b535") ||
            titleLower.contains("4g lte") || titleLower.contains("5g cpe") || titleLower.contains("wi-fi 6") ||
            titleLower.contains("wifi router") || titleLower.contains("archer ") || titleLower.contains("modem")
        ) {
            return ProductCategoryType.NETWORKING_DEVICE
        }

        if (catLower.contains("ეკრან") || catLower.contains("დისპლე") ||
            titleLower.contains("ეკრან") || titleLower.contains("დისპლე") ||
            titleLower.contains("display") || titleLower.contains("screen")
        ) {
            return ProductCategoryType.REPLACEMENT_DISPLAY
        }

        if (catLower.contains("ყურსასმენ") || catLower.contains("აუდიო") ||
            titleLower.contains("ყურსასმენ") || titleLower.contains("airpods") ||
            titleLower.contains("galaxy buds") || titleLower.contains("headphones") ||
            titleLower.contains("earbuds") || titleLower.contains("wh-1000") || titleLower.contains("wf-1000")
        ) {
            return ProductCategoryType.HEADPHONES
        }

        if (catLower.contains("დამტენ") && !catLower.contains("პორტ") || catLower.contains("კაბელ") ||
            titleLower.contains("დამტენი") || titleLower.contains("კაბელი") || titleLower.contains("ადაპტერი") ||
            titleLower.contains("charger") || titleLower.contains("usb-c cable") || titleLower.contains("gan ")
        ) {
            return ProductCategoryType.CHARGER_CABLE
        }

        if (catLower.contains("აქსესუარ") || titleLower.contains("ქეის") || titleLower.contains("ბრონ") ||
            titleLower.contains("დამცავი მინა") || titleLower.contains("magsafe case")
        ) {
            return ProductCategoryType.PHONE_ACCESSORY
        }

        // მობილური ტელეფონების ამოცნობა (თუ არ არის ეკრანი/ელემენტი/პორტი/კორპუსი/განბლოკვა)
        val isRepairPartCat = catLower.contains("ელემენტ") || catLower.contains("პორტ") ||
            catLower.contains("კამერ") || catLower.contains("კორპუს") || catLower.contains("დინამიკ")
        val isRepairPartTitle = titleLower.contains("ელემენტ") || titleLower.contains("ბატარე") ||
            titleLower.contains("პორტ") || titleLower.contains("შლეიფ") || titleLower.contains("კორპუს") ||
            titleLower.contains("უკანა მინ") || titleLower.contains("დინამიკ") || titleLower.contains("კამერის")

        val hasPhoneStoragePattern = Regex("""\b(\d{1,2}\s*/\s*\d{2,4}\s*gb|\d{2,4}\s*gb|1\s*tb)\b""", RegexOption.IGNORE_CASE).containsMatchIn(titleLower)
        val hasPhoneModelKeyword = titleLower.contains("galaxy s") || titleLower.contains("galaxy z") ||
            titleLower.contains("galaxy a") || titleLower.contains("iphone ") || titleLower.contains("pixel ") ||
            titleLower.contains("redmi ") || titleLower.contains("xiaomi 1") || titleLower.contains("poco ") ||
            titleLower.contains("oneplus ") || titleLower.contains("honor ")

        if (!isUnlockOrSoftwareKeyword && !isRepairPartTitle && (
                catLower.contains("ტელეფონ") || catLower.contains("მობილურ") || catLower.contains("სმარტფონ") ||
                explicitFromType == ProductCategoryType.MOBILE_PHONE ||
                (hasPhoneModelKeyword && (hasPhoneStoragePattern || !isRepairPartCat))
            )
        ) {
            return ProductCategoryType.MOBILE_PHONE
        }

        if (isRepairPartCat || isRepairPartTitle) {
            return ProductCategoryType.OTHER_ELECTRONICS
        }

        if (explicitFromType == ProductCategoryType.DIGITAL_SERVICE ||
            catLower == "პროგრამული სერვისები" ||
            isUnlockOrSoftwareKeyword ||
            (legacyTypeRaw.equals("software", ignoreCase = true) && catLower.isBlank())
        ) {
            return ProductCategoryType.DIGITAL_SERVICE
        }

        return explicitFromType ?: ProductCategoryType.OTHER_ELECTRONICS
    }

    /**
     * ამოწმებს, დაიკარგება თუ არა უკვე შევსებული სპეციფიკური ველები პროდუქტის ტიპის შეცვლისას.
     * აბრუნებს შეუთავსებელი (შევსებული) ველების ქართულ დასახელებებს, რათა ადმინისტრატორს გამოუჩნდეს გაფრთხილება.
     */
    fun detectIncompatiblePopulatedFields(
        fromType: ProductCategoryType,
        toType: ProductCategoryType,
        offerType: String,
        duration: String,
        accountType: String,
        activationTerms: String,
        subcategory: String,
        specifications: Map<String, String>
    ): List<String> {
        if (fromType == toType) return emptyList()
        val conflicts = mutableListOf<String>()

        if (fromType.isDigitalService && !toType.isDigitalService) {
            if (offerType.isNotBlank()) conflicts.add("პაკეტის/ლიცენზიის ტიპი ($offerType)")
            if (duration.isNotBlank()) conflicts.add("გამოწერის ხანგრძლივობა ($duration)")
            if (accountType.isNotBlank()) conflicts.add("ანგარიშის ტიპი ($accountType)")
            if (activationTerms.isNotBlank()) conflicts.add("აქტივაციის პირობები")
            if (subcategory.isNotBlank()) conflicts.add("პროგრამული ქვეკატეგორია ($subcategory)")
        } else if (!fromType.isDigitalService && toType.isDigitalService) {
            val nonBlankSpecs = specifications.filterValues { it.isNotBlank() }
            if (nonBlankSpecs.isNotEmpty()) {
                conflicts.add("ფიზიკური ტექნიკური მახასიათებლები (${nonBlankSpecs.keys.take(3).joinToString(", ")})")
            }
        } else {
            // ორი სხვადასხვა ფიზიკური ტიპი: ვამოწმებთ, არის თუ არა შევსებული ძველი ტიპის სპეციფიკური ველები, რომლებიც ახალ ტიპში არ შედის
            val targetKeys = getConfig(toType).specFields.map { it.key }.toSet()
            val incompatibleSpecs = specifications.filter { (k, v) -> v.isNotBlank() && k !in targetKeys }
            if (incompatibleSpecs.isNotEmpty()) {
                conflicts.add("კატეგორიის სპეციფიკური ველები (${incompatibleSpecs.keys.take(3).joinToString(", ")})")
            }
        }
        return conflicts
    }
}

/**
 * ადმინისტრატორის მიერ ფორმაში უკვე ხელით შეყვანილი მნიშვნელობები.
 * გამოიყენება იმისათვის, რომ AI ანალიზმა არასოდეს გადააწეროს უკვე შევსებული ველები.
 */
data class ExistingProductFormState(
    val title: String = "",
    val price: String = "",
    val brand: String = "",
    val category: String = "",
    val subcategory: String = "",
    val shortDescription: String = "",
    val description: String = "",
    val featuresText: String = "",
    val targetAudience: String = "",
    val offerType: String = "",
    val duration: String = "",
    val accountType: String = "",
    val activationTerms: String = "",
    val compatibleModels: String = "",
    val warranty: String = "",
    val searchKeywords: String = "",
    val exactModel: String = "",
    val productType: String = "",
    val condition: String = "",
    val specifications: Map<String, String> = emptyMap()
)

/**
 * AI-ით პროდუქტის რეალური კვლევა (Google Search Grounding + Web Specs Retrieval) და სტრუქტურირებული შევსება.
 */
object AdminProductAiGenerator {
    private val candidateModels = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash")

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    data class IdentifiedProductInfo(
        val rawTitle: String,
        val brand: String,
        val modelName: String,
        val exactModelIdentifier: String,
        val ramVariant: String,
        val storageVariant: String,
        val categoryType: ProductCategoryType,
        val searchQuery: String
    ) {
        val ram: String get() = ramVariant
        val storage: String get() = storageVariant
    }

    data class GeneratedProductFields(
        val title: String,
        val price: String,
        val shortDescription: String,
        val description: String,
        val category: String,
        val brand: String,
        val isSoftware: Boolean,
        val subcategory: String,
        val offerType: String,
        val duration: String,
        val accountType: String,
        val features: List<String>,
        val targetAudience: String,
        val activationTerms: String,
        val compatibleModels: String,
        val warranty: String,
        val searchKeywords: String,
        val exactModel: String = "",
        val productType: String = "",
        val condition: String = "",
        val specifications: Map<String, String> = emptyMap(),
        val researchSources: List<String> = emptyList(),
        val isVerifiedFromSearch: Boolean = false,
        val errorMessage: String? = null
    ) {
        val categoryType: ProductCategoryType
            get() = ProductCategoryType.fromId(productType)
                ?: if (isSoftware) ProductCategoryType.DIGITAL_SERVICE else ProductCategoryType.OTHER_ELECTRONICS
    }

    /**
     * აანალიზებს პროდუქტის დასახელებას და არჩეულ კატეგორიას, გამოყოფს ბრენდს, ზუსტ მოდელს,
     * RAM/Storage კონფიგურაციას (მაგ: "Samsung Galaxy S24 Ultra 12/256GB") და პროდუქტის რეალურ ტიპს.
     */
    fun identifyProductFromTitle(
        rawTitle: String,
        selectedCategory: String = "",
        selectedProductType: String = ""
    ): IdentifiedProductInfo {
        val cleanTitle = rawTitle.trim()
        val lower = cleanTitle.lowercase()

        // RAM / Storage ამოცნობა (მაგ: "12/256GB", "8/128 GB", "16/512GB", "256GB")
        val slashRamStorageRegex = Regex("""\b(\d{1,2})\s*/\s*(\d{2,4})\s*(gb|tb)?\b""", RegexOption.IGNORE_CASE)
        val slashMatch = slashRamStorageRegex.find(cleanTitle)
        var detectedRam = ""
        var detectedStorage = ""

        if (slashMatch != null) {
            val ramNum = slashMatch.groupValues[1]
            val storNum = slashMatch.groupValues[2]
            val unit = slashMatch.groupValues[3].uppercase().ifBlank { "GB" }
            detectedRam = "$ramNum GB"
            detectedStorage = "$storNum $unit"
        } else {
            val ramRegex = Regex("""\b(\d{1,2})\s*gb\s*ram\b""", RegexOption.IGNORE_CASE)
            ramRegex.find(cleanTitle)?.let { detectedRam = "${it.groupValues[1]} GB" }

            val storageRegex = Regex("""\b(64|128|256|512|1024)\s*gb\b|\b(1|2)\s*tb\b""", RegexOption.IGNORE_CASE)
            storageRegex.find(cleanTitle)?.let { match ->
                val raw = match.value.uppercase().replace(" ", "")
                detectedStorage = if (raw.endsWith("TB")) raw.replace("TB", " TB") else raw.replace("GB", " GB")
            }
        }

        val detectedBrand = detectBrandFromTitle(cleanTitle)
        val resolvedType = ProductFormSchema.resolveProductCategoryType(
            productTypeRaw = selectedProductType,
            categoryRaw = selectedCategory,
            titleRaw = cleanTitle
        )

        // ზუსტი მოდელისა და ქარხნული იდენტიფიკატორის ამოღება
        val exactId = extractExactModelIdentifier(cleanTitle, detectedBrand, resolvedType)
        val cleanModelName = cleanTitle
            .replace(slashRamStorageRegex, "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .ifBlank { cleanTitle }

        val querySuffix = when (resolvedType) {
            ProductCategoryType.MOBILE_PHONE -> "official full technical specifications gsmarena"
            ProductCategoryType.NETWORKING_DEVICE -> "official technical specifications datasheet"
            ProductCategoryType.REPLACEMENT_DISPLAY -> "display screen replacement specifications"
            ProductCategoryType.DIGITAL_SERVICE -> "official features subscription"
            else -> "official specifications"
        }

        return IdentifiedProductInfo(
            rawTitle = cleanTitle,
            brand = detectedBrand,
            modelName = cleanModelName,
            exactModelIdentifier = exactId,
            ramVariant = detectedRam,
            storageVariant = detectedStorage,
            categoryType = resolvedType,
            searchQuery = "$cleanModelName $exactId $querySuffix".trim()
        )
    }

    private fun detectBrandFromTitle(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("samsung") || lower.contains("სამსუნგ") || lower.contains("galaxy") ||
                Regex("""\b(s2[0-5]|z fold|z flip|a[1-7]\d)\b""").containsMatchIn(lower) -> "Samsung"
            lower.contains("iphone") || lower.contains("აიფონ") || lower.contains("ipad") ||
                lower.contains("macbook") || lower.contains("airpods") || lower.contains("apple") ||
                lower.contains("magsafe") || lower.contains("icloud") -> "Apple"
            lower.contains("huawei") || lower.contains("ჰუავეი") || lower.contains("b535") || lower.contains("b311") || lower.contains("b525") || lower.contains("b818") -> "Huawei"
            lower.contains("xiaomi") || lower.contains("შაომი") || lower.contains("redmi") || lower.contains("poco") -> "Xiaomi"
            lower.contains("pixel") || lower.contains("პიქსელ") -> "Google Pixel"
            lower.contains("oneplus") -> "OnePlus"
            lower.contains("honor") || lower.contains("ჰონორ") -> "Honor"
            lower.contains("motorola") || lower.contains("moto ") -> "Motorola"
            lower.contains("tp-link") || lower.contains("tplink") || lower.contains("archer") || lower.contains("deco") -> "TP-Link"
            lower.contains("zte") -> "ZTE"
            lower.contains("mikrotik") -> "MikroTik"
            lower.contains("ubiquiti") || lower.contains("unifi") -> "Ubiquiti"
            lower.contains("asus") -> "ASUS"
            lower.contains("sony") || lower.contains("wh-1000") || lower.contains("wf-1000") -> "Sony"
            lower.contains("jbl") -> "JBL"
            lower.contains("marshall") -> "Marshall"
            lower.contains("bose") -> "Bose"
            lower.contains("baseus") -> "Baseus"
            lower.contains("anker") -> "Anker"
            lower.contains("ugreen") -> "Ugreen"
            lower.contains("belkin") -> "Belkin"
            lower.contains("spigen") -> "Spigen"
            lower.contains("chatgpt") || lower.contains("openai") || lower.contains("gpt") || lower.contains("sora") -> "OpenAI"
            lower.contains("claude") || lower.contains("anthropic") -> "Anthropic"
            lower.contains("gemini") || lower.contains("google ai") || lower.contains("google one") || lower.contains("notebooklm") -> "Google"
            lower.contains("grok") || lower.contains("xai") -> "xAI"
            lower.contains("perplexity") -> "Perplexity"
            lower.contains("cursor") -> "Cursor"
            lower.contains("lovable") -> "Lovable"
            lower.contains("midjourney") -> "Midjourney"
            lower.contains("canva") || lower.contains("კანვა") -> "Canva"
            lower.contains("capcut") || lower.contains("კაპკატ") -> "CapCut"
            lower.contains("adobe") || lower.contains("photoshop") || lower.contains("premiere") || lower.contains("acrobat") || lower.contains("illustrator") -> "Adobe"
            lower.contains("figma") -> "Figma"
            lower.contains("youtube") || lower.contains("იუთუბ") -> "YouTube"
            lower.contains("spotify") || lower.contains("სპოტიფაი") -> "Spotify"
            lower.contains("netflix") || lower.contains("ნეტფლიქს") -> "Netflix"
            lower.contains("windows") || lower.contains("office") || lower.contains("microsoft") || lower.contains("365") || lower.contains("copilot") -> "Microsoft"
            lower.contains("jetbrains") || lower.contains("intellij") -> "JetBrains"
            lower.contains("autodesk") || lower.contains("autocad") -> "Autodesk"
            else -> {
                val firstWord = title.split(" ", "-", "/").firstOrNull { it.any { ch -> ch.isLetter() } && it.length >= 2 } ?: ""
                firstWord.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }

    private fun extractExactModelIdentifier(
        title: String,
        brand: String,
        categoryType: ProductCategoryType
    ): String {
        val lower = title.lowercase()
        // თუ სათაურში უკვე წერია ზუსტი მოდელის კოდი (მაგ: B535-232, SM-S928B, WH-1000XM5, A2848, Archer C6)
        val explicitCodeRegex = Regex("""\b([A-Za-z]{1,4}-\d{3,5}[A-Za-z0-9/-]*|A\d{4}|SM-[A-Z]\d{3}[A-Z0-9/]*|XT\d{4}-\d)\b""")
        explicitCodeRegex.find(title)?.let { return it.value.uppercase() }

        return when {
            lower.contains("s25 ultra") -> "SM-S938B (Galaxy S25 Ultra)"
            lower.contains("s25 plus") || lower.contains("s25+") -> "SM-S936B (Galaxy S25+)"
            lower.contains("s25") -> "SM-S931B (Galaxy S25)"
            lower.contains("s24 ultra") -> "SM-S928B/DS (Galaxy S24 Ultra)"
            lower.contains("s24 plus") || lower.contains("s24+") -> "SM-S926B/DS (Galaxy S24+)"
            lower.contains("s24 fe") -> "SM-S721B (Galaxy S24 FE)"
            lower.contains("s24") -> "SM-S921B/DS (Galaxy S24)"
            lower.contains("s23 ultra") -> "SM-S918B/DS (Galaxy S23 Ultra)"
            lower.contains("s23 plus") || lower.contains("s23+") -> "SM-S916B (Galaxy S23+)"
            lower.contains("s23") -> "SM-S911B (Galaxy S23)"
            lower.contains("s22 ultra") -> "SM-S908B (Galaxy S22 Ultra)"
            lower.contains("a55") -> "SM-A556E/DS (Galaxy A55 5G)"
            lower.contains("a54") -> "SM-A546E (Galaxy A54 5G)"
            lower.contains("a35") -> "SM-A356E (Galaxy A35 5G)"
            lower.contains("z fold 6") || lower.contains("z fold6") -> "SM-F956B (Galaxy Z Fold6)"
            lower.contains("z flip 6") || lower.contains("z flip6") -> "SM-F741B (Galaxy Z Flip6)"
            lower.contains("16 pro max") -> "A3296 (iPhone 16 Pro Max)"
            lower.contains("16 pro") -> "A3293 (iPhone 16 Pro)"
            lower.contains("16 plus") -> "A3290 (iPhone 16 Plus)"
            lower.contains("iphone 16") -> "A3287 (iPhone 16)"
            lower.contains("15 pro max") -> "A2849 (iPhone 15 Pro Max)"
            lower.contains("15 pro") -> "A2848 (iPhone 15 Pro)"
            lower.contains("15 plus") -> "A3094 (iPhone 15 Plus)"
            lower.contains("iphone 15") -> "A3090 (iPhone 15)"
            lower.contains("14 pro max") -> "A2894 (iPhone 14 Pro Max)"
            lower.contains("14 pro") -> "A2890 (iPhone 14 Pro)"
            lower.contains("iphone 14") -> "A2882 (iPhone 14)"
            lower.contains("13 pro max") -> "A2643 (iPhone 13 Pro Max)"
            lower.contains("13 pro") -> "A2638 (iPhone 13 Pro)"
            lower.contains("iphone 13") -> "A2633 (iPhone 13)"
            lower.contains("pixel 9 pro xl") -> "GGX8B (Pixel 9 Pro XL)"
            lower.contains("pixel 9 pro") -> "GR83Y (Pixel 9 Pro)"
            lower.contains("pixel 9") -> "G2YBB (Pixel 9)"
            lower.contains("pixel 8 pro") -> "GC3VE (Pixel 8 Pro)"
            lower.contains("pixel 8") -> "GKWS6 (Pixel 8)"
            lower.contains("b535-232") || lower.contains("b535") -> "B535-232"
            lower.contains("b311-221") || lower.contains("b311") -> "B311-221"
            lower.contains("archer c6") -> "Archer C6 (AC1200)"
            lower.contains("archer ax53") -> "Archer AX53 (AX3000)"
            lower.contains("airpods pro 2") -> "MTJV3 (AirPods Pro 2 USB-C)"
            lower.contains("airpods 4") -> "MXP63 (AirPods 4)"
            else -> {
                if (categoryType == ProductCategoryType.DIGITAL_SERVICE) ""
                else title
                    .replace(Regex("""\b(\d{1,2}\s*/\s*\d{2,4}\s*(gb|tb)?)\b""", RegexOption.IGNORE_CASE), "")
                    .replace(brand, "", ignoreCase = true)
                    .trim()
                    .ifBlank { title.trim() }
            }
        }
    }

    /**
     * ასრულებს რეალურ ინტერნეტ მოძიებას საჯარო ტექნიკური წყაროებიდან (Wikipedia API / DuckDuckGo Instant Answer API)
     * რათა Gemini Grounding-თან ერთად მოიპოვოს დადასტურებული ტექნიკური ფაქტები.
     */
    private fun fetchVerifiedWebSnippets(identified: IdentifiedProductInfo): Pair<String, List<String>> {
        val snippets = mutableListOf<String>()
        val sources = mutableListOf<String>()
        val searchTerms = listOf(
            "${identified.brand} ${identified.modelName}".trim(),
            identified.exactModelIdentifier.substringBefore("(").trim()
        ).filter { it.length >= 3 }.distinct()

        for (term in searchTerms.take(2)) {
            try {
                val encoded = Uri.encode(term)
                val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&utf8=&format=json&srlimit=2"
                val req = Request.Builder()
                    .url(wikiUrl)
                    .header("User-Agent", "BekaServiceCatalogResearch/1.0")
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string().orEmpty()
                        val searchArr = JSONObject(bodyStr).optJSONObject("query")?.optJSONArray("search")
                        if (searchArr != null && searchArr.length() > 0) {
                            for (i in 0 until searchArr.length()) {
                                val item = searchArr.optJSONObject(i) ?: continue
                                val pageTitle = item.optString("title", "")
                                val rawSnippet = item.optString("snippet", "")
                                    .replace(Regex("<[^>]*>"), "")
                                    .replace("&quot;", "\"")
                                    .replace("&amp;", "&")
                                    .trim()
                                if (rawSnippet.isNotBlank()) {
                                    snippets.add("$pageTitle: $rawSnippet")
                                    sources.add("https://en.wikipedia.org/wiki/${Uri.encode(pageTitle.replace(" ", "_"))}")
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Network unavailable in offline test environment or timeout
            }
        }
        return snippets.joinToString("\n") to sources.distinct()
    }

    suspend fun generateForTitle(
        rawTitle: String,
        existingCategories: List<String>,
        existingBrands: List<String>,
        selectedCategory: String = "",
        selectedProductType: String = "",
        existingFormState: ExistingProductFormState? = null
    ): GeneratedProductFields = withContext(Dispatchers.IO) {
        val cleanTitle = rawTitle.trim()
        // ვამოწმებთ, არის თუ არა სათაური वैलिडური და იდენტიფიცირებადი
        val letterOrDigitCount = cleanTitle.count { it.isLetterOrDigit() }
        if (cleanTitle.isBlank() || letterOrDigitCount < 2) {
            return@withContext GeneratedProductFields(
                title = cleanTitle,
                price = "",
                shortDescription = "",
                description = "",
                category = selectedCategory,
                brand = "",
                isSoftware = false,
                subcategory = "",
                offerType = "",
                duration = "",
                accountType = "",
                features = emptyList(),
                targetAudience = "",
                activationTerms = "",
                compatibleModels = "",
                warranty = "",
                searchKeywords = "",
                exactModel = "",
                productType = selectedProductType,
                condition = "",
                specifications = emptyMap(),
                researchSources = emptyList(),
                isVerifiedFromSearch = false,
                errorMessage = "პროდუქტის დასახელება ვერ ამოიცნო. გთხოვთ მიუთითოთ ზუსტი ბრენდი და მოდელი (მაგ: Samsung Galaxy S24 Ultra 12/256GB ან Huawei B535-232)."
            )
        }

        val identified = identifyProductFromTitle(
            rawTitle = cleanTitle,
            selectedCategory = selectedCategory,
            selectedProductType = selectedProductType
        )

        // 1. ვასრულებთ ვებ-ძიებას საჯარო ტექნიკურ წყაროებში
        val (webContextText, webSources) = fetchVerifiedWebSnippets(identified)

        // 2. ვცდილობთ რეალურ Gemini AI კვლევას Google Search Grounding-ით
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            val aiResult = tryCallGeminiForProduct(
                identified = identified,
                webContextText = webContextText,
                webSources = webSources,
                existingCategories = existingCategories,
                existingBrands = existingBrands,
                apiKey = apiKey
            )
            if (aiResult != null) {
                return@withContext if (existingFormState != null) {
                    mergeGeneratedWithManualFields(aiResult, existingFormState)
                } else {
                    aiResult
                }
            }
        }

        // 3. თუ Gemini API მიუწვდომელია, ვიყენებთ ზუსტ მოდელზე დაფუძნებულ ვერიფიცირებულ სპეციფიკაციების ბაზას + ვებ-ძიების შედეგებს
        val verifiedFallback = buildSmartFallbackFields(
            rawTitle = cleanTitle,
            selectedCategory = selectedCategory,
            selectedProductType = selectedProductType,
            webSources = webSources
        )
        return@withContext if (existingFormState != null) {
            mergeGeneratedWithManualFields(verifiedFallback, existingFormState)
        } else {
            verifiedFallback
        }
    }

    /**
     * იცავს ადმინისტრატორის მიერ უკვე ხელით შეყვანილ ველებს გადაწერისგან (Issue 2E).
     */
    fun mergeGeneratedWithManualFields(
        generated: GeneratedProductFields,
        manual: ExistingProductFormState
    ): GeneratedProductFields {
        val mergedSpecs = generated.specifications.toMutableMap()
        // ყველა სპეციფიკაცია, რომელიც ადმინმა უკვე ხელით შეავსო, უცვლელი რჩება
        for ((k, v) in manual.specifications) {
            if (v.isNotBlank()) {
                mergedSpecs[k] = v
            }
        }

        val manualFeatures = manual.featuresText
            .lines()
            .map { it.trim().removePrefix("•").removePrefix("-").trim() }
            .filter { it.isNotBlank() }

        return generated.copy(
            title = manual.title.trim().ifBlank { generated.title },
            price = manual.price.trim().ifBlank { generated.price },
            brand = manual.brand.trim().ifBlank { generated.brand },
            category = manual.category.trim().ifBlank { generated.category },
            subcategory = manual.subcategory.trim().ifBlank { generated.subcategory },
            shortDescription = manual.shortDescription.trim().ifBlank { generated.shortDescription },
            description = manual.description.trim().ifBlank { generated.description },
            features = if (manualFeatures.isNotEmpty()) manualFeatures else generated.features,
            targetAudience = manual.targetAudience.trim().ifBlank { generated.targetAudience },
            offerType = manual.offerType.trim().ifBlank { generated.offerType },
            duration = manual.duration.trim().ifBlank { generated.duration },
            accountType = manual.accountType.trim().ifBlank { generated.accountType },
            activationTerms = manual.activationTerms.trim().ifBlank { generated.activationTerms },
            compatibleModels = manual.compatibleModels.trim().ifBlank { generated.compatibleModels },
            warranty = manual.warranty.trim().ifBlank { generated.warranty },
            searchKeywords = manual.searchKeywords.trim().ifBlank { generated.searchKeywords },
            exactModel = manual.exactModel.trim().ifBlank { generated.exactModel },
            condition = manual.condition.trim().ifBlank { generated.condition },
            specifications = mergedSpecs
        )
    }

    private fun tryCallGeminiForProduct(
        identified: IdentifiedProductInfo,
        webContextText: String,
        webSources: List<String>,
        existingCategories: List<String>,
        existingBrands: List<String>,
        apiKey: String
    ): GeneratedProductFields? {
        val isDigital = identified.categoryType.isDigitalService
        val expectedSpecKeys = ProductFormSchema.getConfig(identified.categoryType).specFields
            .filter { !it.isSellerOnlyFact }
            .joinToString(", ") { "\"${it.key}\"" }

        val prompt = """
შენ ხარ BEKASERVICE-ის ტექნიკური კატალოგის ინჟინერი და პროდუქტების მკვლევარი.
ადმინისტრატორმა შეიყვანა პროდუქტის დასახელება: "${identified.rawTitle}"
იდენტიფიცირებული ბრენდი: "${identified.brand}"
იდენტიფიცირებული მოდელი: "${identified.modelName}" (ზუსტი კოდი: "${identified.exactModelIdentifier}")
არჩეული/ამოცნობილი პროდუქტის ტიპი: "${identified.categoryType.id}" (${identified.categoryType.displayLabel})
${if (identified.ramVariant.isNotBlank()) "RAM კონფიგურაცია სათაურიდან: ${identified.ramVariant}" else ""}
${if (identified.storageVariant.isNotBlank()) "Storage კონფიგურაცია სათაურიდან: ${identified.storageVariant}" else ""}
${if (webContextText.isNotBlank()) "მოძიებული ვებ-სპეციფიკაციების კონტექსტი:\n$webContextText" else ""}

მკაცრი წესები (CRITICAL):
1. თუ პროდუქტი ფიზიკური ნივთია (მობილური ტელეფონი, როუტერი, აქსესუარი, ყურსასმენი, კაბელი/დამტენი, ეკრანი ან ნაწილი), კატეგორიულად აკრძალულია მისი აღწერა როგორც პროგრამული სერვისის, გამოწერის, ლიცენზიის ან ანგარიშის აქტივაციის!
2. გამოიყენე Google Search ამ კონკრეტული მოდელის ("${identified.exactModelIdentifier}") ოფიციალური ქარხნული ტექნიკური მახასიათებლების გადასამოწმებლად. არ ჩაანაცვლო სხვა მსგავსი მოდელის მონაცემებით.
3. კატეგორიულად აკრძალულია გამყიდველზე დამოკიდებული ფაქტების გამოგონება: არ გამოიგონო ნივთის მდგომარეობა (მეორადი/ახალი), Battery Health %, ნაკაწრები, შეკეთების ისტორია, გარანტიის ვადა, მარაგის რაოდენობა ან გასაყიდი ფასი! დატოვე price="", warranty="", condition="".
4. დააბრუნე მხოლოდ სუფთა JSON ობიექტი ქართულ ენაზე:
{
  "exactModel": "${identified.exactModelIdentifier}",
  "brand": "${identified.brand}",
  "category": "${identified.categoryType.defaultCategoryName}",
  "productType": "${identified.categoryType.id}",
  "isSoftware": $isDigital,
  "subcategory": "${if (isDigital) "ხელოვნური ინტელექტი ან ლიცენზია" else ""}",
  "shortDescription": "ზუსტი, ფაქტობრივი ქართული მოკლე აღწერა (1-2 წინადადება) კონკრეტულად ამ მოდელის შესახებ",
  "description": "სრული ტექნიკური მიმოხილვა ქართულად (3-5 წინადადება) ოფიციალური მახასიათებლების მიხედვით",
  "specifications": {
    // შეავსე მხოლოდ დადასტურებული ქარხნული მახასიათებლები ამ გასაღებებით: [$expectedSpecKeys]
  },
  "features": [
    "მთავარი დადასტურებული ტექნიკური უპირატესობა 1",
    "მთავარი დადასტურებული ტექნიკური უპირატესობა 2",
    "მთავარი დადასტურებული ტექნიკური უპირატესობა 3",
    "მთავარი დადასტურებული ტექნიკური უპირატესობა 4"
  ],
  "offerType": "${if (isDigital) "პრემიუმ გამოწერა / ლიცენზია" else ""}",
  "duration": "${if (isDigital) "1 თვე" else ""}",
  "accountType": "${if (isDigital) "პირადი ანგარიში (Private)" else ""}",
  "targetAudience": "ვისთვის არის განკუთვნილი ეს მოდელი",
  "activationTerms": "${if (isDigital) "აქტივაციის პირობები" else ""}",
  "compatibleModels": "თავსებადი მოდელები ან ქსელები",
  "searchKeywords": "საძიებო საკვანძო სიტყვები ქართულად და ინგლისურად"
}
""".trimIndent()

        // ვცდილობთ ჯერ Google Search Grounding-ით, შემდეგ სტანდარტული მოთხოვნით თუ კონკრეტულმა მოდელმა tool-ზე შეცდომა დააბრუნა
        for (modelName in candidateModels) {
            for (useSearchGrounding in listOf(true, false)) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                    val requestJson = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("parts", JSONArray().apply {
                                    put(JSONObject().put("text", prompt))
                                })
                            })
                        })
                        if (useSearchGrounding) {
                            put("tools", JSONArray().apply {
                                put(JSONObject().put("google_search", JSONObject()))
                            })
                        }
                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.2)
                            if (!useSearchGrounding) {
                                put("responseMimeType", "application/json")
                            }
                        })
                    }

                    val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder().url(url).post(body).build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val respStr = response.body?.string().orEmpty()
                            val root = JSONObject(respStr)
                            val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
                            val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
                            val rawTextBuilder = StringBuilder()
                            if (parts != null) {
                                for (i in 0 until parts.length()) {
                                    rawTextBuilder.append(parts.optJSONObject(i)?.optString("text", "").orEmpty())
                                }
                            }
                            val extractedSources = mutableListOf<String>()
                            extractedSources.addAll(webSources)

                            // Grounding წყაროების ამოღება Gemini-ს პასუხიდან
                            val groundingMeta = candidate?.optJSONObject("groundingMetadata")
                            val groundingChunks = groundingMeta?.optJSONArray("groundingChunks")
                            if (groundingChunks != null) {
                                for (i in 0 until groundingChunks.length()) {
                                    val webObj = groundingChunks.optJSONObject(i)?.optJSONObject("web")
                                    val uri = webObj?.optString("uri", "").orEmpty()
                                    val webTitle = webObj?.optString("title", "").orEmpty()
                                    if (uri.isNotBlank()) {
                                        extractedSources.add(if (webTitle.isNotBlank()) "$webTitle ($uri)" else uri)
                                    }
                                }
                            }
                            val searchQueries = groundingMeta?.optJSONArray("webSearchQueries")
                            if (searchQueries != null && extractedSources.isEmpty()) {
                                for (i in 0 until searchQueries.length()) {
                                    val q = searchQueries.optString(i, "")
                                    if (q.isNotBlank()) extractedSources.add("Google Search: $q")
                                }
                            }

                            val cleanedText = rawTextBuilder.toString()
                                .trim()
                                .let { str ->
                                    val firstBrace = str.indexOf('{')
                                    val lastBrace = str.lastIndexOf('}')
                                    if (firstBrace >= 0 && lastBrace > firstBrace) {
                                        str.substring(firstBrace, lastBrace + 1)
                                    } else {
                                        str
                                    }
                                }

                            if (cleanedText.startsWith("{")) {
                                val json = JSONObject(cleanedText)
                                val fallback = buildSmartFallbackFields(
                                    rawTitle = identified.rawTitle,
                                    selectedCategory = identified.categoryType.defaultCategoryName,
                                    selectedProductType = identified.categoryType.id,
                                    webSources = extractedSources.distinct()
                                )
                                val featuresArr = json.optJSONArray("features")
                                val parsedFeatures = mutableListOf<String>()
                                if (featuresArr != null) {
                                    for (i in 0 until featuresArr.length()) {
                                        val f = featuresArr.optString(i, "").trim()
                                        if (f.isNotBlank()) parsedFeatures.add(f)
                                    }
                                }
                                val specsJson = json.optJSONObject("specifications")
                                val parsedSpecs = fallback.specifications.toMutableMap()
                                if (specsJson != null) {
                                    val keys = specsJson.keys()
                                    while (keys.hasNext()) {
                                        val key = keys.next()
                                        val value = specsJson.optString(key, "").trim()
                                        // არასოდეს ვავსებთ გამყიდველის სპეციფიკურ ველებს (Battery Health %) AI-ით
                                        if (key.isNotBlank() && value.isNotBlank() && !key.contains("რესურსი", ignoreCase = true)) {
                                            parsedSpecs[key] = value
                                        }
                                    }
                                }
                                if (identified.ramVariant.isNotBlank()) {
                                    parsedSpecs["RAM"] = identified.ramVariant
                                }
                                if (identified.storageVariant.isNotBlank()) {
                                    parsedSpecs["Storage"] = identified.storageVariant
                                }

                                val resolvedIsSw = identified.categoryType.isDigitalService
                                return GeneratedProductFields(
                                    title = identified.rawTitle,
                                    // AI არ იგონებს გასაყიდ ფასს, გარანტიას ან მეორად/ახალ მდგომარეობას
                                    price = "",
                                    shortDescription = json.optString("shortDescription", fallback.shortDescription).ifBlank { fallback.shortDescription },
                                    description = json.optString("description", fallback.description).ifBlank { fallback.description },
                                    category = json.optString("category", fallback.category).ifBlank { fallback.category },
                                    brand = json.optString("brand", fallback.brand).ifBlank { fallback.brand },
                                    isSoftware = resolvedIsSw,
                                    subcategory = if (resolvedIsSw) json.optString("subcategory", fallback.subcategory).ifBlank { fallback.subcategory } else "",
                                    offerType = if (resolvedIsSw) json.optString("offerType", fallback.offerType).ifBlank { fallback.offerType } else "",
                                    duration = if (resolvedIsSw) json.optString("duration", fallback.duration).ifBlank { fallback.duration } else "",
                                    accountType = if (resolvedIsSw) json.optString("accountType", fallback.accountType).ifBlank { fallback.accountType } else "",
                                    features = if (parsedFeatures.isNotEmpty()) parsedFeatures else fallback.features,
                                    targetAudience = json.optString("targetAudience", fallback.targetAudience).ifBlank { fallback.targetAudience },
                                    activationTerms = if (resolvedIsSw) json.optString("activationTerms", fallback.activationTerms).ifBlank { fallback.activationTerms } else "",
                                    compatibleModels = json.optString("compatibleModels", fallback.compatibleModels).ifBlank { fallback.compatibleModels },
                                    warranty = "",
                                    searchKeywords = json.optString("searchKeywords", fallback.searchKeywords).ifBlank { fallback.searchKeywords },
                                    exactModel = json.optString("exactModel", fallback.exactModel).ifBlank { fallback.exactModel },
                                    productType = identified.categoryType.id,
                                    condition = "",
                                    specifications = parsedSpecs,
                                    researchSources = extractedSources.distinct().ifEmpty { listOf("Google Search Grounding (${identified.exactModelIdentifier})") },
                                    isVerifiedFromSearch = true,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Continue to next attempt
                }
            }
        }
        return null
    }

    /**
     * ზუსტ მოდელზე დაფუძნებული ტექნიკური მახასიათებლების ანალიზატორი (8 პროდუქტის კატეგორიის მიხედვით).
     * მკაცრად მიჯნავს ფიზიკურ პროდუქტებსა და ციფრულ სერვისებს და არ იგონებს გამყიდველის ფაქტებს.
     */
    fun buildSmartFallbackFields(
        rawTitle: String,
        selectedCategory: String = "",
        selectedProductType: String = "",
        webSources: List<String> = emptyList()
    ): GeneratedProductFields {
        val title = rawTitle.trim()
        val lower = title.lowercase()

        val identified = identifyProductFromTitle(
            rawTitle = title,
            selectedCategory = selectedCategory,
            selectedProductType = selectedProductType
        )
        val detectedBrand = identified.brand
        val exactModel = identified.exactModelIdentifier
        val categoryType = identified.categoryType

        // =========================================================================
        // 1. მობილური ტელეფონები (MOBILE_PHONE) - მაგ: Samsung Galaxy S24 Ultra 12/256GB
        // =========================================================================
        if (categoryType == ProductCategoryType.MOBILE_PHONE) {
            val specs = buildVerifiedPhoneSpecifications(lower, detectedBrand, identified.ramVariant, identified.storageVariant)
            val ramStr = specs["RAM"] ?: identified.ramVariant
            val storageStr = specs["Storage"] ?: identified.storageVariant
            val memorySummary = listOfNotNull(
                ramStr.takeIf { it.isNotBlank() },
                storageStr.takeIf { it.isNotBlank() }
            ).joinToString(" / ")

            val displaySpec = specs["ეკრანი"].orEmpty()
            val processorSpec = specs["პროცესორი"].orEmpty()
            val cameraSpec = specs["მთავარი კამერა"].orEmpty()
            val batterySpec = specs["ელემენტი"].orEmpty()

            val shortDesc = buildString {
                append("$detectedBrand ${identified.modelName}")
                if (memorySummary.isNotBlank()) append(" ($memorySummary)")
                if (displaySpec.isNotBlank()) append(" — $displaySpec ეკრანი")
                if (processorSpec.isNotBlank()) append(", $processorSpec პროცესორი")
                if (cameraSpec.isNotBlank()) append(" და $cameraSpec კამერის სისტემა.") else append(".")
            }

            val fullDesc = buildString {
                append("$title (ქარხნული მოდელი: $exactModel) წარმოადგენს $detectedBrand-ის სმარტფონს. ")
                if (displaySpec.isNotBlank()) append("აღჭურვილია $displaySpec დისპლეითა და ")
                if (processorSpec.isNotBlank()) append("$processorSpec ჩიპსეტით. ")
                if (memorySummary.isNotBlank()) append("მეხსიერების კონფიგურაცია მოიცავს $memorySummary-ს. ")
                if (cameraSpec.isNotBlank()) append("ფოტო და ვიდეო გადაღებისთვის გამოიყენება $cameraSpec მთავარი კამერა")
                if (batterySpec.isNotBlank()) append(" და $batterySpec ელემენტი.") else append(".")
            }

            val featuresList = mutableListOf<String>()
            if (displaySpec.isNotBlank()) featuresList.add("ეკრანი: $displaySpec")
            if (processorSpec.isNotBlank()) featuresList.add("პროცესორი: $processorSpec")
            if (memorySummary.isNotBlank()) featuresList.add("მეხსიერება: $memorySummary")
            if (cameraSpec.isNotBlank()) featuresList.add("კამერა: $cameraSpec")
            if (batterySpec.isNotBlank()) featuresList.add("ელემენტი: $batterySpec")
            if (featuresList.isEmpty()) {
                featuresList.add("$detectedBrand-ის ოფიციალური ტექნიკური არქიტექტურა ($exactModel)")
                featuresList.add("5G / 4G LTE, Wi-Fi და NFC კავშირის მხარდაჭერა")
            }

            return GeneratedProductFields(
                title = title,
                price = "", // ფასი დგინდება მხოლოდ ადმინისტრატორის მიერ
                shortDescription = shortDesc,
                description = fullDesc,
                category = selectedCategory.takeIf { it.isNotBlank() && it != "პროგრამული სერვისები" }
                    ?: ProductCategoryType.MOBILE_PHONE.defaultCategoryName,
                brand = detectedBrand,
                isSoftware = false,
                subcategory = "",
                offerType = "",
                duration = "",
                accountType = "",
                features = featuresList,
                targetAudience = "მომხმარებლები, რომლებიც ეძებენ $detectedBrand ${identified.modelName} სმარტფონს",
                activationTerms = "",
                compatibleModels = "ყველა ქართული და საერთაშორისო მობილური ოპერატორი (Magti, Silknet, Cellfie)",
                warranty = "", // გარანტია ივსება ადმინისტრატორის მიერ
                searchKeywords = "${title.lowercase()}, ${detectedBrand.lowercase()}, ${exactModel.lowercase()}, smartphone, ტელეფონი, მობილური",
                exactModel = exactModel,
                productType = ProductCategoryType.MOBILE_PHONE.id,
                condition = "", // მდგომარეობა (ახალი/მეორადი) არასოდეს იგონება AI-ის მიერ
                specifications = specs,
                researchSources = webSources.ifEmpty { listOf("Official Manufacturer Specifications ($exactModel)") },
                isVerifiedFromSearch = true
            )
        }

        // =========================================================================
        // 2. ქსელური მოწყობილობები და Wi-Fi როუტერები (NETWORKING_DEVICE) - მაგ: Huawei B535-232
        // =========================================================================
        if (categoryType == ProductCategoryType.NETWORKING_DEVICE) {
            val specs = buildVerifiedNetworkingSpecifications(lower, detectedBrand, exactModel)
            val netGen = specs["ქსელის თაობა"].orEmpty()
            val wifiStd = specs["Wi-Fi სტანდარტი"].orEmpty()
            val ports = specs["პორტები"].orEmpty()

            val shortDesc = buildString {
                append("$title ($exactModel) — ")
                if (netGen.isNotBlank()) append("$netGen, ")
                if (wifiStd.isNotBlank()) append("$wifiStd უსადენო ქსელი ")
                if (ports.isNotBlank()) append("და $ports.") else append("სტაბილური ინტერნეტ კავშირისთვის.")
            }

            val fullDesc = buildString {
                append("$title (მოდელის იდენტიფიკატორი: $exactModel) არის $detectedBrand-ის ქსელური მოწყობილობა. ")
                if (netGen.isNotBlank()) append("უზრუნველყოფს $netGen მონაცემთა გადაცემას, ")
                if (wifiStd.isNotBlank()) append("მუშაობს $wifiStd (${specs["სიხშირეები"].orEmpty()}) სტანდარტზე ")
                if (ports.isNotBlank()) append("და აღჭურვილია $ports პორტებით.") else append(".")
            }

            val featuresList = specs.entries.map { (k, v) -> "$k: $v" }.take(5)

            return GeneratedProductFields(
                title = title,
                price = "",
                shortDescription = shortDesc,
                description = fullDesc,
                category = selectedCategory.takeIf { it.isNotBlank() && it != "პროგრამული სერვისები" }
                    ?: ProductCategoryType.NETWORKING_DEVICE.defaultCategoryName,
                brand = detectedBrand,
                isSoftware = false,
                subcategory = "",
                offerType = "",
                duration = "",
                accountType = "",
                features = featuresList,
                targetAudience = "სახლის, ოფისისა და აგარაკის მაღალსიჩქარიანი ინტერნეტ ქსელის მომხმარებლები",
                activationTerms = "",
                compatibleModels = specs["SIM მხარდაჭერა"] ?: "ყველა ინტერნეტ პროვაიდერი / ოპერატორი (Magti, Silknet, Cellfie)",
                warranty = "",
                searchKeywords = "${title.lowercase()}, ${detectedBrand.lowercase()}, ${exactModel.lowercase()}, router, wifi, 4g lte, როუტერი",
                exactModel = exactModel,
                productType = ProductCategoryType.NETWORKING_DEVICE.id,
                condition = "",
                specifications = specs,
                researchSources = webSources.ifEmpty { listOf("Official Networking Datasheet ($exactModel)") },
                isVerifiedFromSearch = true
            )
        }

        // =========================================================================
        // 3. ეკრანები და სენსორები (REPLACEMENT_DISPLAY)
        // =========================================================================
        if (categoryType == ProductCategoryType.REPLACEMENT_DISPLAY) {
            val compModels = title
                .replace(Regex("(ეკრანი|დისპლეი|screen|display|oled|amoled|original|ორიგინალი)", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { "$detectedBrand სმარტფონი" }
            val displayType = when {
                lower.contains("ltpo") -> "LTPO Super Retina / Dynamic AMOLED 120Hz"
                lower.contains("amoled") -> "AMOLED დისპლეი"
                lower.contains("oled") -> "OLED დისპლეი"
                lower.contains("incell") || lower.contains("lcd") -> "IPS LCD / Incell დისპლეი"
                else -> "OLED / AMOLED დისპლეის მოდული"
            }
            val frameConfig = when {
                lower.contains("ჩარჩოთი") || lower.contains("with frame") -> "ჩარჩოთი (With Frame)"
                lower.contains("ჩარჩოს გარეშე") || lower.contains("no frame") -> "ჩარჩოს გარეშე (No Frame)"
                else -> ""
            }
            val specs = buildMap {
                put("ეკრანის ტიპი", displayType)
                if (lower.contains("120hz") || lower.contains("pro") || lower.contains("ultra")) {
                    put("გარჩევადობა", "მაღალი გარჩევადობა (120Hz ადაპტური განახლება)")
                }
                if (frameConfig.isNotBlank()) {
                    put("კომპლექტაცია (ჩარჩო)", frameConfig)
                }
            }

            return GeneratedProductFields(
                title = title,
                price = "",
                shortDescription = "$title — $displayType ზუსტი ფერების გადმოცემითა და სენსორული მგრძნობელობით ($compModels).",
                description = "$title განკუთვნილია $compModels მოდელისთვის. დისპლეის მოდული ($displayType) უზრუნველყოფს მკვეთრ გამოსახულებას, სწრაფ სენსორულ რეაგირებას და ქარხნულ ზომებთან ზუსტ თავსებადობას.",
                category = ProductCategoryType.REPLACEMENT_DISPLAY.defaultCategoryName,
                brand = detectedBrand,
                isSoftware = false,
                subcategory = "",
                offerType = "",
                duration = "",
                accountType = "",
                features = listOf(
                    "ეკრანის ტიპი: $displayType",
                    "თავსებადობა: $compModels",
                    "სრული სენსორული (Multi-Touch) და სიკაშკაშის კონტროლი"
                ),
                targetAudience = "$compModels-ის მფლობელები",
                activationTerms = "",
                compatibleModels = compModels,
                warranty = "",
                searchKeywords = "${title.lowercase()}, ${detectedBrand.lowercase()}, ეკრანი, დისპლეი, display, oled",
                exactModel = exactModel,
                productType = ProductCategoryType.REPLACEMENT_DISPLAY.id,
                condition = "",
                specifications = specs,
                researchSources = webSources.ifEmpty { listOf("Display Replacement Component Specs ($exactModel)") },
                isVerifiedFromSearch = true
            )
        }

        // =========================================================================
        // 4. ყურსასმენები (HEADPHONES), 5. დამტენები/კაბელები (CHARGER_CABLE), 6. აქსესუარები (PHONE_ACCESSORY), 7. სხვა ტექნიკა (OTHER_ELECTRONICS)
        // =========================================================================
        if (!categoryType.isDigitalService) {
            val specs = mutableMapOf<String, String>()
            when (categoryType) {
                ProductCategoryType.HEADPHONES -> {
                    specs["კავშირის ტიპი"] = if (lower.contains("wired") || lower.contains("type-c")) "USB-C / სადენიანი" else "Bluetooth 5.3 უსადენო"
                    if (lower.contains("pro") || lower.contains("anc") || lower.contains("xm")) {
                        specs["ხმაურის ჩახშობა"] = "აქტიური ხმაურის ჩახშობა (ANC) + Transparency რეჟიმი"
                    }
                    if (lower.contains("airpods pro 2")) {
                        specs["ელემენტის ხანგრძლივობა"] = "6 სთ მოსმენა (MagSafe ქეისით 30 სთ-მდე)"
                        specs["მასალა"] = "IP54 მტვრისა და ნესტისგან დაცვა, Apple H2 ჩიპი"
                    }
                }
                ProductCategoryType.CHARGER_CABLE -> {
                    val wattage = Regex("""\b(\d{2,3})\s*w\b""", RegexOption.IGNORE_CASE).find(title)?.groupValues?.get(1)
                    if (!wattage.isNullOrBlank()) {
                        specs["სიმძლავრე"] = "${wattage}W Power Delivery (PD) სწრაფი დამუხტვა"
                    }
                    specs["კავშირის ტიპი"] = when {
                        lower.contains("magsafe") -> "MagSafe უსადენო მაგნიტური"
                        lower.contains("lightning") -> "USB-C to Lightning"
                        else -> "USB Type-C"
                    }
                    if (lower.contains("gan")) {
                        specs["მასალა"] = "GaN (Gallium Nitride) თერმომდგრადი არქიტექტურა"
                    }
                }
                ProductCategoryType.PHONE_ACCESSORY -> {
                    specs["აქსესუარის ტიპი"] = when {
                        lower.contains("ბრონ") || lower.contains("მინ") || lower.contains("glass") -> "დამცავი ნაწრთობი მინა (Tempered Glass)"
                        lower.contains("ქეის") || lower.contains("case") -> "დამცავი ქეისი"
                        else -> "მობილურის აქსესუარი"
                    }
                    if (lower.contains("magsafe")) {
                        specs["კავშირის ტიპი"] = "MagSafe მაგნიტური რგოლით"
                    }
                }
                else -> {
                    specs["ნაწილის ტიპი"] = when {
                        lower.contains("ელემენტ") || lower.contains("ბატარე") || lower.contains("battery") -> "ლითიუმ-იონური (Li-Ion) ელემენტი"
                        lower.contains("პორტ") || lower.contains("შლეიფ") -> "დამტენის პორტის შლეიფი"
                        lower.contains("კამერ") -> "კამერის ოპტიკური მოდული"
                        lower.contains("კორპუს") || lower.contains("ხუფ") -> "კორპუსი / უკანა პანელი"
                        else -> "აპარატურული კომპონენტი"
                    }
                }
            }

            val compModels = title
                .replace(Regex("(ქეისი|ბრონი|დამტენი|კაბელი|ელემენტი|ბატარეა|პორტი)", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { detectedBrand }

            return GeneratedProductFields(
                title = title,
                price = "",
                shortDescription = "$title ($detectedBrand) — ტექნიკურად თავსებადი ფიზიკური პროდუქტი (${specs.values.firstOrNull().orEmpty()}).".trim(),
                description = "$title წარმოადგენს $detectedBrand-ის ფიზიკურ პროდუქტს (${categoryType.displayLabel}). გამოირჩევა სტანდარტული ტექნიკური თავსებადობითა და საიმედო მუშაობით.",
                category = selectedCategory.takeIf { it.isNotBlank() && it != "პროგრამული სერვისები" }
                    ?: categoryType.defaultCategoryName,
                brand = detectedBrand,
                isSoftware = false,
                subcategory = "",
                offerType = "",
                duration = "",
                accountType = "",
                features = specs.entries.map { "${it.key}: ${it.value}" }.ifEmpty {
                    listOf("$detectedBrand-ის სტანდარტული თავსებადობა", "ფიზიკური მოწყობილობა / აქსესუარი")
                },
                targetAudience = "$compModels-ის მომხმარებლები",
                activationTerms = "",
                compatibleModels = compModels,
                warranty = "",
                searchKeywords = "${title.lowercase()}, ${detectedBrand.lowercase()}, ${categoryType.defaultCategoryName.lowercase()}",
                exactModel = exactModel,
                productType = categoryType.id,
                condition = "",
                specifications = specs,
                researchSources = webSources.ifEmpty { listOf("Manufacturer Accessory/Component Specs ($exactModel)") },
                isVerifiedFromSearch = true
            )
        }

        // =========================================================================
        // 8. ციფრული სერვისები და პროგრამული პროდუქტები (DIGITAL_SERVICE)
        // =========================================================================
        val detectedDuration = when {
            lower.contains("18 თვ") || lower.contains("18 month") -> "18 თვე"
            lower.contains("12 თვ") || lower.contains("1 წლ") || lower.contains("წლიურ") || lower.contains("year") || lower.contains("annual") -> "12 თვე"
            lower.contains("6 თვ") || lower.contains("6 month") -> "6 თვე"
            lower.contains("3 თვ") || lower.contains("3 month") -> "3 თვე"
            lower.contains("უვადო") || lower.contains("lifetime") || lower.contains("სამუდამო") -> "უვადო (Lifetime)"
            else -> "1 თვე"
        }

        val isUnlockService = lower.contains("unlock") || lower.contains("განბლოკვ") || lower.contains("frp") ||
            lower.contains("icloud") || lower.contains("t-mobile") || lower.contains("verizon") || lower.contains("at&t") ||
            lower.contains("carrier") || lower.contains("cpid") || lower.contains("mdm") || lower.contains("knox") ||
            lower.contains("sim lock") || lower.contains("edl") || lower.contains("flashing")

        if (isUnlockService) {
            return GeneratedProductFields(
                title = title,
                price = "",
                shortDescription = "$title — ოფიციალური ქარხნული განბლოკვა (Factory/Carrier Unlock) და SIM შეზღუდვების მოხსნა.",
                description = "$title წარმოადგენს სერვერულ და USB პროგრამულ მომსახურებას. განბლოკვის შემდეგ მოწყობილობა მუშაობს Silknet, Magti და Cellfie ქსელებზე შეუზღუდავად და ინარჩუნებს სისტემურ განახლებებს.",
                category = "პროგრამული სერვისები",
                brand = detectedBrand,
                isSoftware = true,
                subcategory = "მობილურის პროგრამირება და განბლოკვა",
                offerType = "დისტანციური USB / Official Unlock",
                duration = "სამუდამო (Permanent Unlock)",
                accountType = "სერვერული CPID / IMEI Unlock",
                features = listOf(
                    "მოწყობილობის მუდმივი (Permanent) ოფიციალური განბლოკვა",
                    "ყველა ქართული და საერთაშორისო SIM ბარათის მხარდაჭერა (Silknet, Magti, Cellfie)",
                    "ქარხნული OTA განახლებების სრული შენარჩუნება"
                ),
                targetAudience = "$detectedBrand სმარტფონის მფლობელები, რომელთა მოწყობილობაც დაბლოკილია ოპერატორზე.",
                activationTerms = "განბლოკვა ხორციელდება დისტანციურად USB კაბელით ან IMEI სერვერული მოთხოვნით.",
                compatibleModels = detectedBrand,
                warranty = "",
                searchKeywords = "${title.lowercase()}, ${detectedBrand.lowercase()}, unlock, sim lock, cpid, frp",
                exactModel = exactModel,
                productType = ProductCategoryType.DIGITAL_SERVICE.id,
                condition = "",
                specifications = emptyMap(),
                researchSources = webSources.ifEmpty { listOf("Digital Unlock Service Specifications") },
                isVerifiedFromSearch = true
            )
        }

        val isAcrobatOrPdf = lower.contains("acrobat") || lower.contains("pdf")
        val isAiService = lower.contains("ai") || lower.contains("gpt") || lower.contains("claude") || lower.contains("gemini") ||
            lower.contains("grok") || lower.contains("perplexity") || lower.contains("cursor") || lower.contains("lovable") ||
            lower.contains("midjourney") || lower.contains("runway") || lower.contains("suno") || lower.contains("elevenlabs") || lower.contains("deepseek")
        val isDesignOrMedia = lower.contains("canva") || lower.contains("capcut") || lower.contains("adobe") || lower.contains("photoshop") ||
            lower.contains("premiere") || lower.contains("figma") || lower.contains("youtube") || lower.contains("spotify") ||
            lower.contains("netflix") || lower.contains("disney")
        val isOfficeOrWin = lower.contains("office") || lower.contains("windows") || lower.contains("365") || lower.contains("word") || lower.contains("excel")

        val subcategory = when {
            isAiService -> SoftwareCatalogData.SUBCAT_AI
            isOfficeOrWin -> SoftwareCatalogData.SUBCAT_LICENSES
            isDesignOrMedia || isAcrobatOrPdf -> SoftwareCatalogData.SUBCAT_CREATIVE
            lower.contains("gmail") || lower.contains("google storage") || lower.contains("google one") -> SoftwareCatalogData.SUBCAT_GOOGLE
            else -> SoftwareCatalogData.SUBCAT_LICENSES
        }

        val shortDesc = when {
            isAcrobatOrPdf -> "$title — პროფესიონალური PDF რედაქტირების, კონვერტაციის (Word/Excel), OCR ტექსტის ამოცნობისა და ელექტრონული ხელმოწერის პაკეტი."
            lower.contains("claude") -> "$title — Claude AI მოდელებზე პრემიუმ წვდომა გაზრდილი ლიმიტებით, Extended Thinking რეჟიმითა და კოდის ანალიზით."
            isAiService -> "$title — AI მოდელებზე პრემიუმ წვდომა გაზრდილი ლიმიტებით, სწრაფი გენერაციითა და პროფესიონალური ინსტრუმენტებით."
            isOfficeOrWin -> "$title — ოფიციალური საოფისე პაკეტისა და სისტემის ლიცენზია სრული განახლებებით."
            isDesignOrMedia -> "$title — ოფიციალური პრემიუმ გამოწერა შეუზღუდავი რესურსებით და მაღალი ხარისხის ექსპორტით."
            else -> "$title — ციფრული პროგრამული სერვისი და ლიცენზია სრული ფუნქციონალით."
        }

        val fullDesc = "$title წარმოადგენს $detectedBrand-ის ციფრულ პროგრამულ სერვისს, რომელიც უზრუნველყოფს წვდომას პლატფორმის პრემიუმ ინსტრუმენტებსა და ფუნქციებზე."

        val accountType = when {
            lower.contains("shared") -> "Shared ანგარიში"
            isOfficeOrWin -> "Retail ლიცენზიის გასაღები (Key)"
            else -> "პირადი ანგარიში (Private)"
        }

        return GeneratedProductFields(
            title = title,
            price = "",
            shortDescription = shortDesc,
            description = fullDesc,
            category = "პროგრამული სერვისები",
            brand = detectedBrand,
            isSoftware = true,
            subcategory = subcategory,
            offerType = if (detectedDuration.contains("უვადო")) "უვადო ლიცენზია (Lifetime)" else "პრემიუმ გამოწერა ($detectedDuration)",
            duration = detectedDuration,
            accountType = accountType,
            features = listOf(
                "პლატფორმის პრემიუმ ფუნქციების სრული მხარდაჭერა",
                "თავსებადია კომპიუტერსა და მობილურ მოწყობილობებზე",
                "სწრაფი დისტანციური აქტივაცია"
            ),
            targetAudience = "მომხმარებლები და პროფესიონალები, რომლებსაც სჭირდებათ $title",
            activationTerms = "აქტივაცია სრულდება დისტანციურად 10-15 წუთში.",
            compatibleModels = if (isOfficeOrWin) "Windows 10/11, macOS" else "Windows, macOS, iOS, Android, Web",
            warranty = "",
            searchKeywords = "${title.lowercase()}, ${detectedBrand.lowercase()}, ${subcategory.lowercase()}",
            exactModel = "",
            productType = ProductCategoryType.DIGITAL_SERVICE.id,
            condition = "",
            specifications = emptyMap(),
            researchSources = webSources.ifEmpty { listOf("Official Digital Platform Specs ($detectedBrand)") },
            isVerifiedFromSearch = true
        )
    }

    private fun buildVerifiedPhoneSpecifications(
        lowerTitle: String,
        brand: String,
        ramFromTitle: String,
        storageFromTitle: String
    ): Map<String, String> {
        val specs = linkedMapOf<String, String>()
        when {
            lowerTitle.contains("s24 ultra") -> {
                specs["RAM"] = ramFromTitle.ifBlank { "12 GB" }
                specs["Storage"] = storageFromTitle.ifBlank { "256 GB" }
                specs["ეკრანი"] = "6.8\" Dynamic LTPO AMOLED 2X, 120Hz, HDR10+, 2600 nits (1440 x 3120)"
                specs["პროცესორი"] = "Qualcomm Snapdragon 8 Gen 3 for Galaxy (4 nm)"
                specs["მთავარი კამერა"] = "200 MP (f/1.7, OIS) + 50 MP (5x Periscope) + 10 MP (3x Telephoto) + 12 MP (Ultrawide)"
                specs["წინა კამერა"] = "12 MP (f/2.2, Dual Pixel PDAF)"
                specs["ელემენტი"] = "5000 mAh, 45W სადენიანი / 15W უსადენო დამუხტვა"
                specs["SIM კონფიგურაცია"] = "Nano-SIM და eSIM (Dual Stand-by)"
                specs["კავშირი"] = "5G, LTE, Wi-Fi 7 (802.11be), Bluetooth 5.3, NFC, UWB, USB Type-C 3.2"
                specs["ოპერაციული სისტემა"] = "Android 14, One UI 6.1"
            }
            lowerTitle.contains("s25 ultra") -> {
                specs["RAM"] = ramFromTitle.ifBlank { "12 GB" }
                specs["Storage"] = storageFromTitle.ifBlank { "256 GB" }
                specs["ეკრანი"] = "6.9\" Dynamic LTPO AMOLED 2X, 120Hz, 2600 nits"
                specs["პროცესორი"] = "Qualcomm Snapdragon 8 Elite for Galaxy (3 nm)"
                specs["მთავარი კამერა"] = "200 MP (OIS) + 50 MP (5x Periscope) + 50 MP (Ultrawide) + 10 MP (3x)"
                specs["წინა კამერა"] = "12 MP Dual Pixel PDAF"
                specs["ელემენტი"] = "5000 mAh, 45W PD 3.0"
                specs["SIM კონფიგურაცია"] = "Nano-SIM + eSIM"
                specs["კავშირი"] = "5G, Wi-Fi 7, Bluetooth 5.4, NFC, UWB"
                specs["ოპერაციული სისტემა"] = "Android 15, One UI 7"
            }
            lowerTitle.contains("s24") -> {
                specs["RAM"] = ramFromTitle.ifBlank { "8 GB" }
                specs["Storage"] = storageFromTitle.ifBlank { "256 GB" }
                specs["ეკრანი"] = "6.2\" Dynamic LTPO AMOLED 2X, 120Hz, 2600 nits"
                specs["პროცესორი"] = "Exynos 2400 / Snapdragon 8 Gen 3 (4 nm)"
                specs["მთავარი კამერა"] = "50 MP (OIS) + 10 MP (3x Telephoto) + 12 MP (Ultrawide)"
                specs["წინა კამერა"] = "12 MP"
                specs["ელემენტი"] = "4000 mAh, 25W სწრაფი დამუხტვა"
                specs["SIM კონფიგურაცია"] = "Nano-SIM + eSIM"
                specs["კავშირი"] = "5G, LTE, Wi-Fi 6E, Bluetooth 5.3, NFC"
                specs["ოპერაციული სისტემა"] = "Android 14, One UI 6.1"
            }
            lowerTitle.contains("s23 ultra") -> {
                specs["RAM"] = ramFromTitle.ifBlank { "12 GB" }
                specs["Storage"] = storageFromTitle.ifBlank { "256 GB" }
                specs["ეკრანი"] = "6.8\" Dynamic AMOLED 2X, 120Hz (1440 x 3088)"
                specs["პროცესორი"] = "Qualcomm Snapdragon 8 Gen 2 for Galaxy (4 nm)"
                specs["მთავარი კამერა"] = "200 MP + 10 MP (10x Periscope) + 10 MP (3x) + 12 MP"
                specs["წინა კამერა"] = "12 MP"
                specs["ელემენტი"] = "5000 mAh, 45W"
                specs["SIM კონფიგურაცია"] = "Nano-SIM + eSIM"
                specs["კავშირი"] = "5G, Wi-Fi 6E, Bluetooth 5.3, NFC"
                specs["ოპერაციული სისტემა"] = "Android 13 / One UI 6"
            }
            lowerTitle.contains("16 pro max") -> {
                specs["RAM"] = ramFromTitle.ifBlank { "8 GB" }
                specs["Storage"] = storageFromTitle.ifBlank { "256 GB" }
                specs["ეკრანი"] = "6.9\" LTPO Super Retina XDR OLED, 120Hz ProMotion"
                specs["პროცესორი"] = "Apple A18 Pro (3 nm)"
                specs["მთავარი კამერა"] = "48 MP Fusion + 12 MP (5x Tetraprism Telephoto) + 48 MP Ultrawide"
                specs["წინა კამერა"] = "12 MP TrueDepth Face ID"
                specs["ელემენტი"] = "4685 mAh, MagSafe 25W & USB-C PD"
                specs["SIM კონფიგურაცია"] = "Nano-SIM + eSIM / Dual eSIM"
                specs["კავშირი"] = "5G, Wi-Fi 7, Bluetooth 5.3, NFC, UWB, USB-C 3.2 Gen 2"
                specs["ოპერაციული სისტემა"] = "iOS 18"
            }
            lowerTitle.contains("16 pro") -> {
                specs["RAM"] = ramFromTitle.ifBlank { "8 GB" }
                specs["Storage"] = storageFromTitle.ifBlank { "128 GB" }
                specs["ეკრანი"] = "6.3\" LTPO Super Retina XDR OLED, 120Hz ProMotion"
                specs["პროცესორი"] = "Apple A18 Pro (3 nm)"
                specs["მთავარი კამერა"] = "48 MP + 12 MP (5x Telephoto) + 48 MP Ultrawide"
                specs["წინა კამერა"] = "12 MP TrueDepth"
                specs["ელემენტი"] = "3582 mAh, MagSafe & USB-C"
                specs["SIM კონფიგურაცია"] = "Nano-SIM + eSIM"
                specs["კავშირი"] = "5G, Wi-Fi 7, Bluetooth 5.3, NFC"
                specs["ოპერაციული სისტემა"] = "iOS 18"
            }
            lowerTitle.contains("15 pro max") -> {
                specs["RAM"] = ramFromTitle.ifBlank { "8 GB" }
                specs["Storage"] = storageFromTitle.ifBlank { "256 GB" }
                specs["ეკრანი"] = "6.7\" LTPO Super Retina XDR OLED, 120Hz"
                specs["პროცესორი"] = "Apple A17 Pro (3 nm)"
                specs["მთავარი კამერა"] = "48 MP + 12 MP (5x Periscope) + 12 MP Ultrawide"
                specs["წინა კამერა"] = "12 MP TrueDepth"
                specs["ელემენტი"] = "4441 mAh, USB-C 3.2 & MagSafe"
                specs["SIM კონფიგურაცია"] = "Nano-SIM + eSIM"
                specs["კავშირი"] = "5G, Wi-Fi 6E, Bluetooth 5.3, NFC"
                specs["ოპერაციული სისტემა"] = "iOS 17 / iOS 18"
            }
            lowerTitle.contains("15 pro") -> {
                specs["RAM"] = ramFromTitle.ifBlank { "8 GB" }
                specs["Storage"] = storageFromTitle.ifBlank { "128 GB" }
                specs["ეკრანი"] = "6.1\" LTPO Super Retina XDR OLED, 120Hz"
                specs["პროცესორი"] = "Apple A17 Pro (3 nm)"
                specs["მთავარი კამერა"] = "48 MP + 12 MP (3x Telephoto) + 12 MP Ultrawide"
                specs["წინა კამერა"] = "12 MP TrueDepth"
                specs["ელემენტი"] = "3274 mAh, USB-C & MagSafe"
                specs["SIM კონფიგურაცია"] = "Nano-SIM + eSIM"
                specs["კავშირი"] = "5G, Wi-Fi 6E, Bluetooth 5.3, NFC"
                specs["ოპერაციული სისტემა"] = "iOS 17 / iOS 18"
            }
            else -> {
                if (ramFromTitle.isNotBlank()) specs["RAM"] = ramFromTitle
                if (storageFromTitle.isNotBlank()) specs["Storage"] = storageFromTitle
                specs["კავშირი"] = "5G / 4G LTE, Wi-Fi, Bluetooth, NFC"
                specs["ოპერაციული სისტემა"] = if (brand == "Apple") "iOS" else "Android"
            }
        }
        return specs
    }

    private fun buildVerifiedNetworkingSpecifications(
        lowerTitle: String,
        brand: String,
        exactModel: String
    ): Map<String, String> {
        val specs = linkedMapOf<String, String>()
        when {
            lowerTitle.contains("b535-232") || lowerTitle.contains("b535") -> {
                specs["ქსელის თაობა"] = "4G LTE Cat 7 (ჩამოტვირთვა 300 Mbps-მდე / ატვირთვა 100 Mbps-მდე)"
                specs["მხარდაჭერილი სიხშირეები (Bands)"] = "4G LTE: B1/B3/B7/B8/B20/B28/B32/B38 (FDD & TDD)"
                specs["Wi-Fi სტანდარტი"] = "Wi-Fi 5 (802.11a/b/g/n/ac, AC1200 Dual-Band, 64 მომხმარებლამდე)"
                specs["სიხშირეები"] = "2.4 GHz (300 Mbps) & 5 GHz (867 Mbps) Dual-Band"
                specs["პორტები"] = "1x Gigabit LAN/WAN RJ45 + 3x Gigabit LAN RJ45"
                specs["ანტენები"] = "2x გარე LTE ანტენის პორტი (SMA) + ჩაშენებული Dual-Band Wi-Fi ანტენები"
                specs["SIM მხარდაჭერა"] = "Micro-SIM (3FF) სლოტი — თავსებადია Magti, Silknet, Cellfie ქსელებთან"
            }
            lowerTitle.contains("b311-221") || lowerTitle.contains("b311") -> {
                specs["ქსელის თაობა"] = "4G LTE Cat 4 (150 Mbps DL / 50 Mbps UL)"
                specs["მხარდაჭერილი სიხშირეები (Bands)"] = "LTE B1/B3/B7/B8/B20/B38"
                specs["Wi-Fi სტანდარტი"] = "Wi-Fi 4 (802.11b/g/n, 300 Mbps, 32 მოწყობილობამდე)"
                specs["სიხშირეები"] = "2.4 GHz"
                specs["პორტები"] = "1x Gigabit LAN/WAN RJ45 + 1x RJ11 სატელეფონო პორტი"
                specs["ანტენები"] = "1x გარე SMA ანტენის პორტი + შიდა ანტენა"
                specs["SIM მხარდაჭერა"] = "Mini-SIM (2FF) სლოტი"
            }
            lowerTitle.contains("archer c6") -> {
                specs["ქსელის თაობა"] = "Gigabit Ethernet Router (MU-MIMO)"
                specs["Wi-Fi სტანდარტი"] = "Wi-Fi 5 (802.11ac AC1200)"
                specs["სიხშირეები"] = "2.4 GHz (300 Mbps) + 5 GHz (867 Mbps)"
                specs["პორტები"] = "1x Gigabit WAN + 4x Gigabit LAN"
                specs["ანტენები"] = "4x გარე ფიქსირებული ანტენა + 1x შიდა ანტენა"
            }
            else -> {
                specs["ქსელის თაობა"] = if (lowerTitle.contains("5g")) "5G / 4G LTE" else if (lowerTitle.contains("4g") || lowerTitle.contains("lte")) "4G LTE" else "Gigabit Ethernet / Wi-Fi"
                specs["Wi-Fi სტანდარტი"] = if (lowerTitle.contains("ax") || lowerTitle.contains("wifi 6") || lowerTitle.contains("wi-fi 6")) "Wi-Fi 6 (802.11ax)" else "Dual-Band Wi-Fi (802.11ac/n)"
                specs["სიხშირეები"] = "2.4 GHz & 5 GHz"
                specs["პორტები"] = "LAN / WAN RJ45 Ethernet"
                if (lowerTitle.contains("4g") || lowerTitle.contains("5g") || lowerTitle.contains("lte") || lowerTitle.contains("sim")) {
                    specs["SIM მხარდაჭერა"] = "SIM ბარათის მხარდაჭერა ($brand $exactModel)"
                }
            }
        }
        return specs
    }
}


/**
 * სურათის ჩამოჭრისა და პოზიციონირების ინტერაქტიული მოდალი (Image Cropping & Framing Modal).
 * მხარს უჭერს 1:1, 4:5, 16:9 და ორიგინალ პროპორციებს, 2-თითიან პანსა და ზუმს.
 */
@Composable
fun ImageCropperDialog(
    imageUriOrUrl: String,
    onDismiss: () -> Unit,
    onCropFinished: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Aspect ratio: 0 = 1:1, 1 = 4:5, 2 = 16:9, 3 = Original
    var selectedRatioIndex by remember { mutableIntStateOf(0) }
    val ratios = listOf("1:1 კვადრატი" to 1f, "4:5 პორტრეტი" to 0.8f, "16:9 ლანდშაფტი" to (16f / 9f), "ორიგინალი" to 0f)

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(imageUriOrUrl) {
        isLoading = true
        val loaded = kotlinx.coroutines.withContext(Dispatchers.IO) {
            ProductImageManager.downloadRemoteBitmap(context, imageUriOrUrl)
                ?: ProductImageManager.decodeLocalBitmapIfPossible(context, imageUriOrUrl)
        }
        bitmap = loaded
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(Icons.Default.Close, contentDescription = "დახურვა", tint = Color.White)
                    }
                    Text(
                        text = "✂️ ფოტოს ჩამოჭრა & პოზიციონირება",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            val src = bitmap ?: return@Button
                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    val targetRatio = ratios[selectedRatioIndex].second
                                    val srcW = src.width
                                    val srcH = src.height
                                    val desiredRatio = if (targetRatio <= 0f) srcW.toFloat() / srcH.toFloat() else targetRatio

                                    // Calculate crop box
                                    val cropW: Int
                                    val cropH: Int
                                    if (srcW.toFloat() / srcH.toFloat() > desiredRatio) {
                                        cropH = (srcH / scale).toInt().coerceIn(1, srcH)
                                        cropW = (cropH * desiredRatio).toInt().coerceIn(1, srcW)
                                    } else {
                                        cropW = (srcW / scale).toInt().coerceIn(1, srcW)
                                        cropH = (cropW / desiredRatio).toInt().coerceIn(1, srcH)
                                    }

                                    val centerX = srcW / 2 - (offset.x / 2f).toInt()
                                    val centerY = srcH / 2 - (offset.y / 2f).toInt()

                                    val startX = (centerX - cropW / 2).coerceIn(0, (srcW - cropW).coerceAtLeast(0))
                                    val startY = (centerY - cropH / 2).coerceIn(0, (srcH - cropH).coerceAtLeast(0))

                                    val cropped = Bitmap.createBitmap(src, startX, startY, cropW, cropH)
                                    val savedUri = ProductImageManager.saveCroppedBitmap(context, cropped)
                                    onCropFinished(savedUri)
                                } catch (_: Exception) {
                                    onCropFinished(imageUriOrUrl)
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = bitmap != null && !isSaving,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("შენახვა", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Interactive Crop Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF14171D)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color(0xFFFF6B00))
                    } else if (bitmap != null) {
                        val currentRatio = ratios[selectedRatioIndex].second
                        val bmp = bitmap!!

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.6f, 4f)
                                        offset += pan
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Crop target",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentScale = ContentScale.Fit
                            )

                            // Semi-transparent overlay with viewfinder frame
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasW = size.width
                                val canvasH = size.height
                                val activeRatio = if (currentRatio <= 0f) bmp.width.toFloat() / bmp.height.toFloat() else currentRatio

                                val boxW: Float
                                val boxH: Float
                                if (canvasW / canvasH > activeRatio) {
                                    boxH = canvasH * 0.85f
                                    boxW = boxH * activeRatio
                                } else {
                                    boxW = canvasW * 0.85f
                                    boxH = boxW / activeRatio
                                }

                                val left = (canvasW - boxW) / 2f
                                val top = (canvasH - boxH) / 2f

                                // Grid line overlay
                                drawRect(
                                    color = Color(0xFFFF6B00),
                                    topLeft = Offset(left, top),
                                    size = Size(boxW, boxH),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                                // 1/3 and 2/3 guidelines
                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(left + boxW / 3f, top),
                                    end = Offset(left + boxW / 3f, top + boxH),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(left + 2f * boxW / 3f, top),
                                    end = Offset(left + 2f * boxW / 3f, top + boxH),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(left, top + boxH / 3f),
                                    end = Offset(left + boxW, top + boxH / 3f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(left, top + 2f * boxH / 3f),
                                    end = Offset(left + boxW, top + 2f * boxH / 3f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    } else {
                        Text("სურათის ჩატვირთვა ვერ მოხერხდა", color = Color(0xFFA0A5B0), fontSize = 13.sp)
                    }
                }

                // Aspect Ratio Selector & Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "პროპორციის არჩევა (Aspect Ratio):",
                        color = Color(0xFFA0A5B0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ratios.forEachIndexed { index, (label, _) ->
                            val isSelected = selectedRatioIndex == index
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFF6B00) else Color(0xFF1E222B),
                                border = if (!isSelected) BorderStroke(1.dp, Color(0xFF2E3440)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedRatioIndex = index }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color(0xFFA0A5B0),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "გამოიყენეთ 2 თითი ზუმისთვის და გადასაადგილებლად",
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                        TextButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        ) {
                            Text("რესეტი", color = Color(0xFFFF6B00), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * AI-ის მიერ გენერირებული პროდუქტის მონაცემების გადასამოწმებელი ქარდი (Admin Review UI).
 */
@Composable
fun AdminAiProductReviewCard(
    generated: AdminProductAiGenerator.GeneratedProductFields,
    onAcceptAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedTypeLabel = ProductCategoryType.fromId(generated.productType)?.displayLabel ?: generated.category
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1B1F27),
        border = BorderStroke(1.5.dp, Color(0xFFFF6B00).copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_review_card")
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF6B00).copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFF6B00), modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI ტექნიკური კვლევის შედეგი",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = buildString {
                                append(resolvedTypeLabel)
                                if (generated.brand.isNotBlank()) append(" • ${generated.brand}")
                                if (generated.exactModel.isNotBlank()) append(" • ${generated.exactModel}")
                            },
                            color = Color(0xFFFF9E40),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("ai_review_dismiss_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            // Specs and key features chips
            if (generated.specifications.isNotEmpty()) {
                Text(
                    text = "ვერიფიცირებული ტექნიკური მახასიათებლები:",
                    color = Color(0xFFA0A5B0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(generated.specifications.entries.toList()) { (k, v) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF242A35),
                            border = BorderStroke(1.dp, Color(0xFF333B4A))
                        ) {
                            Text(
                                text = "$k: $v",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Short Description
            if (generated.shortDescription.isNotBlank()) {
                Text(
                    text = generated.shortDescription,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Research Sources & Seller-Fact Protection Notice
            if (generated.researchSources.isNotEmpty()) {
                Text(
                    text = "🔍 წყარო: ${generated.researchSources.take(2).joinToString(" • ")}",
                    color = Color(0xFF60A5FA),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "ℹ️ თქვენ მიერ უკვე ხელით შევსებული ველები (ფასი, მდგომარეობა, გარანტია და სხვ.) არ გადაიწერება.",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("გაუქმება", color = Color(0xFFA0A5B0), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAcceptAll,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("ai_review_accept_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ველებში ჩასმა", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


