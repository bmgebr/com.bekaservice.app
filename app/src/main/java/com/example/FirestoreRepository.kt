package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    val currentUser = auth?.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class FirestoreUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val address: String = "",
    val deviceModel: String = "",
    val balance: Double = 0.0,
    val favoriteProductIds: List<Int> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreOrder(
    @DocumentId val docId: String = "",
    val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val deliveryAddress: String = "",
    val deviceModel: String = "",
    val serviceType: String = "",
    val issueDescription: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val status: String = "pending",
    val price: String = "",
    val totalAmount: Double = 0.0,
    val currency: String = "GEL",
    val paymentMethod: String = "WhatsApp & ნაღდი / საბანკო გადარიცხვა",
    val trackingCode: String = "",
    val adminRead: Boolean = false,
    @ServerTimestamp val adminReadAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toOrderRecord(): OrderRecord {
        val resolvedId = id.ifBlank { docId }
        val formattedDate = createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMMM, yyyy • HH:mm", Locale.forLanguageTag("ka")).format(it)
        } ?: "ახლახანს"

        val parsedItems = if (items.isNotEmpty()) {
            items.map { map ->
                OrderItemDetail(
                    productTitle = (map["productTitle"] as? String) ?: (map["name"] as? String) ?: serviceType,
                    quality = (map["quality"] as? String) ?: "Original",
                    quantity = (map["quantity"] as? Number)?.toInt() ?: 1,
                    price = (map["price"] as? String) ?: price,
                    imageUrl = (map["imageUrl"] as? String) ?: "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600"
                )
            }
        } else {
            listOf(
                OrderItemDetail(
                    productTitle = serviceType.ifBlank { "სერვისის შეკვეთა" },
                    quality = deviceModel.ifBlank { "სტანდარტული" },
                    quantity = 1,
                    price = price.ifBlank { "${totalAmount.toInt()} ₾" },
                    imageUrl = "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600"
                )
            )
        }

        val uiStatus = mapFirestoreStatusToUi(status)
        val resolvedPrice = price.ifBlank {
            if (totalAmount > 0) "${totalAmount.toInt()} ₾" else "ფასი შეთანხმებით"
        }

        return OrderRecord(
            id = resolvedId,
            orderNumber = orderNumber.ifBlank { "#BS-${resolvedId.takeLast(4).uppercase()}" },
            date = formattedDate,
            items = parsedItems,
            totalAmount = resolvedPrice,
            status = uiStatus,
            paymentMethod = paymentMethod.ifBlank { "WhatsApp & საბანკო გადარიცხვა" },
            trackingCode = trackingCode.ifBlank { "BS-TRK-${resolvedId.takeLast(5).uppercase()}" }
        )
    }

    companion object {
        fun mapFirestoreStatusToUi(status: String): String {
            return when (status.lowercase()) {
                "completed", "შესრულებული", "დასრულებული" -> "შესრულებული"
                "cancelled", "გაუქმებული" -> "გაუქმებული"
                "in_progress", "დამუშავების პროცესში", "მზადდება", "გზაშია" -> "მიმდინარე"
                "pending", "ახალი", "მიმდინარე" -> "მიმდინარე"
                else -> "მიმდინარე"
            }
        }

        fun mapUiOrAdminStatusLabel(status: String): String {
            return when (status.lowercase()) {
                "pending" -> "ახალი (Pending)"
                "in_progress" -> "დამუშავების პროცესში (In Progress)"
                "completed" -> "დასრულებული (Completed)"
                "cancelled" -> "გაუქმებული (Cancelled)"
                else -> status
            }
        }
    }
}

@IgnoreExtraProperties
data class FirestoreDiagnostic(
    @DocumentId val docId: String = "",
    val id: String = "",
    val userId: String = "",
    val deviceModel: String = "",
    val issueCategory: String = "",
    val symptoms: String = "",
    val aiAnalysis: String = "",
    val estimatedCost: String = "",
    val confidence: Double = 95.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminOrderNotificationRecord(
    val notificationId: String = "",
    val orderId: String = "",
    val orderType: String = "imei_check", // "imei_check" | "service_order"
    val title: String = "",
    val body: String = "",
    val deepLink: String = "",
    val targetAdminEmail: String = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
    val read: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val readAtMillis: Long = 0L,
    val readByUid: String = ""
) {
    val id: String get() = notificationId
}

@IgnoreExtraProperties
data class AdminFcmTokenRecord(
    val tokenId: String = "",
    val token: String = "",
    val userId: String = "",
    val email: String = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
    val deviceId: String = "",
    val deviceModel: String = "",
    val platform: String = "android",
    val active: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

class FirestoreRepository(
    val db: FirebaseFirestore,
    private val appContext: Context? = null
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext),
        context.applicationContext
    )

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // ==========================================================================
    // ADMIN ROLE VERIFICATION (Server-Side / Token Claims / Admins Collection / Sole Admin beqa1994@gmail.com)
    // ==========================================================================
    suspend fun checkIsCurrentUserAdmin(): Boolean {
        val firebaseAuth = runCatching { auth }.getOrNull() ?: return false
        val currentUser = firebaseAuth.currentUser ?: return false
        return try {
            val tokenResult = runCatching { currentUser.getIdToken(false).await() }.getOrNull()
            val claims = tokenResult?.claims.orEmpty()

            val email = (claims["email"] as? String)
                ?.takeIf { it.isNotBlank() }
                ?: currentUser.email?.takeIf { it.isNotBlank() }
                ?: currentUser.providerData.firstNotNullOfOrNull { it.email?.takeIf { e -> e.isNotBlank() } }
                ?: ""

            // Strictly enforce single authorized administrator: "beqa1994@gmail.com"
            if (!AuthAndOrderManager.isSoleAdminEmail(email)) {
                return false
            }

            if (claims["admin"] == true) {
                return true
            }

            val adminDoc = runCatching {
                db.collection("admins").document(currentUser.uid).get().await()
            }.getOrNull()
            if (adminDoc != null && adminDoc.exists()) {
                return adminDoc.getBoolean("enabled") == true
            }

            val explicitEmailVerifiedClaim = claims["email_verified"] as? Boolean
            val firebaseClaimMap = claims["firebase"] as? Map<*, *>
            val signInProvider = firebaseClaimMap?.get("sign_in_provider") as? String
                ?: currentUser.providerData.firstOrNull { it.providerId != "firebase" }?.providerId
                ?: ""

            if (explicitEmailVerifiedClaim == false &&
                signInProvider != "password" &&
                signInProvider != "google.com"
            ) {
                return false
            }

            // Ensure /admins/{uid} document exists in Firestore for the verified sole admin
            runCatching {
                db.collection("admins").document(currentUser.uid).set(
                    mapOf(
                        "uid" to currentUser.uid,
                        "email" to AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                        "enabled" to true,
                        "role" to "sole_admin",
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            true
        } catch (e: Exception) {
            Log.w("FirestoreRepo", "Admin check failed: ${e.message}")
            AuthAndOrderManager.isSoleAdminEmail(currentUser.email)
        }
    }

    // ==========================================================================
    // USER PROFILE OPERATIONS
    // ==========================================================================
    suspend fun saveUserProfile(user: UserProfile) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("FirestoreRepo", "Skipping Firestore profile sync: user is in local session mode")
            return
        }
        saveUserProfileWithResult(user)
    }

    suspend fun saveUserProfileWithResult(user: UserProfile): Result<FirestoreUser> {
        val currentUser = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("პროფილის Cloud-ში შესანახად საჭიროა Google ავტორიზაცია")
            )
        val uid = currentUser.uid
        val docRef = db.collection("users").document(uid)
        val safeDisplayName = user.displayName.ifBlank { currentUser.displayName ?: "User" }.take(100)
        val safeEmail = user.email.ifBlank { currentUser.email ?: "user@bekaservice.ge" }.take(150)
        val safePhone = user.phone.take(30)
        val safePhotoUrl = user.avatarUrl.ifBlank { currentUser.photoUrl?.toString() ?: "" }.take(1000)
        val safeAddress = user.address.take(300)
        val safeDeviceModel = user.deviceModel.trim().take(120)

        return try {
            val existingSnapshot = try {
                docRef.get(Source.SERVER).await()
            } catch (_: Exception) {
                docRef.get().await()
            }
            if (existingSnapshot.exists() &&
                existingSnapshot.contains("userId") &&
                existingSnapshot.contains("createdAt")
            ) {
                val updatePayload = hashMapOf<String, Any>(
                    "displayName" to safeDisplayName,
                    "email" to safeEmail,
                    "phoneNumber" to safePhone,
                    "photoUrl" to safePhotoUrl,
                    "address" to safeAddress,
                    "deviceModel" to safeDeviceModel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(updatePayload, SetOptions.merge()).await()
            } else {
                val createOrMergePayload = hashMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeDisplayName,
                    "email" to safeEmail,
                    "phoneNumber" to safePhone,
                    "photoUrl" to safePhotoUrl,
                    "address" to safeAddress,
                    "deviceModel" to safeDeviceModel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!existingSnapshot.exists() || !existingSnapshot.contains("createdAt")) {
                    createOrMergePayload["createdAt"] = FieldValue.serverTimestamp()
                }
                docRef.set(createOrMergePayload, SetOptions.merge()).await()
            }
            Log.d("FirestoreRepo", "User profile saved successfully to Firestore: $uid (deviceModel=$safeDeviceModel)")
            Result.success(
                FirestoreUser(
                    userId = uid,
                    displayName = safeDisplayName,
                    email = safeEmail,
                    phoneNumber = safePhone,
                    photoUrl = safePhotoUrl,
                    address = safeAddress,
                    deviceModel = safeDeviceModel
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(preferServer: Boolean = false): FirestoreUser? {
        val currentUser = auth.currentUser ?: return null
        val docRef = db.collection("users").document(currentUser.uid)
        return try {
            val snapshot = if (preferServer) {
                try {
                    docRef.get(Source.SERVER).await()
                } catch (_: Exception) {
                    docRef.get().await()
                }
            } else {
                docRef.get().await()
            }
            if (!snapshot.exists()) return null
            val parsed = snapshot.toObject(FirestoreUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val rawFavs = (snapshot.get("favoriteProductIds") as? List<*>)
                ?.mapNotNull { (it as? Number)?.toInt() }
                ?.distinct()
                ?: parsed?.favoriteProductIds.orEmpty()
            parsed?.copy(favoriteProductIds = rawFavs)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun saveUserFavoritesWithResult(
        favoriteIds: Set<Int>,
        fallbackUser: UserProfile? = null
    ): Result<Set<Int>> {
        val currentUser = auth.currentUser
            ?: return Result.failure(
                IllegalStateException("ფავორიטების Cloud-ში შესანახად საჭიროა Google ავტორიზაცია")
            )
        val uid = currentUser.uid
        val docRef = db.collection("users").document(uid)
        val normalizedFavList = favoriteIds.filter { it > 0 }.sorted().take(500)

        return try {
            val existingSnapshot = try {
                docRef.get(Source.SERVER).await()
            } catch (_: Exception) {
                docRef.get().await()
            }
            if (existingSnapshot.exists() &&
                existingSnapshot.contains("userId") &&
                existingSnapshot.contains("createdAt")
            ) {
                val updatePayload = hashMapOf<String, Any>(
                    "favoriteProductIds" to normalizedFavList,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(updatePayload, SetOptions.merge()).await()
            } else {
                val safeDisplayName = (fallbackUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: currentUser.displayName
                    ?: "User").take(100)
                val safeEmail = (fallbackUser?.email?.takeIf { it.isNotBlank() }
                    ?: currentUser.email
                    ?: "user@bekaservice.ge").take(150)
                val safePhone = (fallbackUser?.phone ?: "").take(30)
                val safePhotoUrl = (fallbackUser?.avatarUrl?.takeIf { it.isNotBlank() }
                    ?: currentUser.photoUrl?.toString()
                    ?: "").take(1000)
                val safeAddress = (fallbackUser?.address ?: "").take(300)
                val safeDeviceModel = (fallbackUser?.deviceModel ?: "").trim().take(120)

                val createOrMergePayload = hashMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeDisplayName,
                    "email" to safeEmail,
                    "phoneNumber" to safePhone,
                    "photoUrl" to safePhotoUrl,
                    "address" to safeAddress,
                    "deviceModel" to safeDeviceModel,
                    "favoriteProductIds" to normalizedFavList,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!existingSnapshot.exists() || !existingSnapshot.contains("createdAt")) {
                    createOrMergePayload["createdAt"] = FieldValue.serverTimestamp()
                }
                docRef.set(createOrMergePayload, SetOptions.merge()).await()
            }
            Result.success(normalizedFavList.toSet())
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun listenToUserFavorites(
        onFavoritesUpdated: (Set<Int>) -> Unit,
        onError: (String) -> Unit = {}
    ): ListenerRegistration? {
        val currentUser = auth.currentUser ?: return null
        val docRef = db.collection("users").document(currentUser.uid)
        return docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                val errMsg = handleFirestoreError(error, OperationType.GET, docRef.path)
                onError(errMsg)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists() && snapshot.contains("favoriteProductIds")) {
                val favIds = (snapshot.get("favoriteProductIds") as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toInt() }
                    ?.filter { it > 0 }
                    ?.toSet()
                    ?: emptySet()
                onFavoritesUpdated(favIds)
            }
        }
    }

    suspend fun getAllUsersForAdmin(): Result<List<FirestoreUser>> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: მომხმარებლების სიის სანახავად საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        return try {
            val snapshot = db.collection("users").get().await()
            val users = snapshot.toObjects(FirestoreUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            Result.success(users)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users")
            Result.failure(e)
        }
    }

    // ==========================================================================
    // SERVICE & PRODUCT ORDERS OPERATIONS
    // ==========================================================================
    suspend fun createServiceOrder(
        deviceModel: String,
        serviceType: String,
        issueDescription: String,
        price: String = "",
        userName: String = "",
        userPhone: String = ""
    ): String {
        val orderId = "order_" + System.currentTimeMillis()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("FirestoreRepo", "Skipping Firestore order sync: user is in local session mode")
            return orderId
        }
        val uid = currentUser.uid
        val docRef = db.collection("orders").document(orderId)
        val numericAmount = price.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
        val payload = hashMapOf<String, Any>(
            "id" to orderId,
            "orderNumber" to "#BS-${orderId.takeLast(4)}",
            "userId" to uid,
            "userName" to userName.ifBlank { currentUser.displayName ?: "მომხმარებელი" }.take(100),
            "userPhone" to userPhone.ifBlank { "+995 598 23 33 32" }.take(30),
            "userEmail" to (currentUser.email ?: "").take(150),
            "deviceModel" to deviceModel.ifBlank { "Device" }.take(100),
            "serviceType" to serviceType.ifBlank { "Service" }.take(300),
            "issueDescription" to issueDescription.take(4000),
            "status" to "pending",
            "price" to price.take(50),
            "totalAmount" to numericAmount,
            "priceVerified" to false,
            "currency" to "GEL",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            Log.d("FirestoreRepo", "Order created in Firestore: $orderId")
            runCatching {
                createAdminOrderNotification(orderId = orderId, orderType = "service_order")
            }
            appContext?.let { ctx ->
                BekaNotificationManager.recordOrderCreatedForAdminOnly(
                    context = ctx,
                    orderId = orderId,
                    orderType = "service_order"
                )
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
        return orderId
    }

    suspend fun createOrderFromCart(
        orderRecord: OrderRecord,
        user: UserProfile?
    ): Result<String> {
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("შეკვეთის ონლაინ ბაზაში შესანახად საჭიროა Google ავტორიზაცია"))

        val uid = currentUser.uid
        val safeOrderId = orderRecord.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "ord_${System.currentTimeMillis()}" }
        val docRef = db.collection("orders").document(safeOrderId)

        val snapshotItems = orderRecord.items.take(50).map { item ->
            hashMapOf<String, Any>(
                "productTitle" to item.productTitle.take(200),
                "name" to item.productTitle.take(200),
                "quality" to item.quality.take(100),
                "quantity" to item.quantity,
                "price" to item.price.take(50),
                "imageUrl" to item.imageUrl.take(1000)
            )
        }

        val summaryTitle = orderRecord.items.joinToString(", ") { "${it.productTitle} x${it.quantity}" }
            .ifBlank { "შეკვეთა კალათიდან" }
            .take(300)
        val detailsSummary = orderRecord.items.joinToString("\n") {
            "• ${it.productTitle} (${it.quality}) x${it.quantity} — ${it.price}"
        }.take(4000)
        val numericTotal = orderRecord.totalAmount.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

        val payload = hashMapOf<String, Any>(
            "id" to safeOrderId,
            "orderNumber" to orderRecord.orderNumber.take(40),
            "userId" to uid,
            "userName" to (user?.displayName ?: currentUser.displayName ?: "მომხმარებელი").take(100),
            "userPhone" to (user?.phone ?: "+995 598 23 33 32").ifBlank { "+995 598 23 33 32" }.take(30),
            "userEmail" to (user?.email ?: currentUser.email ?: "").take(150),
            "deliveryAddress" to (user?.address ?: "ქ. ახალციხე, კოსტავას ქ. 10").take(300),
            "deviceModel" to (user?.deviceModel ?: "Device").ifBlank { "Device" }.take(100),
            "serviceType" to summaryTitle,
            "issueDescription" to detailsSummary,
            "items" to snapshotItems,
            "status" to "pending",
            "price" to orderRecord.totalAmount.take(50),
            "totalAmount" to numericTotal,
            "priceVerified" to false,
            "currency" to "GEL",
            "paymentMethod" to orderRecord.paymentMethod.take(120),
            "trackingCode" to orderRecord.trackingCode.take(60),
            "adminRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Log.d("FirestoreRepo", "Cart order saved to Firestore: $safeOrderId")
            runCatching {
                createAdminOrderNotification(orderId = safeOrderId, orderType = "service_order")
            }
            appContext?.let { ctx ->
                BekaNotificationManager.recordOrderCreatedForAdminOnly(
                    context = ctx,
                    orderId = safeOrderId,
                    orderType = "service_order"
                )
            }
            Result.success(safeOrderId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserOrders(): List<FirestoreOrder> {
        val currentUser = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
            snapshot.toObjects(FirestoreOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "orders")
            throw e
        }
    }

    fun listenToUserOrders(
        onOrdersUpdated: (List<OrderRecord>) -> Unit,
        onError: (String) -> Unit = {}
    ): ListenerRegistration? {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onOrdersUpdated(emptyList())
            return null
        }
        return db.collection("orders")
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (auth.currentUser == null) return@addSnapshotListener
                    val errJson = handleFirestoreError(error, OperationType.LIST, "orders")
                    onError(error.localizedMessage ?: errJson)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val records = snapshot.toObjects(
                        FirestoreOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                        .map { it.toOrderRecord() }
                    onOrdersUpdated(records)
                }
            }
    }

    fun listenToAllOrdersForAdmin(
        onOrdersUpdated: (List<FirestoreOrder>) -> Unit,
        onError: (String) -> Unit = {}
    ): ListenerRegistration? {
        val currentUser = auth.currentUser ?: return null
        val userEmail = currentUser.email?.trim().orEmpty().ifBlank {
            currentUser.providerData.firstNotNullOfOrNull { it.email?.trim()?.takeIf { e -> e.isNotBlank() } }.orEmpty()
        }
        if (!AuthAndOrderManager.isSoleAdminEmail(userEmail)) {
            onOrdersUpdated(emptyList())
            return null
        }
        return db.collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (auth.currentUser == null) return@addSnapshotListener
                    val errJson = handleFirestoreError(error, OperationType.LIST, "orders")
                    onError(error.localizedMessage ?: errJson)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val orders = snapshot.toObjects(
                        FirestoreOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                    onOrdersUpdated(orders)
                }
            }
    }

    suspend fun updateOrderStatusAsAdmin(orderId: String, newStatus: String): Result<Unit> {
        if (orderId.isBlank()) return Result.failure(IllegalArgumentException("ცარიელი შეკვეთის ID"))
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: შეკვეთის სტატუსის Cloud-ში შესაცვლელად საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val docRef = db.collection("orders").document(orderId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Log.d("FirestoreRepo", "Order $orderId status updated to $newStatus")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteOrderFromCloud(orderId: String): Result<Unit> {
        if (orderId.isBlank()) return Result.failure(IllegalArgumentException("ცარიელი შეკვეთის ID"))
        if (auth.currentUser == null) {
            checkIsCurrentUserAdmin()
        }
        if (auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: შეკვეთის Cloud-იდან წასაშლელად საჭიროა ავტორიზაცია")
            )
        }
        val docRef = db.collection("orders").document(orderId)
        return try {
            docRef.delete().await()
            Log.d("FirestoreRepo", "Order $orderId deleted from Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveDiagnosticReport(
        deviceModel: String,
        issueCategory: String,
        symptoms: String,
        aiAnalysis: String,
        estimatedCost: String = "",
        confidence: Double = 95.0
    ): String {
        val diagId = "diag_" + System.currentTimeMillis()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d("FirestoreRepo", "Skipping Firestore diagnostic sync: user is in local session mode")
            return diagId
        }
        val uid = currentUser.uid
        val docRef = db.collection("diagnostics").document(diagId)
        val payload = hashMapOf<String, Any>(
            "id" to diagId,
            "userId" to uid,
            "deviceModel" to deviceModel.ifBlank { "Device" }.take(100),
            "issueCategory" to issueCategory.ifBlank { "General" }.take(100),
            "symptoms" to symptoms.take(2000),
            "aiAnalysis" to aiAnalysis.ifBlank { "Analysis" }.take(10000),
            "estimatedCost" to estimatedCost.take(50),
            "confidence" to confidence.coerceIn(0.0, 100.0),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            Log.d("FirestoreRepo", "Diagnostic report saved: $diagId")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
        return diagId
    }

    // ==========================================================================
    // PRODUCTS REAL-TIME & CRUD OPERATIONS (Central Source of Truth)
    // ==========================================================================
    data class ProductSyncSnapshot(
        val products: List<ProductItem>,
        val removedProductIds: Set<Int>,
        val presentDocIds: Set<String>,
        val isFromCache: Boolean
    )

    sealed class MigrationOutcome {
        data class Migrated(val uploadedCount: Int) : MigrationOutcome()
        data class AlreadyPopulated(
            val remoteCount: Int,
            val conflicts: List<AdminStorage.ProductConflict>
        ) : MigrationOutcome()
        object SkippedNotAdmin : MigrationOutcome()
    }

    fun parseProductDocument(doc: DocumentSnapshot): ProductItem? {
        if (!doc.exists()) return null
        if (doc.getBoolean("isDeleted") == true) return null

        val title = (doc.getString("title") ?: doc.getString("name") ?: "").trim()
        if (title.isBlank()) return null

        val price = (doc.getString("price") ?: "ფასი შეთანხმებით").trim()
        val ratingRaw = doc.get("rating")
        val rating = when (ratingRaw) {
            is Number -> ratingRaw.toDouble()
            is String -> ratingRaw.toDoubleOrNull() ?: 5.0
            else -> 5.0
        }.coerceIn(0.0, 5.0)

        val image = (doc.getString("imageUrl") ?: doc.getString("image") ?: "").trim()
        val category = (doc.getString("category") ?: "ეკრანები").trim()
        val explicitId = (doc.get("productId") as? Number)?.toInt()
            ?: doc.id.removePrefix("prod_").toIntOrNull()
            ?: kotlin.math.abs(doc.id.hashCode()).let { if (it == 0) 9999 else it }

        val shortDesc = doc.getString("shortDescription") ?: ""
        val fullDesc = doc.getString("description") ?: ""
        val subcat = doc.getString("subcategory") ?: ""
        val brand = doc.getString("brand") ?: ""
        val inStock = doc.getBoolean("inStock") ?: doc.getBoolean("isAvailable") ?: true
        val isFeatured = doc.getBoolean("isFeatured") ?: false
        val displayOrder = (doc.get("displayOrder") as? Number)?.toInt()
            ?: (doc.get("sortOrder") as? Number)?.toInt()
            ?: 0
        val type = doc.getString("type")
            ?: if (category == "პროგრამული სერვისები") "software" else "product"
        val featuresList = (doc.get("features") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        val targetAudience = doc.getString("targetAudience") ?: ""
        val offerType = doc.getString("offerType") ?: ""
        val duration = doc.getString("duration") ?: ""
        val accountType = doc.getString("accountType") ?: ""
        val activationTerms = doc.getString("activationTerms") ?: ""
        val officialUrl = doc.getString("officialUrl") ?: ""
        val termsUrl = doc.getString("termsUrl") ?: ""
        val lastVerifiedAt = doc.getString("lastVerifiedAt") ?: "2026-10-25"
        val searchKeywords = doc.getString("searchKeywords") ?: ""
        val compatibleModels = doc.getString("compatibleModels") ?: ""
        val warranty = doc.getString("warranty") ?: "6 თვე"
        val brandColorHex = doc.getString("brandColorHex") ?: "#FF7A00"
        val brandBadgeText = doc.getString("brandBadgeText") ?: ""

        val imagesRaw = doc.get("images") as? List<*>
        val imagesList = imagesRaw?.mapNotNull { it as? String }?.filter { it.isNotBlank() } ?: emptyList()
        val primaryImg = doc.getString("primaryImageUrl")
        val specsRaw = doc.get("specifications") as? Map<*, *>
        val specsMap = specsRaw?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap()
        val productType = doc.getString("productType") ?: ""
        val condition = doc.getString("condition") ?: "ახალი"
        val exactModel = doc.getString("exactModel") ?: ""

        return ProductItem(
            id = explicitId,
            title = title,
            price = price,
            category = category,
            type = type,
            subcategory = subcat,
            imageUrl = if (image.isBlank()) ProductImageManager.AUTO_GENERATED_IMAGE_MARKER else image,
            shortDescription = shortDesc,
            description = fullDesc,
            features = featuresList,
            targetAudience = targetAudience,
            offerType = offerType,
            duration = duration,
            accountType = accountType,
            activationTerms = activationTerms,
            officialUrl = officialUrl,
            termsUrl = termsUrl,
            lastVerifiedAt = lastVerifiedAt,
            searchKeywords = searchKeywords,
            isFeatured = isFeatured,
            displayOrder = displayOrder,
            rating = rating,
            inStock = inStock,
            brand = brand.ifBlank { if (title.contains("iPhone", ignoreCase = true)) "Apple" else "Samsung" },
            compatibleModels = compatibleModels,
            warranty = warranty,
            firestoreDocId = doc.id,
            brandColorHex = brandColorHex,
            brandBadgeText = brandBadgeText,
            images = imagesList,
            primaryImageUrl = primaryImg,
            specifications = specsMap,
            productType = productType,
            condition = condition,
            exactModel = exactModel
        )
    }

    private fun extractProductIdFromDoc(doc: DocumentSnapshot): Int {
        return (doc.get("productId") as? Number)?.toInt()
            ?: doc.id.removePrefix("prod_").toIntOrNull()
            ?: kotlin.math.abs(doc.id.hashCode()).let { if (it == 0) 9999 else it }
    }

    fun listenToProductsWithChanges(
        onSnapshotUpdated: (ProductSyncSnapshot) -> Unit,
        onError: (String) -> Unit = {}
    ): ListenerRegistration {
        return db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val errJson = handleFirestoreError(error, OperationType.LIST, "products")
                    onError(formatUserFriendlyFirestoreError(error, errJson))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val prods = mutableListOf<ProductItem>()
                    val removedIds = mutableSetOf<Int>()
                    val presentDocIds = mutableSetOf<String>()

                    for (change in snapshot.documentChanges) {
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                            removedIds.add(extractProductIdFromDoc(change.document))
                        }
                    }

                    for (doc in snapshot.documents) {
                        if (doc.getBoolean("isDeleted") == true) {
                            removedIds.add(extractProductIdFromDoc(doc))
                            continue
                        }
                        val parsed = parseProductDocument(doc)
                        if (parsed != null) {
                            prods.add(parsed)
                            presentDocIds.add(doc.id)
                        }
                    }

                    if (prods.isNotEmpty() || removedIds.isNotEmpty()) {
                        onSnapshotUpdated(
                            ProductSyncSnapshot(
                                products = prods,
                                removedProductIds = removedIds,
                                presentDocIds = presentDocIds,
                                isFromCache = snapshot.metadata.isFromCache
                            )
                        )
                    }
                }
            }
    }

    fun listenToProducts(onProductsUpdated: (List<ProductItem>) -> Unit): ListenerRegistration {
        return listenToProductsWithChanges(
            onSnapshotUpdated = { syncSnapshot ->
                if (syncSnapshot.products.isNotEmpty()) {
                    onProductsUpdated(syncSnapshot.products)
                }
            }
        )
    }

    suspend fun fetchProductsOnce(): Result<List<ProductItem>> {
        return try {
            val snapshot = db.collection("products").get().await()
            val list = snapshot.documents.mapNotNull { parseProductDocument(it) }
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "products")
            Result.failure(e)
        }
    }

    private fun buildProductPayload(item: ProductItem, includeCreatedAt: Boolean): HashMap<String, Any> {
        val safeTitle = item.title.ifBlank { "ნაწილი" }.take(200)
        val safePrice = item.price.ifBlank { "ფასი შეთანხმებით" }.take(50)
        val payload = hashMapOf<String, Any>(
            "productId" to item.id,
            "title" to safeTitle,
            "name" to safeTitle,
            "price" to safePrice,
            "currency" to "GEL",
            "rating" to item.rating.toString().take(20),
            "image" to item.imageUrl.take(2000),
            "imageUrl" to item.imageUrl.take(2000),
            "category" to item.category.take(100),
            "type" to item.type.take(50),
            "subcategory" to item.subcategory.take(150),
            "shortDescription" to item.shortDescription.take(1000),
            "description" to item.description.take(5000),
            "brand" to item.brand.take(100),
            "inStock" to item.inStock,
            "isAvailable" to item.inStock,
            "isFeatured" to item.isFeatured,
            "displayOrder" to item.displayOrder,
            "sortOrder" to item.displayOrder,
            "features" to item.features.take(40),
            "targetAudience" to item.targetAudience.take(600),
            "offerType" to item.offerType.take(150),
            "duration" to item.duration.take(120),
            "accountType" to item.accountType.take(200),
            "activationTerms" to item.activationTerms.take(1500),
            "officialUrl" to item.officialUrl.take(1000),
            "termsUrl" to item.termsUrl.take(1000),
            "lastVerifiedAt" to item.lastVerifiedAt.take(50),
            "searchKeywords" to item.searchKeywords.take(1000),
            "compatibleModels" to item.compatibleModels.take(500),
            "warranty" to item.warranty.take(150),
            "brandColorHex" to item.brandColorHex.take(20),
            "brandBadgeText" to item.brandBadgeText.take(100),
            "updatedByAdmin" to true,
            "isDeleted" to false,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        payload["images"] = item.images.map { it.take(2000) }.take(15)
        payload["primaryImageUrl"] = (item.primaryImageUrl ?: item.imageUrl).take(2000)
        payload["specifications"] = item.specifications.entries
            .take(30)
            .associate { (k, v) -> k.take(100) to v.take(500) }
        payload["productType"] = item.productType.take(50)
        payload["condition"] = item.condition.take(50)
        payload["exactModel"] = item.exactModel.take(100)
        if (includeCreatedAt) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }
        return payload
    }

    suspend fun saveOrUpdateProduct(item: ProductItem): String {
        val docId = item.firestoreDocId.ifBlank { "prod_${item.id}" }
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            throw IllegalStateException("PERMISSION_DENIED: Cloud-ში შესანახად საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
        }
        val docRef = db.collection("products").document(docId)

        return try {
            val existingSnap = try {
                docRef.get().await()
            } catch (_: Exception) {
                null
            }
            val existingCreatedAt = runCatching { existingSnap?.getTimestamp("createdAt") }.getOrNull()
            val needsCreatedAt = existingSnap == null || !existingSnap.exists() || existingCreatedAt == null
            val payload = buildProductPayload(item, includeCreatedAt = needsCreatedAt)
            docRef.set(payload, SetOptions.merge()).await()
            Log.d("FirestoreRepo", "Product saved/updated in Firestore: $docId")
            docId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products/$docId")
            throw e
        }
    }

    suspend fun saveOrUpdateProductResult(item: ProductItem): Result<String> {
        return try {
            val docId = saveOrUpdateProduct(item)
            Result.success(docId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedCatalogToFirestore(products: List<ProductItem>): Result<Int> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: Cloud სინქრონიზაციისთვის საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        return try {
            val existingDocsWithCreatedAt = try {
                db.collection("products").get().await().documents
                    .filter { it.exists() && runCatching { it.getTimestamp("createdAt") }.getOrNull() != null }
                    .map { it.id }
                    .toSet()
            } catch (_: Exception) {
                emptySet()
            }

            var count = 0
            // უსაფრთხო WriteBatch ჩანკირება (<= 400 ოპერაცია ერთ ბეჩში, Firestore 500-ლიმიტის დაცვით)
            val uniqueByDocId = products.distinctBy { it.firestoreDocId.ifBlank { "prod_${it.id}" } }
            for (chunk in uniqueByDocId.chunked(400)) {
                val batch = db.batch()
                for (item in chunk) {
                    val docId = item.firestoreDocId.ifBlank { "prod_${item.id}" }
                    val docRef = db.collection("products").document(docId)
                    val payload = buildProductPayload(item, includeCreatedAt = docId !in existingDocsWithCreatedAt)
                    batch.set(docRef, payload, SetOptions.merge())
                    count++
                }
                batch.commit().await()
            }
            Result.success(count)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products")
            Result.failure(e)
        }
    }

    /**
     * უსაფრთხო საწყისი მიგრაცია (Safe Idempotent Initial & Missing-Product Migration):
     * - ადარებს აპის სრულ კატალოგს (117 პროდუქტი) და Firestore-ის `products` კოლექციას;
     * - არასდროს გადააწერს Firestore-ში უკვე არსებულ დოკუმენტებს ავტომატურად (ინარჩუნებს ადმინის ცვლილებებს);
     * - არ ქმნის დუბლიკატებს განმეორებით გაშვებისას (იყენებს დეტერმინისტულ `prod_{id}` ID-ებს);
     * - თუ მომხმარებელი ვერიფიცირებული ადმინისტრატორია და Firestore-ში კატალოგი ცარიელია ან ნაწილობრივაა
     *   ატვირთული, უსაფრთხო WriteBatch ნაწილებით (<= 400) ტვირთავს მხოლოდ ნაკლულ პროდუქტებს.
     */
    suspend fun performSafeInitialMigrationIfNeeded(context: Context): Result<MigrationOutcome> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val snapshot = db.collection("products").get().await()
                val existingRemote = snapshot.documents.mapNotNull { parseProductDocument(it) }
                val existingDocIds = snapshot.documents.map { it.id }.toSet()
                val existingProductIds = existingRemote.map { it.id }.toSet()

                val deletedIds = AdminStorage.getDeletedProductIds(context)
                val knownCloudDocIds = AdminStorage.getKnownCloudDocIds(context)
                val localCatalog = AdminStorage.loadProducts(context)

                // გამოვთვალოთ მხოლოდ ის პროდუქტები, რომლებიც ჯერ საერთოდ არ არსებობს Firestore-ში
                // და არც წინა სინქრონიზაციისას წაშლილა Cloud-იდან.
                val missingProducts = localCatalog.filter { item ->
                    val docId = item.firestoreDocId.ifBlank { "prod_${item.id}" }
                    item.id !in deletedIds &&
                        docId !in existingDocIds &&
                        item.id !in existingProductIds &&
                        docId !in knownCloudDocIds
                }.distinctBy { it.firestoreDocId.ifBlank { "prod_${it.id}" } }

                if (missingProducts.isEmpty()) {
                    val conflicts = AdminStorage.detectLocalVsCloudConflicts(context, existingRemote)
                    AdminStorage.setInitialCloudMigrationCompleted(context, true)
                    return@withContext Result.success(
                        MigrationOutcome.AlreadyPopulated(
                            remoteCount = existingRemote.size,
                            conflicts = conflicts
                        )
                    )
                }

                // შევამოწმოთ არის თუ არა მომხმარებელი ვერიფიცირებული ადმინი
                val isAdmin = checkIsCurrentUserAdmin()
                if (!isAdmin) {
                    return@withContext if (existingRemote.isNotEmpty()) {
                        val conflicts = AdminStorage.detectLocalVsCloudConflicts(context, existingRemote)
                        Result.success(
                            MigrationOutcome.AlreadyPopulated(
                                remoteCount = existingRemote.size,
                                conflicts = conflicts
                            )
                        )
                    } else {
                        Result.success(MigrationOutcome.SkippedNotAdmin)
                    }
                }

                var uploaded = 0
                for (chunk in missingProducts.chunked(400)) {
                    val batch = db.batch()
                    for (item in chunk) {
                        val docId = item.firestoreDocId.ifBlank { "prod_${item.id}" }
                        val docRef = db.collection("products").document(docId)
                        val payload = buildProductPayload(item, includeCreatedAt = true)
                        batch.set(docRef, payload, SetOptions.merge())
                        uploaded++
                    }
                    batch.commit().await()
                }

                AdminStorage.setInitialCloudMigrationCompleted(context, true)
                if (existingRemote.isEmpty()) {
                    AdminStorage.markLocalEditsSynced(context)
                }
                Result.success(MigrationOutcome.Migrated(uploaded))
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.LIST, "products")
                Result.failure(e)
            }
        }

    suspend fun addProduct(
        title: String,
        price: String,
        rating: String = "5.0",
        imageUrl: String,
        category: String = "ეკრანები",
        explicitProductId: Int? = null
    ): String {
        val resolvedId = explicitProductId ?: (2000 + (System.currentTimeMillis() % 100000).toInt())
        val docId = "prod_$resolvedId"
        val item = ProductItem(
            id = resolvedId,
            title = title.ifBlank { "ნაწილი" }.take(200),
            price = price.ifBlank { "0 ₾" }.take(50),
            category = category.take(100),
            type = if (category == "პროგრამული სერვისები") "software" else "product",
            imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=600" }.take(2000),
            rating = rating.toDoubleOrNull() ?: 5.0,
            inStock = true,
            firestoreDocId = docId
        )
        return saveOrUpdateProduct(item)
    }

    suspend fun deleteProduct(docId: String) {
        if (docId.isBlank()) return
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            throw IllegalStateException("PERMISSION_DENIED: პროდუქტის წასაშლელად საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
        }
        val docRef = db.collection("products").document(docId)
        try {
            docRef.delete().await()
            Log.d("FirestoreRepo", "Product deleted from Firestore: $docId")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProductResult(item: ProductItem): Result<Unit> {
        val docId = item.firestoreDocId.ifBlank { "prod_${item.id}" }
        return deleteProductResult(docId, item.id)
    }

    suspend fun deleteProductResult(docId: String, productId: Int = 0): Result<Unit> {
        val resolvedDocId = docId.ifBlank { if (productId > 0) "prod_$productId" else "" }
        return try {
            deleteProduct(resolvedDocId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun formatUserFriendlyFirestoreError(e: Throwable?, fallbackDetail: String = ""): String {
        return Companion.formatUserFriendlyFirestoreError(e, fallbackDetail)
    }

    // ==========================================================================
    // APP CONFIG: HOME BANNERS & BANK REQUISITES REAL-TIME SYNC
    // ==========================================================================
    suspend fun saveHomeBannersToCloud(banners: List<AdminStorage.HomeBannerItem>): Result<Unit> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: ბანერების Cloud-ში შესანახად საჭიროა ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val docRef = db.collection("app_config").document("home_banners")
        val bannersData = banners.mapIndexed { index, b ->
            hashMapOf<String, Any>(
                "id" to b.id,
                "imageUrl" to b.imageUrl,
                "title" to b.title,
                "subtitle" to b.subtitle,
                "targetProductId" to b.targetProductId,
                "tag" to b.tag,
                "sortOrder" to index
            )
        }
        val payload = hashMapOf<String, Any>(
            "configType" to "home_banners",
            "banners" to bannersData,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Log.d("FirestoreRepo", "Home banners synced to Firestore app_config/home_banners")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun listenToHomeBanners(onBannersUpdated: (List<AdminStorage.HomeBannerItem>) -> Unit): ListenerRegistration {
        return db.collection("app_config").document("home_banners")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "app_config/home_banners")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val rawList = snapshot.get("banners") as? List<*> ?: return@addSnapshotListener
                    val parsed = rawList.mapNotNull { entry ->
                        val map = entry as? Map<*, *> ?: return@mapNotNull null
                        val id = map["id"]?.toString() ?: return@mapNotNull null
                        val title = map["title"]?.toString() ?: ""
                        if (title.isBlank()) return@mapNotNull null
                        AdminStorage.HomeBannerItem(
                            id = id,
                            imageUrl = map["imageUrl"]?.toString() ?: "",
                            title = title,
                            subtitle = map["subtitle"]?.toString() ?: "",
                            targetProductId = (map["targetProductId"] as? Number)?.toInt() ?: 1,
                            tag = map["tag"]?.toString() ?: "სპეც შეთავაზება"
                        )
                    }
                    if (parsed.isNotEmpty()) {
                        onBannersUpdated(parsed)
                    }
                }
            }
    }

    suspend fun saveBankRequisitesToCloud(requisites: AdminStorage.BankRequisites): Result<Unit> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: რეკვიზიტების Cloud-ში შესანახად საჭიროა ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val docRef = db.collection("app_config").document("bank_requisites")
        val payload = hashMapOf<String, Any>(
            "configType" to "bank_requisites",
            "bogIban" to requisites.bogIban,
            "bogRecipient" to requisites.bogRecipient,
            "tbcIban" to requisites.tbcIban,
            "tbcRecipient" to requisites.tbcRecipient,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun listenToBankRequisites(onRequisitesUpdated: (AdminStorage.BankRequisites) -> Unit): ListenerRegistration {
        return db.collection("app_config").document("bank_requisites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "app_config/bank_requisites")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val bogIban = snapshot.getString("bogIban")?.trim().orEmpty()
                    val bogRecipient = snapshot.getString("bogRecipient")?.trim().orEmpty()
                    val tbcIban = snapshot.getString("tbcIban")?.trim().orEmpty()
                    val tbcRecipient = snapshot.getString("tbcRecipient")?.trim().orEmpty()
                    if (bogIban.isNotBlank() || tbcIban.isNotBlank()) {
                        onRequisitesUpdated(
                            AdminStorage.BankRequisites(
                                bogIban = bogIban.ifBlank { AdminStorage.DEFAULT_BOG_IBAN },
                                bogRecipient = bogRecipient.ifBlank { AdminStorage.DEFAULT_BOG_RECIPIENT },
                                tbcIban = tbcIban.ifBlank { AdminStorage.DEFAULT_TBC_IBAN },
                                tbcRecipient = tbcRecipient.ifBlank { AdminStorage.DEFAULT_TBC_RECIPIENT }
                            )
                        )
                    }
                }
            }
    }

    // ==========================================================================
    // APP CONFIG: APP UPDATE MANAGEMENT (app_config/updates)
    // ==========================================================================
    suspend fun saveAppUpdateConfigToCloud(config: AdminStorage.AppUpdateConfig): Result<Unit> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: განახლების პარამეტრების შესანახად საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val cleanVersion = config.latestVersion.trim().take(60)
        val cleanChangeLog = config.changeLog.trim().take(5000)
        val cleanUrl = config.downloadUrl.trim().take(2048)

        if (config.isPublished) {
            if (cleanVersion.isBlank()) {
                return Result.failure(IllegalArgumentException("გამოსაქვეყნებლად მიუთითეთ ახალი ვერსია (მაგ: 1.2.0)"))
            }
            if (cleanChangeLog.isBlank()) {
                return Result.failure(IllegalArgumentException("გამოსაქვეყნებლად შეავსეთ განახლების აღწერა (Change Log)"))
            }
            if (!AdminStorage.isValidHttpsDownloadUrl(cleanUrl)) {
                return Result.failure(IllegalArgumentException("გამოსაქვეყნებლად მიუთითეთ ვალიდური HTTPS APK ბმული (https://...)"))
            }
        } else if (cleanUrl.isNotBlank() && !AdminStorage.isValidHttpsDownloadUrl(cleanUrl)) {
            return Result.failure(IllegalArgumentException("APK გადმოწერის ბმული უნდა იწყებოდეს https:// პროტოკოლით"))
        }

        val docRef = db.collection("app_config").document("updates")
        val payload = hashMapOf<String, Any>(
            "configType" to "updates",
            "latestVersion" to cleanVersion,
            "changeLog" to cleanChangeLog,
            "downloadUrl" to cleanUrl,
            "isPublished" to config.isPublished,
            "updatedBy" to (auth.currentUser?.email ?: config.updatedBy).take(150),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Log.d("FirestoreRepo", "App update config synced to Firestore app_config/updates")
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    private fun parseAppUpdateSnapshot(snapshot: com.google.firebase.firestore.DocumentSnapshot): AdminStorage.AppUpdateConfig? {
        if (!snapshot.exists()) return null
        val latestVersion = snapshot.getString("latestVersion")?.trim().orEmpty()
        val changeLog = snapshot.getString("changeLog")?.trim().orEmpty()
        val downloadUrl = snapshot.getString("downloadUrl")?.trim().orEmpty()
        val isPublished = snapshot.getBoolean("isPublished") ?: false
        val updatedBy = snapshot.getString("updatedBy")?.trim().orEmpty()
        val updatedAtTs = snapshot.getTimestamp(
            "updatedAt",
            com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
        val updatedAtMillis = updatedAtTs?.toDate()?.time ?: 0L
        return AdminStorage.AppUpdateConfig(
            latestVersion = latestVersion,
            changeLog = changeLog,
            downloadUrl = downloadUrl,
            isPublished = isPublished,
            updatedAtMillis = updatedAtMillis,
            updatedBy = updatedBy
        )
    }

    suspend fun fetchAppUpdateConfigFromCloud(): Result<AdminStorage.AppUpdateConfig?> {
        val docRef = db.collection("app_config").document("updates")
        return try {
            val snapshot = docRef.get().await()
            Result.success(parseAppUpdateSnapshot(snapshot))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun listenToAppUpdateConfig(
        onUpdate: (AdminStorage.AppUpdateConfig?) -> Unit,
        onError: (String) -> Unit = {}
    ): ListenerRegistration {
        return db.collection("app_config").document("updates")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "app_config/updates")
                    onError(formatUserFriendlyFirestoreError(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    onUpdate(parseAppUpdateSnapshot(snapshot))
                } else {
                    onUpdate(null)
                }
            }
    }

    // ==========================================================================
    // 4. FCM TOKEN STORAGE & ADMIN PUSH NOTIFICATIONS (/fcm_tokens & /notifications)
    // ==========================================================================
    suspend fun saveCurrentUserFcmToken(
        token: String,
        notificationsEnabled: Boolean = true,
        deviceModel: String = android.os.Build.MODEL ?: "Android",
        deviceId: String = ""
    ): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(
            IllegalStateException("User must be signed in to save FCM token")
        )
        val cleanToken = token.trim()
        if (cleanToken.length < 10 || cleanToken.startsWith("fcm_admin_dev_")) {
            return Result.failure(IllegalArgumentException("Invalid or synthetic FCM token"))
        }
        val uid = currentUser.uid
        val tokenHash = kotlin.math.abs(cleanToken.hashCode()).toString(16)
        val safeDeviceId = deviceId.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { "dev_$tokenHash" }
            .take(64)
        val multiDeviceDocId = "${uid}_$safeDeviceId".take(120)
        val primaryDocRef = db.collection("fcm_tokens").document(multiDeviceDocId)
        val legacyUserDocRef = db.collection("fcm_tokens").document(uid)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "token" to cleanToken.take(4096),
            "deviceId" to safeDeviceId,
            "email" to (currentUser.email ?: "").take(150),
            "deviceModel" to deviceModel.take(120),
            "platform" to "android",
            "notificationsEnabled" to notificationsEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            primaryDocRef.set(payload).await()
            runCatching { legacyUserDocRef.set(payload).await() }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, primaryDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendAdminNotification(
        title: String,
        body: String,
        targetProductId: Int = 0,
        targetTab: Int = 0,
        type: String = "announcement",
        explicitNotificationId: String? = null
    ): Result<String> {
        val cleanTitle = title.trim().take(160)
        val cleanBody = body.trim().take(1500)
        if (cleanTitle.isBlank() || cleanBody.isBlank()) {
            return Result.failure(IllegalArgumentException("შეტყობინების სათაური და ტექსტი სავალდებულოა"))
        }
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: Cloud შეტყობინების გასაგზავნად საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val notifId = explicitNotificationId?.takeIf { it.isNotBlank() }
            ?: "notif_${System.currentTimeMillis()}"
        val docRef = db.collection("notifications").document(notifId)
        val safeType = if (type in listOf("announcement", "product_offer", "service_update")) type else "announcement"
        val payload = hashMapOf<String, Any>(
            "id" to notifId,
            "title" to cleanTitle,
            "body" to cleanBody,
            "type" to safeType,
            "targetProductId" to targetProductId.coerceAtLeast(0),
            "targetTab" to targetTab.coerceIn(0, 7),
            "senderEmail" to (auth.currentUser?.email ?: "").take(150),
            "audience" to "all",
            "status" to "submitted",
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(notifId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAdminNotification(
        notificationId: String,
        title: String,
        body: String,
        targetProductId: Int = 0,
        targetTab: Int = 0,
        type: String = "announcement",
        rebroadcastNow: Boolean = true
    ): Result<String> {
        val cleanId = notificationId.trim()
        if (cleanId.isBlank()) {
            return Result.failure(IllegalArgumentException("შეტყობინების ID ცარიელია"))
        }
        val cleanTitle = title.trim().take(160)
        val cleanBody = body.trim().take(1500)
        if (cleanTitle.isBlank() || cleanBody.isBlank()) {
            return Result.failure(IllegalArgumentException("შეტყობინების სათაური და ტექსტი სავალდებულოა"))
        }
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: შეტყობინების შესასწორებლად საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val docRef = db.collection("notifications").document(cleanId)
        val safeType = if (type in listOf("announcement", "product_offer", "service_update")) type else "announcement"
        val payload = hashMapOf<String, Any>(
            "id" to cleanId,
            "title" to cleanTitle,
            "body" to cleanBody,
            "type" to safeType,
            "targetProductId" to targetProductId.coerceAtLeast(0),
            "targetTab" to targetTab.coerceIn(0, 7),
            "senderEmail" to (auth.currentUser?.email ?: "").take(150),
            "audience" to "all",
            "status" to "submitted",
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (rebroadcastNow) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }
        return try {
            docRef.set(payload, SetOptions.merge()).await()
            Result.success(cleanId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAdminNotification(notificationId: String): Result<Unit> {
        if (notificationId.isBlank()) return Result.failure(IllegalArgumentException("Empty notification ID"))
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: შეტყობინების წასაშლელად საჭიროა ვერიფიცირებული ადმინისტრატორის ავტორიზაცია (beqa1994@gmail.com)")
            )
        }
        val docRef = db.collection("notifications").document(notificationId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun parseDocTimestampMillis(doc: DocumentSnapshot, field: String): Long? {
        return try {
            val raw = doc.get(field) ?: return null
            when (raw) {
                is com.google.firebase.Timestamp -> raw.toDate().time
                is java.util.Date -> raw.time
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull() ?: try {
                    java.time.Instant.parse(raw).toEpochMilli()
                } catch (_: Exception) {
                    null
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getBroadcastNotifications(): Result<List<PushNotificationRecord>> {
        return try {
            val snapshot = db.collection("notifications").limit(100).get().await()
            val items = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = (doc.getString("id") ?: doc.id).trim()
                    val title = (doc.getString("title") ?: (doc.get("title") as? String))?.trim() ?: return@mapNotNull null
                    val body = (doc.getString("body") ?: (doc.get("body") as? String))?.trim() ?: return@mapNotNull null
                    if (id.isBlank() || title.isBlank() || body.isBlank() ||
                        BekaNotificationManager.isSyntheticOrTestNotification(id, title, body)
                    ) {
                        return@mapNotNull null
                    }
                    val type = doc.getString("type") ?: (doc.get("type") as? String) ?: "announcement"
                    val targetProductId = doc.getLong("targetProductId")?.toInt()
                        ?: (doc.get("targetProductId") as? Number)?.toInt() ?: 0
                    val targetTab = doc.getLong("targetTab")?.toInt()
                        ?: (doc.get("targetTab") as? Number)?.toInt() ?: 0
                    val senderEmail = doc.getString("senderEmail") ?: ""
                    val audience = doc.getString("audience") ?: "all"
                    val status = doc.getString("status") ?: "submitted"
                    val createdAtMillis = parseDocTimestampMillis(doc, "createdAt") ?: System.currentTimeMillis()
                    val updatedAtMillis = parseDocTimestampMillis(doc, "updatedAt") ?: 0L
                    PushNotificationRecord(
                        id = id,
                        title = title,
                        body = body,
                        type = type,
                        targetProductId = targetProductId,
                        targetTab = targetTab,
                        senderEmail = senderEmail,
                        createdAtMillis = createdAtMillis,
                        updatedAtMillis = updatedAtMillis,
                        audience = audience,
                        status = status
                    )
                } catch (e: Exception) {
                    android.util.Log.w("BekaPush", "Skipping unparseable notification doc: ${doc.id}", e)
                    null
                }
            }.sortedByDescending { it.createdAtMillis }
            Result.success(items)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "notifications")
            Result.failure(e)
        }
    }

    fun listenToBroadcastNotifications(
        onError: (String) -> Unit = {},
        onNotificationsUpdated: (List<PushNotificationRecord>) -> Unit
    ): ListenerRegistration {
        return db.collection("notifications")
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "notifications")
                    onError(formatUserFriendlyFirestoreError(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = (doc.getString("id") ?: doc.id).trim()
                            val title = (doc.getString("title") ?: (doc.get("title") as? String))?.trim() ?: return@mapNotNull null
                            val body = (doc.getString("body") ?: (doc.get("body") as? String))?.trim() ?: return@mapNotNull null
                            if (id.isBlank() || title.isBlank() || body.isBlank() ||
                                BekaNotificationManager.isSyntheticOrTestNotification(id, title, body)
                            ) {
                                return@mapNotNull null
                            }
                            val type = doc.getString("type") ?: (doc.get("type") as? String) ?: "announcement"
                            val targetProductId = doc.getLong("targetProductId")?.toInt()
                                ?: (doc.get("targetProductId") as? Number)?.toInt() ?: 0
                            val targetTab = doc.getLong("targetTab")?.toInt()
                                ?: (doc.get("targetTab") as? Number)?.toInt() ?: 0
                            val senderEmail = doc.getString("senderEmail") ?: ""
                            val audience = doc.getString("audience") ?: "all"
                            val status = doc.getString("status") ?: "submitted"
                            val createdAtMillis = parseDocTimestampMillis(doc, "createdAt") ?: System.currentTimeMillis()
                            val updatedAtMillis = parseDocTimestampMillis(doc, "updatedAt") ?: 0L
                            PushNotificationRecord(
                                id = id,
                                title = title,
                                body = body,
                                type = type,
                                targetProductId = targetProductId,
                                targetTab = targetTab,
                                senderEmail = senderEmail,
                                createdAtMillis = createdAtMillis,
                                updatedAtMillis = updatedAtMillis,
                                audience = audience,
                                status = status
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("BekaPush", "Skipping unparseable notification doc: ${doc.id}", e)
                            null
                        }
                    }.sortedByDescending { it.createdAtMillis }
                    onNotificationsUpdated(items)
                }
            }
    }


    fun listenToCategories(onCategoriesUpdated: (List<CategoryItem>) -> Unit): ListenerRegistration {
        return db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "categories")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            CategoryItem(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                localizedName = doc.getString("localizedName") ?: "",
                                description = doc.getString("description") ?: "",
                                icon = doc.getString("icon") ?: "📦",
                                imageUrl = doc.getString("imageUrl") ?: "",
                                displayOrder = (doc.get("displayOrder") as? Number)?.toInt() ?: 0,
                                isActive = doc.getBoolean("isActive") ?: true,
                                isPopular = doc.getBoolean("isPopular") ?: false,
                                popularOrder = (doc.get("popularOrder") as? Number)?.toInt() ?: 0,
                                categoryType = doc.getString("categoryType") ?: "general",
                                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                                updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }.filter { it.name.isNotBlank() }
                    if (list.isNotEmpty()) {
                        onCategoriesUpdated(list.sortedBy { it.displayOrder })
                    }
                }
            }
    }

    suspend fun saveCategory(category: CategoryItem): Result<String> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(IllegalStateException("PERMISSION_DENIED: კატეგორიის შესანახად საჭიროა ადმინისტრატორი (beqa1994@gmail.com)"))
        }
        val docId = category.id.ifBlank { "cat_${System.currentTimeMillis()}" }
        val payload = hashMapOf<String, Any>(
            "id" to docId,
            "name" to category.name.trim().take(100),
            "localizedName" to category.localizedName.trim().take(100),
            "description" to category.description.trim().take(500),
            "icon" to category.icon.trim().take(20),
            "imageUrl" to category.imageUrl.trim().take(2000),
            "displayOrder" to category.displayOrder,
            "isActive" to category.isActive,
            "isPopular" to category.isPopular,
            "popularOrder" to category.popularOrder,
            "categoryType" to category.categoryType.trim().take(50),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            db.collection("categories").document(docId).set(payload, SetOptions.merge()).await()
            Result.success(docId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "categories/$docId")
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(IllegalStateException("PERMISSION_DENIED: კატეგორიის წასაშლელად საჭიროა ადმინისტრატორი (beqa1994@gmail.com)"))
        }
        return try {
            db.collection("categories").document(categoryId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "categories/$categoryId")
            Result.failure(e)
        }
    }

    // ==========================================================================
    // SECURE ADMIN-ONLY ORDER NOTIFICATIONS & MULTI-DEVICE ADMIN FCM TOKENS
    // ==========================================================================

    /**
     * Registers an FCM device token in `/admin_fcm_tokens/{tokenId}` exclusively when the
     * currently authenticated Firebase user is the sole authorized administrator ("beqa1994@gmail.com").
     * Supports multiple devices simultaneously logged in as the authorized administrator.
     */
    suspend fun registerAdminDeviceFcmToken(
        token: String,
        deviceId: String,
        deviceModel: String = android.os.Build.MODEL ?: "Android"
    ): Result<String> {
        val cleanToken = token.trim()
        if (cleanToken.length < 10 || cleanToken.startsWith("fcm_admin_dev_")) {
            return Result.failure(IllegalArgumentException("Invalid or synthetic FCM token"))
        }
        val currentUser = auth.currentUser
            ?: return Result.failure(IllegalStateException("PERMISSION_DENIED: Admin authentication required"))
        if (!checkIsCurrentUserAdmin()) {
            return Result.failure(IllegalStateException("PERMISSION_DENIED: Only beqa1994@gmail.com may register admin FCM tokens"))
        }

        val tokenHash = kotlin.math.abs(cleanToken.hashCode()).toString(16)
        val safeDeviceId = deviceId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "dev_$tokenHash" }.take(64)
        val tokenId = "adm_tok_${safeDeviceId}_$tokenHash"
        val docRef = db.collection("admin_fcm_tokens").document(tokenId)

        val payload = hashMapOf<String, Any>(
            "id" to tokenId,
            "tokenId" to tokenId,
            "token" to cleanToken.take(4096),
            "userId" to currentUser.uid,
            "email" to AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            "deviceId" to safeDeviceId,
            "deviceModel" to deviceModel.trim().ifBlank { "Android" }.take(120),
            "platform" to "android",
            "active" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload, SetOptions.merge()).await()
            // Remove any older rotated token documents for the same physical deviceId
            runCatching {
                val existingDeviceDocs = db.collection("admin_fcm_tokens")
                    .whereEqualTo("deviceId", safeDeviceId)
                    .get()
                    .await()
                for (doc in existingDeviceDocs.documents) {
                    if (doc.id != tokenId) {
                        runCatching { doc.reference.delete().await() }
                    }
                }
            }
            Result.success(tokenId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Immediately queues Firestore deletion of the current device's FCM tokens while the
     * Firebase Auth session is still active (before `auth.signOut()`).
     */
    fun enqueueUnregisterDeviceFcmTokensBeforeSignOut(
        token: String,
        deviceId: String = "",
        explicitUid: String = ""
    ) {
        val cleanToken = token.trim()
        val uid = explicitUid.trim().ifBlank { auth.currentUser?.uid.orEmpty() }
        val tokenHash = if (cleanToken.isNotBlank()) {
            kotlin.math.abs(cleanToken.hashCode()).toString(16)
        } else {
            ""
        }
        val safeDeviceId = deviceId.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { if (tokenHash.isNotBlank()) "dev_$tokenHash" else "" }
            .take(64)

        runCatching {
            if (safeDeviceId.isNotBlank() && tokenHash.isNotBlank()) {
                val adminTokenId = "adm_tok_${safeDeviceId}_$tokenHash"
                db.collection("admin_fcm_tokens").document(adminTokenId).delete()
            }
            if (uid.isNotBlank()) {
                if (safeDeviceId.isNotBlank()) {
                    val multiDeviceDocId = "${uid}_$safeDeviceId".take(120)
                    db.collection("fcm_tokens").document(multiDeviceDocId).delete()
                }
                db.collection("fcm_tokens").document(uid).delete()
            }
        }
    }

    /**
     * Removes the current device's FCM token from `/admin_fcm_tokens` and `/fcm_tokens`
     * on logout or when switching to a non-admin account.
     */
    suspend fun unregisterDeviceFcmTokens(
        token: String,
        deviceId: String = "",
        explicitUid: String = ""
    ): Result<Unit> {
        val cleanToken = token.trim()
        val uid = explicitUid.trim().ifBlank { auth.currentUser?.uid.orEmpty() }
        if (cleanToken.isBlank() && uid.isBlank()) return Result.success(Unit)
        val tokenHash = if (cleanToken.isNotBlank()) {
            kotlin.math.abs(cleanToken.hashCode()).toString(16)
        } else {
            ""
        }
        val safeDeviceId = deviceId.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .ifBlank { if (tokenHash.isNotBlank()) "dev_$tokenHash" else "" }
            .take(64)

        return try {
            if (safeDeviceId.isNotBlank() && tokenHash.isNotBlank()) {
                val adminTokenId = "adm_tok_${safeDeviceId}_$tokenHash"
                runCatching { db.collection("admin_fcm_tokens").document(adminTokenId).delete().await() }
            }
            if (uid.isNotBlank()) {
                if (safeDeviceId.isNotBlank()) {
                    val multiDeviceDocId = "${uid}_$safeDeviceId".take(120)
                    runCatching { db.collection("fcm_tokens").document(multiDeviceDocId).delete().await() }
                }
                runCatching { db.collection("fcm_tokens").document(uid).delete().await() }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cleans up an expired or invalid admin FCM token by querying `/admin_fcm_tokens` for matching token value.
     */
    suspend fun removeInvalidAdminFcmToken(token: String, deviceId: String = ""): Result<Unit> {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return Result.success(Unit)
        return try {
            unregisterDeviceFcmTokens(cleanToken, deviceId)
            if (checkIsCurrentUserAdmin()) {
                val snap = db.collection("admin_fcm_tokens")
                    .whereEqualTo("token", cleanToken)
                    .get()
                    .await()
                for (doc in snap.documents) {
                    runCatching { doc.reference.delete().await() }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Creates a sanitized, non-PII administrator order notification record in
     * `/admin_notifications/admin_order_{orderId}` when an order (`imei_check` or `service_order`)
     * is created. Never includes the full IMEI, customer private information, or report data.
     */
    suspend fun createAdminOrderNotification(
        orderId: String,
        orderType: String,
        createdByUid: String = ""
    ): Result<AdminOrderNotificationRecord> {
        val cleanOrderId = orderId.trim()
        if (cleanOrderId.isBlank()) {
            return Result.failure(IllegalArgumentException("Order ID cannot be blank"))
        }
        val normalizedType = if (orderType == "service_order") "service_order" else "imei_check"
        val notificationId = "admin_order_$cleanOrderId"
        val sanitizedTitle = if (normalizedType == "imei_check") {
            "ახალი IMEI შეკვეთა"
        } else {
            "ახალი შეკვეთა"
        }
        val sanitizedBody = if (normalizedType == "imei_check") {
            "შემოვიდა ახალი IMEI შემოწმების მოთხოვნა."
        } else {
            "შემოვიდა ახალი სერვისის შეკვეთა."
        }
        val deepLink = if (normalizedType == "imei_check") {
            "bekaservice://admin/orders/imei/$cleanOrderId"
        } else {
            "bekaservice://admin/orders/service/$cleanOrderId"
        }

        val record = AdminOrderNotificationRecord(
            notificationId = notificationId,
            orderId = cleanOrderId,
            orderType = normalizedType,
            title = sanitizedTitle,
            body = sanitizedBody,
            deepLink = deepLink,
            targetAdminEmail = AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            read = false,
            createdAtMillis = System.currentTimeMillis()
        )

        val currentUser = auth.currentUser ?: return Result.success(record)
        val docRef = db.collection("admin_notifications").document(notificationId)
        val resolvedActorUid = createdByUid.trim().ifBlank { currentUser.uid }
        val payload = hashMapOf<String, Any>(
            "id" to notificationId,
            "notificationId" to notificationId,
            "orderId" to cleanOrderId,
            "orderType" to normalizedType,
            "title" to sanitizedTitle,
            "body" to sanitizedBody,
            "deepLink" to deepLink,
            "actorUid" to resolvedActorUid,
            "targetAdminEmail" to AuthAndOrderManager.SOLE_ADMIN_EMAIL,
            "read" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(record)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    private fun parseAdminNotificationDoc(doc: DocumentSnapshot): AdminOrderNotificationRecord? {
        return try {
            val notifId = doc.getString("notificationId")?.takeIf { it.isNotBlank() } ?: doc.id
            val orderId = doc.getString("orderId") ?: return null
            val orderType = doc.getString("orderType") ?: "imei_check"
            val rawTitle = doc.getString("title") ?: "ახალი შეკვეთა"
            val rawBody = doc.getString("body") ?: "შემოვიდა ახალი შეკვეთა."
            val deepLink = doc.getString("deepLink") ?: if (orderType == "service_order") {
                "bekaservice://admin/orders/service/$orderId"
            } else {
                "bekaservice://admin/orders/imei/$orderId"
            }
            AdminOrderNotificationRecord(
                notificationId = notifId,
                orderId = orderId,
                orderType = orderType,
                title = BekaNotificationManager.stripSensitiveDataFromPayload(rawTitle),
                body = BekaNotificationManager.stripSensitiveDataFromPayload(rawBody),
                deepLink = deepLink,
                targetAdminEmail = doc.getString("targetAdminEmail") ?: AuthAndOrderManager.SOLE_ADMIN_EMAIL,
                read = doc.getBoolean("read") == true,
                createdAtMillis = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                readAtMillis = doc.getTimestamp("readAt")?.toDate()?.time ?: 0L,
                readByUid = doc.getString("readByUid") ?: ""
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchAdminOrderNotifications(): Result<List<AdminOrderNotificationRecord>> {
        if (!checkIsCurrentUserAdmin() || auth.currentUser == null) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: მხოლოდ ავტორიზებულ ადმინისტრატორს (beqa1994@gmail.com) აქვს წვდომა შეკვეთების შეტყობინებებზე.")
            )
        }
        return try {
            val snap = db.collection("admin_notifications").get().await()
            val list = snap.documents.mapNotNull { parseAdminNotificationDoc(it) }
                .sortedByDescending { it.createdAtMillis }
            Result.success(list)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "admin_notifications")
            Result.failure(e)
        }
    }

    /**
     * Real-time listener on `/admin_notifications` strictly gated to the sole authorized
     * administrator ("beqa1994@gmail.com"). Keeps unread counts and notification history
     * synchronized across multiple admin devices.
     */
    fun listenToAdminOrderNotifications(
        onNotificationsUpdated: (List<AdminOrderNotificationRecord>) -> Unit = {},
        onUpdated: (List<AdminOrderNotificationRecord>) -> Unit = onNotificationsUpdated,
        onError: (String) -> Unit = {}
    ): ListenerRegistration? {
        val currentUser = auth.currentUser ?: return null
        val userEmail = currentUser.email?.trim().orEmpty().ifBlank {
            currentUser.providerData.firstNotNullOfOrNull { it.email?.trim()?.takeIf { e -> e.isNotBlank() } }.orEmpty()
        }
        if (!AuthAndOrderManager.isSoleAdminEmail(userEmail)) {
            onUpdated(emptyList())
            return null
        }

        return db.collection("admin_notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (auth.currentUser == null) return@addSnapshotListener
                    val errJson = handleFirestoreError(error, OperationType.LIST, "admin_notifications")
                    onError(error.localizedMessage ?: errJson)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents
                        .mapNotNull { parseAdminNotificationDoc(it) }
                        .sortedByDescending { it.createdAtMillis }
                    onUpdated(items)
                }
            }
    }

    /**
     * Marks an admin order notification (and its corresponding order/IMEI request `adminRead` flag)
     * as read ONLY when invoked by the verified sole administrator ("beqa1994@gmail.com").
     */
    suspend fun markAdminOrderNotificationRead(
        orderIdOrNotificationId: String,
        orderIdOrType: String = "",
        explicitOrderType: String = ""
    ): Result<Unit> {
        val currentUser = auth.currentUser
        if (currentUser == null || !checkIsCurrentUserAdmin()) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: მხოლოდ ავტორიზებულ ადმინისტრატორს (beqa1994@gmail.com) შეუძლია შეტყობინების წაკითხულად მონიშვნა.")
            )
        }
        val cleanId = orderIdOrNotificationId.trim()
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Empty notification/order ID"))

        val orderType = when {
            explicitOrderType.isNotBlank() -> explicitOrderType
            orderIdOrType in listOf("imei_check", "service_order") -> orderIdOrType
            else -> ""
        }
        val notificationId = if (cleanId.startsWith("admin_order_")) cleanId else "admin_order_$cleanId"
        val rawOrderId = if (orderIdOrType.isNotBlank() && orderIdOrType !in listOf("imei_check", "service_order")) {
            orderIdOrType.trim()
        } else {
            cleanId.removePrefix("admin_order_")
        }
        val docRef = db.collection("admin_notifications").document(notificationId)

        return try {
            docRef.update(
                mapOf(
                    "read" to true,
                    "readAt" to FieldValue.serverTimestamp(),
                    "readByUid" to currentUser.uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (orderType == "service_order" || rawOrderId.startsWith("ord_") || rawOrderId.startsWith("order_")) {
                runCatching {
                    db.collection("orders").document(rawOrderId).update(
                        mapOf(
                            "adminRead" to true,
                            "adminReadAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else if (orderType == "imei_check" || rawOrderId.startsWith("imei_req_")) {
                runCatching {
                    db.collection("imei_requests").document(rawOrderId).update(
                        mapOf(
                            "adminRead" to true,
                            "adminReadAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun markAllAdminOrderNotificationsRead(): Result<Unit> {
        val currentUser = auth.currentUser
        if (currentUser == null || !checkIsCurrentUserAdmin()) {
            return Result.failure(
                IllegalStateException("PERMISSION_DENIED: მხოლოდ ავტორიზებულ ადმინისტრატორს (beqa1994@gmail.com) შეუძლია შეტყობინებების წაკითხულად მონიშვნა.")
            )
        }
        return try {
            val snap = db.collection("admin_notifications")
                .whereEqualTo("read", false)
                .get()
                .await()
            if (!snap.isEmpty) {
                val batch = db.batch()
                for (doc in snap.documents) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "read" to true,
                            "readAt" to FieldValue.serverTimestamp(),
                            "readByUid" to currentUser.uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "admin_notifications")
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var isCacheConfigured = false

        fun formatUserFriendlyFirestoreError(e: Throwable?, fallbackDetail: String = ""): String {
            val msg = e?.message ?: fallbackDetail
            return when {
                msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
                    msg.contains("Missing or insufficient permissions", ignoreCase = true) ->
                    "წვდომა უარყოფილია (PERMISSION_DENIED): ცვლილების შესანახად საჭიროა ვერიფიცირებული ადმინისტრატორის ანგარიში (/admins/{uid} ან admin claim)."
                msg.contains("UNAVAILABLE", ignoreCase = true) ||
                    msg.contains("network", ignoreCase = true) ->
                    "ქსელის შეცდომა: ვერ მოხერხდა Firestore სერვერთან დაკავშირება. შეამოწმეთ ინტერნეტი."
                else -> "Firestore შეცდომა: ${e?.localizedMessage ?: msg}"
            }
        }

        private fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            runCatching {
                if (com.google.firebase.FirebaseApp.getApps(appContext).isEmpty()) {
                    com.google.firebase.FirebaseApp.initializeApp(appContext)
                }
            }
            val databaseId = appContext.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(databaseId)
            if (!isCacheConfigured) {
                synchronized(this) {
                    if (!isCacheConfigured) {
                        try {
                            instance.firestoreSettings = FirebaseFirestoreSettings.Builder()
                                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                                .build()
                        } catch (_: Exception) {
                            // Ignore if settings were already locked by a prior operation
                        }
                        isCacheConfigured = true
                    }
                }
            }
            return instance
        }
    }
}
